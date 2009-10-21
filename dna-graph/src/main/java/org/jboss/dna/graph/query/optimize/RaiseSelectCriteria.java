/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.graph.query.optimize;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import net.jcip.annotations.Immutable;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.ValueFactory;
import org.jboss.dna.graph.query.QueryContext;
import org.jboss.dna.graph.query.model.Column;
import org.jboss.dna.graph.query.model.Constraint;
import org.jboss.dna.graph.query.model.EquiJoinCondition;
import org.jboss.dna.graph.query.model.JoinCondition;
import org.jboss.dna.graph.query.model.PropertyExistence;
import org.jboss.dna.graph.query.model.PropertyValue;
import org.jboss.dna.graph.query.model.SelectorName;
import org.jboss.dna.graph.query.model.Visitable;
import org.jboss.dna.graph.query.model.Visitors;
import org.jboss.dna.graph.query.model.Visitors.AbstractVisitor;
import org.jboss.dna.graph.query.plan.PlanNode;
import org.jboss.dna.graph.query.plan.PlanUtil;
import org.jboss.dna.graph.query.plan.PlanNode.Property;
import org.jboss.dna.graph.query.plan.PlanNode.Type;

/**
 * An {@link OptimizerRule optimizer rule} that moves up higher in the plan any SELECT node that appears below a JOIN node and
 * that applies to selectors that are on the other side of the join.
 * <p>
 * This step is often counterintuitive, since one of the best optimizations a query optimizer can do is to
 * {@link PushSelectCriteria push down SELECT nodes} as far as they'll go. But consider the case of a SOURCE node that appears
 * below a JOIN, where the SOURCE node is a view. The optimizer {@link ReplaceViews replaces the SOURCE node with the view
 * definition}, and if the view definition includes a SELECT node, that SELECT node appears below the JOIN. Plus, that SELECT node
 * is already pushed down as far as it can go (assuming the view isn't defined to use another view). However, the JOIN may use the
 * same selector on the opposite side, and it may be possible that the same SELECT node may apply to the other side of the JOIN.
 * In this case, we can push <i>up</i> the SELECT node higher than the JOIN, and then the push-down would cause the SELECT to be
 * copied to both sides of the JOIN.
 * </p>
 * <p>
 * Here is an example plan that involves a JOIN of two SOURCE nodes:
 * 
 * <pre>
 *          ...
 *           |
 *         JOIN
 *        /     \
 *       /       SOURCE({@link Property#SOURCE_NAME SOURCE_NAME}=&quot;t1&quot;)   
 *      /
 *   SOURCE({@link Property#SOURCE_NAME SOURCE_NAME}=&quot;v1&quot;)
 * </pre>
 * 
 * If the right-side SOURCE references the "t1" table, and the left-side SOURCE references a view "v1" defined as "
 * <code>SELECT * FROM t1 WHERE t1.id &lt; 3</code>", then the {@link ReplaceViews} rule would change this plan to be:
 * 
 * <pre>
 *           ...
 *           |
 *         JOIN
 *        /     \
 *       /       SOURCE({@link Property#SOURCE_NAME SOURCE_NAME}=&quot;t1&quot;)   
 *      /
 *    PROJECT
 *      |
 *    SELECT     applies the &quot;t1.id &lt; 3&quot; criteria
 *      |
 *   SOURCE({@link Property#SOURCE_NAME SOURCE_NAME}=&quot;t1&quot;)
 * </pre>
 * 
 * Again, the SELECT cannot be pushed down any further. But the whole query can be made more efficient - because the SELECT on the
 * left-side of the JOIN will include only those tuples from 't1' that satisfy the SELECT, the JOIN will only include those tuples
 * that also satisfy this criteria, even though more tuples are returned from the right-side SOURCE.
 * </p>
 * <p>
 * In this case, the left-hand SELECT can actually be copied to the right-hand side of the JOIN, resulting in:
 * 
 * <pre>
 *           ...
 *           |
 *         JOIN
 *        /     \
 *       /       SELECT   applies the &quot;t1.id &lt; 3&quot; criteria
 *      /          |
 *    PROJECT    SOURCE({@link Property#SOURCE_NAME SOURCE_NAME}=&quot;t1&quot;)   
 *      |
 *    SELECT   applies the &quot;t1.id &lt; 3&quot; criteria
 *      |
 *   SOURCE({@link Property#SOURCE_NAME SOURCE_NAME}=&quot;t1&quot;)
 * </pre>
 * 
 * </p>
 */
@Immutable
public class RaiseSelectCriteria implements OptimizerRule {

    public static final RaiseSelectCriteria INSTANCE = new RaiseSelectCriteria();

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.optimize.OptimizerRule#execute(org.jboss.dna.graph.query.QueryContext,
     *      org.jboss.dna.graph.query.plan.PlanNode, java.util.LinkedList)
     */
    public PlanNode execute( QueryContext context,
                             PlanNode plan,
                             LinkedList<OptimizerRule> ruleStack ) {
        Set<PlanNode> copiedSelectNodes = new HashSet<PlanNode>();

        for (PlanNode join : plan.findAllAtOrBelow(Type.JOIN)) {
            // Get the join condition ...
            JoinCondition joinCondition = join.getProperty(Property.JOIN_CONDITION, JoinCondition.class);
            if (joinCondition instanceof EquiJoinCondition) {
                EquiJoinCondition equiJoinCondition = (EquiJoinCondition)joinCondition;
                SelectorName selector1 = equiJoinCondition.getSelector1Name();
                SelectorName selector2 = equiJoinCondition.getSelector2Name();
                Name property1 = equiJoinCondition.getProperty1Name();
                Name property2 = equiJoinCondition.getProperty2Name();

                // Walk up the tree looking for SELECT nodes that apply to one of the sides ...
                PlanNode node = join.getParent();
                while (node != null) {
                    if (!copiedSelectNodes.contains(node)) {
                        PlanNode copy = copySelectNode(context, node, selector1, property1, selector2, property2);
                        if (copy != null) {
                            node.insertAsParent(copy);
                            copiedSelectNodes.add(node);
                            copiedSelectNodes.add(copy);
                        } else {
                            copy = copySelectNode(context, node, selector2, property2, selector1, property1);
                            if (copy != null) {
                                node.insertAsParent(copy);
                                copiedSelectNodes.add(node);
                                copiedSelectNodes.add(copy);
                            }
                        }
                    }
                    node = node.getParent();
                }
            }
        }
        return plan;
    }

    protected PlanNode copySelectNode( QueryContext context,
                                       PlanNode selectNode,
                                       SelectorName selectorName,
                                       Name propertyName,
                                       SelectorName copySelectorName,
                                       Name copyPropertyName ) {
        if (selectNode.isNot(Type.SELECT)) return null;
        if (selectNode.getSelectors().size() != 1 || !selectNode.getSelectors().contains(selectorName)) return null;

        Constraint constraint = selectNode.getProperty(Property.SELECT_CRITERIA, Constraint.class);
        Set<Column> columns = getColumnsReferencedBy(constraint, context.getExecutionContext());
        if (columns.size() != 1) return null;
        Column column = columns.iterator().next();
        if (!column.getSelectorName().equals(selectorName)) return null;
        if (!column.getPropertyName().equals(propertyName)) return null;

        // We know that this constraint ONLY applies to the referenced selector and property,
        // so we will duplicate this constraint ...

        // Create the new node ...
        PlanNode copy = new PlanNode(Type.SELECT, copySelectorName);

        // Copy the constraint, but change the references to the copy selector and property ...
        ValueFactory<String> stringFactory = context.getExecutionContext().getValueFactories().getStringFactory();
        PlanUtil.ColumnMapping mappings = new PlanUtil.ColumnMapping(selectorName);
        mappings.map(stringFactory.create(propertyName), new Column(copySelectorName, copyPropertyName,
                                                                    stringFactory.create(copyPropertyName)));
        Constraint newCriteria = PlanUtil.replaceReferences(context, constraint, mappings, copy);
        copy.setProperty(Property.SELECT_CRITERIA, newCriteria);

        return copy;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    /**
     * Get the set of Column objects that represent those columns referenced by the visitable object.
     * 
     * @param visitable the object to be visited
     * @param context the context; may not be null
     * @return the set of Column objects, with column names that always are the string-form of the
     *         {@link Column#getPropertyName() property name}; never null
     */
    public static Set<Column> getColumnsReferencedBy( Visitable visitable,
                                                      ExecutionContext context ) {
        if (visitable == null) return Collections.emptySet();
        final ValueFactory<String> stringFactory = context.getValueFactories().getStringFactory();
        final Set<Column> symbols = new HashSet<Column>();
        // Walk the entire structure, so only supply a StrategyVisitor (that does no navigation) ...
        Visitors.visitAll(visitable, new AbstractVisitor() {
            protected void addColumnFor( SelectorName selectorName,
                                         Name property ) {
                symbols.add(new Column(selectorName, property, stringFactory.create(property)));
            }

            @Override
            public void visit( Column column ) {
                symbols.add(column);
            }

            @Override
            public void visit( EquiJoinCondition joinCondition ) {
                addColumnFor(joinCondition.getSelector1Name(), joinCondition.getProperty1Name());
                addColumnFor(joinCondition.getSelector2Name(), joinCondition.getProperty2Name());
            }

            @Override
            public void visit( PropertyExistence prop ) {
                addColumnFor(prop.getSelectorName(), prop.getPropertyName());
            }

            @Override
            public void visit( PropertyValue prop ) {
                addColumnFor(prop.getSelectorName(), prop.getPropertyName());
            }
        });
        return symbols;
    }

}
