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

package org.modeshape.sequencer.wsdl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.util.Map;
import javax.jcr.Node;
import javax.wsdl.WSDLException;
import org.junit.Test;
import org.modeshape.jcr.api.observation.Event;
import org.modeshape.jcr.sequencer.AbstractSequencerTest;

/**
 * Unit test for {@link WsdlSequencer} Note: this was ported from 2.x and is just a suite of smoke-tests, as the sequenced content
 * isn't asserted anywhere.
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
        Map<?, ?> eventInfo = assertSequencingEventInfo(sequencedNode,
                                                        session.getUserID(),
                                                        "WSDL sequencer",
                                                        sequencedNode.getPath(),
                                                        "/wsdl");
        assertEquals(WSDLException.class.getName(), eventInfo.get(Event.Sequencing.SEQUENCING_FAILURE_CAUSE).getClass().getName());
    }

    private void assertSequencedSuccessfully( String filePath ) throws Exception {
        Node parentNode = createNodeWithContentFromFile(filePath, filePath);
        Node wsdlDocument = getOutputNode(rootNode, "wsdl/" + filePath);
        assertNotNull(wsdlDocument);
        assertEquals(WsdlLexicon.WSDL_DOCUMENT, wsdlDocument.getPrimaryNodeType().getName());
        assertCreatedBySessionUser(wsdlDocument, session);
        assertTrue(wsdlDocument.getNodes().getSize() > 0);

        Node sequencedNode = parentNode.getNode("jcr:content");
        assertSequencingEventInfo(sequencedNode, session.getUserID(), "WSDL sequencer", sequencedNode.getPath(), "/wsdl");
    }
}
