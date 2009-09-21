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
package org.jboss.dna.graph.query;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.query.QueryResults.Statistics;
import org.jboss.dna.graph.query.model.Column;
import org.jboss.dna.graph.query.model.Constraint;
import org.jboss.dna.graph.query.model.FullTextSearch;
import org.jboss.dna.graph.query.model.QueryCommand;
import org.jboss.dna.graph.query.model.SelectorName;
import org.jboss.dna.graph.query.model.Visitors;
import org.jboss.dna.graph.query.optimize.Optimizer;
import org.jboss.dna.graph.query.optimize.RuleBasedOptimizer;
import org.jboss.dna.graph.query.plan.CanonicalPlanner;
import org.jboss.dna.graph.query.plan.PlanHints;
import org.jboss.dna.graph.query.plan.PlanNode;
import org.jboss.dna.graph.query.plan.Planner;
import org.jboss.dna.graph.query.plan.PlanNode.Property;
import org.jboss.dna.graph.query.plan.PlanNode.Traversal;
import org.jboss.dna.graph.query.plan.PlanNode.Type;
import org.jboss.dna.graph.query.process.Processor;
import org.jboss.dna.graph.query.process.QueryResultColumns;
import org.jboss.dna.graph.query.validate.Schemata;

/**
 * A query engine that is able to execute formal queries expressed in the Graph API's {@link QueryCommand Abstract Query Model}.
 */
@ThreadSafe
public class QueryEngine {

    private final Planner planner;
    private final Optimizer optimizer;
    private final Processor processor;
    private final Schemata schemata;

    public QueryEngine( Planner planner,
                        Optimizer optimizer,
                        Processor processor,
                        Schemata schemata ) {
        CheckArg.isNotNull(processor, "processor");
        this.planner = planner != null ? planner : new CanonicalPlanner();
        this.optimizer = optimizer != null ? optimizer : new RuleBasedOptimizer();
        this.processor = processor;
        this.schemata = schemata != null ? schemata : new Schemata() {
            public Table getTable( SelectorName name ) {
                // This won't allow the query engine to do anything (or much of anything),
                // but it is legal and will result in meaningful problems
                return null;
            }
        };
    }

    /**
     * Execute the supplied query by planning, optimizing, and then processing it.
     * 
     * @param context the context in which the query should be executed
     * @param query the query that is to be executed
     * @return the query results; never null
     * @throws IllegalArgumentException if the context or query references are null
     */
    public QueryResults execute( ExecutionContext context,
                                 QueryCommand query ) {
        return execute(context, query, new PlanHints());
    }

    /**
     * Execute the supplied query by planning, optimizing, and then processing it.
     * 
     * @param context the context in which the query should be executed
     * @param query the query that is to be executed
     * @param hints the hints for the execution; may be null if there are no hints
     * @return the query results; never null
     * @throws IllegalArgumentException if the context or query references are null
     */
    public QueryResults execute( ExecutionContext context,
                                 QueryCommand query,
                                 PlanHints hints ) {
        QueryContext queryContext = new QueryContext(context, hints, schemata);

        // Create the canonical plan ...
        long start = System.nanoTime();
        PlanNode plan = planner.createPlan(queryContext, query);
        long duration = System.nanoTime() - start;
        Statistics stats = new Statistics(duration);

        QueryResultColumns resultColumns = QueryResultColumns.empty();
        if (!queryContext.getProblems().hasErrors()) {
            // Optimize the plan ...
            start = System.nanoTime();
            PlanNode optimizedPlan = optimizer.optimize(queryContext, plan);
            duration = System.nanoTime() - start;
            stats = stats.withOptimizationTime(duration);

            // Find the query result columns ...
            start = System.nanoTime();
            resultColumns = determineQueryResultColumns(optimizedPlan);
            duration = System.nanoTime() - start;
            stats = stats.withOptimizationTime(duration);

            if (!queryContext.getProblems().hasErrors()) {
                // Execute the plan ...
                try {
                    start = System.nanoTime();
                    return processor.execute(queryContext, query, stats, optimizedPlan);
                } finally {
                    duration = System.nanoTime() - start;
                    stats = stats.withOptimizationTime(duration);
                }
            }
        }
        // There were problems somewhere ...
        return new org.jboss.dna.graph.query.process.QueryResults(queryContext, query, resultColumns, stats);
    }

    protected QueryResultColumns determineQueryResultColumns( PlanNode optimizedPlan ) {
        // Look for which columns to include in the results; this will be defined by the highest PROJECT node ...
        PlanNode project = optimizedPlan.findAtOrBelow(Traversal.LEVEL_ORDER, Type.PROJECT);
        if (project != null) {
            List<Column> columns = project.getPropertyAsList(Property.PROJECT_COLUMNS, Column.class);
            // Determine whether to include the full-text search scores in the results ...
            final AtomicBoolean includeFullTextSearchScores = new AtomicBoolean(false);
            for (PlanNode select : optimizedPlan.findAllAtOrBelow(Type.SELECT)) {
                Constraint constraint = select.getProperty(Property.SELECT_CRITERIA, Constraint.class);
                if (constraint != null) {
                    Visitors.visitAll(constraint, new Visitors.AbstractVisitor() {
                        @Override
                        public void visit( FullTextSearch obj ) {
                            includeFullTextSearchScores.set(true);
                        }
                    });
                }
                if (includeFullTextSearchScores.get()) break;
            }
            return new QueryResultColumns(columns, includeFullTextSearchScores.get());
        }
        return QueryResultColumns.empty();
    }

}
