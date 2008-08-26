/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
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
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author jverhaeg
 */
public class JcrPropertyIteratorTest {

    @Test( expected = AssertionError.class )
    public void shouldNotAllowNoProperties() throws Exception {
        new JcrPropertyIterator(null);
    }

    @Test
    public void shouldIndicateWhenPropertiesRemain() throws Exception {
        Set<Property> props = new HashSet<Property>();
        PropertyIterator iter = new JcrPropertyIterator(props);
        assertThat(iter.hasNext(), is(false));
        props.add(Mockito.mock(Property.class));
        iter = new JcrPropertyIterator(props);
        assertThat(iter.hasNext(), is(true));
    }

    @Test
    public void shouldAllowNextWhenPropertiesRemain() throws Exception {
        Set<Property> props = new HashSet<Property>();
        Property prop1 = Mockito.mock(Property.class);
        props.add(prop1);
        Property prop2 = Mockito.mock(Property.class);
        props.add(prop2);
        PropertyIterator iter = new JcrPropertyIterator(props);
        Property iterProp1 = (Property)iter.next();
        assertThat(iterProp1, notNullValue());
        Property iterProp2 = (Property)iter.next();
        assertThat(iterProp2, notNullValue());
        assertThat(iterProp1, is(not(iterProp2)));
    }

    @Test( expected = NoSuchElementException.class )
    public void shouldNotAllowNextWhenNoPropertiesRemain() throws Exception {
        Set<Property> props = new HashSet<Property>();
        PropertyIterator iter = new JcrPropertyIterator(props);
        iter.next();
    }

    @Test
    public void shouldAllowNextPropertyWhenPropertiesRemain() throws Exception {
        Set<Property> props = new HashSet<Property>();
        Property prop1 = Mockito.mock(Property.class);
        props.add(prop1);
        Property prop2 = Mockito.mock(Property.class);
        props.add(prop2);
        PropertyIterator iter = new JcrPropertyIterator(props);
        Property iterProp1 = iter.nextProperty();
        assertThat(iterProp1, notNullValue());
        Property iterProp2 = iter.nextProperty();
        assertThat(iterProp2, notNullValue());
        assertThat(iterProp1, is(not(iterProp2)));
    }

    @Test( expected = NoSuchElementException.class )
    public void shouldNotAllowNextPropertyWhenNoPropertiesRemain() throws Exception {
        Set<Property> props = new HashSet<Property>();
        PropertyIterator iter = new JcrPropertyIterator(props);
        iter.nextProperty();
    }

    @Test
    public void shouldAllowRemoveAfterNext() throws Exception {
        Set<Property> props = new HashSet<Property>();
        props.add(Mockito.mock(Property.class));
        PropertyIterator iter = new JcrPropertyIterator(props);
        iter.next();
        iter.remove();
        assertThat(props.isEmpty(), is(true));
    }

    @Test( expected = IllegalStateException.class )
    public void shouldNotAllowRemoveBeforeNext() throws Exception {
        Set<Property> props = new HashSet<Property>();
        props.add(Mockito.mock(Property.class));
        PropertyIterator iter = new JcrPropertyIterator(props);
        iter.remove();
    }

    @Test( expected = IllegalStateException.class )
    public void shouldNotAllowRemoveTwice() throws Exception {
        Set<Property> props = new HashSet<Property>();
        props.add(Mockito.mock(Property.class));
        props.add(Mockito.mock(Property.class));
        PropertyIterator iter = new JcrPropertyIterator(props);
        iter.next();
        iter.remove();
        iter.remove();
    }

    @Test
    public void shouldProvideSize() throws Exception {
        Set<Property> props = new HashSet<Property>();
        PropertyIterator iter = new JcrPropertyIterator(props);
        assertThat(iter.getSize(), is(0L));
        props.add(Mockito.mock(Property.class));
        iter = new JcrPropertyIterator(props);
        assertThat(iter.getSize(), is(1L));
        iter.next();
        iter.remove();
        assertThat(iter.getSize(), is(0L));
    }

    @Test
    public void shouldProvidePosition() throws Exception {
        Set<Property> props = new HashSet<Property>();
        PropertyIterator iter = new JcrPropertyIterator(props);
        assertThat(iter.getPosition(), is(0L));
        props.add(Mockito.mock(Property.class));
        iter = new JcrPropertyIterator(props);
        assertThat(iter.getPosition(), is(0L));
        iter.next();
        assertThat(iter.getPosition(), is(1L));
        iter.remove();
        assertThat(iter.getPosition(), is(1L));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNegativeSkip() throws Exception {
        Set<Property> props = new HashSet<Property>();
        PropertyIterator iter = new JcrPropertyIterator(props);
        iter.skip(-1);
    }

    @Test
    public void shouldAllowSkip() throws Exception {
        Set<Property> props = new HashSet<Property>();
        props.add(Mockito.mock(Property.class));
        PropertyIterator iter = new JcrPropertyIterator(props);
        iter.skip(0);
        assertThat(iter.hasNext(), is(true));
        iter.skip(1);
        assertThat(iter.hasNext(), is(false));
    }

    @Test( expected = NoSuchElementException.class )
    public void shouldNotAllowSkipPastRemainingProperties() throws Exception {
        Set<Property> props = new HashSet<Property>();
        PropertyIterator iter = new JcrPropertyIterator(props);
        iter.skip(1);
    }
}
