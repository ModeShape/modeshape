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
package org.modeshape.jcr.query.engine;

import org.modeshape.common.collection.ImmutableProblems;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.collection.SimpleProblems;
import org.modeshape.jcr.cache.CachedNodeSupplier;
import org.modeshape.jcr.query.NodeSequence;
import org.modeshape.jcr.query.model.Column;
import org.modeshape.jcr.query.model.TypeSystem;

/**
 * @author Randall Hauch (rhauch@redhat.com)
 */
public class Results implements org.modeshape.jcr.query.QueryResults {

    public static final Results EMPTY = new Results();

    private static final Problems NO_PROBLEMS = new ImmutableProblems(new SimpleProblems());

    private final Problems problems;
    private final Columns columns;
    private final NodeSequence rows;
    private final Statistics statistics;
    private final String plan;
    private final CachedNodeSupplier cachedNodes;

    /**
     * Create an empty results object.
     */
    private Results() {
        this.problems = NO_PROBLEMS;
        this.columns = IndexQueryEngine.ResultColumns.EMPTY;
        this.statistics = new Statistics();
        this.plan = null;
        this.rows = NodeSequence.emptySequence(0);
        this.cachedNodes = null;
    }

    /**
     * Create a results object for the supplied context, command, and result columns and with the supplied tuples.
     * 
     * @param columns the definition of the query result columns
     * @param statistics the statistics for this query; may not be null
     * @param rows the sequence of rows; may not be null
     * @param cachedNodes the supplier for obtaining cached nodes; may not be null
     * @param problems the problems; may be null if there are no problems
     * @param plan the text representation of the query plan, if the hints asked for it
     */
    public Results( Columns columns,
                    Statistics statistics,
                    NodeSequence rows,
                    CachedNodeSupplier cachedNodes,
                    Problems problems,
                    String plan ) {
        assert columns != null;
        assert statistics != null;
        assert rows != null;
        assert cachedNodes != null;
        this.problems = problems != null ? problems : NO_PROBLEMS;
        this.columns = columns;
        this.statistics = statistics;
        this.plan = plan;
        this.rows = rows;
        this.cachedNodes = cachedNodes;
    }

    @Override
    public CachedNodeSupplier getCachedNodes() {
        return cachedNodes;
    }

    @Override
    public Columns getColumns() {
        return columns;
    }

    @Override
    public NodeSequence getRows() {
        return rows;
    }

    @Override
    public long getRowCount() {
        return rows.getRowCount();
    }

    @Override
    public boolean isEmpty() {
        return rows.isEmpty();
    }

    @Override
    public String getPlan() {
        return plan;
    }

    @Override
    public Problems getProblems() {
        return problems;
    }

    @Override
    public boolean hasErrors() {
        return problems.hasErrors();
    }

    @Override
    public boolean hasWarnings() {
        return problems.hasWarnings();
    }

    @Override
    public Statistics getStatistics() {
        return statistics;
    }

    @Override
    public String toString() {
        return toString(null, Integer.MAX_VALUE);
    }

    /**
     * Get a string representation of this result object, with a maximum number of tuples to include.
     * 
     * @param typeSystem the type system that can be used to convert the values to a string; may be null if
     *        {@link Object#toString()} should be used
     * @param maxTuples the maximum number of tuples to print, or {@link Integer#MAX_VALUE} if all the tuples are to be printed
     * @return the string representation; never null
     */
    public String toString( TypeSystem typeSystem,
                            int maxTuples ) {
        StringBuilder sb = new StringBuilder();
        toString(typeSystem, sb, maxTuples);
        return sb.toString();
    }

    /**
     * Get a string representation of this result object.
     * 
     * @param typeSystem the type system that can be used to convert the values to a string; may be null if
     *        {@link Object#toString()} should be used
     * @param sb the string builder to which the results should be written; may not be null
     */
    public void toString( TypeSystem typeSystem,
                          StringBuilder sb ) {
        toString(typeSystem, sb, Integer.MAX_VALUE);
    }

    /**
     * Get a string representation of this result object, with a maximum number of tuples to include.
     * 
     * @param typeSystem the type system that can be used to convert the values to a string; may be null if
     *        {@link Object#toString()} should be used
     * @param sb the string builder to which the results should be written; may not be null
     * @param maxTuples the maximum number of tuples to print, or {@link Integer#MAX_VALUE} if all the tuples are to be printed
     */
    public void toString( TypeSystem typeSystem,
                          StringBuilder sb,
                          int maxTuples ) {
        sb.append("Result columns: \n");
        for (Column column : columns) {
            sb.append(" ").append(column).append('\n');
        }
    }

}
