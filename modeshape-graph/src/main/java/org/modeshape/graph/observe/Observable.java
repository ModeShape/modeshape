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

import net.jcip.annotations.ThreadSafe;

/**
 * Interface used to register {@link ChangeObserver listeners}. Implementations should use a {@link ChangeObservers} to actually
 * manage the listeners.
 */
@ThreadSafe
public interface Observable {

    /**
     * Register the supplied observer. This method does nothing if the observer reference is null.
     * 
     * @param observer the observer to be added; may be null
     * @return true if the observer was added, or false if the observer was null, if the observer was already registered, or if
     *         the observer could not be added
     */
    boolean register( Observer observer );

    /**
     * Unregister the supplied observer. This method does nothing if the observer reference is null.
     * 
     * @param observer the observer to be removed; may not be null
     * @return true if the observer was removed, or false if the observer was null or if the observer was not registered on this
     *         source
     */
    boolean unregister( Observer observer );

}
