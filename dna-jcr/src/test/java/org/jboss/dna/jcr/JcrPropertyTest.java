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
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.InputStream;
import java.util.Calendar;
import java.util.UUID;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import org.jboss.dna.spi.ExecutionContext;
import org.jboss.dna.spi.connector.BasicExecutionContext;
import org.jboss.dna.spi.graph.Name;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * @author jverhaeg
 */
public class JcrPropertyTest {

    @Mock
    private Node node;
    private ExecutionContext executionContext = new BasicExecutionContext();
    @Mock
    Name name;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test( expected = AssertionError.class )
    public void shouldNotAllowNoValue() {
        new JcrProperty(node, executionContext, name, null);
    }

    @Test
    public void shouldProvideBoolean() throws Exception {
        Property prop = new JcrProperty(node, executionContext, name, true);
        assertThat(prop.getBoolean(), is(true));
    }

    @Test
    public void shouldProvideDate() throws Exception {
        Calendar cal = Calendar.getInstance();
        Property prop = new JcrProperty(node, executionContext, name, cal);
        assertThat(prop.getDate(), is(cal));
        prop = new JcrProperty(node, executionContext, name, cal.getTime());
        assertThat(prop.getDate(), is(cal));
    }

    @Test
    public void shouldProvideDouble() throws Exception {
        Property prop = new JcrProperty(node, executionContext, name, 1.0);
        assertThat(prop.getDouble(), is(1.0));
        prop = new JcrProperty(node, executionContext, name, 1.0F);
        assertThat(prop.getDouble(), is(1.0));
    }

    @Test
    public void shouldProvideLength() throws Exception {
        assertThat(new JcrProperty(node, executionContext, name, "value").getLength(), is(5L));
        Object obj = new Object();
        assertThat(new JcrProperty(node, executionContext, name, obj).getLength(), is((long)obj.toString().length()));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotProvideLengths() throws Exception {
        new JcrProperty(node, executionContext, name, "value").getLengths();
    }

    @Test
    public void shouldProvideLong() throws Exception {
        Property prop = new JcrProperty(node, executionContext, name, 1);
        assertThat(prop.getLong(), is(1L));
        prop = new JcrProperty(node, executionContext, name, 1L);
        assertThat(prop.getLong(), is(1L));
    }

    @Test
    public void shouldProvideStream() throws Exception {
        Property prop = new JcrProperty(node, executionContext, name, "value");
        InputStream stream = prop.getStream();
        assertThat(stream, notNullValue());
        stream.close();
    }

    @Test
    public void shouldProvideString() throws Exception {
        Property prop = new JcrProperty(node, executionContext, name, "value");
        assertThat(prop.getString(), is("value"));
    }

    @Test
    public void shouldProvideUuid() throws Exception {
        UUID uuid = UUID.randomUUID();
        Property prop = new JcrProperty(node, executionContext, name, uuid);
        assertThat(prop.getString(), is(uuid.toString()));
    }

    @Test
    public void shouldProvideType() throws Exception {
        Property prop = new JcrProperty(node, executionContext, name, UUID.randomUUID());
        assertThat(prop.getType(), is(PropertyType.STRING));
    }

    @Test
    public void shouldProvideValue() throws Exception {
        Property prop = new JcrProperty(node, executionContext, name, true);
        Value val = prop.getValue();
        assertThat(val, notNullValue());
        assertThat(val.getBoolean(), is(true));
    }
}
