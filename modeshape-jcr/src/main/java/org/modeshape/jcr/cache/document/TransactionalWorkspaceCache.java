/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.jcr.cache.document;

import java.util.concurrent.ConcurrentHashMap;
import javax.transaction.Transaction;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.RepositoryCache;
import org.modeshape.jcr.cache.change.ChangeSet;
import org.modeshape.jcr.cache.document.TransactionalWorkspaceCaches.OnEachTransactionalCache;
import org.modeshape.jcr.txn.SynchronizedTransactions;

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
public class TransactionalWorkspaceCache extends WorkspaceCache {

    private final WorkspaceCache sharedWorkspaceCache;
    private final TransactionalWorkspaceCaches cacheManager;
    private final Transaction txn;

    protected TransactionalWorkspaceCache( WorkspaceCache sharedWorkspaceCache,
                                           TransactionalWorkspaceCaches cacheManager,
                                           Transaction txn ) {
        // Use a new in-memory map for the transactional cache ...
        super(sharedWorkspaceCache, new ConcurrentHashMap<NodeKey, CachedNode>());
        this.sharedWorkspaceCache = sharedWorkspaceCache;
        this.txn = txn;
        this.cacheManager = cacheManager;
    }

    @Override
    public void changed( ChangeSet changes ) {
        // Delegate to the shared ...
        sharedWorkspaceCache.changed(changes);
        // And then delegate so that all transactional workspace caches are notified ...
        changedWithinTransaction(changes);
    }

    /**
     * Signal that this transaction-specific workspace cache needs to reflect recent changes that have been persisted but not yet
     * committed. Generally, the transactional workspace cache will clear any cached nodes that were included in the change set
     * 
     * @param changes the changes that were persisted but not yet committed
     * @see SynchronizedTransactions#updateCache(WorkspaceCache, ChangeSet, org.modeshape.jcr.txn.Transactions.Transaction)
     */
    public void changedWithinTransaction( final ChangeSet changes ) {
        cacheManager.onAllWorkspacesInTransaction(txn, new OnEachTransactionalCache() {
            @Override
            public void execute( TransactionalWorkspaceCache cache ) {
                cache.internalChangedWithinTransaction(changes);
            }
        });
    }

    @Override
    public void clear() {
        cacheManager.onAllWorkspacesInTransaction(txn, new OnEachTransactionalCache() {
            @Override
            public void execute( TransactionalWorkspaceCache cache ) {
                cache.internalClear();
            }
        });
    }

    void internalClear() {
        super.clear();
    }

    void internalChangedWithinTransaction( ChangeSet changes ) {
        // Handle it ourselves ...
        super.changed(changes);
    }

    @Override
    protected void evictChangedNodes( ChangeSet changeSet ) {
        // Delegate to the shared ...
        sharedWorkspaceCache.evictChangedNodes(changeSet);
        // And then handle it ourselves ...
        super.evictChangedNodes(changeSet);
    }
}
