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

/**
 * @author Randall Hauch
 */
class JcrLexicon extends org.jboss.dna.graph.JcrLexicon {

    public static final Name AUTO_CREATED = new BasicName(Namespace.URI, "autoCreated");
    public static final Name BASE_VERSION = new BasicName(Namespace.URI, "baseVersion");
    public static final Name CHILD_NODE_DEFINITION = new BasicName(Namespace.URI, "childNodeDefinition");
    public static final Name CONTENT = new BasicName(Namespace.URI, "content");
    public static final Name CREATED = new BasicName(Namespace.URI, "created");
    public static final Name DATA = new BasicName(Namespace.URI, "data");
    public static final Name DEFAULT_PRIMARY_TYPE = new BasicName(Namespace.URI, "defaultPrimaryType");
    public static final Name DEFAULT_VALUES = new BasicName(Namespace.URI, "defaultValues");
    public static final Name ENCODING = new BasicName(Namespace.URI, "encoding");
    public static final Name FROZEN_MIXIN_TYPES = new BasicName(Namespace.URI, "frozenMixinTypes");
    public static final Name FROZEN_NODE = new BasicName(Namespace.URI, "frozenNode");
    public static final Name FROZEN_PRIMARY_TYPE = new BasicName(Namespace.URI, "frozenPrimaryType");
    public static final Name FROZEN_UUID = new BasicName(Namespace.URI, "frozenUuid");
    public static final Name HAS_ORDERABLE_CHILD_NODES = new BasicName(Namespace.URI, "hasOrderableChildNodes");
    public static final Name IS_CHECKED_OUT = new BasicName(Namespace.URI, "isCheckedOut");
    public static final Name IS_MIXIN = new BasicName(Namespace.URI, "isMixin");
    public static final Name LANGUAGE = new BasicName(Namespace.URI, "language");
    public static final Name LAST_MODIFIED = new BasicName(Namespace.URI, "lastModified");
    public static final Name LOCK_IS_DEEP = new BasicName(Namespace.URI, "lockIsDeep");
    public static final Name LOCK_OWNER = new BasicName(Namespace.URI, "lockOwner");
    public static final Name MANDATORY = new BasicName(Namespace.URI, "mandatory");
    public static final Name MERGE_FAILED = new BasicName(Namespace.URI, "mergeFailed");
    public static final Name MULTIPLE = new BasicName(Namespace.URI, "multiple");
    public static final Name NAME = new BasicName(Namespace.URI, "name");
    public static final Name NODE_TYPE_NAME = new BasicName(Namespace.URI, "nodeTypeName");
    public static final Name ON_PARENT_VERSION = new BasicName(Namespace.URI, "onParentVersion");
    public static final Name PREDECESSORS = new BasicName(Namespace.URI, "predecessors");
    public static final Name PRIMARY_ITEM_NAME = new BasicName(Namespace.URI, "primaryItemName");
    public static final Name PROPERTY_DEFINITION = new BasicName(Namespace.URI, "propertyDefinition");
    public static final Name PROTECTED = new BasicName(Namespace.URI, "protected");
    public static final Name REQUIRED_PRIMARY_TYPES = new BasicName(Namespace.URI, "requiredPrimaryTypes");
    public static final Name REQUIRED_TYPE = new BasicName(Namespace.URI, "requiredType");
    public static final Name ROOT_VERSION = new BasicName(Namespace.URI, "rootVersion");
    public static final Name SAME_NAME_SIBLINGS = new BasicName(Namespace.URI, "sameNameSiblings");
    public static final Name STATEMENT = new BasicName(Namespace.URI, "statement");
    public static final Name SUCCESSORS = new BasicName(Namespace.URI, "successors");
    public static final Name SUPERTYPES = new BasicName(Namespace.URI, "supertypes");
    public static final Name SYSTEM = new BasicName(Namespace.URI, "system");
    public static final Name VALUE_CONSTRAINTS = new BasicName(Namespace.URI, "valueConstraints");
    public static final Name VERSIONABLE_UUID = new BasicName(Namespace.URI, "versionableUuid");
    public static final Name VERSION_HISTORY = new BasicName(Namespace.URI, "versionHistory");
    public static final Name VERSION_LABELS = new BasicName(Namespace.URI, "versionLabels");

}
