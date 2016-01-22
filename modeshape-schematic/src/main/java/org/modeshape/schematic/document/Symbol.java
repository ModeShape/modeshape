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
import org.modeshape.schematic.annotation.Immutable;

/**
 * A {@link Bson.Type#SYMBOL symbol} value for use within a {@link Document BSON Object}.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 */
@Immutable
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
}
