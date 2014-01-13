/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import org.modeshape.web.jcr.NoSuchRepositoryException;
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
        return Response.status(status).entity(new RestException(t)).build();
    }
}
