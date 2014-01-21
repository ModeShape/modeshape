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
}
