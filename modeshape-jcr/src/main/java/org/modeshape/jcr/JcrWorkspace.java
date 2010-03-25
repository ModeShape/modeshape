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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.observation.ObservationManager;
import javax.jcr.query.QueryManager;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.Location;
import org.modeshape.graph.Subgraph;
import org.modeshape.graph.SubgraphNode;
import org.modeshape.graph.connector.RepositoryConnectionFactory;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.connector.RepositorySourceException;
import org.modeshape.graph.connector.UuidAlreadyExistsException;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.Property;
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
    private final Graph graph;

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

    private final WorkspaceLockManager lockManager;

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
        this.lockManager = repository.getLockManager(workspaceName);

        // Create an execution context for this session, which should use the local namespace registry ...
        NamespaceRegistry globalRegistry = context.getNamespaceRegistry();
        LocalNamespaceRegistry localRegistry = new LocalNamespaceRegistry(globalRegistry);
        this.context = context.with(localRegistry);

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

    }

    final Graph graph() {
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

    final WorkspaceLockManager lockManager() {
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
        if (destAbsPath.endsWith("]")) {
            throw new RepositoryException(JcrI18n.pathCannotHaveSameNameSiblingIndex.text(destAbsPath));
        }

        try {
            // Use the session to verify that the node location has a definition and is valid with the new cloned child.
            // This also performs the check permission for reading the parent ...
            Name newNodeName = destPath.getLastSegment().getName();
            SessionCache cache = this.session.cache();

            /*
             * Find the UUID for the source node.  Have to go directly against the graph.
             */
            org.modeshape.graph.Node sourceNode = repository.createWorkspaceGraph(srcWorkspace, context).getNodeAt(srcPath);
            Property uuidProp = sourceNode.getProperty(ModeShapeLexicon.UUID);

            if (uuidProp != null) {
                UUID sourceUuid = this.context.getValueFactories().getUuidFactory().create(uuidProp.getFirstValue());

                ModeShapeLock sourceLock = lockManager().lockFor(session, Location.create(sourceUuid));
                if (sourceLock != null && sourceLock.getLockToken() == null) {
                    throw new LockException(JcrI18n.lockTokenNotHeld.text(srcAbsPath));
                }
            }

            AbstractJcrNode parentNode = cache.findJcrNode(Location.create(destPath.getParent()));

            if (parentNode.isLocked()) {
                Lock newParentLock = parentNode.getLock();
                if (newParentLock != null && newParentLock.getLockToken() == null) {
                    throw new LockException(destAbsPath);
                }
            }

            if (!parentNode.isCheckedOut()) {
                throw new VersionException(JcrI18n.nodeIsCheckedIn.text(parentNode.getPath()));
            }

            Node<JcrNodePayload, JcrPropertyPayload> parent = cache.findNode(null, destPath.getParent());
            cache.findBestNodeDefinition(parent, newNodeName, parent.getPayload().getPrimaryTypeName());

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
                        JcrNodeDefinition childDefn = nodeType.childNodeDefinition(childDefnId);
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
            Subgraph subgraph = graph.getSubgraphOfDepth(ReadBranchRequest.NO_MAXIMUM_DEPTH).at(sourcePath);
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
        if (destAbsPath.endsWith("]")) {
            throw new RepositoryException(JcrI18n.pathCannotHaveSameNameSiblingIndex.text(destAbsPath));
        }

        try {
            // Use the session to verify that the node location has a definition and is valid with the new cloned child.
            // This also performs the check permission for reading the parent ...
            Name newNodeName = destPath.getLastSegment().getName();
            SessionCache cache = this.session.cache();

            /*
             * Find the UUID for the source node.  Have to go directly against the graph.
             */
            org.modeshape.graph.Node sourceNode = repository.createWorkspaceGraph(srcWorkspace, context).getNodeAt(srcPath);
            Property uuidProp = sourceNode.getProperty(ModeShapeLexicon.UUID);

            if (uuidProp != null) {
                UUID sourceUuid = this.context.getValueFactories().getUuidFactory().create(uuidProp.getFirstValue());

                ModeShapeLock sourceLock = lockManager().lockFor(session, Location.create(sourceUuid));
                if (sourceLock != null && sourceLock.getLockToken() == null) {
                    throw new LockException(srcAbsPath);
                }
            }

            AbstractJcrNode parentNode = cache.findJcrNode(Location.create(destPath.getParent()));

            if (parentNode.isLocked()) {
                Lock newParentLock = parentNode.getLock();
                if (newParentLock != null && newParentLock.getLockToken() == null) {
                    throw new LockException(destAbsPath);
                }
            }

            if (!parentNode.isCheckedOut()) {
                throw new VersionException(JcrI18n.nodeIsCheckedIn.text(parentNode.getPath()));
            }

            Node<JcrNodePayload, JcrPropertyPayload> parent = cache.findNode(null, destPath.getParent());
            cache.findBestNodeDefinition(parent, newNodeName, parent.getPayload().getPrimaryTypeName());


            // Now perform the clone, using the direct (non-session) method ...
            cache.graphSession().immediateCopy(srcPath, srcWorkspace, destPath);
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

        Path parentPath = this.context.getValueFactories().getPathFactory().create(parentAbsPath);

        return new JcrContentHandler(this.session, parentPath, uuidBehavior, SaveMode.WORKSPACE);
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
        if (destAbsPath.endsWith("]")) {
            throw new RepositoryException(JcrI18n.pathCannotHaveSameNameSiblingIndex.text(destAbsPath));
        }

        try {
            // Use the session to verify that the node location has a definition and is valid with the new cloned child.
            // This also performs the check permission for reading the parent ...
            Name newNodeName = destPath.getLastSegment().getName();
            SessionCache cache = this.session.cache();
            Node<JcrNodePayload, JcrPropertyPayload> newParent = cache.findNode(null, destPath.getParent());
            cache.findBestNodeDefinition(newParent, newNodeName, newParent.getPayload().getPrimaryTypeName());

            AbstractJcrNode sourceNode = cache.findJcrNode(Location.create(srcPath));

            if (sourceNode.isLocked()) {
                Lock sourceLock = sourceNode.getLock();
                if (sourceLock != null && sourceLock.getLockToken() == null) {
                    throw new LockException(srcAbsPath);
                }
            }

            AbstractJcrNode parentNode = cache.findJcrNode(Location.create(destPath.getParent()));

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
        if (session.hasPendingChanges()) {
            throw new InvalidItemStateException(JcrI18n.noPendingChangesAllowed.text());
        }

        versionManager().restore(versions, removeExisting);
    }

}
