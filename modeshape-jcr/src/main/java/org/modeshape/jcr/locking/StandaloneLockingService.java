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

import java.util.concurrent.locks.ReentrantLock;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.jcr.JcrI18n;

/**
 * {@link LockingService} implementation that is used by ModeShape when running in non-clustered (local) mode.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @since 5.0
 */
@ThreadSafe
public class StandaloneLockingService extends AbstractLockingService<ReentrantLock> {

    public StandaloneLockingService() {
        this(0);
    }
    
    public StandaloneLockingService(long lockTimeoutMillis) {
        super(lockTimeoutMillis);
    }

    @Override
    protected ReentrantLock createLock(String name) {
        return new ReentrantLock();
    }

    @Override
    protected void validateLock(ReentrantLock lock) {
        if (!lock.isHeldByCurrentThread()) {
            if (lock.isLocked()) {
                logger.warn(JcrI18n.warnAttemptingToUnlockAnotherLock, Thread.currentThread().getName(), lock);
            } else {
                logger.debug("attempting to unlock an already unlocked lock....");
            }
        }     
    }

    @Override
    protected boolean releaseLock( ReentrantLock lock ) {
        if (!lock.isHeldByCurrentThread()) {
            logger.debug("Cannot release {0} because it is not held by the current thread...", lock);
            return false;
        }
        // Try to unlock the lock ...
        // since these are reentrant, remove all the acquisitions by the calling thread
        while (lock.getHoldCount() > 0) {
            lock.unlock();
        }
        return true;
    }
}
