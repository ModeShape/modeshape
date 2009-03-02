/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.jcr;

import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.basic.BasicName;

public class JcrNtLexicon extends org.jboss.dna.graph.JcrNtLexicon {

    public static final Name CHILD_NODE_DEFINITION = new BasicName(Namespace.URI, "childNodeDefinition");
    public static final Name FROZEN_NODE = new BasicName(Namespace.URI, "frozenNode");
    public static final Name HIERARCHY_NODE = new BasicName(Namespace.URI, "hierarchyNode");
    public static final Name LINKED_FILE = new BasicName(Namespace.URI, "linkedFile");
    public static final Name NODE_TYPE = new BasicName(Namespace.URI, "nodeType");
    public static final Name PROPERTY_DEFINITION = new BasicName(Namespace.URI, "propertyDefinition");
    public static final Name QUERY = new BasicName(Namespace.URI, "query");
    public static final Name VERSION = new BasicName(Namespace.URI, "version");
    public static final Name VERSIONED_CHILD = new BasicName(Namespace.URI, "versionedChild");
    public static final Name VERSION_HISTORY = new BasicName(Namespace.URI, "versionHistory");
    public static final Name VERSION_LABELS = new BasicName(Namespace.URI, "versionLabels");

}
