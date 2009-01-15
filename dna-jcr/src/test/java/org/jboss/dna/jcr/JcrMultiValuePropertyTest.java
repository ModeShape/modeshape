/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
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
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.InputStream;
import java.io.Reader;
import java.util.Arrays;
import java.util.Calendar;
import java.util.UUID;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.PropertyDefinition;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * @author jverhaeg
 */
public class JcrMultiValuePropertyTest {

    private Property prop;
    @Mock
    private Node node;
    private ExecutionContext executionContext = new ExecutionContext();
    @Mock
    Name name;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        prop = new JcrMultiValueProperty(node, executionContext, name, Arrays.asList(true));
    }

    @Test( expected = AssertionError.class )
    public void shouldNotAllowNoValue() {
        new JcrMultiValueProperty(node, executionContext, name, null);
    }

    @Test
    public void shouldProvideAppropriateType() throws Exception {
        assertThat(prop.getType(), is(PropertyType.BOOLEAN));
        Calendar cal = Calendar.getInstance();
        Property prop = new JcrMultiValueProperty(node, executionContext, name, Arrays.asList(cal));
        assertThat(prop.getType(), is(PropertyType.DATE));
        prop = new JcrMultiValueProperty(node, executionContext, name, Arrays.asList(cal.getTime()));
        assertThat(prop.getType(), is(PropertyType.DATE));
        prop = new JcrMultiValueProperty(node, executionContext, name, Arrays.asList(1.0));
        assertThat(prop.getType(), is(PropertyType.DOUBLE));
        prop = new JcrMultiValueProperty(node, executionContext, name, Arrays.asList(1.0F));
        assertThat(prop.getType(), is(PropertyType.DOUBLE));
        prop = new JcrMultiValueProperty(node, executionContext, name, Arrays.asList(1));
        assertThat(prop.getType(), is(PropertyType.LONG));
        prop = new JcrMultiValueProperty(node, executionContext, name, Arrays.asList(1L));
        assertThat(prop.getType(), is(PropertyType.LONG));
        prop = new JcrMultiValueProperty(node, executionContext, name, Arrays.asList(new Object()));
        assertThat(prop.getType(), is(PropertyType.BINARY));
        prop = new JcrMultiValueProperty(node, executionContext, name, Arrays.asList("value"));
        assertThat(prop.getType(), is(PropertyType.STRING));
        prop = new JcrMultiValueProperty(node, executionContext, name, Arrays.asList(UUID.randomUUID()));
        assertThat(prop.getType(), is(PropertyType.REFERENCE));
        prop = new JcrMultiValueProperty(node, executionContext, name, Arrays.asList(Mockito.mock(Reader.class)));
        assertThat(prop.getType(), is(PropertyType.BINARY));
        prop = new JcrMultiValueProperty(node, executionContext, name, Arrays.asList(Mockito.mock(InputStream.class)));
        assertThat(prop.getType(), is(PropertyType.BINARY));
        prop = new JcrMultiValueProperty(node, executionContext, name, Arrays.asList(new Object()));
        assertThat(prop.getType(), is(PropertyType.BINARY));
        prop = new JcrMultiValueProperty(node, executionContext, name, Arrays.asList(name));
        assertThat(prop.getType(), is(PropertyType.NAME));
        prop = new JcrMultiValueProperty(node, executionContext, name, Arrays.asList(Mockito.mock(Path.class)));
        assertThat(prop.getType(), is(PropertyType.PATH));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotProvideBoolean() throws Exception {
        prop.getBoolean();
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotProvideDateForCalendar() throws Exception {
        new JcrMultiValueProperty(node, executionContext, name, Arrays.asList(Calendar.getInstance())).getDate();
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotProvideDateForDate() throws Exception {
        new JcrMultiValueProperty(node, executionContext, name, Arrays.asList(Calendar.getInstance().getTime())).getDate();
    }

    @Test
    public void shouldProvidePropertyDefinition() throws Exception {
        assertThat(prop.getDefinition(), notNullValue());
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowPropertyDefinitionDeclaringNodeType() throws Exception {
        prop.getDefinition().getDeclaringNodeType();
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowPropertyDefinitionDefaultValues() throws Exception {
        prop.getDefinition().getDefaultValues();
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowPropertyDefinitionName() throws Exception {
        prop.getDefinition().getName();
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowPropertyDefinitionGetOnParentVersion() throws Exception {
        prop.getDefinition().getOnParentVersion();
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowPropertyDefinitionGetRequiredType() throws Exception {
        prop.getDefinition().getRequiredType();
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowPropertyDefinitionGetValueConstraints() throws Exception {
        prop.getDefinition().getValueConstraints();
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowPropertyDefinitionIsAutoCreated() throws Exception {
        prop.getDefinition().isAutoCreated();
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowPropertyDefinitionIsMandatory() throws Exception {
        prop.getDefinition().isMandatory();
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowPropertyDefinitionIsProtected() throws Exception {
        prop.getDefinition().isProtected();
    }

    @Test
    public void shouldIndicateHasMultipleValues() throws Exception {
        PropertyDefinition def = prop.getDefinition();
        assertThat(def, notNullValue());
        assertThat(def.isMultiple(), is(true));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotProvideDoubleForDouble() throws Exception {
        new JcrMultiValueProperty(node, executionContext, name, Arrays.asList(1.0)).getDouble();
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotProvideDoubleForFloat() throws Exception {
        new JcrMultiValueProperty(node, executionContext, name, Arrays.asList(1.0F)).getDouble();
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotProvideLongForInteger() throws Exception {
        new JcrMultiValueProperty(node, executionContext, name, Arrays.asList(1)).getLong();
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotProvideLongForLong() throws Exception {
        new JcrMultiValueProperty(node, executionContext, name, Arrays.asList(1L)).getLong();
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotProvideStream() throws Exception {
        new JcrMultiValueProperty(node, executionContext, name, Arrays.asList(new Object())).getStream();
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotProvideString() throws Exception {
        new JcrMultiValueProperty(node, executionContext, name, Arrays.asList("value")).getString();
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotProvideValue() throws Exception {
        prop.getValue();
    }

    @Test
    public void shouldProvideValues() throws Exception {
        Value[] vals = prop.getValues();
        assertThat(vals, notNullValue());
        assertThat(vals.length, is(1));
        assertThat(vals[0].getBoolean(), is(true));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotProvideLength() throws Exception {
        prop.getLength();
    }

    @Test
    public void shouldProvideLengths() throws Exception {
        long[] lengths = prop.getLengths();
        assertThat(lengths, notNullValue());
        assertThat(lengths.length, is(1));
        assertThat(lengths[0], is(4L));
        lengths = new JcrMultiValueProperty(node, executionContext, name, Arrays.asList("value")).getLengths();
        assertThat(lengths, notNullValue());
        assertThat(lengths.length, is(1));
        assertThat(lengths[0], is(5L));
        Object obj = new Object();
        lengths = new JcrMultiValueProperty(node, executionContext, name, Arrays.asList(obj)).getLengths();
        assertThat(lengths, notNullValue());
        assertThat(lengths.length, is(1));
        assertThat(lengths[0], is((long)obj.toString().length()));
    }
}
