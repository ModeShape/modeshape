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

public class XsdSequencerIntegrationTest extends AbstractSequencerTest {

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.test.ModeShapeUnitTest#getPathToDefaultConfiguration()
     */
    @Override
    protected String getPathToDefaultConfiguration() {
        return "config/configRepositoryForXsdSequencing.xml";
    }

    @Before
    @Override
    public void beforeEach() throws Exception {
        super.beforeEach();
        session().getWorkspace()
                 .getNamespaceRegistry()
                 .registerNamespace(SrampLexicon.Namespace.PREFIX, SrampLexicon.Namespace.URI);
    }

    @After
    @Override
    public void afterEach() throws Exception {
        super.afterEach();
    }

    @Test
    public void generateSequencerOutputForStockQuoteSchema() throws Exception {
        // Uncomment next line to get the output graph showing the XSD Sequencer chapter of the Ref Guide
        // print = true;
        uploadFile("sequencers/xsd/stockQuote.xsd", "/files/");

        // Find the sequenced node ...
        printSubgraph(waitUntilSequencedNodeIsAvailable("/sequenced/xsd", "nt:unstructured"));
    }

    @Test
    public void generateSequencerOutputForStockQuoteWithDefaultNamespaceSchema() throws Exception {
        // Uncomment next line to get the output graph showing the XSD Sequencer chapter of the Ref Guide
        // print = true;
        uploadFile("sequencers/xsd/stockQuoteWithDefaultNamespace.xsd", "/files/");

        // Find the sequenced node ...
        printSubgraph(waitUntilSequencedNodeIsAvailable("/sequenced/xsd", "nt:unstructured"));
    }

    @Test
    public void generateSequencerOutputForPurchaseOrderSchema() throws Exception {
        // Uncomment next line to get the output graph showing the XSD Sequencer chapter of the Ref Guide
        // print = true;
        uploadFile("sequencers/xsd/po.xsd", "/files/");

        // Find the sequenced node ...
        printSubgraph(waitUntilSequencedNodeIsAvailable("/sequenced/xsd", "nt:unstructured"));
        //
        // Node xsd = assertNode(path, "modexml:document", "mode:derived");
        // printSubgraph(xsd);
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

    @Test
    public void generateSequencerOutputForUddiV3Schema() throws Exception {
        // Uncomment next line to get the output graph showing the XSD Sequencer chapter of the Ref Guide
        // print = true;
        uploadFile("sequencers/xsd/uddi_v3.xsd", "/files/");

        // Find the sequenced node ...
        printSubgraph(waitUntilSequencedNodeIsAvailable("/sequenced/xsd", "nt:unstructured"));
    }

    @Test
    public void generateSequencerOutputForUddiV3CustodySchema() throws Exception {
        // Uncomment next line to get the output graph showing the XSD Sequencer chapter of the Ref Guide
        // print = true;
        uploadFile("sequencers/xsd/uddi_v3custody.xsd", "/files/");

        // Find the sequenced node ...
        printSubgraph(waitUntilSequencedNodeIsAvailable("/sequenced/xsd", "nt:unstructured"));
    }

    @Test
    public void generateSequencerOutputForUddiV3PolicyInstanceParametersSchema() throws Exception {
        // Uncomment next line to get the output graph showing the XSD Sequencer chapter of the Ref Guide
        // print = true;
        uploadFile("sequencers/xsd/uddi_v3policy_instanceParms.xsd", "/files/");

        // Find the sequenced node ...
        printSubgraph(waitUntilSequencedNodeIsAvailable("/sequenced/xsd", "nt:unstructured"));
    }

    @Test
    public void generateSequencerOutputForUddiV3PolicySchema() throws Exception {
        // Uncomment next line to get the output graph showing the XSD Sequencer chapter of the Ref Guide
        // print = true;
        uploadFile("sequencers/xsd/uddi_v3policy.xsd", "/files/");

        // Find the sequenced node ...
        printSubgraph(waitUntilSequencedNodeIsAvailable("/sequenced/xsd", "nt:unstructured"));
    }

    @Test
    public void generateSequencerOutputForUddiV3ReplicationSchema() throws Exception {
        // Uncomment next line to get the output graph showing the XSD Sequencer chapter of the Ref Guide
        // print = true;
        uploadFile("sequencers/xsd/uddi_v3replication.xsd", "/files/");

        // Find the sequenced node ...
        printSubgraph(waitUntilSequencedNodeIsAvailable("/sequenced/xsd", "nt:unstructured"));
    }

    @Test
    public void generateSequencerOutputForUddiV3SubscriptionListenerSchema() throws Exception {
        // Uncomment next line to get the output graph showing the XSD Sequencer chapter of the Ref Guide
        // print = true;
        uploadFile("sequencers/xsd/uddi_v3subscriptionListener.xsd", "/files/");

        // Find the sequenced node ...
        printSubgraph(waitUntilSequencedNodeIsAvailable("/sequenced/xsd", "nt:unstructured"));
    }

    @Test
    public void generateSequencerOutputForUddiV3ValueSetSchema() throws Exception {
        // Uncomment next line to get the output graph showing the XSD Sequencer chapter of the Ref Guide
        // print = true;
        uploadFile("sequencers/xsd/uddi_v3valueset.xsd", "/files/");

        // Find the sequenced node ...
        printSubgraph(waitUntilSequencedNodeIsAvailable("/sequenced/xsd", "nt:unstructured"));
    }

    @Test
    public void generateSequencerOutputForUddiV3ValueSetCachingSchema() throws Exception {
        // Uncomment next line to get the output graph showing the XSD Sequencer chapter of the Ref Guide
        // print = true;
        uploadFile("sequencers/xsd/uddi_v3valuesetcaching.xsd", "/files/");

        // Find the sequenced node ...
        printSubgraph(waitUntilSequencedNodeIsAvailable("/sequenced/xsd", "nt:unstructured"));
    }

    @Test
    public void generateSequencerOutputForDefinitiveChapter01Schema() throws Exception {
        // Uncomment next line to get the output graph showing the XSD Sequencer chapter of the Ref Guide
        // print = true;
        uploadFile("sequencers/xsd/definitiveXmlSchema/chapter01.xsd", "/files/");

        // Find the sequenced node ...
        printSubgraph(waitUntilSequencedNodeIsAvailable("/sequenced/xsd", "nt:unstructured"));
    }

    @Test
    public void generateSequencerOutputForDefinitiveChapter03EnvSchema() throws Exception {
        // Uncomment next line to get the output graph showing the XSD Sequencer chapter of the Ref Guide
        // print = true;
        uploadFile("sequencers/xsd/definitiveXmlSchema/chapter03env.xsd", "/files/");

        // Find the sequenced node ...
        printSubgraph(waitUntilSequencedNodeIsAvailable("/sequenced/xsd", "nt:unstructured"));
    }

    @Test
    public void generateSequencerOutputForDefinitiveChapter03OrdSchema() throws Exception {
        // Uncomment next line to get the output graph showing the XSD Sequencer chapter of the Ref Guide
        // print = true;
        uploadFile("sequencers/xsd/definitiveXmlSchema/chapter03ord.xsd", "/files/");

        // Find the sequenced node ...
        printSubgraph(waitUntilSequencedNodeIsAvailable("/sequenced/xsd", "nt:unstructured"));
    }

    @Test
    public void generateSequencerOutputForDefinitiveChapter03ProdSchema() throws Exception {
        // Uncomment next line to get the output graph showing the XSD Sequencer chapter of the Ref Guide
        // print = true;
        uploadFile("sequencers/xsd/definitiveXmlSchema/chapter03prod.xsd", "/files/");

        // Find the sequenced node ...
        printSubgraph(waitUntilSequencedNodeIsAvailable("/sequenced/xsd", "nt:unstructured"));
    }

    @Test
    public void generateSequencerOutputForDefinitiveChapter03Prod2Schema() throws Exception {
        // Uncomment next line to get the output graph showing the XSD Sequencer chapter of the Ref Guide
        // print = true;
        uploadFile("sequencers/xsd/definitiveXmlSchema/chapter03prod2.xsd", "/files/");

        // Find the sequenced node ...
        printSubgraph(waitUntilSequencedNodeIsAvailable("/sequenced/xsd", "nt:unstructured"));
    }

    @Test
    public void generateSequencerOutputForDefinitiveChapter04CustSchema() throws Exception {
        // Uncomment next line to get the output graph showing the XSD Sequencer chapter of the Ref Guide
        // print = true;
        uploadFile("sequencers/xsd/definitiveXmlSchema/chapter04cust.xsd", "/files/");

        // Find the sequenced node ...
        printSubgraph(waitUntilSequencedNodeIsAvailable("/sequenced/xsd", "nt:unstructured"));
    }

    @Test
    public void generateSequencerOutputForDefinitiveChapter04Ord1Schema() throws Exception {
        // Uncomment next line to get the output graph showing the XSD Sequencer chapter of the Ref Guide
        // print = true;
        uploadFile("sequencers/xsd/definitiveXmlSchema/chapter04ord1.xsd", "/files/");

        // Find the sequenced node ...
        printSubgraph(waitUntilSequencedNodeIsAvailable("/sequenced/xsd", "nt:unstructured"));
    }

    @Test
    public void generateSequencerOutputForDefinitiveChapter04Ord2Schema() throws Exception {
        // Uncomment next line to get the output graph showing the XSD Sequencer chapter of the Ref Guide
        // print = true;
        uploadFile("sequencers/xsd/definitiveXmlSchema/chapter04ord2.xsd", "/files/");

        // Find the sequenced node ...
        printSubgraph(waitUntilSequencedNodeIsAvailable("/sequenced/xsd", "nt:unstructured"));
    }

    @Test
    public void generateSequencerOutputForDefinitiveChapter04ProdSchema() throws Exception {
        // Uncomment next line to get the output graph showing the XSD Sequencer chapter of the Ref Guide
        // print = true;
        uploadFile("sequencers/xsd/definitiveXmlSchema/chapter04prod.xsd", "/files/");

        // Find the sequenced node ...
        printSubgraph(waitUntilSequencedNodeIsAvailable("/sequenced/xsd", "nt:unstructured"));
    }

    @Test
    public void generateSequencerOutputForDefinitiveChapter05OrdSchema() throws Exception {
        // Uncomment next line to get the output graph showing the XSD Sequencer chapter of the Ref Guide
        // print = true;
        uploadFile("sequencers/xsd/definitiveXmlSchema/chapter05ord.xsd", "/files/");

        // Find the sequenced node ...
        printSubgraph(waitUntilSequencedNodeIsAvailable("/sequenced/xsd", "nt:unstructured"));
    }

    @Test
    public void generateSequencerOutputForDefinitiveChapter05ProdSchema() throws Exception {
        // Uncomment next line to get the output graph showing the XSD Sequencer chapter of the Ref Guide
        // print = true;
        uploadFile("sequencers/xsd/definitiveXmlSchema/chapter05prod.xsd", "/files/");

        // Find the sequenced node ...
        printSubgraph(waitUntilSequencedNodeIsAvailable("/sequenced/xsd", "nt:unstructured"));
    }

    @Test
    public void generateSequencerOutputForDefinitiveChapter07Schema() throws Exception {
        // Uncomment next line to get the output graph showing the XSD Sequencer chapter of the Ref Guide
        // print = true;
        uploadFile("sequencers/xsd/definitiveXmlSchema/chapter07.xsd", "/files/");

        // Find the sequenced node ...
        printSubgraph(waitUntilSequencedNodeIsAvailable("/sequenced/xsd", "nt:unstructured"));
    }

    @Test
    public void generateSequencerOutputForDefinitiveChapter08Schema() throws Exception {
        // Uncomment next line to get the output graph showing the XSD Sequencer chapter of the Ref Guide
        // print = true;
        uploadFile("sequencers/xsd/definitiveXmlSchema/chapter08.xsd", "/files/");

        // Find the sequenced node ...
        printSubgraph(waitUntilSequencedNodeIsAvailable("/sequenced/xsd", "nt:unstructured"));
    }

    @Test
    public void generateSequencerOutputForDefinitiveChapter09Schema() throws Exception {
        // Uncomment next line to get the output graph showing the XSD Sequencer chapter of the Ref Guide
        // print = true;
        uploadFile("sequencers/xsd/definitiveXmlSchema/chapter09.xsd", "/files/");

        // Find the sequenced node ...
        printSubgraph(waitUntilSequencedNodeIsAvailable("/sequenced/xsd", "nt:unstructured"));
    }

    @Test
    public void generateSequencerOutputForDefinitiveChapter11Schema() throws Exception {
        // Uncomment next line to get the output graph showing the XSD Sequencer chapter of the Ref Guide
        // print = true;
        uploadFile("sequencers/xsd/definitiveXmlSchema/chapter11.xsd", "/files/");

        // Find the sequenced node ...
        printSubgraph(waitUntilSequencedNodeIsAvailable("/sequenced/xsd", "nt:unstructured"));
    }

    @Test
    public void generateSequencerOutputForDefinitiveChapter13Schema() throws Exception {
        // Uncomment next line to get the output graph showing the XSD Sequencer chapter of the Ref Guide
        // print = true;
        uploadFile("sequencers/xsd/definitiveXmlSchema/chapter13.xsd", "/files/");

        // Find the sequenced node ...
        printSubgraph(waitUntilSequencedNodeIsAvailable("/sequenced/xsd", "nt:unstructured"));
    }

    @Test
    public void generateSequencerOutputForDefinitiveChapter014Schema() throws Exception {
        // Uncomment next line to get the output graph showing the XSD Sequencer chapter of the Ref Guide
        // print = true;
        uploadFile("sequencers/xsd/definitiveXmlSchema/chapter14.xsd", "/files/");

        // Find the sequenced node ...
        printSubgraph(waitUntilSequencedNodeIsAvailable("/sequenced/xsd", "nt:unstructured"));
    }

    @Test
    public void generateSequencerOutputForDefinitiveChapter015Schema() throws Exception {
        // Uncomment next line to get the output graph showing the XSD Sequencer chapter of the Ref Guide
        // print = true;
        uploadFile("sequencers/xsd/definitiveXmlSchema/chapter15.xsd", "/files/");

        // Find the sequenced node ...
        printSubgraph(waitUntilSequencedNodeIsAvailable("/sequenced/xsd", "nt:unstructured"));
    }

    @Test
    public void generateSequencerOutputForDefinitiveChapter016Schema() throws Exception {
        // Uncomment next line to get the output graph showing the XSD Sequencer chapter of the Ref Guide
        // print = true;
        uploadFile("sequencers/xsd/definitiveXmlSchema/chapter16.xsd", "/files/");

        // Find the sequenced node ...
        printSubgraph(waitUntilSequencedNodeIsAvailable("/sequenced/xsd", "nt:unstructured"));
    }

    @Test
    public void generateSequencerOutputForDefinitiveChapter017Schema() throws Exception {
        // Uncomment next line to get the output graph showing the XSD Sequencer chapter of the Ref Guide
        // print = true;
        uploadFile("sequencers/xsd/definitiveXmlSchema/chapter17.xsd", "/files/");

        // Find the sequenced node ...
        printSubgraph(waitUntilSequencedNodeIsAvailable("/sequenced/xsd", "nt:unstructured"));
    }
}
