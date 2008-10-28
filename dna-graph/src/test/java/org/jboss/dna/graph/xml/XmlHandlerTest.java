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
package org.jboss.dna.graph.xml;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.jboss.dna.common.text.Jsr283Encoder;
import org.jboss.dna.common.text.TextDecoder;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.connectors.BasicExecutionContext;
import org.jboss.dna.graph.properties.Name;
import org.jboss.dna.graph.properties.NamespaceRegistry;
import org.jboss.dna.graph.properties.Path;
import org.jboss.dna.graph.properties.PathFactory;
import org.jboss.dna.graph.properties.Property;
import org.jboss.dna.graph.requests.CreateNodeRequest;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * @author Randall Hauch
 */
public class XmlHandlerTest {

    private static final String JCR_NAMESPACE_URI = "http://www.jcp.org/jcr/1.0";

    private XmlHandler handler;
    private ExecutionContext context;
    private XmlHandler.Destination destination;
    private boolean skipRootElement = false;
    private Path parentPath;
    private TextDecoder decoder;
    private Name nameAttribute;
    private XmlHandler.AttributeScoping scoping;
    private LinkedList<CreateNodeRequest> requests;

    @Before
    public void beforeEach() {
        context = new BasicExecutionContext();
        context.getNamespaceRegistry().register("jcr", JCR_NAMESPACE_URI);
        destination = new RecordingDestination();
        parentPath = context.getValueFactories().getPathFactory().create("/a/b");
        decoder = null;
        nameAttribute = context.getValueFactories().getNameFactory().create("jcr:name");
        scoping = XmlHandler.AttributeScoping.USE_DEFAULT_NAMESPACE;
        handler = new XmlHandler(destination, skipRootElement, parentPath, decoder, nameAttribute, scoping);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotConstructInstanceWhenGivenNullDestination() {
        destination = null;
        new XmlHandler(destination, skipRootElement, parentPath, decoder, nameAttribute, scoping);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotConstructInstanceWhenGivenNullGraph() {
        Graph graph = null;
        new XmlHandler(graph, true, skipRootElement, parentPath, decoder, nameAttribute, scoping);
    }

    @Test
    public void shouldUseDefaultDecoderIfNoneIsProvidedInConstructor() {
        decoder = null;
        handler = new XmlHandler(destination, skipRootElement, parentPath, decoder, nameAttribute, scoping);
        assertThat(handler.destination, is(sameInstance(destination)));
        assertThat(handler.currentPath, is(sameInstance(parentPath)));
        assertThat(handler.skipFirstElement, is(skipRootElement));
        assertThat(handler.decoder, is(sameInstance(XmlHandler.DEFAULT_DECODER)));
        assertThat(handler.nameAttribute, is(sameInstance(nameAttribute)));
    }

    @Test
    public void shouldUseDecoderProvidedInConstructor() {
        decoder = new Jsr283Encoder();
        handler = new XmlHandler(destination, skipRootElement, parentPath, decoder, nameAttribute, scoping);
        assertThat(handler.destination, is(sameInstance(destination)));
        assertThat(handler.currentPath, is(sameInstance(parentPath)));
        assertThat(handler.skipFirstElement, is(skipRootElement));
        assertThat(handler.decoder, is(sameInstance(decoder)));
        assertThat(handler.nameAttribute, is(sameInstance(nameAttribute)));
    }

    @Test
    public void shouldPlaceContentUnderRootIfNoPathIsProvidedInConstructor() {
        parentPath = null;
        handler = new XmlHandler(destination, skipRootElement, parentPath, decoder, nameAttribute, scoping);
        assertThat(handler.destination, is(sameInstance(destination)));
        assertThat(handler.currentPath.isRoot(), is(true));
        assertThat(handler.skipFirstElement, is(skipRootElement));
        assertThat(handler.decoder, is(sameInstance(decoder != null ? decoder : XmlHandler.DEFAULT_DECODER)));
        assertThat(handler.nameAttribute, is(sameInstance(nameAttribute)));
    }

    @Test
    public void shouldNotLookForNameAttributeIfNoneIsProvidedInConstructor() {
        nameAttribute = null;
        handler = new XmlHandler(destination, skipRootElement, parentPath, decoder, nameAttribute, scoping);
        assertThat(handler.destination, is(sameInstance(destination)));
        assertThat(handler.currentPath, is(sameInstance(parentPath)));
        assertThat(handler.skipFirstElement, is(skipRootElement));
        assertThat(handler.decoder, is(sameInstance(decoder != null ? decoder : XmlHandler.DEFAULT_DECODER)));
        assertThat(handler.nameAttribute, is(nullValue()));
    }

    @Test
    public void shouldParseXmlDocumentWithoutNamespaces() throws IOException, SAXException {
        parse("xmlHandler/docWithoutNamespaces.xml");
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
        // Check the generated content; note that the attribute name DOES match, so the nodes names come from "jcr:name" attribute
        assertNode("c:Cars");
        assertNode("c:Cars/c:Hybrid");
        assertNode("c:Cars/c:Hybrid/Toyota Prius", "maker=Toyota", "model=Prius");
        assertNode("c:Cars/c:Hybrid/Toyota Highlander", "maker=Toyota", "model=Highlander");
        assertNode("c:Cars/c:Hybrid/Nissan Altima", "maker=Nissan", "model=Altima");
        assertNode("c:Cars/c:Sports");
        assertNode("c:Cars/c:Sports/Aston Martin DB9", "maker=Aston Martin", "model=DB9");
        assertNode("c:Cars/c:Sports/Infiniti G37", "maker=Infiniti", "model=G37");
    }

    @Test
    public void shouldParseXmlDocumentWithNestedNamespaceDeclarations() throws IOException, SAXException {
        context.getNamespaceRegistry().register("c", "http://default.namespace.com");
        context.getNamespaceRegistry().register("i", "http://attributes.com");
        parse("xmlHandler/docWithNestedNamespaces.xml");
        // Check the generated content; note that the attribute name DOES match, so the nodes names come from "jcr:name" attribute
        assertNode("Cars");
        assertNode("Cars/c:Hybrid");
        assertNode("Cars/c:Hybrid/Toyota Prius", "maker=Toyota", "model=Prius");
        assertNode("Cars/c:Hybrid/Toyota Highlander", "maker=Toyota", "model=Highlander");
        assertNode("Cars/c:Hybrid/Nissan Altima", "maker=Nissan", "model=Altima");
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
        assertNode("c:Cars/c:Hybrid/Toyota Prius", "maker=Toyota", "model=Prius");
        assertNode("c:Cars/c:Hybrid/Toyota Highlander", "maker=Toyota", "model=Highlander");
        assertNode("c:Cars/c:Hybrid/Nissan Altima", "maker=Nissan", "model=Altima");
        assertNode("c:Cars/c:Sports");
        assertNode("c:Cars/c:Sports/Aston Martin DB9", "maker=Aston Martin", "model=DB9");
        assertNode("c:Cars/c:Sports/Infiniti G37", "maker=Infiniti", "model=G37");
    }

    @Test
    public void shouldParseXmlDocumentWithNamespacesThatAreNotYetInRegistry() throws IOException, SAXException {
        NamespaceRegistry reg = context.getNamespaceRegistry();
        reg.unregister(JCR_NAMESPACE_URI);
        // Verify the prefixes don't exist ...
        assertThat(reg.getPrefixForNamespaceUri(JCR_NAMESPACE_URI, false), is(nullValue()));
        assertThat(reg.getPrefixForNamespaceUri("http://default.namespace.com", false), is(nullValue()));
        // Parse the XML file ...
        parse("xmlHandler/docWithNestedNamespaces.xml");
        // Get the prefix for the default namespace ...
        String c = reg.getPrefixForNamespaceUri("http://default.namespace.com", false);
        String i = reg.getPrefixForNamespaceUri("http://attributes.com", false);
        String d = reg.getPrefixForNamespaceUri(reg.getDefaultNamespaceUri(), false);
        if (c.length() != 0) c = c + ":";
        if (d.length() != 0) d = d + ":";
        if (i.length() != 0) i = i + ":";
        // Check the generated content; note that the attribute name DOES match, so the nodes names come from "jcr:name" attribute
        assertNode(d + "Cars");
        assertNode(d + "Cars/" + c + "Hybrid");
        assertNode(d + "Cars/" + c + "Hybrid/Toyota Prius", "maker=Toyota", "model=Prius");
        assertNode(d + "Cars/" + c + "Hybrid/Toyota Highlander", "maker=Toyota", "model=Highlander");
        assertNode(d + "Cars/" + c + "Hybrid/Nissan Altima", "maker=Nissan", "model=Altima");
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
        assertNode("c:Cars/c:Hybrid/Toyota Prius", "maker=Toyota", "model=Prius");
        assertNode("c:Cars/c:Hybrid/Toyota Highlander", "maker=Toyota", "model=Highlander");
        assertNode("c:Cars/c:Hybrid/Nissan Altima", "maker=Nissan", "model=Altima");
        assertNode("c:Cars/c:Sports");
        assertNode("c:Cars/c:Sports/Aston Martin DB9", "maker=Aston Martin", "model=DB9");
        assertNode("c:Cars/c:Sports/Infiniti G37", "maker=Infiniti", "model=G37");
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
        handler = new XmlHandler(destination, skipRootElement, parentPath, decoder, nameAttribute, scoping);
        parse("xmlHandler/docWithNamespaces.xml");
        // Check the generated content; note that the attribute name DOES match, so the nodes names come from "jcr:name" attribute
        assertNode("c:Hybrid");
        assertNode("c:Hybrid/Toyota Prius", "maker=Toyota", "model=Prius");
        assertNode("c:Hybrid/Toyota Highlander", "maker=Toyota", "model=Highlander");
        assertNode("c:Hybrid/Nissan Altima", "maker=Nissan", "model=Altima");
        assertNode("c:Sports");
        assertNode("c:Sports/Aston Martin DB9", "maker=Aston Martin", "model=DB9");
        assertNode("c:Sports/Infiniti G37", "maker=Infiniti", "model=G37");
    }

    @Test
    public void shouldParseXmlDocumentAndShouldPlaceContentUnderNonRootNode() throws IOException, SAXException {
        parentPath = context.getValueFactories().getPathFactory().create("/a/b");
        handler = new XmlHandler(destination, skipRootElement, parentPath, decoder, nameAttribute, scoping);
        context.getNamespaceRegistry().register("c", "http://default.namespace.com");
        parse("xmlHandler/docWithNamespaces.xml");
        // Check the generated content; note that the attribute name DOES match, so the nodes names come from "jcr:name" attribute
        assertNode("c:Cars");
        assertNode("c:Cars/c:Hybrid");
        assertNode("c:Cars/c:Hybrid/Toyota Prius", "maker=Toyota", "model=Prius");
        assertNode("c:Cars/c:Hybrid/Toyota Highlander", "maker=Toyota", "model=Highlander");
        assertNode("c:Cars/c:Hybrid/Nissan Altima", "maker=Nissan", "model=Altima");
        assertNode("c:Cars/c:Sports");
        assertNode("c:Cars/c:Sports/Aston Martin DB9", "maker=Aston Martin", "model=DB9");
        assertNode("c:Cars/c:Sports/Infiniti G37", "maker=Infiniti", "model=G37");
    }

    @Test
    public void shouldParseXmlDocumentAndShouldPlaceContentUnderRootNode() throws IOException, SAXException {
        parentPath = null;
        handler = new XmlHandler(destination, skipRootElement, parentPath, decoder, nameAttribute, scoping);
        context.getNamespaceRegistry().register("c", "http://default.namespace.com");
        parse("xmlHandler/docWithNamespaces.xml");
        // Check the generated content; note that the attribute name DOES match, so the nodes names come from "jcr:name" attribute
        assertNode("c:Cars");
        assertNode("c:Cars/c:Hybrid");
        assertNode("c:Cars/c:Hybrid/Toyota Prius", "maker=Toyota", "model=Prius");
        assertNode("c:Cars/c:Hybrid/Toyota Highlander", "maker=Toyota", "model=Highlander");
        assertNode("c:Cars/c:Hybrid/Nissan Altima", "maker=Nissan", "model=Altima");
        assertNode("c:Cars/c:Sports");
        assertNode("c:Cars/c:Sports/Aston Martin DB9", "maker=Aston Martin", "model=DB9");
        assertNode("c:Cars/c:Sports/Infiniti G37", "maker=Infiniti", "model=G37");
    }

    @Test
    public void shouldParseXmlDocumentWithXmlComments() throws IOException, SAXException {
        context.getNamespaceRegistry().register("c", "http://default.namespace.com");
        parse("xmlHandler/docWithComments.xml");
        assertNode("c:Cars");
        assertNode("c:Cars/c:Hybrid");
        assertNode("c:Cars/c:Hybrid/Toyota Prius", "maker=Toyota", "model=Prius");
        assertNode("c:Cars/c:Hybrid/Toyota Highlander", "maker=Toyota", "model=Highlander");
        assertNode("c:Cars/c:Hybrid/Nissan Altima", "maker=Nissan", "model=Altima");
        assertNode("c:Cars/c:Sports");
        assertNode("c:Cars/c:Sports/Aston Martin DB9", "maker=Aston Martin", "model=DB9");
        assertNode("c:Cars/c:Sports/Infiniti G37", "maker=Infiniti", "model=G37");
    }

    protected void assertNode( String path,
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
        // Now get the next request and compare the expected and actual ...
        CreateNodeRequest request = requests.remove();
        assertThat(request.at().getPath(), is(expectedPath));
        for (Property actual : request.properties()) {
            Property expected = expectedProperties.remove(actual.getName());
            assertThat(expected, is(notNullValue()));
            assertThat(actual, is(expected));
        }
        assertThat(expectedProperties.isEmpty(), is(true));
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

    protected class RecordingDestination implements XmlHandler.Destination {
        private final LinkedList<CreateNodeRequest> requests = new LinkedList<CreateNodeRequest>();

        public void create( Path path,
                            List<Property> properties ) {
            requests.add(new CreateNodeRequest(new Location(path), properties));
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
