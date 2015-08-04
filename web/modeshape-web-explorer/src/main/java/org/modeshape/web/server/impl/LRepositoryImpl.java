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
package org.modeshape.web.server.impl;

import java.io.File;
import java.util.HashMap;
import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.servlet.ServletContext;
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.api.RepositoryManager;
import org.modeshape.web.shared.RemoteException;
import org.modeshape.web.server.LRepository;
import org.modeshape.web.shared.BackupParams;
import org.modeshape.web.shared.RestoreParams;

/**
 * @author kulikov
 */
public class LRepositoryImpl implements LRepository {
    private Credentials creds;
    private JcrRepository repository;
    
    //server's env
    private transient ServletContext context;
    private transient File tempDir;
    
    private HashMap<String, Session> sessions = new HashMap<>();
    private String[] workspaces;
    
    private final static Logger logger = Logger.getLogger(LRepositoryImpl.class);

    public LRepositoryImpl( ServletContext context, JcrRepository repository,
                            Credentials creds ) throws LoginException, RepositoryException {
        this.context = context;
        this.creds = creds;
        assert repository != null;
        this.repository = repository;
        
        logger.debug("Logging to repository " + repository + " as " + creds);
        
        Session session = creds != null ? repository.login(creds) : repository.login();
        sessions.put(session.getWorkspace().getName(), session);
        
        workspaces = session.getWorkspace().getAccessibleWorkspaceNames();
        logger.debug("[" + this.repository.getName() + "] available workspaces " + wsnames());
        
        tempDir = (File) context.getAttribute("javax.servlet.context.tempdir");
    }

    @Override
    public String name() {
        return repository.getName();
    }

    @Override
    public String[] getWorkspaces() {
        logger.debug("[" + this.repository.getName() + "] Requested workspaces " + wsnames());
        return workspaces;
    }

    private String wsnames() {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        for (int i = 0; i < workspaces.length - 1; i++) {
            builder.append(workspaces[i]);
            builder.append(",");
        }
        builder.append(workspaces[workspaces.length - 1]);
        builder.append("}");
        return builder.toString();
    }

    @Override
    public Session session( String workspace ) throws RemoteException {
        if (sessions.containsKey(workspace)) {
            logger.debug("[" + this.repository.getName() + "] has already session to " + workspace);
            return sessions.get(workspace);
        }

        try {
            logger.debug("[" + this.repository.getName() + "] has not yet session to " + workspace);
            Session session = creds != null ? repository.login(creds, workspace) : repository.login(workspace);
            sessions.put(workspace, session);
            return session;
        } catch (RepositoryException e) {
            throw new RemoteException(e.getMessage());
        }
    }

    private Session session() {
        return sessions.values().iterator().next();
    }

    @Override
    public Repository repository() {
        return repository;
    }

    @Override
    public void backup( String name, BackupParams options ) throws RemoteException {
        try {
            File dir = new File(tempDir.getAbsolutePath() + File.separator + name);
            RepositoryManager mgr = ((org.modeshape.jcr.api.Session)session()).getWorkspace().getRepositoryManager();
            mgr.backupRepository(dir, new BackupUsrOptions(options));
        } catch (Exception e) {
            throw new RemoteException(e.getMessage());
        }
    }

    @Override
    public void restore( String name, RestoreParams options ) throws RemoteException {
        try {
            File dir = new File(tempDir.getAbsolutePath() + File.separator + name);
            RepositoryManager mgr = ((org.modeshape.jcr.api.Session)session()).getWorkspace().getRepositoryManager();
            mgr.restoreRepository(dir, new RestoreUsrOptions(options));
            
            //after restore process session will be closed so we need to clean up
            //sessions to avoid access of invalid session.
            sessions.clear();
        } catch (Exception e) {
            throw new RemoteException(e.getMessage());
        }
    }

    public void importXML( String workspace ) throws RemoteException {
        try {
            Workspace ws = ((org.modeshape.jcr.api.Session)session(workspace)).getWorkspace();
            ws.importXML(workspace, null, 0);
        } catch (Exception e) {
            throw new RemoteException(e.getMessage());
        }
    }

}
