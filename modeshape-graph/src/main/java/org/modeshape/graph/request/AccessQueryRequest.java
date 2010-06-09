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
package org.modeshape.graph.request;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.HashCode;
import org.modeshape.graph.query.QueryResults.Columns;
import org.modeshape.graph.query.QueryResults.Statistics;
import org.modeshape.graph.query.model.Column;
import org.modeshape.graph.query.model.Constraint;
import org.modeshape.graph.query.model.Limit;
import org.modeshape.graph.query.model.SelectorName;
import org.modeshape.graph.query.model.Visitors;
import org.modeshape.graph.query.validate.Schemata;

/**
 * A {@link Request} to issue an access query a graph, where an access query is a low-level atomic query that is part of a large,
 * planned query.
 */
public class AccessQueryRequest extends SearchRequest {

    private static final Map<String, Object> EMPTY_VARIABLES = Collections.emptyMap();

    private static final long serialVersionUID = 1L;

    private final String workspaceName;
    private final SelectorName tableName;
    private final List<Constraint> andedConstraints;
    private final Limit limit;
    private final Map<String, Object> variables;
    private final Schemata schemata;
    private final int hc;

    /**
     * Create a new request to execute the supplied query against the name workspace.
     * 
     * @param workspace the name of the workspace to be queried
     * @param tableName the name of the selector (or table) being queried
     * @param resultColumns the specification of the expected columns in the result tuples
     * @param andedConstraints the list of AND-ed constraints; may be empty or null if there are no constraints
     * @param limit the limit on the results; may be null if there is no limit
     * @param schemata the schemata that defines the table and columns being queried; may not be null
     * @param variables the variables that are available to be substituted upon execution; may be null if there are no variables
     * @throws IllegalArgumentException if the query or workspace name is null
     */
    public AccessQueryRequest( String workspace,
                               SelectorName tableName,
                               Columns resultColumns,
                               List<Constraint> andedConstraints,
                               Limit limit,
                               Schemata schemata,
                               Map<String, Object> variables ) {
        CheckArg.isNotNull(workspace, "workspace");
        CheckArg.isNotNull(tableName, "tableName");
        CheckArg.isNotNull(resultColumns, "resultColumns");
        this.workspaceName = workspace;
        this.tableName = tableName;
        this.andedConstraints = andedConstraints != null ? andedConstraints : Collections.<Constraint>emptyList();
        this.variables = variables != null ? variables : EMPTY_VARIABLES;
        this.limit = limit != null ? limit : Limit.NONE;
        this.schemata = schemata;
        this.doSetResults(resultColumns, null, null);
        this.hc = HashCode.compute(workspaceName, tableName, resultColumns);
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
     * Get the name of the selector (or table) that is being queried.
     * 
     * @return the selector name; never null
     */
    public SelectorName selectorName() {
        return tableName;
    }

    /**
     * Get the specification of the columns for the {@link #getTuples() results}.
     * 
     * @return the column specifications; never null
     */
    public Columns resultColumns() {
        return super.columns();
    }

    /**
     * Get the immutable list of constraints that are AND-ed together in this query. Every {@link #getTuples() tuple in the
     * results} must satisfy <i>all</i> of these constraints.
     * 
     * @return the AND-ed constraints; never null but possibly empty if there are no constraints
     */
    public List<Constraint> andedConstraints() {
        return andedConstraints;
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
     * Get the schemata that defines the table structure and columns definitions available to this query.
     * 
     * @return the schemata; never null
     */
    public Schemata schemata() {
        return schemata;
    }

    /**
     * Get the limit of the result tuples, which can specify a {@link Limit#rowLimit() maximum number of rows} as well as an
     * {@link Limit#offset() initial offset} for the first row.
     * 
     * @return the limit; never null but may be {@link Limit#isUnlimited() unlimited} if there is no effective limit
     */
    public Limit limit() {
        return limit;
    }

    /**
     * Set the results for this request.
     * 
     * @param tuples the result values
     * @param statistics the statistics, or null if there are none
     */
    public void setResults( List<Object[]> tuples,
                            Statistics statistics ) {
        super.doSetResults(columns(), tuples, statistics);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return hc;
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
            AccessQueryRequest that = (AccessQueryRequest)obj;
            if (this.hashCode() != that.hashCode()) return false;
            if (!this.workspace().equals(that.workspace())) return false;
            if (!this.selectorName().equals(that.selectorName())) return false;
            if (!this.limit().equals(that.limit())) return false;
            if (!this.andedConstraints().equals(that.andedConstraints())) return false;
            if (!this.resultColumns().equals(that.resultColumns())) return false;
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
        StringBuilder sb = new StringBuilder("query the \"");
        sb.append(workspaceName).append("\" workspace: SELECT ");
        boolean first = true;
        for (Column column : resultColumns().getColumns()) {
            if (first) first = false;
            else sb.append(", ");
            sb.append(column);
        }
        sb.append(" FROM ").append(selectorName().name());
        if (!andedConstraints.isEmpty()) {
            sb.append(" WHERE ");
            first = true;
            for (Constraint constraint : andedConstraints) {
                if (first) first = false;
                else sb.append(" AND ");
                sb.append(Visitors.readable(constraint));
            }
        }
        if (!limit.isUnlimited()) {
            sb.append(Visitors.readable(limit));
        }
        if (!variables.isEmpty()) {
            sb.append(" USING <");
            first = true;
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                if (first) first = false;
                else sb.append(", ");
                sb.append(entry.getKey()).append('=');
                Object value = entry.getValue();
                if (value instanceof String) {
                    sb.append('"').append(value).append('"');
                } else {
                    sb.append(value);
                }
            }
            sb.append('>');
        }
        return sb.toString();
    }

    @Override
    public RequestType getType() {
        return RequestType.ACCESS_QUERY;
    }
}
