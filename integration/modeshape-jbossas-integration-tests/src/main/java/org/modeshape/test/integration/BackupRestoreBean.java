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
package org.modeshape.test.integration;

import java.io.File;
import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import org.modeshape.jcr.api.Problems;
import org.modeshape.jcr.api.Repository;
import org.modeshape.jcr.api.RepositoryManager;

/**
 * Singleton EJB that performs backup/restore from with a startup method.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
@Singleton
@Startup
public class BackupRestoreBean extends RepositoryProvider {

    private boolean backupRestoreSuccessful = false;

    @PostConstruct
    @TransactionAttribute( TransactionAttributeType.NOT_SUPPORTED )
    public void run() throws Exception {
        final String path = System.getProperty("jboss.server.data.dir");
        if (path == null || path.trim().length() == 0) {
            throw new IllegalStateException("Cannot locate the jboss server dir");
        }
        final File backupDirectory = new File(path, "modeshape/sampleBackup");
        backupDirectory.mkdirs();

        Repository repository = (Repository)getRepositoryFromJndi("java:/jcr/sample");
        org.modeshape.jcr.api.Session session = repository.login();
        final RepositoryManager repoMgr = session.getWorkspace().getRepositoryManager();
        Problems problems = repoMgr.backupRepository(backupDirectory);
        if (problems.hasProblems()) {
            throw new IllegalStateException("Errors while backing up repository:" + problems.toString());
        }

        problems = session.getWorkspace().getRepositoryManager().restoreRepository(backupDirectory);
        if (problems.hasProblems()) {
            throw new IllegalStateException("Errors while backing up repository:" + problems.toString());
        }
        backupRestoreSuccessful = true;
    }

    public boolean isBackupRestoreSuccessful() {
        return backupRestoreSuccessful;
    }
}
