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

import net.jcip.annotations.ThreadSafe;
import org.modeshape.common.text.TextDecoder;

/**
 * A factory for creating {@link Name names}.
 */
@ThreadSafe
public interface NameFactory extends ValueFactory<Name> {

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
