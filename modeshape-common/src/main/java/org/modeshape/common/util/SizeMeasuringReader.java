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
package org.modeshape.common.util;

import java.io.IOException;
import java.io.Reader;
import java.util.concurrent.atomic.AtomicLong;

/**
 * An {@link Reader} implementation that can wrap another input stream and determine the number of bytes read.
 */
public class SizeMeasuringReader extends Reader {
    private final Reader stream;
    private final AtomicLong size;

    public SizeMeasuringReader( Reader Reader,
                                AtomicLong size ) {
        this.stream = Reader;
        this.size = size;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.io.Reader#read(char[], int, int)
     */
    @Override
    public int read( char[] cbuf,
                     int off,
                     int len ) throws IOException {
        // Read from the stream ...
        int n = stream.read(cbuf, off, len);
        if (n != -1) {
            size.addAndGet(n);
        }
        return n;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.io.Reader#read()
     */
    @Override
    public int read() throws IOException {
        int result = stream.read();
        if (result != -1) {
            size.addAndGet(1);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.io.Reader#mark(int)
     */
    @Override
    public synchronized void mark( int readlimit ) throws IOException {
        stream.mark(readlimit);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.io.Reader#markSupported()
     */
    @Override
    public boolean markSupported() {
        return stream.markSupported();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.io.Reader#reset()
     */
    @Override
    public synchronized void reset() throws IOException {
        stream.reset();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.io.Reader#close()
     */
    @Override
    public void close() throws IOException {
        stream.close();
    }
}
