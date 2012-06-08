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
package org.modeshape.test.integration.sequencer.ddl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.test.integration.sequencer.AbstractSequencerTest;

public class DdlSequencer2IntegrationTest extends AbstractSequencerTest {

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.test.ModeShapeUnitTest#getPathToDefaultConfiguration()
     */
    @Override
    protected String getPathToDefaultConfiguration() {
        return "config/configRepositoryForDdlSequencing.xml";
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

    @FixFor( "MODE-1073" )
    @Test
    public void shouldNotCreateExtraIntermediateNodesWhenUploadingAndSequencingMultipleFiles() throws Exception {
        // print = true;
        uploadFile("org/modeshape/test/integration/sequencer/ddl/create_schema.ddl", "/files/a/b");
        uploadFile("org/modeshape/test/integration/sequencer/ddl/grant_test_statements.ddl", "/files/a/b");

        waitUntilSequencedNodeIsAvailable("/files", "nt:folder", "mode:publishArea");
        assertNode("/files/a", "nt:folder");
        assertNode("/files/a/b", "nt:folder");
        assertNode("/files/a/b/create_schema.ddl", "nt:file");
        assertNode("/files/a/b/create_schema.ddl/jcr:content", "nt:resource");
        assertNode("/files/a/b/grant_test_statements.ddl");
        assertNode("/files/a/b/grant_test_statements.ddl/jcr:content", "nt:resource");
        assertNode("/sequenced/ddl", "nt:unstructured");
        assertNode("/sequenced/ddl/a", "nt:unstructured");
        assertNode("/sequenced/ddl/a/b", "nt:unstructured");
        assertNode("/sequenced/ddl/a/b/create_schema.ddl");
        assertNode("/sequenced/ddl/a/b/grant_test_statements.ddl");
        assertNoNode("/sequenced/ddl[2]");
        assertNoNode("/sequenced/ddl/a[2]");
        assertNoNode("/sequenced/ddl/a/b[2]");
    }

}
