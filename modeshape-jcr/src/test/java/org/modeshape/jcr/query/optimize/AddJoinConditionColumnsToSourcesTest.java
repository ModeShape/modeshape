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
package org.modeshape.jcr.query.optimize;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import java.util.LinkedList;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.cache.NodeCache;
import org.modeshape.jcr.query.AbstractQueryTest;
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
        context = new QueryContext(new ExecutionContext(), mock(NodeCache.class), "workspace", mock(Schemata.class));
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
