/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
