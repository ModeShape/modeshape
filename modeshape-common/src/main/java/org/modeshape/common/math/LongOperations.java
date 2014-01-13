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
