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

import java.io.InputStream;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.sequencer.SequencerOutput;
import org.modeshape.graph.sequencer.StreamSequencer;
import org.modeshape.graph.sequencer.StreamSequencerContext;

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
 * <li><strong>image:height</strong> - optional integer property for the image's height in pixles</li>
 * <li><strong>image:bitsPerPixel</strong> - optional integer property for the number of bits per pixel</li>
 * <li><strong>image:progressive</strong> - optional boolean property specifying whether the image is stored in a progressive
 * (i.e., interlaced) form</li>
 * <li><strong>image:numberOfImages</strong> - optional integer property for the number of images stored in the file; defaults to
 * 1</li>
 * <li><strong>image:physicalWidthDpi</strong> - optional integer property for the physical width of the image in dots per inch</li>
 * <li><strong>image:physicalHeightDpi</strong> - optional integer property for the physical height of the image in dots per inch</li>
 * <li><strong>image:physicalWidthInches</strong> - optional double property for the physical width of the image in inches</li>
 * <li><strong>image:physicalHeightInches</strong> - optional double property for the physical height of the image in inches</li>
 * </ul>
 * </li>
 * </ul>
 * </p>
 * <p>
 * This structure could be extended in the future to add EXIF and IPTC metadata as child nodes. For example, EXIF metadata is
 * structured as tags in directories, where the directories form something like namespaces, and which are used by different camera
 * vendors to store custom metadata. This structure could be mapped with each directory (e.g. "EXIF" or "Nikon Makernote" or
 * "IPTC") as the name of a child node, with the EXIF tags values stored as either properties or child nodes.
 * </p>
 */
public class ImageMetadataSequencer implements StreamSequencer {

    /**
     * {@inheritDoc}
     * 
     * @see StreamSequencer#sequence(InputStream, SequencerOutput, StreamSequencerContext)
     */
    public void sequence( InputStream stream,
                          SequencerOutput output,
                          StreamSequencerContext context ) {

        ImageMetadata metadata = new ImageMetadata();
        metadata.setInput(stream);
        metadata.setDetermineImageNumber(true);
        metadata.setCollectComments(true);

        // Process the image stream and extract the metadata ...
        if (!metadata.check()) {
            metadata = null;
        }

        // Generate the output graph if we found useful metadata ...
        if (metadata != null) {
            PathFactory pathFactory = context.getValueFactories().getPathFactory();
            Path metadataNode = pathFactory.createRelativePath(ImageMetadataLexicon.METADATA_NODE);

            // Place the image metadata into the output map ...
            output.setProperty(metadataNode, JcrLexicon.PRIMARY_TYPE, "image:metadata");
            // output.psetProperty(metadataNode, nameFactory.create(IMAGE_MIXINS), "");
            output.setProperty(metadataNode, JcrLexicon.MIMETYPE, metadata.getMimeType());
            // output.setProperty(metadataNode, nameFactory.create(IMAGE_ENCODING), "");
            output.setProperty(metadataNode, ImageMetadataLexicon.FORMAT_NAME, metadata.getFormatName());
            output.setProperty(metadataNode, ImageMetadataLexicon.WIDTH, metadata.getWidth());
            output.setProperty(metadataNode, ImageMetadataLexicon.HEIGHT, metadata.getHeight());
            output.setProperty(metadataNode, ImageMetadataLexicon.BITS_PER_PIXEL, metadata.getBitsPerPixel());
            output.setProperty(metadataNode, ImageMetadataLexicon.PROGRESSIVE, metadata.isProgressive());
            output.setProperty(metadataNode, ImageMetadataLexicon.NUMBER_OF_IMAGES, metadata.getNumberOfImages());
            output.setProperty(metadataNode, ImageMetadataLexicon.PHYSICAL_WIDTH_DPI, metadata.getPhysicalWidthDpi());
            output.setProperty(metadataNode, ImageMetadataLexicon.PHYSICAL_HEIGHT_DPI, metadata.getPhysicalHeightDpi());
            output.setProperty(metadataNode, ImageMetadataLexicon.PHYSICAL_WIDTH_INCHES, metadata.getPhysicalWidthInch());
            output.setProperty(metadataNode, ImageMetadataLexicon.PHYSICAL_HEIGHT_INCHES, metadata.getPhysicalHeightInch());
        }
    }
}
