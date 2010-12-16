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
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.version.OnParentVersionAction;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.text.TextEncoder;
import org.modeshape.common.text.XmlNameEncoder;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.Logger;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.Location;
import org.modeshape.graph.Subgraph;
import org.modeshape.graph.observe.Changes;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NameFactory;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.PathNotFoundException;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.PropertyFactory;
import org.modeshape.graph.query.QueryResults;
import org.modeshape.graph.query.model.QueryCommand;
import org.modeshape.graph.query.model.TypeSystem;
import org.modeshape.graph.query.parse.QueryParser;
import org.modeshape.graph.query.parse.SqlQueryParser;
import org.modeshape.graph.query.validate.Schemata;
import org.modeshape.graph.request.ChangeRequest;
import org.modeshape.jcr.nodetype.InvalidNodeTypeDefinitionException;
import org.modeshape.jcr.nodetype.NodeTypeExistsException;

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
class RepositoryNodeTypeManager implements JcrSystemObserver {
    private static final Logger LOGGER = Logger.getLogger(RepositoryNodeTypeManager.class);

    private static final TextEncoder NAME_ENCODER = new XmlNameEncoder();

    private final JcrRepository repository;
    private final QueryParser queryParser;
    private final ExecutionContext context;
    private final Path nodeTypesPath;

    @GuardedBy( "nodeTypeManagerLock" )
    private final Map<Name, JcrNodeType> nodeTypes;
    @GuardedBy( "nodeTypeManagerLock" )
    private final Map<PropertyDefinitionId, JcrPropertyDefinition> propertyDefinitions;
    @GuardedBy( "nodeTypeManagerLock" )
    private final Map<NodeDefinitionId, JcrNodeDefinition> childNodeDefinitions;
    @GuardedBy( "nodeTypeManagerLock" )
    private NodeTypeSchemata schemata;
    private final PropertyFactory propertyFactory;
    private final PathFactory pathFactory;
    private final NameFactory nameFactory;
    private final ReadWriteLock nodeTypeManagerLock = new ReentrantReadWriteLock();
    private final boolean includeColumnsForInheritedProperties;
    private final boolean includePseudoColumnsInSelectStar;

    /**
     * List of ways to filter the returned property definitions
     * 
     * @see RepositoryNodeTypeManager#findPropertyDefinitions(List, Name, PropertyCardinality, List)
     */
    private enum PropertyCardinality {
        SINGLE_VALUED_ONLY,
        MULTI_VALUED_ONLY,
        ANY
    }

    /**
     * List of ways to filter the returned node definitions
     * 
     * @see RepositoryNodeTypeManager#findChildNodeDefinitions(List, Name, NodeCardinality, List)
     */
    private enum NodeCardinality {
        NO_SAME_NAME_SIBLINGS,
        SAME_NAME_SIBLINGS,
        ANY
    }

    RepositoryNodeTypeManager( JcrRepository repository,
                               Path nodeTypesPath,
                               boolean includeColumnsForInheritedProperties,
                               boolean includePseudoColumnsInSelectStar ) {
        this.repository = repository;
        this.nodeTypesPath = nodeTypesPath;
        this.context = repository.getExecutionContext();
        this.includeColumnsForInheritedProperties = includeColumnsForInheritedProperties;
        this.includePseudoColumnsInSelectStar = includePseudoColumnsInSelectStar;
        this.propertyFactory = context.getPropertyFactory();
        this.pathFactory = context.getValueFactories().getPathFactory();
        this.nameFactory = context.getValueFactories().getNameFactory();

        propertyDefinitions = new HashMap<PropertyDefinitionId, JcrPropertyDefinition>();
        childNodeDefinitions = new HashMap<NodeDefinitionId, JcrNodeDefinition>();
        nodeTypes = new HashMap<Name, JcrNodeType>(50);
        queryParser = new SqlQueryParser();

        if (nodeTypesPath != null) {
            Graph systemGraph = repository.createSystemGraph(context);
            try {
                systemGraph.getNodeAt(nodeTypesPath);
            } catch (PathNotFoundException pnfe) {
                systemGraph.create(nodeTypesPath).with(JcrLexicon.PRIMARY_TYPE, ModeShapeLexicon.NODE_TYPES).and();
            }
        }
    }

    /**
     * Return an immutable snapshot of the node types that are currently registered in this node type manager.
     * 
     * @return the immutable collection of (immutable) node types; never null
     */
    public Collection<JcrNodeType> getAllNodeTypes() {
        try {
            nodeTypeManagerLock.readLock().lock();
            return Collections.unmodifiableCollection(new ArrayList<JcrNodeType>(nodeTypes.values()));
        } finally {
            nodeTypeManagerLock.readLock().unlock();
        }
    }

    /**
     * Return an immutable snapshot of the mixin node types that are currently registered in this node type manager.
     * 
     * @return the immutable collection of (immutable) mixin node types; never null
     * @see #getPrimaryNodeTypes()
     */
    public Collection<JcrNodeType> getMixinNodeTypes() {
        try {
            nodeTypeManagerLock.readLock().lock();

            List<JcrNodeType> types = new ArrayList<JcrNodeType>(nodeTypes.size());

            for (JcrNodeType nodeType : nodeTypes.values()) {
                if (nodeType.isMixin()) types.add(nodeType);
            }

            return types;
        } finally {
            nodeTypeManagerLock.readLock().unlock();
        }
    }

    /**
     * Return an immutable snapshot of the primary node types that are currently registered in this node type manager.
     * 
     * @return the immutable collection of (immutable) primary node types; never null
     * @see #getMixinNodeTypes()
     */
    public Collection<JcrNodeType> getPrimaryNodeTypes() {
        try {
            nodeTypeManagerLock.readLock().lock();
            List<JcrNodeType> types = new ArrayList<JcrNodeType>(nodeTypes.size());

            for (JcrNodeType nodeType : nodeTypes.values()) {
                if (!nodeType.isMixin()) types.add(nodeType);
            }

            return types;
        } finally {
            nodeTypeManagerLock.readLock().unlock();
        }
    }

    public JcrPropertyDefinition getPropertyDefinition( PropertyDefinitionId id ) {
        try {
            nodeTypeManagerLock.readLock().lock();
            return propertyDefinitions.get(id);
        } finally {
            nodeTypeManagerLock.readLock().unlock();
        }
    }

    public JcrNodeDefinition getChildNodeDefinition( NodeDefinitionId id ) {
        try {
            nodeTypeManagerLock.readLock().lock();
            return childNodeDefinitions.get(id);
        } finally {
            nodeTypeManagerLock.readLock().unlock();
        }
    }

    NodeTypeSchemata getRepositorySchemata() {
        try {
            nodeTypeManagerLock.writeLock().lock();
            if (schemata == null) {
                schemata = new NodeTypeSchemata(context, nodeTypes, propertyDefinitions.values(),
                                                includeColumnsForInheritedProperties, includePseudoColumnsInSelectStar);
            }
            return schemata;
        } finally {
            nodeTypeManagerLock.writeLock().unlock();
        }
    }

    void signalNamespaceChanges() {
        try {
            nodeTypeManagerLock.writeLock().lock();
            schemata = null;
        } finally {
            nodeTypeManagerLock.writeLock().unlock();
        }
        this.schemata = null;
    }

    JcrNodeType getNodeType( Name nodeTypeName ) {
        try {
            nodeTypeManagerLock.readLock().lock();
            return nodeTypes.get(nodeTypeName);
        } finally {
            nodeTypeManagerLock.readLock().unlock();
        }
    }

    /**
     * Tests if the named node type is registered.
     * <p>
     * The return value of this method is equivalent to {@code getNodeType(nodeTypeName) != null}, although the implementation is
     * marginally more efficient that this approach.
     * </p>
     * 
     * @param nodeTypeName the name of the node type to check
     * @return true if a node type with the given name is registered, false otherwise
     */
    boolean hasNodeType( Name nodeTypeName ) {
        try {
            nodeTypeManagerLock.readLock().lock();
            return nodeTypes.containsKey(nodeTypeName);
        } finally {
            nodeTypeManagerLock.readLock().unlock();
        }
    }

    /**
     * Searches the supplied primary node type and the mixin node types for a property definition that is the best match for the
     * given property name, property type, and value.
     * <p>
     * This method first attempts to find a single-valued property definition with the supplied property name and
     * {@link Value#getType() value's property type} in the primary type, skipping any property definitions that are protected.
     * The property definition is returned if it has a matching type (or has an {@link PropertyType#UNDEFINED undefined property
     * type}) and the value satisfies the {@link PropertyDefinition#getValueConstraints() definition's constraints}. Otherwise,
     * the process continues with each of the mixin types, in the order they are named.
     * </p>
     * <p>
     * If no matching property definition could be found (and <code>checkMultiValuedDefinitions</code> parameter is
     * <code>true</code>), the process is repeated except with multi-valued property definitions with the same name, property
     * type, and compatible constraints, starting with the primary type and continuing with each mixin type.
     * </p>
     * <p>
     * If no matching property definition could be found, and the process repeats by searching the primary type (and then mixin
     * types) for single-valued property definitions with a compatible type, where the values can be safely cast to the
     * definition's property type and still satisfy the definition's constraints.
     * </p>
     * <p>
     * If no matching property definition could be found, the previous step is repeated with multi-valued property definitions.
     * </p>
     * <p>
     * If no matching property definition could be found (and the supplied property name is not the residual name), the whole
     * process repeats for residual property definitions (e.g., those that are defined with a {@link JcrNodeType#RESIDUAL_NAME "*"
     * name}).
     * </p>
     * <p>
     * Finally, if no satisfactory property definition could be found, this method returns null.
     * </p>
     * 
     * @param primaryTypeName the name of the primary type; may not be null
     * @param mixinTypeNames the names of the mixin types; may be null or empty if there are no mixins to include in the search
     * @param propertyName the name of the property for which the definition should be retrieved. This method will automatically
     *        look for residual definitions, but you can use {@link JcrNodeType#RESIDUAL_ITEM_NAME} to retrieve only the best
     *        residual property definition (if any).
     * @param value the value, or null if the property is being removed
     * @param checkMultiValuedDefinitions true if the type's multi-valued property definitions should be considered, or false if
     *        only single-value property definitions should be considered
     * @param skipProtected true if this operation is being done from within the public JCR node and property API, or false if
     *        this operation is being done from within internal implementations
     * @return the best property definition, or <code>null</code> if no property definition allows the property with the supplied
     *         name, type and number of values
     */
    JcrPropertyDefinition findPropertyDefinition( Name primaryTypeName,
                                                  List<Name> mixinTypeNames,
                                                  Name propertyName,
                                                  Value value,
                                                  boolean checkMultiValuedDefinitions,
                                                  boolean skipProtected ) {
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
                if ((type == PropertyType.UNDEFINED || type == value.getType()) && definition.satisfiesConstraints(value)) return definition;
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
                        if (definition.canCastToTypeAndSatisfyConstraints(value)) return definition;
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
                        if ((type == PropertyType.UNDEFINED || type == value.getType()) && definition.satisfiesConstraints(value)) return definition;
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
                            if (definition.canCastToTypeAndSatisfyConstraints(value)) return definition;
                        }
                    }
                }
                return null;
            }
        }

        // Look for a single-value property definition on the mixin types that matches by name and type ...
        List<JcrNodeType> mixinTypes = null;
        if (mixinTypeNames != null && !mixinTypeNames.isEmpty()) {
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
                    if ((type == PropertyType.UNDEFINED || type == value.getType()) && definition.satisfiesConstraints(value)) return definition;
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
                            if (definition.canCastToTypeAndSatisfyConstraints(value)) return definition;
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
                            if ((type == PropertyType.UNDEFINED || type == value.getType())
                                && definition.satisfiesConstraints(value)) return definition;
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
                                if (definition.canCastToTypeAndSatisfyConstraints(value)) return definition;

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
                    if ((type == PropertyType.UNDEFINED || type == value.getType()) && definition.satisfiesConstraints(value)) return definition;
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
                        if (definition.canCastToTypeAndSatisfyConstraints(value)) return definition;
                    }
                }
            }

            if (matchedOnName) return null;

            if (mixinTypeNames != null && !mixinTypeNames.isEmpty()) {
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
                        if ((type == PropertyType.UNDEFINED || type == value.getType()) && definition.satisfiesConstraints(value)) return definition;
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
                            if (definition.canCastToTypeAndSatisfyConstraints(value)) return definition;
                        }
                    }
                }
            }
            if (matchedOnName) return null;

        }

        // Nothing was found, so look for residual property definitions ...
        if (!propertyName.equals(JcrNodeType.RESIDUAL_NAME)) return findPropertyDefinition(primaryTypeName,
                                                                                           mixinTypeNames,
                                                                                           JcrNodeType.RESIDUAL_NAME,
                                                                                           value,
                                                                                           checkMultiValuedDefinitions,
                                                                                           skipProtected);
        return null;
    }

    /**
     * Searches the supplied primary node type and the mixin node types for a property definition that is the best match for the
     * given property name, property type, and value.
     * <p>
     * This method first attempts to find a single-valued property definition with the supplied property name and
     * {@link Value#getType() value's property type} in the primary type, skipping any property definitions that are protected.
     * The property definition is returned if it has a matching type (or has an {@link PropertyType#UNDEFINED undefined property
     * type}) and the value satisfies the {@link PropertyDefinition#getValueConstraints() definition's constraints}. Otherwise,
     * the process continues with each of the mixin types, in the order they are named.
     * </p>
     * <p>
     * If no matching property definition could be found (and <code>checkMultiValuedDefinitions</code> parameter is
     * <code>true</code>), the process is repeated except with multi-valued property definitions with the same name, property
     * type, and compatible constraints, starting with the primary type and continuing with each mixin type.
     * </p>
     * <p>
     * If no matching property definition could be found, and the process repeats by searching the primary type (and then mixin
     * types) for single-valued property definitions with a compatible type, where the values can be safely cast to the
     * definition's property type and still satisfy the definition's constraints.
     * </p>
     * <p>
     * If no matching property definition could be found, the previous step is repeated with multi-valued property definitions.
     * </p>
     * <p>
     * If no matching property definition could be found (and the supplied property name is not the residual name), the whole
     * process repeats for residual property definitions (e.g., those that are defined with a {@link JcrNodeType#RESIDUAL_NAME "*"
     * name}).
     * </p>
     * <p>
     * Finally, if no satisfactory property definition could be found, this method returns null.
     * </p>
     * 
     * @param primaryTypeName the name of the primary type; may not be null
     * @param mixinTypeNames the names of the mixin types; may be null or empty if there are no mixins to include in the search
     * @param propertyName the name of the property for which the definition should be retrieved. This method will automatically
     *        look for residual definitions, but you can use {@link JcrNodeType#RESIDUAL_ITEM_NAME} to retrieve only the best
     *        residual property definition (if any).
     * @param values the values
     * @param skipProtected true if this operation is being done from within the public JCR node and property API, or false if
     *        this operation is being done from within internal implementations
     * @return the best property definition, or <code>null</code> if no property definition allows the property with the supplied
     *         name, type and number of values
     */
    JcrPropertyDefinition findPropertyDefinition( Name primaryTypeName,
                                                  List<Name> mixinTypeNames,
                                                  Name propertyName,
                                                  Value[] values,
                                                  boolean skipProtected ) {
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
                if (typeMatches && type == PropertyType.REFERENCE) return definition;
                if (typeMatches && type == PropertyType.WEAKREFERENCE) return definition;
                if (typeMatches && definition.satisfiesConstraints(values)) return definition;
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
                        if (definition.canCastToTypeAndSatisfyConstraints(values)) return definition;
                    }
                }

                return null;
            }
        }

        // Look for a multi-value property definition on the mixin types that matches by name and type ...
        List<JcrNodeType> mixinTypes = null;
        if (mixinTypeNames != null && !mixinTypeNames.isEmpty()) {
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
                    if (typeMatches && type == PropertyType.REFERENCE) return definition;
                    if (typeMatches && type == PropertyType.WEAKREFERENCE) return definition;
                    if (typeMatches && definition.satisfiesConstraints(values)) return definition;
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
                            if (definition.canCastToTypeAndSatisfyConstraints(values)) return definition;
                        }
                    }

                    return null;
                }

            }
        }

        // Nothing was found, so look for residual property definitions ...
        if (!propertyName.equals(JcrNodeType.RESIDUAL_NAME)) return findPropertyDefinition(primaryTypeName,
                                                                                           mixinTypeNames,
                                                                                           JcrNodeType.RESIDUAL_NAME,
                                                                                           values,
                                                                                           skipProtected);
        return null;
    }

    /**
     * Searches the supplied primary and mixin node types for all valid property definitions that match the given property name
     * and cardinality.
     * <p>
     * If no satisfactory property definition could be found, this method returns an empty list.
     * </p>
     * 
     * @param typeNamesToCheck the name of the types to check; may not be null
     * @param propertyName the name of the property for which the definitions should be retrieved
     * @param typeToCheck the type of definitions to consider (single-valued only, multi-valued only, or all)
     * @param pendingTypes a list of types that have been created during type registration but not yet registered in the type map
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

            matchingDefs.addAll(propDefs);
        }

        return matchingDefs;
    }

    /**
     * Determine if the property definitions of the supplied primary type and mixin types allow the property with the supplied
     * name to be removed.
     * 
     * @param primaryTypeNameOfParent the name of the primary type for the parent node; may not be null
     * @param mixinTypeNamesOfParent the names of the mixin types for the parent node; may be null or empty if there are no mixins
     *        to include in the search
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
     * @param mixinTypeNamesOfParent the names of the mixin types for the parent node; may be null or empty if there are no mixins
     *        to include in the search
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

    /**
     * Searches the supplied primary node type and the mixin node types of a parent node for a child node definition that is the
     * best match for a new child with the given name, primary node type name, and whether there are existing children with the
     * same name.
     * 
     * @param primaryTypeNameOfParent the name of the primary type for the parent node; may not be null
     * @param mixinTypeNamesOfParent the names of the mixin types for the parent node; may be null or empty if there are no mixins
     *        to include in the search
     * @param childName the name of the child to be added to the parent; may not be null
     * @param childPrimaryNodeType the name of the primary node type for the child node, or null if the primary type is not known
     *        and the {@link NodeDefinition#getDefaultPrimaryType() definition's default primary type} will be used
     * @param numberOfExistingChildrenWithSameName the number of existing children with the same name as the child to be added, or
     *        0 if this new child will be the first child with this name (or if the number of children is not known)
     * @param skipProtected true if this operation is being done from within the public JCR node and property API, or false if
     *        this operation is being done from within internal implementations
     * @return the best child node definition, or <code>null</code> if no node definition allows a new child with the supplied
     *         name, primary type, and whether there are already children with the same name
     */
    JcrNodeDefinition findChildNodeDefinition( Name primaryTypeNameOfParent,
                                               List<Name> mixinTypeNamesOfParent,
                                               Name childName,
                                               Name childPrimaryNodeType,
                                               int numberOfExistingChildrenWithSameName,
                                               boolean skipProtected ) {
        JcrNodeType childType = childPrimaryNodeType != null ? getNodeType(childPrimaryNodeType) : null;
        boolean requireSns = numberOfExistingChildrenWithSameName > 1;

        // First look in the primary type ...
        JcrNodeType primaryType = getNodeType(primaryTypeNameOfParent);
        if (primaryType != null) {
            for (JcrNodeDefinition definition : primaryType.allChildNodeDefinitions(childName, requireSns)) {
                // Skip protected definitions ...
                if (skipProtected && definition.isProtected()) return null;
                // See if the definition allows a child with the supplied primary type ...
                if (definition.allowsChildWithType(childType)) return definition;
            }
        }

        // Then, look in the mixin types ...
        if (mixinTypeNamesOfParent != null && !mixinTypeNamesOfParent.isEmpty()) {
            for (Name mixinTypeName : mixinTypeNamesOfParent) {
                JcrNodeType mixinType = getNodeType(mixinTypeName);
                if (mixinType == null) continue;
                for (JcrNodeDefinition definition : mixinType.allChildNodeDefinitions(childName, requireSns)) {
                    // Skip protected definitions ...
                    if (skipProtected && definition.isProtected()) return null;
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
     * @param pendingTypes a list of types that have been created during type registration but not yet registered in the type map
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
     * @param mixinTypeNamesOfParent the names of the mixin types for the parent node; may be null or empty if there are no mixins
     *        to include in the search
     * @param childName the name of the child to be added to the parent; may not be null
     * @param skipProtected true if this operation is being done from within the public JCR node and property API, or false if
     *        this operation is being done from within internal implementations
     * @return true if at least one child node definition does not require children with the supplied name to exist, or false
     *         otherwise
     */
    boolean canRemoveAllChildren( Name primaryTypeNameOfParent,
                                  List<Name> mixinTypeNamesOfParent,
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
        if (mixinTypeNamesOfParent != null && !mixinTypeNamesOfParent.isEmpty()) {
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
     * Projects the node types onto the provided graph under the location of <code>parentOfTypeNodes</code>. If no node currently
     * exists at that path, one will be created and assigned a primary type of {@code ModeShapeLexicon.NODE_TYPES}.
     * <p>
     * All node creation is performed through the graph layer. If the primary type of the node at <code>parentOfPathNodes</code>
     * does not contain a residual definition that allows child nodes of type <code>nt:nodeType</code>, this method will create
     * nodes for which the JCR layer cannot determine the corresponding node definition. This WILL corrupt the graph from a JCR
     * standpoint and make it unusable through the ModeShape JCR layer.
     * </p>
     * <p>
     * For each node type, a node is created as a child node of <code>parentOfPathNodes</code>. The created node has a name that
     * corresponds to the node types name and a primary type of <code>nt:nodeType</code>. All other properties and child nodes for
     * the newly created node are added in a manner consistent with the guidance provided in section 6.7.22 of the JCR 1.0
     * specification and section 4.7.24 of the JCR 2.0 specification where possible.
     * </p>
     * 
     * @param graph the graph onto which the type information should be projected
     * @param parentOfTypeNodes the path under which the type information should be projected
     */
    void projectOnto( Graph graph,
                      Path parentOfTypeNodes ) {
        assert graph != null;
        assert parentOfTypeNodes != null;

        // Make sure that the parent of the type nodes exists in the graph.
        try {
            graph.getNodeAt(parentOfTypeNodes);
        } catch (PathNotFoundException pnfe) {
            PropertyFactory propertyFactory = context.getPropertyFactory();
            graph.create(parentOfTypeNodes,
                         propertyFactory.create(JcrLexicon.PRIMARY_TYPE,
                                                ModeShapeLexicon.NODE_TYPES.getString(context.getNamespaceRegistry()))).and();
        }

        Graph.Batch batch = graph.batch();

        for (JcrNodeType nodeType : getAllNodeTypes()) {
            projectNodeTypeOnto(nodeType, parentOfTypeNodes, batch);
        }

        batch.execute();
    }

    /**
     * Projects the node types onto the provided graph under the location of <code>parentOfTypeNodes</code>. The operations needed
     * to create the node (and any child nodes or properties) will be added to the batch specified in <code>batch</code>.
     * 
     * @param nodeType the node type to be projected
     * @param parentOfTypeNodes the path under which the type information should be projected
     * @param batch the batch to which any required graph modification operations should be added
     * @see #projectOnto(Graph, Path)
     */
    private void projectNodeTypeOnto( JcrNodeType nodeType,
                                      Path parentOfTypeNodes,
                                      Graph.Batch batch ) {
        assert nodeType != null;
        assert parentOfTypeNodes != null;
        assert batch != null;

        Path nodeTypePath = pathFactory.create(parentOfTypeNodes, nodeType.getInternalName());

        NodeType[] supertypes = nodeType.getDeclaredSupertypes();
        List<Name> supertypeNames = new ArrayList<Name>(supertypes.length);
        for (int i = 0; i < supertypes.length; i++) {
            supertypeNames.add(((JcrNodeType)supertypes[i]).getInternalName());
        }

        List<Property> propsList = new ArrayList<Property>();
        propsList.add(propertyFactory.create(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.NODE_TYPE));
        propsList.add(propertyFactory.create(JcrLexicon.IS_MIXIN, nodeType.isMixin()));
        propsList.add(propertyFactory.create(JcrLexicon.IS_ABSTRACT, nodeType.isAbstract()));
        propsList.add(propertyFactory.create(JcrLexicon.IS_QUERYABLE, nodeType.isQueryable()));

        if (nodeType.getPrimaryItemName() != null) {
            propsList.add(propertyFactory.create(JcrLexicon.PRIMARY_ITEM_NAME, nodeType.getPrimaryItemName()));
        }

        propsList.add(propertyFactory.create(JcrLexicon.NODE_TYPE_NAME, nodeType.getName()));
        propsList.add(propertyFactory.create(JcrLexicon.HAS_ORDERABLE_CHILD_NODES, nodeType.hasOrderableChildNodes()));
        propsList.add(propertyFactory.create(JcrLexicon.SUPERTYPES, supertypeNames));

        batch.create(nodeTypePath).with(propsList).orReplace().and();

        PropertyDefinition[] propertyDefs = nodeType.getDeclaredPropertyDefinitions();
        for (int i = 0; i < propertyDefs.length; i++) {
            projectPropertyDefinitionOnto(propertyDefs[i], nodeTypePath, batch);
        }

        NodeDefinition[] childNodeDefs = nodeType.getDeclaredChildNodeDefinitions();
        for (int i = 0; i < childNodeDefs.length; i++) {
            projectChildNodeDefinitionOnto(childNodeDefs[i], nodeTypePath, batch);
        }
    }

    /**
     * Projects a single property definition onto the provided graph under the location of <code>nodeTypePath</code>. The
     * operations needed to create the property definition and any of its properties will be added to the batch specified in
     * <code>batch</code>.
     * <p>
     * All node creation is performed through the graph layer. If the primary type of the node at <code>nodeTypePath</code> does
     * not contain a residual definition that allows child nodes of type <code>nt:propertyDefinition</code>, this method creates
     * nodes for which the JCR layer cannot determine the corresponding node definition. This WILL corrupt the graph from a JCR
     * standpoint and make it unusable through the ModeShape JCR layer.
     * </p>
     * 
     * @param propertyDef the property definition to be projected
     * @param nodeTypePath the path under which the property definition should be projected
     * @param batch the batch to which any required graph modification operations should be added
     * @see #projectOnto(Graph, Path)
     */
    private void projectPropertyDefinitionOnto( PropertyDefinition propertyDef,
                                                Path nodeTypePath,
                                                Graph.Batch batch ) {
        assert propertyDef != null;
        assert nodeTypePath != null;
        assert batch != null;

        JcrPropertyDefinition jcrPropDef = (JcrPropertyDefinition)propertyDef;
        String propName = jcrPropDef.getInternalName().getString(context.getNamespaceRegistry(), NAME_ENCODER);
        Path propDefPath = pathFactory.create(nodeTypePath, JcrLexicon.PROPERTY_DEFINITION);

        List<Property> propsList = new ArrayList<Property>();
        propsList.add(propertyFactory.create(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.PROPERTY_DEFINITION));

        if (!JcrNodeType.RESIDUAL_ITEM_NAME.equals(jcrPropDef.getName())) {
            propsList.add(propertyFactory.create(JcrLexicon.NAME, propName));
        }
        propsList.add(propertyFactory.create(JcrLexicon.AUTO_CREATED, jcrPropDef.isAutoCreated()));
        propsList.add(propertyFactory.create(JcrLexicon.MANDATORY, jcrPropDef.isMandatory()));
        propsList.add(propertyFactory.create(JcrLexicon.MULTIPLE, jcrPropDef.isMultiple()));
        propsList.add(propertyFactory.create(JcrLexicon.PROTECTED, jcrPropDef.isProtected()));
        propsList.add(propertyFactory.create(JcrLexicon.ON_PARENT_VERSION,
                                             OnParentVersionAction.nameFromValue(jcrPropDef.getOnParentVersion())));
        propsList.add(propertyFactory.create(JcrLexicon.REQUIRED_TYPE, PropertyType.nameFromValue(jcrPropDef.getRequiredType())
                                                                                   .toUpperCase()));

        List<String> symbols = new ArrayList<String>();
        for (String value : jcrPropDef.getAvailableQueryOperators()) {
            if (value != null) symbols.add(value);
        }
        propsList.add(propertyFactory.create(JcrLexicon.QUERY_OPERATORS, symbols));

        Value[] defaultValues = jcrPropDef.getDefaultValues();
        if (defaultValues.length > 0) {
            String[] defaultsAsString = new String[defaultValues.length];

            for (int i = 0; i < defaultValues.length; i++) {
                try {
                    defaultsAsString[i] = defaultValues[i].getString();
                } catch (RepositoryException re) {
                    // Really shouldn't get here as all values are convertible to string
                    throw new IllegalStateException(re);
                }
            }
            propsList.add(propertyFactory.create(JcrLexicon.DEFAULT_VALUES, (Object[])defaultsAsString));
        }

        String[] valueConstraints = jcrPropDef.getValueConstraints();
        if (valueConstraints.length > 0) {
            propsList.add(propertyFactory.create(JcrLexicon.VALUE_CONSTRAINTS, (Object[])valueConstraints));
        }
        batch.create(propDefPath).with(propsList).and();
    }

    /**
     * Projects a single child node definition onto the provided graph under the location of <code>nodeTypePath</code>. The
     * operations needed to create the child node definition and any of its properties will be added to the batch specified in
     * <code>batch</code>.
     * <p>
     * All node creation is performed through the graph layer. If the primary type of the node at <code>nodeTypePath</code> does
     * not contain a residual definition that allows child nodes of type <code>nt:childNodeDefinition</code>, this method creates
     * nodes for which the JCR layer cannot determine the corresponding node definition. This WILL corrupt the graph from a JCR
     * standpoint and make it unusable through the ModeShape JCR layer.
     * </p>
     * 
     * @param childNodeDef the child node definition to be projected
     * @param nodeTypePath the path under which the child node definition should be projected
     * @param batch the batch to which any required graph modification operations should be added
     * @see #projectOnto(Graph, Path)
     */
    private void projectChildNodeDefinitionOnto( NodeDefinition childNodeDef,
                                                 Path nodeTypePath,
                                                 Graph.Batch batch ) {
        assert childNodeDef != null;
        assert nodeTypePath != null;
        assert batch != null;

        JcrNodeDefinition jcrNodeDef = (JcrNodeDefinition)childNodeDef;
        String nodeName = jcrNodeDef.getInternalName().getString(context.getNamespaceRegistry(), NAME_ENCODER);
        Path nodeDefPath = pathFactory.create(nodeTypePath, JcrLexicon.CHILD_NODE_DEFINITION);

        List<Property> propsList = new ArrayList<Property>();
        propsList.add(propertyFactory.create(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.CHILD_NODE_DEFINITION));

        if (!JcrNodeType.RESIDUAL_ITEM_NAME.equals(jcrNodeDef.getName())) {
            propsList.add(propertyFactory.create(JcrLexicon.NAME, nodeName));
        }

        if (jcrNodeDef.getDefaultPrimaryType() != null) {
            propsList.add(propertyFactory.create(JcrLexicon.DEFAULT_PRIMARY_TYPE, jcrNodeDef.getDefaultPrimaryType().getName()));
        }

        propsList.add(propertyFactory.create(JcrLexicon.REQUIRED_PRIMARY_TYPES, (Object[])jcrNodeDef.requiredPrimaryTypeNames()));
        propsList.add(propertyFactory.create(JcrLexicon.SAME_NAME_SIBLINGS, jcrNodeDef.allowsSameNameSiblings()));
        propsList.add(propertyFactory.create(JcrLexicon.ON_PARENT_VERSION,
                                             OnParentVersionAction.nameFromValue(jcrNodeDef.getOnParentVersion())));
        propsList.add(propertyFactory.create(JcrLexicon.AUTO_CREATED, jcrNodeDef.isAutoCreated()));
        propsList.add(propertyFactory.create(JcrLexicon.MANDATORY, jcrNodeDef.isMandatory()));
        propsList.add(propertyFactory.create(JcrLexicon.PROTECTED, jcrNodeDef.isProtected()));

        batch.create(nodeDefPath).with(propsList).and();
    }

    /**
     * Allows the collection of node types to be unregistered if they are not referenced by other node types as supertypes,
     * default primary types of child nodes, or required primary types of child nodes.
     * <p>
     * <b>NOTE: This method does not check to see if any of the node types are currently being used. Unregistering a node type
     * that is being used will cause the system to become unstable</b>
     * </p>
     * 
     * @param nodeTypeNames the names of the node types to be unregistered
     * @throws NoSuchNodeTypeException if any of the node type names do not correspond to a registered node type
     * @throws InvalidNodeTypeDefinitionException if any of the node types with the given names cannot be unregistered because
     *         they are the supertype, one of the required primary types, or a default primary type of a node type that is not
     *         being unregistered.
     * @throws RepositoryException if any other error occurs
     */
    void unregisterNodeType( Collection<Name> nodeTypeNames )
        throws NoSuchNodeTypeException, InvalidNodeTypeDefinitionException, RepositoryException {
        CheckArg.isNotNull(nodeTypeNames, "nodeTypeNames");
        try {
            /*
             * Grab an exclusive lock on this data to keep other nodes from being added/saved while the unregistration checks are occurring
             */
            nodeTypeManagerLock.writeLock().lock();

            for (Name nodeTypeName : nodeTypeNames) {
                /*
                 * Check that the type names are valid
                 */
                if (nodeTypeName == null) {
                    throw new NoSuchNodeTypeException(JcrI18n.invalidNodeTypeName.text());
                }
                String name = nodeTypeName.getString(context.getNamespaceRegistry());

                if (!this.nodeTypes.containsKey(nodeTypeName)) {
                    throw new NoSuchNodeTypeException(JcrI18n.noSuchNodeType.text(name));
                }

                /*
                 * Check that no other node definitions have dependencies on any of the named types
                 */
                for (JcrNodeType nodeType : nodeTypes.values()) {
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

                    /*
                     * Search the content graph to make sure that this type isn't being used
                     */
                    if (isNodeTypeInUse(nodeTypeName)) {
                        throw new InvalidNodeTypeDefinitionException(JcrI18n.cannotUnregisterInUseType.text(name));

                    }

                }
            }

            this.nodeTypes.keySet().removeAll(nodeTypeNames);

            if (nodeTypesPath != null) {
                Graph.Batch batch = repository.createSystemGraph(context).batch();

                for (Name nodeTypeName : nodeTypeNames) {
                    Path nodeTypePath = pathFactory.create(nodeTypesPath, nodeTypeName);
                    batch.delete(nodeTypePath).and();
                }

                batch.execute();
            }
            this.schemata = null;

        } finally {
            nodeTypeManagerLock.writeLock().unlock();
        }
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

        Set<String> workspaceNames = repository.workspaceNames();
        for (String workspaceName : workspaceNames) {
            QueryResults result = repository.queryManager().query(workspaceName, command, schemata, null, null);

            if (result.getRowCount() > 0) {
                return true;
            }
        }

        return false;
    }

    /**
     * Registers a new node type or updates an existing node type using the specified definition and returns the resulting {@code
     * NodeType} object.
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
        assert ntd != null;
        List<JcrNodeType> result = registerNodeTypes(Collections.singletonList(ntd));
        return result.isEmpty() ? null : result.get(0);
    }

    /**
     * Registers or updates the specified {@link NodeTypeDefinition} objects.
     * <p>
     * For details, see {@link #registerNodeTypes(Iterable)}.
     * </p>
     * 
     * @param ntds the node type definitions
     * @return the newly registered (or updated) {@link NodeType NodeTypes}
     * @throws InvalidNodeTypeDefinitionException
     * @throws NodeTypeExistsException
     * @throws RepositoryException
     */
    List<JcrNodeType> registerNodeTypes( NodeTypeDefinition[] ntds )
        throws InvalidNodeTypeDefinitionException, NodeTypeExistsException, RepositoryException {
        assert ntds != null;
        return registerNodeTypes(Arrays.asList(ntds));
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

        if (nodeTypeDefns == null) {
            return Collections.emptyList();
        }

        List<JcrNodeType> typesPendingRegistration = new ArrayList<JcrNodeType>();

        try {
            nodeTypeManagerLock.writeLock().lock();
            for (NodeTypeDefinition nodeTypeDefn : nodeTypeDefns) {
                if (nodeTypeDefn instanceof JcrNodeTypeTemplate) {
                    // Switch to use this context, so names are properly prefixed ...
                    nodeTypeDefn = ((JcrNodeTypeTemplate)nodeTypeDefn).with(context);
                }
                Name internalName = nameFactory.create(nodeTypeDefn.getName());
                if (internalName == null || internalName.getLocalName().length() == 0) {
                    throw new InvalidNodeTypeDefinitionException(JcrI18n.invalidNodeTypeName.text());
                }

                if (nodeTypes.containsKey(internalName)) {
                    String name = nodeTypeDefn.getName();
                    throw new NodeTypeExistsException(internalName, JcrI18n.nodeTypeAlreadyExists.text(name));
                }

                List<JcrNodeType> supertypes = supertypesFor(nodeTypeDefn, typesPendingRegistration);
                JcrNodeType nodeType = nodeTypeFrom(nodeTypeDefn, supertypes);
                validate(nodeType, supertypes, typesPendingRegistration);

                typesPendingRegistration.add(nodeType);
            }

            // Make sure the nodes have primary types that are either already registered, or pending registration ...
            for (JcrNodeType nodeType : typesPendingRegistration) {
                for (JcrNodeDefinition nodeDef : nodeType.getDeclaredChildNodeDefinitions()) {
                    JcrNodeType[] requiredPrimaryTypes = new JcrNodeType[nodeDef.requiredPrimaryTypeNames().length];
                    int i = 0;
                    for (Name primaryTypeName : nodeDef.requiredPrimaryTypeNames()) {
                        requiredPrimaryTypes[i] = findTypeInMapOrList(primaryTypeName, typesPendingRegistration);

                        if (requiredPrimaryTypes[i] == null) {
                            throw new RepositoryException(
                                                          JcrI18n.invalidPrimaryTypeName.text(primaryTypeName, nodeType.getName()));
                        }
                        i++;
                    }
                }
            }

            Graph.Batch batch = null;
            if (nodeTypesPath != null) batch = repository.createSystemGraph(context).batch();

            for (JcrNodeType nodeType : typesPendingRegistration) {
                /*
                 * See comment in constructor.  Using a ConcurrentHashMap seems to be to weak of a
                 * solution (even it were also used for childNodeDefinitions and propertyDefinitions).
                 * Probably need to block all read access to these maps during this phase of registration.
                 */
                Name name = nodeType.getInternalName();
                nodeTypes.put(name, nodeType);
                for (JcrNodeDefinition childDefinition : nodeType.childNodeDefinitions()) {
                    childNodeDefinitions.put(childDefinition.getId(), childDefinition);
                }
                for (JcrPropertyDefinition propertyDefinition : nodeType.propertyDefinitions()) {
                    propertyDefinitions.put(propertyDefinition.getId(), propertyDefinition);
                }

                if (nodeTypesPath != null) projectNodeTypeOnto(nodeType, nodeTypesPath, batch);
            }

            // Throw away the schemata, since the node types have changed ...
            this.schemata = null;
            if (nodeTypesPath != null) {
                assert batch != null;
                batch.execute();
            }
        } finally {
            nodeTypeManagerLock.writeLock().unlock();
        }

        return typesPendingRegistration;
    }

    /**
     * Registers the node types from the given subgraph containing the node type definitions in the standard ModeShape format.
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
     * @param nodeTypeSubgraph the subgraph containing the of {@link NodeType node types} to register
     * @param locationOfParentOfNodeTypes the location of the parent node under which the node types are found
     * @return the newly registered (or updated) {@link NodeType NodeTypes}
     * @throws UnsupportedRepositoryOperationException if {@code allowUpdates == true}. ModeShape does not support this capability
     *         at this time but the parameter has been retained for API compatibility.
     * @throws InvalidNodeTypeDefinitionException if the {@link NodeTypeDefinition} is invalid
     * @throws NodeTypeExistsException if <code>allowUpdate</code> is false and the {@link NodeTypeDefinition} specifies a node
     *         type name that is already registered
     * @throws RepositoryException if another error occurs
     */
    List<JcrNodeType> registerNodeTypes( Subgraph nodeTypeSubgraph,
                                         Location locationOfParentOfNodeTypes )
        throws InvalidNodeTypeDefinitionException, NodeTypeExistsException, RepositoryException {
        assert nodeTypeSubgraph != null;
        assert locationOfParentOfNodeTypes != null;
        CndNodeTypeReader factory = new CndNodeTypeReader(this.context);
        factory.read(nodeTypeSubgraph, locationOfParentOfNodeTypes, nodeTypeSubgraph.getGraph().getSourceName());
        return registerNodeTypes(factory);
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

        return new JcrNodeType(this.context, this, name, supertypes, primaryItemName, childNodes, properties, mixin, isAbstract,
                               queryable, orderableChildNodes);
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
        if (defaultValues != null) {
            for (int i = 0; i != defaultValues.length; ++i) {
                Value value = defaultValues[i];
                defaultValues[i] = new JcrValue(this.context.getValueFactories(), null, value);
            }
        } else {
            defaultValues = new Value[0];
        }

        String[] valueConstraints = propDefn.getValueConstraints();
        String[] queryOperators = propDefn.getAvailableQueryOperators();
        if (valueConstraints == null) valueConstraints = new String[0];
        return new JcrPropertyDefinition(this.context, null, propertyName, onParentVersionBehavior, autoCreated, mandatory,
                                         isProtected, defaultValues, requiredType, valueConstraints, multiple,
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

        return new JcrNodeDefinition(this.context, null, childNodeName, onParentVersion, autoCreated, mandatory, isProtected,
                                     allowsSns, defaultPrimaryTypeName, requiredTypes);
    }

    /**
     * Finds the named type in the given collection of types pending registration if it exists, else returns the type definition
     * from the repository
     * 
     * @param typeName the name of the type to retrieve
     * @param pendingList a collection of types that have passed validation but have not yet been committed to the repository
     * @return the node type with the given name from {@code pendingList} if it exists in the collection or from the
     *         {@link #nodeTypes registered types} if it exists there; may be null
     */
    private JcrNodeType findTypeInMapOrList( Name typeName,
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
     * @param pendingTypes the list of types that have been processed in this type batch but not yet committed to the repository's
     *        set of types
     * @return a list of node types where each element is the node type for the corresponding element of the array of supertype
     *         names
     * @throws RepositoryException if any of the names in the array of supertype names does not correspond to an
     *         already-registered node type or a node type that is pending registration
     */
    private List<JcrNodeType> supertypesFor( NodeTypeDefinition nodeType,
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
                throw new InvalidNodeTypeDefinitionException(JcrI18n.invalidSupertypeName.text(supertypeNameStr, nodeTypeName));
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
        CheckArg.isNotNull(nodeType, "nodeType");

        try {
            nodeTypeManagerLock.readLock().lock();

            List<JcrNodeType> subtypes = new LinkedList<JcrNodeType>();
            for (JcrNodeType type : this.nodeTypes.values()) {
                if (type.supertypes().contains(nodeType)) {
                    subtypes.add(type);
                }
            }

            return subtypes;
        } finally {
            nodeTypeManagerLock.readLock().unlock();
        }
    }

    /**
     * Returns the list of declared subtypes for the given node.
     * 
     * @param nodeType the node type for which declared subtypes should be returned; may not be null
     * @return the subtypes for the node
     */
    final Collection<JcrNodeType> declaredSubtypesFor( JcrNodeType nodeType ) {
        CheckArg.isNotNull(nodeType, "nodeType");

        try {
            nodeTypeManagerLock.readLock().lock();

            String nodeTypeName = nodeType.getName();
            List<JcrNodeType> subtypes = new LinkedList<JcrNodeType>();
            for (JcrNodeType type : this.nodeTypes.values()) {
                if (Arrays.asList(type.getDeclaredSupertypeNames()).contains(nodeTypeName)) {
                    subtypes.add(type);
                }
            }

            return subtypes;
        } finally {
            nodeTypeManagerLock.readLock().unlock();
        }
    }

    /**
     * Validates that the supertypes are compatible under ModeShape restrictions.
     * <p>
     * ModeShape imposes the following rules on the supertypes of a type:
     * <ol>
     * <li>The type must have at least one supertype (unless the type is {@code nt:base}.</li>
     * <li>No two supertypes {@code t1} and {@code t2} can declare each declare a property ({@code p1} and {@code p2}) with the
     * same name and cardinality ({@code p1.isMultiple() == p2.isMultiple()}). Note that this does prohibit each {@code t1} and
     * {@code t2} from having a common supertype (or super-supertype, etc.) that declares a property).</li>
     * <li>No two supertypes {@code t1} and {@code t2} can declare each declare a child node ({@code n1} and {@code n2}) with the
     * same name and SNS status ({@code p1.allowsSameNameSiblings() == p2.allowsSameNameSiblings()}). Note that this does prohibit
     * each {@code t1} and {@code t2} from having a common supertype (or super-supertype, etc.) that declares a child node).</li>
     * </ol>
     * </p>
     * <p>
     * If any of these rules are violated, a {@link RepositoryException} is thrown.
     * </p>
     * 
     * @param supertypes the supertypes of this node type
     * @param nodeName the name of the node for which the supertypes are being validated.
     * @throws RepositoryException if any of the rules described above are violated
     */
    private void validate( List<JcrNodeType> supertypes,
                           String nodeName ) throws RepositoryException {
        assert supertypes != null;

        Map<PropertyDefinitionId, JcrPropertyDefinition> props = new HashMap<PropertyDefinitionId, JcrPropertyDefinition>();

        for (JcrNodeType supertype : supertypes) {
            for (JcrPropertyDefinition property : supertype.propertyDefinitions()) {
                JcrPropertyDefinition oldProp = props.put(new PropertyDefinitionId(property.getInternalName(),
                                                                                   property.getInternalName(),
                                                                                   PropertyType.UNDEFINED, property.isMultiple()),
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
     * See {@link #registerNodeTypes(Iterable)} for the list of criteria that determine whether a node type definition is valid.
     * </p>
     * 
     * @param nodeType the node type to attempt to validate
     * @param supertypes the names of the supertypes of the node type to which this child node belongs
     * @param pendingTypes the list of types previously registered in this batch but not yet committed to the repository
     * @throws RepositoryException if the given node type template is not valid
     */
    private void validate( JcrNodeType nodeType,
                           List<JcrNodeType> supertypes,
                           List<JcrNodeType> pendingTypes ) throws RepositoryException {
        Name nodeTypeName = nodeType.getInternalName();
        validate(supertypes, nodeTypeName.getString(this.context.getNamespaceRegistry()));

        List<Name> supertypeNames = new ArrayList<Name>(supertypes.size());
        for (JcrNodeType supertype : supertypes) {
            supertypeNames.add(supertype.getInternalName());
        }

        boolean foundExact = false;
        boolean foundResidual = false;
        Name primaryItemName = nodeType.getInternalPrimaryItemName();

        for (JcrNodeDefinition node : nodeType.getDeclaredChildNodeDefinitions()) {
            validate(node, supertypeNames, pendingTypes);
            if (node.isResidual()) foundResidual = true;

            if (primaryItemName != null && primaryItemName.equals(node.getInternalName())) {
                foundExact = true;
            }
        }

        for (JcrPropertyDefinition prop : nodeType.getDeclaredPropertyDefinitions()) {
            validate(prop, supertypeNames, pendingTypes);
            if (prop.isResidual()) foundResidual = true;
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
     * Validates that the given child node definition is valid under the ModeShape and JCR type rules within the given context.
     * <p>
     * ModeShape considers a child node definition valid if it meets these criteria:
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
     * </p>
     * 
     * @param node the child node definition to be validated
     * @param supertypes the names of the supertypes of the node type to which this child node belongs
     * @param pendingTypes the list of types previously registered in this batch but not yet committed to the repository
     * @throws RepositoryException if the child node definition is not valid
     */
    private void validate( JcrNodeDefinition node,
                           List<Name> supertypes,
                           List<JcrNodeType> pendingTypes ) throws RepositoryException {
        if (node.isAutoCreated() && !node.isProtected() && node.defaultPrimaryTypeName() == null) {
            throw new InvalidNodeTypeDefinitionException(JcrI18n.autocreatedNodesNeedDefaults.text(node.getName()));
        }
        if (node.isMandatory() && JcrNodeType.RESIDUAL_ITEM_NAME.equals(node.getName())) {
            throw new InvalidNodeTypeDefinitionException(JcrI18n.residualDefinitionsCannotBeMandatory.text("child nodes"));
        }

        Name nodeName = context.getValueFactories().getNameFactory().create(node.getName());
        nodeName = nodeName == null ? JcrNodeType.RESIDUAL_NAME : nodeName;

        List<JcrNodeDefinition> ancestors = findChildNodeDefinitions(supertypes, nodeName, NodeCardinality.ANY, pendingTypes);

        for (JcrNodeDefinition ancestor : ancestors) {
            if (ancestor.isProtected()) {
                throw new InvalidNodeTypeDefinitionException(
                                                             JcrI18n.cannotOverrideProtectedDefinition.text(ancestor.getDeclaringNodeType()
                                                                                                                    .getName(),
                                                                                                            "child node"));
            }

            if (ancestor.isMandatory() && !node.isMandatory()) {
                throw new InvalidNodeTypeDefinitionException(
                                                             JcrI18n.cannotMakeMandatoryDefinitionOptional.text(ancestor.getDeclaringNodeType()
                                                                                                                        .getName(),
                                                                                                                "child node"));

            }

            Name[] requiredPrimaryTypeNames = ancestor.requiredPrimaryTypeNames();
            for (int i = 0; i < requiredPrimaryTypeNames.length; i++) {
                NodeType apt = findTypeInMapOrList(requiredPrimaryTypeNames[i], pendingTypes);

                if (apt == null) {
                    I18n msg = JcrI18n.couldNotFindDefinitionOfRequiredPrimaryType;
                    throw new InvalidNodeTypeDefinitionException(msg.text(requiredPrimaryTypeNames[i],
                                                                          node.getName(),
                                                                          node.getDeclaringNodeType()));

                }

                boolean found = false;

                for (Name name : node.requiredPrimaryTypeNames()) {
                    JcrNodeType npt = findTypeInMapOrList(name, pendingTypes);

                    if (npt.isNodeType(apt.getName())) {
                        found = true;
                        break;
                    }
                }

                // Allow side-by-side definitions of residual child nodes per JCR 1.0.1 spec 6.7.8
                if (!found && !JcrNodeType.RESIDUAL_NAME.equals(node.name)) {
                    I18n msg = JcrI18n.cannotRedefineChildNodeWithIncompatibleDefinition;
                    throw new InvalidNodeTypeDefinitionException(msg.text(nodeName, apt.getName(), node.getDeclaringNodeType()));

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
     * <li>If the property overrides an existing property definition from a supertype, the new definition must be mandatory if the
     * old definition was mandatory</li>
     * <li>The property cannot override an existing property definition from a supertype if the ancestor definition is protected</li>
     * <li>If the property overrides an existing property definition from a supertype, the new definition must have the same
     * required type as the old definition or a required type that can ALWAYS be cast to the required type of the ancestor (see
     * section 3.6.4 of the JCR 2.0 specification)</li>
     * </ol>
     * Note that an empty set of properties would meet the criteria above.
     * </p>
     * 
     * @param prop the property definition to be validated
     * @param supertypes the names of the supertypes of the node type to which this property belongs
     * @param pendingTypes the list of types previously registered in this batch but not yet committed to the repository
     * @throws RepositoryException if the property definition is not valid
     */
    private void validate( JcrPropertyDefinition prop,
                           List<Name> supertypes,
                           List<JcrNodeType> pendingTypes ) throws RepositoryException {
        assert prop != null;
        assert supertypes != null;
        assert pendingTypes != null;

        if (prop.isMandatory() && !prop.isProtected() && JcrNodeType.RESIDUAL_ITEM_NAME.equals(prop.getName())) {
            throw new InvalidNodeTypeDefinitionException(JcrI18n.residualDefinitionsCannotBeMandatory.text("properties"));
        }

        Value[] defaultValues = prop.getDefaultValues();
        if (prop.isAutoCreated() && !prop.isProtected() && (defaultValues == null || defaultValues.length == 0)) {
            throw new InvalidNodeTypeDefinitionException(JcrI18n.autocreatedPropertyNeedsDefault.text(prop.getName(),
                                                                                                      prop.getDeclaringNodeType()
                                                                                                          .getName()));
        }

        if (!prop.isMultiple() && (defaultValues != null && defaultValues.length > 1)) {
            throw new InvalidNodeTypeDefinitionException(
                                                         JcrI18n.singleValuedPropertyNeedsSingleValuedDefault.text(prop.getName(),
                                                                                                                   prop.getDeclaringNodeType()
                                                                                                                       .getName()));
        }

        Name propName = context.getValueFactories().getNameFactory().create(prop.getName());
        propName = propName == null ? JcrNodeType.RESIDUAL_NAME : propName;

        List<JcrPropertyDefinition> ancestors = findPropertyDefinitions(supertypes,
                                                                        propName,
                                                                        prop.isMultiple() ? PropertyCardinality.MULTI_VALUED_ONLY : PropertyCardinality.SINGLE_VALUED_ONLY,
                                                                        pendingTypes);

        for (JcrPropertyDefinition ancestor : ancestors) {
            if (ancestor.isProtected()) {
                throw new InvalidNodeTypeDefinitionException(
                                                             JcrI18n.cannotOverrideProtectedDefinition.text(ancestor.getDeclaringNodeType()
                                                                                                                    .getName(),
                                                                                                            "property"));
            }

            if (ancestor.isMandatory() && !prop.isMandatory()) {
                throw new InvalidNodeTypeDefinitionException(
                                                             JcrI18n.cannotMakeMandatoryDefinitionOptional.text(ancestor.getDeclaringNodeType()
                                                                                                                        .getName(),
                                                                                                                "property"));

            }

            // TODO: It would be nice if we could allow modification of constraints if the new constraints were more strict than
            // the old
            if (ancestor.getValueConstraints() != null
                && !Arrays.equals(ancestor.getValueConstraints(), prop.getValueConstraints())) {
                throw new InvalidNodeTypeDefinitionException(
                                                             JcrI18n.constraintsChangedInSubtype.text(propName,
                                                                                                      ancestor.getDeclaringNodeType()
                                                                                                              .getName()));
            }

            if (!isAlwaysSafeConversion(prop.getRequiredType(), ancestor.getRequiredType())) {
                throw new InvalidNodeTypeDefinitionException(
                                                             JcrI18n.cannotRedefineProperty.text(propName,
                                                                                                 PropertyType.nameFromValue(prop.getRequiredType()),
                                                                                                 ancestor.getDeclaringNodeType()
                                                                                                         .getName(),
                                                                                                 PropertyType.nameFromValue(ancestor.getRequiredType())));

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
    public Path getObservedPath() {
        return this.nodeTypesPath;
    }

    @Override
    public void notify( Changes changes ) {
        Collection<Name> createdNodeTypeNames = new HashSet<Name>();
        Collection<Name> deletedNodeTypeNames = new HashSet<Name>();

        for (ChangeRequest change : changes.getChangeRequests()) {
            assert change.changedLocation().hasPath();

            Path changedPath = change.changedLocation().getPath();
            if (changedPath.equals(nodeTypesPath)) {
                // nothing to do with the "/jcr:system/jcr:nodeTypes" node ...
                continue;
            }
            assert nodeTypesPath.isAncestorOf(changedPath);

            /*
             * The first segment under the node types root (as stored in nodeTypesPath) must be
             * the node type name.  
             */
            Path relativePath = changedPath.relativeTo(nodeTypesPath);
            Name changedNodeTypeName = relativePath.getSegment(0).getName();

            switch (change.getType()) {
                case CREATE_NODE:
                    /*
                     * Registering one node type can result in many CreateNodeRequests 
                     * being created (1 for the type, 1 for each property, 1 for each child node), but
                     * if anything is changed on the node type, the whole node type needs to be reloaded
                     * anyway.
                     *                     
                     */
                    if (!createdNodeTypeNames.contains(changedNodeTypeName)) {
                        createdNodeTypeNames.add(changedNodeTypeName);
                    }
                    break;

                case DELETE_BRANCH:
                    deletedNodeTypeNames.add(changedNodeTypeName);

                    break;
                default:
                    assert false : "Unexpected change request: " + change;
            }
        }

        if (createdNodeTypeNames.isEmpty() && deletedNodeTypeNames.isEmpty()) return;

        this.nodeTypeManagerLock.writeLock().lock();
        try {
            GraphNodeTypeReader reader = new GraphNodeTypeReader(this.context);
            Graph systemGraph = repository.createSystemGraph(this.context);

            reader.read(systemGraph, nodeTypesPath, createdNodeTypeNames, null);

            Problems readerProblems = reader.getProblems();
            if (readerProblems.hasProblems()) {
                if (readerProblems.hasErrors()) {
                    LOGGER.error(JcrI18n.errorReadingNodeTypesFromRemote, reader.getProblems());
                    return;
                }

                LOGGER.warn(JcrI18n.problemReadingNodeTypesFromRemote, reader.getProblems());
            }

            Map<Name, JcrNodeType> newNodeTypeMap = new HashMap<Name, JcrNodeType>();
            try {
                for (NodeTypeDefinition nodeTypeDefn : reader.getNodeTypeDefinitions()) {
                    List<JcrNodeType> supertypes = supertypesFor(nodeTypeDefn, newNodeTypeMap.values());
                    JcrNodeType nodeType = nodeTypeFrom(nodeTypeDefn, supertypes);

                    newNodeTypeMap.put(nodeType.getInternalName(), nodeType);
                }
            } catch (Throwable re) {
                LOGGER.error(JcrI18n.errorSynchronizingNodeTypes, re);
            }

            assert this.nodeTypes.get(ModeShapeLexicon.ROOT) != null;
            assert !deletedNodeTypeNames.contains(ModeShapeLexicon.ROOT);

            nodeTypes.keySet().removeAll(deletedNodeTypeNames);
            this.nodeTypes.putAll(newNodeTypeMap);

            assert this.nodeTypes.get(ModeShapeLexicon.ROOT) != null;

            for (JcrSession activeSession : repository.activeSessions()) {
                JcrWorkspace workspace = activeSession.workspace();
                if (workspace == null) continue;

                JcrNodeTypeManager nodeTypeManager = workspace.nodeTypeManager();
                if (nodeTypeManager == null) continue;

                nodeTypeManager.signalExternalNodeTypeChanges();
            }

        } finally {
            this.schemata = null;
            this.nodeTypeManagerLock.writeLock().unlock();
        }

    }
}
