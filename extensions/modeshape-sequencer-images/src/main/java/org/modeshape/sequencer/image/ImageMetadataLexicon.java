package org.modeshape.sequencer.image;

import net.jcip.annotations.Immutable;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.basic.BasicName;

/**
 * A lexicon of names used within the image sequencer.
 */
@Immutable
public class ImageMetadataLexicon {

    public static class Namespace {
        public static final String URI = "http://www.modeshape.org/images/1.0";
        public static final String PREFIX = "image";
    }

    public static final Name METADATA_NODE = new BasicName(Namespace.URI, "metadata");
    public static final Name FORMAT_NAME = new BasicName(Namespace.URI, "formatName");
    public static final Name WIDTH = new BasicName(Namespace.URI, "width");
    public static final Name HEIGHT = new BasicName(Namespace.URI, "height");
    public static final Name BITS_PER_PIXEL = new BasicName(Namespace.URI, "bitsPerPixel");
    public static final Name PROGRESSIVE = new BasicName(Namespace.URI, "progressive");
    public static final Name NUMBER_OF_IMAGES = new BasicName(Namespace.URI, "numberOfImages");
    public static final Name PHYSICAL_WIDTH_DPI = new BasicName(Namespace.URI, "physicalWidthDpi");
    public static final Name PHYSICAL_HEIGHT_DPI = new BasicName(Namespace.URI, "physicalHeightDpi");
    public static final Name PHYSICAL_WIDTH_INCHES = new BasicName(Namespace.URI, "physicalWidthInches");
    public static final Name PHYSICAL_HEIGHT_INCHES = new BasicName(Namespace.URI, "physicalHeightInches");

}
