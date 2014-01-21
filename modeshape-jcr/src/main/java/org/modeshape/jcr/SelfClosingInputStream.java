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
