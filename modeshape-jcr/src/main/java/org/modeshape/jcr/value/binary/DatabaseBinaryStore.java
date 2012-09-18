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
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;

/**
 * A {@link BinaryStore} implementation that uses a database for persisting binary values.
 */
@ThreadSafe
public class DatabaseBinaryStore extends AbstractBinaryStore {
    private static final boolean ALIVE = true;
    private static final boolean UNUSED = false;

    private FileSystemBinaryStore cache;

    /** JDBC utility for working with the database. */
    private Database database;

    // JDBC params
    private final String driverClass;
    private final String connectionURL;
    private final String username;
    private final String password;
    private final String datasourceJNDILocation;

    /**
     * Create new store.
     * 
     * @param driverClass JDBC driver class name
     * @param connectionURL database location
     * @param username database user name
     * @param password database password
     */
    public DatabaseBinaryStore( String driverClass,
                                String connectionURL,
                                String username,
                                String password ) {
        this.driverClass = driverClass;
        this.connectionURL = connectionURL;
        this.username = username;
        this.password = password;
        this.datasourceJNDILocation = null;
        this.cache = TransientBinaryStore.get();
    }

    /**
     * Create new store that uses the JDBC DataSource in the given JNDI location.
     * 
     * @param datasourceJNDILocation the JNDI name of the JDBC Data Source that should be used, or null
     */
    public DatabaseBinaryStore( String datasourceJNDILocation ) {
        this.driverClass = null;
        this.connectionURL = null;
        this.username = null;
        this.password = null;
        this.datasourceJNDILocation = datasourceJNDILocation;
        this.cache = TransientBinaryStore.get();
    }

    @Override
    public BinaryValue storeValue( InputStream stream ) throws BinaryStoreException {
        // store into temporary file system store and get SHA-1
        BinaryValue temp = cache.storeValue(stream);
        try {
            // prepare new binary key based on SHA-1
            BinaryKey key = new BinaryKey(temp.getKey().toString());

            // check for duplicate content
            if (this.contentExists(key, ALIVE)) {
                return new StoredBinaryValue(this, key, temp.getSize());
            }

            // check unused content
            if (this.contentExists(key, UNUSED)) {
                PreparedStatement sql = database.restoreContentSQL(key);
                Database.execute(sql);
                return new StoredBinaryValue(this, key, temp.getSize());
            }

            // store content
            try {
                PreparedStatement sql = database.insertContentSQL(key, temp.getStream());
                Database.execute(sql);

                return new StoredBinaryValue(this, key, temp.getSize());
            } catch (Exception e) {
                throw new BinaryStoreException(e);
            }
        } finally {
            // remove content from temp store
            cache.markAsUnused(temp.getKey());
        }
    }

    @Override
    public InputStream getInputStream( BinaryKey key ) throws BinaryStoreException {
        ResultSet rs = Database.executeQuery(database.retrieveContentSQL(key, true));
        InputStream inputStream = Database.asStream(rs);
        if (inputStream == null) {
            try {
                throw new BinaryStoreException(JcrI18n.unableToFindBinaryValue.text(key, database.connection.getCatalog()));
            } catch (SQLException e) {
                logger.debug(e, "Unable to retrieve db information");
            }
        }
        return inputStream;
    }

    @Override
    public void markAsUnused( Iterable<BinaryKey> keys ) throws BinaryStoreException {
        for (BinaryKey key : keys) {
            PreparedStatement sql = database.markUnusedSQL(key);
            Database.executeUpdate(sql);
        }
    }

    @Override
    public void removeValuesUnusedLongerThan( long minimumAge,
                                              TimeUnit unit ) throws BinaryStoreException {
        // compute usage deadline (in past)
        long deadline = now() - unit.toMillis(minimumAge);
        PreparedStatement sql = database.removeExpiredContentSQL(deadline);
        Database.execute(sql);
    }

    @Override
    protected String getStoredMimeType( BinaryValue source ) throws BinaryStoreException {
        checkContentExists(source);
        ResultSet rs = Database.executeQuery(database.retrieveMimeTypeSQL(source.getKey()));
        return Database.asString(rs);
    }

    private void checkContentExists( BinaryValue source ) throws BinaryStoreException {
        if (!contentExists(source.getKey(), true)) {
            try {
                throw new BinaryStoreException(JcrI18n.unableToFindBinaryValue.text(source.getKey(),
                                                                                    database.connection.getCatalog()));
            } catch (SQLException e) {
                logger.debug("Cannot get catalog information", e);
            }
        }
    }

    @Override
    protected void storeMimeType( BinaryValue source,
                                  String mimeType ) throws BinaryStoreException {
        PreparedStatement sql = database.updateMimeTypeSQL(source.getKey(), mimeType);
        Database.executeUpdate(sql);
    }

    @Override
    public String getExtractedText( BinaryValue source ) throws BinaryStoreException {
        checkContentExists(source);
        ResultSet rs = Database.executeQuery(database.retrieveExtTextSQL(source.getKey()));
        return Database.asString(rs);
    }

    @Override
    public void storeExtractedText( BinaryValue source,
                                    String extractedText ) throws BinaryStoreException {
        PreparedStatement sql = database.updateExtTextSQL(source.getKey(), extractedText);
        Database.executeUpdate(sql);
    }

    @Override
    public void start() {
        super.start();
        try {
            Connection connection = datasourceJNDILocation != null ? DatabaseBinaryStore.connect(datasourceJNDILocation) : DatabaseBinaryStore.connect(driverClass,
                                                                                                                                                       connectionURL,
                                                                                                                                                       username,
                                                                                                                                                       password);

            // TODO: here we are making decision which kind of database we will talk to.
            // Right now, we just have one kind of utility, and no specializations for specific databases
            // DatabaseMetaData metaData = connection.getMetaData();
            database = new Database(connection);

            if (!database.tableExists()) {
                database.createTable();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected Database doCreateDatabase( Connection connection ) throws BinaryStoreException {
        return new Database(connection);
    }

    @Override
    public Iterable<BinaryKey> getAllBinaryKeys() throws BinaryStoreException {
        Set<BinaryKey> keys = new HashSet<BinaryKey>();
        try {
            PreparedStatement sql = database.retrieveBinaryKeys(keys);
            List<String> keysString = Database.asStringList(Database.executeQuery(sql));

            Set<BinaryKey> binaryKeys = new HashSet<BinaryKey>(keysString.size());
            for (String keyString : keysString) {
                binaryKeys.add(new BinaryKey(keyString));
            }
            return binaryKeys;
        } catch (Exception e) {
            throw new BinaryStoreException(e);
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();
        if (database != null) {
            database.disconnect();
        }
    }

    /**
     * Test content for existence.
     * 
     * @param key content identifier
     * @param alive true inside used content and false for checking within content marked as unused.
     * @return true if content found
     * @throws BinaryStoreException
     */
    private boolean contentExists( BinaryKey key,
                                   boolean alive ) throws BinaryStoreException {
        try {
            ResultSet rs = Database.executeQuery(database.retrieveContentSQL(key, alive));
            return rs.next();
        } catch (SQLException e) {
            throw new BinaryStoreException(e);
        }
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
     * Creates connection with a database using data source registered via JNDI.
     * 
     * @param jndiName the JNDI location of the data source
     * @return connection to database
     * @throws BinaryStoreException
     */
    private static Connection connect( String jndiName ) throws BinaryStoreException {
        DataSource dataSource = null;
        try {
            InitialContext context = new InitialContext();
            dataSource = (DataSource)context.lookup(jndiName);
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
    private static Connection connect( String driverClass,
                                       String connectionURL,
                                       String username,
                                       String password ) throws BinaryStoreException {
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

}
