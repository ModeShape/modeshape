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
package org.modeshape.jcr.spi.index;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.JoinCondition;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.jcr.query.model.BindVariableName;
import org.modeshape.jcr.spi.index.provider.IndexPlanner;
import org.modeshape.jcr.spi.index.provider.IndexProvider;

/**
 * A collector implemented by ModeShape and supplied to the {@link IndexPlanner#applyIndexes} method so that the
 * {@link IndexPlanner} can add indexes to the query plan.
 * <p>
 * The cardinality estimate is an estimate of the number of nodes that will be returned by this index given the constraints. For
 * example, an index that will return one node should have a cardinality of 1. When possible, the actual cardinality should be
 * used. However, since an accurate number is often expensive or impossible to determine in the planning phase, the cardinality
 * can instead represent a rough order of magnitude.
 * </p>
 * <p>
 * The cost estimate is a measure of the expense of this index for the query in question. An index that is expensive to use will
 * have a higher cost than another index that is less expensive to use. For example, if a {@link IndexProvider} that owns the
 * index is in a remote process, then the cost estimate will need to take into account the cost of transmitting the request with
 * the criteria and the response with all of the node that meet the criteria of the index.
 * </p>
 * <p>
 * Indexes with lower costs and lower cardinalities will be favored over other indexes.
 * </p>
 *
 * @author Randall Hauch (rhauch@redhat.com)
 */
@NotThreadSafe
public interface IndexCostCalculator {

    /**
     * A value representing the maximum selectivity of an index
     */
    Float MAX_SELECTIVITY = 1.0f;

    public static final class Costs {
        public static final int LOCAL = 100;
        public static final int REMOTE = 10000;
    }

    /**
     * Get the name of the node type that the query is selecting, including aliases.
     *
     * @return the node type names; never null
     */
    Set<String> selectedNodeTypes();

    /**
     * Get the ANDed constraints that apply to the index to which this filter is submitted.
     *
     * @return the constraints; never null but maybe empty
     */
    Collection<Constraint> andedConstraints();

    /**
     * Get the join conditions that might apply to the index to which this filter is submitted.
     *
     * @return the join constraints; never null but maybe empty
     */
    Collection<JoinCondition> joinConditions();

    /**
     * Get the variables that are to be substituted into the {@link BindVariableName} used in the query.
     *
     * @return immutable map of variable values keyed by their name; never null but possibly empty
     */
    Map<String, Object> getVariables();

    /**
     * Add to the query plan the information necessary to signal that the supplied index can be used to answer the query.
     *
     * @param name the name of the index; may not be null
     * @param workspaceName the name of the workspace for which the index is used; may be null if the index is built-in
     * @param providerName the name of the provider; may be null if the index is built-in
     * @param joinConditions the join conditions that should be applied to the index if/when it is used
     * @param costEstimate an estimate of the cost of using the index for the query in question; must be non-negative
     * @param cardinalityEstimate an estimate of the number of nodes that will be returned by this index, which for join
     *        constraints is generally equal to the total number of nodes known to the index; must be non-negative
     */
    void addIndex( String name,
                   String workspaceName,
                   String providerName,
                   Collection<JoinCondition> joinConditions,
                   int costEstimate,
                   long cardinalityEstimate );

    /**
     * Add to the query plan the information necessary to signal that the supplied index can be used to answer the query.
     *
     * @param name the name of the index; may not be null
     * @param workspaceName the name of the workspace for which the index is used; may be null if the index is built-in
     * @param providerName the name of the provider; may be null if the index is built-in
     * @param constraints the constraints that should be applied to the index if/when it is used
     * @param costEstimate an estimate of the cost of using the index for the query in question; must be non-negative
     * @param cardinalityEstimate an estimate of the number of nodes that will be returned by this index given the constraints;
     *        must be non-negative
     * @param selectivityEstimate an estimate of the number of rows that are selected by the constraints divided by the total
     *        number rows; must be >= 0 and <= 1.0, or null if the total number of nodes is not known
     */
    void addIndex( String name,
                   String workspaceName,
                   String providerName,
                   Collection<Constraint> constraints,
                   int costEstimate,
                   long cardinalityEstimate,
                   Float selectivityEstimate );

    /**
     * Add to the query plan the information necessary to signal that the supplied index can be used to answer the query
     *
     * @param name the name of the index; may not be null
     * @param workspaceName the name of the workspace for which the index is used; may be null if the index is built-in
     * @param providerName the name of the provider; may be null if the index is built-in
     * @param constraints the constraints that should be applied to the index if/when it is used
     * @param costEstimate an estimate of the cost of using the index for the query in question; must be non-negative
     * @param cardinalityEstimate an estimate of the number of nodes that will be returned by this index given the constraints;
     *        must be non-negative
     * @param selectivityEstimate an estimate of the number of rows that are selected by the constraints divided by the total
     *        number rows; must be >= 0 and <= 1.0, or null if the total number of nodes is not known
     * @param parameterName the name of a parameter that is to be supplied back to the {@link Index} if/when this index is
     *        {@link Index#filter} called; may not be null
     * @param parameterValue the value of a parameter that is to be supplied back to the {@link Index} if/when this index is
     *        {@link Index#filter} called; may not be null
     */
    void addIndex( String name,
                   String workspaceName,
                   String providerName,
                   Collection<Constraint> constraints,
                   int costEstimate,
                   long cardinalityEstimate,
                   Float selectivityEstimate,
                   String parameterName,
                   Object parameterValue );

    /**
     * Add to the query plan the information necessary to signal that the supplied index can be used to answer the query.
     *
     * @param name the name of the index; may not be null
     * @param workspaceName the name of the workspace for which the index is used; may be null if the index is built-in
     * @param providerName the name of the provider; may be null if the index is built-in
     * @param constraints the constraints that should be applied to the index if/when it is used
     * @param costEstimate an estimate of the cost of using the index for the query in question; must be non-negative
     * @param cardinalityEstimate an estimate of the number of nodes that will be returned by this index given the constraints;
     *        must be non-negative
     * @param selectivityEstimate an estimate of the number of rows that are selected by the constraints divided by the total
     *        number rows; must be >= 0 and <= 1.0, or null if the total number of nodes is not known
     * @param parameterName1 the name of the first parameter; may not be null
     * @param parameterValue1 the value of the first parameter
     * @param parameterName2 the name of the second parameter; may not be null
     * @param parameterValue2 the value of the second parameter
     */
    void addIndex( String name,
                   String workspaceName,
                   String providerName,
                   Collection<Constraint> constraints,
                   int costEstimate,
                   long cardinalityEstimate,
                   Float selectivityEstimate,
                   String parameterName1,
                   Object parameterValue1,
                   String parameterName2,
                   Object parameterValue2 );

    /**
     * Add to the query plan the information necessary to signal that the supplied index can be used to answer the query.
     *
     * @param name the name of the index; may not be null
     * @param workspaceName the name of the workspace for which the index is used; may be null if the index is built-in
     * @param providerName the name of the provider; may be null if the index is built-in
     * @param constraints the constraints that should be applied to the index if/when it is used
     * @param costEstimate an estimate of the cost of using the index for the query in question; must be non-negative
     * @param cardinalityEstimate an estimate of the number of nodes that will be returned by this index given the constraints;
     *        must be non-negative
     * @param selectivityEstimate an estimate of the number of rows that are selected by the constraints divided by the total
     *        number rows; must be >= 0 and <= 1.0, or null if the total number of nodes is not known
     * @param parameters the parameter values by name; may be null or empty
     */
    void addIndex( String name,
                   String workspaceName,
                   String providerName,
                   Collection<Constraint> constraints,
                   int costEstimate,
                   long cardinalityEstimate,
                   Float selectivityEstimate,
                   Map<String, Object> parameters );
}
