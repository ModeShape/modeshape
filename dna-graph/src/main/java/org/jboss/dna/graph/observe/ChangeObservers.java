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

import java.util.concurrent.CopyOnWriteArrayList;
import net.jcip.annotations.ThreadSafe;

/**
 * Reusable manager of change listeners, typically employed by another {@link Observable} implementation.
 */
@ThreadSafe
public class ChangeObservers implements Observable {

    private CopyOnWriteArrayList<ChangeObserver> observers = new CopyOnWriteArrayList<ChangeObserver>();

    public ChangeObservers() {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.observe.Observable#register(org.jboss.dna.graph.observe.ChangeObserver)
     */
    public boolean register( ChangeObserver observer ) {
        if (observer != null && observers.addIfAbsent(observer)) {
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

}
