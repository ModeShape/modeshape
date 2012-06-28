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

import org.modeshape.jcr.api.Logger;
import java.io.IOException;
import java.io.InputStream;

/**
 * An abstraction for components that are able to extract text content from an input stream.
 */
public abstract class TextExtractor {

    private Logger logger;

    /**
     * Determine if this extractor is capable of processing content with the supplied MIME type.
     * 
     * @param mimeType the MIME type; never null
     * @return true if this extractor can process content with the supplied MIME type, or false otherwise.
     */
    public abstract boolean supportsMimeType( String mimeType );

    /**
     * Extract text from the given {@link InputStream}, using the given output to record the results.
     *
     * @param stream the stream with the data to be sequenced; never <code>null</code>
     * @param output the output from the sequencing operation; never <code>null</code>
     * @param context the context for the sequencing operation; never <code>null</code>
     * @throws IOException if there is a problem reading the stream
     */
    public abstract void extractFrom( InputStream stream,
                                      TextExtractorOutput output,
                                      Context context ) throws IOException;

    protected final void setLogger(Logger logger) {
        if (logger ==  null) {
            throw new IllegalArgumentException("Logger cannot be null");
        }
        this.logger = logger;
    }

    protected final Logger getLogger() {
        return logger;
    }

    /**
     * Interface which provides additional information to the text extractors, during the extraction operation.
     */
    public interface Context {

        /**
         * Returns a string path of the property on which the text extraction was triggered.
         *
         * @return a String representation of a path, using "/" as the separator between segments.
         */
        String getInputPropertyPath();

        /**
         * Returns the mime-type (if detected by ModeShape) of the content represented by the input stream.
         *
         * @return the mime-type of the input stream, or null if none was detected
         */
        String getMimeType();
    }

}
