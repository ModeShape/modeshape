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
package org.jboss.dna.jcr.cache;

import java.util.UUID;
import javax.jcr.PropertyType;
import net.jcip.annotations.Immutable;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.jcr.PropertyDefinitionId;
import org.jboss.dna.jcr.PropertyId;

/**
 * An immutable representation of the name and current value(s) for a property, along with the JCR metadata for the property,
 * including the {@link PropertyInfo#getDefinitionId() property definition} and {@link PropertyInfo#getPropertyType() property
 * type}.
 * <p>
 * This class is immutable, which means that clients should never hold onto an instance. Instead, clients can obtain an instance
 * by using a {@link PropertyId}, quickly use the information in the instance, and then immediately discard their reference. This
 * is because these instances are replaced and discarded whenever anything about the property changes.
 * </p>
 */
@Immutable
public class PropertyInfo {
    private final PropertyId propertyId;
    private final PropertyDefinitionId definitionId;
    private final Property dnaProperty;
    private final int propertyType;
    private final boolean multiValued;
    private final boolean isNew;
    private final boolean isModified;

    public PropertyInfo( PropertyId propertyId,
                         PropertyDefinitionId definitionId,
                         int propertyType,
                         Property dnaProperty,
                         boolean multiValued,
                         boolean isNew,
                         boolean isModified ) {
        this.propertyId = propertyId;
        this.definitionId = definitionId;
        this.propertyType = propertyType;
        this.dnaProperty = dnaProperty;
        this.multiValued = multiValued;
        this.isNew = isNew;
        this.isModified = isModified;

        assert isNew ? !isModified : true;
        assert isModified ? !isNew : true;
    }

    /**
     * Get the durable identifier for this property.
     * 
     * @return propertyId
     */
    public PropertyId getPropertyId() {
        return propertyId;
    }

    /**
     * Get the UUID of the node to which this property belongs.
     * 
     * @return the owner node's UUID; never null
     */
    public UUID getNodeUuid() {
        return propertyId.getNodeId();
    }

    /**
     * The identifier for the property definition.
     * 
     * @return the property definition ID; never null
     */
    public PropertyDefinitionId getDefinitionId() {
        return definitionId;
    }

    /**
     * Get the DNA Property, which contains the name and value(s)
     * 
     * @return the property; never null
     */
    public Property getProperty() {
        return dnaProperty;
    }

    /**
     * Get the property name.
     * 
     * @return the property name; never null
     */
    public Name getPropertyName() {
        return dnaProperty.getName();
    }

    /**
     * Get the JCR {@link PropertyType} for this property.
     * 
     * @return the property type
     */
    public int getPropertyType() {
        return propertyType;
    }

    /**
     * @return multiValued
     */
    public boolean isMultiValued() {
        return multiValued;
    }

    /**
     * Indicates whether this property/value combination is new (i.e., does not yet exist in the persistent repository).
     * 
     * @return {@code true} if the property has not yet been saved to the persistent repository.
     * @see javax.jcr.Item#isNew()
     */
    public boolean isNew() {
        return this.isNew;
    }

    /**
     * Indicates whether this property/value combination is modified (i.e., exists in the persistent repository with a different
     * value or values).
     * 
     * @return {@code true} if the property has been modified since the last time it was saved to the persistent repository
     * @see javax.jcr.Item#isModified()
     */
    public boolean isModified() {
        return this.isModified;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return propertyId.hashCode();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof PropertyInfo) {
            return propertyId.equals(((PropertyInfo)obj).getPropertyId());
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
        StringBuilder sb = new StringBuilder();
        sb.append(propertyId);
        sb.append(" defined by ").append(definitionId);
        sb.append(" of type ").append(PropertyType.nameFromValue(propertyType));
        if (dnaProperty.isSingle()) {
            sb.append(" with value ");
        } else {
            sb.append(" with values ");
        }
        sb.append(dnaProperty.getValuesAsArray());
        return sb.toString();
    }
}
