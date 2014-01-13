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

package org.modeshape.web.jcr.rest.output;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
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

/**
 * Implementation of {@link MessageBodyWriter} which writes a {@link JSONAble} or a {@link Collection Collection<JSONAble>} instances to
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
            Class<?> baseType = Types.getCollectionBaseType(type, genericType);
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
                         OutputStream entityStream ) throws WebApplicationException {
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

        try {
            PrintStream ps = new PrintStream(new BufferedOutputStream(entityStream), true, "UTF-8");
            String contentTypeHeader = mediaType.toString() + ";charset=utf-8";
            httpHeaders.putSingle("Content-Type", contentTypeHeader);
            ps.print(content);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    protected String getString( JSONAble jsonAble ) throws JSONException {
        return jsonAble.toJSON().toString();
    }

    @SuppressWarnings( "unused" )
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
