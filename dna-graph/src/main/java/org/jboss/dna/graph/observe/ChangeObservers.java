/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.graph.observe;

import java.lang.ref.WeakReference;
import java.util.concurrent.CopyOnWriteArrayList;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.common.util.Logger;

/**
 * Reusable manager of change listeners, typically employed by another {@link Observable} implementation.
 */
@ThreadSafe
public class ChangeObservers implements Observable {

    private CopyOnWriteArrayList<ObserverReference> observers = new CopyOnWriteArrayList<ObserverReference>();

    public ChangeObservers() {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.observe.Observable#register(org.jboss.dna.graph.observe.ChangeObserver)
     */
    public boolean register( ChangeObserver observer ) {
        if (observer != null && observers.addIfAbsent(new ObserverReference(observer))) {
            observer.registeredWith(this);
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.observe.Observable#unregister(org.jboss.dna.graph.observe.ChangeObserver)
     */
    public boolean unregister( ChangeObserver observer ) {
        if (observer != null && observers.remove(observer)) {
            observer.unregisteredWith(this);
            return true;
        }
        return false;
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
            ChangeObserver observer = observerReference.get();
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
    protected final class ObserverReference extends WeakReference<ChangeObserver> {
        final int hc;

        protected ObserverReference( ChangeObserver source ) {
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
                ChangeObserver thisSource = this.get();
                ChangeObserver thatSource = that.get();
                return thisSource == thatSource; // reference equality, not object equality!
            }
            if (obj instanceof ChangeObserver) {
                ChangeObserver that = (ChangeObserver)obj;
                return this.get() == that; // reference equality, not object equality!
            }
            return false;
        }
    }
}
