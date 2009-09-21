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
package org.jboss.dna.graph.query.process;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.jboss.dna.common.util.StringUtil;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.query.QueryContext;
import org.jboss.dna.graph.query.QueryResults.Columns;
import org.jboss.dna.graph.query.QueryResults.Cursor;
import org.jboss.dna.graph.query.QueryResults.Statistics;
import org.jboss.dna.graph.query.model.Column;
import org.jboss.dna.graph.query.model.QueryCommand;
import org.jboss.dna.graph.query.plan.PlanHints;
import org.jboss.dna.graph.query.process.QueryResultColumns;
import org.jboss.dna.graph.query.process.QueryResults;
import org.jboss.dna.graph.query.validate.Schemata;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * 
 */
public class QueryResultColumnsTest extends AbstractQueryResultsTest {

    private QueryContext context;
    private List<Column> columnList;
    private Columns columns;
    private QueryResults results;
    private List<Object[]> tuples;
    @Mock
    private Schemata schemata;
    @Mock
    private QueryCommand command;
    @Mock
    private Statistics statistics;

    @Before
    public void beforeEach() {
        MockitoAnnotations.initMocks(this);
        context = new QueryContext(executionContext, new PlanHints(), schemata);
        columnList = new ArrayList<Column>();
        columnList.add(new Column(selector("table1"), name("colA"), "colA"));
        columnList.add(new Column(selector("table1"), name("colB"), "colB"));
        columnList.add(new Column(selector("table1"), name("colC"), "colC"));
        columnList.add(new Column(selector("table2"), name("colA"), "colA2"));
        columnList.add(new Column(selector("table2"), name("colB"), "colB2"));
        columnList.add(new Column(selector("table2"), name("colX"), "colX"));
        columns = new QueryResultColumns(columnList, false);
        tuples = new ArrayList<Object[]>();
        tuples.add(tuple(columns, new String[] {"/a/b/c", "/a/x/y"}, 1, 2, 3, "2a", "2b", "x"));
        tuples.add(tuple(columns, new String[] {"/a/b/d", "/a/x/y"}, 4, 5, 6, "2a", "2b", "x"));
        results = new QueryResults(context, command, columns, statistics);
    }

    @Test
    public void shouldReturnSameColumnsPassedIntoConstructor() {
        assertThat(results.getColumns(), is(sameInstance(columns)));
    }

    @Test
    public void shouldReturnSameStatisticsPassedIntoConstructor() {
        assertThat(results.getStatistics(), is(sameInstance(statistics)));
    }

    @Test
    public void shouldReturnSameQueryCommandPassedIntoConstructor() {
        assertThat(results.getCommand(), is(sameInstance(command)));
    }

    @Test
    public void shouldReturnSameProblemsObjectAsInQueryContext() {
        assertThat(results.getProblems(), is(sameInstance(context.getProblems())));
    }

    @Test
    public void shouldReturnSameTuplesListPassedIntoConstructor() {
        results = new QueryResults(context, command, columns, statistics, tuples);
        assertThat(results.getTuples(), is(sameInstance(tuples)));
    }

    @Test
    public void shouldHaveNoTuplesIfConstructedWithNoTuples() {
        assertThat(results.getTuples().isEmpty(), is(true));
        assertThat(results.getCursor().hasNext(), is(false));
    }

    @Test
    public void shouldHaveNoTuplesIfConstructedWithEmptyTuplesList() {
        tuples.clear();
        results = new QueryResults(context, command, columns, statistics, tuples);
        assertThat(results.getTuples().isEmpty(), is(true));
        assertThat(results.getCursor().hasNext(), is(false));
    }

    @Test
    public void shouldReturnMutableTuplesList() {
        results = new QueryResults(context, command, columns, statistics, tuples);
        assertThat(results.getTuples().isEmpty(), is(false));
        results.getTuples().clear();
        assertThat(results.getTuples().isEmpty(), is(true));
        assertThat(tuples.isEmpty(), is(true));
    }

    @Test
    public void shouldReturnCursorThatAccessesTuples() {
        results = new QueryResults(context, command, columns, statistics, tuples);
        Cursor cursor = results.getCursor();
        Iterator<Object[]> expectedIter = tuples.iterator();
        int rowNumber = 0;
        while (cursor.hasNext() && expectedIter.hasNext()) {
            cursor.next();
            Object[] tuple = expectedIter.next();
            // Check the column values by column name and index ...
            for (Column column : results.getColumns().getColumns()) {
                String columnName = column.getColumnName();
                int columnIndex = columns.getColumnIndexForName(columnName);
                assertThat(cursor.getValue(columnName), is(tuple[columnIndex]));
                assertThat(cursor.getValue(columnIndex), is(tuple[columnIndex]));
                // Get the location for this column ...
                int locationIndex = columns.getLocationIndex(column.getSelectorName().getName());
                Location location = (Location)tuple[locationIndex];
                assertThat(cursor.getLocation(columnIndex), is(location));
            }
            // Check the locations by selector name and index ...
            for (String selectorName : results.getColumns().getSelectorNames()) {
                int locationIndex = columns.getLocationIndex(selectorName);
                Location location = (Location)tuple[locationIndex];
                assertThat(cursor.getLocation(selectorName), is(location));
                assertThat(location.hasPath(), is(true));
            }
            // Check the row index ...
            assertThat(cursor.getRowIndex(), is(rowNumber++));
        }
        assertThat(cursor.hasNext(), is(false));
        assertThat(expectedIter.hasNext(), is(false));
    }

    @Test( expected = IllegalStateException.class )
    public void shouldRequireNextOnCursorToBeCalledBeforeGettingValueUsingColumnIndex() {
        results = new QueryResults(context, command, columns, statistics, tuples);
        Cursor cursor = results.getCursor();
        assertThat(cursor.hasNext(), is(true));
        cursor.getValue(0);
    }

    @Test( expected = IllegalStateException.class )
    public void shouldRequireNextOnCursorToBeCalledBeforeGettingValueUsingColumnName() {
        results = new QueryResults(context, command, columns, statistics, tuples);
        Cursor cursor = results.getCursor();
        assertThat(cursor.hasNext(), is(true));
        cursor.getValue("colA");
    }

    @Test
    public void shouldPrintToStringAllResults() {
        results = new QueryResults(context, command, columns, statistics, tuples);
        List<String> lines = StringUtil.splitLines(results.toString());
        assertThat(lines.size(), is(tuples.size() + 4)); // = delim + header + delim + (...lines...) + delim
    }

    @Test
    public void shouldPrintToStringBuilderAllResults() {
        results = new QueryResults(context, command, columns, statistics, tuples);
        StringBuilder sb = new StringBuilder();
        results.toString(sb);
        List<String> lines = StringUtil.splitLines(sb.toString());
        assertThat(lines.size(), is(tuples.size() + 4)); // = delim + header + delim + (...lines...) + delim
    }

    @Test
    public void shouldPrintToStringBuilderAllResultsEvenWhenNoTuples() {
        tuples.clear();
        results = new QueryResults(context, command, columns, statistics, tuples);
        StringBuilder sb = new StringBuilder();
        results.toString(sb);
        List<String> lines = StringUtil.splitLines(sb.toString());
        assertThat(lines.size(), is(4)); // = delim + header + delim + (...lines...) + delim
    }

    @Test
    public void shouldPrintToStringBuilderOnlyFirstLinesOfResults() {
        results = new QueryResults(context, command, columns, statistics, tuples);
        StringBuilder sb = new StringBuilder();
        results.toString(sb, 1);
        List<String> lines = StringUtil.splitLines(sb.toString());
        assertThat(lines.size(), is(1 + 4)); // = delim + header + delim + (...lines...) + delim
    }

    @Test
    public void shouldPrintToStringBuilderOnlyFirstLinesOfResultsEvenWhenNoTuples() {
        tuples.clear();
        results = new QueryResults(context, command, columns, statistics, tuples);
        StringBuilder sb = new StringBuilder();
        results.toString(sb, 3);
        List<String> lines = StringUtil.splitLines(sb.toString());
        assertThat(lines.size(), is(4)); // = delim + header + delim + (...lines...) + delim
    }

    @Test
    public void shouldPrintToStringBuilderAllResultsWhenMaxRowParameterIsLargerThanNumberOfTuples() {
        tuples.clear();
        results = new QueryResults(context, command, columns, statistics, tuples);
        StringBuilder sb = new StringBuilder();
        results.toString(sb, 3);
        List<String> lines = StringUtil.splitLines(sb.toString());
        assertThat(lines.size(), is(tuples.size() + 4)); // = delim + header + delim + (...lines...) + delim
    }
}
