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
package org.modeshape.jcr.query.process;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.cache.RepositoryCache;
import org.modeshape.jcr.query.QueryContext;
import org.modeshape.jcr.query.QueryResults.Columns;
import org.modeshape.jcr.query.QueryResults.Cursor;
import org.modeshape.jcr.query.QueryResults.Location;
import org.modeshape.jcr.query.QueryResults.Statistics;
import org.modeshape.jcr.query.model.Column;
import org.modeshape.jcr.query.validate.Schemata;

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
        context = new QueryContext(executionContext, mock(RepositoryCache.class), Collections.singleton("workspace"), schemata);
        columnList = new ArrayList<Column>();
        columnList.add(new Column(selector("table1"), "colA", "colA"));
        columnList.add(new Column(selector("table1"), "colB", "colB"));
        columnList.add(new Column(selector("table1"), "colC", "colC"));
        columnList.add(new Column(selector("table2"), "colA", "colA2"));
        columnList.add(new Column(selector("table2"), "colB", "colB2"));
        columnList.add(new Column(selector("table2"), "colX", "colX"));
        columnTypes = new ArrayList<String>();
        for (int i = 0; i != columnList.size(); ++i) {
            columnTypes.add(org.modeshape.jcr.value.PropertyType.STRING.getName());
        }
        columns = new QueryResultColumns(columnList, columnTypes, false);
        tuples = new ArrayList<Object[]>();
        tuples.add(tuple(columns, new String[] {"/a/b/c", "/a/x/y"}, 1, 2, 3, "2a", "2b", "x"));
        tuples.add(tuple(columns, new String[] {"/a/b/d", "/a/x/y"}, 4, 5, 6, "2a", "2b", "x"));
        results = new QueryResults(columns, statistics, context.getProblems());
    }

    protected String columnNameFor( Column column ) {
        if (column.getColumnName().equals(column.getPropertyName())) {
            return column.getSelectorName() + "." + column.getColumnName();
        }
        return column.getColumnName();
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
                String columnName = columnNameFor(column);
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
                assertThat(location.getPath(), is(notNullValue()));
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
