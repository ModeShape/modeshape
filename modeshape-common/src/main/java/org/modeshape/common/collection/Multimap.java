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

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * A collection similar to {@link Map}, but which may associate multiple values with any single key.
 * <p>
 * Some implementation may not allow duplicate key-value pairs. In such implementations, calling #pu
 * </p>
 * 
 * @param <K> the key type
 * @param <V> the value type
 */
public interface Multimap<K, V> {

    /**
     * Return the number of key-value pairs in this multimap.
     * 
     * @return the size of this multimap.
     */
    int size();

    boolean isEmpty();

    /**
     * Associate the supplied value with the given key, returning true if the size of this multimap was increased.
     * <p>
     * Some implementations allow duplicate key-value pairs, and in these cases this method will always increase the size of the
     * multimap and will thus always return true. Other implementations will not allow duplicate key-value pairs, and may return
     * false if the multimap already contains the supplied key-value pair.
     * </p>
     * 
     * @param key the key
     * @param value the value
     * @return {@code true} if the size of this multimap was increased as a result of this call, or {@code false} if the multimap
     *         already contained the key-value pair and doesn't allow duplicates
     */
    boolean put( K key,
                 V value );

    /**
     * Remove the supplied key-value pair from this multi-map.
     * 
     * @param key the key
     * @param value the value
     * @return {@code true} if the size of this multimap was decreased as a result of this call, or {@code false} if the multimap
     *         did not contain the key-value pair
     */
    boolean remove( K key,
                    V value );

    /**
     * Remove all of the key-value pairs that use the supplied key, returning the collection of values. The resulting collection
     * may be modifiable, but changing it will have no effect on this multimap.
     * 
     * @param key the key
     * @return the collection of values that were removed, or an empty collection if there were none; never null
     */
    Collection<V> removeAll( K key );

    /**
     * Get the collection of values that are associated with the supplied key.
     * <p>
     * Changes to the returned collection will update the values that this multimap associates with the key.
     * </p>
     * 
     * @param key the key
     * @return the collection of values, or an empty collection if the supplied key was not associated with any values
     */
    Collection<V> get( K key );

    /**
     * Determine whether this multimap associates any values with the supplied key.
     * 
     * @param key the key
     * @return {@code true} if there is at least one value associated with the supplied key, or {@code false} otherwise
     */
    boolean containsKey( K key );

    /**
     * Determine whether this multimap associates at least one key with the supplied value.
     * 
     * @param value the value
     * @return {@code true} if there is at least one key associated with the supplied value, or {@code false} otherwise
     */
    boolean containsValue( Object value );

    /**
     * Determine whether this multimap contains the supplied key-value pair.
     * 
     * @param key the key
     * @param value the value
     * @return {@code true} if this multimap contains an entry with the supplied key and value, or {@code false} otherwise
     */
    boolean containsEntry( Object key,
                           Object value );

    /**
     * Return the set of keys in this multimap.
     * 
     * @return the set of keys; never null but possible empty
     */
    Set<K> keySet();

    /**
     * Return a collection of all values in this multimap. Changes to the returned collection will update the underlying multimap,
     * and vice versa.
     * 
     * @return the collection of values, which may include the same value multiple times if it occurs in multiple mappings; never
     *         null but possibly empty
     */
    Collection<V> values();

    /**
     * Return a collection of all key-value pairs. Changes to the returned collection will update the underlying multimap, and
     * vice versa. The entries collection does not support the {@code add} or {@code addAll} operations.
     * 
     * @return the collection of map entries consisting of key-value pairs; never null but possibly empty
     */
    Collection<Map.Entry<K, V>> entries();

    /**
     * Return a map view that associates each key with the corresponding values in the multimap. Changes to the returned map, such
     * as element removal, will update the underlying multimap. The map does not support {@code setValue()} on its entries,
     * {@code put}, or {@code putAll}.
     * <p>
     * The collections returned by {@code asMap().get(Object)} have the same behavior as those returned by {@link #get}.
     * 
     * @return a map view from a key to its collection of values
     */
    Map<K, Collection<V>> asMap();

    /**
     * Remove all key-value pairs from this multimap.
     */
    void clear();
}
