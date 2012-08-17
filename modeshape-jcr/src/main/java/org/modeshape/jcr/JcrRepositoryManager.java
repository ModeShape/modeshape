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
package org.modeshape.jcr;

import java.io.File;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.jcr.RepositoryException;
import org.modeshape.jcr.api.Problems;
import org.modeshape.jcr.api.RepositoryManager;
import org.modeshape.jcr.value.Path;

public class JcrRepositoryManager implements RepositoryManager {
    private final Lock lock = new ReentrantLock();
    private final JcrWorkspace workspace;
    private JcrRepositoryMonitor monitor;

    protected JcrRepositoryManager( JcrWorkspace workspace ) {
        this.workspace = workspace;
    }

    @Override
    public JcrWorkspace getWorkspace() {
        return workspace;
    }

    private final JcrSession session() {
        return workspace.getSession();
    }

    private final JcrRepository repository() {
        return workspace.repository();
    }

    @Override
    public JcrRepositoryMonitor getRepositoryMonitor() throws RepositoryException {
        session().checkLive();
        return repositoryMonitor();
    }

    final JcrRepositoryMonitor repositoryMonitor() throws RepositoryException {
        if (monitor == null) {
            try {
                lock.lock();
                if (monitor == null) monitor = new JcrRepositoryMonitor(session());
            } finally {
                lock.unlock();
            }
        }
        return monitor;
    }

    @Override
    public Problems backupRepository( File backupDirectory ) throws RepositoryException {
        session().checkPermission(Path.ROOT_PATH, ModeShapePermissions.BACKUP);
        return repository().runningState().backupService().backupRepository(backupDirectory);
    }

    @Override
    public Problems restoreRepository( File backupDirectory ) throws RepositoryException {
        session().checkPermission(Path.ROOT_PATH, ModeShapePermissions.RESTORE);
        return repository().runningState().backupService().restoreRepository(repository(), backupDirectory);
    }
}
