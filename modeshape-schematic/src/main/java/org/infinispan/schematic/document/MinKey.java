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
import org.infinispan.marshall.AbstractExternalizer;
import org.infinispan.schematic.internal.marshall.Ids;
import org.infinispan.util.Util;

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

    public static class Externalizer extends AbstractExternalizer<MinKey> {
        /** The serialVersionUID */
        private static final long serialVersionUID = 1L;

        @Override
        public void writeObject( ObjectOutput output,
                                 MinKey value ) {
        }

        @Override
        public MinKey readObject( ObjectInput input ) {
            return MinKey.INSTANCE;
        }

        @Override
        public Integer getId() {
            return Ids.SCHEMATIC_VALUE_MINKEY;
        }

        @SuppressWarnings( "unchecked" )
        @Override
        public Set<Class<? extends MinKey>> getTypeClasses() {
            return Util.<Class<? extends MinKey>>asSet(MinKey.class);
        }
    }
}
