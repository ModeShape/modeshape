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
import static org.junit.Assert.assertThat;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.query.QueryResults.Columns;
import org.modeshape.jcr.query.model.Column;
import org.modeshape.jcr.value.PropertyType;

/**
 * 
 */
public class QueryResultsTest extends AbstractQueryResultsTest {

    private List<Column> columnList;
    private List<String> columnTypes;
    private QueryResultColumns columnsWithoutScores;
    private QueryResultColumns columnsWithScores;

    @Before
    public void beforeEach() {
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
        columnsWithoutScores = new QueryResultColumns(columnList, columnTypes, false);
        columnsWithScores = new QueryResultColumns(columnList, columnTypes, true);
    }

    protected String columnNameFor( Column column ) {
        if (column.getColumnName().equals(column.getPropertyName())) {
            return column.getSelectorName() + "." + column.getColumnName();
        }
        return column.getColumnName();
    }

    @Test
    public void shouldHaveCorrectTupleSize() {
        assertThat(columnsWithScores.getTupleSize(), is(columnList.size() + 2 + 2));
        assertThat(columnsWithoutScores.getTupleSize(), is(columnList.size() + 2 + 0));
    }

    @Test
    public void shouldHaveCorrectTupleNames() {
        List<String> expected = new ArrayList<String>();
        expected.add("table1.colA");
        expected.add("table1.colB");
        expected.add("table1.colC");
        expected.add("colA2");
        expected.add("colB2");
        expected.add("table2.colX");
        expected.add("Location(table1)");
        expected.add("Location(table2)");
        assertThat(columnsWithoutScores.getTupleValueNames(), is(expected));
        expected.add("Score(table1)");
        expected.add("Score(table2)");
        assertThat(columnsWithScores.getTupleValueNames(), is(expected));
    }

    @Test
    public void shouldHaveCorrectSelectorNames() {
        List<String> expected = new ArrayList<String>();
        expected.add("table1");
        expected.add("table2");
        assertThat(columnsWithoutScores.getSelectorNames(), is(expected));
        assertThat(columnsWithScores.getSelectorNames(), is(expected));
    }

    @Test
    public void shouldHaveCorrectNumberOfColumns() {
        assertThat(columnsWithScores.getColumnCount(), is(columnList.size()));
        assertThat(columnsWithoutScores.getColumnCount(), is(columnList.size()));
    }

    @Test
    public void shouldReturnColumns() {
        assertThat(new ArrayList<Column>(columnsWithScores.getColumns()), is(columnList));
        assertThat(new ArrayList<Column>(columnsWithoutScores.getColumns()), is(columnList));
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldReturnImmutableColumns() {
        assertThat(columnsWithoutScores.getColumns().isEmpty(), is(false));
        columnsWithoutScores.getColumns().clear();
    }

    @Test
    public void shouldReturnColumnNames() {
        List<String> names = new ArrayList<String>();
        for (Column column : columnList) {
            names.add(columnNameFor(column));
        }
        assertThat(columnsWithScores.getColumnNames(), is(names));
        assertThat(columnsWithoutScores.getColumnNames(), is(names));
    }

    @Test
    public void shouldReturnCorrectIndexOfColumnGivenColumnName() {
        for (Column column : columnList) {
            assertThat(columnsWithoutScores.getColumnIndexForName(columnNameFor(column)), is(columnList.indexOf(column)));
        }
    }

    @Test
    public void shouldReturnCorrectIndexOfColumnGivenColumnSelectorAndPropertyName() {
        for (Column column : columnList) {
            assertThat(columnsWithoutScores.getColumnIndexForProperty(column.selectorName().name(), column.getPropertyName()),
                       is(columnList.indexOf(column)));
        }
    }

    @Test( expected = NoSuchElementException.class )
    public void shouldFailToFindIndexOfColumnGivenColumnNameWithIncorrectCase() {
        columnsWithScores.getColumnIndexForName("cola");
    }

    @Test( expected = NoSuchElementException.class )
    public void shouldFailToFindIndexOfColumnGivenNonExistantColumnName() {
        columnsWithScores.getColumnIndexForName("non-existant");
    }

    @Test( expected = NoSuchElementException.class )
    public void shouldFailToFindIndexOfColumnGivenNullColumnName() {
        columnsWithScores.getColumnIndexForName(null);
    }

    @Test( expected = NoSuchElementException.class )
    public void shouldFailToFindIndexOfColumnGivenUnusedSelectorName() {
        columnsWithScores.getColumnIndexForProperty("non-existant", "colA");
    }

    @Test( expected = NoSuchElementException.class )
    public void shouldFailToFindIndexOfColumnGivenNullSelectorName() {
        columnsWithScores.getColumnIndexForProperty(null, "colA");
    }

    @Test( expected = NoSuchElementException.class )
    public void shouldFailToFindIndexOfColumnGivenEmptySelectorName() {
        columnsWithScores.getColumnIndexForProperty("", "colA");
    }

    @Test( expected = NoSuchElementException.class )
    public void shouldFailToFindIndexOfColumnGivenUnusedPropertyNameName() {
        columnsWithScores.getColumnIndexForProperty("table1", "non-existant");
    }

    @Test( expected = NoSuchElementException.class )
    public void shouldFailToFindIndexOfColumnGivenNullPropertyNameName() {
        columnsWithScores.getColumnIndexForProperty("table1", null);
    }

    @Test
    public void shouldHaveCorrectNumberOfLocations() {
        assertThat(columnsWithScores.getLocationCount(), is(2));
        assertThat(columnsWithoutScores.getLocationCount(), is(2));
    }

    @Test
    public void shouldReturnCorrectIndexOfLocationGivenSelectorName() {
        assertThat(columnsWithoutScores.getLocationIndex("table1"), is(columnList.size() + 0));
        assertThat(columnsWithoutScores.getLocationIndex("table2"), is(columnList.size() + 1));
    }

    @Test( expected = NoSuchElementException.class )
    public void shouldFailToFindIndexOfLocationGivenUnusedSelectorName() {
        columnsWithScores.getLocationIndex("non-existant");
    }

    @Test( expected = NoSuchElementException.class )
    public void shouldFailToFindIndexOfLocationGivenNullSelectorName() {
        columnsWithScores.getLocationIndex(null);
    }

    @Test( expected = NoSuchElementException.class )
    public void shouldFailToFindIndexOfLocationGivenEmptySelectorName() {
        columnsWithScores.getLocationIndex("");
    }

    @Test
    public void shouldReturnCorrectIndexOfLocationGivenColumnName() {
        assertThat(columnsWithoutScores.getLocationIndexForColumn("table1.colA"), is(columnList.size() + 0));
        assertThat(columnsWithoutScores.getLocationIndexForColumn("table1.colB"), is(columnList.size() + 0));
        assertThat(columnsWithoutScores.getLocationIndexForColumn("table1.colC"), is(columnList.size() + 0));
        assertThat(columnsWithoutScores.getLocationIndexForColumn("colA2"), is(columnList.size() + 1));
        assertThat(columnsWithoutScores.getLocationIndexForColumn("colB2"), is(columnList.size() + 1));
        assertThat(columnsWithoutScores.getLocationIndexForColumn("table2.colX"), is(columnList.size() + 1));
    }

    @Test( expected = NoSuchElementException.class )
    public void shouldFailToFindIndexOfLocationGivenUnusedColumnName() {
        columnsWithScores.getLocationIndexForColumn("non-existant");
    }

    @Test( expected = NoSuchElementException.class )
    public void shouldFailToFindIndexOfLocationGivenNullColumnName() {
        columnsWithScores.getLocationIndexForColumn(null);
    }

    @Test( expected = NoSuchElementException.class )
    public void shouldFailToFindIndexOfLocationGivenEmptyColumnName() {
        columnsWithScores.getLocationIndexForColumn("");
    }

    @Test
    public void shouldReturnCorrectIndexOfLocationGivenColumnIndex() {
        assertThat(columnsWithoutScores.getLocationIndexForColumn(0), is(columnList.size() + 0));
        assertThat(columnsWithoutScores.getLocationIndexForColumn(1), is(columnList.size() + 0));
        assertThat(columnsWithoutScores.getLocationIndexForColumn(2), is(columnList.size() + 0));
        assertThat(columnsWithoutScores.getLocationIndexForColumn(3), is(columnList.size() + 1));
        assertThat(columnsWithoutScores.getLocationIndexForColumn(4), is(columnList.size() + 1));
        assertThat(columnsWithoutScores.getLocationIndexForColumn(5), is(columnList.size() + 1));
    }

    @Test( expected = IndexOutOfBoundsException.class )
    public void shouldFailToFindIndexOfLocationGivenColumnIndexEqualToOrLargerThanNumberOfColumns() {
        columnsWithScores.getLocationIndexForColumn(columnList.size());
    }

    @Test( expected = IndexOutOfBoundsException.class )
    public void shouldFailToFindIndexOfLocationGivenColumnIndexLessThanZero() {
        columnsWithScores.getLocationIndexForColumn(-1);
    }

    @Test
    public void shouldCorrectlyReportWhetherScoresAreIncluded() {
        assertThat(columnsWithScores.hasFullTextSearchScores(), is(true));
        assertThat(columnsWithoutScores.hasFullTextSearchScores(), is(false));
    }

    @Test
    public void shouldReturnCorrectIndexOfFullTextSearchScoreGivenSelectorName() {
        assertThat(columnsWithScores.getFullTextSearchScoreIndexFor("table1"), is(columnList.size() + 2 + 0));
        assertThat(columnsWithScores.getFullTextSearchScoreIndexFor("table2"), is(columnList.size() + 2 + 1));
    }

    @Test
    public void shouldReturnNegativeOneForIndexOfFullTextSearchScoreGivenValidSelectorNameButWhereNoScoresAreIncluded() {
        assertThat(columnsWithoutScores.getFullTextSearchScoreIndexFor("table1"), is(-1));
        assertThat(columnsWithoutScores.getFullTextSearchScoreIndexFor("table2"), is(-1));
    }

    @Test( expected = NoSuchElementException.class )
    public void shouldFailToFindIndexOfFullTextSearchScoreGivenUnusedSelectorName() {
        columnsWithScores.getFullTextSearchScoreIndexFor("non-existant");
    }

    @Test( expected = NoSuchElementException.class )
    public void shouldFailToFindIndexOfFullTextSearchScoreGivenNullSelectorName() {
        columnsWithScores.getFullTextSearchScoreIndexFor(null);
    }

    @Test( expected = NoSuchElementException.class )
    public void shouldFailToFindIndexOfFullTextSearchScoreGivenEmptySelectorName() {
        columnsWithScores.getFullTextSearchScoreIndexFor("");
    }

    @Test
    public void shouldIncludeSelf() {
        assertThat(columnsWithScores.includes(columnsWithScores), is(true));
        assertThat(columnsWithoutScores.includes(columnsWithoutScores), is(true));
    }

    @Test
    public void shouldIncludeColumnsObjectWithSubsetOfColumnObjectsAndIndependentOfFullTextSearchScores() {
        List<Column> subset = new ArrayList<Column>();
        subset.add(columnList.get(0));
        subset.add(columnList.get(1));
        subset.add(columnList.get(4));
        List<String> subsetTypes = new ArrayList<String>();
        subsetTypes.add(PropertyType.STRING.getName());
        subsetTypes.add(PropertyType.STRING.getName());
        subsetTypes.add(PropertyType.STRING.getName());
        Columns other = new QueryResultColumns(subset, subsetTypes, false);
        assertThat(columnsWithScores.includes(other), is(true));
        assertThat(columnsWithoutScores.includes(other), is(true));
        assertThat(columnsWithoutScores.includes(columnsWithScores), is(true));
    }

    @Test
    public void shouldEqualSelf() {
        assertThat(columnsWithScores.equals(columnsWithScores), is(true));
        assertThat(columnsWithoutScores.equals(columnsWithoutScores), is(true));
    }

    @Test
    public void shouldEqualIndependentOfInclusionOfFullTextSearchScores() {
        assertThat(columnsWithoutScores.equals(columnsWithScores), is(true));
    }

    @Test
    public void shouldNotBeUnionCompatibleUnlessBothHaveFullTextSearchScores() {
        Columns other = new QueryResultColumns(columnsWithoutScores.getColumns(), columnsWithoutScores.getColumnTypes(),
                                               !columnsWithoutScores.hasFullTextSearchScores());
        assertThat(columnsWithoutScores.isUnionCompatible(other), is(false));
    }

    @Test
    public void shouldNotBeUnionCompatibleUnlessBothDoNotHaveFullTextSearchScores() {
        Columns other = new QueryResultColumns(columnsWithScores.getColumns(), columnsWithoutScores.getColumnTypes(),
                                               !columnsWithScores.hasFullTextSearchScores());
        assertThat(columnsWithScores.isUnionCompatible(other), is(false));
    }

    @Test
    public void shouldBeUnionCompatibleWithEquivalentColumns() {
        List<Column> columnListCopy = new ArrayList<Column>();
        List<String> columnTypeCopy = new ArrayList<String>();
        for (Column column : columnsWithScores.getColumns()) {
            columnListCopy.add(new Column(column.selectorName(), column.getPropertyName(), column.getColumnName()));
            columnTypeCopy.add(PropertyType.STRING.getName());
        }
        Columns other = new QueryResultColumns(columnListCopy, columnTypeCopy, columnsWithScores.hasFullTextSearchScores());
        assertThat(columnsWithScores.isUnionCompatible(other), is(true));
    }

    @Test
    public void shouldNotBeUnionCompatibleWithSubsetOfColumns() {
        List<Column> columnListCopy = new ArrayList<Column>();
        List<String> columnTypeCopy = new ArrayList<String>();
        for (Column column : columnsWithScores.getColumns()) {
            columnListCopy.add(new Column(column.selectorName(), column.getPropertyName(), column.getColumnName()));
            columnTypeCopy.add(PropertyType.STRING.getName());
        }
        columnListCopy.remove(3);
        columnTypeCopy.remove(3);
        Columns other = new QueryResultColumns(columnListCopy, columnTypeCopy, columnsWithScores.hasFullTextSearchScores());
        assertThat(columnsWithScores.isUnionCompatible(other), is(false));
    }

    @Test
    public void shouldNotBeUnionCompatibleWithExtraColumns() {
        List<Column> columnListCopy = new ArrayList<Column>();
        List<String> columnTypeCopy = new ArrayList<String>();
        for (Column column : columnsWithScores.getColumns()) {
            columnListCopy.add(new Column(column.selectorName(), column.getPropertyName(), column.getColumnName()));
            columnTypeCopy.add(PropertyType.STRING.getName());
        }
        columnListCopy.add(new Column(selector("table2"), "colZ", "colZ"));
        columnTypeCopy.add(PropertyType.STRING.getName());
        Columns other = new QueryResultColumns(columnListCopy, columnTypeCopy, columnsWithScores.hasFullTextSearchScores());
        assertThat(columnsWithScores.isUnionCompatible(other), is(false));
    }

    @Test
    public void shouldBeUnionCompatibleWithSameColumns() {
        Columns other = new QueryResultColumns(columnsWithScores.getColumns(), columnsWithScores.getColumnTypes(),
                                               columnsWithScores.hasFullTextSearchScores());
        assertThat(columnsWithScores.isUnionCompatible(other), is(true));
    }

    @Test
    public void shouldHaveToString() {
        columnsWithScores.toString();
        columnsWithoutScores.toString();
    }
}
