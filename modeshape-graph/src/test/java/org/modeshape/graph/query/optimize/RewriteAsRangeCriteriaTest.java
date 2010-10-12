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
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import java.util.LinkedList;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.query.AbstractQueryTest;
import org.modeshape.graph.query.QueryContext;
import org.modeshape.graph.query.model.Between;
import org.modeshape.graph.query.model.Comparison;
import org.modeshape.graph.query.model.Literal;
import org.modeshape.graph.query.model.Operator;
import org.modeshape.graph.query.model.PropertyValue;
import org.modeshape.graph.query.plan.PlanNode;
import org.modeshape.graph.query.plan.PlanNode.Property;
import org.modeshape.graph.query.plan.PlanNode.Type;
import org.modeshape.graph.query.validate.Schemata;

/**
 * 
 */
public class RewriteAsRangeCriteriaTest extends AbstractQueryTest {

    private RewriteAsRangeCriteria rule;
    private LinkedList<OptimizerRule> rules;
    private QueryContext context;
    private boolean print = false;

    @Before
    public void beforeEach() {
        rule = RewriteAsRangeCriteria.INSTANCE;
        rules = new LinkedList<OptimizerRule>();
        rules.add(rule);
        context = new QueryContext(mock(Schemata.class), new ExecutionContext().getValueFactories().getTypeSystem());
        print = false;
    }

    protected void print( PlanNode node ) {
        if (print) System.out.println(node);
    }

    /**
     * Before:
     * 
     * <pre>
     * Access [t1]
     *   Project [t1]
     *     Select [t1] &lt;SELECT_CRITERIA=t1.c2 = 100&gt;
     *       Select [t1] &lt;SELECT_CRITERIA=t1.c1 &lt; 3&gt;
     *         Select [t1] &lt;SELECT_CRITERIA=t1.c1 &gt; 1&gt;
     *           Source [t1] &lt;SOURCE_NAME=t1&gt;
     * </pre>
     * 
     * And after:
     * 
     * <pre>
     * Access [t1]
     *  Project [t1]
     *    Select [t1] &lt;SELECT_CRITERIA=t1.c1 BETWEEN 1 EXCLUSIVE AND 3 EXCLUSIVE&gt;
     *      Select [t1] &lt;SELECT_CRITERIA=t1.c2 = 100&gt;
     *        Source [t1] &lt;SOURCE_NAME=t1&gt;
     * </pre>
     */
    @Test
    public void shouldReplaceComparisonsSpecifyingExclusiveRangeWithBetweenConstraint() {
        // Each of the PROJECT, SELECT, and SELECT nodes must have the names of the selectors that they apply to ...
        PlanNode access = new PlanNode(Type.ACCESS, selector("t1"));
        PlanNode project = new PlanNode(Type.PROJECT, access, selector("t1"));
        PlanNode select1 = new PlanNode(Type.SELECT, project, selector("t1"));
        PlanNode select2 = new PlanNode(Type.SELECT, select1, selector("t1"));
        PlanNode select3 = new PlanNode(Type.SELECT, select2, selector("t1"));
        PlanNode source = new PlanNode(Type.SOURCE, select3, selector("t1"));
        source.setProperty(Property.SOURCE_NAME, selector("t1"));
        select1.setProperty(Property.SELECT_CRITERIA, new Comparison(new PropertyValue(selector("t1"), "c2"), Operator.EQUAL_TO,
                                                                     new Literal(100L)));
        select2.setProperty(Property.SELECT_CRITERIA, new Comparison(new PropertyValue(selector("t1"), "c1"), Operator.LESS_THAN,
                                                                     new Literal(3L)));
        select3.setProperty(Property.SELECT_CRITERIA, new Comparison(new PropertyValue(selector("t1"), "c1"),
                                                                     Operator.GREATER_THAN, new Literal(1L)));

        // Execute the rule ...
        print(access);
        PlanNode result = executeRules(access);
        print(result);

        // Compare results ...
        assertThat(result, is(sameInstance(access)));
        assertChildren(access, project);
        PlanNode newSelect = project.getFirstChild();
        assertThat(newSelect.getType(), is(Type.SELECT));
        assertThat(newSelect.getSelectors(), is(access.getSelectors()));
        assertThat(newSelect.getParent(), is(sameInstance(project)));
        Between between = newSelect.getProperty(Property.SELECT_CRITERIA, Between.class);
        assertThat(between.operand(), is(select2.getProperty(Property.SELECT_CRITERIA, Comparison.class).operand1()));
        assertThat(between.lowerBound(), is(select3.getProperty(Property.SELECT_CRITERIA, Comparison.class).operand2()));
        assertThat(between.upperBound(), is(select2.getProperty(Property.SELECT_CRITERIA, Comparison.class).operand2()));
        assertThat(between.isLowerBoundIncluded(), is(false));
        assertThat(between.isUpperBoundIncluded(), is(false));
        assertChildren(newSelect, select1);
        assertChildren(select1, source);
    }

    /**
     * Before:
     * 
     * <pre>
     * Access [t1]
     *   Project [t1]
     *     Select [t1] &lt;SELECT_CRITERIA=t1.c2 = 100&gt;
     *       Select [t1] &lt;SELECT_CRITERIA=t1.c1 &lt;= 3&gt;
     *         Select [t1] &lt;SELECT_CRITERIA=t1.c1 &gt;= 1&gt;
     *           Source [t1] &lt;SOURCE_NAME=t1&gt;
     * </pre>
     * 
     * And after:
     * 
     * <pre>
     * Access [t1]
     *  Project [t1]
     *    Select [t1] &lt;SELECT_CRITERIA=t1.c1 BETWEEN 1 AND 3&gt;
     *      Select [t1] &lt;SELECT_CRITERIA=t1.c2 = 100&gt;
     *        Source [t1] &lt;SOURCE_NAME=t1&gt;
     * </pre>
     */
    @Test
    public void shouldReplaceComparisonsSpecifyingInclusiveRangeWithBetweenConstraint() {
        // Each of the PROJECT, SELECT, and SELECT nodes must have the names of the selectors that they apply to ...
        PlanNode access = new PlanNode(Type.ACCESS, selector("t1"));
        PlanNode project = new PlanNode(Type.PROJECT, access, selector("t1"));
        PlanNode select1 = new PlanNode(Type.SELECT, project, selector("t1"));
        PlanNode select2 = new PlanNode(Type.SELECT, select1, selector("t1"));
        PlanNode select3 = new PlanNode(Type.SELECT, select2, selector("t1"));
        PlanNode source = new PlanNode(Type.SOURCE, select3, selector("t1"));
        source.setProperty(Property.SOURCE_NAME, selector("t1"));
        select1.setProperty(Property.SELECT_CRITERIA, new Comparison(new PropertyValue(selector("t1"), "c2"), Operator.EQUAL_TO,
                                                                     new Literal(100L)));
        select2.setProperty(Property.SELECT_CRITERIA, new Comparison(new PropertyValue(selector("t1"), "c1"),
                                                                     Operator.LESS_THAN_OR_EQUAL_TO, new Literal(3L)));
        select3.setProperty(Property.SELECT_CRITERIA, new Comparison(new PropertyValue(selector("t1"), "c1"),
                                                                     Operator.GREATER_THAN_OR_EQUAL_TO, new Literal(1L)));

        // Execute the rule ...
        print(access);
        PlanNode result = executeRules(access);
        print(result);

        // Compare results ...
        assertThat(result, is(sameInstance(access)));
        assertChildren(access, project);
        PlanNode newSelect = project.getFirstChild();
        assertThat(newSelect.getType(), is(Type.SELECT));
        assertThat(newSelect.getSelectors(), is(access.getSelectors()));
        assertThat(newSelect.getParent(), is(sameInstance(project)));
        Between between = newSelect.getProperty(Property.SELECT_CRITERIA, Between.class);
        assertThat(between.operand(), is(select2.getProperty(Property.SELECT_CRITERIA, Comparison.class).operand1()));
        assertThat(between.lowerBound(), is(select3.getProperty(Property.SELECT_CRITERIA, Comparison.class).operand2()));
        assertThat(between.upperBound(), is(select2.getProperty(Property.SELECT_CRITERIA, Comparison.class).operand2()));
        assertThat(between.isLowerBoundIncluded(), is(true));
        assertThat(between.isUpperBoundIncluded(), is(true));
        assertChildren(newSelect, select1);
        assertChildren(select1, source);
    }

    /**
     * Before:
     * 
     * <pre>
     * Access [t1]
     *   Project [t1]
     *     Select [t1] &lt;SELECT_CRITERIA=t1.c2 = 100&gt;
     *       Select [t1] &lt;SELECT_CRITERIA=t1.c1 &gt; 3&gt;
     *         Select [t1] &lt;SELECT_CRITERIA=t1.c1 &lt; 1&gt;
     *           Source [t1] &lt;SOURCE_NAME=t1&gt;
     * </pre>
     * 
     * And after:
     * 
     * <pre>
     * Access [t1] &lt;ACCESS_NO_RESULTS=true&gt;
     *   Project [t1]
     *     Select [t1] &lt;SELECT_CRITERIA=t1.c2 = 100&gt;
     *       Select [t1] &lt;SELECT_CRITERIA=t1.c1 &gt; 3&gt;
     *         Select [t1] &lt;SELECT_CRITERIA=t1.c1 &lt; 1&gt;
     *           Source [t1] &lt;SOURCE_NAME=t1&gt;
     * </pre>
     */
    @Test
    public void shouldReplaceComparisonsSpecifyingExclusiveRangeWithNotBetweenConstraint() {
        // Each of the PROJECT, SELECT, and SELECT nodes must have the names of the selectors that they apply to ...
        PlanNode access = new PlanNode(Type.ACCESS, selector("t1"));
        PlanNode project = new PlanNode(Type.PROJECT, access, selector("t1"));
        PlanNode select1 = new PlanNode(Type.SELECT, project, selector("t1"));
        PlanNode select2 = new PlanNode(Type.SELECT, select1, selector("t1"));
        PlanNode select3 = new PlanNode(Type.SELECT, select2, selector("t1"));
        PlanNode source = new PlanNode(Type.SOURCE, select3, selector("t1"));
        source.setProperty(Property.SOURCE_NAME, selector("t1"));
        select1.setProperty(Property.SELECT_CRITERIA, new Comparison(new PropertyValue(selector("t1"), "c2"), Operator.EQUAL_TO,
                                                                     new Literal(100L)));
        select2.setProperty(Property.SELECT_CRITERIA, new Comparison(new PropertyValue(selector("t1"), "c1"),
                                                                     Operator.GREATER_THAN, new Literal(3L)));
        select3.setProperty(Property.SELECT_CRITERIA, new Comparison(new PropertyValue(selector("t1"), "c1"), Operator.LESS_THAN,
                                                                     new Literal(1L)));

        // Execute the rule ...
        print(access);
        PlanNode result = executeRules(access);
        print(result);

        // Compare results ...
        assertThat(result, is(sameInstance(access)));
        assertChildren(access, project);
        assertThat(access.getProperty(Property.ACCESS_NO_RESULTS, Boolean.class), is(true));
    }

    /**
     * Before:
     * 
     * <pre>
     * Access [t1]
     *   Project [t1]
     *     Select [t1] &lt;SELECT_CRITERIA=t1.c2 = 100&gt;
     *       Select [t1] &lt;SELECT_CRITERIA=t1.c1 &gt;= 3&gt;
     *         Select [t1] &lt;SELECT_CRITERIA=t1.c1 &lt;= 1&gt;
     *           Source [t1] &lt;SOURCE_NAME=t1&gt;
     * </pre>
     * 
     * And after:
     * 
     * <pre>
     * Access [t1] &lt;ACCESS_NO_RESULTS=true&gt;
     *   Project [t1]
     *     Select [t1] &lt;SELECT_CRITERIA=t1.c2 = 100&gt;
     *       Select [t1] &lt;SELECT_CRITERIA=t1.c1 &gt;= 3&gt;
     *         Select [t1] &lt;SELECT_CRITERIA=t1.c1 &lt;= 1&gt;
     *           Source [t1] &lt;SOURCE_NAME=t1&gt;
     * </pre>
     */
    @Test
    public void shouldReplaceComparisonsSpecifyingInclusiveRangeWithNotBetweenConstraint() {
        // Each of the PROJECT, SELECT, and SELECT nodes must have the names of the selectors that they apply to ...
        PlanNode access = new PlanNode(Type.ACCESS, selector("t1"));
        PlanNode project = new PlanNode(Type.PROJECT, access, selector("t1"));
        PlanNode select1 = new PlanNode(Type.SELECT, project, selector("t1"));
        PlanNode select2 = new PlanNode(Type.SELECT, select1, selector("t1"));
        PlanNode select3 = new PlanNode(Type.SELECT, select2, selector("t1"));
        PlanNode source = new PlanNode(Type.SOURCE, select3, selector("t1"));
        source.setProperty(Property.SOURCE_NAME, selector("t1"));
        select1.setProperty(Property.SELECT_CRITERIA, new Comparison(new PropertyValue(selector("t1"), "c2"), Operator.EQUAL_TO,
                                                                     new Literal(100L)));
        select2.setProperty(Property.SELECT_CRITERIA, new Comparison(new PropertyValue(selector("t1"), "c1"),
                                                                     Operator.GREATER_THAN_OR_EQUAL_TO, new Literal(3L)));
        select3.setProperty(Property.SELECT_CRITERIA, new Comparison(new PropertyValue(selector("t1"), "c1"),
                                                                     Operator.LESS_THAN_OR_EQUAL_TO, new Literal(1L)));

        // Execute the rule ...
        print(access);
        PlanNode result = executeRules(access);
        print(result);

        // Compare results ...
        assertThat(result, is(sameInstance(access)));
        assertChildren(access, project);
        assertThat(access.getProperty(Property.ACCESS_NO_RESULTS, Boolean.class), is(true));
    }

    /**
     * Before:
     * 
     * <pre>
     * Access [t1]
     *   Project [t1]
     *     Select [t1] &lt;SELECT_CRITERIA=t1.c2 = 100&gt;
     *       Select [t1] &lt;SELECT_CRITERIA=t1.c1 &lt;= 3&gt;
     *         Select [t1] &lt;SELECT_CRITERIA=t1.c1 &gt;= 3&gt;
     *           Source [t1] &lt;SOURCE_NAME=t1&gt;
     * </pre>
     * 
     * And after:
     * 
     * <pre>
     * Access [t1]
     *  Project [t1]
     *    Select [t1] &lt;SELECT_CRITERIA=t1.c1 = 3&gt;
     *      Select [t1] &lt;SELECT_CRITERIA=t1.c2 = 100&gt;
     *        Source [t1] &lt;SOURCE_NAME=t1&gt;
     * </pre>
     */
    @Test
    public void shouldReplaceComparisonsSpecifyingInclusiveRangeWithOverlappingBoundaryEqualityComparison() {
        // Each of the PROJECT, SELECT, and SELECT nodes must have the names of the selectors that they apply to ...
        PlanNode access = new PlanNode(Type.ACCESS, selector("t1"));
        PlanNode project = new PlanNode(Type.PROJECT, access, selector("t1"));
        PlanNode select1 = new PlanNode(Type.SELECT, project, selector("t1"));
        PlanNode select2 = new PlanNode(Type.SELECT, select1, selector("t1"));
        PlanNode select3 = new PlanNode(Type.SELECT, select2, selector("t1"));
        PlanNode source = new PlanNode(Type.SOURCE, select3, selector("t1"));
        source.setProperty(Property.SOURCE_NAME, selector("t1"));
        select1.setProperty(Property.SELECT_CRITERIA, new Comparison(new PropertyValue(selector("t1"), "c2"), Operator.EQUAL_TO,
                                                                     new Literal(100L)));
        select2.setProperty(Property.SELECT_CRITERIA, new Comparison(new PropertyValue(selector("t1"), "c1"),
                                                                     Operator.LESS_THAN_OR_EQUAL_TO, new Literal(3L)));
        select3.setProperty(Property.SELECT_CRITERIA, new Comparison(new PropertyValue(selector("t1"), "c1"),
                                                                     Operator.GREATER_THAN_OR_EQUAL_TO, new Literal(3L)));

        // Execute the rule ...
        print(access);
        PlanNode result = executeRules(access);
        print(result);

        // Compare results ...
        assertThat(result, is(sameInstance(access)));
        assertThat(access.getProperty(Property.ACCESS_NO_RESULTS, Boolean.class), is(nullValue()));
        assertChildren(access, project);
        PlanNode newSelect = project.getFirstChild();
        assertThat(newSelect.getType(), is(Type.SELECT));
        assertThat(newSelect.getSelectors(), is(access.getSelectors()));
        assertThat(newSelect.getParent(), is(sameInstance(project)));
        Comparison equality = newSelect.getProperty(Property.SELECT_CRITERIA, Comparison.class);
        assertThat(equality.operand1(), is(select2.getProperty(Property.SELECT_CRITERIA, Comparison.class).operand1()));
        assertThat(equality.operator(), is(Operator.EQUAL_TO));
        assertThat(equality.operand2(), is(select2.getProperty(Property.SELECT_CRITERIA, Comparison.class).operand2()));
        assertChildren(newSelect, select1);
        assertChildren(select1, source);
    }

    /**
     * Before:
     * 
     * <pre>
     * Access [t1]
     *   Project [t1]
     *     Select [t1] &lt;SELECT_CRITERIA=t1.c2 = 100&gt;
     *       Select [t1] &lt;SELECT_CRITERIA=t1.c1 &lt; 3&gt;
     *         Select [t1] &lt;SELECT_CRITERIA=t1.c1 &gt; 3&gt;
     *           Source [t1] &lt;SOURCE_NAME=t1&gt;
     * </pre>
     * 
     * And after:
     * 
     * <pre>
     * Access [t1] &lt;ACCESS_NO_RESULTS=true&gt;
     *   Project [t1]
     *     Select [t1] &lt;SELECT_CRITERIA=t1.c2 = 100&gt;
     *       Select [t1] &lt;SELECT_CRITERIA=t1.c1 &lt; 3&gt;
     *         Select [t1] &lt;SELECT_CRITERIA=t1.c1 &gt; 3&gt;
     *           Source [t1] &lt;SOURCE_NAME=t1&gt;
     * </pre>
     */
    @Test
    public void shouldMarkAsHavingNoResultsWhenComparisonsSpecifyRangeWithNonOverlappingBoundary() {
        // Each of the PROJECT, SELECT, and SELECT nodes must have the names of the selectors that they apply to ...
        PlanNode access = new PlanNode(Type.ACCESS, selector("t1"));
        PlanNode project = new PlanNode(Type.PROJECT, access, selector("t1"));
        PlanNode select1 = new PlanNode(Type.SELECT, project, selector("t1"));
        PlanNode select2 = new PlanNode(Type.SELECT, select1, selector("t1"));
        PlanNode select3 = new PlanNode(Type.SELECT, select2, selector("t1"));
        PlanNode source = new PlanNode(Type.SOURCE, select3, selector("t1"));
        source.setProperty(Property.SOURCE_NAME, selector("t1"));
        select1.setProperty(Property.SELECT_CRITERIA, new Comparison(new PropertyValue(selector("t1"), "c2"), Operator.EQUAL_TO,
                                                                     new Literal(100L)));
        select2.setProperty(Property.SELECT_CRITERIA, new Comparison(new PropertyValue(selector("t1"), "c1"), Operator.LESS_THAN,
                                                                     new Literal(3L)));
        select3.setProperty(Property.SELECT_CRITERIA, new Comparison(new PropertyValue(selector("t1"), "c1"),
                                                                     Operator.GREATER_THAN, new Literal(3L)));

        // Execute the rule ...
        print(access);
        PlanNode result = executeRules(access);
        print(result);

        // Compare results ...
        assertThat(result, is(sameInstance(access)));
        assertChildren(access, project);
        assertThat(access.getProperty(Property.ACCESS_NO_RESULTS, Boolean.class), is(true));
    }

    protected PlanNode executeRules( PlanNode node ) {
        while (!rules.isEmpty()) {
            OptimizerRule rule = rules.poll();
            assert rule != null;
            node = rule.execute(context, node, rules);
        }
        return node;
    }
}
