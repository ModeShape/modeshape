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
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.query.AbstractQueryTest;
import org.modeshape.jcr.query.QueryResults.Columns;
import org.modeshape.jcr.query.QueryResults.Location;
import org.modeshape.jcr.query.model.Column;
import org.modeshape.jcr.query.model.FullTextSearchScore;
import org.modeshape.jcr.query.model.Length;
import org.modeshape.jcr.query.model.NodeDepth;
import org.modeshape.jcr.query.model.NodeLocalName;
import org.modeshape.jcr.query.model.NodeName;
import org.modeshape.jcr.query.model.NodePath;
import org.modeshape.jcr.query.model.Order;
import org.modeshape.jcr.query.model.Ordering;
import org.modeshape.jcr.query.model.PropertyValue;
import org.modeshape.jcr.query.model.SelectorName;
import org.modeshape.jcr.query.model.TypeSystem;
import org.modeshape.jcr.query.validate.ImmutableSchemata;
import org.modeshape.jcr.query.validate.Schemata;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.PropertyType;

/**
 * 
 */
public abstract class AbstractQueryResultsTest extends AbstractQueryTest {

    protected ExecutionContext executionContext = ExecutionContext.DEFAULT_CONTEXT;
    protected TypeSystem typeSystem = executionContext.getValueFactories().getTypeSystem();

    protected Path path( String name ) {
        return executionContext.getValueFactories().getPathFactory().create(name);
    }

    protected Schemata schemataFor( Columns columns ) {
        return schemataFor(columns, new PropertyType[] {});
    }

    protected Schemata schemataFor( Columns columns,
                                    PropertyType... types ) {
        ImmutableSchemata.Builder builder = ImmutableSchemata.createBuilder(executionContext);
        for (String selectorName : columns.getSelectorNames()) {
            final SelectorName selector = selector(selectorName);
            int i = 0;
            for (Column column : columns.getColumns()) {
                final String name = column.getColumnName();
                final PropertyType type = types != null && types.length > i && types[i] != null ? types[i] : PropertyType.STRING;
                if (column.selectorName().equals(selector)) {
                    builder.addColumn(selectorName, name, type.getName().toUpperCase());
                    ++i;
                }
            }
        }
        return builder.build();
    }

    protected Columns resultColumns( String selectorName,
                                     String[] columnNames,
                                     PropertyType... columnTypes ) {
        // Define the columns ...
        List<Column> columnObj = new ArrayList<Column>();
        List<String> types = new ArrayList<String>();
        SelectorName selector = selector(selectorName);
        int i = 0;
        for (String columnName : columnNames) {
            columnObj.add(new Column(selector, columnName, columnName));
            types.add(columnTypes[i++].getName());
        }
        return new QueryResultColumns(columnObj, types, false);
    }

    protected Columns resultColumnsWithSearchResults( String selectorName,
                                                      String... columnNames ) {
        // Define the columns ...
        List<Column> columnObj = new ArrayList<Column>();
        List<String> types = new ArrayList<String>();
        SelectorName selector = selector(selectorName);
        for (String columnName : columnNames) {
            columnObj.add(new Column(selector, columnName, columnName));
            types.add(PropertyType.STRING.getName());
        }
        return new QueryResultColumns(columnObj, types, true);
    }

    protected Object[] tuple( Columns columns,
                              String path,
                              Object... values ) {
        return tuple(columns, path(path), values);
    }

    protected Object[] tuple( Columns columns,
                              Path path,
                              Object... values ) {
        return tuple(columns, new Location(path), values);
    }

    protected Object[] tuple( Columns columns,
                              String[] paths,
                              Object... values ) {
        Location[] locations = new Location[paths.length];
        for (int i = 0; i != paths.length; ++i) {
            locations[i] = new Location(path(paths[i]));
        }
        return tuple(columns, locations, values);
    }

    protected Object[] tuple( Columns columns,
                              Path[] paths,
                              Object... values ) {
        Location[] locations = new Location[paths.length];
        for (int i = 0; i != paths.length; ++i) {
            locations[i] = new Location(paths[i]);
        }
        return tuple(columns, locations, values);
    }

    protected Object[] tuple( Columns columns,
                              Location location,
                              Object... values ) {
        return tuple(columns, new Location[] {location}, values);
    }

    protected Object[] tuple( Columns columns,
                              Location[] locations,
                              Object... values ) {
        Object[] results = new Object[columns.getTupleSize()];
        // Set the column values ...
        assertThat(values.length, is(columns.getColumnCount()));
        for (int i = 0; i != columns.getColumnCount(); ++i) {
            results[i] = values[i];
        }
        // Set the location ...
        int index = columns.getColumnCount();
        for (Location location : locations) {
            results[index++] = location;
        }
        // Set the full-text-search results ...
        if (columns.hasFullTextSearchScores()) {
            results[index++] = 1.0;
        }
        return results;
    }

    protected Object[] tuple( Columns columns,
                              Location[] locations,
                              double[] scores,
                              Object... values ) {
        assertThat(values.length, is(columns.getColumnCount()));
        assertThat(locations.length, is(columns.getLocationCount()));
        assertThat(scores.length, is(columns.getLocationCount()));
        Object[] results = new Object[columns.getTupleSize()];
        // Set the column values ...
        for (int i = 0; i != columns.getColumnCount(); ++i) {
            results[i] = values[i];
        }
        // Set the location ...
        int index = columns.getColumnCount();
        for (Location location : locations) {
            results[index++] = location;
        }
        // Set the full-text-search results ...
        if (columns.hasFullTextSearchScores()) {
            for (double score : scores) {
                results[index++] = score;
            }
        }
        return results;
    }

    protected Ordering orderByPropertyValue( Column column ) {
        return orderByPropertyValue(column, Order.ASCENDING);
    }

    protected Ordering orderByPropertyValue( Column column,
                                             Order order ) {
        return new Ordering(new PropertyValue(column.selectorName(), column.getPropertyName()), order);
    }

    protected Ordering orderByPropertyLength( Column column ) {
        return orderByPropertyValue(column, Order.ASCENDING);
    }

    protected Ordering orderByPropertyLength( Column column,
                                              Order order ) {
        return new Ordering(new Length(new PropertyValue(column.selectorName(), column.getPropertyName())), order);
    }

    protected Ordering orderByNodeDepth( String selectorName ) {
        return orderByNodeDepth(selectorName, Order.ASCENDING);
    }

    protected Ordering orderByNodeDepth( String selectorName,
                                         Order order ) {
        return new Ordering(new NodeDepth(selector(selectorName)), order);
    }

    protected Ordering orderByNodePath( String selectorName ) {
        return orderByNodePath(selectorName, Order.ASCENDING);
    }

    protected Ordering orderByNodePath( String selectorName,
                                        Order order ) {
        return new Ordering(new NodePath(selector(selectorName)), order);
    }

    protected Ordering orderByNodeName( String selectorName ) {
        return orderByNodeName(selectorName, Order.ASCENDING);
    }

    protected Ordering orderByNodeName( String selectorName,
                                        Order order ) {
        return new Ordering(new NodeName(selector(selectorName)), order);
    }

    protected Ordering orderByNodeLocalName( String selectorName ) {
        return orderByNodeLocalName(selectorName, Order.ASCENDING);
    }

    protected Ordering orderByNodeLocalName( String selectorName,
                                             Order order ) {
        return new Ordering(new NodeLocalName(selector(selectorName)), order);
    }

    protected Ordering orderByFullTextSearchScore( String selectorName ) {
        return orderByFullTextSearchScore(selectorName, Order.ASCENDING);
    }

    protected Ordering orderByFullTextSearchScore( String selectorName,
                                                   Order order ) {
        return new Ordering(new FullTextSearchScore(selector(selectorName)), order);
    }

}
