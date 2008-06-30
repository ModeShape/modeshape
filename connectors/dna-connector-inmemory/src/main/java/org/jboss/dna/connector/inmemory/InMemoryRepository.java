/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.connector.inmemory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.connection.ExecutionEnvironment;

/**
 * @author Randall Hauch
 */
@NotThreadSafe
public class InMemoryRepository {

    protected final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final String name;
    private final UUID rootNodeUuid;
    private final Map<UUID, Node> nodesByUuid = new HashMap<UUID, Node>();

    public InMemoryRepository( String name,
                               UUID rootNodeUUID ) {
        ArgCheck.isNotNull(rootNodeUUID, "rootNodeUUID");
        ArgCheck.isNotEmpty(name, "name");
        this.name = name;
        this.rootNodeUuid = rootNodeUUID;
        // Create the root node ...
        Node root = new Node(rootNodeUUID);
        nodesByUuid.put(root.getUuid(), root);
    }

    /**
     * @return lock
     */
    public ReadWriteLock getLock() {
        return lock;
    }

    /**
     * @return name
     */
    public String getName() {
        return name;
    }

    public Node getRoot() {
        return nodesByUuid.get(this.rootNodeUuid);
    }

    public Node getNode( UUID uuid ) {
        assert uuid != null;
        return nodesByUuid.get(uuid);
    }

    protected Map<UUID, Node> getNodesByUuid() {
        return nodesByUuid;
    }

    public Node getNode( ExecutionEnvironment env,
                         String path ) {
        assert env != null;
        assert path != null;
        return getNode(env.getValueFactories().getPathFactory().create(path));
    }

    /**
     * Find a node with the given path.
     * 
     * @param path the path to the node; may not be null
     * @return the node with the path, or null if the node does not exist
     */
    public Node getNode( Path path ) {
        assert path != null;
        Node node = getRoot();
        for (Path.Segment segment : path) {
            Node desiredChild = null;
            for (Node child : node.getChildren()) {
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

    protected UUID generateUuid() {
        return UUID.randomUUID();
    }

    public void removeNode( ExecutionEnvironment env,
                            Node node ) {
        assert env != null;
        assert node != null;
        assert getRoot().equals(node) != true;
        Node parent = node.getParent();
        assert parent != null;
        parent.getChildren().remove(node);
        correctSameNameSiblingIndexes(env, parent, node.getName().getName());
        removeUuidReference(node);
    }

    protected void removeUuidReference( Node node ) {
        nodesByUuid.remove(node.getUuid());
        for (Node child : node.getChildren()) {
            removeUuidReference(child);
        }
    }

    /**
     * Create a node at the supplied path. The parent of the new node must already exist.
     * 
     * @param env the environment; may not be null
     * @param pathToNewNode the path to the new node; may not be null
     * @return the new node (or root if the path specified the root)
     */
    public Node createNode( ExecutionEnvironment env,
                            String pathToNewNode ) {
        assert env != null;
        assert pathToNewNode != null;
        Path path = env.getValueFactories().getPathFactory().create(pathToNewNode);
        if (path.isRoot()) return getRoot();
        Path parentPath = path.getAncestor();
        Node parentNode = getNode(parentPath);
        Name name = path.getLastSegment().getName();
        return createNode(env, parentNode, name);
    }

    /**
     * Create a new node with the supplied name, as a child of the supplied parent.
     * 
     * @param env the execution environment
     * @param parentNode the parent node; may not be null
     * @param name the name; may not be null
     * @return the new node
     */
    public Node createNode( ExecutionEnvironment env,
                            Node parentNode,
                            Name name ) {
        assert env != null;
        assert name != null;
        if (parentNode == null) parentNode = getRoot();
        Node node = new Node(generateUuid());
        nodesByUuid.put(node.getUuid(), node);
        node.setParent(parentNode);
        Path.Segment newName = env.getValueFactories().getPathFactory().createSegment(name);
        node.setName(newName);
        parentNode.getChildren().add(node);
        correctSameNameSiblingIndexes(env, parentNode, name);
        return node;
    }

    protected void correctSameNameSiblingIndexes( ExecutionEnvironment env,
                                                  Node parentNode,
                                                  Name name ) {
        if (parentNode == null) return;
        // Look for the highest existing index ...
        List<Node> childrenWithSameNames = new LinkedList<Node>();
        for (Node child : parentNode.getChildren()) {
            if (child.getName().getName().equals(name)) childrenWithSameNames.add(child);
        }
        if (childrenWithSameNames.size() == 0) return;
        if (childrenWithSameNames.size() == 1) {
            Node childWithSameName = childrenWithSameNames.get(0);
            Path.Segment newName = env.getValueFactories().getPathFactory().createSegment(name, Path.NO_INDEX);
            childWithSameName.setName(newName);
            return;
        }
        int index = 1;
        for (Node childWithSameName : childrenWithSameNames) {
            Path.Segment segment = childWithSameName.getName();
            if (segment.getIndex() != index) {
                Path.Segment newName = env.getValueFactories().getPathFactory().createSegment(name, index);
                childWithSameName.setName(newName);
            }
            ++index;
        }
    }

    /**
     * Move the supplied node to the new parent. This method automatically removes the node from its existing parent, and also
     * correctly adjusts the {@link Path.Segment#getIndex() index} to be correct in the new parent.
     * 
     * @param env
     * @param node the node to be moved; may not be the {@link #getRoot() root}
     * @param newParent the new parent; may not be the {@link #getRoot() root}
     */
    public void moveNode( ExecutionEnvironment env,
                          Node node,
                          Node newParent ) {
        assert env != null;
        assert newParent != null;
        assert node != null;
        assert getRoot().equals(newParent) != true;
        assert getRoot().equals(node) != true;
        Node oldParent = node.getParent();
        if (oldParent != null) {
            if (oldParent.equals(newParent)) return;
            boolean removed = oldParent.getChildren().remove(node);
            assert removed == true;
            node.setParent(null);
            correctSameNameSiblingIndexes(env, oldParent, node.getName().getName());
        }
        node.setParent(newParent);
        newParent.getChildren().add(node);
        correctSameNameSiblingIndexes(env, newParent, node.getName().getName());
    }

    public int copyNode( ExecutionEnvironment env,
                         Node original,
                         Node newParent,
                         boolean recursive ) {
        assert env != null;
        assert original != null;
        assert newParent != null;
        // Get or create the new node ...
        Node copy = createNode(env, newParent, original.getName().getName());

        // Copy the properties ...
        copy.getProperties().clear();
        copy.getProperties().putAll(original.getProperties());
        int numNodesCopied = 1;
        if (recursive) {
            // Loop over each child and call this method ...
            for (Node child : original.getChildren()) {
                numNodesCopied += copyNode(env, child, copy, true);
            }
        }
        return numNodesCopied;
    }
}
