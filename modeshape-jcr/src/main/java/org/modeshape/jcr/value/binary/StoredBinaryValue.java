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

import java.io.IOException;
import java.io.InputStream;
import javax.jcr.RepositoryException;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;

/**
 * A {@link BinaryValue} implementation that gets the content from the {@link BinaryStore}.
 */
@Immutable
public class StoredBinaryValue extends AbstractBinary {

    private static final long serialVersionUID = 1L;

    private final transient BinaryStore store;
    private final long size;
    private transient String mimeType;

    public StoredBinaryValue( BinaryStore store,
                              BinaryKey key,
                              long size ) {
        super(key);
        this.store = store;
        this.size = size;
        assert this.store != null;
        assert this.size >= 0L;
    }

    @Override
    public long getSize() {
        return size;
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
    protected InputStream internalStream() throws RepositoryException {
        return store.getInputStream(getKey());
    }
    
    protected String mimeType() {
        return this.mimeType;
    }    
}
