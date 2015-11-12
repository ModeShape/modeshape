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
package org.modeshape.jboss.subsystem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;

enum Namespace {
    // must be first
    UNKNOWN(0, 0, null),

    MODESHAPE_3_0(ModeShapeExtension.MANAGEMENT_API_MAJOR_VERSION, 
                  ModeShapeExtension.MANAGEMENT_API_MINOR_VERSION, 
                  new ModeShapeSubsystemXMLReader_3_0());

    /**
     * The current namespace version.
     */
    public static final Namespace CURRENT = MODESHAPE_3_0;

    private static final String BASE_URN = "urn:jboss:domain:modeshape:";

    private final int major;
    private final int minor;
    private final XMLElementReader<List<ModelNode>> reader;

    Namespace( int major,
               int minor,
               XMLElementReader<List<ModelNode>> reader ) {
        this.major = major;
        this.minor = minor;
        this.reader = reader;
    }


    /**
     * Get the URI of this namespace.
     * 
     * @return the URI
     */
    public String getUri() {
        return BASE_URN + major + "." + minor;
    }

    public XMLElementReader<List<ModelNode>> getXMLReader() {
        return this.reader;
    }

    private static final Map<String, Namespace> namespaces;

    static {
        final Map<String, Namespace> map = new HashMap<String, Namespace>();
        for (Namespace namespace : values()) {
            final String name = namespace.getUri();
            if (name != null) map.put(name, namespace);
        }
        namespaces = map;
    }

    /**
     * Converts the specified uri to a {@link Namespace}.
     * 
     * @param uri a namespace uri
     * @return the matching namespace enum.
     */
    public static Namespace forUri( String uri ) {
        final Namespace element = namespaces.get(uri);
        return element == null ? UNKNOWN : element;
    }
}
