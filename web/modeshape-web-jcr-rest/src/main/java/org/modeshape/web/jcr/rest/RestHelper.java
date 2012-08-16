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

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.modeshape.common.text.UrlEncoder;
import org.modeshape.common.util.Base64;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.api.Logger;
import org.modeshape.web.jcr.WebLogger;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utility class for the rest services.
 *
 * @author Horia Chiorean
 */
public final class RestHelper {

    public static final UrlEncoder URL_ENCODER = new UrlEncoder();

    private static final Logger LOGGER = WebLogger.getLogger(RestHelper.class);

    private RestHelper() {
    }

    /**
     * @deprecated since 3.x, dedicated writers are used for the output
     */
    public static String responseString( Object object,
                                            HttpServletRequest request ) throws JSONException {
        String acceptHeader = request.getHeader("Accept");
        if (StringUtil.isBlank(acceptHeader)) {
            return responseAsText(object);
        }
        acceptHeader = acceptHeader.toLowerCase();
        if (acceptHeader.contains(MediaType.APPLICATION_JSON.toLowerCase())) {
            return responseAsApplicationJSON(object);
        } else if (acceptHeader.contains(MediaType.TEXT_HTML.toLowerCase())) {
            return responseAsHTML(object);
        }
        return responseAsText(object);
    }

    private static String responseAsText( Object object ) throws JSONException {
        if (object instanceof JSONObject) {
            return ((JSONObject) object).toString(2);
        }
        else if (object instanceof JSONArray) {
            return ((JSONArray) object).toString(1);
        }
        return object.toString();
    }

    private static String responseAsApplicationJSON( Object object ) {
        return object.toString();
    }

    private static String responseAsHTML( Object object ) throws JSONException {
        String indentedString = responseAsText(object);
        indentedString = indentedString.replaceAll("\n", "<br/>").replaceAll("\\\\", "").replaceAll("\\s", "&nbsp;");
        return "<code>" + indentedString + "</code>";
    }

    public static String urlFrom( HttpServletRequest request,
                                  String... pathSegments ) {
        return urlFrom(request.getRequestURL().toString(), pathSegments);
    }

    public static String urlFrom( String baseUrl,
                                  String... pathSegments ) {
        StringBuilder urlBuilder = new StringBuilder(baseUrl);
        if (urlBuilder.charAt(urlBuilder.length() - 1) == '/') {
            urlBuilder.deleteCharAt(urlBuilder.length() - 1);
        }
        for (String pathSegment : pathSegments) {
            if (pathSegment.equalsIgnoreCase("..")) {
                urlBuilder.delete(urlBuilder.lastIndexOf("/"), urlBuilder.length());
            } else {
                if (!pathSegment.startsWith("/")) {
                    urlBuilder.append("/");
                }
                urlBuilder.append(pathSegment);
            }
        }
        return urlBuilder.toString();
    }

    /**
     * Return the JSON-compatible string representation of the given property value. If the value is a {@link javax.jcr.PropertyType#BINARY
     * binary} value, then this method returns the Base-64 encoding of that value. Otherwise, it just returns the string
     * representation of the value.
     *
     * @param value the property value; may not be null
     * @return the string representation of the value
     *
     * @deprecated since 3.x binary values are handled via URLs
     */
    public static String jsonEncodedStringFor( Value value ) {
        try {
            if (value.getType() != PropertyType.BINARY) {
                return value.getString();
            }

            // Encode the binary value in Base64 ...
            InputStream stream = value.getBinary().getStream();
            try {
                return Base64.encode(stream);
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        // Error accessing the value, so throw this ...
                        LOGGER.error(e.getMessage(), e);
                    }
                }
            }
        } catch (RepositoryException e) {
            LOGGER.error(e.getMessage(), e);
            return null;
        }
    }


}
