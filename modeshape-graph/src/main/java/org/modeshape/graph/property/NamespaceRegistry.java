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

import java.util.Set;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;

/**
 * Registry of namespaces, which are used to provide isolated and independent domains for {@link Name names}.
 */
@ThreadSafe
public interface NamespaceRegistry {

    /**
     * Return the namespace URI that is currently mapped to the empty prefix, or null if there is no current default namespace.
     * 
     * @return the namespace URI that represents the default namespace, or null if there is no default namespace
     */
    String getDefaultNamespaceUri();

    /**
     * Get the namespace URI for the supplied prefix.
     * 
     * @param prefix the namespace prefix
     * @return the namespace URI for the supplied prefix, or null if there is no namespace currently registered to use that prefix
     * @throws IllegalArgumentException if the prefix is null
     */
    String getNamespaceForPrefix( String prefix );

    /**
     * Return the prefix used for the supplied namespace URI.
     * 
     * @param namespaceUri the namespace URI
     * @param generateIfMissing true if the namespace URI has not already been registered and the method should auto-register the
     *        namespace with a generated prefix, or false if the method should never auto-register the namespace
     * @return the prefix currently being used for the namespace, or <code>null</code> if the namespace has not been registered
     *         and <code>generateIfMissing</code> is <code>false</code>
     * @throws IllegalArgumentException if the namespace URI is null
     * @see #isRegisteredNamespaceUri(String)
     */
    String getPrefixForNamespaceUri( String namespaceUri,
                                     boolean generateIfMissing );

    /**
     * Return whether there is a registered prefix for the supplied namespace URI.
     * 
     * @param namespaceUri the namespace URI
     * @return true if the supplied namespace has been registered with a prefix, or false otherwise
     * @throws IllegalArgumentException if the namespace URI is null
     */
    boolean isRegisteredNamespaceUri( String namespaceUri );

    /**
     * Register a new namespace using the supplied prefix, returning the namespace URI previously registered under that prefix.
     * 
     * @param prefix the prefix for the namespace, or null if a namesapce prefix should be generated automatically
     * @param namespaceUri the namespace URI
     * @return the namespace URI that was previously registered with the supplied prefix, or null if the prefix was not previously
     *         bound to a namespace URI
     * @throws IllegalArgumentException if the namespace URI is null
     */
    String register( String prefix,
                     String namespaceUri );

    /**
     * Unregister the namespace with the supplied URI.
     * 
     * @param namespaceUri the namespace URI
     * @return true if the namespace was removed, or false if the namespace was not registered
     * @throws IllegalArgumentException if the namespace URI is null
     * @throws NamespaceException if there is a problem unregistering the namespace
     */
    boolean unregister( String namespaceUri );

    /**
     * Obtain the set of namespaces that are registered.
     * 
     * @return the set of
     */
    Set<String> getRegisteredNamespaceUris();

    /**
     * Obtain a snapshot of all of the {@link Namespace namespaces} registered at the time this method is called. The resulting
     * set is immutable, and will <i>not</i> reflect changes made to the registry.
     * 
     * @return an immutable set of {@link Namespace} objects reflecting a snapshot of the registry; never null
     */
    Set<Namespace> getNamespaces();

    /**
     * Representation of a single namespace at a single point in time. This object does not change to reflect changes made to the
     * {@link NamespaceRegistry registry}.
     * 
     * @see NamespaceRegistry#getNamespaces()
     * @author Randall Hauch
     */
    @Immutable
    interface Namespace extends Comparable<Namespace> {
        /**
         * Get the prefix for the namespace
         * 
         * @return the prefix; never null but possibly the empty string
         */
        String getPrefix();

        /**
         * Get the URI for the namespace
         * 
         * @return the namespace URI; never null but possibly the empty string
         */
        String getNamespaceUri();
    }

}
