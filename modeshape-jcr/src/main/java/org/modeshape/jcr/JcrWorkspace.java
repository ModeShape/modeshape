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

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.observation.ObservationManager;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.JcrContentHandler.EnclosingSAXException;
import org.modeshape.jcr.api.federation.FederationManager;
import org.modeshape.jcr.api.monitor.ValueMetric;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.MutableCachedNode;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.SessionCache;
import org.modeshape.jcr.value.InvalidPathException;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.PathFactory;
import org.modeshape.jcr.value.ValueFormatException;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessControlException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The ModeShape implementation of the {@link Workspace JCR Workspace}. This implementation is pretty lightweight, and only
 * instantiates the various components when needed.
 */
@ThreadSafe
class JcrWorkspace implements org.modeshape.jcr.api.Workspace {

    private final JcrSession session;
    private final String workspaceName;
    private final Lock lock = new ReentrantLock();
    private JcrNodeTypeManager nodeTypeManager;
    private JcrLockManager lockManager;
    private JcrNamespaceRegistry workspaceRegistry;
    private JcrVersionManager versionManager;
    private JcrQueryManager queryManager;
    private JcrObservationManager observationManager;
    private JcrRepositoryManager repositoryManager;
    private ModeShapeFederationManager federationManager;

    JcrWorkspace( JcrSession session,
                  String workspaceName ) {
        this.session = session;
        this.workspaceName = workspaceName;
    }

    final JcrRepository repository() {
        return session.repository();
    }

    final ExecutionContext context() {
        return session.context();
    }

    @Override
    public final JcrSession getSession() {
        return session;
    }

    @Override
    public final String getName() {
        return workspaceName;
    }

    @Override
    public void copy( String srcAbsPath,
                      String destAbsPath )
        throws ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException,
        LockException, RepositoryException {
        CheckArg.isNotEmpty(srcAbsPath, "srcAbsPath");
        CheckArg.isNotEmpty(destAbsPath, "destAbsPath");

        session.checkLive();

        this.copy(this.workspaceName, srcAbsPath, destAbsPath);
    }

    @Override
    public void copy( String srcWorkspace,
                      String srcAbsPath,
                      String destAbsPath )
        throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException,
        PathNotFoundException, ItemExistsException, LockException, RepositoryException {
        CheckArg.isNotEmpty(srcAbsPath, "srcAbsPath");
        CheckArg.isNotEmpty(destAbsPath, "destAbsPath");

        validateCrossWorkspaceAction(srcWorkspace);

        // Create the paths ...
        PathFactory pathFactory = session.pathFactory();
        Path srcPath = null;
        try {
            srcPath = pathFactory.create(srcAbsPath);
        } catch (ValueFormatException e) {
            throw new PathNotFoundException(JcrI18n.invalidPathParameter.text(srcAbsPath, "srcAbsPath"), e);
        }

        Path destPath = null;
        try {
            destPath = pathFactory.create(destAbsPath);
        } catch (ValueFormatException e) {
            throw new PathNotFoundException(JcrI18n.invalidPathParameter.text(destAbsPath, "destAbsPath"), e);
        }

        // Doing a literal test here because the path pathFactory will canonicalize "/node[1]" to "/node"
        if (!destPath.isIdentifier() && destAbsPath.endsWith("]")) {
            throw new RepositoryException(JcrI18n.pathCannotHaveSameNameSiblingIndex.text(destAbsPath));
        }

        try {
            // create an inner session for copying
            JcrSession copySession = session.spawnSession(false);

            // Use the session to verify that the node location has a definition and is valid with the new cloned child.
            // This also performs the check permission for reading the parent ...
            AbstractJcrNode parentNode = null;
            Name newNodeName = null;
            if (destPath.isIdentifier()) {
                AbstractJcrNode existingDestNode = copySession.node(destPath);
                parentNode = existingDestNode.getParent();
                newNodeName = existingDestNode.segment().getName();
            } else {
                parentNode = copySession.node(destPath.getParent());
                newNodeName = destPath.getLastSegment().getName();
            }

            /*
             * Find the source node and check if it is locked
             */
            JcrSession sourceSession = session.spawnSession(srcWorkspace, true);
            AbstractJcrNode sourceNode = sourceSession.node(srcPath);
            if (session.lockManager().isLocked(sourceNode)
                && !session.lockManager().hasLockToken(sourceNode.getLock().getLockToken())) {
                throw new LockException(srcAbsPath);
            }

            /*
            * Use the JCR add child here to perform the parent validations
            */
            AbstractJcrNode copy = parentNode.addChildNode(newNodeName, sourceNode.getPrimaryTypeName(), null);
            Map<NodeKey, NodeKey> nodeKeyCorrespondence = copy.mutable().deepCopy(copySession.cache(),
                                                                                  sourceNode.node(),
                                                                                  sourceSession.cache());

            /**
             * Do some extra processing for each copied node
             */
            copySession.initOriginalVersionKeys();
            Set<NodeKey> srcNodeKeys = nodeKeyCorrespondence.keySet();
            for (NodeKey sourceKey : srcNodeKeys) {
                AbstractJcrNode srcNode = sourceSession.node(sourceKey, null);
                NodeKey dstNodeKey = nodeKeyCorrespondence.get(sourceKey);
                AbstractJcrNode dstNode = copySession.node(dstNodeKey, null);

                if (srcNode.isNodeType(JcrMixLexicon.VERSIONABLE)) {
                    // For the nodes which were versionable, set the mappings for the original version
                    copySession.setOriginalVersionKey(dstNodeKey, srcNode.getBaseVersion().key());
                }

                if (dstNode.isNodeType(JcrMixLexicon.REFERENCEABLE) && dstNode.hasProperty(JcrLexicon.UUID)) {
                    // for referenceable nodes, update the UUID to be be same as the new identifier
                    JcrValue identifierValue = dstNode.valueFactory().createValue(dstNode.getIdentifier());
                    dstNode.setProperty(JcrLexicon.UUID, identifierValue, true, true);

                    // if there are any incoming references within the copied subgraph, they need to point to the new nodes
                    for (PropertyIterator incomingReferencesIterator = dstNode.getAllReferences(); incomingReferencesIterator.hasNext();) {
                        Property incomingRef = incomingReferencesIterator.nextProperty();
                        NodeKey referringNodeKey = ((AbstractJcrNode)incomingRef.getParent()).key();
                        boolean isReferrerWithinSubgraph = srcNodeKeys.contains(referringNodeKey);
                        if (isReferrerWithinSubgraph) {
                            incomingRef.setValue(copySession.node(dstNodeKey, null));
                        }
                    }
                }
            }

            // save the copy session
            copySession.save();

        } catch (ItemNotFoundException e) {
            // The destination path was not found ...
            throw new PathNotFoundException(e.getLocalizedMessage(), e);
        } catch (AccessControlException ace) {
            throw new AccessDeniedException(ace);
        } catch (InvalidPathException e) {
            throw new RepositoryException(e.getLocalizedMessage(), e);
        }
    }

    @Override
    public void clone( String srcWorkspace,
                       String srcAbsPath,
                       String destAbsPath,
                       boolean removeExisting )
        throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException,
        PathNotFoundException, ItemExistsException, LockException, RepositoryException {
        CheckArg.isNotEmpty(srcAbsPath, "srcAbsPath");
        CheckArg.isNotEmpty(destAbsPath, "destAbsPath");

        validateCrossWorkspaceAction(srcWorkspace);

        final boolean sameWorkspace = getName().equals(srcWorkspace);
        if (sameWorkspace && removeExisting) {
            // This is a special case of cloning within the same workspace but removing the original, which equates to a move ...
            move(srcAbsPath, destAbsPath);
            return;
        }

        // Create the paths ...
        PathFactory pathFactory = session.pathFactory();
        Path srcPath = null;
        try {
            srcPath = pathFactory.create(srcAbsPath);
        } catch (ValueFormatException e) {
            throw new PathNotFoundException(JcrI18n.invalidPathParameter.text(srcAbsPath, "srcAbsPath"), e);
        }

        Path destPath = null;
        try {
            destPath = pathFactory.create(destAbsPath);
        } catch (ValueFormatException e) {
            throw new PathNotFoundException(JcrI18n.invalidPathParameter.text(destAbsPath, "destAbsPath"), e);
        }

        // Doing a literal test here because the path pathFactory will canonicalize "/node[1]" to "/node"
        if (!sameWorkspace && !destPath.isIdentifier() && destAbsPath.endsWith("]")) {
            throw new RepositoryException(JcrI18n.pathCannotHaveSameNameSiblingIndex.text(destAbsPath));
        }

        try {
            // create an inner session for cloning
            JcrSession cloneSession = session.spawnSession(false);

            // Use the session to verify that the node location has a definition and is valid with the new cloned child.
            // This also performs the check permission for reading the parent ...
            AbstractJcrNode parentNode = null;
            Name newNodeName = null;
            if (destPath.isIdentifier()) {
                AbstractJcrNode existingDestNode = cloneSession.node(destPath);
                parentNode = existingDestNode.getParent();
                newNodeName = existingDestNode.segment().getName();
            } else {
                parentNode = cloneSession.node(destPath.getParent());
                newNodeName = destPath.getLastSegment().getName();
            }

            /*
             * Find the source node and check if it is locked
             */
            JcrSession sourceSession = null;
            if (sameWorkspace) {
                sourceSession = cloneSession;
            } else {
                sourceSession = session.spawnSession(srcWorkspace, true);
            }
            AbstractJcrNode sourceNode = sourceSession.node(srcPath);
            if (session.lockManager().isLocked(sourceNode)
                && !session.lockManager().hasLockToken(sourceNode.getLock().getLockToken())) {
                throw new LockException(srcAbsPath);
            }

            if (sameWorkspace && sourceNode.isShareable()) {
                // cloning in the same workspace should produce a shareable node
                assert !removeExisting;

                // Check that we're not creating the shared node below the shareable node ...
                if (destPath.isAtOrBelow(srcPath)) {
                    throw new RepositoryException(JcrI18n.unableToShareNodeWithinSubgraph.text(srcAbsPath, destAbsPath));
                }
                // And that we're not creating a share in a parent that already has the
                Path destParent = destPath.getParent();
                if (destParent.isSameAs(srcPath.getParent())) {
                    String msg = JcrI18n.unableToShareNodeWithinSameParent.text(srcAbsPath, destAbsPath, destParent);
                    throw new UnsupportedRepositoryOperationException(msg);
                }
                // We don't allow any more than 1 share under a given parent, for a shareable node
                JcrSharedNodeCache.SharedSet sharedSet = sourceNode.sharedSet();
                AbstractJcrNode existingShare = sharedSet.getSharedNodeAtOrBelow(destParent);
                if (existingShare != null) {
                    String msg = JcrI18n.shareAlreadyExistsWithinParent.text(destAbsPath, existingShare.getPath());
                    throw new RepositoryException(msg);
                }
                parentNode.addSharedNode(sourceNode, newNodeName);
                // save the changes in the clone session ...
                cloneSession.save();
            } else {
                // use the source session to load all the keys from the source subgraph
                SessionCache sourceCache = sourceSession.cache();
                Set<NodeKey> sourceKeys = sourceCache.getNodeKeysAtAndBelow(sourceNode.key());

                for (NodeKey srcKey : sourceKeys) {
                    try {
                        // use the current session to try and load each cloneSessionNode. If we find such a cloneSessionNode in
                        // the current session,
                        // we need to perform some checks
                        AbstractJcrNode srcNode = sourceSession.node(srcKey, null);
                        NodeKey cloneKey = parentNode.key().withId(srcNode.key().getIdentifier());
                        AbstractJcrNode cloneSessionNode = null;
                        try {
                            cloneSessionNode = cloneSession.node(cloneKey, null);
                        } catch (ItemNotFoundException e) {
                            // no node exists
                            continue;
                        }
                        if (cloneSessionNode.nodeDefinition().isMandatory()) {
                            throw new ConstraintViolationException(
                                                                   JcrI18n.cannotRemoveNodeFromClone.text(cloneSessionNode.getPath(),
                                                                                                          cloneSessionNode.getIdentifier()));
                        }

                        boolean hasAnyPendingChanges = !session.cache()
                                                               .getChangedNodeKeysAtOrBelow(cloneSessionNode.node())
                                                               .isEmpty();
                        if (hasAnyPendingChanges) {
                            throw new RepositoryException(
                                                          JcrI18n.cannotRemoveNodeFromCloneDueToChangesInSession.text(cloneSessionNode.getPath(),
                                                                                                                      cloneSessionNode.getIdentifier()));
                        }

                        if (!removeExisting) {
                            throw new ItemExistsException(JcrI18n.itemAlreadyExistsWithUuid.text(srcKey,
                                                                                                 workspaceName,
                                                                                                 cloneSessionNode.getPath()));
                        }

                        cloneSessionNode.remove();
                    } catch (PathNotFoundException e) {
                        // means we don't have a node with the same path
                    }
                }

                // Use the JCR add child here to perform the parent validations
                NodeKey cloneKey = parentNode.key().withId(sourceNode.key().getIdentifier());
                parentNode.addChildNode(newNodeName, sourceNode.getPrimaryTypeName(), cloneKey);

                deepClone(sourceSession, sourceNode.key(), cloneSession, cloneKey);
            }
        } catch (ItemNotFoundException e) {
            // The destination path was not found ...
            throw new PathNotFoundException(e.getLocalizedMessage(), e);
        } catch (AccessControlException ace) {
            throw new AccessDeniedException(ace);
        } catch (InvalidPathException e) {
            throw new RepositoryException(e.getLocalizedMessage(), e);
        }
    }

    protected void validateCrossWorkspaceAction( String srcWorkspace ) throws RepositoryException {
        CheckArg.isNotEmpty(srcWorkspace, "srcWorkspace");

        session.checkLive();
        session.checkPermission(srcWorkspace, null, ModeShapePermissions.READ);
        session.checkPermission(getName(), null, ModeShapePermissions.READ);

        JcrRepository repository = repository();
        if (!repository.hasWorkspace(srcWorkspace)) {
            throw new NoSuchWorkspaceException(JcrI18n.workspaceNameIsInvalid.text(repository.getName(), srcWorkspace));
        }
    }

    @Override
    public void move( String srcAbsPath,
                      String destAbsPath )
        throws ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException,
        LockException, RepositoryException {
        session.checkLive();

        CheckArg.isNotEmpty(srcAbsPath, "srcAbsPath");
        CheckArg.isNotEmpty(destAbsPath, "destAbsPath");

        // Create a new JCR session, perform the move, and then save the session ...
        JcrSession session = this.session.spawnSession(false);
        try {
            session.move(srcAbsPath, destAbsPath);
            session.save();
        } finally {
            session.logout();
        }
    }

    @Override
    public void restore( Version[] versions,
                         boolean removeExisting )
        throws ItemExistsException, UnsupportedRepositoryOperationException, VersionException, LockException,
        InvalidItemStateException, RepositoryException {
        session.checkLive();
        versionManager().restore(versions, removeExisting);
    }

    @Override
    public JcrLockManager getLockManager() throws UnsupportedRepositoryOperationException, RepositoryException {
        session.checkLive();
        return lockManager();
    }

    final JcrLockManager lockManager() {
        if (lockManager == null) {
            try {
                lock.lock();
                if (lockManager == null) lockManager = new JcrLockManager(session, repository().lockManager());
            } finally {
                lock.unlock();
            }
        }
        return lockManager;
    }

    @Override
    public JcrQueryManager getQueryManager() throws RepositoryException {
        session.checkLive();
        if (this.queryManager == null) {
            try {
                lock.lock();
                if (queryManager == null) queryManager = new JcrQueryManager(session);
            } finally {
                lock.unlock();
            }
        }
        return queryManager;
    }

    @Override
    public javax.jcr.NamespaceRegistry getNamespaceRegistry() throws RepositoryException {
        session.checkLive();
        if (workspaceRegistry == null) {
            try {
                lock.lock();
                if (workspaceRegistry == null) {
                    workspaceRegistry = new JcrNamespaceRegistry(repository().persistentRegistry(), this.session);
                }
            } finally {
                lock.unlock();
            }
        }
        return workspaceRegistry;
    }

    @Override
    public JcrNodeTypeManager getNodeTypeManager() throws RepositoryException {
        session.checkLive();
        return nodeTypeManager();
    }

    final JcrNodeTypeManager nodeTypeManager() {
        if (nodeTypeManager == null) {
            try {
                lock.lock();
                if (nodeTypeManager == null) {
                    nodeTypeManager = new JcrNodeTypeManager(session, repository().nodeTypeManager());
                }
            } finally {
                lock.unlock();
            }
        }
        return nodeTypeManager;
    }

    @Override
    public ObservationManager getObservationManager() throws UnsupportedRepositoryOperationException, RepositoryException {
        session.checkLive();
        return observationManager();
    }

    final JcrObservationManager observationManager() {
        if (observationManager == null) {
            try {
                lock.lock();
                if (observationManager == null) {
                    observationManager = new JcrObservationManager(session, repository().repositoryCache(),
                                                                   repository().getRepositoryStatistics());
                }
            } finally {
                lock.unlock();
            }
        }
        return observationManager;
    }

    @Override
    public JcrVersionManager getVersionManager() throws UnsupportedRepositoryOperationException, RepositoryException {
        session.checkLive();
        return versionManager();
    }

    final JcrVersionManager versionManager() {
        if (versionManager == null) {
            try {
                lock.lock();
                if (versionManager == null) versionManager = new JcrVersionManager(session);
            } finally {
                lock.unlock();
            }
        }
        return versionManager;
    }

    @Override
    public JcrRepositoryManager getRepositoryManager() throws AccessDeniedException, RepositoryException {
        session.checkLive();
        return repositoryManager();
    }

    final JcrRepositoryManager repositoryManager() {
        if (repositoryManager == null) {
            try {
                lock.lock();
                if (repositoryManager == null) repositoryManager = new JcrRepositoryManager(this);
            } finally {
                lock.unlock();
            }
        }
        return repositoryManager;
    }

    @Override
    public ContentHandler getImportContentHandler( String parentAbsPath,
                                                   int uuidBehavior )
        throws PathNotFoundException, ConstraintViolationException, VersionException, LockException, AccessDeniedException,
        RepositoryException {

        CheckArg.isNotNull(parentAbsPath, "parentAbsPath");
        session.checkLive();

        // Create a new session, since we don't want to mess with the current session and because we'll save right
        // when finished reading the document ...
        JcrSession session = this.session.spawnSession(false);
        boolean saveWhenFinished = true;

        // Find the parent path ...
        AbstractJcrNode parent = session.getNode(parentAbsPath);
        if (!parent.isCheckedOut()) {
            throw new VersionException(JcrI18n.nodeIsCheckedIn.text(parent.getPath()));
        }

        Repository repo = getSession().getRepository();
        boolean retainLifecycleInfo = repo.getDescriptorValue(Repository.OPTION_LIFECYCLE_SUPPORTED).getBoolean();
        boolean retainRetentionInfo = repo.getDescriptorValue(Repository.OPTION_RETENTION_SUPPORTED).getBoolean();
        return new JcrContentHandler(session, parent, uuidBehavior, saveWhenFinished, retainRetentionInfo, retainLifecycleInfo);
    }

    @Override
    public void importXML( String parentAbsPath,
                           InputStream in,
                           int uuidBehavior )
        throws IOException, VersionException, PathNotFoundException, ItemExistsException, ConstraintViolationException,
        InvalidSerializedDataException, LockException, AccessDeniedException, RepositoryException {
        CheckArg.isNotNull(parentAbsPath, "parentAbsPath");
        CheckArg.isNotNull(in, "in");
        session.checkLive();

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
    public String[] getAccessibleWorkspaceNames() throws RepositoryException {
        session.checkLive();
        // Make a copy, since the size may change before we iterate over it ...
        Set<String> names = new HashSet<String>(session.repository().repositoryCache().getWorkspaceNames());

        // Remove any workspaces for which we don't have read access ...
        for (Iterator<String> iter = names.iterator(); iter.hasNext();) {
            try {
                session.checkPermission(iter.next(), null, ModeShapePermissions.READ);
            } catch (AccessDeniedException ace) {
                iter.remove();
            }
        }

        return names.toArray(new String[names.size()]);
    }

    @Override
    public void createWorkspace( String name )
        throws AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
        session.checkLive();
        try {
            session.checkPermission(name, null, ModeShapePermissions.CREATE_WORKSPACE);
            JcrRepository repository = session.repository();
            if (repository.hasWorkspace(name)) {
                // TCK: cannot create a workspace with the same name as an already existing one
                String msg = GraphI18n.workspaceAlreadyExistsInRepository.text(name, getName());
                throw new RepositoryException(msg);
            }
            repository.repositoryCache().createWorkspace(name);
            repository.statistics().increment(ValueMetric.WORKSPACE_COUNT);

            //import any initial content
            repository.runningState().initialContentImporter().importInitialContent(name);
        } catch (UnsupportedOperationException e) {
            throw new UnsupportedRepositoryOperationException(e.getMessage());
        }
    }

    @Override
    public void createWorkspace( String name,
                                 String srcWorkspace )
        throws AccessDeniedException, UnsupportedRepositoryOperationException, NoSuchWorkspaceException, RepositoryException {
        validateCrossWorkspaceAction(srcWorkspace);
        createWorkspace(name);

        JcrSession newWorkspaceSession = session.spawnSession(name, false);
        JcrSession srcWorkspaceSession = session.spawnSession(srcWorkspace, true);

        deepClone(srcWorkspaceSession,
                  srcWorkspaceSession.getRootNode().key(),
                  newWorkspaceSession,
                  newWorkspaceSession.getRootNode().key());
    }

    protected void deepClone( JcrSession sourceSession,
                              NodeKey sourceNodeKey,
                              JcrSession cloneSession,
                              NodeKey cloneNodeKey ) throws RepositoryException {
        assert !cloneSession.cache().isReadOnly();

        SessionCache sourceCache = sourceSession.cache();
        CachedNode sourceNode = sourceCache.getNode(sourceNodeKey);

        SessionCache cloneCache = cloneSession.cache();
        MutableCachedNode mutableCloneNode = cloneSession.node(cloneNodeKey, null).mutable();

        /**
         * Perform the clone at the cache level - clone all properties & children
         */
        mutableCloneNode.deepClone(cloneCache, sourceNode, sourceCache);

        /**
         * Make sure the version history is preserved
         */
        cloneSession.initBaseVersionKeys();
        Set<NodeKey> sourceKeys = sourceCache.getNodeKeysAtAndBelow(sourceNodeKey);
        for (NodeKey sourceKey : sourceKeys) {
            AbstractJcrNode srcNode = sourceSession.node(sourceKey, null);
            if (srcNode.isNodeType(JcrMixLexicon.VERSIONABLE)) {
                // Preserve the base version of the versionable nodes (this will in turn preserve the version history)
                cloneSession.setDesiredBaseVersionKey(sourceKey, srcNode.getBaseVersion().key());
            }
        }
        cloneSession.save();
    }

    @Override
    public void deleteWorkspace( String name )
        throws AccessDeniedException, UnsupportedRepositoryOperationException, NoSuchWorkspaceException, RepositoryException {
        session.checkLive();
        try {
            JcrRepository repository = session.repository();
            if (!repository.repositoryCache().destroyWorkspace(name)) {
                throw new NoSuchWorkspaceException(JcrI18n.workspaceNotFound.text(name, getName()));
            }
            repository.statistics().decrement(ValueMetric.WORKSPACE_COUNT);
        } catch (UnsupportedOperationException e) {
            throw new UnsupportedRepositoryOperationException(e.getMessage());
        }
    }

    @Override
    public void reindex() throws RepositoryException {
        // First check permissions ...
        session.checkPermission(workspaceName, Path.ROOT_PATH, ModeShapePermissions.INDEX_WORKSPACE);
        // Then reindex ...
        repository().runningState().queryManager().reindexContent(this);
    }

    @Override
    public void reindex( String pathStr ) throws RepositoryException {
        try {
            // First check permissions ...
            Path path = session.pathFactory().create(pathStr);
            session.checkPermission(workspaceName, path, ModeShapePermissions.INDEX_WORKSPACE);
            // Then reindex ...
            repository().runningState().queryManager().reindexContent(this, path, Integer.MAX_VALUE);
        } catch (ValueFormatException e) {
            throw new RepositoryException(e.getMessage());
        }
    }

    @Override
    public Future<Boolean> reindexAsync() throws RepositoryException {
        // First check permissions ...
        session.checkPermission(workspaceName, Path.ROOT_PATH, ModeShapePermissions.INDEX_WORKSPACE);
        // Then reindex ...
        return repository().runningState().queryManager().reindexContentAsync(this);
    }

    @Override
    public Future<Boolean> reindexAsync( String pathStr ) throws RepositoryException {
        try {
            // First check permissions ...
            Path path = session.pathFactory().create(pathStr);
            session.checkPermission(workspaceName, path, ModeShapePermissions.INDEX_WORKSPACE);
            // Then reindex ...
            return repository().runningState().queryManager().reindexContentAsync(this, path, Integer.MAX_VALUE);
        } catch (ValueFormatException e) {
            throw new RepositoryException(e.getMessage());
        }
    }

    @Override
    public FederationManager getFederationManager() throws RepositoryException {
        session.checkLive();
        return federationManager();
    }

    final ModeShapeFederationManager federationManager() {
        if (federationManager == null) {
            try {
                lock.lock();
                if (federationManager == null) {
                    federationManager = new ModeShapeFederationManager(session);
                }
            } finally {
                lock.unlock();
            }
        }
        return federationManager;
    }

}
