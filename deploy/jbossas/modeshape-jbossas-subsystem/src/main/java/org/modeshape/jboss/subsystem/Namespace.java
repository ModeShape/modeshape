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
package org.modeshape.jboss.subsystem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;

enum Namespace {
    // must be first
    UNKNOWN(0, 0, null),

    MODESHAPE_1_0(1, 0, new ModeShapeSubsystemXMLReader_1_0());

    /**
     * The current namespace version.
     */
    public static final Namespace CURRENT = MODESHAPE_1_0;

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

    public int getMajorVersion() {
        return this.major;
    }

    public int getMinorVersion() {
        return this.minor;
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
