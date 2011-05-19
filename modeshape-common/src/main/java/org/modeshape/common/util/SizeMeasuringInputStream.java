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
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicLong;

/**
 * An {@link InputStream} implementation that can wrap another input stream and determine the number of bytes read.
 */
public class SizeMeasuringInputStream extends InputStream {
    private final InputStream stream;
    private final AtomicLong size;

    public SizeMeasuringInputStream( InputStream stream,
                                     AtomicLong size ) {
        this.stream = stream;
        this.size = size;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.io.InputStream#read()
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
     * @see java.io.InputStream#read(byte[], int, int)
     */
    @Override
    public int read( byte[] b,
                     int off,
                     int len ) throws IOException {
        // Read from the stream ...
        int n = stream.read(b, off, len);
        if (n != -1) {
            size.addAndGet(n);
        }
        return n;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.io.InputStream#read(byte[])
     */
    @Override
    public int read( byte[] b ) throws IOException {
        int n = stream.read(b);
        if (n != -1) {
            size.addAndGet(n);
        }
        return n;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.io.InputStream#mark(int)
     */
    @Override
    public synchronized void mark( int readlimit ) {
        stream.mark(readlimit);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.io.InputStream#markSupported()
     */
    @Override
    public boolean markSupported() {
        return stream.markSupported();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.io.InputStream#available()
     */
    @Override
    public int available() throws IOException {
        return stream.available();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.io.InputStream#reset()
     */
    @Override
    public synchronized void reset() throws IOException {
        stream.reset();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.io.InputStream#close()
     */
    @Override
    public void close() throws IOException {
        stream.close();
    }
}
