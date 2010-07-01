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
package org.modeshape.graph.query;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import net.jcip.annotations.Immutable;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.Location;
import org.modeshape.graph.query.model.Column;
import org.modeshape.graph.query.model.TypeSystem.TypeFactory;

/**
 * The resulting output of a query.
 */
@Immutable
public interface QueryResults extends Serializable {

    /**
     * Get the description of the columns contained in these results. These columns can be used to discover the indexes of the
     * corresponding values from the arrays representing the {@link #getTuples() tuples}.
     * 
     * @return the column descriptions; never null
     */
    public Columns getColumns();

    /**
     * Get a cursor that can be used to walk through the results.
     * 
     * @return the cursor; never null, though possibly empty (meaning {@link Cursor#hasNext()} may return true)
     */
    public Cursor getCursor();

    /**
     * Get the actual tuples that contain the results. Each element in the list represents a tuple, and each tuple corresponds to
     * the column definitions.
     * 
     * @return the list of tuples; never null but possibly empty
     */
    public List<Object[]> getTuples();

    /**
     * Get the number of rows in the results.
     * 
     * @return the number of rows; never negative
     */
    public int getRowCount();

    /**
     * Get a description of the query plan, if requested.
     * 
     * @return the query plan, or null if the plan was not requested
     */
    public String getPlan();

    /**
     * Get the problems encountered during execution.
     * 
     * @return the problems; never null but possibly empty
     */
    public Problems getProblems();

    /**
     * Return true if there is at least one error recorded in the {@link #getProblems() problems}.
     * 
     * @return true if there is one or more errors associated with the query, or false otherwise
     */
    public boolean hasErrors();

    /**
     * Return true if there is at least one warning recorded in the {@link #getProblems() problems}.
     * 
     * @return true if there is one or more warnings associated with the query, or false otherwise
     */
    public boolean hasWarnings();

    /**
     * Get the statistics that describe the time metrics for this query.
     * 
     * @return the statistics; never null
     */
    public Statistics getStatistics();

    /**
     * An interface used to walk through the results.
     */
    public interface Cursor {
        /**
         * Determine whether this cursor can be moved from its current position to the next row.
         * 
         * @return true if there is another row, or false otherwise
         */
        boolean hasNext();

        /**
         * Move this cursor position to the next row. obtained for
         * 
         * @throws NoSuchElementException if there is no next row that the cursor can point to.
         */
        void next();

        /**
         * Get the 0-based index of the current row.
         * 
         * @return the index of the current row; never negative
         * @exception IllegalStateException if the <tt>next()</tt> method has not yet been called
         */
        int getRowIndex();

        /**
         * Get from the current row the location of the node from which the value in the given column was taken.
         * 
         * @param columnNumber the column number (0-based)
         * @return the location of the node; never null
         * @throws IndexOutOfBoundsException if the column number is negative or larger than the number of columns
         * @throws IllegalStateException if the <tt>next()</tt> method has not yet been called, or if there are no results
         */
        Location getLocation( int columnNumber );

        /**
         * Get from the current row the location of the node that was produced by the named selector.
         * 
         * @param selectorName the name of the selector that resulted in a node appearing in the current row
         * @return the location of the node; or null if there is no node corresponding to the named selector for the current row
         * @throws NoSuchElementException if the selector name does not correspond to an available selector
         * @throws IllegalStateException if the <tt>next()</tt> method has not yet been called, or if there are no results
         */
        Location getLocation( String selectorName );

        /**
         * Get from the current row the value in the given column.
         * 
         * @param columnNumber the column number (0-based)
         * @return the value; possibly null
         * @throws IndexOutOfBoundsException if the column number is negative or larger than the number of columns
         * @throws IllegalStateException if the <tt>next()</tt> method has not yet been called, or if there are no results
         */
        Object getValue( int columnNumber );

        /**
         * Get the value in the named column.
         * 
         * @param columnName the name of the column
         * @return the value; possibly null
         * @throws NoSuchElementException if the column name does not correspond to an available column
         * @throws IllegalStateException if the <tt>next()</tt> method has not yet been called, or if there are no results
         */
        Object getValue( String columnName );
    }

    /**
     * Definition of the columns that are available in the results, which outline the structure of the
     * {@link QueryResults#getTuples() tuples} in the results, and which can be used to access the individual values in each of
     * the tuples.
     */
    @Immutable
    public interface Columns extends Serializable, Iterable<Column> {
        /**
         * Get the columns.
         * 
         * @return the immutable list of columns, with size equal to {@link #getColumnCount()}; never null
         */
        public List<? extends Column> getColumns();

        /**
         * Get the names of the columns.
         * 
         * @return the immutable list of column names, with size equal to {@link #getColumnCount()}; never null
         */
        public List<String> getColumnNames();

        /**
         * Get the {@link TypeFactory#getTypeName() type name} for each column.
         * 
         * @return the immutable list of type names, with size equal to {@link #getColumnCount()}; never null
         */
        public List<String> getColumnTypes();

        /**
         * Get the number of columns in each tuple.
         * 
         * @return the number of columns; always positive
         */
        public int getColumnCount();

        /**
         * Get the number of {@link Location} objects in each tuple.
         * 
         * @return the number of Location objects; always positive
         */
        public int getLocationCount();

        /**
         * Get the names of the selectors that are associated with these results. These results contain a single {@link Location}
         * object for each of the selectors.
         * 
         * @return the immutable list of selector names, with size equal to {@link #getLocationCount()}; never null
         */
        public List<String> getSelectorNames();

        /**
         * Get the size of the tuple arrays.
         * 
         * @return the length of each tuple array
         */
        public int getTupleSize();

        /**
         * Get the names of the all of the tuple values.
         * 
         * @return the immutable list of names
         */
        public List<String> getTupleValueNames();

        /**
         * Get the index of a tuple's correct Location object given the column index.
         * 
         * @param columnIndex the column index
         * @return the Location index that corresponds to the supplied column; never negative
         * @throws IndexOutOfBoundsException if the column index is invalid
         */
        public int getLocationIndexForColumn( int columnIndex );

        /**
         * Get the index of a tuple's correct Location object given the column index.
         * 
         * @param columnName the column name
         * @return the Location index that corresponds to the supplied column; never negative
         * @throws NoSuchElementException if the column name is invalid
         */
        public int getLocationIndexForColumn( String columnName );

        /**
         * Get the index of a tuple's correct Location object given the name of the selector used in the query.
         * 
         * @param selectorName the selector name
         * @return the Location index that corresponds to the supplied column; never negative
         * @throws NoSuchElementException if the selector name is invalid
         */
        public int getLocationIndex( String selectorName );

        /**
         * Determine if these results contain values from the selector with the supplied name.
         * 
         * @param selectorName the selector name
         * @return true if the results have values from the supplied selector, or false otherwise
         */
        public boolean hasSelector( String selectorName );

        /**
         * Get the name of the property that corresponds to the supplied column in each tuple.
         * 
         * @param columnIndex the column index
         * @return the property name; never null
         * @throws IndexOutOfBoundsException if the column index is invalid
         */
        public String getPropertyNameForColumn( int columnIndex );

        /**
         * Get the index of the column given the column name.
         * 
         * @param columnName the column name
         * @return the column index
         * @throws NoSuchElementException if the column name is invalid or doesn't match an existing column
         */
        public int getColumnIndexForName( String columnName );

        /**
         * Get the index of the column given the name of the selector and the property name from where the column should be
         * obtained.
         * 
         * @param selectorName the selector name
         * @param propertyName the name of the property
         * @return the column index that corresponds to the supplied column; never negative
         * @throws NoSuchElementException if the selector name or the property name are invalid
         */
        public int getColumnIndexForProperty( String selectorName,
                                              String propertyName );

        /**
         * Get the index of the tuple value containing the full-text search score for the node taken from the named selector.
         * 
         * @param selectorName the selector name
         * @return the index that corresponds to the {@link Double} full-text search score, or -1 if there is no full-text search
         *         score for the named selector
         * @throws NoSuchElementException if the selector name is invalid
         */
        public int getFullTextSearchScoreIndexFor( String selectorName );

        /**
         * Determine whether these results include full-text search scores.
         * 
         * @return true if the full-text search scores are included in the results, or false otherwise
         */
        public boolean hasFullTextSearchScores();

        /**
         * Determine whether this mapping includes all of the columns (and locations) in the supplied mapping.
         * 
         * @param other the other mapping; may not be null
         * @return true if all of the other mapping's columns and locations are included in this mapping, or false otherwise
         */
        public boolean includes( Columns other );

        /**
         * Determine whether this column and the other are <i>union-compatible</i> (that is, having the same columns).
         * 
         * @param other the other mapping; may not be null
         * @return true if this and the supplied columns definition are union-compatible, or false if they are not
         */
        public boolean isUnionCompatible( Columns other );

        /**
         * Obtain a new definition for the query results that can be used to reference the same tuples that use this columns
         * definition, but that defines a subset of the columns in this definition. This is useful in a PROJECT operation, since
         * that reduces the number of columns.
         * 
         * @param columns the new columns, which must be a subset of the columns in this definition; may not be null
         * @return the new columns definition; never null
         */
        public Columns subSelect( List<Column> columns );

        /**
         * Obtain a new definition for the query results that can be used to reference the same tuples that use this columns
         * definition, but that defines a subset of the columns in this definition. This is useful in a PROJECT operation, since
         * that reduces the number of columns.
         * 
         * @param columns the new columns, which must be a subset of the columns in this definition; may not be null
         * @return the new columns definition; never null
         */
        public Columns subSelect( Column... columns );

        /**
         * Obtain a new definition for the query results that is a combination of the these columns and the supplied columns,
         * where the columns from this object appear first, followed by columns from the supplied set. This is useful in a JOIN
         * operation.
         * 
         * @param columns the new columns, which must be a subset of the columns in this definition; may not be null
         * @return the new columns definition; never null
         */
        public Columns joinWith( Columns columns );
    }

    @Immutable
    public static class Statistics implements Comparable<Statistics>, Serializable {
        private static final long serialVersionUID = 1L;

        protected static final Statistics EMPTY_STATISTICS = new Statistics();

        private final long planningNanos;
        private final long optimizationNanos;
        private final long resultFormulationNanos;
        private final long executionNanos;

        public Statistics() {
            this(0L, 0L, 0L, 0L);
        }

        public Statistics( long planningNanos ) {
            this(planningNanos, 0L, 0L, 0L);
        }

        public Statistics( long planningNanos,
                           long optimizationNanos,
                           long resultFormulationNanos,
                           long executionNanos ) {
            this.planningNanos = planningNanos;
            this.optimizationNanos = optimizationNanos;
            this.resultFormulationNanos = resultFormulationNanos;
            this.executionNanos = executionNanos;
        }

        /**
         * Get the time required to come up with the canonical plan.
         * 
         * @param unit the time unit that should be used
         * @return the time to plan, in the desired units
         * @throws IllegalArgumentException if the unit is null
         */
        public long getPlanningTime( TimeUnit unit ) {
            CheckArg.isNotNull(unit, "unit");
            return unit.convert(planningNanos, TimeUnit.NANOSECONDS);
        }

        /**
         * Get the time required to determine or select a (more) optimal plan.
         * 
         * @param unit the time unit that should be used
         * @return the time to determine an optimal plan, in the desired units
         * @throws IllegalArgumentException if the unit is null
         */
        public long getOptimizationTime( TimeUnit unit ) {
            CheckArg.isNotNull(unit, "unit");
            return unit.convert(optimizationNanos, TimeUnit.NANOSECONDS);
        }

        /**
         * Get the time required to formulate the structure of the results.
         * 
         * @param unit the time unit that should be used
         * @return the time to formulate the results, in the desired units
         * @throws IllegalArgumentException if the unit is null
         */
        public long getResultFormulationTime( TimeUnit unit ) {
            CheckArg.isNotNull(unit, "unit");
            return unit.convert(resultFormulationNanos, TimeUnit.NANOSECONDS);
        }

        /**
         * Get the time required to execute the query.
         * 
         * @param unit the time unit that should be used
         * @return the time to execute the query, in the desired units
         * @throws IllegalArgumentException if the unit is null
         */
        public long getExecutionTime( TimeUnit unit ) {
            return unit.convert(executionNanos, TimeUnit.NANOSECONDS);
        }

        /**
         * Get the time required to execute the query.
         * 
         * @param unit the time unit that should be used
         * @return the time to execute the query, in the desired units
         * @throws IllegalArgumentException if the unit is null
         */
        public long getTotalTime( TimeUnit unit ) {
            return unit.convert(totalTime(), TimeUnit.NANOSECONDS);
        }

        protected long totalTime() {
            return planningNanos + optimizationNanos + resultFormulationNanos + executionNanos;
        }

        /**
         * Create a new statistics object that has the supplied planning time.
         * 
         * @param planningNanos the number of nanoseconds required by planning
         * @return the new statistics object; never null
         * @throws IllegalArgumentException if the time value is negative
         */
        public Statistics withPlanningTime( long planningNanos ) {
            CheckArg.isNonNegative(planningNanos, "planningNanos");
            return new Statistics(planningNanos, optimizationNanos, resultFormulationNanos, executionNanos);
        }

        /**
         * Create a new statistics object that has the supplied optimization time.
         * 
         * @param optimizationNanos the number of nanoseconds required by optimization
         * @return the new statistics object; never null
         * @throws IllegalArgumentException if the time value is negative
         */
        public Statistics withOptimizationTime( long optimizationNanos ) {
            CheckArg.isNonNegative(optimizationNanos, "optimizationNanos");
            return new Statistics(planningNanos, optimizationNanos, resultFormulationNanos, executionNanos);
        }

        /**
         * Create a new statistics object that has the supplied result formulation time.
         * 
         * @param resultFormulationNanos the number of nanoseconds required by result formulation
         * @return the new statistics object; never null
         * @throws IllegalArgumentException if the time value is negative
         */
        public Statistics withResultsFormulationTime( long resultFormulationNanos ) {
            CheckArg.isNonNegative(resultFormulationNanos, "resultFormulationNanos");
            return new Statistics(planningNanos, optimizationNanos, resultFormulationNanos, executionNanos);
        }

        /**
         * Create a new statistics object that has the supplied execution time.
         * 
         * @param executionNanos the number of nanoseconds required to execute the query
         * @return the new statistics object; never null
         * @throws IllegalArgumentException if the time value is negative
         */
        public Statistics withExecutionTime( long executionNanos ) {
            CheckArg.isNonNegative(executionNanos, "executionNanos");
            return new Statistics(planningNanos, optimizationNanos, resultFormulationNanos, executionNanos);
        }

        /**
         * Create a new statistics object that has the supplied planning time.
         * 
         * @param planning the time required to plan the query
         * @param unit the time unit
         * @return the new statistics object; never null
         * @throws IllegalArgumentException if the unit is null or if the time value is negative
         */
        public Statistics withPlanningTime( long planning,
                                            TimeUnit unit ) {
            CheckArg.isNonNegative(planning, "planning");
            CheckArg.isNotNull(unit, "unit");
            long planningNanos = TimeUnit.NANOSECONDS.convert(planning, unit);
            return new Statistics(planningNanos, optimizationNanos, resultFormulationNanos, executionNanos);
        }

        /**
         * Create a new statistics object that has the supplied optimization time.
         * 
         * @param optimization the time required by optimization
         * @param unit the time unit
         * @return the new statistics object; never null
         * @throws IllegalArgumentException if the unit is null or if the time value is negative
         */
        public Statistics withOptimizationTime( long optimization,
                                                TimeUnit unit ) {
            CheckArg.isNonNegative(optimization, "optimization");
            CheckArg.isNotNull(unit, "unit");
            long optimizationNanos = TimeUnit.NANOSECONDS.convert(optimization, unit);
            return new Statistics(planningNanos, optimizationNanos, resultFormulationNanos, executionNanos);
        }

        /**
         * Create a new statistics object that has the supplied result formulation time.
         * 
         * @param resultFormulation the time required to formulate the results
         * @param unit the time unit
         * @return the new statistics object; never null
         * @throws IllegalArgumentException if the unit is null or if the time value is negative
         */
        public Statistics withResultsFormulationTime( long resultFormulation,
                                                      TimeUnit unit ) {
            CheckArg.isNonNegative(resultFormulation, "resultFormulation");
            CheckArg.isNotNull(unit, "unit");
            long resultFormulationNanos = TimeUnit.NANOSECONDS.convert(resultFormulation, unit);
            return new Statistics(planningNanos, optimizationNanos, resultFormulationNanos, executionNanos);
        }

        /**
         * Create a new statistics object that has the supplied execution time.
         * 
         * @param execution the time required to execute the query
         * @param unit the time unit
         * @return the new statistics object; never null
         * @throws IllegalArgumentException if the unit is null or if the time value is negative
         */
        public Statistics withExecutionTime( long execution,
                                             TimeUnit unit ) {
            CheckArg.isNonNegative(execution, "execution");
            CheckArg.isNotNull(unit, "unit");
            long executionNanos = TimeUnit.NANOSECONDS.convert(execution, unit);
            return new Statistics(planningNanos, optimizationNanos, resultFormulationNanos, executionNanos);
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        public int compareTo( Statistics that ) {
            if (that == this) return 0;
            long diff = this.totalTime() - that.totalTime();
            if (diff < 0) return -1;
            if (diff > 0) return 1;
            return 0;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            readable(totalTime(), sb);
            boolean first = false;
            if (planningNanos != 0L) {
                sb.append(" (plan=");
                readable(planningNanos, sb);
                first = false;
            }
            if (optimizationNanos != 0L) {
                if (first) {
                    first = false;
                    sb.append(" (");
                } else {
                    sb.append(" ,");
                }
                sb.append("opt=");
                readable(optimizationNanos, sb);
            }
            if (resultFormulationNanos != 0L) {
                if (first) {
                    first = false;
                    sb.append(" (");
                } else {
                    sb.append(" ,");
                }
                sb.append("res=");
                readable(resultFormulationNanos, sb);
            }
            if (executionNanos != 0L) {
                if (first) {
                    first = false;
                    sb.append(" (");
                } else {
                    sb.append(" ,");
                }
                sb.append("exec=");
                readable(executionNanos, sb);
            }
            if (!first) sb.append(')');
            return sb.toString();
        }

        protected void readable( long nanos,
                                 StringBuilder sb ) {
            // 3210987654321
            // XXXXXXXXXXXXX nanos
            // XXXXXXXXXX micros
            // XXXXXXX millis
            // XXXX seconds
            if (nanos < 1000) {
                sb.append(nanos).append(" ns");
            } else if (nanos < 1000000) {
                double value = nanos / 1000d;
                sb.append(FORMATTER.get().format(value)).append(" usec");
            } else if (nanos < 1000000000) {
                double value = nanos / 1000000d;
                sb.append(FORMATTER.get().format(value)).append(" ms");
            } else {
                double value = nanos / 1000000000d;
                sb.append(FORMATTER.get().format(value)).append(" sec");
            }
        }
    }

    static ThreadLocal<DecimalFormat> FORMATTER = new ThreadLocal<DecimalFormat>() {
        @Override
        protected synchronized DecimalFormat initialValue() {
            return new DecimalFormat("###,###,##0.0##");
        }
    };
}
