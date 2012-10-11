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

    private final Logger logger;
    private final Cache<String, byte[]> blobCache;
    private final String key;

    protected int indexInBuffer;
    protected byte[] buffer;
    private int chunkNumber;


    public ChunkInputStream(Cache<String, byte[]> blobCache, String key){
        logger = Logger.getLogger(getClass());
        this.blobCache = blobCache;
        this.key = key;
    }

    @Override
    public int read() throws IOException {
        if(indexInBuffer == -1){
            return -1;
        }
        if(buffer == null || indexInBuffer >= buffer.length){
            fillBuffer();
            return read();
        }
        return buffer[indexInBuffer++] & 0xff;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if(indexInBuffer == -1){
            return -1;
        }
        if(buffer == null){
            fillBuffer();
            return read(b, off, len);
        }
        if(indexInBuffer >= buffer.length){
            return -1;
        }
        if (indexInBuffer + len > buffer.length){
            len = buffer.length - indexInBuffer;
        }
        System.arraycopy(buffer, indexInBuffer, b, off, len);
        indexInBuffer += len;
        if(indexInBuffer >= buffer.length){
            fillBuffer();
        }
        return len;
    }

    @Override
    public int available() {
        if (buffer == null) {
            fillBuffer();
        }
        return buffer.length - indexInBuffer;
    }

    @Override
    public final long skip(long n) throws IOException {
        if(n <= 0 || indexInBuffer == -1){
            return 0;
        }
        if(buffer == null){
            fillBuffer();
            return skip(n);
        }
        if (buffer.length + n > indexInBuffer){
            n = buffer.length - indexInBuffer;
        }
        if (n < 0){
            return 0;
        }
        indexInBuffer += n;
        return n;
    }

    private void fillBuffer() {

        buffer = nextChunk();
        if(buffer == null){
            buffer = new byte[0];
            indexInBuffer = -1;
        } else {
            indexInBuffer = 0;
        }
    }

    protected byte[] nextChunk() {
        String chunkKey = key+"-"+chunkNumber++;
        logger.debug("Read chunk {0}", chunkKey);
        return blobCache.get(chunkKey);
    }
}
