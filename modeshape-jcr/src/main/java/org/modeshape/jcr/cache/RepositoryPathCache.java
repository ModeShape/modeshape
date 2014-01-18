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
import org.modeshape.common.annotation.NotThreadSafe;

/**
 * A set of {@link PathCache} instances keyed by workspace names.
 */
@NotThreadSafe
public class RepositoryPathCache {

    private final Map<String, PathCache> pathCacheByWorkspaceName;

    public RepositoryPathCache() {
        this.pathCacheByWorkspaceName = new HashMap<String, PathCache>();
    }

    public PathCache getPathCache( String workspaceName,
                                   NodeCache nodeCacheForWorkspace ) throws WorkspaceNotFoundException {
        PathCache cache = pathCacheByWorkspaceName.get(workspaceName);
        if (cache == null) {
            cache = new PathCache(nodeCacheForWorkspace);
            pathCacheByWorkspaceName.put(workspaceName, cache);
        }
        return cache;
    }

}
