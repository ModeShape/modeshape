/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.jcr;

import java.io.IOException;
import java.io.InputStream;
import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.ItemExistsException;
import javax.jcr.NamespaceRegistry;
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
import net.jcip.annotations.ThreadSafe;
import org.xml.sax.ContentHandler;

/**
 * @author Randall Hauch
 */
@ThreadSafe
public class DnaWorkspace implements Workspace {

    private final String name;
    private final DnaSession session;
    private final DnaRepository repository;

    /**
     * @param repository the repository instance that owns this workspace; may not be null
     * @param name the name of the workspace; may not be null
     * @param session the session that owns this workspace; may not be null
     */
    /* package */DnaWorkspace( DnaRepository repository, String name, DnaSession session ) {
        assert repository != null;
        assert session != null;
        assert name != null;
        this.repository = repository;
        this.session = session;
        this.name = name;
    }

    /**
     * {@inheritDoc}
     */
    public void clone( String srcWorkspace, String srcAbsPath, String destAbsPath, boolean removeExisting )
        throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, RepositoryException {
    }

    /**
     * {@inheritDoc}
     */
    public void copy( String srcAbsPath, String destAbsPath )
        throws ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, RepositoryException {
    }

    /**
     * {@inheritDoc}
     */
    public void copy( String srcWorkspace, String srcAbsPath, String destAbsPath )
        throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, RepositoryException {
    }

    /**
     * {@inheritDoc}
     */
    public String[] getAccessibleWorkspaceNames() throws RepositoryException {
        return repository.getWorkspaceNamesAccessibleBy(session.getUserID(), session.getCredentials());
    }

    /**
     * {@inheritDoc}
     */
    public ContentHandler getImportContentHandler( String parentAbsPath, int uuidBehavior )
        throws PathNotFoundException, ConstraintViolationException, VersionException, LockException, AccessDeniedException, RepositoryException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public NamespaceRegistry getNamespaceRegistry() throws RepositoryException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public NodeTypeManager getNodeTypeManager() throws RepositoryException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public ObservationManager getObservationManager() throws UnsupportedRepositoryOperationException, RepositoryException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public QueryManager getQueryManager() throws RepositoryException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public Session getSession() {
        return this.session;
    }

    /**
     * {@inheritDoc}
     */
    public void importXML( String parentAbsPath, InputStream in, int uuidBehavior )
        throws IOException, PathNotFoundException, ItemExistsException, ConstraintViolationException, InvalidSerializedDataException, LockException, AccessDeniedException, RepositoryException {
    }

    /**
     * {@inheritDoc}
     */
    public void move( String srcAbsPath, String destAbsPath )
        throws ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, RepositoryException {
    }

    /**
     * {@inheritDoc}
     */
    public void restore( Version[] versions, boolean removeExisting )
        throws ItemExistsException, UnsupportedRepositoryOperationException, VersionException, LockException, InvalidItemStateException, RepositoryException {
    }

}
