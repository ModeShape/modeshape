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
package org.modeshape.web.jcr.webdav;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import net.sf.webdav.exceptions.WebdavException;

/**
 * Interface for a method of resolving a request into a repository name, workspace name, and node path. Implementations can use
 * additional information in the request (such as the {@link HttpServletRequest#getUserPrincipal() principal} to resolve the URI.
 * <p>
 * Implementations of this class must be thread-safe and must provide a public, nilary (no-argument) constructor.
 * </p>
 * 
 * @see SingleRepositoryRequestResolver
 * @see MultiRepositoryRequestResolver
 */
public interface RequestResolver {

    /**
     * Initialize the resolver based on the provided context
     * 
     * @param context the servlet context for this servlet
     */
    void initialize( ServletContext context );

    /**
     * Resolve the given request to the repository, workspace, and path of the node
     * 
     * @param request the request to be resolved
     * @param path the requested relative path; never null or empty
     * @return the repository, workspace, and path to a node
     * @throws WebdavException if the URI cannot be resolved to a repository, workspace, and path
     */
    ResolvedRequest resolve( HttpServletRequest request,
                             String path ) throws WebdavException;

}
