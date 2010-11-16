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
import java.io.OutputStream;
import java.math.BigDecimal;
import java.security.AccessControlException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import javax.jcr.retention.RetentionManager;
import javax.jcr.security.AccessControlManager;
import javax.jcr.version.VersionException;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.Location;
import org.modeshape.graph.SecurityContext;
import org.modeshape.graph.property.Binary;
import org.modeshape.graph.property.DateTime;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.Reference;
import org.modeshape.graph.property.ReferenceFactory;
import org.modeshape.graph.property.ValueFactories;
import org.modeshape.graph.property.Path.Segment;
import org.modeshape.graph.query.QueryBuilder;
import org.modeshape.graph.query.model.QueryCommand;
import org.modeshape.graph.query.model.TypeSystem;
import org.modeshape.graph.request.InvalidWorkspaceException;
import org.modeshape.graph.session.GraphSession;
import org.modeshape.jcr.JcrContentHandler.EnclosingSAXException;
import org.modeshape.jcr.JcrContentHandler.SaveMode;
import org.modeshape.jcr.JcrNamespaceRegistry.Behavior;
import org.modeshape.jcr.JcrRepository.Option;
import org.modeshape.jcr.SessionCache.JcrPropertyPayload;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * The ModeShape implementation of a {@link Session JCR Session}.
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
    private ExecutionContext executionContext;

    /**
     * The session-specific attributes that came from the {@link SimpleCredentials}' {@link SimpleCredentials#getAttributeNames()}
     */
    private final Map<String, Object> sessionAttributes;

    /**
     * The graph representing this session, which uses the {@link #graph session's graph}.
     */
    private final JcrGraph graph;

    private final SessionCache cache;

    /**
     * A cached instance of the root path.
     */
    private final Path rootPath;

    private boolean isLive;

    private final boolean performReferentialIntegrityChecks;
    /**
     * The locations of the nodes that were (transiently) removed in this session and not yet saved.
     */
    private Set<Location> removedNodes = null;
    /**
     * The UUIDs of the mix:referenceable nodes that were (transiently) removed in this session and not yet saved.
     */
    private Set<String> removedReferenceableNodeUuids = null;

    JcrSession( JcrRepository repository,
                JcrWorkspace workspace,
                ExecutionContext sessionContext,
                NamespaceRegistry globalNamespaceRegistry,
                Map<String, Object> sessionAttributes ) {
        assert repository != null;
        assert workspace != null;
        assert sessionAttributes != null;
        assert sessionContext != null;
        this.repository = repository;
        this.sessionAttributes = sessionAttributes;
        this.workspace = workspace;

        // Create an execution context for this session, which should use the local namespace registry ...
        this.executionContext = sessionContext;
        NamespaceRegistry local = sessionContext.getNamespaceRegistry();
        this.sessionRegistry = new JcrNamespaceRegistry(Behavior.SESSION, local, globalNamespaceRegistry, this);
        this.rootPath = this.executionContext.getValueFactories().getPathFactory().createRootPath();

        // Set up the graph to use for this session (which uses the session's namespace registry and context) ...
        this.graph = workspace.graph();

        this.cache = new SessionCache(this);
        this.isLive = true;

        this.performReferentialIntegrityChecks = Boolean.valueOf(repository.getOptions()
                                                                           .get(Option.PERFORM_REFERENTIAL_INTEGRITY_CHECKS))
                                                        .booleanValue();

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

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#isLive()
     */
    public boolean isLive() {
        return isLive;
    }

    /**
     * Method that verifies that this session is still {@link #isLive() live}.
     * 
     * @throws RepositoryException if session has been closed and is no longer usable.
     */
    final void checkLive() throws RepositoryException {
        if (!isLive()) {
            throw new RepositoryException(JcrI18n.sessionIsNotActive.text(sessionId()));
        }
    }

    ExecutionContext getExecutionContext() {
        return this.executionContext;
    }

    void setSessionData( String key,
                         String value ) {
        // This returns the same instance iff the <key,value> would not alter the current context ...
        this.executionContext = this.executionContext.with(key, value);
        this.graph.setContext(this.executionContext);
    }

    String sessionId() {
        return this.executionContext.getId();
    }

    JcrLockManager lockManager() {
        return workspace.lockManager();
    }

    JcrNodeTypeManager nodeTypeManager() {
        return this.workspace.nodeTypeManager();
    }

    NamespaceRegistry namespaces() {
        return this.executionContext.getNamespaceRegistry();
    }

    void signalNamespaceChanges( boolean global ) {
        nodeTypeManager().signalNamespaceChanges();
        if (global) repository.getRepositoryTypeManager().signalNamespaceChanges();
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

    Path pathFor( String path,
                  String parameterName ) throws RepositoryException {
        try {
            return this.executionContext.getValueFactories().getPathFactory().create(path);

        } catch (org.modeshape.graph.property.ValueFormatException vfe) {
            throw new RepositoryException(JcrI18n.invalidPathParameter.text(path, parameterName), vfe);
        }
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
     * @return a copy of the session attributes for this session
     */
    Map<String, Object> sessionAttributes() {
        return new HashMap<String, Object>(sessionAttributes);
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
    public String[] getNamespacePrefixes() throws RepositoryException {
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
     * @see javax.jcr.Session#addLockToken(java.lang.String)
     */
    public void addLockToken( String lt ) {
        CheckArg.isNotNull(lt, "lock token");

        try {
            lockManager().addLockToken(lt);
        } catch (LockException le) {
            // For backwards compatibility (and API compatibility), the LockExceptions from the LockManager need to get swallowed
        }
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
        if (context.hasRole(roleName)) return true;
        roleName = roleName + "." + this.repository.getRepositorySourceName();
        if (context.hasRole(roleName)) return true;
        roleName = roleName + "." + workspaceName;
        return context.hasRole(roleName);
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
        if (hasPermission(workspaceName, path, actions)) return;

        String pathAsString = path != null ? path.getString(this.namespaces()) : "<unknown>";
        throw new AccessControlException(JcrI18n.permissionDenied.text(pathAsString, actions));
    }

    /**
     * A companion method to {@link #checkPermission(String, String)} that returns false (instead of throwing an exception) if the
     * current session doesn't have sufficient privileges to perform the given list of actions at the given path.
     * 
     * @param path the path at which the privileges are to be checked
     * @param actions a comma-delimited list of actions to check
     * @return true if the current session has sufficient privileges to perform all of the actions on the the given path; false
     *         otherwise
     * @see javax.jcr.Session#checkPermission(java.lang.String, java.lang.String)
     */
    public boolean hasPermission( String path,
                                  String actions ) {
        CheckArg.isNotEmpty(path, "path");

        return hasPermission(this.workspace().getName(),
                             executionContext.getValueFactories().getPathFactory().create(path),
                             actions);
    }

    private boolean hasPermission( String workspaceName,
                                   Path path,
                                   String actions ) {
        CheckArg.isNotEmpty(actions, "actions");

        boolean hasPermission = true;
        for (String action : actions.split(",")) {
            if (ModeShapePermissions.READ.equals(action)) {
                hasPermission &= hasRole(ModeShapeRoles.READONLY, workspaceName)
                                 || hasRole(ModeShapeRoles.READWRITE, workspaceName)
                                 || hasRole(ModeShapeRoles.ADMIN, workspaceName);
            } else if (ModeShapePermissions.REGISTER_NAMESPACE.equals(action)
                       || ModeShapePermissions.REGISTER_TYPE.equals(action) || ModeShapePermissions.UNLOCK_ANY.equals(action)
                       || ModeShapePermissions.CREATE_WORKSPACE.equals(action)
                       || ModeShapePermissions.DELETE_WORKSPACE.equals(action)) {
                hasPermission &= hasRole(ModeShapeRoles.ADMIN, workspaceName);
            } else {
                hasPermission &= hasRole(ModeShapeRoles.ADMIN, workspaceName) || hasRole(ModeShapeRoles.READWRITE, workspaceName);
            }
        }
        return hasPermission;
    }

    /**
     * Makes a "best effort" determination of whether the given method can be successfully called on the given target with the
     * given arguments. A return value of {@code false} indicates that the method would not succeed. A return value of {@code
     * true} indicates that the method <i>might</i> succeed.
     * 
     * @param methodName the method to invoke; may not be null
     * @param target the object on which to invoke it; may not be null
     * @param arguments the arguments to pass to the method; varies depending on the method
     * @return true if the given method can be determined to be supported, or false otherwise
     * @throws IllegalArgumentException
     * @throws RepositoryException
     */
    public boolean hasCapability( String methodName,
                                  Object target,
                                  Object[] arguments ) throws IllegalArgumentException, RepositoryException {
        CheckArg.isNotEmpty(methodName, "methodName");
        CheckArg.isNotNull(target, "target");

        if (target instanceof AbstractJcrNode) {
            AbstractJcrNode node = (AbstractJcrNode)target;
            if ("addNode".equals(methodName)) {
                CheckArg.hasSizeOfAtLeast(arguments, 1, "arguments");
                CheckArg.hasSizeOfAtMost(arguments, 2, "arguments");
                CheckArg.isInstanceOf(arguments[0], String.class, "arguments[0]");

                String relPath = (String)arguments[0];
                String primaryNodeTypeName = null;
                if (arguments.length > 1) {
                    CheckArg.isInstanceOf(arguments[1], String.class, "arguments[1]");
                    primaryNodeTypeName = (String)arguments[1];
                }
                return node.canAddNode(relPath, primaryNodeTypeName);
            }
        }
        return true;
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
        boolean retainLifecycleInfo = getRepository().getDescriptorValue(Repository.OPTION_LIFECYCLE_SUPPORTED).getBoolean();
        boolean retainRetentionInfo = getRepository().getDescriptorValue(Repository.OPTION_RETENTION_SUPPORTED).getBoolean();
        return new JcrContentHandler(this, parentPath, uuidBehavior, SaveMode.SESSION, retainRetentionInfo, retainLifecycleInfo);
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
        if (path.isIdentifier() || path.getLastSegment().hasIndex()) {
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
     * Throws a {@code RepositoryException} if {@code path} is not an absolute path, otherwise returns silently.
     * 
     * @param pathAsString the string representation of the path
     * @param path the path to check
     * @throws RepositoryException if {@code !path.isAbsolute()}
     */
    private void checkAbsolute( String pathAsString,
                                Path path ) throws RepositoryException {
        if (!path.isAbsolute()) {
            throw new RepositoryException(JcrI18n.invalidAbsolutePath.text(pathAsString));
        }
    }

    /**
     * @param absolutePath an absolute path
     * @return the specified node
     * @throws IllegalArgumentException if <code>absolutePath</code> is empty or <code>null</code>.
     * @throws PathNotFoundException If no accessible node is found at the specifed path
     * @throws RepositoryException if another error occurs
     * @see javax.jcr.Session#getItem(java.lang.String)
     */
    public AbstractJcrNode getNode( String absolutePath ) throws PathNotFoundException, RepositoryException {
        CheckArg.isNotEmpty(absolutePath, "absolutePath");
        // Return root node if path is "/"
        Path path = executionContext.getValueFactories().getPathFactory().create(absolutePath);
        if (path.isRoot()) {
            return getRootNode();
        }

        checkAbsolute(absolutePath, path);

        return getNode(path);
    }

    /**
     * Returns true if a node exists at the given path and is accessible to the current user.
     * 
     * @param absolutePath the absolute path to the node
     * @return true if a node exists at absolute path and is accessible to the current user.
     * @throws IllegalArgumentException if <code>absolutePath</code> is empty or <code>null</code>.
     * @throws PathNotFoundException If no accessible node is found at the specifed path
     * @throws RepositoryException if another error occurs
     */
    public boolean nodeExists( String absolutePath ) throws PathNotFoundException, RepositoryException {
        CheckArg.isNotEmpty(absolutePath, "absolutePath");
        // Return root node if path is "/"
        Path path = executionContext.getValueFactories().getPathFactory().create(absolutePath);
        if (path.isRoot()) {
            return true;
        }

        checkAbsolute(absolutePath, path);

        try {
            cache.findJcrNode(null, path);
            return true;
        } catch (ItemNotFoundException e) {
            return false;
        }
    }

    /**
     * @param absolutePath an absolute path
     * @return the specified node
     * @throws IllegalArgumentException if <code>absolutePath</code> is empty or <code>null</code>.
     * @throws PathNotFoundException If no accessible node is found at the specifed path
     * @throws RepositoryException if another error occurs
     * @see javax.jcr.Session#getItem(java.lang.String)
     */
    public AbstractJcrProperty getProperty( String absolutePath ) throws PathNotFoundException, RepositoryException {
        CheckArg.isNotEmpty(absolutePath, "absolutePath");
        // Return root node if path is "/"
        Path path = pathFor(absolutePath, "absolutePath");
        if (path.isRoot()) {
            throw new PathNotFoundException(JcrI18n.rootNodeIsNotProperty.text());
        }
        if (path.isIdentifier()) {
            throw new PathNotFoundException(JcrI18n.identifierPathNeverReferencesProperty.text());
        }

        checkAbsolute(absolutePath, path);

        Segment lastSegment = path.getLastSegment();
        if (lastSegment.hasIndex()) {
            throw new RepositoryException(JcrI18n.pathCannotHaveSameNameSiblingIndex.text(absolutePath));
        }

        // This will throw a PNFE if the parent path does not exist
        AbstractJcrNode parentNode = getNode(path.getParent());
        AbstractJcrProperty property = parentNode.getProperty(lastSegment.getName());

        if (property == null) {
            throw new PathNotFoundException(GraphI18n.pathNotFoundExceptionLowestExistingLocationFound.text(absolutePath,
                                                                                                            parentNode.getPath()));
        }
        return property;
    }

    /**
     * Returns true if a property exists at the given path and is accessible to the current user.
     * 
     * @param absolutePath the absolute path to the property
     * @return true if a property exists at absolute path and is accessible to the current user.
     * @throws IllegalArgumentException if <code>absolutePath</code> is empty or <code>null</code>.
     * @throws RepositoryException if another error occurs
     */
    public boolean propertyExists( String absolutePath ) throws RepositoryException {
        CheckArg.isNotEmpty(absolutePath, "absolutePath");
        // Return root node if path is "/"
        Path path = pathFor(absolutePath, "absolutePath");
        if (path.isRoot() || path.isIdentifier()) {
            return false;
        }

        checkAbsolute(absolutePath, path);

        Segment lastSegment = path.getLastSegment();
        if (lastSegment.hasIndex()) {
            throw new RepositoryException(JcrI18n.pathCannotHaveSameNameSiblingIndex.text(absolutePath));
        }

        try {
            // This will throw a PNFE if the parent path does not exist
            AbstractJcrNode parentNode = getNode(path.getParent());
            return parentNode.hasProperty(lastSegment.getName());
        } catch (PathNotFoundException pnfe) {
            return false;
        }
    }

    public void removeItem( String absolutePath ) throws RepositoryException {
        Item item = getItem(absolutePath);
        item.remove();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#getLockTokens()
     */
    public String[] getLockTokens() {
        return lockManager().getLockTokens();
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
            if (path.isIdentifier()) {
                // Convert the path to a UUID ...
                try {
                    UUID uuid = executionContext.getValueFactories().getUuidFactory().create(path);
                    return cache.findJcrNode(Location.create(uuid));
                } catch (org.modeshape.graph.property.ValueFormatException e) {
                    // The identifier path didn't contain a UUID (but another identifier form) ...
                    String pathStr = executionContext.getValueFactories().getStringFactory().create(path);
                    throw new PathNotFoundException(JcrI18n.identifierPathContainedUnsupportedIdentifierFormat.text(pathStr));
                }
            }
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
    public AbstractJcrNode getNodeByUUID( String uuid ) throws ItemNotFoundException, RepositoryException {
        return cache.findJcrNode(Location.create(UUID.fromString(uuid)));
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#getNodeByIdentifier(java.lang.String)
     */
    @Override
    public AbstractJcrNode getNodeByIdentifier( String id ) throws ItemNotFoundException, RepositoryException {
        // Attempt to create a UUID from the identifier ...
        try {
            return cache.findJcrNode(Location.create(UUID.fromString(id)));
        } catch (IllegalArgumentException e) {
            try {
                // See if it's a path ...
                PathFactory pathFactory = executionContext.getValueFactories().getPathFactory();
                Path path = pathFactory.create(id);
                return getNode(path);
            } catch (org.modeshape.graph.property.ValueFormatException e2) {
                // It's not a path either ...
                throw new RepositoryException(JcrI18n.identifierPathContainedUnsupportedIdentifierFormat.text(id));
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#getRootNode()
     */
    public AbstractJcrNode getRootNode() throws RepositoryException {
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

            @Override
            public Value createValue( String value,
                                      int propertyType ) throws ValueFormatException {
                return new JcrValue(valueFactories, sessionCache, propertyType, convertValueToType(value, propertyType));
            }

            @Override
            public Value createValue( Node value ) throws RepositoryException {
                if (!value.isNodeType(JcrMixLexicon.REFERENCEABLE.getString(JcrSession.this.namespaces()))) {
                    throw new RepositoryException(JcrI18n.nodeNotReferenceable.text());
                }
                Reference ref = valueFactories.getReferenceFactory().create(value.getIdentifier());
                return new JcrValue(valueFactories, sessionCache, PropertyType.REFERENCE, ref);
            }

            @Override
            public Value createValue( Node value,
                                      boolean weak ) throws RepositoryException {
                if (!value.isNodeType(JcrMixLexicon.REFERENCEABLE.getString(JcrSession.this.namespaces()))) {
                    throw new RepositoryException(JcrI18n.nodeNotReferenceable.text());
                }
                ReferenceFactory factory = weak ? valueFactories.getWeakReferenceFactory() : valueFactories.getReferenceFactory();
                int refType = weak ? PropertyType.WEAKREFERENCE : PropertyType.REFERENCE;
                Reference ref = factory.create(value.getIdentifier());
                return new JcrValue(valueFactories, sessionCache, refType, ref);
            }

            @Override
            public Value createValue( javax.jcr.Binary value ) {
                return new JcrValue(valueFactories, sessionCache, PropertyType.BINARY, value);
            }

            @Override
            public Value createValue( InputStream value ) {
                Binary binary = valueFactories.getBinaryFactory().create(value);
                return new JcrValue(valueFactories, sessionCache, PropertyType.BINARY, binary);
            }

            @Override
            public javax.jcr.Binary createBinary( InputStream value ) {
                Binary binary = valueFactories.getBinaryFactory().create(value);
                return new JcrBinary(binary);
            }

            @Override
            public Value createValue( Calendar value ) {
                DateTime dateTime = valueFactories.getDateFactory().create(value);
                return new JcrValue(valueFactories, sessionCache, PropertyType.DATE, dateTime);
            }

            @Override
            public Value createValue( boolean value ) {
                return new JcrValue(valueFactories, sessionCache, PropertyType.BOOLEAN, value);
            }

            @Override
            public Value createValue( double value ) {
                return new JcrValue(valueFactories, sessionCache, PropertyType.DOUBLE, value);
            }

            @Override
            public Value createValue( long value ) {
                return new JcrValue(valueFactories, sessionCache, PropertyType.LONG, value);
            }

            @Override
            public Value createValue( String value ) {
                return new JcrValue(valueFactories, sessionCache, PropertyType.STRING, value);
            }

            @Override
            public Value createValue( BigDecimal value ) {
                return new JcrValue(valueFactories, sessionCache, PropertyType.DECIMAL, value);
            }

            Object convertValueToType( Object value,
                                       int toType ) throws ValueFormatException {
                switch (toType) {
                    case PropertyType.BOOLEAN:
                        try {
                            return valueFactories.getBooleanFactory().create(value);
                        } catch (org.modeshape.graph.property.ValueFormatException vfe) {
                            throw new ValueFormatException(vfe);
                        }

                    case PropertyType.DATE:
                        try {
                            return valueFactories.getDateFactory().create(value);
                        } catch (org.modeshape.graph.property.ValueFormatException vfe) {
                            throw new ValueFormatException(vfe);
                        }

                    case PropertyType.NAME:
                        try {
                            return valueFactories.getNameFactory().create(value);
                        } catch (org.modeshape.graph.property.ValueFormatException vfe) {
                            throw new ValueFormatException(vfe);
                        }

                    case PropertyType.PATH:
                        try {
                            return valueFactories.getPathFactory().create(value);
                        } catch (org.modeshape.graph.property.ValueFormatException vfe) {
                            throw new ValueFormatException(vfe);
                        }

                    case PropertyType.REFERENCE:
                    case PropertyType.WEAKREFERENCE:
                        try {
                            return valueFactories.getReferenceFactory().create(value);
                        } catch (org.modeshape.graph.property.ValueFormatException vfe) {
                            throw new ValueFormatException(vfe);
                        }
                    case PropertyType.DOUBLE:
                        try {
                            return valueFactories.getDoubleFactory().create(value);
                        } catch (org.modeshape.graph.property.ValueFormatException vfe) {
                            throw new ValueFormatException(vfe);
                        }
                    case PropertyType.LONG:
                        try {
                            return valueFactories.getLongFactory().create(value);
                        } catch (org.modeshape.graph.property.ValueFormatException vfe) {
                            throw new ValueFormatException(vfe);
                        }
                    case PropertyType.DECIMAL:
                        try {
                            return valueFactories.getDecimalFactory().create(value);
                        } catch (org.modeshape.graph.property.ValueFormatException vfe) {
                            throw new ValueFormatException(vfe);
                        }
                    case PropertyType.URI:
                        try {
                            return valueFactories.getUriFactory().create(value);
                        } catch (org.modeshape.graph.property.ValueFormatException vfe) {
                            throw new ValueFormatException(vfe);
                        }

                        // Anything can be converted to these types
                    case PropertyType.BINARY:
                        try {
                            return valueFactories.getBinaryFactory().create(value);
                        } catch (org.modeshape.graph.property.ValueFormatException vfe) {
                            throw new ValueFormatException(vfe);
                        }
                    case PropertyType.STRING:
                        try {
                            return valueFactories.getStringFactory().create(value);
                        } catch (org.modeshape.graph.property.ValueFormatException vfe) {
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
     * Returns a new {@link JcrSession session} that uses the same security information to create a session that points to the
     * named workspace.
     * 
     * @param workspaceName the name of the workspace to connect to
     * @return a new session that uses the named workspace
     * @throws RepositoryException if an error occurs creating the session
     */
    JcrSession with( String workspaceName ) throws RepositoryException {
        return repository.sessionForContext(executionContext, workspaceName, sessionAttributes);
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
            } else if (cause instanceof VersionException) {
                throw (VersionException)cause;
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
        terminate(true);
    }

    /**
     * This method is called by {@link #logout()} and by {@link JcrRepository#terminateAllSessions()}. It should not be called
     * from anywhere else.
     * 
     * @param removeFromActiveSession true if the session should be removed from the active session list
     */
    void terminate( boolean removeFromActiveSession ) {
        if (!isLive()) {
            return;
        }

        isLive = false;
        this.workspace().observationManager().removeAllEventListeners();
        this.lockManager().cleanLocks();
        if (removeFromActiveSession) this.repository.sessionLoggedOut(this);
        this.executionContext.getSecurityContext().logout();
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

        // Doing a literal test here because the path factory will canonicalize "/node[1]" to "/node"
        if (destAbsPath.endsWith("]")) {
            throw new RepositoryException(JcrI18n.pathCannotHaveSameNameSiblingIndex.text(destAbsPath));
        }

        Path.Segment newNodeName = null;
        AbstractJcrNode sourceNode = getNode(pathFactory.create(srcAbsPath));
        AbstractJcrNode newParentNode = null;
        if (destPath.isIdentifier()) {
            AbstractJcrNode existingDestNode = getNode(destPath);
            newParentNode = existingDestNode.getParent();
            newNodeName = existingDestNode.segment();
        } else {
            newParentNode = getNode(destPath.getParent());
            newNodeName = destPath.getSegment(destPath.size() - 1);
        }

        if (sourceNode.isLocked() && !sourceNode.getLock().isLockOwningSession()) {
            javax.jcr.lock.Lock sourceLock = sourceNode.getLock();
            if (sourceLock != null && sourceLock.getLockToken() == null) {
                throw new LockException(JcrI18n.lockTokenNotHeld.text(srcAbsPath));
            }
        }

        if (newParentNode.isLocked() && !newParentNode.getLock().isLockOwningSession()) {
            javax.jcr.lock.Lock newParentLock = newParentNode.getLock();
            if (newParentLock != null && newParentLock.getLockToken() == null) {
                throw new LockException(JcrI18n.lockTokenNotHeld.text(destAbsPath));
            }
        }

        if (!sourceNode.getParent().isCheckedOut()) {
            throw new VersionException(JcrI18n.nodeIsCheckedIn.text(sourceNode.getPath()));
        }

        if (!newParentNode.isCheckedOut()) {
            throw new VersionException(JcrI18n.nodeIsCheckedIn.text(newParentNode.getPath()));
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
     * @see javax.jcr.Session#removeLockToken(java.lang.String)
     */
    public void removeLockToken( String lockToken ) {
        CheckArg.isNotNull(lockToken, "lock token");
        // A LockException is thrown if the lock associated with the specified lock token is session-scoped.
        try {
            lockManager().removeLockToken(lockToken);
        } catch (LockException le) {
            // For backwards compatibility (and API compatibility), the LockExceptions from the LockManager need to get swallowed
        }
    }

    void recordRemoval( Location location ) throws RepositoryException {
        if (!performReferentialIntegrityChecks) {
            return;
        }
        if (removedNodes == null) {
            removedNodes = new HashSet<Location>();
            removedReferenceableNodeUuids = new HashSet<String>();
        }

        // Find the UUIDs of all of the mix:referenceable nodes that are below this node being removed ...
        Path path = location.getPath();
        org.modeshape.graph.property.ValueFactory<String> stringFactory = executionContext.getValueFactories().getStringFactory();
        String pathStr = stringFactory.create(path);
        int sns = path.getLastSegment().getIndex();
        if (sns == Path.DEFAULT_INDEX) pathStr = pathStr + "[1]";

        TypeSystem typeSystem = executionContext.getValueFactories().getTypeSystem();
        QueryBuilder builder = new QueryBuilder(typeSystem);
        QueryCommand query = builder.select("jcr:uuid")
                                    .from("mix:referenceable AS referenceable")
                                    .where()
                                    .path("referenceable")
                                    .isLike(pathStr + "%")
                                    .end()
                                    .query();
        JcrQueryManager queryManager = workspace().queryManager();
        Query jcrQuery = queryManager.createQuery(query);
        QueryResult result = jcrQuery.execute();
        RowIterator rows = result.getRows();
        while (rows.hasNext()) {
            Row row = rows.nextRow();
            String uuid = row.getValue("jcr:uuid").getString();
            if (uuid != null) removedReferenceableNodeUuids.add(uuid);
        }

        // Now record that this location is being removed ...
        Set<Location> extras = null;
        for (Location alreadyDeleted : removedNodes) {
            Path alreadyDeletedPath = alreadyDeleted.getPath();
            if (alreadyDeletedPath.isAtOrAbove(path)) {
                // Already covered by the alreadyDeleted location ...
                return;
            }
            if (alreadyDeletedPath.isDecendantOf(path)) {
                // The path being deleted is above the path that was already deleted, so remove the already-deleted one ...
                if (extras == null) {
                    extras = new HashSet<Location>();
                }
                extras.add(alreadyDeleted);
            }
        }
        // Not covered by any already-deleted location, so add it ...
        removedNodes.add(location);
        if (extras != null) {
            // Remove the nodes that will be covered by the node being deleted now ...
            removedNodes.removeAll(extras);
        }
    }

    boolean wasRemovedInSession( Location location ) {
        if (removedNodes == null) return false;
        if (removedNodes.contains(location)) return true;
        Path path = location.getPath();
        for (Location removed : removedNodes) {
            if (removed.getPath().isAtOrAbove(path)) return true;
        }
        return false;
    }

    boolean wasRemovedInSession( UUID uuid ) {
        if (removedReferenceableNodeUuids == null) return false;
        return removedReferenceableNodeUuids.contains(uuid);

    }

    /**
     * Determine whether there is at least one other node outside this branch that has a reference to nodes within the branch
     * rooted by this node.
     * 
     * @param subgraphRoot the root of the subgraph under which the references should be checked, or null if the root node should
     *        be used (meaning all references in the workspace should be checked)
     * @throws ReferentialIntegrityException if the changes would leave referential integrity problems
     * @throws RepositoryException if an error occurs while obtaining the information
     */
    void checkReferentialIntegrityOfChanges( AbstractJcrNode subgraphRoot )
        throws ReferentialIntegrityException, RepositoryException {
        if (removedNodes == null) return;
        if (removedReferenceableNodeUuids.isEmpty()) return;

        if (removedNodes.size() == 1 && removedNodes.iterator().next().getPath().isRoot()) {
            // The root node is being removed, so there will be no referencing nodes remaining ...
            return;
        }

        String subgraphPath = null;
        if (subgraphRoot != null) {
            subgraphPath = subgraphRoot.getPath();
            if (subgraphRoot.getIndex() == Path.DEFAULT_INDEX) subgraphPath = subgraphPath + "[1]";
        }

        // Build one (or several) queries to find the first reference to any 'mix:referenceable' nodes
        // that have been (transiently) removed from the session ...
        int maxBatchSize = 100;
        Set<Object> someUuidsInBranch = new HashSet<Object>();
        Iterator<String> uuidIter = removedReferenceableNodeUuids.iterator();
        while (uuidIter.hasNext()) {
            // Accumulate the next 100 UUIDs of referenceable nodes inside this branch ...
            while (uuidIter.hasNext() && someUuidsInBranch.size() <= maxBatchSize) {
                String uuid = uuidIter.next();
                someUuidsInBranch.add(uuid);
            }
            assert !someUuidsInBranch.isEmpty();
            // Now issue a query to see if any nodes outside this branch references these referenceable nodes ...
            TypeSystem typeSystem = executionContext.getValueFactories().getTypeSystem();
            QueryBuilder builder = new QueryBuilder(typeSystem);
            QueryCommand query = null;
            if (subgraphPath != null) {
                query = builder.select("jcr:primaryType")
                               .fromAllNodesAs("allNodes")
                               .where()
                               .strongReferenceValue("allNodes")
                               .isIn(someUuidsInBranch)
                               .and()
                               .path("allNodes")
                               .isLike(subgraphPath + "%")
                               .end()
                               .query();
            } else {
                query = builder.select("jcr:primaryType")
                               .fromAllNodesAs("allNodes")
                               .where()
                               .strongReferenceValue("allNodes")
                               .isIn(someUuidsInBranch)
                               .end()
                               .query();
            }
            Query jcrQuery = workspace().queryManager().createQuery(query);
            // The nodes that have been (transiently) deleted will not appear in these results ...
            QueryResult result = jcrQuery.execute();
            NodeIterator referencingNodes = result.getNodes();
            while (referencingNodes.hasNext()) {
                // The REFERENCE property (or properties) may have been removed in this session,
                // so check whether they referencing nodes have been loaded into the session ...
                AbstractJcrNode referencingNode = (AbstractJcrNode)referencingNodes.nextNode();
                if (!referencingNode.nodeInfo().isChanged(false)) {
                    // This node has not changed, so there is at least one reference; we can stop here ...
                    throw new ReferentialIntegrityException();
                }
                // This node has changed. This node is okay as long as the node no longer
                // contains a REFERENCE property to any of the removed nodes...
                PropertyIterator propIter = referencingNode.getProperties();
                while (propIter.hasNext()) {
                    Property property = propIter.nextProperty();
                    if (property.getType() != PropertyType.REFERENCE) return;
                    if (property.isMultiple()) {
                        for (Value value : property.getValues()) {
                            String referencedUuid = value.getString();
                            if (removedReferenceableNodeUuids.contains(referencedUuid)) {
                                // This node still has a reference to a node being removed ...
                                throw new ReferentialIntegrityException();
                            }
                        }
                    } else {
                        String referencedUuid = property.getValue().getString();
                        if (removedReferenceableNodeUuids.contains(referencedUuid)) {
                            // This node still has a reference to a node being removed ...
                            throw new ReferentialIntegrityException();
                        }
                    }
                }
            }
            someUuidsInBranch.clear();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#save()
     */
    public void save() throws RepositoryException {
        checkReferentialIntegrityOfChanges(null);
        removedNodes = null;
        cache.save();
    }

    /**
     * Crawl and index the content in this workspace.
     * 
     * @throws IllegalArgumentException if the workspace is null
     * @throws InvalidWorkspaceException if there is no workspace with the supplied name
     */
    public void reindexContent() {
        repository().queryManager().reindexContent(workspace());
    }

    /**
     * Crawl and index the content starting at the supplied path in this workspace, to the designated depth.
     * 
     * @param path the path of the content to be indexed
     * @param depth the depth of the content to be indexed
     * @throws IllegalArgumentException if the workspace or path are null, or if the depth is less than 1
     * @throws InvalidWorkspaceException if there is no workspace with the supplied name
     */
    public void reindexContent( String path,
                                int depth ) {
        repository().queryManager().reindexContent(workspace(), path, depth);
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

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#getAccessControlManager()
     */
    @Override
    public AccessControlManager getAccessControlManager() throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Session#getRetentionManager()
     */
    @Override
    public RetentionManager getRetentionManager() throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
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
