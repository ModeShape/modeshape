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

import javax.jcr.PropertyType;
import javax.jcr.Value;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.jcr.nodetype.PropertyDefinitionTemplate;

/**
 * DNA implementation of the JCR 2 PropertyDefinitionTemplate interface.
 */
class JcrPropertyDefinitionTemplate extends JcrItemDefinitionTemplate implements PropertyDefinitionTemplate {

    private boolean multiple = false;
    private String[] defaultValues;
    private int requiredType = PropertyType.STRING;
    private String[] valueConstraints = new String[0];

    JcrPropertyDefinitionTemplate( ExecutionContext context ) {
        super(context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.jcr.nodetype.PropertyDefinitionTemplate#setDefaultValues(java.lang.String[])
     */
    public void setDefaultValues( String[] defaultValues ) {
        CheckArg.isNotNull(defaultValues, "defaultValues");
        this.defaultValues = defaultValues;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.jcr.nodetype.PropertyDefinitionTemplate#setMultiple(boolean)
     */
    public void setMultiple( boolean multiple ) {
        this.multiple = multiple;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.jcr.nodetype.PropertyDefinitionTemplate#setRequiredType(int)
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
     * @see org.jboss.dna.jcr.nodetype.PropertyDefinitionTemplate#setValueConstraints(java.lang.String[])
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

}
