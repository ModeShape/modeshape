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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.cache.change.ChangeSet;
import org.modeshape.jcr.txn.Transactions;

/**
 * A manager for keeping track of transaction-specific WorkspaceCache instances.
 */
public class TransactionalWorkspaceCaches {
    private static final Logger LOGGER = Logger.getLogger(TransactionalWorkspaceCache.class);
    
    private final TransactionManager txnMgr;
    private final Map<Transaction, Map<String, TransactionalWorkspaceCache>> transactionalCachesByTransaction = new ConcurrentHashMap<>();

    public TransactionalWorkspaceCaches( Transactions transactions ) {
        CheckArg.isNotNull(transactions, "transactions");
        this.txnMgr = transactions.getTransactionManager();
    }
    
    protected WorkspaceCache getTransactionalCache(WorkspaceCache globalWorkspaceCache) throws SystemException  {
        // Get the current transaction ...
        Transaction txn = txnMgr.getTransaction();
        if (txn == null || txn.getStatus() != Status.STATUS_ACTIVE) return globalWorkspaceCache;

        String workspaceName = globalWorkspaceCache.getWorkspaceName();
        return transactionalCachesByTransaction.computeIfAbsent(txn, this::newCacheMapForTransaction)
                                               .computeIfAbsent(workspaceName, wsName -> new TransactionalWorkspaceCache(globalWorkspaceCache, this, txn));
    }

    private Map<String, TransactionalWorkspaceCache> newCacheMapForTransaction(final Transaction txn) {
        try {
            txn.registerSynchronization(new Synchronization() {
                @Override
                public void beforeCompletion() {
                    // do nothing ...
                }

                @Override
                public void afterCompletion(int status) {
                    // No matter what, remove this transactional cache from the maps ...
                    Map<String, TransactionalWorkspaceCache> cachesByWsName = transactionalCachesByTransaction.remove(txn);
                    cachesByWsName.clear();
                }
            });
        } catch (RollbackException | SystemException e) {
            throw new RuntimeException(e);
        }
        return new ConcurrentHashMap<>();
    }

    public synchronized void rollbackActiveTransactionsForWorkspace(String workspaceName) {
        List<Transaction> toRemove = new ArrayList<>();
        // first rollback all active transactions and collect them at the same time...
        transactionalCachesByTransaction.entrySet()
                                        .stream()
                                        .filter(entry -> entry.getValue().containsKey(workspaceName))
                                        .forEach(entry -> {
                                            Transaction tx = entry.getKey();
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
        transactionalCachesByTransaction.keySet().removeAll(toRemove);         
    }

    protected void clearAllCachesForTransaction(final Transaction txn) {
        transactionalCachesByTransaction.getOrDefault(txn, Collections.emptyMap())
                                        .forEach((wsName, txWsCache) -> txWsCache.internalClear());
    }

    protected void dispatchChangesForTransaction(final Transaction txn, final ChangeSet changes) {
        transactionalCachesByTransaction.getOrDefault(txn, Collections.emptyMap())
                                        .forEach((wsName, txWsCache) -> txWsCache.internalChangedWithinTransaction(changes));
    }
}
