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
package org.modeshape.jcr.value.binary;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;

/**
 * A {@link BinaryStore} implementation that uses a database for persisting binary values.
 * <p>
 * This binary store implementation establishes a connection to the specified database and then attempts to determine which type
 * of database is being used. ModeShape is aware of the following database types:
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
 * This binary store implementation then uses DDL and DML statements to create the table(s) if not already existing and to perform
 * the various operations required of a binary store. ModeShape can use database-specific statements, although a default set of
 * SQL-99 statements are used as a fallback.
 * </p>
 * <p>
 * These statements are read from a property file named "<code>binary_store_{type}_database.properties</code>", where where "
 * <code>{type}</code>" is one of the above-mentioned database type strings. These properties files are expected to be found on
 * the classpath directly under "org/modeshape/jcr/database". If the corresponding file is not found on the classpath, then the "
 * <code>binary_store_default_database.properties</code>" file provided by ModeShape is used.
 * </p>
 * <p>
 * ModeShape provides out-of-the-box database-specific files for several of the DBMSes that are popular within the open source
 * community. The properties files for the other database types are not provided (though the ModeShape community will gladly
 * incorporate them if you wish to make them available to us); in such cases, simply copy one of the provided properties files
 * (e.g., "<code>binary_store_default_database.properties</code>" is often a good start) and customize it for your particular
 * DBMS, naming it according to the pattern described above and including it on the classpath.
 * </p>
 * <p>
 * Note that this mechanism can also be used to override the statements that ModeShape does provide out-of-the-box. In such cases,
 * be sure to place the file on the classpath before the ModeShape JARs so that your file will be discovered first.
 * </p>
 * <p>
 * The JDBC driver used needs to be at least JDBC 1.4 (JDK 6) compliant, because
 * {@link PreparedStatement#setBinaryStream(int parameterIndex, java.io.InputStream x)} is being used.
 * </p>
 */
@ThreadSafe
public class DatabaseBinaryStore extends AbstractBinaryStore {
    private static final boolean ALIVE = true;
    private static final boolean UNUSED = false;

    /**
     * JDBC params
     */
    private final String driverClass;
    private final String connectionURL;
    private final String username;
    private final String password;
    private final String datasourceJNDILocation;
    private DataSource dataSource;

    /**
     * A temporary fs-based store which stores binaries before they are persisted in the DB
     */
    private final FileSystemBinaryStore cache;

    /**
     * JDBC utility for working with the database.
     */
    private Database database;

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
    public void start() {
        super.start();

        if (!StringUtil.isBlank(datasourceJNDILocation)) {
            lookupDataSource();
        } else {
            lookupDriver();
        }

        database();
    }

    protected Database database() {
        if (this.database == null) {
            Connection connection = newConnection();
            try {
                this.database = new Database(connection);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            } finally {
                Database.tryToClose(connection);
            }
        }
        return database;
    }

    @Override
    public BinaryValue storeValue( InputStream stream, final boolean markAsUnused ) throws BinaryStoreException {
        // store into temporary file system store and get SHA-1
        final BinaryValue temp = cache.storeValue(stream, markAsUnused);
        try {
            return dbCall(new DBCallable<BinaryValue>() {
                @Override
                public BinaryValue execute( Connection connection ) throws Exception {
                    // prepare new binary key based on SHA-1
                    BinaryKey key = new BinaryKey(temp.getKey().toString());

                    // check for duplicate content
                    Database database = database();

                    if (database.contentExists(key, ALIVE, connection)) {
                        return new StoredBinaryValue(DatabaseBinaryStore.this, key, temp.getSize());
                    }

                    // check unused content
                    if (database.contentExists(key, UNUSED, connection)) {
                        if (!markAsUnused) {
                            database.restoreContent(connection, Arrays.asList(key));
                        }
                    } else {
                        // store the content
                        database.insertContent(key, temp.getStream(), temp.getSize(), connection);
                        if (markAsUnused) {
                            database.markUnused(Arrays.asList(key), connection);
                        }
                    }
                    return new StoredBinaryValue(DatabaseBinaryStore.this, key, temp.getSize());
                }
            });
        } finally {
            // remove content from temp store
            cache.markAsUnused(temp.getKey());
        }
    }

    @Override
    public InputStream getInputStream( BinaryKey key ) throws BinaryStoreException {
        Connection connection = newConnection();
        try {
            InputStream inputStream = database.readContent(key, connection);
            if (inputStream == null) {
                throw new BinaryStoreException(JcrI18n.unableToFindBinaryValue.text(key, database.getTableName()));
            }
            // the connection & statement will be left open until the stream is closed !
            return inputStream;
        } catch (SQLException e) {
            throw new BinaryStoreException(e);
        }
    }

    @Override
    public void markAsUsed(final Iterable<BinaryKey> keys ) throws BinaryStoreException {
        dbCall(new DBCallable<Object>() {
            @SuppressWarnings( "synthetic-access" )
            @Override
            public Object execute( Connection connection ) throws Exception {
                database.restoreContent(connection, keys);
                return null;
            }
        }) ;
    }

    @Override
    public void markAsUnused( final Iterable<BinaryKey> keys ) throws BinaryStoreException {
        dbCall(new DBCallable<Void>() {
            @Override
            public Void execute( Connection connection ) throws Exception {
                database().markUnused(keys, connection);
                return null;
            }
        });
    }

    @Override
    public void removeValuesUnusedLongerThan( final long minimumAge,
                                              final TimeUnit unit ) throws BinaryStoreException {
        dbCall(new DBCallable<Void>() {
            @Override
            public Void execute( Connection connection ) throws Exception {
                long deadline = System.currentTimeMillis() - unit.toMillis(minimumAge);
                database().removeExpiredContent(deadline, connection);
                return null;
            }
        });
    }

    @Override
    protected String getStoredMimeType( final BinaryValue source ) throws BinaryStoreException {
        return dbCall(new DBCallable<String>() {
            @Override
            public String execute( Connection connection ) throws Exception {
                BinaryKey key = source.getKey();
                if (!database().contentExists(key, true, connection) && !database().contentExists(key, false, connection)) {
                    throw new BinaryStoreException(JcrI18n.unableToFindBinaryValue.text(key, database().getTableName()));
                }
                return database().getMimeType(key, connection);
            }
        });
    }

    @Override
    protected void storeMimeType( final BinaryValue source,
                                  final String mimeType ) throws BinaryStoreException {
        dbCall(new DBCallable<Void>() {
            @Override
            public Void execute( Connection connection ) throws Exception {
                database().setMimeType(source.getKey(), mimeType, connection);
                return null;
            }
        });
    }

    @Override
    public String getExtractedText( final BinaryValue source ) throws BinaryStoreException {
        return dbCall(new DBCallable<String>() {
            @SuppressWarnings( "synthetic-access" )
            @Override
            public String execute( Connection connection ) throws Exception {
                BinaryKey key = source.getKey();
                if (!database.contentExists(key, true, connection) && !database.contentExists(key, false, connection)) {
                    throw new BinaryStoreException(JcrI18n.unableToFindBinaryValue.text(key, database().getTableName()));
                }
                return database().getExtractedText(key, connection);
            }
        });
    }

    @Override
    public void storeExtractedText( final BinaryValue source,
                                    final String extractedText ) throws BinaryStoreException {
        dbCall(new DBCallable<Void>() {
            @Override
            public Void execute( Connection connection ) throws Exception {
                database().setExtractedText(source.getKey(), extractedText, connection);
                return null;
            }
        });
    }

    @Override
    public Iterable<BinaryKey> getAllBinaryKeys() throws BinaryStoreException {
        return dbCall(new DBCallable<Iterable<BinaryKey>>() {
            @Override
            public Iterable<BinaryKey> execute( Connection connection ) throws Exception {
                return database().getBinaryKeys(connection);
            }
        });
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }

    private Connection newConnection() {
        try {
            return dataSource != null ? dataSource.getConnection() : DriverManager.getConnection(connectionURL, username,
                                                                                                 password);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void lookupDataSource() {
        try {
            InitialContext context = new InitialContext();
            dataSource = (DataSource)context.lookup(datasourceJNDILocation);
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    private void lookupDriver() {
        try {
            Class.forName(driverClass);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private interface DBCallable<T> {
        public T execute( Connection connection ) throws Exception;
    }

    private <T> T dbCall( DBCallable<T> callable ) throws BinaryStoreException {
        Connection connection = newConnection();
        try {
            return callable.execute(connection);
        } catch (BinaryStoreException bse) {
            throw bse;
        } catch (Exception e) {
            throw new BinaryStoreException(e);
        } finally {
            Database.tryToClose(connection);
        }
    }
}
