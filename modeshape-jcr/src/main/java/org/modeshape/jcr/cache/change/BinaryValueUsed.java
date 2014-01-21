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
 * An event signalizing that there are no more usages for the binary value with a specific key.
 */
public class BinaryValueUsed extends BinaryValueUsageChange {

    private static final long serialVersionUID = 1L;

    public BinaryValueUsed( BinaryKey key ) {
        super(key);
    }

    @Override
    public String toString() {
        return "Unused binary value '" + this.getKey() + "'";
    }

}
