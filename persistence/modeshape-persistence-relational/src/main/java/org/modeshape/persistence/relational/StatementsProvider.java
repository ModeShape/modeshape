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

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.modeshape.common.database.DatabaseType;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.StringUtil;

/**
 * Class which manages the SQL statements used by the {@link RelationalDb} to interact with a particular DB.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @since 5.0
 */
public final class StatementsProvider {
    private static final Logger LOGGER = Logger.getLogger(StatementsProvider.class);

    private static final String CREATE_TABLE = "create_table";
    private static final String DELETE_TABLE = "delete_table";
    private static final String GET_ALL_IDS = "get_all_ids";
    private static final String GET_BY_ID = "get_by_id";
    private static final String CONTENT_EXISTS = "content_exists";
    private static final String INSERT_CONTENT = "insert_content";
    private static final String UPDATE_CONTENT = "update_content";
    private static final String REMOVE_CONTENT = "remove_content";
    private static final String REMOVE_ALL_CONTENT = "remove_all_content";

    private final Map<String, String> statements = new HashMap<>();
    private final String tableName;
   
    protected StatementsProvider(DatabaseType dbType, String tableName) {
        this.tableName = Objects.requireNonNull(tableName);
        loadStatementsResource(dbType);
    }
    
    protected Void createTable(Connection connection) throws SQLException {
        logDebug("Creating table {0}...", tableName);
        try (PreparedStatement createStmt = connection.prepareStatement(statements.get(CREATE_TABLE))) {
            if (createStmt.executeUpdate() > 0) {
                logDebug("Table {0} created", tableName);
            } else {
                logDebug("Table {0} already exists", tableName);
            }
        }
        return null;
    }

    protected Void dropTable(Connection connection) throws SQLException {
        logDebug("Dropping table {0}...", tableName);
        try (PreparedStatement createStmt = connection.prepareStatement(statements.get(DELETE_TABLE))) {
            if (createStmt.executeUpdate() > 0) {
                logDebug("Table {0} dropped", tableName);
            } else {
                logDebug("Table {0} does not exist", tableName);
            }
        }
        return null;
    }

    protected <T> T getAllIds(Connection connection, int fetchSize, Function<ResultSet, T> function) throws SQLException {
        logDebug("Returning all ids from {0}", tableName);
        try (PreparedStatement ps = connection.prepareStatement(statements.get(GET_ALL_IDS))) {
            ps.setFetchSize(fetchSize);
            try (ResultSet rs = ps.executeQuery()) {
                return function.apply(rs);
            }
        }
    }

    protected <T> T getById(Connection connection, String id, Function<ResultSet, T> function) throws SQLException {
        logDebug("Searching for entry by id {0} in {1}", id, tableName);
        try (PreparedStatement ps = connection.prepareStatement(statements.get(GET_BY_ID))) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return function.apply(rs);
            }
        }
    }

    protected BatchUpdate batchUpdate(Connection connection) {
        return new BatchUpdate(connection);
    }

    protected void insertOrUpdateContent(Connection connection, String id,
                                         StreamSupplier streamSupplier) throws SQLException {
        logDebug("Performing insert or update on {0} in {1}", id, tableName);
        if (contentExists(connection, id)) {
            try (PreparedStatement ps = connection.prepareStatement(statements.get(UPDATE_CONTENT))) {
                ps.setBinaryStream(1, streamSupplier.get(), streamSupplier.length());
                ps.setString(2, id);
                if (ps.executeUpdate() > 0) {
                    logDebug("Update successful on {0}", id);
                }
            }
        } else {
            logDebug("ID {0} not present in {1}. Attempting to insert...", id, tableName);
            // the update was not performed, so try an insert because the object is most likely missing
            try (PreparedStatement ps = connection.prepareStatement(statements.get(INSERT_CONTENT))) {
                ps.setString(1, id);
                ps.setBinaryStream(2, streamSupplier.get(), streamSupplier.length());
                if (ps.executeUpdate() > 0) {
                    logDebug("Insert successful on {0}", tableName);
                } else {
                    throw new RelationalProviderException(RelationalProviderI18n.insertOrUpdateFailed, id,
                                                          " cannot insert new entry");
                }
            }
        }
    }

    protected boolean contentExists(Connection connection, String id) throws SQLException {
        logDebug("Checking if the content with ID {0} exists in {1}", id, tableName);
        try (PreparedStatement ps = connection.prepareStatement(statements.get(CONTENT_EXISTS))) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            return rs.next();            
        }
    }

    protected boolean removeContent(Connection connection, String id) throws SQLException {
        logDebug("Removing entry {0} from {1}", id, tableName);
        try (PreparedStatement ps = connection.prepareStatement(statements.get(REMOVE_CONTENT))) {
            ps.setString(1, id);
            boolean success = ps.executeUpdate() > 0;
            if (success) {
                logDebug("Successfully removed {0} ", id);
            } else {
                logDebug("{0} not removed");
            }
            return success;
        }
    }

    protected Void removeAllContent(Connection connection) throws SQLException {
        logDebug("Removing all content from {0}", tableName);
        try (PreparedStatement ps = connection.prepareStatement(statements.get(REMOVE_ALL_CONTENT))) {
            ps.executeUpdate();
        }
        return null;
    }

    private void loadStatementsResource(DatabaseType dbType) {
        try (InputStream fileStream = statementsFile(dbType)){
            Properties statements = new Properties();
            statements.load(fileStream);
            statements.entrySet().forEach(entry -> this.statements.put(entry.getKey().toString(),
                                                                       StringUtil.createString(entry.getValue().toString(),
                                                                                               tableName)));
        } catch (IOException e) {
            throw new RelationalProviderException(e);
        }
    }

    private InputStream statementsFile(DatabaseType dbType) {
        String filePrefix = StatementsProvider.class.getPackage().getName().replaceAll("\\.", "/") + "/" + dbType.nameString().toLowerCase();
        // first search for a file matching the major.minor version....
        String majorMinorFile = filePrefix + String.format("%s.%s_database.properties", dbType.majorVersion(), dbType.minorVersion());
        // then a file matching just major version
        String majorFile = filePrefix + String.format("%s_database.properties", dbType.majorVersion());
        // the a default with just the db name
        String defaultFile = filePrefix + "_database.properties";
        return Stream.of(majorMinorFile, majorFile, defaultFile)
                     .map(fileName -> StatementsProvider.class.getClassLoader().getResourceAsStream(fileName))
                     .filter(Objects::nonNull)
                     .findFirst()
                     .orElseThrow(() -> new RelationalProviderException(RelationalProviderI18n.unsupportedDBError, dbType));   
    }

    @Override
    public String toString() {
        return "Statements[tableName=" + tableName + ", statements=" + statements + ']';
    }
    
    protected interface StreamSupplier extends Supplier<InputStream> {
        long length();
    }
    
    private void logDebug(String message, String...args) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(message, args);
        }
    }
   
    protected class BatchUpdate {
        private final Connection connection;

        private final AtomicReference<PreparedStatement> insertStatement;
        private final AtomicReference<PreparedStatement> updateStatement;
        private final AtomicReference<PreparedStatement> removeStatement;

        protected BatchUpdate(Connection connection) {
            this.connection = connection;
            this.insertStatement = new AtomicReference<>();
            this.updateStatement = new AtomicReference<>();
            this.removeStatement = new AtomicReference<>();
        }
        
        protected BatchUpdate insert(String id, StreamSupplier streamSupplier) throws SQLException {
            String sql = statements.get(INSERT_CONTENT);
            this.insertStatement.compareAndSet(null, connection.prepareStatement(sql));
            PreparedStatement insert = this.insertStatement.get();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("adding batch statement: {0}", sql.replaceFirst("\\?", id));
            }
            insert.setString(1, id);
            insert.setBinaryStream(2, streamSupplier.get(), streamSupplier.length());
            insert.addBatch();
            return this;            
        }
        
        protected BatchUpdate update(String id, StreamSupplier streamSupplier) throws SQLException {
            String sql = statements.get(UPDATE_CONTENT);
            this.updateStatement.compareAndSet(null, connection.prepareStatement(sql));
            PreparedStatement update = this.updateStatement.get();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("adding batch statement: {0}", sql.replaceFirst(" ID.*=.*\\?"," ID = " + id));
            }
            update.setBinaryStream(1, streamSupplier.get(), streamSupplier.length());
            update.setString(2, id);
            update.addBatch();
            return this;            
        } 
        
        protected BatchUpdate remove(String id) throws SQLException {
            String sql = statements.get(REMOVE_CONTENT);
            this.removeStatement.compareAndSet(null, connection.prepareStatement(sql));
            PreparedStatement remove = removeStatement.get();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("adding batch statement: {0}", sql.replaceFirst("\\?", id));
            }
            remove.setString(1, id);
            remove.addBatch();
            return this;
        }
        
        private void executeBatch(AtomicReference<PreparedStatement> statementHolder) throws SQLException {
            PreparedStatement statement = statementHolder.getAndSet(null);
            if (statement != null) {
                try {
                    logDebug("executing batch statements...");
                    statement.executeBatch();
                } finally {
                    statement.close();
                }
            }
        }
        
        protected void execute() throws SQLException {
            executeBatch(insertStatement);
            executeBatch(updateStatement);
            executeBatch(removeStatement);
        }
    }
}
