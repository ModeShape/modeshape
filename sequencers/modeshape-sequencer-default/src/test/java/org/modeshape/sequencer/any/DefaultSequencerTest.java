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
package org.modeshape.sequencer.any;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import static junit.framework.Assert.assertEquals;
import org.junit.Test;
import org.modeshape.jcr.sequencer.AbstractSequencerTest;
import org.modeshape.sequencer.any.DefaultMetadataLexicon;
import org.modeshape.sequencer.any.DefaultSequencer;
import org.modeshape.sequencer.sramp.SrampLexicon;

import static org.modeshape.sequencer.any.DefaultMetadataLexicon.*;

/**
 * Unit test for {@link DefaultSequencer}
 *
 * @author Kurt Stam
 */
public class DefaultSequencerTest extends AbstractSequencerTest {
    
    @Test
    public void shouldSequencePdf() throws Exception {
        createNodeWithContentFromFile("s-ramp-press-release.any", "s-ramp-press-release.pdf");
        
        Node sequencedNodeSameLocation = getOutputNode(rootNode, "s-ramp-press-release.any/" + DefaultMetadataLexicon.METADATA_NODE);
        assertSequencedPdf(sequencedNodeSameLocation);

        Node sequencedNodeDifferentLocation = getOutputNode(rootNode, "anys/s-ramp-press-release.any");
        assertSequencedPdf(sequencedNodeDifferentLocation);
    }

    private void assertSequencedPdf( Node sequencedNode ) throws RepositoryException {
        assertEquals(METADATA_NODE, sequencedNode.getPrimaryNodeType().getName());
        assertEquals(19456l, sequencedNode.getProperty(SrampLexicon.CONTENT_SIZE).getLong());
    }


}
