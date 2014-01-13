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

import java.net.URL;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.modeshape.web.jcr.rest.client.IJcrConstants;
import static org.modeshape.web.jcr.rest.client.json.IJsonConstants.CHILDREN_KEY;
import static org.modeshape.web.jcr.rest.client.json.IJsonConstants.PROPERTIES_KEY;

/**
 * The <code>JsonNode</code> class defines the API for interacting with JSON objects. Every <code>JsonNode</code> knows how to
 * create their URL and create their JCR content.
 */
public abstract class JsonNode extends JSONObject {

    private static final long serialVersionUID = 1L;

    // ===========================================================================================================================
    // Fields
    // ===========================================================================================================================

    /**
     * The node identifier.
     */
    private final String id;

    // ===========================================================================================================================
    // Constructors
    // ===========================================================================================================================

    /**
     * @param id the node identifier (never <code>null</code>)
     */
    protected JsonNode( String id ) {
        assert id != null;
        this.id = id;
    }

    // ===========================================================================================================================
    // Methods
    // ===========================================================================================================================

    /**
     * @return the content that gets published
     * @throws Exception if there is a problem obtaining the node content
     */
    public byte[] getContent() throws Exception {
        return super.toString().getBytes();
    }

    /**
     * @return a unique identifier for this node
     */
    public String getId() {
        return this.id;
    }

    /**
     * @return an HTTP URL representing this node
     * @throws Exception if there is a problem constructing the URL
     */
    public abstract URL getUrl() throws Exception;

    @Override
    public String toString() {
        StringBuilder txt = new StringBuilder();
        txt.append("ID: ").append(getId());
        txt.append(", URL: ");

        try {
            txt.append(getUrl());
        } catch (Exception e) {
            txt.append("exception obtaining URL");
        }

        txt.append(", content: ").append(super.toString());
        return txt.toString();
    }

    protected JsonNode withPrimaryType(String primaryType) throws JSONException {
        return withProperty(IJcrConstants.PRIMARY_TYPE_PROPERTY, primaryType);
    }

    protected JsonNode withMixin( String mixin ) throws JSONException {
        JSONObject properties = properties();
        if (!properties.has(IJcrConstants.MIXIN_TYPES_PROPERTY)) {
            withProperty(IJcrConstants.MIXIN_TYPES_PROPERTY, new JSONArray());
        }
        return withProperty(IJcrConstants.MIXIN_TYPES_PROPERTY, mixin);
    }

    protected JsonNode withProperty(String key, Object value) throws JSONException {
        JSONObject properties = properties();
        if (properties.has(key)) {
            Object existingValue = properties.get(key);
            if (existingValue != null && existingValue instanceof JSONArray) {
                ((JSONArray) existingValue).put(value);
                return this;
            }
        }
        properties.put(key, value);
        return this;
    }

    protected JsonNode withChild(String name, JSONObject child) throws JSONException {
        JSONObject children = children();
        children.put(name, child);
        return this;
    }

    protected JSONObject children() throws JSONException {
        if (!has(CHILDREN_KEY)) {
            put(CHILDREN_KEY, new JSONObject());
        }
        return getJSONObject(CHILDREN_KEY);
    }

    protected JSONObject properties() throws JSONException {
        if (!has(PROPERTIES_KEY)) {
            put(PROPERTIES_KEY, new JSONObject());
        }
        return getJSONObject(PROPERTIES_KEY);
    }
}
