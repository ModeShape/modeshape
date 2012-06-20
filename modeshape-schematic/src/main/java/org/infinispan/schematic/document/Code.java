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
import java.util.Set;
import org.infinispan.marshall.SerializeWith;
import org.infinispan.schematic.internal.SchematicExternalizer;
import org.infinispan.schematic.internal.marshall.Ids;
import org.infinispan.util.Util;

/**
 * A {@link Bson.Type#JAVASCRIPT JavaScript code} value for use within a {@link Document BSON Object}.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 * @since 5.1
 */
@Immutable
@SerializeWith( Code.Externalizer.class )
public class Code {

    private final String code;

    public Code( String code ) {
        this.code = code;
        assert this.code != null;
    }

    public String getCode() {
        return code;
    }

    @Override
    public int hashCode() {
        return code.hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof Code) {
            Code that = (Code)obj;
            return this.getCode().equals(that.getCode());
        }
        return false;
    }

    @Override
    public String toString() {
        return "Code (" + getCode() + ')';
    }

    public static class Externalizer extends SchematicExternalizer<Code> {
        private static final long serialVersionUID = 1L;

        @Override
        public void writeObject( ObjectOutput output,
                                 Code value ) throws IOException {
            if (value instanceof CodeWithScope) {
                CodeWithScope withScope = (CodeWithScope)value;
                output.write(2);
                output.writeUTF(withScope.getCode());
                output.writeObject(withScope.getScope());
            } else {
                output.write(1);
                output.writeUTF(value.getCode());
            }
        }

        @Override
        public Code readObject( ObjectInput input ) throws IOException, ClassNotFoundException {
            int type = input.read();
            switch (type) {
                case 1:
                    String code = input.readUTF();
                    return new Code(code);
                case 2:
                    code = input.readUTF();
                    Document scope = (Document)input.readObject();
                    return new CodeWithScope(code, scope);
                default:
                    throw new UnsupportedOperationException("Unknown Code type: " + type);
            }
        }

        @Override
        public Integer getId() {
            return Ids.SCHEMATIC_VALUE_CODE;
        }

        @SuppressWarnings( "unchecked" )
        @Override
        public Set<Class<? extends Code>> getTypeClasses() {
            return Util.<Class<? extends Code>>asSet(Code.class, CodeWithScope.class);
        }
    }

}
