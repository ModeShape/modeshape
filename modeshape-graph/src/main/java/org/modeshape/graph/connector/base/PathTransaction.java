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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.common.util.StringUtil;
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.Location;
import org.modeshape.graph.connector.RepositorySourceException;
import org.modeshape.graph.connector.base.PathWorkspace.ChangeCommand;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathNotFoundException;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.Path.Segment;
import org.modeshape.graph.query.QueryResults;
import org.modeshape.graph.request.AccessQueryRequest;
import org.modeshape.graph.request.FullTextSearchRequest;

/**
 * An implementation of {@link Transaction} that maintains a cache of nodes by their path.
 * 
 * @param <WorkspaceType> the type of workspace
 * @param <NodeType> the type of node
 */
@NotThreadSafe
public abstract class PathTransaction<NodeType extends PathNode, WorkspaceType extends PathWorkspace<NodeType>>
    extends BaseTransaction<NodeType, WorkspaceType> {

    /** The set of changes to the workspaces that have been made by this transaction */
    private Map<String, WorkspaceChanges> changesByWorkspaceName;

    /**
     * Create a new transaction.
     * 
     * @param repository the repository against which the transaction will be operating; may not be null
     * @param rootNodeUuid the UUID of the root node; may not be null
     */
    protected PathTransaction( Repository<NodeType, WorkspaceType> repository,
                               UUID rootNodeUuid ) {
        super(repository.getContext(), repository, rootNodeUuid);
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
        if (getRepository().getRootNodeUuid().equals(uuid)) {
            Path rootPath = pathFactory.createRootPath();

            // The root node can't be removed
            WorkspaceChanges changes = getChangesFor(workspace, false);
            NodeType node = null;
            if (changes != null) {
                assert !changes.isRemoved(rootPath);
                // Not deleted, but maybe changed in this transaction ...
                node = changes.getChangedOrAdded(rootPath);
                if (node != null) return node;
            }
            // It hasn't been loaded already, so attempt to load it from the map owned by the workspace ...
            node = workspace.getNode(rootPath);
            if (node != null) return node;
        }
        // Otherwise, look by path ...
        if (location.hasPath()) {
            // First, find the lowest node in the path that has a change (if any)
            Path path = location.getPath();

            WorkspaceChanges changes = getChangesFor(workspace, false);
            NodeType lowestNode = changes == null ? null : changes.getLowestChangedNodeFor(path);

            if (lowestNode == null) {
                // No nodes on the path were change, go straight to the source
                NodeType node = workspace.getNode(path);

                // If the source has no node at this location, default to a slow walk up the path from the root
                if (node != null) return node;
            }

            return getNode(workspace, location.getPath(), location);
        }
        // Unable to find by UUID or by path, so fail ...
        Path lowestExisting = pathFactory.createRootPath();
        throw new PathNotFoundException(location, lowestExisting, GraphI18n.nodeDoesNotExist.text(readable(location)));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.BaseTransaction#verifyNodeExists(org.modeshape.graph.connector.base.Workspace,
     *      org.modeshape.graph.Location)
     */
    public Location verifyNodeExists( WorkspaceType workspace,
                                      Location location ) {
        assert location != null;
        // First look for the UUID ...
        UUID uuid = location.getUuid();
        if (getRepository().getRootNodeUuid().equals(uuid)) {
            // The root node can't be removed
            return getRootLocation();
        }
        // Otherwise, look by path ...
        if (location.hasPath()) {
            // First, find the lowest node in the path that has a change (if any)
            Path path = location.getPath();

            WorkspaceChanges changes = getChangesFor(workspace, false);
            NodeType lowestNode = changes == null ? null : changes.getLowestChangedNodeFor(path);

            if (lowestNode == null) {
                // No nodes on the path were change, go straight to the source
                return workspace.verifyNodeExists(path);
            }
            // doesn't exist, so find out by path ...
            NodeType node = getNode(workspace, location.getPath(), location);
            return locationFor(node);
        }
        // Unable to find by UUID or by path, so fail ...
        Path lowestExisting = pathFactory.createRootPath();
        throw new PathNotFoundException(location, lowestExisting, GraphI18n.nodeDoesNotExist.text(readable(location)));
    }

    /**
     * Attempt to find the node with the supplied path. This method is "Changes-aware". That is, it checks the cache of changes
     * for the workspace before returning the node.
     * 
     * @param workspace the workspace; may not be null
     * @param path the path of the node; may not be null
     * @return the node, or null if no such node exists
     */
    protected NodeType findNode( WorkspaceType workspace,
                                 Path path ) {
        WorkspaceChanges changes = getChangesFor(workspace, false);
        NodeType node = null;
        if (changes != null) {
            // See if the node we're looking for was deleted ...
            if (changes.isRemoved(path)) {
                // This node was removed within this transaction ...
                return null;
            }
            // Not deleted, but maybe changed in this transaction ...
            node = changes.getChangedOrAdded(path);
            if (node != null) return node;
        }
        // It hasn't been loaded already, so attempt to load it from the map owned by the workspace ...
        node = workspace.getNode(path);
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

        changes.removed(pathTo(node));
    }

    /**
     * Returns the location for the given node, based on its path
     * 
     * @param node the node for which the location should be returned; may not be null
     * @return the location for the given node, based on its path; never null
     */
    private Location locationFor( NodeType node ) {
        return Location.create(pathTo(node));
    }

    /**
     * Returns the path to the given node
     * 
     * @param node the node for which the path should be returned; may not be null
     * @return the path to that node
     */
    protected Path pathTo( NodeType node ) {
        if (node.getParent() == null) {
            return pathFactory.createRootPath();
        }
        return pathFactory.create(node.getParent(), node.getName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.BaseTransaction#pathFor(org.modeshape.graph.connector.base.Workspace,
     *      org.modeshape.graph.connector.base.Node)
     */
    @Override
    public Path pathFor( WorkspaceType workspace,
                         NodeType node ) {
        return pathTo(node);
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
        WorkspaceChanges changes = getChangesFor(workspace, true);

        // If the parent doesn't already have changes, we need to find the new parent in the newWorkspace's changes
        if (!parent.hasChanges()) {
            parent = getNode(workspace, locationFor(parent));
        }
        NodeType oldParent = (NodeType)parent.clone();

        NodeType newNode = null;
        if (index < 0) {
            // Figure out the SNS of the new node ...
            int snsIndex = 1;
            for (NodeType child : getChildren(workspace, parent)) {
                if (child.getName().getName().equals(name)) ++snsIndex;
            }

            // Create the new node ...
            Segment childSegment = pathFactory.createSegment(name, snsIndex);
            newNode = createNode(childSegment, pathTo(parent), properties);
            // And add to the parent ...
            parent = (NodeType)parent.withChild(childSegment);
        } else {
            int snsIndex = 0;
            List<NodeType> children = getChildren(workspace, parent);

            if (index < children.size()) {
                ListIterator<NodeType> existingSiblings = children.listIterator(index);
                while (existingSiblings.hasNext()) {
                    NodeType existingSibling = existingSiblings.next();
                    Segment existingSegment = existingSibling.getName();
                    if (existingSegment.getName().equals(name)) {
                        int existingIndex = existingSegment.getIndex();
                        if (snsIndex == 0) snsIndex = existingIndex;
                        NodeType oldSibling = (NodeType)existingSibling.clone();
                        existingSibling = (NodeType)existingSibling.withName(pathFactory.createSegment(name, existingIndex + 1));
                        changes.changed(oldSibling, existingSibling);
                    }
                }
            }
            // Create the new node ...
            Segment childSegment = pathFactory.createSegment(name, snsIndex + 1);
            newNode = createNode(childSegment, pathTo(parent), properties);
            // And add to the parent ...
            parent = (NodeType)parent.withChild(index, childSegment);
        }
        changes.created(newNode);
        changes.changed(oldParent, parent);
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
                              NodeType originalChild,
                              NodeType beforeOtherChild,
                              Name desiredName ) {
        // If the parent doesn't already have changes, we need to find the new parent in the newWorkspace's changes
        if (!parent.hasChanges()) {
            parent = findNode(workspace, pathTo(parent));
        }

        // Get some information about the child ...
        Segment newChildSegment = originalChild.getName();
        Name newChildName = newChildSegment.getName();
        int snsIndex = newChildSegment.getIndex();

        // Find the existing parent of the new child ...
        NodeType oldParent = getParent(workspace, originalChild);

        // Find the changes for this workspace ...
        WorkspaceChanges changes = getChangesFor(workspace, true);

        if (oldParent != null) {
            // Remove the node from it's parent ...
            int oldIndex = oldParent.getChildren().indexOf(newChildSegment);
            NodeType priorParent = (NodeType)oldParent.clone();
            if (oldParent.equals(parent)) {
                oldParent = (NodeType)oldParent.withoutChild(newChildSegment);
                changes.changed(priorParent, oldParent);
                parent = oldParent;
            } else {
                oldParent = (NodeType)oldParent.withoutChild(newChildSegment);
                changes.changed(priorParent, oldParent);
            }

            // Now find any siblings with the same name that appear after the node in the parent's list of children ...
            List<NodeType> siblings = getChildren(workspace, oldParent);
            if (oldIndex < siblings.size()) {
                for (ListIterator<NodeType> iter = siblings.listIterator(oldIndex); iter.hasNext();) {
                    NodeType sibling = iter.next();
                    if (sibling.getName().getName().equals(newChildName)) {
                        NodeType oldSibling = (NodeType)sibling.clone();
                        sibling = (NodeType)sibling.withName(pathFactory.createSegment(newChildName, snsIndex++));
                        changes.changed(oldSibling, sibling);
                    }
                }
            }
        }

        // Find the index of the other child ...
        int index = parent.getChildren().size();
        if (beforeOtherChild != null) {
            if (!beforeOtherChild.getParent().equals(pathTo(parent))) {
                // The other child doesn't exist in the parent ...
                throw new RepositorySourceException(null);
            }
            Segment otherChild = beforeOtherChild.getName();
            index = parent.getChildren().indexOf(otherChild);
        }

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
                    NodeType oldSibling = (NodeType)existingSibling.clone();
                    existingSibling = (NodeType)existingSibling.withName(pathFactory.createSegment(newChildName, ++snsIndex));
                    changes.changed(oldSibling, existingSibling);
                }
            }
            ++i;
        }
        if (childName == null) {
            // Must be appending the child ...
            childName = pathFactory.createSegment(newChildName, snsIndex);
        }

        /*
         * We need to clone the original child here.  Otherwise, the call to withName(childName) below
         * will modify the name of originalChild as a side effect if the originalChild already has pending changes 
         * during this transaction (q.v., MODE-944).  If that were to happen, the changes.moved() call further
         * below would essentially move the node onto itself, instead of moving the node from the old location
         * to the new location.
         */
        NodeType copyOfOriginalChild = (NodeType)originalChild.clone();

        // Change the name of the new node ...
        NodeType newChild = (NodeType)copyOfOriginalChild.withName(childName).withParent(pathTo(parent));
        parent = (NodeType)parent.withChild(index, newChild.getName());
        changes.moved(originalChild, newChild, parent);

        return locationFor(newChild);
    }

    /**
     * Create a new instance of the node, given the supplied name and parent path. This method should do nothing but instantiate
     * the new node; the caller will add to the appropriate internal structures.
     * 
     * @param name the name of the new node; may be null if this is the root node
     * @param parentPath the path of the parent node; may be null if this is the root node
     * @param properties the properties; may be null if there are no properties
     * @return the new node; never null
     */
    protected abstract NodeType createNode( Segment name,
                                            Path parentPath,
                                            Iterable<Property> properties );

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.Transaction#getChild(org.modeshape.graph.connector.base.Workspace,
     *      org.modeshape.graph.connector.base.Node, org.modeshape.graph.property.Path.Segment)
     */
    @SuppressWarnings( "unchecked" )
    public NodeType getChild( WorkspaceType workspace,
                              NodeType parent,
                              Segment childSegment ) {
        List<Segment> children = parent.getChildren(); // don't make a copy
        for (Segment child : children) {
            if (child.equals(childSegment)) {
                Path childPath = pathFactory.create(pathTo(parent), child);

                WorkspaceChanges changes = getChangesFor(workspace, true);
                NodeType changed = changes.getChangedOrAdded(childPath);
                if (changed != null) {
                    return changed;
                }

                Path persistentPath = changes.persistentPathFor(childPath);
                NodeType childNode = workspace.getNode(persistentPath);

                if (persistentPath.equals(childPath)) return childNode;

                return (NodeType)childNode.withParent(childPath.getParent()).withName(childPath.getLastSegment());
            }
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
        List<Segment> childSegments = node.getChildren(); // make a copy
        if (childSegments.isEmpty()) return Collections.emptyList();

        List<NodeType> children = new ArrayList<NodeType>(childSegments.size());

        for (Segment childSegment : childSegments) {
            children.add(getNode(workspace, Location.create(pathFactory.create(pathTo(node), childSegment))));
        }

        return children;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.Transaction#getChildrenLocations(org.modeshape.graph.connector.base.Workspace,
     *      org.modeshape.graph.connector.base.Node)
     */
    public List<Location> getChildrenLocations( WorkspaceType workspace,
                                                NodeType node ) {
        List<Segment> childSegments = node.getChildren(); // make a copy
        if (childSegments.isEmpty()) return Collections.emptyList();

        List<Location> children = new ArrayList<Location>(childSegments.size());
        Path nodePath = pathTo(node);
        for (Segment childSegment : childSegments) {
            children.add(Location.create(pathFactory.create(nodePath, childSegment)));
        }
        return children;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.Transaction#getParent(org.modeshape.graph.connector.base.Workspace,
     *      org.modeshape.graph.connector.base.Node)
     */
    public NodeType getParent( WorkspaceType workspace,
                               NodeType node ) {
        Path parentPath = node.getParent();
        if (parentPath == null) return null;
        return getNode(workspace, Location.create(parentPath));
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
        for (NodeType child : getChildren(workspace, node)) {
            destroyNode(workspace, child);
        }
        NodeType oldNode = (NodeType)node.clone();
        node = (NodeType)node.withoutChildren();
        getChangesFor(workspace, true).changed(oldNode, node);
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
        NodeType parent = getParent(workspace, node);
        if (parent == null) {
            // The root node is being removed, which means we should just delete everything (except the root) ...
            WorkspaceChanges changes = getChangesFor(workspace, true);
            changes.removeAll(null);
            return Location.create(pathFactory.createRootPath(), rootNodeUuid);
        }
        Location result = locationFor(node);

        // Find the index of the node in it's parent ...
        int index = parent.getChildren().indexOf(node.getName());
        assert index != -1;
        Name name = node.getName().getName();
        int snsIndex = node.getName().getIndex();
        WorkspaceChanges changes = getChangesFor(workspace, true);
        NodeType oldParent = (NodeType)parent.clone();
        // Remove the node from the parent ...
        parent = (NodeType)parent.withoutChild(node.getName());
        changes.changed(oldParent, parent);

        // Now find any siblings with the same name that appear after the node in the parent's list of children ...
        List<NodeType> siblings = getChildren(workspace, parent);
        if (index < siblings.size()) {
            for (ListIterator<NodeType> iter = siblings.listIterator(index); iter.hasNext();) {
                NodeType sibling = iter.next();
                if (sibling.getName().getName().equals(name)) {
                    NodeType oldSibling = (NodeType)sibling.clone();
                    sibling = (NodeType)sibling.withName(pathFactory.createSegment(name, snsIndex++));
                    changes.changed(oldSibling, sibling);
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
        NodeType oldCopy = (NodeType)node.clone();
        NodeType copy = (NodeType)node.withoutProperty(propertyName);
        if (copy != node) {
            WorkspaceChanges changes = getChangesFor(workspace, true);
            changes.changed(oldCopy, copy);
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
        NodeType oldCopy = (NodeType)node.clone();
        NodeType copy = (NodeType)node.withProperties(propertiesToSet, propertiesToRemove, removeAllExisting);
        if (copy != node) {
            WorkspaceChanges changes = getChangesFor(workspace, true);
            changes.changed(oldCopy, copy);
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
        NodeType copy = addChild(newWorkspace, newParent, desiredName, -1, null, original.getProperties().values());

        if (recursive) {
            WorkspaceChanges changes = getChangesFor(newWorkspace, true);
            // Walk through the original branch in its workspace ...
            for (NodeType originalChild : getChildren(originalWorkspace, original)) {
                NodeType newChild = copyBranch(originalWorkspace, originalChild, changes, newWorkspace, copy);
                copy = (NodeType)copy.withChild(newChild.getName());
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
    public NodeType cloneNode( WorkspaceType originalWorkspace,
                               NodeType original,
                               WorkspaceType newWorkspace,
                               NodeType newParent,
                               Name desiredName,
                               Segment desiredSegment,
                               boolean removeExisting,
                               java.util.Set<Location> removedExistingNodes )
        throws org.modeshape.graph.connector.UuidAlreadyExistsException {

        return copyNode(originalWorkspace, original, newWorkspace, newParent, desiredName, true);
    }

    @SuppressWarnings( "unchecked" )
    protected NodeType copyBranch( WorkspaceType originalWorkspace,
                                   NodeType original,
                                   WorkspaceChanges newWorkspaceChanges,
                                   WorkspaceType newWorkspace,
                                   NodeType newParent ) {
        // Create the new node (or reuse the original if we can) ...
        NodeType copy = createNode(original.getName(), pathTo(newParent), original.getProperties().values());
        newWorkspaceChanges.created(copy);

        // Walk through the children and call this method recursively ...
        for (NodeType originalChild : getChildren(originalWorkspace, original)) {
            NodeType newChild = copyBranch(originalWorkspace, originalChild, newWorkspaceChanges, newWorkspace, copy);
            copy = (NodeType)copy.withChild(newChild.getName());
        }
        // newWorkspaceChanges.changed(null, copy);
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

    protected void validateNode( WorkspaceType workspace,
                                 NodeType node ) {
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
        private final TreeMap<Path, NodeType> changedOrAddedNodes = new TreeMap<Path, NodeType>();
        private final TreeMap<Path, Path> movedNodes = new TreeMap<Path, Path>();
        private final Set<Path> removedNodes = new HashSet<Path>();
        private final List<ChangeCommand<NodeType>> commands = new LinkedList<ChangeCommand<NodeType>>();

        protected WorkspaceChanges( WorkspaceType workspace ) {
            this.workspace = workspace;
        }

        public WorkspaceType getWorkspace() {
            return workspace;
        }

        public void removeAll( NodeType newRootNode ) {
            changedOrAddedNodes.clear();
            removedNodes.clear();
            commands.add(workspace.createRemoveCommand(pathFactory.createRootPath()));
        }

        public boolean isRemoved( Path path ) {
            return removedNodes.contains(path);
        }

        public NodeType getLowestChangedNodeFor( Path path ) {
            for (Path newPath : changedOrAddedNodes.descendingKeySet()) {
                if (path.isAtOrBelow(newPath)) {
                    return changedOrAddedNodes.get(newPath);
                }
            }
            return null;

        }

        public Path persistentPathFor( Path path ) {
            for (Path newPath : movedNodes.descendingKeySet()) {
                if (path.isAtOrBelow(newPath)) {
                    return path.relativeTo(newPath).resolveAgainst(movedNodes.get(newPath));
                }
            }
            return path;
        }

        public NodeType getChangedOrAdded( Path path ) {
            return changedOrAddedNodes.get(path);
        }

        public void removed( Path path ) {
            removedNodes.add(path);
            changedOrAddedNodes.remove(path);
            commands.add(workspace.createRemoveCommand(path));
        }

        public void created( NodeType node ) {
            validateNode(workspace, node);

            Path path = pathTo(node);
            removedNodes.remove(path);
            changedOrAddedNodes.put(path, node);
            commands.add(workspace.createPutCommand(null, node));
        }

        public void changed( NodeType from,
                             NodeType to ) {
            validateNode(workspace, to);

            Path path = pathTo(to);
            assert !removedNodes.contains(path); // should not be removed
            changedOrAddedNodes.put(path, to);
            commands.add(workspace.createPutCommand(from, to));
        }

        public void moved( NodeType node,
                           NodeType newNode,
                           NodeType newParent ) {
            validateNode(workspace, newNode);

            Path path = pathTo(node);
            Path newPath = pathTo(newNode);
            changedOrAddedNodes.put(newPath, newNode);
            changedOrAddedNodes.put(pathTo(newParent), newParent);
            movedNodes.put(pathTo(newNode), path);
            commands.add(workspace.createMoveCommand(node, newNode));
        }

        public void commit() {
            workspace.commit(commands);
        }

        @Override
        public String toString() {
            return changedOrAddedNodes.keySet().toString() + "\n\t" + movedNodes.toString();
        }
    }
}
