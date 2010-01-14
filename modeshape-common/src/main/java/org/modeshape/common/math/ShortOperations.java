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
 * The {@link MathOperations math operations} for short numbers.
 */
@Immutable
public class ShortOperations implements MathOperations<Short>, Comparator<Short> {

    public Class<Short> getOperandClass() {
        return Short.class;
    }

    public Short add( Short value1,
                      Short value2 ) {
        if (value1 == null) return value2 != null ? value2 : createZeroValue();
        if (value2 == null) return value1;
        return (short)(value1 + value2);
    }

    public Short subtract( Short value1,
                           Short value2 ) {
        if (value1 == null) return negate(value2);
        if (value2 == null) return value1;
        return (short)(value1 - value2);
    }

    public Short multiply( Short value1,
                           Short value2 ) {
        if (value1 == null || value2 == null) return createZeroValue();
        return (short)(value1 * value2);
    }

    public double divide( Short value1,
                          Short value2 ) {
        if (value1 == null || value2 == null) throw new IllegalArgumentException();
        return value1 / value2;
    }

    public Short negate( Short value ) {
        if (value == null) return createZeroValue();
        return (short)(value * -1);
    }

    public Short increment( Short value ) {
        if (value == null) return createZeroValue();
        return (short)(value + 1);
    }

    public Short maximum( Short value1,
                          Short value2 ) {
        if (value1 == null) return value2;
        if (value2 == null) return value1;
        return (short)Math.max(value1, value2);
    }

    public Short minimum( Short value1,
                          Short value2 ) {
        if (value1 == null) return value2;
        if (value2 == null) return value1;
        return (short)Math.min(value1, value2);
    }

    public int compare( Short value1,
                        Short value2 ) {
        if (value1 == null) return value2 != null ? -1 : 0;
        if (value2 == null) return 1;
        return value1.compareTo(value2);
    }

    public BigDecimal asBigDecimal( Short value ) {
        return value != null ? new BigDecimal(value) : null;
    }

    public Short fromBigDecimal( BigDecimal value ) {
        return value != null ? value.shortValue() : null;
    }

    public Short createZeroValue() {
        return 0;
    }

    public Short create( int value ) {
        return (short)value;
    }

    public Short create( long value ) {
        return (short)value;
    }

    public Short create( double value ) {
        return (short)value;
    }

    public double sqrt( Short value ) {
        return Math.sqrt(value);
    }

    public Comparator<Short> getComparator() {
        return this;
    }

    public Short random( Short minimum,
                         Short maximum,
                         Random rng ) {
        Short difference = subtract(maximum, minimum);
        int increment = rng.nextInt(difference.intValue());
        return new Integer(minimum + increment).shortValue();
    }

    public double doubleValue( Short value ) {
        return value.doubleValue();
    }

    public float floatValue( Short value ) {
        return value.floatValue();
    }

    public int intValue( Short value ) {
        return value.intValue();
    }

    public long longValue( Short value ) {
        return value.longValue();
    }

    public short shortValue( Short value ) {
        return value.shortValue();
    }

    public int getExponentInScientificNotation( Short value ) {
        int v = Math.abs(value);
        int exp = 0;
        if (v > 1) {
            while (v >= 10) {
                v /= 10;
                ++exp;
            }
        } else if (v < 1) {
            while (v < 1) {
                v *= 10;
                --exp;
            }
        }
        return exp;
    }

    public Short roundUp( Short value,
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
        return (short)shiftedValue;
    }

    public Short roundDown( Short value,
                            int decimalShift ) {
        if (value == 0) return 0;
        if (decimalShift >= 0) return value;
        int shiftedValue = Math.abs(value);
        for (int i = 0; i != -decimalShift; ++i)
            shiftedValue /= 10;
        shiftedValue *= Long.signum(value);
        for (int i = 0; i != -decimalShift; ++i)
            shiftedValue *= 10;
        return (short)shiftedValue;
    }

    public Short keepSignificantFigures( Short value,
                                         int numSigFigs ) {
        if (numSigFigs < 0) return value;
        if (numSigFigs == 0) return 0;
        int currentExp = getExponentInScientificNotation(value);
        int decimalShift = -currentExp + numSigFigs - 1;
        return roundUp(value, decimalShift);
    }
}
