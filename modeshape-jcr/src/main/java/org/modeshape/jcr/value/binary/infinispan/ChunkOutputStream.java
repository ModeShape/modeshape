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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.infinispan.Cache;
import org.modeshape.common.logging.Logger;

/**
 * This stream writes data as chunks into separate cache entries.
 */
class ChunkOutputStream extends OutputStream {

    protected static final Logger LOGGER = Logger.getLogger(ChunkOutputStream.class);
    private static final int BUFFER_SIZE = 1024;

    protected final Cache<String, byte[]> blobCache;
    protected final String keyPrefix;
    protected int chunkIndex;

    private final ByteArrayOutputStream chunkBuffer;
    private final int chunkSize;
    private boolean closed;

    protected ChunkOutputStream( Cache<String, byte[]> blobCache,
                                 String keyPrefix ) {
        this(blobCache, keyPrefix, InfinispanBinaryStore.DEFAULT_CHUNK_SIZE);
    }

    protected ChunkOutputStream( Cache<String, byte[]> blobCache,
                                 String keyPrefix,
                                 int chunkSize ) {
        this.blobCache = blobCache;
        this.keyPrefix = keyPrefix;
        this.chunkIndex = 0;
        this.chunkBuffer = new ByteArrayOutputStream(BUFFER_SIZE);
        this.chunkSize = chunkSize;
    }

    protected int chunksCount() {
        return chunkIndex;
    }

    @Override
    public void write( int b ) throws IOException {
        if (chunkBuffer.size() == chunkSize) {
            storeBufferInBLOBCache();
        }
        chunkBuffer.write(b);
    }

    @Override
    public void write( byte[] b,
                       int off,
                       int len ) throws IOException {
        if (len + chunkBuffer.size() <= chunkSize) {
            chunkBuffer.write(b, off, len);
        } else {
            int storeLength = chunkSize - chunkBuffer.size();
            write(b, off, storeLength);
            storeBufferInBLOBCache();
            write(b, off + storeLength, len - storeLength);
        }
    }

    @Override
    public void close() throws IOException {
        LOGGER.debug("Close. Buffer size at close: {0}", chunkBuffer.size());
        if (closed) {
            LOGGER.debug("Stream already closed.");
            return;
        }
        closed = true;
        // store last chunk
        if (chunkBuffer.size() > 0) {
            storeBufferInBLOBCache();
        }
    }

    private void storeBufferInBLOBCache() throws IOException {
        final byte[] chunk = chunkBuffer.toByteArray();
        try {
            new RetryOperation() {
                @Override
                protected boolean call() {
                    String chunkKey = keyPrefix + "-" + chunkIndex;
                    LOGGER.debug("Store chunk {0}", chunkKey);
                    blobCache.put(chunkKey, chunk);
                    return true;
                }
            }.doTry();
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
        chunkIndex++;
        chunkBuffer.reset();
    }
}
