/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.graph.query.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.common.util.ObjectUtil;
import org.jboss.dna.graph.ExecutionContext;

/**
 * This object acts as a Set operator on multiple {@link QueryCommand queries}, such as performing UNION, INTERSECT, and EXCEPT
 * operations.
 * <p>
 * The two {@link QueryCommand queries} are expected to have the same number and order of columns, and the corresponding columns
 * types must be compatible.
 * </p>
 */
@Immutable
public class SetQuery extends QueryCommand {

    public enum Operation implements Readable {
        UNION("UNION"),
        INTERSECT("INTERSECT"),
        EXCEPT("EXCEPT");

        private static final Map<String, Operation> OPERATIONS_BY_SYMBOL;
        static {
            Map<String, Operation> opsBySymbol = new HashMap<String, Operation>();
            for (Operation op : Operation.values()) {
                opsBySymbol.put(op.getSymbol(), op);
            }
            OPERATIONS_BY_SYMBOL = Collections.unmodifiableMap(opsBySymbol);
        }

        private final String symbol;

        private Operation( String symbol ) {
            this.symbol = symbol;
        }

        /**
         * @return symbol
         */
        public String getSymbol() {
            return symbol;
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

        public static Operation forSymbol( String symbol ) {
            CheckArg.isNotNull(symbol, "symbol");
            return OPERATIONS_BY_SYMBOL.get(symbol.toUpperCase());
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.model.Readable#getString(org.jboss.dna.graph.ExecutionContext)
         */
        public String getString( ExecutionContext context ) {
            return getSymbol();
        }
    }

    private final QueryCommand left;
    private final QueryCommand right;
    private final Operation operation;
    private final boolean all;

    public SetQuery( QueryCommand left,
                     Operation operation,
                     QueryCommand right,
                     boolean all ) {
        super();
        CheckArg.isNotNull(left, "left");
        CheckArg.isNotNull(right, "right");
        CheckArg.isNotNull(operation, "operation");
        this.left = left;
        this.right = right;
        this.operation = operation;
        this.all = all;
    }

    public SetQuery( QueryCommand left,
                     Operation operation,
                     QueryCommand right,
                     boolean all,
                     List<Ordering> orderings,
                     Limit limit ) {
        super(orderings, limit);
        CheckArg.isNotNull(left, "left");
        CheckArg.isNotNull(right, "right");
        CheckArg.isNotNull(operation, "operation");
        this.left = left;
        this.right = right;
        this.operation = operation;
        this.all = all;
    }

    /**
     * Get the left-hand query.
     * 
     * @return the left-hand query; never null
     */
    public final QueryCommand getLeft() {
        return left;
    }

    /**
     * Get the right-hand query.
     * 
     * @return the right-hand query; never null
     */
    public final QueryCommand getRight() {
        return right;
    }

    /**
     * Get the set operation for this query.
     * 
     * @return the operation; never null
     */
    public final Operation getOperation() {
        return operation;
    }

    /**
     * Return whether this set query is a 'UNION ALL' or 'INTERSECT ALL' or 'EXCEPT ALL' query.
     * 
     * @return true if this is an 'ALL' query, or false otherwise
     */
    public final boolean isAll() {
        return all;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return Visitors.readable(this);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof SetQuery) {
            SetQuery that = (SetQuery)obj;
            if (this.operation != that.operation) return false;
            if (!this.left.equals(that.left)) return false;
            if (!this.right.equals(that.right)) return false;
            if (!ObjectUtil.isEqualWithNulls(this.getLimits(), that.getLimits())) return false;
            if (!ObjectUtil.isEqualWithNulls(this.getOrderings(), that.getOrderings())) return false;
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.model.Visitable#accept(org.jboss.dna.graph.query.model.Visitor)
     */
    public void accept( Visitor visitor ) {
        visitor.visit(this);
    }

    public SetQuery withLimit( int rowLimit ) {
        return new SetQuery(left, operation, right, all, getOrderings(), getLimits().withRowLimit(rowLimit));
    }

    public SetQuery withOffset( int offset ) {
        return new SetQuery(left, operation, right, all, getOrderings(), getLimits().withOffset(offset));
    }

    public SetQuery adding( Ordering... orderings ) {
        List<Ordering> newOrderings = null;
        if (this.getOrderings() != null) {
            newOrderings = new ArrayList<Ordering>(getOrderings());
            for (Ordering ordering : orderings) {
                newOrderings.add(ordering);
            }
        } else {
            newOrderings = Arrays.asList(orderings);
        }
        return new SetQuery(left, operation, right, all, newOrderings, getLimits());
    }
}
