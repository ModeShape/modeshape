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
package org.modeshape.jcr.value.binary;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A utility class that represents a set of named locks, allowing callers to atomically obtain a lock with a given name. Locks are
 * released as normal. This class uses reference counts to remove locks only when a named lock is no longer being used.
 */
public class NamedLocks {

    private final Lock masterLock = new ReentrantLock();
    private final Map<String, NamedLock> locks = new HashMap<String, NamedLock>();

    /**
     * Obtain a write lock for the supplied name. When this method returns, the current thread will have obtained the lock.
     * Therefore, there is no need to call any of the lock methods (e.g., {@link Lock#lock()}, {@link Lock#lockInterruptibly()},
     * {@link Lock#tryLock()} or {@link Lock#tryLock(long, TimeUnit)}), as those methods will immediately return.
     * 
     * @param name the name of the lock
     * @return the lock held by the current thread; never null
     */
    public Lock writeLock( String name ) {
        return lock(name, true);
    }

    /**
     * Obtain a read lock for the supplied name. When this method returns, the current thread will have obtained the lock.
     * Therefore, there is no need to call any of the lock methods (e.g., {@link Lock#lock()}, {@link Lock#lockInterruptibly()},
     * {@link Lock#tryLock()} or {@link Lock#tryLock(long, TimeUnit)}), as those methods will immediately return.
     * 
     * @param name the name of the lock
     * @return the lock held by the current thread; never null
     */
    public Lock readLock( String name ) {
        return lock(name, false);
    }

    protected final Lock lock( String name,
                               boolean writeLock ) {
        NamedLock lock = null;
        try {
            masterLock.lock();
            // Look for a lock with the supplied name ...
            lock = locks.get(name);
            if (lock == null) {
                // Create a new named lock, which wraps and obtains the actual lock ...
                lock = new NamedLock(name);
                // Now store the wrapper in the map ...
                locks.put(name, lock);
                // Obtain and return the read or write lock (which we just created and nobody else can use yet) ...
                return lock.lock(writeLock);
            }
            // Otherwise we found the lock and just need to increment the counter within the 'masterLock' scope ...
            lock.incrementReferenceCount();
        } finally {
            masterLock.unlock();
        }

        // Now be sure to obtain the lock (outside of the 'masterLock' scope) ...
        return lock.lock(writeLock);
    }

    protected void unlock( NamedLock namedLock,
                           Lock rawLock ) {
        try {
            masterLock.lock();
            try {
                // Decrement the counter ...
                if (namedLock.decrementReferenceCount() == 0) {
                    // This was the last lock holder, so remove it from the map ...
                    locks.remove(namedLock.name);
                }
            } finally {
                // And always unlock the 'raw' (not wrapped) lock ...
                rawLock.unlock();
            }
        } finally {
            masterLock.unlock();
        }
    }

    /**
     * Get the number of named locks.
     * 
     * @return the number of named locks; never negative
     */
    public int size() {
        try {
            masterLock.lock();
            return locks.size();
        } finally {
            masterLock.unlock();
        }
    }

    protected static class WrappedLock implements Lock {
        protected final NamedLock namedLock;
        protected final Lock actualLock;

        protected WrappedLock( NamedLock namedLock,
                               Lock lock ) {
            this.namedLock = namedLock;
            this.actualLock = lock;
        }

        @Override
        public void lock() {
            // no need to do anything, as our lock is already held by this thread
        }

        @Override
        public void lockInterruptibly() {
            // no need to do anything, as our lock is already held by this thread
        }

        @Override
        public Condition newCondition() {
            // Returning the actual lock's condition shouldn't really affect our reference count, since the condition
            // can only be used when the lock is held
            return actualLock.newCondition();
        }

        @Override
        public boolean tryLock() {
            // no need to do anything, as our lock is already held by this thread
            return true;
        }

        @Override
        public boolean tryLock( long time,
                                TimeUnit unit ) {
            // no need to do anything, as our lock is already held by this thread
            return true;
        }

        @Override
        public void unlock() {
            namedLock.unlock(this.actualLock);
        }
    }

    protected final class NamedLock {
        protected final String name;
        protected final Lock readLock;
        protected final Lock writeLock;
        private final ReadWriteLock lock = new ReentrantReadWriteLock();
        private final AtomicInteger referenceCount = new AtomicInteger(1);

        protected NamedLock( String name ) {
            this.name = name;
            this.readLock = new WrappedLock(this, lock.readLock());
            this.writeLock = new WrappedLock(this, lock.writeLock());
        }

        protected void incrementReferenceCount() {
            referenceCount.incrementAndGet();
        }

        protected int decrementReferenceCount() {
            return referenceCount.decrementAndGet();
        }

        protected Lock lock( boolean write ) {
            if (write) {
                this.lock.writeLock().lock();
                return writeLock;
            }
            this.lock.readLock().lock();
            return readLock;
        }

        protected void unlock( Lock lock ) {
            NamedLocks.this.unlock(this, lock);
        }

        @Override
        public String toString() {
            return "NamedLock \"" + name + "\" (" + referenceCount.get() + " referrers)";
        }
    }

}
