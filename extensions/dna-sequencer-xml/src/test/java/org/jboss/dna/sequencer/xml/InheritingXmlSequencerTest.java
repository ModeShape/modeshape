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
public class InheritingXmlSequencerTest {

    private static final String DOCUMENT = "dnaxml:document";

    private XmlSequencer sequencer;
    private InputStream stream;
    private MockSequencerOutput output;
    private URL xsd;
    private SequencerContext context;

    @Before
    public void beforeEach() {
        sequencer = new InheritingXmlSequencer();
        context = new MockSequencerContext();
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
