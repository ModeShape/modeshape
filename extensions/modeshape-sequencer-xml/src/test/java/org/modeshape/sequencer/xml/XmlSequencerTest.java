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
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.sequencer.MockSequencerContext;
import org.modeshape.graph.sequencer.MockSequencerOutput;
import org.modeshape.graph.sequencer.StreamSequencerContext;

/**
 * @author John Verhaeg
 */
public class XmlSequencerTest {

    private static final String CDATA = "modexml:cData";
    private static final String CDATA_CONTENT = "modexml:cDataContent";
    private static final String DOCUMENT = "modexml:document";
    private static final String DTD_NAME = "modedtd:name";
    private static final String DTD_SYSTEM_ID = "modedtd:systemId";
    private static final String DTD_VALUE = "modedtd:value";
    private static final String ELEMENT = "modexml:element";
    private static final String ELEMENT_CONTENT = "modexml:elementContent";
    private static final String ENTITY = "modedtd:entity";
    private static final String PI = "modexml:processingInstruction";
    private static final String PI_CONTENT = "modexml:processingInstructionContent";
    private static final String TARGET = "modexml:target";

    private XmlSequencer sequencer;
    private InputStream stream;
    private MockSequencerOutput output;
    private URL xml2;
    private URL xml3;
    private URL xml4;
    private URL xsd;
    private StreamSequencerContext context;
    private String inputNodeName;

    @Before
    public void beforeEach() {
        inputNodeName = "node";
        sequencer = new XmlSequencer();
        context = new MockSequencerContext("/some/" + inputNodeName);
        output = new MockSequencerOutput(context);
        xml2 = this.getClass().getClassLoader().getResource("master.xml");
        assertThat(xml2, is(notNullValue()));
        xml3 = this.getClass().getClassLoader().getResource("CurrencyFormatterExample.mxml");
        assertThat(xml3, is(notNullValue()));
        xml4 = this.getClass().getClassLoader().getResource("plugin.xml");
        assertThat(xml4, is(notNullValue()));
        xsd = this.getClass().getClassLoader().getResource("Descriptor.1.0.xsd");
        assertThat(xsd, is(notNullValue()));
    }

    @After
    public void afterEach() throws Exception {
        if (stream != null) {
            try {
                stream.close();
            } finally {
                stream = null;
            }
        }
    }

    @Test
    public void shouldHandleNamespaces() throws IOException {
        verifyDocument(xml2);
        verifyName("book[1]/bookinfo[1]/xi:include[1]", "jcr:primaryType", ELEMENT);
        verifyString("book[1]/bookinfo[1]/xi:include[1]", "href", "Author_Group.xml");
        verifyName("book[1]/bookinfo[1]/xi:include[2]", "jcr:primaryType", ELEMENT);
        verifyString("book[1]/bookinfo[1]/xi:include[2]", "href", "Legal_Notice.xml");
    }

    @Test
    public void shouldSequenceEntityDeclarations() throws IOException {
        verifyDocument(xml2);
        verifyName(ENTITY + "[1]", "jcr:primaryType", ENTITY);
        verifyString(ENTITY + "[1]", DTD_NAME, "%RH-ENTITIES");
        verifyString(ENTITY + "[1]", DTD_SYSTEM_ID, "Common_Config/rh-entities.ent");
        verifyName(ENTITY + "[2]", "jcr:primaryType", ENTITY);
        verifyString(ENTITY + "[2]", DTD_NAME, "versionNumber");
        verifyString(ENTITY + "[2]", DTD_VALUE, "0.1");
        verifyName(ENTITY + "[3]", "jcr:primaryType", ENTITY);
        verifyString(ENTITY + "[3]", DTD_NAME, "copyrightYear");
        verifyString(ENTITY + "[3]", DTD_VALUE, "2008");
    }

    @Test
    public void shouldSequenceElementContent() throws IOException {
        verifyDocument(xml2);
        verifyString("book[1]/chapter[1]/para[8]/" + ELEMENT_CONTENT + "[1]",
                     ELEMENT_CONTENT,
                     "ModeShape is building other features as well. One goal of ModeShape is to create federated repositories that "
                     + "dynamically merge the information from multiple databases, services, applications, and other JCR repositories. Another is to "
                     + "create customized views based upon the type of data and the role of the user that is accessing the data. And yet another is "
                     + "to create a REST-ful API to allow the JCR content to be accessed easily by other applications written in other languages.");
    }

    @Test
    public void shouldSequenceCData() throws IOException {
        verifyDocument(xml3);
        verifyString("mx:Application[1]/mx:Script[1]/" + CDATA + "[1]",
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
    public void shouldSequenceProcessingInstructions() throws IOException {
        verifyDocument(xml4);
        verifyName(PI + "[1]", "jcr:primaryType", PI);
        verifyString(PI + "[1]", TARGET, "eclipse");
        verifyString(PI + "[1]", PI_CONTENT, "version=\"3.0\"");
    }

    @Test
    public void shouldSequenceXsds() throws IOException {
        sequencer.setAttributeScoping(XmlSequencer.AttributeScoping.INHERIT_ELEMENT_NAMESPACE);
        verifyDocument(xsd);
        verifyName("xs:schema", "jcr:primaryType", ELEMENT);
        verifyString("xs:schema", "xs:targetNamespace", "http://ns.adobe.com/air/application/1.0");
        verifyString("xs:schema", "xs:elementFormDefault", "qualified");
        verifyName("xs:schema/xs:element", "jcr:primaryType", ELEMENT);
        verifyString("xs:schema/xs:element", "xs:name", "application");
    }

    private <T> T verify( String nodePath,
                          String property,
                          Class<T> expectedClass ) {
        nodePath = nodePath.length() == 0 ? inputNodeName : inputNodeName + "/" + nodePath;
        Object[] values = output.getPropertyValues(nodePath.length() == 0 ? "" : nodePath, property);
        assertThat(values, notNullValue());
        assertThat(values.length, is(1));
        Object value = values[0];
        assertThat(value, instanceOf(expectedClass));
        return expectedClass.cast(value);
    }

    private void verifyDocument( URL url ) throws IOException {
        stream = url.openStream();
        assertThat(stream, is(notNullValue()));
        sequencer.sequence(stream, output, context);
        verifyName("", "jcr:primaryType", DOCUMENT);
    }

    private void verifyName( String nodePath,
                             String property,
                             String expectedName ) {
        Name name = verify(nodePath, property, Name.class);
        assertThat(name, is(context.getValueFactories().getNameFactory().create(expectedName)));
    }

    private void verifyString( String nodePath,
                               String property,
                               String expectedString ) {
        String string = verify(nodePath, property, String.class);
        assertThat(string, is(expectedString));
    }
}
