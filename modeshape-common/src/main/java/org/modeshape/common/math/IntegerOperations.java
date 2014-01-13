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
