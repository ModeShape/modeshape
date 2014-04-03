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
import org.modeshape.jcr.query.model.EquiJoinCondition;
import org.modeshape.jcr.query.model.JoinType;
import org.modeshape.jcr.query.plan.PlanNode;
import org.modeshape.jcr.query.plan.PlanNode.Property;
import org.modeshape.jcr.query.plan.PlanNode.Type;
import org.modeshape.jcr.query.validate.Schemata;

public class AddJoinConditionColumnsToSourcesTest extends AbstractQueryTest {

    private AddJoinConditionColumnsToSources rule;
    private QueryContext context;

    @Before
    public void beforeEach() {
        context = new QueryContext(new ExecutionContext(), mock(RepositoryCache.class), Collections.singleton("workspace"),
                                   mock(Schemata.class), mock(RepositoryIndexes.class), mock(NodeTypes.class),
                                   mock(BufferManager.class));
        rule = AddJoinConditionColumnsToSources.INSTANCE;
        context.getHints().hasJoin = true;
    }

    @Test
    public void shouldNotAddJoinConditionColumnIfAlreadyUsed() {
        // Create a join ...
        PlanNode joinNode = new PlanNode(Type.JOIN);
        joinNode.setProperty(Property.JOIN_TYPE, JoinType.LEFT_OUTER);
        joinNode.setProperty(Property.JOIN_CONDITION, new EquiJoinCondition(selector("left"), "c2", selector("right"), "d2"));
        PlanNode lhs = sourceNode(context, joinNode, "left", "c1", "c2", "c3");
        PlanNode rhs = sourceNode(context, joinNode, "right", "d1", "d2", "d3");

        // Execute the rule ...
        PlanNode result = rule.execute(context, joinNode, new LinkedList<OptimizerRule>());
        assertThat(result, is(sameInstance(joinNode)));

        // Verify nothing has changed ...
        assertThat(joinNode.getProperty(Property.JOIN_TYPE, JoinType.class), is(JoinType.LEFT_OUTER));
        assertThat(joinNode.getFirstChild(), is(sameInstance(lhs)));
        assertThat(joinNode.getLastChild(), is(sameInstance(rhs)));
        assertThat(joinNode.getChildCount(), is(2));
        PlanNode left = joinNode.getFirstChild();
        PlanNode right = joinNode.getLastChild();
        assertProperty(left, Property.PROJECT_COLUMNS, columns(context, selector("left"), "c1", "c2", "c3"));
        assertProperty(left, Property.PROJECT_COLUMN_TYPES, columnTypes(context, selector("left"), "c1", "c2", "c3"));
        assertProperty(right, Property.PROJECT_COLUMNS, columns(context, selector("right"), "d1", "d2", "d3"));
        assertProperty(right, Property.PROJECT_COLUMN_TYPES, columnTypes(context, selector("right"), "d1", "d2", "d3"));
    }

    @Test
    public void shouldNotAddJoinConditionWithChildNodesSwapped() {
        // Create a join ...
        PlanNode joinNode = new PlanNode(Type.JOIN);
        joinNode.setProperty(Property.JOIN_TYPE, JoinType.LEFT_OUTER);
        joinNode.setProperty(Property.JOIN_CONDITION, new EquiJoinCondition(selector("right"), "d2", selector("left"), "c2"));
        PlanNode lhs = sourceNode(context, joinNode, "left", "c1", "c2", "c3");
        PlanNode rhs = sourceNode(context, joinNode, "right", "d1", "d2", "d3");

        // Execute the rule ...
        PlanNode result = rule.execute(context, joinNode, new LinkedList<OptimizerRule>());
        assertThat(result, is(sameInstance(joinNode)));

        // Verify nothing has changed ...
        assertThat(joinNode.getProperty(Property.JOIN_TYPE, JoinType.class), is(JoinType.LEFT_OUTER));
        assertThat(joinNode.getFirstChild(), is(sameInstance(lhs)));
        assertThat(joinNode.getLastChild(), is(sameInstance(rhs)));
        assertThat(joinNode.getChildCount(), is(2));
        PlanNode left = joinNode.getFirstChild();
        PlanNode right = joinNode.getLastChild();
        assertProperty(left, Property.PROJECT_COLUMNS, columns(context, selector("left"), "c1", "c2", "c3"));
        assertProperty(left, Property.PROJECT_COLUMN_TYPES, columnTypes(context, selector("left"), "c1", "c2", "c3"));
        assertProperty(right, Property.PROJECT_COLUMNS, columns(context, selector("right"), "d1", "d2", "d3"));
        assertProperty(right, Property.PROJECT_COLUMN_TYPES, columnTypes(context, selector("right"), "d1", "d2", "d3"));
    }

    @Test
    public void shouldAddJoinConditionColumnOnLeftSideIfNotAlreadyUsed() {
        // Create a join ...
        PlanNode joinNode = new PlanNode(Type.JOIN);
        joinNode.setProperty(Property.JOIN_TYPE, JoinType.LEFT_OUTER);
        joinNode.setProperty(Property.JOIN_CONDITION, new EquiJoinCondition(selector("left"), "c4", selector("right"), "d2"));
        PlanNode lhs = sourceNode(context, joinNode, "left", "c1", "c2", "c3");
        PlanNode rhs = sourceNode(context, joinNode, "right", "d1", "d2", "d3");

        // Execute the rule ...
        PlanNode result = rule.execute(context, joinNode, new LinkedList<OptimizerRule>());
        assertThat(result, is(sameInstance(joinNode)));

        // Verify nothing has changed ...
        assertThat(joinNode.getProperty(Property.JOIN_TYPE, JoinType.class), is(JoinType.LEFT_OUTER));
        assertThat(joinNode.getFirstChild(), is(sameInstance(lhs)));
        assertThat(joinNode.getLastChild(), is(sameInstance(rhs)));
        assertThat(joinNode.getChildCount(), is(2));
        PlanNode left = joinNode.getFirstChild();
        PlanNode right = joinNode.getLastChild();
        assertProperty(left, Property.PROJECT_COLUMNS, columns(context, selector("left"), "c1", "c2", "c3", "c4"));
        assertProperty(left, Property.PROJECT_COLUMN_TYPES, columnTypes(context, selector("left"), "c1", "c2", "c3", "c4"));
        assertProperty(right, Property.PROJECT_COLUMNS, columns(context, selector("right"), "d1", "d2", "d3"));
        assertProperty(right, Property.PROJECT_COLUMN_TYPES, columnTypes(context, selector("right"), "d1", "d2", "d3"));
    }

    @Test
    public void shouldAddJoinConditionColumnOnRightSideIfNotAlreadyUsed() {
        // Create a join ...
        PlanNode joinNode = new PlanNode(Type.JOIN);
        joinNode.setProperty(Property.JOIN_TYPE, JoinType.LEFT_OUTER);
        joinNode.setProperty(Property.JOIN_CONDITION, new EquiJoinCondition(selector("left"), "c2", selector("right"), "d4"));
        PlanNode lhs = sourceNode(context, joinNode, "left", "c1", "c2", "c3");
        PlanNode rhs = sourceNode(context, joinNode, "right", "d1", "d2", "d3");

        // Execute the rule ...
        PlanNode result = rule.execute(context, joinNode, new LinkedList<OptimizerRule>());
        assertThat(result, is(sameInstance(joinNode)));

        // Verify nothing has changed ...
        assertThat(joinNode.getProperty(Property.JOIN_TYPE, JoinType.class), is(JoinType.LEFT_OUTER));
        assertThat(joinNode.getFirstChild(), is(sameInstance(lhs)));
        assertThat(joinNode.getLastChild(), is(sameInstance(rhs)));
        assertThat(joinNode.getChildCount(), is(2));
        PlanNode left = joinNode.getFirstChild();
        PlanNode right = joinNode.getLastChild();
        assertProperty(left, Property.PROJECT_COLUMNS, columns(context, selector("left"), "c1", "c2", "c3"));
        assertProperty(left, Property.PROJECT_COLUMN_TYPES, columnTypes(context, selector("left"), "c1", "c2", "c3"));
        assertProperty(right, Property.PROJECT_COLUMNS, columns(context, selector("right"), "d1", "d2", "d3", "d4"));
        assertProperty(right, Property.PROJECT_COLUMN_TYPES, columnTypes(context, selector("right"), "d1", "d2", "d3", "d4"));
    }

    @Test
    public void shouldAddJoinConditionColumnOnBothSidesIfNotAlreadyUsed() {
        // Create a join ...
        PlanNode joinNode = new PlanNode(Type.JOIN);
        joinNode.setProperty(Property.JOIN_TYPE, JoinType.LEFT_OUTER);
        joinNode.setProperty(Property.JOIN_CONDITION, new EquiJoinCondition(selector("left"), "c4", selector("right"), "d4"));
        PlanNode lhs = sourceNode(context, joinNode, "left", "c1", "c2", "c3");
        PlanNode rhs = sourceNode(context, joinNode, "right", "d1", "d2", "d3");

        // Execute the rule ...
        PlanNode result = rule.execute(context, joinNode, new LinkedList<OptimizerRule>());
        assertThat(result, is(sameInstance(joinNode)));

        // Verify nothing has changed ...
        assertThat(joinNode.getProperty(Property.JOIN_TYPE, JoinType.class), is(JoinType.LEFT_OUTER));
        assertThat(joinNode.getFirstChild(), is(sameInstance(lhs)));
        assertThat(joinNode.getLastChild(), is(sameInstance(rhs)));
        assertThat(joinNode.getChildCount(), is(2));
        PlanNode left = joinNode.getFirstChild();
        PlanNode right = joinNode.getLastChild();
        assertProperty(left, Property.PROJECT_COLUMNS, columns(context, selector("left"), "c1", "c2", "c3", "c4"));
        assertProperty(left, Property.PROJECT_COLUMN_TYPES, columnTypes(context, selector("left"), "c1", "c2", "c3", "c4"));
        assertProperty(right, Property.PROJECT_COLUMNS, columns(context, selector("right"), "d1", "d2", "d3", "d4"));
        assertProperty(right, Property.PROJECT_COLUMN_TYPES, columnTypes(context, selector("right"), "d1", "d2", "d3", "d4"));
    }

    @Test
    public void shouldAddJoinConditionColumnOnBothSidesIfChildrenSwappedAndIfNotAlreadyUsed() {
        // Create a join ...
        PlanNode joinNode = new PlanNode(Type.JOIN);
        joinNode.setProperty(Property.JOIN_TYPE, JoinType.LEFT_OUTER);
        joinNode.setProperty(Property.JOIN_CONDITION, new EquiJoinCondition(selector("right"), "d4", selector("left"), "c4"));
        PlanNode lhs = sourceNode(context, joinNode, "left", "c1", "c2", "c3");
        PlanNode rhs = sourceNode(context, joinNode, "right", "d1", "d2", "d3");

        // Execute the rule ...
        PlanNode result = rule.execute(context, joinNode, new LinkedList<OptimizerRule>());
        assertThat(result, is(sameInstance(joinNode)));

        // Verify nothing has changed ...
        assertThat(joinNode.getProperty(Property.JOIN_TYPE, JoinType.class), is(JoinType.LEFT_OUTER));
        assertThat(joinNode.getFirstChild(), is(sameInstance(lhs)));
        assertThat(joinNode.getLastChild(), is(sameInstance(rhs)));
        assertThat(joinNode.getChildCount(), is(2));
        PlanNode left = joinNode.getFirstChild();
        PlanNode right = joinNode.getLastChild();
        assertProperty(left, Property.PROJECT_COLUMNS, columns(context, selector("left"), "c1", "c2", "c3", "c4"));
        assertProperty(left, Property.PROJECT_COLUMN_TYPES, columnTypes(context, selector("left"), "c1", "c2", "c3", "c4"));
        assertProperty(right, Property.PROJECT_COLUMNS, columns(context, selector("right"), "d1", "d2", "d3", "d4"));
        assertProperty(right, Property.PROJECT_COLUMN_TYPES, columnTypes(context, selector("right"), "d1", "d2", "d3", "d4"));
    }
}
