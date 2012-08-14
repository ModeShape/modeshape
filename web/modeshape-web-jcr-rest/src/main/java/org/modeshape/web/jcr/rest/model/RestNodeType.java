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

package org.modeshape.web.jcr.rest.model;

import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.PropertyDefinition;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.modeshape.web.jcr.rest.RestHelper;
import java.util.ArrayList;
import java.util.List;

/**
 * A REST representation of a {@link NodeType}
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public final class RestNodeType implements JSONAble {

    private final List<String> superTypesLinks;
    private final List<String> subTypesLinks;
    private final List<RestPropertyType> propertyTypes;

    private final String name;
    private final boolean isMixin;
    private final boolean hasOrderableChildNodes;
    private final boolean isAbstract;
    private final boolean isQueryable;

    public RestNodeType(NodeType nodeType, String baseUrl) {
        this.name = nodeType.getName();
        this.isMixin = nodeType.isMixin();
        this.isAbstract = nodeType.isAbstract();
        this.isQueryable = nodeType.isQueryable();
        this.hasOrderableChildNodes = nodeType.hasOrderableChildNodes();

        this.superTypesLinks = new ArrayList<String>();
        for (NodeType superType : nodeType.getDeclaredSupertypes()) {
            String superTypeLink = RestHelper.urlFrom(baseUrl, RestHelper.NODE_TYPES_METHOD_NAME, superType.getName());
            this.superTypesLinks.add(superTypeLink);
        }

        this.subTypesLinks = new ArrayList<String>();
        for (NodeTypeIterator subTypeIterator = nodeType.getDeclaredSubtypes(); subTypeIterator.hasNext(); ) {
            String subTypeLink = RestHelper.urlFrom(baseUrl, RestHelper.NODE_TYPES_METHOD_NAME,
                                                    subTypeIterator.nextNodeType().getName());
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
