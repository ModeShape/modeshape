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
import javax.jcr.RepositoryException;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.jcr.value.BinaryValue;

/**
 * An {@link InputStream} implementation that can be used to access the content of a supplied {@link BinaryValue} value. This class,
 * however, guarantees that the binary lock is {@link BinaryValue#dispose() released} whenever this class throws an exception or when
 * the instance is {@link #close() closed}.
 * <p>
 * The draft version of the JSR-283 specification outlines a new mechanism for obtaining a lock on a binary value, and in fact
 * this mechanism was used as the baseline for the design of ModeShape's Binary value. Therefore, when ModeShape's JCR
 * implementation supports JCR-283, this class will probably no longer be needed.
 * </p>
 */
@NotThreadSafe
class SelfClosingInputStream extends InputStream {

    private final BinaryValue binary;
    private InputStream stream;

    /**
     * Create a self-closing {@link InputStream} to access the content of the supplied {@link BinaryValue} value.
     * 
     * @param binary the {@link BinaryValue} object that this stream accesses; may not be null
     */
    public SelfClosingInputStream( BinaryValue binary ) {
        assert binary != null;
        this.binary = binary;
    }

    protected void open() throws RepositoryException {
        if (this.stream == null) {
            this.stream = binary.getStream();
        }
    }

    @Override
    public int available() throws IOException {
        try {
            open();
            return stream.available();
        } catch (RepositoryException e) {
            this.binary.dispose();
            throw new IOException(e);
        } catch (IOException e) {
            this.binary.dispose();
            throw e;
        } catch (RuntimeException e) {
            this.binary.dispose();
            throw e;
        }
    }

    @Override
    public void close() throws IOException {
        if (stream != null) {
            try {
                stream.close();
            } finally {
                this.binary.dispose();
            }
        }
    }

    @Override
    public boolean equals( Object obj ) {
        return binary.equals(obj);
    }

    @Override
    public int hashCode() {
        return binary.hashCode();
    }

    @Override
    public void mark( int readlimit ) {
        try {
            open();
            stream.mark(readlimit);
        } catch (RepositoryException e) {
            this.binary.dispose();
            throw new RuntimeException(e);
        } catch (RuntimeException e) {
            this.binary.dispose();
            throw e;
        }
    }

    @Override
    public boolean markSupported() {
        try {
            open();
            return stream.markSupported();
        } catch (RepositoryException e) {
            this.binary.dispose();
            throw new RuntimeException(e);
        } catch (RuntimeException e) {
            this.binary.dispose();
            throw e;
        }
    }

    @Override
    public int read( byte[] b,
                     int off,
                     int len ) throws IOException {
        try {
            open();
            int result = stream.read(b, off, len);
            if (result == -1) {
                // the end of the stream has been reached ...
                this.binary.dispose();
            }
            return result;
        } catch (RepositoryException e) {
            this.binary.dispose();
            throw new IOException(e);
        } catch (IOException e) {
            this.binary.dispose();
            throw e;
        } catch (RuntimeException e) {
            this.binary.dispose();
            throw e;
        }
    }

    @Override
    public int read( byte[] b ) throws IOException {
        try {
            open();
            int result = stream.read(b);
            if (result == -1) {
                // the end of the stream has been reached ...
                this.binary.dispose();
            }
            return result;
        } catch (RepositoryException e) {
            this.binary.dispose();
            throw new IOException(e);
        } catch (IOException e) {
            this.binary.dispose();
            throw e;
        } catch (RuntimeException e) {
            this.binary.dispose();
            throw e;
        }
    }

    @Override
    public int read() throws IOException {
        try {
            open();
            int result = stream.read();
            if (result == -1) {
                // the end of the stream has been reached ...
                this.binary.dispose();
            }
            return result;
        } catch (RepositoryException e) {
            this.binary.dispose();
            throw new IOException(e);
        } catch (IOException e) {
            this.binary.dispose();
            throw e;
        } catch (RuntimeException e) {
            this.binary.dispose();
            throw e;
        }
    }

    @Override
    public void reset() throws IOException {
        if (stream != null) {
            try {
                stream.reset();
            } catch (IOException e) {
                this.binary.dispose();
                throw e;
            } catch (RuntimeException e) {
                this.binary.dispose();
                throw e;
            }
        }
    }

    @Override
    public long skip( long n ) throws IOException {
        try {
            open();
            return stream.skip(n);
        } catch (RepositoryException e) {
            this.binary.dispose();
            throw new IOException(e);
        } catch (IOException e) {
            this.binary.dispose();
            throw e;
        } catch (RuntimeException e) {
            this.binary.dispose();
            throw e;
        }
    }

    @Override
    public String toString() {
        return binary.toString();
    }
}
