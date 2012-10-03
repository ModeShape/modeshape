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

import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.query.InvalidQueryException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import org.codehaus.jettison.json.JSONException;
import org.jboss.resteasy.spi.NotFoundException;
import org.modeshape.web.jcr.NoSuchRepositoryException;
import org.modeshape.web.jcr.rest.model.RestException;

/**
 * Class which holds all the RestEASY {@link ExceptionMapper} implementations used by the ModeShape's REST services.
 * 
 * @author Horia Chiorean
 * @see JcrApplication for the mechanism via which these mappers are registered.
 */
public final class ExceptionMappers {

    private ExceptionMappers() {
    }

    @Provider
    public static class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {

        @Override
        public Response toResponse( NotFoundException exception ) {
            return exceptionResponse(exception, Response.Status.NOT_FOUND);
        }
    }

    @Provider
    public static class PathNotFoundExceptionMapper implements ExceptionMapper<PathNotFoundException> {

        @Override
        public Response toResponse( PathNotFoundException exception ) {
            return exceptionResponse(exception, Response.Status.NOT_FOUND);
        }
    }

    @Provider
    public static class NoSuchWorkspaceExceptionMapper implements ExceptionMapper<NoSuchWorkspaceException> {

        @Override
        public Response toResponse( NoSuchWorkspaceException exception ) {
            return exceptionResponse(exception, Response.Status.NOT_FOUND);
        }

    }

    @Provider
    public static class NoSuchRepositoryExceptionMapper implements ExceptionMapper<NoSuchRepositoryException> {

        @Override
        public Response toResponse( NoSuchRepositoryException exception ) {
            return exceptionResponse(exception, Response.Status.NOT_FOUND);
        }

    }

    @Provider
    public static class JSONExceptionMapper implements ExceptionMapper<JSONException> {

        @Override
        public Response toResponse( JSONException exception ) {
            return exceptionResponse(exception, Response.Status.BAD_REQUEST);
        }

    }

    @Provider
    public static class InvalidQueryExceptionMapper implements ExceptionMapper<InvalidQueryException> {

        @Override
        public Response toResponse( InvalidQueryException exception ) {
            return exceptionResponse(exception, Response.Status.BAD_REQUEST);
        }
    }

    @Provider
    public static class RepositoryExceptionMapper implements ExceptionMapper<RepositoryException> {

        @Override
        public Response toResponse( RepositoryException exception ) {
            return exceptionResponse(exception, Response.Status.BAD_REQUEST);
        }
    }

    @Provider
    public static class LoginExceptionMapper implements ExceptionMapper<LoginException> {

        @Override
        public Response toResponse( LoginException exception ) {
            return exceptionResponse(exception, Response.Status.UNAUTHORIZED);
        }
    }

    @Provider
    public static class NoSuchNodeTypeExceptionMapper implements ExceptionMapper<NoSuchNodeTypeException> {
        @Override
        public Response toResponse( NoSuchNodeTypeException exception ) {
            return exceptionResponse(exception, Response.Status.NOT_FOUND);
        }
    }

    @Provider
    public static class IllegalArgumentExceptionMapper implements ExceptionMapper<IllegalArgumentException> {
        @Override
        public Response toResponse( IllegalArgumentException exception ) {
            return exceptionResponse(exception, Response.Status.BAD_REQUEST);
        }
    }

    /**
     * Creates a new {@link Response} which has the expected status code, based on the given exception.
     * 
     * @param e a {@code non-null} {@link Exception}
     * @param status a {@code non-null} {@link javax.ws.rs.core.Response.Status} which will be returned as the code of the response.
     * @return a {@code non-null} {@link Response}
     */
    public static Response exceptionResponse( Exception e,
                                              Response.Status status ) {
        return Response.status(status).entity(new RestException(e)).build();
    }
}
