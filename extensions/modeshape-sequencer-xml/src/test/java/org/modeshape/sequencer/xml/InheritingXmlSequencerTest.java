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

public class InheritingXmlSequencerTest {

    private static final String DOCUMENT = "modexml:document";

    private XmlSequencer sequencer;
    private InputStream stream;
    private MockSequencerOutput output;
    private URL xsd;
    private StreamSequencerContext context;
    private String inputNodeName;

    @Before
    public void beforeEach() {
        inputNodeName = "input";
        sequencer = new InheritingXmlSequencer();
        context = new MockSequencerContext("/some/" + inputNodeName);
        output = new MockSequencerOutput(context);
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
    public void shouldSequenceXsds() throws IOException {
        assertThat(sequencer.getAttributeScoping(), is(XmlSequencer.AttributeScoping.INHERIT_ELEMENT_NAMESPACE));
        verifyDocument(xsd);
        verifyName("xs:schema", "jcr:primaryType", "modexml:element");
        verifyString("xs:schema", "xs:targetNamespace", "http://ns.adobe.com/air/application/1.0");
        verifyString("xs:schema", "xs:elementFormDefault", "qualified");
        verifyName("xs:schema/xs:element", "jcr:primaryType", "modexml:element");
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
