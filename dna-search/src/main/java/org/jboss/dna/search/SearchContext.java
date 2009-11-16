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
package org.jboss.dna.search;

import java.util.Map;
import org.jboss.dna.common.collection.Problems;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.query.QueryContext;
import org.jboss.dna.graph.query.plan.PlanHints;
import org.jboss.dna.graph.query.validate.Schemata;

/**
 * 
 */
class SearchContext extends QueryContext {

    private final IndexContext indexes;

    /**
     * Create a new context for searching and querying.
     * 
     * @param indexes the indexes that should be used
     * @param schemata the definition of the tables available to this query
     */
    public SearchContext( IndexContext indexes,
                          Schemata schemata ) {
        super(indexes.context(), schemata);
        this.indexes = indexes;
        assert this.indexes != null;
    }

    /**
     * Create a new context for searching and querying.
     * 
     * @param queryContext
     * @param indexes
     */
    public SearchContext( QueryContext queryContext,
                          IndexContext indexes ) {
        super(queryContext.getExecutionContext(), queryContext.getSchemata(), queryContext.getHints(),
              queryContext.getProblems(), queryContext.getVariables());
        this.indexes = indexes;
        assert this.indexes != null;
    }

    /**
     * Create a new context for searching and querying.
     * 
     * @param context the execution context
     * @param schemata the schemata
     * @param hints the hints, or null if there are no hints
     * @param problems the problems container, or null if a new problems container should be created
     * @param variables the mapping of variables and values, or null if there are no such variables
     * @throws IllegalArgumentException if the context or schmata are null
     */
    public SearchContext( IndexContext context,
                          Schemata schemata,
                          PlanHints hints,
                          Problems problems,
                          Map<String, Object> variables ) {
        super(context.context(), schemata, hints, problems, variables);
        this.indexes = context;
        assert this.indexes != null;
    }

    /**
     * Get the {@link IndexContext} for this query context.
     * 
     * @return the index context; never null
     */
    public IndexContext getIndexes() {
        return indexes;
    }

    /**
     * Obtain a copy of this context, except that the copy uses the supplied index context.
     * 
     * @param context the index context that should be used in the new query context
     * @return the new context; never null
     * @throws IllegalArgumentException if the index context reference is null
     */
    public SearchContext with( IndexContext context ) {
        CheckArg.isNotNull(context, "context");
        return new SearchContext(context, getSchemata(), getHints(), getProblems(), getVariables());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.QueryContext#with(org.jboss.dna.graph.ExecutionContext)
     */
    @Override
    public SearchContext with( ExecutionContext context ) {
        CheckArg.isNotNull(context, "context");
        return new SearchContext(indexes.with(context), getSchemata(), getHints(), getProblems(), getVariables());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.QueryContext#with(org.jboss.dna.graph.query.validate.Schemata)
     */
    @Override
    public SearchContext with( Schemata schemata ) {
        CheckArg.isNotNull(schemata, "schemata");
        return new SearchContext(indexes, schemata, getHints(), getProblems(), getVariables());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.QueryContext#with(org.jboss.dna.graph.query.plan.PlanHints)
     */
    @Override
    public SearchContext with( PlanHints hints ) {
        CheckArg.isNotNull(hints, "hints");
        return new SearchContext(indexes, getSchemata(), hints, getProblems(), getVariables());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.QueryContext#with(org.jboss.dna.common.collection.Problems)
     */
    @Override
    public SearchContext with( Problems problems ) {
        return new SearchContext(indexes, getSchemata(), getHints(), problems, getVariables());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.QueryContext#with(java.util.Map)
     */
    @Override
    public SearchContext with( Map<String, Object> variables ) {
        return new SearchContext(indexes, getSchemata(), getHints(), getProblems(), variables);
    }
}
