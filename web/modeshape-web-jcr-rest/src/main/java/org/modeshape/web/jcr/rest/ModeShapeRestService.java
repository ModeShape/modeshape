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

import javax.jcr.Property;
import javax.jcr.RepositoryException;
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
import org.codehaus.jettison.json.JSONException;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.jboss.resteasy.spi.NotFoundException;
import org.jboss.resteasy.spi.UnauthorizedException;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.api.Binary;
import org.modeshape.web.jcr.rest.form.FileUploadForm;
import org.modeshape.web.jcr.rest.handler.BinaryHandler;
import org.modeshape.web.jcr.rest.handler.QueryHandler;
import org.modeshape.web.jcr.rest.handler.RestItemHandler;
import org.modeshape.web.jcr.rest.handler.RestRepositoryHandler;
import org.modeshape.web.jcr.rest.handler.RestServerHandler;
import org.modeshape.web.jcr.rest.model.RestItem;
import org.modeshape.web.jcr.rest.model.RestProperty;
import org.modeshape.web.jcr.rest.model.RestRepositories;
import org.modeshape.web.jcr.rest.model.RestWorkspaces;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * @author Horia Chiorean
 */
@Immutable
@Path( "/v2" )
public final class ModeShapeRestService {

    public static final String BINARY_METHOD_NAME = "binary";
    public static final String ITEMS_METHOD_NAME = "items";
    public static final String QUERY_METHOD_NAME = "query";

    public static final String MIME_TYPE_PARAM_NAME = "mimeType";
    public static final String CONTENT_DISPOSITION_PARAM_NAME = "contentDisposition";

    /**
     * This is a duplicate of the FullTextSearchParser.LANGUAGE field, but it is split out here to avoid adding a dependency on
     * the modeshape-jcr package.
     */
    private final static String SEARCH_LANGUAGE = "Search";

    private RestServerHandler serverHandler = new RestServerHandler();
    private RestRepositoryHandler repositoryHandler = new RestRepositoryHandler();
    private RestItemHandler itemHandler = new RestItemHandler();
    private QueryHandler queryHandler = new QueryHandler();
    private BinaryHandler binaryHandler = new BinaryHandler();

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
     * @see JcrResources#getItem(javax.servlet.http.HttpServletRequest, String, String, String, int, int)
     */
    @GET
    @Path( "{repositoryName}/{workspaceName}/" + ITEMS_METHOD_NAME + "{path:.*}" )
    @Produces( { MediaType.TEXT_HTML, MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON } )
    public RestItem getItem( @Context HttpServletRequest request,
                           @PathParam( "repositoryName" ) String rawRepositoryName,
                           @PathParam( "workspaceName" ) String rawWorkspaceName,
                           @PathParam( "path" ) String path,
                           @QueryParam( "depth" ) @DefaultValue( "0" ) int depth )
            throws JSONException, RepositoryException {
        return itemHandler.item(request, rawRepositoryName, rawWorkspaceName, path, depth);
    }

    @GET
    @Path( "{repositoryName}/{workspaceName}/" + BINARY_METHOD_NAME + "{path:.+}" )
    @Produces( { MediaType.TEXT_HTML, MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON } )
    public Response getBinary( @Context HttpServletRequest request,
                               @PathParam( "repositoryName" ) String repositoryName,
                               @PathParam( "workspaceName" ) String workspaceName,
                               @PathParam( "path" ) String path,
                               @QueryParam( MIME_TYPE_PARAM_NAME ) String mimeType,
                               @QueryParam( CONTENT_DISPOSITION_PARAM_NAME ) String contentDisposition )
            throws RepositoryException {
        Property binaryProperty = binaryHandler.getBinaryProperty(request, repositoryName, workspaceName, path);
        Binary binary = (Binary)binaryProperty.getBinary();
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
     * Adds the content of the request as a node (or subtree of nodes) at the location specified by {@code path}.
     * <p>
     * The primary type and mixin type(s) may optionally be specified through the {@code jcr:primaryType} and
     * {@code jcr:mixinTypes} properties.
     * </p>
     *
     * @param request the servlet request; may not be null or unauthenticated
     * @param rawRepositoryName the URL-encoded repository name
     * @param rawWorkspaceName the URL-encoded workspace name
     * @param path the path to the item
     * @param requestContent the JSON-encoded representation of the node or nodes to be added
     * @return the JSON-encoded representation of the top node that was added.
     * @throws JSONException if there is an error encoding the node
     * @throws RepositoryException if any other error occurs
     */
    @POST
    @Path( "{repositoryName}/{workspaceName}/" + ITEMS_METHOD_NAME + "{path:.*}" )
    @Produces( { MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON, MediaType.TEXT_HTML } )
    public Response postItem( @Context HttpServletRequest request,
                              @PathParam( "repositoryName" ) String rawRepositoryName,
                              @PathParam( "workspaceName" ) String rawWorkspaceName,
                              @PathParam( "path" ) String path,
                              String requestContent )
            throws RepositoryException, JSONException {
        return itemHandler.addItem(request,
                                   rawRepositoryName,
                                   rawWorkspaceName,
                                   path,
                                   requestContent);
    }

    /**
     * Deletes the item at {@code path}.
     *
     * @param request the servlet request; may not be null or unauthenticated
     * @param rawRepositoryName the URL-encoded repository name
     * @param rawWorkspaceName the URL-encoded workspace name
     * @param path the path to the item
     * @return a http 204 code (No Content) if the deletion was successful, or an error code otherwise
     * @throws org.jboss.resteasy.spi.NotFoundException if no item exists at {@code path}
     * @throws org.jboss.resteasy.spi.UnauthorizedException if the user does not have the access required to delete the item at this path
     * @throws RepositoryException if any other error occurs
     */
    @DELETE
    @Path( "{repositoryName}/{workspaceName}/" + ITEMS_METHOD_NAME + "/{path:.*}" )
    public Response deleteItem( @Context HttpServletRequest request,
                                @PathParam( "repositoryName" ) String rawRepositoryName,
                                @PathParam( "workspaceName" ) String rawWorkspaceName,
                                @PathParam( "path" ) String path )
            throws NotFoundException, UnauthorizedException, RepositoryException {
        itemHandler.deleteItem(request, rawRepositoryName, rawWorkspaceName, path);
        return Response.noContent().build();
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
     * @return a {@link RestItem} instance representing the item that was changed by this operation.

     * @throws JSONException if there is an error encoding the node
     * @throws RepositoryException if any other error occurs
     */
    @PUT
    @Path( "{repositoryName}/{workspaceName}/" + ITEMS_METHOD_NAME + "{path:.*}" )
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( {MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON, MediaType.TEXT_HTML} )
    public RestItem putItem( @Context HttpServletRequest request,
                           @PathParam( "repositoryName" ) String rawRepositoryName,
                           @PathParam( "workspaceName" ) String rawWorkspaceName,
                           @PathParam( "path" ) String path,
                           String requestContent ) throws JSONException, RepositoryException {
        return itemHandler.updateItem(request, rawRepositoryName, rawWorkspaceName, path, requestContent);
    }

    @POST
    @Path( "{repositoryName}/{workspaceName}/" + BINARY_METHOD_NAME + "{path:.+}" )
    @Produces( { MediaType.TEXT_HTML, MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON } )
    public Response postBinary( @Context HttpServletRequest request,
                                    @PathParam( "repositoryName" ) String repositoryName,
                                    @PathParam( "workspaceName" ) String workspaceName,
                                    @PathParam( "path" ) String path,
                                    InputStream requestBodyInputStream )
            throws RepositoryException {
        return binaryHandler.updateBinary(request, repositoryName, workspaceName, path, requestBodyInputStream, true);
    }

    @PUT
    @Path( "{repositoryName}/{workspaceName}/" + BINARY_METHOD_NAME + "{path:.+}" )
    @Produces( { MediaType.TEXT_HTML, MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON } )
    public Response putBinary( @Context HttpServletRequest request,
                                   @PathParam( "repositoryName" ) String repositoryName,
                                   @PathParam( "workspaceName" ) String workspaceName,
                                   @PathParam( "path" ) String path,
                                   InputStream requestBodyInputStream )
            throws RepositoryException {
        return binaryHandler.updateBinary(request, repositoryName, workspaceName, path, requestBodyInputStream, false);
    }

    @POST
    @Path( "{repositoryName}/{workspaceName}/" + BINARY_METHOD_NAME + "{path:.+}" )
    @Produces( { MediaType.TEXT_HTML, MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON } )
    @Consumes( MediaType.MULTIPART_FORM_DATA )
    public Response postBinaryMultipart( @Context HttpServletRequest request,
                                    @PathParam( "repositoryName" ) String repositoryName,
                                    @PathParam( "workspaceName" ) String workspaceName,
                                    @PathParam( "path" ) String path,
                                    @MultipartForm FileUploadForm form)
            throws RepositoryException {
        if (form.getFileData() == null) {
            return ExceptionMappers.exceptionResponse(new IllegalArgumentException("Please make sure the file is uploaded from an HTML element with the name \"file\""),
                                                      Response.Status.BAD_REQUEST);
        }
        return binaryHandler.updateBinary(request, repositoryName, workspaceName, path,
                                          form.getFileData(), true);
    }
}
