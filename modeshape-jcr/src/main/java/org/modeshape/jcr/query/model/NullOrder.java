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

import org.modeshape.common.util.CheckArg;

/**
 * An enumeration for the kind of {@link Ordering}.
 */
public enum NullOrder implements Readable {
    NULLS_FIRST("NULLS FIRST"),
    NULLS_LAST("NULLS LAST");

    public static NullOrder defaultOrder( Order order ) {
        return order == Order.ASCENDING ? NullOrder.NULLS_LAST : NullOrder.NULLS_FIRST;
    }

    private final String symbol;

    private NullOrder( String symbol ) {
        this.symbol = symbol;
    }

    /**
     * Get the symbolic representation of the order
     * 
     * @return the symbolic representation; never null
     */
    public String symbol() {
        return symbol;
    }

    @Override
    public String toString() {
        return symbol;
    }

    /**
     * Attempt to find the Order given a symbol. The matching is done independent of case.
     * 
     * @param symbol the symbol
     * @return the Order having the supplied symbol, or null if there is no Order with the supplied symbol
     * @throws IllegalArgumentException if the symbol is null
     */
    public static NullOrder forSymbol( String symbol ) {
        CheckArg.isNotNull(symbol, "symbol");
        if (NULLS_FIRST.symbol().equalsIgnoreCase(symbol)) return NULLS_FIRST;
        if (NULLS_LAST.symbol().equalsIgnoreCase(symbol)) return NULLS_LAST;
        return null;
    }

    @Override
    public String getString() {
        return symbol();
    }
}
