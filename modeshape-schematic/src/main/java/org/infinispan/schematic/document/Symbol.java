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
import java.io.Serializable;
import java.util.Set;
import org.infinispan.commons.marshall.SerializeWith;
import org.infinispan.commons.util.Util;
import org.infinispan.schematic.internal.SchematicExternalizer;
import org.infinispan.schematic.internal.marshall.Ids;

/**
 * A {@link Bson.Type#SYMBOL symbol} value for use within a {@link Document BSON Object}.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 * @since 5.1
 */
@Immutable
@SerializeWith( Symbol.Externalizer.class )
public final class Symbol implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String symbol;

    public Symbol( String symbol ) {
        this.symbol = symbol;
        assert this.symbol != null;
    }

    public String getSymbol() {
        return symbol;
    }

    @Override
    public int hashCode() {
        return symbol.hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof Symbol) {
            Symbol that = (Symbol)obj;
            return this.getSymbol().equals(that.getSymbol());
        }
        if (obj instanceof String) {
            String that = (String)obj;
            return that.equals(this.getSymbol());
        }
        return false;
    }

    @Override
    public String toString() {
        return symbol;
    }

    public static class Externalizer extends SchematicExternalizer<Symbol> {
        private static final long serialVersionUID = 1L;

        @Override
        public void writeObject( ObjectOutput output,
                                 Symbol symbol ) throws IOException {
            output.writeUTF(symbol.getSymbol());
        }

        @Override
        public Symbol readObject( ObjectInput input ) throws IOException {
            return new Symbol(input.readUTF());
        }

        @Override
        public Integer getId() {
            return Ids.SCHEMATIC_VALUE_SYMBOL;
        }

        @SuppressWarnings( "unchecked" )
        @Override
        public Set<Class<? extends Symbol>> getTypeClasses() {
            return Util.<Class<? extends Symbol>>asSet(Symbol.class);
        }
    }
}
