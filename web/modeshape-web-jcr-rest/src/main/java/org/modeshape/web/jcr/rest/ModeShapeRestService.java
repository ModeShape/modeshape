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
import org.jboss.resteasy.spi.NotFoundException;
import org.jboss.resteasy.spi.UnauthorizedException;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.util.StringUtil;
import org.modeshape.web.jcr.rest.form.FileUploadForm;
import org.modeshape.web.jcr.rest.handler.RestBinaryHandler;
import org.modeshape.web.jcr.rest.handler.RestItemHandler;
import org.modeshape.web.jcr.rest.handler.RestQueryHandler;
import org.modeshape.web.jcr.rest.handler.RestRepositoryHandler;
import org.modeshape.web.jcr.rest.handler.RestServerHandler;
import org.modeshape.web.jcr.rest.model.RestException;
import org.modeshape.web.jcr.rest.model.RestItem;
import org.modeshape.web.jcr.rest.model.RestQueryResult;
import org.modeshape.web.jcr.rest.model.RestRepositories;
import org.modeshape.web.jcr.rest.model.RestWorkspaces;
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

    private RestServerHandler serverHandler = new RestServerHandler();
    private RestRepositoryHandler repositoryHandler = new RestRepositoryHandler();
    private RestItemHandler itemHandler = new RestItemHandler();
    private RestQueryHandler queryHandler = new RestQueryHandler();
    private RestBinaryHandler binaryHandler = new RestBinaryHandler();

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
     * @see JcrResources#postItem(javax.servlet.http.HttpServletRequest, String, String, String, String, String)
     */
    @POST
    @Path( "{repositoryName}/{workspaceName}/" + ITEMS_METHOD_NAME + "{path:.*}" )
    @Produces( { MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.TEXT_HTML } )
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
     * @see JcrResources#deleteItem(javax.servlet.http.HttpServletRequest, String, String, String)
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
     * @see JcrResources#putItem(javax.servlet.http.HttpServletRequest, String, String, String, String)
     */
    @PUT
    @Path( "{repositoryName}/{workspaceName}/" + ITEMS_METHOD_NAME + "{path:.*}" )
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( { MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.TEXT_HTML } )
    public RestItem putItem( @Context HttpServletRequest request,
                           @PathParam( "repositoryName" ) String rawRepositoryName,
                           @PathParam( "workspaceName" ) String rawWorkspaceName,
                           @PathParam( "path" ) String path,
                           String requestContent ) throws JSONException, RepositoryException {
        return itemHandler.updateItem(request, rawRepositoryName, rawWorkspaceName, path, requestContent);
    }

    @POST
    @Path( "{repositoryName}/{workspaceName}/" + BINARY_METHOD_NAME + "{path:.+}" )
    @Produces( { MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.TEXT_HTML } )
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
    @Produces( { MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.TEXT_HTML } )
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
    @Produces( { MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.TEXT_HTML } )
    @Consumes( MediaType.MULTIPART_FORM_DATA )
    public Response postBinaryMultipart( @Context HttpServletRequest request,
                                    @PathParam( "repositoryName" ) String repositoryName,
                                    @PathParam( "workspaceName" ) String workspaceName,
                                    @PathParam( "path" ) String path,
                                    @MultipartForm FileUploadForm form)
            throws RepositoryException {
        if (form.getFileData() == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity(
                    new RestException("Please make sure the file is uploaded from an HTML element with the name \"file\"")).build();
        }
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
    @Produces( { MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.TEXT_HTML } )
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
    @Produces( { MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.TEXT_HTML } )
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
