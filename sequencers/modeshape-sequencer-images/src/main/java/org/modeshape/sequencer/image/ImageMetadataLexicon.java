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

import org.modeshape.common.annotation.Immutable;


/**
 * A lexicon of names used within the image sequencer.
 */
@Immutable
public final class ImageMetadataLexicon {

    public static final String METADATA_NODE = "image:metadata";
    public static final String EXIF_NODE = "image:exif";
    public static final String FORMAT_NAME = "image:formatName";
    public static final String WIDTH = "image:width";
    public static final String HEIGHT = "image:height";
    public static final String BITS_PER_PIXEL = "image:bitsPerPixel";
    public static final String PROGRESSIVE = "image:progressive";
    public static final String NUMBER_OF_IMAGES = "image:numberOfImages";
    public static final String PHYSICAL_WIDTH_DPI = "image:physicalWidthDpi";
    public static final String PHYSICAL_HEIGHT_DPI = "image:physicalHeightDpi";
    public static final String PHYSICAL_WIDTH_INCHES = "image:physicalWidthInches";
    public static final String PHYSICAL_HEIGHT_INCHES = "image:physicalHeightInches";
    public static final String ARTIST = "image:artist";
    public static final String COPYRIGHT = "image:copyright";
    public static final String DATETIME = "image:datetime";
    public static final String DESCRIPTION = "image:description";
    public static final String MAKE = "image:make";
    public static final String MODEL = "image:model";
    public static final String ORIENTATION = "image:orientation";
    public static final String UNIT = "image:unit";
    public static final String SOFTWARE = "image:software";
    public static final String RESOLUTION_X = "image:resolution_x";
    public static final String RESOLUTION_Y = "image:resolution_y";


    private ImageMetadataLexicon() {
    }
}
