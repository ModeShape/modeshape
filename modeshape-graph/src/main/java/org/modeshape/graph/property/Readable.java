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
package org.modeshape.graph.property;

import net.jcip.annotations.Immutable;
import org.modeshape.common.text.TextEncoder;
import org.modeshape.graph.property.Path.Segment;

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
     *        segment {@link Segment#getName() names}
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
