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
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.jboss.resteasy.spi.WriterException;
import org.jboss.resteasy.util.Types;
import org.modeshape.web.jcr.rest.model.JSONAble;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;

/**
 * Implementation of {@link MessageBodyWriter} which writes a {@link JSONAble} or a {@link Collection<JSONAble>} instances to
 * a response, producing {@link MediaType#APPLICATION_JSON}.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
@Provider
@Produces( { MediaType.APPLICATION_JSON } )
public class JSONBodyWriter implements MessageBodyWriter<Object> {

    @Override
    @SuppressWarnings( "unchecked" )
    public long getSize( Object object,
                         Class<?> type,
                         Type genericType,
                         Annotation[] annotations,
                         MediaType mediaType ) {
        try {
            if (isJSONAble(type)) {
                return getString((JSONAble)object).getBytes().length;
            } else if (isJSONAbleCollection(type, genericType)) {
                return getString((Collection<JSONAble>)object).getBytes().length;
            }
            return 0;
        } catch (JSONException e) {
            throw new WriterException(e);
        }
    }

    @Override
    public boolean isWriteable( Class<?> type,
                                Type genericType,
                                Annotation[] annotations,
                                MediaType mediaType ) {
        return isJSONAble(type) || isJSONAbleCollection(type, genericType);
    }

    private boolean isJSONAble( Class<?> type ) {
        return JSONAble.class.isAssignableFrom(type);
    }

    private boolean isJSONAbleCollection( Class<?> type,
                                          Type genericType ) {
        if ((Collection.class.isAssignableFrom(type) || type.isArray()) && genericType != null) {
            Class baseType = Types.getCollectionBaseType(type, genericType);
            return baseType != null && JSONAble.class.isAssignableFrom(baseType);
        }
        return false;
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public void writeTo( Object object,
                         Class<?> type,
                         Type genericType,
                         Annotation[] annotations,
                         MediaType mediaType,
                         MultivaluedMap<String, Object> httpHeaders,
                         OutputStream entityStream ) throws IOException, WebApplicationException {
        String content;
        try {
            if (isJSONAble(type)) {
                content = getString((JSONAble)object);
            } else if (isJSONAbleCollection(type, genericType)) {
                content = getString((Collection<JSONAble>)object);
            } else {
                return;
            }
        } catch (JSONException e) {
            throw new WriterException(e);
        }

        PrintWriter printWriter = new PrintWriter(new BufferedOutputStream(entityStream));
        printWriter.write(content);
        printWriter.flush();
    }

    protected String getString( JSONAble jsonAble ) throws JSONException {
        return jsonAble.toJSON().toString();
    }

    protected String getString( JSONArray array ) throws JSONException {
        return array.toString();
    }

    private String getString( Collection<JSONAble> collection ) throws JSONException {
        return getString(toArray(collection));
    }

    private JSONArray toArray( Collection<JSONAble> collection ) throws JSONException {
        JSONArray array = new JSONArray();
        for (JSONAble jsonAble : collection) {
            array.put(jsonAble.toJSON());
        }
        return array;
    }
}
