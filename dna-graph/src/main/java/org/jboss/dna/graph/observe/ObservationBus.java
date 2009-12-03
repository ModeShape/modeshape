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

import net.jcip.annotations.ThreadSafe;

/**
 * A simple {@link Observer} that is itself {@link Observable}. This class essentially multiplexes the events from a single
 * Observable to disseminate each event to multiple Observers.
 */
@ThreadSafe
public class ObservationBus implements Observable, Observer {
    private final ChangeObservers observers = new ChangeObservers();
    
    private final ObservedId id;

    public ObservationBus() {
        this.id = new ObservedId();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.dna.graph.observe.Observer#getId()
     */
    public ObservedId getId() {
        return this.id;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.observe.Observable#register(org.jboss.dna.graph.observe.Observer)
     */
    public boolean register( Observer observer ) {
        return observers.register(observer);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.observe.Observable#unregister(org.jboss.dna.graph.observe.Observer)
     */
    public boolean unregister( Observer observer ) {
        return observers.unregister(observer);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.observe.Observer#notify(org.jboss.dna.graph.observe.Changes)
     */
    public void notify( Changes changes ) {
        if (changes != null) {
            // Broadcast the changes to the registered observers ...
            observers.broadcast(changes);
        }
    }

    /**
     * Determine whether this particular bus currently has any observers.
     * 
     * @return true if there is at least one observer, or false otherwise
     */
    public boolean hasObservers() {
        return !observers.isEmpty();
    }

    /**
     * Unregister all registered observers, and mark this as no longer accepting new registered observers.
     */
    public void shutdown() {
        observers.shutdown();
    }
}
