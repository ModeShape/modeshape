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
package org.modeshape.common.collection;

import java.util.List;
import java.util.Map;

/**
 * A collection similar to {@link Map}, but which may associate multiple values with any single key.
 * <p>
 * Some implementation may not allow duplicate key-value pairs. In such implementations, calling #pu
 * </p>
 * 
 * @param <K> the key type
 * @param <V> the value type
 */
public interface ListMultimap<K, V> extends Multimap<K, V> {

    /**
     * Get the collection of values that are associated with the supplied key.
     * <p>
     * Changes to the returned collection will update the values that this multimap associates with the key.
     * </p>
     * 
     * @param key the key
     * @return the collection of values, or an empty collection if the supplied key was not associated with any values
     */
    @Override
    List<V> get( K key );

    // /**
    // * Return a collection of all values in this multimap. Changes to the returned collection will update the underlying
    // multimap,
    // * and vice versa.
    // *
    // * @return the collection of values, which may include the same value multiple times if it occurs in multiple mappings;
    // never
    // * null but possibly empty
    // */
    // Collection<V> values();
}
