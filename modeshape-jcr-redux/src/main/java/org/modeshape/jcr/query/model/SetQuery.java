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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.HashCode;
import org.modeshape.common.util.ObjectUtil;
import org.modeshape.jcr.GraphI18n;
import org.modeshape.jcr.api.query.qom.QueryObjectModelConstants;

/**
 * This object acts as a Set operator on multiple {@link QueryCommand queries}, such as performing UNION, INTERSECT, and EXCEPT
 * operations.
 * <p>
 * The two {@link QueryCommand queries} are expected to have the same number and order of columns, and the corresponding columns
 * types must be compatible.
 * </p>
 */
@Immutable
public class SetQuery implements QueryCommand, org.modeshape.jcr.api.query.qom.SetQuery {
    private static final long serialVersionUID = 1L;

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

        @Override
        public String getString() {
            return getSymbol();
        }
    }

    protected static boolean unionableColumns( List<? extends Column> left,
                                               List<? extends Column> right ) {
        // Check the column size first ...
        if (left.size() != right.size()) return false;
        // Same size, so check the columns ...
        Iterator<? extends Column> leftIter = left.iterator();
        Iterator<? extends Column> rightIter = right.iterator();
        while (leftIter.hasNext() && rightIter.hasNext()) {
            Column leftColumn = leftIter.next();
            Column rightColumn = rightIter.next();
            if (leftColumn == null || rightColumn == null) return false;
        }
        return leftIter.hasNext() == rightIter.hasNext();
    }

    private final List<? extends Ordering> orderings;
    private final Limit limits;
    private final QueryCommand left;
    private final QueryCommand right;
    private final Operation operation;
    private final boolean all;
    private final int hc;
    private transient Column[] columnArray;
    private transient Ordering[] orderingArray;

    /**
     * Create a set query involving the supplied left- and right-hand-side queries.
     * 
     * @param left the left-hand-side query being combined
     * @param operation the set operation
     * @param right the right-hand-side query being combined
     * @param all true if all of the results should be included
     * @throws IllegalArgumentException if the left-hand-side query, right-hand-side query, or operation are null
     */
    public SetQuery( QueryCommand left,
                     Operation operation,
                     QueryCommand right,
                     boolean all ) {
        CheckArg.isNotNull(left, "left");
        CheckArg.isNotNull(right, "right");
        CheckArg.isNotNull(operation, "operation");
        if (!unionableColumns(left.columns(), right.columns())) {
            I18n msg = GraphI18n.leftAndRightQueriesInSetQueryMustHaveUnionableColumns;
            throw new IllegalArgumentException(msg.text(left.columns(), right.columns()));
        }
        this.left = left;
        this.right = right;
        this.operation = operation;
        this.all = all;
        this.orderings = Collections.<Ordering>emptyList();
        this.limits = Limit.NONE;
        this.hc = HashCode.compute(this.left, this.right, this.operation);
    }

    /**
     * Create a set query involving the supplied left- and right-hand-side queries.
     * 
     * @param left the left-hand-side query being combined
     * @param operation the set operation
     * @param right the right-hand-side query being combined
     * @param all true if all of the results should be included
     * @param orderings the specification of the order of the result rows, or null if the results need not be ordered
     * @param limit the limit for the result rows, or null if there are no limits
     * @throws IllegalArgumentException if the left-hand-side query, right-hand-side query, or operation are null
     */
    public SetQuery( QueryCommand left,
                     Operation operation,
                     QueryCommand right,
                     boolean all,
                     List<? extends Ordering> orderings,
                     Limit limit ) {
        CheckArg.isNotNull(left, "left");
        CheckArg.isNotNull(right, "right");
        CheckArg.isNotNull(operation, "operation");
        if (!unionableColumns(left.columns(), right.columns())) {
            I18n msg = GraphI18n.leftAndRightQueriesInSetQueryMustHaveUnionableColumns;
            throw new IllegalArgumentException(msg.text(left.columns(), right.columns()));
        }
        this.left = left;
        this.right = right;
        this.operation = operation;
        this.all = all;
        this.orderings = orderings != null ? orderings : Collections.<Ordering>emptyList();
        this.limits = limit != null ? limit : Limit.NONE;
        this.hc = HashCode.compute(this.left, this.right, this.operation);
    }

    @Override
    public List<? extends Column> columns() {
        return left.columns(); // equivalent to right columns
    }

    @Override
    public Limit getLimits() {
        return limits;
    }

    @Override
    public List<? extends Ordering> orderings() {
        return orderings;
    }

    @Override
    public QueryCommand getLeft() {
        return left;
    }

    @Override
    public QueryCommand getRight() {
        return right;
    }

    /**
     * Get the set operation for this query.
     * 
     * @return the operation; never null
     */
    public final Operation operation() {
        return operation;
    }

    @Override
    public String getOperation() {
        switch (operation()) {
            case UNION:
                return QueryObjectModelConstants.JCR_SET_TYPE_UNION;
            case INTERSECT:
                return QueryObjectModelConstants.JCR_SET_TYPE_INTERSECT;
            case EXCEPT:
                return QueryObjectModelConstants.JCR_SET_TYPE_EXCEPT;
        }
        assert false;
        return null;
    }

    @Override
    public final boolean isAll() {
        return all;
    }

    @Override
    public Ordering[] getOrderings() {
        if (orderingArray == null) {
            // this is idempotent ...
            orderingArray = orderings.toArray(new Ordering[orderings.size()]);
        }
        return orderingArray;
    }

    @Override
    public Column[] getColumns() {
        if (columnArray == null) {
            // this is idempotent ...
            List<? extends Column> columns = columns();
            columnArray = columns.toArray(new Column[columns.size()]);
        }
        return columnArray;
    }

    @Override
    public String toString() {
        return Visitors.readable(this);
    }

    @Override
    public int hashCode() {
        return hc;
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof SetQuery) {
            SetQuery that = (SetQuery)obj;
            if (this.hc != that.hc) return false;
            if (this.operation != that.operation) return false;
            if (!this.left.equals(that.left)) return false;
            if (!this.right.equals(that.right)) return false;
            if (!ObjectUtil.isEqualWithNulls(this.getLimits(), that.getLimits())) return false;
            if (!ObjectUtil.isEqualWithNulls(this.orderings(), that.orderings())) return false;
            return true;
        }
        return false;
    }

    @Override
    public void accept( Visitor visitor ) {
        visitor.visit(this);
    }

    @Override
    public SetQuery withLimit( int rowLimit ) {
        if (getLimits().getRowLimit() == rowLimit) return this; // nothing to change
        return new SetQuery(left, operation, right, all, orderings(), getLimits().withRowLimit(rowLimit));
    }

    @Override
    public SetQuery withOffset( int offset ) {
        if (getLimits().getOffset() == offset) return this; // nothing to change
        return new SetQuery(left, operation, right, all, orderings(), getLimits().withOffset(offset));
    }

    public SetQuery adding( Ordering... orderings ) {
        List<Ordering> newOrderings = null;
        if (this.orderings() != null) {
            newOrderings = new ArrayList<Ordering>(orderings());
            for (Ordering ordering : orderings) {
                newOrderings.add(ordering);
            }
        } else {
            newOrderings = Arrays.asList(orderings);
        }
        return new SetQuery(left, operation, right, all, newOrderings, getLimits());
    }
}
