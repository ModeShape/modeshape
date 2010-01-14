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
package org.modeshape.graph.query.parse;

import org.modeshape.common.util.CheckArg;

/**
 * An exception signalling that a query is invalid (but typically well-formed)
 */
public class InvalidQueryException extends RuntimeException {

    /**
     */
    private static final long serialVersionUID = 1L;

    private final String query;

    /**
     * Create an exception with the invalid query.
     * 
     * @param query the query that is invalid
     */
    public InvalidQueryException( String query ) {
        super();
        this.query = query;
    }

    /**
     * Create an exception with the invalid query and a message.
     * 
     * @param query the query that is invalid
     * @param message
     */
    public InvalidQueryException( String query,
                                  String message ) {
        super(message);
        CheckArg.isNotNull(query, "query");
        this.query = query;
    }

    /**
     * Get the XPath query that is invalid.
     * 
     * @return the query; never null
     */
    public String getQuery() {
        return query;
    }

}
