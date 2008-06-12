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
import java.io.OutputStream;
import java.security.AccessControlException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.jcr.AccessDeniedException;
import javax.jcr.Credentials;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.LoginException;
import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * @author Randall Hauch
 */
@ThreadSafe
public class DnaSession implements Session {

    private final DnaWorkspace workspace;
    private final DnaRepository repository;
    private final String username;
    private final Credentials credentials;
    private final ReadWriteLock sessionLock = new ReentrantReadWriteLock();
    @GuardedBy( "sessionLock" )
    private boolean connected;

    /**
     * @param repository the {@link DnaRepository} that owns this session; may not be null
     * @param workspaceName the name of the workspace to which this session is connected; may not be null
     * @param credentials the credentials that were used to authentiate this session; may not be null
     * @param username the username used to create this session; may not be null
     */
    /* package */DnaSession( DnaRepository repository, String workspaceName, Credentials credentials, String username ) {
        assert repository != null;
        assert workspaceName != null;
        assert credentials != null;
        assert username != null;
        this.workspace = new DnaWorkspace(repository, workspaceName, this);
        this.repository = repository;
        this.credentials = credentials;
        this.username = username;
        this.connected = true;
    }

    /* package */Credentials getCredentials() {
        return this.credentials;
    }

    /**
     * {@inheritDoc}
     */
    public void addLockToken( String lt ) throws LockException, RepositoryException {
    }

    /**
     * {@inheritDoc}
     */
    public void checkPermission( String absPath, String actions ) throws AccessControlException, RepositoryException {
    }

    /**
     * {@inheritDoc}
     */
    public void exportDocumentView( String absPath, ContentHandler contentHandler, boolean skipBinary, boolean noRecurse ) throws PathNotFoundException, SAXException, RepositoryException {
    }

    /**
     * {@inheritDoc}
     */
    public void exportDocumentView( String absPath, OutputStream out, boolean skipBinary, boolean noRecurse ) throws IOException, PathNotFoundException, RepositoryException {
    }

    /**
     * {@inheritDoc}
     */
    public void exportSystemView( String absPath, ContentHandler contentHandler, boolean skipBinary, boolean noRecurse ) throws PathNotFoundException, SAXException, RepositoryException {
    }

    /**
     * {@inheritDoc}
     */
    public void exportSystemView( String absPath, OutputStream out, boolean skipBinary, boolean noRecurse ) throws IOException, PathNotFoundException, RepositoryException {
    }

    /**
     * {@inheritDoc}
     */
    public Object getAttribute( String name ) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String[] getAttributeNames() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public ContentHandler getImportContentHandler( String parentAbsPath, int uuidBehavior )
        throws PathNotFoundException, ConstraintViolationException, VersionException, LockException, RepositoryException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public Item getItem( String absPath ) throws PathNotFoundException, RepositoryException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String[] getLockTokens() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String getNamespacePrefix( String uri ) throws NamespaceException, RepositoryException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String[] getNamespacePrefixes() throws RepositoryException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String getNamespaceURI( String prefix ) throws NamespaceException, RepositoryException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public Node getNodeByUUID( String uuid ) throws ItemNotFoundException, RepositoryException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public Repository getRepository() {
        return this.repository;
    }

    /**
     * {@inheritDoc}
     */
    public Node getRootNode() throws RepositoryException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String getUserID() {
        return this.username;
    }

    /**
     * {@inheritDoc}
     */
    public ValueFactory getValueFactory() throws UnsupportedRepositoryOperationException, RepositoryException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public Workspace getWorkspace() {
        return this.workspace;
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasPendingChanges() throws RepositoryException {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public Session impersonate( Credentials credentials ) throws LoginException, RepositoryException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void importXML( String parentAbsPath, InputStream in, int uuidBehavior )
        throws IOException, PathNotFoundException, ItemExistsException, ConstraintViolationException, VersionException, InvalidSerializedDataException, LockException, RepositoryException {
    }

    /**
     * {@inheritDoc}
     */
    public boolean isLive() {
        Lock lock = this.sessionLock.readLock();
        try {
            return connected;
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean itemExists( String absPath ) throws RepositoryException {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void logout() {
        Lock lock = this.sessionLock.writeLock();
        try {
            repository.logout(this);
            connected = false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void move( String srcAbsPath, String destAbsPath ) throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException, RepositoryException {
    }

    /**
     * {@inheritDoc}
     */
    public void refresh( boolean keepChanges ) throws RepositoryException {
    }

    /**
     * {@inheritDoc}
     */
    public void removeLockToken( String lt ) {
    }

    /**
     * {@inheritDoc}
     */
    public void save()
        throws AccessDeniedException, ItemExistsException, ConstraintViolationException, InvalidItemStateException, VersionException, LockException, NoSuchNodeTypeException, RepositoryException {
    }

    /**
     * {@inheritDoc}
     */
    public void setNamespacePrefix( String newPrefix, String existingUri ) throws NamespaceException, RepositoryException {
    }

}
