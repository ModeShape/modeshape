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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import net.jcip.annotations.NotThreadSafe;

/**
 * The {@link Workspace} implementation that represents all nodes as {@link MapNode} objects and stores them within a {@link Map}
 * keyed by their UUID.
 * <p>
 * Subclasses are required to provide thread-safe access and modification of the state within the encapsulated map, since multiple
 * {@link Transaction} implementations may be {@link Transaction#commit() committing} changes to the map at the same time.
 * However, this class does not provide any thread-safety, since the nature of the thread-safety will almost certainly depend on
 * the actual map implementation. For example, a subclass may use a {@link ReadWriteLock lock}, or it may use a map implementation
 * that provides the thread-safety.
 * </p>
 * 
 * @param <NodeType> the type of node
 */
@NotThreadSafe
public class MapWorkspace<NodeType extends MapNode> implements Workspace {

    private final String name;
    private final UUID rootNodeUuid;
    private final Map<UUID, NodeType> nodesByUuid;

    /**
     * Create a new instance of the workspace.
     * 
     * @param name the workspace name; may not be null
     * @param nodesByUuid the map of nodes keyed by their UUIDs; may not be null
     * @param rootNode the root node that is expected to already exist in the map
     */
    public MapWorkspace( String name,
                         Map<UUID, NodeType> nodesByUuid,
                         NodeType rootNode ) {
        this.name = name;
        this.rootNodeUuid = rootNode.getUuid();
        this.nodesByUuid = nodesByUuid;
        if (!this.nodesByUuid.containsKey(rootNodeUuid)) {
            this.nodesByUuid.put(rootNodeUuid, rootNode);
        }
        assert this.name != null;
        assert this.rootNodeUuid != null;
        assert this.nodesByUuid != null;
    }

    /**
     * Create a new instance of the workspace.
     * 
     * @param name the workspace name; may not be null
     * @param nodesByUuid the map of nodes keyed by their UUIDs; may not be null
     * @param originalToClone the workspace that is to be cloned; may not be null
     */
    public MapWorkspace( String name,
                         Map<UUID, NodeType> nodesByUuid,
                         MapWorkspace<NodeType> originalToClone ) {
        this.name = name;
        this.nodesByUuid = nodesByUuid;
        this.nodesByUuid.putAll(originalToClone.nodesByUuid); // make a copy
        this.rootNodeUuid = originalToClone.getRootNode().getUuid();
        assert this.name != null;
        assert this.rootNodeUuid != null;
        assert this.nodesByUuid != null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.Workspace#getName()
     */
    public String getName() {
        return name;
    }

    /**
     * Get the root node in this workspace.
     * 
     * @return the root node; never null
     */
    public NodeType getRootNode() {
        return nodesByUuid.get(rootNodeUuid);
    }

    /**
     * Get the node with the supplied UUID.
     * 
     * @param uuid the UUID of the node
     * @return the node state as known by this workspace, or null if no such node exists in this workspace
     */
    public NodeType getNode( UUID uuid ) {
        return nodesByUuid.get(uuid);
    }

    /**
     * Add the node into this workspace's map, overwriting any previous record of the node
     * 
     * @param node the new node; may not be null
     * @return the previous node state, or null if the node is new to this workspace
     */
    public NodeType putNode( NodeType node ) {
        return nodesByUuid.put(node.getUuid(), node);
    }

    /**
     * Remove and return the node with the supplied UUID. This method will never remove the root node.
     * 
     * @param uuid the UUID of the node to be removed
     * @return the node that was removed, or null if the supplied UUID is the root node's UUID or if this workspace does not
     *         contain a node with the supplied UUID
     */
    public NodeType removeNode( UUID uuid ) {
        return rootNodeUuid.equals(uuid) ? null : nodesByUuid.remove(uuid);
    }

    /**
     * Remove all of the nodes in this workspace, and make sure there is a single root node with no properties and no children.
     */
    @SuppressWarnings( "unchecked" )
    public void removeAll() {
        NodeType newRootNode = (NodeType)getRootNode().withoutChildren().withoutProperties().freeze();
        nodesByUuid.clear();
        nodesByUuid.put(newRootNode.getUuid(), newRootNode);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return name;
    }
}
