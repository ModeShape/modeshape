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
package org.modeshape.graph.query.plan;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.collection.SimpleProblems;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.query.QueryBuilder;
import org.modeshape.graph.query.QueryContext;
import org.modeshape.graph.query.model.And;
import org.modeshape.graph.query.model.BindVariableName;
import org.modeshape.graph.query.model.Column;
import org.modeshape.graph.query.model.Comparison;
import org.modeshape.graph.query.model.Constraint;
import org.modeshape.graph.query.model.DynamicOperand;
import org.modeshape.graph.query.model.Literal;
import org.modeshape.graph.query.model.NodePath;
import org.modeshape.graph.query.model.Operator;
import org.modeshape.graph.query.model.PropertyValue;
import org.modeshape.graph.query.model.QueryCommand;
import org.modeshape.graph.query.model.SelectorName;
import org.modeshape.graph.query.model.SetCriteria;
import org.modeshape.graph.query.model.StaticOperand;
import org.modeshape.graph.query.model.TypeSystem;
import org.modeshape.graph.query.plan.PlanNode.Property;
import org.modeshape.graph.query.plan.PlanNode.Type;
import org.modeshape.graph.query.validate.ImmutableSchemata;
import org.modeshape.graph.query.validate.Schemata;

/**
 * 
 */
public class CanonicalPlannerTest {

    private CanonicalPlanner planner;
    private TypeSystem typeSystem;
    private QueryBuilder builder;
    private PlanHints hints;
    private QueryCommand query;
    private PlanNode plan;
    private Problems problems;
    private Schemata schemata;
    private ImmutableSchemata.Builder schemataBuilder;
    private QueryContext queryContext;
    private boolean print;

    @Before
    public void beforeEach() {
        planner = new CanonicalPlanner();
        typeSystem = new ExecutionContext().getValueFactories().getTypeSystem();
        hints = new PlanHints();
        builder = new QueryBuilder(typeSystem);
        problems = new SimpleProblems();
        schemataBuilder = ImmutableSchemata.createBuilder(typeSystem);
        print = false;
    }

    protected void print( PlanNode plan ) {
        if (print) System.out.println(plan);
    }

    protected SelectorName selector( String name ) {
        return new SelectorName(name);
    }

    protected Set<SelectorName> selectors( String... names ) {
        Set<SelectorName> selectors = new HashSet<SelectorName>();
        for (String name : names) {
            selectors.add(selector(name));
        }
        return selectors;
    }

    @SuppressWarnings( "unchecked" )
    protected void assertProjectNode( PlanNode node,
                                      String... columnNames ) {
        assertThat(node.getType(), is(Type.PROJECT));
        if (columnNames.length != 0) {
            assertThat(node.hasCollectionProperty(Property.PROJECT_COLUMNS), is(true));
        }
        List<Column> columns = node.getProperty(Property.PROJECT_COLUMNS, List.class);
        assertThat(columns.size(), is(columnNames.length));
        for (int i = 0; i != columns.size(); ++i) {
            Column column = columns.get(i);
            assertThat(column.columnName(), is(columnNames[i]));
        }
    }

    @SuppressWarnings( "unchecked" )
    protected void assertSourceNode( PlanNode node,
                                     String sourceName,
                                     String sourceAlias,
                                     String... availableColumns ) {
        assertThat(node.getType(), is(Type.SOURCE));
        assertThat(node.getProperty(Property.SOURCE_NAME, SelectorName.class), is(selector(sourceName)));
        if (sourceAlias != null) {
            assertThat(node.getProperty(Property.SOURCE_ALIAS, SelectorName.class), is(selector(sourceAlias)));
        } else {
            assertThat(node.hasProperty(Property.SOURCE_ALIAS), is(false));
        }
        Collection<Schemata.Column> columns = (Collection)node.getProperty(Property.SOURCE_COLUMNS);
        assertThat(columns.size(), is(availableColumns.length));
        int i = 0;
        for (Schemata.Column column : columns) {
            String expectedName = availableColumns[i++];
            assertThat(column.getName(), is(expectedName));
        }
    }

    @Test
    public void shouldProducePlanForSelectStarFromTable() {
        schemata = schemataBuilder.addTable("__ALLNODES__", "column1", "column2", "column3").build();
        query = builder.selectStar().fromAllNodes().query();
        queryContext = new QueryContext(schemata, typeSystem, hints, problems);
        plan = planner.createPlan(queryContext, query);
        assertThat(problems.isEmpty(), is(true));
        assertProjectNode(plan, "column1", "column2", "column3");
        assertThat(plan.getType(), is(PlanNode.Type.PROJECT));
        assertThat(plan.getChildCount(), is(1));
        PlanNode source = plan.getFirstChild();
        assertSourceNode(source, "__ALLNODES__", null, "column1", "column2", "column3");
        assertThat(source.getChildCount(), is(0));
        print(plan);
    }

    @Test
    public void shouldProduceErrorWhenSelectingNonExistantTable() {
        schemata = schemataBuilder.addTable("someTable", "column1", "column2", "column3").build();
        query = builder.selectStar().fromAllNodes().query();
        queryContext = new QueryContext(schemata, typeSystem, hints, problems);
        plan = planner.createPlan(queryContext, query);
        assertThat(problems.hasErrors(), is(true));
    }

    @Test
    public void shouldProduceErrorWhenSelectingNonExistantColumnOnExistingTable() {
        schemata = schemataBuilder.addTable("someTable", "column1", "column2", "column3").build();
        query = builder.select("column1", "column4").from("someTable").query();
        queryContext = new QueryContext(schemata, typeSystem, hints, problems);
        plan = planner.createPlan(queryContext, query);
        assertThat(problems.hasErrors(), is(true));
    }

    @Test
    public void shouldProducePlanWhenSelectingAllColumnsOnExistingTable() {
        schemata = schemataBuilder.addTable("someTable", "column1", "column2", "column3").build();
        query = builder.selectStar().from("someTable").query();
        queryContext = new QueryContext(schemata, typeSystem, hints, problems);
        plan = planner.createPlan(queryContext, query);
        print(plan);
        assertThat(problems.hasErrors(), is(false));
        assertThat(problems.isEmpty(), is(true));
        assertProjectNode(plan, "column1", "column2", "column3");
        assertThat(plan.getType(), is(PlanNode.Type.PROJECT));
        assertThat(plan.getChildCount(), is(1));
        PlanNode source = plan.getFirstChild();
        assertSourceNode(source, "someTable", null, "column1", "column2", "column3");
        assertThat(source.getChildCount(), is(0));
    }

    @Test
    public void shouldProducePlanWhenSelectingColumnsFromTableWithoutAlias() {
        schemata = schemataBuilder.addTable("someTable", "column1", "column2", "column3").build();
        query = builder.select("column1", "column2").from("someTable").where().path("someTable").isEqualTo(1L).end().query();
        queryContext = new QueryContext(schemata, typeSystem, hints, problems);
        plan = planner.createPlan(queryContext, query);
        assertThat(problems.hasErrors(), is(false));
        assertThat(plan.getType(), is(PlanNode.Type.PROJECT));
        assertThat(plan.getSelectors(), is(selectors("someTable")));
    }

    @Test
    public void shouldProducePlanWhenSelectingColumnsFromTableWithAlias() {
        schemata = schemataBuilder.addTable("dna:someTable", "column1", "column2", "column3").build();
        query = builder.select("column1", "column2").from("dna:someTable AS t1").where().path("t1").isEqualTo(1L).end().query();
        queryContext = new QueryContext(schemata, typeSystem, hints, problems);
        plan = planner.createPlan(queryContext, query);
        assertThat(problems.hasErrors(), is(false));
        print(plan);
        assertThat(plan.getType(), is(PlanNode.Type.PROJECT));
        assertThat(plan.getSelectors(), is(selectors("t1")));
    }

    @Test
    public void shouldProducePlanWhenSelectingAllColumnsFromTableWithAlias() {
        schemata = schemataBuilder.addTable("dna:someTable", "column1", "column2", "column3").build();
        query = builder.selectStar().from("dna:someTable AS t1").where().path("t1").isEqualTo(1L).end().query();
        queryContext = new QueryContext(schemata, typeSystem, hints, problems);
        plan = planner.createPlan(queryContext, query);
        assertThat(problems.hasErrors(), is(false));
        print(plan);
        assertThat(plan.getType(), is(PlanNode.Type.PROJECT));
        assertThat(plan.getSelectors(), is(selectors("t1")));
    }

    @Test
    public void shouldProduceErrorWhenFullTextSearchingTableWithNoSearchableColumns() {
        schemata = schemataBuilder.addTable("someTable", "column1", "column2", "column3").build();
        // Make sure the query without the search criteria does not have an error
        query = builder.select("column1", "column2").from("someTable").query();
        queryContext = new QueryContext(schemata, typeSystem, hints, problems);
        plan = planner.createPlan(queryContext, query);
        assertThat(problems.hasErrors(), is(false));

        query = builder.select("column1", "column2").from("someTable").where().search("someTable", "term1").end().query();
        queryContext = new QueryContext(schemata, typeSystem, hints, problems);
        plan = planner.createPlan(queryContext, query);
        assertThat(problems.hasErrors(), is(true));
    }

    @Test
    public void shouldProducePlanWhenFullTextSearchingTableWithAtLeastOneSearchableColumn() {
        schemata = schemataBuilder.addTable("someTable", "column1", "column2", "column3")
                                  .makeSearchable("someTable", "column1")
                                  .build();
        query = builder.select("column1", "column4").from("someTable").where().search("someTable", "term1").end().query();
        queryContext = new QueryContext(schemata, typeSystem, hints, problems);
        plan = planner.createPlan(queryContext, query);
        assertThat(problems.hasErrors(), is(true));
    }

    @Test
    public void shouldProduceErrorWhenFullTextSearchingColumnThatIsNotSearchable() {
        schemata = schemataBuilder.addTable("someTable", "column1", "column2", "column3").build();
        // Make sure the query without the search criteria does not have an error
        query = builder.select("column1", "column2").from("someTable").query();
        queryContext = new QueryContext(schemata, typeSystem, hints, problems);
        plan = planner.createPlan(queryContext, query);
        assertThat(problems.hasErrors(), is(false));

        query = builder.select("column1", "column2")
                       .from("someTable")
                       .where()
                       .search("someTable", "column2", "term1")
                       .end()
                       .query();
        queryContext = new QueryContext(schemata, typeSystem, hints, problems);
        plan = planner.createPlan(queryContext, query);
        assertThat(problems.hasErrors(), is(true));
    }

    @Test
    public void shouldProducePlanWhenFullTextSearchingColumnThatIsSearchable() {
        schemata = schemataBuilder.addTable("someTable", "column1", "column2", "column3")
                                  .makeSearchable("someTable", "column1")
                                  .build();
        query = builder.select("column1", "column4")
                       .from("someTable")
                       .where()
                       .search("someTable", "column1", "term1")
                       .end()
                       .query();
        queryContext = new QueryContext(schemata, typeSystem, hints, problems);
        plan = planner.createPlan(queryContext, query);
        assertThat(problems.hasErrors(), is(true));
    }

    @Test
    public void shouldProducePlanWhenOrderByClauseIsUsed() {
        schemata = schemataBuilder.addTable("dna:someTable", "column1", "column2", "column3").build();
        query = builder.selectStar()
                       .from("dna:someTable AS t1")
                       .where()
                       .path("t1")
                       .isEqualTo(1L)
                       .end()
                       .orderBy()
                       .ascending()
                       .propertyValue("t1", "column1")
                       .end()
                       .query();
        queryContext = new QueryContext(schemata, typeSystem, hints, problems);
        plan = planner.createPlan(queryContext, query);
        assertThat(problems.hasErrors(), is(false));
        print(plan);
        assertThat(plan.getType(), is(PlanNode.Type.SORT));
        assertThat(plan.getSelectors(), is(selectors("t1")));
    }

    @Test
    public void shouldProducePlanWhenOrderByClauseWithScoreIsUsed() {
        schemata = schemataBuilder.addTable("dna:someTable", "column1", "column2", "column3").build();
        query = builder.selectStar()
                       .from("dna:someTable AS t1")
                       .where()
                       .path("t1")
                       .isEqualTo(1L)
                       .end()
                       .orderBy()
                       .ascending()
                       .fullTextSearchScore("t1")
                       .end()
                       .query();
        queryContext = new QueryContext(schemata, typeSystem, hints, problems);
        plan = planner.createPlan(queryContext, query);
        assertThat(problems.hasErrors(), is(false));
        print(plan);
        assertThat(plan.getType(), is(PlanNode.Type.SORT));
        assertThat(plan.getSelectors(), is(selectors("t1")));
    }

    @FixFor( "MODE-869" )
    @Test
    public void shouldProducePlanWhenUsingSubquery() {
        // Define the schemata ...
        schemata = schemataBuilder.addTable("someTable", "column1", "column2", "column3").addTable("otherTable",
                                                                                                   "columnA",
                                                                                                   "columnB").build();
        // Define the subquery command ...
        QueryCommand subquery = builder.select("columnA").from("otherTable").query();
        builder = new QueryBuilder(typeSystem);

        // Define the query command (which uses the subquery) ...
        query = builder.selectStar().from("someTable").where().path("someTable").isLike(subquery).end().query();
        queryContext = new QueryContext(schemata, typeSystem, hints, problems);
        plan = planner.createPlan(queryContext, query);
        // print = true;
        print(plan);
        assertThat(problems.hasErrors(), is(false));
        assertThat(problems.isEmpty(), is(true));

        // The top node should be the dependent query ...
        assertThat(plan.getType(), is(Type.DEPENDENT_QUERY));
        assertThat(plan.getChildCount(), is(2));

        // The first child should be the plan for the subquery ...
        PlanNode subqueryPlan = plan.getFirstChild();
        assertProjectNode(subqueryPlan, "columnA");
        assertThat(subqueryPlan.getProperty(Property.VARIABLE_NAME, String.class), is("__subquery1"));
        assertThat(subqueryPlan.getChildCount(), is(1));
        assertThat(subqueryPlan.getSelectors(), is(selectors("otherTable")));
        PlanNode subquerySource = subqueryPlan.getFirstChild();
        assertSourceNode(subquerySource, "otherTable", null, "columnA", "columnB");
        assertThat(subquerySource.getChildCount(), is(0));

        // The second child should be the plan for the regular query ...
        PlanNode queryPlan = plan.getLastChild();
        assertProjectNode(queryPlan, "column1", "column2", "column3");
        assertThat(queryPlan.getType(), is(PlanNode.Type.PROJECT));
        assertThat(queryPlan.getChildCount(), is(1));
        assertThat(queryPlan.getSelectors(), is(selectors("someTable")));
        PlanNode criteriaNode = queryPlan.getFirstChild();
        assertThat(criteriaNode.getType(), is(PlanNode.Type.SELECT));
        assertThat(criteriaNode.getChildCount(), is(1));
        assertThat(criteriaNode.getSelectors(), is(selectors("someTable")));
        assertThat(criteriaNode.getProperty(Property.SELECT_CRITERIA),
                   is((Object)like(nodePath("someTable"), var("__subquery1"))));

        PlanNode source = criteriaNode.getFirstChild();
        assertSourceNode(source, "someTable", null, "column1", "column2", "column3");
        assertThat(source.getChildCount(), is(0));
    }

    @FixFor( "MODE-869" )
    @Test
    public void shouldProducePlanWhenUsingSubqueryInSubquery() {
        // Define the schemata ...
        schemata = schemataBuilder.addTable("someTable", "column1", "column2", "column3")
                                  .addTable("otherTable", "columnA", "columnB")
                                  .addTable("stillOther", "columnX", "columnY")
                                  .build();
        // Define the innermost subquery command ...
        QueryCommand subquery2 = builder.select("columnY")
                                        .from("stillOther")
                                        .where()
                                        .propertyValue("stillOther", "columnX")
                                        .isLessThan()
                                        .cast(3)
                                        .asLong()
                                        .end()
                                        .query();
        builder = new QueryBuilder(typeSystem);

        // Define the outer subquery command ...
        QueryCommand subquery1 = builder.select("columnA")
                                        .from("otherTable")
                                        .where()
                                        .propertyValue("otherTable", "columnB")
                                        .isEqualTo(subquery2)
                                        .end()
                                        .query();
        builder = new QueryBuilder(typeSystem);

        // Define the query command (which uses the subquery) ...
        query = builder.selectStar().from("someTable").where().path("someTable").isLike(subquery1).end().query();
        queryContext = new QueryContext(schemata, typeSystem, hints, problems);
        plan = planner.createPlan(queryContext, query);
        // print = true;
        print(plan);
        assertThat(problems.hasErrors(), is(false));
        assertThat(problems.isEmpty(), is(true));

        // The top node should be the dependent query ...
        assertThat(plan.getType(), is(Type.DEPENDENT_QUERY));
        assertThat(plan.getChildCount(), is(2));

        // The first child of the top node should be a dependent query ...
        PlanNode depQuery1 = plan.getFirstChild();
        assertThat(depQuery1.getType(), is(PlanNode.Type.DEPENDENT_QUERY));
        assertThat(depQuery1.getChildCount(), is(2));

        // The first child should be the plan for the 2nd subquery (since it has to be executed first) ...
        PlanNode subqueryPlan2 = depQuery1.getFirstChild();
        assertProjectNode(subqueryPlan2, "columnY");
        assertThat(subqueryPlan2.getProperty(Property.VARIABLE_NAME, String.class), is("__subquery2"));
        assertThat(subqueryPlan2.getChildCount(), is(1));
        assertThat(subqueryPlan2.getSelectors(), is(selectors("stillOther")));
        PlanNode criteriaNode2 = subqueryPlan2.getFirstChild();
        assertThat(criteriaNode2.getType(), is(PlanNode.Type.SELECT));
        assertThat(criteriaNode2.getChildCount(), is(1));
        assertThat(criteriaNode2.getSelectors(), is(selectors("stillOther")));
        assertThat(criteriaNode2.getProperty(Property.SELECT_CRITERIA), is((Object)lessThan(property("stillOther", "columnX"),
                                                                                            literal(3L))));
        PlanNode subquerySource2 = criteriaNode2.getFirstChild();
        assertSourceNode(subquerySource2, "stillOther", null, "columnX", "columnY");
        assertThat(subquerySource2.getChildCount(), is(0));

        // The second child of the dependent query should be the plan for the subquery ...
        PlanNode subqueryPlan1 = depQuery1.getLastChild();
        assertProjectNode(subqueryPlan1, "columnA");
        assertThat(subqueryPlan1.getProperty(Property.VARIABLE_NAME, String.class), is("__subquery1"));
        assertThat(subqueryPlan1.getChildCount(), is(1));
        assertThat(subqueryPlan1.getSelectors(), is(selectors("otherTable")));
        PlanNode criteriaNode1 = subqueryPlan1.getFirstChild();
        assertThat(criteriaNode1.getType(), is(PlanNode.Type.SELECT));
        assertThat(criteriaNode1.getChildCount(), is(1));
        assertThat(criteriaNode1.getSelectors(), is(selectors("otherTable")));
        assertThat(criteriaNode1.getProperty(Property.SELECT_CRITERIA), is((Object)equals(property("otherTable", "columnB"),
                                                                                          var("__subquery2"))));
        PlanNode subquerySource1 = criteriaNode1.getFirstChild();
        assertSourceNode(subquerySource1, "otherTable", null, "columnA", "columnB");
        assertThat(subquerySource1.getChildCount(), is(0));

        // The second child of the top node should be the plan for the regular query ...
        PlanNode queryPlan = plan.getLastChild();
        assertProjectNode(queryPlan, "column1", "column2", "column3");
        assertThat(queryPlan.getType(), is(PlanNode.Type.PROJECT));
        assertThat(queryPlan.getChildCount(), is(1));
        assertThat(queryPlan.getSelectors(), is(selectors("someTable")));
        PlanNode criteriaNode = queryPlan.getFirstChild();
        assertThat(criteriaNode.getType(), is(PlanNode.Type.SELECT));
        assertThat(criteriaNode.getChildCount(), is(1));
        assertThat(criteriaNode.getSelectors(), is(selectors("someTable")));
        assertThat(criteriaNode.getProperty(Property.SELECT_CRITERIA),
                   is((Object)like(nodePath("someTable"), var("__subquery1"))));

        PlanNode source = criteriaNode.getFirstChild();
        assertSourceNode(source, "someTable", null, "column1", "column2", "column3");
        assertThat(source.getChildCount(), is(0));
    }

    @FixFor( "MODE-869" )
    @Test
    public void shouldProducePlanWhenUsingTwoSubqueries() {
        // Define the schemata ...
        schemata = schemataBuilder.addTable("someTable", "column1", "column2", "column3")
                                  .addTable("otherTable", "columnA", "columnB")
                                  .addTable("stillOther", "columnX", "columnY")
                                  .build();
        // Define the first subquery command ...
        QueryCommand subquery1 = builder.select("columnA")
                                        .from("otherTable")
                                        .where()
                                        .propertyValue("otherTable", "columnB")
                                        .isEqualTo("winner")
                                        .end()
                                        .query();
        builder = new QueryBuilder(typeSystem);

        // Define the second subquery command ...
        QueryCommand subquery2 = builder.select("columnY")
                                        .from("stillOther")
                                        .where()
                                        .propertyValue("stillOther", "columnX")
                                        .isLessThan()
                                        .cast(3)
                                        .asLong()
                                        .end()
                                        .query();
        builder = new QueryBuilder(typeSystem);

        // Define the query command (which uses the subquery) ...
        query = builder.selectStar()
                       .from("someTable")
                       .where()
                       .path("someTable")
                       .isLike(subquery2)
                       .and()
                       .propertyValue("someTable", "column3")
                       .isInSubquery(subquery1)
                       .end()
                       .query();
        queryContext = new QueryContext(schemata, typeSystem, hints, problems);
        plan = planner.createPlan(queryContext, query);
        // print = true;
        print(plan);
        assertThat(problems.hasErrors(), is(false));
        assertThat(problems.isEmpty(), is(true));

        // The top node should be the dependent query ...
        assertThat(plan.getType(), is(Type.DEPENDENT_QUERY));
        assertThat(plan.getChildCount(), is(2));

        // The first child of the top node should be the plan for subquery1 ...
        PlanNode subqueryPlan1 = plan.getFirstChild();
        assertProjectNode(subqueryPlan1, "columnA");
        assertThat(subqueryPlan1.getProperty(Property.VARIABLE_NAME, String.class), is("__subquery1"));
        assertThat(subqueryPlan1.getChildCount(), is(1));
        assertThat(subqueryPlan1.getSelectors(), is(selectors("otherTable")));
        PlanNode criteriaNode1 = subqueryPlan1.getFirstChild();
        assertThat(criteriaNode1.getType(), is(PlanNode.Type.SELECT));
        assertThat(criteriaNode1.getChildCount(), is(1));
        assertThat(criteriaNode1.getSelectors(), is(selectors("otherTable")));
        assertThat(criteriaNode1.getProperty(Property.SELECT_CRITERIA), is((Object)equals(property("otherTable", "columnB"),
                                                                                          literal("winner"))));
        PlanNode subquerySource1 = criteriaNode1.getFirstChild();
        assertSourceNode(subquerySource1, "otherTable", null, "columnA", "columnB");
        assertThat(subquerySource1.getChildCount(), is(0));

        // The second child of the top node should be a dependent query ...
        PlanNode depQuery2 = plan.getLastChild();
        assertThat(depQuery2.getType(), is(PlanNode.Type.DEPENDENT_QUERY));
        assertThat(depQuery2.getChildCount(), is(2));

        // The first child of the second dependent should be the plan for the 2nd subquery (since it has to be executed first) ...
        PlanNode subqueryPlan2 = depQuery2.getFirstChild();
        assertProjectNode(subqueryPlan2, "columnY");
        assertThat(subqueryPlan2.getProperty(Property.VARIABLE_NAME, String.class), is("__subquery2"));
        assertThat(subqueryPlan2.getChildCount(), is(1));
        assertThat(subqueryPlan2.getSelectors(), is(selectors("stillOther")));
        PlanNode criteriaNode2 = subqueryPlan2.getFirstChild();
        assertThat(criteriaNode2.getType(), is(PlanNode.Type.SELECT));
        assertThat(criteriaNode2.getChildCount(), is(1));
        assertThat(criteriaNode2.getSelectors(), is(selectors("stillOther")));
        assertThat(criteriaNode2.getProperty(Property.SELECT_CRITERIA), is((Object)lessThan(property("stillOther", "columnX"),
                                                                                            literal(3L))));
        PlanNode subquerySource2 = criteriaNode2.getFirstChild();
        assertSourceNode(subquerySource2, "stillOther", null, "columnX", "columnY");
        assertThat(subquerySource2.getChildCount(), is(0));

        // The second child of the second dependent node should be the plan for the regular query ...
        PlanNode queryPlan = depQuery2.getLastChild();
        assertProjectNode(queryPlan, "column1", "column2", "column3");
        assertThat(queryPlan.getType(), is(PlanNode.Type.PROJECT));
        assertThat(queryPlan.getChildCount(), is(1));
        assertThat(queryPlan.getSelectors(), is(selectors("someTable")));
        PlanNode criteriaNode3 = queryPlan.getFirstChild();
        assertThat(criteriaNode3.getType(), is(PlanNode.Type.SELECT));
        assertThat(criteriaNode3.getChildCount(), is(1));
        assertThat(criteriaNode3.getSelectors(), is(selectors("someTable")));
        assertThat(criteriaNode3.getProperty(Property.SELECT_CRITERIA),
                   is((Object)like(nodePath("someTable"), var("__subquery2"))));
        PlanNode criteriaNode4 = criteriaNode3.getFirstChild();
        assertThat(criteriaNode4.getProperty(Property.SELECT_CRITERIA), is((Object)in(property("someTable", "column3"),
                                                                                      var("__subquery1"))));

        PlanNode source = criteriaNode4.getFirstChild();
        assertSourceNode(source, "someTable", null, "column1", "column2", "column3");
        assertThat(source.getChildCount(), is(0));
    }

    protected NodePath nodePath( String selectorName ) {
        return nodePath(selector(selectorName));
    }

    protected NodePath nodePath( SelectorName selectorName ) {
        return new NodePath(selectorName);
    }

    protected PropertyValue property( String selectorName,
                                      String columnName ) {
        return property(selector(selectorName), columnName);
    }

    protected PropertyValue property( SelectorName selectorName,
                                      String columnName ) {
        return new PropertyValue(selectorName, columnName);
    }

    protected BindVariableName var( String variableName ) {
        return new BindVariableName(variableName);
    }

    protected Literal literal( Object value ) {
        return new Literal(value);
    }

    protected And and( Constraint left,
                       Constraint right ) {
        return new And(left, right);
    }

    protected Comparison like( DynamicOperand left,
                               StaticOperand right ) {
        return new Comparison(left, Operator.LIKE, right);
    }

    protected Comparison lessThan( DynamicOperand left,
                                   StaticOperand right ) {
        return new Comparison(left, Operator.LESS_THAN, right);
    }

    protected Comparison equals( DynamicOperand left,
                                 StaticOperand right ) {
        return new Comparison(left, Operator.EQUAL_TO, right);
    }

    protected SetCriteria in( DynamicOperand left,
                              StaticOperand... right ) {
        return new SetCriteria(left, right);
    }
}
