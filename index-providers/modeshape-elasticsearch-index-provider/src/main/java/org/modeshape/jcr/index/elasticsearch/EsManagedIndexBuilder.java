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
package org.modeshape.jcr.index.elasticsearch;

import org.modeshape.common.collection.Problems;
import org.modeshape.common.collection.SimpleProblems;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.NodeTypes;
import org.modeshape.jcr.api.index.IndexDefinition;
import org.modeshape.jcr.cache.change.ChangeSetAdapter;
import org.modeshape.jcr.index.elasticsearch.client.EsClient;
import org.modeshape.jcr.index.local.LocalIndexException;
import org.modeshape.jcr.spi.index.provider.ManagedIndexBuilder;
import org.modeshape.jcr.spi.index.provider.ProvidedIndex;

/**
 * Index builder for Elasticsearch indexes.
 * 
 * @author kulikov
 */
public class EsManagedIndexBuilder extends ManagedIndexBuilder {

    private final EsClient client;
    
    /**
     * Create a builder for the supplied index definition.
     *
     * @param client interface for elasticsearch cluster.
     * @param context the execution context in which the index should operate;
     * may not be null
     * @param defn the index definition; may not be null
     * @param nodeTypesSupplier the supplier of the {@link NodeTypes} instance;
     * may not be null
     * @param workspaceName the name of the workspace for which to build the
     * index; may not be null
     * @param matcher the node type matcher used to determine which nodes should
     * be included in the index; may not be null
     * @return the index builder; never null
     */
    public static EsManagedIndexBuilder create(EsClient client, ExecutionContext context,
            IndexDefinition defn,
            NodeTypes.Supplier nodeTypesSupplier,
            String workspaceName,
            ChangeSetAdapter.NodeTypePredicate matcher) {
        SimpleProblems problems = new SimpleProblems();
        validate(defn, problems);
        if (problems.hasErrors()) {
            throw new LocalIndexException(problems.toString());
        }
        return new EsManagedIndexBuilder(client, context, defn, nodeTypesSupplier, workspaceName, matcher);
    }

    /**
     * Validate whether the index definition is acceptable for this provider.
     *
     * @param defn the definition to validate; may not be {@code null}
     * @param problems the component to record any problems, errors, or
     * warnings; may not be null
     */
    protected static void validate(IndexDefinition defn, Problems problems) {
    }

    protected EsManagedIndexBuilder(EsClient client, ExecutionContext context,
            IndexDefinition defn,
            NodeTypes.Supplier nodeTypesSupplier,
            String workspaceName,
            ChangeSetAdapter.NodeTypePredicate matcher) {
        super(context, defn, workspaceName, nodeTypesSupplier, matcher);
        this.client = client;
    }

    @Override
    protected ProvidedIndex<?> buildMultiValueIndex(ExecutionContext context, IndexDefinition defn, String workspaceName, NodeTypes.Supplier nodeTypesSupplier, ChangeSetAdapter.NodeTypePredicate matcher) {
        return new EsIndex(client, context, defn, workspaceName);
    }

    @Override
    protected ProvidedIndex<?> buildUniqueValueIndex(ExecutionContext context, IndexDefinition defn, String workspaceName, NodeTypes.Supplier nodeTypesSupplier, ChangeSetAdapter.NodeTypePredicate matcher) {
        return new EsIndex(client, context, defn, workspaceName);
    }

    @Override
    protected ProvidedIndex<?> buildEnumeratedIndex(ExecutionContext context, IndexDefinition defn, String workspaceName, NodeTypes.Supplier nodeTypesSupplier, ChangeSetAdapter.NodeTypePredicate matcher) {
        return new EsIndex(client, context, defn, workspaceName);
    }

    @Override
    protected ProvidedIndex<?> buildTextIndex(ExecutionContext context, IndexDefinition defn, String workspaceName, NodeTypes.Supplier nodeTypesSupplier, ChangeSetAdapter.NodeTypePredicate matcher) {
        return new EsIndex(client, context, defn, workspaceName);
    }

    @Override
    protected ProvidedIndex<?> buildNodeTypeIndex(ExecutionContext context, IndexDefinition defn, String workspaceName, NodeTypes.Supplier nodeTypesSupplier, ChangeSetAdapter.NodeTypePredicate matcher) {
        return new EsIndex(client, context, defn, workspaceName);
    }
}
