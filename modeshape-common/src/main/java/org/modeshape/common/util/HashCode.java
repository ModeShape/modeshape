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
package org.modeshape.common.util;

import java.util.Arrays;
import net.jcip.annotations.Immutable;

/**
 * Utilities for easily computing hash codes. The algorithm should generally produce good distributions for use in hash-based
 * containers or collections, but as expected does always result in repeatable hash codes given the inputs.
 */
@Immutable
public class HashCode {

    // Prime number used in improving distribution: 1,000,003
    private static final int PRIME = 103;

    /**
     * Compute a combined hash code from the supplied objects. This method always returns 0 if no objects are supplied.
     * 
     * @param objects the objects that should be used to compute the hash code
     * @return the hash code
     */
    public static int compute( Object... objects ) {
        return compute(0, objects);
    }

    /**
     * Compute a combined hash code from the supplied objects using the supplied seed.
     * 
     * @param seed a value upon which the hash code will be based; may be 0
     * @param objects the objects that should be used to compute the hash code
     * @return the hash code
     */
    protected static int compute( int seed,
                                  Object... objects ) {
        if (objects == null || objects.length == 0) {
            return seed * HashCode.PRIME;
        }
        // Compute the hash code for all of the objects ...
        int hc = seed;
        for (Object object : objects) {
            hc = HashCode.PRIME * hc;
            if (object instanceof byte[]) {
                hc += Arrays.hashCode((byte[])object);
            } else if (object instanceof boolean[]) {
                hc += Arrays.hashCode((boolean[])object);
            } else if (object instanceof short[]) {
                hc += Arrays.hashCode((short[])object);
            } else if (object instanceof int[]) {
                hc += Arrays.hashCode((int[])object);
            } else if (object instanceof long[]) {
                hc += Arrays.hashCode((long[])object);
            } else if (object instanceof float[]) {
                hc += Arrays.hashCode((float[])object);
            } else if (object instanceof double[]) {
                hc += Arrays.hashCode((double[])object);
            } else if (object instanceof char[]) {
                hc += Arrays.hashCode((char[])object);
            } else if (object instanceof Object[]) {
                hc += Arrays.hashCode((Object[])object);
            } else if (object != null) {
                hc += object.hashCode();
            }
        }
        return hc;
    }

}
