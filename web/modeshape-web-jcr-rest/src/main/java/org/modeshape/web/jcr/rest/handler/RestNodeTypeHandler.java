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
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.StringUtil;
import org.modeshape.web.jcr.rest.RestHelper;
import org.modeshape.web.jcr.rest.model.RestNodeType;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Horia Chiorean
 */
public class RestNodeTypeHandler extends AbstractHandler {

    public RestNodeType getNodeType(HttpServletRequest request,
                                    String repositoryName,
                                    String workspaceName,
                                    String nodeTypeName) throws RepositoryException {
        Session session = getSession(request, repositoryName, workspaceName);
        NodeTypeManager nodeTypeManager = session.getWorkspace().getNodeTypeManager();
        NodeType nodeType = nodeTypeManager.getNodeType(nodeTypeName);
        return new RestNodeType(nodeType, RestHelper.repositoryUrl(request));
    }

    public Response importCND( HttpServletRequest request,
                               String repositoryName,
                               String workspaceName,
                               boolean allowUpdate,
                               InputStream cndInputStream ) throws RepositoryException {
        CheckArg.isNotNull(cndInputStream, "request body");
        Session session = getSession(request, repositoryName, workspaceName);
        NodeTypeManager nodeTypeManager = session.getWorkspace().getNodeTypeManager();
        if (! (nodeTypeManager instanceof org.modeshape.jcr.api.nodetype.NodeTypeManager)) {
            //not implemented
            return Response.status(Response.Status.fromStatusCode(501)).build();
        }
        org.modeshape.jcr.api.nodetype.NodeTypeManager modeshapeTypeManager = (org.modeshape.jcr.api.nodetype.NodeTypeManager) nodeTypeManager;
        try {
            List<RestNodeType> registeredTypes = registerCND(request, allowUpdate, cndInputStream, modeshapeTypeManager);
            return createOkResponse(registeredTypes);
        } catch (IOException e) {
            throw new RepositoryException(e);
        }
    }

    private Response createOkResponse( final List<RestNodeType> registeredTypes ) {
        GenericEntity<List<RestNodeType>> entity = new GenericEntity<List<RestNodeType>>(registeredTypes){};
        return Response.ok().entity(entity).build();
    }

    private List<RestNodeType> registerCND( HttpServletRequest request,
                                            boolean allowUpdate,
                                            InputStream cndInputStream,
                                            org.modeshape.jcr.api.nodetype.NodeTypeManager modeshapeTypeManager ) throws IOException, RepositoryException {
        NodeTypeIterator nodeTypeIterator = modeshapeTypeManager.registerNodeTypes(cndInputStream, allowUpdate);
        List<RestNodeType> result = new ArrayList<RestNodeType>();
        String baseUrl = RestHelper.repositoryUrl(request);
        while (nodeTypeIterator.hasNext()) {
            result.add(new RestNodeType(nodeTypeIterator.nextNodeType(), baseUrl));
        }
        return result;
    }
}
