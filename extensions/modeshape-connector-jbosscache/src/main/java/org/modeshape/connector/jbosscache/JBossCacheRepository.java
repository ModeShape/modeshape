package org.modeshape.connector.jbosscache;

import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.jboss.cache.Cache;
import org.jboss.cache.Fqn;
import org.jboss.cache.Node;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.connector.LockFailedException;
import org.modeshape.graph.connector.map.AbstractMapWorkspace;
import org.modeshape.graph.connector.map.LockBasedTransaction;
import org.modeshape.graph.connector.map.MapNode;
import org.modeshape.graph.connector.map.MapRepository;
import org.modeshape.graph.connector.map.MapRepositoryTransaction;
import org.modeshape.graph.connector.map.MapWorkspace;
import org.modeshape.graph.request.LockBranchRequest.LockScope;

/**
 * A repository implementation that uses JBoss Cache.
 */
public class JBossCacheRepository extends MapRepository {

    protected final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Cache<UUID, MapNode> cache;

    public JBossCacheRepository( String sourceName,
                                 UUID rootNodeUuid,
                                 Cache<UUID, MapNode> cache ) {
        super(sourceName, rootNodeUuid, null);
        assert cache != null;
        this.cache = cache;
        initialize();
    }

    public JBossCacheRepository( String sourceName,
                                 UUID rootNodeUuid,
                                 String defaultWorkspaceName,
                                 Cache<UUID, MapNode> cache ) {
        super(sourceName, rootNodeUuid, defaultWorkspaceName);

        assert cache != null;
        this.cache = cache;

        initialize();
    }

    @Override
    protected MapWorkspace createWorkspace( ExecutionContext context,
                                            String name ) {
        assert name != null;
        assert cache != null;
        Node<UUID, MapNode> newWorkspaceNode = cache.getRoot().addChild(Fqn.fromElements(name));
        return new Workspace(this, name, newWorkspaceNode);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.map.MapRepository#startTransaction(boolean)
     */
    @Override
    public MapRepositoryTransaction startTransaction( boolean readonly ) {
        return new LockBasedTransaction(readonly ? lock.readLock() : lock.writeLock()) {
            /**
             * {@inheritDoc}
             * 
             * @see org.modeshape.graph.connector.map.LockBasedTransaction#commit()
             */
            @Override
            public void commit() {
                super.commit();
            }

            /**
             * {@inheritDoc}
             * 
             * @see org.modeshape.graph.connector.map.LockBasedTransaction#rollback()
             */
            @Override
            public void rollback() {
                super.rollback();
            }
        };
    }

    protected ReadWriteLock getLock() {
        return lock;
    }

    protected class Workspace extends AbstractMapWorkspace {
        private final Node<UUID, MapNode> workspaceNode;

        public Workspace( MapRepository repository,
                          String name,
                          Node<UUID, MapNode> workspaceNode ) {
            super(repository, name);

            this.workspaceNode = workspaceNode;
            initialize();
        }

        @Override
        protected void addNodeToMap( MapNode node ) {
            assert node != null;
            workspaceNode.put(node.getUuid(), node);
        }

        @Override
        protected MapNode removeNodeFromMap( UUID nodeUuid ) {
            assert nodeUuid != null;
            return workspaceNode.remove(nodeUuid);
        }

        @Override
        protected void removeAllNodesFromMap() {
            workspaceNode.clearData();
        }

        @Override
        public MapNode getNode( UUID nodeUuid ) {
            assert nodeUuid != null;
            return workspaceNode.get(nodeUuid);
        }

        public void lockNode( MapNode node,
                              LockScope lockScope,
                              long lockTimeoutInMillis ) throws LockFailedException {
            // Locking is not supported by this connector
        }

        public void unlockNode( MapNode node ) {
            // Locking is not supported by this connector
        }

    }

}
