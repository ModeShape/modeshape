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
import org.modeshape.common.util.Logger;

import java.lang.ref.WeakReference;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Abstract class that is used to signal that a change set has occurred. This class is typically subclassed by those that wish to
 * observe changes in content, and {@link Observable#register(Observer) registered} with a {@link Observable}.
 * <p>
 * This class maintains a (weak) reference to the ChangeSource instances with which it is registered. Therefore, the observers
 * will not keep a ChangeSource from being garbage collected. And, if a change source is garbage collected, calling
 * {@link #unregister()} will clean up naturally.
 * </p>
 */
@ThreadSafe
public abstract class ChangeObserver implements Observer {

    private final CopyOnWriteArraySet<ChangeSourceReference> sources = new CopyOnWriteArraySet<ChangeSourceReference>();

    protected ChangeObserver() {
    }

    /**
     * Records that this listener has successfully registered by the supplied {@link Observable}.
     * 
     * @param source the source with which this listener was registered
     */
    final void registeredWith( Observable source ) {
        sources.add(new ChangeSourceReference(source));
    }

    /**
     * Records that this listener has successfully unregistered by the supplied {@link Observable}.
     * 
     * @param source the source with which this listener was registered
     */
    final void unregisteredWith( Observable source ) {
        sources.remove(new ChangeSourceReference(source));
    }

    /**
     * Unregister this listener from all {@link Observable sources} that it was registered with. This is preferred over calling
     * {@link Observable#unregister(Observer)} directly.
     */
    public void unregister() {
        doUnregister();
    }

    /**
     * Method called by {@link #unregister()} that actually does the unregistering. This method is final.
     */
    protected final void doUnregister() {
        // Unregister this listener from each source ...
        for (ChangeSourceReference sourceReference : sources) {
            Observable source = sourceReference.get();
            if (source != null) {
                try {
                    source.unregister(this);
                } catch (Throwable t) {
                    Logger.getLogger(getClass()).debug(t, "Error while unregistering {0} from {1}", this, source);
                }
            }
        }
    }

    /**
     * Determine whether this observer is currently registered with any {@link Observable} instances.
     * <p>
     * Although an observer might be registered with an {@link Observable}, if that Observable is garbage collected, then this
     * observer will no longer be registered with it.
     * </p>
     * 
     * @return true if this observer is registered with at least one {@link Observable} instance, or false if this observer is not
     *         currently registered with any {@link Observable} instances.
     */
    public boolean isRegistered() {
        for (ChangeSourceReference reference : sources) {
            if (reference.get() != null) return true;
        }
        return false;
    }

    /**
     * Method that is called for each {@link Changes} from the {@link Observable} instance(s) with which this listener is
     * registered.
     * 
     * @param changeSet the change set
     */
    public abstract void notify( Changes changeSet );

    /**
     * A {@link WeakReference} implementation that provides a valid
     */
    protected final class ChangeSourceReference extends WeakReference<Observable> {
        final int hc;

        protected ChangeSourceReference( Observable source ) {
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
            if (obj instanceof ChangeSourceReference) {
                ChangeSourceReference that = (ChangeSourceReference)obj;
                Observable thisSource = this.get();
                Observable thatSource = that.get();
                return thisSource == thatSource; // reference equality, not object equality!
            }
            if (obj instanceof Observable) {
                Observable that = (Observable)obj;
                return this.get() == that; // reference equality, not object equality!
            }
            return false;
        }
    }
}
