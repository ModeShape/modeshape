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
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
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
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.cache.change.ChangeSet;
import org.modeshape.jcr.cache.document.TransactionalWorkspaceCache;
import org.modeshape.jcr.cache.document.WorkspaceCache;
import org.modeshape.schematic.TransactionListener;

/**
 * An abstraction for the logic of working with transactions. Sessions use this to create local transactions around a single set
 * of changes (persisted via the {@link javax.jcr.Session#save()} invocation), but the implementation actually and transparently
 * coordinates the work based upon whether there is already an existing (container-managed or user-managed) transaction associated
 * with the current thread.
 * <p>
 * The basic workflow is as follows. When transient changes are to be persisted, a new ModeShape transaction is {@link #begin()
 * begun}. The resulting ModeShape {@link Transaction} represents the local transaction, to register {@link TransactionFunction
 * functions} that are to be called upon successful transaction commit, and then either {@link Transaction#commit() committed} or
 * {@link Transaction#rollback() rolled back}. If committed, then any changes made should be forwarded to
 * {@link #updateCache(WorkspaceCache, ChangeSet, Transaction)}.
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
 * register itself as an {@link XAResource}. Therefore, even when XA transactions only involve the JCR repository as 
 * the single resource, ModeShape enlists only a single resource, allowing the transaction manager to optimize the 2PC with a 
 * single resource as a 1PC transaction. (Rather than enlisting the repository as an XAResource, ModeShape registers 
 * a {@link Synchronization} with the transaction to be notified when the transaction commits successfully, and it uses this to 
 * dictate when the events and session's changes are made visible to other sessions.
 * </p>
 */
public class Transactions {

    private static final ThreadLocal<NestableThreadLocalTransaction> LOCAL_TRANSACTION = new ThreadLocal<>();
    private static final Set<String> ACTIVE_TRACE_SYNCHRONIZATIONS = new HashSet<>();

    protected final TransactionManager txnMgr;
    protected final Logger logger = Logger.getLogger(Transactions.class);
    
    private final TransactionListener listener;
    private final ConcurrentMap<javax.transaction.Transaction, SynchronizedTransaction> transactionTable;

    /**
     * Creates a new instance wrapping an existing transaction manager and a transaction listener.
     * 
     * @param txnMgr a {@link TransactionManager} instance; may not be null
     * @param listener a {@link TransactionListener} instance; may not be null
     */
    public Transactions(TransactionManager txnMgr, TransactionListener listener) {
        this.txnMgr = txnMgr;
        this.listener = listener;
        this.transactionTable = new ConcurrentHashMap<>();
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
     * Get a string representation of the current transaction if there already is an existing transaction.
     *
     * @return a string representation of the transaction if there is an existing transaction, or null if there is none
     */
    public String currentTransactionId() {
        try {
            javax.transaction.Transaction txn = txnMgr.getTransaction();
            return txn != null ? txn.toString() : null;
        } catch (SystemException e) {
            return null;
        }
    }

    /**
     * Get the transaction manager.
     *
     * @return the transaction manager
     */
    public TransactionManager getTransactionManager() {
        return txnMgr;
    }

    /**
     * Starts a new transaction if one does not already exist, and associate it with the calling thread.
     *
     * @return the ModeShape transaction
     * @throws NotSupportedException If the calling thread is already associated with a transaction, and nested transactions are
     * not supported.
     * @throws SystemException If the transaction service fails in an unexpected way.
     */
    public Transaction begin() throws NotSupportedException, SystemException, RollbackException {
        // check if there isn't an active transaction already
        NestableThreadLocalTransaction localTx = LOCAL_TRANSACTION.get();
        if (localTx != null) {
            // we have an existing local transaction so we need to be aware of nesting by calling 'begin'
            if (logger.isTraceEnabled()) {
                logger.trace("Found active ModeShape transaction '{0}' ", localTx);
            }
            return localTx.begin();
        }

        // Get the transaction currently associated with this thread (if there is one) ...
        javax.transaction.Transaction txn = txnMgr.getTransaction();
        if (txn != null && Status.STATUS_ACTIVE != txn.getStatus()) {
            // there is a user transaction which is not valid, so abort everything
            throw new IllegalStateException(JcrI18n.errorInvalidUserTransaction.text(txn));
        }
        
        if (txn == null) {
            // There is no transaction or a leftover one which isn't active, so start a local one ...
            txnMgr.begin();
            // create our wrapper ...
            localTx = new NestableThreadLocalTransaction(txnMgr);
            localTx.begin();
            // notify the listener
            localTx.started();
            return logTransactionInformation(localTx);
        }

        // There's an existing tx, meaning user transactions are being used
        SynchronizedTransaction synchronizedTransaction = transactionTable.get(txn);
        if (synchronizedTransaction != null) {
            // notify the listener
            synchronizedTransaction.started();
            // we've already started our own transaction so just return it as is
            return logTransactionInformation(synchronizedTransaction);
        } else {
            synchronizedTransaction = new SynchronizedTransaction(txnMgr, txn);
            transactionTable.put(txn, synchronizedTransaction);
            // and register a synchronization
            txn.registerSynchronization(synchronizedTransaction);
            // and notify the listener
            synchronizedTransaction.started();
            return logTransactionInformation(synchronizedTransaction);
        }
    }

    private Transaction logTransactionInformation( Transaction transaction ) throws SystemException {
        if (!logger.isTraceEnabled()) {
            return transaction;
        }

        logger.trace("Created & stored new ModeShape synchronized transaction '{0}' ", transaction);
        javax.transaction.Transaction txn = txnMgr.getTransaction();
        assert txn != null;
        final String id = txn.toString();
        // Register a synchronization for this transaction ...
        if (!ACTIVE_TRACE_SYNCHRONIZATIONS.contains(id)) {
            if (transaction instanceof SynchronizedTransaction) {
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
        return transaction;
    }
    /**
     * Returns a the current ModeShape transaction, if one exists. 
     * <p>
     * A ModeShape transaction may not necessarily exist when a 
     * {@link javax.transaction.Transaction} is active. This is because ModeShape transactions are only created when a 
     * {@link org.modeshape.jcr.JcrSession} is saved.
     * </p>
     *
     * @return either a {@link org.modeshape.jcr.txn.Transactions.Transaction instance} or {@code null} if no ModeShape transaction
     * exists
     */
    public Transaction currentTransaction() {
        Transaction localTx = LOCAL_TRANSACTION.get();
        if (localTx != null) {
            return localTx;
        }
        try {
            javax.transaction.Transaction txn = txnMgr.getTransaction();
            if (txn == null) {
                // no active transaction
                return null;
            }
            if (Status.STATUS_ACTIVE != txn.getStatus()) {
                // there is a user transaction which is not valid, so abort everything
                throw new IllegalStateException(JcrI18n.errorInvalidUserTransaction.text(txn));
            }
            return transactionTable.get(txn);
        } catch (SystemException e) {
            logger.debug(e, "Cannot determine if there is an active transaction or not");
            return null;
        }

    }

    /**
     * Commits the current transaction, if one exists.
     *
     * @throws IllegalStateException If the calling thread is not associated with a transaction.
     * @throws SystemException If the transaction service fails in an unexpected way.
     * @throws HeuristicMixedException If a heuristic decision was made and some some parts of the transaction have been
     *         committed while other parts have been rolled back.
     * @throws HeuristicRollbackException If a heuristic decision to roll back the transaction was made.
     */
    public void commit() throws HeuristicRollbackException, RollbackException, HeuristicMixedException, SystemException {
        Transaction transaction = currentTransaction();
        if (transaction == null) {
            throw new IllegalStateException("No active transaction");
        }
        transaction.commit();     
    }
 
   /**
     * Rolls back the current transaction, if one exists.
     *
     * @throws IllegalStateException If the calling thread is not associated with a transaction.
     * @throws SystemException If the transaction service fails in an unexpected way.
     */
    public void rollback() throws SystemException {
        Transaction transaction = currentTransaction();
        if (transaction == null) {
            throw new IllegalStateException("No active transaction");
        }
        transaction.rollback();     
    }

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
    public void updateCache( final WorkspaceCache workspace, final ChangeSet changes, Transaction transaction ) {
        if (changes == null || changes.isEmpty()) {
            return;
        }
        if (transaction instanceof SynchronizedTransaction) {
            // only issue the changes when the transaction is successfully committed
            transaction.uponCommit(() -> workspace.changed(changes));
            if (workspace instanceof TransactionalWorkspaceCache) {
                ((TransactionalWorkspaceCache) workspace).changedWithinTransaction(changes);
            }
        } else if (transaction instanceof RollbackOnlyTransaction) {
            // The transaction has been marked for rollback only, so no need to even capture these changes because
            // no changes will ever escape the Session ...
        } else {
            // in all other cases we want to dispatch the changes immediately
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
    public interface Transaction {

        /**
         * Returns a unique identifier for the transaction.
         * 
         * @return a String, never {@code null}
         */
        String id();

        /**
         * Returns the status associated with the current transaction
         *
         * @return an {@code int} code representing a transaction status.
         * @throws SystemException - If the transaction service fails in an unexpected way.
         * @see javax.transaction.Status
         */
        int status() throws SystemException;

        /**
         * Register a function that will be called when the current transaction completes. The function will be executed 
         * regardless whether the transaction was committed or rolled back and regardless if the commit or rollback call failed 
         * or not.
         *
         * @param function the completion function
         */
        void uponCompletion( TransactionFunction function );

        /**
         * Register a function that will be called after the current transaction has been committed successfully, or immediately if there is not
         * currently an active transaction. If the transaction is rolled back, this function will not be executed.
         *
         * @param function the completion function
         */
        void uponCommit( TransactionFunction function );

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
         *         the calling thread is not associated with a transaction, or because it is in the {@link javax.transaction.Status#STATUS_PREPARED
         *         prepared state}.
         * @throws SecurityException If the caller is not allowed to roll back this transaction.
         * @throws SystemException If the transaction service fails in an unexpected way.
         */
        void rollback() throws IllegalStateException, SecurityException, SystemException;
    }

    /**
     * A function that should be executed in relation to a transaction.
     */
    public interface TransactionFunction {
        void execute();
    }

    protected abstract class BaseTransaction implements Transaction {
        protected final TransactionManager txnMgr;
        protected final String id;
        
        private final LinkedHashSet<TransactionFunction> uponCompletionFunctions = new LinkedHashSet<>();
        private final LinkedHashSet<TransactionFunction> uponCommitFunctions = new LinkedHashSet<>();

        protected BaseTransaction( TransactionManager txnMgr ) {
            this.txnMgr = txnMgr;
            this.id = UUID.randomUUID().toString();
        }

        protected BaseTransaction started() {
            Transactions.this.listener.txStarted(id);
            return this;
        }

        @Override
        public void uponCompletion( TransactionFunction function ) {
            uponCompletionFunctions.add(function);
        }

        @Override
        public void uponCommit(TransactionFunction function) {
            uponCommitFunctions.add(function);
        }

        @Override
        public int status() throws SystemException {
            return txnMgr.getStatus();
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append("[");
            sb.append("id='").append(id).append('\'');
            sb.append(']');
            return sb.toString();
        }

        protected void executeFunctionsUponCompletion() {
            try {
                uponCompletionFunctions.forEach(TransactionFunction::execute);
            } finally {
                uponCompletionFunctions.clear();
            }
        }

        protected void executeFunctionsUponCommit() {
            try {
                uponCommitFunctions.forEach(TransactionFunction::execute);
            } finally {
                uponCommitFunctions.clear();
            }
        }
    }

    protected abstract class SimpleTransaction extends BaseTransaction {

        protected SimpleTransaction( TransactionManager txnMgr ) {
            super(txnMgr);
        }

        @Override
        public void rollback() throws IllegalStateException, SecurityException, SystemException {
            boolean canRollback = canRollback();
            try {
                if (canRollback) {
                    // rollback first
                    txnMgr.rollback();
                }
            } finally {
                if (canRollback) {
                    // notify the listener always
                    listener.txRolledback(id);
                    // even if rollback fails, we want to execute the complete functions to try and leave the repository into a consistent state
                    // because a rollback was requested in the first place, meaning something went wrong during the UOW
                    executeFunctionsUponCompletion();
                }
            }
        }

        @Override
        public void commit()
                throws RollbackException, SecurityException,
                       IllegalStateException, SystemException {
            try {
                // commit first
                txnMgr.commit();
                // notify the listener
                listener.txCommitted(id);
                // run the ModeShape commit functions after we've notified the listener
                executeFunctionsUponCommit();
            } catch (RollbackException | HeuristicMixedException | HeuristicRollbackException e) {
                listener.txRolledback(id);
                throw new RollbackException(e.getMessage());
            } catch (SystemException e) {
                listener.txRolledback(id);
                throw e;
            } finally {
                // even if commit fails, we want to execute the complete functions to try and leave the repository into a consistent state
                executeFunctionsUponCompletion();    
            }
        }
        
        protected boolean canRollback() {
            try {
                switch (super.status()) {
                    case Status.STATUS_ACTIVE:
                    case Status.STATUS_COMMITTING:    
                    case Status.STATUS_PREPARED:    
                    case Status.STATUS_PREPARING:    
                    case Status.STATUS_MARKED_ROLLBACK:    
                        return true;
                    default: {
                        return false;
                    }
                }
            } catch (SystemException e) {
                return false;
            }
        }
    }

    protected abstract class TraceableSimpleTransaction extends SimpleTransaction {

        protected TraceableSimpleTransaction( TransactionManager txnMgr ) {
            super(txnMgr);
        }

        @Override
        public void rollback() throws IllegalStateException, SecurityException, SystemException {
            if (logger.isTraceEnabled()) {
                logger.trace("Rolling back transaction '{0}'", id);
            }
            super.rollback();
        }

        @Override
        public void commit()
                throws RollbackException, SecurityException,
                       IllegalStateException, SystemException {
            if (logger.isTraceEnabled()) {
                super.commit();
                logger.trace("Committed transaction '{0}'", id);
            } else {
                super.commit();
            }
        }
    }

    protected class NestableThreadLocalTransaction extends TraceableSimpleTransaction {
        private AtomicInteger nestedLevel = new AtomicInteger(0);
        
        protected NestableThreadLocalTransaction( TransactionManager txnMgr ) {
            super(txnMgr);
            LOCAL_TRANSACTION.set(this);
        }

        @Override
        public void rollback() throws IllegalStateException, SecurityException, SystemException {
            try {
                super.rollback();
            } finally {
                cleanup();    
            }
        }

        @Override
        public void commit() throws RollbackException,  SecurityException, 
                                    IllegalStateException, SystemException {
            if (nestedLevel.getAndDecrement() == 1) {
                try {
                    super.commit();
                } finally {
                    cleanup();
                }
            } else {
                logger.trace("Not committing transaction because it's nested within another transaction. Only the top level transaction should commit");
            }
        }

        protected void cleanup() {
            LOCAL_TRANSACTION.remove();
            nestedLevel = null;
        }

        protected NestableThreadLocalTransaction begin() {
            nestedLevel.incrementAndGet();
            return this;
        }
    }

    protected final class SynchronizedTransaction extends BaseTransaction implements Synchronization {
        private final javax.transaction.Transaction transaction;
        
        protected SynchronizedTransaction( TransactionManager txnMgr, javax.transaction.Transaction transaction ) {
            super(txnMgr);
            this.transaction = transaction;
        }

        @Override
        public void commit()  {
            if (logger.isTraceEnabled()) {
                logger.trace("'{0}' ignoring commit call coming from ModeShape.", id);
            }
            //nothing by default
        }

        @Override
        public void rollback() {
            if (logger.isTraceEnabled()) {
                logger.trace("'{0}' ignoring rollback call coming from ModeShape.", id);
            }
            // nothing by default
        }

        @Override
        public void beforeCompletion() {
            // nothing before completion...
        }

        @Override
        public void afterCompletion(int status) {
            if (logger.isTraceEnabled()) {
                logger.trace("Synchronization for '{0}' notified after completion with status '{1}'", id, status);
            }
            try {
                if (Status.STATUS_COMMITTED == status) {
                    listener.txCommitted(id);
                    executeFunctionsUponCommit();
                } else if (Status.STATUS_ROLLEDBACK == status){
                    listener.txRolledback(id);
                }
            } finally {
                try {
                    executeFunctionsUponCompletion();
                } finally {
                    transactionTable.remove(this.transaction);
                }
            }
        }
    
        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append("[local ").append("txId='")
                                                                                  .append(id).append("', original tx='")
                                                                                  .append(transaction).append("']");
            return sb.toString();
        }
    }

    protected class RollbackOnlyTransaction implements Transaction {

        protected RollbackOnlyTransaction() {
        }

        @Override
        public String id() {
            return "";
        }

        @Override
        public int status() {
            return Status.STATUS_MARKED_ROLLBACK;
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
        public void uponCompletion(TransactionFunction function) {
            // do nothing
        }

        @Override
        public void uponCommit(TransactionFunction function) {
            // do nothing
        }
    }
    
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
