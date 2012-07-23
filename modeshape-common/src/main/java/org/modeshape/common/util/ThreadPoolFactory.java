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

import java.util.concurrent.ExecutorService;

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
     * Signal that the supplied thread pool is no longer needed.
     *
     * Obtain a cached thread pool with the supplied name, or create and return one if no thread pool exists with that name. When
     * finished with the thread pool, it should be {@link #releaseThreadPool released}.
     *
     * @param name the name of the thread pool; may not be null
     * @return the thread pool executor; never null
     */
    ExecutorService getCachedTreadPool( String name );

    /**
     * Obtain a scheduled thread pool with the supplied name, or create and return one if no thread pool exists with that name. When
     * finished with the thread pool, it should be {@link #releaseThreadPool released}.
     *
     * @param name the name of the thread pool; may not be null
     * @return the thread pool executor; never null
     */
    ExecutorService getScheduledThreadPool( String name );

    /**
     * Performs a {@link java.util.concurrent.ExecutorService#shutdownNow()} on the given pool, if the pool has been created
     * previously by this class. Clients which use this method should handle, if necessary, any potential {@link InterruptedException}
    *
     * @param pool the pool that is no longer needed
     */
    void releaseThreadPool( ExecutorService pool );

    /**
     * Terminates all the existing thread pool, by waiting for them maximum {@code maxWaitTimeMillis} milliseconds, after which
     * calling {@link java.util.concurrent.ExecutorService#shutdownNow()}.
     */
    void terminateAllPools( long maxWaitTimeMillis );
}
