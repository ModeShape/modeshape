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
import org.modeshape.jcr.mimetype.MimeTypeDetector;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;

/**
 * The basic interface for a store for Binary value objects. All binary values that are of a
 * {@link #getMinimumBinarySizeInBytes() minimum threshold size} are stored in the store; smaller binary values are kept in-memory
 * and are stored within the nodes.
 */
@ThreadSafe
public interface BinaryStore {

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
     * @throws BinaryStoreException if there any unexpected problem
     */
    BinaryValue storeValue( InputStream stream ) throws BinaryStoreException;

    /**
     * Get an {@link InputStream} to the binary content with the supplied key.
     * 
     * @param key the key to the binary content; never null
     * @return the input stream through which the content can be read, {@code never null}
     * @throws BinaryStoreException if there is a problem reading the content from the store or if a valid, non-null
     *         {@link InputStream} cannot be returned for the given key.
     */
    InputStream getInputStream( BinaryKey key ) throws BinaryStoreException;

    /**
     * Mark the supplied binary keys as unused, but key them in quarantine until needed again (at which point they're removed from
     * quarantine) or until {@link #removeValuesUnusedLongerThan(long, TimeUnit)} is called. This method ignores any keys for
     * values not stored within this store.
     * 
     * @param keys the keys for the binary values that are no longer needed
     * @throws BinaryStoreException if there is a problem marking any of the supplied binary values as unused
     */
    void markAsUnused( Iterable<BinaryKey> keys ) throws BinaryStoreException;

    /**
     * Remove binary values that have been {@link #markAsUnused(Iterable) unused} for at least the specified amount of time.
     * 
     * @param minimumAge the minimum time that a binary value has been {@link #markAsUnused(Iterable) unused} before it can be
     *        removed; must be non-negative
     * @param unit the time unit for the minimum age; may not be null
     * @throws BinaryStoreException if there is a problem removing the unused values
     */
    void removeValuesUnusedLongerThan( long minimumAge,
                                       TimeUnit unit ) throws BinaryStoreException;

    /**
     * Get the text that can be extracted from this binary content. If text extraction isn't enabled (either full text search is
     * not enabled or there aren't any configured extractors), this returns {@code null}
     * <p>
     * If extraction is enabled, this method may block until a text extractor has finished extracting the text.
     * </p>
     * <p>
     * If there are any problems either with the binary value or during the extraction process, the exception will be logged and
     * {@code null} is returned
     * </p>
     * In general, the implementation from {@link AbstractBinaryStore} should be enough and any custom {@link BinaryStore}
     * implementations aren't expected to implement this.
     * 
     * @param binary the binary content; may not be null
     * @return the extracted text, or null if none could be extracted
     * @throws BinaryStoreException if the binary content could not be accessed or if the given binary value cannot be found
     *         within the store.
     */
    String getText( BinaryValue binary ) throws BinaryStoreException;

    /**
     * Get the MIME type for this binary value, never {@code null}.
     * <p>
     * If the store has never determined the mime-type of the given binary and the binary can be located in the store, it will
     * attempt to determine it via the configured {@link MimeTypeDetector detectors} and store it.
     * </p>
     * 
     * @param binary the binary content; may not be null
     * @param name the name of the content, useful for determining the MIME type; may be null if not known
     * @return the MIME type of the content, as determined by the installed detectors or {@code null} if none of the detectors can
     *         determine it.
     * @throws IOException if there is a problem reading the binary content
     * @throws BinaryStoreException if the binary value cannot be found in the store
     * @throws RepositoryException if any other error occurs.
     */
    public String getMimeType( BinaryValue binary,
                               String name ) throws IOException, RepositoryException;

    /**
     * Obtain an iterable implementation containing all of the store's binary keys. The resulting iterator may be lazy, in the
     * sense that it may determine additional {@link BinaryKey}s only as the iterator is used.
     * 
     * @return the iterable set of binary keys; never null
     * @throws BinaryStoreException if anything unexpected happens.
     */
    Iterable<BinaryKey> getAllBinaryKeys() throws BinaryStoreException;

}
