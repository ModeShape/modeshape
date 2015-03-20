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
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import org.infinispan.commons.marshall.SerializeWith;
import org.infinispan.schematic.Base64;
import org.infinispan.schematic.internal.SchematicExternalizer;
import org.infinispan.schematic.internal.marshall.Ids;

/**
 * A {@link Bson.Type#BINARY binary} value for use within a {@link Document BSON Object}.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 * @since 5.1
 */
@Immutable
@SerializeWith( Binary.Externalizer.class )
public final class Binary {

    private final byte type;
    private final byte[] data;

    public Binary( byte[] data ) {
        this.type = Bson.BinaryType.GENERAL;
        this.data = data;
    }

    public Binary( byte type,
                   byte[] data ) {
        this.type = type;
        this.data = data;
    }

    public byte getType() {
        return type;
    }

    public byte[] getBytes() {
        return data;
    }

    public int length() {
        return data.length;
    }

    @Override
    public int hashCode() {
        return length();
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof Binary) {
            Binary that = (Binary)obj;
            if (this.getType() != that.getType()) return false;
            if (this.length() != that.length()) return false;
            return Arrays.equals(this.getBytes(), that.getBytes());
        }
        return false;
    }

    @Override
    public String toString() {
        return "Binary (" + (int)type + ':' + length() + ')';
    }

    public String getBytesInBase64() {
        return Base64.encodeBytes(data);
    }

    public static class Externalizer extends SchematicExternalizer<Binary> {
        private static final long serialVersionUID = 1L;

        @Override
        public void writeObject( ObjectOutput output,
                                 Binary value ) throws IOException {
            output.writeByte(value.getType());
            output.writeInt(value.length());
            output.write(value.getBytes());
        }

        @Override
        public Binary readObject( ObjectInput input ) throws IOException {
            byte type = input.readByte();
            int len = input.readInt();
            byte[] bytes = new byte[len];
            input.readFully(bytes);
            return new Binary(type, bytes);
        }

        @Override
        public Integer getId() {
            return Ids.SCHEMATIC_VALUE_BINARY;
        }

        @SuppressWarnings( "unchecked" )
        @Override
        public Set<Class<? extends Binary>> getTypeClasses() {
            return Collections.<Class<? extends Binary>>singleton(Binary.class);
        }
    }
}
