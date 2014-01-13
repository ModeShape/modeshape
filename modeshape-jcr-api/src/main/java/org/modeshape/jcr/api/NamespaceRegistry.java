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
package org.modeshape.jcr.api;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;

/**
 * An extension of JCR 2.0's {@link javax.jcr.NamespaceRegistry} interface, with a few ModeShape-specific enhancements.
 */
public interface NamespaceRegistry extends javax.jcr.NamespaceRegistry {

    /**
     * Determine if there is a namespace registered with the supplied prefix.
     * 
     * @param prefix the namespace prefix; may not be null
     * @return true if an existing namespace is registered to use the supplied prefix, or false otherwise
     * @throws RepositoryException if another error occurs.
     */
    boolean isRegisteredPrefix( String prefix ) throws RepositoryException;

    /**
     * Determine if there is a namespace registered with the supplied URI.
     * 
     * @param uri the namespace URI; may not be null
     * @return true if a namespace with the supplied URI is already registered, or false otherwise
     * @throws RepositoryException if another error occurs.
     */
    boolean isRegisteredUri( String uri ) throws RepositoryException;

    /**
     * Get the prefix for a registered namespace with the supplied URI or, if no such namespace is registered, register it with a
     * generated prefix and return that prefix.
     * 
     * @param uri The URI of the namespace; may not be null
     * @return the prefix of the already-registered namespace, or the newly-generated prefix if no such namespace was registered
     * @throws UnsupportedRepositoryOperationException if this repository does not support namespace registry changes.
     * @throws AccessDeniedException if the current session does not have sufficent access to register the namespace.
     * @throws RepositoryException if another error occurs.
     */
    public String registerNamespace( String uri )
        throws UnsupportedRepositoryOperationException, AccessDeniedException, RepositoryException;

}
