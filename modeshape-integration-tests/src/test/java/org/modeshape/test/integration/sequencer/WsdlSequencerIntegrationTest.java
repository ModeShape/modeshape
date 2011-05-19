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
package org.modeshape.test.integration.sequencer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.sequencer.sramp.SrampLexicon;
import org.modeshape.sequencer.wsdl.WsdlLexicon;

public class WsdlSequencerIntegrationTest extends AbstractSequencerTest {

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.test.ModeShapeUnitTest#getPathToDefaultConfiguration()
     */
    @Override
    protected String getPathToDefaultConfiguration() {
        return "config/configRepositoryForWsdlSequencing.xml";
    }

    @Before
    @Override
    public void beforeEach() throws Exception {
        super.beforeEach();
        session().getWorkspace()
                 .getNamespaceRegistry()
                 .registerNamespace(SrampLexicon.Namespace.PREFIX, SrampLexicon.Namespace.URI);
        session().getWorkspace()
                 .getNamespaceRegistry()
                 .registerNamespace(WsdlLexicon.Namespace.PREFIX, WsdlLexicon.Namespace.URI);
    }

    @After
    @Override
    public void afterEach() throws Exception {
        super.afterEach();
    }

    @Test
    public void shouldGenerateOutputForWsdlInput_stockQuote() throws Exception {
        // Uncomment next line to get the output graph showing the WSDL Sequencer chapter of the Ref Guide
        // print = true;
        uploadFile("sequencers/wsdl/stockQuote.wsdl", "/files/");
        // Find the sequenced node ...
        printSubgraph(waitUntilSequencedNodeIsAvailable("/sequenced/wsdl", "nt:unstructured"));
    }

    @Test
    public void shouldGenerateOutputForWsdlInput_loanServicePT() throws Exception {
        // Uncomment next line to get the output graph showing the WSDL Sequencer chapter of the Ref Guide
        // print = true;
        uploadFile("sequencers/wsdl/loanServicePT.wsdl", "/files/");
        // Find the sequenced node ...
        printSubgraph(waitUntilSequencedNodeIsAvailable("/sequenced/wsdl", "nt:unstructured"));
    }

    @Test
    public void shouldGenerateOutputForWsdlInput_uddi_api_v3_portType() throws Exception {
        // Uncomment next line to get the output graph showing the WSDL Sequencer chapter of the Ref Guide
        // print = true;
        uploadFile("sequencers/wsdl/uddi_api_v3_portType.wsdl", "/files/");
        // Find the sequenced node ...
        printSubgraph(waitUntilSequencedNodeIsAvailable("/sequenced/wsdl", "nt:unstructured"));
    }

    @Test
    public void shouldGenerateOutputForWsdlInput_uddi_custody_v3_binding() throws Exception {
        // Uncomment next line to get the output graph showing the WSDL Sequencer chapter of the Ref Guide
        // print = true;
        uploadFile("sequencers/wsdl/uddi_custody_v3_binding.wsdl", "/files/");
        // Find the sequenced node ...
        printSubgraph(waitUntilSequencedNodeIsAvailable("/sequenced/wsdl", "nt:unstructured"));
    }

    @Test
    public void shouldGenerateOutputForWsdlInput_uddi_repl_v3_binding() throws Exception {
        // Uncomment next line to get the output graph showing the WSDL Sequencer chapter of the Ref Guide
        // print = true;
        uploadFile("sequencers/wsdl/uddi_repl_v3_binding.wsdl", "/files/");
        // Find the sequenced node ...
        printSubgraph(waitUntilSequencedNodeIsAvailable("/sequenced/wsdl", "nt:unstructured"));
    }

    @Test
    public void shouldGenerateOutputForWsdlInput_uddi_repl_v3_portType() throws Exception {
        // Uncomment next line to get the output graph showing the WSDL Sequencer chapter of the Ref Guide
        // print = true;
        uploadFile("sequencers/wsdl/uddi_repl_v3_portType.wsdl", "/files/");
        // Find the sequenced node ...
        printSubgraph(waitUntilSequencedNodeIsAvailable("/sequenced/wsdl", "nt:unstructured"));
    }

    @Test
    public void shouldGenerateOutputForWsdlInput_uddi_sub_v3_binding() throws Exception {
        // Uncomment next line to get the output graph showing the WSDL Sequencer chapter of the Ref Guide
        // print = true;
        uploadFile("sequencers/wsdl/uddi_sub_v3_binding.wsdl", "/files/");
        // Find the sequenced node ...
        printSubgraph(waitUntilSequencedNodeIsAvailable("/sequenced/wsdl", "nt:unstructured"));
    }

    @Test
    public void shouldGenerateOutputForWsdlInput_uddi_sub_v3_portType() throws Exception {
        // Uncomment next line to get the output graph showing the WSDL Sequencer chapter of the Ref Guide
        // print = true;
        uploadFile("sequencers/wsdl/uddi_sub_v3_portType.wsdl", "/files/");
        // Find the sequenced node ...
        printSubgraph(waitUntilSequencedNodeIsAvailable("/sequenced/wsdl", "nt:unstructured"));
    }

    @Test
    public void shouldGenerateOutputForWsdlInput_uddi_sbr_v3_binding() throws Exception {
        // Uncomment next line to get the output graph showing the WSDL Sequencer chapter of the Ref Guide
        // print = true;
        uploadFile("sequencers/wsdl/uddi_subr_v3_binding.wsdl", "/files/");
        // Find the sequenced node ...
        printSubgraph(waitUntilSequencedNodeIsAvailable("/sequenced/wsdl", "nt:unstructured"));
    }

    @Test
    public void shouldGenerateOutputForWsdlInput_uddi_v3_service() throws Exception {
        // Uncomment next line to get the output graph showing the WSDL Sequencer chapter of the Ref Guide
        // print = true;
        uploadFile("sequencers/wsdl/uddi_v3_service.wsdl", "/files/");
        // Find the sequenced node ...
        printSubgraph(waitUntilSequencedNodeIsAvailable("/sequenced/wsdl", "nt:unstructured"));
    }

    @Test
    public void shouldGenerateOutputForWsdlInput_uddi_vs_v3_binding() throws Exception {
        // Uncomment next line to get the output graph showing the WSDL Sequencer chapter of the Ref Guide
        // print = true;
        uploadFile("sequencers/wsdl/uddi_vs_v3_binding.wsdl", "/files/");
        // Find the sequenced node ...
        printSubgraph(waitUntilSequencedNodeIsAvailable("/sequenced/wsdl", "nt:unstructured"));
    }

    @Test
    public void shouldGenerateOutputForWsdlInput_uddi_vs_v3_portType() throws Exception {
        // Uncomment next line to get the output graph showing the WSDL Sequencer chapter of the Ref Guide
        // print = true;
        uploadFile("sequencers/wsdl/uddi_vs_v3_portType.wsdl", "/files/");
        // Find the sequenced node ...
        printSubgraph(waitUntilSequencedNodeIsAvailable("/sequenced/wsdl", "nt:unstructured"));
    }

    @Test
    public void shouldGenerateOutputForWsdlInput_uddi_vscache_v3_binding() throws Exception {
        // Uncomment next line to get the output graph showing the WSDL Sequencer chapter of the Ref Guide
        // print = true;
        uploadFile("sequencers/wsdl/uddi_vscache_v3_binding.wsdl", "/files/");
        // Find the sequenced node ...
        printSubgraph(waitUntilSequencedNodeIsAvailable("/sequenced/wsdl", "nt:unstructured"));
    }

    @Test
    public void shouldGenerateOutputForWsdlInput_uddi_vscache_v3_portType() throws Exception {
        // Uncomment next line to get the output graph showing the WSDL Sequencer chapter of the Ref Guide
        // print = true;
        uploadFile("sequencers/wsdl/uddi_vscache_v3_portType.wsdl", "/files/");
        // Find the sequenced node ...
        printSubgraph(waitUntilSequencedNodeIsAvailable("/sequenced/wsdl", "nt:unstructured"));
        //
        // Node wsdl = assertNode(path, "modexml:document", "mode:derived");
        // printSubgraph(wsdl);
        //
        // // Node file1 = assertNode(path + "/nt:activity", "nt:nodeType");
        // // assertThat(file1, is(notNullValue()));
        //
        // printQuery("SELECT * FROM [modexml:document]", 1);
        // printQuery("SELECT * FROM [modexml:element] WHERE NAME() = 'xhtml:head'", 1);
        // printQuery("SELECT * FROM [modexml:element] WHERE NAME() = 'xhtml:title'", 1);
        // printQuery("SELECT * FROM [modexml:element] WHERE NAME() = 'svg:ellipse'", 1);
        // printQuery("SELECT * FROM [modexml:element] WHERE NAME() = 'svg:rect'", 1);
        // printQuery("SELECT * FROM [modexml:element] WHERE NAME() = 'xhtml:p'", 2);
        // printQuery("SELECT * FROM [modexml:elementContent]", 13);
    }

}
