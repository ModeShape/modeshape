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
package org.modeshape.sequencer.xml;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.statistic.Stopwatch;
import org.modeshape.common.text.Jsr283Encoder;
import org.modeshape.common.text.TextDecoder;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.JcrNtLexicon;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.sequencer.MockSequencerContext;
import org.modeshape.graph.sequencer.MockSequencerOutput;
import org.modeshape.graph.sequencer.StreamSequencerContext;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * @author Randall Hauch
 */
public class XmlSequencerHandlerTest {

    private XmlSequencerHandler handler;
    private StreamSequencerContext context;
    private MockSequencerOutput output;
    private TextDecoder decoder;
    private Name primaryType;
    private Name nameAttribute;
    private XmlSequencer.AttributeScoping scoping;
    private LinkedList<Path> pathsInCreationOrder;

    @Before
    public void beforeEach() {
        context = new MockSequencerContext();
        output = new MockSequencerOutput(context, true);
        context.getNamespaceRegistry().register(JcrLexicon.Namespace.PREFIX, JcrLexicon.Namespace.URI);
        context.getNamespaceRegistry().register(JcrNtLexicon.Namespace.PREFIX, JcrNtLexicon.Namespace.URI);
        context.getNamespaceRegistry().register(ModeShapeXmlLexicon.Namespace.PREFIX, ModeShapeXmlLexicon.Namespace.URI);
        context.getNamespaceRegistry().register(ModeShapeDtdLexicon.Namespace.PREFIX, ModeShapeDtdLexicon.Namespace.URI);
        decoder = null;
        nameAttribute = JcrLexicon.NAME;
        primaryType = JcrNtLexicon.UNSTRUCTURED;
        scoping = XmlSequencer.AttributeScoping.USE_DEFAULT_NAMESPACE;
        handler = new XmlSequencerHandler(output, context, nameAttribute, primaryType, decoder, scoping);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotConstructInstanceWhenGivenNullContext() {
        context = null;
        new XmlSequencerHandler(output, context, nameAttribute, primaryType, decoder, scoping);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotConstructInstanceWhenGivenNullOutput() {
        output = null;
        new XmlSequencerHandler(output, context, nameAttribute, primaryType, decoder, scoping);
    }

    @Test
    public void shouldUseDefaultDecoderIfNoneIsProvidedInConstructor() {
        decoder = null;
        handler = new XmlSequencerHandler(output, context, nameAttribute, primaryType, decoder, scoping);
    }

    @Test
    public void shouldUseDecoderProvidedInConstructor() {
        decoder = new Jsr283Encoder();
        new XmlSequencerHandler(output, context, nameAttribute, primaryType, decoder, scoping);
    }

    @Test
    public void shouldParseXmlDocumentWithoutNamespaces() throws IOException, SAXException {
        parse("docWithoutNamespaces.xml");
        // Check the generated content; note that the attribute name doesn't match, so the nodes don't get special names
        assertDocumentNode();
        assertNode("Cars");
        assertNode("Cars/Hybrid");
        assertNode("Cars/Hybrid/car[1]", "name=Toyota Prius", "maker=Toyota", "model=Prius");
        assertNode("Cars/Hybrid/car[2]", "name=Toyota Highlander", "maker=Toyota", "model=Highlander");
        assertNode("Cars/Hybrid/car[3]", "name=Nissan Altima", "maker=Nissan", "model=Altima");
        assertNode("Cars/Sports");
        assertNode("Cars/Sports/car[1]", "name=Aston Martin DB9", "maker=Aston Martin", "model=DB9");
        assertNode("Cars/Sports/car[2]", "name=Infiniti G37", "maker=Infiniti", "model=G37");
        assertNoMoreNodes();
    }

    @Test
    public void shouldParseXmlDocumentWithNamespaces() throws IOException, SAXException {
        context.getNamespaceRegistry().register("c", "http://default.namespace.com");
        parse("docWithNamespaces.xml");
        // Check the generated content.
        // Note that the attribute name DOES match, so the nodes names come from "jcr:name" attribute
        // Also, the "jcr:name" attribute values use the default namespace, which is "c" in the registry
        assertDocumentNode();
        assertNode("c:Cars");
        assertNode("c:Cars/c:Hybrid");
        assertNode("c:Cars/c:Hybrid/c:Toyota Prius", "c:maker=Toyota", "c:model=Prius");
        assertNode("c:Cars/c:Hybrid/c:Toyota Highlander", "c:maker=Toyota", "c:model=Highlander");
        assertNode("c:Cars/c:Hybrid/c:Nissan Altima", "c:maker=Nissan", "c:model=Altima");
        assertNode("c:Cars/c:Sports");
        assertNode("c:Cars/c:Sports/c:Aston Martin DB9", "c:maker=Aston Martin", "c:model=DB9");
        assertNode("c:Cars/c:Sports/c:Infiniti G37", "c:maker=Infiniti", "c:model=G37");
        assertNoMoreNodes();
    }

    @Test
    public void shouldParseXmlDocumentWithNestedNamespaceDeclarations() throws IOException, SAXException {
        context.getNamespaceRegistry().register("c", "http://default.namespace.com");
        context.getNamespaceRegistry().register("i", "http://attributes.com");
        parse("docWithNestedNamespaces.xml");
        // Check the generated content; note that the attribute name DOES match, so the nodes names come from "jcr:name" attribute
        assertDocumentNode();
        assertNode("Cars");
        assertNode("Cars/c:Hybrid");
        assertNode("Cars/c:Hybrid/c:Toyota Prius", "c:maker=Toyota", "c:model=Prius");
        assertNode("Cars/c:Hybrid/c:Toyota Highlander", "c:maker=Toyota", "c:model=Highlander");
        assertNode("Cars/c:Hybrid/c:Nissan Altima", "c:maker=Nissan", "c:model=Altima");
        assertNode("Cars/Sports");
        assertNode("Cars/Sports/Aston Martin DB9", "i:maker=Aston Martin", "model=DB9");
        assertNode("Cars/Sports/Infiniti G37", "i:maker=Infiniti", "model=G37");
        assertNoMoreNodes();
    }

    @Test
    public void shouldParseXmlDocumentWithNamespacePrefixesThatDoNotMatchRegistry() throws IOException, SAXException {
        context.getNamespaceRegistry().register("c", "http://default.namespace.com");
        parse("docWithNamespaces.xml");
        // Check the generated content; note that the attribute name DOES match, so the nodes names come from "jcr:name" attribute
        assertDocumentNode();
        assertNode("c:Cars");
        assertNode("c:Cars/c:Hybrid");
        assertNode("c:Cars/c:Hybrid/c:Toyota Prius", "c:maker=Toyota", "c:model=Prius");
        assertNode("c:Cars/c:Hybrid/c:Toyota Highlander", "c:maker=Toyota", "c:model=Highlander");
        assertNode("c:Cars/c:Hybrid/c:Nissan Altima", "c:maker=Nissan", "c:model=Altima");
        assertNode("c:Cars/c:Sports");
        assertNode("c:Cars/c:Sports/c:Aston Martin DB9", "c:maker=Aston Martin", "c:model=DB9");
        assertNode("c:Cars/c:Sports/c:Infiniti G37", "c:maker=Infiniti", "c:model=G37");
        assertNoMoreNodes();
    }

    @Test
    public void shouldParseXmlDocumentWithNamespacesThatAreNotYetInRegistry() throws IOException, SAXException {
        NamespaceRegistry reg = context.getNamespaceRegistry();
        reg.unregister(JcrLexicon.Namespace.URI);
        reg.unregister(JcrNtLexicon.Namespace.URI);
        // Verify the prefixes don't exist ...
        assertThat(reg.getPrefixForNamespaceUri(JcrLexicon.Namespace.URI, false), is(nullValue()));
        assertThat(reg.getPrefixForNamespaceUri(JcrNtLexicon.Namespace.URI, false), is(nullValue()));
        assertThat(reg.getPrefixForNamespaceUri("http://default.namespace.com", false), is(nullValue()));
        // Parse the XML file ...
        parse("docWithNestedNamespaces.xml");
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
        assertDocumentNode();
        assertNode(d + "Cars");
        assertNode(d + "Cars/" + c + "Hybrid");
        assertNode(d + "Cars/" + c + "Hybrid/" + c + "Toyota Prius", c + "maker=Toyota", c + "model=Prius");
        assertNode(d + "Cars/" + c + "Hybrid/" + c + "Toyota Highlander", c + "maker=Toyota", c + "model=Highlander");
        assertNode(d + "Cars/" + c + "Hybrid/" + c + "Nissan Altima", c + "maker=Nissan", c + "model=Altima");
        assertNode(d + "Cars/" + d + "Sports");
        assertNode(d + "Cars/" + d + "Sports/Aston Martin DB9", i + "maker=Aston Martin", "model=DB9");
        assertNode(d + "Cars/" + d + "Sports/Infiniti G37", i + "maker=Infiniti", "model=G37");
        assertNoMoreNodes();
    }

    @Test
    public void shouldParseXmlDocumentThatUsesNameAttribute() throws IOException, SAXException {
        context.getNamespaceRegistry().register("c", "http://default.namespace.com");
        parse("docWithNamespaces.xml");
        // Check the generated content; note that the attribute name DOES match, so the nodes names come from "jcr:name" attribute
        assertDocumentNode();
        assertNode("c:Cars");
        assertNode("c:Cars/c:Hybrid");
        assertNode("c:Cars/c:Hybrid/c:Toyota Prius", "c:maker=Toyota", "c:model=Prius");
        assertNode("c:Cars/c:Hybrid/c:Toyota Highlander", "c:maker=Toyota", "c:model=Highlander");
        assertNode("c:Cars/c:Hybrid/c:Nissan Altima", "c:maker=Nissan", "c:model=Altima");
        assertNode("c:Cars/c:Sports");
        assertNode("c:Cars/c:Sports/c:Aston Martin DB9", "c:maker=Aston Martin", "c:model=DB9");
        assertNode("c:Cars/c:Sports/c:Infiniti G37", "c:maker=Infiniti", "c:model=G37");
        assertNoMoreNodes();
    }

    @Test
    public void shouldParseXmlDocumentWithoutDefaultNamespaceThatUsesNameAttribute() throws IOException, SAXException {
        parse("docWithNamespacesWithoutDefault.xml");
        // Check the generated content; note that the attribute name DOES match, so the nodes names come from "jcr:name" attribute
        assertDocumentNode();
        assertNode("Cars");
        assertNode("Cars/Hybrid");
        assertNode("Cars/Hybrid/Toyota Prius", "maker=Toyota", "model=Prius");
        assertNode("Cars/Hybrid/Toyota Highlander", "maker=Toyota", "model=Highlander");
        assertNode("Cars/Hybrid/Nissan Altima", "maker=Nissan", "model=Altima");
        assertNode("Cars/Sports");
        assertNode("Cars/Sports/Aston Martin DB9", "maker=Aston Martin", "model=DB9");
        assertNode("Cars/Sports/Infiniti G37", "maker=Infiniti", "model=G37");
        assertNoMoreNodes();
    }

    @Test
    public void shouldParseXmlDocumentWithoutDefaultNamespaceThatUsesNoNameAttribute() throws IOException, SAXException {
        nameAttribute = null;
        handler = new XmlSequencerHandler(output, context, nameAttribute, primaryType, decoder, scoping);
        parse("docWithNamespacesWithoutDefault.xml");
        // Check the generated content; note that the attribute name DOES match, so the nodes names come from "jcr:name" attribute
        assertDocumentNode();
        assertNode("Cars");
        assertNode("Cars/Hybrid");
        assertNode("Cars/Hybrid/car[1]", "maker=Toyota", "model=Prius");
        assertNode("Cars/Hybrid/car[2]", "maker=Toyota", "model=Highlander");
        assertNode("Cars/Hybrid/car[3]", "maker=Nissan", "model=Altima");
        assertNode("Cars/Sports");
        assertNode("Cars/Sports/car[1]", "maker=Aston Martin", "model=DB9");
        assertNode("Cars/Sports/car[2]", "maker=Infiniti", "model=G37");
        assertNoMoreNodes();
    }

    @Test
    public void shouldParseXmlDocumentThatContainsNoContent() throws IOException, SAXException {
        parse("docWithOnlyRootElement.xml");
        assertNode("", "jcr:primaryType={http://www.modeshape.org/xml/1.0}document");
        assertNode("Cars");
        assertNoMoreNodes();
    }

    @Test
    public void shouldParseXmlDocumentWithXmlComments() throws IOException, SAXException {
        context.getNamespaceRegistry().register("c", "http://default.namespace.com");
        parse("docWithComments.xml");
        assertDocumentNode();
        assertNode("c:Cars");
        assertComment("c:Cars/modexml:comment[1]", "This is a comment");
        assertNode("c:Cars/c:Hybrid");
        assertNode("c:Cars/c:Hybrid/c:Toyota Prius", "c:maker=Toyota", "c:model=Prius");
        assertNode("c:Cars/c:Hybrid/c:Toyota Highlander", "c:maker=Toyota", "c:model=Highlander");
        assertNode("c:Cars/c:Hybrid/c:Nissan Altima", "c:maker=Nissan", "c:model=Altima");
        assertComment("c:Cars/modexml:comment[2]", "This is another comment");
        assertNode("c:Cars/c:Sports");
        assertNode("c:Cars/c:Sports/c:Aston Martin DB9", "c:maker=Aston Martin", "c:model=DB9");
        assertNode("c:Cars/c:Sports/c:Infiniti G37", "c:maker=Infiniti", "c:model=G37");
        assertNoMoreNodes();
    }

    @Test
    public void shouldParseXmlDocumentWithDtdEntities() throws IOException, SAXException {
        // Note the expected element content has leading and trailing whitespace removed, and any sequential
        // whitespace is replaced with a single space
        String longContent = "This is some long content that spans multiple lines and should span multiple "
                             + "calls to 'character(...)'. Repeating to make really long. This is some "
                             + "long content that spans multiple lines and should span multiple "
                             + "calls to 'character(...)'. Repeating to make really long. "
                             + "This is some long content that spans multiple lines and should span multiple "
                             + "calls to 'character(...)'. Repeating to make really long. This is some "
                             + "long content that spans multiple lines and should span multiple "
                             + "calls to 'character(...)'. Repeating to make really long.";

        parse("docWithDtdEntities.xml");
        assertDocumentNode("book", "-//OASIS//DTD DocBook XML V4.4//EN", "http://www.oasis-open.org/docbook/xml/4.4/docbookx.dtd");
        assertComment("modexml:comment", "Document comment");
        assertEntity(1, "%RH-ENTITIES", null, "Common_Config/rh-entities.ent");
        assertEntity(2, "versionNumber", "0.1");
        assertEntity(3, "copyrightYear", "2008");
        assertEntity(4, "copyrightHolder", "Red Hat Middleware, LLC.");
        assertNode("book");
        assertNode("book/bookinfo");
        assertNode("book/bookinfo/title");
        assertNode("book/bookinfo/title/modexml:elementContent",
                   "jcr:primaryType={http://www.modeshape.org/xml/1.0}elementContent",
                   "modexml:elementContent=ModeShape");
        assertNode("book/bookinfo/releaseinfo");
        assertNode("book/bookinfo/releaseinfo/modexml:elementContent",
                   "jcr:primaryType={http://www.modeshape.org/xml/1.0}elementContent",
                   "modexml:elementContent=&versionNumber;");
        assertNode("book/bookinfo/productnumber");
        assertNode("book/bookinfo/productnumber/modexml:elementContent",
                   "jcr:primaryType={http://www.modeshape.org/xml/1.0}elementContent",
                   "modexml:elementContent=some text with &versionNumber;inside");
        assertNode("book/bookinfo/abstract");
        assertNode("book/bookinfo/abstract/modexml:elementContent",
                   "jcr:primaryType={http://www.modeshape.org/xml/1.0}elementContent",
                   "modexml:elementContent=" + longContent);
        assertNode("book/programlisting1");
        assertNode("book/programlisting1/modexml:elementContent",
                   "jcr:primaryType={http://www.modeshape.org/xml/1.0}elementContent",
                   "modexml:elementContent=&lt;dependency&gt; &lt;/dependency&gt;");
        assertNode("book/programlisting2");
        assertNode("book/programlisting2/modexml:cData", "modexml:cDataContent=\n&lt;dependency&gt;\n&lt;/dependency&gt;\n");
        assertNode("book/programlisting3");
        assertNode("book/programlisting3/modexml:elementContent[1]",
                   "jcr:primaryType={http://www.modeshape.org/xml/1.0}elementContent",
                   "modexml:elementContent=mixture of text and");
        assertNode("book/programlisting3/modexml:cData", "modexml:cDataContent=\n&lt;dependency&gt;\n&lt;/dependency&gt;\n");
        assertNode("book/programlisting3/modexml:elementContent[2]",
                   "jcr:primaryType={http://www.modeshape.org/xml/1.0}elementContent",
                   "modexml:elementContent=and some text");
        assertComment("book/programlisting3/modexml:comment", "comment in content");
        assertNode("book/programlisting3/modexml:elementContent[3]",
                   "jcr:primaryType={http://www.modeshape.org/xml/1.0}elementContent",
                   "modexml:elementContent=after.");
        assertNoMoreNodes();
    }

    @Test
    public void shouldParseXmlDocumentWithProcessingInstructions() throws IOException, SAXException {
        parse("docWithProcessingInstructions.xml");
        assertDocumentNode();
        assertPI(1, "target", "content");
        assertPI(2, "target2", "other stuff in the processing instruction");
        assertNode("Cars");
        assertComment("Cars/modexml:comment", "This is a comment");
        assertNode("Cars/Hybrid");
        assertNode("Cars/Hybrid/Toyota Prius");
        assertNode("Cars/Sports");
        assertNoMoreNodes();
    }

    @Test
    public void shouldParseXmlDocumentWithCDATA() throws IOException, SAXException {
        String cdata = "\n" + "\n" + "              import mx.events.ValidationResultEvent;\t\t\t\n"
                       + "              private var vResult:ValidationResultEvent;\n" + "\t\t\t\n"
                       + "              // Event handler to validate and format input.\n"
                       + "              private function Format():void {\n" + "              \n"
                       + "                    vResult = numVal.validate();\n" + "\n"
                       + "                    if (vResult.type==ValidationResultEvent.VALID) {\n"
                       + "                        var temp:Number=Number(priceUS.text); \n"
                       + "                        formattedUSPrice.text= usdFormatter.format(temp);\n"
                       + "                    }\n" + "                    \n" + "                    else {\n"
                       + "                       formattedUSPrice.text=\"\";\n" + "                    }\n" + "              }\n"
                       + "        ";
        parse("docWithCDATA.xml");
        assertDocumentNode();
        assertComment("modexml:comment", "Simple example to demonstrate the CurrencyFormatter.");
        assertNode("mx:Application");
        assertNode("mx:Application/mx:Script");
        assertCdata("mx:Application/mx:Script/modexml:cData", cdata);
        // Now there's an element that contains a mixture of regular element content, CDATA content, and comments
        assertNode("mx:Application/programlisting3");
        assertNode("mx:Application/programlisting3/modexml:elementContent[1]",
                   "jcr:primaryType={http://www.modeshape.org/xml/1.0}elementContent",
                   "modexml:elementContent=mixture of text and");
        assertNode("mx:Application/programlisting3/modexml:cData",
                   "modexml:cDataContent=\n<dependency>entities like &gt; are not replaced in a CDATA\n</dependency>\n");
        assertNode("mx:Application/programlisting3/modexml:elementContent[2]",
                   "jcr:primaryType={http://www.modeshape.org/xml/1.0}elementContent",
                   "modexml:elementContent=and some text");
        assertComment("mx:Application/programlisting3/modexml:comment", "comment in content");
        assertNode("mx:Application/programlisting3/modexml:elementContent[3]",
                   "jcr:primaryType={http://www.modeshape.org/xml/1.0}elementContent",
                   "modexml:elementContent=after.");
        // Now the final element
        assertNode("mx:Application/mx:NumberValidator",
                   "id=numVal",
                   "source={priceUS}",
                   "property=text",
                   "allowNegative=true",
                   "domain=real");
        assertNoMoreNodes();
    }

    @Test
    public void shouldParseXmlDocumentWithDtd() throws IOException, SAXException {
        parse("master.xml");
        assertDocumentNode("book", "-//OASIS//DTD DocBook XML V4.4//EN", "http://www.oasis-open.org/docbook/xml/4.4/docbookx.dtd");
    }

    protected void assertNoMoreNodes() {
        if (!pathsInCreationOrder.isEmpty()) {
            fail("Extra nodes were not expected:" + pathsInCreationOrder);
        }
    }

    protected void assertNode( String path,
                               String... properties ) {
        // Append an index to the path if not there ...
        if (path.length() != 0 && !path.endsWith("]")) {
            path = path + "[1]";
        }

        // Create the expected path ...
        PathFactory factory = context.getValueFactories().getPathFactory();
        Path expectedPath = null;
        if (path.length() == 0) {
            expectedPath = factory.createRelativePath();
        } else {
            expectedPath = factory.create(path);
        }

        // Pop the next node and compare ...
        Path next = this.pathsInCreationOrder.removeFirst();
        assertThat(next, is(expectedPath));

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
        // If properties does not contain a primaryType property, then add the default ...
        if (!expectedProperties.containsKey(JcrLexicon.PRIMARY_TYPE)) {
            Property property = context.getPropertyFactory().create(JcrLexicon.PRIMARY_TYPE, primaryType);
            expectedProperties.put(property.getName(), property);
        }

        // Now get the Properties for this path ...
        Map<Name, Property> actualProperties = output.getProperties(expectedPath);
        assertThat("node not found", actualProperties, is(notNullValue()));
        for (Property actual : actualProperties.values()) {
            Property expected = expectedProperties.remove(actual.getName());
            assertThat("unexpected actual property: " + actual, expected, is(notNullValue()));
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
            assertThat(msg.toString(), expectedProperties.isEmpty(), is(true));
        }
    }

    protected void assertComment( String path,
                                  String comment ) {
        assertNode(path, "jcr:primaryType={http://www.modeshape.org/xml/1.0}comment", "modexml:commentContent=" + comment.trim());
    }

    protected void assertCdata( String path,
                                String content ) {
        // Append an index to the path if not there ...
        if (path.length() != 0 && !path.endsWith("]")) {
            path = path + "[1]";
        }
        PathFactory factory = context.getValueFactories().getPathFactory();
        Path expectedPath = factory.create("/" + path);

        // Pop the next node and compare ...
        Path next = this.pathsInCreationOrder.removeFirst();
        assertThat(next, is(expectedPath));

        // There should be a single property ...
        Property actualPrimaryType = output.getProperty(expectedPath, JcrLexicon.PRIMARY_TYPE);
        assertThat(actualPrimaryType.getValues().next(), is((Object)JcrNtLexicon.UNSTRUCTURED));
        Property actual = output.getProperty(expectedPath, ModeShapeXmlLexicon.CDATA_CONTENT);
        assertThat("expected one CDATA property", actual, is(notNullValue()));
        Property expected = context.getPropertyFactory().create(ModeShapeXmlLexicon.CDATA_CONTENT, content);
        assertThat("CDATA content differed", actual, is(expected));
    }

    protected void assertDocumentNode() {
        assertNode("", "jcr:primaryType={http://www.modeshape.org/xml/1.0}document");
    }

    protected void assertDocumentNode( String name,
                                       String publicId,
                                       String systemId ) {
        assertNode("",
                   "jcr:primaryType={http://www.modeshape.org/xml/1.0}document",
                   "modedtd:name=" + name,
                   "modedtd:publicId=" + publicId,
                   "modedtd:systemId=" + systemId);
    }

    protected void assertEntity( int index,
                                 String entityName,
                                 String value ) {
        String path = "modedtd:entity[" + index + "]";
        assertNode(path,
                   "jcr:primaryType={http://www.modeshape.org/dtd/1.0}entity",
                   "modedtd:name=" + entityName,
                   "modedtd:value=" + value);
    }

    protected void assertEntity( int index,
                                 String entityName,
                                 String publicId,
                                 String systemId ) {
        String path = "modedtd:entity[" + index + "]";
        if (publicId != null) {
            assertNode(path,
                       "jcr:primaryType={http://www.modeshape.org/dtd/1.0}entity",
                       "modedtd:name=" + entityName,
                       "modedtd:publicId=" + publicId,
                       "modedtd:systemId=" + systemId);
        } else {
            assertNode(path,
                       "jcr:primaryType={http://www.modeshape.org/dtd/1.0}entity",
                       "modedtd:name=" + entityName,
                       "modedtd:systemId=" + systemId);
        }
    }

    protected void assertPI( int index,
                             String target,
                             String data ) {
        assertNode("modexml:processingInstruction[" + index + "]",
                   "jcr:primaryType={http://www.modeshape.org/xml/1.0}processingInstruction",
                   "modexml:target=" + target,
                   "modexml:processingInstructionContent=" + data);
    }

    protected void parse( String relativePathToXmlFile ) throws IOException, SAXException {
        Stopwatch sw = new Stopwatch();
        sw.start();
        InputStream stream = getClass().getClassLoader().getResourceAsStream(relativePathToXmlFile);
        try {
            XMLReader reader = XMLReaderFactory.createXMLReader();
            reader.setContentHandler(handler);
            reader.setErrorHandler(handler);
            // Ensure handler acting as entity resolver 2
            reader.setProperty(XmlSequencer.DECL_HANDLER_FEATURE, handler);
            // Ensure handler acting as lexical handler
            reader.setProperty(XmlSequencer.LEXICAL_HANDLER_FEATURE, handler);
            // Ensure handler acting as entity resolver 2
            XmlSequencer.setFeature(reader, XmlSequencer.ENTITY_RESOLVER_2_FEATURE, true);
            // Prevent loading of external DTDs
            XmlSequencer.setFeature(reader, XmlSequencer.LOAD_EXTERNAL_DTDS_FEATURE, false);
            // Prevent the resolving of DTD entities into fully-qualified URIS
            XmlSequencer.setFeature(reader, XmlSequencer.RESOLVE_DTD_URIS_FEATURE, false);
            reader.parse(new InputSource(stream));
        } finally {
            if (stream != null) stream.close();
            sw.stop();
            System.out.println("Parsing: " + sw);
        }
        pathsInCreationOrder = new LinkedList<Path>(output.getOrderOfCreation());
    }
}
