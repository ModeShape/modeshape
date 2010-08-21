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
package org.modeshape.sequencer.teiid;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.modeshape.common.collection.Collections;
import org.modeshape.common.util.IoUtil;
import org.modeshape.common.util.StringUtil;

/**
 * 
 */
public class DefaultProperties {

    private static final Map<String, Object> EMPTY = java.util.Collections.emptyMap();

    protected static Set<String> KEYWORDS = Collections.unmodifiableSet("mixin",
                                                                        "mix",
                                                                        "m",
                                                                        "orderable",
                                                                        "ord",
                                                                        "o",
                                                                        "abstract",
                                                                        "abs",
                                                                        "a",
                                                                        "query",
                                                                        "q",
                                                                        "nq",
                                                                        "noquery",
                                                                        "primaryItem");

    protected static void loadDefaultsFromCnd( String resourceName,
                                               Map<String, Map<String, Object>> defaultValuesByType ) throws IOException {
        InputStream stream = DefaultProperties.class.getResourceAsStream(resourceName);
        try {
            String nodeTypeName = null;
            Map<String, Object> defaultValues = null;
            for (String line : StringUtil.splitLines(IoUtil.read(stream))) {
                line = line.trim();
                if (line.length() == 0) continue;
                if (line.charAt(0) == '[') {
                    int typeEnd = line.indexOf(']');
                    nodeTypeName = line.substring(1, typeEnd);
                    defaultValues = null;
                    int supertypesStart = line.indexOf('>', typeEnd);
                    if (supertypesStart == -1) continue;
                    List<String> supertypes = findSupertypes(line.substring(supertypesStart + 1));
                    for (String supertype : supertypes) {
                        Map<String, Object> supertypeDefaults = defaultValuesByType.get(supertype);
                        if (supertypeDefaults != null) {
                            defaultValues = new HashMap<String, Object>();
                            defaultValuesByType.put(nodeTypeName, defaultValues);
                            defaultValues.putAll(supertypeDefaults);
                        }
                    }
                    continue;
                }
                if (line.charAt(0) != '-') continue;
                // This is a property definition ...
                int defaultStart = line.indexOf('=');
                if (defaultStart == -1) continue;
                // This is a property definition with a default value ...
                defaultStart = line.indexOf('\'', defaultStart) + 1;
                int defaultEnd = line.indexOf('\'', defaultStart);
                String defaultValueLiteral = line.substring(defaultStart, defaultEnd);
                int typeStart = line.indexOf('(') + 1;
                int typeEnd = line.indexOf(')');
                String type = line.substring(typeStart, typeEnd);
                String name = line.substring(1, typeStart - 1).trim();

                Object defaultValue = defaultValueLiteral;
                if ("boolean".equalsIgnoreCase(type)) {
                    defaultValue = Boolean.parseBoolean(defaultValueLiteral);
                } else if ("long".equalsIgnoreCase(type)) {
                    defaultValue = Long.parseLong(defaultValueLiteral);
                } else if ("double".equalsIgnoreCase(type)) {
                    defaultValue = Double.parseDouble(defaultValueLiteral);
                }
                if (defaultValues == null) {
                    defaultValues = defaultValuesByType.get(nodeTypeName);
                    if (defaultValues == null) {
                        defaultValues = new HashMap<String, Object>();
                        defaultValuesByType.put(nodeTypeName, defaultValues);
                    }
                }
                defaultValues.put(name, defaultValue);
            }
        } finally {
            if (stream != null) stream.close();
        }
    }

    protected static List<String> findSupertypes( String nodeTypeDefnLine ) {
        int keywordStart = nodeTypeDefnLine.length();
        String[] words = nodeTypeDefnLine.split("[\\s?,]");
        List<String> supertypeNames = new ArrayList<String>();
        for (String word : words) {
            word = word.trim();
            if (word.length() == 0) continue;
            if (KEYWORDS.contains(word.toLowerCase())) break;
            supertypeNames.add(word);
        }
        return supertypeNames;
    }

    protected static DefaultProperties instance;

    public static DefaultProperties getDefaults() throws IOException {
        if (instance == null) {
            Map<String, Map<String, Object>> defaultValues = new HashMap<String, Map<String, Object>>();
            loadDefaultsFromCnd("/org/modeshape/sequencer/teiid/teiid.cnd", defaultValues);
            instance = new DefaultProperties(defaultValues);
        }
        return instance;
    }

    private final Map<String, Map<String, Object>> defaultValuesByType;

    private DefaultProperties( Map<String, Map<String, Object>> defaultValuesByType ) {
        this.defaultValuesByType = defaultValuesByType;
    }

    public Object getDefaultFor( String nodeTypeName,
                                 String property ) {
        Map<String, Object> defaults = defaultValuesByType.get(nodeTypeName);
        return defaults != null ? defaults.get(property) : null;
    }

    public Map<String, Object> getDefaultsFor( String nodeTypeName ) {
        Map<String, Object> defaults = defaultValuesByType.get(nodeTypeName);
        return defaults != null ? defaults : EMPTY;
    }
}
