/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.graph.properties.basic;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.graph.properties.NamespaceRegistry;

/**
 * @author Randall Hauch
 */
@ThreadSafe
public class BasicNamespaceRegistry implements NamespaceRegistry {

    public static final String DEFAULT_NAMESPACE_URI = "";
    public static final String DEFAULT_PREFIX_TEMPLATE = "ns##000";
    public static final String DEFAULT_PREFIX_NUMBER_FORMAT = "##000";

    private final ReadWriteLock registryLock = new ReentrantReadWriteLock();
    private final Map<String, String> namespacesByPrefix = new HashMap<String, String>();
    private final Map<String, String> prefixesByNamespace = new HashMap<String, String>();
    private String generatedPrefixTemplate = DEFAULT_PREFIX_TEMPLATE;
    private int nextGeneratedPrefixNumber = 1;

    /**
     * 
     */
    public BasicNamespaceRegistry() {
        this(DEFAULT_NAMESPACE_URI);
    }

    /**
     * @param defaultNamespaceUri the namespace URI to use for the default prefix
     */
    public BasicNamespaceRegistry( final String defaultNamespaceUri ) {
        register("", defaultNamespaceUri);
    }

    /**
     * @return prefixTemplate
     */
    public String getGeneratedPrefixTemplate() {
        Lock lock = this.registryLock.readLock();
        try {
            lock.lock();
            return this.generatedPrefixTemplate;
        } finally {
            lock.unlock();
        }
    }

    /**
     * @param prefixTemplate Sets prefixTemplate to the specified value.
     */
    public void setGeneratedPrefixTemplate( String prefixTemplate ) {
        if (prefixTemplate == null) prefixTemplate = DEFAULT_PREFIX_TEMPLATE;
        Lock lock = this.registryLock.writeLock();
        try {
            lock.lock();
            this.generatedPrefixTemplate = prefixTemplate;
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getNamespaceForPrefix( String prefix ) {
        ArgCheck.isNotNull(prefix, "prefix");
        Lock lock = this.registryLock.readLock();
        try {
            lock.lock();
            return this.namespacesByPrefix.get(prefix);
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getPrefixForNamespaceUri( String namespaceUri,
                                            boolean generateIfMissing ) {
        ArgCheck.isNotNull(namespaceUri, "namespaceUri");
        String prefix = null;
        Lock lock = this.registryLock.readLock();
        try {
            lock.lock();
            prefix = this.prefixesByNamespace.get(namespaceUri);
        } finally {
            lock.unlock();
        }
        if (prefix == null && generateIfMissing) {
            // Get a write lock ...
            lock = this.registryLock.writeLock();
            try {
                lock.lock();
                // Since we got a new lock, we need to check again ...
                prefix = this.prefixesByNamespace.get(namespaceUri);
                if (prefix == null) {
                    // Now we can genereate a prefix and register it ...
                    prefix = this.generatePrefix();
                    this.register(prefix, namespaceUri);
                }
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
        ArgCheck.isNotNull(namespaceUri, "namespaceUri");
        Lock lock = this.registryLock.readLock();
        try {
            lock.lock();
            return this.prefixesByNamespace.containsKey(namespaceUri);
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
            return this.namespacesByPrefix.get("");
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public String register( String prefix,
                            String namespaceUri ) {
        ArgCheck.isNotNull(namespaceUri, "namespaceUri");
        String previousNamespaceForPrefix = null;
        namespaceUri = namespaceUri.trim();
        Lock lock = this.registryLock.writeLock();
        try {
            lock.lock();
            if (prefix == null) prefix = generatePrefix();
            prefix = prefix.trim();
            previousNamespaceForPrefix = this.namespacesByPrefix.put(prefix, namespaceUri);
            String previousPrefix = this.prefixesByNamespace.put(namespaceUri, prefix);
            if (previousPrefix != null && !previousPrefix.equals(prefix)) {
                this.namespacesByPrefix.remove(previousPrefix);
            }
            if (previousNamespaceForPrefix != null && !previousNamespaceForPrefix.equals(namespaceUri)) {
                this.prefixesByNamespace.remove(previousNamespaceForPrefix);
            }
        } finally {
            lock.unlock();
        }
        return previousNamespaceForPrefix;
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getRegisteredNamespaceUris() {
        Set<String> result = new HashSet<String>();
        Lock lock = this.registryLock.readLock();
        try {
            lock.lock();
            result.addAll(this.prefixesByNamespace.keySet());
        } finally {
            lock.unlock();
        }
        return Collections.unmodifiableSet(result);
    }

    @GuardedBy( "registryLock" )
    protected String generatePrefix() {
        DecimalFormat formatter = new DecimalFormat(this.generatedPrefixTemplate);
        return formatter.format(nextGeneratedPrefixNumber++);
    }

}
