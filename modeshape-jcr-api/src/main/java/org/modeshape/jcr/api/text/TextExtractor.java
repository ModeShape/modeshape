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
package org.modeshape.jcr.api.text;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import javax.jcr.RepositoryException;
import org.modeshape.jcr.api.Binary;
import org.modeshape.jcr.api.Logger;

/**
 * An abstraction for components that are able to extract text content from an input stream.
 */
public abstract class TextExtractor {

    /**
     * A logger instance
     */
    private Logger logger;

    /**
     * The name of this text extractor, which can be configured for monitoring purposes; set via reflection.
     */
    private String name;

    /**
     * the MIME types that are explicitly requested to be excluded; set via reflection.
     */
    private Set<String> excludedMimeTypes = new HashSet<String>();

    /**
     * the MIME types that are explicitly requested to be included; set via reflection.
     */
    private Set<String> includedMimeTypes = new HashSet<String>();

    /**
     * Determine if this extractor is capable of processing content with the supplied MIME type.
     * 
     * @param mimeType the MIME type; never null
     * @return true if this extractor can process content with the supplied MIME type, or false otherwise.
     */
    public abstract boolean supportsMimeType( String mimeType );

    /**
     * Extract text from the given {@link Binary}, using the given output to record the results.
     * 
     * @param binary the binary value that can be used in the extraction process; never <code>null</code>
     * @param output the output from the sequencing operation; never <code>null</code>
     * @param context the context for the sequencing operation; never <code>null</code>
     * @throws Exception if there is a problem during the extraction process
     */
    public abstract void extractFrom( Binary binary,
                                      TextExtractor.Output output,
                                      Context context ) throws Exception;

    /**
     * Allows subclasses to process the stream of binary value property in "safe" fashion, making sure the stream is closed at the
     * end of the operation.
     * 
     * @param binary a {@link org.modeshape.jcr.api.Binary} who is expected to contain a non-null binary value.
     * @param operation a {@link org.modeshape.jcr.api.text.TextExtractor.BinaryOperation} which should work with the stream
     * @param <T> the return type of the binary operation
     * @return whatever type of result the stream operation returns
     * @throws Exception if there is an error processing the stream
     */
    protected final <T> T processStream( Binary binary,
                                         BinaryOperation<T> operation ) throws Exception {
        InputStream stream = binary.getStream();
        if (stream == null) {
            throw new IllegalArgumentException("The binary value is empty");
        }

        try {
            return operation.execute(stream);
        } finally {
            stream.close();
        }
    }

    /**
     * Sets a logger instance.
     *
     * @param logger a {@link Logger}, never {@code null}
     */
    public final void setLogger( Logger logger ) {
        if (logger == null) {
            throw new IllegalArgumentException("Logger cannot be null");
        }
        this.logger = logger;
    }

    protected final Logger logger() {
        return logger;
    }

    protected Set<String> getExcludedMimeTypes() {
        return excludedMimeTypes;
    }

    protected Set<String> getIncludedMimeTypes() {
        return includedMimeTypes;
    }

    /**
     * Returns the text extractor name.
     *
     * @return a {@link String}, possibly {@code null}
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the text extractor name.
     *
     * @param  name a {@link String}, never {@code null}
     */
    public void setName( String name ) {
        this.name = name;
    }

    /**
     * Interface which can be used by subclasses to process the input stream of a binary property.
     * 
     * @param <T> the return type of the binary operation
     */
    protected interface BinaryOperation<T> {
        T execute( InputStream stream ) throws Exception;
    }

    /**
     * Interface which provides additional information to the text extractors, during the extraction operation.
     */
    public interface Context {

        /**
         * Determines the mime-type of the given binary value .
         *
         * @param name a symbolic name of the binary value (e.g. the name of a file); may be {@code null}
         * @param binaryValue a {@link Binary} instance which represents the binary data; may not be {@code null}
         * @return either a valid mime-type or {@code null} if it's not possible to determine the mimetype.
         * @throws RepositoryException if there are any problems accessing the binary value from the repository.
         * @throws IOException if there are any problems reading bytes from the binary value.
         */
        String mimeTypeOf( String name,
                           Binary binaryValue ) throws RepositoryException, IOException;

    }

    /**
     * The interface passed to a TextExtractor to which the extractor should record all text content.
     */
    public interface Output {

        /**
         * Record the text as being extracted. This method can be called multiple times during a single extract.
         * 
         * @param text the text extracted from the content.
         */
        void recordText( String text );
    }
}
