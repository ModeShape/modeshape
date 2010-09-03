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

import java.util.HashMap;
import java.util.Map;
import net.jcip.annotations.Immutable;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.collection.SimpleProblems;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.query.model.BindVariableName;
import org.modeshape.graph.query.model.TypeSystem;
import org.modeshape.graph.query.plan.PlanHints;
import org.modeshape.graph.query.validate.Schemata;

/**
 * An immutable context in which queries are to be executed. Each query context defines the information that is available during
 * query execution.
 */
@Immutable
public class QueryContext {
    private final TypeSystem typeSystem;
    private final PlanHints hints;
    private final Schemata schemata;
    private final Problems problems;
    private final Map<String, Object> variables;

    /**
     * Create a new context for query execution.
     * 
     * @param schemata the schemata
     * @param typeSystem the types system
     * @param hints the hints, or null if there are no hints
     * @param problems the problems container, or null if a new problems container should be created
     * @param variables the mapping of variables and values, or null if there are no such variables
     * @throws IllegalArgumentException if the values or schmata are null
     */
    public QueryContext( Schemata schemata,
                         TypeSystem typeSystem,
                         PlanHints hints,
                         Problems problems,
                         Map<String, Object> variables ) {
        CheckArg.isNotNull(typeSystem, "typeSystem");
        CheckArg.isNotNull(schemata, "schemata");
        this.typeSystem = typeSystem;
        this.hints = hints != null ? hints : new PlanHints();
        this.schemata = schemata;
        this.problems = problems != null ? problems : new SimpleProblems();
        this.variables = variables != null ? new HashMap<String, Object>(variables) : new HashMap<String, Object>();
        assert this.typeSystem != null;
        assert this.hints != null;
        assert this.schemata != null;
        assert this.problems != null;
        assert this.variables != null;
    }

    /**
     * Create a new context for query execution.
     * 
     * @param schemata the schemata
     * @param typeSystem the types system
     * @param hints the hints, or null if there are no hints
     * @param problems the problems container, or null if a new problems container should be created
     * @throws IllegalArgumentException if the values or schmata are null
     */
    public QueryContext( Schemata schemata,
                         TypeSystem typeSystem,
                         PlanHints hints,
                         Problems problems ) {
        this(schemata, typeSystem, hints, problems, null);
    }

    /**
     * Create a new context for query execution.
     * 
     * @param schemata the schemata
     * @param typeSystem the types system
     * @param hints the hints, or null if there are no hints
     * @throws IllegalArgumentException if the context or schmata are null
     */
    public QueryContext( Schemata schemata,
                         TypeSystem typeSystem,
                         PlanHints hints ) {
        this(schemata, typeSystem, hints, null, null);
    }

    /**
     * Create a new context for query execution.
     * 
     * @param schemata the schemata
     * @param typeSystem the types system
     * @throws IllegalArgumentException if the values or schmata are null
     */
    public QueryContext( Schemata schemata,
                         TypeSystem typeSystem ) {
        this(schemata, typeSystem, null, null, null);
    }

    /**
     * Create a new context that is a copy of the supplied context. This constructor is useful for subclasses that wish to add
     * store additional fields in a QueryContext.
     * 
     * @param original the original context
     * @throws IllegalArgumentException if the original is null
     */
    protected QueryContext( QueryContext original ) {
        this(original.schemata, original.typeSystem, original.hints, original.problems, original.variables);
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
     * Obtain a copy of this context, except that the copy uses the supplied type system.
     * 
     * @param typeSystem the type system that should be used in the new query context
     * @return the new context; never null
     * @throws IllegalArgumentException if the execution context reference is null
     */
    public QueryContext with( TypeSystem typeSystem ) {
        CheckArg.isNotNull(typeSystem, "typeSystem");
        return new QueryContext(schemata, typeSystem, hints, problems, variables);
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
        return new QueryContext(schemata, typeSystem, hints, problems, variables);
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
        return new QueryContext(schemata, typeSystem, hints, problems, variables);
    }

    /**
     * Obtain a copy of this context, except that the copy uses the supplied problem container.
     * 
     * @param problems the problems that should be used in the new context; may be null if a new problem container should be used
     * @return the new context; never null
     */
    public QueryContext with( Problems problems ) {
        return new QueryContext(schemata, typeSystem, hints, problems, variables);
    }

    /**
     * Obtain a copy of this context, except that the copy uses the supplied variables.
     * 
     * @param variables the variables that should be used in the new context; may be null if there are no such variables
     * @return the new context; never null
     */
    public QueryContext with( Map<String, Object> variables ) {
        return new QueryContext(schemata, typeSystem, hints, problems, variables);
    }

}
