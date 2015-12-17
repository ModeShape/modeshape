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
package org.modeshape.jca;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessControlException;
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
import javax.jcr.Property;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.retention.RetentionManager;
import javax.jcr.security.AccessControlManager;
import javax.jcr.version.VersionException;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * CCI connection.
 * 
 * @author kulikov
 */
public class JcrSessionHandle implements Session {

    /**
     * Managed connection.
     */
    private JcrManagedConnection mc;

    /**
     * Construct a new session.
     * 
     * @param mc Managed connection instance.
     */
    public JcrSessionHandle( JcrManagedConnection mc ) {
        this.mc = mc;
    }

    public JcrManagedConnection getManagedConnection() {
        return mc;
    }

    public void setManagedConnection( JcrManagedConnection mc ) {
        this.mc = mc;
    }

    private Session session() {
        return mc.getSession(this);
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
    public Object getAttribute( String name ) {
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
    public Session impersonate( Credentials c ) throws LoginException, RepositoryException {
        return session().impersonate(c);
    }

    @SuppressWarnings( "deprecation" )
    @Override
    public Node getNodeByUUID( String string ) throws ItemNotFoundException, RepositoryException {
        return session().getNodeByUUID(string);
    }

    @Override
    public Node getNodeByIdentifier( String string ) throws ItemNotFoundException, RepositoryException {
        return session().getNodeByIdentifier(string);
    }

    @Override
    public Item getItem( String string ) throws PathNotFoundException, RepositoryException {
        return session().getItem(string);
    }

    @Override
    public Node getNode( String string ) throws PathNotFoundException, RepositoryException {
        return session().getNode(string);
    }

    @Override
    public Property getProperty( String string ) throws PathNotFoundException, RepositoryException {
        return session().getProperty(string);
    }

    @Override
    public boolean itemExists( String string ) throws RepositoryException {
        return session().itemExists(string);
    }

    @Override
    public boolean nodeExists( String string ) throws RepositoryException {
        return session().nodeExists(string);
    }

    @Override
    public boolean propertyExists( String string ) throws RepositoryException {
        return session().propertyExists(string);
    }

    @Override
    public void move( String string,
                      String string1 )
        throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException,
        RepositoryException {
        session().move(string, string1);
    }

    @Override
    public void removeItem( String string )
        throws VersionException, LockException, ConstraintViolationException, AccessDeniedException, RepositoryException {
        session().removeItem(string);
    }

    @Override
    public void save()
        throws AccessDeniedException, ItemExistsException, ReferentialIntegrityException, ConstraintViolationException,
        InvalidItemStateException, VersionException, LockException, NoSuchNodeTypeException, RepositoryException {
        session().save();
    }

    @Override
    public void refresh( boolean bln ) throws RepositoryException {
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
    public boolean hasPermission( String string,
                                  String string1 ) throws RepositoryException {
        return session().hasPermission(string, string1);
    }

    @Override
    public void checkPermission( String string,
                                 String string1 ) throws AccessControlException, RepositoryException {
        session().checkPermission(string, string1);
    }

    @Override
    public boolean hasCapability( String string,
                                  Object o,
                                  Object[] os ) throws RepositoryException {
        return session().hasCapability(string, o, os);
    }

    @Override
    public ContentHandler getImportContentHandler( String string,
                                                   int i )
        throws PathNotFoundException, ConstraintViolationException, VersionException, LockException, RepositoryException {
        return session().getImportContentHandler(string, i);
    }

    @Override
    public void importXML( String string,
                           InputStream in,
                           int i )
        throws IOException, PathNotFoundException, ItemExistsException, ConstraintViolationException, VersionException,
        InvalidSerializedDataException, LockException, RepositoryException {
        session().importXML(string, in, i);
    }

    @Override
    public void exportSystemView( String string,
                                  ContentHandler ch,
                                  boolean bln,
                                  boolean bln1 ) throws PathNotFoundException, SAXException, RepositoryException {
        session().exportSystemView(string, ch, bln, bln1);
    }

    @Override
    public void exportSystemView( String string,
                                  OutputStream out,
                                  boolean bln,
                                  boolean bln1 ) throws IOException, PathNotFoundException, RepositoryException {
        session().exportDocumentView(string, out, bln, bln1);
    }

    @Override
    public void exportDocumentView( String string,
                                    ContentHandler ch,
                                    boolean bln,
                                    boolean bln1 ) throws PathNotFoundException, SAXException, RepositoryException {
        session().exportDocumentView(string, ch, bln, bln1);
    }

    @Override
    public void exportDocumentView( String string,
                                    OutputStream out,
                                    boolean bln,
                                    boolean bln1 ) throws IOException, PathNotFoundException, RepositoryException {
        session().exportDocumentView(string, out, bln, bln1);
    }

    @Override
    public void setNamespacePrefix( String string,
                                    String string1 ) throws NamespaceException, RepositoryException {
        session().setNamespacePrefix(string, string1);
    }

    @Override
    public String[] getNamespacePrefixes() throws RepositoryException {
        return session().getNamespacePrefixes();
    }

    @Override
    public String getNamespaceURI( String string ) throws NamespaceException, RepositoryException {
        return session().getNamespaceURI(string);
    }

    @Override
    public String getNamespacePrefix( String string ) throws NamespaceException, RepositoryException {
        return session().getNamespacePrefix(string);
    }

    @Override
    public void logout() {
        mc.closeHandle(this);
    }

    @Override
    public boolean isLive() {
        return session().isLive();
    }

    @SuppressWarnings( "deprecation" )
    @Override
    public void addLockToken( String string ) {
        session().addLockToken(string);
    }

    @SuppressWarnings( "deprecation" )
    @Override
    public String[] getLockTokens() {
        return session().getLockTokens();
    }

    @SuppressWarnings( "deprecation" )
    @Override
    public void removeLockToken( String string ) {
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
}
