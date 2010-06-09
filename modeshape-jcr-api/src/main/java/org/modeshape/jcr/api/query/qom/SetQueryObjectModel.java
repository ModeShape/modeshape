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
package org.modeshape.jcr.api.query.qom;

import javax.jcr.query.Query;
import javax.jcr.query.qom.QueryObjectModel;
import javax.jcr.query.qom.QueryObjectModelFactory;

/**
 * A set query extension to the JCR query object model.
 * <p>
 * The JCR query object model describes the queries that can be evaluated by a JCR repository independent of any particular query
 * language, such as SQL. JCR defines the {@link QueryObjectModel} interface as the primary representatino of this query object
 * model, but this interface is not sufficient for certain queries, such as unions or intersections of other queries. This
 * interface is an extension to the JCR API that mirrors the QueryObjectModel interface for set queries.
 * </p>
 * <p>
 * A query consists of:
 * <ul>
 * <li>a source. When the query is evaluated, the source evaluates its selectors and the joins between them to produce a (possibly
 * empty) set of node-tuples. This is a set of 1-tuples if the query has one selector (and therefore no joins), a set of 2-tuples
 * if the query has two selectors (and therefore one join), a set of 3-tuples if the query has three selectors (two joins), and so
 * forth.</li>
 * <li>an optional constraint. When the query is evaluated, the constraint filters the set of node-tuples.</li>
 * <li>a list of zero or more orderings. The orderings specify the order in which the node-tuples appear in the query results. The
 * relative order of two node-tuples is determined by evaluating the specified orderings, in list order, until encountering an
 * ordering for which one node-tuple precedes the other. If no orderings are specified, or if for none of the specified orderings
 * does one node-tuple precede the other, then the relative order of the node-tuples is implementation determined (and may be
 * arbitrary).</li>
 * <li>a list of zero or more columns to include in the tabular view of the query results. If no columns are specified, the
 * columns available in the tabular view are implementation determined, but minimally include, for each selector, a column for
 * each single-valued non-residual property of the selector's node type.</li>
 * </ul>
 * <p>
 * The query object model representation of a query is created by factory methods in the {@link QueryObjectModelFactory}.
 * 
 * @see QueryObjectModel
 */
public interface SetQueryObjectModel extends SetQuery, Query {
}
