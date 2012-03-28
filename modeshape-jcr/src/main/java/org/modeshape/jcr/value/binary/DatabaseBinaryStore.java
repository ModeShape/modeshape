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

import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.BinaryKey;

/**
 * A {@link BinaryStore} implementation that uses a database for persisting binary values.
 */
@ThreadSafe
public class DatabaseBinaryStore extends AbstractBinaryStore {

    @Override
    public BinaryValue storeValue( InputStream stream ) throws BinaryStoreException {
        throw new BinaryStoreException("Not implemented");
    }

    @Override
    public InputStream getInputStream( BinaryKey key ) throws BinaryStoreException {
        throw new BinaryStoreException("Not implemented");
    }

    @Override
    public void markAsUnused( Iterable<BinaryKey> keys ) throws BinaryStoreException {
        throw new BinaryStoreException("Not implemented");
    }

    @Override
    public void removeValuesUnusedLongerThan( long minimumAge,
                                              TimeUnit unit ) throws BinaryStoreException {
        throw new BinaryStoreException("Not implemented");
    }

    @Override
    public String getText( BinaryValue binary ) throws BinaryStoreException {
        throw new BinaryStoreException("Not implemented");
    }

    @Override
    public String getMimeType( BinaryValue binary,
                               String name ) {
        throw new UnsupportedOperationException("Not implemented");
    }

}
