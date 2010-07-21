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

import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.nodetype.ConstraintViolationException;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.property.ValueFactories;
import org.modeshape.jcr.nodetype.PropertyDefinitionTemplate;

/**
 * ModeShape implementation of the JCR 2 PropertyDefinitionTemplate interface.
 */
class JcrPropertyDefinitionTemplate extends JcrItemDefinitionTemplate implements PropertyDefinitionTemplate {

    private boolean multiple = false;
    private Value[] defaultValues = null;
    private int requiredType = PropertyType.STRING;
    private String[] valueConstraints = null;
    private boolean fullTextSearchable = true;
    private boolean queryOrderable = true;
    private String[] availableQueryOperators;

    JcrPropertyDefinitionTemplate( ExecutionContext context ) {
        super(context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.nodetype.PropertyDefinitionTemplate#setDefaultValues(java.lang.String[])
     */
    public void setDefaultValues( String[] defaultValues ) {
        if (defaultValues != null) {
            this.defaultValues = new Value[defaultValues.length];
            ValueFactories factories = getExecutionContext().getValueFactories();
            for (int i = 0; i < defaultValues.length; i++) {
                this.defaultValues[i] = new JcrValue(factories, null, PropertyType.STRING, defaultValues[i]);
            }
        } else {
            this.defaultValues = null;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.nodetype.PropertyDefinitionTemplate#setDefaultValues(Value[])
     */
    public void setDefaultValues( Value[] defaultValues ) {
        this.defaultValues = defaultValues;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.nodetype.PropertyDefinitionTemplate#setMultiple(boolean)
     */
    public void setMultiple( boolean multiple ) {
        this.multiple = multiple;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.nodetype.PropertyDefinitionTemplate#setRequiredType(int)
     */
    public void setRequiredType( int requiredType ) {
        assert requiredType == PropertyType.BINARY || requiredType == PropertyType.BOOLEAN || requiredType == PropertyType.DATE
               || requiredType == PropertyType.DOUBLE || requiredType == PropertyType.DECIMAL
               || requiredType == PropertyType.LONG || requiredType == PropertyType.NAME || requiredType == PropertyType.PATH
               || requiredType == PropertyType.REFERENCE || requiredType == PropertyType.WEAKREFERENCE
               || requiredType == PropertyType.URI || requiredType == PropertyType.STRING
               || requiredType == PropertyType.UNDEFINED;
        this.requiredType = requiredType;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.nodetype.PropertyDefinitionTemplate#setValueConstraints(java.lang.String[])
     */
    public void setValueConstraints( String[] constraints ) {
        this.valueConstraints = constraints;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.PropertyDefinition#getDefaultValues()
     */
    public Value[] getDefaultValues() {
        return this.defaultValues;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.PropertyDefinition#getRequiredType()
     */
    public int getRequiredType() {
        return requiredType;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.PropertyDefinition#getValueConstraints()
     */
    public String[] getValueConstraints() {
        return valueConstraints;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.PropertyDefinition#isMultiple()
     */
    public boolean isMultiple() {
        return multiple;
    }

    public boolean isFullTextSearchable() {
        return this.fullTextSearchable;
    }

    /**
     * {@inheritDoc}
     * 
     * @see PropertyDefinitionTemplate#setFullTextSearchable(boolean)
     */
    public void setFullTextSearchable( boolean fullTextSearchable ) {
        this.fullTextSearchable = fullTextSearchable;
    }

    public String[] getAvailableQueryOperators() {
        return this.availableQueryOperators;
    }

    /**
     * {@inheritDoc}
     * 
     * @see PropertyDefinitionTemplate#setAvailableQueryOperators(String[])
     */
    public void setAvailableQueryOperators( String[] queryOperators ) {
        this.availableQueryOperators = queryOperators;
    }

    public boolean isQueryOrderable() {
        return this.queryOrderable;
    }

    /**
     * {@inheritDoc}
     * 
     * @see PropertyDefinitionTemplate#setQueryOrderable(boolean)
     */
    public void setQueryOrderable( boolean queryOrderable ) {
        this.queryOrderable = queryOrderable;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.PropertyDefinitionTemplate#setName(java.lang.String)
     */
    @Override
    public void setName( String name ) throws ConstraintViolationException {
        super.setName(name);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.PropertyDefinitionTemplate#setAutoCreated(boolean)
     */
    @Override
    public void setAutoCreated( boolean autoCreated ) {
        super.setAutoCreated(autoCreated);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.PropertyDefinitionTemplate#setMandatory(boolean)
     */
    @Override
    public void setMandatory( boolean mandatory ) {
        super.setMandatory(mandatory);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.PropertyDefinitionTemplate#setOnParentVersion(int)
     */
    @Override
    public void setOnParentVersion( int onParentVersion ) {
        super.setOnParentVersion(onParentVersion);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.PropertyDefinitionTemplate#setProtected(boolean)
     */
    @Override
    public void setProtected( boolean isProtected ) {
        super.setProtected(isProtected);
    }
}
