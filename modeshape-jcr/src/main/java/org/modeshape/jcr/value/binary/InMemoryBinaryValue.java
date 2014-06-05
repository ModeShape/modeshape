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
package org.modeshape.jcr.value.binary;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.jcr.RepositoryException;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;

/**
 * An implementation of {@link BinaryValue} that keeps the binary data in-memory.
 */
@Immutable
public class InMemoryBinaryValue extends AbstractBinary {

    private static final BinaryKey INVALID_KEY = new BinaryKey("invalid");

    private static final long serialVersionUID = 2L;

    private transient final BinaryStore store;
    private final byte[] bytes;
    private transient String mimeType;

    public InMemoryBinaryValue( BinaryStore store,
                                byte[] bytes ) {
        super(bytes != null ? BinaryKey.keyFor(bytes) : INVALID_KEY); // only need this until the CheckArg fails
        CheckArg.isNotNull(bytes, "bytes");
        this.bytes = bytes;
        this.store = store;
    }

    public InMemoryBinaryValue( BinaryStore store,
                                BinaryKey key,
                                byte[] bytes ) {
        super(key);
        CheckArg.isNotNull(bytes, "bytes");
        this.bytes = bytes;
        this.store = store;
    }

    @Override
    public long getSize() {
        return bytes.length;
    }

    public byte[] getBytes() {
        return this.bytes;
    }

    @Override
    public String getMimeType() throws IOException, RepositoryException {
        if (mimeType == null) {
            mimeType = store.getMimeType(this, null);
        }
        return mimeType;
    }

    @Override
    public String getMimeType( String name ) throws IOException, RepositoryException {
        if (mimeType == null) {
            mimeType = store.getMimeType(this, name);
        }
        return mimeType;
    }

    @Override
    protected InputStream internalStream() {
        return new ByteArrayInputStream(this.bytes);
    }
}
