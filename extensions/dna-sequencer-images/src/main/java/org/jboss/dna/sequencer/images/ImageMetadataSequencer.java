/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.sequencer.images;

import java.io.InputStream;
import org.jboss.dna.graph.properties.NameFactory;
import org.jboss.dna.graph.properties.Path;
import org.jboss.dna.graph.properties.PathFactory;
import org.jboss.dna.graph.sequencers.SequencerContext;
import org.jboss.dna.graph.sequencers.SequencerOutput;
import org.jboss.dna.graph.sequencers.StreamSequencer;

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
 * 
 * @author Randall Hauch
 * @author John Verhaeg
 */
public class ImageMetadataSequencer implements StreamSequencer {

    public static final String METADATA_NODE = "image:metadata";
    public static final String IMAGE_PRIMARY_TYPE = "jcr:primaryType";
    public static final String IMAGE_MIXINS = "jcr:mixinTypes";
    public static final String IMAGE_MIME_TYPE = "jcr:mimeType";
    public static final String IMAGE_ENCODING = "jcr:encoding";
    public static final String IMAGE_FORMAT_NAME = "image:formatName";
    public static final String IMAGE_WIDTH = "image:width";
    public static final String IMAGE_HEIGHT = "image:height";
    public static final String IMAGE_BITS_PER_PIXEL = "image:bitsPerPixel";
    public static final String IMAGE_PROGRESSIVE = "image:progressive";
    public static final String IMAGE_NUMBER_OF_IMAGES = "image:numberOfImages";
    public static final String IMAGE_PHYSICAL_WIDTH_DPI = "image:physicalWidthDpi";
    public static final String IMAGE_PHYSICAL_HEIGHT_DPI = "image:physicalHeightDpi";
    public static final String IMAGE_PHYSICAL_WIDTH_INCHES = "image:physicalWidthInches";
    public static final String IMAGE_PHYSICAL_HEIGHT_INCHES = "image:physicalHeightInches";

    /**
     * {@inheritDoc}
     * 
     * @see StreamSequencer#sequence(InputStream, SequencerOutput, SequencerContext)
     */
    public void sequence( InputStream stream,
                          SequencerOutput output,
                          SequencerContext context ) {

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
            NameFactory nameFactory = context.getValueFactories().getNameFactory();
            PathFactory pathFactory = context.getValueFactories().getPathFactory();
            Path metadataNode = pathFactory.create(METADATA_NODE);

            // Place the image metadata into the output map ...
            output.setProperty(metadataNode, nameFactory.create(IMAGE_PRIMARY_TYPE), "image:metadata");
            // output.psetProperty(metadataNode, nameFactory.create(IMAGE_MIXINS), "");
            output.setProperty(metadataNode, nameFactory.create(IMAGE_MIME_TYPE), metadata.getMimeType());
            // output.setProperty(metadataNode, nameFactory.create(IMAGE_ENCODING), "");
            output.setProperty(metadataNode, nameFactory.create(IMAGE_FORMAT_NAME), metadata.getFormatName());
            output.setProperty(metadataNode, nameFactory.create(IMAGE_WIDTH), metadata.getWidth());
            output.setProperty(metadataNode, nameFactory.create(IMAGE_HEIGHT), metadata.getHeight());
            output.setProperty(metadataNode, nameFactory.create(IMAGE_BITS_PER_PIXEL), metadata.getBitsPerPixel());
            output.setProperty(metadataNode, nameFactory.create(IMAGE_PROGRESSIVE), metadata.isProgressive());
            output.setProperty(metadataNode, nameFactory.create(IMAGE_NUMBER_OF_IMAGES), metadata.getNumberOfImages());
            output.setProperty(metadataNode, nameFactory.create(IMAGE_PHYSICAL_WIDTH_DPI), metadata.getPhysicalWidthDpi());
            output.setProperty(metadataNode, nameFactory.create(IMAGE_PHYSICAL_HEIGHT_DPI), metadata.getPhysicalHeightDpi());
            output.setProperty(metadataNode, nameFactory.create(IMAGE_PHYSICAL_WIDTH_INCHES), metadata.getPhysicalWidthInch());
            output.setProperty(metadataNode, nameFactory.create(IMAGE_PHYSICAL_HEIGHT_INCHES), metadata.getPhysicalHeightInch());
        }
    }
}
