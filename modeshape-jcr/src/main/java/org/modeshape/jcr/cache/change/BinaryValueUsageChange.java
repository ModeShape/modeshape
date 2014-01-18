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
package org.modeshape.jcr.cache.change;

import org.modeshape.jcr.value.BinaryKey;

/**
 * An event signalizing that the usages have changed for a binary value with a specific key.
 */
public abstract class BinaryValueUsageChange extends Change {

    private static final long serialVersionUID = 1L;

    private final BinaryKey key;

    protected BinaryValueUsageChange( BinaryKey key ) {
        this.key = key;
        assert this.key != null;
    }

    /**
     * Get the binary key.
     * 
     * @return the key; never null
     */
    public BinaryKey getKey() {
        return key;
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof BinaryValueUsageChange) {
            BinaryValueUsageChange that = (BinaryValueUsageChange)obj;
            return this.getKey().equals(that.getKey()) && this.getClass().equals(that.getClass());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }
}
