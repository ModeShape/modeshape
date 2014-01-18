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
import static org.junit.Assert.assertThat;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.query.QueryResults.Columns;
import org.modeshape.jcr.query.QueryResults.Location;
import org.modeshape.jcr.query.model.Column;
import org.modeshape.jcr.query.model.SelectorName;
import org.modeshape.jcr.query.process.JoinComponent.TupleMerger;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.PropertyType;

public class JoinComponentTest {

    private ExecutionContext context;
    private Columns mergedColumns;
    private Columns leftColumns;
    private Columns rightColumns;
    private boolean print = false;

    @Before
    public void beforeEach() {
        context = new ExecutionContext();
    }

    protected Columns columns( String tableName,
                               String... columnNames ) {
        List<Column> columnList = columnList(tableName, columnNames);
        List<String> columnTypes = typesFor(columnList);
        return new QueryResultColumns(columnList, columnTypes, false);
    }

    protected Columns columnsWithScores( String tableName,
                                         String... columnNames ) {
        List<Column> columnList = columnList(tableName, columnNames);
        List<String> columnTypes = typesFor(columnList);
        return new QueryResultColumns(columnList, columnTypes, true);
    }

    protected List<String> typesFor( List<Column> columns ) {
        List<String> types = new ArrayList<String>();
        for (int i = 0; i != columns.size(); ++i) {
            types.add(PropertyType.STRING.getName());
        }
        return types;
    }

    protected List<Column> columnList( String tableName,
                                       String... columnNames ) {
        List<Column> columns = new ArrayList<Column>();
        SelectorName selectorName = new SelectorName(tableName);
        for (String columnName : columnNames) {
            columns.add(new Column(selectorName, columnName, columnName));
        }
        return columns;
    }

    protected Path path( String path ) {
        return context.getValueFactories().getPathFactory().create(path);
    }

    protected Location location( String path ) {
        return new Location(path(path));
    }

    protected String toString( Object[] tuple ) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        boolean first = true;
        for (Object value : tuple) {
            if (first) first = false;
            else sb.append(", ");
            sb.append(value);
        }
        sb.append(']');
        return sb.toString();
    }

    protected void print( Object object ) {
        if (print) {
            if (object instanceof Object[]) {
                System.out.println(toString((Object[])object));
            } else {
                System.out.println(object);
            }
        }
    }

    @Test
    public void shouldCreateMergerAndThenMergeTuplesForColumnsWithoutFullTextScore() {
        List<Column> columns = columnList("t1", "c11", "c12", "c13");
        columns.addAll(columnList("t2", "c21", "c22", "c23"));
        mergedColumns = new QueryResultColumns(columns, typesFor(columns), false);

        leftColumns = columns("t1", "c11", "c12", "c13");
        rightColumns = columns("t2", "c21", "c22", "c23");
        print(mergedColumns);
        print(leftColumns);
        print(rightColumns);

        TupleMerger merger = JoinComponent.createMerger(mergedColumns, leftColumns, rightColumns);
        assertThat(merger, is(notNullValue()));

        Location leftLocation = location("/a/b/c");
        Location rightLocation = location("/a/b/c/d");
        Object[] leftTuple = {"vc11", "vc12", "vc3", leftLocation};
        Object[] rightTuple = {"vc21", "vc22", "vc3", rightLocation};
        Object[] merged = merger.merge(leftTuple, rightTuple);
        print(merged);
        Object[] expectedTuple = {"vc11", "vc12", "vc3", "vc21", "vc22", "vc3", leftLocation, rightLocation};
        assertThat(merged, is(expectedTuple));
    }

    @Test
    public void shouldCreateMergerAndThenMergeTuplesForColumnsWithFullTextScore() {
        List<Column> columns = columnList("t1", "c11", "c12", "c13");
        columns.addAll(columnList("t2", "c21", "c22", "c23"));
        mergedColumns = new QueryResultColumns(columns, typesFor(columns), true);

        leftColumns = columnsWithScores("t1", "c11", "c12", "c13");
        rightColumns = columnsWithScores("t2", "c21", "c22", "c23");
        print(mergedColumns);
        print(leftColumns);
        print(rightColumns);

        TupleMerger merger = JoinComponent.createMerger(mergedColumns, leftColumns, rightColumns);
        assertThat(merger, is(notNullValue()));

        Location leftLocation = location("/a/b/c");
        Location rightLocation = location("/a/b/c/d");
        Object[] leftTuple = {"vc11", "vc12", "vc3", leftLocation, 1.0};
        Object[] rightTuple = {"vc21", "vc22", "vc3", rightLocation, 2.0};
        Object[] merged = merger.merge(leftTuple, rightTuple);
        print(merged);
        Object[] expectedTuple = {"vc11", "vc12", "vc3", "vc21", "vc22", "vc3", leftLocation, rightLocation, 1.0, 2.0};
        assertThat(merged, is(expectedTuple));
    }

}
