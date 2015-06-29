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
package org.modeshape.jcr.cache.document;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.modeshape.jcr.bus.RepositoryChangeBus;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.NodeCache;
import org.modeshape.jcr.cache.NodeKey;

public class WorkspaceCacheTest extends AbstractNodeCacheTest {

    private ExecutorService executor;
    private RepositoryChangeBus changeBus;

    @Override
    protected NodeCache createCache() {
        executor = Executors.newCachedThreadPool();
        changeBus = new RepositoryChangeBus("repo", executor);
        ConcurrentMap<NodeKey, CachedNode> nodeCache = new ConcurrentHashMap<NodeKey, CachedNode>();
        DocumentStore documentStore = new LocalDocumentStore(schematicDb);
        DocumentTranslator translator = new DocumentTranslator(context, documentStore, 100L);
        WorkspaceCache workspaceCache = new WorkspaceCache(context, "repo", "ws", null, documentStore, translator, ROOT_KEY_WS1,
                                                           nodeCache, changeBus,  null);
        loadJsonDocuments(resource(resourceNameForWorkspaceContentDocument()));
        return workspaceCache;
    }

    @Override
    protected void shutdownCache( NodeCache cache ) {
        super.shutdownCache(cache);
        executor.shutdown();
    }
}
