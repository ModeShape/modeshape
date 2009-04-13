/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
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
import java.security.AccessControlException;
import java.security.Principal;
import java.util.Calendar;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.jcr.Credentials;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;
import javax.jcr.Workspace;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.property.Binary;
import org.jboss.dna.graph.property.DateTime;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.NamespaceRegistry;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.ValueFactories;
import org.jboss.dna.graph.property.basic.LocalNamespaceRegistry;
import org.jboss.dna.jcr.JcrNamespaceRegistry.Behavior;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * @author John Verhaeg
 * @author Randall Hauch
 */
@NotThreadSafe
class JcrSession implements Session {

    private static final String[] NO_ATTRIBUTES_NAMES = new String[] {};

    /**
     * The repository that created this session.
     */
    private final JcrRepository repository;

    /**
     * The workspace that corresponds to this session.
     */
    private final JcrWorkspace workspace;

    /**
     * A JCR namespace registry that is specific to this session, with any locally-defined namespaces defined in this session.
     * This is backed by the workspace's namespace registry.
     */
    private final JcrNamespaceRegistry sessionRegistry;

    /**
     * The execution context for this session, which uses the {@link #sessionRegistry session's namespace registry}
     */
    protected final ExecutionContext executionContext;

    /**
     * The session-specific attributes that came from the {@link SimpleCredentials}' {@link SimpleCredentials#getAttributeNames()}
     */
    private final Map<String, Object> sessionAttributes;

    /**
     * The graph representing this session, which uses the {@link #graph session's graph}.
     */
    private final Graph graph;

    private final SessionCache cache;

    /**
     * A cached instance of the root path.
     */
    private final Path rootPath;

    private boolean isLive;

    JcrSession( JcrRepository repository,
                JcrWorkspace workspace,
                ExecutionContext workspaceContext,
                Map<String, Object> sessionAttributes ) {
        assert repository != null;
        assert workspace != null;
        assert sessionAttributes != null;
        assert workspaceContext != null;
        this.repository = repository;
        this.sessionAttributes = sessionAttributes;
        this.workspace = workspace;

        // Create an execution context for this session, which should use the local namespace registry ...
        NamespaceRegistry workspaceRegistry = workspaceContext.getNamespaceRegistry();
        NamespaceRegistry local = new LocalNamespaceRegistry(workspaceRegistry);
        this.executionContext = workspaceContext.with(local);
        this.sessionRegistry = new JcrNamespaceRegistry(Behavior.JSR170_SESSION, local, workspaceRegistry);
        this.rootPath = this.executionContext.getValueFactories().getPathFactory().createRootPath();

        // Set up the graph to use for this session (which uses the session's namespace registry and context) ...
        this.graph = Graph.create(this.repository.getRepositorySourceName(),
                                  this.repository.getConnectionFactory(),
                                  this.executionContext);

        this.cache = new SessionCache(this, workspace.getName(), this.executionContext, this.workspace.nodeTypeManager(),
                                      this.graph);
        this.isLive = true;

        assert this.repository != null;
        assert this.sessionAttributes != null;
        assert this.workspace != null;
        assert this.executionContext != null;
        assert this.sessionRegistry != null;
        assert this.graph != null;
    }

    // Added to facilitate mock testing of items without necessarily requiring an entire repository structure to be built
    final SessionCache cache() {
        return this.cache;
    }

    ExecutionContext getExecutionContext() {
        return this.executionContext;
    }

    JcrNodeTypeManager nodeTypeManager() {
        return this.workspace.nodeTypeManager();
    }

    NamespaceRegistry namespaces() {
        return this.executionContext.getNamespaceRegistry();
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
     * @see javax.jcr.Session#getRepository()
     */
    public Repository getRepository() {
        return this.repository;
    }

    /**
     * {@inheritDoc}
     * 
     * @return <code>null</code>
     * @see javax.jcr.Session#getAttribute(java.lang.String)
     */
    public Object getAttribute( String name ) {
        return sessionAttributes.get(name);
    }

    /**
     * {@inheritDoc}
     * 
     * @return An empty array
     * @see javax.jcr.Session#getAttributeNames()
     */
    public String[] getAttributeNames() {
        Set<String> names = sessionAttributes.keySet();
        if (names.isEmpty()) return NO_ATTRIBUTES_NAMES;
        return names.toArray(new String[names.size()]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#getNamespacePrefix(java.lang.String)
     */
    public String getNamespacePrefix( String uri ) throws RepositoryException {
        return sessionRegistry.getPrefix(uri);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#getNamespacePrefixes()
     */
    public String[] getNamespacePrefixes() {
        return sessionRegistry.getPrefixes();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#getNamespaceURI(java.lang.String)
     */
    public String getNamespaceURI( String prefix ) throws RepositoryException {
        return sessionRegistry.getURI(prefix);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#setNamespacePrefix(java.lang.String, java.lang.String)
     */
    public void setNamespacePrefix( String newPrefix,
                                    String existingUri ) throws NamespaceException, RepositoryException {
        sessionRegistry.registerNamespace(newPrefix, existingUri);
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Session#addLockToken(java.lang.String)
     */
    public void addLockToken( String lt ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IllegalArgumentException if either <code>path</code> or <code>actions</code> is empty or <code>null</code>.
     * @see javax.jcr.Session#checkPermission(java.lang.String, java.lang.String)
     */
    public void checkPermission( String path,
                                 String actions ) {
        CheckArg.isNotEmpty(path, "path");
        CheckArg.isNotEmpty(actions, "actions");
        if (!"read".equals(actions)) {
            throw new AccessControlException(JcrI18n.permissionDenied.text(path, actions));
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
                                    boolean noRecurse ) throws RepositoryException, SAXException {
        CheckArg.isNotNull(absPath, "absPath");
        CheckArg.isNotNull(contentHandler, "contentHandler");

        Path exportRootPath = executionContext.getValueFactories().getPathFactory().create(absPath);
        Node exportRootNode = getNode(exportRootPath);

        AbstractJcrExporter exporter = new JcrDocumentViewExporter(this);

        exporter.exportView(exportRootNode, contentHandler, skipBinary, noRecurse);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#exportDocumentView(java.lang.String, java.io.OutputStream, boolean, boolean)
     */
    public void exportDocumentView( String absPath,
                                    OutputStream out,
                                    boolean skipBinary,
                                    boolean noRecurse ) throws RepositoryException {
        CheckArg.isNotNull(absPath, "absPath");
        CheckArg.isNotNull(out, "out");

        Path exportRootPath = executionContext.getValueFactories().getPathFactory().create(absPath);
        Node exportRootNode = getNode(exportRootPath);

        AbstractJcrExporter exporter = new JcrDocumentViewExporter(this);

        exporter.exportView(exportRootNode, out, skipBinary, noRecurse);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#exportSystemView(java.lang.String, org.xml.sax.ContentHandler, boolean, boolean)
     */
    public void exportSystemView( String absPath,
                                  ContentHandler contentHandler,
                                  boolean skipBinary,
                                  boolean noRecurse ) throws RepositoryException, SAXException {
        CheckArg.isNotNull(absPath, "absPath");
        CheckArg.isNotNull(contentHandler, "contentHandler");

        Path exportRootPath = executionContext.getValueFactories().getPathFactory().create(absPath);
        Node exportRootNode = getNode(exportRootPath);

        AbstractJcrExporter exporter = new JcrSystemViewExporter(this);

        exporter.exportView(exportRootNode, contentHandler, skipBinary, noRecurse);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#exportSystemView(java.lang.String, java.io.OutputStream, boolean, boolean)
     */
    public void exportSystemView( String absPath,
                                  OutputStream out,
                                  boolean skipBinary,
                                  boolean noRecurse ) throws RepositoryException {
        CheckArg.isNotNull(absPath, "absPath");
        CheckArg.isNotNull(out, "out");

        Path exportRootPath = executionContext.getValueFactories().getPathFactory().create(absPath);
        Node exportRootNode = getNode(exportRootPath);

        AbstractJcrExporter exporter = new JcrSystemViewExporter(this);

        exporter.exportView(exportRootNode, out, skipBinary, noRecurse);
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Session#getImportContentHandler(java.lang.String, int)
     */
    public ContentHandler getImportContentHandler( String parentAbsPath,
                                                   int uuidBehavior ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IllegalArgumentException if <code>absolutePath</code> is empty or <code>null</code>.
     * @see javax.jcr.Session#getItem(java.lang.String)
     */
    public Item getItem( String absolutePath ) throws RepositoryException {
        CheckArg.isNotEmpty(absolutePath, "absolutePath");
        // Return root node if path is "/"
        Path path = executionContext.getValueFactories().getPathFactory().create(absolutePath);
        if (path.isRoot()) {
            return getRootNode();
        }
        // Since we don't know whether path refers to a node or a property, look to see if we can tell it's a node ...
        if (path.getLastSegment().hasIndex()) {
            return getNode(path);
        }
        // We can't tell from the name, so try a node first ...
        try {
            return getNode(path);
        } catch (PathNotFoundException e) {
            // A node was not found, so look for a node using the parent as the node's path ...
            Path parentPath = path.getParent();
            Name propertyName = path.getLastSegment().getName();
            return getNode(parentPath).getProperty(propertyName.getString(namespaces()));
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Session#getLockTokens()
     */
    public String[] getLockTokens() {
        throw new UnsupportedOperationException();
    }

    /**
     * Find or create a JCR Node for the given path. This method works for the root node, too.
     * 
     * @param path the path; may not be null
     * @return the JCR node instance for the given path; never null
     * @throws PathNotFoundException if the path could not be found
     * @throws RepositoryException if there is a problem
     */
    Node getNode( Path path ) throws RepositoryException, PathNotFoundException {
        if (path.isRoot()) return cache.findJcrRootNode();
        return cache.findJcrNode(null, path.relativeTo(rootPath));
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#getNodeByUUID(java.lang.String)
     */
    public Node getNodeByUUID( String uuid ) throws ItemNotFoundException, RepositoryException {
        return cache.findJcrNode(UUID.fromString(uuid));
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#getRootNode()
     */
    public Node getRootNode() throws RepositoryException {
        return cache.findJcrRootNode();
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
        final ValueFactories valueFactories = executionContext.getValueFactories();
        final SessionCache sessionCache = this.cache;

        return new ValueFactory() {

            public Value createValue( String value,
                                      int propertyType ) throws ValueFormatException {
                return new JcrValue(valueFactories, sessionCache, propertyType, convertValueToType(value, propertyType));
            }

            public Value createValue( Node value ) throws RepositoryException {
                String uuid = valueFactories.getStringFactory().create(value.getUUID());
                return new JcrValue(valueFactories, sessionCache, PropertyType.REFERENCE, uuid);
            }

            public Value createValue( InputStream value ) {
                Binary binary = valueFactories.getBinaryFactory().create(value);
                return new JcrValue(valueFactories, sessionCache, PropertyType.BINARY, binary);
            }

            public Value createValue( Calendar value ) {
                DateTime dateTime = valueFactories.getDateFactory().create(value);
                return new JcrValue(valueFactories, sessionCache, PropertyType.DATE, dateTime);
            }

            public Value createValue( boolean value ) {
                return new JcrValue(valueFactories, sessionCache, PropertyType.BOOLEAN, value);
            }

            public Value createValue( double value ) {
                return new JcrValue(valueFactories, sessionCache, PropertyType.DOUBLE, value);
            }

            public Value createValue( long value ) {
                return new JcrValue(valueFactories, sessionCache, PropertyType.LONG, value);
            }

            public Value createValue( String value ) {
                return new JcrValue(valueFactories, sessionCache, PropertyType.STRING, value);
            }

            Object convertValueToType( Object value,
                                       int toType ) throws ValueFormatException {
                switch (toType) {
                    case PropertyType.BOOLEAN:
                        try {
                            return valueFactories.getBooleanFactory().create(value);
                        } catch (org.jboss.dna.graph.property.ValueFormatException vfe) {
                            throw new ValueFormatException(vfe);
                        }

                    case PropertyType.DATE:
                        try {
                            return valueFactories.getDateFactory().create(value);
                        } catch (org.jboss.dna.graph.property.ValueFormatException vfe) {
                            throw new ValueFormatException(vfe);
                        }

                    case PropertyType.NAME:
                        try {
                            return valueFactories.getNameFactory().create(value);
                        } catch (org.jboss.dna.graph.property.ValueFormatException vfe) {
                            throw new ValueFormatException(vfe);
                        }

                    case PropertyType.PATH:
                        try {
                            return valueFactories.getPathFactory().create(value);
                        } catch (org.jboss.dna.graph.property.ValueFormatException vfe) {
                            throw new ValueFormatException(vfe);
                        }

                    case PropertyType.REFERENCE:
                        try {
                            return valueFactories.getReferenceFactory().create(value);
                        } catch (org.jboss.dna.graph.property.ValueFormatException vfe) {
                            throw new ValueFormatException(vfe);
                        }
                    case PropertyType.DOUBLE:
                        try {
                            return valueFactories.getDoubleFactory().create(value);
                        } catch (org.jboss.dna.graph.property.ValueFormatException vfe) {
                            throw new ValueFormatException(vfe);
                        }
                    case PropertyType.LONG:
                        try {
                            return valueFactories.getLongFactory().create(value);
                        } catch (org.jboss.dna.graph.property.ValueFormatException vfe) {
                            throw new ValueFormatException(vfe);
                        }

                        // Anything can be converted to these types
                    case PropertyType.BINARY:
                        try {
                            return valueFactories.getBinaryFactory().create(value);
                        } catch (org.jboss.dna.graph.property.ValueFormatException vfe) {
                            throw new ValueFormatException(vfe);
                        }
                    case PropertyType.STRING:
                        try {
                            return valueFactories.getStringFactory().create(value);
                        } catch (org.jboss.dna.graph.property.ValueFormatException vfe) {
                            throw new ValueFormatException(vfe);
                        }
                    case PropertyType.UNDEFINED:
                        return value;

                    default:
                        assert false : "Unexpected JCR property type " + toType;
                        // This should still throw an exception even if assertions are turned off
                        throw new IllegalStateException("Invalid property type " + toType);
                }
            }

        };
    }

    /**
     * {@inheritDoc}
     * 
     * @return false
     * @see javax.jcr.Session#hasPendingChanges()
     */
    public boolean hasPendingChanges() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#impersonate(javax.jcr.Credentials)
     */
    @SuppressWarnings( "unused" )
    public Session impersonate( Credentials credentials ) throws RepositoryException {
        // this is not right:
        // return repository.login(credentials);
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
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
     * @throws IllegalArgumentException if <code>absolutePath</code> is empty or <code>null</code>.
     * @see javax.jcr.Session#itemExists(java.lang.String)
     */
    public boolean itemExists( String absolutePath ) throws RepositoryException {
        try {
            return (getItem(absolutePath) != null);
        } catch (PathNotFoundException error) {
            return false;
        }
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
        LoginContext loginContext = executionContext.getLoginContext();
        if (loginContext != null) {
            try {
                loginContext.logout();
            } catch (LoginException error) {
                // TODO: Change to DnaException once DNA-180 is addressed
                throw new RuntimeException(error);
            }
        }
        isLive = false;
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
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
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
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
    public void save() throws RepositoryException {
        cache.save();
    }
}
