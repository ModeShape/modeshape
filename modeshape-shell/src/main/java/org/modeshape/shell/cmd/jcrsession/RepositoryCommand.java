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
package org.modeshape.shell.cmd.jcrsession;

import org.modeshape.shell.cmd.ShellCommand;
import java.io.File;
import org.modeshape.jcr.api.BackupOptions;
import org.modeshape.jcr.api.RepositoryManager;
import org.modeshape.jcr.api.RestoreOptions;
import org.modeshape.shell.ShellSession;
import org.modeshape.shell.ShellI18n;

/**
 *
 * @author kulikov
 */
public class RepositoryCommand extends ShellCommand {

    public RepositoryCommand() {
        super("repository");
        addChild(new BackupCommand());
        addChild(new RestoreCommand());
    }
    
    private class BackupCommand extends ShellCommand {

        public BackupCommand() {
            super("backup", ShellI18n.repositoryBackupHelp);
        }
        
        @Override
        public String exec(ShellSession session) throws Exception {
            String path = args(0);
            
            if (path == null) {
                return help();
            }
            
            BackupOptions opts = new BackupOptions() {
                
                @Override
                public boolean includeBinaries() {
                    return optionValue("--include-binaries") != null;
                }
                
                @Override
                public long documentsPerFile() {
                    String s = optionValue("--documents-per-file");
                    return s != null ? Long.parseLong(s) : super.documentsPerFile();
                }
            };
            
            org.modeshape.jcr.api.Session msSession = 
                    (org.modeshape.jcr.api.Session)session.jcrSession();
            RepositoryManager rm = msSession.getWorkspace().getRepositoryManager();
            rm.backupRepository(new File(path), opts);
            
            return SILENCE;
        }

    }
    
    private class RestoreCommand extends ShellCommand {

        public RestoreCommand() {
            super("restore", ShellI18n.repositoryRestoreHelp);
        }
        
        @Override
        public String exec(ShellSession session) throws Exception {
            String path = args(0);
            
            if (path == null) {
                return help();
            }
            
            RestoreOptions opts = new RestoreOptions() {
                
                @Override
                public boolean includeBinaries() {
                    return optionValue("--include-binaries") != null;
                }
                
                @Override
                public boolean reindexContentOnFinish() {
                    return optionValue("--reindex-on-finish") != null;
                }               
                
            };
            
            org.modeshape.jcr.api.Session msSession = 
                    (org.modeshape.jcr.api.Session)session.jcrSession();
            RepositoryManager rm = msSession.getWorkspace().getRepositoryManager();
            rm.restoreRepository(new File(path), opts);
            
            return SILENCE;
        }

    }
    
}
