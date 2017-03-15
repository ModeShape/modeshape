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
package org.modeshape.persistence.relational;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.modeshape.schematic.document.Document;

/**
 * A group of statements which are executed against a database.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @since 5.0
 */
public interface Statements {

    /**
     * A set of constants representing keys in the statements properties files, for each of the DB statements
     */
    String CREATE_TABLE = "create_table";
    String DELETE_TABLE = "delete_table";
    String GET_ALL_IDS = "get_all_ids";
    String GET_BY_ID = "get_by_id";
    String CONTENT_EXISTS = "content_exists";
    String INSERT_CONTENT = "insert_content";
    String UPDATE_CONTENT = "update_content";
    String REMOVE_CONTENT = "remove_content";
    String REMOVE_ALL_CONTENT = "remove_all_content";
    String GET_MULTIPLE = "get_multiple";
    String LOCK_CONTENT = "lock_content";
    String MULTIPLE_SELECTION = "multiple_selection_clause";

    /**
     * Create a new table.
     *
     * @param connection a {@link Connection} instance; may not be null
     * @return nothing
     * @throws SQLException if the operation fails.
     */
    Void createTable( Connection connection ) throws SQLException;

    /**
     * Drops a table.
     *
     * @param connection a {@link Connection} instance; may not be null
     * @return nothing
     * @throws SQLException if the operation fails.
     */
    Void dropTable( Connection connection ) throws SQLException;

    /**
     * Returns all the ids from a table.
     *
     * @param connection a {@link Connection} instance; may not be null
     * @return a {@link List} of ids; never {@code null}
     * @throws SQLException if the operation fails.
     */
    List<String> getAllIds(Connection connection) throws SQLException;

    /**
     * Searches for a document with a certain id.
     *
     * @param connection a {@link Connection} instance; may not be null
     * @return a {@link Document} instance or {@code null} if the document is not found.
     * @throws SQLException if the operation fails.
     */
    Document getById( Connection connection, String id ) throws SQLException;

    /**
     * Loads multiple documents based on a set of ids.
     *
     * <p>
     * Depending on the type of DB, if a very large number of IDs is used this may have side effects:
     * <ul>
     * <li>MySQL: limited by the max_allowed_packet value.</li>
     * <li>SQL Server: "limited by 65,536 * Network Packet Size" (which defaults to 4K).</li>
     * <li>Oracle has an expression limit of 1000 values.</li>
     * <li>PostgreSQL apparently slows dramatically with lots of values.</li>
     * </ul>
     * </p>
     *
     * @param connection a {@link Connection} instance; may not be null
     * @param ids a {@link Collection} of ids; may not be null
     * @param parser a {@link Function} which is used to transform or process each of documents corresponding to the given IDS; 
     * may not be null
     * @return a {@link List} of {@code Object} instances for each of the ids which were found in the DB; never {@code null}
     * @throws SQLException if the operation fails.
     */
    <R> List<R> load(Connection connection, Collection<String> ids, Function<Document, R> parser) throws SQLException;

    /**
     * Starts a batch update operation with the given connection.
     *
     * @param connection a {@link Connection} instance; may not be null
     * @return a {@link org.modeshape.persistence.relational.Statements.BatchUpdate} instance, never {@code null}
     */
    BatchUpdate batchUpdate( Connection connection );

    /**
     * Checks if there is a document with the given id.
     *
     * @param connection a {@link Connection} instance; may not be null
     * @param id a {@link String} the id of a document; may not be null
     * @return {@code true} if the document exists, {@code false} otherwise.
     * @throws SQLException if the operation fails.
     */
    boolean exists( Connection connection, String id ) throws SQLException;

    /**
     * Removes all the contents of a table.
     *
     * @param connection a {@link Connection} instance; may not be null
     * @return nothing
     * @throws SQLException if the operation fails.
     */
    Void removeAll( Connection connection ) throws SQLException;

    /**
     * Locks for writing the given list of ids. 
     * <p>
     * Note that if any of the ids are not present in the DB, they should not be taken into account and should simply be ignored 
     * as opposed to causing the operation to fail.
     * </p>
     * @param connection a {@link Connection} instance, never {@code null}
     * @param ids a {@link List} of IDs, never {@code null}
     * @return {@code true} if locks were successfully obtained, false otherwise
     * @throws SQLException if anything unexpected fails
     */
    boolean lockForWriting( Connection connection, List<String> ids ) throws SQLException;

    /**
     * A batch of table update operations.
     */
    interface BatchUpdate {
      
        /**
         * Inserts a bunch of documents into a table.
         *
         * @param documentsById a {@link Map} of documents keyed by their id; may not be {@code null}
         * @throws SQLException if the operation fails.
         */
        void insert( Map<String, Document> documentsById ) throws SQLException;

        /**
         * Updates a bunch of documents from a table.
         *
         * @param documentsById a {@link Map} of documents keyed by their id; may not be {@code null}
         * @throws SQLException if the operation fails.
         */
        void update( Map<String, Document> documentsById ) throws SQLException;

        /**
         * Removes a bunch of documents with a list of ids.
         *
         * @param ids@throws SQLException if the operation fails.
         */
        void remove( List<String> ids ) throws SQLException;
    }
}
