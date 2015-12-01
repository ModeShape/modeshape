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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.Transaction;
import org.junit.Test;

/**
 * Unit test for {@link LocalTransactionManagerTest}
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class LocalTransactionManagerTest {
    
    private final LocalTransactionManager txManager = new LocalTransactionManager();
    private final TestSynchronization sync = new TestSynchronization();
    
    @Test
    public void shouldCreateNewTransactions() throws Exception {
        assertEquals(Status.STATUS_NO_TRANSACTION, txManager.getStatus());
        txManager.begin();
        Transaction tx = txManager.getTransaction();
        assertNotNull(tx);
        assertEquals(Status.STATUS_ACTIVE, tx.getStatus());
        tx.commit();
    }
    
    @Test(expected = NotSupportedException.class)
    public void shouldNotSupportNestedTransactions() throws Exception {
        try {
            txManager.begin();
            txManager.begin();
        } finally {
            LocalTransactionManager.clear();
        }
    }
    
    @Test
    public void shouldCommitTransaction() throws Exception {
        txManager.begin();
        Transaction tx = txManager.getTransaction();
        assertEquals(Status.STATUS_ACTIVE, tx.getStatus());
        assertEquals(Status.STATUS_ACTIVE, txManager.getStatus());
        tx.registerSynchronization(sync);
        txManager.commit();
        sync.assertBeforeCompletion(true);
        sync.assertAfterCompletion(true);
        sync.assertAfterCompletionStatus(Status.STATUS_COMMITTED);
        assertNull(txManager.getTransaction());
    }
    
    @Test
    public void shouldRollbackTransaction() throws Exception {
        txManager.begin();
        Transaction tx = txManager.getTransaction();
        tx.registerSynchronization(sync);
        txManager.rollback();
        sync.assertBeforeCompletion(true);
        sync.assertAfterCompletion(true);
        sync.assertAfterCompletionStatus(Status.STATUS_ROLLEDBACK);
        assertNull(txManager.getTransaction());
    }
    
    @Test
    public void shouldSupportRollbackOnlyTransactionsWithCommit() throws Exception {
        txManager.begin();
        Transaction tx = txManager.getTransaction();
        tx.registerSynchronization(sync);
        txManager.setRollbackOnly();
        try {
            tx.commit();
            fail("Expected Rollback Exception");
        } catch (RollbackException e) {
            //expected
        }
        sync.assertBeforeCompletion(true);
        sync.assertAfterCompletion(true);
        sync.assertAfterCompletionStatus(Status.STATUS_ROLLEDBACK);
        assertNull(txManager.getTransaction());
    }

    @Test
    public void shouldSupportRollbackOnlyTransactionsWithRollback() throws Exception {
        txManager.begin();
        Transaction tx = txManager.getTransaction();
        tx.registerSynchronization(sync);
        txManager.setRollbackOnly();
        tx.rollback();
        sync.assertBeforeCompletion(true);
        sync.assertAfterCompletion(true);
        sync.assertAfterCompletionStatus(Status.STATUS_ROLLEDBACK);
        assertNull(txManager.getTransaction());
    }
    
    @Test
    public void shouldSuspendAndResumeTransaction() throws Exception {
        txManager.begin();
        Transaction tx = txManager.suspend();
        assertNotNull(tx);
        assertEquals(Status.STATUS_NO_TRANSACTION, txManager.getStatus());
        assertNull(txManager.getTransaction());
        txManager.resume(tx);
        assertEquals(Status.STATUS_ACTIVE, txManager.getStatus());
        tx.commit();
        assertNull(txManager.getTransaction());
    }
    
    @Test
    public void shouldConfineTransactionToThread() throws Exception {
        txManager.begin();
        Transaction tx = txManager.getTransaction();
        tx.registerSynchronization(sync);
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            Callable<Void> task =  () -> {
                try {
                    tx.commit();
                    fail("Should not allow committing off another thread");
                } catch (IllegalStateException e) {
                    //expected
                }
                txManager.begin();
                TestSynchronization sync = new TestSynchronization();
                txManager.getTransaction().registerSynchronization(sync);
                txManager.commit();
                sync.assertBeforeCompletion(true);
                sync.assertAfterCompletion(true);
                sync.assertAfterCompletionStatus(Status.STATUS_COMMITTED);
                return null;
            };
            executorService.submit(task).get();
            txManager.commit();
            sync.assertBeforeCompletion(true);
            sync.assertAfterCompletion(true);
            sync.assertAfterCompletionStatus(Status.STATUS_COMMITTED);
            assertNull(txManager.getTransaction());
        } finally {
            executorService.shutdown();
        }
    }

    protected static class TestSynchronization implements Synchronization  {
        private AtomicBoolean beforeCompletion = new AtomicBoolean();
        private AtomicBoolean afterCompletion = new AtomicBoolean();
        private int afterCompletionStatus = -1;
        
        @Override
        public void beforeCompletion() {
            beforeCompletion.compareAndSet(false, true);            
        }

        @Override
        public void afterCompletion(int status) {
            afterCompletion.compareAndSet(false, true);
            afterCompletionStatus = status;
        }
        
        protected void assertBeforeCompletion(boolean called) {   
            assertThat("beforeCompletion invocation failed", called, is(beforeCompletion.get()));
        }

        protected void assertAfterCompletion(boolean called) {
            assertThat("afterCompletion invocation failed", called, is(afterCompletion.get()));
        }
        
        protected void assertAfterCompletionStatus(int status) {
            assertThat("afterCompletion status is invalid", status, is(this.afterCompletionStatus));    
        }
    }
}
