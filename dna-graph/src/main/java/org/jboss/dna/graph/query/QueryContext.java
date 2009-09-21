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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.jboss.dna.common.collection.Problems;
import org.jboss.dna.common.collection.SimpleProblems;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.query.model.BindVariableName;
import org.jboss.dna.graph.query.plan.PlanHints;
import org.jboss.dna.graph.query.validate.Schemata;

/**
 * 
 */
public final class QueryContext {
    private final ExecutionContext context;
    private final PlanHints hints;
    private final Schemata schemata;
    private final Problems problems;
    private final Map<String, Object> variables;

    public QueryContext( ExecutionContext context,
                         PlanHints hints,
                         Schemata schemata,
                         Problems problems,
                         Map<String, Object> variables ) {
        CheckArg.isNotNull(context, "context");
        this.context = context;
        this.hints = hints != null ? hints : new PlanHints();
        this.schemata = schemata;
        this.problems = problems != null ? problems : new SimpleProblems();
        this.variables = variables != null ? Collections.<String, Object>unmodifiableMap(new HashMap<String, Object>(variables)) : Collections.<String, Object>emptyMap();
    }

    public QueryContext( ExecutionContext context,
                         PlanHints hints,
                         Schemata schemata,
                         Problems problems ) {
        this(context, hints, schemata, problems, null);
    }

    public QueryContext( ExecutionContext context,
                         PlanHints hints,
                         Schemata schemata ) {
        this(context, hints, schemata, null, null);
    }

    /**
     * @return context
     */
    public final ExecutionContext getExecutionContext() {
        return context;
    }

    /**
     * @return hints
     */
    public final PlanHints getHints() {
        return hints;
    }

    /**
     * @return problems
     */
    public final Problems getProblems() {
        return problems;
    }

    /**
     * @return schemata
     */
    public Schemata getSchemata() {
        return schemata;
    }

    /**
     * Get the variables that are to be substituted into the {@link BindVariableName} used in the query.
     * 
     * @return immutable map of variable values keyed by their name; never null
     */
    public Map<String, Object> getVariables() {
        return variables;
    }

}
