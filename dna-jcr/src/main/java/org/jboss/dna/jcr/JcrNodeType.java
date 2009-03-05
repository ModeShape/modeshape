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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.ValueFactories;
import org.jboss.dna.graph.property.ValueFormatException;
import org.jboss.dna.graph.property.Path.Segment;

/**
 * DNA implementation of JCR {@link NodeType}s.
 */
@Immutable
class JcrNodeType implements NodeType {

    public static final String RESIDUAL_ITEM_NAME = "*";

    /** The name of the node type (e.g., <code>{http://www.jcp.org/jcr/nt/1.0}base</code>) */
    private final Name name;
    /** The name of the node's primary item */
    private final Name primaryItemName;

    /** The set of child node definitions for nodes of this type (possibly empty). */
    private final Set<JcrNodeDefinition> childNodeDefinitions;
    /** The set of property definitions for nodes of this type (possibly empty). */
    private final Set<JcrPropertyDefinition> propertyDefinitions;
    /** The supertypes for this node. */
    private final List<NodeType> declaredSupertypes;

    /** Indicates whether this node type is a mixin type (as opposed to a primary type). */
    private boolean mixin;
    /** Indicates whether the child nodes of nodes of this type can be ordered. */
    private boolean orderableChildNodes;

    /**
     * A reference to the session in which this node type exists, used to remap the internal names to their appropriate prefixed
     * version (e.g., <code>{http://www.jcp.org/jcr/nt/1.0}base</code> to <code>&quot;nt:base&quot;</code>.).
     */
    private JcrSession session;

    JcrNodeType( JcrSession session,
                 Name name,
                 List<NodeType> declaredSupertypes,
                 Name primaryItemName,
                 Collection<JcrNodeDefinition> childNodeDefinitions,
                 Collection<JcrPropertyDefinition> propertyDefinitions,
                 boolean mixin,
                 boolean orderableChildNodes ) {
        this.session = session;
        this.name = name;
        this.primaryItemName = primaryItemName;
        this.declaredSupertypes = declaredSupertypes != null ? declaredSupertypes : Collections.<NodeType>emptyList();
        this.mixin = mixin;
        this.orderableChildNodes = orderableChildNodes;
        this.propertyDefinitions = new HashSet<JcrPropertyDefinition>(propertyDefinitions.size());
        for (JcrPropertyDefinition property : propertyDefinitions) {
            this.propertyDefinitions.add(property.with(this));
        }

        this.childNodeDefinitions = new HashSet<JcrNodeDefinition>(childNodeDefinitions.size());
        for (JcrNodeDefinition childNode : childNodeDefinitions) {
            this.childNodeDefinitions.add(childNode.with(this));
        }
    }

    /**
     * Returns the property definition with the given name. This method first checks the property definitions declared within this
     * type to see if any property definitions have the given name. If no matches are found, this method initiates a recursive
     * depth first search up the type hierarchy to attempt to find a definition in one of the supertypes (or one the supertypes of
     * the supertypes).
     * 
     * @param propertyName the name of the property for which the definition should be retrieved. Use
     *        {@link JcrNodeType#RESIDUAL_ITEM_NAME} to retrieve the residual property definition (if any).
     * @return the property definition for the given name or <code>null</code> if no such definition exists.
     * @see JcrNodeType#RESIDUAL_ITEM_NAME
     */
    JcrPropertyDefinition getPropertyDefinition( String propertyName ) {
        for (JcrPropertyDefinition property : propertyDefinitions) {
            if (propertyName.equals(property.getName())) {
                return property;
            }
        }

        for (NodeType nodeType : declaredSupertypes) {
            JcrPropertyDefinition definition = ((JcrNodeType)nodeType).getPropertyDefinition(propertyName);
            if (definition != null) return definition;
        }
        return null;
    }

    /**
     * Returns the node definition for the child node with the given name. This method first checks the child node definitions
     * declared within this type to see if any child node definitions have the given name. If no matches are found, this method
     * initiates a recursive depth first search up the type hierarchy to attempt to find a definition in one of the supertypes (or
     * one the supertypes of the supertypes).
     * 
     * @param childNodeName the name of the child node for which the definition should be retrieved. Use
     *        {@link JcrNodeType#RESIDUAL_ITEM_NAME} to retrieve the residual child node definition (if any).
     * @return the child node definition with the given name or <code>null</code> if no such definition exists.
     * @see JcrNodeType#RESIDUAL_ITEM_NAME
     */
    JcrNodeDefinition getChildNodeDefinition( String childNodeName ) {
        for (JcrNodeDefinition childNode : childNodeDefinitions) {
            if (childNodeName.equals(childNode.getName())) {
                return childNode;
            }
        }

        for (NodeType nodeType : declaredSupertypes) {
            JcrNodeDefinition definition = ((JcrNodeType)nodeType).getChildNodeDefinition(childNodeName);
            if (definition != null) return definition;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeType#canAddChildNode(java.lang.String)
     */
    public boolean canAddChildNode( String childNodeName ) {

        CheckArg.isNotNull(childNodeName, "childNodeName");

        // First, try to find a child node definition with the given name
        JcrNodeDefinition childNode = getChildNodeDefinition(childNodeName);

        // If there are no named definitions in the type hierarchy, try to find a residual node definition
        if (childNode == null) {
            childNode = getChildNodeDefinition(RESIDUAL_ITEM_NAME);
        }

        if (childNode != null) {
            NodeType defaultType = childNode.getDefaultPrimaryType();
            // If there's no default type, the child node can't be created
            if (defaultType == null) {
                return false;
            }

            // Check if the node can be added with the named child node definition
            return checkTypeAgainstDefinition(defaultType, childNode);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeType#canAddChildNode(java.lang.String, java.lang.String)
     */
    public boolean canAddChildNode( String childNodeName,
                                    String primaryNodeTypeName ) {

        CheckArg.isNotNull(childNodeName, "childNodeName");
        CheckArg.isNotNull(primaryNodeTypeName, "primaryNodeTypeName");

        NodeType primaryNodeType;
        try {
            primaryNodeType = session.getWorkspace().getNodeTypeManager().getNodeType(primaryNodeTypeName);
        } catch (RepositoryException re) {
            // If the node type doesn't exist, you can't add a child node with that type
            return false;
        }

        // First, try to find a child node definition with the given name
        JcrNodeDefinition childNode = getChildNodeDefinition(childNodeName);

        // If there are no named definitions in the type hierarchy, try to find a residual node definition
        if (childNode == null) {
            childNode = getChildNodeDefinition(RESIDUAL_ITEM_NAME);
        }

        // Check if the node can be added with the named child node definition
        if (childNode != null) {
            return checkTypeAgainstDefinition(primaryNodeType, childNode);
        }

        return false;
    }

    /**
     * Checks whether the given type is the same type or a subtype of each of the required primary types for the given node
     * definition.
     * 
     * @param typeToCheck the type to check
     * @param definition the node definition to check against
     * @return <code>true</code> if and only if the given type is the same type or extends each of the required primary types in
     *         the given definition
     */
    private boolean checkTypeAgainstDefinition( NodeType typeToCheck,
                                                NodeDefinition definition ) {
        NodeType[] requiredPrimaryTypes = definition.getRequiredPrimaryTypes();
        for (int i = 0; i < requiredPrimaryTypes.length; i++) {
            // See if the given type for the node matches all of the required primary types
            if (!typeToCheck.isNodeType(requiredPrimaryTypes[i].getName())) {
                return false;
            }
        }
        // The node can be added with the given type based on the given child node definition
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeType#canRemoveItem(java.lang.String)
     */
    public boolean canRemoveItem( String itemName ) {
        CheckArg.isNotNull(itemName, "itemName");

        // Don't know if item is a property or a node, so check both locally before moving up the type hierarchy
        for (PropertyDefinition item : propertyDefinitions) {
            if (itemName.equals(item.getName())) {
                return !item.isMandatory() && !item.isProtected();
            }
        }

        for (NodeDefinition item : childNodeDefinitions) {
            if (itemName.equals(item.getName())) {
                return !item.isMandatory() && !item.isProtected();
            }
        }

        // Check if any supertypes prevent the removal of this item
        for (NodeType type : declaredSupertypes) {
            if (!type.canRemoveItem(itemName)) {
                return false;
            }
        }

        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeType#canSetProperty(java.lang.String, javax.jcr.Value)
     */
    public boolean canSetProperty( String propertyName,
                                   Value value ) {
        CheckArg.isNotNull(propertyName, "propertyName");

        JcrPropertyDefinition property = getPropertyDefinition(propertyName);
        if (property == null) {
            property = getPropertyDefinition(RESIDUAL_ITEM_NAME);
        }

        if (property == null) {
            return false;
        }

        // Can't modify a multi-property with a single value. Can't modify a protected property at all.
        if (property.isMultiple() || property.isProtected()) {
            return false;
        }

        // Null values indicates an attempt to unset property
        if (value == null) {
            return !property.isMandatory();
        }

        return canCastValueToType(value, property.getRequiredType());
    }

    /**
     * Internal method to validate that a value can be cast to a given JCR property type. The values are set according to the
     * following rules:
     * <ol>
     * <li>If <code>property.getRequiredType()</code> is {@link PropertyType#UNDEFINED}, return <code>true</code></li>
     * <li>Compare the type of the given value to the required type and see if they are compatible based on the rules in the JCR
     * 1.0 spec.</li>
     * </ol>
     * 
     * @param jcrPropertyType a value from the {@link PropertyType} constants to which this value MAY be able to be casted.
     * @param value the value to set (may not be <code>null</code>)
     * @return whether the value can be cast to the given property type
     */
    private boolean canCastValueToType( Value value,
                                        int jcrPropertyType ) {
        assert value != null;

        int valueType = value.getType();

        // Trivial case - no cast required
        if (valueType == jcrPropertyType) {
            return true;
        }

        try {
            switch (jcrPropertyType) {
                case PropertyType.BOOLEAN:
                    if (valueType == PropertyType.STRING) {
                        return true;
                    }

                    if (valueType == PropertyType.BINARY) {
                        // If the binary can be converted to a UTF-8 string, it can be set onto a boolean property
                        value.getString();
                        return true;
                    }
                    return false;

                case PropertyType.DATE:
                    if (valueType == PropertyType.DOUBLE || valueType == PropertyType.LONG) {
                        return true;
                    }

                    if (valueType == PropertyType.STRING || valueType == PropertyType.BINARY) {
                        // If the binary can be converted to a date, it can be set onto a date property
                        value.getDate();
                        return true;
                    }
                    return false;

                case PropertyType.NAME:
                    ValueFactories valueFactories = session.getExecutionContext().getValueFactories();
                    if (valueType == PropertyType.STRING || valueType == PropertyType.BINARY) {
                        valueFactories.getNameFactory().create(value.getString());
                        return true;
                    }

                    if (valueType == PropertyType.PATH) {
                        Path path = valueFactories.getPathFactory().create(value.getString());

                        Segment[] segments = path.getSegmentsArray();
                        return !path.isAbsolute() && segments.length == 1 && !segments[0].hasIndex();
                    }

                    return false;

                case PropertyType.PATH:
                    return value.getType() == PropertyType.STRING;

                    // Nothing can be converted to these types (except themselves)
                case PropertyType.REFERENCE:
                case PropertyType.DOUBLE:
                case PropertyType.LONG:
                    return false;

                    // Anything can be converted to these types
                case PropertyType.BINARY:
                case PropertyType.STRING:
                case PropertyType.UNDEFINED:
                    return true;
                default:
                    assert false : "Unexpected JCR property type " + jcrPropertyType;
                    // This should still throw an exception even if assertions are turned off
                    throw new IllegalStateException("Invalid property type " + jcrPropertyType);
            }
        } catch (RepositoryException re) {
            return false;
        } catch (ValueFormatException vfe) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeType#canSetProperty(java.lang.String, javax.jcr.Value[])
     */
    public boolean canSetProperty( String propertyName,
                                   Value[] values ) {
        CheckArg.isNotNull(propertyName, "propertyName");

        JcrPropertyDefinition property = getPropertyDefinition(propertyName);
        if (property == null) {
            property = getPropertyDefinition(RESIDUAL_ITEM_NAME);
        }

        if (property == null) {
            return false;
        }

        // Can't modify a single valued property with a multiple values. Can't modify a protected property at all.
        if (!property.isMultiple() || property.isProtected()) {
            return false;
        }

        // Null values indicates an attempt to unset property
        if (values == null) {
            return !property.isMandatory();
        }

        for (int i = 0; i < values.length; i++) {
            if (values[i] != null) {
                if (!canCastValueToType(values[i], property.getRequiredType())) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeType#getDeclaredChildNodeDefinitions()
     */
    public NodeDefinition[] getDeclaredChildNodeDefinitions() {
        return childNodeDefinitions.toArray(new JcrNodeDefinition[childNodeDefinitions.size()]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeType#getChildNodeDefinitions()
     */
    public NodeDefinition[] getChildNodeDefinitions() {
        Set<NodeDefinition> nodeDefs = new HashSet<NodeDefinition>();
        NodeType[] supertypes = getSupertypes();

        // TODO: This could be cached after being calculated once
        for (int i = 0; i < supertypes.length; i++) {
            NodeDefinition[] childNodeDefinitions = supertypes[i].getChildNodeDefinitions();
            for (int j = 0; j < childNodeDefinitions.length; i++) {

                // TODO: Could add sanity check here (assertion?) that definitions of the same child node in multiple supertypes
                // are consistent
                nodeDefs.add(childNodeDefinitions[j]);
            }
        }

        nodeDefs.addAll(childNodeDefinitions);

        return nodeDefs.toArray(new JcrNodeDefinition[nodeDefs.size()]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeType#getPropertyDefinitions()
     */
    public PropertyDefinition[] getPropertyDefinitions() {
        Set<PropertyDefinition> propDefs = new HashSet<PropertyDefinition>();
        NodeType[] supertypes = getSupertypes();

        // TODO: This could be cached after being calculated once
        for (int i = 0; i < supertypes.length; i++) {
            PropertyDefinition[] childPropertyDefinitions = supertypes[i].getPropertyDefinitions();
            for (int j = 0; j < childPropertyDefinitions.length; j++) {

                // TODO: Could add sanity check here (assertion?) that definitions of the same child node in multiple supertypes
                // are consistent
                propDefs.add(childPropertyDefinitions[j]);
            }
        }
        propDefs.addAll(propertyDefinitions);

        return propDefs.toArray(new JcrPropertyDefinition[propDefs.size()]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeType#getDeclaredSupertypes()
     */
    public NodeType[] getDeclaredSupertypes() {
        return declaredSupertypes.toArray(new NodeType[declaredSupertypes.size()]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeType#getName()
     */
    public String getName() {
        // Translate the name to the correct prefix. Need to check the session to support url-remapping.
        return name.getString(session.getExecutionContext().getNamespaceRegistry());
    }

    /**
     * Returns the internal {@link Name} object for the note type. This method exists outside the JCR API and should not be
     * exposed outside of the package.
     * 
     * @return the internal {@link Name} object for the note type.
     */
    Name getInternalName() {
        return name;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeType#getPrimaryItemName()
     */
    public String getPrimaryItemName() {
        if (primaryItemName == null) {
            return null;
        }

        // Translate the name to the correct prefix. Need to check the session to support url-remapping.
        return primaryItemName.getString(session.getExecutionContext().getNamespaceRegistry());
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeType#getDeclaredPropertyDefinitions()
     */
    public PropertyDefinition[] getDeclaredPropertyDefinitions() {
        return propertyDefinitions.toArray(new JcrPropertyDefinition[propertyDefinitions.size()]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeType#getSupertypes()
     */
    public NodeType[] getSupertypes() {
        Set<NodeType> supertypes = new HashSet<NodeType>();
        Stack<NodeType> unvisitedSupertypes = new Stack<NodeType>();

        assert declaredSupertypes != null;
        unvisitedSupertypes.addAll(declaredSupertypes);

        // TODO: If this ends up getting called frequently, it should probably be executed once in the constructor and have the
        // results cached.
        while (!unvisitedSupertypes.isEmpty()) {
            NodeType nodeType = unvisitedSupertypes.pop();

            /*
             * If we haven't already visited this nodeType (which we can
             * infer by whether or not it was already added to the return set),
             * then add the supertypes of this new node to the unvisited set for 
             * further inspection.
             */
            if (!supertypes.contains(nodeType)) {
                supertypes.add(nodeType);
                // Violating encapsulation to avoid going from List to array back to List
                unvisitedSupertypes.addAll(((JcrNodeType)nodeType).declaredSupertypes);
            }
        }

        return supertypes.toArray(new NodeType[0]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeType#hasOrderableChildNodes()
     */
    public boolean hasOrderableChildNodes() {
        return orderableChildNodes;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeType#isMixin()
     */
    public boolean isMixin() {
        return mixin;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeType#isNodeType(java.lang.String)
     */
    public boolean isNodeType( String nodeTypeName ) {
        if (this.getName().equals(nodeTypeName)) return true;

        // TODO: This could be optimized
        NodeType[] supertypes = getSupertypes();
        for (int i = 0; i < supertypes.length; i++) {
            if (supertypes[i].isNodeType(nodeTypeName)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String toString() {
        return getName();
    }
}
