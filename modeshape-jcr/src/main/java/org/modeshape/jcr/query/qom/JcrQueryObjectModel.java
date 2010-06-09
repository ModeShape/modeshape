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
package org.modeshape.jcr.query.qom;

import javax.jcr.query.Query;
import javax.jcr.query.qom.Column;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.Ordering;
import javax.jcr.query.qom.QueryObjectModel;
import javax.jcr.query.qom.Source;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.query.model.QueryCommand;
import org.modeshape.graph.query.parse.QueryParser;
import org.modeshape.graph.query.plan.PlanHints;
import org.modeshape.jcr.query.JcrQuery;
import org.modeshape.jcr.query.JcrQueryContext;

/**
 * Implementation of {@link QueryObjectModel} that represents a {@link JcrSelectQuery select query}.
 */
public class JcrQueryObjectModel extends JcrQuery implements QueryObjectModel {

    /**
     * Creates a new JCR {@link Query} by specifying the query statement itself, the language in which the query is stated, the
     * {@link QueryCommand} representation and, optionally, the node from which the query was loaded. The language must be a
     * string from among those returned by {@code QueryManager#getSupportedQueryLanguages()}.
     * 
     * @param context the context that was used to create this query and that will be used to execute this query; may not be null
     * @param statement the original statement as supplied by the client; may not be null
     * @param language the language obtained from the {@link QueryParser}; may not be null
     * @param query the parsed query representation; may not be null
     * @param hints any hints that are to be used; may be null if there are no hints
     * @param storedAtPath the path at which this query was stored, or null if this is not a stored query
     */
    public JcrQueryObjectModel( JcrQueryContext context,
                                String statement,
                                String language,
                                JcrSelectQuery query,
                                PlanHints hints,
                                Path storedAtPath ) {
        super(context, statement, language, query, hints, storedAtPath);

    }

    @Override
    protected JcrSelectQuery query() {
        return (JcrSelectQuery)super.query();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.QueryObjectModel#getSource()
     */
    @Override
    public Source getSource() {
        return query().source();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.QueryObjectModel#getConstraint()
     */
    @Override
    public Constraint getConstraint() {
        return query().constraint();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.QueryObjectModel#getColumns()
     */
    @Override
    public Column[] getColumns() {
        return query().getColumns();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.QueryObjectModel#getOrderings()
     */
    @Override
    public Ordering[] getOrderings() {
        return query().getOrderings();
    }
}
