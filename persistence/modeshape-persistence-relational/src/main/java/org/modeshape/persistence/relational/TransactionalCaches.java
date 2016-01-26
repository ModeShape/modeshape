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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.util.CheckArg;
import org.modeshape.schematic.document.Document;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;

/**
 * Class which provides a set of in-memory caches for each ongoing transaction, attempting to relieve some of the "read pressure"
 * for transactions.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @since 5.0
 */
@ThreadSafe
public final class TransactionalCaches {
    
  
    private static final Map<String, ConcurrentMap<String, Document>> READ_CACHES_BY_TX_ID = new ConcurrentHashMap<>();
    private static final Map<String, ConcurrentMap<String, Document>> EDIT_CACHES_BY_TX_ID = new ConcurrentHashMap<>();
    
    private final int size;

    protected TransactionalCaches(int size) {
        CheckArg.isPositive(size, "cacheSize");
        this.size = size;
    }

    protected ConcurrentMap<String, Document> readCacheForTransaction(String txId, boolean createIfAbsent) {
        return READ_CACHES_BY_TX_ID.computeIfAbsent(txId, id -> createIfAbsent ? new ConcurrentLinkedHashMap.Builder<String, Document>()
                .maximumWeightedCapacity(size).build() : null);
    }
    
    protected ConcurrentMap<String, Document> editCacheForTransaction(String txId, boolean createIfAbsent) {
        return EDIT_CACHES_BY_TX_ID.computeIfAbsent(txId, id -> createIfAbsent ? new ConcurrentHashMap<>() : null);
    }

    protected Document cachedDocument(String txId, String key) {
        ConcurrentMap<String, Document> readingCache = readCacheForTransaction(txId, false);
        return readingCache != null ? readingCache.get(key) : null;
    }
    
    protected void storeForReading(String txId, String key, Document document) {
        readCacheForTransaction(txId, true).put(key, document);
    }
      
    protected void storeForEditing(String txId, String key, Document document) {
        editCacheForTransaction(txId, true).put(key, document);
    }
    
    protected void removeDocument(String txId, String key) {
        ConcurrentMap<String, Document> readingCache = readCacheForTransaction(txId, false);
        if (readingCache != null) {
            readingCache.remove(key);
        }
        ConcurrentMap<String, Document> editCache = editCacheForTransaction(txId, false);
        if (editCache != null) {
            editCache.remove(key);
        }
    }
    
    protected void clearReadCache(String txId) {
        READ_CACHES_BY_TX_ID.remove(txId);
    }

    protected void clearEditCache(String txId) {
        READ_CACHES_BY_TX_ID.remove(txId);
    }
}
