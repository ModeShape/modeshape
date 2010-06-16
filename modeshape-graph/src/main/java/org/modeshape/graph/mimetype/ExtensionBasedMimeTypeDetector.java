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
package org.modeshape.graph.mimetype;

import java.io.InputStream;
import java.util.Map;
import net.jcip.annotations.Immutable;
import org.modeshape.common.util.MimeTypeUtil;

/**
 * A {@link MimeTypeDetector} that attempts to match the extension of the supplied name against a set of known file extensions.
 */
@Immutable
public class ExtensionBasedMimeTypeDetector extends MimeTypeUtil implements MimeTypeDetector {

    /**
     * A default configuration that may be used to {@link MimeTypeDetectors#addDetector(MimeTypeDetectorConfig) add} the
     * {@link ExtensionBasedMimeTypeDetector} to a {@link MimeTypeDetector} instance.
     */
    public static final MimeTypeDetectorConfig CONFIGURATION = new MimeTypeDetectorConfig(
                                                                                          "Extension-Based",
                                                                                          "MIME type detector that uses only the filename extensions",
                                                                                          ExtensionBasedMimeTypeDetector.class);

    /**
     * Create a default instance of the extension-based MIME type detector. The set of extension patterns to MIME-types is loaded
     * from the "{@link MimeTypeUtil#MIME_TYPE_EXTENSIONS_RESOURCE_PATH org/modeshape/mime.types}" classpath resource.
     */
    public ExtensionBasedMimeTypeDetector() {
        super();
    }

    /**
     * Create an instance of the extension-based MIME type detector by using the supplied mappings. The set of extension patterns
     * to MIME-types is loaded from the "{@link MimeTypeUtil#MIME_TYPE_EXTENSIONS_RESOURCE_PATH org/modeshape/mime.types}"
     * classpath resource, but the supplied extension mappings override any default mappings.
     * 
     * @param extensionsToMimeTypes the mapping of extension patterns to MIME types, which will override the default mappings; may
     *        be null if the default mappings are to be used
     */
    public ExtensionBasedMimeTypeDetector( Map<String, String> extensionsToMimeTypes ) {
        super(extensionsToMimeTypes);
    }

    /**
     * Create an instance of the extension-based MIME type detector by using the supplied mappings. If requested, the set of
     * extension patterns to MIME-types is loaded from the "org/modeshape/graph/mime.types" classpath resource and any supplied
     * extension mappings override any default mappings.
     * 
     * @param extensionsToMimeTypes the mapping of extension patterns to MIME types, which will override the default mappings; may
     *        be null if the default mappings are to be used
     * @param initWithDefaults true if the default mappings are to be loaded first
     */
    public ExtensionBasedMimeTypeDetector( Map<String, String> extensionsToMimeTypes,
                                           boolean initWithDefaults ) {
        super(extensionsToMimeTypes, initWithDefaults);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.mimetype.MimeTypeDetector#mimeTypeOf(java.lang.String, java.io.InputStream)
     */
    public String mimeTypeOf( String name,
                              InputStream content ) {
        return mimeTypeOf(name);
    }
}
