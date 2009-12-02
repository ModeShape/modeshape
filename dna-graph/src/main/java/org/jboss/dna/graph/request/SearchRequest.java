/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in JBoss DNA is licensed
 * to you under the terms of the GNU Lesser General Public License as
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

import org.jboss.dna.graph.query.QueryResults;

/**
 * A {@link Request} to search or query a graph.
 */
public abstract class SearchRequest extends Request {

    private static final long serialVersionUID = 1L;

    private QueryResults results;

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.Request#isReadOnly()
     */
    @Override
    public final boolean isReadOnly() {
        return true;
    }

    /**
     * Set the results for this request.
     * 
     * @param results the results
     */
    public void setResults( QueryResults results ) {
        this.results = results;
    }

    /**
     * Get the results of this query.
     * 
     * @return the results of the query, or null if this request has not been processed
     */
    public QueryResults getResults() {
        return results;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.Request#cancel()
     */
    @Override
    public void cancel() {
        super.cancel();
        this.results = null;
    }

}
