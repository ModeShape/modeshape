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

import java.util.HashMap;
import java.util.Map;
import org.modeshape.jcr.value.Path;

/**
 * A simple cache of node paths, useful when obtaining the path for many nodes on a subgraph.
 */
public class PathCache {
    private final NodeCache cache;
    private final Map<NodeKey, Path> paths = new HashMap<NodeKey, Path>();

    public PathCache( NodeCache cache ) {
        this.cache = cache;
    }

    public Path getPath( CachedNode node ) {
        NodeKey key = node.getKey();
        Path path = paths.get(key);
        if (path == null) {
            path = node.getPath(cache);
            paths.put(key, path); // even if null
        }
        return path;
    }

    public boolean removePath( NodeKey key ) {
        return paths.remove(key) != null;
    }
}
