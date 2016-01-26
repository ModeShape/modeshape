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
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.modeshape.common.annotation.RequiresTransaction;
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
public class RelationalDb implements SchematicDb, TransactionalConnectionProvider {  
    
    private static final Logger LOGGER = Logger.getLogger(RelationalDb.class);
    
    private final DataSourceManager dsManager;
    private final RelationalDbConfig config;
    private final StatementsManager statements;
    private final TransactionalCaches transactionalCaches;

    protected RelationalDb(Document configDoc) {
        configDoc = Objects.requireNonNull(configDoc, "Configuration document cannot be null");
        this.config = new RelationalDbConfig(configDoc);
        this.dsManager = new DataSourceManager(config);
        this.statements = new StatementsManager(dsManager.dbType(), config.tableName());
        this.transactionalCaches = new TransactionalCaches(config.cacheSize());
    }

    @Override
    public String id() {
        return config.datasourceJNDIName() != null ? config.datasourceJNDIName() : config.connectionUrl();
    }

    @Override
    public Connection newConnection(boolean autoCommit) {
        return dsManager.newConnection(autoCommit);
    }

    @Override
    public void start() {
        if (config.createOnStart()) {
            try (Connection connection = newConnection(true)) {
                statements.createTable(connection);     
            } catch (SQLException e) {
                if (statements.canIgnore(dsManager.dbType(), e.getErrorCode())) {
                    LOGGER.debug(e, "Ignoring SQL exception");
                    return;
                }
                throw new RelationalProviderException(e);
            }
        }
    }

    private void dropTable() {
        try (Connection connection = newConnection(true)) {
            statements.dropTable(connection);
        } catch (SQLException e) {
            if (statements.canIgnore(dsManager.dbType(), e.getErrorCode())) {
                LOGGER.debug(e, "Ignoring SQL exception");
                return;
            }
            throw new RelationalProviderException(e);
        }
    }

    @Override
    public void stop() {
        // if there is an active connection for the current thread, release it...
        clearAllConnections();
        if (config.dropOnExit()) {
            dropTable();
        }
    }

    @Override
    public Stream<String> keys() {
        try {
            if (activeTransactionId().isPresent()) {
                // there is an active tx...
                Connection txConnection = connectionForActiveTx();
                return statements.getAllIds(txConnection, config.fetchSize(), this::idsCollector);
            } else {
                // there is no active tx, so just use a local read only connection
                try (Connection connection = dsManager.newConnection(true)) {
                    connection.setReadOnly(true);
                    return statements.getAllIds(connection, config.fetchSize(), this::idsCollector);
                }
            }
        } catch (SQLException e) {
            throw new RelationalProviderException(e);
        }
    }

    private Stream<String> idsCollector(ResultSet rs) {
        Stream.Builder<String> builder = Stream.builder();
        try {
            while (rs.next()) {
                builder.add(rs.getString(1));       
            }
        } catch (SQLException e) {
            throw new RelationalProviderException(e);
        }
        return builder.build();
    }

    @Override
    public Document get(String key) {
        try {
            Optional<String> activeTx = activeTransactionId();

            if (activeTx.isPresent()) {
                // search the caches first
                String txId = activeTx.get();
                Document doc = transactionalCaches.cachedDocument(txId, key);
                if (doc != null) {
                    return doc;
                }
                // there is an active tx....
                Connection txConnection = connectionForActiveTx();
                doc = statements.getById(txConnection, key, this::readDocument);
                if (doc != null) {
                    transactionalCaches.storeForReading(txId, key, doc);
                }
                return doc;
            } else {
                // there is no active tx, so use a local read-only connection
                try (Connection connection = dsManager.newConnection(true)) {
                    connection.setReadOnly(true);
                    return statements.getById(connection, key, this::readDocument);
                }
            }
        } catch (SQLException e) {
            throw new RelationalProviderException(e);
        }
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
                return Json.read(contentStream);    
            }
        } catch (Exception e) {
            throw new RelationalProviderException(e);
        } 
    }

    @Override
    public void put(String key, SchematicEntry entry) {
        Connection txConnection = connectionForActiveTx();
        try {
            statements.insertOrUpdateContent(txConnection, key, documentStream(entry.source()));
            // then update the cache
            transactionalCaches.storeForReading(activeTransactionId().get(), key, entry.source());
        } catch (SQLException e) {
            throw new RelationalProviderException(e);
        }
    }

    @Override
    @RequiresTransaction
    public void put(String key, Document content) {
        SchematicDb.super.put(key, content);
    }

    private StatementsManager.StreamSupplier documentStream(Document content)  {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (OutputStream out = config.compress() ? new GZIPOutputStream(bos) : new BufferedOutputStream(bos)) {
                Json.write(content, out);
            }
            byte[] bytes = bos.toByteArray();
            BufferedInputStream bis = new BufferedInputStream(new ByteArrayInputStream(bytes));
            return new StatementsManager.StreamSupplier() {
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
    @RequiresTransaction
    public EditableDocument editContent(String key, boolean createIfMissing) {
        String txId = requireActiveTransaction();
        EditableDocument editableDocument = SchematicDb.super.editContent(key, createIfMissing);
        if (editableDocument != null) {
            // store the editable document in the cache, because we'll have to update the db content on commit....
            transactionalCaches.storeForEditing(txId, key, editableDocument);    
        }
        return editableDocument;
    }

    @Override
    @RequiresTransaction
    public void putEntry(Document entryDocument) {
        SchematicDb.super.putEntry(entryDocument);
    }

    @Override
    @RequiresTransaction
    public SchematicEntry putIfAbsent(String key, Document content) {
        return SchematicDb.super.putIfAbsent(key, content);
    }

    @Override
    @RequiresTransaction
    public boolean remove(String key) {
        Connection txConnection = connectionForActiveTx();
        try {
            boolean success = statements.removeContent(txConnection, key);
            transactionalCaches.removeDocument(activeTransactionId().get(), key);
            return success;
        } catch (SQLException e) {
            throw new RelationalProviderException(e);
        }
    }

    @Override
    @RequiresTransaction
    public void removeAll() {
        Connection txConnection = connectionForActiveTx();
        try {
            statements.removeAllContent(txConnection);
            String txId = activeTransactionId().get();
            transactionalCaches.clearReadCache(txId);
            transactionalCaches.clearEditCache(txId);
        } catch (SQLException e) {
            throw new RelationalProviderException(e);
        }
    }

    @Override
    public boolean containsKey(String key) {
        try {
            Optional<String> activeTx = activeTransactionId();
            if (activeTx.isPresent()) {
                // there is an active tx....
                // search the cache first
                if (transactionalCaches.cachedDocument(activeTx.get(), key) != null) {
                    return true;
                }
                // if not in the cache, search 
                Connection txConnection = connectionForActiveTx();
                return statements.contentExists(txConnection, key);
            } else {
                try (Connection connection = newConnection(true)) {
                    return statements.contentExists(connection, key);
                }
            }
        } catch (SQLException e) {
            throw new RelationalProviderException(e);
        }
    }

    @Override
    public void txStarted(String id) {
        LOGGER.debug("New transaction '{0}' started by ModeShape...", id);
        // release any existing connection for this thread because ModeShape is starting a new transaction...
        // any subsequent calls should create a new TL connection if one isn't already there
        LOGGER.debug("Releasing any preexisting DB connection....");
        allocateConnection(id);
    }

    @Override
    public void txCommitted(String id) {
        LOGGER.debug("Received committed notification for transaction '{0}'", id);
        Connection tlConnection = connectionForActiveTx();
        assert tlConnection != null;
        try {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Committing the active connection for datasource {0} as part of transaction {1}", dsManager, id);
            }
            ConcurrentMap<String, Document> editCacheForTx = transactionalCaches.editCacheForTransaction(id, false);
            if (editCacheForTx != null) {
                LOGGER.debug("There are {0} active edit operations in place for transaction {1}. Flushing them prior to commit...",
                             editCacheForTx.size(), id);
                editCacheForTx.entrySet().forEach(entry -> put(entry.getKey(), entry.getValue()));
            }
            tlConnection.commit();
        } catch (SQLException e) {
            throw new RelationalProviderException(e);
        } finally {
            // clear any tx cache
            transactionalCaches.clearReadCache(id);
            transactionalCaches.clearEditCache(id);
            // release any existing connection for this thread because a transaction has been committed...
            LOGGER.debug("Releasing DB connection for transaction {0}", id);
            releaseConnection(id);
        }        
    }

    @Override
    public void txRolledback(String id) {
        LOGGER.debug("Received rollback notification for transaction '{0}'", id);

        Connection tlConnection = connectionForActiveTx();
        assert tlConnection != null;
        try {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Rolling back the active connection for datasource {0} as part of transaction {1}", dsManager, id);
            }
            tlConnection.rollback();
        } catch (SQLException e) {
            throw new RelationalProviderException(e);
        } finally {
            // clear any tx cache
            transactionalCaches.clearReadCache(id);
            transactionalCaches.clearEditCache(id);
            // release any existing connection for this thread because a transaction has been committed...
            LOGGER.debug("Releasing DB connection for transaction {0}", id);
            releaseConnection(id);
        }
    }
    
    protected RelationalDbConfig config() {
        return config;
    }
    
    protected DataSourceManager dsManager() {
        return dsManager;
    }

    @Override
    public String toString() {
        return "RelationalDB[" + config.toString() + "]";
    }
}
