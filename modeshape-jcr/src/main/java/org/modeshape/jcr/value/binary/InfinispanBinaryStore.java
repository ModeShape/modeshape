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
package org.modeshape.jcr.value.binary;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import org.infinispan.Cache;
import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.EmbeddedCacheManager;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.BinaryKey;

/**
 * A {@link BinaryStore} implementation that uses a database for persisting binary values.
 */
@ThreadSafe
public class InfinispanBinaryStore extends AbstractBinaryStore {

    private String contentCacheName;
    private CacheContainer contentCacheContainer;
    private String cacheName;
    private CacheContainer cacheContainer;
    private Cache<?, ?> cache;
    private boolean ownCacheContainer;

    public InfinispanBinaryStore( String cacheName,
                                  CacheContainer cacheContainer ) {
        this.cacheName = cacheName;
        this.cacheContainer = cacheContainer;
    }

    /**
     * Set the name of the content cache. This will be called before {@link #start()}.
     * 
     * @param cacheName the name of the cache this repository uses to store content; never null
     */
    public void setContentCacheName( String cacheName ) {
        this.contentCacheName = cacheName;
    }

    /**
     * Set the {@link CacheContainer} this repository uses for content. This will be called before {@link #start()}.
     * 
     * @param cacheContainer the cache container this repository uses to store content; never null
     */
    public void setContentCacheContainer( CacheContainer cacheContainer ) {
        this.contentCacheContainer = cacheContainer;
    }

    /**
     * Initialize the store with the name of the cache and the {@link CacheContainer} used for the content cache.
     */
    @Override
    public void start() {
        if (cache != null) {
            if (this.cacheName == null) {
                this.cacheName = contentCacheName;
            }
            if (this.cacheContainer == null) {
                this.cacheContainer = contentCacheContainer;
                this.ownCacheContainer = false;
            } else {
                this.ownCacheContainer = true;
            }
            // find the cache instance ...
            cache = this.cacheContainer.getCache(this.cacheName);
        }
    }

    @Override
    public void shutdown() {
        try {
            if (this.ownCacheContainer) {
                if (cacheContainer instanceof EmbeddedCacheManager) {
                    ((EmbeddedCacheManager)this.cacheContainer).stop();
                }
            }
        } finally {
            this.cacheContainer = null;
            this.cacheName = null;
        }
    }

    public Cache<?, ?> getCache() {
        return cache;
    }

    @Override
    public BinaryValue storeValue( InputStream stream ) throws BinaryStoreException {
        throw new BinaryStoreException("Not implemented");
    }

    @Override
    public InputStream getInputStream( BinaryKey key ) throws BinaryStoreException {
        throw new BinaryStoreException("Not implemented");
    }

    @Override
    public void markAsUnused( Iterable<BinaryKey> keys ) throws BinaryStoreException {
        throw new BinaryStoreException("Not implemented");
    }

    @Override
    public void removeValuesUnusedLongerThan( long minimumAge,
                                              TimeUnit unit ) throws BinaryStoreException {
        throw new BinaryStoreException("Not implemented");
    }

    @Override
    public String getText( BinaryValue binary ) throws BinaryStoreException {
        throw new BinaryStoreException("Not implemented");
    }

    @Override
    public String getMimeType( BinaryValue binary,
                               String name ) /* throws IOException, RepositoryException*/{
        throw new UnsupportedOperationException("Not implemented");
    }
}
