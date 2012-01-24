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
package org.modeshape.jcr.query.model;

import org.modeshape.common.util.CheckArg;

/**
 * An enumeration for the kind of {@link Ordering}.
 */
public enum Order implements Readable {
    ASCENDING("ASC"),
    DESCENDING("DESC");

    private final String symbol;

    private Order( String symbol ) {
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
    public static Order forSymbol( String symbol ) {
        CheckArg.isNotNull(symbol, "symbol");
        if (ASCENDING.symbol().equalsIgnoreCase(symbol)) return ASCENDING;
        if (DESCENDING.symbol().equalsIgnoreCase(symbol)) return DESCENDING;
        return null;
    }

    @Override
    public String getString() {
        return symbol();
    }
}
