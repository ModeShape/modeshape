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

import org.modeshape.common.annotation.ThreadSafe;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A simple {@link ThreadPoolFactory} implementation.
 */
@ThreadSafe
public class ThreadPools implements ThreadPoolFactory {

    private static final int DEFAULT_MAX_THREAD_COUNT = 4;
    private static final int DEFAULT_SCHEDULED_THREAD_COUNT = 1;

    private final ConcurrentMap<String, ExecutorService> poolsByName = new ConcurrentHashMap<String, ExecutorService>();

    @Override
    public Executor getThreadPool( String name ) {
        return getOrCreateNewPool(name, Executors.newFixedThreadPool(DEFAULT_MAX_THREAD_COUNT, new NamedThreadFactory(name)));
    }

    @Override
    public Executor getCachedTreadPool( String name ) {
        return getOrCreateNewPool(name,  Executors.newCachedThreadPool(new NamedThreadFactory(name)));
    }

    @Override
    public Executor getScheduledThreadPool( String name ) {
        return getOrCreateNewPool(name, Executors.newScheduledThreadPool(DEFAULT_SCHEDULED_THREAD_COUNT, new NamedThreadFactory(name)));
    }

    private Executor getOrCreateNewPool( String name,
                                         ExecutorService executorService ) {
        ExecutorService executor = poolsByName.get(name);
        if (executor == null) {
            executor = poolsByName.putIfAbsent(name, executorService);
            if (executor != null) {
                // There was an existing one created since we originally checked, so shut down the new executor we just created
                // ...
                executor.shutdownNow();
            }
            executor = executorService;
        }
        return executor;
    }

    @Override
    public void releaseThreadPool( Executor executor ) {
        for (ExecutorService executorService : poolsByName.values()) {
            if (executor.equals(executorService)) {
                executorService.shutdown();
                return;
            }
        }
    }
}
