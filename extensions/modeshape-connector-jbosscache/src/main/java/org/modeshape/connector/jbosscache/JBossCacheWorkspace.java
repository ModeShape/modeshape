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
package org.modeshape.connector.jbosscache;

import java.util.UUID;
import org.jboss.cache.Cache;
import org.jboss.cache.Fqn;
import org.jboss.cache.Node;
import org.modeshape.graph.connector.base.MapWorkspace;

/**
 * 
 */
public class JBossCacheWorkspace extends MapWorkspace<JBossCacheNode> {

    protected static Node<UUID, JBossCacheNode> findOrCreateWorkspaceRoot( String workspaceName,
                                                                           Cache<UUID, JBossCacheNode> workspaceCache,
                                                                           UUID rootNodeUuid ) {
        assert workspaceName != null;
        assert workspaceCache != null;
        assert rootNodeUuid != null;

        // Make sure the cache has a node for the workspace ...
        Node<UUID, JBossCacheNode> workspace = workspaceCache.getRoot().getChild(Fqn.fromElements(workspaceName));
        if (workspace == null) {
            workspace = workspaceCache.getRoot().addChild(Fqn.fromElements(workspaceName));
        }
        // Make sure there is a root node ...
        JBossCacheNode rootNode = workspace.get(rootNodeUuid);
        if (rootNode == null) {
            rootNode = new JBossCacheNode(rootNodeUuid);
            workspace.put(rootNodeUuid, rootNode);
        }
        return workspace;
    }

    private Cache<UUID, JBossCacheNode> workspaceCache;
    private Node<UUID, JBossCacheNode> workspaceNode;

    /**
     * Create a new workspace instance.
     * 
     * @param name the name of the workspace
     * @param workspaceCache the Infinispan cache containing the workspace content
     * @param rootNode the root node for the workspace
     */
    public JBossCacheWorkspace( String name,
                                Cache<UUID, JBossCacheNode> workspaceCache,
                                JBossCacheNode rootNode ) {
        super(name, rootNode);
        this.workspaceCache = workspaceCache;
        this.workspaceNode = findOrCreateWorkspaceRoot(this.name, this.workspaceCache, this.rootNodeUuid);
    }

    /**
     * Create a new workspace instance.
     * 
     * @param name the name of the workspace
     * @param workspaceCache the Infinispan cache containing the workspace content
     * @param originalToClone the workspace that is to be cloned
     */
    public JBossCacheWorkspace( String name,
                                Cache<UUID, JBossCacheNode> workspaceCache,
                                JBossCacheWorkspace originalToClone ) {
        super(name, originalToClone);
        this.workspaceCache = workspaceCache;
        this.workspaceNode = findOrCreateWorkspaceRoot(this.name, this.workspaceCache, this.rootNodeUuid);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.MapWorkspace#getNode(java.util.UUID)
     */
    @Override
    public JBossCacheNode getNode( UUID uuid ) {
        assert uuid != null;
        return workspaceNode.get(uuid);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.MapWorkspace#putNode(org.modeshape.graph.connector.base.MapNode)
     */
    @Override
    public JBossCacheNode putNode( JBossCacheNode node ) {
        assert node != null;
        JBossCacheNode result = workspaceNode.put(node.getUuid(), node);
        workspaceNode = findOrCreateWorkspaceRoot(this.name, this.workspaceCache, this.rootNodeUuid);
        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.MapWorkspace#removeNode(java.util.UUID)
     */
    @Override
    public JBossCacheNode removeNode( UUID uuid ) {
        assert uuid != null;
        JBossCacheNode result = workspaceNode.remove(uuid);
        workspaceNode = findOrCreateWorkspaceRoot(this.name, this.workspaceCache, this.rootNodeUuid);
        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.MapWorkspace#removeAll()
     */
    @Override
    public void removeAll() {
        workspaceNode.clearData();
        workspaceNode = findOrCreateWorkspaceRoot(this.name, this.workspaceCache, this.rootNodeUuid);
    }

    /**
     * This method shuts down the workspace and makes it no longer usable. This method should also only be called once.
     */
    public void shutdown() {
        this.workspaceCache.stop();
    }

}
