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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.value.NamespaceRegistry;

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

    public void clear() {
        this.namespacesByPrefix.clear();
        this.prefixesByNamespace.clear();
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

    @Override
    public String getNamespaceForPrefix( String prefix ) {
        CheckArg.isNotNull(prefix, "prefix");
        return this.namespacesByPrefix.get(prefix);
    }

    @Override
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

    @Override
    public boolean isRegisteredNamespaceUri( String namespaceUri ) {
        CheckArg.isNotNull(namespaceUri, "namespaceUri");
        return this.prefixesByNamespace.containsKey(namespaceUri);
    }

    @Override
    public String getDefaultNamespaceUri() {
        return this.namespacesByPrefix.get("");
    }

    @Override
    public void register( Iterable<Namespace> namespaces ) {
        for (Namespace namespace : namespaces) {
            register(namespace.getPrefix(), namespace.getNamespaceUri());
        }
    }

    @Override
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

    @Override
    public boolean unregister( String namespaceUri ) {
        CheckArg.isNotNull(namespaceUri, "namespaceUri");
        namespaceUri = namespaceUri.trim();
        String prefix = this.prefixesByNamespace.remove(namespaceUri);
        if (prefix == null) return false;
        this.namespacesByPrefix.remove(prefix);
        return true;
    }

    @Override
    public Set<String> getRegisteredNamespaceUris() {
        Set<String> result = new HashSet<String>();
        result.addAll(this.prefixesByNamespace.keySet());
        return Collections.unmodifiableSet(result);
    }

    @Override
    public Set<Namespace> getNamespaces() {
        Set<Namespace> result = new HashSet<Namespace>();
        for (Map.Entry<String, String> entry : this.namespacesByPrefix.entrySet()) {
            result.add(new BasicNamespace(entry.getKey(), entry.getValue()));
        }
        return Collections.unmodifiableSet(result);
    }

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
