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

import java.util.concurrent.atomic.AtomicInteger;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import org.modeshape.jcr.cache.SessionCache;
import org.modeshape.jcr.cache.SessionEnvironment.MonitorFactory;

/**
 * An implementation of {@link Transactions} that does not even check whether there is a current transaction and instead always
 * attempts to create a transaction within the {@link SessionCache#save()} calls. This is more efficient when the repository can
 * be set up to never use container-managed or user-managed transactions.
 */
public final class NoClientTransactions extends Transactions {

    /**
     * During ws cache initialization (either when the repo starts up of a new ws is created), there can be a case of semantically
     * nested simple transactions, so we need effective make sure that only 1 instance of an active transaction can exist at any
     * given time. We cannot use multiple instance because completion functions are instance-dependent
     */
    protected static final ThreadLocal<NoClientTransaction> ACTIVE_TRANSACTION = new ThreadLocal<NoClientTransaction>();

    /**
     * Creates a new instance passing in the given monitor factory and transaction manager
     * @param monitorFactory a {@link MonitorFactory} instance; never null
     * @param txnMgr a {@link TransactionManager} instance; never null
     */
    public NoClientTransactions( MonitorFactory monitorFactory,
                                 TransactionManager txnMgr ) {
        super(monitorFactory, txnMgr);
    }

    @Override
    public synchronized Transaction begin() throws NotSupportedException, SystemException {
        if (ACTIVE_TRANSACTION.get() == null) {
            // Start a transaction ...
            txnMgr.begin();
            if (logger.isTraceEnabled()) {
                logger.trace("Begin transaction {0}", currentTransactionId());
            }
            ACTIVE_TRANSACTION.set(new NoClientTransaction(txnMgr));
        }
        return ACTIVE_TRANSACTION.get().transactionBegin();
    }

    protected class NoClientTransaction extends TraceableSimpleTransaction {
        private final AtomicInteger nestedLevel = new AtomicInteger(0);

        public NoClientTransaction( TransactionManager txnMgr ) {
            super(txnMgr);
        }

        @Override
        public void commit()
            throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException,
            IllegalStateException, SystemException {
            if (nestedLevel.getAndDecrement() == 1) {
                NoClientTransactions.ACTIVE_TRANSACTION.remove();
                super.commit();
            } else {
                logger.trace("Not committing transaction because it's nested within another transaction. Only the top level transaction should commit");
            }
        }

        @Override
        public void rollback() throws IllegalStateException, SecurityException, SystemException {
            NoClientTransactions.ACTIVE_TRANSACTION.remove();
            super.rollback();
        }

        protected NoClientTransaction transactionBegin() {
            nestedLevel.incrementAndGet();
            return this;
        }
    }
}
