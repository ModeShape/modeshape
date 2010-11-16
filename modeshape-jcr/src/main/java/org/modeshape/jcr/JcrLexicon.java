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

import net.jcip.annotations.Immutable;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.basic.BasicName;

/**
 * Lexicon of names from the standard JCR "<code>http://www.jcp.org/jcr/1.0</code>" namespace.
 */
@Immutable
public class JcrLexicon extends org.modeshape.graph.JcrLexicon {

    public static final Name ACTIVITY = new BasicName(Namespace.URI, "activity");
    public static final Name BASE_VERSION = new BasicName(Namespace.URI, "baseVersion");
    public static final Name CHILD_VERSION_HISTORY = new BasicName(Namespace.URI, "childVersionHistory");
    public static final Name CONFIGURATION = new BasicName(Namespace.URI, "configuration");
    public static final Name CONTENT = new BasicName(Namespace.URI, "content");
    public static final Name COPIED_FROM = new BasicName(Namespace.URI, "copiedFrom");
    public static final Name CURRENT_LIFECYCLE_STATE = new BasicName(Namespace.URI, "currentLifecycleState");
    public static final Name DATA = new BasicName(Namespace.URI, "data");
    public static final Name ENCODING = new BasicName(Namespace.URI, "encoding");
    public static final Name ETAG = new BasicName(Namespace.URI, "etag");
    public static final Name FROZEN_MIXIN_TYPES = new BasicName(Namespace.URI, "frozenMixinTypes");
    public static final Name FROZEN_NODE = new BasicName(Namespace.URI, "frozenNode");
    public static final Name FROZEN_PRIMARY_TYPE = new BasicName(Namespace.URI, "frozenPrimaryType");
    public static final Name FROZEN_UUID = new BasicName(Namespace.URI, "frozenUuid");
    public static final Name HOLD = new BasicName(Namespace.URI, "hold");
    public static final Name IS_CHECKED_OUT = new BasicName(Namespace.URI, "isCheckedOut");
    public static final Name IS_DEEP = new BasicName(Namespace.URI, "isDeep");
    public static final Name LANGUAGE = new BasicName(Namespace.URI, "language");
    public static final Name LIFECYCLE_POLICY = new BasicName(Namespace.URI, "lifecyclePolicy");
    public static final Name LOCK_IS_DEEP = new BasicName(Namespace.URI, "lockIsDeep");
    public static final Name LOCK_OWNER = new BasicName(Namespace.URI, "lockOwner");
    public static final Name MERGE_FAILED = new BasicName(Namespace.URI, "mergeFailed");
    public static final Name NODE_TYPES = new BasicName(Namespace.URI, "nodeTypes");
    /** The "jcr:path" pseudo-column used in queries */
    public static final Name PATH = new BasicName(Namespace.URI, "path");
    public static final Name PREDECESSORS = new BasicName(Namespace.URI, "predecessors");
    public static final Name RETENTION_POLICY = new BasicName(Namespace.URI, "retentionPolicy");
    public static final Name ROOT = new BasicName(Namespace.URI, "root");
    public static final Name ROOT_VERSION = new BasicName(Namespace.URI, "rootVersion");
    /** The "jcr:score" pseudo-column used in queries */
    public static final Name SCORE = new BasicName(Namespace.URI, "score");
    public static final Name STATEMENT = new BasicName(Namespace.URI, "statement");
    public static final Name SUCCESSORS = new BasicName(Namespace.URI, "successors");
    public static final Name SYSTEM = new BasicName(Namespace.URI, "system");
    public static final Name VERSIONABLE_UUID = new BasicName(Namespace.URI, "versionableUuid");
    public static final Name VERSION_HISTORY = new BasicName(Namespace.URI, "versionHistory");
    public static final Name VERSION_LABELS = new BasicName(Namespace.URI, "versionLabels");
    public static final Name VERSION_STORAGE = new BasicName(Namespace.URI, "versionStorage");
    public static final Name XMLTEXT = new BasicName(Namespace.URI, "xmltext");

}
