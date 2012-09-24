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

package org.modeshape.jcr.xml;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.text.NoOpEncoder;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.value.NamespaceRegistry;
import org.modeshape.jcr.value.Path;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
 * Test case for {@link NodeImportXmlHandler}
 *
 * @author Randall Hauch
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class NodeImportXmlHandlerTest {
    private static final String NT_NAMESPACE_URI = "http://www.jcp.org/jcr/nt/1.0";

    private NodeImportXmlHandler handler;
    private ExecutionContext context;
    private Map<String, NodeImportXmlHandler.ImportElement> parseResults;
    private NodeImportDestination parseDestination;

    @Before
    public void beforeEach() {
        context = new ExecutionContext();
        context.getNamespaceRegistry().register(JcrLexicon.Namespace.PREFIX, JcrLexicon.Namespace.URI);
        context.getNamespaceRegistry().register("nt", NT_NAMESPACE_URI);
        parseDestination = new NodeImportDestination() {
            @Override
            public ExecutionContext getExecutionContext() {
                return context;
            }

            @Override
            public void submit( TreeMap<String, NodeImportXmlHandler.ImportElement> parseResults ) {
                NodeImportXmlHandlerTest.this.parseResults = parseResults;
            }
        };
        handler = new NodeImportXmlHandler(parseDestination);
    }

    @Test( expected = SAXException.class )
    public void shouldNotParseXmlWithInvalidRootElement() throws Exception {
        parse("xmlImport/docWithoutJcrRoot.xml");
    }

    @Test
    public void shouldParseXmlDocumentWithoutNamespaces() throws Exception {
        parse("xmlImport/docWithoutNamespaces.xml");

        assertNode("Cars");
        assertNode("Cars/Hybrid");
        assertNode("Cars/Hybrid/car", "name=Toyota Prius", "maker=Toyota", "model=Prius");
        assertNode("Cars/Hybrid/car[2]", "name=Toyota Highlander", "maker=Toyota", "model=Highlander");
        assertNode("Cars/Hybrid/car[3]", "name=Nissan Altima", "maker=Nissan", "model=Altima");
        assertNode("Cars/Sports");
        assertNode("Cars/Sports/car", "name=Aston Martin DB9", "maker=Aston Martin", "model=DB9");
        assertNode("Cars/Sports/car[2]", "name=Infiniti G37", "maker=Infiniti", "model=G37");
    }

    @Test
    public void shouldParseXmlDocumentWithNamespaces() throws Exception {
        context.getNamespaceRegistry().register("c", "http://default.namespace.com");
        parse("xmlImport/docWithNamespaces.xml");
        // Check the generated content.
        // Note that the attribute name DOES match, so the nodes names come from "jcr:name" attribute
        // Also, the "jcr:name" attribute values use the default namespace, which is "c" in the registry
        assertNode("c:Cars");
        assertNode("c:Cars/c:Hybrid");
        assertNode("c:Cars/c:Hybrid/c:Toyota Prius", "c:maker=Toyota", "c:model=Prius");
        assertNode("c:Cars/c:Hybrid/c:Toyota Highlander", "c:maker=Toyota", "c:model=Highlander");
        assertNode("c:Cars/c:Hybrid/c:Nissan Altima", "c:maker=Nissan", "c:model=Altima");
        assertNode("c:Cars/c:Sports");
        assertNode("c:Cars/c:Sports/c:Aston Martin DB9", "c:maker=Aston Martin", "c:model=DB9");
        assertNode("c:Cars/c:Sports/c:Infiniti G37", "c:maker=Infiniti", "c:model=G37");
    }

    @Test
    public void shouldParseXmlDocumentWithNestedNamespaceDeclarations() throws Exception {
        context.getNamespaceRegistry().register("c", "http://default.namespace.com");
        context.getNamespaceRegistry().register("i", "http://attributes.com");
        parse("xmlImport/docWithNestedNamespaces.xml");
        // Check the generated content; note that the attribute name DOES match, so the nodes names come from "jcr:name" attribute
        assertNode("Cars");
        assertNode("Cars/c:Hybrid");
        assertNode("Cars/c:Hybrid/c:Toyota Prius", "c:maker=Toyota", "c:model=Prius");
        assertNode("Cars/c:Hybrid/c:Toyota Highlander", "c:maker=Toyota", "c:model=Highlander");
        assertNode("Cars/c:Hybrid/c:Nissan Altima", "c:maker=Nissan", "c:model=Altima");
        assertNode("Cars/Sports");
        assertNode("Cars/Sports/Aston Martin DB9", "i:maker=Aston Martin", "model=DB9");
        assertNode("Cars/Sports/Infiniti G37", "i:maker=Infiniti", "model=G37");
    }

    @Test
    public void shouldParseXmlDocumentWithNamespacesThatAreNotYetInRegistry() throws Exception {
        NamespaceRegistry reg = context.getNamespaceRegistry();
        reg.unregister(JcrLexicon.Namespace.URI);
        reg.unregister(NT_NAMESPACE_URI);
        // Verify the prefixes don't exist ...
        assertThat(reg.getPrefixForNamespaceUri(JcrLexicon.Namespace.URI, false), is(nullValue()));
        assertThat(reg.getPrefixForNamespaceUri(NT_NAMESPACE_URI, false), is(nullValue()));
        assertThat(reg.getPrefixForNamespaceUri("http://default.namespace.com", false), is(nullValue()));
        // Parse the XML file ...
        parse("xmlImport/docWithNestedNamespaces.xml");
        // Get the prefix for the default namespace ...
        String c = reg.getPrefixForNamespaceUri("http://default.namespace.com", false);
        String i = reg.getPrefixForNamespaceUri("http://attributes.com", false);
        String d = reg.getPrefixForNamespaceUri(reg.getDefaultNamespaceUri(), false);
        assertThat("Namespace not properly registered in primary registry", c, is(notNullValue()));
        assertThat("Namespace not properly registered in primary registry", d, is(notNullValue()));
        assertThat("Namespace not properly registered in primary registry", i, is(notNullValue()));
        if (c.length() != 0) {
            c = c + ":";
        }
        if (d.length() != 0) {
            d = d + ":";
        }
        if (i.length() != 0) {
            i = i + ":";
        }
        // Check the generated content; note that the attribute name DOES match, so the nodes names come from "jcr:name" attribute
        assertNode(d + "Cars");
        assertNode(d + "Cars/" + c + "Hybrid");
        assertNode(d + "Cars/" + c + "Hybrid/" + c + "Toyota Prius", c + "maker=Toyota", c + "model=Prius");
        assertNode(d + "Cars/" + c + "Hybrid/" + c + "Toyota Highlander", c + "maker=Toyota", c + "model=Highlander");
        assertNode(d + "Cars/" + c + "Hybrid/" + c + "Nissan Altima", c + "maker=Nissan", c + "model=Altima");
        assertNode(d + "Cars/" + d + "Sports");
        assertNode(d + "Cars/" + d + "Sports/Aston Martin DB9", i + "maker=Aston Martin", "model=DB9");
        assertNode(d + "Cars/" + d + "Sports/Infiniti G37", i + "maker=Infiniti", "model=G37");
    }

    @Test
    public void shouldParseXmlDocumentWithoutDefaultNamespaceThatUsesNameAttribute() throws Exception {
        parse("xmlImport/docWithNamespacesWithoutDefault.xml");
        // Check the generated content; note that the attribute name DOES match, so the nodes names come from "jcr:name" attribute
        assertNode("Cars");
        assertNode("Cars/Hybrid");
        assertNode("Cars/Hybrid/Toyota Prius", "maker=Toyota", "model=Prius");
        assertNode("Cars/Hybrid/Toyota Highlander", "maker=Toyota", "model=Highlander");
        assertNode("Cars/Hybrid/Nissan Altima", "maker=Nissan", "model=Altima");
        assertNode("Cars/Sports");
        assertNode("Cars/Sports/Aston Martin DB9", "maker=Aston Martin", "model=DB9");
        assertNode("Cars/Sports/Infiniti G37", "maker=Infiniti", "model=G37");
    }

    @Test
    public void shouldParseXmlDocumentThatContainsNoContent() throws Exception {
        parse("xmlImport/docWithOnlyRootElement.xml");
        assertTrue(parseResults.isEmpty());
    }

    @Test
    public void shouldParseXmlDocumentWithXmlComments() throws Exception {
        context.getNamespaceRegistry().register("c", "http://default.namespace.com");
        parse("xmlImport/docWithComments.xml");
        assertNode("c:Cars");
        assertNode("c:Cars/c:Hybrid");
        assertNode("c:Cars/c:Hybrid/c:Toyota Prius", "c:maker=Toyota", "c:model=Prius");
        assertNode("c:Cars/c:Hybrid/c:Toyota Highlander", "c:maker=Toyota", "c:model=Highlander");
        assertNode("c:Cars/c:Hybrid/c:Nissan Altima", "c:maker=Nissan", "c:model=Altima");
        assertNode("c:Cars/c:Sports");
        assertNode("c:Cars/c:Sports/c:Aston Martin DB9", "c:maker=Aston Martin", "c:model=DB9");
        assertNode("c:Cars/c:Sports/c:Infiniti G37", "c:maker=Infiniti", "c:model=G37");
    }

    @Test
    public void shouldParseXmlDocumentWithoutNamespacesUsingTypeAttributeValue() throws Exception {
        String typeAttribute = JcrConstants.JCR_PRIMARY_TYPE;
        String typeAttributeValue = JcrConstants.NT_UNSTRUCTURED;
        handler = new NodeImportXmlHandler(parseDestination, null, typeAttribute, typeAttributeValue, null);
        parse("xmlImport/docWithoutNamespaces.xml");
        // Check the generated content; note that the attribute name doesn't match, so the nodes don't get special names
        String unstructPrimaryType = "jcr:primaryType=nt:unstructured";
        assertNode("Cars", unstructPrimaryType);
        assertNode("Cars/Hybrid", unstructPrimaryType);
        assertNode("Cars/Hybrid/car", unstructPrimaryType, "name=Toyota Prius", "maker=Toyota", "model=Prius");
        assertNode("Cars/Hybrid/car[2]", unstructPrimaryType, "name=Toyota Highlander", "maker=Toyota", "model=Highlander");
        assertNode("Cars/Hybrid/car[3]", unstructPrimaryType, "name=Nissan Altima", "maker=Nissan", "model=Altima");
        assertNode("Cars/Sports", unstructPrimaryType);
        assertNode("Cars/Sports/car", unstructPrimaryType, "name=Aston Martin DB9", "maker=Aston Martin", "model=DB9");
        assertNode("Cars/Sports/car[2]", unstructPrimaryType, "name=Infiniti G37", "maker=Infiniti", "model=G37");
    }

    @Test
    public void shouldParseXmlDocumentWithNamespacesUsingTypeAttributeValue() throws Exception {
        context.getNamespaceRegistry().register("c", "http://default.namespace.com");
        String nameAttribute = JcrConstants.JCR_NAME;
        String typeAttribute = JcrConstants.JCR_PRIMARY_TYPE;
        String typeAttributeValue = JcrConstants.NT_UNSTRUCTURED;
        handler = new NodeImportXmlHandler(parseDestination, nameAttribute, typeAttribute, typeAttributeValue, null);
        parse("xmlImport/docWithNamespaces.xml");
        // Check the generated content; note that the attribute name doesn't match, so the nodes don't get special names
        String unstructPrimaryType = "jcr:primaryType=nt:unstructured";
        String carPrimaryType = "jcr:primaryType={http://default.namespace.com}car";
        assertNode("c:Cars", unstructPrimaryType);
        assertNode("c:Cars/c:Hybrid", unstructPrimaryType);
        assertNode("c:Cars/c:Hybrid/c:Toyota Prius", carPrimaryType, "c:maker=Toyota", "c:model=Prius");
        assertNode("c:Cars/c:Hybrid/c:Toyota Highlander", carPrimaryType, "c:maker=Toyota", "c:model=Highlander");
        assertNode("c:Cars/c:Hybrid/c:Nissan Altima", carPrimaryType, "c:maker=Nissan", "c:model=Altima");
        assertNode("c:Cars/c:Sports", unstructPrimaryType);
        assertNode("c:Cars/c:Sports/c:Aston Martin DB9", carPrimaryType, "c:maker=Aston Martin", "c:model=DB9");
        assertNode("c:Cars/c:Sports/c:Infiniti G37", carPrimaryType, "c:maker=Infiniti", "c:model=G37");
    }

    @Test
    public void shouldParseXmlDocumentWithNestedProperties() throws Exception {
        context.getNamespaceRegistry().register("jcr", "http://www.jcp.org/jcr/1.0");
        handler = new NodeImportXmlHandler(parseDestination, JcrConstants.JCR_NAME, null, null, null);
        parse("xmlImport/docWithNestedProperties.xml");
        // Check the generated content; note that the attribute name DOES match, so the nodes names come from "jcr:name" attribute
        assertNode("Cars");
        assertNode("Cars/Hybrid");
        assertNode("Cars/Hybrid/car", "name=Toyota Prius", "maker=Toyota", "model=Prius");
        assertNode("Cars/Hybrid/car[2]", "name=Toyota Highlander", "maker=Toyota", "model=Highlander");
        assertNode("Cars/Hybrid/car[3]", "name=Nissan Altima", "maker=Nissan", "model=Altima");
        assertNode("Cars/Sports");
        assertNode("Cars/Sports/car", "name=Aston Martin DB9", "maker=Aston Martin", "model=DB9");
        assertNode("Cars/Sports/car[2]", "name=Infiniti G37", "maker=Infiniti", "category=Turbocharged");
        assertNode("Cars/Sports/car[2]/driver", "name=Tony Stewart");
        assertNode("Cars/Sports/car[2]", "model=G37", "category=My Sedan");
        assertNode("Cars/Sports/car[3]");
        assertNode("Cars/Sports/car[3]/jcr:xmltext", "jcr:xmlcharacters=This is my text ");
        assertNode("Cars/Sports/car[3]/jcr:xmltext[2]", "jcr:xmlcharacters=that should be merged");
    }

    @Test
    public void shouldParseXmlDocumentWithMixins() throws Exception {
        parse("xmlImport/docWithMixins.xml");
        assertNode("cars", "jcr:mixinTypes=mix:created,mix:modified");
    }

    @Test
    public void shouldParseXmlDocumentWithCustomPrimaryType() throws Exception {
        parse("xmlImport/docWithCustomType.xml");
        assertNode("folder", "jcr:mixinTypes=mix:created,mix:modified", "jcr:primaryType=nt:folder");
        assertNode("folder/file", "jcr:primaryType=nt:file");
        assertNode("folder/file[2]", "jcr:primaryType=nt:file");
    }

    private void assertNode( String path,
                             String... properties ) {
        Path expectedPath = context.getValueFactories().getPathFactory().create("/" + path);

        NodeImportXmlHandler.ImportElement element = parseResults.get(expectedPath.toString());
        assertNotNull(path + " not found among parsed elements", element);

        for (String propertyValueString : properties) {
            String[] parts = propertyValueString.split("=");
            String propertyName = context.getValueFactories().getNameFactory().create(parts[0]).getString(
                    NoOpEncoder.getInstance());
            String propertyValue = parts[1];
            if (propertyName.equals(JcrConstants.JCR_PRIMARY_TYPE)) {
                assertEquals(propertyValue, element.getType());
            } else {
                Collection<String> actualPropertyValue = propertyName.equalsIgnoreCase(JcrConstants.JCR_MIXIN_TYPES)
                        ? element.getMixins() : element.getProperties().get(propertyName);
                assertNotNull(actualPropertyValue);
                String[] values = propertyValue.split(",");
                for (String value : values) {
                    assertTrue("Expected property not found: " + value, actualPropertyValue.contains(value));
                }
            }
        }
    }

    private void parse( String relativePathToXmlFile ) throws Exception {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(relativePathToXmlFile);
        try {
            XMLReader reader = XMLReaderFactory.createXMLReader();
            reader.setContentHandler(handler);
            reader.setErrorHandler(handler);
            reader.parse(new InputSource(stream));
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
    }


}
