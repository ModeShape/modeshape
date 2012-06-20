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
import java.io.Serializable;
import java.util.Set;
import org.infinispan.marshall.SerializeWith;
import org.infinispan.schematic.internal.SchematicExternalizer;
import org.infinispan.schematic.internal.marshall.Ids;
import org.infinispan.util.Util;

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
