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
import org.modeshape.jcr.query.QueryContext;
import org.modeshape.jcr.query.QueryIndexing;
import org.modeshape.jcr.query.QueryResults.Columns;
import org.modeshape.jcr.query.lucene.LuceneQueryEngine.TupleCollector;
import org.modeshape.jcr.query.model.Constraint;

/**
 * A component that is aware of the particular structure and layout of an index design.
 */
public interface LuceneSchema extends QueryIndexing {

    public LuceneQuery createQuery( List<Constraint> andedConstraints,
                                    LuceneProcessingContext context ) throws LuceneException;

    public TupleCollector createTupleCollector( QueryContext queryContext,
                                                Columns columns );

}
