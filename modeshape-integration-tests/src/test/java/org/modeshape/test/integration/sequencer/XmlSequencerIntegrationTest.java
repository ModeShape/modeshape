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

import javax.jcr.Node;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.FixFor;

public class XmlSequencerIntegrationTest extends AbstractSequencerTest {

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.test.ModeShapeUnitTest#getPathToDefaultConfiguration()
     */
    @Override
    protected String getPathToDefaultConfiguration() {
        return "config/configRepositoryForXmlSequencing.xml";
    }

    @Before
    @Override
    public void beforeEach() throws Exception {
        super.beforeEach();
        session().getWorkspace().getNamespaceRegistry().registerNamespace("xhtml", "http://www.w3.org/1999/xhtml");
        session().getWorkspace().getNamespaceRegistry().registerNamespace("mathml", "http://www.w3.org/1998/Math/MathML");
        session().getWorkspace().getNamespaceRegistry().registerNamespace("svg", "http://www.w3.org/2000/svg");
    }

    @After
    @Override
    public void afterEach() throws Exception {
        super.afterEach();
    }

    @Test
    public void generateSequencerOutputForXmlSequencerChapterOfReferenceGuide() throws Exception {
        // Uncomment next line to get the output graph showin the XML Sequencer chapter of the Ref Guide
        print = true;
        uploadFile("docForReferenceGuide.xml", "/files/");

        // Find the sequenced node ...
        printSubgraph(waitUntilSequencedNodeIsAvailable("/sequenced/xml", "nt:unstructured"));
    }

    @Test
    public void shouldSequenceXmlFile() throws Exception {
        // print = true;
        uploadFile("jcr-import-test.xml", "/files/");

        // Find the sequenced node ...
        printSubgraph(waitUntilSequencedNodeIsAvailable("/sequenced/xml", "nt:unstructured"));
        String path = "/sequenced/xml/jcr-import-test.xml";
        Node xml = assertNode(path, "modexml:document", "mode:derived");
        printSubgraph(xml);

        // Node file1 = assertNode(path + "/nt:activity", "nt:nodeType");
        // assertThat(file1, is(notNullValue()));

        printQuery("SELECT * FROM [modexml:document]", 1);
        printQuery("SELECT * FROM [modexml:element] WHERE NAME() = 'xhtml:head'", 1);
        printQuery("SELECT * FROM [modexml:element] WHERE NAME() = 'xhtml:title'", 1);
        printQuery("SELECT * FROM [modexml:element] WHERE NAME() = 'svg:ellipse'", 1);
        printQuery("SELECT * FROM [modexml:element] WHERE NAME() = 'svg:rect'", 1);
        printQuery("SELECT * FROM [modexml:element] WHERE NAME() = 'xhtml:p'", 2);
        printQuery("SELECT * FROM [modexml:elementContent]", 13);
    }

    @Test
    public void shouldSequenceXmlFileBelowSequencedPath() throws Exception {
        // print = true;
        uploadFile("jcr-import-test.xml", "/files/a/b");

        // Find the sequenced node ...
        String path = "/sequenced/xml/a/b/jcr-import-test.xml";
        Node xml = waitUntilSequencedNodeIsAvailable(path, "modexml:document", "mode:derived");
        printSubgraph(xml);

        // Node file1 = assertNode(path + "/nt:activity", "nt:nodeType");
        // assertThat(file1, is(notNullValue()));

        printQuery("SELECT * FROM [modexml:document]", 1);
        printQuery("SELECT * FROM [modexml:element] WHERE NAME() = 'xhtml:head'", 1);
        printQuery("SELECT * FROM [modexml:element] WHERE NAME() = 'xhtml:title'", 1);
        printQuery("SELECT * FROM [modexml:element] WHERE NAME() = 'svg:ellipse'", 1);
        printQuery("SELECT * FROM [modexml:element] WHERE NAME() = 'svg:rect'", 1);
        printQuery("SELECT * FROM [modexml:element] WHERE NAME() = 'xhtml:p'", 2);
        printQuery("SELECT * FROM [modexml:elementContent]", 13);
    }

    @FixFor( "MODE-981" )
    @Test
    public void shouldSequence2XmlFiles2() throws Exception {
        // print = true;
        uploadFile("docWithComments.xml", "/files/");

        // Find the sequenced node ...
        waitUntilSequencedNodeIsAvailable("/sequenced/xml/docWithComments.xml", "modexml:document", "mode:derived");
        printQuery("SELECT * FROM [nt:base] ORDER BY [jcr:path]", 17);

        // Find the sequenced node ...
        uploadFile("docWithComments2.xml", "/files/");
        waitUntilSequencedNodeIsAvailable("/sequenced/xml/docWithComments2.xml", "modexml:document", "mode:derived");

        printQuery("SELECT * FROM [nt:base]  ORDER BY [jcr:path]", 30);
        printSubgraph(assertNode("/sequenced/xml", "nt:unstructured"));
        uploadFile("docWithComments.xml", "/files/");
        printSubgraph(waitUntilSequencedNodeIsAvailable("/sequenced/xml", "nt:unstructured"));
    }
}
