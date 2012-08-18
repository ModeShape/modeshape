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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * This stream writes data as chunks into separate cache entries.
 */
class ChunkOutputStream extends OutputStream {

    protected final Logger logger;

    public static final int CHUNKSIZE = 1024 * 1024 * 1; // 1 MB

    protected final Cache<String, byte[]> blobCache;
    protected final String keyPrefix;
    private ByteArrayOutputStream chunkBuffer;
    private boolean closed;
    protected int chunkIndex;

    public ChunkOutputStream(Cache<String, byte[]> blobCache, String keyPrefix) {
        logger = Logger.getLogger(getClass());
        this.blobCache = blobCache;
        this.keyPrefix = keyPrefix;
        chunkBuffer = new ByteArrayOutputStream(1024);
    }

    /**
     * @return Number of chunks stored.
     */
    public int getNumberChunks(){
        return chunkIndex;
    }

    @Override
    public void write(int b) throws IOException {
        if (chunkBuffer.size() == CHUNKSIZE) {
            storeBufferInBLOBCache();
        }
        chunkBuffer.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if(len + chunkBuffer.size() <= CHUNKSIZE){
            chunkBuffer.write(b, off, len);
        } else {
            int storeLength = CHUNKSIZE - chunkBuffer.size();
            write(b, off, storeLength);
            storeBufferInBLOBCache();
            write(b, off + storeLength, len - storeLength);
        }
    }

    @Override
    public void close() throws IOException {
        logger.debug("Close. Buffer size at close: {0}", chunkBuffer.size());
        if(closed){
            logger.debug("Stream already closed.");
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
        new RetryOperation(){
            @Override
            protected void call() {
                String chunkKey = keyPrefix +"-"+chunkIndex;
                logger.debug("Store chunk {0}", chunkKey);
                blobCache.put(chunkKey, chunk);
            }
        }.doTry();
        chunkIndex++;
        chunkBuffer.reset();
    }

}
