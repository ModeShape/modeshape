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

import net.jcip.annotations.Immutable;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.basic.BasicName;

/**
 * Lexicon of names from the standard JCR "<code>http://www.jcp.org/jcr/1.0</code>" namespace.
 */
@Immutable
public class JcrLexicon extends org.jboss.dna.graph.JcrLexicon {

    public static final Name BASE_VERSION = new BasicName(Namespace.URI, "baseVersion");
    public static final Name CONTENT = new BasicName(Namespace.URI, "content");
    public static final Name CREATED = new BasicName(Namespace.URI, "created");
    public static final Name DATA = new BasicName(Namespace.URI, "data");
    public static final Name ENCODING = new BasicName(Namespace.URI, "encoding");
    public static final Name FROZEN_MIXIN_TYPES = new BasicName(Namespace.URI, "frozenMixinTypes");
    public static final Name FROZEN_NODE = new BasicName(Namespace.URI, "frozenNode");
    public static final Name FROZEN_PRIMARY_TYPE = new BasicName(Namespace.URI, "frozenPrimaryType");
    public static final Name FROZEN_UUID = new BasicName(Namespace.URI, "frozenUuid");
    public static final Name IS_CHECKED_OUT = new BasicName(Namespace.URI, "isCheckedOut");
    public static final Name LANGUAGE = new BasicName(Namespace.URI, "language");
    public static final Name LAST_MODIFIED = new BasicName(Namespace.URI, "lastModified");
    public static final Name LOCK_IS_DEEP = new BasicName(Namespace.URI, "lockIsDeep");
    public static final Name LOCK_OWNER = new BasicName(Namespace.URI, "lockOwner");
    public static final Name MERGE_FAILED = new BasicName(Namespace.URI, "mergeFailed");
    public static final Name NODE_TYPES = new BasicName(Namespace.URI, "nodeTypes");
    public static final Name PREDECESSORS = new BasicName(Namespace.URI, "predecessors");
    public static final Name ROOT = new BasicName(Namespace.URI, "root");
    public static final Name ROOT_VERSION = new BasicName(Namespace.URI, "rootVersion");
    public static final Name STATEMENT = new BasicName(Namespace.URI, "statement");
    public static final Name SUCCESSORS = new BasicName(Namespace.URI, "successors");
    public static final Name SYSTEM = new BasicName(Namespace.URI, "system");
    public static final Name VERSIONABLE_UUID = new BasicName(Namespace.URI, "versionableUuid");
    public static final Name VERSION_HISTORY = new BasicName(Namespace.URI, "versionHistory");
    public static final Name VERSION_LABELS = new BasicName(Namespace.URI, "versionLabels");
    public static final Name XMLTEXT = new BasicName(Namespace.URI, "xmltext");
    public static final Name XMLCHARACTERS = new BasicName(Namespace.URI, "xmlcharacters");

}
