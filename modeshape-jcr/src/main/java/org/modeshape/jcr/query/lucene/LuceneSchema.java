/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
