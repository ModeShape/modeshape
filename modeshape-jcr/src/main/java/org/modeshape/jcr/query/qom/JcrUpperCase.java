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

import org.modeshape.graph.query.model.UpperCase;

/**
 * Implementation of the upper case dynamic operand for the JCR Query Object Model and the Graph API.
 */
public class JcrUpperCase extends UpperCase implements javax.jcr.query.qom.UpperCase, JcrDynamicOperand {

    private static final long serialVersionUID = 1L;

    /**
     * @param operand
     */
    public JcrUpperCase( JcrDynamicOperand operand ) {
        super(operand);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.LowerCase#getOperand()
     */
    @Override
    public JcrDynamicOperand getOperand() {
        return (JcrDynamicOperand)operand();
    }
}
