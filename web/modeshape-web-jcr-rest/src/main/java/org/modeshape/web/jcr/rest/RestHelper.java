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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.servlet.http.HttpServletRequest;
import org.modeshape.common.text.UrlEncoder;
import org.modeshape.jcr.api.ValueFactory;

/**
 * Utility class for the rest services and supporting classes.
 * 
 * @author Horia Chiorean
 */
public final class RestHelper {

    public static final UrlEncoder URL_ENCODER = new UrlEncoder();

    public static final String BINARY_METHOD_NAME = "binary";
    public static final String ITEMS_METHOD_NAME = "items";
    public static final String NODES_METHOD_NAME = "nodes";
    public static final String QUERY_METHOD_NAME = "query";
    public static final String QUERY_PLAN_METHOD_NAME = "queryPlan";
    public static final String NODE_TYPES_METHOD_NAME = "nodetypes";
    public static final String UPLOAD_METHOD_NAME = "upload";
    public static final String BACKUP_METHOD_NAME = "backup";
    public static final String RESTORE_METHOD_NAME = "restore";
    
    private static final List<String> ALL_METHODS = Arrays.asList(BINARY_METHOD_NAME,
                                                                  ITEMS_METHOD_NAME,
                                                                  NODES_METHOD_NAME,
                                                                  QUERY_METHOD_NAME,
                                                                  QUERY_PLAN_METHOD_NAME,
                                                                  NODE_TYPES_METHOD_NAME,
                                                                  UPLOAD_METHOD_NAME);
    // almost ISO8601, because in JDK 6 Z/z do not support timezones of the format hh:mm
    private static final List<SimpleDateFormat> ISO8601_DATE_PARSERS = Arrays.asList(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"),
                                                                                     new SimpleDateFormat("yyyy-MM-dd"));
    
    private RestHelper() {
    }

    /**
     * Determines the absolute URL to a repository/workspace from the given request, by trimming known service methods.
     * 
     * @param request a {@code non-null} {@link HttpServletRequest}
     * @return a string representing an absolute-url
     */
    public static String repositoryUrl( HttpServletRequest request ) {
        String requestURL = request.getRequestURL().toString();
        int delimiterSegmentIdx = requestURL.length();
        for (String methodName : ALL_METHODS) {
            if (requestURL.indexOf(methodName) != -1) {
                delimiterSegmentIdx = requestURL.indexOf(methodName);
                break;
            }
        }
        return requestURL.substring(0, delimiterSegmentIdx);
    }

    /**
     * Creates an absolute url using the given request's url as base and appending optional segments.
     * 
     * @param request a {@code non-null} {@link HttpServletRequest}
     * @param pathSegments an option array of segments
     * @return a string representing an absolute-url
     */
    public static String urlFrom( HttpServletRequest request,
                                  String... pathSegments ) {
        return urlFrom(request.getRequestURL().toString(), pathSegments);
    }

    /**
     * Creates an url using base url and appending optional segments.
     * 
     * @param baseUrl a {@code non-null} string which will act as a base.
     * @param pathSegments an option array of segments
     * @return a string representing an absolute-url
     */
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
     * Converts an object value coming from a JSON object, to a JCR value by attempting to convert it to a valid data type.
     * 
     * @param value a generic value, may be {@code null}
     * @param valueFactory a {@link ValueFactory} instance which is used to perform the conversion
     * @return either a {@link Value} or {@code null} if the object value is {@code null}.
     */
    public static Value jsonValueToJCRValue( Object value,
                                             ValueFactory valueFactory ) {
        if (value == null) {
            return null;
        }

        // try the datatypes that can be handled by Jettison
        if (value instanceof Integer || value instanceof Long) {
            return valueFactory.createValue(((Number)value).longValue());
        } else if (value instanceof Double || value instanceof Float) {
            return valueFactory.createValue(((Number)value).doubleValue());
        } else if (value instanceof Boolean) {
            return valueFactory.createValue((Boolean)value);
        }

        // try to convert to a date
        String valueString = value.toString();
        for (DateFormat dateFormat : ISO8601_DATE_PARSERS) {
            try {
                Date date = dateFormat.parse(valueString);
                return valueFactory.createValue(date);
            } catch (ParseException e) {
                // ignore
            } catch (ValueFormatException e) {
                // ignore
            }
        }

        // default to a string
        return valueFactory.createValue(valueString);
    }

}
