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
package org.modeshape.jcr.api.query.qom;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum Operator {
    EQUAL_TO("="),
    NOT_EQUAL_TO("<>"),
    LESS_THAN("<"),
    LESS_THAN_OR_EQUAL_TO("<="),
    GREATER_THAN(">"),
    GREATER_THAN_OR_EQUAL_TO(">="),
    LIKE("LIKE");

    private static final Map<String, Operator> OPERATORS_BY_SYMBOL;
    static {
        Map<String, Operator> opsBySymbol = new HashMap<String, Operator>();
        for (Operator operator : Operator.values()) {
            opsBySymbol.put(operator.symbol().toUpperCase(), operator);
        }
        opsBySymbol.put("!=", NOT_EQUAL_TO);
        OPERATORS_BY_SYMBOL = Collections.unmodifiableMap(opsBySymbol);
    }

    private final String symbol;

    private Operator( String symbol ) {
        this.symbol = symbol;
    }

    /**
     * Get the symbol for this operator
     * 
     * @return the symbolic representation; never null
     */
    public String symbol() {
        return symbol;
    }

    /**
     * Get the equivalent operator if the operands are to be reversed.
     * 
     * @return the reverse operator; never null
     */
    public Operator reverse() {
        switch (this) {
            case GREATER_THAN:
                return LESS_THAN;
            case GREATER_THAN_OR_EQUAL_TO:
                return LESS_THAN_OR_EQUAL_TO;
            case LESS_THAN:
                return GREATER_THAN;
            case LESS_THAN_OR_EQUAL_TO:
                return GREATER_THAN_OR_EQUAL_TO;
            case EQUAL_TO:
            case LIKE:
            case NOT_EQUAL_TO:
            default:
                return this;
        }
    }

    /**
     * Get the NOT of this operator.
     * 
     * @return the operator after a NOT operation; never null
     */
    public Operator not() {
        switch (this) {
            case GREATER_THAN:
                return LESS_THAN_OR_EQUAL_TO;
            case GREATER_THAN_OR_EQUAL_TO:
                return LESS_THAN;
            case LESS_THAN:
                return GREATER_THAN_OR_EQUAL_TO;
            case LESS_THAN_OR_EQUAL_TO:
                return GREATER_THAN;
            case EQUAL_TO:
                return NOT_EQUAL_TO;
            case NOT_EQUAL_TO:
                return EQUAL_TO;
            case LIKE:
            default:
                return this;
        }
    }

    /**
     * Determine whether this operator is one that is used to define a range of values: {@link #LESS_THAN <},
     * {@link #GREATER_THAN >}, {@link #LESS_THAN_OR_EQUAL_TO <=}, or {@link #GREATER_THAN_OR_EQUAL_TO >=}.
     * 
     * @return true if this operator is a range operator, or false otherwise
     */
    public boolean isRangeOperator() {
        switch (this) {
            case GREATER_THAN:
            case GREATER_THAN_OR_EQUAL_TO:
            case LESS_THAN:
            case LESS_THAN_OR_EQUAL_TO:
                return true;
            case EQUAL_TO:
            case LIKE:
            case NOT_EQUAL_TO:
            default:
                return false;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Enum#toString()
     */
    @Override
    public String toString() {
        return symbol;
    }

    /**
     * Attempt to find the Operator given a symbol. The matching is done independent of case.
     * 
     * @param symbol the symbol
     * @return the Operator having the supplied symbol, or null if there is no Operator with the supplied symbol
     * @throws IllegalArgumentException if the symbol is null
     */
    public static Operator forSymbol( String symbol ) {
        return OPERATORS_BY_SYMBOL.get(symbol.toUpperCase());
    }
}
