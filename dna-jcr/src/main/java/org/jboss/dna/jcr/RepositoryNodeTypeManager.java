/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in JBoss DNA is licensed
 * to you under the terms of the GNU Lesser General Public License as
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.OnParentVersionAction;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.text.TextEncoder;
import org.jboss.dna.common.text.XmlNameEncoder;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.property.PathNotFoundException;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.property.PropertyFactory;
import org.jboss.dna.jcr.JcrNodeTypeSource;

/**
 * The {@link RepositoryNodeTypeManager} is the maintainer of node type information for the entire repository at run-time. The
 * repository manager maintains a list of all node types and the ability to retrieve node types by {@link Name}. </p> The JCR 1.0
 * and 2.0 specifications both require that node type information be shared across all sessions within a repository and that the
 * {@link javax.jcr.nodetype.NodeTypeManager} perform operations based on the string versions of {@link Name}s based on the
 * permanent (workspace-scoped) and transient (session-scoped) namespace mappings. DNA achieves this by maintaining a single
 * master repository of all node type information (the {@link RepositoryNodeTypeManager}) and per-session wrappers (
 * {@link JcrNodeTypeManager}) for this master repository that perform {@link String} to {@link Name} translation based on the
 * {@link javax.jcr.Session}'s transient mappings and then delegating node type lookups to the repository manager.
 */
@Immutable
class RepositoryNodeTypeManager {

    private static final TextEncoder NAME_ENCODER = new XmlNameEncoder();

    private final ExecutionContext context;
    private final Map<Name, JcrNodeType> nodeTypes;
    private final Map<PropertyDefinitionId, JcrPropertyDefinition> propertyDefinitions;
    private final Map<NodeDefinitionId, JcrNodeDefinition> childNodeDefinitions;
    private final PropertyFactory propertyFactory;
    private final PathFactory pathFactory;

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
        SAME_NAME_SIBLINGS
    }

    RepositoryNodeTypeManager( ExecutionContext context,
                               JcrNodeTypeSource source ) {
        this.context = context;
        this.propertyFactory = context.getPropertyFactory();
        this.pathFactory = context.getValueFactories().getPathFactory();

        Collection<JcrNodeType> types = source.getNodeTypes();
        propertyDefinitions = new HashMap<PropertyDefinitionId, JcrPropertyDefinition>();
        childNodeDefinitions = new HashMap<NodeDefinitionId, JcrNodeDefinition>();

        nodeTypes = new HashMap<Name, JcrNodeType>(types.size());
        for (JcrNodeType nodeType : types) {
            nodeTypes.put(nodeType.getInternalName(), nodeType.with(this));
            for (JcrNodeDefinition childDefinition : nodeType.childNodeDefinitions()) {
                childNodeDefinitions.put(childDefinition.getId(), childDefinition);
            }
            for (JcrPropertyDefinition propertyDefinition : nodeType.propertyDefinitions()) {
                propertyDefinitions.put(propertyDefinition.getId(), propertyDefinition);
            }
        }
    }

    public Collection<JcrNodeType> getAllNodeTypes() {
        return nodeTypes.values();
    }

    public Collection<JcrNodeType> getMixinNodeTypes() {
        List<JcrNodeType> types = new ArrayList<JcrNodeType>(nodeTypes.size());

        for (JcrNodeType nodeType : nodeTypes.values()) {
            if (nodeType.isMixin()) types.add(nodeType);
        }

        return types;
    }

    public Collection<JcrNodeType> getPrimaryNodeTypes() {
        List<JcrNodeType> types = new ArrayList<JcrNodeType>(nodeTypes.size());

        for (JcrNodeType nodeType : nodeTypes.values()) {
            if (!nodeType.isMixin()) types.add(nodeType);
        }

        return types;
    }

    public JcrPropertyDefinition getPropertyDefinition( PropertyDefinitionId id ) {
        return propertyDefinitions.get(id);
    }

    public JcrNodeDefinition getChildNodeDefinition( NodeDefinitionId id ) {
        return childNodeDefinitions.get(id);
    }

    JcrNodeType getNodeType( Name nodeTypeName ) {
        return nodeTypes.get(nodeTypeName);
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

        // Look for a single-value property definition on the primary type that matches by name and type ...
        JcrNodeType primaryType = getNodeType(primaryTypeName);
        if (primaryType != null) {
            for (JcrPropertyDefinition definition : primaryType.allSingleValuePropertyDefinitions(propertyName)) {
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
                if ((type == PropertyType.UNDEFINED || type == value.getType()) && definition.satisfiesConstraints(value)) return definition;
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
                    if ((type == PropertyType.UNDEFINED || type == value.getType()) && definition.satisfiesConstraints(value)) return definition;
                }
            }
        }

        if (checkMultiValuedDefinitions) {
            // Look for a multi-value property definition on the primary type that matches by name and type ...
            if (primaryType != null) {
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
                    if ((type == PropertyType.UNDEFINED || type == value.getType()) && definition.satisfiesConstraints(value)) return definition;
                }
            }

            // Look for a multi-value property definition on the mixin types that matches by name and type ...
            if (mixinTypes != null) {
                for (JcrNodeType mixinType : mixinTypes) {
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
                        if ((type == PropertyType.UNDEFINED || type == value.getType()) && definition.satisfiesConstraints(value)) return definition;
                    }
                }
            }
        }

        if (value != null) {
            // Nothing was found with matching name and type, so look for definitions with
            // matching name and an undefined or castable type ...

            // Look for a single-value property definition on the primary type that matches by name ...
            if (primaryType != null) {
                for (JcrPropertyDefinition definition : primaryType.allSingleValuePropertyDefinitions(propertyName)) {
                    // See if the definition allows the value ...
                    if (skipProtected && definition.isProtected()) return null;
                    assert definition.getRequiredType() != PropertyType.UNDEFINED;
                    if (definition.canCastToTypeAndSatisfyConstraints(value)) return definition;
                }
            }

            // Look for a single-value property definition on the mixin types that matches by name ...
            if (mixinTypes != null) {
                for (JcrNodeType mixinType : mixinTypes) {
                    for (JcrPropertyDefinition definition : mixinType.allSingleValuePropertyDefinitions(propertyName)) {
                        // See if the definition allows the value ...
                        if (skipProtected && definition.isProtected()) return null;
                        assert definition.getRequiredType() != PropertyType.UNDEFINED;
                        if (definition.canCastToTypeAndSatisfyConstraints(value)) return definition;
                    }
                }
            }

            if (checkMultiValuedDefinitions) {
                // Look for a multi-value property definition on the primary type that matches by name ...
                if (primaryType != null) {
                    for (JcrPropertyDefinition definition : primaryType.allMultiValuePropertyDefinitions(propertyName)) {
                        // See if the definition allows the value ...
                        if (skipProtected && definition.isProtected()) return null;
                        assert definition.getRequiredType() != PropertyType.UNDEFINED;
                        if (definition.canCastToTypeAndSatisfyConstraints(value)) return definition;
                    }
                }

                // Look for a multi-value property definition on the mixin types that matches by name ...
                if (mixinTypes != null) {
                    for (JcrNodeType mixinType : mixinTypes) {
                        for (JcrPropertyDefinition definition : mixinType.allMultiValuePropertyDefinitions(propertyName)) {
                            // See if the definition allows the value ...
                            if (skipProtected && definition.isProtected()) return null;
                            assert definition.getRequiredType() != PropertyType.UNDEFINED;
                            if (definition.canCastToTypeAndSatisfyConstraints(value)) return definition;
                        }
                    }
                }
            }
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
        boolean setToEmpty = values == null || values.length == 0;
        int propertyType = values == null || values.length == 0 ? PropertyType.STRING : values[0].getType();

        // Look for a multi-value property definition on the primary type that matches by name and type ...
        JcrNodeType primaryType = getNodeType(primaryTypeName);
        if (primaryType != null) {
            for (JcrPropertyDefinition definition : primaryType.allMultiValuePropertyDefinitions(propertyName)) {
                // See if the definition allows the value ...
                if (skipProtected && definition.isProtected()) return null;
                if (setToEmpty) {
                    if (!definition.isMandatory()) return definition;
                    // Otherwise this definition doesn't work, so continue with the next ...
                    continue;
                }
                assert values != null;
                assert values.length != 0;
                // We can use the definition if it matches the type and satisfies the constraints ...
                int type = definition.getRequiredType();
                if ((type == PropertyType.UNDEFINED || type == propertyType) && definition.satisfiesConstraints(values)) return definition;
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
                    // See if the definition allows the value ...
                    if (skipProtected && definition.isProtected()) return null;
                    if (setToEmpty) {
                        if (!definition.isMandatory()) return definition;
                        // Otherwise this definition doesn't work, so continue with the next ...
                        continue;
                    }
                    assert values != null;
                    assert values.length != 0;
                    // We can use the definition if it matches the type and satisfies the constraints ...
                    int type = definition.getRequiredType();
                    if ((type == PropertyType.UNDEFINED || type == propertyType) && definition.satisfiesConstraints(values)) return definition;
                }
            }
        }

        if (values != null && values.length != 0) {
            // Nothing was found with matching name and type, so look for definitions with
            // matching name and an undefined or castable type ...

            // Look for a multi-value property definition on the primary type that matches by name and type ...
            if (primaryType != null) {
                for (JcrPropertyDefinition definition : primaryType.allMultiValuePropertyDefinitions(propertyName)) {
                    // See if the definition allows the value ...
                    if (skipProtected && definition.isProtected()) return null;
                    assert definition.getRequiredType() != PropertyType.UNDEFINED;
                    if (definition.canCastToTypeAndSatisfyConstraints(values)) return definition;
                }
            }

            // Look for a multi-value property definition on the mixin types that matches by name and type ...
            if (mixinTypes != null) {
                for (JcrNodeType mixinType : mixinTypes) {
                    for (JcrPropertyDefinition definition : mixinType.allMultiValuePropertyDefinitions(propertyName)) {
                        // See if the definition allows the value ...
                        if (skipProtected && definition.isProtected()) return null;
                        assert definition.getRequiredType() != PropertyType.UNDEFINED;
                        if (definition.canCastToTypeAndSatisfyConstraints(values)) return definition;
                    }
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
                if (!definition.isMandatory()) return true;
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
                    if (!definition.isMandatory()) return true;
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
                    nodeDefs = typeName.allChildNodeDefinitions(childNodeName);
                    break;
                case SAME_NAME_SIBLINGS:
                    nodeDefs = typeName.allChildNodeDefinitions(childNodeName, true);
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
                if (!definition.isMandatory()) return true;
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
                    if (!definition.isMandatory()) return true;
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
     * exists at that path, one will be created and assigned a primary type of {@code DnaLexicon.NODE_TYPES}.
     * <p>
     * All node creation is performed through the graph layer. If the primary type of the node at <code>parentOfPathNodes</code>
     * does not contain a residual definition that allows child nodes of type <code>nt:nodeType</code>, this method will create
     * nodes for which the JCR layer cannot determine the corresponding node definition. This WILL corrupt the graph from a JCR
     * standpoint and make it unusable through the DNA JCR layer.
     * </p>
     * <p>
     * For each node type, a node is created as a child node of <code>parentOfPathNodes</code>. The created node has a name that
     * corresponds to the node types name and a primary type of <code>nt:nodeType</code>. All other properties and child nodes for
     * the newly created node are added in a manner consistent with the guidance provide in section 6.7.22 of the JCR 1.0
     * specification and section 4.7.24 of the (draft) JCR 2.0 specification where possible.
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
                                                DnaLexicon.NODE_TYPES.getString(context.getNamespaceRegistry())));
        }

        Graph.Batch batch = graph.batch();

        for (JcrNodeType nodeType : nodeTypes.values()) {
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

        if (nodeType.getPrimaryItemName() != null) {
            propsList.add(propertyFactory.create(JcrLexicon.PRIMARY_ITEM_NAME, nodeType.getPrimaryItemName()));
        }

        propsList.add(propertyFactory.create(JcrLexicon.NODE_TYPE_NAME, nodeType.getName()));
        propsList.add(propertyFactory.create(JcrLexicon.HAS_ORDERABLE_CHILD_NODES, nodeType.hasOrderableChildNodes()));
        propsList.add(propertyFactory.create(JcrLexicon.SUPERTYPES, supertypeNames));

        batch.create(nodeTypePath).with(propsList).and();

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
     * standpoint and make it unusable through the DNA JCR layer.
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
        propsList.add(propertyFactory.create(JcrLexicon.REQUIRED_TYPE, PropertyType.nameFromValue(jcrPropDef.getRequiredType())));

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
            propsList.add(propertyFactory.create(JcrLexicon.DEFAULT_VALUES, (Object[])valueConstraints));
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
     * standpoint and make it unusable through the DNA JCR layer.
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

        propsList.add(propertyFactory.create(JcrLexicon.REQUIRED_PRIMARY_TYPES, jcrNodeDef.getRequiredPrimaryTypeNames()));
        propsList.add(propertyFactory.create(JcrLexicon.SAME_NAME_SIBLINGS, jcrNodeDef.allowsSameNameSiblings()));
        propsList.add(propertyFactory.create(JcrLexicon.ON_PARENT_VERSION,
                                             OnParentVersionAction.nameFromValue(jcrNodeDef.getOnParentVersion())));
        propsList.add(propertyFactory.create(JcrLexicon.AUTO_CREATED, jcrNodeDef.isAutoCreated()));
        propsList.add(propertyFactory.create(JcrLexicon.MANDATORY, jcrNodeDef.isMandatory()));
        propsList.add(propertyFactory.create(JcrLexicon.PROTECTED, jcrNodeDef.isProtected()));

        batch.create(nodeDefPath).with(propsList).and();
    }

    /**
     * Registers the node types from the given {@link JcrNodeTypeSource}.
     * <p>
     * The effect of this method is &quot;all or nothing&quot;; if an error occurs, no node types are registered or updated.
     * </p>
     * <p>
     * <b>DNA Implementation Notes</b>
     * </p>
     * <p>
     * DNA currently supports registration of batches of types with some constraints. DNA will allow types to be registered if
     * they meet the following criteria:
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
     * version of DNA.</i></li>
     * <li>If the property overrides an existing property definition from a supertype, the new definition must have the same
     * required type as the old definition or a required type that can ALWAYS be cast to the required type of the ancestor (see
     * section 6.2.6 of the JCR 1.0.1 specification)</li>
     * </ol>
     * Note that an empty set of properties would meet the above criteria.</li>
     * <li>The type must have a valid set of child nodes - that is, the types's child nodes must meet the following criteria:
     * <ol>
     * <li>Residual child node definitions cannot be mandatory</li>
     * <li>If the child node is auto-created, it must specify a default primary type name</li>
     * <li>All required primary types must already be fully registered with the type manager or must have been defined earlier in
     * the batch. <i>This requirement may be relaxed in a future version of DNA.</i></li>
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
     * @param nodeTypeSource the batch of {@link NodeType node types} to register
     * @return the newly registered (or updated) {@link NodeType NodeTypes}
     * @throws RepositoryException if any of the node types in the the {@link JcrNodeTypeSource} are invalid
     * @throws RepositoryException if another error occurs
     */
    List<JcrNodeType> registerNodeTypes( JcrNodeTypeSource nodeTypeSource ) throws RepositoryException {

        assert nodeTypeSource != null;
        Collection<JcrNodeType> nodeTypeBatch = nodeTypeSource.getNodeTypes();

        List<JcrNodeType> typesPendingRegistration = new ArrayList<JcrNodeType>(nodeTypeBatch.size());

        for (JcrNodeType nodeType : nodeTypeBatch) {
            if (nodeType.getInternalName() == null || nodeType.getName().length() == 0) {
                throw new RepositoryException(JcrI18n.invalidNodeTypeName.text());
            }

            Name name = nodeType.getInternalName();

            if (nodeTypes.containsKey(name)) {
                throw new RepositoryException(JcrI18n.nodeTypeAlreadyExists.text(nodeType.getName()));
            }

            List<JcrNodeType> supertypes = supertypesFor(nodeType, typesPendingRegistration);

            validate(nodeType, supertypes, typesPendingRegistration);

            List<JcrPropertyDefinition> propertyDefs = new ArrayList<JcrPropertyDefinition>(
                                                                                            nodeType.getDeclaredPropertyDefinitions().length);

            for (JcrPropertyDefinition propertyDef : nodeType.getDeclaredPropertyDefinitions()) {
                propertyDefs.add(propertyDef.with(this.context));
            }

            List<JcrNodeDefinition> nodeDefs = new ArrayList<JcrNodeDefinition>(nodeType.getDeclaredChildNodeDefinitions().length);

            for (JcrNodeDefinition nodeDef : nodeType.getDeclaredChildNodeDefinitions()) {
                JcrNodeType[] requiredPrimaryTypes = new JcrNodeType[nodeDef.getRequiredPrimaryTypeNames().size()];

                int i = 0;
                for (Name primaryTypeName : nodeDef.getRequiredPrimaryTypeNames()) {
                    requiredPrimaryTypes[i] = findTypeInMapOrList(primaryTypeName, typesPendingRegistration);

                    if (requiredPrimaryTypes[i] == null) {
                        throw new RepositoryException(JcrI18n.invalidPrimaryTypeName.text(primaryTypeName, nodeType.getName()));
                    }
                    i++;
                }

                nodeDefs.add(nodeDef.with(this.context));
            }

            JcrNodeType newNodeType = new JcrNodeType(this.context, this, name, supertypes,
                                                      nodeType.getInternalPrimaryItemName(), nodeDefs, propertyDefs,
                                                      nodeType.isMixin(), nodeType.hasOrderableChildNodes());
            typesPendingRegistration.add(newNodeType);
        }

        // Graph.Batch batch = graph.batch();
        for (JcrNodeType nodeType : typesPendingRegistration) {
            /*
             * See comment in constructor.  Using a ConcurrentHashMap seems to be to weak of a
             * solution (even it were also used for childNodeDefinitions and propertyDefinitions).
             * Probably need to block all read access to these maps during this phase of registration.
             */
            nodeTypes.put(nodeType.getInternalName(), nodeType);
            for (JcrNodeDefinition childDefinition : nodeType.childNodeDefinitions()) {
                childNodeDefinitions.put(childDefinition.getId(), childDefinition);
            }
            for (JcrPropertyDefinition propertyDefinition : nodeType.propertyDefinitions()) {
                propertyDefinitions.put(propertyDefinition.getId(), propertyDefinition);
            }

            // projectNodeTypeOnto(nodeType, parentOfTypeNodes, batch);
        }

        // batch.execute();

        return typesPendingRegistration;
    }

    /**
     * Finds the named type in the given list of types pending registration if it exists, else returns the type definition from
     * the repository
     * 
     * @param typeName the name of the type to retrieve
     * @param pendingList a list of types that have passed validation but have not yet been committed to the repository
     * @return the node type with the given name from {@code pendingList} if it exists in the list or from the {@link #nodeTypes
     *         registered types} if it exists there; may be null
     */
    private JcrNodeType findTypeInMapOrList( Name typeName,
                                             List<JcrNodeType> pendingList ) {
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
    private List<JcrNodeType> supertypesFor( JcrNodeType nodeType,
                                             List<JcrNodeType> pendingTypes ) throws RepositoryException {
        assert nodeType != null;

        // If no supertypes are provided, assume nt:base as a supertype
        if (nodeType.getDeclaredSupertypes() == null || nodeType.getDeclaredSupertypes().length == 0) {
            return Collections.<JcrNodeType>singletonList(nodeTypes.get(JcrNtLexicon.BASE));
        }

        JcrNodeType[] supertypesArray = nodeType.getDeclaredSupertypes();
        List<JcrNodeType> supertypes = new ArrayList<JcrNodeType>(supertypesArray.length);

        for (int i = 0; i < supertypesArray.length; i++) {
            supertypes.add(findTypeInMapOrList(supertypesArray[i].getInternalName(), pendingTypes));

            if (supertypes.get(i) == null) {
                throw new RepositoryException(JcrI18n.invalidSupertypeName.text(supertypesArray[i].getInternalName(),
                                                                                nodeType.getName()));
            }
        }

        return supertypes;
    }

    /**
     * Validates that the supertypes are compatible under DNA restrictions.
     * <p>
     * DNA imposes the following rules on the supertypes of a type:
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
        assert supertypes.size() > 0; // This is reasonable now that we default to having a supertype of nt:base

        Map<JcrPropertyDefinition.Key, JcrPropertyDefinition> props = new HashMap<JcrPropertyDefinition.Key, JcrPropertyDefinition>();

        for (JcrNodeType supertype : supertypes) {
            for (JcrPropertyDefinition property : supertype.propertyDefinitions()) {
                JcrPropertyDefinition oldProp = props.put(property.getKey(false), property);
                if (oldProp != null) {
                    String oldPropTypeName = oldProp.getDeclaringNodeType().getName();
                    String propTypeName = property.getDeclaringNodeType().getName();
                    if (!oldPropTypeName.equals(propTypeName)) {
                        throw new RepositoryException(JcrI18n.supertypesConflict.text(oldPropTypeName,
                                                                                      propTypeName,
                                                                                      "property",
                                                                                      property.getName()));
                    }
                }
            }
        }

        Map<JcrNodeDefinition.Key, JcrNodeDefinition> childNodes = new HashMap<JcrNodeDefinition.Key, JcrNodeDefinition>();

        for (JcrNodeType supertype : supertypes) {
            for (JcrNodeDefinition childNode : supertype.childNodeDefinitions()) {
                JcrNodeDefinition oldNode = childNodes.put(childNode.getKey(false), childNode);
                if (oldNode != null) {
                    String oldNodeTypeName = oldNode.getDeclaringNodeType().getName();
                    String childNodeTypeName = childNode.getDeclaringNodeType().getName();
                    if (!oldNodeTypeName.equals(childNodeTypeName)) {
                        throw new RepositoryException(JcrI18n.supertypesConflict.text(oldNodeTypeName,
                                                                                      childNodeTypeName,
                                                                                      "child node",
                                                                                      childNode.getName()));
                    }
                }
            }
        }
    }

    /**
     * Validates that the given node type definition is valid under the DNA and JCR type rules within the given context.
     * <p>
     * See {@link #registerNodeTypes(JcrNodeTypeSource)} for the list of criteria that determine whether a node type definition is
     * valid.
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
        validate(supertypes, nodeType.getName());

        List<Name> supertypeNames = new ArrayList<Name>(supertypes.size());
        for (JcrNodeType supertype : supertypes)
            supertypeNames.add(supertype.getInternalName());

        boolean found = false;
        String primaryItemName = nodeType.getPrimaryItemName();
        for (JcrNodeDefinition node : nodeType.getDeclaredChildNodeDefinitions()) {
            validate(node, supertypeNames, pendingTypes);

            if (primaryItemName != null && primaryItemName.equals(node.getName())) {
                found = true;
            }
        }

        for (JcrPropertyDefinition prop : nodeType.getDeclaredPropertyDefinitions()) {
            validate(prop, supertypeNames, pendingTypes);
            if (primaryItemName != null && primaryItemName.equals(prop.getName())) {
                if (found) {
                    throw new RepositoryException(JcrI18n.ambiguousPrimaryItemName.text(primaryItemName));
                }
                found = true;
            }
        }

        if (primaryItemName != null && !found) {
            throw new RepositoryException(JcrI18n.invalidPrimaryItemName.text(primaryItemName));
        }
    }

    /**
     * Validates that the given child node definition is valid under the DNA and JCR type rules within the given context.
     * <p>
     * DNA considers a child node definition valid if it meets these criteria:
     * <ol>
     * <li>Residual child node definitions cannot be mandatory</li>
     * <li>If the child node is auto-created, it must specify a default primary type name</li>
     * <li>All required primary types must already be fully registered with the type manager or must have been defined earlier in
     * the batch. This requirement may be relaxed in a future version of DNA.</li>
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
        if (node.isAutoCreated() && node.getDefaultPrimaryType() == null) {
            throw new RepositoryException(JcrI18n.autocreatedNodesNeedDefaults.text());
        }
        if (node.isMandatory() && node.getName() == null) {
            throw new RepositoryException(JcrI18n.residualDefinitionsCannotBeMandatory.text("child nodes"));
        }

        Name nodeName = context.getValueFactories().getNameFactory().create(node.getName());
        nodeName = nodeName == null ? JcrNodeType.RESIDUAL_NAME : nodeName;

        List<JcrNodeDefinition> ancestors = findChildNodeDefinitions(supertypes,
                                                                     nodeName,
                                                                     node.allowsSameNameSiblings() ? NodeCardinality.SAME_NAME_SIBLINGS : NodeCardinality.NO_SAME_NAME_SIBLINGS,
                                                                     pendingTypes);

        for (JcrNodeDefinition ancestor : ancestors) {
            if (ancestor.isProtected()) {
                throw new RepositoryException(
                                              JcrI18n.cannotOverrideProtectedDefinition.text(ancestor.getDeclaringNodeType().getName(),
                                                                                             "child node"));
            }

            if (ancestor.isMandatory() && !node.isMandatory()) {
                throw new RepositoryException(
                                              JcrI18n.cannotMakeMandatoryDefinitionOptional.text(ancestor.getDeclaringNodeType().getName(),
                                                                                                 "child node"));

            }

            for (int i = 0; i < ancestor.getRequiredPrimaryTypes().length; i++) {
                NodeType apt = ancestor.getRequiredPrimaryTypes()[i];
                boolean found = false;
                for (Name name : node.getRequiredPrimaryTypeNames()) {
                    JcrNodeType npt = findTypeInMapOrList(name, pendingTypes);

                    if (npt.isNodeType(apt.getName())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new RepositoryException(
                                                  "Cannot redefine child node '"
                                                  + nodeName
                                                  + "' with required type '"
                                                  + apt.getName()
                                                  + "' with new child node that does not required that type or a subtype of that type.");

                }
            }
        }
    }

    /**
     * Validates that the given property definition is valid under the DNA and JCR type rules within the given context.
     * <p>
     * DNA considers a property definition valid if it meets these criteria:
     * <ol>
     * <li>Residual properties cannot be mandatory</li>
     * <li>If the property is auto-created, it must specify a default value</li>
     * <li>If the property is single-valued, it can only specify a single default value</li>
     * <li>If the property overrides an existing property definition from a supertype, the new definition must be mandatory if the
     * old definition was mandatory</li>
     * <li>The property cannot override an existing property definition from a supertype if the ancestor definition is protected</li>
     * <li>If the property overrides an existing property definition from a supertype, the new definition must have the same
     * required type as the old definition or a required type that can ALWAYS be cast to the required type of the ancestor (see
     * section 6.2.6 of the JCR 1.0.1 specification)</li>
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

        if (prop.isMandatory() && !prop.isProtected() && prop.getName() == null) {
            throw new RepositoryException(JcrI18n.residualDefinitionsCannotBeMandatory.text("properties"));
        }

        Value[] defaultValues = prop.getDefaultValues();
        if (prop.isAutoCreated() && !prop.isProtected() && (defaultValues == null || defaultValues.length == 0)) {
            throw new RepositoryException(JcrI18n.autocreatedPropertyNeedsDefault.text(prop.getName(),
                                                                                       prop.getDeclaringNodeType().getName()));
        }

        if (!prop.isMultiple() && (defaultValues != null && defaultValues.length > 1)) {
            throw new RepositoryException(
                                          JcrI18n.singleValuedPropertyNeedsSingleValuedDefault.text(prop.getName(),
                                                                                                    prop.getDeclaringNodeType().getName()));
        }

        Name propName = context.getValueFactories().getNameFactory().create(prop.getName());
        propName = propName == null ? JcrNodeType.RESIDUAL_NAME : propName;

        List<JcrPropertyDefinition> ancestors = findPropertyDefinitions(supertypes,
                                                                        propName,
                                                                        prop.isMultiple() ? PropertyCardinality.MULTI_VALUED_ONLY : PropertyCardinality.SINGLE_VALUED_ONLY,
                                                                        pendingTypes);

        for (JcrPropertyDefinition ancestor : ancestors) {
            if (ancestor.isProtected()) {
                throw new RepositoryException(
                                              JcrI18n.cannotOverrideProtectedDefinition.text(ancestor.getDeclaringNodeType().getName(),
                                                                                             "property"));
            }

            if (ancestor.isMandatory() && !prop.isMandatory()) {
                throw new RepositoryException(
                                              JcrI18n.cannotMakeMandatoryDefinitionOptional.text(ancestor.getDeclaringNodeType().getName(),
                                                                                                 "property"));

            }

            // TODO: It would be nice if we could allow modification of constraints if the new constraints were more strict than
            // the old
            if (ancestor.getValueConstraints() != null
                && !Arrays.equals(ancestor.getValueConstraints(), prop.getValueConstraints())) {
                throw new RepositoryException(JcrI18n.constraintsChangedInSubtype.text(propName,
                                                                                       ancestor.getDeclaringNodeType().getName()));
            }

            if (!isAlwaysSafeConversion(prop.getRequiredType(), ancestor.getRequiredType())) {
                throw new RepositoryException(
                                              JcrI18n.cannotRedefineProperty.text(propName,
                                                                                  PropertyType.nameFromValue(prop.getRequiredType()),
                                                                                  ancestor.getDeclaringNodeType().getName(),
                                                                                  PropertyType.nameFromValue(ancestor.getRequiredType())));

            }
        }
    }

    /**
     * Returns whether it is always possible to convert a value with JCR property type {@code fromType} to {@code toType}.
     * <p>
     * This method is based on the conversions which can never throw an exception in the chart in section 6.2.6 of the JCR 1.0.1
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

}
