/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

    /**
     * Specification that NULL values should appear first in an {@link Ordering}.
     */
    public static final String JCR_ORDER_NULLS_FIRST = "jcr.order.nulls.first";

    /**
     * Specification that NULL values should appear last in an {@link Ordering}.
     */
    public static final String JCR_ORDER_NULLS_LAST = "jcr.order.nulls.last";

}
