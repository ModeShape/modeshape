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
package org.modeshape.jcr;

import java.io.IOException;
import java.io.InputStream;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.graph.property.Binary;

/**
 * An {@link InputStream} implementation that can be used to access the content of a supplied {@link Binary} value. An instance of
 * this class immediately {@link Binary#acquire() acquires} the binary's lock, and allows the InputStream to be processed and used
 * normally. This class, however, guarantees that the binary lock is {@link Binary#release() released} whenever this class throws
 * an exception or when the instance is {@link #close() closed}.
 * <p>
 * The draft version of the JSR-283 specification outlines a new mechanism for obtaining a lock on a binary value, and in fact
 * this mechanism was used as the baseline for the design of ModeShape's Binary value. Therefore, when ModeShape's JCR
 * implementation supports JCR-283, this class will probably no longer be needed.
 * </p>
 */
@NotThreadSafe
class SelfClosingInputStream extends InputStream {

    private final Binary binary;
    private final InputStream stream;

    /**
     * Create a self-closing {@link InputStream} to access the content of the supplied {@link Binary} value. This construct
     * immediately {@link Binary#acquire() acquires} the binary's lock, which is {@link Binary#release() released} whenever this
     * class throws an exception or when the instance is {@link #close() closed}.
     * 
     * @param binary the {@link Binary} object that this stream accesses; may not be null
     */
    public SelfClosingInputStream( Binary binary ) {
        assert binary != null;
        this.binary = binary;
        this.binary.acquire();
        this.stream = binary.getStream();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.io.InputStream#available()
     */
    @Override
    public int available() throws IOException {
        try {
            return stream.available();
        } catch (IOException e) {
            this.binary.release();
            throw e;
        } catch (RuntimeException e) {
            this.binary.release();
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.io.InputStream#close()
     */
    @Override
    public void close() throws IOException {
        try {
            stream.close();
        } finally {
            this.binary.release();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        return stream.equals(obj);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return stream.hashCode();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.io.InputStream#mark(int)
     */
    @Override
    public void mark( int readlimit ) {
        try {
            stream.mark(readlimit);
        } catch (RuntimeException e) {
            this.binary.release();
            throw e;
        }
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
     * @see java.io.InputStream#read(byte[], int, int)
     */
    @Override
    public int read( byte[] b,
                     int off,
                     int len ) throws IOException {
        try {
            int result = stream.read(b, off, len);
            if (result == -1) {
                // the end of the stream has been reached ...
                this.binary.release();
            }
            return result;
        } catch (IOException e) {
            this.binary.release();
            throw e;
        } catch (RuntimeException e) {
            this.binary.release();
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.io.InputStream#read(byte[])
     */
    @Override
    public int read( byte[] b ) throws IOException {
        try {
            int result = stream.read(b);
            if (result == -1) {
                // the end of the stream has been reached ...
                this.binary.release();
            }
            return result;
        } catch (IOException e) {
            this.binary.release();
            throw e;
        } catch (RuntimeException e) {
            this.binary.release();
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.io.InputStream#read()
     */
    @Override
    public int read() throws IOException {
        try {
            int result = stream.read();
            if (result == -1) {
                // the end of the stream has been reached ...
                this.binary.release();
            }
            return result;
        } catch (IOException e) {
            this.binary.release();
            throw e;
        } catch (RuntimeException e) {
            this.binary.release();
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.io.InputStream#reset()
     */
    @Override
    public void reset() throws IOException {
        try {
            stream.reset();
        } catch (IOException e) {
            this.binary.release();
            throw e;
        } catch (RuntimeException e) {
            this.binary.release();
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.io.InputStream#skip(long)
     */
    @Override
    public long skip( long n ) throws IOException {
        try {
            return stream.skip(n);
        } catch (IOException e) {
            this.binary.release();
            throw e;
        } catch (RuntimeException e) {
            this.binary.release();
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        try {
            return stream.toString();
        } catch (RuntimeException e) {
            this.binary.release();
            throw e;
        }
    }
}
