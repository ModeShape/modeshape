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
/**
 * Nodes in a graph contain properties, and this package defines the interfaces, classes and exceptions for
 * representing and working with properties and their values.
 * <p>
 * A <i>property</i> consists of a <i>name</i> and a set of <i>values</i>.  A property name is represented
 * by {@link org.modeshape.jcr.value.Name}, and is defined as a {@link org.modeshape.jcr.value.Name#getLocalName() local name} in a {@link org.modeshape.jcr.value.Name#getNamespaceUri() namespace}.
 * Property values can be of any type, although there are specific interfaces for the known types:
 * <ul>
 *  <li>{@link org.modeshape.jcr.value.PropertyType#STRING String} - A value represented with instances of the standard {@link java.lang.String} class.</li>
 *  <li>{@link org.modeshape.jcr.value.PropertyType#BINARY Binary} - A value represented with instances of the {@link org.modeshape.jcr.value.Binary} interface.</li>
 *  <li>{@link org.modeshape.jcr.value.PropertyType#LONG Long} - A value represented with instances of the standard {@link java.lang.Long} class.</li>
 *  <li>{@link org.modeshape.jcr.value.PropertyType#DOUBLE Double} - A value represented with instances of the standard {@link java.lang.Double} class.</li>
 *  <li>{@link org.modeshape.jcr.value.PropertyType#DECIMAL Decimal} - A value represented with instances of the standard {@link java.math.BigDecimal} class.</li>
 *  <li>{@link org.modeshape.jcr.value.PropertyType#DATE Date} - A value represented with instances of the {@link org.modeshape.jcr.api.value.DateTime} interface.
 *  This interface hides the mishmash of Java date representations, and is designed to follow the anticipated
 *  <code>ZonedDateTime</code> that is part of JSR-310.</li>
 *  <li>{@link org.modeshape.jcr.value.PropertyType#BOOLEAN Boolean} - A value represented with instances of the standard {@link java.lang.Boolean} class.</li>
 *  <li>{@link org.modeshape.jcr.value.PropertyType#NAME Name} - A value represented with instances of the {@link org.modeshape.jcr.value.Name} interface.</li>
 *  <li>{@link org.modeshape.jcr.value.PropertyType#PATH Path} - A value represented with instances of the {@link org.modeshape.jcr.value.Path} interface.</li>
 *  <li>{@link org.modeshape.jcr.value.PropertyType#UUID UUID} - A value represented with instances of the standard {@link java.util.UUID} class.</li>
 *  <li>{@link org.modeshape.jcr.value.PropertyType#REFERENCE Reference} - A value represented with instances of the {@link org.modeshape.jcr.value.Reference} interface.</li>
 *  <li>{@link org.modeshape.jcr.value.PropertyType#URI URI} - A value represented with instances of the standard {@link java.net.URI} class.</li>
 *  <li>{@link org.modeshape.jcr.value.PropertyType#OBJECT Object} - A value represented with instances of any class, although the class
 *  should in all practicality implement {@link java.io.Serializable}.</li>
 * </ul>
 * </p>
 * 
 * <h3>Creating and converting property values</h3>
 * <p>
 * The design of properties and their values was centered around one key principle: when using a property value,
 * you often don't care what type the property value actually is, but instead care about converting it to a
 * property type that you know how to work with.  For example, you may be working with a property that represents
 * a date, and you want to work with the value as a {@link org.modeshape.jcr.api.value.DateTime} object, regardless of whether the values
 * are actually String, {@link org.modeshape.jcr.api.value.DateTime}, {@link org.modeshape.jcr.value.Binary}, or even {@link java.util.Calendar} or {@link java.util.Date}
 * instances.  You know its should be a date, so you want to get a value that behaves as a date.
 * </p>
 * <p>
 * This notion of working with a <i>desired type</i> implies the ability to convert from one value type to another.
 * And in fact, creating values is really just converting from "other" types into a known type.
 * So, we can use the <i>factory</i> design pattern to have a single concept of a component that creates property values
 * from a variety of types.  But by using generics, we can use a single {@link org.modeshape.jcr.value.ValueFactory factory} interface
 * that has the same methods for creating value objects, but make the return type specific to the type we want to create.
 * </p>
 * <p>
 * The {@link org.modeshape.jcr.value.ValueFactory} interface is defined as follows:
 * <pre>
 *   public interface ValueFactory&lt;T> {
 *       T create( String value ) throws ValueFormatException;
 *       T create( int value ) throws ValueFormatException;
 *       T create( long value ) throws ValueFormatException;
 *       T create( double value ) throws ValueFormatException;
 *       ...
 *       T create( java.util.Date value ) throws ValueFormatException;
 *       T create( java.util.Calendar value ) throws ValueFormatException;
 *       T create( DateTime value ) throws ValueFormatException;
 *       ...
 *       T create( java.util.UUID value ) throws ValueFormatException;
 *       T create( java.net.URI value ) throws ValueFormatException;
 *       T create( Reference value ) throws ValueFormatException;
 *       T create( Name value ) throws ValueFormatException;
 *       T create( Path value ) throws ValueFormatException;
 *       ...
 *       T create( InputStream value, long approximateLength ) throws ValueFormatException;
 *       T create( Reader value, long approximateLength ) throws ValueFormatException;
 *       T create( Binary value ) throws ValueFormatException;
 *       ...
 *       T[] create( String[] value ) throws ValueFormatException;
 *       T[] create( int[] value ) throws ValueFormatException;
 *       T[] create( long[] value ) throws ValueFormatException;
 *       T[] create( double[] value ) throws ValueFormatException;
 *       ...
 *   }
 * </pre>
 * Notice that all the methods are called <code>create</code>, and most take a single parameter whose type is
 * one of the known types, a primitive, or a number of "other" types frequently encountered.  (The <code>create(...)</code>
 * methods that take an {@link java.io.InputStream} or {@link java.io.Reader} have a second parameter that specifies
 * the length of the data.)  Finally, note that almost all of the <code>create</code> methods have a form that each 
 * take an array of values and return an array of <code>T</code>.
 * </p>
 * <p>
 * These methods also all throw a {@link org.modeshape.jcr.value.ValueFormatException}, in case the supplied
 * parameter cannot be converted to the desired type.  In many cases, there is a conversion (e.g., from the String "123"
 * to an integer), but there certainly are cases where no conversion is allowed (e.g., the String "a123" cannot be converted
 * to an integer, and a {@link org.modeshape.jcr.value.Name} cannot be converted to a <code>boolean</code>).  All types can be converted
 * to a string, and all factories support converting that string back to its original form.
 * </p>
 * <p>
 * The factory for creating {@link org.modeshape.jcr.api.value.DateTime} objects would then be an implementation of <code>ValueFactory&lt;DateTime></code>,
 * a factory for creating {@link org.modeshape.jcr.value.Binary} objects would be an implementation of <code>ValueFactory&lt;Binary</code>,
 * and so on.  In some cases, we'd like to add additional forms of <code>create(...)</code> for specific values, and
 * we can do this by extending a typed {@link org.modeshape.jcr.value.ValueFactory}.  For example, the {@link org.modeshape.jcr.value.DateTimeFactory} adds
 * more methods for creating {@link org.modeshape.jcr.api.value.DateTime} objects for the current time, current time in UTC, from another time
 * and an offset, and from individual field values:
 * <pre>
 *   public interface DateTimeFactory extends ValueFactories&lt;DateTime> {
 *       DateTime create();
 *       DateTime createUtc();
 *       DateTime create( DateTime original, long offsetInMillis );
 *       DateTime create( int year, int monthOfYear, int dayOfMonth,
 *                        int hourOfDay, int minuteOfHour, int secondOfMinute, int millisecondsOfSecond );
 *       DateTime create( int year, int monthOfYear, int dayOfMonth,
 *                        int hourOfDay, int minuteOfHour, int secondOfMinute, int millisecondsOfSecond,
 *                        int timeZoneOffsetHours );
 *       DateTime create( int year, int monthOfYear, int dayOfMonth,
 *                        int hourOfDay, int minuteOfHour, int secondOfMinute, int millisecondsOfSecond,
 *                        String timeZoneId );
 *   }
 * </pre>
 * There are specialized factory interfaces for several other types, including {@link org.modeshape.jcr.value.PathFactory}, {@link org.modeshape.jcr.value.NameFactory},
 * and {@link org.modeshape.jcr.value.UuidFactory}.
 * </p>
 * <p>
 * The {@link org.modeshape.jcr.value.ValueFactories} interface collects all the factories into a single spot:
 * <pre>
 *   public interface ValueFactories&lt;T> {
 *       ValueFactory&lt;String> getStringFactory();
 *       ValueFactory&lt;Binary> getBinaryFactory();
 *       ValueFactory&lt;Long> getLongFactory();
 *       ValueFactory&lt;Double> getDoubleFactory();
 *       ValueFactory&lt;BigDecimal> getDecimalFactory();
 *       DateTimeFactory getDateFactory();
 *       ValueFactory&lt;Boolean> getBooleanFactory();
 *       NameFactory getNameFactory();
 *       ValueFactory&lt;Reference> getReferenceFactory();
 *       PathFactory getPathFactory();
 *       ValueFactory&lt;URI> getUriFactory();
 *       UuidFactory getUuidFactory();
 *       ValueFactory&lt;Object> getObjectFactory();
 *       
 *       ValueFactory&lt;?> getValueFactory( PropertyType type );
 *       ValueFactory&lt;?> getValueFactory( Object prototype );
 *   }
 * </pre>
 * This allows us to programmatically get the correct factory for a type known at compile time, but also 
 * to obtain the correct factory given a prototype object or the enumeration literal representing
 * the desired type.  Thus, the following code compiles:
 * <pre>
 *    ValueFactories factories = ...
 *    DateTime now = factories.getDateFactory.create();
 *    String stringValue = factories.getStringFactory().create(now);
 * </pre>
 * A {@link org.modeshape.jcr.value.ValueFactories} is provided as part of the {@link org.modeshape.jcr.ExecutionContext}.  In this way,
 * the environment may use a different implementation of one or more factories.
 * </p>
 * 
 * <h3>Comparing property values</h3>
 * <p>
 * Because we have a mixture of standard Java types and custom interfaces for property values, we need
 * a set of {@link java.util.Comparator} implementations that allow us to compare property values.
 * The {@link org.modeshape.jcr.value.ValueComparators} class defines a number of singleton comparators that can be used.
 * Plus, the {@link org.modeshape.jcr.value.PropertyType} enumeration has the ability to {@link org.modeshape.jcr.value.PropertyType#getComparator() get the comparator}
 * for the specific type (e.g., <code>PropertyType.BINARY.getComparator()</code>).
 * </p>
 * 
 */

package org.modeshape.jcr.value;

