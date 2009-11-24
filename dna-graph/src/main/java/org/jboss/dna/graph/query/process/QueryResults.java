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
package org.jboss.dna.graph.query.process;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.collection.Problems;
import org.jboss.dna.common.util.StringUtil;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.GraphI18n;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.property.Binary;
import org.jboss.dna.graph.property.ValueFactory;
import org.jboss.dna.graph.query.QueryContext;
import org.jboss.dna.graph.query.model.QueryCommand;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

/**
 * The resulting output of a query.
 */
@Immutable
public class QueryResults implements org.jboss.dna.graph.query.QueryResults {

    private final QueryContext context;
    private final QueryCommand command;
    private final Columns columns;
    private final List<Object[]> tuples;
    private final Statistics statistics;

    /**
     * Create a results object for the supplied context, command, and result columns and with the supplied tuples.
     * 
     * @param context the context in which the query was executed
     * @param command the query command
     * @param columns the definition of the query result columns
     * @param statistics the statistics for this query; may not be null
     * @param tuples the tuples
     */
    public QueryResults( QueryContext context,
                         QueryCommand command,
                         Columns columns,
                         Statistics statistics,
                         List<Object[]> tuples ) {
        assert context != null;
        assert command != null;
        assert columns != null;
        assert statistics != null;
        this.context = context;
        this.command = command;
        this.columns = columns;
        this.tuples = tuples;
        this.statistics = statistics;
    }

    /**
     * Create an empty {@link QueryResults} object for the supplied context, command, and result columns.
     * 
     * @param context the context in which the query was executed
     * @param command the query command
     * @param columns the definition of the query result columns
     * @param statistics the statistics for this query; may not be null
     */
    public QueryResults( QueryContext context,
                         QueryCommand command,
                         Columns columns,
                         Statistics statistics ) {
        this(context, command, columns, statistics, Collections.<Object[]>emptyList());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.QueryResults#getContext()
     */
    public ExecutionContext getContext() {
        return context.getExecutionContext();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.QueryResults#getCommand()
     */
    public QueryCommand getCommand() {
        return command;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.QueryResults#getColumns()
     */
    public Columns getColumns() {
        return columns;
    }

    /**
     * Get a cursor that can be used to walk through the results.
     * 
     * @return the cursor; never null, though possibly empty (meaning {@link Cursor#hasNext()} may return true)
     */
    public Cursor getCursor() {
        return new TupleCursor(columns, tuples.iterator());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.QueryResults#getTuples()
     */
    public List<Object[]> getTuples() {
        return tuples;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.QueryResults#getRowCount()
     */
    public int getRowCount() {
        return tuples.size();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.QueryResults#getProblems()
     */
    public Problems getProblems() {
        return context.getProblems();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.QueryResults#hasErrors()
     */
    public boolean hasErrors() {
        return getProblems().hasErrors();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.QueryResults#hasWarnings()
     */
    public boolean hasWarnings() {
        return getProblems().hasWarnings();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.QueryResults#getStatistics()
     */
    public Statistics getStatistics() {
        return statistics;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return toString(Integer.MAX_VALUE);
    }

    /**
     * Get a string representation of this result object, with a maximum number of tuples to include.
     * 
     * @param maxTuples the maximum number of tuples to print, or {@link Integer#MAX_VALUE} if all the tuples are to be printed
     * @return the string representation; never null
     */
    public String toString( int maxTuples ) {
        StringBuilder sb = new StringBuilder();
        toString(sb, maxTuples);
        return sb.toString();
    }

    /**
     * Get a string representation of this result object.
     * 
     * @param sb the string builder to which the results should be written; may not be null
     */
    public void toString( StringBuilder sb ) {
        toString(sb, Integer.MAX_VALUE);
    }

    /**
     * Get a string representation of this result object, with a maximum number of tuples to include.
     * 
     * @param sb the string builder to which the results should be written; may not be null
     * @param maxTuples the maximum number of tuples to print, or {@link Integer#MAX_VALUE} if all the tuples are to be printed
     */
    public void toString( StringBuilder sb,
                          int maxTuples ) {
        ValueFactory<String> stringFactory = context.getExecutionContext().getValueFactories().getStringFactory();
        int[] columnWidths = determineColumnWidths(Integer.MAX_VALUE, true, stringFactory);
        printDelimiterLine(sb, columnWidths, true);
        printHeader(sb, columnWidths);
        printDelimiterLine(sb, columnWidths, true);
        printLines(sb, columnWidths, stringFactory, maxTuples);
        printDelimiterLine(sb, columnWidths, false);
    }

    /**
     * Determine the width of each column.
     * 
     * @param maxWidth the maximum width; must be positive
     * @param useData true if the data should be used to compute the length, or false if just the column names should be used
     * @param stringFactory the value factory for creating strings; may not be null
     * @return the array of widths for each column, excluding any decorating characters; never null
     */
    protected int[] determineColumnWidths( int maxWidth,
                                           boolean useData,
                                           ValueFactory<String> stringFactory ) {
        assert maxWidth > 0;
        int tupleLength = columns.getTupleSize();
        int[] columnWidths = new int[tupleLength + 1]; // +1 for the row number column
        for (int i = 0; i != columnWidths.length; ++i) {
            columnWidths[i] = 0;
        }
        // Determine the width of the first column that shows the row number ...
        String rowNumber = stringFactory.create(getTuples().size());
        columnWidths[0] = rowNumber.length();

        // Compute the column names ...
        List<String> tupleValueNames = columns.getTupleValueNames();
        for (int i = 0, j = 1, max = tupleValueNames.size(); i != max; ++i, ++j) {
            String name = tupleValueNames.get(i);
            columnWidths[j] = Math.max(Math.min(maxWidth, name.length()), columnWidths[j]);
        }
        // Look at the data ...
        if (useData) {
            for (Object[] tuple : getTuples()) {
                for (int i = 0, j = 1; i != tupleLength; ++i, ++j) {
                    String valueStr = stringOf(tuple[i], stringFactory);
                    if (valueStr == null) continue;
                    columnWidths[j] = Math.max(Math.min(maxWidth, valueStr.length()), columnWidths[j]);
                }
            }
        }
        return columnWidths;
    }

    protected String stringOf( Object value,
                               ValueFactory<String> stringFactory ) {
        if (value instanceof Binary) {
            // Just print out the SHA-1 hash in Base64, plus length
            Binary binary = (Binary)value;
            return "(Binary,length=" + binary.getSize() + ",SHA1=" + Base64.encode(binary.getHash()) + ")";
        }
        return stringFactory.create(value);
    }

    protected void printHeader( StringBuilder sb,
                                int[] columnWidths ) {
        // Print the row number column ...
        sb.append("| ").append(StringUtil.justifyLeft("#", columnWidths[0], ' ')).append(' ');
        // Print the name line ...
        sb.append('|');
        int i = 1;
        for (String name : columns.getTupleValueNames()) {
            sb.append(' ');
            sb.append(StringUtil.justifyLeft(name, columnWidths[i], ' '));
            sb.append(" |");
            ++i;
        }
        sb.append('\n');
    }

    protected void printLines( StringBuilder sb,
                               int[] columnWidths,
                               ValueFactory<String> stringFactory,
                               int maxRowsToPrint ) {
        int rowNumber = 1;
        int tupleLength = columns.getTupleSize();
        // Should they all be printed ?
        if (maxRowsToPrint > tuples.size()) {
            // Print all tuples ...
            for (Object[] tuple : getTuples()) {
                printTuple(sb, columnWidths, stringFactory, rowNumber, tupleLength, tuple);
                ++rowNumber;
            }
        } else {
            // Print max number of rows ...
            for (Object[] tuple : getTuples()) {
                printTuple(sb, columnWidths, stringFactory, rowNumber, tupleLength, tuple);
                if (rowNumber >= maxRowsToPrint) break;
                ++rowNumber;
            }
        }

    }

    /**
     * @param sb
     * @param columnWidths
     * @param stringFactory
     * @param rowNumber
     * @param tupleLength
     * @param tuple
     */
    private final void printTuple( StringBuilder sb,
                                   int[] columnWidths,
                                   ValueFactory<String> stringFactory,
                                   int rowNumber,
                                   int tupleLength,
                                   Object[] tuple ) {
        // Print the row number column ...
        sb.append("| ").append(StringUtil.justifyLeft(Integer.toString(rowNumber), columnWidths[0], ' ')).append(' ');
        // Print the remaining columns ...
        for (int i = 0, j = 1; i != tupleLength; ++i, ++j) {
            String valueStr = stringOf(tuple[i], stringFactory);
            valueStr = StringUtil.justifyLeft(valueStr, columnWidths[j], ' ');
            sb.append('|').append(' ').append(valueStr).append(' ');
        }
        sb.append('|');
        sb.append('\n');
    }

    protected void printDelimiterLine( StringBuilder sb,
                                       int[] columnWidths,
                                       boolean includeLineFeed ) {
        sb.append('+');
        for (int i = 0, max = columnWidths.length; i != max; ++i) {
            for (int j = 0, width = columnWidths[i] + 2; j != width; ++j) { // +1 for space before, +1 for space after
                sb.append('-');
            }
            sb.append('+');
        }
        if (includeLineFeed) sb.append('\n');
    }

    /**
     * An interface used to walk through the results.
     */
    public final class TupleCursor implements Cursor {
        private final Columns columns;
        private final Iterator<Object[]> iterator;
        private Object[] currentTuple;
        private int tupleIndex;

        protected TupleCursor( Columns columns,
                               Iterator<Object[]> iterator ) {
            this.iterator = iterator;
            this.columns = columns;
            this.tupleIndex = -1;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.QueryResults.Cursor#hasNext()
         */
        public boolean hasNext() {
            return iterator.hasNext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.QueryResults.Cursor#next()
         */
        public void next() {
            currentTuple = iterator.next();
            ++tupleIndex;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.QueryResults.Cursor#getLocation(int)
         */
        public Location getLocation( int columnNumber ) {
            return (Location)currentTuple[columns.getLocationIndexForColumn(columnNumber)];
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.QueryResults.Cursor#getLocation(java.lang.String)
         */
        public Location getLocation( String selectorName ) {
            return (Location)currentTuple[columns.getLocationIndex(selectorName)];
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.QueryResults.Cursor#getRowIndex()
         */
        public int getRowIndex() {
            return tupleIndex;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.QueryResults.Cursor#getValue(int)
         */
        public Object getValue( int columnNumber ) {
            if (columnNumber >= columns.getColumnCount()) {
                throw new IndexOutOfBoundsException();
            }
            if (currentTuple == null) {
                throw new IllegalStateException(GraphI18n.nextMethodMustBeCalledBeforeGettingValue.text());
            }
            return currentTuple[columnNumber];
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.query.QueryResults.Cursor#getValue(java.lang.String)
         */
        public Object getValue( String columnName ) {
            if (currentTuple == null) {
                throw new IllegalStateException(GraphI18n.nextMethodMustBeCalledBeforeGettingValue.text());
            }
            return currentTuple[columns.getColumnIndexForName(columnName)];
        }
    }
}
