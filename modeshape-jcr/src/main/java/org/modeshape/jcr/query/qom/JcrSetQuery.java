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
import javax.jcr.query.qom.Ordering;
import org.modeshape.graph.query.model.SetQuery;
import org.modeshape.jcr.api.query.qom.Limit;
import org.modeshape.jcr.api.query.qom.QueryObjectModelConstants;

/**
 * 
 */
public class JcrSetQuery extends SetQuery implements org.modeshape.jcr.api.query.qom.SetQuery, JcrQueryCommand {

    private static final long serialVersionUID = 1L;

    /**
     * Create a set query involving the supplied left- and right-hand-side queries.
     * 
     * @param left the left-hand-side query being combined
     * @param operation the set operation
     * @param right the right-hand-side query being combined
     * @param all true if all of the results should be included
     * @throws IllegalArgumentException if the left-hand-side query, right-hand-side query, or operation are null
     */
    public JcrSetQuery( JcrQueryCommand left,
                        Operation operation,
                        JcrQueryCommand right,
                        boolean all ) {
        super(left, operation, right, all);
    }

    /**
     * Create a set query involving the supplied left- and right-hand-side queries.
     * 
     * @param left the left-hand-side query being combined
     * @param operation the set operation
     * @param right the right-hand-side query being combined
     * @param all true if all of the results should be included
     * @param orderings the specification of the order of the result rows, or null if the results need not be ordered
     * @param limit the limit for the result rows, or null if there are no limits
     * @throws IllegalArgumentException if the left-hand-side query, right-hand-side query, or operation are null
     */
    public JcrSetQuery( JcrQueryCommand left,
                        Operation operation,
                        JcrQueryCommand right,
                        boolean all,
                        List<? extends JcrOrdering> orderings,
                        JcrLimit limit ) {
        super(left, operation, right, all, orderings, limit != null ? limit : JcrLimit.NONE);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.SetQuery#left()
     */
    @Override
    public JcrQueryCommand left() {
        return (JcrQueryCommand)super.left();
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
     * @see org.modeshape.graph.query.model.SetQuery#right()
     */
    @Override
    public JcrQueryCommand right() {
        return (JcrQueryCommand)super.right();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.SetQuery#limits()
     */
    @Override
    public JcrLimit limits() {
        return (JcrLimit)super.limits();
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
     * @see org.modeshape.jcr.api.query.qom.SetQuery#getLeft()
     */
    @Override
    public JcrQueryCommand getLeft() {
        return left();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.api.query.qom.SetQuery#getRight()
     */
    @Override
    public JcrQueryCommand getRight() {
        return right();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.api.query.qom.SetQuery#getOperation()
     */
    @Override
    public String getOperation() {
        switch (operation()) {
            case UNION:
                return QueryObjectModelConstants.JCR_SET_TYPE_UNION;
            case INTERSECT:
                return QueryObjectModelConstants.JCR_SET_TYPE_INTERSECT;
            case EXCEPT:
                return QueryObjectModelConstants.JCR_SET_TYPE_EXCEPT;
        }
        assert false;
        return null;
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
     * @see org.modeshape.graph.query.model.SetQuery#withLimit(int)
     */
    @Override
    public JcrSetQuery withLimit( int rowLimit ) {
        if (limits().rowLimit() == rowLimit) return this; // nothing to change
        return new JcrSetQuery(left(), operation(), right(), isAll(), orderings(), limits().withRowLimit(rowLimit));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.SetQuery#withOffset(int)
     */
    @Override
    public JcrSetQuery withOffset( int offset ) {
        if (limits().offset() == offset) return this; // nothing to change
        return new JcrSetQuery(left(), operation(), right(), isAll(), orderings(), limits().withOffset(offset));
    }
}
