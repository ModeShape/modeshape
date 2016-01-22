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
import org.modeshape.schematic.internal.document.BsonUtils;

/**
 * A {@link Bson.Type#OBJECTID ObjectId} value for use within a {@link Document BSON object}, and are 12-byte binary values
 * designed to have a reasonably high probability of being unique when allocated.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 */
@Immutable
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
}
