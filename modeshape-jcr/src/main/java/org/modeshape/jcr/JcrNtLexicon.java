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
package org.modeshape.jcr;

import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.basic.BasicName;

/**
 * Lexicon of names from the standard JCR "<code>http://www.jcp.org/jcr/nt/1.0</code>" namespace.
 */
public class JcrNtLexicon extends org.modeshape.graph.JcrNtLexicon {

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
