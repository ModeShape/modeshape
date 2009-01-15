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
import java.io.InputStream;
import java.io.Reader;
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
public class JcrPropertyTest {

    private Property prop;
    @Mock
    private Node node;
    private ExecutionContext executionContext = new ExecutionContext();
    @Mock
    Name name;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        prop = new JcrProperty(node, executionContext, name, true);
    }

    @Test( expected = AssertionError.class )
    public void shouldNotAllowNoValue() {
        new JcrProperty(node, executionContext, name, null);
    }

    @Test
    public void shouldProvideBoolean() throws Exception {
        assertThat(prop.getBoolean(), is(true));
        assertThat(prop.getType(), is(PropertyType.BOOLEAN));
    }

    @Test
    public void shouldProvideDate() throws Exception {
        Calendar cal = Calendar.getInstance();
        Property prop = new JcrProperty(node, executionContext, name, cal);
        assertThat(prop.getDate(), is(cal));
        assertThat(prop.getType(), is(PropertyType.DATE));
        prop = new JcrProperty(node, executionContext, name, cal.getTime());
        assertThat(prop.getDate(), is(cal));
        assertThat(prop.getType(), is(PropertyType.DATE));
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
    public void shouldIndicateHasSingleValue() throws Exception {
        PropertyDefinition def = prop.getDefinition();
        assertThat(def, notNullValue());
        assertThat(def.isMultiple(), is(false));
    }

    @Test
    public void shouldProvideDouble() throws Exception {
        Property prop = new JcrProperty(node, executionContext, name, 1.0);
        assertThat(prop.getDouble(), is(1.0));
        assertThat(prop.getType(), is(PropertyType.DOUBLE));
        prop = new JcrProperty(node, executionContext, name, 1.0F);
        assertThat(prop.getDouble(), is(1.0));
        assertThat(prop.getType(), is(PropertyType.DOUBLE));
    }

    @Test
    public void shouldProvideLong() throws Exception {
        Property prop = new JcrProperty(node, executionContext, name, 1);
        assertThat(prop.getLong(), is(1L));
        assertThat(prop.getType(), is(PropertyType.LONG));
        prop = new JcrProperty(node, executionContext, name, 1L);
        assertThat(prop.getLong(), is(1L));
        assertThat(prop.getType(), is(PropertyType.LONG));
    }

    @Test
    public void shouldProvideStream() throws Exception {
        Property prop = new JcrProperty(node, executionContext, name, new Object());
        InputStream stream = prop.getStream();
        try {
            assertThat(stream, notNullValue());
            assertThat(prop.getType(), is(PropertyType.BINARY));
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
    }

    @Test
    public void shouldProvideString() throws Exception {
        Property prop = new JcrProperty(node, executionContext, name, "value");
        assertThat(prop.getString(), is("value"));
        assertThat(prop.getType(), is(PropertyType.STRING));
    }

    @Test
    public void shouldAllowReferenceValue() throws Exception {
        UUID uuid = UUID.randomUUID();
        Property prop = new JcrProperty(node, executionContext, name, uuid);
        assertThat(prop.getString(), is(uuid.toString()));
        assertThat(prop.getType(), is(PropertyType.REFERENCE));
    }

    @Test
    public void shouldAllowBinaryValue() throws Exception {
        Property prop = new JcrProperty(node, executionContext, name, Mockito.mock(Reader.class));
        assertThat(prop.getType(), is(PropertyType.BINARY));
        prop = new JcrProperty(node, executionContext, name, Mockito.mock(InputStream.class));
        assertThat(prop.getType(), is(PropertyType.BINARY));
        prop = new JcrProperty(node, executionContext, name, new Object());
        assertThat(prop.getType(), is(PropertyType.BINARY));
    }

    @Test
    public void shouldAllowNameValue() throws Exception {
        Property prop = new JcrProperty(node, executionContext, name, name);
        assertThat(prop.getType(), is(PropertyType.NAME));
    }

    @Test
    public void shouldAllowPathValue() throws Exception {
        Property prop = new JcrProperty(node, executionContext, name, Mockito.mock(Path.class));
        assertThat(prop.getType(), is(PropertyType.PATH));
    }

    @Test
    public void shouldProvideValue() throws Exception {
        Value val = prop.getValue();
        assertThat(val, notNullValue());
        assertThat(val.getBoolean(), is(true));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotProvideValues() throws Exception {
        prop.getValues();
    }

    @Test
    public void shouldProvideLength() throws Exception {
        assertThat(prop.getLength(), is(4L));
        assertThat(new JcrProperty(node, executionContext, name, "value").getLength(), is(5L));
        Object obj = new Object();
        assertThat(new JcrProperty(node, executionContext, name, obj).getLength(), is((long)obj.toString().length()));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotProvideLengths() throws Exception {
        prop.getLengths();
    }
}
