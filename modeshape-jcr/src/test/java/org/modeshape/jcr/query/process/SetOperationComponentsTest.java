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
