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
import static org.mockito.Mockito.mock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.cache.RepositoryCache;
import org.modeshape.jcr.query.QueryContext;
import org.modeshape.jcr.query.QueryResults.Columns;
import org.modeshape.jcr.query.model.Ordering;
import org.modeshape.jcr.query.validate.Schemata;
import org.modeshape.jcr.value.PropertyType;

/**
 * 
 */
public class SortValuesComponentTest extends AbstractQueryResultsTest {

    private SortValuesComponent component;
    private ProcessingComponent delegate;
    private QueryContext context;
    private Schemata schemata;
    private Columns columns;
    private List<Object[]> inputTuples;
    private List<Ordering> orderings;

    @Before
    public void beforeEach() {
        // Define the columns for the results ...
        columns = resultColumns("Selector1",
                                new String[] {"ColA", "ColB", "ColC"},
                                PropertyType.STRING,
                                PropertyType.LONG,
                                PropertyType.STRING);
        schemata = schemataFor(columns, PropertyType.STRING, PropertyType.LONG, PropertyType.STRING);
        // Define the context ...
        context = new QueryContext(executionContext, mock(RepositoryCache.class), Collections.singleton("workspace"), schemata);
        inputTuples = new ArrayList<Object[]>();
        // And define the delegating component ...
        delegate = new ProcessingComponent(context, columns) {
            @SuppressWarnings( "synthetic-access" )
            @Override
            public List<Object[]> execute() {
                return new ArrayList<Object[]>(inputTuples);
            }
        };
        // Create the component we're testing ...
        orderings = new ArrayList<Ordering>();
        component = new SortValuesComponent(delegate, orderings, null);
    }

    @Test
    public void shouldReturnAllResultsOrderedByNodeName() {
        orderings.add(orderByNodeName("Selector1"));
        component = new SortValuesComponent(delegate, orderings, null);
        inputTuples.add(tuple(columns, "/a/b1/c1", "v1", 100, "v4"));
        inputTuples.add(tuple(columns, "/a/b2/c4", "v4", 100, "v3"));
        inputTuples.add(tuple(columns, "/a/b3/c2", 100, 100, "v2"));
        inputTuples.add(tuple(columns, "/a/b4/c3", "v3", 100, "v1"));
        List<Object[]> expected = new ArrayList<Object[]>();
        expected.add(inputTuples.get(0));
        expected.add(inputTuples.get(2));
        expected.add(inputTuples.get(3));
        expected.add(inputTuples.get(1));
        assertThat(component.execute(), is(expected));
    }

    @Test
    public void shouldReturnAllResultsOrderedByNodeNameWhenThereAreDuplicateTuples() {
        orderings.add(orderByNodeName("Selector1"));
        component = new SortValuesComponent(delegate, orderings, null);
        inputTuples.add(tuple(columns, "/a/b1/c1", "v1", 100, "v3"));
        inputTuples.add(tuple(columns, "/a/b2/c4", "v1", 100, "v3"));
        inputTuples.add(tuple(columns, "/a/b3/c2", "v1", 100, "v3"));
        inputTuples.add(tuple(columns, "/a/b4/c3", "v1", 100, "v3"));
        inputTuples.add(tuple(columns, "/a/b5/c4", "v1", 100, "v3"));
        inputTuples.add(tuple(columns, "/a/b6/c3", "v1", 100, "v3"));
        inputTuples.add(tuple(columns, "/a/b7/c0", "v1", 100, "v3"));
        List<Object[]> expected = new ArrayList<Object[]>();
        expected.add(inputTuples.get(6));
        expected.add(inputTuples.get(0));
        expected.add(inputTuples.get(2));
        expected.add(inputTuples.get(3));
        expected.add(inputTuples.get(5));
        expected.add(inputTuples.get(1));
        expected.add(inputTuples.get(4));
        assertThat(component.execute(), is(expected));
    }

    @Test
    public void shouldReturnAllResultsOrderedByNodeDepth() {
        orderings.add(orderByNodeDepth("Selector1"));
        component = new SortValuesComponent(delegate, orderings, null);
        inputTuples.add(tuple(columns, "/a/b1", "v1", 100, "v4"));
        inputTuples.add(tuple(columns, "/a/b2/c4", "v4", 100, "v3"));
        inputTuples.add(tuple(columns, "/a", 100, 100, "v2"));
        inputTuples.add(tuple(columns, "/a/b4/c3", "v3", 100, "v1"));
        List<Object[]> expected = new ArrayList<Object[]>();
        expected.add(inputTuples.get(2));
        expected.add(inputTuples.get(0));
        expected.add(inputTuples.get(1));
        expected.add(inputTuples.get(3));
        assertThat(component.execute(), is(expected));
    }

    @Test
    public void shouldReturnAllResultsOrderedByNodePath() {
        orderings.add(orderByNodePath("Selector1"));
        component = new SortValuesComponent(delegate, orderings, null);
        inputTuples.add(tuple(columns, "/a/b1", "v1", 100, "v4"));
        inputTuples.add(tuple(columns, "/a/b1/c4[2]", "v4", 100, "v3"));
        inputTuples.add(tuple(columns, "/a/b1/c2", 100, 100, "v2"));
        inputTuples.add(tuple(columns, "/a/b1/c4", "v3", 100, "v1"));
        List<Object[]> expected = new ArrayList<Object[]>();
        expected.add(inputTuples.get(0));
        expected.add(inputTuples.get(2));
        expected.add(inputTuples.get(3));
        expected.add(inputTuples.get(1));
        assertThat(component.execute(), is(expected));
    }

    @Test
    public void shouldReturnAllResultsOrderedByNodeLocalName() {
        orderings.add(orderByNodeLocalName("Selector1"));
        component = new SortValuesComponent(delegate, orderings, null);
        inputTuples.add(tuple(columns, "/a/b1/c1", "v1", 100, "v4"));
        inputTuples.add(tuple(columns, "/a/b2/c4", "v4", 100, "v3"));
        inputTuples.add(tuple(columns, "/a/b3/c2", 100, 100, "v2"));
        inputTuples.add(tuple(columns, "/a/b4/c3", "v3", 100, "v1"));
        List<Object[]> expected = new ArrayList<Object[]>();
        expected.add(inputTuples.get(0));
        expected.add(inputTuples.get(2));
        expected.add(inputTuples.get(3));
        expected.add(inputTuples.get(1));
        assertThat(component.execute(), is(expected));
    }

    @Test
    public void shouldReturnAllResultsOrderedByNodeLocalNameWhenThereAreDuplicateTuples() {
        orderings.add(orderByNodeName("Selector1"));
        component = new SortValuesComponent(delegate, orderings, null);
        inputTuples.add(tuple(columns, "/a/b1/mode:c1", "v1", 100, "v3"));
        inputTuples.add(tuple(columns, "/a/b2/mode:c4", "v1", 100, "v3"));
        inputTuples.add(tuple(columns, "/a/b3/mode:c2", "v1", 100, "v3"));
        inputTuples.add(tuple(columns, "/a/b4/mode:c3", "v1", 100, "v3"));
        inputTuples.add(tuple(columns, "/a/b5/jcr:c4", "v1", 100, "v3"));
        inputTuples.add(tuple(columns, "/a/b6/mode:c3", "v1", 100, "v3"));
        inputTuples.add(tuple(columns, "/a/b7/mode:c0", "v1", 100, "v3"));
        List<Object[]> expected = new ArrayList<Object[]>();
        expected.add(inputTuples.get(4));
        expected.add(inputTuples.get(6));
        expected.add(inputTuples.get(0));
        expected.add(inputTuples.get(2));
        expected.add(inputTuples.get(3));
        expected.add(inputTuples.get(5));
        expected.add(inputTuples.get(1));
        assertThat(component.execute(), is(expected));
    }

    @Test
    public void shouldReturnAllResultsOrderedByValueLengthOfLong() {
        orderings.add(orderByPropertyLength(columns.getColumns().get(1)));
        component = new SortValuesComponent(delegate, orderings, null);
        inputTuples.add(tuple(columns, "/a/b/c1", "v1", 1L, "v4"));
        inputTuples.add(tuple(columns, "/a/b/c4", "v1", 1114L, "v3"));
        inputTuples.add(tuple(columns, "/a/b/c2", "v1", 113L, "v2"));
        inputTuples.add(tuple(columns, "/a/b/c3", "v1", 12L, "v1"));
        List<Object[]> expected = new ArrayList<Object[]>();
        expected.add(inputTuples.get(0));
        expected.add(inputTuples.get(3));
        expected.add(inputTuples.get(2));
        expected.add(inputTuples.get(1));
        assertThat(component.execute(), is(expected));
    }

    @Test
    public void shouldReturnAllResultsOrderedByValueLengthOfString() {
        orderings.add(orderByPropertyLength(columns.getColumns().get(0)));
        component = new SortValuesComponent(delegate, orderings, null);
        inputTuples.add(tuple(columns, "/a/b/c1", "v1", 100L, "v4"));
        inputTuples.add(tuple(columns, "/a/b/c4", "v1111", 100L, "v3"));
        inputTuples.add(tuple(columns, "/a/b/c2", "v111", 100L, "v2"));
        inputTuples.add(tuple(columns, "/a/b/c3", "v11", 100L, "v1"));
        List<Object[]> expected = new ArrayList<Object[]>();
        expected.add(inputTuples.get(0));
        expected.add(inputTuples.get(3));
        expected.add(inputTuples.get(2));
        expected.add(inputTuples.get(1));
        assertThat(component.execute(), is(expected));
    }

    @Test
    public void shouldReturnAllResultsInSuppliedOrderWhenThereAreNoOrderings() {
        orderings.clear();
        component = new SortValuesComponent(delegate, orderings, null);
        inputTuples.add(tuple(columns, "/a/b/c1", "v1", 100L, "v3"));
        inputTuples.add(tuple(columns, "/a/b/c4", "v1", 100L, "v3"));
        inputTuples.add(tuple(columns, "/a/b/c2", "v1", 100L, "v3"));
        inputTuples.add(tuple(columns, "/a/b/c3", "v1", 100L, "v3"));
        inputTuples.add(tuple(columns, "/a/b/c4", "v1", 100L, "v3"));
        inputTuples.add(tuple(columns, "/a/b/c3", "v1", 100L, "v3"));
        inputTuples.add(tuple(columns, "/a/b/c0", "v1", 100L, "v3"));
        List<Object[]> expected = new ArrayList<Object[]>(inputTuples);
        assertThat(component.execute(), is(expected));
    }

    @Test
    public void shouldReturnEmptyResultsWhenDelegateReturnsEmptyResults() {
        assertThat(component.execute().isEmpty(), is(true));
    }
}
