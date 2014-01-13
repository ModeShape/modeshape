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
