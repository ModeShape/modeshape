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
package org.modeshape.graph.query.model;

import java.util.List;

/**
 * Represents the abstract base class for all query commands. Subclasses include {@link Query} and {@link SetQuery}.
 */
public interface QueryCommand extends Command {
    /**
     * Return the orderings for this query.
     * 
     * @return the list of orderings; never null
     */
    public List<? extends Ordering> orderings();

    /**
     * Get the limits associated with this query.
     * 
     * @return the limits; never null but possibly {@link Limit#isUnlimited() unlimited}
     */
    public Limit limits();

    /**
     * Return the columns defining the query results. If there are no columns, then the columns are implementation determined.
     * 
     * @return the list of columns; never null
     */
    public List<? extends Column> columns();

    /**
     * Create a copy of this query, but one that uses the supplied limit on the number of result rows.
     * 
     * @param rowLimit the limit that should be used; must be a positive number
     * @return the copy of the query that uses the supplied limit; never null
     */
    public QueryCommand withLimit( int rowLimit );

    /**
     * Create a copy of this query, but one that uses the supplied offset.
     * 
     * @param offset the limit that should be used; may not be negative
     * @return the copy of the query that uses the supplied offset; never null
     */
    public QueryCommand withOffset( int offset );
}
