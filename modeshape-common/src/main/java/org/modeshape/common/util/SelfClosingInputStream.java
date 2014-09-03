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
