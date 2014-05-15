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

import javax.jcr.Node;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.basic.BasicName;

/**
 * A lexicon of names used within ModeShape.
 */
@Immutable
public class ModeShapeLexicon {

    public static class Namespace {
        public static final String URI = "http://www.modeshape.org/1.0";
        public static final String PREFIX = "mode";
    }

    public static final Name NAMESPACES = new BasicName(Namespace.URI, "namespaces");
    public static final Name ROOT = new BasicName(Namespace.URI, "root");
    public static final Name URI = new BasicName(Namespace.URI, "uri");
    public static final Name GENERATED = new BasicName(Namespace.URI, "generated");
    public static final Name NAMESPACE = new BasicName(Namespace.URI, "namespace");
    public static final Name NODE_TYPES = new BasicName(Namespace.URI, "nodeTypes");
    public static final Name SYSTEM = new BasicName(Namespace.URI, "system");
    public static final Name VERSION_STORAGE = new BasicName(Namespace.URI, "versionStorage");
    public static final Name VERSION_HISTORY_FOLDER = new BasicName(Namespace.URI, "versionHistoryFolder");
    public static final Name WORKSPACE = new BasicName(Namespace.URI, "workspace");

    public static final Name EXPIRATION_DATE = new BasicName(Namespace.URI, "expirationDate");
    public static final Name IS_HELD_BY_SESSION = new BasicName(Namespace.URI, "isHeldBySession");
    public static final Name IS_SESSION_SCOPED = new BasicName(Namespace.URI, "isSessionScoped");
    public static final Name LOCK = new BasicName(Namespace.URI, "lock");
    public static final Name LOCK_TOKEN = new BasicName(Namespace.URI, "lockToken");
    public static final Name LOCKING_SESSION = new BasicName(Namespace.URI, "lockingSession");
    public static final Name LOCKS = new BasicName(Namespace.URI, "locks");
    /**
     * The name of the "mode:share" node type, used as the primary type on nodes that are proxies for the original node. The
     * "mode:share" node type defines a single "{@link #SHARED_UUID mode:shared}" REFERENCE property pointing to the original
     * node.
     * <p>
     * With the way that ModeShape's JCR layer is implemented, JCR clients should never see {@link Node}s of this type. Instead,
     * the JCR layer transparently creates a JcrSharedNode that will mirror the original.
     * </p>
     */
    public static final Name SHARE = new BasicName(Namespace.URI, "share");
    /**
     * The REFERENCE property on the "mode:share" node type. This property references the original node for which this node is a
     * proxy.
     */
    public static final Name SHARED_UUID = new BasicName(Namespace.URI, "sharedUuid");

    public static final Name DEPTH = new BasicName(Namespace.URI, "depth");
    public static final Name LOCALNAME = new BasicName(Namespace.URI, "localName");

    /**
     * The name of the node which is used for running 1-time operations
     */
    public static final Name REPOSITORY= new BasicName(Namespace.URI, "repository");

    /**
     * Federation related items
     */
    public static final Name FEDERATION = new BasicName(Namespace.URI, "federation");
    public static final Name PROJECTION = new BasicName(Namespace.URI, "projection");

    public static final Name EXTERNAL_NODE_KEY = new BasicName(Namespace.URI, "externalNodeKey");
    public static final Name PROJECTED_NODE_KEY = new BasicName(Namespace.URI, "projectedNodeKey");
    public static final Name PROJECTION_ALIAS = new BasicName(Namespace.URI, "alias");

    /**
     * ACL related
     */
    public static final Name ACCESS_CONTROLLABLE = new BasicName(Namespace.URI, "accessControllable");
    public static final Name ACL = new BasicName(Namespace.URI, "Acl");
    public static final Name PERMISSION = new BasicName(Namespace.URI, "Permission");
    public static final Name ACL_COUNT = new BasicName(Namespace.URI, "aclCount");

    /**
     * Deprecated names because of originally invalid node types in modeshape_builtins.cnd.
     * DO NOT USE outside of upgrade functions & tests.
     */
    @Deprecated
    public static final Name LOCKED_KEY =  new BasicName(Namespace.URI, "lockedKey");
    @Deprecated
    public static final Name SESSION_SCOPE =  new BasicName(Namespace.URI, "sessionScope");
    @Deprecated
    public static final Name IS_DEEP =  new BasicName(Namespace.URI, "isDeep");

}
