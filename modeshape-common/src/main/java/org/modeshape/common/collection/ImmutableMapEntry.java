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

import java.util.Map;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.util.HashCode;
import org.modeshape.common.util.ObjectUtil;

/**
 * An immutable {@link java.util.Map.Entry} implementation.
 * 
 * @param <K> the key type
 * @param <V> the value type
 */
@Immutable
public class ImmutableMapEntry<K, V> implements Map.Entry<K, V> {
    private final K key;
    private final V value;

    public ImmutableMapEntry( K key,
                              V value ) {
        this.key = key;
        this.value = value;
    }

    public ImmutableMapEntry( Map.Entry<K, V> entry ) {
        this.key = entry.getKey();
        this.value = entry.getValue();
    }

    @Override
    public K getKey() {
        return key;
    }

    @Override
    public V getValue() {
        return value;
    }

    @Override
    public V setValue( V newValue ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int hashCode() {
        return HashCode.compute(key, value);
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof Map.Entry) {
            Map.Entry<?, ?> that = (Map.Entry<?, ?>)obj;
            if (!ObjectUtil.isEqualWithNulls(this.getKey(), that.getKey())) return false;
            if (!ObjectUtil.isEqualWithNulls(this.getValue(), that.getValue())) return false;
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "" + this.key + " = " + this.getValue();
    }
}
