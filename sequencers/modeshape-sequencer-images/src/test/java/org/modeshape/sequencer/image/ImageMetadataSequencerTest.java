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
package org.modeshape.sequencer.image;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import org.junit.Test;
import org.modeshape.common.FixFor;
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
        try {
            getOutputNode(rootNode, "sequenced/images/" + filename, 1);
        } catch (AssertionError e) {
            //expected
        }
        try {
            getOutputNode(imageNode, ImageMetadataLexicon.METADATA_NODE, 1);
        } catch (AssertionError e) {
            //expected
        }
    }

    @Test
    @FixFor( "MODE-2021")
    public void shouldExtractExifInformationFromJpeg() throws Exception {
        String filename = "image_with_exif.jpg";
        createNodeWithContentFromFile(filename, filename);
        Node sequencedNode = getOutputNode(rootNode, "sequenced/images/" + filename);
        assertNotNull(sequencedNode);

        Node exifNode = sequencedNode.getNode(ImageMetadataLexicon.EXIF_NODE);
        assertEquals("Top, left side (Horizontal / normal)", exifNode.getProperty(ImageMetadataLexicon.ORIENTATION).getString());
        assertEquals("NIKON", exifNode.getProperty(ImageMetadataLexicon.MAKE).getString());
        assertEquals("E4600", exifNode.getProperty(ImageMetadataLexicon.MODEL).getString());
        assertEquals(300.0, exifNode.getProperty(ImageMetadataLexicon.RESOLUTION_X).getDouble(), 0);
        assertEquals(300.0, exifNode.getProperty(ImageMetadataLexicon.RESOLUTION_Y).getDouble(), 0);
        assertEquals("Inch", exifNode.getProperty(ImageMetadataLexicon.UNIT).getString());
        assertEquals("2008:05:08 14:54:46", exifNode.getProperty(ImageMetadataLexicon.DATETIME).getString());
        assertEquals("Adobe Photoshop CS3 Windows", exifNode.getProperty(ImageMetadataLexicon.SOFTWARE).getString());
    }

    @Test
    @FixFor( "MODE-1847" )
    public void shouldSequenceTIFFiles() throws Exception {
        String filename = "tif_image.tif";
        createNodeWithContentFromFile(filename, filename);
        Node sequencedNodeDifferentLocation = getOutputNode(rootNode, "sequenced/images/" + filename);
        assertEquals(ImageMetadataLexicon.METADATA_NODE, sequencedNodeDifferentLocation.getProperty(JcrConstants.JCR_PRIMARY_TYPE).getString());
        assertEquals("image/tiff", sequencedNodeDifferentLocation.getProperty(JcrConstants.JCR_MIME_TYPE).getString());
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
