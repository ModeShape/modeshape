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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
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
import org.infinispan.transaction.LocalTransaction;
import org.infinispan.transaction.TransactionTable;
import org.infinispan.transaction.xa.GlobalTransaction;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.SessionEnvironment;
import org.modeshape.jcr.cache.change.ChangeSet;
import org.modeshape.jcr.cache.document.TransactionalWorkspaceCache;
import org.modeshape.jcr.cache.document.WorkspaceCache;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Property;

/**
 * An implementation of {@link Transactions} that will attempt to register a {@link Synchronization} with the current transaction.
 */
public final class SynchronizedTransactions extends Transactions {

    private static final ThreadLocal<Transaction> ACTIVE_TRANSACTION = new ThreadLocal<Transaction>();

    @SuppressWarnings( "rawtypes")
    private final Cache localCache;
    private final AtomicReference<TransactionListener> transactionListener = new AtomicReference<TransactionListener>();
    private final TransactionTable transactionTable;

    /**
     * Creates a new instance which wraps a transaction manager and monitor factory
     *
     * @param monitorFactory a {@link org.modeshape.jcr.cache.SessionEnvironment.MonitorFactory} instance; never null
     * @param txnMgr a {@link javax.transaction.TransactionManager} instance; never null
     * @param localCache a {@link org.infinispan.Cache} instance representing the main ISPN cache
     */
    @SuppressWarnings( "rawtypes")
    public SynchronizedTransactions( SessionEnvironment.MonitorFactory monitorFactory,
                                     TransactionManager txnMgr, 
                                     Cache localCache ) {
        super(monitorFactory, txnMgr);
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
            // There's an existing tx, meaning user transactions are being used

            // register the ISPN listener lazily, only when we're sure user transactions are used
            if (this.transactionListener.get() == null) {
                if (this.transactionListener.compareAndSet(null, new TransactionListener())) {
                    this.localCache.addListener(this.transactionListener.get());
                }
            }
            //TODO author=Horia Chiorean date=17-Nov-14 description=non ISPN 7 compatible
            // find the ISPN tx id for the current transaction
            LocalTransaction localTransaction = transactionTable.getLocalTransaction(txn);
            if (localTransaction == null) {
                // there's an existing user transaction which hasn't enrolled the ISPN cache. So we'll suspend it and start a 
                // regular transaction instead
                logger.debug("Active transaction detected, but the Infinispan cache isn't aware of it. Suspending it for the duration of the ModeShape transaction..." );

                final javax.transaction.Transaction suspended = txnMgr.suspend();
                assert suspended != null;
                // start a new local (regular) transaction
                txnMgr.begin();
                result = new NestableThreadLocalTransaction(txnMgr, ACTIVE_TRANSACTION).begin();
                // we'll resume the original transaction once we've completed (regardless whether successfully or not)
                result.uponCompletion(new TransactionFunction() {
                    @Override
                    public void execute() {
                        try {
                            txnMgr.resume(suspended);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        } 
                    }
                });
            } else {
                // create our internal wrapper which will be set thread-local active
                result = new SynchronizedTransaction(txnMgr, localTransaction.getGlobalTransaction());
            }
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
    public void updateCache( final WorkspaceCache workspace,
                             final ChangeSet changes,
                             Transaction transaction ) {
        if (changes != null && !changes.isEmpty()) {
            if (transaction instanceof SynchronizedTransaction) {
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
     * An Infinispan transaction listener which will be invoked each time a transaction completes.
     */
    @Listener
    @SuppressWarnings( "rawtypes")
    public final class TransactionListener {

        /**
         * Method which will be invoked by Infinispan once a tx.commit or tx.rollback has finished processing at a cache-level.
         * @param event a {@link TransactionCompletedEvent} instance.
         */
        @TransactionCompleted
        public void transactionCompleted( TransactionCompletedEvent event ) {
            if (logger.isTraceEnabled()) {
                logger.trace("Received transaction completed event: '{0}'", event);
            }

            if (!event.isOriginLocal()) {
                // if the event is not local, we're not interested in processing it
                if (logger.isTraceEnabled()) {
                    logger.trace("Ignoring event '{0}' because it did not originate on this cluster node", event);
                }
                return;
            }
            GlobalTransaction eventIspnTransaction = event.getGlobalTransaction();
            if (eventIspnTransaction == null ) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Ignoring event '{0}' because there is no mapped active user transaction", event);
                }
                return;
            }

            // check if the current transaction is the user transaction that has been completed (i.e. has the same ISPN tx id
            // as the one that's coming in for the event
            Transaction activeTransaction = currentTransaction();
            if (activeTransaction instanceof SynchronizedTransaction &&
                ((SynchronizedTransaction) activeTransaction).ispnTransaction().equals(eventIspnTransaction)) {
                SynchronizedTransaction synchronizedTransaction = (SynchronizedTransaction) activeTransaction;
                // clear the active ModeShape transaction for this thread
                ACTIVE_TRANSACTION.remove();
                // and invoke the functions
                if (event.isTransactionSuccessful()) {
                    synchronizedTransaction.executeFunctionsUponCommit();
                    synchronizedTransaction.dispatchRecordedChanges();
                }
                synchronizedTransaction.executeFunctionsUponCompletion();
            } else {
                if (logger.isTraceEnabled()) {
                    logger.trace("Ignoring event '{0}' because the transaction id does not match that of the active thread transaction '{1}'",
                                 event, activeTransaction);
                }
            }
        }
    }

    protected final class SynchronizedTransaction extends BaseTransaction {
        private final GlobalTransaction ispnTransaction;
        private final SynchronizedMonitor monitor;

        protected SynchronizedTransaction( TransactionManager txnMgr, GlobalTransaction ispnTransaction) {
            super(txnMgr);
            this.ispnTransaction = ispnTransaction;
            this.monitor = new SynchronizedMonitor(newMonitor());
        }

        @Override
        public void commit()  {
            if (logger.isTraceEnabled()) {
                logger.trace("'{0}' ignoring commit call coming from ModeShape. Waiting to be notified by Infinispan'", this);
            }
            //nothing by default
        }

        @Override
        public void rollback() {
            if (logger.isTraceEnabled()) {
                logger.trace("'{0}' ignoring rollback call coming from ModeShape. Waiting to be notified by Infinispan'", this);
            }
            // nothing by default
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("SynchronizedTransaction{");
            sb.append("infinispanTransaction=").append(ispnTransaction);
            sb.append('}');
            return sb.toString();
        }

        protected GlobalTransaction ispnTransaction() {
            return ispnTransaction;
        }

        protected void dispatchRecordedChanges() {
            this.monitor.dispatchRecordedChanges();
        }

        @Override
        public SessionEnvironment.Monitor createMonitor() {
            return this.monitor;
        }
    }

    protected class RollbackOnlyTransaction implements Transaction {

        public RollbackOnlyTransaction() {
        }

        @Override
        public SessionEnvironment.Monitor createMonitor() {
            return newMonitor();
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
        public void uponCompletion( Transactions.TransactionFunction function ) {
            // do nothing
        }

        @Override
        public void uponCommit( Transactions.TransactionFunction function ) {
            // do nothing
        }
    }

    protected static final class SynchronizedMonitor implements SessionEnvironment.Monitor {
        private final SessionEnvironment.Monitor delegate;
        private final AtomicLong changesCount;

        protected SynchronizedMonitor( SessionEnvironment.Monitor delegate ) {
            this.delegate = delegate;
            this.changesCount = new AtomicLong(0);
        }

        @Override
        public void recordAdd( String workspace,
                               NodeKey key,
                               Path path,
                               Name primaryType,
                               Set<Name> mixinTypes,
                               Iterator<Property> propertiesIterator ) {
            delegate.recordAdd(workspace, key, path, primaryType, mixinTypes, propertiesIterator);
        }

        @Override
        public void recordUpdate( String workspace,
                                  NodeKey key,
                                  Path path,
                                  Name primaryType,
                                  Set<Name> mixinTypes,
                                  Iterator<Property> properties ) {
            delegate.recordUpdate(workspace, key, path, primaryType, mixinTypes, properties);
        }

        @Override
        public void recordRemove( String workspace,
                                  Iterable<NodeKey> keys ) {
            delegate.recordRemove(workspace, keys);
        }

        @Override
        public void recordChanged( long changedNodesCount ) {
            changesCount.getAndAdd(changedNodesCount);
        }

        protected void dispatchRecordedChanges() {
            delegate.recordChanged(changesCount.get());
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
