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
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.connector.RepositorySourceException;
import org.modeshape.graph.connector.base.Repository;
import org.modeshape.graph.connector.base.Transaction;
import org.modeshape.graph.request.InvalidWorkspaceException;
import org.modeshape.graph.request.CreateWorkspaceRequest.CreateConflictBehavior;

/**
 * The representation of a disk-based repository and its content.
 */
@ThreadSafe
public class DiskRepository extends Repository<DiskNode, DiskWorkspace> {

    private final File repositoryRoot;
    private File largeValuesRoot;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Set<String> predefinedWorkspaceNames;
    private final DiskSource diskSource;

    public DiskRepository( DiskSource source ) {
        super(source);

        this.diskSource = source;

        repositoryRoot = new File(source.getRepositoryRootPath());
        if (!repositoryRoot.exists()) repositoryRoot.mkdirs();
        assert repositoryRoot.exists();

        largeValuesRoot = new File(repositoryRoot, source.getLargeValuePath());
        if (!largeValuesRoot.exists()) largeValuesRoot.mkdirs();
        assert largeValuesRoot.exists();

        Set<String> workspaceNames = new HashSet<String>();
        for (String workspaceName : source.getPredefinedWorkspaceNames()) {
            workspaceNames.add(workspaceName);
        }
        this.predefinedWorkspaceNames = Collections.unmodifiableSet(workspaceNames);
        initialize();
    }

    @Override
    protected void initialize() {
        super.initialize();

        Transaction<DiskNode, DiskWorkspace> txn = startTransaction(context, false);

        // Discover any existing workspaces
        if (source.isCreatingWorkspacesAllowed()) {
            try {
                for (File file : repositoryRoot.listFiles()) {
                    if (!file.isDirectory()) continue;
                    String workspaceName = file.getName();

                    if (largeValuesRoot != null && largeValuesRoot.getCanonicalPath().startsWith(file.getCanonicalPath())) continue;

                    DiskWorkspace workspace = this.createWorkspace(txn, workspaceName, CreateConflictBehavior.DO_NOT_CREATE, null);
                    if (workspace != null) {
                        assert workspace.getRootNode() != null;
                    }

                }
            } catch (IOException ioe) {
                // This can really only come from the getCanonicalPath() calls, so this would be surprising
                txn.rollback();
                throw new RepositorySourceException(diskSource.getName(), ioe);
            }
        }

        // Then build any missing predefined workspaces
        for (String workspaceName : predefinedWorkspaceNames) {
            this.createWorkspace(txn, workspaceName, CreateConflictBehavior.DO_NOT_CREATE, null);
        }

        txn.commit();

    }

    @Override
    public DiskWorkspace createWorkspace( Transaction<DiskNode, DiskWorkspace> txn,
                                          String name,
                                          CreateConflictBehavior existingWorkspaceBehavior,
                                          String nameOfWorkspaceToClone ) throws InvalidWorkspaceException {
        DiskWorkspace workspace = super.createWorkspace(txn, name, existingWorkspaceBehavior, nameOfWorkspaceToClone);

        if (workspace != null && workspace.getRootNode() == null) {
            workspace.putNode(new DiskNode(diskSource.getRootNodeUuidObject()));
        }

        return workspace;
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
        final Lock lock = readonly ? this.lock.readLock() : this.lock.writeLock();
        lock.lock();
        return new DiskTransaction(context, this, getRootNodeUuid(), lock);
    }

    DiskSource diskSource() {
        return this.diskSource;
    }

    File largeValuesRoot() {
        return this.largeValuesRoot;
    }
}
