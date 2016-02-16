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

import java.util.Optional;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import org.modeshape.schematic.annotation.ThreadSafe;
import org.modeshape.common.annotation.Immutable;

/**
 * A simple, in-memory local transaction manager implementation which is used as fallback by ModeShape when no other "real" transaction
 * manager can be located and which supports only local transactions. 
 * <p>
 * Transactions managed by this manager DO NOT support {@link javax.transaction.xa.XAResource xa resources} and therefore 
 * global transactions.
 * </p>
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @since 5.0
 */
@ThreadSafe
@Immutable
public class LocalTransactionManager implements TransactionManager {
    private static final ThreadLocal<Optional<LocalTransaction>> ACTIVE_TRANSACTION = ThreadLocal.withInitial(Optional::empty);
    
    @Override
    public void begin() throws NotSupportedException, SystemException {
        if (hasActiveTransaction()) {
            throw new NotSupportedException("Nested transactions are not supported");
        }
        ACTIVE_TRANSACTION.set(Optional.of(new LocalTransaction()));
    }

    protected static boolean hasActiveTransaction() {
        return ACTIVE_TRANSACTION.get().isPresent();
    }
    
    protected static void clear() {
        ACTIVE_TRANSACTION.set(Optional.empty());
    }

    @Override
    public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {
        transactionForThread().commit();
    }

    protected static LocalTransaction transactionForThread() {
        return ACTIVE_TRANSACTION.get().orElseThrow(
                () -> new IllegalStateException("Current thread does not have a transaction"));
    }

    @Override
    public void rollback() throws IllegalStateException, SecurityException, SystemException {
        transactionForThread().rollback();
    }

    @Override
    public void setRollbackOnly() throws IllegalStateException, SystemException {
        transactionForThread().setRollbackOnly();
    }

    @Override
    public int getStatus() throws SystemException {
        Optional<LocalTransaction> localTransaction = ACTIVE_TRANSACTION.get();
        return localTransaction.isPresent() ? localTransaction.get().getStatus() : Status.STATUS_NO_TRANSACTION;  
    }

    @Override
    public Transaction getTransaction() throws SystemException {
        return ACTIVE_TRANSACTION.get().orElse(null);
    }

    @Override
    public void setTransactionTimeout(int seconds) throws SystemException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Transaction suspend() throws SystemException {
        Optional<LocalTransaction> localTransaction = ACTIVE_TRANSACTION.get();
        if (localTransaction.isPresent()) {
            LocalTransaction tx = localTransaction.get();
            clear();
            return tx;
        } 
        return null;
    }

    @Override
    public void resume(Transaction tobj) throws InvalidTransactionException, IllegalStateException, SystemException {
        if (ACTIVE_TRANSACTION.get().isPresent()) {
            throw new IllegalStateException("Current thread has a tx associated");
        }
        if (!(tobj instanceof LocalTransaction)) {
            throw new InvalidTransactionException(tobj +  " is not a valid local transaction");
        }
        ACTIVE_TRANSACTION.set(Optional.of((LocalTransaction) tobj));
    }
}
