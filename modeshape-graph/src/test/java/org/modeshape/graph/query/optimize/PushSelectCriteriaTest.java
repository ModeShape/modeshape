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
public class PushSelectCriteriaTest extends AbstractQueryTest {

    private PushSelectCriteria rule;
    private QueryContext context;

    @Before
    public void beforeEach() {
        context = new QueryContext(mock(Schemata.class), new ExecutionContext().getValueFactories().getTypeSystem());
        rule = PushSelectCriteria.INSTANCE;
    }

    /**
     * Before:
     * 
     * <pre>
     *          ...
     *           |
     *        PROJECT      with the list of columns being SELECTed
     *           |
     *        SELECT1
     *           |         One or more SELECT plan nodes that each have
     *        SELECT2      a single non-join constraint that are then all AND-ed
     *           |         together
     *        SELECTn
     *           |
     *        ACCESS
     *           |
     *        SOURCE
     * </pre>
     * 
     * And after:
     * 
     * <pre>
     *          ...
     *           |
     *        PROJECT      with the list of columns being SELECTed
     *           |
     *        ACCESS
     *           |
     *        SELECT1
     *           |         One or more SELECT plan nodes that each have
     *        SELECT2      a single non-join constraint that are then all AND-ed
     *           |         together
     *        SELECTn
     *           |
     *        SOURCE
     * </pre>
     */
    @Test
    public void shouldPushDownAllSelectNodesThatApplyToSelectorBelowAccessNodeButAboveSourceNodeUsingSameSelector() {
        // Each of the PROJECT, SELECT, and SELECT nodes must have the names of the selectors that they apply to ...
        PlanNode project = new PlanNode(Type.PROJECT, selector("Selector1"));
        PlanNode select1 = new PlanNode(Type.SELECT, project, selector("Selector1"));
        PlanNode select2 = new PlanNode(Type.SELECT, select1, selector("Selector1"));
        PlanNode select3 = new PlanNode(Type.SELECT, select2, selector("Selector1"));
        PlanNode select4 = new PlanNode(Type.SELECT, select3, selector("Selector1"));
        PlanNode access = new PlanNode(Type.ACCESS, select4, selector("Selector1"));
        PlanNode source = new PlanNode(Type.SOURCE, access, selector("Selector1"));

        // Execute the rule ...
        PlanNode result = rule.execute(context, project, new LinkedList<OptimizerRule>());
        assertThat(result, is(sameInstance(project)));
        assertChildren(project, access);
        assertChildren(access, select1);
        assertChildren(select1, select2);
        assertChildren(select2, select3);
        assertChildren(select3, select4);
        assertChildren(select4, source);
        assertChildren(source);
    }

    @Test
    public void shouldNotPushDownSelectNodesThatUseDifferentSelectorNamesThanSourceNode() {
        // Each of the PROJECT, SELECT, and SELECT nodes must have the names of the selectors that they apply to ...
        PlanNode project = new PlanNode(Type.PROJECT, selector("Selector1"));
        PlanNode select1 = new PlanNode(Type.SELECT, project, selector("Selector2"));
        PlanNode select2 = new PlanNode(Type.SELECT, select1, selector("Selector1"));
        PlanNode select3 = new PlanNode(Type.SELECT, select2, selector("Selector2"));
        PlanNode select4 = new PlanNode(Type.SELECT, select3, selector("Selector1"));
        PlanNode access = new PlanNode(Type.ACCESS, select4, selector("Selector1"));
        PlanNode source = new PlanNode(Type.SOURCE, access, selector("Selector1"));

        // Execute the rule ...
        PlanNode result = rule.execute(context, project, new LinkedList<OptimizerRule>());
        assertThat(result, is(sameInstance(project)));
        assertChildren(project, select1);
        assertChildren(select1, select3);
        assertChildren(select3, access);
        assertChildren(access, select2);
        assertChildren(select2, select4);
        assertChildren(select4, source);
        assertChildren(source);
    }

    /**
     * Before:
     * 
     * <pre>
     *          ...
     *           |
     *        PROJECT ('s1','s2')      with the list of columns being SELECTed (from 's1' and 's2' selectors)
     *           |
     *        SELECT1 ('s1')
     *           |                     One or more SELECT plan nodes that each have
     *        SELECT2 ('s2')           a single non-join constraint that are then all AND-ed
     *           |                     together, and that each have the selector(s) they apply to
     *        SELECT3 ('s1','s2')
     *           |
     *        SELECT4 ('s1')
     *           |
     *         JOIN ('s1','s2')
     *        /     \
     *       /       \
     *   ACCESS     ACCESS
     *    ('s1')    ('s2')
     *     |           |
     *   SOURCE     SOURCE
     *    ('s1')    ('s2')
     * </pre>
     * 
     * And after:
     * 
     * <pre>
     *          ...
     *           |
     *        PROJECT ('s1','s2')      with the list of columns being SELECTed (from 's1' and 's2' selectors)
     *           |
     *        SELECT3 ('s1','s2')      Any SELECT plan nodes that apply to multiple selectors are left above
     *           |                     the ACCESS nodes.
     *         JOIN ('s1','s2')
     *        /     \
     *       /       \
     *   ACCESS     ACCESS
     *   ('s1')     ('s2')
     *     |           |
     *  SELECT1     SELECT2
     *   ('s1')     ('s2')
     *     |           |
     *  SELECT4     SOURCE
     *   ('s1')     ('s2')
     *     |
     *   SOURCE
     *   ('s1')
     * </pre>
     */
    @Test
    public void shouldPushDownAllSelectNodesThatApplyToOneSelectorToBelowAccessNodeForThatSelector() {
        // Each of the PROJECT, SELECT, and SELECT nodes must have the names of the selectors that they apply to ...
        PlanNode project = new PlanNode(Type.PROJECT, selector("Selector1"), selector("Selector2"));
        PlanNode select1 = new PlanNode(Type.SELECT, project, selector("Selector1"));
        PlanNode select2 = new PlanNode(Type.SELECT, select1, selector("Selector2"));
        PlanNode select3 = new PlanNode(Type.SELECT, select2, selector("Selector1"), selector("Selector2"));
        PlanNode select4 = new PlanNode(Type.SELECT, select3, selector("Selector1"));
        PlanNode join = new PlanNode(Type.JOIN, select4, selector("Selector1"), selector("Selector2"));
        PlanNode s1Access = new PlanNode(Type.ACCESS, join, selector("Selector1"));
        PlanNode s1Source = new PlanNode(Type.SOURCE, s1Access, selector("Selector1"));
        PlanNode s2Access = new PlanNode(Type.ACCESS, join, selector("Selector2"));
        PlanNode s2Source = new PlanNode(Type.SOURCE, s2Access, selector("Selector2"));
        // Set the join type ...
        join.setProperty(Property.JOIN_TYPE, JoinType.INNER);

        // Execute the rule ...
        PlanNode result = rule.execute(context, project, new LinkedList<OptimizerRule>());

        // System.out.println(result);

        assertThat(result, is(sameInstance(project)));
        assertChildren(project, select3);
        assertChildren(select3, join);
        assertChildren(join, s1Access, s2Access);
        assertChildren(s1Access, select1);
        assertChildren(select1, select4);
        assertChildren(select4, s1Source);
        assertChildren(s2Access, select2);
        assertChildren(select2, s2Source);
        assertChildren(s2Source);
        assertChildren(s1Source);
    }
}
