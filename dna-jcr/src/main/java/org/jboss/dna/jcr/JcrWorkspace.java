/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.jcr;

import java.io.IOException;
import java.io.InputStream;
import java.security.AccessControlException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.observation.ObservationManager;
import javax.jcr.query.QueryManager;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.Subgraph;
import org.jboss.dna.graph.SubgraphNode;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.jboss.dna.graph.connector.RepositorySource;
import org.jboss.dna.graph.connector.RepositorySourceException;
import org.jboss.dna.graph.connector.UuidAlreadyExistsException;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.NameFactory;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.property.PropertyFactory;
import org.jboss.dna.graph.property.UuidFactory;
import org.jboss.dna.graph.property.ValueFormatException;
import org.jboss.dna.graph.property.basic.GraphNamespaceRegistry;
import org.jboss.dna.graph.request.ReadBranchRequest;
import org.jboss.dna.jcr.JcrContentHandler.EnclosingSAXException;
import org.jboss.dna.jcr.JcrContentHandler.SaveMode;
import org.jboss.dna.jcr.JcrRepository.Option;
import org.jboss.dna.jcr.cache.NodeInfo;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 *
 */
@NotThreadSafe
final class JcrWorkspace implements Workspace {

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
     * repository object are used; mainly {@link JcrRepository#getConnectionFactory()} and
     * {@link JcrRepository#getRepositorySourceName()}.
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
     * Reference to the JCR query manager for this workspace.
     */
    private final JcrQueryManager queryManager;

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

        // Set up the execution context for this workspace, which should use the namespace registry that persists
        // the namespaces in the graph ...
        Graph namespaceGraph = Graph.create(this.repository.getRepositorySourceName(),
                                            this.repository.getConnectionFactory(),
                                            context);
        namespaceGraph.useWorkspace(workspaceName);

        // Make sure the "/jcr:system" node exists ...
        PathFactory pathFactory = context.getValueFactories().getPathFactory();
        Path root = pathFactory.createRootPath();
        Path systemPath = pathFactory.create(root, JcrLexicon.SYSTEM);
        Property systemPrimaryType = context.getPropertyFactory().create(JcrLexicon.PRIMARY_TYPE, DnaLexicon.SYSTEM);
        namespaceGraph.create(systemPath, systemPrimaryType).ifAbsent().and();

        Name uriProperty = DnaLexicon.NAMESPACE_URI;
        Path namespacesPath = pathFactory.create(systemPath, DnaLexicon.NAMESPACES);
        PropertyFactory propertyFactory = context.getPropertyFactory();
        Property namespaceType = propertyFactory.create(JcrLexicon.PRIMARY_TYPE, DnaLexicon.NAMESPACE);
        org.jboss.dna.graph.property.NamespaceRegistry persistentRegistry = new GraphNamespaceRegistry(namespaceGraph,
                                                                                                       namespacesPath,
                                                                                                       uriProperty, namespaceType);
        this.context = context.with(persistentRegistry);

        // Now create a graph with this new execution context ...
        this.graph = Graph.create(this.repository.getRepositorySourceName(), this.repository.getConnectionFactory(), this.context);
        this.graph.useWorkspace(workspaceName);

        // Set up the session for this workspace ...
        this.session = new JcrSession(this.repository, this, this.context, sessionAttributes);

        // This must be initialized after the session
        RepositoryNodeTypeManager repoTypeManager = repository.getRepositoryTypeManager();
        this.nodeTypeManager = new JcrNodeTypeManager(session, repoTypeManager);
        this.queryManager = new JcrQueryManager(this.session);

        if (Boolean.valueOf(repository.getOptions().get(Option.PROJECT_NODE_TYPES))) {
            Path parentOfTypeNodes = context.getValueFactories().getPathFactory().create(systemPath, JcrLexicon.NODE_TYPES);
            repoTypeManager.projectOnto(this.graph, parentOfTypeNodes);
        }

        // Set up and initialize the persistent JCR namespace registry ...
        this.workspaceRegistry = new JcrNamespaceRegistry(persistentRegistry, this.session);

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
    public final NamespaceRegistry getNamespaceRegistry() {
        return workspaceRegistry;
    }

    /**
     * {@inheritDoc}
     */
    public String[] getAccessibleWorkspaceNames() throws RepositoryException {
        try {
            Set<String> workspaceNamesFromGraph = graph.getWorkspaces();
            Set<String> workspaceNames = new HashSet<String>(workspaceNamesFromGraph.size());
            
            for(String workspaceName : workspaceNamesFromGraph) {
                try {
                    session.checkPermission(workspaceName, null, JcrSession.JCR_READ_PERMISSION);
                    workspaceNames.add(workspaceName);
                }
                catch (AccessControlException ace) {
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
     */
    public final ObservationManager getObservationManager() throws UnsupportedRepositoryOperationException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public final QueryManager getQueryManager() {
        return queryManager;
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
            throw new RepositoryException();
        }

        try {
            this.session.checkPermission(destPath.getParent(), "add_node");
        } catch (AccessControlException ace) {
            throw new AccessDeniedException(ace);
        }

        clone(srcWorkspace, srcPath, destPath, removeExisting, false);
    }

    void clone( String srcWorkspace,
                Path srcPath,
                Path destPath,
                boolean removeExisting,
                boolean destPathIncludesSegment )
        throws ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException,
        LockException, RepositoryException {

        /*
         * Make sure that the node has a definition at the new location
         */
        SessionCache cache = this.session.cache();
        NodeInfo cacheParent = cache.findNodeInfo(null, destPath.getParent());

        // Skip the cache and load the latest parent info directly from the graph
        NodeInfo parent = cache.loadFromGraph(null, cacheParent.getUuid());
        Name newNodeName = destPath.getLastSegment().getName();
        String parentPath = destPath.getParent().getString(this.context.getNamespaceRegistry());

        Subgraph sourceTree = null;
        // This will check for a definition and throw a ConstraintViolationException or ItemExistsException if none is found
        graph.useWorkspace(srcWorkspace);
        Property primaryTypeProp;
        Property uuidProp;
        try {
            Map<Name, Property> props;

            if (removeExisting) {
                sourceTree = graph.getSubgraphOfDepth(ReadBranchRequest.NO_MAXIMUM_DEPTH).at(srcPath);
                props = sourceTree.getRoot().getPropertiesByName();
            } else {
                props = graph.getNodeAt(srcPath).getPropertiesByName();
            }
            primaryTypeProp = props.get(JcrLexicon.PRIMARY_TYPE);
            uuidProp = props.get(DnaLexicon.UUID);
        } catch (org.jboss.dna.graph.property.PathNotFoundException pnfe) {
            String srcAbsPath = srcPath.getString(this.context.getNamespaceRegistry());
            throw new PathNotFoundException(JcrI18n.itemNotFoundAtPath.text(srcAbsPath, srcWorkspace));
        } finally {
            graph.useWorkspace(this.name);
        }

        assert primaryTypeProp != null : "Cannot have a node in a JCR repository with no jcr:primaryType property";
        assert uuidProp != null : "Cannot have a node in a JCR repository with no UUID";

        NameFactory nameFactory = this.context.getValueFactories().getNameFactory();
        this.session.cache().findBestNodeDefinition(parent,
                                                    parentPath,
                                                    newNodeName,
                                                    nameFactory.create(primaryTypeProp.getFirstValue()));

        if (removeExisting) {
            assert sourceTree != null;

            // Find all of the UUIDS in the source tree
            UuidFactory uuidFactory = context().getValueFactories().getUuidFactory();
            PathFactory pathFactory = context().getValueFactories().getPathFactory();

            Set<UUID> sourceUuids = new HashSet<UUID>();
            LinkedList<SubgraphNode> nodesToVisit = new LinkedList<SubgraphNode>();
            nodesToVisit.addFirst(sourceTree.getRoot());

            while (!nodesToVisit.isEmpty()) {
                SubgraphNode node = nodesToVisit.removeFirst();

                UUID uuid = uuidFactory.create(node.getProperty(DnaLexicon.UUID).getFirstValue().toString());
                if (uuid != null) {
                    sourceUuids.add(uuid);
                }
                for (Path.Segment childSegment : node.getChildrenSegments()) {
                    nodesToVisit.add(node.getNode(pathFactory.createRelativePath(childSegment)));
                }
            }

            // See if the nodes exist in the current workspace outside of the current tree
            for (UUID uuid : sourceUuids) {
                NodeInfo nodeInfo = null;
                try {
                    nodeInfo = session.cache().findNodeInfo(uuid);
                } catch (ItemNotFoundException infe) {
                    continue;
                }
                assert nodeInfo != null;

                NodeDefinitionId nodeDefnId = nodeInfo.getDefinitionId();

                JcrNodeType nodeType = nodeTypeManager.getNodeType(nodeDefnId.getNodeTypeName());
                JcrNodeDefinition nodeDefn = nodeType.childNodeDefinition(nodeDefnId);
                if (nodeDefn.isMandatory()) {
                    // We can't just remove a mandatory node... unless its parent will be removed too!
                    String path = session.cache().getPathFor(nodeInfo).getString(context().getNamespaceRegistry());
                    throw new ConstraintViolationException(JcrI18n.cannotRemoveNodeFromClone.text(path, uuid));

                }
            }

            if (destPathIncludesSegment) {
                graph.clone(srcPath).fromWorkspace(srcWorkspace).as(destPath.getLastSegment()).into(destPath.getParent()).replacingExistingNodesWithSameUuids();
            } else {
                graph.clone(srcPath).fromWorkspace(srcWorkspace).as(newNodeName).into(destPath.getParent()).replacingExistingNodesWithSameUuids();
            }
        } else {
            try {
                if (destPathIncludesSegment) {
                    graph.clone(srcPath).fromWorkspace(srcWorkspace).as(destPath.getLastSegment()).into(destPath.getParent()).replacingExistingNodesWithSameUuids();
                } else {
                    graph.clone(srcPath).fromWorkspace(srcWorkspace).as(newNodeName).into(destPath.getParent()).failingIfAnyUuidsMatch();
                }

            } catch (UuidAlreadyExistsException uaee) {
                throw new ItemExistsException(uaee.getMessage());
            }
        }

        assert uuidProp.getFirstValue() instanceof UUID;
        // Load the node that we just copied
        cache.compensateForWorkspaceChildChange(cacheParent.getUuid(), null, (UUID)uuidProp.getFirstValue(), newNodeName);

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
            throw new RepositoryException();
        }

        try {
            this.session.checkPermission(destPath.getParent(), "add_node");
        } catch (AccessControlException ace) {
            throw new AccessDeniedException(ace);
        }

        /*
         * Make sure that the node has a definition at the new location
         */
        SessionCache cache = this.session.cache();
        NodeInfo cacheParent = cache.findNodeInfo(null, destPath.getParent());

        // Skip the cache and load the latest parent info directly from the graph
        NodeInfo parent = cache.loadFromGraph(destPath.getParent(), cacheParent.getUuid());
        Name newNodeName = destPath.getLastSegment().getName();
        String parentPath = destPath.getParent().getString(this.context.getNamespaceRegistry());

        // This will check for a definition and throw a ConstraintViolationException or ItemExistsException if none is found
        graph.useWorkspace(srcWorkspace);

        Property primaryTypeProp;
        Property uuidProp;
        try {
            Map<Name, Property> props = graph.getNodeAt(srcPath).getPropertiesByName();
            primaryTypeProp = props.get(JcrLexicon.PRIMARY_TYPE);
            uuidProp = props.get(DnaLexicon.UUID);
        } catch (org.jboss.dna.graph.property.PathNotFoundException pnfe) {
            throw new PathNotFoundException(JcrI18n.itemNotFoundAtPath.text(srcAbsPath, srcWorkspace));
        } finally {
            graph.useWorkspace(this.name);
        }

        assert primaryTypeProp != null : "Cannot have a node in a JCR repository with no jcr:primaryType property";
        assert uuidProp != null : "Cannot have a node in a JCR repository with no UUID";

        NameFactory nameFactory = this.context.getValueFactories().getNameFactory();
        this.session.cache().findBestNodeDefinition(parent,
                                                    parentPath,
                                                    newNodeName,
                                                    nameFactory.create(primaryTypeProp.getFirstValue()));

        // Perform the copy operation, but use the "to" form (not the "into", which takes the parent) ...
        graph.copy(srcPath).fromWorkspace(srcWorkspace).to(destPath);

        // Load the node that we just copied
        cache.compensateForWorkspaceChildChange(cacheParent.getUuid(),
                                                null,
                                                UUID.fromString(uuidProp.getFirstValue().toString()),
                                                newNodeName);
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
            throw new RepositoryException();
        }

        try {
            this.session.checkPermission(srcAbsPath.substring(0, srcAbsPath.lastIndexOf('/')), "remove");
            this.session.checkPermission(destAbsPath, "add_node");
        } catch (AccessControlException ace) {
            throw new AccessDeniedException(ace);
        }

        /*
         * Make sure that the node has a definition at the new location
         */
        SessionCache cache = this.session.cache();
        NodeInfo nodeInfo = cache.findNodeInfo(null, srcPath);
        NodeInfo cacheParent = cache.findNodeInfo(null, destPath.getParent());
        NodeInfo oldParent = cache.findNodeInfo(null, srcPath.getParent());

        // Skip the cache and load the latest parent info directly from the graph
        NodeInfo parent = cache.loadFromGraph(destPath.getParent(), cacheParent.getUuid());
        Name newNodeName = destPath.getLastSegment().getName();
        String parentPath = destPath.getParent().getString(this.context.getNamespaceRegistry());

        // This will check for a definition and throw a ConstraintViolationException or ItemExistsException if none is found
        cache.findBestNodeDefinition(parent, parentPath, newNodeName, nodeInfo.getPrimaryTypeName());

        // Perform the copy operation, but use the "to" form (not the "into", which takes the parent) ...
        graph.move(srcPath).as(newNodeName).into(destPath.getParent());
        cache.compensateForWorkspaceChildChange(cacheParent.getUuid(), oldParent.getUuid(), nodeInfo.getUuid(), newNodeName);
    }

    /**
     * {@inheritDoc}
     */
    public void restore( Version[] versions,
                         boolean removeExisting ) {
        throw new UnsupportedOperationException();
    }

}
