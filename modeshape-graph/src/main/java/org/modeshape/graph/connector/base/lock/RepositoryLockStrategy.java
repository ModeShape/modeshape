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

import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.graph.connector.base.Repository;
import org.modeshape.graph.request.Request;

/**
 * A lock strategy that locks at the granularity of the {@link Repository repository}. That is, each repository can have at most
 * one request modifying any workspace in the repository at any given time, and while this request is writing, no other requests
 * can read or write from the repository. Multiple concurrent reads are supported.
 * <p/>
 * Although this class is thread-safe, it is important that each repository have its own instance of this class, as the
 * {@link LockProvider contained lock provider} currently keys locks by workspace name. Since more than one {@link Repository
 * repository} can <i>and will</i> have the same workspace name in a federated environment,
 */
@ThreadSafe
public class RepositoryLockStrategy implements LockStrategy {

    // Arbitrary non-null string
    private final String REPOSITORY = "modeshape";

    private final LockProvider provider = new JvmLockProvider();

    /**
     * {@inheritDoc}
     * 
     * @see LockStrategy#lock(Request)
     */
    public HeldLocks lock( Request request ) {
        if (request != null && request.isReadOnly()) {
            return new RepositoryHeldLocks(provider.readLockFor(REPOSITORY));
        }
        return new RepositoryHeldLocks(provider.writeLockFor(REPOSITORY));

    }

    class RepositoryHeldLocks implements HeldLocks {

        private final ModeShapeLock lock;

        RepositoryHeldLocks( ModeShapeLock lock ) {
            this.lock = lock;
            lock.lock();
        }

        public void release() {
            lock.unlock();
        }

    }

}
