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

import java.util.Locale;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.util.Logger;
import org.modeshape.repository.RepositoryI18n;

/**
 * Simple abstract implementation of the service administrator interface that can be easily subclassed by services that require an
 * administrative interface.
 */
@ThreadSafe
public abstract class AbstractServiceAdministrator implements ServiceAdministrator {

    private volatile State state;
    private final I18n serviceName;
    private final Logger logger;

    protected AbstractServiceAdministrator( I18n serviceName,
                                            State initialState ) {
        assert initialState != null;
        assert serviceName != null;
        this.state = initialState;
        this.serviceName = serviceName;
        this.logger = Logger.getLogger(getClass());
    }

    /**
     * Return the current state of this service.
     * 
     * @return the current state
     */
    public State getState() {
        return this.state;
    }

    /**
     * Set the state of the service. This method does nothing if the desired state matches the current state.
     * 
     * @param state the desired state
     * @return this object for method chaining purposes
     * @see #setState(String)
     * @see #start()
     * @see #pause()
     * @see #shutdown()
     */
    @GuardedBy( "this" )
    public synchronized ServiceAdministrator setState( State state ) {
        switch (state) {
            case STARTED:
                start();
                break;
            case PAUSED:
                pause();
                break;
            case SHUTDOWN:
            case TERMINATED:
                shutdown();
                break;
        }
        return this;
    }

    /**
     * Set the state of the service. This method does nothing if the desired state matches the current state.
     * 
     * @param state the desired state in string form
     * @return this object for method chaining purposes
     * @throws IllegalArgumentException if the specified state string is null or does not match one of the predefined
     *         {@link ServiceAdministrator.State predefined enumerated values}
     * @see #setState(State)
     * @see #start()
     * @see #pause()
     * @see #shutdown()
     */
    public ServiceAdministrator setState( String state ) {
        State newState = state == null ? null : State.valueOf(state.toUpperCase());
        if (newState == null) {
            throw new IllegalArgumentException(RepositoryI18n.invalidStateString.text(state));
        }
        return setState(newState);
    }

    /**
     * Start monitoring and sequence the events. This method can be called multiple times, including after the service is
     * {@link #pause() paused}. However, once the service is {@link #shutdown() shutdown}, it cannot be started or paused.
     * 
     * @return this object for method chaining purposes
     * @throws IllegalStateException if called when the service has been {@link #shutdown() shutdown}.
     * @see #pause()
     * @see #shutdown()
     * @see #isStarted()
     */
    public synchronized ServiceAdministrator start() {
        switch (this.state) {
            case STARTED:
                break;
            case PAUSED:
                logger.trace("Starting \"{0}\"", getServiceName());
                doStart(this.state);
                this.state = State.STARTED;
                logger.trace("Started \"{0}\"", getServiceName());
                break;
            case SHUTDOWN:
            case TERMINATED:
                throw new IllegalStateException(RepositoryI18n.serviceShutdowAndMayNotBeStarted.text(getServiceName()));
        }
        return this;
    }

    /**
     * Implementation of the functionality to switch to the started state. This method is only called if the state from which the
     * service is transitioning is appropriate ({@link ServiceAdministrator.State#PAUSED}). This method does nothing by default,
     * and should be overridden if needed.
     * 
     * @param fromState the state from which this service is transitioning; never null
     * @throws IllegalStateException if the service is such that it cannot be transitioned from the supplied state
     */
    @GuardedBy( "this" )
    protected void doStart( State fromState ) {
    }

    /**
     * Temporarily stop monitoring and sequencing events. This method can be called multiple times, including after the service is
     * {@link #start() started}. However, once the service is {@link #shutdown() shutdown}, it cannot be started or paused.
     * 
     * @return this object for method chaining purposes
     * @throws IllegalStateException if called when the service has been {@link #shutdown() shutdown}.
     * @see #start()
     * @see #shutdown()
     * @see #isPaused()
     */
    public synchronized ServiceAdministrator pause() {
        switch (this.state) {
            case STARTED:
                logger.trace("Pausing \"{0}\"", getServiceName());
                doPause(this.state);
                this.state = State.PAUSED;
                logger.trace("Paused \"{0}\"", getServiceName());
                break;
            case PAUSED:
                break;
            case SHUTDOWN:
            case TERMINATED:
                throw new IllegalStateException(RepositoryI18n.serviceShutdowAndMayNotBePaused.text(getServiceName()));
        }
        return this;
    }

    /**
     * Implementation of the functionality to switch to the paused state. This method is only called if the state from which the
     * service is transitioning is appropriate ({@link ServiceAdministrator.State#STARTED}). This method does nothing by default,
     * and should be overridden if needed.
     * 
     * @param fromState the state from which this service is transitioning; never null
     * @throws IllegalStateException if the service is such that it cannot be transitioned from the supplied state
     */
    @GuardedBy( "this" )
    protected void doPause( State fromState ) {
    }

    /**
     * Permanently stop monitoring and sequencing events. This method can be called multiple times, but only the first call has an
     * effect. Once the service has been shutdown, it may not be {@link #start() restarted} or {@link #pause() paused}.
     * 
     * @return this object for method chaining purposes
     * @see #start()
     * @see #pause()
     * @see #isShutdown()
     */
    public synchronized ServiceAdministrator shutdown() {
        switch (this.state) {
            case STARTED:
            case PAUSED:
                logger.trace("Initiating shutdown of \"{0}\"", getServiceName());
                this.state = State.SHUTDOWN;
                doShutdown(this.state);
                logger.trace("Initiated shutdown of \"{0}\"", getServiceName());
                isTerminated();
                break;
            case SHUTDOWN:
            case TERMINATED:
                isTerminated();
                break;
        }
        return this;
    }

    /**
     * Implementation of the functionality to switch to the shutdown state. This method is only called if the state from which the
     * service is transitioning is appropriate ({@link ServiceAdministrator.State#STARTED} or
     * {@link ServiceAdministrator.State#PAUSED}). This method does nothing by default, and should be overridden if needed.
     * 
     * @param fromState the state from which this service is transitioning; never null
     * @throws IllegalStateException if the service is such that it cannot be transitioned from the supplied state
     */
    @GuardedBy( "this" )
    protected void doShutdown( State fromState ) {
    }

    /**
     * Return whether this service has been started and is currently running.
     * 
     * @return true if started and currently running, or false otherwise
     * @see #start()
     * @see #pause()
     * @see #isPaused()
     * @see #isShutdown()
     */
    public boolean isStarted() {
        return this.state == State.STARTED;
    }

    /**
     * Return whether this service is currently paused.
     * 
     * @return true if currently paused, or false otherwise
     * @see #pause()
     * @see #start()
     * @see #isStarted()
     * @see #isShutdown()
     */
    public boolean isPaused() {
        return this.state == State.PAUSED;
    }

    /**
     * Return whether this service is stopped and unable to be restarted.
     * 
     * @return true if currently shutdown, or false otherwise
     * @see #shutdown()
     * @see #isPaused()
     * @see #isStarted()
     */
    public boolean isShutdown() {
        return this.state == State.SHUTDOWN || this.state == State.TERMINATED;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isTerminated() {
        switch (this.state) {
            case PAUSED:
            case STARTED:
            case SHUTDOWN:
                if (doCheckIsTerminated()) {
                    this.state = State.TERMINATED;
                    logger.trace("Service \"{0}\" has terminated", getServiceName());
                    return true;
                }
                return false;
            case TERMINATED:
                return true;
        }
        return false;
    }

    /**
     * Subclasses should implement this method to determine whether the service has completed shutdown.
     * 
     * @return true if terminated, or false otherwise
     */
    protected abstract boolean doCheckIsTerminated();

    /**
     * Get the name of this service in the current locale.
     * 
     * @return the service name
     */
    public String getServiceName() {
        return this.serviceName.text();
    }

    /**
     * Get the name of this service in the specified locale.
     * 
     * @param locale the locale in which the service name is to be returned; may be null if the default locale is to be used
     * @return the service name
     */
    public String getServiceName( Locale locale ) {
        return this.serviceName.text(locale);
    }

}
