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
package org.infinispan.schematic.internal;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.infinispan.atomic.Delta;
import org.infinispan.atomic.DeltaAware;
import org.infinispan.commons.marshall.SerializeWith;
import org.infinispan.commons.util.Util;
import org.infinispan.schematic.SchematicEntry;
import org.infinispan.schematic.internal.delta.Operation;
import org.infinispan.schematic.internal.marshall.Ids;
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
            LOG.trace("Merging delta " + changeLog + " into existing " + other);
        } else {
            other = new SchematicEntryLiteral();
            LOG.trace("Merging delta " + changeLog + " into new SchematicEntryLiteral");
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
