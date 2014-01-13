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
package org.modeshape.web.jcr.webdav;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import org.modeshape.webdav.exceptions.WebdavException;

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
