/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.jcr;

import java.io.File;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.jcr.RepositoryException;
import org.modeshape.jcr.api.BackupOptions;
import org.modeshape.jcr.api.Problems;
import org.modeshape.jcr.api.RepositoryManager;
import org.modeshape.jcr.api.RestoreOptions;
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
        return backupRepository(backupDirectory, BackupOptions.DEFAULT);
    }

    @Override
    public Problems backupRepository( File backupDirectory, BackupOptions backupOptions ) throws RepositoryException {
        session().checkPermission(Path.ROOT_PATH, ModeShapePermissions.BACKUP);
        return repository().runningState().backupService().backupRepository(backupDirectory, backupOptions);
    }

    @Override
    public Problems restoreRepository( File backupDirectory ) throws RepositoryException {
        return restoreRepository(backupDirectory, RestoreOptions.DEFAULT);
    }

    @Override
    public Problems restoreRepository( File backupDirectory, RestoreOptions options ) throws RepositoryException {
        session().checkPermission(Path.ROOT_PATH, ModeShapePermissions.RESTORE);
        return repository().runningState().backupService().restoreRepository(repository(), backupDirectory, options);
    }

}
