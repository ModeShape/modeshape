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

package org.modeshape.web.jcr.rest.output;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import org.codehaus.jettison.json.JSONException;
import org.modeshape.web.jcr.rest.model.JSONAble;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * @author Horia Chiorean
 */
@Provider
@Produces( { MediaType.APPLICATION_JSON } )
public class JSONBodyWriter implements MessageBodyWriter<JSONAble> {

    @Override
    public long getSize( JSONAble jsonAble,
                         Class<?> type,
                         Type genericType,
                         Annotation[] annotations,
                         MediaType mediaType ) {
        return getString(jsonAble).getBytes().length;
    }

    @Override
    public boolean isWriteable( Class<?> type,
                                Type genericType,
                                Annotation[] annotations,
                                MediaType mediaType ) {
        return JSONAble.class.isAssignableFrom(type);
    }

    @Override
    public void writeTo( JSONAble jsonAble,
                         Class<?> type,
                         Type genericType,
                         Annotation[] annotations,
                         MediaType mediaType,
                         MultivaluedMap<String, Object> httpHeaders,
                         OutputStream entityStream ) throws IOException, WebApplicationException {
        PrintWriter printWriter = new PrintWriter(new BufferedOutputStream(entityStream));
        printWriter.write(getString(jsonAble));
        printWriter.flush();
    }

    protected String getString(JSONAble jsonAble) {
        try {
            return jsonAble.toJSON().toString();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
