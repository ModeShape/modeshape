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
package org.modeshape.common.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Factory interface for creating/obtaining named thread pools.
 */
public interface ThreadPoolFactory {

    /**
     * Obtain a thread pool with the supplied name, or create and return one if no thread pool exists with that name. When
     * finished with the thread pool, it should be {@link #releaseThreadPool released}.
     * 
     * @param name the name of the thread pool; may not be null
     * @return the thread pool executor; never null
     */
    ExecutorService getThreadPool( String name );

    /**
     * Signal that the supplied thread pool is no longer needed. Obtain a cached thread pool with the supplied name, or create and
     * return one if no thread pool exists with that name. When finished with the thread pool, it should be
     * {@link #releaseThreadPool released}.
     * 
     * @param maxPoolSize the maximum number of threads that can be spawned by this pool.
     * @param name the name of the thread pool; may not be null
     * @return the thread pool executor; never null
     */
    ExecutorService getCachedTreadPool( String name, int maxPoolSize );

    /**
     * Obtain a scheduled thread pool with the supplied name, or create and return one if no thread pool exists with that name.
     * When finished with the thread pool, it should be {@link #releaseThreadPool released}.
     * 
     * @param name the name of the thread pool; may not be null
     * @return the thread pool executor; never null
     */
    ScheduledExecutorService getScheduledThreadPool( String name );

    /**
     * Performs a {@link java.util.concurrent.ExecutorService#shutdownNow()} on the given pool, if the pool has been created
     * previously by this class. Clients which use this method should handle, if necessary, any potential
     * {@link InterruptedException}
     * 
     * @param pool the pool that is no longer needed
     */
    void releaseThreadPool( ExecutorService pool );

    /**
     * Terminates all the existing thread pool, by waiting for them maximum {@code maxWaitTimeMillis} milliseconds, after which
     * calling {@link java.util.concurrent.ExecutorService#shutdownNow()}.
     * 
     * @param maxWaitTime the maximum amount of time that should be given to the pools to shutdown on their own; must be
     *        non-negative
     * @param timeUnit the unit of time for the {@code maxWaitTime} parameter
     */
    void terminateAllPools( long maxWaitTime,
                            TimeUnit timeUnit );
}
