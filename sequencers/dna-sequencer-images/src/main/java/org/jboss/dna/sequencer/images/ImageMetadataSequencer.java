/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
import java.util.Map;
import org.jboss.dna.common.jcr.Path;
import org.jboss.dna.common.monitor.ProgressMonitor;
import org.jboss.dna.services.ExecutionContext;
import org.jboss.dna.services.sequencers.StreamSequencer;

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
 * <li><strong>image:numberOfImages</strong> - optional integer property for the number of images stored in the file; defaults
 * to 1</li>
 * <li><strong>image:physicalWidthDpi</strong> - optional integer property for the physical width of the image in dots per inch</li>
 * <li><strong>image:physicalHeightDpi</strong> - optional integer property for the physical height of the image in dots per
 * inch</li>
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
 * @author Randall Hauch
 */
public class ImageMetadataSequencer extends StreamSequencer {

    public static final Path METADATA_NODE = new Path("image:metadata");
    public static final Path IMAGE_PRIMARY_TYPE = METADATA_NODE.append("jcr:primaryType");
    public static final Path IMAGE_MIXINS = METADATA_NODE.append("jcr:mixinTypes");
    public static final Path IMAGE_MIME_TYPE = METADATA_NODE.append("jcr:mimeType");
    public static final Path IMAGE_ENCODING = METADATA_NODE.append("jcr:encoding");
    public static final Path IMAGE_FORMAT_NAME = METADATA_NODE.append("image:formatName");
    public static final Path IMAGE_WIDTH = METADATA_NODE.append("image:width");
    public static final Path IMAGE_HEIGHT = METADATA_NODE.append("image:height");
    public static final Path IMAGE_BITS_PER_PIXEL = METADATA_NODE.append("image:bitsPerPixel");
    public static final Path IMAGE_PROGRESSIVE = METADATA_NODE.append("image:progressive");
    public static final Path IMAGE_NUMBER_OF_IMAGES = METADATA_NODE.append("image:numberOfImages");
    public static final Path IMAGE_PHYSICAL_WIDTH_DPI = METADATA_NODE.append("image:physicalWidthDpi");
    public static final Path IMAGE_PHYSICAL_HEIGHT_DPI = METADATA_NODE.append("image:physicalHeightDpi");
    public static final Path IMAGE_PHYSICAL_WIDTH_INCHES = METADATA_NODE.append("image:physicalWidthInches");
    public static final Path IMAGE_PHYSICAL_HEIGHT_INCHES = METADATA_NODE.append("image:physicalHeightInches");

    /**
     * {@inheritDoc}
     */
    @Override
    protected void sequence( InputStream stream, Map<Path, Object> output, ExecutionContext context, ProgressMonitor progressMonitor ) {
        ImageMetadata metadata = new ImageMetadata();
        metadata.setInput(stream);
        metadata.setDetermineImageNumber(true);
        metadata.setCollectComments(true);

        // Process the image ...
        if (!metadata.check()) {
            metadata = null;
        }

        if (metadata != null) {
            // Place the image metadata into the output map ...
            output.put(IMAGE_PRIMARY_TYPE, "image:metadata");
            // output.put(IMAGE_MIXINS, "");
            output.put(IMAGE_MIME_TYPE, metadata.getMimeType());
            // output.put(IMAGE_ENCODING, "");
            output.put(IMAGE_FORMAT_NAME, metadata.getFormatName());
            output.put(IMAGE_WIDTH, metadata.getWidth());
            output.put(IMAGE_HEIGHT, metadata.getHeight());
            output.put(IMAGE_BITS_PER_PIXEL, metadata.getBitsPerPixel());
            output.put(IMAGE_PROGRESSIVE, metadata.isProgressive());
            output.put(IMAGE_NUMBER_OF_IMAGES, metadata.getNumberOfImages());
            output.put(IMAGE_PHYSICAL_WIDTH_DPI, metadata.getPhysicalWidthDpi());
            output.put(IMAGE_PHYSICAL_HEIGHT_DPI, metadata.getPhysicalHeightDpi());
            output.put(IMAGE_PHYSICAL_WIDTH_INCHES, metadata.getPhysicalWidthInch());
            output.put(IMAGE_PHYSICAL_HEIGHT_INCHES, metadata.getPhysicalHeightInch());
        }
    }
}
