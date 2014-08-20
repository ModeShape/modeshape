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

import org.modeshape.jcr.query.QueryContext;
import org.modeshape.jcr.spi.index.IndexCostCalculator;

/**
 * A provider-specific component obtained by ModeShape and then used in the planning and optimization phases of each query to
 * identify which provider-specific indexes, if any, can be used in the processing phase to most-efficiently obtain the set of
 * nodes that satisfies the given criteria.
 *
 * @see IndexProvider#getIndexPlanner()
 * @author Randall Hauch (rhauch@redhat.com)
 */
public abstract class IndexPlanner {

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

    /**
     * Utility that returns an {@link IndexPlanner} implementation that delegates to the first planner and then to the second
     * planner. If only one of the supplied planner instances is not null, then this method will simply return the non-null
     * planner.
     *
     * @param planner1 the first planner
     * @param planner2 the second planner
     * @return the composite planner, or null when both {@code planner1} and {@code planner2} are null
     */
    public static IndexPlanner both( final IndexPlanner planner1,
                                     final IndexPlanner planner2 ) {
        if (planner1 == null) return planner2;
        if (planner2 == null) return planner1;
        return new IndexPlanner() {

            @Override
            public void applyIndexes( QueryContext context,
                                      IndexCostCalculator calculator ) {
                RuntimeException error = null;
                try {
                    planner1.applyIndexes(context, calculator);
                } catch (RuntimeException e) {
                    error = e;
                } finally {
                    try {
                        planner2.applyIndexes(context, calculator);
                    } catch (RuntimeException e) {
                        if (error == null) error = e;
                    } finally {
                        if (error != null) throw error;
                    }
                }
            }
        };
    }

}
