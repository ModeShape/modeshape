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
package org.modeshape.jcr.txn;

import java.util.HashSet;
import java.util.Set;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import org.infinispan.Cache;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.TransactionCompleted;
import org.infinispan.notifications.cachelistener.event.TransactionCompletedEvent;
import org.infinispan.transaction.TransactionTable;
import org.infinispan.transaction.xa.GlobalTransaction;
import org.modeshape.jcr.cache.change.ChangeSet;
import org.modeshape.jcr.cache.document.TransactionalWorkspaceCache;
import org.modeshape.jcr.cache.document.WorkspaceCache;

/**
 * An implementation of {@link Transactions} that will attempt to register a {@link Synchronization} with the current transaction.
 */
public final class SynchronizedTransactions extends Transactions {

    private static final ThreadLocal<Transaction> ACTIVE_TRANSACTION = new ThreadLocal<Transaction>();

    @SuppressWarnings( "rawtypes")
    private final Cache localCache;
    private final TransactionTable transactionTable;

    /**
     * Creates a new instance which wraps a transaction manager and monitor factory
     *
     * @param txnMgr a {@link javax.transaction.TransactionManager} instance; never null
     * @param localCache a {@link org.infinispan.Cache} instance representing the main ISPN cache
     */
    @SuppressWarnings( "rawtypes")
    public SynchronizedTransactions( TransactionManager txnMgr, Cache localCache ) {
        super(txnMgr);
        assert this.txnMgr != null;

        this.localCache = localCache;
        assert this.localCache != null;

        this.transactionTable = localCache.getAdvancedCache().getComponentRegistry().getComponent(TransactionTable.class);
        assert this.transactionTable != null;
    }

    @Override
    public Transaction currentTransaction() {
        return ACTIVE_TRANSACTION.get();
    }

    @Override
    public Transaction begin() throws NotSupportedException, SystemException {
        // check if there isn't an active transaction already
        Transaction result = ACTIVE_TRANSACTION.get();
        if (result != null) {
            // we have an existing transaction so depending on the type we either need to be aware of nesting
            // or return as-is
            if (logger.isTraceEnabled()) {
                logger.trace("Found active ModeShape transaction '{0}' ", result);
            }
            return result instanceof NestableThreadLocalTransaction ?
                   ((NestableThreadLocalTransaction) result).begin() :
                   result;
        }

        // Get the transaction currently associated with this thread (if there is one) ...
        javax.transaction.Transaction txn = txnMgr.getTransaction();
        if (txn == null) {
            // There is no transaction, so start one ...
            txnMgr.begin();
            // and return our wrapper ...
            result = new NestableThreadLocalTransaction(txnMgr, ACTIVE_TRANSACTION).begin();
        } else {
            // Otherwise, there's already a transaction, so wrap it with a cache listener
            result = new ListenerTransaction(txnMgr, txn);
            localCache.addListener(result);
        }
        // Store it
        ACTIVE_TRANSACTION.set(result);
        if (logger.isTraceEnabled()) {
            logger.trace("Created & stored new ModeShape synchronized transaction '{0}' ", result);
            if (txn == null) txn = txnMgr.getTransaction();
            assert txn != null;
            final String id = txn.toString();
            // Register a synchronization for this transaction ...
            if (!ACTIVE_TRACE_SYNCHRONIZATIONS.contains(id)) {
                if (result instanceof ListenerTransaction) {
                    logger.trace("Found user transaction {0}", txn);
                } else {
                    logger.trace("Begin transaction {0}", id);
                }
                // Only if we don't already have one ...
                try {
                    txn.registerSynchronization(new TransactionTracer(id));
                } catch (RollbackException e) {
                    // This transaction has been marked for rollback only ...
                    return new RollbackOnlyTransaction();
                }
            } else {
                logger.trace("Tracer already registered for transaction {0}", id);
            }
        }
        return result;
    }

    @Override
    public void updateCache( final WorkspaceCache workspace,
                             final ChangeSet changes,
                             Transaction transaction ) {
        if (changes != null && !changes.isEmpty()) {
            if (transaction instanceof ListenerTransaction) {
                // only issue the changes when the transaction is successfully committed
                transaction.uponCommit(new TransactionFunction() {
                    @Override
                    public void execute() {
                        workspace.changed(changes);
                    }
                });
                if (workspace instanceof TransactionalWorkspaceCache) {
                    ((TransactionalWorkspaceCache)workspace).changedWithinTransaction(changes);
                }
            } else if (transaction instanceof RollbackOnlyTransaction) {
                // The transaction has been marked for rollback only, so no need to even capture these changes because
                // no changes will ever escape the Session ...
            } else {
                // in all other cases we want to dispatch the changes immediately
                workspace.changed(changes);
            }
        }
    }

    /**
     * A transaction implementation that is an Infinispan listener. This is the only reliable way to be able to tell, when
     * using user transactions, that ISPN has finished updating the data after a "commit" call.
     */
    @Listener
    @SuppressWarnings( "rawtypes")
    protected final class ListenerTransaction extends BaseTransaction {
        private final GlobalTransaction ispnTxID;
        protected ListenerTransaction( TransactionManager txnMgr, javax.transaction.Transaction activeTransaction ) {
            super(txnMgr);
            try {
                assert activeTransaction != null;
                // store a reference to the active ISPN transaction because this listener should only process events for this transaction
                this.ispnTxID = transactionTable.getLocalTransaction(activeTransaction).getGlobalTransaction();
                assert ispnTxID != null;
                if (logger.isTraceEnabled()) {
                    logger.trace(
                            "Registered Infinispan tx listener '{0}' which will fire events after Infinispan has finished processing the '{1}' transaction",
                            this,
                            ispnTxID.toString());
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Method which will be invoked by Infinispan once a tx.commit or tx.rollback has finished processing at a cache-level.
         * @param event a {@link TransactionCompletedEvent} instance.
         */
        @TransactionCompleted
        public void transactionCompleted( TransactionCompletedEvent event ) {
            if (!event.isOriginLocal()) {
                // if the event is not local, we're not interested in processing it
                if (logger.isTraceEnabled()) {
                    logger.trace("Infinispan tx listener '{0}' ignoring event '{1}' because it did not originate on this cluster node",
                                 this, event);
                }
                return;
            }

            // only do the processing if the ISPN transaction for which this was created matches the transaction in the event
            GlobalTransaction eventIspnTransaction = event.getGlobalTransaction();
            if (eventIspnTransaction == null || ispnTxID.getId() != eventIspnTransaction.getId()) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Infinispan tx listener '{0}' ignoring event '{1}' because it was received for another transaction '{2}'. Our transaction is '{3}'",
                                 this, event, eventIspnTransaction, this.ispnTxID);
                }
                return;
            }

            try {
                if (logger.isTraceEnabled()) {
                    logger.trace("Infinispan tx listener '{0}' received event '{1}'", this, event);
                }
                if (event.isTransactionSuccessful()) {
                    // run the functions for a successful commit
                    executeFunctionsUponCommit();
                    if (logger.isTraceEnabled()) {
                        logger.trace("Infinispan tx listener '{0}' executed commit functions for transaction '{1}'", this,
                                     ispnTxID);
                    }
                }
                // run all the other (both commit & rollback) functions
                executeFunctionsUponCompletion();
                if (logger.isTraceEnabled()) {
                    logger.trace("Infinispan tx listener '{0}' executed completion functions for transaction '{1}'", this,
                                 ispnTxID);
                }
            } finally {
                // after we've been invoked, it means that ISPN has finished processing the transaction, we need to always
                // remove ourselves from the cache
                localCache.removeListener(this);
                // clear the active ModeShape transaction for this thread
                ACTIVE_TRANSACTION.remove();
                if (logger.isTraceEnabled()) {
                    logger.trace("Infinispan tx listener '{0}' has finished and has been unregistered for transaction '{1}'", this,
                                 ispnTxID);
                }
            }
        }

        @Override
        public void commit()  {
            if (logger.isTraceEnabled()) {
                logger.trace("Infinispan tx listener '{0}' ignoring commit call coming from ModeShape. Waiting to be notified by Infinispan'", this);
            }
            //nothing by default
        }

        @Override
        public void rollback() {
            if (logger.isTraceEnabled()) {
                logger.trace("Infinispan tx listener '{0}' ignoring rollback call coming from ModeShape. Waiting to be notified by Infinispan'", this);
            }
            // nothing by default
        }
    }

    protected class RollbackOnlyTransaction implements Transaction {

        public RollbackOnlyTransaction() {
        }

        @Override
        public int status() throws SystemException {
            return Status.STATUS_UNKNOWN;
        }

        @Override
        public void commit() {
            // do nothing
        }

        @Override
        public void rollback() {
            // do nothing
        }

        @Override
        public void uponCompletion( TransactionFunction function ) {
            // do nothing
        }

        @Override
        public void uponCommit( TransactionFunction function ) {
            // do nothing
        }
    }

    protected static final Set<String> ACTIVE_TRACE_SYNCHRONIZATIONS = new HashSet<String>();

    protected final class TransactionTracer implements Synchronization {
        private String txnId;

        protected TransactionTracer( String id ) {
            txnId = id;
            ACTIVE_TRACE_SYNCHRONIZATIONS.add(id);
        }

        @Override
        public void beforeCompletion() {
            // do nothing else ...
        }

        @Override
        public void afterCompletion( int status ) {
            ACTIVE_TRACE_SYNCHRONIZATIONS.remove(txnId);
            switch (status) {
                case Status.STATUS_COMMITTED:
                    logger.trace("Commit transaction '{0}'", txnId);
                    break;
                case Status.STATUS_ROLLEDBACK:
                    logger.trace("Roll back transaction '{0}'", txnId);
                    break;
                default:
                    // Don't do anything ...
                    break;
            }
        }
    }
}
