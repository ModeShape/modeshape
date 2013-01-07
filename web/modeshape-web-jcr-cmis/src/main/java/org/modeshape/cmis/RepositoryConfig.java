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
    private static final String MOUNT_PATH_CONFIG = "mount-path";

    public static Map<String, Map<String, String>> load(Map<String, String> params) {
        //create map for results
        Map<String, Map<String, String>> set  = new HashMap();

        //extract parameter's keys
        Set<String> keys = params.keySet();
        for (String key : keys) {
            //search for repositories parameters
            if (key.startsWith(REPO_PARAMETER_PREFIX)) {
                //found repository parameter, cut prefix
                String fqn = key.substring(REPO_PARAMETER_PREFIX.length());

                //cut repository ID from the begining of parameter
                String repositoryId = fqn.substring(0, fqn.indexOf("."));

                //get or create map for this repository Id
                Map<String, String> map = set.get(repositoryId);
                if (map == null) {
                    map = new HashMap();
                    set.put(repositoryId, map);
                }

                //extract clean parameter name and value
                String name = fqn.substring(fqn.indexOf(".") + 1, fqn.length());
                String value = params.get(key);

                //store parameter
                map.put(name, replaceSystemProperties(value));
            }
        }

        return set;
    }

    private static String replaceSystemProperties(String s) {
        if (s == null) {
            return null;
        }

        StringBuilder result = new StringBuilder();
        StringBuilder property = null;
        boolean inProperty = false;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (inProperty) {
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
