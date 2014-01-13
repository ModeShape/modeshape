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
package org.modeshape.jcr.api;

/**
 * ModeShape extension of the {@link javax.jcr.PropertyType} class, which allows additional property types.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public final class PropertyType {

    /**
     * The <code>SIMPLE_REFERENCE</code> property type is used to store references to nodes without also storing
     * back-references.
     */
    public static final int SIMPLE_REFERENCE = 100;

    /**
     * The type name for {@link PropertyType#SIMPLE_REFERENCE}
     */
    public static final String TYPENAME_SIMPLE_REFERENCE = "SimpleReference";

    /**
     * Returns the name of the specified <code>type</code>.
     *
     * @param type the property type
     * @return the name of the specified <code>type</code>
     * @throws IllegalArgumentException if <code>type</code> is not a valid
     *                                  property type.
     */
    public static String nameFromValue(int type) {
        switch (type) {
            case SIMPLE_REFERENCE:
                return TYPENAME_SIMPLE_REFERENCE;
            default:
                return javax.jcr.PropertyType.nameFromValue(type);
        }
    }

    /**
     * Returns the numeric constant value of the type with the specified name.
     *
     * @param name the name of the property type.
     * @return the numeric constant value.
     * @throws IllegalArgumentException if <code>name</code> is not a valid
     *                                  property type name.
     */
    public static int valueFromName(String name) {
        if (name.equals(TYPENAME_SIMPLE_REFERENCE)) {
            return SIMPLE_REFERENCE;
        }
        return javax.jcr.PropertyType.valueFromName(name);
    }

    /**
     * Private constructor to prevent instantiation.
     */
    private PropertyType() {
    }
}
