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

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.LoaderConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.loaders.jdbc.stringbased.JdbcStringBasedCacheStore;
import org.infinispan.manager.DefaultCacheManager;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;
import org.semanticdesktop.aperture.util.IOUtil;

/**
 * A {@link BinaryStore} implementation that uses a database for persisting binary values.
 */
@ThreadSafe
public class DatabaseBinaryStore extends AbstractBinaryStore {

    //JDBC storage through infinispan
    private Cache<String, Record> cache;
    //JDBC storage for unused values
    private Cache<String, Record> trash;

    //JDBC params
    private String driverClass;
    private String connectionURL;
    private String username;
    private String password;
    
    //database specific type
    private String dataColumnType = "VARBINARY(1000)";
    
    /**
     * Create new store.
     *
     * @param driverClass JDBC driver class name
     * @param connectionURL database location
     * @param username database user name
     * @param password database password
     */
    public DatabaseBinaryStore(String driverClass, String connectionURL, String username, String password) {
        this.driverClass = driverClass;
        this.connectionURL = connectionURL;
        this.username = username;
        this.password = password;
    }

    /**
     * Modifies type of the data column.
     * 
     * @param dataColumnType database specific type for binary value.
     */
    public void setDataColumnType(String dataColumnType) {
        this.dataColumnType = dataColumnType;
    }

    @Override
    public BinaryValue storeValue( InputStream stream ) throws BinaryStoreException {
        //read content from stream and generate unique key
        byte[] content = readContent(stream);
        BinaryKey key = BinaryKey.keyFor(content);

        //create local record for the supplied content and store record
        //under the computed key (hex view of the content's hash)
        Record record = new Record(content);
        cache.put(key.toString(), record);

        //create and return representation of the local record
        return new StoredBinaryValue(this, key, content.length);
    }

    @Override
    public InputStream getInputStream( BinaryKey key ) throws BinaryStoreException {
        Record record = cache.get(key.toString());

        //check that there is such record
        if (record == null) {
            throw new BinaryStoreException("No such record: " + key.toString());
        }

        //check content: if content is null return empty stream
        byte[] content = record.getContent();
        if (content == null) {
            content = new byte[]{};
        }

        //return content as stream
        return new ByteArrayInputStream(content);
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
            Record record = cache.remove(key.toString());

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

        //clean up
        for (String k : blackList) {
            trash.remove(k);
        }
        blackList.clear();
    }

    @Override
    protected String getStoredMimeType( BinaryValue source ) throws BinaryStoreException {
        //get record
        Record record = cache.get(source.getKey().toString());
        if (record == null) {
            throw new BinaryStoreException("Value has been deleted");
        }

        return record.getMimeType();
    }

    @Override
    protected void storeMimeType(BinaryValue source, String mimeType)
            throws BinaryStoreException {
        //get record
        Record record = cache.get(source.getKey().toString());
        if (record == null) {
            throw new BinaryStoreException("Value has been deleted");
        }

        //modify record and store modifications
        record.setMimeType(mimeType);
        cache.replace(source.getKey().toString(), record);
    }

    @Override
    public String getExtractedText( BinaryValue source ) throws BinaryStoreException {
        //get record
        Record record = cache.get(source.getKey().toString());
        if (record == null) {
            throw new BinaryStoreException("Value has been deleted");
        }

        return record.getExtractedText();
    }

    @Override
    public void storeExtractedText( BinaryValue source,
                                       String extractedText ) throws BinaryStoreException {
        //get record
        Record record = cache.get(source.getKey().toString());
        if (record == null) {
            throw new BinaryStoreException("Value has been deleted");
        }

        //modify record and store modifications
        record.setExtractedText(extractedText);
        cache.replace(source.getKey().toString(), record);
    }

    /**
     * Reads binary data from the specified stream.
     * @param stream stream to read data.
     * @return data read from stream
     * @throws BinaryStoreException 
     */
    private byte[] readContent(InputStream stream) throws BinaryStoreException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try {
            IOUtil.writeStream(stream, bout);
            return bout.toByteArray();
        } catch (IOException e) {
            throw new BinaryStoreException(e);
        }
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
                .credentials(username, password);

        //init cache
        cache = new DefaultCacheManager(glob, cacheConfig.name("CACHE").build(), true).getCache();
        trash = new DefaultCacheManager(glob, cacheConfig.name("TRASH").build(), true).getCache();

        cache.start();
        trash.start();
    }

    @Override
    public void shutdown() {
        cache.stop();
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
     * Local content representation.
     *
     */
    private class Record implements Serializable {
        //binary content
        private byte[] content;

        //attributes
        private long timestamp;
        private String mimeType;
        private String extractedText;

        /**
         * Create new record.
         *
         * @param content binary content
         */
        public Record (byte[] content) {
            this.content = content;
            this.timestamp = new Date().getTime();
        }

        /**
         * Gets content.
         *
         * @return binary content
         */
        public byte[] getContent() {
            return content;
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
     * Utility class for configuring infinispan cache.
     */
    private class CacheConfigurationBuilder extends ConfigurationBuilder {

        private LoaderConfigurationBuilder builder;

        /**
         * Creates configuration builder.
         */
        public CacheConfigurationBuilder () {
            builder = new ConfigurationBuilder()
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
            builder = builder.addProperty("connectionUrl", url);
            return this;
        }

        /**
         * Specifies JDBC driver class.
         *
         * @param driverClass class name
         * @return provisioned instance.
         */
        public CacheConfigurationBuilder driverClass(String driverClass) {
            builder = builder.addProperty("driverClass", driverClass);
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
            builder = builder
                    .addProperty("userName", username)
                    .addProperty("password", password);
            return this;
        }
        
        @Override
        public Configuration build() {
            return builder.build();
        }
    }

}
