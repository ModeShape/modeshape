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
import org.modeshape.common.annotation.Immutable;

/**
 * The {@link MathOperations math operations} for integer numbers.
 */
@Immutable
public class IntegerOperations implements MathOperations<Integer>, Comparator<Integer> {

    @Override
    public Class<Integer> getOperandClass() {
        return Integer.class;
    }

    @Override
    public Integer add( Integer value1,
                        Integer value2 ) {
        if (value1 == null) return value2 != null ? value2 : createZeroValue();
        if (value2 == null) return value1;
        return value1 + value2;
    }

    @Override
    public Integer subtract( Integer value1,
                             Integer value2 ) {
        if (value1 == null) return negate(value2);
        if (value2 == null) return value1;
        return value1 - value2;
    }

    @Override
    public Integer multiply( Integer value1,
                             Integer value2 ) {
        if (value1 == null || value2 == null) return createZeroValue();
        return value1 * value2;
    }

    @Override
    public double divide( Integer value1,
                          Integer value2 ) {
        if (value1 == null || value2 == null) throw new IllegalArgumentException();
        return value1 / value2;
    }

    @Override
    public Integer negate( Integer value ) {
        if (value == null) return createZeroValue();
        return value * -1;
    }

    @Override
    public Integer increment( Integer value ) {
        if (value == null) return createZeroValue();
        return value + 1;
    }

    @Override
    public Integer maximum( Integer value1,
                            Integer value2 ) {
        if (value1 == null) return value2;
        if (value2 == null) return value1;
        return Math.max(value1, value2);
    }

    @Override
    public Integer minimum( Integer value1,
                            Integer value2 ) {
        if (value1 == null) return value2;
        if (value2 == null) return value1;
        return Math.min(value1, value2);
    }

    @Override
    public int compare( Integer value1,
                        Integer value2 ) {
        if (value1 == null) return value2 != null ? -1 : 0;
        if (value2 == null) return 1;
        return value1.compareTo(value2);
    }

    @Override
    public BigDecimal asBigDecimal( Integer value ) {
        return value != null ? new BigDecimal(value) : null;
    }

    @Override
    public Integer fromBigDecimal( BigDecimal value ) {
        return value != null ? value.intValue() : null;
    }

    @Override
    public Integer createZeroValue() {
        return 0;
    }

    @Override
    public Integer create( int value ) {
        return value;
    }

    @Override
    public Integer create( long value ) {
        return (int)value;
    }

    @Override
    public Integer create( double value ) {
        return (int)value;
    }

    @Override
    public double sqrt( Integer value ) {
        return Math.sqrt(value);
    }

    @Override
    public Comparator<Integer> getComparator() {
        return this;
    }

    @Override
    public Integer random( Integer minimum,
                           Integer maximum,
                           Random rng ) {
        Integer difference = subtract(maximum, minimum);
        return minimum + rng.nextInt(difference);
    }

    @Override
    public double doubleValue( Integer value ) {
        return value.doubleValue();
    }

    @Override
    public float floatValue( Integer value ) {
        return value.floatValue();
    }

    @Override
    public int intValue( Integer value ) {
        return value.intValue();
    }

    @Override
    public long longValue( Integer value ) {
        return value.longValue();
    }

    @Override
    public short shortValue( Integer value ) {
        return value.shortValue();
    }

    @Override
    public int getExponentInScientificNotation( Integer value ) {
        int v = Math.abs(value);
        int exp = 0;
        if (v > 1) {
            while (v >= 10) {
                v /= 10;
                ++exp;
            }
        }
        return exp;
    }

    @Override
    public Integer roundUp( Integer value,
                            int decimalShift ) {
        if (value == 0) return 0;
        if (decimalShift >= 0) return value;
        int shiftedValueP5 = Math.abs(value);
        for (int i = 0; i != (-decimalShift - 1); ++i)
            shiftedValueP5 /= 10;
        shiftedValueP5 += 5l;
        int shiftedValue = shiftedValueP5 / 10;
        if (shiftedValue * 10l - shiftedValueP5 >= 5) ++shiftedValue;
        shiftedValue *= Long.signum(value);
        for (int i = 0; i != -decimalShift; ++i)
            shiftedValue *= 10;
        return shiftedValue;
    }

    @Override
    public Integer roundDown( Integer value,
                              int decimalShift ) {
        if (value == 0) return 0;
        if (decimalShift >= 0) return value;
        int shiftedValue = Math.abs(value);
        for (int i = 0; i != -decimalShift; ++i)
            shiftedValue /= 10;
        shiftedValue *= Long.signum(value);
        for (int i = 0; i != -decimalShift; ++i)
            shiftedValue *= 10;
        return shiftedValue;
    }

    @Override
    public Integer keepSignificantFigures( Integer value,
                                           int numSigFigs ) {
        if (numSigFigs < 0) return value;
        if (numSigFigs == 0) return 0;
        int currentExp = getExponentInScientificNotation(value);
        int decimalShift = -currentExp + numSigFigs - 1;
        return roundUp(value, decimalShift);
    }
}
