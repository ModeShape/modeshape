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
package org.jboss.dna.repository.sequencers.xml;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.jboss.dna.common.monitor.ProgressMonitor;
import org.jboss.dna.common.monitor.SimpleProgressMonitor;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.NameFactory;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.sequencers.MockSequencerOutput;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author John Verhaeg
 */
public class XmlSequencerTest {

    private XmlSequencer sequencer;
    private InputStream stream;
    private MockSequencerOutput output;
    private ProgressMonitor monitor;
    private URL xml1;
    private URL xml2;
    private URL xml3;
    private URL xml4;
    private URL xsd;

    @Before
    public void beforeEach() {
        sequencer = new XmlSequencer();
        output = new MockSequencerOutput() {

            @Override
            public void setProperty( Path nodePath,
                                     Name propertyName,
                                     Object... values ) {
                super.setProperty(nodePath, propertyName, values);
                // System.out.println(nodePath + "." + propertyName + " = " + Arrays.asList(values));
            }
        };
        monitor = new SimpleProgressMonitor("Test activity");
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
        verifyName(XmlSequencer.COMMENT + "[1]", NameFactory.JCR_PRIMARY_TYPE, XmlSequencer.COMMENT);
        String text = verify(XmlSequencer.COMMENT + "[1]", XmlSequencer.COMMENT_CONTENT, String.class);
        assertThat(text.startsWith("\n   Licensed to the Apache Software Foundation (ASF)"), is(true));
        assertThat(text.endsWith("   limitations under the License.\n"), is(true));
        verifyString("", XmlSequencer.DTD_NAME, "Repository");
        verifyString("", XmlSequencer.DTD_PUBLIC_ID, "-//The Apache Software Foundation//DTD Jackrabbit 1.2//EN");
        verifyString("", XmlSequencer.DTD_SYSTEM_ID, "http://jackrabbit.apache.org/dtd/repository-1.2.dtd");
        verifyName(XmlSequencer.COMMENT + "[2]", NameFactory.JCR_PRIMARY_TYPE, XmlSequencer.COMMENT);
        verifyString(XmlSequencer.COMMENT + "[2]", XmlSequencer.COMMENT_CONTENT, " Example Repository Configuration File ");
        verifyName("Repository[1]", NameFactory.JCR_PRIMARY_TYPE, "Repository");
        verifyName("Repository[1]/" + XmlSequencer.COMMENT + "[1]", NameFactory.JCR_PRIMARY_TYPE, XmlSequencer.COMMENT);
    }

    @Test
    public void shouldHandleNamespaces() throws IOException {
        verifyDocument(xml2);
        verifyName("book[1]/bookinfo[1]/xi:include[1]", NameFactory.JCR_PRIMARY_TYPE, "xi:include");
        verifyString("book[1]/bookinfo[1]/xi:include[1]", "xi:href", "Author_Group.xml");
        verifyName("book[1]/bookinfo[1]/xi:include[2]", NameFactory.JCR_PRIMARY_TYPE, "xi:include");
        verifyString("book[1]/bookinfo[1]/xi:include[2]", "xi:href", "Legal_Notice.xml");
    }

    @Test
    public void shouldSequenceEntityDeclarations() throws IOException {
        verifyDocument(xml2);
        verifyName(XmlSequencer.ENTITY + "[1]", NameFactory.JCR_PRIMARY_TYPE, XmlSequencer.ENTITY);
        verifyString(XmlSequencer.ENTITY + "[1]", XmlSequencer.DTD_NAME, "%RH-ENTITIES");
        verifyString(XmlSequencer.ENTITY + "[1]", XmlSequencer.DTD_SYSTEM_ID, "Common_Config/rh-entities.ent");
        verifyName(XmlSequencer.ENTITY + "[2]", NameFactory.JCR_PRIMARY_TYPE, XmlSequencer.ENTITY);
        verifyString(XmlSequencer.ENTITY + "[2]", XmlSequencer.DTD_NAME, "versionNumber");
        verifyString(XmlSequencer.ENTITY + "[2]", XmlSequencer.DTD_VALUE, "0.1");
        verifyName(XmlSequencer.ENTITY + "[3]", NameFactory.JCR_PRIMARY_TYPE, XmlSequencer.ENTITY);
        verifyString(XmlSequencer.ENTITY + "[3]", XmlSequencer.DTD_NAME, "copyrightYear");
        verifyString(XmlSequencer.ENTITY + "[3]", XmlSequencer.DTD_VALUE, "2008");
    }

    @Test
    public void shouldSequenceElementContent() throws IOException {
        verifyDocument(xml2);
        verifyString("book[1]/chapter[4]/sect1[1]/para[8]/" + XmlSequencer.ELEMENT_CONTENT + "[1]",
                     XmlSequencer.ELEMENT_CONTENT,
                     "The path expression is more complicated."
                     + " Sequencer path expressions are used by the sequencing service to determine whether a particular changed node should be sequenced."
                     + " The expressions consist of two parts: a selection criteria and an output expression."
                     + " Here's a simple example:");
        verifyString("book[1]/chapter[4]/sect1[1]/para[8]/programlisting[1]/" + XmlSequencer.ELEMENT_CONTENT + "[1]",
                     XmlSequencer.ELEMENT_CONTENT,
                     "/a/b/c@title =&gt; /d/e/f");
    }

    @Test
    public void shouldSequenceCData() throws IOException {
        verifyDocument(xml3);
        verifyString("mx:Application[1]/mx:Script[1]/" + XmlSequencer.CDATA + "[1]",
                     XmlSequencer.CDATA_CONTENT,
                     "\n\n" + "              import mx.events.ValidationResultEvent;\t\t\t\n"
                     + "              private var vResult:ValidationResultEvent;\n" + "\t\t\t\n"
                     + "              // Event handler to validate and format input.\n"
                     + "              private function Format():void {\n" + "              \n"
                     + "                 	vResult = numVal.validate();\n\n"
                     + "    				if (vResult.type==ValidationResultEvent.VALID) {\n"
                     + "                        var temp:Number=Number(priceUS.text); \n"
                     + "                        formattedUSPrice.text= usdFormatter.format(temp);\n" + "                    }\n"
                     + "                    \n" + "                    else {\n"
                     + "                       formattedUSPrice.text=\"\";\n" + "                    }\n" + "              }\n"
                     + "        ");
    }

    @Test
    public void shouldSequenceProcessingInstructions() throws IOException {
        verifyDocument(xml4);
        verifyName(XmlSequencer.PI + "[1]", NameFactory.JCR_PRIMARY_TYPE, XmlSequencer.PI);
        verifyString(XmlSequencer.PI + "[1]", XmlSequencer.TARGET, "eclipse");
        verifyString(XmlSequencer.PI + "[1]", XmlSequencer.PI_CONTENT, "version=\"3.0\"");
    }

    @Test
    public void shouldSequenceXsds() throws IOException {
        verifyDocument(xsd);
        verifyName("xs:schema[1]", NameFactory.JCR_PRIMARY_TYPE, "xs:schema");
        verifyString("xs:schema[1]", "xs:targetNamespace", "http://ns.adobe.com/air/application/1.0");
        verifyString("xs:schema[1]", "xs:elementFormDefault", "qualified");
        verifyName("xs:schema[1]/xs:element[1]", NameFactory.JCR_PRIMARY_TYPE, "xs:element");
        verifyString("xs:schema[1]/xs:element[1]", "xs:name", "application");
    }

    private <T> T verify( String nodePath,
                          String property,
                          Class<T> expectedClass ) {
        Object[] values = output.getPropertyValues(nodePath.length() == 0 ? "." : "./" + nodePath, property);
        assertThat(values, notNullValue());
        assertThat(values.length, is(1));
        Object value = values[0];
        assertThat(value, instanceOf(expectedClass));
        return expectedClass.cast(value);
    }

    private void verifyDocument( URL url ) throws IOException {
        stream = url.openStream();
        assertThat(stream, is(notNullValue()));
        sequencer.sequence(stream, output, monitor);
        verifyName("", NameFactory.JCR_PRIMARY_TYPE, XmlSequencer.DOCUMENT);
    }

    private void verifyName( String nodePath,
                             String property,
                             String expectedName ) {
        Name name = verify(nodePath, property, Name.class);
        assertThat(name, is(output.getFactories().getNameFactory().create(expectedName)));
    }

    private void verifyString( String nodePath,
                               String property,
                               String expectedString ) {
        String string = verify(nodePath, property, String.class);
        assertThat(string, is(expectedString));
    }
}
