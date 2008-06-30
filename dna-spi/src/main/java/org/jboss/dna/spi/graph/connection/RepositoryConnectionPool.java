/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.spi.graph.connection;

import java.util.concurrent.TimeUnit;

/**
 * @author Randall Hauch
 */
public interface RepositoryConnectionPool extends RepositoryConnectionFactory {

    /**
     * Initiates an orderly shutdown in which connections that are currently in use are allowed to be used and closed as normal,
     * but no new connections will be created. Invocation has no additional effect if already shut down.
     * <p>
     * Once the pool has been shutdown, it may not be used to {@link #getConnection() get connections}.
     * </p>
     * 
     * @throws SecurityException if a security manager exists and shutting down this pool may manipulate threads that the caller
     *         is not permitted to modify because it does not hold {@link java.lang.RuntimePermission}<tt>("modifyThread")</tt>,
     *         or the security manager's <tt>checkAccess</tt> method denies access.
     * @see #shutdownNow()
     */
    void shutdown();

    /**
     * Attempts to close all connections, including those connections currently in use, and prevent the use of other connections.
     * 
     * @throws SecurityException if a security manager exists and shutting down this pool may manipulate threads that the caller
     *         is not permitted to modify because it does not hold {@link java.lang.RuntimePermission}<tt>("modifyThread")</tt>,
     *         or the security manager's <tt>checkAccess</tt> method denies access.
     * @see #shutdown()
     */
    void shutdownNow();

    /**
     * Return whether this connection pool is running and is able to {@link #getConnection() provide connections}. Note that this
     * method is effectively <code>!isShutdown()</code>.
     * 
     * @return true if this pool is running, or false otherwise
     * @see #isShutdown()
     * @see #isTerminated()
     * @see #isTerminating()
     */
    boolean isRunning();

    /**
     * Return whether this connection pool is in the process of shutting down or has already been shut down. A result of
     * <code>true</code> signals that the pool may no longer be used. Note that this method is effectively
     * <code>!isRunning()</code>.
     * 
     * @return true if this pool has been shut down, or false otherwise
     * @see #isShutdown()
     * @see #isTerminated()
     * @see #isTerminating()
     */
    boolean isShutdown();

    /**
     * Returns true if this pool is in the process of terminating after {@link #shutdown()} or {@link #shutdownNow()} has been
     * called but has not completely terminated. This method may be useful for debugging. A return of <tt>true</tt> reported a
     * sufficient period after shutdown may indicate that submitted tasks have ignored or suppressed interruption, causing this
     * executor not to properly terminate.
     * 
     * @return true if terminating but not yet terminated, or false otherwise
     * @see #isTerminated()
     */
    boolean isTerminating();

    /**
     * Return true if this pool has completed its termination and no longer has any open connections.
     * 
     * @return true if terminated, or false otherwise
     * @see #isTerminating()
     */
    boolean isTerminated();

    /**
     * Method that can be called after {@link #shutdown()} or {@link #shutdownNow()} to wait until all connections in use at the
     * time those methods were called have been closed normally. This method accepts a maximum time duration, after which it will
     * return even if all connections have not been closed.
     * 
     * @param timeout the maximum time to wait for all connections to be closed and returned to the pool
     * @param unit the time unit for <code>timeout</code>
     * @return true if the pool was terminated in the supplied time (or was already terminated), or false if the timeout occurred
     *         before all the connections were closed
     * @throws InterruptedException if the thread was interrupted
     */
    boolean awaitTermination( long timeout,
                              TimeUnit unit ) throws InterruptedException;

}
