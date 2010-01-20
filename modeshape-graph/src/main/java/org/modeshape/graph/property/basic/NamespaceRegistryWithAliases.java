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
package org.modeshape.graph.property.basic;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.property.NamespaceRegistry;

/**
 * 
 */
public class NamespaceRegistryWithAliases implements NamespaceRegistry {

    private final NamespaceRegistry delegate;
    private final Map<String, String> aliaseNamespaceUriByPrefix;
    private final Map<String, String> namespaceUrisByAlias;

    /**
     * Construct a new registry around another delegate registry with a set of aliases for existing namespaces in the delegate
     * registry. It is possible to supply multiple aliases for a single existing namespace, simply by registering each alias in
     * <code>aliases</code> and mapping each alias to its existing "real" namespace in <code>namespaceUrisByAliasUri</code>
     * 
     * @param delegate the actual delegate registry containing the existing (real) namespaces
     * @param aliaseNamespaceUriByPrefix the map of alias namespaces keyed by their prefixes
     * @param namespaceUrisByAliasUri the map of existing namespace URIs keyed by the aliases
     * @throws IllegalArgumentException if any parameters are null, if there are no aliases, or if the namespaces in the
     *         <code>aliases</code> registry do not correspond exactly to the mappings in <code>namespaceUrisByAliasUri</code>
     */
    public NamespaceRegistryWithAliases( NamespaceRegistry delegate,
                                         Map<String, String> aliaseNamespaceUriByPrefix,
                                         Map<String, String> namespaceUrisByAliasUri ) {
        CheckArg.isNotNull(delegate, "delegate");
        CheckArg.isNotEmpty(aliaseNamespaceUriByPrefix, "aliases");
        CheckArg.isNotEmpty(namespaceUrisByAliasUri, "namespaceUrisByAlias");
        this.delegate = delegate;
        this.aliaseNamespaceUriByPrefix = aliaseNamespaceUriByPrefix;
        // Make a copy of the namespaces ...
        this.namespaceUrisByAlias = Collections.unmodifiableMap(new HashMap<String, String>(namespaceUrisByAliasUri));
        // Check that the alias map contains exactly the same namespaces as the aliases ...
        Map<String, String> copyOfNamespaceUrisByAlias = new HashMap<String, String>(namespaceUrisByAlias);
        for (String aliasedNamespaceUri : this.aliaseNamespaceUriByPrefix.values()) {
            if (copyOfNamespaceUrisByAlias.remove(aliasedNamespaceUri) == null) {
                I18n msg = GraphI18n.namespaceAliasWasNotMappedToRealNamespace;
                throw new IllegalArgumentException(msg.text(aliasedNamespaceUri));
            }
        }
        if (!copyOfNamespaceUrisByAlias.isEmpty()) {
            I18n msg = GraphI18n.aliasesMappedToRealNamespacesButWereNotRegisteredInAliasNamespace;
            throw new IllegalArgumentException(msg.text(copyOfNamespaceUrisByAlias));
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.NamespaceRegistry#getDefaultNamespaceUri()
     */
    public String getDefaultNamespaceUri() {
        // Just delegate ...
        return delegate.getDefaultNamespaceUri();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.NamespaceRegistry#getNamespaceForPrefix(java.lang.String)
     */
    public String getNamespaceForPrefix( String prefix ) {
        String uri = delegate.getNamespaceForPrefix(prefix);
        if (uri == null) {
            // There was no URI registered with that prefix, so check the aliases ...
            uri = aliaseNamespaceUriByPrefix.get(prefix);
            if (uri != null) {
                // The prefix is for an aliased namespace, so use the original namespace URI instead ...
                String originalUri = namespaceUrisByAlias.get(uri);
                assert originalUri != null;
                uri = originalUri;
            }
        } else {
            // The supplied prefix matched a namespace, but we need to see if the namespace is really an alias
            // (this can happen if the user re-registers an aliased namespace under a different prefix) ...
            String originalUri = namespaceUrisByAlias.get(uri);
            if (originalUri != null) {
                // The delegate's prefix matched an aliased namespace URI, so return the original URI ...
                uri = originalUri;
            }
        }
        return uri;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.NamespaceRegistry#getNamespaces()
     */
    public Set<Namespace> getNamespaces() {
        // Do not include the aliases in the namespaces ...
        return delegate.getNamespaces();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.NamespaceRegistry#getPrefixForNamespaceUri(java.lang.String, boolean)
     */
    public String getPrefixForNamespaceUri( String namespaceUri,
                                            boolean generateIfMissing ) {
        // Before we do anything, see if the supplied namespace is an aliased namespace ...
        String originalNamespaceUri = namespaceUrisByAlias.get(namespaceUri);
        if (originalNamespaceUri != null) {
            namespaceUri = originalNamespaceUri;
        }
        return delegate.getPrefixForNamespaceUri(namespaceUri, generateIfMissing);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.NamespaceRegistry#getRegisteredNamespaceUris()
     */
    public Set<String> getRegisteredNamespaceUris() {
        // Do not include the aliases in the namespace URIs ...
        return delegate.getRegisteredNamespaceUris();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.NamespaceRegistry#isRegisteredNamespaceUri(java.lang.String)
     */
    public boolean isRegisteredNamespaceUri( String namespaceUri ) {
        return delegate.isRegisteredNamespaceUri(namespaceUri);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.NamespaceRegistry#register(java.lang.String, java.lang.String)
     */
    public String register( String prefix,
                            String namespaceUri ) {
        // Let the delegate registry handle this, including cases where an aliased namespace is changed ...
        return delegate.register(prefix, namespaceUri);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.NamespaceRegistry#unregister(java.lang.String)
     */
    public boolean unregister( String namespaceUri ) {
        // We cannot unregister an alias, so we only need to try unregistering with the delegate registry ...
        return delegate.unregister(namespaceUri);
    }

}
