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
package org.modeshape.graph.property;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.UUID;
import net.jcip.annotations.ThreadSafe;
import org.modeshape.common.text.TextDecoder;
import org.modeshape.common.text.TextEncoder;

/**
 * A factory for {@link Property} values. Each create method may throw one of these exceptions when attempting to convert a
 * supplied value to the {@link #getPropertyType() factory's type}:
 * <ul>
 * <li>{@link IllegalArgumentException} - If the supplied value is invalid in respect to the conversion being attempted.</li>
 * <li>{@link UnsupportedOperationException} - If a conversion from the supplied value is not supported.</li>
 * <li>{@link IoException} - If an unexpected problem occurs during the conversion (such as an {@link IOException}).</li>
 * </ul>
 * 
 * @param <T> the type of value to create
 */
@ThreadSafe
public interface ValueFactory<T> {

    static final TextDecoder DEFAULT_DECODER = Path.NO_OP_DECODER;
    static final TextEncoder DEFAULT_ENCODER = Path.NO_OP_ENCODER;

    /**
     * Get the {@link PropertyType type} of values created by this factory.
     * 
     * @return the value type; never null
     */
    PropertyType getPropertyType();

    /**
     * Create a value from a string, using no decoding.
     * 
     * @param value the string from which the value is to be created
     * @return the value, or null if the supplied string is null
     * @throws ValueFormatException if the conversion from a string could not be performed
     * @see #create(String, TextDecoder)
     */
    T create( String value ) throws ValueFormatException;

    /**
     * Create a value from a string, using the supplied decoder.
     * 
     * @param value the string from which the value is to be created
     * @param decoder the decoder that should be used; if null, the {@link #DEFAULT_DECODER default decoder} is used
     * @return the value, or null if the supplied string is null
     * @throws ValueFormatException if the conversion from a string could not be performed
     * @see #create(String)
     */
    T create( String value,
              TextDecoder decoder ) throws ValueFormatException;

    /**
     * Create a value from an integer.
     * 
     * @param value the integer from which the value is to be created
     * @return the value; never null
     * @throws ValueFormatException if the conversion from an integer could not be performed
     */
    T create( int value ) throws ValueFormatException;

    /**
     * Create a long from a string.
     * 
     * @param value the string from which the long is to be created
     * @return the value; never null
     * @throws ValueFormatException if the conversion from a long could not be performed
     */
    T create( long value ) throws ValueFormatException;

    /**
     * Create a boolean from a string.
     * 
     * @param value the boolean from which the value is to be created
     * @return the value; never null
     * @throws ValueFormatException if the conversion from a boolean could not be performed
     */
    T create( boolean value ) throws ValueFormatException;

    /**
     * Create a value from a float.
     * 
     * @param value the float from which the value is to be created
     * @return the value; never null
     * @throws ValueFormatException if the conversion from a float could not be performed
     */
    T create( float value ) throws ValueFormatException;

    /**
     * Create a value from a double.
     * 
     * @param value the double from which the value is to be created
     * @return the value; never null
     * @throws ValueFormatException if the conversion from a double could not be performed
     */
    T create( double value ) throws ValueFormatException;

    /**
     * Create a value from a decimal.
     * 
     * @param value the decimal from which the value is to be created
     * @return the value, or null if the supplied decimal is null
     * @throws ValueFormatException if the conversion from a decimal could not be performed
     */
    T create( BigDecimal value ) throws ValueFormatException;

    /**
     * Create a value from a Calendar instance.
     * 
     * @param value the Calendar instance from which the value is to be created
     * @return the value, or null if the supplied Calendar is null
     * @throws ValueFormatException if the conversion from a Calendar could not be performed
     */
    T create( Calendar value ) throws ValueFormatException;

    /**
     * Create a value from a date.
     * 
     * @param value the date from which the value is to be created
     * @return the value, or null if the supplied date is null
     * @throws ValueFormatException if the conversion from a Date could not be performed
     */
    T create( Date value ) throws ValueFormatException;

    /**
     * Create a value from a date-time instant.
     * 
     * @param value the date-time instant from which the value is to be created
     * @return the value, or null if the supplied date is null
     * @throws ValueFormatException if the conversion from a Date could not be performed
     */
    T create( DateTime value ) throws ValueFormatException;

    /**
     * Create a value from a name.
     * 
     * @param value the name from which the value is to be created
     * @return the value, or null if the supplied name is null
     * @throws ValueFormatException if the conversion from a name could not be performed
     */
    T create( Name value ) throws ValueFormatException;

    /**
     * Create a value from a path.
     * 
     * @param value the path from which the value is to be created
     * @return the value, or null if the supplied path is null
     * @throws ValueFormatException if the conversion from a path could not be performed
     */
    T create( Path value ) throws ValueFormatException;

    /**
     * Create a value from a path segment.
     * 
     * @param value the path segment from which the value is to be created
     * @return the value, or null if the supplied path segment is null
     * @throws ValueFormatException if the conversion from a path could not be performed
     */
    T create( Path.Segment value ) throws ValueFormatException;

    /**
     * Create a value from a reference.
     * 
     * @param value the reference from which the value is to be created
     * @return the value, or null if the supplied reference is null
     * @throws ValueFormatException if the conversion from a reference could not be performed
     */
    T create( Reference value ) throws ValueFormatException;

    /**
     * Create a value from a URI.
     * 
     * @param value the URI from which the value is to be created
     * @return the value, or null if the supplied URI is null
     * @throws ValueFormatException if the conversion from a URI could not be performed
     */
    T create( URI value ) throws ValueFormatException;

    /**
     * Create a value from a UUID.
     * 
     * @param value the UUID from which the value is to be created
     * @return the value, or null if the supplied URI is null
     * @throws ValueFormatException if the conversion from a UUID could not be performed
     */
    T create( UUID value ) throws ValueFormatException;

    /**
     * Create a value from the binary content given by the supplied array.
     * 
     * @param value the content to be used to create the value
     * @return the value, or null if the supplied stream is null
     * @throws ValueFormatException if the conversion from a byte array could not be performed
     */
    T create( byte[] value ) throws ValueFormatException;

    /**
     * Create a value from the binary content given by the supplied stream.
     * 
     * @param value the binary object to be used to create the value
     * @return the value, or null if the supplied stream is null
     * @throws ValueFormatException if the conversion from the binary object could not be performed
     * @throws IoException If an unexpected problem occurs while accessing the supplied binary value (such as an
     *         {@link IOException}).
     */
    T create( Binary value ) throws ValueFormatException, IoException;

    /**
     * Create a value from the binary content given by the supplied stream.
     * 
     * @param stream the stream containing the content to be used to create the value
     * @param approximateLength the approximate length of the content (in bytes)
     * @return the value, or null if the supplied stream is null
     * @throws ValueFormatException if the conversion from an input stream could not be performed
     * @throws IoException If an unexpected problem occurs while accessing the supplied stream (such as an {@link IOException}).
     */
    T create( InputStream stream,
              long approximateLength ) throws ValueFormatException, IoException;

    /**
     * Create a value from a the binary content given by the supplied reader.
     * 
     * @param reader the reader containing the content to be used to create the value
     * @param approximateLength the approximate length of the content (in bytes)
     * @return the value, or null if the supplied string is null
     * @throws ValueFormatException if the conversion from a reader could not be performed
     * @throws IoException If an unexpected problem occurs while accessing the supplied reader (such as an {@link IOException}).
     */
    T create( Reader reader,
              long approximateLength ) throws ValueFormatException, IoException;

    /**
     * Create a value from the specified information by determining which other <code>create</code> method applies and delegating
     * to that method. Note that this method only will call <code>create</code> methods that take a single parameter; so this
     * excludes {@link #create(InputStream, long)}, {@link #create(Reader, long)} and {@link #create(String, TextDecoder)}.
     * 
     * @param value the value
     * @return the new value, or null if the supplied parameter is null
     * @throws ValueFormatException if the conversion from an object could not be performed
     * @throws IoException If an unexpected problem occurs while accessing the supplied binary value (such as an
     *         {@link IOException}).
     */
    T create( Object value ) throws ValueFormatException, IoException;

    /**
     * Create an array of values from an array of string values, using no decoding.
     * 
     * @param values the values
     * @return the values, or null if the supplied string is null
     * @throws ValueFormatException if the conversion from a string array could not be performed
     * @see #create(String[], TextDecoder)
     */
    T[] create( String[] values ) throws ValueFormatException;

    /**
     * Create an array of values from an array of strings, using the supplied decoder.
     * 
     * @param values the string values from which the values are to be created
     * @param decoder the decoder that should be used; if null, the {@link #DEFAULT_DECODER default decoder} is used
     * @return the value, or null if the supplied string is null
     * @throws ValueFormatException if the conversion from a string array could not be performed
     * @see #create(String)
     */
    T[] create( String[] values,
                TextDecoder decoder ) throws ValueFormatException;

    /**
     * Create an array of values from an array of integers.
     * 
     * @param values the integers from which the values are to be created
     * @return the values, or null if the supplied array is null
     * @throws ValueFormatException if the conversion from an integer array could not be performed
     */
    T[] create( int[] values ) throws ValueFormatException;

    /**
     * Create an array of values from an array of longs.
     * 
     * @param values the longs from which the values are to be created
     * @return the values, or null if the supplied array is null
     * @throws ValueFormatException if the conversion from an array of longs could not be performed
     */
    T[] create( long[] values ) throws ValueFormatException;

    /**
     * Create an array of values from an array of booleans.
     * 
     * @param values the booleans from which the values are to be created
     * @return the values, or null if the supplied array is null
     * @throws ValueFormatException if the conversion from an array of booleans could not be performed
     */
    T[] create( boolean[] values ) throws ValueFormatException;

    /**
     * Create an array of values from an array of floats.
     * 
     * @param values the floats from which the values are to be created
     * @return the values, or null if the supplied array is null
     * @throws ValueFormatException if the conversion from an array of floats could not be performed
     */
    T[] create( float[] values ) throws ValueFormatException;

    /**
     * Create an array of values from an array of doubles.
     * 
     * @param values the doubles from which the values are to be created
     * @return the values, or null if the supplied array is null
     * @throws ValueFormatException if the conversion from an array of doubles could not be performed
     */
    T[] create( double[] values ) throws ValueFormatException;

    /**
     * Create an array of values from an array of decimal values.
     * 
     * @param values the decimals from which the values are to be created
     * @return the values, or null if the supplied array is null
     * @throws ValueFormatException if the conversion from an array of decimal values could not be performed
     */
    T[] create( BigDecimal[] values ) throws ValueFormatException;

    /**
     * Create an array of values from an array of Calendar instances.
     * 
     * @param values the Calendar instances from which the values are to be created
     * @return the values, or null if the supplied array is null
     * @throws ValueFormatException if the conversion from an array of calendar instances could not be performed
     */
    T[] create( Calendar[] values ) throws ValueFormatException;

    /**
     * Create an array of values from an array of dates.
     * 
     * @param values the dates from which the values are to be created
     * @return the values, or null if the supplied array is null
     * @throws ValueFormatException if the conversion from an array of date values could not be performed
     */
    T[] create( Date[] values ) throws ValueFormatException;

    /**
     * Create an array of values from an array of {@link DateTime} instants.
     * 
     * @param values the instants from which the values are to be created
     * @return the values, or null if the supplied array is null
     * @throws ValueFormatException if the conversion from an array of date values could not be performed
     */
    T[] create( DateTime[] values ) throws ValueFormatException;

    /**
     * Create an array of values from an array of names.
     * 
     * @param values the names from which the values are to be created
     * @return the values, or null if the supplied array is null
     * @throws ValueFormatException if the conversion from an array of names could not be performed
     */
    T[] create( Name[] values ) throws ValueFormatException;

    /**
     * Create an array of values from an array of paths.
     * 
     * @param values the paths from which the values are to be created
     * @return the values, or null if the supplied array is null
     * @throws ValueFormatException if the conversion from an array of paths could not be performed
     */
    T[] create( Path[] values ) throws ValueFormatException;

    /**
     * Create an array of values from an array of references.
     * 
     * @param values the references from which the values are to be created
     * @return the values, or null if the supplied array is null
     * @throws ValueFormatException if the conversion from an array of references could not be performed
     */
    T[] create( Reference[] values ) throws ValueFormatException;

    /**
     * Create an array of values from an array of URIs.
     * 
     * @param values the URIs from which the values are to be created
     * @return the values, or null if the supplied array is null
     * @throws ValueFormatException if the conversion from an array of URIs could not be performed
     */
    T[] create( URI[] values ) throws ValueFormatException;

    /**
     * Create an array of values from an array of UUIDs.
     * 
     * @param values the UUIDs from which the values are to be created
     * @return the values, or null if the supplied array is null
     * @throws ValueFormatException if the conversion from an array of UUIDs could not be performed
     */
    T[] create( UUID[] values ) throws ValueFormatException;

    /**
     * Create an array of values from the array of binary content.
     * 
     * @param values the array of content to be used to create the values
     * @return the value, or null if the supplied array is null
     * @throws ValueFormatException if the conversion from an array of byte arrays could not be performed
     */
    T[] create( byte[][] values ) throws ValueFormatException;

    /**
     * Create an array of values from the array of binary objects.
     * 
     * @param values the values
     * @return the new value, or null if the supplied parameter is null
     * @throws ValueFormatException if the conversion from an array of objects could not be performed
     * @throws IoException If an unexpected problem occurs during the conversion.
     */
    T[] create( Binary[] values ) throws ValueFormatException, IoException;

    /**
     * Create an array of values from the specified information by determining which other <code>create</code> method applies for
     * each object and then delegating to that method. Note that this method will not consider {@link #create(InputStream, long)},
     * {@link #create(Reader, long)} and {@link #create(String, TextDecoder)}.
     * 
     * @param values the values
     * @return the new value, or null if the supplied parameter is null
     * @throws ValueFormatException if the conversion from an array of objects could not be performed
     * @throws IoException If an unexpected problem occurs during the conversion.
     */
    T[] create( Object[] values ) throws ValueFormatException, IoException;

    /**
     * Create an iterator over the values (of an unknown type). The factory converts any values as required. Note that this method
     * will not consider {@link #create(InputStream, long)}, {@link #create(Reader, long)} and
     * {@link #create(String, TextDecoder)}.
     * <p>
     * This is useful to use when iterating over the {@link Property#getValues() values} of a {@link Property}.
     * </p>
     * 
     * @param values the values
     * @return the iterator of type <code>T</code> over the values, or null if the supplied parameter is null
     * @throws ValueFormatException if the conversion from an iterator of objects could not be performed
     * @throws IoException If an unexpected problem occurs during the conversion.
     * @see Property#getValues()
     */
    Iterator<T> create( Iterator<?> values ) throws ValueFormatException, IoException;

    /**
     * Create an iterable with the values (of an unknown type). The factory converts any values as required. Note that this method
     * will not consider {@link #create(InputStream, long)}, {@link #create(Reader, long)} and
     * {@link #create(String, TextDecoder)}.
     * <p>
     * This is useful to use when converting all the {@link Property#getValues() values} of a {@link Property}.
     * </p>
     * Example:
     * 
     * <pre>
     *      Property property = ...
     *      ExecutionContext executionContext = ...
     *      ValueFactory&lt;String&gt; stringFactory = executionContext.getValueFactories().getStringFactory();
     *      for (String token : stringFactory.create(property)) {
     *          ...
     *      }
     * </pre>
     * 
     * @param valueIterable the values
     * @return the iterator of type <code>T</code> over the values, or null if the supplied parameter is null
     * @throws ValueFormatException if the conversion from an iterator of objects could not be performed
     * @throws IoException If an unexpected problem occurs during the conversion.
     * @see Property#getValues()
     */
    Iterable<T> create( Iterable<?> valueIterable ) throws ValueFormatException, IoException;
}
