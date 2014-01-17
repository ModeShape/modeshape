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
package org.modeshape.common.collection;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

/**
 * A {@link Multimap} implementation that uses an {@link HashSet} to store the values associated with a key. This implementation
 * does not allow duplicates and the values are not ordered.
 * 
 * @param <K> the key type
 * @param <V> the value type
 */
public class HashMultimap<K, V> extends AbstractMultimap<K, V> {

    /**
     * Creates a new, empty {@code LinkedHashMultimap} (that allows no duplicates) with the default initial capacity.
     * 
     * @param <K> the key type
     * @param <V> the value type
     * @return the new linked-hash multimap; never null
     */
    public static <K, V> HashMultimap<K, V> create() {
        return new HashMultimap<K, V>();
    }

    protected HashMultimap() {
        super(new HashMap<K, Collection<V>>());
    }

    @Override
    protected Collection<V> createCollection() {
        return new HashSet<V>();
    }

    @Override
    protected Collection<V> createUnmodifiableEmptyCollection() {
        return Collections.emptyList();
    }
}
