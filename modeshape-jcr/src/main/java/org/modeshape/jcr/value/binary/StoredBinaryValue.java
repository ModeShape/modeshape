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
package org.modeshape.jcr.value.binary;

import java.io.IOException;
import java.io.InputStream;
import javax.jcr.RepositoryException;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.BinaryKey;

/**
 * A {@link BinaryValue} implementation that gets the content from the {@link BinaryStore}.
 */
@Immutable
public class StoredBinaryValue extends AbstractBinary {

    private static final long serialVersionUID = 1L;

    private final BinaryStore store;
    private final long size;
    private transient String mimeType;

    public StoredBinaryValue( BinaryStore store,
                              BinaryKey key,
                              long size ) {
        super(key);
        this.store = store;
        this.size = size;
        assert this.store != null;
        assert this.size > 0L;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public InputStream getStream() throws BinaryStoreException {
        // Delegate to the store ...
        return store.getInputStream(getKey());
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
}
