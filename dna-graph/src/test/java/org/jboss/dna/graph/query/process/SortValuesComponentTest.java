/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.graph.query.process;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.util.ArrayList;
import java.util.List;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.property.PropertyType;
import org.jboss.dna.graph.query.QueryContext;
import org.jboss.dna.graph.query.QueryResults.Columns;
import org.jboss.dna.graph.query.model.Ordering;
import org.jboss.dna.graph.query.plan.PlanHints;
import org.jboss.dna.graph.query.process.ProcessingComponent;
import org.jboss.dna.graph.query.process.SortValuesComponent;
import org.jboss.dna.graph.query.validate.Schemata;
import org.junit.Before;
import org.junit.Test;

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
        columns = resultColumns("Selector1", "ColA", "ColB", "ColC");
        schemata = schemataFor(columns, PropertyType.STRING, PropertyType.LONG, PropertyType.STRING);
        // Define the context ...
        context = new QueryContext(new ExecutionContext(), new PlanHints(), schemata);
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
        component = new SortValuesComponent(delegate, orderings);
    }

    @Test
    public void shouldReturnAllResultsOrderedByNodeName() {
        orderings.add(orderByNodeName("Selector1"));
        component = new SortValuesComponent(delegate, orderings);
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
        component = new SortValuesComponent(delegate, orderings);
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
    public void shouldReturnAllResultsOrderedByNodeLocalName() {
        orderings.add(orderByNodeName("Selector1"));
        component = new SortValuesComponent(delegate, orderings);
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
        component = new SortValuesComponent(delegate, orderings);
        inputTuples.add(tuple(columns, "/a/b1/dna:c1", "v1", 100, "v3"));
        inputTuples.add(tuple(columns, "/a/b2/dna:c4", "v1", 100, "v3"));
        inputTuples.add(tuple(columns, "/a/b3/dna:c2", "v1", 100, "v3"));
        inputTuples.add(tuple(columns, "/a/b4/dna:c3", "v1", 100, "v3"));
        inputTuples.add(tuple(columns, "/a/b5/jcr:c4", "v1", 100, "v3"));
        inputTuples.add(tuple(columns, "/a/b6/dna:c3", "v1", 100, "v3"));
        inputTuples.add(tuple(columns, "/a/b7/dna:c0", "v1", 100, "v3"));
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
    public void shouldReturnAllResultsOrderedByValueLengthOfLong() {
        orderings.add(orderByPropertyLength(columns.getColumns().get(1)));
        component = new SortValuesComponent(delegate, orderings);
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
        component = new SortValuesComponent(delegate, orderings);
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
        component = new SortValuesComponent(delegate, orderings);
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
