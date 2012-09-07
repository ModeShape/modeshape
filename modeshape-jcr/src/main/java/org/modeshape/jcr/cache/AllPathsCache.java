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
package org.modeshape.jcr.cache;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Path.Segment;
import org.modeshape.jcr.value.PathFactory;

/**
 * A simple cache of all valid paths for a given node, where each node may have 1 or more valid paths due to
 * {@link CachedNode#getAdditionalParentKeys(NodeCache) additional parents}.
 */
public class AllPathsCache {
    protected final NodeCache cache;
    protected final NodeCache removedCache;
    private final Map<NodeKey, List<Path>> paths = new HashMap<NodeKey, List<Path>>();
    protected final PathFactory pathFactory;

    public AllPathsCache( NodeCache cache,
                          NodeCache removedCache,
                          ExecutionContext context ) {
        this.cache = cache;
        this.removedCache = removedCache;
        this.pathFactory = context.getValueFactories().getPathFactory();
    }

    public NodeCache getCache() {
        return cache;
    }

    /**
     * Get all of the paths through which the specified node is accessible, including all paths based upon the node's
     * {@link CachedNode#getParentKey(NodeCache) parent} (which can potentially have multiple paths) and upon the node's
     * {@link CachedNode#getAdditionalParentKeys(NodeCache) additional parents} (which each can potentially have multiple paths).
     * 
     * @param node the node for which the paths are to be returned; may not be null
     * @return the paths for the node; never null but possibly empty
     */
    public Iterable<Path> getPaths( CachedNode node ) {
        NodeKey key = node.getKey();
        List<Path> pathList = paths.get(key);
        if (pathList == null) {
            // Compute the node's path ...
            Segment nodeSegment = node.getSegment(cache);
            NodeKey parentKey = node.getParentKey(cache);
            if (parentKey == null) {
                // This is the root node ...
                pathList = Collections.singletonList(node.getPath(cache));
            } else {
                // This is not the root node, so add a path for each of the parent's valid paths ...
                CachedNode parent = cache.getNode(parentKey);
                if (parent == null && removedCache != null) {
                    // This is a removed node, so check the removed cache ...
                    parent = removedCache.getNode(parentKey);
                }
                pathList = new LinkedList<Path>();
                for (Path parentPath : getPaths(parent)) {
                    Path path = pathFactory.create(parentPath, nodeSegment);
                    pathList.add(path);
                }
                // Get the additional parents ...
                Set<NodeKey> additionalParentKeys = getAdditionalParentKeys(node, cache);
                // There is at least one additional parent ...
                for (NodeKey additionalParentKey : additionalParentKeys) {
                    parent = cache.getNode(additionalParentKey);
                    for (Path parentPath : getPaths(parent)) {
                        Path path = pathFactory.create(parentPath, nodeSegment);
                        pathList.add(path);
                    }
                }
            }
            assert pathList != null;
            pathList = Collections.unmodifiableList(pathList);
            paths.put(key, pathList);
        }
        return pathList;
    }

    public boolean removePath( NodeKey key ) {
        return paths.remove(key) != null;
    }

    protected Set<NodeKey> getAdditionalParentKeys( CachedNode node,
                                                    NodeCache cache ) {
        return node.getAdditionalParentKeys(cache);
    }
}
