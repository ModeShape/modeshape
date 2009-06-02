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
package org.jboss.dna.graph.connector.inmemory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.property.PropertyFactory;
import org.jboss.dna.graph.property.PropertyType;
import org.jboss.dna.graph.property.Reference;
import org.jboss.dna.graph.property.UuidFactory;
import org.jboss.dna.graph.property.ValueFactory;
import org.jboss.dna.graph.property.basic.RootPath;
import org.jboss.dna.graph.request.CreateWorkspaceRequest.CreateConflictBehavior;

/**
 * @author Randall Hauch
 */
@NotThreadSafe
public class InMemoryRepository {

    protected final ReadWriteLock lock = new ReentrantReadWriteLock();
    protected final UUID rootNodeUuid;
    private final String sourceName;
    private final String defaultWorkspaceName;
    private final Map<String, Workspace> workspaces = new HashMap<String, Workspace>();

    public InMemoryRepository( String sourceName,
                               UUID rootNodeUuid ) {
        this(sourceName, rootNodeUuid, null);
    }

    public InMemoryRepository( String sourceName,
                               UUID rootNodeUuid,
                               String defaultWorkspaceName ) {
        CheckArg.isNotEmpty(sourceName, "sourceName");
        CheckArg.isNotNull(rootNodeUuid, "rootNodeUUID");
        this.rootNodeUuid = rootNodeUuid;
        this.sourceName = sourceName;
        this.defaultWorkspaceName = defaultWorkspaceName != null ? defaultWorkspaceName : "";
        // Create the default workspace ...
        workspaces.put(this.defaultWorkspaceName, new Workspace(this.defaultWorkspaceName));
    }

    /**
     * @return sourceName
     */
    public String getSourceName() {
        return sourceName;
    }

    /**
     * @return lock
     */
    public ReadWriteLock getLock() {
        return lock;
    }

    @GuardedBy( "getLock()" )
    public Set<String> getWorkspaceNames() {
        return workspaces.keySet();
    }

    @GuardedBy( "getLock()" )
    public Workspace getWorkspace( ExecutionContext context,
                                   String name ) {
        if (name == null) name = defaultWorkspaceName;
        return workspaces.get(name);
    }

    @GuardedBy( "getLock()" )
    public Workspace createWorkspace( ExecutionContext context,
                                      String name,
                                      CreateConflictBehavior behavior ) {
        String newName = name;
        boolean conflictingName = workspaces.containsKey(newName);
        if (conflictingName) {
            switch (behavior) {
                case DO_NOT_CREATE:
                    return null;
                case CREATE_WITH_ADJUSTED_NAME:
                    int counter = 0;
                    do {
                        newName = name + (++counter);
                    } while (workspaces.containsKey(newName));
                    break;
            }
        }
        assert workspaces.containsKey(newName) == false;
        Workspace workspace = new Workspace(newName);
        workspaces.put(newName, workspace);
        return workspace;
    }

    @GuardedBy( "getLock()" )
    public Workspace createWorkspace( ExecutionContext context,
                                      String name,
                                      CreateConflictBehavior existingWorkspaceBehavior,
                                      String nameOfWorkspaceToClone ) {
        Workspace workspace = createWorkspace(context, name, existingWorkspaceBehavior);
        if (workspace == null) {
            // Unable to create because of a duplicate name ...
            return null;
        }
        Workspace original = getWorkspace(context, nameOfWorkspaceToClone);
        if (original != null) {
            // Copy the properties of the root node ...
            InMemoryNode root = workspace.getRoot();
            InMemoryNode origRoot = original.getRoot();
            root.getProperties().clear();
            root.getProperties().putAll(origRoot.getProperties());

            // Loop over each child and call this method to copy the immediate children (and below).
            // Note that this makes the copy have the same UUID as the original.
            for (InMemoryNode originalNode : origRoot.getChildren()) {
                original.copyNode(context, originalNode, workspace, root, originalNode.getName().getName(), true, null);
            }
        }
        return workspace;
    }

    @GuardedBy( "getLock()" )
    public boolean destroyWorkspace( String name ) {
        return workspaces.remove(name) != null;
    }

    protected class Workspace {
        private final Map<UUID, InMemoryNode> nodesByUuid = new HashMap<UUID, InMemoryNode>();
        private final String name;

        protected Workspace( String name ) {
            assert name != null;
            this.name = name;
            // Create the root node ...
            InMemoryNode root = new InMemoryNode(rootNodeUuid);
            nodesByUuid.put(root.getUuid(), root);
        }

        /**
         * @return name
         */
        public String getName() {
            return name;
        }

        public InMemoryNode getRoot() {
            return nodesByUuid.get(rootNodeUuid);
        }

        public InMemoryNode getNode( UUID uuid ) {
            assert uuid != null;
            return nodesByUuid.get(uuid);
        }

        protected Map<UUID, InMemoryNode> getNodesByUuid() {
            return nodesByUuid;
        }

        public InMemoryNode getNode( ExecutionContext context,
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
        public InMemoryNode getNode( Path path ) {
            assert path != null;
            InMemoryNode node = getRoot();
            for (Path.Segment segment : path) {
                InMemoryNode desiredChild = null;
                for (InMemoryNode child : node.getChildren()) {
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
         * Find the lowest existing node along the path.
         * 
         * @param path the path to the node; may not be null
         * @return the lowest existing node along the path, or the root node if no node exists on the path
         */
        public Path getLowestExistingPath( Path path ) {
            assert path != null;
            InMemoryNode node = getRoot();
            int segmentNumber = 0;
            for (Path.Segment segment : path) {
                InMemoryNode desiredChild = null;
                for (InMemoryNode child : node.getChildren()) {
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

        public void removeNode( ExecutionContext context,
                                InMemoryNode node ) {
            assert context != null;
            assert node != null;
            if (getRoot().equals(node)) {
                nodesByUuid.clear();
                // Create the root node ...
                InMemoryNode root = new InMemoryNode(rootNodeUuid);
                nodesByUuid.put(root.getUuid(), root);
                return;
            }
            InMemoryNode parent = node.getParent();
            assert parent != null;
            parent.getChildren().remove(node);
            correctSameNameSiblingIndexes(context, parent, node.getName().getName());
            removeUuidReference(node);
        }

        protected void removeUuidReference( InMemoryNode node ) {
            nodesByUuid.remove(node.getUuid());
            for (InMemoryNode child : node.getChildren()) {
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
        public InMemoryNode createNode( ExecutionContext context,
                                        String pathToNewNode ) {
            assert context != null;
            assert pathToNewNode != null;
            Path path = context.getValueFactories().getPathFactory().create(pathToNewNode);
            if (path.isRoot()) return getRoot();
            Path parentPath = path.getParent();
            InMemoryNode parentNode = getNode(parentPath);
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
        public InMemoryNode createNode( ExecutionContext context,
                                        InMemoryNode parentNode,
                                        Name name,
                                        UUID uuid ) {
            assert context != null;
            assert name != null;
            if (parentNode == null) parentNode = getRoot();
            if (uuid == null) uuid = UUID.randomUUID();
            InMemoryNode node = new InMemoryNode(uuid);
            nodesByUuid.put(node.getUuid(), node);
            node.setParent(parentNode);
            // Find the last node with this same name ...
            int nextIndex = 1;
            if (parentNode.existingNames.contains(name)) {
                ListIterator<InMemoryNode> iter = parentNode.getChildren().listIterator(parentNode.getChildren().size());
                while (iter.hasPrevious()) {
                    InMemoryNode prev = iter.previous();
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
         * @param node the node to be moved; may not be the {@link Workspace#getRoot() root}
         * @param desiredNewName the new name for the node, if it is to be changed; may be null
         * @param newWorkspace the workspace containing the new parent node
         * @param newParent the new parent; may not be the {@link Workspace#getRoot() root}
         * @param beforeNode the node before which this new node should be placed
         */
        public void moveNode( ExecutionContext context,
                              InMemoryNode node,
                              Name desiredNewName,
                              Workspace newWorkspace,
                              InMemoryNode newParent,
                              InMemoryNode beforeNode) {
            assert context != null;
            assert newParent != null;
            assert node != null;
// Why was this restriction here? -- BRC            
//            assert newWorkspace.getRoot().equals(newParent) != true;
            assert this.getRoot().equals(node) != true;
            InMemoryNode oldParent = node.getParent();
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
            }
            else {
                int index = newParent.getChildren().indexOf(beforeNode);
                newParent.getChildren().add(index, node);
            }
            correctSameNameSiblingIndexes(context, newParent, newName);
            
            // If the node was moved to a new workspace...
            if (!this.equals(newWorkspace)) {
                // We need to remove the node from this workspace's map of nodes ...
                this.moveNodeToWorkspace(node, newWorkspace);
            }
        }

        protected void moveNodeToWorkspace( InMemoryNode node,
                                            Workspace newWorkspace ) {
            assert this.nodesByUuid.containsKey(node.getUuid());
            assert !newWorkspace.nodesByUuid.containsKey(node.getUuid());

            this.nodesByUuid.remove(node.getUuid());
            newWorkspace.nodesByUuid.put(node.getUuid(), node);
            for (InMemoryNode child : node.getChildren()) {
                moveNodeToWorkspace(child, newWorkspace);
            }
        }

        /**
         * This should copy the subgraph given by the original node and place the new copy under the supplied new parent. Note
         * that internal references between nodes within the original subgraph must be reflected as internal nodes within the new
         * subgraph.
         * 
         * @param context the context; may not be null
         * @param original the node to be copied; may not be null
         * @param newWorkspace the workspace containing the new parent node; may not be null
         * @param newParent the parent where the copy is to be placed; may not be null
         * @param desiredName the desired name for the node; if null, the name will be obtained from the original node
         * @param recursive true if the copy should be recursive
         * @param oldToNewUuids the map of UUIDs of nodes in the new subgraph keyed by the UUIDs of nodes in the original; may be
         *        null if the UUIDs are to be maintained
         * @return the new node, which is the top of the new subgraph
         */
        public InMemoryNode copyNode( ExecutionContext context,
                                      InMemoryNode original,
                                      Workspace newWorkspace,
                                      InMemoryNode newParent,
                                      Name desiredName,
                                      boolean recursive,
                                      Map<UUID, UUID> oldToNewUuids ) {
            assert context != null;
            assert original != null;
            assert newParent != null;
            assert newWorkspace != null;
            if (this.equals(newWorkspace) && oldToNewUuids == null) oldToNewUuids = new HashMap<UUID, UUID>();
            boolean reuseUuids = oldToNewUuids == null;

            // Get or create the new node ...
            Name childName = desiredName != null ? desiredName : original.getName().getName();
            UUID uuidForCopy = reuseUuids ? original.getUuid() : UUID.randomUUID();
            InMemoryNode copy = newWorkspace.createNode(context, newParent, childName, uuidForCopy);
            if (oldToNewUuids != null) {
                oldToNewUuids.put(original.getUuid(), copy.getUuid());
            }

            // Copy the properties ...
            copy.getProperties().clear();
            copy.getProperties().putAll(original.getProperties());
            if (recursive) {
                // Loop over each child and call this method ...
                for (InMemoryNode child : original.getChildren()) {
                    copyNode(context, child, newWorkspace, copy, null, true, oldToNewUuids);
                }
            }

            if (oldToNewUuids != null) {
                // Now, adjust any references in the new subgraph to objects in the original subgraph
                // (because they were internal references, and need to be internal to the new subgraph)
                PropertyFactory propertyFactory = context.getPropertyFactory();
                UuidFactory uuidFactory = context.getValueFactories().getUuidFactory();
                ValueFactory<Reference> referenceFactory = context.getValueFactories().getReferenceFactory();
                for (Map.Entry<UUID, UUID> oldToNew : oldToNewUuids.entrySet()) {
                    InMemoryNode oldNode = this.getNode(oldToNew.getKey());
                    InMemoryNode newNode = newWorkspace.getNode(oldToNew.getValue());
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

        protected void correctSameNameSiblingIndexes( ExecutionContext context,
                                                      InMemoryNode parentNode,
                                                      Name name ) {
            if (parentNode == null) return;
            // Look for the highest existing index ...
            List<InMemoryNode> childrenWithSameNames = new LinkedList<InMemoryNode>();
            for (InMemoryNode child : parentNode.getChildren()) {
                if (child.getName().getName().equals(name)) childrenWithSameNames.add(child);
            }
            if (childrenWithSameNames.size() == 0) return;
            if (childrenWithSameNames.size() == 1) {
                InMemoryNode childWithSameName = childrenWithSameNames.get(0);
                Path.Segment newName = context.getValueFactories().getPathFactory().createSegment(name, Path.DEFAULT_INDEX);
                childWithSameName.setName(newName);
                return;
            }
            int index = 1;
            for (InMemoryNode childWithSameName : childrenWithSameNames) {
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
            if (obj instanceof Workspace) {
                Workspace that = (Workspace)obj;
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
            return InMemoryRepository.this.getSourceName() + "/" + this.getName();
        }

    }

}
