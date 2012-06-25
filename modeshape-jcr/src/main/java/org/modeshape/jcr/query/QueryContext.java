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
package org.modeshape.jcr.query;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.collection.SimpleProblems;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.cache.NodeCache;
import org.modeshape.jcr.cache.RepositoryCache;
import org.modeshape.jcr.cache.WorkspaceNotFoundException;
import org.modeshape.jcr.query.model.BindVariableName;
import org.modeshape.jcr.query.model.TypeSystem;
import org.modeshape.jcr.query.plan.PlanHints;
import org.modeshape.jcr.query.validate.Schemata;
import org.modeshape.jcr.value.NamespaceRegistry;

/**
 * An immutable context in which queries are to be executed. Each query context defines the information that is available during
 * query execution.
 * <p>
 * The only mutable state on this context is whether the query has been {@link #cancel() cancelled}.
 * </p>
 */
@ThreadSafe
public class QueryContext {
    private final ExecutionContext context;
    private final RepositoryCache repositoryCache;
    private final TypeSystem typeSystem;
    private final PlanHints hints;
    private final Schemata schemata;
    private final Problems problems;
    private final Map<String, Object> variables;
    private final Set<String> workspaceNames;
    private final Map<String, NodeCache> overriddenNodeCachesByWorkspaceName;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    /**
     * Create a new context for query execution.
     * 
     * @param context the context in which the query is being executed; may not be null
     * @param repositoryCache the repository cache that should be used to load results; may be null if no results are to be loaded
     * @param workspaceNames the name of each workspace to be queried, or an empty set if all the workspaces should be queried;
     *        may not be null
     * @param overriddenNodeCachesByWorkspaceName the NodeCache instances that should be used to load results, which will be used
     *        instead of the RepositoryCache's NodeCache for a given workspace name; may be null or empty
     * @param schemata the schemata
     * @param hints the hints, or null if there are no hints
     * @param problems the problems container, or null if a new problems container should be created
     * @param variables the mapping of variables and values, or null if there are no such variables
     * @throws IllegalArgumentException if the context, workspace name, or schemata are null
     */
    public QueryContext( ExecutionContext context,
                         RepositoryCache repositoryCache,
                         Set<String> workspaceNames,
                         Map<String, NodeCache> overriddenNodeCachesByWorkspaceName,
                         Schemata schemata,
                         PlanHints hints,
                         Problems problems,
                         Map<String, Object> variables ) {
        CheckArg.isNotNull(context, "context");
        CheckArg.isNotNull(workspaceNames, "workspaceNames");
        CheckArg.isNotNull(schemata, "schemata");
        this.context = context;
        this.repositoryCache = repositoryCache;
        this.workspaceNames = workspaceNames;
        this.typeSystem = context.getValueFactories().getTypeSystem();
        this.hints = hints != null ? hints : new PlanHints();
        this.schemata = schemata;
        this.problems = problems != null ? problems : new SimpleProblems();
        this.variables = variables != null ? new HashMap<String, Object>(variables) : new HashMap<String, Object>();
        this.overriddenNodeCachesByWorkspaceName = overriddenNodeCachesByWorkspaceName != null ? overriddenNodeCachesByWorkspaceName : Collections.<String, NodeCache>emptyMap();
        assert this.typeSystem != null;
        assert this.hints != null;
        assert this.schemata != null;
        assert this.problems != null;
        assert this.variables != null;
        assert this.workspaceNames != null;
    }

    /**
     * Create a new context for query execution.
     * 
     * @param context the context in which the query is being executed; may not be null
     * @param repositoryCache the repository cache that should be used to load results; may not be null
     * @param workspaceNames the name of each workspace to be queried, or an empty set if all the workspaces should be queried;
     *        may not be null
     * @param schemata the schemata
     * @param hints the hints, or null if there are no hints
     * @param problems the problems container, or null if a new problems container should be created
     * @throws IllegalArgumentException if the context, workspace name, or schemata are null
     */
    public QueryContext( ExecutionContext context,
                         RepositoryCache repositoryCache,
                         Set<String> workspaceNames,
                         Schemata schemata,
                         PlanHints hints,
                         Problems problems ) {
        this(context, repositoryCache, workspaceNames, null, schemata, hints, problems, null);
    }

    /**
     * Create a new context for query execution.
     * 
     * @param context the context in which the query is being executed; may not be null
     * @param repositoryCache the repository cache that should be used to load results; may not be null
     * @param workspaceNames the name of each workspace to be queried, or an empty set if all the workspaces should be queried;
     *        may not be null
     * @param schemata the schemata
     * @throws IllegalArgumentException if the context, workspace name, or schemata are null
     */
    public QueryContext( ExecutionContext context,
                         RepositoryCache repositoryCache,
                         Set<String> workspaceNames,
                         Schemata schemata ) {
        this(context, repositoryCache, workspaceNames, null, schemata, null, null, null);
    }

    /**
     * Create a new context that is a copy of the supplied context. This constructor is useful for subclasses that wish to add
     * store additional fields in a QueryContext.
     * 
     * @param original the original context
     * @throws IllegalArgumentException if the original is null
     */
    protected QueryContext( QueryContext original ) {
        this(original.context, original.repositoryCache, original.workspaceNames, original.overriddenNodeCachesByWorkspaceName,
             original.schemata, original.hints, original.problems, original.variables);
    }

    /**
     * Cancel the query if it is currently running. Note that this method does not block until the query is cancelled; it merely
     * marks the query as being cancelled, and the query will terminate at its next available opportunity.
     * 
     * @return true if the query was executing and will be cancelled, or false if the query was no longer running (because it had
     *         finished successfully or had already been cancelled) and could not be cancelled.
     */
    public final boolean cancel() {
        return this.cancelled.compareAndSet(false, true);
    }

    public final boolean isCancelled() {
        return this.cancelled.get();
    }

    /**
     * Get the NodeCache for the given workspace name. The result will either be the overridden value supplied in the constructor
     * or the workspace cache from the referenced RepositoryCache.
     * 
     * @param workspaceName the name of the workspace
     * @return the node cache; never null
     * @throws WorkspaceNotFoundException if there is no workspace with the supplied name
     */
    public NodeCache getNodeCache( String workspaceName ) throws WorkspaceNotFoundException {
        NodeCache cache = overriddenNodeCachesByWorkspaceName.get(workspaceName);
        if (cache == null) {
            cache = repositoryCache.getWorkspaceCache(workspaceName);
        }
        return cache;
    }

    /**
     * Get the namespace registry with durable prefixes as used by the query indexes.
     * 
     * @return the durable namespace registry; never null
     */
    public NamespaceRegistry getNamespaceRegistry() {
        // TODO: Durable namespace registry
        return context.getNamespaceRegistry();
    }

    /**
     * Get the RepositoryCache instance that should be used to load the result tuple values.
     * 
     * @return the node cache
     */
    public RepositoryCache getRepositoryCache() {
        return repositoryCache;
    }

    /**
     * Get the execution context in which the query is to be evaluated
     * 
     * @return the execution context; never null
     */
    public ExecutionContext getExecutionContext() {
        return context;
    }

    /**
     * Get the names of each workspace that is to be queried.
     * 
     * @return the workspace name; never null
     */
    public Set<String> getWorkspaceNames() {
        return workspaceNames;
    }

    /**
     * Get the interface for working with literal values and types.
     * 
     * @return the type system; never null
     */
    public TypeSystem getTypeSystem() {
        return typeSystem;
    }

    /**
     * Get the plan hints.
     * 
     * @return the plan hints; never null
     */
    public final PlanHints getHints() {
        return hints;
    }

    /**
     * Get the problem container used by this query context. Any problems that have been encountered will be accumlated in this
     * container.
     * 
     * @return the problem container; never null
     */
    public final Problems getProblems() {
        return problems;
    }

    /**
     * Get the definition of the tables available within this query context.
     * 
     * @return the schemata; never null
     */
    public Schemata getSchemata() {
        return schemata;
    }

    /**
     * Get the variables that are to be substituted into the {@link BindVariableName} used in the query.
     * 
     * @return immutable map of variable values keyed by their name; never null but possibly empty
     */
    public Map<String, Object> getVariables() {
        return variables;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof QueryContext) {
            QueryContext that = (QueryContext)obj;
            if (!this.typeSystem.equals(that.getTypeSystem())) return false;
            if (!this.schemata.equals(that.getSchemata())) return false;
            if (!this.variables.equals(that.getVariables())) return false;
            return true;
        }
        return false;
    }

    /**
     * Obtain a copy of this context, except that the copy uses the supplied schemata.
     * 
     * @param schemata the schemata that should be used in the new context
     * @return the new context; never null
     * @throws IllegalArgumentException if the schemata reference is null
     */
    public QueryContext with( Schemata schemata ) {
        CheckArg.isNotNull(schemata, "schemata");
        return new QueryContext(context, repositoryCache, workspaceNames, overriddenNodeCachesByWorkspaceName, schemata, hints,
                                problems, variables);
    }

    /**
     * Obtain a copy of this context, except that the copy uses the supplied hints.
     * 
     * @param hints the hints that should be used in the new context
     * @return the new context; never null
     * @throws IllegalArgumentException if the hints reference is null
     */
    public QueryContext with( PlanHints hints ) {
        CheckArg.isNotNull(hints, "hints");
        return new QueryContext(context, repositoryCache, workspaceNames, overriddenNodeCachesByWorkspaceName, schemata, hints,
                                problems, variables);
    }

    /**
     * Obtain a copy of this context, except that the copy uses the supplied problem container.
     * 
     * @param problems the problems that should be used in the new context; may be null if a new problem container should be used
     * @return the new context; never null
     */
    public QueryContext with( Problems problems ) {
        return new QueryContext(context, repositoryCache, workspaceNames, overriddenNodeCachesByWorkspaceName, schemata, hints,
                                problems, variables);
    }

    /**
     * Obtain a copy of this context, except that the copy uses the supplied variables.
     * 
     * @param variables the variables that should be used in the new context; may be null if there are no such variables
     * @return the new context; never null
     */
    public QueryContext with( Map<String, Object> variables ) {
        return new QueryContext(context, repositoryCache, workspaceNames, overriddenNodeCachesByWorkspaceName, schemata, hints,
                                problems, variables);
    }

}
