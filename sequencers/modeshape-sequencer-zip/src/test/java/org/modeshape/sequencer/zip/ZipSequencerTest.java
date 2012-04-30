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

package org.modeshape.sequencer.zip;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import org.modeshape.common.util.IoUtil;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.api.mimetype.MimeTypeConstants;
import org.modeshape.jcr.sequencer.AbstractSequencerTest;
import java.io.EOFException;
import java.io.IOException;

/**
 * Unit test for {@link ZipSequencer}
 *
 * @author Horia Chiorean
 */
public class ZipSequencerTest extends AbstractSequencerTest {

    @Test
    public void shouldSequenceZip1() throws Exception {
        String filename = "testzip.zip";
        createNodeWithContentFromFile(filename, filename);

        Node sequencedZip = getSequencedNode(rootNode, "zip/" + filename);
        assertNotNull(sequencedZip);
        assertEquals(ZipLexicon.FILE, sequencedZip.getPrimaryNodeType().getName());

        assertEquals(2, sequencedZip.getNodes().getSize());
        assertFile(sequencedZip, "test1.txt", "This is a test content of file 1\n");

        Node folder = sequencedZip.getNode("test subfolder");
        assertEquals(1, folder.getNodes().getSize());
        assertEquals(JcrConstants.NT_FOLDER, folder.getPrimaryNodeType().getName());

        assertFile(folder, "test2.txt", "This is a test content of file2\n");
    }


    private void assertFile( Node parentNode,
                             String relativePath,
                             String expectedContent ) throws RepositoryException, IOException {
        Node file = parentNode.getNode(relativePath);
        assertNotNull(file);
        assertEquals(1, file.getNodes().getSize());
        assertEquals(JcrConstants.NT_FILE, file.getPrimaryNodeType().getName());
        Node fileContent = file.getNode(JcrConstants.JCR_CONTENT);
        assertNotNull(fileContent);
        assertEquals(MimeTypeConstants.TEXT_PLAIN, fileContent.getProperty(JcrConstants.JCR_MIME_TYPE).getString());
        Binary fileData = fileContent.getProperty(JcrConstants.JCR_DATA).getBinary();
        assertNotNull(fileData);
        if (expectedContent != null) {
            assertEquals(expectedContent, IoUtil.read(fileData.getStream()));
        }
    }

    @Test
    public void shouldSequenceZip2() throws Exception {
        String filename = "test-files.zip";
        createNodeWithContentFromFile(filename, filename);

        Node sequencedZip = getSequencedNode(rootNode, "zip/" + filename);
        assertNotNull(sequencedZip);
        assertEquals(ZipLexicon.FILE, sequencedZip.getPrimaryNodeType().getName());

        // Find the sequenced node ...
        String path = "/zip/test-files.zip";       
        assertNode(path + "/MODE-966-fix.patch", JcrConstants.NT_FILE);
        assertNode(path + "/MODE-966-fix.patch/jcr:content", JcrConstants.NT_RESOURCE);
        assertNode(path + "/testFolder", JcrConstants.NT_FOLDER);
        assertNode(path + "/testFolder/MODE-962-fix.patch", JcrConstants.NT_FILE);
        assertNode(path + "/testFolder/MODE-962-fix.patch/jcr:content", JcrConstants.NT_RESOURCE);
        assertNode(path + "/testFolder/testInnerFolder", JcrConstants.NT_FOLDER);
        assertNode(path + "/testFolder/testInnerFolder/MODE-960-fix.patch", JcrConstants.NT_FILE);
        assertNode(path + "/testFolder/testInnerFolder/MODE-960-fix.patch/jcr:content", JcrConstants.NT_RESOURCE);
        assertNode(path + "/testFolder/testInnerFolder/MODE-960-fix2.patch", JcrConstants.NT_FILE);
        assertNode(path + "/testFolder/testInnerFolder/MODE-960-fix2.patch/jcr:content", JcrConstants.NT_RESOURCE);      
    }

    @Test(expected = EOFException.class)
    public void shouldFailIfZipCorrupted() throws Throwable {
        String filename = "corrupt.zip";
        Node parent = createNodeWithContentFromFile(filename, filename);
        expectSequencingFailure(parent.getNode("jcr:content"));
    }
}
