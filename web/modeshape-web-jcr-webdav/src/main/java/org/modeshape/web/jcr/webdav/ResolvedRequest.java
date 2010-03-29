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

/**
 * Returns the output of {@link RequestResolver#resolve(javax.servlet.http.HttpServletRequest)}, namely the repository and
 * workspace that URI, as well as a {@link UriResolver} for that request.
 */
public final class ResolvedRequest {
    private final String repositoryName;
    private final String workspaceName;
    private final UriResolver uriResolver;

    public ResolvedRequest( String repositoryName,
                            String workspaceName,
                            UriResolver uriResolver ) {
        super();
        this.repositoryName = repositoryName;
        this.workspaceName = workspaceName;
        this.uriResolver = uriResolver;
    }

    /**
     * @return the resolved repository name
     */
    public String getRepositoryName() {
        return repositoryName;
    }

    /**
     * @return the resolved workspace name
     */
    public String getWorkspaceName() {
        return workspaceName;
    }

    /**
     * @return the URI resolver
     */
    public UriResolver getUriResolver() {
        return uriResolver;
    }

}
