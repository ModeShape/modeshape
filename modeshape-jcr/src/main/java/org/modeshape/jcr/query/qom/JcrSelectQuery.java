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

import java.util.List;
import javax.jcr.query.qom.Column;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.Ordering;
import javax.jcr.query.qom.Source;
import org.modeshape.graph.query.model.Query;
import org.modeshape.jcr.api.query.qom.Limit;

/**
 * 
 */
public class JcrSelectQuery extends Query implements org.modeshape.jcr.api.query.qom.SelectQuery, JcrQueryCommand {
    private static final long serialVersionUID = 1L;

    /**
     * Create a new query that uses the supplied source, constraint, orderings, columns and limits.
     * 
     * @param source the source
     * @param constraint the constraint (or composite constraint), or null or empty if there are no constraints
     * @param orderings the specifications of how the results are to be ordered, or null if the order is to be implementation
     *        determined
     * @param columns the columns to be included in the results, or null or empty if there are no explicit columns and the actual
     *        result columns are to be implementation determiend
     * @param limit the limit for the results, or null if all of the results are to be included
     * @param isDistinct true if duplicates are to be removed from the results
     * @throws IllegalArgumentException if the source is null
     */
    public JcrSelectQuery( JcrSource source,
                           JcrConstraint constraint,
                           List<? extends JcrOrdering> orderings,
                           List<? extends JcrColumn> columns,
                           JcrLimit limit,
                           boolean isDistinct ) {
        super(source, constraint, orderings, columns, limit != null ? limit : JcrLimit.NONE, isDistinct);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Query#limits()
     */
    @Override
    public JcrLimit limits() {
        return (JcrLimit)super.limits();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Query#constraint()
     */
    @Override
    public JcrConstraint constraint() {
        return (JcrConstraint)super.constraint();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Query#source()
     */
    @Override
    public JcrSource source() {
        return (JcrSource)super.source();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Query#columns()
     */
    @SuppressWarnings( "unchecked" )
    @Override
    public List<? extends JcrColumn> columns() {
        return (List<? extends JcrColumn>)super.columns();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Query#orderings()
     */
    @SuppressWarnings( "unchecked" )
    @Override
    public List<? extends JcrOrdering> orderings() {
        return (List<? extends JcrOrdering>)super.orderings();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.api.query.qom.SelectQuery#getConstraint()
     */
    @Override
    public Constraint getConstraint() {
        return constraint();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.api.query.qom.SelectQuery#getSource()
     */
    @Override
    public Source getSource() {
        return source();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.api.query.qom.QueryCommand#getLimits()
     */
    @Override
    public Limit getLimits() {
        return limits();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.api.query.qom.QueryCommand#getColumns()
     */
    @Override
    public Column[] getColumns() {
        List<? extends JcrColumn> columns = columns();
        return columns.toArray(new Column[columns.size()]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.api.query.qom.QueryCommand#getOrderings()
     */
    @Override
    public Ordering[] getOrderings() {
        List<? extends JcrOrdering> orderings = orderings();
        return orderings.toArray(new Ordering[orderings.size()]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Query#withLimit(int)
     */
    @Override
    public Query withLimit( int rowLimit ) {
        if (limits().rowLimit() == rowLimit) return this; // nothing to change
        return new JcrSelectQuery(source(), constraint(), orderings(), columns(), limits().withRowLimit(rowLimit), isDistinct());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Query#withOffset(int)
     */
    @Override
    public Query withOffset( int offset ) {
        if (limits().offset() == offset) return this; // nothing to change
        return new JcrSelectQuery(source(), constraint(), orderings(), columns(), limits().withOffset(offset), isDistinct());
    }

}
