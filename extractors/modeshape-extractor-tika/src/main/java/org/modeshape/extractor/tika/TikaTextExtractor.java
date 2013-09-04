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
package org.modeshape.extractor.tika;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.jcr.RepositoryException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.DefaultParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.modeshape.common.collection.Collections;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.api.Binary;
import org.modeshape.jcr.api.text.TextExtractor;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * A {@link TextExtractor} that uses the Apache Tika library.
 * <p>
 * This extractor will automatically discover all of the Tika {@link Parser} implementations that are defined in
 * <code>META-INF/services/org.apache.tika.parser.Parser</code> text files accessible via the current classloader and that contain
 * the class names of the Parser implementations (one class name per line in each file).
 * </p>
 * <p>
 * This text extractor can be configured in a ModeShape configuration by specifying several optional properties:
 * <ul>
 * <li><strong>excludedMimeTypes</strong> - The comma- or whitespace-separated list of MIME types that should be excluded from
 * text extraction, even if there is a Tika Parser available for that MIME type. By default, the MIME types for
 * {@link #DEFAULT_EXCLUDED_MIME_TYPES package files} are excluded, though explicitly setting any excluded MIME types will
 * override these default.</li>
 * <li><strong>includedMimeTypes</strong> - The comma- or whitespace-separated list of MIME types that should be included in text
 * extraction. This extractor will ignore any MIME types in this list that are not covered by Tika Parser implementations.</li>
 * </ul>
 * </p>
 */
public class TikaTextExtractor extends TextExtractor {

    protected static final Logger LOGGER = Logger.getLogger(TikaTextExtractor.class);

    /**
     * The MIME types that are excluded by default. Currently, this list consists of:
     * <ul>
     * <li>application/x-archive</li>
     * <li>application/x-bzip</li>
     * <li>application/x-bzip2</li>
     * <li>application/x-cpio</li>
     * <li>application/x-gtar</li>
     * <li>application/x-gzip</li>
     * <li>application/x-tar</li>
     * <li>application/zip</li>
     * <li>application/vnd.teiid.vdb</li>
     * <li>image/*</li>
     * <li>audio/*</li>
     * <li>video/*</li>
     * </ul>
     */
    public static final Set<MediaType> DEFAULT_EXCLUDED_MIME_TYPES = Collections.unmodifiableSet(
            MediaType.application("x-archive"), MediaType.application("x-bzip"), MediaType.application("x-bzip2"),
            MediaType.application("x-cpio"), MediaType.application("x-gtar"), MediaType.application("x-gzip"),
            MediaType.application("x-tar"), MediaType.application("zip"), MediaType.application("vnd.teiid.vdb"),
            MediaType.image("*"), MediaType.audio("*"), MediaType.video("*"));

    private Set<MediaType> excludedMimeTypes = new HashSet<MediaType>();
    private Set<String> includedMimeTypes = new HashSet<String>();
    private Set<String> supportedMediaTypes = new HashSet<String>();

    private Integer writeLimit;

    private final Lock initLock = new ReentrantLock();
    private DefaultParser parser;

    /**
     * No-arg constructor is required because this is instantiated by reflection.
     */
    public TikaTextExtractor() {
        this.excludedMimeTypes.addAll(DEFAULT_EXCLUDED_MIME_TYPES);
    }

    @Override
    public boolean supportsMimeType( String mimeType ) {
        MediaType mediaType = MediaType.parse(mimeType);
        if (mediaType == null) {
            getLogger().debug("Invalid mime-type:" + mimeType);
            return false;
        }

        for (MediaType excludedMediaType : excludedMimeTypes) {
            if (excludedMediaType.equals(mediaType)) {
                return false;
            }
            if (excludedMediaType.getSubtype().equalsIgnoreCase("*") && mediaType.getType().equalsIgnoreCase(excludedMediaType.getType())) {
                return false;
            }
        }
        initialize();
        return includedMimeTypes.isEmpty() ? supportedMediaTypes.contains(mimeType) : supportedMediaTypes.contains(mimeType)
                                                                                      && includedMimeTypes.contains(mimeType);
    }

    @Override
    public void extractFrom( final Binary binary,
                             final TextExtractor.Output output,
                             final Context context ) throws Exception {

        final DefaultParser parser = initialize();
        final Integer writeLimit = this.writeLimit;
        processStream(binary, new BinaryOperation<Object>() {
            @Override
            public Object execute( InputStream stream ) throws Exception {
                Metadata metadata = prepareMetadata(binary, context);
                try {
                    LOGGER.debug("Using TikaTextExtractor to extract text");
                    //TODO author=Horia Chiorean date=1/30/13 description=//TIKA 1.2 TXTParser seems to have a bug, always adding 1 ignorable whitespace to the actual chars to be parsed
                    //https://issues.apache.org/jira/browse/TIKA-1069
                    ContentHandler textHandler = writeLimit == null ? new BodyContentHandler() : new BodyContentHandler(writeLimit + 1);
                    // Parse the input stream ...
                    parser.parse(stream, textHandler, metadata, new ParseContext());

                    // Record all of the text in the body ...
                    String text = textHandler.toString().trim();
                    output.recordText(text);
                    LOGGER.debug("TikaTextExtractor found text: " + text);
                } catch (SAXException sae) {
                    LOGGER.warn(TikaI18n.parseExceptionWhileExtractingText, sae.getMessage());
                } catch (NoClassDefFoundError ncdfe) {
                    LOGGER.warn(TikaI18n.warnNoClassDefFound, ncdfe.getMessage());
                } catch (Throwable e) {
                    LOGGER.error(e, TikaI18n.errorWhileExtractingTextFrom, e.getMessage());
                }
                return null;
            }
        });

    }

    /**
     * Creates a new tika metadata object used by the parser. This will contain the mime-type of the content being parsed, if this
     * is available to the underlying context. If not, Tika's autodetection mechanism is used to try and get the mime-type.
     * 
     * @param binary a <code>org.modeshape.jcr.api.Binary</code> instance of the content being parsed
     * @param context the extraction context; may not be null
     * @return a <code>Metadata</code> instance.
     * @throws java.io.IOException if auto-detecting the mime-type via Tika fails
     * @throws RepositoryException if error obtaining MIME-type of the binary parameter
     */
    protected final Metadata prepareMetadata( final Binary binary,
                                              final Context context ) throws IOException, RepositoryException {
        Metadata metadata = new Metadata();

        String mimeType = binary.getMimeType();
        if (StringUtil.isBlank(mimeType)) {
            // Call the detector (we don't know the name) ...
            mimeType = context.mimeTypeOf(null, binary);
        }
        if (!StringUtil.isBlank(mimeType)) {
            metadata.set(Metadata.CONTENT_TYPE, mimeType);
        }
        return metadata;
    }

    /**
     * This class lazily initializes the {@link DefaultParser} instance.
     * 
     * @return the default parser; same as {@link #parser}
     */
    protected DefaultParser initialize() {
        if (parser == null) {
            try {
                initLock.lock();
                if (parser == null) {
                    parser = new DefaultParser(this.getClass().getClassLoader());
                }
                LOGGER.debug("Initializing TikaTextExtractor");
                Map<MediaType, Parser> parsers = parser.getParsers();
                LOGGER.debug("TikaTextExtractor found " + parsers.size() + " parsers");
                for (MediaType mediaType : parsers.keySet()) {
                    // Don't use the toString() method, as it may append properties ...
                    String mimeType = mediaType.getType() + "/" + mediaType.getSubtype();
                    supportedMediaTypes.add(mimeType);
                    LOGGER.debug("TikaTextExtractor will support '" + mimeType + "'");
                }
            } finally {
                initLock.unlock();
            }
        }
        return parser;
    }

    /**
     * Get the MIME types that are explicitly requested to be included. This list may not correspond to the MIME types that can be
     * handled via the available Parser implementations.
     * 
     * @return the set of MIME types that are to be included; never null
     */
    public Set<String> getIncludedMimeTypes() {
        return Collections.unmodifiableSet(includedMimeTypes);
    }

    /**
     * Set the MIME types that should be included. This method clears all previously-set excluded MIME types.
     * 
     * @param includedMimeTypes the whitespace-delimited or comma-separated list of MIME types that are to be included
     */
    public void setIncludedMimeTypes( String includedMimeTypes ) {
        if (includedMimeTypes == null || includedMimeTypes.length() == 0) return;
        this.includedMimeTypes.clear();
        for (String mimeType : includedMimeTypes.split("[,\\s]")) {
            includeMimeType(mimeType);
        }
    }

    /**
     * Sets the mime-types supported by this extractor.
     *
     * @param includedMimeTypes a collection of mime-types.
     */
    public void setIncludedMimeTypes( Collection<String> includedMimeTypes ) {
        if (includedMimeTypes != null) {
            this.includedMimeTypes = new HashSet<String>(includedMimeTypes);
        }
    }

    /**
     * Include the MIME type from extraction.
     * 
     * @param mimeType MIME type that should be included
     */
    private void includeMimeType( String mimeType ) {
        if (mimeType == null) return;
        mimeType = mimeType.trim();
        if (mimeType.length() != 0) includedMimeTypes.add(mimeType);
    }

    /**
     * Set the MIME types that should be excluded.
     * 
     * @return the set of MIME types that are to be excluded; never null
     */
    public Set<String> getExcludedMimeTypes() {
        Set<String> result = new HashSet<String>();
        for (MediaType mediaType : this.excludedMimeTypes) {
            result.add(mediaType.toString());
        }
        return Collections.unmodifiableSet(result);
    }

    /**
     * Set the MIME types that should be excluded. This method clears all previously-set excluded MIME types.
     * 
     * @param excludedMimeTypes the whitespace-delimited or comma-separated list of MIME types that are to be excluded
     */
    public void setExcludedMimeTypes( String excludedMimeTypes ) {
        if (excludedMimeTypes == null || excludedMimeTypes.length() == 0) return;
        this.excludedMimeTypes.clear();
        for (String mimeType : excludedMimeTypes.split("[,\\s]")) {
            excludeMimeType(mimeType);
        }
    }


    /**
     * Sets the mime-types ignored by this extractor
     *
     * @param excludedMimeTypes a collection of mime-types.
     */
    public void setExcludedMimeTypes( Collection<String> excludedMimeTypes ) {
        if (excludedMimeTypes != null) {
            this.excludedMimeTypes.clear();
            for (String excludedMimeType : excludedMimeTypes) {
                excludeMimeType(excludedMimeType);
            }
        }
    }

    /**
     * Exclude the MIME type from extraction.
     * 
     * @param mimeType MIME type that should be excluded
     */
    private void excludeMimeType( String mimeType ) {
        MediaType mediaType = MediaType.parse(mimeType);
        if (mediaType == null) {
            getLogger().debug("Invalid media type: {0}", mimeType);
            return;
        }
        excludedMimeTypes.add(mediaType);
    }


    /**
     * Sets the write limit for the Tika parser, representing the maximum number of characters that should be extracted by the
     * TIKA parser.
     *
     * @param writeLimit an {@link Integer} which represents the write limit; may be null
     * @see BodyContentHandler#BodyContentHandler(int)
     */
    public void setWriteLimit( Integer writeLimit ) {
        this.writeLimit = writeLimit;
    }
}
