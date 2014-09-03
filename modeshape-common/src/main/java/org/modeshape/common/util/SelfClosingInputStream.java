/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in ModeShape is licensed
 * to you under the terms of the GNU Lesser General Public License as
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
import org.modeshape.common.CommonI18n;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.common.logging.Logger;

/**
 * An {@link InputStream} implementation that wraps another stream and makes sure that {@link java.io.InputStream#close()}
 * is always called on the wrapped stream when there is no more content to read or when an unexpected exception occurs.
 */
@NotThreadSafe
public class SelfClosingInputStream extends InputStream {

    private static final Logger LOGGER = Logger.getLogger(SelfClosingInputStream.class);

    private final InputStream stream;

    /**
     * Create a self-closing {@link InputStream} that wraps another input stream.
     *
     * @param stream the wrapped {@link java.io.InputStream}; may not be null.
     */
    public SelfClosingInputStream( InputStream stream ) {
        assert stream != null;
        this.stream = stream;
    }

    @Override
    public int available() throws IOException {
        try {
            return stream.available();
        } catch (IOException e) {
            closeStream();
            throw e;
        } catch (RuntimeException e) {
            closeStream();
            throw e;
        }
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }

    @Override
    public int hashCode() {
        return stream.hashCode();
    }

    @Override
    public void mark( int readlimit ) {
        try {
            stream.mark(readlimit);
        } catch (RuntimeException e) {
            closeStream();
            throw e;
        }
    }

    @Override
    public boolean markSupported() {
        try {
            return stream.markSupported();
        } catch (RuntimeException e) {
            closeStream();
            throw e;
        }
    }

    @Override
    public int read( byte[] b,
                     int off,
                     int len ) throws IOException {
        try {
            int result = stream.read(b, off, len);
            if (result == -1) {
                // the end of the stream has been reached ...
                closeStream();
            }
            return result;
        } catch (IOException e) {
            closeStream();
            throw e;
        } catch (RuntimeException e) {
            closeStream();
            throw e;
        }
    }

    @Override
    public int read( byte[] b ) throws IOException {
        try {
            int result = stream.read(b);
            if (result == -1) {
                // the end of the stream has been reached ...
                closeStream();
            }
            return result;
        } catch (IOException e) {
            closeStream();
            throw e;
        } catch (RuntimeException e) {
            closeStream();
            throw e;
        }
    }

    @Override
    public int read() throws IOException {
        try {
            int result = stream.read();
            if (result == -1) {
                // the end of the stream has been reached ...
                closeStream();
            }
            return result;
        } catch (IOException e) {
            closeStream();
            throw e;
        } catch (RuntimeException e) {
            closeStream();
            throw e;
        }
    }

    @Override
    public void reset() throws IOException {
        try {
            stream.reset();
        } catch (IOException e) {
            closeStream();
            throw e;
        } catch (RuntimeException e) {
            closeStream();
            throw e;
        }
    }

    @Override
    public long skip( long n ) throws IOException {
        try {
            return stream.skip(n);
        } catch (IOException e) {
            closeStream();
            throw e;
        } catch (RuntimeException e) {
            closeStream();
            throw e;
        }
    }

    @Override
    public String toString() {
        return stream.toString();
    }

    /**
     * Returns the stream that this instance wraps.
     *
     * @return an {@link InputStream} instance, never null.
     */
    public InputStream wrappedStream() {
        return stream;
    }

    private void closeStream() {
        try {
            stream.close();
        } catch (IOException e) {
            LOGGER.error(e, CommonI18n.errorClosingWrappedStream);
        }
    }
}
