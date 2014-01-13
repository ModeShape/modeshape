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
package org.modeshape.cmis;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Splits common configuration map into set of repository specific parameters.
 * 
 * @author kulikov
 */
public class RepositoryConfig {
    private static final String REPO_PARAMETER_PREFIX = "jcr.";

    public static Map<String, Map<String, String>> load( Map<String, String> params ) {
        // create map for results
        Map<String, Map<String, String>> set = new HashMap<String, Map<String, String>>();

        // extract parameter's keys
        Set<String> keys = params.keySet();
        for (String key : keys) {
            // search for repositories parameters
            if (key.startsWith(REPO_PARAMETER_PREFIX)) {
                // found repository parameter, cut prefix
                String fqn = key.substring(REPO_PARAMETER_PREFIX.length());

                // cut repository ID from the begining of parameter
                String repositoryId = fqn.substring(0, fqn.indexOf("."));

                // get or create map for this repository Id
                Map<String, String> map = set.get(repositoryId);
                if (map == null) {
                    map = new HashMap<String, String>();
                    set.put(repositoryId, map);
                }

                // extract clean parameter name and value
                String name = fqn.substring(fqn.indexOf(".") + 1, fqn.length());
                String value = params.get(key);

                // store parameter
                map.put(name, replaceSystemProperties(value));
            }
        }

        return set;
    }

    private static String replaceSystemProperties( String s ) {
        if (s == null) {
            return null;
        }

        StringBuilder result = new StringBuilder();
        StringBuilder property = null;
        boolean inProperty = false;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (inProperty) {
                assert property != null;
                if (c == '}') {
                    String value = System.getProperty(property.toString());
                    if (value != null) {
                        result.append(value);
                    }
                    inProperty = false;
                } else {
                    property.append(c);
                }
            } else {
                if (c == '{') {
                    property = new StringBuilder();
                    inProperty = true;
                } else {
                    result.append(c);
                }
            }
        }

        return result.toString();
    }
}
