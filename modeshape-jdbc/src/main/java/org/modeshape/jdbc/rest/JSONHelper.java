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
package org.modeshape.jdbc.rest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * Helper class used by the client to parse {@link org.codehaus.jettison.json.JSONObject}
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public final class JSONHelper {

    private JSONHelper() {
    }

    protected static List<String> valuesFrom( JSONObject json,
                                           String name ) {
        if (!json.has(name)) {
            // Just an empty collection ...
            return Collections.emptyList();
        }
        Object prop = null;
        try {
            prop = json.get(name);
            if (prop instanceof JSONArray) {
                // There are multiple values ...
                JSONArray array = (JSONArray)prop;
                int length = array.length();
                if (length == 0) {
                    return Collections.emptyList();
                }
                List<String> result = new ArrayList<>(length);
                for (int i = 0; i < length; i++) {
                    String value = array.getString(i);
                    result.add(value);
                }
                return result;
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        // Just a single value ...
        return Collections.singletonList(prop.toString());
    }

    protected static boolean valueFrom( JSONObject object,
                                     String name,
                                     boolean defaultValue ) {
        if (!object.has(name)) {
            return defaultValue;
        }
        try {
            return object.getBoolean(name);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    protected static String valueFrom( JSONObject object,
                                    String name ) {
        return valueFrom(object, name, null);
    }

    protected static String valueFrom( JSONObject object,
                                    String name,
                                    String defaultValue ) {
        if (!object.has(name)) {
            return defaultValue;
        }
        try {
            return object.getString(name);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
