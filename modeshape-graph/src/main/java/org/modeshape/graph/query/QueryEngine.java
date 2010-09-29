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

import java.util.List;
import net.jcip.annotations.ThreadSafe;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.query.QueryResults.Statistics;
import org.modeshape.graph.query.model.Column;
import org.modeshape.graph.query.model.Constraint;
import org.modeshape.graph.query.model.QueryCommand;
import org.modeshape.graph.query.optimize.Optimizer;
import org.modeshape.graph.query.optimize.RuleBasedOptimizer;
import org.modeshape.graph.query.plan.CanonicalPlanner;
import org.modeshape.graph.query.plan.PlanHints;
import org.modeshape.graph.query.plan.PlanNode;
import org.modeshape.graph.query.plan.Planner;
import org.modeshape.graph.query.plan.PlanNode.Property;
import org.modeshape.graph.query.plan.PlanNode.Traversal;
import org.modeshape.graph.query.plan.PlanNode.Type;
import org.modeshape.graph.query.process.Processor;
import org.modeshape.graph.query.process.QueryResultColumns;
import org.modeshape.graph.query.validate.Schemata;

/**
 * A query engine that is able to execute formal queries expressed in the Graph API's {@link QueryCommand Abstract Query Model}.
 */
@ThreadSafe
public class QueryEngine implements Queryable {

    private final Planner planner;
    private final Optimizer optimizer;
    private final Processor processor;

    /**
     * Create a new query engine given the {@link Planner planner}, {@link Optimizer optimizer}, {@link Processor processor}, and
     * {@link Schemata schemata}.
     * 
     * @param planner the planner that should be used to generate canonical query plans for the queries; may be null if the
     *        {@link CanonicalPlanner} should be used
     * @param optimizer the optimizer that should be used to optimize the canonical query plan; may be null if the
     *        {@link RuleBasedOptimizer} should be used
     * @param processor the processor implementation that should be used to process the planned query and return the results
     * @throws IllegalArgumentException if the processor reference is null
     */
    public QueryEngine( Planner planner,
                        Optimizer optimizer,
                        Processor processor ) {
        CheckArg.isNotNull(processor, "processor");
        this.planner = planner != null ? planner : new CanonicalPlanner();
        this.optimizer = optimizer != null ? optimizer : new RuleBasedOptimizer();
        this.processor = processor;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.Queryable#execute(org.modeshape.graph.query.QueryContext,
     *      org.modeshape.graph.query.model.QueryCommand)
     */
    public QueryResults execute( QueryContext context,
                                 QueryCommand query ) {
        CheckArg.isNotNull(context, "context");
        CheckArg.isNotNull(query, "query");

        // Create the canonical plan ...
        long start = System.nanoTime();
        PlanNode plan = planner.createPlan(context, query);
        long duration = System.nanoTime() - start;
        Statistics stats = new Statistics(duration);

        QueryResultColumns resultColumns = QueryResultColumns.empty();
        if (!context.getProblems().hasErrors()) {
            // Optimize the plan ...
            start = System.nanoTime();
            PlanNode optimizedPlan = optimizer.optimize(context, plan);
            duration = System.nanoTime() - start;
            stats = stats.withOptimizationTime(duration);

            // Find the query result columns ...
            start = System.nanoTime();
            resultColumns = determineQueryResultColumns(optimizedPlan, context.getHints());
            duration = System.nanoTime() - start;
            stats = stats.withResultsFormulationTime(duration);

            if (!context.getProblems().hasErrors()) {
                // Execute the plan ...
                try {
                    start = System.nanoTime();
                    return processor.execute(context, query, stats, optimizedPlan);
                } finally {
                    duration = System.nanoTime() - start;
                    stats = stats.withExecutionTime(duration);
                }
            }
        }
        // There were problems somewhere ...
        return new org.modeshape.graph.query.process.QueryResults(resultColumns, stats, context.getProblems());
    }

    protected QueryResultColumns determineQueryResultColumns( PlanNode optimizedPlan,
                                                              PlanHints hints ) {
        // Look for which columns to include in the results; this will be defined by the highest PROJECT node ...
        PlanNode project = optimizedPlan.findAtOrBelow(Traversal.LEVEL_ORDER, Type.PROJECT);
        if (project != null) {
            List<Column> columns = project.getPropertyAsList(Property.PROJECT_COLUMNS, Column.class);
            List<String> columnTypes = project.getPropertyAsList(Property.PROJECT_COLUMN_TYPES, String.class);
            // Determine whether to include the full-text search scores in the results ...
            boolean includeFullTextSearchScores = hints.hasFullTextSearch;
            if (!includeFullTextSearchScores) {
                for (PlanNode select : optimizedPlan.findAllAtOrBelow(Type.SELECT)) {
                    Constraint constraint = select.getProperty(Property.SELECT_CRITERIA, Constraint.class);
                    if (QueryResultColumns.includeFullTextScores(constraint)) {
                        includeFullTextSearchScores = true;
                        break;
                    }
                }
            }
            return new QueryResultColumns(columns, columnTypes, includeFullTextSearchScores);
        }
        return QueryResultColumns.empty();
    }

}
