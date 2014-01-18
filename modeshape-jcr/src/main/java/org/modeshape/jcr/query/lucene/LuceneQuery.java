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
package org.modeshape.jcr.query.lucene;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.jcr.query.model.And;
import org.modeshape.jcr.query.model.Constraint;

/**
 * A utility class that represents the Lucene Query object (or objects) that are to be executed.
 */
@NotThreadSafe
public class LuceneQuery {
    private Query pushDownQuery;
    private Constraint postProcessingConstraints;
    private boolean matchNone = false;
    private final String pushDownIndexName;

    public LuceneQuery( String pushDownIndexName ) {
        this.pushDownIndexName = pushDownIndexName;
    }

    /**
     * Add a Lucene query for one of the ANDed constraints.
     * 
     * @param query the Lucene query; never null
     */
    public void addQuery( Query query ) {
        if (matchNone) {
            // We've seen a constraint where nothing can be matched (with any ANDed constraint) ...
            return;
        }

        if (query instanceof MatchNoneQuery) {
            // This query invalidates all queries (since they're all anded) ...
            this.pushDownQuery = null;
            this.matchNone = true;
            return;
        }

        if (pushDownQuery == null) {
            // This is the first query we've seen, so just record it ...
            pushDownQuery = query;
            return;
        }

        // We can simply AND these Lucene queries together, since they're going to the same index ...
        BooleanQuery booleanQuery = null;
        assert this.pushDownQuery != null;
        if (pushDownQuery instanceof BooleanQuery) {
            // The existing pushdown query is already a BooleanQuery, so try to merge ...
            booleanQuery = (BooleanQuery)pushDownQuery;
            boolean canMerge = true;
            for (BooleanClause clause : booleanQuery.getClauses()) {
                if (clause.getOccur() == BooleanClause.Occur.SHOULD) {
                    canMerge = false;
                    break;
                }
            }
            if (canMerge) {
                // The boolean query has all MUST occurs, so we can just add another one ...
                booleanQuery.add(query, Occur.MUST);
                return;
            }
        }
        // The existing pushdown query is not yet a BooleanQuery, so we need to wrap it ...
        booleanQuery = new BooleanQuery();
        booleanQuery.add(this.pushDownQuery, Occur.MUST);

        // If the new query is a BooleanQuery, then it is probably a 'NOT(query)'
        if (query instanceof BooleanQuery) {
            // See if the query can be merged ...
            boolean merged = false;
            BooleanQuery booleanSecond = (BooleanQuery)query;
            if (booleanSecond.getClauses().length == 1) {
                BooleanClause onlyClause = booleanSecond.getClauses()[0];
                if (onlyClause.isProhibited()) {
                    booleanQuery.add(onlyClause.getQuery(), Occur.MUST_NOT);
                    merged = true;
                } else if (onlyClause.isRequired()) {
                    booleanQuery.add(onlyClause.getQuery(), Occur.MUST);
                    merged = true;
                }
            }
            if (!merged) {
                booleanQuery.add(query, Occur.MUST);
            }
        } else {
            // Just add the query to our boolean query ...
            booleanQuery.add(query, Occur.MUST);
        }

        // And re-assign the pushdown query to be the new BooleanQuery that wraps the old pushdown and 'query' ...
        this.pushDownQuery = booleanQuery;
    }

    /**
     * Record one of the ANDed constraints could not be converted into a Lucene query and thus will need to be handled after the
     * Lucene processing is done.
     * 
     * @param constraint
     */
    public void addConstraintForPostprocessing( Constraint constraint ) {
        postProcessingConstraints = postProcessingConstraints == null ? constraint : new And(postProcessingConstraints,
                                                                                             constraint);
    }

    /**
     * Return whether the {@link #getPushDownQuery() pushdown query} will always return no results.
     * 
     * @return true if the queries will always return no results, or false if there are {@link #getPushDownQuery} to run.
     */
    public boolean matchesNone() {
        return matchNone;
    }

    /**
     * Get the Lucene query that should be executed.
     * 
     * @return the queries for each index; never null but possibly empty if there are no Lucene queries to execute (perhaps
     *         because the criteria resulted in queries that always returned no results)
     */
    public Query getPushDownQuery() {
        assert matchNone ? pushDownQuery == null : true;
        return pushDownQuery;
    }

    /**
     * Get the index against which the {@link #getPushDownQuery() push-down query} should be executed.
     * 
     * @return the name of the index for the push-down query; never null
     */
    public String getPushDownIndexName() {
        return pushDownIndexName;
    }

    /**
     * Get the ANDed-constraints that could not be pushed down to Lucene.
     * 
     * @return the constraints that could not be pushed down, or null if all constraints could be transformed into
     *         {@link #getPushDownQuery() Lucene queries}
     */
    public Constraint getPostProcessingConstraints() {
        return postProcessingConstraints;
    }

    @Override
    public String toString() {
        return pushDownQuery.toString()
               + (postProcessingConstraints != null ? (" w/ postprocessing:" + postProcessingConstraints) : "");
    }
}
