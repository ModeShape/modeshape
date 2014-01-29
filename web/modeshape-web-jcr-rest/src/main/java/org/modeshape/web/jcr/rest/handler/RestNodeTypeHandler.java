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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;
import org.modeshape.common.util.CheckArg;
import org.modeshape.web.jcr.rest.RestHelper;
import org.modeshape.web.jcr.rest.model.RestNodeType;

/**
 * Class which handles {@link NodeType} operations for incoming http requests on {@link org.modeshape.web.jcr.rest.ModeShapeRestService}
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public final class RestNodeTypeHandler extends AbstractHandler {

    /**
     * HTTP response code for "Not Implemented"
     */
    private static final int HTTP_NOT_IMPLEMENTED = 501;

    /**
     * Retrieves the {@link RestNodeType rest node type representation} of the {@link NodeType} with the given name.
     *
     * @param request a non-null {@link HttpServletRequest}
     * @param repositoryName a non-null, URL encoded {@link String} representing the name of a repository
     * @param workspaceName a non-null, URL encoded {@link String} representing the name of a workspace
     * @param nodeTypeName a non-null, URL encoded {@link String} representing the name of type
     * @return a {@link RestNodeType} instance.
     * @throws RepositoryException if any JCR related operation fails, including if the node type cannot be found.
     */
    public RestNodeType getNodeType( HttpServletRequest request,
                                     String repositoryName,
                                     String workspaceName,
                                     String nodeTypeName ) throws RepositoryException {
        Session session = getSession(request, repositoryName, workspaceName);
        NodeTypeManager nodeTypeManager = session.getWorkspace().getNodeTypeManager();
        NodeType nodeType = nodeTypeManager.getNodeType(nodeTypeName);
        return new RestNodeType(nodeType, RestHelper.repositoryUrl(request));
    }

    /**
     * Imports a CND file into the repository, providing that the repository's {@link NodeTypeManager} is a valid ModeShape
     * node type manager.
     *
     * @param request a non-null {@link HttpServletRequest}
     * @param repositoryName a non-null, URL encoded {@link String} representing the name of a repository
     * @param workspaceName a non-null, URL encoded {@link String} representing the name of a workspace
     * @param allowUpdate a flag which indicates whether existing types should be updated or not.
     * @param cndInputStream a {@link InputStream} which is expected to be the input stream of a CND file.
     * @return a non-null {@link Response} instance
     * @throws RepositoryException if any JCR related operation fails
     */
    public Response importCND( HttpServletRequest request,
                               String repositoryName,
                               String workspaceName,
                               boolean allowUpdate,
                               InputStream cndInputStream ) throws RepositoryException {
        CheckArg.isNotNull(cndInputStream, "request body");
        Session session = getSession(request, repositoryName, workspaceName);
        NodeTypeManager nodeTypeManager = session.getWorkspace().getNodeTypeManager();
        if (!(nodeTypeManager instanceof org.modeshape.jcr.api.nodetype.NodeTypeManager)) {
            //501 = not implemented
            return Response.status(Response.Status.fromStatusCode(HTTP_NOT_IMPLEMENTED)).build();
        }
        org.modeshape.jcr.api.nodetype.NodeTypeManager modeshapeTypeManager = (org.modeshape.jcr.api.nodetype.NodeTypeManager)nodeTypeManager;
        try {
            List<RestNodeType> registeredTypes = registerCND(request, allowUpdate, cndInputStream, modeshapeTypeManager);
            return createOkResponse(registeredTypes);
        } catch (IOException e) {
            throw new RepositoryException(e);
        }
    }

    private Response createOkResponse( final List<RestNodeType> registeredTypes ) {
        GenericEntity<List<RestNodeType>> entity = new GenericEntity<List<RestNodeType>>(registeredTypes) {
        };
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
