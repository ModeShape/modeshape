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

import javax.jcr.PropertyType;

/**
 * 
 */
class PropertyTypeUtil {

    /**
     * Compute the JCR {@link PropertyType} for the given DNA {@link org.jboss.dna.graph.property.PropertyType}.
     * <p>
     * See DNA-293 for complete discussion on why this method works the way it does. The best option appears to be basing the
     * PropertyType on the first value, since that should be compatible with the PropertyType that was used when the values were
     * set on the property in the first place.
     * </p>
     * 
     * @param property the DNA property for which the {@link PropertyType} is to be determined; never null
     * @return the JCR property type; always a valid value and never {@link PropertyType#UNDEFINED}.
     */
    static final int jcrPropertyTypeFor( org.jboss.dna.graph.property.Property property ) {
        Object value = property.getFirstValue();
        if (value == null) return PropertyType.UNDEFINED;

        // Get the DNA property type for this ...
        switch (org.jboss.dna.graph.property.PropertyType.discoverType(value)) {
            case STRING:
                return PropertyType.STRING;
            case NAME:
                return PropertyType.NAME;
            case LONG:
                return PropertyType.LONG;
            case UUID:
                return PropertyType.STRING; // JCR treats UUID properties as strings
            case URI:
                return PropertyType.STRING;
            case PATH:
                return PropertyType.PATH;
            case BOOLEAN:
                return PropertyType.BOOLEAN;
            case DATE:
                return PropertyType.DATE;
            case DECIMAL:
                return PropertyType.STRING; // better than losing information
            case DOUBLE:
                return PropertyType.DOUBLE;
            case BINARY:
                return PropertyType.BINARY;
            case OBJECT:
                return PropertyType.UNDEFINED;
            case REFERENCE:
                return PropertyType.REFERENCE;
        }
        assert false;
        return PropertyType.UNDEFINED;
    }

    /**
     * Compute the DNA {@link org.jboss.dna.graph.property.PropertyType} for the given JCR {@link PropertyType} value.
     * 
     * @param jcrPropertyType the DNA property for which the {@link PropertyType} is to be determined; never null
     * @return the JCR property type; always a valid value and never {@link PropertyType#UNDEFINED}.
     */
    static final org.jboss.dna.graph.property.PropertyType dnaPropertyTypeFor( int jcrPropertyType ) {
        // Make sure the value is the correct type ...
        switch (jcrPropertyType) {
            case PropertyType.STRING:
                return org.jboss.dna.graph.property.PropertyType.STRING;
            case PropertyType.BINARY:
                return org.jboss.dna.graph.property.PropertyType.BINARY;
            case PropertyType.BOOLEAN:
                return org.jboss.dna.graph.property.PropertyType.BOOLEAN;
            case PropertyType.DOUBLE:
                return org.jboss.dna.graph.property.PropertyType.DOUBLE;
            case PropertyType.LONG:
                return org.jboss.dna.graph.property.PropertyType.LONG;
            case PropertyType.DATE:
                return org.jboss.dna.graph.property.PropertyType.DATE;
            case PropertyType.PATH:
                return org.jboss.dna.graph.property.PropertyType.PATH;
            case PropertyType.NAME:
                return org.jboss.dna.graph.property.PropertyType.NAME;
            case PropertyType.REFERENCE:
                return org.jboss.dna.graph.property.PropertyType.REFERENCE;
        }
        assert false;
        return org.jboss.dna.graph.property.PropertyType.STRING;
    }

}
