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

import java.util.UUID;
import java.util.concurrent.locks.Lock;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.connector.base.MapTransaction;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.Path.Segment;

/**
 * 
 */
@NotThreadSafe
public class InMemoryTransaction extends MapTransaction<InMemoryNode, InMemoryWorkspace> {

    private final InMemoryRepository repository;
    private final Lock lock;

    protected InMemoryTransaction( ExecutionContext context,
                                   InMemoryRepository repository,
                                   UUID rootNodeUuid,
                                   Lock lock ) {
        super(context, repository, rootNodeUuid);
        this.repository = repository;
        this.lock = lock;
        assert this.lock != null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.Transaction#getWorkspace(java.lang.String,
     *      org.modeshape.graph.connector.base.Workspace)
     */
    public InMemoryWorkspace getWorkspace( String name,
                                           InMemoryWorkspace originalToClone ) {
        if (originalToClone != null) {
            return new InMemoryWorkspace(name, originalToClone);
        }
        return new InMemoryWorkspace(name, new InMemoryNode(repository.getRootNodeUuid()));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.Transaction#destroyWorkspace(org.modeshape.graph.connector.base.Workspace)
     */
    public boolean destroyWorkspace( InMemoryWorkspace workspace ) {
        // The InMemoryRepository is holding onto the Workspace objects for us and will be cleaned up properly,
        // so we can just return true here
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.MapTransaction#createNode(java.util.UUID,
     *      org.modeshape.graph.property.Path.Segment, java.util.UUID, java.lang.Iterable)
     */
    @Override
    protected InMemoryNode createNode( UUID uuid,
                                       Segment name,
                                       UUID parentUuid,
                                       Iterable<Property> properties ) {
        return new InMemoryNode(uuid, name, parentUuid, properties, null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.Transaction#commit()
     */
    @Override
    public void commit() {
        try {
            super.commit();
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.Transaction#rollback()
     */
    @Override
    public void rollback() {
        try {
            super.rollback();
        } finally {
            this.lock.unlock();
        }
    }

}
