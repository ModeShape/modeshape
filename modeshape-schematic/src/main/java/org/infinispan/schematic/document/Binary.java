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
import java.util.Arrays;
import java.util.Set;
import org.infinispan.marshall.AbstractExternalizer;
import org.infinispan.schematic.internal.marshall.Ids;
import org.infinispan.util.Base64;
import org.infinispan.util.Util;

/**
 * A {@link Bson.Type#BINARY binary} value for use within a {@link Document BSON Object}.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 * @since 5.1
 */
@Immutable
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

    public static class Externalizer extends AbstractExternalizer<Binary> {
        /** The serialVersionUID */
        private static final long serialVersionUID = 1L;

        @Override
        public void writeObject( ObjectOutput output,
                                 Binary value ) throws IOException {
            output.write(value.getType());
            output.write(value.length());
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
            return Util.<Class<? extends Binary>>asSet(Binary.class);
        }
    }
}
