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
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Future;
import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.ItemExistsException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.observation.ObservationManager;
import javax.jcr.query.QueryManager;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.ImmediateFuture;
import org.modeshape.jcr.JcrContentHandler.EnclosingSAXException;
import org.modeshape.jcr.RepositoryStatistics.ValueMetric;
import org.modeshape.jcr.core.ExecutionContext;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * 
 */
class JcrWorkspace implements org.modeshape.jcr.api.Workspace {

    private final JcrSession session;
    private final String workspaceName;
    private final JcrNodeTypeManager nodeTypeManager;
    private final JcrLockManager lockManager;
    private final JcrNamespaceRegistry workspaceRegistry;
    private final JcrVersionManager versionManager;

    JcrWorkspace( JcrSession session,
                  String workspaceName ) {
        this.session = session;
        this.workspaceName = workspaceName;
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
    public final Session getSession() {
        return session;
    }

    @Override
    public final String getName() {
        return workspaceName;
    }

    @Override
    public void copy( String srcAbsPath,
                      String destAbsPath )
        throws ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException,
        LockException, RepositoryException {
        session.checkLive();
        // TODO: copy
        CheckArg.isNotEmpty(srcAbsPath, "srcAbsPath");
        CheckArg.isNotEmpty(destAbsPath, "destAbsPath");
    }

    @Override
    public void copy( String srcWorkspace,
                      String srcAbsPath,
                      String destAbsPath )
        throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException,
        PathNotFoundException, ItemExistsException, LockException, RepositoryException {
        session.checkLive();
        // TODO: copy
        CheckArg.isNotEmpty(srcWorkspace, "srcWorkspace");
        CheckArg.isNotEmpty(srcAbsPath, "srcAbsPath");
        CheckArg.isNotEmpty(destAbsPath, "destAbsPath");
    }

    @Override
    public void clone( String srcWorkspace,
                       String srcAbsPath,
                       String destAbsPath,
                       boolean removeExisting )
        throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException,
        PathNotFoundException, ItemExistsException, LockException, RepositoryException {
        session.checkLive();
        // TODO: clone
        CheckArg.isNotEmpty(srcWorkspace, "srcWorkspace");
        CheckArg.isNotEmpty(srcAbsPath, "srcAbsPath");
        CheckArg.isNotEmpty(destAbsPath, "destAbsPath");
    }

    @Override
    public void move( String srcAbsPath,
                      String destAbsPath )
        throws ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException,
        LockException, RepositoryException {
        session.checkLive();

        CheckArg.isNotEmpty(srcAbsPath, "srcAbsPath");
        CheckArg.isNotEmpty(destAbsPath, "destAbsPath");

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

    final JcrLockManager lockManager() {
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

        CheckArg.isNotNull(parentAbsPath, "parentAbsPath");
        session.checkLive();

        // Create a new session, since we don't want to mess with the current session and because we'll save right
        // when finished reading the document ...
        JcrSession session = this.session.spawnSession(false);
        boolean saveWhenFinished = true;

        // Find the parent path ...
        AbstractJcrNode parent = session.getNode(parentAbsPath);
        if (!parent.isCheckedOut()) {
            throw new VersionException(JcrI18n.nodeIsCheckedIn.text(parent.getPath()));
        }

        Repository repo = getSession().getRepository();
        boolean retainLifecycleInfo = repo.getDescriptorValue(Repository.OPTION_LIFECYCLE_SUPPORTED).getBoolean();
        boolean retainRetentionInfo = repo.getDescriptorValue(Repository.OPTION_RETENTION_SUPPORTED).getBoolean();
        return new JcrContentHandler(session, parent, uuidBehavior, saveWhenFinished, retainRetentionInfo, retainLifecycleInfo);
    }

    @Override
    public void importXML( String parentAbsPath,
                           InputStream in,
                           int uuidBehavior )
        throws IOException, VersionException, PathNotFoundException, ItemExistsException, ConstraintViolationException,
        InvalidSerializedDataException, LockException, AccessDeniedException, RepositoryException {
        CheckArg.isNotNull(parentAbsPath, "parentAbsPath");
        CheckArg.isNotNull(in, "in");
        session.checkLive();

        boolean error = false;
        try {
            XMLReader parser = XMLReaderFactory.createXMLReader();
            parser.setContentHandler(getImportContentHandler(parentAbsPath, uuidBehavior));
            parser.parse(new InputSource(in));
        } catch (EnclosingSAXException ese) {
            Exception cause = ese.getException();
            if (cause instanceof RepositoryException) {
                throw (RepositoryException)cause;
            }
            throw new RepositoryException(cause);
        } catch (SAXParseException se) {
            error = true;
            throw new InvalidSerializedDataException(se);
        } catch (SAXException se) {
            error = true;
            throw new RepositoryException(se);
        } finally {
            try {
                in.close();
            } catch (IOException t) {
                if (!error) throw t; // throw only if no error in outer try
            } catch (RuntimeException re) {
                if (!error) throw re; // throw only if no error in outer try
            }
        }
    }

    @Override
    public String[] getAccessibleWorkspaceNames() throws RepositoryException {
        session.checkLive();
        // Make a copy, since the size may change before we iterate over it ...
        Set<String> names = new HashSet<String>(session.repository().repositoryCache().getWorkspaceNames());

        // Remove any workspaces for which we don't have read access ...
        for (Iterator<String> iter = names.iterator(); iter.hasNext();) {
            try {
                session.checkPermission(iter.next(), null, ModeShapePermissions.READ);
            } catch (AccessDeniedException ace) {
                iter.remove();
            }
        }

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
        // TODO: Copy the workspace contents ...
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

    @Override
    public void reindex() throws RepositoryException {
        // TODO: Query
    }

    @Override
    public void reindex( String path ) throws RepositoryException {
        // TODO: Query
    }

    @Override
    public Future<Boolean> reindexAsync() throws RepositoryException {
        // TODO: Query

        // This is a bogus Future that always returns it's done ...
        return ImmediateFuture.create(Boolean.TRUE);
    }

    @Override
    public Future<Boolean> reindexAsync( String path ) throws RepositoryException {
        // TODO: Query

        // This is a bogus Future that always returns it's done ...
        return ImmediateFuture.create(Boolean.TRUE);
    }

}
