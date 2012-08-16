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
import java.util.Collections;
import java.util.List;

/**
 * @author Horia Chiorean
 */
public final class RestProperty extends RestItem {

    private final List<String> values;

    public RestProperty( String name,
                         String url,
                         String parentUrl,
                         List<String> values ) {
        super(name, url, parentUrl);
        this.values = values != null ? values : Collections.<String>emptyList();
    }

    List<String> getValues() {
        return values;
    }

    String getValue() {
        return !values.isEmpty() ? values.get(0) : null;
    }

    boolean isMultiValue() {
        return values.size() > 1;
    }

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject object = new JSONObject();
        if (isMultiValue()) {
            object.put("values", values);
        }
        else if (getValue() != null) {
            object.put(name, getValue());
        }
        object.put("self", url);
        object.put("up", parentUrl);
        return object;
    }
}
