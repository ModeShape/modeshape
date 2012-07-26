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
package org.modeshape.common.util;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    public ExecutorService getCachedTreadPool( String name ) {
        return getOrCreateNewPool(name, Executors.newCachedThreadPool(new NamedThreadFactory(name)));
    }

    @Override
    public ExecutorService getScheduledThreadPool( String name ) {
        return getOrCreateNewPool(name,
                                  Executors.newScheduledThreadPool(DEFAULT_SCHEDULED_THREAD_COUNT, new NamedThreadFactory(name)));
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
            executorService.shutdown();

            // Calculate how long till we have to wait till the future stop time ...
            long waitTimeInMillis = futureStopTimeInMillis - System.currentTimeMillis();
            try {
                if (waitTimeInMillis > 0) {
                    executorService.awaitTermination(waitTimeInMillis, TimeUnit.MILLISECONDS);
                }
                executorService.shutdownNow();
                entryIterator.remove();
            } catch (InterruptedException e) {
                Thread.interrupted();
            }
        }
    }
}
