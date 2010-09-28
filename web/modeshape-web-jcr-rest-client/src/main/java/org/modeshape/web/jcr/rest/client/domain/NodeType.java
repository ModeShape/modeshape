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
package org.modeshape.web.jcr.rest.client.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeTypeIterator;
import net.jcip.annotations.Immutable;
import org.modeshape.web.jcr.rest.client.RestClientI18n;

/**
 * The NodeType class is the business object for a ModeShape supported node type.
 */
@Immutable
public class NodeType implements IModeShapeObject, javax.jcr.nodetype.NodeType {

    /**
     * The node type name.
     */
    private final String name;
    private final boolean isAbstract;
    private final boolean isMixin;
    private final boolean isQueryable;
    private final boolean hasOrderableChildNodes;
    private final String primaryItemName;
    private final Map<PropertyDefinition.Id, PropertyDefinition> propertyDefinitions;
    private final Map<ChildNodeDefinition.Id, ChildNodeDefinition> childNodeDefinitions;
    private final List<String> declaredSuperTypes;
    private final Map<String, NodeType> nodeTypes;
    private List<NodeType> allSuperTypes;
    private Set<String> allSuperTypeNames;
    private Map<PropertyDefinition.Id, PropertyDefinition> allPropertyDefinitions;
    private Map<ChildNodeDefinition.Id, ChildNodeDefinition> allChildNodeDefinitions;

    // ===========================================================================================================================
    // Constructors
    // ===========================================================================================================================

    /**
     * Constructs a new instance.
     * 
     * @param name the node type name; may not be null
     * @param isMixin true if this node type is a mixin, or false otherwise
     * @param isAbstract true if this node type is an abstract node type, or false otherwise
     * @param superTypes the names of the declared supertypes for this node type; may be null or empty if there are none
     * @param propertyDefinitions the property definitions declared on this node type; may be null or empty if there are none
     * @param childNodeDefinitions the child node definitions declared on this node type; may be null or empty if there are none
     * @param primaryItemName the name of the primary child item; may be null or empty
     * @param hasOrderableChildNodes true if this node type's children are orderable, or false otherwise
     * @param isQueryable true if this node type can be queried, or false otherwise
     * @param nodeTypes the map of node types keyed by their name; may be null only if no node types will be found when needed
     * @throws IllegalArgumentException if the name is null
     */
    public NodeType( String name,
                     boolean isMixin,
                     boolean isAbstract,
                     List<String> superTypes,
                     List<PropertyDefinition> propertyDefinitions,
                     List<ChildNodeDefinition> childNodeDefinitions,
                     String primaryItemName,
                     boolean hasOrderableChildNodes,
                     boolean isQueryable,
                     Map<String, NodeType> nodeTypes ) {
        assert name != null;
        this.name = name;
        this.isMixin = isMixin;
        this.isAbstract = isAbstract;
        this.isQueryable = isQueryable;
        this.hasOrderableChildNodes = hasOrderableChildNodes;
        this.primaryItemName = primaryItemName;
        this.declaredSuperTypes = superTypes != null ? superTypes : Collections.<String>emptyList();
        if (propertyDefinitions != null && !propertyDefinitions.isEmpty()) {
            Map<PropertyDefinition.Id, PropertyDefinition> propDefns = new HashMap<PropertyDefinition.Id, PropertyDefinition>();
            for (PropertyDefinition propDefn : propertyDefinitions) {
                propDefns.put(propDefn.id(), propDefn);
            }
            this.propertyDefinitions = propDefns;
        } else {
            this.propertyDefinitions = Collections.emptyMap();
        }
        if (childNodeDefinitions != null && !childNodeDefinitions.isEmpty()) {
            Map<ChildNodeDefinition.Id, ChildNodeDefinition> nodeDefns = new HashMap<ChildNodeDefinition.Id, ChildNodeDefinition>();
            for (ChildNodeDefinition nodeDefn : childNodeDefinitions) {
                nodeDefns.put(nodeDefn.id(), nodeDefn);
            }
            this.childNodeDefinitions = nodeDefns;
        } else {
            this.childNodeDefinitions = Collections.emptyMap();
        }
        this.nodeTypes = nodeTypes != null ? nodeTypes : new HashMap<String, NodeType>();
        this.nodeTypes.put(this.name, this);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.web.jcr.rest.client.domain.IModeShapeObject#getName()
     */
    public String getName() {
        return this.name;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeType#getDeclaredSupertypes()
     */
    @Override
    public javax.jcr.nodetype.NodeType[] getDeclaredSupertypes() {
        return ItemDefinition.nodeTypes(declaredSuperTypes, nodeTypes);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeType#getSupertypes()
     */
    @Override
    public javax.jcr.nodetype.NodeType[] getSupertypes() {
        List<NodeType> allSuperTypes = allSuperTypes();
        return allSuperTypes.toArray(new javax.jcr.nodetype.NodeType[allSuperTypes.size()]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeTypeDefinition#getDeclaredSupertypeNames()
     */
    @Override
    public String[] getDeclaredSupertypeNames() {
        return ItemDefinition.toArray(declaredSuperTypes);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeType#getDeclaredSubtypes()
     */
    @Override
    public NodeTypeIterator getDeclaredSubtypes() {
        List<NodeType> results = new ArrayList<NodeType>();
        for (NodeType nodeType : nodeTypes.values()) {
            if (nodeType == this) continue;
            if (nodeType.declaredSuperTypes.contains(name)) {
                results.add(nodeType);
            }
        }
        return new NodeTypes(results);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeType#getSubtypes()
     */
    @Override
    public NodeTypeIterator getSubtypes() {
        List<NodeType> results = new ArrayList<NodeType>();
        for (NodeType nodeType : nodeTypes.values()) {
            if (nodeType == this) continue;
            if (nodeType.allSuperTypeNames().contains(name)) {
                results.add(nodeType);
            }
        }
        return new NodeTypes(results);
    }

    protected List<NodeType> allSuperTypes() {
        if (this.allSuperTypes == null) {
            List<NodeType> allSuperTypes = new ArrayList<NodeType>();
            Set<String> allSuperTypeNames = new HashSet<String>();
            for (String superTypeName : declaredSuperTypes) {
                NodeType superType = nodeTypes.get(superTypeName);
                if (superType != null) {
                    allSuperTypes.add(superType);
                    allSuperTypeNames.add(superType.getName());
                    // Add all of the supertypes ...
                    allSuperTypes.addAll(superType.allSuperTypes());
                    allSuperTypeNames.addAll(superType.allSuperTypeNames());
                }
            }
            if (allSuperTypes.isEmpty() && !isMixin) {
                // All non-mixin node types ultimately extend 'nt:base' ...
                NodeType ntBase = nodeTypes.get("nt:base");
                if (ntBase != null) {
                    allSuperTypes.add(ntBase);
                    allSuperTypeNames.add(ntBase.getName());
                }
            }
            this.allSuperTypes = allSuperTypes;
            this.allSuperTypeNames = allSuperTypeNames;
        }
        return this.allSuperTypes;
    }

    protected Set<String> allSuperTypeNames() {
        allSuperTypes();
        return allSuperTypeNames;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeTypeDefinition#getDeclaredChildNodeDefinitions()
     */
    @Override
    public NodeDefinition[] getDeclaredChildNodeDefinitions() {
        return childNodeDefinitions.values().toArray(new NodeDefinition[childNodeDefinitions.size()]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeType#getChildNodeDefinitions()
     */
    @Override
    public NodeDefinition[] getChildNodeDefinitions() {
        Collection<ChildNodeDefinition> allDefns = allChildNodeDefinitions();
        return allDefns.toArray(new javax.jcr.nodetype.NodeDefinition[allDefns.size()]);
    }

    protected Collection<ChildNodeDefinition> declaredChildNodeDefinitions() {
        return childNodeDefinitions.values();
    }

    protected Collection<ChildNodeDefinition> allChildNodeDefinitions() {
        if (this.allChildNodeDefinitions == null) {
            Map<ChildNodeDefinition.Id, ChildNodeDefinition> allDefns = new HashMap<ChildNodeDefinition.Id, ChildNodeDefinition>();
            // Add the declared child node definitions for this node ...
            allDefns.putAll(childNodeDefinitions);
            for (NodeType superType : allSuperTypes()) {
                for (ChildNodeDefinition childDefn : superType.declaredChildNodeDefinitions()) {
                    if (!allDefns.containsKey(childDefn.id())) {
                        allDefns.put(childDefn.id(), childDefn);
                    }
                }
            }
            this.allChildNodeDefinitions = allDefns;
        }
        return this.allChildNodeDefinitions.values();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeTypeDefinition#getDeclaredPropertyDefinitions()
     */
    @Override
    public javax.jcr.nodetype.PropertyDefinition[] getDeclaredPropertyDefinitions() {
        return propertyDefinitions.values().toArray(new javax.jcr.nodetype.PropertyDefinition[propertyDefinitions.size()]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeType#getPropertyDefinitions()
     */
    @Override
    public javax.jcr.nodetype.PropertyDefinition[] getPropertyDefinitions() {
        Collection<PropertyDefinition> allDefns = allPropertyDefinitions();
        return allDefns.toArray(new javax.jcr.nodetype.PropertyDefinition[allDefns.size()]);
    }

    protected Collection<PropertyDefinition> declaredPropertyDefinitions() {
        return propertyDefinitions.values();
    }

    protected Collection<PropertyDefinition> allPropertyDefinitions() {
        if (this.allPropertyDefinitions == null) {
            Map<PropertyDefinition.Id, PropertyDefinition> allDefns = new HashMap<PropertyDefinition.Id, PropertyDefinition>();
            // Add the declared child node definitions for this node ...
            allDefns.putAll(propertyDefinitions);
            for (NodeType superType : allSuperTypes()) {
                for (PropertyDefinition propDefn : superType.declaredPropertyDefinitions()) {
                    if (!allDefns.containsKey(propDefn.id())) {
                        allDefns.put(propDefn.id(), propDefn);
                    }
                }
            }
            this.allPropertyDefinitions = allDefns;
        }
        return this.allPropertyDefinitions.values();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeTypeDefinition#getPrimaryItemName()
     */
    @Override
    public String getPrimaryItemName() {
        return primaryItemName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeTypeDefinition#hasOrderableChildNodes()
     */
    @Override
    public boolean hasOrderableChildNodes() {
        return hasOrderableChildNodes;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeTypeDefinition#isAbstract()
     */
    @Override
    public boolean isAbstract() {
        return isAbstract;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeTypeDefinition#isMixin()
     */
    @Override
    public boolean isMixin() {
        return isMixin;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeTypeDefinition#isQueryable()
     */
    @Override
    public boolean isQueryable() {
        return isQueryable;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeType#isNodeType(java.lang.String)
     */
    @Override
    public boolean isNodeType( String nodeTypeName ) {
        if (nodeTypeName == null) return false;
        if (this.name.equals(nodeTypeName)) return true;
        return allSuperTypeNames().contains(nodeTypeName);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.web.jcr.rest.client.domain.IModeShapeObject#getShortDescription()
     */
    public String getShortDescription() {
        return RestClientI18n.nodeTypeShortDescription.text(this.name);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation always returns false.
     * </p>
     * 
     * @see javax.jcr.nodetype.NodeType#canAddChildNode(java.lang.String)
     */
    @Override
    public boolean canAddChildNode( String childNodeName ) {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeType#canAddChildNode(java.lang.String, java.lang.String)
     */
    @Override
    public boolean canAddChildNode( String childNodeName,
                                    String nodeTypeName ) {
        return false;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation always returns false.
     * </p>
     * 
     * @see javax.jcr.nodetype.NodeType#canRemoveItem(java.lang.String)
     */
    @Override
    public boolean canRemoveItem( String itemName ) {
        return false;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation always returns false.
     * </p>
     * 
     * @see javax.jcr.nodetype.NodeType#canRemoveNode(java.lang.String)
     */
    @Override
    public boolean canRemoveNode( String nodeName ) {
        return false;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation always returns false.
     * </p>
     * 
     * @see javax.jcr.nodetype.NodeType#canRemoveProperty(java.lang.String)
     */
    @Override
    public boolean canRemoveProperty( String propertyName ) {
        return false;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation always returns false.
     * </p>
     * 
     * @see javax.jcr.nodetype.NodeType#canSetProperty(java.lang.String, javax.jcr.Value)
     */
    @Override
    public boolean canSetProperty( String propertyName,
                                   Value value ) {
        return false;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation always returns false.
     * </p>
     * 
     * @see javax.jcr.nodetype.NodeType#canSetProperty(java.lang.String, javax.jcr.Value[])
     */
    @Override
    public boolean canSetProperty( String propertyName,
                                   Value[] values ) {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return name.hashCode();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (this == obj) return true;
        if (obj instanceof NodeType) {

        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        sb.append(name);
        sb.append(']');
        if (getDeclaredSupertypeNames().length != 0) {
            sb.append(" > ");
            boolean first = true;
            for (String typeName : getDeclaredSupertypeNames()) {
                if (typeName == null) continue;
                if (first) first = false;
                else sb.append(',');
                sb.append(typeName);
            }
        }
        if (isAbstract()) sb.append(" abstract");
        if (isMixin()) sb.append(" mixin");
        if (!isQueryable()) sb.append(" noquery");
        if (hasOrderableChildNodes()) sb.append(" orderable");
        if (getPrimaryItemName() != null) {
            sb.append(" primaryitem ").append(getPrimaryItemName());
        }
        for (PropertyDefinition propDefn : declaredPropertyDefinitions()) {
            sb.append('\n').append(propDefn);
        }
        for (ChildNodeDefinition childDefn : declaredChildNodeDefinitions()) {
            sb.append('\n').append(childDefn);
        }
        sb.append('\n');
        return sb.toString();
    }

    protected static class NodeTypes implements NodeTypeIterator {
        private final List<NodeType> nodeTypes;
        private final Iterator<NodeType> iter;
        private long position;

        protected NodeTypes( List<NodeType> nodeTypes ) {
            this.nodeTypes = nodeTypes;
            this.iter = nodeTypes.iterator();
            this.position = 0L;
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.nodetype.NodeTypeIterator#nextNodeType()
         */
        @Override
        public javax.jcr.nodetype.NodeType nextNodeType() {
            NodeType next = iter.next();
            ++position;
            return next;
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.RangeIterator#getPosition()
         */
        @Override
        public long getPosition() {
            return position;
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.RangeIterator#getSize()
         */
        @Override
        public long getSize() {
            return nodeTypes.size();
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.RangeIterator#skip(long)
         */
        @Override
        public void skip( long skipNum ) {
            for (long i = 0; i != skipNum; ++i) {
                if (!hasNext()) return;
                nextNodeType();
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#hasNext()
         */
        @Override
        public boolean hasNext() {
            return iter.hasNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#next()
         */
        @Override
        public Object next() {
            return nextNodeType();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.util.Iterator#remove()
         */
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

}
