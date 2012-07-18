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
import java.sql.*;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;

/**
 * A {@link BinaryStore} implementation that uses a database for persisting binary values.
 */
@ThreadSafe
public class DatabaseBinaryStore extends AbstractBinaryStore {

    //JDBC Datasource
    private Connection connection;

    //JDBC params
    private String driverClass;
    private String connectionURL;
    private String username;
    private String password;
    private String datasourceJNDILocation;

    /**
     * Create new store.
     *
     * @param driverClass JDBC driver class name
     * @param connectionURL database location
     * @param username database user name
     * @param password database password
     */
    public DatabaseBinaryStore(String driverClass, String connectionURL, 
            String username, String password, String datasourceJNDILocation) {
        this.driverClass = driverClass;
        this.connectionURL = connectionURL;
        this.username = username;
        this.password = password;
        this.datasourceJNDILocation = datasourceJNDILocation;
    }


    @Override
    public BinaryValue storeValue( InputStream stream ) throws BinaryStoreException {
        BinaryKey key = new BinaryKey(UUID.randomUUID().toString());

        PreparedStatement sql = insertContentSQL(key, stream);
        DatabaseBinaryStore.execute(sql);

        return new StoredBinaryValue(this, key, getContentLength(getInputStream(key)));
    }

    @Override
    public InputStream getInputStream( BinaryKey key ) throws BinaryStoreException {
        PreparedStatement sql = retrieveContentSQL(key);
        return asStream(DatabaseBinaryStore.executeQuery(sql));
    }

    @Override
    public void markAsUnused( Iterable<BinaryKey> keys ) throws BinaryStoreException {
        for (BinaryKey key : keys) {
            PreparedStatement sql = markUnusedSQL(key);
            DatabaseBinaryStore.executeUpdate(sql);
        }
    }

    @Override
    public void removeValuesUnusedLongerThan(long minimumAge,  TimeUnit unit) throws BinaryStoreException {
        //compute usage deadline (in past)
        long deadline = now() - unit.toMillis(minimumAge);
        PreparedStatement sql = removeExpiredContentSQL(deadline);
        DatabaseBinaryStore.execute(sql);
    }

    @Override
    protected String getStoredMimeType( BinaryValue source ) throws BinaryStoreException {
        PreparedStatement sql = retrieveMimeTypeSQL(source.getKey());
        return asString(DatabaseBinaryStore.executeQuery(sql));
    }

    @Override
    protected void storeMimeType(BinaryValue source, String mimeType) throws BinaryStoreException {
        PreparedStatement sql = updateMimeTypeSQL(source.getKey(), mimeType);
        DatabaseBinaryStore.executeUpdate(sql);
    }

    @Override
    public String getExtractedText( BinaryValue source ) throws BinaryStoreException {
        PreparedStatement sql = retrieveExtTextSQL(source.getKey());
        return asString(DatabaseBinaryStore.executeQuery(sql));
    }

    @Override
    public void storeExtractedText( BinaryValue source, String extractedText ) throws BinaryStoreException {
        PreparedStatement sql = updateExtTextSQL(source.getKey(), extractedText);
        DatabaseBinaryStore.executeUpdate(sql);
    }

    @Override
    public void start() {
        super.start();
        try {
            connection = datasourceJNDILocation != null ?
                DatabaseBinaryStore.connect(datasourceJNDILocation) :
                DatabaseBinaryStore.connect(driverClass, connectionURL, username, password);

            if (!tableExists()) {
                createTable();
            }
        } catch (BinaryStoreException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }

    /**
     * Current time.
     *
     * @return current time in milliseconds
     */
    private long now() {
        return new Date().getTime();
    }

    /**
     * Determine length of the content.
     * 
     * @param stream content 
     * @return the length of the content in bytes.
     * @throws BinaryStoreException 
     */
    private long getContentLength(InputStream stream) throws BinaryStoreException {
        try {
            int b = 0; int count = 0;
            while (b != -1) {
                b = stream.read();
                count++;
            }
            return count;
        } catch (Exception e) {
            throw new BinaryStoreException(e);
        }
    }

    /**
     * Create statement for store content.
     *
     * @param key unique content identifier
     * @param stream content to store
     * @return SQL statement.
     * @throws BinaryStoreException
     */
    private PreparedStatement insertContentSQL(BinaryKey key, InputStream stream) throws BinaryStoreException {
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
    private PreparedStatement retrieveContentSQL(BinaryKey key) throws BinaryStoreException {
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
    private PreparedStatement markUnusedSQL(BinaryKey key) throws BinaryStoreException {
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
    private PreparedStatement removeExpiredContentSQL(long deadline) throws BinaryStoreException {
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
    private PreparedStatement retrieveMimeTypeSQL(BinaryKey key) throws BinaryStoreException {
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
    private PreparedStatement updateMimeTypeSQL(BinaryKey key, String mimeType) throws BinaryStoreException {
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
    private PreparedStatement retrieveExtTextSQL(BinaryKey key) throws BinaryStoreException {
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
    private PreparedStatement updateExtTextSQL(BinaryKey key, String text) throws BinaryStoreException {
        try {
            PreparedStatement sql = connection.prepareStatement(
                    "update content_store set ext_text= ? where cid = ?");
            sql.setString(0, text);
            sql.setString(1, key.toString());
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
    private static InputStream asStream(ResultSet rs) throws BinaryStoreException {
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
    private static String asString(ResultSet rs) throws BinaryStoreException {
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
    private boolean tableExists() throws BinaryStoreException {
        PreparedStatement sql = null;

        try {
            sql = connection.prepareStatement("select * from content_store");
        } catch (SQLException e) {
            throw new BinaryStoreException(e);
        }

        try {
            DatabaseBinaryStore.execute(sql);
            return true;
        } catch (BinaryStoreException e) {
            return false;
        }
    }

    /**
     * Creates table for storage.
     *
     * @throws BinaryStoreException
     */
    private void createTable() throws BinaryStoreException {
        try {
            PreparedStatement sql = connection.prepareStatement("create table content_store ("
                    + "cid varchar(255) not null,"
                    + "mime_type varchar(255),"
                    + "ext_text varchar(1000),"
                    + "usage integer,"
                    + "usage_time timestamp,"
                    + "payload" + blob(connection, 0) + ","
                    + "primary key(cid))");
            DatabaseBinaryStore.executeQuery(sql);
        } catch (Exception e) {
            throw new BinaryStoreException(e);
        }
    }


    /**
     * Creates connection with a database using data source registered via JNDI.
     *
     * @param jndiName the JNDI location of the data source
     * @return connection to database
     * @throws BinaryStoreException
     */
    private static Connection connect(String jndiName) throws BinaryStoreException {
        DataSource dataSource = null;
        try {
            InitialContext context = new InitialContext();
            dataSource = (DataSource) context.lookup(jndiName);
        } catch (NamingException e) {
            throw new BinaryStoreException(e);
        }

        if (dataSource == null) {
            throw new BinaryStoreException("Datasource is not bound: " + jndiName);
        }

        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new BinaryStoreException(e);
        }
    }

    /**
     * Creates connection to a database using driver, location and credentials.
     *
     * @param driverClass driver's class name
     * @param connectionURL database location
     * @param username database user name
     * @param password user's password.
     * @return connection to a database.
     * @throws BinaryStoreException
     */
    private static Connection connect(String driverClass, String connectionURL,
            String username, String password) throws BinaryStoreException {
        try {
            Class.forName(driverClass);
        } catch (Exception e) {
            throw new BinaryStoreException(e);
        }

        try {
            return DriverManager.getConnection(connectionURL, username, password);
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
            String name = connection.getMetaData().getDatabaseProductName();
            if (name.toLowerCase().contains("mysql")) {
                return String.format("LONGBLOB", size);
            } else if (name.toLowerCase().contains("postgres")) {
                return "oid";
            } else if (name.toLowerCase().contains("derby")) {
                return String.format("blolb", size);
            } else if (name.toLowerCase().contains("hsql") || name.toLowerCase().contains("hypersonic")) {
            } else if (name.toLowerCase().contains("h2")) {
                return "blob";
            } else if (name.toLowerCase().contains("sqlite")) {
                return "blob";
            } else if (name.toLowerCase().contains("db2")) {
                return "blob";
            } else if (name.toLowerCase().contains("informix")) {
                return "blob";
            } else if (name.toLowerCase().contains("interbase")) {
                return "blob subtype 0";
            } else if (name.toLowerCase().contains("firebird")) {
                return "blob subtype 0";
            } else if (name.toLowerCase().contains("sqlserver") || name.toLowerCase().contains("microsoft")) {
                return "blob";
            } else if (name.toLowerCase().contains("access")) {
            } else if (name.toLowerCase().contains("oracle")) {
                return "blob";
            } else if (name.toLowerCase().contains("adaptive")) {
            }
            return "";
        } catch (SQLException e) {
            throw new BinaryStoreException(e);
        }
    }

}
