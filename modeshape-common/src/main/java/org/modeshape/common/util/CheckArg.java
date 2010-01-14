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
package org.modeshape.common.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import net.jcip.annotations.Immutable;
import org.modeshape.common.CommonI18n;

/**
 * Utility class that checks arguments to methods. This class is to be used only in API methods, where failure to supply correct
 * arguments should result in a useful error message. In all cases, use the <code>assert</code> statement.
 */
@Immutable
public final class CheckArg {

    // ########################## int METHODS ###################################

    /**
     * Check that the argument is not less than the supplied value
     * 
     * @param argument The argument
     * @param notLessThanValue the value that is to be used to check the value
     * @param name The name of the argument
     * @throws IllegalArgumentException If argument greater than or equal to the supplied vlaue
     */
    public static void isNotLessThan( int argument,
                                      int notLessThanValue,
                                      String name ) {
        if (argument < notLessThanValue) {
            throw new IllegalArgumentException(CommonI18n.argumentMayNotBeLessThan.text(name, argument, notLessThanValue));
        }
    }

    /**
     * Check that the argument is not greater than the supplied value
     * 
     * @param argument The argument
     * @param notGreaterThanValue the value that is to be used to check the value
     * @param name The name of the argument
     * @throws IllegalArgumentException If argument is less than or equal to the supplied value
     */
    public static void isNotGreaterThan( int argument,
                                         int notGreaterThanValue,
                                         String name ) {
        if (argument > notGreaterThanValue) {
            throw new IllegalArgumentException(CommonI18n.argumentMayNotBeGreaterThan.text(name, argument, notGreaterThanValue));
        }
    }

    /**
     * Check that the argument is greater than the supplied value
     * 
     * @param argument The argument
     * @param greaterThanValue the value that is to be used to check the value
     * @param name The name of the argument
     * @throws IllegalArgumentException If argument is not greater than the supplied value
     */
    public static void isGreaterThan( int argument,
                                      int greaterThanValue,
                                      String name ) {
        if (argument <= greaterThanValue) {
            throw new IllegalArgumentException(CommonI18n.argumentMustBeGreaterThan.text(name, argument, greaterThanValue));
        }
    }

    /**
     * Check that the argument is greater than the supplied value
     * 
     * @param argument The argument
     * @param greaterThanValue the value that is to be used to check the value
     * @param name The name of the argument
     * @throws IllegalArgumentException If argument is not greater than the supplied value
     */
    public static void isGreaterThan( double argument,
                                      double greaterThanValue,
                                      String name ) {
        if (argument <= greaterThanValue) {
            throw new IllegalArgumentException(CommonI18n.argumentMustBeGreaterThan.text(name, argument, greaterThanValue));
        }
    }

    /**
     * Check that the argument is less than the supplied value
     * 
     * @param argument The argument
     * @param lessThanValue the value that is to be used to check the value
     * @param name The name of the argument
     * @throws IllegalArgumentException If argument is not less than the supplied value
     */
    public static void isLessThan( int argument,
                                   int lessThanValue,
                                   String name ) {
        if (argument >= lessThanValue) {
            throw new IllegalArgumentException(CommonI18n.argumentMustBeLessThan.text(name, argument, lessThanValue));
        }
    }

    /**
     * Check that the argument is greater than or equal to the supplied value
     * 
     * @param argument The argument
     * @param greaterThanOrEqualToValue the value that is to be used to check the value
     * @param name The name of the argument
     * @throws IllegalArgumentException If argument is not greater than or equal to the supplied value
     */
    public static void isGreaterThanOrEqualTo( int argument,
                                               int greaterThanOrEqualToValue,
                                               String name ) {
        if (argument < greaterThanOrEqualToValue) {
            throw new IllegalArgumentException(CommonI18n.argumentMustBeGreaterThanOrEqualTo.text(name,
                                                                                                  argument,
                                                                                                  greaterThanOrEqualToValue));
        }
    }

    /**
     * Check that the argument is less than or equal to the supplied value
     * 
     * @param argument The argument
     * @param lessThanOrEqualToValue the value that is to be used to check the value
     * @param name The name of the argument
     * @throws IllegalArgumentException If argument is not less than or equal to the supplied value
     */
    public static void isLessThanOrEqualTo( int argument,
                                            int lessThanOrEqualToValue,
                                            String name ) {
        if (argument > lessThanOrEqualToValue) {
            throw new IllegalArgumentException(CommonI18n.argumentMustBeLessThanOrEqualTo.text(name,
                                                                                               argument,
                                                                                               lessThanOrEqualToValue));
        }
    }

    /**
     * Check that the argument is non-negative (>=0).
     * 
     * @param argument The argument
     * @param name The name of the argument
     * @throws IllegalArgumentException If argument is negative (<0)
     */
    public static void isNonNegative( int argument,
                                      String name ) {
        if (argument < 0) {
            throw new IllegalArgumentException(CommonI18n.argumentMayNotBeNegative.text(name, argument));
        }
    }

    /**
     * Check that the argument is non-positive (<=0).
     * 
     * @param argument The argument
     * @param name The name of the argument
     * @throws IllegalArgumentException If argument is positive (>0)
     */
    public static void isNonPositive( int argument,
                                      String name ) {
        if (argument > 0) {
            throw new IllegalArgumentException(CommonI18n.argumentMayNotBePositive.text(name, argument));
        }
    }

    /**
     * Check that the argument is negative (<0).
     * 
     * @param argument The argument
     * @param name The name of the argument
     * @throws IllegalArgumentException If argument is non-negative (>=0)
     */
    public static void isNegative( int argument,
                                   String name ) {
        if (argument >= 0) {
            throw new IllegalArgumentException(CommonI18n.argumentMustBeNegative.text(name, argument));
        }
    }

    /**
     * Check that the argument is positive (>0).
     * 
     * @param argument The argument
     * @param name The name of the argument
     * @throws IllegalArgumentException If argument is non-positive (<=0)
     */
    public static void isPositive( int argument,
                                   String name ) {
        if (argument <= 0) {
            throw new IllegalArgumentException(CommonI18n.argumentMustBePositive.text(name, argument));
        }
    }

    // ########################## long METHODS ###################################

    /**
     * Check that the argument is non-negative (>=0).
     * 
     * @param argument The argument
     * @param name The name of the argument
     * @throws IllegalArgumentException If argument is negative (<0)
     */
    public static void isNonNegative( long argument,
                                      String name ) {
        if (argument < 0) {
            throw new IllegalArgumentException(CommonI18n.argumentMayNotBeNegative.text(name, argument));
        }
    }

    /**
     * Check that the argument is non-positive (<=0).
     * 
     * @param argument The argument
     * @param name The name of the argument
     * @throws IllegalArgumentException If argument is positive (>0)
     */
    public static void isNonPositive( long argument,
                                      String name ) {
        if (argument > 0) {
            throw new IllegalArgumentException(CommonI18n.argumentMayNotBePositive.text(name, argument));
        }
    }

    /**
     * Check that the argument is negative (<0).
     * 
     * @param argument The argument
     * @param name The name of the argument
     * @throws IllegalArgumentException If argument is non-negative (>=0)
     */
    public static void isNegative( long argument,
                                   String name ) {
        if (argument >= 0) {
            throw new IllegalArgumentException(CommonI18n.argumentMustBeNegative.text(name, argument));
        }
    }

    /**
     * Check that the argument is positive (>0).
     * 
     * @param argument The argument
     * @param name The name of the argument
     * @throws IllegalArgumentException If argument is non-positive (<=0)
     */
    public static void isPositive( long argument,
                                   String name ) {
        if (argument <= 0) {
            throw new IllegalArgumentException(CommonI18n.argumentMustBePositive.text(name, argument));
        }
    }

    // ########################## double METHODS ###################################

    /**
     * Check that the argument is non-negative (>=0).
     * 
     * @param argument The argument
     * @param name The name of the argument
     * @throws IllegalArgumentException If argument is negative (<0)
     */
    public static void isNonNegative( double argument,
                                      String name ) {
        if (argument < 0.0) {
            throw new IllegalArgumentException(CommonI18n.argumentMayNotBeNegative.text(name, argument));
        }
    }

    /**
     * Check that the argument is non-positive (<=0).
     * 
     * @param argument The argument
     * @param name The name of the argument
     * @throws IllegalArgumentException If argument is positive (>0)
     */
    public static void isNonPositive( double argument,
                                      String name ) {
        if (argument > 0.0) {
            throw new IllegalArgumentException(CommonI18n.argumentMayNotBePositive.text(name, argument));
        }
    }

    /**
     * Check that the argument is negative (<0).
     * 
     * @param argument The argument
     * @param name The name of the argument
     * @throws IllegalArgumentException If argument is non-negative (>=0)
     */
    public static void isNegative( double argument,
                                   String name ) {
        if (argument >= 0.0) {
            throw new IllegalArgumentException(CommonI18n.argumentMustBeNegative.text(name, argument));
        }
    }

    /**
     * Check that the argument is positive (>0).
     * 
     * @param argument The argument
     * @param name The name of the argument
     * @throws IllegalArgumentException If argument is non-positive (<=0)
     */
    public static void isPositive( double argument,
                                   String name ) {
        if (argument <= 0.0) {
            throw new IllegalArgumentException(CommonI18n.argumentMustBePositive.text(name, argument));
        }
    }

    /**
     * Check that the argument is not NaN.
     * 
     * @param argument The argument
     * @param name The name of the argument
     * @throws IllegalArgumentException If argument is NaN
     */
    public static void isNotNan( double argument,
                                 String name ) {
        if (Double.isNaN(argument)) {
            throw new IllegalArgumentException(CommonI18n.argumentMustBeNumber.text(name));
        }
    }

    // ########################## String METHODS ###################################

    /**
     * Check that the string is non-null and has length > 0
     * 
     * @param argument The argument
     * @param name The name of the argument
     * @throws IllegalArgumentException If value is null or length == 0
     */
    public static void isNotZeroLength( String argument,
                                        String name ) {
        isNotNull(argument, name);
        if (argument.length() <= 0) {
            throw new IllegalArgumentException(CommonI18n.argumentMayNotBeNullOrZeroLength.text(name));
        }
    }

    /**
     * Check that the string is not empty, is not null, and does not contain only whitespace.
     * 
     * @param argument String
     * @param name The name of the argument
     * @throws IllegalArgumentException If string is null or empty
     */
    public static void isNotEmpty( String argument,
                                   String name ) {
        isNotZeroLength(argument, name);
        if (argument != null && argument.trim().length() == 0) {
            throw new IllegalArgumentException(CommonI18n.argumentMayNotBeNullOrZeroLengthOrEmpty.text(name));
        }
    }

    // ########################## Object METHODS ###################################

    /**
     * Check that the specified argument is non-null
     * 
     * @param argument The argument
     * @param name The name of the argument
     * @throws IllegalArgumentException If argument is null
     */
    public static void isNotNull( Object argument,
                                  String name ) {
        if (argument == null) {
            throw new IllegalArgumentException(CommonI18n.argumentMayNotBeNull.text(name));
        }
    }

    /**
     * Returns the specified argument if it is not <code>null</code>.
     * 
     * @param <T>
     * @param argument The argument
     * @param name The name of the argument
     * @return The argument
     * @throws IllegalArgumentException If argument is <code>null</code>
     */
    public static <T> T getNotNull( T argument,
                                    String name ) {
        isNotNull(argument, name);
        return argument;
    }

    /**
     * Check that the argument is null
     * 
     * @param argument The argument
     * @param name The name of the argument
     * @throws IllegalArgumentException If value is non-null
     */
    public static void isNull( Object argument,
                               String name ) {
        if (argument != null) {
            throw new IllegalArgumentException(CommonI18n.argumentMustBeNull.text(name));
        }
    }

    /**
     * Check that the object is an instance of the specified Class
     * 
     * @param argument Value
     * @param expectedClass Class
     * @param name The name of the argument
     * @throws IllegalArgumentException If value is null
     */
    public static void isInstanceOf( Object argument,
                                     Class<?> expectedClass,
                                     String name ) {
        isNotNull(argument, name);
        if (!expectedClass.isInstance(argument)) {
            throw new IllegalArgumentException(CommonI18n.argumentMustBeInstanceOf.text(name,
                                                                                        argument.getClass(),
                                                                                        expectedClass.getName()));
        }
    }

    /**
     * Checks that the object is an instance of the specified Class and then returns the object cast to the specified Class
     * 
     * @param <C> the class type
     * @param argument Value
     * @param expectedClass Class
     * @param name The name of the argument
     * @return value cast to the specified Class
     * @throws IllegalArgumentException If value is not an instance of theClass.
     */
    // due to cast in return
    public static <C> C getInstanceOf( Object argument,
                                       Class<C> expectedClass,
                                       String name ) {
        isInstanceOf(argument, expectedClass, name);
        return expectedClass.cast(argument);
    }

    /**
     * Asserts that the specified first object is the same as (==) the specified second object.
     * 
     * @param <T>
     * @param argument The argument to assert as the same as <code>object</code>.
     * @param argumentName The name that will be used within the exception message for the argument should an exception be thrown
     * @param object The object to assert as the same as <code>argument</code>.
     * @param objectName The name that will be used within the exception message for <code>object</code> should an exception be
     *        thrown; if <code>null</code> and <code>object</code> is not <code>null</code>, <code>object.toString()</code> will
     *        be used.
     * @throws IllegalArgumentException If the specified objects are not the same.
     */
    public static <T> void isSame( final T argument,
                                   String argumentName,
                                   final T object,
                                   String objectName ) {
        if (argument != object) {
            if (objectName == null) objectName = getObjectName(object);
            throw new IllegalArgumentException(CommonI18n.argumentMustBeSameAs.text(argumentName, objectName));
        }
    }

    /**
     * Asserts that the specified first object is not the same as (==) the specified second object.
     * 
     * @param <T>
     * @param argument The argument to assert as not the same as <code>object</code>.
     * @param argumentName The name that will be used within the exception message for the argument should an exception be thrown
     * @param object The object to assert as not the same as <code>argument</code>.
     * @param objectName The name that will be used within the exception message for <code>object</code> should an exception be
     *        thrown; if <code>null</code> and <code>object</code> is not <code>null</code>, <code>object.toString()</code> will
     *        be used.
     * @throws IllegalArgumentException If the specified objects are the same.
     */
    public static <T> void isNotSame( final T argument,
                                      String argumentName,
                                      final T object,
                                      String objectName ) {
        if (argument == object) {
            if (objectName == null) objectName = getObjectName(object);
            throw new IllegalArgumentException(CommonI18n.argumentMustNotBeSameAs.text(argumentName, objectName));
        }
    }

    /**
     * Asserts that the specified first object is {@link Object#equals(Object) equal to} the specified second object. This method
     * does take null references into consideration.
     * 
     * @param <T>
     * @param argument The argument to assert equal to <code>object</code>.
     * @param argumentName The name that will be used within the exception message for the argument should an exception be thrown
     * @param object The object to assert as equal to <code>argument</code>.
     * @param objectName The name that will be used within the exception message for <code>object</code> should an exception be
     *        thrown; if <code>null</code> and <code>object</code> is not <code>null</code>, <code>object.toString()</code> will
     *        be used.
     * @throws IllegalArgumentException If the specified objects are not equal.
     */
    public static <T> void isEquals( final T argument,
                                     String argumentName,
                                     final T object,
                                     String objectName ) {
        if (argument == null) {
            if (object == null) return;
            // fall through ... one is null
        } else {
            if (argument.equals(object)) return;
            // fall through ... they are not equal
        }
        if (objectName == null) objectName = getObjectName(object);
        throw new IllegalArgumentException(CommonI18n.argumentMustBeEquals.text(argumentName, objectName));
    }

    /**
     * Asserts that the specified first object is not {@link Object#equals(Object) equal to} the specified second object. This
     * method does take null references into consideration.
     * 
     * @param <T>
     * @param argument The argument to assert equal to <code>object</code>.
     * @param argumentName The name that will be used within the exception message for the argument should an exception be thrown
     * @param object The object to assert as equal to <code>argument</code>.
     * @param objectName The name that will be used within the exception message for <code>object</code> should an exception be
     *        thrown; if <code>null</code> and <code>object</code> is not <code>null</code>, <code>object.toString()</code> will
     *        be used.
     * @throws IllegalArgumentException If the specified objects are equals.
     */
    public static <T> void isNotEquals( final T argument,
                                        String argumentName,
                                        final T object,
                                        String objectName ) {
        if (argument == null) {
            if (object != null) return;
            // fall through ... both are null
        } else {
            if (!argument.equals(object)) return; // handles object==null
            // fall through ... they are equal
        }
        if (objectName == null) objectName = getObjectName(object);
        throw new IllegalArgumentException(CommonI18n.argumentMustNotBeEquals.text(argumentName, objectName));
    }

    // ########################## ITERATOR METHODS ###################################

    /**
     * Checks that the iterator is not empty, and throws an exception if it is.
     * 
     * @param argument the iterator to check
     * @param name The name of the argument
     * @throws IllegalArgumentException If iterator is empty (i.e., iterator.hasNext() returns false)
     */
    public static void isNotEmpty( Iterator<?> argument,
                                   String name ) {
        isNotNull(argument, name);
        if (!argument.hasNext()) {
            throw new IllegalArgumentException(CommonI18n.argumentMayNotBeEmpty.text(name));
        }
    }

    // ########################## COLLECTION METHODS ###################################

    /**
     * Check that the collection is not empty
     * 
     * @param argument Collection
     * @param name The name of the argument
     * @throws IllegalArgumentException If collection is null or empty
     */
    public static void isNotEmpty( Collection<?> argument,
                                   String name ) {
        isNotNull(argument, name);
        if (argument.isEmpty()) {
            throw new IllegalArgumentException(CommonI18n.argumentMayNotBeEmpty.text(name));
        }
    }

    /**
     * Check that the map is not empty
     * 
     * @param argument Map
     * @param name The name of the argument
     * @throws IllegalArgumentException If map is null or empty
     */
    public static void isNotEmpty( Map<?, ?> argument,
                                   String name ) {
        isNotNull(argument, name);
        if (argument.isEmpty()) {
            throw new IllegalArgumentException(CommonI18n.argumentMayNotBeEmpty.text(name));
        }
    }

    /**
     * Check that the array is empty
     * 
     * @param argument Array
     * @param name The name of the argument
     * @throws IllegalArgumentException If array is not empty
     */
    public static void isEmpty( Object[] argument,
                                String name ) {
        isNotNull(argument, name);
        if (argument.length > 0) {
            throw new IllegalArgumentException(CommonI18n.argumentMustBeEmpty.text(name));
        }
    }

    /**
     * Check that the array is not empty
     * 
     * @param argument Array
     * @param name The name of the argument
     * @throws IllegalArgumentException If array is null or empty
     */
    public static void isNotEmpty( Object[] argument,
                                   String name ) {
        isNotNull(argument, name);
        if (argument.length == 0) {
            throw new IllegalArgumentException(CommonI18n.argumentMayNotBeEmpty.text(name));
        }
    }

    protected static String getObjectName( Object obj ) {
        return obj == null ? null : "'" + obj.toString() + "'";
    }

    /**
     * Check that the collection contains the value
     * 
     * @param argument Collection to check
     * @param value Value to check for, may be null
     * @param name The name of the argument
     * @throws IllegalArgumentException If collection is null or doesn't contain value
     */
    public static void contains( Collection<?> argument,
                                 Object value,
                                 String name ) {
        isNotNull(argument, name);
        if (!argument.contains(value)) {
            throw new IllegalArgumentException(CommonI18n.argumentDidNotContainObject.text(name, getObjectName(value)));
        }
    }

    /**
     * Check that the map contains the key
     * 
     * @param argument Map to check
     * @param key Key to check for, may be null
     * @param name The name of the argument
     * @throws IllegalArgumentException If map is null or doesn't contain key
     */
    public static void containsKey( Map<?, ?> argument,
                                    Object key,
                                    String name ) {
        isNotNull(argument, name);
        if (!argument.containsKey(key)) {
            throw new IllegalArgumentException(CommonI18n.argumentDidNotContainKey.text(name, getObjectName(key)));
        }
    }

    /**
     * Check that the collection is not null and contains no nulls
     * 
     * @param argument Array
     * @param name The name of the argument
     * @throws IllegalArgumentException If array is null or has null values
     */
    public static void containsNoNulls( Iterable<?> argument,
                                        String name ) {
        isNotNull(argument, name);
        int i = 0;
        for (Object object : argument) {
            if (object == null) {
                throw new IllegalArgumentException(CommonI18n.argumentMayNotContainNullValue.text(name, i));
            }
            ++i;
        }
    }

    /**
     * Check that the array is not null and contains no nulls
     * 
     * @param argument Array
     * @param name The name of the argument
     * @throws IllegalArgumentException If array is null or has null values
     */
    public static void containsNoNulls( Object[] argument,
                                        String name ) {
        isNotNull(argument, name);
        int i = 0;
        for (Object object : argument) {
            if (object == null) {
                throw new IllegalArgumentException(CommonI18n.argumentMayNotContainNullValue.text(name, i));
            }
            ++i;
        }
    }

    /**
     * Check that the collection contains at least the supplied number of elements
     * 
     * @param argument Collection
     * @param minimumSize the minimum size
     * @param name The name of the argument
     * @throws IllegalArgumentException If collection has a size smaller than the supplied value
     */
    public static void hasSizeOfAtLeast( Collection<?> argument,
                                         int minimumSize,
                                         String name ) {
        isNotNull(argument, name);
        if (argument.size() < minimumSize) {
            throw new IllegalArgumentException(CommonI18n.argumentMustBeOfMinimumSize.text(name,
                                                                                           Collection.class.getSimpleName(),
                                                                                           argument.size(),
                                                                                           minimumSize));
        }
    }

    /**
     * Check that the collection contains no more than the supplied number of elements
     * 
     * @param argument Collection
     * @param maximumSize the maximum size
     * @param name The name of the argument
     * @throws IllegalArgumentException If collection has a size smaller than the supplied value
     */
    public static void hasSizeOfAtMost( Collection<?> argument,
                                        int maximumSize,
                                        String name ) {
        isNotNull(argument, name);
        if (argument.size() > maximumSize) {
            throw new IllegalArgumentException(CommonI18n.argumentMustBeOfMinimumSize.text(name,
                                                                                           Collection.class.getSimpleName(),
                                                                                           argument.size(),
                                                                                           maximumSize));
        }
    }

    /**
     * Check that the map contains at least the supplied number of entries
     * 
     * @param argument the map
     * @param minimumSize the minimum size
     * @param name The name of the argument
     * @throws IllegalArgumentException If the map has a size smaller than the supplied value
     */
    public static void hasSizeOfAtLeast( Map<?, ?> argument,
                                         int minimumSize,
                                         String name ) {
        isNotNull(argument, name);
        if (argument.size() < minimumSize) {
            throw new IllegalArgumentException(CommonI18n.argumentMustBeOfMinimumSize.text(name,
                                                                                           Map.class.getSimpleName(),
                                                                                           argument.size(),
                                                                                           minimumSize));
        }
    }

    /**
     * Check that the map contains no more than the supplied number of entries
     * 
     * @param argument the map
     * @param maximumSize the maximum size
     * @param name The name of the argument
     * @throws IllegalArgumentException If the map has a size smaller than the supplied value
     */
    public static void hasSizeOfAtMost( Map<?, ?> argument,
                                        int maximumSize,
                                        String name ) {
        isNotNull(argument, name);
        if (argument.size() > maximumSize) {
            throw new IllegalArgumentException(CommonI18n.argumentMustBeOfMinimumSize.text(name,
                                                                                           Map.class.getSimpleName(),
                                                                                           argument.size(),
                                                                                           maximumSize));
        }
    }

    /**
     * Check that the array contains at least the supplied number of elements
     * 
     * @param argument the array
     * @param minimumSize the minimum size
     * @param name The name of the argument
     * @throws IllegalArgumentException If the array has a size smaller than the supplied value
     */
    public static void hasSizeOfAtLeast( Object[] argument,
                                         int minimumSize,
                                         String name ) {
        isNotNull(argument, name);
        if (argument.length < minimumSize) {
            throw new IllegalArgumentException(CommonI18n.argumentMustBeOfMinimumSize.text(name,
                                                                                           Object[].class.getSimpleName(),
                                                                                           argument.length,
                                                                                           minimumSize));
        }
    }

    /**
     * Check that the array contains no more than the supplied number of elements
     * 
     * @param argument the array
     * @param maximumSize the maximum size
     * @param name The name of the argument
     * @throws IllegalArgumentException If the array has a size smaller than the supplied value
     */
    public static void hasSizeOfAtMost( Object[] argument,
                                        int maximumSize,
                                        String name ) {
        isNotNull(argument, name);
        if (argument.length > maximumSize) {
            throw new IllegalArgumentException(CommonI18n.argumentMustBeOfMinimumSize.text(name,
                                                                                           Object[].class.getSimpleName(),
                                                                                           argument.length,
                                                                                           maximumSize));
        }
    }

    private CheckArg() {
        // prevent construction
    }
}
