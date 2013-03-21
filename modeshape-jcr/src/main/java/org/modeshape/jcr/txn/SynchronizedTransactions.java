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
package org.modeshape.jcr.txn;

import java.util.LinkedList;
import java.util.List;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import org.modeshape.jcr.cache.SessionEnvironment.Monitor;
import org.modeshape.jcr.cache.SessionEnvironment.MonitorFactory;
import org.modeshape.jcr.cache.change.ChangeSet;
import org.modeshape.jcr.cache.document.TransactionalWorkspaceCache;
import org.modeshape.jcr.cache.document.WorkspaceCache;

/**
 * An implementation of {@link Transactions} that will attempt to register a {@link Synchronization} with the current transaction.
 */
public final class SynchronizedTransactions extends Transactions {

    public SynchronizedTransactions( MonitorFactory monitorFactory,
                                     TransactionManager txnMgr ) {
        super(monitorFactory, txnMgr);
        assert txnMgr != null;
    }

    @Override
    public Transaction begin() throws NotSupportedException, SystemException {
        // Get the transaction currently associated with this thread (if there is one) ...
        javax.transaction.Transaction txn = txnMgr.getTransaction();
        if (txn == null) {
            // There is no transaction, so start one ...
            logger.trace("Begin transaction");
            txnMgr.begin();
            // and return immediately ...
            return new SimpleTransaction(txnMgr);
        }

        // Otherwise, there's already a transaction, so wrap it ...
        try {
            return new SynchronizedTransaction(txnMgr);
        } catch (RollbackException e) {
            // This transaction has been marked for rollback only ...
            return new RollbackOnlyTransaction();
        }
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
                            logger.trace("Committed transaction");
                            afterCommit();
                            break;
                        case Status.STATUS_ROLLEDBACK:
                            logger.trace("Rolled back transaction");
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
        public Monitor createMonitor() {
            return newMonitor();
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
}
