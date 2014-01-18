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
package org.modeshape.jcr.query.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.modeshape.common.util.CheckArg;

public enum JoinType implements Readable {
    INNER("INNER JOIN"),
    LEFT_OUTER("LEFT OUTER JOIN"),
    RIGHT_OUTER("RIGHT OUTER JOIN"),
    FULL_OUTER("FULL OUTER JOIN"),
    CROSS("CROSS JOIN");

    private static final Map<String, JoinType> TYPE_BY_SYMBOL;
    static {
        Map<String, JoinType> typesBySymbol = new HashMap<String, JoinType>();
        for (JoinType type : JoinType.values()) {
            typesBySymbol.put(type.symbol().toUpperCase(), type);
        }
        TYPE_BY_SYMBOL = Collections.unmodifiableMap(typesBySymbol);
    }

    private final String symbol;

    private JoinType( String symbol ) {
        this.symbol = symbol;
    }

    /**
     * @return symbol
     */
    public String symbol() {
        return symbol;
    }

    /**
     * Check if this join type is an outer join.
     * 
     * @return true if left/right/full outer, or false otherwise
     */
    public boolean isOuter() {
        return this.equals(LEFT_OUTER) || this.equals(FULL_OUTER) || this.equals(RIGHT_OUTER);
    }

    @Override
    public String toString() {
        return symbol;
    }

    /**
     * Attempt to find the JoinType given a symbol. The matching is done independent of case.
     * 
     * @param symbol the symbol
     * @return the JoinType having the supplied symbol, or null if there is no JoinType with the supplied symbol
     * @throws IllegalArgumentException if the symbol is null
     */
    public static JoinType forSymbol( String symbol ) {
        CheckArg.isNotNull(symbol, "symbol");
        return TYPE_BY_SYMBOL.get(symbol.toUpperCase());
    }

    @Override
    public String getString() {
        return symbol();
    }
}
