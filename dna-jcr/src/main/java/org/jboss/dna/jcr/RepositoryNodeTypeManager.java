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
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
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
        boolean requireSns = numberOfExistingChildrenWithSameName > 1;

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
}
