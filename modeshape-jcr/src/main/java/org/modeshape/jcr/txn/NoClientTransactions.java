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

import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import org.modeshape.jcr.cache.SessionCache;

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
    private static final ThreadLocal<NestableThreadLocalTransaction> ACTIVE_TRANSACTION = new ThreadLocal<NestableThreadLocalTransaction>();

    /**
     * Creates a new instance passing in the given monitor factory and transaction manager
     * 
     * @param txnMgr a {@link TransactionManager} instance; never null
     */
    public NoClientTransactions( TransactionManager txnMgr ) {
        super(txnMgr);
    }

    @Override
    public Transaction currentTransaction() {
        return ACTIVE_TRANSACTION.get();
    }

    @Override
    public synchronized Transaction begin() throws NotSupportedException, SystemException {
        NestableThreadLocalTransaction tx = ACTIVE_TRANSACTION.get();
        if (tx == null) {
            // Start a transaction ...
            txnMgr.begin();
            if (logger.isTraceEnabled()) {
                logger.trace("Begin transaction {0}", currentTransactionId());
            }
            tx = new NestableThreadLocalTransaction(txnMgr, ACTIVE_TRANSACTION);
        }
        return tx.begin();
    }
}
