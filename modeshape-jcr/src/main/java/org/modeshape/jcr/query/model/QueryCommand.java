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
package org.modeshape.jcr.query.model;

import java.util.List;

/**
 * Represents the abstract base class for all query commands. Subclasses include {@link Query} and {@link SetQuery}.
 */
public interface QueryCommand extends Command, org.modeshape.jcr.api.query.qom.QueryCommand {
    /**
     * Return the orderings for this query.
     * 
     * @return the list of orderings; never null
     */
    public List<? extends Ordering> orderings();

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
