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
import java.util.List;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;
import net.jcip.annotations.Immutable;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.property.Name;

/**
 * Local implementation of @{link NodeTypeManager}. This class handles translation between {@link Name}s and {@link String}s based
 * on the namespace registry from the session's execution context in order to support transient namespace remappings. All
 * {@link NodeType}s returned by this implementation are wrapped with the execution context of the session to allow proper ongoing
 * handling of names. This implies that reference equality is not a safe test for node type equivalence.
 * 
 * @see RepositoryNodeTypeManager
 */
@Immutable
class JcrNodeTypeManager implements NodeTypeManager {

    private final ExecutionContext context;
    private final RepositoryNodeTypeManager repositoryTypeManager;

    JcrNodeTypeManager( ExecutionContext context,
                        RepositoryNodeTypeManager repositoryTypeManager ) {
        this.context = context;
        this.repositoryTypeManager = repositoryTypeManager;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeTypeManager#getAllNodeTypes()
     */
    public NodeTypeIterator getAllNodeTypes() {
        return new JcrNodeTypeIterator(repositoryTypeManager.getAllNodeTypes());
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeTypeManager#getMixinNodeTypes()
     */
    public NodeTypeIterator getMixinNodeTypes() {
        Collection<JcrNodeType> rawTypes = repositoryTypeManager.getMixinNodeTypes();
        List<JcrNodeType> types = new ArrayList<JcrNodeType>(rawTypes.size());

        // Need to return a version of the node type with the current context
        for (JcrNodeType type : rawTypes) {
            types.add(type.with(context));
        }

        return new JcrNodeTypeIterator(types);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeTypeManager#getNodeType(java.lang.String)
     */
    public JcrNodeType getNodeType( String nodeTypeName ) throws NoSuchNodeTypeException, RepositoryException {
        Name ntName = context.getValueFactories().getNameFactory().create(nodeTypeName);
        JcrNodeType type = repositoryTypeManager.getNodeType(ntName);
        if (type != null) {
            type = type.with(context);
            return type;
        }
        throw new NoSuchNodeTypeException(JcrI18n.typeNotFound.text(nodeTypeName));
    }

    /**
     * Returns the node type with the given name (if one exists)
     * 
     * @param nodeTypeName the name of the node type to be returned
     * @return the node type with the given name (if one exists)
     * @see RepositoryNodeTypeManager#getNodeType(Name)
     */
    JcrNodeType getNodeType( Name nodeTypeName ) {
        JcrNodeType nodeType = repositoryTypeManager.getNodeType(nodeTypeName);

        if (nodeType != null) {
            nodeType = nodeType.with(context);
        }

        return nodeType;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeTypeManager#getPrimaryNodeTypes()
     */
    public NodeTypeIterator getPrimaryNodeTypes() {
        Collection<JcrNodeType> rawTypes = repositoryTypeManager.getPrimaryNodeTypes();
        List<JcrNodeType> types = new ArrayList<JcrNodeType>(rawTypes.size());

        // Need to return a version of the node type with the current context
        for (JcrNodeType type : rawTypes) {
            types.add(type.with(context));
        }

        return new JcrNodeTypeIterator(types);
    }

    /**
     * Get the {@link NodeDefinition} for the root node.
     * 
     * @return the definition; never null
     * @throws RepositoryException
     * @throws NoSuchNodeTypeException
     */
    JcrNodeDefinition getRootNodeDefinition() throws NoSuchNodeTypeException, RepositoryException {
        for (NodeDefinition definition : repositoryTypeManager.getNodeType(DnaLexicon.ROOT).getChildNodeDefinitions()) {
            if (definition.getName().equals(JcrNodeType.RESIDUAL_ITEM_NAME)) return (JcrNodeDefinition)definition;
        }
        assert false; // should not get here
        return null;
    }

    /**
     * Get the node definition given the supplied identifier.
     * 
     * @param definitionId the identifier of the node definition
     * @return the node definition, or null if there is no such definition (or if the ID was null)
     */
    JcrNodeDefinition getNodeDefinition( NodeDefinitionId definitionId ) {
        if (definitionId == null) return null;
        return repositoryTypeManager.getChildNodeDefinition(definitionId);
    }

    /**
     * Get the property definition given the supplied identifier.
     * 
     * @param definitionId the identifier of the node definition
     * @return the property definition, or null if there is no such definition (or if the ID was null)
     */
    JcrPropertyDefinition getPropertyDefinition( PropertyDefinitionId definitionId ) {
        if (definitionId == null) return null;
        return repositoryTypeManager.getPropertyDefinition(definitionId);
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
    final JcrPropertyDefinition findPropertyDefinition( Name primaryTypeName,
                                                        List<Name> mixinTypeNames,
                                                        Name propertyName,
                                                        Value value,
                                                        boolean checkMultiValuedDefinitions,
                                                        boolean skipProtected ) {
        return repositoryTypeManager.findPropertyDefinition(primaryTypeName,
                                                            mixinTypeNames,
                                                            propertyName,
                                                            value,
                                                            checkMultiValuedDefinitions,
                                                            skipProtected);
    }

    /**
     * Searches the supplied primary node type and the mixin node types for a property definition that is the best match for the
     * given property name, property type, and value. with the given name and property type that allows the supplied values.
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
    final JcrPropertyDefinition findPropertyDefinition( Name primaryTypeName,
                                                        List<Name> mixinTypeNames,
                                                        Name propertyName,
                                                        Value[] values,
                                                        boolean skipProtected ) {
        return repositoryTypeManager.findPropertyDefinition(primaryTypeName, mixinTypeNames, propertyName, values, skipProtected);
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
        return repositoryTypeManager.canRemoveProperty(primaryTypeNameOfParent,
                                                       mixinTypeNamesOfParent,
                                                       propertyName,
                                                       skipProtected);
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
    final JcrNodeDefinition findChildNodeDefinition( Name primaryTypeNameOfParent,
                                                     List<Name> mixinTypeNamesOfParent,
                                                     Name childName,
                                                     Name childPrimaryNodeType,
                                                     int numberOfExistingChildrenWithSameName,
                                                     boolean skipProtected ) {
        return repositoryTypeManager.findChildNodeDefinition(primaryTypeNameOfParent,
                                                             mixinTypeNamesOfParent,
                                                             childName,
                                                             childPrimaryNodeType,
                                                             numberOfExistingChildrenWithSameName,
                                                             skipProtected);
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
    final boolean canRemoveAllChildren( Name primaryTypeNameOfParent,
                                        List<Name> mixinTypeNamesOfParent,
                                        Name childName,
                                        boolean skipProtected ) {
        return repositoryTypeManager.canRemoveAllChildren(primaryTypeNameOfParent,
                                                          mixinTypeNamesOfParent,
                                                          childName,
                                                          skipProtected);
    }

}
