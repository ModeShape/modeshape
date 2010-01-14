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
 * The {@link MathOperations math operations} for float numbers.
 */
@Immutable
public class FloatOperations implements MathOperations<Float>, Comparator<Float> {

    public Class<Float> getOperandClass() {
        return Float.class;
    }

    public Float add( Float value1,
                      Float value2 ) {
        if (value1 == null) return value2 != null ? value2 : createZeroValue();
        if (value2 == null) return value1;
        return (value1 + value2);
    }

    public Float subtract( Float value1,
                           Float value2 ) {
        if (value1 == null) return negate(value2);
        if (value2 == null) return value1;
        return (value1 - value2);
    }

    public Float multiply( Float value1,
                           Float value2 ) {
        if (value1 == null || value2 == null) return createZeroValue();
        return (value1 * value2);
    }

    public double divide( Float value1,
                          Float value2 ) {
        if (value1 == null || value2 == null) throw new IllegalArgumentException();
        return value1 / value2;
    }

    public Float negate( Float value ) {
        if (value == null) return createZeroValue();
        return (value * -1);
    }

    public Float increment( Float value ) {
        if (value == null) return createZeroValue();
        return (value + 1);
    }

    public Float maximum( Float value1,
                          Float value2 ) {
        if (value1 == null) return value2;
        if (value2 == null) return value1;
        return Math.max(value1, value2);
    }

    public Float minimum( Float value1,
                          Float value2 ) {
        if (value1 == null) return value2;
        if (value2 == null) return value1;
        return Math.min(value1, value2);
    }

    public int compare( Float value1,
                        Float value2 ) {
        if (value1 == null) return value2 != null ? -1 : 0;
        if (value2 == null) return 1;
        return value1.compareTo(value2);
    }

    public BigDecimal asBigDecimal( Float value ) {
        return value != null ? new BigDecimal(value) : null;
    }

    public Float fromBigDecimal( BigDecimal value ) {
        return value != null ? value.floatValue() : null;
    }

    public Float createZeroValue() {
        return 0.0f;
    }

    public Float create( int value ) {
        return (float)value;
    }

    public Float create( long value ) {
        return (float)value;
    }

    public Float create( double value ) {
        return (float)value;
    }

    public double sqrt( Float value ) {
        return Math.sqrt(value);
    }

    public Comparator<Float> getComparator() {
        return this;
    }

    public Float random( Float minimum,
                         Float maximum,
                         Random rng ) {
        Float difference = subtract(maximum, minimum);
        return minimum + difference.floatValue() * rng.nextFloat();
    }

    public double doubleValue( Float value ) {
        return value.doubleValue();
    }

    public float floatValue( Float value ) {
        return value.floatValue();
    }

    public int intValue( Float value ) {
        return value.intValue();
    }

    public long longValue( Float value ) {
        return value.longValue();
    }

    public short shortValue( Float value ) {
        return value.shortValue();
    }

    public int getExponentInScientificNotation( Float value ) {
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

    public Float roundUp( Float value,
                          int decimalShift ) {
        if (value == 0) return 0.0f;
        double shiftedValue = (Math.abs(value.doubleValue()) * Math.pow(10.0d, decimalShift) + 0.5d) * Math.signum(value);
        double roundedValue = (long)shiftedValue;
        return (float)(roundedValue * Math.pow(10.0d, -decimalShift));
    }

    public Float roundDown( Float value,
                            int decimalShift ) {
        if (value == 0) return 0.0f;
        if (decimalShift > 0) return value;
        double shiftedValue = (Math.abs(value.doubleValue()) * Math.pow(10.0d, decimalShift)) * Math.signum(value);
        double roundedValue = (long)shiftedValue;
        return (float)(roundedValue * Math.pow(10.0d, -decimalShift));
    }

    public Float keepSignificantFigures( Float value,
                                         int numSigFigs ) {
        int currentExp = getExponentInScientificNotation(value);
        int decimalShift = (int)Math.signum(currentExp) * (Math.abs(currentExp) + numSigFigs - 1);
        return roundUp(value, decimalShift);
    }
}
