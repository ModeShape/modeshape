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

package org.modeshape.jcr.spi.index.provider;

import java.util.Set;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.cache.CachedNode.Properties;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.change.ChangeSetAdapter;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;

/**
 * @author Randall Hauch (rhauch@redhat.com)
 */
public class IndexChangeAdapter extends ChangeSetAdapter {

    private final String workspaceName;

    public IndexChangeAdapter( ExecutionContext context,
                               String workspaceName,
                               ChangeSetAdapter.NodeTypePredicate predicate ) {
        super(context, predicate);
        assert workspaceName != null;
        this.workspaceName = workspaceName;
    }

    @Override
    protected boolean includesWorkspace( String workspaceName ) {
        return this.workspaceName.equals(workspaceName);
    }

    /**
     * Index the specific node. This is called only by the reindexing operations.
     *
     * @param workspaceName the workspace in which the node information should be available; may not be null
     * @param key the unique key for the node; may not be null
     * @param path the path of the node; may not be null
     * @param primaryType the primary type of the node; may not be null
     * @param mixinTypes the mixin types for the node; may not be null but may be empty
     * @param properties the properties of the node; may not be null but may be empty
     * @param queryable true if the node is queryable, false otherwise
     */
    protected final void index( String workspaceName,
                                NodeKey key,
                                Path path,
                                Name primaryType,
                                Set<Name> mixinTypes,
                                Properties properties,
                                boolean queryable ) {
        if (predicate.matchesType(primaryType, mixinTypes)) {
            removeNode(workspaceName, key, null, path, primaryType, mixinTypes, queryable);
            addNode(workspaceName, key, path, primaryType, mixinTypes, properties, queryable);
        }
    }

}
