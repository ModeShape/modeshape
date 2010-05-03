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
import org.modeshape.jcr.nodetype.NodeDefinitionTemplate;
import org.modeshape.jcr.nodetype.NodeTypeTemplate;
import org.modeshape.jcr.nodetype.PropertyDefinitionTemplate;

/**
 * ModeShape implementation of the JCR NodeTypeTemplate interface
 */
@NotThreadSafe
public class JcrNodeTypeTemplate implements NodeTypeTemplate {

    private final ExecutionContext context;
    private final List<NodeDefinitionTemplate> nodeDefinitionTemplates = new ArrayList<NodeDefinitionTemplate>();
    private final List<PropertyDefinitionTemplate> propertyDefinitionTemplates = new ArrayList<PropertyDefinitionTemplate>();
    private boolean isAbstract;
    private boolean queryable = true;
    private boolean mixin;
    private boolean orderableChildNodes;
    private String[] declaredSupertypeNames;
    private String name;
    private String primaryItemName;

    JcrNodeTypeTemplate( ExecutionContext context ) {
        assert context != null;

        this.context = context;
    }

    ExecutionContext getExecutionContext() {
        return context;
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
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.nodetype.NodeTypeTemplate#setDeclaredSupertypeNames(java.lang.String[])
     */
    public void setDeclaredSupertypeNames( String[] names ) {
        CheckArg.isNotNull(names, "names");
        this.declaredSupertypeNames = names;
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
        this.name = name;
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
        this.primaryItemName = name;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.nodetype.NodeTypeDefinition#getDeclaredNodeDefinitions()
     */
    public NodeDefinition[] getDeclaredNodeDefinitions() {
        return null; // per JSR-283 specification (section 4.7.10)
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.nodetype.NodeTypeDefinition#getDeclaredPropertyDefinitions()
     */
    public PropertyDefinition[] getDeclaredPropertyDefinitions() {
        return null; // per JSR-283 specification (section 4.7.10)
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.nodetype.NodeTypeDefinition#getDeclaredSupertypes()
     */
    public String[] getDeclaredSupertypes() {
        return declaredSupertypeNames;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.nodetype.NodeTypeDefinition#getName()
     */
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.nodetype.NodeTypeDefinition#getPrimaryItemName()
     */
    public String getPrimaryItemName() {
        return primaryItemName;
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
