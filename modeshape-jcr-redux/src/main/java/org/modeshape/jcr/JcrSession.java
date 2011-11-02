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
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
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
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.retention.RetentionManager;
import javax.jcr.security.AccessControlManager;
import javax.jcr.version.VersionException;
import org.infinispan.schematic.SchematicEntry;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.AbstractJcrNode.Type;
import org.modeshape.jcr.JcrNamespaceRegistry.Behavior;
import org.modeshape.jcr.JcrRepository.RunningState;
import org.modeshape.jcr.RepositoryStatistics.DurationMetric;
import org.modeshape.jcr.RepositoryStatistics.ValueMetric;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.ChildReference;
import org.modeshape.jcr.cache.MutableCachedNode;
import org.modeshape.jcr.cache.NodeCache;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.NodeNotFoundException;
import org.modeshape.jcr.cache.SessionCache;
import org.modeshape.jcr.cache.WorkspaceNotFoundException;
import org.modeshape.jcr.core.ExecutionContext;
import org.modeshape.jcr.core.SecurityContext;
import org.modeshape.jcr.security.AuthorizationProvider;
import org.modeshape.jcr.value.DateTimeFactory;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NameFactory;
import org.modeshape.jcr.value.NamespaceRegistry;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Path.Segment;
import org.modeshape.jcr.value.PathFactory;
import org.modeshape.jcr.value.PropertyFactory;
import org.modeshape.jcr.value.UuidFactory;
import org.modeshape.jcr.value.basic.LocalNamespaceRegistry;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * 
 */
public class JcrSession implements Session {

    private static final String[] NO_ATTRIBUTES_NAMES = new String[] {};

    private final ExecutionContext context;
    private final JcrRepository repository;
    private final SessionCache cache;
    private final JcrValueFactory valueFactory;
    private final JcrRootNode rootNode;
    private final String workspaceName;
    private final ConcurrentMap<NodeKey, AbstractJcrNode> jcrNodes = new ConcurrentHashMap<NodeKey, AbstractJcrNode>();
    private final Map<String, Object> sessionAttributes;
    private final JcrWorkspace workspace;
    private final String systemWorkspaceKey;
    private final NamespaceRegistry globalNamespaceRegistry;
    private final JcrNamespaceRegistry sessionRegistry;
    private volatile boolean isLive = true;
    private final long nanosCreated;

    protected JcrSession( JcrRepository repository,
                          String workspaceName,
                          ExecutionContext context,
                          Map<String, Object> sessionAttributes,
                          boolean readOnly ) {
        this.repository = repository;
        this.workspaceName = workspaceName;

        // Create an execution context for this session that uses a local namespace registry ...
        this.globalNamespaceRegistry = context.getNamespaceRegistry(); // thread-safe!
        LocalNamespaceRegistry localRegistry = new LocalNamespaceRegistry(this.globalNamespaceRegistry); // not-thread-safe!
        this.context = context.with(localRegistry);
        this.sessionRegistry = new JcrNamespaceRegistry(Behavior.SESSION, localRegistry, this.globalNamespaceRegistry, this);

        // Create the session cache ...
        this.cache = repository.repositoryCache().createSession(context, workspaceName, readOnly);
        this.valueFactory = new JcrValueFactory(this.context);
        this.rootNode = new JcrRootNode(this, this.cache.getRootKey());
        this.jcrNodes.put(this.rootNode.key(), this.rootNode);
        if (sessionAttributes == null) {
            this.sessionAttributes = Collections.emptyMap();
        } else {
            this.sessionAttributes = Collections.unmodifiableMap(sessionAttributes);
        }
        this.workspace = new JcrWorkspace(this);
        this.systemWorkspaceKey = repository.systemWorkspaceKey();

        // Pre-cache all of the namespaces to be a snapshot of what's in the global registry at this time.
        // This behavior is specified in Section 3.5.2 of the JCR 2.0 specification.
        localRegistry.getNamespaces();

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
        // TODO: Observation
        // this.workspace().observationManager().removeAllEventListeners();
        // TODO: Locks
        // this.lockManager().cleanLocks();
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

    String workspaceName() {
        return workspaceName;
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
        return null;
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

    final int nodeTypesVersion() {
        return this.repository().nodeTypeManager().nodeTypesVersion();
    }

    final JcrLockManager lockManager() throws RepositoryException {
        return workspace().getLockManager();
    }

    final void signalNamespaceChanges( boolean global ) {
        nodeTypeManager().signalNamespaceChanges();
        if (global) repository.nodeTypeManager().signalNamespaceChanges();
    }

    final JcrSession spawnSession( boolean readOnly ) {
        return new JcrSession(repository(), workspaceName, context(), sessionAttributes, readOnly);
    }

    final SessionCache spawnSessionCache( boolean readOnly ) {
        return repository().repositoryCache().createSession(context(), workspaceName(), readOnly);
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
        if (node == null) {
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
                if (this.systemWorkspaceKey.equals(nodeKey.getWorkspaceKey())) {
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
                node = cache.getNode(ref);
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
        return node(cachedNode(cache, node, path).getKey(), null);
    }

    final AbstractJcrNode node( Path absolutePath ) throws PathNotFoundException, AccessDeniedException, RepositoryException {
        assert absolutePath.isAbsolute();
        if (absolutePath.isRoot()) return getRootNode();
        if (absolutePath.isIdentifier()) {
            // Look up the node by identifier ...
            NodeKey key = new NodeKey(stringFactory().create(absolutePath));
            return node(key, null);
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
    public Repository getRepository() {
        return repository;
    }

    @Override
    public String getUserID() {
        return context.getSecurityContext().getUserName();
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
    public Workspace getWorkspace() {
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
        return repository.login(credentials, workspaceName);
    }

    @Deprecated
    @Override
    public AbstractJcrNode getNodeByUUID( String uuid ) throws ItemNotFoundException, RepositoryException {
        return getNodeByIdentifier(uuid);
    }

    @Override
    public AbstractJcrNode getNodeByIdentifier( String id ) throws ItemNotFoundException, RepositoryException {
        checkLive();
        // Try the identifier as a node key ...
        try {
            NodeKey key = new NodeKey(id);
            return node(key, null);
        } catch (ItemNotFoundException e) {
            // continue ...
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
            return parent.getProperty(path.getLastSegment().getName());
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

        if (absolutePath.isRoot()) return true;
        if (absolutePath.isIdentifier()) {
            // Look up the node by identifier ...
            NodeKey key = new NodeKey(stringFactory().create(absolutePath));
            return cache().getNode(key) != null;
        }

        return cachedNode(absolutePath) != null;
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

        // Find the source path and destination path ...
        Path srcPath = absolutePathFor(srcAbsPath);
        Path destPath = absolutePathFor(destAbsPath);
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

        try {
            // Create a session to perform the move ...
            MutableCachedNode mutableSrcParent = srcParent.mutable();
            MutableCachedNode mutableDestParent = destParentNode.mutable();
            mutableSrcParent.moveChild(cache, srcNode.key(), mutableDestParent, destPath.getLastSegment().getName());
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
        cache().save();
        // Record the save operation ...
        repository().statistics().increment(ValueMetric.SESSION_SAVES);
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
        return valueFactory;
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
                       || ModeShapePermissions.DELETE_WORKSPACE.equals(action)) {
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
    private final boolean hasRole( SecurityContext context,
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
        // TODO: Import/export
        return null;
    }

    @Override
    public void importXML( String parentAbsPath,
                           InputStream in,
                           int uuidBehavior )
        throws IOException, PathNotFoundException, ItemExistsException, ConstraintViolationException, VersionException,
        InvalidSerializedDataException, LockException, RepositoryException {
        checkLive();
        // TODO: Import/export
        throw new IOException();
    }

    @Override
    public void exportSystemView( String absPath,
                                  ContentHandler contentHandler,
                                  boolean skipBinary,
                                  boolean noRecurse ) throws PathNotFoundException, SAXException, RepositoryException {
        checkLive();
        // TODO: Import/export
        throw new SAXException();
    }

    @Override
    public void exportSystemView( String absPath,
                                  OutputStream out,
                                  boolean skipBinary,
                                  boolean noRecurse ) throws IOException, PathNotFoundException, RepositoryException {
        checkLive();
        // TODO: Import/export
        throw new IOException();
    }

    @Override
    public void exportDocumentView( String absPath,
                                    ContentHandler contentHandler,
                                    boolean skipBinary,
                                    boolean noRecurse ) throws PathNotFoundException, SAXException, RepositoryException {
        checkLive();
        // TODO: Import/export
        throw new SAXException();
    }

    @Override
    public void exportDocumentView( String absPath,
                                    OutputStream out,
                                    boolean skipBinary,
                                    boolean noRecurse ) throws IOException, PathNotFoundException, RepositoryException {
        checkLive();
        // TODO: Import/export
        throw new IOException();
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
        RunningState running = repository.runningState();
        long lifetime = System.nanoTime() - this.nanosCreated;
        running.statistics().recordDuration(DurationMetric.SESSION_LIFETIME, lifetime, TimeUnit.NANOSECONDS, getUserID());
        running.statistics().decrement(ValueMetric.SESSION_COUNT);
        running.removeSession(this);
    }

    @Override
    public boolean isLive() {
        return isLive;
    }

    @Override
    public void addLockToken( String lockToken ) {
        CheckArg.isNotNull(lockToken, "lockToken");
        JcrLockManager lockManager = null;
        try {
            lockManager = lockManager();
        } catch (RepositoryException le) {
            // basically means this session is not live ...
            return;
        }
        try {
            lockManager.addLockToken(lockToken);
        } catch (LockException le) {
            // For backwards compatibility (and API compatibility), the LockExceptions from the LockManager need to get swallowed
        }
    }

    @Override
    public String[] getLockTokens() {
        JcrLockManager lockManager = null;
        try {
            lockManager = lockManager();
        } catch (RepositoryException le) {
            // basically means this session is not live ...
            return new String[] {};
        }
        return lockManager.getLockTokens();
    }

    @Override
    public void removeLockToken( String lockToken ) {
        CheckArg.isNotNull(lockToken, "lockToken");
        JcrLockManager lockManager = null;
        try {
            lockManager = lockManager();
        } catch (RepositoryException le) {
            // basically means this session is not live ...
            return;
        }
        // A LockException is thrown if the lock associated with the specified lock token is session-scoped.
        try {
            lockManager.removeLockToken(lockToken);
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
                throw new ItemNotFoundException(JcrI18n.itemNotFoundWithUuid.text(key.toString()));
            }
            if (relativePath != null) {
                for (Segment segment : relativePath) {
                    ChildReference child = node.getChildReferences(cache).getChild(segment);
                    CachedNode childNode = cache.getNode(child);
                    if (childNode == null) {
                        Path path = pathFactory().create(node.getPath(cache), segment);
                        throw new ItemNotFoundException(JcrI18n.itemNotFoundAtPath.text(path.getString(namespaces())));
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
}
