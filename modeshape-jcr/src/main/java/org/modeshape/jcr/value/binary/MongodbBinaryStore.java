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
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.common.util.IoUtil;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;

/**
 * A {@link BinaryStore} implementation that uses a MongoDB for persisting binary values.
 *
 * @author kulikov
 */
public class MongodbBinaryStore extends AbstractBinaryStore {

    // default database name
    private static final String DEFAULT_DB_NAME = "ModeShape_BinaryStore";

    // field names
    private static final String FIELD_CHUNK_TYPE = "chunk-type";
    private static final String FIELD_MIME_TYPE = "mime-type";
    private static final String FIELD_EXTRACTED_TEXT = "extracted-text";
    private static final String FIELD_UNUSED_SINCE = "unused-since";
    private static final String FIELD_UNUSED = "unused";
    private static final String FIELD_CHUNK_SIZE = "chunk-size";
    private static final String FIELD_CHUNK_BUFFER = "chunk-buffer";
    private static final String FIELD_CHUNK_POSITION = "chunk-order";
    private static final String FIELD_CHUNK_VERSION = "chunk-version";

    // chunk types
    private static final String CHUNK_TYPE_HEADER = "header";
    private static final String CHUNK_TYPE_DATA_CHUNK = "data";
    
    // chunk versions
    private static final int VERSION_1 = 1;

    // keys for chunks(header or data)
    protected static final BasicDBObject HEADER_QUERY = new BasicDBObject().append(FIELD_CHUNK_TYPE, CHUNK_TYPE_HEADER);
    protected static final BasicDBObject DATA_CHUNK_QUERY = new BasicDBObject().append(FIELD_CHUNK_TYPE, CHUNK_TYPE_DATA_CHUNK);
    protected static final BasicDBObject DATA_CHUNK_SORT_INDEX = new BasicDBObject().append(FIELD_CHUNK_POSITION, 1);

    private FileSystemBinaryStore cache;

    // database name
    private String database;

    // credentials
    private String username;
    private String password;

    // server address(es) - note that order is important
    private Set<String> hostAddresses = new LinkedHashSet<>();

    // database instance
    private DB db;

    // chunk size in bytes
    protected int chunkSize = 1024;
    
    /**
     * Creates a new mongo binary store instance using the supplied params.
     * 
     * @param host the mongo primary host; may be null in which case {@code hostAddresses} has to be provided
     * @param port the port of the primary host; may be null in which case {@code hostAddresses} has to be provided
     * @param database the name of the database; may be null in which case a default will be used
     * @param username the username; may be null 
     * @param password the password; may be null
     * @param hostAddresses a {@link List} of (host:port) pairs representing multiple server addresses; may be null
     */
    public MongodbBinaryStore(String host, Integer port, String database, String username, String password, List<String> hostAddresses) {
        this.cache = TransientBinaryStore.get();
        this.database = !StringUtil.isBlank(database) ? database : DEFAULT_DB_NAME;
        this.username = username;
        this.password = password;
        boolean hostAddressesProvided = hostAddresses != null && !hostAddresses.isEmpty();
        this.hostAddresses = new LinkedHashSet<>();
        String defaultServer = !StringUtil.isBlank(host) && port != null ? host + ":" + port : null;
        if (defaultServer == null && !hostAddressesProvided) {
            throw new IllegalArgumentException("Invalid Mongo binary store configuration: either (host and port) or host addresses have to provided");
        } 
        if (defaultServer != null) {
            this.hostAddresses.add(defaultServer);
        }
        if (hostAddressesProvided) {
            this.hostAddresses.addAll(hostAddresses);
        }
    }

    /**
     * Converts list of addresses specified in text format to mongodb specific address.
     *
     * @param addresses list of addresses in text format
     * @return list of mongodb addresses
     * @throws IllegalArgumentException if address has bad format or is not valid
     */
    private List<ServerAddress> convertToServerAddresses(Set<String> addresses)  {
        return addresses.stream()
                        .map(this::stringToServerAddress)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
    }
    
    private ServerAddress stringToServerAddress(String address) {
        if (address == null || address.trim().length() == 0) {
            return null;
        }
        // address has format <host:port>
        String[] tokens = address.split(":");
    
        // checking tokens number after split
        if (tokens.length != 2) {
            throw new IllegalArgumentException("Wrong address format: " + address + " (expected host:port)") ;
        }
    
        String host = tokens[0];
    
        // convert port number
        int port;
        try {
            port = Integer.parseInt(tokens[1]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Wrong address format: " + address + " (expected host:port)");
        }
    
        try {
            return new ServerAddress(host, port);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException(e);
        }
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
    public BinaryValue storeValue( InputStream stream,
                                   boolean markAsUnused ) throws BinaryStoreException {
        // store into temporary file system store and get SHA-1
        BinaryValue temp = cache.storeValue(stream, markAsUnused);
        try {
            // prepare new binary key based on SHA-1
            BinaryKey key = new BinaryKey(temp.getKey().toString());

            // check for duplicate records
            if (db.collectionExists(key.toString())) {
                return new StoredBinaryValue(this, key, temp.getSize());
            }

            // store content
            DBCollection content = db.getCollection(key.toString());
            content.createIndex(DATA_CHUNK_SORT_INDEX);
            ChunkOutputStream dbStream = markAsUnused ? 
                                         new ChunkOutputStream(content, System.currentTimeMillis()) : 
                                         new ChunkOutputStream(content);
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
    public void markAsUsed( Iterable<BinaryKey> keys ) {
        for (BinaryKey key : keys) {
            if (db.collectionExists(key.toString())) {
                DBCollection content = db.getCollection(key.toString());
                setAttribute(content, FIELD_UNUSED, false);
                setAttribute(content, FIELD_UNUSED_SINCE, null);
            }
        }
    }

    @Override
    public void markAsUnused( Iterable<BinaryKey> keys ) {
        for (BinaryKey key : keys) {
            // silently ignore if content does not exist
            if (db.collectionExists(key.toString())) {
                DBCollection content = db.getCollection(key.toString());
                setAttribute(content, FIELD_UNUSED, true);
                setAttribute(content, FIELD_UNUSED_SINCE, System.currentTimeMillis());
            }
        }
    }

    @Override
    public void removeValuesUnusedLongerThan( long minimumAge,
                                              TimeUnit unit ) {
        long deadline = System.currentTimeMillis() - unit.toMillis(minimumAge);
        Set<String> keys = getStoredKeys(false);
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
        Set<String> keys = getStoredKeys(true);
        for (String s : keys) {
            list.add(new BinaryKey(s));
        }
        return list;
    }

    private Set<String> getStoredKeys( boolean onlyUsed ) {
        Set<String> storedKeys = new HashSet<String>();

        Set<String> collectionNames = db.getCollectionNames();
        for (String collectionName : collectionNames) {
            // make sure Mongo predefined collections are not taken into account
            if (collectionName.toLowerCase().startsWith("system") || collectionName.toLowerCase().startsWith("local")) {
                continue;
            }
            DBCollection collection = db.getCollection(collectionName);
            boolean unused = (Boolean)getAttribute(collection, FIELD_UNUSED);
            if (!unused || !onlyUsed) {
                storedKeys.add(collectionName);
            }
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

        initMongo(username, password);
    }

    private void initMongo(String username, String password) {
        List<MongoCredential> credentials = new ArrayList<>();
        if (!StringUtil.isBlank(username) && !StringUtil.isBlank(password)) {
            credentials.add(MongoCredential.createCredential(username, database, password.toCharArray()));
        }

        // connect to database
        MongoClient client = hostAddresses.size() > 1 ?
                             new MongoClient(convertToServerAddresses(hostAddresses), credentials) :
                             new MongoClient(stringToServerAddress(hostAddresses.iterator().next()), credentials);
        client.setWriteConcern(WriteConcern.ACKNOWLEDGED);
        db = client.getDB(database);
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
        DBObject header = content.findOne(HEADER_QUERY);
        BasicDBObject newHeader = new BasicDBObject();

        // clone header
        newHeader.put(FIELD_CHUNK_TYPE, header.get(FIELD_CHUNK_TYPE));
        newHeader.put(FIELD_MIME_TYPE, header.get(FIELD_MIME_TYPE));
        newHeader.put(FIELD_EXTRACTED_TEXT, header.get(FIELD_EXTRACTED_TEXT));
        newHeader.put(FIELD_UNUSED, header.get(FIELD_UNUSED));
        newHeader.put(FIELD_UNUSED_SINCE, header.get(FIELD_UNUSED_SINCE));

        // modify specified field and update record
        newHeader.put(fieldName, value);
        content.update(HEADER_QUERY, newHeader);
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
        return content.findOne(HEADER_QUERY).get(fieldName);
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
    @NotThreadSafe
    protected class ChunkOutputStream extends OutputStream {
        // stored content
        private DBCollection content;

        // local intermediate chunk buffer
        private byte[] buffer = new byte[chunkSize];
        // current position in the local buffer
        private int offset;
        // the position of a chunk with a series of chunks
        private int position;

        // object for writing chunks into storage
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
            header.put(FIELD_CHUNK_VERSION, VERSION_1);

            // insert into database
            this.content.insert(header);
        }

        /**
         * Creates new stream.
         *
         * @param content stored content
         * @param unusedSince the number of milliseconds the binary has not been used; this value will be recorded in the binary
         *        value
         */
        public ChunkOutputStream( DBCollection content,
                                  long unusedSince ) {
            this.content = content;

            // start from header
            // mark first chunk as header and mark it as used
            BasicDBObject header = new BasicDBObject();
            header.put(FIELD_CHUNK_TYPE, CHUNK_TYPE_HEADER);
            header.put(FIELD_UNUSED, true);
            header.put(FIELD_UNUSED_SINCE, unusedSince);

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
                chunk.put(FIELD_CHUNK_POSITION, position);

                // store chink
                content.insert(chunk);

                // reset (weird thing is that we can't use mutable objects here)
                offset = 0;
                position++;
                chunk = new BasicDBObject();
            }
        }
    }

    /**
     * Provide an InputStream which will read from a database storage.
     */
    @NotThreadSafe
    protected class ChunkInputStream extends InputStream {
        // list of datachunks
        private final DBCursor cursor;

        // local buffer and current position inthe buffer
        private byte[] buffer = new byte[chunkSize];
        private int offset = 0;

        // object for reading chunks from database
        private DBObject chunk = new BasicDBObject();
        // the actual amount of data stored in chunk
        private int size = 0;

        public ChunkInputStream( DBCollection chunks ) {
            // dynamically create the cursor based on the version
            cursor = cursorFor(chunks);
        }
        
        private DBCursor cursorFor(DBCollection parent) {
            DBObject header = parent.findOne(HEADER_QUERY);
            // we should always have a header
            assert header != null;
            
            Object version = header.get(FIELD_CHUNK_VERSION);
            // we're always interested in data chunks
            DBCursor result = parent.find(DATA_CHUNK_QUERY);
            if (version == null) {
                // no version present
                return result; 
            }
            switch ((Integer) version) {
                case VERSION_1:
                    return result.sort(DATA_CHUNK_SORT_INDEX);
                default:    
                    throw new IllegalArgumentException("Unknown chunk version " + version);
            }
        } 

        @Override
        public int read() {
            // read current chunk
            if (offset < size) {
                // make sure it's unsigned (see javadoc)
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
