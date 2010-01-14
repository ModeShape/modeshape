/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in ModeShape is licensed
 * to you under the terms of the GNU Lesser General Public License as
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

import java.util.List;
import org.modeshape.graph.query.QueryResults.Columns;
import org.modeshape.graph.query.QueryResults.Statistics;

/**
 * A {@link Request} to search or query a graph.
 */
public abstract class SearchRequest extends Request {

    private static final long serialVersionUID = 1L;

    private Columns columns;
    private List<Object[]> tuples;
    private Statistics statistics;

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.Request#isReadOnly()
     */
    @Override
    public final boolean isReadOnly() {
        return true;
    }

    /**
     * Set the results for this request.
     * 
     * @param resultColumns the definition of the result columns
     * @param tuples the result values
     * @param statistics the statistics, or null if there are none
     */
    protected void doSetResults( Columns resultColumns,
                                 List<Object[]> tuples,
                                 Statistics statistics ) {
        this.columns = resultColumns;
        this.tuples = tuples;
        this.statistics = statistics;
    }

    /**
     * Get the specification of the columns for the results.
     * 
     * @return the column specifications; never null
     */
    protected Columns columns() {
        return columns;
    }

    /**
     * Get the results of this query.
     * 
     * @return the results of the query, or null if this request has not been processed
     */
    public List<Object[]> getTuples() {
        return tuples;
    }

    /**
     * Get the statistics that describe the time metrics for this query.
     * 
     * @return the statistics; may be null if there are no statistics
     */
    public Statistics getStatistics() {
        return statistics;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.Request#cancel()
     */
    @Override
    public void cancel() {
        super.cancel();
        this.tuples = null;
        this.statistics = null;
    }

}
