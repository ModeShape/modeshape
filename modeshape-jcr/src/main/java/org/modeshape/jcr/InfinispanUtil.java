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
package org.modeshape.jcr;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.infinispan.Cache;
import org.infinispan.distexec.DefaultExecutorService;
import org.infinispan.distexec.DistributedCallable;
import org.infinispan.distexec.DistributedExecutorService;
import org.infinispan.loaders.CacheLoader;
import org.infinispan.loaders.CacheLoaderException;
import org.infinispan.loaders.CacheLoaderManager;
import org.modeshape.common.logging.Logger;

/**
 * A set of utility methods for Infinispan caches.
 */
public class InfinispanUtil {

    protected static final Logger LOGGER = Logger.getLogger(InfinispanUtil.class);

    private InfinispanUtil() {
        // prevent instantiation ...
    }

    public static enum Location {
        /** In all processes */
        EVERYWHERE,
        /** In the local process only, even if the cache is distributed */
        LOCALLY;
    }

    /**
     * Utility to run the supplied callable in all the processes where the cache is being run.
     * 
     * @param <K> the type of key used in the cache
     * @param <V> the type of value used in the cache
     * @param <T> the type of result
     * @param cache the cache
     * @param location the location where the callable should be run; if null, {@link Location#LOCALLY} is used
     * @param callable the callable
     * @param combiner the component that can combine multiple results of type <i>T</i> into a single result
     * @return the result of the operation
     * @throws InterruptedException if the process is interrupted
     * @throws ExecutionException if there is an error while getting executing the operation
     */
    public static <K, V, T> T execute( Cache<K, V> cache,
                                       Location location,
                                       DistributedCallable<K, V, T> callable,
                                       Combiner<T> combiner ) throws InterruptedException, ExecutionException {
        if (location == null) location = Location.LOCALLY;

        DistributedExecutorService distributedExecutor = new DefaultExecutorService(cache);
        boolean shared = cache.getCacheConfiguration().loaders().shared();
        T result = null;
        if (!shared) {
            // store is not shared so every node must return key list of the store
            List<Future<T>> futures = null;
            switch (location) {
                case EVERYWHERE:
                    futures = distributedExecutor.submitEverywhere(callable);
                    break;
                case LOCALLY:
                    futures = Collections.singletonList(distributedExecutor.submit(callable));
                    break;
            }

            while (futures != null && !futures.isEmpty()) {
                // Get the next future that is ready ...
                Iterator<Future<T>> futureIter = futures.iterator();
                while (futureIter.hasNext()) {
                    Future<T> future = futureIter.next();
                    try {
                        // But done't wait too long for this future ...
                        T value = future.get(100, TimeUnit.MILLISECONDS);
                        // We got some keys, so this future is done and should be removed from our list ...
                        futureIter.remove();
                        result = combiner.combine(result, value);
                    } catch (TimeoutException e) {
                        // continue;
                    }
                }
                if (futures.isEmpty()) break;
            }
        } else {
            // store is shared, so we can short-circuit the logic and just run locally; otherwise, if distributed
            // each process will see the all of the keys ...
            result = distributedExecutor.submit(callable).get();
        }
        return result;
    }

    /**
     * The interface that defines how the results should be merged
     * 
     * @param <T>
     */
    public static interface Combiner<T> {
        T combine( T priorResult,
                   T newResult ) throws InterruptedException, ExecutionException;
    }

    /**
     * Get all of the keys in the cache.
     * 
     * @param <K> the type of key
     * @param <V> the type of value
     * @param cache the cache
     * @return the sequence that can be used to obtain the keys; never null
     * @throws CacheLoaderException if there is an error within the cache loader
     * @throws InterruptedException if the process is interrupted
     * @throws ExecutionException if there is an error while getting all keys
     */
    public static <K, V> Sequence<K> getAllKeys( Cache<K, V> cache )
        throws CacheLoaderException, InterruptedException, ExecutionException {
        LOGGER.debug("getAllKeys of {0}", cache.getName());

        GetAllKeys<K, V> task = new GetAllKeys<K, V>();
        KeyMerger<K> merger = new KeyMerger<K>();
        Set<K> cacheKeys = execute(cache, Location.EVERYWHERE, task, merger);
        return new IteratorSequence<K>(cacheKeys.iterator());
    }

    /**
     * Since keys can appear more than one time e.g. multiple owners in a distributed cache, they all must be merged into a Set.
     * 
     * @param <K> the type of key
     */
    protected static class KeyMerger<K> implements Combiner<Set<K>> {
        @Override
        public Set<K> combine( Set<K> priorResult,
                               Set<K> newResult ) {
            if (priorResult == null) priorResult = new HashSet<K>();
            if (newResult != null) priorResult.addAll(newResult);
            return priorResult;
        }
    }

    /**
     * A sequence of values. This abstracts how the values are obtained.
     * 
     * @param <T> the key type
     */
    public static interface Sequence<T> {
        /**
         * Get the next values.
         * 
         * @return the next value, or null if there are no more values.
         * @throws ExecutionException if there is an exception obtaining the next value
         * @throws CancellationException if the operation finding the values has been cancelled
         * @throws InterruptedException if the operation finding the values has been interrupted
         */
        T next() throws ExecutionException, CancellationException, InterruptedException;

        boolean hasNext();
    }

    private static final class IteratorSequence<T> implements Sequence<T> {
        private final Iterator<T> iterator;

        public IteratorSequence( Iterator<T> iterator ) {
            this.iterator = iterator;
        }

        @Override
        public T next() {
            return iterator.hasNext() ? iterator.next() : null;
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }
    }

    /**
     * A {@link DistributedCallable} implementation that returns the set of keys in an Infinispan cache. The keys inside cache
     * store are ignored.
     * 
     * @param <K> the type of key
     * @param <V> the type of value
     */
    protected static final class GetAllMemoryKeys<K, V> implements DistributedCallable<K, V, Set<K>>, Serializable {
        private static final long serialVersionUID = 1L;

        private Cache<K, V> cache;

        @Override
        public void setEnvironment( Cache<K, V> cache,
                                    Set<K> inputKeys ) {
            this.cache = cache;
        }

        @Override
        public Set<K> call() throws Exception {
            return cache.keySet();
        }
    }

    /**
     * A {@link DistributedCallable} implementation that returns the set of keys in an Infinispan cache which are stored in
     * memory.
     * 
     * @param <K> the type of key
     * @param <V> the type of value
     */
    protected static final class GetAllKeys<K, V> implements DistributedCallable<K, V, Set<K>>, Serializable {
        private static final long serialVersionUID = 1L;

        private Cache<K, V> cache;

        @Override
        public void setEnvironment( Cache<K, V> cache,
                                    Set<K> inputKeys ) {
            this.cache = cache;
        }

        @Override
        @SuppressWarnings( "unchecked" )
        public Set<K> call() throws Exception {
            CacheLoaderManager cacheLoaderManager = cache.getAdvancedCache()
                                                         .getComponentRegistry()
                                                         .getComponent(CacheLoaderManager.class);
            if (cacheLoaderManager == null) {
                return cache.keySet();
            }
            CacheLoader cacheLoader = cacheLoaderManager.getCacheLoader();
            if (cacheLoader == null) {
                return cache.keySet();
            }
            // load only these keys, which are not already in memory
            Set<K> cacheKeys = new HashSet<K>(cache.keySet());
            cacheKeys.addAll((Set<K>)cacheLoader.loadAllKeys((Set<Object>)cacheKeys));
            return cacheKeys;
        }
    }
}
