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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.cache.SessionEnvironment.Monitor;
import org.modeshape.jcr.cache.SessionEnvironment.MonitorFactory;
import org.modeshape.jcr.cache.change.ChangeSet;
import org.modeshape.jcr.cache.document.WorkspaceCache;

/**
 * An abstraction for the logic of working with transactions. Sessions use this to create local transactions around a single set
 * of changes (persisted via the {@link javax.jcr.Session#save()} invocation), but the implementation actually and transparently
 * coordinates the work based upon whether there is already an existing (container-managed or user-managed) transaction associated
 * with the current thread.
 * <p>
 * The basic workflow is as follows. When transient changes are to be persisted, a new ModeShape transaction is {@link #begin()
 * begun}. The resulting ModeShape {@link Transaction} represents the local transaction, and should be used to obtain the
 * {@link Monitor} that is interested in all changes, to register {@link TransactionFunction functions} that are to be called upon
 * successful transaction commit, and then either {@link Transaction#commit() committed} or {@link Transaction#rollback() rolled
 * back}. If committed, then any changes made should be forwarded to {@link #updateCache(WorkspaceCache, ChangeSet, Transaction)}.
 * </p>
 * <p>
 * In the typical case where no JTA transactions are being used with JCR, then each time changes are made to a Session and
 * {@link javax.jcr.Session#save()} is called a new transaction will be created, the changes applied to the workspace, the
 * transaction will be committed, and the workspace cache will be updated based upon the changes.
 * </p>
 * <p>
 * However, when JTA (local or distributed) transactions are being used with JCR (that is, container-managed transactions or
 * user-managed transactions), then {@link javax.jcr.Session#save()} still needs to be called (see Chapter 21 of the JSR-283
 * specification), but the changes are not persisted until the transaction is completed. In other words, even when one or more
 * Session.save() calls are made, the changes are persisted and made visible to the rest of the repository users only when the
 * transaction successfully commits. In these cases, the current thread is already associated with a transaction when
 * {@link javax.jcr.Session#save()} is called, and therefore ModeShape simply uses that transaction but defers the monitoring and
 * cache update calls until after commit.
 * </p>
 * <p>
 * Note that when distributed (XA) transactions are used, ModeShape properly integrates and uses the XA transaction but does not
 * register itself as an {@link XAResource}. (Note that the Infinispan cache <i>is</i> enlisted as a resource in the transaction.)
 * Therefore, even when XA transactions only involve the JCR repository as the single resource, ModeShape enlists only a single
 * resource, allowing the transaction manager to optimize the 2PC with a single resource as a 1PC transaction. (Rather than
 * enlisting the repository as an XAResource, ModeShape registers a {@link Synchronization} with the transaction to be notified
 * when the transaction commits successfully, and it uses this to dictate when the events and session's changes are made visible
 * to other sessions.
 * </p>
 */
public abstract class Transactions {

    protected final TransactionManager txnMgr;
    protected final MonitorFactory monitorFactory;
    protected final Logger logger = Logger.getLogger(Transactions.class);

    protected Transactions( MonitorFactory monitorFactory,
                            TransactionManager txnMgr ) {
        this.monitorFactory = monitorFactory;
        this.txnMgr = txnMgr;
    }

    /**
     * Determine if the current thread is already associated with an existing transaction.
     * 
     * @return true if there is an existing transaction, or false if there is none
     * @throws SystemException If the transaction service fails in an unexpected way.
     */
    public boolean isCurrentlyInTransaction() throws SystemException {
        return txnMgr.getTransaction() != null;
    }

    /**
     * Starts a new transaction if one does not already exist, and associate it with the calling thread.
     * 
     * @return the ModeShape transaction
     * @throws NotSupportedException If the calling thread is already associated with a transaction, and nested transactions are
     *         not supported.
     * @throws SystemException If the transaction service fails in an unexpected way.
     */
    public abstract Transaction begin() throws NotSupportedException, SystemException;

    /**
     * Notify the workspace of the supplied changes, if and when the current transaction is completed. If the current thread is
     * not associated with a transaction when this method is called (e.g., the transaction was started, changes were made, the
     * transaction was committed, and then this method was called), then the workspace is notified immediately. Otherwise, the
     * notifications will be accumulated until the current transaction is committed.
     * 
     * @param workspace the workspace to which the changes were made; may not be null
     * @param changes the changes; may be null if there are no changes
     * @param transaction the transaction with which the changes were made; may not be null
     */
    public void updateCache( WorkspaceCache workspace,
                             ChangeSet changes,
                             Transaction transaction ) {
        // Notify the workspaces of the changes made. This is done outside of our lock but still before the save returns ...
        if (changes != null && !changes.isEmpty()) {
            // Notify the workspace (outside of the lock, but still before the save returns) ...
            workspace.changed(changes);
        }
    }

    /**
     * Suspends the existing transaction, if there is one.
     * 
     * @return either the {@link javax.transaction.Transaction} which was suspended or {@code null} if there isn't such a
     *         transaction.
     * @throws SystemException if the operation fails.
     * @see javax.transaction.TransactionManager#suspend()
     */
    public javax.transaction.Transaction suspend() throws SystemException {
        return txnMgr.suspend();
    }

    /**
     * Resumes a transaction that was previously suspended via the {@link org.modeshape.jcr.txn.Transactions#suspend()} call. If
     * there is no such transaction or there is another active transaction, nothing happens.
     * 
     * @param transaction a {@link javax.transaction.Transaction} instance which was suspended previously or {@code null}
     * @throws javax.transaction.SystemException if the operation fails.
     * @see javax.transaction.TransactionManager#resume(javax.transaction.Transaction)
     */
    public void resume( javax.transaction.Transaction transaction ) throws SystemException {
        if (transaction != null && txnMgr.getTransaction() == null) {
            try {
                txnMgr.resume(transaction);
            } catch (InvalidTransactionException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * The representation of a ModeShape fine-grained transaction for use when saving the changes made to a Session. Note that
     * this transaction may wrap a newly-created transaction for the current thread, or it may wrap an existing and on-going
     * transaction for the current thread. In either case, the caller still {@link #commit() commits} or {@link #rollback()
     * rollsback} the transaction as normal when it's work is done.
     */
    public static interface Transaction extends MonitorFactory {

        /**
         * Get a monitor that should be used to capture what has changed during this transaction.
         * 
         * @return the monitor, or null if there is no monitoring
         */
        @Override
        Monitor createMonitor();

        /**
         * Register a function that will be called when the current transaction completes, or immediately if there is not
         * currently an active transaction.
         * 
         * @param function the completion function
         */
        void uponCompletion( TransactionFunction function );

        /**
         * Commit the transaction currently associated with the calling thread.
         * 
         * @throws RollbackException If the transaction was marked for rollback only, the transaction is rolled back and this
         *         exception is thrown.
         * @throws IllegalStateException If the calling thread is not associated with a transaction.
         * @throws SystemException If the transaction service fails in an unexpected way.
         * @throws HeuristicMixedException If a heuristic decision was made and some some parts of the transaction have been
         *         committed while other parts have been rolled back.
         * @throws HeuristicRollbackException If a heuristic decision to roll back the transaction was made.
         * @throws SecurityException If the caller is not allowed to commit this transaction.
         */
        void commit()
            throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException,
            IllegalStateException, SystemException;

        /**
         * Rolls back the transaction currently associated with the calling thread.
         * 
         * @throws IllegalStateException If the transaction is in a state where it cannot be rolled back. This could be because
         *         the calling thread is not associated with a transaction, or because it is in the {@link Status#STATUS_PREPARED
         *         prepared state}.
         * @throws SecurityException If the caller is not allowed to roll back this transaction.
         * @throws SystemException If the transaction service fails in an unexpected way.
         */
        void rollback() throws IllegalStateException, SecurityException, SystemException;
    }

    public static interface TransactionFunction {
        void transactionComplete();
    }

    protected Monitor newMonitor() {
        return monitorFactory.createMonitor();
    }

    protected abstract class BaseTransaction implements Transaction {
        protected final TransactionManager txnMgr;
        private List<TransactionFunction> functions;

        protected BaseTransaction( TransactionManager txnMgr ) {
            this.txnMgr = txnMgr;
        }

        @Override
        public Monitor createMonitor() {
            return newMonitor();
        }

        protected void executeFunctions() {
            // Execute the functions immediately ...
            if (functions != null) {
                for (TransactionFunction function : functions) {
                    function.transactionComplete();
                }
            }
        }

        @Override
        public void uponCompletion( TransactionFunction function ) {
            if (functions == null) {
                functions = Collections.singletonList(function);
            } else {
                if (functions.size() == 1) {
                    functions = new LinkedList<TransactionFunction>(functions);
                }
                functions.add(function);
            }
        }
    }

    protected class SimpleTransaction extends BaseTransaction {

        protected SimpleTransaction( TransactionManager txnMgr ) {
            super(txnMgr);
        }

        @Override
        public void rollback() throws IllegalStateException, SecurityException, SystemException {
            logger.trace("Rolling back transaction");
            txnMgr.rollback();
        }

        @Override
        public void commit()
            throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException,
            IllegalStateException, SystemException {
            txnMgr.commit();
            logger.trace("Committed transaction");

            // Execute the functions immediately ...
            executeFunctions();
        }
    }

}
