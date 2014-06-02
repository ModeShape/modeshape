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
package org.modeshape.jcr;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
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
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionIterator;
import org.infinispan.schematic.SchematicEntry;
import org.modeshape.common.collection.LinkedListMultimap;
import org.modeshape.common.collection.Multimap;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.text.TextDecoder;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.AbstractJcrNode.Type;
import org.modeshape.jcr.JcrContentHandler.EnclosingSAXException;
import org.modeshape.jcr.JcrNamespaceRegistry.Behavior;
import org.modeshape.jcr.JcrRepository.RunningState;
import org.modeshape.jcr.JcrSharedNodeCache.SharedSet;
import org.modeshape.jcr.api.Binary;
import org.modeshape.jcr.api.ValueFactory;
import org.modeshape.jcr.api.monitor.DurationMetric;
import org.modeshape.jcr.api.monitor.ValueMetric;
import org.modeshape.jcr.api.sequencer.Sequencer;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.ChildReference;
import org.modeshape.jcr.cache.ChildReferences;
import org.modeshape.jcr.cache.DocumentAlreadyExistsException;
import org.modeshape.jcr.cache.DocumentNotFoundException;
import org.modeshape.jcr.cache.MutableCachedNode;
import org.modeshape.jcr.cache.NodeCache;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.NodeNotFoundException;
import org.modeshape.jcr.cache.RepositoryCache;
import org.modeshape.jcr.cache.SessionCache;
import org.modeshape.jcr.cache.SessionCache.SaveContext;
import org.modeshape.jcr.cache.SessionCacheWrapper;
import org.modeshape.jcr.cache.WorkspaceNotFoundException;
import org.modeshape.jcr.cache.WrappedException;
import org.modeshape.jcr.cache.document.WorkspaceCache;
import org.modeshape.jcr.query.BufferManager;
import org.modeshape.jcr.security.AdvancedAuthorizationProvider;
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
import org.modeshape.jcr.value.ValueFactories;
import org.modeshape.jcr.value.basic.LocalNamespaceRegistry;
import org.modeshape.jcr.value.basic.NodeIdentifierReferenceFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * 
 */
public class JcrSession implements org.modeshape.jcr.api.Session {

    private static final String[] NO_ATTRIBUTES_NAMES = new String[] {};

    protected final JcrRepository repository;
    private final SessionCache cache;
    private final JcrRootNode rootNode;
    private final ConcurrentMap<NodeKey, AbstractJcrNode> jcrNodes = new ConcurrentHashMap<>();
    private final Map<String, Object> sessionAttributes;
    private final JcrWorkspace workspace;
    private final JcrNamespaceRegistry sessionRegistry;
    private final AtomicReference<Map<NodeKey, NodeKey>> baseVersionKeys = new AtomicReference<>();
    private final AtomicReference<Map<NodeKey, NodeKey>> originalVersionKeys = new AtomicReference<>();
    private final AtomicReference<JcrSharedNodeCache> shareableNodeCache = new AtomicReference<>();
    private final AtomicLong aclChangesCount = new AtomicLong(0);
    private volatile JcrValueFactory valueFactory;
    private volatile boolean isLive = true;
    private final long nanosCreated;
    private volatile BufferManager bufferMgr;

    private ExecutionContext context;

    private final AccessControlManagerImpl acm;

    private final AdvancedAuthorizationProvider.Context authorizerContext = new AdvancedAuthorizationProvider.Context() {
        @Override
        public ExecutionContext getExecutionContext() {
            return context();
        }

        @Override
        public String getRepositoryName() {
            return repository().getName();
        }

        @Override
        public Session getSession() {
            return JcrSession.this;
        }

        @Override
        public String getWorkspaceName() {
            return workspaceName();
        }
    };

    protected JcrSession( JcrRepository repository,
                          String workspaceName,
                          ExecutionContext context,
                          Map<String, Object> sessionAttributes,
                          boolean readOnly ) {
        this.repository = repository;

        // Get the node key of the workspace we're going to use ...
        final RepositoryCache repositoryCache = repository.repositoryCache();
        WorkspaceCache workspace = repositoryCache.getWorkspaceCache(workspaceName);
        NodeKey rootKey = workspace.getRootKey();

        // Now create a specific reference factories that know about the root node key ...
        TextDecoder decoder = context.getDecoder();
        ValueFactories factories = context.getValueFactories();
        ReferenceFactory rootKeyAwareStrongRefFactory = NodeIdentifierReferenceFactory.newInstance(rootKey, decoder, factories,
                                                                                                   false, false);
        ReferenceFactory rootKeyAwareWeakRefFactory = NodeIdentifierReferenceFactory.newInstance(rootKey, decoder, factories,
                                                                                                 true, false);
        ReferenceFactory rootKeyAwareSimpleRefFactory = NodeIdentifierReferenceFactory.newInstance(rootKey, decoder, factories,
                                                                                                   true, true);
        context = context.with(rootKeyAwareStrongRefFactory).with(rootKeyAwareWeakRefFactory).with(rootKeyAwareSimpleRefFactory);

        // Create an execution context for this session that uses a local namespace registry ...
        final NamespaceRegistry globalNamespaceRegistry = context.getNamespaceRegistry(); // thread-safe!
        final LocalNamespaceRegistry localRegistry = new LocalNamespaceRegistry(globalNamespaceRegistry); // not-thread-safe!
        this.context = context.with(localRegistry);
        this.sessionRegistry = new JcrNamespaceRegistry(Behavior.SESSION, localRegistry, globalNamespaceRegistry, this);
        this.workspace = new JcrWorkspace(this, workspaceName);

        // Create the session cache ...
        this.cache = repositoryCache.createSession(context, workspaceName, readOnly);
        this.rootNode = new JcrRootNode(this, this.cache.getRootKey());
        this.jcrNodes.put(this.rootNode.key(), this.rootNode);
        this.sessionAttributes = sessionAttributes != null ? sessionAttributes : Collections.<String, Object>emptyMap();

        // Pre-cache all of the namespaces to be a snapshot of what's in the global registry at this time.
        // This behavior is specified in Section 3.5.2 of the JCR 2.0 specification.
        localRegistry.getNamespaces();

        // Increment the statistics ...
        this.nanosCreated = System.nanoTime();
        repository.statistics().increment(ValueMetric.SESSION_COUNT);

        acm = new AccessControlManagerImpl(this);
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
        acm = new AccessControlManagerImpl(this);
    }

    final JcrWorkspace workspace() {
        return workspace;
    }

    final JcrRepository repository() {
        return repository;
    }

    final synchronized BufferManager bufferManager() {
        if (bufferMgr == null) {
            bufferMgr = new BufferManager(this.context);
        }
        return bufferMgr;
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

        JcrObservationManager jcrObservationManager = observationManager();
        if (jcrObservationManager != null) {
            jcrObservationManager.removeAllEventListeners();
        }

        cleanLocks();
        if (removeFromActiveSession) this.repository.runningState().removeSession(this);
        this.context.getSecurityContext().logout();
    }

    private void cleanLocks() {
        try {
            lockManager().cleanLocks();
        } catch (RepositoryException e) {
            // This can only happen if the session is not live, which is checked above ...
            Logger.getLogger(getClass()).error(e, JcrI18n.unexpectedException, e.getMessage());
        }
    }

    protected SchematicEntry entryForNode( NodeKey nodeKey ) throws RepositoryException {
        SchematicEntry entry = repository.documentStore().get(nodeKey.toString());
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

    public final boolean isReadOnly() {
        return cache().isReadOnly();
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
        // This method returns a SessionCache used by various Session-owned components that can automatically
        // save system content. This session should be notified when such activities happen.
        SessionCache systemCache = repository.createSystemSession(context, readOnly);
        return readOnly ? systemCache : new SystemSessionCache(systemCache);

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

    final void setOriginalVersionKey( NodeKey nodeKey,
                                      NodeKey originalVersionKey ) {
        originalVersionKeys.get().put(nodeKey, originalVersionKey);
    }

    final JcrSession spawnSession( boolean readOnly ) {
        return new JcrSession(this, readOnly);
    }

    final JcrSession spawnSession( String workspaceName,
                                   boolean readOnly ) {
        return new JcrSession(repository(), workspaceName, context(), sessionAttributes, readOnly);
    }

    final SessionCache spawnSessionCache( boolean readOnly ) {
        SessionCache cache = repository().repositoryCache().createSession(context(), workspaceName(), readOnly);
        return readOnly ? cache : new SystemSessionCache(cache);
    }

    final void addContextData( String key,
                               String value ) {
        this.context = context.with(key, value);
        this.cache.addContextData(key, value);
    }

    final JcrSharedNodeCache shareableNodeCache() {
        JcrSharedNodeCache result = this.shareableNodeCache.get();
        if (result == null) {
            this.shareableNodeCache.compareAndSet(null, new JcrSharedNodeCache(this));
            result = this.shareableNodeCache.get();
        }
        return result;
    }

    protected final String readableLocation( CachedNode node ) {
        try {
            return stringFactory().create(node.getPath(cache));
        } catch (Throwable t) {
            return node.getKey().toString();
        }
    }

    protected final long aclChangesCount() {
        return aclChangesCount.longValue();
    }

    protected final long aclAdded(long count) {
        return aclChangesCount.addAndGet(count);
    }

    protected final long aclRemoved(long count) {
        return aclChangesCount.addAndGet(-count);
    }

    protected final String readable( Path path ) {
        return stringFactory().create(path);
    }

    protected void releaseCachedNode( AbstractJcrNode node ) {
        jcrNodes.remove(node.key(), node);
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
        return node(nodeKey, expectedType, null);
    }

    /**
     * Obtain the {@link Node JCR Node} object for the node with the supplied key.
     * 
     * @param nodeKey the node's key
     * @param expectedType the expected implementation type for the node, or null if it is not known
     * @param parentKey the node key for the parent node, or null if the parent is not known
     * @return the JCR node; never null
     * @throws ItemNotFoundException if there is no node with the supplied key
     */
    AbstractJcrNode node( NodeKey nodeKey,
                          AbstractJcrNode.Type expectedType,
                          NodeKey parentKey ) throws ItemNotFoundException {
        CachedNode cachedNode = cache.getNode(nodeKey);
        if (cachedNode == null) {
            // The node must not exist or must have been deleted ...
            throw new ItemNotFoundException(nodeKey.toString());
        }
        AbstractJcrNode node = jcrNodes.get(nodeKey);
        if (node == null) {
            node = node(cachedNode, expectedType, parentKey);
        } else if (parentKey != null) {
            // There was an existing node found, but it might not be the node we're looking for:
            // In some cases (e.g., shared nodes), the node key might be used in multiple parents,
            // and we need to find the right one ...
            node = node(cachedNode, expectedType, parentKey);
        }
        return node;
    }

    /**
     * Obtain the {@link Node JCR Node} object for the node with the supplied key.
     * 
     * @param cachedNode the cached node; may not be null
     * @param expectedType the expected implementation type for the node, or null if it is not known
     * @return the JCR node; never null
     * @see #node(CachedNode, Type, NodeKey)
     */
    AbstractJcrNode node( CachedNode cachedNode,
                          AbstractJcrNode.Type expectedType ) {
        return node(cachedNode, expectedType, null);
    }

    /**
     * Obtain the {@link Node JCR Node} object for the node with the supplied key.
     * 
     * @param cachedNode the cached node; may not be null
     * @param expectedType the expected implementation type for the node, or null if it is not known
     * @param parentKey the node key for the parent node, or null if the parent is not known
     * @return the JCR node; never null
     */
    AbstractJcrNode node( CachedNode cachedNode,
                          AbstractJcrNode.Type expectedType,
                          NodeKey parentKey ) {
        assert cachedNode != null;
        NodeKey nodeKey = cachedNode.getKey();
        AbstractJcrNode node = jcrNodes.get(nodeKey);
        boolean mightBeShared = true;
        if (node == null) {

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
                    mightBeShared = false;
                    break;
                case VERSION_HISTORY:
                    node = new JcrVersionHistoryNode(this, nodeKey);
                    mightBeShared = false;
                    break;
                case SYSTEM:
                    node = new JcrSystemNode(this, nodeKey);
                    mightBeShared = false;
                    break;
                case ROOT:
                    try {
                        return getRootNode();
                    } catch (RepositoryException e) {
                        assert false : "Should never happen: " + e.getMessage();
                    }
            }
            assert node != null;
            AbstractJcrNode newNode = jcrNodes.putIfAbsent(nodeKey, node);
            if (newNode != null) {
                // Another thread snuck in and created the node object ...
                node = newNode;
            }
        }

        if (mightBeShared && parentKey != null && cachedNode.getMixinTypes(cache).contains(JcrMixLexicon.SHAREABLE)) {
            // This is a shareable node, so we have to get the proper Node instance for the given parent ...
            node = node.sharedSet().getSharedNode(cachedNode, parentKey);
        }
        return node;
    }

    final CachedNode cachedNode( Path absolutePath, boolean checkReadPermission ) throws PathNotFoundException, RepositoryException {
        return checkReadPermission ?
               cachedNode(cache, getRootNode().node(), absolutePath, ModeShapePermissions.READ) :
               cachedNode(cache, getRootNode().node(), absolutePath);
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
        // Find the absolute path, based upon the parent ...
        Path absPath = path.isAbsolute() ? path : null;
        if (absPath == null) {
            try {
                // We need to look up the absolute path ..
                if (actions.length > 0) {
                    checkPermission(node, cache, actions);
                }
            } catch (NodeNotFoundException e) {
                throw new PathNotFoundException(JcrI18n.nodeNotFound.text(stringFactory().create(path), workspaceName()));
            }
        }
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
        CachedNode child = cachedNode(cache, node, path, ModeShapePermissions.READ);
        AbstractJcrNode result = node(child, (Type)null, null);
        if (result.isShareable()) {
            // Find the shared node with the desired path ...
            AbstractJcrNode atOrBelow = result.sharedSet().getSharedNodeAtOrBelow(path);
            if (atOrBelow != null) result = atOrBelow;
        }
        return result;
    }

    final AbstractJcrNode node( Path absolutePath ) throws PathNotFoundException, AccessDeniedException, RepositoryException {
        assert absolutePath.isAbsolute();
        if (absolutePath.isRoot()) return getRootNode();
        if (absolutePath.isIdentifier()) {
            // Look up the node by identifier ...
            String identifierString = stringFactory().create(absolutePath).replaceAll("\\[", "").replaceAll("\\]", "");
            return getNodeByIdentifier(identifierString);
        }
        CachedNode node = getRootNode().node();
        return node(node, absolutePath);
    }

    final AbstractJcrItem findItem( NodeKey nodeKey,
                                    Path relativePath ) throws RepositoryException {
        return findItem(node(nodeKey, null, null), relativePath);
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
                AbstractJcrNode node = node(key, null);
                checkPermission(pathSupplierFor(node), ModeShapePermissions.READ);
                return node;
            } catch (ItemNotFoundException e) {
                // continue ...
            }
        }
        // First, we're given a partial key, so look first in this workspace's content ...
        NodeKey key = null;
        ItemNotFoundException first = null;
        try {
            // Try as node key identifier ...
            key = this.rootNode.key.withId(id);
            AbstractJcrNode node = node(key, null);
            checkPermission(pathSupplierFor(node), ModeShapePermissions.READ);
            return node;
        } catch (ItemNotFoundException e) {
            // Not found, so capture the exception (which we might use later) and continue ...
            first = e;
        }
        // Next look for it using the same key except with the system workspace part ...
        try {
            String systemWorkspaceKey = this.repository().systemWorkspaceKey();
            key = key.withWorkspaceKey(systemWorkspaceKey);
            AbstractJcrNode systemNode = node(key, null);
            if (systemNode instanceof JcrVersionHistoryNode) {
                // because the version history node has the same key as the original node, we don't want to expose it to clients
                // this means that if we got this far, the original hasn't been found, so neither should the version history
                throw first;
            }
            checkPermission(pathSupplierFor(systemNode), ModeShapePermissions.READ);
            return systemNode;
        } catch (ItemNotFoundException e) {
            // Not found, so throw the original exception ...
            throw first;
        }
    }

    /**
     * A variant of the standard {@link #getNodeByIdentifier(String)} method that does <i>not</i> find nodes within the system
     * area. This is often needed by the {@link JcrVersionManager} functionality.
     * 
     * @param id the string identifier
     * @return the node; never null
     * @throws ItemNotFoundException if a node cannot be found in the non-system content of the repository
     * @throws RepositoryException if there is another problem
     * @see #getNodeByIdentifier(String)
     */
    public AbstractJcrNode getNonSystemNodeByIdentifier( String id ) throws ItemNotFoundException, RepositoryException {
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
        return getNode(absPath, false);
    }

    protected AbstractJcrNode getNode( String absPath,
                                       boolean accessControlScope ) throws PathNotFoundException, RepositoryException {
        checkLive();
        CheckArg.isNotEmpty(absPath, "absolutePath");
        Path path = absolutePathFor(absPath);

        if (!accessControlScope) {
            checkPermission(path, ModeShapePermissions.READ);
        }
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
        try {
            return node(absolutePath) != null;
        } catch (PathNotFoundException e) {
            return false;
        }
    }

    /**
     * Utility method to determine if the node with the specified key still exists within the transient & persisted state.
     * 
     * @param key the key of the node; may not be null
     * @return true if the node exists, or false if it does not
     */
    protected boolean nodeExists( NodeKey key ) {
        return cache.getNode(key) != null;
    }

    @Override
    public boolean propertyExists( String absPath ) throws RepositoryException {
        checkLive();
        CheckArg.isNotEmpty(absPath, "absPath");
        Path path = absolutePathFor(absPath);

        // Check the kind of path ...
        if (path.isRoot() || path.isIdentifier()) {
            // These are not properties ...
            return false;
        }

        // There is at least one segment ...
        Segment lastSegment = path.getLastSegment();
        if (lastSegment.hasIndex()) {
            throw new RepositoryException(JcrI18n.pathCannotHaveSameNameSiblingIndex.text(absPath));
        }

        try {
            // This will throw a PNFE if the parent path does not exist
            CachedNode parentNode = cachedNode(path.getParent(), true);
            return parentNode != null && parentNode.hasProperty(lastSegment.getName(), cache());
        } catch (PathNotFoundException e) {
            return false;
        }
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

        // Check whether external nodes are involved
        validateMoveForExternalNodes(srcPath, destPath);

        // check whether the parent definition allows children which match the source
        final Name newChildName = destPath.getLastSegment().getName();
        destParentNode.validateChildNodeDefinition(newChildName, srcNode.getPrimaryTypeName(), true);

        // We already checked whether the supplied destination path is below the supplied source path, but this isn't
        // sufficient if any of the ancestors are shared nodes. Therefore, check whether the destination node
        // is actually underneath the source node by walking up the destination path to see if there are any
        // shared nodes (including the shareable node) below the source path ...
        AbstractJcrNode destAncestor = destParentNode;
        while (!destAncestor.isRoot()) {
            if (destAncestor.isShareable()) {
                SharedSet sharedSet = destAncestor.sharedSet();
                AbstractJcrNode sharedNodeThatCreatesCircularity = sharedSet.getSharedNodeAtOrBelow(srcPath);
                if (sharedNodeThatCreatesCircularity != null) {
                    Path badPath = sharedNodeThatCreatesCircularity.path();
                    throw new RepositoryException(JcrI18n.unableToMoveNodeDueToCycle.text(srcAbsPath, destAbsPath,
                                                                                          readable(badPath)));
                }
            }
            destAncestor = destAncestor.getParent();
        }

        try {
            MutableCachedNode mutableSrcParent = srcParent.mutable();
            MutableCachedNode mutableDestParent = destParentNode.mutable();
            if (mutableSrcParent.equals(mutableDestParent)) {
                // It's just a rename ...
                mutableSrcParent.renameChild(sessionCache, srcNode.key(), destPath.getLastSegment().getName());
            } else {
                // It is a move from one parent to another ...
                mutableSrcParent.moveChild(sessionCache, srcNode.key(), mutableDestParent, newChildName);
            }
        } catch (NodeNotFoundException e) {
            // Not expected ...
            String msg = JcrI18n.nodeNotFound.text(stringFactory().create(srcPath.getParent()), workspaceName());
            throw new PathNotFoundException(msg);
        }
    }

    private void validateMoveForExternalNodes( Path srcPath,
                                               Path destPath ) throws RepositoryException {
        AbstractJcrNode srcNode = node(srcPath);
        String rootSourceKey = getRootNode().key().getSourceKey();

        Set<NodeKey> sourceNodeKeys = cache().getNodeKeysAtAndBelow(srcNode.key());
        boolean sourceContainsExternalNodes = false;
        String externalSourceKey = null;
        for (NodeKey sourceNodeKey : sourceNodeKeys) {
            if (!sourceNodeKey.getSourceKey().equalsIgnoreCase(rootSourceKey)) {
                externalSourceKey = sourceNodeKey.getSourceKey();
                sourceContainsExternalNodes = true;
                break;
            }
        }

        AbstractJcrNode destNode = null;
        try {
            destNode = node(destPath);
        } catch (PathNotFoundException e) {
            // the destPath does not point to an existing node, so we'll use the parent
            destNode = node(destPath.getParent());
        }
        String externalTargetKey = null;
        boolean targetIsExternal = false;
        if (!destNode.key().getSourceKey().equalsIgnoreCase(rootSourceKey)) {
            targetIsExternal = true;
            externalTargetKey = destNode.key().getSourceKey();
        }

        Connectors connectors = repository().runningState().connectors();
        if (sourceContainsExternalNodes && !targetIsExternal) {
            String sourceName = connectors.getSourceNameAtKey(externalSourceKey);
            throw new RepositoryException(JcrI18n.unableToMoveSourceContainExternalNodes.text(srcPath, sourceName));
        } else if (!sourceContainsExternalNodes && targetIsExternal) {
            String sourceName = connectors.getSourceNameAtKey(externalTargetKey);
            throw new RepositoryException(JcrI18n.unableToMoveTargetContainExternalNodes.text(srcPath, sourceName));
        } else if (targetIsExternal) {
            // both source and target are external nodes, but belonging to different sources
            assert externalTargetKey != null;
            if (!externalTargetKey.equalsIgnoreCase(srcNode.key().getSourceKey())) {
                String sourceNodeSourceName = connectors.getSourceNameAtKey(srcNode.key().getSourceKey());
                String targetNodeSourceName = connectors.getSourceNameAtKey(externalTargetKey);
                throw new RepositoryException(JcrI18n.unableToMoveSourceTargetMismatch.text(sourceNodeSourceName,
                                                                                            targetNodeSourceName));
            }

            // both source and target belong to the same source, but one of them is a projection root
            if (connectors.hasExternalProjection(srcPath.getLastSegment().getString(), srcNode.key().toString())) {
                throw new RepositoryException(JcrI18n.unableToMoveProjection.text(srcPath));
            }

            if (connectors.hasExternalProjection(destPath.getLastSegment().getString(), destNode.key().toString())) {
                throw new RepositoryException(JcrI18n.unableToMoveProjection.text(destPath));
            }
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
        Map<NodeKey, NodeKey> originalVersionKeys = this.originalVersionKeys.get();
        try {
            cache().save(systemContent.cache(),
                         new JcrPreSave(systemContent, baseVersionKeys, originalVersionKeys, aclChangesCount()));
            this.baseVersionKeys.set(null);
            this.originalVersionKeys.set(null);
            this.aclChangesCount.set(0);
        } catch (WrappedException e) {
            Throwable cause = e.getCause();
            throw (cause instanceof RepositoryException) ? (RepositoryException)cause : new RepositoryException(e.getCause());
        } catch (DocumentNotFoundException e) {
            throw new InvalidItemStateException(JcrI18n.nodeModifiedBySessionWasRemovedByAnotherSession.text(e.getKey()), e);
        } catch (DocumentAlreadyExistsException e) {
            // Try to figure out which node in this transient state was the problem ...
            NodeKey key = new NodeKey(e.getKey());
            AbstractJcrNode problemNode = node(key, null);
            String path = problemNode.getPath();
            throw new InvalidItemStateException(JcrI18n.nodeCreatedBySessionUsedExistingKey.text(path, key), e);
        } catch (org.modeshape.jcr.cache.ReferentialIntegrityException e) {
            throw new ReferentialIntegrityException(e);
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
        // first check the node is valid from a cache perspective
        Set<NodeKey> keysToBeSaved = null;
        try {
            if (node.isNew()) {
                // expected by TCK
                throw new RepositoryException(JcrI18n.unableToSaveNodeThatWasCreatedSincePreviousSave.text(node.getPath(),
                                                                                                           workspaceName()));
            }

            AtomicReference<Set<NodeKey>> refToKeys = new AtomicReference<Set<NodeKey>>();
            if (node.containsChangesWithExternalDependencies(refToKeys)) {
                // expected by TCK
                I18n msg = JcrI18n.unableToSaveBranchBecauseChangesDependOnChangesToNodesOutsideOfBranch;
                throw new ConstraintViolationException(msg.text(node.path(), workspaceName()));
            }
            keysToBeSaved = refToKeys.get();
        } catch (ItemNotFoundException e) {
            throw new InvalidItemStateException(e);
        } catch (NodeNotFoundException e) {
            throw new InvalidItemStateException(e);
        }
        assert keysToBeSaved != null;

        SessionCache sessionCache = cache();
        if (sessionCache.getChangedNodeKeys().size() == keysToBeSaved.size()) {
            // The node is above all the other changes, so go ahead and save the whole session ...
            save();
            return;
        }

        // Perform the save, using 'JcrPreSave' operations ...
        SessionCache systemCache = createSystemCache(false);
        SystemContent systemContent = new SystemContent(systemCache);
        Map<NodeKey, NodeKey> baseVersionKeys = this.baseVersionKeys.get();
        Map<NodeKey, NodeKey> originalVersionKeys = this.originalVersionKeys.get();
        try {
            sessionCache.save(keysToBeSaved, systemContent.cache(), new JcrPreSave(systemContent, baseVersionKeys,
                                                                                   originalVersionKeys, aclChangesCount()));
        } catch (WrappedException e) {
            Throwable cause = e.getCause();
            throw (cause instanceof RepositoryException) ? (RepositoryException)cause : new RepositoryException(e.getCause());
        } catch (DocumentNotFoundException e) {
            throw new InvalidItemStateException(JcrI18n.nodeModifiedBySessionWasRemovedByAnotherSession.text(e.getKey()), e);
        } catch (DocumentAlreadyExistsException e) {
            // Try to figure out which node in this transient state was the problem ...
            NodeKey key = new NodeKey(e.getKey());
            AbstractJcrNode problemNode = node(key, null);
            String path = problemNode.getPath();
            throw new InvalidItemStateException(JcrI18n.nodeCreatedBySessionUsedExistingKey.text(path, key), e);
        } catch (org.modeshape.jcr.cache.ReferentialIntegrityException e) {
            throw new ReferentialIntegrityException(e);
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
            aclChangesCount.set(0);
        }
        // Otherwise there is nothing to do, as all persistent changes are always immediately visible to all sessions
        // using that same workspace
    }

    @Override
    public boolean hasPendingChanges() throws RepositoryException {
        checkLive();
        return cache().hasChanges();
    }

    @Override
    public JcrValueFactory getValueFactory() throws RepositoryException {
        checkLive();
        return valueFactory();
    }

    private static interface PathSupplier {
        /**
         * Get the absolute path
         * 
         * @return the absolute path
         * @throws ItemNotFoundException if the node was deleted
         */
        Path getAbsolutePath() throws ItemNotFoundException;
    }

    private PathSupplier pathSupplierFor( final Path path ) {
        return new PathSupplier() {
            @Override
            public Path getAbsolutePath() {
                return path;
            }
        };
    }

    private PathSupplier pathSupplierFor( final CachedNode node,
                                          final NodeCache nodeCache ) {
        return new PathSupplier() {
            @Override
            public Path getAbsolutePath() throws ItemNotFoundException {
                return node.getPath(nodeCache);
            }
        };
    }

    private PathSupplier pathSupplierFor( final AbstractJcrItem item ) {
        return new PathSupplier() {
            @Override
            public Path getAbsolutePath() throws ItemNotFoundException {
                try {
                    return item.path();
                } catch (InvalidItemStateException err) {
                    // This happens when the session removes the node, but we know that couldn't have happened since we have the
                    // node instance, so ignore it ...
                } catch (ItemNotFoundException err) {
                    throw err;
                } catch (RepositoryException e) {
                    return null;
                }
                assert false;
                return null;
            }
        };
    }

    /**
     * Determine if the current user does not have permission for all of the named actions in the named workspace in the given
     * context, otherwise returns silently.
     * 
     * @param workspaceName the name of the workspace in which the path exists
     * @param pathSupplier the supplier for the path on which the actions are occurring; may be null if the permission is on the
     *        whole workspace
     * @param actions the list of {@link ModeShapePermissions actions} to check
     * @return true if the subject has privilege to perform all of the named actions on the content at the supplied path in the
     *         given workspace within the repository, or false otherwise
     */
    private boolean hasPermission( String workspaceName,
                                   PathSupplier pathSupplier,
                                   String... actions ) {
        SecurityContext sec = context.getSecurityContext();
        final boolean checkAcl = repository.repositoryCache().isAccessControlEnabled();

        boolean hasPermission = true;

        final String repositoryName = this.repository.repositoryName();
        try {
            if (sec instanceof AuthorizationProvider) {
                // Delegate to the security context ...
                AuthorizationProvider authorizer = (AuthorizationProvider)sec;
                Path path = pathSupplier != null ? pathSupplier.getAbsolutePath() : null;
                if (path != null) {
                    assert path.isAbsolute() : "The path (if provided) must be absolute";
                    hasPermission = authorizer.hasPermission(context, repositoryName, repositoryName, workspaceName, path, actions);
    
                    if (checkAcl && hasPermission) {
                        hasPermission = acm.hasPermission(path, actions);
                    }
                    return hasPermission;
                }
            }

            if (sec instanceof AdvancedAuthorizationProvider) {
                // Delegate to the security context ...
                AdvancedAuthorizationProvider authorizer = (AdvancedAuthorizationProvider)sec;
                Path path = pathSupplier != null ? pathSupplier.getAbsolutePath() : null;
                if (path != null) {
                    assert path.isAbsolute() : "The path (if provided) must be absolute";
                    hasPermission = authorizer.hasPermission(authorizerContext, path, actions);
    
                    if (checkAcl && hasPermission) {
                        hasPermission = acm.hasPermission(path, actions);
                    }
                    return hasPermission;
                }
            }

            // It is a role-based security context, so apply role-based authorization ...
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

            if (checkAcl && hasPermission) {
                Path path = pathSupplier != null ? pathSupplier.getAbsolutePath() : null;
                if (path != null) {
                    assert path.isAbsolute() : "The path (if provided) must be absolute";
                    hasPermission = acm.hasPermission(path, actions);
                }
            }

            return hasPermission;
        } catch (ItemNotFoundException err) {
            // The node was removed from this session
            return false;
        }
    }

    private boolean hasPermissionOnExternalPath( PathSupplier pathSupplier,
                                                 String... actions ) throws RepositoryException {
        Connectors connectors = this.repository().runningState().connectors();
        if (!connectors.hasConnectors() || !connectors.hasReadonlyConnectors()) {
            // federation is not enabled or there are no readonly connectors
            return true;
        }
        Path path = pathSupplier.getAbsolutePath();
        if (path == null) return false;
        if (connectors.isReadonlyPath(path, this)) {
            // this is a readonly external path, so we need to see what the actual actions are
            if (actions.length > ModeShapePermissions.READONLY_EXTERNAL_PATH_PERMISSIONS.size()) {
                return false;
            }
            List<String> actionsList = new ArrayList<String>(Arrays.asList(actions));
            for (Iterator<String> actionsIterator = actionsList.iterator(); actionsIterator.hasNext();) {
                String action = actionsIterator.next();
                if (!ModeShapePermissions.READONLY_EXTERNAL_PATH_PERMISSIONS.contains(action)) {
                    return false;
                }
                actionsIterator.remove();
            }
            return actionsList.isEmpty();
        }
        return true;
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
    static boolean hasRole( SecurityContext context,
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
            PathSupplier supplier = pathSupplierFor(absolutePathFor(path));
            String[] actionsArray = actions.split(",");
            checkPermission(workspace().getName(), supplier, actionsArray);
            if (!hasPermissionOnExternalPath(supplier, actionsArray)) {
                String absPath = supplier.getAbsolutePath().getString(namespaces());
                throw new AccessDeniedException(JcrI18n.permissionDenied.text(absPath, actions));
            }
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
     * @param path the absolute path on which the actions are occurring
     * @param actions a comma-delimited list of actions to check
     * @throws AccessDeniedException if the actions cannot be performed on the node at the specified path
     */
    void checkPermission( Path path,
                          String... actions ) throws AccessDeniedException {
        checkPermission(this.workspace().getName(), path, actions);
    }

    void checkPermission( PathSupplier pathSupplier,
                          String... actions ) throws AccessDeniedException {
        checkPermission(this.workspace().getName(), pathSupplier, actions);
    }

    /**
     * Throws an {@link AccessControlException} if the current user does not have permission for all of the named actions in the
     * current workspace, otherwise returns silently.
     * <p>
     * The {@code path} parameter is included for future use and is currently ignored
     * </p>
     * 
     * @param item the property or node on which the actions are occurring
     * @param actions a comma-delimited list of actions to check
     * @throws AccessDeniedException if the actions cannot be performed on the node at the specified path
     */
    void checkPermission( AbstractJcrItem item,
                          String... actions ) throws AccessDeniedException {
        checkPermission(this.workspace().getName(), pathSupplierFor(item), actions);
    }

    void checkPermission( CachedNode node,
                          NodeCache cache,
                          String... actions ) throws AccessDeniedException {
        checkPermission(this.workspace().getName(), pathSupplierFor(node, cache), actions);
    }

    /**
     * Throws an {@link AccessControlException} if the current user does not have permission for all of the named actions in the
     * named workspace, otherwise returns silently.
     * <p>
     * The {@code path} parameter is included for future use and is currently ignored
     * </p>
     * 
     * @param workspaceName the name of the workspace in which the path exists
     * @param path the absolute path on which the actions are occurring
     * @param actions a comma-delimited list of actions to check
     * @throws AccessDeniedException if the actions cannot be performed on the node at the specified path
     */
    void checkPermission( String workspaceName,
                          Path path,
                          String... actions ) throws AccessDeniedException {
        checkPermission(workspaceName, pathSupplierFor(path), actions);
    }

    void checkPermission( String workspaceName,
                          PathSupplier pathSupplier,
                          String... actions ) throws AccessDeniedException {
        CheckArg.isNotEmpty(actions, "actions");

        if (hasPermission(workspaceName, pathSupplier, actions)) return;

        String pathAsString = "<unknown>";
        if (pathSupplier != null) {
            try {
                pathAsString = pathSupplier.getAbsolutePath().getString(namespaces());
            } catch (ItemNotFoundException e) {
                // Node was somehow removed from this session
            }
        }
        throw new AccessDeniedException(JcrI18n.permissionDenied.text(pathAsString, actions));
    }

    void checkWorkspacePermission( String workspaceName,
                                   String... actions ) throws AccessDeniedException {
        checkPermission(workspaceName, (PathSupplier)null, actions);
    }

    @Override
    public boolean hasPermission( String absPath,
                                  String actions ) throws RepositoryException {
        checkLive();
        CheckArg.isNotEmpty(absPath, "absPath");
        PathSupplier pathSupplier = pathSupplierFor(absolutePathFor(absPath));
        String[] actionsArray = actions.split(",");
        String workspaceName = this.workspace().getName();
        return hasPermission(workspaceName, pathSupplier, actionsArray)
               && hasPermissionOnExternalPath(pathSupplier, actionsArray);
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

    protected void initOriginalVersionKeys() {
        originalVersionKeys.compareAndSet(null, new ConcurrentHashMap<NodeKey, NodeKey>());
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
    public synchronized void logout() {
        this.isLive = false;
        cleanLocks();
        try {
            RunningState running = repository.runningState();
            long lifetime = Math.abs(System.nanoTime() - this.nanosCreated);
            Map<String, String> payload = Collections.singletonMap("userId", getUserID());
            running.statistics().recordDuration(DurationMetric.SESSION_LIFETIME, lifetime, TimeUnit.NANOSECONDS, payload);
            running.statistics().decrement(ValueMetric.SESSION_COUNT);
            running.removeSession(this);
        } catch (IllegalStateException e) {
            // The repository has been shutdown
        } finally {
            if (bufferMgr != null) {
                try {
                    bufferMgr.close();
                } finally {
                    bufferMgr = null;
                }
            }
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
    public AccessControlManager getAccessControlManager() {
        return acm;
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
                        throw new ItemNotFoundException(JcrI18n.itemNotFoundAtPath.text(path.getString(namespaces()),
                                                                                        workspaceName()));
                    }
                    CachedNode childNode = cache.getNode(child);
                    if (childNode == null) {
                        Path path = pathFactory().create(node.getPath(cache), segment);
                        throw new ItemNotFoundException(JcrI18n.itemNotFoundAtPath.text(path.getString(namespaces()),
                                                                                        workspaceName()));
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

    /**
     * Determine if the supplied string represents just the {@link Node#getIdentifier() node's identifier} or whether it is a
     * string representation of a NodeKey. If it is just the node's identifier, then the NodeKey is created by using the same
     * {@link NodeKey#getSourceKey() source key} and {@link NodeKey#getWorkspaceKey() workspace key} from the supplied root node.
     * 
     * @param identifier the identifier string; may not be null
     * @param rootKey the node of the root in the workspace; may not be null
     * @return the node key re-created from the supplied identifier; never null
     */
    public static NodeKey createNodeKeyFromIdentifier( String identifier,
                                                       NodeKey rootKey ) {
        // If this node is a random identifier, then we need to use it as a node key identifier ...
        if (NodeKey.isValidRandomIdentifier(identifier)) {
            return rootKey.withId(identifier);
        }
        return new NodeKey(identifier);
    }

    /**
     * Checks if the node given key is foreign by comparing the source key & workspace key against the same keys from this
     * session's root. This method is used for reference resolving.
     * 
     * @param key the node key; may be null
     * @param rootKey the key of the root node in the workspace; may not be null
     * @return true if the node key is considered foreign, false otherwise.
     */
    public static boolean isForeignKey( NodeKey key,
                                        NodeKey rootKey ) {
        if (key == null) {
            return false;
        }
        String nodeWorkspaceKey = key.getWorkspaceKey();

        boolean sameWorkspace = rootKey.getWorkspaceKey().equals(nodeWorkspaceKey);
        boolean sameSource = rootKey.getSourceKey().equalsIgnoreCase(key.getSourceKey());
        return !sameWorkspace || !sameSource;
    }

    /**
     * Returns a string representing a node's identifier, based on whether the node is foreign or not.
     * 
     * @param key the node key; may be null
     * @param rootKey the key of the root node in the workspace; may not be null
     * @return the identifier for the node; never null
     * @see javax.jcr.Node#getIdentifier()
     */
    public static String nodeIdentifier( NodeKey key,
                                         NodeKey rootKey ) {
        return isForeignKey(key, rootKey) ? key.toString() : key.getIdentifier();
    }

    /**
     * Checks if the node given key is foreign by comparing the source key & workspace key against the same keys from this
     * session's root. This method is used for reference resolving.
     * 
     * @param key the node key; may be null
     * @return true if the node key is considered foreign, false otherwise.
     */
    protected final boolean isForeignKey( NodeKey key ) {
        return isForeignKey(key, cache.getRootKey());
    }

    /**
     * Returns a string representing a node's identifier, based on whether the node is foreign or not.
     * 
     * @param key the node key; may be null
     * @return the identifier for the node; never null
     * @see javax.jcr.Node#getIdentifier()
     */
    protected final String nodeIdentifier( NodeKey key ) {
        return nodeIdentifier(key, cache.getRootKey());
    }

    @Override
    public boolean sequence( String sequencerName,
                             Property inputProperty,
                             Node outputNode ) throws RepositoryException {
        CheckArg.isSame(inputProperty.getSession(), "inputProperty", this, "this session");
        CheckArg.isSame(outputNode.getSession(), "outputNode", this, "this session");
        Sequencer sequencer = repository().runningState().sequencers().getSequencer(sequencerName);
        if (sequencer == null) return false;

        final ValueFactory values = getValueFactory();
        final DateTime now = dateFactory().create();
        final Sequencer.Context context = new Sequencer.Context() {

            @Override
            public ValueFactory valueFactory() {
                return values;
            }

            @Override
            public Calendar getTimestamp() {
                return now.toCalendar();
            }
        };
        try {
            if (sequencer.hasAcceptedMimeTypes()) {
                // Get the MIME type, first by looking at the changed property's parent node
                // (or grand-parent node if parent is 'jcr:content') ...
                String mimeType = SequencingRunner.getInputMimeType(inputProperty);

                // See if the sequencer accepts the MIME type ...
                if (mimeType != null && !sequencer.isAccepted(mimeType)) {
                    Logger.getLogger(getClass())
                          .debug("Skipping sequencing because input's MIME type '{0}' is not accepted by the '{1}' sequencer",
                                 mimeType, sequencerName);
                    return false;
                }
            }
            return sequencer.execute(inputProperty, outputNode, context);
        } catch (RepositoryException e) {
            throw e;
        } catch (Exception e) {
            throw new RepositoryException(e);
        }
    }

    @Override
    public String toString() {
        return cache.toString();
    }

    @Override
    public String decode( final String localName ) {
        return Path.JSR283_DECODER.decode(localName);
    }

    @Override
    public String encode( final String localName ) {
        return Path.JSR283_ENCODER.encode(localName);
    }

    /**
     * Define the operations that are to be performed on all the nodes that were created or modified within this session. This
     * class was designed to be as efficient as possible for most nodes, since most nodes do not need any additional processing.
     */
    protected final class JcrPreSave implements SessionCache.PreSave {
        private final SessionCache cache;
        private final SessionCache systemCache;
        private final RepositoryNodeTypeManager nodeTypeMgr;
        private final NodeTypes nodeTypeCapabilities;
        private final SystemContent systemContent;
        private final Map<NodeKey, NodeKey> baseVersionKeys;
        private final Map<NodeKey, NodeKey> originalVersionKeys;

        private boolean initialized = false;
        private PropertyFactory propertyFactory;
        private ReferenceFactory referenceFactory;
        private JcrVersionManager versionManager;

        protected JcrPreSave( SystemContent content,
                              Map<NodeKey, NodeKey> baseVersionKeys,
                              Map<NodeKey, NodeKey> originalVersionKeys,
                              long aclChangesCount ) {
            assert content != null;
            this.cache = cache();
            this.systemContent = content;
            this.systemCache = content.cache();
            this.baseVersionKeys = baseVersionKeys;
            this.originalVersionKeys = originalVersionKeys;

            // Get the capabilities cache. This is immutable, so we'll use it for the entire pre-save operation ...
            this.nodeTypeMgr = repository().nodeTypeManager();
            this.nodeTypeCapabilities = nodeTypeMgr.getNodeTypes();

            if (aclChangesCount != 0) {
                aclMetadataRefresh(aclChangesCount);
            }
        }

        private void aclMetadataRefresh( long aclChangesCount ) {
            // we have a session that has added and/or removed ACLs from nodes, so we need to reflect this in the repository
            // metadata
            MutableCachedNode systemNode = systemContent.mutableSystemNode();
            org.modeshape.jcr.value.Property aclCount = systemNode.getProperty(ModeShapeLexicon.ACL_COUNT, systemCache);
            if (aclCount == null && aclChangesCount > 0) {
                systemNode.setProperty(systemCache, propertyFactory().create(ModeShapeLexicon.ACL_COUNT, aclChangesCount));
                repository().repositoryCache().setAccessControlEnabled(true);
            } else if (aclCount != null) {
                long newCount = Long.valueOf(aclCount.getFirstValue().toString()) + aclChangesCount;
                if (newCount < 0) {
                    newCount = 0;
                }
                if (newCount == 0) {
                    repository().repositoryCache().setAccessControlEnabled(false);
                }
                systemNode.setProperty(systemCache, propertyFactory().create(ModeShapeLexicon.ACL_COUNT, newCount));
            }
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
                    // it may happen during an import, that a node with version history & base version is assigned a new key and
                    // therefore
                    // the base version points to an existing version while no version history is found initially
                    boolean shouldCreateNewVersionHistory = true;
                    if (baseVersionKey != null) {
                        CachedNode baseVersionNode = systemCache.getNode(baseVersionKey);
                        if (baseVersionNode != null) {
                            historyKey = baseVersionNode.getParentKey(systemCache);
                            shouldCreateNewVersionHistory = (historyKey == null);
                        }
                    }
                    if (shouldCreateNewVersionHistory) {
                        // a new version history should be initialized
                        assert historyKey != null;
                        if (baseVersionKey == null) baseVersionKey = historyKey.withRandomId();
                        NodeKey originalVersionKey = originalVersionKeys != null ? originalVersionKeys.get(versionableKey) : null;
                        Path versionHistoryPath = versionManager.versionHistoryPathFor(versionableKey);
                        systemContent.initializeVersionStorage(versionableKey, historyKey, baseVersionKey, primaryType,
                                                               mixinTypes, versionHistoryPath, originalVersionKey,
                                                               context.getTime());
                    }

                    // Now update the node as if it's checked in (with the exception of the predecessors...)
                    Reference historyRef = referenceFactory.create(historyKey, true);
                    Reference baseVersionRef = referenceFactory.create(baseVersionKey, true);
                    node.setProperty(cache, propertyFactory.create(JcrLexicon.IS_CHECKED_OUT, Boolean.TRUE));
                    node.setReference(cache, propertyFactory.create(JcrLexicon.VERSION_HISTORY, historyRef), systemCache);
                    node.setReference(cache, propertyFactory.create(JcrLexicon.BASE_VERSION, baseVersionRef), systemCache);
                    // JSR 283 - 15.1
                    node.setReference(cache, propertyFactory.create(JcrLexicon.PREDECESSORS, new Object[] {baseVersionRef}),
                                      systemCache);
                } else {
                    // we're dealing with node which has a version history, check if there any versionable properties present
                    boolean hasVersioningProperties = node.hasProperty(JcrLexicon.IS_CHECKED_OUT, cache)
                                                      || node.hasProperty(JcrLexicon.VERSION_HISTORY, cache)
                                                      || node.hasProperty(JcrLexicon.BASE_VERSION, cache)
                                                      || node.hasProperty(JcrLexicon.PREDECESSORS, cache);

                    if (!hasVersioningProperties) {
                        // the node doesn't have any versionable properties, so this is a case of mix:versionable removed at some
                        // point and then re-added. If it had any versioning properties, we might've been dealing with something
                        // else
                        // e.g. a restore

                        // Re-link the versionable properties, based on the existing version history
                        node.setProperty(cache, propertyFactory.create(JcrLexicon.IS_CHECKED_OUT, Boolean.TRUE));

                        JcrVersionHistoryNode versionHistoryNode = versionManager().getVersionHistory(node(node.getKey(), null));
                        Reference historyRef = referenceFactory.create(versionHistoryNode.key(), true);
                        node.setReference(cache, propertyFactory.create(JcrLexicon.VERSION_HISTORY, historyRef), systemCache);

                        // set the base version to the last existing version
                        JcrVersionNode baseVersion = null;
                        for (VersionIterator versionIterator = versionHistoryNode.getAllVersions(); versionIterator.hasNext();) {
                            JcrVersionNode version = (JcrVersionNode)versionIterator.nextVersion();
                            if (baseVersion == null || version.isLinearSuccessorOf(baseVersion)) {
                                baseVersion = version;
                            }
                        }
                        assert baseVersion != null;
                        Reference baseVersionRef = referenceFactory.create(baseVersion.key(), true);
                        node.setReference(cache, propertyFactory.create(JcrLexicon.BASE_VERSION, baseVersionRef), systemCache);

                        // set the predecessors to the same list as the base version's predecessors
                        Version[] baseVersionPredecessors = baseVersion.getPredecessors();
                        Reference[] predecessors = new Reference[baseVersionPredecessors.length];
                        for (int i = 0; i < baseVersionPredecessors.length; i++) {
                            predecessors[i] = referenceFactory.create(((JcrVersionNode)baseVersionPredecessors[i]).key(), true);
                        }
                        node.setReference(cache, propertyFactory.create(JcrLexicon.PREDECESSORS, predecessors), systemCache);
                    }
                }
            }

            // -----------
            // nt:resource
            // -----------
            if (nodeTypeCapabilities.isNtResource(primaryType)) {
                // If there is no "jcr:mimeType" property ...
                if (!node.hasProperty(JcrLexicon.MIMETYPE, cache)) {
                    // Try to get the MIME type for the binary value ...
                    org.modeshape.jcr.value.Property dataProp = node.getProperty(JcrLexicon.DATA, cache);
                    if (dataProp != null) {
                        Object dataValue = dataProp.getFirstValue();
                        if (dataValue instanceof Binary) {
                            Binary binaryValue = (Binary)dataValue;
                            // Get the name of this node's parent ...
                            String fileName = null;
                            NodeKey parentKey = node.getParentKey(cache);
                            if (parentKey != null) {
                                CachedNode parent = cache.getNode(parentKey);
                                Name parentName = parent.getName(cache);
                                fileName = stringFactory().create(parentName);
                            }
                            String mimeType = binaryValue.getMimeType(fileName);
                            if (mimeType != null) {
                                node.setProperty(cache, propertyFactory.create(JcrLexicon.MIMETYPE, mimeType));
                            }
                        }
                    }
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
                            if (jcrNode == null) jcrNode = node(node, (Type)null, null);
                            JcrValue[] defaultValues = defn.getDefaultValues();
                            if (defn.isMultiple()) {
                                jcrNode.setProperty(propName, defaultValues, defn.getRequiredType(), false);
                            } else {
                                // don't skip constraint checks or protected checks
                                jcrNode.setProperty(propName, defaultValues[0], false, false, false, false);
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
                        if (jcrNode == null) jcrNode = node(node, (Type)null, null);
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
                Set<Name> childrenNames = new HashSet<Name>();
                for (ChildReference childRef : node.getChildReferences(cache())) {
                    childrenNames.add(childRef.getName());
                }

                for (JcrNodeDefinition defn : mandatoryChildDefns) {
                    Name childName = defn.getInternalName();
                    if (!childrenNames.contains(childName)) {
                        throw new ConstraintViolationException(
                                                               JcrI18n.propertyNoLongerSatisfiesConstraints.text(childName,
                                                                                                                 readableLocation(node),
                                                                                                                 defn.getName(),
                                                                                                                 defn.getDeclaringNodeType()
                                                                                                                     .getName()));
                    }
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

        @Override
        public void processAfterLocking( MutableCachedNode modifiedNode,
                                         SaveContext context,
                                         NodeCache persistentNodeCache ) throws RepositoryException {
            // We actually can avoid this altogether if certain conditions are met ...
            final Name primaryType = modifiedNode.getPrimaryType(cache);
            final Set<Name> mixinTypes = modifiedNode.getMixinTypes(cache);
            if (!nodeTypeCapabilities.disallowsSameNameSiblings(primaryType, mixinTypes)) return;

            MutableCachedNode.NodeChanges changes = modifiedNode.getNodeChanges();
            Map<NodeKey, Name> appendedChildren = changes.appendedChildren();
            Map<NodeKey, Name> renamedChildren = changes.renamedChildren();
            Set<NodeKey> removedChildren = changes.removedChildren();
            if (!appendedChildren.isEmpty() || !renamedChildren.isEmpty()) {

                Multimap<Name, NodeKey> appendedOrRenamedChildrenByName = LinkedListMultimap.create();
                for (Map.Entry<NodeKey, Name> appended : appendedChildren.entrySet()) {
                    appendedOrRenamedChildrenByName.put(appended.getValue(), appended.getKey());
                }
                for (Map.Entry<NodeKey, Name> renamed : renamedChildren.entrySet()) {
                    appendedOrRenamedChildrenByName.put(renamed.getValue(), renamed.getKey());
                }

                assert appendedOrRenamedChildrenByName.isEmpty() == false;

                // look at the information that was already persisted to determine whether some other thread has already
                // created a child with the same name
                CachedNode persistentNode = persistentNodeCache.getNode(modifiedNode.getKey());

                // process appended/renamed children
                for (Name childName : appendedOrRenamedChildrenByName.keySet()) {
                    ChildReferences persistedChildReferences = persistentNode.getChildReferences(persistentNodeCache);
                    int existingChildrenWithSameName = persistedChildReferences.getChildCount(childName);
                    if (existingChildrenWithSameName == 0) {
                        continue;
                    }
                    if (existingChildrenWithSameName == 1) {
                        // See if the existing same-name sibling is removed ...
                        NodeKey persistedChildKey = persistedChildReferences.getChild(childName).getKey();
                        if (removedChildren.contains(persistedChildKey)) {
                            // the sole existing child with this name is being removed, so we can ignore it ...
                            // existingChildrenWithSameName = 0;
                            continue;
                        }
                    }

                    // There is at least one persisted child with the same name, and we're adding a new child
                    // or renaming an existing child to this name. Therefore, we have to find a child node definition
                    // that allows SNS. Look for one ignoring the child node type (this is faster than finding the
                    // child node primary types) ...
                    JcrNodeDefinition childNodeDefinition = nodeTypeCapabilities.findChildNodeDefinition(primaryType,
                                                                                                         mixinTypes,
                                                                                                         childName,
                                                                                                         null,
                                                                                                         existingChildrenWithSameName + 1,
                                                                                                         true);
                    if (childNodeDefinition != null) {
                        // found the one child node definition that applies, so it's okay ...
                        continue;
                    }

                    // We were NOT able to find a definition that allows SNS for this name, but we need to make sure that
                    // the node that already exists (persisted) isn't the one that's being changed
                    NodeKey persistedChildKey = persistedChildReferences.getChild(childName).getKey();
                    if (appendedChildren.containsKey(persistedChildKey) || renamedChildren.containsKey(persistedChildKey)) {
                        // The persisted node is being changed, so it's okay ...
                        continue;
                    }

                    // We still were NOT able to find a definition that allows SNS for this name WITHOUT considering the
                    // specific child node type. This likely means there is either 0 or more than 1 (possibly residual)
                    // child node definitions. We need to find all of the added/renamed child nodes and use their specific
                    // primary types. The first to fail will result in an exception ...
                    final SessionCache session = cache();
                    for (NodeKey appendedOrRenamedKey : appendedOrRenamedChildrenByName.get(childName)) {
                        MutableCachedNode appendedOrRenamedChild = session.mutable(appendedOrRenamedKey);
                        if (appendedOrRenamedChild == null) continue;
                        Name childPrimaryType = appendedOrRenamedChild.getPrimaryType(session);
                        childNodeDefinition = nodeTypeCapabilities.findChildNodeDefinition(primaryType, mixinTypes, childName,
                                                                                           childPrimaryType,
                                                                                           existingChildrenWithSameName + 1, true);
                        if (childNodeDefinition == null) {
                            // Could not find a valid child node definition that allows SNS given the child's primary type and
                            // name plus the parent's primary type and mixin types.
                            throw new ItemExistsException(JcrI18n.noSnsDefinitionForNode.text(childName, workspaceName()));
                        }
                    }
                }
            }
        }
    }

    protected class SystemSessionCache extends SessionCacheWrapper {
        protected SystemSessionCache( SessionCache delegate ) {
            super(delegate);
        }

        @Override
        public void save() {
            super.save();
            signalSaveOfSystemChanges();
        }

        @Override
        public void save( SessionCache otherSession,
                          PreSave preSaveOperation ) {
            super.save(otherSession, preSaveOperation);
            signalSaveOfSystemChanges();
        }

        @Override
        public void save( Set<NodeKey> toBeSaved,
                          SessionCache otherSession,
                          PreSave preSaveOperation ) {
            super.save(toBeSaved, otherSession, preSaveOperation);
            signalSaveOfSystemChanges();
        }

        /**
         * This method can be called by workspace-write methods, which (if a transaction has started after this session was
         * created) can persist changes (via their SessionCache.save())
         */
        private void signalSaveOfSystemChanges() {
            cache().checkForTransaction();
        }
    }
}
