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
package org.infinispan.schematic.internal;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.infinispan.atomic.Delta;
import org.infinispan.atomic.DeltaAware;
import org.infinispan.marshall.SerializeWith;
import org.infinispan.schematic.SchematicEntry;
import org.infinispan.schematic.internal.delta.Operation;
import org.infinispan.schematic.internal.marshall.Ids;
import org.infinispan.util.Util;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * An implementation of the {@link Delta} for a {@link SchematicEntry}.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 * @since 5.1
 * @see org.infinispan.atomic.AtomicHashMapDelta
 */
@SerializeWith( SchematicEntryDelta.Externalizer.class )
public class SchematicEntryDelta implements SchematicDelta {
    private static final Log LOG = LogFactory.getLog(SchematicEntryDelta.class);
    private static final boolean TRACE = LOG.isTraceEnabled();

    private final String key;
    private final List<Operation> changeLog;

    public SchematicEntryDelta( String key ) {
        this.key = key;
        this.changeLog = new LinkedList<Operation>();
        assert this.key != null;
        assert this.changeLog != null;
    }

    public SchematicEntryDelta( String key,
                                List<Operation> changeLog ) {
        this.key = key;
        this.changeLog = changeLog;
        assert this.key != null;
        assert this.changeLog != null;
    }

    @Override
    public DeltaAware merge( DeltaAware d ) {
        SchematicEntryLiteral other = null;
        if (d != null && (d instanceof SchematicEntryLiteral)) {
            other = (SchematicEntryLiteral)d;
            LOG.trace("Merging delta into existing " + other.getClass() + " -> " + other);
        } else {
            other = new SchematicEntryLiteral();
            LOG.trace("Merging delta into new SchematicEntryLiteral; DeltaAware is " + (d != null ? d.getClass() : "null")
                      + " -> " + d);
        }
        try {
            if (changeLog != null) {
                other.apply(changeLog);
            }
        } catch (RuntimeException e) {
            LOG.debug("Exception while merging delta " + this + " onto " + d, e);
            throw e;
        } finally {
            other.commit();
        }
        return other;
    }

    @Override
    public boolean isRecordingOperations() {
        return true;
    }

    @Override
    public void addOperation( Operation o ) {
        changeLog.add(o);
    }

    @Override
    public String toString() {
        return "SchematicEntryDelta{key=" + key + "," + "changeLog=" + changeLog + '}';
    }

    @Override
    public int hashCode() {
        return changeLog.hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj instanceof SchematicEntryDelta) {
            SchematicEntryDelta other = (SchematicEntryDelta)obj;
            return changeLog.equals(other.changeLog);
        }
        return false;
    }

    public static class Externalizer extends SchematicExternalizer<SchematicEntryDelta> {
        private static final long serialVersionUID = 1L;

        @SuppressWarnings( "synthetic-access" )
        @Override
        public void writeObject( ObjectOutput output,
                                 SchematicEntryDelta delta ) throws IOException {
            if (TRACE) LOG.tracef("Serializing %s", delta);
            output.writeUTF(delta.key);
            output.writeObject(delta.changeLog);
        }

        @SuppressWarnings( {"synthetic-access", "unchecked"} )
        @Override
        public SchematicEntryDelta readObject( ObjectInput input ) throws IOException, ClassNotFoundException {
            String key = input.readUTF();
            List<Operation> changeLog = (List<Operation>)input.readObject();
            SchematicEntryDelta delta = new SchematicEntryDelta(key, changeLog);
            if (TRACE) LOG.tracef("Deserialized %s", delta);
            return delta;
        }

        @Override
        public Integer getId() {
            return Ids.SCHEMATIC_VALUE_DELTA;
        }

        @Override
        @SuppressWarnings( "unchecked" )
        public Set<Class<? extends SchematicEntryDelta>> getTypeClasses() {
            return Util.<Class<? extends SchematicEntryDelta>>asSet(SchematicEntryDelta.class);
        }
    }
}
