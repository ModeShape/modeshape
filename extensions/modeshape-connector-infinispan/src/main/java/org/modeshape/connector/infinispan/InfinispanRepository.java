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
package org.modeshape.connector.infinispan;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.infinispan.Cache;
import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.EmbeddedCacheManager;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.connector.base.Repository;

/**
 * The representation of an in-memory repository and its content.
 */
@ThreadSafe
public class InfinispanRepository extends Repository<InfinispanNode, InfinispanWorkspace> {

    private final CacheContainer cacheContainer;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Set<String> predefinedWorkspaceNames;

    public InfinispanRepository( BaseInfinispanSource source,
                                 CacheContainer cacheContainer ) {
        super(source);
        this.cacheContainer = cacheContainer;
        assert this.cacheContainer != null;
        Set<String> workspaceNames = new HashSet<String>();
        for (String workspaceName : source.getPredefinedWorkspaceNames()) {
            workspaceNames.add(workspaceName);
        }
        this.predefinedWorkspaceNames = Collections.unmodifiableSet(workspaceNames);
        initialize();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.Repository#getWorkspaceNames()
     */
    @Override
    public Set<String> getWorkspaceNames() {
        Set<String> names = new HashSet<String>(super.getWorkspaceNames());
        names.addAll(predefinedWorkspaceNames);
        // Look for any new caches ...
        names.addAll(getAllWorkspaceNames(names));

        return Collections.unmodifiableSet(names);
    }

    /**
     * Utility method to obtain all of the names of the workspaces within the supplied cache container. In case the method needs
     * to validate the caches to see if they are workspaces, this method also takes the names of the caches that are known to be
     * valid workspaces.
     * 
     * @param alreadyKnownNames the names of the workspaces that are already known; may not be null but may be empty
     * @return the names of all available workspaces in the supplied container; never null but possibly empty
     */
    protected Set<String> getAllWorkspaceNames( Set<String> alreadyKnownNames ) {
        Set<String> cacheNames = null;
        if (cacheContainer instanceof EmbeddedCacheManager) {
            // EmbeddedCacheManager.getCacheNames() returns an immutable set. Copy the elements so that
            // we can remove any cache names that don't map to workspaces below.
            cacheNames = new HashSet<String>(((EmbeddedCacheManager)cacheContainer).getCacheNames());
        } else {
            cacheNames = alreadyKnownNames;
        }

        if (cacheNames.equals(alreadyKnownNames)) {
            // There are the same caches available as there are known names ...
            return cacheNames;
        }

        // Check each cache that is not already known to see if it is a valid workspace ...
        final UUID rootNodeUuid = getRootNodeUuid();
        Set<String> nonWorkspaceCacheNames = new HashSet<String>();
        for (String cacheName : cacheNames) {
            if (alreadyKnownNames.contains(cacheName)) continue;

            // Otherwise check the cache to see if it has a root node ...
            Cache<UUID, InfinispanNode> cache = cacheContainer.getCache(cacheName);
            if (!cache.containsKey(rootNodeUuid)) {
                nonWorkspaceCacheNames.add(cacheName);
            }
        }

        // Remove all cache names that are not valid workspaces ...
        if (!nonWorkspaceCacheNames.isEmpty()) cacheNames.removeAll(nonWorkspaceCacheNames);

        return cacheNames;
    }

    protected Cache<UUID, InfinispanNode> getCacheOrCreateIfMissing( String cacheName ) {
        if (cacheContainer instanceof EmbeddedCacheManager) {
            EmbeddedCacheManager mgr = (EmbeddedCacheManager)cacheContainer;
            if (mgr.isRunning(cacheName)) return mgr.getCache(cacheName);
            if (mgr.getCacheNames().contains(cacheName)) {
                // The cache exists but is not running ...
                Cache<UUID, InfinispanNode> cache = mgr.getCache(cacheName);
                cache.start();
                return cache;
            }
            // Otherwise the cache does not yet exist ...
            mgr.defineConfiguration(cacheName, mgr.getDefaultConfiguration());
            return mgr.getCache(cacheName);
        }
        return cacheContainer.getCache(cacheName);
    }

    /**
     * Get the cache manager used by this repository.
     * 
     * @return the cacheManager; never null
     */
    protected CacheContainer getCacheContainer() {
        return cacheContainer;
    }

    /**
     * This method shuts down the workspace and makes it no longer usable. This method should also only be called once.
     */
    public void shutdown() {
        this.cacheContainer.stop();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.Repository#startTransaction(org.modeshape.graph.ExecutionContext, boolean)
     */
    @Override
    public InfinispanTransaction startTransaction( ExecutionContext context,
                                                   boolean readonly ) {
        final Lock lock = readonly ? this.lock.readLock() : this.lock.writeLock();
        lock.lock();
        return new InfinispanTransaction(context, this, getRootNodeUuid(), lock);
    }
}
