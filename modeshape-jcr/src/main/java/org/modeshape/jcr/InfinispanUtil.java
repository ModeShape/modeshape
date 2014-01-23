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
package org.modeshape.jcr;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.StoreConfiguration;
import org.infinispan.distexec.DefaultExecutorService;
import org.infinispan.distexec.DistributedCallable;
import org.infinispan.distexec.DistributedExecutorService;
import org.infinispan.tasks.GlobalKeySetTask;
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
        List<StoreConfiguration> stores = cache.getCacheConfiguration().persistence().stores();
        boolean shared = (stores != null) && !stores.isEmpty() && stores.get(0).shared();
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
     * @throws InterruptedException if the process is interrupted
     * @throws ExecutionException if there is an error while getting all keys
     */
    public static <K, V> Sequence<K> getAllKeys( Cache<K, V> cache )
        throws InterruptedException, ExecutionException {
        LOGGER.debug("getAllKeys of {0}", cache.getName());
        Set<K> cacheKeys = GlobalKeySetTask.getGlobalKeySet(cache);
        return new IteratorSequence<K>(cacheKeys.iterator());
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
}
