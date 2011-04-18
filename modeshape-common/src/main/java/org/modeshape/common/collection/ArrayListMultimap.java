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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * A {@link Multimap} implementation that uses an {@link ArrayList} to store the values associated with a key. This implementation
 * allows duplicates and the values are ordered.
 * 
 * @param <K> the key type
 * @param <V> the value type
 */
public class ArrayListMultimap<K, V> extends AbstractMultimap<K, V> implements ListMultimap<K, V> {

    /**
     * Creates a new, empty {@code LinkedHashMultimap} (that allows no duplicates) with the default initial capacity.
     * 
     * @param <K> the key type
     * @param <V> the value type
     * @return the new linked-hash multimap; never null
     */
    public static <K, V> ArrayListMultimap<K, V> create() {
        return new ArrayListMultimap<K, V>();
    }

    protected ArrayListMultimap() {
        super(new HashMap<K, Collection<V>>());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.AbstractMultimap#createCollection()
     */
    @Override
    protected Collection<V> createCollection() {
        return new ArrayList<V>();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.AbstractMultimap#createUnmodifiableEmptyCollection()
     */
    @Override
    protected Collection<V> createUnmodifiableEmptyCollection() {
        return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.common.collection.AbstractMultimap#get(java.lang.Object)
     */
    @Override
    public List<V> get( K key ) {
        return (List<V>)super.get(key);
    }
}
