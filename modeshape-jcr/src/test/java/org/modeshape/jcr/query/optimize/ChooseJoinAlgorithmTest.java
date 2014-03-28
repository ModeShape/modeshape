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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import java.util.Collections;
import java.util.LinkedList;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.NodeTypes;
import org.modeshape.jcr.RepositoryIndexes;
import org.modeshape.jcr.cache.RepositoryCache;
import org.modeshape.jcr.query.AbstractQueryTest;
import org.modeshape.jcr.query.BufferManager;
import org.modeshape.jcr.query.QueryContext;
import org.modeshape.jcr.query.model.ChildNodeJoinCondition;
import org.modeshape.jcr.query.model.DescendantNodeJoinCondition;
import org.modeshape.jcr.query.model.JoinCondition;
import org.modeshape.jcr.query.model.JoinType;
import org.modeshape.jcr.query.plan.JoinAlgorithm;
import org.modeshape.jcr.query.plan.PlanNode;
import org.modeshape.jcr.query.plan.PlanNode.Property;
import org.modeshape.jcr.query.plan.PlanNode.Type;
import org.modeshape.jcr.query.validate.Schemata;

/**
 * 
 */
public class ChooseJoinAlgorithmTest extends AbstractQueryTest {

    private ChooseJoinAlgorithm bestRule;
    private ChooseJoinAlgorithm nestedRule;
    private QueryContext context;

    @Before
    public void beforeEach() {
        context = new QueryContext(new ExecutionContext(), mock(RepositoryCache.class), Collections.singleton("workspace"),
                                   mock(Schemata.class), mock(RepositoryIndexes.class), mock(NodeTypes.class),
                                   mock(BufferManager.class));
        bestRule = ChooseJoinAlgorithm.USE_BEST_JOIN_ALGORITHM;
        nestedRule = ChooseJoinAlgorithm.USE_ONLY_NESTED_JOIN_ALGORITHM;
    }

    /**
     * The {@link ChooseJoinAlgorithm#USE_ONLY_NESTED_JOIN_ALGORITHM} instance will convert this simple tree:
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
     */
    @Test
    public void shouldHaveNestedRuleAlwaysSetJoinAlgorithmToNestedLoop() {
        PlanNode join = new PlanNode(Type.JOIN, selector("Selector1"), selector("Selector2"));
        PlanNode s1Source = new PlanNode(Type.SOURCE, join, selector("Selector1"));
        PlanNode s2Source = new PlanNode(Type.SOURCE, join, selector("Selector2"));
        // Set the join type ...
        join.setProperty(Property.JOIN_TYPE, JoinType.INNER);

        // Execute the rule ...
        PlanNode result = nestedRule.execute(context, join, new LinkedList<OptimizerRule>());
        assertThat(result, is(sameInstance(join)));
        assertThat(join.getProperty(Property.JOIN_TYPE, JoinType.class), is(JoinType.INNER));
        assertThat(join.getProperty(Property.JOIN_ALGORITHM, JoinAlgorithm.class), is(JoinAlgorithm.NESTED_LOOP));
        assertChildren(join, s1Source, s2Source);
    }

    @Test
    public void shouldHaveBestRuleAlwaysSetJoinAlgorithmToNestedLoopIfConditionIsDescendantNode() {
        PlanNode join = new PlanNode(Type.JOIN, selector("Ancestor"), selector("Descendant"));
        PlanNode ancestorSource = new PlanNode(Type.SOURCE, join, selector("Ancestor"));
        PlanNode descendantSource = new PlanNode(Type.SOURCE, join, selector("Descendant"));
        // Set the join type and condition ...
        JoinCondition joinCondition = new DescendantNodeJoinCondition(selector("Ancestor"), selector("Descendant"));
        join.setProperty(Property.JOIN_CONDITION, joinCondition);
        join.setProperty(Property.JOIN_TYPE, JoinType.INNER);

        // Execute the rule ...
        PlanNode result = bestRule.execute(context, join, new LinkedList<OptimizerRule>());
        assertThat(result, is(sameInstance(join)));
        assertThat(join.getProperty(Property.JOIN_TYPE, JoinType.class), is(JoinType.INNER));
        assertThat(join.getProperty(Property.JOIN_ALGORITHM, JoinAlgorithm.class), is(JoinAlgorithm.NESTED_LOOP));
        assertThat(join.getProperty(Property.JOIN_CONDITION, JoinCondition.class), is(sameInstance(joinCondition)));
        assertChildren(join, ancestorSource, descendantSource);
    }

    @Test
    public void shouldHaveBestRuleSetJoinAlgorithmToMergeIfConditionIsNotDescendantNode() {
        PlanNode join = new PlanNode(Type.JOIN, selector("Parent"), selector("Child"));
        PlanNode parentSource = new PlanNode(Type.SOURCE, join, selector("Parent"));
        PlanNode childSource = new PlanNode(Type.SOURCE, join, selector("Child"));
        // Set the join type and condition ...
        JoinCondition joinCondition = new ChildNodeJoinCondition(selector("Parent"), selector("Child"));
        join.setProperty(Property.JOIN_CONDITION, joinCondition);
        join.setProperty(Property.JOIN_TYPE, JoinType.INNER);

        // Execute the rule ...
        PlanNode result = bestRule.execute(context, join, new LinkedList<OptimizerRule>());
        assertThat(result, is(sameInstance(join)));
        assertThat(join.getProperty(Property.JOIN_TYPE, JoinType.class), is(JoinType.INNER));
        assertThat(join.getProperty(Property.JOIN_ALGORITHM, JoinAlgorithm.class), is(JoinAlgorithm.MERGE));
        assertThat(join.getProperty(Property.JOIN_CONDITION, JoinCondition.class), is(sameInstance(joinCondition)));

        PlanNode leftDup = join.getFirstChild();
        assertThat(leftDup.getType(), is(Type.DUP_REMOVE));
        assertSelectors(leftDup, "Parent");
        PlanNode leftSort = leftDup.getFirstChild();
        assertThat(leftSort.getType(), is(Type.SORT));
        assertSortOrderBy(leftSort, "Parent");
        assertSelectors(leftSort, "Parent");
        assertChildren(leftDup, leftSort);
        assertChildren(leftSort, parentSource);

        PlanNode rightDup = join.getLastChild();
        assertThat(rightDup.getType(), is(Type.DUP_REMOVE));
        assertSelectors(rightDup, "Child");
        PlanNode rightSort = rightDup.getLastChild();
        assertThat(rightSort.getType(), is(Type.SORT));
        assertSortOrderBy(rightSort, "Child");
        assertSelectors(rightSort, "Child");
        assertChildren(rightDup, rightSort);
        assertChildren(rightSort, childSource);

        assertChildren(join, leftDup, rightDup);
    }
}
