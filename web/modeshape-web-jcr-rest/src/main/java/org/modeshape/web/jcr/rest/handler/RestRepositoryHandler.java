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
