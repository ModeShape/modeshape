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

import java.io.Serializable;
import java.util.Arrays;
import org.modeshape.schematic.Base64;
import org.modeshape.schematic.annotation.Immutable;

/**
 * A {@link Bson.Type#BINARY binary} value for use within a {@link Document BSON Object}.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 */
@Immutable
public final class Binary implements Serializable {
    private static final long serialVersionUID = 1L;

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
}
