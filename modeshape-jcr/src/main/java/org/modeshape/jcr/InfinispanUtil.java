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

import java.util.Iterator;
import java.util.LinkedList;
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

/**
 * A set of utility methods for Infinispan caches.
 */
public class InfinispanUtil {

    private InfinispanUtil() {
        // prevent instantiation ...
    }

    /**
     * Get all of the keys in the cache.
     * 
     * @param <K> the type of key
     * @param <V> the type of value
     * @param cache the cache
     * @return the sequence that can be used to obtain the keys; never null
     */
    public static <K, V> Sequence<K> getAllKeys( Cache<K, V> cache ) {
        if (cache.getAdvancedCache().getRpcManager() == null) {
            // This is not a distributed cache, so there's no choice but to use 'keySet()' even though
            // it has limitations ...
            return new IteratorSequence<K>(cache.keySet().iterator());
        }
        // Use a distributed executor to execute callables throughout the Infinispan cluster ...
        DistributedExecutorService distributedExecutor = new DefaultExecutorService(cache);
        final List<Future<Set<K>>> futures = distributedExecutor.submitEverywhere(new GetAllKeys<K, V>());
        return new DistributedKeySequence<K>(futures);
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
    }

    public static final class IteratorSequence<T> implements Sequence<T> {
        private final Iterator<T> iterator;

        public IteratorSequence( Iterator<T> iterator ) {
            this.iterator = iterator;
        }

        @Override
        public T next() {
            return iterator.hasNext() ? iterator.next() : null;
        }
    }

    /**
     * A {@link DistributedCallable} implementation that returns the complete set of keys in an Infinispan cache.
     * 
     * @param <K> the type of key
     * @param <V> the type of value
     */
    protected static final class GetAllKeys<K, V> implements DistributedCallable<K, V, Set<K>> {
        private Set<K> keys;

        @Override
        public void setEnvironment( Cache<K, V> cache,
                                    Set<K> inputKeys ) {
            this.keys = inputKeys;
        }

        @Override
        public Set<K> call() throws Exception {
            return keys;
        }
    }

    /**
     * A {@link Sequence} implementation that returns the keys returned by multiple futures that make take time to complete. This
     * method attempts to use the futures as soon as they are completed.
     * 
     * @param <T> the key type
     */
    protected static class DistributedKeySequence<T> implements Sequence<T> {
        private final List<Future<Set<T>>> futures;
        private Iterator<T> currentIter;

        protected DistributedKeySequence( List<Future<Set<T>>> futures ) {
            // Make a copy so that we can remove entries as we process ...
            this.futures = new LinkedList<Future<Set<T>>>(futures);
        }

        @Override
        public T next() throws ExecutionException, CancellationException, InterruptedException {
            while (true) {
                if (futures.isEmpty()) {
                    // No more futures, so we're done!
                    return null;
                }
                if (currentIter == null) {
                    // Wait until the next results are available ...
                    while (true) {
                        // Get the next future that is ready ...
                        Iterator<Future<Set<T>>> futureIter = futures.iterator();
                        while (futureIter.hasNext()) {
                            Future<Set<T>> future = futureIter.next();
                            try {
                                // But done't wait too long for this future ...
                                Set<T> keys = future.get(100, TimeUnit.MILLISECONDS);
                                // We got some keys, so this future is done and should be removed from our list ...
                                futureIter.remove();
                                // And set the current iterator to these keys ...
                                currentIter = keys.iterator();
                            } catch (TimeoutException e) {
                                // continue;
                            }
                        }
                        if (futures.isEmpty()) break;
                    }
                    if (currentIter == null) {
                        // No more futures, so we're done!
                        return null;
                    }
                }
                while (currentIter.hasNext()) {
                    T key = currentIter.next();
                    if (key != null) return key;
                }
                // We're done with this iterator ...
                currentIter = null;
            }
        }

    }

}
