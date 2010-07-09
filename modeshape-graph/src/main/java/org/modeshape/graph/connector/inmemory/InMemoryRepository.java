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
package org.modeshape.graph.connector.inmemory;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.jcip.annotations.ThreadSafe;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.connector.base.Repository;

/**
 * The representation of an in-memory repository and its content.
 */
@ThreadSafe
public class InMemoryRepository extends Repository<InMemoryNode, InMemoryWorkspace> {

    protected final ReadWriteLock lock = new ReentrantReadWriteLock();

    public InMemoryRepository( InMemoryRepositorySource source ) {
        super(source);
        initialize();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.Repository#startTransaction(org.modeshape.graph.ExecutionContext, boolean)
     */
    @Override
    public InMemoryTransaction startTransaction( ExecutionContext context,
                                                 boolean readonly ) {
        final Lock lock = readonly ? this.lock.readLock() : this.lock.writeLock();
        lock.lock();
        return new InMemoryTransaction(context, this, getRootNodeUuid(), lock);
    }
}
