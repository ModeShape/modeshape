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
package org.modeshape.jcr.value.binary;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import javax.jcr.RepositoryException;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.jcr.TextExtractors;
import org.modeshape.jcr.api.mimetype.MimeTypeDetector;
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.BinaryKey;

/**
 * The basic interface for a store for Binary value objects. All binary values that are of a
 * {@link #getMinimumBinarySizeInBytes() minimum threshold size} are stored in the store; smaller binary values are kept in-memory
 * and are stored within the nodes.
 */
@ThreadSafe
public interface BinaryStore {

    /**
     * The default minimum size (in bytes) of binary values that are persisted in the binary store is 4096 bytes, or 4KB.
     */
    public static final long DEFAULT_MINIMUM_BINARY_SIZE_IN_BYTES = 1024 * 4;

    /**
     * Get the minimum number of bytes that a binary value must contain before it can be stored in the binary store.
     * 
     * @return the minimum number of bytes for a stored binary value; never negative
     */
    long getMinimumBinarySizeInBytes();

    /**
     * Set the minimum number of bytes that a binary value must contain before it can be stored in the binary store.
     * 
     * @param minSizeInBytes the minimum number of bytes for a stored binary value; may not be negative
     */
    void setMinimumBinarySizeInBytes( long minSizeInBytes );

    /**
     * Set the text extractor that can be used for extracting text from binary content.
     *
     * @param textExtractors a non-null {@link TextExtractors} instance
     */
    void setTextExtractors( TextExtractors textExtractors );

    /**
     * Set the MIME type detector that can be used for determining the MIME type for binary content.
     * 
     * @param mimeTypeDetector the detector
     */
    void setMimeTypeDetector( MimeTypeDetector mimeTypeDetector );

    /**
     * Store the binary value and return the JCR representation. Note that if the binary content in the supplied stream is already
     * persisted in the store, the store may simply return the binary value referencing the existing content.
     * 
     * @param stream the stream containing the binary content to be stored; may not be null
     * @return the binary value representing the stored binary value; never null
     * @throws BinaryStoreException
     */
    BinaryValue storeValue( InputStream stream ) throws BinaryStoreException;

    /**
     * Get an {@link InputStream} to the binary content with the supplied key.
     * 
     * @param key the key to the binary content; never null
     * @return the input stream through which the content can be read
     * @throws BinaryStoreException if there is a problem reading the content from the store
     */
    InputStream getInputStream( BinaryKey key ) throws BinaryStoreException;

    /**
     * Mark the supplied binary keys as unused, but key them in quarantine until needed again (at which point they're removed from
     * quarantine) or until {@link #removeValuesUnusedLongerThan(long, TimeUnit)} is called. This method ignores any keys for
     * values not stored within this store.
     * <p>
     * Note that the implementation must <i>never</i> block.
     * </p>
     * 
     * @param keys the keys for the binary values that are no longer needed
     * @throws BinaryStoreException if there is a problem marking any of the supplied binary values as unused
     */
    void markAsUnused( Iterable<BinaryKey> keys ) throws BinaryStoreException;

    /**
     * Remove binary values that have been {@link #markAsUnused(Iterable) unused} for at least the specified amount of time.
     * <p>
     * Note that the implementation must <i>never</i> block.
     * </p>
     * 
     * @param minimumAge the minimum time that a binary value has been {@link #markAsUnused(Iterable) unused} before it can be
     *        removed; must be non-negative
     * @param unit the time unit for the minimum age; may not be null
     * @throws BinaryStoreException if there is a problem removing the unused values
     */
    void removeValuesUnusedLongerThan( long minimumAge,
                                       TimeUnit unit ) throws BinaryStoreException;

    /**
     * Get the text that can be extracted from this binary content. If text extraction isn't enabled (either full text search
     * is not enabled or there aren't any configured extractors), this returns {@code null}
     *
     * If extraction is enabled, this method may block until a text extractor has finished extracting the text.
     *
     * @param binary the binary content; may not be null
     * @return the extracted text, or null if none could be extracted
     * @throws BinaryStoreException if the binary content could not be accessed
     */
    String getText( BinaryValue binary ) throws BinaryStoreException;

    /**
     * Get the MIME type for this binary value.
     * 
     * @param binary the binary content; may not be null
     * @param name the name of the content, useful for determining the MIME type; may be null if not known
     * @return the MIME type, or null if it cannot be determined (e.g., the Binary is empty)
     * @throws IOException if there is a problem reading the binary content
     * @throws RepositoryException if an error occurs.
     */
    public String getMimeType( BinaryValue binary,
                               String name ) throws IOException, RepositoryException;

    /**
     * Stores the extracted text of a binary value into this store.
     *
     * @param source a {@code non-null} {@link BinaryValue} instance from which the text was extracted
     * @param extractedText a {@code non-null} and {@code non-blank} string representing the extracted text
     * @throws BinaryStoreException if the operation fails for whatever reason
     */
    void storeExtractedText(BinaryValue source, String extractedText) throws BinaryStoreException;

    /**
     * Retrieves the extracted text of a binary value, which may or may not have been stored previously.
     *
     * @param source a {@code non-null} {@link BinaryValue} instance from which the text was extracted
     * @return a {@code String} representing the extracted text, or {@code null} if such text hasn't been stored in this
     * store  previously.
     * @throws BinaryStoreException if the operation fails
     */
    String getExtractedText(BinaryValue source) throws BinaryStoreException;

}
