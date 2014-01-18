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

import java.util.List;
import org.hibernate.search.SearchFactory;
import org.modeshape.jcr.query.QueryContext;
import org.modeshape.jcr.query.QueryIndexing;
import org.modeshape.jcr.query.QueryResults.Columns;
import org.modeshape.jcr.query.lucene.LuceneQueryEngine.TupleCollector;
import org.modeshape.jcr.query.model.Constraint;
import org.modeshape.jcr.query.model.SelectorName;

/**
 * A component that is aware of the particular structure and layout of an index design.
 */
public interface LuceneSchema extends QueryIndexing {

    /**
     * Create a {@link LuceneQuery} for the supplied ANDed constraints of the ModeShape access query, which comes from the leaves
     * of a query plan.
     * 
     * @param selectorName the name of the selector (or node type); never null
     * @param andedConstraints the constraints of the access query that are all ANDed together; never null
     * @param context the processing context; never null
     * @return the query that represents the Lucene Query object(s) to be executed; never null
     * @throws LuceneException
     */
    public LuceneQuery createQuery( SelectorName selectorName,
                                    List<Constraint> andedConstraints,
                                    LuceneProcessingContext context ) throws LuceneException;

    public TupleCollector createTupleCollector( QueryContext queryContext,
                                                Columns columns );

    public LuceneQueryFactory createLuceneQueryFactory( QueryContext context,
                                                        SearchFactory searchFactory );

}
