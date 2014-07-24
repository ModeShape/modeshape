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

import static javax.ws.rs.core.Response.Status;
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
import org.modeshape.jcr.api.Logger;
import org.modeshape.web.jcr.NoSuchRepositoryException;
import org.modeshape.web.jcr.WebLogger;
import org.modeshape.web.jcr.rest.handler.AbstractHandler;
import org.modeshape.web.jcr.rest.model.RestException;

/**
 * {@link ExceptionMapper} implementation which handles all thrown {@link Throwable} instances for the ModeShape REST service.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @param <T> a {@link Throwable} instance
 */
@Provider
public class ModeShapeExceptionMapper<T extends Throwable> implements ExceptionMapper<T> {

    private static final Logger LOGGER = WebLogger.getLogger(ModeShapeExceptionMapper.class);

    @Override
    public Response toResponse( T throwable ) {
        //always cleanup a potential active session is case of an exception
        AbstractHandler.cleanupActiveSession();

        if (throwable instanceof NotFoundException ||
            throwable instanceof PathNotFoundException ||
            throwable instanceof NoSuchWorkspaceException ||
            throwable instanceof NoSuchRepositoryException ||
            throwable instanceof NoSuchNodeTypeException) {
            return exceptionResponse(throwable, Status.NOT_FOUND);
        }

        if (throwable instanceof JSONException ||
            throwable instanceof InvalidQueryException ||
            throwable instanceof RepositoryException ||
            throwable instanceof IllegalArgumentException) {
            return exceptionResponse(throwable, Status.BAD_REQUEST);
        }

        return exceptionResponse(throwable, Status.INTERNAL_SERVER_ERROR);
    }


    private Response exceptionResponse( Throwable t,
                                        Status status ) {
        switch (status) {
            case NOT_FOUND: {
                LOGGER.debug(t, "Item not found");
                break;
            }
            default: {
                LOGGER.error(t, "Server error");
                break;
            }
        }
        return Response.status(status).entity(new RestException(t)).build();
    }
}
