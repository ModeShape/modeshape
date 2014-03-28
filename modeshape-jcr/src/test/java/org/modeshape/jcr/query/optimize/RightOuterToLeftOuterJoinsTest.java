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
import org.modeshape.jcr.query.model.JoinType;
import org.modeshape.jcr.query.plan.PlanNode;
import org.modeshape.jcr.query.plan.PlanNode.Property;
import org.modeshape.jcr.query.plan.PlanNode.Type;
import org.modeshape.jcr.query.validate.Schemata;

/**
 * 
 */
public class RightOuterToLeftOuterJoinsTest extends AbstractQueryTest {

    private RightOuterToLeftOuterJoins rule;
    private QueryContext context;

    @Before
    public void beforeEach() {
        context = new QueryContext(new ExecutionContext(), mock(RepositoryCache.class), Collections.singleton("workspace"),
                                   mock(Schemata.class), mock(RepositoryIndexes.class), mock(NodeTypes.class),
                                   mock(BufferManager.class));
        rule = RightOuterToLeftOuterJoins.INSTANCE;
    }

    @Test
    public void shouldDoNothingWithLeftOuterJoin() {
        // Create a LEFT_OUTER join ...
        PlanNode joinNode = new PlanNode(Type.JOIN);
        joinNode.setProperty(Property.JOIN_TYPE, JoinType.LEFT_OUTER);
        PlanNode lhs = new PlanNode(Type.SOURCE, joinNode);
        PlanNode rhs = new PlanNode(Type.SOURCE, joinNode);

        // Execute the rule ...
        PlanNode result = rule.execute(context, joinNode, new LinkedList<OptimizerRule>());
        assertThat(result, is(sameInstance(joinNode)));

        // Verify nothing has changed ...
        assertThat(joinNode.getProperty(Property.JOIN_TYPE, JoinType.class), is(JoinType.LEFT_OUTER));
        assertThat(joinNode.getFirstChild(), is(sameInstance(lhs)));
        assertThat(joinNode.getLastChild(), is(sameInstance(rhs)));
        assertThat(joinNode.getChildCount(), is(2));
    }

    @Test
    public void shouldDoNothingWithCrossJoin() {
        // Create a LEFT_OUTER join ...
        PlanNode joinNode = new PlanNode(Type.JOIN);
        joinNode.setProperty(Property.JOIN_TYPE, JoinType.CROSS);
        PlanNode lhs = new PlanNode(Type.SOURCE, joinNode);
        PlanNode rhs = new PlanNode(Type.SOURCE, joinNode);

        // Execute the rule ...
        PlanNode result = rule.execute(context, joinNode, new LinkedList<OptimizerRule>());
        assertThat(result, is(sameInstance(joinNode)));

        // Verify nothing has changed ...
        assertThat(joinNode.getProperty(Property.JOIN_TYPE, JoinType.class), is(JoinType.CROSS));
        assertThat(joinNode.getFirstChild(), is(sameInstance(lhs)));
        assertThat(joinNode.getLastChild(), is(sameInstance(rhs)));
        assertThat(joinNode.getChildCount(), is(2));
    }

    @Test
    public void shouldDoNothingWithFullOuterJoin() {
        // Create a LEFT_OUTER join ...
        PlanNode joinNode = new PlanNode(Type.JOIN);
        joinNode.setProperty(Property.JOIN_TYPE, JoinType.FULL_OUTER);
        PlanNode lhs = new PlanNode(Type.SOURCE, joinNode);
        PlanNode rhs = new PlanNode(Type.SOURCE, joinNode);

        // Execute the rule ...
        PlanNode result = rule.execute(context, joinNode, new LinkedList<OptimizerRule>());
        assertThat(result, is(sameInstance(joinNode)));

        // Verify nothing has changed ...
        assertThat(joinNode.getProperty(Property.JOIN_TYPE, JoinType.class), is(JoinType.FULL_OUTER));
        assertThat(joinNode.getFirstChild(), is(sameInstance(lhs)));
        assertThat(joinNode.getLastChild(), is(sameInstance(rhs)));
        assertThat(joinNode.getChildCount(), is(2));
    }

    @Test
    public void shouldDoNothingWithInnerJoin() {
        // Create a LEFT_OUTER join ...
        PlanNode joinNode = new PlanNode(Type.JOIN);
        joinNode.setProperty(Property.JOIN_TYPE, JoinType.INNER);
        PlanNode lhs = new PlanNode(Type.SOURCE, joinNode);
        PlanNode rhs = new PlanNode(Type.SOURCE, joinNode);

        // Execute the rule ...
        PlanNode result = rule.execute(context, joinNode, new LinkedList<OptimizerRule>());
        assertThat(result, is(sameInstance(joinNode)));

        // Verify nothing has changed ...
        assertThat(joinNode.getProperty(Property.JOIN_TYPE, JoinType.class), is(JoinType.INNER));
        assertThat(joinNode.getFirstChild(), is(sameInstance(lhs)));
        assertThat(joinNode.getLastChild(), is(sameInstance(rhs)));
        assertThat(joinNode.getChildCount(), is(2));
    }

    @Test
    public void shouldChangeRightOuterJoinToLeftOuterJoinAndReverseChildNodes() {
        // Create a RIGHT_OUTER join ...
        PlanNode joinNode = new PlanNode(Type.JOIN);
        joinNode.setProperty(Property.JOIN_TYPE, JoinType.RIGHT_OUTER);
        PlanNode lhs = new PlanNode(Type.SOURCE, joinNode);
        PlanNode rhs = new PlanNode(Type.SOURCE, joinNode);

        // Execute the rule ...
        PlanNode result = rule.execute(context, joinNode, new LinkedList<OptimizerRule>());
        assertThat(result, is(sameInstance(joinNode)));

        // Verify the change ...
        assertThat(joinNode.getProperty(Property.JOIN_TYPE, JoinType.class), is(JoinType.LEFT_OUTER));
        assertThat(joinNode.getFirstChild(), is(sameInstance(rhs)));
        assertThat(joinNode.getLastChild(), is(sameInstance(lhs)));
        assertThat(joinNode.getChildCount(), is(2));
    }

}
