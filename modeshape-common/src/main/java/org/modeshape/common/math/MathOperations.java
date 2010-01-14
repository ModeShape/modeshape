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
 * The set of mathematic operations for a particular class of values. This is useful for generic classes that must work with one
 * of the {@link Number} subclasses.
 * 
 * @param <T> the numeric class, usually a subclass of {@link Number} (although this is not required)
 */
@Immutable
public interface MathOperations<T> {

    /**
     * Return the class that these operations operate upon.
     * 
     * @return the class
     */
    public Class<T> getOperandClass();

    /**
     * Add the two operands and return the sum. The {@link #createZeroValue() zero value} is used in place of any operand that is
     * null.
     * 
     * @param value1 the first operand
     * @param value2 the second operand
     * @return the sum of the two operands.
     */
    public T add( T value1,
                  T value2 );

    /**
     * Subtract the second operand from the first, and return the difference. The {@link #createZeroValue() zero value} is used in
     * place of any operand that is null.
     * 
     * @param value1 the first operand
     * @param value2 the second operand
     * @return the difference between the two operands.
     */
    public T subtract( T value1,
                       T value2 );

    /**
     * Multiply the two operands and return the product. The {@link #createZeroValue() zero value} is used in place of any operand
     * that is null.
     * 
     * @param value1 the first operand
     * @param value2 the second operand
     * @return the product of the two operands.
     */
    public T multiply( T value1,
                       T value2 );

    /**
     * Divide the first operand by the second, and return the result. The {@link #createZeroValue() zero value} is used in place
     * of any operand that is null.
     * 
     * @param value1 the first operand
     * @param value2 the second operand
     * @return the result of the division
     */
    public double divide( T value1,
                          T value2 );

    /**
     * Negate the supplied operand. The {@link #createZeroValue() zero value} is used in place of any operand that is null.
     * 
     * @param value the value that is to be negated
     * @return the result of the negation
     */
    public T negate( T value );

    /**
     * Increment the supplied operand by 1. (Note, the exact meaning of "1" is dependent upon the particular
     * {@link #getOperandClass() operand class}. The {@link #createZeroValue() zero value} is used in place of any operand that is
     * null.
     * 
     * @param value the value that is to be incremented
     * @return the incremented value
     */
    public T increment( T value );

    /**
     * Compare the two operands and return the one that is larger. A null value is considered smaller than non-null values
     * (including 0).
     * 
     * @param value1 the first operand
     * @param value2 the second operand
     * @return the larger of the two operands
     */
    public T maximum( T value1,
                      T value2 );

    /**
     * Compare the two operands and return the one that is smaller. A null value is considered larger than non-null values
     * (including 0).
     * 
     * @param value1 the first operand
     * @param value2 the second operand
     * @return the smaller of the two operands
     */
    public T minimum( T value1,
                      T value2 );

    /**
     * Compare the two operands and return an integer that describes whether the first value is larger, smaller or the same as the
     * second value. The semantics are identical to those of {@link Comparable}. The {@link #createZeroValue() zero value} is used
     * in place of any operand that is null.
     * 
     * @param value1 the first operand
     * @param value2 the second operand
     * @return -1 if the first value is smaller than the second, 1 if the first value is larger than the second, or 0 if they are
     *         equal.
     */
    public int compare( T value1,
                        T value2 );

    /**
     * Create a {@link BigDecimal} representation of the supplied value.
     * 
     * @param value the value that is to be converted to a BigDecimal
     * @return the BigDecimal representation, or null if <code>value</code> is null
     */
    public BigDecimal asBigDecimal( T value );

    /**
     * Convert the {@link BigDecimal} representation into the natural object representation. This may result in loss of some data
     * (e.g., converting a decimal to an integer results in the loss of the fractional part of the number).
     * 
     * @param value the BigDecimal value
     * @return the natural representation, or null if <code>value</code> is null
     */
    public T fromBigDecimal( BigDecimal value );

    /**
     * Convert the value to a double. This may result in a loss of information depending upon the {@link #getOperandClass()
     * operand class}.
     * 
     * @param value the value
     * @return the representation as a double
     */
    public double doubleValue( T value );

    /**
     * Convert the value to a float. This may result in a loss of information depending upon the {@link #getOperandClass() operand
     * class}.
     * 
     * @param value the value
     * @return the representation as a float
     */
    public float floatValue( T value );

    /**
     * Convert the value to an integer. This may result in a loss of information depending upon the {@link #getOperandClass()
     * operand class}.
     * 
     * @param value the value
     * @return the representation as an integer
     */
    public int intValue( T value );

    /**
     * Convert the value to a short. This may result in a loss of information depending upon the {@link #getOperandClass() operand
     * class}.
     * 
     * @param value the value
     * @return the representation as a short
     */
    public short shortValue( T value );

    /**
     * Convert the value to a long integer. This may result in a loss of information depending upon the {@link #getOperandClass()
     * operand class}.
     * 
     * @param value the value
     * @return the representation as a long
     */
    public long longValue( T value );

    /**
     * Create the object form of the "zero value". This is often used to create an uninitialized object.
     * 
     * @return the object that represents zero.
     */
    public T createZeroValue();

    /**
     * Convert the integer representation into the natural object representation.
     * 
     * @param value the integer value
     * @return the object representation of the integer
     */
    public T create( int value );

    /**
     * Convert the long representation into the natural object representation.
     * 
     * @param value the long value
     * @return the object representation of the long integer
     */
    public T create( long value );

    /**
     * Convert the double representation into the natural object representation.
     * 
     * @param value the double value
     * @return the object representation of the floating point number
     */
    public T create( double value );

    /**
     * Return the square root of the supplied operand.
     * 
     * @param value the value whose root is to be found; may not be null or 0
     * @return the square root of the value
     */
    public double sqrt( T value );

    /**
     * Return a {@link Comparator Comparator<T>} for this {@link #getOperandClass() operand class}. The implementation is free to
     * return the same comparator instance from multiple invocations of this method.
     * 
     * @return a comparator
     */
    public Comparator<T> getComparator();

    /**
     * Get the exponent if the number were written in exponential form.
     * 
     * @param value the value
     * @return the scale
     */
    public int getExponentInScientificNotation( T value );

    /**
     * Round up the supplied value to the desired scale. This process works (conceptually) by shifting the decimal point of the
     * value by <code>decimalShift</code> places, rounding, and then shifting the decimal point of the rounded value by
     * <code>-decimalShift</code>
     * <p>
     * For example, consider the number 10.000354. This can be rounded to 10.0004 by calling this method and supplying the value
     * and an "exponentToKeep" value of -4.
     * </p>
     * 
     * @param value the value to be rounded
     * @param decimalShift the number of places the decimal point should be shifted before rounding
     * @return the rounded value
     */
    public T roundUp( T value,
                      int decimalShift );

    /**
     * Round down the supplied value to the desired scale. This process works (conceptually) by shifting the decimal point of the
     * value by <code>decimalShift</code> places, rounding, and then shifting the decimal point of the rounded value by
     * <code>-decimalShift</code>
     * <p>
     * For example, consider the number 10.000354. This can be rounded to 10.0003 by calling this method and supplying the value
     * and an "exponentToKeep" value of -4.
     * </p>
     * 
     * @param value the value to be rounded
     * @param decimalShift the number of places the decimal point should be shifted before rounding
     * @return the rounded value
     */
    public T roundDown( T value,
                        int decimalShift );

    public T keepSignificantFigures( T value,
                                     int numSigFigs );

    /**
     * Generate a random instance within the specified range.
     * 
     * @param minimum the minimum value, or null if the {@link #createZeroValue() zero-value} should be used for the minimum
     * @param maximum the maximum value, or null if the {@link #createZeroValue() zero-value} should be used for the maximum
     * @param rng the random number generator to use
     * @return an instance of the {@link #getOperandClass() operand class} placed within the desired range using a random
     *         distribution, or null if this class does not support generating random instances
     */
    public T random( T minimum,
                     T maximum,
                     Random rng );
}
