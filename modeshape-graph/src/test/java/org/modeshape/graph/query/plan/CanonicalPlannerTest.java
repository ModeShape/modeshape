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
import org.modeshape.common.collection.Problems;
import org.modeshape.common.collection.SimpleProblems;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.query.QueryBuilder;
import org.modeshape.graph.query.QueryContext;
import org.modeshape.graph.query.model.Column;
import org.modeshape.graph.query.model.QueryCommand;
import org.modeshape.graph.query.model.SelectorName;
import org.modeshape.graph.query.model.TypeSystem;
import org.modeshape.graph.query.plan.PlanNode.Property;
import org.modeshape.graph.query.plan.PlanNode.Type;
import org.modeshape.graph.query.validate.ImmutableSchemata;
import org.modeshape.graph.query.validate.Schemata;
import org.junit.Before;
import org.junit.Test;

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

}
