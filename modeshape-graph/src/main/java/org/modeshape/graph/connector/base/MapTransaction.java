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
package org.modeshape.graph.connector.base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.RandomAccess;
import java.util.Set;
import java.util.UUID;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.common.util.StringUtil;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.Location;
import org.modeshape.graph.connector.RepositorySourceException;
import org.modeshape.graph.connector.UuidAlreadyExistsException;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathNotFoundException;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.PropertyType;
import org.modeshape.graph.property.Reference;
import org.modeshape.graph.property.UuidFactory;
import org.modeshape.graph.property.ValueFactory;
import org.modeshape.graph.property.Path.Segment;
import org.modeshape.graph.query.QueryResults;
import org.modeshape.graph.request.AccessQueryRequest;
import org.modeshape.graph.request.FullTextSearchRequest;

/**
 * An implementation of {@link Transaction} that maintains a cache of nodes by their hash (or {@link UUID}).
 * 
 * @param <WorkspaceType> the type of workspace
 * @param <NodeType> the type of node
 */
@NotThreadSafe
public abstract class MapTransaction<NodeType extends MapNode, WorkspaceType extends MapWorkspace<NodeType>>
    extends BaseTransaction<NodeType, WorkspaceType> {

    /** The set of changes to the workspaces that have been made by this transaction */
    private Map<String, WorkspaceChanges> changesByWorkspaceName;
    /** The set of nodes that have been read during this transaction */
    private Map<WorkspaceType, Map<UUID, NodeType>> nodesByWorkspaceName;

    /**
     * Create a new transaction.
     * 
     * @param context the execution context for this transaction; may not be null
     * @param repository the repository against which the transaction will be operating; may not be null
     * @param rootNodeUuid the UUID of the root node; may not be null
     */
    protected MapTransaction( ExecutionContext context,
                              Repository<NodeType, WorkspaceType> repository,
                              UUID rootNodeUuid ) {
        super(context, repository, rootNodeUuid);
    }

    /**
     * Get the changes for the supplied workspace, optionally creating the necessary object if it does not yet exist. The changes
     * object is used to record the changes made to the workspace by operations within this transaction, which are either pushed
     * into the workspace upon {@link #commit()} or cleared upon {@link #rollback()}.
     * 
     * @param workspace the workspace
     * @param createIfMissing true if the changes object should be created if it does not yet exist, or false otherwise
     * @return the changes object; may be null if <code>createIfMissing</code> is <code>false</code> and the changes object does
     *         not yet exist, or never null if <code>createIfMissing</code> is <code>true</code>
     */
    protected WorkspaceChanges getChangesFor( WorkspaceType workspace,
                                              boolean createIfMissing ) {
        if (changesByWorkspaceName == null) {
            if (!createIfMissing) return null;
            WorkspaceChanges changes = new WorkspaceChanges(workspace);
            changesByWorkspaceName = new HashMap<String, WorkspaceChanges>();
            changesByWorkspaceName.put(workspace.getName(), changes);
            return changes;
        }
        WorkspaceChanges changes = changesByWorkspaceName.get(workspace.getName());
        if (changes == null && createIfMissing) {
            changes = new WorkspaceChanges(workspace);
            changesByWorkspaceName.put(workspace.getName(), changes);
        }
        return changes;
    }

    /**
     * Gets the map of changed nodes for the supplied workspace, populating the {@link #nodesByWorkspaceName transaction cache} as
     * needed.
     * 
     * @param workspace the workspace; may not be null
     * @return the map of changed nodes for that workspace; never null
     */
    protected Map<UUID, NodeType> getCachedNodesfor( WorkspaceType workspace ) {
        if (nodesByWorkspaceName == null) {
            nodesByWorkspaceName = new HashMap<WorkspaceType, Map<UUID, NodeType>>();
        }

        Map<UUID, NodeType> cachedNodes = nodesByWorkspaceName.get(workspace);
        if (cachedNodes == null) {
            cachedNodes = new HashMap<UUID, NodeType>();
            nodesByWorkspaceName.put(workspace, cachedNodes);
        }

        return cachedNodes;
    }

    /**
     * Returns the node with the given UUID in the given workspace from the transaction read cache, if it exists in that cache.
     * Otherwise, this method uses {@link MapWorkspace#getNode(UUID)} to retrieve the node directly from the workspace.
     * <p>
     * The values returned by this method do not take any node modifications or deletions from this session into consideration.
     * </p>
     * 
     * @param workspace the workspace from which the node should be read; may not be null
     * @param uuid the UUID of the node to read; may not be null
     * @return the node with the given UUID in the given workspace
     */
    private NodeType getNode( WorkspaceType workspace,
                              UUID uuid ) {
        Map<UUID, NodeType> cachedNodes = getCachedNodesfor(workspace);
        NodeType cachedNode = cachedNodes.get(uuid);
        if (cachedNode == null) {
            cachedNode = workspace.getNode(uuid);
            cachedNodes.put(uuid, cachedNode);
        }

        return cachedNode;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.Transaction#getNode(org.modeshape.graph.connector.base.Workspace,
     *      org.modeshape.graph.Location)
     */
    public NodeType getNode( WorkspaceType workspace,
                             Location location ) {
        assert location != null;
        // First look for the UUID ...
        UUID uuid = location.getUuid();
        if (uuid != null) {
            WorkspaceChanges changes = getChangesFor(workspace, false);
            NodeType node = null;
            if (changes != null) {
                // See if the node we're looking for was deleted ...
                if (changes.isRemoved(uuid)) {
                    // This node was removed within this transaction ...
                    Path lowestExisting = null;
                    if (location.hasPath()) {
                        getNode(workspace, location.getPath(), location); // should fail
                        assert false;
                    }
                    lowestExisting = pathFactory.createRootPath();
                    throw new PathNotFoundException(location, lowestExisting, GraphI18n.nodeDoesNotExist.text(readable(location)));
                }
                // Not deleted, but maybe changed in this transaction ...
                node = changes.getChangedOrAdded(uuid);
                if (node != null) return node;
            }
            // It hasn't been loaded already, so attempt to load it from the map owned by the workspace ...
            node = getNode(workspace, uuid);
            if (node != null) return node;
        }
        // Otherwise, look by path ...
        if (location.hasPath()) {
            return getNode(workspace, location.getPath(), location);
        }
        // Unable to find by UUID or by path, so fail ...
        Path lowestExisting = pathFactory.createRootPath();
        throw new PathNotFoundException(location, lowestExisting, GraphI18n.nodeDoesNotExist.text(readable(location)));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.Transaction#verifyNodeExists(org.modeshape.graph.connector.base.Workspace,
     *      org.modeshape.graph.Location)
     */
    public Location verifyNodeExists( WorkspaceType workspace,
                                      Location location ) {
        NodeType node = getNode(workspace, location);
        if (location.hasPath() && location.getUuid() != null) return location;
        if (location.hasPath()) {
            if (location.getUuid() != null) return location;
            // Missing UUID ...
            return location.with(node.getUuid());
        }
        Path path = pathFor(workspace, node);
        return location.with(path);
    }

    /**
     * Find the latest version of the supplied node.
     * 
     * @param workspace the workspace
     * @param node the node
     * @return the latest version of the node, with or without changes
     */
    protected NodeType findLatest( WorkspaceType workspace,
                                   NodeType node ) {
        if (node.hasChanges()) return node;
        return findNode(workspace, node.getUuid());
    }

    /**
     * Attempt to find the node with the supplied UUID.
     * 
     * @param workspace the workspace; may not be null
     * @param uuid the UUID of the node; may not be null
     * @return the node, or null if no such node exists
     */
    protected NodeType findNode( WorkspaceType workspace,
                                 UUID uuid ) {
        WorkspaceChanges changes = getChangesFor(workspace, false);
        NodeType node = null;
        if (changes != null) {
            // See if the node we're looking for was deleted ...
            if (changes.isRemoved(uuid)) {
                // This node was removed within this transaction ...
                return null;
            }
            // Not deleted, but maybe changed in this transaction ...
            node = changes.getChangedOrAdded(uuid);
            if (node != null) return node;
        }
        // It hasn't been loaded already, so attempt to load it from the map owned by the workspace ...
        node = getNode(workspace, uuid);
        return node;
    }

    /**
     * Destroy the node.
     * 
     * @param workspace the workspace; never null
     * @param node the node to be destroyed
     */
    protected void destroyNode( WorkspaceType workspace,
                                NodeType node ) {
        WorkspaceChanges changes = getChangesFor(workspace, true);
        destroyNode(changes, workspace, node);
    }

    /**
     * Destroy the node and it's contents.
     * 
     * @param changes the record of the workspace changes; never null
     * @param workspace the workspace; never null
     * @param node the node to be destroyed
     */
    private void destroyNode( WorkspaceChanges changes,
                              WorkspaceType workspace,
                              NodeType node ) {
        // First destroy the children ...
        for (UUID childUuid : node.getChildren()) {
            NodeType child = changes.getChangedOrAdded(childUuid);
            if (child == null) {
                // The node was not changed or added, so go back to the workspace ...
                child = getNode(workspace, childUuid);
            }
            destroyNode(changes, workspace, child);
        }
        // Then remove the requested node ...
        UUID uuid = node.getUuid();
        changes.removed(uuid);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.Transaction#addChild(org.modeshape.graph.connector.base.Workspace,
     *      org.modeshape.graph.connector.base.Node, org.modeshape.graph.property.Name, int, java.util.UUID, java.lang.Iterable)
     */
    @SuppressWarnings( "unchecked" )
    public NodeType addChild( WorkspaceType workspace,
                              NodeType parent,
                              Name name,
                              int index,
                              UUID uuid,
                              Iterable<Property> properties ) {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        parent = findLatest(workspace, parent);

        WorkspaceChanges changes = getChangesFor(workspace, true);
        NodeType newNode = null;
        if (index < 0) {
            // Figure out the SNS of the new node ...
            int snsIndex = 1;
            for (NodeType child : getChildren(workspace, parent)) {
                if (child.getName().getName().equals(name)) ++snsIndex;
            }

            // Create the new node ...
            newNode = createNode(uuid, pathFactory.createSegment(name, snsIndex), parent.getUuid(), properties);
            // And add to the parent ...
            parent = (NodeType)parent.withChild(uuid);
        } else {
            int snsIndex = 0;
            ListIterator<NodeType> existingSiblings = getChildren(workspace, parent).listIterator(index);
            while (existingSiblings.hasNext()) {
                NodeType existingSibling = existingSiblings.next();
                Segment existingSegment = existingSibling.getName();
                if (existingSegment.getName().equals(name)) {
                    int existingIndex = existingSegment.getIndex();
                    if (snsIndex == 0) snsIndex = existingIndex;
                    existingSibling = (NodeType)existingSibling.withName(pathFactory.createSegment(name, existingIndex + 1));
                    changes.changed(existingSibling);
                }
            }
            // Create the new node ...
            newNode = createNode(uuid, pathFactory.createSegment(name, snsIndex + 1), parent.getUuid(), properties);
            // And add to the parent ...
            parent = (NodeType)parent.withChild(index, uuid);
        }
        changes.created(newNode);
        changes.changed(parent);
        return newNode;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.Transaction#addChild(org.modeshape.graph.connector.base.Workspace,
     *      org.modeshape.graph.connector.base.Node, org.modeshape.graph.connector.base.Node,
     *      org.modeshape.graph.connector.base.Node, org.modeshape.graph.property.Name)
     */
    @SuppressWarnings( "unchecked" )
    public Location addChild( WorkspaceType workspace,
                              NodeType parent,
                              NodeType newChild,
                              NodeType beforeOtherChild,
                              Name desiredName ) {
        parent = findLatest(workspace, parent);

        // Get some information about the child ...
        Segment newChildSegment = newChild.getName();
        Name newChildName = newChildSegment.getName();
        int snsIndex = newChildSegment.getIndex();
        UUID newChildUuid = newChild.getUuid();

        // Find the existing parent of the new child ...
        NodeType oldParent = getParent(workspace, newChild);

        // Find the changes for this workspace ...
        WorkspaceChanges changes = getChangesFor(workspace, true);

        // if (oldParent == parent && beforeOtherChild == null) {
        // // this node is being renamed, so find the correct index ...
        // index = parent.getChildren().indexOf(newChildUuid);
        // assert index >= 0;
        // } else if (oldParent != null) {
        int oldIndex = -1;
        if (oldParent != null) {
            // Remove the node from it's parent ...
            oldIndex = oldParent.getChildren().indexOf(newChildUuid);
            if (oldParent == parent) {
                oldParent = (NodeType)oldParent.withoutChild(newChildUuid);
                changes.changed(oldParent);
                parent = oldParent;
            } else {
                oldParent = (NodeType)oldParent.withoutChild(newChildUuid);
                changes.changed(oldParent);
            }

            // Now find any siblings with the same name that appear after the node in the parent's list of children ...
            List<NodeType> siblings = getChildren(workspace, oldParent);
            for (ListIterator<NodeType> iter = siblings.listIterator(oldIndex); iter.hasNext();) {
                NodeType sibling = iter.next();
                if (sibling.getName().getName().equals(newChildName)) {
                    sibling = (NodeType)sibling.withName(pathFactory.createSegment(newChildName, snsIndex++));
                    changes.changed(sibling);
                }
            }
        }

        // Find the index of the other child ...
        int index = parent.getChildren().size();
        if (beforeOtherChild != null) {
            if (!beforeOtherChild.getParent().equals(parent.getUuid())) {
                // The other child doesn't exist in the parent ...
                throw new RepositorySourceException(null);
            }
            UUID otherChild = beforeOtherChild.getUuid();
            index = parent.getChildren().indexOf(otherChild);
            if (index == -1) index = parent.getChildren().size();
        }
        assert index >= 0;

        // Determine the desired new name for the node ...
        newChildName = desiredName != null ? desiredName : newChildName;

        // Find the SNS index for the new child ...
        ListIterator<NodeType> existingSiblings = getChildren(workspace, parent).listIterator(); // makes a copy
        int i = 0;
        snsIndex = 1;
        Segment childName = null;
        while (existingSiblings.hasNext()) {
            NodeType existingSibling = existingSiblings.next();
            Segment existingSegment = existingSibling.getName();
            if (i < index) {
                // Nodes before the insertion point
                if (existingSegment.getName().equals(newChildName)) {
                    ++snsIndex;
                }
            } else {
                if (i == index) {
                    // Add the child node ...
                    childName = pathFactory.createSegment(newChildName, snsIndex);
                }
                if (existingSegment.getName().equals(newChildName)) {
                    existingSibling = (NodeType)existingSibling.withName(pathFactory.createSegment(newChildName, ++snsIndex));
                    changes.changed(existingSibling);
                }
            }
            ++i;
        }
        if (childName == null) {
            // Must be appending the child ...
            childName = pathFactory.createSegment(newChildName, snsIndex);
        }

        // Change the name of the new node ...
        newChild = (NodeType)newChild.withName(childName).withParent(parent.getUuid());
        parent = (NodeType)parent.withChild(index, newChildUuid);
        changes.changed(newChild);
        changes.changed(parent);
        return Location.create(pathFor(workspace, newChild), newChildUuid);
    }

    /**
     * Create a new instance of the node, given the supplied UUID. This method should do nothing but instantiate the new node; the
     * caller will add to the appropriate maps.
     * 
     * @param uuid the desired UUID; never null
     * @param name the name of the new node; may be null if the name is not known
     * @param parentUuid the UUID of the parent node; may be null if this is the root node
     * @param properties the properties; may be null if there are no properties
     * @return the new node; never null
     */
    @SuppressWarnings( "unchecked" )
    protected NodeType createNode( UUID uuid,
                                   Segment name,
                                   UUID parentUuid,
                                   Iterable<Property> properties ) {
        return (NodeType)new MapNode(uuid, name, parentUuid, properties, null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.Transaction#getChild(org.modeshape.graph.connector.base.Workspace,
     *      org.modeshape.graph.connector.base.Node, org.modeshape.graph.property.Path.Segment)
     */
    public NodeType getChild( WorkspaceType workspace,
                              NodeType parent,
                              Segment childSegment ) {
        parent = findLatest(workspace, parent);
        List<NodeType> children = new Children(parent.getChildren(), workspace); // don't make a copy
        for (NodeType child : children) {
            if (child.getName().equals(childSegment)) return child;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.Transaction#getChildren(org.modeshape.graph.connector.base.Workspace,
     *      org.modeshape.graph.connector.base.Node)
     */
    public List<NodeType> getChildren( WorkspaceType workspace,
                                       NodeType node ) {
        node = findLatest(workspace, node);
        List<UUID> children = node.getChildren(); // make a copy
        if (children.isEmpty()) return Collections.emptyList();
        return new Children(children, workspace);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.Transaction#getChildrenLocations(org.modeshape.graph.connector.base.Workspace,
     *      org.modeshape.graph.connector.base.Node)
     */
    public List<Location> getChildrenLocations( WorkspaceType workspace,
                                                NodeType node ) {
        node = findLatest(workspace, node);
        List<UUID> children = node.getChildren(); // make a copy
        if (children.isEmpty()) return Collections.emptyList();
        List<Location> locations = new ArrayList<Location>(children.size());
        for (UUID uuid : children) {
            NodeType child = findNode(workspace, uuid);
            locations.add(Location.create(this.pathFor(workspace, child), uuid));
        }
        return locations;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.Transaction#getParent(org.modeshape.graph.connector.base.Workspace,
     *      org.modeshape.graph.connector.base.Node)
     */
    public NodeType getParent( WorkspaceType workspace,
                               NodeType node ) {
        node = findLatest(workspace, node);
        UUID parentUuid = node.getParent();
        if (parentUuid == null) return null;
        return getNode(workspace, Location.create(parentUuid));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.Transaction#removeAllChildren(org.modeshape.graph.connector.base.Workspace,
     *      org.modeshape.graph.connector.base.Node)
     */
    @SuppressWarnings( "unchecked" )
    public void removeAllChildren( WorkspaceType workspace,
                                   NodeType node ) {
        boolean hadChanges = node.hasChanges();
        node = findLatest(workspace, node);
        for (NodeType child : getChildren(workspace, node)) {
            destroyNode(workspace, child);
        }
        node = (NodeType)node.withoutChildren();
        if (!hadChanges) {
            getChangesFor(workspace, true).changed(node);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.Transaction#removeNode(org.modeshape.graph.connector.base.Workspace,
     *      org.modeshape.graph.connector.base.Node)
     */
    @SuppressWarnings( "unchecked" )
    public Location removeNode( WorkspaceType workspace,
                                NodeType node ) {
        node = findLatest(workspace, node);

        NodeType parent = getParent(workspace, node);
        if (parent == null) {
            // The root node is being removed, which means we should just delete everything (except the root) ...
            WorkspaceChanges changes = getChangesFor(workspace, true);
            changes.removeAll(createNode(rootNodeUuid, null, null, null));
            return Location.create(pathFactory.createRootPath(), rootNodeUuid);
        }
        Location result = Location.create(pathFor(workspace, node), node.getUuid());

        // Find the index of the node in it's parent ...
        int index = parent.getChildren().indexOf(node.getUuid());
        assert index != -1;
        Name name = node.getName().getName();
        int snsIndex = node.getName().getIndex();
        // Remove the node from the parent ...
        parent = (NodeType)parent.withoutChild(node.getUuid());
        WorkspaceChanges changes = getChangesFor(workspace, true);
        changes.changed(parent);

        // Now find any siblings with the same name that appear after the node in the parent's list of children ...
        List<NodeType> siblings = getChildren(workspace, parent);
        if (index < siblings.size()) {
            for (ListIterator<NodeType> iter = siblings.listIterator(index); iter.hasNext();) {
                NodeType sibling = iter.next();
                if (sibling.getName().getName().equals(name)) {
                    sibling = (NodeType)sibling.withName(pathFactory.createSegment(name, snsIndex++));
                    changes.changed(sibling);
                }
            }
        }
        // Destroy the subgraph starting at the node, and record the change on the parent ...
        destroyNode(changes, workspace, node);
        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.Transaction#removeProperty(org.modeshape.graph.connector.base.Workspace,
     *      org.modeshape.graph.connector.base.Node, org.modeshape.graph.property.Name)
     */
    @SuppressWarnings( "unchecked" )
    public NodeType removeProperty( WorkspaceType workspace,
                                    NodeType node,
                                    Name propertyName ) {
        node = findLatest(workspace, node);

        // Remove the property ...
        NodeType copy = (NodeType)node.withoutProperty(propertyName);
        if (copy != node) {
            // These were the first changes on the node, so record the new change ...
            WorkspaceChanges changes = getChangesFor(workspace, true);
            changes.changed(copy);
        }
        return copy;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.Transaction#setProperties(org.modeshape.graph.connector.base.Workspace,
     *      org.modeshape.graph.connector.base.Node, java.lang.Iterable, java.lang.Iterable, boolean)
     */
    @SuppressWarnings( "unchecked" )
    public NodeType setProperties( WorkspaceType workspace,
                                   NodeType node,
                                   Iterable<Property> propertiesToSet,
                                   Iterable<Name> propertiesToRemove,
                                   boolean removeAllExisting ) {
        node = findLatest(workspace, node);

        // Apply the property changes...
        NodeType copy = (NodeType)node.withProperties(propertiesToSet, propertiesToRemove, removeAllExisting);
        if (copy != node) {
            // These were the first changes on the node, so record the new change ...
            WorkspaceChanges changes = getChangesFor(workspace, true);
            changes.changed(copy);
        }
        return copy;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.Transaction#copyNode(org.modeshape.graph.connector.base.Workspace,
     *      org.modeshape.graph.connector.base.Node, org.modeshape.graph.connector.base.Workspace,
     *      org.modeshape.graph.connector.base.Node, org.modeshape.graph.property.Name, boolean)
     */
    @SuppressWarnings( "unchecked" )
    public NodeType copyNode( WorkspaceType originalWorkspace,
                              NodeType original,
                              WorkspaceType newWorkspace,
                              NodeType newParent,
                              Name desiredName,
                              boolean recursive ) {
        if (desiredName == null) desiredName = original.getName().getName();
        // Create a copy of the original under the new parent ...
        UUID uuid = UUID.randomUUID();
        NodeType copy = addChild(newWorkspace, newParent, desiredName, -1, uuid, original.getProperties().values());

        Map<UUID, UUID> oldToNewUuids = new HashMap<UUID, UUID>();
        oldToNewUuids.put(original.getUuid(), uuid);

        if (recursive) {
            WorkspaceChanges changes = getChangesFor(newWorkspace, true);
            // Walk through the original branch in its workspace ...
            for (NodeType originalChild : getChildren(originalWorkspace, original)) {
                NodeType newChild = copyBranch(originalWorkspace,
                                               originalChild,
                                               changes,
                                               newWorkspace,
                                               copy,
                                               false,
                                               oldToNewUuids);
                copy = (NodeType)copy.withChild(newChild.getUuid());
            }
        }

        // Record the latest changes on the newly-created node ..
        WorkspaceChanges changes = getChangesFor(newWorkspace, true);
        changes.changed(copy);

        // Now, adjust any references in the new subgraph to objects in the original subgraph
        // (because they were internal references, and need to be internal to the new subgraph)
        UuidFactory uuidFactory = context.getValueFactories().getUuidFactory();
        ValueFactory<Reference> referenceFactory = context.getValueFactories().getReferenceFactory();
        for (Map.Entry<UUID, UUID> oldToNew : oldToNewUuids.entrySet()) {
            NodeType newNode = findNode(newWorkspace, oldToNew.getValue());
            assert newNode != null;
            // Iterate over the properties of the new ...
            for (Map.Entry<Name, Property> entry : newNode.getProperties().entrySet()) {
                Property property = entry.getValue();
                // Now see if any of the property values are references ...
                List<Object> newValues = new ArrayList<Object>();
                boolean foundReference = false;
                for (Iterator<?> iter = property.getValues(); iter.hasNext();) {
                    Object value = iter.next();
                    PropertyType type = PropertyType.discoverType(value);
                    if (type == PropertyType.REFERENCE) {
                        UUID oldReferencedUuid = uuidFactory.create(value);
                        UUID newReferencedUuid = oldToNewUuids.get(oldReferencedUuid);
                        if (newReferencedUuid != null) {
                            newValues.add(referenceFactory.create(newReferencedUuid));
                            foundReference = true;
                        }
                    } else {
                        newValues.add(value);
                    }
                }
                // If we found at least one reference, we have to build a new Property object ...
                if (foundReference) {
                    Property newProperty = propertyFactory.create(property.getName(), newValues);
                    newNode = (NodeType)newNode.withProperty(newProperty);
                    changes.changed(newNode);
                }
            }
        }

        return copy;
    }

    protected void print( WorkspaceType workspace,
                          NodeType node,
                          int level ) {
        StringBuilder sb = new StringBuilder();
        sb.append(StringUtil.createString(' ', level * 2));
        sb.append(readable(node.getName())).append(" (").append(node.getUuid()).append(") {");
        boolean first = true;
        for (Property property : node.getProperties().values()) {
            if (first) first = false;
            else sb.append(',');
            sb.append(readable(property.getName())).append('=');
            if (property.isMultiple()) sb.append(property.getValuesAsArray());
            else sb.append(readable(property.getFirstValue()));
        }
        sb.append('}');
        System.out.println(sb);
        for (NodeType child : getChildren(workspace, node)) {
            print(workspace, child, level + 1);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.Transaction#cloneNode(org.modeshape.graph.connector.base.Workspace,
     *      org.modeshape.graph.connector.base.Node, org.modeshape.graph.connector.base.Workspace,
     *      org.modeshape.graph.connector.base.Node, org.modeshape.graph.property.Name, org.modeshape.graph.property.Path.Segment,
     *      boolean, java.util.Set)
     */
    @SuppressWarnings( "unchecked" )
    public NodeType cloneNode( WorkspaceType originalWorkspace,
                               NodeType original,
                               WorkspaceType newWorkspace,
                               NodeType newParent,
                               Name desiredName,
                               Segment desiredSegment,
                               boolean removeExisting,
                               java.util.Set<Location> removedExistingNodes )
        throws org.modeshape.graph.connector.UuidAlreadyExistsException {

        WorkspaceChanges changes = getChangesFor(newWorkspace, true);
        NodeType newNode = null;

        // System.out.println("Original workspace under " + pathFor(originalWorkspace, original));
        // print(originalWorkspace, original, 0);
        // System.out.println("New workspace under " + pathFor(newWorkspace, newParent));
        // print(newWorkspace, newParent, 0);
        Set<UUID> uuidsInFromBranch = getUuidsUnderNode(originalWorkspace, original);
        NodeType existing = null;
        if (removeExisting) {
            // Remove all of the nodes that have a UUID from under the original node, but DO NOT yet remove
            // a node that has the UUID of the original node (as this will be handled later) ...
            for (UUID uuid : uuidsInFromBranch) {
                if (null != (existing = findNode(newWorkspace, uuid))) {
                    if (removedExistingNodes != null) {
                        Path path = pathFor(newWorkspace, existing);
                        removedExistingNodes.add(Location.create(path, uuid));
                    }
                    removeNode(newWorkspace, existing);
                }
            }
            // System.out.println("New workspace under " + pathFor(newWorkspace, newParent) + " (after removing existing nodes)");
            // print(newWorkspace, newParent, 0);
        } else {
            uuidsInFromBranch.add(original.getUuid()); // uuidsInFromBranch does not include the UUID of the original
            for (UUID uuid : uuidsInFromBranch) {
                if (null != (existing = findNode(newWorkspace, uuid))) {
                    NamespaceRegistry namespaces = context.getNamespaceRegistry();
                    String path = pathFor(newWorkspace, existing).getString(namespaces);
                    throw new UuidAlreadyExistsException(getRepository().getSourceName(), uuid, path, newWorkspace.getName());
                }
            }
        }

        UUID uuid = original.getUuid();
        if (desiredSegment != null) {
            // Look for an existing child under 'newParent' that has that name ...
            int index = 0;
            List<NodeType> children = getChildren(newWorkspace, newParent);
            NodeType existingWithSameName = null;
            for (NodeType child : children) {
                if (child.getName().equals(desiredSegment)) {
                    existingWithSameName = child;
                    break;
                }
                ++index;
            }
            if (index == children.size()) {
                // Couldn't find the desired segment, so just append ...
                newNode = addChild(newWorkspace, newParent, original.getName().getName(), -1, uuid, original.getProperties()
                                                                                                            .values());
            } else {
                // Create the new node with the desired name ...
                newNode = createNode(uuid, desiredSegment, newParent.getUuid(), original.getProperties().values());

                // Destroy the existing node ...
                assert existingWithSameName != null;
                destroyNode(newWorkspace, existingWithSameName);

                // Replace the existing node ...
                newParent = (NodeType)newParent.withoutChild(existingWithSameName.getUuid()).withChild(index, uuid);
            }
        } else {
            // Need to remove the existing node with the same UUID ...
            existing = findNode(newWorkspace, original.getUuid());
            if (existing != null) {
                if (removedExistingNodes != null) {
                    Path path = pathFor(newWorkspace, existing);
                    removedExistingNodes.add(Location.create(path, original.getUuid()));
                }
                removeNode(newWorkspace, existing);
            }

            if (desiredName != null) {
                // Simply add a new node with the desired name ...
                newNode = addChild(newWorkspace, newParent, desiredName, -1, uuid, original.getProperties().values());
            } else {
                // Simply append and use the original's name ...
                newNode = addChild(newWorkspace, newParent, original.getName().getName(), -1, uuid, original.getProperties()
                                                                                                            .values());
            }
        }

        // If the parent doesn't already have changes, we need to find the new parent in the newWorkspace's changes
        if (!newParent.hasChanges()) {
            newParent = findNode(newWorkspace, newParent.getUuid());
        }

        // Walk through the original branch in its workspace ...
        for (NodeType originalChild : getChildren(originalWorkspace, original)) {
            NodeType newChild = copyBranch(originalWorkspace, originalChild, changes, newWorkspace, newNode, true, null);
            newNode = (NodeType)newNode.withChild(newChild.getUuid());
        }
        changes.created(newNode);
        changes.changed(newParent);

        return newNode;
    }

    /**
     * Returns all of the UUIDs in the branch rooted at {@code node}. The UUID of {@code node} <i>will</i> be included in the set
     * of returned UUIDs.
     * 
     * @param workspace the workspace
     * @param node the root of the branch
     * @return all of the UUIDs in the branch rooted at {@code node}
     */
    protected Set<UUID> getUuidsUnderNode( WorkspaceType workspace,
                                           NodeType node ) {
        Set<UUID> uuids = new HashSet<UUID>();
        uuidsUnderNode(workspace, node, uuids);
        return uuids;
    }

    private void uuidsUnderNode( WorkspaceType workspace,
                                 NodeType node,
                                 Set<UUID> accumulator ) {
        for (NodeType child : getChildren(workspace, node)) {
            accumulator.add(child.getUuid());
            uuidsUnderNode(workspace, child, accumulator);
        }
    }

    @SuppressWarnings( "unchecked" )
    protected NodeType copyBranch( WorkspaceType originalWorkspace,
                                   NodeType original,
                                   WorkspaceChanges newWorkspaceChanges,
                                   WorkspaceType newWorkspace,
                                   NodeType newParent,
                                   boolean reuseUuid,
                                   Map<UUID, UUID> oldToNewUuids ) {
        // Create the new node (or reuse the original if we can) ...
        UUID copyUuid = reuseUuid ? original.getUuid() : UUID.randomUUID();
        NodeType copy = createNode(copyUuid, original.getName(), newParent.getUuid(), original.getProperties().values());
        newWorkspaceChanges.created(copy);
        if (!reuseUuid) {
            assert oldToNewUuids != null;
            oldToNewUuids.put(original.getUuid(), copy.getUuid());
        }

        // Walk through the children and call this method recursively ...
        for (NodeType originalChild : getChildren(originalWorkspace, original)) {
            NodeType newChild = copyBranch(originalWorkspace,
                                           originalChild,
                                           newWorkspaceChanges,
                                           newWorkspace,
                                           copy,
                                           reuseUuid,
                                           oldToNewUuids);
            copy = (NodeType)copy.withChild(newChild.getUuid());
        }
        newWorkspaceChanges.changed(copy);
        return copy;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation does not support querying the repository contents, so this method returns null. Subclasses can override
     * this if they do support querying.
     * </p>
     * 
     * @see org.modeshape.graph.connector.base.Transaction#query(org.modeshape.graph.connector.base.Workspace,
     *      org.modeshape.graph.request.AccessQueryRequest)
     */
    public QueryResults query( WorkspaceType workspace,
                               AccessQueryRequest accessQuery ) {
        return null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation does not support searching the repository contents, so this method returns null. Subclasses can
     * override this if they do support searching.
     * </p>
     * 
     * @see org.modeshape.graph.connector.base.Transaction#search(org.modeshape.graph.connector.base.Workspace,
     *      org.modeshape.graph.request.FullTextSearchRequest)
     */
    public QueryResults search( WorkspaceType workspace,
                                FullTextSearchRequest search ) {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.BaseTransaction#commit()
     */
    @Override
    public void commit() {
        super.commit();
        // Push all of the changes or added nodes onto the workspace ...
        if (changesByWorkspaceName != null) {
            for (WorkspaceChanges changes : changesByWorkspaceName.values()) {
                changes.commit();
            }
            changesByWorkspaceName.clear();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.BaseTransaction#rollback()
     */
    @Override
    public void rollback() {
        super.rollback();
        if (changesByWorkspaceName != null) {
            changesByWorkspaceName.clear();
        }
    }

    /**
     * Record of the changes made to a particular workspace.
     */
    protected class WorkspaceChanges {
        private final WorkspaceType workspace;
        private final Map<UUID, NodeType> changedOrAddedNodes = new HashMap<UUID, NodeType>();
        private final Set<UUID> removedNodes = new HashSet<UUID>();
        private boolean removeAll = false;

        protected WorkspaceChanges( WorkspaceType workspace ) {
            this.workspace = workspace;
        }

        public WorkspaceType getWorkspace() {
            return workspace;
        }

        public void removeAll( NodeType newRootNode ) {
            changedOrAddedNodes.clear();
            removedNodes.clear();
            removeAll = true;
            changedOrAddedNodes.put(newRootNode.getUuid(), newRootNode);
        }

        public boolean isRemoved( UUID uuid ) {
            return removedNodes.contains(uuid);
        }

        public NodeType getChangedOrAdded( UUID uuid ) {
            return changedOrAddedNodes.get(uuid);
        }

        public void removed( UUID uuid ) {
            removedNodes.add(uuid);
            changedOrAddedNodes.remove(uuid);
        }

        public void created( NodeType node ) {
            UUID uuid = node.getUuid();
            removedNodes.remove(uuid);
            changedOrAddedNodes.put(uuid, node);
        }

        public void changed( NodeType node ) {
            UUID uuid = node.getUuid();
            assert !removedNodes.contains(uuid); // should not be removed
            changedOrAddedNodes.put(uuid, node);
        }

        @SuppressWarnings( "unchecked" )
        public void commit() {
            if (removeAll) {
                workspace.removeAll();
            }
            for (NodeType changed : changedOrAddedNodes.values()) {
                workspace.putNode((NodeType)changed.freeze());
            }
            for (UUID uuid : removedNodes) {
                // the node may not exist in the workspace (i.e., it was created and then deleted in the txn)
                // but this method call won't care ...
                workspace.removeNode(uuid);
            }
        }
    }

    protected class Children implements List<NodeType> {
        private final List<UUID> uuids;
        private final WorkspaceType workspace;
        private final List<NodeType> cache;

        protected Children( List<UUID> uuids,
                            WorkspaceType workspace ) {
            // If the UUIDs list is small enough (e.g., say a size less than '10') or a RandomAccess list, we can
            // simply just use the list. However, if the list of UUIDs is a larger LinkedLists, then random
            // access will be inefficient, so we need to copy to an ArrayList ...
            this.uuids = (uuids instanceof RandomAccess || uuids.size() < 10) ? uuids : new ArrayList<UUID>(uuids);
            this.workspace = workspace;
            this.cache = new ArrayList<NodeType>(uuids.size());
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.List#size()
         */
        public int size() {
            return uuids.size();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.List#contains(java.lang.Object)
         */
        public boolean contains( Object o ) {
            if (o instanceof MapNode) {
                return uuids.contains(((MapNode)o).getUuid());
            }
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.List#containsAll(java.util.Collection)
         */
        public boolean containsAll( Collection<?> c ) {
            for (Object o : c) {
                if (!contains(o)) return false;
            }
            return true;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.List#get(int)
         */
        public NodeType get( int index ) {
            NodeType result = null;
            if (cache.size() > index) {
                result = cache.get(index);
            }
            if (result == null) {
                UUID uuid = uuids.get(index);
                // Do the fast lookup first, but this returns a null if not found ...
                result = findNode(workspace, uuid);
                if (result == null) {
                    // Wasn't found, so we need to throw an exception ...
                    result = getNode(workspace, Location.create(uuid));
                }
                while (cache.size() <= index) {
                    cache.add(null);
                }
                cache.set(index, result);
            }
            return result;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.List#indexOf(java.lang.Object)
         */
        public int indexOf( Object o ) {
            if (o instanceof MapNode) {
                return uuids.indexOf(((MapNode)o).getUuid());
            }
            return -1;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.List#lastIndexOf(java.lang.Object)
         */
        public int lastIndexOf( Object o ) {
            // The list of UUIDs cannot contain duplicates, so just do the simple thing here ...
            return indexOf(0);
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.List#isEmpty()
         */
        public boolean isEmpty() {
            return uuids.isEmpty();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.List#iterator()
         */
        public Iterator<NodeType> iterator() {
            return listIterator(0);
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.List#listIterator()
         */
        public ListIterator<NodeType> listIterator() {
            return listIterator(0);
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.List#listIterator(int)
         */
        public ListIterator<NodeType> listIterator( final int index ) {
            final int maxIndex = size();
            return new ListIterator<NodeType>() {
                private int current = index;

                public boolean hasNext() {
                    return current < maxIndex;
                }

                public NodeType next() {
                    return get(current++);
                }

                public void remove() {
                    throw new UnsupportedOperationException();
                }

                public void add( NodeType e ) {
                    throw new UnsupportedOperationException();
                }

                public boolean hasPrevious() {
                    return current > 0;
                }

                public int nextIndex() {
                    return current;
                }

                public NodeType previous() {
                    return get(--current);
                }

                public int previousIndex() {
                    return current - 1;
                }

                public void set( NodeType e ) {
                    throw new UnsupportedOperationException();
                }
            };
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.List#subList(int, int)
         */
        public List<NodeType> subList( int fromIndex,
                                       int toIndex ) {
            return new Children(uuids.subList(fromIndex, toIndex), workspace);
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.List#toArray()
         */
        public Object[] toArray() {
            final int length = uuids.size();
            Object[] result = new Object[length];
            for (int i = 0; i != length; ++i) {
                result[i] = get(i);
            }
            return result;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.List#toArray(T[])
         */
        @SuppressWarnings( "unchecked" )
        public <T> T[] toArray( T[] a ) {
            final int length = uuids.size();
            for (int i = 0; i != length; ++i) {
                a[i] = (T)get(i);
            }
            return a;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.List#add(java.lang.Object)
         */
        public boolean add( NodeType e ) {
            throw new UnsupportedOperationException();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.List#add(int, java.lang.Object)
         */
        public void add( int index,
                         NodeType element ) {
            throw new UnsupportedOperationException();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.List#addAll(java.util.Collection)
         */
        public boolean addAll( Collection<? extends NodeType> c ) {
            throw new UnsupportedOperationException();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.List#addAll(int, java.util.Collection)
         */
        public boolean addAll( int index,
                               Collection<? extends NodeType> c ) {
            throw new UnsupportedOperationException();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.List#clear()
         */
        public void clear() {
            throw new UnsupportedOperationException();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.List#remove(java.lang.Object)
         */
        public boolean remove( Object o ) {
            throw new UnsupportedOperationException();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.List#remove(int)
         */
        public NodeType remove( int index ) {
            throw new UnsupportedOperationException();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.List#removeAll(java.util.Collection)
         */
        public boolean removeAll( Collection<?> c ) {
            throw new UnsupportedOperationException();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.List#retainAll(java.util.Collection)
         */
        public boolean retainAll( Collection<?> c ) {
            throw new UnsupportedOperationException();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.List#set(int, java.lang.Object)
         */
        public NodeType set( int index,
                             NodeType element ) {
            throw new UnsupportedOperationException();
        }
    }
}
