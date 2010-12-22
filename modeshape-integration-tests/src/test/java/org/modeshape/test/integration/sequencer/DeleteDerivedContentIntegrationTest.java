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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.util.concurrent.TimeUnit;
import javax.jcr.Node;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DeleteDerivedContentIntegrationTest extends AbstractSequencerTest {

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.test.integration.sequencer.AbstractSequencerTest#getResourcePathToConfigurationFile()
     */
    @Override
    protected String getResourcePathToConfigurationFile() {
        return "config/configRepositoryForCndSequencing.xml";
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
    public void shouldDeleteDerivedContentWhenOriginalFileIsDeleted() throws Exception {
        // print = true;
        for (int i = 0; i != 2; ++i) {
            uploadFile("sequencers/cnd/jsr_283_builtins.cnd", "/files/");
            waitUntilSequencedNodesIs(1 * (i + 1));
            Thread.sleep(200); // wait a bit while the new content is indexed
            // printSubgraph(assertNode("/"));

            // Find the sequenced node ...
            String derivedPath = "/sequenced/cnd/jsr_283_builtins.cnd";
            Node cnd = assertNode(derivedPath, "nt:unstructured");
            printSubgraph(cnd);

            Node file1 = assertNode(derivedPath + "/nt:activity", "nt:nodeType", "mode:derived");
            assertThat(file1, is(notNullValue()));

            printQuery("SELECT * FROM [mode:derived]", 34);
            // printQuery("SELECT * FROM [nt:nodeType]", 34);
            // printQuery("SELECT * FROM [nt:propertyDefinition]", 86);
            // printQuery("SELECT * FROM [nt:childNodeDefinition]", 10);

            // Register a delete listener ...
            DeleteListener listener = registerListenerForDeletes();

            // Now delete the original node ...
            String filePath = "/files/jsr_283_builtins.cnd";
            assertNode(filePath);
            session().removeItem(filePath);
            session().save();

            // And wait for the events signalling the original and derived content were deleted.
            // The CND sequencer outputs multiple node type definitions, not a single parent node under which these nodes appear.
            // Therefore, we need to see delete events for each node definition.
            listener.waitForDeleted(5,
                                    TimeUnit.SECONDS,
                                    filePath,
                                    derivedPath + "/nt:activity",
                                    derivedPath + "/nt:base",
                                    derivedPath + "/mode:defined",
                                    derivedPath + "/mix:referenceable",
                                    derivedPath + "/nt:query");

            printQuery("SELECT * FROM [mode:derived]", 0);
        }
    }
}
