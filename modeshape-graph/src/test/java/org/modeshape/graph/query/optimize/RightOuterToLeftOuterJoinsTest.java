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
import org.modeshape.graph.query.model.JoinType;
import org.modeshape.graph.query.plan.PlanNode;
import org.modeshape.graph.query.plan.PlanNode.Property;
import org.modeshape.graph.query.plan.PlanNode.Type;
import org.modeshape.graph.query.validate.Schemata;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class RightOuterToLeftOuterJoinsTest extends AbstractQueryTest {

    private RightOuterToLeftOuterJoins rule;
    private QueryContext context;

    @Before
    public void beforeEach() {
        context = new QueryContext(mock(Schemata.class), new ExecutionContext().getValueFactories().getTypeSystem());
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
