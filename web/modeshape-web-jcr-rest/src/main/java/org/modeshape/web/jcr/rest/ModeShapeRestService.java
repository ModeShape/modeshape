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

import java.io.InputStream;
import javax.jcr.Binary;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.codehaus.jettison.json.JSONException;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.util.StringUtil;
import org.modeshape.web.jcr.rest.form.FileUploadForm;
import org.modeshape.web.jcr.rest.handler.RestBinaryHandler;
import org.modeshape.web.jcr.rest.handler.RestItemHandler;
import org.modeshape.web.jcr.rest.handler.RestNodeHandler;
import org.modeshape.web.jcr.rest.handler.RestNodeTypeHandler;
import org.modeshape.web.jcr.rest.handler.RestQueryHandler;
import org.modeshape.web.jcr.rest.handler.RestRepositoryHandler;
import org.modeshape.web.jcr.rest.handler.RestServerHandler;
import org.modeshape.web.jcr.rest.model.RestException;
import org.modeshape.web.jcr.rest.model.RestItem;
import org.modeshape.web.jcr.rest.model.RestNodeType;
import org.modeshape.web.jcr.rest.model.RestQueryResult;
import org.modeshape.web.jcr.rest.model.RestRepositories;
import org.modeshape.web.jcr.rest.model.RestWorkspaces;

/**
 * RESTEasy handler to provide the JCR resources at the URIs below. Please note that these URIs assume a context of
 * {@code /resources} for the web application.
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
 * <td>accesses/creates/updates/deletes the item (node or property) at the path. For POST and PUT, the body of the request is
 * expected to be valid JSON</td>
 * <td>GET, POST, PUT, DELETE</td>
 * </tr>
 * <tr>
 * <td>/resources/{repositoryName}/{workspaceName}/items</td>
 * <td>performs bulk create/update/delete of items. For POST, PUT and DELETE the body of the request is expected to be valid JSON</td>
 * <td>POST, PUT, DELETE</td>
 * </tr>
 * <tr>
 * <tr>
 * <td>/resources/{repositoryName}/{workspaceName}/nodes/{id}</td>
 * <td>accesses/updates/deletes the with the given identifier. For POST and PUT, the body of the request is expected to be valid
 * JSON</td>
 * <td>GET, PUT, DELETE</td>
 * </tr>
 * <td>/resources/{repositoryName}/{workspaceName}/binary/{path}</td>
 * <td>accesses/creates/updates a binary property at the path</td>
 * <td>GET, POST, PUT. The binary data is expected to be written to the body of the request.</td> </tr>
 * <tr>
 * <td>/resources/{repositoryName}/{workspaceName}/nodetypes/{name}</td>
 * <td>accesses a node type from the repository, at the given name</td>
 * <td>GET</td>
 * </tr>
 * <tr>
 * <td>/resources/{repositoryName}/{workspaceName}/nodetypes</td>
 * <td>imports a CND file into the repository. The binary content of the CND file is expected to be the body of the request.</td>
 * <td>POST</td>
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
 * When working with binary values, the <i>/resources/{repositoryName}/{workspaceName}/binary/{path}</i> method should be used.
 * When returning information involving binary values (either nodes with binary properties or binary properties directly), the
 * response will contain an URL which can be then called to retrieve the actual content of the binary value.
 * </p>
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
@Immutable
@Path( "/" )
public final class ModeShapeRestService {

    private RestServerHandler serverHandler = new RestServerHandler();
    private RestRepositoryHandler repositoryHandler = new RestRepositoryHandler();
    private RestItemHandler itemHandler = new RestItemHandler();
    private RestNodeHandler nodeHandler = new RestNodeHandler();
    private RestQueryHandler queryHandler = new RestQueryHandler();
    private RestBinaryHandler binaryHandler = new RestBinaryHandler();
    private RestNodeTypeHandler nodeTypeHandler = new RestNodeTypeHandler();

    /**
     * Returns the list of JCR repositories available on this server
     * 
     * @param request the servlet request; may not be null
     * @return the list of JCR repositories available on this server, as a {@link RestRepositories} instance.
     */
    @GET
    @Path( "/" )
    @Produces( {MediaType.TEXT_HTML, MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON} )
    public RestRepositories getRepositories( @Context HttpServletRequest request ) {
        return serverHandler.getRepositories(request);
    }

    /**
     * Returns the list of workspaces available to this user within the named repository.
     * 
     * @param rawRepositoryName the name of the repository; may not be null
     * @param request the servlet request; may not be null
     * @return the list of workspaces available to this user within the named repository, as a {@link RestWorkspaces} instance.
     * @throws RepositoryException if there is any other error accessing the list of available workspaces for the repository
     */
    @GET
    @Path( "{repositoryName}" )
    @Produces( {MediaType.TEXT_HTML, MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON} )
    public RestWorkspaces getWorkspaces( @Context HttpServletRequest request,
                                         @PathParam( "repositoryName" ) String rawRepositoryName ) throws RepositoryException {
        return repositoryHandler.getWorkspaces(request, rawRepositoryName);
    }

    /**
     * Retrieves the binary content of the binary property at the given path, allowing 2 extra (optional) parameters: the
     * mime-type and the content-disposition of the binary value.
     * 
     * @param request a non-null {@link HttpServletRequest} request
     * @param repositoryName a non-null {@link String} representing the name of a repository.
     * @param workspaceName a non-null {@link String} representing the name of a workspace.
     * @param path a non-null {@link String} representing the absolute path to a binary property.
     * @param mimeType an optional {@link String} representing the "already-known" mime-type of the binary. Can be {@code null}
     * @param contentDisposition an optional {@link String} representing the client-preferred content disposition of the respose.
     *        Can be {@code null}
     * @return the binary stream of the requested binary property or NOT_FOUND if either the property isn't found or it isn't a
     *         binary
     * @throws RepositoryException if any JCR related operation fails, including the case when the path to the property isn't
     *         valid.
     */
    @GET
    @Path( "{repositoryName}/{workspaceName}/" + RestHelper.BINARY_METHOD_NAME + "{path:.+}" )
    @Produces( {MediaType.TEXT_HTML, MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON} )
    public Response getBinary( @Context HttpServletRequest request,
                               @PathParam( "repositoryName" ) String repositoryName,
                               @PathParam( "workspaceName" ) String workspaceName,
                               @PathParam( "path" ) String path,
                               @QueryParam( "mimeType" ) String mimeType,
                               @QueryParam( "contentDisposition" ) String contentDisposition ) throws RepositoryException {
        Property binaryProperty = binaryHandler.getBinaryProperty(request, repositoryName, workspaceName, path);
        if (binaryProperty.getType() != PropertyType.BINARY) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity(new RestException("The property " + binaryProperty.getPath() + " is not a binary"))
                           .build();
        }
        Binary binary = binaryProperty.getBinary();
        if (StringUtil.isBlank(mimeType)) {
            mimeType = binaryHandler.getDefaultMimeType(binaryProperty);
        }
        if (StringUtil.isBlank(contentDisposition)) {
            contentDisposition = binaryHandler.getDefaultContentDisposition(binaryProperty);
        }
        /**
         * TODO author=Horia Chiorean date=8/15/12 description=There is nasty RestEASY bug:
         * https://issues.jboss.org/browse/RESTEASY-741 so we need to be aware that with the current version the stream won't be
         * closed.
         */
        Response.ResponseBuilder responseBuilder = Response.ok(binary.getStream(), mimeType);
        responseBuilder.header("Content-Disposition", contentDisposition);
        return responseBuilder.build();
    }

    /**
     * Retrieves the node type definition with the given name from the repository.
     * 
     * @param request a non-null {@link HttpServletRequest} request
     * @param repositoryName a non-null {@link String} representing the name of a repository.
     * @param workspaceName a non-null {@link String} representing the name of a workspace.
     * @param nodeTypeName a non-null {@link String} representing the name of a node type.
     * @return the node type information.
     * @throws RepositoryException if any JCR related operation fails.
     */
    @GET
    @Path( "{repositoryName}/{workspaceName}/" + RestHelper.NODE_TYPES_METHOD_NAME + "/{nodeTypeName:.+}" )
    @Produces( {MediaType.TEXT_HTML, MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON} )
    public RestNodeType getNodeType( @Context HttpServletRequest request,
                                     @PathParam( "repositoryName" ) String repositoryName,
                                     @PathParam( "workspaceName" ) String workspaceName,
                                     @PathParam( "nodeTypeName" ) String nodeTypeName ) throws RepositoryException {
        return nodeTypeHandler.getNodeType(request, repositoryName, workspaceName, nodeTypeName);
    }

    /**
     * Imports a single CND file into the repository. The CND file should be submitted as the body of the request.
     * 
     * @param request a non-null {@link HttpServletRequest} request
     * @param repositoryName a non-null {@link String} representing the name of a repository.
     * @param workspaceName a non-null {@link String} representing the name of a workspace.
     * @param allowUpdate an optional parameter which indicates whether existing node types should be updated (overridden) or not.
     * @param requestBodyInputStream a {@code non-null} {@link InputStream} instance, representing the body of the request.
     * @return a list with the registered node types if the operation was successful, or an appropriate error code otherwise.
     * @throws RepositoryException if any JCR related operation fails.
     */
    @POST
    @Path( "{repositoryName}/{workspaceName}/" + RestHelper.NODE_TYPES_METHOD_NAME )
    @Produces( {MediaType.APPLICATION_JSON, MediaType.TEXT_HTML, MediaType.TEXT_PLAIN} )
    public Response postCND( @Context HttpServletRequest request,
                             @PathParam( "repositoryName" ) String repositoryName,
                             @PathParam( "workspaceName" ) String workspaceName,
                             @QueryParam( "allowUpdate" ) @DefaultValue( "true" ) boolean allowUpdate,
                             InputStream requestBodyInputStream ) throws RepositoryException {
        return nodeTypeHandler.importCND(request, repositoryName, workspaceName, allowUpdate, requestBodyInputStream);
    }

    /**
     * Imports a single CND file into the repository, using a {@link MediaType#MULTIPART_FORM_DATA} request. The CND file is
     * expected to be submitted from an HTML element with the name <i>file</i>
     * 
     * @param request a non-null {@link HttpServletRequest} request
     * @param repositoryName a non-null {@link String} representing the name of a repository.
     * @param workspaceName a non-null {@link String} representing the name of a workspace.
     * @param allowUpdate an optional parameter which indicates whether existing node types should be updated (overridden) or not.
     * @param form a {@link FileUploadForm} instance representing the HTML form from which the cnd was submitted
     * @return a {@code non-null} {@link Response}
     * @throws RepositoryException if any JCR operations fail
     * @throws IllegalArgumentException if the submitted form does not contain an HTML element named "file".
     */
    @POST
    @Path( "{repositoryName}/{workspaceName}/" + RestHelper.NODE_TYPES_METHOD_NAME )
    @Produces( {MediaType.TEXT_HTML, MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN} )
    @Consumes( MediaType.MULTIPART_FORM_DATA )
    public Response postCNDViaForm( @Context HttpServletRequest request,
                                    @PathParam( "repositoryName" ) String repositoryName,
                                    @PathParam( "workspaceName" ) String workspaceName,
                                    @QueryParam( "allowUpdate" ) @DefaultValue( "true" ) boolean allowUpdate,
                                    @MultipartForm FileUploadForm form ) throws RepositoryException {
        form.validate();
        return nodeTypeHandler.importCND(request, repositoryName, workspaceName, allowUpdate, form.getFileData());
    }

    /**
     * Retrieves an item from a workspace
     * 
     * @param request the servlet request; may not be null or unauthenticated
     * @param rawRepositoryName the URL-encoded repository name
     * @param rawWorkspaceName the URL-encoded workspace name
     * @param path the path to the item
     * @param depth the depth of the node graph that should be returned if {@code path} refers to a node. @{code 0} means return
     *        the requested node only. A negative value indicates that the full subgraph under the node should be returned. This
     *        parameter defaults to {@code 0} and is ignored if {@code path} refers to a property.
     * @return a {@code non-null} {@link RestItem}
     * @throws RepositoryException if any JCR error occurs
     * @see javax.jcr.Session#getItem(String)
     */
    @GET
    @Path( "{repositoryName}/{workspaceName}/" + RestHelper.ITEMS_METHOD_NAME + "{path:.*}" )
    @Produces( {MediaType.TEXT_HTML, MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON} )
    public RestItem getItem( @Context HttpServletRequest request,
                             @PathParam( "repositoryName" ) String rawRepositoryName,
                             @PathParam( "workspaceName" ) String rawWorkspaceName,
                             @PathParam( "path" ) String path,
                             @QueryParam( "depth" ) @DefaultValue( "0" ) int depth ) throws RepositoryException {
        return itemHandler.item(request, rawRepositoryName, rawWorkspaceName, path, depth);
    }

    /**
     * Adds the content of the request as a node (or subtree of nodes) at the location specified by {@code path}.
     * <p>
     * The primary type and mixin type(s) may optionally be specified through the {@code jcr:primaryType} and
     * {@code jcr:mixinTypes} properties as request attributes.
     * </p>
     * 
     * @param request the servlet request; may not be null or unauthenticated
     * @param rawRepositoryName the URL-encoded repository name
     * @param rawWorkspaceName the URL-encoded workspace name
     * @param path the path to the item
     * @param requestContent the JSON-encoded representation of the node or nodes to be added
     * @return a {@code non-null} {@link Response} instance which either contains the node or an error code.
     * @throws JSONException if there is an error reading the request body as a valid JSON object.
     * @throws RepositoryException if any other error occurs
     */
    @POST
    @Consumes( MediaType.APPLICATION_JSON )
    @Path( "{repositoryName}/{workspaceName}/" + RestHelper.ITEMS_METHOD_NAME + "{path:.+}" )
    @Produces( {MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.TEXT_HTML} )
    public Response postItem( @Context HttpServletRequest request,
                              @PathParam( "repositoryName" ) String rawRepositoryName,
                              @PathParam( "workspaceName" ) String rawWorkspaceName,
                              @PathParam( "path" ) String path,
                              String requestContent ) throws RepositoryException, JSONException {
        return itemHandler.addItem(request, rawRepositoryName, rawWorkspaceName, path, requestContent);
    }

    /**
     * Performs a bulk creation of items via a single session, using the body of the request, which is expected to be a valid JSON
     * object. The format of the JSON request must be an object of the form:
     * <ul>
     * <li>{ "node1_path" : { node1_body }, "node2_path": { node2_body } ... }</li>
     * <li>{ "property1_path" : { property1_body }, "property2_path": { property2_body } ... }</li>
     * <li>{ "property1_path" : { property1_body }, "node1_path": { node1_body } ... }</li>
     * </ul>
     * where each body (either of a property or of a node) is expected to be a JSON object which has the same format as the one
     * used when creating a single item.
     * 
     * @param request the servlet request; may not be null or unauthenticated
     * @param rawRepositoryName the URL-encoded repository name
     * @param rawWorkspaceName the URL-encoded workspace name
     * @param requestContent the JSON-encoded representation of the node or nodes to be added
     * @return a {@code non-null} {@link Response} instance which either contains the item or an error code.
     * @throws JSONException if there is an error reading the request body as a valid JSON object.
     * @throws RepositoryException if any other error occurs
     * @see ModeShapeRestService#postItem(javax.servlet.http.HttpServletRequest, String, String, String, String)
     */
    @POST
    @Consumes( MediaType.APPLICATION_JSON )
    @Path( "{repositoryName}/{workspaceName}/" + RestHelper.ITEMS_METHOD_NAME )
    @Produces( {MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.TEXT_HTML} )
    public Response postItems( @Context HttpServletRequest request,
                               @PathParam( "repositoryName" ) String rawRepositoryName,
                               @PathParam( "workspaceName" ) String rawWorkspaceName,
                               String requestContent ) throws RepositoryException, JSONException {
        return itemHandler.addItems(request, rawRepositoryName, rawWorkspaceName, requestContent);
    }

    /**
     * Deletes the item at {@code path}.
     * 
     * @param request the servlet request; may not be null or unauthenticated
     * @param rawRepositoryName the URL-encoded repository name
     * @param rawWorkspaceName the URL-encoded workspace name
     * @param path the path to the item
     * @return a {@code non-null} {@link Response} instance.
     * @throws RepositoryException if any other error occurs
     */
    @SuppressWarnings( "deprecation" )
    @DELETE
    @Path( "{repositoryName}/{workspaceName}/" + RestHelper.ITEMS_METHOD_NAME + "{path:.+}" )
    public Response deleteItem( @Context HttpServletRequest request,
                                @PathParam( "repositoryName" ) String rawRepositoryName,
                                @PathParam( "workspaceName" ) String rawWorkspaceName,
                                @PathParam( "path" ) String path ) throws RepositoryException {
        itemHandler.deleteItem(request, rawRepositoryName, rawWorkspaceName, path);
        return Response.noContent().build();
    }

    /**
     * Performs a bulk deletion of nodes via a single session, using the body of the request, which is expected to be a valid JSON
     * array. The format of the JSON request must an array of the form:
     * <ul>
     * <li>["node1_path", "node2_path",...]</li>
     * <li>["property1_path", "property2_path",...]</li>
     * <li>["property1_path", "node1_path",...]</li>
     * </ul>
     * 
     * @param request the servlet request; may not be null or unauthenticated
     * @param rawRepositoryName the URL-encoded repository name
     * @param rawWorkspaceName the URL-encoded workspace name
     * @param requestContent the JSON-encoded representation of the node or nodes to be added
     * @return a {@code non-null} {@link Response} instance.
     * @throws JSONException if there is an error reading the request body as a valid JSON object.
     * @throws RepositoryException if any other error occurs
     * @see ModeShapeRestService#deleteItem(javax.servlet.http.HttpServletRequest, String, String, String)
     */
    @DELETE
    @Consumes( MediaType.APPLICATION_JSON )
    @Path( "{repositoryName}/{workspaceName}/" + RestHelper.ITEMS_METHOD_NAME )
    public Response deleteItems( @Context HttpServletRequest request,
                                 @PathParam( "repositoryName" ) String rawRepositoryName,
                                 @PathParam( "workspaceName" ) String rawWorkspaceName,
                                 String requestContent ) throws RepositoryException, JSONException {
        return itemHandler.deleteItems(request, rawRepositoryName, rawWorkspaceName, requestContent);
    }

    /**
     * Updates the node or property at the path.
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
     * @return a {@link RestItem} instance representing the modified item.
     * @throws JSONException if there is an error reading the request body as a valid JSON object.
     * @throws RepositoryException if any other error occurs
     */
    @PUT
    @Path( "{repositoryName}/{workspaceName}/" + RestHelper.ITEMS_METHOD_NAME + "{path:.+}" )
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( {MediaType.APPLICATION_JSON, MediaType.TEXT_HTML, MediaType.TEXT_PLAIN} )
    public RestItem putItem( @Context HttpServletRequest request,
                             @PathParam( "repositoryName" ) String rawRepositoryName,
                             @PathParam( "workspaceName" ) String rawWorkspaceName,
                             @PathParam( "path" ) String path,
                             String requestContent ) throws JSONException, RepositoryException {
        return itemHandler.updateItem(request, rawRepositoryName, rawWorkspaceName, path, requestContent);
    }

    /**
     * Performs a bulk update of items via a single session, using the body of the request, which is expected to be a valid JSON
     * object. The format of the JSON request must be an object of the form:
     * <ul>
     * <li>{ "node1_path" : { node1_body }, "node2_path": { node2_body } ... }</li>
     * <li>{ "property1_path" : { property1_body }, "property2_path": { property2_body } ... }</li>
     * <li>{ "property1_path" : { property1_body }, "node1_path": { node1_body } ... }</li>
     * </ul>
     * where each body (either of a property or of a node) is expected to be a JSON object which has the same format as the one
     * used when updating a single item.
     * 
     * @param request the servlet request; may not be null or unauthenticated
     * @param rawRepositoryName the URL-encoded repository name
     * @param rawWorkspaceName the URL-encoded workspace name
     * @param requestContent the JSON-encoded representation of the values and, possibly, properties to be set
     * @return a {@code non-null} {@link Response}
     * @throws JSONException if there is an error reading the request body as a valid JSON object.
     * @throws RepositoryException if any other error occurs
     * @see ModeShapeRestService#putItem(javax.servlet.http.HttpServletRequest, String, String, String, String)
     */
    @PUT
    @Path( "{repositoryName}/{workspaceName}/" + RestHelper.ITEMS_METHOD_NAME )
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( {MediaType.APPLICATION_JSON, MediaType.TEXT_HTML, MediaType.TEXT_PLAIN} )
    public Response putItems( @Context HttpServletRequest request,
                              @PathParam( "repositoryName" ) String rawRepositoryName,
                              @PathParam( "workspaceName" ) String rawWorkspaceName,
                              String requestContent ) throws JSONException, RepositoryException {
        return itemHandler.updateItems(request, rawRepositoryName, rawWorkspaceName, requestContent);
    }

    /**
     * Creates or updates a binary property in the repository, at the given path. The binary content is expected to be written
     * directly to the request body.
     * 
     * @param request a non-null {@link HttpServletRequest} request
     * @param repositoryName a non-null {@link String} representing the name of a repository.
     * @param workspaceName a non-null {@link String} representing the name of a workspace.
     * @param path a non-null {@link String} representing the absolute path to a binary property.
     * @param requestBodyInputStream a non-null {@link InputStream} stream which represents the body of the request, where the
     *        binary content is expected.
     * @return a representation of the binary property that was created/updated.
     * @throws RepositoryException if any JCR related operation fails.
     */
    @POST
    @Path( "{repositoryName}/{workspaceName}/" + RestHelper.BINARY_METHOD_NAME + "{path:.+}" )
    @Produces( {MediaType.APPLICATION_JSON, MediaType.TEXT_HTML, MediaType.TEXT_PLAIN} )
    public Response postBinary( @Context HttpServletRequest request,
                                @PathParam( "repositoryName" ) String repositoryName,
                                @PathParam( "workspaceName" ) String workspaceName,
                                @PathParam( "path" ) String path,
                                InputStream requestBodyInputStream ) throws RepositoryException {
        return binaryHandler.updateBinary(request, repositoryName, workspaceName, path, requestBodyInputStream, true);
    }

    /**
     * Updates a binary property in the repository, at the given path. If the binary property does not exist, the NOT_FOUND http
     * response code is returned. The binary content is expected to be written directly to the request body.
     * 
     * @param request a non-null {@link HttpServletRequest} request
     * @param repositoryName a non-null {@link String} representing the name of a repository.
     * @param workspaceName a non-null {@link String} representing the name of a workspace.
     * @param path a non-null {@link String} representing the absolute path to an existing binary property.
     * @param requestBodyInputStream a non-null {@link InputStream} stream which represents the body of the request, where the
     *        binary content is expected.
     * @return a representation of the binary property that was updated.
     * @throws RepositoryException if any JCR related operation fails.
     */
    @PUT
    @Path( "{repositoryName}/{workspaceName}/" + RestHelper.BINARY_METHOD_NAME + "{path:.+}" )
    @Produces( {MediaType.APPLICATION_JSON, MediaType.TEXT_HTML, MediaType.TEXT_PLAIN} )
    public Response putBinary( @Context HttpServletRequest request,
                               @PathParam( "repositoryName" ) String repositoryName,
                               @PathParam( "workspaceName" ) String workspaceName,
                               @PathParam( "path" ) String path,
                               InputStream requestBodyInputStream ) throws RepositoryException {
        return binaryHandler.updateBinary(request, repositoryName, workspaceName, path, requestBodyInputStream, false);
    }

    /**
     * Creates/updates a binary file into the repository, using a {@link MediaType#MULTIPART_FORM_DATA} request, at {@code path}.
     * The binary file is expected to be submitted from an HTML element with the name <i>file</i>
     * 
     * @param request a non-null {@link HttpServletRequest} request
     * @param repositoryName a non-null {@link String} representing the name of a repository.
     * @param workspaceName a non-null {@link String} representing the name of a workspace.
     * @param path the path to the binary property
     * @param form a {@link FileUploadForm} instance representing the HTML form from which the binary was submitted
     * @return a {@code non-null} {@link Response}
     * @throws RepositoryException if any JCR related operation fails.
     * @see ModeShapeRestService#postBinary(javax.servlet.http.HttpServletRequest, String, String, String, java.io.InputStream)
     */
    @POST
    @Path( "{repositoryName}/{workspaceName}/" + RestHelper.BINARY_METHOD_NAME + "{path:.+}" )
    @Produces( {MediaType.TEXT_HTML, MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN} )
    @Consumes( MediaType.MULTIPART_FORM_DATA )
    public Response postBinaryViaForm( @Context HttpServletRequest request,
                                       @PathParam( "repositoryName" ) String repositoryName,
                                       @PathParam( "workspaceName" ) String workspaceName,
                                       @PathParam( "path" ) String path,
                                       @MultipartForm FileUploadForm form ) throws RepositoryException {
        form.validate();
        return binaryHandler.updateBinary(request, repositoryName, workspaceName, path, form.getFileData(), true);
    }

    /**
     * Executes the XPath query contained in the body of the request against the give repository and workspace.
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
     * @return a {@code non-null} {@link RestQueryResult} instance.
     * @throws RepositoryException if any JCR error occurs
     */
    @SuppressWarnings( "deprecation" )
    @POST
    @Path( "{repositoryName}/{workspaceName}/query" )
    @Consumes( "application/jcr+xpath" )
    @Produces( {MediaType.APPLICATION_JSON, MediaType.TEXT_HTML, MediaType.TEXT_PLAIN} )
    public RestQueryResult postXPathQuery( @Context HttpServletRequest request,
                                           @PathParam( "repositoryName" ) String rawRepositoryName,
                                           @PathParam( "workspaceName" ) String rawWorkspaceName,
                                           @QueryParam( "offset" ) @DefaultValue( "-1" ) long offset,
                                           @QueryParam( "limit" ) @DefaultValue( "-1" ) long limit,
                                           @Context UriInfo uriInfo,
                                           String requestContent ) throws RepositoryException {
        return queryHandler.executeQuery(request,
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
     * @return a {@code non-null} {@link RestQueryResult} instance.
     * @throws RepositoryException if any JCR error occurs
     */
    @SuppressWarnings( "deprecation" )
    @POST
    @Path( "{repositoryName}/{workspaceName}/query" )
    @Consumes( "application/jcr+sql" )
    @Produces( {MediaType.APPLICATION_JSON, MediaType.TEXT_HTML, MediaType.TEXT_PLAIN} )
    public RestQueryResult postJcrSqlQuery( @Context HttpServletRequest request,
                                            @PathParam( "repositoryName" ) String rawRepositoryName,
                                            @PathParam( "workspaceName" ) String rawWorkspaceName,
                                            @QueryParam( "offset" ) @DefaultValue( "-1" ) long offset,
                                            @QueryParam( "limit" ) @DefaultValue( "-1" ) long limit,
                                            @Context UriInfo uriInfo,
                                            String requestContent ) throws RepositoryException {
        return queryHandler.executeQuery(request,
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
     * @return a {@code non-null} {@link RestQueryResult} instance.
     * @throws RepositoryException if any JCR error occurs
     */
    @POST
    @Path( "{repositoryName}/{workspaceName}/query" )
    @Consumes( "application/jcr+sql2" )
    @Produces( {MediaType.APPLICATION_JSON, MediaType.TEXT_HTML, MediaType.TEXT_PLAIN} )
    public RestQueryResult postJcrSql2Query( @Context HttpServletRequest request,
                                             @PathParam( "repositoryName" ) String rawRepositoryName,
                                             @PathParam( "workspaceName" ) String rawWorkspaceName,
                                             @QueryParam( "offset" ) @DefaultValue( "-1" ) long offset,
                                             @QueryParam( "limit" ) @DefaultValue( "-1" ) long limit,
                                             @Context UriInfo uriInfo,
                                             String requestContent ) throws RepositoryException {
        return queryHandler.executeQuery(request,
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
     * @return a {@code non-null} {@link RestQueryResult} instance.
     * @throws RepositoryException if any JCR error occurs
     */
    @POST
    @Path( "{repositoryName}/{workspaceName}/query" )
    @Consumes( "application/jcr+search" )
    @Produces( {MediaType.APPLICATION_JSON, MediaType.TEXT_HTML, MediaType.TEXT_PLAIN} )
    public RestQueryResult postJcrSearchQuery( @Context HttpServletRequest request,
                                               @PathParam( "repositoryName" ) String rawRepositoryName,
                                               @PathParam( "workspaceName" ) String rawWorkspaceName,
                                               @QueryParam( "offset" ) @DefaultValue( "-1" ) long offset,
                                               @QueryParam( "limit" ) @DefaultValue( "-1" ) long limit,
                                               @Context UriInfo uriInfo,
                                               String requestContent ) throws RepositoryException {
        return queryHandler.executeQuery(request,
                                         rawRepositoryName,
                                         rawWorkspaceName,
                                         org.modeshape.jcr.api.query.Query.FULL_TEXT_SEARCH,
                                         requestContent,
                                         offset,
                                         limit,
                                         uriInfo);
    }

    /**
     * Retrieves from a workspace the node with the specified identifier.
     * 
     * @param request the servlet request; may not be null or unauthenticated
     * @param rawRepositoryName the URL-encoded repository name
     * @param rawWorkspaceName the URL-encoded workspace name
     * @param id the node identifier of the existing item
     * @param depth the depth of the node graph that should be returned. @{code 0} means return the requested node only. A
     *        negative value indicates that the full subgraph under the node should be returned. This parameter defaults to
     *        {@code 0}.
     * @return a {@code non-null} {@link RestItem}
     * @throws RepositoryException if any JCR error occurs
     * @see javax.jcr.Session#getNodeByIdentifier(String)
     */
    @GET
    @Path( "{repositoryName}/{workspaceName}/" + RestHelper.NODES_METHOD_NAME + "/{id:.*}" )
    @Produces( {MediaType.TEXT_HTML, MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON} )
    public RestItem getNodeWithId( @Context HttpServletRequest request,
                                   @PathParam( "repositoryName" ) String rawRepositoryName,
                                   @PathParam( "workspaceName" ) String rawWorkspaceName,
                                   @PathParam( "id" ) String id,
                                   @QueryParam( "depth" ) @DefaultValue( "0" ) int depth ) throws RepositoryException {
        return nodeHandler.nodeWithId(request, rawRepositoryName, rawWorkspaceName, id, depth);
    }

    /**
     * Updates the node with the given identifier
     * <p>
     * This method expects the request content to be a JSON object. The keys of the objects correspond to property names that will
     * be set and the values for the keys correspond to the values that will be set on the properties.
     * </p>
     * 
     * @param request the servlet request; may not be null or unauthenticated
     * @param rawRepositoryName the URL-encoded repository name
     * @param rawWorkspaceName the URL-encoded workspace name
     * @param id the node identifier of the existing item
     * @param requestContent the JSON-encoded representation of the values and, possibly, properties to be set
     * @return a {@link RestItem} instance representing the modified item.
     * @throws JSONException if there is an error reading the request body as a valid JSON object.
     * @throws RepositoryException if any other error occurs
     */
    @PUT
    @Path( "{repositoryName}/{workspaceName}/" + RestHelper.NODES_METHOD_NAME + "/{id:.+}" )
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( {MediaType.APPLICATION_JSON, MediaType.TEXT_HTML, MediaType.TEXT_PLAIN} )
    public RestItem putNodeWithId( @Context HttpServletRequest request,
                                   @PathParam( "repositoryName" ) String rawRepositoryName,
                                   @PathParam( "workspaceName" ) String rawWorkspaceName,
                                   @PathParam( "id" ) String id,
                                   String requestContent ) throws JSONException, RepositoryException {
        return nodeHandler.updateNodeWithId(request, rawRepositoryName, rawWorkspaceName, id, requestContent);
    }

    /**
     * Deletes the subgraph at the node with the given identifier.
     * 
     * @param request the servlet request; may not be null or unauthenticated
     * @param rawRepositoryName the URL-encoded repository name
     * @param rawWorkspaceName the URL-encoded workspace name
     * @param id the node identifier of the existing item
     * @return a {@code non-null} {@link Response} instance.
     * @throws RepositoryException if any other error occurs
     */
    @DELETE
    @Path( "{repositoryName}/{workspaceName}/" + RestHelper.NODES_METHOD_NAME + "/{id:.+}" )
    public Response deleteNodeWithId( @Context HttpServletRequest request,
                                      @PathParam( "repositoryName" ) String rawRepositoryName,
                                      @PathParam( "workspaceName" ) String rawWorkspaceName,
                                      @PathParam( "id" ) String id ) throws RepositoryException {
        nodeHandler.deleteNodeWithId(request, rawRepositoryName, rawWorkspaceName, id);
        return Response.noContent().build();
    }
}
