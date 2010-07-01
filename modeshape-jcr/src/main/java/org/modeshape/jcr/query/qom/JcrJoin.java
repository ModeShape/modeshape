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

import javax.jcr.query.qom.JoinCondition;
import org.modeshape.graph.query.model.Join;
import org.modeshape.graph.query.model.JoinType;
import org.modeshape.jcr.api.query.qom.QueryObjectModelConstants;

/**
 * Implementation of the join for the JCR Query Object Model and the Graph API.
 */
public class JcrJoin extends Join implements javax.jcr.query.qom.Join, JcrSource {

    private static final long serialVersionUID = 1L;

    /**
     * Create a join of the left and right sources, using the supplied join condition. The outputs of the left and right sources
     * are expected to be equivalent.
     * 
     * @param left the left source being joined
     * @param type the type of join
     * @param right the right source being joined
     * @param joinCondition the join condition
     */
    public JcrJoin( JcrSource left,
                    JoinType type,
                    JcrSource right,
                    JcrJoinCondition joinCondition ) {
        super(left, type, right, joinCondition);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Join#joinCondition()
     */
    @Override
    public JcrJoinCondition joinCondition() {
        return (JcrJoinCondition)super.joinCondition();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Join#left()
     */
    @Override
    public JcrSource left() {
        return (JcrSource)super.left();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Join#right()
     */
    @Override
    public JcrSource right() {
        return (JcrSource)super.right();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.Join#getJoinCondition()
     */
    @Override
    public JoinCondition getJoinCondition() {
        return joinCondition();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.Join#getJoinType()
     */
    @Override
    public String getJoinType() {
        switch (type()) {
            case CROSS:
                return QueryObjectModelConstants.JCR_JOIN_TYPE_CROSS;
            case INNER:
                return QueryObjectModelConstants.JCR_JOIN_TYPE_INNER;
            case FULL_OUTER:
                return QueryObjectModelConstants.JCR_JOIN_TYPE_FULL_OUTER;
            case LEFT_OUTER:
                return QueryObjectModelConstants.JCR_JOIN_TYPE_LEFT_OUTER;
            case RIGHT_OUTER:
                return QueryObjectModelConstants.JCR_JOIN_TYPE_RIGHT_OUTER;
        }
        assert false;
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.Join#getLeft()
     */
    @Override
    public JcrSource getLeft() {
        return left();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.Join#getRight()
     */
    @Override
    public JcrSource getRight() {
        return right();
    }
}
