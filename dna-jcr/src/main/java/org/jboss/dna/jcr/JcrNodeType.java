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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.basic.BasicName;

/**
 * DNA implementation of JCR {@link NodeType}s.
 */
@Immutable
class JcrNodeType implements NodeType {

    public static final String RESIDUAL_ITEM_NAME = "*";
    public static final Name RESIDUAL_NAME = new BasicName("", RESIDUAL_ITEM_NAME);

    /** The name of the node type (e.g., <code>{http://www.jcp.org/jcr/nt/1.0}base</code>) */
    private final Name name;
    /** The name of the node's primary item */
    private final Name primaryItemName;

    /** The supertypes for this node. */
    private final List<JcrNodeType> declaredSupertypes;

    /**
     * The list of all supertypes for this node, beginning with the immediate supertypes, followed by the supertypes of those
     * supertypes, etc.
     */
    private final List<JcrNodeType> allSupertypes;

    /**
     * The list of this type and all supertypes for this node, beginning with this type, continuing with the immediate supertypes,
     * followed by the supertypes of those supertypes, etc.
     */
    private final List<JcrNodeType> thisAndAllSupertypes;
    private final Set<Name> thisAndAllSupertypesNames;

    /** Indicates whether this node type is a mixin type (as opposed to a primary type). */
    private boolean mixin;
    /** Indicates whether the child nodes of nodes of this type can be ordered. */
    private boolean orderableChildNodes;

    /**
     * The child node definitions that are defined on this node type.
     */
    private final List<JcrNodeDefinition> childNodeDefinitions;

    /**
     * The property definitions that are defined on this node type.
     */
    private final List<JcrPropertyDefinition> propertyDefinitions;

    /**
     * A local cache of all defined and inherited child node definitions and property definitions. Residual definitions are
     * included. This class's methods to find a property definition and find child node definitions, and since they're frequently
     * used by SessionCache this cache provides very quick access.
     */
    private final DefinitionCache allDefinitions;

    /**
     * A reference to the execution context in which this node type exists, used to remap the internal names to their appropriate
     * prefixed version (e.g., <code>{http://www.jcp.org/jcr/nt/1.0}base</code> to <code>&quot;nt:base&quot;</code>.).
     */
    private ExecutionContext context;

    /** Link to the repository node type manager for the repository to which this node type belongs. */
    private RepositoryNodeTypeManager nodeTypeManager;

    JcrNodeType( ExecutionContext context,
                 RepositoryNodeTypeManager nodeTypeManager,
                 Name name,
                 List<JcrNodeType> declaredSupertypes,
                 Name primaryItemName,
                 Collection<JcrNodeDefinition> childNodeDefinitions,
                 Collection<JcrPropertyDefinition> propertyDefinitions,
                 boolean mixin,
                 boolean orderableChildNodes ) {
        this.context = context;
        this.nodeTypeManager = nodeTypeManager;
        this.name = name;
        this.primaryItemName = primaryItemName;
        this.declaredSupertypes = declaredSupertypes != null ? declaredSupertypes : Collections.<JcrNodeType>emptyList();
        this.mixin = mixin;
        this.orderableChildNodes = orderableChildNodes;
        this.propertyDefinitions = new ArrayList<JcrPropertyDefinition>(propertyDefinitions.size());
        for (JcrPropertyDefinition property : propertyDefinitions) {
            this.propertyDefinitions.add(property.with(this));
        }

        this.childNodeDefinitions = new ArrayList<JcrNodeDefinition>(childNodeDefinitions.size());
        for (JcrNodeDefinition childNode : childNodeDefinitions) {
            this.childNodeDefinitions.add(childNode.with(this));
        }

        // Build the list of all types, including supertypes ...
        List<JcrNodeType> thisAndAllSupertypes = new LinkedList<JcrNodeType>();
        Set<Name> typeNames = new HashSet<Name>();
        thisAndAllSupertypes.add(this);
        typeNames.add(this.name);
        for (int i = 0; i != thisAndAllSupertypes.size(); ++i) {
            JcrNodeType superType = thisAndAllSupertypes.get(i);
            for (NodeType superSuperType : superType.getDeclaredSupertypes()) {
                JcrNodeType jcrSuperSuperType = (JcrNodeType)superSuperType;
                if (typeNames.add(jcrSuperSuperType.getInternalName())) {
                    thisAndAllSupertypes.add(jcrSuperSuperType);
                }
            }
        }
        this.thisAndAllSupertypes = Collections.unmodifiableList(thisAndAllSupertypes);
        // Make the list of all supertypes to be a sublist of the first ...
        this.allSupertypes = thisAndAllSupertypes.size() > 1 ? thisAndAllSupertypes.subList(1, thisAndAllSupertypes.size()) : Collections.<JcrNodeType>emptyList();

        // Set up the set of all supertype names ...
        this.thisAndAllSupertypesNames = Collections.unmodifiableSet(typeNames);

        this.allDefinitions = new DefinitionCache(this);
    }

    List<JcrNodeType> getTypeAndSupertypes() {
        return thisAndAllSupertypes;
    }

    /**
     * Get the child definitions defined on this node type (excluding inherited definitions).
     * 
     * @return this node's child node definitions; never null
     */
    List<JcrNodeDefinition> childNodeDefinitions() {
        return childNodeDefinitions;
    }

    /**
     * Get the property definitions defined on this node type (excluding inherited definitions).
     * 
     * @return this node's property definitions; never null
     */
    List<JcrPropertyDefinition> propertyDefinitions() {
        return propertyDefinitions;
    }

    Collection<JcrPropertyDefinition> allSingleValuePropertyDefinitions( Name propertyName ) {
        return allDefinitions.allSingleValuePropertyDefinitions(propertyName);
    }

    Collection<JcrPropertyDefinition> allMultiValuePropertyDefinitions( Name propertyName ) {
        return allDefinitions.allMultiValuePropertyDefinitions(propertyName);
    }

    Collection<JcrPropertyDefinition> allPropertyDefinitions( Name propertyName ) {
        return allDefinitions.allPropertyDefinitions(propertyName);
    }

    Collection<JcrNodeDefinition> allChildNodeDefinitions( Name propertyName,
                                                           boolean requireSns ) {
        return allDefinitions.allChildNodeDefinitions(propertyName, requireSns);
    }

    Collection<JcrNodeDefinition> allChildNodeDefinitions( Name propertyName ) {
        return allDefinitions.allChildNodeDefinitions(propertyName);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeType#canAddChildNode(java.lang.String)
     */
    public boolean canAddChildNode( String childNodeName ) {
        CheckArg.isNotNull(childNodeName, "childNodeName");
        Name childName = context.getValueFactories().getNameFactory().create(childNodeName);
        return nodeTypeManager().findChildNodeDefinition(this.name, null, childName, null, 0, true) != null;
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
        Name childName = context.getValueFactories().getNameFactory().create(childNodeName);
        Name childPrimaryTypeName = context.getValueFactories().getNameFactory().create(primaryNodeTypeName);
        return nodeTypeManager().findChildNodeDefinition(this.name, null, childName, childPrimaryTypeName, 0, true) != null;
    }

    public boolean canRemoveNode( String itemName ) {
        CheckArg.isNotNull(itemName, "itemName");
        Name childName = context.getValueFactories().getNameFactory().create(itemName);
        return nodeTypeManager().canRemoveAllChildren(this.name, null, childName, true);
    }

    /**
     * {@inheritDoc}
     * <p>
     * According to the JCR 1.0 JavaDoc, this method applies to all children. However, this appears to be changed in the JSR-283
     * draft to apply only to nodes, and it is also deprecated.
     * </p>
     * 
     * @see javax.jcr.nodetype.NodeType#canRemoveItem(java.lang.String)
     */
    public boolean canRemoveItem( String itemName ) {
        return canRemoveNode(itemName) || canRemoveProperty(itemName);
    }

    /**
     * Returns <code>true</code> if <code>value</code> can be cast to <code>property.getRequiredType()</code> per the type
     * conversion rules in section 6.2.6 of the JCR 1.0 specification AND <code>value</code> satisfies the constraints (if any)
     * for the property definition. If the property definition has a required type of {@link PropertyType#UNDEFINED}, the cast
     * will be considered to have succeeded and the value constraints (if any) will be interpreted using the semantics for the
     * type specified in <code>value.getType()</code>.
     * 
     * @param propertyDefinition the property definition to validate against
     * @param value the value to be validated
     * @return <code>true</code> if the value can be cast to the required type for the property definition (if it exists) and
     *         satisfies the constraints for the property (if any exist).
     * @see PropertyDefinition#getValueConstraints()
     * @see JcrPropertyDefinition#satisfiesConstraints(Value)
     */
    boolean canCastToTypeAndMatchesConstraints( JcrPropertyDefinition propertyDefinition,
                                                Value value ) {
        try {
            assert value instanceof JcrValue : "Illegal implementation of Value interface";
            ((JcrValue)value).asType(propertyDefinition.getRequiredType()); // throws ValueFormatException if there's a problem
            return propertyDefinition.satisfiesConstraints(value);
        } catch (javax.jcr.ValueFormatException vfe) {
            // Cast failed
            return false;
        }
    }

    /**
     * Returns <code>true</code> if <code>value</code> can be cast to <code>property.getRequiredType()</code> per the type
     * conversion rules in section 6.2.6 of the JCR 1.0 specification AND <code>value</code> satisfies the constraints (if any)
     * for the property definition. If the property definition has a required type of {@link PropertyType#UNDEFINED}, the cast
     * will be considered to have succeeded and the value constraints (if any) will be interpreted using the semantics for the
     * type specified in <code>value.getType()</code>.
     * 
     * @param propertyDefinition the property definition to validate against
     * @param values the values to be validated
     * @return <code>true</code> if the value can be cast to the required type for the property definition (if it exists) and
     *         satisfies the constraints for the property (if any exist).
     * @see PropertyDefinition#getValueConstraints()
     * @see JcrPropertyDefinition#satisfiesConstraints(Value)
     */
    boolean canCastToTypeAndMatchesConstraints( JcrPropertyDefinition propertyDefinition,
                                                Value[] values ) {
        for (Value value : values) {
            if (!canCastToTypeAndMatchesConstraints(propertyDefinition, value)) return false;
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
        Name name = context.getValueFactories().getNameFactory().create(propertyName);

        // Reuse the logic in RepositoryNodeTypeManager ...
        return nodeTypeManager().findPropertyDefinition(this.name, null, name, value, false, true) != null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeType#canSetProperty(java.lang.String, javax.jcr.Value[])
     */
    public boolean canSetProperty( String propertyName,
                                   Value[] values ) {
        CheckArg.isNotNull(propertyName, "propertyName");
        if (values == null || values.length == 0) {
            return canRemoveProperty(propertyName);
        }

        Name name = context.getValueFactories().getNameFactory().create(propertyName);
        // Reuse the logic in RepositoryNodeTypeManager ...
        return nodeTypeManager().findPropertyDefinition(this.name, null, name, values, true) != null;
    }

    public boolean canRemoveProperty( String propertyName ) {
        CheckArg.isNotNull(propertyName, "propertyName");
        Name name = context.getValueFactories().getNameFactory().create(propertyName);

        // Reuse the logic in RepositoryNodeTypeManager ...
        return nodeTypeManager().canRemoveProperty(this.name, null, name, true);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeType#getDeclaredChildNodeDefinitions()
     */
    public NodeDefinition[] getDeclaredChildNodeDefinitions() {
        // Always have to make a copy to prevent changes ...
        return childNodeDefinitions.toArray(new JcrNodeDefinition[childNodeDefinitions.size()]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeType#getChildNodeDefinitions()
     */
    public NodeDefinition[] getChildNodeDefinitions() {
        // Always have to make a copy to prevent changes ...
        Collection<JcrNodeDefinition> definitions = this.allDefinitions.allChildNodeDefinitions();
        return definitions.toArray(new NodeDefinition[definitions.size()]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeType#getPropertyDefinitions()
     */
    public PropertyDefinition[] getPropertyDefinitions() {
        // Always have to make a copy to prevent changes ...
        Collection<JcrPropertyDefinition> definitions = this.allDefinitions.allPropertyDefinitions();
        return definitions.toArray(new PropertyDefinition[definitions.size()]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeType#getDeclaredSupertypes()
     */
    public NodeType[] getDeclaredSupertypes() {
        // Always have to make a copy to prevent changes ...
        return declaredSupertypes.toArray(new NodeType[declaredSupertypes.size()]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeType#getName()
     */
    public String getName() {
        // Translate the name to the correct prefix. Need to check the session to support url-remapping.
        return name.getString(context.getNamespaceRegistry());
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
        return primaryItemName.getString(context.getNamespaceRegistry());
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
        return allSupertypes.toArray(new NodeType[allSupertypes.size()]);
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
        if (nodeTypeName == null) return false;
        Name name = context.getValueFactories().getNameFactory().create(nodeTypeName);
        return this.thisAndAllSupertypesNames.contains(name);
    }

    boolean isNodeType( Name nodeTypeName ) {
        if (nodeTypeName == null) return false;
        return this.thisAndAllSupertypesNames.contains(nodeTypeName);
    }

    @Override
    public String toString() {
        return getName();
    }

    /**
     * Returns a {@link JcrNodeType} that is equivalent to this {@link JcrNodeType}, except with a different repository node type
     * manager. This method should only be called during the initialization of the repository node type manager, unless some kind
     * of cross-repository type shipping is implemented.
     * 
     * @param nodeTypeManager the new repository node type manager
     * @return a new {@link JcrNodeType} that has the same state as this node type, but with the given node type manager.
     */
    final JcrNodeType with( RepositoryNodeTypeManager nodeTypeManager ) {
        return new JcrNodeType(this.context, nodeTypeManager, this.name, this.declaredSupertypes, this.primaryItemName,
                               this.childNodeDefinitions, this.propertyDefinitions, this.mixin, this.orderableChildNodes);
    }

    /**
     * Returns a {@link JcrNodeType} that is equivalent to this {@link JcrNodeType}, except with a different execution context.
     * 
     * @param context the new execution context
     * @return a new {@link JcrNodeType} that has the same state as this node type, but with the given node type manager.
     * @see JcrNodeTypeManager
     */
    final JcrNodeType with( ExecutionContext context ) {
        return new JcrNodeType(context, this.nodeTypeManager, this.name, this.declaredSupertypes, this.primaryItemName,
                               this.childNodeDefinitions, this.propertyDefinitions, this.mixin, this.orderableChildNodes);
    }

    final RepositoryNodeTypeManager nodeTypeManager() {
        return nodeTypeManager;
    }
}
