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
import java.util.Set;
import java.util.TreeSet;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.PropertyDefinition;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.modeshape.web.jcr.rest.RestHelper;

/**
 * A REST representation of a {@link NodeType}
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public final class RestNodeType implements JSONAble {

    private final Set<String> superTypesLinks;
    private final Set<String> subTypesLinks;
    private final List<RestPropertyType> propertyTypes;

    private final String name;
    private final boolean isMixin;
    private final boolean hasOrderableChildNodes;
    private final boolean isAbstract;
    private final boolean isQueryable;

    /**
     * Creates a new rest node type.
     *
     * @param nodeType the {@code non-null} JCR {@link NodeType}.
     * @param baseUrl the {@code non-null} root url, which is used to construct urls to the children and properties of the node type
     */
    public RestNodeType( NodeType nodeType,
                         String baseUrl ) {
        this.name = nodeType.getName();
        this.isMixin = nodeType.isMixin();
        this.isAbstract = nodeType.isAbstract();
        this.isQueryable = nodeType.isQueryable();
        this.hasOrderableChildNodes = nodeType.hasOrderableChildNodes();

        this.superTypesLinks = new TreeSet<String>();
        for (NodeType superType : nodeType.getDeclaredSupertypes()) {
            String superTypeLink = RestHelper.urlFrom(baseUrl, RestHelper.NODE_TYPES_METHOD_NAME, RestHelper.URL_ENCODER.encode(superType.getName()));
            this.superTypesLinks.add(superTypeLink);
        }

        this.subTypesLinks = new TreeSet<String>();
        for (NodeTypeIterator subTypeIterator = nodeType.getDeclaredSubtypes(); subTypeIterator.hasNext(); ) {
            String subTypeLink = RestHelper.urlFrom(baseUrl, RestHelper.NODE_TYPES_METHOD_NAME,
                                                    RestHelper.URL_ENCODER.encode(subTypeIterator.nextNodeType().getName()));
            this.subTypesLinks.add(subTypeLink);
        }

        this.propertyTypes = new ArrayList<RestPropertyType>();
        for (PropertyDefinition propertyDefinition : nodeType.getDeclaredPropertyDefinitions()) {
            this.propertyTypes.add(new RestPropertyType(propertyDefinition));
        }
    }

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject content = new JSONObject();
        content.put("mixin", isMixin);
        content.put("abstract", isAbstract);
        content.put("queryable", isQueryable);
        content.put("hasOrderableChildNodes", hasOrderableChildNodes);

        if (!propertyTypes.isEmpty()) {
            for (RestPropertyType restPropertyType : propertyTypes) {
                content.accumulate("propertyDefinitions", restPropertyType.toJSON());
            }
        }

        if (!superTypesLinks.isEmpty()) {
            content.put("superTypes", superTypesLinks);
        }

        if (!subTypesLinks.isEmpty()) {
            content.put("subTypes", subTypesLinks);
        }

        JSONObject result = new JSONObject();
        result.put(name, content);
        return result;
    }
}
