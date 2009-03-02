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
package org.jboss.dna.jcr;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.util.HashSet;
import java.util.Set;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import org.jboss.dna.jcr.AbstractJcrNodeTest.MockAbstractJcrNode;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * @author jverhaeg
 */
public class JcrPropertyIteratorTest {

    private AbstractJcrNode node;
    @Mock
    private JcrSession session;
    private Set<Property> properties;

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);
        properties = new HashSet<Property>();
        node = new MockAbstractJcrNode(session, "node", null);
        node.setProperties(properties);
    }

    @Test
    public void shouldProvidePropertyIterator() throws Exception {
        properties.add(Mockito.mock(Property.class));
        properties.add(Mockito.mock(Property.class));
        properties.add(Mockito.mock(Property.class));
        properties.add(Mockito.mock(Property.class));
        PropertyIterator iter = node.getProperties();
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
        node.getProperties().remove();
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowPropertyIteratorNegativeSkip() throws Exception {
        node.getProperties().skip(-1);
    }
}
