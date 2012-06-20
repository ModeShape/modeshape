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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.modeshape.common.annotation.ThreadSafe;

/**
 * A simple {@link ThreadPoolFactory} implementation.
 */
@ThreadSafe
public class ThreadPools implements ThreadPoolFactory {

    public static final String DEFAULT_THREAD_NAME = "modeshape-workers";
    public static final int DEFAULT_MAX_THREAD_COUNT = 4;

    private final int defaultMaxThreadCount;
    private final ThreadFactory threadFactory;
    private final ConcurrentMap<String, ThreadPoolHolder> poolsByName = new ConcurrentHashMap<String, ThreadPoolHolder>();

    public ThreadPools() {
        this.threadFactory = new NamedThreadFactory(DEFAULT_THREAD_NAME);
        this.defaultMaxThreadCount = DEFAULT_MAX_THREAD_COUNT;
    }

    public ThreadPools( int defaultMaxThreads,
                        String threadFactoryName ) {
        CheckArg.isGreaterThan(0, defaultMaxThreads, "defaultMaxThreads");
        if (threadFactoryName == null || threadFactoryName.trim().length() == 0) threadFactoryName = DEFAULT_THREAD_NAME;
        this.threadFactory = new NamedThreadFactory(threadFactoryName);
        this.defaultMaxThreadCount = defaultMaxThreads;
    }

    public ThreadPools( int defaultMaxThreads,
                        ThreadFactory threadFactory ) {
        CheckArg.isGreaterThan(0, defaultMaxThreads, "defaultMaxThreads");
        CheckArg.isNotNull(threadFactory, "threadFactory");
        this.threadFactory = threadFactory;
        this.defaultMaxThreadCount = defaultMaxThreads;
    }

    @Override
    public Executor getThreadPool( String name ) {
        ThreadPoolHolder pool = poolsByName.get(name);
        if (pool == null) {
            // Create an executor ...
            ExecutorService newExecutor = Executors.newFixedThreadPool(defaultMaxThreadCount, threadFactory);
            ThreadPoolHolder newPool = new ThreadPoolHolder(newExecutor);
            pool = poolsByName.putIfAbsent(name, newPool);
            if (pool != null) {
                // There was an existing one created since we originally checked, so shut down the new executor we just created
                // ...
                newExecutor.shutdownNow();
            } else {
                pool = newPool;
            }
        }
        pool.users.incrementAndGet();
        return pool.threadPool;
    }

    @Override
    public void releaseThreadPool( Executor pool ) {
        for (ThreadPoolHolder holder : poolsByName.values()) {
            if (holder.release(pool)) return;
        }
    }

    protected static final class ThreadPoolHolder {
        protected final ExecutorService threadPool;
        protected final AtomicInteger users = new AtomicInteger(0);

        protected ThreadPoolHolder( ExecutorService threadPool ) {
            this.threadPool = threadPool;
        }

        protected boolean release( Executor pool ) {
            if (threadPool == pool) {
                // It's a match ...
                if (users.decrementAndGet() == 0) {
                    threadPool.shutdown();
                }
                return true;
            }
            return false;
        }
    }

}
