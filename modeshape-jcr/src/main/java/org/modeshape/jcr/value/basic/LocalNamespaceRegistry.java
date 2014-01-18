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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.value.NamespaceRegistry;

/**
 * A special {@link NamespaceRegistry} implementation that can be used to track transient registrations for another delegate
 * registry.
 */
@NotThreadSafe
public class LocalNamespaceRegistry extends SimpleNamespaceRegistry {

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

    @Override
    public String getDefaultNamespaceUri() {
        String result = super.getDefaultNamespaceUri();
        if (result == null) result = this.delegate.getDefaultNamespaceUri();
        return result;
    }

    @Override
    public String getNamespaceForPrefix( String prefix ) {
        String result = super.getNamespaceForPrefix(prefix);
        if (result == null) {
            result = this.delegate.getNamespaceForPrefix(prefix);
            // Catch if this namespace was remapped
            if (result != null && super.getPrefixForNamespaceUri(result, false) != null) {
                return null;
            }
        }
        return result;
    }

    @Override
    public Set<Namespace> getNamespaces() {
        Set<Namespace> delegateNamespaces = this.delegate.getNamespaces();
        Set<Namespace> localNamespaces = super.getNamespaces();
        if (localNamespaces.isEmpty()) return delegateNamespaces;

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
     * Obtain the set of namespaces that are overridden within this {@link LocalNamespaceRegistry} instance.
     * 
     * @return the set of overridden namespace mappings; never null but possibly empty
     */
    public Set<Namespace> getLocalNamespaces() {
        return super.getNamespaces();
    }

    @Override
    public String getPrefixForNamespaceUri( String namespaceUri,
                                            boolean generateIfMissing ) {
        String result = super.getPrefixForNamespaceUri(namespaceUri, false);
        if (result == null) {
            result = this.delegate.getPrefixForNamespaceUri(namespaceUri, false);
            if (result != null) {
                // This was remapped at the session layer
                if (!this.getNamespaceForPrefix(result).equals(namespaceUri)) {
                    result = null;
                }
            }
        }
        if (result == null && generateIfMissing) result = super.getPrefixForNamespaceUri(namespaceUri, true);
        return result;
    }

    @Override
    public Set<String> getRegisteredNamespaceUris() {
        Set<String> uris = new HashSet<String>(this.delegate.getRegisteredNamespaceUris());
        uris.addAll(super.getRegisteredNamespaceUris());
        return Collections.unmodifiableSet(uris);
    }

    @Override
    public boolean isRegisteredNamespaceUri( String namespaceUri ) {
        return super.isRegisteredNamespaceUri(namespaceUri) || this.delegate.isRegisteredNamespaceUri(namespaceUri);
    }

    @Override
    public String register( String prefix,
                            String namespaceUri ) {
        // Just register the namespace locally ...
        String previous = super.register(prefix, namespaceUri);
        // But check whether there is a "previous" from the delegate ...
        if (previous == null && delegate != null) previous = delegate.getPrefixForNamespaceUri(namespaceUri, false);
        return previous;
    }

    @Override
    public boolean unregister( String namespaceUri ) {
        // Unregister locally ...
        return super.unregister(namespaceUri);
    }

}
