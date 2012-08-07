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

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.value.BinaryKey;

/**
 * Helper class for manipulation with database.
 *
 * @author kulikov
 */
public class Database {
    //connection to a database
    private Connection connection;
    //table name prefix
    private String prefix;

    private Type databaseType;

    //SQLBuilder
    private SQLBuilder sqlBuilder = new SQLBuilder();
    private SQLType sqlType = new SQLType();

    public enum Type {
        MYSQL, POSTGRES, DERBY, HSQL, H2, SQLITE, DB2, DB2_390,INFORMIX,
        INTERBASE, FIREBIRD, SQL_SERVER, ACCESS, ORACLE, SYBASE, UNKNOWN;
    }

    /**
     * Creates new instance of the database.
     *
     * @param connection connection to a database
     */
    public Database(Connection connection) throws BinaryStoreException {
        this.connection = connection;
        databaseType = determineType();
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
     * Modifies database type.
     *
     * @param databaseType new database type identifier.
     */
    protected void setDatabaseType(Type databaseType) {
        this.databaseType = databaseType;
    }

    /**
     * Configures table name prefix.
     * 
     * @param prefix table name prefix.
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Convergence table name including prefix if configured.
     *
     * @return table name.
     */
    private String tableName() {
        return StringUtil.isBlank(prefix) ? "CONTENT_STORE" : prefix + "_CONTENT_STORE";
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
    public PreparedStatement insertContentSQL(BinaryKey key, InputStream stream) throws BinaryStoreException {
        try {
            PreparedStatement sql = sqlBuilder
                    .insert().into(tableName())
                    .columns("cid", "usage_time", "payload", "usage")
                    .values("?","?","?", "1")
                    .build();
            sql.setString(1, key.toString());
            sql.setTimestamp(2, new java.sql.Timestamp(now()));
            sql.setBinaryStream(3, stream);
            return sql;
        } catch (SQLException e) {
            throw new BinaryStoreException(e);
        }
    }

    /**
     * Generates SQL statement for content retrieve.
     *
     * @param key content id
     * @return executable SQL statement
     * @throws BinaryStoreException
     */
    public PreparedStatement retrieveContentSQL(BinaryKey key) throws BinaryStoreException {
        try {
            PreparedStatement sql = sqlBuilder
                    .select().columns("payload").from(tableName())
                    .where().condition("cid", sqlType.integer(), "=", "?")
                    .and().condition("usage", sqlType.integer(), "=", "1")
                    .build();
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
    public PreparedStatement markUnusedSQL(BinaryKey key) throws BinaryStoreException {
        try {
            PreparedStatement sql = sqlBuilder
                    .update(tableName())
                    .set("usage", "?")
                    .set("usage_time", "?")
                    .where().condition("cid", sqlType.integer(), "=", "?")
                    .build();
            sql.setInt(1, 0);
            sql.setTimestamp(2, new java.sql.Timestamp(now()));
            sql.setString(3, key.toString());
            return sql;
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
    public PreparedStatement removeExpiredContentSQL(long deadline) throws BinaryStoreException {
        try {
            PreparedStatement sql = sqlBuilder
                    .delete().from(tableName())
                    .where().condition("usage_time", sqlType.timestamp(), "<", "?")
                    .build();
            sql.setTimestamp(1, new java.sql.Timestamp(deadline));
            return sql;
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
    public PreparedStatement retrieveMimeTypeSQL(BinaryKey key) throws BinaryStoreException {
        try {
            PreparedStatement sql = sqlBuilder
                    .select().columns("mime_type").from(tableName())
                    .where().condition("cid", sqlType.integer(), "=", "?").build();
            sql.setString(1, key.toString());
            return sql;
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
    public PreparedStatement updateMimeTypeSQL(BinaryKey key, String mimeType) throws BinaryStoreException {
        try {
            PreparedStatement sql = sqlBuilder
                    .update(tableName())
                    .set("mime_type", "?")
                    .where().condition("cid", sqlType.integer(), "=", "?").build();
            sql.setString(1, mimeType);
            sql.setString(2, key.toString());
            return sql;
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
    public PreparedStatement retrieveExtTextSQL(BinaryKey key) throws BinaryStoreException {
        try {
            PreparedStatement sql = sqlBuilder
                    .select().columns("ext_text").from(tableName())
                    .where().condition("cid", sqlType.integer(), "=", "?").build();
            sql.setString(1, key.toString());
            return sql;
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
    public PreparedStatement updateExtTextSQL(BinaryKey key, String text) throws BinaryStoreException {
        try {
            PreparedStatement sql = sqlBuilder
                    .update(tableName())
                    .set("ext_text", "?")
                    .where().condition("cid", sqlType.integer(), "=", "?").build();
            sql.setString(1, text);
            sql.setString(2, key.toString());
            return sql;
        } catch (SQLException e) {
            throw new BinaryStoreException(e);
        }
    }

    /**
     * Executes specifies statement.
     *
     * @param sql the statement to execute
     * @throws BinaryStoreException
     */
    public static void execute(PreparedStatement sql) throws BinaryStoreException {
        try {
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
    public static ResultSet executeQuery(PreparedStatement sql) throws BinaryStoreException {
        try {
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
    public static void executeUpdate(PreparedStatement sql) throws BinaryStoreException {
        try {
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
    public static InputStream asStream(ResultSet rs) throws BinaryStoreException {
        try {
            boolean hasRaw = rs.first();
            if (!hasRaw) {
                return null;
            }
            return rs.getBinaryStream(1);
        } catch (SQLException e) {
            throw new BinaryStoreException(e);
        }
    }

    /**
     * Provides access to query data
     *
     * @param rs retrieved query result
     * @return result as string.
     * @throws BinaryStoreException
     */
    public static String asString(ResultSet rs) throws BinaryStoreException {
        try {
            boolean hasRaw = rs.first();
            if (!hasRaw) {
                return null;
            }
            return rs.getString(1);
        } catch (SQLException e) {
            throw new BinaryStoreException(e);
        }
    }

    /**
     * Checks database for CONTENT_STORE table
     *
     * @return true if table exists
     * @throws BinaryStoreException
     */
    public boolean tableExists() throws BinaryStoreException {
        try {
            PreparedStatement sql = connection.prepareStatement("select count(*) from " + tableName());
            Database.execute(sql);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Creates table for storage.
     *
     * @throws BinaryStoreException
     */
    public void createTable() throws BinaryStoreException {
        try {
            PreparedStatement sql = connection.prepareStatement("create table " + tableName() + " ("
                    + "cid " + sqlType.varchar(255) + " not null,"
                    + "mime_type " + sqlType.varchar(255)+ ", "
                    + "ext_text " + sqlType.varchar(1000) + ","
                    + "usage " + sqlType.integer() +","
                    + "usage_time " + sqlType.timestamp() +","
                    + "payload " + sqlType.blob() + ","
                    + "primary key(cid))");
            Database.execute(sql);
        } catch (Exception e) {
            throw new BinaryStoreException(e);
        }
    }

    private Type determineType() throws BinaryStoreException {
        if (connection == null) {
            return Type.UNKNOWN;
        }
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
                return Type.SQL_SERVER;
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
            try {
                connection.close();
            } catch (SQLException e) {
            }
        }
    }

    /**
     * Database specific SQL types
     */
    private class SQLType {

        /**
         * Integer type.
         * @return integer type descriptor.
         */
        public String integer() {
            return "INTEGER";
        }

        /**
         * Timestamp type.
         * @return timestamp type descriptor.
         */
        public String timestamp() {
            return "TIMESTAMP";
        }

        /**
         * BLOB type.
         * @return BLOB type descriptor.
         */
        private String blob() {
            switch (databaseType) {
                case SQL_SERVER:
                case SYBASE:
                    return "IMAGE";
                case HSQL:
                    return "OBJECT";
                default:
                    return "BLOB";
            }
        }

        /**
         * VARCHAR type.
         *
         * @param size size in characters.
         * @return VACRCHAR type descriptor.
         */
        private String varchar(int size) {
            switch (databaseType) {
                case ORACLE:
                    return "VARCHAR2(" + size + " char)";
                default:
                    return "VARCHAR(" + size + ")";
            }
        }
    }

    /**
     * Database specific SQL query builder.
     */
    public class SQLBuilder {
        private boolean set = false;

        //inner buffer for building sql string
        private StringBuilder sql;

        /**
         * Generates prepared statement.
         *
         * @return prepared statement
         * @throws SQLException
         */
        public PreparedStatement build() throws SQLException {
            return connection.prepareStatement(sql.toString());
        }

        /**
         * Shows built statement as text.
         *
         * @return build statement as text.
         */
        public String getSQL() {
            return sql.toString();
        }

        /**
         * Appends 'insert' keyword to the statement.
         *
         * @return this builder instance.
         */
        public SQLBuilder insert() {
            set = false;
            sql = new StringBuilder();
            sql.append("INSERT ");
            return this;
        }

        /**
         * Appends 'select' keyword to the statement.
         *
         * @return this builder instance.
         */
        public SQLBuilder select() {
            set = false;
            sql = new StringBuilder();
            sql.append("SELECT ");
            return this;
        }

        /**
         * Appends 'delete' keyword to the statement.
         *
         * @return this builder instance.
         */
        public SQLBuilder delete() {
            set = false;
            sql = new StringBuilder();
            sql.append("DELETE ");
            return this;
        }

        /**
         * Appends 'update' keyword with table name to the statement.
         *
         * @param tableName the name of the table to update
         * @return this builder instance.
         */
        public SQLBuilder update(String tableName) {
            set = false;
            sql = new StringBuilder();
            sql.append("UPDATE ");
            sql.append(tableName);
            return this;
        }

        /**
         * Appends 'set' part.
         * 
         * @param col column name to update
         * @param val new value
         * @return this builder instance
         */
        public SQLBuilder set(String col, String val) {
            if (!set) {
                sql.append(" SET ");
                set = true;
            } else {
                sql.append(", ");
            }
            sql.append(col);
            sql.append("=");
            sql.append(val);
            return this;
        }

        /**
         * Appends 'into 'keyword and open bracket to the statement.
         *
         * @return this builder instance.
         */
        public SQLBuilder into(String tableName) {
            sql.append("INTO ");
            sql.append(tableName);
            sql.append(" (");
            return this;
        }

        /**
         * Appends comma separated list of specified column names.
         *
         * @param  columns list of column names
         * @return this builder instance.
         */
        public SQLBuilder columns(String... columns) {
            sql.append(columns[0]);

            for (int i = 1; i < columns.length; i++) {
                sql.append(", ");
                sql.append(columns[i]);
            }

            return this;
        }

        /**
         * Appends closed bracket and 'value(...)' of sql statement.
         *
         * @param  columns list of values
         * @return this builder instance.
         */

        public SQLBuilder values(String... columns) {
            sql.append(") VALUES (");
            sql.append(columns[0]);

            for (int i = 1; i < columns.length; i++) {
                sql.append(", ");
                sql.append(columns[i]);
            }

            sql.append(")");
            return this;
        }

        /**
         * Appends 'from' keyword.
         *
         * @return this builder instance.
         */
        public SQLBuilder from(String tableName) {
            sql.append(" FROM ");
            sql.append(tableName);
            return this;
        }

        /**
         * Appends 'where' keyword.
         *
         * @return this builder instance.
         */
        public SQLBuilder where() {
            sql.append(" WHERE ");
            return this;
        }

        /**
         * Appends 'and' keyword.
         *
         * @return this builder instance.
         */
        public SQLBuilder and() {
            sql.append(" AND ");
            return this;
        }

        /**
         * Builds database specific condition statement.
         *
         * @param column  column name used in left hand side of condition
         * @param colType type of the column
         * @param sign sign between lhs and rhs
         * @param value right hand side of the condition
         * @return this builder instance.
         */
        public SQLBuilder condition(String column, String colType, String sign, String value) {
            sql.append(column);
            sql.append(sign);
            switch (databaseType) {
                case SYBASE :
                    sql.append("convert(");
                    sql.append(colType);
                    sql.append(",");
                    sql.append(value);
                    sql.append(")");
                    break;
                case POSTGRES :
                    sql.append("cast(");
                    sql.append(value);
                    sql.append(" as ");
                    sql.append(colType);
                    sql.append(")");
                    break;
                default :
                    sql.append(value);
            }
            return this;
        }

    }

}
