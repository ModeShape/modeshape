/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.spi.graph;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import org.jboss.dna.common.text.TextDecoder;
import org.jboss.dna.common.text.TextEncoder;

/**
 * A factory for {@link Property} values. Each create method may throw one of these exceptions when attempting to convert a
 * supplied value to the {@link #getPropertyType() factory's type}:
 * <ul>
 * <li>{@link IllegalArgumentException} - If the supplied value is invalid in respect to the conversion being attempted.</li>
 * <li>{@link UnsupportedOperationException} - If a conversion from the supplied value is not supported.</li>
 * <li>{@link IoException} - If an unexpected problem occurs during the conversion (such as an {@link IOException}).</li>
 * </ul>
 * 
 * @author Randall Hauch
 * @author John Verhaeg
 * @param <T> the type of value to create
 */
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
     * @throws IoException If an unexpected problem occurs during the conversion.
     * @see #create(String, TextDecoder)
     */
    T create( String value ) throws IoException;

    /**
     * Create a value from a string, using the supplied decoder.
     * 
     * @param value the string from which the value is to be created
     * @param decoder the decoder that should be used; if null, the {@link #DEFAULT_DECODER default decoder} is used
     * @return the value, or null if the supplied string is null
     * @throws IoException If an unexpected problem occurs during the conversion.
     * @see #create(String)
     */
    T create( String value,
              TextDecoder decoder ) throws IoException;

    /**
     * Create a value from an integer.
     * 
     * @param value the integer from which the value is to be created
     * @return the value; never null
     * @throws IoException If an unexpected problem occurs during the conversion.
     */
    T create( int value ) throws IoException;

    /**
     * Create a long from a string.
     * 
     * @param value the string from which the long is to be created
     * @return the value; never null
     * @throws IoException If an unexpected problem occurs during the conversion.
     */
    T create( long value ) throws IoException;

    /**
     * Create a boolean from a string.
     * 
     * @param value the boolean from which the value is to be created
     * @return the value; never null
     * @throws IoException If an unexpected problem occurs during the conversion.
     */
    T create( boolean value ) throws IoException;

    /**
     * Create a value from a float.
     * 
     * @param value the float from which the value is to be created
     * @return the value; never null
     * @throws IoException If an unexpected problem occurs during the conversion.
     */
    T create( float value ) throws IoException;

    /**
     * Create a value from a double.
     * 
     * @param value the double from which the value is to be created
     * @return the value; never null
     * @throws IoException If an unexpected problem occurs during the conversion.
     */
    T create( double value ) throws IoException;

    /**
     * Create a value from a decimal.
     * 
     * @param value the decimal from which the value is to be created
     * @return the value, or null if the supplied decimal is null
     * @throws IoException If an unexpected problem occurs during the conversion.
     */
    T create( BigDecimal value ) throws IoException;

    /**
     * Create a value from a Calendar instance.
     * 
     * @param value the Calendar instance from which the value is to be created
     * @return the value, or null if the supplied Calendar is null
     * @throws IoException If an unexpected problem occurs during the conversion.
     */
    T create( Calendar value ) throws IoException;

    /**
     * Create a value from a date.
     * 
     * @param value the date from which the value is to be created
     * @return the value, or null if the supplied date is null
     * @throws IoException If an unexpected problem occurs during the conversion.
     */
    T create( Date value ) throws IoException;

    /**
     * Create a value from a name.
     * 
     * @param value the name from which the value is to be created
     * @return the value, or null if the supplied name is null
     * @throws IoException If an unexpected problem occurs during the conversion.
     */
    T create( Name value ) throws IoException;

    /**
     * Create a value from a path.
     * 
     * @param value the path from which the value is to be created
     * @return the value, or null if the supplied path is null
     * @throws IoException If an unexpected problem occurs during the conversion.
     */
    T create( Path value ) throws IoException;

    /**
     * Create a value from a reference.
     * 
     * @param value the reference from which the value is to be created
     * @return the value, or null if the supplied reference is null
     * @throws IoException If an unexpected problem occurs during the conversion.
     */
    T create( Reference value ) throws IoException;

    /**
     * Create a value from a URI.
     * 
     * @param value the URI from which the value is to be created
     * @return the value, or null if the supplied URI is null
     * @throws IoException If an unexpected problem occurs during the conversion.
     */
    T create( URI value ) throws IoException;

    /**
     * Create a value from the binary content given by the supplied array.
     * 
     * @param value the content to be used to create the value
     * @return the value, or null if the supplied stream is null
     * @throws IoException If an unexpected problem occurs during the conversion.
     */
    T create( byte[] value ) throws IoException;

    /**
     * Create a value from the binary content given by the supplied stream.
     * 
     * @param stream the stream containing the content to be used to create the value
     * @param approximateLength the approximate length of the content (in bytes)
     * @return the value, or null if the supplied stream is null
     * @throws IoException If an unexpected problem occurs during the conversion (such as an {@link IOException}).
     */
    T create( InputStream stream,
              int approximateLength ) throws IoException;

    /**
     * Create a value from a the binary content given by the supplied reader.
     * 
     * @param reader the reader containing the content to be used to create the value
     * @param approximateLength the approximate length of the content (in bytes)
     * @return the value, or null if the supplied string is null
     * @throws IoException If an unexpected problem occurs during the conversion (such as an {@link IOException}).
     */
    T create( Reader reader,
              int approximateLength ) throws IoException;

    /**
     * Create a value from the specified information by determining which other <code>create</code> method applies and
     * delegating to that method. Note that this method only will call <code>create</code> methods that take a single parameter;
     * so this excludes {@link #create(InputStream, int)}, {@link #create(Reader, int)} and {@link #create(String, TextDecoder)}.
     * 
     * @param value the value
     * @return the new value, or null if the supplied parameter is null
     * @throws IoException If an unexpected problem occurs during the conversion.
     */
    T create( Object value ) throws IoException;

}
