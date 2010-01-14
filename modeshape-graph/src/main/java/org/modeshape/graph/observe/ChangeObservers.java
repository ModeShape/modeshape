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
package org.modeshape.graph.observe;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import net.jcip.annotations.ThreadSafe;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.Logger;

/**
 * Reusable manager of change listeners, typically employed by another {@link Observable} implementation.
 */
@ThreadSafe
public class ChangeObservers implements Observable {

    private final CopyOnWriteArrayList<ObserverReference> observers = new CopyOnWriteArrayList<ObserverReference>();
    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    public ChangeObservers() {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.observe.Observable#register(org.modeshape.graph.observe.Observer)
     */
    public boolean register( Observer observer ) {
        if (observer != null && !shutdown.get() && observers.addIfAbsent(new ObserverReference(observer))) {
            if (observer instanceof ChangeObserver) ((ChangeObserver)observer).registeredWith(this);
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.observe.Observable#unregister(org.modeshape.graph.observe.Observer)
     */
    public boolean unregister( Observer observer ) {
        if (observer != null && observers.remove(observer)) {
            if (observer instanceof ChangeObserver) ((ChangeObserver)observer).unregisteredWith(this);
            return true;
        }
        return false;
    }

    /**
     * Unregister all registered observers, and mark this as no longer accepting new registered observers.
     */
    public void shutdown() {
        shutdown.set(true);
        while (!observers.isEmpty()) {
            Iterator<ObserverReference> iter = observers.iterator(); // gets snapshot
            observers.clear();
            while (iter.hasNext()) {
                ObserverReference reference = iter.next();
                if (reference.get() != null) {
                    Observer observer = reference.get();
                    if (observer instanceof ChangeObserver) ((ChangeObserver)observer).unregisteredWith(this);
                }
            }
        }
    }

    /**
     * Determine whether there are any observers at the time this method is called.
     * 
     * @return true if there are currently no observers, or false if there is at least one observer
     */
    public boolean isEmpty() {
        return observers.isEmpty();
    }

    /**
     * Broadcast the supplied changes to the registered observers.
     * 
     * @param changes the changes to broadcast
     * @throws IllegalArgumentException if the changes reference is null
     */
    public void broadcast( Changes changes ) {
        CheckArg.isNotNull(changes, "changes");
        for (ObserverReference observerReference : observers) {
            Observer observer = observerReference.get();
            if (observer == null) {
                observers.remove(observerReference);
                continue;
            }
            try {
                observer.notify(changes);
            } catch (Throwable t) {
                Logger.getLogger(getClass()).debug(t, "Exception while notifying");
            }
        }
    }

    /**
     * A {@link WeakReference} implementation that provides a valid
     */
    protected final class ObserverReference extends WeakReference<Observer> {
        final int hc;

        protected ObserverReference( Observer source ) {
            super(source);
            this.hc = source.hashCode();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            return hc;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof ObserverReference) {
                ObserverReference that = (ObserverReference)obj;
                Observer thisSource = this.get();
                Observer thatSource = that.get();
                return thisSource == thatSource; // reference equality, not object equality!
            }
            if (obj instanceof Observer) {
                Observer that = (Observer)obj;
                return this.get() == that; // reference equality, not object equality!
            }
            return false;
        }
    }
}
