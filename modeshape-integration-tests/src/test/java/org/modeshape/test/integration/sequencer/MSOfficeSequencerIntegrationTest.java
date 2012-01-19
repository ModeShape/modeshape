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
import javax.jcr.RepositoryException;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import org.junit.After;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MSOfficeSequencerIntegrationTest extends AbstractSequencerTest {
    /**
     * @see org.modeshape.test.ModeShapeUnitTest#getPathToDefaultConfiguration()
     */
    @Override
    protected String getPathToDefaultConfiguration() {
        return "config/configRepositoryForTextExtraction.xml";
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
    public void shouldStartEngineWithRegisteredMSOfficeNodeTypes() throws Exception {
        assertNodeType("msoffice:metadata", false, false, true, false, null, 3, 18, "nt:unstructured", "mix:mimeType");
    }

    @Test
    public void shouldSequenceWordDocument() throws Exception {
        uploadAndAssertSequencedNode("word.doc");
    }

    @Test
    public void shouldSequenceExcelDocument() throws Exception {
        uploadAndAssertSequencedNode("excel.xls");
    }

    @Test
    public void shouldSequencePowerpointDocument() throws Exception {
        uploadAndAssertSequencedNode("powerpoint.ppt");
    }

    private void uploadAndAssertSequencedNode(String fileName) throws RepositoryException, IOException, InterruptedException {
        // print = true;
        uploadFile("sequencers/msoffice/" +  fileName, "/files/");

        // Thread.sleep(10 * 1000);
        waitUntilSequencingFinishes();

        Node sequenced = session().getNode("/sequenced");
        assertThat(sequenced, is(notNullValue()));
        printSubgraph(sequenced);

        // Find the sequenced node ...
        Node node = waitUntilSequencedNodeIsAvailable("/sequenced/msoffice/" + fileName, "nt:unstructured");
        assertNotNull(node);
        printSubgraph(node);
        SequencedNodeValidator.validateSequencedNodeType(node);
    }
}
