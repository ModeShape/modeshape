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
 * The arithmetic operators.
 */
public enum ArithmeticOperator {

    ADD("+", Arity.BINARY, 20),
    SUBTRACT("-", Arity.BINARY, 19),
    MULTIPLY("*", Arity.BINARY, 18),
    DIVIDE("/", Arity.BINARY, 17);

    public static enum Arity {
        UNARY,
        BINARY;
    }

    private static final Map<String, ArithmeticOperator> OPERATORS_BY_SYMBOL;
    static {
        Map<String, ArithmeticOperator> opsBySymbol = new HashMap<String, ArithmeticOperator>();
        for (ArithmeticOperator operator : ArithmeticOperator.values()) {
            opsBySymbol.put(operator.symbol(), operator);
        }
        OPERATORS_BY_SYMBOL = Collections.unmodifiableMap(opsBySymbol);
    }

    private final String symbol;
    private final Arity arity;
    private final int precedence;

    private ArithmeticOperator( String symbol,
                                Arity arity,
                                int precedence ) {
        this.symbol = symbol;
        this.arity = arity;
        this.precedence = precedence;
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
     * Get the 'arity' of the operator.
     * 
     * @return the number of parameters required
     * @see #isUnary()
     * @see #isBinary()
     */
    public Arity arity() {
        return arity;
    }

    /**
     * Return whether this is an unary operator.
     * 
     * @return true if this operator is unary, or false otherwise
     * @see #arity()
     * @see #isBinary()
     */
    public boolean isUnary() {
        return arity == Arity.UNARY;
    }

    /**
     * Return whether this is an binary operator.
     * 
     * @return true if this operator is binary, or false otherwise
     * @see #arity()
     * @see #isUnary()
     */
    public boolean isBinary() {
        return arity == Arity.BINARY;
    }

    /**
     * Determine whether this operator has a higher precedence than the supplied operator.
     * 
     * @param operator the other operator; may not be null
     * @return true if this operator has a higher precedence, or false otherwise
     */
    public boolean precedes( ArithmeticOperator operator ) {
        return this.precedence > operator.precedence;
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
    public static ArithmeticOperator forSymbol( String symbol ) {
        CheckArg.isNotNull(symbol, "symbol");
        return OPERATORS_BY_SYMBOL.get(symbol.toUpperCase());
    }
}
