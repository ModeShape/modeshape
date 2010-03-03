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

import javax.jcr.RepositoryException;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import net.jcip.annotations.Immutable;
import org.codehaus.jettison.json.JSONException;
import org.jboss.resteasy.spi.NotFoundException;

/**
 * RESTEasy exception handlers for the Modeshape REST server.
 */
@Immutable
@Path( "/" )
public class JcrResources extends AbstractJcrResource {

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
