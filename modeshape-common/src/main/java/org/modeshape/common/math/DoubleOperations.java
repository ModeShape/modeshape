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
 * The {@link MathOperations math operations} for double numbers.
 */
@Immutable
public class DoubleOperations implements MathOperations<Double>, Comparator<Double> {

    @Override
    public Class<Double> getOperandClass() {
        return Double.class;
    }

    @Override
    public Double add( Double value1,
                       Double value2 ) {
        if (value1 == null) return value2 != null ? value2 : createZeroValue();
        if (value2 == null) return value1;
        return (value1 + value2);
    }

    @Override
    public Double subtract( Double value1,
                            Double value2 ) {
        if (value1 == null) return negate(value2);
        if (value2 == null) return value1;
        return (value1 - value2);
    }

    @Override
    public Double multiply( Double value1,
                            Double value2 ) {
        if (value1 == null || value2 == null) return createZeroValue();
        return (value1 * value2);
    }

    @Override
    public double divide( Double value1,
                          Double value2 ) {
        if (value1 == null || value2 == null) throw new IllegalArgumentException();
        return value1 / value2;
    }

    @Override
    public Double negate( Double value ) {
        if (value == null) return createZeroValue();
        return (value * -1);
    }

    @Override
    public Double increment( Double value ) {
        if (value == null) return createZeroValue();
        return (value + 1);
    }

    @Override
    public Double maximum( Double value1,
                           Double value2 ) {
        if (value1 == null) return value2;
        if (value2 == null) return value1;
        return Math.max(value1, value2);
    }

    @Override
    public Double minimum( Double value1,
                           Double value2 ) {
        if (value1 == null) return value2;
        if (value2 == null) return value1;
        return Math.min(value1, value2);
    }

    @Override
    public int compare( Double value1,
                        Double value2 ) {
        if (value1 == null) return value2 != null ? -1 : 0;
        if (value2 == null) return 1;
        return value1.compareTo(value2);
    }

    @Override
    public BigDecimal asBigDecimal( Double value ) {
        return value != null ? new BigDecimal(value) : null;
    }

    @Override
    public Double fromBigDecimal( BigDecimal value ) {
        return value != null ? value.doubleValue() : null;
    }

    @Override
    public Double createZeroValue() {
        return 0.0d;
    }

    @Override
    public Double create( int value ) {
        return (double)value;
    }

    @Override
    public Double create( long value ) {
        return (double)value;
    }

    @Override
    public Double create( double value ) {
        return value;
    }

    @Override
    public double sqrt( Double value ) {
        return Math.sqrt(value);
    }

    @Override
    public Comparator<Double> getComparator() {
        return this;
    }

    @Override
    public Double random( Double minimum,
                          Double maximum,
                          Random rng ) {
        Double difference = subtract(maximum, minimum);
        return minimum + difference.doubleValue() * rng.nextDouble();
    }

    @Override
    public double doubleValue( Double value ) {
        return value.doubleValue();
    }

    @Override
    public float floatValue( Double value ) {
        return value.floatValue();
    }

    @Override
    public int intValue( Double value ) {
        return value.intValue();
    }

    @Override
    public long longValue( Double value ) {
        return value.longValue();
    }

    @Override
    public short shortValue( Double value ) {
        return value.shortValue();
    }

    @Override
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

    @Override
    public Double roundUp( Double value,
                           int decimalShift ) {
        if (value == 0) return 0.0d;
        double shiftedValue = (Math.abs(value) * Math.pow(10.0d, decimalShift) + 0.5d) * Math.signum(value);
        double roundedValue = (int)shiftedValue;
        return roundedValue * Math.pow(10.0d, -decimalShift);
    }

    @Override
    public Double roundDown( Double value,
                             int decimalShift ) {
        if (value == 0) return 0.0d;
        double shiftedValue = (Math.abs(value) * Math.pow(10.0d, decimalShift)) * Math.signum(value);
        double roundedValue = (int)shiftedValue;
        return roundedValue * Math.pow(10.0d, -decimalShift);
    }

    @Override
    public Double keepSignificantFigures( Double value,
                                          int numSigFigs ) {
        if (numSigFigs < 0) return value;
        if (numSigFigs == 0) return 0.0d;
        int currentExp = getExponentInScientificNotation(value);
        int decimalShift = -currentExp + numSigFigs - 1;
        return roundUp(value, decimalShift);
    }
}
