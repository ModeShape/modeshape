package org.jboss.dna.common.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * Utility class that checks arguments to methods. This class is to be used only in API methods, where failure to supply correct
 * arguments should result in a useful error message. In all cases, use the <code>assert</code> statement.
 */
public final class ArgCheck {

	// ########################## int METHODS ###################################

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
			throw new IllegalArgumentException("The \"" + name + "\" argument value, " + argument + ", may not be negative");
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
			throw new IllegalArgumentException("The \"" + name + "\" argument value, " + argument + ", may not be positive");
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
			throw new IllegalArgumentException("The \"" + name + "\" argument value, " + argument + ", must be negative");
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
			throw new IllegalArgumentException("The \"" + name + "\" argument value, " + argument + ", must be positive");
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
			throw new IllegalArgumentException("The \"" + name + "\" argument value, " + argument + ", may not be negative");
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
			throw new IllegalArgumentException("The \"" + name + "\" argument value, " + argument + ", may not be positive");
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
			throw new IllegalArgumentException("The \"" + name + "\" argument value, " + argument + ", must be negative");
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
			throw new IllegalArgumentException("The \"" + name + "\" argument value, " + argument + ", must be positive");
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
			throw new IllegalArgumentException("The \"" + name + "\" argument value, " + argument + ", may not be negative");
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
			throw new IllegalArgumentException("The \"" + name + "\" argument value, " + argument + ", may not be positive");
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
			throw new IllegalArgumentException("The \"" + name + "\" argument value, " + argument + ", must be negative");
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
			throw new IllegalArgumentException("The \"" + name + "\" argument value, " + argument + ", must be positive");
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
			throw new IllegalArgumentException("The \"" + name + "\" argument value must be a number");
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
			throw new IllegalArgumentException("The \"" + name + "\" argument may not be null or zero-length");
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
			throw new IllegalArgumentException("The \"" + name + "\" argument may not be empty or contain only whitespace");
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
			throw new IllegalArgumentException("The \"" + name + "\" argument must not be null.");
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
			throw new IllegalArgumentException("The \"" + name + "\" argument must be null");
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
	                                 Class expectedClass,
	                                 String name ) {
		isNotNull(argument, name);
		if (!expectedClass.isInstance(argument)) {
			throw new IllegalArgumentException("The \"" + name + "\" argument was an instance of " + argument.getClass()
			                                   + " but was expected to be an instance of " + expectedClass.getName());
		}
	}

	/**
	 * Checks that the object is an instance of the specified Class and then returns the object cast to the specified Class
	 *
	 * @param <C>
	 * @param argument Value
	 * @param expectedClass Class
	 * @param name The name of the argument
	 * @return value cast to the specified Class
	 * @throws IllegalArgumentException If value is not an instance of theClass.
	 */
	@SuppressWarnings( "unchecked" )
	// due to cast in return
	public static <C> C getInstanceOf( Object argument,
	                                   Class<C> expectedClass,
	                                   String name ) {
		isInstanceOf(argument, expectedClass, name);
		return (C)argument;
	}

	/**
	 * Asserts that the specified first object is the same as (==) the specified second object.
	 *
	 * @param <T>
	 * @param argument The argument to assert as the same as <code>object</code>.
	 * @param argumentName The name that will be used within the exception message for the argument should an exception be thrown
	 * @param object The object to assert as the same as <code>argument</code>.
	 * @param objectName The name that will be used within the exception message for <code>object</code> should an exception be
	 *        thrown; if <code>null</code> and <code>object</code> is not <code>null</code>, <code>object.toString()</code>
	 *        will be used.
	 * @throws IllegalArgumentException If the specified objects are not the same.
	 */
	public static <T> void isSame( final T argument,
	                               String argumentName,
	                               final T object,
	                               String objectName ) {
		if (argument != object) {
			if (objectName == null) objectName = getObjectName(object);
			throw new IllegalArgumentException("The \"" + argumentName + "\" is not the same as \"" + objectName + '"');
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
	 *        thrown; if <code>null</code> and <code>object</code> is not <code>null</code>, <code>object.toString()</code>
	 *        will be used.
	 * @throws IllegalArgumentException If the specified objects are the same.
	 */
	public static <T> void isNotSame( final T argument,
	                                  String argumentName,
	                                  final T object,
	                                  String objectName ) {
		if (argument == object) {
			if (objectName == null) objectName = getObjectName(object);
			throw new IllegalArgumentException("The \"" + argumentName + "\" is the same as \"" + objectName + '"');
		}
	}

	// ########################## ITERATOR METHODS ###################################

	/**
	 * Checks that the iterator is not empty, and throws an exception if it is.
	 *
	 * @param argument the iterator to check
	 * @param name The name of the argument
	 * @throws IllegalArgumentException If iterator is empty (i.e., iterator.hasNext() returns false)
	 */
	public static void isNotEmpty( Iterator argument,
	                               String name ) {
		isNotNull(argument, name);
		if (!argument.hasNext()) {
			throw new IllegalArgumentException("The \"" + name + "\" argument may not be empty");
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
	public static void isNotEmpty( Collection argument,
	                               String name ) {
		isNotNull(argument, name);
		if (argument.isEmpty()) {
			throw new IllegalArgumentException("The \"" + name + "\" argument may not be empty");
		}
	}

	/**
	 * Check that the map is not empty
	 *
	 * @param argument Map
	 * @param name The name of the argument
	 * @throws IllegalArgumentException If map is null or empty
	 */
	public static void isNotEmpty( Map argument,
	                               String name ) {
		isNotNull(argument, name);
		if (argument.isEmpty()) {
			throw new IllegalArgumentException("The \"" + name + "\" argument may not be empty");
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
			throw new IllegalArgumentException("The \"" + name + "\" argument may not be empty");
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
	public static void contains( Collection argument,
	                             Object value,
	                             String name ) {
		isNotNull(argument, name);
		if (!argument.contains(value)) {
			throw new IllegalArgumentException("The \"" + name + "\" argument did not contain the expected object \""
			                                   + getObjectName(value) + '"');
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
	public static void containsKey( Map argument,
	                                Object key,
	                                String name ) {
		isNotNull(argument, name);
		if (!argument.containsKey(key)) {
			throw new IllegalArgumentException("The \"" + name + "\" argument did not contain the expected key \""
			                                   + getObjectName(key) + '"');
		}
	}

	/**
	 * Check that the collection is not null and contains no nulls
	 *
	 * @param argument Array
	 * @param name The name of the argument
	 * @throws IllegalArgumentException If array is null or has null values
	 */
	public static void containsNoNulls( Collection argument,
	                                    String name ) {
		isNotNull(argument, name);
		int i = 0;
		for (Object object : argument) {
			if (object == null) {
				throw new IllegalArgumentException("The \"" + name
				                                   + "\" argument may not contain a null value (first null found at position "
				                                   + i + ")");
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
				throw new IllegalArgumentException("The \"" + name
				                                   + "\" argument may not contain a null value (first null found at position "
				                                   + i + ")");
			}
			++i;
		}
	}

	private ArgCheck() {
		// prevent construction
	}
}
