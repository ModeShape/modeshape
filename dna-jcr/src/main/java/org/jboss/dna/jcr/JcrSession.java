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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessControlException;
import java.util.Calendar;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.jcr.Credentials;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
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
import javax.jcr.nodetype.ConstraintViolationException;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.SecurityContext;
import org.jboss.dna.graph.property.Binary;
import org.jboss.dna.graph.property.DateTime;
import org.jboss.dna.graph.property.NamespaceRegistry;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.property.ValueFactories;
import org.jboss.dna.graph.property.basic.LocalNamespaceRegistry;
import org.jboss.dna.graph.session.GraphSession;
import org.jboss.dna.jcr.JcrContentHandler.EnclosingSAXException;
import org.jboss.dna.jcr.JcrContentHandler.SaveMode;
import org.jboss.dna.jcr.JcrNamespaceRegistry.Behavior;
import org.jboss.dna.jcr.SessionCache.JcrPropertyPayload;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * The DNA implementation of a {@link Session JCR Session}.
 */
@NotThreadSafe
class JcrSession implements Session {

    private static final String[] NO_ATTRIBUTES_NAMES = new String[] {};

    public static final String DNA_READ_PERMISSION = "readonly";
    public static final String DNA_WRITE_PERMISSION = "readwrite";
    public static final String DNA_ADMIN_PERMISSION = "admin";

    public static final String DNA_REGISTER_NAMESPACE_PERMISSION = "register_namespace";
    public static final String DNA_REGISTER_TYPE_PERMISSION = "register_type";

    public static final String JCR_ADD_NODE_PERMISSION = "add_node";
    public static final String JCR_SET_PROPERTY_PERMISSION = "set_property";
    public static final String JCR_REMOVE_PERMISSION = "remove";
    public static final String JCR_READ_PERMISSION = "read";

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
        this.sessionRegistry = new JcrNamespaceRegistry(Behavior.JSR170_SESSION, local, workspaceRegistry, this);
        this.rootPath = this.executionContext.getValueFactories().getPathFactory().createRootPath();

        // Set up the graph to use for this session (which uses the session's namespace registry and context) ...
        this.graph = workspace.graph();

        this.cache = new SessionCache(this);
        this.isLive = true;

        assert this.sessionAttributes != null;
        assert this.workspace != null;
        assert this.repository != null;
        assert this.executionContext != null;
        assert this.sessionRegistry != null;
        assert this.graph != null;
        assert this.executionContext.getSecurityContext() != null;
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

    JcrWorkspace workspace() {
        return this.workspace;
    }

    JcrRepository repository() {
        return this.repository;
    }

    Graph.Batch createBatch() {
        return graph.batch();
    }

    Graph graph() {
        return graph;
    }

    String sourceName() {
        return this.repository.getRepositorySourceName();
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
     * Returns whether the authenticated user has the given role.
     * 
     * @param roleName the name of the role to check
     * @param workspaceName the workspace under which the user must have the role. This may be different from the current
     *        workspace.
     * @return true if the user has the role and is logged in; false otherwise
     */
    final boolean hasRole( String roleName,
                           String workspaceName ) {
        SecurityContext context = getExecutionContext().getSecurityContext();

        return context.hasRole(roleName) || context.hasRole(roleName + "." + this.repository.getName())
               || context.hasRole(roleName + "." + this.repository.getName() + "." + workspaceName);
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

        this.checkPermission(executionContext.getValueFactories().getPathFactory().create(path), actions);
    }

    /**
     * Throws an {@link AccessControlException} if the current user does not have permission for all of the named actions in the
     * current workspace, otherwise returns silently.
     * <p>
     * The {@code path} parameter is included for future use and is currently ignored
     * </p>
     * 
     * @param path the path on which the actions are occurring
     * @param actions a comma-delimited list of actions to check
     */
    void checkPermission( Path path,
                          String actions ) {
        checkPermission(this.workspace().getName(), path, actions);
    }

    /**
     * Throws an {@link AccessControlException} if the current user does not have permission for all of the named actions in the
     * named workspace, otherwise returns silently.
     * <p>
     * The {@code path} parameter is included for future use and is currently ignored
     * </p>
     * 
     * @param workspaceName the name of the workspace in which the path exists
     * @param path the path on which the actions are occurring
     * @param actions a comma-delimited list of actions to check
     */
    void checkPermission( String workspaceName,
                          Path path,
                          String actions ) {

        CheckArg.isNotEmpty(actions, "actions");

        boolean hasPermission = true;
        for (String action : actions.split(",")) {
            if (JCR_READ_PERMISSION.equals(action)) {
                hasPermission &= hasRole(DNA_READ_PERMISSION, workspaceName) || hasRole(DNA_WRITE_PERMISSION, workspaceName)
                                 || hasRole(DNA_ADMIN_PERMISSION, workspaceName);
            } else if (DNA_REGISTER_NAMESPACE_PERMISSION.equals(action) || DNA_REGISTER_TYPE_PERMISSION.equals(action)) {
                hasPermission &= hasRole(DNA_ADMIN_PERMISSION, workspaceName);
            } else {
                hasPermission &= hasRole(DNA_ADMIN_PERMISSION, workspaceName) || hasRole(DNA_WRITE_PERMISSION, workspaceName);
            }
        }

        if (hasPermission) return;

        String pathAsString = path != null ? path.getString(this.namespaces()) : "<unknown>";
        throw new AccessControlException(JcrI18n.permissionDenied.text(pathAsString, actions));

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
     * @see javax.jcr.Session#getImportContentHandler(java.lang.String, int)
     */
    public ContentHandler getImportContentHandler( String parentAbsPath,
                                                   int uuidBehavior ) throws PathNotFoundException, RepositoryException {
        Path parentPath = this.executionContext.getValueFactories().getPathFactory().create(parentAbsPath);

        return new JcrContentHandler(this, parentPath, uuidBehavior, SaveMode.SESSION);
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
        // We can't tell from the name, so ask for an item ...
        try {
            return cache.findJcrItem(null, rootPath, path.relativeTo(rootPath));
        } catch (ItemNotFoundException e) {
            throw new PathNotFoundException(e.getMessage(), e);
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
    AbstractJcrNode getNode( Path path ) throws RepositoryException, PathNotFoundException {
        if (path.isRoot()) return cache.findJcrRootNode();
        try {
            return cache.findJcrNode(null, path);
        } catch (ItemNotFoundException e) {
            throw new PathNotFoundException(e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#getNodeByUUID(java.lang.String)
     */
    public Node getNodeByUUID( String uuid ) throws ItemNotFoundException, RepositoryException {
        return cache.findJcrNode(Location.create(UUID.fromString(uuid)));
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
     * @see SecurityContext#getUserName()
     */
    public String getUserID() {
        return executionContext.getSecurityContext().getUserName();
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
                if (!value.isNodeType(JcrMixLexicon.REFERENCEABLE.getString(JcrSession.this.namespaces()))) {
                    throw new RepositoryException(JcrI18n.nodeNotReferenceable.text());
                }
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
     * @see javax.jcr.Session#hasPendingChanges()
     */
    public boolean hasPendingChanges() {
        return cache.hasPendingChanges();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#impersonate(javax.jcr.Credentials)
     */
    public Session impersonate( Credentials credentials ) throws RepositoryException {
        return repository.login(credentials, this.workspace.getName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#importXML(java.lang.String, java.io.InputStream, int)
     */
    public void importXML( String parentAbsPath,
                           InputStream in,
                           int uuidBehavior ) throws IOException, InvalidSerializedDataException, RepositoryException {

        try {
            XMLReader parser = XMLReaderFactory.createXMLReader();

            parser.setContentHandler(getImportContentHandler(parentAbsPath, uuidBehavior));
            parser.parse(new InputSource(in));
        } catch (EnclosingSAXException ese) {
            Exception cause = ese.getException();
            if (cause instanceof ItemExistsException) {
                throw (ItemExistsException)cause;
            } else if (cause instanceof ConstraintViolationException) {
                throw (ConstraintViolationException)cause;
            }
            throw new RepositoryException(cause);
        } catch (SAXParseException se) {
            throw new InvalidSerializedDataException(se);
        } catch (SAXException se) {
            throw new RepositoryException(se);
        }
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

        this.executionContext.getSecurityContext().logout();
        isLive = false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#move(java.lang.String, java.lang.String)
     */
    public void move( String srcAbsPath,
                      String destAbsPath ) throws ItemExistsException, RepositoryException {
        CheckArg.isNotNull(srcAbsPath, "srcAbsPath");
        CheckArg.isNotNull(destAbsPath, "destAbsPath");

        PathFactory pathFactory = executionContext.getValueFactories().getPathFactory();
        Path destPath = pathFactory.create(destAbsPath);

        Path.Segment newNodeName = destPath.getSegment(destPath.size() - 1);
        // Doing a literal test here because the path factory will canonicalize "/node[1]" to "/node"
        if (destAbsPath.endsWith("]")) {
            throw new RepositoryException(JcrI18n.pathCannotHaveSameNameSiblingIndex.text(destAbsPath));
        }

        AbstractJcrNode sourceNode = getNode(pathFactory.create(srcAbsPath));
        AbstractJcrNode newParentNode = getNode(destPath.getParent());

        String newNodeNameAsString = newNodeName.getString(executionContext.getNamespaceRegistry());
        if (newParentNode.hasNode(newNodeName.getString(executionContext.getNamespaceRegistry()))) {
            throw new ItemExistsException(JcrI18n.childNodeAlreadyExists.text(newNodeNameAsString, newParentNode.getPath()));
        }

        newParentNode.editor().moveToBeChild(sourceNode, newNodeName.getName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#refresh(boolean)
     */
    public void refresh( boolean keepChanges ) {
        this.cache.refresh(keepChanges);
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

    /**
     * Get a snapshot of the current session state. This snapshot is immutable and will not reflect any future state changes in
     * the session.
     * 
     * @return the snapshot; never null
     */
    public Snapshot getSnapshot() {
        return new Snapshot(cache.graphSession().getRoot().getSnapshot(false));
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getSnapshot().toString();
    }

    @Immutable
    public class Snapshot {
        private final GraphSession.StructureSnapshot<JcrPropertyPayload> rootSnapshot;

        protected Snapshot( GraphSession.StructureSnapshot<JcrPropertyPayload> snapshot ) {
            this.rootSnapshot = snapshot;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return rootSnapshot.toString();
        }
    }
}
