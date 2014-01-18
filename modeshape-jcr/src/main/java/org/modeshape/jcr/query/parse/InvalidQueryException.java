/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.jcr.query.parse;

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
     * Get the query that is invalid.
     * 
     * @return the query; never null
     */
    public String getQuery() {
        return query;
    }

}
