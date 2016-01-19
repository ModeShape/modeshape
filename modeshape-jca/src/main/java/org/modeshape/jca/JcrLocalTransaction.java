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
package org.modeshape.jca;

import javax.resource.ResourceException;
import javax.resource.spi.LocalTransaction;
import javax.resource.spi.LocalTransactionException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.jcr.txn.Transactions;

/**
 * ModeShape's JCA implementation of a {@link javax.resource.spi.LocalTransaction}.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
@Immutable
public final class JcrLocalTransaction implements LocalTransaction {
    
    private final Transactions transactions;

    protected JcrLocalTransaction(Transactions transactions) {
        this.transactions = transactions;
    }

    @Override
    public void begin() throws ResourceException {
        try {
            transactions.begin();
        } catch (NotSupportedException | SystemException | RollbackException e) {
            throw new LocalTransactionException(e);
        } 
    }

    @Override
    public void commit() throws ResourceException {
        try {
            final Transactions.Transaction transaction = transactions.currentTransaction();
            if (transaction == null) {
                throw new LocalTransactionException("A local transaction does not exist");
            }
            transaction.commit();
        } catch (RollbackException | HeuristicMixedException | HeuristicRollbackException | SystemException e) {
            throw new LocalTransactionException(e);
        } 
    }

    @Override
    public void rollback() throws ResourceException {
        final Transactions.Transaction transaction = transactions.currentTransaction();
        if (transaction == null) {
            throw new LocalTransactionException("A local transaction does not exist");
        }
        try {
            transaction.rollback();
        } catch (SystemException e) {
            throw new LocalTransactionException(e);
        }
    }
}
