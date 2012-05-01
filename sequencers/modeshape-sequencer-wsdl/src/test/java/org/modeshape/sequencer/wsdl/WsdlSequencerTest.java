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

package org.modeshape.sequencer.wsdl;

import javax.jcr.Node;
import javax.wsdl.WSDLException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.modeshape.jcr.api.observation.Event;
import org.modeshape.jcr.sequencer.AbstractSequencerTest;
import java.util.Map;

/**
 * Unit test for {@link WsdlSequencer}
 *
 * Note: this was ported from 2.x and is just a suite of smoke-tests, as the sequenced content isn't asserted anywhere.
 */
public class WsdlSequencerTest extends AbstractSequencerTest {

    @Test
    public void shouldBeAbleToSequence_loanServicePT() throws Exception {
        assertSequencedSuccessfully("loanServicePT.wsdl");
    }

    @Test
    public void shouldBeAbleToSequence_stockQuote() throws Exception {
        assertSequencedSuccessfully("stockQuote.wsdl");
    }

    @Test
    public void shouldBeAbleToSequence_uddi_api_v3_portType() throws Exception {
        assertSequencedSuccessfully("uddi_api_v3_portType.wsdl");
    }

    @Test
    public void shouldBeAbleToSequence_uddi_custody_v3_binding() throws Exception {
        assertSequencedSuccessfully("uddi_custody_v3_binding.wsdl");
    }

    @Test
    public void shouldBeAbleToSequence_uddi_repl_v3_binding() throws Exception {
        assertSequencedSuccessfully("uddi_repl_v3_binding.wsdl");
    }

    @Test
    public void shouldBeAbleToSequence_uddi_repl_v3_portType() throws Exception {
        assertSequencedSuccessfully("uddi_repl_v3_portType.wsdl");
    }

    @Test
    public void shouldBeAbleToSequence_uddi_sub_v3_binding() throws Exception {
        assertSequencedSuccessfully("uddi_sub_v3_binding.wsdl");
    }

    @Test
    public void shouldBeAbleToSequence_uddi_sub_v3_portType() throws Exception {
        assertSequencedSuccessfully("uddi_sub_v3_portType.wsdl");
    }

    @Test
    public void shouldBeAbleToSequence_uddi_sbr_v3_binding() throws Exception {
        assertSequencedSuccessfully("uddi_subr_v3_binding.wsdl");
    }

    @Test
    public void shouldBeAbleToSequence_uddi_v3_service() throws Exception {
        assertSequencedSuccessfully("uddi_v3_service.wsdl");
    }

    @Test
    public void shouldBeAbleToSequence_uddi_vs_v3_binding() throws Exception {
        assertSequencedSuccessfully("uddi_vs_v3_binding.wsdl");
    }

    @Test
    public void shouldBeAbleToSequence_uddi_vs_v3_portType() throws Exception {
        assertSequencedSuccessfully("uddi_vs_v3_portType.wsdl");
    }

    @Test
    public void shouldBeAbleToSequence_uddi_vscache_v3_binding() throws Exception {
        assertSequencedSuccessfully("uddi_vscache_v3_binding.wsdl");
    }

    @Test
    public void shouldBeAbleToSequence_uddi_vscache_v3_portType() throws Exception {
        assertSequencedSuccessfully("uddi_vscache_v3_portType.wsdl");
    }

    @Test
    public void shouldFailIfWsdlInvalid() throws Throwable {
        String filename = "invalid.wsdl";
        createNodeWithContentFromFile(filename, filename);
        Node sequencedNode = rootNode.getNode("invalid.wsdl/jcr:content");

        expectSequencingFailure(sequencedNode);
        Map eventInfo = assertSequencingEventInfo(sequencedNode, session.getUserID(), "WSDL sequencer", sequencedNode.getPath(), "/wsdl");
        assertEquals(WSDLException.class.getName(), eventInfo.get(Event.Sequencing.SEQUENCING_FAILURE_CAUSE).getClass().getName());
    }

    private void assertSequencedSuccessfully( String filePath ) throws Exception {
        Node parentNode = createNodeWithContentFromFile(filePath, filePath);
        Node wsdlDocument = getSequencedNode(rootNode, "wsdl/" + filePath);
        assertNotNull(wsdlDocument);
        assertEquals(WsdlLexicon.WSDL_DOCUMENT, wsdlDocument.getPrimaryNodeType().getName());
        assertCreatedBySessionUser(wsdlDocument, session);
        assertTrue(wsdlDocument.getNodes().getSize() > 0);

        Node sequencedNode = parentNode.getNode("jcr:content");
        assertSequencingEventInfo(sequencedNode, session.getUserID(), "WSDL sequencer", sequencedNode.getPath(), "/wsdl");
    }
}
