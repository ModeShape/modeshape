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
package org.infinispan.schematic.document;

import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.Set;
import org.infinispan.commons.marshall.SerializeWith;
import org.infinispan.schematic.internal.SchematicExternalizer;
import org.infinispan.schematic.internal.marshall.Ids;

@Immutable
@SerializeWith( MaxKey.Externalizer.class )
public final class MaxKey {

    /**
     * The largest UTF-8 character that will fit into 2 bytes is U+07FF. Therefore, if we make a string that contains a (non
     * UTF-8) unicode character larger than this, that string should always be treated as larger than all UTF-8 strings.
     */
    private static final String MAX_KEY_VALUE = new String(new char[] {'\u07FF' + 1});

    protected static final MaxKey INSTANCE = new MaxKey();

    public static final MaxKey getInstance() {
        return INSTANCE;
    }

    private MaxKey() {
        // prevent instantiation
    }

    @Override
    public int hashCode() {
        return MAX_KEY_VALUE.hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        return obj == this || obj instanceof MaxKey;
    }

    @Override
    public String toString() {
        return "MaxKey";
    }

    /**
     * There should only be on instance of this (though there may be others due to serialization).
     */
    @Override
    protected final Object clone() {
        return INSTANCE;
    }

    public static class Externalizer extends SchematicExternalizer<MaxKey> {
        private static final long serialVersionUID = 1L;

        @Override
        public void writeObject( ObjectOutput output,
                                 MaxKey value ) {
        }

        @Override
        public MaxKey readObject( ObjectInput input ) {
            return MaxKey.INSTANCE;
        }

        @Override
        public Integer getId() {
            return Ids.SCHEMATIC_VALUE_MAXKEY;
        }

        @SuppressWarnings( "unchecked" )
        @Override
        public Set<Class<? extends MaxKey>> getTypeClasses() {
            return Collections.<Class<? extends MaxKey>>singleton(MaxKey.class);
        }
    }
}
