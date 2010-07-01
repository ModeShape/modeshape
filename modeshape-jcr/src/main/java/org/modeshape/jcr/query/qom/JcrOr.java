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

import org.modeshape.graph.query.model.Or;

/**
 * Implementation of the 'or' constraint for the JCR Query Object Model and the Graph API.
 */
public class JcrOr extends Or implements javax.jcr.query.qom.Or, JcrConstraint {

    private static final long serialVersionUID = 1L;

    /**
     * @param left
     * @param right
     */
    public JcrOr( JcrConstraint left,
                  JcrConstraint right ) {
        super(left, right);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.And#left()
     */
    @Override
    public JcrConstraint left() {
        return (JcrConstraint)super.left();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.And#right()
     */
    @Override
    public JcrConstraint right() {
        return (JcrConstraint)super.right();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.And#getConstraint1()
     */
    @Override
    public JcrConstraint getConstraint1() {
        return left();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.And#getConstraint2()
     */
    @Override
    public JcrConstraint getConstraint2() {
        return right();
    }
}
