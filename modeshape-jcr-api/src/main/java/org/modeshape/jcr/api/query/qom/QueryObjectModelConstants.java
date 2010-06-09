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

/**
 * 
 */
public interface QueryObjectModelConstants extends javax.jcr.query.qom.QueryObjectModelConstants {

    /**
     * The 'add' operator for arithmetic operations.
     */
    public static final String JCR_ARITHMETIC_OPERATOR_ADD = "jcr.arithmetic.operator.add";

    /**
     * The 'add' operator for arithmetic operations.
     */
    public static final String JCR_ARITHMETIC_OPERATOR_SUBTRACT = "jcr.arithmetic.operator.subtract";

    /**
     * The 'add' operator for arithmetic operations.
     */
    public static final String JCR_ARITHMETIC_OPERATOR_MULTIPLY = "jcr.arithmetic.operator.multiply";

    /**
     * The 'add' operator for arithmetic operations.
     */
    public static final String JCR_ARITHMETIC_OPERATOR_DIVIDE = "jcr.arithmetic.operator.divide";

    /**
     * A 'union' set operation.
     */
    public static final String JCR_SET_TYPE_UNION = "jcr.set.type.union";

    /**
     * A 'intersect' set operation.
     */
    public static final String JCR_SET_TYPE_INTERSECT = "jcr.set.type.intersect";

    /**
     * A 'except' set operation.
     */
    public static final String JCR_SET_TYPE_EXCEPT = "jcr.set.type.except";

    /**
     * A full-outer join.
     */
    public static final String JCR_JOIN_TYPE_FULL_OUTER = "jcr.join.type.full.outer";

    /**
     * A cross join.
     */
    public static final String JCR_JOIN_TYPE_CROSS = "jcr.join.type.cross";

}
