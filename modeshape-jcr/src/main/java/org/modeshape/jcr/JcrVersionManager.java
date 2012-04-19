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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.MergeException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.OnParentVersionAction;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.AbstractJcrNode.Type;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.ChildReference;
import org.modeshape.jcr.cache.ChildReferences;
import org.modeshape.jcr.cache.MutableCachedNode;
import org.modeshape.jcr.cache.NodeCache;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.SessionCache;
import org.modeshape.jcr.value.DateTimeFactory;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NameFactory;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.PathFactory;
import org.modeshape.jcr.value.Property;
import org.modeshape.jcr.value.PropertyFactory;
import org.modeshape.jcr.value.Reference;
import org.modeshape.jcr.value.ReferenceFactory;

/**
 * Local implementation of version management code, comparable to an implementation of the JSR-283 {@code VersionManager}
 * interface. Valid instances of this class can be obtained by calling {@link JcrWorkspace#versionManager()}.
 */
final class JcrVersionManager implements VersionManager {

    /**
     * Property names from nt:frozenNode that should never be copied directly to a node when the frozen node is restored.
     */
    static final Set<Name> IGNORED_PROP_NAMES_FOR_RESTORE = Collections.unmodifiableSet(new HashSet<Name>(
                                                                                                          Arrays.asList(new Name[] {
                                                                                                              JcrLexicon.FROZEN_PRIMARY_TYPE,
                                                                                                              JcrLexicon.FROZEN_MIXIN_TYPES,
                                                                                                              JcrLexicon.FROZEN_UUID,
                                                                                                              JcrLexicon.PRIMARY_TYPE,
                                                                                                              JcrLexicon.MIXIN_TYPES,
                                                                                                              JcrLexicon.UUID})));

    private final JcrSession session;
    private final Path versionStoragePath;
    private final PathAlgorithm versionHistoryPathAlgorithm;
    private final SystemContent readableSystem;

    public JcrVersionManager( JcrSession session ) {
        super();
        this.session = session;
        versionStoragePath = absolutePath(JcrLexicon.SYSTEM, JcrLexicon.VERSION_STORAGE);
        ExecutionContext context = session.context();
        versionHistoryPathAlgorithm = new HiearchicalPathAlgorithm(versionStoragePath, context);
        readableSystem = new SystemContent(this.session.cache());
    }

    final ExecutionContext context() {
        return session.context();
    }

    final Name name( String s ) {
        return session().nameFactory().create(s);
    }

    final String string( Object propertyValue ) {
        return session.stringFactory().create(propertyValue);
    }

    final Name name( Object ob ) {
        return session.nameFactory().create(ob);
    }

    final Path path( Path root,
                     Name child ) {
        return session.pathFactory().create(root, child);
    }

    final Path path( Path root,
                     Path.Segment childSegment ) {
        return session.pathFactory().create(root, childSegment);
    }

    final Path absolutePath( Name... absolutePathSegments ) {
        return session.pathFactory().createAbsolutePath(absolutePathSegments);
    }

    final PropertyFactory propertyFactory() {
        return session.propertyFactory();
    }

    final SessionCache cache() {
        return session.cache();
    }

    final JcrRepository repository() {
        return session.repository();
    }

    final JcrSession session() {
        return session;
    }

    final JcrWorkspace workspace() {
        return session.workspace();
    }

    /**
     * Return the path to the nt:versionHistory node for the node with the supplied NodeKey.
     * <p>
     * This method uses one of two algorithms, both of which operate upon the {@link NodeKey#getIdentifierHash() SHA-1 hash of the
     * identifier part} of the versionable node's {@link NodeKey key}. In the following descriptions, "{sha1}" is hex string form
     * of the SHA-1 hash of the identifier part of the versionable node's key.
     * <ul>
     * <li>The flat algorithm just returns the path <code>/jcr:system/jcr:versionStorage/{sha1}</code>. For example, given a node
     * key with an identifier part of "fae2b929-c5ef-4ce5-9fa1-514779ca0ae3", the SHA-1 hash of the identifier is
     * "b46dde8905f76361779339fa3ccacc4f47664255", so the path to the node's version history would be
     * <code>/jcr:system/jcr:versionStorage/b46dde8905f76361779339fa3ccacc4f47664255</code>.</li>
     * <li>The hierarchical algorithm creates a hiearchical path based upon the first 6 characters of the "{sha1}" hash:
     * <code>/jcr:system/jcr:versionStorage/{part1}/{part2}/{part3}/{part4}</code>, where "{part1}" consists of the 1st and 2nd
     * hex characters of the "{sha1}" string, "{part2}" consists of the 3rd and 4th hex characters of the "{sha1}" string,
     * "{part3}" consists of the 5th and 6th characters of the "{sha1}" string, "{part4}" consists of the remaining characters.
     * For example, given a node key with an identifier part of "fae2b929-c5ef-4ce5-9fa1-514779ca0ae3", the SHA-1 hash of the
     * identifier is "b46dde8905f76361779339fa3ccacc4f47664255", so the path to the node's version history would be
     * <code>/jcr:system/jcr:versionStorage/b4/6d/de/298905f76361779339fa3ccacc4f47664255</code>.</li>
     * </ul>
     * </p>
     * 
     * @param key the key for the node for which the path to the version history should be returned
     * @return the path to the version history node that corresponds to the node with the given key. This does not guarantee that
     *         a node exists at the returned path. In fact, this method will return null for every node that is and has never been
     *         versionable, or every node that is versionable but not checked in.
     */
    Path versionHistoryPathFor( NodeKey key ) {
        return versionHistoryPathAlgorithm.versionHistoryPathFor(key.getIdentifierHash());
    }

    @Override
    public JcrVersionHistoryNode getVersionHistory( String absPath ) throws RepositoryException {
        return getVersionHistory(session.getNode(absPath));
    }

    JcrVersionHistoryNode getVersionHistory( AbstractJcrNode node ) throws RepositoryException {
        checkVersionable(node);

        // Try to look up the version history by its key ...
        NodeKey historyKey = readableSystem.versionHistoryNodeKeyFor(node.key());
        SessionCache cache = session.cache();
        CachedNode historyNode = cache.getNode(historyKey);
        if (historyNode != null) {
            return (JcrVersionHistoryNode)session.node(historyNode, Type.VERSION_HISTORY);
        }
        // Per Section 15.1:
        // "Under both simple and full versioning, on persist of a new versionable node N that neither corresponds
        // nor shares with an existing node:
        // - The jcr:isCheckedOut property of N is set to true and
        // - A new VersionHistory (H) is created for N. H contains one Version, the root version (V0)
        // (see §3.13.5.2 Root Version)."
        //
        // This means that the version history should not be created until save is performed. This makes sense,
        // because otherwise the version history would be persisted for a newly-created node, even though that node
        // is not yet persisted. Tests with the reference implementation (see sandbox) verified this behavior.
        //
        // If the node is new, then we'll throw an exception
        if (node.isNew()) {
            String msg = JcrI18n.noVersionHistoryForTransientVersionableNodes.text(node.location());
            throw new InvalidItemStateException(msg);
        }

        // Get the cached node and see if the 'mix:versionable' mixin was added transiently ...
        CachedNode cachedNode = node.node();
        if (cachedNode instanceof MutableCachedNode) {
            // There are at least some changes. See if the node is newly versionable ...
            MutableCachedNode mutable = (MutableCachedNode)cachedNode;
            RepositoryNodeTypeManager.NodeTypes nodeTypeCapabilities = repository().nodeTypeManager().getNodeTypes();
            Name primaryType = mutable.getPrimaryType(cache);
            Set<Name> mixinTypes = mutable.getAddedMixins(cache);
            if (nodeTypeCapabilities.isVersionable(primaryType, mixinTypes)) {
                // We don't create the verison history until the versionable state is persisted ...
                String msg = JcrI18n.versionHistoryForNewlyVersionableNodesNotAvailableUntilSave.text(node.location());
                throw new UnsupportedRepositoryOperationException(msg);
            }
        }

        // Otherwise the node IS versionable and we need to initialize the version history ...
        initializeVersionHistoryFor(node, historyKey, cache);
        // Look up the history node again, using this session ...
        historyNode = cache.getNode(historyKey);
        return (JcrVersionHistoryNode)session.node(historyNode, Type.VERSION_HISTORY);
    }

    private void initializeVersionHistoryFor( AbstractJcrNode node,
                                              NodeKey historyKey,
                                              SessionCache cache ) throws RepositoryException {
        SystemContent content = new SystemContent(session.createSystemCache(false));
        CachedNode cachedNode = node.node();
        Name primaryTypeName = cachedNode.getPrimaryType(cache);
        Set<Name> mixinTypeNames = cachedNode.getMixinTypes(cache);
        NodeKey versionedKey = cachedNode.getKey();
        Path versionHistoryPath = versionHistoryPathFor(versionedKey);
        DateTime now = session().dateFactory().create();

        content.initializeVersionStorage(versionedKey,
                                         historyKey,
                                         null,
                                         primaryTypeName,
                                         mixinTypeNames,
                                         versionHistoryPath,
                                         null,
                                         now);
        content.save();
    }

    /**
     * Throw an {@link UnsupportedRepositoryOperationException} if the node is not versionable (i.e.,
     * isNodeType(JcrMixLexicon.VERSIONABLE) == false).
     * 
     * @param node the node to check
     * @throws UnsupportedRepositoryOperationException if <code>!isNodeType({@link JcrMixLexicon#VERSIONABLE})</code>
     * @throws RepositoryException if an error occurs reading the node types for this node
     */
    private void checkVersionable( AbstractJcrNode node ) throws UnsupportedRepositoryOperationException, RepositoryException {
        if (!node.isNodeType(JcrMixLexicon.VERSIONABLE)) {
            throw new UnsupportedRepositoryOperationException(JcrI18n.requiresVersionable.text());
        }
    }

    @Override
    public Version getBaseVersion( String absPath ) throws UnsupportedRepositoryOperationException, RepositoryException {
        return session.getNode(absPath).getBaseVersion();
    }

    @Override
    public boolean isCheckedOut( String absPath ) throws RepositoryException {
        return session.getNode(absPath).isCheckedOut();
    }

    @Override
    public Version checkin( String absPath )
        throws VersionException, UnsupportedRepositoryOperationException, InvalidItemStateException, LockException,
        RepositoryException {
        return checkin(session.getNode(absPath));
    }

    /**
     * Checks in the given node, creating (and returning) a new {@link Version}.
     * 
     * @param node the node to be checked in
     * @return the {@link Version} object created as a result of this checkin
     * @throws RepositoryException if an error occurs during the checkin. See {@link javax.jcr.Node#checkin()} for a full
     *         description of the possible error conditions.
     * @see #checkin(String)
     * @see AbstractJcrNode#checkin()
     */
    JcrVersionNode checkin( AbstractJcrNode node ) throws RepositoryException {
        checkVersionable(node);

        if (node.isNew() || node.isModified()) {
            throw new InvalidItemStateException(JcrI18n.noPendingChangesAllowed.text());
        }

        // Check this separately since it throws a different type of exception
        if (node.isLocked() && !node.holdsLock()) {
            throw new LockException(JcrI18n.lockTokenNotHeld.text(node.getPath()));
        }

        if (node.getProperty(JcrLexicon.MERGE_FAILED) != null) {
            throw new VersionException(JcrI18n.pendingMergeConflicts.text(node.getPath()));
        }

        javax.jcr.Property isCheckedOut = node.getProperty(JcrLexicon.IS_CHECKED_OUT);
        if (!isCheckedOut.getBoolean()) {
            return node.getBaseVersion();
        }

        // Collect some of the information about the node that we'll need ...
        SessionCache cache = cache();
        NodeKey versionedKey = node.key();
        Path versionHistoryPath = versionHistoryPathFor(versionedKey);
        CachedNode cachedNode = node.node();
        DateTime now = session().dateFactory().create();

        // Create the system content that we'll use to update the system branch ...
        SessionCache systemSession = session.createSystemCache(false);
        SystemContent systemContent = new SystemContent(systemSession);

        MutableCachedNode version = null;
        try {
            // Create a new version in the history for this node; this initializes the version history if it is missing ...
            List<Property> versionableProps = new ArrayList<Property>();
            addVersionedPropertiesFor(node, false, versionableProps);

            AtomicReference<MutableCachedNode> frozen = new AtomicReference<MutableCachedNode>();
            version = systemContent.recordNewVersion(cachedNode, cache, versionHistoryPath, null, versionableProps, now, frozen);
            NodeKey historyKey = version.getParentKey(systemSession);

            // Update the node's 'mix:versionable' properties, using a new session ...
            SessionCache versionSession = session.spawnSessionCache(false);
            MutableCachedNode versionableNode = versionSession.mutable(versionedKey);
            PropertyFactory props = propertyFactory();
            ReferenceFactory refFactory = session.referenceFactory();
            Reference historyRef = refFactory.create(historyKey);
            Reference baseVersionRef = refFactory.create(version.getKey());
            versionableNode.setProperty(versionSession, props.create(JcrLexicon.VERSION_HISTORY, historyRef));
            versionableNode.setProperty(versionSession, props.create(JcrLexicon.BASE_VERSION, baseVersionRef));
            versionableNode.setProperty(versionSession, props.create(JcrLexicon.IS_CHECKED_OUT, Boolean.FALSE));
            // The 'jcr:predecessors' set to an empty array, per Section 15.2 in JSR-283
            versionableNode.setProperty(versionSession, props.create(JcrLexicon.PREDECESSORS, new Object[] {}));

            // Now process the children of the versionable node, and add them under the frozen node ...
            MutableCachedNode frozenNode = frozen.get();
            for (ChildReference childRef : cachedNode.getChildReferences(versionSession)) {
                AbstractJcrNode child = session.node(childRef.getKey(), null);
                versionNodeAt(child, frozenNode, false, versionSession, systemSession);
            }

            // Now save all of the changes ...
            versionSession.save(systemSession, null);
        } finally {
            // TODO: Versioning: may want to catch this block and retry, if the new version name couldn't be created
        }

        return (JcrVersionNode)session.node(version, Type.VERSION);
    }

    /**
     * Create a version record for the given node under the given parent path with the given batch.
     * 
     * @param node the node for which the frozen version record should be created
     * @param parentInVersionHistory the node in the version history under which the frozen version should be recorded
     * @param forceCopy true if the OPV should be ignored and a COPY is to be performed, or false if the OPV should be used
     * @param nodeCache the session cache used to access the node information; may not be null
     * @param versionHistoryCache the session cache used to create nodes in the version history; may not be null
     * @throws RepositoryException if an error occurs accessing the repository
     */
    @SuppressWarnings( "fallthrough" )
    private void versionNodeAt( AbstractJcrNode node,
                                MutableCachedNode parentInVersionHistory,
                                boolean forceCopy,
                                SessionCache nodeCache,
                                SessionCache versionHistoryCache ) throws RepositoryException {
        int onParentVersion = 0;
        if (forceCopy) {
            onParentVersion = OnParentVersionAction.COPY;
        } else {
            onParentVersion = node.getDefinition().getOnParentVersion();
        }

        NodeKey key = parentInVersionHistory.getKey().withRandomId();

        switch (onParentVersion) {
            case OnParentVersionAction.ABORT:
                throw new VersionException(JcrI18n.cannotCheckinNodeWithAbortChildNode.text(node.getName(), node.getParent()
                                                                                                                .getName()));
            case OnParentVersionAction.VERSION:
                if (node.isNodeType(JcrMixLexicon.VERSIONABLE)) {
                    // The frozen node should reference the version history of the node ...
                    JcrVersionHistoryNode history = node.getVersionHistory();
                    org.modeshape.jcr.value.Property primaryType = propertyFactory().create(JcrLexicon.PRIMARY_TYPE,
                                                                                            JcrNtLexicon.VERSIONED_CHILD);
                    org.modeshape.jcr.value.Property childVersionHistory = propertyFactory().create(JcrLexicon.CHILD_VERSION_HISTORY,
                                                                                                    history.key().toString());
                    parentInVersionHistory.createChild(versionHistoryCache, key, node.name(), primaryType, childVersionHistory);
                    return;
                }

                // Otherwise, treat it as a copy, as per section 3.13.9 bullet item 5 in JSR-283, so DO NOT break ...
            case OnParentVersionAction.COPY:
                // Per section 3.13.9 item 5 in JSR-283, an OPV of COPY or VERSION (iff not versionable)
                // results in COPY behavior "regardless of the OPV values of the sub-items".
                // We can achieve this by making the onParentVersionAction always COPY for the
                // recursive call ...
                forceCopy = true;

                // But the copy needs to be a 'nt:frozenNode', so that it doesn't compete with the actual node
                // (outside of version history) ...
                Name primaryTypeName = node.getPrimaryTypeName();
                Set<Name> mixinTypeNames = node.getMixinTypeNames();
                PropertyFactory factory = propertyFactory();
                List<Property> props = new LinkedList<Property>();
                props.add(factory.create(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.FROZEN_NODE));
                props.add(factory.create(JcrLexicon.FROZEN_PRIMARY_TYPE, primaryTypeName));
                props.add(factory.create(JcrLexicon.FROZEN_MIXIN_TYPES, mixinTypeNames));
                props.add(factory.create(JcrLexicon.FROZEN_UUID, node.getIdentifier()));
                addVersionedPropertiesFor(node, forceCopy, props);
                MutableCachedNode newCopy = parentInVersionHistory.createChild(versionHistoryCache, key, node.name(), props);

                // Now process the children of the versionable node ...
                for (ChildReference childRef : node.node().getChildReferences(nodeCache)) {
                    AbstractJcrNode child = session.node(childRef.getKey(), null);
                    versionNodeAt(child, newCopy, forceCopy, nodeCache, versionHistoryCache);
                }
                return;
            case OnParentVersionAction.INITIALIZE:
            case OnParentVersionAction.COMPUTE:
            case OnParentVersionAction.IGNORE:
                // Do nothing for these. No built-in types require initialize or compute for child nodes.
                return;
            default:
                throw new IllegalStateException("Unexpected value: " + onParentVersion);
        }
    }

    /**
     * @param node the node for which the properties should be versioned
     * @param forceCopy true if all of the properties should be copied, regardless of the property's OPV setting
     * @param props the collection in which should be added the versioned properties for {@code node} (i.e., the properties to add
     *        the the frozen version of {@code node})
     * @throws RepositoryException if an error occurs accessing the repository
     */
    private void addVersionedPropertiesFor( AbstractJcrNode node,
                                            boolean forceCopy,
                                            List<Property> props ) throws RepositoryException {

        for (PropertyIterator iter = node.getProperties(); iter.hasNext();) {
            AbstractJcrProperty property = (AbstractJcrProperty)iter.nextProperty();

            // We want to skip the actual primary type, mixin types, and uuid since those are handled above ...
            Name name = property.name();
            if (JcrLexicon.PRIMARY_TYPE.equals(name)) continue;
            if (JcrLexicon.MIXIN_TYPES.equals(name)) continue;
            if (JcrLexicon.UUID.equals(name)) continue;

            Property prop = property.property();
            if (forceCopy) {
                props.add(prop);
            } else {
                JcrPropertyDefinition propDefn = property.propertyDefinition();

                switch (propDefn.getOnParentVersion()) {
                    case OnParentVersionAction.ABORT:
                        I18n msg = JcrI18n.cannotCheckinNodeWithAbortProperty;
                        throw new VersionException(msg.text(property.getName(), node.getName()));
                    case OnParentVersionAction.COPY:
                    case OnParentVersionAction.VERSION:
                        props.add(prop);
                        break;
                    case OnParentVersionAction.INITIALIZE:
                    case OnParentVersionAction.COMPUTE:
                    case OnParentVersionAction.IGNORE:
                        // Do nothing for these
                }
            }
        }
    }

    @Override
    public void checkout( String absPath ) throws LockException, RepositoryException {
        checkout(session.getNode(absPath));
    }

    /**
     * Checks out the given node, updating version-related properties on the node as needed.
     * 
     * @param node the node to be checked out
     * @throws LockException if a lock prevents the node from being checked out
     * @throws RepositoryException if an error occurs during the checkout. See {@link javax.jcr.Node#checkout()} for a full
     *         description of the possible error conditions.
     */
    void checkout( AbstractJcrNode node ) throws LockException, RepositoryException {
        checkVersionable(node);

        // Check this separately since it throws a different type of exception
        if (node.isLocked() && !node.holdsLock()) {
            throw new LockException(JcrI18n.lockTokenNotHeld.text(node.getPath()));
        }

        if (!node.hasProperty(JcrLexicon.BASE_VERSION)) {
            // This happens when we've added mix:versionable, but not saved it to create the base
            // version (and the rest of the version storage graph). See MODE-704.
            return;
        }

        // Checking out an already checked-out node is supposed to return silently
        if (node.getProperty(JcrLexicon.IS_CHECKED_OUT).getBoolean()) {
            return;
        }

        // Create a session that we'll used to change the node ...
        SessionCache versionSession = session.spawnSessionCache(false);
        MutableCachedNode versionable = versionSession.mutable(node.key());
        NodeKey baseVersionKey = node.getBaseVersion().key();
        PropertyFactory props = propertyFactory();
        versionable.setProperty(versionSession, props.create(JcrLexicon.PREDECESSORS, new String[]{baseVersionKey.toString()}));
        versionable.setProperty(versionSession, props.create(JcrLexicon.IS_CHECKED_OUT, Boolean.TRUE));
        versionSession.save();
    }

    @Override
    public Version checkpoint( String absPath )
        throws VersionException, UnsupportedRepositoryOperationException, InvalidItemStateException, LockException,
        RepositoryException {
        Version version = checkin(absPath);
        checkout(absPath);
        return version;
    }

    protected static interface PathAlgorithm {
        Path versionHistoryPathFor( String sha1OrUuid );
    }

    protected static abstract class BasePathAlgorithm implements PathAlgorithm {
        protected final PathFactory paths;
        protected final NameFactory names;
        protected final Path versionStoragePath;

        protected BasePathAlgorithm( Path versionStoragePath,
                                     ExecutionContext context ) {
            this.paths = context.getValueFactories().getPathFactory();
            this.names = context.getValueFactories().getNameFactory();
            this.versionStoragePath = versionStoragePath;
        }
    }

    protected static class HiearchicalPathAlgorithm extends BasePathAlgorithm {
        protected HiearchicalPathAlgorithm( Path versionStoragePath,
                                            ExecutionContext context ) {
            super(versionStoragePath, context);
        }

        @Override
        public Path versionHistoryPathFor( String sha1OrUuid ) {
            Name p1 = names.create(sha1OrUuid.substring(0, 2));
            Name p2 = names.create(sha1OrUuid.substring(2, 4));
            Name p3 = names.create(sha1OrUuid.substring(4, 6));
            Name p4 = names.create(sha1OrUuid);
            return paths.createAbsolutePath(JcrLexicon.SYSTEM, JcrLexicon.VERSION_STORAGE, p1, p2, p3, p4);
        }
    }

    protected static class FlatPathAlgorithm extends BasePathAlgorithm {
        protected FlatPathAlgorithm( Path versionStoragePath,
                                     ExecutionContext context ) {
            super(versionStoragePath, context);
        }

        @Override
        public Path versionHistoryPathFor( String sha1OrUuid ) {
            return paths.createAbsolutePath(JcrLexicon.SYSTEM, JcrLexicon.VERSION_STORAGE, names.create(sha1OrUuid));
        }
    }

    @Override
    public void restore( Version[] versions,
                         boolean removeExisting )
        throws ItemExistsException, UnsupportedRepositoryOperationException, VersionException, LockException,
        InvalidItemStateException, RepositoryException {
        validateSessionLiveWithoutPendingChanges();

        // Create a new session in which we'll perform the restore, so this session remains thread-safe ...
        JcrSession restoreSession = session.spawnSession(false);

        Map<JcrVersionNode, AbstractJcrNode> existingVersions = new HashMap<JcrVersionNode, AbstractJcrNode>(versions.length);
        Set<Path> versionRootPaths = new HashSet<Path>(versions.length);
        List<Version> nonExistingVersions = new ArrayList<Version>(versions.length);

        for (int i = 0; i < versions.length; i++) {
            VersionHistory history = versions[i].getContainingHistory();

            if (history.getRootVersion().isSame(versions[i])) {
                throw new VersionException(JcrI18n.cannotRestoreRootVersion.text(versions[i].getPath()));
            }

            try {
                AbstractJcrNode existingNode = restoreSession.getNodeByIdentifier(history.getVersionableIdentifier());
                existingVersions.put((JcrVersionNode)versions[i], existingNode);
                versionRootPaths.add(existingNode.path());
            } catch (ItemNotFoundException infe) {
                nonExistingVersions.add(versions[i]);
            }
        }

        if (existingVersions.isEmpty()) {
            throw new VersionException(JcrI18n.noExistingVersionForRestore.text());
        }

        // Now create and run the restore operation ...
        RestoreCommand op = new RestoreCommand(restoreSession, existingVersions, versionRootPaths, nonExistingVersions, null,
                                               removeExisting);
        op.execute();
        restoreSession.save();
    }

    @Override
    public void restore( String absPath,
                         String versionName,
                         boolean removeExisting )
        throws VersionException, ItemExistsException, UnsupportedRepositoryOperationException, LockException,
        InvalidItemStateException, RepositoryException {
        validateSessionLiveWithoutPendingChanges();

        // Create a new session in which we'll finish the restore, so this session remains thread-safe ...
        JcrSession restoreSession = session.spawnSession(false);

        Version version = null;

        // See if the node at absPath exists and has version storage.
        Path path = restoreSession.absolutePathFor(absPath);
        AbstractJcrNode existingNode = restoreSession.node(path);
        VersionHistory historyNode = existingNode.getVersionHistory();
        version = historyNode.getVersion(versionName);
        assert version != null;

        restore(restoreSession, path, version, null, removeExisting);
    }

    private void validateSessionLiveWithoutPendingChanges() throws RepositoryException {
        session.checkLive();
        if (session.hasPendingChanges()) {
            throw new InvalidItemStateException(JcrI18n.noPendingChangesAllowed.text());
        }
    }

    @Override
    public void restore( Version version,
                         boolean removeExisting )
        throws VersionException, ItemExistsException, InvalidItemStateException, UnsupportedRepositoryOperationException,
        LockException, RepositoryException {
        validateSessionLiveWithoutPendingChanges();
        // Create a new session in which we'll finish the restore, so this session remains thread-safe ...
        JcrSession restoreSession = session.spawnSession(false);
        AbstractJcrNode node = restoreSession.getNodeByIdentifier(version.getContainingHistory().getVersionableIdentifier());
        Path path = node.path();
        restore(restoreSession, path, version, null, removeExisting);
    }

    @Override
    public void restore( String absPath,
                         Version version,
                         boolean removeExisting )
        throws PathNotFoundException, ItemExistsException, VersionException, ConstraintViolationException,
        UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        validateSessionLiveWithoutPendingChanges();
        // Create a new session in which we'll finish the restore, so this session remains thread-safe ...
        JcrSession restoreSession = session.spawnSession(false);
        Path path = restoreSession.absolutePathFor(absPath);
        restore(restoreSession, path, version, null, removeExisting);
    }

    @Override
    public void restoreByLabel( String absPath,
                                String versionLabel,
                                boolean removeExisting )
        throws VersionException, ItemExistsException, UnsupportedRepositoryOperationException, LockException,
        InvalidItemStateException, RepositoryException {
        validateSessionLiveWithoutPendingChanges();
        // Create a new session in which we'll finish the restore, so this session remains thread-safe ...
        JcrSession restoreSession = session.spawnSession(false);
        restoreSession.getNode(absPath).restoreByLabel(versionLabel, removeExisting);
    }

    @Override
    public NodeIterator merge( String absPath,
                               String srcWorkspace,
                               boolean bestEffort )
        throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException,
        RepositoryException {
        return merge(absPath, srcWorkspace, bestEffort, false);
    }

    @Override
    public NodeIterator merge( String absPath,
                               String srcWorkspace,
                               boolean bestEffort,
                               boolean isShallow )
        throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException,
        RepositoryException {
        CheckArg.isNotNull(srcWorkspace, "source workspace name");
        // Create a new session in which we'll finish the merge, so this session remains thread-safe ...
        JcrSession mergeSession = session.spawnSession(false);
        AbstractJcrNode node = mergeSession.getNode(absPath);
        return merge(node, srcWorkspace, bestEffort, isShallow);
    }

    @Override
    public void doneMerge( String absPath,
                           Version version )
        throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
        // Create a new session in which we'll finish the merge, so this session remains thread-safe ...
        JcrSession mergeSession = session.spawnSession(false);
        doneMerge(mergeSession.getNode(absPath), version);
    }

    @Override
    public void cancelMerge( String absPath,
                             Version version )
        throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
        // Create a new session in which we'll perform the cancel, so this session remains thread-safe ...
        JcrSession cancelSession = session.spawnSession(false);
        cancelMerge(cancelSession.getNode(absPath), version);
    }

    /**
     * Restores the given version to the given path.
     * 
     * @param session the session that should be used; may not be null
     * @param path the path at which the version should be restored; may not be null
     * @param version the version to restore; may not be null
     * @param labelToRestore the label that was used to identify the version; may be null
     * @param removeExisting if UUID conflicts resulting from this restore should cause the conflicting node to be removed or an
     *        exception to be thrown and the operation to fail
     * @throws RepositoryException if an error occurs accessing the repository
     * @see javax.jcr.Node#restore(Version, String, boolean)
     * @see javax.jcr.Node#restoreByLabel(String, boolean)
     */
    void restore( JcrSession session,
                  Path path,
                  Version version,
                  String labelToRestore,
                  boolean removeExisting ) throws RepositoryException {

        // Ensure that the parent node exists - this will throw a PNFE if no node exists at that path
        AbstractJcrNode parentNode = session.node(path.getParent());
        AbstractJcrNode existingNode = null;
        AbstractJcrNode nodeToCheckLock;

        JcrVersionNode jcrVersion = (JcrVersionNode)version;
        SessionCache cache = session.cache();
        PropertyFactory propFactory = session.propertyFactory();

        try {
            existingNode = parentNode.childNode(path.getLastSegment(), null);
            nodeToCheckLock = existingNode;

            // These checks only make sense if there is an existing node
            JcrVersionHistoryNode versionHistory = existingNode.getVersionHistory();
            if (!versionHistory.isSame(jcrVersion.getParent())) {
                throw new VersionException(JcrI18n.invalidVersion.text(version.getPath(), versionHistory.getPath()));
            }

            if (!versionHistory.isSame(existingNode.getVersionHistory())) {
                throw new VersionException(JcrI18n.invalidVersion.text(version.getPath(), existingNode.getVersionHistory()
                                                                                                      .getPath()));
            }

            if (jcrVersion.isSame(versionHistory.getRootVersion())) {
                throw new VersionException(JcrI18n.cannotRestoreRootVersion.text(existingNode.getPath()));
            }

        } catch (PathNotFoundException pnfe) {
            // This is allowable, but the node needs to be checked out
            if (!parentNode.isCheckedOut()) {
                String parentPath = path.getString(session.context().getNamespaceRegistry());
                throw new VersionException(JcrI18n.nodeIsCheckedIn.text(parentPath));
            }

            AbstractJcrNode sourceNode = session.workspace().getVersionManager().frozenNodeFor(version);
            Name primaryTypeName = session.nameFactory().create(sourceNode.getProperty(JcrLexicon.FROZEN_PRIMARY_TYPE)
                                                                          .property()
                                                                          .getFirstValue());
            AbstractJcrProperty uuidProp = sourceNode.getProperty(JcrLexicon.FROZEN_UUID);
            NodeKey desiredKey = new NodeKey(session.stringFactory().create(uuidProp.property().getFirstValue()));

            Property primaryType = propFactory.create(JcrLexicon.PRIMARY_TYPE, primaryTypeName);
            MutableCachedNode newChild = parentNode.mutable().createChild(cache,
                                                                          desiredKey,
                                                                          path.getLastSegment().getName(),
                                                                          primaryType);
            existingNode = session.node(newChild, (Type)null);
            nodeToCheckLock = parentNode;
        }

        // Check whether the node to check is locked
        if (nodeToCheckLock.isLocked() && !nodeToCheckLock.holdsLock()) {
            throw new LockException(JcrI18n.lockTokenNotHeld.text(nodeToCheckLock.getPath()));
        }

        RestoreCommand op = new RestoreCommand(session, Collections.singletonMap(jcrVersion, existingNode),
                                               Collections.singleton(existingNode.path()), Collections.<Version>emptySet(),
                                               labelToRestore, removeExisting);
        op.execute();

        clearCheckoutStatus(existingNode.mutable(), jcrVersion.key(), cache, propertyFactory());
        ReferenceFactory refFactory = session.referenceFactory();
        Reference baseVersionRef = refFactory.create(jcrVersion.key());

        MutableCachedNode mutable = existingNode.mutable();
        mutable.setProperty(cache, propFactory.create(JcrLexicon.IS_CHECKED_OUT, Boolean.FALSE));
        mutable.setProperty(cache, propFactory.create(JcrLexicon.BASE_VERSION, baseVersionRef));

        session.save();
    }

    private void clearCheckoutStatus( MutableCachedNode node,
                                      NodeKey baseVersion,
                                      SessionCache cache,
                                      PropertyFactory propFactory ) {
        Reference baseVersionRef = session.referenceFactory().create(baseVersion);
        node.setProperty(cache, propFactory.create(JcrLexicon.IS_CHECKED_OUT, Boolean.FALSE));
        node.setProperty(cache, propFactory.create(JcrLexicon.BASE_VERSION, baseVersionRef));
    }

    /**
     * @param version the version for which the frozen node should be returned
     * @return the frozen node for the given version
     * @throws RepositoryException if an error occurs accessing the repository
     */
    AbstractJcrNode frozenNodeFor( Version version ) throws RepositoryException {
        return ((AbstractJcrNode)version).getNode(JcrLexicon.FROZEN_NODE);
    }

    NodeIterator merge( AbstractJcrNode targetNode,
                        String srcWorkspace,
                        boolean bestEffort,
                        boolean isShallow ) throws RepositoryException {
        targetNode.session().checkLive();

        if (session().hasPendingChanges()) {
            throw new InvalidItemStateException(JcrI18n.noPendingChangesAllowed.text());
        }

        try {
            targetNode.correspondingNodePath(srcWorkspace);
        } catch (ItemNotFoundException infe) {
            // return immediately if no corresponding node exists in that workspace
            return JcrEmptyNodeIterator.INSTANCE;
        }

        JcrSession sourceSession = targetNode.session().spawnSession(srcWorkspace, true);
        MergeCommand op = new MergeCommand(targetNode, sourceSession, bestEffort, isShallow);
        op.execute();

        session.save();

        return op.getFailures();
    }

    void doneMerge( AbstractJcrNode targetNode,
                    Version version ) throws RepositoryException {
        targetNode.session().checkLive();
        checkVersionable(targetNode);

        if (targetNode.isNew() || targetNode.isModified()) {
            throw new InvalidItemStateException(JcrI18n.noPendingChangesAllowedForNode.text());
        }

        if (!targetNode.isNodeType(JcrMixLexicon.VERSIONABLE)) {
            throw new VersionException(JcrI18n.requiresVersionable.text());
        }

        AbstractJcrProperty prop = targetNode.getProperty(JcrLexicon.PREDECESSORS);

        JcrValue[] values = prop.getValues();
        JcrValue[] newValues = new JcrValue[values.length + 1];
        System.arraycopy(values, 0, newValues, 0, values.length);
        newValues[values.length] = targetNode.valueFrom(version);

        targetNode.setProperty(JcrLexicon.PREDECESSORS, newValues, PropertyType.REFERENCE, false);
        removeVersionFromMergeFailedProperty(targetNode, version);

        targetNode.session().save();
    }

    void cancelMerge( AbstractJcrNode targetNode,
                      Version version ) throws RepositoryException {
        targetNode.session().checkLive();
        checkVersionable(targetNode);

        if (targetNode.isNew() || targetNode.isModified()) {
            throw new InvalidItemStateException(JcrI18n.noPendingChangesAllowedForNode.text());
        }

        if (!targetNode.isNodeType(JcrMixLexicon.VERSIONABLE)) {
            throw new UnsupportedRepositoryOperationException(JcrI18n.requiresVersionable.text());
        }

        removeVersionFromMergeFailedProperty(targetNode, version);
        targetNode.session().save();
    }

    @SuppressWarnings( "deprecation" )
    private void removeVersionFromMergeFailedProperty( AbstractJcrNode targetNode,
                                                       Version version ) throws RepositoryException {

        if (!targetNode.hasProperty(JcrLexicon.MERGE_FAILED)) {
            throw new VersionException(JcrI18n.versionNotInMergeFailed.text(version.getName(), targetNode.getPath()));
        }

        AbstractJcrProperty prop = targetNode.getProperty(JcrLexicon.MERGE_FAILED);
        Value[] values = prop.getValues();

        String uuidString = version.getUUID();
        int matchIndex = -1;
        for (int i = 0; i < values.length; i++) {
            if (uuidString.equals(values[i].getString())) {
                matchIndex = i;
                break;
            }
        }

        if (matchIndex == -1) {
            throw new VersionException(JcrI18n.versionNotInMergeFailed.text(version.getName(), targetNode.getPath()));
        }

        if (values.length == 1) {
            prop.remove();
        } else {
            Value[] newValues = new JcrValue[values.length - 2];

            if (matchIndex == 0) {
                System.arraycopy(values, 1, newValues, 0, values.length - 1);
            } else if (matchIndex == values.length - 1) {
                System.arraycopy(values, 0, newValues, 0, values.length - 2);
            } else {
                System.arraycopy(values, 0, newValues, 0, matchIndex);
                System.arraycopy(values, matchIndex + 1, newValues, matchIndex, values.length - matchIndex - 1);
            }

            prop.setValue(newValues);
        }

    }

    @Override
    public Node createConfiguration( String absPath ) throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    @Override
    public Node setActivity( Node activity ) throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    @Override
    public Node getActivity() throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    @Override
    public Node createActivity( String title ) throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    @Override
    public void removeActivity( Node activityNode )
        throws UnsupportedRepositoryOperationException, /*VersionException,*/RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    @Override
    public NodeIterator merge( Node activityNode ) throws /*VersionException, AccessDeniedException, MergeException, LockException, InvalidItemStateException,*/
    RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    @NotThreadSafe
    private class RestoreCommand {

        private final JcrSession session;
        private final SessionCache cache;
        private final PropertyFactory propFactory;
        private Map<JcrVersionNode, AbstractJcrNode> existingVersions;
        private Set<Path> versionRootPaths;
        private Collection<Version> nonExistingVersions;
        private boolean removeExisting;
        private String labelToRestore;
        private Map<AbstractJcrNode, AbstractJcrNode> changedNodes;

        public RestoreCommand( JcrSession session,
                               Map<JcrVersionNode, AbstractJcrNode> existingVersions,
                               Set<Path> versionRootPaths,
                               Collection<Version> nonExistingVersions,
                               String labelToRestore,
                               boolean removeExisting ) {
            this.session = session;
            this.cache = session.cache();
            this.propFactory = session.propertyFactory();
            this.existingVersions = existingVersions;
            this.versionRootPaths = versionRootPaths;
            this.nonExistingVersions = nonExistingVersions;
            this.removeExisting = removeExisting;
            this.labelToRestore = labelToRestore;

            // The default size for a HashMap is pretty low and this could get big fast
            this.changedNodes = new HashMap<AbstractJcrNode, AbstractJcrNode>(100);
        }

        final String string( Object value ) {
            return session.stringFactory().create(value);
        }

        final Name name( Object value ) {
            return session.nameFactory().create(value);
        }

        final DateTime date( Calendar value ) {
            return session.dateFactory().create(value);
        }

        void execute() throws RepositoryException {
            Collection<JcrVersionNode> versionsToCheck = new ArrayList<JcrVersionNode>(existingVersions.keySet());
            JcrVersionManager versionManager = session.workspace().getVersionManager();
            for (JcrVersionNode version : versionsToCheck) {
                AbstractJcrNode root = existingVersions.get(version);
                // This can happen if the version was already restored in another node
                if (root == null) continue;

                // This updates the changedNodes and nonExistingVersions fields as a side effect
                AbstractJcrNode frozenNode = versionManager.frozenNodeFor(version);
                MutableCachedNode mutableRoot = root.mutable();
                restoreNodeMixins(frozenNode.node(), mutableRoot, cache);
                restoreNode(frozenNode, root, date(version.getCreated()));
                clearCheckoutStatus(mutableRoot, version.key(), cache, propFactory);
            }

            if (!nonExistingVersions.isEmpty()) {
                StringBuilder versions = new StringBuilder();
                boolean first = true;
                for (Version version : nonExistingVersions) {
                    if (!first) {
                        versions.append(", ");
                    } else {
                        first = false;
                    }
                    versions.append(version.getName());
                }
                throw new VersionException(JcrI18n.unrootedVersionsInRestore.text(versions.toString()));
            }

            for (Map.Entry<AbstractJcrNode, AbstractJcrNode> changedNode : changedNodes.entrySet()) {
                restoreProperties(changedNode.getKey(), changedNode.getValue());
            }
        }

        /**
         * Restores the child nodes and mixin types for {@code targetNode} based on the frozen version stored at
         * {@code sourceNode}. This method will remove and add child nodes as necessary based on the documentation in the JCR 2.0
         * specification (sections 15.7), but this method will not modify properties (other than jcr:mixinTypes, jcr:baseVersion,
         * and jcr:isCheckedOut).
         * 
         * @param sourceNode a node in the subgraph of frozen nodes under a version; may not be null, but may be a node with
         *        primary type of nt:version or nt:versionedChild
         * @param targetNode the node to be updated based on {@code sourceNode}; may not be null
         * @param checkinTime the time at which the version that instigated this restore was checked in; may not be null
         * @throws RepositoryException if an error occurs accessing the repository
         */
        private void restoreNode( AbstractJcrNode sourceNode,
                                  AbstractJcrNode targetNode,
                                  DateTime checkinTime ) throws RepositoryException {
            changedNodes.put(sourceNode, targetNode);

            MutableCachedNode target = targetNode.mutable();
            CachedNode source = sourceNode.node();

            Set<CachedNode> versionedChildrenThatShouldNotBeRestored = new HashSet<CachedNode>();

            // Try to match the existing nodes with nodes from the version to be restored
            Map<NodeKey, CachedNode> presentInBoth = new HashMap<NodeKey, CachedNode>();

            // Start with all target children in this set and pull them out as matches are found
            List<NodeKey> inTargetOnly = asList(target.getChildReferences(cache));

            // Start with no source children in this set, but add them in when no match is found
            Map<CachedNode, CachedNode> inSourceOnly = new HashMap<CachedNode, CachedNode>();

            // Map the source children to existing target children where possible
            for (ChildReference sourceChild : source.getChildReferences(cache)) {
                CachedNode child = cache.getNode(sourceChild);
                boolean isVersionedChild = JcrNtLexicon.VERSIONED_CHILD.equals(name(child.getPrimaryType(cache)));
                CachedNode resolvedNode = resolveSourceNode(child, checkinTime, cache);
                CachedNode match = findMatchFor(resolvedNode, cache);

                if (match != null) {
                    if (isVersionedChild) {
                        if (!removeExisting) {
                            throw new ItemExistsException(JcrI18n.itemAlreadyExistsWithUuid.text(match.getKey(),
                                                                                                 session.workspace().getName(),
                                                                                                 match.getPath(cache)));
                        }
                        // use match directly
                        versionedChildrenThatShouldNotBeRestored.add(match);
                    }
                    inTargetOnly.remove(match.getKey());
                    presentInBoth.put(child.getKey(), match);

                } else {
                    inSourceOnly.put(child, resolvedNode);
                }
            }

            // Remove all the extraneous children of the target node
            for (NodeKey childKey : inTargetOnly) {
                AbstractJcrNode child = session.node(childKey, null);
                switch (child.getDefinition().getOnParentVersion()) {
                    case OnParentVersionAction.ABORT:
                    case OnParentVersionAction.VERSION:
                    case OnParentVersionAction.COPY:
                        target.removeChild(cache, childKey);
                        cache.destroy(childKey);
                        // Otherwise we're going to reuse the exisiting node
                        break;
                    case OnParentVersionAction.COMPUTE:
                        // Technically, this should reinitialize the node per its defaults.
                    case OnParentVersionAction.INITIALIZE:
                    case OnParentVersionAction.IGNORE:
                        // Do nothing
                }
            }

            LinkedList<ChildReference> reversedChildren = new LinkedList<ChildReference>();
            for (ChildReference sourceChildRef : source.getChildReferences(cache)) {
                reversedChildren.addFirst(sourceChildRef);
            }

            // Now walk through the source node children (in reversed order), inserting children as needed
            // The order is reversed because SessionCache$NodeEditor supports orderBefore, but not orderAfter
            NodeKey prevChildKey = null;
            for (ChildReference sourceChildRef : reversedChildren) {
                CachedNode sourceChild = cache.getNode(sourceChildRef);
                CachedNode targetChild = presentInBoth.get(sourceChildRef.getKey());
                CachedNode resolvedChild = null;
                Name resolvedPrimaryTypeName = null;

                AbstractJcrNode sourceChildNode;
                AbstractJcrNode targetChildNode;

                boolean shouldRestore = !versionedChildrenThatShouldNotBeRestored.contains(targetChild);
                boolean shouldRestoreMixinsAndUuid = false;

                if (targetChild != null) {
                    // Reorder if necessary
                    resolvedChild = resolveSourceNode(sourceChild, checkinTime, cache);
                    resolvedPrimaryTypeName = name(resolvedChild.getPrimaryType(cache));
                    sourceChildNode = session.node(resolvedChild, (Type)null);
                    targetChildNode = session.node(targetChild, (Type)null);

                } else {
                    // Pull the resolved node
                    resolvedChild = inSourceOnly.get(sourceChild);
                    resolvedPrimaryTypeName = name(resolvedChild.getPrimaryType(cache));

                    sourceChildNode = session.node(resolvedChild, (Type)null);
                    shouldRestoreMixinsAndUuid = true;

                    Name primaryTypeName = null;
                    NodeKey desiredKey = null;
                    Name desiredName = null;
                    if (JcrNtLexicon.FROZEN_NODE.equals(resolvedPrimaryTypeName)) {
                        primaryTypeName = name(resolvedChild.getProperty(JcrLexicon.FROZEN_PRIMARY_TYPE, cache).getFirstValue());
                        Property idProp = resolvedChild.getProperty(JcrLexicon.FROZEN_UUID, cache);
                        String frozenUuid = string(idProp.getFirstValue());
                        desiredKey = NodeKey.isValidFormat(frozenUuid) ? new NodeKey(frozenUuid) : target.getKey().withId(frozenUuid);
                        //the name should be that of the versioned child
                        desiredName = session.node(sourceChild, (Type) null).name();
                    } else {
                        primaryTypeName = resolvedChild.getPrimaryType(cache);
                        Property idProp = resolvedChild.getProperty(JcrLexicon.UUID, cache);
                        if (idProp == null || idProp.isEmpty()) {
                            desiredKey = target.getKey().withRandomId();
                        } else {
                            String uuid = string(idProp.getFirstValue());
                            desiredKey = NodeKey.isValidFormat(uuid) ? new NodeKey(uuid) : target.getKey().withId(uuid);
                        }
                        desiredName = sourceChildNode.name();
                    }
                    Property primaryType = propFactory.create(JcrLexicon.PRIMARY_TYPE, primaryTypeName);
                    targetChild = target.createChild(cache, desiredKey, desiredName, primaryType);
                    targetChildNode = session.node(targetChild, (Type)null);
                    assert shouldRestore;
                }

                if (shouldRestore) {
                    MutableCachedNode mutableTarget = targetChild instanceof MutableCachedNode ? (MutableCachedNode)targetChild : cache.mutable(targetChild.getKey());
                    // Have to do this first, as the properties below only exist for mix:versionable nodes
                    if (shouldRestoreMixinsAndUuid) {
                        if (JcrNtLexicon.FROZEN_NODE.equals(resolvedPrimaryTypeName)) {
                            //if we're dealing with a nt:versionedChild (and therefore the resolved node is a frozen node), we need the mixins from the frozen node
                            restoreNodeMixinsFromProperty(resolvedChild, mutableTarget, cache,JcrLexicon.FROZEN_MIXIN_TYPES);
                        } else {
                            restoreNodeMixins(sourceChild, mutableTarget, cache);
                        }
                    }

                    AbstractJcrNode parent = sourceChildNode.getParent();
                    if (parent.isNodeType(JcrNtLexicon.VERSION)) {
                        // Clear the checkout status ...
                        clearCheckoutStatus(mutableTarget, parent.key(), cache, propFactory);
                    }
                    restoreNode(sourceChildNode, targetChildNode, checkinTime);
                }

                if (prevChildKey != null) target.reorderChild(cache, targetChildNode.key(), prevChildKey);
                prevChildKey = targetChildNode.key();
            }
        }

        /**
         * Adds any missing mixin types from the source node to the target node
         * 
         * @param sourceNode the frozen source node; may not be be null
         * @param targetNode the target node; may not be null
         * @param cache the session cache; may not be null
         * @throws RepositoryException if an error occurs while accessing the repository or adding the mixin types
         */
        private void restoreNodeMixins( CachedNode sourceNode,
                                        MutableCachedNode targetNode,
                                        SessionCache cache ) throws RepositoryException {
            restoreNodeMixinsFromProperty(sourceNode, targetNode, cache, JcrLexicon.FROZEN_MIXIN_TYPES);
        }

        private void restoreNodeMixinsFromProperty( CachedNode sourceNode,
                                                    MutableCachedNode targetNode,
                                                    SessionCache cache,
                                                    Name sourceNodeMixinTypesPropertyName ) {
            Property mixinTypesProp = sourceNode.getProperty(sourceNodeMixinTypesPropertyName, cache);
            if (mixinTypesProp == null || mixinTypesProp.isEmpty()) return;
            Object[] mixinTypeNames = mixinTypesProp.getValuesAsArray();
            Collection<Name> currentMixinTypes = new HashSet<Name>(targetNode.getMixinTypes(cache));
            for (Object mixinTypeName1 : mixinTypeNames) {
                Name mixinTypeName = name(mixinTypeName1);
                if (!currentMixinTypes.remove(mixinTypeName)) {
                    targetNode.addMixin(cache, mixinTypeName);
                }
            }
        }

        /**
         * Restores the properties on the target node based on the stored properties on the source node. The restoration process
         * is based on the documentation in sections 8.2.7 and 8.2.11 of the JCR 1.0.1 specification.
         * 
         * @param sourceNode the frozen source node; may not be be null
         * @param targetNode the target node; may not be null
         * @throws RepositoryException if an error occurs while accessing the repository or modifying the properties
         */
        private void restoreProperties( AbstractJcrNode sourceNode,
                                        AbstractJcrNode targetNode ) throws RepositoryException {
            Map<Name, Property> sourceProperties = new HashMap<Name, Property>();
            Iterator<Property> iter = sourceNode.node().getProperties(cache);
            while (iter.hasNext()) {
                Property property = iter.next();
                if (!IGNORED_PROP_NAMES_FOR_RESTORE.contains(property.getName())) {
                    sourceProperties.put(property.getName(), property);
                }
            }

            MutableCachedNode mutable = targetNode.mutable();
            PropertyIterator existingPropIter = targetNode.getProperties();
            while (existingPropIter.hasNext()) {
                AbstractJcrProperty jcrProp = (AbstractJcrProperty)existingPropIter.nextProperty();
                Name propName = jcrProp.name();

                Property prop = sourceProperties.remove(propName);
                if (prop != null) {
                    // Overwrite the current property with the property from the version
                    mutable.setProperty(cache, prop);
                } else {
                    JcrPropertyDefinition propDefn = jcrProp.getDefinition();
                    switch (propDefn.getOnParentVersion()) {
                        case OnParentVersionAction.COPY:
                        case OnParentVersionAction.ABORT:
                        case OnParentVersionAction.VERSION:
                            // Use the internal method, which bypasses the checks
                            // and removes the AbstractJcrProperty object from the node's internal cache
                            targetNode.removeProperty(jcrProp);
                            break;

                        case OnParentVersionAction.COMPUTE:
                        case OnParentVersionAction.INITIALIZE:
                        case OnParentVersionAction.IGNORE:
                            // Do nothing
                    }
                }
            }

            // Write any properties that were on the source that weren't on the target ...
            for (Property sourceProperty : sourceProperties.values()) {
                mutable.setProperty(cache, sourceProperty);
            }
        }

        /**
         * Resolves the given source node into a frozen node. This may be as simple as returning the node itself (if it has a
         * primary type of nt:frozenNode) or converting the node to a version history, finding the best match from the versions in
         * that version history, and returning the frozen node for the best match (if the original source node has a primary type
         * of nt:versionedChild).
         * 
         * @param sourceNode the node for which the corresponding frozen node should be returned; may not be null
         * @param checkinTime the checkin time against which the versions in the version history should be matched; may not be
         *        null
         * @param cache the cache for the source node; may not be null
         * @return the frozen node that corresponds to the give source node; may not be null
         * @throws RepositoryException if an error occurs while accessing the repository
         * @see #closestMatchFor(JcrVersionHistoryNode, DateTime)
         */
        private CachedNode resolveSourceNode( CachedNode sourceNode,
                                              DateTime checkinTime,
                                              NodeCache cache ) throws RepositoryException {
            Name sourcePrimaryTypeName = name(sourceNode.getPrimaryType(cache));
            if (JcrNtLexicon.FROZEN_NODE.equals(sourcePrimaryTypeName)) return sourceNode;
            if (!JcrNtLexicon.VERSIONED_CHILD.equals(sourcePrimaryTypeName)) {
                return sourceNode;
            }

            // Must be a versioned child - try to see if it's one of the versions we're restoring
            org.modeshape.jcr.value.Property historyRefProp = sourceNode.getProperty(JcrLexicon.CHILD_VERSION_HISTORY, cache);
            String keyStr = string(historyRefProp.getFirstValue());
            assert keyStr != null;

            /*
             * First try to find a match among the rootless versions in this restore operation
             */
            for (Version version : nonExistingVersions) {
                if (keyStr.equals(version.getContainingHistory().getIdentifier())) {
                    JcrVersionNode versionNode = (JcrVersionNode)version;
                    nonExistingVersions.remove(version);
                    return versionNode.getFrozenNode().node();
                }
            }

            /*
             * Then check the rooted versions in this restore operation
             */
            for (Version version : existingVersions.keySet()) {
                if (keyStr.equals(version.getContainingHistory().getIdentifier())) {
                    JcrVersionNode versionNode = (JcrVersionNode)version;
                    existingVersions.remove(version);
                    return versionNode.getFrozenNode().node();
                }
            }

            /*
             * If there was a label for this restore operation, try to match that way
             */
            JcrVersionHistoryNode versionHistory = (JcrVersionHistoryNode)session.getNodeByIdentifier(keyStr);
            if (labelToRestore != null) {
                try {
                    JcrVersionNode versionNode = versionHistory.getVersionByLabel(labelToRestore);
                    return versionNode.getFrozenNode().node();
                } catch (VersionException noVersionWithThatLabel) {
                    // This can happen if there's no version with that label - valid
                }
            }

            /*
             * If all else fails, find the last version checked in before the checkin time for the version being restored
             */
            AbstractJcrNode match = closestMatchFor(versionHistory, checkinTime);
            return match.node();
        }

        /**
         * Finds a node that has the same UUID as is specified in the jcr:frozenUuid property of {@code sourceNode}. If a match
         * exists and it is a descendant of one of the {@link #versionRootPaths root paths} for this restore operation, it is
         * returned. If a match exists but is not a descendant of one of the root paths for this restore operation, either an
         * exception is thrown (if {@link #removeExisting} is false) or the match is deleted and null is returned (if
         * {@link #removeExisting} is true).
         * 
         * @param sourceNode the node for which the match should be checked; may not be null
         * @param cache the cache containing the source node; may not be null
         * @return the existing node with the same UUID as is specified in the jcr:frozenUuid property of {@code sourceNode}; null
         *         if no node exists with that UUID
         * @throws ItemExistsException if {@link #removeExisting} is false and the node is not a descendant of any of the
         *         {@link #versionRootPaths root paths} for this restore command
         * @throws RepositoryException if any other error occurs while accessing the repository
         */
        private CachedNode findMatchFor( CachedNode sourceNode,
                                         NodeCache cache ) throws ItemExistsException, RepositoryException {

            org.modeshape.jcr.value.Property idProp = sourceNode.getProperty(JcrLexicon.FROZEN_UUID, cache);
            if (idProp == null) return null;

            String idStr = string(idProp.getFirstValue());
            try {
                AbstractJcrNode match = session.getNodeByIdentifier(idStr);
                if (nodeIsOutsideRestoredForest(match)) return null;
                return match.node();
            } catch (ItemNotFoundException infe) {
                return null;
            }
        }

        /**
         * Creates a list that is a copy of the supplied ChildReferences object.
         * 
         * @param references the child references
         * @return a list containing the same elements as {@code references} in the same order; never null
         */
        private List<NodeKey> asList( ChildReferences references ) {
            assert references.size() < Integer.MAX_VALUE;
            List<NodeKey> newList = new ArrayList<NodeKey>((int)references.size());
            for (ChildReference ref : references) {
                newList.add(ref.getKey());
            }
            return newList;
        }

        /**
         * Checks if the given node is outside any of the root paths for this restore command. If this occurs, a special check of
         * the {@link #removeExisting} flag must be performed.
         * 
         * @param node the node to check; may not be null
         * @return true if the node is not a descendant of any of the {@link #versionRootPaths root paths} for this restore
         *         command, false otherwise.
         * @throws ItemExistsException if {@link #removeExisting} is false and the node is not a descendant of any of the
         *         {@link #versionRootPaths root paths} for this restore command
         * @throws RepositoryException if any other error occurs while accessing the repository
         */
        private boolean nodeIsOutsideRestoredForest( AbstractJcrNode node ) throws ItemExistsException, RepositoryException {
            Path nodePath = node.path();
            for (Path rootPath : versionRootPaths) {
                if (nodePath.isAtOrBelow(rootPath)) return false;
            }
            if (!removeExisting) {
                throw new ItemExistsException(JcrI18n.itemAlreadyExistsWithUuid.text(node.key(),
                                                                                     session.workspace().getName(),
                                                                                     node.getPath()));
            }
            node.remove();
            return true;
        }

        /**
         * Returns the most recent version for the given version history that was checked in before the given time.
         * 
         * @param versionHistory the version history to check; may not be null
         * @param checkinTime the checkin time against which the versions in the version history should be matched; may not be
         *        null
         * @return the {@link JcrVersionNode#getFrozenNode() frozen node} under the most recent {@link Version version} for the
         *         version history that was checked in before {@code checkinTime}; never null
         * @throws RepositoryException if an error occurs accessing the repository
         */
        private AbstractJcrNode closestMatchFor( JcrVersionHistoryNode versionHistory,
                                                 DateTime checkinTime ) throws RepositoryException {
            DateTimeFactory dateFactory = session.context().getValueFactories().getDateFactory();

            VersionIterator iter = versionHistory.getAllVersions();
            Map<DateTime, Version> versions = new HashMap<DateTime, Version>((int)iter.getSize());

            while (iter.hasNext()) {
                Version version = iter.nextVersion();
                versions.put(dateFactory.create(version.getCreated()), version);
            }

            List<DateTime> versionDates = new ArrayList<DateTime>(versions.keySet());
            Collections.sort(versionDates);

            for (int i = versionDates.size() - 1; i >= 0; i--) {
                if (versionDates.get(i).isBefore(checkinTime)) {
                    Version version = versions.get(versionDates.get(i));
                    return ((JcrVersionNode)version).getFrozenNode();
                }
            }

            throw new IllegalStateException("First checkin must be before the checkin time of the node to be restored");
        }
    }

    @NotThreadSafe
    private class MergeCommand {
        private final Collection<AbstractJcrNode> failures;
        private final AbstractJcrNode targetNode;
        private final boolean bestEffort;
        private final boolean isShallow;
        private final JcrSession sourceSession;
        private final SessionCache cache;
        private final String workspaceName;

        public MergeCommand( AbstractJcrNode targetNode,
                             JcrSession sourceSession,
                             boolean bestEffort,
                             boolean isShallow ) {
            this.targetNode = targetNode;
            this.sourceSession = sourceSession;
            this.cache = this.sourceSession.cache();
            this.bestEffort = bestEffort;
            this.isShallow = isShallow;

            this.workspaceName = sourceSession.getWorkspace().getName();
            this.failures = new LinkedList<AbstractJcrNode>();
        }

        final NodeIterator getFailures() {
            return new JcrNodeListIterator(failures.iterator(), failures.size());
        }

        void execute() throws RepositoryException {
            doMerge(targetNode);
        }

        /*
        let n' be the corresponding node of n in ws'. 
        if no such n' doleave(n).

        else if n is not versionable doupdate(n, n'). 
        else if n' is not versionable doleave(n). 
        let v be base version of n. 
        let v' be base version of n'.
        if v' is an eventual successor of v and n is not checked-in doupdate(n, n').
        else if v is equal to or an eventual predecessor of v' doleave(n). 
        else dofail(n, v').
         */
        private void doMerge( AbstractJcrNode targetNode ) throws RepositoryException {
            // n is targetNode
            // n' is sourceNode
            Path sourcePath = targetNode.correspondingNodePath(workspaceName);

            AbstractJcrNode sourceNode;
            try {
                sourceNode = sourceSession.node(sourcePath);
            } catch (ItemNotFoundException infe) {
                doLeave(targetNode);
                return;
            }

            if (!targetNode.isNodeType(JcrMixLexicon.VERSIONABLE)) {
                doUpdate(targetNode, sourceNode);
                return;
            } else if (!sourceNode.isNodeType(JcrMixLexicon.VERSIONABLE)) {
                doLeave(targetNode);
                return;
            }

            JcrVersionNode sourceVersion = sourceNode.getBaseVersion();
            JcrVersionNode targetVersion = targetNode.getBaseVersion();

            if (sourceVersion.isSuccessorOf(targetVersion) && !targetNode.isCheckedOut()) {
                doUpdate(targetNode, sourceNode);
                return;
            }

            if (targetVersion.isSuccessorOf(sourceVersion) || targetVersion.key().equals(sourceVersion.key())) {
                doLeave(targetNode);
                return;
            }

            doFail(targetNode, sourceVersion);
        }

        /*
        if isShallow = false
            for each child node c of n domerge(c).
         */
        private void doLeave( AbstractJcrNode targetNode ) throws RepositoryException {
            if (isShallow == false) {

                for (NodeIterator iter = targetNode.getNodes(); iter.hasNext();) {
                    doMerge((AbstractJcrNode)iter.nextNode());
                }
            }
        }

        /*
        replace set of properties of n with those of n'. 
        let S be the set of child nodes of n. 
        let S' be the set of child nodes of n'. 

        judging by the name of the child node:
        let C be the set of nodes in S and in S' 
        let D be the set of nodes in S but not in S'. 
        let D' be the set of nodes in S' but not in S.
        remove from n all child nodes in D. 
        for each child node of n' in D' copy it (and its subtree) to n
        as a new child node (if an incoming node has the same UUID as a node already existing in this workspace, the already existing node is removed).
        for each child node m of n in C domerge(m).
         */
        private void doUpdate( AbstractJcrNode targetNode,
                               AbstractJcrNode sourceNode ) throws RepositoryException {
            restoreProperties(sourceNode, targetNode);

            LinkedHashMap<String, AbstractJcrNode> sourceNodes = childNodeMapFor(sourceNode);
            LinkedHashMap<String, AbstractJcrNode> targetNodes = childNodeMapFor(targetNode);

            // D' set in algorithm above
            Map<String, AbstractJcrNode> sourceOnly = new LinkedHashMap<String, AbstractJcrNode>(sourceNodes);
            sourceOnly.keySet().removeAll(targetNodes.keySet());

            for (AbstractJcrNode node : sourceOnly.values()) {
                workspace().copy(workspaceName, node.getPath(), targetNode.getPath() + "/" + node.getName());
            }

            // D set in algorithm above
            LinkedHashMap<String, AbstractJcrNode> targetOnly = new LinkedHashMap<String, AbstractJcrNode>(targetNodes);
            targetOnly.keySet().removeAll(targetOnly.keySet());

            for (AbstractJcrNode node : targetOnly.values()) {
                node.remove();
            }

            // C set in algorithm above
            Map<String, AbstractJcrNode> presentInBoth = new HashMap<String, AbstractJcrNode>(targetNodes);
            presentInBoth.keySet().retainAll(sourceNodes.keySet());
            for (AbstractJcrNode node : presentInBoth.values()) {
                if (isShallow && node.isNodeType(JcrMixLexicon.VERSIONABLE)) continue;
                doMerge(node);
            }
        }

        private LinkedHashMap<String, AbstractJcrNode> childNodeMapFor( AbstractJcrNode node ) throws RepositoryException {
            LinkedHashMap<String, AbstractJcrNode> childNodes = new LinkedHashMap<String, AbstractJcrNode>();

            for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
                AbstractJcrNode child = (AbstractJcrNode)iter.nextNode();
                childNodes.put(child.getName(), child);
            }

            return childNodes;
        }

        /*
        if bestEffort = false throw MergeException. 
        else add identifier of v' (if not already present) to the
            jcr:mergeFailed property of n, 
            add identifier of n to failedset, 
            if isShallow = false 
                for each versionable child node c of n domerge(c)
         */

        private void doFail( AbstractJcrNode targetNode,
                             JcrVersionNode sourceVersion ) throws RepositoryException {
            if (!bestEffort) {
                throw new MergeException();
            }

            if (targetNode.hasProperty(JcrLexicon.MERGE_FAILED)) {
                JcrValue[] existingValues = targetNode.getProperty(JcrLexicon.MERGE_FAILED).getValues();

                boolean found = false;
                String sourceKeyString = sourceVersion.getIdentifier();
                for (int i = 0; i < existingValues.length; i++) {
                    if (sourceKeyString.equals(existingValues[i].getString())) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    JcrValue[] newValues = new JcrValue[existingValues.length + 1];
                    System.arraycopy(existingValues, 0, newValues, 0, existingValues.length);
                    newValues[newValues.length - 1] = targetNode.valueFrom(sourceVersion);
                    targetNode.setProperty(JcrLexicon.MERGE_FAILED, newValues, PropertyType.REFERENCE, false);
                }

            } else {
                targetNode.setProperty(JcrLexicon.MERGE_FAILED, targetNode.valueFrom(sourceVersion), false, false);
            }
            failures.add(targetNode);

            if (isShallow == false) {
                for (NodeIterator iter = targetNode.getNodes(); iter.hasNext();) {
                    AbstractJcrNode childNode = (AbstractJcrNode)iter.nextNode();

                    if (childNode.isNodeType(JcrMixLexicon.VERSIONABLE)) {
                        doMerge(childNode);
                    }
                }
            }
        }

        /**
         * Restores the properties on the target node based on the stored properties on the source node. The restoration process
         * involves copying over all of the properties on the source to the target.
         * 
         * @param sourceNode the source node; may not be be null
         * @param targetNode the target node; may not be null
         * @throws RepositoryException if an error occurs while accessing the repository or modifying the properties
         */
        private void restoreProperties( AbstractJcrNode sourceNode,
                                        AbstractJcrNode targetNode ) throws RepositoryException {
            Map<Name, Property> sourceProperties = new HashMap<Name, Property>();
            Iterator<Property> iter = sourceNode.node().getProperties(cache);
            while (iter.hasNext()) {
                Property property = iter.next();
                if (!IGNORED_PROP_NAMES_FOR_RESTORE.contains(property.getName())) {
                    sourceProperties.put(property.getName(), property);
                }
            }

            MutableCachedNode mutable = targetNode.mutable();
            PropertyIterator existingPropIter = targetNode.getProperties();
            while (existingPropIter.hasNext()) {
                AbstractJcrProperty jcrProp = (AbstractJcrProperty)existingPropIter.nextProperty();
                Name propName = jcrProp.name();

                Property prop = sourceProperties.remove(propName);
                if (prop != null) {
                    // Overwrite the current property with the property from the version
                    mutable.setProperty(cache, prop);
                } else {
                    JcrPropertyDefinition propDefn = jcrProp.getDefinition();
                    switch (propDefn.getOnParentVersion()) {
                        case OnParentVersionAction.COPY:
                        case OnParentVersionAction.ABORT:
                        case OnParentVersionAction.VERSION:
                            // Use the internal method, which bypasses the checks
                            // and removes the AbstractJcrProperty object from the node's internal cache
                            targetNode.removeProperty(jcrProp);
                            break;

                        case OnParentVersionAction.COMPUTE:
                        case OnParentVersionAction.INITIALIZE:
                        case OnParentVersionAction.IGNORE:
                            // Do nothing
                    }
                }
            }

            // Write any properties that were on the source that weren't on the target ...
            for (Property sourceProperty : sourceProperties.values()) {
                mutable.setProperty(cache, sourceProperty);
            }
        }
    }

}
