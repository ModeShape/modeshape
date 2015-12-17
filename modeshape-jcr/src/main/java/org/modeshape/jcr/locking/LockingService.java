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
     */
     boolean tryLock(long time, TimeUnit unit, String... names);

    /**
     * Attempts to acquire a series of locks immediately.
     * <p>
     * NOTE: Implementors must make sure that either all the locks are obtained, or none. Otherwise possible deadlocks can occur.
     * </p>
     * 
     * @param names the names of the locks to acquire
     * @return {@code true} if *all* the locks were successfully acquired, {@code false} otherwise
     */
    default boolean tryLock(String... names) {
        return tryLock(0, TimeUnit.MILLISECONDS, names);
    }

    /**
     * Unlocks a series of locks. If a lock cannot be released, it's up to the implementor to decide whether this method 
     * returns immediately or attempts to continue with the other locks. 
     *
     * @param names the names of the locks to unlock
     * @return {@code true} if all the locks were successfully unlocked, or {@code false} if at least one lock could not be unlocked.
     */
    boolean unlock(String... names);
}
