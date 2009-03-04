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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;
import java.io.InputStream;
import java.util.UUID;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.PropertyDefinition;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.property.DateTime;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * @author jverhaeg
 */
public class JcrPropertyTest {

    private Property prop;
    @Mock
    private Node node;
    private ExecutionContext executionContext;
    @Mock
    Name name;
    @Mock
    private PropertyDefinition definition;
    private org.jboss.dna.graph.property.Property dnaProperty;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        executionContext = new ExecutionContext();
        dnaProperty = executionContext.getPropertyFactory().create(JcrLexicon.MIMETYPE, "text/plain");
        stub(definition.getRequiredType()).toReturn(PropertyType.STRING);
        stub(definition.isMultiple()).toReturn(false);
        prop = new JcrProperty(node, executionContext, definition, dnaProperty);
    }

    @Test
    public void shouldProvideBoolean() throws Exception {
        dnaProperty = executionContext.getPropertyFactory().create(JcrLexicon.MIMETYPE, "true");
        stub(definition.getRequiredType()).toReturn(PropertyType.BOOLEAN);
        prop = new JcrProperty(node, executionContext, definition, dnaProperty);
        assertThat(prop.getBoolean(), is(true));
        assertThat(prop.getType(), is(PropertyType.BOOLEAN));
    }

    @Test
    public void shouldIndicateHasSingleValue() throws Exception {
        PropertyDefinition def = prop.getDefinition();
        assertThat(def, notNullValue());
        assertThat(def.isMultiple(), is(false));
    }

    @Test
    public void shouldProvideDate() throws Exception {
        DateTime dnaDate = executionContext.getValueFactories().getDateFactory().create();
        dnaProperty = executionContext.getPropertyFactory().create(JcrLexicon.MIMETYPE, dnaDate);
        stub(definition.getRequiredType()).toReturn(PropertyType.DATE);
        prop = new JcrProperty(node, executionContext, definition, dnaProperty);
        assertThat(prop.getDate(), is(dnaDate.toCalendar()));
        assertThat(prop.getLong(), is(dnaDate.getMilliseconds()));
        assertThat(prop.getType(), is(PropertyType.DATE));
        assertThat(prop.getName(), is(dnaProperty.getName().getString(executionContext.getNamespaceRegistry())));
    }

    @Test
    public void shouldProvideNode() throws Exception {
        UUID referencedUuid = UUID.randomUUID();
        dnaProperty = executionContext.getPropertyFactory().create(JcrLexicon.MIMETYPE, referencedUuid);
        stub(definition.getRequiredType()).toReturn(PropertyType.REFERENCE);
        Node referencedNode = mock(Node.class);
        JcrSession session = mock(JcrSession.class);
        stub(node.getSession()).toReturn(session);
        stub(session.getNode(referencedUuid)).toReturn(referencedNode);
        prop = new JcrProperty(node, executionContext, definition, dnaProperty);
        assertThat(prop.getNode(), is(referencedNode));
        assertThat(prop.getType(), is(PropertyType.REFERENCE));
        assertThat(prop.getName(), is(dnaProperty.getName().getString(executionContext.getNamespaceRegistry())));
    }

    @Test
    public void shouldProvideDouble() throws Exception {
        dnaProperty = executionContext.getPropertyFactory().create(JcrLexicon.MIMETYPE, 1.0);
        stub(definition.getRequiredType()).toReturn(PropertyType.DOUBLE);
        prop = new JcrProperty(node, executionContext, definition, dnaProperty);
        assertThat(prop.getDouble(), is(1.0));
        assertThat(prop.getType(), is(PropertyType.DOUBLE));
        assertThat(prop.getName(), is(dnaProperty.getName().getString(executionContext.getNamespaceRegistry())));
        dnaProperty = executionContext.getPropertyFactory().create(JcrLexicon.MIMETYPE, 1.0F);
        prop = new JcrProperty(node, executionContext, definition, dnaProperty);
        assertThat(prop.getDouble(), is(1.0));
        assertThat(prop.getType(), is(PropertyType.DOUBLE));
        assertThat(prop.getName(), is(dnaProperty.getName().getString(executionContext.getNamespaceRegistry())));
    }

    @Test
    public void shouldProvideLong() throws Exception {
        dnaProperty = executionContext.getPropertyFactory().create(JcrLexicon.MIMETYPE, 1);
        stub(definition.getRequiredType()).toReturn(PropertyType.DOUBLE);
        prop = new JcrProperty(node, executionContext, definition, dnaProperty);
        assertThat(prop.getLong(), is(1L));
        assertThat(prop.getType(), is(PropertyType.DOUBLE));
        assertThat(prop.getName(), is(dnaProperty.getName().getString(executionContext.getNamespaceRegistry())));

        dnaProperty = executionContext.getPropertyFactory().create(JcrLexicon.MIMETYPE, 1L);
        stub(definition.getRequiredType()).toReturn(PropertyType.DOUBLE);
        prop = new JcrProperty(node, executionContext, definition, dnaProperty);
        assertThat(prop.getLong(), is(1L));
        assertThat(prop.getString(), is("1"));
        assertThat(prop.getType(), is(PropertyType.DOUBLE));
        assertThat(prop.getName(), is(dnaProperty.getName().getString(executionContext.getNamespaceRegistry())));
    }

    @Test
    public void shouldProvideStream() throws Exception {
        dnaProperty = executionContext.getPropertyFactory().create(JcrLexicon.MIMETYPE, new Object());
        stub(definition.getRequiredType()).toReturn(PropertyType.BINARY);
        prop = new JcrProperty(node, executionContext, definition, dnaProperty);
        assertThat(prop.getType(), is(PropertyType.BINARY));
        assertThat(prop.getName(), is(dnaProperty.getName().getString(executionContext.getNamespaceRegistry())));
        InputStream stream = prop.getStream();
        try {
            assertThat(stream, notNullValue());
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
    }

    @Test
    public void shouldProvideString() throws Exception {
        dnaProperty = executionContext.getPropertyFactory().create(JcrLexicon.MIMETYPE, "value");
        stub(definition.getRequiredType()).toReturn(PropertyType.STRING);
        prop = new JcrProperty(node, executionContext, definition, dnaProperty);
        assertThat(prop.getString(), is("value"));
        assertThat(prop.getType(), is(PropertyType.STRING));
        assertThat(prop.getName(), is(dnaProperty.getName().getString(executionContext.getNamespaceRegistry())));
    }

    @Test
    public void shouldAllowReferenceValue() throws Exception {
        UUID uuid = UUID.randomUUID();
        dnaProperty = executionContext.getPropertyFactory().create(JcrLexicon.MIMETYPE, uuid);
        stub(definition.getRequiredType()).toReturn(PropertyType.STRING);
        prop = new JcrProperty(node, executionContext, definition, dnaProperty);
        assertThat(prop.getString(), is(uuid.toString()));
        assertThat(prop.getType(), is(PropertyType.STRING));
        assertThat(prop.getName(), is(dnaProperty.getName().getString(executionContext.getNamespaceRegistry())));
    }

    @Test
    public void shouldAllowNameValue() throws Exception {
        executionContext.getNamespaceRegistry().register("acme", "http://example.com");
        Name path = executionContext.getValueFactories().getNameFactory().create("acme:something");
        dnaProperty = executionContext.getPropertyFactory().create(JcrLexicon.MIMETYPE, path);
        stub(definition.getRequiredType()).toReturn(PropertyType.NAME);
        prop = new JcrProperty(node, executionContext, definition, dnaProperty);
        assertThat(prop.getString(), is("acme:something"));
        assertThat(prop.getType(), is(PropertyType.NAME));
        assertThat(prop.getName(), is(dnaProperty.getName().getString(executionContext.getNamespaceRegistry())));
        // Change the namespace registry ...
        executionContext.getNamespaceRegistry().register("acme2", "http://example.com");
        assertThat(prop.getString(), is("acme2:something"));
    }

    @Test
    public void shouldAllowPathValue() throws Exception {
        executionContext.getNamespaceRegistry().register("acme", "http://example.com");
        Path path = executionContext.getValueFactories().getPathFactory().create("/a/b/acme:c");
        dnaProperty = executionContext.getPropertyFactory().create(JcrLexicon.MIMETYPE, (Object)path);
        stub(definition.getRequiredType()).toReturn(PropertyType.PATH);
        prop = new JcrProperty(node, executionContext, definition, dnaProperty);
        assertThat(prop.getString(), is("/a/b/acme:c"));
        assertThat(prop.getType(), is(PropertyType.PATH));
        assertThat(prop.getName(), is(dnaProperty.getName().getString(executionContext.getNamespaceRegistry())));
        // Change the namespace registry ...
        executionContext.getNamespaceRegistry().register("acme2", "http://example.com");
        assertThat(prop.getString(), is("/a/b/acme2:c"));
    }

    @Test
    public void shouldProvideValue() throws Exception {
        Value val = prop.getValue();
        assertThat(val, notNullValue());
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotProvideValues() throws Exception {
        prop.getValues();
    }

    @Test
    public void shouldProvideLength() throws Exception {
        String dnaValue = executionContext.getValueFactories().getStringFactory().create(dnaProperty.getFirstValue());
        assertThat(prop.getLength(), is((long)dnaValue.length()));

        dnaValue = "some other value";
        dnaProperty = executionContext.getPropertyFactory().create(JcrLexicon.MIMETYPE, dnaValue);
        stub(definition.getRequiredType()).toReturn(PropertyType.STRING);
        prop = new JcrProperty(node, executionContext, definition, dnaProperty);
        assertThat(prop.getLength(), is((long)dnaValue.length()));

        Object obj = new Object();
        long binaryLength = executionContext.getValueFactories().getBinaryFactory().create(obj).getSize();
        dnaProperty = executionContext.getPropertyFactory().create(JcrLexicon.MIMETYPE, obj);
        stub(definition.getRequiredType()).toReturn(PropertyType.BINARY);
        prop = new JcrProperty(node, executionContext, definition, dnaProperty);
        assertThat(prop.getLength(), is(binaryLength));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotProvideLengths() throws Exception {
        prop.getLengths();
    }

    @Test
    public void shouldProvidePropertyDefinition() throws Exception {
        assertThat(prop.getDefinition(), notNullValue());
    }

}
