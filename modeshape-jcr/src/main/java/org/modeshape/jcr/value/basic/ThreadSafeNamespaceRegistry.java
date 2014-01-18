/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.jcr.value.basic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.value.NamespaceRegistry;

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

    @Override
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

    @Override
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

    @Override
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

    @Override
    public String getDefaultNamespaceUri() {
        Lock lock = this.registryLock.readLock();
        try {
            lock.lock();
            return delegate.getDefaultNamespaceUri();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void register( Iterable<Namespace> namespaces ) {
        CheckArg.isNotNull(namespaces, "namespaces");
        Lock lock = this.registryLock.writeLock();
        try {
            lock.lock();
            for (Namespace namespace : namespaces) {
                register(namespace.getPrefix(), namespace.getNamespaceUri());
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
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

    @Override
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

    @Override
    public Set<String> getRegisteredNamespaceUris() {
        Lock lock = this.registryLock.readLock();
        try {
            lock.lock();
            return delegate.getRegisteredNamespaceUris();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Set<Namespace> getNamespaces() {
        Lock lock = this.registryLock.readLock();
        try {
            lock.lock();
            return delegate.getNamespaces();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String toString() {
        List<Namespace> namespaces = new ArrayList<Namespace>(getNamespaces());
        Collections.sort(namespaces);
        return namespaces.toString();
    }

}
