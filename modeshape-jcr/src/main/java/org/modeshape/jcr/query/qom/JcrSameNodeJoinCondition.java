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

import org.modeshape.graph.query.model.SameNodeJoinCondition;
import org.modeshape.graph.query.model.SelectorName;

/**
 * Implementation of the same-node join condition for the JCR Query Object Model and the Graph API.
 */
public class JcrSameNodeJoinCondition extends SameNodeJoinCondition
    implements javax.jcr.query.qom.SameNodeJoinCondition, JcrJoinCondition {

    private static final long serialVersionUID = 1L;

    /**
     * Create a join condition that determines whether the node identified by the first selector is the same as the node at the
     * given path relative to the node identified by the second selector.
     * 
     * @param selector1Name the name of the first selector
     * @param selector2Name the name of the second selector
     * @param selector2Path the relative path from the second selector locating the node being compared with the first selector
     */
    public JcrSameNodeJoinCondition( SelectorName selector1Name,
                                     SelectorName selector2Name,
                                     String selector2Path ) {
        super(selector1Name, selector2Name, selector2Path);
    }

    /**
     * Create a join condition that determines whether the node identified by the first selector is the same as the node
     * identified by the second selector.
     * 
     * @param selector1Name the name of the first selector
     * @param selector2Name the name of the second selector
     * @throws IllegalArgumentException if either selector name is null
     */
    public JcrSameNodeJoinCondition( SelectorName selector1Name,
                                     SelectorName selector2Name ) {
        super(selector1Name, selector2Name);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.SameNodeJoinCondition#getSelector2Path()
     */
    @Override
    public String getSelector2Path() {
        return selector2Path();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.SameNodeJoinCondition#getSelector1Name()
     */
    @Override
    public String getSelector1Name() {
        return selector1Name().name();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.SameNodeJoinCondition#getSelector2Name()
     */
    @Override
    public String getSelector2Name() {
        return selector2Name().name();
    }
}
