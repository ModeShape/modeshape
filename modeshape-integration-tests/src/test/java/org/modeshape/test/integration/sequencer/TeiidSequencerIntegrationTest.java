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

/**
 * 
 */
public class TeiidSequencerIntegrationTest extends AbstractSequencerTest {

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.test.integration.sequencer.AbstractSequencerTest#getResourcePathToConfigurationFile()
     */
    @Override
    protected String getResourcePathToConfigurationFile() {
        return "config/configRepositoryForTeiidSequencing.xml";
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.test.integration.AbstractSingleUseModeShapeTest#getRepositoryName()
     */
    @Override
    protected String getRepositoryName() {
        return "Content";
    }

    @Before
    @Override
    public void beforeEach() throws Exception {
        super.beforeEach();
    }

    @After
    @Override
    public void afterEach() throws Exception {
        super.afterEach();
    }

    @Test
    public void shouldStartEngineWithRegisteredTeiidNodeTypes() throws Exception {
        assertNodeType("relational:column",
                       false,
                       false,
                       true,
                       false,
                       null,
                       0,
                       42,
                       "nt:unstructured",
                       "relational:relationalEntity");
        assertNodeType("relational:baseTable", false, false, true, true, null, 0, 0, "relational:table");
        assertNodeTypes("relational:relationalEntity",
                        "relational:column",
                        "relational:columnSet",
                        "relational:uniqueKey",
                        "relational:primaryKey",
                        "relational:foreignKey",
                        "vdb:virtualDatabase",
                        "vdb:model",
                        "jdbcs:imported");
    }

    @Test
    public void shouldSequenceQuickEmployeesVdbWithoutVersionNumberInFileName() throws Exception {
        // print = true;
        uploadFile("sequencers/teiid/vdb/qe.vdb", "/files/");
        waitUntilSequencingFinishes();
        Thread.sleep(200); // wait a bit while the new content is indexed

        // Find the sequenced node ...
        Node vdb = assertNode("/sequenced/teiid/vdbs/qe", "vdb:virtualDatabase", "mix:referenceable", "mode:derived");
        printSubgraph(vdb);
        printQuery("SELECT * FROM [vdb:virtualDatabase]", 1);
        printQuery("SELECT * FROM [vdb:model]", 3);
        printQuery("SELECT * FROM [relational:table]", 2);
        printQuery("SELECT * FROM [relational:column]", 30);
        printQuery("SELECT * FROM [relational:primaryKey]", 0);
        printQuery("SELECT * FROM [relational:foreignKey]", 0);
        printQuery("SELECT * FROM [relational:procedure]", 1);
        printQuery("SELECT * FROM [relational:procedureParameter]", 1);
        printQuery("SELECT * FROM [relational:procedureResult]", 1);
    }

    @Test
    public void shouldSequenceQuickEmployeesVdbWithoutVersionNumberInFileNameUploadedBelowSequencingPath() throws Exception {
        // print = true;
        uploadFile("sequencers/teiid/vdb/qe.vdb", "/files/my/favorites");
        waitUntilSequencingFinishes();
        Thread.sleep(200); // wait a bit while the new content is indexed

        // Find the sequenced node ...
        Node vdb = assertNode("/sequenced/teiid/vdbs/my/favorites/qe", "vdb:virtualDatabase", "mix:referenceable", "mode:derived");
        printSubgraph(vdb);
        printQuery("SELECT * FROM [vdb:virtualDatabase]", 1);
        printQuery("SELECT * FROM [vdb:model]", 3);
        printQuery("SELECT * FROM [relational:table]", 2);
        printQuery("SELECT * FROM [relational:column]", 30);
        printQuery("SELECT * FROM [relational:primaryKey]", 0);
        printQuery("SELECT * FROM [relational:foreignKey]", 0);
        printQuery("SELECT * FROM [relational:procedure]", 1);
        printQuery("SELECT * FROM [relational:procedureParameter]", 1);
        printQuery("SELECT * FROM [relational:procedureResult]", 1);
    }

    @Test
    public void shouldSequencePartsFromXmlVdb() throws Exception {
        // print = true;
        uploadFile("sequencers/teiid/vdb/PartsFromXml.vdb", "/files/");
        waitUntilSequencingFinishes();
        Thread.sleep(200); // wait a bit while the new content is indexed

        // Find the sequenced node ...
        Node vdb = assertNode("/sequenced/teiid/vdbs/PartsFromXml", "vdb:virtualDatabase", "mix:referenceable", "mode:derived");
        printSubgraph(vdb);
        printQuery("SELECT * FROM [vdb:virtualDatabase]", 1);
        printQuery("SELECT * FROM [vdb:model]", 2);
        // The 'PartsView' virtual model contains a single table ...
        printQuery("SELECT * FROM [relational:table]", 1);
        printQuery("SELECT * FROM [relational:column]", 6);
        printQuery("SELECT * FROM [relational:primaryKey]", 0);
        printQuery("SELECT * FROM [relational:foreignKey]", 0);
        // The 'PartsData' physical model contains a single procedure ...
        printQuery("SELECT * FROM [relational:procedure]", 1);
        printQuery("SELECT * FROM [relational:procedureParameter]", 1);
        printQuery("SELECT * FROM [relational:procedureResult]", 1);
    }

    @Test
    public void shouldSequenceYahooUdfTestVdb() throws Exception {
        // print = true;
        uploadFile("sequencers/teiid/vdb/YahooUdfTest.vdb", "/files/");
        waitUntilSequencingFinishes();
        Thread.sleep(200); // wait a bit while the new content is indexed

        // Find the sequenced node ...
        Node vdb = assertNode("/sequenced/teiid/vdbs/YahooUdfTest", "vdb:virtualDatabase", "mix:referenceable", "mode:derived");
        printSubgraph(vdb);
        printQuery("SELECT * FROM [vdb:virtualDatabase]", 1);
        printQuery("SELECT * FROM [vdb:model]", 4);
        printQuery("SELECT * FROM [relational:table]", 7);
        printQuery("SELECT * FROM [relational:column]", 55);
        printQuery("SELECT * FROM [relational:primaryKey]", 6);
        printQuery("SELECT * FROM [relational:foreignKey]", 1);
        printQuery("SELECT * FROM [relational:procedure]", 0);
        printQuery("SELECT * FROM [relational:procedureParameter]", 0);
        printQuery("SELECT * FROM [relational:procedureResult]", 0);
    }

    @FixFor( "MODE-860" )
    @Test
    public void shouldFindVdbsUsingQueryWithMultipleVariables() throws Exception {
        String[] vdbFiles = {"YahooUdfTest.vdb", "qe.vdb", "qe.2.vdb", "qe.3.vdb", "qe.4.vdb", "PartsFromXml.vdb"};
        uploadVdbs("/files/", vdbFiles);
        Thread.sleep(1000); // wait a bit while the new content is indexed

        // Print out the top level of the VDBs ...
        Node files = assertNode("/files");
        // print = true;
        printSubgraph(files, 5);

        // Wait for the sequencing to finish ...
        waitUntilSequencedNodesIs(vdbFiles.length, 20);
        session.refresh(false);

        // Print out the top level of the VDBs ...
        // print = true;
        Node vdbs = assertNode("/sequenced");
        // print = true;
        printSubgraph(vdbs, 4);

        // Query for the VDBs by path glob ...
        printQuery("SELECT * FROM [nt:file] WHERE PATH() LIKE '/file*/*.vdb'", 6);
        printQuery("SELECT * FROM [nt:file] WHERE PATH() LIKE '*/Yah*.vdb'", 1);
        printQuery("SELECT * FROM [nt:file] WHERE PATH() LIKE '/files/q*.vdb'", 4);
        printQuery("SELECT * FROM [nt:file] WHERE PATH() LIKE '/files/q*.2.vdb'", 1);
        printQuery("SELECT file.*,content.* FROM [nt:file] AS file JOIN [nt:resource] AS content ON ISCHILDNODE(content,file) WHERE PATH(file) LIKE '/files/q*.2.vdb'",
                   1);
        printQuery("SELECT file.*,content.[jcr:lastModified],content.[jcr:lastModifiedBy] FROM [nt:file] AS file JOIN [nt:resource] AS content ON ISCHILDNODE(content,file) WHERE PATH(file) LIKE '/files/q*.2.vdb'",
                   1);

        // Query for the VDBs by path glob using variable ...
        printQuery("SELECT * FROM [nt:file] WHERE PATH() LIKE $path", 1, var("path", "/files/q*.2.vdb"));

        // Query for the VDBs by version ...
        printQuery("SELECT [jcr:primaryType],[jcr:created],[jcr:createdBy] FROM [nt:file] WHERE PATH() LIKE $path",
                   1,
                   var("path", "/files/q*.2.vdb"));

        // Query for the VDBs by version range (which is actually on the derived/sequenced information) ...
        printQuery("SELECT [jcr:primaryType],[jcr:created],[jcr:createdBy] FROM [nt:file] WHERE PATH() IN "
                   + "( SELECT [vdb:originalFile] FROM [vdb:virtualDatabase] WHERE [vdb:version] BETWEEN $minVersion AND $maxVersion )",
                   3,
                   var("minVersion", "2"),
                   var("maxVersion", "5"));

        // Query for the VDBs by version range and description (which is actually on the derived/sequenced information) ...
        printQuery("SELECT [jcr:primaryType],[jcr:created],[jcr:createdBy] FROM [nt:file] WHERE PATH() IN "
                   + "( SELECT [vdb:originalFile] FROM [vdb:virtualDatabase] "
                   + "WHERE [vdb:version] <= $maxVersion AND CONTAINS([vdb:description],'xml OR maybe'))",
                   4,
                   var("description", "*"),
                   var("maxVersion", "3"));
        printQuery("SELECT [jcr:primaryType],[jcr:created],[jcr:createdBy] FROM [nt:file] WHERE PATH() IN "
                   + "( SELECT [vdb:originalFile] FROM [vdb:virtualDatabase] "
                   + "WHERE [vdb:version] <= $maxVersion AND CONTAINS([vdb:description],'xml maybe'))",
                   1,
                   var("description", "*"),
                   var("maxVersion", "3"));
        printQuery("SELECT [jcr:primaryType],[jcr:created],[jcr:createdBy] FROM [nt:file] WHERE PATH() IN "
                   + "( SELECT [vdb:originalFile] FROM [vdb:virtualDatabase] "
                   + "WHERE [vdb:version] <= $maxVersion AND CONTAINS([vdb:description],'xml OR xml maybe'))",
                   4,
                   var("description", "*"),
                   var("maxVersion", "3"));
    }

    protected void uploadVdbs( String destinationPath,
                               String... resourcePaths ) throws Exception {
        for (String resourcePath : resourcePaths) {
            uploadFile("sequencers/teiid/vdb/" + resourcePath, destinationPath);
        }
    }

}
