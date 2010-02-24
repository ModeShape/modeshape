/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in ModeShape is licensed
 * to you under the terms of the GNU Lesser General Public License as
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

import java.io.Serializable;
import java.util.HashMap;
import javax.jcr.PropertyType;
import net.jcip.annotations.Immutable;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NameFactory;
import org.modeshape.graph.property.ValueFormatException;

/**
 * An immutable identifier for a property definition. Although instances can be serialized, the property definitions are often
 * stored within the graph as {@link #getString() string values} on a property. These string values can later be
 * {@link #fromString(String, NameFactory) parsed} to reconstruct the identifier. Note that this string representation does not
 * use namespace prefixes, so they are long-lasting and durable.
 * <p>
 * What distinguishes one property definition from another is not well documented in the JSR-170 specification. The closest this
 * version of the spec gets is Section 6.7.15, but that merely says that more than one property definition can have the same name.
 * The proposed draft of the JSR-283 specification does clarify this more: Section 4.7.15 says :
 * </p>
 * <p>
 * <quote>"A node type may have two or more property definitions with identical name attributes (the value returned by
 * ItemDefinition.getName) as long as the definitions are otherwise distinguishable by either the required type attribute (the
 * value returned by PropertyDefinition.getRequiredType) or the multiple attribute (the value returned by
 * PropertyDefinition.isMultiple)."</quote>
 * </p>
 * <p>
 * This class is {@link Serializable} and designed to be used as a key in a {@link HashMap}.
 * </p>
 */
@Immutable
final class PropertyDefinitionId implements Serializable {

    /**
     * Current version is {@value} .
     */
    private static final long serialVersionUID = 1L;

    /**
     * The string-form of the name that can be used to represent a residual property definition.
     */
    public static final String ANY_NAME = JcrNodeType.RESIDUAL_ITEM_NAME;

    private final Name nodeTypeName;
    private final Name propertyDefinitionName;
    private final int propertyType;
    private final boolean allowsMultiple;
    /**
     * A cached string representation, which is used for {@link #equals(Object)} and {@link #hashCode()} among other things.
     */
    private final String stringRepresentation;

    /**
     * Create a new identifier for a property definition.
     * 
     * @param nodeTypeName the name of the node type; may not be null
     * @param propertyDefinitionName the name of the property definition, which may be a {@link #ANY_NAME residual property}; may
     *        not be null
     * @param propertyType the required property type for the definition; must be a valid {@link PropertyType} value
     * @param allowsMultiple true if the property definition should allow multiple values, or false if it is a single-value
     *        property definition
     */
    public PropertyDefinitionId( Name nodeTypeName,
                                 Name propertyDefinitionName,
                                 int propertyType,
                                 boolean allowsMultiple ) {
        this.nodeTypeName = nodeTypeName;
        this.propertyDefinitionName = propertyDefinitionName;
        this.propertyType = propertyType;
        this.allowsMultiple = allowsMultiple;
        this.stringRepresentation = this.nodeTypeName.getString() + '/' + this.propertyDefinitionName.getString() + '/'
                                    + PropertyType.nameFromValue(propertyType) + '/' + (allowsMultiple ? '*' : '1');
    }

    /**
     * Get the name of the node type on which the property definition is defined
     * 
     * @return the node type's name; may not be null
     */
    public Name getNodeTypeName() {
        return nodeTypeName;
    }

    /**
     * Get the name of the property definition.
     * 
     * @return the property definition's name; never null
     */
    public Name getPropertyDefinitionName() {
        return propertyDefinitionName;
    }

    /**
     * Get the required property type
     * 
     * @return the property type; always a valid {@link PropertyType} value
     */
    public int getPropertyType() {
        return propertyType;
    }

    /**
     * Return whether the property definition allows multiple values.
     * 
     * @return true if the property definition allows multiple values, or false if it is a single-value property definition
     */
    public boolean allowsMultiple() {
        return allowsMultiple;
    }

    /**
     * Determine whether this property definition allows properties with any name.
     * 
     * @return true if this node definition allows properties with any name, or false if this definition requires a particular
     *         property name
     */
    public boolean allowsAnyChildName() {
        return propertyDefinitionName.getLocalName().equals(ANY_NAME) && propertyDefinitionName.getNamespaceUri().length() == 0;
    }

    /**
     * Get the string form of this identifier. This form can be persisted, since it does not rely upon namespace prefixes.
     * 
     * @return the string form
     */
    public String getString() {
        return this.stringRepresentation;
    }

    /**
     * Parse the supplied string for of an identifer, and return the object form for that identifier.
     * 
     * @param definition the {@link #getString() string form of the identifier}; may not be null
     * @param factory the factory that should be used to create Name objects; may not be null
     * @return the object form of the identifier; never null
     * @throws ValueFormatException if the definition is not the valid format
     */
    public static PropertyDefinitionId fromString( String definition,
                                                   NameFactory factory ) {
        String[] parts = definition.split("/");
        String nodeTypeNameString = parts[0];
        String propertyDefinitionNameString = parts[1];
        Name nodeTypeName = factory.create(nodeTypeNameString);
        Name propertyDefinitionName = factory.create(propertyDefinitionNameString);
        int propertyType = PropertyType.valueFromName(parts[2]);
        boolean allowsMultiple = parts[3].charAt(0) == '*';
        return new PropertyDefinitionId(nodeTypeName, propertyDefinitionName, propertyType, allowsMultiple);
    }

    public PropertyDefinitionId asSingleValued() {
        return new PropertyDefinitionId(nodeTypeName, propertyDefinitionName, propertyType, false);
    }

    public PropertyDefinitionId asMultiValued() {
        return new PropertyDefinitionId(nodeTypeName, propertyDefinitionName, propertyType, true);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return this.stringRepresentation.hashCode();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof PropertyDefinitionId) {
            PropertyDefinitionId that = (PropertyDefinitionId)obj;
            return this.stringRepresentation.equals(that.stringRepresentation);
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
        return this.stringRepresentation;
    }

}
