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
import org.modeshape.jcr.query.validate.Schemata;
import org.modeshape.jcr.value.PropertyType;

/**
 * 
 */
public class SortLocationsComponentTest extends AbstractQueryResultsTest {

    private SortLocationsComponent component;
    private QueryContext context;
    private Columns columns;
    private List<Object[]> inputTuples;

    @Before
    public void beforeEach() {
        Schemata schemata = mock(Schemata.class);
        context = new QueryContext(executionContext, mock(RepositoryCache.class), Collections.singleton("workspace"), schemata);
        inputTuples = new ArrayList<Object[]>();
        // Define the columns for the results ...
        columns = resultColumns("Selector1",
                                new String[] {"ColA", "ColB", "ColC"},
                                PropertyType.STRING,
                                PropertyType.STRING,
                                PropertyType.STRING);
        // And define the delegating component ...
        ProcessingComponent delegate = new ProcessingComponent(context, columns) {
            @SuppressWarnings( "synthetic-access" )
            @Override
            public List<Object[]> execute() {
                return new ArrayList<Object[]>(inputTuples);
            }
        };
        // Create the component we're testing ...
        component = new SortLocationsComponent(delegate);
    }

    @Test
    public void shouldReturnAllResultsInPathOrderForTuplesContainingOneLocation() {
        inputTuples.add(tuple(columns, "/a/b/c1", "v1", "v2", "v3"));
        inputTuples.add(tuple(columns, "/a/b/c4", "v1", "v2", "v3"));
        inputTuples.add(tuple(columns, "/a/b/c2", "v1", "v2", "v3"));
        inputTuples.add(tuple(columns, "/a/b/c3", "v1", "v2", "v3"));
        List<Object[]> expected = new ArrayList<Object[]>();
        expected.add(inputTuples.get(0));
        expected.add(inputTuples.get(2));
        expected.add(inputTuples.get(3));
        expected.add(inputTuples.get(1));
        assertThat(component.execute(), is(expected));
    }

    @Test
    public void shouldReturnAllResultsInPathOrderForTuplesContainingOneLocationAndWithDuplicateTuples() {
        inputTuples.add(tuple(columns, "/a/b/c1", "v1", "v2", "v3"));
        inputTuples.add(tuple(columns, "/a/b/c4", "v1", "v2", "v3"));
        inputTuples.add(tuple(columns, "/a/b/c2", "v1", "v2", "v3"));
        inputTuples.add(tuple(columns, "/a/b/c3", "v1", "v2", "v3"));
        inputTuples.add(tuple(columns, "/a/b/c4", "v1", "v2", "v3"));
        inputTuples.add(tuple(columns, "/a/b/c3", "v1", "v2", "v3"));
        inputTuples.add(tuple(columns, "/a/b/c0", "v1", "v2", "v3"));
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
    public void shouldReturnEmptyResultsWhenDelegateReturnsEmptyResults() {
        assertThat(component.execute().isEmpty(), is(true));
    }
}
