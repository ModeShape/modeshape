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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.nodetype.InvalidNodeTypeDefinitionException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.query.InvalidQueryException;
import org.modeshape.common.annotation.GuardedBy;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.api.query.qom.QueryCommand;
import org.modeshape.jcr.cache.NodeCache;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.RepositoryCache;
import org.modeshape.jcr.cache.SessionCache;
import org.modeshape.jcr.cache.change.Change;
import org.modeshape.jcr.cache.change.ChangeSet;
import org.modeshape.jcr.cache.change.ChangeSetListener;
import org.modeshape.jcr.cache.change.NodeAdded;
import org.modeshape.jcr.cache.change.NodeRemoved;
import org.modeshape.jcr.cache.change.PropertyChanged;
import org.modeshape.jcr.query.CancellableQuery;
import org.modeshape.jcr.query.NodeSequence;
import org.modeshape.jcr.query.NodeSequence.Batch;
import org.modeshape.jcr.query.QueryResults;
import org.modeshape.jcr.query.model.TypeSystem;
import org.modeshape.jcr.query.parse.BasicSqlQueryParser;
import org.modeshape.jcr.query.parse.QueryParser;
import org.modeshape.jcr.query.validate.Schemata;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NameFactory;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.PathFactory;
import org.modeshape.jcr.value.ValueFactory;

/**
 * The {@link RepositoryNodeTypeManager} is the maintainer of node type information for the entire repository at run-time. The
 * repository manager maintains a list of all node types and the ability to retrieve node types by {@link Name}.
 * <p>
 * The JCR 1.0 and 2.0 specifications both require that node type information be shared across all sessions within a repository
 * and that the {@link javax.jcr.nodetype.NodeTypeManager} perform operations based on the string versions of {@link Name}s based
 * on the permanent (workspace-scoped) and transient (session-scoped) namespace mappings. ModeShape achieves this by maintaining a
 * single master repository of all node type information (the {@link RepositoryNodeTypeManager}) and per-session wrappers (
 * {@link JcrNodeTypeManager}) for this master repository that perform {@link String} to {@link Name} translation based on the
 * {@link javax.jcr.Session}'s transient mappings and then delegating node type lookups to the repository manager.
 * </p>
 */
@ThreadSafe
class RepositoryNodeTypeManager implements ChangeSetListener, NodeTypes.Supplier {

    private final JcrRepository.RunningState repository;
    private final ExecutionContext context;
    private final String systemWorkspaceName;
    private final Path nodeTypesPath;
    private final NameFactory nameFactory;
    private final Logger logger = Logger.getLogger(getClass());

    private final ReadWriteLock nodeTypesLock = new ReentrantReadWriteLock();
    @GuardedBy( "nodeTypesLock" )
    private volatile NodeTypes nodeTypesCache;

    private final QueryParser queryParser;
    private final boolean includeColumnsForInheritedProperties;
    private final boolean includePseudoColumnsInSelectStar;
    private volatile NodeTypeSchemata schemata;

    private final CopyOnWriteArrayList<NodeTypes.Listener> listeners = new CopyOnWriteArrayList<>();

    RepositoryNodeTypeManager( JcrRepository.RunningState repository,
                               boolean includeColumnsForInheritedProperties,
                               boolean includePseudoColumnsInSelectStar ) {
        this.repository = repository;
        this.context = repository.context();
        this.nameFactory = this.context.getValueFactories().getNameFactory();
        this.systemWorkspaceName = this.repository.repositoryCache().getSystemWorkspaceName();

        PathFactory pathFactory = this.context.getValueFactories().getPathFactory();
        this.nodeTypesPath = pathFactory.createAbsolutePath(JcrLexicon.SYSTEM, JcrLexicon.NODE_TYPES);
        this.nodeTypesCache = new NodeTypes(this.context);

        this.includeColumnsForInheritedProperties = includeColumnsForInheritedProperties;
        this.includePseudoColumnsInSelectStar = includePseudoColumnsInSelectStar;
        queryParser = new BasicSqlQueryParser();
    }

    RepositoryNodeTypeManager with( JcrRepository.RunningState repository,
                                    boolean includeColumnsForInheritedProperties,
                                    boolean includePseudoColumnsInSelectStar ) {
        assert this.systemWorkspaceName.equals(repository.repositoryCache().getSystemWorkspaceName());
        PathFactory pathFactory = repository.context().getValueFactories().getPathFactory();
        Path nodeTypesPath = pathFactory.createAbsolutePath(JcrLexicon.SYSTEM, JcrLexicon.NODE_TYPES);
        assert this.nodeTypesPath.equals(nodeTypesPath);
        RepositoryNodeTypeManager result = new RepositoryNodeTypeManager(repository, includeColumnsForInheritedProperties,
                                                                         includePseudoColumnsInSelectStar);
        // Now copy the node types from this cache into the new manager's cache ...
        // (If we didn't do this, we'd have to refresh from the system storage)
        result.nodeTypesCache = result.nodeTypesCache.with(this.nodeTypesCache.getAllNodeTypes());

        // Do not copy the listeners
        return result;
    }

    protected final ValueFactory<String> strings() {
        return this.context.getValueFactories().getStringFactory();
    }

    @Override
    public NodeTypes getNodeTypes() {
        return nodeTypesCache;
    }

    /**
     * Add a listener that will be notified when the NodeTypes changes. Listeners will be called in a single thread, and should do
     * almost no work.
     *
     * @param listener the new listener
     * @return true if the listener was registered, or false if {@code listener} is null or if it is already registered
     */
    final boolean registerListener( NodeTypes.Listener listener ) {
        return listener != null ? this.listeners.addIfAbsent(listener) : false;
    }

    /**
     * Remove an existing listener to NodeTypes changes.
     *
     * @param listener the existing listener
     * @return true if the listener was removed, or false otherwise
     */
    final boolean unregisterListener( NodeTypes.Listener listener ) {
        return this.listeners.remove(listener);
    }

    private void notifiyListeners() {
        for (NodeTypes.Listener listener : this.listeners) {
            assert listener != null;
            try {
                listener.notify(nodeTypesCache);
            } catch (RuntimeException e) {
                logger.error(e, JcrI18n.errorNotifyingNodeTypesListener, listener);
            }
        }
    }

    /**
     * Allows the collection of node types to be unregistered if they are not referenced by other node types as supertypes,
     * default primary types of child nodes, or required primary types of child nodes.
     *
     * @param nodeTypeNames the names of the node types to be unregistered
     * @param failIfNodeTypesAreUsed true if this method should fail to unregister the named node types if any of the node types
     *        are still in use by nodes, or false if this method should not perform such a check
     * @throws NoSuchNodeTypeException if any of the node type names do not correspond to a registered node type
     * @throws InvalidNodeTypeDefinitionException if any of the node types with the given names cannot be unregistered because
     *         they are the supertype, one of the required primary types, or a default primary type of a node type that is not
     *         being unregistered.
     * @throws RepositoryException if any other error occurs
     */
    void unregisterNodeType( Collection<Name> nodeTypeNames,
                             boolean failIfNodeTypesAreUsed )
        throws NoSuchNodeTypeException, InvalidNodeTypeDefinitionException, RepositoryException {
        CheckArg.isNotNull(nodeTypeNames, "nodeTypeNames");
        if (nodeTypeNames.isEmpty()) return;

        if (failIfNodeTypesAreUsed) {
            long start = System.nanoTime();
            // Search the content graph to make sure that this type isn't being used
            for (Name nodeTypeName : nodeTypeNames) {
                if (isNodeTypeInUse(nodeTypeName)) {
                    String name = nodeTypeName.getString(context.getNamespaceRegistry());
                    throw new InvalidNodeTypeDefinitionException(JcrI18n.cannotUnregisterInUseType.text(name));
                }
            }
            long time = TimeUnit.MILLISECONDS.convert(Math.abs(System.nanoTime() - start), TimeUnit.NANOSECONDS);
            logger.debug("{0} milliseconds to check if any of these node types are unused before unregistering them: {1}", time,
                         nodeTypeNames);
        }

        try {
            /*
             * Grab an exclusive lock on this data to keep other nodes from being added/saved while the unregistration checks are occurring
             */
            List<JcrNodeType> removedNodeTypes = new ArrayList<JcrNodeType>(nodeTypeNames.size());
            nodeTypesLock.writeLock().lock();
            final NodeTypes nodeTypes = this.nodeTypesCache;

            for (Name nodeTypeName : nodeTypeNames) {
                /*
                 * Check that the type names are valid
                 */
                if (nodeTypeName == null) {
                    throw new NoSuchNodeTypeException(JcrI18n.invalidNodeTypeName.text());
                }
                String name = nodeTypeName.getString(context.getNamespaceRegistry());

                JcrNodeType foundNodeType = nodeTypes.getNodeType(nodeTypeName);
                if (foundNodeType == null) {
                    throw new NoSuchNodeTypeException(JcrI18n.noSuchNodeType.text(name));
                }
                removedNodeTypes.add(foundNodeType);

                /*
                 * Check that no other node definitions have dependencies on any of the named types
                 */
                for (JcrNodeType nodeType : nodeTypes.getAllNodeTypes()) {
                    // If this node is also being unregistered, don't run checks against it
                    if (nodeTypeNames.contains(nodeType.getInternalName())) {
                        continue;
                    }

                    for (JcrNodeType supertype : nodeType.supertypes()) {
                        if (nodeTypeName.equals(supertype.getInternalName())) {
                            throw new InvalidNodeTypeDefinitionException(JcrI18n.cannotUnregisterSupertype.text(name,
                                                                                                                nodeType.getName()));
                        }
                    }

                    for (JcrNodeDefinition childNode : nodeType.childNodeDefinitions()) {
                        NodeType defaultPrimaryType = childNode.getDefaultPrimaryType();
                        if (defaultPrimaryType != null && name.equals(defaultPrimaryType.getName())) {
                            throw new InvalidNodeTypeDefinitionException(
                                                                         JcrI18n.cannotUnregisterDefaultPrimaryType.text(name,
                                                                                                                         nodeType.getName(),
                                                                                                                         childNode.getName()));
                        }
                        if (childNode.requiredPrimaryTypeNameSet().contains(nodeTypeName)) {
                            throw new InvalidNodeTypeDefinitionException(
                                                                         JcrI18n.cannotUnregisterRequiredPrimaryType.text(name,
                                                                                                                          nodeType.getName(),
                                                                                                                          childNode.getName()));
                        }
                    }
                }
            }

            // Create the new cache ...
            NodeTypes newNodeTypes = nodeTypes.without(removedNodeTypes);

            // Remove the node types from persistent storage ...
            SessionCache system = repository.createSystemSession(context, false);
            SystemContent systemContent = new SystemContent(system);
            systemContent.unregisterNodeTypes(removedNodeTypes.toArray(new JcrNodeType[removedNodeTypes.size()]));
            systemContent.save();

            // Now change the cache ...
            this.nodeTypesCache = newNodeTypes;
            this.schemata = null;
            notifiyListeners();
        } finally {
            nodeTypesLock.writeLock().unlock();
        }
    }

    NodeTypeSchemata getRepositorySchemata() {
        // Try reading first, since this will work most of the time ...
        if (schemata != null) return schemata;
        // This is idempotent, so it's okay not to lock ...
        schemata = new NodeTypeSchemata(context, nodeTypesCache, includeColumnsForInheritedProperties,
                                        includePseudoColumnsInSelectStar);
        return schemata;
    }

    void signalNamespaceChanges() {
        this.schemata = null;
    }

    /**
     * Check if the named node type is in use in any workspace in the repository
     *
     * @param nodeTypeName the name of the node type to check
     * @return true if at least one node is using that type; false otherwise
     * @throws InvalidQueryException if there is an error searching for uses of the named node type
     */
    boolean isNodeTypeInUse( Name nodeTypeName ) throws InvalidQueryException {

        String nodeTypeString = nodeTypeName.getString(context.getNamespaceRegistry());
        String expression = "SELECT * from [" + nodeTypeString + "] LIMIT 1";
        TypeSystem typeSystem = context.getValueFactories().getTypeSystem();
        // Parsing must be done now ...
        QueryCommand command = queryParser.parseQuery(expression, typeSystem);
        assert command != null : "Could not parse " + expression;

        Schemata schemata = getRepositorySchemata();

        // Now query the entire repository for any nodes that use this node type ...
        RepositoryCache repoCache = repository.repositoryCache();
        RepositoryQueryManager queryManager = repository.queryManager();
        Set<String> workspaceNames = repoCache.getWorkspaceNames();
        Map<String, NodeCache> overridden = null;
        NodeTypes nodeTypes = repository.nodeTypeManager().getNodeTypes();
        RepositoryIndexes indexDefns = repository.queryManager().getIndexes();
        CancellableQuery query = queryManager.query(context, repoCache, workspaceNames, overridden, command, schemata,
                                                    indexDefns, nodeTypes, null, null);
        try {
            QueryResults result = query.execute();
            if (result.isEmpty()) return false;
            if (result.getRowCount() < 0) {
                // Try to get the first row ...
                NodeSequence seq = result.getRows();
                Batch batch = seq.nextBatch();
                while (batch != null) {
                    if (batch.hasNext()) return true;
                    // It's not common for the first batch may be empty, but it's possible. So try the next batch ...
                    batch = seq.nextBatch();
                }
                return false;
            }
            return result.getRowCount() > 0;
        } catch (RepositoryException e) {
            logger.error(e, JcrI18n.errorCheckingNodeTypeUsage, nodeTypeName, e.getLocalizedMessage());
            return true;
        }
    }

    /**
     * Registers a new node type or updates an existing node type using the specified definition and returns the resulting
     * {@code NodeType} object.
     * <p>
     * For details, see {@link #registerNodeTypes(Iterable)}.
     * </p>
     *
     * @param ntd the {@code NodeTypeDefinition} to register
     * @return the newly registered (or updated) {@code NodeType}
     * @throws InvalidNodeTypeDefinitionException if the {@code NodeTypeDefinition} is invalid
     * @throws NodeTypeExistsException if <code>allowUpdate</code> is false and the {@code NodeTypeDefinition} specifies a node
     *         type name that is already registered
     * @throws RepositoryException if another error occurs
     */
    JcrNodeType registerNodeType( NodeTypeDefinition ntd )
        throws InvalidNodeTypeDefinitionException, NodeTypeExistsException, RepositoryException {

        return registerNodeType(ntd, true);
    }

    /**
     * Registers a new node type or updates an existing node type using the specified definition and returns the resulting
     * {@code NodeType} object.
     * <p>
     * For details, see {@link #registerNodeTypes(Iterable)}.
     * </p>
     *
     * @param ntd the {@code NodeTypeDefinition} to register
     * @param failIfNodeTypeExists indicates whether the registration should proceed if there is already a type with the same
     *        name; {@code true} indicates that the registration should fail with an error if a node type with the same name
     *        already exists
     * @return the newly registered (or updated) {@code NodeType}
     * @throws InvalidNodeTypeDefinitionException if the {@code NodeTypeDefinition} is invalid
     * @throws NodeTypeExistsException if <code>allowUpdate</code> is false and the {@code NodeTypeDefinition} specifies a node
     *         type name that is already registered
     * @throws RepositoryException if another error occurs
     */
    JcrNodeType registerNodeType( NodeTypeDefinition ntd,
                                  boolean failIfNodeTypeExists )
        throws InvalidNodeTypeDefinitionException, NodeTypeExistsException, RepositoryException {
        assert ntd != null;
        List<JcrNodeType> result = registerNodeTypes(Collections.singletonList(ntd), failIfNodeTypeExists, false, true);
        return result.isEmpty() ? null : result.get(0);
    }

    /**
     * Registers or updates the specified {@code Collection} of {@link NodeTypeDefinition} objects.
     * <p>
     * This method is used to register or update a set of node types with mutual dependencies.
     * </p>
     * <p>
     * The effect of this method is &quot;all or nothing&quot;; if an error occurs, no node types are registered or updated.
     * </p>
     * <p>
     * <b>ModeShape Implementation Notes</b>
     * </p>
     * <p>
     * ModeShape currently supports registration of batches of types with some constraints. ModeShape will allow types to be
     * registered if they meet the following criteria:
     * <ol>
     * <li>The batch must consist of {@code NodeTypeDefinitionTemplate node type definition templates} created through the user's
     * JCR session.</li>
     * <li>Existing types cannot be modified in-place - They must be unregistered and re-registered</li>
     * <li>Types must have a non-null, non-empty name</li>
     * <li>If a primary item name is specified for the node type, it must match the name of a property OR a child node, not both</li>
     * <li>Each type must have a valid set of supertypes - that is, the type's supertypes must meet the following criteria:
     * <ol>
     * <li>The type must have at least one supertype (unless the type is {@code nt:base}.</li>
     * <li>No two supertypes {@code t1} and {@code t2} can declare each declare a property ({@code p1} and {@code p2}) with the
     * same name and cardinality ({@code p1.isMultiple() == p2.isMultiple()}). Note that this does prohibit each {@code t1} and
     * {@code t2} from having a common supertype (or super-supertype, etc.) that declares a property).</li>
     * <li>No two supertypes {@code t1} and {@code t2} can declare each declare a child node ({@code n1} and {@code n2}) with the
     * same name and SNS status ({@code p1.allowsSameNameSiblings() == p2.allowsSameNameSiblings()}). Note that this does prohibit
     * each {@code t1} and {@code t2} from having a common supertype (or super-supertype, etc.) that declares a child node).</li>
     * </ol>
     * </li>
     * <li>Each type must have a valid set of properties - that is, the type's properties must meet the following criteria:
     * <ol>
     * <li>Residual property definitions cannot be mandatory</li>
     * <li>If the property is auto-created, it must specify a default value</li>
     * <li>If the property is single-valued, it can only specify a single default value</li>
     * <li>If the property overrides an existing property definition from a supertype, the new definition must be mandatory if the
     * old definition was mandatory</li>
     * <li>The property cannot override an existing property definition from a supertype if the ancestor definition is protected</li>
     * <li>If the property overrides an existing property definition from a supertype that specifies value constraints, the new
     * definition must have the same value constraints as the old definition. <i>This requirement may be relaxed in a future
     * version of ModeShape.</i></li>
     * <li>If the property overrides an existing property definition from a supertype, the new definition must have the same
     * required type as the old definition or a required type that can ALWAYS be cast to the required type of the ancestor (see
     * section 3.6.4 of the JCR 2.0 specification)</li>
     * </ol>
     * Note that an empty set of properties would meet the above criteria.</li>
     * <li>The type must have a valid set of child nodes - that is, the types's child nodes must meet the following criteria:
     * <ol>
     * <li>Residual child node definitions cannot be mandatory</li>
     * <li>If the child node is auto-created, it must specify a default primary type name</li>
     * <li>If the child node overrides an existing child node definition from a supertype, the new definition must be mandatory if
     * the old definition was mandatory</li>
     * <li>The child node cannot override an existing child node definition from a supertype if the ancestor definition is
     * protected</li>
     * <li>If the child node overrides an existing child node definition from a supertype, the required primary types of the new
     * definition must be more restrictive than the required primary types of the old definition - that is, the new primary types
     * must defined such that any type that satisfies all of the required primary types for the new definition must also satisfy
     * all of the required primary types for the old definition. This requirement is analogous to the requirement that overriding
     * property definitions have a required type that is always convertible to the required type of the overridden definition.</li>
     * </ol>
     * Note that an empty set of child nodes would meet the above criteria.</li>
     * </p>
     *
     * @param nodeTypeDefns the {@link NodeTypeDefinition node type definitions} to register
     * @return the newly registered (or updated) {@link NodeType NodeTypes}
     * @throws UnsupportedRepositoryOperationException if {@code allowUpdates == true}. ModeShape does not support this capability
     *         at this time but the parameter has been retained for API compatibility.
     * @throws InvalidNodeTypeDefinitionException if the {@link NodeTypeDefinition} is invalid
     * @throws NodeTypeExistsException if <code>allowUpdate</code> is false and the {@link NodeTypeDefinition} specifies a node
     *         type name that is already registered
     * @throws RepositoryException if another error occurs
     */
    List<JcrNodeType> registerNodeTypes( Iterable<NodeTypeDefinition> nodeTypeDefns )
        throws InvalidNodeTypeDefinitionException, NodeTypeExistsException, RepositoryException {
        return registerNodeTypes(nodeTypeDefns, true, false, true);
    }

    List<JcrNodeType> registerNodeTypes( Iterable<NodeTypeDefinition> nodeTypeDefns,
                                         boolean failIfNodeTypeDefinitionsExist,
                                         boolean skipIfNodeTypeDefinitionExists,
                                         boolean persist )
        throws InvalidNodeTypeDefinitionException, NodeTypeExistsException, RepositoryException {

        if (nodeTypeDefns == null) {
            return Collections.emptyList();
        }

        List<JcrNodeType> typesPendingRegistration = new ArrayList<JcrNodeType>();

        try {
            nodeTypesLock.writeLock().lock();
            final NodeTypes nodeTypes = this.nodeTypesCache;

            for (NodeTypeDefinition nodeTypeDefn : nodeTypeDefns) {
                if (nodeTypeDefn instanceof JcrNodeTypeTemplate) {
                    // Switch to use this context, so names are properly prefixed ...
                    nodeTypeDefn = ((JcrNodeTypeTemplate)nodeTypeDefn).with(context);
                }
                Name internalName = nodeTypes.nameFactory().create(nodeTypeDefn.getName());
                if (internalName == null || internalName.getLocalName().length() == 0) {
                    throw new InvalidNodeTypeDefinitionException(JcrI18n.invalidNodeTypeName.text());
                }

                boolean found = nodeTypes.hasNodeType(internalName);
                if (found && failIfNodeTypeDefinitionsExist) {
                    String name = nodeTypeDefn.getName();
                    throw new NodeTypeExistsException(internalName, JcrI18n.nodeTypeAlreadyExists.text(name));
                }
                if (found && skipIfNodeTypeDefinitionExists) continue;

                List<JcrNodeType> supertypes = nodeTypes.supertypesFor(nodeTypeDefn, typesPendingRegistration);
                JcrNodeType nodeType = nodeTypeFrom(nodeTypeDefn, supertypes);

                typesPendingRegistration.add(nodeType);
            }

            if (!typesPendingRegistration.isEmpty()) {
                // Make sure the nodes have primary types that are either already registered, or pending registration ...
                validateTypes(typesPendingRegistration);

                // Validate each of types that should be registered
                for (JcrNodeType typePendingRegistration : typesPendingRegistration) {
                    nodeTypes.validate(typePendingRegistration, Arrays.asList(typePendingRegistration.getDeclaredSupertypes()),
                                       typesPendingRegistration);
                }

                SystemContent system = null;
                if (persist) {
                    SessionCache systemCache = repository.createSystemSession(context, false);
                    system = new SystemContent(systemCache);
                }

                for (JcrNodeType nodeType : typesPendingRegistration) {
                    if (system != null) system.store(nodeType, true);
                }

                // Create the new cache ...
                NodeTypes newNodeTypes = nodeTypes.with(typesPendingRegistration);

                // Save the changes ...
                if (system != null) system.save();

                // And finally update the capabilities cache ...
                this.nodeTypesCache = newNodeTypes;
                this.schemata = null;
                notifiyListeners();
            }
        } finally {
            nodeTypesLock.writeLock().unlock();
        }

        return typesPendingRegistration;
    }

    private void validateTypes( List<JcrNodeType> typesPendingRegistration ) throws RepositoryException {
        NodeTypes nodeTypes = this.nodeTypesCache;

        for (JcrNodeType nodeType : typesPendingRegistration) {
            for (JcrNodeDefinition nodeDef : nodeType.getDeclaredChildNodeDefinitions()) {
                Name[] requiredPrimaryTypeNames = nodeDef.requiredPrimaryTypeNames();
                for (Name primaryTypeName : requiredPrimaryTypeNames) {
                    JcrNodeType requiredPrimaryType = nodeTypes.findTypeInMapOrList(primaryTypeName, typesPendingRegistration);
                    if (requiredPrimaryType == null) {
                        String msg = JcrI18n.invalidPrimaryTypeName.text(primaryTypeName, nodeType.getName());
                        throw new RepositoryException(msg);
                    }
                }
            }

            if (nodeType.isMixin()) {
                for (NodeType superType : nodeType.getSupertypes()) {
                    if (!superType.isMixin()) {
                        String msg = JcrI18n.invalidMixinSupertype.text(nodeType.getName(), superType.getName());
                        throw new RepositoryException(msg);
                    }
                }
            }
        }
    }

    private JcrNodeType nodeTypeFrom( NodeTypeDefinition nodeType,
                                      List<JcrNodeType> supertypes ) throws RepositoryException {
        PropertyDefinition[] propDefns = nodeType.getDeclaredPropertyDefinitions();
        NodeDefinition[] childDefns = nodeType.getDeclaredChildNodeDefinitions();
        List<JcrPropertyDefinition> properties = new ArrayList<JcrPropertyDefinition>();
        List<JcrNodeDefinition> childNodes = new ArrayList<JcrNodeDefinition>();

        if (propDefns != null) {
            for (PropertyDefinition propDefn : propDefns) {
                properties.add(propertyDefinitionFrom(propDefn));
            }
        }
        if (childDefns != null) {
            for (NodeDefinition childNodeDefn : childDefns) {
                childNodes.add(childNodeDefinitionFrom(childNodeDefn));
            }
        }

        Name name = nameFactory.create(nodeType.getName());
        Name primaryItemName = nameFactory.create(nodeType.getPrimaryItemName());
        boolean mixin = nodeType.isMixin();
        boolean isAbstract = nodeType.isAbstract();
        boolean queryable = nodeType.isQueryable();
        boolean orderableChildNodes = nodeType.hasOrderableChildNodes();

        NodeKey prototypeKey = repository.repositoryCache().getSystemKey();
        return new JcrNodeType(prototypeKey, this.context, null, this, name, supertypes, primaryItemName, childNodes, properties,
                               mixin, isAbstract, queryable, orderableChildNodes);
    }

    private JcrPropertyDefinition propertyDefinitionFrom( PropertyDefinition propDefn ) throws RepositoryException {
        Name propertyName = nameFactory.create(propDefn.getName());
        int onParentVersionBehavior = propDefn.getOnParentVersion();
        int requiredType = propDefn.getRequiredType();
        boolean mandatory = propDefn.isMandatory();
        boolean multiple = propDefn.isMultiple();
        boolean autoCreated = propDefn.isAutoCreated();
        boolean isProtected = propDefn.isProtected();
        boolean fullTextSearchable = propDefn.isFullTextSearchable();
        boolean queryOrderable = propDefn.isQueryOrderable();

        Value[] defaultValues = propDefn.getDefaultValues();
        JcrValue[] jcrDefaultValues = null;
        if (defaultValues != null) {
            jcrDefaultValues = new JcrValue[defaultValues.length];
            for (int i = 0; i != defaultValues.length; ++i) {
                Value value = defaultValues[i];
                jcrDefaultValues[i] = new JcrValue(this.context.getValueFactories(), value);
            }
        }

        String[] valueConstraints = propDefn.getValueConstraints();
        String[] queryOperators = propDefn.getAvailableQueryOperators();
        if (valueConstraints == null) valueConstraints = new String[0];
        NodeKey prototypeKey = repository.repositoryCache().getSystemKey();
        return new JcrPropertyDefinition(this.context, null, prototypeKey, propertyName, onParentVersionBehavior, autoCreated,
                                         mandatory, isProtected, jcrDefaultValues, requiredType, valueConstraints, multiple,
                                         fullTextSearchable, queryOrderable, queryOperators);
    }

    private JcrNodeDefinition childNodeDefinitionFrom( NodeDefinition childNodeDefn ) {
        Name childNodeName = nameFactory.create(childNodeDefn.getName());
        Name defaultPrimaryTypeName = nameFactory.create(childNodeDefn.getDefaultPrimaryTypeName());
        int onParentVersion = childNodeDefn.getOnParentVersion();

        boolean mandatory = childNodeDefn.isMandatory();
        boolean allowsSns = childNodeDefn.allowsSameNameSiblings();
        boolean autoCreated = childNodeDefn.isAutoCreated();
        boolean isProtected = childNodeDefn.isProtected();

        Name[] requiredTypes;
        String[] requiredTypeNames = childNodeDefn.getRequiredPrimaryTypeNames();
        if (requiredTypeNames != null) {
            List<Name> names = new ArrayList<Name>(requiredTypeNames.length);
            for (String typeName : requiredTypeNames) {
                names.add(nameFactory.create(typeName));
            }
            requiredTypes = names.toArray(new Name[names.size()]);
        } else {
            requiredTypes = new Name[0];
        }

        NodeKey prototypeKey = repository.repositoryCache().getSystemKey();
        return new JcrNodeDefinition(this.context, null, prototypeKey, childNodeName, onParentVersion, autoCreated, mandatory,
                                     isProtected, allowsSns, defaultPrimaryTypeName, requiredTypes);
    }

    @Override
    public void notify( ChangeSet changeSet ) {
        if (!systemWorkspaceName.equals(changeSet.getWorkspaceName())) {
            // The change does not affect the 'system' workspace, so skip it ...
            return;
        }
        if (context.getProcessId().equals(changeSet.getProcessKey())) {
            // We generated these changes, so skip them ...
            return;
        }

        // Now process the changes ...
        Set<Name> nodeTypesToRefresh = new HashSet<Name>();
        Set<Name> nodeTypesToDelete = new HashSet<Name>();
        for (Change change : changeSet) {
            if (change instanceof NodeAdded) {
                NodeAdded added = (NodeAdded)change;
                Path addedPath = added.getPath();
                if (nodeTypesPath.isAncestorOf(addedPath)) {
                    // Get the name of the node type ...
                    Name nodeTypeName = addedPath.getSegment(2).getName();
                    nodeTypesToRefresh.add(nodeTypeName);
                }
            } else if (change instanceof NodeRemoved) {
                NodeRemoved removed = (NodeRemoved)change;
                Path removedPath = removed.getPath();
                if (nodeTypesPath.isAncestorOf(removedPath)) {
                    // Get the name of the node type ...
                    Name nodeTypeName = removedPath.getSegment(2).getName();
                    if (removedPath.size() == 3) {
                        nodeTypesToDelete.add(nodeTypeName);
                    } else {
                        // It's a child defn or property defn ...
                        if (!nodeTypesToDelete.contains(nodeTypeName)) {
                            // The child defn or property defn is being removed but the node type is not ...
                            nodeTypesToRefresh.add(nodeTypeName);
                        }
                    }
                }
            } else if (change instanceof PropertyChanged) {
                PropertyChanged propChanged = (PropertyChanged)change;
                Path changedPath = propChanged.getPathToNode();
                if (nodeTypesPath.isAncestorOf(changedPath)) {
                    // Get the name of the node type ...
                    Name nodeTypeName = changedPath.getSegment(2).getName();
                    nodeTypesToRefresh.add(nodeTypeName);
                }
            } // we don't care about node moves (don't happen) or property added/removed (handled by node add/remove)
        }

        if (nodeTypesToRefresh.isEmpty() && nodeTypesToDelete.isEmpty()) {
            // No changes
            return;
        }

        // There were at least some changes ...
        this.nodeTypesLock.writeLock().lock();
        try {
            // Re-register the node types that were changed or added ...
            SessionCache systemCache = repository.createSystemSession(context, false);
            SystemContent system = new SystemContent(systemCache);
            Collection<NodeTypeDefinition> nodeTypes = system.readNodeTypes(nodeTypesToRefresh);
            registerNodeTypes(nodeTypes, false, false, false);

            // Unregister those that were removed ...
            unregisterNodeType(nodeTypesToDelete, false);
        } catch (Throwable e) {
            logger.error(e, JcrI18n.errorRefreshingNodeTypes, repository.name());
        } finally {
            this.nodeTypesLock.writeLock().unlock();
        }
    }

    /**
     * Refresh the node types from the stored representation.
     *
     * @return true if there was at least one node type found, or false if there were none
     */
    protected boolean refreshFromSystem() {
        this.nodeTypesLock.writeLock().lock();
        try {
            // Re-read and re-register all of the node types ...
            SessionCache systemCache = repository.createSystemSession(context, true);
            SystemContent system = new SystemContent(systemCache);
            Collection<NodeTypeDefinition> nodeTypes = system.readAllNodeTypes();
            if (nodeTypes.isEmpty()) return false;
            registerNodeTypes(nodeTypes, false, false, false);
            return true;
        } catch (Throwable e) {
            logger.error(e, JcrI18n.errorRefreshingNodeTypes, repository.name());
            return false;
        } finally {
            this.nodeTypesLock.writeLock().unlock();
        }
    }

    @Override
    public String toString() {
        return getNodeTypes().toString();
    }
}
