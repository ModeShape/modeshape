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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.logging.Logger;

/**
 * {@link LockingService} implementation that is used by ModeShape when running in non-clustered (local) mode.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @since 5.0
 */
@Immutable
@ThreadSafe
public class StandaloneLockingService implements  LockingService {

    private static final Logger LOGGER = Logger.getLogger(StandaloneLockingService.class);
    
    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public StandaloneLockingService() {
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit, String... names) {
        Set<String> successfullyLocked = new HashSet<>();
        try {
            for (String name : names) {
                LOGGER.debug("Attempting to lock {0}", name);
                ReentrantLock lock = locks.get(name);
                boolean success = false;
                try {
                    if (lock != null) {
                        success = time > 0 ? lock.tryLock(time, unit) : lock.tryLock();
                    } else {
                        lock = new ReentrantLock();
                        ReentrantLock existingLock = locks.putIfAbsent(name, lock);
                        if (existingLock != null) {
                           lock = existingLock; 
                        }
                        success = time > 0 ? lock.tryLock(time, unit) : lock.tryLock();                                
                    }
                } catch (InterruptedException e) {
                    LOGGER.debug("Thread " + Thread.currentThread().getName()
                                 + " received interrupt request while waiting to acquire lock '{0}'", name);
                    Thread.currentThread().interrupt();
                }
                if (!success) {
                    LOGGER.debug("Unable to acquire lock on {0}. Reverting back the already obtained locks: {1}", name,
                                 successfullyLocked);
                    unlock(successfullyLocked.toArray(new String[successfullyLocked.size()]));
                    return false;
                }
                LOGGER.debug("{0} locked successfully", name);
                successfullyLocked.add(name);
            }
        } catch (Throwable t) {
            LOGGER.debug(t, "Unexpected exception while attempting to lock");
            // in case of any exception release all owned locks... 
            unlock(successfullyLocked.toArray(new String[successfullyLocked.size()]));
            return false;
        }
        return true;
    }

    @Override
    public boolean unlock(String... names) {
        boolean success = true;
        try {
            for (String name : names) {
                LOGGER.debug("Attempting to unlock {0}", name);
                ReentrantLock lock = locks.get(name);
                if (lock != null) {
                    lock.unlock();
                    if (lock.isLocked()) {
                        LOGGER.debug("Unlock {0} failed. Lock is held by someone else...", name);
                        success = false;
                    }
                    LOGGER.debug("{0} unlocked", name);
                }
            }
            return success;
        } catch (Throwable t) {
            LOGGER.debug(t, "Unexpected exception while attempting to unlock");
            return false;
        }
    }
}
