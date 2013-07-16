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
