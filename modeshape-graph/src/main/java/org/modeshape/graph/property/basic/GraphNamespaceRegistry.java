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
package org.modeshape.graph.property.basic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.Graph;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.Location;
import org.modeshape.graph.ModeShapeLexicon;
import org.modeshape.graph.Node;
import org.modeshape.graph.Subgraph;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.PathNotFoundException;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.ValueFactory;

/**
 * A {@link NamespaceRegistry} implementation that stores the namespaces in a Graph as individual nodes for each namespace, under
 * a parent supplied by the constructor.
 * 
 * @See {@link ThreadSafeNamespaceRegistry}
 */
@NotThreadSafe
public class GraphNamespaceRegistry implements NamespaceRegistry {

    public static final Name DEFAULT_URI_PROPERTY_NAME = ModeShapeLexicon.URI;
    public static final String GENERATED_PREFIX = "ns";

    private SimpleNamespaceRegistry cache;
    private final Graph store;
    private final Path parentOfNamespaceNodes;
    private final Name uriPropertyName;
    private final List<Property> namespaceProperties;

    public GraphNamespaceRegistry( Graph store,
                                   Path parentOfNamespaceNodes,
                                   Name uriPropertyName,
                                   Property... additionalProperties ) {
        this.cache = new SimpleNamespaceRegistry();
        this.store = store;
        this.parentOfNamespaceNodes = parentOfNamespaceNodes;
        this.uriPropertyName = uriPropertyName != null ? uriPropertyName : DEFAULT_URI_PROPERTY_NAME;
        List<Property> properties = Collections.emptyList();
        if (additionalProperties != null && additionalProperties.length != 0) {
            properties = new ArrayList<Property>(additionalProperties.length);
            Set<Name> propertyNames = new HashSet<Name>();
            for (Property property : additionalProperties) {
                if (!propertyNames.contains(property.getName())) properties.add(property);
            }
        }
        this.namespaceProperties = Collections.unmodifiableList(properties);
        createNamespaceParentIfNeeded();
        initializeCacheFromStore(cache);
    }

    private void createNamespaceParentIfNeeded() {
        try {
            this.store.getNodeAt(this.parentOfNamespaceNodes);
        } catch (PathNotFoundException pnfe) {
            // The node did not already exist - create it!
            this.store.create(parentOfNamespaceNodes).and();
            this.store.set(JcrLexicon.PRIMARY_TYPE).on(parentOfNamespaceNodes).to(ModeShapeLexicon.NAMESPACES);
        }
    }

    /**
     * @return parentOfNamespaceNodes
     */
    public Path getParentOfNamespaceNodes() {
        return parentOfNamespaceNodes;
    }

    /**
     * {@inheritDoc}
     */
    public String getNamespaceForPrefix( String prefix ) {
        CheckArg.isNotNull(prefix, "prefix");
        // Try the cache first ...
        String uri = cache.getNamespaceForPrefix(prefix);
        if (uri == null) {
            // See if the store has it ...
            uri = readUriFor(prefix);
            if (uri != null) {
                // update the cache ...
                cache.register(prefix, uri);
            }
        }
        return uri;
    }

    /**
     * {@inheritDoc}
     */
    public String getPrefixForNamespaceUri( String namespaceUri,
                                            boolean generateIfMissing ) {
        CheckArg.isNotNull(namespaceUri, "namespaceUri");
        // Try the cache first ...
        String prefix = cache.getPrefixForNamespaceUri(namespaceUri, false);
        if (prefix == null && generateIfMissing) {
            prefix = readPrefixFor(namespaceUri, generateIfMissing);
            if (prefix != null) {
                cache.register(prefix, namespaceUri);
            }
        }
        return prefix;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isRegisteredNamespaceUri( String namespaceUri ) {
        CheckArg.isNotNull(namespaceUri, "namespaceUri");
        if (cache.isRegisteredNamespaceUri(namespaceUri)) return true;
        // Otherwise it was not found in the cache, so check the store ...
        String prefix = readPrefixFor(namespaceUri, false);
        if (prefix != null) {
            cache.register(prefix, namespaceUri);
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public String getDefaultNamespaceUri() {
        return this.getNamespaceForPrefix("");
    }

    /**
     * {@inheritDoc}
     */
    public String register( String prefix,
                            String namespaceUri ) {
        CheckArg.isNotNull(namespaceUri, "namespaceUri");
        namespaceUri = namespaceUri.trim();
        // Register it in the cache first ...
        String previousCachedUriForPrefix = this.cache.register(prefix, namespaceUri);
        // And register it in the source ...
        String previousPersistentUriForPrefix = doRegister(prefix, namespaceUri);
        return previousCachedUriForPrefix != null ? previousPersistentUriForPrefix : previousPersistentUriForPrefix;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.NamespaceRegistry#unregister(java.lang.String)
     */
    public boolean unregister( String namespaceUri ) {
        CheckArg.isNotNull(namespaceUri, "namespaceUri");
        namespaceUri = namespaceUri.trim();
        // Remove it from the cache ...
        boolean found = this.cache.unregister(namespaceUri);
        // Then from the source ...
        return doUnregister(namespaceUri) || found;
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getRegisteredNamespaceUris() {
        // Just return what's in the cache ...
        return cache.getRegisteredNamespaceUris();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.NamespaceRegistry#getNamespaces()
     */
    public Set<Namespace> getNamespaces() {
        // Just return what's in the cache ...
        return cache.getNamespaces();
    }

    /**
     * Refresh the namespaces from the persistent store, and update the embedded cache. This operation is done atomically.
     */
    public void refresh() {
        SimpleNamespaceRegistry newCache = new SimpleNamespaceRegistry();
        initializeCacheFromStore(newCache);
        this.cache = newCache;
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

    protected void initializeCacheFromStore( NamespaceRegistry cache ) {
        // Get the namespaces that the store is using ...
        Set<Namespace> toRegister = new HashSet<Namespace>(store.getContext().getNamespaceRegistry().getNamespaces());

        // Read the store ...
        try {
            Subgraph nsGraph = store.getSubgraphOfDepth(2).at(parentOfNamespaceNodes);
            ValueFactory<String> stringFactory = store.getContext().getValueFactories().getStringFactory();
            for (Location nsLocation : nsGraph.getRoot().getChildren()) {
                Node ns = nsGraph.getNode(nsLocation);
                // This node is a namespace ...
                String uri = stringFactory.create(ns.getProperty(uriPropertyName).getFirstValue());
                if (uri != null) {
                    String prefix = getPrefixFor(nsLocation.getPath());
                    cache.register(prefix, uri);
                    // If we found it, we don't need to register it ...
                    toRegister.remove(new BasicNamespace(prefix, uri));
                }
            }

            // Empty prefix to namespace mapping is built-in
            cache.register("", "");
            toRegister.remove(cache.getNamespaceForPrefix(""));

            // Persist any namespaces that we didn't find ...
            if (!toRegister.isEmpty()) {
                Graph.Batch batch = store.batch();
                PathFactory pathFactory = store.getContext().getValueFactories().getPathFactory();
                for (Namespace namespace : toRegister) {
                    String prefix = namespace.getPrefix();
                    if (prefix.length() == 0) continue;
                    String uri = namespace.getNamespaceUri();
                    Path pathToNamespaceNode = pathFactory.create(parentOfNamespaceNodes, prefix);
                    batch.create(pathToNamespaceNode).with(namespaceProperties).and(uriPropertyName, uri).and();
                }
                batch.execute();
            }

        } catch (PathNotFoundException e) {
            // Nothing to read
        }
        // Load in the namespaces from the execution context used by the store ...
        for (Namespace namespace : store.getContext().getNamespaceRegistry().getNamespaces()) {
            register(namespace.getPrefix(), namespace.getNamespaceUri());
        }
    }

    protected String readUriFor( String prefix ) {
        // Read the store ...
        try {
            PathFactory pathFactory = store.getContext().getValueFactories().getPathFactory();
            Path pathToNamespaceNode = pathFactory.create(parentOfNamespaceNodes, prefix);
            Property uri = store.getProperty(uriPropertyName).on(pathToNamespaceNode);
            // Get the URI property value ...
            ValueFactory<String> stringFactory = store.getContext().getValueFactories().getStringFactory();
            return stringFactory.create(uri.getFirstValue());
        } catch (PathNotFoundException e) {
            // Nothing to read
            return null;
        }
    }

    protected String getPrefixFor( Path path ) {
        Path.Segment lastSegment = path.getLastSegment();
        String localName = lastSegment.getName().getLocalName();
        int index = lastSegment.getIndex();
        if (index == 1) {
            if (GENERATED_PREFIX.equals(localName)) return localName + "00" + index;
            return localName;
        }
        if (index < 10) {
            return localName + "00" + index;
        }
        if (index < 100) {
            return localName + "0" + index;
        }
        return localName + index;
    }

    protected String readPrefixFor( String namespaceUri,
                                    boolean generateIfMissing ) {
        // Read the store ...
        try {
            Subgraph nsGraph = store.getSubgraphOfDepth(2).at(parentOfNamespaceNodes);
            ValueFactory<String> stringFactory = store.getContext().getValueFactories().getStringFactory();
            for (Location nsLocation : nsGraph.getRoot().getChildren()) {
                Node ns = nsGraph.getNode(nsLocation);
                String prefix = getPrefixFor(nsLocation.getPath());
                String uri = stringFactory.create(ns.getProperty(uriPropertyName).getFirstValue());
                if (prefix != null && uri != null) {
                    if (uri.equals(namespaceUri)) return prefix;
                }
            }
            if (generateIfMissing) {
                // Generated prefixes are simply "ns" followed by the SNS index ...
                PathFactory pathFactory = store.getContext().getValueFactories().getPathFactory();
                Path pathToNamespaceNode = pathFactory.create(parentOfNamespaceNodes, GENERATED_PREFIX);
                Property uriProperty = store.getContext().getPropertyFactory().create(uriPropertyName, namespaceUri);
                List<Property> props = new ArrayList<Property>(namespaceProperties);
                props.add(uriProperty);
                // Location actualLocation = store.createIfMissing(pathToNamespaceNode, props).andReturn().getLocation();
                store.create(pathToNamespaceNode, props).ifAbsent().and();

                return getPrefixFor(pathToNamespaceNode);
            }

        } catch (PathNotFoundException e) {
            // Nothing to read
        }
        return null;
    }

    protected String doRegister( String prefix,
                                 String uri ) {
        assert prefix != null;
        assert uri != null;
        prefix = prefix.trim();
        uri = uri.trim();

        // Empty prefix to namespace mapping is built in
        if (prefix.length() == 0) {
            return null;
        }

        // Read the store ...
        String previousUri = null;
        ValueFactory<String> stringFactory = store.getContext().getValueFactories().getStringFactory();
        PathFactory pathFactory = store.getContext().getValueFactories().getPathFactory();
        Path pathToNamespaceNode = pathFactory.create(parentOfNamespaceNodes, prefix);
        try {
            Subgraph nsGraph = store.getSubgraphOfDepth(2).at(parentOfNamespaceNodes);
            // Iterate over the existing mappings, looking for one that uses the URI ...
            Location nsNodeWithPrefix = null;
            boolean updateNode = true;
            Set<Location> locationsToRemove = new HashSet<Location>();
            for (Location nsLocation : nsGraph.getRoot().getChildren()) {
                Node ns = nsGraph.getNode(nsLocation);
                String actualPrefix = getPrefixFor(nsLocation.getPath());
                String actualUri = stringFactory.create(ns.getProperty(uriPropertyName).getFirstValue());
                if (actualPrefix != null && actualUri != null) {
                    if (actualPrefix.equals(prefix)) {
                        nsNodeWithPrefix = nsLocation;
                        if (actualUri.equals(uri)) {
                            updateNode = false;
                            break;
                        }
                        previousUri = actualUri;
                    }
                    if (actualUri.equals(uri)) {
                        locationsToRemove.add(ns.getLocation());
                    }
                }
            }
            Graph.Batch batch = store.batch();
            // Remove any other nodes that have the same URI ...
            for (Location namespaceToRemove : locationsToRemove) {
                batch.delete(namespaceToRemove).and();
            }
            // Now update/create the namespace mapping ...
            if (nsNodeWithPrefix == null) {
                // We didn't find an existing node, so we have to create it ...
                batch.create(pathToNamespaceNode).with(namespaceProperties).and(uriPropertyName, uri).and();
            } else {
                if (updateNode) {
                    // There was already an existing node, so update it ...
                    batch.set(uriPropertyName).to(uri).on(pathToNamespaceNode).and();
                }
            }
            // Execute all these changes ...
            batch.execute();
        } catch (PathNotFoundException e) {
            // Nothing stored yet ...
            store.createAt(pathToNamespaceNode).with(namespaceProperties).and(uriPropertyName, uri).getLocation();
        }
        return previousUri;
    }

    protected boolean doUnregister( String uri ) {
        // Read the store ...
        ValueFactory<String> stringFactory = store.getContext().getValueFactories().getStringFactory();
        boolean result = false;
        try {
            Subgraph nsGraph = store.getSubgraphOfDepth(2).at(parentOfNamespaceNodes);
            // Iterate over the existing mappings, looking for one that uses the prefix and uri ...
            Set<Location> locationsToRemove = new HashSet<Location>();
            for (Location nsLocation : nsGraph.getRoot().getChildren()) {
                Node ns = nsGraph.getNode(nsLocation);
                String actualUri = stringFactory.create(ns.getProperty(uriPropertyName).getFirstValue());
                if (actualUri.equals(uri)) {
                    locationsToRemove.add(ns.getLocation());
                    result = true;
                }
            }
            // Remove any other nodes that have the same URI ...
            Graph.Batch batch = store.batch();
            for (Location namespaceToRemove : locationsToRemove) {
                batch.delete(namespaceToRemove).and();
            }
            // Execute all these changes ...
            batch.execute();
        } catch (PathNotFoundException e) {
            // Nothing stored yet, so do nothing ...
        }
        return result;
    }

}
