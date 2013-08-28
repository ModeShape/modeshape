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
package org.modeshape.jcr.cache;

import javax.jcr.PropertyType;
import org.modeshape.common.SystemFailureException;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.jcr.JcrI18n;

/**
 * A utility for working with {@link PropertyType JCR property types} and {@link org.modeshape.jcr.value.PropertyType ModeShape
 * property types}.
 */
@Immutable
public class PropertyTypeUtil {

    /**
     * Compute the JCR {@link PropertyType} for the given ModeShape {@link org.modeshape.jcr.value.PropertyType}.
     * <p>
     * See MODE-328 for complete discussion on why this method works the way it does. The best option appears to be basing the
     * PropertyType on the first value, since that should be compatible with the PropertyType that was used when the values were
     * set on the property in the first place.
     * </p>
     * 
     * @param property the ModeShape property for which the {@link PropertyType} is to be determined; never null
     * @return the JCR property type; always a valid value and never {@link PropertyType#UNDEFINED}.
     */
    public static final int jcrPropertyTypeFor( org.modeshape.jcr.value.Property property ) {
        for (Object value : property) {
            if (value != null) {
                return jcrPropertyTypeFor(org.modeshape.jcr.value.PropertyType.discoverType(value));
            }
        }
        return PropertyType.STRING;
    }

    /**
     * Compute the ModeShape {@link org.modeshape.jcr.value.PropertyType} for the given JCR {@link PropertyType} value.
     * 
     * @param jcrPropertyType the JCR property type for which the {@link PropertyType ModeShape PropertyType} is to be determined;
     *        never null
     * @return the ModeShape property type; always a valid value and never {@link PropertyType#UNDEFINED}.
     */
    public static final org.modeshape.jcr.value.PropertyType modePropertyTypeFor( int jcrPropertyType ) {
        // Make sure the value is the correct type ...
        switch (jcrPropertyType) {
            case PropertyType.STRING:
                return org.modeshape.jcr.value.PropertyType.STRING;
            case PropertyType.BINARY:
                return org.modeshape.jcr.value.PropertyType.BINARY;
            case PropertyType.BOOLEAN:
                return org.modeshape.jcr.value.PropertyType.BOOLEAN;
            case PropertyType.DOUBLE:
                return org.modeshape.jcr.value.PropertyType.DOUBLE;
            case PropertyType.LONG:
                return org.modeshape.jcr.value.PropertyType.LONG;
            case PropertyType.DATE:
                return org.modeshape.jcr.value.PropertyType.DATE;
            case PropertyType.DECIMAL:
                return org.modeshape.jcr.value.PropertyType.DECIMAL;
            case PropertyType.PATH:
                return org.modeshape.jcr.value.PropertyType.PATH;
            case PropertyType.NAME:
                return org.modeshape.jcr.value.PropertyType.NAME;
            case PropertyType.REFERENCE:
                return org.modeshape.jcr.value.PropertyType.REFERENCE;
            case PropertyType.URI:
                return org.modeshape.jcr.value.PropertyType.URI;
            case PropertyType.WEAKREFERENCE:
                return org.modeshape.jcr.value.PropertyType.WEAKREFERENCE;
            case org.modeshape.jcr.api.PropertyType.SIMPLE_REFERENCE:
                return org.modeshape.jcr.value.PropertyType.SIMPLEREFERENCE;
            case PropertyType.UNDEFINED:
                return org.modeshape.jcr.value.PropertyType.OBJECT;
            default:
                // All JCR PropertyType values should be explicitly handled above ...
                throw new SystemFailureException(JcrI18n.invalidPropertyType.text(jcrPropertyType));
        }
    }

    /**
     * Compute the ModeShape {@link org.modeshape.jcr.value.PropertyType} for the given JCR {@link PropertyType} value.
     * 
     * @param dnaPropertyType the ModeShape property type; never null
     * @return the JCR property type; always a valid value and never {@link PropertyType#UNDEFINED}.
     */
    public static final int jcrPropertyTypeFor( org.modeshape.jcr.value.PropertyType dnaPropertyType ) {
        // Make sure the value is the correct type ...
        switch (dnaPropertyType) {
            case STRING:
                return PropertyType.STRING;
            case NAME:
                return PropertyType.NAME;
            case LONG:
                return PropertyType.LONG;
            case UUID:
                return PropertyType.STRING; // JCR treats UUID properties as strings
            case URI:
                return PropertyType.URI;
            case PATH:
                return PropertyType.PATH;
            case BOOLEAN:
                return PropertyType.BOOLEAN;
            case DATE:
                return PropertyType.DATE;
            case DECIMAL:
                return PropertyType.DECIMAL;
            case DOUBLE:
                return PropertyType.DOUBLE;
            case BINARY:
                return PropertyType.BINARY;
            case OBJECT:
                return PropertyType.UNDEFINED;
            case REFERENCE:
                return PropertyType.REFERENCE;
            case WEAKREFERENCE:
                return PropertyType.WEAKREFERENCE;
            case SIMPLEREFERENCE: {
                return org.modeshape.jcr.api.PropertyType.SIMPLE_REFERENCE;
            }
        }
        assert false;
        return PropertyType.UNDEFINED;
    }

}
