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
import org.modeshape.common.text.TextDecoder;

/**
 * A factory for creating {@link Name names}.
 */
@ThreadSafe
public interface NameFactory extends ValueFactory<Name> {

    public static interface Holder {
        NameFactory getNameFactory();
    }

    @Override
    NameFactory with( ValueFactories valueFactories );

    /**
     * Return a potentially new copy of this factory that uses the supplied {@link NamespaceRegistry.Holder} object.
     * 
     * @param namespaceRegistryHolder the holder of the namespace registry; may not be null
     * @return the factory, which may be a new instance or may be this object if the supplied namespace registry holder is the
     *         same as used by this factory; never null
     */
    NameFactory with( NamespaceRegistry.Holder namespaceRegistryHolder );

    /**
     * Create a name from the given namespace URI and local name.
     * <p>
     * This method is equivalent to calling {@link #create(String, String, TextDecoder)} with a null encoder.
     * </p>
     * 
     * @param namespaceUri the namespace URI
     * @param localName the local name
     * @return the new name
     * @throws IllegalArgumentException if the local name is <code>null</code> or empty
     */
    Name create( String namespaceUri,
                 String localName );

    /**
     * Create a name from the given namespace URI and local name.
     * 
     * @param namespaceUri the namespace URI
     * @param localName the local name
     * @param decoder the decoder that should be used to decode the qualified name
     * @return the new name
     * @throws IllegalArgumentException if the local name is <code>null</code> or empty
     */
    Name create( String namespaceUri,
                 String localName,
                 TextDecoder decoder );

    /**
     * Get the namespace registry.
     * 
     * @return the namespace registry; never <code>null</code>
     */
    NamespaceRegistry getNamespaceRegistry();
}
