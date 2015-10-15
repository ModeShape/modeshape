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
package org.modeshape.jcr.index.lucene;

import java.util.HashMap;
import java.util.Map;
import org.modeshape.jcr.value.PropertyType;

/**
 * Utility for the various Lucene tests.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public final class PropertiesTestUtil {
 
    static final String BINARY_PROP = "prop:binary";
    static final String BOOLEAN_PROP = "prop:boolean";
    static final String DATE_PROP = "prop:date";
    static final String DECIMAL_PROP = "prop:decimal";
    static final String DOUBLE_PROP = "prop:double";
    static final String LONG_PROP = "prop:long";
    static final String REF_PROP = "prop:ref";
    static final String WEAK_REF_PROP = "prop:weakref";
    static final String STRING_PROP = "prop:string";
    static final String SIMPLE_REF_PROP = "prop:simpleref";
    static final String URI_PROP = "prop:uri";
    static final String PATH_PROP = "prop:path";
    static final String NAME_PROP = "prop:name";
    
    static final Map<String, PropertyType> ALLOWED_PROPERTIES = new HashMap<String, PropertyType>() {{
        put(NAME_PROP, PropertyType.NAME);
        put(PATH_PROP, PropertyType.PATH);
        put(BINARY_PROP, PropertyType.BINARY);
        put(BOOLEAN_PROP, PropertyType.BOOLEAN);
        put(DATE_PROP, PropertyType.DATE);
        put(DECIMAL_PROP, PropertyType.DECIMAL);
        put(DOUBLE_PROP, PropertyType.DOUBLE);
        put(LONG_PROP, PropertyType.LONG);
        put(REF_PROP, PropertyType.REFERENCE);
        put(WEAK_REF_PROP, PropertyType.WEAKREFERENCE);
        put(STRING_PROP, PropertyType.STRING);
        put(SIMPLE_REF_PROP, PropertyType.SIMPLEREFERENCE);
        put(URI_PROP, PropertyType.URI);
    }};

    private PropertiesTestUtil() {
    }
}
