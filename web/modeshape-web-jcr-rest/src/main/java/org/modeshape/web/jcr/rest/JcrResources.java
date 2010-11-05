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
package org.modeshape.web.jcr.rest;

import java.io.IOException;
import java.util.Map;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
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
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import net.jcip.annotations.Immutable;
import org.codehaus.jettison.json.JSONException;
import org.jboss.resteasy.spi.NotFoundException;
import org.jboss.resteasy.spi.UnauthorizedException;
import org.modeshape.common.util.Base64;
import org.modeshape.web.jcr.rest.model.RepositoryEntry;
import org.modeshape.web.jcr.rest.model.WorkspaceEntry;
import org.modeshape.web.jcr.spi.NoSuchRepositoryException;

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
 * <td>/resources/{repositoryName}/{workspaceName}/items/{path}</td>
 * <td>accesses the item (node or property) at the path</td>
 * <td>GET, POST, PUT, DELETE</td>
 * </tr>
 * <tr>
 * <td>/resources/{repositoryName}/{workspaceName}/query</td>
 * <td>executes the query in the body of the request with a language specified by the content type (application/jcr+xpath,
 * application/jcr+sql, application/jcr+sql2, or application/search)</td>
 * <td>POST</td>
 * </tr>
 * </table>
 * <h3>Binary data</h3>
 * <p>
 * There are several ways to transfer binary property values, but all involve encoding the binary value into ASCII characters
 * using a {@link Base64} notation and denoting this by adding annotating the property name with a suffix defining the type of
 * encoding. Currently, only "base64" encoding is supported.
 * </p>
 * <p>
 * For example, if the "jcr:data" property contains a single binary value of "propertyValue", then the JSON object representing
 * that property will be:
 * 
 * <pre>
 *   &quot;jcr:data/base64/&quot; : &quot;cHJvcGVydHlWYWx1ZQ==&quot;
 * </pre>
 * 
 * Likewise, if the "jcr:data" property contains two binary values each being "propertyValue", then the JSON object representing
 * that property will be:
 * 
 * <pre>
 *   &quot;jcr:data/base64/&quot; : [ &quot;cHJvcGVydHlWYWx1ZQ==&quot;, &quot;cHJvcGVydHlWYWx1ZQ==&quot; ]
 * </pre>
 * 
 * Note that JCR 1.0.1 does not allow property names to and with a '/' character (among others), while JCR 2.0 does not allow
 * property names to contain an unescaped or unencoded '/' character. Therefore, the "/{encoding}/" suffix can never appear in a
 * valid JCR property name, and will always identify an encoded property.
 * </p>
 * <p>
 * Here are the details:
 * <ul>
 * <li>Getting a node with <code>GET /resources/{repositoryName}/item/{pathToNode}</code> obtains the JSON object representing the
 * node, and each property is represented as a nested JSON object where the name is the property name and the value(s) are
 * represented as either a single string value or an array of string values. If the property has a binary value, then the property
 * name is appended with "/base64/" and the string representation of each value is encoded in Base64.</li>
 * <li>Getting a property with <code>GET /resources/{repositoryName}/item/{pathToProperty}</code> allows only the value(s) for the
 * one property to be included in the response. If any of the values is a binary value, then <i>all</i> of the values will be
 * encoded in Base64.</li>
 * <li>Setting a property with <code>PUT /resources/{repositoryName}/item/{pathToProperty}</code> allows setting the property to a
 * single value, and only that value needs to be included in the body of the request. If the value is binary, the value
 * <i>must</i> be {@link Base64 encoded} by the client and the "Content-Transfer-Encoding" header must be set to "base64" (case
 * does not matter). When the request is received, the value is decoded before the property value is updated on the node.</li>
 * <li>Creating a node with <code>POST /resources/{repositoryName}/item/{pathToNode}</code> requires a request that is structured
 * in the same way as the response from getting a node: the resulting JSON object represents the node, with nested JSON objects
 * for the properties and children. If any property of the new node has a binary value, then the name of the property <i>must</i>
 * be appended with "/base64/" and the string representation of each value are to be encoded in Base64.</li>
 * <li>Updating a node with <code>PUT /resources/{repositoryName}/item/{pathToNode}</code> requires a request that is structured
 * in the same way as the response from getting or posting a node: the resulting JSON object represents the node, with nested JSON
 * objects for the properties and children. If any property of the new node has a binary value, then the name of the property
 * <i>must</i> be appended with "/base64/" and the string representation of each value are to be encoded in Base64.</li>
 * </ul>
 * </p>
 */
@Immutable
@Path( "/" )
public class JcrResources extends AbstractHandler {

    /**
     * This is a duplicate of the FullTextSearchParser.LANGUAGE field, but it is split out here to avoid adding a dependency on
     * the modeshape-jcr package.
     */
    private final static String SEARCH_LANGUAGE = "Search";

    private ServerHandler serverHandler = new ServerHandler();
    private RepositoryHandler repositoryHandler = new RepositoryHandler();
    private ItemHandler itemHandler = new ItemHandler();
    private QueryHandler queryHandler = new QueryHandler();

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
        return serverHandler.getRepositories(request);
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
        return repositoryHandler.getWorkspaces(request, rawRepositoryName);
    }

    /**
     * Handles GET requests for an item in a workspace.
     * 
     * @param request the servlet request; may not be null or unauthenticated
     * @param rawRepositoryName the URL-encoded repository name
     * @param rawWorkspaceName the URL-encoded workspace name
     * @param path the path to the item
     * @param deprecatedDepth the old depth parameter ("mode:depth"). This version is deprecated and should use the "depth" query
     *        parameter instead.
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
                           @QueryParam( "mode:depth" ) @DefaultValue( "0" ) int deprecatedDepth,
                           @QueryParam( "depth" ) @DefaultValue( "0" ) int depth )
        throws JSONException, UnauthorizedException, RepositoryException {
        if (depth == 0 && deprecatedDepth != 0) depth = deprecatedDepth;
        return itemHandler.getItem(request, rawRepositoryName, rawWorkspaceName, path, depth);
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
     * @param fullNodeInResponse if {@code fullNodeInResponse == null || Boolean.valueOf(fullNodeInResponse)}, indicates that a
     *        representation of the created node (including all properties and children) should be returned; otherwise, only the
     *        path to the new node will be returned
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
                              @QueryParam( "mode:includeNode" ) String fullNodeInResponse,
                              String requestContent )
        throws NotFoundException, UnauthorizedException, RepositoryException, JSONException {
        return itemHandler.postItem(request,
                                    rawRepositoryName,
                                    rawWorkspaceName,
                                    path,
                                    fullNodeInResponse == null || Boolean.valueOf(fullNodeInResponse),
                                    requestContent);
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
        itemHandler.deleteItem(request, rawRepositoryName, rawWorkspaceName, path);
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
     * @throws IOException if there is a problem reading the value
     */
    @PUT
    @Path( "/{repositoryName}/{workspaceName}/items{path:.*}" )
    @Consumes( "application/json" )
    public String putItem( @Context HttpServletRequest request,
                           @PathParam( "repositoryName" ) String rawRepositoryName,
                           @PathParam( "workspaceName" ) String rawWorkspaceName,
                           @PathParam( "path" ) String path,
                           String requestContent ) throws UnauthorizedException, JSONException, RepositoryException, IOException {
        return itemHandler.putItem(request, rawRepositoryName, rawWorkspaceName, path, requestContent);
    }

    /**
     * Executes the XPath query contained in the body of the request against the give repository and workspace.
     * <p>
     * The query results will be JSON-encoded in the response body.
     * </p>
     * 
     * @param request the servlet request; may not be null or unauthenticated
     * @param rawRepositoryName the URL-encoded repository name
     * @param rawWorkspaceName the URL-encoded workspace name
     * @param offset the offset to the first row to be returned. If this value is greater than the size of the result set, no
     *        records will be returned. If this value is less than 0, results will be returned starting from the first record in
     *        the result set.
     * @param limit the maximum number of rows to be returned. If this value is greater than the size of the result set, the
     *        entire result set will be returned. If this value is less than zero, the entire result set will be returned. The
     *        results are counted from the record specified in the offset parameter.
     * @param uriInfo the information about the URI (from which the other query parameters will be obtained)
     * @param requestContent the query expression
     * @return the JSON-encoded representation of the query results.
     * @throws JSONException if there is an error encoding the node
     * @throws InvalidQueryException if the query contained an error, was invalid, or could not be executed
     * @throws RepositoryException if any other error occurs
     */
    @SuppressWarnings( "deprecation" )
    @POST
    @Path( "/{repositoryName}/{workspaceName}/query" )
    @Consumes( "application/jcr+xpath" )
    public String postXPathQuery( @Context HttpServletRequest request,
                                  @PathParam( "repositoryName" ) String rawRepositoryName,
                                  @PathParam( "workspaceName" ) String rawWorkspaceName,
                                  @QueryParam( "offset" ) @DefaultValue( "-1" ) long offset,
                                  @QueryParam( "limit" ) @DefaultValue( "-1" ) long limit,
                                  @Context UriInfo uriInfo,
                                  String requestContent ) throws InvalidQueryException, RepositoryException, JSONException {
        return queryHandler.postItem(request,
                                     rawRepositoryName,
                                     rawWorkspaceName,
                                     Query.XPATH,
                                     requestContent,
                                     offset,
                                     limit,
                                     uriInfo);
    }

    /**
     * Executes the JCR-SQL query contained in the body of the request against the give repository and workspace.
     * <p>
     * The query results will be JSON-encoded in the response body.
     * </p>
     * 
     * @param request the servlet request; may not be null or unauthenticated
     * @param rawRepositoryName the URL-encoded repository name
     * @param rawWorkspaceName the URL-encoded workspace name
     * @param offset the offset to the first row to be returned. If this value is greater than the size of the result set, no
     *        records will be returned. If this value is less than 0, results will be returned starting from the first record in
     *        the result set.
     * @param limit the maximum number of rows to be returned. If this value is greater than the size of the result set, the
     *        entire result set will be returned. If this value is less than zero, the entire result set will be returned. The
     *        results are counted from the record specified in the offset parameter.
     * @param uriInfo the information about the URI (from which the other query parameters will be obtained)
     * @param requestContent the query expression
     * @return the JSON-encoded representation of the query results.
     * @throws JSONException if there is an error encoding the node
     * @throws InvalidQueryException if the query contained an error, was invalid, or could not be executed
     * @throws RepositoryException if any other error occurs
     */
    @SuppressWarnings( "deprecation" )
    @POST
    @Path( "/{repositoryName}/{workspaceName}/query" )
    @Consumes( "application/jcr+sql" )
    public String postJcrSqlQuery( @Context HttpServletRequest request,
                                   @PathParam( "repositoryName" ) String rawRepositoryName,
                                   @PathParam( "workspaceName" ) String rawWorkspaceName,
                                   @QueryParam( "offset" ) @DefaultValue( "-1" ) long offset,
                                   @QueryParam( "limit" ) @DefaultValue( "-1" ) long limit,
                                   @Context UriInfo uriInfo,
                                   String requestContent ) throws InvalidQueryException, RepositoryException, JSONException {
        return queryHandler.postItem(request,
                                     rawRepositoryName,
                                     rawWorkspaceName,
                                     Query.SQL,
                                     requestContent,
                                     offset,
                                     limit,
                                     uriInfo);
    }

    /**
     * Executes the JCR-SQL2 query contained in the body of the request against the give repository and workspace.
     * <p>
     * The query results will be JSON-encoded in the response body.
     * </p>
     * 
     * @param request the servlet request; may not be null or unauthenticated
     * @param rawRepositoryName the URL-encoded repository name
     * @param rawWorkspaceName the URL-encoded workspace name
     * @param offset the offset to the first row to be returned. If this value is greater than the size of the result set, no
     *        records will be returned. If this value is less than 0, results will be returned starting from the first record in
     *        the result set.
     * @param limit the maximum number of rows to be returned. If this value is greater than the size of the result set, the
     *        entire result set will be returned. If this value is less than zero, the entire result set will be returned. The
     *        results are counted from the record specified in the offset parameter.
     * @param uriInfo the information about the URI (from which the other query parameters will be obtained)
     * @param requestContent the query expression
     * @return the JSON-encoded representation of the query results.
     * @throws JSONException if there is an error encoding the node
     * @throws InvalidQueryException if the query contained an error, was invalid, or could not be executed
     * @throws RepositoryException if any other error occurs
     */
    @POST
    @Path( "/{repositoryName}/{workspaceName}/query" )
    @Consumes( "application/jcr+sql2" )
    public String postJcrSql2Query( @Context HttpServletRequest request,
                                    @PathParam( "repositoryName" ) String rawRepositoryName,
                                    @PathParam( "workspaceName" ) String rawWorkspaceName,
                                    @QueryParam( "offset" ) @DefaultValue( "-1" ) long offset,
                                    @QueryParam( "limit" ) @DefaultValue( "-1" ) long limit,
                                    @Context UriInfo uriInfo,
                                    String requestContent ) throws InvalidQueryException, RepositoryException, JSONException {
        return queryHandler.postItem(request,
                                     rawRepositoryName,
                                     rawWorkspaceName,
                                     Query.JCR_SQL2,
                                     requestContent,
                                     offset,
                                     limit,
                                     uriInfo);
    }

    /**
     * Executes the JCR-SQL query contained in the body of the request against the give repository and workspace.
     * <p>
     * The query results will be JSON-encoded in the response body.
     * </p>
     * 
     * @param request the servlet request; may not be null or unauthenticated
     * @param rawRepositoryName the URL-encoded repository name
     * @param rawWorkspaceName the URL-encoded workspace name
     * @param offset the offset to the first row to be returned. If this value is greater than the size of the result set, no
     *        records will be returned. If this value is less than 0, results will be returned starting from the first record in
     *        the result set.
     * @param limit the maximum number of rows to be returned. If this value is greater than the size of the result set, the
     *        entire result set will be returned. If this value is less than zero, the entire result set will be returned. The
     *        results are counted from the record specified in the offset parameter.
     * @param uriInfo the information about the URI (from which the other query parameters will be obtained)
     * @param requestContent the query expression
     * @return the JSON-encoded representation of the query results.
     * @throws JSONException if there is an error encoding the node
     * @throws InvalidQueryException if the query contained an error, was invalid, or could not be executed
     * @throws RepositoryException if any other error occurs
     */
    @POST
    @Path( "/{repositoryName}/{workspaceName}/query" )
    @Consumes( "application/jcr+search" )
    public String postJcrSearchQuery( @Context HttpServletRequest request,
                                      @PathParam( "repositoryName" ) String rawRepositoryName,
                                      @PathParam( "workspaceName" ) String rawWorkspaceName,
                                      @QueryParam( "offset" ) @DefaultValue( "-1" ) long offset,
                                      @QueryParam( "limit" ) @DefaultValue( "-1" ) long limit,
                                      @Context UriInfo uriInfo,
                                      String requestContent ) throws RepositoryException, JSONException {
        return queryHandler.postItem(request,
                                     rawRepositoryName,
                                     rawWorkspaceName,
                                     SEARCH_LANGUAGE,
                                     requestContent,
                                     offset,
                                     limit,
                                     uriInfo);
    }

    @Provider
    public static class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {

        public Response toResponse( NotFoundException exception ) {
            return Response.status(Status.NOT_FOUND).entity(exception.getMessage()).build();
        }

    }

    @Provider
    public static class NoSuchRepositoryExceptionMapper implements ExceptionMapper<NoSuchRepositoryException> {

        public Response toResponse( NoSuchRepositoryException exception ) {
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
    public static class InvalidQueryExceptionMapper implements ExceptionMapper<InvalidQueryException> {

        public Response toResponse( InvalidQueryException exception ) {
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
