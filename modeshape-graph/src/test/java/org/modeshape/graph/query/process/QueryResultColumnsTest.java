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
package org.modeshape.graph.query.process;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modeshape.common.util.StringUtil;
import org.modeshape.graph.Location;
import org.modeshape.graph.property.PropertyType;
import org.modeshape.graph.query.QueryContext;
import org.modeshape.graph.query.QueryResults.Columns;
import org.modeshape.graph.query.QueryResults.Cursor;
import org.modeshape.graph.query.QueryResults.Statistics;
import org.modeshape.graph.query.model.Column;
import org.modeshape.graph.query.validate.Schemata;

/**
 * 
 */
public class QueryResultColumnsTest extends AbstractQueryResultsTest {

    private QueryContext context;
    private List<Column> columnList;
    private List<String> columnTypes;
    private Columns columns;
    private QueryResults results;
    private List<Object[]> tuples;
    @Mock
    private Schemata schemata;
    @Mock
    private Statistics statistics;

    @Before
    public void beforeEach() {
        MockitoAnnotations.initMocks(this);
        context = new QueryContext(schemata, typeSystem);
        columnList = new ArrayList<Column>();
        columnList.add(new Column(selector("table1"), "colA", "colA"));
        columnList.add(new Column(selector("table1"), "colB", "colB"));
        columnList.add(new Column(selector("table1"), "colC", "colC"));
        columnList.add(new Column(selector("table2"), "colA", "colA2"));
        columnList.add(new Column(selector("table2"), "colB", "colB2"));
        columnList.add(new Column(selector("table2"), "colX", "colX"));
        columnTypes = new ArrayList<String>();
        for (int i = 0; i != columnList.size(); ++i) {
            columnTypes.add(PropertyType.STRING.getName());
        }
        columns = new QueryResultColumns(columnList, columnTypes, false);
        tuples = new ArrayList<Object[]>();
        tuples.add(tuple(columns, new String[] {"/a/b/c", "/a/x/y"}, 1, 2, 3, "2a", "2b", "x"));
        tuples.add(tuple(columns, new String[] {"/a/b/d", "/a/x/y"}, 4, 5, 6, "2a", "2b", "x"));
        results = new QueryResults(columns, statistics, context.getProblems());
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
    public void shouldReturnSameProblemsObjectAsInQueryContext() {
        assertThat(results.getProblems(), is(sameInstance(context.getProblems())));
    }

    @Test
    public void shouldReturnSameTuplesListPassedIntoConstructor() {
        results = new QueryResults(columns, statistics, tuples, context.getProblems(), null);
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
        results = new QueryResults(columns, statistics, tuples, context.getProblems(), null);
        assertThat(results.getTuples().isEmpty(), is(true));
        assertThat(results.getCursor().hasNext(), is(false));
    }

    @Test
    public void shouldReturnMutableTuplesList() {
        results = new QueryResults(columns, statistics, tuples, context.getProblems(), null);
        assertThat(results.getTuples().isEmpty(), is(false));
        results.getTuples().clear();
        assertThat(results.getTuples().isEmpty(), is(true));
        assertThat(tuples.isEmpty(), is(true));
    }

    @Test
    public void shouldReturnCursorThatAccessesTuples() {
        results = new QueryResults(columns, statistics, tuples, context.getProblems(), null);
        Cursor cursor = results.getCursor();
        Iterator<Object[]> expectedIter = tuples.iterator();
        int rowNumber = 0;
        while (cursor.hasNext() && expectedIter.hasNext()) {
            cursor.next();
            Object[] tuple = expectedIter.next();
            // Check the column values by column name and index ...
            for (Column column : results.getColumns().getColumns()) {
                String columnName = column.columnName();
                int columnIndex = columns.getColumnIndexForName(columnName);
                assertThat(cursor.getValue(columnName), is(tuple[columnIndex]));
                assertThat(cursor.getValue(columnIndex), is(tuple[columnIndex]));
                // Get the location for this column ...
                int locationIndex = columns.getLocationIndex(column.selectorName().name());
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
        results = new QueryResults(columns, statistics, tuples, context.getProblems(), null);
        Cursor cursor = results.getCursor();
        assertThat(cursor.hasNext(), is(true));
        cursor.getValue(0);
    }

    @Test( expected = IllegalStateException.class )
    public void shouldRequireNextOnCursorToBeCalledBeforeGettingValueUsingColumnName() {
        results = new QueryResults(columns, statistics, tuples, context.getProblems(), null);
        Cursor cursor = results.getCursor();
        assertThat(cursor.hasNext(), is(true));
        cursor.getValue("colA");
    }

    @Test
    public void shouldPrintToStringAllResults() {
        results = new QueryResults(columns, statistics, tuples, context.getProblems(), null);
        List<String> lines = StringUtil.splitLines(results.toString());
        assertThat(lines.size(), is(tuples.size() + 4)); // = delim + header + delim + (...lines...) + delim
    }

    @Test
    public void shouldPrintToStringBuilderAllResults() {
        results = new QueryResults(columns, statistics, tuples, context.getProblems(), null);
        StringBuilder sb = new StringBuilder();
        results.toString(typeSystem, sb);
        List<String> lines = StringUtil.splitLines(sb.toString());
        assertThat(lines.size(), is(tuples.size() + 4)); // = delim + header + delim + (...lines...) + delim
    }

    @Test
    public void shouldPrintToStringBuilderAllResultsEvenWhenNoTuples() {
        tuples.clear();
        results = new QueryResults(columns, statistics, tuples, context.getProblems(), null);
        StringBuilder sb = new StringBuilder();
        results.toString(typeSystem, sb);
        List<String> lines = StringUtil.splitLines(sb.toString());
        assertThat(lines.size(), is(4)); // = delim + header + delim + (...lines...) + delim
    }

    @Test
    public void shouldPrintToStringBuilderOnlyFirstLinesOfResults() {
        results = new QueryResults(columns, statistics, tuples, context.getProblems(), null);
        StringBuilder sb = new StringBuilder();
        results.toString(typeSystem, sb, 1);
        List<String> lines = StringUtil.splitLines(sb.toString());
        assertThat(lines.size(), is(1 + 4)); // = delim + header + delim + (...lines...) + delim
    }

    @Test
    public void shouldPrintToStringBuilderOnlyFirstLinesOfResultsEvenWhenNoTuples() {
        tuples.clear();
        results = new QueryResults(columns, statistics, tuples, context.getProblems(), null);
        StringBuilder sb = new StringBuilder();
        results.toString(typeSystem, sb, 3);
        List<String> lines = StringUtil.splitLines(sb.toString());
        assertThat(lines.size(), is(4)); // = delim + header + delim + (...lines...) + delim
    }

    @Test
    public void shouldPrintToStringBuilderAllResultsWhenMaxRowParameterIsLargerThanNumberOfTuples() {
        tuples.clear();
        results = new QueryResults(columns, statistics, tuples, context.getProblems(), null);
        StringBuilder sb = new StringBuilder();
        results.toString(typeSystem, sb, 3);
        List<String> lines = StringUtil.splitLines(sb.toString());
        assertThat(lines.size(), is(tuples.size() + 4)); // = delim + header + delim + (...lines...) + delim
    }
}
