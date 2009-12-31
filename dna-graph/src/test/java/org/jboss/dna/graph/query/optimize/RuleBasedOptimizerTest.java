/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.graph.query.optimize;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.jboss.dna.common.collection.Problems;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.GraphI18n;
import org.jboss.dna.graph.query.AbstractQueryTest;
import org.jboss.dna.graph.query.QueryContext;
import org.jboss.dna.graph.query.model.Column;
import org.jboss.dna.graph.query.model.Comparison;
import org.jboss.dna.graph.query.model.EquiJoinCondition;
import org.jboss.dna.graph.query.model.FullTextSearch;
import org.jboss.dna.graph.query.model.JoinType;
import org.jboss.dna.graph.query.model.Literal;
import org.jboss.dna.graph.query.model.Operator;
import org.jboss.dna.graph.query.model.PropertyValue;
import org.jboss.dna.graph.query.model.QueryCommand;
import org.jboss.dna.graph.query.model.SelectorName;
import org.jboss.dna.graph.query.model.SetCriteria;
import org.jboss.dna.graph.query.model.TypeSystem;
import org.jboss.dna.graph.query.parse.SqlQueryParser;
import org.jboss.dna.graph.query.plan.CanonicalPlanner;
import org.jboss.dna.graph.query.plan.JoinAlgorithm;
import org.jboss.dna.graph.query.plan.PlanHints;
import org.jboss.dna.graph.query.plan.PlanNode;
import org.jboss.dna.graph.query.plan.PlanNode.Property;
import org.jboss.dna.graph.query.plan.PlanNode.Type;
import org.jboss.dna.graph.query.validate.ImmutableSchemata;
import org.jboss.dna.graph.query.validate.Schemata;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class RuleBasedOptimizerTest extends AbstractQueryTest {

    private RuleBasedOptimizer optimizer;
    private List<OptimizerRule> rules;
    private List<Integer> ruleExecutionOrder;
    private QueryContext context;
    private PlanNode node;
    private final boolean print = false;

    @Before
    public void beforeEach() {
        TypeSystem typeSystem = new ExecutionContext().getValueFactories().getTypeSystem();
        ImmutableSchemata.Builder builder = ImmutableSchemata.createBuilder(typeSystem);
        builder.addTable("t1", "c11", "c12", "c13");
        builder.addTable("t2", "c21", "c22", "c23");
        builder.addTable("all", "a1", "a2", "a3", "a4", "primaryType", "mixins");
        builder.makeSearchable("all", "a2");
        builder.makeSearchable("all", "a1");
        builder.addKey("all", "a1");
        builder.addKey("all", "a3");
        builder.addView("v1", "SELECT c11, c12 AS c2 FROM t1 WHERE c13 < CAST('3' AS LONG)");
        builder.addView("v2", "SELECT t1.c11, t1.c12, t2.c23 FROM t1 JOIN t2 ON t1.c11 = t2.c21");
        builder.addView("type1",
                        "SELECT all.a1, all.a2 FROM all WHERE all.primaryType IN ('t1','t0') AND all.mixins IN ('t3','t4')");
        builder.addView("type2",
                        "SELECT all.a3, all.a4 FROM all WHERE all.primaryType IN ('t2','t0') AND all.mixins IN ('t4','t5')");
        Schemata schemata = builder.build();
        context = new QueryContext(schemata, typeSystem);

        node = new PlanNode(Type.ACCESS);

        ruleExecutionOrder = new ArrayList<Integer>();
        rules = new ArrayList<OptimizerRule>();

        // Add rules that, when executed, add their number to the 'ruleExecutionOrder' list ...
        for (int i = 0; i != 5; ++i) {
            final int ruleNumber = i;
            this.rules.add(new OptimizerRule() {
                @SuppressWarnings( "synthetic-access" )
                public PlanNode execute( QueryContext context,
                                         PlanNode plan,
                                         LinkedList<OptimizerRule> ruleStack ) {
                    ruleExecutionOrder.add(ruleNumber);
                    return plan;
                }
            });
        }

        // Create a rule-based optimizer that uses a stack of completely artificial mock rules ...
        this.optimizer = new RuleBasedOptimizer() {
            @SuppressWarnings( "synthetic-access" )
            @Override
            protected void populateRuleStack( LinkedList<OptimizerRule> ruleStack,
                                              PlanHints hints ) {
                ruleStack.addAll(rules);
            }
        };
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Test the rule stack logic
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldExecuteEachRuleInSequence() {
        optimizer.optimize(context, node);
        for (int i = 0; i != rules.size(); ++i) {
            assertThat(ruleExecutionOrder.get(i), is(i));
        }
    }

    @Test
    public void shouldStopExecutingRulesIfThereIsAnErrorInTheProblems() {
        // Change of the rules to generate an error ...
        this.rules.set(3, new OptimizerRule() {
            public PlanNode execute( QueryContext context,
                                     PlanNode plan,
                                     LinkedList<OptimizerRule> ruleStack ) {
                context.getProblems().addError(GraphI18n.errorReadingPropertyValueBytes);
                return plan;
            }
        });

        optimizer.optimize(context, node);
        assertThat(ruleExecutionOrder.get(0), is(0));
        assertThat(ruleExecutionOrder.get(1), is(1));
        assertThat(ruleExecutionOrder.get(2), is(2));
        assertThat(ruleExecutionOrder.size(), is(3));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Test the actual rules
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldOptimizePlanForSimpleQueryWithSelectColumns() {
        node = optimize("SELECT c11,c12 FROM t1");
        // Create the expected plan ...
        PlanNode expected = new PlanNode(Type.ACCESS, selector("t1"));
        PlanNode project = new PlanNode(Type.PROJECT, expected, selector("t1"));
        project.setProperty(Property.PROJECT_COLUMNS, columns(column("t1", "c11"), column("t1", "c12")));
        PlanNode source = new PlanNode(Type.SOURCE, project, selector("t1"));
        source.setProperty(Property.SOURCE_NAME, selector("t1"));
        source.setProperty(Property.SOURCE_COLUMNS, context.getSchemata().getTable(selector("t1")).getColumns());
        // Compare the expected and actual plan ...
        assertThat(node.isSameAs(expected), is(true));
    }

    @Test
    public void shouldOptimizePlanForSimpleQueryWithSelectStar() {
        node = optimize("SELECT * FROM t1");
        // Create the expected plan ...
        PlanNode expected = new PlanNode(Type.ACCESS, selector("t1"));
        PlanNode project = new PlanNode(Type.PROJECT, expected, selector("t1"));
        project.setProperty(Property.PROJECT_COLUMNS, columns(column("t1", "c11"), column("t1", "c12"), column("t1", "c13")));
        PlanNode source = new PlanNode(Type.SOURCE, project, selector("t1"));
        source.setProperty(Property.SOURCE_NAME, selector("t1"));
        source.setProperty(Property.SOURCE_COLUMNS, context.getSchemata().getTable(selector("t1")).getColumns());
        // Compare the expected and actual plan ...
        assertThat(node.isSameAs(expected), is(true));
    }

    @Test
    public void shouldOptimizePlanForSimpleQueryWithSelectStarWithAlias() {
        node = optimize("SELECT * FROM t1 AS x1");
        // Create the expected plan ...
        PlanNode expected = new PlanNode(Type.ACCESS, selector("x1"));
        PlanNode project = new PlanNode(Type.PROJECT, expected, selector("x1"));
        project.setProperty(Property.PROJECT_COLUMNS, columns(column("x1", "c11"), column("x1", "c12"), column("x1", "c13")));
        PlanNode source = new PlanNode(Type.SOURCE, project, selector("x1"));
        source.setProperty(Property.SOURCE_NAME, selector("t1"));
        source.setProperty(Property.SOURCE_ALIAS, selector("x1"));
        source.setProperty(Property.SOURCE_COLUMNS, context.getSchemata().getTable(selector("t1")).getColumns());
        // Compare the expected and actual plan ...
        assertThat(node.isSameAs(expected), is(true));
    }

    @Test
    public void shouldOptimizePlanForSimpleQueryWithSelectStarFromTableWithAliasAndValueCriteria() {
        node = optimize("SELECT * FROM t1 AS x1 WHERE c13 < CAST('3' AS LONG)");
        // Create the expected plan ...
        PlanNode expected = new PlanNode(Type.ACCESS, selector("x1"));
        PlanNode project = new PlanNode(Type.PROJECT, expected, selector("x1"));
        project.setProperty(Property.PROJECT_COLUMNS, columns(column("x1", "c11"), column("x1", "c12"), column("x1", "c13")));
        PlanNode select = new PlanNode(Type.SELECT, project, selector("x1"));
        select.setProperty(Property.SELECT_CRITERIA, new Comparison(new PropertyValue(selector("x1"), "c13"), Operator.LESS_THAN,
                                                                    new Literal(3L)));
        PlanNode source = new PlanNode(Type.SOURCE, select, selector("x1"));
        source.setProperty(Property.SOURCE_NAME, selector("t1"));
        source.setProperty(Property.SOURCE_ALIAS, selector("x1"));
        source.setProperty(Property.SOURCE_COLUMNS, context.getSchemata().getTable(selector("t1")).getColumns());
        // Compare the expected and actual plan ...
        assertThat(node.isSameAs(expected), is(true));
    }

    @Test
    public void shouldOptimizePlanForSimpleQueryWithSelectStarFromViewWithNoAliasAndValueCriteria() {
        node = optimize("SELECT * FROM v1 WHERE c11 = 'value'");
        // Create the expected plan ...
        PlanNode expected = new PlanNode(Type.ACCESS, selector("t1"));
        PlanNode project = new PlanNode(Type.PROJECT, expected, selector("t1"));
        project.setProperty(Property.PROJECT_COLUMNS, columns(column("t1", "c11"), column("t1", "c12", "c2")));
        PlanNode select1 = new PlanNode(Type.SELECT, project, selector("t1"));
        select1.setProperty(Property.SELECT_CRITERIA, new Comparison(new PropertyValue(selector("t1"), "c11"), Operator.EQUAL_TO,
                                                                     new Literal("value")));
        PlanNode select2 = new PlanNode(Type.SELECT, select1, selector("t1"));
        select2.setProperty(Property.SELECT_CRITERIA, new Comparison(new PropertyValue(selector("t1"), "c13"),
                                                                     Operator.LESS_THAN, new Literal(3L)));
        PlanNode source = new PlanNode(Type.SOURCE, select2, selector("t1"));
        source.setProperty(Property.SOURCE_NAME, selector("t1"));
        source.setProperty(Property.SOURCE_COLUMNS, context.getSchemata().getTable(selector("t1")).getColumns());
        // Compare the expected and actual plan ...
        assertThat(node.isSameAs(expected), is(true));
    }

    @Test
    public void shouldOptimizePlanForSimpleQueryWithSelectStarFromViewWithAliasAndValueCriteria() {
        node = optimize("SELECT * FROM v1 AS x1 WHERE c11 = 'value'");
        // Create the expected plan ...
        PlanNode expected = new PlanNode(Type.ACCESS, selector("t1"));
        PlanNode project = new PlanNode(Type.PROJECT, expected, selector("t1"));
        project.setProperty(Property.PROJECT_COLUMNS, columns(column("t1", "c11"), column("t1", "c12", "c2")));
        PlanNode select1 = new PlanNode(Type.SELECT, project, selector("t1"));
        select1.setProperty(Property.SELECT_CRITERIA, new Comparison(new PropertyValue(selector("t1"), "c11"), Operator.EQUAL_TO,
                                                                     new Literal("value")));
        PlanNode select2 = new PlanNode(Type.SELECT, select1, selector("t1"));
        select2.setProperty(Property.SELECT_CRITERIA, new Comparison(new PropertyValue(selector("t1"), "c13"),
                                                                     Operator.LESS_THAN, new Literal(3L)));
        PlanNode source = new PlanNode(Type.SOURCE, select2, selector("t1"));
        source.setProperty(Property.SOURCE_NAME, selector("t1"));
        source.setProperty(Property.SOURCE_COLUMNS, context.getSchemata().getTable(selector("t1")).getColumns());
        // Compare the expected and actual plan ...
        assertThat(node.isSameAs(expected), is(true));
    }

    @Test
    public void shouldOptimizePlanForSimpleQueryWithPropertyValueCriteria() {
        node = optimize("SELECT c11, c12 FROM t1 WHERE c13 < CAST('3' AS LONG)");
        // Create the expected plan ...
        PlanNode expected = new PlanNode(Type.ACCESS, selector("t1"));
        PlanNode project = new PlanNode(Type.PROJECT, expected, selector("t1"));
        project.setProperty(Property.PROJECT_COLUMNS, columns(column("t1", "c11"), column("t1", "c12")));
        PlanNode select = new PlanNode(Type.SELECT, project, selector("t1"));
        select.setProperty(Property.SELECT_CRITERIA, new Comparison(new PropertyValue(selector("t1"), "c13"), Operator.LESS_THAN,
                                                                    new Literal(3L)));
        PlanNode source = new PlanNode(Type.SOURCE, select, selector("t1"));
        source.setProperty(Property.SOURCE_NAME, selector("t1"));
        source.setProperty(Property.SOURCE_COLUMNS, context.getSchemata().getTable(selector("t1")).getColumns());
        // Compare the expected and actual plan ...
        assertThat(node.isSameAs(expected), is(true));
    }

    @Test
    public void shouldOptimizePlanForEquiJoinQuery() {
        node = optimize("SELECT t1.c11, t1.c12, t2.c23 FROM t1 JOIN t2 ON t1.c11 = t2.c21");

        // Create the expected plan ...
        PlanNode project = new PlanNode(Type.PROJECT, selector("t2"), selector("t1"));
        project.setProperty(Property.PROJECT_COLUMNS, columns(column("t1", "c11"), column("t1", "c12"), column("t2", "c23")));
        PlanNode join = new PlanNode(Type.JOIN, project, selector("t2"), selector("t1"));
        join.setProperty(Property.JOIN_ALGORITHM, JoinAlgorithm.NESTED_LOOP);
        join.setProperty(Property.JOIN_TYPE, JoinType.INNER);
        join.setProperty(Property.JOIN_CONDITION, new EquiJoinCondition(selector("t1"), "c11", selector("t2"), "c21"));

        PlanNode leftAccess = new PlanNode(Type.ACCESS, join, selector("t1"));
        PlanNode leftProject = new PlanNode(Type.PROJECT, leftAccess, selector("t1"));
        leftProject.setProperty(Property.PROJECT_COLUMNS, columns(column("t1", "c11"), column("t1", "c12")));
        PlanNode leftSource = new PlanNode(Type.SOURCE, leftProject, selector("t1"));
        leftSource.setProperty(Property.SOURCE_NAME, selector("t1"));
        leftSource.setProperty(Property.SOURCE_COLUMNS, context.getSchemata().getTable(selector("t1")).getColumns());

        PlanNode rightAccess = new PlanNode(Type.ACCESS, join, selector("t2"));
        PlanNode rightProject = new PlanNode(Type.PROJECT, rightAccess, selector("t2"));
        rightProject.setProperty(Property.PROJECT_COLUMNS, columns(column("t2", "c23"), column("t2", "c21")));
        PlanNode rightSource = new PlanNode(Type.SOURCE, rightProject, selector("t2"));
        rightSource.setProperty(Property.SOURCE_NAME, selector("t2"));
        rightSource.setProperty(Property.SOURCE_COLUMNS, context.getSchemata().getTable(selector("t2")).getColumns());

        // Compare the expected and actual plan ...
        assertThat(node.isSameAs(project), is(true));
    }

    @Test
    public void shouldOptimizePlanForQueryUsingView() {
        node = optimize("SELECT v1.c11 AS c1 FROM v1 WHERE v1.c11 = 'x' AND v1.c2 = 'y'");

        // Create the expected plan ...
        PlanNode access = new PlanNode(Type.ACCESS, selector("t1"));
        PlanNode project = new PlanNode(Type.PROJECT, access, selector("t1"));
        project.setProperty(Property.PROJECT_COLUMNS, columns(column("t1", "c11", "c1")));
        PlanNode select1 = new PlanNode(Type.SELECT, project, selector("t1"));
        select1.setProperty(Property.SELECT_CRITERIA, new Comparison(new PropertyValue(selector("t1"), "c11"), Operator.EQUAL_TO,
                                                                     new Literal('x')));
        PlanNode select2 = new PlanNode(Type.SELECT, select1, selector("t1"));
        select2.setProperty(Property.SELECT_CRITERIA, new Comparison(new PropertyValue(selector("t1"), "c12"), Operator.EQUAL_TO,
                                                                     new Literal('y')));
        PlanNode select3 = new PlanNode(Type.SELECT, select2, selector("t1"));
        select3.setProperty(Property.SELECT_CRITERIA, new Comparison(new PropertyValue(selector("t1"), "c13"),
                                                                     Operator.LESS_THAN, new Literal(3L)));
        PlanNode source = new PlanNode(Type.SOURCE, select3, selector("t1"));
        source.setProperty(Property.SOURCE_NAME, selector("t1"));
        source.setProperty(Property.SOURCE_COLUMNS, context.getSchemata().getTable(selector("t1")).getColumns());

        // Compare the expected and actual plan ...
        assertThat(node.isSameAs(access), is(true));
    }

    @Test
    public void shouldOptimizePlanForQueryUsingViewContainingJoin() {
        node = optimize("SELECT v2.c11 AS c1 FROM v2 WHERE v2.c11 = 'x' AND v2.c12 = 'y'");

        // Create the expected plan ...
        PlanNode project = new PlanNode(Type.PROJECT, selector("t1"));
        project.setProperty(Property.PROJECT_COLUMNS, columns(column("t1", "c11", "c1")));
        PlanNode join = new PlanNode(Type.JOIN, project, selector("t2"), selector("t1"));
        join.setProperty(Property.JOIN_ALGORITHM, JoinAlgorithm.NESTED_LOOP);
        join.setProperty(Property.JOIN_TYPE, JoinType.INNER);
        join.setProperty(Property.JOIN_CONDITION, new EquiJoinCondition(selector("t1"), "c11", selector("t2"), "c21"));

        PlanNode leftAccess = new PlanNode(Type.ACCESS, join, selector("t1"));
        PlanNode leftProject = new PlanNode(Type.PROJECT, leftAccess, selector("t1"));
        leftProject.setProperty(Property.PROJECT_COLUMNS, columns(column("t1", "c11")));
        PlanNode leftSelect1 = new PlanNode(Type.SELECT, leftProject, selector("t1"));
        leftSelect1.setProperty(Property.SELECT_CRITERIA, new Comparison(new PropertyValue(selector("t1"), "c11"),
                                                                         Operator.EQUAL_TO, new Literal('x')));
        PlanNode leftSelect2 = new PlanNode(Type.SELECT, leftSelect1, selector("t1"));
        leftSelect2.setProperty(Property.SELECT_CRITERIA, new Comparison(new PropertyValue(selector("t1"), "c12"),
                                                                         Operator.EQUAL_TO, new Literal('y')));
        PlanNode leftSource = new PlanNode(Type.SOURCE, leftSelect2, selector("t1"));
        leftSource.setProperty(Property.SOURCE_NAME, selector("t1"));
        leftSource.setProperty(Property.SOURCE_COLUMNS, context.getSchemata().getTable(selector("t1")).getColumns());

        PlanNode rightAccess = new PlanNode(Type.ACCESS, join, selector("t2"));
        PlanNode rightProject = new PlanNode(Type.PROJECT, rightAccess, selector("t2"));
        rightProject.setProperty(Property.PROJECT_COLUMNS, columns(column("t2", "c21")));
        PlanNode rightSelect1 = new PlanNode(Type.SELECT, rightProject, selector("t2"));
        rightSelect1.setProperty(Property.SELECT_CRITERIA, new Comparison(new PropertyValue(selector("t2"), "c21"),
                                                                          Operator.EQUAL_TO, new Literal('x')));
        PlanNode rightSource = new PlanNode(Type.SOURCE, rightSelect1, selector("t2"));
        rightSource.setProperty(Property.SOURCE_NAME, selector("t2"));
        rightSource.setProperty(Property.SOURCE_COLUMNS, context.getSchemata().getTable(selector("t2")).getColumns());

        // Compare the expected and actual plan ...
        assertThat(node.isSameAs(project), is(true));
    }

    @Test
    public void shouldOptimizePlanForQueryUsingTypeView() {
        node = optimize("SELECT type1.a1 AS a, type1.a2 AS b FROM type1 WHERE CONTAINS(type1.a2,'something')");

        // Create the expected plan ...
        PlanNode access = new PlanNode(Type.ACCESS, selector("all"));
        PlanNode project = new PlanNode(Type.PROJECT, access, selector("all"));
        project.setProperty(Property.PROJECT_COLUMNS, columns(column("all", "a1", "a"), column("all", "a2", "b")));
        PlanNode select1 = new PlanNode(Type.SELECT, project, selector("all"));
        select1.setProperty(Property.SELECT_CRITERIA, new FullTextSearch(selector("all"), "a2", "something"));
        PlanNode select2 = new PlanNode(Type.SELECT, select1, selector("all"));
        select2.setProperty(Property.SELECT_CRITERIA, new SetCriteria(new PropertyValue(selector("all"), "primaryType"),
                                                                      new Literal("t1"), new Literal("t0")));
        PlanNode select3 = new PlanNode(Type.SELECT, select2, selector("all"));
        select3.setProperty(Property.SELECT_CRITERIA, new SetCriteria(new PropertyValue(selector("all"), "mixins"),
                                                                      new Literal("t3"), new Literal("t4")));
        PlanNode source = new PlanNode(Type.SOURCE, select3, selector("all"));
        source.setProperty(Property.SOURCE_NAME, selector("all"));
        source.setProperty(Property.SOURCE_COLUMNS, context.getSchemata().getTable(selector("all")).getColumns());

        // Compare the expected and actual plan ...
        assertThat(node.isSameAs(access), is(true));
    }

    @Test
    public void shouldOptimizePlanForQueryJoiningMultipleTypeViewsUsingIdentityEquiJoin() {
        node = optimize("SELECT type1.a1 AS a, type1.a2 AS b, type2.a3 as c, type2.a4 as d "
                        + "FROM type1 JOIN type2 ON type1.a1 = type2.a3 WHERE CONTAINS(type1.a2,'something')");

        // Create the expected plan ...
        PlanNode access = new PlanNode(Type.ACCESS, selector("all"));
        PlanNode project = new PlanNode(Type.PROJECT, access, selector("all"));
        project.setProperty(Property.PROJECT_COLUMNS, columns(column("all", "a1", "a"),
                                                              column("all", "a2", "b"),
                                                              column("all", "a3", "c"),
                                                              column("all", "a4", "d")));
        PlanNode select1 = new PlanNode(Type.SELECT, project, selector("all"));
        select1.setProperty(Property.SELECT_CRITERIA, new FullTextSearch(selector("all"), "a2", "something"));
        PlanNode select2 = new PlanNode(Type.SELECT, select1, selector("all"));
        select2.setProperty(Property.SELECT_CRITERIA, new SetCriteria(new PropertyValue(selector("all"), "primaryType"),
                                                                      new Literal("t1"), new Literal("t0")));
        PlanNode select3 = new PlanNode(Type.SELECT, select2, selector("all"));
        select3.setProperty(Property.SELECT_CRITERIA, new SetCriteria(new PropertyValue(selector("all"), "mixins"),
                                                                      new Literal("t3"), new Literal("t4")));
        PlanNode select4 = new PlanNode(Type.SELECT, select3, selector("all"));
        select4.setProperty(Property.SELECT_CRITERIA, new SetCriteria(new PropertyValue(selector("all"), "primaryType"),
                                                                      new Literal("t2"), new Literal("t0")));
        PlanNode select5 = new PlanNode(Type.SELECT, select4, selector("all"));
        select5.setProperty(Property.SELECT_CRITERIA, new SetCriteria(new PropertyValue(selector("all"), "mixins"),
                                                                      new Literal("t4"), new Literal("t5")));
        PlanNode source = new PlanNode(Type.SOURCE, select5, selector("all"));
        source.setProperty(Property.SOURCE_NAME, selector("all"));
        source.setProperty(Property.SOURCE_COLUMNS, context.getSchemata().getTable(selector("all")).getColumns());

        // Compare the expected and actual plan ...
        assertThat(node.isSameAs(access), is(true));
    }

    @Test
    public void shouldOptimizePlanForQueryJoiningMultipleTypeViewsUsingNonIdentityEquiJoin() {
        node = optimize("SELECT type1.a1 AS a, type1.a2 AS b, type2.a3 as c, type2.a4 as d "
                        + "FROM type1 JOIN type2 ON type1.a2 = type2.a3 WHERE CONTAINS(type1.a1,'something')");

        // Create the expected plan ...
        PlanNode project = new PlanNode(Type.PROJECT, selector("all"));
        project.setProperty(Property.PROJECT_COLUMNS, columns(column("all", "a1", "a"),
                                                              column("all", "a2", "b"),
                                                              column("all", "a3", "c"),
                                                              column("all", "a4", "d")));
        PlanNode select1 = new PlanNode(Type.SELECT, project, selector("all"));
        select1.setProperty(Property.SELECT_CRITERIA, new FullTextSearch(selector("all"), "a1", "something"));
        PlanNode join = new PlanNode(Type.JOIN, select1, selector("all"));
        join.setProperty(Property.JOIN_ALGORITHM, JoinAlgorithm.NESTED_LOOP);
        join.setProperty(Property.JOIN_TYPE, JoinType.INNER);
        join.setProperty(Property.JOIN_CONDITION, new EquiJoinCondition(selector("all"), "a2", selector("all"), "a3"));

        PlanNode leftAccess = new PlanNode(Type.ACCESS, join, selector("all"));
        PlanNode leftProject = new PlanNode(Type.PROJECT, leftAccess, selector("all"));
        leftProject.setProperty(Property.PROJECT_COLUMNS, columns(column("all", "a1"), column("all", "a2")));
        PlanNode leftSelect1 = new PlanNode(Type.SELECT, leftProject, selector("all"));
        leftSelect1.setProperty(Property.SELECT_CRITERIA, new SetCriteria(new PropertyValue(selector("all"), "primaryType"),
                                                                          new Literal("t1"), new Literal("t0")));
        PlanNode leftSelect2 = new PlanNode(Type.SELECT, leftSelect1, selector("all"));
        leftSelect2.setProperty(Property.SELECT_CRITERIA, new SetCriteria(new PropertyValue(selector("all"), "mixins"),
                                                                          new Literal("t3"), new Literal("t4")));
        PlanNode leftSource = new PlanNode(Type.SOURCE, leftSelect2, selector("all"));
        leftSource.setProperty(Property.SOURCE_NAME, selector("all"));
        leftSource.setProperty(Property.SOURCE_COLUMNS, context.getSchemata().getTable(selector("all")).getColumns());

        PlanNode rightAccess = new PlanNode(Type.ACCESS, join, selector("all"));
        PlanNode rightProject = new PlanNode(Type.PROJECT, rightAccess, selector("all"));
        rightProject.setProperty(Property.PROJECT_COLUMNS, columns(column("all", "a3"), column("all", "a4")));
        PlanNode rightSelect1 = new PlanNode(Type.SELECT, rightProject, selector("all"));
        rightSelect1.setProperty(Property.SELECT_CRITERIA, new SetCriteria(new PropertyValue(selector("all"), "primaryType"),
                                                                           new Literal("t2"), new Literal("t0")));
        PlanNode rightSelect2 = new PlanNode(Type.SELECT, rightSelect1, selector("all"));
        rightSelect2.setProperty(Property.SELECT_CRITERIA, new SetCriteria(new PropertyValue(selector("all"), "mixins"),
                                                                           new Literal("t4"), new Literal("t5")));
        PlanNode rightSource = new PlanNode(Type.SOURCE, rightSelect2, selector("all"));
        rightSource.setProperty(Property.SOURCE_NAME, selector("all"));
        rightSource.setProperty(Property.SOURCE_COLUMNS, context.getSchemata().getTable(selector("all")).getColumns());

        // Compare the expected and actual plan ...
        assertThat(node.isSameAs(project), is(true));
    }

    @Test
    public void shouldOptimizePlanForQueryJoiningMultipleTypeViewsUsingSameNodeJoin() {
        node = optimize("SELECT type1.a1 AS a, type1.a2 AS b, type2.a3 as c, type2.a4 as d "
                        + "FROM type1 JOIN type2 ON ISSAMENODE(type1,type2) WHERE CONTAINS(type1.a2,'something')");

        // Create the expected plan ...
        PlanNode access = new PlanNode(Type.ACCESS, selector("all"));
        PlanNode project = new PlanNode(Type.PROJECT, access, selector("all"));
        project.setProperty(Property.PROJECT_COLUMNS, columns(column("all", "a1", "a"),
                                                              column("all", "a2", "b"),
                                                              column("all", "a3", "c"),
                                                              column("all", "a4", "d")));
        PlanNode select1 = new PlanNode(Type.SELECT, project, selector("all"));
        select1.setProperty(Property.SELECT_CRITERIA, new FullTextSearch(selector("all"), "a2", "something"));
        PlanNode select2 = new PlanNode(Type.SELECT, select1, selector("all"));
        select2.setProperty(Property.SELECT_CRITERIA, new SetCriteria(new PropertyValue(selector("all"), "primaryType"),
                                                                      new Literal("t1"), new Literal("t0")));
        PlanNode select3 = new PlanNode(Type.SELECT, select2, selector("all"));
        select3.setProperty(Property.SELECT_CRITERIA, new SetCriteria(new PropertyValue(selector("all"), "mixins"),
                                                                      new Literal("t3"), new Literal("t4")));
        PlanNode select4 = new PlanNode(Type.SELECT, select3, selector("all"));
        select4.setProperty(Property.SELECT_CRITERIA, new SetCriteria(new PropertyValue(selector("all"), "primaryType"),
                                                                      new Literal("t2"), new Literal("t0")));
        PlanNode select5 = new PlanNode(Type.SELECT, select4, selector("all"));
        select5.setProperty(Property.SELECT_CRITERIA, new SetCriteria(new PropertyValue(selector("all"), "mixins"),
                                                                      new Literal("t4"), new Literal("t5")));
        PlanNode source = new PlanNode(Type.SOURCE, select5, selector("all"));
        source.setProperty(Property.SOURCE_NAME, selector("all"));
        source.setProperty(Property.SOURCE_COLUMNS, context.getSchemata().getTable(selector("all")).getColumns());

        // Compare the expected and actual plan ...
        assertThat(node.isSameAs(access), is(true));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Utility methods ...
    // ----------------------------------------------------------------------------------------------------------------

    protected List<Column> columns( Column... columns ) {
        return Arrays.asList(columns);
    }

    protected Column column( String table,
                             String columnName ) {
        return new Column(new SelectorName(table), columnName, columnName);
    }

    protected Column column( String table,
                             String columnName,
                             String alias ) {
        return new Column(new SelectorName(table), columnName, alias);
    }

    protected PlanNode optimize( String sql ) {
        QueryCommand query = new SqlQueryParser().parseQuery(sql, context.getTypeSystem());
        Problems problems = context.getProblems();
        assertThat("Problems parsing query: " + sql + "\n" + problems, problems.hasErrors(), is(false));
        PlanNode plan = new CanonicalPlanner().createPlan(context, query);
        assertThat("Problems planning query: " + sql + "\n" + problems, problems.hasErrors(), is(false));
        PlanNode optimized = new RuleBasedOptimizer().optimize(context, plan);
        assertThat("Problems optimizing query: " + sql + "\n" + problems, problems.hasErrors(), is(false));
        if (print) {
            System.out.println();
            System.out.println(sql);
            System.out.println(optimized);
        }
        return optimized;
    }
}
