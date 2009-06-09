/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.web.jcr.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import net.jcip.annotations.Immutable;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jboss.dna.common.text.UrlEncoder;
import org.jboss.dna.web.jcr.rest.model.RepositoryEntry;
import org.jboss.dna.web.jcr.rest.model.WorkspaceEntry;
import org.jboss.resteasy.spi.NotFoundException;
import org.jboss.resteasy.spi.UnauthorizedException;

/**
 * RESTEasy handler to provide the JCR resources at the URIs below. Please note that these URIs assume a context of {@code
 * /resources} for the web application.
 * <table border="1">
 * <tr>
 * <th>URI Pattern</th>
 * <th>Description</th>
 * <th>Supported Methods</th>
 * </tr>
 * <tr>
 * <td>/resources</td>
 * <td>returns a list of accessible repositories</td>
 * <td>GET</td>
 * </tr>
 * <tr>
 * <td>/resources/{repositoryName}</td>
 * <td>returns a list of accessible workspaces within that repository</td>
 * <td>GET</td>
 * </tr>
 * <tr>
 * <td>/resources/{repositoryName}/{workspaceName}</td>
 * <td>returns a list of operations within the workspace</td>
 * <td>GET</td>
 * </tr>
 * <tr>
 * <td>/resources/{repositoryName}/{workspaceName}/item/{path}</td>
 * <td>accesses the item (node or property) at the path</td>
 * <td>ALL</td>
 * </tr>
 * </table>
 */
@Immutable
@Path( "/" )
public class JcrResources {

    private static final UrlEncoder URL_ENCODER = new UrlEncoder();

    private static final String PROPERTIES_HOLDER = "properties";
    private static final String CHILD_NODE_HOLDER = "children";

    private static final String PRIMARY_TYPE_PROPERTY = "jcr:primaryType";
    private static final String MIXIN_TYPES_PROPERTY = "jcr:mixinTypes";

    /** Name to be used when the repository name is empty string as {@code "//"} is not a valid path. */
    public static final String EMPTY_REPOSITORY_NAME = "<default>";
    /** Name to be used when the workspace name is empty string as {@code "//"} is not a valid path. */
    public static final String EMPTY_WORKSPACE_NAME = "<default>";

    /**
     * Returns an active session for the given workspace name in the named repository.
     * 
     * @param request the servlet request; may not be null or unauthenticated
     * @param rawRepositoryName the URL-encoded name of the repository in which the session is created
     * @param rawWorkspaceName the URL-encoded name of the workspace to which the session should be connected
     * @return an active session with the given workspace in the named repository
     * @throws RepositoryException if any other error occurs
     */
    private Session getSession( HttpServletRequest request, 
                                String rawRepositoryName,
                                String rawWorkspaceName ) throws NotFoundException, RepositoryException {
        assert request != null;
        assert request.getUserPrincipal() != null: "Request must be authorized";

        // Sanity check
        if (request.getUserPrincipal() == null) {
            throw new UnauthorizedException("Client is not authorized");
        }
        
        return RepositoryFactory.getSession(request, repositoryNameFor(rawRepositoryName), workspaceNameFor(rawWorkspaceName));
    }

    /**
     * Returns the list of JCR repositories available on this server
     * 
     * @param request the servlet request; may not be null
     * @return the list of JCR repositories available on this server
     */
    @GET
    @Path( "/" )
    @Produces( "application/json" )
    public Map<String, RepositoryEntry> getRepositories( @Context HttpServletRequest request ) {
        assert request != null;

        Map<String, RepositoryEntry> repositories = new HashMap<String, RepositoryEntry>();

        for (String name : RepositoryFactory.getJcrRepositoryNames()) {
            if (name.trim().length() == 0) {
                name = EMPTY_REPOSITORY_NAME;
            }
            name = URL_ENCODER.encode(name);
            repositories.put(name, new RepositoryEntry(request.getContextPath(), name));
        }

        return repositories;
    }

    /**
     * Returns the list of workspaces available to this user within the named repository.
     * 
     * @param rawRepositoryName the name of the repository; may not be null
     * @param request the servlet request; may not be null
     * @return the list of workspaces available to this user within the named repository.
     * @throws IOException if the given repository name does not map to any repositories and there is an error writing the error
     *         code to the response.
     * @throws RepositoryException if there is any other error accessing the list of available workspaces for the repository
     */
    @GET
    @Path( "/{repositoryName}" )
    @Produces( "application/json" )
    public Map<String, WorkspaceEntry> getWorkspaces( @Context HttpServletRequest request,
                                                      @PathParam( "repositoryName" ) String rawRepositoryName )
        throws RepositoryException, IOException {
        
        assert request != null;
        assert rawRepositoryName != null;

        Map<String, WorkspaceEntry> workspaces = new HashMap<String, WorkspaceEntry>();

        Session session = getSession(request, rawRepositoryName, null);
        rawRepositoryName = URL_ENCODER.encode(rawRepositoryName);
        
        for (String name : session.getWorkspace().getAccessibleWorkspaceNames()) {
            if (name.trim().length() == 0) {
                name = EMPTY_WORKSPACE_NAME;
            }
            name = URL_ENCODER.encode(name);
            workspaces.put(name, new WorkspaceEntry(request.getContextPath(), rawRepositoryName, name));
        }

        return workspaces;
    }

    /**
     * Handles GET requests for an item in a workspace.
     * 
     * @param request the servlet request; may not be null or unauthenticated
     * @param rawRepositoryName the URL-encoded repository name
     * @param rawWorkspaceName the URL-encoded workspace name
     * @param path the path to the item
     * @param depth the depth of the node graph that should be returned if {@code path} refers to a node. @{code 0} means return
     *        the requested node only. A negative value indicates that the full subgraph under the node should be returned. This
     *        parameter defaults to {@code 0} and is ignored if {@code path} refers to a property.
     * @return the JSON-encoded version of the item (and, if the item is a node, its subgraph, depending on the value of {@code
     *         depth})
     * @throws NotFoundException if the named repository does not exists, the named workspace does not exist, or the user does not
     *         have access to the named workspace
     * @throws JSONException if there is an error encoding the node
     * @throws UnauthorizedException if the given login information is invalid
     * @throws RepositoryException if any other error occurs
     * @see #EMPTY_REPOSITORY_NAME
     * @see #EMPTY_WORKSPACE_NAME
     * @see Session#getItem(String)
     */
    @GET
    @Path( "/{repositoryName}/{workspaceName}/items{path:.*}" )
    @Produces( "application/json" )
    public String getItem( @Context HttpServletRequest request, 
                           @PathParam( "repositoryName" ) String rawRepositoryName,
                           @PathParam( "workspaceName" ) String rawWorkspaceName,
                           @PathParam( "path" ) String path,
                           @QueryParam( "dna:depth" ) @DefaultValue( "0" ) int depth )
        throws JSONException, UnauthorizedException, RepositoryException {
        assert path != null;
        assert rawRepositoryName != null;
        assert rawWorkspaceName != null;

        Session session = getSession(request, rawRepositoryName, rawWorkspaceName);
        Item item;

        if ("/".equals(path) || "".equals(path)) {
            item = session.getRootNode();
        } else {
            try {
                item = session.getItem(path);
            } catch (PathNotFoundException pnfe) {
                throw new NotFoundException(pnfe.getMessage(), pnfe);
            }
        }

        if (item instanceof Node) {
            return jsonFor((Node)item, depth).toString();
        }
        return jsonFor((Property)item);
    }

    /**
     * Returns the JSON-encoded version of the given property. If the property is single-valued, the returned string is {@code
     * property.getValue().getString()} encoded as a JSON string. If the property is multi-valued with {@code N} values, this
     * method returns a JSON array containing {@code property.getValues()[N].getString()} for all values of {@code N}.
     * 
     * @param property the property to be encoded
     * @return the JSON-encoded version of the property
     * @throws RepositoryException if an error occurs accessing the property, its values, or its definition.
     * @see Property#getDefinition()
     * @see PropertyDefinition#isMultiple()
     */
    private String jsonFor( Property property ) throws RepositoryException {
        if (property.getDefinition().isMultiple()) {
            Value[] values = property.getValues();
            List<String> list = new ArrayList<String>(values.length);
            for (int i = 0; i < values.length; i++) {
                list.add(values[i].getString());
            }
            return new JSONArray(list).toString();
        }
        return JSONObject.quote(property.getValue().getString());
    }

    /**
     * Recursively returns the JSON-encoding of a node and its children to depth {@code toDepth}.
     * 
     * @param node the node to be encoded
     * @param toDepth the depth to which the recursion should extend; {@code 0} means no further recursion should occur.
     * @return the JSON-encoding of a node and its children to depth {@code toDepth}.
     * @throws JSONException if there is an error encoding the node
     * @throws RepositoryException if any other error occurs
     */
    private JSONObject jsonFor( Node node,
                                int toDepth ) throws JSONException, RepositoryException {
        JSONObject jsonNode = new JSONObject();

        JSONObject properties = new JSONObject();

        for (PropertyIterator iter = node.getProperties(); iter.hasNext();) {
            Property prop = iter.nextProperty();
            String propName = prop.getName();

            if (prop.getDefinition().isMultiple()) {
                Value[] values = prop.getValues();
                JSONArray array = new JSONArray();
                for (int i = 0; i < values.length; i++) {
                    array.put(values[i].getString());
                }
                properties.put(propName, array);

            } else {
                properties.put(propName, prop.getValue().getString());
            }

        }
        if (properties.length() > 0) {
            jsonNode.put(PROPERTIES_HOLDER, properties);
        }

        if (toDepth == 0) {
            List<String> children = new ArrayList<String>();

            for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
                Node child = iter.nextNode();

                children.add(child.getName());
            }

            if (children.size() > 0) {
                jsonNode.put(CHILD_NODE_HOLDER, new JSONArray(children));
            }
        } else {
            JSONObject children = new JSONObject();

            for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
                Node child = iter.nextNode();

                children.put(child.getName(), jsonFor(child, toDepth - 1));
            }

            if (children.length() > 0) {
                jsonNode.put(CHILD_NODE_HOLDER, children);
            }
        }

        return jsonNode;
    }

    /**
     * Adds the content of the request as a node (or subtree of nodes) at the location specified by {@code path}.
     * <p>
     * The primary type and mixin type(s) may optionally be specified through the {@code jcr:primaryType} and {@code
     * jcr:mixinTypes} properties.
     * </p>
     * 
     * @param request the servlet request; may not be null or unauthenticated
     * @param rawRepositoryName the URL-encoded repository name
     * @param rawWorkspaceName the URL-encoded workspace name
     * @param path the path to the item
     * @param requestContent the JSON-encoded representation of the node or nodes to be added
     * @return the JSON-encoded representation of the node or nodes that were added. This will differ from {@code requestContent}
     *         in that auto-created and protected properties (e.g., jcr:uuid) will be populated.
     * @throws NotFoundException if the parent of the item to be added does not exist
     * @throws UnauthorizedException if the user does not have the access required to create the node at this path
     * @throws JSONException if there is an error encoding the node
     * @throws RepositoryException if any other error occurs
     */
    @POST
    @Path( "/{repositoryName}/{workspaceName}/items/{path:.*}" )
    @Consumes( "application/json" )
    public Response postItem( @Context HttpServletRequest request,
                              @PathParam( "repositoryName" ) String rawRepositoryName,
                              @PathParam( "workspaceName" ) String rawWorkspaceName,
                              @PathParam( "path" ) String path,
                              String requestContent )
        throws NotFoundException, UnauthorizedException, RepositoryException, JSONException {

        assert rawRepositoryName != null;
        assert rawWorkspaceName != null;
        assert path != null;
        JSONObject body = new JSONObject(requestContent);

        int lastSlashInd = path.lastIndexOf('/');
        String parentPath = lastSlashInd == -1 ? "/" : "/" + path.substring(0, lastSlashInd);
        String newNodeName = lastSlashInd == -1 ? path : path.substring(lastSlashInd + 1);

        Session session = getSession(request, rawRepositoryName, rawWorkspaceName);

        Node parentNode = (Node)session.getItem(parentPath);

        Node newNode = addNode(parentNode, newNodeName, body);

        session.save();

        String json = jsonFor(newNode, -1).toString();
        return Response.status(Status.CREATED).entity(json).build();
    }

    /**
     * Adds the node described by {@code jsonNode} with name {@code nodeName} to the existing node {@code parentNode}.
     * 
     * @param parentNode the parent of the node to be added
     * @param nodeName the name of the node to be added
     * @param jsonNode the JSON-encoded representation of the node or nodes to be added.
     * @return the JSON-encoded representation of the node or nodes that were added. This will differ from {@code requestContent}
     *         in that auto-created and protected properties (e.g., jcr:uuid) will be populated.
     * @throws JSONException if there is an error encoding the node
     * @throws RepositoryException if any other error occurs
     */
    private Node addNode( Node parentNode,
                          String nodeName,
                          JSONObject jsonNode ) throws RepositoryException, JSONException {
        Node newNode;

        JSONObject properties = jsonNode.has(PROPERTIES_HOLDER) ? jsonNode.getJSONObject(PROPERTIES_HOLDER) : new JSONObject();

        if (properties.has(PRIMARY_TYPE_PROPERTY)) {
            String primaryType = properties.getString(PRIMARY_TYPE_PROPERTY);
            newNode = parentNode.addNode(nodeName, primaryType);
        } else {
            newNode = parentNode.addNode(nodeName);
        }

        if (properties.has(MIXIN_TYPES_PROPERTY)) {
            Object rawMixinTypes = properties.get(MIXIN_TYPES_PROPERTY);

            if (rawMixinTypes instanceof JSONArray) {
                JSONArray mixinTypes = (JSONArray)rawMixinTypes;
                for (int i = 0; i < mixinTypes.length(); i++) {
                    newNode.addMixin(mixinTypes.getString(i));
                }

            } else {
                newNode.addMixin(rawMixinTypes.toString());

            }
        }

        for (Iterator<?> iter = properties.keys(); iter.hasNext();) {
            String key = (String)iter.next();

            if (PRIMARY_TYPE_PROPERTY.equals(key)) continue;
            if (MIXIN_TYPES_PROPERTY.equals(key)) continue;
            setPropertyOnNode(newNode, key, properties.get(key));
        }

        if (jsonNode.has(CHILD_NODE_HOLDER)) {
            JSONObject children = jsonNode.getJSONObject(CHILD_NODE_HOLDER);

            for (Iterator<?> iter = children.keys(); iter.hasNext();) {
                String childName = (String)iter.next();
                JSONObject child = children.getJSONObject(childName);

                addNode(newNode, childName, child);
            }
        }

        return newNode;
    }

    /**
     * Sets the named property on the given node. This method expects {@code value} to be either a JSON string or a JSON array of
     * JSON strings. If {@code value} is a JSON array, {@code Node#setProperty(String, String[]) the multi-valued property setter}
     * will be used.
     * 
     * @param node the node on which the property is to be set
     * @param propName the name of the property to set
     * @param value the JSON-encoded values to be set
     * @throws RepositoryException if there is an error setting the property
     * @throws JSONException if {@code value} cannot be decoded
     */
    private void setPropertyOnNode( Node node,
                                    String propName,
                                    Object value ) throws RepositoryException, JSONException {
        if (value instanceof JSONArray) {
            JSONArray jsonValues = (JSONArray)value;
            String[] values = new String[jsonValues.length()];

            for (int i = 0; i < values.length; i++) {
                values[i] = jsonValues.getString(i);
            }
            node.setProperty(propName, values);
        } else {
            node.setProperty(propName, (String)value);
        }

    }

    /**
     * Deletes the item at {@code path}.
     * 
     * @param request the servlet request; may not be null or unauthenticated
     * @param rawRepositoryName the URL-encoded repository name
     * @param rawWorkspaceName the URL-encoded workspace name
     * @param path the path to the item
     * @throws NotFoundException if no item exists at {@code path}
     * @throws UnauthorizedException if the user does not have the access required to delete the item at this path
     * @throws RepositoryException if any other error occurs
     */
    @DELETE
    @Path( "/{repositoryName}/{workspaceName}/items{path:.*}" )
    @Consumes( "application/json" )
    public void deleteItem( @Context HttpServletRequest request,
                            @PathParam( "repositoryName" ) String rawRepositoryName,
                            @PathParam( "workspaceName" ) String rawWorkspaceName,
                            @PathParam( "path" ) String path )
        throws NotFoundException, UnauthorizedException, RepositoryException {

        assert rawRepositoryName != null;
        assert rawWorkspaceName != null;
        assert path != null;

        Session session = getSession(request, rawRepositoryName, rawWorkspaceName);

        Item item;
        try {
            item = session.getItem(path);
        } catch (PathNotFoundException pnfe) {
            throw new NotFoundException(pnfe.getMessage(), pnfe);
        }
        item.remove();
        session.save();
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
     * @throws NotFoundException if the parent of the item to be added does not exist
     * @throws UnauthorizedException if the user does not have the access required to create the node at this path
     * @throws JSONException if there is an error encoding the node
     * @throws RepositoryException if any other error occurs
     */
    @PUT
    @Path( "/{repositoryName}/{workspaceName}/items{path:.*}" )
    @Consumes( "application/json" )
    public String putItem( @Context HttpServletRequest request,
                           @PathParam( "repositoryName" ) String rawRepositoryName,
                           @PathParam( "workspaceName" ) String rawWorkspaceName,
                           @PathParam( "path" ) String path,
                           String requestContent ) throws UnauthorizedException, JSONException, RepositoryException {

        assert path != null;
        assert rawRepositoryName != null;
        assert rawWorkspaceName != null;

        Session session = getSession(request, rawRepositoryName, rawWorkspaceName);
        Node node;
        Item item;
        if ("".equals(path) || "/".equals(path)) {
            item = session.getRootNode();
        } else {
            try {
                item = session.getItem(path);
            } catch (PathNotFoundException pnfe) {
                throw new NotFoundException(pnfe.getMessage(), pnfe);
            }
        }

        if (item instanceof Node) {
            JSONObject properties = new JSONObject(requestContent);
            node = (Node)item;

            for (Iterator<?> iter = properties.keys(); iter.hasNext();) {
                String key = (String)iter.next();

                setPropertyOnNode(node, key, properties.get(key));
            }

        } else {
            /*
             * The incoming content should be a JSON string or a JSON array. Wrap it into an object so it can be parsed more easily
             */

            JSONObject properties = new JSONObject("{ \"value\": " + requestContent + "}");
            Property property = (Property)item;
            node = property.getParent();

            setPropertyOnNode(node, property.getName(), properties.get("value"));
        }

        return jsonFor(node, 0).toString();
    }

    private String workspaceNameFor( String rawWorkspaceName ) {
        String workspaceName = URL_ENCODER.decode(rawWorkspaceName);

        if (EMPTY_WORKSPACE_NAME.equals(workspaceName)) {
            workspaceName = "";
        }

        return workspaceName;
    }

    private String repositoryNameFor( String rawRepositoryName ) {
        String repositoryName = URL_ENCODER.decode(rawRepositoryName);

        if (EMPTY_REPOSITORY_NAME.equals(repositoryName)) {
            repositoryName = "";
        }

        return repositoryName;
    }

    @Provider
    public static class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {

        public Response toResponse( NotFoundException exception ) {
            return Response.status(Status.NOT_FOUND).entity(exception.getMessage()).build();
        }

    }

    @Provider
    public static class JSONExceptionMapper implements ExceptionMapper<JSONException> {

        public Response toResponse( JSONException exception ) {
            return Response.status(Status.BAD_REQUEST).entity(exception.getMessage()).build();
        }

    }

    @Provider
    public static class RepositoryExceptionMapper implements ExceptionMapper<RepositoryException> {

        public Response toResponse( RepositoryException exception ) {
            /*
             * This error code is murky - the request must have been syntactically valid to get to
             * the JCR operations, but there isn't an HTTP status code for "semantically invalid." 
             */
            return Response.status(Status.BAD_REQUEST).entity(exception.getMessage()).build();
        }

    }

}
