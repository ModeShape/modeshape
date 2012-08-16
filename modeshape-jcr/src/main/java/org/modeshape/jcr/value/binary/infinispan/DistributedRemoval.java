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

import org.infinispan.Cache;
import org.infinispan.distexec.DistributedCallable;

import java.io.Serializable;
import java.util.Set;


class DistributedRemoval implements DistributedCallable<String, Metadata, Object>, Serializable {

    private String blobCacheName;
    private long minimumAgeInMS;
    private Cache<String, Metadata> metadataCache;
    private Cache<String, byte[]> blobCache;

    public DistributedRemoval(String blobCacheName, long minimumAgeInMS){
        this.blobCacheName = blobCacheName;
        this.minimumAgeInMS = minimumAgeInMS;
    }

    @Override
    public void setEnvironment(Cache<String, Metadata> metadataCache, Set<String> inputKeys) {
        this.metadataCache = metadataCache;
        this.blobCache = metadataCache.getCacheManager().getCache(blobCacheName);
    }

    @Override
    public Object call() throws Exception {
        long now = System.currentTimeMillis();
        InfinispanBinaryStore.LockFactory lockFactory = new InfinispanBinaryStore.LockFactory(metadataCache);
        // todo in case of eviction enabled, keySet() is insufficient. We need to query also the CacheStore.
        // todo if store is shared query only from coordinator node
        for(String keyString : metadataCache.keySet()){
            // need to be locked to avoid problems e.g. when parallel the same binary data are stored
            InfinispanBinaryStore.Lock lock = lockFactory.writeLock(keyString);
            try {
                InfinispanBinaryStore.removeBinaryValue(metadataCache, blobCache, keyString);
                Metadata metadata = metadataCache.get(keyString);
                // double check != null
                if(metadata != null && metadata.isUnused() && now - metadata.unusedSince() > minimumAgeInMS){
                    InfinispanBinaryStore.removeBinaryValue(metadataCache, blobCache, keyString);
                }
            } finally {
                lock.unlock();
            }
        }
        return null;
    }
}
