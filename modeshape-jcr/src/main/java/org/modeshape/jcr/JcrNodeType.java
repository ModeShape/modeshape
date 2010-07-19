/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
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
import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.PropertyDefinition;
import net.jcip.annotations.ThreadSafe;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.basic.BasicName;

/**
 * ModeShape implementation of JCR {@link NodeType}s.
 */
@ThreadSafe
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
    private final boolean mixin;
    /** Indicates whether the child nodes of nodes of this type can be ordered. */
    private final boolean orderableChildNodes;

    /** Indicates whether this node type is abstract */
    private final boolean isAbstract;

    /** Indicates whether this node is queryable (i.e., should be included in query results) */
    private final boolean queryable;

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
                 boolean isAbstract,
                 boolean queryable,
                 boolean orderableChildNodes ) {
        assert context != null;

        this.context = context;
        this.nodeTypeManager = nodeTypeManager;
        this.name = name;
        this.primaryItemName = primaryItemName;
        this.declaredSupertypes = declaredSupertypes != null ? declaredSupertypes : Collections.<JcrNodeType>emptyList();
        this.mixin = mixin;
        this.queryable = queryable;
        this.isAbstract = isAbstract;
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

                if (jcrSuperSuperType == null) {
                    assert JcrNtLexicon.BASE.equals(name);
                    continue;
                }

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

    List<JcrNodeType> supertypes() {
        return allSupertypes;
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

    /**
     * Get all of the property definitions defined on this node type and its supertypes.
     * 
     * @return this node's explicit and inherited property definitions; never null
     */
    Collection<JcrPropertyDefinition> allPropertyDefinitions() {
        return allDefinitions.allPropertyDefinitions();
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

    /**
     * Get all of the child node definitions defined on this node type and its supertypes.
     * 
     * @return this node's explicit and inherited child node definitions; never null
     */
    Collection<JcrNodeDefinition> allChildNodeDefinitions() {
        return allDefinitions.allChildNodeDefinitions();
    }

    Collection<JcrNodeDefinition> allChildNodeDefinitions( Name childName,
                                                           boolean requireSns ) {
        return allDefinitions.allChildNodeDefinitions(childName, requireSns);
    }

    Collection<JcrNodeDefinition> allChildNodeDefinitions( Name childName ) {
        return allDefinitions.allChildNodeDefinitions(childName);
    }

    JcrNodeDefinition childNodeDefinition( NodeDefinitionId nodeDefnId ) {
        List<Name> requiredPrimaryTypeNames = Arrays.asList(nodeDefnId.getRequiredPrimaryTypes());
        for (JcrNodeDefinition nodeDefn : allChildNodeDefinitions(nodeDefnId.getChildDefinitionName())) {
            if (nodeDefn.requiredPrimaryTypeNameSet().size() == requiredPrimaryTypeNames.size()
                && nodeDefn.requiredPrimaryTypeNameSet().containsAll(requiredPrimaryTypeNames)) {
                return nodeDefn;
            }
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

        if (primaryNodeTypeName != null) {
            JcrNodeType childType = this.nodeTypeManager().getNodeType(childPrimaryTypeName);
            if (childType.isAbstract() || childType.isMixin()) return false;
        }

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
     * In JCR 1.0, this method applied to all children. However, this was changed in the JSR-283 specification to apply only to
     * nodes, and it is also deprecated.
     * </p>
     * 
     * @see javax.jcr.nodetype.NodeType#canRemoveItem(java.lang.String)
     */
    public boolean canRemoveItem( String itemName ) {
        CheckArg.isNotNull(itemName, "itemName");
        Name childName = context.getValueFactories().getNameFactory().create(itemName);
        return nodeTypeManager().canRemoveItem(this.name, null, childName, true);
    }

    /**
     * Returns <code>true</code> if <code>value</code> can be cast to <code>property.getRequiredType()</code> per the type
     * conversion rules in section 3.6.4 of the JCR 2.0 specification AND <code>value</code> satisfies the constraints (if any)
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
     * conversion rules in section 3.6.4 of the JCR 2.0 specification AND <code>value</code> satisfies the constraints (if any)
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
    public JcrNodeDefinition[] getDeclaredChildNodeDefinitions() {
        // Always have to make a copy to prevent changes ...
        return childNodeDefinitions.toArray(new JcrNodeDefinition[childNodeDefinitions.size()]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeType#getChildNodeDefinitions()
     */
    public JcrNodeDefinition[] getChildNodeDefinitions() {
        // Always have to make a copy to prevent changes ...
        Collection<JcrNodeDefinition> definitions = this.allDefinitions.allChildNodeDefinitions();
        return definitions.toArray(new JcrNodeDefinition[definitions.size()]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeType#getPropertyDefinitions()
     */
    public JcrPropertyDefinition[] getPropertyDefinitions() {
        // Always have to make a copy to prevent changes ...
        Collection<JcrPropertyDefinition> definitions = this.allDefinitions.allPropertyDefinitions();
        return definitions.toArray(new JcrPropertyDefinition[definitions.size()]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeType#getDeclaredSupertypes()
     */
    public JcrNodeType[] getDeclaredSupertypes() {
        // Always have to make a copy to prevent changes ...
        return declaredSupertypes.toArray(new JcrNodeType[declaredSupertypes.size()]);
    }

    /**
     * @return the array of names of supertypes declared for this node; possibly empty, never null
     */
    public String[] getDeclaredSupertypeNames() {
        List<String> supertypeNames = new ArrayList<String>(declaredSupertypes.size());

        for (JcrNodeType declaredSupertype : declaredSupertypes) {
            supertypeNames.add(declaredSupertype.getName());
        }

        // Always have to make a copy to prevent changes ...
        return supertypeNames.toArray(new String[supertypeNames.size()]);
    }

    public NodeTypeIterator getSubtypes() {
        return new JcrNodeTypeIterator(nodeTypeManager.subtypesFor(this));
    }

    public NodeTypeIterator getDeclaredSubtypes() {
        return new JcrNodeTypeIterator(nodeTypeManager.declaredSubtypesFor(this));
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
     * Returns the internal {@link Name} object for the node type. This method exists outside the JCR API and should not be
     * exposed outside of the package.
     * 
     * @return the internal {@link Name} object for the node type.
     */
    Name getInternalName() {
        return name;
    }

    /**
     * Returns the internal {@link Name} object for the primary item of this node type. This method exists outside the JCR API and
     * should not be exposed outside of the package.
     * 
     * @return the internal {@link Name} object for the primary item of this node type.
     */
    Name getInternalPrimaryItemName() {
        return primaryItemName;
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
    public JcrPropertyDefinition[] getDeclaredPropertyDefinitions() {
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

    public boolean isAbstract() {
        return isAbstract;
    }

    public boolean isQueryable() {
        return queryable;
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

    boolean isNodeTypeOneOf( Name... nodeTypeNames ) {
        if (nodeTypeNames == null || nodeTypeNames.length == 0) return false;
        for (Name nodeTypeName : nodeTypeNames) {
            if (this.thisAndAllSupertypesNames.contains(nodeTypeName)) return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof JcrNodeType) {
            JcrNodeType that = (JcrNodeType)obj;
            return this.name.equals(that.name);
        }
        if (obj instanceof NodeType) {
            NodeType that = (NodeType)obj;
            return this.getName().equals(that.getName());
        }
        return false;
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
                               this.childNodeDefinitions, this.propertyDefinitions, this.mixin, this.isAbstract, this.queryable,
                               this.orderableChildNodes);
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
                               this.childNodeDefinitions, this.propertyDefinitions, this.mixin, this.isAbstract, this.queryable,
                               this.orderableChildNodes);
    }

    final RepositoryNodeTypeManager nodeTypeManager() {
        return nodeTypeManager;
    }

    /**
     * Returns whether this node type is in conflict with the provided primary node type or mixin types.
     * <p>
     * A node type is in conflict with another set of node types if either of the following is true:
     * <ol>
     * <li>This node type has the same name as any of the other node types</li>
     * <li>This node type defines a property (or inherits the definition of a property) with the same name as a property defined
     * in any of the other node types <i>unless</i> this node type and the other node type both inherited the property definition
     * from the same ancestor node type</li>
     * <li>This node type defines a child node (or inherits the definition of a child node) with the same name as a child node
     * defined in any of the other node types <i>unless</i> this node type and the other node type both inherited the child node
     * definition from the same ancestor node type</li>
     * </ol>
     * </p>
     * 
     * @param primaryNodeType the primary node type to check
     * @param mixinNodeTypes the mixin node types to check
     * @return true if this node type conflicts with the provided primary or mixin node types as defined below
     */
    final boolean conflictsWith( NodeType primaryNodeType,
                                 NodeType[] mixinNodeTypes ) {
        Map<PropertyDefinitionId, JcrPropertyDefinition> props = new HashMap<PropertyDefinitionId, JcrPropertyDefinition>();
        /*
         * Need to have the same parent name for all PropertyDefinitionIds and NodeDefinitionIds as we're checking for conflicts on the definition name
         */
        final Name DEFAULT_NAME = this.name;

        for (JcrPropertyDefinition property : propertyDefinitions()) {
            /*
             * I'm trying really hard to reuse existing code, but it's a stretch in this case.  I don't care about the property
             * types or where they were declared... if more than one definition with the given name exists (not counting definitions
             * inherited from the same root definition), then there is a conflict.
             */
            PropertyDefinitionId pid = new PropertyDefinitionId(DEFAULT_NAME, property.name, PropertyType.UNDEFINED,
                                                                property.isMultiple());
            props.put(pid, property);
        }

        /*
         * The specification does not mandate whether this should or should not be consider a conflict.  However, the Apache
         * TCK canRemoveMixin test cases assume that this will generate a conflict.
         */
        if (primaryNodeType.getName().equals(getName())) {
            // This node type has already been applied to the node
            return true;
        }

        for (JcrPropertyDefinition property : ((JcrNodeType)primaryNodeType).propertyDefinitions()) {
            PropertyDefinitionId pid = new PropertyDefinitionId(DEFAULT_NAME, property.name, PropertyType.UNDEFINED,
                                                                property.isMultiple());
            JcrPropertyDefinition oldProp = props.put(pid, property);
            if (oldProp != null) {
                String oldPropTypeName = oldProp.getDeclaringNodeType().getName();
                String propTypeName = property.getDeclaringNodeType().getName();
                if (!oldPropTypeName.equals(propTypeName)) {
                    // The two types conflict as both separately declare a property with the same name
                    return true;
                }
            }
        }

        for (NodeType mixinNodeType : mixinNodeTypes) {
            /*
             * The specification does not mandate whether this should or should not be consider a conflict.  However, the Apache
             * TCK canRemoveMixin test cases assume that this will generate a conflict.
             */
            if (mixinNodeType.getName().equals(getName())) {
                // This node type has already been applied to the node
                return true;
            }

            for (JcrPropertyDefinition property : ((JcrNodeType)mixinNodeType).propertyDefinitions()) {
                PropertyDefinitionId pid = new PropertyDefinitionId(DEFAULT_NAME, property.name, PropertyType.UNDEFINED,
                                                                    property.isMultiple());
                JcrPropertyDefinition oldProp = props.put(pid, property);
                if (oldProp != null) {
                    String oldPropTypeName = oldProp.getDeclaringNodeType().getName();
                    String propTypeName = property.getDeclaringNodeType().getName();
                    if (!oldPropTypeName.equals(propTypeName)) {
                        // The two types conflict as both separately declare a property with the same name
                        return true;
                    }
                }
            }
        }

        Map<NodeDefinitionId, JcrNodeDefinition> childNodes = new HashMap<NodeDefinitionId, JcrNodeDefinition>();

        for (JcrNodeDefinition childNode : childNodeDefinitions()) {
            NodeDefinitionId nid = new NodeDefinitionId(DEFAULT_NAME, childNode.name, new Name[0]);
            childNodes.put(nid, childNode);
        }

        for (JcrNodeDefinition childNode : ((JcrNodeType)primaryNodeType).childNodeDefinitions()) {
            NodeDefinitionId nid = new NodeDefinitionId(DEFAULT_NAME, childNode.name, new Name[0]);
            JcrNodeDefinition oldNode = childNodes.put(nid, childNode);
            if (oldNode != null) {
                String oldNodeTypeName = oldNode.getDeclaringNodeType().getName();
                String childNodeTypeName = childNode.getDeclaringNodeType().getName();
                if (!oldNodeTypeName.equals(childNodeTypeName)) {
                    // The two types conflict as both separately declare a child node with the same name
                    return true;
                }
            }
        }

        for (NodeType mixinNodeType : mixinNodeTypes) {
            for (JcrNodeDefinition childNode : ((JcrNodeType)mixinNodeType).childNodeDefinitions()) {
                NodeDefinitionId nid = new NodeDefinitionId(DEFAULT_NAME, childNode.name, new Name[0]);
                JcrNodeDefinition oldNode = childNodes.put(nid, childNode);
                if (oldNode != null) {
                    String oldNodeTypeName = oldNode.getDeclaringNodeType().getName();
                    String childNodeTypeName = childNode.getDeclaringNodeType().getName();
                    if (!oldNodeTypeName.equals(childNodeTypeName)) {
                        // The two types conflict as both separately declare a child node with the same name
                        return true;
                    }
                }
            }
        }

        return false;
    }

}
