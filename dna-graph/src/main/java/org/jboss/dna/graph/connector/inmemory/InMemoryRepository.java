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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.connector.LockFailedException;
import org.jboss.dna.graph.connector.map.AbstractMapWorkspace;
import org.jboss.dna.graph.connector.map.LockBasedTransaction;
import org.jboss.dna.graph.connector.map.MapNode;
import org.jboss.dna.graph.connector.map.MapRepository;
import org.jboss.dna.graph.connector.map.MapRepositoryTransaction;
import org.jboss.dna.graph.connector.map.MapWorkspace;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.request.LockBranchRequest.LockScope;

/**
 * A specialized {@link MapRepository} that represents an in-memory repository.
 */
@NotThreadSafe
public class InMemoryRepository extends MapRepository {

    protected final ReadWriteLock lock = new ReentrantReadWriteLock();

    public InMemoryRepository( String sourceName,
                               UUID rootNodeUuid ) {
        super(sourceName, rootNodeUuid, null);
        initialize();
    }

    public InMemoryRepository( String sourceName,
                               UUID rootNodeUuid,
                               String defaultWorkspaceName ) {
        super(sourceName, rootNodeUuid, defaultWorkspaceName);
        initialize();
    }

    @Override
    @GuardedBy( "getLock()" )
    protected MapWorkspace createWorkspace( ExecutionContext context,
                                            String name ) {
        return new Workspace(this, name);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.map.MapRepository#startTransaction(boolean)
     */
    @Override
    public MapRepositoryTransaction startTransaction( boolean readonly ) {
        return new LockBasedTransaction(readonly ? lock.readLock() : lock.writeLock()) {
            /**
             * {@inheritDoc}
             * 
             * @see org.jboss.dna.graph.connector.map.LockBasedTransaction#commit()
             */
            @Override
            public void commit() {
                // Get rid of the state of each workspace ...
                releaseBackups();
                // Then let the superclass do its thing (like releasing the lock) ...
                super.commit();
            }

            /**
             * {@inheritDoc}
             * 
             * @see org.jboss.dna.graph.connector.map.LockBasedTransaction#rollback()
             */
            @Override
            public void rollback() {
                // Restore the state of each workspace ...
                restoreBackups();
                // Then let the superclass do its thing (like releasing the lock) ...
                super.rollback();
            }
        };
    }

    protected ReadWriteLock getLock() {
        return lock;
    }

    @GuardedBy( "lock" )
    protected void restoreBackups() {
        for (String name : getWorkspaceNames()) {
            Workspace workspace = (Workspace)getWorkspace(name);
            workspace.restoreBackups();
        }
    }

    @GuardedBy( "lock" )
    protected void releaseBackups() {
        for (String name : getWorkspaceNames()) {
            Workspace workspace = (Workspace)getWorkspace(name);
            workspace.releaseBackups();
        }
    }

    protected class Workspace extends AbstractMapWorkspace implements InMemoryNode.ChangeListener {
        private final Map<UUID, MapNode> nodesByUuid = new HashMap<UUID, MapNode>();
        /**
         * The record of the node state before changes were made. This is used by the
         * {@link InMemoryRepository#startTransaction(boolean) transactions} to restore the state upon a
         * {@link MapRepositoryTransaction#rollback() rollback}.
         * <p>
         * This will be created the first time a node is changed, and is then used (and cleared) when #rollbackChanges() rolling
         * back changes}.
         * </p>
         */
        private Map<UUID, InMemoryNodeState> stateBeforeChanges;

        public Workspace( MapRepository repository,
                          String name ) {
            super(repository, name);
            initialize();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.connector.map.AbstractMapWorkspace#createMapNode(java.util.UUID)
         */
        @Override
        protected MapNode createMapNode( UUID uuid ) {
            assert uuid != null;
            return new InMemoryNode(this, uuid);
        }

        @Override
        protected void addNodeToMap( MapNode node ) {
            assert node != null;
            assert nodesByUuid != null;
            nodesByUuid.put(node.getUuid(), node);
        }

        @Override
        protected MapNode removeNodeFromMap( UUID nodeUuid ) {
            assert nodeUuid != null;
            return nodesByUuid.remove(nodeUuid);
        }

        @Override
        protected void removeAllNodesFromMap() {
            nodesByUuid.clear();
        }

        @Override
        public MapNode getNode( UUID nodeUuid ) {
            assert nodeUuid != null;
            return nodesByUuid.get(nodeUuid);
        }

        /**
         * This should copy the subgraph given by the original node and place the new copy under the supplied new parent. Note
         * that internal references between nodes within the original subgraph must be reflected as internal nodes within the new
         * subgraph.
         * <p>
         * This method is trivially overridden for testing purposes.
         * </p>
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
        @Override
        protected MapNode copyNode( ExecutionContext context,
                                    MapNode original,
                                    MapWorkspace newWorkspace,
                                    MapNode newParent,
                                    Name desiredName,
                                    boolean recursive,
                                    Map<UUID, UUID> oldToNewUuids ) {
            return super.copyNode(context, original, newWorkspace, newParent, desiredName, recursive, oldToNewUuids);
        }

        public void lockNode( MapNode node,
                              LockScope lockScope,
                              long lockTimeoutInMillis ) throws LockFailedException {
            // Locking is not supported by this connector
        }

        public void unlockNode( MapNode node ) {
            // Locking is not supported by this connector
        }

        /**
         * Method added to support testing
         * 
         * @return the number of nodes in the backing map
         */
        final int size() {
            return nodesByUuid.size();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.connector.inmemory.InMemoryNode.ChangeListener#prepareForChange(org.jboss.dna.graph.connector.inmemory.InMemoryNode)
         */
        public void prepareForChange( InMemoryNode node ) {
            assert node != null;
            UUID uuid = node.getUuid();
            if (!nodesByUuid.containsKey(uuid)) return;
            if (stateBeforeChanges == null) {
                stateBeforeChanges = new HashMap<UUID, InMemoryNodeState>();
                stateBeforeChanges.put(uuid, new InMemoryNodeState(node));
            } else {
                assert stateBeforeChanges != null;
                if (!stateBeforeChanges.containsKey(uuid)) {
                    stateBeforeChanges.put(uuid, new InMemoryNodeState(node));
                }
            }
        }

        protected void restoreBackups() {
            if (this.stateBeforeChanges != null) {
                for (Map.Entry<UUID, InMemoryNodeState> originalState : stateBeforeChanges.entrySet()) {
                    UUID uuid = originalState.getKey();
                    InMemoryNodeState state = originalState.getValue();
                    InMemoryNode node = (InMemoryNode)nodesByUuid.get(uuid);
                    if (node == null) {
                        // It must have been deleted ...
                        node = state.getOriginalNode();
                    }
                    node.restoreFrom(state);
                }
                stateBeforeChanges = null;
            }
        }

        protected void releaseBackups() {
            this.stateBeforeChanges = null;
        }
    }

    @Immutable
    protected static final class InMemoryNodeState {
        private Path.Segment name;
        private final Set<Property> properties;
        private final List<MapNode> children;
        private final Set<Name> uniqueChildNames;
        private final InMemoryNode node;
        private final MapNode parent;

        protected InMemoryNodeState( InMemoryNode node ) {
            this.parent = node.getParent();
            this.node = node;
            this.name = node.getName();
            this.properties = new HashSet<Property>(node.getProperties().values());
            this.children = new ArrayList<MapNode>(node.getChildren());
            this.uniqueChildNames = new HashSet<Name>(node.getUniqueChildNames());
        }

        /**
         * @return name
         */
        public Path.Segment getName() {
            return name;
        }

        /**
         * @return children
         */
        public List<MapNode> getChildren() {
            return children;
        }

        /**
         * @return properties
         */
        public Set<Property> getProperties() {
            return properties;
        }

        /**
         * @return uniqueChildNames
         */
        public Set<Name> getUniqueChildNames() {
            return uniqueChildNames;
        }

        /**
         * @return node
         */
        public InMemoryNode getOriginalNode() {
            return node;
        }

        /**
         * @return parent
         */
        public MapNode getParent() {
            return parent;
        }
    }
}
