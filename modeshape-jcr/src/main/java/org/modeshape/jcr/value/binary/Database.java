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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
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
 * located within the "org/modeshape/jcr/database" area of the classpath, where "<code>{type}</code>" is {@link #determineType()
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
 */
public class Database {

    private static final Logger LOGGER = Logger.getLogger(Database.class);

    public static final String TABLE_NAME = "CONTENT_STORE";

    public static final String STATEMENTS_FILE_PATH = "org/modeshape/jcr/database/";
    protected static final String STATEMENTS_FILE_PREFIX = "binary_store_";
    protected static final String STATEMENTS_FILENAME_SUFFIX = "_database.properties";
    protected static final String DEFAULT_STATEMENTS_FILE_PATH = STATEMENTS_FILE_PATH + STATEMENTS_FILE_PREFIX + "default"
                                                                 + STATEMENTS_FILENAME_SUFFIX;

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
        UNKNOWN;
    }

    private final Connection connection;
    private final Type databaseType;
    private final String prefix;
    private final String tableName;
    private final Properties statements;
    private PreparedStatement addContentSql;
    private PreparedStatement getUsedContentSql;
    private PreparedStatement getUnusedContentSql;
    private PreparedStatement markUnusedSql;
    private PreparedStatement markUsedSql;
    private PreparedStatement removedExpiredSql;
    private PreparedStatement getMimeType;
    private PreparedStatement setMimeType;
    private PreparedStatement getExtractedTextSql;
    private PreparedStatement setExtractedTextSql;
    private PreparedStatement getBinaryKeysSql;

    /**
     * Creates new instance of the database.
     * 
     * @param connection connection to a database
     * @throws BinaryStoreException if the database type cannot be determined
     */
    public Database( Connection connection ) throws BinaryStoreException {
        this(connection, null, null);
    }

    /**
     * Creates new instance of the database.
     * 
     * @param connection connection to a database
     * @param type the type of database; may be null if the type is to be determined
     * @param prefix the prefix for the table name; may be null or blank
     * @throws BinaryStoreException if the database type cannot be determined
     */
    public Database( Connection connection,
                     Type type,
                     String prefix ) throws BinaryStoreException {
        assert connection != null;
        this.connection = connection;
        this.databaseType = type != null ? type : determineType();
        this.prefix = prefix == null ? null : prefix.trim();
        this.tableName = this.prefix != null && this.prefix.length() != 0 ? this.prefix + TABLE_NAME : TABLE_NAME;
        LOGGER.debug("Discovered DBMS type for binary store as '{0}' on {1}", databaseType, connection);

        try {
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

        } catch (IOException e) {
            throw new BinaryStoreException(e);
        }
    }

    /**
     * Prepare this instance and the underlying database for usage.
     * 
     * @throws BinaryStoreException if there is a problem
     */
    public void initialize() throws BinaryStoreException {
        try {
            // First, prepare a statement to see if the table exists ...
            boolean createTable = true;
            try {
                PreparedStatement exists = prepareStatement("table_exists_query");
                LOGGER.trace("Running statement: {0}", exists);
                exists.execute();
                exists.close();
                createTable = false;
            } catch (SQLException e) {
                // proceed to create the table ...
            }

            if (createTable) {
                LOGGER.debug("Unable to find existing table. Attempting to create '{0}' table in {1}", tableName, connection);
                try {
                    PreparedStatement create = prepareStatement("create_table");
                    LOGGER.trace("Running statement: {0}", create);
                    create.execute();
                    create.close();
                } catch (SQLException e) {
                    String msg = JcrI18n.errorCreatingDatabaseTable.text(tableName, databaseType, connection, e.getMessage());
                    throw new BinaryStoreException(msg);
                }
            }

            // Now prepare all of the SQL statements ...
            addContentSql = prepareStatement("add_content");
            getUsedContentSql = prepareStatement("get_used_content");
            getUnusedContentSql = prepareStatement("get_unused_content");
            markUnusedSql = prepareStatement("mark_unused");
            markUsedSql = prepareStatement("mark_used");
            removedExpiredSql = prepareStatement("remove_expired");
            getMimeType = prepareStatement("get_mimetype");
            setMimeType = prepareStatement("set_mimetype");
            getExtractedTextSql = prepareStatement("get_extracted_text");
            setExtractedTextSql = prepareStatement("set_extracted_text");
            getBinaryKeysSql = prepareStatement("get_binary_keys");
        } catch (SQLException e) {
            throw new BinaryStoreException(e);
        }
    }

    protected PreparedStatement prepareStatement( String statementKey ) throws SQLException {
        String statementString = statements.getProperty(statementKey);
        statementString = StringUtil.createString(statementString, tableName);
        LOGGER.trace("Preparing statement: {0}", statementString);
        return connection.prepareStatement(statementString);
    }

    protected Type determineType() throws BinaryStoreException {
        try {
            String name = connection.getMetaData().getDatabaseProductName().toLowerCase();
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
            }
            return Type.UNKNOWN;
        } catch (SQLException e) {
            throw new BinaryStoreException(e);
        }
    }

    /**
     * Closes connection with database.
     */
    public void disconnect() {
        if (connection != null) {
            boolean failed = false;
            try {
                if (addContentSql != null) addContentSql.close();
                if (getUsedContentSql != null) getUsedContentSql.close();
                if (getUnusedContentSql != null) getUnusedContentSql.close();
                if (markUnusedSql != null) markUnusedSql.close();
                if (markUsedSql != null) markUsedSql.close();
                if (removedExpiredSql != null) removedExpiredSql.close();
                if (getMimeType != null) getMimeType.close();
                if (setMimeType != null) setMimeType.close();
                if (getExtractedTextSql != null) getExtractedTextSql.close();
                if (setExtractedTextSql != null) setExtractedTextSql.close();
                if (getBinaryKeysSql != null) getBinaryKeysSql.close();
            } catch (SQLException e) {
                failed = true;
                throw new RuntimeException(e);
            } finally {
                try {
                    connection.close();
                } catch (SQLException e) {
                    if (!failed) throw new RuntimeException(e);
                } finally {
                    addContentSql = null;
                    getUsedContentSql = null;
                    getUnusedContentSql = null;
                    markUnusedSql = null;
                    markUsedSql = null;
                    removedExpiredSql = null;
                    getMimeType = null;
                    setMimeType = null;
                    getExtractedTextSql = null;
                    setExtractedTextSql = null;
                    getBinaryKeysSql = null;
                }
            }
        }
    }

    /**
     * The connection that this object is using.
     * 
     * @return the connection; never null
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Shows type of this database.
     * 
     * @return database type identifier.
     */
    public Type getDatabaseType() {
        return databaseType;
    }

    /**
     * Current time.
     * 
     * @return current time in milliseconds
     */
    private long now() {
        return new java.util.Date().getTime();
    }

    /**
     * Create statement for store content.
     * 
     * @param key unique content identifier
     * @param stream content to store
     * @return SQL statement.
     * @throws BinaryStoreException
     */
    public PreparedStatement insertContentSQL( BinaryKey key,
                                               InputStream stream ) throws BinaryStoreException {
        try {
            addContentSql.setString(1, key.toString());
            addContentSql.setTimestamp(2, new java.sql.Timestamp(now()));
            addContentSql.setBinaryStream(3, stream);
            return addContentSql;
        } catch (SQLException e) {
            throw new BinaryStoreException(e);
        }
    }

    /**
     * Generates SQL statement for content retrieve.
     * 
     * @param key content id
     * @param inUse true if the binary given by the key is expected to be still be in use, or false if the binary can be no longer
     *        used
     * @return executable SQL statement
     * @throws BinaryStoreException
     */
    public PreparedStatement retrieveContentSQL( BinaryKey key,
                                                 boolean inUse ) throws BinaryStoreException {
        try {
            PreparedStatement sql = inUse ? getUsedContentSql : getUnusedContentSql;
            sql.setString(1, key.toString());
            return sql;
        } catch (SQLException e) {
            throw new BinaryStoreException(e);
        }
    }

    /**
     * Generates SQL statement which marks content as not used.
     * 
     * @param key the content id.
     * @return SQL statement.
     * @throws BinaryStoreException
     */
    public PreparedStatement markUnusedSQL( BinaryKey key ) throws BinaryStoreException {
        try {
            markUnusedSql.setTimestamp(1, new java.sql.Timestamp(now()));
            markUnusedSql.setString(2, key.toString());
            return markUnusedSql;
        } catch (SQLException e) {
            throw new BinaryStoreException(e);
        }
    }

    /**
     * Generates SQL statement which marks content as used.
     * 
     * @param key the content id.
     * @return SQL statement.
     * @throws BinaryStoreException
     */
    public PreparedStatement restoreContentSQL( BinaryKey key ) throws BinaryStoreException {
        try {
            markUsedSql.setString(1, key.toString());
            return markUsedSql;
        } catch (SQLException e) {
            throw new BinaryStoreException(e);
        }
    }

    /**
     * Generates SQL statement which removes expired content.
     * 
     * @param deadline expire time
     * @return SQL statement.
     * @throws BinaryStoreException
     */
    public PreparedStatement removeExpiredContentSQL( long deadline ) throws BinaryStoreException {
        try {
            removedExpiredSql.setTimestamp(1, new java.sql.Timestamp(deadline));
            return removedExpiredSql;
        } catch (SQLException e) {
            throw new BinaryStoreException(e);
        }
    }

    /**
     * Generates SQL statement for mime type retrieve.
     * 
     * @param key content id
     * @return SQL statement.
     * @throws BinaryStoreException
     */
    public PreparedStatement retrieveMimeTypeSQL( BinaryKey key ) throws BinaryStoreException {
        try {
            getMimeType.setString(1, key.toString());
            return getMimeType;
        } catch (SQLException e) {
            throw new BinaryStoreException(e);
        }
    }

    /**
     * Generates SQL statement which modifies mime type value.
     * 
     * @param key content id
     * @param mimeType the new value for mime type
     * @return SQL statement
     * @throws BinaryStoreException
     */
    public PreparedStatement updateMimeTypeSQL( BinaryKey key,
                                                String mimeType ) throws BinaryStoreException {
        try {
            setMimeType.setString(1, mimeType);
            setMimeType.setString(2, key.toString());
            return setMimeType;
        } catch (SQLException e) {
            throw new BinaryStoreException(e);
        }
    }

    /**
     * Generate SQL statement which returns extracted text.
     * 
     * @param key content id
     * @return SQL statement
     * @throws BinaryStoreException
     */
    public PreparedStatement retrieveExtTextSQL( BinaryKey key ) throws BinaryStoreException {
        try {
            getExtractedTextSql.setString(1, key.toString());
            return getExtractedTextSql;
        } catch (SQLException e) {
            throw new BinaryStoreException(e);
        }
    }

    /**
     * Generates SQL statement which updates extracted text field.
     * 
     * @param key content id
     * @param text new value for the extracted text
     * @return SQL statement
     * @throws BinaryStoreException
     */
    public PreparedStatement updateExtTextSQL( BinaryKey key,
                                               String text ) throws BinaryStoreException {
        try {
            setExtractedTextSql.setString(1, text);
            setExtractedTextSql.setString(2, key.toString());
            return setExtractedTextSql;
        } catch (SQLException e) {
            throw new BinaryStoreException(e);
        }
    }

    /**
     * Generates SQL statement for retrieving the binary keys in the store.
     * 
     * @param keys the container into which the keys should be placed
     * @return executable SQL statement
     * @throws BinaryStoreException
     */
    public PreparedStatement retrieveBinaryKeys( Set<BinaryKey> keys ) throws BinaryStoreException {
        return getBinaryKeysSql;
    }

    /**
     * Executes specifies statement.
     * 
     * @param sql the statement to execute
     * @throws BinaryStoreException
     */
    public static void execute( PreparedStatement sql ) throws BinaryStoreException {
        try {
            LOGGER.trace("Running statement: {0}", sql);
            sql.execute();
        } catch (SQLException e) {
            throw new BinaryStoreException(e);
        }
    }

    /**
     * Runs SQL statement
     * 
     * @param sql SQL statement
     * @return result of statement execution
     * @throws BinaryStoreException
     */
    public static ResultSet executeQuery( PreparedStatement sql ) throws BinaryStoreException {
        try {
            LOGGER.trace("Running statement: {0}", sql);
            return sql.executeQuery();
        } catch (SQLException e) {
            throw new BinaryStoreException(e);
        }
    }

    /**
     * Executes specifies update statement.
     * 
     * @param sql the statement to execute
     * @throws BinaryStoreException
     */
    public static void executeUpdate( PreparedStatement sql ) throws BinaryStoreException {
        try {
            LOGGER.trace("Running statement: {0}", sql);
            sql.executeUpdate();
        } catch (SQLException e) {
            throw new BinaryStoreException(e);
        }
    }

    /**
     * Provides access to query data
     * 
     * @param rs retrieved single value
     * @return result as input stream.
     * @throws BinaryStoreException
     */
    public static InputStream asStream( ResultSet rs ) throws BinaryStoreException {
        boolean error = false;
        try {
            boolean hasRaw = rs.first();
            if (!hasRaw) {
                return null;
            }
            return rs.getBinaryStream(1);
        } catch (SQLException e) {
            error = true;
            throw new BinaryStoreException(e);
        } catch (RuntimeException e) {
            error = true;
            throw e;
        } finally {
            // Always close the result set ...
            try {
                rs.close();
            } catch (SQLException e) {
                if (!error) throw new BinaryStoreException(e);
            }
        }
    }

    /**
     * Provides access to query data
     * 
     * @param rs retrieved query result
     * @return result as string.
     * @throws BinaryStoreException
     */
    public static String asString( ResultSet rs ) throws BinaryStoreException {
        boolean error = false;
        try {
            boolean hasRaw = rs.first();
            if (!hasRaw) {
                return null;
            }
            return rs.getString(1);
        } catch (SQLException e) {
            error = true;
            throw new BinaryStoreException(e);
        } catch (RuntimeException e) {
            error = true;
            throw e;
        } finally {
            // Always close the result set ...
            try {
                rs.close();
            } catch (SQLException e) {
                if (!error) throw new BinaryStoreException(e);
            }
        }
    }

    /**
     * Provides access to query data
     * 
     * @param rs retrieved query result
     * @return result as string.
     * @throws BinaryStoreException
     */
    public static List<String> asStringList( ResultSet rs ) throws BinaryStoreException {
        boolean error = false;
        List<String> result = new ArrayList<String>();
        try {
            while (rs.next()) {
                result.add(rs.getString(1));
            }
        } catch (SQLException e) {
            error = true;
            throw new BinaryStoreException(e);
        } catch (RuntimeException e) {
            error = true;
            throw e;
        } finally {
            // Always close the result set ...
            try {
                rs.close();
            } catch (SQLException e) {
                if (!error) throw new BinaryStoreException(e);
            }
        }
        return result;
    }

}
