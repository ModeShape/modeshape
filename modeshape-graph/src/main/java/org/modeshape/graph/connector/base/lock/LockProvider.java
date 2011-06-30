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
package org.modeshape.graph.connector.base.lock;

import java.nio.channels.FileChannel;
import java.util.concurrent.locks.ReadWriteLock;


/**
 * A specific lock provider that creates {@link ModeShapeLock ModeShape locks} for a given underlying lock mechanism. This
 * interface provides a common access point for {@link ReadWriteLock JVM locks}, {@link FileChannel#lock(long, long, boolean)
 * file-based locks}, or any other locking mechanism.
 */
public interface LockProvider {

    /**
     * Acquires a shared lock for the given domain. The definition of the domain is variable and based on the {@link LockStrategy
     * lock strategy}. Implementations of this method must block until the lock is acquired.
     * 
     * @param domain the domain for which the lock should be acquired; may not be null
     * @return the lock for the given domain; never null
     */
    ModeShapeLock readLockFor(String domain);
    
    /**
     * Acquires an exclusive lock for the given domain. The definition of the domain is variable and based on the
     * {@link LockStrategy lock strategy}. Implementations of this method must block until the lock is acquired.
     * 
     * @param domain the domain for which the lock should be acquired; may not be null
     * @return the lock for the given domain; never null
     */
    ModeShapeLock writeLockFor(String domain);
    
}
