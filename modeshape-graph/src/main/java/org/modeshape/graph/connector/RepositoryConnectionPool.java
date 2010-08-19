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
package org.modeshape.graph.connector;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import javax.transaction.xa.XAResource;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.modeshape.common.annotation.Category;
import org.modeshape.common.annotation.Description;
import org.modeshape.common.annotation.Label;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.Logger;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.cache.CachePolicy;
import org.modeshape.graph.request.Request;

/**
 * A reusable implementation of a managed pool of connections that is optimized for safe concurrent operations.
 */
@ThreadSafe
public class RepositoryConnectionPool {

    /**
     * The default time to wait for ping when called from a client.
     */
    public static final long DEFAULT_TIME_TO_WAIT = 10;

    /**
     * The default TimeUnit to wait for ping when called from a client.
     */
    public static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.SECONDS;

    /**
     * The core pool size for default-constructed pools is {@value} .
     */
    public static final int DEFAULT_CORE_POOL_SIZE = 1;

    /**
     * The maximum pool size for default-constructed pools is {@value} .
     */
    public static final int DEFAULT_MAXIMUM_POOL_SIZE = 10;

    /**
     * The keep-alive time for connections in default-constructed pools is * * * * {@value} seconds.
     */
    public static final long DEFAULT_KEEP_ALIVE_TIME_IN_SECONDS = 30;

    /**
     * Permission for checking shutdown
     */
    private static final RuntimePermission shutdownPerm = new RuntimePermission("modifyThread");

    /**
     * The source that this pool uses to create new connections.
     */
    private final RepositorySource source;

    /**
     * Lock held on updates to poolSize, corePoolSize, maximumPoolSize, and workers set.
     */
    private final ReentrantLock mainLock = new ReentrantLock();

    /**
     * Wait condition to support awaitTermination
     */
    private final Condition termination = mainLock.newCondition();

    /**
     * Set containing all connections that are available for use.
     */
    @GuardedBy( "mainLock" )
    private final BlockingQueue<ConnectionWrapper> availableConnections = new LinkedBlockingQueue<ConnectionWrapper>();

    /**
     * The connections that are currently in use.
     */
    @GuardedBy( "mainLock" )
    private final Set<ConnectionWrapper> inUseConnections = new HashSet<ConnectionWrapper>();

    /**
     * Timeout in nanoseconds for idle connections waiting to be used. Threads use this timeout only when there are more than
     * corePoolSize present. Otherwise they wait forever to be used.
     */
    @Description( i18n = GraphI18n.class, value = "poolKeepAliveTimeDescription" )
    @Label( i18n = GraphI18n.class, value = "poolKeepAliveTimeLabel" )
    @Category( i18n = GraphI18n.class, value = "poolKeepAliveTimeCategory" )
    private volatile long keepAliveTime;

    /**
     * The target pool size, updated only while holding mainLock, but volatile to allow concurrent readability even during
     * updates.
     */
    @Description( i18n = GraphI18n.class, value = "poolCorePoolSizeDescription" )
    @Label( i18n = GraphI18n.class, value = "poolCorePoolSizeLabel" )
    @Category( i18n = GraphI18n.class, value = "poolCorePoolSizeCategory" )
    @GuardedBy( "mainLock" )
    private volatile int corePoolSize;

    /**
     * Maximum pool size, updated only while holding mainLock but volatile to allow concurrent readability even during updates.
     */
    @Description( i18n = GraphI18n.class, value = "poolMaxiumumPoolSizeDescription" )
    @Label( i18n = GraphI18n.class, value = "poolMaxiumumPoolSizeLabel" )
    @Category( i18n = GraphI18n.class, value = "poolMaxiumumPoolSizeCategory" )
    @GuardedBy( "mainLock" )
    private volatile int maximumPoolSize;

    /**
     * Current pool size, updated only while holding mainLock but volatile to allow concurrent readability even during updates.
     */
    @GuardedBy( "mainLock" )
    private volatile int poolSize;

    /**
     * Lifecycle state, updated only while holding mainLock but volatile to allow concurrent readability even during updates.
     */
    @GuardedBy( "mainLock" )
    private volatile int runState;

    // Special values for runState
    /** Normal, not-shutdown mode */
    static final int RUNNING = 0;
    /** Controlled shutdown mode */
    static final int SHUTDOWN = 1;
    /** Immediate shutdown mode */
    static final int STOP = 2;
    /** Final state */
    static final int TERMINATED = 3;

    /**
     * Flag specifying whether a connection should be validated before returning it from the {@link #getConnection()} method.
     */
    @Description( i18n = GraphI18n.class, value = "poolValidateConnectionBeforeUseDescription" )
    @Label( i18n = GraphI18n.class, value = "poolValidateConnectionBeforeUseLabel" )
    @Category( i18n = GraphI18n.class, value = "poolValidateConnectionBeforeUseCategory" )
    private final AtomicBoolean validateConnectionBeforeUse = new AtomicBoolean(false);

    /**
     * The time in nanoseconds that ping should wait before timing out and failing.
     */
    @Description( i18n = GraphI18n.class, value = "poolPingTimeoutDescription" )
    @Label( i18n = GraphI18n.class, value = "poolPingTimeoutLabel" )
    @Category( i18n = GraphI18n.class, value = "poolPingTimeoutCategory" )
    private final AtomicLong pingTimeout = new AtomicLong(0);

    /**
     * The number of times an attempt to obtain a connection should fail with invalid connections before throwing an exception.
     */
    @Description( i18n = GraphI18n.class, value = "poolMaximumFailedAttemptsBeforeErrorDescription" )
    @Label( i18n = GraphI18n.class, value = "poolMaximumFailedAttemptsBeforeErrorLabel" )
    @Category( i18n = GraphI18n.class, value = "poolMaximumFailedAttemptsBeforeErrorCategory" )
    private final AtomicInteger maxFailedAttemptsBeforeError = new AtomicInteger(10);

    private final AtomicLong totalConnectionsCreated = new AtomicLong(0);

    private final AtomicLong totalConnectionsUsed = new AtomicLong(0);

    private static final Logger LOGGER = Logger.getLogger(RepositoryConnectionPool.class);

    /**
     * Create the pool to use the supplied connection factory, which is typically a {@link RepositorySource}. This constructor
     * uses the {@link #DEFAULT_CORE_POOL_SIZE default core pool size}, {@link #DEFAULT_MAXIMUM_POOL_SIZE default maximum pool
     * size}, and {@link #DEFAULT_KEEP_ALIVE_TIME_IN_SECONDS default keep-alive time (in seconds)}.
     * 
     * @param source the source for connections
     * @throws IllegalArgumentException if the connection factory is null or any of the supplied arguments are invalid
     */
    public RepositoryConnectionPool( RepositorySource source ) {
        this(source, DEFAULT_CORE_POOL_SIZE, DEFAULT_MAXIMUM_POOL_SIZE, DEFAULT_KEEP_ALIVE_TIME_IN_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Create the pool to use the supplied connection factory, which is typically a {@link RepositorySource}.
     * 
     * @param source the source for connections
     * @param corePoolSize the number of connections to keep in the pool, even if they are idle.
     * @param maximumPoolSize the maximum number of connections to allow in the pool.
     * @param keepAliveTime when the number of connection is greater than the core, this is the maximum time that excess idle
     *        connections will be kept before terminating.
     * @param unit the time unit for the keepAliveTime argument.
     * @throws IllegalArgumentException if the connection factory is null or any of the supplied arguments are invalid
     */
    public RepositoryConnectionPool( RepositorySource source,
                                     int corePoolSize,
                                     int maximumPoolSize,
                                     long keepAliveTime,
                                     TimeUnit unit ) {
        CheckArg.isNonNegative(corePoolSize, "corePoolSize");
        CheckArg.isPositive(maximumPoolSize, "maximumPoolSize");
        CheckArg.isNonNegative(keepAliveTime, "keepAliveTime");
        CheckArg.isNotNull(source, "source");
        if (maximumPoolSize < corePoolSize) {
            throw new IllegalArgumentException(GraphI18n.maximumPoolSizeMayNotBeSmallerThanCorePoolSize.text());
        }
        this.source = source;
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.keepAliveTime = unit.toNanos(keepAliveTime);
        this.setPingTimeout(100, TimeUnit.MILLISECONDS);
    }

    /**
     * Used to get and test a connection in the pool
     * 
     * @param time
     * @param unit
     * @return boolean
     * @throws InterruptedException
     */
    public boolean ping( long time,
                         TimeUnit unit ) throws InterruptedException {

        if (isRunning()) {
            RepositoryConnection connection = null;
            Throwable error = null;
            try {
                connection = getConnection();
                return connection.ping(time, unit);
            } catch (RuntimeException t) {
                error = t;
                throw t;
            } catch (InterruptedException t) {
                error = t;
                throw t;
            } finally {
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (RuntimeException e) {
                        if (error == null) throw e;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Used to get and test a connection in the pool with default wait and time unit values
     * 
     * @return boolean
     * @throws InterruptedException
     */
    public boolean ping() throws InterruptedException {
        return ping(DEFAULT_TIME_TO_WAIT, DEFAULT_TIME_UNIT);
    }

    /**
     * Get the {@link RepositorySource} that's used by this pool.
     * 
     * @return the repository source; never null
     */
    public final RepositorySource getRepositorySource() {
        return source;
    }

    /**
     * Get the name of this pool, which delegates to the connection factory.
     * 
     * @return the name of the source
     */
    protected String getSourceName() {
        return source.getName();
    }

    // -------------------------------------------------
    // Property settings ...
    // -------------------------------------------------

    /**
     * @return validateConnectionBeforeUse
     */
    public boolean getValidateConnectionBeforeUse() {
        return this.validateConnectionBeforeUse.get();
    }

    /**
     * @param validateConnectionBeforeUse Sets validateConnectionBeforeUse to the specified value.
     */
    public void setValidateConnectionBeforeUse( boolean validateConnectionBeforeUse ) {
        this.validateConnectionBeforeUse.set(validateConnectionBeforeUse);
    }

    /**
     * @return pingTimeout
     */
    public long getPingTimeoutInNanos() {
        return this.pingTimeout.get();
    }

    /**
     * @param pingTimeout the time to wait for a ping to complete
     * @param unit the time unit of the time argument
     */
    public void setPingTimeout( long pingTimeout,
                                TimeUnit unit ) {
        CheckArg.isNonNegative(pingTimeout, "time");
        this.pingTimeout.set(unit.toNanos(pingTimeout));
    }

    /**
     * The ping timeout, in seconds.
     * 
     * @return pingTimeout
     */
    public long getPingTimeout() {
        return TimeUnit.NANOSECONDS.toSeconds(this.pingTimeout.get());
    }

    /**
     * Sets the ping timeout, in seconds.
     * 
     * @param pingTimeoutInSeconds the time to wait for a ping to complete
     */
    public void setPingTimeout( long pingTimeoutInSeconds ) {
        setPingTimeout(pingTimeoutInSeconds, TimeUnit.SECONDS);
    }

    /**
     * @return maxFailedAttemptsBeforeError
     */
    public int getMaxFailedAttemptsBeforeError() {
        return this.maxFailedAttemptsBeforeError.get();
    }

    /**
     * @param maxFailedAttemptsBeforeError Sets maxFailedAttemptsBeforeError to the specified value.
     */
    public void setMaxFailedAttemptsBeforeError( int maxFailedAttemptsBeforeError ) {
        this.maxFailedAttemptsBeforeError.set(maxFailedAttemptsBeforeError);
    }

    /**
     * Sets the time limit for which connections may remain idle before being closed. If there are more than the core number of
     * connections currently in the pool, after waiting this amount of time without being used, excess threads will be terminated.
     * This overrides any value set in the constructor.
     * 
     * @param time the time to wait. A time value of zero will cause excess connections to terminate immediately after being
     *        returned.
     * @param unit the time unit of the time argument
     * @throws IllegalArgumentException if time less than zero
     * @see #getKeepAliveTime
     */
    public void setKeepAliveTime( long time,
                                  TimeUnit unit ) {
        CheckArg.isNonNegative(time, "time");
        this.keepAliveTime = unit.toNanos(time);
    }

    /**
     * Returns the connection keep-alive time, which is the amount of time which connections in excess of the core pool size may
     * remain idle before being closed.
     * 
     * @param unit the desired time unit of the result
     * @return the time limit
     * @see #setKeepAliveTime
     */
    public long getKeepAliveTime( TimeUnit unit ) {
        assert unit != null;
        return unit.convert(keepAliveTime, TimeUnit.NANOSECONDS);
    }

    /**
     * Returns the connection keep-alive time, which is the amount of time which connections in excess of the core pool size may
     * remain idle before being closed.
     * 
     * @return the time limit in seconds
     * @see #setKeepAliveTime
     */
    public long getKeepAliveTime() {
        return getKeepAliveTime(TimeUnit.SECONDS);
    }

    /**
     * Sets the time limit for which connections may remain idle before being closed. If there are more than the core number of
     * connections currently in the pool, after waiting this amount of time without being used, excess threads will be terminated.
     * This overrides any value set in the constructor.
     * 
     * @param time the time to wait, in seconds. A time value of zero will cause excess connections to terminate immediately after
     *        being returned.
     * @throws IllegalArgumentException if time less than zero
     * @see #getKeepAliveTime
     */
    public void setKeepAliveTime( long time ) {
        setKeepAliveTime(time, TimeUnit.SECONDS);
    }

    /**
     * @return maximumPoolSize
     */
    public int getMaximumPoolSize() {
        return this.maximumPoolSize;
    }

    /**
     * Sets the maximum allowed number of connections. This overrides any value set in the constructor. If the new value is
     * smaller than the current value, excess existing but unused connections will be closed.
     * 
     * @param maximumPoolSize the new maximum
     * @throws IllegalArgumentException if maximumPoolSize less than zero or the {@link #getCorePoolSize() core pool size}
     * @see #getMaximumPoolSize
     */
    public void setMaximumPoolSize( int maximumPoolSize ) {
        CheckArg.isPositive(maximumPoolSize, "maximum pool size");
        if (maximumPoolSize < corePoolSize) {
            throw new IllegalArgumentException(GraphI18n.maximumPoolSizeMayNotBeSmallerThanCorePoolSize.text());
        }
        final ReentrantLock mainLock = this.mainLock;
        try {
            mainLock.lock();
            int extra = this.maximumPoolSize - maximumPoolSize;
            this.maximumPoolSize = maximumPoolSize;
            if (extra > 0 && poolSize > maximumPoolSize) {
                // Drain the extra connections from those available ...
                drainUnusedConnections(extra);
            }
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Returns the core number of connections.
     * 
     * @return the core number of connections
     * @see #setCorePoolSize(int)
     */
    public int getCorePoolSize() {
        return this.corePoolSize;
    }

    /**
     * Sets the core number of connections. This overrides any value set in the constructor. If the new value is smaller than the
     * current value, excess existing and unused connections will be closed. If larger, new connections will, if needed, be
     * created.
     * 
     * @param corePoolSize the new core size
     * @throws RepositorySourceException if there was an error obtaining the new connection
     * @throws InterruptedException if the thread was interrupted during the operation
     * @throws IllegalArgumentException if <tt>corePoolSize</tt> less than zero
     * @see #getCorePoolSize()
     */
    public void setCorePoolSize( int corePoolSize ) throws RepositorySourceException, InterruptedException {
        CheckArg.isNonNegative(corePoolSize, "core pool size");
        if (maximumPoolSize < corePoolSize) {
            throw new IllegalArgumentException(GraphI18n.maximumPoolSizeMayNotBeSmallerThanCorePoolSize.text());
        }
        final ReentrantLock mainLock = this.mainLock;
        try {
            mainLock.lock();
            int extra = this.corePoolSize - corePoolSize;
            this.corePoolSize = corePoolSize;
            if (extra < 0) {
                // Add connections ...
                addConnectionsIfUnderCorePoolSize();
            } else if (extra > 0 && poolSize > corePoolSize) {
                // Drain the extra connections from those available ...
                drainUnusedConnections(extra);
            }
        } finally {
            mainLock.unlock();
        }
    }

    // -------------------------------------------------
    // Statistics ...
    // -------------------------------------------------

    /**
     * Returns the current number of connections in the pool, including those that are checked out (in use) and those that are not
     * being used.
     * 
     * @return the number of connections
     */
    public int getPoolSize() {
        return poolSize;
    }

    /**
     * Returns the approximate number of connections that are currently checked out from the pool.
     * 
     * @return the number of checked-out connections
     */
    public int getInUseCount() {
        final ReentrantLock mainLock = this.mainLock;
        try {
            mainLock.lock();
            return this.inUseConnections.size();
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Get the total number of connections that have been created by this pool.
     * 
     * @return the total number of connections created by this pool
     */
    public long getTotalConnectionsCreated() {
        return this.totalConnectionsCreated.get();
    }

    /**
     * Get the total number of times connections have been {@link #getConnection()} used.
     * 
     * @return the total number
     */
    public long getTotalConnectionsUsed() {
        return this.totalConnectionsUsed.get();
    }

    // -------------------------------------------------
    // State management methods ...
    // -------------------------------------------------

    /**
     * Starts a core connection, causing it to idly wait for use. This overrides the default policy of starting core connections
     * only when they are {@link #getConnection() needed}. This method will return <tt>false</tt> if all core connections have
     * already been started.
     * 
     * @return true if a connection was started
     * @throws RepositorySourceException if there was an error obtaining the new connection
     * @throws InterruptedException if the thread was interrupted during the operation
     */
    public boolean prestartCoreConnection() throws RepositorySourceException, InterruptedException {
        final ReentrantLock mainLock = this.mainLock;
        try {
            mainLock.lock();
            return addConnectionIfUnderCorePoolSize();
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Starts all core connections, causing them to idly wait for use. This overrides the default policy of starting core
     * connections only when they are {@link #getConnection() needed}.
     * 
     * @return the number of connections started.
     * @throws RepositorySourceException if there was an error obtaining the new connection
     * @throws InterruptedException if the thread was interrupted during the operation
     */
    public int prestartAllCoreConnections() throws RepositorySourceException, InterruptedException {
        final ReentrantLock mainLock = this.mainLock;
        try {
            mainLock.lock();
            return addConnectionsIfUnderCorePoolSize();
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Initiates an orderly shutdown of the pool in which connections that are currently in use are allowed to be used and closed
     * as normal, but no new connections will be created. Invocation has no additional effect if already shut down.
     * <p>
     * Once the pool has been shutdown, it may not be used to {@link #getConnection() get connections}.
     * </p>
     * 
     * @throws SecurityException if a security manager exists and shutting down this pool may manipulate threads that the caller
     *         is not permitted to modify because it does not hold {@link java.lang.RuntimePermission} <tt>("modifyThread")</tt>,
     *         or the security manager's <tt>checkAccess</tt> method denies access.
     * @see #shutdownNow()
     */
    public void shutdown() {
        // Fail if caller doesn't have modifyThread permission. We
        // explicitly check permissions directly because we can't trust
        // implementations of SecurityManager to correctly override
        // the "check access" methods such that our documented
        // security policy is implemented.
        SecurityManager security = System.getSecurityManager();
        if (security != null) java.security.AccessController.checkPermission(shutdownPerm);

        LOGGER.debug("Shutting down repository connection pool for {0}", getSourceName());
        boolean fullyTerminated = false;
        final ReentrantLock mainLock = this.mainLock;
        try {
            mainLock.lock();
            int state = this.runState;
            if (state == RUNNING) {
                // don't override shutdownNow
                this.runState = SHUTDOWN;
            }

            // Kill the maintenance thread ...

            // Remove and close all available connections ...
            if (!this.availableConnections.isEmpty()) {
                // Drain the extra connections from those available ...
                drainUnusedConnections(this.availableConnections.size());
            }

            // If there are no connections being used, trigger full termination
            // now ...
            if (this.inUseConnections.isEmpty()) {
                fullyTerminated = true;
                LOGGER.trace("Signalling termination of repository connection pool for {0}", getSourceName());
                runState = TERMINATED;
                termination.signalAll();
                LOGGER.debug("Terminated repository connection pool for {0}", getSourceName());
            }
            // Otherwise the last connection that is closed will transition the
            // runState to TERMINATED ...
        } finally {
            mainLock.unlock();
        }
        if (fullyTerminated) terminated();
    }

    /**
     * Attempts to close all connections in the pool, including those connections currently in use, and prevent the use of other
     * connections.
     * 
     * @throws SecurityException if a security manager exists and shutting down this pool may manipulate threads that the caller
     *         is not permitted to modify because it does not hold {@link java.lang.RuntimePermission} <tt>("modifyThread")</tt>,
     *         or the security manager's <tt>checkAccess</tt> method denies access.
     * @see #shutdown()
     */
    public void shutdownNow() {
        // Almost the same code as shutdown()
        SecurityManager security = System.getSecurityManager();
        if (security != null) java.security.AccessController.checkPermission(shutdownPerm);

        LOGGER.debug("Shutting down (immediately) repository connection pool for {0}", getSourceName());
        boolean fullyTerminated = false;
        final ReentrantLock mainLock = this.mainLock;
        try {
            mainLock.lock();
            int state = this.runState;
            if (state != TERMINATED) {
                // don't override shutdownNow
                this.runState = STOP;
            }

            // Kill the maintenance thread ...

            // Remove and close all available connections ...
            if (!this.availableConnections.isEmpty()) {
                // Drain the extra connections from those available ...
                drainUnusedConnections(this.availableConnections.size());
            }

            // If there are connections being used, close them now ...
            if (!this.inUseConnections.isEmpty()) {
                for (ConnectionWrapper connectionInUse : this.inUseConnections) {
                    LOGGER.trace("Closing repository connection to {0}", getSourceName());
                    connectionInUse.getOriginal().close();
                }
                this.poolSize -= this.inUseConnections.size();
                // The last connection that is closed will transition the
                // runState to TERMINATED ...
            } else {
                // There are no connections in use, so trigger full termination
                // now ...
                fullyTerminated = true;
                LOGGER.trace("Signalling termination of repository connection pool for {0}", getSourceName());
                runState = TERMINATED;
                termination.signalAll();
                LOGGER.debug("Terminated repository connection pool for {0}", getSourceName());
            }

        } finally {
            mainLock.unlock();
        }
        if (fullyTerminated) terminated();
    }

    /**
     * Return whether this connection pool is running and is able to {@link #getConnection() provide connections}. Note that this
     * method is effectively <code>!isShutdown()</code>.
     * 
     * @return true if this pool is running, or false otherwise
     * @see #isShutdown()
     * @see #isTerminated()
     * @see #isTerminating()
     */
    public boolean isRunning() {
        return runState == RUNNING;
    }

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
    public boolean isShutdown() {
        return runState != RUNNING;
    }

    /**
     * Returns true if this pool is in the process of terminating after {@link #shutdown()} or {@link #shutdownNow()} has been
     * called but has not completely terminated. This method may be useful for debugging. A return of <tt>true</tt> reported a
     * sufficient period after shutdown may indicate that submitted tasks have ignored or suppressed interruption, causing this
     * executor not to properly terminate.
     * 
     * @return true if terminating but not yet terminated, or false otherwise
     * @see #isTerminated()
     */
    public boolean isTerminating() {
        return runState == STOP;
    }

    /**
     * Return true if this pool has completed its termination and no longer has any open connections.
     * 
     * @return true if terminated, or false otherwise
     * @see #isTerminating()
     */
    public boolean isTerminated() {
        return runState == TERMINATED;
    }

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
    public boolean awaitTermination( long timeout,
                                     TimeUnit unit ) throws InterruptedException {
        LOGGER.trace("Awaiting termination");
        long nanos = unit.toNanos(timeout);
        final ReentrantLock mainLock = this.mainLock;
        try {
            mainLock.lock();
            for (;;) {
                // LOGGER.trace("---> Run state = {0}; condition = {1}, {2} open",
                // runState, termination, poolSize);
                if (runState == TERMINATED) return true;
                if (nanos <= 0) return false;
                nanos = termination.awaitNanos(nanos);
                // LOGGER.trace("---> Done waiting: run state = {0}; condition = {1}, {2} open",runState,termination,poolSize)
                // ;
            }
        } finally {
            mainLock.unlock();
            LOGGER.trace("Finished awaiting termination");
        }
    }

    /**
     * Method invoked when the pool has terminated. Default implementation does nothing. Note: To properly nest multiple
     * overridings, subclasses should generally invoke <tt>super.terminated</tt> within this method.
     */
    protected void terminated() {
    }

    /**
     * Invokes <tt>shutdown</tt> when this pool is no longer referenced.
     */
    @Override
    protected void finalize() {
        shutdown();
    }

    // -------------------------------------------------
    // Connection management methods ...
    // -------------------------------------------------

    /**
     * Get a connection from the pool. This method either returns an unused connection if one is available, creates a connection
     * if there is still room in the pool, or blocks until a connection becomes available if the pool already contains the maximum
     * number of connections and all connections are currently being used.
     * 
     * @return a connection
     * @throws RepositorySourceException if there is a problem obtaining a connection
     * @throws IllegalStateException if the factory is not in a state to create or return connections
     */
    public RepositoryConnection getConnection() throws RepositorySourceException {
        int attemptsAllowed = this.maxFailedAttemptsBeforeError.get();
        ConnectionWrapper connection = null;
        // Do this until we get a good connection ...
        int attemptsRemaining = attemptsAllowed;
        while (connection == null && attemptsRemaining > 0) {
            --attemptsRemaining;
            ReentrantLock mainLock = this.mainLock;
            try {
                mainLock.lock();
                // If we're shutting down the pool, then just close the
                // connection ...
                if (this.runState != RUNNING) {
                    throw new IllegalStateException(GraphI18n.repositoryConnectionPoolIsNotRunning.text());
                }
                // If there are fewer total connections than the core size ...
                if (this.poolSize < this.corePoolSize) {
                    // Immediately create a wrapped connection and return it ...
                    connection = newWrappedConnection();
                }
                // Peek to see if there is a connection available ...
                else if (this.availableConnections.peek() != null) {
                    // There is, so take it and return it ...
                    try {
                        connection = this.availableConnections.take();
                    } catch (InterruptedException e) {
                        LOGGER.trace("Cancelled obtaining a repository connection from pool {0}", getSourceName());
                        Thread.interrupted();
                        throw new RepositorySourceException(getSourceName(), e);
                    }
                }
                // There is no connection available. If there are fewer total
                // connections than the maximum size ...
                else if (this.poolSize < this.maximumPoolSize) {
                    // Immediately create a wrapped connection and return it ...
                    connection = newWrappedConnection();
                }
                if (connection != null) {
                    this.inUseConnections.add(connection);
                }
            } finally {
                mainLock.unlock();
            }
            if (connection == null) {
                // There are not enough connections, so wait in line for the
                // next available connection ...
                LOGGER.trace("Waiting for a repository connection from pool {0}", getSourceName());
                try {
                    connection = this.availableConnections.take();
                } catch (InterruptedException e) {
                    LOGGER.trace("Cancelled obtaining a repository connection from pool {0}", getSourceName());
                    Thread.interrupted();
                    throw new RepositorySourceException(getSourceName(), e);
                }
                mainLock = this.mainLock;
                mainLock.lock();
                try {
                    if (connection != null) {
                        this.inUseConnections.add(connection);
                    }
                } finally {
                    mainLock.unlock();
                }
                LOGGER.trace("Recieved a repository connection from pool {0}", getSourceName());
            }
            if (connection != null && this.validateConnectionBeforeUse.get()) {
                try {
                    connection = validateConnection(connection);
                } catch (InterruptedException e) {
                    LOGGER.trace("Cancelled validating a repository connection obtained from pool {0}", getSourceName());
                    returnConnection(connection);
                    Thread.interrupted();
                    throw new RepositorySourceException(getSourceName(), e);
                }
            }
        }
        if (connection == null) {
            // We were unable to obtain a usable connection, so fail ...
            throw new RepositorySourceException(GraphI18n.unableToObtainValidRepositoryAfterAttempts.text(attemptsAllowed));
        }
        this.totalConnectionsUsed.incrementAndGet();
        return connection;
    }

    /**
     * This method is automatically called by the {@link ConnectionWrapper} when it is {@link ConnectionWrapper#close() closed}.
     * 
     * @param wrapper the wrapper to the connection that is being returned to the pool
     */
    protected void returnConnection( ConnectionWrapper wrapper ) {
        assert wrapper != null;
        ConnectionWrapper wrapperToClose = null;
        final ReentrantLock mainLock = this.mainLock;
        try {
            mainLock.lock();
            // Remove the connection from the in-use set ...
            boolean removed = this.inUseConnections.remove(wrapper);
            assert removed;

            // If we're shutting down the pool, then just close the connection
            // ...
            if (this.runState != RUNNING) {
                wrapperToClose = wrapper;
            }
            // If there are more connections than the maximum size...
            else if (this.poolSize > this.maximumPoolSize) {
                // Immediately close this connection ...
                wrapperToClose = wrapper;
            }
            // Attempt to make the connection available (this should generally
            // work, unless there is an upper limit
            // to the number of available connections) ...
            else if (!this.availableConnections.offer(new ConnectionWrapper(wrapper.getOriginal()))) {
                // The pool of available connection is full, so release it ...
                wrapperToClose = wrapper;
            }
        } finally {
            mainLock.unlock();
        }
        // Close the connection if we're supposed to (do it outside of the main
        // lock)...
        if (wrapperToClose != null) {
            closeConnection(wrapperToClose);
        }
    }

    /**
     * Validate the supplied connection, returning the connection if valid or null if the connection is not valid.
     * 
     * @param connection the connection to be validated; may not be null
     * @return the validated connection, or null if the connection did not validate and was removed from the pool
     * @throws InterruptedException if the thread is interrupted while validating the connection
     */
    protected ConnectionWrapper validateConnection( ConnectionWrapper connection ) throws InterruptedException {
        assert connection != null;
        ConnectionWrapper invalidConnection = null;
        try {
            if (!connection.ping(this.pingTimeout.get(), TimeUnit.NANOSECONDS)) {
                invalidConnection = connection;
            }
        } finally {
            if (invalidConnection != null) {
                try {
                    mainLock.lock();
                    connection = null;
                    this.poolSize--;
                    this.inUseConnections.remove(connection);
                    // returnConnection(invalidConnection);
                } finally {
                    mainLock.unlock();
                }
            }
        }
        return connection;
    }

    /**
     * Obtain a new connection wrapped in a {@link ConnectionWrapper}. This method does not check whether creating the new
     * connection would violate the {@link #maximumPoolSize maximum pool size} nor does it add the new connection to the
     * {@link #availableConnections available connections} (as the caller may want it immediately), but it does increment the
     * {@link #poolSize pool size}.
     * 
     * @return the connection wrapper with a new connection
     * @throws RepositorySourceException if there was an error obtaining the new connection
     */
    @GuardedBy( "mainLock" )
    protected ConnectionWrapper newWrappedConnection() throws RepositorySourceException {
        RepositoryConnection connection = this.source.getConnection();
        ++this.poolSize;
        this.totalConnectionsCreated.incrementAndGet();
        return new ConnectionWrapper(connection);
    }

    /**
     * Close a connection that is in the pool but no longer in the {@link #availableConnections available connections}. This
     * method does decrement the {@link #poolSize pool size}.
     * 
     * @param wrapper the wrapper for the connection to be closed
     */
    protected void closeConnection( ConnectionWrapper wrapper ) {
        assert wrapper != null;
        RepositoryConnection original = wrapper.getOriginal();
        assert original != null;
        try {
            LOGGER.debug("Closing repository connection to {0} ({1} open connections remain)", getSourceName(), poolSize);
            original.close();
        } finally {
            final ReentrantLock mainLock = this.mainLock;
            try {
                mainLock.lock();
                // No matter what reduce the pool size count
                --this.poolSize;
                // And if shutting down and this was the last connection being
                // used...
                if ((runState == SHUTDOWN || runState == STOP) && this.poolSize <= 0) {
                    // then signal anybody that has called
                    // "awaitTermination(...)"
                    LOGGER.trace("Signalling termination of repository connection pool for {0}", getSourceName());
                    this.runState = TERMINATED;
                    this.termination.signalAll();
                    LOGGER.trace("Terminated repository connection pool for {0}", getSourceName());

                    // fall through to call terminate() outside of lock.
                }
            } finally {
                mainLock.unlock();
            }
        }
    }

    @GuardedBy( "mainLock" )
    protected int drainUnusedConnections( int count ) {
        if (count <= 0) return 0;
        LOGGER.trace("Draining up to {0} unused repository connections to {1}", count, getSourceName());
        // Drain the extra connections from those available ...
        Collection<ConnectionWrapper> extraConnections = new LinkedList<ConnectionWrapper>();
        this.availableConnections.drainTo(extraConnections, count);
        for (ConnectionWrapper connection : extraConnections) {
            LOGGER.trace("Closing repository connection to {0}", getSourceName());
            connection.getOriginal().close();
        }
        int numClosed = extraConnections.size();
        this.poolSize -= numClosed;
        LOGGER.trace("Drained {0} unused connections ({1} open connections remain)", numClosed, poolSize);
        return numClosed;
    }

    @GuardedBy( "mainLock" )
    protected boolean addConnectionIfUnderCorePoolSize() throws RepositorySourceException {
        // Add connection ...
        if (this.poolSize < this.corePoolSize) {
            this.availableConnections.offer(newWrappedConnection());
            LOGGER.trace("Added connection to {0} in undersized pool", getSourceName());
            return true;
        }
        return false;
    }

    @GuardedBy( "mainLock" )
    protected int addConnectionsIfUnderCorePoolSize() throws RepositorySourceException {
        // Add connections ...
        int n = 0;
        while (this.poolSize < this.corePoolSize) {
            this.availableConnections.offer(newWrappedConnection());
            ++n;
        }
        LOGGER.trace("Added {0} connection(s) to {1} in undersized pool", n, getSourceName());
        return n;
    }

    protected class ConnectionWrapper implements RepositoryConnection {

        private final RepositoryConnection original;
        private final long timeCreated;
        private long lastUsed;
        private boolean closed = false;

        protected ConnectionWrapper( RepositoryConnection connection ) {
            assert connection != null;
            this.original = connection;
            this.timeCreated = System.currentTimeMillis();
        }

        /**
         * @return original
         */
        protected RepositoryConnection getOriginal() {
            return this.original;
        }

        /**
         * @return lastUsed
         */
        public long getTimeLastUsed() {
            return this.lastUsed;
        }

        /**
         * @return timeCreated
         */
        public long getTimeCreated() {
            return this.timeCreated;
        }

        /**
         * {@inheritDoc}
         */
        public String getSourceName() {
            return this.original.getSourceName();
        }

        /**
         * {@inheritDoc}
         */
        public XAResource getXAResource() {
            if (closed) throw new IllegalStateException(GraphI18n.closedConnectionMayNotBeUsed.text());
            return this.original.getXAResource();
        }

        /**
         * {@inheritDoc}
         */
        public CachePolicy getDefaultCachePolicy() {
            if (closed) throw new IllegalStateException(GraphI18n.closedConnectionMayNotBeUsed.text());
            return this.original.getDefaultCachePolicy();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.connector.RepositoryConnection#execute(org.modeshape.graph.ExecutionContext,
         *      org.modeshape.graph.request.Request)
         */
        public void execute( ExecutionContext context,
                             Request request ) throws RepositorySourceException {
            if (closed) throw new IllegalStateException(GraphI18n.closedConnectionMayNotBeUsed.text());
            this.original.execute(context, request);
        }

        /**
         * {@inheritDoc}
         */
        public boolean ping( long time,
                             TimeUnit unit ) throws InterruptedException {
            if (closed) throw new IllegalStateException(GraphI18n.closedConnectionMayNotBeUsed.text());
            return this.original.ping(time, unit);
        }

        /**
         * {@inheritDoc}
         */
        public void close() {
            if (!closed) {
                this.lastUsed = System.currentTimeMillis();
                this.closed = true;
                returnConnection(this);
            }
        }
    }

}
