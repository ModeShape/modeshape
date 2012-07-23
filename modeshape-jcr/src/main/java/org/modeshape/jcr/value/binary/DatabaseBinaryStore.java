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

import java.io.File;
import java.io.InputStream;
import java.sql.*;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.modeshape.common.SystemFailureException;
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
    private static final String JAVA_IO_TMPDIR = "java.io.tmpdir";
    private FileSystemBinaryStore cache;

    //JDBC Datasource
    private Database database;

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

        String path = System.getProperty(JAVA_IO_TMPDIR);
        if (path == null) {
            throw new SystemFailureException(JcrI18n.tempDirectorySystemPropertyMustBeSet.text(JAVA_IO_TMPDIR));
        }

        cache = new FileSystemBinaryStore(new File(path));
    }


    @Override
    public BinaryValue storeValue( InputStream stream ) throws BinaryStoreException {
        BinaryValue temp = cache.storeValue(stream);
        try {
            BinaryKey key = new BinaryKey(temp.getKey().toString());

            InputStream content = this.getInputStream(key);
            if (content != null && getContentLength(content) > 0) {
                return new StoredBinaryValue(this, key, temp.getSize());
            }

            try {
                PreparedStatement sql = database.insertContentSQL(key, temp.getStream());
                Database.execute(sql);

                return new StoredBinaryValue(this, key, temp.getSize());
            } catch (Exception e) {
                throw new BinaryStoreException(e);
            }
        } finally {
            cache.markAsUnused(temp.getKey());
        }
    }

    @Override
    public InputStream getInputStream( BinaryKey key ) throws BinaryStoreException {
        PreparedStatement sql = database.retrieveContentSQL(key);
        return Database.asStream(Database.executeQuery(sql));
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
        PreparedStatement sql = database.retrieveMimeTypeSQL(source.getKey());
        return Database.asString(Database.executeQuery(sql));
    }

    @Override
    protected void storeMimeType(BinaryValue source, String mimeType) throws BinaryStoreException {
        PreparedStatement sql = database.updateMimeTypeSQL(source.getKey(), mimeType);
        Database.executeUpdate(sql);
    }

    @Override
    public String getExtractedText( BinaryValue source ) throws BinaryStoreException {
        PreparedStatement sql = database.retrieveExtTextSQL(source.getKey());
        return Database.asString(Database.executeQuery(sql));
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

            DatabaseMetaData metaData = connection.getMetaData();
            //here we are making decision which database we will create
            database = new Database(connection);

            if (!database.tableExists()) {
                database.createTable();
            }
        } catch (Exception e) {
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
