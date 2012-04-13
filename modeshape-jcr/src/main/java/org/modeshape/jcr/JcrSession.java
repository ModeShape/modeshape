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
import java.security.AccessControlException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.jcr.AccessDeniedException;
import javax.jcr.Credentials;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.LoginException;
import javax.jcr.NamespaceException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.retention.RetentionManager;
import javax.jcr.security.AccessControlManager;
import javax.jcr.version.VersionException;
import org.infinispan.schematic.SchematicEntry;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.Logger;
import org.modeshape.jcr.AbstractJcrNode.Type;
import org.modeshape.jcr.JcrContentHandler.EnclosingSAXException;
import org.modeshape.jcr.JcrNamespaceRegistry.Behavior;
import org.modeshape.jcr.JcrRepository.RunningState;
import org.modeshape.jcr.RepositoryNodeTypeManager.NodeTypes;
import org.modeshape.jcr.api.monitor.DurationMetric;
import org.modeshape.jcr.api.monitor.ValueMetric;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.ChildReference;
import org.modeshape.jcr.cache.ChildReferences;
import org.modeshape.jcr.cache.DocumentAlreadyExistsException;
import org.modeshape.jcr.cache.DocumentNotFoundException;
import org.modeshape.jcr.cache.MutableCachedNode;
import org.modeshape.jcr.cache.NodeCache;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.NodeNotFoundException;
import org.modeshape.jcr.cache.SessionCache;
import org.modeshape.jcr.cache.SessionCache.SaveContext;
import org.modeshape.jcr.cache.WorkspaceNotFoundException;
import org.modeshape.jcr.cache.WrappedException;
import org.modeshape.jcr.security.AuthorizationProvider;
import org.modeshape.jcr.security.SecurityContext;
import org.modeshape.jcr.value.DateTimeFactory;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NameFactory;
import org.modeshape.jcr.value.NamespaceRegistry;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Path.Segment;
import org.modeshape.jcr.value.PathFactory;
import org.modeshape.jcr.value.PropertyFactory;
import org.modeshape.jcr.value.Reference;
import org.modeshape.jcr.value.ReferenceFactory;
import org.modeshape.jcr.value.UuidFactory;
import org.modeshape.jcr.value.basic.LocalNamespaceRegistry;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * 
 */
public class JcrSession implements Session {

    private static final String[] NO_ATTRIBUTES_NAMES = new String[] {};

    private final JcrRepository repository;
    private final SessionCache cache;
    private final JcrRootNode rootNode;
    private final ConcurrentMap<NodeKey, AbstractJcrNode> jcrNodes = new ConcurrentHashMap<NodeKey, AbstractJcrNode>();
    private final Map<String, Object> sessionAttributes;
    private final JcrWorkspace workspace;
    private final JcrNamespaceRegistry sessionRegistry;
    private final AtomicReference<Map<NodeKey, NodeKey>> baseVersionKeys = new AtomicReference<Map<NodeKey, NodeKey>>();
    private volatile JcrValueFactory valueFactory;
    private volatile boolean isLive = true;
    private final long nanosCreated;

    private ExecutionContext context;

    protected JcrSession( JcrRepository repository,
                          String workspaceName,
                          ExecutionContext context,
                          Map<String, Object> sessionAttributes,
                          boolean readOnly ) {
        this.repository = repository;

        // Create an execution context for this session that uses a local namespace registry ...
        final NamespaceRegistry globalNamespaceRegistry = context.getNamespaceRegistry(); // thread-safe!
        final LocalNamespaceRegistry localRegistry = new LocalNamespaceRegistry(globalNamespaceRegistry); // not-thread-safe!
        this.context = context.with(localRegistry);
        this.sessionRegistry = new JcrNamespaceRegistry(Behavior.SESSION, localRegistry, globalNamespaceRegistry, this);
        this.workspace = new JcrWorkspace(this, workspaceName);

        // Create the session cache ...
        this.cache = repository.repositoryCache().createSession(context, workspaceName, readOnly);
        this.rootNode = new JcrRootNode(this, this.cache.getRootKey());
        this.jcrNodes.put(this.rootNode.key(), this.rootNode);
        this.sessionAttributes = sessionAttributes != null ? sessionAttributes : Collections.<String, Object>emptyMap();

        // Pre-cache all of the namespaces to be a snapshot of what's in the global registry at this time.
        // This behavior is specified in Section 3.5.2 of the JCR 2.0 specification.
        localRegistry.getNamespaces();

        // Increment the statistics ...
        this.nanosCreated = System.nanoTime();
        repository.statistics().increment(ValueMetric.SESSION_COUNT);
    }

    protected JcrSession( JcrSession original,
                          boolean readOnly ) {
        // Most of the components can be reused from the original session ...
        this.repository = original.repository;
        this.context = original.context;
        this.sessionRegistry = original.sessionRegistry;
        this.valueFactory = original.valueFactory;
        this.sessionAttributes = original.sessionAttributes;
        this.workspace = original.workspace;

        // Create a new session cache and root node ...
        this.cache = repository.repositoryCache().createSession(context, this.workspace.getName(), readOnly);
        this.rootNode = new JcrRootNode(this, this.cache.getRootKey());
        this.jcrNodes.put(this.rootNode.key(), this.rootNode);

        // Increment the statistics ...
        this.nanosCreated = System.nanoTime();
        repository.statistics().increment(ValueMetric.SESSION_COUNT);
    }

    final JcrWorkspace workspace() {
        return workspace;
    }

    final JcrRepository repository() {
        return repository;
    }

    /**
     * This method is called by {@link #logout()} and by {@link JcrRepository#shutdown()}. It should not be called from anywhere
     * else.
     * 
     * @param removeFromActiveSession true if the session should be removed from the active session list
     */
    void terminate( boolean removeFromActiveSession ) {
        if (!isLive()) {
            return;
        }

        isLive = false;

        observationManager().removeAllEventListeners();

        try {
            lockManager().cleanLocks();
        } catch (RepositoryException e) {
            // This can only happen if the session is not live, which is checked above ...
            Logger.getLogger(getClass()).error(e, JcrI18n.unexpectedException, e.getMessage());
        }
        if (removeFromActiveSession) this.repository.runningState().removeSession(this);
        this.context.getSecurityContext().logout();
    }

    protected SchematicEntry entryForNode( NodeKey nodeKey ) throws RepositoryException {
        SchematicEntry entry = repository.database().get(nodeKey.toString());
        if (entry == null) {
            throw new PathNotFoundException(nodeKey.toString());
        }
        return entry;
    }

    final String workspaceName() {
        return workspace.getName();
    }

    final String sessionId() {
        return context.getId();
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

    NamespaceRegistry namespaces() {
        return context.getNamespaceRegistry();
    }

    final org.modeshape.jcr.value.ValueFactory<String> stringFactory() {
        return context.getValueFactories().getStringFactory();
    }

    final NameFactory nameFactory() {
        return context.getValueFactories().getNameFactory();
    }

    final PathFactory pathFactory() {
        return context.getValueFactories().getPathFactory();
    }

    final PropertyFactory propertyFactory() {
        return context.getPropertyFactory();
    }

    final ReferenceFactory referenceFactory() {
        return context.getValueFactories().getReferenceFactory();
    }

    final UuidFactory uuidFactory() {
        return context.getValueFactories().getUuidFactory();
    }

    final DateTimeFactory dateFactory() {
        return context.getValueFactories().getDateFactory();
    }

    final ExecutionContext context() {
        return context;
    }

    final JcrValueFactory valueFactory() {
        if (valueFactory == null) {
            // Never gets unset, and this is idempotent so okay to create without a lock
            valueFactory = new JcrValueFactory(this.context);
        }
        return valueFactory;
    }

    final SessionCache cache() {
        return cache;
    }

    final SessionCache createSystemCache( boolean readOnly ) {
        return repository.createSystemSession(context, readOnly);
    }

    final JcrNodeTypeManager nodeTypeManager() {
        return this.workspace.nodeTypeManager();
    }

    final NodeTypes nodeTypes() {
        return this.repository().nodeTypeManager().getNodeTypes();
    }

    final JcrVersionManager versionManager() {
        return this.workspace.versionManager();
    }

    final JcrLockManager lockManager() {
        return workspace().lockManager();
    }
    
    final JcrObservationManager observationManager() {
        return workspace().observationManager();
    }

    final void signalNamespaceChanges( boolean global ) {
        nodeTypeManager().signalNamespaceChanges();
        if (global) repository.nodeTypeManager().signalNamespaceChanges();
    }

    final void setDesiredBaseVersionKey( NodeKey nodeKey,
                                         NodeKey baseVersionKey ) {
        baseVersionKeys.get().put(nodeKey, baseVersionKey);
    }

    final JcrSession spawnSession( boolean readOnly ) {
        return new JcrSession(this, readOnly);
    }

    final JcrSession spawnSession( String workspaceName,
                                   boolean readOnly ) {
        return new JcrSession(repository(), workspaceName, context(), sessionAttributes, readOnly);
    }

    final SessionCache spawnSessionCache( boolean readOnly ) {
        return repository().repositoryCache().createSession(context(), workspaceName(), readOnly);
    }

    final void addContextData( String key,
                               String value ) {
        this.context = context.with(key, value);
        this.cache.addContextData(key, value);
    }

    protected final String readableLocation( CachedNode node ) {
        try {
            return stringFactory().create(node.getPath(cache));
        } catch (Throwable t) {
            return node.getKey().toString();
        }
    }

    /**
     * Obtain the {@link Node JCR Node} object for the node with the supplied key.
     * 
     * @param nodeKey the node's key
     * @param expectedType the expected implementation type for the node, or null if it is not known
     * @return the JCR node; never null
     * @throws ItemNotFoundException if there is no node with the supplied key
     */
    AbstractJcrNode node( NodeKey nodeKey,
                          AbstractJcrNode.Type expectedType ) throws ItemNotFoundException {
        AbstractJcrNode node = jcrNodes.get(nodeKey);
        if (node != null) {
            // Make sure the node is still valid ...
            CachedNode cachedNode = cache.getNode(nodeKey);
            if (cachedNode == null) {
                // The node must have been deleted ...
                throw new ItemNotFoundException(nodeKey.toString());
            }
        } else {
            CachedNode cachedNode = cache.getNode(nodeKey);
            if (cachedNode != null) {
                node = node(cachedNode, expectedType);
            } else {
                // The node does not exist ...
                throw new ItemNotFoundException(nodeKey.toString());
            }
        }
        return node;
    }

    /**
     * Obtain the {@link Node JCR Node} object for the node with the supplied key.
     * 
     * @param cachedNode the cached node; may not be null
     * @param expectedType the expected implementation type for the node, or null if it is not known
     * @return the JCR node; never null
     */
    AbstractJcrNode node( CachedNode cachedNode,
                          AbstractJcrNode.Type expectedType ) {
        assert cachedNode != null;
        NodeKey nodeKey = cachedNode.getKey();
        AbstractJcrNode node = jcrNodes.get(nodeKey);
        if (node != null) return node;

        if (expectedType == null) {
            Name primaryType = cachedNode.getPrimaryType(cache);
            expectedType = Type.typeForPrimaryType(primaryType);
            if (expectedType == null) {
                // If this node from the system workspace, then the default is Type.SYSTEM rather than Type.NODE ...
                if (repository().systemWorkspaceKey().equals(nodeKey.getWorkspaceKey())) {
                    expectedType = Type.SYSTEM;
                } else {
                    expectedType = Type.NODE;
                }
                assert expectedType != null;
            }
        }
        switch (expectedType) {
            case NODE:
                node = new JcrNode(this, nodeKey);
                break;
            case VERSION:
                node = new JcrVersionNode(this, nodeKey);
                break;
            case VERSION_HISTORY:
                node = new JcrVersionHistoryNode(this, nodeKey);
                break;
            case SHARED:
                // TODO: Shared nodes
                throw new UnsupportedOperationException("Need to implement JcrSharedNode node");
            case SYSTEM:
                node = new JcrSystemNode(this, nodeKey);
                break;
            case ROOT:
                try {
                    return getRootNode();
                } catch (RepositoryException e) {
                    assert false : "Should never happen: " + e.getMessage();
                }
        }
        AbstractJcrNode newNode = jcrNodes.putIfAbsent(nodeKey, node);
        if (newNode != null) {
            // Another thread snuck in and created the node object ...
            node = newNode;
        }
        return node;
    }

    final CachedNode cachedNode( Path absolutePath ) throws PathNotFoundException, RepositoryException {
        return cachedNode(cache, getRootNode().node(), absolutePath, ModeShapePermissions.READ);
    }

    final CachedNode cachedNode( SessionCache cache,
                                 CachedNode node,
                                 Path path,
                                 String... actions ) throws PathNotFoundException, AccessDeniedException, RepositoryException {
        // We treat the path as a relative path, but the algorithm actually works for absolute, too. So don't enforce.
        for (Segment segment : path) {
            if (segment.isSelfReference()) continue;
            if (segment.isParentReference()) {
                node = cache.getNode(node.getParentKey(cache));
            } else {
                ChildReference ref = node.getChildReferences(cache).getChild(segment);
                if (ref == null) {
                    throw new PathNotFoundException(JcrI18n.nodeNotFound.text(stringFactory().create(path), workspaceName()));
                }
                CachedNode child = cache.getNode(ref);
                assert child != null : "Found a child reference in " + node.getPath(cache) + " to a non-existant child "
                                       + segment;
                node = child;
            }
        }
        checkPermission(path, actions);
        return node;
    }

    final MutableCachedNode mutableNode( SessionCache cache,
                                         CachedNode node,
                                         Path path,
                                         String... actions ) throws PathNotFoundException, RepositoryException {
        return cache.mutable(cachedNode(cache, node, path, actions).getKey());
    }

    final AbstractJcrNode node( CachedNode node,
                                Path path ) throws PathNotFoundException, AccessDeniedException, RepositoryException {
        CachedNode child = cachedNode(cache, node, path, "read");
        return node(child, (Type)null);
    }

    final AbstractJcrNode node( Path absolutePath ) throws PathNotFoundException, AccessDeniedException, RepositoryException {
        assert absolutePath.isAbsolute();
        if (absolutePath.isRoot()) return getRootNode();
        if (absolutePath.isIdentifier()) {
            // Look up the node by identifier ...
            String identifierString = stringFactory().create(absolutePath).replaceAll("\\[","").replaceAll("\\]","");
            return getNodeByIdentifier(identifierString);
        }
        CachedNode node = getRootNode().node();
        return node(node, absolutePath);
    }

    final AbstractJcrItem findItem( NodeKey nodeKey,
                                    Path relativePath ) throws RepositoryException {
        return findItem(node(nodeKey, null), relativePath);
    }

    final AbstractJcrItem findItem( AbstractJcrNode node,
                                    Path relativePath ) throws RepositoryException {
        assert !relativePath.isAbsolute();
        if (relativePath.size() == 1) {
            Segment last = relativePath.getLastSegment();
            if (last.isSelfReference()) return node;
            if (last.isParentReference()) return node.getParent();
        }
        // Find the path to the referenced node ...
        Path nodePath = node.path();
        Path absolutePath = nodePath.resolve(relativePath);
        if (absolutePath.isAtOrBelow(nodePath)) {
            // Find the item starting at 'node' ...
        }
        return getItem(absolutePath);
    }

    /**
     * Parse the supplied string into an absolute {@link Path} representation.
     * 
     * @param absPath the string containing an absolute path
     * @return the absolute path object; never null
     * @throws RepositoryException if the supplied string is not a valid absolute path
     */
    Path absolutePathFor( String absPath ) throws RepositoryException {
        Path path = null;
        try {
            path = pathFactory().create(absPath);
        } catch (org.modeshape.jcr.value.ValueFormatException e) {
            throw new RepositoryException(e.getMessage());
        }
        if (!path.isAbsolute()) {
            throw new RepositoryException(JcrI18n.invalidAbsolutePath.text(absPath));
        }
        return path;
    }

    @Override
    public JcrRepository getRepository() {
        return repository;
    }

    @Override
    public String getUserID() {
        return context.getSecurityContext().getUserName();
    }

    public boolean isAnonymous() {
        return context.getSecurityContext().isAnonymous();
    }

    @Override
    public String[] getAttributeNames() {
        Set<String> names = sessionAttributes.keySet();
        if (names.isEmpty()) return NO_ATTRIBUTES_NAMES;
        return names.toArray(new String[names.size()]);
    }

    @Override
    public Object getAttribute( String name ) {
        return this.sessionAttributes.get(name);
    }

    @Override
    public JcrWorkspace getWorkspace() {
        return workspace;
    }

    @Override
    public JcrRootNode getRootNode() throws RepositoryException {
        checkLive();
        return rootNode;
    }

    @Override
    public Session impersonate( Credentials credentials ) throws LoginException, RepositoryException {
        checkLive();
        return repository.login(credentials, workspaceName());
    }

    @Deprecated
    @Override
    public AbstractJcrNode getNodeByUUID( String uuid ) throws ItemNotFoundException, RepositoryException {
        return getNodeByIdentifier(uuid);
    }

    @Override
    public AbstractJcrNode getNodeByIdentifier( String id ) throws ItemNotFoundException, RepositoryException {
        checkLive();
        if (NodeKey.isValidFormat(id)) {
            // Try the identifier as a node key ...
            try {
                NodeKey key = new NodeKey(id);
                return node(key, null);
            } catch (ItemNotFoundException e) {
                // continue ...
            }
        }
        // Try as node key identifier ...
        NodeKey key = this.rootNode.key.withId(id);
        return node(key, null);
    }

    @Override
    public AbstractJcrNode getNode( String absPath ) throws PathNotFoundException, RepositoryException {
        checkLive();
        CheckArg.isNotEmpty(absPath, "absolutePath");
        Path path = absolutePathFor(absPath);

        // Return root node if path is "/" ...
        if (path.isRoot()) {
            return getRootNode();
        }

        return node(path);
    }

    @Override
    public AbstractJcrItem getItem( String absPath ) throws PathNotFoundException, RepositoryException {
        checkLive();
        CheckArg.isNotEmpty(absPath, "absPath");
        Path path = absolutePathFor(absPath);
        return getItem(path);
    }

    AbstractJcrItem getItem( Path path ) throws PathNotFoundException, RepositoryException {
        assert path.isAbsolute() : "Path supplied to Session.getItem(Path) must be absolute";
        // Return root node if path is "/" ...
        if (path.isRoot()) {
            return getRootNode();
        }
        // Since we don't know whether path refers to a node or a property, look to see if we can tell it's a node ...
        if (path.isIdentifier() || path.getLastSegment().hasIndex()) {
            return node(path);
        }
        // We can't tell from the name, so ask for an item.
        // JSR-170 doesn't allow children and proeprties to have the same name, but this is relaxed in JSR-283.
        // But JSR-283 Section 3.3.4 states "The method Session.getItem will return the item at the specified path
        // if there is only one such item, if there is both a node and a property at the specified path, getItem
        // will return the node." Therefore, look for a child first ...
        try {
            return node(path);
        } catch (PathNotFoundException e) {
            // Must not be any child by that name, so now look for a property on the parent node ...
            AbstractJcrNode parent = node(path.getParent());
            AbstractJcrProperty prop = parent.getProperty(path.getLastSegment().getName());
            if (prop != null) return prop;
            // Failed to find any item ...
            String pathStr = stringFactory().create(path);
            throw new PathNotFoundException(JcrI18n.itemNotFoundAtPath.text(pathStr, workspaceName()));
        }
    }

    @Override
    public Property getProperty( String absPath ) throws PathNotFoundException, RepositoryException {
        checkLive();
        CheckArg.isNotEmpty(absPath, "absPath");
        // Return root node if path is "/"
        Path path = absolutePathFor(absPath);
        if (path.isRoot()) {
            throw new PathNotFoundException(JcrI18n.rootNodeIsNotProperty.text());
        }
        if (path.isIdentifier()) {
            throw new PathNotFoundException(JcrI18n.identifierPathNeverReferencesProperty.text());
        }

        Segment lastSegment = path.getLastSegment();
        if (lastSegment.hasIndex()) {
            throw new RepositoryException(JcrI18n.pathCannotHaveSameNameSiblingIndex.text(absPath));
        }

        // This will throw a PNFE if the parent path does not exist
        AbstractJcrNode parentNode = node(path.getParent());
        AbstractJcrProperty property = parentNode.getProperty(lastSegment.getName());

        if (property == null) {
            throw new PathNotFoundException(GraphI18n.pathNotFoundExceptionLowestExistingLocationFound.text(absPath,
                                                                                                            parentNode.getPath()));
        }
        return property;
    }

    @Override
    public boolean itemExists( String absPath ) throws RepositoryException {
        try {
            return getItem(absPath) != null;
        } catch (PathNotFoundException error) {
            return false;
        }
    }

    @Override
    public void removeItem( String absPath )
        throws VersionException, LockException, ConstraintViolationException, AccessDeniedException, RepositoryException {
        getItem(absPath).remove();
    }

    @Override
    public boolean nodeExists( String absPath ) throws RepositoryException {
        // This is an optimized version of 'getNode(absPath)' ...
        checkLive();
        CheckArg.isNotEmpty(absPath, "absPath");
        Path absolutePath = absolutePathFor(absPath);
        return node(absolutePath) != null;
    }

    @Override
    public boolean propertyExists( String absPath ) throws RepositoryException {
        checkLive();
        CheckArg.isNotEmpty(absPath, "absPath");
        // Return root node if path is "/"
        Path path = absolutePathFor(absPath);
        if (path.isRoot()) {
            throw new PathNotFoundException(JcrI18n.rootNodeIsNotProperty.text());
        }
        if (path.isIdentifier()) {
            throw new PathNotFoundException(JcrI18n.identifierPathNeverReferencesProperty.text());
        }

        Segment lastSegment = path.getLastSegment();
        if (lastSegment.hasIndex()) {
            throw new RepositoryException(JcrI18n.pathCannotHaveSameNameSiblingIndex.text(absPath));
        }

        // This will throw a PNFE if the parent path does not exist
        CachedNode parentNode = cachedNode(path.getParent());
        return parentNode != null && parentNode.hasProperty(lastSegment.getName(), cache());
    }

    @Override
    public void move( String srcAbsPath,
                      String destAbsPath )
        throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException,
        RepositoryException {
        checkLive();

        // Find the source path and destination path and check for permissions
        Path srcPath = absolutePathFor(srcAbsPath);
        checkPermission(srcPath, ModeShapePermissions.REMOVE);

        Path destPath = absolutePathFor(destAbsPath);
        checkPermission(destPath.getParent(), ModeShapePermissions.ADD_NODE);

        if (srcPath.isRoot()) {
            throw new RepositoryException(JcrI18n.unableToMoveRootNode.text(workspaceName()));
        }
        if (destPath.isRoot()) {
            throw new RepositoryException(JcrI18n.rootNodeCannotBeDestinationOfMovedNode.text(workspaceName()));
        }
        if (!destPath.isIdentifier() && destAbsPath.endsWith("]")) {
            // Doing a literal test here because the path factory will canonicalize "/node[1]" to "/node"
            throw new RepositoryException(JcrI18n.pathCannotHaveSameNameSiblingIndex.text(destAbsPath));
        }
        if (srcPath.isAncestorOf(destPath)) {
            String msg = JcrI18n.unableToMoveNodeToBeChildOfDecendent.text(srcAbsPath, destAbsPath, workspaceName());
            throw new RepositoryException(msg);
        }
        if (srcPath.equals(destPath)) {
            // Nothing to do ...
            return;
        }

        // Get the node at the source path and the parent node of the destination path ...
        AbstractJcrNode srcNode = node(srcPath);
        AbstractJcrNode destParentNode = node(destPath.getParent());

        SessionCache sessionCache = cache();

        // Check whether these nodes are locked ...
        if (srcNode.isLocked() && !srcNode.getLock().isLockOwningSession()) {
            javax.jcr.lock.Lock sourceLock = srcNode.getLock();
            if (sourceLock != null && sourceLock.getLockToken() == null) {
                throw new LockException(JcrI18n.lockTokenNotHeld.text(srcAbsPath));
            }
        }
        if (destParentNode.isLocked() && !destParentNode.getLock().isLockOwningSession()) {
            javax.jcr.lock.Lock newParentLock = destParentNode.getLock();
            if (newParentLock != null && newParentLock.getLockToken() == null) {
                throw new LockException(JcrI18n.lockTokenNotHeld.text(destAbsPath));
            }
        }

        // Check whether the nodes that will be modified are checked out ...
        AbstractJcrNode srcParent = srcNode.getParent();
        if (!srcParent.isCheckedOut()) {
            throw new VersionException(JcrI18n.nodeIsCheckedIn.text(srcNode.getPath()));
        }
        if (!destParentNode.isCheckedOut()) {
            throw new VersionException(JcrI18n.nodeIsCheckedIn.text(destParentNode.getPath()));
        }

        //check whether the parent definition allows children which match the source
        destParentNode.validateChildNodeDefinition(srcNode.name(),
                                                   srcNode.getPrimaryTypeName(), true);

        try {
            MutableCachedNode mutableSrcParent = srcParent.mutable();
            MutableCachedNode mutableDestParent = destParentNode.mutable();
            if (mutableSrcParent.equals(mutableDestParent)) {
                // It's just a rename ...
                mutableSrcParent.renameChild(sessionCache, srcNode.key(), destPath.getLastSegment().getName());
            } else {
                // It is a move from one parent to another ...
                mutableSrcParent.moveChild(sessionCache, srcNode.key(), mutableDestParent, destPath.getLastSegment().getName());
            }
        } catch (NodeNotFoundException e) {
            // Not expected ...
            String msg = JcrI18n.nodeNotFound.text(stringFactory().create(srcPath.getParent()), workspaceName());
            throw new PathNotFoundException(msg);
        }
    }

    @Override
    public void save()
        throws AccessDeniedException, ItemExistsException, ReferentialIntegrityException, ConstraintViolationException,
        InvalidItemStateException, VersionException, LockException, NoSuchNodeTypeException, RepositoryException {
        checkLive();

        // Perform the save, using 'JcrPreSave' operations ...
        SessionCache systemCache = createSystemCache(false);
        SystemContent systemContent = new SystemContent(systemCache);
        Map<NodeKey, NodeKey> baseVersionKeys = this.baseVersionKeys.get();
        try {
            cache().save(systemContent.cache(), new JcrPreSave(systemContent, baseVersionKeys));
            this.baseVersionKeys.set(null);
        } catch (WrappedException e) {
            Throwable cause = e.getCause();
            throw (cause instanceof RepositoryException) ? (RepositoryException)cause : new RepositoryException(e.getCause());
        } catch (DocumentNotFoundException e) {
            // Try to figure out which node in this transient state was the problem ...
            NodeKey key = new NodeKey(e.getKey());
            AbstractJcrNode problemNode = node(key, null);
            String path = problemNode.getPath();
            throw new InvalidItemStateException(JcrI18n.nodeModifiedBySessionWasRemovedByAnotherSession.text(path, key), e);
        } catch (DocumentAlreadyExistsException e) {
            // Try to figure out which node in this transient state was the problem ...
            NodeKey key = new NodeKey(e.getKey());
            AbstractJcrNode problemNode = node(key, null);
            String path = problemNode.getPath();
            throw new InvalidItemStateException(JcrI18n.nodeCreatedBySessionUsedExistingKey.text(path, key), e);
        } catch (Throwable t) {
            throw new RepositoryException(t);
        }

        try {
            // Record the save operation ...
            repository().statistics().increment(ValueMetric.SESSION_SAVES);
        } catch (IllegalStateException e) {
            // The repository has been shutdown ...
        }
    }

    /**
     * Save a subset of the changes made within this session.
     * 
     * @param node the node at or below which the changes are to be saved; may not be null
     * @throws RepositoryException if there is a problem saving the changes
     * @see AbstractJcrNode#save()
     */
    void save( AbstractJcrNode node ) throws RepositoryException {
        if (node.isNew()) {
            //expected by TCK
            throw new RepositoryException(JcrI18n.unableToSaveNodeThatWasCreatedSincePreviousSave.text(node.getPath(), workspaceName()));
        }

        if (node.containsChangesWithExternalDependencies()) {
            //expected by TCK
            I18n msg = JcrI18n.unableToSaveBranchBecauseChangesDependOnChangesToNodesOutsideOfBranch;
            throw new ConstraintViolationException(msg.text(node.path(), workspaceName()));
        }

        SessionCache sessionCache = cache();

        // Perform the save, using 'JcrPreSave' operations ...
        SessionCache systemCache = createSystemCache(false);
        SystemContent systemContent = new SystemContent(systemCache);
        Map<NodeKey, NodeKey> baseVersionKeys = this.baseVersionKeys.get();
        try {
            sessionCache.save(node.node(), systemContent.cache(), new JcrPreSave(systemContent, baseVersionKeys));
        } catch (WrappedException e) {
            Throwable cause = e.getCause();
            throw (cause instanceof RepositoryException) ? (RepositoryException)cause : new RepositoryException(e.getCause());
        } catch (DocumentNotFoundException e) {
            // Try to figure out which node in this transient state was the problem ...
            NodeKey key = new NodeKey(e.getKey());
            AbstractJcrNode problemNode = node(key, null);
            String path = problemNode.getPath();
            throw new InvalidItemStateException(JcrI18n.nodeModifiedBySessionWasRemovedByAnotherSession.text(path, key), e);
        } catch (DocumentAlreadyExistsException e) {
            // Try to figure out which node in this transient state was the problem ...
            NodeKey key = new NodeKey(e.getKey());
            AbstractJcrNode problemNode = node(key, null);
            String path = problemNode.getPath();
            throw new InvalidItemStateException(JcrI18n.nodeCreatedBySessionUsedExistingKey.text(path, key), e);
        } catch (Throwable t) {
            throw new RepositoryException(t);
        }

        try {
            // Record the save operation ...
            repository().statistics().increment(ValueMetric.SESSION_SAVES);
        } catch (IllegalStateException e) {
            // The repository has been shutdown ...
        }
    }

    @Override
    public void refresh( boolean keepChanges ) throws RepositoryException {
        checkLive();
        if (!keepChanges) {
            cache.clear();
        }
        // Otherwise there is nothing to do, as all persistent changes are always immediately vislble to all sessions
        // using that same workspace
    }

    @Override
    public boolean hasPendingChanges() throws RepositoryException {
        checkLive();
        return cache().hasChanges();
    }

    @Override
    public JcrValueFactory getValueFactory() throws UnsupportedRepositoryOperationException, RepositoryException {
        checkLive();
        return valueFactory();
    }

    /**
     * Determine if the current user does not have permission for all of the named actions in the named workspace, otherwise
     * returns silently.
     * 
     * @param workspaceName the name of the workspace in which the path exists
     * @param path the path on which the actions are occurring
     * @param actions the list of {@link ModeShapePermissions actions} to check
     * @return true if the subject has privilege to perform all of the named actions on the content at the supplied path in the
     *         given workspace within the repository, or false otherwise
     */
    private final boolean hasPermission( String workspaceName,
                                         Path path,
                                         String... actions ) {
        final String repositoryName = this.repository.repositoryName();
        SecurityContext sec = context.getSecurityContext();
        if (sec instanceof AuthorizationProvider) {
            // Delegate to the security context ...
            AuthorizationProvider authorizer = (AuthorizationProvider)sec;
            return authorizer.hasPermission(context, repositoryName, repositoryName, workspaceName, path, actions);
        }
        // It is a role-based security context, so apply role-based authorization ...
        boolean hasPermission = true;
        for (String action : actions) {
            if (ModeShapePermissions.READ.equals(action)) {
                hasPermission &= hasRole(sec, ModeShapeRoles.READONLY, repositoryName, workspaceName)
                                 || hasRole(sec, ModeShapeRoles.READWRITE, repositoryName, workspaceName)
                                 || hasRole(sec, ModeShapeRoles.ADMIN, repositoryName, workspaceName);
            } else if (ModeShapePermissions.REGISTER_NAMESPACE.equals(action)
                       || ModeShapePermissions.REGISTER_TYPE.equals(action) || ModeShapePermissions.UNLOCK_ANY.equals(action)
                       || ModeShapePermissions.CREATE_WORKSPACE.equals(action)
                       || ModeShapePermissions.DELETE_WORKSPACE.equals(action) || ModeShapePermissions.MONITOR.equals(action)
                       || ModeShapePermissions.DELETE_WORKSPACE.equals(action)
                       || ModeShapePermissions.INDEX_WORKSPACE.equals(action)) {
                hasPermission &= hasRole(sec, ModeShapeRoles.ADMIN, repositoryName, workspaceName);
            } else {
                hasPermission &= hasRole(sec, ModeShapeRoles.ADMIN, repositoryName, workspaceName)
                                 || hasRole(sec, ModeShapeRoles.READWRITE, repositoryName, workspaceName);
            }
        }
        return hasPermission;
    }

    /**
     * Returns whether the authenticated user has the given role.
     * 
     * @param context the security context
     * @param roleName the name of the role to check
     * @param repositoryName the name of the repository
     * @param workspaceName the workspace under which the user must have the role. This may be different from the current
     *        workspace.
     * @return true if the user has the role and is logged in; false otherwise
     */
    final static boolean hasRole( SecurityContext context,
                                  String roleName,
                                  String repositoryName,
                                  String workspaceName ) {
        if (context.hasRole(roleName)) return true;
        roleName = roleName + "." + repositoryName;
        if (context.hasRole(roleName)) return true;
        roleName = roleName + "." + workspaceName;
        return context.hasRole(roleName);
    }

    @Override
    public void checkPermission( String path,
                                 String actions ) {
        CheckArg.isNotEmpty(path, "path");
        CheckArg.isNotEmpty(actions, "actions");
        try {
            this.checkPermission(absolutePathFor(path), actions.split(","));
        } catch (RepositoryException e) {
            throw new AccessControlException(JcrI18n.permissionDenied.text(path, actions));
        }
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
     * @throws AccessDeniedException if the actions cannot be performed on the node at the specified path
     */
    void checkPermission( Path path,
                          String... actions ) throws AccessDeniedException {
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
     * @throws AccessDeniedException if the actions cannot be performed on the node at the specified path
     */
    void checkPermission( String workspaceName,
                          Path path,
                          String... actions ) throws AccessDeniedException {
        CheckArg.isNotEmpty(actions, "actions");
        if (hasPermission(workspaceName, path, actions)) return;

        String pathAsString = path != null ? path.getString(this.namespaces()) : "<unknown>";
        throw new AccessDeniedException(JcrI18n.permissionDenied.text(pathAsString, actions));
    }

    @Override
    public boolean hasPermission( String absPath,
                                  String actions ) throws RepositoryException {
        checkLive();
        CheckArg.isNotEmpty(absPath, "absPath");
        Path p = absolutePathFor(absPath);
        return hasPermission(this.workspace().getName(), p, actions.split(","));
    }

    /**
     * Makes a "best effort" determination of whether the given method can be successfully called on the given target with the
     * given arguments. A return value of {@code false} indicates that the method would not succeed. A return value of
     * {@code true} indicates that the method <i>might</i> succeed.
     * 
     * @param methodName the method to invoke; may not be null
     * @param target the object on which to invoke it; may not be null
     * @param arguments the arguments to pass to the method; varies depending on the method
     * @return true if the given method can be determined to be supported, or false otherwise
     * @throws IllegalArgumentException
     * @throws RepositoryException
     */
    @Override
    public boolean hasCapability( String methodName,
                                  Object target,
                                  Object[] arguments ) throws RepositoryException {
        CheckArg.isNotEmpty(methodName, "methodName");
        CheckArg.isNotNull(target, "target");
        checkLive();

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
            // TODO: Should 'hasCapability' support methods other than 'addNode'?
        }
        return true;
    }

    @Override
    public ContentHandler getImportContentHandler( String parentAbsPath,
                                                   int uuidBehavior )
        throws PathNotFoundException, ConstraintViolationException, VersionException, LockException, RepositoryException {
        checkLive();

        // Find the parent path ...
        AbstractJcrNode parent = getNode(parentAbsPath);
        if (!parent.isCheckedOut()) {
            throw new VersionException(JcrI18n.nodeIsCheckedIn.text(parent.getPath()));
        }

        boolean retainLifecycleInfo = getRepository().getDescriptorValue(Repository.OPTION_LIFECYCLE_SUPPORTED).getBoolean();
        boolean retainRetentionInfo = getRepository().getDescriptorValue(Repository.OPTION_RETENTION_SUPPORTED).getBoolean();

        return new JcrContentHandler(this, parent, uuidBehavior, false, retainRetentionInfo, retainLifecycleInfo);
    }

    protected void initBaseVersionKeys() {
        // Since we're importing into this session, we need to capture any base version information in the imported file ...
        baseVersionKeys.compareAndSet(null, new ConcurrentHashMap<NodeKey, NodeKey>());
    }

    @Override
    public void importXML( String parentAbsPath,
                           InputStream in,
                           int uuidBehavior )
        throws IOException, PathNotFoundException, ItemExistsException, ConstraintViolationException, VersionException,
        InvalidSerializedDataException, LockException, RepositoryException {
        CheckArg.isNotNull(parentAbsPath, "parentAbsPath");
        CheckArg.isNotNull(in, "in");
        checkLive();

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
    public void exportSystemView( String absPath,
                                  ContentHandler contentHandler,
                                  boolean skipBinary,
                                  boolean noRecurse ) throws PathNotFoundException, SAXException, RepositoryException {
        CheckArg.isNotNull(absPath, "absPath");
        CheckArg.isNotNull(contentHandler, "contentHandler");
        Node exportRootNode = getNode(absPath);
        AbstractJcrExporter exporter = new JcrSystemViewExporter(this);
        exporter.exportView(exportRootNode, contentHandler, skipBinary, noRecurse);
    }

    @Override
    public void exportSystemView( String absPath,
                                  OutputStream out,
                                  boolean skipBinary,
                                  boolean noRecurse ) throws IOException, PathNotFoundException, RepositoryException {
        CheckArg.isNotNull(absPath, "absPath");
        CheckArg.isNotNull(out, "out");
        Node exportRootNode = getNode(absPath);
        AbstractJcrExporter exporter = new JcrSystemViewExporter(this);
        exporter.exportView(exportRootNode, out, skipBinary, noRecurse);
    }

    @Override
    public void exportDocumentView( String absPath,
                                    ContentHandler contentHandler,
                                    boolean skipBinary,
                                    boolean noRecurse ) throws PathNotFoundException, SAXException, RepositoryException {
        CheckArg.isNotNull(absPath, "absPath");
        CheckArg.isNotNull(contentHandler, "contentHandler");
        checkLive();
        Node exportRootNode = getNode(absPath);
        AbstractJcrExporter exporter = new JcrDocumentViewExporter(this);
        exporter.exportView(exportRootNode, contentHandler, skipBinary, noRecurse);
    }

    @Override
    public void exportDocumentView( String absPath,
                                    OutputStream out,
                                    boolean skipBinary,
                                    boolean noRecurse ) throws IOException, PathNotFoundException, RepositoryException {
        CheckArg.isNotNull(absPath, "absPath");
        CheckArg.isNotNull(out, "out");
        Node exportRootNode = getNode(absPath);
        AbstractJcrExporter exporter = new JcrDocumentViewExporter(this);
        exporter.exportView(exportRootNode, out, skipBinary, noRecurse);
    }

    @Override
    public String getNamespacePrefix( String uri ) throws RepositoryException {
        checkLive();
        return sessionRegistry.getPrefix(uri);
    }

    @Override
    public String[] getNamespacePrefixes() throws RepositoryException {
        checkLive();
        return sessionRegistry.getPrefixes();
    }

    @Override
    public String getNamespaceURI( String prefix ) throws RepositoryException {
        checkLive();
        return sessionRegistry.getURI(prefix);
    }

    @Override
    public void setNamespacePrefix( String newPrefix,
                                    String existingUri ) throws NamespaceException, RepositoryException {
        checkLive();
        sessionRegistry.registerNamespace(newPrefix, existingUri);
    }

    @Override
    public void logout() {
        this.isLive = false;
        try {
            RunningState running = repository.runningState();
            long lifetime = System.nanoTime() - this.nanosCreated;
            Map<String, String> payload = Collections.singletonMap("userId", getUserID());
            running.statistics().recordDuration(DurationMetric.SESSION_LIFETIME, lifetime, TimeUnit.NANOSECONDS, payload);
            running.statistics().decrement(ValueMetric.SESSION_COUNT);
            running.removeSession(this);
        } catch (IllegalStateException e) {
            // The repository has been shutdown
        }
    }

    @Override
    public boolean isLive() {
        return isLive;
    }

    @Override
    public void addLockToken( String lockToken ) {
        CheckArg.isNotNull(lockToken, "lockToken");
        try {
            lockManager().addLockToken(lockToken);
        } catch (LockException le) {
            // For backwards compatibility (and API compatibility), the LockExceptions from the LockManager need to get swallowed
        }
    }

    @Override
    public String[] getLockTokens() {
        if (!isLive()) return new String[] {};
        return lockManager().getLockTokens();
    }

    @Override
    public void removeLockToken( String lockToken ) {
        CheckArg.isNotNull(lockToken, "lockToken");
        // A LockException is thrown if the lock associated with the specified lock token is session-scoped.
        try {
            lockManager().removeLockToken(lockToken);
        } catch (LockException le) {
            // For backwards compatibility (and API compatibility), the LockExceptions from the LockManager need to get swallowed
        }
    }

    @Override
    public AccessControlManager getAccessControlManager() throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    @Override
    public RetentionManager getRetentionManager() throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * Returns the absolute path of the node in the specified workspace that corresponds to this node.
     * <p>
     * The corresponding node is defined as the node in srcWorkspace with the same UUID as this node or, if this node has no UUID,
     * the same path relative to the nearest ancestor that does have a UUID, or the root node, whichever comes first. This is
     * qualified by the requirement that referencable nodes only correspond with other referencables and non-referenceables with
     * other non-referenceables.
     * </p>
     * 
     * @param workspaceName the name of the workspace; may not be null
     * @param key the key for the node; may not be null
     * @param relativePath the relative path from the referenceable node, or null if the supplied UUID identifies the
     *        corresponding node
     * @return the absolute path to the corresponding node in the workspace; never null
     * @throws NoSuchWorkspaceException if the specified workspace does not exist
     * @throws ItemNotFoundException if no corresponding node exists
     * @throws AccessDeniedException if the current session does not have sufficient rights to perform this operation
     * @throws RepositoryException if another exception occurs
     */
    Path getPathForCorrespondingNode( String workspaceName,
                                      NodeKey key,
                                      Path relativePath )
        throws NoSuchWorkspaceException, AccessDeniedException, ItemNotFoundException, RepositoryException {
        assert key != null;

        try {
            NodeCache cache = repository.repositoryCache().getWorkspaceCache(workspaceName);
            CachedNode node = cache.getNode(key);
            if (node == null) {
                throw new ItemNotFoundException(JcrI18n.itemNotFoundWithUuid.text(key.toString(), workspaceName));
            }
            if (relativePath != null) {
                for (Segment segment : relativePath) {
                    ChildReference child = node.getChildReferences(cache).getChild(segment);
                    if (child == null) {
                        Path path = pathFactory().create(node.getPath(cache), segment);
                        throw new ItemNotFoundException(JcrI18n.itemNotFoundAtPath.text(path.getString(namespaces()), workspaceName()));
                    }
                    CachedNode childNode = cache.getNode(child);
                    if (childNode == null) {
                        Path path = pathFactory().create(node.getPath(cache), segment);
                        throw new ItemNotFoundException(JcrI18n.itemNotFoundAtPath.text(path.getString(namespaces()), workspaceName()));
                    }
                    node = childNode;
                }
            }
            return node.getPath(cache);
        } catch (AccessControlException ace) {
            throw new AccessDeniedException(ace);
        } catch (WorkspaceNotFoundException e) {
            throw new NoSuchWorkspaceException(JcrI18n.workspaceNameIsInvalid.text(repository().repositoryName(), workspaceName));
        }
    }

    @Override
    public String toString() {
        return cache.toString();
    }

    /**
     * Define the operations that are to be performed on all the nodes that were created or modified within this session. This
     * class was designed to be as efficient as possible for most nodes, since most nodes do not need any additional processing.
     */
    protected final class JcrPreSave implements SessionCache.PreSave {
        private final SessionCache cache;
        private final RepositoryNodeTypeManager nodeTypeMgr;
        private final NodeTypes nodeTypeCapabilities;
        private final SystemContent systemContent;
        private final Map<NodeKey, NodeKey> baseVersionKeys;
        private boolean initialized = false;
        private PropertyFactory propertyFactory;
        private ReferenceFactory referenceFactory;
        private JcrVersionManager versionManager;

        protected JcrPreSave( SystemContent content,
                              Map<NodeKey, NodeKey> baseVersionKeys ) {
            assert content != null;
            this.cache = cache();
            this.systemContent = content;
            this.baseVersionKeys = baseVersionKeys;
            // Get the capabilities cache. This is immutable, so we'll use it for the entire pre-save operation ...
            this.nodeTypeMgr = repository().nodeTypeManager();
            this.nodeTypeCapabilities = nodeTypeMgr.getNodeTypes();
        }

        @Override
        public void process( MutableCachedNode node,
                             SaveContext context ) throws Exception {
            // Most nodes do not need any extra processing, so the first thing to do is figure out whether this
            // node has a primary type or mixin types that need extra processing. Unfortunately, this means we always have
            // to get the primary type and mixin types.
            final Name primaryType = node.getPrimaryType(cache);
            final Set<Name> mixinTypes = node.getMixinTypes(cache);

            if (nodeTypeCapabilities.isFullyDefinedType(primaryType, mixinTypes)) {
                // There is nothing to do for this node ...
                return;
            }

            if (!initialized) {
                // We're gonna need a few more objects, so create them now ...
                initialized = true;
                versionManager = versionManager();
                propertyFactory = propertyFactory();
                referenceFactory = referenceFactory();
            }

            AbstractJcrNode jcrNode = null;

            // -----------
            // mix:created
            // -----------
            boolean initializeVersionHistory = false;
            if (node.isNew()) {
                if (nodeTypeCapabilities.isCreated(primaryType, mixinTypes)) {
                    // Set the created by and time information if not changed explicitly
                    node.setPropertyIfUnchanged(cache, propertyFactory.create(JcrLexicon.CREATED, context.getTime()));
                    node.setPropertyIfUnchanged(cache, propertyFactory.create(JcrLexicon.CREATED_BY, context.getUserId()));
                }
                initializeVersionHistory = nodeTypeCapabilities.isVersionable(primaryType, mixinTypes);
            } else {
                // Changed nodes can only be made versionable if the primary type or mixins changed ...
                if (node.hasChangedPrimaryType() || !node.getAddedMixins(cache).isEmpty()) {
                    initializeVersionHistory = nodeTypeCapabilities.isVersionable(primaryType, mixinTypes);
                }
            }

            // ----------------
            // mix:lastModified
            // ----------------
            if (nodeTypeCapabilities.isLastModified(primaryType, mixinTypes)) {
                // Set the last modified by and time information if it has not been changed explicitly
                node.setPropertyIfUnchanged(cache, propertyFactory.create(JcrLexicon.LAST_MODIFIED, context.getTime()));
                node.setPropertyIfUnchanged(cache, propertyFactory.create(JcrLexicon.LAST_MODIFIED_BY, context.getUserId()));
            }

            // ---------------
            // mix:versionable
            // ---------------
            if (initializeVersionHistory) {
                // See if there is a version history for the node ...
                NodeKey versionableKey = node.getKey();
                if (!systemContent.hasVersionHistory(versionableKey)) {
                    // Initialize the version history ...
                    NodeKey historyKey = systemContent.versionHistoryNodeKeyFor(versionableKey);
                    NodeKey baseVersionKey = baseVersionKeys == null ? null : baseVersionKeys.get(versionableKey);
                    //it may happen during an import, that a node with version history & base version is assigned a new key and therefore
                    //the base version points to an existing version while no version history is found initially
                    boolean shouldCreateNewVersionHistory = true;
                    if (baseVersionKey != null){
                        SessionCache systemCache = systemContent.cache();
                        CachedNode baseVersionNode = systemCache.getNode(baseVersionKey);
                        if (baseVersionNode != null) {
                            historyKey = baseVersionNode.getParentKey(systemCache);
                            shouldCreateNewVersionHistory = (historyKey == null);
                        }
                    }
                    if (shouldCreateNewVersionHistory) {
                        //a new version history should be initialized
                        if (baseVersionKey == null) baseVersionKey = historyKey.withRandomId();
                        Path versionHistoryPath = versionManager.versionHistoryPathFor(versionableKey);
                        systemContent.initializeVersionStorage(versionableKey,
                                                               historyKey,
                                                               baseVersionKey,
                                                               primaryType,
                                                               mixinTypes,
                                                               versionHistoryPath,
                                                               null,
                                                               context.getTime());
                    }

                    // Now update the node as if it's checked in ...
                    Reference historyRef = referenceFactory.create(historyKey);
                    Reference baseVersionRef = referenceFactory.create(baseVersionKey);
                    node.setProperty(cache, propertyFactory.create(JcrLexicon.IS_CHECKED_OUT, Boolean.TRUE));
                    node.setProperty(cache, propertyFactory.create(JcrLexicon.VERSION_HISTORY, historyRef));
                    node.setProperty(cache, propertyFactory.create(JcrLexicon.BASE_VERSION, baseVersionRef));
                    node.setProperty(cache, propertyFactory.create(JcrLexicon.PREDECESSORS, new Object[] {}));
                }
            }

            // --------------------
            // Mandatory properties
            // --------------------
            // Some of the version history properties are mandatory, so we need to initialize the version history first ...
            Collection<JcrPropertyDefinition> mandatoryPropDefns = null;
            mandatoryPropDefns = nodeTypeCapabilities.getMandatoryPropertyDefinitions(primaryType, mixinTypes);
            if (!mandatoryPropDefns.isEmpty()) {
                // There is at least one mandatory property on this node, so go through all of the mandatory property
                // definitions and see if any do not correspond to existing properties ...
                for (JcrPropertyDefinition defn : mandatoryPropDefns) {
                    Name propName = defn.getInternalName();
                    if (!node.hasProperty(propName, cache)) {
                        // There is no mandatory property ...
                        if (defn.hasDefaultValues()) {
                            // This may or may not be auto-created; we don't care ...
                            if (jcrNode == null) jcrNode = node(node, (Type)null);
                            JcrValue[] defaultValues = defn.getDefaultValues();
                            if (defn.isMultiple()) {
                                jcrNode.setProperty(propName, defaultValues, defn.getRequiredType(), false);
                            } else {
                                // don't skip constraint checks or protected checks
                                jcrNode.setProperty(propName, defaultValues[0], false,false);
                            }
                        } else {
                            // There is no default for this mandatory property, so this is a constraint violation ...
                            String pName = defn.getName();
                            String typeName = defn.getDeclaringNodeType().getName();
                            String loc = readableLocation(node);
                            throw new ConstraintViolationException(JcrI18n.missingMandatoryProperty.text(pName, typeName, loc));
                        }
                    } else {
                        // There is a property with the same name as the mandatory property, so verify that the
                        // existing property does indeed use this property definition. Use the JCR property
                        // since it may already cache the property definition ID or will know how to find it ...
                        if (jcrNode == null) jcrNode = node(node, (Type)null);
                        AbstractJcrProperty jcrProperty = jcrNode.getProperty(propName);
                        PropertyDefinitionId defnId = jcrProperty.propertyDefinitionId();
                        if (defn.getId().equals(defnId)) {
                            // This existing property does use the auto-created definition ...
                            continue;
                        }
                        // The existing property does not use the property definition, but we can't auto-create the property
                        // because there is already an existing one with the same name. First see if we can forcibly
                        // recompute the property definition ...
                        jcrProperty.releasePropertyDefinitionId();
                        defnId = jcrProperty.propertyDefinitionId();
                        if (defn.getId().equals(defnId)) {
                            // This existing property does use the auto-created definition ...
                            continue;
                        }

                        // Still didn't match, so this is a constraint violation of the existing property ...
                        String pName = defn.getName();
                        String typeName = defn.getDeclaringNodeType().getName();
                        String loc = readableLocation(node);
                        I18n msg = JcrI18n.propertyNoLongerSatisfiesConstraints;
                        throw new ConstraintViolationException(msg.text(pName, loc, defn.getName(), typeName));
                    }
                }
            }

            // ---------------------
            // Mandatory child nodes
            // ---------------------
            Collection<JcrNodeDefinition> mandatoryChildDefns = null;
            mandatoryChildDefns = nodeTypeCapabilities.getMandatoryChildNodeDefinitions(primaryType, mixinTypes);
            if (!mandatoryChildDefns.isEmpty()) {
                // There is at least one auto-created child node definition on this node, so figure out if they are all
                // covered by existing children ...
                for (JcrNodeDefinition defn : mandatoryChildDefns) {
                    Name propName = defn.getInternalName();
                    // TODO: Validation
                }
            }

            // --------
            // mix:etag
            // --------
            // The 'jcr:etag' property may depend on auto-created properties, so do this last ...
            if (nodeTypeCapabilities.isETag(primaryType, mixinTypes)) {
                // Per section 3.7.12 of JCR 2, the 'jcr:etag' property should be changed whenever BINARY properties
                // are added, removed, or changed. So, go through the properties (in sorted-name order so it is repeatable)
                // and create this value by simply concatenating the SHA-1 hash of each BINARY value ...
                String etagValue = node.getEtag(cache);
                node.setProperty(cache, propertyFactory.create(JcrLexicon.ETAG, etagValue));
            }

        }
    }
}
