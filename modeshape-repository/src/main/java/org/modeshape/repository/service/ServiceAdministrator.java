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
package org.modeshape.repository.service;

import java.util.concurrent.TimeUnit;
import net.jcip.annotations.ThreadSafe;

/**
 * Contract defining an administrative interface for controlling the running state of a service.
 */
@ThreadSafe
public interface ServiceAdministrator {

    /**
     * The available states.
     * 
     * @author Randall Hauch
     */
    public static enum State {
        STARTED,
        PAUSED,
        SHUTDOWN,
        TERMINATED;
    }

    /**
     * Return the current state of this system.
     * 
     * @return the current state
     */
    public State getState();

    /**
     * Set the state of the system. This method does nothing if the desired state matches the current state.
     * 
     * @param state the desired state
     * @return this object for method chaining purposes
     * @see #setState(String)
     * @see #start()
     * @see #pause()
     * @see #shutdown()
     */
    public ServiceAdministrator setState( State state );

    /**
     * Set the state of the system. This method does nothing if the desired state matches the current state.
     * 
     * @param state the desired state in string form
     * @return this object for method chaining purposes
     * @throws IllegalArgumentException if the specified state string is null or does not match one of the predefined
     *         {@link State predefined enumerated values}
     * @see #setState(State)
     * @see #start()
     * @see #pause()
     * @see #shutdown()
     */
    public ServiceAdministrator setState( String state );

    /**
     * Start monitoring and sequence the events. This method can be called multiple times, including after the system is
     * {@link #pause() paused}. However, once the system is {@link #shutdown() shutdown}, it cannot be started or paused.
     * 
     * @return this object for method chaining purposes
     * @throws IllegalStateException if called when the system has been {@link #shutdown() shutdown}.
     * @see #pause()
     * @see #shutdown()
     * @see #isStarted()
     */
    public ServiceAdministrator start();

    /**
     * Temporarily stop monitoring and sequencing events. This method can be called multiple times, including after the system is
     * {@link #start() started}. However, once the system is {@link #shutdown() shutdown}, it cannot be started or paused.
     * 
     * @return this object for method chaining purposes
     * @throws IllegalStateException if called when the system has been {@link #shutdown() shutdown}.
     * @see #start()
     * @see #shutdown()
     * @see #isPaused()
     */
    public ServiceAdministrator pause();

    /**
     * Permanently stop monitoring and sequencing events. This method can be called multiple times, but only the first call has an
     * effect. Once the system has been shutdown, it may not be {@link #start() restarted} or {@link #pause() paused}.
     * 
     * @return this object for method chaining purposes
     * @see #start()
     * @see #pause()
     * @see #isShutdown()
     */
    public ServiceAdministrator shutdown();

    /**
     * Blocks until the shutdown has completed, or the timeout occurs, or the current thread is interrupted, whichever happens
     * first.
     * 
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return <tt>true</tt> if this service complete shut down and <tt>false</tt> if the timeout elapsed before it was shut down
     *         completely
     * @throws InterruptedException if interrupted while waiting
     */
    boolean awaitTermination( long timeout,
                              TimeUnit unit ) throws InterruptedException;

    /**
     * Return whether this system has been started and is currently running.
     * 
     * @return true if started and currently running, or false otherwise
     * @see #start()
     * @see #pause()
     * @see #isPaused()
     * @see #isShutdown()
     * @see #isTerminated()
     */
    public boolean isStarted();

    /**
     * Return whether this system is currently paused.
     * 
     * @return true if currently paused, or false otherwise
     * @see #pause()
     * @see #start()
     * @see #isStarted()
     * @see #isShutdown()
     * @see #isTerminated()
     */
    public boolean isPaused();

    /**
     * Return whether this system has been shut down.
     * 
     * @return true if this service has been shut down, or false otherwise
     * @see #shutdown()
     * @see #isPaused()
     * @see #isStarted()
     * @see #isTerminated()
     */
    public boolean isShutdown();

    /**
     * Return whether this system has finished {@link #shutdown() shutting down}. Note that <code>isTerminated</code> is never
     * <code>true</code> unless either {@link #shutdown()} was called first.
     * 
     * @return true if the system has finished shutting down, or false otherwise
     * @see #shutdown()
     * @see #isPaused()
     * @see #isStarted()
     * @see #isShutdown()
     */
    public boolean isTerminated();

}
