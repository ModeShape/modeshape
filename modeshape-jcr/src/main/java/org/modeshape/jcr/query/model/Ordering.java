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
package org.modeshape.jcr.query.model;

import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.api.query.qom.QueryObjectModelConstants;

/**
 * A specification of the ordering for the results.
 */
@Immutable
public class Ordering implements LanguageObject, org.modeshape.jcr.api.query.qom.Ordering {
    private static final long serialVersionUID = 1L;

    private final DynamicOperand operand;
    private final Order order;
    private final NullOrder nullOrder;

    /**
     * Create a new ordering specification, given the supplied operand and order.
     * 
     * @param operand the operand being ordered
     * @param order the order type
     * @param nullOrder the order specification for null values; may not be null
     * @throws IllegalArgumentException if the operand or order type is null
     */
    public Ordering( DynamicOperand operand,
                     Order order,
                     NullOrder nullOrder ) {
        CheckArg.isNotNull(operand, "operand");
        CheckArg.isNotNull(order, "order");
        CheckArg.isNotNull(nullOrder, "nullOrder");
        this.operand = operand;
        this.order = order;
        this.nullOrder = nullOrder;
    }

    @Override
    public DynamicOperand getOperand() {
        return operand;
    }

    public Order order() {
        return order;
    }

    public NullOrder nullOrder() {
        return nullOrder;
    }

    public boolean isDefaultNullOrder() {
        return NullOrder.defaultOrder(order) == nullOrder;
    }

    @Override
    public String getOrder() {
        switch (order()) {
            case ASCENDING:
                return QueryObjectModelConstants.JCR_ORDER_ASCENDING;
            case DESCENDING:
                return QueryObjectModelConstants.JCR_ORDER_DESCENDING;
        }
        assert false;
        return null;
    }

    @Override
    public String getNullOrder() {
        switch (nullOrder()) {
            case NULLS_FIRST:
                return QueryObjectModelConstants.JCR_ORDER_NULLS_FIRST;
            case NULLS_LAST:
                return QueryObjectModelConstants.JCR_ORDER_NULLS_LAST;
        }
        assert false;
        return null;
    }

    @Override
    public String toString() {
        return Visitors.readable(this);
    }

    @Override
    public int hashCode() {
        return operand.hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof Ordering) {
            Ordering that = (Ordering)obj;
            if (this.order != that.order) return false;
            return this.operand.equals(that.operand);
        }
        return false;
    }

    @Override
    public void accept( Visitor visitor ) {
        visitor.visit(this);
    }
}
