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
import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.infinispan.distexec.mapreduce.Collector;
import org.infinispan.distexec.mapreduce.MapReduceTask;
import org.infinispan.distexec.mapreduce.Mapper;
import org.infinispan.distexec.mapreduce.Reducer;
import org.infinispan.loaders.CacheLoader;
import org.infinispan.loaders.CacheLoaderException;
import org.infinispan.loaders.CacheLoaderManager;
import org.infinispan.manager.CacheContainer;
import org.infinispan.transaction.LockingMode;
import org.infinispan.transaction.TransactionMode;
import org.modeshape.common.SystemFailureException;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.IoUtil;
import org.modeshape.common.util.SecureHash;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.text.TextExtractorContext;
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
public class InfinispanBinaryStore extends AbstractBinaryStore {

    private static final String META_SUFFIX = "-meta";
    private static final String DATA_SUFFIX = "-data";
    private static final String TEXT_SUFFIX = "-text";
    private static final int SUFFIX_LENGTH = 5;

    private static final int MIN_KEY_LENGTH = BinaryKey.maxHexadecimalLength() + SUFFIX_LENGTH;
    private static final int MAX_KEY_LENGTH = MIN_KEY_LENGTH;

    private static final int RETRY_COUNT = 5;

    private final Logger logger;
    private LockFactory lockFactory;
    private CacheContainer cacheContainer;
    private boolean dedicatedCacheContainer;
    protected Cache<String, Metadata> metadataCache;
    private Cache<String, byte[]> blobCache;

    private String metadataCacheName;
    private String blobCacheName;

    /**
     * @param cacheContainer cache container which used for cache management
     * @param dedicatedCacheContainer true if the cache container should be started/stopped when store is start or stopped
     * @param metadataCacheName name of the cache used for metadata
     * @param blobCacheName name of the cache used for store of chunked binary values
     */
    public InfinispanBinaryStore( CacheContainer cacheContainer,
                                  boolean dedicatedCacheContainer,
                                  String metadataCacheName,
                                  String blobCacheName ) {
        logger = Logger.getLogger(getClass());
        this.cacheContainer = cacheContainer;
        this.dedicatedCacheContainer = dedicatedCacheContainer;
        this.metadataCacheName = metadataCacheName;
        this.blobCacheName = blobCacheName;
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
    public BinaryValue storeValue( InputStream inputStream ) throws BinaryStoreException, SystemFailureException {
        File tmpFile = null;
        try {
            // using tmp file to determine SHA1
            SecureHash.HashingInputStream hashingStream = SecureHash.createHashingStream(SecureHash.Algorithm.SHA_1, inputStream);
            tmpFile = File.createTempFile("ms-ispn-binstore", "hashing");
            IoUtil.write(hashingStream,
                         new BufferedOutputStream(new FileOutputStream(tmpFile)),
                         AbstractBinaryStore.MEDIUM_BUFFER_SIZE);
            final BinaryKey binaryKey = new BinaryKey(hashingStream.getHash());
            Lock lock = lockFactory.writeLock(lockKeyFrom(binaryKey));
            BinaryValue value;
            try {
                // check if binary data already exists
                final String metadataKey = metadataKeyFrom(binaryKey);
                Metadata metadata = metadataCache.get(metadataKey);
                if (metadata != null) {
                    logger.debug("Binary value already exist.");
                    // in case of an unused entry, this entry is from now used
                    if (metadata.isUnused()) {
                        metadata.markAsUsed();
                        putMetadata(metadataKey, metadata);
                    }
                    return new StoredBinaryValue(this, binaryKey, metadata.getLength());
                }

                logger.debug("Store binary value into chunks.");
                // store the chunks based referenced to SHA1-key
                final String dataKey = dataKeyFrom(binaryKey);
                final long lastModified = tmpFile.lastModified();
                final long fileLength = tmpFile.length();
                int bufferSize = 8192;
                if (bufferSize > fileLength) bufferSize = 4096;
                if (bufferSize > fileLength) bufferSize = 2096;
                ChunkOutputStream chunkOutputStream = new ChunkOutputStream(blobCache, dataKey);
                IoUtil.write(new FileInputStream(tmpFile), chunkOutputStream, bufferSize);
                // now store metadata
                metadata = new Metadata(lastModified, fileLength, chunkOutputStream.getNumberChunks());
                putMetadata(metadataKey, metadata);
                value = new StoredBinaryValue(this, binaryKey, fileLength);
            } finally {
                lock.unlock();
            }
            // initial text extraction
            if (extractors() != null) {
                extractors().extract(this, value, new TextExtractorContext(detector()));
            }
            return value;
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
        return new ChunkInputStream(blobCache, dataKeyFrom(binaryKey));
    }

    @Override
    public void markAsUnused( Iterable<BinaryKey> keys ) throws BinaryStoreException {
        for (BinaryKey binaryKey : keys) {
            // Try to mark the metadata as unused. We loop here in case other processes (not other threads in this process,
            // which are handled via locks) are doing the same thing.
            for (int i = 0; i != RETRY_COUNT; ++i) {
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
        CacheLoader cacheLoader = null;
        boolean cacheLoaderShared = false;
        CacheLoaderManager cacheLoaderManager = metadataCache.getAdvancedCache()
                                                             .getComponentRegistry()
                                                             .getComponent(CacheLoaderManager.class);
        if (cacheLoaderManager != null) {
            cacheLoader = cacheLoaderManager.getCacheLoader();
            cacheLoaderShared = cacheLoaderManager.isShared();
        }

        if (!metadataCache.getCacheManager().isCoordinator() && cacheLoaderShared) {
            // in this case an other node will care...
            return;
        }

        long minimumAgeInMS = unit.toMillis(minimumAge);
        Set<Object> processedKeys = new HashSet<Object>();
        if (metadataCache.getCacheConfiguration().clustering().cacheMode().isDistributed()
            && metadataCache.getCacheManager().isCoordinator()) {
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

        }
        if (metadataCache.getCacheManager().isCoordinator() && cacheLoader != null) {
            // process cache loader content
            try {
                for (Object key : new ArrayList<Object>(cacheLoader.loadAllKeys(processedKeys))) {
                    if (!(key instanceof String)) continue;
                    if (!isMetadataKey((String)key)) continue;
                    Metadata metadata = metadataCache.get(key);
                    if (isValueUnused(metadata, minimumAgeInMS)) {
                        InfinispanBinaryStore.Lock lock = lockFactory.writeLock((String)key);
                        try {
                            removeUnusedBinaryValue(metadataCache, blobCache, (String)key);
                        } finally {
                            lock.unlock();
                        }
                    }
                }
            } catch (CacheLoaderException cle) {
                logger.debug("Error during cleanup of cache loader", cle);
                throw new BinaryStoreException(JcrI18n.errorDuringGarbageCollection.text(cle.getMessage()));
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
            return IoUtil.read(new ChunkInputStream(blobCache, textKey), "UTF-8");
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
                chunkOutputStream = new ChunkOutputStream(blobCache, textKey);
                chunkOutputStream.write(extractedText.getBytes("UTF-8"));
            } finally {
                IoUtil.closeQuietly(chunkOutputStream);
            }
            putMetadata(metadataKey, metadata.withNumberOfTextChunks(chunkOutputStream.getNumberChunks()));
        } catch (IOException ex) {
            logger.debug(ex, "Error during store of extracted text for {0}", key);
            throw new BinaryStoreException(JcrI18n.errorStoringExtractedText.text(ex.getCause().getMessage()));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Iterable<BinaryKey> getAllBinaryKeys() throws BinaryStoreException {
        throw new BinaryStoreException("Not implemented");
    }

    /**
     * Locks are created based upon metadata cache configuration
     */
    static class LockFactory {

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
                infinispanLocks = metadataCache.getCacheConfiguration().transaction().transactionMode() != TransactionMode.NON_TRANSACTIONAL
                                  && metadataCache.getCacheConfiguration().transaction().lockingMode() == LockingMode.PESSIMISTIC;
                namedLocks = !infinispanLocks && !metadataCache.getCacheConfiguration().clustering().cacheMode().isClustered() ? new NamedLocks() : null;
            } else {
                namedLocks = null;
                infinispanLocks = false;
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

            private final Cache<String, Metadata> cache;
            private final String key;

            public ISPNLock( Cache<String, Metadata> cache,
                             String key ) throws BinaryStoreException {
                this.cache = cache;
                this.key = key;
                try {
                    cache.getAdvancedCache().getTransactionManager().begin();
                    boolean lockObtained = cache.getAdvancedCache().withFlags(Flag.FAIL_SILENTLY).lock(key);
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
                try {
                    cache.getAdvancedCache().getTransactionManager().commit();
                } catch (Exception ex) {
                    throw new BinaryStoreException(JcrI18n.errorStoringBinaryValue.text(key), ex);
                }
            }
        }
    }

    interface Lock {

        void unlock() throws BinaryStoreException;
    }

    private static class DummyReducer implements Reducer<String, String> {
        private static final long serialVersionUID = 1L;

        @Override
        public String reduce( String s,
                              Iterator<String> stringIterator ) {
            return s;
        }
    }
}
