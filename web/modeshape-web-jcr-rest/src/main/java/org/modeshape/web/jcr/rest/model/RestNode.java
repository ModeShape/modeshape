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

package org.modeshape.web.jcr.rest.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.modeshape.common.collection.Collections;

/**
 * A REST representation of a {@link javax.jcr.Node}
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public final class RestNode extends RestItem {

    public static final String SELF_FIELD_NAME = "self";
    public static final String UP_FIELD_NAME = "up";
    public static final String ID_FIELD_NAME = "id";
    public static final String CHILDREN_FIELD_NAME = "children";

    private static final Set<String> RESERVED_FIELD_NAMES = Collections.unmodifiableSet(SELF_FIELD_NAME,
                                                                                        UP_FIELD_NAME,
                                                                                        ID_FIELD_NAME,
                                                                                        CHILDREN_FIELD_NAME);

    private final List<RestProperty> jcrProperties;
    private final List<RestNode> children;
    private final Map<String, String> customProperties;
    protected final String id;

    /**
     * Creates a new rest node
     * 
     * @param name a {@code non-null} string, representing the name
     * @param id the node identifier
     * @param url a {@code non-null} string, representing the url to this node
     * @param parentUrl a {@code non-null} string, representing the url to this node's parent
     */
    public RestNode( String name,
                     String id,
                     String url,
                     String parentUrl ) {
        super(name, url, parentUrl);
        this.id = id;
        jcrProperties = new ArrayList<RestProperty>();
        children = new ArrayList<RestNode>();
        customProperties = new TreeMap<String, String>();
    }

    /**
     * Adds a new child to this node.
     * 
     * @param child a {@code non-null} {@link RestNode}
     * @return this rest node.
     */
    public RestNode addChild( RestNode child ) {
        children.add(child);
        return this;
    }

    /**
     * Adds a new jcr property to this node.
     * 
     * @param property a {@code non-null} {@link RestProperty}
     * @return this rest node.
     */
    public RestNode addJcrProperty( RestProperty property ) {
        jcrProperties.add(property);
        return this;
    }

    /**
     * Adds a custom property to this node, meaning a property which is not among the standard JCR properties
     * 
     * @param name a {@code non-null} String, representing the name of the custom property
     * @param value a {@code non-null} String, representing the value of the custom property
     * @return this instance, with the custom property added
     */
    public RestNode addCustomProperty( String name,
                                       String value ) {
        customProperties.put(name, value);
        return this;
    }

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject node = new JSONObject();

        // do these first so that they appear first in the JSON ...
        node.put(SELF_FIELD_NAME, url);
        node.put(UP_FIELD_NAME, parentUrl);
        node.put(ID_FIELD_NAME, id);

        addCustomProperties(node);
        addJcrProperties(node);
        addChildren(node);

        return node;
    }

    private boolean isReservedField( String fieldName ) {
        return RESERVED_FIELD_NAMES.contains(fieldName);
    }

    private void addChildren( JSONObject node ) throws JSONException {
        // children
        if (!children.isEmpty()) {
            JSONObject children = new JSONObject();
            for (RestNode child : this.children) {
                children.put(child.name, child.toJSON());
            }
            node.put(CHILDREN_FIELD_NAME, children);
        }
    }

    private void addJcrProperties( JSONObject node ) throws JSONException {
        // properties
        for (RestProperty restProperty : jcrProperties) {
            if (isReservedField(restProperty.name)) continue; // skip
            if (restProperty.isMultiValue()) {
                node.put(restProperty.name, restProperty.getValues());
            } else if (restProperty.getValue() != null) {
                node.put(restProperty.name, restProperty.getValue());
            }
        }
    }

    private void addCustomProperties( JSONObject node ) throws JSONException {
        // custom properties
        for (String customPropertyName : customProperties.keySet()) {
            if (isReservedField(customPropertyName)) continue; // skip
            node.put(customPropertyName, customProperties.get(customPropertyName));
        }
    }
}
