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
package org.modeshape.persistence.relational;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.modeshape.common.database.DatabaseType;
import org.modeshape.common.logging.Logger;
import org.modeshape.schematic.SchematicDb;
import org.modeshape.schematic.SchematicEntry;
import org.modeshape.schematic.document.Document;
import org.modeshape.schematic.document.EditableDocument;
import org.modeshape.schematic.document.Json;

/**
 * {@link SchematicDb} implementation which stores data in Relational databases.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @since 5.0
 */
public class RelationalDb implements SchematicDb {  
    
    private static final Logger LOGGER = Logger.getLogger(RelationalDb.class);

    private static final Map<DatabaseType.Name, List<Integer>> IGNORABLE_ERROR_CODES_BY_DB = new HashMap<>();

    private final ConcurrentMap<String, Connection> connectionsByTxId;
    private final DataSourceManager dsManager;
    private final RelationalDbConfig config;
    private final StatementsProvider statements;
    private final TransactionalCaches transactionalCaches;

    static {
        // Oracle doesn't have an IF EXISTS clause for DROP or CREATE table, so in this case we want to ignore such exceptions
        IGNORABLE_ERROR_CODES_BY_DB.put(DatabaseType.Name.ORACLE, Arrays.asList(942, 955));
    }

    protected RelationalDb(Document configDoc) {
        this.connectionsByTxId = new ConcurrentHashMap<>();
        configDoc = Objects.requireNonNull(configDoc, "Configuration document cannot be null");
        this.config = new RelationalDbConfig(configDoc);
        this.dsManager = new DataSourceManager(config);
        this.statements = new StatementsProvider(dsManager.dbType(), config.tableName());
        this.transactionalCaches = new TransactionalCaches();
    }

    @Override
    public String id() {
        return config.datasourceJNDIName() != null ? config.datasourceJNDIName() : config.connectionUrl();
    }

    @Override
    public void start() {
        if (config.createOnStart()) {
            runWithConnection(statements::createTable, false);
        }
    }

    @Override
    public void stop() {
        // remove the active tx Id
        TransactionsHolder.clearActiveTransaction();
        
        // cleanup any possible active connections....
        cleanupConnections();
       
        // drop the table if configured to do so 
        if (config.dropOnExit()) {
            runWithConnection(statements::dropTable, false);
        }

        // and release any idle connections
        dsManager.close();
    }

    private void cleanupConnections() {
        if (connectionsByTxId.isEmpty()) {
            return;
        }
        LOGGER.warn(RelationalProviderI18n.warnConnectionsNeedCleanup, connectionsByTxId.size());
        // this should not normally happen because each flow should end with either a commit/rollback which should release
        // the allocated connection
        connectionsByTxId.values().stream().forEach(this::closeConnection);
        connectionsByTxId.clear();
    }

    private void closeConnection(Connection connection) {
        try {
            if (!connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            // ignore
        }
    }

    @Override
    public Set<String> keys() {
        //first read everything from the db
        Set<String> persistedKeys = runWithConnection(connection -> statements.getAllIds(connection, config.fetchSize(), this::idsCollector),
                                                      true);
      
        if (!TransactionsHolder.hasActiveTransaction()) {
            // there is no active tx for just return the persistent view
            return persistedKeys;
        }
        // there is an active transaction, so just filter out the keys which have been removed
        persistedKeys.addAll(transactionalCaches.documentKeys());
        return persistedKeys.stream().filter(id -> !transactionalCaches.isRemoved(id)).collect(Collectors.toSet());
    }

    private Set<String> idsCollector(ResultSet rs) {
        Set<String> ids = new LinkedHashSet<>();
        try {
            while (rs.next()) {
                ids.add(rs.getString(1));
            }
        } catch (SQLException e) {
            throw new RelationalProviderException(e);
        }
        return ids;
    }

    @Override
    public Document get(String key) {
        if (!TransactionsHolder.hasActiveTransaction()) {
            // there is no active tx, so use a local read-only connection
            return runWithConnection(connection -> statements.getById(connection, key, this::readDocument), true);
        }
       
        // there is an active transaction so: 
        // search for the document in the cache
        Document cachedDocument = transactionalCaches.search(key);
        // if we found a cached value, return either that or null if it has been removed
        if (cachedDocument != null) {
            logDebug("Getting {0} from cache; value {1}", key, cachedDocument);
            return cachedDocument != TransactionalCaches.REMOVED ? cachedDocument : null;
        }
        // if it's not in the cache, bring one from the DB using a TL connection
        Document doc = runWithConnection(connection -> statements.getById(connection, key, this::readDocument), false);
        if (doc != null) {
            // store for further reading...
            transactionalCaches.putForReading(key, doc);
        }
        return doc;
    }

    private Document readDocument(ResultSet resultSet) {
        try {
            if (!resultSet.next()) {
                return null;
            }
            InputStream binaryStream = resultSet.getBinaryStream(1);
            try (InputStream contentStream = config.compress() ? 
                                             new GZIPInputStream(binaryStream) : 
                                             new BufferedInputStream(binaryStream)) {
                return Json.read(contentStream, false);    
            }
        } catch (Exception e) {
            throw new RelationalProviderException(e);
        } 
    }

    @Override
    public void put(String key, SchematicEntry entry) {
        // simply store the put into the cache
        transactionalCaches.putForWriting(key, entry.source());
    }

    private StatementsProvider.StreamSupplier writeDocument(Document content)  {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (OutputStream out = config.compress() ? new GZIPOutputStream(bos) : new BufferedOutputStream(bos)) {
                Json.write(content, out);
            }
            byte[] bytes = bos.toByteArray();
            BufferedInputStream bis = new BufferedInputStream(new ByteArrayInputStream(bytes));
            return new StatementsProvider.StreamSupplier() {
                @Override
                public long length() {
                    return bytes.length;
                }

                @Override
                public InputStream get() {
                    return bis;
                }
            };
        } catch (IOException e) {
            throw new RelationalProviderException(e); 
        }
    }

    @Override
    public EditableDocument editContent(String key, boolean createIfMissing) {
        SchematicEntry entry = getEntry(key);
        if (entry == null) {
            if (createIfMissing) {
                put(key, SchematicEntry.create(key));
            } else {
                return null;
            }
        }
        // look for an entry which was set for writing
        Document entryDocument = transactionalCaches.getForWriting(key);
        if (entryDocument == null) {
            // it's the first time we're editing this document as part of this tx so store this document for writing...
            entryDocument = transactionalCaches.putForWriting(key, entry.source());
        }
        return SchematicEntry.content(entryDocument).editable(); 
    }

    @Override
    public SchematicEntry putIfAbsent(String key, Document content) {
        SchematicEntry existingEntry = getEntry(key);
        if (existingEntry != null) {
            return existingEntry;
        } else {
            put(key, SchematicEntry.create(key, content));
            return null;
        }
    }

    @Override
    public boolean remove(String key) {
        transactionalCaches.remove(key);
        return true;
    }

    @Override
    public void removeAll() {
        runWithConnection(statements::removeAllContent, false);
    }

    @Override
    public boolean containsKey(String key) {
        if (!TransactionsHolder.hasActiveTransaction()) {
            // if there is no active tx, just search the DB directly
            return runWithConnection(connection -> statements.contentExists(connection, key), true);
        }
        // else look first in the caches for any transient / changed state
        Document cachedDocument = transactionalCaches.search(key);
        if (cachedDocument != null) {
            // if it's in the cache, just return based on the cached info
            return cachedDocument != TransactionalCaches.REMOVED;
        }
        // otherwise it's not in the cache, so look in the DB
        return runWithConnection(connection -> statements.contentExists(connection, key), true);
    }

    @Override
    public void locksObtained(String txId, Set<String> ids) {
        logDebug("Transaction {0} now has exclusive locks on {1}. Flushing local cache...", txId, ids);
        transactionalCaches.flushReadCache(ids);
    }

    @Override
    public void txStarted(String id) {
        logDebug("New transaction '{0}' started by ModeShape...", id);
        // mark the current thread as linked to a tx...
        TransactionsHolder.setActiveTxId(id);
        // and allocate a new connection for this transaction preemptively to isolate it from other connections
        connectionForActiveTx();
        logDebug("New DB connection allocated for tx '{0}'", id);
    }

    @Override
    public void txCommitted(String id) {
        logDebug("Received committed notification for transaction '{0}'", id);
        // make sure the id that was there when the tx started matches this id...
        TransactionsHolder.validateTransaction(id);
        try {
            runWithConnection(this::persistContent, false);
        } finally {
            cleanupTransaction(id);
        }
    }

    private void cleanupTransaction(String id) {
        // clear the tx cache
        transactionalCaches.clearCache();
        // release any existing connection for this thread because a transaction has been committed...
        logDebug("Releasing DB connection for transaction {0}", id);
        releaseConnectionForActiveTx();
        // and clear the tx 
        TransactionsHolder.clearActiveTransaction();
    }

    private Void persistContent(Connection tlConnection) throws SQLException {
        ConcurrentMap<String, Document> txCache = transactionalCaches.writeCache();
        logDebug("Committing the active connection for transaction {0} with the changes: {1}",
                 TransactionsHolder.requireActiveTransaction(),
                 txCache);
        StatementsProvider.BatchUpdate batchUpdate = statements.batchUpdate(tlConnection);
        txCache.forEach((key, document) -> {
            try {
                if (TransactionalCaches.REMOVED == document) {
                    batchUpdate.remove(key);
                } else {
                    // if the key is in our read cache OR it's in the DB we must perform an update
                    // otherwise we should do an insert
                    // this is a slight optimization over going to the db each time
                    boolean update = transactionalCaches.hasBeenRead(key) || statements.contentExists(tlConnection, key);
                    if (update) {
                        batchUpdate.update(key, writeDocument(document));
                    } else {
                        batchUpdate.insert(key, writeDocument(document));
                    }
                }
            } catch (SQLException e) {
                throw new RelationalProviderException(e);
            }
        });
        batchUpdate.execute();
        tlConnection.commit();
        return null;
    }

    @Override
    public void txRolledback(String id) {
        logDebug("Received rollback notification for transaction '{0}'", id);
        // make sure the id that was there when the tx started matches this id...
        TransactionsHolder.validateTransaction(id);
        try {
            runWithConnection(this::rollback, false);
        } finally {
           cleanupTransaction(id);
        } 
    }
    
    private Void rollback(Connection connection) throws SQLException {
        connection.rollback();
        return null;
    }
    
    protected <R> R runWithConnection(SQLFunction<R> function, boolean readonly) {
        try {
            if (TransactionsHolder.hasActiveTransaction()) {
                // don't autoclose...
                Connection connection = connectionForActiveTx();
                return function.execute(connection);
            }

            // always autoclose
            try (Connection connection = newConnection(true, readonly)) {
                return function.execute(connection);
            }
        } catch (SQLException e) {
            DatabaseType dbType = dsManager.dbType();
            int errorCode = e.getErrorCode();
            if (canIgnore(dbType, errorCode)) {
                LOGGER.debug(e, "Ignoring SQL exception for database {0} with error code {1}", dbType, errorCode);
                return null;
            }
            throw new RelationalProviderException(e);
        }
    }

    protected Connection connectionForActiveTx() {
        return connectionsByTxId.computeIfAbsent(TransactionsHolder.requireActiveTransaction(), 
                                                    transactionId -> dsManager.newConnection(false, false));
    }
    
    protected RelationalDbConfig config() {
        return config;
    }
    
    protected DataSourceManager dsManager() {
        return dsManager;
    }
    
    protected void releaseConnectionForActiveTx() {
        connectionsByTxId.computeIfPresent(TransactionsHolder.requireActiveTransaction(),
                                              (txId, connection) -> {
                                                  closeConnection(connection);
                                                  return null;
                                              });
    }

    protected Connection newConnection(boolean autoCommit, boolean readonly) {
        return dsManager.newConnection(autoCommit, readonly);
    }
    
    protected boolean canIgnore(DatabaseType dbType, int errorCode) {
        return IGNORABLE_ERROR_CODES_BY_DB.getOrDefault(dbType.name(), Collections.emptyList()).contains(errorCode);
    }

    @Override
    public String toString() {
        return "RelationalDB[" + config.toString() + "]";
    }

    private void logDebug(String message, Object...args) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(message, args);
        }
    }

    @FunctionalInterface
    private interface SQLFunction<R>  {
        R execute(Connection connection) throws SQLException;
    }
}
