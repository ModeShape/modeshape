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
package org.modeshape.connector.infinispan;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import net.jcip.annotations.NotThreadSafe;
import org.infinispan.Cache;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.connector.base.MapTransaction;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.Path.Segment;
import org.modeshape.graph.request.InvalidWorkspaceException;

/**
 * 
 */
@NotThreadSafe
public class InfinispanTransaction extends MapTransaction<InfinispanNode, InfinispanWorkspace> {

    private final InfinispanRepository repository;
    private final Lock lock;

    protected InfinispanTransaction( ExecutionContext context,
                                     InfinispanRepository repository,
                                     UUID rootNodeUuid,
                                     Lock lock ) {
        super(context, repository, rootNodeUuid);
        this.repository = repository;
        this.lock = lock;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.Transaction#getWorkspaceNames()
     */
    @Override
    public Set<String> getWorkspaceNames() {
        return repository.getWorkspaceNames();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.Transaction#getWorkspace(java.lang.String,
     *      org.modeshape.graph.connector.base.Workspace)
     */
    public InfinispanWorkspace getWorkspace( String name,
                                             InfinispanWorkspace originalToClone ) {
        Cache<UUID, InfinispanNode> workspaceCache = repository.getCacheContainer().getCache(name);
        if (workspaceCache == null) {
            String msg = InfinispanConnectorI18n.unableToCreateWorkspace.text(name, repository.getSourceName());
            throw new InvalidWorkspaceException(msg);
        }
        if (originalToClone != null) {
            return new InfinispanWorkspace(name, workspaceCache, originalToClone);
        }
        return new InfinispanWorkspace(name, workspaceCache, new InfinispanNode(repository.getRootNodeUuid()));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.Transaction#destroyWorkspace(org.modeshape.graph.connector.base.Workspace)
     */
    public boolean destroyWorkspace( InfinispanWorkspace workspace ) {
        // Can't seem to tell Infinispan to destroy the cache, so perhaps we should destroy all the content ...
        workspace.destroy();
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.MapTransaction#createNode(java.util.UUID,
     *      org.modeshape.graph.property.Path.Segment, java.util.UUID, java.lang.Iterable)
     */
    @Override
    protected InfinispanNode createNode( UUID uuid,
                                         Segment name,
                                         UUID parentUuid,
                                         Iterable<Property> properties ) {
        return new InfinispanNode(uuid, name, parentUuid, properties, null);
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
            lock.unlock();
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
            lock.unlock();
        }
    }

}
