/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.spi.graph;

import java.util.Set;
import net.jcip.annotations.ThreadSafe;

/**
 * Registry of namespaces, which are used to provide isolated and independent domains for {@link Name names}.
 * @author Randall Hauch
 */
@ThreadSafe
public interface NamespaceRegistry {

    /**
     * Return the namespace URI that is currently mapped to the empty prefix, or null if there is no current default namespace.
     * @return the namespace URI that represents the default namespace, or null if there is no default namespace
     */
    String getDefaultNamespaceUri();

    /**
     * Get the namespace URI for the supplied prefix.
     * @param prefix the namespace prefix
     * @return the namespace URI for the supplied prefix, or null if there is no namespace currently registered to use that prefix
     * @throws IllegalArgumentException if the prefix is null
     */
    String getNamespaceForPrefix( String prefix );

    /**
     * Return the prefix used for the supplied namespace URI.
     * @param namespaceUri the namespace URI
     * @param generateIfMissing true if the namespace URI has not already been registered and the method should auto-register the
     * namespace with a generated prefix, or false if the method should never auto-register the namespace
     * @return the prefix currently being used for the namespace; never null but possibly empty for the default namespace
     * @throws IllegalArgumentException if the namespace URI is null
     * @see #isRegisteredNamespaceUri(String)
     */
    String getPrefixForNamespaceUri( String namespaceUri, boolean generateIfMissing );

    /**
     * Return whether there is a registered prefix for the supplied namespace URI.
     * @param namespaceUri the namespace URI
     * @return true if the supplied namespace has been registered with a prefix, or false otherwise
     * @throws IllegalArgumentException if the namespace URI is null
     */
    boolean isRegisteredNamespaceUri( String namespaceUri );

    /**
     * Register a new namespace using the supplied prefix, returning the namespace URI previously registered under that prefix.
     * @param prefix the prefix for the namespace, or null if a namesapce prefix should be generated automatically
     * @param namespaceUri the namespace URI
     * @return the namespace URI that was previously registered with the supplied prefix, or null if the prefix was not previously
     * bound to a namespace URI
     * @throws IllegalArgumentException if the namespace URI is null
     */
    String register( String prefix, String namespaceUri );

    /**
     * Obtain the set of namespaces that are registered.
     * @return the set of
     */
    Set<String> getRegisteredNamespaceUris();

}
