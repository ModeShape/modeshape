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
    public final long skip( long n ) {
        if (n <= 0 || indexInBuffer == -1 || totalSize == 0) {
            return 0;
        }
        return directSkip(n);
    }

    @Override
    public void close() {
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
        }
        //we jumped to a valid chunk, but it doesn't have enough data
        endOfStream();
        return availableInBuffer + bytesAvailableToSkip;
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
