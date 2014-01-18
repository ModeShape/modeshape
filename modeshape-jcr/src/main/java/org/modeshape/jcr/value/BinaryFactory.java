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
