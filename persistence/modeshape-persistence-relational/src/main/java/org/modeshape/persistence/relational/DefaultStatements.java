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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.common.logging.Logger;
import org.modeshape.schematic.document.Bson;
import org.modeshape.schematic.document.Document;

/**
 * Default implementation for the {@link Statements} interface which applies to all databases.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @since 5.0
 */
public class DefaultStatements implements Statements {
    
    protected final Logger logger = Logger.getLogger(getClass());
    
    private final Map<String, String> statements;
    private final RelationalDbConfig config;

    protected DefaultStatements( RelationalDbConfig config, Map<String, String> statements ) {
        this.statements = statements;
        this.config = config;
    }

    @Override
    public Void createTable( Connection connection ) throws SQLException {
        logTableInfo("Creating table {0}...");
        try (PreparedStatement createStmt = connection.prepareStatement(statements.get(CREATE_TABLE))) {
            if (createStmt.executeUpdate() > 0) {
                logTableInfo("Table {0} created");
            } else {
                logTableInfo("Table {0} already exists");
            }
        }
        return null;
    }

    @Override
    public Void dropTable( Connection connection ) throws SQLException {
        logTableInfo("Dropping table {0}...");
        try (PreparedStatement createStmt = connection.prepareStatement(statements.get(DELETE_TABLE))) {
            if (createStmt.executeUpdate() > 0) {
                logTableInfo("Table {0} dropped");
            } else {
                logTableInfo("Table {0} does not exist");
            }
        }
        return null;
    }

    @Override
    public List<String> getAllIds(Connection connection) throws SQLException {
        logTableInfo("Returning all ids from {0}");
        try (PreparedStatement ps = connection.prepareStatement(statements.get(GET_ALL_IDS))) {
            List<String> result = new ArrayList<>();
            ps.setFetchSize(config.fetchSize());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getString(1));                    
                }
            }
            return result;
        }
    }

    @Override
    public Document getById( Connection connection, String id ) throws SQLException {
        if (logger.isDebugEnabled()) {
            logger.debug("Searching for entry by id {0} in {1}", id, tableName());
        }
        try (PreparedStatement ps = connection.prepareStatement(statements.get(GET_BY_ID))) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return readDocument(rs.getBinaryStream(1));
            }
        }
    }
    
    @Override
    public <R> List<R> load( Connection connection, List<String> ids, Function<Document, R> parser ) throws SQLException {
        if (logger.isDebugEnabled()) {
            logger.debug("Loading ids {0} from {1}", ids.toString(), tableName());
        }
        String getMultipleStatement = statements.get(GET_MULTIPLE);
        int batchLoadSize = batchLoadSize();
        List<R> results = new ArrayList<>();
        runBatchOperation(connection, getMultipleStatement, ids, batchLoadSize,
                          (dbConnection, statement, data) -> {
                              results.addAll(loadIDs(dbConnection, statement, ids, parser));
                              return true;
                          });
        return results;
    }
                    
    private <R> List<R> loadIDs( Connection connection, String statement, List<String> ids, Function<Document, R> parser) 
            throws SQLException {
        String params = ids.stream().map(id -> "?").collect(Collectors.joining(","));
        String statementString = statement.replaceAll("#", params);
        try (PreparedStatement ps = connection.prepareStatement(statementString)) {
            int paramIdx = 1;
            for (String id : ids) {
                ps.setString(paramIdx++, id);
            }
            
            try (ResultSet rs = ps.executeQuery()) {
                List<R> results = new ArrayList<>();
                while (rs.next()) {
                    Document document = readDocument(rs.getBinaryStream(1));
                    results.add(parser.apply(document));
                }
                return results;
            }
        }
    }

    @Override
    public boolean lockForWriting( Connection connection, List<String> ids ) throws SQLException {
        if (logger.isDebugEnabled()) {
            logger.debug("Attempting to lock ids {0} from {1}", ids.toString(), tableName());
        }
        String lockContent = statements.get(LOCK_CONTENT);
        int batchLoadSize = batchLoadSize();
        return runBatchOperation(connection, lockContent, ids, batchLoadSize, (dbConnection, statement, data) ->
                lockIDs(dbConnection, statement, ids));
    }

    private boolean lockIDs( Connection connection, String statement, List<String> ids ) throws SQLException {
        String params = ids.stream().map(id -> "?").collect(Collectors.joining(","));
        String statementString = statement.replaceAll("#", params);
        try (PreparedStatement ps = connection.prepareStatement(statementString)) {
            int paramIdx = 1;
            for (String id : ids) {
                ps.setString(paramIdx++, id);
            }
            
            try (ResultSet rs = ps.executeQuery()) {
                // any failed lock should result in a timeout being eventually thrown by the DB
                // ModeShape will frequently try to lock new nodes before inserting them, so it's important that this method 
                // returns 'true' for those nodes
                logger.debug("successfully locked ids");
                return true;
            } catch (SQLException e) {
                logger.debug(e, " cannot lock ids");
                return false;
            }
        } 
    }

    @Override
    public DefaultBatchUpdate batchUpdate( Connection connection ) {
        return new DefaultBatchUpdate(connection);
    }

    @Override
    public boolean exists( Connection connection, String id ) throws SQLException {
        if (logger.isDebugEnabled()) {
            logger.debug("Checking if the content with ID {0} exists in {1}", id, tableName());
        }
        
        try (PreparedStatement ps = connection.prepareStatement(statements.get(CONTENT_EXISTS))) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        }
    }

    @Override
    public Void removeAll( Connection connection ) throws SQLException {
        logTableInfo("Removing all content from {0}");
        try (PreparedStatement ps = connection.prepareStatement(statements.get(REMOVE_ALL_CONTENT))) {
            ps.executeUpdate();
        }
        return null;
    }
    
    protected int batchLoadSize() {
        return 500;
    }
   
    protected void logTableInfo( String message ) {
        if (logger.isDebugEnabled()) {
            logger.debug(message, tableName());
        }
    }

    protected String tableName() {
        return config.tableName();
    }

    protected Document readDocument(InputStream is) {
        try (InputStream contentStream = config.compress() ? new GZIPInputStream(is) : is) {
            return Bson.read(contentStream);
        } catch (IOException e) {
            throw new RelationalProviderException(e);
        }
    }

    protected byte[] writeDocument(Document content)  {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (OutputStream out = config.compress() ? new GZIPOutputStream(bos) : bos) {
                Bson.write(content, out);
            }
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RelationalProviderException(e);
        }
    }

    private <T> boolean runBatchOperation(Connection connection, String statement, List<T> data, int batchSize,
                                          BatchOperation<T> operation) throws SQLException {
        if (data.isEmpty()) {
            return false;
        }
        
        int dataSize = data.size();
        if (dataSize <= batchSize) {
            return operation.run(connection, statement, data);
        }

        int startIdx = 0;
        while (startIdx < dataSize) {
            int endIdx = startIdx + batchSize > dataSize ? dataSize : startIdx + batchSize;
            boolean result = operation.run(connection, statement, data.subList(startIdx, endIdx));
            if (!result) {
                return false;
            }
            startIdx = endIdx;
        }    
        return true;
    }
        
        
    @FunctionalInterface
    protected interface BatchOperation<T> {
        boolean run(Connection connection, String statement, List<T> data) throws SQLException;
    }    

    @NotThreadSafe
    protected class DefaultBatchUpdate implements BatchUpdate{
        private final Connection connection;
     
        protected DefaultBatchUpdate( Connection connection ) {
            this.connection = connection;
        }

        @Override
        public void insert( Map<String, Document> documentsById ) throws SQLException {
            String sql = statements.get(INSERT_CONTENT);
            PreparedStatement insert = connection.prepareStatement(sql);
            documentsById.forEach(( id, document ) -> {
                if (logger.isDebugEnabled()) {
                    logger.debug("adding batch statement: {0}", sql.replaceFirst("\\?", id));
                }
                insertDocument(insert, id, document);
            });
            insert.executeBatch();
        }
        
        protected void insertDocument(PreparedStatement statement, String id, Document document) {
            try {
                statement.setString(1, id);
                byte[] content = writeDocument(document);
                statement.setBytes(2, content);
                statement.addBatch();
            } catch (SQLException e) {
                throw new RelationalProviderException(e);
            }    
        }

        @Override
        public void update( Map<String, Document> documentsById ) throws SQLException {
            String sql = statements.get(UPDATE_CONTENT);
            PreparedStatement update = connection.prepareStatement(sql);
            documentsById.forEach(( id, document ) -> {
                if (logger.isDebugEnabled()) {
                    logger.debug("adding batch statement: {0}", sql.replaceFirst(" ID.*=.*\\?", " ID = " + id));
                }
                updateDocument(update, id, document);
            });
            update.executeBatch();
        }

        protected void updateDocument(PreparedStatement statement, String id, Document document) {
            try {
                byte[] content = writeDocument(document);
                statement.setBytes(1, content);
                statement.setString(2, id);
                statement.addBatch();
            } catch (SQLException e) {
                throw new RelationalProviderException(e);
            }
        }

        @Override
        public void remove( List<String> ids ) throws SQLException {
            String sql = statements.get(REMOVE_CONTENT);
            runBatchOperation(connection, sql, ids, batchLoadSize(), this::batchRemove);    
        }

        private boolean batchRemove( Connection connection, String statement, List<String> ids ) throws SQLException {
            String params = ids.stream().map(id -> "?").collect(Collectors.joining(","));
            String statementString = statement.replaceAll("#", params);
            if (logger.isDebugEnabled()) {
                logger.debug("running statement: {0}", statementString);
            }
            try (PreparedStatement remove = connection.prepareStatement(statementString)) {
                int paramIdx = 1;
                for (String id : ids) {
                    remove.setString(paramIdx++, id);
                }
                remove.executeUpdate();
            }
            return true;
        }
    }
}
