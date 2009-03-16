/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in JBoss DNA is licensed
 * to you under the terms of the GNU Lesser General Public License as
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

import java.io.Serializable;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.util.HashCode;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.NameFactory;
import org.jboss.dna.graph.property.ValueFormatException;

/**
 * An immutable identifier for a property definition. Although instances can be serialized, the property definitions are often
 * stored within the graph as {@link #getString() string values} on a property. These string values can later be
 * {@link #fromString(String, NameFactory) parsed} to reconstruct the identifier. Note that this string representation does not
 * use namespace prefixes, so they are long-lasting and durable.
 */
@Immutable
public final class PropertyDefinitionId implements Serializable {

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
    private final int hc;

    /**
     * Create a new identifier for a propety definition.
     * 
     * @param nodeTypeName the name of the node type; may not be null
     * @param propertyDefinitionName the name of the property definition, which may be a {@link #ANY_NAME residual property}; may
     *        not be null
     */
    public PropertyDefinitionId( Name nodeTypeName,
                                 Name propertyDefinitionName ) {
        this.nodeTypeName = nodeTypeName;
        this.propertyDefinitionName = propertyDefinitionName;
        this.hc = HashCode.compute(this.nodeTypeName, this.propertyDefinitionName);
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
        return this.nodeTypeName.getString() + '/' + this.propertyDefinitionName.getString();
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
        int index = definition.indexOf('/');
        String nodeTypeNameString = definition.substring(0, index);
        String propertyDefinitionNameString = definition.substring(index + 1);
        Name nodeTypeName = factory.create(nodeTypeNameString);
        Name propertyDefinitionName = factory.create(propertyDefinitionNameString);
        return new PropertyDefinitionId(nodeTypeName, propertyDefinitionName);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return hc;
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
            if (this.hc != that.hc) return false;
            if (!this.nodeTypeName.equals(that.nodeTypeName)) return false;
            return this.propertyDefinitionName.equals(that.propertyDefinitionName);
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
        return this.nodeTypeName.toString() + '/' + this.propertyDefinitionName.toString();
    }

}
