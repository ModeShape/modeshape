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
package org.modeshape.jcr.locking;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import org.modeshape.common.annotation.ThreadSafe;

/**
 * {@link LockingService} implementation that is used by ModeShape when running in non-clustered (local) mode.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @since 5.0
 */
@ThreadSafe
public class StandaloneLockingService extends AbstractLockingService<StandaloneLockingService.NodeLock> {

    public StandaloneLockingService() {
        this(0);
    }
    
    public StandaloneLockingService(long lockTimeoutMillis) {
        super(lockTimeoutMillis);
    }

    @Override
    protected NodeLock createLock(String name) {
        return new NodeLock();
    }
    
    @Override
    protected boolean releaseLock( NodeLock lock ) {
        // always allow unlocking, regardless of the thread
        lock.unlock();
        return true;
    }
    
    /**
     * A simple lock implementation which explicitly violates the default lock contract of only the owning thread being able
     * to unlock. This is required because transaction commit/rollback ops can occur off different threads and therefore
     * locks which were acquired on one thread should be unlocked in another.
     */
    protected static class NodeLock extends AbstractQueuedSynchronizer implements Lock {
    
        public NodeLock() {
        }
    
        @Override
        public void lock() {
            acquire(1);        
        }
    
        @Override
        public void lockInterruptibly() throws InterruptedException {
            acquireInterruptibly(1);        
        }
    
        @Override
        protected boolean tryAcquire(int arg) {
            if (isHeldExclusively()) {
                return true;
            }
            if (compareAndSetState(0, arg)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }
    
        @Override
        protected boolean isHeldExclusively() {
            return getState() > 0  && Thread.currentThread() == getExclusiveOwnerThread();
        }
    
        @Override
        public boolean tryLock() {
            return tryAcquire(1);            
        }
    
        @Override
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            return tryAcquireNanos(1, unit.toNanos(time));
        }
    
        @Override
        public void unlock() {
            release(0);        
        }
    
        @Override
        protected boolean tryRelease(int arg) {
            // simply allow the release to happen from *any* thread (see the comment for this class)
            setExclusiveOwnerThread(null);
            setState(0);
            return true;
        }
    
        @Override
        public Condition newCondition() {
            return new ConditionObject();
        }
    }
}
