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
package org.modeshape.graph.query.model;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

/**
 * 
 */
public class QueryTest extends AbstractQueryObjectTest {

    private Query query;
    private Source source;
    private Constraint constraint;
    private List<? extends Ordering> orderings;
    private List<? extends Column> columns;
    private Limit limits;
    private boolean distinct;

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotCreateWithNullSource() {
        new Query(null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotCreateWithNullSourceWhenSupplyingOtherParameters() {
        source = null;
        constraint = mock(Constraint.class);
        orderings = Collections.emptyList();
        columns = Collections.emptyList();
        limits = null;
        new Query(source, constraint, orderings, columns, limits, distinct);
    }

    @Test
    public void shouldAllowNullConstraint() {
        source = mock(Source.class);
        constraint = null;
        orderings = Collections.emptyList();
        columns = Collections.emptyList();
        limits = null;
        query = new Query(source, constraint, orderings, columns, limits, distinct);
        assertThat(query.source(), is(sameInstance(source)));
        assertThat(query.constraint(), is(nullValue()));
        assertThat(query.orderings() == orderings, is(true));
        assertThat(query.columns() == columns, is(true));
    }

    @Test
    public void shouldAllowNullOrderingsList() {
        source = mock(Source.class);
        constraint = mock(Constraint.class);
        orderings = null;
        columns = Collections.emptyList();
        limits = null;
        query = new Query(source, constraint, orderings, columns, limits, distinct);
        assertThat(query.source(), is(sameInstance(source)));
        assertThat(query.constraint(), is(sameInstance(constraint)));
        assertThat(query.orderings().isEmpty(), is(true));
        assertThat(query.columns() == columns, is(true));
    }

    @Test
    public void shouldAllowNullColumnsList() {
        source = mock(Source.class);
        constraint = mock(Constraint.class);
        orderings = Collections.emptyList();
        columns = null;
        limits = null;
        query = new Query(source, constraint, orderings, columns, limits, distinct);
        assertThat(query.source(), is(sameInstance(source)));
        assertThat(query.constraint(), is(sameInstance(constraint)));
        assertThat(query.orderings() == orderings, is(true));
        assertThat(query.columns().isEmpty(), is(true));
    }

    @Test
    public void shouldCreateWithNonNullParameters() {
        source = mock(Source.class);
        constraint = mock(Constraint.class);
        orderings = Collections.emptyList();
        columns = Collections.emptyList();
        limits = null;
        query = new Query(source, constraint, orderings, columns, limits, distinct);
        assertThat(query.source(), is(sameInstance(source)));
        assertThat(query.constraint(), is(sameInstance(constraint)));
        assertThat(query.orderings() == orderings, is(true));
        assertThat(query.columns() == columns, is(true));
    }

    @Test
    public void shouldConstructReadableString() {
        source = new NamedSelector(selector("nt:unstructured"));
        columns = Collections.singletonList(new Column(selector("selector1")));
        constraint = new PropertyExistence(selector("selector1"), "jcr:uuid");
        orderings = Collections.singletonList(new Ordering(new NodeName(selector("selector1")), Order.ASCENDING));
        query = new Query(source, constraint, orderings, columns, limits, distinct);
        assertThat(Visitors.readable(query),
                   is("SELECT selector1.* FROM nt:unstructured WHERE selector1.jcr:uuid IS NOT NULL ORDER BY NAME(selector1) ASC"));
    }

    @Test
    public void shouldConstructReadableStringWithLimits() {
        source = new NamedSelector(selector("nt:unstructured"));
        columns = Collections.singletonList(new Column(selector("selector1")));
        constraint = new PropertyExistence(selector("selector1"), "jcr:uuid");
        orderings = Collections.singletonList(new Ordering(new NodeName(selector("selector1")), Order.ASCENDING));
        limits = new Limit(10, 100);
        query = new Query(source, constraint, orderings, columns, limits, distinct);
        assertThat(Visitors.readable(query),
                   is("SELECT selector1.* FROM nt:unstructured WHERE selector1.jcr:uuid IS NOT NULL ORDER BY NAME(selector1) ASC LIMIT 10 OFFSET 100"));
    }

    @Test
    public void shouldConstructReadableStringWithNoColumns() {
        source = new NamedSelector(selector("nt:unstructured"));
        columns = Collections.emptyList();
        constraint = new PropertyExistence(selector("selector1"), "jcr:uuid");
        orderings = Collections.singletonList(new Ordering(new NodeName(selector("selector1")), Order.ASCENDING));
        query = new Query(source, constraint, orderings, columns, limits, distinct);
        assertThat(Visitors.readable(query),
                   is("SELECT * FROM nt:unstructured WHERE selector1.jcr:uuid IS NOT NULL ORDER BY NAME(selector1) ASC"));
    }

    @Test
    public void shouldConstructReadableStringWithNoOrderings() {
        source = new NamedSelector(selector("nt:unstructured"));
        columns = Collections.singletonList(new Column(selector("selector1")));
        constraint = new PropertyExistence(selector("selector1"), "jcr:uuid");
        orderings = Collections.emptyList();
        query = new Query(source, constraint, orderings, columns, limits, distinct);
        assertThat(Visitors.readable(query), is("SELECT selector1.* FROM nt:unstructured WHERE selector1.jcr:uuid IS NOT NULL"));
    }

    @Test
    public void shouldConstructReadableStringWithNoConstraint() {
        source = new NamedSelector(selector("nt:unstructured"));
        columns = Collections.singletonList(new Column(selector("selector1")));
        constraint = null;
        orderings = Collections.singletonList(new Ordering(new NodeName(selector("selector1")), Order.ASCENDING));
        query = new Query(source, constraint, orderings, columns, limits, distinct);
        assertThat(Visitors.readable(query), is("SELECT selector1.* FROM nt:unstructured ORDER BY NAME(selector1) ASC"));
    }

    @Test
    public void shouldConstructReadableStringWithDistinctAndNoConstraintOrColumnsOrOrderings() {
        source = new NamedSelector(selector("nt:unstructured"));
        columns = Collections.emptyList();
        constraint = null;
        orderings = Collections.emptyList();
        distinct = true;
        query = new Query(source, constraint, orderings, columns, limits, distinct);
        assertThat(Visitors.readable(query), is("SELECT DISTINCT * FROM nt:unstructured"));

        source = new AllNodes();
        query = new Query(source, constraint, orderings, columns, limits, distinct);
        assertThat(Visitors.readable(query), is("SELECT DISTINCT * FROM __ALLNODES__"));
    }

    @Test
    public void shouldConstructReadableStringWithNoConstraintOrColumnsOrOrderings() {
        source = new NamedSelector(selector("nt:unstructured"));
        columns = Collections.emptyList();
        constraint = null;
        orderings = Collections.emptyList();
        query = new Query(source, constraint, orderings, columns, limits, distinct);
        assertThat(Visitors.readable(query), is("SELECT * FROM nt:unstructured"));

        source = new AllNodes();
        query = new Query(source, constraint, orderings, columns, limits, distinct);
        assertThat(Visitors.readable(query), is("SELECT * FROM __ALLNODES__"));
    }
}
