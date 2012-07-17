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
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.transaction.TransactionManager;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.LoaderConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.loaders.jdbc.stringbased.JdbcStringBasedCacheStore;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.transaction.TransactionMode;
import org.infinispan.transaction.lookup.JBossStandaloneJTAManagerLookup;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;

/**
 * A {@link BinaryStore} implementation that uses a database for persisting binary values.
 */
@ThreadSafe
public class DatabaseBinaryStore extends AbstractBinaryStore {

    //JDBC storage through infinispan
    private Cache<String, Metadata> metadata;
    private Cache<String, Chunk> payload;

    //JDBC storage for unused values
    private Cache<String, Metadata> trash;

    //Transactio manager
    private TransactionManager tm;

    //JDBC params
    private String driverClass;
    private String connectionURL;
    private String username;
    private String password;
    private String datasourceJNDILocation;

    //database specific type
    private String dataColumnType = "VARBINARY(1000)";
    private int chunkSize = 1000;

    //max idle time before key/value passivation
    private long expireTime = 2;

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

    /**
     * Modifies type of the data column.
     * 
     * @param dataColumnType database specific type for binary value.
     */
    public void setDataColumnType(String dataColumnType) {
        this.dataColumnType = dataColumnType;
    }

    public void setExpireTime(long expireTime) {
        this.expireTime = expireTime;
    }

    @Override
    public BinaryValue storeValue( InputStream stream ) throws BinaryStoreException {
        //Generating unique key. There is no qurantee that supplied input stream
        //support marks and reset, so no quarantee that we can walk back and forth
        //through the stream for generating key and then again for reading data.
        //It is unnecessary to hold entire content in memory and write termporary
        //to local disk as well, so let's use UUID generator for generating the key
        String uuid = UUID.randomUUID().toString();
        BinaryKey root = BinaryKey.keyFor(uuid.getBytes());

        //Create receiving stream
        ChunkOutputStream dataField = new ChunkOutputStream(root);

        //Store data: here we have a deal with two caches so we need
        //to customize transaction's boundary
        this.begin();
        try {
            //write metadata and content
            metadata.put(root.toString(), new Metadata());
            int contentLength = this.writeStream(stream, dataField);

            this.commit();

            //create and return representation of the record
            return new StoredBinaryValue(this, root, contentLength);
        } catch (Exception e) {
            this.rollback();
            throw new BinaryStoreException(e);
        }
    }

    @Override
    public InputStream getInputStream( BinaryKey key ) throws BinaryStoreException {
        //just wrap chunks stored in table raws with input stream wrapper
        return new ChunkInputStream(key);
    }

    @Override
    public void markAsUnused( Iterable<BinaryKey> keys ) throws BinaryStoreException {
        //do nothing if no keys
        if (keys == null) {
            return;
        }

        //move records from working cache to trash
        for (BinaryKey key : keys) {
            //poll record
            Metadata record = metadata.remove(key.toString());

            //no such record, ignore and proceed with next key
            if (record == null) {
                continue;
            }

            //update timestamp and put into trash
            record.touch();
            trash.put(key.toString(), record);
        }
    }

    @Override
    public void removeValuesUnusedLongerThan( long minimumAge,
                                              TimeUnit unit ) throws BinaryStoreException {
        //compute usage deadline (in past)
        long deadline = now() - unit.toMillis(minimumAge);

        //list for keys of the objects not used after deadline
        ArrayList<String> blackList = new ArrayList();

        //analyse usage
        Set<String> keys = trash.keySet();
        for (String k : keys) {
            if (!trash.get(k).usedAfter(deadline)) {
                blackList.add(k);
            }
        }

        //clean up: again two caches so customize tx boundaries
        this.begin();
        try {
            for (String k : blackList) {
                //remove metadata stored under root key
                trash.remove(k);

                //subsequently generare composite key with specified root
                //and remove all chunks with this root:
                CompositeKey ck = new CompositeKey(k);
                Chunk chunk = payload.remove(ck.toString());
                while (chunk != null) {
                    ck.next();
                    chunk = payload.remove(ck.toString());
                }
            }

            blackList.clear();
            this.commit();
        } catch (Exception e) {
            this.rollback();
            throw new BinaryStoreException(e);
        }
    }

    @Override
    protected String getStoredMimeType( BinaryValue source ) throws BinaryStoreException {
        //get record
        Metadata record = metadata.get(source.getKey().toString());
        if (record == null) {
            throw new BinaryStoreException("Value has been deleted");
        }

        return record.getMimeType();
    }

    @Override
    protected void storeMimeType(BinaryValue source, String mimeType)
            throws BinaryStoreException {
        //get record
        Metadata record = metadata.get(source.getKey().toString());
        if (record == null) {
            throw new BinaryStoreException("Value has been deleted");
        }

        //modify record and store modifications
        record.setMimeType(mimeType);
        metadata.replace(source.getKey().toString(), record);
    }

    @Override
    public String getExtractedText( BinaryValue source ) throws BinaryStoreException {
        //get record
        Metadata record = metadata.get(source.getKey().toString());
        if (record == null) {
            throw new BinaryStoreException("Value has been deleted");
        }

        return record.getExtractedText();
    }

    @Override
    public void storeExtractedText( BinaryValue source,
                                       String extractedText ) throws BinaryStoreException {
        //get record
        Metadata record = metadata.get(source.getKey().toString());
        if (record == null) {
            throw new BinaryStoreException("Value has been deleted");
        }

        //modify record and store modifications
        record.setExtractedText(extractedText);
        metadata.replace(source.getKey().toString(), record);
    }

    @Override
    public void start() {
        super.start();
        //prepare global ISPN config
        GlobalConfiguration glob = new GlobalConfigurationBuilder().nonClusteredDefault().build();

        //prepare string based JDBC cache loader config
        CacheConfigurationBuilder cacheConfig = new CacheConfigurationBuilder()
                .driverClass(driverClass)
                .connectionURL(connectionURL)
                .credentials(username, password)
                .jndiLocation(datasourceJNDILocation);

        //init cache
        metadata = new DefaultCacheManager(glob, cacheConfig.name("METADATA").build(), true).getCache();
        payload = new DefaultCacheManager(glob, cacheConfig.name("PAYLOAD").build(), true).getCache();
        trash = new DefaultCacheManager(glob, cacheConfig.name("TRASH").build(), true).getCache();

        metadata.start();
        trash.start();

        tm = metadata.getAdvancedCache().getTransactionManager();
    }

    @Override
    public void shutdown() {
        metadata.stop();
        trash.stop();
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
     * Transmits data between streams.
     * 
     * @param in stream to read data
     * @param out stream to transfer data to
     * @return number of bytes transfered.
     * @throws IOException 
     */
    private int writeStream(InputStream in, OutputStream out) throws IOException {
        int b = 0;
        int count = 0;
        while (b != -1) {
            b = in.read();
            if (b != -1) {
                count++;
                out.write(b);
            }
        }
        out.flush();
        return count;
    }

    /**
     * Starts transaction.
     * 
     * @throws BinaryStoreException 
     */
    private void begin() throws BinaryStoreException {
        try {
            tm.begin();
        } catch (Exception e) {
            throw new BinaryStoreException(e);
        }
    }

    /**
     * Commits transaction.
     *
     * @throws BinaryStoreException
     */
    private void commit() throws BinaryStoreException {
        try {
            tm.commit();
        } catch (Exception e) {
            throw new BinaryStoreException(e);
        }
    }

    /**
     * Rollback transaction.
     * 
     * @throws BinaryStoreException
     */
    private void rollback() throws BinaryStoreException {
        try {
            tm.rollback();
        } catch (Exception e) {
            throw new BinaryStoreException(e);
        }
    }

    /**
     * Local content representation.
     *
     */
    private class Metadata implements Serializable {
        //attributes
        private long timestamp;
        private String mimeType;
        private String extractedText;

        /**
         * Create new record.
         *
         * @param content binary content
         */
        public Metadata () {
            this.timestamp = new Date().getTime();
        }

        /**
         * Mime type of the content.
         *
         * @return mime type or null if not known
         */
        public String getMimeType() {
            return mimeType;
        }

        /**
         * Stores mime type of the content.
         *
         * @param mimeType mime type
         */
        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        /**
         * Text extracted from the content.
         *
         * @return text or null if text is unknown.
         */
        public String getExtractedText() {
            return extractedText;
        }

        /**
         * Stores text extracted from the content.
         *
         * @param extractedText
         */
        public void setExtractedText(String extractedText) {
            this.extractedText = extractedText;
        }

        /**
         * Reset usage timer.
         */
        public void touch() {
            this.timestamp = now();
        }

        /**
         * Checks usage time.
         * @param time the time threshold
         * @return true if this object was used after threshold false otherwise.
         */
        public boolean usedAfter(long time) {
            return timestamp > time;
        }
    }

    /**
     * InputStream which retrieve rows which stores chunk of data.
     */
    private class ChunkInputStream extends InputStream {
        private CompositeKey key;
        private Chunk chunk = new Chunk();

        public ChunkInputStream(BinaryKey root) {
            this.key = new CompositeKey(root.toString());
        }

        @Override
        public int read() throws IOException {
            if (chunk.eoc()) {
                chunk = payload.get(key.toString());
                chunk.reset();

                key.next();
            }

            if (chunk == null) {
                return -1;
            }

            return chunk.read();
        }

    }

    /**
     * OutputStream which stores chunks of data in rows.
     */
    private class ChunkOutputStream extends OutputStream {

        private CompositeKey key;
        private Chunk chunk = new Chunk();

        public ChunkOutputStream(BinaryKey root) {
            key = new CompositeKey(root.toString());
        }

        @Override
        public void write(int b) throws IOException {
            chunk.write((byte)b);
            if (chunk.eob()) flush();
        }

        @Override
        public void flush() {
            payload.put(key.toString(), chunk, expireTime, TimeUnit.SECONDS);
            key.next();
            chunk = new Chunk();
        }
    }

    /**
     * Represent chunk of payload.
     */
    private class Chunk implements Serializable {
        //internal buffer
        private byte[] buffer = new byte[chunkSize];
        //the length of payload stored in the buffer
        private int length;

        //current read/write position
        private transient int offset = 0;

        /**
         * The state of the chunk.
         *
         * @return true if end of chunk reached and false otherwise
         */
        public boolean eoc() {
            return offset == length;
        }

        /**
         * The state of the backing buffer.
         *
         * @return true if end of backing buffer reached and false otherwise
         */
        public boolean eob() {
            return offset == buffer.length;
        }

        /**
         * Reads single byte from this chunk.
         *
         * @return byte read or -1 if end of data reached.
         */
        public byte read() {
            return eoc()? -1 : buffer[offset++];
        }

        /**
         * Writes buffer into the chunk's buffer.
         *
         * @param b the byte to write
         */
        public void write(byte b) {
            if (!eob()) {
                buffer[offset++] = b;
                length++;
            }
        }

        /**
         * Puts read/write pointer into the beginning of the buffer.
         */
        public void reset() {
            offset = 0;
        }
    }

    /**
     * Composite key which is used for identification of the chunks.
     *
     * The key consists from unique root which is common for the entire sequence
     * and dynamic suffix.
     *
     */
    private class CompositeKey {

        //base root for this key
        private String root;
        //no of chunk
        private int chunkNo;

        /**
         * Creates new instance of the key.
         *
         * @param root root key which is common
         */
        public CompositeKey(String root) {
            this.root = root;
        }

        /**
         * Key for next chunk in a chain
         */
        public void next() {
            chunkNo++;
        }

        public void reset() {
            chunkNo = 0;
        }

        @Override
        public String toString() {
            return String.format("%s-%d", root.toString(), chunkNo);
        }
    }

    /**
     * Utility class for configuring infinispan cache.
     */
    private class CacheConfigurationBuilder extends ConfigurationBuilder {

        private LoaderConfigurationBuilder builder;

        /**
         * Creates configuration builder.
         */
        public CacheConfigurationBuilder () {
            builder = new ConfigurationBuilder()
                .transaction().transactionMode(TransactionMode.TRANSACTIONAL)
                .autoCommit(false)
                .transactionManagerLookup(new JBossStandaloneJTAManagerLookup())
                .loaders().addCacheLoader()
                .cacheLoader(new JdbcStringBasedCacheStore())
                .fetchPersistentState(false).purgeOnStartup(true)
                .addProperty("stringsTableNamePrefix", "carmart_table")
                .addProperty("idColumnName", "ID_COLUMN")
                .addProperty("dataColumnName", "DATA_COLUMN")
                .addProperty("timestampColumnName", "TIMESTAMP_COLUMN")
                //for different DB, use different type
                .addProperty("timestampColumnType", "BIGINT")
                .addProperty("connectionFactoryClass", "org.infinispan.loaders.jdbc.connectionfactory.ManagedConnectionFactory").addProperty("connectionUrl", "jdbc:mysql://localhost:3306/carmartdb")
                //for different DB, use different type
                .addProperty("idColumnType", "VARCHAR(255)")
                //for different DB, use different type
                .addProperty("dataColumnType", dataColumnType)
                .addProperty("dropTableOnExit", "false")
                .addProperty("createTableOnStart", "true");
        }

        /**
         * Specifies table name prefix.
         *
         * @param name table name prefix.
         * @return provisioned builder instance.
         */
        public CacheConfigurationBuilder name(String name) {
            builder = builder.addProperty("stringsTableNamePrefix", name);
            return this;
        }

        /**
         * Specifies database location.
         *
         * @param url path to database
         * @return provisioned builder instance.
         */
        public CacheConfigurationBuilder connectionURL(String url) {
            if (url != null) {
                builder = builder.addProperty("connectionUrl", url);
            }
            return this;
        }

        /**
         * Specifies JDBC driver class.
         *
         * @param driverClass class name
         * @return provisioned instance.
         */
        public CacheConfigurationBuilder driverClass(String driverClass) {
            if (driverClass != null) {
                builder = builder.addProperty("driverClass", driverClass);
            }
            return this;
        }

        /**
         * Specifies authentication information.
         *
         * @param username user's name
         * @param username user's password
         * @return provisioned builder
         */
        public CacheConfigurationBuilder credentials(String username, String password) {
            if (username != null) {
                builder = builder.addProperty("username", username);
            }
            if (password != null) {
                builder = builder.addProperty("password", password);
            }
            return this;
        }

        public CacheConfigurationBuilder jndiLocation(String jndiLocation) {
            if (jndiLocation != null) {
                builder = builder.addProperty("datasourceJNDILocation", jndiLocation);
            }
            return this;
        }

        @Override
        public Configuration build() {
            return builder.build();
        }
    }

}
