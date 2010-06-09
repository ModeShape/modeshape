/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.graph.query.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.modeshape.common.util.CheckArg;

/**
 * 
 */
public enum Operator {
    EQUAL_TO("="),
    NOT_EQUAL_TO("!="),
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
        opsBySymbol.put("<>", NOT_EQUAL_TO);
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
        CheckArg.isNotNull(symbol, "symbol");
        return OPERATORS_BY_SYMBOL.get(symbol.toUpperCase());
    }
}
