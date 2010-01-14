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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.property.NamespaceRegistry;

/**
 * A simple {@link NamespaceRegistry} implementation that is not thread-safe, but that provides all the basic functionality.
 */
@NotThreadSafe
public class SimpleNamespaceRegistry implements NamespaceRegistry {

    public static final String DEFAULT_NAMESPACE_URI = "";
    public static final String DEFAULT_PREFIX_TEMPLATE = "ns##000";
    public static final String DEFAULT_PREFIX_NUMBER_FORMAT = "##000";

    private final Map<String, String> namespacesByPrefix = new HashMap<String, String>();
    private final Map<String, String> prefixesByNamespace = new HashMap<String, String>();
    private String generatedPrefixTemplate = DEFAULT_PREFIX_TEMPLATE;
    private int nextGeneratedPrefixNumber = 1;

    /**
     * 
     */
    public SimpleNamespaceRegistry() {
        this(DEFAULT_NAMESPACE_URI);
    }

    /**
     * @param defaultNamespaceUri the namespace URI to use for the default prefix
     */
    public SimpleNamespaceRegistry( final String defaultNamespaceUri ) {
        register("", defaultNamespaceUri);
    }

    /**
     * @return prefixTemplate
     */
    public String getGeneratedPrefixTemplate() {
        return this.generatedPrefixTemplate;
    }

    /**
     * @param prefixTemplate Sets prefixTemplate to the specified value.
     */
    public void setGeneratedPrefixTemplate( String prefixTemplate ) {
        if (prefixTemplate == null) prefixTemplate = DEFAULT_PREFIX_TEMPLATE;
        this.generatedPrefixTemplate = prefixTemplate;
    }

    /**
     * {@inheritDoc}
     */
    public String getNamespaceForPrefix( String prefix ) {
        CheckArg.isNotNull(prefix, "prefix");
        return this.namespacesByPrefix.get(prefix);
    }

    /**
     * {@inheritDoc}
     */
    public String getPrefixForNamespaceUri( String namespaceUri,
                                            boolean generateIfMissing ) {
        CheckArg.isNotNull(namespaceUri, "namespaceUri");
        String prefix = null;
        prefix = this.prefixesByNamespace.get(namespaceUri);
        if (prefix == null && generateIfMissing) {
            // Now we can genereate a prefix and register it ...
            prefix = this.generatePrefix();
            this.register(prefix, namespaceUri);
            return prefix;
        }
        return prefix;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isRegisteredNamespaceUri( String namespaceUri ) {
        CheckArg.isNotNull(namespaceUri, "namespaceUri");
        return this.prefixesByNamespace.containsKey(namespaceUri);
    }

    /**
     * {@inheritDoc}
     */
    public String getDefaultNamespaceUri() {
        return this.namespacesByPrefix.get("");
    }

    /**
     * {@inheritDoc}
     */
    public String register( String prefix,
                            String namespaceUri ) {
        CheckArg.isNotNull(namespaceUri, "namespaceUri");
        String previousNamespaceForPrefix = null;
        namespaceUri = namespaceUri.trim();
        if (prefix == null) prefix = generatePrefix();
        prefix = prefix.trim();
        prefix = prefix.replaceFirst("^:+", "");
        prefix = prefix.replaceFirst(":+$", "");
        previousNamespaceForPrefix = this.namespacesByPrefix.put(prefix, namespaceUri);
        String previousPrefix = this.prefixesByNamespace.put(namespaceUri, prefix);
        if (previousPrefix != null && !previousPrefix.equals(prefix)) {
            this.namespacesByPrefix.remove(previousPrefix);
        }
        if (previousNamespaceForPrefix != null && !previousNamespaceForPrefix.equals(namespaceUri)) {
            this.prefixesByNamespace.remove(previousNamespaceForPrefix);
        }
        return previousNamespaceForPrefix;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.NamespaceRegistry#unregister(java.lang.String)
     */
    public boolean unregister( String namespaceUri ) {
        CheckArg.isNotNull(namespaceUri, "namespaceUri");
        namespaceUri = namespaceUri.trim();
        String prefix = this.prefixesByNamespace.remove(namespaceUri);
        if (prefix == null) return false;
        this.namespacesByPrefix.remove(prefix);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getRegisteredNamespaceUris() {
        Set<String> result = new HashSet<String>();
        result.addAll(this.prefixesByNamespace.keySet());
        return Collections.unmodifiableSet(result);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.NamespaceRegistry#getNamespaces()
     */
    public Set<Namespace> getNamespaces() {
        Set<Namespace> result = new HashSet<Namespace>();
        for (Map.Entry<String, String> entry : this.namespacesByPrefix.entrySet()) {
            result.add(new BasicNamespace(entry.getKey(), entry.getValue()));
        }
        return Collections.unmodifiableSet(result);
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

    protected String generatePrefix() {
        DecimalFormat formatter = new DecimalFormat(this.generatedPrefixTemplate);
        return formatter.format(nextGeneratedPrefixNumber++);
    }

}
