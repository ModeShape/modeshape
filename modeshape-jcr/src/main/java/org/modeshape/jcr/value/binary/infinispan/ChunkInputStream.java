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
package org.modeshape.jcr.value.binary.infinispan;

import org.infinispan.Cache;
import org.modeshape.common.logging.Logger;

import java.io.IOException;
import java.io.InputStream;

/**
 * Merges chunks from cache and provides InputStream-feeling.
 */
class ChunkInputStream extends InputStream {

    private static final Logger LOGGER = Logger.getLogger(ChunkInputStream.class);

    private final Cache<String, byte[]> blobCache;
    private final String key;
    private final int chunkSize;
    private final long totalSize;
    private final int chunksCount;

    protected int indexInBuffer;
    protected byte[] buffer;
    private int chunkNumber;

    protected ChunkInputStream( Cache<String, byte[]> blobCache,
                                String key,
                                int chunkSize,
                                long totalSize ) {
        this.blobCache = blobCache;
        this.key = key;
        this.chunkSize = chunkSize;
        this.totalSize = totalSize;
        this.chunkNumber = 0;
        this.indexInBuffer = 0;
        int remainderSize = (int) (totalSize % chunkSize);
        int numberOfChunks = (int) totalSize / chunkSize;
        this.chunksCount = remainderSize > 0 ? numberOfChunks + 1 : numberOfChunks;
    }

    @Override
    public int read() throws IOException {
        if (indexInBuffer == -1) {
            return -1;
        }
        if (buffer == null) {
            fillBufferWithFirstChunk();
            return read();
        } else if (indexInBuffer >= buffer.length) {
            fillBufferWithNextChunk();
            return read();
        }
        return buffer[indexInBuffer++] & 0xff;
    }

    @Override
    public int read( byte[] b,
                     int off,
                     int len ) throws IOException {
        if (indexInBuffer == -1) {
            return -1;
        }
        if (buffer == null) {
            fillBufferWithFirstChunk();
            return read(b, off, len);
        }
        if (indexInBuffer >= buffer.length) {
            return -1;
        }
        if (indexInBuffer + len > buffer.length) {
            len = buffer.length - indexInBuffer;
        }
        System.arraycopy(buffer, indexInBuffer, b, off, len);
        indexInBuffer += len;
        if (indexInBuffer >= buffer.length) {
            fillBufferWithNextChunk();
        }
        return len;
    }

    @Override
    public int available() {
        if (buffer == null) {
            fillBufferWithFirstChunk();
        }
        return buffer.length - indexInBuffer;
    }

    @Override
    public final long skip( long n ) throws IOException {
        if (n <= 0 || indexInBuffer == -1 || totalSize == 0) {
            return 0;
        }
        return directSkip(n);
    }

    @Override
    public void close() throws IOException {
        endOfStream();
    }

    private long directSkip( long n ) {
        long availableInBuffer = buffer != null ? (buffer.length - indexInBuffer) : chunkSize;

        if (n < availableInBuffer) {
            //we can skip "n" without requiring any additional chunks
            if (buffer == null) {
                //we haven't been initialized yet, so load the first chunk
                fillBufferWithFirstChunk();
            }
            indexInBuffer += n;
            return n;
        }

        //we need to skip past the current chunk, so find the chunk which needs to be loaded
        long lastChunkSize = totalSize - (chunksCount - 1) * chunkSize;
        int chunksAvailableToSkip = chunksCount - chunkNumber - 1;
        long bytesAvailableToSkip = (chunksAvailableToSkip - 1) * chunkSize + lastChunkSize;

        long stillRequiredToSkip = n - availableInBuffer;
        int chunksToSkipOver = (int) (stillRequiredToSkip / chunkSize);
        int leftToReadAfterSkip = (int) (stillRequiredToSkip % chunkSize);
        chunkNumber = chunkNumber + chunksToSkipOver + 1;   //chunk# is 0 based

        if (chunkNumber >= chunksCount) {
            //we would need to skip more chunks than we have
            endOfStream();
            return availableInBuffer + bytesAvailableToSkip;
        }
        //move directly to the required chunk
        fillBuffer(chunkNumber);
        if (buffer.length > leftToReadAfterSkip) {
            //move the pointer in this chunk
            indexInBuffer = leftToReadAfterSkip;
            return n;
        } else {
            //we jumped to a valid chunk, but it doesn't have enough data
            endOfStream();
            return availableInBuffer + bytesAvailableToSkip;
        }
    }

    private void fillBufferWithNextChunk() {
        this.chunkNumber++;
        fillBuffer(this.chunkNumber);
    }

    private void fillBufferWithFirstChunk() {
        fillBuffer(0);
    }

    private void fillBuffer(int chunkNumber) {
        buffer = readChunk(chunkNumber);
        if (buffer == null) {
            endOfStream();
        } else {
            indexInBuffer = 0;
        }
    }

    private void endOfStream() {
        buffer = new byte[0];
        indexInBuffer = -1;
        chunkNumber = -1;
    }

    private byte[] readChunk( int chunkNumber ) {
        String chunkKey = key + "-" + chunkNumber;
        LOGGER.debug("Read chunk {0}", chunkKey);
        return blobCache.get(chunkKey);
    }
}
