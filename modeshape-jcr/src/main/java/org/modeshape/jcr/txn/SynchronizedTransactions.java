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
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import org.modeshape.jcr.cache.change.ChangeSet;
import org.modeshape.jcr.cache.document.TransactionalWorkspaceCache;
import org.modeshape.jcr.cache.document.WorkspaceCache;

/**
 * An implementation of {@link Transactions} that will attempt to register a {@link Synchronization} with the current transaction.
 */
public final class SynchronizedTransactions extends Transactions {

    /**
     * Creates a new instance which wrapps a transaction manager and monitor factory
     * 
     * @param txnMgr a {@link TransactionManager} instance; never null
     */
    public SynchronizedTransactions( TransactionManager txnMgr ) {
        super(txnMgr);
        assert txnMgr != null;
    }

    @Override
    public Transaction begin() throws NotSupportedException, SystemException {
        // Get the transaction currently associated with this thread (if there is one) ...
        javax.transaction.Transaction txn = txnMgr.getTransaction();
        Transaction result = null;
        if (txn == null) {
            // There is no transaction, so start one ...
            txnMgr.begin();
            // and return our wrapper ...
            result = new SimpleTransaction(txnMgr);
        } else {
            // Otherwise, there's already a transaction, so wrap it ...
            try {
                result = new SynchronizedTransaction(txnMgr); // may throw RollbackException ...
            } catch (RollbackException e) {
                // This transaction has been marked for rollback only ...
                return new RollbackOnlyTransaction();
            }
        }
        if (logger.isTraceEnabled()) {
            if (txn == null) txn = txnMgr.getTransaction();
            assert txn != null;
            final String id = txn.toString();
            // Register a synchronization for this transaction ...
            if (!ACTIVE_TRACE_SYNCHRONIZATIONS.contains(id)) {
                if (result instanceof SynchronizedTransaction) {
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
    public void updateCache( WorkspaceCache workspace,
                             ChangeSet changes,
                             Transaction transaction ) {
        if (changes != null && !changes.isEmpty()) {
            if (transaction instanceof SynchronizedTransaction) {
                // We're in a transaction being managed outside of ModeShape (e.g., container-managed, user-managed,
                // distributed, etc.) ...
                // Capture the changes so they can be applied if and only if the transaction is committed succesfully ...
                SynchronizedTransaction synched = (SynchronizedTransaction)transaction;
                synched.addUpdate(new WorkspaceUpdates(workspace, changes));
                // Also, if we're in a transaction then the workspace should be a TransactionalWorkspaceCache, in which case
                // we should also immediately notify the workspace of the changes ...
                if (workspace instanceof TransactionalWorkspaceCache) {
                    ((TransactionalWorkspaceCache)workspace).changedWithinTransaction(changes);
                }
            } else if (transaction instanceof RollbackOnlyTransaction) {
                // The transaction has been marked for rollback only, so no need to even capture these changes because
                // no changes will ever escape the Session ...
            } else {
                // We're not in a transaction anymore (the changes were succesfully committed already),
                // so immediately fire the changes ...
                workspace.changed(changes);
            }
        }
    }

    protected class SynchronizedTransaction extends BaseTransaction {

        private final Synchronization synchronization;
        private final List<WorkspaceUpdates> updates = new LinkedList<WorkspaceUpdates>();
        private boolean finished = false;

        protected SynchronizedTransaction( TransactionManager txnMgr ) throws SystemException, RollbackException {
            super(txnMgr);
            this.synchronization = new Synchronization() {

                @Override
                public void beforeCompletion() {
                    // do nothing ...
                }

                @Override
                public void afterCompletion( int status ) {
                    switch (status) {
                        case Status.STATUS_COMMITTED:
                            afterCommit();
                            break;
                        case Status.STATUS_ROLLEDBACK:
                            break;
                        default:
                            // Don't do anything ...
                            break;
                    }
                }
            };
            txnMgr.getTransaction().registerSynchronization(synchronization);
        }

        protected void addUpdate( WorkspaceUpdates updates ) {
            assert updates != null;
            assert !finished;
            this.updates.add(updates);
        }

        @Override
        public void commit() {
            // This transaction spans more than just our usage, so we don't commit anything here ...
        }

        @Override
        public void rollback() {
            // This transaction spans more than just our usage, so we don't rollback anything here ...
        }

        /**
         * Method called after the transaction has successfully completed via commit (not rollback). Override this method in
         * subclasses to alter the behavior.
         */
        protected void afterCommit() {
            // Execute the functions
            executeFunctions();

            // Apply the updates, and do AFTER the monitor is updated ...
            for (WorkspaceUpdates update : updates) {
                update.apply();
            }
            finished = true;
        }
    }

    protected class RollbackOnlyTransaction implements Transaction {

        public RollbackOnlyTransaction() {
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
    }

    protected static final class WorkspaceUpdates {
        private final WorkspaceCache workspace;
        private final ChangeSet changes;

        protected WorkspaceUpdates( WorkspaceCache workspace,
                                    ChangeSet changes ) {
            this.workspace = workspace;
            this.changes = changes;
        }

        protected void apply() {
            workspace.changed(changes);
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
