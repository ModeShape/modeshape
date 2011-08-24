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

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.MergeException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;
import org.modeshape.common.text.Jsr283Encoder;
import org.modeshape.common.text.TextEncoder;
import org.modeshape.common.util.Logger;
import org.modeshape.jcr.AbstractJcrNode.Type;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.SessionCache;
import org.modeshape.jcr.core.ExecutionContext;
import org.modeshape.jcr.value.DateTime;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NameFactory;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.PathFactory;
import org.modeshape.jcr.value.PropertyFactory;

/**
 * Local implementation of version management code, comparable to an implementation of the JSR-283 {@code VersionManager}
 * interface. Valid instances of this class can be obtained by calling {@link JcrWorkspace#versionManager()}.
 */
final class JcrVersionManager implements VersionManager {

    private static final Logger LOGGER = Logger.getLogger(JcrVersionManager.class);

    protected static final TextEncoder NODE_ENCODER = new Jsr283Encoder();

    static final Object[] EMPTY_OBJECT_ARRAY = new Object[] {};

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

    final Name name( Object ob ) {
        return session().nameFactory().create(ob);
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

    final DateTime dateTime( Calendar cal ) {
        return session.dateFactory().create(cal);
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

    /**
     * Returns the version history (if one exists) for the given node.
     * 
     * @param node the node for which the history should be returned
     * @return the version history for the node
     * @throws ItemNotFoundException if there is no version history for the given node
     * @throws RepositoryException if any other error occurs accessing the repository
     * @see AbstractJcrNode#getVersionHistory()
     */
    JcrVersionHistoryNode getVersionHistory( AbstractJcrNode node ) throws RepositoryException {
        session.checkLive();
        // checkVersionable(node);

        // Try to look up the version history by its key ...
        NodeKey historyKey = readableSystem.versionHistoryNodeKeyFor(node.key());
        SessionCache cache = session.cache();
        CachedNode historyNode = cache.getNode(historyKey);
        if (historyNode != null) {
            return (JcrVersionHistoryNode)session.node(historyNode, Type.VERSION_HISTORY);
        }
        // We have to initialize the version history for this node ...
        initializeVersionHistoryFor(node, historyKey, cache);
        return (JcrVersionHistoryNode)session.node(historyNode, Type.VERSION_HISTORY);
    }

    void initializeVersionHistoryFor( AbstractJcrNode node,
                                      NodeKey historyKey,
                                      SessionCache cache ) throws RepositoryException {
        CachedNode cachedNode = node.node();
        Name primaryTypeName = cachedNode.getPrimaryType(cache);
        Set<Name> mixinTypeNames = cachedNode.getMixinTypes(cache);
        NodeKey versionedKey = cachedNode.getKey();
        Path versionHistoryPath = versionHistoryPathFor(versionedKey);
        String workspace = workspace().getName();
        DateTime now = session().dateFactory().create();

        SystemContent content = new SystemContent(session.createSystemCache(false));
        content.initializeVersionStorage(versionedKey,
                                         historyKey,
                                         primaryTypeName,
                                         mixinTypeNames,
                                         versionHistoryPath,
                                         historyKey,
                                         context(),
                                         workspace,
                                         now);
        content.save();
    }

    /**
     * Checks in the given node, creating (and returning) a new {@link Version}.
     * 
     * @param node the node to be checked in
     * @return the {@link Version} object created as a result of this checkin
     * @throws RepositoryException if an error occurs during the checkin. See {@link javax.jcr.Node#checkin()} for a full
     *         description of the possible error conditions.
     */
    JcrVersionNode checkin( AbstractJcrNode node ) throws RepositoryException {
        session.checkLive();
        // checkVersionable(node);

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

        Property isCheckedOut = node.getProperty(JcrLexicon.IS_CHECKED_OUT);
        if (!isCheckedOut.getBoolean()) {
            return node.getBaseVersion();
        }

        // SessionCache systemSession = session.createSystemCache(false);
        // SystemContent systemContent = new SystemContent(systemSession);
        // systemContent.createVersionNode()
        //
        //
        // // Collect some of the information about the node that we'll need ...
        // final UUID jcrUuid = node.uuid();
        // final Name primaryTypeName = node.getPrimaryTypeName();
        // final List<Name> mixinTypeNames = node.getMixinTypeNames();
        // final org.modeshape.graph.property.Property predecessorsProp = node.getProperty(JcrLexicon.PREDECESSORS).property();
        // final List<org.modeshape.graph.property.Property> versionedProperties = versionedPropertiesFor(node, false);
        // final Path historyPath = versionHistoryPathFor(jcrUuid);
        // final DateTime now = context().getValueFactories().getDateFactory().create();
        // final Name versionName = name(NODE_ENCODER.encode(now.getString()));
        // Path versionPath = path(historyPath, versionName);
        // Path frozenVersionPath = path(versionPath, JcrLexicon.FROZEN_NODE);
        //
        // Graph systemGraph = repository().createSystemGraph(context());
        // Graph.Batch systemBatch = systemGraph.batch();
        //
        // systemBatch.applyFunction(SystemFunctions.CREATE_VERSION_NODE)
        // .withInput(CreateVersionNodeFunction.VERSION_HISTORY_PATH, historyPath)
        // .withInput(CreateVersionNodeFunction.VERSION_NAME, versionName)
        // .withInput(CreateVersionNodeFunction.VERSIONED_NODE_UUID, jcrUuid)
        // .withInput(CreateVersionNodeFunction.PRIMARY_TYPE_NAME, primaryTypeName)
        // .withInput(CreateVersionNodeFunction.MIXIN_TYPE_NAME_LIST, mixinTypeNames)
        // .withInput(CreateVersionNodeFunction.PREDECESSOR_PROPERTY, predecessorsProp)
        // .withInput(CreateVersionNodeFunction.VERSION_PROPERTY_LIST, versionedProperties)
        // .to(versionStoragePath);
        //
        // for (NodeIterator childNodes = node.getNodes(); childNodes.hasNext();) {
        // AbstractJcrNode childNode = (AbstractJcrNode)childNodes.nextNode();
        // versionNodeAt(childNode, frozenVersionPath, systemBatch, false);
        // }
        //
        // // Execute the batch and get the results ...
        // Results results = systemBatch.execute();
        // List<Request> requests = results.getRequests();
        // FunctionRequest createVersionFunction = (FunctionRequest)requests.get(0);
        //
        // if (createVersionFunction.hasError()) {
        // Throwable error = createVersionFunction.getError();
        // String msg = JcrI18n.errorDuringCheckinNode.text(node.getPath(), error.getMessage());
        // throw new VersionException(msg, error);
        // }
        //
        // // Get the highest node that was changed ...
        // Path highestChanged = (Path)createVersionFunction.output(CreateVersionNodeFunction.PATH_OF_HIGHEST_MODIFIED_NODE);
        //
        // // Find the version history UUID (which may have been created) ...
        // UUID versionHistoryUuid = (UUID)createVersionFunction.output(CreateVersionNodeFunction.VERSION_HISTORY_UUID);
        //
        // // Find the new version (which we'll need to return) ...
        // UUID versionUuid = (UUID)createVersionFunction.output(CreateVersionNodeFunction.VERSION_UUID);
        // versionPath = (Path)createVersionFunction.output(CreateVersionNodeFunction.VERSION_PATH);
        // Location versionLocation = Location.create(versionPath, versionUuid);
        //
        // // We have to refresh the parent of the highest node that was changed ...
        // Path refreshPath = highestChanged.getParent();
        // cache().refresh(refreshPath, false);
        //
        // // But make sure we refresh the version history for this node if we didn't change it ...
        // if (!refreshPath.isAtOrAbove(historyPath)) {
        // cache().refresh(historyPath, false);
        // }
        //
        // // Update the node's 'mix:versionable' properties ...
        // NodeEditor editor = node.editor();
        // editor.setProperty(JcrLexicon.PREDECESSORS,
        // node.valuesFrom(PropertyType.REFERENCE, EMPTY_OBJECT_ARRAY),
        // PropertyType.REFERENCE,
        // false);
        // editor.setProperty(JcrLexicon.VERSION_HISTORY, node.valueFrom(versionHistoryUuid), false, false);
        // editor.setProperty(JcrLexicon.BASE_VERSION, node.valueFrom(versionUuid), false, false);
        // editor.setProperty(JcrLexicon.IS_CHECKED_OUT, node.valueFrom(PropertyType.BOOLEAN, false), false, false);
        // node.save();
        //
        // // fix for MODE-1245
        // // because of the performance of the jpa source (i.e., mysql, oracle, etc.), it could cause a timing
        // // issue with loading the cache and finding the node. Therefore, by moving the find
        // // to the end of the method and, on the rare case the find isn't found on the first lookup,
        // // a loop of maximum 5 tries will be peformed in order to allow the cache time to fill.
        // AbstractJcrNode newVersion = null;
        // javax.jcr.ItemNotFoundException notFoundException = null;
        //
        // for (int i = 0; newVersion == null && i < 5; i++) {
        // try {
        // newVersion = cache().findJcrNode(versionLocation);
        // } catch (javax.jcr.ItemNotFoundException infe) {
        // LOGGER.debug("VersionLocation {0} not found, retry findJcrNode", versionLocation);
        //
        // // capture 1st occurrence of the exception
        // if (notFoundException == null) notFoundException = infe;
        // }
        // }
        // if (notFoundException != null) throw notFoundException;
        //
        // return (JcrVersionNode)newVersion;
        return null;
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

        public Path versionHistoryPathFor( String sha1OrUuid ) {
            Name p1 = names.create(sha1OrUuid.substring(0, 2));
            Name p2 = names.create(sha1OrUuid.substring(2, 4));
            Name p3 = names.create(sha1OrUuid.substring(4, 6));
            Name p4 = names.create(sha1OrUuid.substring(6));
            return paths.createAbsolutePath(JcrLexicon.SYSTEM, JcrLexicon.VERSION_STORAGE, p1, p2, p3, p4);
        }
    }

    protected static class FlatPathAlgorithm extends BasePathAlgorithm {
        protected FlatPathAlgorithm( Path versionStoragePath,
                                     ExecutionContext context ) {
            super(versionStoragePath, context);
        }

        public Path versionHistoryPathFor( String sha1OrUuid ) {
            return paths.createAbsolutePath(JcrLexicon.SYSTEM, JcrLexicon.VERSION_STORAGE, names.create(sha1OrUuid));
        }
    }

    @Override
    public Version checkin( String absPath )
        throws VersionException, UnsupportedRepositoryOperationException, InvalidItemStateException, LockException,
        RepositoryException {
        return null;
    }

    @Override
    public void checkout( String absPath ) throws UnsupportedRepositoryOperationException, LockException, RepositoryException {
    }

    @Override
    public Version checkpoint( String absPath )
        throws VersionException, UnsupportedRepositoryOperationException, InvalidItemStateException, LockException,
        RepositoryException {
        return null;
    }

    @Override
    public boolean isCheckedOut( String absPath ) throws RepositoryException {
        return false;
    }

    @Override
    public VersionHistory getVersionHistory( String absPath ) throws UnsupportedRepositoryOperationException, RepositoryException {
        return null;
    }

    @Override
    public Version getBaseVersion( String absPath ) throws UnsupportedRepositoryOperationException, RepositoryException {
        return null;
    }

    @Override
    public void restore( Version[] versions,
                         boolean removeExisting )
        throws ItemExistsException, UnsupportedRepositoryOperationException, VersionException, LockException,
        InvalidItemStateException, RepositoryException {
    }

    @Override
    public void restore( String absPath,
                         String versionName,
                         boolean removeExisting )
        throws VersionException, ItemExistsException, UnsupportedRepositoryOperationException, LockException,
        InvalidItemStateException, RepositoryException {
    }

    @Override
    public void restore( Version version,
                         boolean removeExisting )
        throws VersionException, ItemExistsException, InvalidItemStateException, UnsupportedRepositoryOperationException,
        LockException, RepositoryException {
    }

    @Override
    public void restore( String absPath,
                         Version version,
                         boolean removeExisting )
        throws PathNotFoundException, ItemExistsException, VersionException, ConstraintViolationException,
        UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
    }

    @Override
    public void restoreByLabel( String absPath,
                                String versionLabel,
                                boolean removeExisting )
        throws VersionException, ItemExistsException, UnsupportedRepositoryOperationException, LockException,
        InvalidItemStateException, RepositoryException {
    }

    @Override
    public NodeIterator merge( String absPath,
                               String srcWorkspace,
                               boolean bestEffort )
        throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException,
        RepositoryException {
        return null;
    }

    @Override
    public NodeIterator merge( String absPath,
                               String srcWorkspace,
                               boolean bestEffort,
                               boolean isShallow )
        throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException,
        RepositoryException {
        return null;
    }

    @Override
    public void doneMerge( String absPath,
                           Version version )
        throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
    }

    @Override
    public void cancelMerge( String absPath,
                             Version version )
        throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
    }

    @Override
    public Node createConfiguration( String absPath ) throws UnsupportedRepositoryOperationException, RepositoryException {
        return null;
    }

    @Override
    public Node setActivity( Node activity ) throws UnsupportedRepositoryOperationException, RepositoryException {
        return null;
    }

    @Override
    public Node getActivity() throws UnsupportedRepositoryOperationException, RepositoryException {
        return null;
    }

    @Override
    public Node createActivity( String title ) throws UnsupportedRepositoryOperationException, RepositoryException {
        return null;
    }

    @Override
    public void removeActivity( Node activityNode )
        throws UnsupportedRepositoryOperationException, VersionException, RepositoryException {
    }

    @Override
    public NodeIterator merge( Node activityNode )
        throws VersionException, AccessDeniedException, MergeException, LockException, InvalidItemStateException,
        RepositoryException {
        return null;
    }

}
