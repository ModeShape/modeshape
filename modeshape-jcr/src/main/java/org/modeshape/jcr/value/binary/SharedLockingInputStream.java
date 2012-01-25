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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.locks.Lock;
import org.modeshape.jcr.value.BinaryKey;

/**
 * A {@link InputStream} implementation around a file that creates a shared lock when reading the file, ensuring the file is not
 * changed by other writers in this or other JVM processes. The stream automatically closes itself and releases the lock when
 * {@link #close() closed explicitly} or if there are any errors or exceptions while reading.
 */
public final class SharedLockingInputStream extends InputStream {

    private final BinaryKey key;
    private final File file;
    private final NamedLocks lockManager;
    private InputStream stream;
    private Lock processLock;
    private FileLocks.WrappedLock fileLock;

    /**
     * Create a self-closing, (shared) locking {@link InputStream} to read the content of the supplied {@link File file}.
     * 
     * @param key the binary key; may not be null
     * @param file the file that is to be read; may not be null
     * @param lockManager the manager of the locks, from which a read lock is to be obtained; may be null if no read lock is
     *        needed
     */
    public SharedLockingInputStream( BinaryKey key,
                                     File file,
                                     NamedLocks lockManager ) {
        assert key != null;
        assert file != null;
        this.key = key;
        this.file = file;
        this.lockManager = lockManager;
    }

    protected void open() throws IOException {
        if (this.stream == null) {
            // At this point, we know the lock exists and we just need to wait until the write (if there is one) is done.
            // We do that by getting a read lock for the SHA-1 (to prevent other threads from modifying the file) ...
            if (lockManager != null) {
                processLock = lockManager.readLock(key.toString());
            }
            // Also get a shared file lock to prevent other processes from modifying the file ...
            this.fileLock = FileLocks.get().readLock(file);

            // Now create a buffered stream ...
            this.stream = new BufferedInputStream(new FileInputStream(file), AbstractBinaryStore.bestBufferSize(file.length()));
        }
    }

    @Override
    public int available() throws IOException {
        try {
            open();
            return stream.available();
        } catch (IOException e) {
            try {
                close();
            } catch (Throwable t) {
            }
            throw e;
        } catch (RuntimeException e) {
            try {
                close();
            } catch (Throwable t) {
            }
            throw e;
        }
    }

    @Override
    public void close() throws IOException {
        if (this.stream != null) {
            try {
                // this will release the lock automatically ...
                stream.close();
            } finally {
                stream = null;
                if (fileLock != null) {
                    try {
                        fileLock.unlock();
                    } finally {
                        fileLock = null;
                        if (processLock != null) {
                            try {
                                processLock.unlock();
                            } finally {
                                processLock = null;
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof File) return file.equals(obj);
        if (obj instanceof BinaryKey) return key.equals(obj);
        return false;
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public void mark( int readlimit ) {
        try {
            open();
            stream.mark(readlimit);
        } catch (IOException e) {
            try {
                close();
            } catch (Throwable t) {
            }
            throw new RuntimeException(e);
        } catch (RuntimeException e) {
            try {
                close();
            } catch (Throwable t) {
            }
            throw e;
        }
    }

    @Override
    public boolean markSupported() {
        try {
            open();
            return stream.markSupported();
        } catch (IOException e) {
            try {
                close();
            } catch (Throwable t) {
            }
            throw new RuntimeException(e);
        } catch (RuntimeException e) {
            try {
                close();
            } catch (Throwable t) {
            }
            throw e;
        }
    }

    @Override
    public int read( byte[] b,
                     int off,
                     int len ) throws IOException {
        try {
            open();
            int result = stream.read(b, off, len);
            if (result == -1) {
                // the end of the stream has been reached ...
                close();
            }
            return result;
        } catch (IOException e) {
            try {
                close();
            } catch (Throwable t) {
            }
            throw e;
        } catch (RuntimeException e) {
            try {
                close();
            } catch (Throwable t) {
            }
            throw e;
        }
    }

    @Override
    public int read( byte[] b ) throws IOException {
        try {
            open();
            int result = stream.read(b);
            if (result == -1) {
                // the end of the stream has been reached ...
                close();
            }
            return result;
        } catch (IOException e) {
            try {
                close();
            } catch (Throwable t) {
            }
            throw e;
        } catch (RuntimeException e) {
            try {
                close();
            } catch (Throwable t) {
            }
            throw e;
        }
    }

    @Override
    public int read() throws IOException {
        try {
            open();
            int result = stream.read();
            if (result == -1) {
                // the end of the stream has been reached ...
                close();
            }
            return result;
        } catch (IOException e) {
            try {
                close();
            } catch (Throwable t) {
            }
            throw e;
        } catch (RuntimeException e) {
            try {
                close();
            } catch (Throwable t) {
            }
            throw e;
        }
    }

    @Override
    public void reset() throws IOException {
        if (stream != null) {
            try {
                stream.reset();
            } catch (IOException e) {
                try {
                    close();
                } catch (Throwable t) {
                }
                throw e;
            } catch (RuntimeException e) {
                try {
                    close();
                } catch (Throwable t) {
                }
                throw e;
            }
        }
    }

    @Override
    public long skip( long n ) throws IOException {
        try {
            open();
            return stream.skip(n);
        } catch (IOException e) {
            try {
                close();
            } catch (Throwable t) {
            }
            throw e;
        } catch (RuntimeException e) {
            try {
                close();
            } catch (Throwable t) {
            }
            throw e;
        }
    }

    @Override
    public String toString() {
        return key.toString();
    }

}
