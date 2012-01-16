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
 * Lesser General Public License for more details
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org
 */
package org.modeshape.sequencer.mp3;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import static junit.framework.Assert.assertEquals;
import org.junit.Test;
import org.modeshape.jcr.sequencer.AbstractSequencerTest;
import static org.modeshape.sequencer.mp3.Mp3MetadataLexicon.*;

/**
 * Unit test for {@link Mp3MetadataSequencer}
 *
 * @author Horia Chiorean
 */
public class Mp3SequencerTest extends AbstractSequencerTest {
    
    @Test
    public void shouldSequenceMp3() throws Exception {
        createNodeWithContentFromFile("sample.mp3", "sample1.mp3");
        
        Node sequencedNodeSameLocation = getSequencedNode(rootNode, "sample.mp3/" + Mp3MetadataLexicon.METADATA_NODE);
        assertSequencedMp3(sequencedNodeSameLocation);

        Node sequencedNodeDifferentLocation = getSequencedNode(rootNode, "mp3s/sample.mp3");
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
