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

package org.modeshape.jcr.query.engine;

import static java.util.Collections.singletonList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.jcr.query.qom.Constraint;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.api.query.qom.NodePath;
import org.modeshape.jcr.api.query.qom.Operator;
import org.modeshape.jcr.query.QueryContext;
import org.modeshape.jcr.query.model.BindVariableName;
import org.modeshape.jcr.query.model.ChildNode;
import org.modeshape.jcr.query.model.Comparison;
import org.modeshape.jcr.query.model.DescendantNode;
import org.modeshape.jcr.query.model.DynamicOperand;
import org.modeshape.jcr.query.model.Literal;
import org.modeshape.jcr.query.model.SameNode;
import org.modeshape.jcr.query.model.SelectorName;
import org.modeshape.jcr.query.model.StaticOperand;
import org.modeshape.jcr.spi.index.IndexCollector;
import org.modeshape.jcr.spi.index.IndexDefinition;
import org.modeshape.jcr.spi.index.provider.IndexPlanner;
import org.modeshape.jcr.spi.index.provider.IndexProvider;

@Immutable
public final class IndexPlan implements Comparable<IndexPlan> {

    private static final Map<String, Object> NO_PARAMETERS = Collections.emptyMap();

    private final String name;
    private final String providerName;
    private final int costEstimate;
    private final long cardinalityEstimate;
    private final Collection<Constraint> constraints;
    private final Map<String, Object> parameters;

    public IndexPlan( String name,
                      String providerName,
                      Collection<Constraint> constraints,
                      int costEstimate,
                      long cardinalityEstimate,
                      Map<String, Object> parameters ) {
        CheckArg.isNotEmpty(name, "name");
        CheckArg.isNonNegative(costEstimate, "costEstimate");
        CheckArg.isNonNegative(cardinalityEstimate, "cardinalityEstimate");
        this.name = name;
        this.providerName = providerName; // may be null or empty
        this.constraints = constraints;
        this.costEstimate = costEstimate;
        this.cardinalityEstimate = cardinalityEstimate;
        this.parameters = parameters == null ? NO_PARAMETERS : parameters;
    }

    /**
     * Return an esimate of the number of nodes that will be returned by this index given the constraints. For example, an index
     * that will return one node should have a cardinality of 1.
     * <p>
     * When possible, the actual cardinality should be used. However, since an accurate number is often expensive or impossible to
     * determine in the planning phase, the cardinality can instead represent a rough order of magnitude.
     * </p>
     * <p>
     * Indexes with lower costs and lower {@link #getCardinalityEstimate() cardinalities} will be favored over other indexes.
     * </p>
     * 
     * @return the cardinality estimate; never negative
     */
    public long getCardinalityEstimate() {
        return cardinalityEstimate;
    }

    /**
     * Return an estimate of the cost of using the index for the query in question. An index that is expensive to use will have a
     * higher cost than another index that is less expensive to use. For example, if a {@link IndexProvider} that owns the
     * index is in a remote process, then the cost estimate will need to take into account the cost of transmitting the request
     * with the criteria and the response with all of the node that meet the criteria of the index.
     * <p>
     * Indexes with lower costs and lower {@link #getCardinalityEstimate() cardinalities} will be favored over other indexes.
     * </p>
     * 
     * @return the cost estimate; never negative
     */
    public int getCostEstimate() {
        return costEstimate;
    }

    /**
     * Get the name of this index.
     * 
     * @return the index name; never null
     */
    public String getName() {
        return name;
    }

    /**
     * The name of the provider that owns the index.
     * 
     * @return the provider name; null if the index is handled internally by ModeShape by something other than a provider
     */
    public String getProviderName() {
        return providerName;
    }

    /**
     * Get the constraints that should be applied to this index if/when it is used.
     * 
     * @return the constraints; may be null or empty if there are no constraints
     */
    public Collection<Constraint> getConstraints() {
        return constraints;
    }

    /**
     * Get the provider-specific parameters for this index usage.
     * 
     * @return the parameters; never null but possibly empty
     */
    public Map<String, Object> getParameters() {
        return parameters;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName());
        sb.append(" cost=").append(getCostEstimate());
        sb.append(", cardinality=").append(getCardinalityEstimate());
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            sb.append(", ").append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }

    @Override
    public int compareTo( IndexPlan that ) {
        if (that == this) return 0;
        if (that == null) return 1;
        return this.getCostEstimate() - that.costEstimate;
    }

    protected static final String NODE_BY_PATH_INDEX_NAME = "NodeByPath";
    protected static final String CHILDREN_BY_PATH_INDEX_NAME = "ChildrenByPath";
    protected static final String DESCENDANTS_BY_PATH_INDEX_NAME = "DescendantsByPath";
    protected static final String PATH_PARAMETER = "path";

    @Immutable
    public static class StandardIndexPlanner extends IndexPlanner {
        public static final IndexPlanner INSTANCE = new StandardIndexPlanner();

        @Override
        public void applyIndexes( QueryContext context,
                                  SelectorName selector,
                                  List<javax.jcr.query.qom.Constraint> andedConstraints,
                                  Iterable<IndexDefinition> indexesOnSelector,
                                  IndexCollector indexes ) {
            for (javax.jcr.query.qom.Constraint constraint : andedConstraints) {
                if (constraint instanceof SameNode) {
                    SameNode sameNode = (SameNode)constraint;
                    String path = sameNode.getPath();
                    indexes.addIndex(NODE_BY_PATH_INDEX_NAME, null, singletonList(constraint), 1, 1L, PATH_PARAMETER, path);
                } else if (constraint instanceof ChildNode) {
                    ChildNode childNode = (ChildNode)constraint;
                    String path = childNode.getParentPath();
                    indexes.addIndex(CHILDREN_BY_PATH_INDEX_NAME, null, singletonList(constraint), 10, 100L, PATH_PARAMETER, path);
                } else if (constraint instanceof DescendantNode) {
                    DescendantNode descendantNode = (DescendantNode)constraint;
                    String path = descendantNode.getAncestorPath();
                    indexes.addIndex(DESCENDANTS_BY_PATH_INDEX_NAME, null, singletonList(constraint), 1000, 10000L,
                                     PATH_PARAMETER, path);
                } else if (constraint instanceof Comparison) {
                    Comparison comparison = (Comparison)constraint;
                    if (comparison.operator() != Operator.EQUAL_TO) return;
                    DynamicOperand leftSide = comparison.getOperand1();
                    if (leftSide instanceof NodePath) {
                        // This is a constraint on the path of a node ...
                        StaticOperand rightSide = comparison.getOperand2();
                        Object value = null;
                        if (rightSide instanceof BindVariableName) {
                            BindVariableName varName = (BindVariableName)rightSide;
                            value = context.getVariables().get(varName.getBindVariableName());
                        } else if (rightSide instanceof Literal) {
                            value = ((Literal)rightSide).value();
                        }
                        if (value == null) return;
                        String path = null;
                        if (value instanceof String) {
                            path = (String)value;
                        }
                        if (path != null) {
                            indexes.addIndex(NODE_BY_PATH_INDEX_NAME, null, singletonList(constraint), 1, 1L, PATH_PARAMETER,
                                             path);
                        }
                    }
                }
            }
        }
    }

}
