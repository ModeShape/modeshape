/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.modeshape.sequencer.xsd;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import javax.jcr.Node;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.jcr.sequencer.AbstractSequencerTest;

/**
 * Unit test for {@link XsdSequencer}
 *
 * Note: this was ported from 2.x and is just a suite of smoke-tests, as the sequenced content isn't asserted anywhere.
 */
public class XsdSequencerTest extends AbstractSequencerTest {

    @Test
    public void shouldBeAbleToParseXsdForStockQuote() throws Exception {
        assertSequencedSuccessfully("stockQuote.xsd");
    }

    private Node assertSequencedSuccessfully( String filename ) throws Exception {
        int pathSeparatorIndex = filename.lastIndexOf("/");
        String nodeName = pathSeparatorIndex != -1 ? filename.substring(pathSeparatorIndex + 1) : filename;

        createNodeWithContentFromFile(nodeName, filename);

        Node outputNode = getOutputNode(rootNode, nodeName + "/" + XsdLexicon.SCHEMA_DOCUMENT);
        assertNotNull(outputNode);
        assertCreatedBySessionUser(outputNode, session);
        assertEquals(XsdLexicon.SCHEMA_DOCUMENT, outputNode.getPrimaryNodeType().getName());
        assertTrue(outputNode.getNodes().getSize() > 0);

        return outputNode;
    }

    @Test
    public void shouldBeAbleToParseXsdForUddiV3() throws Exception {
        assertSequencedSuccessfully("uddi_v3.xsd");
    }

    @Test
    public void shouldBeAbleToParseXsdFromDefinitiveXmlSchemaExampleChapter01() throws Exception {
        assertSequencedSuccessfully("definitiveXmlSchema/chapter01.xsd");
    }

    @Test
    public void shouldBeAbleToParseXsdFromDefinitiveXmlSchemaExampleChapter03env() throws Exception {
        assertSequencedSuccessfully("definitiveXmlSchema/chapter03env.xsd");
    }

    @Test
    public void shouldBeAbleToParseXsdFromDefinitiveXmlSchemaExampleChapter03ord() throws Exception {
        assertSequencedSuccessfully("definitiveXmlSchema/chapter03ord.xsd");
    }

    @Test
    public void shouldBeAbleToParseXsdFromDefinitiveXmlSchemaExampleChapter03prod() throws Exception {
        assertSequencedSuccessfully("definitiveXmlSchema/chapter03prod.xsd");
    }

    @Test
    public void shouldBeAbleToParseXsdFromDefinitiveXmlSchemaExampleChapter03prod2() throws Exception {
        assertSequencedSuccessfully("definitiveXmlSchema/chapter03prod2.xsd");
    }

    @Test
    @FixFor("MODE-2183")
    public void shouldBeAbleToParseXsdFromDefinitiveXmlSchemaExampleChapter04ord1() throws Exception {
        final Node outputNode = assertSequencedSuccessfully("definitiveXmlSchema/chapter04ord1.xsd");
        final Node includeNode = outputNode.getNode(XsdLexicon.INCLUDE);
        assertThat(includeNode.hasProperty(XsdLexicon.SCHEMA_LOCATION), is(true));
        assertThat(includeNode.hasProperty("schemaLocation"), is(false));
    }

    @Test
    public void shouldBeAbleToParseXsdFromDefinitiveXmlSchemaExampleChapter04ord2() throws Exception {
        assertSequencedSuccessfully("definitiveXmlSchema/chapter04ord2.xsd");
    }

    @Test
    public void shouldBeAbleToParseXsdFromDefinitiveXmlSchemaExampleChapter04prod() throws Exception {
        assertSequencedSuccessfully("definitiveXmlSchema/chapter04prod.xsd");
    }

    @Test
    public void shouldBeAbleToParseXsdFromDefinitiveXmlSchemaExampleChapter05ord() throws Exception {
        assertSequencedSuccessfully("definitiveXmlSchema/chapter05ord.xsd");
    }

    @Test
    public void shouldBeAbleToParseXsdFromDefinitiveXmlSchemaExampleChapter05prod() throws Exception {
        assertSequencedSuccessfully("definitiveXmlSchema/chapter05prod.xsd");
    }

    @Test
    public void shouldBeAbleToParseXsdFromDefinitiveXmlSchemaExampleChapter07() throws Exception {
        assertSequencedSuccessfully("definitiveXmlSchema/chapter07.xsd");
    }

    @Test
    public void shouldBeAbleToParseXsdFromDefinitiveXmlSchemaExampleChapter08() throws Exception {
        assertSequencedSuccessfully("definitiveXmlSchema/chapter08.xsd");
    }

    @Test
    public void shouldBeAbleToParseXsdFromDefinitiveXmlSchemaExampleChapter09() throws Exception {
        assertSequencedSuccessfully("definitiveXmlSchema/chapter09.xsd");
    }

    @Test
    public void shouldBeAbleToParseXsdFromDefinitiveXmlSchemaExampleChapter11() throws Exception {
        assertSequencedSuccessfully("definitiveXmlSchema/chapter11.xsd");
    }

    @Test
    public void shouldBeAbleToParseXsdFromDefinitiveXmlSchemaExampleChapter13() throws Exception {
        assertSequencedSuccessfully("definitiveXmlSchema/chapter13.xsd");
    }

    @Test
    public void shouldBeAbleToParseXsdFromDefinitiveXmlSchemaExampleChapter14() throws Exception {
        assertSequencedSuccessfully("definitiveXmlSchema/chapter14.xsd");
    }

    @Test
    public void shouldBeAbleToParseXsdFromDefinitiveXmlSchemaExampleChapter15() throws Exception {
        assertSequencedSuccessfully("definitiveXmlSchema/chapter15.xsd");
    }

    @Test
    @FixFor({"MODE-1464", "MODE-2183"})
    public void shouldBeAbleToParseUnsignedLong() throws Exception {
        final Node outputNode = assertSequencedSuccessfully("unsigned_long.xsd");

        final Node includeNode = outputNode.getNode(XsdLexicon.IMPORT);
        assertThat(includeNode.hasProperty(XsdLexicon.SCHEMA_LOCATION), is(true));
        assertThat(includeNode.hasProperty("schemaLocation"), is(false));

        final Node complexType = outputNode.getNode("openAttrs");
        assertThat(complexType.hasProperty(XsdLexicon.MIXED), is(true));
        assertThat(complexType.hasProperty("mixed"), is(false));
    }
}
