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
package org.modeshape.graph.session;

import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.NotThreadSafe;
import net.jcip.annotations.ThreadSafe;
import org.modeshape.common.collection.ReadOnlyIterator;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.StringUtil;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.Location;
import org.modeshape.graph.Results;
import org.modeshape.graph.Subgraph;
import org.modeshape.graph.connector.RepositorySourceException;
import org.modeshape.graph.connector.UuidAlreadyExistsException;
import org.modeshape.graph.property.DateTime;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.PathNotFoundException;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.Path.Segment;
import org.modeshape.graph.request.BatchRequestBuilder;
import org.modeshape.graph.request.ChangeRequest;
import org.modeshape.graph.request.CloneBranchRequest;
import org.modeshape.graph.request.CopyBranchRequest;
import org.modeshape.graph.request.CreateNodeRequest;
import org.modeshape.graph.request.InvalidWorkspaceException;
import org.modeshape.graph.request.MoveBranchRequest;
import org.modeshape.graph.request.Request;
import org.modeshape.graph.request.RequestException;
import org.modeshape.graph.session.GraphSession.Authorizer.Action;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;

/**
 * This class represents an interactive session for working with the content within a graph. This session maintains a cache of
 * content read from the repository, as well as transient changes that have been made to the nodes within this session that are
 * then pushed to the graph when the session is {@link #save() saved}.
 * <p>
 * Like the other Graph APIs, the mutable objects in this session should not be held onto for very long periods of time. When the
 * session is {@link #save() saved} or {{@link #refresh(boolean) refreshed} (or when a node is {@link #save(Node) saved} or
 * {@link #refresh(Node, boolean) refreshed}), the session may {@link Node#unload() unload} and discard some of its nodes. Using
 * nodes after they are discarded may result in assertion errors (assuming Java assertions are enabled).
 * </p>
 * 
 * @param <Payload> the type of the payload object for each node, used to allow the nodes to hold additional cached information
 * @param <PropertyPayload> the type of payload object for each property, used to allow the nodes to hold additional cached
 *        information
 */
@NotThreadSafe
public class GraphSession<Payload, PropertyPayload> {

    protected final ListMultimap<Name, Node<Payload, PropertyPayload>> NO_CHILDREN = LinkedListMultimap.create();
    protected final Map<Name, PropertyInfo<PropertyPayload>> NO_PROPERTIES = Collections.emptyMap();

    protected final Authorizer authorizer;
    protected final ExecutionContext context;
    protected final Graph store;
    protected final Node<Payload, PropertyPayload> root;
    protected final Operations<Payload, PropertyPayload> nodeOperations;
    protected final PathFactory pathFactory;
    protected final NodeIdFactory idFactory;
    protected final String workspaceName;
    protected int loadDepth = 1;

    /**
     * A map of the nodes keyed by their identifier. This map contains all of the nodes and is the normative
     */
    protected final Map<NodeId, Node<Payload, PropertyPayload>> nodes = new HashMap<NodeId, Node<Payload, PropertyPayload>>();

    /**
     * A utility map of the nodes keyed by their UUID. This map will not contain nodes that have no UUIDs.
     */
    protected final Map<UUID, Node<Payload, PropertyPayload>> nodesByUuid = new HashMap<UUID, Node<Payload, PropertyPayload>>();

    /**
     * A map that records how the changes to a node are dependent upon other nodes.
     */
    protected final Map<NodeId, Dependencies> changeDependencies = new HashMap<NodeId, Dependencies>();
    /**
     * A set that records the UUIDs of the nodes that have been deleted from this session (via the {@link #operations}) but not
     * yet {@link #save() saved}. This is used to know whether a node has been locally removed to prevent reloading the node from
     * the persistent store.
     */
    protected final Set<UUID> deletedNodes = new HashSet<UUID>();

    private LinkedList<Request> requests;
    private BatchRequestBuilder requestBuilder;
    protected Graph.Batch operations;

    /**
     * Create a session that uses the supplied graph and the supplied node operations.
     * 
     * @param graph the graph that this session is to use
     * @param workspaceName the name of the workspace that is to be used, or null if the current workspace should be used
     * @param nodeOperations the operations that are to be performed during various stages in the lifecycle of a node, or null if
     *        there are no special operations that should be performed
     */
    public GraphSession( Graph graph,
                         String workspaceName,
                         Operations<Payload, PropertyPayload> nodeOperations ) {
        this(graph, workspaceName, nodeOperations, null);
    }

    /**
     * Create a session that uses the supplied graph and the supplied node operations.
     * 
     * @param graph the graph that this session is to use
     * @param workspaceName the name of the workspace that is to be used, or null if the current workspace should be used
     * @param nodeOperations the operations that are to be performed during various stages in the lifecycle of a node, or null if
     *        there are no special operations that should be performed
     * @param authorizer the authorizing component, or null if no special authorization is to be performed
     * @throws IllegalArgumentException if the graph reference is null
     * @throws IllegalArgumentException if the depth is not positive
     */
    public GraphSession( Graph graph,
                         String workspaceName,
                         Operations<Payload, PropertyPayload> nodeOperations,
                         Authorizer authorizer ) {
        assert graph != null;
        this.store = graph;
        this.context = store.getContext();
        if (workspaceName != null) {
            this.workspaceName = this.store.useWorkspace(workspaceName).getName();
        } else {
            this.workspaceName = this.store.getCurrentWorkspaceName();
        }
        this.nodeOperations = nodeOperations != null ? nodeOperations : new NodeOperations<Payload, PropertyPayload>();
        this.pathFactory = context.getValueFactories().getPathFactory();
        this.authorizer = authorizer != null ? authorizer : new NoOpAuthorizer();
        // Create the NodeId factory ...
        this.idFactory = new NodeIdFactory() {
            private long nextId = 0L;

            public NodeId create() {
                return new NodeId(++nextId);
            }
        };
        // Create the root node ...
        Location rootLocation = Location.create(pathFactory.createRootPath());
        NodeId rootId = idFactory.create();
        this.root = createNode(null, rootId, rootLocation);
        this.nodes.put(rootId, root);

        // Create the batch operations ...
        this.requests = new LinkedList<Request>();
        this.requestBuilder = new BatchRequestBuilder(this.requests);
        this.operations = this.store.batch(this.requestBuilder);
    }

    protected void put( Node<Payload, PropertyPayload> node ) {
        this.nodes.put(node.getNodeId(), node);
        // The UUID of the node should never change once its assigned ...
        UUID uuid = node.getLocation().getUuid();
        if (uuid != null) this.nodesByUuid.put(uuid, node);
    }

    protected Node<Payload, PropertyPayload> node( NodeId id ) {
        return this.nodes.get(id);
    }

    protected Node<Payload, PropertyPayload> node( UUID uuid ) {
        // The UUID of the node should never change once its assigned ...
        return this.nodesByUuid.get(uuid);
    }

    protected void removeNode( NodeId id ) {
        Node<Payload, PropertyPayload> removed = this.nodes.remove(id);
        if (removed != null) {
            UUID uuid = removed.getLocation().getUuid();
            if (uuid != null) this.nodesByUuid.remove(uuid);
        }
        this.changeDependencies.remove(id);
    }

    protected void clearNodes() {
        nodes.clear();
        nodes.put(root.getNodeId(), root);
    }

    ExecutionContext context() {
        return context;
    }

    protected Graph.Batch operations() {
        return operations;
    }

    final String readable( Name name ) {
        return name.getString(context.getNamespaceRegistry());
    }

    final String readable( Path.Segment segment ) {
        return segment.getString(context.getNamespaceRegistry());
    }

    final String readable( Path path ) {
        return path.getString(context.getNamespaceRegistry());
    }

    final String readable( Location location ) {
        return location.getString(context.getNamespaceRegistry());
    }

    /**
     * Get the subgraph depth that is read when a node is loaded from the persistence store. By default, this value is 1.
     * 
     * @return the loading depth; always positive
     */
    public int getDepthForLoadingNodes() {
        return loadDepth;
    }

    /**
     * Set the loading depth parameter, which controls how deep a subgraph should be read when a node is loaded from the
     * persistence store. By default, this value is 1.
     * 
     * @param depth the depth that should be read whenever a single node is loaded
     * @throws IllegalArgumentException if the depth is not positive
     */
    public void setDepthForLoadingNodes( int depth ) {
        CheckArg.isPositive(depth, "depth");
        this.loadDepth = depth;
    }

    /**
     * Get the root node.
     * 
     * @return the root node; never null
     */
    public Node<Payload, PropertyPayload> getRoot() {
        return root;
    }

    /**
     * Get the path factory that should be used to adjust the path objects.
     * 
     * @return the path factory; never null
     */
    public PathFactory getPathFactory() {
        return pathFactory;
    }

    /**
     * Find in the session the node with the supplied location. If the location does not have a path, this method must first query
     * the actual persistent store, even if the session already has loaded the node. Thus, this method may not be the most
     * efficient technique to find a node.
     * 
     * @param location the location of the node
     * @return the cached node at the supplied location
     * @throws PathNotFoundException if the node at the supplied location does not exist
     * @throws AccessControlException if the user does not have permission to read the node given by the supplied location
     * @throws IllegalArgumentException if the location is null
     */
    public Node<Payload, PropertyPayload> findNodeWith( Location location ) throws PathNotFoundException, AccessControlException {
        UUID uuid = location.getUuid();
        if (uuid != null) {

            // Try to find the node in the cache ...
            Node<Payload, PropertyPayload> node = node(uuid);
            if (node != null) return node;

            // Has this node already been deleted by this session (but not yet committed)?
            if (this.deletedNodes.contains(uuid)) {
                String msg = GraphI18n.nodeDoesNotExistWithUuid.text(uuid, workspaceName);
                throw new PathNotFoundException(location, this.root.getPath(), msg);
            }

            // Query for the actual location ...
            org.modeshape.graph.Node persistentNode = store.getNodeAt(location);
            location = persistentNode.getLocation();
            Path path = location.getPath();

            // If the node is the root, we already have it ...
            if (path.isRoot()) return root;

            // Make sure the user has access ...
            authorizer.checkPermissions(path, Action.READ);

            // Now find the nodes down to (but not including) this node ...
            Path relativePathFromRootToParent = path.getParent().relativeToRoot();
            Node<Payload, PropertyPayload> parent = findNodeRelativeTo(root, relativePathFromRootToParent, true);

            // Now materialize the child at our location ...
            if (!parent.isLoaded()) parent.load();
            node = parent.getChild(path.getLastSegment());
            nodeOperations.materialize(persistentNode, node);
            return node;
        }

        // There was no UUID, so there has to be a path ...
        assert location.hasPath();
        return findNodeWith(null, location.getPath());
    }

    private UUID uuidFor( Location location ) {
        UUID uuid = location.getUuid();
        if (uuid != null) return uuid;

        Property idProp = location.getIdProperty(JcrLexicon.UUID);
        if (idProp == null) return null;

        return (UUID)idProp.getFirstValue();
    }

    /**
     * Find in the session the node with the supplied identifier.
     * 
     * @param id the identifier of the node
     * @return the identified node, or null if the session has no node with the supplied identifier
     * @throws IllegalArgumentException if the identifier is null
     */
    public Node<Payload, PropertyPayload> findNodeWith( NodeId id ) {
        CheckArg.isNotNull(id, "id");
        return node(id);
    }

    /**
     * Find the node with the supplied identifier or, if no such node is found, the node at the supplied path. Note that if a node
     * was found by the identifier, the resulting may not have the same path as that supplied as a parameter.
     * 
     * @param id the identifier to the node; may be null if the node is to be found by path
     * @param path the path that should be used to find the node only when the cache doesn't contain a node with the identifier
     * @return the node with the supplied id and/or path
     * @throws IllegalArgumentException if the identifier and path are both node
     * @throws PathNotFoundException if the node at the supplied path does not exist
     * @throws AccessControlException if the user does not have permission to read the nodes given by the supplied path
     */
    public Node<Payload, PropertyPayload> findNodeWith( NodeId id,
                                                        Path path ) throws PathNotFoundException, AccessControlException {
        if (id == null && path == null) {
            CheckArg.isNotNull(id, "id");
            CheckArg.isNotNull(path, "path");
        }
        Node<Payload, PropertyPayload> result = id != null ? node(id) : null; // if found, the user should have read
        // privilege since it was
        // already in the cache
        if (result == null || result.isStale()) {
            assert path != null;
            result = findNodeWith(path);
        }
        return result;
    }

    /**
     * Find the node with the supplied path. This node quickly finds the node if it exists in the cache, or if it is not in the
     * cache, it loads the nodes down the supplied path.
     * 
     * @param path the path to the node
     * @return the node information
     * @throws PathNotFoundException if the node at the supplied path does not exist
     * @throws AccessControlException if the user does not have permission to read the nodes given by the supplied path
     */
    public Node<Payload, PropertyPayload> findNodeWith( Path path ) throws PathNotFoundException, AccessControlException {
        if (path.isRoot()) return getRoot();
        if (path.isIdentifier()) return findNodeWith(Location.create(path));
        return findNodeRelativeTo(root, path.relativeTo(root.getPath()), true);
    }

    /**
     * Find the node with the supplied path. This node quickly finds the node if it exists in the cache, or if it is not in the
     * cache, it loads the nodes down the supplied path. However, if <code>loadIfRequired</code> is <code>false</code>, then any
     * node along the path that is not loaded will result in this method returning null.
     * 
     * @param path the path to the node
     * @param loadIfRequired true if any missing nodes should be loaded, or false if null should be returned if any nodes along
     *        the path are not loaded
     * @return the node information
     * @throws PathNotFoundException if the node at the supplied path does not exist
     * @throws AccessControlException if the user does not have permission to read the nodes given by the supplied path
     */
    protected Node<Payload, PropertyPayload> findNodeWith( Path path,
                                                           boolean loadIfRequired )
        throws PathNotFoundException, AccessControlException {
        if (path.isRoot()) return getRoot();
        if (path.isIdentifier()) return findNodeWith(Location.create(path));
        return findNodeRelativeTo(root, path.relativeTo(root.getPath()), loadIfRequired);
    }

    /**
     * Find the node with the supplied path relative to another node. This node quickly finds the node by walking the supplied
     * relative path starting at the supplied node. As soon as a cached node is found to not be fully loaded, the persistent
     * information for that node and all remaining nodes along the relative path are read from the persistent store and inserted
     * into the cache.
     * 
     * @param startingPoint the node from which the path is relative
     * @param relativePath the relative path from the designated starting point to the desired node; may not be null and may not
     *        be an {@link Path#isAbsolute() absolute} path
     * @return the node information
     * @throws PathNotFoundException if the node at the supplied path does not exist
     * @throws AccessControlException if the user does not have permission to read the nodes given by the supplied path
     */
    public Node<Payload, PropertyPayload> findNodeRelativeTo( Node<Payload, PropertyPayload> startingPoint,
                                                              Path relativePath )
        throws PathNotFoundException, AccessControlException {
        return findNodeRelativeTo(startingPoint, relativePath, true);
    }

    /**
     * Find the node with the supplied path relative to another node. This node quickly finds the node by walking the supplied
     * relative path starting at the supplied node. As soon as a cached node is found to not be fully loaded, the persistent
     * information for that node and all remaining nodes along the relative path are read from the persistent store and inserted
     * into the cache.
     * 
     * @param startingPoint the node from which the path is relative
     * @param relativePath the relative path from the designated starting point to the desired node; may not be null and may not
     *        be an {@link Path#isAbsolute() absolute} path
     * @param loadIfRequired true if any missing nodes should be loaded, or false if null should be returned if any nodes along
     *        the path are not loaded
     * @return the node information, or null if the node was not yet loaded (and <code>loadRequired</code> was false)
     * @throws PathNotFoundException if the node at the supplied path does not exist
     * @throws AccessControlException if the user does not have permission to read the nodes given by the supplied path
     */
    @SuppressWarnings( "synthetic-access" )
    protected Node<Payload, PropertyPayload> findNodeRelativeTo( Node<Payload, PropertyPayload> startingPoint,
                                                                 Path relativePath,
                                                                 boolean loadIfRequired )
        throws PathNotFoundException, AccessControlException {
        Node<Payload, PropertyPayload> node = startingPoint;
        if (!relativePath.isRoot()) {
            // Find the absolute path, which ensures that the relative path is well-formed ...
            Path absolutePath = relativePath.resolveAgainst(startingPoint.getPath());

            // Verify that the user has the appropriate privileges to read these nodes...
            authorizer.checkPermissions(absolutePath, Action.READ);

            // Walk down the path ...
            Iterator<Path.Segment> iter = relativePath.iterator();
            while (iter.hasNext()) {
                Path.Segment segment = iter.next();
                try {
                    if (segment.isSelfReference()) continue;
                    if (segment.isParentReference()) {
                        node = node.getParent();
                        assert node != null; // since the relative path is well-formed
                        continue;
                    }

                    if (node.isLoaded()) {
                        // The child is the next node we need to process ...
                        node = node.getChild(segment);
                    } else {
                        if (!loadIfRequired) return null;
                        // The node has not yet been loaded into the cache, so read this node
                        // from the store as well as all nodes along the path to the node we're really
                        // interested in. We'll do this in a batch, so first create this batch ...
                        Graph.Batch batch = store.batch();

                        // Figure out which nodes along the path need to be loaded from the store ...
                        Path firstPath = node.getPath();
                        batch.read(firstPath);
                        // Now add the path to the child (which is no longer on the iterator) ...
                        Path nextPath = pathFactory.create(firstPath, segment);
                        if (!iter.hasNext() && loadDepth > 1) {
                            batch.readSubgraphOfDepth(loadDepth).at(nextPath);
                        } else {
                            batch.read(nextPath);
                        }
                        // Now add any remaining paths that are still on the iterator ...
                        while (iter.hasNext()) {
                            nextPath = pathFactory.create(nextPath, iter.next());
                            if (!iter.hasNext() && loadDepth > 1) {
                                batch.readSubgraphOfDepth(loadDepth).at(nextPath);
                            } else {
                                batch.read(nextPath);
                            }
                        }

                        // Load all of the nodes (we should be reading at least 2 nodes) ...
                        Results batchResults = batch.execute();

                        // Add the children and properties in the lowest cached node ...
                        Path previousPath = null;
                        Node<Payload, PropertyPayload> topNode = node;
                        Node<Payload, PropertyPayload> previousNode = node;
                        for (org.modeshape.graph.Node persistentNode : batchResults) {
                            Location location = persistentNode.getLocation();
                            Path path = location.getPath();
                            if (path.isRoot()) {
                                previousNode = root;
                                root.location = location;
                            } else {
                                if (path.getParent().equals(previousPath)) {
                                    previousNode = previousNode.getChild(path.getLastSegment());
                                } else {
                                    Path subgraphPath = path.relativeTo(topNode.getPath());
                                    previousNode = findNodeRelativeTo(topNode, subgraphPath);
                                }
                                // Set the node that we're looking for ...
                                if (path.getLastSegment().equals(relativePath.getLastSegment()) && path.equals(absolutePath)) {
                                    node = previousNode;
                                }
                            }
                            nodeOperations.materialize(persistentNode, previousNode);
                            previousPath = path;
                        }
                    }
                } catch (RequestException re) {
                    // This can happen if there are multiple segments in the relative path and one of
                    // segment does not exist. Try to resubmit the requests one at a time.
                    try {
                        // Walk down the path ...
                        Iterator<Path.Segment> redoIter = relativePath.iterator();
                        Node<Payload, PropertyPayload> redoNode = startingPoint;
                        while (redoIter.hasNext()) {
                            Path.Segment redoSegment = redoIter.next();
                            if (redoSegment.isSelfReference()) continue;
                            if (redoSegment.isParentReference()) {
                                redoNode = redoNode.getParent();
                                assert redoNode != null; // since the relative path is well-formed
                                continue;
                            }
                            Path firstPath = redoNode.getPath();

                            if (redoNode.isLoaded()) {
                                // The child is the next node we need to process ...
                                redoNode = redoNode.getChild(redoSegment);
                            } else {
                                Path nextPath = firstPath;

                                while (redoIter.hasNext()) {
                                    nextPath = pathFactory.create(nextPath, redoSegment);
                                    store.getNodeAt(nextPath);
                                }
                            }
                        }
                    } catch (PathNotFoundException e) {
                        // Use the correct desired path ...
                        throw new PathNotFoundException(Location.create(relativePath), e.getLowestAncestorThatDoesExist());
                    }

                } catch (PathNotFoundException e) {
                    // Use the correct desired path ...
                    throw new PathNotFoundException(Location.create(relativePath), e.getLowestAncestorThatDoesExist());
                }
            }
        }
        return node;
    }

    /**
     * Returns whether the session cache has any pending changes that need to be executed.
     * 
     * @return true if there are pending changes, or false if there is currently no changes
     */
    public boolean hasPendingChanges() {
        return root.isChanged(true);
    }

    /**
     * Remove any cached information that has been marked as a transient change.
     */
    public void clearAllChangedNodes() {
        root.clearChanges();
        changeDependencies.clear();
        requests.clear();
    }

    /**
     * Move this node from its current location so that is is a child of the supplied parent, doing so immediately without
     * enqueuing the operation within the session's operations. The current session is modified immediately to reflect the move
     * result.
     * 
     * @param nodeToMove the path to the node that is to be moved; may not be null
     * @param destination the desired new path; may not be null
     * @throws IllegalArgumentException if the node being moved is the root node
     * @throws AccessControlException if the caller does not have the permission to perform the operation
     * @throws RepositorySourceException if any error resulting while performing the operation
     */
    public void immediateMove( Path nodeToMove,
                               Path destination ) throws AccessControlException, RepositorySourceException {
        CheckArg.isNotNull(nodeToMove, "nodeToMove");
        CheckArg.isNotNull(destination, "destination");

        Path newParentPath = destination.getParent();
        Name newName = destination.getLastSegment().getName();

        // Check authorization ...
        authorizer.checkPermissions(newParentPath, Action.ADD_NODE);
        authorizer.checkPermissions(nodeToMove.getParent(), Action.REMOVE);

        // Perform the move operation, but use a batch so that we can read the latest list of children ...
        Results results = store.batch().move(nodeToMove).as(newName).into(newParentPath).execute();
        MoveBranchRequest moveRequest = (MoveBranchRequest)results.getRequests().get(0);
        Location locationAfter = moveRequest.getActualLocationAfter();

        // Find the parent node in the session ...
        Node<Payload, PropertyPayload> parent = this.findNodeWith(locationAfter.getPath().getParent(), false);
        if (parent != null && parent.isLoaded()) {
            // Update the children to make them match the latest snapshot from the store ...
            parent.synchronizeWithNewlyPersistedNode(locationAfter);
        }
    }

    /**
     * Copy the node at the supplied source path in the named workspace, and place the copy at the supplied location within the
     * current workspace, doing so immediately without enqueuing the operation within the session's operations. The current
     * session is modified immediately to reflect the copy result.
     * <p>
     * Note that the destination path should not include a same-name-sibling index, since this will be ignored and will always be
     * recomputed (as the copy will be appended to any children already in the destination's parent).
     * </p>
     * 
     * @param source the path to the node that is to be copied; may not be null
     * @param destination the path where the copy is to be placed; may not be null
     * @throws IllegalArgumentException either path is null or invalid
     * @throws AccessControlException if the caller does not have the permission to perform the operation
     * @throws RepositorySourceException if any error resulting while performing the operation
     */
    public void immediateCopy( Path source,
                               Path destination ) throws AccessControlException, RepositorySourceException {
        immediateCopy(source, workspaceName, destination);
    }

    /**
     * Copy the node at the supplied source path in the named workspace, and place the copy at the supplied location within the
     * current workspace, doing so immediately without enqueuing the operation within the session's operations. The current
     * session is modified immediately to reflect the copy result.
     * <p>
     * Note that the destination path should not include a same-name-sibling index, since this will be ignored and will always be
     * recomputed (as the copy will be appended to any children already in the destination's parent).
     * </p>
     * 
     * @param source the path to the node that is to be copied; may not be null
     * @param sourceWorkspace the name of the workspace where the source node is to be found, or null if the current workspace
     *        should be used
     * @param destination the path where the copy is to be placed; may not be null
     * @return the location of the copy
     * @throws IllegalArgumentException either path is null or invalid
     * @throws PathNotFoundException if the node being copied or the parent of the destination path do not exist
     * @throws InvalidWorkspaceException if the source workspace name is invalid or does not exist
     * @throws AccessControlException if the caller does not have the permission to perform the operation
     * @throws RepositorySourceException if any error resulting while performing the operation
     */
    public Location immediateCopy( Path source,
                                   String sourceWorkspace,
                                   Path destination )
        throws InvalidWorkspaceException, AccessControlException, PathNotFoundException, RepositorySourceException {
        CheckArg.isNotNull(source, "source");
        CheckArg.isNotNull(destination, "destination");
        if (sourceWorkspace == null) sourceWorkspace = workspaceName;

        // Check authorization ...
        authorizer.checkPermissions(destination, Action.ADD_NODE);
        authorizer.checkPermissions(source, Action.READ);

        // Perform the copy operation, but use the "to" form (not the "into", which takes the parent), but
        // but use a batch so that we can read the latest list of children ...
        Results results = store.batch().copy(source).fromWorkspace(sourceWorkspace).to(destination).execute();

        // Find the copy request to get the actual location of the copy ...
        CopyBranchRequest request = (CopyBranchRequest)results.getRequests().get(0);
        Location locationOfCopy = request.getActualLocationAfter();

        // Find the parent node in the session ...
        Node<Payload, PropertyPayload> parent = this.findNodeWith(locationOfCopy.getPath().getParent(), false);
        if (parent != null && parent.isLoaded()) {
            // Update the children to make them match the latest snapshot from the store ...
            parent.synchronizeWithNewlyPersistedNode(locationOfCopy);
        }
        return locationOfCopy;
    }

    /**
     * Create a new node at the supplied location, appending to any existing node at that path.
     * 
     * @param path the path where the new node should be created
     * @param properties the properties to be added to the node; may be null or empty
     * @return the location of the new node
     * @throws AccessControlException if the caller does not have the permission to perform the operation
     * @throws RepositorySourceException if any error resulting while performing the operation
     */
    public Location immediateCreateOrReplace( Path path,
                                              Collection<Property> properties )
        throws AccessControlException, RepositorySourceException {
        CheckArg.isNotNull(path, "path");

        // Check authorization ...
        Path newParentPath = path.getParent();
        authorizer.checkPermissions(newParentPath, Action.ADD_NODE);

        // Perform the create operation, but use a batch so that we can read the latest list of children ...
        Results results = null;
        if (properties == null || properties.isEmpty()) {
            results = store.batch().create(path).byAppending().and().execute();
        } else {
            results = store.batch().create(path).byAppending().with(properties).and().execute();
        }
        CreateNodeRequest createRequest = (CreateNodeRequest)results.getRequests().get(0);
        Location locationAfter = createRequest.getActualLocationOfNode();

        // Find the parent node in the session ...
        Node<Payload, PropertyPayload> parent = this.findNodeWith(locationAfter.getPath().getParent(), false);
        if (parent != null && parent.isLoaded()) {
            // Update the children to make them match the latest snapshot from the store ...
            parent.synchronizeWithNewlyPersistedNode(locationAfter);
        }
        return locationAfter;
    }

    /**
     * Clone the supplied source branch and place into the destination location, optionally removing any existing copy that
     * already exists in the destination location, doing so immediately without enqueuing the operation within the session's
     * operations. The current session is modified immediately to reflect the clone result.
     * 
     * @param source the path to the node that is to be cloned; may not be null
     * @param sourceWorkspace the name of the workspace where the source node is to be found, or null if the current workspace
     *        should be used
     * @param destination the path for the new cloned copy; may not be null index
     * @param removeExisting true if the original should be removed, or false if the original should be left
     * @param destPathIncludesSegment true if the destination path includes the segment that should be used
     * @throws IllegalArgumentException either path is null or invalid
     * @throws InvalidWorkspaceException if the source workspace name is invalid or does not exist
     * @throws UuidAlreadyExistsException if copy could not be completed because the current workspace already includes at least
     *         one of the nodes at or below the <code>source</code> branch in the source workspace
     * @throws PathNotFoundException if the node being clone or the destination node do not exist
     * @throws AccessControlException if the caller does not have the permission to perform the operation
     * @throws RepositorySourceException if any error resulting while performing the operation
     */
    public void immediateClone( Path source,
                                String sourceWorkspace,
                                Path destination,
                                boolean removeExisting,
                                boolean destPathIncludesSegment )
        throws InvalidWorkspaceException, AccessControlException, UuidAlreadyExistsException, PathNotFoundException,
        RepositorySourceException {
        CheckArg.isNotNull(source, "source");
        CheckArg.isNotNull(destination, "destination");
        if (sourceWorkspace == null) sourceWorkspace = workspaceName;

        // Check authorization ...
        authorizer.checkPermissions(destination.getParent(), Action.ADD_NODE);
        authorizer.checkPermissions(source, Action.READ);

        // Perform the copy operation, but use the "to" form (not the "into", which takes the parent), but
        // but use a batch so that we can read the latest list of children ...
        Graph.Batch batch = store.batch();
        if (removeExisting) {
            // Perform the copy operation, but use the "to" form (not the "into", which takes the parent) ...
            if (destPathIncludesSegment) {
                batch.clone(source)
                     .fromWorkspace(sourceWorkspace)
                     .as(destination.getLastSegment())
                     .into(destination.getParent())
                     .replacingExistingNodesWithSameUuids();
            } else {
                Name newNodeName = destination.getLastSegment().getName();
                batch.clone(source)
                     .fromWorkspace(sourceWorkspace)
                     .as(newNodeName)
                     .into(destination.getParent())
                     .replacingExistingNodesWithSameUuids();
            }
        } else {
            // Perform the copy operation, but use the "to" form (not the "into", which takes the parent) ...
            if (destPathIncludesSegment) {
                batch.clone(source)
                     .fromWorkspace(sourceWorkspace)
                     .as(destination.getLastSegment())
                     .into(destination.getParent())
                     .failingIfAnyUuidsMatch();
            } else {
                Name newNodeName = destination.getLastSegment().getName();
                batch.clone(source)
                     .fromWorkspace(sourceWorkspace)
                     .as(newNodeName)
                     .into(destination.getParent())
                     .failingIfAnyUuidsMatch();
            }
        }
        // Now execute these two operations ...
        Results results = batch.execute();

        // Find the copy request to get the actual location of the copy ...
        CloneBranchRequest request = (CloneBranchRequest)results.getRequests().get(0);
        Location locationOfCopy = request.getActualLocationAfter();

        // Remove from the session all of the nodes that were removed as part of this clone ...
        Set<Path> removedAlready = new HashSet<Path>();
        for (Location removed : request.getRemovedNodes()) {
            Path path = removed.getPath();
            if (isBelow(path, removedAlready)) {
                // This node is below a node we've already removed, so skip it ...
                continue;
            }
            Node<Payload, PropertyPayload> removedNode = findNodeWith(path, false);
            removedNode.remove(false);
            removedAlready.add(path);
        }

        // Find the parent node in the session ...
        Node<Payload, PropertyPayload> parent = this.findNodeWith(locationOfCopy.getPath().getParent(), false);
        if (parent != null && parent.isLoaded()) {
            // Update the children to make them match the latest snapshot from the store ...
            parent.synchronizeWithNewlyPersistedNode(locationOfCopy);
        }
    }

    private static final boolean isBelow( Path path,
                                          Collection<Path> paths ) {
        for (Path aPath : paths) {
            if (aPath.isAncestorOf(path)) return true;
        }
        return false;
    }

    /**
     * Refreshes (removes the cached state) for all cached nodes.
     * <p>
     * If {@code keepChanges == true}, modified nodes will not have their state refreshed, while all others will either be
     * unloaded or changed to reflect the current state of the persistent store.
     * </p>
     * 
     * @param keepChanges indicates whether changed nodes should be kept or refreshed from the repository.
     * @throws InvalidStateException if any error resulting while reading information from the repository
     * @throws RepositorySourceException if any error resulting while reading information from the repository
     */
    @SuppressWarnings( "synthetic-access" )
    public void refresh( boolean keepChanges ) throws InvalidStateException, RepositorySourceException {
        if (keepChanges) {
            refresh(root, keepChanges);
        } else {
            // Clear out all state ...
            clearNodes();
            // Clear out all changes ...
            requests.clear();
            changeDependencies.clear();
            // And force the root node to be 'unloaded' (in an efficient way) ...
            root.status = Status.UNCHANGED;
            root.childrenByName = null;
            root.expirationTime = Long.MAX_VALUE;
            root.changedBelow = false;
            root.payload = null;
        }
    }

    /**
     * Refreshes (removes the cached state) for the given node and its descendants.
     * <p>
     * If {@code keepChanges == true}, modified nodes will not have their state refreshed, while all others will either be
     * unloaded or changed to reflect the current state of the persistent store.
     * </p>
     * 
     * @param node the node that is to be refreshed; may not be null
     * @param keepChanges indicates whether changed nodes should be kept or refreshed from the repository.
     * @throws InvalidStateException if any error resulting while reading information from the repository
     * @throws RepositorySourceException if any error resulting while reading information from the repository
     */
    public void refresh( Node<Payload, PropertyPayload> node,
                         boolean keepChanges ) throws InvalidStateException, RepositorySourceException {
        if (!node.isRoot() && node.isChanged(true)) {
            // Need to make sure that changes to this branch are not dependent upon changes to nodes outside of this branch...
            if (node.containsChangesWithExternalDependencies()) {
                I18n msg = GraphI18n.unableToRefreshBranchBecauseChangesDependOnChangesToNodesOutsideOfBranch;
                String path = readable(node.getPath());
                throw new InvalidStateException(msg.text(path, workspaceName));
            }
        }

        if (keepChanges && node.isChanged(true)) {
            // Perform the refresh while retaining changes ...
            // Phase 1: determine which nodes can be unloaded, which must be refreshed, and which must be unchanged ...
            RefreshState<Payload, PropertyPayload> refreshState = new RefreshState<Payload, PropertyPayload>();
            node.refreshPhase1(refreshState);
            // If there are any nodes to be refreshed, read then in a single batch ...
            Results readResults = null;
            if (!refreshState.getNodesToBeRefreshed().isEmpty()) {
                Graph.Batch batch = store.batch();
                for (Node<Payload, PropertyPayload> nodeToBeRefreshed : refreshState.getNodesToBeRefreshed()) {
                    batch.read(nodeToBeRefreshed.getLocation());
                }
                // Execute the reads. No modifications have been made to the cache, so it is not a problem
                // if this throws a repository exception.
                try {
                    readResults = batch.execute();
                } catch (org.modeshape.graph.property.PathNotFoundException e) {
                    throw new InvalidStateException(e.getLocalizedMessage(), e);
                }
            }

            // Phase 2: update the cache by unloading or refreshing the nodes ...
            node.refreshPhase2(refreshState, readResults);
        } else {
            // Get rid of all changes ...
            node.clearChanges();
            // And then unload the node ...
            node.unload();

            // Throw out the old pending operations
            if (operations.isExecuteRequired()) {
                // Make sure the builder has finished all the requests ...
                this.requestBuilder.finishPendingRequest();

                // Remove all of the enqueued requests for this branch ...
                for (Iterator<Request> iter = this.requests.iterator(); iter.hasNext();) {
                    Request request = iter.next();
                    assert request instanceof ChangeRequest;
                    ChangeRequest change = (ChangeRequest)request;
                    if (change.changes(workspaceName, node.getPath())) {
                        iter.remove();
                    }
                }
            }
        }
    }

    /**
     * Refreshes all properties for the given node only. This refresh always discards changed properties.
     * <p>
     * This method is not recursive and will not modify or access any descendants of the given node.
     * </p>
     * <p>
     * <b>NOTE: Calling this method on a node that already has modified properties can result in the enqueued property changes
     * overwriting the current properties on a save() call. This method should be used with great care to avoid this
     * situation.</b>
     * </p>
     * 
     * @param node the node for which the properties are to be refreshed; may not be null
     * @throws InvalidStateException if the node is new
     * @throws RepositorySourceException if any error resulting while reading information from the repository
     */
    public void refreshProperties( Node<Payload, PropertyPayload> node ) throws InvalidStateException, RepositorySourceException {
        assert node != null;

        if (node.isNew()) {
            I18n msg = GraphI18n.unableToRefreshPropertiesBecauseNodeIsModified;
            String path = readable(node.getPath());
            throw new InvalidStateException(msg.text(path, workspaceName));
        }

        org.modeshape.graph.Node persistentNode = store.getNodeAt(node.getLocation());
        nodeOperations.materializeProperties(persistentNode, node);
    }

    /**
     * Save any changes that have been accumulated by this session.
     * 
     * @throws PathNotFoundException if the state of this session is invalid and is attempting to change a node that doesn't exist
     * @throws ValidationException if any of the changes being made result in an invalid node state
     * @throws InvalidStateException if the supplied node is no longer a node within this cache (because it was unloaded)
     */
    public void save() throws PathNotFoundException, ValidationException, InvalidStateException {
        if (!operations.isExecuteRequired()) {
            // Remove all the cached items ...
            this.root.clearChanges();
            this.root.unload();
            return;
        }

        if (!root.isChanged(true)) {
            // Then a bunch of changes could have been made and rolled back manually, so recompute the change state ...
            root.recomputeChangedBelow();
            if (!root.isChanged(true)) {
                // If still no changes, then simply do a refresh ...
                this.root.clearChanges();
                this.root.unload();
                return;
            }
        }

        // Make sure that each of the changed node is valid. This process requires that all children of
        // all changed nodes are loaded, so in this process load all unloaded children in one batch ...
        final DateTime saveTime = context.getValueFactories().getDateFactory().create();
        root.onChangedNodes(new LoadAllChildrenVisitor() {
            @Override
            protected void finishParentAfterLoading( Node<Payload, PropertyPayload> node ) {
                nodeOperations.preSave(node, saveTime);
            }
        });

        root.onChangedNodes(new LoadAllChildrenVisitor() {
            @Override
            protected void finishParentAfterLoading( Node<Payload, PropertyPayload> node ) {
                nodeOperations.compute(operations, node);
            }
        });

        // Execute the batched operations ...
        try {
            operations.execute();
        } catch (org.modeshape.graph.property.PathNotFoundException e) {
            throw new InvalidStateException(e.getLocalizedMessage(), e);
        } catch (RuntimeException e) {
            throw new RepositorySourceException(e.getLocalizedMessage(), e);
        }

        // Clear out the record of which nodes were deleted in that batch ...
        this.deletedNodes.clear();

        // Create a new batch for future operations ...
        // LinkedList<Request> oldRequests = this.requests;
        this.requests = new LinkedList<Request>();
        this.requestBuilder = new BatchRequestBuilder(this.requests);
        this.operations = store.batch(this.requestBuilder);

        // Remove all the cached items ...
        this.root.clearChanges();
        this.root.unload();
    }

    /**
     * Save any changes to the identified node or its descendants. The supplied node may not have been deleted or created in this
     * session since the last save operation.
     * 
     * @param node the node being saved; may not be null
     * @throws PathNotFoundException if the state of this session is invalid and is attempting to change a node that doesn't exist
     * @throws ValidationException if any of the changes being made result in an invalid node state
     * @throws InvalidStateException if the supplied node is no longer a node within this cache (because it was unloaded)
     */
    public void save( Node<Payload, PropertyPayload> node )
        throws PathNotFoundException, ValidationException, InvalidStateException {
        assert node != null;
        if (node.isRoot()) {
            // We're actually saving the root, so the other 'save' method is faster and more efficient ...
            save();
            return;
        }
        if (node.isStale()) {
            // This node was deleted in this session ...
            String readableLocation = readable(node.getLocation());
            I18n msg = GraphI18n.nodeHasAlreadyBeenRemovedFromThisSession;
            throw new InvalidStateException(msg.text(readableLocation, workspaceName));
        }
        if (node.isNew()) {
            String path = readable(node.getPath());
            throw new RepositorySourceException(GraphI18n.unableToSaveNodeThatWasCreatedSincePreviousSave.text(path,
                                                                                                               workspaceName));
        }
        if (!node.isChanged(true)) {
            // There are no changes within this branch
            return;
        }

        // Need to make sure that changes to this branch are not dependent upon changes to nodes outside of this branch...
        if (node.containsChangesWithExternalDependencies()) {
            I18n msg = GraphI18n.unableToSaveBranchBecauseChangesDependOnChangesToNodesOutsideOfBranch;
            String path = readable(node.getPath());
            throw new ValidationException(msg.text(path, workspaceName));
        }

        // Make sure that each of the changed node is valid. This process requires that all children of
        // all changed nodes are loaded, so in this process load all unloaded children in one batch ...
        final DateTime saveTime = context.getValueFactories().getDateFactory().create();
        root.onChangedNodes(new LoadAllChildrenVisitor() {
            @Override
            protected void finishParentAfterLoading( Node<Payload, PropertyPayload> node ) {
                nodeOperations.preSave(node, saveTime);
            }
        });

        // Make sure the builder has finished all the requests ...
        this.requestBuilder.finishPendingRequest();

        // Remove all of the enqueued requests for this branch ...
        Path path = node.getPath();
        LinkedList<Request> branchRequests = new LinkedList<Request>();
        LinkedList<Request> nonBranchRequests = new LinkedList<Request>();
        for (Request request : this.requests) {
            assert request instanceof ChangeRequest;
            ChangeRequest change = (ChangeRequest)request;
            if (change.changes(workspaceName, path)) {
                branchRequests.add(request);
            } else {
                nonBranchRequests.add(request);
            }
        }
        if (branchRequests.isEmpty()) return;

        // Now execute the branch ...
        final Graph.Batch branchBatch = store.batch(new BatchRequestBuilder(branchRequests));

        node.onChangedNodes(new LoadAllChildrenVisitor() {
            @Override
            protected void finishParentAfterLoading( Node<Payload, PropertyPayload> node ) {
                nodeOperations.compute(branchBatch, node);
            }
        });

        try {
            branchBatch.execute();
        } catch (org.modeshape.graph.property.PathNotFoundException e) {
            throw new InvalidStateException(e.getLocalizedMessage(), e);
        } catch (RuntimeException e) {
            throw new RepositorySourceException(e.getLocalizedMessage(), e);
        }

        // Still have non-branch related requests that we haven't executed ...
        this.requests = nonBranchRequests;
        this.requestBuilder = new BatchRequestBuilder(this.requests);
        this.operations = store.batch(this.requestBuilder);

        // Remove all the cached, changed or deleted items that were just saved ...
        node.clearChanges();
        node.unload();
    }

    protected Node<Payload, PropertyPayload> createNode( Node<Payload, PropertyPayload> parent,
                                                         NodeId nodeId,
                                                         Location location ) {
        return new Node<Payload, PropertyPayload>(this, parent, nodeId, location);
    }

    protected long getCurrentTime() {
        return System.currentTimeMillis();
    }

    protected void recordMove( Node<Payload, PropertyPayload> nodeBeingMoved,
                               Node<Payload, PropertyPayload> oldParent,
                               Node<Payload, PropertyPayload> newParent ) {
        // Fix the cache's state ...
        NodeId id = nodeBeingMoved.getNodeId();
        Dependencies dependencies = changeDependencies.get(id);
        if (dependencies == null) {
            dependencies = new Dependencies();
            dependencies.setMovedFrom(oldParent.getNodeId());
            changeDependencies.put(id, dependencies);
        } else {
            dependencies.setMovedFrom(newParent.getNodeId());
        }
    }

    /**
     * Record the fact that the supplied node is in the process of being deleted, so any cached information (outside of the node
     * object itself) should be cleaned up.
     * 
     * @param node the node being deleted; never null
     */
    protected void recordDelete( Node<Payload, PropertyPayload> node ) {
        // Record the operation ...
        Location location = node.getLocation();
        operations.delete(location);
        UUID nodeUuid = uuidFor(location);
        if (nodeUuid != null) {
            deletedNodes.add(nodeUuid);
        }
        // Fix the cache's state ...
        removeNode(node.getNodeId());
        recordUnloaded(node);
    }

    /**
     * Record the fact that the supplied node is in the process of being unloaded, so any cached information (outside of the node
     * object itself) should be cleaned up.
     * 
     * @param node the node being unloaded; never null
     */
    protected void recordUnloaded( final Node<Payload, PropertyPayload> node ) {
        if (node.isLoaded() && node.getChildrenCount() > 0) {
            // Walk the branch and remove all nodes from the map of all nodes ...
            node.onCachedNodes(new NodeVisitor<Payload, PropertyPayload>() {
                @SuppressWarnings( "synthetic-access" )
                @Override
                public boolean visit( Node<Payload, PropertyPayload> unloaded ) {
                    if (unloaded != node) { // info for 'node' should not be removed
                        removeNode(unloaded.getNodeId());
                        unloaded.parent = null;
                    }
                    return true;
                }
            });
        }
    }

    @ThreadSafe
    public static interface Operations<NodePayload, PropertyPayload> {

        /**
         * Update the children and properties for the node with the information from the persistent store.
         * 
         * @param persistentNode the persistent node that should be converted into a node info; never null
         * @param node the session's node representation that is to be updated; never null
         */
        void materialize( org.modeshape.graph.Node persistentNode,
                          Node<NodePayload, PropertyPayload> node );

        /**
         * Update the properties ONLY for the node with the information from the persistent store.
         * 
         * @param persistentNode the persistent node that should be converted into a node info; never null
         * @param node the session's node representation that is to be updated; never null
         */
        void materializeProperties( org.modeshape.graph.Node persistentNode,
                                    Node<NodePayload, PropertyPayload> node );

        /**
         * Signal that the node's {@link GraphSession.Node#getLocation() location} has been changed
         * 
         * @param node the node with the new location
         * @param oldLocation the old location of the node
         */
        void postUpdateLocation( Node<NodePayload, PropertyPayload> node,
                                 Location oldLocation );

        void preSetProperty( Node<NodePayload, PropertyPayload> node,
                             Name propertyName,
                             PropertyInfo<PropertyPayload> newProperty ) throws ValidationException;

        void postSetProperty( Node<NodePayload, PropertyPayload> node,
                              Name propertyName,
                              PropertyInfo<PropertyPayload> oldProperty );

        void preRemoveProperty( Node<NodePayload, PropertyPayload> node,
                                Name propertyName ) throws ValidationException;

        void postRemoveProperty( Node<NodePayload, PropertyPayload> node,
                                 Name propertyName,
                                 PropertyInfo<PropertyPayload> oldProperty );

        /**
         * Notify that a new child with the supplied path segment is about to be created. When this method is called, the child
         * has not yet been added to the parent node.
         * 
         * @param parentNode the parent node; never null
         * @param newChild the path segment for the new child; never null
         * @param properties the initial properties for the new child, which can be manipulated directly; never null
         * @throws ValidationException if the parent may not have a child with the supplied name and the creation of the new node
         *         should be aborted
         */
        void preCreateChild( Node<NodePayload, PropertyPayload> parentNode,
                             Path.Segment newChild,
                             Map<Name, PropertyInfo<PropertyPayload>> properties ) throws ValidationException;

        /**
         * Notify that a new child has been added to the supplied parent node. The child may have an initial set of properties
         * specified at creation time, although none of the PropertyInfo objects will have a
         * {@link GraphSession.PropertyInfo#getPayload() payload}.
         * 
         * @param parentNode the parent node; never null
         * @param newChild the child that was just added to the parent node; never null
         * @param properties the properties of the child, which can be manipulated directly; never null
         * @throws ValidationException if the parent and child are not valid and the creation of the new node should be aborted
         */
        void postCreateChild( Node<NodePayload, PropertyPayload> parentNode,
                              Node<NodePayload, PropertyPayload> newChild,
                              Map<Name, PropertyInfo<PropertyPayload>> properties ) throws ValidationException;

        /**
         * Notify that an existing child will be moved from its current parent and placed under the supplied parent. When this
         * method is called, the child node has not yet been moved.
         * 
         * @param nodeToBeMoved the existing node that is to be moved from its current parent to the supplied parent; never null
         * @param newParentNode the new parent node; never null
         * @throws ValidationException if the child should not be moved
         */
        void preMove( Node<NodePayload, PropertyPayload> nodeToBeMoved,
                      Node<NodePayload, PropertyPayload> newParentNode ) throws ValidationException;

        /**
         * Notify that an existing child has been moved from the supplied previous parent into its new location. When this method
         * is called, the child node has been moved and any same-name-siblings that were after the child in the old parent have
         * had their SNS indexes adjusted.
         * 
         * @param movedNode the existing node that is was moved; never null
         * @param oldParentNode the old parent node; never null
         */
        void postMove( Node<NodePayload, PropertyPayload> movedNode,
                       Node<NodePayload, PropertyPayload> oldParentNode );

        /**
         * Notify that an existing child will be copied with the new copy being placed under the supplied parent. When this method
         * is called, the copy has not yet been performed.
         * 
         * @param original the existing node that is to be copied; never null
         * @param newParentNode the parent node where the copy is to be placed; never null
         * @throws ValidationException if the copy is not valid
         */
        void preCopy( Node<NodePayload, PropertyPayload> original,
                      Node<NodePayload, PropertyPayload> newParentNode ) throws ValidationException;

        /**
         * Notify that an existing child will be copied with the new copy being placed under the supplied parent. When this method
         * is called, the copy has been performed, but the new copy will not be loaded nor will be capable of being loaded.
         * 
         * @param original the original node that was copied; never null
         * @param copy the new copy that was made; never null
         */
        void postCopy( Node<NodePayload, PropertyPayload> original,
                       Node<NodePayload, PropertyPayload> copy );

        /**
         * Notify that an existing child will be removed from the supplied parent. When this method is called, the child node has
         * not yet been removed.
         * 
         * @param parentNode the parent node; never null
         * @param child the child that is to be removed from the parent node; never null
         * @throws ValidationException if the child should not be removed from the parent node
         */
        void preRemoveChild( Node<NodePayload, PropertyPayload> parentNode,
                             Node<NodePayload, PropertyPayload> child ) throws ValidationException;

        /**
         * Notify that an existing child has been removed from the supplied parent. When this method is called, the child node has
         * been removed and any same-name-siblings following the child have had their SNS indexes adjusted. Additionally, the
         * removed child no longer has a parent and is considered {@link GraphSession.Node#isStale() stale}.
         * 
         * @param parentNode the parent node; never null
         * @param removedChild the child that is to be removed from the parent node; never null
         */
        void postRemoveChild( Node<NodePayload, PropertyPayload> parentNode,
                              Node<NodePayload, PropertyPayload> removedChild );

        /**
         * Validate a node for consistency and well-formedness.
         * 
         * @param node the node to be validated
         * @param saveTime the time at which the save operation is occurring; never null
         * @throws ValidationException if there is a problem during validation
         */
        void preSave( Node<NodePayload, PropertyPayload> node,
                      DateTime saveTime ) throws ValidationException;

        /**
         * Update any computed fields based on the given node
         * 
         * @param batch the workspace graph batch in which computed fields should be created
         * @param node the node form which computed fields will be derived
         */
        void compute( Graph.Batch batch,
                      Node<NodePayload, PropertyPayload> node );
    }

    @ThreadSafe
    public static interface NodeIdFactory {
        NodeId create();
    }

    @ThreadSafe
    public static interface Authorizer {

        public enum Action {
            READ,
            REMOVE,
            ADD_NODE,
            SET_PROPERTY;
        }

        /**
         * Throws an {@link AccessControlException} if the current user is not able to perform the action on the node at the
         * supplied path in the current workspace.
         * 
         * @param path the path on which the actions are occurring
         * @param action the action to check
         * @throws AccessControlException if the user does not have permission to perform the actions
         */
        void checkPermissions( Path path,
                               Action action ) throws AccessControlException;
    }

    /**
     * {@link Authorizer} implementation that does nothing.
     */
    @ThreadSafe
    protected static class NoOpAuthorizer implements Authorizer {
        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.session.GraphSession.Authorizer#checkPermissions(org.modeshape.graph.property.Path,
         *      org.modeshape.graph.session.GraphSession.Authorizer.Action)
         */
        public void checkPermissions( Path path,
                                      Action action ) throws AccessControlException {
        }
    }

    /**
     * A default implementation of {@link GraphSession.Operations} that provides all the basic functionality required by a graph
     * session. In this implementation, only the {@link GraphSession.NodeOperations#materialize(org.modeshape.graph.Node, Node)
     * materialize(...)} method does something.
     * 
     * @param <Payload> the type of node payload object
     * @param <PropertyPayload> the type of property payload object
     */
    @ThreadSafe
    public static class NodeOperations<Payload, PropertyPayload> implements Operations<Payload, PropertyPayload> {
        /**
         * {@inheritDoc}
         * 
         * @see GraphSession.Operations#materialize(org.modeshape.graph.Node, GraphSession.Node)
         */
        public void materialize( org.modeshape.graph.Node persistentNode,
                                 Node<Payload, PropertyPayload> node ) {
            // Create the map of property info objects ...
            Map<Name, PropertyInfo<PropertyPayload>> properties = new HashMap<Name, PropertyInfo<PropertyPayload>>();
            for (Property property : persistentNode.getProperties()) {
                Name propertyName = property.getName();
                PropertyInfo<PropertyPayload> info = new PropertyInfo<PropertyPayload>(property, property.isMultiple(),
                                                                                       Status.UNCHANGED, null);
                properties.put(propertyName, info);
            }
            // Set only the children ...
            node.loadedWith(persistentNode.getChildren(), properties, persistentNode.getExpirationTime());
        }

        /**
         * {@inheritDoc}
         * 
         * @see GraphSession.Operations#materializeProperties(org.modeshape.graph.Node, GraphSession.Node)
         */
        public void materializeProperties( org.modeshape.graph.Node persistentNode,
                                           Node<Payload, PropertyPayload> node ) {
            // Create the map of property info objects ...
            Map<Name, PropertyInfo<PropertyPayload>> properties = new HashMap<Name, PropertyInfo<PropertyPayload>>();
            for (Property property : persistentNode.getProperties()) {
                Name propertyName = property.getName();
                PropertyInfo<PropertyPayload> info = new PropertyInfo<PropertyPayload>(property, property.isMultiple(),
                                                                                       Status.UNCHANGED, null);
                properties.put(propertyName, info);
            }
            // Set only the children ...
            node.loadedWith(properties);
        }

        /**
         * {@inheritDoc}
         * 
         * @see GraphSession.Operations#postUpdateLocation(GraphSession.Node, org.modeshape.graph.Location)
         */
        public void postUpdateLocation( Node<Payload, PropertyPayload> node,
                                        Location oldLocation ) {
            // do nothing here
        }

        /**
         * {@inheritDoc}
         * 
         * @see GraphSession.Operations#preSave(GraphSession.Node,DateTime)
         */
        public void preSave( Node<Payload, PropertyPayload> node,
                             DateTime saveTime ) throws ValidationException {
            // do nothing here
        }

        /**
         * {@inheritDoc}
         * 
         * @see GraphSession.Operations#compute(Graph.Batch, GraphSession.Node)
         */
        public void compute( Graph.Batch batch,
                             Node<Payload, PropertyPayload> node ) {
            // do nothing here
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.session.GraphSession.Operations#preSetProperty(Node, Name, PropertyInfo)
         */
        public void preSetProperty( Node<Payload, PropertyPayload> node,
                                    Name propertyName,
                                    PropertyInfo<PropertyPayload> newProperty ) throws ValidationException {
            // do nothing here
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.session.GraphSession.Operations#postSetProperty(Node, Name, PropertyInfo)
         */
        public void postSetProperty( Node<Payload, PropertyPayload> node,
                                     Name propertyName,
                                     PropertyInfo<PropertyPayload> oldProperty ) {
            // do nothing here
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.session.GraphSession.Operations#preRemoveProperty(Node, Name)
         */
        public void preRemoveProperty( Node<Payload, PropertyPayload> node,
                                       Name propertyName ) throws ValidationException {
            // do nothing here
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.session.GraphSession.Operations#postRemoveProperty(Node, Name, PropertyInfo)
         */
        public void postRemoveProperty( Node<Payload, PropertyPayload> node,
                                        Name propertyName,
                                        PropertyInfo<PropertyPayload> oldProperty ) {
            // do nothing here
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.session.GraphSession.Operations#preCreateChild(org.modeshape.graph.session.GraphSession.Node,
         *      org.modeshape.graph.property.Path.Segment, java.util.Map)
         */
        public void preCreateChild( Node<Payload, PropertyPayload> parent,
                                    Segment newChild,
                                    Map<Name, PropertyInfo<PropertyPayload>> properties ) throws ValidationException {
            // do nothing here
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.session.GraphSession.Operations#postCreateChild(org.modeshape.graph.session.GraphSession.Node,
         *      org.modeshape.graph.session.GraphSession.Node, java.util.Map)
         */
        public void postCreateChild( Node<Payload, PropertyPayload> parent,
                                     Node<Payload, PropertyPayload> childChild,
                                     Map<Name, PropertyInfo<PropertyPayload>> properties ) throws ValidationException {
            // do nothing here
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.session.GraphSession.Operations#preCopy(org.modeshape.graph.session.GraphSession.Node,
         *      org.modeshape.graph.session.GraphSession.Node)
         */
        public void preCopy( Node<Payload, PropertyPayload> original,
                             Node<Payload, PropertyPayload> newParent ) throws ValidationException {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.session.GraphSession.Operations#postCopy(org.modeshape.graph.session.GraphSession.Node,
         *      org.modeshape.graph.session.GraphSession.Node)
         */
        public void postCopy( Node<Payload, PropertyPayload> original,
                              Node<Payload, PropertyPayload> copy ) throws ValidationException {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.session.GraphSession.Operations#preMove(org.modeshape.graph.session.GraphSession.Node,
         *      org.modeshape.graph.session.GraphSession.Node)
         */
        public void preMove( Node<Payload, PropertyPayload> nodeToBeMoved,
                             Node<Payload, PropertyPayload> newParent ) throws ValidationException {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.session.GraphSession.Operations#postMove(org.modeshape.graph.session.GraphSession.Node,
         *      org.modeshape.graph.session.GraphSession.Node)
         */
        public void postMove( Node<Payload, PropertyPayload> movedNode,
                              Node<Payload, PropertyPayload> oldParent ) {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.session.GraphSession.Operations#preRemoveChild(org.modeshape.graph.session.GraphSession.Node,
         *      org.modeshape.graph.session.GraphSession.Node)
         */
        public void preRemoveChild( Node<Payload, PropertyPayload> parent,
                                    Node<Payload, PropertyPayload> newChild ) throws ValidationException {
            // do nothing here
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.session.GraphSession.Operations#postRemoveChild(org.modeshape.graph.session.GraphSession.Node,
         *      org.modeshape.graph.session.GraphSession.Node)
         */
        public void postRemoveChild( Node<Payload, PropertyPayload> parent,
                                     Node<Payload, PropertyPayload> oldChild ) {
            // do nothing here
        }
    }

    @NotThreadSafe
    public static class Node<Payload, PropertyPayload> {
        private final GraphSession<Payload, PropertyPayload> cache;
        private final NodeId nodeId;
        private Node<Payload, PropertyPayload> parent;
        private long expirationTime = Long.MAX_VALUE;
        private Location location;
        private Status status = Status.UNCHANGED;
        private boolean changedBelow;
        private Map<Name, PropertyInfo<PropertyPayload>> properties;
        private ListMultimap<Name, Node<Payload, PropertyPayload>> childrenByName;
        private Payload payload;
        private Location originalLocation;

        public Node( GraphSession<Payload, PropertyPayload> cache,
                     Node<Payload, PropertyPayload> parent,
                     NodeId nodeId,
                     Location location ) {
            this.cache = cache;
            this.parent = parent;
            this.nodeId = nodeId;
            this.location = location;
            this.originalLocation = location;
            assert this.cache != null;
            assert this.nodeId != null;
            assert this.location != null;
            assert this.location.hasPath();
        }

        /**
         * Get the session to which this node belongs.
         * 
         * @return the session; never null
         */
        public GraphSession<Payload, PropertyPayload> getSession() {
            return cache;
        }

        /**
         * Get the time when this node expires.
         * 
         * @return the time in milliseconds past the epoch when this node's cached information expires, or {@link Long#MAX_VALUE
         *         Long.MAX_VALUE} if there is no expiration or if the node has not been loaded
         * @see #isExpired()
         * @see #isLoaded()
         */
        public final long getExpirationTimeInMillis() {
            return expirationTime;
        }

        /**
         * Determine if this node's information has expired. This method will never return true if the node is not loaded. This
         * method is idempotent.
         * 
         * @return true if this node's information has been read from the store and is expired
         */
        public final boolean isExpired() {
            return expirationTime != Long.MAX_VALUE && expirationTime < cache.getCurrentTime();
        }

        /**
         * Determine if this node is loaded and usable. Even though the node may have been loaded previously, this method may
         * return false (and unloads the cached information) if the cached information has expired and thus is no longer usable.
         * Note, however, that changes on or below this node will prevent the node from being unloaded.
         * 
         * @return true if the node's information has already been loaded and may be used, or false otherwise
         */
        public final boolean isLoaded() {
            if (childrenByName == null) return false;
            // Otherwise, it is already loaded. First see if this is expired ...
            if (isExpired()) {
                // It is expired, so we'd normally return false. But we should not unload if it has changes ...
                if (isChanged(true)) return true;
                // It is expired and contains no changes on this branch, so we can unload it ...
                unload();
                return false;
            }
            // Otherwise it is loaded and not expired ...
            return true;
        }

        /**
         * Method that causes the information for this node to be read from the store and loaded into the cache
         * 
         * @throws AccessControlException if the caller does not have the permission to perform the operation
         * @throws RepositorySourceException if there is a problem reading the store
         */
        protected final void load() throws RepositorySourceException {
            if (isLoaded()) return;
            assert !isStale();
            // If this node is new, then there's nothing to read ...
            if (status == Status.NEW) {
                this.childrenByName = cache.NO_CHILDREN;
                this.properties = cache.NO_PROPERTIES;
                return;
            }

            // Check authorization before reading ...
            Path path = getPath();
            cache.authorizer.checkPermissions(path, Action.READ);
            int depth = cache.getDepthForLoadingNodes();
            if (depth == 1) {
                // Then read the node from the store ...
                org.modeshape.graph.Node persistentNode = cache.store.getNodeAt(getOriginalLocation());
                // Check the actual location ...
                Location actualLocation = persistentNode.getLocation();
                if (!this.location.isSame(actualLocation)) {
                    // The actual location is changed, so update it ...
                    this.originalLocation = actualLocation;
                    // However, the location of the node needs to be a function of the parent node's location,
                    // which has already been loaded by this session ...
                    this.location = actualLocation.with(this.location.getPath()); // copy the ID props, if any
                }
                // Update the persistent information ...
                cache.nodeOperations.materialize(persistentNode, this);
            } else {
                // Then read the node from the store ...
                Subgraph subgraph = cache.store.getSubgraphOfDepth(depth).at(getOriginalLocation());
                Location actualLocation = subgraph.getLocation();
                if (!this.location.isSame(actualLocation)) {
                    // The actual location is changed, so update it ...
                    this.originalLocation = actualLocation;
                    // However, the location of the node needs to be a function of the parent node's location,
                    // which has already been loaded by this session ...
                    this.location = actualLocation.with(this.location.getPath()); // copy the ID props, if any
                }
                // Update the persistent information ...
                cache.nodeOperations.materialize(subgraph.getRoot(), this);
                // Now update any nodes below this node ...
                for (org.modeshape.graph.Node persistentNode : subgraph) {
                    // Find the node at the path ...
                    Path relativePath = persistentNode.getLocation().getPath().relativeTo(path);
                    Node<Payload, PropertyPayload> node = cache.findNodeRelativeTo(this, relativePath);
                    if (!node.isLoaded()) {
                        // Update the persistent information ...
                        cache.nodeOperations.materialize(persistentNode, node);
                    }
                }
            }
        }

        /**
         * Utility method to unload this cached node.
         */
        protected final void unload() {
            assert !isStale();
            assert status == Status.UNCHANGED;
            assert !changedBelow;
            if (!isLoaded()) return;
            cache.recordUnloaded(this);
            childrenByName = null;
            expirationTime = Long.MAX_VALUE;
        }

        /**
         * Phase 1 of the process of refreshing the cached content while retaining changes. This phase walks the entire tree to
         * determine which nodes have changes, which nodes can be unloaded, and which nodes have no changes but are ancestors of
         * those nodes with changes (and therefore have to be refreshed). Each node has a {@link #isChanged(boolean) changed
         * state}, and the supplied RefreshState tracks which nodes must be
         * {@link GraphSession.RefreshState#markAsRequiringRefresh(Node) refreshed} in
         * {@link #refreshPhase2(RefreshState, Results) phase 2}; all other nodes are able to be unloaded in
         * {@link #refreshPhase2(RefreshState, Results) phase 2}.
         * 
         * @param refreshState the holder of the information about which nodes are to be unloaded or refreshed; may not be null
         * @return true if the node could be (or already is) unloaded, or false otherwise
         * @see #refreshPhase2(RefreshState, Results)
         */
        protected final boolean refreshPhase1( RefreshState<Payload, PropertyPayload> refreshState ) {
            assert !isStale();
            if (childrenByName == null) {
                // This node is not yet loaded, so don't record it as needing to be unloaded but return true
                return true;
            }
            // Perform phase 1 on each of the children ...
            boolean canUnloadChildren = true;
            for (Node<Payload, PropertyPayload> child : childrenByName.values()) {
                if (child.refreshPhase1(refreshState)) {
                    // The child can be unloaded
                    canUnloadChildren = false;
                }
            }

            // If this node has changes, then we cannot do anything with this node ...
            if (isChanged(false)) return false;

            // Otherwise, this node contains no changes ...
            if (canUnloadChildren) {
                // Since all the children can be unloaded, we can completely unload this node ...
                return true;
            }
            // Otherwise, we have to hold onto the children, so we can't unload and must be refreshed ...
            refreshState.markAsRequiringRefresh(this);
            return false;
        }

        /**
         * Phase 2 of the process of refreshing the cached content while retaining changes. This phase walks the graph and either
         * unloads the node or, if the node is an ancestor of changed nodes, refreshes the node state to reflect that of the
         * persistent store.
         * 
         * @param refreshState
         * @param persistentInfoForRefreshedNodes
         * @see #refreshPhase1(RefreshState)
         */
        protected final void refreshPhase2( RefreshState<Payload, PropertyPayload> refreshState,
                                            Results persistentInfoForRefreshedNodes ) {
            assert !isStale();
            if (this.status != Status.UNCHANGED) {
                // There are changes, so nothing to do ...
                return;
            }
            if (refreshState.requiresRefresh(this)) {
                // This node must be refreshed since it has no changes but is an ancestor of a node that is changed.
                // Therefore, update the children and properties with the just-read persistent information ...
                assert childrenByName != null;
                org.modeshape.graph.Node persistentNode = persistentInfoForRefreshedNodes.getNode(location);
                assert !persistentNode.getChildren().isEmpty();

                // We need to keep the children that have been modified (or are ancestors of modified children),
                // so build a list of the children that SHOULD NOT be replaced with the persistent info ...
                Map<Location, Node<Payload, PropertyPayload>> childrenToKeep = new HashMap<Location, Node<Payload, PropertyPayload>>();
                for (Node<Payload, PropertyPayload> existing : childrenByName.values()) {
                    if (existing.isChanged(true)) {
                        childrenToKeep.put(existing.getLocation(), existing);
                    } else {
                        // Otherwise, remove the child from the cache since we won't be needing it anymore ...
                        cache.removeNode(existing.getNodeId());
                        existing.parent = null;
                    }
                }

                // Now, clear the children ...
                childrenByName.clear();

                // And add the persistent children ...
                for (Location location : persistentNode.getChildren()) {
                    Name childName = location.getPath().getLastSegment().getName();
                    List<Node<Payload, PropertyPayload>> currentChildren = childrenByName.get(childName);
                    // Find if there was an existing child that is supposed to stay ...
                    Node<Payload, PropertyPayload> existingChild = childrenToKeep.get(location);
                    if (existingChild != null) {
                        // The existing child is supposed to stay, since it has changes ...
                        currentChildren.add(existingChild);
                        if (currentChildren.size() != existingChild.getPath().getLastSegment().getIndex()) {
                            // Make sure the SNS index is correct ...
                            Path.Segment segment = cache.pathFactory.createSegment(childName, currentChildren.size());
                            existingChild.updateLocation(segment);
                            // TODO: Can the location be different? If so, doesn't that mean that the change requests
                            // have to be updated???
                        }
                    } else {
                        // The existing child (if there was one) is to be refreshed ...
                        NodeId nodeId = cache.idFactory.create();
                        Node<Payload, PropertyPayload> replacementChild = cache.createNode(this, nodeId, location);
                        cache.put(replacementChild);
                        assert replacementChild.getName().equals(childName);
                        assert replacementChild.parent == this;
                        // Add it to the parent node ...
                        currentChildren.add(replacementChild);
                        // Create a segment with the SNS ...
                        Path.Segment segment = cache.pathFactory.createSegment(childName, currentChildren.size());
                        replacementChild.updateLocation(segment);
                    }
                }
                return;
            }
            // This node can be unloaded (since it has no changes and isn't above a node with changes) ...
            if (!this.changedBelow) unload();
        }

        /**
         * Define the persistent child information that this node is to be populated with. This method does not cause the node's
         * information to be read from the store.
         * <p>
         * This method is intended to be called by the {@link GraphSession.Operations#materialize(org.modeshape.graph.Node, Node)}
         * , and should not be called by other components.
         * </p>
         * 
         * @param children the children for this node; may not be null
         * @param properties the properties for this node; may not be null
         * @param expirationTime the time that this cached information expires, or null if there is no expiration
         */
        public void loadedWith( List<Location> children,
                                Map<Name, PropertyInfo<PropertyPayload>> properties,
                                DateTime expirationTime ) {
            assert !isStale();
            // Load the children ...
            if (children.isEmpty()) {
                childrenByName = cache.NO_CHILDREN;
            } else {
                childrenByName = LinkedListMultimap.create();
                for (Location location : children) {
                    NodeId id = cache.idFactory.create();
                    Name childName = location.getPath().getLastSegment().getName();
                    Node<Payload, PropertyPayload> child = cache.createNode(this, id, location);
                    cache.put(child);
                    List<Node<Payload, PropertyPayload>> currentChildren = childrenByName.get(childName);
                    currentChildren.add(child);
                    child.parent = this;
                    // Create a segment with the SNS ...
                    Path.Segment segment = cache.pathFactory.createSegment(childName, currentChildren.size());
                    child.updateLocation(segment);
                }
            }

            loadedWith(properties);

            // Set the expiration time ...
            this.expirationTime = expirationTime != null ? expirationTime.getMilliseconds() : Long.MAX_VALUE;
        }

        /**
         * Define the persistent property information that this node is to be populated with. This method does not cause the
         * node's information to be read from the store.
         * 
         * @param properties the properties for this node; may not be null
         */
        public void loadedWith( Map<Name, PropertyInfo<PropertyPayload>> properties ) {
            // Load the properties ...
            if (properties.isEmpty()) {
                this.properties = cache.NO_PROPERTIES;
            } else {
                this.properties = new HashMap<Name, PropertyInfo<PropertyPayload>>(properties);
            }
        }

        /**
         * Reconstruct the location object for this node, given the information at the parent.
         * 
         * @param segment the path segment for this node; may be null only when this node is the root node
         */
        protected void updateLocation( Path.Segment segment ) {
            assert !isStale();
            Path newPath = null;
            if (segment != null) {
                // Recompute the path based upon the parent path ...
                Path parentPath = getParent().getPath();
                newPath = cache.pathFactory.create(parentPath, segment);
            } else {
                if (this.isRoot()) return;
                // This must be the root ...
                newPath = cache.pathFactory.createRootPath();
                assert this.isRoot();
            }
            Location newLocation = this.location.with(newPath);
            if (newLocation != this.location) {
                Location oldLocation = this.location;
                this.location = newLocation;
                cache.nodeOperations.postUpdateLocation(this, oldLocation);
            }

            if (isLoaded() && childrenByName != cache.NO_CHILDREN) {
                // Update all of the children ...
                for (Map.Entry<Name, Collection<Node<Payload, PropertyPayload>>> entry : childrenByName.asMap().entrySet()) {
                    Name childName = entry.getKey();
                    int sns = 1;
                    for (Node<Payload, PropertyPayload> child : entry.getValue()) {
                        Path.Segment childSegment = cache.pathFactory.createSegment(childName, sns++);
                        child.updateLocation(childSegment);
                    }
                }
            }
        }

        /**
         * This method is used to adjust the existing children by adding a child that was recently added to the persistent store
         * (via clone or copy). The new child will appear at the end of the existing children, but before any children that were
         * added to, moved into, created under this parent.
         * 
         * @param newChild the new child that was added
         */
        protected void synchronizeWithNewlyPersistedNode( Location newChild ) {
            if (!this.isLoaded()) return;
            Path childPath = newChild.getPath();
            Name childName = childPath.getLastSegment().getName();
            if (this.childrenByName.isEmpty()) {
                // Just have to add the child ...
                this.childrenByName = LinkedListMultimap.create();
                if (childPath.getLastSegment().hasIndex()) {
                    // The child has a SNS index, but this is an only child ...
                    newChild = newChild.with(cache.pathFactory.create(childPath.getParent(), childName));
                }
                Node<Payload, PropertyPayload> child = cache.createNode(this, cache.idFactory.create(), newChild);
                this.childrenByName.put(childName, child);
                return;
            }

            // Unfortunately, there is no efficient way to insert into the multi-map, so we need to recreate it ...
            ListMultimap<Name, Node<Payload, PropertyPayload>> children = LinkedListMultimap.create();
            boolean added = false;
            for (Node<Payload, PropertyPayload> child : this.childrenByName.values()) {
                if (!added && child.isNew()) {
                    // Add the new child here ...
                    Node<Payload, PropertyPayload> newChildNode = cache.createNode(this, cache.idFactory.create(), newChild);
                    children.put(childName, newChildNode);
                    added = true;
                }
                children.put(child.getName(), child);
            }
            if (!added) {
                Node<Payload, PropertyPayload> newChildNode = cache.createNode(this, cache.idFactory.create(), newChild);
                children.put(childName, newChildNode);
            }

            // Replace the children ...
            this.childrenByName = children;

            // Adjust the SNS indexes for those children with the same name as 'childToBeMoved' ...
            List<Node<Payload, PropertyPayload>> childrenWithName = childrenByName.get(childName);
            int snsIndex = 1;
            for (Node<Payload, PropertyPayload> sns : childrenWithName) {
                if (sns.getSegment().getIndex() != snsIndex) {
                    // The SNS index is not correct, so fix it and update the location ...
                    Path.Segment newSegment = cache.pathFactory.createSegment(childName, snsIndex);
                    sns.updateLocation(newSegment);
                    sns.markAsChanged();
                }
                ++snsIndex;
            }
        }

        /**
         * Determine whether this node has been marked as having changes.
         * 
         * @param recursive true if the nodes under this node should be checked, or false if only this node should be checked
         * @return true if there are changes in the specified scope, or false otherwise
         */
        public final boolean isChanged( boolean recursive ) {
            if (this.status == Status.UNCHANGED) return recursive && this.changedBelow;
            return true;
        }

        /**
         * Determine whether this node has been created since the last save. If this method returns true, then by definition the
         * parent node will be marked as having {@link #isChanged(boolean) changed}.
         * 
         * @return true if this node is new, or false otherwise
         */
        public final boolean isNew() {
            return this.status == Status.NEW;
        }

        /**
         * This method determines whether this node, or any nodes below it, contain changes that depend on nodes that are outside
         * of this branch.
         * 
         * @return true if this branch has nodes with changes dependent on nodes outside of this branch
         */
        public boolean containsChangesWithExternalDependencies() {
            assert !isStale();
            if (!isChanged(true)) {
                // There are no changes in this branch ...
                return false;
            }
            // Need to make sure that nodes were not moved into or out of this branch, since that would mean that we
            // cannot refresh this branch without also refreshing the other affected branches (per the JCR specification) ...
            for (Map.Entry<NodeId, Dependencies> entry : cache.changeDependencies.entrySet()) {
                Dependencies dependency = entry.getValue();
                NodeId nodeId = entry.getKey();
                Node<Payload, PropertyPayload> changedNode = cache.node(nodeId);

                // First, check whether the changed node is within the branch ...
                if (!changedNode.isAtOrBelow(this)) {
                    // The node is not within this branch, so the original parent must not be at or below this node ...
                    if (cache.node(dependency.getMovedFrom()).isAtOrBelow(this)) {
                        // The original parent is below 'this' but the changed node is not ...
                        return true;
                    }
                    // None of the other dependencies can be within this branch ...
                    for (NodeId dependentId : dependency.getRequireChangesTo()) {
                        // The dependent node must not be at or below this node ...
                        if (cache.node(dependentId).isAtOrBelow(this)) {
                            // The other node that must change is at or below 'this'
                            return true;
                        }
                    }
                    // Otherwise, continue with the next change ...
                    continue;
                }
                // The changed node is within this branch!

                // Second, check whether this node was moved from outside this branch ...
                if (dependency.getMovedFrom() != null) {
                    Node<Payload, PropertyPayload> originalParent = cache.node(dependency.getMovedFrom());
                    // If the original parent cannot be found ...
                    if (originalParent == null) {
                        continue;
                    }
                    // The original parent must be at or below this node ...
                    if (!originalParent.isAtOrBelow(this)) {
                        // The original parent is not within this branch (but the new parent is)
                        return true;
                    }
                    // All of the other dependencies must be within this branch ...
                    for (NodeId dependentId : dependency.getRequireChangesTo()) {
                        // The dependent node must not be at or below this node ...
                        if (!cache.node(dependentId).isAtOrBelow(this)) {
                            // Another dependent node is not at or below this branch either ...
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        /**
         * Clear any transient changes that have been accumulated in this node.
         * 
         * @see #markAsChanged()
         */
        public void clearChanges() {
            assert !isStale();
            if (this.status != Status.UNCHANGED) {
                this.status = Status.UNCHANGED;
                this.changedBelow = false;
                unload();
            } else {
                if (!this.changedBelow) return;
                // This node has not changed but something below has, so call to the children ...
                if (childrenByName != null && childrenByName != cache.NO_CHILDREN) {
                    for (Node<Payload, PropertyPayload> child : childrenByName.values()) {
                        child.clearChanges();
                    }
                }
                this.changedBelow = false;
            }
            // Update the parent ...
            if (this.parent != null) this.parent.recomputeChangedBelow();
        }

        /**
         * Mark this node as having changes.
         * 
         * @see #clearChanges()
         * @see #markAsNew()
         */
        public final void markAsChanged() {
            assert !isStale();
            if (this.status == Status.NEW) return;
            this.status = Status.CHANGED;
            if (this.parent != null) this.parent.markAsChangedBelow();
        }

        public final void markAsCopied() {
            assert !isStale();
            this.status = Status.COPIED;
            if (this.parent != null) this.parent.markAsChangedBelow();
        }

        /**
         * Mark this node has having been created and not yet saved.
         * 
         * @see #clearChanges()
         * @see #markAsChanged()
         */
        public final void markAsNew() {
            assert !isStale();
            this.status = Status.NEW;
            if (this.parent != null) this.parent.markAsChanged();
        }

        protected final void markAsChangedBelow() {
            if (!this.changedBelow) {
                this.changedBelow = true;
                if (this.parent != null) this.parent.markAsChangedBelow();
            }
        }

        protected final void recomputeChangedBelow() {
            if (!this.changedBelow) return; // we're done
            // there are changes ...
            assert childrenByName != null;
            for (Node<Payload, PropertyPayload> child : childrenByName.values()) {
                if (child.isChanged(true)) {
                    this.markAsChangedBelow();
                    return;
                }
            }
            // No changes found ...
            this.changedBelow = false;
            if (this.parent != null) this.parent.recomputeChangedBelow();
        }

        /**
         * Move this node from its current location so that is is a child of the supplied parent.
         * 
         * @param parent the new parent for this node; may not be null
         * @throws RepositorySourceException if the parent node is to be loaded but a problem is encountered while doing so
         * @throws IllegalArgumentException if this is the root node
         */
        public void moveTo( Node<Payload, PropertyPayload> parent ) {
            moveTo(parent, null, true);
        }

        /**
         * Move this node from its current location so that is is a child of the supplied parent, renaming the node in the
         * process.
         * 
         * @param parent the new parent for this node; may not be null
         * @param newNodeName the new name for the node, or null if the node should keep the same name
         * @throws RepositorySourceException if the parent node is to be loaded but a problem is encountered while doing so
         * @throws IllegalArgumentException if this is the root node
         */
        public void moveTo( Node<Payload, PropertyPayload> parent,
                            Name newNodeName ) {
            moveTo(parent, newNodeName, true);
        }

        /**
         * Move this node from its current location so that is is a child of the supplied parent.
         * 
         * @param parent the new parent for this node; may not be null
         * @param newNodeName the new name for the node, or null if the node should keep the same name
         * @param useBatch true if this operation should be performed using the session's current batch operation and executed
         *        upon {@link GraphSession#save()}, or false if the move should be performed immediately
         * @throws ValidationException if the supplied parent node is a decendant of this node
         * @throws RepositorySourceException if the parent node is to be loaded but a problem is encountered while doing so
         * @throws IllegalArgumentException if this is the root node
         * @throws AccessControlException if the caller does not have the permission to perform the operation
         */
        protected void moveTo( Node<Payload, PropertyPayload> parent,
                               Name newNodeName,
                               boolean useBatch ) {
            final Node<Payload, PropertyPayload> child = this;
            assert !parent.isStale();
            // Make sure the parent is not a decendant of the child ...
            if (parent.isAtOrBelow(child)) {
                String path = cache.readable(getPath());
                String parentPath = cache.readable(parent.getPath());
                String workspaceName = cache.workspaceName;
                String msg = GraphI18n.unableToMoveNodeToBeChildOfDecendent.text(path, parentPath, workspaceName);
                throw new ValidationException(msg);
            }

            assert !child.isRoot();
            if (newNodeName == null) newNodeName = getName();

            // Check authorization ...
            cache.authorizer.checkPermissions(parent.getPath(), Action.ADD_NODE);
            cache.authorizer.checkPermissions(child.getPath().getParent(), Action.REMOVE);

            parent.load();

            cache.nodeOperations.preMove(child, parent);

            // Remove the child from it's existing parent ...
            final Node<Payload, PropertyPayload> oldParent = child.parent;
            // Record the operation ...
            if (useBatch) {
                if (newNodeName.equals(getName())) {
                    cache.operations.move(child.getLocation()).into(parent.getLocation());
                } else {
                    cache.operations.move(child.getLocation()).as(newNodeName).into(parent.getLocation());
                }
            } else {
                if (newNodeName.equals(getName())) {
                    cache.store.move(child.getLocation()).into(parent.getLocation());
                } else {
                    cache.store.move(child.getLocation()).as(newNodeName).into(parent.getLocation());
                }
            }
            // Remove the child from the current location (even if its the same node; there's cleanup to do) ...
            child.remove();
            // Now add the child ...
            if (parent.childrenByName == cache.NO_CHILDREN) {
                parent.childrenByName = LinkedListMultimap.create();
            }
            parent.childrenByName.put(newNodeName, child);
            child.parent = parent;
            parent.markAsChanged();
            // Update the new child with the correct location ...
            int snsIndex = parent.childrenByName.get(newNodeName).size();
            Path.Segment segment = cache.pathFactory.createSegment(newNodeName, snsIndex);
            child.updateLocation(segment);
            cache.recordMove(child, oldParent, parent);

            cache.nodeOperations.postMove(child, oldParent);
        }

        /**
         * Rename this node to have a different name.
         * 
         * @param newNodeName
         */
        public void rename( Name newNodeName ) {
            moveTo(this.parent, newNodeName, true);
        }

        /**
         * Copy this node (and all nodes below it) and place the copy under the supplied parent location. The new copy will be
         * appended to any existing children of the supplied parent node, and will be given the appropriate same-name-sibling
         * index. This method may not be called on the root node.
         * 
         * @param parent the new parent for the new copy; may not be null
         * @throws RepositorySourceException if the parent node is to be loaded but a problem is encountered while doing so
         * @throws IllegalArgumentException if the parent is null, or if this is the root node
         * @throws AccessControlException if the caller does not have the permission to perform the operation
         */
        public void copyTo( Node<Payload, PropertyPayload> parent ) {
            CheckArg.isNotNull(parent, "parent");
            CheckArg.isEquals(this.isRoot(), "this.isRoot()", false, "false");
            final Node<Payload, PropertyPayload> child = this;
            assert !parent.isStale();
            assert child.parent != this;
            assert !child.isRoot();

            // Check authorization ...
            cache.authorizer.checkPermissions(parent.getPath(), Action.ADD_NODE);
            cache.authorizer.checkPermissions(child.getPath(), Action.READ);

            parent.load();
            if (parent.childrenByName == cache.NO_CHILDREN) {
                parent.childrenByName = LinkedListMultimap.create();
            }

            cache.nodeOperations.preCopy(this, parent);

            Name childName = child.getName();
            // Figure out the name and SNS of the new copy ...
            List<Node<Payload, PropertyPayload>> currentChildren = parent.childrenByName.get(childName);
            Location copyLocation = Location.create(cache.pathFactory.create(parent.getPath(),
                                                                             childName,
                                                                             currentChildren.size() + 1));

            // Perform the copy ...
            cache.operations.copy(child.getLocation()).to(copyLocation);

            // Add the child to the parent ...
            Node<Payload, PropertyPayload> copy = cache.createNode(parent, cache.idFactory.create(), copyLocation);
            copy.markAsCopied(); // marks parent as changed

            cache.nodeOperations.postCopy(this, copy);
        }

        /**
         * Clone this node (and all nodes below it). The new copy will be appended to the existing children of the
         * {@link #getParent() parent}, and will be given the appropriate same-name-sibling index.
         * <p>
         * This is equivalent to calling <code>node.copyTo(node.getParent())</code>
         * </p>
         * 
         * @throws IllegalArgumentException if this is the root node
         */
        public void cloneNode() {
            copyTo(getParent());
        }

        /**
         * Move the specified child to be located immediately before the other supplied node.
         * 
         * @param childToBeMoved the path segment specifying the child that is to be moved
         * @param before the path segment of the node before which the {@code childToBeMoved} should be placed, or null if the
         *        child should be moved to the end
         * @throws PathNotFoundException if the <code>childToBeMoved</code> or <code>before</code> segments do not specify an
         *         existing child
         * @throws IllegalArgumentException if either segment is null or does not specify an existing node
         */
        public void orderChildBefore( Path.Segment childToBeMoved,
                                      Path.Segment before ) throws PathNotFoundException {
            CheckArg.isNotNull(childToBeMoved, "childToBeMoved");

            // Check authorization ...
            cache.authorizer.checkPermissions(getPath(), Action.REMOVE);
            cache.authorizer.checkPermissions(getPath(), Action.ADD_NODE);

            // Find the node to be moved ...
            Node<Payload, PropertyPayload> nodeToBeMoved = getChild(childToBeMoved);
            Node<Payload, PropertyPayload> beforeNode = before != null ? getChild(before) : null;

            if (beforeNode == null) {
                // Moving the node into its parent will remove it from its current spot in the child list and re-add it to the end
                cache.operations.move(nodeToBeMoved.getLocation()).into(this.location);
            } else {
                // Record the move ...
                cache.operations.move(nodeToBeMoved.getLocation()).before(beforeNode.getLocation());
            }

            // Unfortunately, there is no efficient way to insert into the multi-map, so we need to recreate it ...
            ListMultimap<Name, Node<Payload, PropertyPayload>> children = LinkedListMultimap.create();
            for (Node<Payload, PropertyPayload> child : childrenByName.values()) {
                if (child == nodeToBeMoved) continue;
                if (before != null && child.getSegment().equals(before)) {
                    children.put(nodeToBeMoved.getName(), nodeToBeMoved);
                }
                children.put(child.getName(), child);
            }
            if (before == null) {
                children.put(nodeToBeMoved.getName(), nodeToBeMoved);
            }

            // Replace the children ...
            this.childrenByName = children;
            this.markAsChanged();

            // Adjust the SNS indexes for those children with the same name as 'childToBeMoved' ...
            Name movedName = nodeToBeMoved.getName();
            List<Node<Payload, PropertyPayload>> childrenWithName = childrenByName.get(movedName);
            int snsIndex = 1;
            for (Node<Payload, PropertyPayload> sns : childrenWithName) {
                if (sns.getSegment().getIndex() != snsIndex) {
                    // The SNS index is not correct, so fix it and update the location ...
                    Path.Segment newSegment = cache.pathFactory.createSegment(movedName, snsIndex);
                    sns.updateLocation(newSegment);
                    sns.markAsChanged();
                }
                ++snsIndex;
            }
        }

        /**
         * Remove this node from it's parent. Note that locations are <i>not</i> updated, since they will be updated if this node
         * is added to a different parent. However, the locations of same-name-siblings under the parent <i>are</i> updated.
         */
        protected void remove() {
            remove(true);
        }

        /**
         * Remove this node from it's parent. Note that locations are <i>not</i> updated, since they will be updated if this node
         * is added to a different parent. However, the locations of same-name-siblings under the parent <i>are</i> updated.
         * 
         * @param markParentAsChanged true if the parent should be marked as being changed (i.e., when changes are initiated from
         *        within this session), or false otherwise (i.e., when changes are made to reflect the persistent state)
         */
        protected void remove( boolean markParentAsChanged ) {
            assert !isStale();
            assert this.parent != null;
            assert this.parent.isLoaded();
            assert this.parent.childrenByName != null;
            assert this.parent.childrenByName != cache.NO_CHILDREN;
            if (markParentAsChanged) {
                this.parent.markAsChanged();
                this.markAsChanged();
            }
            Name name = getName();
            List<Node<Payload, PropertyPayload>> childrenWithSameName = this.parent.childrenByName.get(name);
            this.parent = null;
            if (childrenWithSameName.size() == 1) {
                // No same-name-siblings ...
                childrenWithSameName.clear();
            } else {
                // There is at least one other sibling with the same name ...
                int lastIndex = childrenWithSameName.size() - 1;
                assert lastIndex > 0;
                int index = childrenWithSameName.indexOf(this);
                // remove this node ...
                childrenWithSameName.remove(index);
                if (index != lastIndex) {
                    // There are same-name-siblings that have higher SNS indexes that this node had ...
                    for (int i = index; i != lastIndex; ++i) {
                        Node<Payload, PropertyPayload> sibling = childrenWithSameName.get(i);
                        Path.Segment segment = cache.pathFactory.createSegment(name, i + 1);
                        sibling.updateLocation(segment);
                    }
                }
            }
        }

        /**
         * Remove this node from it's parent and destroy it's contents. The location of sibling nodes with the same name will be
         * updated, and the node and all nodes below it will be destroyed and removed from the cache.
         * 
         * @throws AccessControlException if the caller does not have the permission to perform the operation
         */
        public void destroy() {
            assert !isStale();
            // Check authorization ...
            cache.authorizer.checkPermissions(getPath(), Action.REMOVE);

            final Node<Payload, PropertyPayload> parent = this.parent;
            cache.nodeOperations.preRemoveChild(parent, this);
            // Remove the node from its parent ...
            remove();
            // This node was successfully removed, so now remove it from the cache ...
            cache.recordDelete(this);
            cache.nodeOperations.postRemoveChild(parent, this);
        }

        public final boolean isRoot() {
            return this.parent == null;
        }

        /**
         * Determine whether this node is stale because it was dropped from the cache.
         * 
         * @return true if the node is stale and should no longer be used
         */
        public boolean isStale() {
            // Find the root of this node ...
            Node<?, ?> node = this;
            while (node.parent != null) {
                node = node.parent;
            }
            // The root of this branch MUST be the actual root of the cache
            return node != cache.root;
        }

        /**
         * Get this node's parent node.
         * 
         * @return the parent node
         */
        public Node<Payload, PropertyPayload> getParent() {
            assert !isStale();
            return parent;
        }

        /**
         * @return nodeId
         */
        public final NodeId getNodeId() {
            return nodeId;
        }

        /**
         * Get the name of this node, without any same-name-sibling index.
         * 
         * @return the name; never null
         */
        public Name getName() {
            return location.getPath().getLastSegment().getName();
        }

        /**
         * Get the {@link Path.Segment path segment} for this node.
         * 
         * @return the path segment; never null
         */
        public final Path.Segment getSegment() {
            return location.getPath().getLastSegment();
        }

        /**
         * Get the current path to this node.
         * 
         * @return the current path; never null
         */
        public final Path getPath() {
            return location.getPath();
        }

        /**
         * Get the current location for this node.
         * 
         * @return the current location; never null
         */
        public final Location getLocation() {
            return location;
        }

        /**
         * Get the original location for this node prior to making any transient changes.
         * 
         * @return the current location; never null
         */
        public final Location getOriginalLocation() {
            return originalLocation;
        }

        /**
         * Create a new child node with the supplied name. The same-name-sibling index will be determined based upon the existing
         * children.
         * 
         * @param name the name of the new child node
         * @return the new child node
         * @throws IllegalArgumentException if the name is null
         * @throws RepositorySourceException if this node must be loaded but doing so results in a problem
         */
        public Node<Payload, PropertyPayload> createChild( Name name ) {
            CheckArg.isNotNull(name, "name");
            return doCreateChild(name, null, null);
        }

        /**
         * Create a new child node with the supplied name and multiple initial properties. The same-name-sibling index will be
         * determined based upon the existing children.
         * 
         * @param name the name of the new child node
         * @param properties the (non-identification) properties for the new node
         * @return the new child node
         * @throws IllegalArgumentException if the name or properties are null
         * @throws ValidationException if the new node is not valid as a child
         * @throws RepositorySourceException if this node must be loaded but doing so results in a problem
         */
        public Node<Payload, PropertyPayload> createChild( Name name,
                                                           Property... properties ) {
            CheckArg.isNotNull(name, "name");
            CheckArg.isNotNull(properties, "properties");
            return doCreateChild(name, null, properties);
        }

        /**
         * Create a new child node with the supplied name and multiple initial identification properties. The same-name-sibling
         * index will be determined based upon the existing children.
         * 
         * @param name the name of the new child node
         * @param idProperties the identification properties for the new node
         * @return the new child node
         * @throws IllegalArgumentException if the name or properties are null
         * @throws ValidationException if the new node is not valid as a child
         * @throws RepositorySourceException if this node must be loaded but doing so results in a problem
         */
        public Node<Payload, PropertyPayload> createChild( Name name,
                                                           Collection<Property> idProperties ) {
            CheckArg.isNotNull(name, "name");
            CheckArg.isNotEmpty(idProperties, "idProperties");
            return doCreateChild(name, idProperties, null);
        }

        /**
         * Create a new child node with the supplied name and multiple initial properties. The same-name-sibling index will be
         * determined based upon the existing children.
         * 
         * @param name the name of the new child node
         * @param idProperties the identification properties for the new node
         * @param remainingProperties the remaining (non-identification) properties for the new node
         * @return the new child node
         * @throws IllegalArgumentException if the name or properties are null
         * @throws ValidationException if the new node is not valid as a child
         * @throws RepositorySourceException if this node must be loaded but doing so results in a problem
         */
        public Node<Payload, PropertyPayload> createChild( Name name,
                                                           Collection<Property> idProperties,
                                                           Property... remainingProperties ) {
            CheckArg.isNotNull(name, "name");
            CheckArg.isNotEmpty(idProperties, "idProperties");
            return doCreateChild(name, idProperties, remainingProperties);
        }

        private Node<Payload, PropertyPayload> doCreateChild( Name name,
                                                              Collection<Property> idProperties,
                                                              Property[] remainingProperties ) throws ValidationException {
            assert !isStale();

            // Check permission here ...
            Path path = getPath();
            cache.authorizer.checkPermissions(path, Action.ADD_NODE);

            // Now load if required ...
            load();

            // Figure out the name and SNS of the new copy ...
            List<Node<Payload, PropertyPayload>> currentChildren = childrenByName.get(name);
            Path newPath = cache.pathFactory.create(path, name, currentChildren.size() + 1);
            Location newChild = idProperties != null && !idProperties.isEmpty() ? Location.create(newPath, idProperties) : Location.create(newPath);

            // Create the properties ...
            Map<Name, PropertyInfo<PropertyPayload>> newProperties = new HashMap<Name, PropertyInfo<PropertyPayload>>();
            if (idProperties != null) {
                for (Property idProp : idProperties) {
                    PropertyInfo<PropertyPayload> info = new PropertyInfo<PropertyPayload>(idProp, idProp.isMultiple(),
                                                                                           Status.NEW, null);
                    newProperties.put(info.getName(), info);
                }
            }
            if (remainingProperties != null) {
                for (Property property : remainingProperties) {
                    PropertyInfo<PropertyPayload> info2 = new PropertyInfo<PropertyPayload>(property, property.isMultiple(),
                                                                                            Status.NEW, null);
                    newProperties.put(info2.getName(), info2);
                }
            }

            // Notify before the addition ...
            cache.nodeOperations.preCreateChild(this, newPath.getLastSegment(), newProperties);

            // Record the current state before any changes ...
            Status statusBefore = this.status;
            boolean changedBelowBefore = this.changedBelow;

            // Add the child to the parent ...
            Node<Payload, PropertyPayload> child = cache.createNode(this, cache.idFactory.create(), newChild);
            child.markAsNew(); // marks parent as changed
            if (childrenByName == cache.NO_CHILDREN) {
                childrenByName = LinkedListMultimap.create();
            }
            childrenByName.put(name, child);

            // Set the properties on the new node, but in a private backdoor way ...
            assert child.properties == null;
            child.properties = newProperties;
            child.childrenByName = cache.NO_CHILDREN;

            try {
                // The node has been changed, so try notifying before we record the creation (which can't be undone) ...
                cache.nodeOperations.postCreateChild(this, child, child.properties);

                // Notification was fine, so now do the create ...
                Graph.Create<Graph.Batch> create = cache.operations.create(newChild.getPath());
                if (!child.properties.isEmpty()) {
                    // Process the property infos (in case some were added during the pre- or post- operations ...
                    for (PropertyInfo<PropertyPayload> property : child.properties.values()) {
                        create.with(property.getProperty());
                    }
                }
                create.and();
            } catch (ValidationException e) {
                // Clean up the children ...
                if (childrenByName.size() == 1) {
                    childrenByName = cache.NO_CHILDREN;
                } else {
                    childrenByName.remove(child.getName(), child);
                }
                this.status = statusBefore;
                this.changedBelow = changedBelowBefore;
                throw e;
            }

            cache.put(child);

            return child;
        }

        /**
         * Determine whether this node has a child with the supplied name and SNS index.
         * 
         * @param segment the segment of the child
         * @return true if there is a child, or false if there is no such child
         * @throws RepositorySourceException if there is a problem loading this node's information from the store
         */
        public boolean hasChild( Path.Segment segment ) {
            return hasChild(segment.getName(), segment.getIndex());
        }

        /**
         * Determine whether this node has a child with the supplied name and SNS index.
         * 
         * @param name the name of the child
         * @param sns the same-name-sibling index; must be 1 or more
         * @return true if there is a child, or false if there is no such child
         * @throws RepositorySourceException if there is a problem loading this node's information from the store
         */
        public boolean hasChild( Name name,
                                 int sns ) {
            load();
            List<Node<Payload, PropertyPayload>> children = childrenByName.get(name); // never null
            return children.size() >= sns; // SNS is 1-based, index is 0-based
        }

        /**
         * Get the child with the supplied segment.
         * 
         * @param segment the segment of the child
         * @return the child with the supplied name and SNS index, or null if the children have not yet been loaded
         * @throws PathNotFoundException if the children have been loaded and the child does not exist
         * @throws RepositorySourceException if there is a problem loading this node's information from the store
         */
        public Node<Payload, PropertyPayload> getChild( Path.Segment segment ) {
            return getChild(segment.getName(), segment.getIndex());
        }

        /**
         * Get the first child matching the name and lowest SNS index
         * 
         * @param name the name of the child
         * @return the first child with the supplied name, or null if the children have not yet been loaded
         * @throws PathNotFoundException if the children have been loaded and the child does not exist
         * @throws RepositorySourceException if there is a problem loading this node's information from the store
         */
        public Node<Payload, PropertyPayload> getFirstChild( Name name ) {
            return getChild(name, 1);
        }

        /**
         * Get the child with the supplied name and SNS index.
         * 
         * @param name the name of the child
         * @param sns the same-name-sibling index; must be 1 or more
         * @return the child with the supplied name and SNS index; never null
         * @throws PathNotFoundException if the children have been loaded and the child does not exist
         * @throws RepositorySourceException if there is a problem loading this node's information from the store
         */
        public Node<Payload, PropertyPayload> getChild( Name name,
                                                        int sns ) {
            load();
            List<Node<Payload, PropertyPayload>> children = childrenByName.get(name); // never null
            try {
                return children.get(sns - 1); // SNS is 1-based, index is 0-based
            } catch (IndexOutOfBoundsException e) {
                Path missingPath = cache.pathFactory.create(getPath(), name, sns);
                throw new PathNotFoundException(Location.create(missingPath), getPath());
            }
        }

        /**
         * Get an iterator over the children that have the supplied name.
         * 
         * @param name the of the child nodes to be returned; may not be null
         * @return an unmodifiable iterator over the cached children that have the supplied name; never null but possibly empty
         * @throws RepositorySourceException if there is a problem loading this node's information from the store
         */
        public Iterable<Node<Payload, PropertyPayload>> getChildren( Name name ) {
            load();
            final Collection<Node<Payload, PropertyPayload>> children = childrenByName.get(name);
            return new Iterable<Node<Payload, PropertyPayload>>() {
                public Iterator<Node<Payload, PropertyPayload>> iterator() {
                    return new ReadOnlyIterator<Node<Payload, PropertyPayload>>(children.iterator());
                }
            };
        }

        /**
         * Get an iterator over the children.
         * 
         * @return an unmodifiable iterator over the cached children; never null
         * @throws RepositorySourceException if there is a problem loading this node's information from the store
         */
        public Iterable<Node<Payload, PropertyPayload>> getChildren() {
            load();
            final Collection<Node<Payload, PropertyPayload>> children = childrenByName.values();
            return new Iterable<Node<Payload, PropertyPayload>>() {
                public Iterator<Node<Payload, PropertyPayload>> iterator() {
                    return new ReadOnlyIterator<Node<Payload, PropertyPayload>>(children.iterator());
                }
            };
        }

        /**
         * Get the child node that immediately follows the supplied child.
         * 
         * @param child the existing child; must be a child of this node
         * @return the node that appears directly after the supplied child, or null if the supplied child is the last child
         */
        public Node<Payload, PropertyPayload> getChildAfter( Node<Payload, PropertyPayload> child ) {
            assert child.getParent() == this;
            if (getChildrenCount() < 2) return null;
            Iterator<Node<Payload, PropertyPayload>> iter = getChildren().iterator();
            while (iter.hasNext()) {
                Node<Payload, PropertyPayload> nextChild = iter.next();
                if (child.equals(nextChild)) {
                    return iter.hasNext() ? iter.next() : null;
                }
            }
            assert false;
            return null;
        }

        /**
         * Get the number of children.
         * 
         * @return the number of children in the cache
         * @throws RepositorySourceException if there is a problem loading this node's information from the store
         */
        public int getChildrenCount() {
            load();
            return childrenByName.size();
        }

        /**
         * Get the number of children that have the same supplied name.
         * 
         * @param name the name of the children to count
         * @return the number of children in the cache
         * @throws RepositorySourceException if there is a problem loading this node's information from the store
         */
        public int getChildrenCount( Name name ) {
            load();
            return childrenByName.get(name).size();
        }

        /**
         * Determine if this node is a leaf node with no children.
         * 
         * @return true if this node has no children
         * @throws RepositorySourceException if there is a problem loading this node's information from the store
         */
        public boolean isLeaf() {
            load();
            return childrenByName.isEmpty();
        }

        /**
         * Get from this node the property with the supplied name.
         * 
         * @param name the property name; may not be null
         * @return the property with the supplied name, or null if there is no such property on this node
         */
        public PropertyInfo<PropertyPayload> getProperty( Name name ) {
            load();
            return properties.get(name);
        }

        /**
         * Set the supplied property information on this node.
         * 
         * @param property the new property; may not be null
         * @param isMultiValued true if the property is multi-valued
         * @param payload the optional payload for this property; may be null
         * @return the previous information for the property, or null if there was no previous property
         * @throws AccessControlException if the caller does not have the permission to perform the operation
         */
        public PropertyInfo<PropertyPayload> setProperty( Property property,
                                                          boolean isMultiValued,
                                                          PropertyPayload payload ) {
            assert !isStale();
            cache.authorizer.checkPermissions(getPath(), Action.SET_PROPERTY);

            load();
            if (properties == cache.NO_PROPERTIES) {
                properties = new HashMap<Name, PropertyInfo<PropertyPayload>>();
            }

            Name name = property.getName();
            PropertyInfo<PropertyPayload> previous = properties.get(name);
            Status status = null;
            if (previous != null) {
                status = previous.getStatus(); // keep NEW or CHANGED status, but UNCHANGED -> CHANGED
                if (status == Status.UNCHANGED) status = Status.CHANGED;
            } else {
                status = Status.NEW;
            }
            PropertyInfo<PropertyPayload> info = new PropertyInfo<PropertyPayload>(property, isMultiValued, status, payload);
            cache.nodeOperations.preSetProperty(this, property.getName(), info);
            properties.put(name, info);
            cache.operations.set(property).on(location);
            markAsChanged();
            cache.nodeOperations.postSetProperty(this, property.getName(), previous);
            return previous;
        }

        /**
         * Remove a property from this node.
         * 
         * @param name the name of the property to be removed; may not be null
         * @return the previous information for the property, or null if there was no previous property
         */
        public PropertyInfo<PropertyPayload> removeProperty( Name name ) {
            assert !isStale();
            cache.authorizer.checkPermissions(getPath(), Action.REMOVE);

            load();
            if (!properties.containsKey(name)) return null;
            cache.nodeOperations.preRemoveProperty(this, name);
            PropertyInfo<PropertyPayload> results = properties.remove(name);
            markAsChanged();
            cache.operations.remove(name).on(location);
            cache.nodeOperations.postRemoveProperty(this, name, results);
            return results;
        }

        /**
         * Get the names of the properties on this node.
         * 
         * @return the names of the properties; never null
         */
        public Set<Name> getPropertyNames() {
            load();
            return properties.keySet();
        }

        /**
         * Get the information for each of the properties on this node.
         * 
         * @return the information for each of the properties; never null
         */
        public Collection<PropertyInfo<PropertyPayload>> getProperties() {
            load();
            return properties.values();
        }

        /**
         * Get the number of properties owned by this node.
         * 
         * @return the number of properties; never negative
         */
        public int getPropertyCount() {
            load();
            return properties.size();
        }

        public boolean isAtOrBelow( Node<Payload, PropertyPayload> other ) {
            Node<Payload, PropertyPayload> node = this;
            while (node != null) {
                if (node == other) return true;
                node = node.getParent();
            }
            return false;
        }

        /**
         * @return payload
         */
        public Payload getPayload() {
            load();
            return payload;
        }

        /**
         * @param payload Sets payload to the specified value.
         */
        public void setPayload( Payload payload ) {
            this.payload = payload;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#hashCode()
         */
        @Override
        public final int hashCode() {
            return nodeId.hashCode();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @SuppressWarnings( "unchecked" )
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof Node) {
                Node<Payload, PropertyPayload> that = (Node<Payload, PropertyPayload>)obj;
                if (this.isStale() || that.isStale()) return false;
                if (!this.nodeId.equals(that.nodeId)) return false;
                return this.location.isSame(that.location);
            }
            return false;
        }

        /**
         * Utility method to obtain a string representation that uses the namespace prefixes where appropriate.
         * 
         * @param registry the namespace registry, or null if no prefixes should be used
         * @return the string representation; never null
         */
        public String getString( NamespaceRegistry registry ) {
            return "Cached node <" + nodeId + "> at " + location.getString(registry);
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return getString(null);
        }

        /**
         * Visit all nodes in the cache that are already loaded
         * 
         * @param visitor the visitor; may not be null
         */
        public void onLoadedNodes( NodeVisitor<Payload, PropertyPayload> visitor ) {
            if (this.isLoaded()) {
                // Create a queue. This queue will contain all the nodes to be visited,
                // so if loading is not forced, then the queue should only contain already-loaded nodes...
                LinkedList<Node<Payload, PropertyPayload>> queue = new LinkedList<Node<Payload, PropertyPayload>>();
                queue.add(this);
                while (!queue.isEmpty()) {
                    Node<Payload, PropertyPayload> node = queue.poll();
                    // Get an iterator over the children *before* we visit the node
                    Iterator<Node<Payload, PropertyPayload>> iter = node.getChildren().iterator();
                    // Visit this node ...
                    if (visitor.visit(node)) {
                        // Visit the children ...
                        int index = -1;
                        while (iter.hasNext()) {
                            Node<Payload, PropertyPayload> child = iter.next();
                            if (child.isLoaded()) {
                                queue.add(++index, child);
                            }
                        }
                    }
                }
            }
            visitor.finish();
        }

        /**
         * Visit all loaded and unloaded nodes in the cache.
         * 
         * @param visitor the visitor; may not be null
         */
        public void onCachedNodes( NodeVisitor<Payload, PropertyPayload> visitor ) {
            // Create a queue. This queue will contain all the nodes to be visited,
            // so if loading is not forced, then the queue should only contain already-loaded nodes...
            LinkedList<Node<Payload, PropertyPayload>> queue = new LinkedList<Node<Payload, PropertyPayload>>();
            queue.add(this);
            while (!queue.isEmpty()) {
                Node<Payload, PropertyPayload> node = queue.poll();
                if (!node.isLoaded()) {
                    visitor.visit(node);
                    continue;
                }
                // Get an iterator over the children *before* we visit the node
                Iterator<Node<Payload, PropertyPayload>> iter = node.getChildren().iterator();
                // Visit this node ...
                if (visitor.visit(node)) {
                    // Visit the children ...
                    int index = -1;
                    while (iter.hasNext()) {
                        Node<Payload, PropertyPayload> child = iter.next();
                        queue.add(++index, child);
                    }
                }
            }
            visitor.finish();
        }

        /**
         * Visit all changed nodes in the cache.
         * 
         * @param visitor the visitor; may not be null
         */
        public void onChangedNodes( NodeVisitor<Payload, PropertyPayload> visitor ) {
            if (this.isChanged(true)) {
                // Create a queue. This queue will contain all the nodes to be visited ...
                LinkedList<Node<Payload, PropertyPayload>> changedNodes = new LinkedList<Node<Payload, PropertyPayload>>();
                changedNodes.add(this);
                while (!changedNodes.isEmpty()) {
                    Node<Payload, PropertyPayload> node = changedNodes.poll();
                    // Visit this node ...
                    boolean visitChildren = true;
                    if (node.isChanged(false)) {
                        visitChildren = visitor.visit(node);
                    }
                    if (visitChildren && node.isChanged(true)) {
                        // Visit the children ...
                        int index = -1;
                        Iterator<Node<Payload, PropertyPayload>> iter = node.getChildren().iterator();
                        while (iter.hasNext()) {
                            Node<Payload, PropertyPayload> child = iter.next();
                            if (node.isChanged(true)) {
                                changedNodes.add(++index, child);
                            }
                        }
                    }
                }
            }
            visitor.finish();
        }

        /**
         * Obtain a snapshot of the structure below this node.
         * 
         * @param pathsOnly true if the snapshot should only include paths, or false if the entire locations should be included
         * @return the snapshot
         */
        public StructureSnapshot<PropertyPayload> getSnapshot( final boolean pathsOnly ) {
            final List<Snapshot<PropertyPayload>> snapshots = new ArrayList<Snapshot<PropertyPayload>>();
            onCachedNodes(new NodeVisitor<Payload, PropertyPayload>() {
                @Override
                public boolean visit( Node<Payload, PropertyPayload> node ) {
                    snapshots.add(new Snapshot<PropertyPayload>(node, pathsOnly, true));
                    return node.isLoaded();
                }
            });
            return new StructureSnapshot<PropertyPayload>(cache.context().getNamespaceRegistry(),
                                                          Collections.unmodifiableList(snapshots));
        }
    }

    public static enum Status {
        NEW,
        CHANGED,
        UNCHANGED,
        COPIED;
    }

    public static final class PropertyInfo<PropertyPayload> {
        private final Property property;
        private final Status status;
        private final boolean multiValued;
        private final PropertyPayload payload;

        public PropertyInfo( Property property,
                             boolean multiValued,
                             Status status,
                             PropertyPayload payload ) {
            assert property != null;
            assert status != null;
            this.property = property;
            this.status = status;
            this.multiValued = multiValued;
            this.payload = payload;
        }

        /**
         * Get the status of this property.
         * 
         * @return the current status; never null
         */
        public Status getStatus() {
            return status;
        }

        /**
         * Determine whether this property has been modified since it was last saved.
         * 
         * @return true if the {@link #getStatus() status} is {@link Status#CHANGED changed}
         */
        public boolean isModified() {
            return status != Status.UNCHANGED && status != Status.NEW;
        }

        /**
         * Determine whether this property has been created since the last save.
         * 
         * @return true if the {@link #getStatus() status} is {@link Status#NEW new}
         */
        public boolean isNew() {
            return status == Status.NEW;
        }

        /**
         * Get the name of the property.
         * 
         * @return the propert name; never null
         */
        public Name getName() {
            return property.getName();
        }

        /**
         * Get the Graph API property object containing the values.
         * 
         * @return the property object; never null
         */
        public Property getProperty() {
            return property;
        }

        /**
         * Get the payload for this property.
         * 
         * @return the payload; may be null if there is no payload
         */
        public PropertyPayload getPayload() {
            return payload;
        }

        /**
         * Determine whether this property has multiple values
         * 
         * @return multiValued
         */
        public boolean isMultiValued() {
            return multiValued;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            return getName().hashCode();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof PropertyInfo<?>) {
                PropertyInfo<?> that = (PropertyInfo<?>)obj;
                return getName().equals(that.getName());
            }
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(getName());
            // if (payload != null) sb.append(payload);
            if (property.isSingle()) {
                sb.append(" with value ");
            } else {
                sb.append(" with values ");
            }
            sb.append(Arrays.asList(property.getValuesAsArray()));
            return sb.toString();
        }
    }

    /**
     * The node visitor.
     * 
     * @param <NodePayload> the type of node payload object
     * @param <PropertyPayloadType> the type of property payload object
     */
    @NotThreadSafe
    public static abstract class NodeVisitor<NodePayload, PropertyPayloadType> {
        /**
         * Visit the supplied node, returning whether the children should be visited.
         * 
         * @param node the node to be visited; never null
         * @return true if the node's children should be visited, or false if no children should be visited
         */
        public abstract boolean visit( Node<NodePayload, PropertyPayloadType> node );

        /**
         * Method that should be called after all visiting has been done successfully (with no exceptions), including when no
         * nodes were visited.
         */
        public void finish() {
        }
    }

    /**
     * An abstract base class for visitors that need to load nodes using a single batch for all read operations. To use, simply
     * subclass and supply a {@link #visit(Node)} implementation that calls {@link #load(Node)} for each node that is to be
     * loaded. When the visitor is {@link #finish() finished}, all of these nodes will be read from the store and loaded. The
     * {@link #finishNodeAfterLoading(Node)} is called after each node is loaded, allowing the subclass to perform an operation on
     * the newly-loaded nodes.
     */
    @NotThreadSafe
    protected abstract class LoadNodesVisitor extends NodeVisitor<Payload, PropertyPayload> {
        private Graph.Batch batch = GraphSession.this.store.batch();
        private List<Node<Payload, PropertyPayload>> nodesToLoad = new LinkedList<Node<Payload, PropertyPayload>>();

        /**
         * Method that signals that the supplied node should be loaded (if it is not already loaded). This method should be called
         * from within the {@link #visit(Node)} method of the subclass.
         * 
         * @param node the node that should be loaded (if it is not already)
         */
        protected void load( Node<Payload, PropertyPayload> node ) {
            if (node != null && !node.isLoaded() && !node.isNew()) {
                nodesToLoad.add(node);
                batch.read(node.getOriginalLocation());
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see GraphSession.NodeVisitor#finish()
         */
        @Override
        public void finish() {
            super.finish();
            if (!nodesToLoad.isEmpty()) {
                // Read all of the children in one batch ...
                Results results = batch.execute();
                // Now load all of the children into the correct node ...
                for (Node<Payload, PropertyPayload> childToBeRead : nodesToLoad) {
                    org.modeshape.graph.Node persistentNode = results.getNode(childToBeRead.getOriginalLocation());
                    nodeOperations.materialize(persistentNode, childToBeRead);
                    finishNodeAfterLoading(childToBeRead);
                }
            }
        }

        /**
         * Method that is called on each node loaded by this visitor. This method does nothing by default.
         * 
         * @param node the just-loaded node; never null
         */
        protected void finishNodeAfterLoading( Node<Payload, PropertyPayload> node ) {
            // do nothing
        }
    }

    /**
     * A visitor that ensures that all children of a node are loaded, and provides a hook to {@link #finishNodeAfterLoading(Node)
     * post-process the parent}.
     */
    @NotThreadSafe
    protected class LoadAllChildrenVisitor extends LoadNodesVisitor {
        private List<Node<Payload, PropertyPayload>> parentsVisited = new LinkedList<Node<Payload, PropertyPayload>>();

        /**
         * {@inheritDoc}
         * 
         * @see GraphSession.NodeVisitor#visit(GraphSession.Node)
         */
        @Override
        public boolean visit( Node<Payload, PropertyPayload> node ) {
            parentsVisited.add(node);
            Iterator<Node<Payload, PropertyPayload>> iter = node.getChildren().iterator();
            while (iter.hasNext()) {
                load(iter.next());
            }
            return true;
        }

        /**
         * {@inheritDoc}
         * 
         * @see GraphSession.LoadNodesVisitor#finish()
         */
        @Override
        public void finish() {
            super.finish();
            for (Node<Payload, PropertyPayload> parent : parentsVisited) {
                finishParentAfterLoading(parent);
            }
        }

        /**
         * Method that is called at the end of the {@link #finish()} stage with each parent node whose children were all loaded.
         * 
         * @param parentNode the parent of the just-loaded children; never null
         */
        protected void finishParentAfterLoading( Node<Payload, PropertyPayload> parentNode ) {
            // do nothing
        }
    }

    protected static final class Snapshot<PropertyPayload> {
        private final Location location;
        private final boolean isLoaded;
        private final boolean isChanged;
        private final Collection<PropertyInfo<PropertyPayload>> properties;
        private final NodeId id;

        protected Snapshot( Node<?, PropertyPayload> node,
                            boolean pathsOnly,
                            boolean includeProperties ) {
            this.location = pathsOnly && node.getLocation().hasIdProperties() ? Location.create(node.getLocation().getPath()) : node.getLocation();
            this.isLoaded = node.isLoaded();
            this.isChanged = node.isChanged(false);
            this.id = node.getNodeId();
            this.properties = includeProperties ? node.getProperties() : null;
        }

        /**
         * @return location
         */
        public Location getLocation() {
            return location;
        }

        /**
         * @return isChanged
         */
        public boolean isChanged() {
            return isChanged;
        }

        /**
         * @return isLoaded
         */
        public boolean isLoaded() {
            return isLoaded;
        }

        /**
         * @return id
         */
        public NodeId getId() {
            return id;
        }

        /**
         * @return properties
         */
        public Collection<PropertyInfo<PropertyPayload>> getProperties() {
            return properties;
        }
    }

    /**
     * A read-only visitor that walks the cache to obtain a snapshot of the cache structure. The resulting snapshot contains the
     * location of each node in the tree, including unloaded nodes.
     * 
     * @param <PropertyPayload> the property payload
     */
    @Immutable
    public static final class StructureSnapshot<PropertyPayload> implements Iterable<Snapshot<PropertyPayload>> {
        private final List<Snapshot<PropertyPayload>> snapshotsInPreOrder;
        private final NamespaceRegistry registry;

        protected StructureSnapshot( NamespaceRegistry registry,
                                     List<Snapshot<PropertyPayload>> snapshotsInPreOrder ) {
            assert snapshotsInPreOrder != null;
            this.snapshotsInPreOrder = snapshotsInPreOrder;
            this.registry = registry;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Iterable#iterator()
         */
        public Iterator<Snapshot<PropertyPayload>> iterator() {
            return snapshotsInPreOrder.iterator();
        }

        /**
         * Get the Location for every node in this cache
         * 
         * @return the node locations (in pre-order)
         */
        public List<Snapshot<PropertyPayload>> getSnapshotsInPreOrder() {
            return snapshotsInPreOrder;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            int maxLength = 0;
            for (Snapshot<PropertyPayload> snapshot : this) {
                String path = snapshot.getLocation().getPath().getString(registry);
                maxLength = Math.max(maxLength, path.length());
            }
            StringBuilder sb = new StringBuilder();
            for (Snapshot<PropertyPayload> snapshot : this) {
                Location location = snapshot.getLocation();
                sb.append(StringUtil.justifyLeft(location.getPath().getString(registry), maxLength, ' '));
                // Append the node identifier ...
                sb.append(StringUtil.justifyRight(snapshot.getId().toString(), 10, ' '));
                // Append the various state flags
                if (snapshot.isChanged()) sb.append(" (*)");
                else if (!snapshot.isLoaded()) sb.append(" (-)");
                else sb.append("    ");
                // Append the location's identifier properties ...
                if (location.hasIdProperties()) {
                    sb.append("  ");
                    if (location.getIdProperties().size() == 1 && location.getUuid() != null) {
                        sb.append(location.getUuid());
                    } else {
                        boolean first = true;
                        sb.append('[');
                        for (Property property : location) {
                            sb.append(property.getString(registry));
                            if (first) first = false;
                            else sb.append(", ");
                        }
                        sb.append(']');
                    }
                }
                // Append the property information ...
                if (snapshot.getProperties() != null) {
                    boolean first = true;
                    sb.append("  {");
                    for (PropertyInfo<?> info : snapshot.getProperties()) {
                        if (first) first = false;
                        else sb.append("} {");
                        sb.append(info.getProperty().getString(registry));
                    }
                    sb.append("}");
                }
                sb.append("\n");
            }
            return sb.toString();
        }
    }

    @NotThreadSafe
    protected static final class RefreshState<Payload, PropertyPayload> {
        private final Set<Node<Payload, PropertyPayload>> refresh = new HashSet<Node<Payload, PropertyPayload>>();

        public void markAsRequiringRefresh( Node<Payload, PropertyPayload> node ) {
            refresh.add(node);
        }

        public boolean requiresRefresh( Node<Payload, PropertyPayload> node ) {
            return refresh.contains(node);
        }

        public Set<Node<Payload, PropertyPayload>> getNodesToBeRefreshed() {
            return refresh;
        }
    }

    @NotThreadSafe
    protected final static class Dependencies {
        private Set<NodeId> requireChangesTo;
        private NodeId movedFrom;

        public Dependencies() {
        }

        /**
         * @return movedFrom
         */
        public NodeId getMovedFrom() {
            return movedFrom;
        }

        /**
         * Record that this node is being moved from one parent to another. This method only records the original parent, so
         * subsequent calls to this method do nothing.
         * 
         * @param movedFrom the identifier of the original parent of this node
         */
        public void setMovedFrom( NodeId movedFrom ) {
            if (this.movedFrom == null) this.movedFrom = movedFrom;
        }

        /**
         * @return requireChangesTo
         */
        public Set<NodeId> getRequireChangesTo() {
            return requireChangesTo != null ? requireChangesTo : Collections.<NodeId>emptySet();
        }

        /**
         * @param other the other node that changes are dependent upon
         */
        public void addRequireChangesTo( NodeId other ) {
            if (other == null) return;
            if (requireChangesTo == null) {
                requireChangesTo = new HashSet<NodeId>();
            }
            requireChangesTo.add(other);
        }
    }

    /**
     * An immutable identifier for a node, used within the {@link GraphSession}.
     */
    @Immutable
    public final static class NodeId {

        private final long nodeId;

        /**
         * Create a new node identifier.
         * 
         * @param nodeId unique identifier
         */
        public NodeId( long nodeId ) {
            this.nodeId = nodeId;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            return (int)nodeId;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof NodeId) {
                NodeId that = (NodeId)obj;
                return this.nodeId == that.nodeId;
            }
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return Long.toString(nodeId);
        }
    }

}
