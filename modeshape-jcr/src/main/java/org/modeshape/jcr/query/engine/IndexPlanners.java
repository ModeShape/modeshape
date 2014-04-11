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
import java.util.List;
import java.util.Map;
import javax.jcr.query.qom.Constraint;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.jcr.RepositoryIndexes;
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

/**
 * @author Randall Hauch (rhauch@redhat.com)
 */
public abstract class IndexPlanners {

    /**
     * Examine the supplied constraints applied to the given selector in a query, and record in the supplied
     * {@link IndexCollector} any and all indexes in this provider that can be used in this query.
     * 
     * @param context the context in which the query is being executed, provided by ModeShape; never null
     * @param selector the name of the selector against which all of the {@code andedConstraints} are to be applied; never null
     * @param andedConstraints the immutable list of {@link Constraint} instances that are all AND-ed and applied against the
     *        {@code selector}; never null but possibly empty
     * @param indexDefinitions the available index definitions that apply to the node type identified by the named selector; may
     *        be null if there are no indexes defined
     * @param indexes the list provided by the caller into which this method should add the index(es), if any, that the query
     *        engine might use to satisfy the relevant portion of the query; never null
     */
    public abstract void applyIndexes( QueryContext context,
                                       SelectorName selector,
                                       List<Constraint> andedConstraints,
                                       RepositoryIndexes indexDefinitions,
                                       IndexCollector indexes );

    private static final IndexPlanners IMPLICIT = new IndexPlanners() {
        @Override
        public void applyIndexes( QueryContext context,
                                  SelectorName selector,
                                  List<Constraint> andedConstraints,
                                  RepositoryIndexes indexDefinitions,
                                  IndexCollector indexes ) {
            StandardIndexPlanner.INSTANCE.applyIndexes(context, selector, andedConstraints, null, indexes);
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
                                      SelectorName selector,
                                      List<Constraint> andedConstraints,
                                      RepositoryIndexes indexDefinitions,
                                      IndexCollector indexes ) {
                // Call the standard index planner ...
                StandardIndexPlanner.INSTANCE.applyIndexes(context, selector, andedConstraints, null, indexes);

                if (indexDefinitions != null) {
                    // Only call the planners for providers that own at least one of the indexes ...
                    for (Map.Entry<String, IndexPlanner> entry : plannersByProviderName.entrySet()) {
                        String providerName = entry.getKey();
                        Iterable<IndexDefinition> indexDefns = indexDefinitions.indexesFor(selector.name(), providerName);
                        if (indexDefns != null) {
                            IndexPlanner planner = entry.getValue();
                            planner.applyIndexes(context, selector, andedConstraints, indexDefns, indexes);
                        }
                    }
                }
            }
        };

    }

    protected static final String NODE_BY_PATH_INDEX_NAME = "NodeByPath";
    protected static final String CHILDREN_BY_PATH_INDEX_NAME = "ChildrenByPath";
    protected static final String DESCENDANTS_BY_PATH_INDEX_NAME = "DescendantsByPath";
    protected static final String PATH_PARAMETER = "path";

    @Immutable
    private static class StandardIndexPlanner extends IndexPlanner {
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
