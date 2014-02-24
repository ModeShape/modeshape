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
    protected static final Set<MediaType> DEFAULT_EXCLUDED_MIME_TYPES = Collections.unmodifiableSet(
            MediaType.application("x-archive"), MediaType.application("x-bzip"), MediaType.application("x-bzip2"),
            MediaType.application("x-cpio"), MediaType.application("x-gtar"), MediaType.application("x-gzip"),
            MediaType.application("x-tar"), MediaType.application("zip"), MediaType.application("vnd.teiid.vdb"),
            MediaType.image("*"), MediaType.audio("*"), MediaType.video("*"));

    private final Set<MediaType> excludedMediaTypes = new HashSet<MediaType>();
    private final Set<MediaType> includedMediaTypes = new HashSet<MediaType>();
    private final Set<MediaType> parserSupportedMediaTypes = new HashSet<MediaType>();

    /**
     * The write limit for the Tika parser, representing the maximum number of characters that should be extracted by the
     * TIKA parser; set via reflection
     */
    private Integer writeLimit;

    private final Lock initLock = new ReentrantLock();
    private DefaultParser parser;

    /**
     * No-arg constructor is required because this is instantiated by reflection.
     */
    public TikaTextExtractor() {
        this.excludedMediaTypes.addAll(DEFAULT_EXCLUDED_MIME_TYPES);
    }

    @Override
    public boolean supportsMimeType( String mimeType ) {
        MediaType mediaType = MediaType.parse(mimeType);
        if (mediaType == null) {
            logger().debug("Invalid mime-type: {0}", mimeType);
            return false;
        }
        initialize();
        for (MediaType excludedMediaType : excludedMediaTypes) {
            if (excludedMediaType.equals(mediaType)) {
                return false;
            }
            if (excludedMediaType.getSubtype().equalsIgnoreCase("*") && mediaType.getType().equalsIgnoreCase(excludedMediaType.getType())) {
                return false;
            }
        }
        return includedMediaTypes.isEmpty() ? parserSupportedMediaTypes.contains(mediaType)
                                            : parserSupportedMediaTypes.contains(mediaType) && includedMediaTypes.contains(mediaType);
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
                //TODO author=Horia Chiorean date=1/30/13 description=//TIKA 1.2 TXTParser seems to have a bug, always adding 1 ignorable whitespace to the actual chars to be parsed
                //https://issues.apache.org/jira/browse/TIKA-1069
                ContentHandler textHandler = writeLimit == null ? new BodyContentHandler() : new BodyContentHandler(writeLimit + 1);
                try {
                    LOGGER.debug("Using TikaTextExtractor to extract text");
                    // Parse the input stream ...
                    parser.parse(stream, textHandler, metadata, new ParseContext());
                } catch (SAXException sae) {
                    LOGGER.warn(TikaI18n.parseExceptionWhileExtractingText, sae.getMessage());
                } catch (NoClassDefFoundError ncdfe) {
                    LOGGER.warn(TikaI18n.warnNoClassDefFound, ncdfe.getMessage());
                } catch (Throwable e) {
                    LOGGER.error(e, TikaI18n.errorWhileExtractingTextFrom, e.getMessage());
                } finally {
                    // Record all of the text in the body ...
                    String text = textHandler.toString().trim();
                    if (!StringUtil.isBlank(text)) {
                        output.recordText(text);
                        LOGGER.debug("TikaTextExtractor found text: " + text);
                    }
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
            initLock.lock();
            try {
                if (parser == null) {
                    parser = new DefaultParser(this.getClass().getClassLoader());
                }
                LOGGER.debug("Initializing Tika Text Extractor");
                Map<MediaType, Parser> parsers = parser.getParsers();
                LOGGER.debug("Tika parsers found: {0}",parsers.size());
                for (MediaType mediaType : parsers.keySet()) {
                    parserSupportedMediaTypes.add(mediaType);
                    LOGGER.debug("Tika Text Extractor will support the {0} media-type",mediaType);
                }
                convertStringMimeTypesToMediaTypes(getExcludedMimeTypes(), excludedMediaTypes);
                convertStringMimeTypesToMediaTypes(getIncludedMimeTypes(), includedMediaTypes);
                LOGGER.debug("Initialized {0}", this);
            } finally {
                initLock.unlock();
            }
        }
        return parser;
    }

    private void convertStringMimeTypesToMediaTypes(Set<String> mimeTypes, Set<MediaType> mediaTypes) {
        for (String mimeTypeEntry : mimeTypes) {
            //allow each mime type entry to be an array in itself
            String[] multipleMimeTypes = mimeTypeEntry.split("[,\\s]");
            for (String mimeType : multipleMimeTypes) {
                if (StringUtil.isBlank(mimeType)) {
                    continue;
                }
                MediaType mediaType = MediaType.parse(mimeType.trim());
                if (mediaType == null) {
                    logger().debug("Invalid media type: {0}", mimeType);
                    continue;
                }
                mediaTypes.add(mediaType);
            }
        }
    }

    /**
     * Sets the write limit for the Tika parser, representing the maximum number of characters that should be extracted by the
     * TIKA parser.
     *
     * @param writeLimit an {@link Integer} which represents the write limit; may be null
     * @see BodyContentHandler#BodyContentHandler(int)
     */
    protected void setWriteLimit( Integer writeLimit ) {
        this.writeLimit = writeLimit;
    }

    protected Set<MediaType> getExcludedMediaTypes() {
        return excludedMediaTypes;
    }

    protected Set<MediaType> getIncludedMediaTypes() {
        return includedMediaTypes;
    }

    protected Set<MediaType> getParserSupportedMediaTypes() {
        return parserSupportedMediaTypes;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("TikaTextExtractor{");
        sb.append("excludedMediaTypes=").append(excludedMediaTypes);
        sb.append(", includedMediaTypes=").append(includedMediaTypes);
        sb.append(", parserSupportedMediaTypes=").append(parserSupportedMediaTypes);
        sb.append(", writeLimit=").append(writeLimit != null ? writeLimit : "unlimited");
        sb.append('}');
        return sb.toString();
    }
}
