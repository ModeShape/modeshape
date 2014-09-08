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
package org.modeshape.jcr.query;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.CachedNodeSupplier;
import org.modeshape.jcr.query.NodeSequence.Batch;
import org.modeshape.jcr.query.model.Column;

/**
 * The resulting output of a query.
 */
@Immutable
public interface QueryResults {

    /**
     * Get the description of the columns contained in these results.
     * 
     * @return the column descriptions; never null
     */
    public Columns getColumns();

    /**
     * Get the rows that make up these query results.
     * 
     * @return the sequence of rows; never null
     */
    public NodeSequence getRows();

    /**
     * Get the supplier with which a node can be found by key.
     * 
     * @return the supplier of {@link CachedNode} instances; never null
     */
    public CachedNodeSupplier getCachedNodes();

    /**
     * Get the number of rows in the results, if that information is available.
     * 
     * @return the number of rows; may be equal to -1 if the number of rows is not known without significant overhead
     */
    public long getRowCount();

    /**
     * Determine whether this results is known to be empty.
     * 
     * @return the true if there are no results, or false if there is at least some rows or if the number of rows is not known
     */
    public boolean isEmpty();

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
     * Definition of the columns that are available in the results.
     */
    @Immutable
    public interface Columns extends Serializable, Iterable<Column> {
        /**
         * Get the columns.
         * 
         * @return the immutable list of columns; never null
         */
        public List<? extends Column> getColumns();

        /**
         * Get the names of the columns.
         * 
         * @return the immutable list of column names, with size equal to the number of {@link #getColumns() columns}; never null
         */
        public List<String> getColumnNames();

        /**
         * Get the type name for each column.
         * 
         * @return the immutable list of type names, with size equal to the number of {@link #getColumns() columns}; never null
         */
        public List<String> getColumnTypes();

        /**
         * Get the type of the column given the name of the selector and the property name from where the column should be
         * obtained.
         * 
         * @param selectorName the selector name
         * @param propertyName the name of the property
         * @return the type for the named column
         * @throws NoSuchElementException if the selector name or the property name are invalid
         */
        public String getColumnTypeForProperty( String selectorName,
                                                String propertyName );

        /**
         * Get the index of the nodes for this selector in each of the {@link Batch node sequence batches}.
         * 
         * @param selectorName the selector name
         * @return the index of the node for the named selector, or negative if the selector is not known
         */
        public int getSelectorIndex( String selectorName );

        /**
         * Get the names of the selectors that are associated with these results.
         * 
         * @return the immutable list of selector names; never null
         */
        public List<String> getSelectorNames();

        /**
         * Get the name of the property that corresponds to the named column in each tuple.
         * 
         * @param columnName the column name
         * @return the property name, or the supplied column name if there is no property for it
         */
        public String getPropertyNameForColumnName( String columnName );

        /**
         * Get the name of the selector that produced the column with the given name.
         * 
         * @param columnName the column name
         * @return the selector name
         * @throws NoSuchElementException if the column name is invalid or doesn't match an existing column
         */
        public String getSelectorNameForColumnName( String columnName );

        /**
         * Determine whether these results include full-text search scores.
         * 
         * @return true if the full-text search scores are included in the results, or false otherwise
         */
        public boolean hasFullTextSearchScores();

        /**
         * Return a new Columns that is a superset combination of both this Columns and the supplied Columns.
         * 
         * @param other the other columns; may not be null
         * @return a new Columns instance that is a superset of {@code this} and {@code other}; never null
         */
        public Columns with( Columns other );
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

        @Override
        public int compareTo( Statistics that ) {
            if (that == this) return 0;
            long diff = this.totalTime() - that.totalTime();
            if (diff < 0) return -1;
            if (diff > 0) return 1;
            return 0;
        }

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
                    sb.append(", ");
                }
                sb.append("optim=");
                readable(optimizationNanos, sb);
            }
            if (resultFormulationNanos != 0L) {
                if (first) {
                    first = false;
                    sb.append(" (");
                } else {
                    sb.append(", ");
                }
                sb.append("resultform=");
                readable(resultFormulationNanos, sb);
            }
            if (executionNanos != 0L) {
                if (first) {
                    first = false;
                    sb.append(" (");
                } else {
                    sb.append(", ");
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
