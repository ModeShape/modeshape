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

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.modeshape.common.annotation.ThreadSafe;

/**
 * A simple {@link ThreadPoolFactory} implementation.
 */
@ThreadSafe
public class ThreadPools implements ThreadPoolFactory {

    private static final int DEFAULT_MAX_THREAD_COUNT = 4;
    private static final int DEFAULT_SCHEDULED_THREAD_COUNT = 1;

    private final ConcurrentMap<String, ExecutorService> poolsByName = new ConcurrentHashMap<String, ExecutorService>();

    @Override
    public ExecutorService getThreadPool( String name ) {
        return getOrCreateNewPool(name, Executors.newFixedThreadPool(DEFAULT_MAX_THREAD_COUNT, new NamedThreadFactory(name)));
    }

    @Override
    public ExecutorService getCachedTreadPool( String name, int maxPoolSize ) {
        NamedThreadFactory threadFactory = new NamedThreadFactory(name);
        ExecutorService executorService = new ThreadPoolExecutor(0, maxPoolSize, 60L, TimeUnit.SECONDS, 
                                                                 new SynchronousQueue<Runnable>(), 
                                                                 threadFactory);
        return getOrCreateNewPool(name, executorService);
    }

    @Override
    public ScheduledExecutorService getScheduledThreadPool( String name ) {
        return (ScheduledExecutorService)getOrCreateNewPool(name,
                                                            Executors.newScheduledThreadPool(DEFAULT_SCHEDULED_THREAD_COUNT,
                                                                                             new NamedThreadFactory(name)));
    }

    private ExecutorService getOrCreateNewPool( String name,
                                                ExecutorService executorService ) {
        ExecutorService executor = poolsByName.get(name);
        if (executor == null) {
            executor = poolsByName.putIfAbsent(name, executorService);
            if (executor != null) {
                // There was an existing one created since we originally checked, so shut down the new executor we just created
                executor.shutdownNow();
            }
            executor = executorService;
        }
        return executor;
    }

    @Override
    public void releaseThreadPool( ExecutorService executor ) {
        for (String executorServiceName : poolsByName.keySet()) {
            ExecutorService executorService = poolsByName.get(executorServiceName);
            if (executor.equals(executorService)) {
                executorService.shutdown();
                return;
            }
        }
    }

    @Override
    public void terminateAllPools( long maxWaitTime,
                                   TimeUnit unit ) {
        // Calculate the time in the future when we don't need to wait any more ...
        long futureStopTimeInMillis = System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(maxWaitTime, unit);

        for (Iterator<Map.Entry<String, ExecutorService>> entryIterator = poolsByName.entrySet().iterator(); entryIterator.hasNext();) {
            Map.Entry<String, ExecutorService> entry = entryIterator.next();
            ExecutorService executorService = entry.getValue();
            if (!executorService.isShutdown()) {
                executorService.shutdown();
                // Calculate how long till we have to wait till the future stop time ...
                long waitTimeInMillis = futureStopTimeInMillis - System.currentTimeMillis();
                try {
                    if (waitTimeInMillis > 0) {
                        executorService.awaitTermination(waitTimeInMillis, TimeUnit.MILLISECONDS);
                    }
                    executorService.shutdownNow();
                } catch (InterruptedException e) {
                    Thread.interrupted();
                }
            }

            entryIterator.remove();
        }
    }
}
