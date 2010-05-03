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
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.jcr.nodetype.PropertyDefinitionTemplate;

/**
 * ModeShape implementation of the JCR 2 PropertyDefinitionTemplate interface.
 */
class JcrPropertyDefinitionTemplate extends JcrItemDefinitionTemplate implements PropertyDefinitionTemplate {

    private boolean multiple = false;
    private String[] defaultValues;
    private int requiredType = PropertyType.STRING;
    private String[] valueConstraints = new String[0];
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
        CheckArg.isNotNull(defaultValues, "defaultValues");
        this.defaultValues = defaultValues;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.nodetype.PropertyDefinitionTemplate#setDefaultValues(Value[])
     */
    public void setDefaultValues( Value[] defaultValues ) {
        CheckArg.isNotNull(defaultValues, "defaultValues");
        this.defaultValues = new String[defaultValues.length];

        try {
            for (int i = 0; i < defaultValues.length; i++) {
                this.defaultValues[i] = defaultValues[i].getString();
            }
        } catch (RepositoryException re) {
            throw new IllegalStateException(re);
        }
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
               || requiredType == PropertyType.DOUBLE || requiredType == PropertyType.LONG || requiredType == PropertyType.NAME
               || requiredType == PropertyType.PATH || requiredType == PropertyType.REFERENCE
               || requiredType == PropertyType.STRING || requiredType == PropertyType.UNDEFINED;
        this.requiredType = requiredType;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.nodetype.PropertyDefinitionTemplate#setValueConstraints(java.lang.String[])
     */
    public void setValueConstraints( String[] constraints ) {
        CheckArg.isNotNull(constraints, "constraints");
        this.valueConstraints = constraints;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.PropertyDefinition#getDefaultValues()
     */
    public Value[] getDefaultValues() {
        return null;
    }

    String[] getInternalDefaultValues() {
        return defaultValues;
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
}
