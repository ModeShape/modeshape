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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.i18n.I18n;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.common.util.IoUtil;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.common.util.StringUtil;
import org.jboss.dna.graph.GraphI18n;

/**
 * A {@link MimeTypeDetector} that attempts to match the extension of the supplied name against a set of known file extensions.
 * 
 * @author Randall Hauch
 */
@Immutable
public class ExtensionBasedMimeTypeDetector implements MimeTypeDetector {

    /**
     * The default location of the properties file containing the extension patterns to MIME types. Value is "{@value} ".
     */
    public static final String MIME_TYPE_EXTENSIONS_RESOURCE_PATH = "/org/jboss/dna/graph/mime.types";
    // public static final String MIME_TYPE_EXTENSIONS_RESOURCE_PATH = "/org/jboss/dna/graph/MimeTypes.properties";

    /**
     * The mapping of extension (which includes the leading '.') to MIME type.
     */
    private final Map<String, String> mimeTypesByExtension;

    /**
     * Create a default instance of the extension-based MIME type detector. The set of extension patterns to MIME-types is loaded
     * from the "org/jboss/dna/graph/mime.types" classpath resource.
     */
    public ExtensionBasedMimeTypeDetector() {
        this(null, true);
    }

    /**
     * Create an instance of the extension-based MIME type detector by using the supplied mappings. The set of extension patterns
     * to MIME-types is loaded from the "org/jboss/dna/graph/mime.types" classpath resource, but the supplied extension mappings
     * override any default mappings.
     * 
     * @param extensionsToMimeTypes the mapping of extension patterns to MIME types, which will override the default mappings; may
     *        be null if the default mappings are to be used
     */
    public ExtensionBasedMimeTypeDetector( Map<String, String> extensionsToMimeTypes ) {
        this(extensionsToMimeTypes, true);
    }

    /**
     * Create an instance of the extension-based MIME type detector by using the supplied mappings. If requested, the set of
     * extension patterns to MIME-types is loaded from the "org/jboss/dna/graph/mime.types" classpath resource and any supplied
     * extension mappings override any default mappings.
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

    /**
     * Load the default extensions from {@link #MIME_TYPE_EXTENSIONS_RESOURCE_PATH}, which can either be a property file or a
     * tab-delimited *nix-style MIME types file (common in web servers and libraries). If an extension applies to more than one
     * MIME type, the first one in the file wins.
     * 
     * @return the default mappings; never null
     */
    protected static Map<String, String> getDefaultMappings() {
        Map<String, Set<String>> duplicates = new HashMap<String, Set<String>>();
        return load(ExtensionBasedMimeTypeDetector.class.getResourceAsStream(MIME_TYPE_EXTENSIONS_RESOURCE_PATH), duplicates);
    }

    /**
     * Load the default extensions from the supplied stream, which may provide the contents in the format of property file or a
     * tab-delimited *nix-style MIME types file (common in web servers and libraries). If an extension applies to more than one
     * MIME type, the first one in the file wins.
     * 
     * @param stream the stream containing the content; may not be null
     * @param duplicateMimeTypesByExtension a map into which any extension should be placed if there are multiple MIME types that
     *        apply; may be null if this information is not required
     * @return the default mappings; never null
     */
    protected static Map<String, String> load( InputStream stream,
                                               Map<String, Set<String>> duplicateMimeTypesByExtension ) {
        CheckArg.isNotNull(stream, "stream");
        // Create a Regex pattern that can be used for each line. This pattern looks for a mime type
        // (which may contain no whitespace or '=') followed by an optional whitespace, an optional equals sign,
        // optionally more whitespace, and finally by a string of one or more extensions.
        Pattern linePattern = Pattern.compile("\\s*([^\\s=]+)\\s*=?\\s*(.*)");
        List<String> lines = null;
        try {
            String content = IoUtil.read(stream);
            lines = StringUtil.splitLines(content);
        } catch (IOException e) {
            I18n msg = GraphI18n.unableToAccessResourceFileFromClassLoader;
            Logger.getLogger(ExtensionBasedMimeTypeDetector.class).warn(e, msg, MIME_TYPE_EXTENSIONS_RESOURCE_PATH);
        }
        Map<String, String> mimeTypesByExtension = new HashMap<String, String>();
        if (lines != null) {
            for (String line : lines) {
                line = line.trim();
                if (line.length() == 0 || line.startsWith("#")) continue;
                // Apply the pattern to each line ...
                Matcher matcher = linePattern.matcher(line);
                if (matcher.matches()) {
                    String mimeType = matcher.group(1).trim().toLowerCase();
                    String extensions = matcher.group(2).trim().toLowerCase();
                    if (extensions.length() != 0) {
                        // A valid mime type with at least one extension was found, so for each extension ...
                        for (String extensionString : extensions.split("\\s+")) {
                            extensionString = extensionString.trim();
                            if (extensionString.length() != 0) {
                                // Register the extension with the MIME type ...
                                String existingMimeType = mimeTypesByExtension.put(extensionString, mimeType);
                                if (existingMimeType != null) {
                                    // A MIME type already had this extension, so use the first one ...
                                    mimeTypesByExtension.put(extensionString, existingMimeType);
                                    if (duplicateMimeTypesByExtension != null) {
                                        // And record the duplicate ...
                                        Set<String> dups = duplicateMimeTypesByExtension.get(extensionString);
                                        if (dups == null) {
                                            dups = new HashSet<String>();
                                            duplicateMimeTypesByExtension.put(extensionString, dups);
                                        }
                                        dups.add(existingMimeType);
                                        dups.add(mimeType);
                                    }
                                }
                            }
                        }
                    }
                }
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
