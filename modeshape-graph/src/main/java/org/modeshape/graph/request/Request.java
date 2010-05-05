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
package org.modeshape.graph.request;

import java.io.Serializable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.connector.RepositoryConnection;

/**
 * The abstract base class for all classes representing requests to be executed against a {@link RepositoryConnection}.
 */
public abstract class Request implements Serializable {

    private static final long serialVersionUID = 1L;

    private Throwable error;
    private AtomicBoolean cancelled = new AtomicBoolean(false);
    private final AtomicBoolean frozen = new AtomicBoolean(false);
    private transient CountDownLatch freezingLatch = null;

    protected Request() {
    }

    public void setLatchForFreezing( CountDownLatch latch ) {
        checkNotFrozen();
        this.freezingLatch = latch;
    }

    /**
     * Set the error for this request.
     * 
     * @param error the error to be associated with this request, or null if this request is to have no error
     * @throws IllegalStateException if the request is frozen
     */
    public void setError( Throwable error ) {
        checkNotFrozen();
        this.error = error;
    }

    /**
     * Return whether there is an error associated with this request
     * 
     * @return true if there is an error, or false otherwise
     */
    public boolean hasError() {
        return this.error != null;
    }

    /**
     * Get the error associated with this request, if there is such an error.
     * 
     * @return the error, or null if there is none
     */
    public Throwable getError() {
        return error;
    }

    /**
     * Check whether this request has been cancelled. Although it is a recommendation that the result of this method be followed
     * wherever possible, it is not required to immediately stop processing the request if this method returns <code>true</code>.
     * For example, if processing is almost complete, it may be appropriate to simply finish processing the request.
     * <p>
     * This method is safe to be called by different threads.
     * </p>
     * 
     * @return true if this request has been cancelled, or false otherwise.
     */
    public boolean isCancelled() {
        return cancelled.get();
    }

    /**
     * Set the cancelled state of this request. All requests are initially marked as not cancelled. Note that this is designed so
     * that the same {@link AtomicBoolean} instance can be passed to multiple requests, allowing a single flag to dictate the
     * cancelled state of all of those requests.
     * <p>
     * So, by default, each request should already be set up to not be cancelled, so for most cases this method does not need to
     * be called at all. This method should be called when this flag is to be shared among multiple requests, usually when the
     * requests are being initialized or assembled.
     * </p>
     * 
     * @param cancelled the new (potentially shared) cancelled state for the request; may not be null
     */
    /*package*/void setCancelledFlag( AtomicBoolean cancelled ) {
        assert cancelled != null;
        this.cancelled = cancelled;
    }

    /**
     * Get this request's cancelled flag.
     * 
     * @return the cancelled flag
     */
    /*package*/AtomicBoolean getCancelledFlag() {
        return cancelled;
    }

    /**
     * Cancel this request. After this method is called, the {@link #isCancelled() cancellation flag} is set, and any current or
     * future processing of the request may be affected by the cancellation. (Note however, that processors may choose to not
     * respect this request.)
     * <p>
     * This method is safe to be called by different threads.
     * </p>
     * 
     * @throws IllegalStateException if the request is frozen
     */
    public void cancel() {
        checkNotFrozen();
        this.cancelled.set(true);
    }

    /**
     * Return whether this request only reads information.
     * 
     * @return true if this request reads information, or false if it requests that the repository content be changed in some way
     */
    public abstract boolean isReadOnly();

    /**
     * Determine whether this request has been frozen, preventing any further updates.
     * 
     * @return true if the request has been frozen, or false otherwise
     */
    public boolean isFrozen() {
        return frozen.get();
    }

    /**
     * Freeze this request to prevent any further modification. This method does nothing if the request is already frozen.
     * 
     * @return true if this request was frozen, or false if it was already frozen
     */
    public boolean freeze() {
        if (frozen.compareAndSet(false, true)) {
            // Was not already frozen, so decrement the latch (atomically)
            CountDownLatch latch = this.freezingLatch;
            if (latch != null) latch.countDown();
            return true;
        }
        return false;
    }

    /**
     * Utility method to check that the request is not frozen, and if it is to throw an {@link IllegalStateException}.
     * 
     * @throws IllegalStateException if the request is frozen
     */
    protected void checkNotFrozen() throws IllegalStateException {
        if (frozen.get()) {
            throw new IllegalStateException(GraphI18n.requestIsFrozenAndMayNotBeChanged.text(this));
        }
    }

    /**
     * Returns the type of the request
     * 
     * @return the type of the request
     */
    public abstract RequestType getType();
}
