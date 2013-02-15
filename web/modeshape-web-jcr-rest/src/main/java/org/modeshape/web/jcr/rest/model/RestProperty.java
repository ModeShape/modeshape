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

import java.util.Collections;
import java.util.List;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * A REST representation of the {@link javax.jcr.Property}
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public final class RestProperty extends RestItem {

    private final List<String> values;
    private final boolean multiValued;

    /**
     * Creates a new rest property instance.
     * 
     * @param name a {@code non-null} string, the name of the property
     * @param url a {@code non-null} string, the absolute url to this property
     * @param parentUrl a {@code non-null} string, the absolute url to this property's parent
     * @param values a list of possible values for the property, may be {@code null}
     * @param multiValued true if this property is a multi-valued property
     */
    public RestProperty( String name,
                         String url,
                         String parentUrl,
                         List<String> values,
                         boolean multiValued ) {
        super(name, url, parentUrl);
        this.values = values != null ? values : Collections.<String>emptyList();
        this.multiValued = multiValued;
    }

    List<String> getValues() {
        return values;
    }

    String getValue() {
        return !values.isEmpty() ? values.get(0) : null;
    }

    boolean isMultiValue() {
        return multiValued;
    }

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject object = new JSONObject();
        if (isMultiValue()) {
            object.put("values", values);
        } else if (getValue() != null) {
            object.put(name, getValue());
        }
        object.put("self", url);
        object.put("up", parentUrl);
        return object;
    }
}
