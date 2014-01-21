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

import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.text.TextEncoder;

/**
 * An interface defining methods to obtain a "readable" string representation.
 */
@Immutable
public interface Readable {

    /**
     * Get the string form of the object. A {@link Path#DEFAULT_ENCODER default encoder} is used to encode characters.
     * 
     * @return the encoded string
     * @see #getString(TextEncoder)
     */
    public String getString();

    /**
     * Get the encoded string form of the object, using the supplied encoder to encode characters.
     * 
     * @param encoder the encoder to use, or null if the {@link Path#DEFAULT_ENCODER default encoder} should be used
     * @return the encoded string
     * @see #getString()
     */
    public String getString( TextEncoder encoder );

    /**
     * Get the string form of the object, using the supplied namespace registry to convert any namespace URIs to prefixes. A
     * {@link Path#DEFAULT_ENCODER default encoder} is used to encode characters.
     * 
     * @param namespaceRegistry the namespace registry that should be used to obtain the prefix for any namespace URIs
     * @return the encoded string
     * @throws IllegalArgumentException if the namespace registry is null
     * @see #getString(NamespaceRegistry,TextEncoder)
     */
    public String getString( NamespaceRegistry namespaceRegistry );

    /**
     * Get the encoded string form of the object, using the supplied namespace registry to convert the any namespace URIs to
     * prefixes.
     * 
     * @param namespaceRegistry the namespace registry that should be used to obtain the prefix for the namespace URIs
     * @param encoder the encoder to use, or null if the {@link Path#DEFAULT_ENCODER default encoder} should be used
     * @return the encoded string
     * @throws IllegalArgumentException if the namespace registry is null
     * @see #getString(NamespaceRegistry)
     */
    public String getString( NamespaceRegistry namespaceRegistry,
                             TextEncoder encoder );

    /**
     * Get the encoded string form of the object, using the supplied namespace registry to convert the names' namespace URIs to
     * prefixes and the supplied encoder to encode characters, and using the second delimiter to encode (or convert) the delimiter
     * used between the namespace prefix and the local part of any names.
     * 
     * @param namespaceRegistry the namespace registry that should be used to obtain the prefix for the namespace URIs in the
     *        segment {@link Path.Segment#getName() names}
     * @param encoder the encoder to use for encoding the local part and namespace prefix of any names, or null if the
     *        {@link Path#DEFAULT_ENCODER default encoder} should be used
     * @param delimiterEncoder the encoder to use for encoding the delimiter between the local part and namespace prefix of any
     *        names, or null if the standard delimiter should be used
     * @return the encoded string
     * @see #getString(NamespaceRegistry)
     * @see #getString(NamespaceRegistry, TextEncoder)
     */
    public String getString( NamespaceRegistry namespaceRegistry,
                             TextEncoder encoder,
                             TextEncoder delimiterEncoder );
}
