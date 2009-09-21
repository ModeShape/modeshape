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
package org.jboss.dna.search;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.common.text.NoOpEncoder;
import org.jboss.dna.common.text.TextEncoder;
import org.jboss.dna.graph.DnaLexicon;
import org.jboss.dna.graph.JcrLexicon;
import org.jboss.dna.graph.JcrMixLexicon;
import org.jboss.dna.graph.JcrNtLexicon;
import org.jboss.dna.graph.property.NamespaceRegistry;
import org.jboss.dna.graph.property.basic.BasicNamespace;

/**
 * A {@link NamespaceRegistry} implementation that uses encoded representations of the namespace URIs for the namespace prefixes.
 */
@ThreadSafe
class EncodingNamespaceRegistry implements NamespaceRegistry {

    public static final Set<String> DEFAULT_FIXED_NAMESPACES = Collections.unmodifiableSet(new HashSet<String>(
                                                                                                               Arrays.asList(new String[] {
                                                                                                                   "",
                                                                                                                   DnaLexicon.Namespace.URI,
                                                                                                                   JcrLexicon.Namespace.URI,
                                                                                                                   JcrNtLexicon.Namespace.URI,
                                                                                                                   JcrMixLexicon.Namespace.URI})));

    private final NamespaceRegistry registry;
    private final TextEncoder encoder;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    @GuardedBy( "lock" )
    private final Map<String, String> uriToEncodedPrefix = new HashMap<String, String>();
    @GuardedBy( "lock" )
    private final Map<String, String> encodedPrefixToUri = new HashMap<String, String>();
    private final Set<String> fixedNamespaceUris;

    /**
     * @param registry the original registry
     * @param encoder the encoder; may be null if no encoding should be used
     */
    EncodingNamespaceRegistry( NamespaceRegistry registry,
                               TextEncoder encoder ) {
        this(registry, encoder, null);
    }

    /**
     * @param registry the original registry
     * @param encoder the encoder; may be null if no encoding should be used
     * @param fixedUris the set of URIs that is to be fixed and not encoded; or null if the default namespaces are to be fixed
     */
    EncodingNamespaceRegistry( NamespaceRegistry registry,
                               TextEncoder encoder,
                               Set<String> fixedUris ) {
        this.registry = registry;
        this.encoder = encoder != null ? encoder : new NoOpEncoder();
        this.fixedNamespaceUris = fixedUris != null ? Collections.unmodifiableSet(new HashSet<String>(fixedUris)) : DEFAULT_FIXED_NAMESPACES;
        assert this.registry != null;
        assert this.encoder != null;
        assert this.fixedNamespaceUris != null;
    }

    /**
     * @return fixedNamespaceUris
     */
    public Set<String> getFixedNamespaceUris() {
        return fixedNamespaceUris;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.NamespaceRegistry#getDefaultNamespaceUri()
     */
    public String getDefaultNamespaceUri() {
        return this.registry.getDefaultNamespaceUri();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.NamespaceRegistry#getNamespaceForPrefix(java.lang.String)
     */
    public String getNamespaceForPrefix( String prefix ) {
        // First look in the map ...
        String result = null;
        try {
            lock.readLock().lock();
            result = encodedPrefixToUri.get(prefix);
            if (result != null) return result;
        } finally {
            lock.readLock().unlock();
        }

        // Make sure we have encoded all the namespaces in the registry ...
        Set<Namespace> namespaces = new HashSet<Namespace>(this.registry.getNamespaces());
        Set<Namespace> encodedNamespaces = this.getNamespaces();
        namespaces.removeAll(encodedNamespaces);
        try {
            lock.writeLock().lock();
            for (Namespace namespace : namespaces) {
                String namespaceUri = namespace.getNamespaceUri();
                String encoded = fixedNamespaceUris.contains(namespaceUri) ? namespace.getPrefix() : encoder.encode(namespaceUri);
                uriToEncodedPrefix.put(namespaceUri, encoded);
                encodedPrefixToUri.put(encoded, namespaceUri);
                if (result == null && encoded.equals(prefix)) result = namespaceUri;
            }
        } finally {
            lock.writeLock().unlock();
        }
        if (result != null) return result;

        // There's nothing, so just delegate to the registry ...
        return this.registry.getNamespaceForPrefix(prefix);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.NamespaceRegistry#getRegisteredNamespaceUris()
     */
    public Set<String> getRegisteredNamespaceUris() {
        return this.registry.getRegisteredNamespaceUris();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.NamespaceRegistry#isRegisteredNamespaceUri(java.lang.String)
     */
    public boolean isRegisteredNamespaceUri( String namespaceUri ) {
        return this.registry.isRegisteredNamespaceUri(namespaceUri);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.NamespaceRegistry#getPrefixForNamespaceUri(java.lang.String, boolean)
     */
    public String getPrefixForNamespaceUri( String namespaceUri,
                                            boolean generateIfMissing ) {
        if (fixedNamespaceUris.contains(namespaceUri)) {
            return this.registry.getPrefixForNamespaceUri(namespaceUri, generateIfMissing);
        }
        String encoded = null;
        try {
            lock.readLock().lock();
            encoded = uriToEncodedPrefix.get(namespaceUri);
        } finally {
            lock.readLock().unlock();
        }
        if (encoded == null) {
            encoded = encoder.encode(namespaceUri);
            try {
                lock.writeLock().lock();
                uriToEncodedPrefix.put(namespaceUri, encoded);
                encodedPrefixToUri.put(encoded, namespaceUri);
            } finally {
                lock.writeLock().unlock();
            }
        }
        return encoded;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.NamespaceRegistry#getNamespaces()
     */
    public Set<Namespace> getNamespaces() {
        Set<Namespace> results = new HashSet<Namespace>();
        try {
            lock.readLock().lock();
            for (Map.Entry<String, String> entry : uriToEncodedPrefix.entrySet()) {
                String uri = entry.getKey();
                String prefix = entry.getValue();
                results.add(new BasicNamespace(prefix, uri));
            }
        } finally {
            lock.readLock().unlock();
        }
        return results;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.NamespaceRegistry#register(java.lang.String, java.lang.String)
     */
    public String register( String prefix,
                            String namespaceUri ) {
        return this.registry.register(prefix, namespaceUri);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.NamespaceRegistry#unregister(java.lang.String)
     */
    public boolean unregister( String namespaceUri ) {
        return this.registry.unregister(namespaceUri);
    }
}
