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
import java.util.Set;
import java.util.concurrent.ExecutionException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;
import org.infinispan.Cache;
import org.infinispan.distexec.DistributedCallable;
import org.infinispan.schematic.Schematic;
import org.infinispan.schematic.SchematicDb;
import org.infinispan.schematic.SchematicEntry;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableDocument;
import org.modeshape.common.SystemFailureException;
import org.modeshape.jcr.InfinispanUtil;
import org.modeshape.jcr.InfinispanUtil.Combiner;
import org.modeshape.jcr.InfinispanUtil.Location;
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
    private String localSourceKey;

    /**
     * Creates a new local store with the given database
     * 
     * @param database a {@link SchematicDb} instance which must be non-null.
     */
    public LocalDocumentStore( SchematicDb database ) {
        this.database = database;
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
        return putIfAbsent(key, document);
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
     * @return the existing entry for the supplied key, or null if there was no entry and the put was successful
     * @see SchematicDb#putIfAbsent(String, org.infinispan.schematic.document.Document)
     */
    public SchematicEntry putIfAbsent( String key,
                                       Document document ) {
        return database.putIfAbsent(key, document);
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

    /**
     * Replace the existing document and metadata at the given key with the document that is supplied. This method does nothing if
     * there is not an existing entry at the given key.
     * 
     * @param key the key or identifier for the document
     * @param document the new document that is to replace the existing document (or binary content) the replacement
     */
    public void replace( String key,
                         Document document ) {
        database.replace(key, document);
    }

    @Override
    public boolean remove( String key ) {
        return database.remove(key) != null;
    }

    @Override
    public boolean lockDocuments( Collection<String> keys ) {
        return database.lock(keys);
    }

    @Override
    public EditableDocument edit( String key,
                                  boolean createIfMissing ) {
        return database.editContent(key, createIfMissing);
    }

    @Override
    public EditableDocument edit( String key,
                                  boolean createIfMissing,
                                  boolean acquireLock ) {
        return database.editContent(key, createIfMissing, acquireLock);
    }

    @Override
    public LocalDocumentStore localStore() {
        return this;
    }

    @Override
    public TransactionManager transactionManager() {
        return localCache().getAdvancedCache().getTransactionManager();
    }

    @Override
    public XAResource xaResource() {
        return localCache().getAdvancedCache().getXAResource();
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
     * @throws InterruptedException if the process is interrupted
     * @throws ExecutionException if there is an error while getting executing the operation
     */
    public DocumentOperationResults performOnEachDocument( DocumentOperation operation )
        throws InterruptedException, ExecutionException {
        DistributedOperation distOp = new DistributedOperation(operation);
        return InfinispanUtil.execute(database.getCache(), Location.LOCALLY, distOp, distOp);
    }

    /**
     * An operation upon a persisted document.
     */
    public static abstract class DocumentOperation implements Serializable {
        private static final long serialVersionUID = 1L;
        protected Cache<String, SchematicEntry> cache;

        /**
         * Invoked by execution environment after the operation has been migrated for execution to a specific Infinispan node.
         * 
         * @param cache cache whose keys are used as input data for this DistributedCallable task
         */
        public void setEnvironment( Cache<String, SchematicEntry> cache ) {
            this.cache = cache;
        }

        /**
         * Execute the operation upon the given {@link EditableDocument}.
         * 
         * @param key the document's key; never null
         * @param document the editable document; never null
         * @return true if the operation modified the document, or false otherwise
         */
        public abstract boolean execute( String key,
                                         EditableDocument document );
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

    protected static class DistributedOperation
        implements DistributedCallable<String, SchematicEntry, DocumentOperationResults>, Serializable,
        Combiner<DocumentOperationResults> {
        private static final long serialVersionUID = 1L;

        private transient SchematicDb db;
        private transient Set<String> inputKeys;
        private transient TransactionManager txnMgr;
        private transient DocumentOperation operation;

        protected DistributedOperation( DocumentOperation operation ) {
            this.operation = operation;
        }

        @Override
        public void setEnvironment( Cache<String, SchematicEntry> cache,
                                    Set<String> inputKeys ) {
            assert cache != null;
            assert inputKeys != null;
            this.db = Schematic.get(cache);
            this.inputKeys = inputKeys;
            this.txnMgr = cache.getAdvancedCache().getTransactionManager();
            this.operation.setEnvironment(cache);
        }

        @Override
        public DocumentOperationResults call() throws Exception {
            DocumentOperationResults results = new DocumentOperationResults();
            for (String key : inputKeys) {
                // We operate upon each document within a transaction ...
                try {
                    txnMgr.begin();
                    EditableDocument doc = db.editContent(key, false, true);
                    if (doc != null) {
                        if (operation.execute(key, doc)) {
                            results.recordModified();
                        } else {
                            results.recordUnmodified();
                        }
                    }
                    txnMgr.commit();
                } catch (org.infinispan.util.concurrent.TimeoutException e) {
                    // Couldn't wait long enough for the lock, so skip this for now ...
                    results.recordSkipped();
                } catch (NotSupportedException err) {
                    // No nested transactions are supported ...
                    results.recordFailure();
                    throw new SystemFailureException(err);
                } catch (SecurityException err) {
                    // No privilege to commit ...
                    results.recordFailure();
                    throw new SystemFailureException(err);
                } catch (IllegalStateException err) {
                    // Not associated with a txn??
                    results.recordFailure();
                    throw new SystemFailureException(err);
                } catch (RollbackException err) {
                    // Couldn't be committed, but the txn is already rolled back ...
                    results.recordFailure();
                } catch (HeuristicMixedException err) {
                    // Rollback has occurred ...
                    results.recordFailure();
                } catch (HeuristicRollbackException err) {
                    // Rollback has occurred ...
                    results.recordFailure();
                } catch (SystemException err) {
                    // System failed unexpectedly ...
                    results.recordFailure();
                    throw new SystemFailureException(err);
                } catch (Throwable t) {
                    // any other exception/error we should rollback and just continue (skipping this key for now) ...
                    txnMgr.rollback();
                    results.recordFailure();
                    continue;
                }
            }
            return results;
        }

        @Override
        public DocumentOperationResults combine( DocumentOperationResults priorResult,
                                                 DocumentOperationResults newResult ) {
            return priorResult.combine(newResult);
        }
    }
}
