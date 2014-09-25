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
package org.modeshape.web.server.impl;

import java.io.File;
import java.util.HashMap;
import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.api.RepositoryManager;
import org.modeshape.web.client.RemoteException;
import org.modeshape.web.server.LRepository;

/**
 * @author kulikov
 */
public class LRepositoryImpl implements LRepository {
    private Credentials creds;
    private JcrRepository repository;
    private HashMap<String, Session> sessions = new HashMap<String, Session>();
    private String[] workspaces;
    private final static Logger logger = Logger.getLogger(LRepositoryImpl.class);

    public LRepositoryImpl( JcrRepository repository,
                            Credentials creds ) throws RepositoryException {
        this.creds = creds;
        assert repository != null;
        this.repository = repository;
        logger.debug("Logging to repository " + repository + " as " + creds);
        Session session = creds != null ? repository.login(creds) : repository.login();
        sessions.put(session.getWorkspace().getName(), session);
        workspaces = session.getWorkspace().getAccessibleWorkspaceNames();
        logger.debug("[" + this.repository.getName() + "] available workspaces " + wsnames());
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
    public void backup( String name ) throws RemoteException {
        try {
            File backupDir = new File(name);
            RepositoryManager mgr = ((org.modeshape.jcr.api.Session)session()).getWorkspace().getRepositoryManager();
            mgr.backupRepository(backupDir);
        } catch (Exception e) {
            throw new RemoteException(e.getMessage());
        }
    }

    @Override
    public void restore( String name ) throws RemoteException {
        try {
            File backupDir = new File(name);
            RepositoryManager mgr = ((org.modeshape.jcr.api.Session)session()).getWorkspace().getRepositoryManager();
            mgr.restoreRepository(backupDir);
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
