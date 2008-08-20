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
import java.lang.ref.WeakReference;
import java.security.Principal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.jcr.Credentials;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.common.util.StringUtil;
import org.jboss.dna.spi.DnaLexicon;
import org.jboss.dna.spi.ExecutionContext;
import org.jboss.dna.spi.connector.RepositoryConnection;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.Path.Segment;
import org.jboss.dna.spi.graph.commands.GraphCommand;
import org.jboss.dna.spi.graph.commands.impl.BasicGetNodeCommand;
import org.xml.sax.ContentHandler;

/**
 * @author John Verhaeg
 * @author Randall Hauch
 */
@NotThreadSafe
final class JcrSession implements Session {

    private final Repository repository;
    private final ExecutionContext executionContext;
    private RepositoryConnection connection;
    private final Map<String, WeakReference<Node>> uuid2NodeMap;
    private boolean isLive;
    private Workspace workspace;
    private JcrRootNode rootNode;

    JcrSession( Repository repository,
                ExecutionContext executionContext,
                String workspaceName,
                RepositoryConnection connection,
                Map<String, WeakReference<Node>> uuid2NodeMap ) throws RepositoryException {
        assert repository != null;
        assert executionContext != null;
        assert workspaceName != null;
        assert connection != null;
        assert uuid2NodeMap != null;
        this.repository = repository;
        this.executionContext = executionContext;
        this.connection = connection;
        this.uuid2NodeMap = uuid2NodeMap;
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

    private void execute( GraphCommand... commands ) throws RepositoryException {
        try {
            connection.execute(executionContext, commands);
        } catch (RuntimeException error) {
            throw error;
        } catch (Exception error) {
            throw new RepositoryException(error);
        }
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
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#getAttributeNames()
     */
    public String[] getAttributeNames() {
        return StringUtil.EMPTY_STRING_ARRAY;
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
    public Item getItem( String absolutePath ) throws RepositoryException {
        ArgCheck.isNotEmpty(absolutePath, "absolutePath");
        // Return root node if path is "/"
        Path path = executionContext.getValueFactories().getPathFactory().create(absolutePath);
        if (path.isRoot()) {
            return getRootNode();
        }
        // Since we don't know whether path refers to a node or property, get the parent contents, which must refer to a node
        Path parentPath = path.getAncestor();
        BasicGetNodeCommand getNodeCommand = new BasicGetNodeCommand(parentPath);
        execute(getNodeCommand);
        // First search for a child with the last name in the path
        Name name = path.getLastSegment().getName();
        for (Segment seg : getNodeCommand.getChildren()) {
            if (seg.getName().equals(name)) {
                return getNode(path);
            }
        }
        // If a node isn't found & last segment contains no index, get parent node & search for a property with the last name in
        // the path
        Segment seg = path.getLastSegment();
        if (!seg.hasIndex()) {
            return getNode(parentPath).getProperty(seg.getString());
        }
        // If a property isn't found, throw a PathNotFoundException
        throw new PathNotFoundException(JcrI18n.pathNotFound.text(path));
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

    private Node getNode( Path path ) throws RepositoryException {
        // Get node from source
        BasicGetNodeCommand command = new BasicGetNodeCommand(path);
        execute(command);
        // First check if node already exists. We don't need to check for changes since that will be handled by an observer
        org.jboss.dna.spi.graph.Property dnaUuidProp = command.getPropertiesByName().get(DnaLexicon.UUID);
        if (dnaUuidProp != null) {
            String uuid = executionContext.getValueFactories().getStringFactory().create(dnaUuidProp.getValues()).next();
            WeakReference<Node> ref = uuid2NodeMap.get(uuid);
            if (ref != null) {
                Node node = ref.get();
                if (node != null) {
                    return node;
                }
            }
        }
        // If not create a new one & populate it
        JcrNode node = new JcrNode(this);
        populateNode(node, command);
        return node;
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
        // Return cached root node if available
        if (rootNode != null) {
            return rootNode;
        }
        // Get root node from source
        assert executionContext.getValueFactories() != null;
        assert executionContext.getValueFactories().getPathFactory() != null;
        rootNode = new JcrRootNode(this);
        // Get root node from source
        BasicGetNodeCommand getNodeCommand = new BasicGetNodeCommand(
                                                                     executionContext.getValueFactories().getPathFactory().createRootPath());
        execute(getNodeCommand);
        populateNode(rootNode, getNodeCommand);
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
        if (!isLive()) {
            return;
        }
        try {
            if (connection != null) {
                connection.close();
                connection = null;
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

    private void populateNode( AbstractJcrNode node,
                               BasicGetNodeCommand getNodeCommand ) throws RepositoryException {
        // TODO: What do we do to validate node against its primary type?
        assert node != null;
        assert getNodeCommand != null;
        // Create JCR children for corresponding DNA children
        node.setChildren(getNodeCommand.getChildren());
        // Create JCR properties for corresponding DNA properties
        Set<Property> properties = new HashSet<Property>();
        boolean uuidFound = false;
        for (org.jboss.dna.spi.graph.Property dnaProp : getNodeCommand.getProperties()) {
            if (DnaLexicon.UUID.equals(dnaProp.getName())) {
                uuidFound = true;
            }
            properties.add(new JcrProperty(node, executionContext, dnaProp.getName(), dnaProp.getValues().next()));
        }
        // Ensure a UUID property exists
        if (!uuidFound) {
            properties.add(new JcrProperty(node, executionContext, DnaLexicon.UUID, UUID.randomUUID()));
        }
        node.setProperties(properties);
        // Setup node to be retrieved by UUID
        uuid2NodeMap.put(node.getUUID(), new WeakReference<Node>(node));
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
