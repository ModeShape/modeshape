/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.jcr.query.optimize;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.jcr.query.QueryContext;
import org.modeshape.jcr.query.model.ChildNodeJoinCondition;
import org.modeshape.jcr.query.model.DescendantNodeJoinCondition;
import org.modeshape.jcr.query.model.DynamicOperand;
import org.modeshape.jcr.query.model.EquiJoinCondition;
import org.modeshape.jcr.query.model.JoinCondition;
import org.modeshape.jcr.query.model.NullOrder;
import org.modeshape.jcr.query.model.Order;
import org.modeshape.jcr.query.model.Ordering;
import org.modeshape.jcr.query.model.PropertyValue;
import org.modeshape.jcr.query.model.SameNodeJoinCondition;
import org.modeshape.jcr.query.model.SelectorName;
import org.modeshape.jcr.query.plan.JoinAlgorithm;
import org.modeshape.jcr.query.plan.PlanNode;
import org.modeshape.jcr.query.plan.PlanNode.Property;
import org.modeshape.jcr.query.plan.PlanNode.Type;

/**
 * An {@link OptimizerRule optimizer rule} that choose the appropriate join algorithm and sets up any prerequisites, based upon
 * the {@link JoinCondition}.
 * <p>
 * There are two static instances that can be used (or the equivalent can be instantiated or subclassed using the constructor):
 * one that only uses {@link JoinAlgorithm#NESTED_LOOP nested-loop}, and another that will attempt to use
 * {@link JoinAlgorithm#MERGE merge} where possible. Both instances ignore any existing {@link Property#JOIN_ALGORITHM} property
 * value set on the JOIN node.
 * </p>
 * <p>
 * For example, the {@link #USE_ONLY_NESTED_JOIN_ALGORITHM} instance will convert this simple tree:
 * 
 * <pre>
 *          ...
 *           |
 *         JOIN
 *        /     \
 *      ...     ...
 * </pre>
 * 
 * into this:
 * 
 * <pre>
 *          ...
 *           |
 *         JOIN ({@link Property#JOIN_ALGORITHM JOIN_ALGORITHM}={@link JoinAlgorithm#NESTED_LOOP NESTED_LOOP})
 *        /     \
 *      ...     ...
 * </pre>
 * 
 * On the other hand, the {@link #USE_BEST_JOIN_ALGORITHM} instance will do a couple of different things, depending upon the input
 * plan.
 * <ol>
 * <li>If the condition is a {@link DescendantNodeJoinCondition}, then the join algorithm will always be
 * {@link JoinAlgorithm#NESTED_LOOP}.</li>
 * <li>Otherwise, the rule will use the {@link JoinAlgorithm#MERGE} algorithm and will change this structure:
 * 
 * <pre>
 *          ...
 *           |
 *         JOIN
 *        /     \
 *      ...     ...
 * </pre>
 * 
 * into this:
 * 
 * <pre>
 *          ...
 *           |
 *         JOIN ({@link Property#JOIN_ALGORITHM JOIN_ALGORITHM}={@link JoinAlgorithm#MERGE MERGE})
 *        /     \
 *       /       \
 * DUP_REMOVE  DUP_REMOVE
 *     |           |
 *    SORT       SORT
 *     |           |
 *    ...         ...
 * </pre>
 * 
 * </li>
 * </ol>
 * </p>
 */
@Immutable
public class ChooseJoinAlgorithm implements OptimizerRule {

    public static final ChooseJoinAlgorithm USE_ONLY_NESTED_JOIN_ALGORITHM = new ChooseJoinAlgorithm(true);
    public static final ChooseJoinAlgorithm USE_BEST_JOIN_ALGORITHM = new ChooseJoinAlgorithm(false);

    private final boolean useOnlyNested;

    protected ChooseJoinAlgorithm( boolean useOnlyNested ) {
        this.useOnlyNested = useOnlyNested;
    }

    @Override
    public PlanNode execute( QueryContext context,
                             PlanNode plan,
                             LinkedList<OptimizerRule> ruleStack ) {
        // For each of the JOIN nodes ...
        for (PlanNode joinNode : plan.findAllAtOrBelow(Type.JOIN)) {
            JoinCondition condition = joinNode.getProperty(Property.JOIN_CONDITION, JoinCondition.class);
            if (useOnlyNested) {
                joinNode.setProperty(Property.JOIN_ALGORITHM, JoinAlgorithm.NESTED_LOOP);
                break;
            }

            if (condition instanceof DescendantNodeJoinCondition) {
                // It has to be a nest-loop join ...
                joinNode.setProperty(Property.JOIN_ALGORITHM, JoinAlgorithm.NESTED_LOOP);
            } else {
                joinNode.setProperty(Property.JOIN_ALGORITHM, JoinAlgorithm.MERGE);
                assert joinNode.getChildCount() == 2;

                // We can try to use the merge join, but we need to sort and remove remove duplicates ...
                // on the left and right children of the join ...
                Set<SelectorName> leftSelectors = joinNode.getFirstChild().getSelectors();
                Set<SelectorName> rightSelectors = joinNode.getLastChild().getSelectors();
                List<Object> leftSortBy = new LinkedList<Object>();
                List<Object> rightSortBy = new LinkedList<Object>();
                createOrderBysForJoinCondition(condition, leftSelectors, leftSortBy, rightSelectors, rightSortBy);

                PlanNode leftSort = new PlanNode(Type.SORT, leftSelectors);
                leftSort.setProperty(Property.SORT_ORDER_BY, leftSortBy);
                joinNode.getFirstChild().insertAsParent(leftSort);
                if (joinNode.getFirstChild().findAllAtOrBelow(Type.DUP_REMOVE).isEmpty()) {
                    // There is no duplicate removal below the left-hand side of the join, so insert it ...
                    PlanNode leftDupRemoval = new PlanNode(Type.DUP_REMOVE, leftSelectors);
                    joinNode.getFirstChild().insertAsParent(leftDupRemoval);
                }

                // There is no sort below the right-hand side of the join, so insert it ...
                PlanNode rightSort = new PlanNode(Type.SORT, rightSelectors);
                rightSort.setProperty(Property.SORT_ORDER_BY, rightSortBy);
                joinNode.getLastChild().insertAsParent(rightSort);
                if (joinNode.getLastChild().findAllAtOrBelow(Type.DUP_REMOVE).isEmpty()) {
                    // There is no duplicate removal below the right-hand side of the join, so insert it ...
                    PlanNode rightDupRemoval = new PlanNode(Type.DUP_REMOVE, rightSelectors);
                    joinNode.getLastChild().insertAsParent(rightDupRemoval);
                }
            }
        }
        return plan;
    }

    protected void createOrderBysForJoinCondition( JoinCondition condition,
                                                   Set<SelectorName> leftSelectors,
                                                   List<Object> leftSortBy,
                                                   Set<SelectorName> rightSelectors,
                                                   List<Object> rightSortBy ) {
        if (condition instanceof SameNodeJoinCondition) {
            SameNodeJoinCondition joinCondition = (SameNodeJoinCondition)condition;
            SelectorName name1 = joinCondition.selector1Name();
            SelectorName name2 = joinCondition.selector2Name();
            if (leftSelectors.contains(name1)) {
                leftSortBy.add(name1);
                rightSortBy.add(name2);
            } else {
                leftSortBy.add(name2);
                rightSortBy.add(name1);
            }
        } else if (condition instanceof ChildNodeJoinCondition) {
            ChildNodeJoinCondition joinCondition = (ChildNodeJoinCondition)condition;
            SelectorName childName = joinCondition.childSelectorName();
            SelectorName parentName = joinCondition.parentSelectorName();
            if (leftSelectors.contains(childName)) {
                leftSortBy.add(childName);
                rightSortBy.add(parentName);
            } else {
                leftSortBy.add(parentName);
                rightSortBy.add(childName);
            }
        } else if (condition instanceof EquiJoinCondition) {
            EquiJoinCondition joinCondition = (EquiJoinCondition)condition;
            SelectorName selector1 = joinCondition.selector1Name();
            SelectorName selector2 = joinCondition.selector2Name();
            String property1 = joinCondition.getProperty1Name();
            String property2 = joinCondition.getProperty2Name();

            // Create the Ordering for the first selector/property pair ...
            DynamicOperand operand1 = new PropertyValue(selector1, property1);
            Ordering ordering1 = new Ordering(operand1, Order.ASCENDING, NullOrder.NULLS_LAST);
            // Create the Ordering for the second selector/property pair ...
            DynamicOperand operand2 = new PropertyValue(selector2, property2);
            Ordering ordering2 = new Ordering(operand2, Order.ASCENDING, NullOrder.NULLS_LAST);

            if (leftSelectors.contains(selector1)) {
                leftSortBy.add(ordering1);
                rightSortBy.add(ordering2);
            } else {
                leftSortBy.add(ordering2);
                rightSortBy.add(ordering1);
            }
        } else {
            assert false;
            throw new IllegalArgumentException();
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
