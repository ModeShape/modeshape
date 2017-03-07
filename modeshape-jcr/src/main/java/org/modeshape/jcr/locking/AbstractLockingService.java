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
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.CheckArg;

/**
 * Base class for {@link LockingService} implementations.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @since 5.0
 */
@ThreadSafe
public abstract class AbstractLockingService<T extends Lock> implements LockingService  {

    protected final Logger logger = Logger.getLogger(getClass());

    private final ConcurrentHashMap<String, T> locksByName = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> locksByWaiters = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final long lockTimeoutMillis;

    protected AbstractLockingService() {
       this(0);
    }
    
    protected AbstractLockingService(long lockTimeoutMillis) {
        CheckArg.isNonNegative(lockTimeoutMillis, "lockTimeoutMillis");
        this.lockTimeoutMillis = lockTimeoutMillis;
        this.running.compareAndSet(false, true);
    }

    @Override
    public boolean tryLock(String... names) throws InterruptedException {
        return tryLock(lockTimeoutMillis, TimeUnit.MILLISECONDS, names);
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit, String... names) throws InterruptedException {
        if (!running.get()) {
            throw new IllegalStateException("Service has been shut down");
        }
        Set<String> successfullyLocked = new LinkedHashSet<>();
        for (String name : names) {
            logger.debug("attempting to lock {0}", name);
            locksByWaiters.computeIfAbsent(name, lockName -> new AtomicInteger(0)).incrementAndGet();
            boolean success = false;
            T lock = null;
            try {
                lock = locksByName.computeIfAbsent(name, this::createLock);
                success = doLock(lock, time, unit);
            } catch (Exception e) {
                logger.debug(e, "unexpected exception while attempting to lock '{0}'", name);
            }
            // if lock acquisition was not successful (for whatever reason) revert the lock
            if (!success) {
                // decrement the waiter value for the lock we failed to get
                locksByWaiters.computeIfPresent(name, (lockName, atomicInteger) -> {
                    if (atomicInteger.decrementAndGet() == 0) {
                        // we couldn't get the lock, but no one is using it, meaning it just became unlocked so just remove it
                        locksByName.computeIfPresent(name, (internalLockName, internalLock) -> null);
                        return null;
                    }
                    return atomicInteger;
                });
                if (!successfullyLocked.isEmpty()) {
                    logger.debug("Unable to acquire lock on {0}. Reverting back the already obtained locks: {1}", name,
                                 successfullyLocked);
                    // and unlock all the rest of the locks...
                    unlock(successfullyLocked.toArray(new String[0]));
                }
                return false;
            }
            logger.debug("{0} locked successfully (ref {1})", name, lock);
            successfullyLocked.add(name);
        }
        return true;
    }
    
    @Override
    public boolean unlock(String... names) {
        if (!running.get()) {
            throw new IllegalStateException("Service has been shut down");
        }
        return Arrays.stream(names)
                     .map(this::unlock)
                     .allMatch(Boolean::booleanValue);
    }

    protected boolean unlock(String name) {
        logger.debug("attempting to unlock {0}", name);
        
        AtomicBoolean unlocked = new AtomicBoolean(false);
        locksByName.computeIfPresent(name, (key, lock) -> {
            AtomicInteger waiters = locksByWaiters.computeIfPresent(name, (lockName, atomicInteger) -> {
                if (releaseLock(lock)) {
                    logger.debug("{0} unlocked (ref {1})...", name, lock);
                    unlocked.compareAndSet(false, true);
                    if (atomicInteger.decrementAndGet() > 0) {
                        // there are still threads waiting on this, so don't remove it...
                        return atomicInteger;
                    } else {
                        // no one waiting on the this lock anymore
                        return null;
                    }
                }
                // the lock was not unlocked
                logger.debug("{0} failed to unlock (ref {1})...", name, lock);
                return atomicInteger;
            });
            if (waiters != null) {
                logger.debug("lock '{0}' is not currently locked but will be", name);
                return lock;
            } else {
                // The lock is not used, and returning null will remove it from the map ...
                logger.debug("lock '{0}' not used anymore; removing it from map", name);
                return null;
            }
        });
        return unlocked.get();
    }
    

    @Override
    public synchronized boolean shutdown() {
        if (!running.get()) {
            return false;
        }
        logger.debug("Shutting down locking service...");
        doShutdown();
        locksByName.clear();
        running.compareAndSet(true, false);
        return true;
    }

    protected void doShutdown() {
        locksByName.forEach((key, lock) -> {
            if (releaseLock(lock)) {
                logger.debug("{0} unlocked successfully ", key);
            } else {
                logger.debug("{0} cannot be released...", key);
            }
        });
    }

    protected boolean doLock(T lock, long time, TimeUnit timeUnit) throws InterruptedException {
        return time > 0 ? lock.tryLock(time, timeUnit) : lock.tryLock();
    }

    protected abstract T createLock(String name);
    
    protected abstract boolean releaseLock(T lock);
}
