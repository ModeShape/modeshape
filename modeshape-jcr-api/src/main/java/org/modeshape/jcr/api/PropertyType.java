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
