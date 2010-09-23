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

import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.graph.Location;
import org.modeshape.graph.property.Path;

/**
 * The {@link Workspace} implementation that represents all nodes as {@link PathNode} objects and stores them in an internal data
 * structure that allows for nodes to be accessed via a {@link Path}.
 * <p>
 * Subclasses are required to provide thread-safe access and modification of the state within the encapsulated data structure,
 * since multiple {@link Transaction} implementations may be {@link Transaction#commit() committing} changes to the data structure
 * at the same time. However, this class does not provide any thread-safety, since the nature of the thread-safety will almost
 * certainly depend on the actual implementation. For example, a subclass may use a {@link ReadWriteLock lock}, or it may use a
 * map implementation that provides the thread-safety.
 * </p>
 * 
 * @param <NodeType> the type of node
 */
@NotThreadSafe
public abstract class PathWorkspace<NodeType extends PathNode> implements Workspace {

    private final String name;
    private final UUID rootNodeUuid;

    /**
     * Create a new instance of the workspace.
     * 
     * @param name the workspace name; may not be null
     * @param rootNodeUuid the root node that is expected to already exist in the map
     */
    public PathWorkspace( String name,
                          UUID rootNodeUuid ) {
        this.name = name;
        this.rootNodeUuid = rootNodeUuid;
        assert this.name != null;
        assert this.rootNodeUuid != null;
    }

    /**
     * Create a new instance of the workspace.
     * 
     * @param name the workspace name; may not be null
     * @param originalToClone the workspace that is to be cloned; may not be null
     */
    public PathWorkspace( String name,
                          PathWorkspace<NodeType> originalToClone ) {
        this.name = name;
        this.rootNodeUuid = originalToClone.getRootNode().getUuid();
        assert this.name != null;
        assert this.rootNodeUuid != null;
        throw new UnsupportedOperationException("Need to implement the ability to clone a workspace");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.Workspace#getName()
     */
    public String getName() {
        return name;
    }

    protected UUID getRootNodeUuid() {
        return rootNodeUuid;
    }

    /**
     * Get the root node in this workspace.
     * 
     * @return the root node; never null
     */
    public abstract NodeType getRootNode();

    /**
     * Get the node with the supplied path.
     * 
     * @param path the path to the node
     * @return the node state as known by this workspace, or null if no such node exists in this workspace
     */
    public abstract NodeType getNode( Path path );

    /**
     * Verify that the supplied node exists.
     * 
     * @param path the path of the node; may not be null
     * @return the location of the node if it exists, or null if it does not
     */
    public Location verifyNodeExists( Path path ) {
        NodeType node = getNode(path);
        return node != null ? Location.create(path) : null;
    }

    /**
     * Save this node into the workspace, overwriting any previous record of the node. This method should be overridden by
     * writable path workspace implementations that use the default {@link ChangeCommand} implementations.
     * 
     * @param node the new node; may not be null
     * @return the previous node state, or null if the node is new to this workspace
     * @throws UnsupportedOperationException by default, subclasses should override this method so that this exception is not
     *         thrown
     * @see #createMoveCommand(PathNode, PathNode)
     * @see #createPutCommand(PathNode, PathNode)
     * @see #createRemoveCommand(Path)
     */
    public NodeType putNode( NodeType node ) {
        throw new UnsupportedOperationException();
    }

    /**
     * Move the node from it's previous location to the new location, overwriting any previous node at that location. This method
     * should be overridden by writable path workspace implementations that use the default {@link ChangeCommand} implementations.
     * <p>
     * The move operation is intended to reflect changes to the node's {@link PathNode#getName() name} or
     * {@link PathNode#getParent() parent} only. Changes to the children or properties of the node should be reflected separately
     * in a {@link #putNode(PathNode) put command} of some sort. The details of the put command are implementation-specific.
     * </p>
     * 
     * @param source the original version of the node to be moved; may not be null
     * @param target the new version (implying a change to the name or parent) of the node to be moved; may not be null
     * @return the new node state;may not be null
     * @throws UnsupportedOperationException by default, subclasses should override this method so that this exception is not
     *         thrown
     * @see #createMoveCommand(PathNode, PathNode)
     * @see #createPutCommand(PathNode, PathNode)
     * @see #createRemoveCommand(Path)
     */
    public NodeType moveNode( NodeType source,
                              NodeType target ) {
        throw new UnsupportedOperationException();
    }

    /**
     * Remove this node and its descendants from the workspace. This method should be overridden by writable path workspace
     * implementations that use the default {@link ChangeCommand} implementations.
     * 
     * @param path the path to the node to be removed; may not be null
     * @return the previous node state, or null if the node does not exist in this workspace
     * @throws UnsupportedOperationException by default, subclasses should override this method so that this exception is not
     *         thrown
     * @see #createMoveCommand(PathNode, PathNode)
     * @see #createPutCommand(PathNode, PathNode)
     * @see #createRemoveCommand(Path)
     */
    public NodeType removeNode( Path path ) {
        throw new UnsupportedOperationException();
    }

    /**
     * Remove all of the nodes in this workspace, and make sure there is a single root node with no properties and no children.
     */
    public void removeAll() {
        throw new UnsupportedOperationException();

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

    /**
     * Successively (and in order) apply the changes from the list of pending commands
     * <p>
     * All validation for each of the objects (including validation of resource availability in the underlying persistent store)
     * should be performed prior to invoking this method.
     * </p>
     * 
     * @param commands the list of commands to apply
     */
    public void commit( List<ChangeCommand<NodeType>> commands ) {
        for (ChangeCommand<NodeType> command : commands) {
            command.apply();
        }
    }

    /**
     * Create a change command for the required update to the given node
     * 
     * @param oldNode the prior version of the node; may be null if this is a new node
     * @param node the new version of the node; may not be null
     * @return a {@link ChangeCommand} instance that reflects the changes to the node
     * @see #commit(List)
     */
    public ChangeCommand<NodeType> createPutCommand( NodeType oldNode,
                                                     NodeType node ) {
        return new PutCommand(node);
    }

    /**
     * Create a change command for the removal of the given node and its descendants
     * 
     * @param path the path to the node at the root of the branch to be removed; may not be null
     * @return a {@link ChangeCommand} instance that reflects the changes to the node
     * @see #createPutCommand(PathNode, PathNode)
     * @see #commit(List)
     */
    public ChangeCommand<NodeType> createRemoveCommand( Path path ) {
        return new RemoveCommand(path);
    }

    /**
     * Create a change command that represents the movement of a node. The movement record will only reflect the changes to the
     * node's name and/or parent. Changes to the node's properties or children should be ignored. A separate
     * {@link #createPutCommand(PathNode, PathNode) put command} should be used to reflect these changes.
     * 
     * @param source the original version of the node; may not be null
     * @param target the new version of the node; may not be null
     * @return a {@link ChangeCommand} instance that reflects the changes to the node
     * @see #createMoveCommand(PathNode, PathNode)
     * @see #commit(List)
     */
    public ChangeCommand<NodeType> createMoveCommand( NodeType source,
                                                      NodeType target ) {
        return new MoveCommand(source, target);
    }

    /**
     * A specific operation that mutates the underlying persistent repository.
     * 
     * @param <NodeType> the type of node against which this change should apply
     */
    public interface ChangeCommand<NodeType extends PathNode> {
        /**
         * Make the change represented by this command permanent.
         */
        void apply();
    }

    private class PutCommand implements ChangeCommand<NodeType> {
        private NodeType node;

        protected PutCommand( NodeType node ) {
            super();
            this.node = node;
        }

        public void apply() {
            PathWorkspace.this.putNode(node);
        }

        @Override
        public String toString() {
            return "Put: { " + node + "}";
        }
    }

    private class RemoveCommand implements ChangeCommand<NodeType> {
        private Path path;

        protected RemoveCommand( Path path ) {
            super();
            this.path = path;
        }

        public void apply() {
            PathWorkspace.this.removeNode(path);
        }

        @Override
        public String toString() {
            return "Remove: { " + path.getString() + "}";
        }
    }

    private class MoveCommand implements ChangeCommand<NodeType> {
        private NodeType node;
        private NodeType newNode;

        protected MoveCommand( NodeType node,
                               NodeType newNode ) {
            super();
            this.node = node;
            this.newNode = newNode;
        }

        public void apply() {
            PathWorkspace.this.moveNode(node, newNode);
        }

        @Override
        public String toString() {
            return "Move: { " + node + " to " + newNode + "}";
        }
    }

}
