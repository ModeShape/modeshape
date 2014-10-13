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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.jcr.Binary;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.api.sequencer.Sequencer;
import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Descriptor;
import com.drew.metadata.exif.ExifIFD0Directory;

/**
 * A sequencer that processes the binary content of an image file, extracts the metadata for the image, and then writes that image
 * metadata to the repository.
 * <p>
 * This sequencer produces data that corresponds to the following structure:
 * <ul>
 * <li><strong>image:metadata</strong> node of type <code>image:metadata</code>
 * <ul>
 * <li><strong>jcr:mimeType</strong> - optional string property for the mime type of the image</li>
 * <li><strong>jcr:encoding</strong> - optional string property for the encoding of the image</li>
 * <li><strong>image:formatName</strong> - string property for the name of the format</li>
 * <li><strong>image:width</strong> - optional integer property for the image's width in pixels</li>
 * <li><strong>image:height</strong> - optional integer property for the image's height in pixels</li>
 * <li><strong>image:bitsPerPixel</strong> - optional integer property for the number of bits per pixel</li>
 * <li><strong>image:progressive</strong> - optional boolean property specifying whether the image is stored in a progressive
 * (i.e., interlaced) form</li>
 * <li><strong>image:numberOfImages</strong> - optional integer property for the number of images stored in the file; defaults to
 * 1</li>
 * <li><strong>image:physicalWidthDpi</strong> - optional integer property for the physical width of the image in dots per inch</li>
 * <li><strong>image:physicalHeightDpi</strong> - optional integer property for the physical height of the image in dots per inch</li>
 * <li><strong>image:physicalWidthInches</strong> - optional double property for the physical width of the image in inches</li>
 * <li><strong>image:physicalHeightInches</strong> - optional double property for the physical height of the image in inches</li>
 * <li><strong>image:exif</strong> - optional child node which contains additional EXIF information (if available)</li>
 * <ul>
 * <li><strong>image:artist</strong></li>
 * <li><strong>image:copyright</strong></li>
 * <li><strong>image:datetime</strong></li>
 * <li><strong>image:description</strong></li>
 * <li><strong>image:make</strong></li>
 * <li><strong>image:model</strong></li>
 * <li><strong>image:unit</strong></li>
 * <li><strong>image:orientation</strong></li>
 * <li><strong>image:software</strong></li>
 * <li><strong>image:resolution_x</strong></li>
 * <li><strong>image:resolution_y</strong></li>
 * </ul>
 * </ul>
 * </li>
 * </ul>
 * </p>
 */
public class ImageMetadataSequencer extends Sequencer {

    private static final int[] EXIF_TAGS = new int[] { ExifIFD0Directory.TAG_ARTIST, ExifIFD0Directory.TAG_COPYRIGHT, ExifIFD0Directory.TAG_DATETIME,
            ExifIFD0Directory.TAG_IMAGE_DESCRIPTION, ExifIFD0Directory.TAG_MAKE, ExifIFD0Directory.TAG_MODEL, ExifIFD0Directory.TAG_RESOLUTION_UNIT,
            ExifIFD0Directory.TAG_SOFTWARE, ExifIFD0Directory.TAG_X_RESOLUTION, ExifIFD0Directory.TAG_Y_RESOLUTION, ExifIFD0Directory.TAG_ORIENTATION
    };

    private static final String TIFF_FORMAT = "TIFF";
    private static final String[] TIFF_MIME_TYPES = new String[] {"image/tiff", "image/x-tiff", "image/tif", "image/x-tif", "application/tif", "application/x-tif",
            "application/tiff", "application/x-tiff" };
    private static final String IMAGE_TIFF_DEFAULT_MIME_TYPE = TIFF_MIME_TYPES[0];

    @Override
    public boolean execute( Property inputProperty,
                            Node outputNode,
                            Context context ) throws Exception {
        Binary binaryValue = inputProperty.getBinary();
        CheckArg.isNotNull(binaryValue, "binary");
        Node imageNode = getImageMetadataNode(outputNode);
        boolean imageParsedUsingDefaultMetadata = processUsingDefaultMetadata(imageNode, binaryValue);
        return processUsingAdvancedMetadata(imageNode, binaryValue, imageParsedUsingDefaultMetadata);
    }

    private boolean processUsingAdvancedMetadata( Node imageNode,
                                                  Binary binaryValue,
                                                  boolean imageParsedUsingDefaultMetadata ) throws Exception {
        try (InputStream stream = binaryValue.getStream()) {
            Metadata advancedMetadata = ImageMetadataReader.readMetadata(new BufferedInputStream(stream), false);
            ExifIFD0Directory exifIFD0Directory = advancedMetadata.getDirectory(ExifIFD0Directory.class);
            if (exifIFD0Directory == null || !hasTags(exifIFD0Directory, EXIF_TAGS)) {
                if (!imageParsedUsingDefaultMetadata) {
                    getLogger().info("Neither default nor advanced metadata parser can resolve image. Ignoring sequencing");
                }
                getLogger().debug("No relevant IFD0 information found, ignoring EXIF node.");
                return imageParsedUsingDefaultMetadata;
            }

            if (!imageParsedUsingDefaultMetadata) {
                //it's a format not supported by the default metadata and since we're reading the IFD0 descriptor, mark the image
                //as a TIFF
                getLogger().info(
                        "Image has IFD0 block information but is not one of the standard image types, marking it as TIFF");
                imageNode.setProperty(ImageMetadataLexicon.FORMAT_NAME, TIFF_FORMAT);
                imageNode.setProperty(JcrConstants.JCR_MIME_TYPE, IMAGE_TIFF_DEFAULT_MIME_TYPE);
            }

            addEXIFNode(imageNode, exifIFD0Directory);
            return true;
        } catch (Exception e) {
            getLogger().debug(e, "Cannot process image for advanced metadata");
            return imageParsedUsingDefaultMetadata;
        }
    }

    private void addEXIFNode( Node imageNode,
                              ExifIFD0Directory exifIFD0Directory ) throws RepositoryException {
        ExifIFD0Descriptor exifDescriptor = new ExifIFD0Descriptor(exifIFD0Directory);

        Node exifNode = imageNode.addNode(ImageMetadataLexicon.EXIF_NODE, ImageMetadataLexicon.EXIF_NODE);

        setStringIfTagPresent(exifNode, ImageMetadataLexicon.ARTIST, exifIFD0Directory, ExifIFD0Directory.TAG_ARTIST);
        setStringIfTagPresent(exifNode, ImageMetadataLexicon.COPYRIGHT, exifIFD0Directory, ExifIFD0Directory.TAG_COPYRIGHT);
        setStringIfTagPresent(exifNode, ImageMetadataLexicon.DESCRIPTION, exifIFD0Directory, ExifIFD0Directory.TAG_IMAGE_DESCRIPTION);
        setStringIfTagPresent(exifNode, ImageMetadataLexicon.MAKE, exifIFD0Directory, ExifIFD0Directory.TAG_MAKE);
        setStringIfTagPresent(exifNode, ImageMetadataLexicon.MODEL, exifIFD0Directory, ExifIFD0Directory.TAG_MODEL);
        setStringIfTagPresent(exifNode, ImageMetadataLexicon.SOFTWARE, exifIFD0Directory, ExifIFD0Directory.TAG_SOFTWARE);
        setStringIfTagPresent(exifNode, ImageMetadataLexicon.DATETIME, exifIFD0Directory, ExifIFD0Directory.TAG_DATETIME);
        setStringIfNotBlank(exifNode, ImageMetadataLexicon.UNIT, exifDescriptor.getResolutionDescription());
        setStringIfNotBlank(exifNode, ImageMetadataLexicon.ORIENTATION, exifDescriptor.getOrientationDescription());
        setDoubleIfTagPresent(exifNode, ImageMetadataLexicon.RESOLUTION_X, exifIFD0Directory,
                              ExifIFD0Directory.TAG_X_RESOLUTION);
        setDoubleIfTagPresent(exifNode, ImageMetadataLexicon.RESOLUTION_Y, exifIFD0Directory,
                              ExifIFD0Directory.TAG_Y_RESOLUTION);
    }

    private void setStringIfTagPresent(Node node, String propertyName, Directory directory, int tag) throws RepositoryException {
        setStringIfNotBlank(node, propertyName, directory.getString(tag));
    }

    private void setStringIfNotBlank( Node node,
                                      String propertyName,
                                      String value ) throws RepositoryException {
        if (!StringUtil.isBlank(value)) {
            node.setProperty(propertyName, value);
        }
    }

    private void setDoubleIfTagPresent( Node node,
                                        String propertyName,
                                        Directory directory,
                                        int tag ) throws RepositoryException {
        if (!directory.containsTag(tag)) {
            return;
        }
        node.setProperty(propertyName, directory.getRational(tag).doubleValue());
    }

    private boolean hasTags(Directory directory, int... tags) {
        for (int tag : tags) {
            if (directory.containsTag(tag)) {
                return true;
            }
        }
        return false;
    }

    private boolean processUsingDefaultMetadata( Node imageNode,
                                                 Binary binaryValue ) throws Exception {
        InputStream stream = binaryValue.getStream();
        try {
            ImageMetadata metadata = new ImageMetadata();
            metadata.setInput(stream);
            metadata.setDetermineImageNumber(true);
            metadata.setCollectComments(true);

            // Process the image stream and extract the metadata ...
            if (!metadata.check()) {
                getLogger().debug("Unknown format detected using default metadata parser.");
                return false;
            }
            setImagePropertiesOnNode(imageNode, metadata);
            return true;
        } finally {
            stream.close();
        }
    }

    private Node getImageMetadataNode( Node outputNode ) throws RepositoryException {
        if (outputNode.isNew()) {
            outputNode.setPrimaryType(ImageMetadataLexicon.METADATA_NODE);
            return outputNode;
        }
        return outputNode.addNode(ImageMetadataLexicon.METADATA_NODE, ImageMetadataLexicon.METADATA_NODE);
    }

    private void setImagePropertiesOnNode( Node node,
                                           ImageMetadata metadata ) throws Exception {
        node.setProperty(JcrConstants.JCR_MIME_TYPE, metadata.getMimeType());
        node.setProperty(ImageMetadataLexicon.FORMAT_NAME, metadata.getFormatName());
        node.setProperty(ImageMetadataLexicon.WIDTH, metadata.getWidth());
        node.setProperty(ImageMetadataLexicon.HEIGHT, metadata.getHeight());
        node.setProperty(ImageMetadataLexicon.BITS_PER_PIXEL, metadata.getBitsPerPixel());
        node.setProperty(ImageMetadataLexicon.PROGRESSIVE, metadata.isProgressive());
        node.setProperty(ImageMetadataLexicon.NUMBER_OF_IMAGES, metadata.getNumberOfImages());
        node.setProperty(ImageMetadataLexicon.PHYSICAL_WIDTH_DPI, metadata.getPhysicalWidthDpi());
        node.setProperty(ImageMetadataLexicon.PHYSICAL_HEIGHT_DPI, metadata.getPhysicalHeightDpi());
        node.setProperty(ImageMetadataLexicon.PHYSICAL_WIDTH_INCHES, metadata.getPhysicalWidthInch());
        node.setProperty(ImageMetadataLexicon.PHYSICAL_HEIGHT_INCHES, metadata.getPhysicalHeightInch());

    }

    @Override
    public void initialize( NamespaceRegistry registry,
                            NodeTypeManager nodeTypeManager ) throws RepositoryException, IOException {
        registerNodeTypes("images.cnd", nodeTypeManager, true);
        registerDefaultMimeTypes(ImageMetadata.MIME_TYPE_STRINGS);
        registerDefaultMimeTypes(TIFF_MIME_TYPES);
    }
}
