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
import org.modeshape.jcr.query.model.Constraint;
import org.modeshape.jcr.query.model.PropertyExistence;
import org.modeshape.jcr.query.validate.Schemata;
import org.modeshape.jcr.value.PropertyType;

public class SetOperationComponentsTest extends AbstractQueryResultsTest {

    private SetOperationComponent component;
    private List<ProcessingComponent> selects;
    private QueryContext context;
    private Columns columns;
    private List<Object[]> tuplesA;
    private List<Object[]> tuplesB;

    @Before
    public void beforeEach() {
        Schemata schemata = mock(Schemata.class);
        context = new QueryContext(executionContext, mock(RepositoryCache.class), Collections.singleton("workspace"), schemata);

        tuplesA = new ArrayList<Object[]>();
        tuplesB = new ArrayList<Object[]>();

        // Define the columns for the results ...
        columns = resultColumns("Selector1",
                                new String[] {"ColA", "ColB", "ColC"},
                                PropertyType.STRING,
                                PropertyType.STRING,
                                PropertyType.STRING);

        // And define the delegating components ...
        ProcessingComponent delegateA = new ProcessingComponent(context, columns) {
            @SuppressWarnings( "synthetic-access" )
            @Override
            public List<Object[]> execute() {
                return new ArrayList<Object[]>(tuplesA);
            }
        };

        ProcessingComponent delegateB = new ProcessingComponent(context, columns) {
            @SuppressWarnings( "synthetic-access" )
            @Override
            public List<Object[]> execute() {
                return new ArrayList<Object[]>(tuplesB);
            }
        };

        Constraint constraint = new PropertyExistence(selector("Selector1"), "ColA");

        // Create the sets ...
        selects = new ArrayList<ProcessingComponent>();

        selects.add(new SelectComponent(delegateA, constraint, null));
        selects.add(new SelectComponent(delegateB, constraint, null));
    }

    @Test
    public void shouldUnionResultsForCompatibleSets() {
        tuplesA.add(tuple(columns, "/a/b/c1", "v1", "v2", "v3"));
        tuplesA.add(tuple(columns, "/a/b/c2", "v1", "v2", "v3"));
        tuplesB.add(tuple(columns, "/a/b/c1", "v1", "v2", "v3"));
        tuplesB.add(tuple(columns, "/a/b/c3", "v4", "v5", "v6"));

        component = new UnionComponent(context, columns, selects, false, false);

        final List<Object[]> results = component.execute();

        // Should remove the duplicate row (row 0 in tuplesB) for 3 total.
        assertThat(results.size(), is(3));
    }

    @Test
    public void shouldUnionAllResultsForCompatibleSets() {
        tuplesA.add(tuple(columns, "/a/b/c1", "v1", "v2", "v3"));
        tuplesA.add(tuple(columns, "/a/b/c2", "v1", "v2", "v3"));
        tuplesB.add(tuple(columns, "/a/b/c1", "v1", "v2", "v3"));
        tuplesB.add(tuple(columns, "/a/b/c3", "v4", "v5", "v6"));

        component = new UnionComponent(context, columns, selects, false, true);

        final List<Object[]> results = component.execute();

        // Should keep all 4 rows.
        assertThat(results.size(), is(4));
    }

    @Test
    public void shouldIntersectResultsForCompatibleSets() {
        tuplesA.add(tuple(columns, "/a/b/c1", "v1", "v2", "v3"));
        tuplesA.add(tuple(columns, "/a/b/c2", "v1", "v2", "v3"));
        tuplesB.add(tuple(columns, "/a/b/c1", "v1", "v2", "v3"));
        tuplesB.add(tuple(columns, "/a/b/c3", "v4", "v5", "v6"));

        component = new IntersectComponent(context, columns, selects, false, false);

        final List<Object[]> results = component.execute();

        // Should get a single row /a/b/c1, v1, v2, v3
        assertThat(results.size(), is(1));
    }

    @Test
    public void shouldIntersectAllResultsForCompatibleSets() {
        tuplesA.add(tuple(columns, "/a/b/c1", "v1", "v2", "v3"));
        tuplesA.add(tuple(columns, "/a/b/c2", "v1", "v2", "v3"));
        tuplesB.add(tuple(columns, "/a/b/c1", "v1", "v2", "v3"));

        /*
        *  Note that unlike the previous sample sets this entry is pathed to
        *  /c2 in order to duplicate the c2 entry for tuplesA, but the fields
        *  are different. The comparator used by the IntersectComponent (and
        *  all other SetOperationComponents) compares on path ONLY.
        */

        tuplesB.add(tuple(columns, "/a/b/c2", "v4", "v5", "v6"));

        component = new IntersectComponent(context, columns, selects, false, true);

        final List<Object[]> results = component.execute();

        // Should get double rows /a/b/c1, v1, v2, v3
        assertThat(results.size(), is(2));
    }

    @Test
    public void shouldExceptResultsForCompatibleSets() {
        tuplesA.add(tuple(columns, "/a/b/c1", "v1", "v2", "v3"));
        tuplesA.add(tuple(columns, "/a/b/c2", "v1", "v2", "v3"));
        tuplesB.add(tuple(columns, "/a/b/c1", "v1", "v2", "v3"));
        tuplesB.add(tuple(columns, "/a/b/c3", "v4", "v5", "v6"));

        component = new ExceptComponent(context, columns, selects, false, false);

        final List<Object[]> results = component.execute();

        // Should get single row /a/b/c2, v1, v2, v3 from tuplesA
        assertThat(results.size(), is(1));
    }

    @Test
    public void shouldExceptAllResultsForCompatibleSets() {
        // In order to test Except All, the A set needs to contain duplicates.
        tuplesA.add(tuple(columns, "/a/b/c1", "v1", "v2", "v3"));
        tuplesA.add(tuple(columns, "/a/b/c1", "v1", "v2", "v3"));
        tuplesB.add(tuple(columns, "/a/b/c2", "v1", "v2", "v3"));
        tuplesB.add(tuple(columns, "/a/b/c3", "v4", "v5", "v6"));

        component = new ExceptComponent(context, columns, selects, false, true);

        final List<Object[]> results = component.execute();

        // Should get both duplicate rows from tuplesA
        assertThat(results.size(), is(2));
    }

}
