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
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Workspace;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.lock.LockManager;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.observation.ObservationManager;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionManager;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Location;
import org.modeshape.graph.Subgraph;
import org.modeshape.graph.SubgraphNode;
import org.modeshape.graph.Graph.Batch;
import org.modeshape.graph.connector.RepositoryConnectionFactory;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.connector.RepositorySourceException;
import org.modeshape.graph.connector.UuidAlreadyExistsException;
import org.modeshape.graph.property.InvalidPathException;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NameFactory;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.PropertyFactory;
import org.modeshape.graph.property.ValueFormatException;
import org.modeshape.graph.property.basic.LocalNamespaceRegistry;
import org.modeshape.graph.request.InvalidWorkspaceException;
import org.modeshape.graph.request.ReadBranchRequest;
import org.modeshape.graph.session.GraphSession;
import org.modeshape.graph.session.GraphSession.Node;
import org.modeshape.jcr.JcrContentHandler.EnclosingSAXException;
import org.modeshape.jcr.JcrContentHandler.SaveMode;
import org.modeshape.jcr.SessionCache.JcrNodePayload;
import org.modeshape.jcr.SessionCache.JcrPropertyPayload;
import org.modeshape.jcr.WorkspaceLockManager.ModeShapeLock;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * ModeShape implementation of a {@link Workspace JCR Workspace}.
 */
@NotThreadSafe
class JcrWorkspace implements Workspace {

    /**
     * The name of this workspace. This name is used as the name of the source when
     * {@link RepositoryConnectionFactory#createConnection(String) creating connections} to the underlying
     * {@link RepositorySource} that stores the content for this workspace.
     */
    private final String name;

    /**
     * The context in which this workspace is executing/operating. This context already has been authenticated.
     */
    private final ExecutionContext context;

    /**
     * The reference to the {@link JcrRepository} instance that owns this {@link Workspace} instance. Very few methods on this
     * repository object are used; mainly {@link JcrRepository#createWorkspaceGraph(String,ExecutionContext)},
     * {@link JcrRepository#getPersistentRegistry()} and {@link JcrRepository#getRepositorySourceName()}.
     */
    private final JcrRepository repository;

    /**
     * The graph used by this workspace to access persistent content. This graph is not thread-safe, but since this workspace is
     * not thread-safe, it is okay for any method in this workspace to use the same graph. It is also okay for other objects that
     * have the same thread context as this workspace (e.g., the session, namespace registry, etc.) to also reuse this same graph
     * instance (though it's not very expensive at all for each to have their own instance, too).
     */
    private final JcrGraph graph;

    /**
     * Reference to the namespace registry for this workspace. Per the JCR specification, this registry instance is persistent
     * (unlike the namespace-related methods in the {@link Session}, like {@link Session#getNamespacePrefix(String)},
     * {@link Session#setNamespacePrefix(String, String)}, etc.).
     */
    private final JcrNamespaceRegistry workspaceRegistry;

    /**
     * Reference to the JCR type manager for this workspace.
     */
    private final JcrNodeTypeManager nodeTypeManager;

    /**
     * Reference to the version manager for this workspace.
     */
    private final JcrVersionManager versionManager;

    /**
     * Reference to the JCR query manager for this workspace.
     */
    private final JcrQueryManager queryManager;

    /**
     * Reference to the JCR observation manager for this workspace.
     */
    private final JcrObservationManager observationManager;

    private final JcrLockManager lockManager;

    /**
     * The {@link Session} instance that this corresponds with this workspace.
     */
    private final JcrSession session;

    JcrWorkspace( JcrRepository repository,
                  String workspaceName,
                  ExecutionContext context,
                  Map<String, Object> sessionAttributes ) {

        assert workspaceName != null;
        assert context != null;
        assert context.getSecurityContext() != null;
        assert repository != null;
        this.name = workspaceName;
        this.repository = repository;

        // Create an execution context for this session, which should use the local namespace registry ...
        NamespaceRegistry globalRegistry = context.getNamespaceRegistry();
        LocalNamespaceRegistry localRegistry = new LocalNamespaceRegistry(globalRegistry);
        this.context = context.with(localRegistry);

        // Pre-cache all of the namespaces to be a snapshot of what's in the global registry at this time.
        // This behavior is specified in Section 3.5.2 of the JCR 2.0 specification.
        localRegistry.getNamespaces();

        // Now create a graph for the session ...
        this.graph = this.repository.createWorkspaceGraph(this.name, this.context);

        // Set up the session for this workspace ...
        this.session = new JcrSession(this.repository, this, this.context, globalRegistry, sessionAttributes);

        // This must be initialized after the session
        this.nodeTypeManager = new JcrNodeTypeManager(session, this.repository.getRepositoryTypeManager());
        this.versionManager = new JcrVersionManager(this.session);
        this.queryManager = new JcrQueryManager(this.session);
        this.observationManager = new JcrObservationManager(this.session, this.repository.getRepositoryObservable());

        // if (Boolean.valueOf(repository.getOptions().get(Option.PROJECT_NODE_TYPES))) {
        // Path parentOfTypeNodes = context.getValueFactories().getPathFactory().create(systemPath, JcrLexicon.NODE_TYPES);
        // repoTypeManager.projectOnto(this.graph, parentOfTypeNodes);
        // }
        //
        // Set up and initialize the persistent JCR namespace registry ...
        this.workspaceRegistry = new JcrNamespaceRegistry(this.repository.getPersistentRegistry(), this.session);
        this.lockManager = new JcrLockManager(session, repository.getRepositoryLockManager().getLockManager(workspaceName));

    }

    final JcrGraph graph() {
        return this.graph;
    }

    final String getSourceName() {
        return this.repository.getRepositorySourceName();
    }

    final JcrNodeTypeManager nodeTypeManager() {
        return this.nodeTypeManager;
    }

    final ExecutionContext context() {
        return this.context;
    }

    final JcrLockManager lockManager() {
        return this.lockManager;
    }

    final JcrObservationManager observationManager() {
        return this.observationManager;
    }

    final JcrQueryManager queryManager() {
        return this.queryManager;
    }

    /**
     * {@inheritDoc}
     */
    public final String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public final Session getSession() {
        return this.session;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Workspace#getNamespaceRegistry()
     */
    public final javax.jcr.NamespaceRegistry getNamespaceRegistry() {
        return workspaceRegistry;
    }

    /**
     * {@inheritDoc}
     */
    public String[] getAccessibleWorkspaceNames() throws RepositoryException {
        session.checkLive();
        try {
            Set<String> workspaceNamesFromGraph = graph.getWorkspaces();
            Set<String> workspaceNames = new HashSet<String>(workspaceNamesFromGraph.size());

            for (String workspaceName : workspaceNamesFromGraph) {
                try {
                    session.checkPermission(workspaceName, null, ModeShapePermissions.READ);
                    workspaceNames.add(workspaceName);
                } catch (AccessControlException ace) {
                    // Can happen if user doesn't have the privileges to read from the workspace
                }
            }

            return workspaceNames.toArray(new String[workspaceNames.size()]);
        } catch (RepositorySourceException e) {
            throw new RepositoryException(JcrI18n.errorObtainingWorkspaceNames.text(getSourceName(), e.getMessage()), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public final NodeTypeManager getNodeTypeManager() {
        return nodeTypeManager;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Workspace#getObservationManager()
     */
    public final ObservationManager getObservationManager() {
        return this.observationManager;
    }

    /**
     * @return the lock manager for this workspace and session
     */
    public LockManager getLockManager() {
        return lockManager;
    }

    /**
     * {@inheritDoc}
     */
    public final QueryManager getQueryManager() {
        return queryManager;
    }

    final JcrVersionManager versionManager() {
        return versionManager;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Workspace#clone(java.lang.String, java.lang.String, java.lang.String, boolean)
     */
    public void clone( String srcWorkspace,
                       String srcAbsPath,
                       String destAbsPath,
                       boolean removeExisting )
        throws ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException,
        LockException, RepositoryException {
        CheckArg.isNotNull(srcWorkspace, "source workspace name");
        CheckArg.isNotNull(srcAbsPath, "source path");
        CheckArg.isNotNull(destAbsPath, "destination path");

        final boolean sameWorkspace = getName().equals(srcWorkspace);
        if (sameWorkspace && removeExisting) {
            // This is a special case of cloning within the same workspace but removing the original, which equates to a move ...
            move(srcAbsPath, destAbsPath);
            return;
        }

        session.checkLive();
        if (!sameWorkspace && !graph.getWorkspaces().contains(srcWorkspace)) {
            throw new NoSuchWorkspaceException(JcrI18n.workspaceNameIsInvalid.text(graph.getSourceName(), this.name));
        }

        // Create the paths ...
        PathFactory factory = context.getValueFactories().getPathFactory();
        Path srcPath = null;
        Path destPath = null;
        try {
            srcPath = factory.create(srcAbsPath);
        } catch (ValueFormatException e) {
            throw new PathNotFoundException(JcrI18n.invalidPathParameter.text(srcAbsPath, "srcAbsPath"), e);
        }
        try {
            destPath = factory.create(destAbsPath);
        } catch (ValueFormatException e) {
            throw new PathNotFoundException(JcrI18n.invalidPathParameter.text(destAbsPath, "destAbsPath"), e);
        }

        // Doing a literal test here because the path factory will canonicalize "/node[1]" to "/node"
        if (!sameWorkspace && !destPath.isIdentifier() && destAbsPath.endsWith("]")) {
            throw new RepositoryException(JcrI18n.pathCannotHaveSameNameSiblingIndex.text(destAbsPath));
        }

        try {
            // Use the session to verify that the node location has a definition and is valid with the new cloned child.
            // This also performs the check permission for reading the parent ...
            SessionCache cache = this.session.cache();
            AbstractJcrNode parentNode = null;
            Name newNodeName = null;
            if (destPath.isIdentifier()) {
                AbstractJcrNode existingDestNode = cache.findJcrNode(Location.create(destPath));
                parentNode = existingDestNode.getParent();
                newNodeName = existingDestNode.segment().getName();
                destPath = factory.create(parentNode.path(), newNodeName);
            } else {
                parentNode = cache.findJcrNode(null, destPath.getParent());
                newNodeName = destPath.getLastSegment().getName();
            }

            /*
             * Find the UUID for the source node.  Have to go directly against the graph.
             */
            org.modeshape.graph.Node sourceNode = repository.createWorkspaceGraph(srcWorkspace, context).getNodeAt(srcPath);
            Property uuidProp = sourceNode.getProperty(ModeShapeLexicon.UUID);
            UUID sourceUuid = null;

            if (uuidProp != null) {
                sourceUuid = this.context.getValueFactories().getUuidFactory().create(uuidProp.getFirstValue());

                ModeShapeLock sourceLock = lockManager().lockFor(sourceUuid);
                if (sourceLock != null && sourceLock.getLockToken() == null) {
                    throw new LockException(JcrI18n.lockTokenNotHeld.text(srcAbsPath));
                }
            }

            if (parentNode.isLocked()) {
                Lock newParentLock = parentNode.getLock();
                if (newParentLock != null && newParentLock.getLockToken() == null) {
                    throw new LockException(destAbsPath);
                }
            }

            if (!parentNode.isCheckedOut()) {
                throw new VersionException(JcrI18n.nodeIsCheckedIn.text(parentNode.getPath()));
            }

            // Verify that this node accepts a child of the supplied name (given any existing SNS nodes) ...
            Node<JcrNodePayload, JcrPropertyPayload> parent = cache.findNode(parentNode.nodeId, parentNode.path());
            JcrNodeDefinition childDefn = null;
            try {
                childDefn = cache.findBestNodeDefinition(parent, newNodeName, parent.getPayload().getPrimaryTypeName());
            } catch (RepositoryException e) {
                if (sameWorkspace) {
                    // We're creating a shared node, so get the child node defn for the original's primary type
                    // under the parent of the new shared (proxy) node ...
                    AbstractJcrNode originalShareable = cache.findJcrNode(Location.create(sourceUuid));
                    Name originalShareablePrimaryType = originalShareable.getPrimaryTypeName();
                    childDefn = cache.findBestNodeDefinition(parent, newNodeName, originalShareablePrimaryType);
                } else {
                    throw e;
                }
            }

            if (sameWorkspace) {
                assert !removeExisting;

                // This method is also used to create a shared node, so first check that the source is shareable ...
                Property primaryType = sourceNode.getProperty(JcrLexicon.PRIMARY_TYPE);
                NameFactory nameFactory = context.getValueFactories().getNameFactory();
                boolean shareable = false;
                if (primaryType != null) {
                    Name primaryTypeName = nameFactory.create(primaryType.getFirstValue());
                    JcrNodeType nodeType = nodeTypeManager().getNodeType(primaryTypeName);
                    if (nodeType != null && nodeType.isNodeType(JcrMixLexicon.SHAREABLE)) shareable = true;
                }
                if (!shareable) {
                    Property mixinTypes = sourceNode.getProperty(JcrLexicon.MIXIN_TYPES);
                    if (mixinTypes != null) {
                        for (Object value : mixinTypes) {
                            Name mixinTypeName = nameFactory.create(value);
                            JcrNodeType nodeType = nodeTypeManager().getNodeType(mixinTypeName);
                            if (nodeType != null && nodeType.isNodeType(JcrMixLexicon.SHAREABLE)) {
                                shareable = true;
                                break;
                            }
                        }
                    }
                }
                if (shareable) {

                    // All is okay so far, so all we need to do here is create a "mode:share" node with a primary type ...
                    // and "mode:sharedUuid" property ...
                    assert sourceUuid != null;
                    assert childDefn != null;
                    PropertyFactory propertyFactory = context.getPropertyFactory();
                    Collection<Property> properties = new ArrayList<Property>(2);
                    properties.add(propertyFactory.create(JcrLexicon.PRIMARY_TYPE, ModeShapeLexicon.SHARE));
                    properties.add(propertyFactory.create(ModeShapeLexicon.SHARED_UUID, sourceUuid));
                    properties.add(propertyFactory.create(ModeShapeIntLexicon.NODE_DEFINITON, childDefn.getId().getString()));
                    cache.graphSession().immediateCreateOrReplace(destPath, properties);
                    return;
                }
                // Otherwise, just fall through and attempt to create a clone without removing the existing nodes;
                // this may fail or it may succeed, but it is the prescribed behavior...
            }
            if (removeExisting) {
                // This will remove any existing nodes in this (the "target") workspace that have the same UUIDs
                // as nodes that will be put into this workspace with the clone operation. Thus, any such
                // existing nodes will be removed; but if they're mandatory they cannot be removed, resulting
                // in a ConstraintViolationException. Therefore, we have to do a little homework here ...
                Set<UUID> uuidsInCloneBranch = getUuidsInBranch(srcPath, srcWorkspace);
                if (!uuidsInCloneBranch.isEmpty()) {
                    // See if any of these exist in the current workspace, and if so whether they can be removed ...
                    // This is NOT very efficient, since it may result in a batch read for each node ...
                    GraphSession<JcrNodePayload, JcrPropertyPayload> graphSession = cache.graphSession();
                    Node<JcrNodePayload, JcrPropertyPayload> node = null;
                    for (UUID uuid : uuidsInCloneBranch) {
                        Location location = Location.create(uuid);
                        try {
                            node = graphSession.findNodeWith(location);
                        } catch (org.modeshape.graph.property.PathNotFoundException e) {
                            // okay, it's not found in the current workspace, so nothing to check ...
                            continue;
                        }
                        // Get the node type that owns the child node definition ...
                        NodeDefinitionId childDefnId = node.getPayload().getDefinitionId();
                        JcrNodeType nodeType = nodeTypeManager().getNodeType(childDefnId.getNodeTypeName());
                        childDefn = nodeType.childNodeDefinition(childDefnId);
                        if (childDefn.isMandatory()) {
                            // We can't just remove a mandatory node... unless its parent will be removed too!
                            String path = node.getPath().getString(context.getNamespaceRegistry());
                            throw new ConstraintViolationException(JcrI18n.cannotRemoveNodeFromClone.text(path, uuid));
                        }
                        // Check whether the node has any local changes ...
                        if (node.isChanged(true)) {
                            // This session has changes on nodes that will be removed as a result of the clone ...
                            String path = node.getPath().getString(context.getNamespaceRegistry());
                            throw new RepositoryException(JcrI18n.cannotRemoveNodeFromCloneDueToChangesInSession.text(path, uuid));
                        }
                    }
                }
            }

            // Now perform the clone, using the direct (non-session) method ...
            cache.graphSession().immediateClone(srcPath, srcWorkspace, destPath, removeExisting, false);
        } catch (ItemNotFoundException e) {
            // The destination path was not found ...
            throw new PathNotFoundException(e.getLocalizedMessage(), e);
        } catch (org.modeshape.graph.property.PathNotFoundException e) {
            throw new PathNotFoundException(e.getLocalizedMessage(), e);
        } catch (UuidAlreadyExistsException e) {
            throw new ItemExistsException(e.getLocalizedMessage(), e);
        } catch (InvalidWorkspaceException e) {
            throw new NoSuchWorkspaceException(e.getLocalizedMessage(), e);
        } catch (RepositorySourceException e) {
            throw new RepositoryException(e.getLocalizedMessage(), e);
        } catch (AccessControlException ace) {
            throw new AccessDeniedException(ace);
        }
    }

    protected Set<UUID> getUuidsInBranch( Path sourcePath,
                                          String workspace ) {
        String existingWorkspace = graph.getCurrentWorkspaceName();
        try {
            graph.useWorkspace(workspace);
            Location location = Location.create(sourcePath);
            Subgraph subgraph = graph.getSubgraphOfDepth(ReadBranchRequest.NO_MAXIMUM_DEPTH).at(location);
            // Collect up the UUIDs; we use UUID here because that's what JCR requires ...
            Set<UUID> uuids = new HashSet<UUID>();
            for (SubgraphNode nodeInSubgraph : subgraph) {
                UUID uuid = nodeInSubgraph.getLocation().getUuid();
                if (uuid != null) uuids.add(uuid);
            }
            return uuids;
        } finally {
            graph.useWorkspace(existingWorkspace);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Workspace#copy(java.lang.String, java.lang.String)
     */
    public void copy( String srcAbsPath,
                      String destAbsPath )
        throws ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException,
        LockException, RepositoryException {
        this.copy(this.name, srcAbsPath, destAbsPath);
    }

    /**
     * {@inheritDoc}
     */
    public void copy( String srcWorkspace,
                      String srcAbsPath,
                      String destAbsPath )
        throws ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException,
        LockException, RepositoryException {
        CheckArg.isNotNull(srcWorkspace, "source workspace name");
        CheckArg.isNotNull(srcAbsPath, "source path");
        CheckArg.isNotNull(destAbsPath, "destination path");
        session.checkLive();

        if (!graph.getWorkspaces().contains(srcWorkspace)) {
            throw new NoSuchWorkspaceException(JcrI18n.workspaceNameIsInvalid.text(graph.getSourceName(), this.name));
        }

        // Create the paths ...
        PathFactory factory = context.getValueFactories().getPathFactory();
        Path srcPath = null;
        Path destPath = null;
        try {
            srcPath = factory.create(srcAbsPath);
        } catch (ValueFormatException e) {
            throw new PathNotFoundException(JcrI18n.invalidPathParameter.text(srcAbsPath, "srcAbsPath"), e);
        }
        try {
            destPath = factory.create(destAbsPath);
        } catch (ValueFormatException e) {
            throw new PathNotFoundException(JcrI18n.invalidPathParameter.text(destAbsPath, "destAbsPath"), e);
        }

        // Doing a literal test here because the path factory will canonicalize "/node[1]" to "/node"
        if (!destPath.isIdentifier() && destAbsPath.endsWith("]")) {
            throw new RepositoryException(JcrI18n.pathCannotHaveSameNameSiblingIndex.text(destAbsPath));
        }

        try {
            // Use the session to verify that the node location has a definition and is valid with the new cloned child.
            // This also performs the check permission for reading the parent ...
            SessionCache cache = this.session.cache();
            AbstractJcrNode parentNode = null;
            Name newNodeName = null;
            if (destPath.isIdentifier()) {
                AbstractJcrNode existingDestNode = cache.findJcrNode(Location.create(destPath));
                parentNode = existingDestNode.getParent();
                newNodeName = existingDestNode.segment().getName();
                destPath = factory.create(parentNode.path(), newNodeName);
            } else {
                parentNode = cache.findJcrNode(null, destPath.getParent());
                newNodeName = destPath.getLastSegment().getName();
            }

            /*
             * Find the UUID for the source node.  Have to go directly against the graph.
             */
            org.modeshape.graph.Node sourceNode = repository.createWorkspaceGraph(srcWorkspace, context).getNodeAt(srcPath);
            Property uuidProp = sourceNode.getProperty(ModeShapeLexicon.UUID);

            if (uuidProp != null) {
                UUID sourceUuid = this.context.getValueFactories().getUuidFactory().create(uuidProp.getFirstValue());

                ModeShapeLock sourceLock = lockManager().lockFor(sourceUuid);
                if (sourceLock != null && sourceLock.getLockToken() == null) {
                    throw new LockException(srcAbsPath);
                }
            }

            /*
             * Next, find the primary type for the source node.  
             */
            Property primaryTypeProp = sourceNode.getProperty(JcrLexicon.PRIMARY_TYPE);
            Name primaryTypeName = this.context.getValueFactories().getNameFactory().create(primaryTypeProp.getFirstValue());

            if (parentNode.isLocked()) {
                Lock newParentLock = parentNode.getLock();
                if (newParentLock != null && newParentLock.getLockToken() == null) {
                    throw new LockException(destAbsPath);
                }
            }

            if (!parentNode.isCheckedOut()) {
                throw new VersionException(JcrI18n.nodeIsCheckedIn.text(parentNode.getPath()));
            }

            cache.findBestNodeDefinition(parentNode.nodeInfo(), newNodeName, primaryTypeName);

            // ----------------------------------------------------------------------------------------
            // Now perform the clone, using the direct (non-session) method ...
            // ----------------------------------------------------------------------------------------
            Location copy = cache.graphSession().immediateCopy(srcPath, srcWorkspace, destPath);
            destPath = copy.getPath();

            // ----------------------------------------------------------------------------------------
            // We need to initialize the version history for all newly-created versionable nodes ...
            // ----------------------------------------------------------------------------------------
            if (repository.isQueryExecutionEnabled()) {
                // Use the query system to find the (hopefully small) subset of the new nodes that are versionable ...

                // Step 1: Query the source branch to find all versionable nodes in the source brach ...
                String queryStr = "SELECT [jcr:path],[jcr:uuid] FROM [mix:versionable] WHERE PATH() = '" + srcAbsPath
                                  + "' OR PATH() LIKE '" + srcAbsPath + "/%'";
                Query query = getQueryManager().createQuery(queryStr, JcrRepository.QueryLanguage.JCR_SQL2);
                QueryResult result = query.execute();

                // Step 2: Load the new versionable nodes into the cache and initialize their version history ...
                Batch batch = repository.createWorkspaceGraph(srcWorkspace, context).batch();
                NodeIterator versionableIter = result.getNodes();
                int initializedCount = 0;
                while (versionableIter.hasNext()) {
                    AbstractJcrNode versionable = (AbstractJcrNode)versionableIter.nextNode();
                    // Map this source node's path into the destination path ...
                    Path sourcePath = versionable.path();
                    Path newNodePath = null;
                    if (sourcePath.equals(srcPath)) {
                        newNodePath = destPath;
                    } else {
                        Path relativePath = versionable.path().relativeTo(srcPath);
                        newNodePath = relativePath.resolveAgainst(destPath);
                    }

                    // We have to load the node in the cache ...
                    AbstractJcrNode newVersionableNode = cache.findJcrNode(Location.create(newNodePath));
                    if (newVersionableNode instanceof JcrSharedNode) continue;
                    UUID originalVersion = versionable.getBaseVersion().uuid();
                    versionManager.initializeVersionHistoryFor(batch, newVersionableNode.nodeInfo(), originalVersion, true);
                    ++initializedCount;
                }
                batch.execute();
            } else {
                // Don't use the queries ...
                List<AbstractJcrNode> nodesToCheck = new LinkedList<AbstractJcrNode>();
                nodesToCheck.add(cache.findJcrNode(Location.create(destPath)));

                // Create a batch that we'll use for initializing the version history of all newly-created versionable nodes ...
                Batch batch = repository.createWorkspaceGraph(srcWorkspace, context).batch();
                while (!nodesToCheck.isEmpty()) {
                    AbstractJcrNode node = nodesToCheck.remove(0);

                    if (node.isNodeType(JcrMixLexicon.VERSIONABLE)) {
                        // Find the node that this was copied from
                        Path nodeDestPath = node.path().relativeTo(destPath);
                        Path nodeSourcePath = nodeDestPath.resolveAgainst(srcPath);

                        AbstractJcrNode fromNode = cache.findJcrNode(Location.create(nodeSourcePath));
                        if (!(fromNode instanceof JcrSharedNode)) {
                            UUID originalVersion = fromNode.getBaseVersion().uuid();
                            // versionManager.initializeVersionHistoryFor(node, originalVersion);
                            versionManager.initializeVersionHistoryFor(batch, node.nodeInfo(), originalVersion, true);
                        }
                    }

                    for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
                        nodesToCheck.add((AbstractJcrNode)iter.nextNode());
                    }
                }
                batch.execute();
            }

        } catch (ItemNotFoundException e) {
            // The destination path was not found ...
            throw new PathNotFoundException(e.getLocalizedMessage(), e);
        } catch (org.modeshape.graph.property.PathNotFoundException e) {
            throw new PathNotFoundException(e.getLocalizedMessage(), e);
        } catch (UuidAlreadyExistsException e) {
            throw new ItemExistsException(e.getLocalizedMessage(), e);
        } catch (InvalidWorkspaceException e) {
            throw new NoSuchWorkspaceException(e.getLocalizedMessage(), e);
        } catch (RepositorySourceException e) {
            throw new RepositoryException(e.getLocalizedMessage(), e);
        } catch (AccessControlException ace) {
            throw new AccessDeniedException(ace);
        } catch (InvalidPathException e) {
            throw new RepositoryException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Workspace#getImportContentHandler(java.lang.String, int)
     */
    public ContentHandler getImportContentHandler( String parentAbsPath,
                                                   int uuidBehavior )
        throws PathNotFoundException, ConstraintViolationException, VersionException, LockException, AccessDeniedException,
        RepositoryException {

        CheckArg.isNotNull(parentAbsPath, "parentAbsPath");
        session.checkLive();
        Path parentPath = this.context.getValueFactories().getPathFactory().create(parentAbsPath);
        Repository repo = getSession().getRepository();
        boolean retainLifecycleInfo = repo.getDescriptorValue(Repository.OPTION_LIFECYCLE_SUPPORTED).getBoolean();
        boolean retainRetentionInfo = repo.getDescriptorValue(Repository.OPTION_RETENTION_SUPPORTED).getBoolean();
        return new JcrContentHandler(this.session, parentPath, uuidBehavior, SaveMode.WORKSPACE, retainRetentionInfo,
                                     retainLifecycleInfo);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Workspace#importXML(java.lang.String, java.io.InputStream, int)
     */
    public void importXML( String parentAbsPath,
                           InputStream in,
                           int uuidBehavior )
        throws IOException, PathNotFoundException, ItemExistsException, ConstraintViolationException,
        InvalidSerializedDataException, LockException, AccessDeniedException, RepositoryException {

        CheckArg.isNotNull(parentAbsPath, "parentAbsPath");
        CheckArg.isNotNull(in, "in");
        session.checkLive();

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
     * @see javax.jcr.Workspace#move(java.lang.String, java.lang.String)
     */
    public void move( String srcAbsPath,
                      String destAbsPath ) throws PathNotFoundException, RepositoryException {
        CheckArg.isNotEmpty(srcAbsPath, "srcAbsPath");
        CheckArg.isNotEmpty(destAbsPath, "destAbsPath");
        session.checkLive();

        // Create the paths ...
        PathFactory factory = context.getValueFactories().getPathFactory();
        Path srcPath = null;
        Path destPath = null;
        try {
            srcPath = factory.create(srcAbsPath);
        } catch (ValueFormatException e) {
            throw new PathNotFoundException(JcrI18n.invalidPathParameter.text(srcAbsPath, "srcAbsPath"), e);
        }
        try {
            destPath = factory.create(destAbsPath);
        } catch (ValueFormatException e) {
            throw new PathNotFoundException(JcrI18n.invalidPathParameter.text(destAbsPath, "destAbsPath"), e);
        }

        // Doing a literal test here because the path factory will canonicalize "/node[1]" to "/node"
        if (!destPath.isIdentifier() && destAbsPath.endsWith("]")) {
            throw new RepositoryException(JcrI18n.pathCannotHaveSameNameSiblingIndex.text(destAbsPath));
        }

        try {
            // Use the session to verify that the node location has a definition and is valid with the new cloned child.
            // This also performs the check permission for reading the parent ...
            SessionCache cache = this.session.cache();
            Node<JcrNodePayload, JcrPropertyPayload> newParent = null;
            Name newNodeName = null;
            if (destPath.isIdentifier()) {
                Node<JcrNodePayload, JcrPropertyPayload> existingDestNode = cache.findNodeWith(Location.create(destPath));
                newParent = existingDestNode.getParent();
                newNodeName = existingDestNode.getName();
                destPath = factory.create(newParent.getPath(), newNodeName);
            } else {
                newParent = cache.findNode(null, destPath.getParent());
                newNodeName = destPath.getLastSegment().getName();
            }

            cache.findBestNodeDefinition(newParent, newNodeName, newParent.getPayload().getPrimaryTypeName());

            AbstractJcrNode sourceNode = cache.findJcrNode(Location.create(srcPath));

            if (sourceNode.isLocked()) {
                Lock sourceLock = sourceNode.getLock();
                if (sourceLock != null && sourceLock.getLockToken() == null) {
                    throw new LockException(srcAbsPath);
                }
            }

            AbstractJcrNode parentNode = cache.findJcrNode(newParent.getNodeId(), newParent.getPath());

            if (parentNode.isLocked()) {
                Lock newParentLock = parentNode.getLock();
                if (newParentLock != null && newParentLock.getLockToken() == null) {
                    throw new LockException(destAbsPath);
                }
            }

            if (!sourceNode.isCheckedOut()) {
                throw new VersionException(JcrI18n.nodeIsCheckedIn.text(sourceNode.getPath()));
            }

            if (!parentNode.isCheckedOut()) {
                throw new VersionException(JcrI18n.nodeIsCheckedIn.text(parentNode.getPath()));
            }

            // Now perform the clone, using the direct (non-session) method ...
            cache.graphSession().immediateMove(srcPath, destPath);
        } catch (AccessControlException ace) {
            throw new AccessDeniedException(ace);
        } catch (ItemNotFoundException infe) {
            throw new PathNotFoundException(infe);
        } catch (org.modeshape.graph.property.PathNotFoundException pnfe) {
            throw new PathNotFoundException(pnfe);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Workspace#restore(Version[], boolean)
     */
    public void restore( Version[] versions,
                         boolean removeExisting ) throws RepositoryException {
        versionManager().restore(versions, removeExisting);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The caller must have {@link ModeShapePermissions#CREATE_WORKSPACE permission to create workspaces}, and must have
     * {@link ModeShapePermissions#READ read permission} on the source workspace.
     * </p>
     * 
     * @see javax.jcr.Workspace#createWorkspace(java.lang.String, java.lang.String)
     */
    @Override
    public void createWorkspace( String name,
                                 String srcWorkspace )
        throws AccessDeniedException, UnsupportedRepositoryOperationException, NoSuchWorkspaceException, RepositoryException {
        CheckArg.isNotNull(name, "name");
        CheckArg.isNotNull(srcWorkspace, "srcWorkspace");
        session.checkLive();
        try {
            session.checkPermission(srcWorkspace, null, ModeShapePermissions.READ);
            session.checkPermission(name, null, ModeShapePermissions.CREATE_WORKSPACE);
            repository.createWorkspace(name, srcWorkspace);
        } catch (AccessControlException e) {
            throw new AccessDeniedException(e);
        } catch (InvalidWorkspaceException e) {
            throw new NoSuchWorkspaceException(e);
        } catch (RepositorySourceException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * The caller must have {@link ModeShapePermissions#CREATE_WORKSPACE permission to create workspaces}.
     * </p>
     * 
     * @see javax.jcr.Workspace#createWorkspace(java.lang.String)
     */
    @Override
    public void createWorkspace( String name )
        throws AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
        CheckArg.isNotNull(name, "name");
        session.checkLive();
        try {
            session.checkPermission(name, null, ModeShapePermissions.CREATE_WORKSPACE);
            repository.createWorkspace(name, null);
        } catch (AccessControlException e) {
            throw new AccessDeniedException(e);
        } catch (InvalidWorkspaceException e) {
            throw new RepositoryException(e);
        } catch (RepositorySourceException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * It is not possible to delete the current workspace. Also, the caller must have
     * {@link ModeShapePermissions#DELETE_WORKSPACE permission to delete workspaces}.
     * </p>
     * 
     * @see javax.jcr.Workspace#deleteWorkspace(java.lang.String)
     */
    @Override
    public void deleteWorkspace( String name )
        throws AccessDeniedException, UnsupportedRepositoryOperationException, NoSuchWorkspaceException, RepositoryException {
        CheckArg.isNotNull(name, "name");
        session.checkLive();
        try {
            session.checkPermission(name, null, ModeShapePermissions.DELETE_WORKSPACE);
            repository.destroyWorkspace(name, this);
        } catch (AccessControlException e) {
            throw new AccessDeniedException(e);
        } catch (InvalidWorkspaceException e) {
            throw new RepositoryException(e);
        } catch (RepositorySourceException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Workspace#getVersionManager()
     */
    @Override
    public VersionManager getVersionManager() {
        return versionManager;
    }

}
