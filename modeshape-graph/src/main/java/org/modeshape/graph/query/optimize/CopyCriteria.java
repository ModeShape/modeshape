/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.graph.query.optimize;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import net.jcip.annotations.Immutable;
import org.modeshape.graph.query.QueryContext;
import org.modeshape.graph.query.model.Column;
import org.modeshape.graph.query.model.Constraint;
import org.modeshape.graph.query.model.EquiJoinCondition;
import org.modeshape.graph.query.model.JoinCondition;
import org.modeshape.graph.query.model.PropertyExistence;
import org.modeshape.graph.query.model.PropertyValue;
import org.modeshape.graph.query.model.ReferenceValue;
import org.modeshape.graph.query.model.SameNodeJoinCondition;
import org.modeshape.graph.query.model.SelectorName;
import org.modeshape.graph.query.model.Visitable;
import org.modeshape.graph.query.model.Visitors;
import org.modeshape.graph.query.model.Visitors.AbstractVisitor;
import org.modeshape.graph.query.plan.PlanNode;
import org.modeshape.graph.query.plan.PlanUtil;
import org.modeshape.graph.query.plan.PlanNode.Property;
import org.modeshape.graph.query.plan.PlanNode.Type;

/**
 * An {@link OptimizerRule optimizer rule} that copies SELECT nodes that apply to one side of a equi-join condition so that they
 * also apply to the other side fo the equi-join condition.
 */
@Immutable
public class CopyCriteria implements OptimizerRule {

    public static final CopyCriteria INSTANCE = new CopyCriteria();

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.optimize.OptimizerRule#execute(org.modeshape.graph.query.QueryContext,
     *      org.modeshape.graph.query.plan.PlanNode, java.util.LinkedList)
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
                SelectorName selector1 = equiJoinCondition.selector1Name();
                SelectorName selector2 = equiJoinCondition.selector2Name();
                String property1 = equiJoinCondition.property1Name();
                String property2 = equiJoinCondition.property2Name();

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

            if (joinCondition instanceof EquiJoinCondition || joinCondition instanceof SameNodeJoinCondition) {
                // Then for each side of the join ...
                PlanNode left = join.getFirstChild();
                PlanNode right = join.getLastChild();
                copySelectNodes(context, left, right);
                copySelectNodes(context, right, left);
            }
        }
        return plan;
    }

    protected void copySelectNodes( QueryContext context,
                                    PlanNode fromJoined,
                                    PlanNode toJoined ) {
        // Find all of the selectors used on the 'to' side ...
        Set<SelectorName> toSelectors = new HashSet<SelectorName>();
        for (PlanNode toNode : toJoined.findAllAtOrBelow()) {
            toSelectors.addAll(toNode.getSelectors());
        }

        PlanNode nodeBelowSelects = null;

        // Walk down the 'fromJoined' side looking for all SELECT nodes ...
        for (PlanNode select : fromJoined.findAllAtOrBelow(Type.SELECT)) {
            // If all of the SELECT's selectors are also found on the right ...
            if (toSelectors.containsAll(select.getSelectors())) {
                // Copy the criteria ...
                PlanNode copy = new PlanNode(Type.SELECT, select.getSelectors());
                copy.setProperty(Property.SELECT_CRITERIA, select.getProperty(Property.SELECT_CRITERIA));

                if (nodeBelowSelects == null) {
                    nodeBelowSelects = toJoined.findAtOrBelow(Type.SOURCE, Type.JOIN, Type.SET_OPERATION, Type.NULL);
                    if (nodeBelowSelects == null) {
                        nodeBelowSelects = toJoined;
                    }
                }
                nodeBelowSelects.insertAsParent(copy);
                nodeBelowSelects = copy;
            }
        }
    }

    protected PlanNode copySelectNode( QueryContext context,
                                       PlanNode selectNode,
                                       SelectorName selectorName,
                                       String propertyName,
                                       SelectorName copySelectorName,
                                       String copyPropertyName ) {
        if (selectNode.isNot(Type.SELECT)) return null;
        if (selectNode.getSelectors().size() != 1 || !selectNode.getSelectors().contains(selectorName)) return null;

        Constraint constraint = selectNode.getProperty(Property.SELECT_CRITERIA, Constraint.class);
        Set<Column> columns = getColumnsReferencedBy(constraint);
        if (columns.size() != 1) return null;
        Column column = columns.iterator().next();
        if (!column.selectorName().equals(selectorName)) return null;
        if (!column.propertyName().equals(propertyName)) return null;

        // We know that this constraint ONLY applies to the referenced selector and property,
        // so we will duplicate this constraint ...

        // Create the new node ...
        PlanNode copy = new PlanNode(Type.SELECT, copySelectorName);

        // Copy the constraint, but change the references to the copy selector and property ...
        PlanUtil.ColumnMapping mappings = new PlanUtil.ColumnMapping(selectorName);
        mappings.map(propertyName, new Column(copySelectorName, copyPropertyName, copyPropertyName));
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
     * @return the set of Column objects, with column names that always are the string-form of the {@link Column#propertyName()
     *         property name}; never null
     */
    public static Set<Column> getColumnsReferencedBy( Visitable visitable ) {
        if (visitable == null) return Collections.emptySet();
        final Set<Column> symbols = new HashSet<Column>();
        // Walk the entire structure, so only supply a StrategyVisitor (that does no navigation) ...
        Visitors.visitAll(visitable, new AbstractVisitor() {
            protected void addColumnFor( SelectorName selectorName,
                                         String property ) {
                symbols.add(new Column(selectorName, property, property));
            }

            @Override
            public void visit( Column column ) {
                symbols.add(column);
            }

            @Override
            public void visit( EquiJoinCondition joinCondition ) {
                addColumnFor(joinCondition.selector1Name(), joinCondition.property1Name());
                addColumnFor(joinCondition.selector2Name(), joinCondition.property2Name());
            }

            @Override
            public void visit( PropertyExistence prop ) {
                addColumnFor(prop.selectorName(), prop.propertyName());
            }

            @Override
            public void visit( PropertyValue prop ) {
                addColumnFor(prop.selectorName(), prop.propertyName());
            }

            @Override
            public void visit( ReferenceValue ref ) {
                String propertyName = ref.propertyName();
                if (propertyName != null) {
                    addColumnFor(ref.selectorName(), propertyName);
                }
            }
        });
        return symbols;
    }

}
