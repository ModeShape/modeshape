/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in ModeShape is licensed
 * to you under the terms of the GNU Lesser General Public License as
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

import java.lang.ref.SoftReference;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.VersionException;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.util.Logger;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.Location;
import org.modeshape.graph.Graph.Batch;
import org.modeshape.graph.connector.RepositorySourceException;
import org.modeshape.graph.property.Binary;
import org.modeshape.graph.property.BinaryFactory;
import org.modeshape.graph.property.DateTime;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NameFactory;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.PropertyFactory;
import org.modeshape.graph.property.UuidFactory;
import org.modeshape.graph.property.ValueFactories;
import org.modeshape.graph.property.ValueFactory;
import org.modeshape.graph.property.ValueFormatException;
import org.modeshape.graph.request.InvalidWorkspaceException;
import org.modeshape.graph.session.GraphSession;
import org.modeshape.graph.session.InvalidStateException;
import org.modeshape.graph.session.ValidationException;
import org.modeshape.graph.session.GraphSession.Node;
import org.modeshape.graph.session.GraphSession.NodeId;
import org.modeshape.graph.session.GraphSession.PropertyInfo;
import org.modeshape.graph.session.GraphSession.Status;
import org.modeshape.jcr.JcrRepository.Option;

/**
 * The class that manages the session's information that has been locally-cached after reading from the underlying {@link Graph
 * repository} or modified by the session but not yet saved or commited to the repository.
 * <p>
 * The cached information is broken into several different categories that are each described below.
 * </p>
 * <h3>JCR API objects</h3>
 * <p>
 * Clients using the ModeShape JCR implementation obtain a {@link JcrSession JCR Session} (which generally owns this cache
 * instance) as well as the JCR {@link JcrNode Node} and {@link AbstractJcrProperty Property} instances. This cache ensures that
 * the same JCR Node or Property objects are always returned for the same item in the repository, ensuring that the "==" operator
 * always holds true for the same item. However, as soon as all (client) references to these objects are garbage collected, this
 * class is free to also release those objects and, when needed, recreate new implementation objects.
 * </p>
 * <p>
 * This approach helps reduce memory utilization since any unused items are available for garbage collection, but it also
 * guarantees that once a client maintains a reference to an item, the same Java object will always be used for any references to
 * that item.
 * </p>
 * <h3>Cached nodes</h3>
 * <p>
 * The session cache is also responsible for maintaining a local cache of node information retrieved from the underlying
 * repository, reducing the need to request information any more than necessary. This information includes that obtained directly
 * from the repository store, including node properties, children, and references to the parent. It also includes computed
 * information, such as the NodeDefinition for a node, the name of the primary type and mixin types, and the original
 * {@link Location} of the node in the repository.
 * </p>
 * <h3>Transient changes</h3>
 * <p>
 * Any time content is changed in the session, those changes are held within the session until they are saved either by
 * {@link Session#save() saving the session} or {@link Item#save() saving an individual item} (which includes any content below
 * that item). This cache maintains all these transient changes, and when requested will send the change requests down the
 * repository. At any point, these transient changes may be rolled back (or "released"), again either for the
 * {@link Session#refresh(boolean) whole session} or for {@link Item#refresh(boolean) individual items}.
 * </p>
 */
@ThreadSafe
class SessionCache {

    /**
     * Hidden flag that controls whether properties that appear on ModeShape nodes but not allowed by the node type or mixins
     * should be included anyway. This is currently {@value} .
     */
    protected static final boolean INCLUDE_PROPERTIES_NOT_ALLOWED_BY_NODE_TYPE_OR_MIXINS = true;

    protected static final Set<Name> EMPTY_NAMES = Collections.emptySet();

    private final JcrSession session;
    private final String workspaceName;
    protected final ExecutionContext context;
    protected final ValueFactories factories;
    protected final PathFactory pathFactory;
    protected final NameFactory nameFactory;
    protected final ValueFactory<String> stringFactory;
    protected final NamespaceRegistry namespaces;
    protected final PropertyFactory propertyFactory;
    private final Graph store;
    protected final Name defaultPrimaryTypeName;
    protected final Property defaultPrimaryTypeProperty;
    protected final Path rootPath;
    protected final Name residualName;

    private final CustomGraphSession graphSession;

    public SessionCache( JcrSession session ) {
        this(session, session.workspace().getName(), session.getExecutionContext(), session.nodeTypeManager(), session.graph());
    }

    public SessionCache( JcrSession session,
                         String workspaceName,
                         ExecutionContext context,
                         JcrNodeTypeManager nodeTypes,
                         Graph store ) {
        assert session != null;
        assert workspaceName != null;
        assert context != null;
        assert store != null;
        this.session = session;
        this.workspaceName = workspaceName;
        this.store = store;
        this.context = context;
        this.factories = context.getValueFactories();
        this.pathFactory = this.factories.getPathFactory();
        this.nameFactory = this.factories.getNameFactory();
        this.stringFactory = context.getValueFactories().getStringFactory();
        this.namespaces = context.getNamespaceRegistry();
        this.propertyFactory = context.getPropertyFactory();
        this.defaultPrimaryTypeName = JcrNtLexicon.UNSTRUCTURED;
        this.defaultPrimaryTypeProperty = propertyFactory.create(JcrLexicon.PRIMARY_TYPE, this.defaultPrimaryTypeName);
        this.rootPath = pathFactory.createRootPath();
        this.residualName = nameFactory.create(JcrNodeType.RESIDUAL_ITEM_NAME);

        // Create the graph session, customized for JCR ...
        this.graphSession = new CustomGraphSession(this.store, this.workspaceName, new JcrNodeOperations(), new JcrAuthorizer());
        // Set the read-depth if we can...
        try {
            int depth = Integer.parseInt(session.repository().getOptions().get(Option.READ_DEPTH));
            if (depth > 0) this.graphSession.setDepthForLoadingNodes(depth);
        } catch (RuntimeException e) {
        }
    }

    protected class CustomGraphSession extends GraphSession<JcrNodePayload, JcrPropertyPayload> {
        CustomGraphSession( Graph graph,
                            String workspaceName,
                            Operations<JcrNodePayload, JcrPropertyPayload> nodeOperations,
                            Authorizer authorizer ) {
            super(graph, workspaceName, nodeOperations, authorizer);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.session.GraphSession#operations()
         */
        @Override
        protected Batch operations() {
            return super.operations();
        }
    }

    final GraphSession<JcrNodePayload, JcrPropertyPayload> graphSession() {
        return graphSession;
    }

    Graph.Batch currentBatch() {
        return graphSession.operations();
    }

    JcrSession session() {
        return session;
    }

    String workspaceName() {
        return workspaceName;
    }

    String sourceName() {
        return store.getSourceName();
    }

    ExecutionContext context() {
        return context;
    }

    ValueFactories factories() {
        return factories;
    }

    PathFactory pathFactory() {
        return pathFactory;
    }

    NameFactory nameFactory() {
        return nameFactory;
    }

    UuidFactory uuidFactory() {
        return factories.getUuidFactory();
    }

    ValueFactory<String> stringFactory() {
        return factories.getStringFactory();
    }

    JcrNodeTypeManager nodeTypes() {
        return session.nodeTypeManager();
    }

    final String readable( Name name ) {
        return name.getString(namespaces);
    }

    final String readable( Path.Segment segment ) {
        return segment.getString(namespaces);
    }

    final String readable( Path path ) {
        return path.getString(namespaces);
    }

    final String readable( Location location ) {
        return location.getString(namespaces);
    }

    final String readable( Iterable<Name> names ) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        boolean first = true;
        for (Name name : names) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append(name.getString(namespaces));
        }
        sb.append(']');
        return sb.toString();
    }

    /**
     * Returns whether the session cache has any pending changes that need to be executed.
     * 
     * @return true if there are pending changes, or false if there is currently no changes
     */
    boolean hasPendingChanges() {
        return graphSession.hasPendingChanges();
    }

    /**
     * Refreshes (removes the cached state) for all cached nodes.
     * <p>
     * If {@code keepChanges == true}, modified nodes will not have their state refreshed.
     * </p>
     * 
     * @param keepChanges indicates whether changed nodes should be kept or refreshed from the repository.
     */
    public void refresh( boolean keepChanges ) {
        graphSession.refresh(keepChanges);
    }

    /**
     * Refreshes (removes the cached state) for the node with the given UUID and any of its descendants.
     * <p>
     * If {@code keepChanges == true}, modified nodes will not have their state refreshed.
     * </p>
     * 
     * @param nodeId the identifier of the node that is to be saved; may not be null
     * @param absolutePath the absolute path to the node; may not be null
     * @param keepChanges indicates whether changed nodes should be kept or refreshed from the repository.
     * @throws InvalidItemStateException if the node being refreshed no longer exists
     * @throws RepositoryException if any error resulting while saving the changes to the repository
     */
    public void refresh( NodeId nodeId,
                         Path absolutePath,
                         boolean keepChanges ) throws InvalidItemStateException, RepositoryException {
        assert nodeId != null;
        try {
            Node<JcrNodePayload, JcrPropertyPayload> node = graphSession.findNodeWith(nodeId, absolutePath);
            graphSession.refresh(node, keepChanges);
        } catch (InvalidStateException e) {
            throw new InvalidItemStateException(e.getLocalizedMessage());
        } catch (org.modeshape.graph.property.PathNotFoundException e) {
            throw new InvalidItemStateException(e.getLocalizedMessage());
        } catch (RepositorySourceException e) {
            throw new RepositoryException(e.getLocalizedMessage());
        }
    }

    /**
     * Refreshes the properties for the node with the given UUID.
     * 
     * @param location the location of the node that is to be refreshed; may not be null
     * @throws InvalidItemStateException if the node being refreshed no longer exists
     * @throws RepositoryException if any error resulting while saving the changes to the repository
     */
    public void refreshProperties( Location location ) throws InvalidItemStateException, RepositoryException {
        assert location != null;
        try {
            Node<JcrNodePayload, JcrPropertyPayload> node = graphSession.findNodeWith(location);

            graphSession.refreshProperties(node);
        } catch (InvalidStateException e) {
            throw new InvalidItemStateException(e.getLocalizedMessage());
        } catch (org.modeshape.graph.property.PathNotFoundException e) {
            throw new InvalidItemStateException(e.getLocalizedMessage());
        } catch (RepositorySourceException e) {
            throw new RepositoryException(e.getLocalizedMessage());
        }
    }

    /**
     * Find the best definition for the child node with the given name on the node with the given UUID.
     * 
     * @param parent the parent node; may not be null
     * @param newNodeName the name of the potential new child node; may not be null
     * @param newNodePrimaryTypeName the primary type of the potential new child node; may not be null
     * @return the definition that best fits the new node name and type
     * @throws ItemExistsException if there is no definition that allows same-name siblings for the name and type and the parent
     *         node already has a child node with the given name
     * @throws ConstraintViolationException if there is no definition for the name and type among the parent node's primary and
     *         mixin types
     * @throws RepositoryException if any other error occurs
     */
    protected JcrNodeDefinition findBestNodeDefinition( Node<JcrNodePayload, JcrPropertyPayload> parent,
                                                        Name newNodeName,
                                                        Name newNodePrimaryTypeName )
        throws ItemExistsException, ConstraintViolationException, RepositoryException {
        assert parent != null;
        assert newNodeName != null;

        Name primaryTypeName = parent.getPayload().getPrimaryTypeName();
        List<Name> mixinTypeNames = parent.getPayload().getMixinTypeNames();

        // Need to add one to speculate that this node will be added
        int snsCount = parent.getChildrenCount(newNodeName) + 1;
        JcrNodeDefinition definition = nodeTypes().findChildNodeDefinition(primaryTypeName,
                                                                           mixinTypeNames,
                                                                           newNodeName,
                                                                           newNodePrimaryTypeName,
                                                                           snsCount,
                                                                           true);
        if (definition == null) {
            if (snsCount > 1) {
                definition = nodeTypes().findChildNodeDefinition(primaryTypeName,
                                                                 mixinTypeNames,
                                                                 newNodeName,
                                                                 newNodePrimaryTypeName,
                                                                 1,
                                                                 true);

                if (definition != null) {
                    throw new ItemExistsException(JcrI18n.noSnsDefinition.text(readable(newNodeName),
                                                                               readable(parent.getPath()),
                                                                               readable(primaryTypeName),
                                                                               readable(mixinTypeNames)));
                }
            }

            throw new ConstraintViolationException(JcrI18n.noDefinition.text("child node",
                                                                             readable(newNodeName),
                                                                             readable(parent.getPath()),
                                                                             readable(primaryTypeName),
                                                                             readable(mixinTypeNames)));
        }

        return definition;
    }

    /**
     * Save any changes that have been accumulated by this session.
     * 
     * @throws IllegalArgumentException if the identifier and path are both node
     * @throws ItemNotFoundException if a node with the supplied identifier and path could not be found
     * @throws AccessDeniedException if the caller does not have privilege to perform the operation
     * @throws ConstraintViolationException if there was a constraint violation
     * @throws RepositoryException if any error resulting while saving the changes to the repository
     */
    public void save() throws ItemNotFoundException, AccessDeniedException, ConstraintViolationException, RepositoryException {
        try {
            graphSession.save();
        } catch (ValidationException e) {
            throw new ConstraintViolationException(e.getLocalizedMessage(), e);
        } catch (InvalidStateException e) {
            throw new InvalidItemStateException(e.getLocalizedMessage(), e);
        } catch (RepositorySourceException e) {
            throw new RepositoryException(e.getLocalizedMessage(), e);
        } catch (AccessControlException e) {
            throw new AccessDeniedException(e.getMessage(), e);
        }
    }

    /**
     * Save any changes to the identified node or its descendants. The supplied node may not have been deleted or created in this
     * session since the last save operation.
     * 
     * @param nodeId the identifier of the node that is to be saved; may not be null
     * @param absolutePath the absolute path to the node; may not be null
     * @throws IllegalArgumentException if the identifier and path are both node
     * @throws ItemNotFoundException if a node with the supplied identifier and path could not be found
     * @throws AccessDeniedException if the caller does not have privilege to perform the operation
     * @throws ConstraintViolationException if there was a constraint violation
     * @throws RepositoryException if any error resulting while saving the changes to the repository
     */
    public void save( NodeId nodeId,
                      Path absolutePath )
        throws ItemNotFoundException, AccessDeniedException, ConstraintViolationException, RepositoryException {
        assert nodeId != null;
        try {
            Node<JcrNodePayload, JcrPropertyPayload> node = graphSession.findNodeWith(nodeId, absolutePath);
            assert node != null;
            graphSession.save(node);
        } catch (ValidationException e) {
            throw new ConstraintViolationException(e.getLocalizedMessage(), e);
        } catch (InvalidStateException e) {
            throw new InvalidItemStateException(e.getLocalizedMessage(), e);
        } catch (RepositorySourceException e) {
            throw new RepositoryException(e.getLocalizedMessage(), e);
        } catch (AccessControlException e) {
            throw new AccessDeniedException(e.getMessage(), e);
        }
    }

    /**
     * Find the session's node for the given identifier and path.
     * 
     * @param id the identifier for the node
     * @param absolutePath the absolute path to the node; may not be null
     * @return the existing node implementation
     * @throws IllegalArgumentException if the identifier and path are both node
     * @throws ItemNotFoundException if a node with the supplied identifier and path could not be found
     * @throws AccessDeniedException if the caller does not have privilege to read the node
     * @throws RepositoryException if an error resulting in finding this node in the repository
     */
    public Node<JcrNodePayload, JcrPropertyPayload> findNode( NodeId id,
                                                              Path absolutePath )
        throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        try {
            return graphSession.findNodeWith(id, absolutePath);
        } catch (org.modeshape.graph.property.PathNotFoundException e) {
            throw new ItemNotFoundException(e.getMessage(), e);
        } catch (RepositorySourceException e) {
            throw new RepositoryException(e.getMessage(), e);
        } catch (AccessControlException e) {
            throw new AccessDeniedException(e.getMessage(), e);
        }
    }

    /**
     * Find the session's node for the given location.
     * 
     * @param location the location for the node
     * @return the existing node implementation
     * @throws IllegalArgumentException if the identifier and path are both node
     * @throws ItemNotFoundException if a node with the supplied identifier and path could not be found
     * @throws AccessDeniedException if the caller does not have privilege to read the node
     * @throws RepositoryException if an error resulting in finding this node in the repository
     */
    public Node<JcrNodePayload, JcrPropertyPayload> findNodeWith( Location location )
        throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        try {
            return graphSession.findNodeWith(location);
        } catch (org.modeshape.graph.property.PathNotFoundException e) {
            throw new ItemNotFoundException(e.getMessage(), e);
        } catch (RepositorySourceException e) {
            throw new RepositoryException(e.getMessage(), e);
        } catch (AccessControlException e) {
            throw new AccessDeniedException(e.getMessage(), e);
        }
    }

    /**
     * Find the session's node for the given identifier and path.
     * 
     * @param from the identifier of the reference node; may be null if the root node is to be used as the reference
     * @param fromAbsolutePath the absolute path of the reference node; may not be null
     * @param relativePath the relative (but normalized) path from the reference node, but which may be an absolute (and
     *        normalized) path only when the reference node is the root node; may not be null
     * @return the existing node implementation
     * @throws IllegalArgumentException if the identifier and path are both node
     * @throws ItemNotFoundException if a node with the supplied identifier and path could not be found
     * @throws PathNotFoundException if the node given by the relative path does not exist
     * @throws AccessDeniedException if the caller does not have privilege to read the node
     * @throws RepositoryException if an error resulting in finding this node in the repository
     */
    public Node<JcrNodePayload, JcrPropertyPayload> findNode( NodeId from,
                                                              Path fromAbsolutePath,
                                                              Path relativePath )
        throws PathNotFoundException, ItemNotFoundException, AccessDeniedException, RepositoryException {
        // Find the reference node ...
        Node<JcrNodePayload, JcrPropertyPayload> referenceNode = findNode(from, fromAbsolutePath);
        try {
            return graphSession.findNodeRelativeTo(referenceNode, relativePath);
        } catch (org.modeshape.graph.property.PathNotFoundException e) {
            throw new PathNotFoundException(e.getMessage(), e);
        } catch (RepositorySourceException e) {
            throw new RepositoryException(e.getMessage(), e);
        } catch (AccessControlException e) {
            throw new AccessDeniedException(e.getMessage(), e);
        }
    }

    /**
     * Find the root node associated with this workspace.
     * 
     * @return the root node; never null
     * @throws RepositoryException if an error resulting in finding this node in the repository
     */
    public JcrRootNode findJcrRootNode() throws RepositoryException {
        return (JcrRootNode)graphSession.getRoot().getPayload().getJcrNode();
    }

    /**
     * Find the JCR {@link JcrNode Node implementation} for the given identifier and path.
     * 
     * @param location the location of the node
     * @return the existing node implementation
     * @throws IllegalArgumentException if the identifier and path are both node
     * @throws ItemNotFoundException if a node with the supplied identifier and path could not be found
     * @throws AccessDeniedException if the caller does not have privilege to read the node
     * @throws RepositoryException if an error resulting in finding this node in the repository
     */
    public AbstractJcrNode findJcrNode( Location location )
        throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        try {
            return graphSession.findNodeWith(location).getPayload().getJcrNode();
        } catch (org.modeshape.graph.property.PathNotFoundException e) {
            throw new ItemNotFoundException(e.getMessage(), e);
        } catch (RepositorySourceException e) {
            throw new RepositoryException(e.getMessage(), e);
        } catch (AccessControlException e) {
            throw new AccessDeniedException(e.getMessage(), e);
        }
    }

    /**
     * Find the JCR {@link JcrNode Node implementation} for the given identifier and path.
     * 
     * @param id the identifier for the node
     * @param absolutePath the absolute path to the node; may not be null
     * @return the existing node implementation
     * @throws IllegalArgumentException if the identifier and path are both node
     * @throws ItemNotFoundException if a node with the supplied identifier and path could not be found
     * @throws AccessDeniedException if the caller does not have privilege to read the node
     * @throws RepositoryException if an error resulting in finding this node in the repository
     */
    public AbstractJcrNode findJcrNode( NodeId id,
                                        Path absolutePath )
        throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        return findNode(id, absolutePath).getPayload().getJcrNode();
    }

    /**
     * Find the JCR {@link AbstractJcrNode Node implementation} for the node given by the UUID of a reference node and a relative
     * path from the reference node. The relative path should already have been {@link Path#getNormalizedPath() normalized}.
     * 
     * @param from the identifier of the reference node; may be null if the root node is to be used as the reference
     * @param fromAbsolutePath the absolute path of the reference node; may not be null
     * @param relativePath the relative (but normalized) path from the reference node, but which may be an absolute (and
     *        normalized) path only when the reference node is the root node; may not be null
     * @return the information for the referenced node; never null
     * @throws ItemNotFoundException if the reference node with the supplied identifier and path does not exist
     * @throws PathNotFoundException if the node given by the relative path does not exist
     * @throws InvalidItemStateException if the reference node has been deleted in this session
     * @throws AccessDeniedException if the caller does not have privilege to read the reference or result node
     * @throws RepositoryException if any other error occurs while reading information from the repository
     */
    public AbstractJcrNode findJcrNode( NodeId from,
                                        Path fromAbsolutePath,
                                        Path relativePath )
        throws ItemNotFoundException, PathNotFoundException, InvalidItemStateException, AccessDeniedException,
        RepositoryException {
        // Find the reference node ...
        Node<JcrNodePayload, JcrPropertyPayload> referenceNode = findNode(from, fromAbsolutePath);
        try {
            // Find the reference node ...
            return graphSession.findNodeRelativeTo(referenceNode, relativePath).getPayload().getJcrNode();
        } catch (org.modeshape.graph.property.PathNotFoundException e) {
            throw new PathNotFoundException(e.getMessage(), e);
        } catch (RepositorySourceException e) {
            throw new RepositoryException(e.getMessage(), e);
        } catch (AccessControlException e) {
            throw new AccessDeniedException(e.getMessage(), e);
        }
    }

    /**
     * Find the JCR {@link javax.jcr.Property} with the givenname on the node with the supplied ID and/or at the absolute path.
     * 
     * @param id the identifier of the node, or null if the path should be used
     * @param absolutePath the absolute path to the node; should not be null
     * @param propertyName the property name; may not be null
     * @return the property, or null if there is no such property
     * @throws PathNotFoundException if the node does not exist
     * @throws AccessDeniedException if the caller cannot read the property
     * @throws RepositoryException if there is a problem reading the node and/or property
     */
    public AbstractJcrProperty findJcrProperty( NodeId id,
                                                Path absolutePath,
                                                Name propertyName )
        throws PathNotFoundException, AccessDeniedException, RepositoryException {
        // Find the node that owns the property ...
        Node<JcrNodePayload, JcrPropertyPayload> node = findNode(id, absolutePath);
        PropertyInfo<JcrPropertyPayload> propertyInfo = node.getProperty(propertyName);
        if (propertyInfo != null) {
            if (propertyName.equals(JcrLexicon.UUID) && !isReferenceable(node)) return null;
            return propertyInfo.getPayload().getJcrProperty();
        }
        return null;
    }

    /**
     * Find the properties for the node given by the supplied identifier and/or path.
     * 
     * @param id the identifier of the node, or null if the path should be used
     * @param absolutePath the absolute path to the node; should not be null
     * @return an immutable snapshot of the properties in the node
     * @throws PathNotFoundException if the node does not exist
     * @throws AccessDeniedException if the caller cannot read the property
     * @throws RepositoryException if there is a problem reading the node and/or property
     */
    public Collection<AbstractJcrProperty> findJcrPropertiesFor( NodeId id,
                                                                 Path absolutePath )
        throws PathNotFoundException, AccessDeniedException, RepositoryException {
        try {
            Node<JcrNodePayload, JcrPropertyPayload> node = graphSession.findNodeWith(id, absolutePath);
            Collection<AbstractJcrProperty> result = new ArrayList<AbstractJcrProperty>(node.getPropertyCount());
            for (org.modeshape.graph.session.GraphSession.PropertyInfo<JcrPropertyPayload> property : node.getProperties()) {
                Name propertyName = property.getName();
                if (propertyName.equals(JcrLexicon.UUID) && !isReferenceable(node)) continue;
                if (!propertyName.getNamespaceUri().equals(ModeShapeIntLexicon.Namespace.URI)) {
                    AbstractJcrProperty prop = property.getPayload().getJcrProperty();
                    if (prop != null) result.add(prop);
                }
            }
            return result;
        } catch (org.modeshape.graph.property.PathNotFoundException e) {
            throw new PathNotFoundException(e.getMessage(), e);
        } catch (RepositorySourceException e) {
            throw new RepositoryException(e.getMessage(), e);
        } catch (AccessControlException e) {
            throw new AccessDeniedException(e.getMessage(), e);
        }
    }

    /**
     * Find the JCR {@link AbstractJcrItem Item implementation} for the node or property given by the UUID of a reference node and
     * a relative path from the reference node to the desired item. The relative path should already have been
     * {@link Path#getNormalizedPath() normalized}.
     * 
     * @param from the identifier of the reference node; may be null if the root node is to be used as the reference
     * @param fromAbsolutePath the absolute path of the reference node; may be null if the root node is to be used as the
     *        reference
     * @param relativePath the relative (but normalized) path from the reference node to the desired item, but which may be an
     *        absolute (and normalized) path only when the reference node is the root node; may not be null
     * @return the information for the referenced item; never null
     * @throws ItemNotFoundException if the reference node with the supplied UUID does not exist, or if an item given by the
     *         supplied relative path does not exist
     * @throws InvalidItemStateException if the node with the UUID has been deleted in this session
     * @throws RepositoryException if any other error occurs while reading information from the repository
     */
    public AbstractJcrItem findJcrItem( NodeId from,
                                        Path fromAbsolutePath,
                                        Path relativePath )
        throws ItemNotFoundException, InvalidItemStateException, RepositoryException {
        if (from == null && fromAbsolutePath == null) {
            from = graphSession.getRoot().getNodeId();
        }
        // A pathological case is an empty relative path ...
        if (relativePath.size() == 0) {
            return findJcrNode(from, fromAbsolutePath);
        }
        if (relativePath.size() == 1) {
            Path.Segment segment = relativePath.getLastSegment();
            if (segment.isSelfReference()) return findJcrNode(from, fromAbsolutePath);
            if (segment.isParentReference()) {
                return findJcrNode(from, fromAbsolutePath, relativePath);
            }
        }

        // Peek into the last segment of the path to see whether it uses a SNS index (and it's > 1) ...
        Path.Segment lastSegment = relativePath.getLastSegment();
        if (lastSegment.getIndex() > 1) {
            // Only nodes can have SNS index (but an index of 1 is the default)...
            return findJcrNode(from, fromAbsolutePath);
        }

        Node<JcrNodePayload, JcrPropertyPayload> fromNode = null;
        Node<JcrNodePayload, JcrPropertyPayload> parent = null;
        try {
            fromNode = graphSession.findNodeWith(from, fromAbsolutePath);
            if (from == null) from = fromNode.getNodeId();
            assert from != null;
            if (relativePath.size() == 1) {
                // The referenced node must be the parent ...
                parent = fromNode;
            } else {
                // We know that the parent of the referenced item should be a node (if the path is right) ...
                parent = graphSession.findNodeRelativeTo(fromNode, relativePath.getParent());
            }
        } catch (org.modeshape.graph.property.PathNotFoundException e) {
            throw new ItemNotFoundException(e.getMessage(), e);
        } catch (RepositorySourceException e) {
            throw new RepositoryException(e.getMessage(), e);
        } catch (AccessControlException e) {
            throw new AccessDeniedException(e.getMessage(), e);
        }

        // JSR-170 doesn't allow children and proeprties to have the same name, but this is relaxed in JSR-283.
        // But JSR-283 Section 3.3.4 states "The method Session.getItem will return the item at the specified path
        // if there is only one such item, if there is both a node and a property at the specified path, getItem
        // will return the node." Therefore, look for a child first ...
        if (parent.hasChild(lastSegment)) {
            // There is a child!
            Node<JcrNodePayload, JcrPropertyPayload> child = parent.getChild(lastSegment);
            return child.getPayload().getJcrNode();
        }

        // Otherwise it should be a property ...
        org.modeshape.graph.session.GraphSession.PropertyInfo<JcrPropertyPayload> propertyInfo = parent.getProperty(lastSegment.getName());
        if (propertyInfo != null) {
            return propertyInfo.getPayload().getJcrProperty();
        }

        // It was not found, so prepare a good exception message ...
        String msg = null;
        if (from.equals(graphSession.getRoot().getNodeId())) {
            // The reference node was the root, so use this fact to convert the path to an absolute path in the message
            Path absolutePath = rootPath.resolve(relativePath);
            msg = JcrI18n.itemNotFoundAtPath.text(readable(absolutePath), workspaceName);
        } else {
            // Find the path of the reference node ...
            Path referenceNodePath = fromNode.getPath();
            msg = JcrI18n.itemNotFoundAtPathRelativeToReferenceNode.text(readable(relativePath),
                                                                         readable(referenceNodePath),
                                                                         workspaceName);
        }
        throw new ItemNotFoundException(msg);
    }

    /**
     * Determine whether the node's primary type or any of the mixins are or extend the node type with the supplied name. This
     * method is semantically equivalent to but slightly more efficient than the {@link javax.jcr.Node#isNodeType(String)
     * equivalent in the JCR API}.
     * 
     * @param node the node to be evaluated
     * @param nodeType the name of the node type
     * @return true if this node is of the node type given by the supplied name, or false otherwise
     * @throws RepositoryException if there is an exception
     */
    public final boolean isNodeType( Node<JcrNodePayload, JcrPropertyPayload> node,
                                     Name nodeType ) throws RepositoryException {
        Name primaryTypeName = node.getPayload().getPrimaryTypeName();
        JcrNodeType primaryType = nodeTypes().getNodeType(primaryTypeName);
        if (primaryType.isNodeType(nodeType)) {
            return true;
        }
        JcrNodeTypeManager nodeTypes = session().nodeTypeManager();
        for (Name mixinTypeName : node.getPayload().getMixinTypeNames()) {
            JcrNodeType mixinType = nodeTypes.getNodeType(mixinTypeName);
            if (mixinType != null && mixinType.isNodeType(nodeType)) {
                return true;
            }
        }
        return false;
    }

    public boolean isReferenceable( Node<JcrNodePayload, JcrPropertyPayload> node ) throws RepositoryException {
        return isNodeType(node, JcrMixLexicon.REFERENCEABLE);
    }

    public boolean isVersionable( Node<JcrNodePayload, JcrPropertyPayload> node ) throws RepositoryException {
        return isNodeType(node, JcrMixLexicon.VERSIONABLE);
    }

    /**
     * Obtain an {@link NodeEditor editor} that can be used to manipulate the properties or children on the node identified by the
     * supplied identifier and path. The node must exist prior to this call, either as a node that exists in the workspace or as a
     * node that was created within this session but not yet persisted to the workspace. This method returns an editor that
     * batches all changes in transient storage from where they can be persisted to the repository by
     * {@link javax.jcr.Session#save() saving the session} or by {@link javax.jcr.Item#save() saving an ancestor}.
     * 
     * @param node the node
     * @return the editor; never null
     */
    public NodeEditor getEditorFor( Node<JcrNodePayload, JcrPropertyPayload> node ) {
        return new NodeEditor(node);
    }

    /**
     * Obtain an {@link NodeEditor editor} that can be used to manipulate the properties or children on the node identified by the
     * supplied identifier and path. The node must exist prior to this call, either as a node that exists in the workspace or as a
     * node that was created within this session but not yet persisted to the workspace. This method returns an editor that
     * batches all changes in transient storage from where they can be persisted to the repository by
     * {@link javax.jcr.Session#save() saving the session} or by {@link javax.jcr.Item#save() saving an ancestor}.
     * 
     * @param id the identifier for the node
     * @param absolutePath the absolute path to the node; may not be null
     * @return the editor; never null
     * @throws ItemNotFoundException if no such node could be found in the session or workspace
     * @throws AccessDeniedException if the caller does not have privilege to read the reference or result node
     * @throws InvalidItemStateException if the item has been marked for deletion within this session
     * @throws RepositoryException if any other error occurs while reading information from the repository
     */
    public NodeEditor getEditorFor( NodeId id,
                                    Path absolutePath )
        throws ItemNotFoundException, AccessDeniedException, InvalidItemStateException, RepositoryException {
        Node<JcrNodePayload, JcrPropertyPayload> node = this.graphSession.findNodeWith(id, absolutePath);
        return new NodeEditor(node);
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
     * @param workspaceName the name of the workspace
     * @param uuid the UUID of the corresponding node, or the UUID of the closest ancestor that is referenceable
     * @param relativePath the relative path from the referenceable node, or null if the supplied UUID identifies the
     *        corresponding node
     * @return the absolute path to the corresponding node in the workspace; never null
     * @throws NoSuchWorkspaceException if the specified workspace does not exist
     * @throws ItemNotFoundException if no corresponding node exists
     * @throws AccessDeniedException if the current session does not have sufficient rights to perform this operation
     * @throws RepositoryException if another exception occurs
     */
    Path getPathForCorrespondingNode( String workspaceName,
                                      UUID uuid,
                                      Path relativePath )
        throws NoSuchWorkspaceException, AccessDeniedException, ItemNotFoundException, RepositoryException {
        assert workspaceName != null;
        assert uuid != null || relativePath != null;

        try {
            try {
                store.useWorkspace(workspaceName);
            } catch (InvalidWorkspaceException iwe) {
                throw new NoSuchWorkspaceException(JcrI18n.workspaceNameIsInvalid.text(store.getSourceName(), workspaceName));
            }
            org.modeshape.graph.Node node;
            if (uuid != null) {
                node = store.getNodeAt(uuid);

                if (relativePath != null) {
                    Path nodePath = node.getLocation().getPath();
                    Path absolutePath = relativePath.resolveAgainst(nodePath);
                    node = store.getNodeAt(absolutePath);
                }

            } else {
                Path absolutePath = pathFactory.createAbsolutePath(relativePath.getSegmentsList());
                node = store.getNodeAt(absolutePath);
            }
            assert node != null;

            Path path = node.getLocation().getPath();
            try {
                this.session().checkPermission(workspaceName, path, "read");
            } catch (AccessControlException ace) {
                throw new AccessDeniedException(ace);
            }
            return path;
        } catch (org.modeshape.graph.property.PathNotFoundException pnfe) {
            throw new ItemNotFoundException(pnfe);
        } finally {
            store.useWorkspace(this.workspaceName);
        }
    }

    /**
     * An interface used to manipulate a node's properties and children.
     */
    public final class NodeEditor {
        private final Node<JcrNodePayload, JcrPropertyPayload> node;

        protected NodeEditor( Node<JcrNodePayload, JcrPropertyPayload> node ) {
            this.node = node;
        }

        protected Node<JcrNodePayload, JcrPropertyPayload> node() {
            return node;
        }

        /**
         * Checks whether there is an existing property with this name that does not match the given cardinality. If such a
         * property exists, a {@code javax.jcr.ValueFormatException} is thrown, as per section 10.4.2.6 of the JCR 2.0
         * specification
         * 
         * @param propertyName the name of the property
         * @param isMultiple whether the property must have multiple values
         * @throws javax.jcr.ValueFormatException if the property exists but has the opposite cardinality
         * @throws RepositoryException if any other error occurs
         */
        private void checkCardinalityOfExistingProperty( Name propertyName,
                                                         boolean isMultiple )
            throws javax.jcr.ValueFormatException, RepositoryException {
            // Check for existing single-valued property - can't set multiple values on single-valued property
            PropertyInfo<JcrPropertyPayload> propInfo = this.node.getProperty(propertyName);
            if (propInfo != null && propInfo.isMultiValued() != isMultiple) {
                String workspaceName = SessionCache.this.workspaceName();
                String propName = readable(propertyName);
                String path = readable(node.getPath());
                if (isMultiple) {
                    I18n msg = JcrI18n.unableToSetSingleValuedPropertyUsingMultipleValues;
                    throw new javax.jcr.ValueFormatException(msg.text(propName, path, workspaceName));
                }
                I18n msg = JcrI18n.unableToSetMultiValuedPropertyUsingSingleValue;
                throw new javax.jcr.ValueFormatException(msg.text(propName, path, workspaceName));
            }

        }

        /**
         * @return if this node (or its nearest versionable ancestor) is checked out.
         * @throws RepositoryException if there is an error accessing the repository
         * @see javax.jcr.Node#isCheckedOut()
         */
        boolean isCheckedOut() throws RepositoryException {
            for (Node<JcrNodePayload, JcrPropertyPayload> curr = node; curr.getParent() != null; curr = curr.getParent()) {
                if (isNodeType(curr, JcrMixLexicon.VERSIONABLE)) {
                    PropertyInfo<JcrPropertyPayload> prop = curr.getProperty(JcrLexicon.IS_CHECKED_OUT);

                    // This prop can only be null if the node has not been saved since it was made versionable.
                    return prop == null || prop.getPayload().getJcrProperty().getBoolean();
                }
            }

            return true;
        }

        /**
         * Set the value for the property. If the property does not exist, it will be added. If the property does exist, the
         * existing values will be replaced with the supplied value.
         * 
         * @param name the property name; may not be null
         * @param value the new property values; may not be null
         * @return the JCR property object for the property; never null
         * @throws ConstraintViolationException if the property could not be set because of a node type constraint or property
         *         definition constraint
         * @throws AccessDeniedException if the current session does not have the requisite privileges to perform this task
         * @throws RepositoryException if any other error occurs
         */
        public AbstractJcrProperty setProperty( Name name,
                                                JcrValue value )
            throws AccessDeniedException, ConstraintViolationException, RepositoryException {
            return setProperty(name, value, true, false);
        }

        /**
         * Set the value for the property. If the property does not exist, it will be added. If the property does exist, the
         * existing values will be replaced with the supplied value. Protected property definitions may be considered, based on
         * the {@code skipProtected} flag.
         * 
         * @param name the property name; may not be null
         * @param value the new property values; may not be null
         * @param skipProtected indicates whether protected property definitions should be ignored
         * @param skipReferenceValidation indicates whether constraints on REFERENCE properties should be enforced
         * @return the JCR property object for the property; never null
         * @throws ConstraintViolationException if the property could not be set because of a node type constraint or property
         *         definition constraint
         * @throws AccessDeniedException if the current session does not have the requisite privileges to perform this task
         * @throws VersionException if this node is not checked out
         * @throws RepositoryException if any other error occurs
         */
        public AbstractJcrProperty setProperty( Name name,
                                                JcrValue value,
                                                boolean skipProtected,
                                                boolean skipReferenceValidation )
            throws AccessDeniedException, ConstraintViolationException, VersionException, RepositoryException {
            assert name != null;
            assert value != null;

            /*
             * Skip this check for protected nodes.  They can't be modified by users and, in some cases (e.g., jcr:isLocked),
             * may be able to be modified for checked-in nodes.
             */
            if (!isCheckedOut() && skipProtected) {
                String path = node.getLocation().getPath().getString(context().getNamespaceRegistry());
                throw new VersionException(JcrI18n.nodeIsCheckedIn.text(path));
            }

            checkCardinalityOfExistingProperty(name, false);

            JcrPropertyDefinition definition = null;

            // Look for an existing property ...
            PropertyInfo<JcrPropertyPayload> existing = node.getProperty(name);
            if (existing != null) {

                // We're replacing an existing property, but we still need to check that the property definition
                // (still) defines a type. So, find the property definition for the existing property ...
                definition = nodeTypes().getPropertyDefinition(existing.getPayload().getPropertyDefinitionId());

                if (definition != null) {
                    // The definition's require type must match the value's ...
                    if (definition.getRequiredType() != PropertyType.UNDEFINED && definition.getRequiredType() != value.getType()) {
                        // The property type is not right, so we have to check if we can cast.
                        // It's easier and could save more work if we just find a new property definition that works ...
                        definition = null;
                    } else {
                        // The types match, so see if the value satisfies the constraints ...
                        if (!definition.satisfiesConstraints(value)) definition = null;
                    }
                }
            }
            JcrNodePayload payload = node.getPayload();
            if (definition == null) {
                // Look for a definition ...
                definition = nodeTypes().findPropertyDefinition(payload.getPrimaryTypeName(),
                                                                payload.getMixinTypeNames(),
                                                                name,
                                                                value,
                                                                true,
                                                                skipProtected);
                /*
                 * findPropertyDefinition checks constraints for all property types except REFERENCE.  To avoid unnecessary loading of nodes,
                 * REFERENCE constraints are only checked when the property is first set.
                 */
                boolean referencePropMissedConstraints = skipReferenceValidation && definition != null
                                                         && definition.getRequiredType() == PropertyType.REFERENCE
                                                         && !definition.canCastToTypeAndSatisfyConstraints(value);
                if (definition == null || referencePropMissedConstraints) {
                    throw new ConstraintViolationException(JcrI18n.noDefinition.text("property",
                                                                                     readable(name),
                                                                                     readable(node.getPath()),
                                                                                     readable(payload.getPrimaryTypeName()),
                                                                                     readable(payload.getMixinTypeNames())));
                }
            } else {
                // Check that the existing definition isn't protected
                if (skipProtected && definition.isProtected()) throw new ConstraintViolationException(
                                                                                                      JcrI18n.noDefinition.text("property",
                                                                                                                                readable(name),
                                                                                                                                readable(node.getPath()),
                                                                                                                                readable(payload.getPrimaryTypeName()),
                                                                                                                                readable(payload.getMixinTypeNames())));
            }
            // Create the ModeShape property ...
            Object objValue = value.value();
            int propertyType = definition.getRequiredType();
            if (propertyType == PropertyType.UNDEFINED || propertyType == value.getType()) {
                // Can use the values as is ...
                propertyType = value.getType();
            } else {
                // A cast is required ...
                org.modeshape.graph.property.PropertyType dnaPropertyType = PropertyTypeUtil.dnaPropertyTypeFor(propertyType);
                ValueFactory<?> factory = factories().getValueFactory(dnaPropertyType);
                objValue = factory.create(objValue);
            }
            Property dnaProp = propertyFactory.create(name, objValue);

            try {
                // Create (or reuse) the JCR Property object ...
                AbstractJcrProperty jcrProp = null;
                if (existing != null) {
                    jcrProp = existing.getPayload().getJcrProperty();
                } else {
                    AbstractJcrNode jcrNode = payload.getJcrNode();
                    if (definition.isMultiple()) {
                        jcrProp = new JcrMultiValueProperty(SessionCache.this, jcrNode, dnaProp.getName());
                    } else {
                        jcrProp = new JcrSingleValueProperty(SessionCache.this, jcrNode, dnaProp.getName());
                    }
                }
                assert jcrProp != null;
                JcrPropertyPayload propPayload = new JcrPropertyPayload(definition.getId(), propertyType, jcrProp);
                node.setProperty(dnaProp, definition.isMultiple(), propPayload);
                return jcrProp;
            } catch (ValidationException e) {
                throw new ConstraintViolationException(e.getMessage(), e);
            } catch (RepositorySourceException e) {
                throw new RepositoryException(e.getMessage(), e);
            } catch (AccessControlException e) {
                throw new AccessDeniedException(e.getMessage(), e);
            }
        }

        /**
         * Set the values for the property. If the property does not exist, it will be added. If the property does exist, the
         * existing values will be replaced with those that are supplied.
         * <p>
         * This method will not set protected property definitions and should be used in almost all cases.
         * </p>
         * 
         * @param name the property name; may not be null
         * @param values new property values, all of which must have the same {@link Value#getType() property type}; may not be
         *        null but may be empty
         * @param valueType
         * @return the JCR property object for the property; never null
         * @throws ConstraintViolationException if the property could not be set because of a node type constraint or property
         *         definition constraint
         * @throws javax.jcr.ValueFormatException
         * @throws AccessDeniedException if the current session does not have the requisite privileges to perform this task
         * @throws RepositoryException if any other error occurs
         */
        public AbstractJcrProperty setProperty( Name name,
                                                Value[] values,
                                                int valueType )
            throws AccessDeniedException, ConstraintViolationException, RepositoryException, javax.jcr.ValueFormatException {
            return setProperty(name, values, valueType, true);
        }

        /**
         * Set the values for the property. If the property does not exist, it will be added. If the property does exist, the
         * existing values will be replaced with those that are supplied.
         * 
         * @param name the property name; may not be null
         * @param values new property values, all of which must have the same {@link Value#getType() property type}; may not be
         *        null but may be empty
         * @param skipProtected if true, attempts to set protected properties will fail. If false, attempts to set protected
         *        properties will be allowed.
         * @param valueType
         * @return the JCR property object for the property; never null
         * @throws ConstraintViolationException if the property could not be set because of a node type constraint or property
         *         definition constraint
         * @throws javax.jcr.ValueFormatException
         * @throws AccessDeniedException if the current session does not have the requisite privileges to perform this task
         * @throws VersionException if this node is not checked out
         * @throws RepositoryException if any other error occurs
         */
        public AbstractJcrProperty setProperty( Name name,
                                                Value[] values,
                                                int valueType,
                                                boolean skipProtected )
            throws AccessDeniedException, ConstraintViolationException, RepositoryException, javax.jcr.ValueFormatException,
            VersionException {
            assert name != null;
            assert values != null;

            /*
             * Skip this check for protected nodes.  They can't be modified by users and, in some cases (e.g., jcr:isLocked),
             * may be able to be modified for checked-in nodes.
             */
            if (!isCheckedOut() && skipProtected) {
                String path = node.getLocation().getPath().getString(context().getNamespaceRegistry());
                throw new VersionException(JcrI18n.nodeIsCheckedIn.text(path));
            }

            checkCardinalityOfExistingProperty(name, true);

            int len = values.length;
            Value[] newValues = null;
            if (len == 0) {
                newValues = JcrMultiValueProperty.EMPTY_VALUES;
            } else {
                List<Value> valuesWithDesiredType = new ArrayList<Value>(len);
                int expectedType = -1;
                for (int i = 0; i != len; ++i) {
                    Value value = values[i];

                    if (value == null) continue;
                    if (expectedType == -1) {
                        expectedType = value.getType();
                    } else if (value.getType() != expectedType) {
                        // Make sure the type of each value is the same, as per Javadoc in section 10.4.2.6 of the JCR 2.0 spec
                        StringBuilder sb = new StringBuilder();
                        sb.append('[');
                        for (int j = 0; j != values.length; ++j) {
                            if (j != 0) sb.append(",");
                            sb.append(values[j].toString());
                        }
                        sb.append(']');
                        String propType = PropertyType.nameFromValue(expectedType);
                        I18n msg = JcrI18n.allPropertyValuesMustHaveSameType;
                        String path = readable(node.getPath());
                        String workspaceName = SessionCache.this.workspaceName();
                        throw new javax.jcr.ValueFormatException(msg.text(readable(name), values, propType, path, workspaceName));
                    }
                    if (value.getType() != valueType && valueType != PropertyType.UNDEFINED) {
                        value = ((JcrValue)value).asType(valueType);
                    }
                    valuesWithDesiredType.add(value);
                }
                if (valuesWithDesiredType.isEmpty()) {
                    newValues = JcrMultiValueProperty.EMPTY_VALUES;
                } else {
                    newValues = valuesWithDesiredType.toArray(new Value[valuesWithDesiredType.size()]);
                }
            }

            int numValues = newValues.length;
            JcrPropertyDefinition definition = null;

            // Look for an existing property ...
            PropertyInfo<JcrPropertyPayload> existing = node.getProperty(name);
            if (existing != null) {
                // We're replacing an existing property, but we still need to check that the property definition
                // (still) defines a type. So, find the property definition for the existing property ...
                definition = nodeTypes().getPropertyDefinition(existing.getPayload().getPropertyDefinitionId());

                if (definition != null) {
                    // The definition's require type must match the value's ...
                    if (numValues == 0) {
                        // Just use the definition as is ...
                    } else {
                        // Use the property type for the first non-null value ...
                        int type = newValues[0].getType();
                        if (definition.getRequiredType() != PropertyType.UNDEFINED && definition.getRequiredType() != type) {
                            // The property type is not right, so we have to check if we can cast.
                            // It's easier and could save more work if we just find a new property definition that works ...
                            definition = null;
                        } else {
                            // The types match, so see if the value satisfies the constraints ...
                            if (!definition.satisfiesConstraints(newValues)) definition = null;
                        }
                    }
                }
            }
            JcrNodePayload payload = node.getPayload();
            if (definition == null) {
                // Look for a definition ...
                definition = nodeTypes().findPropertyDefinition(payload.getPrimaryTypeName(),
                                                                payload.getMixinTypeNames(),
                                                                name,
                                                                newValues,
                                                                skipProtected);
                /*
                 * findPropertyDefinition checks constraints for all property types except REFERENCE.  To avoid unnecessary loading of nodes,
                 * REFERENCE constraints are only checked when the property is first set.
                 */
                boolean referencePropMissedConstraints = definition != null
                                                         && definition.getRequiredType() == PropertyType.REFERENCE
                                                         && !definition.canCastToTypeAndSatisfyConstraints(newValues);
                if (definition == null || referencePropMissedConstraints) {
                    throw new ConstraintViolationException(JcrI18n.noDefinition.text("property",
                                                                                     readable(name),
                                                                                     readable(node.getPath()),
                                                                                     readable(payload.getPrimaryTypeName()),
                                                                                     readable(payload.getMixinTypeNames())));
                }
            } else {
                // Check that the existing definition isn't protected
                if (skipProtected && definition.isProtected()) throw new ConstraintViolationException(
                                                                                                      JcrI18n.noDefinition.text("property",
                                                                                                                                readable(name),
                                                                                                                                readable(node.getPath()),
                                                                                                                                readable(payload.getPrimaryTypeName()),
                                                                                                                                readable(payload.getMixinTypeNames())));
            }

            // Create the ModeShape property ...
            int type = numValues != 0 ? newValues[0].getType() : definition.getRequiredType();
            Object[] objValues = new Object[numValues];
            int propertyType = definition.getRequiredType();
            if (propertyType == PropertyType.UNDEFINED || propertyType == type) {
                // Can use the values as is ...
                propertyType = type;
                for (int i = 0; i != numValues; ++i) {
                    objValues[i] = ((JcrValue)newValues[i]).value();
                }
            } else {
                // A cast is required ...
                assert propertyType != type;
                org.modeshape.graph.property.PropertyType dnaPropertyType = PropertyTypeUtil.dnaPropertyTypeFor(propertyType);
                ValueFactory<?> factory = factories().getValueFactory(dnaPropertyType);
                for (int i = 0; i != numValues; ++i) {
                    objValues[i] = factory.create(((JcrValue)newValues[i]).value());
                }
            }
            Property dnaProp = propertyFactory.create(name, objValues);

            try {
                // Create (or reuse) the JCR Property object ...
                AbstractJcrProperty jcrProp = null;
                if (existing != null) {
                    jcrProp = existing.getPayload().getJcrProperty();
                } else {
                    AbstractJcrNode jcrNode = payload.getJcrNode();
                    if (definition.isMultiple()) {
                        jcrProp = new JcrMultiValueProperty(SessionCache.this, jcrNode, dnaProp.getName());
                    } else {
                        jcrProp = new JcrSingleValueProperty(SessionCache.this, jcrNode, dnaProp.getName());
                    }
                }
                assert jcrProp != null;
                JcrPropertyPayload propPayload = new JcrPropertyPayload(definition.getId(), propertyType, jcrProp);
                node.setProperty(dnaProp, definition.isMultiple(), propPayload);
                return jcrProp;
            } catch (ValidationException e) {
                throw new ConstraintViolationException(e.getMessage(), e);
            } catch (RepositorySourceException e) {
                throw new RepositoryException(e.getMessage(), e);
            } catch (AccessControlException e) {
                throw new AccessDeniedException(e.getMessage(), e);
            }
        }

        /**
         * Remove the existing property with the supplied name.
         * 
         * @param name the property name; may not be null
         * @return true if there was a property with the supplied name, or false if no such property existed
         * @throws AccessDeniedException if the current session does not have the requisite permissions to remove this property
         * @throws RepositoryException if any other error occurs
         */
        public boolean removeProperty( Name name ) throws AccessDeniedException, RepositoryException {
            try {
                return node.removeProperty(name) != null;
            } catch (ValidationException e) {
                throw new ConstraintViolationException(e.getMessage(), e);
            } catch (RepositorySourceException e) {
                throw new RepositoryException(e.getMessage(), e);
            } catch (AccessControlException e) {
                throw new AccessDeniedException(e.getMessage(), e);
            }
        }

        /**
         * Move the specified child to be located immediately before the other supplied node.
         * 
         * @param childToBeMoved the path segment specifying the child that is to be moved
         * @param before the path segment of the node before which the {@code childToBeMoved} should be placed, or null if the
         *        node should be moved to the end
         * @throws IllegalArgumentException if either segment is null or does not specify an existing node
         * @throws AccessDeniedException if the current session does not have the requisite permissions to remove this property
         * @throws RepositoryException if any other error occurs
         */
        public void orderChildBefore( Path.Segment childToBeMoved,
                                      Path.Segment before ) throws AccessDeniedException, RepositoryException {
            try {
                node.orderChildBefore(childToBeMoved, before);
            } catch (ValidationException e) {
                throw new ConstraintViolationException(e.getMessage(), e);
            } catch (RepositorySourceException e) {
                throw new RepositoryException(e.getMessage(), e);
            } catch (AccessControlException e) {
                throw new AccessDeniedException(e.getMessage(), e);
            }
        }

        /**
         * Move the child specified by the supplied UUID to be a child of this node, appending the child to the end of the current
         * list of children. This method automatically disconnects the node from its current parent.
         * 
         * @param child the UUID of the existing node; may not be null
         * @param newNodeName
         * @return the newly-added child in its location under the new parent; never null
         * @throws ItemNotFoundException if the specified child node could be found in the session or workspace
         * @throws InvalidItemStateException if the specified child has been marked for deletion within this session
         * @throws ConstraintViolationException if moving the node into this node violates this node's definition
         * @throws RepositoryException if any other error occurs while reading information from the repository
         */
        public Node<JcrNodePayload, JcrPropertyPayload> moveToBeChild( AbstractJcrNode child,
                                                                       Name newNodeName )
            throws ItemNotFoundException, InvalidItemStateException, ConstraintViolationException, RepositoryException {

            // Look up the child and verify that the child can move into this node ...
            if (child instanceof JcrSharedNode) {
                child = ((JcrSharedNode)child).proxyNode();
            }
            Node<JcrNodePayload, JcrPropertyPayload> existingChild = findNode(child.nodeId, child.location.getPath());
            if (existingChild.equals(node) || node.isAtOrBelow(existingChild)) {
                String pathOfChild = readable(existingChild.getPath());
                String thisPath = readable(node.getPath());
                String msg = JcrI18n.unableToMoveNodeToBeChildOfDecendent.text(pathOfChild, thisPath, workspaceName());
                throw new RepositoryException(msg);
            }

            // Find the best node definition for the child in the new parent, or throw an exception if there is none ...
            JcrNodeDefinition defn = findBestNodeDefinition(node, newNodeName, child.getPrimaryTypeName());

            try {
                // Perform the move ...
                existingChild.moveTo(node, newNodeName);

                NodeDefinitionId existingChildDefinitionId = existingChild.getPayload().getDefinitionId();
                if (!defn.getId().equals(existingChildDefinitionId)) {
                    // The node definition changed, so try to set the property ...
                    NodeEditor newChildEditor = getEditorFor(existingChild);
                    try {
                        JcrValue value = new JcrValue(factories(), SessionCache.this, PropertyType.STRING, defn.getId()
                                                                                                               .getString());
                        newChildEditor.setProperty(ModeShapeIntLexicon.NODE_DEFINITON, value);
                    } catch (ConstraintViolationException e) {
                        // We can't set this property on the node (according to the node definition).
                        // But we still want the node info to have the correct node definition.
                        // When it is reloaded into a cache (after being persisted), the correct node definition
                        // will be computed again ...
                        existingChild.setPayload(existingChild.getPayload().with(defn.getId()));

                        // And remove the property from the info ...
                        newChildEditor.removeProperty(ModeShapeIntLexicon.NODE_DEFINITON);
                    }
                }

                // Update the location of the JcrNode object...
                Path newPath = existingChild.getPath();
                setNewLocation(existingChild, newPath);

                return existingChild;
            } catch (ValidationException e) {
                throw new ConstraintViolationException(e.getMessage(), e);
            } catch (RepositorySourceException e) {
                throw new RepositoryException(e.getMessage(), e);
            } catch (AccessControlException e) {
                throw new AccessDeniedException(e.getMessage(), e);
            }
        }

        private void setNewLocation( Node<JcrNodePayload, JcrPropertyPayload> node,
                                     Path newPath ) {
            AbstractJcrNode jcrNode = node.getPayload().getJcrNode(false);
            if (jcrNode != null) {
                // The JCR Node object has been cached, so update the location ...
                node.getPayload().getJcrNode().setLocation(node.getLocation().with(newPath));

                // Now update the location on the cached children of that moved node ...
                for (Node<JcrNodePayload, JcrPropertyPayload> child : node.getChildren()) {
                    if (!child.isLoaded()) continue;
                    Path newChildPath = pathFactory.create(newPath, child.getSegment());
                    setNewLocation(child, newChildPath);
                }
            }
        }

        public void addMixin( JcrNodeType mixinCandidateType ) throws javax.jcr.ValueFormatException, RepositoryException {
            try {
                PropertyInfo<JcrPropertyPayload> existingMixinProperty = node.getProperty(JcrLexicon.MIXIN_TYPES);

                // getProperty(JcrLexicon.MIXIN_TYPES);
                Value[] existingMixinValues;
                if (existingMixinProperty != null) {
                    existingMixinValues = existingMixinProperty.getPayload().getJcrProperty().getValues();
                } else {
                    existingMixinValues = new Value[0];
                }

                Value[] newMixinValues = new Value[existingMixinValues.length + 1];
                System.arraycopy(existingMixinValues, 0, newMixinValues, 0, existingMixinValues.length);
                newMixinValues[newMixinValues.length - 1] = new JcrValue(factories(), SessionCache.this, PropertyType.NAME,
                                                                         mixinCandidateType.getInternalName());

                setProperty(JcrLexicon.MIXIN_TYPES, newMixinValues, PropertyType.NAME, false);

                // ------------------------------------------------------------------------------
                // Create any auto-created properties/nodes from new type
                // ------------------------------------------------------------------------------
                autoCreateItemsFor(mixinCandidateType);

                if (mixinCandidateType.isNodeType(JcrMixLexicon.REFERENCEABLE)) {
                    // This node is now referenceable, so make sure there is a UUID property ...
                    UUID uuid = node.getLocation().getUuid();
                    if (uuid == null) uuid = (UUID)node.getLocation().getIdProperty(JcrLexicon.UUID).getFirstValue();
                    if (uuid == null) uuid = UUID.randomUUID();
                    JcrValue value = new JcrValue(factories(), SessionCache.this, PropertyType.STRING, uuid);
                    setProperty(JcrLexicon.UUID, value, false, false);
                }
            } catch (RepositorySourceException e) {
                throw new RepositoryException(e.getMessage(), e);
            } catch (AccessControlException e) {
                throw new AccessDeniedException(e.getMessage(), e);
            }
        }

        private void autoCreateItemsFor( JcrNodeType nodeType )
            throws InvalidItemStateException, ConstraintViolationException, AccessDeniedException, RepositoryException {

            for (JcrPropertyDefinition propertyDefinition : nodeType.allPropertyDefinitions()) {
                if (propertyDefinition.isAutoCreated() && !propertyDefinition.isProtected()) {
                    PropertyInfo<JcrPropertyPayload> autoCreatedProp = node.getProperty(propertyDefinition.getInternalName());
                    if (autoCreatedProp == null) {
                        // We have to 'auto-create' the property ...
                        if (propertyDefinition.getDefaultValues() != null) {
                            if (propertyDefinition.isMultiple()) {
                                setProperty(propertyDefinition.getInternalName(),
                                            propertyDefinition.getDefaultValues(),
                                            propertyDefinition.getRequiredType());
                            } else {
                                assert propertyDefinition.getDefaultValues().length == 1;
                                setProperty(propertyDefinition.getInternalName(),
                                            (JcrValue)propertyDefinition.getDefaultValues()[0]);
                            }
                        }
                        // otherwise, we don't care
                    }
                }
            }

            for (JcrNodeDefinition nodeDefinition : nodeType.allChildNodeDefinitions()) {
                if (nodeDefinition.isAutoCreated() && !nodeDefinition.isProtected()) {
                    Name nodeName = nodeDefinition.getInternalName();
                    if (node.getChildrenCount(nodeName) == 0) {
                        assert nodeDefinition.getDefaultPrimaryType() != null;
                        createChild(nodeName, null, ((JcrNodeType)nodeDefinition.getDefaultPrimaryType()).getInternalName());
                    }
                }
            }
        }

        /**
         * Create a new node as a child of this node, using the supplied name and (optionally) the supplied UUID.
         * 
         * @param name the name for the new child; may not be null
         * @param desiredUuid the desired UUID, or null if the UUID for the child should be generated automatically
         * @param primaryTypeName the name of the primary type for the new node
         * @return the representation of the newly-created child
         * @throws InvalidItemStateException if the specified child has been marked for deletion within this session
         * @throws ConstraintViolationException if moving the node into this node violates this node's definition
         * @throws NoSuchNodeTypeException if the node type for the primary type could not be found
         * @throws AccessDeniedException if the current session does not have the requisite privileges to perform this task
         * @throws RepositoryException if any other error occurs while reading information from the repository
         */
        public JcrNode createChild( Name name,
                                    UUID desiredUuid,
                                    Name primaryTypeName )
            throws InvalidItemStateException, ConstraintViolationException, AccessDeniedException, RepositoryException {

            if (desiredUuid == null) desiredUuid = UUID.randomUUID();
            try {

                // Verify that this node accepts a child of the supplied name (given any existing SNS nodes) ...
                int numSns = node.getChildrenCount(name) + 1;
                JcrNodePayload payload = node.getPayload();
                JcrNodeDefinition definition = nodeTypes().findChildNodeDefinition(payload.getPrimaryTypeName(),
                                                                                   payload.getMixinTypeNames(),
                                                                                   name,
                                                                                   primaryTypeName,
                                                                                   numSns,
                                                                                   true);
                // Make sure there was a valid child node definition ...
                if (definition == null) {

                    // Check if the definition would have worked with less SNS
                    definition = nodeTypes().findChildNodeDefinition(payload.getPrimaryTypeName(),
                                                                     payload.getMixinTypeNames(),
                                                                     name,
                                                                     primaryTypeName,
                                                                     numSns - 1,
                                                                     true);
                    if (definition != null) {
                        // Only failed because there was no SNS definition - throw ItemExistsException per 7.1.4 of 1.0.1 spec
                        Path pathForChild = pathFactory.create(node.getPath(), name, numSns);
                        String msg = JcrI18n.noSnsDefinitionForNode.text(pathForChild, workspaceName());
                        throw new ItemExistsException(msg);
                    }
                    // Didn't work for other reasons - throw ConstraintViolationException
                    Path pathForChild = pathFactory.create(node.getPath(), name, numSns);
                    String msg = JcrI18n.nodeDefinitionCouldNotBeDeterminedForNode.text(pathForChild,
                                                                                        workspaceName(),
                                                                                        sourceName());

                    throw new ConstraintViolationException(msg);
                }

                // Find the primary type ...
                JcrNodeType primaryType = null;
                if (primaryTypeName != null) {
                    primaryType = nodeTypes().getNodeType(primaryTypeName);
                    if (primaryType == null) {
                        Path pathForChild = pathFactory.create(node.getPath(), name, numSns);
                        I18n msg = JcrI18n.unableToCreateNodeWithPrimaryTypeThatDoesNotExist;
                        throw new NoSuchNodeTypeException(msg.text(primaryTypeName, pathForChild, workspaceName()));
                    }

                    if (primaryType.isMixin()) {
                        I18n msg = JcrI18n.cannotUseMixinTypeAsPrimaryType;
                        throw new ConstraintViolationException(msg.text(primaryType.getName()));
                    }

                    if (primaryType.isAbstract()) {
                        I18n msg = JcrI18n.primaryTypeCannotBeAbstract;
                        throw new ConstraintViolationException(msg.text(primaryType.getName()));
                    }

                } else {
                    primaryType = (JcrNodeType)definition.getDefaultPrimaryType();
                    if (primaryType == null) {
                        // There is no default primary type ...
                        Path pathForChild = pathFactory.create(node.getPath(), name, numSns);
                        I18n msg = JcrI18n.unableToCreateNodeWithNoDefaultPrimaryTypeOnChildNodeDefinition;
                        String nodeTypeName = definition.getDeclaringNodeType().getName();
                        throw new NoSuchNodeTypeException(msg.text(definition.getName(),
                                                                   nodeTypeName,
                                                                   pathForChild,
                                                                   workspaceName()));
                    }
                }
                primaryTypeName = primaryType.getInternalName();

                // ---------------------------------------------------------
                // Now create the child node representation in the cache ...
                // ---------------------------------------------------------

                // Create the initial properties ...
                Property primaryTypeProp = propertyFactory.create(JcrLexicon.PRIMARY_TYPE, primaryTypeName);
                Property nodeDefinitionProp = propertyFactory.create(ModeShapeIntLexicon.NODE_DEFINITON, definition.getId()
                                                                                                                   .getString());

                // Now add the "jcr:uuid" property if and only if referenceable ...
                Node<JcrNodePayload, JcrPropertyPayload> result = null;
                boolean isReferenceable = primaryType.isNodeType(JcrMixLexicon.REFERENCEABLE);
                Property uuidProperty = null;
                if (desiredUuid != null || isReferenceable) {
                    if (desiredUuid == null) {
                        desiredUuid = UUID.randomUUID();
                    }
                    uuidProperty = propertyFactory.create(JcrLexicon.UUID, desiredUuid);
                }
                if (uuidProperty != null) {
                    result = node.createChild(name, Collections.singleton(uuidProperty), primaryTypeProp, nodeDefinitionProp);
                } else {
                    result = node.createChild(name, primaryTypeProp, nodeDefinitionProp);
                }

                JcrNode jcrNode = (JcrNode)result.getPayload().getJcrNode();

                // Fix the "jcr:created", "jcr:createdBy", "jcr:lastModified" and "jcr:lastModifiedBy" properties on the new child
                // ...
                JcrValue now = jcrNode.valueFrom(Calendar.getInstance());
                JcrValue by = jcrNode.valueFrom(session().getUserID());
                boolean isCreatedType = primaryType.isNodeType(JcrMixLexicon.CREATED);
                boolean isHierarchyNode = primaryType.isNodeType(JcrNtLexicon.HIERARCHY_NODE);
                if (isHierarchyNode || isCreatedType) {
                    NodeEditor editor = jcrNode.editor();
                    if (isHierarchyNode) {
                        editor.setProperty(JcrLexicon.CREATED, now, false, false);
                    }
                    if (isCreatedType) {
                        editor.setProperty(JcrLexicon.CREATED, now, false, false);
                        editor.setProperty(JcrLexicon.CREATED_BY, by, false, false);
                    }
                }

                // The postCreateChild hook impl should populate the payloads
                jcrNode.editor().autoCreateItemsFor(primaryType);

                // Finally, return the jcr node ...
                return jcrNode;
            } catch (ValidationException e) {
                throw new ConstraintViolationException(e.getMessage(), e);
            } catch (RepositorySourceException e) {
                throw new RepositoryException(e.getMessage(), e);
            } catch (AccessControlException e) {
                throw new AccessDeniedException(e.getMessage(), e);
            }
        }

        /**
         * Destroy the child node with the supplied UUID and all nodes that exist below it, including any nodes that were created
         * and haven't been persisted.
         * 
         * @param child the child node; may not be null
         * @throws AccessDeniedException if the current session does not have the requisite privileges to perform this task
         * @throws RepositoryException if any other error occurs
         * @return true if the child was successfully removed, or false if the node did not exist as a child
         */
        public boolean destroyChild( Node<JcrNodePayload, JcrPropertyPayload> child )
            throws AccessDeniedException, RepositoryException {
            if (!child.getParent().equals(node)) return false;
            try {
                child.destroy();
            } catch (AccessControlException e) {
                throw new AccessDeniedException(e.getMessage(), e);
            }
            return true;
        }

        /**
         * Convenience method that destroys this node.
         * 
         * @return true if this node was successfully removed
         * @throws AccessDeniedException if the current session does not have the requisite privileges to perform this task
         * @throws RepositoryException if any other error occurs
         */
        public boolean destroy() throws AccessDeniedException, RepositoryException {
            try {
                node.destroy();
            } catch (AccessControlException e) {
                throw new AccessDeniedException(e.getMessage(), e);
            }
            return true;
        }
    }

    /**
     * Find the best property definition in this node's primary type and mixin types.
     * 
     * @param primaryTypeNameOfParent the name of the primary type for the parent node; may not be null
     * @param mixinTypeNamesOfParent the names of the mixin types for the parent node; may be null or empty if there are no mixins
     *        to include in the search
     * @param dnaProperty the new property that is to be set on this node
     * @param propertyType the property type; must be a valid {@link PropertyType} value
     * @param isSingle true if the property definition should be single-valued, or false if the property definition should allow
     *        multiple values
     * @param skipProtected true if this operation is being done from within the public JCR node and property API, or false if
     *        this operation is being done from within internal implementations
     * @return the property definition that allows setting this property, or null if there is no such definition
     */
    protected JcrPropertyDefinition findBestPropertyDefintion( Name primaryTypeNameOfParent,
                                                               List<Name> mixinTypeNamesOfParent,
                                                               Property dnaProperty,
                                                               int propertyType,
                                                               boolean isSingle,
                                                               boolean skipProtected ) {
        JcrPropertyDefinition definition = null;
        if (propertyType == PropertyType.UNDEFINED) {
            propertyType = PropertyTypeUtil.jcrPropertyTypeFor(dnaProperty);
        }

        // If single-valued ...
        if (isSingle) {
            // Create a value for the ModeShape property value ...
            Object value = dnaProperty.getFirstValue();
            Value jcrValue = new JcrValue(factories(), SessionCache.this, propertyType, value);
            definition = nodeTypes().findPropertyDefinition(primaryTypeNameOfParent,
                                                            mixinTypeNamesOfParent,
                                                            dnaProperty.getName(),
                                                            jcrValue,
                                                            true,
                                                            skipProtected);
        } else {
            // Create values for the ModeShape property value ...
            Value[] jcrValues = new Value[dnaProperty.size()];
            int index = 0;
            for (Object value : dnaProperty) {
                jcrValues[index++] = new JcrValue(factories(), SessionCache.this, propertyType, value);
            }
            definition = nodeTypes().findPropertyDefinition(primaryTypeNameOfParent,
                                                            mixinTypeNamesOfParent,
                                                            dnaProperty.getName(),
                                                            jcrValues,
                                                            skipProtected);
        }

        if (definition != null) return definition;

        // No definition that allowed the values ...
        return null;
    }

    // Path getPathFor( String workspaceName,
    // UUID uuid,
    // Path relativePath ) throws NoSuchWorkspaceException, ItemNotFoundException, RepositoryException {
    // assert workspaceName != null;
    // assert uuid != null || relativePath != null;
    //
    // Graph graph = operations.getGraph();
    // try {
    // graph.useWorkspace(workspaceName);
    // } catch (InvalidWorkspaceException iwe) {
    // throw new NoSuchWorkspaceException(JcrI18n.workspaceNameIsInvalid.text(graph.getSourceName(), workspaceName));
    // }
    //
    // try {
    // org.modeshape.graph.Node node;
    // if (uuid != null) {
    // node = graph.getNodeAt(uuid);
    //
    // if (relativePath != null) {
    // Path nodePath = node.getLocation().getPath();
    // Path absolutePath = relativePath.resolveAgainst(nodePath);
    // node = graph.getNodeAt(absolutePath);
    // }
    //
    // } else {
    // Path absolutePath = pathFactory.createAbsolutePath(relativePath.getSegmentsList());
    // node = graph.getNodeAt(absolutePath);
    // }
    // assert node != null;
    //
    // return node.getLocation().getPath();
    // } catch (org.modeshape.graph.property.PathNotFoundException pnfe) {
    // throw new ItemNotFoundException(pnfe);
    // } finally {
    // graph.useWorkspace(this.workspaceName);
    // }
    //
    // }
    //
    // Path getPathFor( UUID uuid ) throws ItemNotFoundException, InvalidItemStateException, RepositoryException {
    // if (uuid.equals(root)) return rootPath;
    // return getPathFor(findNodeInfo(uuid));
    // }
    //
    // Path getPathFor( NodeInfo info ) throws ItemNotFoundException, InvalidItemStateException, RepositoryException {
    // if (info == null) {
    // return pathFactory.createRootPath();
    // }
    // UUID uuid = info.getUuid();
    // if (uuid.equals(root)) return rootPath;
    //
    // // This isn't the root node ...
    // Path result = pathCache.get(uuid);
    // if (result == null) {
    // // We need to build a path using the parent path ...
    // UUID parent = info.getParent();
    // if (parent == null) {
    // // Then this node is the root ...
    // root = info.getUuid();
    // result = rootPath;
    // } else {
    // NodeInfo parentInfo = findNodeInfo(parent);
    // Path parentPath = getPathFor(parentInfo);
    // ChildNode child = parentInfo.getChildren().getChild(info.getUuid());
    // result = pathFactory.create(parentPath, child.getSegment());
    // }
    // pathCache.put(uuid, result);
    // }
    // assert result != null;
    // return result;
    // }
    //
    // Path getPathFor( PropertyInfo propertyInfo ) throws ItemNotFoundException, RepositoryException {
    // Path nodePath = getPathFor(propertyInfo.getNodeUuid());
    // return pathFactory.create(nodePath, propertyInfo.getPropertyName());
    // }
    //
    // Path getPathFor( PropertyId propertyId ) throws ItemNotFoundException, RepositoryException {
    // return getPathFor(findPropertyInfo(propertyId));
    // }
    //
    // protected Name getNameOf( UUID nodeUuid ) throws ItemNotFoundException, InvalidItemStateException, RepositoryException {
    // findNodeInfoForRoot();
    // if (nodeUuid == root) return nameFactory.create("");
    // // Get the parent ...
    // NodeInfo info = findNodeInfo(nodeUuid);
    // NodeInfo parent = findNodeInfo(info.getParent());
    // ChildNode child = parent.getChildren().getChild(info.getUuid());
    // return child.getName();
    // }
    //
    // protected int getSnsIndexOf( UUID nodeUuid ) throws ItemNotFoundException, InvalidItemStateException, RepositoryException {
    // findNodeInfoForRoot();
    // if (nodeUuid == root) return 1;
    // // Get the parent ...
    // NodeInfo info = findNodeInfo(nodeUuid);
    // NodeInfo parent = findNodeInfo(info.getParent());
    // ChildNode child = parent.getChildren().getChild(info.getUuid());
    // return child.getSnsIndex();
    // }
    //
    // /**
    // * Load from the underlying repository graph the information for the node with the supplied UUID. This method returns the
    // * information for the requested node (after placing it in the cache), but this method may (at its discretion) also load and
    // * cache information for other nodes.
    // * <p>
    // * Note that this method does not check the cache before loading from the repository graph.
    // * </p>
    // *
    // * @param path the path of the node, if known; may be null only if the UUID is supplied
    // * @param uuid the UUID of the node, if known; may be null only if the path is supplied
    // * @return the information for the node
    // * @throws ItemNotFoundException if the node does not exist in the repository
    // * @throws RepositoryException if there was an error obtaining this information from the repository
    // */
    // protected ImmutableNodeInfo loadFromGraph( Path path,
    // UUID uuid ) throws ItemNotFoundException, RepositoryException {
    // // Load the node information from the store ...
    // try {
    // // See if there is a path for this uuid ...
    // Location location = Location.create(path, uuid);
    // org.modeshape.graph.Node node = store.getNodeAt(location);
    // ImmutableNodeInfo info = createNodeInfoFrom(node, null);
    // this.cachedNodes.put(info.getUuid(), info);
    // return info;
    // } catch (org.modeshape.graph.property.PathNotFoundException e) {
    // throw new ItemNotFoundException(JcrI18n.itemNotFoundWithUuid.text(uuid, workspaceName, e.getLocalizedMessage()));
    // } catch (RepositorySourceException e) {
    // throw new RepositoryException(
    // JcrI18n.errorWhileFindingNodeWithUuid.text(uuid, workspaceName, e.getLocalizedMessage()),
    // e);
    // }
    // }
    //
    // /**
    // * Load from the underlying repository graph the information for the node with the supplied UUID. This method returns the
    // * information for the requested node (after placing it in the cache), but this method may (at its discretion) also load and
    // * cache information for other nodes.
    // * <p>
    // * Note that this method does not check the cache before loading from the repository graph.
    // * </p>
    // *
    // * @param path the path to the node; may not be null
    // * @param parentInfo the parent information; may be null if not known
    // * @return the information for the node
    // * @throws PathNotFoundException if the node does not exist in the repository
    // * @throws RepositoryException if there was an error obtaining this information from the repository
    // */
    // protected ImmutableNodeInfo loadFromGraph( Path path,
    // NodeInfo parentInfo ) throws PathNotFoundException, RepositoryException {
    // // Load the node information from the store ...
    // try {
    // org.modeshape.graph.Node node = store.getNodeAt(path);
    // ImmutableNodeInfo info = createNodeInfoFrom(node, parentInfo);
    // this.cachedNodes.put(info.getUuid(), info);
    // return info;
    // } catch (org.modeshape.graph.property.PathNotFoundException e) {
    // throw new PathNotFoundException(JcrI18n.pathNotFound.text(path, workspaceName));
    // } catch (RepositorySourceException e) {
    // throw new RepositoryException(JcrI18n.errorWhileFindingNodeWithPath.text(path, workspaceName));
    // }
    // }

    // /**
    // * Create the {@link NodeInfo} object given the ModeShape graph node and the parent node information (if it is available).
    // *
    // * @param graphNode the ModeShape graph node; may not be null
    // * @param parentInfo the information for the parent node, or null if the supplied graph node represents the root node, or if
    // * the parent information is not known
    // * @return the node information; never null
    // * @throws RepositoryException if there is an error determining the child {@link NodeDefinition} for the supplied node,
    // * preventing the node information from being constructed
    // */
    // private ImmutableNodeInfo createNodeInfoFrom( org.modeshape.graph.Node graphNode,
    // NodeInfo parentInfo ) throws RepositoryException {
    // // Now get the ModeShape node's UUID and find the ModeShape property containing the UUID ...
    // Location location = graphNode.getLocation();
    // UUID uuid = location.getUuid();
    // org.modeshape.graph.property.Property uuidProperty = null;
    // if (uuid != null) {
    // // Check for an identification property ...
    // uuidProperty = location.getIdProperty(JcrLexicon.UUID);
    // if (uuidProperty == null) {
    // uuidProperty = propertyFactory.create(JcrLexicon.UUID, uuid);
    // }
    // }
    // if (uuidProperty == null) {
    // uuidProperty = graphNode.getProperty(JcrLexicon.UUID);
    // if (uuidProperty != null) {
    // // Grab the first 'good' UUID value ...
    // for (Object uuidValue : uuidProperty) {
    // try {
    // uuid = factories.getUuidFactory().create(uuidValue);
    // break;
    // } catch (ValueFormatException e) {
    // // Ignore; just continue with the next property value
    // }
    // }
    // }
    // if (uuid == null) {
    // // Look for the ModeShape UUID property ...
    // org.modeshape.graph.property.Property dnaUuidProperty = graphNode.getProperty(ModeShapeLexicon.UUID);
    // if (dnaUuidProperty != null) {
    // // Grab the first 'good' UUID value ...
    // for (Object uuidValue : dnaUuidProperty) {
    // try {
    // uuid = factories.getUuidFactory().create(uuidValue);
    // break;
    // } catch (ValueFormatException e) {
    // // Ignore; just continue with the next property value
    // }
    // }
    // }
    // }
    // }
    // if (uuid == null) uuid = UUID.randomUUID();
    // if (uuidProperty == null) uuidProperty = propertyFactory.create(JcrLexicon.UUID, uuid);
    //
    // // Either the UUID is not known, or there was no node. Either way, we have to create the node ...
    // if (uuid == null) uuid = UUID.randomUUID();
    //
    // // Look for the primary type of the node ...
    // Map<Name, Property> graphProperties = graphNode.getPropertiesByName();
    // final boolean isRoot = location.getPath().isRoot();
    // Name primaryTypeName = null;
    // org.modeshape.graph.property.Property primaryTypeProperty = graphNode.getProperty(JcrLexicon.PRIMARY_TYPE);
    // if (primaryTypeProperty != null && !primaryTypeProperty.isEmpty()) {
    // try {
    // primaryTypeName = factories.getNameFactory().create(primaryTypeProperty.getFirstValue());
    // } catch (ValueFormatException e) {
    // // use the default ...
    // }
    // }
    // if (primaryTypeName == null) {
    // // We have to have a primary type, so use the default ...
    // if (isRoot) {
    // primaryTypeName = ModeShapeLexicon.ROOT;
    // primaryTypeProperty = propertyFactory.create(JcrLexicon.PRIMARY_TYPE, primaryTypeName);
    // } else {
    // primaryTypeName = defaultPrimaryTypeName;
    // primaryTypeProperty = defaultPrimaryTypeProperty;
    // }
    // // We have to add this property to the graph node...
    // graphProperties = new HashMap<Name, Property>(graphProperties);
    // graphProperties.put(primaryTypeProperty.getName(), primaryTypeProperty);
    // }
    // assert primaryTypeProperty != null;
    // assert primaryTypeProperty.isEmpty() == false;
    //
    // // Look for a node definition stored on the node ...
    // JcrNodeDefinition definition = null;
    // org.modeshape.graph.property.Property nodeDefnProperty = graphProperties.get(ModeShapeIntLexicon.NODE_DEFINITON);
    // if (nodeDefnProperty != null && !nodeDefnProperty.isEmpty()) {
    // String nodeDefinitionString = stringFactory.create(nodeDefnProperty.getFirstValue());
    // NodeDefinitionId id = NodeDefinitionId.fromString(nodeDefinitionString, nameFactory);
    // definition = nodeTypes().getNodeDefinition(id);
    // }
    // // Figure out the node definition for this node ...
    // if (isRoot) {
    // if (definition == null) definition = nodeTypes().getRootNodeDefinition();
    // } else {
    // // We need the parent ...
    // Path path = location.getPath();
    // if (parentInfo == null) {
    // Path parentPath = path.getParent();
    // parentInfo = findNodeInfo(null, parentPath.getNormalizedPath());
    // }
    // if (definition == null) {
    // Name childName = path.getLastSegment().getName();
    // int numExistingChildrenWithSameName = parentInfo.getChildren().getCountOfSameNameSiblingsWithName(childName);
    // definition = nodeTypes().findChildNodeDefinition(parentInfo.getPrimaryTypeName(),
    // parentInfo.getMixinTypeNames(),
    // childName,
    // primaryTypeName,
    // numExistingChildrenWithSameName,
    // false);
    // }
    // if (definition == null) {
    // String msg = JcrI18n.nodeDefinitionCouldNotBeDeterminedForNode.text(path, workspaceName);
    // throw new RepositorySourceException(sourceName(), msg);
    // }
    // }
    //
    // // ------------------------------------------------------
    // // Set the node's properties ...
    // // ------------------------------------------------------
    // boolean referenceable = false;
    //
    // // Start with the primary type ...
    // JcrNodeType primaryType = nodeTypes().getNodeType(primaryTypeName);
    // if (primaryType == null) {
    // Path path = location.getPath();
    // String msg = JcrI18n.missingNodeTypeForExistingNode.text(primaryTypeName.getString(namespaces), path, workspaceName);
    // throw new RepositorySourceException(sourceName(), msg);
    // }
    // if (primaryType.isNodeType(JcrMixLexicon.REFERENCEABLE)) referenceable = true;
    //
    // // The process the mixin types ...
    // Property mixinTypesProperty = graphProperties.get(JcrLexicon.MIXIN_TYPES);
    // List<Name> mixinTypeNames = null;
    // if (mixinTypesProperty != null && !mixinTypesProperty.isEmpty()) {
    // for (Object mixinTypeValue : mixinTypesProperty) {
    // Name mixinTypeName = nameFactory.create(mixinTypeValue);
    // if (mixinTypeNames == null) mixinTypeNames = new LinkedList<Name>();
    // mixinTypeNames.add(mixinTypeName);
    // JcrNodeType mixinType = nodeTypes().getNodeType(mixinTypeName);
    // if (mixinType == null) continue;
    // if (!referenceable && mixinType.isNodeType(JcrMixLexicon.REFERENCEABLE)) referenceable = true;
    // }
    // }
    //
    // // Create the set of multi-valued property names ...
    // Set<Name> multiValuedPropertyNames = EMPTY_NAMES;
    // Set<Name> newSingleMultiPropertyNames = null;
    // Property multiValuedPropNamesProp = graphProperties.get(ModeShapeIntLexicon.MULTI_VALUED_PROPERTIES);
    // if (multiValuedPropNamesProp != null && !multiValuedPropNamesProp.isEmpty()) {
    // multiValuedPropertyNames = getSingleMultiPropertyNames(multiValuedPropNamesProp, location.getPath(), uuid);
    // }
    //
    // // Now create the JCR property object wrappers around the other properties ...
    // Map<Name, PropertyInfo> props = new HashMap<Name, PropertyInfo>();
    // for (Property dnaProp : graphProperties.values()) {
    // Name name = dnaProp.getName();
    //
    // // Is this is single-valued property?
    // boolean isSingle = dnaProp.isSingle();
    // // Make sure that this isn't a multi-valued property with one value ...
    // if (isSingle && multiValuedPropertyNames.contains(name)) isSingle = false;
    //
    // // Figure out the JCR property type for this property ...
    // int propertyType = PropertyTypeUtil.jcrPropertyTypeFor(dnaProp);
    // PropertyDefinition propertyDefinition = findBestPropertyDefintion(primaryTypeName,
    // mixinTypeNames,
    // dnaProp,
    // propertyType,
    // isSingle,
    // false);
    //
    // // If there still is no property type defined ...
    // if (propertyDefinition == null && INCLUDE_PROPERTIES_NOT_ALLOWED_BY_NODE_TYPE_OR_MIXINS) {
    // // We can use the "nt:unstructured" property definitions for any property ...
    // NodeType unstructured = nodeTypes().getNodeType(JcrNtLexicon.UNSTRUCTURED);
    // for (PropertyDefinition anyDefinition : unstructured.getDeclaredPropertyDefinitions()) {
    // if (anyDefinition.isMultiple()) {
    // propertyDefinition = anyDefinition;
    // break;
    // }
    // }
    // }
    // if (propertyDefinition == null) {
    // // We're supposed to skip this property (since we don't have a definition for it) ...
    // continue;
    // }
    //
    // // Figure out if this is a multi-valued property ...
    // boolean isMultiple = propertyDefinition.isMultiple();
    // if (!isMultiple && dnaProp.isEmpty()) {
    // // Only multi-valued properties can have no values; so if not multi-valued, then skip ...
    // continue;
    // }
    //
    // // Update the list of single-valued multi-property names ...
    // if (isMultiple && isSingle) {
    // if (newSingleMultiPropertyNames == null) newSingleMultiPropertyNames = new HashSet<Name>();
    // newSingleMultiPropertyNames.add(name);
    // }
    //
    // // Figure out the property type ...
    // int definitionType = propertyDefinition.getRequiredType();
    // if (definitionType != PropertyType.UNDEFINED) {
    // propertyType = definitionType;
    // }
    //
    // // Record the property in the node information ...
    // PropertyId propId = new PropertyId(uuid, name);
    // JcrPropertyDefinition defn = (JcrPropertyDefinition)propertyDefinition;
    // PropertyInfo propInfo = new PropertyInfo(propId, defn.getId(), propertyType, dnaProp, defn.isMultiple(), false, false);
    // props.put(name, propInfo);
    // }
    //
    // // Now add the "jcr:uuid" property if and only if referenceable ...
    // if (referenceable) {
    // // We know that this property is single-valued
    // JcrValue value = new JcrValue(factories(), this, PropertyType.STRING, uuid);
    // PropertyDefinition propertyDefinition = nodeTypes().findPropertyDefinition(primaryTypeName,
    // mixinTypeNames,
    // JcrLexicon.UUID,
    // value,
    // false,
    // false);
    // PropertyId propId = new PropertyId(uuid, JcrLexicon.UUID);
    // JcrPropertyDefinition defn = (JcrPropertyDefinition)propertyDefinition;
    // PropertyInfo propInfo = new PropertyInfo(propId, defn.getId(), PropertyType.STRING, uuidProperty, defn.isMultiple(),
    // false, false);
    // props.put(JcrLexicon.UUID, propInfo);
    // } else {
    // // Make sure there is NOT a "jcr:uuid" property ...
    // props.remove(JcrLexicon.UUID);
    // }
    // // Make sure the "dna:uuid" property did not get in there ...
    // props.remove(ModeShapeLexicon.UUID);
    //
    // // Make sure the single-valued multi-property names are stored as a property ...
    // if (newSingleMultiPropertyNames != null) {
    // PropertyInfo info = createSingleMultiplePropertyInfo(uuid,
    // primaryTypeName,
    // mixinTypeNames,
    // newSingleMultiPropertyNames);
    // props.put(info.getPropertyName(), info);
    // }
    //
    // // Create the node information ...
    // UUID parentUuid = parentInfo != null ? parentInfo.getUuid() : null;
    // List<Location> locations = graphNode.getChildren();
    // Children children = locations.isEmpty() ? new EmptyChildren(parentUuid) : new ImmutableChildren(parentUuid, locations);
    // props = Collections.unmodifiableMap(props);
    // return new ImmutableNodeInfo(location, primaryTypeName, mixinTypeNames, definition.getId(), parentUuid, children, props);
    // }
    //
    protected final void updateSingleMultipleProperty( Node<JcrNodePayload, JcrPropertyPayload> node,
                                                       Name singleMultiPropertyName,
                                                       boolean add ) {
        PropertyInfo<JcrPropertyPayload> existing = node.getProperty(ModeShapeIntLexicon.MULTI_VALUED_PROPERTIES);
        Set<Name> singleMultiPropertyNames = null;
        if (existing != null) {
            singleMultiPropertyNames = new HashSet<Name>();
            // Grab the existing values ...
            for (Object value : existing.getProperty()) {
                singleMultiPropertyNames.add(nameFactory().create(value));
            }
            if (add) singleMultiPropertyNames.add(singleMultiPropertyName);
            else singleMultiPropertyNames.remove(singleMultiPropertyName);
        } else {
            if (add) {
                singleMultiPropertyNames = Collections.singleton(singleMultiPropertyName);
            } else {
                // supposed to remove the property name, but there isn't a property, so just return
                return;
            }
        }

        if (singleMultiPropertyNames.isEmpty()) {
            // Remove the property ...
            assert existing != null;
            node.removeProperty(existing.getName());
            return;
        }
        PropertyInfo<JcrPropertyPayload> property = createSingleMultipleProperty(node.getPayload(),
                                                                                 existing,
                                                                                 singleMultiPropertyNames);
        node.setProperty(property.getProperty(), property.isMultiValued(), property.getPayload());
    }

    protected PropertyInfo<JcrPropertyPayload> createSingleMultipleProperty( JcrNodePayload nodePayload,
                                                                             PropertyInfo<JcrPropertyPayload> existing,
                                                                             Set<Name> singleMultiPropertyNames ) {

        int number = singleMultiPropertyNames.size();
        // Otherwise, we have to set/update the property ...
        String[] names = new String[number];
        JcrValue[] values = new JcrValue[number];
        if (number == 1) {
            String str = singleMultiPropertyNames.iterator().next().getString(namespaces);
            names[0] = str;
            values[0] = new JcrValue(factories(), this, PropertyType.STRING, str);
        } else {
            int index = 0;
            for (Name name : singleMultiPropertyNames) {
                String str = name.getString(namespaces);
                names[index] = str;
                values[index] = new JcrValue(factories(), this, PropertyType.STRING, str);
                ++index;
            }
        }
        JcrPropertyDefinition definition = nodeTypes().findPropertyDefinition(nodePayload.getPrimaryTypeName(),
                                                                              nodePayload.getMixinTypeNames(),
                                                                              ModeShapeIntLexicon.MULTI_VALUED_PROPERTIES,
                                                                              values,
                                                                              false);
        Property dnaProp = propertyFactory.create(ModeShapeIntLexicon.MULTI_VALUED_PROPERTIES,
                                                  singleMultiPropertyNames.iterator());
        return createPropertyInfo(nodePayload, dnaProp, definition, PropertyType.STRING, existing);
    }

    protected final PropertyInfo<JcrPropertyPayload> createPropertyInfo( JcrNodePayload nodePayload,
                                                                         Property dnaProp,
                                                                         JcrPropertyDefinition definition,
                                                                         int propertyType,
                                                                         PropertyInfo<JcrPropertyPayload> existing ) {
        // Create (or reuse) the JCR Property object ...
        AbstractJcrProperty jcrProp = null;
        if (existing != null && existing.getPayload() != null) {
            jcrProp = existing.getPayload().getJcrProperty();
        } else {
            AbstractJcrNode jcrNode = nodePayload.getJcrNode();
            if (definition.isMultiple()) {
                jcrProp = new JcrMultiValueProperty(SessionCache.this, jcrNode, dnaProp.getName());
            } else {
                jcrProp = new JcrSingleValueProperty(SessionCache.this, jcrNode, dnaProp.getName());
            }
        }
        assert jcrProp != null;
        JcrPropertyPayload propPayload = new JcrPropertyPayload(definition.getId(), propertyType, jcrProp);
        Status status = existing != null ? Status.CHANGED : Status.NEW;
        return new GraphSession.PropertyInfo<JcrPropertyPayload>(dnaProp, definition.isMultiple(), status, propPayload);
    }

    @Immutable
    final class JcrNodeOperations extends GraphSession.NodeOperations<JcrNodePayload, JcrPropertyPayload> {
        private final Logger LOGGER = Logger.getLogger(JcrNodeOperations.class);
        private final String user = SessionCache.this.session().getUserID();

        private Map<Name, PropertyInfo<JcrPropertyPayload>> buildProperties( org.modeshape.graph.Node persistentNode,
                                                                             Node<JcrNodePayload, JcrPropertyPayload> node,
                                                                             JcrNodePayload nodePayload,
                                                                             boolean referenceable ) {

            AbstractJcrNode jcrNode = nodePayload.getJcrNode();
            Name primaryTypeName = nodePayload.getPrimaryTypeName();
            List<Name> mixinTypeNames = nodePayload.getMixinTypeNames();

            Location location = persistentNode.getLocation();
            Map<Name, Property> graphProperties = persistentNode.getPropertiesByName();

            if (!graphProperties.containsKey(JcrLexicon.PRIMARY_TYPE)) {
                Property primaryTypeProperty;
                // We have to have a primary type, so use the default ...
                if (location.getPath().isRoot()) {
                    primaryTypeProperty = propertyFactory.create(JcrLexicon.PRIMARY_TYPE, primaryTypeName);
                } else {
                    primaryTypeProperty = defaultPrimaryTypeProperty;
                }
                // We have to add this property to the graph node...
                graphProperties = new HashMap<Name, Property>(graphProperties);
                graphProperties.put(primaryTypeProperty.getName(), primaryTypeProperty);
            }

            // Create the set of multi-valued property names ...
            Set<Name> multiValuedPropertyNames = EMPTY_NAMES;
            Set<Name> newSingleMultiPropertyNames = null;
            Property multiValuedPropNamesProp = graphProperties.get(ModeShapeIntLexicon.MULTI_VALUED_PROPERTIES);
            if (multiValuedPropNamesProp != null && !multiValuedPropNamesProp.isEmpty()) {
                multiValuedPropertyNames = getSingleMultiPropertyNames(multiValuedPropNamesProp, location);
            }

            // Now create the JCR property object wrappers around the other properties ...
            Map<Name, GraphSession.PropertyInfo<JcrPropertyPayload>> props = new HashMap<Name, GraphSession.PropertyInfo<JcrPropertyPayload>>();
            for (Property dnaProp : graphProperties.values()) {
                Name name = dnaProp.getName();

                /*
                 * Don't add mode:uuid to the node.  If the node is referenceable, this has already been added as jcr:uuid
                 * and if the node is not referenceable, the UUID should not be exposed as public API.
                 */
                if (ModeShapeLexicon.UUID.equals(name)) continue;

                // Is this is single-valued property?
                boolean isSingle = dnaProp.isSingle();
                // Make sure that this isn't a multi-valued property with one value ...
                if (isSingle && multiValuedPropertyNames.contains(name)) isSingle = false;

                // Figure out the JCR property type for this property ...
                int propertyType = PropertyTypeUtil.jcrPropertyTypeFor(dnaProp);
                PropertyDefinition propertyDefinition = findBestPropertyDefintion(primaryTypeName,
                                                                                  mixinTypeNames,
                                                                                  dnaProp,
                                                                                  propertyType,
                                                                                  isSingle,
                                                                                  false);

                // If there still is no property type defined ...
                if (propertyDefinition == null && INCLUDE_PROPERTIES_NOT_ALLOWED_BY_NODE_TYPE_OR_MIXINS) {
                    // We can use the "nt:unstructured" property definitions for any property ...
                    NodeType unstructured = nodeTypes().getNodeType(JcrNtLexicon.UNSTRUCTURED);
                    for (PropertyDefinition anyDefinition : unstructured.getDeclaredPropertyDefinitions()) {
                        if (anyDefinition.isMultiple()) {
                            propertyDefinition = anyDefinition;
                            break;
                        }
                    }
                }
                if (propertyDefinition == null) {
                    // We're supposed to skip this property (since we don't have a definition for it) ...
                    continue;
                }

                // Figure out if this is a multi-valued property ...
                boolean isMultiple = propertyDefinition.isMultiple();
                if (!isMultiple && dnaProp.isEmpty()) {
                    // Only multi-valued properties can have no values; so if not multi-valued, then skip ...
                    continue;
                }

                // Update the list of single-valued multi-property names ...
                if (isMultiple && isSingle) {
                    if (newSingleMultiPropertyNames == null) newSingleMultiPropertyNames = new HashSet<Name>();
                    newSingleMultiPropertyNames.add(name);
                }

                // Figure out the property type ...
                int definitionType = propertyDefinition.getRequiredType();
                if (definitionType != PropertyType.UNDEFINED) {
                    propertyType = definitionType;
                }

                // Record the property in the node information ...
                JcrPropertyDefinition defn = (JcrPropertyDefinition)propertyDefinition;
                AbstractJcrProperty jcrProp = null;
                if (isMultiple) {
                    jcrProp = new JcrMultiValueProperty(SessionCache.this, jcrNode, dnaProp.getName());
                } else {
                    jcrProp = new JcrSingleValueProperty(SessionCache.this, jcrNode, dnaProp.getName());
                }
                JcrPropertyPayload payload = new JcrPropertyPayload(defn.getId(), propertyType, jcrProp);
                PropertyInfo<JcrPropertyPayload> propInfo = new PropertyInfo<JcrPropertyPayload>(dnaProp, defn.isMultiple(),
                                                                                                 Status.UNCHANGED, payload);
                props.put(name, propInfo);
            }

            // Now add the "jcr:uuid" property if and only if referenceable ...
            if (referenceable) {
                UUID uuid = location.getUuid();
                org.modeshape.graph.property.Property uuidProperty = null;
                if (uuid != null) {
                    // Check for an identification property ...
                    uuidProperty = location.getIdProperty(JcrLexicon.UUID);
                    if (uuidProperty == null) {
                        uuidProperty = propertyFactory.create(JcrLexicon.UUID, uuid);
                    }
                } else {
                    uuidProperty = location.getIdProperty(JcrLexicon.UUID);
                    // The Basic model on the JPA connector sometimes returns locations with no UUID for referenceable nodes.
                    if (uuidProperty != null) {
                        uuid = factories().getUuidFactory().create(uuidProperty.getFirstValue());
                    } else {
                        uuidProperty = graphProperties.get(ModeShapeLexicon.UUID);
                        uuid = factories().getUuidFactory().create(uuidProperty.getFirstValue());
                        // Recreate the property below as jcr:uuid
                        uuidProperty = null;
                    }
                }

                if (uuid != null && uuidProperty == null) uuidProperty = propertyFactory.create(JcrLexicon.UUID, uuid);

                // We know that this property is single-valued
                JcrValue value = new JcrValue(factories(), SessionCache.this, PropertyType.STRING, uuid);
                JcrPropertyDefinition propDefn = nodeTypes().findPropertyDefinition(primaryTypeName,
                                                                                    mixinTypeNames,
                                                                                    JcrLexicon.UUID,
                                                                                    value,
                                                                                    false,
                                                                                    false);
                PropertyInfo<JcrPropertyPayload> propInfo = createPropertyInfo(nodePayload,
                                                                               uuidProperty,
                                                                               propDefn,
                                                                               PropertyType.STRING,
                                                                               null);
                props.put(JcrLexicon.UUID, propInfo);
            } else {
                // Make sure there is NOT a "jcr:uuid" property ...
                props.remove(JcrLexicon.UUID);
            }
            // Make sure the "dna:uuid" property did not get in there ...
            props.remove(ModeShapeLexicon.UUID);

            // Make sure the single-valued multi-property names are stored as a property ...
            if (newSingleMultiPropertyNames != null) {
                PropertyInfo<JcrPropertyPayload> info = createSingleMultipleProperty(nodePayload,
                                                                                     null,
                                                                                     newSingleMultiPropertyNames);
                props.put(info.getName(), info);
            }

            return props;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.session.GraphSession.Operations#materializeProperties(org.modeshape.graph.Node,
         *      org.modeshape.graph.session.GraphSession.Node)
         */
        @Override
        public void materializeProperties( org.modeshape.graph.Node persistentNode,
                                           Node<JcrNodePayload, JcrPropertyPayload> node ) {

            JcrNodePayload nodePayload = node.getPayload();
            boolean referenceable = false;

            try {
                referenceable = isReferenceable(node);
            } catch (RepositoryException re) {
                throw new IllegalStateException(re);
            }

            Map<Name, PropertyInfo<JcrPropertyPayload>> props = buildProperties(persistentNode, node, nodePayload, referenceable);
            // Set the information on the node ...
            node.loadedWith(props);

        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.session.GraphSession.Operations#materialize(org.modeshape.graph.Node,
         *      org.modeshape.graph.session.GraphSession.Node)
         */
        @Override
        public void materialize( org.modeshape.graph.Node persistentNode,
                                 Node<JcrNodePayload, JcrPropertyPayload> node ) {
            // Now get the ModeShape node's UUID and find the ModeShape property containing the UUID ...
            Location location = node.getLocation();

            // Look for the primary type of the node ...
            Map<Name, Property> graphProperties = persistentNode.getPropertiesByName();
            final boolean isRoot = location.getPath().isRoot();
            Name primaryTypeName = null;
            org.modeshape.graph.property.Property primaryTypeProperty = graphProperties.get(JcrLexicon.PRIMARY_TYPE);
            if (primaryTypeProperty != null && !primaryTypeProperty.isEmpty()) {
                try {
                    primaryTypeName = factories.getNameFactory().create(primaryTypeProperty.getFirstValue());
                } catch (ValueFormatException e) {
                    // use the default ...
                }
            }
            if (primaryTypeName == null) {
                // We have to have a primary type, so use the default ...
                if (isRoot) {
                    primaryTypeName = ModeShapeLexicon.ROOT;
                    primaryTypeProperty = propertyFactory.create(JcrLexicon.PRIMARY_TYPE, primaryTypeName);
                } else {
                    primaryTypeName = defaultPrimaryTypeName;
                    primaryTypeProperty = defaultPrimaryTypeProperty;
                }
            }
            assert primaryTypeProperty != null;
            assert primaryTypeProperty.isEmpty() == false;

            final boolean isSharedNode = ModeShapeLexicon.SHARE.equals(primaryTypeName);
            UUID sharedUuid = null;
            if (isSharedNode) {
                Property sharedUuidProperty = graphProperties.get(ModeShapeLexicon.SHARED_UUID);
                if (sharedUuidProperty != null) {
                    sharedUuid = uuidFactory().create(sharedUuidProperty.getFirstValue());
                }
            }

            // Look for a node definition stored on the node ...
            JcrNodeDefinition definition = null;
            org.modeshape.graph.property.Property nodeDefnProperty = graphProperties.get(ModeShapeIntLexicon.NODE_DEFINITON);
            if (nodeDefnProperty != null && !nodeDefnProperty.isEmpty()) {
                String nodeDefinitionString = stringFactory.create(nodeDefnProperty.getFirstValue());
                NodeDefinitionId id = NodeDefinitionId.fromString(nodeDefinitionString, nameFactory);
                definition = nodeTypes().getNodeDefinition(id);
            }
            // Figure out the node definition for this node ...
            if (definition == null) {
                if (isRoot) {
                    try {
                        definition = nodeTypes().getRootNodeDefinition();
                    } catch (RepositoryException e) {
                        // Shouldn't really happen ...
                        throw new ValidationException(e.getMessage(), e);
                    }
                } else {
                    Name primaryTypeForDefn = primaryTypeName;
                    if (isSharedNode) {
                        // We're creating a shared node, so get the child node defn for the original's primary type
                        // under the parent of the new shared (proxy) node ...
                        try {
                            AbstractJcrNode originalShareable = findJcrNode(Location.create(sharedUuid));
                            primaryTypeForDefn = originalShareable.getPrimaryTypeName();
                        } catch (RepositoryException e) {
                            // do nothing (will be treated as a non-shared node) ...
                        }
                    }
                    Name childName = node.getName();
                    Node<JcrNodePayload, JcrPropertyPayload> parent = node.getParent();
                    JcrNodePayload parentInfo = parent.getPayload();
                    int numExistingChildrenWithSameName = parent.getChildrenCount(childName);
                    // The children include this node, so we need to subtract one from the count so that the
                    // number of existing children is either 0 (if there are no other SNS nodes) or 1+
                    // (if there are at least 2 SNS nodes)
                    --numExistingChildrenWithSameName;
                    definition = nodeTypes().findChildNodeDefinition(parentInfo.getPrimaryTypeName(),
                                                                     parentInfo.getMixinTypeNames(),
                                                                     childName,
                                                                     primaryTypeForDefn,
                                                                     numExistingChildrenWithSameName,
                                                                     false);
                }
            }
            if (definition == null) {
                String msg = JcrI18n.nodeDefinitionCouldNotBeDeterminedForNode.text(readable(node.getPath()),
                                                                                    workspaceName(),
                                                                                    sourceName());
                throw new ValidationException(msg);
            }

            // ------------------------------------------------------
            // Set the node's properties ...
            // ------------------------------------------------------
            boolean referenceable = false;

            // Start with the primary type ...
            JcrNodeType primaryType = nodeTypes().getNodeType(primaryTypeName);
            if (primaryType == null) {
                Path path = location.getPath();
                String msg = JcrI18n.missingNodeTypeForExistingNode.text(readable(primaryTypeName),
                                                                         readable(path),
                                                                         workspaceName(),
                                                                         sourceName());
                throw new ValidationException(msg);
            }
            if (primaryType.isNodeType(JcrMixLexicon.REFERENCEABLE)) referenceable = true;

            // The process the mixin types ...
            Property mixinTypesProperty = graphProperties.get(JcrLexicon.MIXIN_TYPES);
            List<Name> mixinTypeNames = null;
            if (mixinTypesProperty != null && !mixinTypesProperty.isEmpty()) {
                for (Object mixinTypeValue : mixinTypesProperty) {
                    Name mixinTypeName = nameFactory.create(mixinTypeValue);
                    if (mixinTypeNames == null) mixinTypeNames = new LinkedList<Name>();
                    mixinTypeNames.add(mixinTypeName);
                    JcrNodeType mixinType = nodeTypes().getNodeType(mixinTypeName);
                    if (mixinType == null) continue;
                    if (!referenceable && mixinType.isNodeType(JcrMixLexicon.REFERENCEABLE)) referenceable = true;
                }
            }

            // Create the JCR Node payload object ...
            JcrNodePayload nodePayload = createJcrNodePayload(node,
                                                              primaryTypeName,
                                                              mixinTypeNames,
                                                              definition.getId(),
                                                              sharedUuid);
            Map<Name, PropertyInfo<JcrPropertyPayload>> props = buildProperties(persistentNode, node, nodePayload, referenceable);

            // Set the information on the node ...
            node.loadedWith(persistentNode.getChildren(), props, persistentNode.getExpirationTime());
            node.setPayload(nodePayload);
        }

        protected JcrNodePayload createJcrNodePayload( Node<JcrNodePayload, JcrPropertyPayload> node,
                                                       Name primaryTypeName,
                                                       List<Name> mixinTypeNames,
                                                       NodeDefinitionId defnId,
                                                       UUID sharedUuid ) {
            if (sharedUuid != null) {
                try {
                    AbstractJcrNode shared = findJcrNode(Location.create(sharedUuid));
                    return new JcrSharedNodePayload(SessionCache.this, node, primaryTypeName, mixinTypeNames, defnId, shared);
                } catch (RepositoryException e) {
                    // do nothing (will be treated as a non-shared node) ...
                }
            }
            return new JcrNodePayload(SessionCache.this, node, primaryTypeName, mixinTypeNames, defnId);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.session.GraphSession.NodeOperations#postSetProperty(org.modeshape.graph.session.GraphSession.Node,
         *      org.modeshape.graph.property.Name, org.modeshape.graph.session.GraphSession.PropertyInfo)
         */
        @Override
        public void postSetProperty( Node<JcrNodePayload, JcrPropertyPayload> node,
                                     Name propertyName,
                                     PropertyInfo<JcrPropertyPayload> oldProperty ) {
            super.postSetProperty(node, propertyName, oldProperty);

            if (propertyName.equals(ModeShapeIntLexicon.MULTI_VALUED_PROPERTIES)) return;
            if (propertyName.equals(JcrLexicon.MIXIN_TYPES)) {
                // Add all of the values from the property ...
                Set<Name> mixinTypeNames = new HashSet<Name>();
                NameFactory nameFactory = context().getValueFactories().getNameFactory();
                for (Object value : node.getProperty(propertyName).getProperty()) {
                    mixinTypeNames.add(nameFactory.create(value));
                }
                node.setPayload(node.getPayload().with(new ArrayList<Name>(mixinTypeNames)));
            }

            // If the property is multi-valued but has only a single value, we need to record that this property
            // is actually a multi-valued property definition ...
            PropertyInfo<JcrPropertyPayload> changedProperty = node.getProperty(propertyName);
            if (changedProperty.isMultiValued()) {
                // We're changing a multi-valued property ...
                if (changedProperty.getProperty().isSingle()) {
                    // There's only one actual value in this property, so we record the name of this property in a hidden property
                    updateSingleMultipleProperty(node, propertyName, true);
                } else {
                    // There are multiple actual values, so we don't need to name this property in the hidden property ...
                    updateSingleMultipleProperty(node, propertyName, false);
                }
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.session.GraphSession.NodeOperations#preSave(org.modeshape.graph.session.GraphSession.Node,
         *      org.modeshape.graph.property.DateTime)
         */
        @Override
        public void preSave( org.modeshape.graph.session.GraphSession.Node<JcrNodePayload, JcrPropertyPayload> node,
                             DateTime saveTime ) throws ValidationException {
            JcrNodePayload payload = node.getPayload();
            AbstractJcrNode jcrNode = payload.getJcrNode();
            if (jcrNode.isShared()) {
                jcrNode = ((JcrSharedNode)jcrNode).proxyNode();
                try {
                    payload = jcrNode.nodeInfo().getPayload();
                } catch (RepositoryException e) {
                    throw new ValidationException(e.getMessage(), e);
                }
            }

            Name primaryTypeName = payload.getPrimaryTypeName();
            List<Name> mixinTypeNames = payload.getMixinTypeNames();
            Set<JcrNodeDefinition> satisfiedChildNodes = new HashSet<JcrNodeDefinition>();
            Set<JcrPropertyDefinition> satisfiedProperties = new HashSet<JcrPropertyDefinition>();

            // Is this node referenceable ...
            boolean referenceable = false;
            try {
                referenceable = isReferenceable(node);
            } catch (RepositoryException e) {
                throw new ValidationException(e.getLocalizedMessage());
            }
            for (org.modeshape.graph.session.GraphSession.PropertyInfo<JcrPropertyPayload> property : node.getProperties()) {
                if (property.getName().equals(JcrLexicon.UUID) && !referenceable) continue;
                JcrPropertyPayload propPayload = property.getPayload();
                JcrPropertyDefinition definition = findBestPropertyDefintion(primaryTypeName,
                                                                             mixinTypeNames,
                                                                             property.getProperty(),
                                                                             propPayload.getPropertyType(),
                                                                             property.getProperty().isSingle(),
                                                                             false);
                if (definition == null) {
                    throw new ValidationException(JcrI18n.noDefinition.text("property",
                                                                            readable(property.getName()),
                                                                            readable(node.getPath()),
                                                                            readable(primaryTypeName),
                                                                            readable(mixinTypeNames)));
                }

                satisfiedProperties.add(definition);
            }

            for (org.modeshape.graph.session.GraphSession.Node<JcrNodePayload, JcrPropertyPayload> child : node.getChildren()) {
                int snsCount = node.getChildrenCount(child.getName());
                JcrNodePayload childPayload = child.getPayload();
                AbstractJcrNode childJcrNode = childPayload.getJcrNode();
                if (childJcrNode.isShared()) {
                    childJcrNode = ((JcrSharedNode)childJcrNode).proxyNode();
                    try {
                        childPayload = childJcrNode.nodeInfo().getPayload();
                    } catch (RepositoryException e) {
                        throw new ValidationException(e.getMessage(), e);
                    }
                }
                JcrNodeDefinition definition = nodeTypes().findChildNodeDefinition(primaryTypeName,
                                                                                   mixinTypeNames,
                                                                                   child.getName(),
                                                                                   childPayload.getPrimaryTypeName(),
                                                                                   snsCount,
                                                                                   false);
                if (definition == null) {
                    throw new ValidationException(JcrI18n.noDefinition.text("child node",
                                                                            readable(child.getName()),
                                                                            readable(node.getPath()),
                                                                            readable(primaryTypeName),
                                                                            readable(mixinTypeNames)));
                }
                satisfiedChildNodes.add(definition);
            }

            JcrNodeType primaryType = nodeTypes().getNodeType(primaryTypeName);
            boolean isLastModifiedType = primaryType.isNodeType(JcrMixLexicon.LAST_MODIFIED);
            boolean isCreatedType = primaryType.isNodeType(JcrMixLexicon.CREATED);
            boolean isETag = primaryType.isNodeType(JcrMixLexicon.ETAG);
            for (JcrPropertyDefinition definition : primaryType.getPropertyDefinitions()) {
                if (definition.isMandatory() && !definition.isProtected() && !satisfiedProperties.contains(definition)) {
                    throw new ValidationException(JcrI18n.noDefinition.text("property",
                                                                            definition.getName(),
                                                                            readable(node.getPath()),
                                                                            readable(primaryTypeName),
                                                                            readable(mixinTypeNames)));
                }
            }
            for (JcrNodeDefinition definition : primaryType.getChildNodeDefinitions()) {
                if (definition.isMandatory() && !definition.isProtected() && !satisfiedChildNodes.contains(definition)) {
                    throw new ValidationException(JcrI18n.noDefinition.text("child node",
                                                                            definition.getName(),
                                                                            readable(node.getPath()),
                                                                            readable(primaryTypeName),
                                                                            readable(mixinTypeNames)));
                }
            }

            if (mixinTypeNames != null) {
                for (Name mixinTypeName : mixinTypeNames) {
                    JcrNodeType mixinType = nodeTypes().getNodeType(mixinTypeName);
                    isLastModifiedType = isLastModifiedType || mixinType.isNodeType(JcrMixLexicon.LAST_MODIFIED);
                    isCreatedType = isCreatedType || mixinType.isNodeType(JcrMixLexicon.CREATED);
                    isETag = isETag || mixinType.isNodeType(JcrMixLexicon.ETAG);
                    for (JcrPropertyDefinition definition : mixinType.getPropertyDefinitions()) {
                        if (definition.isMandatory() && !definition.isProtected() && !satisfiedProperties.contains(definition)) {
                            throw new ValidationException(JcrI18n.noDefinition.text("child node",
                                                                                    definition.getName(),
                                                                                    readable(node.getPath()),
                                                                                    readable(primaryTypeName),
                                                                                    readable(mixinTypeNames)));
                        }
                    }
                    for (JcrNodeDefinition definition : mixinType.getChildNodeDefinitions()) {
                        if (definition.isMandatory() && !definition.isProtected() && !satisfiedChildNodes.contains(definition)) {
                            throw new ValidationException(JcrI18n.noDefinition.text("child node",
                                                                                    definition.getName(),
                                                                                    readable(node.getPath()),
                                                                                    readable(primaryTypeName),
                                                                                    readable(mixinTypeNames)));
                        }
                    }

                }
            }

            // Do we need to update the 'jcr:etag' property?
            if (isETag) {
                // Per section 3.7.12 of JCR 2, this property should be changed whenever BINARY properties are added, removed, or
                // changed. So, go through the properties (in sorted-name order so it is repeatable) and create this value
                // by simply concatenating the SHA-1 hash of each BINARY value ...
                List<Name> binaryPropertyNames = new ArrayList<Name>();
                for (org.modeshape.graph.session.GraphSession.PropertyInfo<JcrPropertyPayload> property : node.getProperties()) {
                    if (property.getProperty().size() == 0) continue;
                    if (property.getPayload().getPropertyType() != PropertyType.BINARY) continue;
                    binaryPropertyNames.add(property.getName());
                }
                StringBuilder sb = new StringBuilder();
                if (!binaryPropertyNames.isEmpty()) {
                    Collections.sort(binaryPropertyNames);
                    BinaryFactory binaryFactory = context().getValueFactories().getBinaryFactory();
                    for (Name name : binaryPropertyNames) {
                        org.modeshape.graph.session.GraphSession.PropertyInfo<JcrPropertyPayload> property = node.getProperty(name);
                        for (Object value : property.getProperty()) {
                            Binary binary = binaryFactory.create(value);
                            String hash = new String(binary.getHash()); // doesn't matter what charset, as long as its always the
                            // same
                            sb.append(hash);
                        }
                    }
                }
                String etagValue = sb.toString(); // may be empty
                setProperty(node, primaryTypeName, mixinTypeNames, false, JcrLexicon.ETAG, PropertyType.STRING, etagValue);
            }

            // See if the node is an instance of 'mix:created'.
            // This is done even if the node is not newly-created, because this needs to happen whenever the
            // 'mix:created' node type is added as a mixin (which can happen to an existing node).
            if (isCreatedType) {
                setPropertyIfAbsent(node, primaryTypeName, mixinTypeNames, false, JcrLexicon.CREATED, PropertyType.DATE, saveTime);
                setPropertyIfAbsent(node,
                                    primaryTypeName,
                                    mixinTypeNames,
                                    false,
                                    JcrLexicon.CREATED_BY,
                                    PropertyType.STRING,
                                    user);
            }

            // See if the node is an instance of 'mix:lastModified' ...
            if (isLastModifiedType) {
                // Check to see if the 'jcr:lastModified' or 'jcr:lastModifiedBy' properties were explicitly changed ...
                setPropertyIfAbsent(node,
                                    primaryTypeName,
                                    mixinTypeNames,
                                    false,
                                    JcrLexicon.LAST_MODIFIED,
                                    PropertyType.DATE,
                                    saveTime);
                setPropertyIfAbsent(node,
                                    primaryTypeName,
                                    mixinTypeNames,
                                    false,
                                    JcrLexicon.LAST_MODIFIED_BY,
                                    PropertyType.STRING,
                                    user);
            }
        }

        protected void setPropertyIfAbsent( org.modeshape.graph.session.GraphSession.Node<JcrNodePayload, JcrPropertyPayload> node,
                                            Name primaryTypeName,
                                            List<Name> mixinTypeNames,
                                            boolean skipProtected,
                                            Name propertyName,
                                            int propertyType,
                                            Object value ) {
            if (node.getProperty(propertyName) != null) return;
            setProperty(node, primaryTypeName, mixinTypeNames, skipProtected, propertyName, propertyType, value);
        }

        protected void setProperty( org.modeshape.graph.session.GraphSession.Node<JcrNodePayload, JcrPropertyPayload> node,
                                    Name primaryTypeName,
                                    List<Name> mixinTypeNames,
                                    boolean skipProtected,
                                    Name propertyName,
                                    int propertyType,
                                    Object value ) {
            Property graphProp = propertyFactory.create(propertyName, value);
            JcrPropertyDefinition propDefn = findBestPropertyDefintion(primaryTypeName,
                                                                       mixinTypeNames,
                                                                       graphProp,
                                                                       propertyType,
                                                                       true,
                                                                       skipProtected);
            AbstractJcrNode jcrNode = node.getPayload().getJcrNode();
            AbstractJcrProperty jcrProp = new JcrSingleValueProperty(SessionCache.this, jcrNode, propertyName);
            JcrPropertyPayload propPayload = new JcrPropertyPayload(propDefn.getId(), propertyType, jcrProp);
            node.setProperty(graphProp, false, propPayload);
        }

        @Override
        public void compute( Graph.Batch batch,
                             Node<JcrNodePayload, JcrPropertyPayload> node ) {
            try {
                JcrWorkspace workspace = session().workspace();

                // Some tests don't set this up.
                if (workspace != null) {
                    workspace.versionManager().initializeVersionHistoryFor(batch, node, null, false);
                }
            } catch (RepositoryException re) {
                throw new IllegalStateException(re);
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.session.GraphSession.NodeOperations#postCreateChild(org.modeshape.graph.session.GraphSession.Node,
         *      org.modeshape.graph.session.GraphSession.Node, java.util.Map)
         */
        @Override
        public void postCreateChild( Node<JcrNodePayload, JcrPropertyPayload> parent,
                                     Node<JcrNodePayload, JcrPropertyPayload> child,
                                     Map<Name, PropertyInfo<JcrPropertyPayload>> properties ) throws ValidationException {
            super.postCreateChild(parent, child, properties);
            // Populate the node and properties with the payloads ...

            // Get the 2 properties that WILL be here ...
            PropertyInfo<JcrPropertyPayload> primaryTypeInfo = properties.get(JcrLexicon.PRIMARY_TYPE);
            PropertyInfo<JcrPropertyPayload> nodeDefnInfo = properties.get(ModeShapeIntLexicon.NODE_DEFINITON);
            Name primaryTypeName = nameFactory().create(primaryTypeInfo.getProperty().getFirstValue());
            String nodeDefnIdStr = stringFactory().create(nodeDefnInfo.getProperty().getFirstValue());
            NodeDefinitionId nodeDefnId = NodeDefinitionId.fromString(nodeDefnIdStr, nameFactory);

            // Now create the payload ...
            PropertyInfo<JcrPropertyPayload> sharedUuidInfo = properties.get(ModeShapeLexicon.SHARED_UUID);
            UUID sharedUuid = sharedUuidInfo != null && ModeShapeLexicon.SHARE.equals(primaryTypeName) ? uuidFactory().create(sharedUuidInfo.getProperty()
                                                                                                                                            .getFirstValue()) : null;
            JcrNodePayload nodePayload = createJcrNodePayload(child, primaryTypeName, null, nodeDefnId, sharedUuid);
            child.setPayload(nodePayload);

            // Now update the property infos for the two mandatory properties ...
            JcrNodeType ntBase = nodeTypes().getNodeType(JcrNtLexicon.BASE);
            assert ntBase != null;
            primaryTypeInfo = createPropertyInfo(child.getPayload(),
                                                 primaryTypeInfo.getProperty(),
                                                 ntBase.allPropertyDefinitions(JcrLexicon.PRIMARY_TYPE).iterator().next(),
                                                 PropertyType.NAME,
                                                 primaryTypeInfo);
            properties.put(primaryTypeInfo.getName(), primaryTypeInfo);
            nodeDefnInfo = createPropertyInfo(child.getPayload(),
                                              nodeDefnInfo.getProperty(),
                                              ntBase.allPropertyDefinitions(ModeShapeIntLexicon.NODE_DEFINITON).iterator().next(),
                                              PropertyType.STRING,
                                              nodeDefnInfo);
            properties.put(nodeDefnInfo.getName(), nodeDefnInfo);

            // The UUID property is optional ...
            PropertyInfo<JcrPropertyPayload> uuidInfo = properties.get(JcrLexicon.UUID);
            if (uuidInfo != null) {
                JcrNodeType mixRef = nodeTypes().getNodeType(JcrMixLexicon.REFERENCEABLE);
                assert mixRef != null;
                uuidInfo = createPropertyInfo(child.getPayload(),
                                              uuidInfo.getProperty(),
                                              mixRef.allPropertyDefinitions(JcrLexicon.UUID).iterator().next(),
                                              PropertyType.STRING,
                                              uuidInfo);
                properties.put(uuidInfo.getName(), uuidInfo);
            }
        }

        protected final Set<Name> getSingleMultiPropertyNames( Property dnaProperty,
                                                               Location location ) {
            Set<Name> multiValuedPropertyNames = new HashSet<Name>();
            for (Object value : dnaProperty) {
                try {
                    multiValuedPropertyNames.add(nameFactory.create(value));
                } catch (ValueFormatException e) {
                    String msg = "{0} value \"{1}\" on {2} in \"{3}\" workspace is not a valid name and is being ignored";
                    LOGGER.trace(e,
                                 msg,
                                 readable(ModeShapeIntLexicon.MULTI_VALUED_PROPERTIES),
                                 value,
                                 readable(location),
                                 workspaceName());
                }
            }
            return multiValuedPropertyNames;
        }
    }

    @Immutable
    final class JcrAuthorizer implements GraphSession.Authorizer {
        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.session.GraphSession.Authorizer#checkPermissions(org.modeshape.graph.property.Path,
         *      org.modeshape.graph.session.GraphSession.Authorizer.Action)
         */
        public void checkPermissions( Path path,
                                      Action action ) throws AccessControlException {
            String jcrAction = null;
            switch (action) {
                case ADD_NODE:
                    jcrAction = ModeShapePermissions.ADD_NODE;
                    break;
                case READ:
                    jcrAction = ModeShapePermissions.READ;
                    break;
                case REMOVE:
                    jcrAction = ModeShapePermissions.REMOVE;
                    break;
                case SET_PROPERTY:
                    jcrAction = ModeShapePermissions.SET_PROPERTY;
                    break;
            }
            session().checkPermission(path, jcrAction);
        }
    }

    @Immutable
    final static class JcrPropertyPayload {
        private final PropertyDefinitionId propertyDefinitionId;
        private final int jcrPropertyType;
        private final AbstractJcrProperty jcrProperty;

        JcrPropertyPayload( PropertyDefinitionId propertyDefinitionId,
                            int jcrPropertyType,
                            AbstractJcrProperty jcrProperty ) {
            assert jcrProperty != null;
            this.propertyDefinitionId = propertyDefinitionId;
            this.jcrPropertyType = jcrPropertyType;
            this.jcrProperty = jcrProperty;
        }

        /**
         * @return jcrProperty
         */
        public AbstractJcrProperty getJcrProperty() {
            return jcrProperty;
        }

        /**
         * @return jcrPropertyType
         */
        public int getPropertyType() {
            return jcrPropertyType;
        }

        /**
         * @return propertyDefinitionId
         */
        public PropertyDefinitionId getPropertyDefinitionId() {
            return propertyDefinitionId;
        }

        public JcrPropertyPayload with( PropertyDefinitionId propertyDefinitionId ) {
            return new JcrPropertyPayload(propertyDefinitionId, jcrPropertyType, jcrProperty);
        }

        public JcrPropertyPayload with( int jcrPropertyType ) {
            return new JcrPropertyPayload(propertyDefinitionId, jcrPropertyType, jcrProperty);
        }

        public JcrPropertyPayload with( AbstractJcrProperty jcrProperty ) {
            return new JcrPropertyPayload(propertyDefinitionId, jcrPropertyType, jcrProperty);
        }
    }

    @Immutable
    static class JcrNodePayload {
        protected final SessionCache cache;
        protected final Node<JcrNodePayload, JcrPropertyPayload> owner;
        protected final Name primaryTypeName;
        protected final List<Name> mixinTypeNames;
        protected final NodeDefinitionId nodeDefinitionId;
        protected SoftReference<AbstractJcrNode> jcrNode;

        JcrNodePayload( SessionCache cache,
                        Node<JcrNodePayload, JcrPropertyPayload> owner,
                        Name primaryTypeName,
                        List<Name> mixinTypeNames,
                        NodeDefinitionId nodeDefinitionId ) {
            assert owner != null;
            assert cache != null;
            this.cache = cache;
            this.owner = owner;
            this.primaryTypeName = primaryTypeName;
            this.mixinTypeNames = mixinTypeNames;
            this.nodeDefinitionId = nodeDefinitionId;
            this.jcrNode = new SoftReference<AbstractJcrNode>(null);
        }

        JcrNodePayload( SessionCache cache,
                        Node<JcrNodePayload, JcrPropertyPayload> owner,
                        Name primaryTypeName,
                        List<Name> mixinTypeNames,
                        NodeDefinitionId nodeDefinitionId,
                        SoftReference<AbstractJcrNode> jcrNode ) {
            assert jcrNode != null;
            assert owner != null;
            assert cache != null;
            this.cache = cache;
            this.owner = owner;
            this.primaryTypeName = primaryTypeName;
            this.mixinTypeNames = mixinTypeNames;
            this.nodeDefinitionId = nodeDefinitionId;
            this.jcrNode = jcrNode;
        }

        /**
         * @return primaryTypeName
         */
        public Name getPrimaryTypeName() {
            return this.primaryTypeName;
        }

        /**
         * Get the names of the mixin types for this node.
         * 
         * @return the unmodifiable list of mixin type names; never null but possibly empty
         */
        public List<Name> getMixinTypeNames() {
            return this.mixinTypeNames != null ? this.mixinTypeNames : Collections.<Name>emptyList();
        }

        /**
         * @return definition
         */
        public NodeDefinitionId getDefinitionId() {
            return this.nodeDefinitionId;
        }

        public AbstractJcrNode getJcrNode( boolean loadIfMissing ) {
            return loadIfMissing ? getJcrNode() : jcrNode.get();
        }

        /**
         * Get the JCR node instance.
         * 
         * @return jcrNode
         */
        public AbstractJcrNode getJcrNode() {
            AbstractJcrNode node = jcrNode.get();
            if (node == null) {
                if (owner.isRoot()) {
                    node = new JcrRootNode(cache, owner.getNodeId(), owner.getLocation());
                } else {
                    node = new JcrNode(cache, owner.getNodeId(), owner.getLocation());
                }
                jcrNode = new SoftReference<AbstractJcrNode>(node);
            }

            if (JcrNtLexicon.VERSION.equals(primaryTypeName)) {
                return new JcrVersionNode(jcrNode.get());
            }
            if (JcrNtLexicon.VERSION_HISTORY.equals(primaryTypeName)) {
                return new JcrVersionHistoryNode(jcrNode.get());
            }

            return jcrNode.get();
        }

        public JcrNodePayload with( Name primaryTypeName ) {
            return new JcrNodePayload(cache, owner, primaryTypeName, mixinTypeNames, nodeDefinitionId, jcrNode);
        }

        public JcrNodePayload with( List<Name> mixinTypeNames ) {
            return new JcrNodePayload(cache, owner, primaryTypeName, mixinTypeNames, nodeDefinitionId, jcrNode);
        }

        public JcrNodePayload with( NodeDefinitionId nodeDefinitionId ) {
            return new JcrNodePayload(cache, owner, primaryTypeName, mixinTypeNames, nodeDefinitionId, jcrNode);
        }

        public JcrNodePayload with( AbstractJcrNode jcrNode ) {
            return new JcrNodePayload(cache, owner, primaryTypeName, mixinTypeNames, nodeDefinitionId,
                                      new SoftReference<AbstractJcrNode>(jcrNode));
        }
    }

    @Immutable
    static class JcrSharedNodePayload extends JcrNodePayload {
        protected SoftReference<AbstractJcrNode> sharedNode;
        protected Location sharedLocation;

        JcrSharedNodePayload( SessionCache cache,
                              Node<JcrNodePayload, JcrPropertyPayload> owner,
                              Name primaryTypeName,
                              List<Name> mixinTypeNames,
                              NodeDefinitionId nodeDefinitionId,
                              AbstractJcrNode sharedNode ) {
            super(cache, owner, primaryTypeName, mixinTypeNames, nodeDefinitionId);
            assert sharedNode != null;
            this.sharedNode = new SoftReference<AbstractJcrNode>(sharedNode);
            this.sharedLocation = sharedNode.location();
        }

        JcrSharedNodePayload( SessionCache cache,
                              Node<JcrNodePayload, JcrPropertyPayload> owner,
                              Name primaryTypeName,
                              List<Name> mixinTypeNames,
                              NodeDefinitionId nodeDefinitionId,
                              SoftReference<AbstractJcrNode> jcrNode,
                              SoftReference<AbstractJcrNode> sharedNode ) {
            super(cache, owner, primaryTypeName, mixinTypeNames, nodeDefinitionId, jcrNode);
            assert this.sharedNode != null;
            this.sharedNode = sharedNode;
        }

        /**
         * Get the JCR node instance.
         * 
         * @return jcrNode
         */
        @Override
        public AbstractJcrNode getJcrNode() {
            AbstractJcrNode proxy = super.getJcrNode();
            AbstractJcrNode shared = sharedNode.get();
            if (shared == null) {
                UUID uuid = cache.uuidFactory().create(owner.getProperty(ModeShapeLexicon.SHARED_UUID));
                try {
                    AbstractJcrNode node = cache.findJcrNode(Location.create(uuid));
                    sharedNode = new SoftReference<AbstractJcrNode>(node);
                } catch (RepositoryException e) {
                    // We can't find the shared node, so don't wrap this node (just continue)
                }
            }
            return new JcrSharedNode(proxy, sharedNode.get());
        }

        @Override
        public JcrNodePayload with( Name primaryTypeName ) {
            return new JcrSharedNodePayload(cache, owner, primaryTypeName, mixinTypeNames, nodeDefinitionId, jcrNode, sharedNode);
        }

        @Override
        public JcrNodePayload with( List<Name> mixinTypeNames ) {
            return new JcrSharedNodePayload(cache, owner, primaryTypeName, mixinTypeNames, nodeDefinitionId, jcrNode, sharedNode);
        }

        @Override
        public JcrNodePayload with( NodeDefinitionId nodeDefinitionId ) {
            return new JcrSharedNodePayload(cache, owner, primaryTypeName, mixinTypeNames, nodeDefinitionId, jcrNode, sharedNode);
        }

        @Override
        public JcrNodePayload with( AbstractJcrNode jcrNode ) {
            return new JcrSharedNodePayload(cache, owner, primaryTypeName, mixinTypeNames, nodeDefinitionId,
                                            new SoftReference<AbstractJcrNode>(jcrNode), null);
        }
    }
}
