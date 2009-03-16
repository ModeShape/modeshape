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
import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.PropertyDefinition;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.property.DateTime;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.jcr.SessionCache.PropertyInfo;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * @author jverhaeg
 */
public class JcrSingleValuePropertyTest {

    private PropertyId propertyId;
    private JcrSingleValueProperty prop;
    private ExecutionContext executionContext;
    private org.jboss.dna.graph.property.Property dnaProperty;
    @Mock
    private SessionCache cache;
    @Mock
    private JcrSession session;
    @Mock
    private PropertyInfo propertyInfo;
    @Mock
    private JcrPropertyDefinition definition;
    @Mock
    private JcrNodeTypeManager nodeTypes;

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);
        executionContext = new ExecutionContext();
        stub(cache.session()).toReturn(session);
        stub(cache.context()).toReturn(executionContext);
        stub(session.nodeTypeManager()).toReturn(nodeTypes);
        stub(session.getExecutionContext()).toReturn(executionContext);

        dnaProperty = executionContext.getPropertyFactory().create(JcrLexicon.MIMETYPE, "text/plain");
        stub(definition.getRequiredType()).toReturn(PropertyType.STRING);
        stub(definition.isMultiple()).toReturn(false);
        PropertyDefinitionId definitionId = new PropertyDefinitionId(name("nodeTypeName"), name("propDefnName"));
        stub(nodeTypes.getPropertyDefinition(definitionId, false)).toReturn(definition);

        UUID uuid = UUID.randomUUID();
        propertyId = new PropertyId(uuid, JcrLexicon.MIMETYPE);
        prop = new JcrSingleValueProperty(cache, propertyId);

        stub(cache.findPropertyInfo(propertyId)).toReturn(propertyInfo);
        stub(propertyInfo.getDefinitionId()).toReturn(definitionId);
        stub(propertyInfo.getPropertyType()).toReturn(PropertyType.STRING);
        stub(propertyInfo.isMultiValued()).toReturn(false);
        stub(propertyInfo.getProperty()).toReturn(dnaProperty);
        stub(propertyInfo.getPropertyName()).toReturn(dnaProperty.getName());
    }

    protected Name name( String name ) {
        return executionContext.getValueFactories().getNameFactory().create(name);
    }

    @Test
    public void shouldProvideBoolean() throws Exception {
        stub(propertyInfo.getPropertyType()).toReturn(PropertyType.BOOLEAN);
        dnaProperty = executionContext.getPropertyFactory().create(JcrLexicon.MIMETYPE, "true");
        stub(propertyInfo.getProperty()).toReturn(dnaProperty);
        assertThat(prop.getBoolean(), is(true));
        assertThat(prop.getType(), is(PropertyType.BOOLEAN));
    }

    @Test
    public void shouldIndicateHasSingleValue() throws Exception {
        PropertyDefinition def = prop.getDefinition();
        assertThat(def.isMultiple(), is(false));
    }

    @Test
    public void shouldProvideDate() throws Exception {
        DateTime dnaDate = executionContext.getValueFactories().getDateFactory().create();
        dnaProperty = executionContext.getPropertyFactory().create(JcrLexicon.MIMETYPE, dnaDate);
        stub(propertyInfo.getProperty()).toReturn(dnaProperty);
        stub(propertyInfo.getPropertyType()).toReturn(PropertyType.DATE);
        assertThat(prop.getDate(), is(dnaDate.toCalendar()));
        assertThat(prop.getLong(), is(dnaDate.getMilliseconds()));
        assertThat(prop.getType(), is(PropertyType.DATE));
        assertThat(prop.getName(), is(dnaProperty.getName().getString(executionContext.getNamespaceRegistry())));
    }

    @Test
    public void shouldProvideNode() throws Exception {
        UUID referencedUuid = UUID.randomUUID();
        dnaProperty = executionContext.getPropertyFactory().create(JcrLexicon.MIMETYPE, referencedUuid);
        stub(propertyInfo.getProperty()).toReturn(dnaProperty);
        stub(propertyInfo.getPropertyType()).toReturn(PropertyType.REFERENCE);
        AbstractJcrNode referencedNode = mock(AbstractJcrNode.class);
        stub(cache.findJcrNode(referencedUuid)).toReturn(referencedNode);
        assertThat(prop.getNode(), is((Node)referencedNode));
        assertThat(prop.getType(), is(PropertyType.REFERENCE));
        assertThat(prop.getName(), is(dnaProperty.getName().getString(executionContext.getNamespaceRegistry())));
    }

    @Test
    public void shouldProvideDouble() throws Exception {
        dnaProperty = executionContext.getPropertyFactory().create(JcrLexicon.MIMETYPE, 1.0);
        stub(propertyInfo.getProperty()).toReturn(dnaProperty);
        stub(propertyInfo.getPropertyType()).toReturn(PropertyType.DOUBLE);
        assertThat(prop.getDouble(), is(1.0));
        assertThat(prop.getType(), is(PropertyType.DOUBLE));
        assertThat(prop.getName(), is(dnaProperty.getName().getString(executionContext.getNamespaceRegistry())));
        dnaProperty = executionContext.getPropertyFactory().create(JcrLexicon.MIMETYPE, 1.0F);
        stub(propertyInfo.getProperty()).toReturn(dnaProperty);
        assertThat(prop.getDouble(), is(1.0));
        assertThat(prop.getType(), is(PropertyType.DOUBLE));
        assertThat(prop.getName(), is(dnaProperty.getName().getString(executionContext.getNamespaceRegistry())));
    }

    @Test
    public void shouldProvideLong() throws Exception {
        dnaProperty = executionContext.getPropertyFactory().create(JcrLexicon.MIMETYPE, 1);
        stub(propertyInfo.getProperty()).toReturn(dnaProperty);
        stub(propertyInfo.getPropertyType()).toReturn(PropertyType.LONG);
        assertThat(prop.getLong(), is(1L));
        assertThat(prop.getType(), is(PropertyType.LONG));
        assertThat(prop.getName(), is(dnaProperty.getName().getString(executionContext.getNamespaceRegistry())));

        dnaProperty = executionContext.getPropertyFactory().create(JcrLexicon.MIMETYPE, 1L);
        stub(propertyInfo.getProperty()).toReturn(dnaProperty);
        stub(propertyInfo.getPropertyType()).toReturn(PropertyType.LONG);
        assertThat(prop.getLong(), is(1L));
        assertThat(prop.getString(), is("1"));
        assertThat(prop.getType(), is(PropertyType.LONG));
        assertThat(prop.getName(), is(dnaProperty.getName().getString(executionContext.getNamespaceRegistry())));
    }

    @Test
    public void shouldProvideStream() throws Exception {
        dnaProperty = executionContext.getPropertyFactory().create(JcrLexicon.MIMETYPE, new Object());
        stub(propertyInfo.getProperty()).toReturn(dnaProperty);
        stub(propertyInfo.getPropertyType()).toReturn(PropertyType.BINARY);
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
        stub(propertyInfo.getProperty()).toReturn(dnaProperty);
        stub(propertyInfo.getPropertyType()).toReturn(PropertyType.STRING);
        assertThat(prop.getString(), is("value"));
        assertThat(prop.getType(), is(PropertyType.STRING));
        assertThat(prop.getName(), is(dnaProperty.getName().getString(executionContext.getNamespaceRegistry())));
    }

    @Test
    public void shouldAllowReferenceValue() throws Exception {
        UUID uuid = UUID.randomUUID();
        dnaProperty = executionContext.getPropertyFactory().create(JcrLexicon.MIMETYPE, uuid);
        stub(propertyInfo.getProperty()).toReturn(dnaProperty);
        stub(propertyInfo.getPropertyType()).toReturn(PropertyType.STRING);
        assertThat(prop.getString(), is(uuid.toString()));
        assertThat(prop.getType(), is(PropertyType.STRING));
        assertThat(prop.getName(), is(dnaProperty.getName().getString(executionContext.getNamespaceRegistry())));
    }

    @Test
    public void shouldAllowNameValue() throws Exception {
        executionContext.getNamespaceRegistry().register("acme", "http://example.com");
        Name path = executionContext.getValueFactories().getNameFactory().create("acme:something");
        dnaProperty = executionContext.getPropertyFactory().create(JcrLexicon.MIMETYPE, path);
        stub(propertyInfo.getProperty()).toReturn(dnaProperty);
        stub(propertyInfo.getPropertyType()).toReturn(PropertyType.NAME);
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
        stub(propertyInfo.getProperty()).toReturn(dnaProperty);
        stub(propertyInfo.getPropertyType()).toReturn(PropertyType.PATH);
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
        stub(propertyInfo.getProperty()).toReturn(dnaProperty);
        assertThat(prop.getLength(), is((long)dnaValue.length()));

        Object obj = new Object();
        long binaryLength = executionContext.getValueFactories().getBinaryFactory().create(obj).getSize();
        dnaProperty = executionContext.getPropertyFactory().create(JcrLexicon.MIMETYPE, obj);
        stub(propertyInfo.getProperty()).toReturn(dnaProperty);
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
