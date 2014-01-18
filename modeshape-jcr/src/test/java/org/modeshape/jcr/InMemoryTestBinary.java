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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import org.modeshape.common.util.IoUtil;
import org.modeshape.common.util.SecureHash;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.api.Binary;

/**
 * An in-memory implementation of a {@link javax.jcr.Binary} which should used for tests.
 * 
 * @author Horia Chiorean
 */
public final class InMemoryTestBinary implements Binary {

    private byte[] bytes;
    private byte[] hash;

    public InMemoryTestBinary( byte[] bytes ) {
        try {
            this.bytes = bytes;
            this.hash = SecureHash.getHash(SecureHash.Algorithm.SHA_1, bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public InMemoryTestBinary( InputStream is ) throws IOException {
        this(IoUtil.readBytes(is));
    }

    @Override
    public void dispose() {
        this.bytes = null;
    }

    @Override
    public InputStream getStream() {
        return new ByteArrayInputStream(bytes);
    }

    @Override
    public int read( byte[] b,
                     long position ) throws IOException {
        if (getSize() <= position) {
            return -1;
        }
        InputStream stream = null;
        IOException error = null;
        try {
            stream = getStream();
            // Read/skip the next 'position' bytes ...
            long skip = position;
            while (skip > 0) {
                long skipped = stream.skip(skip);
                if (skipped <= 0) {
                    return -1;
                }
                skip -= skipped;
            }
            return stream.read(b);
        } catch (IOException e) {
            error = e;
            throw e;
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException t) {
                    // Only throw if we've not already thrown an exception ...
                    if (error == null) {
                        throw t;
                    }
                }
            }
        }
    }

    @Override
    public long getSize() {
        return bytes.length;
    }

    @Override
    public byte[] getHash() {
        return hash;
    }

    @Override
    public String getHexHash() {
        return StringUtil.getHexString(hash);
    }

    @Override
    public String getMimeType() {
        return null;
    }

    @Override
    public String getMimeType( String name ) {
        return null;
    }
}
