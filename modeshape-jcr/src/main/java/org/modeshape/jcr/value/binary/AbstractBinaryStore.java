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

import java.util.concurrent.atomic.AtomicLong;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.api.mimetype.MimeTypeDetector;
import org.modeshape.jcr.mimetype.ExtensionBasedMimeTypeDetector;
import org.modeshape.jcr.text.NoOpTextExtractor;
import org.modeshape.jcr.text.TextExtractor;

/**
 * An abstract class for a {@link BinaryStore}, with common functionality needed by implementation classes.
 */
@ThreadSafe
public abstract class AbstractBinaryStore implements BinaryStore {

    private static final long LARGE_SIZE = 1 << 25; // 32MB
    private static final long MEDIUM_FILE_SIZE = 1 << 20; // 1MB
    private static final long SMALL_FILE_SIZE = 1 << 15; // 32K
    private static final long TINY_FILE_SIZE = 1 << 10; // 1K

    private static final int LARGE_BUFFER_SIZE = 1 << 20; // 1MB
    protected static final int MEDIUM_BUFFER_SIZE = 1 << 16; // 64K
    private static final int SMALL_BUFFER_SIZE = 1 << 12; // 4K
    private static final int TINY_BUFFER_SIZE = 1 << 11; // 2K

    public static int bestBufferSize( long fileSize ) {
        assert fileSize >= 0;
        if (fileSize < TINY_FILE_SIZE) return (int)fileSize + 2;
        if (fileSize < SMALL_FILE_SIZE) return TINY_BUFFER_SIZE;
        if (fileSize < MEDIUM_FILE_SIZE) return SMALL_BUFFER_SIZE;
        if (fileSize < LARGE_SIZE) return MEDIUM_BUFFER_SIZE;
        return LARGE_BUFFER_SIZE;
    }

    private final AtomicLong minBinarySizeInBytes = new AtomicLong(DEFAULT_MINIMUM_BINARY_SIZE_IN_BYTES);
    private volatile TextExtractor extractor = NoOpTextExtractor.INSTANCE;
    private volatile MimeTypeDetector detector = ExtensionBasedMimeTypeDetector.INSTANCE;

    @Override
    public long getMinimumBinarySizeInBytes() {
        return minBinarySizeInBytes.get();
    }

    @Override
    public void setMinimumBinarySizeInBytes( long minSizeInBytes ) {
        CheckArg.isNonNegative(minSizeInBytes, "minSizeInBytes");
        minBinarySizeInBytes.set(minSizeInBytes);
    }

    @Override
    public void setTextExtractor( TextExtractor textExtractor ) {
        this.extractor = textExtractor != null ? textExtractor : NoOpTextExtractor.INSTANCE;
    }

    @Override
    public void setMimeTypeDetector( MimeTypeDetector mimeTypeDetector ) {
        this.detector = mimeTypeDetector != null ? mimeTypeDetector : ExtensionBasedMimeTypeDetector.INSTANCE;
    }

    /**
     * Get the text extractor that can be used to extract text by this store.
     * 
     * @return the text extractor; never null
     */
    protected final TextExtractor extractor() {
        return this.extractor;
    }

    /**
     * Get the MIME type detector that can be used to find the MIME type for binary content
     * 
     * @return the detector; never null
     */
    protected final MimeTypeDetector detector() {
        return detector;
    }
}
