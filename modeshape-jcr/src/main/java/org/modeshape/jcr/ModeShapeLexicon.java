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
    public static final Name ID = new BasicName(Namespace.URI, "id");
    public static final Name LOCALNAME = new BasicName(Namespace.URI, "localName");

    /**
     * The name of the node which is used for running 1-time operations
     */
    public static final Name REPOSITORY = new BasicName(Namespace.URI, "repository");

    /**
     * Federation related items
     */
    public static final Name FEDERATION = new BasicName(Namespace.URI, "federation");
    public static final Name PROJECTION = new BasicName(Namespace.URI, "projection");

    public static final Name EXTERNAL_NODE_KEY = new BasicName(Namespace.URI, "externalNodeKey");
    public static final Name PROJECTED_NODE_KEY = new BasicName(Namespace.URI, "projectedNodeKey");
    public static final Name PROJECTION_ALIAS = new BasicName(Namespace.URI, "alias");

    /**
     * Index-related items
     */
    public static final Name INDEXES = new BasicName(Namespace.URI, "indexes");
    public static final Name INDEX = new BasicName(Namespace.URI, "index");
    public static final Name INDEX_COLUMN = new BasicName(Namespace.URI, "indexColumn");
    public static final Name INDEX_PROVIDER = new BasicName(Namespace.URI, "indexProvider");
    public static final Name KIND = new BasicName(Namespace.URI, "kind");
    public static final Name NODE_TYPE_NAME = new BasicName(Namespace.URI, "nodeTypeName");
    public static final Name PROPERTY_NAME = new BasicName(Namespace.URI, "propertyName");
    public static final Name COLUMN_TYPE_NAME = new BasicName(Namespace.URI, "columnTypeName");
    public static final Name WORKSPACES = new BasicName(Namespace.URI, "workspaces");
    public static final Name SYNCHRONOUS = new BasicName(Namespace.URI, "synchronous");

    /**
     * ACL related
     */
    public static final Name ACCESS_CONTROLLABLE = new BasicName(Namespace.URI, "accessControllable");
    public static final String ACCESS_CONTROLLABLE_STRING = ACCESS_CONTROLLABLE.getString();
    public static final Name ACCESS_LIST_NODE_TYPE = new BasicName(Namespace.URI, "Acl");
    public static final String ACCESS_LIST_NODE_TYPE_STRING = ACCESS_LIST_NODE_TYPE.getString();
    public static final Name ACCESS_LIST_NODE_NAME = new BasicName(Namespace.URI, "acl");
    public static final Name PERMISSION_PRINCIPAL_NAME = new BasicName("", "name");
    public static final Name PERMISSION_PRIVILEGES_NAME = new BasicName("", "privileges");
    public static final Name PERMISSION = new BasicName(Namespace.URI, "Permission");
    public static final Name ACL_COUNT = new BasicName(Namespace.URI, "aclCount");

    /**
     * Deprecated names because of originally invalid node types in modeshape_builtins.cnd. DO NOT USE outside of upgrade
     * functions & tests.
     */
    @Deprecated
    public static final Name LOCKED_KEY = new BasicName(Namespace.URI, "lockedKey");
    @Deprecated
    public static final Name SESSION_SCOPE = new BasicName(Namespace.URI, "sessionScope");
    @Deprecated
    public static final Name IS_DEEP = new BasicName(Namespace.URI, "isDeep");

    /**
     * Unordered collections
     */
     public static final Name UNORDERED_COLLECTION = new BasicName(Namespace.URI, "unorderedCollection"); 
     public static final Name TINY_UNORDERED_COLLECTION = new BasicName(Namespace.URI, "unorderedTinyCollection"); 
     public static final Name SMALL_UNORDERED_COLLECTION = new BasicName(Namespace.URI, "unorderedSmallCollection"); 
     public static final Name LARGE_UNORDERED_COLLECTION = new BasicName(Namespace.URI, "unorderedLargeCollection"); 
     public static final Name HUGE_UNORDERED_COLLECTION = new BasicName(Namespace.URI, "unorderedHugeCollection"); 
}
