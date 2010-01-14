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
package org.modeshape.common.math;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Random;
import net.jcip.annotations.Immutable;

/**
 * The {@link MathOperations math operations} for double numbers.
 */
@Immutable
public class DoubleOperations implements MathOperations<Double>, Comparator<Double> {

    public Class<Double> getOperandClass() {
        return Double.class;
    }

    public Double add( Double value1,
                       Double value2 ) {
        if (value1 == null) return value2 != null ? value2 : createZeroValue();
        if (value2 == null) return value1;
        return (value1 + value2);
    }

    public Double subtract( Double value1,
                            Double value2 ) {
        if (value1 == null) return negate(value2);
        if (value2 == null) return value1;
        return (value1 - value2);
    }

    public Double multiply( Double value1,
                            Double value2 ) {
        if (value1 == null || value2 == null) return createZeroValue();
        return (value1 * value2);
    }

    public double divide( Double value1,
                          Double value2 ) {
        if (value1 == null || value2 == null) throw new IllegalArgumentException();
        return value1 / value2;
    }

    public Double negate( Double value ) {
        if (value == null) return createZeroValue();
        return (value * -1);
    }

    public Double increment( Double value ) {
        if (value == null) return createZeroValue();
        return (value + 1);
    }

    public Double maximum( Double value1,
                           Double value2 ) {
        if (value1 == null) return value2;
        if (value2 == null) return value1;
        return Math.max(value1, value2);
    }

    public Double minimum( Double value1,
                           Double value2 ) {
        if (value1 == null) return value2;
        if (value2 == null) return value1;
        return Math.min(value1, value2);
    }

    public int compare( Double value1,
                        Double value2 ) {
        if (value1 == null) return value2 != null ? -1 : 0;
        if (value2 == null) return 1;
        return value1.compareTo(value2);
    }

    public BigDecimal asBigDecimal( Double value ) {
        return value != null ? new BigDecimal(value) : null;
    }

    public Double fromBigDecimal( BigDecimal value ) {
        return value != null ? value.doubleValue() : null;
    }

    public Double createZeroValue() {
        return 0.0d;
    }

    public Double create( int value ) {
        return (double)value;
    }

    public Double create( long value ) {
        return (double)value;
    }

    public Double create( double value ) {
        return value;
    }

    public double sqrt( Double value ) {
        return Math.sqrt(value);
    }

    public Comparator<Double> getComparator() {
        return this;
    }

    public Double random( Double minimum,
                          Double maximum,
                          Random rng ) {
        Double difference = subtract(maximum, minimum);
        return minimum + difference.doubleValue() * rng.nextDouble();
    }

    public double doubleValue( Double value ) {
        return value.doubleValue();
    }

    public float floatValue( Double value ) {
        return value.floatValue();
    }

    public int intValue( Double value ) {
        return value.intValue();
    }

    public long longValue( Double value ) {
        return value.longValue();
    }

    public short shortValue( Double value ) {
        return value.shortValue();
    }

    public int getExponentInScientificNotation( Double value ) {
        double v = Math.abs(value);
        int exp = 0;
        if (v > 1.0d) {
            while (v >= 10.0d) {
                v /= 10.0d;
                ++exp;
            }
        } else if (v == 0.0d) {
        } else if (v < 1.0d) {
            while (v < 1.0d) {
                v *= 10.0d;
                --exp;
            }
        }
        return exp;
    }

    public Double roundUp( Double value,
                           int decimalShift ) {
        if (value == 0) return 0.0d;
        double shiftedValue = (Math.abs(value) * Math.pow(10.0d, decimalShift) + 0.5d) * Math.signum(value);
        double roundedValue = (int)shiftedValue;
        return roundedValue * Math.pow(10.0d, -decimalShift);
    }

    public Double roundDown( Double value,
                             int decimalShift ) {
        if (value == 0) return 0.0d;
        double shiftedValue = (Math.abs(value) * Math.pow(10.0d, decimalShift)) * Math.signum(value);
        double roundedValue = (int)shiftedValue;
        return roundedValue * Math.pow(10.0d, -decimalShift);
    }

    public Double keepSignificantFigures( Double value,
                                          int numSigFigs ) {
        if (numSigFigs < 0) return value;
        if (numSigFigs == 0) return 0.0d;
        int currentExp = getExponentInScientificNotation(value);
        int decimalShift = -currentExp + numSigFigs - 1;
        return roundUp(value, decimalShift);
    }
}
