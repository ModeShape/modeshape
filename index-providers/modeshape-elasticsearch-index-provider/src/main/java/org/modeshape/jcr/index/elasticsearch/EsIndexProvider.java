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

import java.util.Collection;
import javax.jcr.RepositoryException;
import javax.jcr.query.qom.ChildNodeJoinCondition;
import javax.jcr.query.qom.DescendantNodeJoinCondition;
import javax.jcr.query.qom.DynamicOperand;
import javax.jcr.query.qom.JoinCondition;
import org.modeshape.common.collection.Problems;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.NodeTypes;
import org.modeshape.jcr.api.index.IndexDefinition;
import org.modeshape.jcr.api.query.qom.ChildCount;
import org.modeshape.jcr.cache.change.ChangeSetAdapter;
import org.modeshape.jcr.index.elasticsearch.client.EsClient;
import org.modeshape.jcr.query.QueryContext;
import org.modeshape.jcr.query.model.FullTextSearch;
import org.modeshape.jcr.query.model.Or;
import org.modeshape.jcr.spi.index.IndexCostCalculator;
import org.modeshape.jcr.spi.index.provider.IndexProvider;
import org.modeshape.jcr.spi.index.provider.IndexUsage;
import org.modeshape.jcr.spi.index.provider.ManagedIndexBuilder;

/**
 *
 * @author kulikov
 */
public class EsIndexProvider extends IndexProvider {

    private String host = "localhost";
    private int port = 9200;
    private EsClient client;
    
    @Override
    protected void doInitialize() throws RepositoryException {
        logger().debug("Elasticsearch index provider for repository '{1}' "
                    + "is trying to connect to cluster", getRepositoryName());
        client = new EsClient(host, port);
    }

    /**
     * Gets ES instance address.
     * 
     * @return ip address or domain name.
     */
    public String getHost() {
        return host;
    }
    
    /**
     * Assigns ES instance address.
     * 
     * @param host ip address or domain name.
     */
    public void setHost(String host) {
        this.host = host;
    }
    
    /**
     * Gets ES instance port number.
     * 
     * @return port number
     */
    public int getPort() {
        return port;
    }
    
    /**
     * Gets ES instance port number.
     * 
     * @return port number
     */
    public void setPort(int port) {
        this.port = port;
    }
    
    @Override
    protected void postShutdown() {
        logger().debug("Shutting down the elasticsearch index provider '{0}' in repository '{1}'", getName(), getRepositoryName());
    }

    @Override
    protected int getCostEstimate() {
        return IndexCostCalculator.Costs.REMOTE;
    }

    @Override
    public void validateProposedIndex(ExecutionContext context,
            IndexDefinition defn,
            NodeTypes.Supplier nodeTypeSupplier,
            Problems problems) {
        // first perform some custom validations
    }

    @Override
    protected ManagedIndexBuilder getIndexBuilder(IndexDefinition defn,
            String workspaceName,
            NodeTypes.Supplier nodeTypesSupplier,
            ChangeSetAdapter.NodeTypePredicate matcher) {
        return EsManagedIndexBuilder.create(client, context(), defn, nodeTypesSupplier, workspaceName, matcher);
    }

    @Override
    protected IndexUsage evaluateUsage(QueryContext context, final IndexCostCalculator calculator, final IndexDefinition defn) {
        return new IndexUsage(context, calculator, defn) {
            @Override
            protected boolean applies(ChildCount operand) {
                // nothing to do about this...
                return false;
            }

            @Override
            protected boolean applies(DynamicOperand operand) {
                if (IndexDefinition.IndexKind.TEXT == defn.getKind() && !(operand instanceof FullTextSearch)) {
                    // text indexes only support FTS operands...
                    return false;
                }
                return super.applies(operand);
            }

            @Override
            protected boolean indexAppliesTo(Or or) {
                boolean appliesToConstraints = super.indexAppliesTo(or);
                if (!appliesToConstraints) {
                    return false;
                }
                Collection<JoinCondition> joinConditions = calculator.joinConditions();
                if (joinConditions.isEmpty()) {
                    return true;
                }
                for (JoinCondition joinCondition : joinConditions) {
                    if (joinCondition instanceof ChildNodeJoinCondition || joinCondition instanceof DescendantNodeJoinCondition) {
                        // the index can't handle OUTER JOINS with OR criteria (see https://issues.jboss.org/browse/MODE-2054)
                        // so reject it, making the query engine fallback to the default behavior which works
                        return false;
                    }
                }
                return true;
            }
        };
    }
}
