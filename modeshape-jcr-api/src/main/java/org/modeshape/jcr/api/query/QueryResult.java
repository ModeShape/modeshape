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
package org.modeshape.jcr.api.query;

import java.util.Collection;
import javax.jcr.PropertyType;
import javax.jcr.Session;

/**
 * Replicates some of the methods introduced in JCR 2.0, but also provides an extension that allows accessing the JCR
 * {@link PropertyType} for each of the columns.
 */
public interface QueryResult extends javax.jcr.query.QueryResult, AutoCloseable {

    /**
     * Return whether the number of rows in the results is 0. This is often significantly more efficient and more accurate than
     * {@link javax.jcr.RangeIterator#getSize() getting the size} of the {@link #getRows() rows} or {@link #getNodes() nodes}.
     * 
     * @return true if this result set is empty, or false if there is at least one row.
     */
    public boolean isEmpty();

    /**
     * Returns an array of the {@link PropertyType} name for each of the columns in this result.
     * 
     * @return the array of property type names; never null, never has null elements, and the size always matches
     *         {@link QueryResult#getColumnNames()}.
     */
    public String[] getColumnTypes();

    /**
     * Get a description of ModeShape's plan for executing this query. The plan uses relational algebra and operations, and may be
     * used to get insight into what operations are performed when executing the query.
     * <p>
     * Note that as of ModeShape 3.1, the plan is always captured and available, though this may change in future versions. This
     * means that clients should be written to never <i>expect</i> a non-null String response from this method.
     * </p>
     * 
     * @return the string representation of the query plan as executed by the query; may be null if the query plan was not
     *         captured for the query (though currently it is always captured)
     */
    public String getPlan();

    /**
     * Get any warnings that might describe potential problems with this query.
     * <p>
     * Note that a query that has warnings is not necessarily incorrect or potentially wrong - because of residual properties,
     * ModeShape may produce warnings for queries that are perfectly valid.
     * </p>
     * <p>
     * However, if a query does not give the expected results (during development), check the warnings to see if ModeShape can
     * suggest specific things to look at. For example, a warnings might suggest that a column might be resolved on a different
     * selector, or that a column might have been misspelled.
     * </p>
     * 
     * @return the collection of warnings; never null be empty when there are no warnings
     */
    public Collection<String> getWarnings();

    /**
     * Close and release all resources associated with these results. This method is optional but recommended, since it allows
     * client applications full control over when such resources can be reclaimed. If this method is not called, then the results'
     * resources will be reclaimed when the session's {@link Session#logout()} method is called.
     */
    @Override
    public void close();

}
