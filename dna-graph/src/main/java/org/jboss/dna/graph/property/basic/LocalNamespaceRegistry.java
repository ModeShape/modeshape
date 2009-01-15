/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.dna.graph.property.basic;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.property.NamespaceRegistry;

/**
 * @author Randall Hauch
 */
@ThreadSafe
public class LocalNamespaceRegistry extends BasicNamespaceRegistry {

    private final NamespaceRegistry delegate;

    /**
     * @param delegate the namespace registry that this registry should delegate to if not found locally
     */
    public LocalNamespaceRegistry( NamespaceRegistry delegate ) {
        super();
        CheckArg.isNotNull(delegate, "delegate");
        this.delegate = delegate;
        unregister(DEFAULT_NAMESPACE_URI);
    }

    /**
     * @param delegate the namespace registry that this registry should delegate to if not found locally
     * @param defaultNamespaceUri the namespace URI to use for the default prefix
     */
    public LocalNamespaceRegistry( NamespaceRegistry delegate,
                                   final String defaultNamespaceUri ) {
        super();
        CheckArg.isNotNull(delegate, "delegate");
        this.delegate = delegate;
        register("", defaultNamespaceUri);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.basic.BasicNamespaceRegistry#getDefaultNamespaceUri()
     */
    @Override
    public String getDefaultNamespaceUri() {
        String result = super.getDefaultNamespaceUri();
        if (result == null) result = this.delegate.getDefaultNamespaceUri();
        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.basic.BasicNamespaceRegistry#getNamespaceForPrefix(java.lang.String)
     */
    @Override
    public String getNamespaceForPrefix( String prefix ) {
        String result = super.getNamespaceForPrefix(prefix);
        if (result == null) result = this.delegate.getNamespaceForPrefix(prefix);
        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.basic.BasicNamespaceRegistry#getNamespaces()
     */
    @Override
    public Set<Namespace> getNamespaces() {
        Set<Namespace> delegateNamespaces = this.delegate.getNamespaces();
        Set<Namespace> localNamespaces = super.getNamespaces();

        // Load the local namespaces first ...
        Set<Namespace> namespaces = new HashSet<Namespace>(localNamespaces);

        // Now build a map of the local prefixes so we can check for prefixes
        Map<String, Namespace> localNamespacesByPrefix = new HashMap<String, Namespace>();
        for (Namespace ns : localNamespaces)
            localNamespacesByPrefix.put(ns.getPrefix(), ns);

        // Now iterate over the local namespaces, removing any existing namespace with the same prefix
        for (Namespace ns : delegateNamespaces) {
            if (localNamespacesByPrefix.get(ns.getPrefix()) != null) continue;
            // Try to add the delegate namespace, which won't work if a local with the same URI was already added...
            namespaces.add(ns);
        }
        return Collections.unmodifiableSet(namespaces);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.basic.BasicNamespaceRegistry#getPrefixForNamespaceUri(java.lang.String, boolean)
     */
    @Override
    public String getPrefixForNamespaceUri( String namespaceUri,
                                            boolean generateIfMissing ) {
        String result = super.getPrefixForNamespaceUri(namespaceUri, false);
        if (result == null) result = this.delegate.getPrefixForNamespaceUri(namespaceUri, false);
        if (result == null && generateIfMissing) result = super.getPrefixForNamespaceUri(namespaceUri, true);
        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.basic.BasicNamespaceRegistry#getRegisteredNamespaceUris()
     */
    @Override
    public Set<String> getRegisteredNamespaceUris() {
        Set<String> uris = new HashSet<String>(this.delegate.getRegisteredNamespaceUris());
        uris.addAll(super.getRegisteredNamespaceUris());
        return Collections.unmodifiableSet(uris);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.basic.BasicNamespaceRegistry#isRegisteredNamespaceUri(java.lang.String)
     */
    @Override
    public boolean isRegisteredNamespaceUri( String namespaceUri ) {
        return super.isRegisteredNamespaceUri(namespaceUri) || this.delegate.isRegisteredNamespaceUri(namespaceUri);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.basic.BasicNamespaceRegistry#register(java.lang.String, java.lang.String)
     */
    @Override
    public String register( String prefix,
                            String namespaceUri ) {
        // Just register the namespace locally ...
        String previous = super.register(prefix, namespaceUri);
        // But check whether there is a "previous" from the delegate ...
        if (previous == null && delegate != null) previous = delegate.getPrefixForNamespaceUri(namespaceUri, false);
        return previous;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.basic.BasicNamespaceRegistry#unregister(java.lang.String)
     */
    @Override
    public boolean unregister( String namespaceUri ) {
        // Unregister locally ...
        return super.unregister(namespaceUri);
    }

}
