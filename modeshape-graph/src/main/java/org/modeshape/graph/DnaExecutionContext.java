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
package org.modeshape.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.modeshape.common.text.TextDecoder;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NameFactory;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.PropertyType;
import org.modeshape.graph.property.ValueFactories;
import org.modeshape.graph.property.ValueFactory;
import org.modeshape.graph.property.basic.AbstractValueFactory;
import org.modeshape.graph.property.basic.NameValueFactory;
import org.modeshape.graph.property.basic.NamespaceRegistryWithAliases;
import org.modeshape.graph.property.basic.PathValueFactory;
import org.modeshape.graph.property.basic.StandardValueFactories;

/**
 * A specialized form of {@link ExecutionContext} that supports the legacy JBoss DNA namespaces by automatically mapping them to
 * the corresponding ModeShape namespaces. <b><i>Note: This is only needed in cases where content originally persisted using JBoss
 * DNA is being accessed by ModeShape.</i></b>
 */
public class DnaExecutionContext extends ExecutionContext {

    public static class LegacyNamespaceUris {
        public static final String DNA = "http://www.jboss.org/dna/1.0";
        public static final String DNAINT = "http://www.jboss.org/dna/int/1.0";
    }

    public static class LegacyNamespacePrefixes {
        public static final String DNA = "dna";
        public static final String DNAINT = "dnaint";
    }

    private static final Map<String, String> LEGACY_NAMESPACE_URIS_BY_PREFIX;
    private static final Map<String, String> ORIGINAL_NAMESPACE_URIS_BY_LEGACY_URI;

    static {
        LEGACY_NAMESPACE_URIS_BY_PREFIX = new HashMap<String, String>();
        LEGACY_NAMESPACE_URIS_BY_PREFIX.put(LegacyNamespacePrefixes.DNA, LegacyNamespaceUris.DNA);
        LEGACY_NAMESPACE_URIS_BY_PREFIX.put(LegacyNamespacePrefixes.DNAINT, LegacyNamespaceUris.DNAINT);
        ORIGINAL_NAMESPACE_URIS_BY_LEGACY_URI = new HashMap<String, String>();
        ORIGINAL_NAMESPACE_URIS_BY_LEGACY_URI.put(LegacyNamespaceUris.DNA, ModeShapeLexicon.Namespace.URI);
        ORIGINAL_NAMESPACE_URIS_BY_LEGACY_URI.put(LegacyNamespaceUris.DNAINT, ModeShapeIntLexicon.Namespace.URI);
    }

    /**
     * Create a new execution context that supports the legacy JBoss DNA namespaces by automatically mapping them to the
     * corresponding ModeShape namespaces;
     */
    public DnaExecutionContext() {
        this(new ExecutionContext());
    }

    /**
     * Create a new execution context that mirrors the supplied context but with support for the legacy JBoss DNA namespaces by
     * automatically mapping them to the corresponding ModeShape namespaces;
     * 
     * @param original the original
     */
    public DnaExecutionContext( ExecutionContext original ) {
        super(legacyContextUsing(original)); // super just extracts the various pieces from the supplied context
    }

    @SuppressWarnings( "unchecked" )
    private static ExecutionContext legacyContextUsing( ExecutionContext original ) {
        if (original instanceof DnaExecutionContext) return original;

        final Map<String, String> originalUriByAliasUri = ORIGINAL_NAMESPACE_URIS_BY_LEGACY_URI;
        NamespaceRegistry newRegistry = new NamespaceRegistryWithAliases(original.getNamespaceRegistry(),
                                                                         LEGACY_NAMESPACE_URIS_BY_PREFIX, originalUriByAliasUri);
        // Collect the existing value factories (except the name and path factories) ...
        ValueFactories originalValueFactories = original.getValueFactories();
        List<ValueFactory<?>> factories = new ArrayList<ValueFactory<?>>();
        NameFactory originalNameFactory = null;
        PathFactory originalPathFactory = null;
        ValueFactory<String> stringFactory = null;
        for (ValueFactory<?> factory : originalValueFactories) {
            if (factory.getPropertyType() == PropertyType.NAME) {
                // Capture this reference, but don't add it ...
                originalNameFactory = (NameFactory)factory;
            } else if (factory.getPropertyType() == PropertyType.PATH) {
                // Capture this reference, but don't add it ...
                originalPathFactory = (PathFactory)factory;
            } else if (factory.getPropertyType() == PropertyType.STRING) {
                // Capture this reference AND add it ...
                stringFactory = (ValueFactory<String>)factory;
                factories.add(factory);
            } else {
                factories.add(factory);
            }
        }

        // Now add a new name factory that maps the aliased namespace URIs to the original ...
        TextDecoder nameDecoder = originalNameFactory instanceof AbstractValueFactory ? ((AbstractValueFactory<Name>)originalNameFactory).getDecoder() : null;
        NameFactory nameFactory = new NameValueFactory(newRegistry, nameDecoder, stringFactory) {
            /**
             * {@inheritDoc}
             * 
             * @see org.modeshape.graph.property.basic.NameValueFactory#create(java.lang.String, java.lang.String,
             *      org.modeshape.common.text.TextDecoder)
             */
            @Override
            public Name create( String namespaceUri,
                                String localName,
                                TextDecoder decoder ) {
                // If the given URI is an alias, replace it with the original ...
                String originalUri = originalUriByAliasUri.get(namespaceUri);
                if (originalUri != null) namespaceUri = originalUri;
                return super.create(namespaceUri, localName, decoder);
            }
        };
        factories.add(nameFactory);

        // Now add a new path factory that uses the new name factory ...
        TextDecoder pathDecoder = originalPathFactory instanceof AbstractValueFactory ? ((AbstractValueFactory<Path>)originalPathFactory).getDecoder() : null;
        PathFactory pathFactory = new PathValueFactory(pathDecoder, stringFactory, nameFactory);
        factories.add(pathFactory);

        // Create the value factories object ...
        ValueFactory<?>[] valueFactories = factories.toArray(new ValueFactory<?>[factories.size()]);
        ValueFactories newValueFactories = new StandardValueFactories(newRegistry, null, null, valueFactories);

        // Create the new context ...
        return new ExecutionContext(original.getSecurityContext(), newRegistry, newValueFactories, null,
                                    original.getMimeTypeDetector(), original.getClassLoaderFactory());
    }
}
