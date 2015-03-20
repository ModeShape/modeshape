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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.Set;
import org.infinispan.commons.marshall.SerializeWith;
import org.infinispan.schematic.internal.SchematicExternalizer;
import org.infinispan.schematic.internal.document.BsonUtils;
import org.infinispan.schematic.internal.marshall.Ids;

/**
 * A {@link Bson.Type#OBJECTID ObjectId} value for use within a {@link Document BSON object}, and are 12-byte binary values
 * designed to have a reasonably high probability of being unique when allocated.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 * @since 5.1
 */
@Immutable
@SerializeWith( ObjectId.Externalizer.class )
public final class ObjectId {

    private final int time;
    private final int machine;
    private final int process;
    private final int inc;

    public ObjectId( int time,
                     int machine,
                     int process,
                     int inc ) {
        this.time = time;
        this.machine = machine;
        this.process = process;
        this.inc = inc;
    }

    public int getTime() {
        return time;
    }

    public int getMachine() {
        return machine;
    }

    public int getProcess() {
        return process;
    }

    public int getInc() {
        return inc;
    }

    public byte[] getBytes() {
        byte b[] = new byte[12];
        BsonUtils.writeObjectId(this, b);
        return b;
    }
    
    public String getBytesInBase16() {
        byte b[] = getBytes();
        StringBuilder buf = new StringBuilder(24);
        for (int i = 0; i < b.length; i++) {
            int x = b[i] & 0xFF;
            String s = Integer.toHexString(x);
            if (s.length() == 1) {
                buf.append("0");
            }
            buf.append(s);
        }
        return buf.toString();
    }

    @Override
    public int hashCode() {
        return getTime();
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof ObjectId) {
            ObjectId that = (ObjectId)obj;
            return this.getTime() == that.getTime() && this.getMachine() == that.getMachine()
                   && this.getProcess() == this.getProcess() && this.getInc() == that.getInc();
        }
        return false;
    }

    @Override
    public String toString() {
        return "ObjectID(" + time + ':' + machine + ':' + process + ':' + inc + ')';
    }

    public static class Externalizer extends SchematicExternalizer<ObjectId> {
        private static final long serialVersionUID = 1L;

        @Override
        public void writeObject( ObjectOutput output,
                                 ObjectId id ) throws IOException {
            output.writeInt(id.getMachine());
            output.writeInt(id.getTime());
            output.writeInt(id.getProcess());
            output.writeInt(id.getInc());
        }

        @Override
        public ObjectId readObject( ObjectInput input ) throws IOException {
            int time = input.readInt();
            int mach = input.readInt();
            int proc = input.readInt();
            int inc = input.readInt();
            return new ObjectId(time, mach, proc, inc);
        }

        @Override
        public Integer getId() {
            return Ids.SCHEMATIC_VALUE_OBJECT_ID;
        }

        @SuppressWarnings( "unchecked" )
        @Override
        public Set<Class<? extends ObjectId>> getTypeClasses() {
            return Collections.<Class<? extends ObjectId>>singleton(ObjectId.class);
        }
    }
}
