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
import org.modeshape.jcr.value.BinaryKey;

/**
 * Helper class for manipulation with database.
 *
 * @author kulikov
 */
public class Database {
    private Connection connection;

    public Database(Connection connection) {
        this.connection = connection;
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
            PreparedStatement sql = connection.prepareStatement(
                    "insert into content_store(cid, usage_time, payload, usage) values(?,?,?,1)");
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
            PreparedStatement sql = connection.prepareStatement(
                    "select payload from content_store where cid =?");
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
            PreparedStatement sql = connection.prepareStatement(
                    "update content_store set usage=0, timestamp=? where cid =?");
            sql.setTimestamp(1, new java.sql.Timestamp(now()));
            sql.setString(2, key.toString());
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
            PreparedStatement sql = connection.prepareStatement(
                    "delete from content_store where usage_time < ?");
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
            PreparedStatement sql = connection.prepareStatement(
                    "select mime_type from content_store where cid = ?");
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
            PreparedStatement sql = connection.prepareStatement(
                    "update content_store set mime_type= ? where cid = ?");
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
            PreparedStatement sql = connection.prepareStatement(
                    "select ext_text from content_store where cid = ?");
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
            PreparedStatement sql = connection.prepareStatement(
                    "update content_store set ext_text= ? where cid = ?");
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
                throw new BinaryStoreException("The content has been deleted");
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
                throw new BinaryStoreException("The content has been deleted");
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
            PreparedStatement sql = connection.prepareStatement("select * from content_store");
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
            PreparedStatement sql = connection.prepareStatement("create table content_store ("
                    + "cid varchar(255) not null,"
                    + "mime_type varchar(255),"
                    + "ext_text varchar(1000),"
                    + "usage integer,"
                    + "usage_time timestamp,"
                    + "payload " + blob(connection, 0) + ","
                    + "primary key(cid))");
            Database.execute(sql);
        } catch (Exception e) {
            throw new BinaryStoreException(e);
        }
    }

    /**
     * Database specific BLOB column type.
     *
     * @param connection
     * @param size
     * @return
     * @throws BinaryStoreException
     */
    private static String blob(Connection connection, int size) throws BinaryStoreException {
        try {
            String name = connection.getMetaData().getDatabaseProductName().toLowerCase();
            if (name.toLowerCase().contains("mysql")) {
                return String.format("LONGBLOB", size);
            } else if (name.contains("postgres")) {
                return "oid";
            } else if (name.contains("derby")) {
                return String.format("blolb", size);
            } else if (name.contains("hsql") || name.toLowerCase().contains("hypersonic")) {
            } else if (name.contains("h2")) {
                return "blob";
            } else if (name.contains("sqlite")) {
                return "blob";
            } else if (name.contains("db2")) {
                return "blob";
            } else if (name.contains("informix")) {
                return "blob";
            } else if (name.contains("interbase")) {
                return "blob subtype 0";
            } else if (name.contains("firebird")) {
                return "blob subtype 0";
            } else if (name.contains("sqlserver") || name.toLowerCase().contains("microsoft")) {
                return "blob";
            } else if (name.contains("access")) {
            } else if (name.contains("oracle")) {
                return "blob";
            } else if (name.contains("adaptive")) {
            }
            return "";
        } catch (SQLException e) {
            throw new BinaryStoreException(e);
        }
    }

}
