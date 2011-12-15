/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.jcr.value.binary;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.modeshape.common.annotation.GuardedBy;

/**
 * A utility class that represents read and write lock for files, which internally uses {@link FileLock} to coordinate file locks
 * with other processes. This class maintains a single file lock per file, since multiple concurrent {@link FileLock}s on the same
 * file are not allowed by the JVM. It also uses reference counts to remove locks only when a named lock is no longer being used.
 */
public final class FileLocks {

    private static final FileLocks INSTANCE = new FileLocks();

    /**
     * Obtain the singleton instance for this virtual machine.
     * 
     * @return the file lock manager; never null
     */
    public static FileLocks get() {
        return INSTANCE;
    }

    private final Lock masterLock = new ReentrantLock();
    private final Map<String, LockHolder> locks = new HashMap<String, LockHolder>();

    private FileLocks() {
        // Prevent others from instantiating
    }

    /**
     * Obtain a write lock for the supplied file. When this method returns, the current thread will have obtained the lock.
     * Therefore, there is no need to call any of the lock methods (e.g., {@link Lock#lock()}, {@link Lock#lockInterruptibly()},
     * {@link Lock#tryLock()} or {@link Lock#tryLock(long, TimeUnit)}), as those methods will immediately return.
     * 
     * @param file the file to be locked; may not be null
     * @return the lock held by the current thread; never null
     * @throws IOException if there is a problem obtaining the file lock
     */
    public Lock writeLock( File file ) throws IOException {
        return lock(file, true);
    }

    /**
     * Obtain a read lock for the supplied file. When this method returns, the current thread will have obtained the lock.
     * Therefore, there is no need to call any of the lock methods (e.g., {@link Lock#lock()}, {@link Lock#lockInterruptibly()},
     * {@link Lock#tryLock()} or {@link Lock#tryLock(long, TimeUnit)}), as those methods will immediately return.
     * 
     * @param file the file to be locked; may not be null
     * @return the lock held by the current thread; never null
     * @throws IOException if there is a problem obtaining the file lock
     */
    public Lock readLock( File file ) throws IOException {
        return lock(file, false);
    }

    protected final Lock lock( File file,
                               boolean writeLock ) throws IOException {
        LockHolder holder = null;
        try {
            masterLock.lock();
            // Look for a lock with the supplied name ...
            holder = locks.get(file.getAbsolutePath());
            if (holder == null) {
                // Create a new named lock, which wraps and obtains the actual lock ...
                holder = new LockHolder(file);
                // Now store the wrapper in the map ...
                locks.put(file.getAbsolutePath(), holder);
                // Obtain and return the read or write lock (which we just created and nobody else can even see yet) ...
                return holder.lock(writeLock);
            }
            // Otherwise we found the lock and just need to increment the counter within the 'masterLock' scope ...
            holder.incrementReferenceCount();
        } finally {
            masterLock.unlock();
        }

        // Now be sure to obtain the lock (outside of the 'masterLock' scope) ...
        return holder.lock(writeLock);
    }

    protected void unlock( LockHolder holder,
                           Lock rawLock ) {
        try {
            masterLock.lock();
            try {
                // Decrement the counter ...
                if (holder.decrementReferenceCount() == 0) {
                    // This was the last lock holder, so remove it from the map ...
                    locks.remove(holder.file.getAbsolutePath());
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
        protected final LockHolder holder;
        protected final Lock actualLock;

        protected WrappedLock( LockHolder namedLock,
                               Lock lock ) {
            this.holder = namedLock;
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
            holder.unlock(this.actualLock);
        }
    }

    protected final class LockHolder {
        protected final File file;
        protected final Lock readLock;
        protected final Lock writeLock;
        private final ReadWriteLock lock = new ReentrantReadWriteLock();
        private final AtomicInteger referenceCount = new AtomicInteger(1);
        private final Lock fileLockLock = new ReentrantLock();
        @GuardedBy( "fileLockLock" )
        private FileLock fileLock;
        @GuardedBy( "fileLockLock" )
        private int lockedReaders = 0;

        protected LockHolder( File file ) {
            this.file = file;
            this.readLock = new WrappedLock(this, lock.readLock());
            this.writeLock = new WrappedLock(this, lock.writeLock());
        }

        protected void incrementReferenceCount() {
            referenceCount.incrementAndGet();
        }

        protected int decrementReferenceCount() {
            return referenceCount.decrementAndGet();
        }

        protected Lock lock( boolean write ) throws IOException {
            if (write) {
                this.lock.writeLock().lock();
                assert this.fileLock == null;
                assert this.lockedReaders == 0;
                // We have an exclusive write lock in this VM, so we can safely create the file lock ...
                RandomAccessFile raf = new RandomAccessFile(file, "rw");
                FileChannel channel = raf.getChannel();
                // Create a exclusive (non-shared) lock that does not allow other readers ...
                fileLock = channel.lock(0, Long.MAX_VALUE, false);

                // And return the wrapped write lock that will forward the unlock to us ...
                return writeLock;
            }

            // This is a read lock, so grab one of the read locks ...
            this.lock.readLock().lock();
            // We need to create a shared file lock for reading, but we only want to do this once for the first read lock ...
            try {
                fileLockLock.lock();

                if (fileLock == null) {
                    assert this.lockedReaders == 0;

                    // We have an exclusive write lock in this VM, so we can safely create the file lock ...
                    RandomAccessFile raf = new RandomAccessFile(file, "r");
                    FileChannel channel = raf.getChannel();
                    // Create a shared lock that allows other readers ...
                    fileLock = channel.lock(0, Long.MAX_VALUE, true);
                    lockedReaders++;
                } else {
                    assert this.lockedReaders != 0;
                }
            } finally {
                fileLockLock.unlock();
            }
            return readLock;
        }

        protected void unlock( Lock lock ) {
            boolean readLock = lock == this.lock.readLock();
            try {
                fileLockLock.lock();
                boolean unlock = true;
                if (readLock) {
                    --lockedReaders;
                    if (lockedReaders > 0) unlock = false;
                }
                // First get rid of the file lock ...
                if (unlock && fileLock != null && fileLock.channel().isOpen()) {
                    try {
                        fileLock.release();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } finally {
                        fileLock = null;
                    }
                }
            } finally {
                try {
                    fileLockLock.unlock();
                } finally {
                    FileLocks.this.unlock(this, lock);
                }
            }
        }

        @Override
        public String toString() {
            return "FileLockHolder \"" + file + "\" (" + referenceCount.get() + " referrers)";
        }
    }

}
