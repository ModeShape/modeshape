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
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.PropertyDefinition;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.ValueFormatException;
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
    private final boolean createdFromExistingDefinition;
    private boolean isAbstract;
    private boolean queryable = true;
    private boolean mixin;
    private boolean orderableChildNodes;
    private Name[] declaredSupertypeNames;
    private Name name;
    private Name primaryItemName;

    JcrNodeTypeTemplate( ExecutionContext context ) {
        this(context, false);
    }

    JcrNodeTypeTemplate( ExecutionContext context,
                         boolean createdFromExistingDefinition ) {
        assert context != null;

        this.context = context;
        this.createdFromExistingDefinition = createdFromExistingDefinition;
    }

    JcrNodeTypeTemplate( JcrNodeTypeTemplate original,
                         ExecutionContext context ) {
        this.context = context;
        this.isAbstract = original.isAbstract;
        this.queryable = original.queryable;
        this.mixin = original.mixin;
        this.name = original.name;
        this.orderableChildNodes = original.orderableChildNodes;
        this.declaredSupertypeNames = original.declaredSupertypeNames;
        this.primaryItemName = original.primaryItemName;
        JcrItemDefinitionTemplate.registerMissingNamespaces(original.context, context, this.name);
        JcrItemDefinitionTemplate.registerMissingNamespaces(original.context, context, this.declaredSupertypeNames);
        JcrItemDefinitionTemplate.registerMissingNamespaces(original.context, context, this.primaryItemName);
        for (NodeDefinitionTemplate childDefn : original.nodeDefinitionTemplates) {
            this.nodeDefinitionTemplates.add(((JcrNodeDefinitionTemplate)childDefn).with(context));
        }
        for (PropertyDefinitionTemplate propDefn : original.propertyDefinitionTemplates) {
            this.propertyDefinitionTemplates.add(((JcrPropertyDefinitionTemplate)propDefn).with(context));
        }
        this.createdFromExistingDefinition = original.createdFromExistingDefinition;
    }

    JcrNodeTypeTemplate with( ExecutionContext context ) {
        return context == this.context ? this : new JcrNodeTypeTemplate(this, context);
    }

    ExecutionContext getExecutionContext() {
        return context;
    }

    private String string( Name name ) {
        if (name == null) return null;
        return name.getString(context.getNamespaceRegistry());
    }

    Name[] declaredSupertypeNames() {
        return this.declaredSupertypeNames;
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
     * Set the direct supertypes for this node type.
     * 
     * @param names the names of the direct supertypes, or empty or null if there are none.
     * @throws ConstraintViolationException
     */
    public void setDeclaredSuperTypeNames( String[] names ) throws ConstraintViolationException {
        if (names == null) {
            throw new ConstraintViolationException(JcrI18n.badNodeTypeName.text("names"));
        }

        Name[] supertypeNames = new Name[names.length];

        for (int i = 0; i < names.length; i++) {
            CheckArg.isNotEmpty(names[i], "names[" + i + "");
            try {
                supertypeNames[i] = context.getValueFactories().getNameFactory().create(names[i]);
            } catch (ValueFormatException vfe) {
                throw new ConstraintViolationException(vfe);
            }
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
    public void setName( String name ) throws ConstraintViolationException {
        CheckArg.isNotEmpty(name, "name");
        try {
            this.name = context.getValueFactories().getNameFactory().create(name);
        } catch (ValueFormatException vfe) {
            throw new ConstraintViolationException(vfe);
        }
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
     * <p>
     * Passing a null or blank name is equivalent to "unsetting" (or removing) the primary item name.
     * </p>
     * 
     * @see org.modeshape.jcr.nodetype.NodeTypeDefinition#getPrimaryItemName()
     *      type.NodeTypeTemplate#setPrimaryItemName(java.lang.String)
     */
    public void setPrimaryItemName( String name ) throws ConstraintViolationException {
        if (name == null || name.trim().length() == 0) {
            this.primaryItemName = null;
        } else {
            try {
                this.primaryItemName = context.getValueFactories().getNameFactory().create(name);
            } catch (ValueFormatException vfe) {
                throw new ConstraintViolationException(vfe);
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see NodeTypeDefinition#getDeclaredChildNodeDefinitions()
     */
    public NodeDefinition[] getDeclaredChildNodeDefinitions() {
        if (!createdFromExistingDefinition && nodeDefinitionTemplates.isEmpty()) return null;

        return nodeDefinitionTemplates.toArray(new NodeDefinition[nodeDefinitionTemplates.size()]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.nodetype.NodeTypeDefinition#getDeclaredPropertyDefinitions()
     */
    public PropertyDefinition[] getDeclaredPropertyDefinitions() {
        if (!createdFromExistingDefinition && propertyDefinitionTemplates.isEmpty()) return null;

        return propertyDefinitionTemplates.toArray(new PropertyDefinition[propertyDefinitionTemplates.size()]);
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
