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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.ItemExistsException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.observation.ObservationManager;
import javax.jcr.query.QueryManager;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.GraphImporter;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.jboss.dna.graph.connector.RepositorySource;
import org.jboss.dna.graph.connector.RepositorySourceException;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.property.PropertyFactory;
import org.jboss.dna.graph.property.ValueFormatException;
import org.jboss.dna.graph.property.basic.GraphNamespaceRegistry;
import org.xml.sax.ContentHandler;

/**
 * @author John Verhaeg
 * @author Randall Hauch
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
    private final NodeTypeManager nodeTypeManager;

    /**
     * The {@link Session} instance that this corresponds with this workspace.
     */
    private final JcrSession session;

    @SuppressWarnings( "synthetic-access" )
    JcrWorkspace( JcrRepository repository,
                  String workspaceName,
                  ExecutionContext context,
                  Map<String, Object> sessionAttributes ) {
        assert workspaceName != null;
        assert context != null;
        assert repository != null;
        this.name = workspaceName;
        this.repository = repository;

        // Set up the execution context for this workspace, which should use the namespace registry that persists
        // the namespaces in the graph ...
        Graph namespaceGraph = Graph.create(this.repository.getRepositorySourceName(),
                                            this.repository.getConnectionFactory(),
                                            context);
        Name uriProperty = DnaLexicon.NAMESPACE_URI;
        PathFactory pathFactory = context.getValueFactories().getPathFactory();
        Path root = pathFactory.createRootPath();
        Path namespacesPath = context.getValueFactories().getPathFactory().create(root, JcrLexicon.SYSTEM, DnaLexicon.NAMESPACES);
        PropertyFactory propertyFactory = context.getPropertyFactory();
        Property namespaceType = propertyFactory.create(JcrLexicon.PRIMARY_TYPE, DnaLexicon.NAMESPACE);
        org.jboss.dna.graph.property.NamespaceRegistry persistentRegistry = new GraphNamespaceRegistry(namespaceGraph,
                                                                                                       namespacesPath,
                                                                                                       uriProperty, namespaceType);
        this.context = context.with(persistentRegistry);

        // Set up and initialize the persistent JCR namespace registry ...
        this.workspaceRegistry = new JcrNamespaceRegistry(persistentRegistry);

        // Now create a graph with this new execution context ...
        this.graph = Graph.create(this.repository.getRepositorySourceName(), this.repository.getConnectionFactory(), this.context);
        this.graph.useWorkspace(workspaceName);

        // Set up the session for this workspace ...
        this.session = new JcrSession(this.repository, this, this.context, sessionAttributes);

        // This must be initialized after the session
        this.nodeTypeManager = new JcrNodeTypeManager(new DnaBuiltinNodeTypeSource(this.session,
                                                                                   new JcrBuiltinNodeTypeSource(this.session)));

    }

    String getSourceName() {
        return this.repository.getRepositorySourceName();
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public Session getSession() {
        return this.session;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Workspace#getNamespaceRegistry()
     */
    public NamespaceRegistry getNamespaceRegistry() {
        return workspaceRegistry;
    }

    /**
     * {@inheritDoc}
     */
    public String[] getAccessibleWorkspaceNames() throws RepositoryException {
        try {
            Set<String> workspaces = graph.getWorkspaces();
            return workspaces.toArray(new String[workspaces.size()]);
        } catch (RepositorySourceException e) {
            throw new RepositoryException(JcrI18n.errorObtainingWorkspaceNames.text(getSourceName(), e.getMessage()), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public NodeTypeManager getNodeTypeManager() {
        return nodeTypeManager;
    }

    /**
     * {@inheritDoc}
     */
    public ObservationManager getObservationManager() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public QueryManager getQueryManager() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Workspace#clone(java.lang.String, java.lang.String, java.lang.String, boolean)
     */
    public void clone( String srcWorkspace,
                       String srcAbsPath,
                       String destAbsPath,
                       boolean removeExisting ) {
        throw new UnsupportedOperationException();
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
        CheckArg.isNotEmpty(srcAbsPath, "srcAbsPath");
        CheckArg.isNotEmpty(destAbsPath, "destAbsPath");

        // Create the paths ...
        PathFactory factory = context.getValueFactories().getPathFactory();
        Path srcPath = null;
        Path destPath = null;
        try {
            srcPath = factory.create(srcAbsPath);
        } catch (ValueFormatException e) {
            throw new RepositoryException(JcrI18n.invalidPathParameter.text(srcAbsPath, "srcAbsPath"), e);
        }
        try {
            destPath = factory.create(destAbsPath);
        } catch (ValueFormatException e) {
            throw new RepositoryException(JcrI18n.invalidPathParameter.text(destAbsPath, "destAbsPath"), e);
        }

        // Perform the copy operation, but use the "to" form (not the "into", which takes the parent) ...
        graph.copy(srcPath).to(destPath);
    }

    /**
     * {@inheritDoc}
     */
    public void copy( String srcWorkspace,
                      String srcAbsPath,
                      String destAbsPath ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Workspace#getImportContentHandler(java.lang.String, int)
     */
    @SuppressWarnings( "unused" )
    public ContentHandler getImportContentHandler( String parentAbsPath,
                                                   int uuidBehavior )
        throws PathNotFoundException, ConstraintViolationException, VersionException, LockException, AccessDeniedException,
        RepositoryException {
        CheckArg.isNotEmpty(parentAbsPath, "parentAbsPath");
        // Create a graph importer, which can return the content handler that can be used by the caller
        // to call the handler's event methods to create content...
        GraphImporter importer = new GraphImporter(graph);
        Path parentPath = context.getValueFactories().getPathFactory().create(parentAbsPath);
        return importer.getHandlerForImportingXml(Location.create(parentPath), false);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Workspace#importXML(java.lang.String, java.io.InputStream, int)
     */
    @SuppressWarnings( "unused" )
    public void importXML( String parentAbsPath,
                           InputStream in,
                           int uuidBehavior )
        throws IOException, PathNotFoundException, ItemExistsException, ConstraintViolationException,
        InvalidSerializedDataException, LockException, AccessDeniedException, RepositoryException {
        // try {
        // graph.importXmlFrom(in).into(parentAbsPath);
        // } catch (org.jboss.dna.graph.property.PathNotFoundException e) {
        // throw new PathNotFoundException(e.getMessage(), e);
        // } catch (SAXException err) {
        // }
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Workspace#move(java.lang.String, java.lang.String)
     */
    @SuppressWarnings( "unused" )
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
            throw new RepositoryException(JcrI18n.invalidPathParameter.text(srcAbsPath, "srcAbsPath"), e);
        }
        try {
            destPath = factory.create(destAbsPath);
        } catch (ValueFormatException e) {
            throw new RepositoryException(JcrI18n.invalidPathParameter.text(destAbsPath, "destAbsPath"), e);
        }

        // Perform the copy operation, but use the "to" form (not the "into", which takes the parent) ...
        // graph.move(srcPath).to(destPath);
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void restore( Version[] versions,
                         boolean removeExisting ) {
        throw new UnsupportedOperationException();
    }

    /**
     * Local implementation of @{link NodeTypeManager}. Initialized with {@link NodeType} source data when it is created (in the
     * {@link JcrWorkspace} constructor.
     */
    @NotThreadSafe
    private class JcrNodeTypeManager implements NodeTypeManager {

        private final Map<Name, JcrNodeType> primaryNodeTypes;
        private final Map<Name, JcrNodeType> mixinNodeTypes;

        private JcrNodeTypeManager( JcrNodeTypeSource source ) {
            Collection<JcrNodeType> primary = source.getPrimaryNodeTypes();
            Collection<JcrNodeType> mixins = source.getMixinNodeTypes();

            primaryNodeTypes = new HashMap<Name, JcrNodeType>(primary.size());
            for (JcrNodeType nodeType : primary) {
                primaryNodeTypes.put(nodeType.getInternalName(), nodeType);
            }

            mixinNodeTypes = new HashMap<Name, JcrNodeType>(mixins.size());
            for (JcrNodeType nodeType : mixins) {
                mixinNodeTypes.put(nodeType.getInternalName(), nodeType);
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.nodetype.NodeTypeManager#getAllNodeTypes()
         */
        public NodeTypeIterator getAllNodeTypes() {

            // TODO: Can revisit this approach later if it becomes a performance issue
            /*
             * Note also that this creates a subtle difference in behavior for concurrent modification
             * between this method and the specific get*NodeTypes methods.  That is, if a type is added
             * while an iterator from the corresponding specific get*NodeType method is being traversed,
             * a ConcurrentModificationException will be thrown.  Because this iterator is based on a copy
             * of the underlying maps, no exception would be thrown in the same case.
             */

            List<NodeType> allTypes = new ArrayList<NodeType>(primaryNodeTypes.size() + mixinNodeTypes.size());
            allTypes.addAll(primaryNodeTypes.values());
            allTypes.addAll(mixinNodeTypes.values());
            return new JcrNodeTypeIterator(allTypes);
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.nodetype.NodeTypeManager#getMixinNodeTypes()
         */
        public NodeTypeIterator getMixinNodeTypes() {
            return new JcrNodeTypeIterator(mixinNodeTypes.values());
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.nodetype.NodeTypeManager#getNodeType(java.lang.String)
         */
        @SuppressWarnings( "synthetic-access" )
        public NodeType getNodeType( String nodeTypeName ) throws NoSuchNodeTypeException, RepositoryException {
            Name ntName = session.getExecutionContext().getValueFactories().getNameFactory().create(nodeTypeName);

            NodeType nodeType = primaryNodeTypes.get(ntName);

            if (nodeType != null) {
                return nodeType;
            }

            nodeType = mixinNodeTypes.get(ntName);

            if (nodeType != null) {
                return nodeType;
            }

            throw new NoSuchNodeTypeException(JcrI18n.typeNotFound.text(nodeTypeName));
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.nodetype.NodeTypeManager#getPrimaryNodeTypes()
         */
        public NodeTypeIterator getPrimaryNodeTypes() {
            return new JcrNodeTypeIterator(primaryNodeTypes.values());
        }

    }

}
