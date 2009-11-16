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
import org.jboss.dna.graph.query.QueryResults.Statistics;
import org.jboss.dna.graph.query.model.Column;
import org.jboss.dna.graph.query.model.Constraint;
import org.jboss.dna.graph.query.model.FullTextSearch;
import org.jboss.dna.graph.query.model.QueryCommand;
import org.jboss.dna.graph.query.model.Visitors;
import org.jboss.dna.graph.query.optimize.Optimizer;
import org.jboss.dna.graph.query.optimize.RuleBasedOptimizer;
import org.jboss.dna.graph.query.plan.CanonicalPlanner;
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
     * @see org.jboss.dna.graph.query.Queryable#execute(org.jboss.dna.graph.query.QueryContext,
     *      org.jboss.dna.graph.query.model.QueryCommand)
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
            resultColumns = determineQueryResultColumns(optimizedPlan);
            duration = System.nanoTime() - start;
            stats = stats.withOptimizationTime(duration);

            if (!context.getProblems().hasErrors()) {
                // Execute the plan ...
                try {
                    start = System.nanoTime();
                    return processor.execute(context, query, stats, optimizedPlan);
                } finally {
                    duration = System.nanoTime() - start;
                    stats = stats.withOptimizationTime(duration);
                }
            }
        }
        // There were problems somewhere ...
        return new org.jboss.dna.graph.query.process.QueryResults(context, query, resultColumns, stats);
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
