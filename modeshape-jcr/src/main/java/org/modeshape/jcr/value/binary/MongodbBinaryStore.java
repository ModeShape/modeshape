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

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.ServerAddress;
import org.modeshape.common.util.IoUtil;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * A {@link BinaryStore} implementation that uses a MongoDB for persisting binary values.
 * 
 * @author kulikov
 */
public class MongodbBinaryStore extends AbstractBinaryStore {

    //default database name
    private static final String DEFAULT_DB_NAME = "ModeShape_BinaryStore";

    // field names
    private static final String FIELD_CHUNK_TYPE = "chunk-type";
    private static final String FIELD_MIME_TYPE = "mime-type";
    private static final String FIELD_EXTRACTED_TEXT = "extracted-text";
    private static final String FIELD_UNUSED_SINCE = "unused-since";
    private static final String FIELD_UNUSED = "unused";
    private static final String FIELD_CHUNK_SIZE = "chunk-size";
    private static final String FIELD_CHUNK_BUFFER = "chunk-buffer";

    // chunk types
    private static final String CHUNK_TYPE_HEADER = "header";
    private static final String CHUNK_TYPE_DATA_CHUNK = "data";

    // keys for chunks(header or data)
    protected static final BasicDBObject HEADER = new BasicDBObject().append(FIELD_CHUNK_TYPE, CHUNK_TYPE_HEADER);
    protected static final BasicDBObject DATA_CHUNK = new BasicDBObject().append(FIELD_CHUNK_TYPE, CHUNK_TYPE_DATA_CHUNK);

    private FileSystemBinaryStore cache;

    // database name
    private String database;

    private String host;
    private int port;

    // credentials
    private String username;
    private String password;

    // server address(es)
    private Set<String> replicaSet = new HashSet<String>();

    // database instance
    private DB db;

    // chunk size in bytes
    protected int chunkSize = 1024;

    public MongodbBinaryStore() {
        this.cache = TransientBinaryStore.get();
        this.database = DEFAULT_DB_NAME;
    }

    /**
     * Creates new store.
     * 
     * @param database database name
     * @param replicaSet list of server addresses in the form 'host:port' or null for localhost
     */
    public MongodbBinaryStore( String database,
                               Set<String> replicaSet ) {
        this.cache = TransientBinaryStore.get();
        this.database = database;
        if (replicaSet != null) {
            this.replicaSet.addAll(replicaSet);
        }
    }

    /**
     * Creates new store.
     * 
     * @param database database name
     * @param username database user
     * @param password user's password
     * @param replicaSet list of server addresses in the form 'host:port' or null for localhost
     */
    public MongodbBinaryStore( String database,
                               String username,
                               String password,
                               Set<String> replicaSet ) {
        this.cache = TransientBinaryStore.get();
        this.database = database;
        this.username = username;
        this.password = password;
        if (replicaSet != null) {
            this.replicaSet.addAll(replicaSet);
        }
    }

    /**
     * Creates a new instance of the store, using a single MongoDB.
     *
     * @param host
     * @param port
     * @param database
     */
    public MongodbBinaryStore( String host,
                               int port,
                               String database ) {
        this.cache = TransientBinaryStore.get();
        this.host = host;
        this.port = port;
        this.database = database;
    }

    /**
     * Converts list of addresses specified in text format to mongodb specific address.
     * 
     * @param addresses list of addresses in text format
     * @return list of mongodb addresses
     * @throws UnknownHostException when at least one host is unknown
     * @throws IllegalArgumentException if address has bad format
     */
    private List<ServerAddress> replicaSet( Set<String> addresses ) throws UnknownHostException {
        List<ServerAddress> list = new ArrayList<ServerAddress>();
        for (String address : addresses) {
            // address has format <host:port>
            String[] tokens = address.split(":");

            // checking tokens number after split
            if (tokens.length != 2) {
                throw new IllegalArgumentException("Wrong address format: " + address);
            }

            String host = tokens[0];

            // convert port number
            int port;
            try {
                port = Integer.parseInt(tokens[1]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Wrong address format: " + address);
            }

            list.add(new ServerAddress(host, port));
        }

        return list;
    }

    /**
     * Gets the size of the chunk used to store content.
     * 
     * @return chunk size in bytes.
     */
    public int getChunkSize() {
        return chunkSize;
    }

    /**
     * Modifies chunk size used to store content.
     * 
     * @param chunkSize chunk size in bytes.
     */
    public void setChunkSize( int chunkSize ) {
        this.chunkSize = chunkSize;
    }

    @Override
    public BinaryValue storeValue( InputStream stream ) throws BinaryStoreException {
        // store into temporary file system store and get SHA-1
        BinaryValue temp = cache.storeValue(stream);
        try {
            // prepare new binary key based on SHA-1
            BinaryKey key = new BinaryKey(temp.getKey().toString());

            // check for duplicate records
            if (db.collectionExists(key.toString())) {
                return new StoredBinaryValue(this, key, temp.getSize());
            }

            // store content
            DBCollection content = db.getCollection(key.toString());
            ChunkOutputStream dbStream = new ChunkOutputStream(content);
            try {
                IoUtil.write(temp.getStream(), dbStream);
            } catch (Exception e) {
                throw new BinaryStoreException(e);
            }

            return new StoredBinaryValue(this, key, temp.getSize());
        } finally {
            // clean up temp store
            cache.markAsUnused(temp.getKey());
        }
    }

    @Override
    public InputStream getInputStream( BinaryKey key ) throws BinaryStoreException {
        if (!db.collectionExists(key.toString())) {
            throw new BinaryStoreException(JcrI18n.unableToFindBinaryValue.text(key, db.getName()));
        }
        return new ChunkInputStream(db.getCollection(key.toString()));
    }

    @Override
    public void markAsUnused( Iterable<BinaryKey> keys ) {
        for (BinaryKey key : keys) {
            // silently ignore if content does not exist
            if (db.collectionExists(key.toString())) {
                DBCollection content = db.getCollection(key.toString());
                setAttribute(content, FIELD_UNUSED, true);
                setAttribute(content, FIELD_UNUSED_SINCE, new Date().getTime());
            }
        }
    }

    @Override
    public void removeValuesUnusedLongerThan( long minimumAge,
                                              TimeUnit unit ) {
        long deadline = new Date().getTime() - unit.toMillis(minimumAge);
        Set<String> keys = getStoredKeys();
        for (String key : keys) {
            DBCollection content = db.getCollection(key);
            if (isExpired(content, deadline)) content.drop();
        }
    }

    @Override
    protected void storeMimeType( BinaryValue source,
                                  String mimeType ) throws BinaryStoreException {
        if (db.collectionExists(source.getKey().toString())) {
            DBCollection content = db.getCollection(source.getKey().toString());
            setAttribute(content, FIELD_MIME_TYPE, mimeType);
        } else {
            throw new BinaryStoreException(JcrI18n.unableToFindBinaryValue.text(source.getKey(), db.getName()));
        }
    }

    @Override
    protected String getStoredMimeType( BinaryValue source ) throws BinaryStoreException {
        if (!db.collectionExists(source.getKey().toString())) {
            throw new BinaryStoreException(JcrI18n.unableToFindBinaryValue.text(source.getKey(), db.getName()));
        }
        DBCollection content = db.getCollection(source.getKey().toString());
        return (String)getAttribute(content, FIELD_MIME_TYPE);
    }

    @Override
    public void storeExtractedText( BinaryValue source,
                                    String extractedText ) throws BinaryStoreException {
        if (!db.collectionExists(source.getKey().toString())) {
            throw new BinaryStoreException(JcrI18n.unableToFindBinaryValue.text(source.getKey(), db.getName()));
        }
        DBCollection content = db.getCollection(source.getKey().toString());
        setAttribute(content, FIELD_EXTRACTED_TEXT, extractedText);
    }

    @Override
    public String getExtractedText( BinaryValue source ) throws BinaryStoreException {
        if (!db.collectionExists(source.getKey().toString())) {
            throw new BinaryStoreException(JcrI18n.unableToFindBinaryValue.text(source.getKey(), db.getName()));
        }
        DBCollection content = db.getCollection(source.getKey().toString());
        return (String)getAttribute(content, FIELD_EXTRACTED_TEXT);
    }

    @Override
    public Iterable<BinaryKey> getAllBinaryKeys() {
        ArrayList<BinaryKey> list = new ArrayList<BinaryKey>();
        Set<String> keys = getStoredKeys();
        for (String s : keys) {
            list.add(new BinaryKey(s));
        }
        return list;
    }

    private Set<String> getStoredKeys() {
        Set<String> storedKeys = new HashSet<String>();

        Set<String> collectionNames = db.getCollectionNames();
        for (String collectionName : collectionNames) {
            //make sure Mongo predefined collections are not taken into account
            if (collectionName.toLowerCase().startsWith("system") || collectionName.toLowerCase().startsWith("local")) {
                continue;
            }
            storedKeys.add(collectionName);
        }
        return storedKeys;
    }

    @Override
    public void start() {
        super.start();

        // check database name
        if (StringUtil.isBlank(database)) {
            throw new RuntimeException("Database name is not specified");
        }

        initMongoDatabase();

        // authenticate if required
        if (!StringUtil.isBlank(username) && !StringUtil.isBlank(password)) {
            if (!db.authenticate(username, password.toCharArray())) {
                throw new RuntimeException("Invalid username/password");
            }
        }
    }

    private void initMongoDatabase() {
        // connect to database
        try {
            Mongo mongo = null;
            if (!replicaSet.isEmpty())  {
                mongo = new Mongo(replicaSet(replicaSet));
            } else if (!StringUtil.isBlank(host)) {
                mongo = new Mongo(host, port);
            } else {
                mongo = new Mongo();
            }
            db = mongo.getDB(database);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Modifies content header.
     * 
     * @param content stored content
     * @param fieldName attribute name
     * @param value new value for the attribute
     */
    private void setAttribute( DBCollection content,
                               String fieldName,
                               Object value ) {
        DBObject header = content.findOne(HEADER);
        BasicDBObject newHeader = new BasicDBObject();

        // clone header
        newHeader.put(FIELD_CHUNK_TYPE, header.get(FIELD_CHUNK_TYPE));
        newHeader.put(FIELD_MIME_TYPE, header.get(FIELD_MIME_TYPE));
        newHeader.put(FIELD_EXTRACTED_TEXT, header.get(FIELD_EXTRACTED_TEXT));
        newHeader.put(FIELD_UNUSED, header.get(FIELD_UNUSED));
        newHeader.put(FIELD_UNUSED_SINCE, header.get(FIELD_UNUSED_SINCE));

        // modify specified field and update record
        newHeader.put(fieldName, value);
        content.update(HEADER, newHeader);
    }

    /**
     * Gets attribute's value.
     * 
     * @param content stored content
     * @param fieldName attribute name
     * @return attributes value
     */
    private Object getAttribute( DBCollection content,
                                 String fieldName ) {
        return content.findOne(HEADER).get(fieldName);
    }

    /**
     * Checks status of unused content.
     * 
     * @param content content to check status
     * @param deadline moment of time in past
     * @return true if content is marked as unused before the deadline
     */
    private boolean isExpired( DBCollection content,
                               long deadline ) {
        Long unusedSince = (Long)getAttribute(content, FIELD_UNUSED_SINCE);
        return unusedSince != null && unusedSince < deadline;
    }

    /**
     * Provide an OutputStream which will write to a database storage.
     */
    private class ChunkOutputStream extends OutputStream {
        // stored content
        private DBCollection content;

        // local intermediate chunk buffer
        private byte[] buffer = new byte[chunkSize];
        // current position in the local buffer
        private int offset;

        // object for writting chunks into storage
        private BasicDBObject chunk = new BasicDBObject();

        /**
         * Creates new stream.
         * 
         * @param content stored content
         */
        public ChunkOutputStream( DBCollection content ) {
            this.content = content;

            // start from header
            // mark first chunk as header and mark it as used
            BasicDBObject header = new BasicDBObject();
            header.put(FIELD_CHUNK_TYPE, CHUNK_TYPE_HEADER);
            header.put(FIELD_UNUSED, false);

            // insert into database
            this.content.insert(header);
        }

        @Override
        public void write( int b ) {
            // fill the local buffer first
            if (offset < buffer.length) {
                buffer[offset++] = (byte)b;
            }

            // push chunk into storage
            if (offset == buffer.length) {
                flush();
            }
        }

        @Override
        public void flush() {
            if (offset > 0) {
                // fill data
                chunk.put(FIELD_CHUNK_TYPE, CHUNK_TYPE_DATA_CHUNK);
                chunk.put(FIELD_CHUNK_SIZE, offset);
                chunk.put(FIELD_CHUNK_BUFFER, buffer);

                // store chink
                content.insert(chunk);

                // reset (weird thing is that we can't use mutable objects here)
                offset = 0;
                chunk = new BasicDBObject();
            }
        }
    }

    /**
     * Provide an InputStream which will read from a database storage.
     */
    private class ChunkInputStream extends InputStream {
        // list of datachunks
        private DBCursor cursor;

        // local buffer and current position inthe buffer
        private byte[] buffer = new byte[chunkSize];
        private int offset = 0;

        // object for reading chunks from database
        private DBObject chunk = new BasicDBObject();
        // the actual amount of data stored in chunk
        private int size = 0;

        public ChunkInputStream( DBCollection chunks ) {
            // execute query for selecting data chunks only
            cursor = chunks.find(DATA_CHUNK);
        }

        @Override
        public int read() {
            // read current chunk
            if (offset < size) {
                //make sure it's unsigned (see javadoc)
                return 0xff & buffer[offset++];
            }

            // try to pick up next chunk
            if (cursor.hasNext()) {
                chunk = cursor.next();
                size = (Integer)chunk.get(FIELD_CHUNK_SIZE);
                buffer = (byte[])chunk.get(FIELD_CHUNK_BUFFER);
                offset = 0;
            }

            // start reading from new chunk
            if (offset < size) {
                return 0xff & buffer[offset++];
            }

            // end of stream reached
            return -1;
        }

    }
}
