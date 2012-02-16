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
 * Lesser General Public License for more details
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org
 */
package org.modeshape.sequencer.xml;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import static org.modeshape.jcr.api.JcrConstants.JCR_PRIMARY_TYPE;
import static org.modeshape.sequencer.xml.DtdLexicon.ENTITY;
import static org.modeshape.sequencer.xml.DtdLexicon.NAME;
import static org.modeshape.sequencer.xml.DtdLexicon.SYSTEM_ID;
import static org.modeshape.sequencer.xml.DtdLexicon.VALUE;
import static org.modeshape.sequencer.xml.XmlLexicon.CDATA;
import static org.modeshape.sequencer.xml.XmlLexicon.CDATA_CONTENT;
import static org.modeshape.sequencer.xml.XmlLexicon.ELEMENT;
import static org.modeshape.sequencer.xml.XmlLexicon.ELEMENT_CONTENT;
import static org.modeshape.sequencer.xml.XmlLexicon.PROCESSING_INSTRUCTION;
import static org.modeshape.sequencer.xml.XmlLexicon.PROCESSING_INSTRUCTION_CONTENT;
import static org.modeshape.sequencer.xml.XmlLexicon.TARGET;


/**
 * Unit test for {@link XmlSequencer}, using {@link XmlSequencer.AttributeScoping#USE_DEFAULT_NAMESPACE default namespace}
 *
 * @author John Verhaeg
 * @author Horia Chiorean
 */
public class XmlSequencerTest extends AbstractXmlSequencerTest {

    @Test
    public void shouldHandleNamespaces() throws Exception {
        Node document = sequenceAndAssertDocument("master.xml");
        assertProperty(document, "book[1]/bookinfo[1]/xi:include[1]", JCR_PRIMARY_TYPE, ELEMENT);
        assertProperty(document, "book[1]/bookinfo[1]/xi:include[1]", "href", "Author_Group.xml");
        assertProperty(document, "book[1]/bookinfo[1]/xi:include[2]", JCR_PRIMARY_TYPE, ELEMENT);
        assertProperty(document, "book[1]/bookinfo[1]/xi:include[2]", "href", "Legal_Notice.xml");
    }

    @Test
    public void shouldSequenceEntityDeclarations() throws Exception {
        Node document = sequenceAndAssertDocument("master.xml");
        assertProperty(document, ENTITY + "[1]", JCR_PRIMARY_TYPE, ENTITY);
        assertProperty(document, ENTITY + "[1]", NAME, "%RH-ENTITIES");
        assertProperty(document, ENTITY + "[1]", SYSTEM_ID, "Common_Config/rh-entities.ent");
        assertProperty(document, ENTITY + "[2]", "jcr:primaryType", ENTITY);
        assertProperty(document, ENTITY + "[2]", NAME, "versionNumber");
        assertProperty(document, ENTITY + "[2]", VALUE, "0.1");
        assertProperty(document, ENTITY + "[3]", "jcr:primaryType", ENTITY);
        assertProperty(document, ENTITY + "[3]", NAME, "copyrightYear");
        assertProperty(document, ENTITY + "[3]", VALUE, "2008");
    }

    @Test
    public void shouldSequenceElementContent() throws Exception {
        Node document = sequenceAndAssertDocument("master.xml");
        assertProperty(document, "book[1]/chapter[1]/para[8]/" + ELEMENT_CONTENT + "[1]",
                       ELEMENT_CONTENT,
                       "ModeShape is building other features as well. One goal of ModeShape is to create federated repositories that "
                               + "dynamically merge the information from multiple databases, services, applications, and other JCR repositories. Another is to "
                               + "create customized views based upon the type of data and the role of the user that is accessing the data. And yet another is "
                               + "to create a REST-ful API to allow the JCR content to be accessed easily by other applications written in other languages.");
    }


    @Test
    public void shouldSequenceCData() throws Exception {
        Node document = sequenceAndAssertDocument("CurrencyFormatterExample.mxml");
        assertProperty(document, "mx:Application[1]/mx:Script[1]/" + CDATA + "[1]",
                       CDATA_CONTENT,
                       "\n\n" + "              import mx.events.ValidationResultEvent;\t\t\t\n"
                               + "              private var vResult:ValidationResultEvent;\n" + "\t\t\t\n"
                               + "              // Event handler to validate and format input.\n"
                               + "              private function Format():void {\n" + "              \n"
                               + "                    vResult = numVal.validate();\n\n"
                               + "                    if (vResult.type==ValidationResultEvent.VALID) {\n"
                               + "                        var temp:Number=Number(priceUS.text); \n"
                               + "                        formattedUSPrice.text= usdFormatter.format(temp);\n" + "                    }\n"
                               + "                    \n" + "                    else {\n"
                               + "                       formattedUSPrice.text=\"\";\n" + "                    }\n" + "              }\n"
                               + "        ");
    }

    @Test
    public void shouldSequenceProcessingInstructions() throws Exception {
        Node document = sequenceAndAssertDocument("plugin.xml");
        assertProperty(document, PROCESSING_INSTRUCTION + "[1]", JCR_PRIMARY_TYPE, PROCESSING_INSTRUCTION);
        assertProperty(document, PROCESSING_INSTRUCTION + "[1]", TARGET, "eclipse");
        assertProperty(document, PROCESSING_INSTRUCTION + "[1]", PROCESSING_INSTRUCTION_CONTENT, "version=\"3.0\"");
    }

    @Test
    public void shouldSequenceXmlDocumentWithoutNamespaces() throws Exception {
        Node document = sequenceAndAssertDocument("docWithoutNamespaces.xml");
        // Check the generated content; note that the attribute name doesn't match, so the nodes don't get special names
        assertElement(document, "Cars");
        assertElement(document, "Cars/Hybrid");
        assertElement(document, "Cars/Hybrid/car[1]", "name=Toyota Prius", "maker=Toyota", "model=Prius");
        assertElement(document, "Cars/Hybrid/car[2]", "name=Toyota Highlander", "maker=Toyota", "model=Highlander");
        assertElement(document, "Cars/Hybrid/car[3]", "name=Nissan Altima", "maker=Nissan", "model=Altima");
        assertElement(document, "Cars/Sports");
        assertElement(document, "Cars/Sports/car[1]", "name=Aston Martin DB9", "maker=Aston Martin", "model=DB9");
        assertElement(document, "Cars/Sports/car[2]", "name=Infiniti G37", "maker=Infiniti", "model=G37");
    }

    @Test
    public void shouldSequenceXmlDocumentWithNamespaces() throws Exception {
        registerDefaultNamespace();
        Node document = sequenceAndAssertDocument("docWithNamespaces.xml");
        assertElement(document, "c:Cars");
        assertElement(document, "c:Cars/c:Hybrid");
        assertElement(document, "c:Cars/c:Hybrid/c:car[1]", "maker=Toyota", "model=Prius");
        assertElement(document, "c:Cars/c:Hybrid/c:car[2]", "maker=Toyota", "model=Highlander");
        assertElement(document, "c:Cars/c:Hybrid/c:car[3]", "maker=Nissan", "model=Altima");
        assertElement(document, "c:Cars/c:Sports");
        assertElement(document, "c:Cars/c:Sports/c:car[1]", "maker=Aston Martin", "model=DB9");
        assertElement(document, "c:Cars/c:Sports/c:car[2]", "maker=Infiniti", "model=G37");
    }

    @Test
    public void shouldSequenceXmlDocumentWithNestedNamespaceDeclarations() throws Exception {
        registerDefaultNamespace();
        Node document = sequenceAndAssertDocument("docWithNestedNamespaces.xml");
        assertElement(document, "Cars");
        assertElement(document, "Cars/c:Hybrid");
        assertElement(document, "Cars/c:Hybrid/c:car[1]", "maker=Toyota", "model=Prius");
        assertElement(document, "Cars/c:Hybrid/c:car[2]", "maker=Toyota", "model=Highlander");
        assertElement(document, "Cars/c:Hybrid/c:car[3]", "maker=Nissan", "model=Altima");
        assertElement(document, "Cars/Sports");
        assertElement(document, "Cars/Sports/car[1]", "info:maker=Aston Martin", "model=DB9");
        assertElement(document, "Cars/Sports/car[2]", "info:maker=Infiniti", "model=G37");
    }

    @Test
    public void shouldSequenceXmlDocumentThatContainsNoContent() throws Exception {
        Node document = sequenceAndAssertDocument("docWithOnlyRootElement.xml");
        Node cars = assertElement(document, "Cars");
        assertEquals(0, cars.getNodes().getSize());
    }

    @Test
    public void shouldSequenceXmlDocumentWithXmlComments() throws Exception {
        registerDefaultNamespace();
        Node document = sequenceAndAssertDocument("docWithComments.xml");
        assertElement(document, "c:Cars");
        assertComment(document, "c:Cars", 1, "This is a comment");
        assertElement(document, "c:Cars/c:Hybrid");
        assertElement(document, "c:Cars/c:Hybrid/c:car[1]", "maker=Toyota", "model=Prius");
        assertElement(document, "c:Cars/c:Hybrid/c:car[2]", "maker=Toyota", "model=Highlander");
        assertElement(document, "c:Cars/c:Hybrid/c:car[3]", "maker=Nissan", "model=Altima");
        assertComment(document, "c:Cars", 2, "This is another comment");
        assertElement(document, "c:Cars/c:Sports");
        assertElement(document, "c:Cars/c:Sports/c:car[1]", "maker=Aston Martin", "model=DB9");
        assertElement(document, "c:Cars/c:Sports/c:car[2]", "maker=Infiniti", "model=G37");
    }

    @Test
    public void shouldSequenceXmlDocumentWithXmlElementsContainingOnlyChildElements() throws Exception {
        Node document = sequenceAndAssertDocument("docWithElementsContainingElements.xml");
        assertElement(document, "xhtml:html");
        assertElement(document, "xhtml:html/xhtml:head");
        assertElement(document, "xhtml:html/xhtml:head/xhtml:title");
        assertContent(document, "xhtml:html/xhtml:head/xhtml:title", 1, "Three Namespaces");
        assertElement(document, "xhtml:html/xhtml:body");
        assertElement(document, "xhtml:html/xhtml:body/xhtml:h1", "{}align=center");
        assertContent(document, "xhtml:html/xhtml:body/xhtml:h1", 1, "An Ellipse and a Rectangle");
        assertElement(document, "xhtml:html/xhtml:body/svg:svg", "{}width=12cm", "{}height=10cm");
        assertElement(document, "xhtml:html/xhtml:body/svg:svg/svg:ellipse", "{}rx=110", "{}ry=130");
        assertElement(document, "xhtml:html/xhtml:body/svg:svg/svg:rect", "{}x=4cm", "{}y=1cm", "{}width=3cm", "{}height=6cm");
        assertElement(document, "xhtml:html/xhtml:body/xhtml:p");
        assertContent(document, "xhtml:html/xhtml:body/xhtml:p", 1, "The equation for ellipses");
        assertElement(document, "xhtml:html/xhtml:body/mathml:math");
        assertElement(document, "xhtml:html/xhtml:body/mathml:math/mathml:apply");
        assertElement(document, "xhtml:html/xhtml:body/mathml:math/mathml:apply/mathml:eq");
        assertElement(document, "xhtml:html/xhtml:body/mathml:math/mathml:apply/mathml:cn");
        assertContent(document, "xhtml:html/xhtml:body/mathml:math/mathml:apply/mathml:cn", 1, "1");
        assertElement(document, "xhtml:html/xhtml:body/mathml:math/mathml:apply/mathml:apply");
        assertElement(document, "xhtml:html/xhtml:body/mathml:math/mathml:apply/mathml:apply/mathml:plus");
        assertElement(document, "xhtml:html/xhtml:body/mathml:math/mathml:apply/mathml:apply/mathml:apply");
        assertElement(document, "xhtml:html/xhtml:body/mathml:math/mathml:apply/mathml:apply/mathml:apply/mathml:divide");
        assertElement(document, "xhtml:html/xhtml:body/mathml:math/mathml:apply/mathml:apply/mathml:apply/mathml:apply");
        assertElement(document,
                      "xhtml:html/xhtml:body/mathml:math/mathml:apply/mathml:apply/mathml:apply/mathml:apply/mathml:power");
        assertElement(document,
                      "xhtml:html/xhtml:body/mathml:math/mathml:apply/mathml:apply/mathml:apply/mathml:apply/mathml:ci");
        assertContent(document,
                      "xhtml:html/xhtml:body/mathml:math/mathml:apply/mathml:apply/mathml:apply/mathml:apply/mathml:ci",
                      1, "x");
        assertElement(document,
                      "xhtml:html/xhtml:body/mathml:math/mathml:apply/mathml:apply/mathml:apply/mathml:apply/mathml:cn");
        assertContent(document,
                      "xhtml:html/xhtml:body/mathml:math/mathml:apply/mathml:apply/mathml:apply/mathml:apply/mathml:cn",
                      1, "2");
        assertElement(document, "xhtml:html/xhtml:body/mathml:math/mathml:apply/mathml:apply/mathml:apply/mathml:apply[2]");
        assertElement(document,
                      "xhtml:html/xhtml:body/mathml:math/mathml:apply/mathml:apply/mathml:apply/mathml:apply[2]/mathml:power");
        assertElement(document,
                      "xhtml:html/xhtml:body/mathml:math/mathml:apply/mathml:apply/mathml:apply/mathml:apply[2]/mathml:ci");
        assertContent(document,
                      "xhtml:html/xhtml:body/mathml:math/mathml:apply/mathml:apply/mathml:apply/mathml:apply[2]/mathml:ci",
                      1, "a");
        assertElement(document,
                      "xhtml:html/xhtml:body/mathml:math/mathml:apply/mathml:apply/mathml:apply/mathml:apply[2]/mathml:cn");
        assertContent(document,
                      "xhtml:html/xhtml:body/mathml:math/mathml:apply/mathml:apply/mathml:apply/mathml:apply[2]/mathml:cn",
                      1, "2");

        assertElement(document, "xhtml:html/xhtml:body/mathml:math/mathml:apply/mathml:apply/mathml:apply[2]");
        assertElement(document, "xhtml:html/xhtml:body/mathml:math/mathml:apply/mathml:apply/mathml:apply[2]/mathml:divide");
        assertElement(document, "xhtml:html/xhtml:body/mathml:math/mathml:apply/mathml:apply/mathml:apply[2]/mathml:apply");
        assertElement(document,
                      "xhtml:html/xhtml:body/mathml:math/mathml:apply/mathml:apply/mathml:apply[2]/mathml:apply/mathml:power");
        assertElement(document,
                      "xhtml:html/xhtml:body/mathml:math/mathml:apply/mathml:apply/mathml:apply[2]/mathml:apply/mathml:ci");
        assertContent(document,
                      "xhtml:html/xhtml:body/mathml:math/mathml:apply/mathml:apply/mathml:apply[2]/mathml:apply/mathml:ci",
                      1, "y");
        assertElement(document,
                      "xhtml:html/xhtml:body/mathml:math/mathml:apply/mathml:apply/mathml:apply[2]/mathml:apply/mathml:cn");
        assertContent(document,
                      "xhtml:html/xhtml:body/mathml:math/mathml:apply/mathml:apply/mathml:apply[2]/mathml:apply/mathml:cn",
                      1, "2");
        assertElement(document, "xhtml:html/xhtml:body/mathml:math/mathml:apply/mathml:apply/mathml:apply[2]/mathml:apply[2]");
        assertElement(document,
                      "xhtml:html/xhtml:body/mathml:math/mathml:apply/mathml:apply/mathml:apply[2]/mathml:apply[2]/mathml:power");
        assertElement(document,
                      "xhtml:html/xhtml:body/mathml:math/mathml:apply/mathml:apply/mathml:apply[2]/mathml:apply[2]/mathml:ci");
        assertContent(document,
                      "xhtml:html/xhtml:body/mathml:math/mathml:apply/mathml:apply/mathml:apply[2]/mathml:apply[2]/mathml:ci",
                      1, "b");
        assertElement(document,
                      "xhtml:html/xhtml:body/mathml:math/mathml:apply/mathml:apply/mathml:apply[2]/mathml:apply[2]/mathml:cn");
        assertContent(document,
                      "xhtml:html/xhtml:body/mathml:math/mathml:apply/mathml:apply/mathml:apply[2]/mathml:apply[2]/mathml:cn",
                      1, "2");
        assertElement(document, "xhtml:html/xhtml:body/xhtml:hr");
        assertElement(document, "xhtml:html/xhtml:body/xhtml:p[2]");
        assertContent(document, "xhtml:html/xhtml:body/xhtml:p[2]", 1, "Last Modified January 10, 2002");
    }

    @Test
    public void shouldParseXmlDocumentWithDtdEntities() throws Exception {
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

        Node document = sequenceAndAssertDocument("docWithDtdEntities.xml", "book", "-//OASIS//DTD DocBook XML V4.4//EN",
                                                  "http://www.oasis-open.org/docbook/xml/4.4/docbookx.dtd");
        assertComment(document, null, 1, "Document comment");
        assertEntity(document, 1, "%RH-ENTITIES", null, "Common_Config/rh-entities.ent");
        assertEntity(document, 2, "versionNumber", "0.1");
        assertEntity(document, 3, "copyrightYear", "2008");
        assertEntity(document, 4, "copyrightHolder", "Red Hat Middleware, LLC.");
        assertElement(document, "book");
        assertElement(document, "book/bookinfo");
        assertElement(document, "book/bookinfo/title");
        assertContent(document, "book/bookinfo/title", 1, "ModeShape");
        assertElement(document, "book/bookinfo/releaseinfo");
        assertContent(document, "book/bookinfo/releaseinfo", 1, "&versionNumber;");
        assertElement(document, "book/bookinfo/productnumber");
        assertContent(document, "book/bookinfo/productnumber", 1, "some text with &versionNumber;inside");
        assertElement(document, "book/bookinfo/abstract");
        assertContent(document, "book/bookinfo/abstract", 1, longContent);
        assertElement(document, "book/programlisting1");
        assertContent(document, "book/programlisting1", 1, "&lt;dependency&gt; &lt;/dependency&gt;");
        assertElement(document, "book/programlisting2");
        assertCData(document, "book/programlisting2", 1, "\n&lt;dependency&gt;\n&lt;/dependency&gt;\n");
        assertElement(document, "book/programlisting3");
        assertContent(document, "book/programlisting3", 1, "mixture of text and");
        assertCData(document, "book/programlisting3", 1, "\n&lt;dependency&gt;\n&lt;/dependency&gt;\n");
        assertContent(document, "book/programlisting3", 2, "and some text");
        assertComment(document, "book/programlisting3", 1, "comment in content");
        assertContent(document, "book/programlisting3", 3, "after.");
    }

    @Test
    public void shouldParseXmlDocumentWithProcessingInstructions() throws Exception {
        Node document = sequenceAndAssertDocument("docWithProcessingInstructions.xml");
        assertProcessingInstruction(document, 1, "target", "content");
        assertProcessingInstruction(document, 2, "target2", "other stuff in the processing instruction");
        assertElement(document, "Cars");
        assertComment(document, "Cars", 1, "This is a comment");
        assertElement(document, "Cars/Hybrid");
        assertElement(document, "Cars/Hybrid/car[1]");
        assertElement(document, "Cars/Sports");
    }

    @Test
    public void shouldParseXmlDocumentWithCDATA() throws Exception {
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
        Node document = sequenceAndAssertDocument("docWithCDATA.xml");
        assertComment(document, null, 1, "Simple example to demonstrate the CurrencyFormatter.");
        assertElement(document, "mx:Application");
        assertElement(document, "mx:Application/mx:Script");
        assertCdata(document, "mx:Application/mx:Script", cdata);
        // Now there's an element that contains a mixture of regular element content, CDATA content, and comments
        assertElement(document, "mx:Application/programlisting3");
        assertContent(document, "mx:Application/programlisting3", 1, "mixture of text and");
        assertCdata(document, "mx:Application/programlisting3",
                    "\n<dependency>entities like &gt; are not replaced in a CDATA\n</dependency>\n");
        assertContent(document, "mx:Application/programlisting3", 2, "and some text");
        assertComment(document, "mx:Application/programlisting3", 1, "comment in content");
        assertContent(document, "mx:Application/programlisting3", 3, "after.");
        // Now the final element
        assertElement(document, "mx:Application/mx:NumberValidator",
                      "id=numVal",
                      "source={priceUS}",
                      "property=text",
                      "allowNegative=true",
                      "domain=real");
    }

    @Test
    public void shouldParseXmlDocumentWithDtd() throws Exception {
        sequenceAndAssertDocument("master.xml", "book", "-//OASIS//DTD DocBook XML V4.4//EN",
                                  "http://www.oasis-open.org/docbook/xml/4.4/docbookx.dtd");
    }

    private void registerDefaultNamespace() throws RepositoryException {
        ((Session)session()).getWorkspace().getNamespaceRegistry().registerNamespace("c", "http://default.namespace.com");
    }



    private Node assertCData( Node document,
                              String parentRelativePath,
                              int index,
                              String content ) throws RepositoryException {
        return assertNode(document, pathAtIndex(parentRelativePath, XmlLexicon.CDATA, index),
                          XmlLexicon.CDATA, nameValuePair(XmlLexicon.CDATA_CONTENT, content));
    }

    private void assertComment( Node document,
                                String parentRelativePath,
                                int index,
                                String expectedValue ) throws RepositoryException {
        assertNode(document, pathAtIndex(parentRelativePath, XmlLexicon.COMMENT, index), XmlLexicon.COMMENT, nameValuePair(
                XmlLexicon.COMMENT_CONTENT, expectedValue));
    }

    private void assertContent( Node document,
                                String parentRelativePath,
                                int index,
                                String expectedValue ) throws RepositoryException {
        assertNode(document, pathAtIndex(parentRelativePath, XmlLexicon.ELEMENT_CONTENT, index),
                   XmlLexicon.ELEMENT_CONTENT, nameValuePair(XmlLexicon.ELEMENT_CONTENT, expectedValue));
    }


    private Node sequenceAndAssertDocument( String documentFilename,
                                            String name,
                                            String publicId,
                                            String systemId ) throws Exception {
        Node document = sequenceAndAssertDocument(documentFilename);
        assertEquals(name, document.getProperty(DtdLexicon.NAME).getString());
        assertEquals(publicId, document.getProperty(DtdLexicon.PUBLIC_ID).getString());
        assertEquals(systemId, document.getProperty(DtdLexicon.SYSTEM_ID).getString());
        return document;
    }

    private void assertEntity( Node rootNode,
                               int index,
                               String entityName,
                               String value ) throws RepositoryException {
        assertNode(rootNode, pathAtIndex(null, DtdLexicon.ENTITY, index), DtdLexicon.ENTITY, nameValuePair(DtdLexicon.NAME,
                                                                                                           entityName),
                   nameValuePair(
                           DtdLexicon.VALUE, value));
    }

    private void assertEntity( Node rootNode,
                               int index,
                               String entityName,
                               String publicId,
                               String systemId ) throws RepositoryException {
        if (publicId != null) {
            assertNode(rootNode, pathAtIndex(null, DtdLexicon.ENTITY, index), DtdLexicon.ENTITY, nameValuePair(DtdLexicon.NAME,
                                                                                                               entityName),
                       nameValuePair(DtdLexicon.PUBLIC_ID, publicId), nameValuePair(DtdLexicon.SYSTEM_ID, systemId));
        } else {
            assertNode(rootNode, pathAtIndex(null, DtdLexicon.ENTITY, index), DtdLexicon.ENTITY, nameValuePair(DtdLexicon.NAME,
                                                                                                               entityName),
                       nameValuePair(DtdLexicon.SYSTEM_ID, systemId));
        }
    }

    private void assertProcessingInstruction( Node rootNode,
                                              int index,
                                              String target,
                                              String data ) throws RepositoryException {
        assertNode(rootNode, pathAtIndex(null, XmlLexicon.PROCESSING_INSTRUCTION, index), XmlLexicon.PROCESSING_INSTRUCTION,
                   nameValuePair(XmlLexicon.TARGET, target),
                   nameValuePair(XmlLexicon.PROCESSING_INSTRUCTION_CONTENT, data));
    }


    private void assertCdata( Node rootNode,
                              String parentRelativePath,
                              String value ) throws RepositoryException {
        assertNode(rootNode, pathAtIndex(parentRelativePath, XmlLexicon.CDATA, 1), XmlLexicon.CDATA, nameValuePair(
                XmlLexicon.CDATA_CONTENT, value));
    }



    private void assertProperty( Node parentNode,
                                 String relativePath,
                                 String propertyName,
                                 String expectedValue ) throws RepositoryException {
        Node expectedNode = parentNode.getNode(relativePath);
        assertNotNull(expectedNode);
        assertEquals(expectedValue, expectedNode.getProperty(propertyName).getValue().getString());
    }

    private String nameValuePair( String name,
                                  String value ) {
        return name + "=" + value;
    }


    private String pathAtIndex( String parentPath,
                                String nodeName,
                                int index ) {
        String childPath = nodeName + "[" + index + "]";
        return parentPath != null ? parentPath + "/" + childPath : childPath;
    }
}
