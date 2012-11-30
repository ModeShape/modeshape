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
package org.modeshape.jcr.value;

import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.jcr.value.binary.BinaryStore;
import org.modeshape.jcr.value.binary.BinaryStoreException;

/**
 * A factory for creating {@link BinaryValue} instances. This interface extends the {@link ValueFactory} generic interface and
 * adds specific methods for creating binary objects.
 */
@ThreadSafe
public interface BinaryFactory extends ValueFactory<BinaryValue> {

    @Override
    BinaryFactory with( ValueFactories valueFactories );

    /**
     * Return a potentially new copy of this factory that uses the supplied {@link BinaryStore} object.
     * 
     * @param binaryStore the binary store; may not be null
     * @return the factory, which may be a new instance or may be this object if the supplied value binary store is the same as
     *         used by this factory; never null
     */
    BinaryFactory with( BinaryStore binaryStore );

    /**
     * Find an existing binary value given the supplied binary key. If no such binary value exists, null is returned. This method
     * can be used when the caller knows the secure hash (e.g., from a previously-held Binary object), and would like to reuse an
     * existing binary value (if possible).
     * 
     * @param secureHash the secure hash of the binary content, which was probably {@link BinaryValue#getHexHash() obtained} from
     *        a previously-held {@link BinaryValue} object; a null or empty value is allowed, but will always result in returning
     *        null
     * @param size the size of the binary content
     * @return the existing Binary value that has the same secure hash; never null
     * @throws BinaryStoreException if there is a problem accessing the binary store
     */
    BinaryValue find( BinaryKey secureHash,
                      long size ) throws BinaryStoreException;
}
