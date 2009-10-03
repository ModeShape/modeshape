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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.jcip.annotations.Immutable;
import org.jboss.dna.graph.GraphI18n;
import org.jboss.dna.graph.property.ValueFactory;
import org.jboss.dna.graph.query.QueryContext;
import org.jboss.dna.graph.query.model.And;
import org.jboss.dna.graph.query.model.ChildNode;
import org.jboss.dna.graph.query.model.ChildNodeJoinCondition;
import org.jboss.dna.graph.query.model.Column;
import org.jboss.dna.graph.query.model.Comparison;
import org.jboss.dna.graph.query.model.Constraint;
import org.jboss.dna.graph.query.model.DescendantNode;
import org.jboss.dna.graph.query.model.DescendantNodeJoinCondition;
import org.jboss.dna.graph.query.model.DynamicOperand;
import org.jboss.dna.graph.query.model.EquiJoinCondition;
import org.jboss.dna.graph.query.model.FullTextSearch;
import org.jboss.dna.graph.query.model.FullTextSearchScore;
import org.jboss.dna.graph.query.model.JoinCondition;
import org.jboss.dna.graph.query.model.Length;
import org.jboss.dna.graph.query.model.LowerCase;
import org.jboss.dna.graph.query.model.NodeDepth;
import org.jboss.dna.graph.query.model.NodeLocalName;
import org.jboss.dna.graph.query.model.NodeName;
import org.jboss.dna.graph.query.model.NodePath;
import org.jboss.dna.graph.query.model.Not;
import org.jboss.dna.graph.query.model.Or;
import org.jboss.dna.graph.query.model.Ordering;
import org.jboss.dna.graph.query.model.PropertyExistence;
import org.jboss.dna.graph.query.model.PropertyValue;
import org.jboss.dna.graph.query.model.SameNode;
import org.jboss.dna.graph.query.model.SameNodeJoinCondition;
import org.jboss.dna.graph.query.model.SelectorName;
import org.jboss.dna.graph.query.model.StaticOperand;
import org.jboss.dna.graph.query.model.UpperCase;
import org.jboss.dna.graph.query.plan.PlanNode;
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
            replaceReferencesToRemovedSource(context, plan, rewrittenSelectors);
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

    protected void replaceReferencesToRemovedSource( QueryContext context,
                                                     PlanNode planNode,
                                                     Map<SelectorName, SelectorName> rewrittenSelectors ) {
        switch (planNode.getType()) {
            case PROJECT:
                List<Column> columns = planNode.getPropertyAsList(Property.PROJECT_COLUMNS, Column.class);
                for (int i = 0; i != columns.size(); ++i) {
                    Column column = columns.get(i);
                    SelectorName replacement = rewrittenSelectors.get(column.getSelectorName());
                    if (replacement != null) {
                        columns.set(i, new Column(replacement, column.getPropertyName(), column.getColumnName()));
                    }
                }
                break;
            case SELECT:
                Constraint constraint = planNode.getProperty(Property.SELECT_CRITERIA, Constraint.class);
                Constraint newConstraint = replaceReferencesToRemovedSource(context, constraint, rewrittenSelectors);
                if (constraint != newConstraint) {
                    planNode.setProperty(Property.SELECT_CRITERIA, newConstraint);
                }
                break;
            case SORT:
                List<Object> orderBys = planNode.getPropertyAsList(Property.SORT_ORDER_BY, Object.class);
                if (orderBys != null && !orderBys.isEmpty()) {
                    if (orderBys.get(0) instanceof SelectorName) {
                        for (int i = 0; i != orderBys.size(); ++i) {
                            SelectorName selectorName = (SelectorName)orderBys.get(i);
                            SelectorName replacement = rewrittenSelectors.get(selectorName);
                            if (replacement != null) {
                                orderBys.set(i, replacement);
                            }
                        }
                    } else {
                        for (int i = 0; i != orderBys.size(); ++i) {
                            Ordering ordering = (Ordering)orderBys.get(i);
                            DynamicOperand operand = ordering.getOperand();
                            orderBys.set(i, replaceReferencesToRemovedSource(context, operand, rewrittenSelectors));
                        }
                    }
                }
                break;
            case JOIN:
                // Update the join condition ...
                JoinCondition joinCondition = planNode.getProperty(Property.JOIN_CONDITION, JoinCondition.class);
                JoinCondition newCondition = replaceReferencesToRemovedSource(context, joinCondition, rewrittenSelectors);
                if (joinCondition != newCondition) {
                    planNode.setProperty(Property.JOIN_CONDITION, newCondition);
                }

                // Update the join criteria (if there are some) ...
                List<Constraint> constraints = planNode.getPropertyAsList(Property.JOIN_CONSTRAINTS, Constraint.class);
                if (constraints != null && !constraints.isEmpty()) {
                    for (int i = 0; i != constraints.size(); ++i) {
                        Constraint old = constraints.get(i);
                        Constraint replacement = replaceReferencesToRemovedSource(context, old, rewrittenSelectors);
                        if (replacement != old) {
                            constraints.set(i, replacement);
                        }
                    }
                }
                break;
            case GROUP:
            case SET_OPERATION:
            case DUP_REMOVE:
            case LIMIT:
            case NULL:
            case SOURCE:
            case ACCESS:
                // None of these have to be changed ...
                break;
        }

        // Update the selectors referenced by the node ...
        Set<SelectorName> selectorsToAdd = null;
        for (Iterator<SelectorName> iter = planNode.getSelectors().iterator(); iter.hasNext();) {
            SelectorName replacement = rewrittenSelectors.get(iter.next());
            if (replacement != null) {
                iter.remove();
                if (selectorsToAdd == null) selectorsToAdd = new HashSet<SelectorName>();
                selectorsToAdd.add(replacement);
            }
        }
        if (selectorsToAdd != null) planNode.getSelectors().addAll(selectorsToAdd);

        // Now call recursively on the children ...
        for (PlanNode child : planNode) {
            replaceReferencesToRemovedSource(context, child, rewrittenSelectors);
        }
    }

    protected DynamicOperand replaceReferencesToRemovedSource( QueryContext context,
                                                               DynamicOperand operand,
                                                               Map<SelectorName, SelectorName> rewrittenSelectors ) {
        if (operand instanceof FullTextSearchScore) {
            FullTextSearchScore score = (FullTextSearchScore)operand;
            SelectorName replacement = rewrittenSelectors.get(score.getSelectorName());
            if (replacement == null) return score;
            return new FullTextSearchScore(replacement);
        }
        if (operand instanceof Length) {
            Length operation = (Length)operand;
            PropertyValue wrapped = operation.getPropertyValue();
            SelectorName replacement = rewrittenSelectors.get(wrapped.getSelectorName());
            if (replacement == null) return operand;
            return new Length(new PropertyValue(replacement, wrapped.getPropertyName()));
        }
        if (operand instanceof LowerCase) {
            LowerCase operation = (LowerCase)operand;
            SelectorName replacement = rewrittenSelectors.get(operation.getSelectorName());
            if (replacement == null) return operand;
            return new LowerCase(replaceReferencesToRemovedSource(context, operand, rewrittenSelectors));
        }
        if (operand instanceof UpperCase) {
            UpperCase operation = (UpperCase)operand;
            SelectorName replacement = rewrittenSelectors.get(operation.getSelectorName());
            if (replacement == null) return operand;
            return new UpperCase(replaceReferencesToRemovedSource(context, operand, rewrittenSelectors));
        }
        if (operand instanceof NodeName) {
            NodeName name = (NodeName)operand;
            SelectorName replacement = rewrittenSelectors.get(name.getSelectorName());
            if (replacement == null) return name;
            return new NodeName(replacement);
        }
        if (operand instanceof NodeLocalName) {
            NodeLocalName name = (NodeLocalName)operand;
            SelectorName replacement = rewrittenSelectors.get(name.getSelectorName());
            if (replacement == null) return name;
            return new NodeLocalName(replacement);
        }
        if (operand instanceof PropertyValue) {
            PropertyValue value = (PropertyValue)operand;
            SelectorName replacement = rewrittenSelectors.get(value.getSelectorName());
            if (replacement == null) return operand;
            return new PropertyValue(replacement, value.getPropertyName());
        }
        if (operand instanceof NodeDepth) {
            NodeDepth depth = (NodeDepth)operand;
            SelectorName replacement = rewrittenSelectors.get(depth.getSelectorName());
            if (replacement == null) return operand;
            return new NodeDepth(replacement);
        }
        if (operand instanceof NodePath) {
            NodePath path = (NodePath)operand;
            SelectorName replacement = rewrittenSelectors.get(path.getSelectorName());
            if (replacement == null) return operand;
            return new NodePath(replacement);
        }
        return operand;
    }

    protected Constraint replaceReferencesToRemovedSource( QueryContext context,
                                                           Constraint constraint,
                                                           Map<SelectorName, SelectorName> rewrittenSelectors ) {
        if (constraint instanceof And) {
            And and = (And)constraint;
            Constraint left = replaceReferencesToRemovedSource(context, and.getLeft(), rewrittenSelectors);
            Constraint right = replaceReferencesToRemovedSource(context, and.getRight(), rewrittenSelectors);
            if (left == and.getLeft() && right == and.getRight()) return and;
            return new And(left, right);
        }
        if (constraint instanceof Or) {
            Or or = (Or)constraint;
            Constraint left = replaceReferencesToRemovedSource(context, or.getLeft(), rewrittenSelectors);
            Constraint right = replaceReferencesToRemovedSource(context, or.getRight(), rewrittenSelectors);
            if (left == or.getLeft() && right == or.getRight()) return or;
            return new Or(left, right);
        }
        if (constraint instanceof Not) {
            Not not = (Not)constraint;
            Constraint wrapped = replaceReferencesToRemovedSource(context, not.getConstraint(), rewrittenSelectors);
            if (wrapped == not.getConstraint()) return not;
            return new Not(wrapped);
        }
        if (constraint instanceof SameNode) {
            SameNode sameNode = (SameNode)constraint;
            SelectorName replacement = rewrittenSelectors.get(sameNode.getSelectorName());
            if (replacement == null) return sameNode;
            return new SameNode(replacement, sameNode.getPath());
        }
        if (constraint instanceof ChildNode) {
            ChildNode childNode = (ChildNode)constraint;
            SelectorName replacement = rewrittenSelectors.get(childNode.getSelectorName());
            if (replacement == null) return childNode;
            return new ChildNode(replacement, childNode.getParentPath());
        }
        if (constraint instanceof DescendantNode) {
            DescendantNode descendantNode = (DescendantNode)constraint;
            SelectorName replacement = rewrittenSelectors.get(descendantNode.getSelectorName());
            if (replacement == null) return descendantNode;
            return new DescendantNode(replacement, descendantNode.getAncestorPath());
        }
        if (constraint instanceof PropertyExistence) {
            PropertyExistence existence = (PropertyExistence)constraint;
            SelectorName replacement = rewrittenSelectors.get(existence.getSelectorName());
            if (replacement == null) return existence;
            return new PropertyExistence(replacement, existence.getPropertyName());
        }
        if (constraint instanceof FullTextSearch) {
            FullTextSearch search = (FullTextSearch)constraint;
            SelectorName replacement = rewrittenSelectors.get(search.getSelectorName());
            if (replacement == null) return search;
            return new PropertyExistence(replacement, search.getPropertyName());
        }
        if (constraint instanceof Comparison) {
            Comparison comparison = (Comparison)constraint;
            DynamicOperand lhs = comparison.getOperand1();
            StaticOperand rhs = comparison.getOperand2(); // Current only a literal; therefore, no reference to selector
            DynamicOperand newLhs = replaceReferencesToRemovedSource(context, lhs, rewrittenSelectors);
            if (lhs == newLhs) return comparison;
            return new Comparison(newLhs, comparison.getOperator(), rhs);
        }
        return constraint;
    }

    protected JoinCondition replaceReferencesToRemovedSource( QueryContext context,
                                                              JoinCondition joinCondition,
                                                              Map<SelectorName, SelectorName> rewrittenSelectors ) {
        if (joinCondition instanceof EquiJoinCondition) {
            EquiJoinCondition condition = (EquiJoinCondition)joinCondition;
            SelectorName replacement1 = rewrittenSelectors.get(condition.getSelector1Name());
            SelectorName replacement2 = rewrittenSelectors.get(condition.getSelector2Name());
            if (replacement1 == condition.getSelector1Name() && replacement2 == condition.getSelector2Name()) return condition;
            return new EquiJoinCondition(replacement1, condition.getProperty1Name(), replacement2, condition.getProperty2Name());
        }
        if (joinCondition instanceof SameNodeJoinCondition) {
            SameNodeJoinCondition condition = (SameNodeJoinCondition)joinCondition;
            SelectorName replacement1 = rewrittenSelectors.get(condition.getSelector1Name());
            SelectorName replacement2 = rewrittenSelectors.get(condition.getSelector2Name());
            if (replacement1 == condition.getSelector1Name() && replacement2 == condition.getSelector2Name()) return condition;
            return new SameNodeJoinCondition(replacement1, replacement2, condition.getSelector2Path());
        }
        if (joinCondition instanceof ChildNodeJoinCondition) {
            ChildNodeJoinCondition condition = (ChildNodeJoinCondition)joinCondition;
            SelectorName childSelector = rewrittenSelectors.get(condition.getChildSelectorName());
            SelectorName parentSelector = rewrittenSelectors.get(condition.getParentSelectorName());
            if (childSelector == condition.getChildSelectorName() && parentSelector == condition.getParentSelectorName()) return condition;
            return new ChildNodeJoinCondition(parentSelector, childSelector);
        }
        if (joinCondition instanceof DescendantNodeJoinCondition) {
            DescendantNodeJoinCondition condition = (DescendantNodeJoinCondition)joinCondition;
            SelectorName ancestor = rewrittenSelectors.get(condition.getAncestorSelectorName());
            SelectorName descendant = rewrittenSelectors.get(condition.getDescendantSelectorName());
            if (ancestor == condition.getAncestorSelectorName() && descendant == condition.getDescendantSelectorName()) return condition;
            return new ChildNodeJoinCondition(ancestor, descendant);
        }
        return joinCondition;
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
