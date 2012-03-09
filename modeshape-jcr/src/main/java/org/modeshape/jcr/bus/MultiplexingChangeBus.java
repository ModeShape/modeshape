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

package org.modeshape.jcr.bus;

import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.jcr.cache.change.ChangeSet;
import org.modeshape.jcr.cache.change.ChangeSetListener;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@ThreadSafe
public class MultiplexingChangeBus implements ChangeBus {

    private final Set<ChangeSetListener> delegates = new CopyOnWriteArraySet<ChangeSetListener>();

    public MultiplexingChangeBus() {
    }

    @Override
    public boolean register( ChangeSetListener listener ) {
        return listener != null ? delegates.add(listener) : false;
    }

    @Override
    public boolean unregister( ChangeSetListener listener ) {
        return listener != null ? delegates.remove(listener) : false;
    }

    @Override
    public void notify( ChangeSet changeSet ) {
        for (ChangeSetListener listener : delegates) {
            listener.notify(changeSet);
        }
    }

    @Override
    public boolean hasObservers() {
        return delegates.isEmpty();
    }

    @Override
    public void start() {
    }

    @Override
    public void shutdown() {
    }
}
