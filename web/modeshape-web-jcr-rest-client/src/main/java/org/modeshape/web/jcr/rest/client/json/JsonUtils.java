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
package org.modeshape.web.jcr.rest.client.json;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import org.modeshape.common.util.CheckArg;

/**
 * The <code>JsonUtils</code> class provides utilities needed to work with the ModeShape REST server JSON API.
 */
public final class JsonUtils {

    // ===========================================================================================================================
    // Class Methods
    // ===========================================================================================================================

    /**
     * The default character set being used.
     */
    private static final String DEFAULT_CHARSET = "UTF-8"; // TODO need to property drive charset

    // ===========================================================================================================================
    // Class Methods
    // ===========================================================================================================================

    /**
     * @param text the text being URL decoded (never <code>null</code>)
     * @return the decoded text
     * @throws UnsupportedEncodingException if the charset is not supported
     */
    public static String decode( String text ) throws UnsupportedEncodingException {
        CheckArg.isNotNull(text, "text");
        return URLDecoder.decode(text, DEFAULT_CHARSET);
    }

    /**
     * Forward slashes ('/') are not encoded.
     * 
     * @param text the text being URL encoded (never <code>null</code>)
     * @return the decoded text
     * @throws UnsupportedEncodingException if the charset is not supported
     */
    public static String encode( String text ) throws UnsupportedEncodingException {
        CheckArg.isNotNull(text, "text");

        // don't encode '/' as it needs to stay that way in the URL
        StringBuilder encoded = new StringBuilder();

        for (char c : text.toCharArray()) {
            encoded.append((c == '/') ? c : URLEncoder.encode(Character.toString(c), DEFAULT_CHARSET));
        }

        return encoded.toString();
    }

    /**
     * Note: The connection is not disconnected during this method.
     * 
     * @param connection the connection whose input stream is going to be read from (never <code>null</code>)
     * @return the data read from the connection input stream (never <code>null</code>)
     * @throws IOException if there is a problem reading from the connection
     */
    public static String readInputStream( HttpURLConnection connection ) throws IOException {
        CheckArg.isNotNull(connection, "connection");

        InputStream stream = connection.getInputStream();
        int bytesRead;
        byte[] bytes = new byte[1024];
        StringBuffer buff = new StringBuffer();

        while (-1 != (bytesRead = stream.read(bytes, 0, 1024))) {
            buff.append(new String(bytes, 0, bytesRead));
        }

        return buff.toString();
    }

    // ===========================================================================================================================
    // Constructors
    // ===========================================================================================================================

    /**
     * Don't allow construction.
     */
    private JsonUtils() {
        // nothing to do
    }

}
