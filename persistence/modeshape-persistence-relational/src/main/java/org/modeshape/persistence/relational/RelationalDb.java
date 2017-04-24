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

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.modeshape.common.database.DatabaseType;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.StringUtil;
import org.modeshape.schematic.SchematicDb;
import org.modeshape.schematic.SchematicEntry;
import org.modeshape.schematic.document.Document;
import org.modeshape.schematic.document.EditableDocument;

/**
 * {@link SchematicDb} implementation which stores data in Relational databases.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @since 5.0
 */
public class RelationalDb implements SchematicDb {  
    
    private static final Logger LOGGER = Logger.getLogger(RelationalDb.class);

    private final ConcurrentMap<String, Connection> connectionsByTxId;
    private final DataSourceManager dsManager;
    private final RelationalDbConfig config;
    private final Statements statements;
    private final TransactionalCaches transactionalCaches;

    protected RelationalDb(Document configDoc) {
        this.connectionsByTxId = new ConcurrentHashMap<>();
        configDoc = Objects.requireNonNull(configDoc, "Configuration document cannot be null");
        this.config = new RelationalDbConfig(configDoc);
        this.dsManager = new DataSourceManager(config);
        DatabaseType dbType = dsManager.dbType();
        this.statements = createStatements(dbType);
        this.transactionalCaches = new TransactionalCaches();
    }

    private Statements createStatements(DatabaseType dbType) {
        Map<String, String> statementsFile = loadStatementsResource();
        switch (dbType.name()) {
            case ORACLE:
                return new OracleStatements(config, statementsFile);
            case SQLSERVER:
                return new SQLServerStatements(config, statementsFile);
            case DB2: {
                return new DB2Statements(config, statementsFile);
            }
            default:
                return new DefaultStatements(config, statementsFile);
        }
    }

    @Override
    public String id() {
        return config.name();
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
        
        // and clear the caches
        transactionalCaches.stop();
    }

    private void cleanupConnections() {
        if (connectionsByTxId.isEmpty()) {
            return;
        }
        LOGGER.warn(RelationalProviderI18n.warnConnectionsNeedCleanup, connectionsByTxId.size());
        // this should not normally happen because each flow should end with either a commit/rollback which should release
        // the allocated connection
        for (Iterator<Map.Entry<String, Connection>> iterator = connectionsByTxId.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<String, Connection> entry = iterator.next();
            closeConnection(entry.getKey(), entry.getValue());
            iterator.remove();
        }
    }

    private void closeConnection(String txId, Connection connection) {
        try {
            if (connection == null || connection.isClosed()) {
                return;
            }
        } catch (Throwable t) {
            LOGGER.debug(t, "Cannot determine DB connection status for transaction {0}", txId);
            return;
        }

        try {
            connection.close();
        } catch (Throwable t) {
            LOGGER.warn(RelationalProviderI18n.warnCannotCloseConnection, config.tableName(), txId, t.getMessage());
            // log the full stack trace via debug
            LOGGER.debug(t, "Cannot close connection");
        }
    }

    @Override
    public List<String> keys() {
        //first read everything from the db
        List<String> persistedKeys = runWithConnection(statements::getAllIds, true);
      
        if (!TransactionsHolder.hasActiveTransaction()) {
            // there is no active tx for just return the persistent view
            return persistedKeys;
        }
        // there is an active transaction, so just filter out the keys which have been removed
        persistedKeys.addAll(transactionalCaches.documentKeys());
        return persistedKeys.stream().filter(id -> !transactionalCaches.isRemoved(id)).collect(Collectors.toList());
    }
    
    @Override
    public Document get(String key) {
        if (!TransactionsHolder.hasActiveTransaction()) {
            // there is no active tx, so use a local read-only connection
            return runWithConnection(connection -> statements.getById(connection, key), true);
        }
       
        // there is an active transaction so: 
        // search for the document in the cache
        Document cachedDocument = transactionalCaches.search(key);
        // if we found a cached value, return either that or null if it has been removed
        if (cachedDocument != null) {
            logDebug("Getting {0} from cache; value {1}", key, cachedDocument);
            return cachedDocument != TransactionalCaches.REMOVED ? cachedDocument : null;
        } else if (transactionalCaches.isNew(key)) {
            return null;
        }

        // if it's not in the cache, bring one from the DB using a TL connection
        Document doc = runWithConnection(connection -> statements.getById(connection, key), false);
        if (doc != null) {
            // store for further reading...
            transactionalCaches.putForReading(key, doc);
        } else {
            // mark the key as new
            transactionalCaches.putNew(key);
        }
        return doc;
    }

    @Override
    public List<SchematicEntry> load(Collection<String> keys) {
        List<SchematicEntry> alreadyChangedInTransaction = Collections.emptyList();
        List<String> alreadyChangedKeys = new ArrayList<>();
        if (TransactionsHolder.hasActiveTransaction()) {
            // there's an active transaction so we want to look at stuff which we've already written in this tx and if there 
            // is anything, use it
            alreadyChangedInTransaction = keys.stream()
                .map(transactionalCaches::getForWriting)
                .filter(Objects::nonNull)
                .map(SchematicEntry::fromDocument)
                .collect(ArrayList::new, (list, schematicEntry) -> {
                    alreadyChangedKeys.add(schematicEntry.id());
                    if (TransactionalCaches.REMOVED != schematicEntry.source()) {
                        list.add(schematicEntry);
                    }
                }, ArrayList::addAll);
        }
        
        keys.removeAll(alreadyChangedKeys);
        Function<Document, SchematicEntry> documentParser = document -> {
            SchematicEntry entry = SchematicEntry.fromDocument(document);
            String id = entry.id();
            //always cache it to mark it as "existing"
            transactionalCaches.putForReading(id, document);
            return entry;
        };

        List<SchematicEntry> results = runWithConnection(connection -> statements.load(connection, keys, documentParser), true);
        results.addAll(alreadyChangedInTransaction);
        // if there's an active transaction make sure we also mark all the keys which were not found in the DB as 'new'
        // to prevent further DB lookups
        transactionalCaches.putNew(keys);
        return results;
    }

    @Override
    public boolean lockForWriting( List<String> locks ) {
        if (locks.isEmpty()) {
            return false;
        }
        TransactionsHolder.requireActiveTransaction();
        return runWithConnection(connection -> statements.lockForWriting(connection, locks), true);
    }

    @Override
    public void put(String key, SchematicEntry entry) {
        // simply store the put into the cache
        transactionalCaches.putForWriting(key, entry.source());
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
        runWithConnection(statements::removeAll, false);
    }

    @Override
    public boolean containsKey(String key) {
        if (!TransactionsHolder.hasActiveTransaction()) {
            // if there is no active tx, just search the DB directly
            return runWithConnection(connection -> statements.exists(connection, key), true);
        }
        // else look first in the caches for any transient / changed state
        Document cachedDocument = transactionalCaches.search(key);
        if (cachedDocument != null) {
            // if it's in the cache, just return based on the cached info
            return cachedDocument != TransactionalCaches.REMOVED;
        } else if (transactionalCaches.isNew(key)) {
            return false;
        }
        // otherwise it's not in the cache, so look in the DB
        boolean existsInDB = runWithConnection(connection -> statements.exists(connection, key), true);
        if (!existsInDB) {
            // it's not in the DB, so mark it as such
            transactionalCaches.putNew(key);
        }
        return existsInDB;
    }

    @Override
    public void txStarted(String id) {
        logDebug("New transaction '{0}' started by ModeShape...", id);
        String activeTx = TransactionsHolder.activeTransaction();
        if (activeTx != null && !activeTx.equals(id)) {
            LOGGER.warn(RelationalProviderI18n.threadAssociatedWithAnotherTransaction,
                    Thread.currentThread().getName(), activeTx, id);
        }
        // mark the current thread as linked to a tx...
        TransactionsHolder.setActiveTxId(id);
        // and allocate a new connection for this transaction preemptively to isolate it from other connections
        connectionForActiveTx();
    }

    @Override
    public void txCommitted(String id) {
        logDebug("Received committed notification for transaction '{0}'", id);
        try {
            Connection connection = connectionsByTxId.get(id);
            persistContent(connection, id);
        } catch (SQLException e) {
            throw new RelationalProviderException(e);
        } finally {
            cleanupTransaction(id);
        }
    }

    private void cleanupTransaction(String id) {
        try {
            // release any existing connection for this thread because a transaction has been committed...
            connectionsByTxId.computeIfPresent(id, (txId, connection) -> {
                closeConnection(txId, connection);
                logDebug("Released DB connection for transaction '{0}'", id);
                return null;
            });
        } finally {
            // clear the tx cache
            transactionalCaches.clearCache(id);
            // and clear the tx
            TransactionsHolder.clearActiveTransaction();
        }
    }

    private void persistContent(Connection tlConnection, String txId) throws SQLException {
        TransactionalCaches.TransactionalCache cache = transactionalCaches.cacheForTransaction(txId);
        if (cache == null) {
            // simply commit the connection
            tlConnection.commit();
            return;
        }
        Map<String, Document> writeCache = cache.writeCache();
        Map<String, Document> readCache = cache.readCache();

        logDebug("Committing the active connection for transaction {0} with the changes: {1}", txId, writeCache);
        Statements.BatchUpdate batchUpdate = statements.batchUpdate(tlConnection);
        Map<String, Document> toInsert = new HashMap<>();
        Map<String, Document> toUpdate = new HashMap<>();
        List<String> toRemove = new ArrayList<>();
        writeCache.forEach(( key, document ) -> {
            if (TransactionalCaches.REMOVED == document) {
                toRemove.add(key);
            } else if (readCache.containsKey(key)) {
                toUpdate.put(key, document);
            } else {
                toInsert.put(key, document);
            }
        });

        try {
            batchUpdate.insert(toInsert);
            batchUpdate.update(toUpdate);
            batchUpdate.remove(toRemove);
        } catch (SQLException e) {
            throw new RelationalProviderException(e);
        }
        tlConnection.commit();
    }

    @Override
    public void txRolledback(String id) {
        logDebug("Received rollback notification for transaction '{0}'", id);
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
            throw new RelationalProviderException(e);
        }
    }

    protected Connection connectionForActiveTx() {
        String activeTxId = TransactionsHolder.requireActiveTransaction();
        Connection connection = connectionsByTxId.get(activeTxId);
        if (connection != null) {
            return connection;
        }
        connection = dsManager.newConnection(false, false);
        connectionsByTxId.put(activeTxId, connection);
        logDebug("New DB connection allocated for tx '{0}'", activeTxId);
        return connection;
    }
    
    protected RelationalDbConfig config() {
        return config;
    }
    
    protected DataSourceManager dsManager() {
        return dsManager;
    }

    protected Connection newConnection(boolean autoCommit, boolean readonly) {
        return dsManager.newConnection(autoCommit, readonly);
    }
  
    private Map<String, String> loadStatementsResource() {
        try (InputStream fileStream = loadStatementsFile(dsManager.dbType())) {
            Properties statements = new Properties();
            statements.load(fileStream);
            return statements.entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey().toString(),
                                                                           entry -> {
                                                                               String value = entry.getValue().toString();
                                                                               return !value.contains("{0}") ?
                                                                                      value :
                                                                                      StringUtil.createString(value,
                                                                                                              config.tableName());
                                                                           }));
        } catch (IOException e) {
            throw new RelationalProviderException(e);
        }
    }
    
    private InputStream loadStatementsFile( DatabaseType dbType ) {
        String filePrefix = RelationalDb.class.getPackage().getName().replaceAll("\\.", "/") + "/" + dbType.nameString().toLowerCase();
        // first search for a file matching the major.minor version....
        String majorMinorFile = filePrefix + String.format("_%s.%s_database.properties", dbType.majorVersion(), dbType.minorVersion());
        // then a file matching just major version
        String majorFile = filePrefix + String.format("_%s_database.properties", dbType.majorVersion());
        // the a default with just the db name
        String defaultFile = filePrefix + "_database.properties";
        return Stream.of(majorMinorFile, majorFile, defaultFile)
                     .map(fileName -> {
                         InputStream is = RelationalDb.class.getClassLoader().getResourceAsStream(fileName);
                         if (LOGGER.isDebugEnabled()) {
                             if (is != null) {
                                 LOGGER.debug("located DB statements file '{0}'", fileName);
                             } else {
                                 LOGGER.debug("'{0}' statements file not found", fileName);
                             }
                         }
                         return is;
                     })
                     .filter(Objects::nonNull)
                     .findFirst()
                     .orElseThrow(() -> new RelationalProviderException(RelationalProviderI18n.unsupportedDBError, dbType));
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
