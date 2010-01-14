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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import java.util.LinkedList;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.query.AbstractQueryTest;
import org.modeshape.graph.query.QueryContext;
import org.modeshape.graph.query.model.ChildNodeJoinCondition;
import org.modeshape.graph.query.model.DescendantNodeJoinCondition;
import org.modeshape.graph.query.model.JoinCondition;
import org.modeshape.graph.query.model.JoinType;
import org.modeshape.graph.query.plan.JoinAlgorithm;
import org.modeshape.graph.query.plan.PlanNode;
import org.modeshape.graph.query.plan.PlanNode.Property;
import org.modeshape.graph.query.plan.PlanNode.Type;
import org.modeshape.graph.query.validate.Schemata;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class ChooseJoinAlgorithmTest extends AbstractQueryTest {

    private ChooseJoinAlgorithm bestRule;
    private ChooseJoinAlgorithm nestedRule;
    private QueryContext context;

    @Before
    public void beforeEach() {
        context = new QueryContext(mock(Schemata.class), new ExecutionContext().getValueFactories().getTypeSystem());
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
