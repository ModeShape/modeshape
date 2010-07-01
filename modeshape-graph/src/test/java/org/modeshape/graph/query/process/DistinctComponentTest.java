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
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.graph.property.PropertyType;
import org.modeshape.graph.query.QueryContext;
import org.modeshape.graph.query.QueryResults.Columns;
import org.modeshape.graph.query.validate.Schemata;

/**
 * 
 */
public class DistinctComponentTest extends AbstractQueryResultsTest {

    private DistinctComponent component;
    private QueryContext context;
    private Columns columns;
    private List<Object[]> inputTuples;

    @Before
    public void beforeEach() {
        context = new QueryContext(mock(Schemata.class), typeSystem);
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
        component = new DistinctComponent(delegate);
    }

    @Test
    public void shouldReturnAllResultsIfThereAreNoDuplicatesInTuplesContainingOneLocation() {
        inputTuples.add(tuple(columns, "/a/b/c1", "v1", "v2", "v3"));
        inputTuples.add(tuple(columns, "/a/b/c4", "v1", "v2", "v3"));
        inputTuples.add(tuple(columns, "/a/b/c2", "v1", "v2", "v3"));
        inputTuples.add(tuple(columns, "/a/b/c3", "v1", "v2", "v3"));
        assertThat(component.execute(), is(inputTuples));
    }

    @Test
    public void shouldReturnNoDuplicatesFromTuplesContainingOneLocation() {
        inputTuples.add(tuple(columns, "/a/b/c1", "v1", "v2", "v3"));
        inputTuples.add(tuple(columns, "/a/b/c4", "v1", "v2", "v3"));
        inputTuples.add(tuple(columns, "/a/b/c2", "v1", "v2", "v3"));
        inputTuples.add(tuple(columns, "/a/b/c3", "v1", "v2", "v3"));
        inputTuples.add(tuple(columns, "/a/b/c4", "v1", "v2", "v3"));
        inputTuples.add(tuple(columns, "/a/b/c3", "v1", "v2", "v3"));
        inputTuples.add(tuple(columns, "/a/b/c0", "v1", "v2", "v3"));
        List<Object[]> expected = new ArrayList<Object[]>(inputTuples);
        expected.remove(5);
        expected.remove(4);
        assertThat(component.execute(), is(expected));
    }

    @Test
    public void shouldReturnEmptyResultsWhenDelegateReturnsEmptyResults() {
        assertThat(component.execute().isEmpty(), is(true));
    }
}
