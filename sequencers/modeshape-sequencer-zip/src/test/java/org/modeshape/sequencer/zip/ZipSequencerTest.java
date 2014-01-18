/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.modeshape.sequencer.zip;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.io.EOFException;
import java.io.IOException;
import java.util.Map;
import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import org.junit.Test;
import org.modeshape.common.util.IoUtil;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.api.observation.Event;
import org.modeshape.jcr.sequencer.AbstractSequencerTest;

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

        Node outputZip = getOutputNode(rootNode, "zip/" + filename);
        assertNotNull(outputZip);
        assertEquals(ZipLexicon.FILE, outputZip.getPrimaryNodeType().getName());

        assertEquals(2, outputZip.getNodes().getSize());
        assertFile(outputZip, "test1.txt", "This is a test content of file 1\n");

        Node folder = outputZip.getNode("test subfolder");
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
        assertEquals("text/plain", fileContent.getProperty(JcrConstants.JCR_MIME_TYPE).getString());
        Binary fileData = fileContent.getProperty(JcrConstants.JCR_DATA).getBinary();
        assertNotNull(fileData);
        if (expectedContent != null) {
            assertEquals(expectedContent, IoUtil.read(fileData.getStream()));
        }
    }

    @Test
    public void shouldSequenceZip2() throws Exception {
        String filename = "test-files.zip";
        Node parent = createNodeWithContentFromFile(filename, filename);
        Node outputNode = parent.getNode("jcr:content");

        Node outputZip = getOutputNode(rootNode, "zip/" + filename);
        assertNotNull(outputZip);
        assertEquals(ZipLexicon.FILE, outputZip.getPrimaryNodeType().getName());
        assertSequencingEventInfo(outputNode, session.getUserID(), "ZIP sequencer", outputNode.getPath(), "/zip");

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

    @Test
    public void shouldFailIfZipCorrupted() throws Throwable {
        String filename = "corrupt.zip";
        Node parent = createNodeWithContentFromFile(filename, filename);
        Node outputNode = parent.getNode("jcr:content");
        expectSequencingFailure(outputNode);

        Map<?, ?> sequencingEventInfo = assertSequencingEventInfo(outputNode,
                                                                  session.getUserID(),
                                                                  "ZIP sequencer",
                                                                  outputNode.getPath(),
                                                                  "/zip");
        assertEquals(EOFException.class.getName(), sequencingEventInfo.get(Event.Sequencing.SEQUENCING_FAILURE_CAUSE)
                                                                      .getClass()
                                                                      .getName());
    }

}
