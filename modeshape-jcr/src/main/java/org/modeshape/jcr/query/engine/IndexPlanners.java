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
import java.util.Map;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.jcr.api.query.qom.NodePath;
import org.modeshape.jcr.api.query.qom.Operator;
import org.modeshape.jcr.query.QueryContext;
import org.modeshape.jcr.query.model.BindVariableName;
import org.modeshape.jcr.query.model.ChildNode;
import org.modeshape.jcr.query.model.Comparison;
import org.modeshape.jcr.query.model.DescendantNode;
import org.modeshape.jcr.query.model.DynamicOperand;
import org.modeshape.jcr.query.model.Literal;
import org.modeshape.jcr.query.model.NodeId;
import org.modeshape.jcr.query.model.PropertyValue;
import org.modeshape.jcr.query.model.SameNode;
import org.modeshape.jcr.query.model.StaticOperand;
import org.modeshape.jcr.spi.index.IndexCostCalculator;
import org.modeshape.jcr.spi.index.provider.IndexPlanner;

/**
 * @author Randall Hauch (rhauch@redhat.com)
 */
public abstract class IndexPlanners {

    /**
     * Examine the supplied constraints applied to the given selector in a query, and record in the supplied
     * {@link IndexCostCalculator} any and all indexes in this provider that can be used in this query.
     *
     * @param context the context in which the query is being executed, provided by ModeShape; never null
     * @param calculator the cost calculator that this method can use to find information about the query and to record
     *        information about the index(es), if any, that the query engine might use to satisfy the relevant portion of the
     *        query; never null
     */
    public abstract void applyIndexes( QueryContext context,
                                       IndexCostCalculator calculator );

    private static final IndexPlanners IMPLICIT = new IndexPlanners() {
        @Override
        public void applyIndexes( QueryContext context,
                                  IndexCostCalculator calculator ) {
            StandardIndexPlanner.INSTANCE.applyIndexes(context, calculator);
        }
    };

    /**
     * Get the IndexPlanners instance that looks only for the implicit (built-in) indexes.
     *
     * @return the instance; never null
     */
    public static IndexPlanners implicit() {
        return IMPLICIT;
    }

    /**
     * Get an IndexPlanners instance that looks for the implicit (built-in) indexes and that calls the appropriate
     * {@link IndexPlanner} instances given the available indexes for this selector.
     *
     * @param plannersByProviderName the map of query index planners keyed by the provider's name
     * @return the instance; never null
     */
    public static IndexPlanners withProviders( final Map<String, IndexPlanner> plannersByProviderName ) {
        return new IndexPlanners() {
            @Override
            public void applyIndexes( QueryContext context,
                                      IndexCostCalculator calculator ) {
                // Call the standard index planner ...
                StandardIndexPlanner.INSTANCE.applyIndexes(context, calculator);

                if (context.getIndexDefinitions().hasIndexDefinitions()) {
                    // Call the index planners ...
                    for (IndexPlanner planner : plannersByProviderName.values()) {
                        planner.applyIndexes(context, calculator);
                    }
                }
            }
        };

    }

    public static final String NODE_BY_PATH_INDEX_NAME = "NodeByPath";
    public static final String NODE_BY_ID_INDEX_NAME = "NodeById";
    public static final String CHILDREN_BY_PATH_INDEX_NAME = "ChildrenByPath";
    public static final String DESCENDANTS_BY_PATH_INDEX_NAME = "DescendantsByPath";
    public static final String PATH_PARAMETER = "path";
    public static final String ID_PARAMETER = "id";

    @Immutable
    private static class StandardIndexPlanner extends IndexPlanner {
        public static final IndexPlanner INSTANCE = new StandardIndexPlanner();

        @Override
        public void applyIndexes( QueryContext context,
                                  IndexCostCalculator calculator ) {
            for (javax.jcr.query.qom.Constraint constraint : calculator.andedConstraints()) {
                if (constraint instanceof SameNode) {
                    SameNode sameNode = (SameNode)constraint;
                    String path = sameNode.getPath();
                    calculator.addIndex(NODE_BY_PATH_INDEX_NAME, null, null, singletonList(constraint), 1, 1L, -1.0f,
                                        PATH_PARAMETER, path);
                } else if (constraint instanceof ChildNode) {
                    ChildNode childNode = (ChildNode)constraint;
                    String path = childNode.getParentPath();
                    calculator.addIndex(CHILDREN_BY_PATH_INDEX_NAME, null, null, singletonList(constraint), 10, 100L, -1.0f,
                                        PATH_PARAMETER, path);
                } else if (constraint instanceof DescendantNode) {
                    DescendantNode descendantNode = (DescendantNode)constraint;
                    String path = descendantNode.getAncestorPath();
                    calculator.addIndex(DESCENDANTS_BY_PATH_INDEX_NAME, null, null, singletonList(constraint), 1000, 10000L,
                                        -1.0f, PATH_PARAMETER, path);
                } else if (constraint instanceof Comparison) {
                    Comparison comparison = (Comparison)constraint;
                    if (comparison.operator() != Operator.EQUAL_TO) return;
                    DynamicOperand leftSide = comparison.getOperand1();
                    if (leftSide instanceof NodePath) {
                        // This is a constraint on the path of a node ...
                        StaticOperand rightSide = comparison.getOperand2();
                        String path = stringValue(rightSide, context);
                        if (path != null) {
                            calculator.addIndex(NODE_BY_PATH_INDEX_NAME, null, null, singletonList(constraint), 1, 1L, -1.0f,
                                                PATH_PARAMETER, path);
                        }
                    }
                    if (leftSide instanceof NodeId) {
                        // This is a constraint on the ID of a node ...
                        StaticOperand rightSide = comparison.getOperand2();
                        String id = stringValue(rightSide, context);
                        if (id != null) {
                            calculator.addIndex(NODE_BY_ID_INDEX_NAME, null, null, singletonList(constraint), 1, 1L, -1.0f,
                                                ID_PARAMETER, id);
                        }
                    }
                    if (leftSide instanceof PropertyValue) {
                        PropertyValue propValue = (PropertyValue)leftSide;
                        if ("jcr:uuid".equals(propValue.getPropertyName()) || "mode:id".equals(propValue.getPropertyName())) {
                            // This is a constraint on the ID of a node ...
                            StaticOperand rightSide = comparison.getOperand2();
                            String id = stringValue(rightSide, context);
                            if (id != null) {
                                calculator.addIndex(NODE_BY_ID_INDEX_NAME, null, null, singletonList(constraint), 1, 1L, -1.0f,
                                                    ID_PARAMETER, id);
                            }
                        } else if ("jcr:path".equals(propValue.getPropertyName())) {
                            // This is a constraint on the PATH of a node ...
                            StaticOperand rightSide = comparison.getOperand2();
                            String path = stringValue(rightSide, context);
                            if (path != null) {
                                calculator.addIndex(NODE_BY_PATH_INDEX_NAME, null, null, singletonList(constraint), 1, 1L, -1.0f,
                                                    PATH_PARAMETER, path);
                            }
                        }
                    }
                }
            }
        }
    }

    protected static String stringValue( StaticOperand operand,
                                         QueryContext context ) {
        // This is a constraint on the ID of a node ...
        Object value = null;
        if (operand instanceof BindVariableName) {
            BindVariableName varName = (BindVariableName)operand;
            value = context.getVariables().get(varName.getBindVariableName());
        } else if (operand instanceof Literal) {
            value = ((Literal)operand).value();
        }
        if (value != null && value instanceof String) {
            return (String)value;
        }
        return null;
    }
}
