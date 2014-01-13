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
        private static final int CODE_WITHOUT_SCOPE_TYPE = 1;
        private static final int CODE_WITH_SCOPE_TYPE = 2;

        @Override
        public void writeObject( ObjectOutput output,
                                 Code value ) throws IOException {
            if (value instanceof CodeWithScope) {
                CodeWithScope withScope = (CodeWithScope)value;
                output.writeInt(CODE_WITH_SCOPE_TYPE);
                output.writeUTF(withScope.getCode());
                output.writeObject(withScope.getScope());
            } else {
                output.writeInt(CODE_WITHOUT_SCOPE_TYPE);
                output.writeUTF(value.getCode());
            }
        }

        @Override
        public Code readObject( ObjectInput input ) throws IOException, ClassNotFoundException {
            int type = input.readInt();
            switch (type) {
                case CODE_WITHOUT_SCOPE_TYPE:
                    String code = input.readUTF();
                    return new Code(code);
                case CODE_WITH_SCOPE_TYPE:
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
