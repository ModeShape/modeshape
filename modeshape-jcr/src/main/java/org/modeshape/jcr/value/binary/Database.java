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
package org.modeshape.jcr.value.binary;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.value.BinaryKey;

/**
 * Helper class for manipulation with database.
 * <p>
 * This class looks for database SQL statements in properties files named "<code>binary_store_{type}_database.properties</code>"
 * located within the "org/modeshape/jcr/database" area of the classpath, where "<code>{type}</code>" is {@link #determineType(java.sql.DatabaseMetaData)
 * determined} from the connection, and matches one of the following:
 * <ul>
 * <li><code>mysql</code></li>
 * <li><code>postgres</code></li>
 * <li><code>derby</code></li>
 * <li><code>hsql</code></li>
 * <li><code>h2</code></li>
 * <li><code>sqlite</code></li>
 * <li><code>db2</code></li>
 * <li><code>db2_390</code></li>
 * <li><code>informix</code></li>
 * <li><code>interbase</code></li>
 * <li><code>firebird</code></li>
 * <li><code>sqlserver</code></li>
 * <li><code>access</code></li>
 * <li><code>oracle</code></li>
 * <li><code>sybase</code></li>
 * </ul>
 * If the corresponding file is not found on the classpath, then the "<code>binary_store_default_database.properties</code>" file
 * is used.
 * </p>
 * <p>
 * Each property file should contain the set of DDL and DML statements that are used by the binary store, and the
 * database-specific file allows database-specific schemas and queries to be used. If the properties file that corresponds to the
 * connection's database type is not found on the classpath, then the "<code>binary_store_default_database.properties</code>" file
 * is used.
 * </p>
 * <p>
 * ModeShape does not provide out-of-the-box properties files for each of the database types listed above. If you run into any
 * problems, you can override the statements by providing a property file that matches the naming pattern described above, and by
 * putting that file on the classpath. (If you want to override one of ModeShape's out-of-the-box properties files, then be sure
 * to put your custom file first on the classpath.)
 * </p>
 * 
 * @author kulikov
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class Database {
    public static final String TABLE_NAME = "CONTENT_STORE";
    public static final String STATEMENTS_FILE_PATH = "org/modeshape/jcr/database/";

    protected static final String STATEMENTS_FILE_PREFIX = "binary_store_";
    protected static final String STATEMENTS_FILENAME_SUFFIX = "_database.properties";
    protected static final String DEFAULT_STATEMENTS_FILE_PATH = STATEMENTS_FILE_PATH + STATEMENTS_FILE_PREFIX + "default"
                                                                 + STATEMENTS_FILENAME_SUFFIX;

    private static final Logger LOGGER = Logger.getLogger(Database.class);

    private static final String INSERT_CONTENT_STMT_KEY = "add_content";
    private static final String USED_CONTENT_STMT_KEY = "get_used_content";
    private static final String UNUSED_CONTENT_STMT_KEY = "get_unused_content";
    private static final String MARK_UNUSED_STMT_KEY = "mark_unused";
    private static final String MARK_USED_STMT_KEY = "mark_used";
    private static final String REMOVE_EXPIRED_STMT_KEY = "remove_expired";
    private static final String GET_MIMETYPE_STMT_KEY = "get_mimetype";
    private static final String SET_MIMETYPE_STMT_KEY = "set_mimetype";
    private static final String GET_EXTRACTED_TEXT_STMT_KEY = "get_extracted_text";
    private static final String SET_EXTRACTED_TEXT_STMT_KEY = "set_extracted_text";
    private static final String GET_BINARY_KEYS_STMT_KEY = "get_binary_keys";
    private static final String CREATE_TABLE_STMT_KEY = "create_table";
    private static final String TABLE_EXISTS_STMT_KEY = "table_exists_query";

    public static enum Type {
        MYSQL,
        POSTGRES,
        DERBY,
        HSQL,
        H2,
        SQLITE,
        DB2,
        DB2_390,
        INFORMIX,
        INTERBASE,
        FIREBIRD,
        SQLSERVER,
        ACCESS,
        ORACLE,
        SYBASE,
        CASSANDRA,
        UNKNOWN
    }

    private final Type databaseType;
    private final String prefix;
    private final String tableName;

    private Properties statements;

    /**
     * Creates new instance of the database.
     *
     * @param connection a {@link java.sql.Connection} instance; may not be null
     * @throws java.io.IOException if the statements cannot be processed
     * @throws java.sql.SQLException if the db initialization sequence fails
     */
    protected Database( Connection connection ) throws IOException, SQLException {
        this(connection, null, null);
    }

    /**
     * Creates new instance of the database.
     *
     * @param connection a {@link java.sql.Connection} instance; may not be null
     * @param type the type of database; may be null if the type is to be determined
     * @param prefix the prefix for the table name; may be null or blank
     * @throws java.io.IOException if the statements cannot be processed
     * @throws java.sql.SQLException if the db initialization sequence fails
     */
    protected Database( Connection connection,
                     Type type,
                     String prefix ) throws IOException, SQLException {
        assert connection != null;
        DatabaseMetaData metaData = connection.getMetaData();
        this.databaseType = type != null ? type : determineType(metaData);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Discovered DBMS type for binary store as '{0}' on '{1}", databaseType, metaData.getURL());
        }
        this.prefix = prefix == null ? null : prefix.trim();
        this.tableName = this.prefix != null && this.prefix.length() != 0 ? this.prefix + TABLE_NAME : TABLE_NAME;

        initializeStatements();
        initializeStorage(connection);
    }

    private void initializeStatements() throws IOException {
        // Load the default statements ...
        String statementsFilename = DEFAULT_STATEMENTS_FILE_PATH;
        InputStream statementStream = getClass().getClassLoader().getResourceAsStream(statementsFilename);
        Properties defaultStatements = new Properties();
        try {
            LOGGER.trace("Loading default statement from '{0}'", statementsFilename);
            defaultStatements.load(statementStream);
        } finally {
            statementStream.close();
        }

        // Look for type-specific statements ...
        statementsFilename = STATEMENTS_FILE_PATH + STATEMENTS_FILE_PREFIX + databaseType.name().toLowerCase()
                             + STATEMENTS_FILENAME_SUFFIX;
        statementStream = getClass().getClassLoader().getResourceAsStream(statementsFilename);
        if (statementStream != null) {
            // Try to read the type-specific statements ...
            try {
                LOGGER.trace("Loading DBMS-specific statement from '{0}'", statementsFilename);
                statements = new Properties(defaultStatements);
                statements.load(statementStream);
            } finally {
                statementStream.close();
            }
        } else {
            // No type-specific statements, so just use the default statements ...
            statements = defaultStatements;
            LOGGER.trace("No DBMS-specific statement found in '{0}'", statementsFilename);
        }
    }

    private void initializeStorage( Connection connection ) throws SQLException {
        //First, prepare a statement to see if the table exists ...
        boolean createTable = true;
        PreparedStatement exists = null;
        try {
            exists = prepareStatement(TABLE_EXISTS_STMT_KEY, connection);
            execute(exists);
            createTable = false;
        } catch (SQLException e) {
            // proceed to create the table ...
        } finally {
           tryToClose(exists);
        }

        if (createTable) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Unable to find existing table. Attempting to create '{0}' table in {1}", tableName,
                             connection.getMetaData().getURL());
            }
            PreparedStatement create = prepareStatement(CREATE_TABLE_STMT_KEY, connection);
            try {
                execute(create);
            } catch (SQLException e) {
                String msg = JcrI18n.errorCreatingDatabaseTable.text(tableName, databaseType);
                throw new RuntimeException(msg, e);
            } finally {
               tryToClose(create);
            }
        }
    }

    protected String getTableName() {
        return tableName;
    }

    protected PreparedStatement prepareStatement( String statementKey, Connection connection ) throws SQLException {
        String statementString = statements.getProperty(statementKey);
        statementString = StringUtil.createString(statementString, tableName);
        LOGGER.trace("Preparing statement: {0}", statementString);
        return connection.prepareStatement(statementString);
    }

    protected Type determineType( DatabaseMetaData metaData ) throws SQLException {

        String name = metaData.getDatabaseProductName().toLowerCase();
        if (name.toLowerCase().contains("mysql")) {
            return Type.MYSQL;
        } else if (name.contains("postgres")) {
            return Type.POSTGRES;
        } else if (name.contains("derby")) {
            return Type.DERBY;
        } else if (name.contains("hsql") || name.toLowerCase().contains("hypersonic")) {
            return Type.HSQL;
        } else if (name.contains("h2")) {
            return Type.H2;
        } else if (name.contains("sqlite")) {
            return Type.SQLITE;
        } else if (name.contains("db2")) {
            return Type.DB2;
        } else if (name.contains("informix")) {
            return Type.INFORMIX;
        } else if (name.contains("interbase")) {
            return Type.INTERBASE;
        } else if (name.contains("firebird")) {
            return Type.FIREBIRD;
        } else if (name.contains("sqlserver") || name.toLowerCase().contains("microsoft")) {
            return Type.SQLSERVER;
        } else if (name.contains("access")) {
            return Type.ACCESS;
        } else if (name.contains("oracle")) {
            return Type.ORACLE;
        } else if (name.contains("adaptive")) {
            return Type.SYBASE;
        } else if (name.contains("Cassandra")) {
            return Type.CASSANDRA;
        }
        return Type.UNKNOWN;
    }

    protected void insertContent( BinaryKey key,
                                  InputStream stream,
                                  long size,
                                  Connection connection ) throws SQLException {
        PreparedStatement addContentSql = prepareStatement(INSERT_CONTENT_STMT_KEY, connection);
        try {
            addContentSql.setString(1, key.toString());
            addContentSql.setTimestamp(2, new java.sql.Timestamp(System.currentTimeMillis()));
            addContentSql.setBinaryStream(3, stream, size);
            execute(addContentSql);
        } finally {
            try {
                //it's not guaranteed that the driver will close the stream, so we mush always close it to prevent read-locks
                stream.close();
            } catch (IOException e) {
                //ignore
            }
            tryToClose(addContentSql);
        }
    }

    protected boolean contentExists(BinaryKey key, boolean inUse, Connection connection) throws SQLException {
        PreparedStatement readContentStatement = inUse ?
                                prepareStatement(USED_CONTENT_STMT_KEY, connection) :
                                prepareStatement(UNUSED_CONTENT_STMT_KEY, connection);
        try {
            readContentStatement.setString(1, key.toString());
            ResultSet rs = executeQuery(readContentStatement);
            return rs.next();
        } catch (SQLException e) {
            LOGGER.debug("Cannot determine if content exists under key '{0}'", key.toString());
            return false;
        } finally {
            //always closes the result set
            tryToClose(readContentStatement);
        }
    }

    /**
     * Attempts to return the content stream for a given binary value.
     * @param key a {@link org.modeshape.jcr.value.BinaryKey} the key of the binary value, may not be null
     * @param connection a {@link java.sql.Connection} instance, may not be null
     * @return either a stream that wraps the input stream of the binary value and closes the connection and the statement
     * when it terminates or {@code null}, meaning that the binary was not found.
     *
     * @throws SQLException if anything unexpected fails
     */
    protected InputStream readContent(BinaryKey key, Connection connection) throws SQLException {
        PreparedStatement readContentStatement = prepareStatement(USED_CONTENT_STMT_KEY, connection);
        try {
            readContentStatement.setString(1, key.toString());
            ResultSet rs = executeQuery(readContentStatement);
            if (!rs.next()) {
                tryToClose(readContentStatement);
                tryToClose(connection);
                return null;
            } else {
                return new DatabaseBinaryStream(connection, readContentStatement, rs.getBinaryStream(1));
            }
        } catch (SQLException e) {
            tryToClose(readContentStatement);
            tryToClose(connection);
            throw e;
        } catch (Throwable t) {
            tryToClose(readContentStatement);
            tryToClose(connection);
            throw new RuntimeException(t);
        }
    }

    protected void markUnused( Iterable<BinaryKey> keys, Connection connection ) throws SQLException {
        PreparedStatement markUnusedSql = prepareStatement(MARK_UNUSED_STMT_KEY, connection);
        try {
            Timestamp now = new Timestamp(System.currentTimeMillis());
            for (BinaryKey key : keys) {
                markUnusedSql.setTimestamp(1, now);
                markUnusedSql.setString(2, key.toString());
                executeUpdate(markUnusedSql);
            }
        } finally {
            tryToClose(markUnusedSql);
        }
    }

    protected void restoreContent( BinaryKey key, Connection connection ) throws SQLException {
        PreparedStatement markUsedSql = prepareStatement(MARK_USED_STMT_KEY, connection);
        try {
            markUsedSql.setString(1, key.toString());
            execute(markUsedSql);
        } finally {
            tryToClose(markUsedSql);
        }
    }

    protected void removeExpiredContent( long deadline, Connection connection ) throws SQLException {
        PreparedStatement removedExpiredSql = prepareStatement(REMOVE_EXPIRED_STMT_KEY, connection);
        try {
            removedExpiredSql.setTimestamp(1, new java.sql.Timestamp(deadline));
            execute(removedExpiredSql);
        } finally {
            tryToClose(removedExpiredSql);
        }
    }

    protected String getMimeType( BinaryKey key, Connection connection ) throws SQLException {
        PreparedStatement getMimeType = prepareStatement(GET_MIMETYPE_STMT_KEY, connection);
        try {
            getMimeType.setString(1, key.toString());
            ResultSet rs = executeQuery(getMimeType);
            if (rs.next()) {
                return rs.getString(1);
            } else {
                return null;
            }
        } finally {
            //will also close the result set
            tryToClose(getMimeType);
        }
    }

    protected void setMimeType( BinaryKey key,
                                String mimeType,
                                Connection connection) throws SQLException {
        PreparedStatement setMimeTypeSQL = prepareStatement(SET_MIMETYPE_STMT_KEY, connection);
        try {
            setMimeTypeSQL.setString(1, mimeType);
            setMimeTypeSQL.setString(2, key.toString());
            executeUpdate(setMimeTypeSQL);
        } finally {
            tryToClose(setMimeTypeSQL);
        }
    }

    protected String getExtractedText( BinaryKey key, Connection connection ) throws SQLException {
        PreparedStatement getExtractedTextSql = prepareStatement(GET_EXTRACTED_TEXT_STMT_KEY, connection);
        try {
            getExtractedTextSql.setString(1, key.toString());
            ResultSet rs = executeQuery(getExtractedTextSql);
            if (rs.next()) {
                return rs.getString(1);
            } else {
                return null;
            }
        } finally {
            //will also close the result set
            tryToClose(getExtractedTextSql);
        }
    }

    protected void setExtractedText( BinaryKey key,
                                     String text,
                                     Connection connection ) throws SQLException {
        PreparedStatement setExtractedTextSql = prepareStatement(SET_EXTRACTED_TEXT_STMT_KEY, connection);
        try {
            setExtractedTextSql.setString(1, text);
            setExtractedTextSql.setString(2, key.toString());
            executeUpdate(setExtractedTextSql);
        } finally {
            tryToClose(setExtractedTextSql);
        }
    }

    protected Set<BinaryKey> getBinaryKeys( Connection connection ) throws SQLException {
        PreparedStatement getBinaryKeysSql = prepareStatement(GET_BINARY_KEYS_STMT_KEY, connection);
        Set<BinaryKey> keys = new LinkedHashSet<BinaryKey>();

        try {
            ResultSet rs = executeQuery(getBinaryKeysSql);
            while (rs.next()) {
                keys.add(new BinaryKey(rs.getString(1)));
            }
            return keys;
        } finally {
            tryToClose(getBinaryKeysSql);
        }
    }

    private void execute( PreparedStatement sql ) throws SQLException {
        LOGGER.trace("Executing statement: {0}", sql);
        sql.execute();
    }

    private ResultSet executeQuery( PreparedStatement sql ) throws SQLException {
        LOGGER.trace("Executing query statement: {0}", sql);
        return sql.executeQuery();
    }

    private void executeUpdate( PreparedStatement sql ) throws SQLException {
        LOGGER.trace("Executing update statement: {0}", sql);
        sql.executeUpdate();
    }

    protected class DatabaseBinaryStream extends InputStream {
        private final Connection connection;
        private final PreparedStatement statement;
        private final InputStream jdbcBinaryStream;

        protected DatabaseBinaryStream( Connection connection, PreparedStatement statement, InputStream jdbcBinaryStream ) {
            this.connection = connection;
            this.statement = statement;
            this.jdbcBinaryStream = jdbcBinaryStream;
        }

        @Override
        public int read() throws IOException {
            return jdbcBinaryStream.read();
        }

        @Override
        public int read( byte[] b ) throws IOException {
            return jdbcBinaryStream.read(b);
        }

        @Override
        public int read( byte[] b, int off, int len ) throws IOException {
            return jdbcBinaryStream.read(b, off, len);
        }

        @Override
        public long skip( long n ) throws IOException {
            return jdbcBinaryStream.skip(n);
        }

        @Override
        public int available() throws IOException {
            return jdbcBinaryStream.available();
        }

        @Override
        public void close() throws IOException {
            tryToClose(statement);
            tryToClose(connection);
        }

        @Override
        public synchronized void mark( int readlimit ) {
            jdbcBinaryStream.mark(readlimit);
        }

        @Override
        public synchronized void reset() throws IOException {
            jdbcBinaryStream.reset();
        }

        @Override
        public boolean markSupported() {
            return jdbcBinaryStream.markSupported();
        }
    }

    protected static void tryToClose(PreparedStatement statement) {
        if (statement != null) {
            try {
                statement.close();
            } catch (Throwable t) {
                LOGGER.debug(t, "Cannot close prepared statement");
            }
        }
    }

    protected static void tryToClose(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (Throwable t) {
                LOGGER.debug(t, "Cannot close prepared statement");
            }
        }
    }
}
