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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.CheckArg;

/**
 * {@link LockingService} implementation that is used by ModeShape when running in non-clustered (local) mode.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @since 5.0
 */
@ThreadSafe
public class StandaloneLockingService implements LockingService {

    private static final Logger LOGGER = Logger.getLogger(StandaloneLockingService.class);

    private final ConcurrentHashMap<String, ReentrantLock> locksByName = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile long lockTimeoutMillis = 0;

    public StandaloneLockingService() {
        running.compareAndSet(false, true);
    }                                     

    @Override
    public boolean tryLock(String... names) {
        return tryLock(lockTimeoutMillis, TimeUnit.MILLISECONDS, names);
    }

    @Override
    public void setLockTimeout(long lockTimeoutMillis) {
        CheckArg.isNonNegative(lockTimeoutMillis, "lockTimeoutMillis");
        this.lockTimeoutMillis = lockTimeoutMillis;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit, String... names) {
        Set<ReentrantLock> successfullyLocked = new HashSet<>();
        for (String name : names) {
            LOGGER.debug("Attempting to lock {0}", name);
            ReentrantLock lock = null;
            boolean success = false;
            try {
                lock = locksByName.computeIfAbsent(name, (lockName) -> new ReentrantLock());
                success = time > 0 ? lock.tryLock(time, unit) : lock.tryLock();
            } catch (InterruptedException e) {
                LOGGER.debug("Thread {0} received interrupt request while waiting to acquire lock '{1}'", 
                             Thread.currentThread().getName(), name);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                LOGGER.debug(e, "Unexpected exception while attempting to lock '{0}'", name);
            }
            // if lock acquisition was not successful (for whatever reason) revert the lock
            if (!success) {
                LOGGER.debug("Unable to acquire lock on {0}. Reverting back the already obtained locks: {1}", name,
                             successfullyLocked);
                    successfullyLocked.stream().forEach(ReentrantLock::unlock);
                return false;
            }
            LOGGER.debug("{0} locked successfully", name);
            successfullyLocked.add(lock);
        }
        return true;
    }

    @Override
    public List<String> unlock(String... names) {
        return Arrays.stream(names)
                     .map(this::unlock)
                     .filter(Optional::isPresent)
                     .map(Optional::get)
                     .collect(Collectors.toList());
    }

    private Optional<String> unlock(String name) {
        LOGGER.debug("Attempting to unlock {0}", name);
        AtomicBoolean unlocked = new AtomicBoolean(false);
        locksByName.computeIfPresent(name, (key,lock)-> {
            if (!lock.isHeldByCurrentThread()) {
                LOGGER.debug("The thread '{0}' is attempting to unlock '{1}' which is held by another thread", 
                             Thread.currentThread().getName(), key);
                return lock;
            }
            // Try to unlock the lock ...
            // since these are reentrant, remove all the acquisitions by the calling thread
            while (lock.getHoldCount() > 0) {
                lock.unlock();
            }
            unlocked.set(true);
            LOGGER.debug("{0} unlocked...", name);
            if (lock.hasQueuedThreads()) {
                LOGGER.debug("Lock '{0}' is not currently locked but will be", name);
                return lock;
            }
            // The lock is not used, and returning null will remove it from the map ...
            return null;
        });
        return unlocked.get() ? Optional.empty() : Optional.of(name);
    }

    @Override
    public synchronized boolean shutdown() {
        if (!running.get()) {
            return false;
        }
        unlock(locksByName.keySet().toArray(new String[locksByName.size()]));
        locksByName.clear();
        running.compareAndSet(true, false);
        return true;
    }
}
