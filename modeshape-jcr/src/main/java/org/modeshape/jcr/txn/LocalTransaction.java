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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.annotation.ThreadSafe;

/**
 * Transaction type returned by {@link LocalTransactionManager}
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @since 5.0
 */         
@Immutable
@ThreadSafe
public class LocalTransaction implements Transaction {
    private final List<Synchronization> synchronizations = new ArrayList<>();
    private final AtomicInteger status;
    private final String id;
    
    protected LocalTransaction() {
        this.status = new AtomicInteger(Status.STATUS_ACTIVE);
        this.id = UUID.randomUUID().toString();
    }

    @Override
    public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, SystemException {
        try {
            validateThreadOwnership();
            if (markedForRollback()) {
                rollback();
                throw new RollbackException("Transaction was marked for rollback");
            }
            if (!isActive()) {
               throw new SystemException("Illegal transaction status:" + status); 
            }
            synchronizations.forEach(Synchronization::beforeCompletion);
            status.set(Status.STATUS_COMMITTED);
            synchronizations.forEach((sync)->sync.afterCompletion(Status.STATUS_COMMITTED));
        } finally {
            LocalTransactionManager.clear();
        }
    }

    private void validateThreadOwnership() {
        LocalTransaction txForThread =
                LocalTransactionManager.hasActiveTransaction() ? LocalTransactionManager.transactionForThread()
                                                               : null;
        if (!this.equals(txForThread)) {
            throw new IllegalStateException("Current thread does not own the transaction");
        }
    }

    private boolean markedForRollback() {
        return status.get() == Status.STATUS_MARKED_ROLLBACK;
    }

    private boolean isActive() {
        return status.get() == Status.STATUS_ACTIVE;
    }

    @Override
    public void rollback() throws IllegalStateException, SystemException {
        try {
            validateThreadOwnership();
            if (!isActive() && !markedForRollback()) {
                throw new SystemException("Illegal transaction status:" + status);
            }
            synchronizations.forEach(Synchronization::beforeCompletion);
            status.set(Status.STATUS_ROLLEDBACK);
            synchronizations.forEach((sync)->sync.afterCompletion(Status.STATUS_ROLLEDBACK));
        } finally {
            LocalTransactionManager.clear();
        }
    }

    @Override
    public void setRollbackOnly() throws IllegalStateException, SystemException {
        if (!isActive()) {
            throw new IllegalStateException("Current status is invalid" + status.get()); 
        }
        this.status.compareAndSet(Status.STATUS_ACTIVE, Status.STATUS_MARKED_ROLLBACK);
    }

    @Override
    public int getStatus() throws SystemException {
        return status.get();
    }

    @Override
    public boolean enlistResource(XAResource xaRes) throws RollbackException, IllegalStateException, SystemException {
        throw new UnsupportedOperationException("Local transactions do not support XA resources...");
    }

    @Override
    public boolean delistResource(XAResource xaRes, int flag) throws IllegalStateException, SystemException {
        throw new UnsupportedOperationException("Local transactions do not support XA resources...");
    }

    @Override
    public void registerSynchronization(Synchronization sync) throws RollbackException, IllegalStateException, SystemException {
        synchronizations.add(sync);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LocalTransaction that = (LocalTransaction) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
