/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.graph.mimetype;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.i18n.I18n;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.graph.GraphI18n;

/**
 * A {@link MimeTypeDetector} that attempts to match the extension of the supplied name against a set of known file extensions.
 * 
 * @author Randall Hauch
 */
@Immutable
public class ExtensionBasedMimeTypeDetector implements MimeTypeDetector {

    /**
     * The default location of the properties file containing the extension patterns to MIME types.
     */
    public static final String MIME_TYPE_EXTENSIONS_RESOURCE_PATH = "/org/jboss/dna/graph/MimeTypes.properties";

    /**
     * The mapping of extension (which includes the leading '.') to MIME type.
     */
    private final Map<String, String> mimeTypesByExtension;

    /**
     * Create a default instance of the extension-based MIME type detector. The set of extension patterns to MIME-types is loaded
     * from "org.jboss.dna.graph.MimeTypes.properties".
     */
    public ExtensionBasedMimeTypeDetector() {
        this(null, true);
    }

    /**
     * Create an instance of the extension-based MIME type detector by using the supplied mappings. The set of extension patterns
     * to MIME-types is loaded from "org.jboss.dna.graph.MimeTypes.properties", but the supplied extension mappings override any
     * default mappings.
     * 
     * @param extensionsToMimeTypes the mapping of extension patterns to MIME types, which will override the default mappings; may
     *        be null if the default mappings are to be used
     */
    public ExtensionBasedMimeTypeDetector( Map<String, String> extensionsToMimeTypes ) {
        this(extensionsToMimeTypes, true);
    }

    /**
     * Create an instance of the extension-based MIME type detector by using the supplied mappings. If requested, the set of
     * extension patterns to MIME-types is loaded from "org.jboss.dna.graph.MimeTypes.properties" and any supplied extension
     * mappings override any default mappings.
     * 
     * @param extensionsToMimeTypes the mapping of extension patterns to MIME types, which will override the default mappings; may
     *        be null if the default mappings are to be used
     * @param initWithDefaults true if the default mappings are to be loaded first
     */
    public ExtensionBasedMimeTypeDetector( Map<String, String> extensionsToMimeTypes,
                                           boolean initWithDefaults ) {
        Map<String, String> mappings = getDefaultMappings();
        if (extensionsToMimeTypes != null) {
            for (Map.Entry<String, String> entry : extensionsToMimeTypes.entrySet()) {
                String extensionString = entry.getKey();
                if (extensionString == null) continue;
                // Lowercase, trime, and remove all leading '.' characters ...
                extensionString = extensionString.toLowerCase().trim().replaceAll("^.+", "");
                if (extensionString.length() == 0) continue;
                String mimeType = entry.getValue();
                if (mimeType == null) continue;
                mimeType = entry.getValue().trim();
                if (mimeType.length() == 0) continue;
                assert extensionString.length() != 0;
                assert mimeType.length() != 0;
                mappings.put(extensionString, mimeType);
            }
        }
        // Now put the mappings into the different maps ...
        Map<String, String> mappingsByAnyCharExtension = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : mappings.entrySet()) {
            String extensionString = entry.getKey();
            String mimeType = entry.getValue();
            assert extensionString != null;
            assert extensionString.length() != 0;
            assert mimeType != null;
            assert mimeType.length() != 0;
            mappingsByAnyCharExtension.put("." + extensionString, mimeType);
        }
        mimeTypesByExtension = Collections.unmodifiableMap(mappingsByAnyCharExtension);
    }

    protected static Map<String, String> getDefaultMappings() {
        Properties extensionsToMimeTypes = new Properties();
        try {
            extensionsToMimeTypes.load(ExtensionBasedMimeTypeDetector.class.getResourceAsStream(MIME_TYPE_EXTENSIONS_RESOURCE_PATH));
        } catch (IOException e) {
            I18n msg = GraphI18n.unableToAccessResourceFileFromClassLoader;
            Logger.getLogger(ExtensionBasedMimeTypeDetector.class).warn(e, msg, MIME_TYPE_EXTENSIONS_RESOURCE_PATH);
        }
        Map<String, String> mimeTypesByExtension = new HashMap<String, String>();
        for (Map.Entry<Object, Object> entry : extensionsToMimeTypes.entrySet()) {
            String mimeType = entry.getKey().toString().trim();
            String extensionStrings = entry.getValue().toString().toLowerCase().trim();
            for (String extensionString : extensionStrings.split("\\s+")) {
                extensionString = extensionString.trim();
                if (extensionString.length() != 0) mimeTypesByExtension.put(extensionString, mimeType);
            }
        }
        return mimeTypesByExtension;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.mimetype.MimeTypeDetector#mimeTypeOf(java.lang.String, java.io.InputStream)
     */
    public String mimeTypeOf( String name,
                              InputStream content ) {
        if (name == null || name.length() == 0) return null;
        String trimmedName = name.trim();
        if (trimmedName.length() == 0) return null;

        // Find the extension ...
        int indexOfDelimiter = trimmedName.lastIndexOf('.');
        if (indexOfDelimiter < 1) return null;
        String extension = trimmedName.substring(indexOfDelimiter).toLowerCase();

        // Look for a match ...
        return mimeTypesByExtension.get(extension);
    }
}
