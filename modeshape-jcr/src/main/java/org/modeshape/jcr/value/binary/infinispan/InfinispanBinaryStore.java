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
package org.modeshape.jcr.value.binary.infinispan;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.transaction.TransactionManager;
import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.StoreConfiguration;
import org.infinispan.configuration.cache.TransactionConfiguration;
import org.infinispan.context.Flag;
import org.infinispan.distexec.mapreduce.Collector;
import org.infinispan.distexec.mapreduce.MapReduceTask;
import org.infinispan.distexec.mapreduce.Mapper;
import org.infinispan.distexec.mapreduce.Reducer;
import org.infinispan.filter.KeyFilter;
import org.infinispan.manager.CacheContainer;
import org.infinispan.marshall.core.MarshalledEntry;
import org.infinispan.persistence.manager.PersistenceManager;
import org.infinispan.persistence.spi.AdvancedCacheLoader;
import org.infinispan.persistence.spi.AdvancedCacheLoader.CacheLoaderTask;
import org.infinispan.transaction.LockingMode;
import org.infinispan.transaction.TransactionMode;
import org.modeshape.common.SystemFailureException;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.util.IoUtil;
import org.modeshape.common.util.SecureHash;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.binary.AbstractBinaryStore;
import org.modeshape.jcr.value.binary.BinaryStoreException;
import org.modeshape.jcr.value.binary.NamedLocks;
import org.modeshape.jcr.value.binary.StoredBinaryValue;

/**
 * A {@link org.modeshape.jcr.value.binary.BinaryStore} implementation that uses Infinispan for persisting binary values.
 */
@ThreadSafe
public final class InfinispanBinaryStore extends AbstractBinaryStore {

    public static final int DEFAULT_CHUNK_SIZE = 1024 * 1024 * 1; // 1 MB

    private static final String META_SUFFIX = "-meta";
    private static final String DATA_SUFFIX = "-data";
    private static final String TEXT_SUFFIX = "-text";
    private static final int SUFFIX_LENGTH = 5;

    private static final int MIN_KEY_LENGTH = BinaryKey.maxHexadecimalLength() + SUFFIX_LENGTH;
    private static final int MAX_KEY_LENGTH = MIN_KEY_LENGTH;

    protected Cache<String, Metadata> metadataCache;
    protected LockFactory lockFactory;
    protected Cache<String, byte[]> blobCache;
    private CacheContainer cacheContainer;
    private boolean dedicatedCacheContainer;
    private int chunkSize;

    private String metadataCacheName;
    private String blobCacheName;

    /**
     * Creates a new instance.
     * 
     * @param cacheContainer cache container which used for cache management
     * @param dedicatedCacheContainer true if the cache container should be started/stopped when store is start or stopped
     * @param metadataCacheName name of the cache used for metadata
     * @param blobCacheName name of the cache used for store of chunked binary values
     */
    public InfinispanBinaryStore( CacheContainer cacheContainer,
                                  boolean dedicatedCacheContainer,
                                  String metadataCacheName,
                                  String blobCacheName ) {
        this(cacheContainer, dedicatedCacheContainer, metadataCacheName, blobCacheName, DEFAULT_CHUNK_SIZE);
    }

    /**
     * Creates a new instance.
     * 
     * @param cacheContainer cache container which used for cache management
     * @param dedicatedCacheContainer true if the cache container should be started/stopped when store is start or stopped
     * @param metadataCacheName name of the cache used for metadata
     * @param blobCacheName name of the cache used for store of chunked binary values
     * @param chunkSize the size (in bytes) of a chunk
     */
    public InfinispanBinaryStore( CacheContainer cacheContainer,
                                  boolean dedicatedCacheContainer,
                                  String metadataCacheName,
                                  String blobCacheName,
                                  int chunkSize ) {
        this.cacheContainer = cacheContainer;
        this.dedicatedCacheContainer = dedicatedCacheContainer;
        this.metadataCacheName = metadataCacheName;
        this.blobCacheName = blobCacheName;
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("Invalid chunk size:" + chunkSize);
        }
        this.chunkSize = chunkSize;
    }

    protected final String lockKeyFrom( BinaryKey key ) {
        return key.toString();
    }

    protected final String metadataKeyFrom( BinaryKey key ) {
        return key.toString() + META_SUFFIX;
    }

    protected final String dataKeyFrom( BinaryKey key ) {
        return key.toString() + DATA_SUFFIX;
    }

    protected final String textKeyFrom( BinaryKey key ) {
        return key.toString() + TEXT_SUFFIX;
    }

    protected final boolean isMetadataKey( String str ) {
        if (str == null) return false;
        int len = str.length();
        if (len < MIN_KEY_LENGTH || len > MAX_KEY_LENGTH) return false;
        if (!str.endsWith(META_SUFFIX)) return false;
        String key = str.substring(0, len - SUFFIX_LENGTH);
        return BinaryKey.isProperlyFormattedKey(key);
    }

    protected final BinaryKey binaryKeyFromCacheKey( String key ) {
        String plainKey;

        if (isMetadataKey(key)) {
            plainKey = key.replace(META_SUFFIX, "");
        } else if (key.contains(DATA_SUFFIX)) {
            plainKey = key.replaceFirst(DATA_SUFFIX + "-\\d+$", "");
        } else if (key.contains(TEXT_SUFFIX)) {
            plainKey = key.replaceFirst(TEXT_SUFFIX + "-\\d+$", "");
        } else {
            plainKey = key;
        }

        return new BinaryKey(plainKey);
    }

    @Override
    public void start() {
        logger.debug("start()");
        if (metadataCache != null) {
            logger.debug("Already started.");
            return;
        }
        if (dedicatedCacheContainer) {
            cacheContainer.start();
        }
        metadataCache = cacheContainer.getCache(metadataCacheName);
        blobCache = cacheContainer.getCache(blobCacheName);
        lockFactory = new LockFactory(metadataCache);
    }

    @Override
    public void shutdown() {
        try {
            if (dedicatedCacheContainer) {
                cacheContainer.stop();
            }
        } finally {
            cacheContainer = null;
            metadataCache = null;
            blobCache = null;
        }
    }

    public List<Cache<?, ?>> getCaches() {
        List<Cache<?, ?>> caches = new ArrayList<Cache<?, ?>>(2);
        if (!dedicatedCacheContainer) {
            if (metadataCache != null) {
                caches.add(metadataCache);
            }
            if (blobCache != null) {
                caches.add(blobCache);
            }
        }
        return caches;
    }

    private void putMetadata( final String metadataKey,
                              final Metadata metadata ) throws IOException {
        new RetryOperation() {
            @Override
            protected boolean call() {
                metadataCache.put(metadataKey, metadata);
                return true;
            }
        }.doTry();
    }

    @Override
    public BinaryValue storeValue( InputStream inputStream, boolean markAsUnused ) throws BinaryStoreException, SystemFailureException {
        File tmpFile = null;
        try {
            // using tmp file to determine SHA1
            SecureHash.HashingInputStream hashingStream = SecureHash.createHashingStream(SecureHash.Algorithm.SHA_1, inputStream);
            tmpFile = File.createTempFile("ms-ispn-binstore", "hashing");
            IoUtil.write(hashingStream, new BufferedOutputStream(new FileOutputStream(tmpFile)),
                         AbstractBinaryStore.MEDIUM_BUFFER_SIZE);
            final BinaryKey binaryKey = new BinaryKey(hashingStream.getHash());

            // check if binary data already exists
            final String metadataKey = metadataKeyFrom(binaryKey);
            Metadata metadata = metadataCache.get(metadataKey);
            if (metadata != null) {
                logger.debug("Binary value already exist.");
                // in case of an unused entry, this entry is from now used
                if (metadata.isUnused() && !markAsUnused) {
                    metadata.markAsUsed();
                    putMetadata(metadataKey, metadata);
                }
                return new StoredBinaryValue(this, binaryKey, metadata.getLength());
            }

            logger.debug("Store binary value into chunks.");
            // store the chunks based referenced to SHA1-key
            // we do store outside of transaction to prevent problems with
            // large content and transaction timeouts
            final String dataKey = dataKeyFrom(binaryKey);
            final long lastModified = tmpFile.lastModified();
            final long fileLength = tmpFile.length();
            int bufferSize = bestBufferSize(fileLength);
            ChunkOutputStream chunkOutputStream = new ChunkOutputStream(blobCache, dataKey, chunkSize);
            IoUtil.write(new FileInputStream(tmpFile), chunkOutputStream, bufferSize);

            Lock lock = lockFactory.writeLock(lockKeyFrom(binaryKey));
            try {
                // now store metadata
                metadata = new Metadata(lastModified, fileLength, chunkOutputStream.chunksCount(), chunkSize);
                if (markAsUnused) {
                    metadata.markAsUnusedSince(System.currentTimeMillis());
                }
                putMetadata(metadataKey, metadata);
                return new StoredBinaryValue(this, binaryKey, fileLength);
            } finally {
                lock.unlock();
            }
        } catch (IOException e) {
            throw new BinaryStoreException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new SystemFailureException(e);
        } finally {
            try {
                IoUtil.closeQuietly(inputStream);
            } finally {
                if (tmpFile != null) tmpFile.delete();
            }
        }
    }

    @Override
    public InputStream getInputStream( BinaryKey binaryKey ) throws BinaryStoreException {
        Metadata metadata = metadataCache.get(metadataKeyFrom(binaryKey));
        if (metadata == null) {
            throw new BinaryStoreException(JcrI18n.unableToFindBinaryValue.text(binaryKey,
                                                                                "Infinispan cache " + metadataCache.getName()));
        }
        if (metadata.getLength() == 0) {
            return new ByteArrayInputStream(new byte[0]);
        }
        return new ChunkInputStream(blobCache, dataKeyFrom(binaryKey), metadata.getChunkSize(), metadata.getLength());
    }

    @Override
    public void markAsUsed( Iterable<BinaryKey> keys ) throws BinaryStoreException {
        for (BinaryKey binaryKey : keys) {
            Lock lock = lockFactory.writeLock(lockKeyFrom(binaryKey));
            try {
                final String metadataKey = metadataKeyFrom(binaryKey);
                final Metadata metadata = metadataCache.get(metadataKey);
                // we use the copy of the original object to avoid changes cache values in case of errors
                if (metadata == null) {
                    continue;
                }
                metadata.markAsUsed();
                putMetadata(metadataKey, metadata);
            } catch (IOException e) {
                logger.debug(e, "Error during mark binary value used {0}", binaryKey);
                throw new BinaryStoreException(JcrI18n.errorMarkingBinaryValuesUnused.text(e.getCause().getMessage()), e);
            } finally {
                lock.unlock();
            }
        }
    }

    @Override
    public void markAsUnused( Iterable<BinaryKey> keys ) throws BinaryStoreException {
        for (BinaryKey binaryKey : keys) {
            // Try to mark the metadata as unused. We loop here in case other processes (not other threads in this process,
            // which are handled via locks) are doing the same thing.
            Lock lock = lockFactory.writeLock(lockKeyFrom(binaryKey));
            try {
                final String metadataKey = metadataKeyFrom(binaryKey);
                final Metadata metadata = metadataCache.get(metadataKey);
                // we use the copy of the original object to avoid changes cache values in case of errors
                if (metadata == null || metadata.isUnused()) {
                    continue;
                }
                metadata.markAsUnusedSince(System.currentTimeMillis());
                putMetadata(metadataKey, metadata);
            } catch (IOException ex) {
                logger.debug(ex, "Error during mark binary value unused {0}", binaryKey);
                throw new BinaryStoreException(JcrI18n.errorMarkingBinaryValuesUnused.text(ex.getCause().getMessage()), ex);
            } finally {
                lock.unlock();
            }
        }
    }

    @Override
    public void removeValuesUnusedLongerThan( long minimumAge,
                                              TimeUnit unit ) throws BinaryStoreException {
        // This method is called on every cluster node. So in case of distributed cache
        // a MapReduce processes is initiated by the coordinator. Also the processing of the
        // entries inside cache store are done by the coordinator. Only in case of non-shared
        // cache store the local cache store is processed by every cluster node.
        //
        // todo what about GC thread interruption?

        // determine type of cache store
        List<StoreConfiguration> storeConfigurations = metadataCache.getCacheConfiguration().persistence().stores();
        boolean cacheLoaderShared = storeConfigurations != null && !storeConfigurations.isEmpty()
                                    && storeConfigurations.get(0).shared();
        boolean isCoordinator = metadataCache.getCacheManager().isCoordinator();

        if (!isCoordinator && cacheLoaderShared) {
            // in this case an other node will care...
            return;
        }

        final long minimumAgeInMS = unit.toMillis(minimumAge);
        if (metadataCache.getCacheConfiguration().clustering().cacheMode().isDistributed() && isCoordinator) {
            // distributed mapper finds unused...
            MapReduceTask<String, Metadata, String, String> task = new MapReduceTask<String, Metadata, String, String>(
                                                                                                                       metadataCache);
            task.mappedWith(new UnusedMapper(minimumAgeInMS));
            task.reducedWith(new DummyReducer());
            Map<String, String> result = task.execute();
            for (String key : result.values()) {
                InfinispanBinaryStore.Lock lock = lockFactory.writeLock(key);
                try {
                    removeUnusedBinaryValue(metadataCache, blobCache, key);
                } finally {
                    lock.unlock();
                }
            }
        } else {
            // local / repl cache
            // process entries in memory
            final Set<Object> processedKeys = new HashSet<Object>();
            for (String key : metadataCache.keySet()) {
                if (!isMetadataKey(key)) continue;
                Metadata metadata = metadataCache.get(key);
                processedKeys.add(key);
                if (isValueUnused(metadata, minimumAgeInMS)) {
                    InfinispanBinaryStore.Lock lock = lockFactory.writeLock(key);
                    try {
                        removeUnusedBinaryValue(metadataCache, blobCache, key);
                    } finally {
                        lock.unlock();
                    }
                }
            }
            // process stored entries
            PersistenceManager persistenceManager = metadataCache.getAdvancedCache().getComponentRegistry()
                                                                 .getComponent(PersistenceManager.class);
            if (isCoordinator && persistenceManager != null) {
                // process cache loader content
                CacheLoaderTask<Object, Object> task = new CacheLoaderTask<Object, Object>() {
                    @Override
                    public void processEntry( MarshalledEntry<Object, Object> marshalledEntry,
                                              AdvancedCacheLoader.TaskContext taskContext ) {
                        Object key = marshalledEntry.getKey();
                        if (!(key instanceof String)) {
                            return;
                        }
                        if (!isMetadataKey((String)key)) {
                            return;
                        }
                        if (processedKeys.contains(key)) {
                            return;
                        }
                        Metadata metadata = metadataCache.get(key);
                        if (isValueUnused(metadata, minimumAgeInMS)) {
                            try {
                                Lock lock = lockFactory.writeLock((String)key);
                                try {
                                    removeUnusedBinaryValue(metadataCache, blobCache, (String)key);
                                } finally {
                                    lock.unlock();
                                }
                            } catch (BinaryStoreException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                };
                persistenceManager.processOnAllStores(KeyFilter.ACCEPT_ALL_FILTER, task, false, false);

            }
        }
    }

    static boolean isValueUnused( Metadata metadata,
                                  long minimumAgeInMS ) {
        if (metadata == null || !metadata.isUnused()) {
            return false;
        }
        return System.currentTimeMillis() - metadata.unusedSince() > minimumAgeInMS;
    }

    static void removeUnusedBinaryValue( final Cache<String, Metadata> metadataCache,
                                         final Cache<String, byte[]> blobCache,
                                         final String metadataKey ) {
        Metadata metadata = metadataCache.get(metadataKey);
        // double check != null
        if (metadata == null || !metadata.isUnused()) {
            return;
        }
        // the metadata entry itself, and do this first in case there's an error
        metadataCache.remove(metadataKey);
        // Remove the metadata suffix ...
        final String key = metadataKey.replace(META_SUFFIX, "");
        // remove chunks (if any)
        if (metadata.getNumberChunks() > 0) {
            for (int chunkIndex = 0; chunkIndex < metadata.getNumberChunks(); chunkIndex++) {
                blobCache.remove(key + DATA_SUFFIX + "-" + chunkIndex);
            }
        }
        if (metadata.getNumberTextChunks() > 0) {
            for (int chunkIndex = 0; chunkIndex < metadata.getNumberTextChunks(); chunkIndex++) {
                blobCache.remove(key + TEXT_SUFFIX + "-" + chunkIndex);
            }
        }
    }

    private static class UnusedMapper implements Mapper<String, Metadata, String, String> {
        private static final long serialVersionUID = 1L;
        private long minimumAgeInMS;

        public UnusedMapper( long minimumAgeInMS ) {
            this.minimumAgeInMS = minimumAgeInMS;
        }

        @Override
        public void map( String key,
                         Metadata metadata,
                         Collector<String, String> stringCollector ) {
            if (isValueUnused(metadata, minimumAgeInMS)) {
                stringCollector.emit(key, key);
            }
        }
    }

    @Override
    protected String getStoredMimeType( BinaryValue binary ) throws BinaryStoreException {
        BinaryKey key = binary.getKey();
        Metadata metadata = metadataCache.get(metadataKeyFrom(key));
        if (metadata == null) {
            String msg = JcrI18n.unableToFindBinaryValueInCache.text(key, metadataCache.getName());
            throw new BinaryStoreException(JcrI18n.errorStoringMimeType.text(msg));
        }
        return metadata.getMimeType();
    }

    @Override
    protected void storeMimeType( final BinaryValue binary,
                                  String mimeType ) throws BinaryStoreException {
        final BinaryKey key = binary.getKey();
        Lock lock = lockFactory.writeLock(lockKeyFrom(key));
        try {
            final String metadataKeyStr = metadataKeyFrom(key);
            Metadata metadata = metadataCache.get(metadataKeyStr);
            if (metadata == null) {
                String msg = JcrI18n.unableToFindBinaryValueInCache.text(key, metadataCache.getName());
                throw new BinaryStoreException(JcrI18n.errorStoringMimeType.text(msg));
            }
            // Note that it's okay if another process intercedes at this point, because it should be idempotent ...
            putMetadata(metadataKeyStr, metadata.withMimeType(mimeType));
        } catch (IOException ex) {
            logger.debug(ex, "Error during store of mime type for {0}", key);
            throw new BinaryStoreException(JcrI18n.errorStoringMimeType.text(ex.getCause().getMessage()));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String getExtractedText( BinaryValue binary ) throws BinaryStoreException {
        final BinaryKey key = binary.getKey();
        final String metadataKeyStr = metadataKeyFrom(key);
        Metadata metadata = metadataCache.get(metadataKeyStr);
        if (metadata == null) {
            String msg = JcrI18n.unableToFindBinaryValueInCache.text(key, metadataCache.getName());
            throw new BinaryStoreException(JcrI18n.errorStoringMimeType.text(msg));
        }
        if (metadata.getNumberTextChunks() == 0) {
            return null;
        }
        try {
            final String textKey = textKeyFrom(key);
            return IoUtil.read(new ChunkInputStream(blobCache, textKey, metadata.getChunkSize(), metadata.getLength()), "UTF-8");
        } catch (IOException ex) {
            logger.debug(ex, "Error during read of extracted text for {0}", key);
            throw new BinaryStoreException(JcrI18n.errorReadingExtractedText.text(ex.getCause().getMessage()));
        }
    }

    @Override
    public void storeExtractedText( final BinaryValue binary,
                                    String extractedText ) throws BinaryStoreException {
        final BinaryKey key = binary.getKey();
        Lock lock = lockFactory.writeLock(lockKeyFrom(key));
        try {
            final String metadataKey = metadataKeyFrom(key);
            final Metadata metadata = metadataCache.get(metadataKey);
            if (metadata == null) {
                String msg = JcrI18n.unableToFindBinaryValueInCache.text(key, metadataCache.getName());
                throw new BinaryStoreException(JcrI18n.errorStoringMimeType.text(msg));
            }
            // Note that it's okay if another process intercedes at this point, because it should be idempotent ...
            final String textKey = textKeyFrom(key);
            ChunkOutputStream chunkOutputStream = null;
            try {
                chunkOutputStream = new ChunkOutputStream(blobCache, textKey, chunkSize);
                chunkOutputStream.write(extractedText.getBytes("UTF-8"));
            } finally {
                IoUtil.closeQuietly(chunkOutputStream);
            }
            putMetadata(metadataKey, metadata.withNumberOfTextChunks(chunkOutputStream.chunksCount()));
        } catch (IOException ex) {
            logger.debug(ex, "Error during store of extracted text for {0}", key);
            throw new BinaryStoreException(JcrI18n.errorStoringExtractedText.text(ex.getCause().getMessage()));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Iterable<BinaryKey> getAllBinaryKeys() throws BinaryStoreException {
        final Set<BinaryKey> allBinaryUsedKeys = new HashSet<BinaryKey>();

        try {
            for (String key : metadataCache.keySet()) {
                if (!isMetadataKey(key)) { continue; }
                Metadata metadata = metadataCache.get(key);
                if (!metadata.isUnused()) {
                    allBinaryUsedKeys.add(binaryKeyFromCacheKey(key));
                }
            }

            PersistenceManager persistenceManager = metadataCache.getAdvancedCache().getComponentRegistry()
                                                                 .getComponent(PersistenceManager.class);
            if (persistenceManager != null) {
                // process cache loader content
                CacheLoaderTask<Object, Object> task = new CacheLoaderTask<Object, Object>() {
                    @Override
                    public void processEntry( MarshalledEntry<Object, Object> marshalledEntry,
                                              AdvancedCacheLoader.TaskContext taskContext ) {
                        Object key = marshalledEntry.getKey();
                        if (!(key instanceof String)) {
                            return;
                        }
                        String keyString = key.toString();
                        if (!isMetadataKey(keyString)) {
                            return;
                        }
                        BinaryKey binaryKey = binaryKeyFromCacheKey(keyString);
                        if (allBinaryUsedKeys.contains(binaryKey)) {
                            return;
                        }
                        Metadata metadata = metadataCache.get(key);
                        if (!metadata.isUnused()) {
                            allBinaryUsedKeys.add(binaryKey);
                        }
                    }
                };
                persistenceManager.processOnAllStores(KeyFilter.ACCEPT_ALL_FILTER, task, false, false);
            }
        } catch (Exception ex) {
            throw new BinaryStoreException(JcrI18n.problemsGettingBinaryKeysFromBinaryStore.text(ex.getCause().getMessage()));
        }
        return allBinaryUsedKeys;
    }

    /**
     * Locks are created based upon metadata cache configuration
     */
    @SuppressWarnings( "synthetic-access" )
    class LockFactory {

        private final NamedLocks namedLocks;
        private final boolean infinispanLocks;
        private final Cache<String, Metadata> metadataCache;
        private final Lock DUMMY_LOCK = new Lock() {
            @Override
            public void unlock() {
            }
        };

        public LockFactory( Cache<String, Metadata> metadataCache ) {
            this.metadataCache = metadataCache;
            if (this.metadataCache != null) {
                TransactionConfiguration txCfg = metadataCache.getCacheConfiguration().transaction();
                infinispanLocks = txCfg.transactionMode() != TransactionMode.NON_TRANSACTIONAL && txCfg.lockingMode() == LockingMode.PESSIMISTIC;
                namedLocks = !infinispanLocks && !metadataCache.getCacheConfiguration().clustering().cacheMode().isClustered() ? new NamedLocks() : null;
                if (infinispanLocks && logger.isTraceEnabled()) {
                    logger.trace("Detected PESSIMISTIC & TRANSACTIONAL binary cache configuration. ISPN locks will be used when storing binaries");
                } else if (!infinispanLocks && logger.isTraceEnabled()) {
                    logger.trace("Binary cache is not configured as PESSIMISTIC & TRANSACTIONAL. Will use JDK locks for storing binaries.");
                }
            } else {
                namedLocks = null;
                infinispanLocks = false;
                if (logger.isTraceEnabled()) {
                    logger.trace("Not metadata cache configuration found. No locking will be used for storing binaries.");
                }
            }
        }

        public Lock readLock( String key ) throws BinaryStoreException {
            if (namedLocks != null) {
                return new NamedLock(namedLocks.readLock(key));
            } else if (infinispanLocks) {
                return new ISPNLock(metadataCache, key);
            } else {
                return DUMMY_LOCK;
            }
        }

        public Lock writeLock( String key ) throws BinaryStoreException {
            if (namedLocks != null) {
                return new NamedLock(namedLocks.writeLock(key));
            } else if (infinispanLocks) {
                return new ISPNLock(metadataCache, key);
            } else {
                return DUMMY_LOCK;
            }
        }

        private class NamedLock implements Lock {

            private final java.util.concurrent.locks.Lock lock;

            public NamedLock( java.util.concurrent.locks.Lock lock ) {
                this.lock = lock;
            }

            @Override
            public void unlock() {
                lock.unlock();
            }
        }

        private class ISPNLock implements Lock {

            private final AdvancedCache<String, Metadata> cache;
            private final String key;
            private final boolean transactionStarted;

            public ISPNLock( Cache<String, Metadata> cache,
                             String key ) throws BinaryStoreException {
                this.cache = cache.getAdvancedCache().withFlags(Flag.FAIL_SILENTLY);
                this.key = key;
                try {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Attempting to lock binary key {0} via the ISPN cache", key);
                    }
                    TransactionManager transactionManager = this.cache.getTransactionManager();
                    if (transactionManager.getTransaction() == null) {
                        transactionManager.begin();
                        transactionStarted = true;
                        if (logger.isTraceEnabled()) {
                            logger.trace("Started new transaction in order to be able to lock binary value");
                        }
                    } else {
                        transactionStarted = false;
                        if (logger.isTraceEnabled()) {
                            logger.trace("Detected ongoing transaction which will be used for locking");
                        }
                    }
                    boolean lockObtained = this.cache.lock(key);
                    if (!lockObtained) {
                        throw new BinaryStoreException(JcrI18n.errorLockingBinaryValue.text(key));
                    }
                } catch (BinaryStoreException ex) {
                    throw ex;
                } catch (Exception ex) {
                    throw new BinaryStoreException(JcrI18n.errorStoringBinaryValue.text(key), ex);
                }
            }

            @Override
            public void unlock() throws BinaryStoreException {
                if (!transactionStarted) {
                    return;
                }
                if (logger.isTraceEnabled()) {
                    logger.trace("Unlocking binary {0}", key);
                }
                try {
                    this.cache.getTransactionManager().commit();
                } catch (Exception ex) {
                    throw new BinaryStoreException(JcrI18n.errorStoringBinaryValue.text(key), ex);
                }
            }
        }
    }

    interface Lock {

        void unlock() throws BinaryStoreException;
    }

    protected static class DummyReducer implements Reducer<String, String> {
        private static final long serialVersionUID = 1L;

        @Override
        public String reduce( String s,
                              Iterator<String> stringIterator ) {
            return s;
        }
    }
}
