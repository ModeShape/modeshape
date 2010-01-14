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
package org.modeshape.graph.property.basic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.jcip.annotations.ThreadSafe;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.property.NamespaceRegistry;

/**
 * A thread-safe {@link NamespaceRegistry} that may be used as a thread-safe wrapper around another non-thread-safe
 * implementation.
 */
@ThreadSafe
public class ThreadSafeNamespaceRegistry implements NamespaceRegistry {

    private final ReadWriteLock registryLock = new ReentrantReadWriteLock();
    private final NamespaceRegistry delegate;

    /**
     */
    public ThreadSafeNamespaceRegistry() {
        this(new SimpleNamespaceRegistry());
    }

    /**
     * @param nonThreadSafeRegistry a {@link NamespaceRegistry} implementation that is not thread safe and to which this instance
     *        will delegate; may not be null
     */
    public ThreadSafeNamespaceRegistry( NamespaceRegistry nonThreadSafeRegistry ) {
        CheckArg.isNotNull(nonThreadSafeRegistry, "nonThreadSafeRegistry");
        delegate = nonThreadSafeRegistry;
    }

    /**
     * {@inheritDoc}
     */
    public String getNamespaceForPrefix( String prefix ) {
        CheckArg.isNotNull(prefix, "prefix");
        Lock lock = this.registryLock.readLock();
        try {
            lock.lock();
            return this.delegate.getNamespaceForPrefix(prefix);
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getPrefixForNamespaceUri( String namespaceUri,
                                            boolean generateIfMissing ) {
        CheckArg.isNotNull(namespaceUri, "namespaceUri");
        String prefix = null;
        Lock lock = this.registryLock.readLock();
        try {
            lock.lock();
            prefix = delegate.getPrefixForNamespaceUri(namespaceUri, false);
        } finally {
            lock.unlock();
        }
        if (prefix == null && generateIfMissing) {
            // Get a write lock ...
            lock = this.registryLock.writeLock();
            try {
                lock.lock();
                prefix = delegate.getPrefixForNamespaceUri(namespaceUri, true);
                return prefix;
            } finally {
                lock.unlock();
            }
        }
        return prefix;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isRegisteredNamespaceUri( String namespaceUri ) {
        CheckArg.isNotNull(namespaceUri, "namespaceUri");
        Lock lock = this.registryLock.readLock();
        try {
            lock.lock();
            return delegate.isRegisteredNamespaceUri(namespaceUri);
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getDefaultNamespaceUri() {
        Lock lock = this.registryLock.readLock();
        try {
            lock.lock();
            return delegate.getDefaultNamespaceUri();
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public String register( String prefix,
                            String namespaceUri ) {
        CheckArg.isNotNull(namespaceUri, "namespaceUri");
        Lock lock = this.registryLock.writeLock();
        try {
            lock.lock();
            return delegate.register(prefix, namespaceUri);
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.NamespaceRegistry#unregister(java.lang.String)
     */
    public boolean unregister( String namespaceUri ) {
        CheckArg.isNotNull(namespaceUri, "namespaceUri");
        Lock lock = this.registryLock.writeLock();
        try {
            lock.lock();
            return delegate.unregister(namespaceUri);
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getRegisteredNamespaceUris() {
        Lock lock = this.registryLock.readLock();
        try {
            lock.lock();
            return delegate.getRegisteredNamespaceUris();
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.NamespaceRegistry#getNamespaces()
     */
    public Set<Namespace> getNamespaces() {
        Lock lock = this.registryLock.readLock();
        try {
            lock.lock();
            return delegate.getNamespaces();
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        List<Namespace> namespaces = new ArrayList<Namespace>(getNamespaces());
        Collections.sort(namespaces);
        return namespaces.toString();
    }

}
