package org.modeshape.jcr.query.model;

import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.HashCode;
import org.modeshape.common.annotation.Immutable;

/**
 * A constraint that evaluates to true when the reverse like operation evaluates to true.
 */
@Immutable
public class Relike implements org.modeshape.jcr.api.query.qom.Relike, Constraint {

    private final StaticOperand operand1;
    private final PropertyValue operand2;
    private final int hc;
        
    public Relike(StaticOperand operand1, PropertyValue operand2) {
        CheckArg.isNotNull(operand1, "operand1");
        CheckArg.isNotNull(operand2, "operator");
        
        this.operand1 = operand1;
        this.operand2 = operand2;
        this.hc = HashCode.compute(this.operand1, this.operand2);
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    @Override
    public StaticOperand getOperand1() {
        return operand1;
    }

    @Override
    public PropertyValue getOperand2() {
        return operand2;
    }
    
    @Override
    public String toString() {
        return Visitors.readable(this);
    }
    
    @Override
    public int hashCode() {
        return hc;
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof Relike) {
            Relike that = (Relike)obj;
            if (this.hc != that.hc) return false;
            if (!this.operand1.equals(that.operand1)) return false;
            if (!this.operand2.equals(that.operand2)) return false;
            return true;
        }
        return false;
    }
}
