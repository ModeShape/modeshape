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
