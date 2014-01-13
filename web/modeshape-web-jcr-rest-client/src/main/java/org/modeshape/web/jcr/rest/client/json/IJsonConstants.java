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

/**
 * The <code>IJsonConstants</code> interface provides JSON-specific constants used when JSON as a transport mechanism between the
 * REST client and the REST server.
 */
public interface IJsonConstants {

    /**
     * The HTTP method to use when creating a connection with the REST server.
     */
    enum RequestMethod {
        /**
         * The HTTP DELETE request method.
         */
        DELETE,

        /**
         * The HTTP GET request method.
         */
        GET,

        /**
         * The HTTP POST request method.
         */
        POST,

        /**
         * The HTTP PUT request method.
         */
        PUT
    }

    /**
     * The key in the <code>JSONObject</code> whose value is the collection of node children.
     */
    String CHILDREN_KEY = "children";

    /**
     * The key in the <code>JSONObject</code> whose value is the collection of node properties.
     */
    String PROPERTIES_KEY = "properties";

    /**
     * The workspace context added to the URLs.
     */
    String WORKSPACE_CONTEXT = "/items";

    /**
     * The segment added to the URLs for queries.
     */
    String QUERY_CONTEXT = "/query";

    /**
     * The segment added to the URLs for queries.
     */
    String QUERY_PLAN_CONTEXT = "/queryPlan";

    /**
     * The suffix appended to properties whose values are base64-encoded
     */
    String BASE64_SUFFIX = "/base64/";

}
