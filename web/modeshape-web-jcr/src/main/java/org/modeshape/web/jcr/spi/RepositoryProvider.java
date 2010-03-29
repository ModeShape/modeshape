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
package org.modeshape.web.jcr.spi;

import java.util.Set;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

/**
 * Interface for any class that provides access to one or more local JCR repositories.
 * <p>
 * Repository providers must provide a public, no-argument constructor and be thread-safe.
 * </p>
 */
public interface RepositoryProvider {

    /**
     * Returns an active session for the given workspace name in the named repository.
     * <p>
     * JCR implementations that do not support multiple repositories on the same server can ignore the repositoryName parameter.
     * </p>
     * 
     * @param request the servlet request; may not be null or unauthenticated
     * @param repositoryName the name of the repository in which the session is created
     * @param workspaceName the name of the workspace to which the session should be connected
     * @return an active session with the given workspace in the named repository
     * @throws RepositoryException if any other error occurs
     */
    public Session getSession( HttpServletRequest request,
                               String repositoryName,
                               String workspaceName ) throws RepositoryException;

    /**
     * Returns the available repository names
     * <p>
     * JCR implementations that do not support multiple repositories on the same server should provide a singleton set containing
     * some default repository name.
     * </p>
     * 
     * @return the available repository names; may not be null or empty
     */
    Set<String> getJcrRepositoryNames();

    /**
     * Signals the repository provider that it should initialize itself based on the provided {@link ServletContext servlet
     * context} and begin accepting connections.
     * 
     * @param context the servlet context for the REST servlet
     */
    void startup( ServletContext context );

    /**
     * Signals the repository provider that it should complete any pending transactions, shutdown, and release any external
     * resource held.
     */
    void shutdown();

}
