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
package org.modeshape.jcr.nodetype;

import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.OnParentVersionAction;
import net.jcip.annotations.NotThreadSafe;

/**
 * A template that can be used to create new property definitions, patterned after the approach in the proposed <a
 * href="http://jcp.org/en/jsr/detail?id=283">JSR-283</a>. This interface extends the standard {@link PropertyDefinition}
 * interface and adds setter methods for the various attributes.
 * 
 * @see NodeTypeTemplate#getDeclaredPropertyDefinitions()
 */
@NotThreadSafe
public interface PropertyDefinitionTemplate extends javax.jcr.nodetype.PropertyDefinitionTemplate {

    /**
     * Set the name of the property definition
     * 
     * @param name the name
     */
    public void setName( String name );

    /**
     * Set whether this definition describes a child node that is auto-created by the system.
     * 
     * @param autoCreated true if this child should be auto-created
     */
    public void setAutoCreated( boolean autoCreated );

    /**
     * Set whether this definition describes a child that is required (mandatory).
     * 
     * @param mandatory true if the child is mandatory
     */
    public void setMandatory( boolean mandatory );

    /**
     * Set the mode for the versioning of the child with respect to versioning of the parent.
     * 
     * @param opv the on-parent versioning mode; one of {@link OnParentVersionAction} values.
     */
    public void setOnParentVersion( int opv );

    /**
     * Set whether the child node described by this definition is protected from changes through the JCR API.
     * 
     * @param isProtected true if the child node is protected, or false if it may be changed through the JCR API
     */
    public void setProtected( boolean isProtected );

    /**
     * Set the required property type for the values of the property, or {@link PropertyType#UNDEFINED} if there is no type
     * requirement
     * 
     * @param requiredType the required type for the property values
     */
    public void setRequiredType( int requiredType );

    /**
     * Set the constraint expressions for the values of the property. See {@link PropertyDefinition#getValueConstraints()} for
     * more details about the formats of the constraints.
     * 
     * @param constraints the value constraints, or null or an empty array if there are no constraints.
     */
    public void setValueConstraints( String[] constraints );

    /**
     * Set the default values for the property, using their string representation. See
     * {@link PropertyDefinition#getDefaultValues()} for more details.
     * 
     * @param defaultValues the string representation of the default values, or null or an empty array if there are no default
     *        values
     */
    public void setDefaultValues( String[] defaultValues );

    /**
     * Set the default values for the property, using their {link Value} representation. See
     * {@link PropertyDefinition#getDefaultValues()} for more details.
     * 
     * @param values the value representation of the default values, or null or an empty array if there are no default values
     */
    public void setDefaultValues( Value[] values );

    /**
     * Set whether the properties described by this definition may have multiple values.
     * 
     * @param multiple true if the properties may have multiple values, or false if they are limited to one value each
     */
    public void setMultiple( boolean multiple );

    /**
     * @return whether the properties described by this definition should be searchable in a full-text search
     */
    public boolean isFullTextSearchable();

    /**
     * Set whether the properties described by this definition should be searchable in a full-text search
     * 
     * @param fullTextSearchable whether the properties described by this definition should be searchable in a full-text search
     */
    public void setFullTextSearchable( boolean fullTextSearchable );

    /**
     * @return whether the query results containing properties described by this definition should be able to be ordered by those
     *         properties.
     */
    public boolean isQueryOrderable();

    /**
     * Set whether the query results containing properties described by this definition should be able to be ordered by those
     * properties.
     * 
     * @param queryOrderable whether the query results containing properties described by this definition should be able to be
     *        ordered by those properties.
     */
    public void setQueryOrderable( boolean queryOrderable );

    /**
     * @return the available operators for use in property constraints that reference properties described by this definition.
     */
    public String[] getAvailableQueryOperators();

    /**
     * Sets the available operators for use in property constraints that reference properties described by this definition.
     * 
     * @param queryOperators the available operators for use in property constraints that reference properties described by this
     *        definition.
     */
    public void setAvailableQueryOperators( String[] queryOperators );
}
