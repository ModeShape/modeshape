package org.jboss.dna.connector.jbosscache;

import java.util.UUID;
import org.jboss.cache.Cache;
import org.jboss.cache.Fqn;
import org.jboss.cache.Node;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.connector.LockFailedException;
import org.jboss.dna.graph.connector.map.AbstractMapWorkspace;
import org.jboss.dna.graph.connector.map.MapNode;
import org.jboss.dna.graph.connector.map.MapRepository;
import org.jboss.dna.graph.connector.map.MapWorkspace;
import org.jboss.dna.graph.request.LockBranchRequest.LockScope;

/**
 * A repository implementation that uses JBoss Cache.
 */
public class JBossCacheRepository extends MapRepository {

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
