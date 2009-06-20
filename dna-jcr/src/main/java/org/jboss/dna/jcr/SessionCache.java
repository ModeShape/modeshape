/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in JBoss DNA is licensed
 * to you under the terms of the GNU Lesser General Public License as
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

import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.UUID;
import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.common.i18n.I18n;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.Results;
import org.jboss.dna.graph.connector.RepositorySourceException;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.NameFactory;
import org.jboss.dna.graph.property.NamespaceRegistry;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.property.PropertyFactory;
import org.jboss.dna.graph.property.ValueFactories;
import org.jboss.dna.graph.property.ValueFactory;
import org.jboss.dna.graph.property.ValueFormatException;
import org.jboss.dna.graph.request.BatchRequestBuilder;
import org.jboss.dna.graph.request.ChangeRequest;
import org.jboss.dna.graph.request.CreateNodeRequest;
import org.jboss.dna.graph.request.InvalidWorkspaceException;
import org.jboss.dna.graph.request.Request;
import org.jboss.dna.jcr.cache.ChangedNodeInfo;
import org.jboss.dna.jcr.cache.ChildNode;
import org.jboss.dna.jcr.cache.Children;
import org.jboss.dna.jcr.cache.EmptyChildren;
import org.jboss.dna.jcr.cache.ImmutableChildren;
import org.jboss.dna.jcr.cache.ImmutableNodeInfo;
import org.jboss.dna.jcr.cache.NewNodeInfo;
import org.jboss.dna.jcr.cache.NodeInfo;
import org.jboss.dna.jcr.cache.PropertyInfo;
import com.google.common.base.ReferenceType;
import com.google.common.collect.ReferenceMap;

/**
 * The class that manages the session's information that has been locally-cached after reading from the underlying {@link Graph
 * repository} or modified by the session but not yet saved or commited to the repository.
 * <p>
 * The cached information is broken into several different categories that are each described below.
 * </p>
 * <h3>JCR API objects</h3>
 * <p>
 * Clients using the DNA JCR implementation obtain a {@link JcrSession JCR Session} (which generally owns this cache instance) as
 * well as the JCR {@link JcrNode Node} and {@link AbstractJcrProperty Property} instances. This cache ensures that the same JCR
 * Node or Property objects are always returned for the same item in the repository, ensuring that the "==" operator always holds
 * true for the same item. However, as soon as all (client) references to these objects are garbage collected, this class is free
 * to also release those objects and, when needed, recreate new implementation objects.
 * </p>
 * <p>
 * This approach helps reduce memory utilization since any unused items are available for garbage collection, but it also
 * guarantees that once a client maintains a reference to an item, the same Java object will always be used for any references to
 * that item.
 * </p>
 * <h3>Cached nodes</h3>
 * <p>
 * The session cache is also responsible for maintaining a local cache of node information retrieved from the underlying
 * repository, reducing the need to request information any more than necessary. This information includes that obtained directly
 * from the repository store, including node properties, children, and references to the parent. It also includes computed
 * information, such as the NodeDefinition for a node, the name of the primary type and mixin types, and the original
 * {@link Location} of the node in the repository.
 * </p>
 * <h3>Transient changes</h3>
 * <p>
 * Any time content is changed in the session, those changes are held within the session until they are saved either by
 * {@link Session#save() saving the session} or {@link Item#save() saving an individual item} (which includes any content below
 * that item). This cache maintains all these transient changes, and when requested will send the change requests down the
 * repository. At any point, these transient changes may be rolled back (or "released"), again either for the
 * {@link Session#refresh(boolean) whole session} or for {@link Item#refresh(boolean) individual items}.
 * </p>
 */
@ThreadSafe
class SessionCache {

    /**
     * Hidden flag that controls whether properties that appear on DNA nodes but not allowed by the node type or mixins should be
     * included anyway. This is currently {@value} .
     */
    private static final boolean INCLUDE_PROPERTIES_NOT_ALLOWED_BY_NODE_TYPE_OR_MIXINS = true;

    private static final Set<Name> EMPTY_NAMES = Collections.emptySet();

    private final JcrSession session;
    private final String workspaceName;
    protected final ExecutionContext context;
    protected final ValueFactories factories;
    protected final PathFactory pathFactory;
    protected final NameFactory nameFactory;
    protected final ValueFactory<String> stringFactory;
    protected final NamespaceRegistry namespaces;
    protected final PropertyFactory propertyFactory;
    private final Graph store;
    private final Name defaultPrimaryTypeName;
    private final Property defaultPrimaryTypeProperty;
    protected final Path rootPath;
    protected final Name residualName;

    private UUID root;
    private final ReferenceMap<UUID, AbstractJcrNode> jcrNodes;
    private final ReferenceMap<PropertyId, AbstractJcrProperty> jcrProperties;

    protected final HashMap<UUID, ImmutableNodeInfo> cachedNodes;
    protected final HashMap<UUID, ChangedNodeInfo> changedNodes;
    protected final HashMap<UUID, NodeInfo> deletedNodes;
    protected final Map<UUID, Path> pathCache;

    private LinkedList<Request> requests;
    private BatchRequestBuilder requestBuilder;
    protected Graph.Batch operations;

    public SessionCache( JcrSession session,
                         String workspaceName,
                         ExecutionContext context,
                         JcrNodeTypeManager nodeTypes,
                         Graph store ) {
        assert session != null;
        assert workspaceName != null;
        assert context != null;
        assert store != null;
        this.session = session;
        this.workspaceName = workspaceName;
        this.store = store;
        this.context = context;
        this.factories = context.getValueFactories();
        this.pathFactory = this.factories.getPathFactory();
        this.nameFactory = this.factories.getNameFactory();
        this.stringFactory = context.getValueFactories().getStringFactory();
        this.namespaces = context.getNamespaceRegistry();
        this.propertyFactory = context.getPropertyFactory();
        this.defaultPrimaryTypeName = JcrNtLexicon.UNSTRUCTURED;
        this.defaultPrimaryTypeProperty = propertyFactory.create(JcrLexicon.PRIMARY_TYPE, this.defaultPrimaryTypeName);
        this.rootPath = pathFactory.createRootPath();
        this.residualName = nameFactory.create(JcrNodeType.RESIDUAL_ITEM_NAME);

        this.jcrNodes = new ReferenceMap<UUID, AbstractJcrNode>(ReferenceType.STRONG, ReferenceType.SOFT);
        this.jcrProperties = new ReferenceMap<PropertyId, AbstractJcrProperty>(ReferenceType.STRONG, ReferenceType.SOFT);

        this.cachedNodes = new HashMap<UUID, ImmutableNodeInfo>();
        this.changedNodes = new HashMap<UUID, ChangedNodeInfo>();
        this.deletedNodes = new HashMap<UUID, NodeInfo>();
        this.pathCache = new HashMap<UUID, Path>();

        // Create the batch operations ...
        this.requests = new LinkedList<Request>();
        this.requestBuilder = new BatchRequestBuilder(this.requests);
        this.operations = this.store.batch(this.requestBuilder);
    }

    JcrSession session() {
        return session;
    }

    String workspaceName() {
        return workspaceName;
    }

    String sourceName() {
        return session.sourceName();
    }

    ExecutionContext context() {
        return context;
    }

    ValueFactories factories() {
        return factories;
    }

    PathFactory pathFactory() {
        return pathFactory;
    }

    NameFactory nameFactory() {
        return nameFactory;
    }

    JcrNodeTypeManager nodeTypes() {
        return session.nodeTypeManager();
    }

    /**
     * Checks whether the current session has the appropriate permissions to perform the given action.
     * 
     * @param node the node on which the action will be performed
     * @param action the name of the action to perform, should be "add_node", "remove", or "set_property"
     * @throws AccessDeniedException if the current session does not have the requisite privileges to perform this task
     * @throws RepositoryException if any other error occurs
     */
    protected void checkPermission( NodeInfo node,
                                    String action ) throws AccessDeniedException, RepositoryException {
        try {
            this.session.checkPermission(SessionCache.this.getPathFor(node), action);
        } catch (AccessControlException ace) {
            throw new AccessDeniedException(ace);
        }
    }

    /**
     * Returns whether the session cache has any pending changes that need to be executed.
     * 
     * @return true if there are pending changes, or false if there is currently no changes
     */
    boolean hasPendingChanges() {
        return operations.isExecuteRequired();
    }

    /**
     * Refreshes (removes the cached state) for all cached nodes.
     * <p>
     * If {@code keepChanges == true}, modified nodes will not have their state refreshed.
     * </p>
     * 
     * @param keepChanges indicates whether changed nodes should be kept or refreshed from the repository.
     */
    public void refresh( boolean keepChanges ) {
        if (keepChanges) {
            // Keep the pending operations
            Set<UUID> retainedSet = new HashSet<UUID>(this.changedNodes.size() + this.deletedNodes.size());
            retainedSet.addAll(this.changedNodes.keySet());
            retainedSet.addAll(this.deletedNodes.keySet());

            // Removed any cached nodes not already in the changed or deleted set
            this.cachedNodes.keySet().retainAll(retainedSet);
        } else {
            // Throw out the old pending operations
            this.requests.clear();
            this.changedNodes.clear();
            this.cachedNodes.clear();
            this.deletedNodes.clear();
        }

    }

    /**
     * Refreshes (removes the cached state) for the node with the given UUID and any of its descendants.
     * <p>
     * If {@code keepChanges == true}, modified nodes will not have their state refreshed.
     * </p>
     * 
     * @param nodeUuid the UUID of the node that is to be saved; may not be null
     * @param keepChanges indicates whether changed nodes should be kept or refreshed from the repository.
     * @throws RepositoryException if any error resulting while saving the changes to the repository
     */
    public void refresh( UUID nodeUuid,
                         boolean keepChanges ) throws RepositoryException {
        assert nodeUuid != null;
        // If the node being refreshed is the root node, then it's more efficient to refresh the whole workspace ...
        if (this.root.equals(nodeUuid)) {
            refresh(keepChanges);
            return;
        }

        // Build the set of affected node UUIDs
        Set<UUID> nodesUnderBranch = new HashSet<UUID>();
        Stack<UUID> nodesToVisit = new Stack<UUID>();
        Set<UUID> nodesRemovedFromBranch = new HashSet<UUID>();

        nodesToVisit.add(nodeUuid);
        while (!nodesToVisit.isEmpty()) {
            UUID uuid = nodesToVisit.pop();
            nodesUnderBranch.add(uuid);

            NodeInfo nodeInfo = null;
            ChangedNodeInfo changedInfo = this.changedNodes.get(uuid);
            if (changedInfo != null) {
                Collection<UUID> removedNodes = changedInfo.getUuidsForRemovedChildren();
                nodesToVisit.addAll(removedNodes);
                nodesRemovedFromBranch.addAll(removedNodes);
                nodeInfo = changedInfo;
            } else {
                nodeInfo = this.cachedNodes.get(uuid);
            }
            if (nodeInfo != null) {
                for (ChildNode childNode : nodeInfo.getChildren()) {
                    nodesToVisit.add(childNode.getUuid());
                }
            }
        }

        if (!nodesRemovedFromBranch.isEmpty()) {
            // Skip any nodes that were moved from one parent to another within this branch ...
            nodesRemovedFromBranch.removeAll(nodesUnderBranch);
            // Skip any nodes that were actually deleted (not moved)...
            nodesRemovedFromBranch.removeAll(this.deletedNodes.keySet());
            if (!nodesRemovedFromBranch.isEmpty()) {
                // There was at least one node that was moved from this branch to another parent outside this branch
                Path path = getPathFor(nodeUuid);
                String msg = JcrI18n.unableToRefreshBranchSinceAtLeastOneNodeMovedToParentOutsideOfBranch.text(path,
                                                                                                               workspaceName());
                throw new RepositoryException(msg);
            }
        }

        // Find the path of the given node ...
        Path path = getPathFor(nodeUuid);

        if (keepChanges) {
            // Keep the pending operations
            for (UUID uuid : nodesUnderBranch) {
                // getPathFor can (and will) add entries to cachedNodes - hence the existence of nodesToCheck
                if (getPathFor(uuid).isDecendantOf(path)) {
                    if (!this.changedNodes.containsKey(uuid) && !this.deletedNodes.containsKey(uuid)) {
                        this.cachedNodes.remove(uuid);
                    }
                }
            }
        } else {
            this.cachedNodes.keySet().removeAll(nodesUnderBranch);
            this.changedNodes.keySet().removeAll(nodesUnderBranch);
            this.deletedNodes.keySet().removeAll(nodesUnderBranch);

            // Throw out the old pending operations
            if (operations.isExecuteRequired()) {

                // Make sure the builder has finished all the requests ...
                this.requestBuilder.finishPendingRequest();

                // Remove all of the enqueued requests for this branch ...
                for (Iterator<Request> iter = this.requests.iterator(); iter.hasNext();) {
                    Request request = iter.next();
                    assert request instanceof ChangeRequest;
                    ChangeRequest change = (ChangeRequest)request;
                    if (change.changes(workspaceName, path)) {
                        iter.remove();
                    }
                }
            }
        }

    }

    /**
     * Checks that the child items of the node are consistent with the definitions required by the node's primary type and mixin
     * types (if any).
     * <p>
     * This method first checks that all of the child nodes and properties for the node have definitions based on the current
     * primary and mixin node types for the node as held in the node type registry. The method then checks that all mandatory (and
     * non-protected) items are populated.
     * </p>
     * 
     * @param nodeUuid the UUID of the node to check
     * @param checkSns if true indicates that this method should distinguish between child nodes that have no matching definition
     *        and child nodes that would have a definition that would match if it allowed same-name siblings. This flag determines
     *        which exception type should be thrown in that case.
     * @throws ItemExistsException if checkSns is true and there is no definition that allows same-name siblings for one of the
     *         node's child nodes and the node already has a child node with the given name
     * @throws ConstraintViolationException if one of the node's properties or child nodes does not have a matching definition for
     *         the name and type among the node's primary and mixin types; this should only occur if type definitions have been
     *         modified since the node was loaded or modified.
     * @throws RepositoryException if any other error occurs
     */
    private void checkAgainstTypeDefinitions( UUID nodeUuid,
                                              boolean checkSns )
        throws ConstraintViolationException, ItemExistsException, RepositoryException {

        assert nodeUuid != null;

        if (this.deletedNodes.containsKey(nodeUuid)) {
            nodeUuid = this.deletedNodes.get(nodeUuid).getParent();
        }

        NodeInfo nodeInfo = findNodeInfo(nodeUuid);
        AbstractJcrNode node = findJcrNode(nodeUuid);

        Name primaryTypeName = node.getPrimaryTypeName();
        List<Name> mixinTypeNames = node.getMixinTypeNames();
        Set<JcrNodeDefinition> satisfiedChildNodes = new HashSet<JcrNodeDefinition>();
        Set<JcrPropertyDefinition> satisfiedProperties = new HashSet<JcrPropertyDefinition>();

        for (AbstractJcrProperty property : findJcrPropertiesFor(nodeUuid)) {
            JcrPropertyDefinition definition = findBestPropertyDefintion(primaryTypeName,
                                                                         mixinTypeNames,
                                                                         property.property(),
                                                                         property.getType(),
                                                                         property.property().isSingle(),
                                                                         false);
            if (definition == null) {
                throw new ConstraintViolationException(JcrI18n.noDefinition.text("property",
                                                                                 property.getName(),
                                                                                 node.getPath(),
                                                                                 primaryTypeName,
                                                                                 mixinTypeNames));
            }

            satisfiedProperties.add(definition);
        }

        Children children = nodeInfo.getChildren();
        for (ChildNode child : children) {
            int snsCount = children.getCountOfSameNameSiblingsWithName(child.getName());
            NodeInfo childInfo = findNodeInfo(child.getUuid());
            JcrNodeDefinition definition = nodeTypes().findChildNodeDefinition(primaryTypeName,
                                                                               mixinTypeNames,
                                                                               child.getName(),
                                                                               childInfo.getPrimaryTypeName(),
                                                                               snsCount,
                                                                               false);
            if (definition == null) {
                if (checkSns && snsCount > 1) {
                    definition = nodeTypes().findChildNodeDefinition(primaryTypeName,
                                                                     mixinTypeNames,
                                                                     child.getName(),
                                                                     childInfo.getPrimaryTypeName(),
                                                                     1,
                                                                     false);

                    if (definition != null) {
                        throw new ItemExistsException(JcrI18n.noSnsDefinition.text(child.getName(),
                                                                                   node.getPath(),
                                                                                   primaryTypeName,
                                                                                   mixinTypeNames));
                    }
                }
                throw new ConstraintViolationException(JcrI18n.noDefinition.text("child node",
                                                                                 child.getName(),
                                                                                 node.getPath(),
                                                                                 primaryTypeName,
                                                                                 mixinTypeNames));
            }
            satisfiedChildNodes.add(definition);
        }

        JcrNodeType primaryType = nodeTypes().getNodeType(primaryTypeName);
        for (JcrPropertyDefinition definition : primaryType.getPropertyDefinitions()) {
            if (definition.isMandatory() && !definition.isProtected() && !satisfiedProperties.contains(definition)) {
                throw new ConstraintViolationException(JcrI18n.noDefinition.text("property",
                                                                                 definition.getName(),
                                                                                 node.getPath(),
                                                                                 primaryTypeName,
                                                                                 mixinTypeNames));
            }
        }
        for (JcrNodeDefinition definition : primaryType.getChildNodeDefinitions()) {
            if (definition.isMandatory() && !definition.isProtected() && !satisfiedChildNodes.contains(definition)) {
                throw new ConstraintViolationException(JcrI18n.noDefinition.text("child node",
                                                                                 definition.getName(),
                                                                                 node.getPath(),
                                                                                 primaryTypeName,
                                                                                 mixinTypeNames));
            }
        }

        for (Name mixinTypeName : mixinTypeNames) {
            JcrNodeType mixinType = nodeTypes().getNodeType(mixinTypeName);
            for (JcrPropertyDefinition definition : mixinType.getPropertyDefinitions()) {
                if (definition.isMandatory() && !definition.isProtected() && !satisfiedProperties.contains(definition)) {
                    throw new ConstraintViolationException(JcrI18n.noDefinition.text("child node",
                                                                                     definition.getName(),
                                                                                     node.getPath(),
                                                                                     primaryTypeName,
                                                                                     mixinTypeNames));
                }
            }
            for (JcrNodeDefinition definition : mixinType.getChildNodeDefinitions()) {
                if (definition.isMandatory() && !definition.isProtected() && !satisfiedChildNodes.contains(definition)) {
                    throw new ConstraintViolationException(JcrI18n.noDefinition.text("child node",
                                                                                     definition.getName(),
                                                                                     node.getPath(),
                                                                                     primaryTypeName,
                                                                                     mixinTypeNames));
                }
            }

        }
    }

    /**
     * Find the best definition for the child node with the given name on the node with the given UUID.
     * 
     * @param nodeUuid the parent node; may not be null
     * @param newNodeName the name of the potential new child node; may not be null
     * @param newNodePrimaryTypeName the primary type of the potential new child node; may not be null
     * @return the definition that best fits the new node name and type
     * @throws ItemExistsException if there is no definition that allows same-name siblings for the name and type and the parent
     *         node already has a child node with the given name
     * @throws ConstraintViolationException if there is no definition for the name and type among the parent node's primary and
     *         mixin types
     * @throws RepositoryException if any other error occurs
     */
    protected JcrNodeDefinition findBestNodeDefinition( UUID nodeUuid,
                                                        Name newNodeName,
                                                        Name newNodePrimaryTypeName )
        throws ItemExistsException, ConstraintViolationException, RepositoryException {
        assert (nodeUuid != null);
        assert (newNodeName != null);

        NodeInfo nodeInfo = findNodeInfo(nodeUuid);
        AbstractJcrNode node = findJcrNode(nodeUuid);

        return findBestNodeDefinition(nodeInfo, node.getPath(), newNodeName, newNodePrimaryTypeName);
    }

    /**
     * Find the best definition for the child node with the given name on the node with the given UUID.
     * 
     * @param parentInfo the parent node's info; may not be null
     * @param parentPath the path to the parent node; may not be null
     * @param newNodeName the name of the potential new child node; may not be null
     * @param newNodePrimaryTypeName the primary type of the potential new child node; may not be null
     * @return the definition that best fits the new node name and type
     * @throws ItemExistsException if there is no definition that allows same-name siblings for the name and type and the parent
     *         node already has a child node with the given name
     * @throws ConstraintViolationException if there is no definition for the name and type among the parent node's primary and
     *         mixin types
     * @throws RepositoryException if any other error occurs
     */
    protected JcrNodeDefinition findBestNodeDefinition( NodeInfo parentInfo,
                                                        String parentPath,
                                                        Name newNodeName,
                                                        Name newNodePrimaryTypeName )
        throws ItemExistsException, ConstraintViolationException, RepositoryException {
        assert (parentInfo != null);
        assert (parentPath != null);
        assert (newNodeName != null);

        Name primaryTypeName = parentInfo.getPrimaryTypeName();
        List<Name> mixinTypeNames = parentInfo.getMixinTypeNames();

        Children children = parentInfo.getChildren();
        // Need to add one to speculate that this node will be added
        int snsCount = children.getCountOfSameNameSiblingsWithName(newNodeName) + 1;
        JcrNodeDefinition definition = nodeTypes().findChildNodeDefinition(primaryTypeName,
                                                                           mixinTypeNames,
                                                                           newNodeName,
                                                                           newNodePrimaryTypeName,
                                                                           snsCount,
                                                                           true);
        if (definition == null) {
            if (snsCount > 1) {
                definition = nodeTypes().findChildNodeDefinition(primaryTypeName,
                                                                 mixinTypeNames,
                                                                 newNodeName,
                                                                 newNodePrimaryTypeName,
                                                                 1,
                                                                 true);

                if (definition != null) {
                    throw new ItemExistsException(JcrI18n.noSnsDefinition.text(newNodeName,
                                                                               parentPath,
                                                                               primaryTypeName,
                                                                               mixinTypeNames));
                }
            }

            throw new ConstraintViolationException(JcrI18n.noDefinition.text("child node",
                                                                             newNodeName,
                                                                             parentPath,
                                                                             primaryTypeName,
                                                                             mixinTypeNames));
        }

        return definition;
    }

    /**
     * The JCR specification assumes that reading a node into the session does not imply reading the relationship between the node
     * and its children into the session. As a performance optimization, DNA eagerly loads the list of child names and UUIDs (but
     * not the child nodes themselves). This creates an issue when direct writes are performed through the workspace. The act of
     * modifying a node is assumed to imply loading its children, but we must load the node in order to modify it.
     * <p>
     * This method provides a way to signal that a child should be added to one parent and, optionally, removed from another. The
     * cache of loaded nodes and the cache of changed nodes are modified accordingly, but no additional graph requests are
     * batched.
     * </p>
     * 
     * @param newParentUuid the UUID of the node to which the child is to be moved; may not be null
     * @param oldParentUuid the UUID of the parent node from which the child was moved; may not be null
     * @param child the UUID of the child node that was moved or copied; may not be null
     * @param childName the new name of the child node; may not be null
     * @throws RepositoryException if an error occurs
     */
    public void compensateForWorkspaceChildChange( UUID newParentUuid,
                                                   UUID oldParentUuid,
                                                   UUID child,
                                                   Name childName ) throws RepositoryException {
        assert newParentUuid != null;
        assert child != null;
        assert childName != null;

        ChangedNodeInfo changedNode = this.changedNodes.get(newParentUuid);
        if (changedNode != null) {
            // This adds the child to the changed node, but doesn't generate a corresponding pending request
            // avoiding a challenge later.
            changedNode.addChild(childName, child, this.pathFactory);

        } else {
            this.cachedNodes.remove(newParentUuid);
        }

        if (oldParentUuid != null) {
            changedNode = this.changedNodes.get(oldParentUuid);
            if (changedNode != null) {
                changedNode.removeChild(child, this.pathFactory);
            } else {
                this.cachedNodes.remove(newParentUuid);
            }
        }
    }

    /**
     * Save any changes that have been accumulated by this session.
     * 
     * @throws RepositoryException if any error resulting while saving the changes to the repository
     */
    public void save() throws RepositoryException {
        if (operations.isExecuteRequired()) {
            for (UUID changedUuid : this.changedNodes.keySet()) {
                checkAgainstTypeDefinitions(changedUuid, false);
            }

            // Execute the batched operations ...
            try {
                operations.execute();
            } catch (org.jboss.dna.graph.property.PathNotFoundException e) {
                throw new InvalidItemStateException(e.getLocalizedMessage(), e);
            } catch (RuntimeException e) {
                throw new RepositoryException(e.getLocalizedMessage(), e);
            }

            // Create a new batch for future operations ...
            // LinkedList<Request> oldRequests = this.requests;
            this.requests = new LinkedList<Request>();
            this.requestBuilder = new BatchRequestBuilder(this.requests);
            operations = store.batch(this.requestBuilder);

            // Remove all the cached items that have been changed or deleted ...
            for (UUID changedUuid : changedNodes.keySet()) {
                cachedNodes.remove(changedUuid);
            }
            for (UUID changedUuid : deletedNodes.keySet()) {
                cachedNodes.remove(changedUuid);
            }
            // Remove all the changed and deleted infos ...
            changedNodes.clear();
            deletedNodes.clear();
        }
    }

    /**
     * Save any changes to the identified node or its descendants. The supplied node may not have been deleted or created in this
     * session since the last save operation.
     * 
     * @param nodeUuid the UUID of the node that is to be saved; may not be null
     * @throws RepositoryException if any error resulting while saving the changes to the repository
     */
    public void save( UUID nodeUuid ) throws RepositoryException {
        assert nodeUuid != null;
        if (deletedNodes.containsKey(nodeUuid)) {
            // This node was deleted in this session ...
            throw new InvalidItemStateException(JcrI18n.nodeHasAlreadyBeenRemovedFromThisSession.text(nodeUuid, workspaceName()));
        }

        // The node must not have been created since the last save ...
        if (!cachedNodes.containsKey(nodeUuid)) {
            // It is not cached, which means it WAS created ...
            Path path = getPathFor(nodeUuid);
            throw new RepositoryException(JcrI18n.unableToSaveNodeThatWasCreatedSincePreviousSave.text(path, workspaceName()));
        }

        if (operations.isExecuteRequired()) {
            // Find the path of the given node ...
            Path path = getPathFor(nodeUuid);

            // Make sure the builder has finished all the requests ...
            this.requestBuilder.finishPendingRequest();

            // Remove all of the enqueued requests for this branch ...
            LinkedList<Request> branchRequests = new LinkedList<Request>();
            LinkedList<Request> nonBranchRequests = new LinkedList<Request>();
            Set<UUID> branchUuids = new HashSet<UUID>();
            for (Request request : this.requests) {
                assert request instanceof ChangeRequest;
                ChangeRequest change = (ChangeRequest)request;
                if (change.changes(workspaceName, path)) {
                    branchRequests.add(request);
                    // Record the UUID of the node being saved now ...
                    UUID changedUuid = null;
                    if (change instanceof CreateNodeRequest) {
                        // We want the parent UUID ...
                        changedUuid = ((CreateNodeRequest)change).under().getUuid();
                    } else {
                        changedUuid = change.changedLocation().getUuid();
                    }
                    assert changedUuid != null;
                    branchUuids.add(changedUuid);
                } else {
                    nonBranchRequests.add(request);
                }
            }

            if (branchRequests.isEmpty()) {
                // None of the changes affected the branch given by the node ...
                return;
            }

            /*
             * branchUuids contains all the roots of the changes, but there may be further changes under the roots (e.g., a
             * newly created node will have it's parent's UUID in branchUuids, but not the new node's uuid. 
             */
            Set<UUID> uuidsUnderBranch = new HashSet<UUID>();
            LinkedList<UUID> peersToCheck = new LinkedList<UUID>();

            for (UUID changedUuid : branchUuids) {
                checkAgainstTypeDefinitions(changedUuid, false);
            }

            for (UUID branchUuid : branchUuids) {
                uuidsUnderBranch.add(branchUuid);
                ChangedNodeInfo changedNode = changedNodes.get(branchUuid);
                if (changedNode != null) {
                    for (ChildNode childNode : changedNode.getChildren()) {
                        uuidsUnderBranch.add(childNode.getUuid());
                    }

                    Collection<UUID> peers = changedNode.getPeers();
                    if (peers != null) peersToCheck.addAll(peers);
                }

            }

            /*
             * Need to check that any peers in a Session.move operation are both in the save
             */
            for (UUID peerUuid : peersToCheck) {
                if (!uuidsUnderBranch.contains(peerUuid)) {
                    throw new ConstraintViolationException();
                }
            }

            // Now execute the branch ...
            Graph.Batch branchBatch = store.batch(new BatchRequestBuilder(branchRequests));
            try {
                branchBatch.execute();

                // Still have non-branch related requests that we haven't executed ...
                this.requests = nonBranchRequests;
                this.requestBuilder = new BatchRequestBuilder(this.requests);
                this.operations = store.batch(this.requestBuilder);

                // Remove all the cached, changed or deleted items that were just saved ...
                cachedNodes.keySet().removeAll(uuidsUnderBranch);
                changedNodes.keySet().removeAll(uuidsUnderBranch);
                deletedNodes.keySet().removeAll(uuidsUnderBranch);
            } catch (org.jboss.dna.graph.property.PathNotFoundException e) {
                throw new InvalidItemStateException(e.getLocalizedMessage(), e);
            } catch (RuntimeException e) {
                throw new RepositoryException(e.getLocalizedMessage(), e);
            }
        }
    }

    public JcrRootNode findJcrRootNode() throws RepositoryException {
        return (JcrRootNode)findJcrNode(findNodeInfoForRoot().getUuid());
    }

    /**
     * Find the JCR {@link JcrNode Node implementation} for the given UUID.
     * 
     * @param uuid the node's UUID
     * @return the existing node implementation
     * @throws ItemNotFoundException if a node with the supplied UUID could not be found
     * @throws RepositoryException if an error resulting in finding this node in the repository
     */
    public AbstractJcrNode findJcrNode( UUID uuid ) throws ItemNotFoundException, RepositoryException {
        AbstractJcrNode node = jcrNodes.get(uuid);
        if (node != null) return node;

        // An existing JCR Node object was not found, so we'll have to create it by finding the underlying
        // NodeInfo for the node (from the changed state or the cache) ...
        NodeInfo info = findNodeInfo(uuid);
        assert info != null;

        // Create the appropriate JCR Node object ...
        return createAndCacheJcrNodeFor(info);
    }

    /**
     * Find the JCR {@link AbstractJcrNode Node implementation} for the node given by the UUID of a reference node and a relative
     * path from the reference node. The relative path should already have been {@link Path#getNormalizedPath() normalized}.
     * 
     * @param uuidOfReferenceNode the UUID of the reference node; may be null if the root node is to be used as the reference
     * @param relativePath the relative (but normalized) path from the reference node, but which may be an absolute (and
     *        normalized) path only when the reference node is the root node; may not be null
     * @return the information for the referenced node; never null
     * @throws ItemNotFoundException if the reference node with the supplied UUID does not exist
     * @throws PathNotFoundException if the node given by the relative path does not exist
     * @throws InvalidItemStateException if the node with the UUID has been deleted in this session
     * @throws RepositoryException if any other error occurs while reading information from the repository
     * @see #findNodeInfoForRoot()
     */
    public AbstractJcrNode findJcrNode( UUID uuidOfReferenceNode,
                                        Path relativePath )
        throws PathNotFoundException, InvalidItemStateException, RepositoryException {
        // An existing JCR Node object was not found, so we'll have to create it by finding the underlying
        // NodeInfo for the node (from the changed state or the cache) ...
        NodeInfo info = findNodeInfo(uuidOfReferenceNode, relativePath);
        assert info != null;

        // Look for an existing node ...
        AbstractJcrNode node = jcrNodes.get(info.getUuid());
        if (node != null) return node;

        // Create the appropriate JCR Node object ...
        return createAndCacheJcrNodeFor(info);
    }

    public AbstractJcrProperty findJcrProperty( PropertyId propertyId ) throws PathNotFoundException, RepositoryException {
        AbstractJcrProperty property = jcrProperties.get(propertyId);
        if (property != null) return property;

        // An existing JCR Property object was not found, so we'll have to create it by finding the underlying
        // NodeInfo for the property's parent (from the changed state or the cache) ...
        PropertyInfo info = findPropertyInfo(propertyId); // throws PathNotFoundException if node not there
        if (info == null) return null; // no such property on this node

        // Skip all internal properties ...
        if (info.getPropertyName().getNamespaceUri().equals(DnaIntLexicon.Namespace.URI)) return null;

        // Now create the appropriate JCR Property object ...
        return createAndCacheJcrPropertyFor(info);
    }

    public Collection<AbstractJcrProperty> findJcrPropertiesFor( UUID nodeUuid )
        throws ItemNotFoundException, RepositoryException {
        NodeInfo info = findNodeInfo(nodeUuid);
        Set<Name> propertyNames = info.getPropertyNames();
        Collection<AbstractJcrProperty> result = new ArrayList<AbstractJcrProperty>(propertyNames.size());
        for (Name propertyName : propertyNames) {
            if (!propertyName.getNamespaceUri().equals(DnaIntLexicon.Namespace.URI)) {
                result.add(findJcrProperty(new PropertyId(nodeUuid, propertyName)));
            }
        }
        return result;
    }

    /**
     * Find the JCR {@link AbstractJcrItem Item implementation} for the node or property given by the UUID of a reference node and
     * a relative path from the reference node to the desired item. The relative path should already have been
     * {@link Path#getNormalizedPath() normalized}.
     * 
     * @param uuidOfReferenceNode the UUID of the reference node; may be null if the root node is to be used as the reference
     * @param relativePath the relative (but normalized) path from the reference node to the desired item, but which may be an
     *        absolute (and normalized) path only when the reference node is the root node; may not be null
     * @return the information for the referenced item; never null
     * @throws ItemNotFoundException if the reference node with the supplied UUID does not exist, or if an item given by the
     *         supplied relative path does not exist
     * @throws InvalidItemStateException if the node with the UUID has been deleted in this session
     * @throws RepositoryException if any other error occurs while reading information from the repository
     * @see #findNodeInfoForRoot()
     */
    public AbstractJcrItem findJcrItem( UUID uuidOfReferenceNode,
                                        Path relativePath )
        throws ItemNotFoundException, InvalidItemStateException, RepositoryException {
        // A pathological case is an empty relative path ...
        if (relativePath.size() == 0) {
            return findJcrNode(uuidOfReferenceNode);
        }
        if (relativePath.size() == 1) {
            Path.Segment segment = relativePath.getLastSegment();
            if (segment.isSelfReference()) return findJcrNode(uuidOfReferenceNode);
            if (segment.isParentReference()) {
                NodeInfo referencedNode = findNodeInfo(uuidOfReferenceNode);
                return findJcrNode(referencedNode.getParent());
            }
        }

        // Peek into the last segment of the path to see whether it uses a SNS index (and it's > 1) ...
        Path.Segment lastSegment = relativePath.getLastSegment();
        if (lastSegment.getIndex() > 1) {
            // Only nodes can have SNS index (but an index of 1 is the default)...
            return findJcrNode(uuidOfReferenceNode);
        }

        NodeInfo parent = null;
        if (relativePath.size() == 1) {
            // The referenced node must be the parent ...
            parent = findNodeInfo(uuidOfReferenceNode);
        } else {
            // We know that the parent of the referenced item should be a node (if the path is right) ...
            parent = findNodeInfo(uuidOfReferenceNode, relativePath.getParent());
        }

        // JSR-170 doesn't allow children and proeprties to have the same name, but this is relaxed in JSR-283.
        // But JSR-283 Section 3.3.4 states "The method Session.getItem will return the item at the specified path
        // if there is only one such item, if there is both a node and a property at the specified path, getItem
        // will return the node." Therefore, look for a child first ...
        ChildNode child = parent.getChildren().getChild(lastSegment);
        if (child != null) {
            return findJcrNode(child.getUuid());
        }

        // Otherwise it should be a property ...
        PropertyInfo propertyInfo = parent.getProperty(lastSegment.getName());
        if (propertyInfo != null) {
            return findJcrProperty(propertyInfo.getPropertyId());
        }

        // It was not found, so prepare a good exception message ...
        String msg = null;
        if (findNodeInfoForRoot().getUuid().equals(uuidOfReferenceNode)) {
            // The reference node was the root, so use this fact to convert the path to an absolute path in the message
            Path absolutePath = rootPath.resolve(relativePath);
            msg = JcrI18n.itemNotFoundAtPath.text(absolutePath, workspaceName);
        } else {
            // Find the path of the reference node ...
            Path referenceNodePath = getPathFor(uuidOfReferenceNode);
            msg = JcrI18n.itemNotFoundAtPathRelativeToReferenceNode.text(relativePath, referenceNodePath, workspaceName);
        }
        throw new ItemNotFoundException(msg);
    }

    /**
     * Obtain an {@link NodeEditor editor} that can be used to manipulate the properties or children on the node identified by the
     * supplied UUID. The node must exist prior to this call, either as a node that exists in the workspace or as a node that was
     * created within this session but not yet persisted to the workspace. This method returns an editor that batches all changes
     * in transient storage from where they can be persisted to the repository by {@link javax.jcr.Session#save() saving the
     * session} or by {@link javax.jcr.Item#save() saving an ancestor}.
     * 
     * @param uuid the UUID of the node that is to be changed; may not be null and must represent an <i>existing</i> node
     * @return the editor; never null
     * @throws ItemNotFoundException if no such node could be found in the session or workspace
     * @throws InvalidItemStateException if the item has been marked for deletion within this session
     * @throws RepositoryException if any other error occurs while reading information from the repository
     */
    public NodeEditor getEditorFor( UUID uuid ) throws ItemNotFoundException, InvalidItemStateException, RepositoryException {
        return getEditorFor(uuid, this.operations);
    }

    /**
     * Obtain an {@link NodeEditor editor} that can be used to manipulate the properties or children on the node identified by the
     * supplied UUID. The node must exist prior to this call, either as a node that exists in the workspace or as a node that was
     * created within this session but not yet persisted to the workspace.
     * 
     * @param uuid the UUID of the node that is to be changed; may not be null and must represent an <i>existing</i> node
     * @param operationsBatch the {@link Graph.Batch} to use for batching operations. This should be populated for direct
     *        persistence (q.v. section 7.1.3.7 of the JCR 1.0.1 specification) and should be null to use session-based
     *        persistence.
     * @return the editor; never null
     * @throws ItemNotFoundException if no such node could be found in the session or workspace
     * @throws InvalidItemStateException if the item has been marked for deletion within this session
     * @throws RepositoryException if any other error occurs while reading information from the repository
     */
    public NodeEditor getEditorFor( UUID uuid,
                                    Graph.Batch operationsBatch )
        throws ItemNotFoundException, InvalidItemStateException, RepositoryException {
        // See if we already have something in the changed nodes ...
        ChangedNodeInfo info = changedNodes.get(uuid);
        Location currentLocation = null;
        if (info == null) {
            // Or in the cache ...
            NodeInfo cached = cachedNodes.get(uuid);
            if (cached == null) {
                cached = loadFromGraph(null, uuid);
            }
            // Now put into the changed nodes ...
            info = new ChangedNodeInfo(cached);
            changedNodes.put(uuid, info);
            currentLocation = info.getOriginalLocation();
        } else {
            // compute the current location ...
            currentLocation = Location.create(getPathFor(info), uuid);
        }
        return new NodeEditor(info, currentLocation, operationsBatch == null ? this.operations : operationsBatch);
    }

    /**
     * An interface used to manipulate a node's properties and children.
     */
    public final class NodeEditor {
        private final ChangedNodeInfo node;
        private final Location currentLocation;
        private final Graph.Batch operations;

        protected NodeEditor( ChangedNodeInfo node,
                              Location currentLocation,
                              Graph.Batch operations ) {
            this.node = node;
            this.currentLocation = currentLocation;
            this.operations = operations;
        }

        /**
         * Checks whether there is an existing property with this name that does not match the given cardinality. If such a
         * property exists, a {@code javax.jcr.ValueFormatException} is thrown, as per section 7.1.5 of the JCR 1.0.1
         * specification.
         * 
         * @param propertyName the name of the property
         * @param isMultiple whether the property must have multiple values
         * @throws javax.jcr.ValueFormatException if the property exists but has the opposite cardinality
         * @throws RepositoryException if any other error occurs
         */
        private void checkCardinalityOfExistingProperty( Name propertyName,
                                                         boolean isMultiple )
            throws javax.jcr.ValueFormatException, RepositoryException {
            // Check for existing single-valued property - can't set multiple values on single-valued property
            PropertyInfo propInfo = this.node.getProperty(propertyName);
            if (propInfo != null && propInfo.isMultiValued() != isMultiple) {
                NamespaceRegistry namespaces = SessionCache.this.namespaces;
                String workspaceName = SessionCache.this.workspaceName();
                if (isMultiple) {
                    I18n msg = JcrI18n.unableToSetSingleValuedPropertyUsingMultipleValues;
                    throw new javax.jcr.ValueFormatException(msg.text(propertyName.getString(namespaces),
                                                                      getPathFor(this.node).getString(namespaces),
                                                                      workspaceName));
                }
                I18n msg = JcrI18n.unableToSetMultiValuedPropertyUsingSingleValue;
                throw new javax.jcr.ValueFormatException(msg.text(getPathFor(this.node).getString(namespaces),
                                                                  propertyName,
                                                                  workspaceName));
            }

        }

        /**
         * Set the value for the property. If the property does not exist, it will be added. If the property does exist, the
         * existing values will be replaced with the supplied value.
         * 
         * @param name the property name; may not be null
         * @param value the new property values; may not be null
         * @return the identifier for the property; never null
         * @throws ConstraintViolationException if the property could not be set because of a node type constraint or property
         *         definition constraint
         * @throws AccessDeniedException if the current session does not have the requisite privileges to perform this task
         * @throws RepositoryException if any other error occurs
         */
        public PropertyId setProperty( Name name,
                                       JcrValue value )
            throws AccessDeniedException, ConstraintViolationException, RepositoryException {
            return setProperty(name, value, true);
        }

        /**
         * Set the value for the property. If the property does not exist, it will be added. If the property does exist, the
         * existing values will be replaced with the supplied value. Protected property definitions may be considered, based on
         * the {@code skipProtected} flag.
         * 
         * @param name the property name; may not be null
         * @param value the new property values; may not be null
         * @param skipProtected indicates whether protected property definitions should be ignored
         * @return the identifier for the property; never null
         * @throws ConstraintViolationException if the property could not be set because of a node type constraint or property
         *         definition constraint
         * @throws AccessDeniedException if the current session does not have the requisite privileges to perform this task
         * @throws RepositoryException if any other error occurs
         */
        public PropertyId setProperty( Name name,
                                       JcrValue value,
                                       boolean skipProtected )
            throws AccessDeniedException, ConstraintViolationException, RepositoryException {
            assert name != null;
            assert value != null;

            SessionCache.this.checkPermission(node, JcrSession.JCR_SET_PROPERTY_PERMISSION);

            JcrPropertyDefinition definition = null;
            PropertyId id = null;

            checkCardinalityOfExistingProperty(name, false);

            // Look for an existing property ...
            PropertyInfo existing = node.getProperty(name);
            if (existing != null) {
                // Reuse the existing ID ...
                id = existing.getPropertyId();

                // We're replacing an existing property, but we still need to check that the property definition
                // (still) defines a type. So, find the property definition for the existing property ...
                definition = nodeTypes().getPropertyDefinition(existing.getDefinitionId());

                if (definition != null) {
                    // The definition's require type must match the value's ...
                    if (definition.getRequiredType() != PropertyType.UNDEFINED && definition.getRequiredType() != value.getType()) {
                        // The property type is not right, so we have to check if we can cast.
                        // It's easier and could save more work if we just find a new property definition that works ...
                        definition = null;
                    } else {
                        // The types match, so see if the value satisfies the constraints ...
                        if (!definition.satisfiesConstraints(value)) definition = null;
                    }
                }
            } else {
                // This is a new property, so create a new ID ...
                id = new PropertyId(node.getUuid(), name);
            }
            if (definition == null) {
                // Look for a definition ...
                definition = nodeTypes().findPropertyDefinition(node.getPrimaryTypeName(),
                                                                node.getMixinTypeNames(),
                                                                name,
                                                                value,
                                                                true,
                                                                skipProtected);
                if (definition == null) {
                    throw new ConstraintViolationException();
                }
            }
            // Create the DNA property ...
            Object objValue = value.value();
            int propertyType = definition.getRequiredType();
            if (propertyType == PropertyType.UNDEFINED || propertyType == value.getType()) {
                // Can use the values as is ...
                propertyType = value.getType();
            } else {
                // A cast is required ...
                org.jboss.dna.graph.property.PropertyType dnaPropertyType = PropertyTypeUtil.dnaPropertyTypeFor(propertyType);
                ValueFactory<?> factory = factories().getValueFactory(dnaPropertyType);
                objValue = factory.create(objValue);
            }
            Property dnaProp = propertyFactory.create(name, objValue);

            // Create the property info ...
            PropertyInfo newProperty = new PropertyInfo(id, definition.getId(), propertyType, dnaProp, definition.isMultiple(),
                                                        existing == null, existing != null);

            // Finally update the cached information and record the change ...
            node.setProperty(newProperty, factories());
            operations.set(dnaProp).on(currentLocation);
            return newProperty.getPropertyId();
        }

        /**
         * Set the values for the property. If the property does not exist, it will be added. If the property does exist, the
         * existing values will be replaced with those that are supplied.
         * <p>
         * This method will not set protected property definitions and should be used in almost all cases.
         * </p>
         * 
         * @param name the property name; may not be null
         * @param values new property values, all of which must have the same {@link Value#getType() property type}; may not be
         *        null but may be empty
         * @param valueType
         * @return the identifier for the property; never null
         * @throws ConstraintViolationException if the property could not be set because of a node type constraint or property
         *         definition constraint
         * @throws javax.jcr.ValueFormatException
         * @throws AccessDeniedException if the current session does not have the requisite privileges to perform this task
         * @throws RepositoryException if any other error occurs
         */
        public PropertyId setProperty( Name name,
                                       Value[] values,
                                       int valueType )
            throws AccessDeniedException, ConstraintViolationException, RepositoryException, javax.jcr.ValueFormatException {
            return setProperty(name, values, valueType, true);
        }

        /**
         * Set the values for the property. If the property does not exist, it will be added. If the property does exist, the
         * existing values will be replaced with those that are supplied.
         * 
         * @param name the property name; may not be null
         * @param values new property values, all of which must have the same {@link Value#getType() property type}; may not be
         *        null but may be empty
         * @param skipProtected if true, attempts to set protected properties will fail. If false, attempts to set protected
         *        properties will be allowed.
         * @param valueType
         * @return the identifier for the property; never null
         * @throws ConstraintViolationException if the property could not be set because of a node type constraint or property
         *         definition constraint
         * @throws javax.jcr.ValueFormatException
         * @throws AccessDeniedException if the current session does not have the requisite privileges to perform this task
         * @throws RepositoryException if any other error occurs
         */
        public PropertyId setProperty( Name name,
                                       Value[] values,
                                       int valueType,
                                       boolean skipProtected )
            throws AccessDeniedException, ConstraintViolationException, RepositoryException, javax.jcr.ValueFormatException {
            assert name != null;
            assert values != null;

            SessionCache.this.checkPermission(node, JcrSession.JCR_SET_PROPERTY_PERMISSION);
            checkCardinalityOfExistingProperty(name, true);

            int len = values.length;
            Value[] newValues = null;
            if (len == 0) {
                newValues = JcrMultiValueProperty.EMPTY_VALUES;
            } else {
                List<Value> valuesWithDesiredType = new ArrayList<Value>(len);
                int expectedType = -1;
                for (int i = 0; i != len; ++i) {
                    Value value = values[i];

                    if (value == null) continue;
                    if (expectedType == -1) {
                        expectedType = value.getType();
                    } else if (value.getType() != expectedType) {
                        // Make sure the type of each value is the same, as per Javadoc in section 7.1.5 of the JCR 1.0.1 spec
                        StringBuilder sb = new StringBuilder();
                        sb.append('[');
                        for (int j = 0; j != values.length; ++j) {
                            if (j != 0) sb.append(",");
                            sb.append(values[j].toString());
                        }
                        sb.append(']');
                        String propType = PropertyType.nameFromValue(expectedType);
                        I18n msg = JcrI18n.allPropertyValuesMustHaveSameType;
                        NamespaceRegistry namespaces = SessionCache.this.namespaces;
                        String path = getPathFor(node.getUuid()).getString(namespaces);
                        String workspaceName = SessionCache.this.workspaceName();
                        throw new javax.jcr.ValueFormatException(msg.text(name, values, propType, path, workspaceName));
                    }
                    if (value.getType() != valueType && valueType != PropertyType.UNDEFINED) {
                        value = ((JcrValue)value).asType(valueType);
                    }
                    valuesWithDesiredType.add(value);
                }
                if (valuesWithDesiredType.isEmpty()) {
                    newValues = JcrMultiValueProperty.EMPTY_VALUES;
                } else {
                    newValues = valuesWithDesiredType.toArray(new Value[valuesWithDesiredType.size()]);
                }
            }

            int numValues = newValues.length;
            JcrPropertyDefinition definition = null;
            PropertyId id = null;

            // Look for an existing property ...
            PropertyInfo existing = node.getProperty(name);
            if (existing != null) {
                // Reuse the existing ID ...
                id = existing.getPropertyId();

                // We're replacing an existing property, but we still need to check that the property definition
                // (still) defines a type. So, find the property definition for the existing property ...
                definition = nodeTypes().getPropertyDefinition(existing.getDefinitionId());

                if (definition != null) {
                    // The definition's require type must match the value's ...
                    if (numValues == 0) {
                        // Just use the definition as is ...
                    } else {
                        // Use the property type for the first non-null value ...
                        int type = newValues[0].getType();
                        if (definition.getRequiredType() != PropertyType.UNDEFINED && definition.getRequiredType() != type) {
                            // The property type is not right, so we have to check if we can cast.
                            // It's easier and could save more work if we just find a new property definition that works ...
                            definition = null;
                        } else {
                            // The types match, so see if the value satisfies the constraints ...
                            if (!definition.satisfiesConstraints(newValues)) definition = null;
                        }
                    }
                }
            } else {
                // This is a new property, so create a new ID ...
                id = new PropertyId(node.getUuid(), name);
            }
            if (definition == null) {
                // Look for a definition ...
                definition = nodeTypes().findPropertyDefinition(node.getPrimaryTypeName(),
                                                                node.getMixinTypeNames(),
                                                                name,
                                                                newValues,
                                                                skipProtected);
                if (definition == null) {
                    throw new ConstraintViolationException();
                }
            }
            // Create the DNA property ...
            int type = numValues != 0 ? newValues[0].getType() : definition.getRequiredType();
            Object[] objValues = new Object[numValues];
            int propertyType = definition.getRequiredType();
            if (propertyType == PropertyType.UNDEFINED || propertyType == type) {
                // Can use the values as is ...
                propertyType = type;
                for (int i = 0; i != numValues; ++i) {
                    objValues[i] = ((JcrValue)newValues[i]).value();
                }
            } else {
                // A cast is required ...
                assert propertyType != type;
                org.jboss.dna.graph.property.PropertyType dnaPropertyType = PropertyTypeUtil.dnaPropertyTypeFor(propertyType);
                ValueFactory<?> factory = factories().getValueFactory(dnaPropertyType);
                for (int i = 0; i != numValues; ++i) {
                    objValues[i] = factory.create(((JcrValue)newValues[i]).value());
                }
            }
            Property dnaProp = propertyFactory.create(name, objValues);

            // Create the property info ...
            PropertyInfo newProperty = new PropertyInfo(id, definition.getId(), propertyType, dnaProp, definition.isMultiple(),
                                                        existing == null, existing != null);

            // Finally update the cached information and record the change ...
            node.setProperty(newProperty, factories());
            operations.set(dnaProp).on(currentLocation);

            // If there is a single value, we need to record that this property is actually a multi-valued property definition ...
            if (numValues == 1) {
                if (node.setSingleMultiProperty(name)) {
                    Set<Name> names = node.getSingleMultiPropertyNames();
                    // Added this property name to the set, so record the change ...
                    PropertyInfo singleMulti = createSingleMultiplePropertyInfo(node.getUuid(),
                                                                                node.getPrimaryTypeName(),
                                                                                node.getMixinTypeNames(),
                                                                                names);
                    node.setProperty(singleMulti, factories());
                    operations.set(singleMulti.getProperty()).on(currentLocation);
                }
            } else {
                removeSingleMultiProperty(node, name);
            }

            return newProperty.getPropertyId();
        }

        /**
         * Remove the existing property with the supplied name.
         * 
         * @param name the property name; may not be null
         * @return true if there was a property with the supplied name, or false if no such property existed
         * @throws AccessDeniedException if the current session does not have the requisite permissions to remove this property
         * @throws RepositoryException if any other error occurs
         */
        public boolean removeProperty( Name name ) throws AccessDeniedException, RepositoryException {
            SessionCache.this.checkPermission(node, JcrSession.JCR_REMOVE_PERMISSION);

            PropertyInfo info = node.removeProperty(name);
            if (info != null) {
                operations.remove(name).on(currentLocation);
                // Is this named in the single-multi property names? ...
                removeSingleMultiProperty(node, name);
                return true;
            }
            return false;
        }

        private void removeSingleMultiProperty( ChangedNodeInfo node,
                                                Name propertyName ) {
            if (node.removeSingleMultiProperty(propertyName)) {
                Set<Name> names = node.getSingleMultiPropertyNames();
                if (names == null || names.isEmpty()) {
                    node.removeProperty(DnaIntLexicon.MULTI_VALUED_PROPERTIES);
                    operations.remove(DnaIntLexicon.MULTI_VALUED_PROPERTIES).on(currentLocation);
                } else {
                    // Added this property name to the set, so record the change ...
                    PropertyInfo singleMulti = createSingleMultiplePropertyInfo(node.getUuid(),
                                                                                node.getPrimaryTypeName(),
                                                                                node.getMixinTypeNames(),
                                                                                names);
                    node.setProperty(singleMulti, factories());
                    operations.set(singleMulti.getProperty()).on(currentLocation);
                }
            }
        }

        public void orderChildBefore( Path.Segment childToBeMoved,
                                      Path.Segment before ) {
            // Clear the path cache ...
            SessionCache.this.pathCache.clear();

            PathFactory pathFactory = SessionCache.this.pathFactory;
            Path thisPath = this.currentLocation.getPath();
            UUID fromUuid = this.node.getChildren().getChild(childToBeMoved).getUuid();
            Location fromLocation = Location.create(pathFactory.create(thisPath, childToBeMoved), fromUuid);

            Children children = this.node.getChildren();
            ChildNode nodeToBeMoved = children.getChild(childToBeMoved);
            this.node.removeChild(nodeToBeMoved.getUuid(), pathFactory);

            if (before == null) {
                this.node.addChild(nodeToBeMoved.getName(), nodeToBeMoved.getUuid(), pathFactory);
                // Moving the node into its parent will remove it from its current spot in the child list and re-add it to the end
                operations.move(fromLocation).into(this.currentLocation);

            } else {
                Path beforePath = pathFactory.create(thisPath, before);
                UUID beforeUuid = this.node.getChildren().getChild(before).getUuid();
                Location beforeLocation = Location.create(beforePath, beforeUuid);

                this.node.addChild(nodeToBeMoved.getName(), before, nodeToBeMoved.getUuid(), pathFactory);
                operations.move(fromLocation).before(beforeLocation);
            }
        }

        /**
         * Move the child specified by the supplied UUID to be a child of this node, appending the child to the end of the current
         * list of children. This method automatically disconnects the node from its current parent.
         * 
         * @param child the UUID of the existing node; may not be null
         * @param newNodeName
         * @return the representation of the newly-added child, which includes the {@link ChildNode#getSnsIndex()
         *         same-name-sibling index}
         * @throws ItemNotFoundException if the specified child node could be found in the session or workspace
         * @throws InvalidItemStateException if the specified child has been marked for deletion within this session
         * @throws ConstraintViolationException if moving the node into this node violates this node's definition
         * @throws RepositoryException if any other error occurs while reading information from the repository
         */
        public ChildNode moveToBeChild( AbstractJcrNode child,
                                        Name newNodeName )
            throws ItemNotFoundException, InvalidItemStateException, ConstraintViolationException, RepositoryException {

            UUID nodeUuid = child.nodeUuid;
            if (nodeUuid.equals(node.getUuid()) || isAncestor(nodeUuid)) {
                Path pathOfNode = getPathFor(nodeUuid);
                Path thisPath = currentLocation.getPath();
                String msg = JcrI18n.unableToMoveNodeToBeChildOfDecendent.text(pathOfNode, thisPath, workspaceName());
                throw new RepositoryException(msg);
            }

            // Is the node already a child?
            boolean nameDoesNotChange = newNodeName == null || newNodeName.equals(child.path().getLastSegment());
            ChildNode existingChild = node.getChildren().getChild(nodeUuid);
            if (existingChild != null && nameDoesNotChange) return existingChild;

            JcrNodeDefinition definition = findBestNodeDefinition(node.getUuid(), newNodeName, null);

            // Get an editor for the child (in its current location) and one for its parent ...
            NodeEditor newChildEditor = getEditorFor(nodeUuid);

            if (!definition.getId().equals(node.getDefinitionId())) {
                // The node definition changed, so try to set the property ...
                try {
                    JcrValue value = new JcrValue(factories(), SessionCache.this, PropertyType.STRING, definition.getId()
                                                                                                                 .getString());
                    setProperty(DnaIntLexicon.NODE_DEFINITON, value);
                } catch (ConstraintViolationException e) {
                    // We can't set this property on the node (according to the node definition).
                    // But we still want the node info to have the correct node definition.
                    // When it is reloaded into a cache (after being persisted), the correct node definition
                    // will be computed again ...
                    node.setDefinitionId(definition.getId());

                    // And remove the property from the info ...
                    newChildEditor.removeProperty(DnaIntLexicon.NODE_DEFINITON);
                }
            }

            // Clear the path cache ...
            SessionCache.this.pathCache.clear();

            // Remove the node from the current parent and add it to this ...
            ChangedNodeInfo newChildInfo = newChildEditor.node;
            UUID existingParent = newChildInfo.getParent();

            ChangedNodeInfo existingParentInfo = getEditorFor(existingParent).node;
            existingChild = existingParentInfo.removeChild(nodeUuid, pathFactory);
            ChildNode newChild = node.addChild(newNodeName, existingChild.getUuid(), pathFactory);

            // Set the child's changed representation to point to this node as its parent ...
            newChildInfo.setParent(node.getUuid());

            // Set up the peer relationship between the two nodes that must be saved together
            node.addPeer(existingParent);
            existingParentInfo.addPeer(node.getUuid());

            // Set up the peer relationship between the two nodes that must be saved together
            node.addPeer(existingParent);
            existingParentInfo.addPeer(node.getUuid());

            // Now, record the operation to do this ...
            if (nameDoesNotChange) {
                operations.move(newChildEditor.currentLocation).into(currentLocation);
            } else {
                operations.move(newChildEditor.currentLocation).as(newNodeName).into(currentLocation);
            }

            return newChild;
        }

        public void addMixin( JcrNodeType mixinCandidateType ) throws javax.jcr.ValueFormatException, RepositoryException {
            PropertyInfo existingMixinProperty = node.getProperty(JcrLexicon.MIXIN_TYPES);

            // getProperty(JcrLexicon.MIXIN_TYPES);
            Value[] existingMixinValues;
            if (existingMixinProperty != null) {
                existingMixinValues = findJcrProperty(existingMixinProperty.getPropertyId()).getValues();
            } else {
                existingMixinValues = new Value[0];
            }

            Value[] newMixinValues = new Value[existingMixinValues.length + 1];
            System.arraycopy(existingMixinValues, 0, newMixinValues, 0, existingMixinValues.length);
            newMixinValues[newMixinValues.length - 1] = new JcrValue(factories(), SessionCache.this, PropertyType.NAME,
                                                                     mixinCandidateType.getInternalName());

            findJcrProperty(setProperty(JcrLexicon.MIXIN_TYPES, newMixinValues, PropertyType.NAME, false));

            // ------------------------------------------------------------------------------
            // Create any auto-created properties/nodes from new type
            // ------------------------------------------------------------------------------

            for (JcrPropertyDefinition propertyDefinition : mixinCandidateType.propertyDefinitions()) {
                if (propertyDefinition.isAutoCreated() && !propertyDefinition.isProtected()) {
                    if (null == findJcrProperty(new PropertyId(node.getUuid(), propertyDefinition.getInternalName()))) {
                        assert propertyDefinition.getDefaultValues() != null;
                        if (propertyDefinition.isMultiple()) {
                            setProperty(propertyDefinition.getInternalName(),
                                        propertyDefinition.getDefaultValues(),
                                        propertyDefinition.getRequiredType());
                        } else {
                            assert propertyDefinition.getDefaultValues().length == 1;
                            setProperty(propertyDefinition.getInternalName(), (JcrValue)propertyDefinition.getDefaultValues()[0]);
                        }
                    }
                }
            }

            for (JcrNodeDefinition nodeDefinition : mixinCandidateType.childNodeDefinitions()) {
                if (nodeDefinition.isAutoCreated() && !nodeDefinition.isProtected()) {
                    Name nodeName = nodeDefinition.getInternalName();
                    if (!node.getChildren().getChildren(nodeName).hasNext()) {
                        assert nodeDefinition.getDefaultPrimaryType() != null;
                        createChild(nodeName, (UUID)null, ((JcrNodeType)nodeDefinition.getDefaultPrimaryType()).getInternalName());
                    }
                }
            }

        }

        /**
         * Create a new node as a child of this node, using the supplied name and (optionally) the supplied UUID.
         * 
         * @param name the name for the new child; may not be null
         * @param desiredUuid the desired UUID, or null if the UUID for the child should be generated automatically
         * @param primaryTypeName the name of the primary type for the new node
         * @return the representation of the newly-created child, which includes the {@link ChildNode#getSnsIndex()
         *         same-name-sibling index}
         * @throws InvalidItemStateException if the specified child has been marked for deletion within this session
         * @throws ConstraintViolationException if moving the node into this node violates this node's definition
         * @throws NoSuchNodeTypeException if the node type for the primary type could not be found
         * @throws AccessDeniedException if the current session does not have the requisite privileges to perform this task
         * @throws RepositoryException if any other error occurs while reading information from the repository
         */
        public ChildNode createChild( Name name,
                                      UUID desiredUuid,
                                      Name primaryTypeName )
            throws InvalidItemStateException, ConstraintViolationException, AccessDeniedException, RepositoryException {

            SessionCache.this.checkPermission(node, JcrSession.JCR_ADD_NODE_PERMISSION);

            if (desiredUuid == null) desiredUuid = UUID.randomUUID();

            // Verify that this node accepts a child of the supplied name (given any existing SNS nodes) ...
            int numSns = node.getChildren().getCountOfSameNameSiblingsWithName(name) + 1;
            JcrNodeDefinition definition = nodeTypes().findChildNodeDefinition(node.getPrimaryTypeName(),
                                                                               node.getMixinTypeNames(),
                                                                               name,
                                                                               primaryTypeName,
                                                                               numSns,
                                                                               true);
            // Make sure there was a valid child node definition ...
            if (definition == null) {

                // Check if the definition would have worked with less SNS
                definition = nodeTypes().findChildNodeDefinition(node.getPrimaryTypeName(),
                                                                 node.getMixinTypeNames(),
                                                                 name,
                                                                 primaryTypeName,
                                                                 numSns - 1,
                                                                 true);
                if (definition != null) {
                    // Only failed because there was no SNS definition - throw ItemExistsException per 7.1.4 of 1.0.1 spec
                    Path pathForChild = pathFactory.create(getPathFor(node), name, numSns + 1);
                    String msg = JcrI18n.noSnsDefinitionForNode.text(pathForChild, workspaceName());
                    throw new ItemExistsException(msg);
                }
                // Didn't work for other reasons - throw ConstraintViolationException
                Path pathForChild = pathFactory.create(getPathFor(node), name, numSns + 1);
                String msg = JcrI18n.nodeDefinitionCouldNotBeDeterminedForNode.text(pathForChild, workspaceName());
                throw new ConstraintViolationException(msg);
            }

            // Find the primary type ...
            JcrNodeType primaryType = null;
            if (primaryTypeName != null) {
                primaryType = nodeTypes().getNodeType(primaryTypeName);
                if (primaryType == null) {
                    Path pathForChild = pathFactory.create(getPathFor(node), name, numSns + 1);
                    I18n msg = JcrI18n.unableToCreateNodeWithPrimaryTypeThatDoesNotExist;
                    throw new NoSuchNodeTypeException(msg.text(primaryTypeName, pathForChild, workspaceName()));
                }
            } else {
                primaryType = (JcrNodeType)definition.getDefaultPrimaryType();
                if (primaryType == null) {
                    // There is no default primary type ...
                    Path pathForChild = pathFactory.create(getPathFor(node), name, numSns + 1);
                    I18n msg = JcrI18n.unableToCreateNodeWithNoDefaultPrimaryTypeOnChildNodeDefinition;
                    String nodeTypeName = definition.getDeclaringNodeType().getName();
                    throw new NoSuchNodeTypeException(msg.text(definition.getName(), nodeTypeName, pathForChild, workspaceName()));
                }
            }
            primaryTypeName = primaryType.getInternalName();

            ChildNode result = node.addChild(name, desiredUuid, pathFactory);

            // ---------------------------------------------------------
            // Now create the child node representation in the cache ...
            // ---------------------------------------------------------
            Path newPath = pathFactory.create(currentLocation.getPath(), result.getSegment());
            Location location = Location.create(newPath, desiredUuid);

            // Create the properties ...
            Map<Name, PropertyInfo> properties = new HashMap<Name, PropertyInfo>();
            Property primaryTypeProp = propertyFactory.create(JcrLexicon.PRIMARY_TYPE, primaryTypeName);
            Property nodeDefinitionProp = propertyFactory.create(DnaIntLexicon.NODE_DEFINITON, definition.getId().getString());

            // Now add the "jcr:uuid" property if and only if referenceable ...
            if (primaryType.isNodeType(JcrMixLexicon.REFERENCEABLE)) {
                if (desiredUuid == null) {
                    desiredUuid = UUID.randomUUID();
                }

                // We know that this property is single-valued
                JcrValue value = new JcrValue(factories(), SessionCache.this, PropertyType.STRING, desiredUuid.toString());
                PropertyDefinition propertyDefinition = nodeTypes().findPropertyDefinition(primaryTypeName,
                                                                                           Collections.<Name>emptyList(),
                                                                                           JcrLexicon.UUID,
                                                                                           value,
                                                                                           false,
                                                                                           false);
                PropertyId propId = new PropertyId(desiredUuid, JcrLexicon.UUID);
                JcrPropertyDefinition defn = (JcrPropertyDefinition)propertyDefinition;
                org.jboss.dna.graph.property.Property uuidProperty = propertyFactory.create(JcrLexicon.UUID, desiredUuid);
                PropertyInfo propInfo = new PropertyInfo(propId, defn.getId(), PropertyType.STRING, uuidProperty,
                                                         defn.isMultiple(), true, false);
                properties.put(JcrLexicon.UUID, propInfo);
            }

            // Create the property info for the "jcr:primaryType" child property ...
            JcrPropertyDefinition primaryTypeDefn = findBestPropertyDefintion(node.getPrimaryTypeName(),
                                                                              node.getMixinTypeNames(),
                                                                              primaryTypeProp,
                                                                              PropertyType.NAME,
                                                                              true,
                                                                              false);
            PropertyDefinitionId primaryTypeDefinitionId = primaryTypeDefn.getId();
            PropertyInfo primaryTypeInfo = new PropertyInfo(new PropertyId(desiredUuid, primaryTypeProp.getName()),
                                                            primaryTypeDefinitionId, PropertyType.NAME, primaryTypeProp, false,
                                                            true, false);
            properties.put(primaryTypeProp.getName(), primaryTypeInfo);

            // Create the property info for the "dna:nodeDefinition" child property ...
            JcrPropertyDefinition nodeDefnDefn = findBestPropertyDefintion(node.getPrimaryTypeName(),
                                                                           node.getMixinTypeNames(),
                                                                           nodeDefinitionProp,
                                                                           PropertyType.STRING,
                                                                           false,
                                                                           false);
            if (nodeDefnDefn != null) {
                PropertyDefinitionId nodeDefnDefinitionId = nodeDefnDefn.getId();
                PropertyInfo nodeDefinitionInfo = new PropertyInfo(new PropertyId(desiredUuid, nodeDefinitionProp.getName()),
                                                                   nodeDefnDefinitionId, PropertyType.STRING, nodeDefinitionProp,
                                                                   true, true, false);
                properties.put(nodeDefinitionProp.getName(), nodeDefinitionInfo);
            }

            // Now create the child node info, putting it in the changed map (and not the cache map!) ...
            NewNodeInfo newInfo = new NewNodeInfo(location, primaryTypeName, definition.getId(), node.getUuid(), properties);
            changedNodes.put(desiredUuid, newInfo);

            // ---------------------------------------
            // Now record the changes to the store ...
            // ---------------------------------------
            Graph.Create<Graph.Batch> create = operations.createUnder(currentLocation)
                                                         .nodeNamed(name)
                                                         .with(desiredUuid)
                                                         .with(primaryTypeProp);
            if (nodeDefnDefn != null) {
                create = create.with(nodeDefinitionProp);
            }
            create.and();
            return result;
        }

        /**
         * Destroy the child node with the supplied UUID and all nodes that exist below it, including any nodes that were created
         * and haven't been persisted.
         * 
         * @param nodeUuid the UUID of the child node; may not be null
         * @throws AccessDeniedException if the current session does not have the requisite privileges to perform this task
         * @throws RepositoryException if any other error occurs
         * @return true if the child was successfully removed, or false if the node did not exist as a child
         */
        public boolean destroyChild( UUID nodeUuid ) throws AccessDeniedException, RepositoryException {
            SessionCache.this.checkPermission(node, JcrSession.JCR_REMOVE_PERMISSION);

            ChildNode deleted = node.removeChild(nodeUuid, pathFactory);

            if (deleted != null) {
                // Recursively mark the cached/changed information as deleted ...
                deleteNodeInfos(nodeUuid);

                // Now make the request to the source ...
                Path childPath = pathFactory.create(currentLocation.getPath(), deleted.getSegment());
                Location locationOfChild = Location.create(childPath, nodeUuid);
                operations.delete(locationOfChild);
                return true;
            }
            return false;
        }

        protected boolean isAncestor( UUID uuid ) throws ItemNotFoundException, InvalidItemStateException, RepositoryException {
            UUID ancestor = node.getParent();
            while (ancestor != null) {
                if (ancestor.equals(uuid)) return true;
                NodeInfo info = findNodeInfo(ancestor);
                ancestor = info.getParent();
            }
            return false;
        }
    }

    /**
     * Find the best property definition in this node's primary type and mixin types.
     * 
     * @param primaryTypeNameOfParent the name of the primary type for the parent node; may not be null
     * @param mixinTypeNamesOfParent the names of the mixin types for the parent node; may be null or empty if there are no mixins
     *        to include in the search
     * @param dnaProperty the new property that is to be set on this node
     * @param propertyType the property type; must be a valid {@link PropertyType} value
     * @param isSingle true if the property definition should be single-valued, or false if the property definition should allow
     *        multiple values
     * @param skipProtected true if this operation is being done from within the public JCR node and property API, or false if
     *        this operation is being done from within internal implementations
     * @return the property definition that allows setting this property, or null if there is no such definition
     */
    protected JcrPropertyDefinition findBestPropertyDefintion( Name primaryTypeNameOfParent,
                                                               List<Name> mixinTypeNamesOfParent,
                                                               Property dnaProperty,
                                                               int propertyType,
                                                               boolean isSingle,
                                                               boolean skipProtected ) {
        JcrPropertyDefinition definition = null;
        if (propertyType == PropertyType.UNDEFINED) {
            propertyType = PropertyTypeUtil.jcrPropertyTypeFor(dnaProperty);
        }

        // If single-valued ...
        if (isSingle) {
            // Create a value for the DNA property value ...
            Object value = dnaProperty.getFirstValue();
            Value jcrValue = new JcrValue(factories(), SessionCache.this, propertyType, value);
            definition = nodeTypes().findPropertyDefinition(primaryTypeNameOfParent,
                                                            mixinTypeNamesOfParent,
                                                            dnaProperty.getName(),
                                                            jcrValue,
                                                            true,
                                                            skipProtected);
        } else {
            // Create values for the DNA property value ...
            Value[] jcrValues = new Value[dnaProperty.size()];
            int index = 0;
            for (Object value : dnaProperty) {
                jcrValues[index++] = new JcrValue(factories(), SessionCache.this, propertyType, value);
            }
            definition = nodeTypes().findPropertyDefinition(primaryTypeNameOfParent,
                                                            mixinTypeNamesOfParent,
                                                            dnaProperty.getName(),
                                                            jcrValues,
                                                            skipProtected);
        }

        if (definition != null) return definition;

        // No definition that allowed the values ...
        return null;
    }

    /**
     * Utility method that creates and caches the appropriate kind of AbstractJcrNode implementation for node given by the
     * supplied information.
     * 
     * @param info the information for the node; may not be null
     * @return the <i>new</i> instance of the {@link Node}; never null
     */
    private AbstractJcrNode createAndCacheJcrNodeFor( NodeInfo info ) {
        UUID uuid = info.getUuid();
        Location location = info.getOriginalLocation();
        NodeDefinitionId nodeDefinitionId = info.getDefinitionId();
        JcrNodeDefinition definition = nodeTypes().getNodeDefinition(nodeDefinitionId);
        assert definition != null;

        // Need to determine if this is the root node ...
        if (location.getPath().isRoot()) {
            // It is a root node ...
            JcrRootNode node = new JcrRootNode(this, uuid);
            jcrNodes.put(uuid, node);
            root = uuid;
            return node;
        }

        // It is not a root node ...
        JcrNode node = new JcrNode(this, uuid);
        assert !uuid.equals(root);
        jcrNodes.put(uuid, node);
        return node;
    }

    /**
     * Utility method that creates and caches the appropriate kind of AbstractJcrProperty implementation for property given by the
     * supplied information.
     * 
     * @param info the information for the property; may not be null
     * @return the <i>new</i> instance of the {@link Property}; never null
     */
    private AbstractJcrProperty createAndCacheJcrPropertyFor( PropertyInfo info ) {
        boolean multiValued = info.isMultiValued();
        JcrPropertyDefinition definition = nodeTypes().getPropertyDefinition(info.getDefinitionId());
        assert definition != null;
        if (multiValued) {
            return new JcrMultiValueProperty(this, info.getPropertyId());
        }
        return new JcrSingleValueProperty(this, info.getPropertyId());
    }

    /**
     * Find the information for the node given by the UUID of the node. This is often the fastest way to find information,
     * especially after the information has been cached. Note, however, that this method first checks the cache, and if the
     * information is not in the cache, the information is read from the repository.
     * 
     * @param uuid the UUID for the node; may not be null
     * @return the information for the node with the supplied UUID, or null if the information is not in the cache
     * @throws AccessDeniedException if the node cannot be accessed
     * @throws ItemNotFoundException if there is no node with the supplied UUID
     * @throws InvalidItemStateException if the node with the UUID has been deleted in this session
     * @throws RepositoryException if any other error occurs while reading information from the repository
     * @see #findNodeInfoInCache(UUID)
     * @see #findNodeInfo(UUID, Path)
     * @see #findNodeInfoForRoot()
     */
    NodeInfo findNodeInfo( UUID uuid )
        throws AccessDeniedException, ItemNotFoundException, InvalidItemStateException, RepositoryException {
        assert uuid != null;
        // See if we already have something in the cache ...
        NodeInfo info = findNodeInfoInCache(uuid);
        if (info == null) {
            // Nope, so go ahead and load it ...
            info = loadFromGraph(null, uuid);

        }
        SessionCache.this.checkPermission(info, JcrSession.JCR_READ_PERMISSION);

        return info;
    }

    /**
     * Find the information for the node given by the UUID of the node. This is often the fastest way to find information,
     * especially after the information has been cached. Note, however, that this method only checks the cache.
     * 
     * @param uuid the UUID for the node; may not be null
     * @return the information for the node with the supplied UUID, or null if the information is not in the cache
     * @see #findNodeInfo(UUID)
     * @see #findNodeInfo(UUID, Path)
     * @see #findNodeInfoForRoot()
     * @throws InvalidItemStateException if the node with the UUID has been deleted in this session
     */
    NodeInfo findNodeInfoInCache( UUID uuid ) throws InvalidItemStateException {
        // See if we already have something in the changed nodes ...
        NodeInfo info = changedNodes.get(uuid);
        if (info == null) {
            // Or in the cache ...
            info = cachedNodes.get(uuid);
            if (info == null) {
                // Finally check if the node was deleted ...
                if (deletedNodes.containsKey(uuid)) {
                    throw new InvalidItemStateException();
                }
            }
        }
        return info;
    }

    /**
     * Find the information for the root node. Generally, this returns information that's in the cache, except for the first time
     * the root node is needed.
     * 
     * @return the node information
     * @throws RepositoryException if there is an error while obtaining the information from the repository
     */
    NodeInfo findNodeInfoForRoot() throws RepositoryException {
        if (root == null) {
            // We haven't found the root yet ...
            NodeInfo info = loadFromGraph(this.rootPath, (NodeInfo)null);
            root = info.getUuid();
            this.jcrNodes.put(root, new JcrRootNode(this, root));
            return info;
        }
        return findNodeInfo(root);
    }

    /**
     * Find the information for the node given by the UUID of a reference node and a relative path from the reference node.
     * 
     * @param node the reference node; may be null if the root node is to be used as the reference
     * @param relativePath the relative path from the reference node, but which may be an absolute path only when the reference
     *        node is the root node; may not be null
     * @return the information for the referenced node; never null
     * @throws ItemNotFoundException if the reference node with the supplied UUID does not exist
     * @throws PathNotFoundException if the node given by the relative path does not exist
     * @throws InvalidItemStateException if the node with the UUID has been deleted in this session
     * @throws RepositoryException if any other error occurs while reading information from the repository
     * @see #findNodeInfoForRoot()
     */
    NodeInfo findNodeInfo( UUID node,
                           Path relativePath )
        throws ItemNotFoundException, InvalidItemStateException, PathNotFoundException, RepositoryException {
        // The relative path must be normalized ...
        assert relativePath.isNormalized();

        // Find the node from which we're starting ...
        NodeInfo fromInfo = null;
        if (node == null) {
            // This is only valid when the path is relative to the root (or it's an absolute path)
            fromInfo = findNodeInfoForRoot();
            node = fromInfo.getUuid();
        } else {
            fromInfo = findNodeInfo(node);
            assert relativePath.isAbsolute() ? node == root : true;
        }
        if (relativePath.isAbsolute()) {
            relativePath = relativePath.relativeTo(this.rootPath);
        }

        // If the relative path is of zero-length ...
        if (relativePath.size() == 0) {
            SessionCache.this.checkPermission(fromInfo, JcrSession.JCR_READ_PERMISSION);
            return fromInfo;
        }
        // Or it is of length 1 but it is a self reference ...
        if (relativePath.size() == 1 && relativePath.getLastSegment().isSelfReference()) {
            SessionCache.this.checkPermission(fromInfo, JcrSession.JCR_READ_PERMISSION);
            return fromInfo;
        }

        // TODO: This could be more efficient than always walking the path. For example, we could
        // maintain a cache of paths. Right now, we are walking as much of the path as we can,
        // but as soon as we reach the bottom-most cached/changed node, we need to read the rest
        // from the graph. We are figuring out all of the remaining nodes and read them from
        // the graph in one batch operation, so that part is pretty good.

        // Now, walk the path to find the nodes, being sure to look for changed information ...
        NodeInfo info = fromInfo;
        Iterator<Path.Segment> pathsIter = relativePath.iterator();
        while (pathsIter.hasNext()) {
            Path.Segment child = pathsIter.next();
            if (child.isParentReference()) {
                // Walk up ...
                UUID parentUuid = info.getParent();
                if (parentUuid == null) {
                    assert info.getUuid() == findNodeInfoForRoot().getUuid();
                    String msg = JcrI18n.errorWhileFindingNodeWithPath.text(relativePath, workspaceName);
                    throw new PathNotFoundException(msg);
                }
                info = findNodeInfo(parentUuid);
            } else {
                // Walk down ...
                // Note that once we start walking down, a normalized path should never have any more parent
                // or self references
                ChildNode childNodeInfo = info.getChildren().getChild(child);
                if (childNodeInfo == null) {
                    // The node (no longer?) exists, so compute the
                    Path fromPath = getPathFor(fromInfo);
                    String msg = JcrI18n.pathNotFoundRelativeTo.text(relativePath, fromPath, workspaceName);
                    throw new PathNotFoundException(msg);
                }
                // See if we already have something in the changed nodes ...
                UUID uuid = childNodeInfo.getUuid();
                NodeInfo childInfo = changedNodes.get(uuid);
                if (childInfo == null) {
                    // Or in the cache ...
                    childInfo = cachedNodes.get(uuid);
                    if (childInfo == null) {
                        // At this point, we've reached the bottom of the nodes that we have locally.
                        // Get the actual location of the last 'info', since all paths will be relative to it...
                        Location actualLocation = info.getOriginalLocation();
                        Path actualPath = actualLocation.getPath();
                        Path nextPath = pathFactory.create(actualPath, child);
                        if (pathsIter.hasNext()) {
                            // There are multiple remaining paths, so load them all in one batch operation,
                            // starting at the top-most path (the one we're currently at)...
                            List<Path> pathsInBatch = new LinkedList<Path>();
                            Results batchResults = null;
                            try {
                                Graph.Batch batch = store.batch();
                                batch.read(nextPath);
                                pathsInBatch.add(nextPath);
                                while (pathsIter.hasNext()) {
                                    child = pathsIter.next();
                                    nextPath = pathFactory.create(nextPath, child);
                                    batch.read(nextPath);
                                    pathsInBatch.add(nextPath);
                                }
                                batchResults = batch.execute();
                            } catch (org.jboss.dna.graph.property.PathNotFoundException e) {
                                Path fromPath = getPathFor(fromInfo);
                                throw new PathNotFoundException(JcrI18n.pathNotFoundRelativeTo.text(relativePath,
                                                                                                    fromPath,
                                                                                                    workspaceName));
                            } catch (RepositorySourceException e) {
                                throw new RepositoryException(JcrI18n.errorWhileFindingNodeWithUuid.text(uuid,
                                                                                                         workspaceName,
                                                                                                         e.getLocalizedMessage()));
                            }
                            // Now process all of the nodes that we loaded, again starting at the top and going down ...
                            for (Path batchPath : pathsInBatch) {
                                org.jboss.dna.graph.Node dnaNode = batchResults.getNode(batchPath);
                                ImmutableNodeInfo originalChildInfo = createNodeInfoFrom(dnaNode, info);
                                this.cachedNodes.put(originalChildInfo.getUuid(), originalChildInfo);
                                childInfo = originalChildInfo;
                                info = originalChildInfo;
                            }
                        } else {
                            // This is the last path, so do it a little more efficiently than above ...
                            childInfo = loadFromGraph(nextPath, info);
                            info = childInfo;
                        }
                    } else {
                        info = childInfo;
                    }
                } else {
                    info = childInfo;
                }
            }
        }
        SessionCache.this.checkPermission(info, JcrSession.JCR_READ_PERMISSION);
        return info;
    }

    /**
     * Find the property information with the given identifier. If the property is not yet loaded into the cache, the node (and
     * its properties) will be read from the repository.
     * 
     * @param propertyId the identifier for the property; may not be null
     * @return the property information, or null if the node does not contain the specified property
     * @throws PathNotFoundException if the node containing this property does not exist
     * @throws InvalidItemStateException if the node with the UUID has been deleted in this session
     * @throws RepositoryException if there is an error while obtaining the information
     */
    PropertyInfo findPropertyInfo( PropertyId propertyId )
        throws PathNotFoundException, InvalidItemStateException, RepositoryException {
        NodeInfo info = findNodeInfo(propertyId.getNodeId());
        return info.getProperty(propertyId.getPropertyName());
    }

    Path getPathFor(String workspaceName, UUID uuid, Path relativePath) throws NoSuchWorkspaceException, ItemNotFoundException, RepositoryException {
        assert workspaceName != null;
        assert uuid != null || relativePath != null;
        
        Graph graph = operations.getGraph();
        try {
            graph.useWorkspace(workspaceName);
        }
        catch (InvalidWorkspaceException iwe) {
            throw new NoSuchWorkspaceException(JcrI18n.workspaceNameIsInvalid.text(graph.getSourceName(), workspaceName));
        }
        
        try {
            org.jboss.dna.graph.Node node;
            if (uuid != null) {
                node = graph.getNodeAt(uuid);

                if (relativePath != null) {
                    Path nodePath = node.getLocation().getPath();
                    Path absolutePath = relativePath.resolveAgainst(nodePath);
                    node = graph.getNodeAt(absolutePath);       
                }
                
            }
            else {
                Path absolutePath = pathFactory.createAbsolutePath(relativePath.getSegmentsList());
                node = graph.getNodeAt(absolutePath);       
            }
            assert node != null;
            
            return node.getLocation().getPath();
        }
        catch (org.jboss.dna.graph.property.PathNotFoundException pnfe) {
            throw new ItemNotFoundException(pnfe);
        }
        finally {
            graph.useWorkspace(this.workspaceName);
        }
        
    }
    
    Path getPathFor( UUID uuid ) throws ItemNotFoundException, InvalidItemStateException, RepositoryException {
        if (uuid.equals(root)) return rootPath;
        return getPathFor(findNodeInfo(uuid));
    }

    Path getPathFor( NodeInfo info ) throws ItemNotFoundException, InvalidItemStateException, RepositoryException {
        if (info == null) {
            return pathFactory.createRootPath();
        }
        UUID uuid = info.getUuid();
        if (uuid.equals(root)) return rootPath;

        // This isn't the root node ...
        Path result = pathCache.get(uuid);
        if (result == null) {
            // We need to build a path using the parent path ...
            UUID parent = info.getParent();
            if (parent == null) {
                // Then this node is the root ...
                root = info.getUuid();
                result = rootPath;
            } else {
                NodeInfo parentInfo = findNodeInfo(parent);
                Path parentPath = getPathFor(parentInfo);
                ChildNode child = parentInfo.getChildren().getChild(info.getUuid());
                result = pathFactory.create(parentPath, child.getSegment());
            }
            pathCache.put(uuid, result);
        }
        assert result != null;
        return result;
    }

    Path getPathFor( PropertyInfo propertyInfo ) throws ItemNotFoundException, RepositoryException {
        Path nodePath = getPathFor(propertyInfo.getNodeUuid());
        return pathFactory.create(nodePath, propertyInfo.getPropertyName());
    }

    Path getPathFor( PropertyId propertyId ) throws ItemNotFoundException, RepositoryException {
        return getPathFor(findPropertyInfo(propertyId));
    }

    protected Name getNameOf( UUID nodeUuid ) throws ItemNotFoundException, InvalidItemStateException, RepositoryException {
        findNodeInfoForRoot();
        if (nodeUuid == root) return nameFactory.create("");
        // Get the parent ...
        NodeInfo info = findNodeInfo(nodeUuid);
        NodeInfo parent = findNodeInfo(info.getParent());
        ChildNode child = parent.getChildren().getChild(info.getUuid());
        return child.getName();
    }

    protected int getSnsIndexOf( UUID nodeUuid ) throws ItemNotFoundException, InvalidItemStateException, RepositoryException {
        findNodeInfoForRoot();
        if (nodeUuid == root) return 1;
        // Get the parent ...
        NodeInfo info = findNodeInfo(nodeUuid);
        NodeInfo parent = findNodeInfo(info.getParent());
        ChildNode child = parent.getChildren().getChild(info.getUuid());
        return child.getSnsIndex();
    }

    /**
     * Load from the underlying repository graph the information for the node with the supplied UUID. This method returns the
     * information for the requested node (after placing it in the cache), but this method may (at its discretion) also load and
     * cache information for other nodes.
     * <p>
     * Note that this method does not check the cache before loading from the repository graph.
     * </p>
     * 
     * @param path the path of the node, if known; may be null only if the UUID is supplied
     * @param uuid the UUID of the node, if known; may be null only if the path is supplied
     * @return the information for the node
     * @throws ItemNotFoundException if the node does not exist in the repository
     * @throws RepositoryException if there was an error obtaining this information from the repository
     */
    protected ImmutableNodeInfo loadFromGraph( Path path,
                                               UUID uuid ) throws ItemNotFoundException, RepositoryException {
        // Load the node information from the store ...
        try {
            // See if there is a path for this uuid ...
            Location location = Location.create(path, uuid);
            org.jboss.dna.graph.Node node = store.getNodeAt(location);
            ImmutableNodeInfo info = createNodeInfoFrom(node, null);
            this.cachedNodes.put(info.getUuid(), info);
            return info;
        } catch (org.jboss.dna.graph.property.PathNotFoundException e) {
            throw new ItemNotFoundException(JcrI18n.itemNotFoundWithUuid.text(uuid, workspaceName, e.getLocalizedMessage()));
        } catch (RepositorySourceException e) {
            throw new RepositoryException(
                                          JcrI18n.errorWhileFindingNodeWithUuid.text(uuid, workspaceName, e.getLocalizedMessage()),
                                          e);
        }
    }

    /**
     * Load from the underlying repository graph the information for the node with the supplied UUID. This method returns the
     * information for the requested node (after placing it in the cache), but this method may (at its discretion) also load and
     * cache information for other nodes.
     * <p>
     * Note that this method does not check the cache before loading from the repository graph.
     * </p>
     * 
     * @param path the path to the node; may not be null
     * @param parentInfo the parent information; may be null if not known
     * @return the information for the node
     * @throws PathNotFoundException if the node does not exist in the repository
     * @throws RepositoryException if there was an error obtaining this information from the repository
     */
    protected ImmutableNodeInfo loadFromGraph( Path path,
                                               NodeInfo parentInfo ) throws PathNotFoundException, RepositoryException {
        // Load the node information from the store ...
        try {
            org.jboss.dna.graph.Node node = store.getNodeAt(path);
            ImmutableNodeInfo info = createNodeInfoFrom(node, parentInfo);
            this.cachedNodes.put(info.getUuid(), info);
            return info;
        } catch (org.jboss.dna.graph.property.PathNotFoundException e) {
            throw new PathNotFoundException(JcrI18n.pathNotFound.text(path, workspaceName));
        } catch (RepositorySourceException e) {
            throw new RepositoryException(JcrI18n.errorWhileFindingNodeWithPath.text(path, workspaceName));
        }
    }

    /**
     * Create the {@link NodeInfo} object given the DNA graph node and the parent node information (if it is available).
     * 
     * @param graphNode the DNA graph node; may not be null
     * @param parentInfo the information for the parent node, or null if the supplied graph node represents the root node, or if
     *        the parent information is not known
     * @return the node information; never null
     * @throws RepositoryException if there is an error determining the child {@link NodeDefinition} for the supplied node,
     *         preventing the node information from being constructed
     */
    private ImmutableNodeInfo createNodeInfoFrom( org.jboss.dna.graph.Node graphNode,
                                                  NodeInfo parentInfo ) throws RepositoryException {
        // Now get the DNA node's UUID and find the DNA property containing the UUID ...
        Location location = graphNode.getLocation();
        UUID uuid = location.getUuid();
        org.jboss.dna.graph.property.Property uuidProperty = null;
        if (uuid != null) {
            // Check for an identification property ...
            uuidProperty = location.getIdProperty(JcrLexicon.UUID);
            if (uuidProperty == null) {
                uuidProperty = propertyFactory.create(JcrLexicon.UUID, uuid);
            }
        }
        if (uuidProperty == null) {
            uuidProperty = graphNode.getProperty(JcrLexicon.UUID);
            if (uuidProperty != null) {
                // Grab the first 'good' UUID value ...
                for (Object uuidValue : uuidProperty) {
                    try {
                        uuid = factories.getUuidFactory().create(uuidValue);
                        break;
                    } catch (ValueFormatException e) {
                        // Ignore; just continue with the next property value
                    }
                }
            }
            if (uuid == null) {
                // Look for the DNA UUID property ...
                org.jboss.dna.graph.property.Property dnaUuidProperty = graphNode.getProperty(DnaLexicon.UUID);
                if (dnaUuidProperty != null) {
                    // Grab the first 'good' UUID value ...
                    for (Object uuidValue : dnaUuidProperty) {
                        try {
                            uuid = factories.getUuidFactory().create(uuidValue);
                            break;
                        } catch (ValueFormatException e) {
                            // Ignore; just continue with the next property value
                        }
                    }
                }
            }
        }
        if (uuid == null) uuid = UUID.randomUUID();
        if (uuidProperty == null) uuidProperty = propertyFactory.create(JcrLexicon.UUID, uuid);

        // Either the UUID is not known, or there was no node. Either way, we have to create the node ...
        if (uuid == null) uuid = UUID.randomUUID();

        // Look for the primary type of the node ...
        Map<Name, Property> graphProperties = graphNode.getPropertiesByName();
        final boolean isRoot = location.getPath().isRoot();
        Name primaryTypeName = null;
        org.jboss.dna.graph.property.Property primaryTypeProperty = graphNode.getProperty(JcrLexicon.PRIMARY_TYPE);
        if (primaryTypeProperty != null && !primaryTypeProperty.isEmpty()) {
            try {
                primaryTypeName = factories.getNameFactory().create(primaryTypeProperty.getFirstValue());
            } catch (ValueFormatException e) {
                // use the default ...
            }
        }
        if (primaryTypeName == null) {
            // We have to have a primary type, so use the default ...
            if (isRoot) {
                primaryTypeName = DnaLexicon.ROOT;
                primaryTypeProperty = propertyFactory.create(JcrLexicon.PRIMARY_TYPE, primaryTypeName);
            } else {
                primaryTypeName = defaultPrimaryTypeName;
                primaryTypeProperty = defaultPrimaryTypeProperty;
            }
            // We have to add this property to the graph node...
            graphProperties = new HashMap<Name, Property>(graphProperties);
            graphProperties.put(primaryTypeProperty.getName(), primaryTypeProperty);
        }
        assert primaryTypeProperty != null;
        assert primaryTypeProperty.isEmpty() == false;

        // Look for a node definition stored on the node ...
        JcrNodeDefinition definition = null;
        org.jboss.dna.graph.property.Property nodeDefnProperty = graphProperties.get(DnaIntLexicon.NODE_DEFINITON);
        if (nodeDefnProperty != null && !nodeDefnProperty.isEmpty()) {
            String nodeDefinitionString = stringFactory.create(nodeDefnProperty.getFirstValue());
            NodeDefinitionId id = NodeDefinitionId.fromString(nodeDefinitionString, nameFactory);
            definition = nodeTypes().getNodeDefinition(id);
        }
        // Figure out the node definition for this node ...
        if (isRoot) {
            if (definition == null) definition = nodeTypes().getRootNodeDefinition();
        } else {
            // We need the parent ...
            Path path = location.getPath();
            if (parentInfo == null) {
                Path parentPath = path.getParent();
                parentInfo = findNodeInfo(null, parentPath.getNormalizedPath());
            }
            if (definition == null) {
                Name childName = path.getLastSegment().getName();
                int numExistingChildrenWithSameName = parentInfo.getChildren().getCountOfSameNameSiblingsWithName(childName);
                definition = nodeTypes().findChildNodeDefinition(parentInfo.getPrimaryTypeName(),
                                                                 parentInfo.getMixinTypeNames(),
                                                                 childName,
                                                                 primaryTypeName,
                                                                 numExistingChildrenWithSameName,
                                                                 false);
            }
            if (definition == null) {
                String msg = JcrI18n.nodeDefinitionCouldNotBeDeterminedForNode.text(path, workspaceName);
                throw new RepositorySourceException(sourceName(), msg);
            }
        }

        // ------------------------------------------------------
        // Set the node's properties ...
        // ------------------------------------------------------
        boolean referenceable = false;

        // Start with the primary type ...
        JcrNodeType primaryType = nodeTypes().getNodeType(primaryTypeName);
        if (primaryType == null) {
            Path path = location.getPath();
            String msg = JcrI18n.missingNodeTypeForExistingNode.text(primaryTypeName.getString(namespaces), path, workspaceName);
            throw new RepositorySourceException(sourceName(), msg);
        }
        if (primaryType.isNodeType(JcrMixLexicon.REFERENCEABLE)) referenceable = true;

        // The process the mixin types ...
        Property mixinTypesProperty = graphProperties.get(JcrLexicon.MIXIN_TYPES);
        List<Name> mixinTypeNames = null;
        if (mixinTypesProperty != null && !mixinTypesProperty.isEmpty()) {
            for (Object mixinTypeValue : mixinTypesProperty) {
                Name mixinTypeName = nameFactory.create(mixinTypeValue);
                if (mixinTypeNames == null) mixinTypeNames = new LinkedList<Name>();
                mixinTypeNames.add(mixinTypeName);
                JcrNodeType mixinType = nodeTypes().getNodeType(mixinTypeName);
                if (mixinType == null) continue;
                if (!referenceable && mixinType.isNodeType(JcrMixLexicon.REFERENCEABLE)) referenceable = true;
            }
        }

        // Create the set of multi-valued property names ...
        Set<Name> multiValuedPropertyNames = EMPTY_NAMES;
        Set<Name> newSingleMultiPropertyNames = null;
        Property multiValuedPropNamesProp = graphProperties.get(DnaIntLexicon.MULTI_VALUED_PROPERTIES);
        if (multiValuedPropNamesProp != null && !multiValuedPropNamesProp.isEmpty()) {
            multiValuedPropertyNames = getSingleMultiPropertyNames(multiValuedPropNamesProp, location.getPath(), uuid);
        }

        // Now create the JCR property object wrappers around the other properties ...
        Map<Name, PropertyInfo> props = new HashMap<Name, PropertyInfo>();
        for (Property dnaProp : graphProperties.values()) {
            Name name = dnaProp.getName();

            // Is this is single-valued property?
            boolean isSingle = dnaProp.isSingle();
            // Make sure that this isn't a multi-valued property with one value ...
            if (isSingle && multiValuedPropertyNames.contains(name)) isSingle = false;

            // Figure out the JCR property type for this property ...
            int propertyType = PropertyTypeUtil.jcrPropertyTypeFor(dnaProp);
            PropertyDefinition propertyDefinition = findBestPropertyDefintion(primaryTypeName,
                                                                              mixinTypeNames,
                                                                              dnaProp,
                                                                              propertyType,
                                                                              isSingle,
                                                                              false);

            // If there still is no property type defined ...
            if (propertyDefinition == null && INCLUDE_PROPERTIES_NOT_ALLOWED_BY_NODE_TYPE_OR_MIXINS) {
                // We can use the "nt:unstructured" property definitions for any property ...
                NodeType unstructured = nodeTypes().getNodeType(JcrNtLexicon.UNSTRUCTURED);
                for (PropertyDefinition anyDefinition : unstructured.getDeclaredPropertyDefinitions()) {
                    if (anyDefinition.isMultiple()) {
                        propertyDefinition = anyDefinition;
                        break;
                    }
                }
            }
            if (propertyDefinition == null) {
                // We're supposed to skip this property (since we don't have a definition for it) ...
                continue;
            }

            // Figure out if this is a multi-valued property ...
            boolean isMultiple = propertyDefinition.isMultiple();
            if (!isMultiple && dnaProp.isEmpty()) {
                // Only multi-valued properties can have no values; so if not multi-valued, then skip ...
                continue;
            }

            // Update the list of single-valued multi-property names ...
            if (isMultiple && isSingle) {
                if (newSingleMultiPropertyNames == null) newSingleMultiPropertyNames = new HashSet<Name>();
                newSingleMultiPropertyNames.add(name);
            }

            // Figure out the property type ...
            int definitionType = propertyDefinition.getRequiredType();
            if (definitionType != PropertyType.UNDEFINED) {
                propertyType = definitionType;
            }

            // Record the property in the node information ...
            PropertyId propId = new PropertyId(uuid, name);
            JcrPropertyDefinition defn = (JcrPropertyDefinition)propertyDefinition;
            PropertyInfo propInfo = new PropertyInfo(propId, defn.getId(), propertyType, dnaProp, defn.isMultiple(), false, false);
            props.put(name, propInfo);
        }

        // Now add the "jcr:uuid" property if and only if referenceable ...
        if (referenceable) {
            // We know that this property is single-valued
            JcrValue value = new JcrValue(factories(), this, PropertyType.STRING, uuid);
            PropertyDefinition propertyDefinition = nodeTypes().findPropertyDefinition(primaryTypeName,
                                                                                       mixinTypeNames,
                                                                                       JcrLexicon.UUID,
                                                                                       value,
                                                                                       false,
                                                                                       false);
            PropertyId propId = new PropertyId(uuid, JcrLexicon.UUID);
            JcrPropertyDefinition defn = (JcrPropertyDefinition)propertyDefinition;
            PropertyInfo propInfo = new PropertyInfo(propId, defn.getId(), PropertyType.STRING, uuidProperty, defn.isMultiple(),
                                                     false, false);
            props.put(JcrLexicon.UUID, propInfo);
        } else {
            // Make sure there is NOT a "jcr:uuid" property ...
            props.remove(JcrLexicon.UUID);
        }
        // Make sure the "dna:uuid" property did not get in there ...
        props.remove(DnaLexicon.UUID);

        // Make sure the single-valued multi-property names are stored as a property ...
        if (newSingleMultiPropertyNames != null) {
            PropertyInfo info = createSingleMultiplePropertyInfo(uuid,
                                                                 primaryTypeName,
                                                                 mixinTypeNames,
                                                                 newSingleMultiPropertyNames);
            props.put(info.getPropertyName(), info);
        }

        // Create the node information ...
        UUID parentUuid = parentInfo != null ? parentInfo.getUuid() : null;
        List<Location> locations = graphNode.getChildren();
        Children children = locations.isEmpty() ? new EmptyChildren(parentUuid) : new ImmutableChildren(parentUuid, locations);
        props = Collections.unmodifiableMap(props);
        return new ImmutableNodeInfo(location, primaryTypeName, mixinTypeNames, definition.getId(), parentUuid, children, props);
    }

    protected final PropertyInfo createSingleMultiplePropertyInfo( UUID uuid,
                                                                   Name primaryTypeName,
                                                                   List<Name> mixinTypeNames,
                                                                   Set<Name> newSingleMultiPropertyNames ) {
        int number = newSingleMultiPropertyNames.size();
        String[] names = new String[number];
        JcrValue[] values = new JcrValue[number];
        if (number == 1) {
            String str = newSingleMultiPropertyNames.iterator().next().getString(namespaces);
            names[0] = str;
            values[0] = new JcrValue(factories(), this, PropertyType.STRING, str);
        } else {
            int index = 0;
            for (Name name : newSingleMultiPropertyNames) {
                String str = name.getString(namespaces);
                names[index] = str;
                values[index] = new JcrValue(factories(), this, PropertyType.STRING, str);
                ++index;
            }
        }
        PropertyDefinition propertyDefinition = nodeTypes().findPropertyDefinition(primaryTypeName,
                                                                                   mixinTypeNames,
                                                                                   DnaIntLexicon.MULTI_VALUED_PROPERTIES,
                                                                                   values,
                                                                                   false);
        Property dnaProp = propertyFactory.create(DnaIntLexicon.MULTI_VALUED_PROPERTIES, newSingleMultiPropertyNames.iterator()
                                                                                                                    .next());
        PropertyId propId = new PropertyId(uuid, dnaProp.getName());
        JcrPropertyDefinition defn = (JcrPropertyDefinition)propertyDefinition;
        return new PropertyInfo(propId, defn.getId(), PropertyType.STRING, dnaProp, defn.isMultiple(), true, false);
    }

    protected final Set<Name> getSingleMultiPropertyNames( Property dnaProperty,
                                                           Path knownPath,
                                                           UUID knownUuid ) {
        Set<Name> multiValuedPropertyNames = new HashSet<Name>();
        for (Object value : dnaProperty) {
            try {
                multiValuedPropertyNames.add(nameFactory.create(value));
            } catch (ValueFormatException e) {
                String msg = "{0} value \"{1}\" on {2} in \"{3}\" workspace is not a valid name and is being ignored";
                String path = null;
                if (knownPath != null) {
                    path = knownPath.getString(namespaces);
                } else {
                    assert knownUuid != null;
                    try {
                        path = getPathFor(knownUuid).getString(namespaces);
                    } catch (RepositoryException err) {
                        path = knownUuid.toString();
                    }
                }
                Logger.getLogger(getClass()).trace(e,
                                                   msg,
                                                   DnaIntLexicon.MULTI_VALUED_PROPERTIES.getString(namespaces),
                                                   value,
                                                   path,
                                                   workspaceName());
            }
        }
        return multiValuedPropertyNames;
    }

    /**
     * This method finds the {@link NodeInfo} for the node with the supplied UUID and marks it as being deleted, and does the same
     * for all decendants (e.g., children, grandchildren, great-grandchildren, etc.) that have been cached or changed.
     * <p>
     * Note that this method only processes those nodes that are actually represented in this cache. Any branches that are not
     * loaded are not processed. This is an acceptable assumption, since all ancestors of a cached node should also be cached.
     * </p>
     * <p>
     * Also be aware that the returned count of deleted node info representations will only reflect the total number of nodes in
     * the branch if and only if all branch nodes were cached. In all other cases, the count returned will be fewer than the
     * number of actual nodes in the branch.
     * </p>
     * 
     * @param uuid the UUID of the node that should be marked as deleted; may not be null
     * @return the number of node info representations that were marked as deleted
     */
    protected int deleteNodeInfos( UUID uuid ) {
        Queue<UUID> nodesToDelete = new LinkedList<UUID>();
        int numDeleted = 0;
        nodesToDelete.add(uuid);
        while (!nodesToDelete.isEmpty()) {
            UUID toDelete = nodesToDelete.remove();
            // Remove the node info from the changed map ...
            NodeInfo info = changedNodes.remove(toDelete);
            if (info == null) {
                // Wasn't changed, so remove it from the cache map ...
                info = cachedNodes.remove(toDelete);
            }
            // Whether or not we found an info, add it to the deleted map ...
            this.deletedNodes.put(toDelete, info);

            // Remove it from the path cache ...
            this.pathCache.remove(toDelete);

            if (info != null) {
                // Get all the children and add them to the queue ...
                for (ChildNode child : info.getChildren()) {
                    nodesToDelete.add(child.getUuid());
                }
            }
            ++numDeleted;
        }
        return numDeleted;
    }
}
