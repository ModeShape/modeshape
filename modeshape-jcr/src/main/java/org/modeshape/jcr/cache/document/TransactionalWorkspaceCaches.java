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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.txn.Transactions;

/**
 * A manager for keeping track of transaction-specific WorkspaceCache instances.
 */
public class TransactionalWorkspaceCaches {

    private TransactionManager txnMgr;
    private Map<Transaction, Map<String, TransactionalWorkspaceCache>> transactionalCachesByTransaction = new HashMap<Transaction, Map<String, TransactionalWorkspaceCache>>();

    public TransactionalWorkspaceCaches( Transactions transactions ) {
        this.txnMgr = transactions != null ? transactions.getTransactionManager() : null;
    }

    public WorkspaceCache getTransactionalWorkspaceCache( WorkspaceCache sharedWorkspaceCache )
        throws SystemException, RollbackException {
        if (txnMgr == null) return sharedWorkspaceCache;

        // Get the current transaction ...
        Transaction txn = txnMgr.getTransaction();
        if (txn == null || txn.getStatus() != Status.STATUS_ACTIVE) return sharedWorkspaceCache;

        synchronized (this) {
            String workspaceName = sharedWorkspaceCache.getWorkspaceName();
            Map<String, TransactionalWorkspaceCache> workspaceCachesForTransaction = transactionalCachesByTransaction.get(txn);
            if (workspaceCachesForTransaction == null) {
                // No transactional caches for this transaction yet ...
                workspaceCachesForTransaction = new HashMap<String, TransactionalWorkspaceCache>();
                transactionalCachesByTransaction.put(txn, workspaceCachesForTransaction);
                TransactionalWorkspaceCache newCache = createCache(sharedWorkspaceCache, txn);
                workspaceCachesForTransaction.put(workspaceName, newCache);
                return newCache;
            }

            TransactionalWorkspaceCache cache = workspaceCachesForTransaction.get(workspaceName);
            if (cache != null) {
                return cache;
            }

            // No transactional cache for this workspace ...
            cache = createCache(sharedWorkspaceCache, txn);
            workspaceCachesForTransaction.put(workspaceName, cache);
            return cache;
        }
    }

    public synchronized void remove( String workspaceName ) {
        if (txnMgr == null) return;
        Set<Transaction> transactions = new HashSet<Transaction>();
        synchronized (this) {
            for (Map.Entry<Transaction, Map<String, TransactionalWorkspaceCache>> entry : transactionalCachesByTransaction.entrySet()) {
                if (entry.getValue().containsKey(workspaceName)) {
                    transactions.add(entry.getKey());
                }
            }
        }
        for (Transaction transaction : transactions) {
            try {
                // rollback the transaction ...
                transaction.rollback();
            } catch (SystemException e) {
                Logger.getLogger(getClass())
                      .error(JcrI18n.errorWhileRollingBackActiveTransactionUsingWorkspaceThatIsBeingDeleted,
                             workspaceName,
                             e.getMessage());
            }
        }
    }

    protected synchronized void remove( Transaction txn ) {
        transactionalCachesByTransaction.remove(txn);
    }

    /**
     * Invoke the supplied operation on each of the transactional workspace caches associated with the supplied transaction.
     * 
     * @param txn the transaction; may not be null
     * @param operation the operation to call on each {@link TransactionalWorkspaceCache} in the given transaction; may not be
     *        null
     */
    synchronized void onAllWorkspacesInTransaction( final Transaction txn,
                                                    final OnEachTransactionalCache operation ) {
        assert operation != null;
        assert txn != null;
        Map<String, TransactionalWorkspaceCache> cachesForTxn = transactionalCachesByTransaction.get(txn);
        if (cachesForTxn != null) {
            for (TransactionalWorkspaceCache cache : cachesForTxn.values()) {
                if (cache != null) operation.execute(cache);
            }
        }
    }

    /**
     * See #onAllWorkspacesInTransaction
     */
    static interface OnEachTransactionalCache {
        /**
         * Invoke the operation on the supplied cache
         * 
         * @param cache the transactional workspace cache; never null
         */
        void execute( TransactionalWorkspaceCache cache );
    }

    protected TransactionalWorkspaceCache createCache( WorkspaceCache sharedWorkspaceCache,
                                                       final Transaction txn ) throws SystemException, RollbackException {
        final TransactionalWorkspaceCache cache = new TransactionalWorkspaceCache(sharedWorkspaceCache, this, txn);
        txn.registerSynchronization(new Synchronization() {

            @Override
            public void beforeCompletion() {
                // do nothing ...
            }

            @Override
            public void afterCompletion( int status ) {
                // No matter what, remove this transactional cache from the maps ...
                remove(txn);
            }
        });
        return cache;
    }
}
