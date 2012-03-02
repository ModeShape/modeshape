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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.ItemExistsException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.observation.ObservationManager;
import javax.jcr.query.QueryManager;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.JcrContentHandler.EnclosingSAXException;
import org.modeshape.jcr.api.monitor.RepositoryMonitor;
import org.modeshape.jcr.api.monitor.ValueMetric;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.ValueFormatException;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * The ModeShape implementation of the {@link Workspace JCR Workspace}. This implementation is pretty lightweight, and only
 * instantiates the various components when needed.
 */
@ThreadSafe
class JcrWorkspace implements org.modeshape.jcr.api.Workspace {

    private final JcrSession session;
    private final String workspaceName;
    private final Lock lock = new ReentrantLock();
    private JcrNodeTypeManager nodeTypeManager;
    private JcrLockManager lockManager;
    private JcrNamespaceRegistry workspaceRegistry;
    private JcrVersionManager versionManager;
    private JcrQueryManager queryManager;
    private JcrRepositoryMonitor monitor;
    private JcrObservationManager observationManager;

    JcrWorkspace( JcrSession session,
                  String workspaceName ) {
        this.session = session;
        this.workspaceName = workspaceName;
    }

    final JcrRepository repository() {
        return session.repository();
    }

    final ExecutionContext context() {
        return session.context();
    }

    @Override
    public final JcrSession getSession() {
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
        return lockManager();
    }

    final JcrLockManager lockManager() {
        if (lockManager == null) {
            try {
                lock.lock();
                if (lockManager == null) lockManager = new JcrLockManager(session, repository().lockManager());
            } finally {
                lock.unlock();
            }
        }
        return lockManager;
    }

    @Override
    public QueryManager getQueryManager() throws RepositoryException {
        session.checkLive();
        if (this.queryManager == null) {
            try {
                lock.lock();
                if (queryManager == null) queryManager = new JcrQueryManager(session);
            } finally {
                lock.unlock();
            }
        }
        return queryManager;
    }

    @Override
    public javax.jcr.NamespaceRegistry getNamespaceRegistry() throws RepositoryException {
        session.checkLive();
        if (workspaceRegistry == null) {
            try {
                lock.lock();
                if (workspaceRegistry == null) {
                    workspaceRegistry = new JcrNamespaceRegistry(repository().persistentRegistry(), this.session);
                }
            } finally {
                lock.unlock();
            }
        }
        return workspaceRegistry;
    }

    @Override
    public JcrNodeTypeManager getNodeTypeManager() throws RepositoryException {
        session.checkLive();
        return nodeTypeManager();
    }

    final JcrNodeTypeManager nodeTypeManager() {
        if (nodeTypeManager == null) {
            try {
                lock.lock();
                if (nodeTypeManager == null) {
                    nodeTypeManager = new JcrNodeTypeManager(session, repository().nodeTypeManager());
                }
            } finally {
                lock.unlock();
            }
        }
        return nodeTypeManager;
    }

    @Override
    public ObservationManager getObservationManager() throws UnsupportedRepositoryOperationException, RepositoryException {
        session.checkLive();
        return observationManager();
    }

    final JcrObservationManager observationManager() {
        if (observationManager == null) {
            try {
                lock.lock();
                if (observationManager == null) {
                    observationManager = new JcrObservationManager(session, repository().repositoryCache());
                }
            } finally {
                lock.unlock();
            }
        }
        return observationManager;
    }

    @Override
    public JcrVersionManager getVersionManager() throws UnsupportedRepositoryOperationException, RepositoryException {
        session.checkLive();
        return versionManager();
    }

    final JcrVersionManager versionManager() {
        if (versionManager == null) {
            try {
                lock.lock();
                if (versionManager == null) versionManager = new JcrVersionManager(session);
            } finally {
                lock.unlock();
            }
        }
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
        // First check permissions ...
        session.checkPermission(workspaceName, Path.ROOT_PATH, ModeShapePermissions.INDEX_WORKSPACE);
        // Then reindex ...
        repository().runningState().queryManager().reindexContent(this);
    }

    @Override
    public void reindex( String pathStr ) throws RepositoryException {
        try {
            // First check permissions ...
            Path path = session.pathFactory().create(pathStr);
            session.checkPermission(workspaceName, path, ModeShapePermissions.INDEX_WORKSPACE);
            // Then reindex ...
            repository().runningState().queryManager().reindexContent(this, path, Integer.MAX_VALUE);
        } catch (ValueFormatException e) {
            throw new RepositoryException(e.getMessage());
        }
    }

    @Override
    public Future<Boolean> reindexAsync() throws RepositoryException {
        // First check permissions ...
        session.checkPermission(workspaceName, Path.ROOT_PATH, ModeShapePermissions.INDEX_WORKSPACE);
        // Then reindex ...
        return repository().runningState().queryManager().reindexContentAsync(this);
    }

    @Override
    public Future<Boolean> reindexAsync( String pathStr ) throws RepositoryException {
        try {
            // First check permissions ...
            Path path = session.pathFactory().create(pathStr);
            session.checkPermission(workspaceName, path, ModeShapePermissions.INDEX_WORKSPACE);
            // Then reindex ...
            return repository().runningState().queryManager().reindexContentAsync(this, path, Integer.MAX_VALUE);
        } catch (ValueFormatException e) {
            throw new RepositoryException(e.getMessage());
        }
    }

    @Override
    public RepositoryMonitor getRepositoryMonitor() throws RepositoryException {
        if (monitor == null) {
            try {
                lock.lock();
                if (monitor == null) {
                    monitor = new JcrRepositoryMonitor(session);
                }
            } finally {
                lock.unlock();
            }
        }
        return monitor;
    }

}
