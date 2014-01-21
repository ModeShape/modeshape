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

import java.net.URI;

/**
 * 
 */
public interface UriFactory extends ValueFactory<URI> {

    @Override
    UriFactory with( ValueFactories valueFactories );

    /**
     * Return a potentially new copy of this factory that uses the supplied {@link NamespaceRegistry.Holder} object.
     * 
     * @param namespaceRegistryHolder the holder of the namespace registry; may not be null
     * @return the factory, which may be a new instance or may be this object if the supplied namespace registry holder is the
     *         same as used by this factory; never null
     */
    UriFactory with( NamespaceRegistry.Holder namespaceRegistryHolder );

}
