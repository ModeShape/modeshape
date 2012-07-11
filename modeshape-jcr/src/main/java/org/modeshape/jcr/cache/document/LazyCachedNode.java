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
package org.modeshape.jcr.cache.document;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.infinispan.schematic.document.Document;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.ChildReference;
import org.modeshape.jcr.cache.ChildReferences;
import org.modeshape.jcr.cache.NodeCache;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.NodeNotFoundException;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NameFactory;
import org.modeshape.jcr.value.NamespaceRegistry;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Path.Segment;
import org.modeshape.jcr.value.Property;

/**
 * This is an immutable {@link CachedNode} implementation that lazily loads its content. Technically each instance modifies its
 * internal state, but all the state is based upon a single Document that is read-in only once and never changed again. And thus
 * externally each instance appears to be immutable and invariant.
 */
@Immutable
public class LazyCachedNode implements CachedNode {

    private final NodeKey key;
    private Document document;
    private Map<Name, Property> properties;
    private NodeKey parent;
    private Set<NodeKey> additionalParents;
    private ChildReference parentReferenceToSelf;
    private boolean propertiesFullyLoaded = false;
    private ChildReferences childReferences;

    public LazyCachedNode( NodeKey key ) {
        this.key = key;
    }

    public LazyCachedNode( NodeKey key,
                           Document document ) {
        this.key = key;
        this.document = document;
    }

    protected final WorkspaceCache workspaceCache( NodeCache cache ) {
        return ((DocumentCache)cache).workspaceCache();
    }

    /**
     * Get the {@link Document} that represents this node.
     * 
     * @param cache the cache to which this node belongs, required in case this node needs to use the cache; may not be null
     * @return the document; never null
     * @throws NodeNotFoundException if this node no longer exists
     */
    protected Document document( WorkspaceCache cache ) {
        if (document == null) {
            // Fetch from the cache ...
            document = cache.documentFor(key);
            if (document == null) {
                throw new NodeNotFoundException(key);
            }
        }
        return document;
    }

    @Override
    public NodeKey getParentKey( NodeCache cache ) {
        if (parent == null) {
            WorkspaceCache wsCache = workspaceCache(cache);
            parent = wsCache.translator().getParentKey(document(wsCache), wsCache.getWorkspaceKey(), key.getWorkspaceKey());
        }
        return parent;
    }

    @Override
    public NodeKey getParentKeyInAnyWorkspace( NodeCache cache ) {
        WorkspaceCache wsCache = workspaceCache(cache);
        return wsCache.translator().getParentKey(document(wsCache), key.getWorkspaceKey(), key.getWorkspaceKey());
    }

    @Override
    public Set<NodeKey> getAdditionalParentKeys( NodeCache cache ) {
        if (additionalParents == null) {
            WorkspaceCache wsCache = workspaceCache(cache);
            Set<NodeKey> additionalParents = wsCache.translator().getParentKeys(document(wsCache),
                                                                                wsCache.getWorkspaceKey(),
                                                                                key.getWorkspaceKey());
            this.additionalParents = additionalParents.isEmpty() ? additionalParents : Collections.unmodifiableSet(additionalParents);
        }
        return additionalParents;
    }

    @Override
    public boolean isAtOrBelow( NodeCache cache,
                                Path path ) {
        Path aPath = getPath(cache);
        if (path.isAtOrAbove(aPath)) return true;
        Set<NodeKey> additionalParents = getAdditionalParentKeys(cache);
        if (!additionalParents.isEmpty()) {
            Path parentOfPath = path.getParent();
            for (NodeKey parentKey : additionalParents) {
                CachedNode parent = cache.getNode(parentKey);
                if (parent.getPath(cache).isAtOrBelow(parentOfPath)) {
                    ChildReference ref = parent.getChildReferences(cache).getChild(key);
                    if (ref != null && ref.getSegment().equals(path.getLastSegment())) return true;
                }
            }
        }
        return false;
    }

    protected CachedNode parent( WorkspaceCache cache ) {
        NodeKey parentKey = getParentKey(cache);
        if (parentKey == null) {
            return null;
        }
        CachedNode parent = cache.getNode(parentKey);
        if (parent == null) {
            throw new NodeNotFoundException(parentKey);
        }
        return parent;
    }

    /**
     * Get the parent node's child reference to this node.
     * 
     * @param cache the cache
     * @return the child reference; never null (even for the root node)
     */
    protected ChildReference parentReferenceToSelf( WorkspaceCache cache ) {
        if (parentReferenceToSelf == null) {
            CachedNode parent = parent(cache);
            if (parent == null) {
                // This should be the root node ...
                parentReferenceToSelf = cache.childReferenceForRoot();
            } else {
                parentReferenceToSelf = parent.getChildReferences(cache).getChild(key);
            }
        }
        return parentReferenceToSelf;
    }

    protected Map<Name, Property> properties() {
        if (properties == null) {
            properties = new ConcurrentHashMap<Name, Property>();
        }
        return properties;
    }

    @Override
    public NodeKey getKey() {
        return key;
    }

    @Override
    public Name getName( NodeCache cache ) {
        return parentReferenceToSelf(workspaceCache(cache)).getName();
    }

    @Override
    public Segment getSegment( NodeCache cache ) {
        return parentReferenceToSelf(workspaceCache(cache)).getSegment();
    }

    protected Segment getSegment( WorkspaceCache cache ) {
        return parentReferenceToSelf(cache).getSegment();
    }

    @Override
    public Path getPath( NodeCache cache ) {
        WorkspaceCache wsCache = workspaceCache(cache);
        CachedNode parent = parent(wsCache);
        if (parent != null) {
            Path parentPath = parent.getPath(wsCache);
            return wsCache.pathFactory().create(parentPath, getSegment(wsCache));
        }
        // check that the node hasn't been removed in the meantime
        if (wsCache.getNode(key) == null) {
            throw new NodeNotFoundException(key);
        }
        // This is the root node ...
        return wsCache.rootPath();
    }

    @Override
    public Name getPrimaryType( NodeCache cache ) {
        Property prop = getProperty(JcrLexicon.PRIMARY_TYPE, cache);
        WorkspaceCache wsCache = workspaceCache(cache);
        return wsCache.nameFactory().create(prop.getFirstValue());
    }

    @Override
    public Set<Name> getMixinTypes( NodeCache cache ) {
        Property prop = getProperty(JcrLexicon.MIXIN_TYPES, cache);
        if (prop == null || prop.size() == 0) return Collections.emptySet();

        final NameFactory nameFactory = workspaceCache(cache).nameFactory();
        if (prop.size() == 1) {
            Name name = nameFactory.create(prop.getFirstValue());
            return Collections.singleton(name);
        }
        Set<Name> names = new HashSet<Name>();
        for (Object value : prop) {
            Name name = nameFactory.create(value);
            names.add(name);
        }
        return names;
    }

    @Override
    public int getPropertyCount( NodeCache cache ) {
        if (propertiesFullyLoaded) return properties().size();
        WorkspaceCache wsCache = workspaceCache(cache);
        return wsCache.translator().countProperties(document(wsCache));
    }

    @Override
    public boolean hasProperties( NodeCache cache ) {
        Map<Name, Property> props = properties();
        if (!props.isEmpty()) return true;
        if (propertiesFullyLoaded) return false;
        WorkspaceCache wsCache = workspaceCache(cache);
        return wsCache.translator().hasProperties(document(wsCache));
    }

    @Override
    public boolean hasProperty( Name name,
                                NodeCache cache ) {
        Map<Name, Property> props = properties();
        if (props.containsKey(name)) return true;
        if (propertiesFullyLoaded) return false;
        WorkspaceCache wsCache = workspaceCache(cache);
        return wsCache.translator().hasProperty(document(wsCache), name);
    }

    @Override
    public Property getProperty( Name name,
                                 NodeCache cache ) {
        Map<Name, Property> props = properties();
        Property property = props.get(name);
        if (property == null && !propertiesFullyLoaded) {
            WorkspaceCache wsCache = workspaceCache(cache);
            property = wsCache.translator().getProperty(document(wsCache), name);
            if (property != null) {
                props.put(name, property);
            }
        }
        return property;
    }

    @Override
    public Iterator<Property> getProperties( NodeCache cache ) {
        if (!propertiesFullyLoaded) {
            WorkspaceCache wsCache = workspaceCache(cache);
            wsCache.translator().getProperties(document(wsCache), properties());
            this.propertiesFullyLoaded = true;
        }
        return properties().values().iterator();
    }

    @Override
    public Iterator<Property> getProperties( Collection<?> namePatterns,
                                             NodeCache cache ) {
        WorkspaceCache wsCache = workspaceCache(cache);
        final NamespaceRegistry registry = wsCache.context().getNamespaceRegistry();
        return new PatternIterator<Property>(getProperties(wsCache), namePatterns) {
            @Override
            protected String matchable( Property value ) {
                return value.getName().getString(registry);
            }
        };
    }

    @Override
    public ChildReferences getChildReferences( NodeCache cache ) {
        if (childReferences == null) {
            WorkspaceCache wsCache = workspaceCache(cache);
            childReferences = wsCache.translator().getChildReferences(wsCache, document(wsCache));
        }
        return childReferences;
    }

    @Override
    public Set<NodeKey> getReferrers( NodeCache cache,
                                      ReferenceType type ) {
        // Get the referrers ...
        WorkspaceCache wsCache = workspaceCache(cache);
        return wsCache.translator().getReferrers(document(wsCache), type);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof CachedNode) {
            CachedNode that = (CachedNode)obj;
            return this.getKey().equals(that.getKey());
        }
        return false;
    }

    @Override
    public String toString() {
        return getString(null);
    }

    public String getString( NamespaceRegistry registry ) {
        StringBuilder sb = new StringBuilder();
        sb.append("Node ").append(key).append(": ");
        if (document != null) sb.append(document);
        else sb.append(" <unloaded>");
        return sb.toString();
    }

}
