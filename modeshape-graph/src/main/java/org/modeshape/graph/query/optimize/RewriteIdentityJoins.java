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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import net.jcip.annotations.Immutable;
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.query.QueryContext;
import org.modeshape.graph.query.model.Column;
import org.modeshape.graph.query.model.EquiJoinCondition;
import org.modeshape.graph.query.model.JoinCondition;
import org.modeshape.graph.query.model.SameNodeJoinCondition;
import org.modeshape.graph.query.model.SelectorName;
import org.modeshape.graph.query.plan.PlanNode;
import org.modeshape.graph.query.plan.PlanUtil;
import org.modeshape.graph.query.plan.PlanNode.Property;
import org.modeshape.graph.query.plan.PlanNode.Type;
import org.modeshape.graph.query.validate.Schemata;
import org.modeshape.graph.query.validate.Schemata.Table;

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
     * @see org.modeshape.graph.query.optimize.OptimizerRule#execute(org.modeshape.graph.query.QueryContext,
     *      org.modeshape.graph.query.plan.PlanNode, java.util.LinkedList)
     */
    public PlanNode execute( QueryContext context,
                             PlanNode plan,
                             LinkedList<OptimizerRule> ruleStack ) {
        if (!context.getHints().hasJoin) return plan;

        // For each of the JOIN nodes ...
        Map<SelectorName, SelectorName> rewrittenSelectors = null;
        int rewrittenJoins = 0;
        int numJoins = 0;
        for (PlanNode joinNode : plan.findAllAtOrBelow(Type.JOIN)) {
            ++numJoins;
            JoinCondition condition = joinNode.getProperty(Property.JOIN_CONDITION, JoinCondition.class);
            if (condition instanceof EquiJoinCondition) {
                PlanNode leftNode = joinNode.getFirstChild().findAtOrBelow(Type.SOURCE);
                PlanNode rightNode = joinNode.getLastChild().findAtOrBelow(Type.SOURCE);
                assert leftNode != null;
                assert rightNode != null;
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
                String leftColumnName = equiJoin.property1Name();
                String rightColumnName = equiJoin.property2Name();
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
                    rewriteJoinNode(context, joinNode, rewrittenSelectors);
                    ++rewrittenJoins;
                }
            } else if (condition instanceof SameNodeJoinCondition) {
                SameNodeJoinCondition sameNodeCondition = (SameNodeJoinCondition)condition;
                if (sameNodeCondition.selector2Path() == null) {
                    // It meets all the criteria, so rewrite this join node ...
                    if (rewrittenSelectors == null) rewrittenSelectors = new HashMap<SelectorName, SelectorName>();
                    rewriteJoinNode(context, joinNode, rewrittenSelectors);
                    ++rewrittenJoins;
                }
            }
        }

        if (rewrittenSelectors != null && !rewrittenSelectors.isEmpty()) {
            // We re-wrote at least one JOIN, but since this only applies to JOIN nodes that meet certain criteria,
            // the rewriting may have changed JOIN nodes that did not meet this criteria into nodes that now meet
            // this criteria, so we need to re-run this rule...
            ruleStack.addFirst(this);

            // After this rule is done as is no longer needed, we need to try to push SELECTs and PROJECTs again ...
            if (!(ruleStack.peek() instanceof PushSelectCriteria)) {
                // We haven't already added these, so add them now ...
                ruleStack.addFirst(PushProjects.INSTANCE);
                if (context.getHints().hasCriteria) {
                    ruleStack.addFirst(PushSelectCriteria.INSTANCE);
                }
            }

            // Now rewrite the various portions of the plan that make use of the now-removed selectors ...
            PlanUtil.replaceReferencesToRemovedSource(context, plan, rewrittenSelectors);

            assert rewrittenJoins > 0;
            if (rewrittenJoins == numJoins) {
                assert plan.findAllAtOrBelow(Type.JOIN).isEmpty();
                context.getHints().hasJoin = false;
            }
        }
        return plan;
    }

    protected void rewriteJoinNode( QueryContext context,
                                    PlanNode joinNode,
                                    Map<SelectorName, SelectorName> rewrittenSelectors ) {
        // Remove the right source node from the join node ...
        PlanNode rightChild = joinNode.getLastChild();
        rightChild.removeFromParent();
        PlanNode rightSource = rightChild.findAtOrBelow(Type.SOURCE);

        // Replace the join node with the left source node ...
        PlanNode leftChild = joinNode.getFirstChild();
        joinNode.extractFromParent();
        PlanNode leftSource = leftChild.findAtOrBelow(Type.SOURCE);

        // Combine the right PROJECT node with that on the left ...
        PlanNode rightProject = rightChild.findAtOrBelow(Type.PROJECT);
        if (rightProject != null) {
            PlanNode leftProject = leftChild.findAtOrBelow(Type.PROJECT);
            if (leftProject != null) {
                List<Column> leftColumns = leftProject.getPropertyAsList(Property.PROJECT_COLUMNS, Column.class);
                for (Column rightColumn : rightProject.getPropertyAsList(Property.PROJECT_COLUMNS, Column.class)) {
                    if (!leftColumns.contains(rightColumn)) leftColumns.add(rightColumn);
                }
            } else {
                // Just create a project on the left side ...
                leftProject = new PlanNode(Type.PROJECT);
                leftProject.setProperty(Property.PROJECT_COLUMNS, rightProject.getProperty(Property.PROJECT_COLUMNS));
                leftChild.getFirstChild().insertAsParent(leftProject);
            }
        }

        // Accumulate any SELECT nodes from the right side and add to the left ...
        PlanNode topRightSelect = rightChild.findAtOrBelow(Type.SELECT);
        if (topRightSelect != null) {
            PlanNode bottomRightSelect = topRightSelect;
            while (true) {
                if (bottomRightSelect.getFirstChild().isNot(Type.SELECT)) break;
                bottomRightSelect = bottomRightSelect.getFirstChild();
            }
            topRightSelect.setParent(null);
            bottomRightSelect.removeAllChildren();
            // Place just above the left source ...
            leftSource.getParent().addLastChild(topRightSelect);
            leftSource.setParent(bottomRightSelect);
        }

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
