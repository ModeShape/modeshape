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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.InvalidNodeTypeDefinitionException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.query.InvalidQueryException;
import org.modeshape.common.annotation.GuardedBy;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.collection.HashMultimap;
import org.modeshape.common.collection.Multimap;
import org.modeshape.common.i18n.I18n;
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
class RepositoryNodeTypeManager implements ChangeSetListener {

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

    /**
     * List of ways to filter the returned property definitions
     * 
     * @see NodeTypes#findPropertyDefinitions(List, Name, PropertyCardinality, List)
     */
    private enum PropertyCardinality {
        SINGLE_VALUED_ONLY,
        MULTI_VALUED_ONLY,
        ANY
    }

    /**
     * List of ways to filter the returned node definitions
     * 
     * @see NodeTypes#findChildNodeDefinitions(List, Name, NodeCardinality, List)
     */
    private enum NodeCardinality {
        NO_SAME_NAME_SIBLINGS,
        SAME_NAME_SIBLINGS,
        ANY
    }

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
        return result;
    }

    protected final ValueFactory<String> strings() {
        return this.context.getValueFactories().getStringFactory();
    }

    /**
     * Get the immutable cache of node types.
     * 
     * @return the immutable node types cache; never null
     */
    public NodeTypes getNodeTypes() {
        return nodeTypesCache;
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
            logger.debug("{0} milliseconds to check if any of these node types are unused before unregistering them: {1}",
                         time,
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
                            throw new InvalidNodeTypeDefinitionException(
                                                                         JcrI18n.cannotUnregisterSupertype.text(name,
                                                                                                                supertype.getName()));
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
        CancellableQuery query = queryManager.query(context, repoCache, workspaceNames, overridden, command, schemata, null, null);
        try {
            QueryResults result = query.getResults();
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
                    nodeTypes.validate(typePendingRegistration,
                                       Arrays.asList(typePendingRegistration.getDeclaredSupertypes()),
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

    @Immutable
    public static final class NodeTypes {
        private final Map<Name, JcrNodeType> nodeTypes = new HashMap<Name, JcrNodeType>();
        private final Map<PropertyDefinitionId, JcrPropertyDefinition> propertyDefinitions = new HashMap<PropertyDefinitionId, JcrPropertyDefinition>();
        private final Map<NodeDefinitionId, JcrNodeDefinition> childNodeDefinitions = new HashMap<NodeDefinitionId, JcrNodeDefinition>();

        private final Collection<JcrNodeType> unmodifiableNodeTypes;
        private final Collection<JcrNodeType> unmodifiableMixinNodeTypes;
        private final Collection<JcrNodeType> unmodifiablePrimaryNodeTypes;
        private final Set<Name> unmodifiableNodeTypeNames;
        private final Set<Name> unmodifiableMixinTypeNames;

        private final int nodeTypesVersion;
        private final JcrNodeDefinition ntUnstructuredSnsChildDefinition;
        private final JcrNodeDefinition ntUnstructuredSingleChildDefinition;

        private final ExecutionContext context;
        private final NameFactory nameFactory;

        /**
         * The set of node type names that require no extra work during pre-save operations, as long as nodes that have this
         * primary type do not have any mixins. Note that this contains all node types not in any of the other sets.
         */
        private static final Set<Name> fullyDefinedNodeTypes = new HashSet<Name>();

        /**
         * The set of names for the node types that are 'mix:created'. See {@link #isCreated(Name, Set)}
         */
        private final Set<Name> createdNodeTypeNames = new HashSet<Name>();
        /**
         * The set of names for the node types that are 'mix:lastModified'. See {@link #isLastModified(Name, Set)}
         */
        private final Set<Name> lastModifiedNodeTypeNames = new HashSet<Name>();
        /**
         * The set of names for the node types that are 'mix:mimeType'. See {@link #isNtResource(Name)}
         */
        private final Set<Name> resourceNodeTypeNames = new HashSet<Name>();
        /**
         * The set of names for the node types that are 'mix:etag'. See {@link #isETag(Name, Set)}
         */
        private final Set<Name> etagNodeTypeNames = new HashSet<Name>();
        /**
         * The set of names for the node types that are 'mix:versionable'. See {@link #isVersionable(Name, Set)}
         */
        private final Set<Name> versionableNodeTypeNames = new HashSet<Name>();
        /**
         * The set of names for the node types that have child node definitions that allow same name siblings for every
         * combination. In other words, given any valid child, there will always be a satisfying child node definition that allows
         * same name siblings. For example, 'nt:unstructured' contains residual child node definitions that allow and disallow
         * same name siblings, so 'nt:unstructured' will always be in this set. If a node type is not in this set, then nodes of
         * this type must be checked for children to ensure the children satisfy the child node definitions.
         */
        private final Set<Name> nodeTypeNamesThatAllowSameNameSiblings = new HashSet<Name>();
        /**
         * The map of mandatory (and perhaps auto-created) property definitions for a node type keyed by the name of the node
         * type. See {@link #hasMandatoryPropertyDefinitions}
         */
        private final Multimap<Name, JcrPropertyDefinition> mandatoryPropertiesNodeTypes = HashMultimap.create();
        /**
         * The map of mandatory (and perhaps auto-created) child node definitions for a node type keyed by the name of the node
         * type. See {@link #hasMandatoryChildNodeDefinitions}
         */
        private final Multimap<Name, JcrNodeDefinition> mandatoryChildrenNodeTypes = HashMultimap.create();

        /**
         * The map of auto-created property definitions for a node type keyed by the name of the node type. See
         * {@link #hasMandatoryPropertyDefinitions}
         */
        private final Multimap<Name, JcrPropertyDefinition> autoCreatedPropertiesNodeTypes = HashMultimap.create();

        /**
         * The map of auto-created child node definitions for a node type keyed by the name of the node type. See
         * {@link #hasMandatoryChildNodeDefinitions}
         */
        private final Multimap<Name, JcrNodeDefinition> autoCreatedChildrenNodeTypes = HashMultimap.create();

        protected NodeTypes( ExecutionContext context ) {
            this(context, null, 0);
        }

        protected NodeTypes( ExecutionContext context,
                             Iterable<JcrNodeType> nodeTypes,
                             int version ) {
            this.nodeTypesVersion = version;
            this.context = context;
            this.nameFactory = context.getValueFactories().getNameFactory();

            Set<Name> mixinNames = new HashSet<Name>();
            List<JcrNodeType> mixins = new ArrayList<JcrNodeType>();
            List<JcrNodeType> primaries = new ArrayList<JcrNodeType>();
            if (nodeTypes != null) {
                JcrNodeType ntUnstructured = null;
                for (JcrNodeType nodeType : nodeTypes) {
                    Name name = nodeType.getInternalName();

                    // Store the node type in the quick-lookup maps ...
                    this.nodeTypes.put(name, nodeType);
                    for (JcrNodeDefinition childDefinition : nodeType.childNodeDefinitions()) {
                        this.childNodeDefinitions.put(childDefinition.getId(), childDefinition);
                    }
                    for (JcrPropertyDefinition propertyDefinition : nodeType.propertyDefinitions()) {
                        this.propertyDefinitions.put(propertyDefinition.getId(), propertyDefinition);
                    }
                    if (nodeType.isMixin()) {
                        mixins.add(nodeType);
                        mixinNames.add(name);
                    } else {
                        primaries.add(nodeType);
                    }

                    if (name.equals(JcrNtLexicon.UNSTRUCTURED)) {
                        ntUnstructured = nodeType;
                        nodeTypeNamesThatAllowSameNameSiblings.add(name);
                    }

                    boolean fullyDefined = true;
                    if (nodeType.isNodeType(JcrMixLexicon.CREATED)) {
                        createdNodeTypeNames.add(name);
                        fullyDefined = false;
                    }
                    if (nodeType.isNodeType(JcrMixLexicon.LAST_MODIFIED)) {
                        lastModifiedNodeTypeNames.add(name);
                        fullyDefined = false;
                    }
                    if (nodeType.isNodeType(JcrNtLexicon.RESOURCE)) {
                        resourceNodeTypeNames.add(name);
                        fullyDefined = false;
                    }
                    if (nodeType.isNodeType(JcrMixLexicon.ETAG)) {
                        etagNodeTypeNames.add(name);
                        fullyDefined = false;
                    }
                    if (nodeType.isNodeType(JcrMixLexicon.VERSIONABLE)) {
                        versionableNodeTypeNames.add(name);
                        fullyDefined = false;
                    }
                    for (JcrPropertyDefinition propDefn : nodeType.allPropertyDefinitions()) {
                        if (propDefn.isMandatory() && !propDefn.isProtected()) {
                            mandatoryPropertiesNodeTypes.put(name, propDefn);
                            fullyDefined = false;
                        }
                        if (propDefn.isAutoCreated() && !propDefn.isProtected()) {
                            autoCreatedPropertiesNodeTypes.put(name, propDefn);
                            // This isn't used in the pre-save operations, since auto-created items should be set on node creation
                            // fullDefined = false;
                        }
                    }
                    Collection<JcrNodeDefinition> allChildNodeDefinitions = nodeType.allChildNodeDefinitions();
                    boolean allowsResidualWithSameNameSiblings = false;
                    boolean allowsOnlySameNameSiblings = true;
                    boolean mixinWithNoChildNodeDefinitions = nodeType.isMixin() && allChildNodeDefinitions.isEmpty();
                    for (JcrNodeDefinition childDefn : allChildNodeDefinitions) {
                        if (childDefn.isMandatory() && !childDefn.isProtected()) {
                            mandatoryChildrenNodeTypes.put(name, childDefn);
                            fullyDefined = false;
                        }
                        if (childDefn.isAutoCreated() && !childDefn.isProtected()) {
                            autoCreatedChildrenNodeTypes.put(name, childDefn);
                            // This isn't used in the pre-save operations, since auto-created items should be set on node creation
                            // fullDefined = false;
                        }
                        if (childDefn.allowsSameNameSiblings()) {
                            if (childDefn.isResidual() && !childDefn.hasRequiredPrimaryTypes()) {
                                allowsResidualWithSameNameSiblings = true;
                            }
                        } else {
                            // same name siblings are not allowed ...
                            allowsOnlySameNameSiblings = false;
                        }
                    }
                    if (!nodeType.isAbstract()
                        && (allowsResidualWithSameNameSiblings || allowsOnlySameNameSiblings || mixinWithNoChildNodeDefinitions)) {
                        nodeTypeNamesThatAllowSameNameSiblings.add(name);
                    }

                    if (fullyDefined) {
                        fullyDefinedNodeTypes.add(name);
                    }
                }

                assert ntUnstructured != null;

                // Find and cache the 'nt:unstructured' residual child node definition that allows multiple SNS ...
                Collection<JcrNodeDefinition> childDefns = ntUnstructured.allChildNodeDefinitions(JcrNodeType.RESIDUAL_NAME, true);
                assert childDefns.size() == 1;
                ntUnstructuredSnsChildDefinition = childDefns.iterator().next();
                assert ntUnstructuredSnsChildDefinition != null;

                // Find and cache the 'nt:unstructured' residual child node definition that allows no SNS ...
                childDefns = ntUnstructured.allChildNodeDefinitions(JcrNodeType.RESIDUAL_NAME, false);
                assert childDefns.size() == 1;
                ntUnstructuredSingleChildDefinition = childDefns.iterator().next();
                assert ntUnstructuredSingleChildDefinition != null;
            } else {
                this.ntUnstructuredSnsChildDefinition = null;
                this.ntUnstructuredSingleChildDefinition = null;
            }

            this.unmodifiableNodeTypes = Collections.unmodifiableCollection(this.nodeTypes.values());
            this.unmodifiableNodeTypeNames = Collections.unmodifiableSet(this.nodeTypes.keySet());
            this.unmodifiableMixinTypeNames = Collections.unmodifiableSet(mixinNames);

            this.unmodifiableMixinNodeTypes = Collections.unmodifiableList(mixins);
            this.unmodifiablePrimaryNodeTypes = Collections.unmodifiableList(primaries);

        }

        /**
         * Obtain a new version of this cache with the specified node types removed from the new cache.
         * 
         * @param removedNodeTypes the node types that are to be removed from the resulting cache; may not be null but may be
         *        empty
         * @return the resulting cache that contains all of the node types within this cache but without the supplied node types;
         *         never null
         */
        protected NodeTypes without( Collection<JcrNodeType> removedNodeTypes ) {
            if (removedNodeTypes.isEmpty()) return this;
            Collection<JcrNodeType> nodeTypes = new HashSet<JcrNodeType>(this.nodeTypes.values());
            nodeTypes.removeAll(removedNodeTypes);
            return new NodeTypes(this.context, nodeTypes, getVersion() + 1);
        }

        /**
         * Obtain a new version of this cache with the specified node types added to the new cache.
         * 
         * @param addedNodeTypes the node types that are to be added to the resulting cache; may not be null but may be empty
         * @return the resulting cache that contains all of the node types within this cache and the supplied node types; never
         *         null
         */
        protected NodeTypes with( Collection<JcrNodeType> addedNodeTypes ) {
            if (addedNodeTypes.isEmpty()) return this;
            Collection<JcrNodeType> nodeTypes = new HashSet<JcrNodeType>(this.nodeTypes.values());
            // if there are updated node types, remove them first (hashcode is based on name alone),
            // else addAll() will ignore the changes.
            nodeTypes.removeAll(addedNodeTypes);
            nodeTypes.addAll(addedNodeTypes);
            return new NodeTypes(this.context, nodeTypes, getVersion() + 1);
        }

        /**
         * @return nameFactory
         */
        protected final NameFactory nameFactory() {
            return nameFactory;
        }

        /**
         * Get the version number of this cache. This essentially acts as an ETag, allowing other components to cache node type
         * information as long as the version number stays the same.
         * 
         * @return the version number of this cache
         */
        public int getVersion() {
            return nodeTypesVersion;
        }

        /**
         * Determine if the named node type does not appear in any of the other sets. Such node types are fully-defined, in that
         * nodes using them require no additional processing prior to save.
         * <p>
         * Note that this method's signature is different from the other methods. This is because a node's primary type and mixin
         * types must all be fully-defined types.
         * </p>
         * 
         * @param primaryTypeName the name of the primary node type; may not be null
         * @param mixinTypeNames the set of mixin type names; never null but possibly empty
         * @return true if the named node type is fully-defined, or false otherwise
         */
        public boolean isFullyDefinedType( Name primaryTypeName,
                                           Set<Name> mixinTypeNames ) {
            if (!fullyDefinedNodeTypes.contains(primaryTypeName)) return false;
            if (!mixinTypeNames.isEmpty()) {
                for (Name nodeTypeName : mixinTypeNames) {
                    if (!fullyDefinedNodeTypes.contains(nodeTypeName)) return false;
                }
            }
            return true;
        }

        /**
         * Determine if at least one of the named primary node type or mixin types is or subtypes the 'mix:created' mixin type.
         * 
         * @param primaryType the primary type name; may not be null
         * @param mixinTypes the mixin type names; may not be null but may be empty
         * @return true if any of the named node types is a created type, or false if there are none
         */
        public boolean isCreated( Name primaryType,
                                  Set<Name> mixinTypes ) {
            if (createdNodeTypeNames.contains(primaryType)) return true;
            for (Name mixinType : mixinTypes) {
                if (createdNodeTypeNames.contains(mixinType)) return true;
            }
            return false;
        }

        /**
         * Determine if at least one of the named primary node type or mixin types is or subtypes the 'mix:lastModified' mixin
         * type.
         * 
         * @param primaryType the primary type name; may not be null
         * @param mixinTypes the mixin type names; may not be null but may be empty
         * @return true if any of the named node types is a last-modified type, or false if there are none
         */
        public boolean isLastModified( Name primaryType,
                                       Set<Name> mixinTypes ) {
            if (lastModifiedNodeTypeNames.contains(primaryType)) return true;
            for (Name mixinType : mixinTypes) {
                if (lastModifiedNodeTypeNames.contains(mixinType)) return true;
            }
            return false;
        }

        /**
         * Determine if the named primary node type is or subtypes the 'nt:resource' node type.
         * 
         * @param primaryType the primary type name; may not be null
         * @return true if the primary node type is an 'nt:resource' node type (or subtype), or false otherwise
         */
        public boolean isNtResource( Name primaryType ) {
            // 'nt:resource' is a node type (not a mixin), so it can't appear in the mixin types ...
            return resourceNodeTypeNames.contains(primaryType);
        }

        /**
         * Determine if at least one of the named primary node type or mixin types is or subtypes the 'mix:etag' mixin type.
         * 
         * @param primaryType the primary type name; may not be null
         * @param mixinTypes the mixin type names; may not be null but may be empty
         * @return true if any of the named node types has an ETag, or false if there are none
         */
        public boolean isETag( Name primaryType,
                               Set<Name> mixinTypes ) {
            if (etagNodeTypeNames.contains(primaryType)) return true;
            for (Name mixinType : mixinTypes) {
                if (etagNodeTypeNames.contains(mixinType)) return true;
            }
            return false;
        }

        public boolean disallowsSameNameSiblings( Name primaryType,
                                                  Set<Name> mixinTypes ) {
            if (!nodeTypeNamesThatAllowSameNameSiblings.contains(primaryType)) return true;
            for (Name mixinType : mixinTypes) {
                if (!nodeTypeNamesThatAllowSameNameSiblings.contains(mixinType)) return true;
            }
            return false;
        }

        /**
         * Determine if at least one of the named primary node type or mixin types is or subtypes the 'mix:versionable' mixin
         * type.
         * 
         * @param primaryType the primary type name; may be null
         * @param mixinTypes the mixin type names; may not be null but may be empty
         * @return true if any of the named node types is versionable, or false if there are none
         */
        public boolean isVersionable( Name primaryType,
                                      Set<Name> mixinTypes ) {
            if (primaryType != null && versionableNodeTypeNames.contains(primaryType)) return true;
            for (Name mixinType : mixinTypes) {
                if (versionableNodeTypeNames.contains(mixinType)) return true;
            }
            return false;
        }

        /**
         * Determine if the named primary node type or mixin types has at least one mandatory property definitions declared on it
         * or any of its supertypes.
         * 
         * @param primaryType the primary type name; may not be null
         * @param mixinTypes the mixin type names; may not be null but may be empty
         * @return true if any of the named node types has one or more mandatory property definitions, or false if there are none
         */
        public boolean hasMandatoryPropertyDefinitions( Name primaryType,
                                                        Set<Name> mixinTypes ) {
            if (mandatoryPropertiesNodeTypes.containsKey(primaryType)) return true;
            for (Name mixinType : mixinTypes) {
                if (mandatoryPropertiesNodeTypes.containsKey(mixinType)) return true;
            }
            return false;
        }

        /**
         * Determine if the named primary node type or mixin types has at least one mandatory child node definitions declared on
         * it or any of its supertypes.
         * 
         * @param primaryType the primary type name; may not be null
         * @param mixinTypes the mixin type names; may not be null but may be empty
         * @return true if any of the the named node types has one or more mandatory child node definitions, or false if there are
         *         none
         */
        public boolean hasMandatoryChildNodeDefinitions( Name primaryType,
                                                         Set<Name> mixinTypes ) {
            if (mandatoryChildrenNodeTypes.containsKey(primaryType)) return true;
            for (Name mixinType : mixinTypes) {
                if (mandatoryChildrenNodeTypes.containsKey(mixinType)) return true;
            }
            return false;
        }

        /**
         * Get the mandatory property definitions for a node with the named primary type and mixin types. Note that the
         * {@link #hasMandatoryPropertyDefinitions(Name, Set)} method should first be called with the primary type and mixin
         * types; if that method returns <code>true</code>, then this method will never return an empty collection.
         * 
         * @param primaryType the primary type name; may not be null
         * @param mixinTypes the mixin type names; may not be null but may be empty
         * @return the collection of mandatory property definitions; never null but possibly empty
         */
        public Collection<JcrPropertyDefinition> getMandatoryPropertyDefinitions( Name primaryType,
                                                                                  Set<Name> mixinTypes ) {
            if (mixinTypes.isEmpty()) {
                return mandatoryPropertiesNodeTypes.get(primaryType);
            }
            Set<JcrPropertyDefinition> defn = new HashSet<JcrPropertyDefinition>();
            defn.addAll(mandatoryPropertiesNodeTypes.get(primaryType));
            for (Name mixinType : mixinTypes) {
                defn.addAll(mandatoryPropertiesNodeTypes.get(mixinType));
            }
            return defn;
        }

        /**
         * Get the mandatory child node definitions for a node with the named primary type and mixin types. Note that the
         * {@link #hasMandatoryChildNodeDefinitions(Name, Set)} method should first be called with the primary type and mixin
         * types; if that method returns <code>true</code>, then this method will never return an empty collection.
         * 
         * @param primaryType the primary type name; may not be null
         * @param mixinTypes the mixin type names; may not be null but may be empty
         * @return the collection of mandatory child node definitions; never null but possibly empty
         */
        public Collection<JcrNodeDefinition> getMandatoryChildNodeDefinitions( Name primaryType,
                                                                               Set<Name> mixinTypes ) {
            if (mixinTypes.isEmpty()) {
                return mandatoryChildrenNodeTypes.get(primaryType);
            }
            Set<JcrNodeDefinition> defn = new HashSet<JcrNodeDefinition>();
            defn.addAll(mandatoryChildrenNodeTypes.get(primaryType));
            for (Name mixinType : mixinTypes) {
                defn.addAll(mandatoryChildrenNodeTypes.get(mixinType));
            }
            return defn;
        }

        /**
         * Get the auto-created property definitions for the named node type. This method is used when
         * {@link AbstractJcrNode#addChildNode(Name, Name, NodeKey, boolean, boolean) creating nodes}, which only needs the
         * auto-created properties for the primary type. It's also used when {@link AbstractJcrNode#addMixin(String) adding a
         * mixin}.
         * 
         * @param nodeType the node type name; may not be null
         * @return the collection of auto-created property definitions; never null but possibly empty
         */
        public Collection<JcrPropertyDefinition> getAutoCreatedPropertyDefinitions( Name nodeType ) {
            return autoCreatedPropertiesNodeTypes.get(nodeType);
        }

        /**
         * Get the auto-created child node definitions for the named node type. This method is used when
         * {@link AbstractJcrNode#addChildNode(Name, Name, NodeKey, boolean, boolean) creating nodes}, which only needs the
         * auto-created properties for the primary type. It's also used when {@link AbstractJcrNode#addMixin(String) adding a
         * mixin}.
         * 
         * @param nodeType the node type name; may not be null
         * @return the collection of auto-created child node definitions; never null but possibly empty
         */
        public Collection<JcrNodeDefinition> getAutoCreatedChildNodeDefinitions( Name nodeType ) {
            return autoCreatedChildrenNodeTypes.get(nodeType);
        }

        /**
         * Return the immutable list of node types that are currently registered in this node type manager.
         * 
         * @return the immutable collection of (immutable) node types; never null
         */
        public Collection<JcrNodeType> getAllNodeTypes() {
            return this.unmodifiableNodeTypes;
        }

        /**
         * Return an immutable snapshot of the names of the node types currently registered in this node type manager.
         * 
         * @return the immutable collection of (immutable) node type names; never null
         */
        public Set<Name> getAllNodeTypeNames() {
            return this.unmodifiableNodeTypeNames;
        }

        /**
         * Return an immutable snapshot of the mixin node types that are currently registered in this node type manager.
         * 
         * @return the immutable collection of (immutable) mixin node types; never null
         * @see #getPrimaryNodeTypes()
         */
        public Collection<JcrNodeType> getMixinNodeTypes() {
            return this.unmodifiableMixinNodeTypes;
        }

        /**
         * Determine whether the node type given by the supplied name is a mixin node type.
         * 
         * @param nodeTypeName the name of the node type
         * @return true if there is an existing mixin node type with the supplied name, or false otherwise
         */
        public boolean isMixin( Name nodeTypeName ) {
            return unmodifiableMixinTypeNames.contains(nodeTypeName);
        }

        /**
         * Return an immutable snapshot of the primary node types that are currently registered in this node type manager.
         * 
         * @return the immutable collection of (immutable) primary node types; never null
         * @see #getMixinNodeTypes()
         */
        public Collection<JcrNodeType> getPrimaryNodeTypes() {
            return this.unmodifiablePrimaryNodeTypes;
        }

        public JcrPropertyDefinition getPropertyDefinition( PropertyDefinitionId id ) {
            return propertyDefinitions.get(id);
        }

        public Collection<JcrPropertyDefinition> getAllPropertyDefinitions() {
            return propertyDefinitions.values();
        }

        public JcrNodeDefinition getChildNodeDefinition( NodeDefinitionId id ) {
            return childNodeDefinitions.get(id);
        }

        JcrNodeType getNodeType( Name nodeTypeName ) {
            return nodeTypes.get(nodeTypeName);
        }

        /**
         * Tests if the named node type is registered.
         * <p>
         * The return value of this method is equivalent to {@code getNodeType(nodeTypeName) != null}, although the implementation
         * is marginally more efficient that this approach.
         * </p>
         * 
         * @param nodeTypeName the name of the node type to check
         * @return true if a node type with the given name is registered, false otherwise
         */
        boolean hasNodeType( Name nodeTypeName ) {
            return nodeTypes.containsKey(nodeTypeName);
        }

        /**
         * Searches the supplied primary node type and the mixin node types for a property definition that is the best match for
         * the given property name, property type, and value.
         * <p>
         * This method first attempts to find a single-valued property definition with the supplied property name and
         * {@link Value#getType() value's property type} in the primary type, skipping any property definitions that are
         * protected. The property definition is returned if it has a matching type (or has an {@link PropertyType#UNDEFINED
         * undefined property type}) and the value satisfies the {@link PropertyDefinition#getValueConstraints() definition's
         * constraints}. Otherwise, the process continues with each of the mixin types, in the order they are named.
         * </p>
         * <p>
         * If no matching property definition could be found (and <code>checkMultiValuedDefinitions</code> parameter is
         * <code>true</code>), the process is repeated except with multi-valued property definitions with the same name, property
         * type, and compatible constraints, starting with the primary type and continuing with each mixin type.
         * </p>
         * <p>
         * If no matching property definition could be found, and the process repeats by searching the primary type (and then
         * mixin types) for single-valued property definitions with a compatible type, where the values can be safely cast to the
         * definition's property type and still satisfy the definition's constraints.
         * </p>
         * <p>
         * If no matching property definition could be found, the previous step is repeated with multi-valued property
         * definitions.
         * </p>
         * <p>
         * If no matching property definition could be found (and the supplied property name is not the residual name), the whole
         * process repeats for residual property definitions (e.g., those that are defined with a
         * {@link JcrNodeType#RESIDUAL_NAME "*" name}).
         * </p>
         * <p>
         * Finally, if no satisfactory property definition could be found, this method returns null.
         * </p>
         * 
         * @param session the session in which the constraints are to be checked; may not be null
         * @param primaryTypeName the name of the primary type; may not be null
         * @param mixinTypeNames the names of the mixin types; may be null or empty if there are no mixins to include in the
         *        search
         * @param propertyName the name of the property for which the definition should be retrieved. This method will
         *        automatically look for residual definitions, but you can use {@link JcrNodeType#RESIDUAL_ITEM_NAME} to retrieve
         *        only the best residual property definition (if any).
         * @param value the value, or null if the property is being removed
         * @param checkMultiValuedDefinitions true if the type's multi-valued property definitions should be considered, or false
         *        if only single-value property definitions should be considered
         * @param skipProtected true if this operation is being done from within the public JCR node and property API, or false if
         *        this operation is being done from within internal implementations
         * @return the best property definition, or <code>null</code> if no property definition allows the property with the
         *         supplied name, type and number of values
         */
        JcrPropertyDefinition findPropertyDefinition( JcrSession session,
                                                      Name primaryTypeName,
                                                      Collection<Name> mixinTypeNames,
                                                      Name propertyName,
                                                      Value value,
                                                      boolean checkMultiValuedDefinitions,
                                                      boolean skipProtected ) {
            return findPropertyDefinition(session,
                                          primaryTypeName,
                                          mixinTypeNames,
                                          propertyName,
                                          value,
                                          checkMultiValuedDefinitions,
                                          skipProtected,
                                          true);
        }

        /**
         * Searches the supplied primary node type and the mixin node types for a property definition that is the best match for
         * the given property name, property type, and value.
         * <p>
         * This method first attempts to find a single-valued property definition with the supplied property name and
         * {@link Value#getType() value's property type} in the primary type, skipping any property definitions that are
         * protected. The property definition is returned if it has a matching type (or has an {@link PropertyType#UNDEFINED
         * undefined property type}) and the value satisfies the {@link PropertyDefinition#getValueConstraints() definition's
         * constraints}. Otherwise, the process continues with each of the mixin types, in the order they are named.
         * </p>
         * <p>
         * If no matching property definition could be found (and <code>checkMultiValuedDefinitions</code> parameter is
         * <code>true</code>), the process is repeated except with multi-valued property definitions with the same name, property
         * type, and compatible constraints, starting with the primary type and continuing with each mixin type.
         * </p>
         * <p>
         * If no matching property definition could be found, and the process repeats by searching the primary type (and then
         * mixin types) for single-valued property definitions with a compatible type, where the values can be safely cast to the
         * definition's property type and still satisfy the definition's constraints.
         * </p>
         * <p>
         * If no matching property definition could be found, the previous step is repeated with multi-valued property
         * definitions.
         * </p>
         * <p>
         * If no matching property definition could be found (and the supplied property name is not the residual name), the whole
         * process repeats for residual property definitions (e.g., those that are defined with a
         * {@link JcrNodeType#RESIDUAL_NAME "*" name}).
         * </p>
         * <p>
         * Finally, if no satisfactory property definition could be found, this method returns null.
         * </p>
         * 
         * @param session the session in which the constraints are to be checked; may not be null
         * @param primaryTypeName the name of the primary type; may not be null
         * @param mixinTypeNames the names of the mixin types; may be null or empty if there are no mixins to include in the
         *        search
         * @param propertyName the name of the property for which the definition should be retrieved. This method will
         *        automatically look for residual definitions, but you can use {@link JcrNodeType#RESIDUAL_ITEM_NAME} to retrieve
         *        only the best residual property definition (if any).
         * @param value the value, or null if the property is being removed
         * @param checkMultiValuedDefinitions true if the type's multi-valued property definitions should be considered, or false
         *        if only single-value property definitions should be considered
         * @param skipProtected true if this operation is being done from within the public JCR node and property API, or false if
         *        this operation is being done from within internal implementations
         * @param checkTypeAndConstraints true if the type and constraints of the property definition should be checked, or false
         *        otherwise
         * @return the best property definition, or <code>null</code> if no property definition allows the property with the
         *         supplied name, type and number of values
         */
        JcrPropertyDefinition findPropertyDefinition( JcrSession session,
                                                      Name primaryTypeName,
                                                      Collection<Name> mixinTypeNames,
                                                      Name propertyName,
                                                      Value value,
                                                      boolean checkMultiValuedDefinitions,
                                                      boolean skipProtected,
                                                      boolean checkTypeAndConstraints ) {
            boolean setToEmpty = value == null;

            /*
             * We use this flag to indicate that there was a definition encountered with the same name.  If
             * a named definition (or definitions - for example the same node type could define a LONG and BOOLEAN
             * version of the same property) is encountered and no match is found for the name, then processing should not
             * proceed.  If processing did proceed, a residual definition might be found and matched.  This would 
             * lead to a situation where a node defined a type for a named property, but contained a property with 
             * the same name and the wrong type. 
             */
            boolean matchedOnName = false;

            // Look for a single-value property definition on the primary type that matches by name and type ...
            JcrNodeType primaryType = getNodeType(primaryTypeName);
            if (primaryType != null) {
                for (JcrPropertyDefinition definition : primaryType.allSingleValuePropertyDefinitions(propertyName)) {
                    matchedOnName = true;
                    // See if the definition allows the value ...
                    if (skipProtected && definition.isProtected()) return null;
                    if (setToEmpty) {
                        if (!definition.isMandatory()) return definition;
                        // Otherwise this definition doesn't work, so continue with the next ...
                        continue;
                    }
                    assert value != null;
                    // We can use the definition if it matches the type and satisfies the constraints ...
                    int type = definition.getRequiredType();
                    // Don't check constraints on reference properties
                    if (type == PropertyType.REFERENCE && type == value.getType()) return definition;
                    if (type == PropertyType.WEAKREFERENCE && type == value.getType()) return definition;
                    if (type == org.modeshape.jcr.api.PropertyType.SIMPLE_REFERENCE && type == value.getType()) return definition;
                    if (type == PropertyType.UNDEFINED || type == value.getType()) {
                        if (!checkTypeAndConstraints) return definition;
                        if (definition.satisfiesConstraints(value, session)) return definition;
                    }
                }

                if (matchedOnName) {
                    if (value != null) {
                        for (JcrPropertyDefinition definition : primaryType.allSingleValuePropertyDefinitions(propertyName)) {
                            // See if the definition allows the value ...
                            if (skipProtected && definition.isProtected()) return null;
                            // Don't check constraints on reference properties
                            int type = definition.getRequiredType();
                            if (type == PropertyType.REFERENCE && definition.canCastToType(value)) {
                                return definition;
                            }
                            if (type == PropertyType.WEAKREFERENCE && definition.canCastToType(value)) {
                                return definition;
                            }
                            if (type == org.modeshape.jcr.api.PropertyType.SIMPLE_REFERENCE && definition.canCastToType(value)) {
                                return definition;
                            }
                            if (!checkTypeAndConstraints) return definition;
                            if (definition.canCastToTypeAndSatisfyConstraints(value, session)) return definition;
                        }
                    }

                    if (checkMultiValuedDefinitions) {
                        // Look for a multi-value property definition on the primary type that matches by name and type ...
                        for (JcrPropertyDefinition definition : primaryType.allMultiValuePropertyDefinitions(propertyName)) {
                            // See if the definition allows the value ...
                            if (skipProtected && definition.isProtected()) return null;
                            if (setToEmpty) {
                                if (!definition.isMandatory()) return definition;
                                // Otherwise this definition doesn't work, so continue with the next ...
                                continue;
                            }
                            assert value != null;
                            // We can use the definition if it matches the type and satisfies the constraints ...
                            int type = definition.getRequiredType();
                            // Don't check constraints on reference properties
                            if (type == PropertyType.REFERENCE && type == value.getType()) return definition;
                            if (type == PropertyType.WEAKREFERENCE && type == value.getType()) return definition;
                            if (type == org.modeshape.jcr.api.PropertyType.SIMPLE_REFERENCE && type == value.getType()) return definition;
                            if (type == PropertyType.UNDEFINED || type == value.getType()) {
                                if (!checkTypeAndConstraints) return definition;
                                if (definition.satisfiesConstraints(value, session)) return definition;
                            }
                        }
                        if (value != null) {
                            for (JcrPropertyDefinition definition : primaryType.allMultiValuePropertyDefinitions(propertyName)) {
                                // See if the definition allows the value ...
                                if (skipProtected && definition.isProtected()) return null;
                                assert definition.getRequiredType() != PropertyType.UNDEFINED;
                                // Don't check constraints on reference properties
                                int type = definition.getRequiredType();
                                if (type == PropertyType.REFERENCE && definition.canCastToType(value)) {
                                    return definition;
                                }
                                if (type == PropertyType.WEAKREFERENCE && definition.canCastToType(value)) {
                                    return definition;
                                }
                                if (type == org.modeshape.jcr.api.PropertyType.SIMPLE_REFERENCE && definition.canCastToType(value)) {
                                    return definition;
                                }
                                if (!checkTypeAndConstraints) return definition;
                                if (definition.canCastToTypeAndSatisfyConstraints(value, session)) return definition;
                            }
                        }
                    }
                    return null;
                }
            }

            // Look for a single-value property definition on the mixin types that matches by name and type ...
            List<JcrNodeType> mixinTypes = null;
            if (mixinTypeNames != null) {
                mixinTypes = new LinkedList<JcrNodeType>();
                for (Name mixinTypeName : mixinTypeNames) {
                    JcrNodeType mixinType = getNodeType(mixinTypeName);
                    if (mixinType == null) continue;
                    mixinTypes.add(mixinType);
                    for (JcrPropertyDefinition definition : mixinType.allSingleValuePropertyDefinitions(propertyName)) {
                        matchedOnName = true;
                        // See if the definition allows the value ...
                        if (skipProtected && definition.isProtected()) return null;
                        if (setToEmpty) {
                            if (!definition.isMandatory()) return definition;
                            // Otherwise this definition doesn't work, so continue with the next ...
                            continue;
                        }
                        assert value != null;
                        // We can use the definition if it matches the type and satisfies the constraints ...
                        int type = definition.getRequiredType();
                        // Don't check constraints on reference properties
                        if (type == PropertyType.REFERENCE && type == value.getType()) return definition;
                        if (type == PropertyType.WEAKREFERENCE && type == value.getType()) return definition;
                        if (type == org.modeshape.jcr.api.PropertyType.SIMPLE_REFERENCE && type == value.getType()) return definition;
                        if (type == PropertyType.UNDEFINED || type == value.getType()) {
                            if (!checkTypeAndConstraints) return definition;
                            if (definition.satisfiesConstraints(value, session)) return definition;
                        }
                    }
                    if (matchedOnName) {
                        if (value != null) {
                            for (JcrPropertyDefinition definition : mixinType.allSingleValuePropertyDefinitions(propertyName)) {
                                // See if the definition allows the value ...
                                if (skipProtected && definition.isProtected()) return null;
                                assert definition.getRequiredType() != PropertyType.UNDEFINED;
                                // Don't check constraints on reference properties
                                int type = definition.getRequiredType();
                                if (type == PropertyType.REFERENCE && definition.canCastToType(value)) {
                                    return definition;
                                }
                                if (type == PropertyType.WEAKREFERENCE && definition.canCastToType(value)) {
                                    return definition;
                                }
                                if (type == org.modeshape.jcr.api.PropertyType.SIMPLE_REFERENCE && definition.canCastToType(value)) {
                                    return definition;
                                }
                                if (!checkTypeAndConstraints) return definition;
                                if (definition.canCastToTypeAndSatisfyConstraints(value, session)) return definition;
                            }
                        }

                        if (checkMultiValuedDefinitions) {
                            for (JcrPropertyDefinition definition : mixinType.allMultiValuePropertyDefinitions(propertyName)) {
                                // See if the definition allows the value ...
                                if (skipProtected && definition.isProtected()) return null;
                                if (setToEmpty) {
                                    if (!definition.isMandatory()) return definition;
                                    // Otherwise this definition doesn't work, so continue with the next ...
                                    continue;
                                }
                                assert value != null;
                                // We can use the definition if it matches the type and satisfies the constraints ...
                                int type = definition.getRequiredType();
                                // Don't check constraints on reference properties
                                if (type == PropertyType.REFERENCE && type == value.getType()) return definition;
                                if (type == PropertyType.WEAKREFERENCE && type == value.getType()) return definition;
                                if (type == org.modeshape.jcr.api.PropertyType.SIMPLE_REFERENCE && type == value.getType()) return definition;
                                if (type == PropertyType.UNDEFINED || type == value.getType()) {
                                    if (!checkTypeAndConstraints) return definition;
                                    if (definition.satisfiesConstraints(value, session)) return definition;
                                }
                            }
                            if (value != null) {
                                for (JcrPropertyDefinition definition : mixinType.allMultiValuePropertyDefinitions(propertyName)) {
                                    matchedOnName = true;
                                    // See if the definition allows the value ...
                                    if (skipProtected && definition.isProtected()) return null;
                                    assert definition.getRequiredType() != PropertyType.UNDEFINED;
                                    // Don't check constraints on reference properties
                                    int type = definition.getRequiredType();
                                    if (type == PropertyType.REFERENCE && definition.canCastToType(value)) {
                                        return definition;
                                    }
                                    if (type == PropertyType.WEAKREFERENCE && definition.canCastToType(value)) {
                                        return definition;
                                    }
                                    if (type == org.modeshape.jcr.api.PropertyType.SIMPLE_REFERENCE && definition.canCastToType(value)) {
                                        return definition;
                                    }
                                    if (!checkTypeAndConstraints) return definition;
                                    if (definition.canCastToTypeAndSatisfyConstraints(value, session)) return definition;
                                }
                            }
                        }

                        return null;
                    }
                }
            }

            if (checkMultiValuedDefinitions) {
                if (primaryType != null) {
                    // Look for a multi-value property definition on the primary type that matches by name and type ...
                    for (JcrPropertyDefinition definition : primaryType.allMultiValuePropertyDefinitions(propertyName)) {
                        matchedOnName = true;
                        // See if the definition allows the value ...
                        if (skipProtected && definition.isProtected()) return null;
                        if (setToEmpty) {
                            if (!definition.isMandatory()) return definition;
                            // Otherwise this definition doesn't work, so continue with the next ...
                            continue;
                        }
                        assert value != null;
                        // We can use the definition if it matches the type and satisfies the constraints ...
                        int type = definition.getRequiredType();
                        // Don't check constraints on reference properties
                        if (type == PropertyType.REFERENCE && type == value.getType()) return definition;
                        if (type == PropertyType.WEAKREFERENCE && type == value.getType()) return definition;
                        if (type == org.modeshape.jcr.api.PropertyType.SIMPLE_REFERENCE && type == value.getType()) return definition;
                        if (type == PropertyType.UNDEFINED || type == value.getType()) {
                            if (!checkTypeAndConstraints) return definition;
                            if (definition.satisfiesConstraints(value, session)) return definition;
                        }
                    }
                    if (value != null) {
                        for (JcrPropertyDefinition definition : primaryType.allMultiValuePropertyDefinitions(propertyName)) {
                            matchedOnName = true;
                            // See if the definition allows the value ...
                            if (skipProtected && definition.isProtected()) return null;
                            assert definition.getRequiredType() != PropertyType.UNDEFINED;
                            // Don't check constraints on reference properties
                            int type = definition.getRequiredType();
                            if (type == PropertyType.REFERENCE && definition.canCastToType(value)) {
                                return definition;
                            }
                            if (type == PropertyType.WEAKREFERENCE && definition.canCastToType(value)) {
                                return definition;
                            }
                            if (type == org.modeshape.jcr.api.PropertyType.SIMPLE_REFERENCE && definition.canCastToType(value)) {
                                return definition;
                            }
                            if (!checkTypeAndConstraints) return definition;
                            if (definition.canCastToTypeAndSatisfyConstraints(value, session)) return definition;
                        }
                    }
                }

                if (matchedOnName) return null;

                if (mixinTypeNames != null) {
                    mixinTypes = new LinkedList<JcrNodeType>();
                    for (Name mixinTypeName : mixinTypeNames) {
                        JcrNodeType mixinType = getNodeType(mixinTypeName);
                        if (mixinType == null) continue;
                        mixinTypes.add(mixinType);
                        for (JcrPropertyDefinition definition : mixinType.allMultiValuePropertyDefinitions(propertyName)) {
                            matchedOnName = true;
                            // See if the definition allows the value ...
                            if (skipProtected && definition.isProtected()) return null;
                            if (setToEmpty) {
                                if (!definition.isMandatory()) return definition;
                                // Otherwise this definition doesn't work, so continue with the next ...
                                continue;
                            }
                            assert value != null;
                            // We can use the definition if it matches the type and satisfies the constraints ...
                            int type = definition.getRequiredType();
                            // Don't check constraints on reference properties
                            if (type == PropertyType.REFERENCE && type == value.getType()) return definition;
                            if (type == PropertyType.WEAKREFERENCE && type == value.getType()) return definition;
                            if (type == org.modeshape.jcr.api.PropertyType.SIMPLE_REFERENCE && type == value.getType()) return definition;
                            if (type == PropertyType.UNDEFINED || type == value.getType()) {
                                if (!checkTypeAndConstraints) return definition;
                                if (definition.satisfiesConstraints(value, session)) return definition;
                            }
                        }
                        if (value != null) {
                            for (JcrPropertyDefinition definition : mixinType.allMultiValuePropertyDefinitions(propertyName)) {
                                matchedOnName = true;
                                // See if the definition allows the value ...
                                if (skipProtected && definition.isProtected()) return null;
                                assert definition.getRequiredType() != PropertyType.UNDEFINED;
                                // Don't check constraints on reference properties
                                int type = definition.getRequiredType();
                                if (type == PropertyType.REFERENCE && definition.canCastToType(value)) {
                                    return definition;
                                }
                                if (type == PropertyType.WEAKREFERENCE && definition.canCastToType(value)) {
                                    return definition;
                                }
                                if (type == org.modeshape.jcr.api.PropertyType.SIMPLE_REFERENCE && definition.canCastToType(value)) {
                                    return definition;
                                }
                                if (!checkTypeAndConstraints) return definition;
                                if (definition.canCastToTypeAndSatisfyConstraints(value, session)) return definition;
                            }
                        }
                    }
                }
                if (matchedOnName) return null;

            }

            // Nothing was found, so look for residual property definitions ...
            if (!propertyName.equals(JcrNodeType.RESIDUAL_NAME)) return findPropertyDefinition(session,
                                                                                               primaryTypeName,
                                                                                               mixinTypeNames,
                                                                                               JcrNodeType.RESIDUAL_NAME,
                                                                                               value,
                                                                                               checkMultiValuedDefinitions,
                                                                                               skipProtected,
                                                                                               checkTypeAndConstraints);
            return null;
        }

        /**
         * Searches the supplied primary node type and the mixin node types for a property definition that is the best match for
         * the given property name, property type, and value.
         * <p>
         * This method first attempts to find a single-valued property definition with the supplied property name and
         * {@link Value#getType() value's property type} in the primary type, skipping any property definitions that are
         * protected. The property definition is returned if it has a matching type (or has an {@link PropertyType#UNDEFINED
         * undefined property type}) and the value satisfies the {@link PropertyDefinition#getValueConstraints() definition's
         * constraints}. Otherwise, the process continues with each of the mixin types, in the order they are named.
         * </p>
         * <p>
         * If no matching property definition could be found (and <code>checkMultiValuedDefinitions</code> parameter is
         * <code>true</code>), the process is repeated except with multi-valued property definitions with the same name, property
         * type, and compatible constraints, starting with the primary type and continuing with each mixin type.
         * </p>
         * <p>
         * If no matching property definition could be found, and the process repeats by searching the primary type (and then
         * mixin types) for single-valued property definitions with a compatible type, where the values can be safely cast to the
         * definition's property type and still satisfy the definition's constraints.
         * </p>
         * <p>
         * If no matching property definition could be found, the previous step is repeated with multi-valued property
         * definitions.
         * </p>
         * <p>
         * If no matching property definition could be found (and the supplied property name is not the residual name), the whole
         * process repeats for residual property definitions (e.g., those that are defined with a
         * {@link JcrNodeType#RESIDUAL_NAME "*" name}).
         * </p>
         * <p>
         * Finally, if no satisfactory property definition could be found, this method returns null.
         * </p>
         * 
         * @param session the session in which the constraints are to be checked; may not be null
         * @param primaryTypeName the name of the primary type; may not be null
         * @param mixinTypeNames the names of the mixin types; may be null or empty if there are no mixins to include in the
         *        search
         * @param propertyName the name of the property for which the definition should be retrieved. This method will
         *        automatically look for residual definitions, but you can use {@link JcrNodeType#RESIDUAL_ITEM_NAME} to retrieve
         *        only the best residual property definition (if any).
         * @param values the values
         * @param skipProtected true if this operation is being done from within the public JCR node and property API, or false if
         *        this operation is being done from within internal implementations
         * @return the best property definition, or <code>null</code> if no property definition allows the property with the
         *         supplied name, type and number of values
         */
        JcrPropertyDefinition findPropertyDefinition( JcrSession session,
                                                      Name primaryTypeName,
                                                      Collection<Name> mixinTypeNames,
                                                      Name propertyName,
                                                      Value[] values,
                                                      boolean skipProtected ) {
            return findPropertyDefinition(session, primaryTypeName, mixinTypeNames, propertyName, values, skipProtected, true);
        }

        /**
         * Searches the supplied primary node type and the mixin node types for a property definition that is the best match for
         * the given property name, property type, and value.
         * <p>
         * This method first attempts to find a single-valued property definition with the supplied property name and
         * {@link Value#getType() value's property type} in the primary type, skipping any property definitions that are
         * protected. The property definition is returned if it has a matching type (or has an {@link PropertyType#UNDEFINED
         * undefined property type}) and the value satisfies the {@link PropertyDefinition#getValueConstraints() definition's
         * constraints}. Otherwise, the process continues with each of the mixin types, in the order they are named.
         * </p>
         * <p>
         * If no matching property definition could be found (and <code>checkMultiValuedDefinitions</code> parameter is
         * <code>true</code>), the process is repeated except with multi-valued property definitions with the same name, property
         * type, and compatible constraints, starting with the primary type and continuing with each mixin type.
         * </p>
         * <p>
         * If no matching property definition could be found, and the process repeats by searching the primary type (and then
         * mixin types) for single-valued property definitions with a compatible type, where the values can be safely cast to the
         * definition's property type and still satisfy the definition's constraints.
         * </p>
         * <p>
         * If no matching property definition could be found, the previous step is repeated with multi-valued property
         * definitions.
         * </p>
         * <p>
         * If no matching property definition could be found (and the supplied property name is not the residual name), the whole
         * process repeats for residual property definitions (e.g., those that are defined with a
         * {@link JcrNodeType#RESIDUAL_NAME "*" name}).
         * </p>
         * <p>
         * Finally, if no satisfactory property definition could be found, this method returns null.
         * </p>
         * 
         * @param session the session in which the constraints are to be checked; may not be null
         * @param primaryTypeName the name of the primary type; may not be null
         * @param mixinTypeNames the names of the mixin types; may be null or empty if there are no mixins to include in the
         *        search
         * @param propertyName the name of the property for which the definition should be retrieved. This method will
         *        automatically look for residual definitions, but you can use {@link JcrNodeType#RESIDUAL_ITEM_NAME} to retrieve
         *        only the best residual property definition (if any).
         * @param values the values
         * @param skipProtected true if this operation is being done from within the public JCR node and property API, or false if
         *        this operation is being done from within internal implementations
         * @param checkTypeAndConstraints true if the type and constraints of the property definition should be checked, or false
         *        otherwise
         * @return the best property definition, or <code>null</code> if no property definition allows the property with the
         *         supplied name, type and number of values
         */
        JcrPropertyDefinition findPropertyDefinition( JcrSession session,
                                                      Name primaryTypeName,
                                                      Collection<Name> mixinTypeNames,
                                                      Name propertyName,
                                                      Value[] values,
                                                      boolean skipProtected,
                                                      boolean checkTypeAndConstraints ) {
            boolean setToEmpty = values == null;
            int propertyType = values == null || values.length == 0 ? PropertyType.STRING : values[0].getType();

            /*
             * We use this flag to indicate that there was a definition encountered with the same name.  If
             * a named definition (or definitions - for example the same node type could define a LONG and BOOLEAN
             * version of the same property) is encountered and no match is found for the name, then processing should not
             * proceed.  If processing did proceed, a residual definition might be found and matched.  This would 
             * lead to a situation where a node defined a type for a named property, but contained a property with 
             * the same name and the wrong type. 
             */
            boolean matchedOnName = false;

            // Look for a multi-value property definition on the primary type that matches by name and type ...
            JcrNodeType primaryType = getNodeType(primaryTypeName);
            if (primaryType != null) {
                for (JcrPropertyDefinition definition : primaryType.allMultiValuePropertyDefinitions(propertyName)) {
                    matchedOnName = true;
                    // See if the definition allows the value ...
                    if (skipProtected && definition.isProtected()) return null;
                    if (setToEmpty) {
                        if (!definition.isMandatory()) return definition;
                        // Otherwise this definition doesn't work, so continue with the next ...
                        continue;
                    }
                    assert values != null;
                    // We can use the definition if it matches the type and satisfies the constraints ...
                    int type = definition.getRequiredType();
                    boolean typeMatches = values.length == 0 || type == PropertyType.UNDEFINED || type == propertyType;
                    // Don't check constraints on reference properties
                    if (typeMatches) {
                        if (type == PropertyType.REFERENCE) return definition;
                        if (type == PropertyType.WEAKREFERENCE) return definition;
                        if (type == org.modeshape.jcr.api.PropertyType.SIMPLE_REFERENCE) return definition;
                        if (!checkTypeAndConstraints) return definition;
                        if (definition.satisfiesConstraints(values, session)) return definition;
                    }
                }

                if (matchedOnName) {
                    if (values != null && values.length != 0) {
                        // Nothing was found with matching name and type, so look for definitions with
                        // matching name and an undefined or castable type ...

                        // Look for a multi-value property definition on the primary type that matches by name and type ...
                        for (JcrPropertyDefinition definition : primaryType.allMultiValuePropertyDefinitions(propertyName)) {
                            // See if the definition allows the value ...
                            if (skipProtected && definition.isProtected()) return null;
                            assert definition.getRequiredType() != PropertyType.UNDEFINED;
                            // Don't check constraints on reference properties
                            if (definition.getRequiredType() == PropertyType.REFERENCE && definition.canCastToType(values)) return definition;
                            if (definition.getRequiredType() == PropertyType.WEAKREFERENCE && definition.canCastToType(values)) return definition;
                            if (definition.getRequiredType() == org.modeshape.jcr.api.PropertyType.SIMPLE_REFERENCE && definition.canCastToType(values)) return definition;
                            if (!checkTypeAndConstraints) return definition;
                            if (definition.canCastToTypeAndSatisfyConstraints(values, session)) return definition;
                        }
                    }

                    return null;
                }
            }

            // Look for a multi-value property definition on the mixin types that matches by name and type ...
            List<JcrNodeType> mixinTypes = null;
            if (mixinTypeNames != null) {
                mixinTypes = new LinkedList<JcrNodeType>();
                for (Name mixinTypeName : mixinTypeNames) {
                    JcrNodeType mixinType = getNodeType(mixinTypeName);
                    if (mixinType == null) continue;
                    mixinTypes.add(mixinType);
                    for (JcrPropertyDefinition definition : mixinType.allMultiValuePropertyDefinitions(propertyName)) {
                        matchedOnName = true;
                        // See if the definition allows the value ...
                        if (skipProtected && definition.isProtected()) return null;
                        if (setToEmpty) {
                            if (!definition.isMandatory()) return definition;
                            // Otherwise this definition doesn't work, so continue with the next ...
                            continue;
                        }
                        assert values != null;
                        // We can use the definition if it matches the type and satisfies the constraints ...
                        int type = definition.getRequiredType();
                        boolean typeMatches = values.length == 0 || type == PropertyType.UNDEFINED || type == propertyType;
                        // Don't check constraints on reference properties
                        if (typeMatches) {
                            if (type == PropertyType.REFERENCE) return definition;
                            if (type == PropertyType.WEAKREFERENCE) return definition;
                            if (type == org.modeshape.jcr.api.PropertyType.SIMPLE_REFERENCE) return definition;
                            if (!checkTypeAndConstraints) return definition;
                            if (definition.satisfiesConstraints(values, session)) return definition;
                        }
                    }
                    if (matchedOnName) {
                        if (values != null && values.length != 0) {
                            // Nothing was found with matching name and type, so look for definitions with
                            // matching name and an undefined or castable type ...

                            // Look for a multi-value property definition on the mixin type that matches by name and type ...
                            for (JcrPropertyDefinition definition : mixinType.allMultiValuePropertyDefinitions(propertyName)) {
                                // See if the definition allows the value ...
                                if (skipProtected && definition.isProtected()) return null;
                                assert definition.getRequiredType() != PropertyType.UNDEFINED;
                                // Don't check constraints on reference properties
                                if (definition.getRequiredType() == PropertyType.REFERENCE && definition.canCastToType(values)) return definition;
                                if (definition.getRequiredType() == PropertyType.WEAKREFERENCE && definition.canCastToType(values)) return definition;
                                if (definition.getRequiredType() == org.modeshape.jcr.api.PropertyType.SIMPLE_REFERENCE && definition.canCastToType(values)) return definition;
                                if (!checkTypeAndConstraints) return definition;
                                if (definition.canCastToTypeAndSatisfyConstraints(values, session)) return definition;
                            }
                        }

                        return null;
                    }

                }
            }

            // Nothing was found, so look for residual property definitions ...
            if (!propertyName.equals(JcrNodeType.RESIDUAL_NAME)) return findPropertyDefinition(session,
                                                                                               primaryTypeName,
                                                                                               mixinTypeNames,
                                                                                               JcrNodeType.RESIDUAL_NAME,
                                                                                               values,
                                                                                               skipProtected,
                                                                                               checkTypeAndConstraints);
            return null;
        }

        /**
         * Searches the supplied primary and mixin node types for all valid property definitions that match the given property
         * name and cardinality.
         * <p>
         * If no satisfactory property definition could be found, this method returns an empty list.
         * </p>
         * 
         * @param typeNamesToCheck the name of the types to check; may not be null
         * @param propertyName the name of the property for which the definitions should be retrieved
         * @param typeToCheck the type of definitions to consider (single-valued only, multi-valued only, or all)
         * @param pendingTypes a list of types that have been created during type registration but not yet registered in the type
         *        map
         * @return a list of all valid property definitions that match the given property name and cardinality
         */
        private List<JcrPropertyDefinition> findPropertyDefinitions( List<Name> typeNamesToCheck,
                                                                     Name propertyName,
                                                                     PropertyCardinality typeToCheck,
                                                                     List<JcrNodeType> pendingTypes ) {
            assert typeNamesToCheck != null;

            Collection<JcrPropertyDefinition> propDefs = null;
            List<JcrPropertyDefinition> matchingDefs = new ArrayList<JcrPropertyDefinition>();

            // Look for a single-value property definition on the mixin types that matches by name and type ...
            for (Name typeNameToCheck : typeNamesToCheck) {
                JcrNodeType typeName = findTypeInMapOrList(typeNameToCheck, pendingTypes);
                if (typeName == null) continue;

                switch (typeToCheck) {
                    case SINGLE_VALUED_ONLY:
                        propDefs = typeName.allSingleValuePropertyDefinitions(propertyName);
                        break;
                    case MULTI_VALUED_ONLY:
                        propDefs = typeName.allMultiValuePropertyDefinitions(propertyName);
                        break;
                    case ANY:
                        propDefs = typeName.allPropertyDefinitions(propertyName);
                        break;
                    default:
                        throw new IllegalStateException("Should be unreachable: " + typeToCheck);
                }

                if (!propDefs.isEmpty()) matchingDefs.addAll(propDefs);
            }

            return matchingDefs;
        }

        /**
         * Determine if the property definitions of the supplied primary type and mixin types allow the property with the supplied
         * name to be removed.
         * 
         * @param primaryTypeNameOfParent the name of the primary type for the parent node; may not be null
         * @param mixinTypeNamesOfParent the names of the mixin types for the parent node; may be null or empty if there are no
         *        mixins to include in the search
         * @param propertyName the name of the property to be removed; may not be null
         * @param skipProtected true if this operation is being done from within the public JCR node and property API, or false if
         *        this operation is being done from within internal implementations
         * @return true if at least one child node definition does not require children with the supplied name to exist, or false
         *         otherwise
         */
        boolean canRemoveProperty( Name primaryTypeNameOfParent,
                                   List<Name> mixinTypeNamesOfParent,
                                   Name propertyName,
                                   boolean skipProtected ) {
            // First look in the primary type ...
            JcrNodeType primaryType = getNodeType(primaryTypeNameOfParent);
            if (primaryType != null) {
                for (JcrPropertyDefinition definition : primaryType.allPropertyDefinitions(propertyName)) {
                    // Skip protected definitions ...
                    if (skipProtected && definition.isProtected()) continue;
                    // If this definition is not mandatory, then we have found that we CAN remove the property ...
                    return !definition.isMandatory();
                }
            }

            // Then, look in the mixin types ...
            if (mixinTypeNamesOfParent != null && !mixinTypeNamesOfParent.isEmpty()) {
                for (Name mixinTypeName : mixinTypeNamesOfParent) {
                    JcrNodeType mixinType = getNodeType(mixinTypeName);
                    if (mixinType == null) continue;
                    for (JcrPropertyDefinition definition : mixinType.allPropertyDefinitions(propertyName)) {
                        // Skip protected definitions ...
                        if (skipProtected && definition.isProtected()) continue;
                        // If this definition is not mandatory, then we have found that we CAN remove the property ...
                        return !definition.isMandatory();
                    }
                }
            }

            // Nothing was found, so look for residual node definitions ...
            if (!propertyName.equals(JcrNodeType.RESIDUAL_NAME)) return canRemoveProperty(primaryTypeNameOfParent,
                                                                                          mixinTypeNamesOfParent,
                                                                                          JcrNodeType.RESIDUAL_NAME,
                                                                                          skipProtected);
            return false;
        }

        /**
         * Determine if the node and property definitions of the supplied primary type and mixin types allow the item with the
         * supplied name to be removed.
         * 
         * @param primaryTypeNameOfParent the name of the primary type for the parent node; may not be null
         * @param mixinTypeNamesOfParent the names of the mixin types for the parent node; may be null or empty if there are no
         *        mixins to include in the search
         * @param itemName the name of the item to be removed; may not be null
         * @param skipProtected true if this operation is being done from within the public JCR node and property API, or false if
         *        this operation is being done from within internal implementations
         * @return true if at least one child node definition does not require children with the supplied name to exist, or false
         *         otherwise
         */
        boolean canRemoveItem( Name primaryTypeNameOfParent,
                               List<Name> mixinTypeNamesOfParent,
                               Name itemName,
                               boolean skipProtected ) {
            // First look in the primary type for a matching property definition...
            JcrNodeType primaryType = getNodeType(primaryTypeNameOfParent);
            if (primaryType != null) {
                for (JcrPropertyDefinition definition : primaryType.allPropertyDefinitions(itemName)) {
                    // Skip protected definitions ...
                    if (skipProtected && definition.isProtected()) continue;
                    // If this definition is not mandatory, then we have found that we CAN remove the property ...
                    return !definition.isMandatory();
                }
            }

            // Then, look in the primary type for a matching child node definition...
            if (primaryType != null) {
                for (JcrNodeDefinition definition : primaryType.allChildNodeDefinitions(itemName)) {
                    // Skip protected definitions ...
                    if (skipProtected && definition.isProtected()) continue;
                    // If this definition is not mandatory, then we have found that we CAN remove all children ...
                    return !definition.isMandatory();
                }
            }

            // Then, look in the mixin types for a matching property definition...
            if (mixinTypeNamesOfParent != null && !mixinTypeNamesOfParent.isEmpty()) {
                for (Name mixinTypeName : mixinTypeNamesOfParent) {
                    JcrNodeType mixinType = getNodeType(mixinTypeName);
                    if (mixinType == null) continue;
                    for (JcrPropertyDefinition definition : mixinType.allPropertyDefinitions(itemName)) {
                        // Skip protected definitions ...
                        if (skipProtected && definition.isProtected()) continue;
                        // If this definition is not mandatory, then we have found that we CAN remove the property ...
                        return !definition.isMandatory();
                    }
                }
            }

            // Then, look in the mixin types for a matching child node definition...
            if (mixinTypeNamesOfParent != null && !mixinTypeNamesOfParent.isEmpty()) {
                for (Name mixinTypeName : mixinTypeNamesOfParent) {
                    JcrNodeType mixinType = getNodeType(mixinTypeName);
                    if (mixinType == null) continue;
                    for (JcrNodeDefinition definition : mixinType.allChildNodeDefinitions(itemName)) {
                        // Skip protected definitions ...
                        if (skipProtected && definition.isProtected()) continue;
                        // If this definition is not mandatory, then we have found that we CAN remove all children ...
                        return !definition.isMandatory();
                    }
                }
            }

            // Nothing was found, so look for residual item definitions ...
            if (!itemName.equals(JcrNodeType.RESIDUAL_NAME)) return canRemoveItem(primaryTypeNameOfParent,
                                                                                  mixinTypeNamesOfParent,
                                                                                  JcrNodeType.RESIDUAL_NAME,
                                                                                  skipProtected);
            return false;
        }

        protected final JcrNodeDefinition findChildNodeDefinitionForUnstructured( boolean requireSns ) {
            return requireSns ? ntUnstructuredSnsChildDefinition : ntUnstructuredSingleChildDefinition;
        }

        /**
         * Searches the supplied primary node type and the mixin node types of a parent node for a child node definition that is
         * the best match for a new child with the given name, primary node type name, and whether there are existing children
         * with the same name.
         * 
         * @param primaryTypeNameOfParent the name of the primary type for the parent node; may not be null
         * @param mixinTypeNamesOfParent the names of the mixin types for the parent node; may be null or empty if there are no
         *        mixins to include in the search
         * @param childName the name of the child to be added to the parent; may not be null
         * @param childPrimaryNodeType the name of the primary node type for the child node, or null if the primary type is not
         *        known and the {@link NodeDefinition#getDefaultPrimaryType() definition's default primary type} will be used
         * @param numberOfExistingChildrenWithSameName the number of existing children with the same name as the child to be
         *        added, or 0 if this new child will be the first child with this name (or if the number of children is not known)
         * @param skipProtected true if this operation is being done from within the public JCR node and property API, or false if
         *        this operation is being done from within internal implementations
         * @return the best child node definition, or <code>null</code> if no node definition allows a new child with the supplied
         *         name, primary type, and whether there are already children with the same name
         */
        JcrNodeDefinition findChildNodeDefinition( Name primaryTypeNameOfParent,
                                                   Collection<Name> mixinTypeNamesOfParent,
                                                   Name childName,
                                                   Name childPrimaryNodeType,
                                                   int numberOfExistingChildrenWithSameName,
                                                   boolean skipProtected ) {
            JcrNodeType childType = childPrimaryNodeType != null ? getNodeType(childPrimaryNodeType) : null;
            boolean requireSns = numberOfExistingChildrenWithSameName > 1;

            // Check for a very common case first ...
            if ((mixinTypeNamesOfParent == null || mixinTypeNamesOfParent.isEmpty())
                && JcrNtLexicon.UNSTRUCTURED.equals(primaryTypeNameOfParent)) {
                // This is a very common case of an 'nt:unstructured' node with no mixins ...
                return findChildNodeDefinitionForUnstructured(requireSns);
            }

            // First look in the primary type ...
            JcrNodeType primaryType = getNodeType(primaryTypeNameOfParent);
            if (primaryType != null) {
                for (JcrNodeDefinition definition : primaryType.allChildNodeDefinitions(childName, requireSns)) {
                    // Skip protected definitions ...
                    if (skipProtected && definition.isProtected()) continue;
                    // See if the definition allows a child with the supplied primary type ...
                    if (definition.allowsChildWithType(childType)) return definition;
                }
            }

            // Then, look in the mixin types ...
            if (mixinTypeNamesOfParent != null) {
                for (Name mixinTypeName : mixinTypeNamesOfParent) {
                    JcrNodeType mixinType = getNodeType(mixinTypeName);
                    if (mixinType == null) continue;
                    for (JcrNodeDefinition definition : mixinType.allChildNodeDefinitions(childName, requireSns)) {
                        // Skip protected definitions ...
                        if (skipProtected && definition.isProtected()) continue;
                        // See if the definition allows a child with the supplied primary type ...
                        if (definition.allowsChildWithType(childType)) return definition;
                    }
                }
            }

            // Nothing was found, so look for residual node definitions ...
            if (!childName.equals(JcrNodeType.RESIDUAL_NAME)) return findChildNodeDefinition(primaryTypeNameOfParent,
                                                                                             mixinTypeNamesOfParent,
                                                                                             JcrNodeType.RESIDUAL_NAME,
                                                                                             childPrimaryNodeType,
                                                                                             numberOfExistingChildrenWithSameName,
                                                                                             skipProtected);
            return null;
        }

        /**
         * Searches the supplied primary and mixin node types for all valid child node definitions that match the given child node
         * name and cardinality.
         * <p>
         * If no satisfactory child node definition could be found, this method returns an empty list.
         * </p>
         * 
         * @param typeNamesToCheck the name of the types to check; may not be null
         * @param childNodeName the name of the child node for which the definitions should be retrieved
         * @param typesToCheck the type of definitions to consider (allows SNS or does not allow SNS)
         * @param pendingTypes a list of types that have been created during type registration but not yet registered in the type
         *        map
         * @return a list of all valid chlid node definitions that match the given child node name and cardinality
         */
        private List<JcrNodeDefinition> findChildNodeDefinitions( List<Name> typeNamesToCheck,
                                                                  Name childNodeName,
                                                                  NodeCardinality typesToCheck,
                                                                  List<JcrNodeType> pendingTypes ) {
            assert typeNamesToCheck != null;
            Collection<JcrNodeDefinition> nodeDefs = null;
            List<JcrNodeDefinition> matchingDefs = new ArrayList<JcrNodeDefinition>();

            for (Name typeNameToCheck : typeNamesToCheck) {
                JcrNodeType typeName = findTypeInMapOrList(typeNameToCheck, pendingTypes);
                if (typeName == null) continue;

                switch (typesToCheck) {
                    case NO_SAME_NAME_SIBLINGS:
                        nodeDefs = typeName.allChildNodeDefinitions(childNodeName, false);
                        break;
                    case SAME_NAME_SIBLINGS:
                        nodeDefs = typeName.allChildNodeDefinitions(childNodeName, true);
                        break;
                    case ANY:
                        nodeDefs = typeName.allChildNodeDefinitions(childNodeName);
                        break;
                }

                assert nodeDefs != null;
                for (JcrNodeDefinition definition : nodeDefs) {
                    if (NodeCardinality.NO_SAME_NAME_SIBLINGS == typesToCheck && definition.allowsSameNameSiblings()) continue;
                    matchingDefs.add(definition);
                }
            }

            return matchingDefs;
        }

        /**
         * Determine if the child node definitions of the supplied primary type and mixin types of a parent node allow all of the
         * children with the supplied name to be removed.
         * 
         * @param primaryTypeNameOfParent the name of the primary type for the parent node; may not be null
         * @param mixinTypeNamesOfParent the names of the mixin types for the parent node; may be null or empty if there are no
         *        mixins to include in the search
         * @param childName the name of the child to be added to the parent; may not be null
         * @param skipProtected true if this operation is being done from within the public JCR node and property API, or false if
         *        this operation is being done from within internal implementations
         * @return true if at least one child node definition does not require children with the supplied name to exist, or false
         *         otherwise
         */
        boolean canRemoveAllChildren( Name primaryTypeNameOfParent,
                                      Collection<Name> mixinTypeNamesOfParent,
                                      Name childName,
                                      boolean skipProtected ) {
            // First look in the primary type ...
            JcrNodeType primaryType = getNodeType(primaryTypeNameOfParent);
            if (primaryType != null) {
                for (JcrNodeDefinition definition : primaryType.allChildNodeDefinitions(childName)) {
                    // Skip protected definitions ...
                    if (skipProtected && definition.isProtected()) continue;
                    // If this definition is not mandatory, then we have found that we CAN remove all children ...
                    return !definition.isMandatory();
                }
            }

            // Then, look in the mixin types ...
            if (mixinTypeNamesOfParent != null) {
                for (Name mixinTypeName : mixinTypeNamesOfParent) {
                    JcrNodeType mixinType = getNodeType(mixinTypeName);
                    if (mixinType == null) continue;
                    for (JcrNodeDefinition definition : mixinType.allChildNodeDefinitions(childName)) {
                        // Skip protected definitions ...
                        if (skipProtected && definition.isProtected()) continue;
                        // If this definition is not mandatory, then we have found that we CAN remove all children ...
                        return !definition.isMandatory();
                    }
                }
            }

            // Nothing was found, so look for residual node definitions ...
            if (!childName.equals(JcrNodeType.RESIDUAL_NAME)) return canRemoveAllChildren(primaryTypeNameOfParent,
                                                                                          mixinTypeNamesOfParent,
                                                                                          JcrNodeType.RESIDUAL_NAME,
                                                                                          skipProtected);
            return false;
        }

        /**
         * Finds the named type in the given collection of types pending registration if it exists, else returns the type
         * definition from the repository
         * 
         * @param typeName the name of the type to retrieve
         * @param pendingList a collection of types that have passed validation but have not yet been committed to the repository
         * @return the node type with the given name from {@code pendingList} if it exists in the collection or from the
         *         {@link #nodeTypes registered types} if it exists there; may be null
         */
        protected JcrNodeType findTypeInMapOrList( Name typeName,
                                                   Collection<JcrNodeType> pendingList ) {
            for (JcrNodeType pendingNodeType : pendingList) {
                if (pendingNodeType.getInternalName().equals(typeName)) {
                    return pendingNodeType;
                }
            }

            return nodeTypes.get(typeName);
        }

        /**
         * Returns the list of node types for the supertypes defined in the given node type.
         * 
         * @param nodeType a node type with a non-null array of supertypes
         * @param pendingTypes the list of types that have been processed in this type batch but not yet committed to the
         *        repository's set of types
         * @return a list of node types where each element is the node type for the corresponding element of the array of
         *         supertype names
         * @throws RepositoryException if any of the names in the array of supertype names does not correspond to an
         *         already-registered node type or a node type that is pending registration
         */
        protected List<JcrNodeType> supertypesFor( NodeTypeDefinition nodeType,
                                                   Collection<JcrNodeType> pendingTypes ) throws RepositoryException {
            assert nodeType != null;

            List<JcrNodeType> supertypes = new LinkedList<JcrNodeType>();

            boolean isMixin = nodeType.isMixin();
            boolean needsPrimaryAncestor = !isMixin;
            String nodeTypeName = nodeType.getName();

            for (String supertypeNameStr : nodeType.getDeclaredSupertypeNames()) {
                Name supertypeName = nameFactory.create(supertypeNameStr);
                JcrNodeType supertype = findTypeInMapOrList(supertypeName, pendingTypes);
                if (supertype == null) {
                    throw new InvalidNodeTypeDefinitionException(
                                                                 JcrI18n.invalidSupertypeName.text(supertypeNameStr, nodeTypeName));
                }
                needsPrimaryAncestor &= supertype.isMixin();
                supertypes.add(supertype);
            }

            // primary types (other than nt:base) always have at least one ancestor that's a primary type - nt:base
            if (needsPrimaryAncestor) {
                Name nodeName = nameFactory.create(nodeTypeName);
                if (!JcrNtLexicon.BASE.equals(nodeName)) {
                    JcrNodeType ntBase = findTypeInMapOrList(JcrNtLexicon.BASE, pendingTypes);
                    assert ntBase != null;
                    supertypes.add(0, ntBase);
                }
            }
            return supertypes;
        }

        /**
         * Returns the list of subtypes for the given node.
         * 
         * @param nodeType the node type for which subtypes should be returned; may not be null
         * @return the subtypes for the node
         */
        final Collection<JcrNodeType> subtypesFor( JcrNodeType nodeType ) {
            List<JcrNodeType> subtypes = new LinkedList<JcrNodeType>();
            for (JcrNodeType type : this.nodeTypes.values()) {
                if (type.supertypes().contains(nodeType)) {
                    subtypes.add(type);
                }
            }
            return subtypes;
        }

        /**
         * Returns the list of declared subtypes for the given node.
         * 
         * @param nodeType the node type for which declared subtypes should be returned; may not be null
         * @return the subtypes for the node
         */
        final Collection<JcrNodeType> declaredSubtypesFor( JcrNodeType nodeType ) {
            CheckArg.isNotNull(nodeType, "nodeType");
            String nodeTypeName = nodeType.getName();
            List<JcrNodeType> subtypes = new LinkedList<JcrNodeType>();
            for (JcrNodeType type : this.nodeTypes.values()) {
                if (Arrays.asList(type.getDeclaredSupertypeNames()).contains(nodeTypeName)) {
                    subtypes.add(type);
                }
            }
            return subtypes;
        }

        /**
         * Validates that the supertypes are compatible under ModeShape restrictions.
         * <p>
         * ModeShape imposes the following rules on the supertypes of a type:
         * <ol>
         * <li>The type must have at least one supertype (unless the type is {@code nt:base}.</li>
         * <li>No two supertypes {@code t1} and {@code t2} can declare each declare a property ({@code p1} and {@code p2}) with
         * the same name and cardinality ({@code p1.isMultiple() == p2.isMultiple()}). Note that this does prohibit each
         * {@code t1} and {@code t2} from having a common supertype (or super-supertype, etc.) that declares a property).</li>
         * <li>No two supertypes {@code t1} and {@code t2} can declare each declare a child node ({@code n1} and {@code n2}) with
         * the same name and SNS status ({@code p1.allowsSameNameSiblings() == p2.allowsSameNameSiblings()}). Note that this does
         * prohibit each {@code t1} and {@code t2} from having a common supertype (or super-supertype, etc.) that declares a child
         * node).</li>
         * </ol>
         * </p>
         * <p>
         * If any of these rules are violated, a {@link RepositoryException} is thrown.
         * </p>
         * 
         * @param supertypes the supertypes of this node type
         * @throws RepositoryException if any of the rules described above are violated
         */
        private void validateSupertypes( List<JcrNodeType> supertypes ) throws RepositoryException {
            assert supertypes != null;

            Map<PropertyDefinitionId, JcrPropertyDefinition> props = new HashMap<PropertyDefinitionId, JcrPropertyDefinition>();

            for (JcrNodeType supertype : supertypes) {
                for (JcrPropertyDefinition property : supertype.propertyDefinitions()) {
                    JcrPropertyDefinition oldProp = props.put(new PropertyDefinitionId(property.getInternalName(),
                                                                                       property.getInternalName(),
                                                                                       PropertyType.UNDEFINED,
                                                                                       property.isMultiple()),
                                                              property);
                    if (oldProp != null) {
                        String oldPropTypeName = oldProp.getDeclaringNodeType().getName();
                        String propTypeName = property.getDeclaringNodeType().getName();
                        if (!oldPropTypeName.equals(propTypeName)) {
                            throw new InvalidNodeTypeDefinitionException(JcrI18n.supertypesConflict.text(oldPropTypeName,
                                                                                                         propTypeName,
                                                                                                         "property",
                                                                                                         property.getName()));
                        }
                    }
                }
            }

            Map<NodeDefinitionId, JcrNodeDefinition> childNodes = new HashMap<NodeDefinitionId, JcrNodeDefinition>();

            for (JcrNodeType supertype : supertypes) {
                for (JcrNodeDefinition childNode : supertype.childNodeDefinitions()) {
                    JcrNodeDefinition oldNode = childNodes.put(new NodeDefinitionId(childNode.getInternalName(),
                                                                                    childNode.getInternalName(), new Name[0]),
                                                               childNode);
                    if (oldNode != null) {
                        String oldNodeTypeName = oldNode.getDeclaringNodeType().getName();
                        String childNodeTypeName = childNode.getDeclaringNodeType().getName();
                        if (!oldNodeTypeName.equals(childNodeTypeName)) {
                            throw new InvalidNodeTypeDefinitionException(JcrI18n.supertypesConflict.text(oldNodeTypeName,
                                                                                                         childNodeTypeName,
                                                                                                         "child node",
                                                                                                         childNode.getName()));
                        }
                    }
                }
            }
        }

        /**
         * Validates that the given node type definition is valid under the ModeShape and JCR type rules within the given context.
         * <p>
         * See {@link #registerNodeTypes(Iterable)} for the list of criteria that determine whether a node type definition is
         * valid.
         * </p>
         * 
         * @param nodeType the node type to attempt to validate
         * @param supertypes the names of the supertypes of the node type
         * @param pendingTypes the list of types previously registered in this batch but not yet committed to the repository
         * @throws RepositoryException if the given node type template is not valid
         */
        protected void validate( JcrNodeType nodeType,
                                 List<JcrNodeType> supertypes,
                                 List<JcrNodeType> pendingTypes ) throws RepositoryException {
            validateSupertypes(supertypes);

            List<Name> supertypeNames = new ArrayList<Name>(supertypes.size());
            for (JcrNodeType supertype : supertypes) {
                supertypeNames.add(supertype.getInternalName());
            }

            boolean foundExact = false;
            boolean foundResidual = false;
            Name primaryItemName = nodeType.getInternalPrimaryItemName();

            for (JcrNodeDefinition node : nodeType.getDeclaredChildNodeDefinitions()) {
                validateChildNodeDefinition(node, supertypeNames, pendingTypes);
                if (node.isResidual()) {
                    foundResidual = true;
                }

                if (primaryItemName != null && primaryItemName.equals(node.getInternalName())) {
                    foundExact = true;
                }
            }

            for (JcrPropertyDefinition prop : nodeType.getDeclaredPropertyDefinitions()) {
                validatePropertyDefinition(prop, supertypeNames, pendingTypes);
                if (prop.isResidual()) {
                    foundResidual = true;
                }
                if (primaryItemName != null && primaryItemName.equals(prop.getInternalName())) {
                    if (foundExact) {
                        throw new RepositoryException(JcrI18n.ambiguousPrimaryItemName.text(primaryItemName));
                    }
                    foundExact = true;
                }
            }

            if (primaryItemName != null && !foundExact && !foundResidual) {
                throw new RepositoryException(JcrI18n.invalidPrimaryItemName.text(primaryItemName));
            }
        }

        /**
         * Validates that the given child node definition is valid under the ModeShape and JCR type rules within the given
         * context.
         * <p>
         * ModeShape considers a child node definition valid if it meets these criteria:
         * <ol>
         * <li>Residual child node definitions cannot be mandatory</li>
         * <li>If the child node is auto-created, it must specify a default primary type name</li>
         * <li>If the child node overrides an existing child node definition from a supertype, the new definition must be
         * mandatory if the old definition was mandatory</li>
         * <li>The child node cannot override an existing child node definition from a supertype if the ancestor definition is
         * protected</li>
         * <li>If the child node overrides an existing child node definition from a supertype, the required primary types of the
         * new definition must be more restrictive than the required primary types of the old definition - that is, the new
         * primary types must defined such that any type that satisfies all of the required primary types for the new definition
         * must also satisfy all of the required primary types for the old definition. This requirement is analogous to the
         * requirement that overriding property definitions have a required type that is always convertible to the required type
         * of the overridden definition.</li>
         * </ol>
         * </p>
         * 
         * @param childNodeDefinition the child node definition to be validated
         * @param supertypes the names of the supertypes of the node type to which this child node belongs
         * @param pendingTypes the list of types previously registered in this batch but not yet committed to the repository
         * @throws RepositoryException if the child node definition is not valid
         */
        private void validateChildNodeDefinition( JcrNodeDefinition childNodeDefinition,
                                                  List<Name> supertypes,
                                                  List<JcrNodeType> pendingTypes ) throws RepositoryException {
            if (childNodeDefinition.isAutoCreated() && !childNodeDefinition.isProtected()
                && childNodeDefinition.defaultPrimaryTypeName() == null) {
                throw new InvalidNodeTypeDefinitionException(
                                                             JcrI18n.autocreatedNodesNeedDefaults.text(childNodeDefinition.getName()));
            }
            boolean residual = JcrNodeType.RESIDUAL_ITEM_NAME.equals(childNodeDefinition.getName());
            if (childNodeDefinition.isMandatory() && residual) {
                throw new InvalidNodeTypeDefinitionException(
                                                             JcrI18n.residualNodeDefinitionsCannotBeMandatory.text(childNodeDefinition.getName()));
            }
            if (childNodeDefinition.isAutoCreated() && residual) {
                throw new InvalidNodeTypeDefinitionException(
                                                             JcrI18n.residualNodeDefinitionsCannotBeAutoCreated.text(childNodeDefinition.getName()));
            }

            Name childNodeName = context.getValueFactories().getNameFactory().create(childNodeDefinition.getName());
            childNodeName = childNodeName == null ? JcrNodeType.RESIDUAL_NAME : childNodeName;

            List<JcrNodeDefinition> childNodesInAncestors = findChildNodeDefinitions(supertypes,
                                                                                     childNodeName,
                                                                                     NodeCardinality.ANY,
                                                                                     pendingTypes);

            for (JcrNodeDefinition childNodeFromAncestor : childNodesInAncestors) {
                if (childNodeFromAncestor.isProtected()) {
                    throw new InvalidNodeTypeDefinitionException(
                                                                 JcrI18n.cannotOverrideProtectedDefinition.text(childNodeFromAncestor.getDeclaringNodeType()
                                                                                                                                     .getName(),
                                                                                                                "child node"));
                }

                if (childNodeFromAncestor.isMandatory() && !childNodeDefinition.isMandatory()) {
                    throw new InvalidNodeTypeDefinitionException(
                                                                 JcrI18n.cannotMakeMandatoryDefinitionOptional.text(childNodeFromAncestor.getDeclaringNodeType()
                                                                                                                                         .getName(),
                                                                                                                    "child node"));

                }

                Name[] requiredPrimaryTypeNames = childNodeFromAncestor.requiredPrimaryTypeNames();
                for (Name requiredPrimaryTypeName : requiredPrimaryTypeNames) {
                    NodeType requiredPrimaryTypeFromAncestor = findTypeInMapOrList(requiredPrimaryTypeName, pendingTypes);

                    if (requiredPrimaryTypeFromAncestor == null) {
                        I18n msg = JcrI18n.couldNotFindDefinitionOfRequiredPrimaryType;
                        throw new InvalidNodeTypeDefinitionException(msg.text(requiredPrimaryTypeName,
                                                                              childNodeDefinition.getName(),
                                                                              childNodeDefinition.getDeclaringNodeType()));

                    }

                    boolean found = false;

                    for (Name name : childNodeDefinition.requiredPrimaryTypeNames()) {
                        JcrNodeType childNodePrimaryType = findTypeInMapOrList(name, pendingTypes);

                        if (childNodePrimaryType != null
                            && childNodePrimaryType.isNodeType(requiredPrimaryTypeFromAncestor.getName())) {
                            found = true;
                            break;
                        }
                    }

                    // Allow side-by-side definitions of residual child nodes per JCR 1.0.1 spec 6.7.8
                    if (!found && !residual) {
                        I18n msg = JcrI18n.cannotRedefineChildNodeWithIncompatibleDefinition;
                        throw new InvalidNodeTypeDefinitionException(msg.text(childNodeName,
                                                                              requiredPrimaryTypeFromAncestor.getName(),
                                                                              childNodeDefinition.getDeclaringNodeType()));
                    }
                }
            }
        }

        /**
         * Validates that the given property definition is valid under the ModeShape and JCR type rules within the given context.
         * <p>
         * ModeShape considers a property definition valid if it meets these criteria:
         * <ol>
         * <li>Residual properties cannot be mandatory</li>
         * <li>If the property is auto-created, it must specify a default value</li>
         * <li>If the property is single-valued, it can only specify a single default value</li>
         * <li>If the property overrides an existing property definition from a supertype, the new definition must be mandatory if
         * the old definition was mandatory</li>
         * <li>The property cannot override an existing property definition from a supertype if the ancestor definition is
         * protected</li>
         * <li>If the property overrides an existing property definition from a supertype, the new definition must have the same
         * required type as the old definition or a required type that can ALWAYS be cast to the required type of the ancestor
         * (see section 3.6.4 of the JCR 2.0 specification)</li>
         * </ol>
         * Note that an empty set of properties would meet the criteria above.
         * </p>
         * 
         * @param propertyDefinition the property definition to be validated
         * @param supertypes the names of the supertypes of the node type to which this property belongs
         * @param pendingTypes the list of types previously registered in this batch but not yet committed to the repository
         * @throws RepositoryException if the property definition is not valid
         */
        private void validatePropertyDefinition( JcrPropertyDefinition propertyDefinition,
                                                 List<Name> supertypes,
                                                 List<JcrNodeType> pendingTypes ) throws RepositoryException {
            assert propertyDefinition != null;
            assert supertypes != null;
            assert pendingTypes != null;

            boolean residual = JcrNodeType.RESIDUAL_ITEM_NAME.equals(propertyDefinition.getName());
            if (propertyDefinition.isMandatory() && !propertyDefinition.isProtected() && residual) {
                throw new InvalidNodeTypeDefinitionException(
                                                             JcrI18n.residualPropertyDefinitionsCannotBeMandatory.text(propertyDefinition.getName()));
            }
            if (propertyDefinition.isAutoCreated() && residual) {
                throw new InvalidNodeTypeDefinitionException(
                                                             JcrI18n.residualPropertyDefinitionsCannotBeAutoCreated.text(propertyDefinition.getName()));
            }

            Value[] defaultValues = propertyDefinition.getDefaultValues();
            if (propertyDefinition.isAutoCreated() && !propertyDefinition.isProtected()
                && (defaultValues == null || defaultValues.length == 0)) {
                throw new InvalidNodeTypeDefinitionException(
                                                             JcrI18n.autocreatedPropertyNeedsDefault.text(propertyDefinition.getName(),
                                                                                                          propertyDefinition.getDeclaringNodeType()
                                                                                                                            .getName()));
            }

            if (!propertyDefinition.isMultiple() && (defaultValues != null && defaultValues.length > 1)) {
                throw new InvalidNodeTypeDefinitionException(
                                                             JcrI18n.singleValuedPropertyNeedsSingleValuedDefault.text(propertyDefinition.getName(),
                                                                                                                       propertyDefinition.getDeclaringNodeType()
                                                                                                                                         .getName()));
            }

            Name propName = context.getValueFactories().getNameFactory().create(propertyDefinition.getName());
            propName = propName == null ? JcrNodeType.RESIDUAL_NAME : propName;

            List<JcrPropertyDefinition> propertyDefinitionsFromAncestors = findPropertyDefinitions(supertypes,
                                                                                                   propName,
                                                                                                   propertyDefinition.isMultiple() ? PropertyCardinality.MULTI_VALUED_ONLY : PropertyCardinality.SINGLE_VALUED_ONLY,
                                                                                                   pendingTypes);

            for (JcrPropertyDefinition propertyDefinitionFromAncestor : propertyDefinitionsFromAncestors) {
                if (propertyDefinitionFromAncestor.isProtected()) {
                    throw new InvalidNodeTypeDefinitionException(
                                                                 JcrI18n.cannotOverrideProtectedDefinition.text(propertyDefinitionFromAncestor.getDeclaringNodeType()
                                                                                                                                              .getName(),
                                                                                                                "property"));
                }

                if (propertyDefinitionFromAncestor.isMandatory() && !propertyDefinition.isMandatory()) {
                    throw new InvalidNodeTypeDefinitionException(
                                                                 JcrI18n.cannotMakeMandatoryDefinitionOptional.text(propertyDefinitionFromAncestor.getDeclaringNodeType()
                                                                                                                                                  .getName(),
                                                                                                                    "property"));

                }

                if (!propertyDefinition.isAsOrMoreConstrainedThan(propertyDefinitionFromAncestor, context)) {
                    throw new InvalidNodeTypeDefinitionException(
                                                                 JcrI18n.constraintsChangedInSubtype.text(propName,
                                                                                                          propertyDefinitionFromAncestor.getDeclaringNodeType()
                                                                                                                                        .getName()));
                }

                if (!isAlwaysSafeConversion(propertyDefinition.getRequiredType(),
                                            propertyDefinitionFromAncestor.getRequiredType())) {
                    throw new InvalidNodeTypeDefinitionException(
                                                                 JcrI18n.cannotRedefineProperty.text(propName,
                                                                                                     org.modeshape.jcr.api.PropertyType.nameFromValue(propertyDefinition.getRequiredType()),
                                                                                                     propertyDefinitionFromAncestor.getDeclaringNodeType()
                                                                                                                                   .getName(),
                                                                                                     org.modeshape.jcr.api.PropertyType.nameFromValue(propertyDefinitionFromAncestor.getRequiredType())));

                }
            }
        }

        /**
         * Returns whether it is always possible to convert a value with JCR property type {@code fromType} to {@code toType}.
         * <p>
         * This method is based on the conversions which can never throw an exception in the chart in section 3.6.4 of the JCR 2.0
         * specification.
         * </p>
         * 
         * @param fromType the type to be converted from
         * @param toType the type to convert to
         * @return true if any value with type {@code fromType} can be converted to a type of {@code toType} without a
         *         {@link ValueFormatException} being thrown.
         * @see PropertyType
         */
        private boolean isAlwaysSafeConversion( int fromType,
                                                int toType ) {

            if (fromType == toType) return true;

            switch (toType) {
                case PropertyType.BOOLEAN:
                    return fromType == PropertyType.BINARY || fromType == PropertyType.STRING;

                case PropertyType.DATE:
                    return fromType == PropertyType.DOUBLE || fromType == PropertyType.LONG;

                case PropertyType.DOUBLE:
                    // Conversion from DATE could result in out-of-range value
                    return fromType == PropertyType.LONG;
                case PropertyType.LONG:
                    // Conversion from DATE could result in out-of-range value
                    return fromType == PropertyType.DOUBLE;

                case PropertyType.PATH:
                    return fromType == PropertyType.NAME;

                    // Values of any type MAY fail when converting to these types
                case PropertyType.NAME:
                case PropertyType.REFERENCE:
                case PropertyType.WEAKREFERENCE:
                case org.modeshape.jcr.api.PropertyType.SIMPLE_REFERENCE:
                    return false;

                    // Any type can be converted to these types
                case PropertyType.BINARY:
                case PropertyType.STRING:
                case PropertyType.UNDEFINED:
                    return true;

                default:
                    throw new IllegalStateException("Unexpected state: " + toType);
            }
        }

        @Override
        public String toString() {
            return getAllNodeTypes().toString();
        }
    }
}
