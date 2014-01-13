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
