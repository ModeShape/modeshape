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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.common.text.FilenameEncoder;
import org.modeshape.common.text.TextEncoder;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.connector.base.MapTransaction;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.Path.Segment;
import org.modeshape.graph.request.InvalidWorkspaceException;

/**
 * 
 */
@NotThreadSafe
public class DiskTransaction extends MapTransaction<DiskNode, DiskWorkspace> {

    private static final TextEncoder FILE_ENCODER = new FilenameEncoder();

    private final DiskRepository repository;
    private final Lock lock;

    protected DiskTransaction( ExecutionContext context,
                                     DiskRepository repository,
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
    @Override
    public DiskWorkspace getWorkspace( String name,
                                             DiskWorkspace originalToClone ) {
        File workspaceRoot = workspaceRootFor(name);

        if (!workspaceRoot.exists()) {
            if (!workspaceRoot.mkdir()) {
                String msg = DiskConnectorI18n.unableToCreateWorkspace.text(name, repository.getSourceName());
                throw new InvalidWorkspaceException(msg);
            }
        }
        if (originalToClone != null) {
            return new DiskWorkspace(name, workspaceRoot, originalToClone);
        }
        return new DiskWorkspace(name, workspaceRoot, new DiskNode(repository.getRootNodeUuid()));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.Transaction#destroyWorkspace(org.modeshape.graph.connector.base.Workspace)
     */
    @Override
    public boolean destroyWorkspace( DiskWorkspace workspace ) {
        if (!getRepository().destroyWorkspace(workspace.getName())) return false;

        workspace.destroy();
        return true;
    }

    File workspaceRootFor( String workspaceName ) {
        String encodedName = FILE_ENCODER.encode(workspaceName);
        File repositoryRoot = repository.getRepositoryRoot();
        return new File(repositoryRoot, encodedName);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.MapTransaction#createNode(java.util.UUID,
     *      org.modeshape.graph.property.Path.Segment, java.util.UUID, java.lang.Iterable)
     */
    @Override
    protected DiskNode createNode( UUID uuid,
                                         Segment name,
                                         UUID parentUuid,
                                         Iterable<Property> properties ) {
        return new DiskNode(uuid, name, parentUuid, properties, null);
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
