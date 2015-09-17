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
import java.security.AccessControlException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
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
import org.modeshape.jcr.api.index.IndexManager;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.MutableCachedNode;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.RepositoryCache;
import org.modeshape.jcr.cache.SessionCache;
import org.modeshape.jcr.cache.document.WritableSessionCache;
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

        // Do not allow to copy a subgraph into root
        if (destPath.isRoot() && !srcPath.isRoot()) {
            throw new RepositoryException(JcrI18n.cannotCopySubgraphIntoRoot.text(srcAbsPath, srcWorkspace, getName()));
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
                if (!existingDestNode.isRoot()) {
                    parentNode = existingDestNode.getParent();
                    newNodeName = existingDestNode.segment().getName();
                } else {
                    parentNode = existingDestNode;
                }
            } else {
                if (!destPath.isRoot()) {
                    parentNode = copySession.node(destPath.getParent());
                    newNodeName = destPath.getLastSegment().getName();
                } else {
                    parentNode = copySession.getRootNode();
                }
            }

            /*
             * Find the source node and check if it is locked
             */
            JcrSession sourceSession = session.spawnSession(srcWorkspace, true);
            AbstractJcrNode sourceNode = sourceSession.node(srcPath);
            JcrLockManager lockManager = session.lockManager();
            javax.jcr.lock.Lock lock = lockManager.getLockIfExists(sourceNode);
            if (lock != null && !lock.isLockOwningSession()) {
                throw new LockException(srcAbsPath);
            }

            /**
             * Perform validations if external nodes are present.
             */
            validateCopyForExternalNode(sourceNode, parentNode);

            NodeTypes nodeTypes = repository().nodeTypeManager().getNodeTypes();
            if (nodeTypes.isUnorderedCollection(sourceNode.getPrimaryTypeName(), sourceNode.getMixinTypeNames())) {
                throw new ConstraintViolationException(JcrI18n.operationNotSupportedForUnorderedCollections.text("copy"));    
            }
            
            /*
            * Use the JCR add child here to perform the parent validations
            */
            AbstractJcrNode copy = newNodeName == null ? parentNode : parentNode.addChildNode(newNodeName,
                                                                                              sourceNode.getPrimaryTypeName(),
                                                                                              null, false, false);
            Map<NodeKey, NodeKey> nodeKeyCorrespondence = copy.mutable().deepCopy(copySession.cache(), sourceNode.node(),
                                                                                  sourceSession.cache(),
                                                                                  repository().systemWorkspaceKey(),
                                                                                  repository().runningState().connectors());
            /**
             * Do some extra processing for each copied node
             */
            copySession.initOriginalVersionKeys();
            Set<NodeKey> srcNodeKeys = nodeKeyCorrespondence.keySet();
            for (NodeKey sourceKey : srcNodeKeys) {
                AbstractJcrNode srcNode = sourceSession.node(sourceKey, null);
                if (srcNode.isNodeType(JcrMixLexicon.VERSIONABLE)) {
                    NodeKey dstNodeKey = nodeKeyCorrespondence.get(sourceKey);

                    // For the nodes which were versionable, set the mappings for the original version
                    copySession.setOriginalVersionKey(dstNodeKey, srcNode.getBaseVersion().key());
                }
                
                // if we copied nodes which have ACLs we need to make sure this is reflected in the overall ACL count
                Map<String, Set<String>> permissions = srcNode.node().getPermissions(copySession.cache());
                if (permissions != null && !permissions.isEmpty()) {
                    copySession.aclAdded(permissions.size());
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

    private void validateCopyForExternalNode( AbstractJcrNode sourceNode,
                                              AbstractJcrNode destParentNode ) throws RepositoryException {
        String rootSourceKey = session.getRootNode().key().getSourceKey();

        NodeKey parentKey = destParentNode.key();
        if (parentKey.getSourceKey().equalsIgnoreCase(rootSourceKey)) {
            return;
        }
        String destExternalKey = parentKey.getSourceKey();
        Connectors connectors = repository().runningState().connectors();
        String destSourceName = connectors.getSourceNameAtKey(destExternalKey);

        Set<NodeKey> sourceKeys = session.cache().getNodeKeysAtAndBelow(sourceNode.key());
        boolean sourceContainsExternalNodes = false;
        for (NodeKey sourceKey : sourceKeys) {
            String sourceNodeSourceKey = sourceKey.getSourceKey();
            if (!rootSourceKey.equalsIgnoreCase(sourceNodeSourceKey)) {
                sourceContainsExternalNodes = true;
                if (!sourceNodeSourceKey.equalsIgnoreCase(destExternalKey)) {
                    String sourceExternalSourceName = connectors.getSourceNameAtKey(sourceNodeSourceKey);
                    throw new RepositoryException(JcrI18n.unableToCopySourceTargetMismatch.text(sourceExternalSourceName,
                                                                                                destSourceName));
                }
            }
        }

        String sourceNodeSourceKey = sourceNode.key().getSourceKey();
        if (sourceContainsExternalNodes && !sourceNodeSourceKey.equalsIgnoreCase(destExternalKey)) {
            // the source graph contains external nodes, but the source itself is not an external node
            throw new RepositoryException(JcrI18n.unableToCopySourceNotExternal.text(sourceNode.path()));
        }
    }

    @Override
    public void clone( String srcWorkspace,
                       String srcAbsPath,
                       String destAbsPath,
                       boolean removeExisting )
        throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException,
        PathNotFoundException, ItemExistsException, LockException, RepositoryException {
        internalClone(srcWorkspace, srcAbsPath, destAbsPath, removeExisting, false);
    }

    void internalClone( String srcWorkspace,
                        String srcAbsPath,
                        String destAbsPath,
                        boolean removeExisting,
                        boolean skipVersioningValidation )
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

        // Do not allow to clone a subgraph into root
        if (destPath.isRoot() && !srcPath.isRoot()) {
            throw new RepositoryException(JcrI18n.cannotCloneSubgraphIntoRoot.text(srcAbsPath, srcWorkspace, getName()));
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
                if (!existingDestNode.isRoot()) {
                    parentNode = existingDestNode.getParent();
                    newNodeName = existingDestNode.segment().getName();
                } else {
                    parentNode = existingDestNode;
                }
            } else {
                if (!destPath.isRoot()) {
                    parentNode = cloneSession.node(destPath.getParent());
                    newNodeName = destPath.getLastSegment().getName();
                } else {
                    parentNode = cloneSession.getRootNode();
                }
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
            JcrLockManager lockManager = session.lockManager();
            javax.jcr.lock.Lock lock = lockManager.getLockIfExists(sourceNode);
            if (lock != null && !lock.isLockOwningSession()) {
                throw new LockException(srcAbsPath);
            }

            validateCloneForExternalNodes(sameWorkspace, sourceSession, sourceNode, parentNode);
            
            NodeTypes nodeTypes = repository().nodeTypeManager().getNodeTypes();
            if (nodeTypes.isUnorderedCollection(sourceNode.getPrimaryTypeName(), sourceNode.getMixinTypeNames())) {
                throw new ConstraintViolationException(JcrI18n.operationNotSupportedForUnorderedCollections.text("clone"));
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
                sourceKeys = filterNodeKeysForClone(sourceKeys, sourceCache);

                for (NodeKey srcKey : sourceKeys) {
                    try {
                        // use the current session to try and load each cloneSessionNode. If we find such a cloneSessionNode in
                        // the current session,
                        // we need to perform some checks
                        AbstractJcrNode srcNode = sourceSession.node(srcKey, null);
                        
                        // look if the source node has any ACLs and if yes, reflect that in the clone session's ACL count
                        Map<String, Set<String>> permissions = srcNode.node().getPermissions(sourceCache);
                        if (permissions != null && !permissions.isEmpty()) {
                            cloneSession.aclAdded(permissions.size());
                        }
                        boolean isExternal = !srcKey.getSourceKey().equalsIgnoreCase(sourceCache.getRootKey().getSourceKey());
                        if (isExternal && session.nodeExists(srcKey) && !removeExisting) {
                            throw new ItemExistsException(JcrI18n.itemAlreadyExistsWithUuid.text(srcKey, workspaceName,
                                                                                                 srcNode.getPath()));
                        }
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

                        boolean hasAnyPendingChanges = !session.cache().getChangedNodeKeysAtOrBelow(cloneSessionNode.node())
                                                               .isEmpty();
                        if (hasAnyPendingChanges) {
                            throw new RepositoryException(
                                                          JcrI18n.cannotRemoveNodeFromCloneDueToChangesInSession.text(cloneSessionNode.getPath(),
                                                                                                                      cloneSessionNode.getIdentifier()));
                        }

                        if (!removeExisting) {
                            throw new ItemExistsException(JcrI18n.itemAlreadyExistsWithUuid.text(srcKey, workspaceName,
                                                                                                 cloneSessionNode.getPath()));
                        }
                        // if the clone node which we're about to remove has any ACLs, we need to reflect the fact that we're
                        // removing this node in ACL count
                        Map<String, Set<String>> existingPermissions = cloneSessionNode.node().getPermissions(cloneSessionNode.cache());
                        if (existingPermissions != null && !existingPermissions.isEmpty()) {
                            cloneSession.aclRemoved(existingPermissions.size());
                        }
                        cloneSessionNode.remove();
                    } catch (PathNotFoundException e) {
                        // means we don't have a node with the same path
                    }
                }

                NodeKey cloneKey = null;
                if (!destPath.isRoot()) {
                    // Use the JCR add child here to perform the parent validations
                    cloneKey = parentNode.key().withId(sourceNode.key().getIdentifier());
                    parentNode.addChildNode(newNodeName, sourceNode.getPrimaryTypeName(), cloneKey, skipVersioningValidation,
                                            false);
                } else {
                    cloneKey = parentNode.key();
                }
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

    private void validateCloneForExternalNodes( boolean sameWorkspace,
                                                JcrSession sourceSession,
                                                AbstractJcrNode sourceNode,
                                                AbstractJcrNode parentNode ) throws RepositoryException {

        String rootSourceKey = sourceSession.getRootNode().key().getSourceKey();
        Connectors connectors = repository().runningState().connectors();

        Set<NodeKey> sourceKeys = sourceSession.cache().getNodeKeysAtAndBelow(sourceNode.key());
        for (NodeKey sourceKey : sourceKeys) {
            String sourceNodeSourceKey = sourceKey.getSourceKey();
            if (!sourceNodeSourceKey.equalsIgnoreCase(rootSourceKey)) {
                String sourceNameAtKey = connectors.getSourceNameAtKey(sourceNodeSourceKey);
                if (sameWorkspace) {
                    throw new RepositoryException(JcrI18n.unableToCloneSameWsContainsExternalNode.text(sourceNameAtKey));
                } else if (!sourceNode.isRoot() || !parentNode.isRoot()) {
                    throw new RepositoryException(JcrI18n.unableToCloneExternalNodesRequireRoot.text(sourceNameAtKey));
                }
            }
        }
    }

    private Set<NodeKey> filterNodeKeysForClone( Set<NodeKey> sourceKeys,
                                                 SessionCache sourceCache ) {
        Set<NodeKey> filteredSet = new HashSet<NodeKey>();
        for (NodeKey sourceKey : sourceKeys) {
            if (sourceKey.equals(sourceCache.getRootKey())
                || sourceKey.getWorkspaceKey().equalsIgnoreCase(repository().systemWorkspaceKey())) {
                continue;
            }
            filteredSet.add(sourceKey);
        }
        return filteredSet;
    }

    protected void validateCrossWorkspaceAction( String srcWorkspace ) throws RepositoryException {
        CheckArg.isNotEmpty(srcWorkspace, "srcWorkspace");

        session.checkLive();
        session.checkWorkspacePermission(srcWorkspace, ModeShapePermissions.READ);
        session.checkWorkspacePermission(getName(), ModeShapePermissions.READ);

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
        JcrSession moveSession = this.session.spawnSession(false);
        moveSession.move(srcAbsPath, destAbsPath);
        moveSession.save();
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
    public IndexManager getIndexManager() throws RepositoryException {
        session.checkLive();
        return repository().queryManager().getIndexManager();
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
        if (observationManager == null && session.isLive()) {
            try {
                lock.lock();
                if (observationManager == null) {
                    observationManager = new JcrObservationManager(session, repository().changeBus());
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
                session.checkWorkspacePermission(iter.next(), ModeShapePermissions.READ);
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
            session.checkWorkspacePermission(name, ModeShapePermissions.CREATE_WORKSPACE);
            JcrRepository repository = session.repository();
            if (repository.hasWorkspace(name)) {
                // TCK: cannot create a workspace with the same name as an already existing one
                String msg = GraphI18n.workspaceAlreadyExistsInRepository.text(name, getName());
                throw new RepositoryException(msg);
            }
            repository.repositoryCache().createWorkspace(name);

            // import any initial content
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

        deepClone(srcWorkspaceSession, srcWorkspaceSession.getRootNode().key(), newWorkspaceSession,
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
        mutableCloneNode.deepClone(cloneCache, sourceNode, sourceCache, repository().systemWorkspaceKey(),
                                   repository().runningState().connectors());

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
        session.checkWorkspacePermission(name, ModeShapePermissions.DELETE_WORKSPACE);
        // start an internal remove session which needs to act as a unit for the entire operation
        JcrSession removeSession = session.spawnSession(name, false);
        JcrRepository repository = session.repository();
        RepositoryCache repositoryCache = repository.repositoryCache();
        NodeKey systemKey = repositoryCache.getSystemKey();
        try {
            JcrRootNode rootNode = removeSession.getRootNode();

            // first remove all the nodes via JCR, because we need validations to be performed
            for (NodeIterator nodeIterator = rootNode.getNodesInternal(); nodeIterator.hasNext();) {
                AbstractJcrNode child = (AbstractJcrNode)nodeIterator.nextNode();
                if (child.key().equals(systemKey)) {
                    // we don't remove the jcr:system node here, we just unlink it via the cache
                    continue;
                }
                child.remove();
            }

            // then remove the workspace itself and unlink the system content. This method will create & save the session cache
            // therefore, we don't need to call removeSession.save() from here.
            if (!repositoryCache.destroyWorkspace(name, (WritableSessionCache)removeSession.cache())) {
                throw new NoSuchWorkspaceException(JcrI18n.workspaceNotFound.text(name, getName()));
            }
        } catch (UnsupportedOperationException e) {
            throw new UnsupportedRepositoryOperationException(e.getMessage());
        } finally {
            removeSession.logout();
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
    public void reindexSince( long timestamp ) throws RepositoryException {
        // First check permissions ...
        session.checkPermission(workspaceName, Path.ROOT_PATH, ModeShapePermissions.INDEX_WORKSPACE);
        
        // then if the journal is available
        JcrRepository.RunningState runningState = repository().runningState();
        if (runningState.journal() == null) {
            throw new RepositoryException(JcrI18n.cannotReindexJournalNotEnabled.text(timestamp, repository().getName()));           
        }
        
        runningState.queryManager().reindexSince(this, timestamp);
    }

    @Override
    public Future<Boolean> reindexSinceAsync( long timestamp ) throws RepositoryException {
        // First check permissions ...
        session.checkPermission(workspaceName, Path.ROOT_PATH, ModeShapePermissions.INDEX_WORKSPACE);

        // then if the journal is available
        JcrRepository.RunningState runningState = repository().runningState();
        if (runningState.journal() == null) {
            throw new RepositoryException(JcrI18n.cannotReindexJournalNotEnabled.text(timestamp, repository().getName()));
        }

        return runningState.queryManager().reindexSinceAsync(this, timestamp);
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
                    federationManager = new ModeShapeFederationManager(session, session.repository().runningState()
                                                                                       .documentStore());
                }
            } finally {
                lock.unlock();
            }
        }
        return federationManager;
    }

}
