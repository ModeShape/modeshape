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
package org.modeshape.jcr;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.util.HashMap;
import java.util.Map;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.property.Name;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

/**
 * @author jverhaeg
 */
public class JcrPropertyIteratorTest {

    private Map<Name, Property> properties;
    private ExecutionContext context;
    private PropertyIterator iter;

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);
        context = new ExecutionContext();
        properties = new HashMap<Name, Property>();
        properties.put(name("prop1"), Mockito.mock(Property.class));
        properties.put(name("prop2"), Mockito.mock(Property.class));
        properties.put(name("prop3"), Mockito.mock(Property.class));
        properties.put(name("prop4"), Mockito.mock(Property.class));
        iter = new JcrPropertyIterator(properties.values());
    }

    protected Name name( String name ) {
        return context.getValueFactories().getNameFactory().create(name);
    }

    @Test
    public void shouldProvidePropertyIterator() throws Exception {
        assertThat(iter, notNullValue());
        assertThat(iter.getSize(), is(4L));
        assertThat(iter.getPosition(), is(0L));
        assertThat(iter.hasNext(), is(true));
        assertThat(iter.next(), notNullValue());
        assertThat(iter.getPosition(), is(1L));
        assertThat(iter.hasNext(), is(true));
        iter.skip(2);
        assertThat(iter.getPosition(), is(3L));
        assertThat(iter.hasNext(), is(true));
        assertThat(iter.nextProperty(), notNullValue());
        assertThat(iter.getPosition(), is(4L));
        assertThat(iter.hasNext(), is(false));
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowPropertyIteratorRemove() throws Exception {
        new JcrPropertyIterator(properties.values()).remove();
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowPropertyIteratorNegativeSkip() throws Exception {
        new JcrPropertyIterator(properties.values()).skip(-1);
    }
}
