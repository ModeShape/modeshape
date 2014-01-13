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
package org.modeshape.web.jcr.rest.client.json;

import org.modeshape.common.util.CheckArg;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

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
