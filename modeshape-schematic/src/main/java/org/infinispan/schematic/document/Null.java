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

/**
 * An object representation of 'null'. This is sometimes more convenient than dealing with 'null' as values.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 * @since 5.1
 */
@Immutable
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

    public static class Externalizer extends AbstractExternalizer<Null> {
        /** The serialVersionUID */
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
