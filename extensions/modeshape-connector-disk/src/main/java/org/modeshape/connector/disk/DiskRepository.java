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
package org.modeshape.connector.disk;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.util.Logger;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.connector.base.Repository;

/**
 * The representation of a disk-based repository and its content.
 */
@ThreadSafe
public class DiskRepository extends Repository<DiskNode, DiskWorkspace> {

    protected static final Logger LOGGER = Logger.getLogger(DiskRepository.class);

    private static final String LOCK_FILE_NAME = "lock";

    private final File repositoryRoot;
    protected final FileChannel lockFileChannel;
    protected AtomicInteger readLockCount = new AtomicInteger(0);
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Set<String> predefinedWorkspaceNames;

    public DiskRepository( DiskSource source ) {
        super(source);

        repositoryRoot = new File(source.getRepositoryRootPath());
        if (!repositoryRoot.exists()) repositoryRoot.mkdir();
        
        File repositoryLockFile = null;
        FileChannel lfc = null;

        if (source.isLockFileUsed()) {
            repositoryLockFile = new File(repositoryRoot, LOCK_FILE_NAME);
            try {
                if (!repositoryLockFile.exists()) {
                    FileOutputStream fos = null;
                    fos = new FileOutputStream(repositoryLockFile);
                    fos.write("modeshape".getBytes());
                    fos.close();
                }
                RandomAccessFile raf = new RandomAccessFile(repositoryLockFile, "rw");
                lfc = raf.getChannel();
            } catch (IOException ioe) {
                LOGGER.warn(ioe, DiskConnectorI18n.couldNotCreateLockFile, source.getName());
            }
        }

        this.lockFileChannel = lfc;

        Set<String> workspaceNames = new HashSet<String>();
        for (String workspaceName : source.getPredefinedWorkspaceNames()) {
            workspaceNames.add(workspaceName);
        }
        this.predefinedWorkspaceNames = Collections.unmodifiableSet(workspaceNames);
        initialize();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.Repository#getWorkspaceNames()
     */
    @Override
    public Set<String> getWorkspaceNames() {
        Set<String> names = new HashSet<String>(super.getWorkspaceNames());
        names.addAll(predefinedWorkspaceNames);
        return Collections.unmodifiableSet(names);
    }

    /**
     * Get the root of this repository
     * 
     * @return the root of this repository; never null
     */
    protected File getRepositoryRoot() {
        return repositoryRoot;
    }


    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.Repository#startTransaction(org.modeshape.graph.ExecutionContext, boolean)
     */
    @Override
    public DiskTransaction startTransaction( ExecutionContext context,
                                                   boolean readonly ) {

        DiskLock diskLock = readonly ? new DiskBackedReadLock(lock) : new DiskBackedWriteLock(lock);
        diskLock.lock();

        return new DiskTransaction(context, this, getRootNodeUuid(), diskLock);
    }

    interface DiskLock {
        void lock();

        void unlock();
    }

    class DiskBackedReadLock implements DiskLock {
        private final Lock lock;
        private FileLock fileLock = null;

        public DiskBackedReadLock( ReadWriteLock lock ) {
            super();
            this.lock = lock.readLock();
        }

        public void lock() {
            this.lock.lock();
            /*
             * FileLocks are held on behalf of the entire JVM and are not reentrant (at least on OS X), so we
             * need to track how many open read locks exist within this JVM.  If anyone knows a good Java implementation
             * of a counting semaphore, that could be used instead.
             */
            synchronized (DiskRepository.this) {
                int count = readLockCount.get();
                assert count >= 0;

                if (lockFileChannel != null && count == 0) {
                    try {
                        fileLock = lockFileChannel.lock(0, 1, true);
                    } catch (IOException ioe) {
                        LOGGER.warn(ioe, DiskConnectorI18n.problemAcquiringFileLock, getSourceName());
                    }
                }
                readLockCount.getAndIncrement();
            }
        }

        public void unlock() {
            synchronized (DiskRepository.this) {
                int count = readLockCount.getAndDecrement();
                assert count >= 0;

                if (fileLock != null && readLockCount.get() == 0) {
                    try {
                        fileLock.release();
                    } catch (IOException ioe) {
                        LOGGER.warn(ioe, DiskConnectorI18n.problemReleasingFileLock, getSourceName());
                    }
                }
            }
            lock.unlock();
        }
    }

    class DiskBackedWriteLock implements DiskLock {
        private final Lock lock;
        private FileLock fileLock;

        public DiskBackedWriteLock( ReadWriteLock lock ) {
            super();
            this.lock = lock.writeLock();
        }

        public void lock() {
            this.lock.lock();

            if (lockFileChannel != null) {
                try {
                    fileLock = lockFileChannel.lock(0, 1, false);
                } catch (IOException ioe) {
                    LOGGER.warn(ioe, DiskConnectorI18n.problemAcquiringFileLock, getSourceName());
                }
            }
        }

        public void unlock() {
            if (fileLock != null) {
                try {
                    fileLock.release();
                } catch (IOException ioe) {
                    LOGGER.warn(ioe, DiskConnectorI18n.problemReleasingFileLock, getSourceName());
                }
            }
            this.lock.unlock();
        }
    }

}
