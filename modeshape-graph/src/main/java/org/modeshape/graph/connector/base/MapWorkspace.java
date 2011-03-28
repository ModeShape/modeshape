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

import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import org.modeshape.common.annotation.NotThreadSafe;

/**
 * The {@link Workspace} implementation that represents all nodes as {@link MapNode} objects and stores them keyed by their
 * UUID.
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
public abstract class MapWorkspace<NodeType extends MapNode> implements Workspace {

    protected final String name;
    protected final UUID rootNodeUuid;

    /**
     * Create a new instance of the workspace.
     * 
     * @param name the workspace name; may not be null
     * @param rootNode the root node that is expected to already exist in the map
     */
    public MapWorkspace( String name,
                            NodeType rootNode ) {
        this.name = name;
        this.rootNodeUuid = rootNode.getUuid();
        assert this.name != null;
        assert this.rootNodeUuid != null;
    }

    /**
     * Create a new instance of the workspace.
     * 
     * @param name the workspace name; may not be null
     * @param originalToClone the workspace that is to be cloned; may not be null
     */
    public MapWorkspace( String name,
                            MapWorkspace<NodeType> originalToClone ) {
        this.name = name;
        this.rootNodeUuid = originalToClone.getRootNode().getUuid();
        assert this.name != null;
        assert this.rootNodeUuid != null;
    }

    /**
     * Get the UUID of the root node.
     * 
     * @return rootNodeUuid
     */
    public UUID getRootNodeUuid() {
        return rootNodeUuid;
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
        return getNode(rootNodeUuid);
    }

    /**
     * Get the node with the supplied UUID.
     * 
     * @param uuid the UUID of the node
     * @return the node state as known by this workspace, or null if no such node exists in this workspace
     */
    public abstract NodeType getNode( UUID uuid );

    /**
     * Add the node into this workspace's map, overwriting any previous record of the node
     * 
     * @param node the new node; may not be null
     * @return the previous node state, or null if the node is new to this workspace
     */
    public abstract NodeType putNode( NodeType node );

    /**
     * Remove and return the node with the supplied UUID. This method will never remove the root node.
     * 
     * @param uuid the UUID of the node to be removed
     * @return the node that was removed, or null if the supplied UUID is the root node's UUID or if this workspace does not
     *         contain a node with the supplied UUID
     */
    public abstract NodeType removeNode( UUID uuid );

    /**
     * Remove all of the nodes in this workspace, and make sure there is a single root node with no properties and no children.
     */
    public abstract void removeAll();

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
