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
import org.modeshape.jcr.cache.RepositoryCache;
import org.modeshape.jcr.query.AbstractQueryTest;
import org.modeshape.jcr.query.QueryContext;
import org.modeshape.jcr.query.model.JoinType;
import org.modeshape.jcr.query.plan.PlanNode;
import org.modeshape.jcr.query.plan.PlanNode.Property;
import org.modeshape.jcr.query.plan.PlanNode.Type;
import org.modeshape.jcr.query.validate.Schemata;

/**
 * 
 */
public class PushSelectCriteriaTest extends AbstractQueryTest {

    private PushSelectCriteria rule;
    private QueryContext context;

    @Before
    public void beforeEach() {
        context = new QueryContext(new ExecutionContext(), mock(RepositoryCache.class), Collections.singleton("workspace"),
                                   mock(Schemata.class));
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
