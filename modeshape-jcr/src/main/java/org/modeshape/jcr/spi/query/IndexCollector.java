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
package org.modeshape.jcr.spi.query;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.jcr.query.qom.Constraint;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.jcr.query.QueryContext;
import org.modeshape.jcr.query.model.SelectorName;

/**
 * A collector implemented by ModeShape and supplied to the
 * {@link QueryIndexPlanner#applyIndexes(QueryContext, SelectorName, List, IndexCollector)} method so that the
 * {@link QueryIndexPlanner} can add indexes to the query plan.
 * <p>
 * The cardinality estimate is an esimate of the number of nodes that will be returned by this index given the constraints. For
 * example, an index that will return one node should have a cardinality of 1. When possible, the actual cardinality should be
 * used. However, since an accurate number is often expensive or impossible to determine in the planning phase, the cardinality
 * can instead represent a rough order of magnitude.
 * </p>
 * <p>
 * Return an estimate of the cost of using the index for the query in question. An index that is expensive to use will have a
 * higher cost than another index that is less expensive to use. For example, if a {@link QueryIndexProvider} that owns the index
 * is in a remote process, then the cost estimate will need to take into account the cost of transmitting the request with the
 * criteria and the response with all of the node that meet the criteria of the index.
 * </p>
 * <p>
 * Indexes with lower costs and lower cardinalities will be favored over other indexes.
 * </p>
 * 
 * @author Randall Hauch (rhauch@redhat.com)
 */
@NotThreadSafe
public interface IndexCollector {
    /**
     * Add to the query plan the information necessary to signal that the supplied index can be used to answer the query.
     * 
     * @param name the name of the index; may not be null
     * @param providerName the name of the provider; may not be null
     * @param constraints the constraints that should be applied to the index if/when it is used
     * @param costEstimate an estimate of the cost of using the index for the query in question; must be non-negative
     * @param cardinalityEstimate an esimate of the number of nodes that will be returned by this index given the constraints;
     *        must be non-negative
     */
    void addIndex( String name,
                   String providerName,
                   Collection<Constraint> constraints,
                   int costEstimate,
                   long cardinalityEstimate );

    /**
     * Add to the query plan the information necessary to signal that the supplied index can be used to answer the query
     * 
     * @param name the name of the index; may not be null
     * @param providerName the name of the provider; may not be null
     * @param constraints the constraints that should be applied to the index if/when it is used
     * @param costEstimate an estimate of the cost of using the index for the query in question; must be non-negative
     * @param cardinalityEstimate an esimate of the number of nodes that will be returned by this index given the constraints;
     *        must be non-negative
     * @param parameterName the name of a parameter that is to be supplied back to the {@link QueryIndex} if/when this index is
     *        {@link QueryIndex#filter(org.modeshape.jcr.spi.query.QueryIndex.Filter)} called; may not be null
     * @param parameterValue the value of a parameter that is to be supplied back to the {@link QueryIndex} if/when this index is
     *        {@link QueryIndex#filter(org.modeshape.jcr.spi.query.QueryIndex.Filter)} called; may not be null
     */
    void addIndex( String name,
                   String providerName,
                   Collection<Constraint> constraints,
                   int costEstimate,
                   long cardinalityEstimate,
                   String parameterName,
                   Object parameterValue );

    /**
     * Add to the query plan the information necessary to signal that the supplied index can be used to answer the query.
     * 
     * @param name the name of the index; may not be null
     * @param providerName the name of the provider; may not be null
     * @param constraints the constraints that should be applied to the index if/when it is used
     * @param costEstimate an estimate of the cost of using the index for the query in question; must be non-negative
     * @param cardinalityEstimate an esimate of the number of nodes that will be returned by this index given the constraints;
     *        must be non-negative
     * @param parameterName1 the name of the first parameter; may not be null
     * @param parameterValue1 the value of the first parameter
     * @param parameterName2 the name of the second parameter; may not be null
     * @param parameterValue2 the value of the second parameter
     */
    void addIndex( String name,
                   String providerName,
                   Collection<Constraint> constraints,
                   int costEstimate,
                   long cardinalityEstimate,
                   String parameterName1,
                   Object parameterValue1,
                   String parameterName2,
                   Object parameterValue2 );

    /**
     * Add to the query plan the information necessary to signal that the supplied index can be used to answer the query.
     * 
     * @param name the name of the index; may not be null
     * @param providerName the name of the provider; may not be null
     * @param constraints the constraints that should be applied to the index if/when it is used
     * @param costEstimate an estimate of the cost of using the index for the query in question; must be non-negative
     * @param cardinalityEstimate an esimate of the number of nodes that will be returned by this index given the constraints;
     *        must be non-negative
     * @param parameters the parameter values by name; may be null or empty
     */
    void addIndex( String name,
                   String providerName,
                   Collection<Constraint> constraints,
                   int costEstimate,
                   long cardinalityEstimate,
                   Map<String, Object> parameters );
}
