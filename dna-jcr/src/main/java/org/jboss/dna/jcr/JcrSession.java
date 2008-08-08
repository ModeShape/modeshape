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

import java.io.InputStream;
import java.io.OutputStream;
import java.security.Principal;
import java.util.Set;
import javax.jcr.Credentials;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.jcr.GraphTools.NodeContent;
import org.xml.sax.ContentHandler;

/**
 * @author John Verhaeg
 * @author Randall Hauch
 */
@NotThreadSafe
final class JcrSession implements Session {

    private final JcrRepository repository;
    private final JcrExecutionContext executionContext;
    private boolean isLive;
    private JcrWorkspace workspace;
    private Node rootNode;

    // private final Map<String, Node> uuid2NodeMap = new HashMap<String, Node>();

    JcrSession( JcrRepository repository,
                JcrExecutionContext executionContext,
                String workspaceName ) throws RepositoryException {
        assert repository != null;
        assert executionContext != null;
        assert workspaceName != null;
        this.repository = repository;
        this.executionContext = executionContext;
        executionContext.getGraphTools().setSession(this);
        this.isLive = true;
        // Following must be initialized after session's state is initialized
        this.workspace = new JcrWorkspace(this, workspaceName);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#addLockToken(java.lang.String)
     */
    public void addLockToken( String lt ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#checkPermission(java.lang.String, java.lang.String)
     */
    public void checkPermission( String absPath,
                                 String actions ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#exportDocumentView(java.lang.String, org.xml.sax.ContentHandler, boolean, boolean)
     */
    public void exportDocumentView( String absPath,
                                    ContentHandler contentHandler,
                                    boolean skipBinary,
                                    boolean noRecurse ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#exportDocumentView(java.lang.String, java.io.OutputStream, boolean, boolean)
     */
    public void exportDocumentView( String absPath,
                                    OutputStream out,
                                    boolean skipBinary,
                                    boolean noRecurse ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#exportSystemView(java.lang.String, org.xml.sax.ContentHandler, boolean, boolean)
     */
    public void exportSystemView( String absPath,
                                  ContentHandler contentHandler,
                                  boolean skipBinary,
                                  boolean noRecurse ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#exportSystemView(java.lang.String, java.io.OutputStream, boolean, boolean)
     */
    public void exportSystemView( String absPath,
                                  OutputStream out,
                                  boolean skipBinary,
                                  boolean noRecurse ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#getAttribute(java.lang.String)
     */
    public Object getAttribute( String name ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#getAttributeNames()
     */
    public String[] getAttributeNames() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#getImportContentHandler(java.lang.String, int)
     */
    public ContentHandler getImportContentHandler( String parentAbsPath,
                                                   int uuidBehavior ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#getItem(java.lang.String)
     */
    public Item getItem( String absPath ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#getLockTokens()
     */
    public String[] getLockTokens() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#getNamespacePrefix(java.lang.String)
     */
    public String getNamespacePrefix( String uri ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#getNamespacePrefixes()
     */
    public String[] getNamespacePrefixes() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#getNamespaceURI(java.lang.String)
     */
    public String getNamespaceURI( String prefix ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#getNodeByUUID(java.lang.String)
     */
    public Node getNodeByUUID( String uuid ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#getRepository()
     */
    public Repository getRepository() {
        return this.repository;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#getRootNode()
     */
    public Node getRootNode() throws RepositoryException {
        // If root has no UUID, populate its contents from source
        if (rootNode != null) {
            return rootNode;
        }
        // Get root node from source
        assert executionContext.getGraphTools() != null;
        assert executionContext.getValueFactories() != null;
        assert executionContext.getValueFactories().getPathFactory() != null;
        NodeContent content = executionContext.getGraphTools().getNodeContent(executionContext.getValueFactories().getPathFactory().createRootPath());
        rootNode = new JcrRootNode(this, content.properties);
        return rootNode;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#getUserID()
     */
    public String getUserID() {
        Subject subject = executionContext.getSubject();
        if (subject == null) return null;
        Set<Principal> principals = subject.getPrincipals();
        if (principals == null || principals.isEmpty()) return null;
        return principals.iterator().next().getName();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#getValueFactory()
     */
    public ValueFactory getValueFactory() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#getWorkspace()
     */
    public Workspace getWorkspace() {
        return this.workspace;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#hasPendingChanges()
     */
    public boolean hasPendingChanges() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#impersonate(javax.jcr.Credentials)
     */
    public Session impersonate( Credentials credentials ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#importXML(java.lang.String, java.io.InputStream, int)
     */
    public void importXML( String parentAbsPath,
                           InputStream in,
                           int uuidBehavior ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#isLive()
     */
    public boolean isLive() {
        return isLive;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#itemExists(java.lang.String)
     */
    public boolean itemExists( String absPath ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#logout()
     */
    public void logout() {
        try {
            if (executionContext.getRepositoryConnection() != null) {
                executionContext.getRepositoryConnection().close();
            }
            assert executionContext.getLoginContext() != null;
            executionContext.getLoginContext().logout();
            isLive = false;
        } catch (InterruptedException error) {
            // TODO: Change to DnaException once DNA-180 is addressed
            throw new RuntimeException(error);
        } catch (LoginException error) {
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#move(java.lang.String, java.lang.String)
     */
    public void move( String srcAbsPath,
                      String destAbsPath ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#refresh(boolean)
     */
    public void refresh( boolean keepChanges ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#removeLockToken(java.lang.String)
     */
    public void removeLockToken( String lt ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#save()
     */
    public void save() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#setNamespacePrefix(java.lang.String, java.lang.String)
     */
    public void setNamespacePrefix( String newPrefix,
                                    String existingUri ) {
        throw new UnsupportedOperationException();
    }
}
