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

package org.modeshape.sequencer.xsd;

import javax.jcr.Node;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
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

    private void assertSequencedSuccessfully( String filename ) throws Exception {
        int pathSeparatorIndex = filename.lastIndexOf("/");
        String nodeName = pathSeparatorIndex != -1 ? filename.substring(pathSeparatorIndex + 1) : filename;

        createNodeWithContentFromFile(nodeName, filename);

        Node sequencedNode = getSequencedNode(rootNode, nodeName + "/" + XsdLexicon.SCHEMA_DOCUMENT, 4);
        assertNotNull(sequencedNode);
        assertEquals(XsdLexicon.SCHEMA_DOCUMENT, sequencedNode.getPrimaryNodeType().getName());
        assertTrue(sequencedNode.getNodes().getSize() > 0);
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
    public void shouldBeAbleToParseXsdFromDefinitiveXmlSchemaExampleChapter04ord1() throws Exception {
        assertSequencedSuccessfully("definitiveXmlSchema/chapter04ord1.xsd");
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
}
