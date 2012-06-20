/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.infinispan.schematic.document;

import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;
import org.infinispan.marshall.SerializeWith;
import org.infinispan.schematic.internal.SchematicExternalizer;
import org.infinispan.schematic.internal.marshall.Ids;
import org.infinispan.util.Util;

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
            return Util.<Class<? extends MaxKey>>asSet(MaxKey.class);
        }
    }
}
