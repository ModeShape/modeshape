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
 * The {@link MathOperations math operations} for long numbers.
 */
@Immutable
public class LongOperations implements MathOperations<Long>, Comparator<Long> {

    @Override
    public Class<Long> getOperandClass() {
        return Long.class;
    }

    @Override
    public Long add( Long value1,
                     Long value2 ) {
        if (value1 == null) return value2 != null ? value2 : createZeroValue();
        if (value2 == null) return value1;
        return (value1 + value2);
    }

    @Override
    public Long subtract( Long value1,
                          Long value2 ) {
        if (value1 == null) return negate(value2);
        if (value2 == null) return value1;
        return (value1 - value2);
    }

    @Override
    public Long multiply( Long value1,
                          Long value2 ) {
        if (value1 == null || value2 == null) return createZeroValue();
        return (value1 * value2);
    }

    @Override
    public double divide( Long value1,
                          Long value2 ) {
        if (value1 == null || value2 == null) throw new IllegalArgumentException();
        return value1 / value2;
    }

    @Override
    public Long negate( Long value ) {
        if (value == null) return createZeroValue();
        return (value * -1);
    }

    @Override
    public Long increment( Long value ) {
        if (value == null) return createZeroValue();
        return (value + 1);
    }

    @Override
    public Long maximum( Long value1,
                         Long value2 ) {
        if (value1 == null) return value2;
        if (value2 == null) return value1;
        return Math.max(value1, value2);
    }

    @Override
    public Long minimum( Long value1,
                         Long value2 ) {
        if (value1 == null) return value2;
        if (value2 == null) return value1;
        return Math.min(value1, value2);
    }

    @Override
    public int compare( Long value1,
                        Long value2 ) {
        if (value1 == null) return value2 != null ? -1 : 0;
        if (value2 == null) return 1;
        return value1.compareTo(value2);
    }

    @Override
    public BigDecimal asBigDecimal( Long value ) {
        return value != null ? new BigDecimal(value) : null;
    }

    @Override
    public Long fromBigDecimal( BigDecimal value ) {
        return value != null ? value.longValue() : null;
    }

    @Override
    public Long createZeroValue() {
        return 0l;
    }

    @Override
    public Long create( int value ) {
        return (long)value;
    }

    @Override
    public Long create( long value ) {
        return value;
    }

    @Override
    public Long create( double value ) {
        return (long)value;
    }

    @Override
    public double sqrt( Long value ) {
        return Math.sqrt(value);
    }

    @Override
    public Comparator<Long> getComparator() {
        return this;
    }

    @Override
    public Long random( Long minimum,
                        Long maximum,
                        Random rng ) {
        Long difference = subtract(maximum, minimum);
        return minimum + rng.nextInt(difference.intValue());
    }

    @Override
    public double doubleValue( Long value ) {
        return value.doubleValue();
    }

    @Override
    public float floatValue( Long value ) {
        return value.floatValue();
    }

    @Override
    public int intValue( Long value ) {
        return value.intValue();
    }

    @Override
    public long longValue( Long value ) {
        return value.longValue();
    }

    @Override
    public short shortValue( Long value ) {
        return value.shortValue();
    }

    @Override
    public int getExponentInScientificNotation( Long value ) {
        long v = Math.abs(value);
        int exp = 0;
        if (v > 1l) {
            while (v >= 10l) {
                v /= 10l;
                ++exp;
            }
        } else if (v == 0l) {
        } else if (v < 1l) {
            while (v < 1l) {
                v *= 10l;
                --exp;
            }
        }
        return exp;
    }

    @Override
    public Long roundUp( Long value,
                         int decimalShift ) {
        if (value == 0) return 0l;
        if (decimalShift >= 0) return value;
        long shiftedValueP5 = Math.abs(value);
        for (int i = 0; i != (-decimalShift - 1); ++i)
            shiftedValueP5 /= 10l;
        shiftedValueP5 += 5l;
        long shiftedValue = shiftedValueP5 / 10l;
        if (shiftedValue * 10l - shiftedValueP5 >= 5) ++shiftedValue;
        shiftedValue *= Long.signum(value);
        for (int i = 0; i != -decimalShift; ++i)
            shiftedValue *= 10l;
        return shiftedValue;
    }

    @Override
    public Long roundDown( Long value,
                           int decimalShift ) {
        if (value == 0) return 0l;
        if (decimalShift >= 0) return value;
        long shiftedValue = Math.abs(value);
        for (int i = 0; i != -decimalShift; ++i)
            shiftedValue /= 10l;
        shiftedValue *= Long.signum(value);
        for (int i = 0; i != -decimalShift; ++i)
            shiftedValue *= 10l;
        return shiftedValue;
    }

    @Override
    public Long keepSignificantFigures( Long value,
                                        int numSigFigs ) {
        if (value == 0l) return value;
        if (numSigFigs < 0) return value;
        if (numSigFigs == 0) return 0l;
        int currentExp = getExponentInScientificNotation(value);
        int decimalShift = -currentExp + numSigFigs - 1;
        return roundUp(value, decimalShift);
    }
}
