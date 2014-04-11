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

import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.query.optimize.Optimizer;
import org.modeshape.jcr.query.optimize.RuleBasedOptimizer;
import org.modeshape.jcr.query.plan.CanonicalPlanner;
import org.modeshape.jcr.query.plan.Planner;
import org.modeshape.jcr.spi.index.IndexManager;

/**
 * @author Randall Hauch (rhauch@redhat.com)
 */
public abstract class QueryEngineBuilder {

    private RepositoryConfiguration config;
    private IndexManager indexManager;
    private ExecutionContext context;
    private Planner planner;
    private Optimizer optimizer;

    public QueryEngineBuilder() {
    }

    public QueryEngineBuilder using( RepositoryConfiguration configuration,
                                     IndexManager indexManager,
                                     ExecutionContext context ) {
        this.config = configuration;
        this.context = context;
        this.indexManager = indexManager;
        return this;
    }

    public QueryEngineBuilder with( Planner planner ) {
        this.planner = planner;
        return this;
    }

    public QueryEngineBuilder with( Optimizer optimizer ) {
        this.optimizer = optimizer;
        return this;
    }

    public abstract QueryEngine build();

    protected final RepositoryConfiguration config() {
        return config;
    }

    protected final ExecutionContext context() {
        return context;
    }

    protected final IndexManager indexManager() {
        return indexManager;
    }

    protected String repositoryName() {
        return config().getName();
    }

    protected final Planner planner() {
        return this.planner != null ? this.planner : defaultPlanner();
    }

    protected final Optimizer optimizer() {
        return this.optimizer != null ? this.optimizer : defaultOptimizer();
    }

    protected Planner defaultPlanner() {
        return new CanonicalPlanner();
    }

    protected Optimizer defaultOptimizer() {
        return new RuleBasedOptimizer();
    }

}
