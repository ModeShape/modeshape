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
package org.modeshape.jcr.cache.document;

import java.util.concurrent.ConcurrentHashMap;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.RepositoryCache;
import org.modeshape.jcr.cache.change.ChangeSet;

/**
 * A special WorkspaceCache implementation that should be used by sessions running within user transactions.
 * <p>
 * Normally, each {@link RepositoryCache} instance has for each workspace a single WorkspaceCache instance shared by all sessions.
 * However, in the event that a session is running within a user transaction, it is possible for the session (or other sessions
 * that are also participating in the same user transaction) to persist node changes in the document store but to not commit the
 * transaction. This means that no other transactions should see these persisted-but-not-committed node representations. Thus,
 * these sessions running within a user transaction cannot share the common WorkspaceCache instance, lest any node representations
 * persisted-but-not-committed be loaded into the WorkspaceCache (leaking transaction-scoped data outside of the transaction) or
 * the shared WorkspaceCache instance has already-cached pre-modified representations of the persisted-but-not-committed nodes
 * (transaction-scoped changes are not visible to the transaction).
 * </p>
 * <p>
 * Therefore, such sessions running within user transactions need a transactionally-scoped WorkspaceCache instance. Because the
 * ModeShape infrastructure is not set up to handle lots of WorkspaceCache instances, we only want one instance that is actually
 * caching nodes. Therefore, the WorkspaceCache returned from this method will never cache any nodes and will always re-read the
 * nodes from the document store.
 * </p>
 */
class TransactionalWorkspaceCache extends WorkspaceCache {

    private final WorkspaceCache sharedWorkspaceCache;

    protected TransactionalWorkspaceCache( WorkspaceCache sharedWorkspaceCache ) {
        // Use a new in-memory map for the transactional cache ...
        super(sharedWorkspaceCache, new ConcurrentHashMap<NodeKey, CachedNode>(), null);
        this.sharedWorkspaceCache = sharedWorkspaceCache;
    }

    @Override
    public void changed( ChangeSet changes ) {
        // Delegate to the shared ...
        sharedWorkspaceCache.changed(changes);
        // And then handle it ourselves ...
        super.changed(changes);
    }

    @Override
    public void notify( ChangeSet changeSet ) {
        // Delegate to the shared ...
        sharedWorkspaceCache.notify(changeSet);
        // And then handle it ourselves ...
        super.notify(changeSet);
    }
}
