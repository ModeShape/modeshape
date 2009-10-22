package org.jboss.dna.connector.infinispan;

import java.util.UUID;
import org.infinispan.Cache;
import org.infinispan.manager.CacheManager;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.connector.LockFailedException;
import org.jboss.dna.graph.connector.map.AbstractMapWorkspace;
import org.jboss.dna.graph.connector.map.MapNode;
import org.jboss.dna.graph.connector.map.MapRepository;
import org.jboss.dna.graph.connector.map.MapWorkspace;
import org.jboss.dna.graph.request.LockBranchRequest.LockScope;

/**
 * The repository that uses an Infinispan instance.
 */
public class InfinispanRepository extends MapRepository {

    private final CacheManager cacheManager;

    public InfinispanRepository( String sourceName,
                                 UUID rootNodeUuid,
                                 CacheManager cacheManager ) {
        super(sourceName, rootNodeUuid, null);
        assert cacheManager != null;
        this.cacheManager = cacheManager;
        initialize();
    }

    public InfinispanRepository( String sourceName,
                                 UUID rootNodeUuid,
                                 String defaultWorkspaceName,
                                 CacheManager cacheManager ) {
        super(sourceName, rootNodeUuid, defaultWorkspaceName);

        assert cacheManager != null;
        this.cacheManager = cacheManager;

        initialize();
    }

    @Override
    protected MapWorkspace createWorkspace( ExecutionContext context,
                                            String name ) {
        assert name != null;
        assert cacheManager != null;
        Cache<UUID, MapNode> newWorkspaceCache = cacheManager.getCache(name);
        return new Workspace(this, name, newWorkspaceCache);
    }

    protected class Workspace extends AbstractMapWorkspace {
        private final Cache<UUID, MapNode> workspaceCache;

        public Workspace( MapRepository repository,
                          String name,
                          Cache<UUID, MapNode> workspaceCache ) {
            super(repository, name);

            this.workspaceCache = workspaceCache;
            initialize();
        }

        @Override
        protected void addNodeToMap( MapNode node ) {
            assert node != null;
            workspaceCache.put(node.getUuid(), node);
        }

        @Override
        protected MapNode removeNodeFromMap( UUID nodeUuid ) {
            assert nodeUuid != null;
            return workspaceCache.remove(nodeUuid);
        }

        @Override
        protected void removeAllNodesFromMap() {
            workspaceCache.clear();
        }

        @Override
        public MapNode getNode( UUID nodeUuid ) {
            assert nodeUuid != null;
            return workspaceCache.get(nodeUuid);
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
