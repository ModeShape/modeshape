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

import org.jboss.dna.common.util.CheckArg;

/**
 * A {@link Request} to perform a full-text search on a graph.
 */
public class FullTextSearchRequest extends SearchRequest {

    private static final long serialVersionUID = 1L;

    private final String expression;
    private final String workspaceName;

    /**
     * Create a new request to execute the supplied query against the name workspace.
     * 
     * @param fullTextSearch the full-text search to be performed; may not be null
     * @param workspace the name of the workspace to be queried
     * @throws IllegalArgumentException if the query or workspace name is null
     */
    public FullTextSearchRequest( String fullTextSearch,
                                  String workspace ) {
        CheckArg.isNotEmpty(fullTextSearch, "fullTextSearch");
        CheckArg.isNotNull(workspace, "workspace");
        this.expression = fullTextSearch;
        this.workspaceName = workspace;
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
     * Get the full-text search expression that is to be executed.
     * 
     * @return the full-text search expression; never null and never empty
     */
    public String expression() {
        return expression;
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
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return expression.hashCode();
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
            FullTextSearchRequest that = (FullTextSearchRequest)obj;
            if (!this.expression().equals(that.expression())) return false;
            if (!this.workspace().equals(that.workspace())) return false;
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
        return "search the \"" + workspaceName + "\" workspace with \"" + expression + "\"";
    }
}
