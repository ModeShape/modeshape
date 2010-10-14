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

import javax.servlet.http.HttpServletRequest;

/**
 * The resolved repository name, workspace name, and path of node for a given {@link HttpServletRequest request}.
 */
public final class ResolvedRequest {
    private final String repositoryName;
    private final String workspaceName;
    private final String path;
    private final HttpServletRequest request;

    public ResolvedRequest( HttpServletRequest request,
                            String repositoryName,
                            String workspaceName,
                            String path ) {
        super();
        this.request = request;
        assert this.request != null;
        this.repositoryName = repositoryName;
        this.workspaceName = workspaceName;
        this.path = path;
    }

    /**
     * Get the request.
     * 
     * @return request the request; never null
     */
    public HttpServletRequest getRequest() {
        return request;
    }

    /**
     * Get the name of the repository.
     * 
     * @return the repository name; may be null if the request did not resolve to a repository
     */
    public String getRepositoryName() {
        return repositoryName;
    }

    /**
     * Get the name of the workspace.
     * 
     * @return the workspace name; may be null if the request did not resolve to a node
     */
    public String getWorkspaceName() {
        return workspaceName;
    }

    /**
     * Get the path to the node.
     * 
     * @return the path of the node; may be null if the request did not resolve to a node
     */
    public String getPath() {
        return path;
    }

    /**
     * Create a new request that is similar to this request except with the supplied path. This can only be done if the repository
     * name and workspace name are non-null
     * 
     * @param path the new path
     * @return the new request; never null
     */
    public ResolvedRequest withPath( String path ) {
        assert repositoryName != null;
        assert workspaceName != null;
        return new ResolvedRequest(request, repositoryName, workspaceName, path);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "/" + repositoryName + "/" + workspaceName + path;
    }

}
