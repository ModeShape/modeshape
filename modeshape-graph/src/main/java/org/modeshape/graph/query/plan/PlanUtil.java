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
package org.modeshape.graph.query.plan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.modeshape.graph.query.QueryContext;
import org.modeshape.graph.query.model.And;
import org.modeshape.graph.query.model.ArithmeticOperand;
import org.modeshape.graph.query.model.ArithmeticOperator;
import org.modeshape.graph.query.model.Between;
import org.modeshape.graph.query.model.ChildNode;
import org.modeshape.graph.query.model.ChildNodeJoinCondition;
import org.modeshape.graph.query.model.Column;
import org.modeshape.graph.query.model.Comparison;
import org.modeshape.graph.query.model.Constraint;
import org.modeshape.graph.query.model.DescendantNode;
import org.modeshape.graph.query.model.DescendantNodeJoinCondition;
import org.modeshape.graph.query.model.DynamicOperand;
import org.modeshape.graph.query.model.EquiJoinCondition;
import org.modeshape.graph.query.model.FullTextSearch;
import org.modeshape.graph.query.model.FullTextSearchScore;
import org.modeshape.graph.query.model.JoinCondition;
import org.modeshape.graph.query.model.Length;
import org.modeshape.graph.query.model.LowerCase;
import org.modeshape.graph.query.model.NodeDepth;
import org.modeshape.graph.query.model.NodeLocalName;
import org.modeshape.graph.query.model.NodeName;
import org.modeshape.graph.query.model.NodePath;
import org.modeshape.graph.query.model.Not;
import org.modeshape.graph.query.model.Or;
import org.modeshape.graph.query.model.Ordering;
import org.modeshape.graph.query.model.PropertyExistence;
import org.modeshape.graph.query.model.PropertyValue;
import org.modeshape.graph.query.model.SameNode;
import org.modeshape.graph.query.model.SameNodeJoinCondition;
import org.modeshape.graph.query.model.SelectorName;
import org.modeshape.graph.query.model.StaticOperand;
import org.modeshape.graph.query.model.UpperCase;
import org.modeshape.graph.query.model.Visitors;
import org.modeshape.graph.query.model.Visitors.AbstractVisitor;
import org.modeshape.graph.query.plan.PlanNode.Property;
import org.modeshape.graph.query.plan.PlanNode.Type;
import org.modeshape.graph.query.validate.Schemata.Table;
import org.modeshape.graph.query.validate.Schemata.View;

/**
 * Utilities for working with {@link PlanNode}s.
 */
public class PlanUtil {

    /**
     * Collected the minimum set of columns from the supplied table that are required by or used within the plan at the supplied
     * node or above. This method first looks to see if the supplied node is a {@link Type#PROJECT} node, and if so immediately
     * returns that node's list of {@link Property#PROJECT_COLUMNS projected columns}. Otherwise, this method finds all of the
     * {@link Type#SOURCE} nodes below the supplied plan node, and accumulates the set of sources for which the columns are to be
     * found. The method then walks up the plan, looking for references to columns of the supplied table, starting with the
     * supplied node and walking up until a {@link PlanNode.Type#PROJECT PROJECT} node is found or the top of the plan is reached.
     * <p>
     * The resulting column list will always contain at the front the {@link Property#PROJECT_COLUMNS columns} from the nearest
     * PROJECT ancestor, followed by any columns required by criteria. This is done so that the processing of the nearest PROJECT
     * ancestor node can simply use the sublist of each tuple.
     * </p>
     * 
     * @param context the query context; may not be null
     * @param planNode the plan node at which the required columns need to be found; may not be null
     * @return the list of {@link Column} objects that are used in the plan; never null but possibly empty if no columns are
     *         actually needed
     */
    public static List<Column> findRequiredColumns( QueryContext context,
                                                    PlanNode planNode ) {
        List<Column> columns = null;
        PlanNode node = planNode;
        // First find the columns from the nearest PROJECT ancestor ...
        do {
            switch (node.getType()) {
                case PROJECT:
                    columns = node.getPropertyAsList(Property.PROJECT_COLUMNS, Column.class);
                    node = null;
                    break;
                default:
                    node = node.getParent();
                    break;
            }
        } while (node != null);

        // Find the names of the selectors ...
        Set<SelectorName> names = new HashSet<SelectorName>();
        for (PlanNode source : planNode.findAllAtOrBelow(Type.SOURCE)) {
            names.add(source.getProperty(Property.SOURCE_NAME, SelectorName.class));
            SelectorName alias = source.getProperty(Property.SOURCE_ALIAS, SelectorName.class);
            if (alias != null) names.add(alias);
        }

        // Add the PROJECT columns first ...
        RequiredColumnVisitor collectionVisitor = new RequiredColumnVisitor(names);
        if (columns != null) {
            for (Column projectedColumn : columns) {
                collectionVisitor.visit(projectedColumn);
            }
        }

        // Now add the columns from the JOIN, SELECT, PROJECT and SORT ancestors ...
        node = planNode;
        do {
            switch (node.getType()) {
                case JOIN:
                    Constraint criteria = node.getProperty(Property.JOIN_CONSTRAINTS, Constraint.class);
                    JoinCondition joinCondition = node.getProperty(Property.JOIN_CONDITION, JoinCondition.class);
                    Visitors.visitAll(criteria, collectionVisitor);
                    Visitors.visitAll(joinCondition, collectionVisitor);
                    break;
                case SELECT:
                    criteria = node.getProperty(Property.SELECT_CRITERIA, Constraint.class);
                    Visitors.visitAll(criteria, collectionVisitor);
                    break;
                case SORT:
                    List<Object> orderBys = node.getPropertyAsList(Property.SORT_ORDER_BY, Object.class);
                    if (orderBys != null && !orderBys.isEmpty()) {
                        if (orderBys.get(0) instanceof Ordering) {
                            for (int i = 0; i != orderBys.size(); ++i) {
                                Ordering ordering = (Ordering)orderBys.get(i);
                                Visitors.visitAll(ordering, collectionVisitor);
                            }
                        }
                    }
                    break;
                case PROJECT:
                    if (node != planNode) {
                        // Already handled above, but we can stop looking for columns ...
                        return collectionVisitor.getRequiredColumns();
                    }
                    break;
                default:
                    break;
            }
            node = node.getParent();
        } while (node != null);
        return collectionVisitor.getRequiredColumns();
    }

    protected static class RequiredColumnVisitor extends AbstractVisitor {
        private final Set<SelectorName> names;
        private final List<Column> columns = new LinkedList<Column>();
        private final Set<String> requiredColumnNames = new HashSet<String>();

        protected RequiredColumnVisitor( Set<SelectorName> names ) {
            this.names = names;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitors.AbstractVisitor#visit(org.modeshape.graph.query.model.PropertyExistence)
         */
        @Override
        public void visit( PropertyExistence existence ) {
            requireColumn(existence.getSelectorName(), existence.getPropertyName());
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitors.AbstractVisitor#visit(org.modeshape.graph.query.model.PropertyValue)
         */
        @Override
        public void visit( PropertyValue value ) {
            requireColumn(value.getSelectorName(), value.getPropertyName());
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitors.AbstractVisitor#visit(org.modeshape.graph.query.model.EquiJoinCondition)
         */
        @Override
        public void visit( EquiJoinCondition condition ) {
            requireColumn(condition.getSelector1Name(), condition.getProperty1Name());
            requireColumn(condition.getSelector2Name(), condition.getProperty2Name());
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitors.AbstractVisitor#visit(org.modeshape.graph.query.model.Column)
         */
        @Override
        public void visit( Column column ) {
            requireColumn(column.getSelectorName(), column.getPropertyName(), column.getColumnName());
        }

        protected void requireColumn( SelectorName selector,
                                      String propertyName ) {
            requireColumn(selector, propertyName, null);
        }

        protected void requireColumn( SelectorName selector,
                                      String propertyName,
                                      String alias ) {
            if (names.contains(selector)) {
                // The column is part of the table we're interested in ...
                if (alias != null && !alias.equals(propertyName)) {
                    if (requiredColumnNames.add(propertyName) && requiredColumnNames.add(alias)) {
                        columns.add(new Column(selector, propertyName, alias));
                    }
                } else {
                    if (requiredColumnNames.add(propertyName)) {
                        alias = propertyName;
                        columns.add(new Column(selector, propertyName, alias));
                    }
                }
            }
        }

        /**
         * Get the columns that are required.
         * 
         * @return the columns; never null
         */
        public List<Column> getRequiredColumns() {
            return columns;
        }
    }

    public static void replaceReferencesToRemovedSource( QueryContext context,
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

    public static DynamicOperand replaceReferencesToRemovedSource( QueryContext context,
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
            return new LowerCase(replaceReferencesToRemovedSource(context, operation.getOperand(), rewrittenSelectors));
        }
        if (operand instanceof UpperCase) {
            UpperCase operation = (UpperCase)operand;
            SelectorName replacement = rewrittenSelectors.get(operation.getSelectorName());
            if (replacement == null) return operand;
            return new UpperCase(replaceReferencesToRemovedSource(context, operation.getOperand(), rewrittenSelectors));
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

    public static Constraint replaceReferencesToRemovedSource( QueryContext context,
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
            return new FullTextSearch(replacement, search.getPropertyName(), search.getFullTextSearchExpression());
        }
        if (constraint instanceof Between) {
            Between between = (Between)constraint;
            DynamicOperand lhs = between.getOperand();
            StaticOperand lower = between.getLowerBound(); // Current only a literal; therefore, no reference to selector
            StaticOperand upper = between.getUpperBound(); // Current only a literal; therefore, no reference to selector
            DynamicOperand newLhs = replaceReferencesToRemovedSource(context, lhs, rewrittenSelectors);
            if (lhs == newLhs) return between;
            return new Between(newLhs, lower, upper, between.isLowerBoundIncluded(), between.isUpperBoundIncluded());
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

    public static JoinCondition replaceReferencesToRemovedSource( QueryContext context,
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

    public static void replaceViewReferences( QueryContext context,
                                              PlanNode topOfViewInPlan,
                                              ColumnMapping mappings ) {
        assert topOfViewInPlan != null;
        SelectorName viewName = mappings.getOriginalName();

        // Walk up the plan to change any references to the view columns into references to the source columns...
        PlanNode node = topOfViewInPlan;
        List<PlanNode> potentiallyRemovableSources = new LinkedList<PlanNode>();
        do {
            // Remove the view from the selectors ...
            if (node.getSelectors().remove(viewName)) {
                switch (node.getType()) {
                    case PROJECT:
                        // Adjust the columns ...
                        List<Column> columns = node.getPropertyAsList(Property.PROJECT_COLUMNS, Column.class);
                        if (columns != null) {
                            for (int i = 0; i != columns.size(); ++i) {
                                Column column = columns.get(i);
                                if (column.getSelectorName().equals(viewName)) {
                                    // This column references the view ...
                                    String columnName = column.getPropertyName();
                                    String columnAlias = column.getColumnName();
                                    // Find the source column that this view column corresponds to ...
                                    Column sourceColumn = mappings.getMappedColumn(columnName);
                                    if (sourceColumn != null) {
                                        SelectorName sourceName = sourceColumn.getSelectorName();
                                        // Replace the view column with one that uses the same alias but that references the
                                        // source
                                        // column ...
                                        columns.set(i, new Column(sourceName, sourceColumn.getPropertyName(), columnAlias));
                                        node.addSelector(sourceName);
                                    } else {
                                        node.addSelector(column.getSelectorName());
                                    }
                                } else {
                                    node.addSelector(column.getSelectorName());
                                }
                            }
                        }
                        break;
                    case SELECT:
                        Constraint constraint = node.getProperty(Property.SELECT_CRITERIA, Constraint.class);
                        Constraint newConstraint = replaceReferences(context, constraint, mappings, node);
                        if (constraint != newConstraint) {
                            node.getSelectors().clear();
                            node.addSelectors(Visitors.getSelectorsReferencedBy(newConstraint));
                            node.setProperty(Property.SELECT_CRITERIA, newConstraint);
                        }
                        break;
                    case SOURCE:
                        SelectorName sourceName = node.getProperty(Property.SOURCE_NAME, SelectorName.class);
                        assert sourceName.equals(sourceName); // selector name already matches
                        potentiallyRemovableSources.add(node);
                        break;
                    case JOIN:
                        JoinCondition joinCondition = node.getProperty(Property.JOIN_CONDITION, JoinCondition.class);
                        JoinCondition newJoinCondition = replaceViewReferences(context, joinCondition, mappings, node);
                        node.getSelectors().clear();
                        node.setProperty(Property.JOIN_CONDITION, newJoinCondition);
                        node.addSelectors(Visitors.getSelectorsReferencedBy(newJoinCondition));
                        List<Constraint> joinConstraints = node.getPropertyAsList(Property.JOIN_CONSTRAINTS, Constraint.class);
                        if (joinConstraints != null && !joinConstraints.isEmpty()) {
                            List<Constraint> newConstraints = new ArrayList<Constraint>(joinConstraints.size());
                            for (Constraint joinConstraint : joinConstraints) {
                                newConstraint = replaceReferences(context, joinConstraint, mappings, node);
                                newConstraints.add(newConstraint);
                                node.addSelectors(Visitors.getSelectorsReferencedBy(newConstraint));
                            }
                            node.setProperty(Property.JOIN_CONSTRAINTS, newConstraints);
                        }
                        break;
                    case ACCESS:
                        // Add all the selectors used by the subnodes ...
                        for (PlanNode child : node) {
                            node.addSelectors(child.getSelectors());
                        }
                        break;
                    case SORT:
                        // The selector names and Ordering objects needs to be changed ...
                        List<Ordering> orderings = node.getPropertyAsList(Property.SORT_ORDER_BY, Ordering.class);
                        List<Ordering> newOrderings = new ArrayList<Ordering>(orderings.size());
                        node.getSelectors().clear();
                        for (Ordering ordering : orderings) {
                            DynamicOperand operand = ordering.getOperand();
                            DynamicOperand newOperand = replaceViewReferences(context, operand, mappings, node);
                            if (newOperand != operand) {
                                ordering = new Ordering(newOperand, ordering.getOrder());
                            }
                            node.addSelectors(Visitors.getSelectorsReferencedBy(ordering));
                            newOrderings.add(ordering);
                        }
                        node.setProperty(Property.SORT_ORDER_BY, newOrderings);
                        break;
                    case GROUP:
                        // Don't yet use GROUP BY
                    case SET_OPERATION:
                    case DUP_REMOVE:
                    case LIMIT:
                    case NULL:
                        break;
                }
            }
            // Move to the parent ...
            node = node.getParent();
        } while (node != null);

        // Remove any source node that was the view but that is no longer used in any higher node ...
        for (PlanNode sourceNode : potentiallyRemovableSources) {
            boolean stillRequired = false;
            node = sourceNode.getParent();
            while (node != null) {
                if (node.getSelectors().contains(viewName)) {
                    stillRequired = true;
                    break;
                }
                node = node.getParent();
            }
            if (!stillRequired) {
                assert sourceNode.getParent() != null;
                sourceNode.extractFromParent();
            }
        }
    }

    public static Constraint replaceReferences( QueryContext context,
                                                Constraint constraint,
                                                ColumnMapping mapping,
                                                PlanNode node ) {
        if (constraint instanceof And) {
            And and = (And)constraint;
            Constraint left = replaceReferences(context, and.getLeft(), mapping, node);
            Constraint right = replaceReferences(context, and.getRight(), mapping, node);
            if (left == and.getLeft() && right == and.getRight()) return and;
            return new And(left, right);
        }
        if (constraint instanceof Or) {
            Or or = (Or)constraint;
            Constraint left = replaceReferences(context, or.getLeft(), mapping, node);
            Constraint right = replaceReferences(context, or.getRight(), mapping, node);
            if (left == or.getLeft() && right == or.getRight()) return or;
            return new Or(left, right);
        }
        if (constraint instanceof Not) {
            Not not = (Not)constraint;
            Constraint wrapped = replaceReferences(context, not.getConstraint(), mapping, node);
            return wrapped == not.getConstraint() ? not : new Not(wrapped);
        }
        if (constraint instanceof SameNode) {
            SameNode sameNode = (SameNode)constraint;
            if (!mapping.getOriginalName().equals(sameNode.getSelectorName())) return sameNode;
            if (!mapping.isMappedToSingleSelector()) return sameNode;
            SelectorName selector = mapping.getSingleMappedSelectorName();
            node.addSelector(selector);
            return new SameNode(selector, sameNode.getPath());
        }
        if (constraint instanceof ChildNode) {
            ChildNode childNode = (ChildNode)constraint;
            if (!mapping.getOriginalName().equals(childNode.getSelectorName())) return childNode;
            if (!mapping.isMappedToSingleSelector()) return childNode;
            SelectorName selector = mapping.getSingleMappedSelectorName();
            node.addSelector(selector);
            return new ChildNode(selector, childNode.getParentPath());
        }
        if (constraint instanceof DescendantNode) {
            DescendantNode descendantNode = (DescendantNode)constraint;
            if (!mapping.getOriginalName().equals(descendantNode.getSelectorName())) return descendantNode;
            if (!mapping.isMappedToSingleSelector()) return descendantNode;
            SelectorName selector = mapping.getSingleMappedSelectorName();
            node.addSelector(selector);
            return new DescendantNode(selector, descendantNode.getAncestorPath());
        }
        if (constraint instanceof PropertyExistence) {
            PropertyExistence existence = (PropertyExistence)constraint;
            if (!mapping.getOriginalName().equals(existence.getSelectorName())) return existence;
            Column sourceColumn = mapping.getMappedColumn(existence.getPropertyName());
            if (sourceColumn == null) return existence;
            node.addSelector(sourceColumn.getSelectorName());
            return new PropertyExistence(sourceColumn.getSelectorName(), sourceColumn.getPropertyName());
        }
        if (constraint instanceof FullTextSearch) {
            FullTextSearch search = (FullTextSearch)constraint;
            if (!mapping.getOriginalName().equals(search.getSelectorName())) return search;
            Column sourceColumn = mapping.getMappedColumn(search.getPropertyName());
            if (sourceColumn == null) return search;
            node.addSelector(sourceColumn.getSelectorName());
            return new FullTextSearch(sourceColumn.getSelectorName(), sourceColumn.getPropertyName(),
                                      search.getFullTextSearchExpression());
        }
        if (constraint instanceof Between) {
            Between between = (Between)constraint;
            DynamicOperand lhs = between.getOperand();
            StaticOperand lower = between.getLowerBound(); // Current only a literal; therefore, no reference to selector
            StaticOperand upper = between.getUpperBound(); // Current only a literal; therefore, no reference to selector
            DynamicOperand newLhs = replaceViewReferences(context, lhs, mapping, node);
            if (lhs == newLhs) return between;
            return new Between(newLhs, lower, upper, between.isLowerBoundIncluded(), between.isUpperBoundIncluded());
        }
        if (constraint instanceof Comparison) {
            Comparison comparison = (Comparison)constraint;
            DynamicOperand lhs = comparison.getOperand1();
            StaticOperand rhs = comparison.getOperand2(); // Current only a literal; therefore, no reference to selector
            DynamicOperand newLhs = replaceViewReferences(context, lhs, mapping, node);
            if (lhs == newLhs) return comparison;
            return new Comparison(newLhs, comparison.getOperator(), rhs);
        }
        return constraint;
    }

    public static DynamicOperand replaceViewReferences( QueryContext context,
                                                        DynamicOperand operand,
                                                        ColumnMapping mapping,
                                                        PlanNode node ) {
        if (operand instanceof ArithmeticOperand) {
            ArithmeticOperand arith = (ArithmeticOperand)operand;
            DynamicOperand newLeft = replaceViewReferences(context, arith.getLeft(), mapping, node);
            DynamicOperand newRight = replaceViewReferences(context, arith.getRight(), mapping, node);
            return new ArithmeticOperand(newLeft, arith.getOperator(), newRight);
        }
        if (operand instanceof FullTextSearchScore) {
            FullTextSearchScore score = (FullTextSearchScore)operand;
            if (!mapping.getOriginalName().equals(score.getSelectorName())) return score;
            if (mapping.isMappedToSingleSelector()) {
                return new FullTextSearchScore(mapping.getSingleMappedSelectorName());
            }
            // There are multiple mappings, so we have to create a composite score ...
            DynamicOperand composite = null;
            for (SelectorName name : mapping.getMappedSelectorNames()) {
                FullTextSearchScore mappedScore = new FullTextSearchScore(name);
                if (composite == null) {
                    composite = mappedScore;
                } else {
                    composite = new ArithmeticOperand(composite, ArithmeticOperator.ADD, mappedScore);
                }
            }
            return composite;
        }
        if (operand instanceof Length) {
            Length operation = (Length)operand;
            return new Length((PropertyValue)replaceViewReferences(context, operation.getPropertyValue(), mapping, node));
        }
        if (operand instanceof LowerCase) {
            LowerCase operation = (LowerCase)operand;
            return new LowerCase(replaceViewReferences(context, operation.getOperand(), mapping, node));
        }
        if (operand instanceof UpperCase) {
            UpperCase operation = (UpperCase)operand;
            return new UpperCase(replaceViewReferences(context, operation.getOperand(), mapping, node));
        }
        if (operand instanceof NodeName) {
            NodeName name = (NodeName)operand;
            if (!mapping.getOriginalName().equals(name.getSelectorName())) return name;
            if (!mapping.isMappedToSingleSelector()) return name;
            node.addSelector(mapping.getSingleMappedSelectorName());
            return new NodeName(mapping.getSingleMappedSelectorName());
        }
        if (operand instanceof NodeLocalName) {
            NodeLocalName name = (NodeLocalName)operand;
            if (!mapping.getOriginalName().equals(name.getSelectorName())) return name;
            if (!mapping.isMappedToSingleSelector()) return name;
            node.addSelector(mapping.getSingleMappedSelectorName());
            return new NodeLocalName(mapping.getSingleMappedSelectorName());
        }
        if (operand instanceof PropertyValue) {
            PropertyValue value = (PropertyValue)operand;
            if (!mapping.getOriginalName().equals(value.getSelectorName())) return value;
            Column sourceColumn = mapping.getMappedColumn(value.getPropertyName());
            if (sourceColumn == null) return value;
            node.addSelector(sourceColumn.getSelectorName());
            return new PropertyValue(sourceColumn.getSelectorName(), sourceColumn.getPropertyName());
        }
        if (operand instanceof NodeDepth) {
            NodeDepth depth = (NodeDepth)operand;
            if (!mapping.getOriginalName().equals(depth.getSelectorName())) return depth;
            if (!mapping.isMappedToSingleSelector()) return depth;
            node.addSelector(mapping.getSingleMappedSelectorName());
            return new NodeDepth(mapping.getSingleMappedSelectorName());
        }
        if (operand instanceof NodePath) {
            NodePath path = (NodePath)operand;
            if (!mapping.getOriginalName().equals(path.getSelectorName())) return path;
            if (!mapping.isMappedToSingleSelector()) return path;
            node.addSelector(mapping.getSingleMappedSelectorName());
            return new NodePath(mapping.getSingleMappedSelectorName());
        }
        return operand;
    }

    public static JoinCondition replaceViewReferences( QueryContext context,
                                                       JoinCondition joinCondition,
                                                       ColumnMapping mapping,
                                                       PlanNode node ) {
        if (joinCondition instanceof EquiJoinCondition) {
            EquiJoinCondition condition = (EquiJoinCondition)joinCondition;
            SelectorName replacement1 = condition.getSelector1Name();
            SelectorName replacement2 = condition.getSelector2Name();
            String property1 = condition.getProperty1Name();
            String property2 = condition.getProperty2Name();
            if (replacement1.equals(mapping.getOriginalName())) {
                Column sourceColumn = mapping.getMappedColumn(property1);
                if (sourceColumn != null) {
                    replacement1 = sourceColumn.getSelectorName();
                    property1 = sourceColumn.getPropertyName();
                }
            }
            if (replacement2.equals(mapping.getOriginalName())) {
                Column sourceColumn = mapping.getMappedColumn(property2);
                if (sourceColumn != null) {
                    replacement2 = sourceColumn.getSelectorName();
                    property2 = sourceColumn.getPropertyName();
                }
            }
            if (replacement1 == condition.getSelector1Name() && replacement2 == condition.getSelector2Name()) return condition;
            node.addSelector(replacement1, replacement2);
            return new EquiJoinCondition(replacement1, property1, replacement2, property2);
        }
        // All the remaining conditions can only be rewritten if there is a single source ...
        if (!mapping.isMappedToSingleSelector()) return joinCondition;

        // There is only a single source ...
        SelectorName viewName = mapping.getOriginalName();
        SelectorName sourceName = mapping.getSingleMappedSelectorName();

        if (joinCondition instanceof SameNodeJoinCondition) {
            SameNodeJoinCondition condition = (SameNodeJoinCondition)joinCondition;
            SelectorName replacement1 = condition.getSelector1Name();
            SelectorName replacement2 = condition.getSelector2Name();
            if (replacement1.equals(viewName)) replacement1 = sourceName;
            if (replacement2.equals(viewName)) replacement2 = sourceName;
            if (replacement1 == condition.getSelector1Name() && replacement2 == condition.getSelector2Name()) return condition;
            node.addSelector(replacement1, replacement2);
            if (condition.getSelector2Path() == null) return new SameNodeJoinCondition(replacement1, replacement2);
            return new SameNodeJoinCondition(replacement1, replacement2, condition.getSelector2Path());
        }
        if (joinCondition instanceof ChildNodeJoinCondition) {
            ChildNodeJoinCondition condition = (ChildNodeJoinCondition)joinCondition;
            SelectorName childSelector = condition.getChildSelectorName();
            SelectorName parentSelector = condition.getParentSelectorName();
            if (childSelector.equals(viewName)) childSelector = sourceName;
            if (parentSelector.equals(viewName)) parentSelector = sourceName;
            if (childSelector == condition.getChildSelectorName() && parentSelector == condition.getParentSelectorName()) return condition;
            node.addSelector(childSelector, parentSelector);
            return new ChildNodeJoinCondition(parentSelector, childSelector);
        }
        if (joinCondition instanceof DescendantNodeJoinCondition) {
            DescendantNodeJoinCondition condition = (DescendantNodeJoinCondition)joinCondition;
            SelectorName ancestor = condition.getAncestorSelectorName();
            SelectorName descendant = condition.getDescendantSelectorName();
            if (ancestor.equals(viewName)) ancestor = sourceName;
            if (descendant.equals(viewName)) descendant = sourceName;
            if (ancestor == condition.getAncestorSelectorName() && descendant == condition.getDescendantSelectorName()) return condition;
            node.addSelector(ancestor, descendant);
            return new ChildNodeJoinCondition(ancestor, descendant);
        }
        return joinCondition;
    }

    public static ColumnMapping createMappingFor( View view,
                                                  PlanNode viewPlan ) {
        ColumnMapping mapping = new ColumnMapping(view.getName());

        // Find the PROJECT node in the view plan ...
        PlanNode project = viewPlan.findAtOrBelow(Type.PROJECT);
        assert project != null;

        // Get the Columns from the PROJECT in the plan node ...
        List<Column> projectedColumns = project.getPropertyAsList(Property.PROJECT_COLUMNS, Column.class);

        // Get the Schemata columns defined by the view ...
        List<org.modeshape.graph.query.validate.Schemata.Column> viewColumns = view.getColumns();
        assert viewColumns.size() == projectedColumns.size();

        for (int i = 0; i != viewColumns.size(); ++i) {
            Column projectedColunn = projectedColumns.get(i);
            String viewColumnName = viewColumns.get(i).getName();
            mapping.map(viewColumnName, projectedColunn);
        }
        return mapping;
    }

    public static ColumnMapping createMappingForAliased( SelectorName viewAlias,
                                                         View view,
                                                         PlanNode viewPlan ) {
        ColumnMapping mapping = new ColumnMapping(viewAlias);

        // Find the PROJECT node in the view plan ...
        PlanNode project = viewPlan.findAtOrBelow(Type.PROJECT);
        assert project != null;

        // Get the Columns from the PROJECT in the plan node ...
        List<Column> projectedColumns = project.getPropertyAsList(Property.PROJECT_COLUMNS, Column.class);

        // Get the Schemata columns defined by the view ...
        List<org.modeshape.graph.query.validate.Schemata.Column> viewColumns = view.getColumns();
        assert viewColumns.size() == projectedColumns.size();

        for (int i = 0; i != viewColumns.size(); ++i) {
            Column projectedColunn = projectedColumns.get(i);
            String viewColumnName = viewColumns.get(i).getName();
            mapping.map(viewColumnName, projectedColunn);
        }
        return mapping;
    }

    public static ColumnMapping createMappingForAliased( SelectorName tableAlias,
                                                         Table table,
                                                         PlanNode tableSourceNode ) {
        ColumnMapping mapping = new ColumnMapping(tableAlias);

        // Find the projected columns on the nearest PROJECT ...
        PlanNode project = tableSourceNode.findAncestor(Type.PROJECT);
        assert project != null;

        // Get the Columns from the PROJECT in the plan node ...
        List<Column> projectedColumns = project.getPropertyAsList(Property.PROJECT_COLUMNS, Column.class);

        for (int i = 0; i != projectedColumns.size(); ++i) {
            Column projectedColumn = projectedColumns.get(i);
            Column projectedColumnInTable = projectedColumns.get(i).with(table.getName());
            org.modeshape.graph.query.validate.Schemata.Column column = table.getColumn(projectedColumnInTable.getPropertyName());
            mapping.map(column.getName(), projectedColumnInTable);
            if (projectedColumn.getColumnName() != null) {
                // The projected column has an alias, so add a mapping for it, too
                mapping.map(projectedColumn.getColumnName(), projectedColumnInTable);
            }
        }
        return mapping;
    }

    /**
     * Defines how the view columns are mapped (or resolved) into the columns from the source tables.
     */
    public static class ColumnMapping {
        private final SelectorName originalName;
        private final Map<String, Column> mappedColumnsByOriginalColumnName = new HashMap<String, Column>();
        private final Set<SelectorName> mappedSelectorNames = new LinkedHashSet<SelectorName>(); // maintains insertion order

        public ColumnMapping( SelectorName originalName ) {
            this.originalName = originalName;
        }

        public void map( String originalColumnName,
                         Column projectedColumn ) {
            mappedColumnsByOriginalColumnName.put(originalColumnName, projectedColumn);
            mappedSelectorNames.add(projectedColumn.getSelectorName());
        }

        public SelectorName getOriginalName() {
            return originalName;
        }

        public Column getMappedColumn( String viewColumnName ) {
            return mappedColumnsByOriginalColumnName.get(viewColumnName);
        }

        public boolean isMappedToSingleSelector() {
            return mappedSelectorNames.size() == 1;
        }

        /**
         * @return tableNames
         */
        public Set<SelectorName> getMappedSelectorNames() {
            return mappedSelectorNames;
        }

        public SelectorName getSingleMappedSelectorName() {
            return isMappedToSingleSelector() ? mappedSelectorNames.iterator().next() : null;
        }
    }

}
