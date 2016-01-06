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

package org.modeshape.jcr.cache.document;

import java.io.Serializable;
import java.util.Collection;
import java.util.function.BiFunction;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import org.infinispan.Cache;
import org.infinispan.schematic.SchematicDb;
import org.infinispan.schematic.SchematicEntry;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableDocument;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.RepositoryEnvironment;
import org.modeshape.jcr.locking.LockingService;
import org.modeshape.jcr.txn.Transactions;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.binary.ExternalBinaryValue;

/**
 * An implementation of {@link DocumentStore} which always uses the local cache to store/retrieve data and which provides some
 * additional methods for exposing local cache information.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class LocalDocumentStore implements DocumentStore {

    private final SchematicDb database;
    private final RepositoryEnvironment repoEnv;
    private String localSourceKey;

    /**
     * Creates a new local store with the given database
     *
     * @param database a {@link SchematicDb} instance which must be non-null.
     * @param repoEnv a {@link RepositoryEnvironment} instance which must be non-null
     */
    public LocalDocumentStore(SchematicDb database, RepositoryEnvironment repoEnv) {
        CheckArg.isNotNull(database, "database");
        this.database = database;
        CheckArg.isNotNull(repoEnv, "repoEnv");
        this.repoEnv = repoEnv;
    }

    @Override
    public boolean containsKey( String key ) {
        return database.containsKey(key);
    }

    @Override
    public SchematicEntry get( String key ) {
        return database.get(key);
    }

    @Override
    public SchematicEntry storeDocument( String key,
                                         Document document ) {
        return database.putIfAbsent(key, document);
    }

    @Override
    public void updateDocument( String key,
                                Document document,
                                SessionNode sessionNode ) {
        // do nothing, the way the local store updates is via editing schematic entry literals
    }

    @Override
    public String newDocumentKey( String parentKey,
                                  Name documentName,
                                  Name documentPrimaryType ) {
        // the local store doesn't generate explicit keys for new nodes
        return null;
    }

    /**
     * Store the supplied document and metadata at the given key.
     * 
     * @param key the key or identifier for the document
     * @param document the document that is to be stored
     * @see SchematicDb#put(String, org.infinispan.schematic.document.Document)
     */
    public void put( String key,
                     Document document ) {
        database.put(key, document);
    }

    /**
     * Store the supplied document in the local db
     * 
     * @param entryDocument the document that contains the metadata document, content document, and key
     */
    public void put( Document entryDocument ) {
        database.put(entryDocument);
    }

    @Override
    public boolean remove( String key ) {
        return database.remove(key) != null;
    }

    @Override
    public boolean lockDocuments( Collection<String> keys ) {
        return lockDocuments(keys.toArray(new String[keys.size()]));
    }

    @Override
    public boolean lockDocuments(String... keys) {
        Transactions.Transaction tx = repoEnv.getTransactions().currentTransaction();
        if (tx == null) {
            throw new IllegalStateException("Cannot attempt to lock documents without an existing ModeShape transaction");
        }
        try {
            LockingService lockingService = repoEnv.lockingService();
            boolean locked = lockingService.tryLock(keys);
            if (locked) {
                tx.uponCompletion(() -> lockingService.unlock(keys));
            }
            return locked;
        } catch (RuntimeException rt) {
            throw rt;            
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public EditableDocument edit( String key,
                                  boolean createIfMissing ) {
        return database.editContent(key, createIfMissing);
    }

    @Override
    public LocalDocumentStore localStore() {
        return this;
    }

    @Override
    public void setLocalSourceKey( String sourceKey ) {
        this.localSourceKey = sourceKey;
    }

    @Override
    public String getLocalSourceKey() {
        return this.localSourceKey;
    }

    @Override
    public String createExternalProjection( String projectedNodeKey,
                                            String sourceName,
                                            String externalPath,
                                            String alias ) {
        throw new UnsupportedOperationException("External projections are not supported in the local document store");
    }

    @Override
    public Document getChildrenBlock( String key ) {
        // Look up the information in the database ...
        SchematicEntry entry = get(key);
        if (entry == null) {
            // There is no such node ...
            return null;
        }
        return entry.getContent();
    }

    @Override
    public Document getChildReference( String parentKey,
                                       String childKey ) {
        return null; // don't support this
    }

    /**
     * Returns the local Infinispan cache.
     * 
     * @return a {@code non-null} {@link Cache} instance.
     */
    public Cache<String, SchematicEntry> localCache() {
        return database.getCache();
    }

    @Override
    public ExternalBinaryValue getExternalBinary( String sourceName,
                                                  String id ) {
        throw new UnsupportedOperationException("External binaries are only supported by the federated document store");
    }

    /**
     * Perform the supplied operation on each stored document that is accessible within this process. Each document will be
     * operated upon in a separate transaction, which will be committed if the operation is successful or rolledback if the
     * operation cannot be complete successfully.
     * <p>
     * Generally, this method executes the operation upon all documents. If there is an error processing a single document, that
     * document is skipped and the execution will continue with the next document(s). However, if there is an exception with the
     * transactions or another system failure, this method will terminate with an exception.
     * 
     * @param operation the operation to be performed
     * @return the summary of the number of documents that were affected
     */
    public DocumentOperationResults performOnEachDocument( BiFunction<String, EditableDocument, Boolean> operation ) {
        DocumentOperationResults results = new DocumentOperationResults();
        Transactions transactions = repoEnv.getTransactions();
        database.keys().forEach(key -> {
            // We operate upon each document within a transaction ...
            try {
                transactions.begin();
                lockDocuments(key);
                EditableDocument doc = edit(key, false);
                if (doc != null) {
                    if (operation.apply(key, doc)) {
                        results.recordModified();
                    } else {
                        results.recordUnmodified();
                    }
                }
                transactions.commit();
            } catch (RollbackException | HeuristicMixedException | HeuristicRollbackException err) {
                // Couldn't be committed, but the txn is already rolled back ...
                results.recordFailure();
            } catch (Throwable t) {
                results.recordFailure();
                // any other exception/error we should rollback and just continue (skipping this key for now) ...
                try {
                    transactions.rollback();
                } catch (SystemException e) {
                    //ignore....
                }
            }
        });
        return results;
    }

    public static class DocumentOperationResults implements Serializable {
        private static final long serialVersionUID = 1L;

        private long modifiedCount;
        private long unmodifiedCount;
        private long skipCount;
        private long failureCount;

        /**
         * Return the number of documents that were successfully updated/modified by the operation.
         * 
         * @return the number of modified documents
         */
        public long getModifiedCount() {
            return modifiedCount;
        }

        /**
         * Return the number of documents that were not updated/modified by the operation.
         * 
         * @return the number of unmodified documents
         */
        public long getUnmodifiedCount() {
            return unmodifiedCount;
        }

        /**
         * Return the number of documents that caused some failure.
         * 
         * @return the number of failed documents
         */
        public long getFailureCount() {
            return failureCount;
        }

        /**
         * Return the number of documents that were skipped by the operation because the document could not be obtained in an
         * timely fashion.
         * 
         * @return the number of skipped documents
         */
        public long getSkipCount() {
            return skipCount;
        }

        protected void recordModified() {
            ++modifiedCount;
        }

        protected void recordUnmodified() {
            ++unmodifiedCount;
        }

        protected void recordFailure() {
            ++failureCount;
        }

        protected void recordSkipped() {
            ++skipCount;
        }

        protected DocumentOperationResults combine( DocumentOperationResults other ) {
            if (other != null) {
                this.modifiedCount += other.modifiedCount;
                this.unmodifiedCount += other.unmodifiedCount;
                this.skipCount += other.skipCount;
                this.failureCount += other.failureCount;
            }
            return this;
        }

        @Override
        public String toString() {
            return "" + modifiedCount + " documents changed, " + unmodifiedCount + " unchanged, " + skipCount + " skipped, and "
                   + failureCount + " resulted in errors or failures";
        }
    }
}
