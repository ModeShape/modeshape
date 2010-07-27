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

import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeType;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NameFactory;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.ValueFormatException;
import org.modeshape.jcr.nodetype.NodeDefinitionTemplate;

/**
 * ModeShape implementation of the JCR 2 NodeDefinitionTemplate interface
 */
@NotThreadSafe
class JcrNodeDefinitionTemplate extends JcrItemDefinitionTemplate implements NodeDefinitionTemplate {

    private Name defaultPrimaryType;
    private Name[] requiredPrimaryTypes;
    private boolean allowSameNameSiblings;

    JcrNodeDefinitionTemplate( ExecutionContext context ) {
        super(context);
    }

    JcrNodeDefinitionTemplate( JcrNodeDefinitionTemplate original,
                               ExecutionContext context ) {
        super(original, context);
        this.defaultPrimaryType = original.defaultPrimaryType;
        this.requiredPrimaryTypes = original.requiredPrimaryTypes;
        this.allowSameNameSiblings = original.allowSameNameSiblings;
        JcrItemDefinitionTemplate.registerMissingNamespaces(original.getContext(), context, this.defaultPrimaryType);
        JcrItemDefinitionTemplate.registerMissingNamespaces(original.getContext(), context, this.requiredPrimaryTypes);
    }

    JcrNodeDefinitionTemplate with( ExecutionContext context ) {
        return context == super.getContext() ? this : new JcrNodeDefinitionTemplate(this, context);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Passing a null or blank name is equivalent to "unsetting" (or removing) the primary item name.
     * </p>
     * 
     * @see org.modeshape.jcr.nodetype.NodeDefinitionTemplate#setDefaultPrimaryType(String)
     */
    public void setDefaultPrimaryType( String defaultPrimaryType ) throws ConstraintViolationException {
        setDefaultPrimaryTypeName(defaultPrimaryType);
    }

    /**
     * Set the name of the primary type that should be used by default when creating children using this node definition
     * 
     * @param defaultPrimaryType the default primary type for this child node, or null if there is to be no default primary type
     * @throws ConstraintViolationException
     */
    public void setDefaultPrimaryTypeName( String defaultPrimaryType ) throws ConstraintViolationException {
        if (defaultPrimaryType == null || defaultPrimaryType.trim().length() == 0) {
            this.defaultPrimaryType = null;
        } else {
            try {
                this.defaultPrimaryType = getContext().getValueFactories().getNameFactory().create(defaultPrimaryType);
            } catch (ValueFormatException vfe) {
                throw new ConstraintViolationException(vfe);
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.nodetype.NodeDefinitionTemplate#setRequiredPrimaryTypes(java.lang.String[])
     * @deprecated As of ModeShape 2.0, use {@link #setRequiredPrimaryTypeNames(String[])} instead
     */
    @SuppressWarnings( "dep-ann" )
    public void setRequiredPrimaryTypes( String[] requiredPrimaryTypes ) throws ConstraintViolationException {
        setRequiredPrimaryTypeNames(requiredPrimaryTypes);
    }

    /**
     * {@inheritDoc}
     * 
     * @see NodeDefinitionTemplate#setRequiredPrimaryTypeNames(String[])
     */
    public void setRequiredPrimaryTypeNames( String[] requiredPrimaryTypes ) throws ConstraintViolationException {
        if (requiredPrimaryTypes == null) {
            throw new ConstraintViolationException(JcrI18n.badNodeTypeName.text("requiredPrimaryTypes"));
        }

        NameFactory nameFactory = getContext().getValueFactories().getNameFactory();
        Name[] rpts = new Name[requiredPrimaryTypes.length];
        for (int i = 0; i < requiredPrimaryTypes.length; i++) {
            try {
                rpts[i] = nameFactory.create(requiredPrimaryTypes[i]);
            } catch (ValueFormatException vfe) {
                throw new ConstraintViolationException(vfe);
            }
        }

        this.requiredPrimaryTypes = rpts;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.nodetype.NodeDefinitionTemplate#setSameNameSiblings(boolean)
     */
    public void setSameNameSiblings( boolean allowSameNameSiblings ) {
        this.allowSameNameSiblings = allowSameNameSiblings;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeDefinition#allowsSameNameSiblings()
     */
    public boolean allowsSameNameSiblings() {
        return allowSameNameSiblings;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeDefinition#getDefaultPrimaryType()
     */
    public NodeType getDefaultPrimaryType() {
        return null;
    }

    public String getDefaultPrimaryTypeName() {
        if (defaultPrimaryType == null) return null;
        return defaultPrimaryType.getString(getContext().getNamespaceRegistry());
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeDefinition#getRequiredPrimaryTypes()
     */
    public NodeType[] getRequiredPrimaryTypes() {
        // This method should return null since it's not attached to a registered node type ...
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeDefinition#getRequiredPrimaryTypeNames()
     */
    public String[] getRequiredPrimaryTypeNames() {
        if (requiredPrimaryTypes == null) return null;

        NamespaceRegistry registry = getContext().getNamespaceRegistry();
        String[] rpts = new String[requiredPrimaryTypes.length];
        for (int i = 0; i < requiredPrimaryTypes.length; i++) {
            rpts[i] = requiredPrimaryTypes[i].getString(registry);
        }
        return rpts;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeDefinitionTemplate#setName(java.lang.String)
     */
    @Override
    public void setName( String name ) throws ConstraintViolationException {
        super.setName(name);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeDefinitionTemplate#setAutoCreated(boolean)
     */
    @Override
    public void setAutoCreated( boolean autoCreated ) {
        super.setAutoCreated(autoCreated);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeDefinitionTemplate#setMandatory(boolean)
     */
    @Override
    public void setMandatory( boolean mandatory ) {
        super.setMandatory(mandatory);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeDefinitionTemplate#setOnParentVersion(int)
     */
    @Override
    public void setOnParentVersion( int onParentVersion ) {
        super.setOnParentVersion(onParentVersion);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.NodeDefinitionTemplate#setProtected(boolean)
     */
    @Override
    public void setProtected( boolean isProtected ) {
        super.setProtected(isProtected);
    }
}
