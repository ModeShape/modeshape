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
package org.modeshape.jcr;

import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.basic.BasicName;

/**
 * Lexicon of names from the standard JCR "<code>http://www.jcp.org/jcr/nt/1.0</code>" namespace.
 */
public class JcrNtLexicon {

    public static class Namespace {
        public static final String URI = "http://www.jcp.org/jcr/nt/1.0";
        public static final String PREFIX = "nt";
    }

    public static final Name UNSTRUCTURED = new BasicName(Namespace.URI, "unstructured");
    public static final Name FILE = new BasicName(Namespace.URI, "file");
    public static final Name FOLDER = new BasicName(Namespace.URI, "folder");
    public static final Name RESOURCE = new BasicName(Namespace.URI, "resource");
    public static final Name BASE = new BasicName(Namespace.URI, "base");

    // Names used in the node type definitions ...
    public static final Name NODE_TYPE = new BasicName(Namespace.URI, "nodeType");
    public static final Name CHILD_NODE_DEFINITION = new BasicName(Namespace.URI, "childNodeDefinition");
    public static final Name PROPERTY_DEFINITION = new BasicName(Namespace.URI, "propertyDefinition");
    public static final Name FROZEN_NODE = new BasicName(Namespace.URI, "frozenNode");
    public static final Name HIERARCHY_NODE = new BasicName(Namespace.URI, "hierarchyNode");
    public static final Name LINKED_FILE = new BasicName(Namespace.URI, "linkedFile");
    public static final Name QUERY = new BasicName(Namespace.URI, "query");
    public static final Name VERSION = new BasicName(Namespace.URI, "version");
    public static final Name VERSIONED_CHILD = new BasicName(Namespace.URI, "versionedChild");
    public static final Name VERSION_HISTORY = new BasicName(Namespace.URI, "versionHistory");
    public static final Name VERSION_LABELS = new BasicName(Namespace.URI, "versionLabels");
    /**
     * The "nt:share" node type name only appears in a serialized XML document exported from a repository, and <i>never</i>
     * appears in the actual repository content. Therefore, there is no actual "nt:share" node type. For details, see Sections
     * 14.7 and 14.8 of the JCR 2.0 specification.
     */
    public static final Name SHARE = new BasicName(Namespace.URI, "share");

}
