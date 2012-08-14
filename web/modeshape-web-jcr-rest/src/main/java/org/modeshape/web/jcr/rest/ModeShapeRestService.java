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

import javax.jcr.Binary;
import javax.jcr.Property;
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
import org.modeshape.web.jcr.rest.handler.RestNodeTypeHandler;
import org.modeshape.web.jcr.rest.handler.RestQueryHandler;
import org.modeshape.web.jcr.rest.handler.RestRepositoryHandler;
import org.modeshape.web.jcr.rest.handler.RestServerHandler;
import org.modeshape.web.jcr.rest.model.RestItem;
import org.modeshape.web.jcr.rest.model.RestNodeType;
import org.modeshape.web.jcr.rest.model.RestQueryResult;
import org.modeshape.web.jcr.rest.model.RestRepositories;
import org.modeshape.web.jcr.rest.model.RestWorkspaces;
import java.io.IOException;
import java.io.InputStream;

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
 * <td>/resources/{repositoryName}/{workspaceName}/binary/{path}</td>
 * <td>accesses/creates/updates a binary property at the path</td>
 * <td>GET, POST, PUT. The binary data is expected to be written to the body of the request.</td>
 * </tr>
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
 * When returning information involving binary values (either nodes with binary properties or binary properties directly),
 * the response will contain an URL which can be then called to retrieve the actual content of the binary value.
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
    private RestQueryHandler queryHandler = new RestQueryHandler();
    private RestBinaryHandler binaryHandler = new RestBinaryHandler();
    private RestNodeTypeHandler nodeTypeHandler = new RestNodeTypeHandler();

    /**
     * @see JcrResources#getRepositories(javax.servlet.http.HttpServletRequest)
     */
    @GET
    @Path( "/" )
    @Produces( { MediaType.TEXT_HTML, MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON } )
    public RestRepositories getRepositories( @Context HttpServletRequest request ) {
        return serverHandler.getRepositories(request);
    }

    /**
     * @see JcrResources#getWorkspaces(javax.servlet.http.HttpServletRequest, String)
     */
    @GET
    @Path( "{repositoryName}" )
    @Produces( { MediaType.TEXT_HTML, MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON } )
    public RestWorkspaces getWorkspaces( @Context HttpServletRequest request,
                                         @PathParam( "repositoryName" ) String rawRepositoryName ) throws RepositoryException {
        return repositoryHandler.getWorkspaces(request, rawRepositoryName);
    }

    /**
     * Retrieves the binary content of the binary property at the given path, allowing 2 extra (optional) parameters: the mime-type
     * and the content-disposition of the binary value.
     *
     * @param request a non-null {@link HttpServletRequest} request
     * @param repositoryName a non-null {@link String} representing the name of a repository.
     * @param workspaceName a non-null {@link String} representing the name of a workspace.
     * @param path a non-null {@link String} representing the absolute path to a binary property.
     * @param mimeType an optional {@link String} representing the "already-known" mime-type of the binary. Can be {@code null}
     * @param contentDisposition an optional {@link String} representing the client-preferred content disposition of the respose.
     * Can be {@code null}
     * @return the binary stream of the requested binary property
     * @throws RepositoryException if any JCR related operation fails, including the case when the path to the property isn't valid.
     */
    @GET
    @Path( "{repositoryName}/{workspaceName}/" + RestHelper.BINARY_METHOD_NAME + "{path:.+}" )
    @Produces( { MediaType.TEXT_HTML, MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON } )
    public Response getBinary( @Context HttpServletRequest request,
                               @PathParam( "repositoryName" ) String repositoryName,
                               @PathParam( "workspaceName" ) String workspaceName,
                               @PathParam( "path" ) String path,
                               @QueryParam( "mimeType" ) String mimeType,
                               @QueryParam( "contentDisposition" ) String contentDisposition )
            throws RepositoryException {
        Property binaryProperty = binaryHandler.getBinaryProperty(request, repositoryName, workspaceName, path);
        Binary binary = binaryProperty.getBinary();
        if (StringUtil.isBlank(mimeType)) {
            mimeType = binaryHandler.getDefaultMimeType(binaryProperty);
        }
        if (StringUtil.isBlank(contentDisposition)) {
            contentDisposition = binaryHandler.getDefaultContentDisposition(binaryProperty);
        }
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
    @Produces( { MediaType.TEXT_HTML, MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON } )
    public RestNodeType getNodeType( @Context HttpServletRequest request,
                                     @PathParam( "repositoryName" ) String repositoryName,
                                     @PathParam( "workspaceName" ) String workspaceName,
                                     @PathParam( "nodeTypeName" ) String nodeTypeName )
            throws RepositoryException {
        return nodeTypeHandler.getNodeType(request, repositoryName, workspaceName, nodeTypeName);
    }

    /**
     * Imports a single CND file into the repository. The CND file should be submitted as the body of the request.
     *
     * @param request a non-null {@link HttpServletRequest} request
     * @param repositoryName a non-null {@link String} representing the name of a repository.
     * @param workspaceName a non-null {@link String} representing the name of a workspace.
     * @param allowUpdate an optional parameter which indicates whether existing node types should be updated (overridden) or not.
     * @return a list with the registered node types if the operation was successful, or an appropriate error code otherwise.
     * @throws RepositoryException if any JCR related operation fails.
     */
    @POST
    @Path( "{repositoryName}/{workspaceName}/" + RestHelper.NODE_TYPES_METHOD_NAME )
    @Produces( { MediaType.APPLICATION_JSON, MediaType.TEXT_HTML, MediaType.TEXT_PLAIN } )
    public Response postCND( @Context HttpServletRequest request,
                             @PathParam( "repositoryName" ) String repositoryName,
                             @PathParam( "workspaceName" ) String workspaceName,
                             @QueryParam( "allowUpdate" ) @DefaultValue( "true" ) boolean allowUpdate,
                             InputStream requestBodyInputStream )
            throws JSONException, RepositoryException {
        return nodeTypeHandler.importCND(request, repositoryName, workspaceName, allowUpdate, requestBodyInputStream);
    }

    /**
     * Imports a single CND file into the repository, using a {@link MediaType#MULTIPART_FORM_DATA} request. The CND file
     * is expected to be submitted from an HTML element with the name <i>file</i>
     *
     * @see ModeShapeRestService#postCND(javax.servlet.http.HttpServletRequest, String, String, boolean, java.io.InputStream)
     */
    @POST
    @Path( "{repositoryName}/{workspaceName}/" + RestHelper.NODE_TYPES_METHOD_NAME )
    @Produces( { MediaType.TEXT_HTML, MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN } )
    @Consumes( MediaType.MULTIPART_FORM_DATA )
    public Response postCNDViaForm( @Context HttpServletRequest request,
                                    @PathParam( "repositoryName" ) String repositoryName,
                                    @PathParam( "workspaceName" ) String workspaceName,
                                    @QueryParam( "allowUpdate" ) @DefaultValue( "true" ) boolean allowUpdate,
                                    @MultipartForm FileUploadForm form )
            throws JSONException, RepositoryException, IOException {
        form.validate();
        return nodeTypeHandler.importCND(request, repositoryName, workspaceName, allowUpdate, form.getFileData());
    }

    /**
     * @see JcrResources#getItem(javax.servlet.http.HttpServletRequest, String, String, String, int, int)
     */
    @GET
    @Path( "{repositoryName}/{workspaceName}/" + RestHelper.ITEMS_METHOD_NAME + "{path:.*}" )
    @Produces( { MediaType.TEXT_HTML, MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON } )
    public RestItem getItem( @Context HttpServletRequest request,
                             @PathParam( "repositoryName" ) String rawRepositoryName,
                             @PathParam( "workspaceName" ) String rawWorkspaceName,
                             @PathParam( "path" ) String path,
                             @QueryParam( "depth" ) @DefaultValue( "0" ) int depth )
            throws JSONException, RepositoryException {
        return itemHandler.item(request, rawRepositoryName, rawWorkspaceName, path, depth);
    }

    /**
     * @see JcrResources#postItem(javax.servlet.http.HttpServletRequest, String, String, String, String, String)
     */
    @POST
    @Consumes( MediaType.APPLICATION_JSON )
    @Path( "{repositoryName}/{workspaceName}/" + RestHelper.ITEMS_METHOD_NAME + "{path:.+}" )
    @Produces( { MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.TEXT_HTML } )
    public Response postItem( @Context HttpServletRequest request,
                              @PathParam( "repositoryName" ) String rawRepositoryName,
                              @PathParam( "workspaceName" ) String rawWorkspaceName,
                              @PathParam( "path" ) String path,
                              String requestContent )
            throws RepositoryException, JSONException {
        return itemHandler.addItem(request, rawRepositoryName, rawWorkspaceName, path, requestContent);
    }

    /**
     * Performs a bulk creation of items via a single session, using the body of the request,
     * which is expected to be a valid JSON object.
     *
     * The format of the JSON request must be an object of the form:
     * <ul>
     *     <li>
     * {
     *   "node1_path" : {
     *       node1_body
     *   },
     *   "node2_path": {
     *       node2_body
     *   }
     *   ...
     * }
     *     </li>
     *     <li>
     *      {
     *   "property1_path" : {
     *       property1_body
     *   },
     *   "property2_path": {
     *       property2_body
     *   }
     *   ...
     * }
     *     </li>
     *     <li>
     *       {
     *   "property1_path" : {
     *       property1_body
     *   },
     *   "node1_path": {
     *       node1_body
     *   }
     *   ...
     * }
     * </li>
     * </ul>
     *
     *
     * where each body (either of a property or of a node) is expected to be a JSON object which has the same format as
     * the one used when creating a single item.
     *
     * @see ModeShapeRestService#postItem(javax.servlet.http.HttpServletRequest, String, String, String, String)
     */
    @POST
    @Consumes( MediaType.APPLICATION_JSON )
    @Path( "{repositoryName}/{workspaceName}/" + RestHelper.ITEMS_METHOD_NAME )
    @Produces( { MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.TEXT_HTML } )
    public Response postItems( @Context HttpServletRequest request,
                               @PathParam( "repositoryName" ) String rawRepositoryName,
                               @PathParam( "workspaceName" ) String rawWorkspaceName,
                               String requestContent )
            throws RepositoryException, JSONException {
        return itemHandler.addItems(request, rawRepositoryName, rawWorkspaceName, requestContent);
    }

    /**
     * @see JcrResources#deleteItem(javax.servlet.http.HttpServletRequest, String, String, String)
     */
    @DELETE
    @Path( "{repositoryName}/{workspaceName}/" + RestHelper.ITEMS_METHOD_NAME + "{path:.+}" )
    public Response deleteItem( @Context HttpServletRequest request,
                                @PathParam( "repositoryName" ) String rawRepositoryName,
                                @PathParam( "workspaceName" ) String rawWorkspaceName,
                                @PathParam( "path" ) String path )
            throws RepositoryException {
        itemHandler.deleteItem(request, rawRepositoryName, rawWorkspaceName, path);
        return Response.noContent().build();
    }

    /**
     * Performs a bulk deletion of nodes via a single session, using the body of the request,
     * which is expected to be a valid JSON array.
     *
     * The format of the JSON request must an array of the form:
     * <ul>
     *     <li>["node1_path", "node2_path",...]</li>
     *     <li>["property1_path", "property2_path",...]</li>
     *     <li>["property1_path", "node1_path",...]</li>
     * </ul>

     * @see {@link ModeShapeRestService#deleteItem(javax.servlet.http.HttpServletRequest, String, String, String)}
     */
    @DELETE
    @Consumes( MediaType.APPLICATION_JSON )
    @Path( "{repositoryName}/{workspaceName}/" + RestHelper.ITEMS_METHOD_NAME )
    public Response deleteItems( @Context HttpServletRequest request,
                                 @PathParam( "repositoryName" ) String rawRepositoryName,
                                 @PathParam( "workspaceName" ) String rawWorkspaceName,
                                 String requestContent )
            throws RepositoryException, JSONException {
        return itemHandler.deleteItems(request, rawRepositoryName, rawWorkspaceName, requestContent);
    }

    /**
     * @see JcrResources#putItem(javax.servlet.http.HttpServletRequest, String, String, String, String)
     */
    @PUT
    @Path( "{repositoryName}/{workspaceName}/" + RestHelper.ITEMS_METHOD_NAME + "{path:.+}" )
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( { MediaType.APPLICATION_JSON, MediaType.TEXT_HTML, MediaType.TEXT_PLAIN } )
    public RestItem putItem( @Context HttpServletRequest request,
                             @PathParam( "repositoryName" ) String rawRepositoryName,
                             @PathParam( "workspaceName" ) String rawWorkspaceName,
                             @PathParam( "path" ) String path,
                             String requestContent ) throws JSONException, RepositoryException {
        return itemHandler.updateItem(request, rawRepositoryName, rawWorkspaceName, path, requestContent);
    }
    /**
     * Performs a bulk update of items via a single session, using the body of the request,
     * which is expected to be a valid JSON object.
     *
     * The format of the JSON request must be an object of the form:
     * <ul>
     *     <li>
     * {
     *   "node1_path" : {
     *       node1_body
     *   },
     *   "node2_path": {
     *       node2_body
     *   }
     *   ...
     * }
     *     </li>
     *     <li>
     *      {
     *   "property1_path" : {
     *       property1_body
     *   },
     *   "property2_path": {
     *       property2_body
     *   }
     *   ...
     * }
     *     </li>
     *     <li>
     *       {
     *   "property1_path" : {
     *       property1_body
     *   },
     *   "node1_path": {
     *       node1_body
     *   }
     *   ...
     * }
     * </li>
     * </ul>
     *
     * where each body (either of a property or of a node) is expected to be a JSON object which has the same format as
     * the one used when updating a single item.
     *
     * @see ModeShapeRestService#putItem(javax.servlet.http.HttpServletRequest, String, String, String, String)
     */
    @PUT
    @Path( "{repositoryName}/{workspaceName}/" + RestHelper.ITEMS_METHOD_NAME )
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( { MediaType.APPLICATION_JSON, MediaType.TEXT_HTML, MediaType.TEXT_PLAIN } )
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
     * @param requestBodyInputStream a non-null {@link InputStream} stream which represents the body of the request, where
     * the binary content is expected.
     * @return a representation of the binary property that was created/updated.
     *
     * @throws RepositoryException if any JCR related operation fails.
     */
    @POST
    @Path( "{repositoryName}/{workspaceName}/" + RestHelper.BINARY_METHOD_NAME + "{path:.+}" )
    @Produces( { MediaType.APPLICATION_JSON, MediaType.TEXT_HTML, MediaType.TEXT_PLAIN } )
    public Response postBinary( @Context HttpServletRequest request,
                                @PathParam( "repositoryName" ) String repositoryName,
                                @PathParam( "workspaceName" ) String workspaceName,
                                @PathParam( "path" ) String path,
                                InputStream requestBodyInputStream )
            throws RepositoryException {
        return binaryHandler.updateBinary(request, repositoryName, workspaceName, path, requestBodyInputStream, true);
    }

    /**
     * Updates a binary property in the repository, at the given path. If the binary property does not exist, the NOT_FOUND
     * http response code is returned. The binary content is expected to be written directly to the request body.
     *
     * @param request a non-null {@link HttpServletRequest} request
     * @param repositoryName a non-null {@link String} representing the name of a repository.
     * @param workspaceName a non-null {@link String} representing the name of a workspace.
     * @param path a non-null {@link String} representing the absolute path to an existing binary property.
     * @param requestBodyInputStream a non-null {@link InputStream} stream which represents the body of the request, where
     * the binary content is expected.
     * @return a representation of the binary property that was updated.
     *
     * @throws RepositoryException if any JCR related operation fails.
     */
    @PUT
    @Path( "{repositoryName}/{workspaceName}/" + RestHelper.BINARY_METHOD_NAME + "{path:.+}" )
    @Produces( { MediaType.APPLICATION_JSON, MediaType.TEXT_HTML, MediaType.TEXT_PLAIN } )
    public Response putBinary( @Context HttpServletRequest request,
                               @PathParam( "repositoryName" ) String repositoryName,
                               @PathParam( "workspaceName" ) String workspaceName,
                               @PathParam( "path" ) String path,
                               InputStream requestBodyInputStream )
            throws RepositoryException {
        return binaryHandler.updateBinary(request, repositoryName, workspaceName, path, requestBodyInputStream, false);
    }

    /**
     * Creates/updates a binary file into the repository, using a {@link MediaType#MULTIPART_FORM_DATA} request. The binary file
     * is expected to be submitted from an HTML element with the name <i>file</i>
     *
     * @see ModeShapeRestService#postBinary(javax.servlet.http.HttpServletRequest, String, String, String, java.io.InputStream)
     */
    @POST
    @Path( "{repositoryName}/{workspaceName}/" + RestHelper.BINARY_METHOD_NAME + "{path:.+}" )
    @Produces( { MediaType.TEXT_HTML, MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN } )
    @Consumes( MediaType.MULTIPART_FORM_DATA )
    public Response postBinaryViaForm( @Context HttpServletRequest request,
                                       @PathParam( "repositoryName" ) String repositoryName,
                                       @PathParam( "workspaceName" ) String workspaceName,
                                       @PathParam( "path" ) String path,
                                       @MultipartForm FileUploadForm form )
            throws RepositoryException {
        form.validate();
        return binaryHandler.updateBinary(request, repositoryName, workspaceName, path, form.getFileData(), true);
    }

    /**
     * @see JcrResources#postXPathQuery(javax.servlet.http.HttpServletRequest, String, String, long, long, javax.ws.rs.core.UriInfo, String)
     */
    @POST
    @Path( "{repositoryName}/{workspaceName}/query" )
    @Consumes( "application/jcr+xpath" )
    @Produces( { MediaType.APPLICATION_JSON, MediaType.TEXT_HTML, MediaType.TEXT_PLAIN } )
    public RestQueryResult postXPathQuery( @Context HttpServletRequest request,
                                           @PathParam( "repositoryName" ) String rawRepositoryName,
                                           @PathParam( "workspaceName" ) String rawWorkspaceName,
                                           @QueryParam( "offset" ) @DefaultValue( "-1" ) long offset,
                                           @QueryParam( "limit" ) @DefaultValue( "-1" ) long limit,
                                           @Context UriInfo uriInfo,
                                           String requestContent ) throws RepositoryException {
        return queryHandler.executeQuery(request, rawRepositoryName,
                                         rawWorkspaceName,
                                         Query.XPATH,
                                         requestContent,
                                         offset,
                                         limit,
                                         uriInfo);
    }

    /**
     * @see JcrResources#postJcrSqlQuery(javax.servlet.http.HttpServletRequest, String, String, long, long, javax.ws.rs.core.UriInfo, String)
     */
    @POST
    @Path( "{repositoryName}/{workspaceName}/query" )
    @Consumes( "application/jcr+sql" )
    @Produces( { MediaType.APPLICATION_JSON, MediaType.TEXT_HTML, MediaType.TEXT_PLAIN } )
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
     * @see JcrResources#postJcrSql2Query(javax.servlet.http.HttpServletRequest, String, String, long, long, javax.ws.rs.core.UriInfo, String)
     */
    @POST
    @Path( "{repositoryName}/{workspaceName}/query" )
    @Consumes( "application/jcr+sql2" )
    @Produces( { MediaType.APPLICATION_JSON, MediaType.TEXT_HTML, MediaType.TEXT_PLAIN } )
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
     * @see JcrResources#postJcrSearchQuery(javax.servlet.http.HttpServletRequest, String, String, long, long, javax.ws.rs.core.UriInfo, String)
     */
    @POST
    @Path( "{repositoryName}/{workspaceName}/query" )
    @Consumes( "application/jcr+search" )
    @Produces( { MediaType.APPLICATION_JSON, MediaType.TEXT_HTML, MediaType.TEXT_PLAIN } )
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
}
