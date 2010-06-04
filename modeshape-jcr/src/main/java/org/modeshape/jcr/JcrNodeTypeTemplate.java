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
import java.util.List;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.PropertyDefinition;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.property.Name;
import org.modeshape.jcr.nodetype.NodeDefinitionTemplate;
import org.modeshape.jcr.nodetype.NodeTypeDefinition;
import org.modeshape.jcr.nodetype.NodeTypeTemplate;
import org.modeshape.jcr.nodetype.PropertyDefinitionTemplate;

/**
 * ModeShape implementation of the JCR NodeTypeTemplate interface
 */
@NotThreadSafe
public class JcrNodeTypeTemplate implements NodeTypeDefinition, NodeTypeTemplate {

    private final ExecutionContext context;
    private final List<NodeDefinitionTemplate> nodeDefinitionTemplates = new ArrayList<NodeDefinitionTemplate>();
    private final List<PropertyDefinitionTemplate> propertyDefinitionTemplates = new ArrayList<PropertyDefinitionTemplate>();
    private boolean isAbstract;
    private boolean queryable = true;
    private boolean mixin;
    private boolean orderableChildNodes;
    private Name[] declaredSupertypeNames;
    private Name name;
    private Name primaryItemName;

    JcrNodeTypeTemplate( ExecutionContext context ) {
        assert context != null;

        this.context = context;
    }

    ExecutionContext getExecutionContext() {
        return context;
    }

    private String string( Name name ) {
        if (name == null) return null;
        return name.getString(context.getNamespaceRegistry());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.nodetype.NodeTypeTemplate#getNodeDefinitionTemplates()
     */
    public List<NodeDefinitionTemplate> getNodeDefinitionTemplates() {
        return nodeDefinitionTemplates;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.nodetype.NodeTypeTemplate#getPropertyDefinitionTemplates()
     */
    public List<PropertyDefinitionTemplate> getPropertyDefinitionTemplates() {
        return propertyDefinitionTemplates;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.nodetype.NodeTypeTemplate#setAbstract(boolean)
     */
    public void setAbstract( boolean isAbstract ) {
        this.isAbstract = isAbstract;
    }

    /**
     * @param names the names of the supertypes
     * @see org.modeshape.jcr.nodetype.NodeTypeTemplate#setDeclaredSupertypeNames(java.lang.String[])
     * @deprecated Use {@link #setDeclaredSuperTypeNames(String[])} instead
     */
    @SuppressWarnings( "dep-ann" )
    public void setDeclaredSupertypeNames( String[] names ) {
        setDeclaredSuperTypeNames(names);
    }

    /**
     * Set the direct supertypes for this node type.
     * 
     * @param names the names of the direct supertypes, or empty or null if there are none.
     */
    public void setDeclaredSuperTypeNames( String[] names ) {
        CheckArg.isNotNull(names, "names");

        Name[] supertypeNames = new Name[names.length];

        for (int i = 0; i < names.length; i++) {
            CheckArg.isNotEmpty(names[i], "names[" + i + "");
            supertypeNames[i] = context.getValueFactories().getNameFactory().create(names[i]);
        }
        this.declaredSupertypeNames = supertypeNames;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.nodetype.NodeTypeTemplate#setMixin(boolean)
     */
    public void setMixin( boolean mixin ) {
        this.mixin = mixin;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.nodetype.NodeTypeTemplate#setName(java.lang.String)
     */
    public void setName( String name ) {
        CheckArg.isNotEmpty(name, "name");
        this.name = context.getValueFactories().getNameFactory().create(name);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.nodetype.NodeTypeTemplate#setOrderableChildNodes(boolean)
     */
    public void setOrderableChildNodes( boolean orderable ) {
        this.orderableChildNodes = orderable;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.nodetype.NodeTypeDefinition#getPrimaryItemName()
     *      type.NodeTypeTemplate#setPrimaryItemName(java.lang.String)
     */
    public void setPrimaryItemName( String name ) {
        this.primaryItemName = context.getValueFactories().getNameFactory().create(name);
    }

    /**
     * @return the list of node definitions
     * @see org.modeshape.jcr.nodetype.NodeTypeDefinition#getDeclaredNodeDefinitions()
     * @deprecated use {@link #getDeclaredChildNodeDefinitions()} instead
     */
    @SuppressWarnings( "dep-ann" )
    public NodeDefinition[] getDeclaredNodeDefinitions() {
        return getDeclaredChildNodeDefinitions();
    }

    /**
     * Get the array of child node definition templates for this node type. This method always returns null from a {@code
     * JcrNodeTypeTemplate}, as the method is only meaningful for registered types.
     * 
     * @return null always
     */
    public NodeDefinition[] getDeclaredChildNodeDefinitions() {
        return null;
    }

    /**
     * {@inheritDoc} This method always returns null from a {@code JcrNodeTypeTemplate}, as the method is only meaningful for
     * registered types.
     * 
     * @return null always
     * @see org.modeshape.jcr.nodetype.NodeTypeDefinition#getDeclaredPropertyDefinitions()
     */
    public PropertyDefinition[] getDeclaredPropertyDefinitions() {
        return null;
    }

    /**
     * @return the names of the declared supertypes
     * @see org.modeshape.jcr.nodetype.NodeTypeDefinition#getDeclaredSupertypes()
     * @deprecated Use {@link #getDeclaredSupertypeNames()} instead
     */
    @Deprecated
    public String[] getDeclaredSupertypes() {
        return getDeclaredSupertypeNames();
    }

    /**
     * Get the direct supertypes for this node type.
     * 
     * @return the names of the direct supertypes, or an empty array if there are none
     */
    public String[] getDeclaredSupertypeNames() {
        if (declaredSupertypeNames == null) return new String[0];
        String[] names = new String[declaredSupertypeNames.length];

        for (int i = 0; i < declaredSupertypeNames.length; i++) {
            names[i] = declaredSupertypeNames[i].getString(context.getNamespaceRegistry());
        }
        return names;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.nodetype.NodeTypeDefinition#getName()
     */
    public String getName() {
        return string(name);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.nodetype.NodeTypeDefinition#getPrimaryItemName()
     */
    public String getPrimaryItemName() {
        return string(primaryItemName);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.nodetype.NodeTypeDefinition#hasOrderableChildNodes()
     */
    public boolean hasOrderableChildNodes() {
        return orderableChildNodes;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.nodetype.NodeTypeDefinition#isAbstract()
     */
    public boolean isAbstract() {
        return isAbstract;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.nodetype.NodeTypeDefinition#isMixin()
     */
    public boolean isMixin() {
        return mixin;
    }

    /**
     * Get whether this node is queryable
     * 
     * @return true if the node is queryable; false otherwise
     */
    public boolean isQueryable() {
        return queryable;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.nodetype.NodeTypeTemplate#setQueryable(boolean)
     */
    public void setQueryable( boolean queryable ) {
        this.queryable = queryable;
    }
}
