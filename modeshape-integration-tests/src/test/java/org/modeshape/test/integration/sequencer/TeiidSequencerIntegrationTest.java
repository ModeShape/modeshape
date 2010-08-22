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

    @Before
    @Override
    public void beforeEach() throws Exception {
        super.beforeEach();
    }

    @After
    @Override
    public void afterEach() {
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

        // Find the sequenced node ...
        Node vdb = assertNode("/sequenced/teiid/vdbs/qe", "vdb:virtualDatabase", "mix:referenceable");
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

        // Find the sequenced node ...
        Node vdb = assertNode("/sequenced/teiid/vdbs/my/favorites/qe", "vdb:virtualDatabase", "mix:referenceable");
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

        // Find the sequenced node ...
        Node vdb = assertNode("/sequenced/teiid/vdbs/PartsFromXml", "vdb:virtualDatabase", "mix:referenceable");
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

        // Find the sequenced node ...
        Node vdb = assertNode("/sequenced/teiid/vdbs/YahooUdfTest", "vdb:virtualDatabase", "mix:referenceable");
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

}
