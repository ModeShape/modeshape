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
package org.jboss.dna.graph.connector.map;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.connector.UuidAlreadyExistsException;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.NamespaceRegistry;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.property.PropertyFactory;
import org.jboss.dna.graph.property.PropertyType;
import org.jboss.dna.graph.property.Reference;
import org.jboss.dna.graph.property.UuidFactory;
import org.jboss.dna.graph.property.ValueFactory;
import org.jboss.dna.graph.property.Path.Segment;
import org.jboss.dna.graph.property.basic.RootPath;

/**
 * A default implementation of {@link MapWorkspace} that only requires the user to implement some simple, map-like operations.
 */
public abstract class AbstractMapWorkspace implements MapWorkspace {

    private final MapRepository repository;
    private final String name;

    protected AbstractMapWorkspace( MapRepository repository,
                                    String name ) {
        assert repository != null;
        assert name != null;

        this.repository = repository;
        this.name = name;
    }

    protected void initialize() {

        // Create the root node ...
        MapNode root = createMapNode(repository.getRootNodeUuid());
        assert root != null;
        addNodeToMap(root);

    }

    /**
     * Adds the given node to the backing map, replacing any node already in the backing map with the same UUID.
     * 
     * @param node the node to add to the map; may not be null
     */
    protected abstract void addNodeToMap( MapNode node );

    /**
     * Removes the node with the given UUID from the backing map, returning the node if it exists
     * 
     * @param nodeUuid the UUID of the node to replace; may not be null
     * @return if a node with that UUID exists in the backing map (prior to removal), that node is returned; otherwise null
     */
    protected abstract MapNode removeNodeFromMap( UUID nodeUuid );

    /**
     * Removes all of the nodes from the backing map
     */
    protected abstract void removeAllNodesFromMap();

    /**
     * Gets the node with the given UUID from the backing map, if one exists
     * 
     * @param nodeUuid the UUID of the node to be retrieved; may not be null
     * @return the node that exists in the backing map with the given UUID
     */
    public abstract MapNode getNode( UUID nodeUuid );

    /**
     * Creates an empty {@link MapNode node} with the given UUID.
     * <p>
     * This method does not add the new map node to the map. That must be done with a separate call to
     * {@link #addNodeToMap(MapNode)}.
     * </p>
     * 
     * @param uuid the UUID that identifies the new node; may not be null
     * @return the new node with the given UUID
     */
    protected MapNode createMapNode( UUID uuid ) {
        assert uuid != null;
        return new MapNode(uuid);
    }

    /**
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     * 
     * @see MapWorkspace#getRoot()
     */
    public MapNode getRoot() {
        return getNode(repository.getRootNodeUuid());
    }

    /**
     * Find a node with the given path.
     * 
     * @param context the execution context to use to convert {@code path} to a {@link Path}; may not be null
     * @param path the path to the node; may not be null
     * @return the node with the path, or null if the node does not exist
     */
    public MapNode getNode( ExecutionContext context,
                            String path ) {
        assert context != null;
        assert path != null;
        return getNode(context.getValueFactories().getPathFactory().create(path));
    }

    /**
     * Find a node with the given path.
     * 
     * @param path the path to the node; may not be null
     * @return the node with the path, or null if the node does not exist
     */
    public MapNode getNode( Path path ) {
        assert path != null;
        MapNode node = getRoot();
        for (Path.Segment segment : path) {
            MapNode desiredChild = null;
            for (MapNode child : node.getChildren()) {
                if (child == null) continue;
                Path.Segment childName = child.getName();
                if (childName == null) continue;
                if (childName.equals(segment)) {
                    desiredChild = child;
                    break;
                }
            }
            if (desiredChild != null) {
                node = desiredChild;
            } else {
                return null;
            }
        }
        return node;
    }

    /**
     * Returns the absolute path to the given node
     * 
     * @param pathFactory the path factory to use to create the path from the list of names of all of the nodes on the path from
     *        the root node to the given node
     * @param node the node for which the path should be returned
     * @return the absolute path to the given node
     */
    public Path pathFor( PathFactory pathFactory,
                         MapNode node ) {
        assert node != null;
        assert pathFactory != null;

        LinkedList<Path.Segment> segments = new LinkedList<Path.Segment>();
        MapNode root = getRoot();

        do {
            segments.addFirst(node.getName());
            node = node.getParent();
        } while (!node.equals(root));

        return pathFactory.createAbsolutePath(segments);
    }

    /**
     * Find the lowest existing node along the path.
     * 
     * @param path the path to the node; may not be null
     * @return the lowest existing node along the path, or the root node if no node exists on the path
     */
    public Path getLowestExistingPath( Path path ) {
        assert path != null;
        MapNode node = getRoot();
        int segmentNumber = 0;
        for (Path.Segment segment : path) {
            MapNode desiredChild = null;
            for (MapNode child : node.getChildren()) {
                if (child == null) continue;
                Path.Segment childName = child.getName();
                if (childName == null) continue;
                if (childName.equals(segment)) {
                    desiredChild = child;
                    break;
                }
            }
            if (desiredChild != null) {
                node = desiredChild;
            } else {
                return path.subpath(0, segmentNumber);
            }
            ++segmentNumber;
        }
        return RootPath.INSTANCE;
    }

    /**
     * Removes the given node and its children, correcting the SNS and child indices for its parent.
     * 
     * @param context the current execution context
     * @param node the node to be removed
     */
    public void removeNode( ExecutionContext context,
                            MapNode node ) {
        assert context != null;
        assert node != null;
        if (getRoot().equals(node)) {
            removeAllNodesFromMap();
            // Recreate the root node ...
            addNodeToMap(createMapNode(repository.getRootNodeUuid()));
            return;
        }
        MapNode parent = node.getParent();
        assert parent != null;
        parent.getChildren().remove(node);
        correctSameNameSiblingIndexes(context, parent, node.getName().getName());
        removeUuidReference(node);
    }

    /**
     * Recursively removes the given node and its children from the backing map
     * 
     * @param node the root of the branch to be removed
     */
    protected void removeUuidReference( MapNode node ) {
        assert node != null;
        removeNodeFromMap(node.getUuid());

        for (MapNode child : node.getChildren()) {
            removeUuidReference(child);
        }
    }

    /**
     * Create a node at the supplied path. The parent of the new node must already exist.
     * 
     * @param context the environment; may not be null
     * @param pathToNewNode the path to the new node; may not be null
     * @return the new node (or root if the path specified the root)
     */
    public MapNode createNode( ExecutionContext context,
                               String pathToNewNode ) {
        assert context != null;
        assert pathToNewNode != null;
        Path path = context.getValueFactories().getPathFactory().create(pathToNewNode);
        if (path.isRoot()) return getRoot();
        Path parentPath = path.getParent();
        MapNode parentNode = getNode(parentPath);
        Name name = path.getLastSegment().getName();
        return createNode(context, parentNode, name, null);
    }

    /**
     * Create a new node with the supplied name, as a child of the supplied parent.
     * 
     * @param context the execution context
     * @param parentNode the parent node; may not be null
     * @param name the name; may not be null
     * @param uuid the UUID of the node, or null if the UUID is to be generated
     * @return the new node
     */
    public MapNode createNode( ExecutionContext context,
                               MapNode parentNode,
                               Name name,
                               UUID uuid ) {
        assert context != null;
        assert name != null;
        if (parentNode == null) parentNode = getRoot();
        if (uuid == null) uuid = UUID.randomUUID();

        MapNode node = createMapNode(uuid);
        addNodeToMap(node);

        node.setParent(parentNode);
        // Find the last node with this same name ...
        int nextIndex = 1;
        if (parentNode.existingNames.contains(name)) {
            ListIterator<MapNode> iter = parentNode.getChildren().listIterator(parentNode.getChildren().size());
            while (iter.hasPrevious()) {
                MapNode prev = iter.previous();
                if (prev.getName().getName().equals(name)) {
                    nextIndex = prev.getName().getIndex() + 1;
                    break;
                }
            }
        }
        Path.Segment newName = context.getValueFactories().getPathFactory().createSegment(name, nextIndex);
        node.setName(newName);
        parentNode.getChildren().add(node);
        parentNode.existingNames.add(name);
        return node;
    }

    /**
     * Move the supplied node to the new parent. This method automatically removes the node from its existing parent, and also
     * correctly adjusts the {@link Path.Segment#getIndex() index} to be correct in the new parent.
     * 
     * @param context
     * @param node the node to be moved; may not be the {@link AbstractMapWorkspace#getRoot() root}
     * @param desiredNewName the new name for the node, if it is to be changed; may be null
     * @param newWorkspace the workspace containing the new parent node
     * @param newParent the new parent; may not be the {@link AbstractMapWorkspace#getRoot() root}
     * @param beforeNode the node before which this new node should be placed
     */
    public void moveNode( ExecutionContext context,
                          MapNode node,
                          Name desiredNewName,
                          MapWorkspace newWorkspace,
                          MapNode newParent,
                          MapNode beforeNode ) {
        assert context != null;
        assert newParent != null;
        assert node != null;
        assert newWorkspace instanceof AbstractMapWorkspace;

        AbstractMapWorkspace newAbstractMapWorkspace = (AbstractMapWorkspace)newWorkspace;

        // Why was this restriction here? -- BRC
        // assert newAbstractMapWorkspace.getRoot().equals(newParent) != true;
        assert this.getRoot().equals(node) != true;
        MapNode oldParent = node.getParent();
        Name oldName = node.getName().getName();
        if (oldParent != null) {
            boolean removed = oldParent.getChildren().remove(node);
            assert removed == true;
            node.setParent(null);
            correctSameNameSiblingIndexes(context, oldParent, oldName);
        }
        node.setParent(newParent);
        Name newName = oldName;
        if (desiredNewName != null) {
            newName = desiredNewName;
            node.setName(context.getValueFactories().getPathFactory().createSegment(desiredNewName, 1));
        }

        if (beforeNode == null) {
            newParent.getChildren().add(node);
        } else {
            int index = newParent.getChildren().indexOf(beforeNode);
            newParent.getChildren().add(index, node);
        }
        correctSameNameSiblingIndexes(context, newParent, newName);

        // If the node was moved to a new workspace...
        if (!this.equals(newAbstractMapWorkspace)) {
            // We need to remove the node from this workspace's map of nodes ...
            this.moveNodeToWorkspace(node, newAbstractMapWorkspace);
        }
    }

    /**
     * Moves the branch rooted at the given node to the new workspace, removing it from this workspace. If a node with any of the
     * UUIDs in the branch already exists in the new workspace, it will be replaced by the node from the branch.
     * 
     * @param node the root node of the branch to be moved
     * @param newWorkspace the workspace to which the branch should be moved
     */
    protected void moveNodeToWorkspace( MapNode node,
                                        AbstractMapWorkspace newWorkspace ) {
        assert getNode(node.getUuid()) != null;
        assert newWorkspace.getNode(node.getUuid()) == null;

        removeNodeFromMap(node.getUuid());
        newWorkspace.addNodeToMap(node);

        for (MapNode child : node.getChildren()) {
            moveNodeToWorkspace(child, newWorkspace);
        }
    }

    /**
     * This should copy the subgraph given by the original node and place the new copy under the supplied new parent. Note that
     * internal references between nodes within the original subgraph must be reflected as internal nodes within the new subgraph.
     * 
     * @param context the context; may not be null
     * @param original the node to be copied; may not be null
     * @param newWorkspace the workspace containing the new parent node; may not be null
     * @param newParent the parent where the copy is to be placed; may not be null
     * @param desiredName the desired name for the node; if null, the name will be obtained from the original node
     * @param recursive true if the copy should be recursive
     * @return the new node, which is the top of the new subgraph
     */
    public MapNode copyNode( ExecutionContext context,
                             MapNode original,
                             MapWorkspace newWorkspace,
                             MapNode newParent,
                             Name desiredName,
                             boolean recursive ) {
        return copyNode(context, original, newWorkspace, newParent, desiredName, true, new HashMap<UUID, UUID>());
    }

    /**
     * This should copy the subgraph given by the original node and place the new copy under the supplied new parent. Note that
     * internal references between nodes within the original subgraph must be reflected as internal nodes within the new subgraph.
     * 
     * @param context the context; may not be null
     * @param original the node to be copied; may not be null
     * @param newWorkspace the workspace containing the new parent node; may not be null
     * @param newParent the parent where the copy is to be placed; may not be null
     * @param desiredName the desired name for the node; if null, the name will be obtained from the original node
     * @param recursive true if the copy should be recursive
     * @param oldToNewUuids the map of UUIDs of nodes in the new subgraph keyed by the UUIDs of nodes in the original; may be null
     *        if the UUIDs are to be maintained
     * @return the new node, which is the top of the new subgraph
     */
    protected MapNode copyNode( ExecutionContext context,
                                MapNode original,
                                MapWorkspace newWorkspace,
                                MapNode newParent,
                                Name desiredName,
                                boolean recursive,
                                Map<UUID, UUID> oldToNewUuids ) {
        assert context != null;
        assert original != null;
        assert newParent != null;
        assert newWorkspace != null;
        boolean reuseUuids = oldToNewUuids == null;

        // Get or create the new node ...
        Name childName = desiredName != null ? desiredName : original.getName().getName();
        UUID uuidForCopy = reuseUuids ? original.getUuid() : UUID.randomUUID();
        MapNode copy = newWorkspace.createNode(context, newParent, childName, uuidForCopy);
        if (!reuseUuids) {
            oldToNewUuids.put(original.getUuid(), copy.getUuid());
        }

        // Copy the properties ...
        copy.getProperties().clear();
        copy.getProperties().putAll(original.getProperties());
        if (recursive) {
            // Loop over each child and call this method ...
            for (MapNode child : original.getChildren()) {
                copyNode(context, child, newWorkspace, copy, null, true, oldToNewUuids);
            }
        }

        if (!reuseUuids) {
            // Now, adjust any references in the new subgraph to objects in the original subgraph
            // (because they were internal references, and need to be internal to the new subgraph)
            PropertyFactory propertyFactory = context.getPropertyFactory();
            UuidFactory uuidFactory = context.getValueFactories().getUuidFactory();
            ValueFactory<Reference> referenceFactory = context.getValueFactories().getReferenceFactory();
            for (Map.Entry<UUID, UUID> oldToNew : oldToNewUuids.entrySet()) {
                MapNode oldNode = this.getNode(oldToNew.getKey());
                MapNode newNode = newWorkspace.getNode(oldToNew.getValue());
                assert oldNode != null;
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
                        entry.setValue(newProperty);
                    }
                }
            }
        }

        return copy;
    }

    /**
     * {@inheritDoc}
     * 
     * @see MapWorkspace#cloneNode(ExecutionContext, MapNode, MapWorkspace, MapNode, Name, Segment, boolean)
     */
    public MapNode cloneNode( ExecutionContext context,
                              MapNode original,
                              MapWorkspace newWorkspace,
                              MapNode newParent,
                              Name desiredName,
                              Segment desiredSegment,
                              boolean removeExisting ) throws UuidAlreadyExistsException {
        assert context != null;
        assert original != null;
        assert newWorkspace != null;
        assert newParent != null;

        Set<UUID> uuidsInFromBranch = getUuidsUnderNode(original);
        MapNode existing;

        // TODO: Need to handle removing/throwing root node
        if (removeExisting) {

            for (UUID uuid : uuidsInFromBranch) {
                if (null != (existing = newWorkspace.getNode(uuid))) {
                    newWorkspace.removeNode(context, existing);
                }
            }
        } else {
            PathFactory pathFactory = context.getValueFactories().getPathFactory();
            uuidsInFromBranch.add(original.getUuid());
            for (UUID uuid : uuidsInFromBranch) {
                if (null != (existing = newWorkspace.getNode(uuid))) {
                    NamespaceRegistry namespaces = context.getNamespaceRegistry();
                    String path = newWorkspace.pathFor(pathFactory, existing).getString(namespaces);
                    throw new UuidAlreadyExistsException(repository.getSourceName(), uuid, path, newWorkspace.getName());
                }
            }
        }

        if (desiredSegment != null) {
            MapNode newRoot = null;
            for (MapNode child : newParent.getChildren()) {
                if (desiredSegment.equals(child.getName())) {
                    newRoot = child;
                    break;
                }
            }

            assert newRoot != null;

            newRoot.getProperties().clear();
            for (MapNode child : newRoot.getChildren()) {
                newWorkspace.removeNode(context, child);
            }

            for (Property property : original.getProperties().values()) {
                newRoot.setProperty(property);
            }

            for (MapNode child : original.getChildren()) {
                copyNode(context, child, newWorkspace, newRoot, null, true, (Map<UUID, UUID>)null);
            }
            return newRoot;
        }

        existing = newWorkspace.getNode(original.getUuid());

        if (existing != null) {
            newWorkspace.removeNode(context, existing);
        }
        return copyNode(context, original, newWorkspace, newParent, desiredName, true, (Map<UUID, UUID>)null);
    }

    /**
     * Returns all of the UUIDs in the branch rooted at {@code node}. The UUID of {@code node} will not be included in the set of
     * returned UUIDs.
     * 
     * @param node the root of the branch
     * @return all of the UUIDs in the branch rooted at {@code node}
     */
    public Set<UUID> getUuidsUnderNode( MapNode node ) {
        Set<UUID> uuids = new HashSet<UUID>();
        uuidsUnderNode(node, uuids);

        return uuids;
    }

    private void uuidsUnderNode( MapNode node,
                                 Set<UUID> accumulator ) {
        for (MapNode child : node.getChildren()) {
            accumulator.add(child.getUuid());
            uuidsUnderNode(child, accumulator);
        }
    }

    /**
     * Corrects the SNS indices for all children of the node with the given name
     * 
     * @param context the execution context
     * @param parentNode the parent node
     * @param name the name of the child nodes for which the SNS indices should be recalculated
     */
    protected void correctSameNameSiblingIndexes( ExecutionContext context,
                                                  MapNode parentNode,
                                                  Name name ) {
        if (parentNode == null) return;
        // Look for the highest existing index ...
        List<MapNode> childrenWithSameNames = new LinkedList<MapNode>();
        for (MapNode child : parentNode.getChildren()) {
            if (child.getName().getName().equals(name)) childrenWithSameNames.add(child);
        }
        if (childrenWithSameNames.size() == 0) return;
        if (childrenWithSameNames.size() == 1) {
            MapNode childWithSameName = childrenWithSameNames.get(0);
            Path.Segment newName = context.getValueFactories().getPathFactory().createSegment(name, Path.DEFAULT_INDEX);
            childWithSameName.setName(newName);
            return;
        }
        int index = 1;
        for (MapNode childWithSameName : childrenWithSameNames) {
            Path.Segment segment = childWithSameName.getName();
            if (segment.getIndex() != index) {
                Path.Segment newName = context.getValueFactories().getPathFactory().createSegment(name, index);
                childWithSameName.setName(newName);
            }
            ++index;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof AbstractMapWorkspace) {
            AbstractMapWorkspace that = (AbstractMapWorkspace)obj;
            // Assume the workspaces are in the same repository ...
            if (!this.name.equals(that.name)) return false;
            return true;
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
        return repository.getSourceName() + "/" + this.getName();
    }

}
