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
package org.modeshape.sequencer.image;

import org.modeshape.common.annotation.Immutable;


/**
 * A lexicon of names used within the image sequencer.
 */
@Immutable
public final class ImageMetadataLexicon {

    public static class Namespace {
        public static final String URI = "http://www.modeshape.org/images/1.0";
        public static final String PREFIX = "image";

        private Namespace() {
        }
    }

    public static final String METADATA_NODE = "image:metadata";
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


    private ImageMetadataLexicon() {
    }
}
