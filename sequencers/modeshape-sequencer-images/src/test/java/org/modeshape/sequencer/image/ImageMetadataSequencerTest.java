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
package org.modeshape.sequencer.image;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import static org.junit.Assert.*;
import org.junit.Test;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.sequencer.AbstractSequencerTest;

/**
 * Unit test for {@link ImageMetadataSequencer}. This test runs a minimal, in memory MS repository, which has two image sequencers
 * configured: one which writes sequenced output to the same location as the input and one which writes the output to another,
 * custom location.
 *
 * @author Randall Hauch
 * @author John Verhaeg
 * @author Horia Chiorean
 */
public class ImageMetadataSequencerTest extends AbstractSequencerTest {

    @Test
    public void shouldGenerateMetadataForJpegImageFiles() throws Exception {
        String filename = "caution.jpg";
        Node imageNode = createNodeWithContentFromFile(filename, filename);

        Node sequencedNodeDifferentLocation = getOutputNode(rootNode, "sequenced/images/" + filename);
        assertMetaDataProperties(sequencedNodeDifferentLocation, "image/jpeg", "jpeg", 48, 48, 24, false, 1, 72, 72, 0.666667, 0.666667);

        Node sequencedNodeSameLocation = getOutputNode(imageNode, ImageMetadataLexicon.METADATA_NODE);
        assertMetaDataProperties(sequencedNodeSameLocation, "image/jpeg", "jpeg", 48, 48, 24, false, 1, 72, 72, 0.666667, 0.666667);
    }

    @Test
    public void shouldGenerateMetadataForPngImageFiles() throws Exception {
        String filename = "caution.png";
        Node imageNode = createNodeWithContentFromFile(filename, filename);

        Node sequencedNodeDifferentLocation = getOutputNode(rootNode, "sequenced/images/" + filename);
        assertMetaDataProperties(sequencedNodeDifferentLocation, "image/png", "png", 48, 48, 24, false, 1, -1, -1, -1, -1);

        Node sequencedNodeSameLocation = getOutputNode(imageNode, ImageMetadataLexicon.METADATA_NODE);
        assertMetaDataProperties(sequencedNodeSameLocation, "image/png", "png", 48, 48, 24, false, 1, -1, -1, -1, -1);
    }

    @Test
    public void shouldGenerateMetadataForGifImageFiles() throws Exception {
        String filename = "caution.gif";
        Node imageNode = createNodeWithContentFromFile(filename, filename);

        Node sequencedNodeDifferentLocation = getOutputNode(rootNode, "sequenced/images/" + filename);
        assertMetaDataProperties(sequencedNodeDifferentLocation, "image/gif", "gif", 48, 48, 8, false, 1, -1, -1, -1, -1);

        Node sequencedNodeSameLocation = getOutputNode(imageNode, ImageMetadataLexicon.METADATA_NODE);
        assertMetaDataProperties(sequencedNodeSameLocation, "image/gif", "gif", 48, 48, 8, false, 1, -1, -1, -1, -1);
    }

    @Test
    public void shouldGenerateNoMetadataforPictImageFiles() throws Exception {
        String filename = "caution.pict";
        Node imageNode = createNodeWithContentFromFile(filename, filename);
        assertNull(getOutputNode(rootNode, "sequenced/images/" + filename, 1));
        assertNull(getOutputNode(imageNode, ImageMetadataLexicon.METADATA_NODE, 1));
    }   

    private void assertMetaDataProperties( Node metadataNode, String mimeType, String format, int width, int height, int bitsPerPixel,
                                           boolean progressive, int numberOfImages, int physicalWidthDpi, int physicalHeightDpi,
                                           double physicalWidthInches, double physicalHeightInches ) throws RepositoryException {
        assertNotNull(metadataNode);

        assertEquals(ImageMetadataLexicon.METADATA_NODE, metadataNode.getProperty(JcrConstants.JCR_PRIMARY_TYPE).getString());
        assertEquals(mimeType, metadataNode.getProperty(JcrConstants.JCR_MIME_TYPE).getString());
        assertEquals(format, metadataNode.getProperty(ImageMetadataLexicon.FORMAT_NAME).getString().toLowerCase());
        assertEquals(width, metadataNode.getProperty(ImageMetadataLexicon.WIDTH).getLong());
        assertEquals(height, metadataNode.getProperty(ImageMetadataLexicon.HEIGHT).getLong());
        assertEquals(bitsPerPixel, metadataNode.getProperty(ImageMetadataLexicon.BITS_PER_PIXEL).getLong());
        assertEquals(progressive, metadataNode.getProperty(ImageMetadataLexicon.PROGRESSIVE).getBoolean());
        assertEquals(numberOfImages, metadataNode.getProperty(ImageMetadataLexicon.NUMBER_OF_IMAGES).getLong());
        assertEquals(physicalWidthDpi, metadataNode.getProperty(ImageMetadataLexicon.PHYSICAL_WIDTH_DPI).getLong());
        assertEquals(physicalHeightDpi, metadataNode.getProperty(ImageMetadataLexicon.PHYSICAL_HEIGHT_DPI).getLong());
        assertEquals(physicalWidthInches, metadataNode.getProperty(ImageMetadataLexicon.PHYSICAL_WIDTH_INCHES).getDouble(), 0.0001d);
        assertEquals(physicalHeightInches, metadataNode.getProperty(ImageMetadataLexicon.PHYSICAL_HEIGHT_INCHES).getDouble(), 0.0001d);
    }
}
