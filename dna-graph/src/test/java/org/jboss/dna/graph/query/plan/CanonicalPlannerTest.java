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
package org.jboss.dna.graph.query.plan;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.util.Collection;
import java.util.List;
import org.jboss.dna.common.collection.Problems;
import org.jboss.dna.common.collection.SimpleProblems;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.query.QueryBuilder;
import org.jboss.dna.graph.query.QueryContext;
import org.jboss.dna.graph.query.model.Column;
import org.jboss.dna.graph.query.model.QueryCommand;
import org.jboss.dna.graph.query.model.SelectorName;
import org.jboss.dna.graph.query.plan.PlanNode.Property;
import org.jboss.dna.graph.query.plan.PlanNode.Type;
import org.jboss.dna.graph.query.validate.ImmutableSchemata;
import org.jboss.dna.graph.query.validate.Schemata;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class CanonicalPlannerTest {

    private CanonicalPlanner planner;
    private ExecutionContext context;
    private QueryBuilder builder;
    private PlanHints hints;
    private QueryCommand query;
    private PlanNode plan;
    private Problems problems;
    private Schemata schemata;
    private ImmutableSchemata.Builder schemataBuilder;
    private QueryContext queryContext;

    @Before
    public void beforeEach() {
        planner = new CanonicalPlanner();
        context = new ExecutionContext();
        hints = new PlanHints();
        builder = new QueryBuilder(context);
        problems = new SimpleProblems();
        schemataBuilder = ImmutableSchemata.createBuilder(context);
    }

    protected SelectorName selector( String name ) {
        return new SelectorName(name);
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
            assertThat(column.getColumnName(), is(columnNames[i]));
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
        queryContext = new QueryContext(context, schemata, hints, problems);
        plan = planner.createPlan(queryContext, query);
        assertThat(problems.isEmpty(), is(true));
        assertProjectNode(plan, "column1", "column2", "column3");
        assertThat(plan.getType(), is(PlanNode.Type.PROJECT));
        assertThat(plan.getChildCount(), is(1));
        PlanNode source = plan.getFirstChild();
        assertSourceNode(source, "__ALLNODES__", null, "column1", "column2", "column3");
        assertThat(source.getChildCount(), is(0));
        System.out.println(plan);
    }

    @Test
    public void shouldProduceErrorWhenSelectingNonExistantTable() {
        schemata = schemataBuilder.addTable("someTable", "column1", "column2", "column3").build();
        query = builder.selectStar().fromAllNodes().query();
        queryContext = new QueryContext(context, schemata, hints, problems);
        plan = planner.createPlan(queryContext, query);
        assertThat(problems.hasErrors(), is(true));
    }

    @Test
    public void shouldProduceErrorWhenSelectingNonExistantColumnOnExistingTable() {
        schemata = schemataBuilder.addTable("someTable", "column1", "column2", "column3").build();
        query = builder.select("column1", "column4").from("someTable").query();
        queryContext = new QueryContext(context, schemata, hints, problems);
        plan = planner.createPlan(queryContext, query);
        assertThat(problems.hasErrors(), is(true));
    }

    @Test
    public void shouldProducePlanWhenSelectingAllColumnsOnExistingTable() {
        schemata = schemataBuilder.addTable("someTable", "column1", "column2", "column3").build();
        query = builder.selectStar().from("someTable").query();
        queryContext = new QueryContext(context, schemata, hints, problems);
        plan = planner.createPlan(queryContext, query);
        System.out.println(plan);
        assertThat(problems.hasErrors(), is(false));
        assertThat(problems.isEmpty(), is(true));
        assertProjectNode(plan, "column1", "column2", "column3");
        assertThat(plan.getType(), is(PlanNode.Type.PROJECT));
        assertThat(plan.getChildCount(), is(1));
        PlanNode source = plan.getFirstChild();
        assertSourceNode(source, "someTable", null, "column1", "column2", "column3");
        assertThat(source.getChildCount(), is(0));
    }

}
