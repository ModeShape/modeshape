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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.util.SizeMeasuringInputStream;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;

/**
 * A {@link BinaryStore} implementation that uses a database for persisting binary values.
 */
@ThreadSafe
public class DatabaseBinaryStore extends AbstractBinaryStore {
    private FileSystemBinaryStore cache;

    /** JDBC utility for working with the database. */
    private Database database;

    //JDBC params
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
    public DatabaseBinaryStore(String driverClass, String connectionURL, 
            String username, String password) {
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
    public DatabaseBinaryStore(String datasourceJNDILocation) {
        this.driverClass = null;
        this.connectionURL = null;
        this.username = null;
        this.password = null;
        this.datasourceJNDILocation = datasourceJNDILocation;
        this.cache = TransientBinaryStore.get();
    }


    @Override
    public BinaryValue storeValue( InputStream stream ) throws BinaryStoreException {
        //store into temporary file system store and get SHA-1
        BinaryValue temp = cache.storeValue(stream);
        try {
            //prepare new binary key based on SHA-1
            BinaryKey key = new BinaryKey(temp.getKey().toString());

            //check for duplicate content
            InputStream content = this.getInputStream(key);
            if (content != null && getContentLength(content) > 0) {
                return new StoredBinaryValue(this, key, temp.getSize());
            }

            //store content
            try {
                PreparedStatement sql = database.insertContentSQL(key, temp.getStream());
                Database.execute(sql);

                return new StoredBinaryValue(this, key, temp.getSize());
            } catch (Exception e) {
                throw new BinaryStoreException(e);
            }
        } finally {
            //remove content from temp store
            cache.markAsUnused(temp.getKey());
        }
    }

    @Override
    public InputStream getInputStream( BinaryKey key ) throws BinaryStoreException {
        ResultSet rs = Database.executeQuery(database.retrieveContentSQL(key));
        if (rs == null) {
            throw new BinaryStoreException(JcrI18n.unableToFindBinaryValue.toString());
        }
        return Database.asStream(rs);
    }

    @Override
    public void markAsUnused( Iterable<BinaryKey> keys ) throws BinaryStoreException {
        for (BinaryKey key : keys) {
            PreparedStatement sql = database.markUnusedSQL(key);
            Database.executeUpdate(sql);
        }
    }

    @Override
    public void removeValuesUnusedLongerThan(long minimumAge,  TimeUnit unit) throws BinaryStoreException {
        //compute usage deadline (in past)
        long deadline = now() - unit.toMillis(minimumAge);
        PreparedStatement sql = database.removeExpiredContentSQL(deadline);
        Database.execute(sql);
    }

    @Override
    protected String getStoredMimeType( BinaryValue source ) throws BinaryStoreException {
        ResultSet rs = Database.executeQuery(database.retrieveMimeTypeSQL(source.getKey()));
        if (rs == null) {
            throw new BinaryStoreException(JcrI18n.unableToFindBinaryValue.toString());
        }
        return Database.asString(rs);
    }

    @Override
    protected void storeMimeType(BinaryValue source, String mimeType) throws BinaryStoreException {
        PreparedStatement sql = database.updateMimeTypeSQL(source.getKey(), mimeType);
        Database.executeUpdate(sql);
    }

    @Override
    public String getExtractedText( BinaryValue source ) throws BinaryStoreException {
        ResultSet rs = Database.executeQuery(database.retrieveExtTextSQL(source.getKey()));
        if (rs == null) {
            throw new BinaryStoreException(JcrI18n.unableToFindBinaryValue.toString());
        }
        return Database.asString(rs);
    }

    @Override
    public void storeExtractedText( BinaryValue source, String extractedText ) throws BinaryStoreException {
        PreparedStatement sql = database.updateExtTextSQL(source.getKey(), extractedText);
        Database.executeUpdate(sql);
    }

    @Override
    public void start() {
        super.start();
        try {
            Connection connection = datasourceJNDILocation != null ?
                DatabaseBinaryStore.connect(datasourceJNDILocation) :
                DatabaseBinaryStore.connect(driverClass, connectionURL, username, password);

            // TODO: here we are making decision which kind of database we will talk to.
            // Right now, we just have one kind of utility, and no specializations for specific databases
            //DatabaseMetaData metaData = connection.getMetaData();
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
    public void shutdown() {
        super.shutdown();
        if (database != null) {
            database.disconnect();
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
     * Determine length of the content.
     * 
     * @param stream content 
     * @return the length of the content in bytes.
     * @throws BinaryStoreException 
     */
    private long getContentLength(InputStream stream) throws BinaryStoreException {
        AtomicLong length = new AtomicLong();
        SizeMeasuringInputStream ms = new SizeMeasuringInputStream(stream, length);
        try {
            int b = 0;
            while (b != -1) {
                b = ms.read();
            }
            return length.longValue();
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

}
