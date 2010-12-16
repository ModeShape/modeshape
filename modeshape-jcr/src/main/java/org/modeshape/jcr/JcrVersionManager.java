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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.MergeException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.OnParentVersionAction;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.text.Jsr283Encoder;
import org.modeshape.common.text.TextEncoder;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.Logger;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.Location;
import org.modeshape.graph.Graph.Batch;
import org.modeshape.graph.property.DateTime;
import org.modeshape.graph.property.DateTimeFactory;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NameFactory;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.PropertyFactory;
import org.modeshape.graph.property.Reference;
import org.modeshape.graph.property.ValueFactories;
import org.modeshape.graph.property.ValueFactory;
import org.modeshape.graph.property.Path.Segment;
import org.modeshape.graph.session.GraphSession.Node;
import org.modeshape.graph.session.GraphSession.PropertyInfo;
import org.modeshape.jcr.JcrRepository.Option;
import org.modeshape.jcr.JcrRepository.VersionHistoryOption;
import org.modeshape.jcr.SessionCache.JcrNodePayload;
import org.modeshape.jcr.SessionCache.JcrPropertyPayload;
import org.modeshape.jcr.SessionCache.NodeEditor;

/**
 * Local implementation of version management code, comparable to an implementation of the JSR-283 {@code VersionManager}
 * interface. Valid instances of this class can be obtained by calling {@link JcrWorkspace#versionManager()}.
 */
final class JcrVersionManager implements VersionManager {

    private static final Logger LOGGER = Logger.getLogger(JcrVersionManager.class);

    private static final TextEncoder NODE_ENCODER = new Jsr283Encoder();

    static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

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
    protected final Path versionStoragePath;
    private final PathAlgorithm hiearchicalPathAlgorithm;
    private final PathAlgorithm flatPathAlgorithm;
    private final PathAlgorithm versionHistoryPathAlgorithm;

    public JcrVersionManager( JcrSession session ) {
        super();
        this.session = session;
        versionStoragePath = absolutePath(JcrLexicon.SYSTEM, JcrLexicon.VERSION_STORAGE);
        String storageFormat = session.repository().getOptions().get(Option.VERSION_HISTORY_STRUCTURE);
        if (storageFormat != null) storageFormat = storageFormat.trim().toLowerCase();
        ExecutionContext context = session.getExecutionContext();
        boolean isHier = VersionHistoryOption.HIERARCHICAL.equals(storageFormat);
        hiearchicalPathAlgorithm = new HiearchicalPathAlgorithm(versionStoragePath, context);
        flatPathAlgorithm = new FlatPathAlgorithm(versionStoragePath, context);
        versionHistoryPathAlgorithm = isHier ? hiearchicalPathAlgorithm : flatPathAlgorithm;
    }

    ExecutionContext context() {
        return session.getExecutionContext();
    }

    private ValueFactories factories() {
        return context().getValueFactories();
    }

    UUID uuid( Object ob ) {
        return factories().getUuidFactory().create(ob);
    }

    Name name( String s ) {
        return factories().getNameFactory().create(s);
    }

    Name name( Object ob ) {
        return factories().getNameFactory().create(ob);
    }

    final Path path( Path root,
                     Name child ) {
        return factories().getPathFactory().create(root, child);
    }

    Path path( Path root,
               Path.Segment childSegment ) {
        return factories().getPathFactory().create(root, childSegment);
    }

    private Path absolutePath( Name... absolutePathSegments ) {
        return factories().getPathFactory().createAbsolutePath(absolutePathSegments);
    }

    DateTime dateTime( Calendar cal ) {
        return factories().getDateFactory().create(cal);
    }

    private PropertyFactory propertyFactory() {
        return context().getPropertyFactory();
    }

    SessionCache cache() {
        return session.cache();
    }

    private JcrRepository repository() {
        return session.repository();
    }

    JcrSession session() {
        return session;
    }

    JcrWorkspace workspace() {
        return session.workspace();
    }

    /**
     * Return the path to the nt:versionHistory node for the node with the supplied UUID.
     * <p>
     * This method uses one of two algorithms.
     * <ul>
     * <li>The flat algorithm just returns the path <code>/jcr:system/jcr:versionStorage/&lt;UUID></code>. For example, given the
     * UUID <code>fae2b929-c5ef-4ce5-9fa1-514779ca0ae3</code>, the returned path would be
     * <code>/jcr:system/jcr:versionStorage/fae2b929-c5ef-4ce5-9fa1-514779ca0ae3</code>.</li>
     * <li>The hierarchical algorithm breaks the UUID string into 5 parts (where the first 4 parts each consist of two characters
     * in the string representation) and returns the path
     * <code>/jcr:system/jcr:versionStorage/&lt;part1>/&lt;part2>/&lt;part3>/&lt;part4>/&lt;part5></code>, where
     * <code>part1</code> consists of the 1st and 2nd characters of the UUID string, <code>part2</code> consists of the 3rd and
     * 4th characters of the UUID string, <code>part3</code> consists of the 5th and 6th characters of the UUID string,
     * <code>part4</code> consists of the 7th and 8th characters of the UUID string, and <code>part5</code> consists of the 10th
     * and remaining characters. (Note the 9th character is a '-' and is not used.) For example, given the UUID
     * <code>fae2b929-c5ef-4ce5-9fa1-514779ca0ae3</code>, the returned path would be
     * <code>/jcr:system/jcr:versionStorage/fa/e2/b9/29/c5ef-4ce5-9fa1-514779ca0ae3</code>.</li>
     * </ul>
     * </p>
     * 
     * @param uuid the value of the {@code jcr:uuid} property (as a UUID) for the node for which the version history should be
     *        returned
     * @return the path to the version history node that corresponds to the node with the given UUID. This does not guarantee that
     *         a node exists at the returned path. In fact, if the node with the given UUID is not versionable (i.e., {@code
     *         node.getUUID().equals(uuid.toString()) && !node.isNodeType("mix:versionable")}), there will most likely not be a
     *         node at the path returned by this method.
     */
    Path versionHistoryPathFor( UUID uuid ) {
        return versionHistoryPathAlgorithm.versionHistoryPathFor(uuid);
    }

    Path[] versionHistoryPathsTo( Path historyPath ) {
        int numIntermediatePaths = historyPath.size() - versionStoragePath.size() - 1;
        if (numIntermediatePaths <= 0) return null;

        Path[] paths = new Path[numIntermediatePaths];
        Path path = historyPath.getParent();
        for (int i = numIntermediatePaths - 1; i >= 0; --i) {
            paths[i] = path;
            path = path.getParent();
        }
        return paths;
    }

    /**
     * Returns the version history (if one exists) for the given node.
     * 
     * @param node the node for which the history should be returned
     * @return the version history for the node
     * @throws ItemNotFoundException if there is no version history for the given UUID
     * @throws RepositoryException if any other error occurs accessing the repository
     * @see AbstractJcrNode#getVersionHistory()
     */
    JcrVersionHistoryNode getVersionHistory( AbstractJcrNode node ) throws RepositoryException {
        session.checkLive();
        checkVersionable(node);

        Location historyLocation = Location.create(versionHistoryPathFor(node.uuid()));
        try {
            return (JcrVersionHistoryNode)cache().findJcrNode(historyLocation);
        } catch (ItemNotFoundException infe) {
            if (versionHistoryPathAlgorithm != flatPathAlgorithm) {
                // Next try the flat structure, in case the version history was already created and the structure was changed ...
                try {
                    Location flatHistoryLocation = Location.create(versionHistoryPathFor(node.uuid()));
                    return (JcrVersionHistoryNode)cache().findJcrNode(flatHistoryLocation);
                } catch (ItemNotFoundException nested) {
                    // Ignore and continue ...
                }
            }
            initializeVersionHistoryFor(node);

            // This will throw an ItemNotFoundException if the history node still doesn't exist
            JcrVersionHistoryNode historyNode = (JcrVersionHistoryNode)cache().findJcrNode(historyLocation);

            LOGGER.warn(JcrI18n.repairedVersionStorage, historyLocation);

            return historyNode;
        }
    }

    /**
     * Returns the node definition for the given node
     * 
     * @param node the node for which the definition should be returned
     * @return the active node definition for the given node
     * @throws RepositoryException if an error occurs accessing the repository
     * @see JcrNodeTypeManager#getNodeDefinition(NodeDefinitionId)
     */
    JcrNodeDefinition nodeDefinitionFor( Node<JcrNodePayload, JcrPropertyPayload> node ) throws RepositoryException {
        NodeDefinitionId nodeDefnId = node.getPayload().getDefinitionId();
        return session().nodeTypeManager().getNodeDefinition(nodeDefnId);
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

        Property isCheckedOut = node.getProperty(JcrLexicon.IS_CHECKED_OUT);

        if (!isCheckedOut.getBoolean()) {
            return node.getBaseVersion();
        }

        Name primaryTypeName = node.getPrimaryTypeName();
        List<Name> mixinTypeNames = node.getMixinTypeNames();

        UUID jcrUuid = node.uuid();
        UUID versionUuid = UUID.randomUUID();

        AbstractJcrNode historyNode = getVersionHistory(node);
        Path historyPath = historyNode.path();

        Graph systemGraph = repository().createSystemGraph(context());
        Graph.Batch systemBatch = systemGraph.batch();
        DateTime now = context().getValueFactories().getDateFactory().create();

        Path versionPath = path(historyPath, name(NODE_ENCODER.encode(now.getString())));
        AbstractJcrProperty predecessorsProp = node.getProperty(JcrLexicon.PREDECESSORS);

        systemBatch.create(versionPath)
                   .with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.VERSION)
                   .and(JcrLexicon.CREATED, now)
                   .and(JcrLexicon.UUID, versionUuid)
                   .and(predecessorsProp.property())
                   .and();
        Path frozenVersionPath = path(versionPath, JcrLexicon.FROZEN_NODE);
        systemBatch.create(frozenVersionPath)
                   .with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.FROZEN_NODE)
                   .and(JcrLexicon.FROZEN_UUID, jcrUuid)
                   .and(JcrLexicon.FROZEN_PRIMARY_TYPE, primaryTypeName)
                   .and(JcrLexicon.FROZEN_MIXIN_TYPES, mixinTypeNames)
                   .and(versionedPropertiesFor(node))
                   .and();
        int onParentVersion = node.getDefinition().getOnParentVersion();
        for (NodeIterator childNodes = node.getNodes(); childNodes.hasNext();) {
            AbstractJcrNode childNode = (AbstractJcrNode)childNodes.nextNode();
            versionNodeAt(childNode, frozenVersionPath, systemBatch, onParentVersion);
        }

        PropertyFactory propFactory = propertyFactory();

        for (Object ob : predecessorsProp.property()) {
            UUID predUuid = uuid(ob);

            org.modeshape.graph.property.Property successorsProp = systemGraph.getNodeAt(predUuid)
                                                                              .getProperty(JcrLexicon.SUCCESSORS);

            List<Object> newSuccessors = new LinkedList<Object>();
            boolean alreadySuccessor = false;
            if (successorsProp != null) {
                for (Object successor : successorsProp) {
                    newSuccessors.add(successor);
                    if (uuid(successor).equals(predUuid)) alreadySuccessor = true;
                }
            }

            if (!alreadySuccessor) {
                newSuccessors.add(versionUuid);

                org.modeshape.graph.property.Property newSuccessorsProp = propFactory.create(JcrLexicon.SUCCESSORS,
                                                                                             newSuccessors.toArray());
                systemBatch.set(newSuccessorsProp).on(predUuid).and();
            }
        }

        systemBatch.execute();
        historyNode.refresh(false);

        AbstractJcrNode newVersion = cache().findJcrNode(Location.create(versionUuid));

        NodeEditor editor = node.editor();
        editor.setProperty(JcrLexicon.PREDECESSORS,
                           node.valuesFrom(PropertyType.REFERENCE, EMPTY_OBJECT_ARRAY),
                           PropertyType.REFERENCE,
                           false);
        editor.setProperty(JcrLexicon.BASE_VERSION, node.valueFrom(newVersion), false, false);
        editor.setProperty(JcrLexicon.IS_CHECKED_OUT, node.valueFrom(PropertyType.BOOLEAN, false), false, false);
        node.save();

        return (JcrVersionNode)newVersion;
    }

    /**
     * Create a version record for the given node under the given parent path with the given batch.
     * 
     * @param node the node for which the frozen version record should be created
     * @param verisonedParentPath the parent for the frozen version record for this node
     * @param batch the batch with which the frozen version should be created
     * @param onParentVersionAction the {@link OnParentVersionAction} of the node whose {@link #checkin} resulted in this node
     *        being versioned
     * @throws RepositoryException if an error occurs accessing the repository
     */
    @SuppressWarnings( "fallthrough" )
    private void versionNodeAt( AbstractJcrNode node,
                                Path verisonedParentPath,
                                Graph.Batch batch,
                                int onParentVersionAction ) throws RepositoryException {
        Path childPath = path(verisonedParentPath, node.path().getLastSegment());

        Name primaryTypeName = node.getPrimaryTypeName();
        List<Name> mixinTypeNames = node.getMixinTypeNames();
        UUID uuid = UUID.randomUUID();
        if (node.isReferenceable()) uuid = node.uuid();

        switch (onParentVersionAction) {
            case OnParentVersionAction.ABORT:
                throw new VersionException(JcrI18n.cannotCheckinNodeWithAbortChildNode.text(node.getName(), node.getParent()
                                                                                                                .getName()));
            case OnParentVersionAction.VERSION:
                if (node.isNodeType(JcrMixLexicon.VERSIONABLE)) {
                    JcrVersionHistoryNode history = node.getVersionHistory();
                    UUID historyUuid = history.uuid();
                    batch.create(childPath)
                         .with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.VERSIONED_CHILD)
                         .with(JcrLexicon.CHILD_VERSION_HISTORY, historyUuid)
                         .and();

                    break;
                }

                // Otherwise, treat it as a copy, as per 8.2.11.2 in the 1.0.1 Spec
            case OnParentVersionAction.COPY:
                batch.create(childPath)
                     .with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.FROZEN_NODE)
                     .and(JcrLexicon.FROZEN_PRIMARY_TYPE, primaryTypeName)
                     .and(JcrLexicon.FROZEN_MIXIN_TYPES, mixinTypeNames)
                     .and(JcrLexicon.FROZEN_UUID, uuid)
                     .and(versionedPropertiesFor(node))
                     .and();
                break;
            case OnParentVersionAction.INITIALIZE:
            case OnParentVersionAction.COMPUTE:
            case OnParentVersionAction.IGNORE:
                // Do nothing for these. No built-in types require initialize or compute for child nodes.
                return;
            default:
                throw new IllegalStateException("Unexpected value: " + onParentVersionAction);
        }

        for (NodeIterator childNodes = node.getNodes(); childNodes.hasNext();) {
            AbstractJcrNode childNode = (AbstractJcrNode)childNodes.nextNode();
            versionNodeAt(childNode, childPath, batch, onParentVersionAction);
        }

    }

    /**
     * @param node the node for which the properties should be versioned
     * @return the versioned properties for {@code node} (i.e., the properties to add the the frozen version of {@code node}
     * @throws RepositoryException if an error occurs accessing the repository
     */
    private Collection<org.modeshape.graph.property.Property> versionedPropertiesFor( AbstractJcrNode node )
        throws RepositoryException {

        Collection<org.modeshape.graph.property.Property> props = new LinkedList<org.modeshape.graph.property.Property>();

        // Have to add this directly as it's not returned by AbstractJcrNode.getProperties
        AbstractJcrProperty multiValuedProperties = node.getProperty(ModeShapeIntLexicon.MULTI_VALUED_PROPERTIES);
        if (multiValuedProperties != null) props.add(multiValuedProperties.property());

        for (PropertyIterator iter = node.getProperties(); iter.hasNext();) {
            AbstractJcrProperty property = (AbstractJcrProperty)iter.nextProperty();

            org.modeshape.graph.property.Property prop = property.property();
            PropertyDefinitionId propDefnId = property.propertyInfo().getPayload().getPropertyDefinitionId();
            JcrPropertyDefinition propDefn = cache().nodeTypes().getPropertyDefinition(propDefnId);

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

        return props;
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
        session.checkLive();
        checkVersionable(node);

        // Check this separately since it throws a different type of exception
        if (node.isLocked() && !node.holdsLock()) {
            throw new LockException(JcrI18n.lockTokenNotHeld.text(node.getPath()));
        }

        /*
         * This happens when we've added mix:versionable, but not saved it to create the base
         * version (and the rest of the version storage graph).  See MODE-704.
         */
        if (!node.hasProperty(JcrLexicon.BASE_VERSION)) {
            return;
        }

        // Checking out an already checked-out node is supposed to return silently
        if (node.getProperty(JcrLexicon.IS_CHECKED_OUT).getBoolean()) {
            return;
        }

        PropertyFactory propFactory = propertyFactory();

        PropertyInfo<JcrPropertyPayload> mvProp = node.nodeInfo().getProperty(ModeShapeIntLexicon.MULTI_VALUED_PROPERTIES);
        org.modeshape.graph.property.Property multiValuedProps = mvProp != null ? mvProp.getProperty() : null;

        if (multiValuedProps == null) {
            multiValuedProps = propFactory.create(ModeShapeIntLexicon.MULTI_VALUED_PROPERTIES, JcrLexicon.PREDECESSORS);
        } else if (!Arrays.<Object>asList(multiValuedProps.getValues()).contains(JcrLexicon.PREDECESSORS)) {
            List<Object> values = new LinkedList<Object>();

            for (Object value : multiValuedProps) {
                values.add(value);
            }

            values.add(JcrLexicon.PREDECESSORS);
            multiValuedProps = propFactory.create(ModeShapeIntLexicon.MULTI_VALUED_PROPERTIES, values);
        }

        ValueFactory<Reference> refFactory = context().getValueFactories().getReferenceFactory();
        Object[] oldPreds = EMPTY_OBJECT_ARRAY;

        AbstractJcrProperty oldPredsProperty = node.getBaseVersion().getProperty(JcrLexicon.PREDECESSORS);
        if (oldPredsProperty != null) {
            oldPreds = oldPredsProperty.property().getValuesAsArray();
        }

        Object[] newPreds = new Object[oldPreds.length + 1];
        newPreds[0] = refFactory.create(node.getBaseVersion().uuid());
        System.arraycopy(oldPreds, 0, newPreds, 1, oldPreds.length);

        org.modeshape.graph.property.Property isCheckedOut = propFactory.create(JcrLexicon.IS_CHECKED_OUT, true);
        org.modeshape.graph.property.Property predecessors = propFactory.create(JcrLexicon.PREDECESSORS, newPreds);

        Graph graph = workspace().graph();
        Location location = Location.create(node.uuid());
        graph.set(isCheckedOut, predecessors, multiValuedProps).on(location).and();

        cache().refreshProperties(location);
    }

    /**
     * See {@link javax.jcr.Workspace#restore(Version[], boolean)} for details of this operation.
     * 
     * @param versions the versions to be restored
     * @param removeExisting if UUID conflicts resulting from this restore should cause the conflicting node to be removed or an
     *        exception to be thrown and the operation to fail
     * @throws RepositoryException if an error occurs accessing the repository
     * @see javax.jcr.Workspace#restore(Version[], boolean)
     */
    @Override
    public void restore( Version[] versions,
                         boolean removeExisting ) throws RepositoryException {
        session.checkLive();
        if (session.hasPendingChanges()) {
            throw new InvalidItemStateException(JcrI18n.noPendingChangesAllowed.text());
        }

        Map<Version, AbstractJcrNode> existingVersions = new HashMap<Version, AbstractJcrNode>(versions.length);
        Set<Path> versionRootPaths = new HashSet<Path>(versions.length);
        List<Version> nonExistingVersions = new ArrayList<Version>(versions.length);

        for (int i = 0; i < versions.length; i++) {
            VersionHistory history = versions[i].getContainingHistory();

            if (history.getRootVersion().isSame(versions[i])) {
                throw new VersionException(JcrI18n.cannotRestoreRootVersion.text(versions[i].getPath()));
            }

            try {
                AbstractJcrNode existingNode = session.getNodeByIdentifier(history.getVersionableIdentifier());
                existingVersions.put(versions[i], existingNode);
                versionRootPaths.add(existingNode.path());
            } catch (ItemNotFoundException infe) {
                nonExistingVersions.add(versions[i]);
            }
        }

        if (existingVersions.isEmpty()) {
            throw new VersionException(JcrI18n.noExistingVersionForRestore.text());
        }

        RestoreCommand op = new RestoreCommand(existingVersions, versionRootPaths, nonExistingVersions, null, removeExisting);
        op.execute();

        session.save();
    }

    /**
     * Restores the given version to the given path.
     * 
     * @param path the path at which the version should be restored; may not be null
     * @param version the version to restore; may not be null
     * @param labelToRestore the label that was used to identify the version; may be null
     * @param removeExisting if UUID conflicts resulting from this restore should cause the conflicting node to be removed or an
     *        exception to be thrown and the operation to fail
     * @throws RepositoryException if an error occurs accessing the repository
     * @see javax.jcr.Node#restore(Version, String, boolean)
     * @see javax.jcr.Node#restoreByLabel(String, boolean)
     */
    void restore( Path path,
                  Version version,
                  String labelToRestore,
                  boolean removeExisting ) throws RepositoryException {
        session.checkLive();

        if (session().hasPendingChanges()) {
            throw new InvalidItemStateException(JcrI18n.noPendingChangesAllowed.text());
        }

        // Ensure that the parent node exists - this will throw a PNFE if no node exists at that path
        AbstractJcrNode parentNode = cache().findJcrNode(null, path.getParent());
        AbstractJcrNode existingNode = null;
        AbstractJcrNode nodeToCheckLock;

        JcrVersionNode jcrVersion = (JcrVersionNode)version;

        try {
            existingNode = cache().findJcrNode(null, path);
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

        } catch (ItemNotFoundException pnfe) {
            // This is allowable, but the node needs to be checked out
            if (!parentNode.isCheckedOut()) {
                String parentPath = path.getString(context().getNamespaceRegistry());
                throw new VersionException(JcrI18n.nodeIsCheckedIn.text(parentPath));
            }

            AbstractJcrNode sourceNode = frozenNodeFor(version);
            Name primaryTypeName = name(sourceNode.getProperty(JcrLexicon.FROZEN_PRIMARY_TYPE).property().getFirstValue());
            AbstractJcrProperty uuidProp = sourceNode.getProperty(JcrLexicon.FROZEN_UUID);
            UUID desiredUuid = uuid(uuidProp.property().getFirstValue());

            existingNode = parentNode.editor().createChild(path.getLastSegment().getName(), desiredUuid, primaryTypeName);

            nodeToCheckLock = parentNode;
        }

        if (nodeToCheckLock.isLocked() && !nodeToCheckLock.holdsLock()) {
            throw new LockException(JcrI18n.lockTokenNotHeld.text(nodeToCheckLock.getPath()));
        }

        RestoreCommand op = new RestoreCommand(Collections.singletonMap(version, existingNode),
                                               Collections.singleton(existingNode.path()), Collections.<Version>emptySet(),
                                               labelToRestore, removeExisting);
        op.execute();

        NodeEditor editor = existingNode.editor();
        editor.setProperty(JcrLexicon.IS_CHECKED_OUT, existingNode.valueFrom(PropertyType.BOOLEAN, false), false, false);
        editor.setProperty(JcrLexicon.BASE_VERSION, existingNode.valueFrom(jcrVersion), false, false);

        session().save();

    }

    /**
     * @param version the version for which the frozen node should be returned
     * @return the frozen node for the given version
     * @throws RepositoryException if an error occurs accessing the repository
     */
    AbstractJcrNode frozenNodeFor( Version version ) throws RepositoryException {
        return ((AbstractJcrNode)version).getNode(JcrLexicon.FROZEN_NODE);
    }

    void doneMerge( AbstractJcrNode targetNode,
                    Version version ) throws RepositoryException {
        session.checkLive();
        checkVersionable(targetNode);

        if (targetNode.isNew() || targetNode.isModified()) {
            throw new InvalidItemStateException(JcrI18n.noPendingChangesAllowedForNode.text());
        }

        if (!targetNode.isNodeType(JcrMixLexicon.VERSIONABLE)) {
            throw new VersionException(JcrI18n.requiresVersionable.text());
        }

        AbstractJcrProperty prop = targetNode.getProperty(JcrLexicon.PREDECESSORS);

        JcrValue[] values = (JcrValue[])prop.getValues();
        JcrValue[] newValues = new JcrValue[values.length + 1];
        System.arraycopy(values, 0, newValues, 0, values.length);
        newValues[values.length] = targetNode.valueFrom(version);

        targetNode.editor().setProperty(JcrLexicon.PREDECESSORS, newValues, PropertyType.REFERENCE, false);

        removeVersionFromMergeFailedProperty(targetNode, version);

        targetNode.save();
    }

    void cancelMerge( AbstractJcrNode targetNode,
                      Version version ) throws RepositoryException {
        session.checkLive();
        checkVersionable(targetNode);

        if (targetNode.isNew() || targetNode.isModified()) {
            throw new InvalidItemStateException(JcrI18n.noPendingChangesAllowedForNode.text());
        }

        if (!targetNode.isNodeType(JcrMixLexicon.VERSIONABLE)) {
            throw new UnsupportedRepositoryOperationException(JcrI18n.requiresVersionable.text());
        }

        removeVersionFromMergeFailedProperty(targetNode, version);

        targetNode.save();
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

    NodeIterator merge( AbstractJcrNode targetNode,
                        String srcWorkspace,
                        boolean bestEffort,
                        boolean isShallow ) throws RepositoryException {
        session.checkLive();

        if (session().hasPendingChanges()) {
            throw new InvalidItemStateException(JcrI18n.noPendingChangesAllowed.text());
        }

        try {
            targetNode.correspondingNodePath(srcWorkspace);
        } catch (ItemNotFoundException infe) {
            // return immediately if no corresponding node exists in that workspace
            return new JcrChildNodeIterator(Collections.<AbstractJcrNode>emptySet(), 0);
        }

        JcrSession sourceSession = session().with(srcWorkspace);
        MergeCommand op = new MergeCommand(targetNode, sourceSession, bestEffort, isShallow);
        op.execute();

        session.save();

        return op.getFailures();
    }

    /**
     * Restores the given property onto the node managed by the given editor
     * 
     * @param property the property to restore; may not be null
     * @param editor the {@link NodeEditor editor} for the node that is to be modified; may not be null
     * @throws RepositoryException if an error occurs while accessing the repository or setting the property
     */
    void restoreProperty( AbstractJcrProperty property,
                          NodeEditor editor ) throws RepositoryException {
        Name propName = property.name();
        editor.removeProperty(propName);

        if (property.isMultiple()) {
            JcrValue[] values = (JcrValue[])property.getValues();
            editor.setProperty(propName, values, property.getType(), false);
        } else {
            JcrValue value = (JcrValue)property.getValue();
            editor.setProperty(propName, value, false, false);
        }
    }

    void initializeVersionHistoryFor( AbstractJcrNode node ) throws RepositoryException {
        initializeVersionHistoryFor(node, null);
    }

    void initializeVersionHistoryFor( AbstractJcrNode node,
                                      UUID originalVersionUuid ) throws RepositoryException {
        Batch batch = session().createBatch();

        initializeVersionHistoryFor(batch, node.nodeInfo(), originalVersionUuid, true);

        batch.execute();
    }

    void initializeVersionHistoryFor( Graph.Batch batch,
                                      Node<JcrNodePayload, JcrPropertyPayload> node,
                                      UUID originalVersionUuid,
                                      boolean forceWrite ) throws RepositoryException {

        if (!cache().isVersionable(node)) return;

        /*
         * Determine if the node has already had its version history initialized based on whether the protected property
         * jcr:isCheckedOut exists.
         */

        boolean initialized = node.getProperty(JcrLexicon.IS_CHECKED_OUT) != null;
        if (!forceWrite && initialized) return;

        UUID historyUuid = UUID.randomUUID();
        UUID versionUuid = UUID.randomUUID();

        initializeVersionStorageFor(node, historyUuid, originalVersionUuid, versionUuid);

        ValueFactory<Reference> refFactory = context().getValueFactories().getReferenceFactory();
        org.modeshape.graph.property.Property isCheckedOut = propertyFactory().create(JcrLexicon.IS_CHECKED_OUT, true);
        org.modeshape.graph.property.Property versionHistory = propertyFactory().create(JcrLexicon.VERSION_HISTORY,
                                                                                        refFactory.create(historyUuid));
        org.modeshape.graph.property.Property baseVersion = propertyFactory().create(JcrLexicon.BASE_VERSION,
                                                                                     refFactory.create(versionUuid));
        org.modeshape.graph.property.Property predecessors = propertyFactory().create(JcrLexicon.PREDECESSORS,
                                                                                      new Object[] {refFactory.create(versionUuid)});

        // This batch will get executed as part of the save
        batch.set(isCheckedOut, versionHistory, baseVersion, predecessors).on(node.getPath()).and();

        // Refresh the version storage node ...
        Node<JcrNodePayload, JcrPropertyPayload> storageNode = cache().findNode(null, versionStoragePath);
        cache().refresh(storageNode.getNodeId(), versionStoragePath, false);
    }

    void initializeVersionStorageFor( Node<JcrNodePayload, JcrPropertyPayload> node,
                                      UUID historyUuid,
                                      UUID originalVersionUuid,
                                      UUID versionUuid ) {
        JcrNodePayload payload = node.getPayload();

        Graph systemGraph = session().repository().createSystemGraph(context());

        Name primaryTypeName = payload.getPrimaryTypeName();
        List<Name> mixinTypeNames = payload.getMixinTypeNames();

        PropertyInfo<JcrPropertyPayload> jcrUuidProp = node.getProperty(JcrLexicon.UUID);

        UUID jcrUuid = uuid(jcrUuidProp.getProperty().getFirstValue());
        Path historyPath = versionHistoryPathFor(jcrUuid);
        Batch systemBatch = systemGraph.batch();

        // Determine if there are any intermediate paths to where this history node is to be ...
        Path[] intermediatePaths = versionHistoryPathsTo(historyPath);
        if (intermediatePaths != null) {
            // Create any intermediate nodes, if absent ...
            for (Path intermediatePath : intermediatePaths) {
                systemBatch.create(intermediatePath)
                           .with(JcrLexicon.PRIMARY_TYPE, ModeShapeLexicon.VERSION_HISTORY_FOLDER)
                           .ifAbsent()
                           .and();
            }
        }

        systemBatch.create(historyPath)
                   .with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.VERSION_HISTORY)
                   .and(JcrLexicon.VERSIONABLE_UUID, jcrUuid)
                   .and(JcrLexicon.UUID, historyUuid)
                   .ifAbsent()
                   .and();

        if (originalVersionUuid != null) {
            systemBatch.set(JcrLexicon.COPIED_FROM).on(historyPath).to(originalVersionUuid).and();
        }

        Path versionLabelsPath = path(historyPath, JcrLexicon.VERSION_LABELS);
        systemBatch.create(versionLabelsPath).with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.VERSION_LABELS).and();

        Path rootVersionPath = path(historyPath, JcrLexicon.ROOT_VERSION);
        DateTime now = context().getValueFactories().getDateFactory().create();
        systemBatch.create(rootVersionPath)
                   .with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.VERSION)
                   .and(JcrLexicon.CREATED, now)
                   .and(JcrLexicon.UUID, versionUuid)
                   .ifAbsent()
                   .and();

        Path frozenVersionPath = path(rootVersionPath, JcrLexicon.FROZEN_NODE);
        systemBatch.create(frozenVersionPath)
                   .with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.FROZEN_NODE)
                   .and(JcrLexicon.FROZEN_UUID, jcrUuid)
                   .and(JcrLexicon.FROZEN_PRIMARY_TYPE, primaryTypeName)
                   .and(JcrLexicon.FROZEN_MIXIN_TYPES, mixinTypeNames)
                   .ifAbsent()
                   .and();

        systemBatch.execute();
    }

    @NotThreadSafe
    private class RestoreCommand {

        private Map<Version, AbstractJcrNode> existingVersions;
        private Set<Path> versionRootPaths;
        private Collection<Version> nonExistingVersions;
        private boolean removeExisting;
        private String labelToRestore;
        private Map<AbstractJcrNode, AbstractJcrNode> changedNodes;

        public RestoreCommand( Map<Version, AbstractJcrNode> existingVersions,
                               Set<Path> versionRootPaths,
                               Collection<Version> nonExistingVersions,
                               String labelToRestore,
                               boolean removeExisting ) {
            super();
            this.existingVersions = existingVersions;
            this.versionRootPaths = versionRootPaths;
            this.nonExistingVersions = nonExistingVersions;
            this.removeExisting = removeExisting;
            this.labelToRestore = labelToRestore;

            // The default size for a HashMap is pretty low and this could get big fast
            this.changedNodes = new HashMap<AbstractJcrNode, AbstractJcrNode>(100);
        }

        void execute() throws RepositoryException {
            Collection<Version> versionsToCheck = new ArrayList<Version>(existingVersions.keySet());
            for (Version version : versionsToCheck) {
                AbstractJcrNode root = existingVersions.get(version);
                // This can happen if the version was already restored in another node
                if (root == null) continue;

                // This updates the changedNodes and nonExistingVersions fields as a side effect
                restoreNodeMixins(frozenNodeFor(version), root);
                restoreNode(frozenNodeFor(version), root, dateTime(version.getCreated()));
                clearCheckoutStatus(frozenNodeFor(version), root);
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
         * Restores the child nodes and mixin types for {@code targetNode} based on the frozen version stored at {@code
         * sourceNode}. This method will remove and add child nodes as necessary based on the documentation in the JCR 2.0
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

            NodeEditor targetEditor = targetNode.editor();
            Node<JcrNodePayload, JcrPropertyPayload> targetNodeInfo = targetNode.nodeInfo();
            Node<JcrNodePayload, JcrPropertyPayload> sourceNodeInfo = sourceNode.nodeInfo();

            Set<Node<JcrNodePayload, JcrPropertyPayload>> versionedChildrenThatShouldNotBeRestored = new HashSet<Node<JcrNodePayload, JcrPropertyPayload>>();

            // Try to match the existing nodes with nodes from the version to be restored
            Map<Node<JcrNodePayload, JcrPropertyPayload>, Node<JcrNodePayload, JcrPropertyPayload>> presentInBoth = new HashMap<Node<JcrNodePayload, JcrPropertyPayload>, Node<JcrNodePayload, JcrPropertyPayload>>();

            // Start with all target children in this set and pull them out as matches are found
            List<Node<JcrNodePayload, JcrPropertyPayload>> inTargetOnly = copyOf(targetNodeInfo.getChildren(),
                                                                                 targetNodeInfo.getChildrenCount());

            // Start with no source children in this set, but add them in when no match is found
            Map<Node<JcrNodePayload, JcrPropertyPayload>, Node<JcrNodePayload, JcrPropertyPayload>> inSourceOnly = new HashMap<Node<JcrNodePayload, JcrPropertyPayload>, Node<JcrNodePayload, JcrPropertyPayload>>();

            // Map the source children to existing target children where possible
            for (Node<JcrNodePayload, JcrPropertyPayload> sourceChild : sourceNodeInfo.getChildren()) {
                boolean isVersionedChild = JcrNtLexicon.VERSIONED_CHILD.equals(name(sourceChild.getProperty(JcrLexicon.PRIMARY_TYPE)
                                                                                               .getProperty()
                                                                                               .getFirstValue()));
                Node<JcrNodePayload, JcrPropertyPayload> resolvedNode = resolveSourceNode(sourceChild, checkinTime);
                Node<JcrNodePayload, JcrPropertyPayload> match = findMatchFor(resolvedNode);

                if (match != null) {
                    if (isVersionedChild) {
                        if (!removeExisting) {
                            Object rawUuid = match.getProperty(JcrLexicon.UUID).getProperty().getFirstValue();
                            String uuid = rawUuid == null ? null : rawUuid.toString();
                            throw new ItemExistsException(JcrI18n.itemAlreadyExistsWithUuid.text(uuid,
                                                                                                 workspace().getName(),
                                                                                                 match.getPath()));
                        }
                        // use match directly
                        versionedChildrenThatShouldNotBeRestored.add(match);
                    }
                    inTargetOnly.remove(match);
                    presentInBoth.put(sourceChild, match);

                } else {
                    inSourceOnly.put(sourceChild, resolvedNode);
                }
            }

            // Remove all the extraneous children of the target node
            for (Node<JcrNodePayload, JcrPropertyPayload> targetChild : inTargetOnly) {
                switch (nodeDefinitionFor(targetChild).getOnParentVersion()) {
                    case OnParentVersionAction.COPY:
                    case OnParentVersionAction.ABORT:
                    case OnParentVersionAction.VERSION:
                        targetEditor.destroyChild(targetChild);
                        break;

                    case OnParentVersionAction.COMPUTE:
                        // Technically, this should reinitialize the node per its defaults.
                    case OnParentVersionAction.INITIALIZE:
                    case OnParentVersionAction.IGNORE:
                        // Do nothing
                }
            }

            LinkedList<Node<JcrNodePayload, JcrPropertyPayload>> reversedChildren = new LinkedList<Node<JcrNodePayload, JcrPropertyPayload>>();
            for (Node<JcrNodePayload, JcrPropertyPayload> sourceChild : sourceNodeInfo.getChildren()) {
                reversedChildren.addFirst(sourceChild);
            }

            // Now walk through the source node children (in reversed order), inserting children as needed
            // The order is reversed because SessionCache$NodeEditor supports orderBefore, but not orderAfter
            Node<JcrNodePayload, JcrPropertyPayload> prevChild = null;
            for (Node<JcrNodePayload, JcrPropertyPayload> sourceChild : reversedChildren) {
                Node<JcrNodePayload, JcrPropertyPayload> targetChild = presentInBoth.get(sourceChild);
                Node<JcrNodePayload, JcrPropertyPayload> resolvedChild;

                AbstractJcrNode sourceChildNode;
                AbstractJcrNode targetChildNode;

                boolean shouldRestore = !versionedChildrenThatShouldNotBeRestored.contains(targetChild);

                if (targetChild != null) {
                    // Reorder if necessary
                    resolvedChild = resolveSourceNode(sourceChild, checkinTime);

                    sourceChildNode = cache().findJcrNode(resolvedChild.getNodeId(), resolvedChild.getPath());
                    targetChildNode = cache().findJcrNode(targetChild.getNodeId(), targetChild.getPath());

                } else {
                    // Pull the resolved node
                    resolvedChild = inSourceOnly.get(sourceChild);
                    sourceChildNode = cache().findJcrNode(resolvedChild.getNodeId(), resolvedChild.getPath());

                    Name primaryTypeName = name(resolvedChild.getProperty(JcrLexicon.FROZEN_PRIMARY_TYPE)
                                                             .getProperty()
                                                             .getFirstValue());
                    PropertyInfo<JcrPropertyPayload> uuidProp = resolvedChild.getProperty(JcrLexicon.FROZEN_UUID);
                    UUID desiredUuid = uuid(uuidProp.getProperty().getFirstValue());

                    targetChildNode = targetEditor.createChild(sourceChild.getName(), desiredUuid, primaryTypeName);

                    assert shouldRestore == true;
                }

                if (shouldRestore) {
                    // Have to do this first, as the properties below only exist for mix:versionable nodes
                    restoreNodeMixins(sourceChildNode, targetChildNode);

                    if (sourceChildNode.getParent().isNodeType(JcrNtLexicon.VERSION)) {
                        clearCheckoutStatus(sourceChildNode, targetChildNode);
                    }
                    restoreNode(sourceChildNode, targetChildNode, checkinTime);
                }

                orderBefore(sourceChild, prevChild, targetEditor);
                prevChild = sourceChild;
            }
        }

        private void clearCheckoutStatus( AbstractJcrNode sourceChildNode,
                                          AbstractJcrNode targetChildNode ) throws RepositoryException {

            NodeEditor editor = targetChildNode.editor();
            editor.setProperty(JcrLexicon.IS_CHECKED_OUT, targetChildNode.valueFrom(PropertyType.BOOLEAN, false), false, false);
            editor.setProperty(JcrLexicon.BASE_VERSION, targetChildNode.valueFrom(sourceChildNode.getParent()), false, false);

        }

        /**
         * Moves {@code targetNode} immediately before {@code beforeNode} under their shared parent. This version is very
         * inefficient in that it always tries to move the node, regardless of whether a move is actually required.
         * <p>
         * The key postcondition for this method is that {@code targetNode} must be the last "versioned" child node before {@code
         * beforeNode}, although {@code targetNode} need not be the immediate predecessor of {@code beforeNode} if all intervening
         * nodes are not "versioned". That is, there can be nodes between {@code targetNode} and {@code beforeNode} as long as
         * these nodes all have a {@link NodeDefinition node definition} with an {@link NodeDefinition#getOnParentVersion()
         * onParentVersionAction} of IGNORE, COMPUTE, or INITIALIZE.
         * </p>
         * 
         * @param targetNode the node to be reordered; may not be null
         * @param beforeNode the node that must succeed {@code targetNode}; null indicates that {@code targetNode} comes last in
         *        the list of "versionable" child nodes
         * @param parentEditor the {@link NodeEditor editor} for the parent node
         * @throws RepositoryException if an error occurs while accessing the repository
         */
        private void orderBefore( Node<JcrNodePayload, JcrPropertyPayload> targetNode,
                                  Node<JcrNodePayload, JcrPropertyPayload> beforeNode,
                                  NodeEditor parentEditor ) throws RepositoryException {
            Segment beforeSegment = beforeNode == null ? null : beforeNode.getSegment();

            parentEditor.orderChildBefore(targetNode.getSegment(), beforeSegment);

        }

        /**
         * Adds any missing mixin types from the source node to the target node
         * 
         * @param sourceNode the frozen source node; may not be be null
         * @param targetNode the target node; may not be null
         * @throws RepositoryException if an error occurs while accessing the repository or adding the mixin types
         */
        private void restoreNodeMixins( AbstractJcrNode sourceNode,
                                        AbstractJcrNode targetNode ) throws RepositoryException {
            AbstractJcrProperty mixinTypesProp = sourceNode.getProperty(JcrLexicon.FROZEN_MIXIN_TYPES);
            NodeEditor childEditor = targetNode.editor();
            Object[] mixinTypeNames = mixinTypesProp == null ? EMPTY_OBJECT_ARRAY : mixinTypesProp.property().getValuesAsArray();

            Collection<Name> currentMixinTypes = new HashSet<Name>(targetNode.getMixinTypeNames());

            for (int i = 0; i < mixinTypeNames.length; i++) {
                Name mixinTypeName = name(mixinTypeNames[i]);

                if (!currentMixinTypes.remove(mixinTypeName)) {
                    JcrNodeType mixinType = session().nodeTypeManager().getNodeType(mixinTypeName);
                    childEditor.addMixin(mixinType);
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
            NodeEditor childEditor = targetNode.editor();
            Map<Name, PropertyInfo<JcrPropertyPayload>> sourcePropertyNames = new HashMap<Name, PropertyInfo<JcrPropertyPayload>>();
            for (PropertyInfo<JcrPropertyPayload> propInfo : sourceNode.nodeInfo().getProperties()) {
                if (!IGNORED_PROP_NAMES_FOR_RESTORE.contains(propInfo.getName())) {
                    sourcePropertyNames.put(propInfo.getName(), propInfo);
                }
            }

            Collection<PropertyInfo<JcrPropertyPayload>> targetProps = new ArrayList<PropertyInfo<JcrPropertyPayload>>(
                                                                                                                       targetNode.nodeInfo()
                                                                                                                                 .getProperties());
            for (PropertyInfo<JcrPropertyPayload> propInfo : targetProps) {
                Name propName = propInfo.getName();

                if (sourcePropertyNames.containsKey(propName)) {
                    // Overwrite the current property with the property from the version
                    restoreProperty(sourcePropertyNames.get(propName).getPayload().getJcrProperty(), childEditor);
                    sourcePropertyNames.remove(propName);
                } else {
                    PropertyDefinitionId propDefnId = propInfo.getPayload().getPropertyDefinitionId();
                    PropertyDefinition propDefn = session().nodeTypeManager().getPropertyDefinition(propDefnId);

                    switch (propDefn.getOnParentVersion()) {
                        case OnParentVersionAction.COPY:
                        case OnParentVersionAction.ABORT:
                        case OnParentVersionAction.VERSION:
                            childEditor.removeProperty(propName);
                            break;

                        case OnParentVersionAction.COMPUTE:
                        case OnParentVersionAction.INITIALIZE:
                        case OnParentVersionAction.IGNORE:
                            // Do nothing
                    }
                }
            }

            for (Map.Entry<Name, PropertyInfo<JcrPropertyPayload>> sourceProperty : sourcePropertyNames.entrySet()) {
                restoreProperty(sourceProperty.getValue().getPayload().getJcrProperty(), childEditor);
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
         * @return the frozen node that corresponds to the give source node; may not be null
         * @throws RepositoryException if an error occurs while accessing the repository
         * @see #closestMatchFor(JcrVersionHistoryNode, DateTime)
         */
        private Node<JcrNodePayload, JcrPropertyPayload> resolveSourceNode( Node<JcrNodePayload, JcrPropertyPayload> sourceNode,
                                                                            DateTime checkinTime ) throws RepositoryException {
            Name sourcePrimaryTypeName = name(sourceNode.getProperty(JcrLexicon.PRIMARY_TYPE).getProperty().getFirstValue());

            if (JcrNtLexicon.FROZEN_NODE.equals(sourcePrimaryTypeName)) return sourceNode;
            assert JcrNtLexicon.VERSIONED_CHILD.equals(sourcePrimaryTypeName);

            // Must be a versioned child - try to see if it's one of the versions we're restoring
            PropertyInfo<JcrPropertyPayload> historyUuidProp = sourceNode.getProperty(JcrLexicon.CHILD_VERSION_HISTORY);
            UUID uuid = uuid(historyUuidProp.getProperty().getFirstValue());
            assert uuid != null;
            String uuidString = uuid.toString();

            /*
             * First try to find a match among the rootless versions in this restore operation
             */
            for (Version version : nonExistingVersions) {
                if (uuidString.equals(version.getContainingHistory().getIdentifier())) {
                    JcrVersionNode versionNode = (JcrVersionNode)version;
                    nonExistingVersions.remove(version);
                    return versionNode.getFrozenNode().nodeInfo();
                }
            }

            /*
             * Then check the rooted versions in this restore operation
             */
            for (Version version : existingVersions.keySet()) {
                if (uuidString.equals(version.getContainingHistory().getIdentifier())) {
                    JcrVersionNode versionNode = (JcrVersionNode)version;
                    existingVersions.remove(version);
                    return versionNode.getFrozenNode().nodeInfo();
                }
            }

            /*
             * If there was a label for this restore operation, try to match that way
             */
            JcrVersionHistoryNode versionHistory = (JcrVersionHistoryNode)cache().findJcrNode(Location.create(uuid));

            if (labelToRestore != null) {
                try {
                    JcrVersionNode versionNode = versionHistory.getVersionByLabel(labelToRestore);
                    return versionNode.getFrozenNode().nodeInfo();
                } catch (VersionException noVersionWithThatLabel) {
                    // This can happen if there's no version with that label - valid
                }
            }

            /*
             * If all else fails, find the last version checked in before the checkin time for the version being restored
             */
            AbstractJcrNode match = closestMatchFor(versionHistory, checkinTime);

            return match.nodeInfo();
        }

        /**
         * Finds a node that has the same UUID as is specified in the jcr:frozenUuid property of {@code sourceNode}. If a match
         * exists and it is a descendant of one of the {@link #versionRootPaths root paths} for this restore operation, it is
         * returned. If a match exists but is not a descendant of one of the root paths for this restore operation, either an
         * exception is thrown (if {@link #removeExisting} is false) or the match is deleted and null is returned (if
         * {@link #removeExisting} is true).
         * 
         * @param sourceNode the node for which the match should be checked; may not be null
         * @return the existing node with the same UUID as is specified in the jcr:frozenUuid property of {@code sourceNode}; null
         *         if no node exists with that UUID
         * @throws ItemExistsException if {@link #removeExisting} is false and the node is not a descendant of any of the
         *         {@link #versionRootPaths root paths} for this restore command
         * @throws RepositoryException if any other error occurs while accessing the repository
         */
        private Node<JcrNodePayload, JcrPropertyPayload> findMatchFor( Node<JcrNodePayload, JcrPropertyPayload> sourceNode )
            throws ItemExistsException, RepositoryException {

            PropertyInfo<JcrPropertyPayload> uuidProp = sourceNode.getProperty(JcrLexicon.FROZEN_UUID);
            UUID sourceUuid = uuid(uuidProp.getProperty().getFirstValue());

            try {
                AbstractJcrNode match = cache().findJcrNode(Location.create(sourceUuid));

                if (nodeIsOutsideRestoredForest(match)) return null;

                return match.nodeInfo();
            } catch (ItemNotFoundException infe) {
                return null;
            }
        }

        /**
         * Copies the given {@link Iterable} into a {@link List} and returns that list.
         * 
         * @param rawElements the iterator containing the items that should be copied; may not be null
         * @param size the number of elements in the iterator; may not be negative, but inaccurate values will lead to a badly
         *        sized list
         * @return a list containing the same elements as {@code rawElements} in the same order; never null
         */
        private List<Node<JcrNodePayload, JcrPropertyPayload>> copyOf( Iterable<Node<JcrNodePayload, JcrPropertyPayload>> rawElements,
                                                                       int size ) {
            List<Node<JcrNodePayload, JcrPropertyPayload>> newList = new ArrayList<Node<JcrNodePayload, JcrPropertyPayload>>(size);
            for (Node<JcrNodePayload, JcrPropertyPayload> node : rawElements) {
                newList.add(node);
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
                throw new ItemExistsException(JcrI18n.itemAlreadyExistsWithUuid.text(node.uuid(),
                                                                                     workspace().getName(),
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
            DateTimeFactory dateFactory = context().getValueFactories().getDateFactory();

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
        private final String workspaceName;

        public MergeCommand( AbstractJcrNode targetNode,
                             JcrSession sourceSession,
                             boolean bestEffort,
                             boolean isShallow ) {
            super();
            this.targetNode = targetNode;
            this.sourceSession = sourceSession;
            this.bestEffort = bestEffort;
            this.isShallow = isShallow;

            this.workspaceName = sourceSession.getWorkspace().getName();
            this.failures = new LinkedList<AbstractJcrNode>();
        }

        final JcrChildNodeIterator getFailures() {
            return new JcrChildNodeIterator(failures, failures.size());
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
                sourceNode = sourceSession.getNode(sourcePath);
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

            if (targetVersion.isSuccessorOf(sourceVersion) || targetVersion.uuid().equals(sourceVersion.uuid())) {
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

            NodeEditor targetEditor = targetNode.editor();
            if (targetNode.hasProperty(JcrLexicon.MERGE_FAILED)) {
                JcrValue[] existingValues = (JcrValue[])targetNode.getProperty(JcrLexicon.MERGE_FAILED).getValues();

                boolean found = false;
                String sourceUuidString = sourceVersion.uuid().toString();
                for (int i = 0; i < existingValues.length; i++) {
                    if (sourceUuidString.equals(existingValues[i].getString())) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    JcrValue[] newValues = new JcrValue[existingValues.length + 1];
                    System.arraycopy(existingValues, 0, newValues, 0, existingValues.length);
                    newValues[newValues.length - 1] = targetNode.valueFrom(sourceVersion);
                    targetEditor.setProperty(JcrLexicon.MERGE_FAILED, newValues, PropertyType.REFERENCE, false);
                }

            } else {
                targetEditor.setProperty(JcrLexicon.MERGE_FAILED, targetNode.valueFrom(sourceVersion), false, false);
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
            NodeEditor childEditor = targetNode.editor();
            Map<Name, PropertyInfo<JcrPropertyPayload>> sourcePropertyNames = new HashMap<Name, PropertyInfo<JcrPropertyPayload>>();
            for (PropertyInfo<JcrPropertyPayload> propInfo : sourceNode.nodeInfo().getProperties()) {
                if (!IGNORED_PROP_NAMES_FOR_RESTORE.contains(propInfo.getName())) {
                    sourcePropertyNames.put(propInfo.getName(), propInfo);
                }
            }

            Collection<PropertyInfo<JcrPropertyPayload>> targetProps = new ArrayList<PropertyInfo<JcrPropertyPayload>>(
                                                                                                                       targetNode.nodeInfo()
                                                                                                                                 .getProperties());
            for (PropertyInfo<JcrPropertyPayload> propInfo : targetProps) {
                Name propName = propInfo.getName();

                if (sourcePropertyNames.containsKey(propName)) {
                    // Overwrite the current property with the property from the version
                    restoreProperty(sourcePropertyNames.get(propName).getPayload().getJcrProperty(), childEditor);
                    sourcePropertyNames.remove(propName);
                } else {
                    PropertyDefinitionId propDefnId = propInfo.getPayload().getPropertyDefinitionId();
                    PropertyDefinition propDefn = session().nodeTypeManager().getPropertyDefinition(propDefnId);

                    switch (propDefn.getOnParentVersion()) {
                        case OnParentVersionAction.COPY:
                        case OnParentVersionAction.ABORT:
                        case OnParentVersionAction.VERSION:
                            childEditor.removeProperty(propName);
                            break;

                        case OnParentVersionAction.COMPUTE:
                        case OnParentVersionAction.INITIALIZE:
                        case OnParentVersionAction.IGNORE:
                            // Do nothing
                    }
                }
            }

            for (Map.Entry<Name, PropertyInfo<JcrPropertyPayload>> sourceProperty : sourcePropertyNames.entrySet()) {
                restoreProperty(sourceProperty.getValue().getPayload().getJcrProperty(), childEditor);
            }
        }

    }

    @Override
    public void cancelMerge( String absPath,
                             Version version ) throws VersionException, InvalidItemStateException, RepositoryException {
        cancelMerge(session.getNode(absPath), version);
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
    public Version checkin( String absPath )
        throws VersionException, InvalidItemStateException, LockException, RepositoryException {
        return checkin(session.getNode(absPath));
    }

    @Override
    public void checkout( String absPath ) throws LockException, RepositoryException {
        checkout(session.getNode(absPath));
    }

    @Override
    public Version checkpoint( String absPath )
        throws VersionException, InvalidItemStateException, LockException, RepositoryException {
        Version version = checkin(absPath);
        checkout(absPath);
        return version;
    }

    @Override
    public void doneMerge( String absPath,
                           Version version ) throws VersionException, InvalidItemStateException, RepositoryException {
        doneMerge(session.getNode(absPath), version);
    }

    @Override
    public javax.jcr.Node getActivity() throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    @Override
    public Version getBaseVersion( String absPath ) throws RepositoryException {
        return session.getNode(absPath).getBaseVersion();
    }

    @Override
    public VersionHistory getVersionHistory( String absPath ) throws UnsupportedRepositoryOperationException, RepositoryException {
        return session.getNode(absPath).getVersionHistory();
    }

    @Override
    public boolean isCheckedOut( String absPath ) throws RepositoryException {
        AbstractJcrNode node = session.getNode(absPath);
        return node.isCheckedOut();
    }

    @SuppressWarnings( "unused" )
    @Override
    public NodeIterator merge( javax.jcr.Node activityNode )
        throws VersionException, AccessDeniedException, MergeException, LockException, InvalidItemStateException,
        RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    @Override
    public NodeIterator merge( String absPath,
                               String srcWorkspace,
                               boolean bestEffort,
                               boolean isShallow )
        throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException,
        RepositoryException {
        CheckArg.isNotNull(srcWorkspace, "source workspace name");

        AbstractJcrNode node = session.getNode(absPath);

        return merge(node, srcWorkspace, bestEffort, isShallow);
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
    public void restore( String absPath,
                         String versionName,
                         boolean removeExisting )
        throws VersionException, ItemExistsException, LockException, InvalidItemStateException, RepositoryException {

        Version version = null;

        // See if the node at absPath exists and has version storage.
        Path path = session.pathFor(absPath, "absPath");

        AbstractJcrNode existingNode = session.getNode(path);
        VersionHistory historyNode = existingNode.getVersionHistory();
        if (historyNode != null) {
            version = historyNode.getVersion(versionName);
        }

        assert version != null;

        // AbstractJcrNode versionStorage = session.getRootNode().getNode(JcrLexicon.SYSTEM).getNode(JcrLexicon.VERSION_STORAGE);
        // assert versionStorage != null;

        restore(path, version, null, removeExisting);
    }

    @Override
    public void restore( String absPath,
                         Version version,
                         boolean removeExisting )
        throws PathNotFoundException, ItemExistsException, VersionException, ConstraintViolationException, LockException,
        InvalidItemStateException, RepositoryException {
        Path path = session.pathFor(absPath, "absPath");

        restore(path, version, null, removeExisting);
    }

    @Override
    public void restore( Version version,
                         boolean removeExisting )
        throws VersionException, ItemExistsException, InvalidItemStateException, LockException, RepositoryException {
        AbstractJcrNode node = session.getNodeByIdentifier(version.getContainingHistory().getVersionableIdentifier());
        Path path = node.path();

        restore(path, version, null, removeExisting);
    }

    @Override
    public void restoreByLabel( String absPath,
                                String versionLabel,
                                boolean removeExisting )
        throws VersionException, ItemExistsException, LockException, InvalidItemStateException, RepositoryException {
        session.getNode(absPath).restoreByLabel(versionLabel, removeExisting);
    }

    @Override
    public javax.jcr.Node setActivity( javax.jcr.Node activity )
        throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    @SuppressWarnings( "unused" )
    @Override
    public void removeActivity( javax.jcr.Node activityNode )
        throws UnsupportedRepositoryOperationException, VersionException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    @Override
    public javax.jcr.Node createActivity( String title ) throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    @Override
    public javax.jcr.Node createConfiguration( String absPath )
        throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    protected static interface PathAlgorithm {
        Path versionHistoryPathFor( UUID uuid );
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
        public Path versionHistoryPathFor( UUID uuid ) {
            String uuidStr = uuid.toString();
            Name p1 = names.create(uuidStr.substring(0, 2));
            Name p2 = names.create(uuidStr.substring(2, 4));
            Name p3 = names.create(uuidStr.substring(4, 6));
            Name p4 = names.create(uuidStr.substring(6, 8));
            Name p5 = names.create(uuidStr.substring(9));
            return paths.createAbsolutePath(JcrLexicon.SYSTEM, JcrLexicon.VERSION_STORAGE, p1, p2, p3, p4, p5);
        }
    }

    protected static class FlatPathAlgorithm extends BasePathAlgorithm {
        protected FlatPathAlgorithm( Path versionStoragePath,
                                     ExecutionContext context ) {
            super(versionStoragePath, context);
        }

        @Override
        public Path versionHistoryPathFor( UUID uuid ) {
            return paths.createAbsolutePath(JcrLexicon.SYSTEM, JcrLexicon.VERSION_STORAGE, names.create(uuid.toString()));
        }
    }

}
