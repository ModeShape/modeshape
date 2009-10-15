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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import net.jcip.annotations.Immutable;
import org.jboss.dna.graph.GraphI18n;
import org.jboss.dna.graph.property.ValueFactory;
import org.jboss.dna.graph.query.QueryContext;
import org.jboss.dna.graph.query.model.EquiJoinCondition;
import org.jboss.dna.graph.query.model.JoinCondition;
import org.jboss.dna.graph.query.model.SelectorName;
import org.jboss.dna.graph.query.plan.PlanNode;
import org.jboss.dna.graph.query.plan.PlanUtil;
import org.jboss.dna.graph.query.plan.PlanNode.Property;
import org.jboss.dna.graph.query.plan.PlanNode.Type;
import org.jboss.dna.graph.query.validate.Schemata;
import org.jboss.dna.graph.query.validate.Schemata.Table;

/**
 * An {@link OptimizerRule optimizer rule} that rewrites JOIN nodes that have {@link EquiJoinCondition equi-join criteria} where
 * the columns involved in the equi-join are all identity columns (that is, they form a {@link Schemata.Table#getKeys() key} for
 * the table). This rewrite only happens when the left and right children of the JOIN node are both SOURCE nodes.
 * <p>
 * The basic idea is that in these identity equi-join cases, the following structure:
 * 
 * <pre>
 *          ...
 *           |
 *         JOIN
 *        /     \
 *       /       \
 *   SOURCE     SOURCE
 * </pre>
 * 
 * is transformed into a simple SOURCE node:
 * 
 * <pre>
 *        ...
 *         |
 *       SOURCE
 * </pre>
 * 
 * Note that this rewriting removes a selector, and thus the nodes above the JOIN node that made use of the removed selector are
 * also modified to reference the remaining selector.
 * </p>
 */
@Immutable
public class RewriteIdentityJoins implements OptimizerRule {

    public static final RewriteIdentityJoins INSTANCE = new RewriteIdentityJoins();

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.optimize.OptimizerRule#execute(org.jboss.dna.graph.query.QueryContext,
     *      org.jboss.dna.graph.query.plan.PlanNode, java.util.LinkedList)
     */
    public PlanNode execute( QueryContext context,
                             PlanNode plan,
                             LinkedList<OptimizerRule> ruleStack ) {
        // For each of the JOIN nodes ...
        Map<SelectorName, SelectorName> rewrittenSelectors = null;
        for (PlanNode joinNode : plan.findAllAtOrBelow(Type.JOIN)) {
            JoinCondition condition = joinNode.getProperty(Property.JOIN_CONDITION, JoinCondition.class);
            if (condition instanceof EquiJoinCondition) {
                PlanNode leftNode = joinNode.getFirstChild();
                PlanNode rightNode = joinNode.getLastChild();
                assert leftNode != null;
                assert rightNode != null;
                if (leftNode.getType() == Type.SOURCE && rightNode.getType() == Type.SOURCE) {
                    EquiJoinCondition equiJoin = (EquiJoinCondition)condition;
                    // Find the names (or aliases) of the tables ...
                    Schemata schemata = context.getSchemata();
                    assert schemata != null;
                    SelectorName leftTableName = leftNode.getProperty(Property.SOURCE_NAME, SelectorName.class);
                    SelectorName rightTableName = rightNode.getProperty(Property.SOURCE_NAME, SelectorName.class);
                    assert leftTableName != null;
                    assert rightTableName != null;
                    // Presumably the join condition is using at least one alias, but we only care about the actual name ...
                    if (!leftTableName.equals(rightTableName)) {
                        // The join is not joining the same table, so this doesn't meet the condition ...
                        continue;
                    }
                    // Find the schemata columns referenced by the join condition ...
                    Table table = schemata.getTable(leftTableName);
                    if (table == null) {
                        context.getProblems().addError(GraphI18n.tableDoesNotExist, leftTableName);
                        continue;
                    }
                    ValueFactory<String> stringFactory = context.getExecutionContext().getValueFactories().getStringFactory();
                    String leftColumnName = stringFactory.create(equiJoin.getProperty1Name());
                    String rightColumnName = stringFactory.create(equiJoin.getProperty2Name());
                    Schemata.Column leftColumn = table.getColumn(leftColumnName);
                    Schemata.Column rightColumn = table.getColumn(rightColumnName);
                    if (leftColumn == null) {
                        context.getProblems().addError(GraphI18n.columnDoesNotExistOnTable, leftColumnName, leftTableName);
                        continue;
                    }
                    if (rightColumn == null) {
                        context.getProblems().addError(GraphI18n.columnDoesNotExistOnTable, rightColumnName, leftTableName);
                        continue;
                    }
                    // Are the join columns (on both sides) keys?
                    if (table.hasKey(leftColumn) && (rightColumn == leftColumn || table.hasKey(rightColumn))) {
                        // It meets all the criteria, so rewrite this join node ...
                        if (rewrittenSelectors == null) rewrittenSelectors = new HashMap<SelectorName, SelectorName>();
                        rewriteJoinNode(context, joinNode, equiJoin, rewrittenSelectors);
                    }
                }
            }
        }

        if (rewrittenSelectors != null && !rewrittenSelectors.isEmpty()) {
            // We re-wrote at least one JOIN, but since this only applies to JOIN nodes that meet certain criteria,
            // the rewriting may have changed JOIN nodes that did not meet this criteria into nodes that now meet
            // this criteria, so we need to re-run this rule...
            ruleStack.addFirst(this);

            // Now rewrite the various portions of the plan that make use of the now-removed selectors ...
            PlanUtil.replaceReferencesToRemovedSource(context, plan, rewrittenSelectors);
        } else {
            // There are no-untouched JOIN nodes, which means the sole JOIN node was rewritten as a single SOURCE node
            assert plan.findAllAtOrBelow(Type.JOIN).isEmpty();
            context.getHints().hasJoin = false;
        }
        return plan;
    }

    protected void rewriteJoinNode( QueryContext context,
                                    PlanNode joinNode,
                                    EquiJoinCondition joinCondition,
                                    Map<SelectorName, SelectorName> rewrittenSelectors ) {
        // Remove the right source node from the join node ...
        PlanNode rightSource = joinNode.getLastChild();
        rightSource.removeFromParent();

        // Replace the join node with the left source node ...
        PlanNode leftSource = joinNode.getFirstChild();
        joinNode.extractFromParent();

        // Now record that references to the right selector name should be removed ...
        SelectorName rightTableName = rightSource.getProperty(Property.SOURCE_NAME, SelectorName.class);
        SelectorName rightTableAlias = rightSource.getProperty(Property.SOURCE_ALIAS, SelectorName.class);
        SelectorName leftTableAlias = leftSource.getProperty(Property.SOURCE_ALIAS, SelectorName.class);
        if (leftTableAlias != null) {
            if (rightTableName != null) rewrittenSelectors.put(rightTableName, leftTableAlias);
            if (rightTableAlias != null) rewrittenSelectors.put(rightTableAlias, leftTableAlias);
        } else {
            SelectorName leftTableName = leftSource.getProperty(Property.SOURCE_NAME, SelectorName.class);
            assert leftTableName != null;
            if (rightTableName != null) rewrittenSelectors.put(rightTableName, leftTableName);
            if (rightTableAlias != null) rewrittenSelectors.put(rightTableAlias, leftTableName);
        }
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

}
