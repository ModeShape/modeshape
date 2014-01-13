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

package org.modeshape.web.jcr.rest.handler;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import org.modeshape.web.jcr.rest.RestHelper;
import org.modeshape.web.jcr.rest.model.RestWorkspaces;

/**
 * An extension of the {@link RepositoryHandler} which returns POJO-based rest model instances.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public final class RestRepositoryHandler extends AbstractHandler {

    /**
     * Returns the list of workspaces available to this user within the named repository.
     *
     * @param request the servlet request; may not be null
     * @param repositoryName the name of the repository; may not be null
     * @return the list of workspaces available to this user within the named repository, as a {@link RestWorkspaces} object
     *
     * @throws RepositoryException if there is any other error accessing the list of available workspaces for the repository
     */
    public RestWorkspaces getWorkspaces( HttpServletRequest request,
                                         String repositoryName ) throws RepositoryException {
        assert request != null;
        assert repositoryName != null;

        RestWorkspaces workspaces = new RestWorkspaces();
        Session session = getSession(request, repositoryName, null);
        for (String workspaceName : session.getWorkspace().getAccessibleWorkspaceNames()) {
            String repositoryUrl = RestHelper.urlFrom(request);
            workspaces.addWorkspace(workspaceName, repositoryUrl);
        }
        return workspaces;
    }
}
