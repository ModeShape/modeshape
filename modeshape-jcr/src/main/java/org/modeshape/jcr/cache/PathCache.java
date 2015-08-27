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

    public NodeCache getCache() {
        return cache;
    }

    public Path getPath( CachedNode node ) {
        NodeKey key = node.getKey();
        Path path = paths.get(key);
        if (path == null) {
            path = node.getPath(this);
            paths.put(key, path); // even if null
        }
        return path;
    }

    public boolean removePath( NodeKey key ) {
        return paths.remove(key) != null;
    }
    
    public void put(NodeKey key, Path path) {
        this.paths.put(key, path);
    }
}
