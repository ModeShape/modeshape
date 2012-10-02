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
 * A REST representation of a {@link javax.jcr.Node}
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public final class RestNode extends RestItem {

    private final List<RestProperty> properties;
    private final List<RestNode> children;

    /**
     * Creates a new rest node
     *
     * @param name a {@code non-null} string, representing the name
     * @param url a {@code non-null} string, representing the url to this node
     * @param parentUrl a {@code non-null} string, representing the url to this node's parent
     */
    public RestNode( String name,
                     String url,
                     String parentUrl ) {
        super(name, url, parentUrl);
        properties = new ArrayList<RestProperty>();
        children = new ArrayList<RestNode>();
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
     * Adds a new property to this node.
     *
     * @param property a {@code non-null} {@link RestProperty}
     * @return this rest node.
     */
    public RestNode addProperty( RestProperty property ) {
        properties.add(property);
        return this;
    }

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject node = new JSONObject();
        node.put("self", url);
        node.put("up", parentUrl);

        convertPropertiesToJSON(node);
        convertChildrenToJSON(node);

        return node;
    }

    private void convertChildrenToJSON( JSONObject node ) throws JSONException {
        //children
        if (!children.isEmpty()) {
            JSONObject children = new JSONObject();
            for (RestNode child : this.children) {
                children.put(child.name, child.toJSON());
            }
            node.put("children", children);
        }
    }

    private void convertPropertiesToJSON( JSONObject node ) throws JSONException {
        //properties
        for (RestProperty restProperty : properties) {
            if (restProperty.isMultiValue()) {
                node.put(restProperty.name, restProperty.getValues());
            } else if (restProperty.getValue() != null) {
                node.put(restProperty.name, restProperty.getValue());
            }
        }
    }
}
