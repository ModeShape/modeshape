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
package org.modeshape.sequencer.mp3;

import static org.junit.Assert.assertEquals;
import static org.modeshape.sequencer.mp3.Mp3MetadataLexicon.ALBUM;
import static org.modeshape.sequencer.mp3.Mp3MetadataLexicon.AUTHOR;
import static org.modeshape.sequencer.mp3.Mp3MetadataLexicon.COMMENT;
import static org.modeshape.sequencer.mp3.Mp3MetadataLexicon.METADATA_NODE;
import static org.modeshape.sequencer.mp3.Mp3MetadataLexicon.TITLE;
import static org.modeshape.sequencer.mp3.Mp3MetadataLexicon.YEAR;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import org.junit.Test;
import org.modeshape.jcr.sequencer.AbstractSequencerTest;

/**
 * Unit test for {@link Mp3MetadataSequencer}
 *
 * @author Horia Chiorean
 */
public class Mp3SequencerTest extends AbstractSequencerTest {
    
    @Test
    public void shouldSequenceMp3() throws Exception {
        createNodeWithContentFromFile("sample.mp3", "sample1.mp3");
        
        Node sequencedNodeSameLocation = getOutputNode(rootNode, "sample.mp3/" + Mp3MetadataLexicon.METADATA_NODE);
        assertSequencedMp3(sequencedNodeSameLocation);

        Node sequencedNodeDifferentLocation = getOutputNode(rootNode, "mp3s/sample.mp3");
        assertSequencedMp3(sequencedNodeDifferentLocation);
    }

    private void assertSequencedMp3( Node sequencedNode ) throws RepositoryException {
        assertEquals(METADATA_NODE, sequencedNode.getPrimaryNodeType().getName());
        assertEquals("Badwater Slim Performs Live", sequencedNode.getProperty(ALBUM).getString());
        assertEquals("Badwater Slim", sequencedNode.getProperty(AUTHOR).getString());
        assertEquals("This is a test audio file.", sequencedNode.getProperty(COMMENT).getString());
        assertEquals("Sample MP3", sequencedNode.getProperty(TITLE).getString());
        assertEquals("2008", sequencedNode.getProperty(YEAR).getString());
    }
}
