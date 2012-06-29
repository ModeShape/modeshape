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
package org.modeshape.jcr.api.text;

import javax.jcr.RepositoryException;
import org.modeshape.jcr.api.Binary;
import org.modeshape.jcr.api.Logger;
import java.io.IOException;
import java.io.InputStream;

/**
 * An abstraction for components that are able to extract text content from an input stream.
 */
public abstract class TextExtractor {

    private Logger logger;

    /**
     * The name of this text extraction, which can be configured for monitoring purposes
     */
    private String name;

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
     * @param binary a {@link org.modeshape.jcr.api.Binary} who is expected to contain a non-null binary value.
     * @param operation a {@link org.modeshape.jcr.api.text.TextExtractor.BinaryOperation} which should work with the stream
     * @return whatever type of result the stream operation returns
     * @throws RepositoryException
     * @throws IOException
     */
    protected final <T> T processStream(Binary binary, BinaryOperation<T> operation) throws Exception {
        InputStream stream = binary.getStream();
        if (stream == null) {
            throw new IllegalArgumentException("The binary value is empty");
        }

        try {
            return operation.execute(stream);
        }
        finally {
            stream.close();
        }
    }

    public final void setLogger(Logger logger) {
        if (logger ==  null) {
            throw new IllegalArgumentException("Logger cannot be null");
        }
        this.logger = logger;
    }

    protected final Logger getLogger() {
        return logger;
    }

    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    /**
     * Interface which can be used by subclasses to process the input stream of a binary property.
     */
    protected interface BinaryOperation<T> {
        T execute(InputStream stream) throws Exception;
    }

    /**
     * Interface which provides additional information to the text extractors, during the extraction operation.
     */
    public interface Context {

        /**
         * Returns the path to the input node (the owner node) of the property which has the binary value that triggered
         * the sequencing.
         *
         * @return the path of the owning node, never {@code null}
         */
        String getInputNodePath();
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
