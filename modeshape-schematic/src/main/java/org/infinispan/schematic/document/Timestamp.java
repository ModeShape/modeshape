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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Date;
import java.util.Set;
import org.infinispan.marshall.SerializeWith;
import org.infinispan.schematic.internal.SchematicExternalizer;
import org.infinispan.schematic.internal.marshall.Ids;
import org.infinispan.util.Util;

/**
 * A {@link Bson.Type#TIMESTAMP timestamp} value for use within a {@link Document BSON object}. <b>Note that using {@link Date} is
 * recommended for most applications and use cases, as they are more generally suited for representing instants in time.</b>
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 * @since 5.1
 */
@Immutable
@SerializeWith( Timestamp.Externalizer.class )
public final class Timestamp {

    private final Date time;
    private final int inc;

    public Timestamp() {
        this.time = null;
        this.inc = 0;
    }

    public Timestamp( Date date ) {
        this((int)(date.getTime() / 1000L), 0);
    }

    public Timestamp( int timeInSeconds,
                      int inc ) {
        this.time = new Date(timeInSeconds * 1000L);
        this.inc = inc;
    }

    public int getTime() {
        return time == null ? 0 : (int)(time.getTime() / 1000L);
    }

    public int getInc() {
        return inc;
    }

    public Date getAsDate() {
        return new Date(time.getTime()); // make defensive copy
    }

    @Override
    public int hashCode() {
        return getTime(); // note that Date.hashCode() calls Date.getTime(), so our approach works great
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof Timestamp) {
            Timestamp that = (Timestamp)obj;
            return this.getInc() == that.getInc() && this.getTime() == that.getTime();
        }
        return false;
    }

    @Override
    public String toString() {
        return "TS(" + time + ':' + inc + ')';
    }

    public static class Externalizer extends SchematicExternalizer<Timestamp> {
        private static final long serialVersionUID = 1L;

        @Override
        public void writeObject( ObjectOutput output,
                                 Timestamp timestamp ) throws IOException {
            output.write(timestamp.getInc());
            output.write(timestamp.getTime());
        }

        @Override
        public Timestamp readObject( ObjectInput input ) throws IOException {
            int inc = input.readInt();
            int time = input.readInt();
            return new Timestamp(inc, time);
        }

        @Override
        public Integer getId() {
            return Ids.SCHEMATIC_VALUE_TIMESTAMP;
        }

        @SuppressWarnings( "unchecked" )
        @Override
        public Set<Class<? extends Timestamp>> getTypeClasses() {
            return Util.<Class<? extends Timestamp>>asSet(Timestamp.class);
        }
    }
}
