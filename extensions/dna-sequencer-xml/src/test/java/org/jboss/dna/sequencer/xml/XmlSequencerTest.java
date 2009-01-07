/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.dna.sequencer.xml;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.jboss.dna.graph.properties.Name;
import org.jboss.dna.graph.sequencers.MockSequencerContext;
import org.jboss.dna.graph.sequencers.MockSequencerOutput;
import org.jboss.dna.graph.sequencers.SequencerContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author John Verhaeg
 */
public class XmlSequencerTest {

    private static final String CDATA = "dnaxml:cData";
    private static final String CDATA_CONTENT = "dnaxml:cDataContent";
    private static final String COMMENT = "dnaxml:comment";
    private static final String COMMENT_CONTENT = "dnaxml:commentContent";
    private static final String DOCUMENT = "dnaxml:document";
    private static final String DTD_NAME = "dnadtd:name";
    private static final String DTD_PUBLIC_ID = "dnadtd:publicId";
    private static final String DTD_SYSTEM_ID = "dnadtd:systemId";
    private static final String DTD_VALUE = "dnadtd:value";
    private static final String ELEMENT_CONTENT = "dnaxml:elementContent";
    private static final String ENTITY = "dnadtd:entity";
    private static final String PI = "dnaxml:processingInstruction";
    private static final String PI_CONTENT = "dnaxml:processingInstructionContent";
    private static final String TARGET = "dnaxml:target";

    private XmlSequencer sequencer;
    private InputStream stream;
    private MockSequencerOutput output;
    private URL xml1;
    private URL xml2;
    private URL xml3;
    private URL xml4;
    private URL xsd;
    private SequencerContext context;

    @Before
    public void beforeEach() {
        sequencer = new XmlSequencer();
        context = new MockSequencerContext();
        output = new MockSequencerOutput(context);
        xml1 = this.getClass().getClassLoader().getResource("jackrabbitInMemoryTestRepositoryConfig.xml");
        assertThat(xml1, is(notNullValue()));
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
    public void shouldSequenceXml() throws IOException {
        verifyDocument(xml1);
        verifyName(COMMENT + "[1]", "jcr:primaryType", COMMENT);
        String text = verify(COMMENT + "[1]", COMMENT_CONTENT, String.class);
        assertThat(text.startsWith("Licensed to the Apache Software Foundation (ASF)"), is(true));
        assertThat(text.indexOf('\n') > 0, is(true));
        assertThat(text.endsWith("   limitations under the License."), is(true));
        verifyString("", DTD_NAME, "Repository");
        verifyString("", DTD_PUBLIC_ID, "-//The Apache Software Foundation//DTD Jackrabbit 1.2//EN");
        verifyString("", DTD_SYSTEM_ID, "http://jackrabbit.apache.org/dtd/repository-1.2.dtd");
        verifyName(COMMENT + "[2]", "jcr:primaryType", COMMENT);
        verifyString(COMMENT + "[2]", COMMENT_CONTENT, "Example Repository Configuration File");
        verifyName("Repository[1]", "jcr:primaryType", "nt:unstructured");
        verifyName("Repository[1]/" + COMMENT + "[1]", "jcr:primaryType", COMMENT);
    }

    @Test
    public void shouldHandleNamespaces() throws IOException {
        verifyDocument(xml2);
        verifyName("book[1]/bookinfo[1]/xi:include[1]", "jcr:primaryType", "nt:unstructured");
        verifyString("book[1]/bookinfo[1]/xi:include[1]", "href", "Author_Group.xml");
        verifyName("book[1]/bookinfo[1]/xi:include[2]", "jcr:primaryType", "nt:unstructured");
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
        verifyString("book[1]/chapter[4]/sect1[1]/para[8]/" + ELEMENT_CONTENT + "[1]",
                     ELEMENT_CONTENT,
                     "The path expression is more complicated."
                     + " Sequencer path expressions are used by the sequencing service to determine whether a particular changed node should be sequenced."
                     + " The expressions consist of two parts: a selection criteria and an output expression."
                     + " Here's a simple example:");
        verifyString("book[1]/chapter[4]/sect1[1]/para[8]/programlisting[1]/" + ELEMENT_CONTENT + "[1]",
                     ELEMENT_CONTENT,
                     "/a/b/c@title =&gt; /d/e/f");
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
        verifyName("xs:schema", "jcr:primaryType", "nt:unstructured");
        verifyString("xs:schema", "xs:targetNamespace", "http://ns.adobe.com/air/application/1.0");
        verifyString("xs:schema", "xs:elementFormDefault", "qualified");
        verifyName("xs:schema/xs:element", "jcr:primaryType", "nt:unstructured");
        verifyString("xs:schema/xs:element", "xs:name", "application");
    }

    private <T> T verify( String nodePath,
                          String property,
                          Class<T> expectedClass ) {
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
