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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.ItemExistsException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.observation.ObservationManager;
import javax.jcr.query.QueryManager;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import org.modeshape.jcr.RepositoryStatistics.ValueMetric;
import org.modeshape.jcr.core.ExecutionContext;
import org.xml.sax.ContentHandler;

/**
 * 
 */
class JcrWorkspace implements Workspace {

    private final JcrSession session;
    private final JcrNodeTypeManager nodeTypeManager;
    private final JcrLockManager lockManager;
    private final JcrNamespaceRegistry workspaceRegistry;
    private final JcrVersionManager versionManager;

    JcrWorkspace( JcrSession session ) {
        this.session = session;
        JcrRepository repository = session.repository();
        this.nodeTypeManager = new JcrNodeTypeManager(session, repository.nodeTypeManager());
        this.workspaceRegistry = new JcrNamespaceRegistry(repository.persistentRegistry(), this.session);
        this.lockManager = new JcrLockManager(session, repository.lockManager());
        this.versionManager = new JcrVersionManager(session);
    }

    final JcrNodeTypeManager nodeTypeManager() {
        return nodeTypeManager;
    }

    final JcrRepository repository() {
        return session.repository();
    }

    final JcrVersionManager versionManager() {
        return versionManager;
    }

    final ExecutionContext context() {
        return session.context();
    }

    @Override
    public Session getSession() {
        return session;
    }

    @Override
    public String getName() {
        return session.workspaceName();
    }

    @Override
    public void copy( String srcAbsPath,
                      String destAbsPath )
        throws ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException,
        LockException, RepositoryException {
        session.checkLive();
    }

    @Override
    public void copy( String srcWorkspace,
                      String srcAbsPath,
                      String destAbsPath )
        throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException,
        PathNotFoundException, ItemExistsException, LockException, RepositoryException {
        session.checkLive();
    }

    @Override
    public void clone( String srcWorkspace,
                       String srcAbsPath,
                       String destAbsPath,
                       boolean removeExisting )
        throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException,
        PathNotFoundException, ItemExistsException, LockException, RepositoryException {
        session.checkLive();
    }

    @Override
    public void move( String srcAbsPath,
                      String destAbsPath )
        throws ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException,
        LockException, RepositoryException {
        session.checkLive();

        // Create a new JCR session, perform the move, and then save the session ...
        JcrSession session = this.session.spawnSession(false);
        try {
            session.move(srcAbsPath, destAbsPath);
            session.save();
        } finally {
            session.logout();
        }
    }

    @Override
    public void restore( Version[] versions,
                         boolean removeExisting )
        throws ItemExistsException, UnsupportedRepositoryOperationException, VersionException, LockException,
        InvalidItemStateException, RepositoryException {
        session.checkLive();
    }

    @Override
    public JcrLockManager getLockManager() throws UnsupportedRepositoryOperationException, RepositoryException {
        session.checkLive();
        return lockManager;
    }

    @Override
    public QueryManager getQueryManager() throws RepositoryException {
        session.checkLive();
        // TODO: Query
        return null;
    }

    @Override
    public javax.jcr.NamespaceRegistry getNamespaceRegistry() throws RepositoryException {
        session.checkLive();
        return workspaceRegistry;
    }

    @Override
    public NodeTypeManager getNodeTypeManager() throws RepositoryException {
        session.checkLive();
        return nodeTypeManager;
    }

    @Override
    public ObservationManager getObservationManager() throws UnsupportedRepositoryOperationException, RepositoryException {
        session.checkLive();
        // TODO: Observation
        return null;
    }

    @Override
    public JcrVersionManager getVersionManager() throws UnsupportedRepositoryOperationException, RepositoryException {
        session.checkLive();
        return versionManager;
    }

    @Override
    public ContentHandler getImportContentHandler( String parentAbsPath,
                                                   int uuidBehavior )
        throws PathNotFoundException, ConstraintViolationException, VersionException, LockException, AccessDeniedException,
        RepositoryException {
        session.checkLive();
        // TODO: Import/export
        return null;
    }

    @Override
    public void importXML( String parentAbsPath,
                           InputStream in,
                           int uuidBehavior )
        throws IOException, VersionException, PathNotFoundException, ItemExistsException, ConstraintViolationException,
        InvalidSerializedDataException, LockException, AccessDeniedException, RepositoryException {
        session.checkLive();
        // TODO: Import/export
        throw new IOException();
    }

    @Override
    public String[] getAccessibleWorkspaceNames() throws RepositoryException {
        session.checkLive();
        // Make a copy, since the size may change before we iterate over it ...
        Set<String> names = new HashSet<String>(session.repository().repositoryCache().getWorkspaceNames());
        return names.toArray(new String[names.size()]);
    }

    @Override
    public void createWorkspace( String name )
        throws AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
        session.checkLive();
        try {
            JcrRepository repository = session.repository();
            repository.repositoryCache().createWorkspace(name);
            repository.statistics().increment(ValueMetric.WORKSPACE_COUNT);
        } catch (UnsupportedOperationException e) {
            throw new UnsupportedRepositoryOperationException(e.getMessage());
        }
    }

    @Override
    public void createWorkspace( String name,
                                 String srcWorkspace )
        throws AccessDeniedException, UnsupportedRepositoryOperationException, NoSuchWorkspaceException, RepositoryException {
        session.checkLive();
        createWorkspace(name);
        // TODO: copy the workspace contents ...
    }

    @Override
    public void deleteWorkspace( String name )
        throws AccessDeniedException, UnsupportedRepositoryOperationException, NoSuchWorkspaceException, RepositoryException {
        session.checkLive();
        try {
            JcrRepository repository = session.repository();
            if (!repository.repositoryCache().destroyWorkspace(name)) {
                throw new NoSuchWorkspaceException(JcrI18n.workspaceNotFound.text(name, getName()));
            }
            repository.statistics().decrement(ValueMetric.WORKSPACE_COUNT);
        } catch (UnsupportedOperationException e) {
            throw new UnsupportedRepositoryOperationException(e.getMessage());
        }
    }

}
