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

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.*;
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
import org.modeshape.jcr.value.binary.*;

/**
 * A {@link org.modeshape.jcr.value.binary.BinaryStore} implementation that uses Infinispan for persisting binary values.
 */
@ThreadSafe
public class InfinispanBinaryStore extends AbstractBinaryStore {

    private final Logger logger;
    private LockFactory lockFactory;
    private CacheContainer cacheContainer;
    private boolean dedicatedCacheContainer;
    private Cache<String, Metadata> metadataCache;
    private Cache<String, byte[]> blobCache;

    private String metadataCacheName;
    private String blobCacheName;

    /**
     *
     * @param cacheContainer cache container which used for cache management
     * @param dedicatedCacheContainer true if the cache container should be started/stopped when store is start or stopped
     * @param metadataCacheName name of the cache used for metadata
     * @param blobCacheName name of the cache used for store of chunked binary values
     */
    @SuppressWarnings("unchecked")
    public InfinispanBinaryStore(CacheContainer cacheContainer, boolean dedicatedCacheContainer, String metadataCacheName, String blobCacheName){
        logger = Logger.getLogger(getClass());
        this.cacheContainer = cacheContainer;
        this.dedicatedCacheContainer = dedicatedCacheContainer;
        this.metadataCacheName = metadataCacheName;
        this.blobCacheName = blobCacheName;
    }

    @Override
    public void start() {
        logger.debug("start()");
        if(metadataCache != null){
            logger.debug("Already started.");
            return;
        }
        if(dedicatedCacheContainer){
            cacheContainer.start();
        }
        metadataCache = cacheContainer.getCache(metadataCacheName);
        blobCache = cacheContainer.getCache(blobCacheName);
        lockFactory = new LockFactory(metadataCache);
    }

    @Override
    public void shutdown() {
        try {
            if(dedicatedCacheContainer){
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
        if(!dedicatedCacheContainer){
            if(metadataCache != null){
                caches.add(metadataCache);
            }
            if(blobCache != null){
                caches.add(blobCache);
            }
        }
        return caches;
    }

    @Override
    public BinaryValue storeValue(InputStream inputStream) throws BinaryStoreException, SystemFailureException {
        try {
            // using tmp file to determine SHA1
            SecureHash.HashingInputStream hashingStream = SecureHash.createHashingStream(SecureHash.Algorithm.SHA_1, inputStream);
            File tmpFile = File.createTempFile("ms-ispn-binstore", "hashing");
            IoUtil.write(hashingStream,
                    new BufferedOutputStream(new FileOutputStream(tmpFile)),
                    AbstractBinaryStore.MEDIUM_BUFFER_SIZE);
            final BinaryKey binaryKey = new BinaryKey(hashingStream.getHash());
            Lock lock = lockFactory.writeLock(binaryKey.toString());
            BinaryValue value;
            try {
                // check if binary data already exists
                final Metadata checkMetadata = metadataCache.get(binaryKey.toString());
                if(checkMetadata != null){
                    logger.debug("Binary value already exist.");
                    // in case of an unused entry, this entry is from now used
                    if(checkMetadata.isUnused()){
                        checkMetadata.setUsed();
                        new RetryOperation(){
                            @Override
                            protected void call() throws IOException {
                                metadataCache.put(binaryKey.toString(), checkMetadata);
                            }
                        }.doTry();
                    }
                    return new StoredBinaryValue(this, binaryKey, checkMetadata.getLength());
                }

                logger.debug("Store binary value into chunks.");
                // store the chunks based referenced to SHA1-key
                ChunkOutputStream chunkOutputStream = new ChunkOutputStream(blobCache, binaryKey.toString());
                IoUtil.write(new FileInputStream(tmpFile), chunkOutputStream);
                // now store metadata
                final Metadata metadata = new Metadata();
                metadata.setModificationTime(System.currentTimeMillis());
                metadata.setNumberChunks(chunkOutputStream.getNumberChunks());
                metadata.setLength(tmpFile.length());
                new RetryOperation(){
                    @Override
                    protected void call() throws IOException {
                        metadataCache.put(binaryKey.toString(), metadata);
                    }
                }.doTry();
                value = new StoredBinaryValue(this, binaryKey, tmpFile.length());
            } finally {
                lock.unlock();
            }
            // initial text extraction
            if (extractors() != null) {
                extractors().extract(this, value, new TextExtractorContext());
            }
            return value;
        } catch (IOException e) {
            throw new BinaryStoreException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new SystemFailureException(e);
        } finally {
            IoUtil.closeQuietly(inputStream);
        }
    }

    @Override
    public InputStream getInputStream(BinaryKey binaryKey) throws BinaryStoreException {
        Metadata metadata = metadataCache.get(binaryKey.toString());
        if(metadata == null){
            throw new BinaryStoreException(JcrI18n.unableToFindBinaryValue.text(binaryKey, "Infinispan cache "+metadataCache.getName()));
        }
        if(metadata.getLength() == 0){
            return new ByteArrayInputStream(new byte[0]);
        } else {
            return new ChunkInputStream(blobCache, binaryKey.toString());
        }
    }

    @Override
    public void markAsUnused( Iterable<BinaryKey> keys ) throws BinaryStoreException {
        for(BinaryKey binaryKey : keys){
            Lock lock = lockFactory.writeLock(binaryKey.toString());
            try {
                final Metadata tmpMetadata = metadataCache.get(binaryKey.toString());
                // we use the copy of the original object to avoid changes cache values in case of errors
                final BinaryKey binaryKey1 = binaryKey;
                if(tmpMetadata == null || tmpMetadata.isUnused()){
                    continue;
                }
                // we use the copy of the original object to avoid changes cache values in case of errors
                final Metadata metadata = tmpMetadata.copy();
                metadata.setUnused();

                new RetryOperation(){
                    @Override
                    protected void call() throws IOException {
                        metadataCache.put(binaryKey1.toString(), metadata);
                    }
                }.doTry();
            } catch (IOException ex){
                logger.debug(ex, "Error during mark binary value unused {0}", binaryKey.toString());
                throw new BinaryStoreException(JcrI18n.errorMarkingBinaryValuesUnused.text(ex.getCause().getMessage()));
            } finally {
                lock.unlock();
            }
        }
    }

    @SuppressWarnings("unchecked")
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
        CacheLoaderManager cacheLoaderManager = metadataCache.getAdvancedCache().getComponentRegistry().getComponent(CacheLoaderManager.class);
        if(cacheLoaderManager != null){
            cacheLoader = cacheLoaderManager.getCacheLoader();
            cacheLoaderShared = cacheLoaderManager.isShared();
        }

        if(!metadataCache.getCacheManager().isCoordinator() && cacheLoaderShared){
            // in this case an other node will care...
            return;
        }

        long minimumAgeInMS = unit.toMillis(minimumAge);
        Set processedKeys = new HashSet();
        if(metadataCache.getCacheConfiguration().clustering().cacheMode().isDistributed() && metadataCache.getCacheManager().isCoordinator()){
            // distributed mapper finds unused...
            MapReduceTask<String, Metadata, String, String> task = new MapReduceTask<String, Metadata, String, String>(metadataCache);
            task.mappedWith(new UnusedMapper(minimumAgeInMS));
            task.reducedWith(new Reducer<String, String>() {
                @Override
                public String reduce(String s, Iterator<String> stringIterator) {
                    return s;
                }
            });
            Map<String, String> result = task.execute();
            for(String key : result.values()){
                InfinispanBinaryStore.Lock lock = lockFactory.writeLock(key);
                try {
                    removeBinaryValue(metadataCache, blobCache, key);
                } finally {
                    lock.unlock();
                }
            }
        } else {
            // local / repl cache
            // process entries in memory
            for (String key : new ArrayList<String>(metadataCache.keySet())){
                Metadata metadata = metadataCache.get(key);
                processedKeys.add(key);
                if(isValueUnused(metadata, minimumAgeInMS)){
                    InfinispanBinaryStore.Lock lock = lockFactory.writeLock(key);
                    try {
                        removeBinaryValue(metadataCache, blobCache, key);
                    } finally {
                        lock.unlock();
                    }
                }
            }

        }
        if(metadataCache.getCacheManager().isCoordinator() && cacheLoader != null){
            // process cache loader content
            try {
                for(Object key : new ArrayList<Object>(cacheLoader.loadAllKeys(processedKeys))){
                    Metadata metadata = metadataCache.get(key);
                    if(isValueUnused(metadata, minimumAgeInMS)){
                        InfinispanBinaryStore.Lock lock = lockFactory.writeLock((String)key);
                        try {
                            removeBinaryValue(metadataCache, blobCache, (String)key);
                        } finally {
                            lock.unlock();
                        }
                    }
                }
            } catch (CacheLoaderException cle){
                logger.debug("Error during cleanup of cache loader", cle);
                throw new BinaryStoreException(JcrI18n.errorDuringGarbageCollection.text(cle.getMessage()));
            }
        }

    }

    static boolean isValueUnused(Metadata metadata, long minimumAgeInMS){
        if(metadata == null || !metadata.isUnused()){
            return false;
        }
        return System.currentTimeMillis() - metadata.unusedSince() > minimumAgeInMS;
    }

    static void removeBinaryValue(final Cache<String, Metadata> metadataCache, final Cache<String, byte[]> blobCache, final String key) throws BinaryStoreException {
        Metadata metadata = metadataCache.get(key);
        // double check != null
        if(metadata == null || !metadata.isUnused()){
            return;
        }
        // the metadata entry itself
        metadataCache.remove(key);
        // remove chunks (if any)
        if(metadata.getNumberChunks() > 0){
            for(int chunkIndex = 0; chunkIndex < metadata.getNumberChunks(); chunkIndex++){
                blobCache.remove(key+"-"+chunkIndex);
            }
        }
        if(metadata.getNumberTextChunks() > 0){
            for(int chunkIndex = 0; chunkIndex < metadata.getNumberTextChunks(); chunkIndex++){
                blobCache.remove(key+"-text-"+chunkIndex);
            }
        }
    }

    private static class UnusedMapper implements Mapper<String, Metadata, String, String> {

        private long minimumAgeInMS;

        public UnusedMapper(long minimumAgeInMS){
            this.minimumAgeInMS = minimumAgeInMS;
        }

        @Override
        public void map(String key, Metadata metadata, Collector<String, String> stringCollector) {
            if(isValueUnused(metadata, minimumAgeInMS)){
                stringCollector.emit(key, key);
            }
        }
    }

    @Override
    protected String getStoredMimeType(BinaryValue binary) throws BinaryStoreException {
        Metadata metadata = metadataCache.get(binary.getKey().toString());
        if(metadata == null){
            throw new BinaryStoreException(JcrI18n.errorStoringMimeType.text(JcrI18n.unableToFindBinaryValue.text(binary.getKey(), "Infinispan cache "+metadataCache.getName())));
        }
        return metadata.getMimeType();
    }

    @Override
    protected void storeMimeType(final BinaryValue binary, String mimeType) throws BinaryStoreException {
        Lock lock = lockFactory.writeLock(binary.getKey().toString());
        try {
            Metadata tmpMetadata = metadataCache.get(binary.getKey().toString());
            if(tmpMetadata == null){
                throw new BinaryStoreException(JcrI18n.errorStoringMimeType.text(JcrI18n.unableToFindBinaryValue.text(binary.getKey(), "Infinispan cache "+metadataCache.getName())));
            }
            // we use the copy of the original object to avoid changes cache values in case of errors
            final Metadata metadata = tmpMetadata.copy();
            metadata.setMimeType(mimeType);
            new RetryOperation(){
                @Override
                protected void call() throws IOException {
                    metadataCache.put(binary.getKey().toString(), metadata);
                }
            }.doTry();
        } catch (IOException ex){
            logger.debug(ex, "Error during store of mime type for {0}", binary.getKey().toString());
            throw new BinaryStoreException(JcrI18n.errorStoringMimeType.text(ex.getCause().getMessage()));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String getExtractedText(BinaryValue binary) throws BinaryStoreException {
        Metadata metadata = metadataCache.get(binary.getKey().toString());
        if(metadata == null || metadata.getNumberTextChunks() == 0){
            return null;
        }
        try {
            return IoUtil.read(new ChunkInputStream(blobCache, binary+"-text"), "UTF-8");
        } catch (IOException ex){
            logger.debug(ex, "Error during read of extracted text for {0}", binary.getKey().toString());
            throw new BinaryStoreException(JcrI18n.errorReadingExtractedText.text(ex.getCause().getMessage()));
        }
    }

    @Override
    public void storeExtractedText(final BinaryValue binary, String extractedText) throws BinaryStoreException {
        Lock lock = lockFactory.writeLock(binary.getKey().toString());
        try {
            final Metadata metadata = metadataCache.get(binary.getKey().toString());
            if(metadata == null){
                throw new BinaryStoreException(JcrI18n.errorStoringMimeType.text(JcrI18n.unableToFindBinaryValue.text(binary.getKey(), "Infinispan cache "+metadataCache.getName())));
            }
            ChunkOutputStream chunkOutputStream = null;
            try {
                chunkOutputStream = new ChunkOutputStream(blobCache, binary.toString()+"-text");
                chunkOutputStream.write(extractedText.getBytes("UTF-8"));
            } finally {
                IoUtil.closeQuietly(chunkOutputStream);
            }
            metadata.setNumberTextChunks(chunkOutputStream.getNumberChunks());
            new RetryOperation(){
                @Override
                protected void call() throws IOException {
                    metadataCache.put(binary.getKey().toString(), metadata);
                }
            }.doTry();
        } catch (IOException ex){
            logger.debug(ex, "Error during store of extracted text for {0}", binary.getKey().toString());
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
        private final Cache metadataCache;
        private final Lock DUMMY_LOCK = new Lock() {
            @Override
            public void unlock() {
            }
        };

        public LockFactory(Cache metadataCache){
            this.metadataCache = metadataCache;
            if(metadataCache != null){
                infinispanLocks = metadataCache.getCacheConfiguration().transaction().transactionMode() != TransactionMode.NON_TRANSACTIONAL &&
                        metadataCache.getCacheConfiguration().transaction().lockingMode() == LockingMode.PESSIMISTIC;
                namedLocks = !infinispanLocks && !metadataCache.getCacheConfiguration().clustering().cacheMode().isClustered() ? new NamedLocks() : null;
            } else {
                namedLocks = null;
                infinispanLocks = false;
            }
        }

        public Lock readLock(String key) throws BinaryStoreException {
            if(namedLocks != null){
                return new NamedLock(namedLocks.readLock(key));
            } else if(infinispanLocks){
                return new ISPNLock(metadataCache, key);
            } else {
                return DUMMY_LOCK;
            }
        }

        public Lock writeLock(String key) throws BinaryStoreException {
            if(namedLocks != null){
                return new NamedLock(namedLocks.writeLock(key));
            } else if(infinispanLocks){
                return new ISPNLock(metadataCache, key);
            } else {
                return DUMMY_LOCK;
            }
        }

        private class NamedLock implements Lock {

            private final java.util.concurrent.locks.Lock lock;

            public NamedLock(java.util.concurrent.locks.Lock lock){
                this.lock = lock;
            }

            @Override
            public void unlock() {
                lock.unlock();
            }
        }

        private class ISPNLock implements Lock {

            private final Cache cache;
            private final String key;

            public ISPNLock(Cache cache, String key) throws BinaryStoreException {
                this.cache = cache;
                this.key = key;
                try {
                    cache.getAdvancedCache().getTransactionManager().begin();
                    boolean lockObtained = cache.getAdvancedCache().withFlags(Flag.FAIL_SILENTLY).lock(key);
                    if(!lockObtained){
                        throw new BinaryStoreException(JcrI18n.errorLockingBinaryValue.text(key));
                    }
                } catch (BinaryStoreException ex){
                    throw ex;
                } catch (Exception ex){
                    throw new BinaryStoreException(JcrI18n.errorStoringBinaryValue.text(key), ex);
                }
            }

            @Override
            public void unlock() throws BinaryStoreException {
                try {
                    cache.getAdvancedCache().getTransactionManager().commit();
                } catch (Exception ex){
                    throw new BinaryStoreException(JcrI18n.errorStoringBinaryValue.text(key), ex);
                }
            }
        }
    }

    interface Lock {

        void unlock() throws BinaryStoreException;
    }
}
