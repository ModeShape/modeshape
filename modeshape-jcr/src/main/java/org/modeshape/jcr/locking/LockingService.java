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

/**
 * A service which allows the locking/unlocking of named locks as batch operations.
 * This is purely a locking abstraction and does not take into account other concepts such as transactions. 
 * <p>
 * For realizing the purpose of ACID and strong consistency, the {@link org.modeshape.jcr.cache.document.LocalDocumentStore}
 * is the place where locking and transactions are tied together.
 * </p>
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @since 5.0
 */
public interface LockingService {

    /**
     * Attempts to acquire a series of locks, waiting a maximum amount of time to obtain each lock.
     * <p>
     * NOTE: Implementors must make sure that either all the locks are obtained, or none. Otherwise possible deadlocks can occur.    
     * </p>
     * 
     * @param time an amount of time
     * @param unit a {@link java.util.concurrent.TimeUnit}; may not be null
     * @param names the names of the locks to acquire
     * @return {@code true} if *all* the locks were successfully acquired, {@code false} otherwise
     * @throws InterruptedException if this thread is interrupted while waiting to obtain the locks
     */
     boolean tryLock(long time, TimeUnit unit, String... names) throws InterruptedException;

    /**
     * Attempts to acquire a series of locks using the default lock timeout. If not timeout is explicitly set, this will not wait
     * to obtain the lock.
     * <p>
     * NOTE: Implementors must make sure that either all the locks are obtained, or none. Otherwise possible deadlocks can occur.
     * </p>
     * 
     * @param names the names of the locks to acquire
     * @return {@code true} if *all* the locks were successfully acquired, {@code false} otherwise
     * @throws InterruptedException if this thread is interrupted while waiting to obtain the locks
     */
     boolean tryLock(String... names) throws InterruptedException;

    /**
     * Unlocks a number of locks.  
     *
     * @param names the names of the locks to unlock
     * @return a {@code true} if all the locks were successfully unlocked and {@code false} if at least one lock could not be
     * released.
     */
    boolean unlock(String... names);

    /**
     * Shuts down this locking service.
     * 
     * @return {@code true} if the service was shutdown successfully or {@code false} if the service was already shutdown prior
     * to this call.
     */
    boolean shutdown();
}
