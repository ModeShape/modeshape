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
import java.util.Set;
import org.infinispan.commons.marshall.SerializeWith;
import org.infinispan.commons.util.Util;
import org.infinispan.schematic.internal.SchematicExternalizer;
import org.infinispan.schematic.internal.marshall.Ids;

/**
 * An object representation of 'null'. This is sometimes more convenient than dealing with 'null' as values.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 * @since 5.1
 */
@Immutable
@SerializeWith( Null.Externalizer.class )
public class Null {

    protected static final Null INSTANCE = new Null();

    public static final Null getInstance() {
        return INSTANCE;
    }

    public static final boolean matches( Object value ) {
        return value == null || value == INSTANCE || value instanceof Null;
    }

    private Null() {
        // prevent instantiation
    }

    @Override
    public int hashCode() {
        return INSTANCE.hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        return obj == null || obj == this || obj instanceof Null;
    }

    @Override
    public String toString() {
        return "null";
    }

    /**
     * There should only be on instance of this (though there may be others due to serialization).
     */
    @Override
    protected final Object clone() {
        return INSTANCE;
    }

    public static class Externalizer extends SchematicExternalizer<Null> {
        private static final long serialVersionUID = 1L;

        @Override
        public void writeObject( ObjectOutput output,
                                 Null value ) {
        }

        @Override
        public Null readObject( ObjectInput input ) {
            return Null.INSTANCE;
        }

        @Override
        public Integer getId() {
            return Ids.SCHEMATIC_VALUE_NULL;
        }

        @SuppressWarnings( "unchecked" )
        @Override
        public Set<Class<? extends Null>> getTypeClasses() {
            return Util.<Class<? extends Null>>asSet(Null.class);
        }
    }
}
