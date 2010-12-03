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
import javax.jcr.Node;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ZipSequencerIntegrationTest extends AbstractSequencerTest {

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.test.integration.sequencer.AbstractSequencerTest#getResourcePathToConfigurationFile()
     */
    @Override
    protected String getResourcePathToConfigurationFile() {
        return "config/configRepositoryForZipSequencing.xml";
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
    public void shouldStartEngineWithRegisteredZipNodeTypes() throws Exception {
        assertNodeType("zip:content", false, false, true, false, null, 2, 0, "nt:unstructured", "mix:mimeType");
    }

    @Test
    public void shouldSequenceZipFile() throws Exception {
        // print = true;
        uploadFile("sequencers/zip/test-files.zip", "/files/");
        waitUntilSequencedNodesIs(1);
        Thread.sleep(1000); // wait a bit while the new content is indexed

        // Find the sequenced node ...
        String path = "/sequenced/zip/test-files.zip";
        Node zipped = assertNode(path, "zip:file","mode:derived");
        Node file1 = assertNode(path + "/MODE-966-fix.patch", "nt:file");
        Node data1 = assertNode(path + "/MODE-966-fix.patch/jcr:content", "nt:resource");
        Node fold1 = assertNode(path + "/testFolder", "nt:folder");
        Node file2 = assertNode(path + "/testFolder/MODE-962-fix.patch", "nt:file");
        Node data2 = assertNode(path + "/testFolder/MODE-962-fix.patch/jcr:content", "nt:resource");
        Node fold3 = assertNode(path + "/testFolder/testInnerFolder", "nt:folder");
        Node file4 = assertNode(path + "/testFolder/testInnerFolder/MODE-960-fix.patch", "nt:file");
        Node data4 = assertNode(path + "/testFolder/testInnerFolder/MODE-960-fix.patch/jcr:content", "nt:resource");
        Node file5 = assertNode(path + "/testFolder/testInnerFolder/MODE-960-fix2.patch", "nt:file");
        Node data5 = assertNode(path + "/testFolder/testInnerFolder/MODE-960-fix2.patch/jcr:content", "nt:resource");
        assertThat(file1, is(notNullValue()));
        assertThat(data1, is(notNullValue()));
        assertThat(file2, is(notNullValue()));
        assertThat(data2, is(notNullValue()));
        assertThat(fold1, is(notNullValue()));
        assertThat(fold3, is(notNullValue()));
        assertThat(file4, is(notNullValue()));
        assertThat(data4, is(notNullValue()));
        assertThat(file5, is(notNullValue()));
        assertThat(data5, is(notNullValue()));
        printSubgraph(zipped);

        printQuery("SELECT * FROM [nt:file]", 5);
        printQuery("SELECT * FROM [nt:folder]", 4);
        printQuery("SELECT * FROM [zip:file]", 1);
    }

    @Test
    public void shouldSequenceZipFileBelowSequencedPath() throws Exception {
        // print = true;
        uploadFile("sequencers/zip/test-files.zip", "/files/a/b");
        waitUntilSequencedNodesIs(1);
        Thread.sleep(1000); // wait a bit while the new content is indexed

        // Find the sequenced node ...
        String path = "/sequenced/zip/a/b/test-files.zip";
        Node zipped = assertNode(path, "zip:file","mode:derived");
        Node file1 = assertNode(path + "/MODE-966-fix.patch", "nt:file");
        Node data1 = assertNode(path + "/MODE-966-fix.patch/jcr:content", "nt:resource");
        Node fold1 = assertNode(path + "/testFolder", "nt:folder");
        Node file2 = assertNode(path + "/testFolder/MODE-962-fix.patch", "nt:file");
        Node data2 = assertNode(path + "/testFolder/MODE-962-fix.patch/jcr:content", "nt:resource");
        Node fold3 = assertNode(path + "/testFolder/testInnerFolder", "nt:folder");
        Node file4 = assertNode(path + "/testFolder/testInnerFolder/MODE-960-fix.patch", "nt:file");
        Node data4 = assertNode(path + "/testFolder/testInnerFolder/MODE-960-fix.patch/jcr:content", "nt:resource");
        Node file5 = assertNode(path + "/testFolder/testInnerFolder/MODE-960-fix2.patch", "nt:file");
        Node data5 = assertNode(path + "/testFolder/testInnerFolder/MODE-960-fix2.patch/jcr:content", "nt:resource");
        assertThat(file1, is(notNullValue()));
        assertThat(data1, is(notNullValue()));
        assertThat(file2, is(notNullValue()));
        assertThat(data2, is(notNullValue()));
        assertThat(fold1, is(notNullValue()));
        assertThat(fold3, is(notNullValue()));
        assertThat(file4, is(notNullValue()));
        assertThat(data4, is(notNullValue()));
        assertThat(file5, is(notNullValue()));
        assertThat(data5, is(notNullValue()));
        printSubgraph(zipped);
        printSubgraph(assertNode("/sequenced/zip"));

        printQuery("SELECT * FROM [nt:file]", 5);
        printQuery("SELECT * FROM [nt:folder]", 6); // 2 extra folders
        printQuery("SELECT * FROM [zip:file]", 1);
    }
}
