package org.modeshape.jcr.api.query.qom;

import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.PropertyValue;
import javax.jcr.query.qom.StaticOperand;

public interface Relike extends Constraint {

    StaticOperand getOperand1();

    PropertyValue getOperand2();
}
