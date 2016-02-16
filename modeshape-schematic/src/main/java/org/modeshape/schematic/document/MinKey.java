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
package org.modeshape.schematic.document;

import org.modeshape.schematic.annotation.Immutable;

@Immutable
public class MinKey {

    private static final String MIN_KEY_VALUE = "";

    protected static final MinKey INSTANCE = new MinKey();

    public static final MinKey getInstance() {
        return INSTANCE;
    }

    private MinKey() {
        // prevent instantiation
    }

    @Override
    public int hashCode() {
        return MIN_KEY_VALUE.hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        return obj == this || obj instanceof MinKey;
    }

    @Override
    public String toString() {
        return "MaxKey";
    }
}
