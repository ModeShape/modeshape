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

    @Override
    public int read() throws IOException {
        int result = stream.read();
        if (result != -1) {
            size.addAndGet(1);
        }
        return result;
    }

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

    @Override
    public int read( byte[] b ) throws IOException {
        int n = stream.read(b);
        if (n != -1) {
            size.addAndGet(n);
        }
        return n;
    }

    @Override
    public synchronized void mark( int readlimit ) {
        stream.mark(readlimit);
    }

    @Override
    public boolean markSupported() {
        return stream.markSupported();
    }

    @Override
    public int available() throws IOException {
        return stream.available();
    }

    @Override
    public synchronized void reset() throws IOException {
        stream.reset();
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }
}
