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
package org.modeshape.graph.connector.map;

import java.util.concurrent.locks.Lock;
import net.jcip.annotations.Immutable;

/**
 * A {@link MapRepositoryTransaction} based upon a {@link Lock}.
 */
@Immutable
public class LockBasedTransaction implements MapRepositoryTransaction {
    private final Lock lock;

    /**
     * Create a transaction given the lock. This construtor attempts to {@link Lock#lock() acquire the lock}.
     * 
     * @param lock the lock; may not be null
     */
    public LockBasedTransaction( Lock lock ) {
        assert lock != null;
        this.lock = lock;
        // Make sure we acquire the lock...
        this.lock.lock();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.map.MapRepositoryTransaction#commit()
     */
    public void commit() {
        this.lock.unlock();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.map.MapRepositoryTransaction#rollback()
     */
    public void rollback() {
        this.lock.unlock();
    }
}
