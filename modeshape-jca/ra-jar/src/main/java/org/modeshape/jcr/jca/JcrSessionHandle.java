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
package org.modeshape.jcr.jca;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessControlException;
import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.retention.RetentionManager;
import javax.jcr.security.AccessControlManager;
import javax.jcr.version.VersionException;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 *
 * @author kulikov
 */
public class JcrSessionHandle implements Session, XAResource{

    /**
     * Managed connection.
     */
    private JcrManagedConnection mc;

    /**
     * Construct a new session().
     */
    public JcrSessionHandle(JcrManagedConnection mc) {
        this.mc = mc;
    }

    private Session session() {
        return mc.getSession(this);
    }

    /**
     * Return the managed connection.
     */
    public JcrManagedConnection getManagedConnection() {
        return mc;
    }

    /**
     * Set the managed connection.
     */
    public void setManagedConnection(JcrManagedConnection mc) {
        this.mc = mc;
    }
    
    @Override
    public Repository getRepository() {
        return session().getRepository();
    }

    @Override
    public String getUserID() {
        return session().getUserID();
    }

    @Override
    public String[] getAttributeNames() {
        return session().getAttributeNames();
    }

    @Override
    public Object getAttribute(String name) {
        return session().getAttribute(name);
    }

    @Override
    public Workspace getWorkspace() {
        return session().getWorkspace();
    }

    @Override
    public Node getRootNode() throws RepositoryException {
        return session().getRootNode();
    }

    @Override
    public Session impersonate(Credentials c) throws LoginException, RepositoryException {
        return session().impersonate(c);
    }

    @Override
    public Node getNodeByUUID(String string) throws ItemNotFoundException, RepositoryException {
        return session().getNodeByUUID(string);
    }

    @Override
    public Node getNodeByIdentifier(String string) throws ItemNotFoundException, RepositoryException {
        return session().getNodeByIdentifier(string);
    }

    @Override
    public Item getItem(String string) throws PathNotFoundException, RepositoryException {
        return session().getItem(string);
    }

    @Override
    public Node getNode(String string) throws PathNotFoundException, RepositoryException {
        return session().getNode(string);
    }

    @Override
    public Property getProperty(String string) throws PathNotFoundException, RepositoryException {
        return session().getProperty(string);
    }

    @Override
    public boolean itemExists(String string) throws RepositoryException {
        return session().itemExists(string);
    }

    @Override
    public boolean nodeExists(String string) throws RepositoryException {
        return session().nodeExists(string);
    }

    @Override
    public boolean propertyExists(String string) throws RepositoryException {
        return session().propertyExists(string);
    }

    @Override
    public void move(String string, String string1) throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException, RepositoryException {
        session().move(string, string1);
    }

    @Override
    public void removeItem(String string) throws VersionException, LockException, ConstraintViolationException, AccessDeniedException, RepositoryException {
        session().removeItem(string);
    }

    @Override
    public void save() throws AccessDeniedException, ItemExistsException, ReferentialIntegrityException, ConstraintViolationException, InvalidItemStateException, VersionException, LockException, NoSuchNodeTypeException, RepositoryException {
        session().save();
    }

    @Override
    public void refresh(boolean bln) throws RepositoryException {
        session().refresh(bln);
    }

    @Override
    public boolean hasPendingChanges() throws RepositoryException {
        return session().hasPendingChanges();
    }

    @Override
    public ValueFactory getValueFactory() throws UnsupportedRepositoryOperationException, RepositoryException {
        return session().getValueFactory();
    }

    @Override
    public boolean hasPermission(String string, String string1) throws RepositoryException {
        return session().hasPermission(string, string1);
    }

    @Override
    public void checkPermission(String string, String string1) throws AccessControlException, RepositoryException {
        session().checkPermission(string, string1);
    }

    @Override
    public boolean hasCapability(String string, Object o, Object[] os) throws RepositoryException {
        return session().hasCapability(string, o, os);
    }

    @Override
    public ContentHandler getImportContentHandler(String string, int i) throws PathNotFoundException, ConstraintViolationException, VersionException, LockException, RepositoryException {
        return session().getImportContentHandler(string, i);
    }

    @Override
    public void importXML(String string, InputStream in, int i) throws IOException, PathNotFoundException, ItemExistsException, ConstraintViolationException, VersionException, InvalidSerializedDataException, LockException, RepositoryException {
        session().importXML(string, in, i);
    }

    @Override
    public void exportSystemView(String string, ContentHandler ch, boolean bln, boolean bln1) throws PathNotFoundException, SAXException, RepositoryException {
        session().exportSystemView(string, ch, bln, bln1);
    }

    @Override
    public void exportSystemView(String string, OutputStream out, boolean bln, boolean bln1) throws IOException, PathNotFoundException, RepositoryException {
        session().exportDocumentView(string, out, bln, bln1);
    }

    @Override
    public void exportDocumentView(String string, ContentHandler ch, boolean bln, boolean bln1) throws PathNotFoundException, SAXException, RepositoryException {
        session().exportDocumentView(string, ch, bln, bln1);
    }

    @Override
    public void exportDocumentView(String string, OutputStream out, boolean bln, boolean bln1) throws IOException, PathNotFoundException, RepositoryException {
        session().exportDocumentView(string, out, bln, bln1);
    }

    @Override
    public void setNamespacePrefix(String string, String string1) throws NamespaceException, RepositoryException {
        session().setNamespacePrefix(string, string1);
    }

    @Override
    public String[] getNamespacePrefixes() throws RepositoryException {
        return session().getNamespacePrefixes();
    }

    @Override
    public String getNamespaceURI(String string) throws NamespaceException, RepositoryException {
        return session().getNamespaceURI(string);
    }

    @Override
    public String getNamespacePrefix(String string) throws NamespaceException, RepositoryException {
        return session().getNamespacePrefix(string);
    }

    @Override
    public void logout() {
        session().logout();
    }

    @Override
    public boolean isLive() {
        return session().isLive();
    }

    @Override
    public void addLockToken(String string) {
        session().addLockToken(string);
    }

    @Override
    public String[] getLockTokens() {
        return session().getLockTokens();
    }

    @Override
    public void removeLockToken(String string) {
        session().removeLockToken(string);
    }

    @Override
    public AccessControlManager getAccessControlManager() throws UnsupportedRepositoryOperationException, RepositoryException {
        return session().getAccessControlManager();
    }

    @Override
    public RetentionManager getRetentionManager() throws UnsupportedRepositoryOperationException, RepositoryException {
        return session().getRetentionManager();
    }

    private XAResource getXAResource() throws XAException {
        Session session = session();
        if (session instanceof XAResource) {
            return (XAResource) session;
        } else {
            throw new XAException(
                    "XA transactions are not supported with " + session);
        }
    }

    @Override
    public void commit(Xid xid, boolean bln) throws XAException {
        getXAResource().commit(xid, bln);
    }

    @Override
    public void end(Xid xid, int i) throws XAException {
        getXAResource().end(xid, i);
    }

    @Override
    public void forget(Xid xid) throws XAException {
        getXAResource().forget(xid);
    }

    @Override
    public int getTransactionTimeout() throws XAException {
        return getXAResource().getTransactionTimeout();
    }

    @Override
    public boolean isSameRM(XAResource xar) throws XAException {
        return getXAResource().isSameRM(xar);
    }

    @Override
    public int prepare(Xid xid) throws XAException {
        return getXAResource().prepare(xid);
    }

    @Override
    public Xid[] recover(int i) throws XAException {
        return getXAResource().recover(i);
    }

    @Override
    public void rollback(Xid xid) throws XAException {
        getXAResource().rollback(xid);
    }

    @Override
    public boolean setTransactionTimeout(int i) throws XAException {
        return getXAResource().setTransactionTimeout(i);
    }

    @Override
    public void start(Xid xid, int i) throws XAException {
        getXAResource().start(xid, i);
    }

}
