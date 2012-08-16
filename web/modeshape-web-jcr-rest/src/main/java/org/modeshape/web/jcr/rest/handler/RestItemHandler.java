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

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jboss.resteasy.spi.NotFoundException;
import org.modeshape.common.util.StringUtil;
import org.modeshape.web.jcr.rest.model.RestItem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * @author Horia Chiorean
 */
public class RestItemHandler extends ItemHandler {

    public RestItem item( HttpServletRequest request,
                          String rawRepositoryName,
                          String rawWorkspaceName,
                          String path,
                          int depth ) throws RepositoryException {
        Session session = getSession(request, rawRepositoryName, rawWorkspaceName);
        Item item = itemAtPath(path, session);
        return createRestItem(request, depth, session, item);
    }

    /**
     * Adds the content of the request as a node (or subtree of nodes) at the location specified by {@code path}.
     * <p>
     * The primary type and mixin type(s) may optionally be specified through the {@code jcr:primaryType} and
     * {@code jcr:mixinTypes} properties.
     * </p>
     *
     * @param request the servlet request; may not be null or unauthenticated
     * @param repositoryName the URL-encoded repository name
     * @param workspaceName the URL-encoded workspace name
     * @param path the path to the item
     * @param requestBody the JSON-encoded representation of the node or nodes to be added
     * @return the JSON-encoded representation of the node or nodes that were added. This will differ from {@code requestBody}
     *         in that auto-created and protected properties (e.g., jcr:uuid) will be populated.
     * @throws org.codehaus.jettison.json.JSONException if the request body cannot be translated into json
     * @throws RepositoryException if any other error occurs while interacting with the repository
     */
    public Response addItem( HttpServletRequest request,
                             String repositoryName,
                             String workspaceName,
                             String path,
                             String requestBody ) throws JSONException, RepositoryException {
        JSONObject requestBodyJSON = stringToJSONObject(requestBody);

        String parentAbsPath = parentPath(path);
        String newNodeName = newNodeName(path);

        Session session = getSession(request, repositoryName, workspaceName);
        Node parentNode = (Node)session.getItem(parentAbsPath);
        Node newNode = addNode(parentNode, newNodeName, requestBodyJSON);

        session.save();
        RestItem restNewNode = createRestItem(request, 0, session, newNode);
        return Response.status(Response.Status.CREATED).entity(restNewNode).build();
    }

    private String newNodeName( String path ) {
        int lastSlashInd = path.lastIndexOf('/');
        return lastSlashInd == -1 ? path : path.substring(lastSlashInd + 1);
    }

    private String parentPath( String path ) {
        int lastSlashInd = path.lastIndexOf('/');
        return lastSlashInd == -1 ? "/" : "/" + path.substring(0, lastSlashInd);
    }

    /**
     * Updates the properties at the path.
     * <p>
     * If path points to a property, this method expects the request content to be either a JSON array or a JSON string. The array
     * or string will become the values or value of the property. If path points to a node, this method expects the request
     * content to be a JSON object. The keys of the objects correspond to property names that will be set and the values for the
     * keys correspond to the values that will be set on the properties.
     * </p>
     *
     * @param request the servlet request; may not be null or unauthenticated
     * @param rawRepositoryName the URL-encoded repository name
     * @param rawWorkspaceName the URL-encoded workspace name
     * @param path the path to the item
     * @param requestContent the JSON-encoded representation of the values and, possibly, properties to be set
     * @return the JSON-encoded representation of the node on which the property or properties were set.
     * @throws JSONException if there is an error encoding the node
     * @throws RepositoryException if any error occurs at the repository level.
     */
    public RestItem updateItem( HttpServletRequest request,
                                String rawRepositoryName,
                                String rawWorkspaceName,
                                String path,
                                String requestContent ) throws JSONException, RepositoryException {
        Session session = getSession(request, rawRepositoryName, rawWorkspaceName);
        Item item = itemAtPath(path, session);
        item = updateItem(item, stringToJSONObject(requestContent));
        session.save();

        return createRestItem(request, 0, session, item);
    }

    private JSONObject stringToJSONObject( String requestBody ) throws JSONException {
        return StringUtil.isBlank(requestBody) ? new JSONObject() : new JSONObject(requestBody);
    }

    private JSONArray stringToJSONArray( String requestBody ) throws JSONException {
        return StringUtil.isBlank(requestBody) ? new JSONArray() : new JSONArray(requestBody);
    }

    public Response addItems( HttpServletRequest request,
                              String repositoryName,
                              String workspaceName,
                              String requestContent ) throws JSONException, RepositoryException {
        JSONObject requestBody = stringToJSONObject(requestContent);
        if (requestBody.length() == 0) {
            return Response.ok().build();
        }
        Session session = getSession(request, repositoryName, workspaceName);
        TreeMap<String, JSONObject> nodesByPath = createNodesByPathMap(requestBody);
        return addMultipleNodes(request, nodesByPath, session);
    }

    public Response updateItems( HttpServletRequest request,
                                 String repositoryName,
                                 String workspaceName,
                                 String requestContent ) throws JSONException, RepositoryException {
        JSONObject requestBody = stringToJSONObject(requestContent);
        if (requestBody.length() == 0) {
            return Response.ok().build();
        }
        Session session = getSession(request, repositoryName, workspaceName);
        Map<String, JSONObject> nodesByPath = createNodesByPathMap(requestBody);
        List<RestItem> result = updateMultipleNodes(request, session, nodesByPath);
        return createOkResponse(result);
    }

    public Response deleteItems( HttpServletRequest request,
                                 String repositoryName,
                                 String workspaceName,
                                 String requestContent) throws JSONException, RepositoryException {
        JSONArray requestArray = stringToJSONArray(requestContent);
        if (requestArray.length() == 0) {
            return Response.ok().build();
        }

        Session session = getSession(request, repositoryName, workspaceName);
        TreeSet<String> pathsInOrder = new TreeSet<String>();
        for (int i = 0; i < requestArray.length(); i++) {
            pathsInOrder.add(requestArray.get(i).toString());
        }
        List<String> pathsInOrderList = new ArrayList<String>(pathsInOrder);
        Collections.reverse(pathsInOrderList);
        for (String path : pathsInOrderList) {
            try {
                doDelete(path, session);
            } catch (NotFoundException e) {
                logger.info("Node at path {0} already deleted", path);
            }
        }
        session.save();
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    private List<RestItem> updateMultipleNodes( HttpServletRequest request,
                                                Session session,
                                                Map<String, JSONObject> nodesByPath ) throws RepositoryException, JSONException {
        List<RestItem> result = new ArrayList<RestItem>();
        for (String nodePath : nodesByPath.keySet()) {
            Item item = session.getItem(nodePath);
            item = updateItem(item, nodesByPath.get(nodePath));
            result.add(createRestItem(request, 0, session, item));
        }
        session.save();
        return result;
    }

    private TreeMap<String, JSONObject> createNodesByPathMap(JSONObject requestBodyJSON) throws JSONException {
        TreeMap<String, JSONObject> nodesByPath = new TreeMap<String, JSONObject>();
        for (Iterator<?> iterator = requestBodyJSON.keys(); iterator.hasNext(); ) {
            String nodePath = iterator.next().toString();
            JSONObject nodeJSON = requestBodyJSON.getJSONObject(nodePath);
            nodesByPath.put(nodePath, nodeJSON);
        }
        return nodesByPath;
    }

    private Response addMultipleNodes( HttpServletRequest request,
                                       TreeMap<String, JSONObject> nodesByPath,
                                       Session session ) throws RepositoryException, JSONException {
        List<RestItem> result = new ArrayList<RestItem>();

        for (String nodePath : nodesByPath.keySet()) {
            String parentAbsPath = parentPath(nodePath);
            String newNodeName = newNodeName(nodePath);

            Node parentNode = (Node)session.getItem(parentAbsPath);
            Node newNode = addNode(parentNode, newNodeName, nodesByPath.get(nodePath));
            RestItem restNewNode = createRestItem(request, 0, session, newNode);
            result.add(restNewNode);
        }

        session.save();
        return createOkResponse(result);
    }

    private Response createOkResponse( final List<RestItem> result ) {
        GenericEntity<List<RestItem>> entity = new GenericEntity<List<RestItem>>(result){};
        return Response.ok().entity(entity).build();
    }
}
