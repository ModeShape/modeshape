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
