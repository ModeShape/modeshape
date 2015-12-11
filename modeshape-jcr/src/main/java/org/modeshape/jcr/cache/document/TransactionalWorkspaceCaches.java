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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.txn.Transactions;

/**
 * A manager for keeping track of transaction-specific WorkspaceCache instances.
 */
public class TransactionalWorkspaceCaches {
    private static final Logger LOGGER = Logger.getLogger(TransactionalWorkspaceCache.class);
    
    private final TransactionManager txnMgr;
    private final Map<Transaction, Map<String, TransactionalWorkspaceCache>> transactionalCachesByTransaction = new HashMap<>();

    public TransactionalWorkspaceCaches( Transactions transactions ) {
        CheckArg.isNotNull(transactions, "transactions");
        this.txnMgr = transactions.getTransactionManager();
    }
    
    protected WorkspaceCache getTransactionalCache(WorkspaceCache globalWorkspaceCache) throws Exception {
        // Get the current transaction ...
        Transaction txn = txnMgr.getTransaction();
        if (txn == null || txn.getStatus() != Status.STATUS_ACTIVE) return globalWorkspaceCache;


        synchronized (this) {
            String workspaceName = globalWorkspaceCache.getWorkspaceName();
            return transactionalCachesByTransaction.computeIfAbsent(txn, tx -> new HashMap<>())
                                                   .computeIfAbsent(workspaceName,
                                                                    wsName -> createCache(globalWorkspaceCache, txn));
        }
    }
    
    public synchronized void rollbackActiveTransactionsForWorkspace(String workspaceName) {
        List<Transaction> toRemove = new ArrayList<>();
        // first rollback all active transactions and collect them at the same time...
        transactionalCachesByTransaction.entrySet().stream()
                                        .filter(entry -> entry.getValue().containsKey(workspaceName))
                                        .map(Map.Entry::getKey)
                                        .forEach(tx -> {
                                            toRemove.add(tx);
                                            try {
                                                tx.rollback();
                                            } catch (SystemException e) {
                                                LOGGER.error(e,
                                                             JcrI18n.errorWhileRollingBackActiveTransactionUsingWorkspaceThatIsBeingDeleted,
                                                             workspaceName, e.getMessage());
                                            }
                                        });
        // then remove them from the map
        toRemove.stream().forEach(transactionalCachesByTransaction::remove);         
    }

    protected synchronized void remove( Transaction txn ) {
        transactionalCachesByTransaction.remove(txn);
    }
    
    protected synchronized Stream<TransactionalWorkspaceCache> workspaceCachesFor(final Transaction txn) {
        return transactionalCachesByTransaction.getOrDefault(txn, Collections.emptyMap()).values().stream();
    }

    private TransactionalWorkspaceCache createCache(WorkspaceCache sharedWorkspaceCache,
                                                    final Transaction txn) {
        final TransactionalWorkspaceCache cache = new TransactionalWorkspaceCache(sharedWorkspaceCache, this, txn);
        try {
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
        } catch (RollbackException | SystemException e) {
            throw new RuntimeException(e);
        } 
        return cache;
    }
}
