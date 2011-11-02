/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in ModeShape is licensed
 * to you under the terms of the GNU Lesser General Public License as
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
package org.modeshape.jcr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.Logger;
import org.modeshape.jcr.cache.SessionCache;
import org.modeshape.jcr.core.ExecutionContext;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NamespaceRegistry;
import org.modeshape.jcr.value.basic.SimpleNamespaceRegistry;

/**
 * A {@link NamespaceRegistry} implementation that stores the namespaces in the '/jcr:system' area as individual nodes for each
 * namespace.
 */
@ThreadSafe
public class SystemNamespaceRegistry implements NamespaceRegistry {

    public static final Name URI_PROPERTY_NAME = ModeShapeLexicon.URI;
    public static final Name GENERATED_PROPERTY_NAME = ModeShapeLexicon.GENERATED;

    private final JcrRepository.RunningState repository;
    private final SimpleNamespaceRegistry cache;
    private ExecutionContext context;
    private final ReadWriteLock namespacesLock = new ReentrantReadWriteLock();
    private final Logger logger = Logger.getLogger(getClass());

    SystemNamespaceRegistry( JcrRepository.RunningState repository ) {
        this.repository = repository;
        this.cache = new SimpleNamespaceRegistry();
        // Pre-load all of the built-in namespaces ...
        this.cache.register(new ExecutionContext().getNamespaceRegistry().getNamespaces());
    }

    void setContext( ExecutionContext context ) {
        this.context = context;
    }

    /**
     * Refresh the node types from the stored representation.
     * 
     * @return true if there was at least one node type found, or false if there were none
     */
    protected boolean refreshFromSystem() {
        Lock lock = this.namespacesLock.writeLock();
        try {
            lock.lock();
            // Re-read and re-register all of the namespaces ...
            SessionCache systemCache = repository.createSystemSession(context, false);
            SystemContent system = new SystemContent(systemCache);
            Collection<Namespace> namespaces = system.readAllNamespaces();
            if (namespaces.isEmpty()) return false;
            this.cache.clear();
            this.cache.register(namespaces);
        } catch (Throwable e) {
            logger.error(JcrI18n.errorRefreshingNodeTypes, e);
        } finally {
            lock.unlock();
        }
        return true;
    }

    private final SystemContent systemContent( boolean readOnly ) {
        SessionCache systemCache = repository.createSystemSession(context, readOnly);
        return new SystemContent(systemCache);
    }

    @Override
    public String getNamespaceForPrefix( String prefix ) {
        Lock lock = this.namespacesLock.readLock();
        try {
            lock.lock();
            CheckArg.isNotNull(prefix, "prefix");
            return cache.getNamespaceForPrefix(prefix);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String getPrefixForNamespaceUri( String namespaceUri,
                                            boolean generateIfMissing ) {
        CheckArg.isNotNull(namespaceUri, "namespaceUri");
        Lock lock = this.namespacesLock.readLock();
        try {
            lock.lock();
            // Try the cache first ...
            String prefix = cache.getPrefixForNamespaceUri(namespaceUri, false);
            if (prefix == null && generateIfMissing) {
                SystemContent systemContent = systemContent(!generateIfMissing);
                prefix = systemContent.readNamespacePrefix(namespaceUri, generateIfMissing);
                if (prefix != null) {
                    systemContent.save();
                    cache.register(prefix, namespaceUri);
                }
            }
            return prefix;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isRegisteredNamespaceUri( String namespaceUri ) {
        CheckArg.isNotNull(namespaceUri, "namespaceUri");
        Lock lock = this.namespacesLock.readLock();
        try {
            lock.lock();
            return cache.isRegisteredNamespaceUri(namespaceUri);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String getDefaultNamespaceUri() {
        return this.getNamespaceForPrefix("");
    }

    @Override
    public void register( Iterable<Namespace> namespaces ) {
        final Lock lock = this.namespacesLock.writeLock();
        try {
            lock.lock();
            Map<String, String> urisByPrefix = new HashMap<String, String>();
            for (Namespace namespace : namespaces) {
                urisByPrefix.put(namespace.getPrefix(), namespace.getNamespaceUri());
            }
            register(urisByPrefix);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Register a set of namespaces.
     * 
     * @param namespaceUrisByPrefix the map of new namespace URIs by their prefix
     */
    public void register( Map<String, String> namespaceUrisByPrefix ) {
        if (namespaceUrisByPrefix == null || namespaceUrisByPrefix.isEmpty()) return;
        final Lock lock = this.namespacesLock.writeLock();
        try {
            lock.lock();
            SystemContent systemContent = systemContent(false);
            systemContent.registerNamespaces(namespaceUrisByPrefix);
            systemContent.save();
            for (Map.Entry<String, String> entry : namespaceUrisByPrefix.entrySet()) {
                String prefix = entry.getKey().trim();
                String uri = entry.getValue().trim();
                if (prefix.length() == 0) continue;
                this.cache.register(prefix, uri);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String register( String prefix,
                            String namespaceUri ) {
        CheckArg.isNotNull(namespaceUri, "namespaceUri");
        namespaceUri = namespaceUri.trim();
        final Lock lock = this.namespacesLock.writeLock();
        try {
            lock.lock();
            // Register it in the cache first ...
            String previousCachedUriForPrefix = this.cache.register(prefix, namespaceUri);
            if (!namespaceUri.equals(previousCachedUriForPrefix)) {
                // And register it in the source ...
                SystemContent systemContent = systemContent(false);
                systemContent.registerNamespaces(Collections.singletonMap(prefix, namespaceUri));
                systemContent.save();
            }
            return previousCachedUriForPrefix;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean unregister( String namespaceUri ) {
        CheckArg.isNotNull(namespaceUri, "namespaceUri");
        namespaceUri = namespaceUri.trim();
        final Lock lock = this.namespacesLock.writeLock();
        try {
            lock.lock();
            // Remove it from the cache ...
            boolean found = this.cache.unregister(namespaceUri);
            // Then from the source ...
            SystemContent systemContent = systemContent(false);
            boolean foundPersistent = systemContent.unregisterNamespace(namespaceUri);
            systemContent.save();
            return foundPersistent || found;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Set<String> getRegisteredNamespaceUris() {
        final Lock lock = this.namespacesLock.readLock();
        try {
            lock.lock();
            // Just return what's in the cache ...
            return cache.getRegisteredNamespaceUris();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Set<Namespace> getNamespaces() {
        final Lock lock = this.namespacesLock.readLock();
        try {
            lock.lock();
            // Just return what's in the cache ...
            return cache.getNamespaces();
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
