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

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.PropertyDefinition;
import net.jcip.annotations.Immutable;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.property.Name;

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

    private final Map<Name, JcrNodeType> primaryNodeTypes;
    private final Map<Name, JcrNodeType> mixinNodeTypes;
    private final Map<PropertyDefinitionId, JcrPropertyDefinition> propertyDefinitions;
    private final Map<NodeDefinitionId, JcrNodeDefinition> childNodeDefinitions;

    RepositoryNodeTypeManager( ExecutionContext context,
                               JcrNodeTypeSource source ) {
        Collection<JcrNodeType> primary = source.getPrimaryNodeTypes();
        Collection<JcrNodeType> mixins = source.getMixinNodeTypes();
        propertyDefinitions = new HashMap<PropertyDefinitionId, JcrPropertyDefinition>();
        childNodeDefinitions = new HashMap<NodeDefinitionId, JcrNodeDefinition>();

        primaryNodeTypes = new HashMap<Name, JcrNodeType>(primary.size());
        for (JcrNodeType nodeType : primary) {
            primaryNodeTypes.put(nodeType.getInternalName(), nodeType.with(this));
            for (JcrNodeDefinition childDefinition : nodeType.childNodeDefinitions()) {
                childNodeDefinitions.put(childDefinition.getId(), childDefinition);
            }
            for (JcrPropertyDefinition propertyDefinition : nodeType.propertyDefinitions()) {
                propertyDefinitions.put(propertyDefinition.getId(), propertyDefinition);
            }
        }

        mixinNodeTypes = new HashMap<Name, JcrNodeType>(mixins.size());
        for (JcrNodeType nodeType : mixins) {
            mixinNodeTypes.put(nodeType.getInternalName(), nodeType.with(this));
            for (JcrNodeDefinition childDefinition : nodeType.childNodeDefinitions()) {
                childNodeDefinitions.put(childDefinition.getId(), childDefinition);
            }
            for (JcrPropertyDefinition propertyDefinition : nodeType.propertyDefinitions()) {
                propertyDefinitions.put(propertyDefinition.getId(), propertyDefinition);
            }
        }
    }

    public Collection<JcrNodeType> getMixinNodeTypes() {
        return mixinNodeTypes.values();
    }

    public Collection<JcrNodeType> getPrimaryNodeTypes() {
        return primaryNodeTypes.values();
    }

    public JcrPropertyDefinition getPropertyDefinition( PropertyDefinitionId id ) {
        return propertyDefinitions.get(id);
    }

    public JcrNodeDefinition getChildNodeDefinition( NodeDefinitionId id ) {
        return childNodeDefinitions.get(id);
    }

    JcrNodeType getNodeType( Name nodeTypeName ) {

        JcrNodeType nodeType = primaryNodeTypes.get(nodeTypeName);
        if (nodeType == null) {
            nodeType = mixinNodeTypes.get(nodeTypeName);
        }
        return nodeType;
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
                if (skipProtected && definition.isProtected()) continue;
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
                    if (skipProtected && definition.isProtected()) continue;
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
                    if (skipProtected && definition.isProtected()) continue;
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
                        if (skipProtected && definition.isProtected()) continue;
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
                    if (skipProtected && definition.isProtected()) continue;
                    assert definition.getRequiredType() != PropertyType.UNDEFINED;
                    if (definition.canCastToTypeAndSatisfyConstraints(value)) return definition;
                }
            }

            // Look for a single-value property definition on the mixin types that matches by name ...
            if (mixinTypes != null) {
                for (JcrNodeType mixinType : mixinTypes) {
                    for (JcrPropertyDefinition definition : mixinType.allSingleValuePropertyDefinitions(propertyName)) {
                        // See if the definition allows the value ...
                        if (skipProtected && definition.isProtected()) continue;
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
                        if (skipProtected && definition.isProtected()) continue;
                        assert definition.getRequiredType() != PropertyType.UNDEFINED;
                        if (definition.canCastToTypeAndSatisfyConstraints(value)) return definition;
                    }
                }

                // Look for a multi-value property definition on the mixin types that matches by name ...
                if (mixinTypes != null) {
                    for (JcrNodeType mixinType : mixinTypes) {
                        for (JcrPropertyDefinition definition : mixinType.allMultiValuePropertyDefinitions(propertyName)) {
                            // See if the definition allows the value ...
                            if (skipProtected && definition.isProtected()) continue;
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
                if (skipProtected && definition.isProtected()) continue;
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
                    if (skipProtected && definition.isProtected()) continue;
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
                    if (skipProtected && definition.isProtected()) continue;
                    assert definition.getRequiredType() != PropertyType.UNDEFINED;
                    if (definition.canCastToTypeAndSatisfyConstraints(values)) return definition;
                }
            }

            // Look for a multi-value property definition on the mixin types that matches by name and type ...
            if (mixinTypes != null) {
                for (JcrNodeType mixinType : mixinTypes) {
                    for (JcrPropertyDefinition definition : mixinType.allMultiValuePropertyDefinitions(propertyName)) {
                        // See if the definition allows the value ...
                        if (skipProtected && definition.isProtected()) continue;
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
        boolean requireSns = numberOfExistingChildrenWithSameName != 0;

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
        if (mixinTypeNamesOfParent != null && !mixinTypeNamesOfParent.isEmpty()) {
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
}
