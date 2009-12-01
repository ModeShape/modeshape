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
package org.jboss.dna.graph.request;

import java.util.Collections;
import java.util.Map;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.query.model.QueryCommand;
import org.jboss.dna.graph.query.model.Visitors;
import org.jboss.dna.graph.query.plan.PlanHints;
import org.jboss.dna.graph.query.validate.Schemata;

/**
 * A {@link Request} to query a graph.
 */
public class QueryRequest extends SearchRequest {

    private static final Map<String, Object> EMPTY_VARIABLES = Collections.emptyMap();

    private static final long serialVersionUID = 1L;

    private final QueryCommand query;
    private final String workspaceName;
    private final Map<String, Object> variables;
    private final PlanHints hints;
    private final transient Schemata schemata;

    /**
     * Create a new request to execute the supplied query against the name workspace.
     * 
     * @param query the query to be executed
     * @param workspace the name of the workspace to be queried
     * @throws IllegalArgumentException if the query or workspace name is null
     */
    public QueryRequest( QueryCommand query,
                         String workspace ) {
        CheckArg.isNotNull(query, "query");
        CheckArg.isNotNull(workspace, "workspace");
        this.query = query;
        this.workspaceName = workspace;
        this.variables = EMPTY_VARIABLES;
        this.hints = null;
        this.schemata = null;
    }

    /**
     * Create a new request to execute the supplied query against the name workspace.
     * 
     * @param query the query to be executed
     * @param workspace the name of the workspace to be queried
     * @param variables the variables that are available to be substituted upon execution; may be null if there are no variables
     * @param hints the hints; may be null if there are no hints
     * @param schemata the schemata defining the structure of the tables that may be queried, or null if the default schemata
     *        should be used
     * @throws IllegalArgumentException if the query or workspace name is null
     */
    public QueryRequest( QueryCommand query,
                         String workspace,
                         Map<String, Object> variables,
                         PlanHints hints,
                         Schemata schemata ) {
        CheckArg.isNotNull(query, "query");
        CheckArg.isNotNull(workspace, "workspace");
        this.query = query;
        this.workspaceName = workspace;
        this.variables = variables != null ? variables : EMPTY_VARIABLES;
        this.hints = hints;
        this.schemata = schemata;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.Request#isReadOnly()
     */
    @Override
    public boolean isReadOnly() {
        return true;
    }

    /**
     * Get the query that is to be executed.
     * 
     * @return the query; never null
     */
    public QueryCommand query() {
        return query;
    }

    /**
     * Get the name of the workspace in which the node exists.
     * 
     * @return the name of the workspace; never null
     */
    public String workspace() {
        return workspaceName;
    }

    /**
     * The variables that are available to be substituted upon execution.
     * 
     * @return the variables; never null but possibly empty
     */
    public Map<String, Object> variables() {
        return variables;
    }

    /**
     * Get the hints for the query.
     * 
     * @return the hints, or null if there are no hints
     */
    public PlanHints hints() {
        return hints;
    }

    /**
     * Get the schemata that defines the structure of the tables that may be queried.
     * 
     * @return the schemata, or null if the default schemata should be used
     */
    public Schemata schemata() {
        return schemata;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return query.hashCode();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (this.getClass().isInstance(obj)) {
            QueryRequest that = (QueryRequest)obj;
            if (!this.query().equals(that.query())) return false;
            if (!this.workspace().equals(that.workspace())) return false;
            if (!this.variables().equals(that.variables())) return false;
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "query the \"" + workspaceName + "\" workspace with \"" + Visitors.readable(query) + "\"";
    }
}
