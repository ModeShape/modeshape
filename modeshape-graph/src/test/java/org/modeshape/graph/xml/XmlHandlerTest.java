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
package org.modeshape.graph.xml;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.text.Jsr283Encoder;
import org.modeshape.common.text.TextDecoder;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.Location;
import org.modeshape.graph.io.Destination;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.request.CreateNodeRequest;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * @author Randall Hauch
 */
public class XmlHandlerTest {

    private static final String NT_NAMESPACE_URI = "http://www.jcp.org/jcr/nt/1.0";

    private XmlHandler handler;
    private ExecutionContext context;
    private Destination destination;
    private boolean skipRootElement = false;
    private Path parentPath;
    private TextDecoder decoder;
    private Name nameAttribute;
    private Name typeAttribute;
    private Name typeAttributeValue;
    private XmlHandler.AttributeScoping scoping;
    private LinkedList<CreateNodeRequest> requests;

    @Before
    public void beforeEach() {
        context = new ExecutionContext();
        context.getNamespaceRegistry().register(JcrLexicon.Namespace.PREFIX, JcrLexicon.Namespace.URI);
        context.getNamespaceRegistry().register("nt", NT_NAMESPACE_URI);
        destination = new RecordingDestination();
        parentPath = context.getValueFactories().getPathFactory().create("/a/b");
        decoder = null;
        nameAttribute = JcrLexicon.NAME;
        typeAttribute = null;
        typeAttributeValue = null;
        scoping = XmlHandler.AttributeScoping.USE_DEFAULT_NAMESPACE;
        handler = new XmlHandler(destination, skipRootElement, parentPath, decoder, nameAttribute, typeAttribute,
                                 typeAttributeValue, scoping);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotConstructInstanceWhenGivenNullDestination() {
        destination = null;
        new XmlHandler(destination, skipRootElement, parentPath, decoder, nameAttribute, typeAttribute, typeAttributeValue,
                       scoping);
    }

    @Test
    public void shouldUseDefaultDecoderIfNoneIsProvidedInConstructor() {
        decoder = null;
        handler = new XmlHandler(destination, skipRootElement, parentPath, decoder, nameAttribute, typeAttribute,
                                 typeAttributeValue, scoping);
        assertThat(handler.destination, is(sameInstance(destination)));
        assertThat(handler.currentPath, is(sameInstance(parentPath)));
        assertThat(handler.skipFirstElement, is(skipRootElement));
        assertThat(handler.decoder, is(sameInstance(XmlHandler.DEFAULT_DECODER)));
        assertThat(handler.nameAttribute, is(sameInstance(nameAttribute)));
    }

    @Test
    public void shouldUseDecoderProvidedInConstructor() {
        decoder = new Jsr283Encoder();
        handler = new XmlHandler(destination, skipRootElement, parentPath, decoder, nameAttribute, typeAttribute,
                                 typeAttributeValue, scoping);
        assertThat(handler.destination, is(sameInstance(destination)));
        assertThat(handler.currentPath, is(sameInstance(parentPath)));
        assertThat(handler.skipFirstElement, is(skipRootElement));
        assertThat(handler.decoder, is(sameInstance(decoder)));
        assertThat(handler.nameAttribute, is(sameInstance(nameAttribute)));
    }

    @Test
    public void shouldPlaceContentUnderRootIfNoPathIsProvidedInConstructor() {
        parentPath = null;
        handler = new XmlHandler(destination, skipRootElement, parentPath, decoder, nameAttribute, typeAttribute,
                                 typeAttributeValue, scoping);
        assertThat(handler.destination, is(sameInstance(destination)));
        assertThat(handler.currentPath.isRoot(), is(true));
        assertThat(handler.skipFirstElement, is(skipRootElement));
        assertThat(handler.decoder, is(sameInstance(decoder != null ? decoder : XmlHandler.DEFAULT_DECODER)));
        assertThat(handler.nameAttribute, is(sameInstance(nameAttribute)));
    }

    @Test
    public void shouldNotLookForNameAttributeIfNoneIsProvidedInConstructor() {
        nameAttribute = null;
        handler = new XmlHandler(destination, skipRootElement, parentPath, decoder, nameAttribute, typeAttribute,
                                 typeAttributeValue, scoping);
        assertThat(handler.destination, is(sameInstance(destination)));
        assertThat(handler.currentPath, is(sameInstance(parentPath)));
        assertThat(handler.skipFirstElement, is(skipRootElement));
        assertThat(handler.decoder, is(sameInstance(decoder != null ? decoder : XmlHandler.DEFAULT_DECODER)));
        assertThat(handler.nameAttribute, is(nullValue()));
    }

    @Test
    public void shouldParseXmlDocumentWithoutNamespaces() throws IOException, SAXException {
        // System.out.println("\n");
        // System.out.flush();
        parse("xmlHandler/docWithoutNamespaces.xml");
        // System.out.println("\n");
        // System.out.flush();
        // Check the generated content; note that the attribute name doesn't match, so the nodes don't get special names
        assertNode("Cars");
        assertNode("Cars/Hybrid");
        assertNode("Cars/Hybrid/car", "name=Toyota Prius", "maker=Toyota", "model=Prius");
        assertNode("Cars/Hybrid/car", "name=Toyota Highlander", "maker=Toyota", "model=Highlander");
        assertNode("Cars/Hybrid/car", "name=Nissan Altima", "maker=Nissan", "model=Altima");
        assertNode("Cars/Sports");
        assertNode("Cars/Sports/car", "name=Aston Martin DB9", "maker=Aston Martin", "model=DB9");
        assertNode("Cars/Sports/car", "name=Infiniti G37", "maker=Infiniti", "model=G37");
    }

    @Test
    public void shouldParseXmlDocumentWithNamespaces() throws IOException, SAXException {
        context.getNamespaceRegistry().register("c", "http://default.namespace.com");
        parse("xmlHandler/docWithNamespaces.xml");
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
    public void shouldParseXmlDocumentWithNestedNamespaceDeclarations() throws IOException, SAXException {
        context.getNamespaceRegistry().register("c", "http://default.namespace.com");
        context.getNamespaceRegistry().register("i", "http://attributes.com");
        parse("xmlHandler/docWithNestedNamespaces.xml");
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
    public void shouldParseXmlDocumentWithNamespacePrefixesThatDoNotMatchRegistry() throws IOException, SAXException {
        context.getNamespaceRegistry().register("c", "http://default.namespace.com");
        parse("xmlHandler/docWithNamespaces.xml");
        // Check the generated content; note that the attribute name DOES match, so the nodes names come from "jcr:name" attribute
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
    public void shouldParseXmlDocumentWithNamespacesThatAreNotYetInRegistry() throws IOException, SAXException {
        NamespaceRegistry reg = context.getNamespaceRegistry();
        reg.unregister(JcrLexicon.Namespace.URI);
        reg.unregister(NT_NAMESPACE_URI);
        // Verify the prefixes don't exist ...
        assertThat(reg.getPrefixForNamespaceUri(JcrLexicon.Namespace.URI, false), is(nullValue()));
        assertThat(reg.getPrefixForNamespaceUri(NT_NAMESPACE_URI, false), is(nullValue()));
        assertThat(reg.getPrefixForNamespaceUri("http://default.namespace.com", false), is(nullValue()));
        // Parse the XML file ...
        parse("xmlHandler/docWithNestedNamespaces.xml");
        // Get the prefix for the default namespace ...
        String c = reg.getPrefixForNamespaceUri("http://default.namespace.com", false);
        String i = reg.getPrefixForNamespaceUri("http://attributes.com", false);
        String d = reg.getPrefixForNamespaceUri(reg.getDefaultNamespaceUri(), false);
        assertThat("Namespace not properly registered in primary registry", c, is(notNullValue()));
        assertThat("Namespace not properly registered in primary registry", d, is(notNullValue()));
        assertThat("Namespace not properly registered in primary registry", i, is(notNullValue()));
        if (c.length() != 0) c = c + ":";
        if (d.length() != 0) d = d + ":";
        if (i.length() != 0) i = i + ":";
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
    public void shouldParseXmlDocumentThatUsesNameAttribute() throws IOException, SAXException {
        context.getNamespaceRegistry().register("c", "http://default.namespace.com");
        parse("xmlHandler/docWithNamespaces.xml");
        // Check the generated content; note that the attribute name DOES match, so the nodes names come from "jcr:name" attribute
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
    public void shouldParseXmlDocumentWithoutDefaultNamespaceThatUsesNameAttribute() throws IOException, SAXException {
        parse("xmlHandler/docWithNamespacesWithoutDefault.xml");
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
    public void shouldParseXmlDocumentThatContainsNoContent() throws IOException, SAXException {
        parse("xmlHandler/docWithOnlyRootElement.xml");
        assertNode("Cars");
    }

    @Test
    public void shouldParseXmlDocumentAndShouldNotCreateNodeForRootElement() throws IOException, SAXException {
        context.getNamespaceRegistry().register("c", "http://default.namespace.com");
        parentPath = null;
        skipRootElement = true;
        handler = new XmlHandler(destination, skipRootElement, parentPath, decoder, nameAttribute, typeAttribute,
                                 typeAttributeValue, scoping);
        parse("xmlHandler/docWithNamespaces.xml");
        // Check the generated content; note that the attribute name DOES match, so the nodes names come from "jcr:name" attribute
        assertNode("c:Hybrid");
        assertNode("c:Hybrid/c:Toyota Prius", "c:maker=Toyota", "c:model=Prius");
        assertNode("c:Hybrid/c:Toyota Highlander", "c:maker=Toyota", "c:model=Highlander");
        assertNode("c:Hybrid/c:Nissan Altima", "c:maker=Nissan", "c:model=Altima");
        assertNode("c:Sports");
        assertNode("c:Sports/c:Aston Martin DB9", "c:maker=Aston Martin", "c:model=DB9");
        assertNode("c:Sports/c:Infiniti G37", "c:maker=Infiniti", "c:model=G37");
    }

    @Test
    public void shouldParseXmlDocumentAndShouldPlaceContentUnderNonRootNode() throws IOException, SAXException {
        parentPath = context.getValueFactories().getPathFactory().create("/a/b");
        handler = new XmlHandler(destination, skipRootElement, parentPath, decoder, nameAttribute, typeAttribute,
                                 typeAttributeValue, scoping);
        context.getNamespaceRegistry().register("c", "http://default.namespace.com");
        parse("xmlHandler/docWithNamespaces.xml");
        // Check the generated content; note that the attribute name DOES match, so the nodes names come from "jcr:name" attribute
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
    public void shouldParseXmlDocumentAndShouldPlaceContentUnderRootNode() throws IOException, SAXException {
        parentPath = null;
        handler = new XmlHandler(destination, skipRootElement, parentPath, decoder, nameAttribute, typeAttribute,
                                 typeAttributeValue, scoping);
        context.getNamespaceRegistry().register("c", "http://default.namespace.com");
        parse("xmlHandler/docWithNamespaces.xml");
        // Check the generated content; note that the attribute name DOES match, so the nodes names come from "jcr:name" attribute
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
    public void shouldParseXmlDocumentWithXmlComments() throws IOException, SAXException {
        context.getNamespaceRegistry().register("c", "http://default.namespace.com");
        parse("xmlHandler/docWithComments.xml");
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
    public void shouldParseXmlDocumentWithoutNamespacesUsingTypeAttributeValue() throws IOException, SAXException {
        typeAttribute = JcrLexicon.PRIMARY_TYPE;
        typeAttributeValue = context.getValueFactories().getNameFactory().create("nt:unstructured");
        handler = new XmlHandler(destination, skipRootElement, parentPath, decoder, nameAttribute, typeAttribute,
                                 typeAttributeValue, scoping);
        parse("xmlHandler/docWithoutNamespaces.xml");
        // Check the generated content; note that the attribute name doesn't match, so the nodes don't get special names
        String unstructPrimaryType = "jcr:primaryType={http://www.jcp.org/jcr/nt/1.0}unstructured";
        assertNode("Cars");
        assertNode("Cars/Hybrid");
        assertNode("Cars/Hybrid/car", unstructPrimaryType, "name=Toyota Prius", "maker=Toyota", "model=Prius");
        assertNode("Cars/Hybrid/car", unstructPrimaryType, "name=Toyota Highlander", "maker=Toyota", "model=Highlander");
        assertNode("Cars/Hybrid/car", unstructPrimaryType, "name=Nissan Altima", "maker=Nissan", "model=Altima");
        assertProperties("Cars/Hybrid", unstructPrimaryType);
        assertNode("Cars/Sports");
        assertNode("Cars/Sports/car", unstructPrimaryType, "name=Aston Martin DB9", "maker=Aston Martin", "model=DB9");
        assertNode("Cars/Sports/car", unstructPrimaryType, "name=Infiniti G37", "maker=Infiniti", "model=G37");
        assertProperties("Cars/Sports", unstructPrimaryType);
        assertProperties("Cars", unstructPrimaryType);

    }

    @Test
    public void shouldParseXmlDocumentWithNamespacesUsingTypeAttributeValue() throws IOException, SAXException {
        context.getNamespaceRegistry().register("c", "http://default.namespace.com");
        typeAttribute = JcrLexicon.PRIMARY_TYPE;
        typeAttributeValue = context.getValueFactories().getNameFactory().create("nt:unstructured");
        handler = new XmlHandler(destination, skipRootElement, parentPath, decoder, nameAttribute, typeAttribute,
                                 typeAttributeValue, scoping);
        parse("xmlHandler/docWithNamespaces.xml");
        // Check the generated content; note that the attribute name doesn't match, so the nodes don't get special names
        String unstructPrimaryType = "jcr:primaryType={http://www.jcp.org/jcr/nt/1.0}unstructured";
        String carPrimaryType = "jcr:primaryType={http://default.namespace.com}car";
        assertNode("c:Cars");
        assertNode("c:Cars/c:Hybrid");
        assertNode("c:Cars/c:Hybrid/c:Toyota Prius", carPrimaryType, "c:maker=Toyota", "c:model=Prius");
        assertNode("c:Cars/c:Hybrid/c:Toyota Highlander", carPrimaryType, "c:maker=Toyota", "c:model=Highlander");
        assertNode("c:Cars/c:Hybrid/c:Nissan Altima", carPrimaryType, "c:maker=Nissan", "c:model=Altima");
        assertProperties("c:Cars/c:Hybrid", unstructPrimaryType);
        assertNode("c:Cars/c:Sports");
        assertNode("c:Cars/c:Sports/c:Aston Martin DB9", carPrimaryType, "c:maker=Aston Martin", "c:model=DB9");
        assertNode("c:Cars/c:Sports/c:Infiniti G37", carPrimaryType, "c:maker=Infiniti", "c:model=G37");
        assertProperties("c:Cars/c:Sports", unstructPrimaryType);
        assertProperties("c:Cars", unstructPrimaryType);
    }

    @Test
    public void shouldParseXmlDocumentWithNestedPropertiesShouldPlaceContentUnderRootNode() throws IOException, SAXException {
        context.getNamespaceRegistry().register("jcr", "http://www.jcp.org/jcr/1.0");
        handler = new XmlHandler(destination, skipRootElement, parentPath, decoder, nameAttribute, typeAttribute,
                                 typeAttributeValue, scoping);
        parse("xmlHandler/docWithNestedProperties.xml");
        // Check the generated content; note that the attribute name DOES match, so the nodes names come from "jcr:name" attribute
        assertNode("Cars");
        assertNode("Cars/Hybrid");
        assertNode("Cars/Hybrid/car", "name=Toyota Prius", "maker=Toyota", "model=Prius");
        assertNode("Cars/Hybrid/car[2]", "name=Toyota Highlander", "maker=Toyota", "model=Highlander");
        assertNode("Cars/Hybrid/car[3]");
        assertProperties("Cars/Hybrid/car[3]", "name=Nissan Altima", "maker=Nissan", "model=Altima");
        assertNode("Cars/Sports");
        assertNode("Cars/Sports/car", "name=Aston Martin DB9", "maker=Aston Martin", "model=DB9");
        assertNode("Cars/Sports/car");
        assertNode("Cars/Sports/car[2]/driver", "name=Tony Stewart");
        assertProperties("Cars/Sports/car[2]",
                         "name=Infiniti G37",
                         "maker=Infiniti",
                         "model=G37",
                         "category=Turbocharged=My Sedan");
        assertNode("Cars/Sports/car[3]");
        assertNode("Cars/Sports/car[3]/jcr:xmltext", "jcr:xmlcharacters=This is my text ");
        assertNode("Cars/Sports/car[3]/jcr:xmltext", "jcr:xmlcharacters=that should be merged");
        assertProperties("Cars/Sports/car", "name=Infiniti G37", "maker=Infiniti", "model=G37");
    }

    protected void assertNode( String path,
                               String... properties ) {
        // Create the expected path ...
        PathFactory factory = context.getValueFactories().getPathFactory();
        Path expectedPath = parentPath != null ? factory.create(parentPath, path) : factory.create("/" + path);
        // Now get the next request and compare the expected and actual ...
        CreateNodeRequest request = requests.remove();
        Path parentPath = request.under().getPath();
        assertThat(parentPath, is(expectedPath.getParent()));
        assertThat(request.named(), is(expectedPath.getLastSegment().getName()));

        if (properties.length != 0) {
            // Create the list of properties ...
            Map<Name, Property> expectedProperties = new HashMap<Name, Property>();
            for (String propertyString : properties) {
                String[] strings = propertyString.split("=");
                if (strings.length < 2) continue;
                Name name = context.getValueFactories().getNameFactory().create(strings[0]);
                Object[] values = new Object[strings.length - 1];
                for (int i = 1; i != strings.length; ++i) {
                    values[i - 1] = strings[i];
                }
                Property property = context.getPropertyFactory().create(name, values);
                expectedProperties.put(name, property);
            }

            for (Property actual : request.properties()) {
                Property expected = expectedProperties.remove(actual.getName());
                assertThat("unexpected property: " + actual, expected, is(notNullValue()));
                assertThat(actual, is(expected));
            }
            if (!expectedProperties.isEmpty()) {
                StringBuilder msg = new StringBuilder("missing actual properties: ");
                boolean isFirst = true;
                for (Property expected : expectedProperties.values()) {
                    if (!isFirst) msg.append(", ");
                    else isFirst = false;
                    msg.append(expected.getName());
                }
                msg.append(" on node ").append(request.under());
                // System.out.println("Found properties: " + request.properties());
                assertThat(msg.toString(), expectedProperties.isEmpty(), is(true));
            }
        }
    }

    protected void assertProperties( String path,
                                     String... properties ) {
        // Create the expected path ...
        PathFactory factory = context.getValueFactories().getPathFactory();
        Path expectedPath = parentPath != null ? factory.create(parentPath, path) : factory.create("/" + path);
        // Create the list of properties ...
        Map<Name, Property> expectedProperties = new HashMap<Name, Property>();
        for (String propertyString : properties) {
            String[] strings = propertyString.split("=");
            if (strings.length < 2) continue;
            Name name = context.getValueFactories().getNameFactory().create(strings[0]);
            Object[] values = new Object[strings.length - 1];
            for (int i = 1; i != strings.length; ++i) {
                values[i - 1] = strings[i];
            }
            Property property = context.getPropertyFactory().create(name, values);
            expectedProperties.put(name, property);
        }

        CreateNodeRequest propertyRequest = requests.remove();
        Path parentPath = propertyRequest.under().getPath();
        assertThat(parentPath, is(expectedPath.getParent()));
        assertThat(propertyRequest.named(), is(expectedPath.getLastSegment().getName()));

        for (Property actual : propertyRequest.properties()) {
            Property expected = expectedProperties.remove(actual.getName());
            assertThat("unexpected property: " + actual, expected, is(notNullValue()));
            assertThat(actual, is(expected));
        }
        if (!expectedProperties.isEmpty()) {
            StringBuilder msg = new StringBuilder("missing actual properties: ");
            boolean isFirst = true;
            for (Property expected : expectedProperties.values()) {
                if (!isFirst) msg.append(", ");
                else isFirst = false;
                msg.append(expected.getName());
            }
            msg.append(" on node ").append(propertyRequest.under());
            // System.out.println("Found properties: " + propertyRequest.properties());
            assertThat(msg.toString(), expectedProperties.isEmpty(), is(true));
        }
    }

    protected void parse( String relativePathToXmlFile ) throws IOException, SAXException {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(relativePathToXmlFile);
        try {
            XMLReader reader = XMLReaderFactory.createXMLReader();
            reader.setContentHandler(handler);
            reader.setErrorHandler(handler);
            reader.parse(new InputSource(stream));
        } finally {
            if (stream != null) stream.close();
        }
    }

    protected class RecordingDestination implements Destination {
        private final LinkedList<CreateNodeRequest> requests = new LinkedList<CreateNodeRequest>();
        private final String workspace = "Recording Workspace";

        public void create( Path path,
                            Iterable<Property> properties ) {
            assert path != null;
            Path parent = path.getParent();
            Name child = path.getLastSegment().getName();
            requests.add(new CreateNodeRequest(Location.create(parent), workspace, child, properties));
        }

        public void create( final Path path,
                            final Property firstProperty,
                            final Property... additionalProperties ) {
            Path parent = path.getParent();
            Name child = path.getLastSegment().getName();
            Location location = Location.create(parent);
            if (firstProperty == null) {
                requests.add(new CreateNodeRequest(location, workspace, child));
            } else {
                if (additionalProperties == null || additionalProperties.length == 0) {
                    requests.add(new CreateNodeRequest(location, workspace, child, firstProperty));
                } else {
                    Iterator<Property> iter = new Iterator<Property>() {
                        private int index = -1;

                        public boolean hasNext() {
                            return index < additionalProperties.length;
                        }

                        public Property next() {
                            if (index == -1) {
                                ++index;
                                return firstProperty;
                            }
                            return additionalProperties[index++];
                        }

                        public void remove() {
                            throw new UnsupportedOperationException();
                        }
                    };
                    requests.add(new CreateNodeRequest(location, workspace, child, iter));
                }
            }
        }

        public void setProperties( Path path,
                                   Property... properties ) {
            if (properties.length == 0) {
                return;
            } else if (properties.length == 1) {
                create(path, properties[0]);
            } else {
                Property[] additionalProperties = new Property[properties.length - 1];
                System.arraycopy(properties, 1, additionalProperties, 0, properties.length - 1);
                create(path, properties[0], additionalProperties);
            }
        }

        @SuppressWarnings( "synthetic-access" )
        public ExecutionContext getExecutionContext() {
            return XmlHandlerTest.this.context;
        }

        @SuppressWarnings( "synthetic-access" )
        public void submit() {
            XmlHandlerTest.this.requests = requests;
        }
    }
}
