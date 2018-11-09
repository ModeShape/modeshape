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
package org.modeshape.persistence.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.db.TransactionStore;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.FileUtil;
import org.modeshape.common.util.StringUtil;
import org.modeshape.schematic.SchematicDb;
import org.modeshape.schematic.SchematicEntry;
import org.modeshape.schematic.document.Document;
import org.modeshape.schematic.document.EditableDocument;

/**
 * {@link SchematicDb} implementation which uses H2's MV Store to store data in memory or on disk.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class FileDb implements SchematicDb {
    
    private final static Logger LOGGER = Logger.getLogger(FileDb.class);
    
    private final static String FILENAME = "modeshape.repository";
    private final static ThreadLocal<String> ACTIVE_TX_ID = new ThreadLocal<>();
    private final static String REPOSITORY_CONTENT = "modeshape_data";

    private final boolean compress;
    private final String path;
    
    private final ConcurrentMap<String, TransactionStore.TransactionMap<String, Document>> transactionalContentById = new ConcurrentHashMap<>();

    private MVStore store;
    private TransactionStore txStore; 
    private TransactionStore.TransactionMap<String, Document> persistedContent;
    
    protected static FileDb inMemory(boolean compress) {
        return new FileDb(null, compress);
    }    

    protected static FileDb onDisk(boolean compress, String path) {
        path = Objects.requireNonNull(path, "The 'path' configuration parameter is required by the FS persistence provider");
        return new FileDb(path, compress);
    }    
    
    private FileDb( String path, boolean compress ) {
        this.path = path;
        this.compress = compress;
    }

    @Override
    public String id() {
        String prefix = "modeshape-file-persistence";
        return path == null ? prefix : prefix + "_" + path;
    }

    @Override
    public List<String> keys() {
        List<String> keys = new ArrayList<>();
        persistedContent.keyIterator(persistedContent.firstKey()).forEachRemaining(keys::add);        
        TransactionStore.TransactionMap<String, Document> txContent = transactionalContent(false);
        if (txContent != null) {
            txContent.keyIterator(txContent.firstKey()).forEachRemaining(keys::add);
        }
        return keys;
    }

    @Override
    public Document get( String key ) {
        LOGGER.debug("reading {0}", key);
        TransactionStore.TransactionMap<String, Document> txContent = transactionalContent(false);
        Document result = txContent != null ? txContent.getLatest(key) : persistedContent.get(key);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("{0} is {1}", key, result);
        }
        return result;
    }

    @Override
    public List<SchematicEntry> load( Collection<String> keys ) {
        final TransactionStore.TransactionMap<String, Document> txContent = transactionalContent(false);
        final TransactionStore.TransactionMap<String, Document> actualContent = txContent != null ? txContent : persistedContent; 
        return keys.stream()
                   .map(actualContent::get)
                   .filter(Objects::nonNull)
                   .map(SchematicEntry::fromDocument)
                   .collect(Collectors.toList()); 
    }

    @Override
    public void put( String key, SchematicEntry entry ) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("putting at {0} document {1}", key, entry.source());
        }
        TransactionStore.TransactionMap<String, Document> txContent = transactionalContent(true);
        Document source = entry.source();
        Document content = entry.content();
        if (content instanceof EditableDocument) {
            source = SchematicEntry.create(entry.id(), ((EditableDocument) content).unwrap()).source();
        }
        txContent.put(key, source);
    }
    

    @Override
    public EditableDocument editContent( String key, boolean createIfMissing ) {
        TransactionStore.TransactionMap<String, Document> txContent = transactionalContent(true);
        Document existingTxDoc = txContent.get(key);
        if (existingTxDoc == null && createIfMissing) {
            existingTxDoc = SchematicEntry.create(key).source();
            txContent.put(key, existingTxDoc);            
        }
        
        if (existingTxDoc == null) {
            return null;
        } 
        
        if (!txContent.isSameTransaction(key)) {
            // this transaction is processing this key for the first time, so we need to clone it
            existingTxDoc = existingTxDoc.clone();
            if (!txContent.trySet(key, existingTxDoc, true)) {
                throw new FileProviderException("cannot write new value for the first time");
            }
        }
        
        return SchematicEntry.content(existingTxDoc).editable();
    }

    @Override
    public SchematicEntry putIfAbsent( String key, Document content ) {
        SchematicEntry existingEntry = getEntry(key);
        if (existingEntry != null) {
            return existingEntry;
        } else {
            put(key, SchematicEntry.create(key, content));
            return null;
        }
    }

    @Override
    public boolean remove( String key ) {
        TransactionStore.TransactionMap<String, Document> txContent = transactionalContent(true);
        Document doc = txContent.remove(key);
        if (doc != null) {
            LOGGER.debug("removed document at {0}", key);
            return true;
        }
        return false;
    }

    @Override
    public void removeAll() {
        TransactionStore.TransactionMap<String, Document> txContent = transactionalContent(true);  
        txContent.clear();
    }

    @Override
    public void start() {
        MVStore.Builder builder = new MVStore.Builder();
        builder.autoCommitDisabled();
        if (compress) {
            builder.compress();    
        }
        if (!StringUtil.isBlank(path)) {
            File file = new File(path);
            if (!file.exists() || !file.isDirectory() || !file.canRead()) {
                FileUtil.delete(file);
                try {
                    Files.createDirectories(Paths.get(path));
                } catch (IOException e) {
                    throw new FileProviderException(e);
                }
            }
            builder.fileName(path + "/" + FILENAME);
        }
        this.store = builder.open();
        this.txStore = new TransactionStore(store);
        this.txStore.init();
        // start a new transaction (which has READ_COMMITTED isolation) which will give us the view of the latest persisted data
        TransactionStore.Transaction tx = this.txStore.begin();
        this.persistedContent = tx.openMap(REPOSITORY_CONTENT);
    }

    @Override
    public void stop() {
        this.txStore.getOpenTransactions().forEach(TransactionStore.Transaction::rollback);
        // close the store
        this.store.close();
    }

    @Override
    public void txStarted( String id ) {
        LOGGER.debug("New tx '{0}' started...", id);
        String currentTx = ACTIVE_TX_ID.get();
        if (currentTx != null && !id.equals(currentTx)) {
            throw new FileProviderException(
                    "ModeShape transaction '" + currentTx + "' already associated to current thread; cannot associate new transaction " +
                    "'" + id  + "'");
        }
        ACTIVE_TX_ID.set(id);
        if (!this.transactionalContentById.containsKey(id)) {
            TransactionStore.TransactionMap<String, Document> transactionMap = this.txStore.begin().openMap(REPOSITORY_CONTENT);
            this.transactionalContentById.put(id, transactionMap);
        }
    }

    @Override
    public void txCommitted( String id ) {
        LOGGER.debug("Received committed notification for tx '{0}'", id);
        try {
            TransactionStore.TransactionMap<String, Document> txContent = this.transactionalContentById.remove(id);
            TransactionStore.Transaction tx = txContent.getTransaction();
            tx.commit();
            LOGGER.debug("tx '{0}' committed", id);
        } finally {
            ACTIVE_TX_ID.remove();
        }
    }

    @Override
    public void txRolledback( String id ) {
        LOGGER.debug("Received rollback notification for tx '{0}'", id);
        try {
            TransactionStore.Transaction tx = this.transactionalContentById.remove(id).getTransaction();
            tx.rollback();
            LOGGER.debug("tx '{0}' rolled back", id);
        } finally {
            ACTIVE_TX_ID.remove();
        }
    }
    
    protected TransactionStore.TransactionMap<String, Document> transactionalContent(boolean failIfMissing) {
        String currentTxId = ACTIVE_TX_ID.get();
        if (currentTxId == null) {
            if (failIfMissing) {
                throw new FileProviderException("An active transaction is required, but wasn't detected");
            } else {
                return null;
            }
        }
        TransactionStore.TransactionMap<String, Document> result = this.transactionalContentById.get(currentTxId);
        if (result == null) {
            if (failIfMissing) {
                throw new FileProviderException("No MV store transaction was found for tx id '" + currentTxId +"'");
            } else {
                LOGGER.debug(
                        "Found active ModeShape transaction '{0}' without a corresponding MV store transaction; most likely this has been committed off a separate thread",
                        currentTxId);
                ACTIVE_TX_ID.remove();
            }
        }
        return result;
    }
}
