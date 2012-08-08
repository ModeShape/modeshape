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

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Horia Chiorean
 */
public final class RestNode extends RestItem {

    private final List<RestProperty> properties;
    private final List<RestNode> children;

    public RestNode( String name, String url, String parentUrl ) {
        super(name, url, parentUrl);
        properties = new ArrayList<RestProperty>();
        children = new ArrayList<RestNode>();
    }

    public RestNode addChild(RestNode child) {
        children.add(child);
        return this;
    }

    public RestNode addProperty(RestProperty property) {
        properties.add(property);
        return this;
    }

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject node = new JSONObject();
        node.put("self", url);
        node.put("up", parentUrl);
        //properties
        for (RestProperty restProperty : properties) {
            if (restProperty.isMultiValue()) {
                node.put(restProperty.name, restProperty.getValues());
            } else if (restProperty.getValue() != null) {
                node.put(restProperty.name, restProperty.getValue());
            }
        }
        //children
        if (!children.isEmpty()) {
            JSONObject children = new JSONObject();
            for (RestNode child : this.children) {
                children.put(child.name, child.toJSON());
            }
            node.put("children", children);
        }

        return node;
    }
}
