/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.infinispan.schematic.document;

import java.text.ParseException;
import java.util.Date;
import java.util.UUID;
import java.util.regex.Pattern;
import org.infinispan.schematic.document.Bson.BinaryType;

public interface EditableArray extends EditableDocument, Array {

    /**
     * Set the value for the field with the given name to the supplied value.
     * 
     * @param name The name of the field
     * @param value the new value for the field
     * @return This document, to allow for chaining methods
     */
    @Override
    EditableArray set( String name,
                       Object value );

    /**
     * Set the value for the field with the given name to the supplied value.
     * 
     * @param index The index in the array
     * @param value the new value
     * @return This array, to allow for chaining methods
     */
    EditableArray setValue( int index,
                            Object value );

    /**
     * Insert the value for the field with the given name to the supplied value.
     * 
     * @param index The index in the array
     * @param value the new value
     * @return This array, to allow for chaining methods
     */
    EditableArray addValue( int index,
                            Object value );

    /**
     * Add the supplied value to this array.
     * 
     * @param value the new value
     * @return This array, to allow for chaining methods
     */
    EditableArray addValue( Object value );

    /**
     * Add the supplied value to this array if and only if there is not already an equivalent value in the array.
     * 
     * @param value the value
     * @return This array, to allow for chaining methods
     */
    EditableDocument addValueIfAbsent( Object value );

    /**
     * Set the value for the field at the given index to the supplied boolean value.
     * 
     * @param name The name of the field, which is the string representation of the index in the array
     * @param value the new value for the field
     * @return This array, to allow for chaining methods
     */
    @Override
    EditableArray setBoolean( String name,
                              boolean value );

    /**
     * Set the value for the field at the given index to the supplied integer value.
     * 
     * @param name The name of the field, which is the string representation of the index in the array
     * @param value the new value for the field
     * @return This document, to allow for chaining methods
     */
    @Override
    EditableArray setNumber( String name,
                             int value );

    /**
     * Set the value for the field at the given index to the supplied long value.
     * 
     * @param name The name of the field, which is the string representation of the index in the array
     * @param value the new value for the field
     * @return This document, to allow for chaining methods
     */
    @Override
    EditableArray setNumber( String name,
                             long value );

    /**
     * Set the value for the field at the given index to the supplied float value.
     * 
     * @param name The name of the field, which is the string representation of the index in the array
     * @param value the new value for the field
     * @return This document, to allow for chaining methods
     */
    @Override
    EditableArray setNumber( String name,
                             float value );

    /**
     * Set the value for the field at the given index to the supplied double value.
     * 
     * @param name The name of the field, which is the string representation of the index in the array
     * @param value the new value for the field
     * @return This document, to allow for chaining methods
     */
    @Override
    EditableArray setNumber( String name,
                             double value );

    /**
     * Set the value for the field at the given index to the supplied string value.
     * 
     * @param name The name of the field, which is the string representation of the index in the array
     * @param value the new value for the field
     * @return This document, to allow for chaining methods
     */
    @Override
    EditableArray setString( String name,
                             String value );

    /**
     * Set the value for the field at the given index to a {@link Symbol} created from the supplied string value. Symbols are
     * defined in the BSON specification as being similar to a string but which exists for those languages that have a specific
     * symbol type. Symbols are serialized to JSON as a normal string.
     * 
     * @param name The name of the field, which is the string representation of the index in the array
     * @param value the new value for the field
     * @return This document, to allow for chaining methods
     * @see #setString(String, String)
     */
    @Override
    EditableArray setSymbol( String name,
                             String value );

    /**
     * Set the value for the field at the given index to be a new, empty Document.
     * 
     * @param name The name of the field, which is the string representation of the index in the array
     * @return The editable document that was just created; never null
     */
    @Override
    EditableDocument setDocument( String name );

    /**
     * Set the value for the field at the given index to be the supplied Document.
     * 
     * @param name The name of the field, which is the string representation of the index in the array
     * @param document the document
     * @return The editable document that was just set as the value for the named field; never null and may or may not be the same
     *         instance as the supplied <code>document</code>.
     */
    @Override
    EditableDocument setDocument( String name,
                                  Document document );

    /**
     * Get the existing document value in this array for the given index.
     * 
     * @param name The name of the field, which is the string representation of the index in the array
     * @return The editable document field value, if found, or null if there is no such pair or if the value is not a document
     */
    @Override
    EditableDocument getDocument( String name );

    /**
     * Set the value for the field at the given index to be a new, empty array.
     * 
     * @param name The name of the field, which is the string representation of the index in the array
     * @return The editable array that was just created; never null
     */
    @Override
    EditableArray setArray( String name );

    /**
     * Set the value for the field at the given index to be the supplied array.
     * 
     * @param name The name of the field, which is the string representation of the index in the array
     * @param array the array
     * @return The editable array that was just set as the value for the named field; never null and may or may not be the same
     *         instance as the supplied <code>array</code>.
     */
    @Override
    EditableArray setArray( String name,
                            Array array );

    /**
     * Get the existing array value in this array for the given index.
     * 
     * @param name The name of the field, which is the string representation of the index in the array
     * @return The editable array field value, if found, or null if there is no such pair or if the value is not an array
     */
    @Override
    EditableArray getArray( String name );

    /**
     * Set the value for the field at the given index to the supplied date value.
     * 
     * @param name The name of the field, which is the string representation of the index in the array
     * @param value the new value for the field
     * @return This document, to allow for chaining methods
     */
    @Override
    EditableArray setDate( String name,
                           Date value );

    /**
     * Set the value for the field at the given index to the date value parsed from the ISO-8601 date representation.
     * Specifically, the date string must match one of these patterns:
     * <ul>
     * <li>"<code><i>yyyy</i>-<i>MM</i>-<i>dd</i>T<i>HH</i>:<i>mm</i>:<i>ss</i></code>" where "<code>T</code>" is a literal
     * character</li>
     * <li>"<code><i>yyyy</i>-<i>MM</i>-<i>dd</i>T<i>HH</i>:<i>mm</i>:<i>ss</i>Z</code>" where "<code>T</code>" and "
     * <code>Z</code>" are literal characters</li>
     * <li>"<code><i>yyyy</i>-<i>MM</i>-<i>dd</i>T<i>HH</i>:<i>mm</i>:<i>ss</i>GMT+<i>00</i>:<i>00</i></code>" where "
     * <code>T</code>", and "<code>GMT</code>" are literal characters</li>
     * </ul>
     * 
     * @param name The name of the field, which is the string representation of the index in the array
     * @param isoDate the new value for the field
     * @return This document, to allow for chaining methods
     * @throws ParseException if the supplied value could not be parsed into a valid date
     */
    @Override
    EditableArray setDate( String name,
                           String isoDate ) throws ParseException;

    /**
     * Set the value for the field at the given index to a {@link Timestamp} with the supplied time in seconds and increment. Note
     * that {@link Date} values are recommended for most purposes, as they are better suited to most applications' representations
     * of time instants.
     * 
     * @param name The name of the field, which is the string representation of the index in the array
     * @param timeInSeconds the time in seconds for the new Timestamp
     * @param increment the time increment for the new Timestamp
     * @return This document, to allow for chaining methods
     * @see #setDate(String, Date)
     */
    @Override
    EditableArray setTimestamp( String name,
                                int timeInSeconds,
                                int increment );

    /**
     * Set the value for the field at the given index to an {@link ObjectId} created from the supplied hexadecimal binary value.
     * Object IDs are defined by the BSON specification as 12-byte binary values designed to have a reasonably high probability of
     * being unique when allocated. Since there is no explicit way to represent these in a JSON document, each ObjectId value is
     * serialized in a JSON document as a nested document of the form:
     * 
     * <pre>
     * { "$oid" : "<i>12bytesOfIdInBase16</i>" }
     * </pre>
     * 
     * When nested documents of this form are read by this library's {@link Json JSON reader}, nested documents of this form will
     * be converted to an ObjectId value.
     * <p>
     * For example, an ObjectId with time value of "1310745823", machine value of "1", process value of "2", and increment value
     * of "3" would be written as
     * 
     * <pre>
     * { "$oid" : "4e2064df0000010002000003" }
     * </pre>
     * 
     * </p>
     * 
     * @param name The name of the field, which is the string representation of the index in the array
     * @param hex the hexadecimal binary value for the ObjectId
     * @return This document, to allow for chaining methods
     * @see #setObjectId(String, byte[])
     * @see #setObjectId(String, int, int, int, int)
     */
    @Override
    EditableArray setObjectId( String name,
                               String hex );

    /**
     * Set the value for the field at the given index to an {@link ObjectId} created from the supplied 12-byte binary value.
     * Object IDs are defined by the BSON specification as 12-byte binary values designed to have a reasonably high probability of
     * being unique when allocated. Since there is no explicit way to represent these in a JSON document, each ObjectId value is
     * serialized in a JSON document as a nested document of the form:
     * 
     * <pre>
     * { "$oid" : "<i>12bytesOfIdInBase16</i>" }
     * </pre>
     * 
     * When nested documents of this form are read by this library's {@link Json JSON reader}, nested documents of this form will
     * be converted to an ObjectId value.
     * <p>
     * For example, an ObjectId with time value of "1310745823", machine value of "1", process value of "2", and increment value
     * of "3" would be written as
     * 
     * <pre>
     * { "$oid" : "4e2064df0000010002000003" }
     * </pre>
     * 
     * </p>
     * 
     * @param name The name of the field, which is the string representation of the index in the array
     * @param bytes the 12-byte value for the ObjectId
     * @return This document, to allow for chaining methods
     * @see #setObjectId(String, String)
     * @see #setObjectId(String, int, int, int, int)
     */
    @Override
    EditableArray setObjectId( String name,
                               byte[] bytes );

    /**
     * Set the value for the field at the given index to an {@link ObjectId} created from the supplied hexadecimal binary value.
     * Object IDs are defined by the BSON specification as 12-byte binary values designed to have a reasonably high probability of
     * being unique when allocated. Since there is no explicit way to represent these in a JSON document, each ObjectId value is
     * serialized in a JSON document as a nested document of the form:
     * 
     * <pre>
     * { "$oid" : "<i>12bytesOfIdInBase16</i>" }
     * </pre>
     * 
     * When nested documents of this form are read by this library's {@link Json JSON reader}, nested documents of this form will
     * be converted to an ObjectId value.
     * <p>
     * For example, an ObjectId with time value of "1310745823", machine value of "1", process value of "2", and increment value
     * of "3" would be written as
     * 
     * <pre>
     * { "$oid" : "4e2064df0000010002000003" }
     * </pre>
     * 
     * </p>
     * 
     * @param name The name of the field, which is the string representation of the index in the array
     * @param time the Unix-style timestamp, which is a signed integer representing the number of seconds before or after January
     *        1st 1970 (UTC)
     * @param machine the first three bytes of the (md5) hash of the machine host name, or of the mac/network address, or the
     *        virtual machine id
     * @param process the 2 bytes of the process id (or thread id) of the process generating the object id
     * @param inc an ever incrementing value, or a random number if a counter can't be used in the language/runtime
     * @return This document, to allow for chaining methods
     * @see #setObjectId(String, String)
     * @see #setObjectId(String, byte[])
     */
    @Override
    EditableArray setObjectId( String name,
                               int time,
                               int machine,
                               int process,
                               int inc );

    /**
     * Set the value for the field at the given index to the supplied regular expression. Regular expression values are
     * represented in memory using {@link Pattern} instances, and are stored natively in BSON as regular expressions. However,
     * when serialized to JSON, regular expressions are written as nested documents of the form:
     * 
     * <pre>
     * { "$regex" : "<i>pattern</i>" }
     * </pre>
     * 
     * where "<i>pattern</i>" is the regular expression pattern.
     * <p>
     * When nested documents of this form are read by this library's {@link Json JSON reader}, nested documents of this form will
     * be converted to a regular expression value.
     * </p>
     * 
     * @param name The name of the field, which is the string representation of the index in the array
     * @param pattern the regular expression pattern string
     * @return This document, to allow for chaining methods
     * @see #setRegularExpression(String, String, int)
     */
    @Override
    EditableArray setRegularExpression( String name,
                                        String pattern );

    /**
     * Set the value for the field at the given index to the supplied regular expression. Regular expression values are
     * represented in memory using {@link Pattern} instances, and are stored natively in BSON as regular expressions. However,
     * when serialized to JSON, regular expressions are written as nested documents of the form:
     * 
     * <pre>
     * { "$regex" : "<i>pattern</i>", "$options" : "<i>flags</i>" }
     * </pre>
     * 
     * where "<i>pattern</i>" is the regular expression pattern, and "<i>flags</i>" is a string representation of the regular
     * expression options.
     * <p>
     * When nested documents of this form are read by this library's {@link Json JSON reader}, nested documents of this form will
     * be converted to a regular expression value.
     * </p>
     * 
     * @param name The name of the field, which is the string representation of the index in the array
     * @param pattern the regular expression pattern string
     * @param flags the bitwise-anded {@link Pattern} options: {@link Pattern#CANON_EQ}, {@link Pattern#CASE_INSENSITIVE},
     *        {@link Pattern#CASE_INSENSITIVE}, {@link Pattern#COMMENTS}, {@link Pattern#DOTALL}, {@link Pattern#LITERAL},
     *        {@link Pattern#MULTILINE}, {@link Pattern#UNICODE_CASE}, and {@link Pattern#UNIX_LINES}
     * @return This document, to allow for chaining methods
     * @see #setRegularExpression(String, String)
     */
    @Override
    EditableArray setRegularExpression( String name,
                                        String pattern,
                                        int flags );

    /**
     * Set the value for the field at the given index to be a null value. Both JSON and BSON formats support null values, and
     * {@link Null} is used for the value in the in-memory representation. The {@link #isNull(String)} methods can be used to
     * determine if a field has been set to null, or {@link #isNullOrMissing(String)} if the field has not be set or if it has
     * been set to null.
     * 
     * @param name The name of the field, which is the string representation of the index in the array
     * @return This document, to allow for chaining methods
     * @see #isNull(String)
     * @see #isNullOrMissing(String)
     */
    @Override
    EditableArray setNull( String name );

    /**
     * Set the value for the field at the given index to be a binary value. JSON does not formally support binary values, and so
     * such values will be encoded using a nested document of the form:
     * 
     * <pre>
     * { "$type" : <i>typeAsInt</i>, "$base64" : "<i>bytesInBase64</i>" }
     * </pre>
     * 
     * where "<i>typeAsInt</i>" is the integer representation of the {@link BinaryType BSON type}, and "<i>bytesInBase64</i>" is
     * the Base64 encoding of the actual Binary {@link Binary#getBytes() bytes}.
     * <p>
     * When nested documents of this form are read by this library's {@link Json JSON reader}, nested documents of this form will
     * be converted to Binary value.
     * </p>
     * 
     * @param name The name of the field, which is the string representation of the index in the array
     * @param type one of the {@link BinaryType BSON type} constants denoting the type of the {@link Binary} value
     * @param data the bytes for the {@link Binary} value
     * @return This document, to allow for chaining methods
     */
    @Override
    EditableArray setBinary( String name,
                             byte type,
                             byte[] data );

    /**
     * Set the value for the field at the given index to be a {@link UUID}. JSON does not formally support binary values, and so
     * such values will be encoded using a nested document of the form:
     * 
     * <pre>
     * { "$uuid" : "<i>string-form-of-uuid</i>" }
     * </pre>
     * 
     * where "<i>string-form-of-uuid</i>" is the UUID's {@link UUID#toString() string representation}
     * <p>
     * When nested documents of this form are read by this library's {@link Json JSON reader}, nested documents of this form will
     * be converted to UUID value.
     * </p>
     * 
     * @param name The name of the field, which is the string representation of the index in the array
     * @param uuid the UUID value
     * @return This document, to allow for chaining methods
     */
    @Override
    EditableArray setUuid( String name,
                           UUID uuid );

    /**
     * Set the value for the field at the given index to be a {@link Code} or {@link CodeWithScope}. JSON does not formally
     * support such values, and so when written to JSON they will be encoded using a nested document of the form:
     * 
     * <pre>
     * { "$code" : "<i>code</i>" }
     * </pre>
     * 
     * or, if there is a scope document
     * 
     * <pre>
     * { "$code" : "<i>code</i>", "$scope" : <i>scope document</i> }
     * </pre>
     * 
     * where "<i>code</i>" is the {@link Code}'s {@link Code#getCode() JavaScript code} and <i>scopeDocument</i> is the nested
     * document representing the {@link CodeWithScope#getScope() scope} in which the JavaScript code should be evaluated.
     * <p>
     * When nested documents of this form are read by this library's {@link Json JSON reader}, nested documents of this form will
     * be converted to {@link Code} or {@link CodeWithScope} value.
     * </p>
     * <p>
     * Note that when <code>includeScope</code> is <code>true</code>, the returned {@link EditableArray} can be used to populate
     * the scope document.
     * 
     * @param name The name of the field, which is the string representation of the index in the array
     * @param code the code
     * @param includeScope true if the code should include a scope (and if this method should return an {@link EditableArray} for
     *        this scope document), or false otherwise
     * @return if <code>includeScope</code> is <code>true</code>, then the {@link EditableDocument} for the scope; otherwise, this
     *         array to allow for chaining methods
     * @see #setCode(String, String, Document)
     */
    @Override
    EditableDocument setCode( String name,
                              String code,
                              boolean includeScope );

    /**
     * Set the value for the field at the given index to be a {@link Code} or {@link CodeWithScope}. JSON does not formally
     * support such values, and so when written to JSON they will be encoded using a nested document of the form:
     * 
     * <pre>
     * { "$code" : "<i>code</i>" }
     * </pre>
     * 
     * or, if there is a scope document
     * 
     * <pre>
     * { "$code" : "<i>code</i>", "$scope" : <i>scope document</i> }
     * </pre>
     * 
     * where "<i>code</i>" is the {@link Code}'s {@link Code#getCode() JavaScript code} and <i>scopeDocument</i> is the nested
     * document representing the {@link CodeWithScope#getScope() scope} in which the JavaScript code should be evaluated.
     * <p>
     * When nested documents of this form are read by this library's {@link Json JSON reader}, nested documents of this form will
     * be converted to {@link Code} or {@link CodeWithScope} value.
     * </p>
     * 
     * @param name The name of the field, which is the string representation of the index in the array
     * @param code the code
     * @param scope the scope in which the JavaScript code should be evaulated, or null if there is no scope
     * @return the {@link EditableDocument} for the scope
     * @see #setCode(String, String, boolean)
     */
    @Override
    EditableDocument setCode( String name,
                              String code,
                              Document scope );

    /**
     * Set the value for the field at the given index to the supplied boolean value.
     * 
     * @param index The index in the array at which the value is to be set
     * @param value the new value for the field
     * @return This document, to allow for chaining methods
     */
    EditableArray setBoolean( int index,
                              boolean value );

    /**
     * Set the value for the field at the given index to the supplied integer value.
     * 
     * @param index The index in the array at which the value is to be set
     * @param value the new value for the field
     * @return This document, to allow for chaining methods
     */
    EditableArray setNumber( int index,
                             int value );

    /**
     * Set the value for the field at the given index to the supplied long value.
     * 
     * @param index The index in the array at which the value is to be set
     * @param value the new value for the field
     * @return This document, to allow for chaining methods
     */
    EditableArray setNumber( int index,
                             long value );

    /**
     * Set the value for the field at the given index to the supplied float value.
     * 
     * @param index The index in the array at which the value is to be set
     * @param value the new value for the field
     * @return This document, to allow for chaining methods
     */
    EditableArray setNumber( int index,
                             float value );

    /**
     * Set the value for the field at the given index to the supplied double value.
     * 
     * @param index The index in the array at which the value is to be set
     * @param value the new value for the field
     * @return This document, to allow for chaining methods
     */
    EditableArray setNumber( int index,
                             double value );

    /**
     * Set the value for the field at the given index to the supplied string value.
     * 
     * @param index The index in the array at which the value is to be set
     * @param value the new value for the field
     * @return This document, to allow for chaining methods
     */
    EditableArray setString( int index,
                             String value );

    /**
     * Set the value for the field at the given index to a {@link Symbol} created from the supplied string value. Symbols are
     * defined in the BSON specification as being similar to a string but which exists for those languages that have a specific
     * symbol type. Symbols are serialized to JSON as a normal string.
     * 
     * @param index The index in the array at which the value is to be set
     * @param value the new value for the field
     * @return This document, to allow for chaining methods
     * @see #setString(int, String)
     */
    EditableArray setSymbol( int index,
                             String value );

    /**
     * Set the value for the field at the given index to be a new, empty Document.
     * 
     * @param index The index in the array at which the value is to be set
     * @return The editable document that was just created; never null
     */
    EditableDocument setDocument( int index );

    /**
     * Set the value for the field at the given index to be the supplied Document.
     * 
     * @param index The index in the array at which the value is to be set
     * @param document the document
     * @return The editable document that was just set as the value at the supplied index in this array; never null and may or may
     *         not be the same instance as the supplied <code>document</code>.
     */
    EditableDocument setDocument( int index,
                                  Document document );

    /**
     * Set the value for the field at the given index to be a new, empty array.
     * 
     * @param index The index in the array at which the value is to be set
     * @return The editable array that was just created; never null
     */
    EditableArray setArray( int index );

    /**
     * Set the value for the field at the given index to be the supplied array.
     * 
     * @param index The index in the array at which the value is to be set
     * @param array the array
     * @return The editable array that was just set as the value at the supplied index in this array; never null and may or may
     *         not be the same instance as the supplied <code>array</code>.
     */
    EditableArray setArray( int index,
                            Array array );

    /**
     * Set the value for the field at the given index to the supplied date value.
     * 
     * @param index The index in the array at which the value is to be set
     * @param value the new value for the field
     * @return This document, to allow for chaining methods
     */
    EditableArray setDate( int index,
                           Date value );

    /**
     * Set the value for the field at the given index to the date value parsed from the ISO-8601 date representation.
     * Specifically, the date string must match one of these patterns:
     * <ul>
     * <li>"<code><i>yyyy</i>-<i>MM</i>-<i>dd</i>T<i>HH</i>:<i>mm</i>:<i>ss</i></code>" where "<code>T</code>" is a literal
     * character</li>
     * <li>"<code><i>yyyy</i>-<i>MM</i>-<i>dd</i>T<i>HH</i>:<i>mm</i>:<i>ss</i>Z</code>" where "<code>T</code>" and "
     * <code>Z</code>" are literal characters</li>
     * <li>"<code><i>yyyy</i>-<i>MM</i>-<i>dd</i>T<i>HH</i>:<i>mm</i>:<i>ss</i>GMT+<i>00</i>:<i>00</i></code>" where "
     * <code>T</code>", and "<code>GMT</code>" are literal characters</li>
     * </ul>
     * 
     * @param index The index in the array at which the value is to be set
     * @param isoDate the new value for the field
     * @return This document, to allow for chaining methods
     * @throws ParseException if the supplied value could not be parsed into a valid date
     */
    EditableArray setDate( int index,
                           String isoDate ) throws ParseException;

    /**
     * Set the value for the field at the given index to a {@link Timestamp} with the supplied time in seconds and increment. Note
     * that {@link Date} values are recommended for most purposes, as they are better suited to most applications' representations
     * of time instants.
     * 
     * @param index The index in the array at which the value is to be set
     * @param timeInSeconds the time in seconds for the new Timestamp
     * @param increment the time increment for the new Timestamp
     * @return This document, to allow for chaining methods
     * @see #setDate(int, Date)
     */
    EditableArray setTimestamp( int index,
                                int timeInSeconds,
                                int increment );

    /**
     * Set the value for the field at the given index to an {@link ObjectId} created from the supplied hexadecimal binary value.
     * Object IDs are defined by the BSON specification as 12-byte binary values designed to have a reasonably high probability of
     * being unique when allocated. Since there is no explicit way to represent these in a JSON document, each ObjectId value is
     * serialized in a JSON document as a nested document of the form:
     * 
     * <pre>
     * { "$oid" : "<i>12bytesOfIdInBase16</i>" }
     * </pre>
     * 
     * When nested documents of this form are read by this library's {@link Json JSON reader}, nested documents of this form will
     * be converted to an ObjectId value.
     * <p>
     * For example, an ObjectId with time value of "1310745823", machine value of "1", process value of "2", and increment value
     * of "3" would be written as
     * 
     * <pre>
     * { "$oid" : "4e2064df0000010002000003" }
     * </pre>
     * 
     * </p>
     * 
     * @param index The index in the array at which the value is to be set
     * @param hex the hexadecimal binary value for the ObjectId
     * @return This document, to allow for chaining methods
     * @see #setObjectId(int, byte[])
     * @see #setObjectId(int, int, int, int, int)
     */
    EditableArray setObjectId( int index,
                               String hex );

    /**
     * Set the value for the field at the given index to an {@link ObjectId} created from the supplied 12-byte binary value.
     * Object IDs are defined by the BSON specification as 12-byte binary values designed to have a reasonably high probability of
     * being unique when allocated. Since there is no explicit way to represent these in a JSON document, each ObjectId value is
     * serialized in a JSON document as a nested document of the form:
     * 
     * <pre>
     * { "$oid" : "<i>12bytesOfIdInBase16</i>" }
     * </pre>
     * 
     * When nested documents of this form are read by this library's {@link Json JSON reader}, nested documents of this form will
     * be converted to an ObjectId value.
     * <p>
     * For example, an ObjectId with time value of "1310745823", machine value of "1", process value of "2", and increment value
     * of "3" would be written as
     * 
     * <pre>
     * { "$oid" : "4e2064df0000010002000003" }
     * </pre>
     * 
     * </p>
     * 
     * @param index The index in the array at which the value is to be set
     * @param bytes the 12-byte value for the ObjectId
     * @return This document, to allow for chaining methods
     * @see #setObjectId(int, String)
     * @see #setObjectId(int, int, int, int, int)
     */
    EditableArray setObjectId( int index,
                               byte[] bytes );

    /**
     * Set the value for the field at the given index to an {@link ObjectId} created from the supplied hexadecimal binary value.
     * Object IDs are defined by the BSON specification as 12-byte binary values designed to have a reasonably high probability of
     * being unique when allocated. Since there is no explicit way to represent these in a JSON document, each ObjectId value is
     * serialized in a JSON document as a nested document of the form:
     * 
     * <pre>
     * { "$oid" : "<i>12bytesOfIdInBase16</i>" }
     * </pre>
     * 
     * When nested documents of this form are read by this library's {@link Json JSON reader}, nested documents of this form will
     * be converted to an ObjectId value.
     * <p>
     * For example, an ObjectId with time value of "1310745823", machine value of "1", process value of "2", and increment value
     * of "3" would be written as
     * 
     * <pre>
     * { "$oid" : "4e2064df0000010002000003" }
     * </pre>
     * 
     * </p>
     * 
     * @param index The index in the array at which the value is to be set
     * @param time the Unix-style timestamp, which is a signed integer representing the number of seconds before or after January
     *        1st 1970 (UTC)
     * @param machine the first three bytes of the (md5) hash of the machine host name, or of the mac/network address, or the
     *        virtual machine id
     * @param process the 2 bytes of the process id (or thread id) of the process generating the object id
     * @param inc an ever incrementing value, or a random number if a counter can't be used in the language/runtime
     * @return This document, to allow for chaining methods
     * @see #setObjectId(int, String)
     * @see #setObjectId(int, byte[])
     */
    EditableArray setObjectId( int index,
                               int time,
                               int machine,
                               int process,
                               int inc );

    /**
     * Set the value for the field at the given index to the supplied regular expression. Regular expression values are
     * represented in memory using {@link Pattern} instances, and are stored natively in BSON as regular expressions. However,
     * when serialized to JSON, regular expressions are written as nested documents of the form:
     * 
     * <pre>
     * { "$regex" : "<i>pattern</i>" }
     * </pre>
     * 
     * where "<i>pattern</i>" is the regular expression pattern.
     * <p>
     * When nested documents of this form are read by this library's {@link Json JSON reader}, nested documents of this form will
     * be converted to a regular expression value.
     * </p>
     * 
     * @param index The index in the array at which the value is to be set
     * @param pattern the regular expression pattern string
     * @return This document, to allow for chaining methods
     * @see #setRegularExpression(int, String, int)
     */
    EditableArray setRegularExpression( int index,
                                        String pattern );

    /**
     * Set the value for the field at the given index to the supplied regular expression. Regular expression values are
     * represented in memory using {@link Pattern} instances, and are stored natively in BSON as regular expressions. However,
     * when serialized to JSON, regular expressions are written as nested documents of the form:
     * 
     * <pre>
     * { "$regex" : "<i>pattern</i>", "$options" : "<i>flags</i>" }
     * </pre>
     * 
     * where "<i>pattern</i>" is the regular expression pattern, and "<i>flags</i>" is a string representation of the regular
     * expression options.
     * <p>
     * When nested documents of this form are read by this library's {@link Json JSON reader}, nested documents of this form will
     * be converted to a regular expression value.
     * </p>
     * 
     * @param index The index in the array at which the value is to be set
     * @param pattern the regular expression pattern string
     * @param flags the bitwise-anded {@link Pattern} options: {@link Pattern#CANON_EQ}, {@link Pattern#CASE_INSENSITIVE},
     *        {@link Pattern#CASE_INSENSITIVE}, {@link Pattern#COMMENTS}, {@link Pattern#DOTALL}, {@link Pattern#LITERAL},
     *        {@link Pattern#MULTILINE}, {@link Pattern#UNICODE_CASE}, and {@link Pattern#UNIX_LINES}
     * @return This document, to allow for chaining methods
     * @see #setRegularExpression(int, String)
     */
    EditableArray setRegularExpression( int index,
                                        String pattern,
                                        int flags );

    /**
     * Set the value for the field at the given index to be a null value. Both JSON and BSON formats support null values, and
     * {@link Null} is used for the value in the in-memory representation. The {@link #isNull(String)} methods can be used to
     * determine if a field has been set to null, or {@link #isNullOrMissing(String)} if the field has not be set or if it has
     * been set to null.
     * 
     * @param index The index in the array at which the value is to be set
     * @return This document, to allow for chaining methods
     * @see #isNull(String)
     * @see #isNullOrMissing(String)
     */
    EditableArray setNull( int index );

    /**
     * Set the value for the field at the given index to be a binary value. JSON does not formally support binary values, and so
     * such values will be encoded using a nested document of the form:
     * 
     * <pre>
     * { "$type" : <i>typeAsInt</i>, "$base64" : "<i>bytesInBase64</i>" }
     * </pre>
     * 
     * where "<i>typeAsInt</i>" is the integer representation of the {@link BinaryType BSON type}, and "<i>bytesInBase64</i>" is
     * the Base64 encoding of the actual Binary {@link Binary#getBytes() bytes}.
     * <p>
     * When nested documents of this form are read by this library's {@link Json JSON reader}, nested documents of this form will
     * be converted to Binary value.
     * </p>
     * 
     * @param index The index in the array at which the value is to be set
     * @param type one of the {@link BinaryType BSON type} constants denoting the type of the {@link Binary} value
     * @param data the bytes for the {@link Binary} value
     * @return This document, to allow for chaining methods
     */
    EditableArray setBinary( int index,
                             byte type,
                             byte[] data );

    /**
     * Set the value for the field at the given index to be a {@link UUID}. JSON does not formally support binary values, and so
     * such values will be encoded using a nested document of the form:
     * 
     * <pre>
     * { "$uuid" : "<i>string-form-of-uuid</i>" }
     * </pre>
     * 
     * where "<i>string-form-of-uuid</i>" is the UUID's {@link UUID#toString() string representation}
     * <p>
     * When nested documents of this form are read by this library's {@link Json JSON reader}, nested documents of this form will
     * be converted to UUID value.
     * </p>
     * 
     * @param index The index in the array at which the value is to be set
     * @param uuid the UUID value
     * @return This document, to allow for chaining methods
     */
    EditableArray setUuid( int index,
                           UUID uuid );

    /**
     * Set the value for the field at the given index to be a {@link Code} or {@link CodeWithScope}. JSON does not formally
     * support such values, and so when written to JSON they will be encoded using a nested document of the form:
     * 
     * <pre>
     * { "$code" : "<i>code</i>" }
     * </pre>
     * 
     * or, if there is a scope document
     * 
     * <pre>
     * { "$code" : "<i>code</i>", "$scope" : <i>scope document</i> }
     * </pre>
     * 
     * where "<i>code</i>" is the {@link Code}'s {@link Code#getCode() JavaScript code} and <i>scopeDocument</i> is the nested
     * document representing the {@link CodeWithScope#getScope() scope} in which the JavaScript code should be evaluated.
     * <p>
     * When nested documents of this form are read by this library's {@link Json JSON reader}, nested documents of this form will
     * be converted to {@link Code} or {@link CodeWithScope} value.
     * </p>
     * <p>
     * Note that when <code>includeScope</code> is <code>true</code>, the returned {@link EditableArray} can be used to populate
     * the scope document.
     * 
     * @param index The index in the array at which the value is to be set
     * @param code the code
     * @param includeScope true if the code should include a scope (and if this method should return an {@link EditableArray} for
     *        this scope document), or false otherwise
     * @return if <code>includeScope</code> is <code>true</code>, then the {@link EditableDocument} for the scope; otherwise, this
     *         array to allow for chaining methods
     * @see #setCode(int, String, Document)
     */
    EditableDocument setCode( int index,
                              String code,
                              boolean includeScope );

    /**
     * Set the value for the field at the given index to be a {@link Code} or {@link CodeWithScope}. JSON does not formally
     * support such values, and so when written to JSON they will be encoded using a nested document of the form:
     * 
     * <pre>
     * { "$code" : "<i>code</i>" }
     * </pre>
     * 
     * or, if there is a scope document
     * 
     * <pre>
     * { "$code" : "<i>code</i>", "$scope" : <i>scope document</i> }
     * </pre>
     * 
     * where "<i>code</i>" is the {@link Code}'s {@link Code#getCode() JavaScript code} and <i>scopeDocument</i> is the nested
     * document representing the {@link CodeWithScope#getScope() scope} in which the JavaScript code should be evaluated.
     * <p>
     * When nested documents of this form are read by this library's {@link Json JSON reader}, nested documents of this form will
     * be converted to {@link Code} or {@link CodeWithScope} value.
     * </p>
     * 
     * @param index The index in the array at which the value is to be set
     * @param code the code
     * @param scope the scope in which the JavaScript code should be evaulated, or null if there is no scope
     * @return the {@link EditableDocument} for the scope; or this array if the scope is null
     * @see #setCode(int, String, boolean)
     */
    EditableDocument setCode( int index,
                              String code,
                              Document scope );

    /**
     * Insert the value for the field at the given index to the supplied boolean value.
     * 
     * @param index The index in the array at which the value is to be set
     * @param value the new value for the field
     * @return This document, to allow for chaining methods
     */
    EditableArray addBoolean( int index,
                              boolean value );

    /**
     * Insert the value for the field at the given index to the supplied integer value.
     * 
     * @param index The index in the array at which the value is to be set
     * @param value the new value for the field
     * @return This document, to allow for chaining methods
     */
    EditableArray addNumber( int index,
                             int value );

    /**
     * Insert the value for the field at the given index to the supplied long value.
     * 
     * @param index The index in the array at which the value is to be set
     * @param value the new value for the field
     * @return This document, to allow for chaining methods
     */
    EditableArray addNumber( int index,
                             long value );

    /**
     * Insert the value for the field at the given index to the supplied float value.
     * 
     * @param index The index in the array at which the value is to be set
     * @param value the new value for the field
     * @return This document, to allow for chaining methods
     */
    EditableArray addNumber( int index,
                             float value );

    /**
     * Insert the value for the field at the given index to the supplied double value.
     * 
     * @param index The index in the array at which the value is to be set
     * @param value the new value for the field
     * @return This document, to allow for chaining methods
     */
    EditableArray addNumber( int index,
                             double value );

    /**
     * Insert the value for the field at the given index to the supplied string value.
     * 
     * @param index The index in the array at which the value is to be set
     * @param value the new value for the field
     * @return This document, to allow for chaining methods
     */
    EditableArray addString( int index,
                             String value );

    /**
     * Insert the value for the field at the given index to a {@link Symbol} created from the supplied string value. Symbols are
     * defined in the BSON specification as being similar to a string but which exists for those languages that have a specific
     * symbol type. Symbols are serialized to JSON as a normal string.
     * 
     * @param index The index in the array at which the value is to be set
     * @param value the new value for the field
     * @return This document, to allow for chaining methods
     * @see #setString(int, String)
     */
    EditableArray addSymbol( int index,
                             String value );

    /**
     * Insert the value for the field at the given index to be a new, empty Document.
     * 
     * @param index The index in the array at which the value is to be set
     * @return The editable document that was just created; never null
     */
    EditableDocument addDocument( int index );

    /**
     * Insert the value for the field at the given index to be the supplied Document.
     * 
     * @param index The index in the array at which the value is to be set
     * @param document the document
     * @return The editable document that was just set as the value at the supplied index in this array; never null and may or may
     *         not be the same instance as the supplied <code>document</code>.
     */
    EditableDocument addDocument( int index,
                                  Document document );

    /**
     * Insert the value for the field at the given index to be a new, empty array.
     * 
     * @param index The index in the array at which the value is to be set
     * @return The editable array that was just created; never null
     */
    EditableArray addArray( int index );

    /**
     * Insert the value for the field at the given index to be the supplied array.
     * 
     * @param index The index in the array at which the value is to be set
     * @param array the array
     * @return The editable array that was just set as the value at the supplied index in this array; never null and may or may
     *         not be the same instance as the supplied <code>array</code>.
     */
    EditableArray addArray( int index,
                            Array array );

    /**
     * Insert the value for the field at the given index to the supplied date value.
     * 
     * @param index The index in the array at which the value is to be set
     * @param value the new value for the field
     * @return This document, to allow for chaining methods
     */
    EditableArray addDate( int index,
                           Date value );

    /**
     * Insert the value for the field at the given index to the date value parsed from the ISO-8601 date representation.
     * Specifically, the date string must match one of these patterns:
     * <ul>
     * <li>"<code><i>yyyy</i>-<i>MM</i>-<i>dd</i>T<i>HH</i>:<i>mm</i>:<i>ss</i></code>" where "<code>T</code>" is a literal
     * character</li>
     * <li>"<code><i>yyyy</i>-<i>MM</i>-<i>dd</i>T<i>HH</i>:<i>mm</i>:<i>ss</i>Z</code>" where "<code>T</code>" and "
     * <code>Z</code>" are literal characters</li>
     * <li>"<code><i>yyyy</i>-<i>MM</i>-<i>dd</i>T<i>HH</i>:<i>mm</i>:<i>ss</i>GMT+<i>00</i>:<i>00</i></code>" where "
     * <code>T</code>", and "<code>GMT</code>" are literal characters</li>
     * </ul>
     * 
     * @param index The index in the array at which the value is to be set
     * @param isoDate the new value for the field
     * @return This document, to allow for chaining methods
     * @throws ParseException if the supplied value could not be parsed into a valid date
     */
    EditableArray addDate( int index,
                           String isoDate ) throws ParseException;

    /**
     * Insert the value for the field at the given index to a {@link Timestamp} with the supplied time in seconds and increment.
     * Note that {@link Date} values are recommended for most purposes, as they are better suited to most applications'
     * representations of time instants.
     * 
     * @param index The index in the array at which the value is to be set
     * @param timeInSeconds the time in seconds for the new Timestamp
     * @param increment the time increment for the new Timestamp
     * @return This document, to allow for chaining methods
     * @see #setDate(int, Date)
     */
    EditableArray addTimestamp( int index,
                                int timeInSeconds,
                                int increment );

    /**
     * Insert the value for the field at the given index to an {@link ObjectId} created from the supplied hexadecimal binary
     * value. Object IDs are defined by the BSON specification as 12-byte binary values designed to have a reasonably high
     * probability of being unique when allocated. Since there is no explicit way to represent these in a JSON document, each
     * ObjectId value is serialized in a JSON document as a nested document of the form:
     * 
     * <pre>
     * { "$oid" : "<i>12bytesOfIdInBase16</i>" }
     * </pre>
     * 
     * When nested documents of this form are read by this library's {@link Json JSON reader}, nested documents of this form will
     * be converted to an ObjectId value.
     * <p>
     * For example, an ObjectId with time value of "1310745823", machine value of "1", process value of "2", and increment value
     * of "3" would be written as
     * 
     * <pre>
     * { "$oid" : "4e2064df0000010002000003" }
     * </pre>
     * 
     * </p>
     * 
     * @param index The index in the array at which the value is to be set
     * @param hex the hexadecimal binary value for the ObjectId
     * @return This document, to allow for chaining methods
     * @see #setObjectId(int, byte[])
     * @see #setObjectId(int, int, int, int, int)
     */
    EditableArray addObjectId( int index,
                               String hex );

    /**
     * Insert the value for the field at the given index to an {@link ObjectId} created from the supplied 12-byte binary value.
     * Object IDs are defined by the BSON specification as 12-byte binary values designed to have a reasonably high probability of
     * being unique when allocated. Since there is no explicit way to represent these in a JSON document, each ObjectId value is
     * serialized in a JSON document as a nested document of the form:
     * 
     * <pre>
     * { "$oid" : "<i>12bytesOfIdInBase16</i>" }
     * </pre>
     * 
     * When nested documents of this form are read by this library's {@link Json JSON reader}, nested documents of this form will
     * be converted to an ObjectId value.
     * <p>
     * For example, an ObjectId with time value of "1310745823", machine value of "1", process value of "2", and increment value
     * of "3" would be written as
     * 
     * <pre>
     * { "$oid" : "4e2064df0000010002000003" }
     * </pre>
     * 
     * </p>
     * 
     * @param index The index in the array at which the value is to be set
     * @param bytes the 12-byte value for the ObjectId
     * @return This document, to allow for chaining methods
     * @see #setObjectId(int, String)
     * @see #setObjectId(int, int, int, int, int)
     */
    EditableArray addObjectId( int index,
                               byte[] bytes );

    /**
     * Insert the value for the field at the given index to an {@link ObjectId} created from the supplied hexadecimal binary
     * value. Object IDs are defined by the BSON specification as 12-byte binary values designed to have a reasonably high
     * probability of being unique when allocated. Since there is no explicit way to represent these in a JSON document, each
     * ObjectId value is serialized in a JSON document as a nested document of the form:
     * 
     * <pre>
     * { "$oid" : "<i>12bytesOfIdInBase16</i>" }
     * </pre>
     * 
     * When nested documents of this form are read by this library's {@link Json JSON reader}, nested documents of this form will
     * be converted to an ObjectId value.
     * <p>
     * For example, an ObjectId with time value of "1310745823", machine value of "1", process value of "2", and increment value
     * of "3" would be written as
     * 
     * <pre>
     * { "$oid" : "4e2064df0000010002000003" }
     * </pre>
     * 
     * </p>
     * 
     * @param index The index in the array at which the value is to be set
     * @param time the Unix-style timestamp, which is a signed integer representing the number of seconds before or after January
     *        1st 1970 (UTC)
     * @param machine the first three bytes of the (md5) hash of the machine host name, or of the mac/network address, or the
     *        virtual machine id
     * @param process the 2 bytes of the process id (or thread id) of the process generating the object id
     * @param inc an ever incrementing value, or a random number if a counter can't be used in the language/runtime
     * @return This document, to allow for chaining methods
     * @see #setObjectId(int, String)
     * @see #setObjectId(int, byte[])
     */
    EditableArray addObjectId( int index,
                               int time,
                               int machine,
                               int process,
                               int inc );

    /**
     * Insert the value for the field at the given index to the supplied regular expression. Regular expression values are
     * represented in memory using {@link Pattern} instances, and are stored natively in BSON as regular expressions. However,
     * when serialized to JSON, regular expressions are written as nested documents of the form:
     * 
     * <pre>
     * { "$regex" : "<i>pattern</i>" }
     * </pre>
     * 
     * where "<i>pattern</i>" is the regular expression pattern.
     * <p>
     * When nested documents of this form are read by this library's {@link Json JSON reader}, nested documents of this form will
     * be converted to a regular expression value.
     * </p>
     * 
     * @param index The index in the array at which the value is to be set
     * @param pattern the regular expression pattern string
     * @return This document, to allow for chaining methods
     * @see #setRegularExpression(int, String, int)
     */
    EditableArray addRegularExpression( int index,
                                        String pattern );

    /**
     * Insert the value for the field at the given index to the supplied regular expression. Regular expression values are
     * represented in memory using {@link Pattern} instances, and are stored natively in BSON as regular expressions. However,
     * when serialized to JSON, regular expressions are written as nested documents of the form:
     * 
     * <pre>
     * { "$regex" : "<i>pattern</i>", "$options" : "<i>flags</i>" }
     * </pre>
     * 
     * where "<i>pattern</i>" is the regular expression pattern, and "<i>flags</i>" is a string representation of the regular
     * expression options.
     * <p>
     * When nested documents of this form are read by this library's {@link Json JSON reader}, nested documents of this form will
     * be converted to a regular expression value.
     * </p>
     * 
     * @param index The index in the array at which the value is to be set
     * @param pattern the regular expression pattern string
     * @param flags the bitwise-anded {@link Pattern} options: {@link Pattern#CANON_EQ}, {@link Pattern#CASE_INSENSITIVE},
     *        {@link Pattern#CASE_INSENSITIVE}, {@link Pattern#COMMENTS}, {@link Pattern#DOTALL}, {@link Pattern#LITERAL},
     *        {@link Pattern#MULTILINE}, {@link Pattern#UNICODE_CASE}, and {@link Pattern#UNIX_LINES}
     * @return This document, to allow for chaining methods
     * @see #setRegularExpression(int, String)
     */
    EditableArray addRegularExpression( int index,
                                        String pattern,
                                        int flags );

    /**
     * Insert the value for the field at the given index to be a null value. Both JSON and BSON formats support null values, and
     * {@link Null} is used for the value in the in-memory representation. The {@link #isNull(String)} methods can be used to
     * determine if a field has been set to null, or {@link #isNullOrMissing(String)} if the field has not be set or if it has
     * been set to null.
     * 
     * @param index The index in the array at which the value is to be set
     * @return This document, to allow for chaining methods
     * @see #isNull(String)
     * @see #isNullOrMissing(String)
     */
    EditableArray addNull( int index );

    /**
     * Insert the value for the field at the given index to be a binary value. JSON does not formally support binary values, and
     * so such values will be encoded using a nested document of the form:
     * 
     * <pre>
     * { "$type" : <i>typeAsInt</i>, "$base64" : "<i>bytesInBase64</i>" }
     * </pre>
     * 
     * where "<i>typeAsInt</i>" is the integer representation of the {@link BinaryType BSON type}, and "<i>bytesInBase64</i>" is
     * the Base64 encoding of the actual Binary {@link Binary#getBytes() bytes}.
     * <p>
     * When nested documents of this form are read by this library's {@link Json JSON reader}, nested documents of this form will
     * be converted to Binary value.
     * </p>
     * 
     * @param index The index in the array at which the value is to be set
     * @param type one of the {@link BinaryType BSON type} constants denoting the type of the {@link Binary} value
     * @param data the bytes for the {@link Binary} value
     * @return This document, to allow for chaining methods
     */
    EditableArray addBinary( int index,
                             byte type,
                             byte[] data );

    /**
     * Insert the value for the field at the given index to be a {@link UUID}. JSON does not formally support binary values, and
     * so such values will be encoded using a nested document of the form:
     * 
     * <pre>
     * { "$uuid" : "<i>string-form-of-uuid</i>" }
     * </pre>
     * 
     * where "<i>string-form-of-uuid</i>" is the UUID's {@link UUID#toString() string representation}
     * <p>
     * When nested documents of this form are read by this library's {@link Json JSON reader}, nested documents of this form will
     * be converted to UUID value.
     * </p>
     * 
     * @param index The index in the array at which the value is to be set
     * @param uuid the UUID value
     * @return This document, to allow for chaining methods
     */
    EditableArray addUuid( int index,
                           UUID uuid );

    /**
     * Insert the value for the field at the given index to be a {@link Code} or {@link CodeWithScope}. JSON does not formally
     * support such values, and so when written to JSON they will be encoded using a nested document of the form:
     * 
     * <pre>
     * { "$code" : "<i>code</i>" }
     * </pre>
     * 
     * or, if there is a scope document
     * 
     * <pre>
     * { "$code" : "<i>code</i>", "$scope" : <i>scope document</i> }
     * </pre>
     * 
     * where "<i>code</i>" is the {@link Code}'s {@link Code#getCode() JavaScript code} and <i>scopeDocument</i> is the nested
     * document representing the {@link CodeWithScope#getScope() scope} in which the JavaScript code should be evaluated.
     * <p>
     * When nested documents of this form are read by this library's {@link Json JSON reader}, nested documents of this form will
     * be converted to {@link Code} or {@link CodeWithScope} value.
     * </p>
     * <p>
     * Note that when <code>includeScope</code> is <code>true</code>, the returned {@link EditableArray} can be used to populate
     * the scope document.
     * 
     * @param index The index in the array at which the value is to be set
     * @param code the code
     * @param includeScope true if the code should include a scope (and if this method should return an {@link EditableArray} for
     *        this scope document), or false otherwise
     * @return if <code>includeScope</code> is <code>true</code>, then the {@link EditableDocument} for the scope; otherwise, this
     *         array to allow for chaining methods
     * @see #setCode(int, String, Document)
     */
    EditableDocument addCode( int index,
                              String code,
                              boolean includeScope );

    /**
     * Insert the value for the field at the given index to be a {@link Code} or {@link CodeWithScope}. JSON does not formally
     * support such values, and so when written to JSON they will be encoded using a nested document of the form:
     * 
     * <pre>
     * { "$code" : "<i>code</i>" }
     * </pre>
     * 
     * or, if there is a scope document
     * 
     * <pre>
     * { "$code" : "<i>code</i>", "$scope" : <i>scope document</i> }
     * </pre>
     * 
     * where "<i>code</i>" is the {@link Code}'s {@link Code#getCode() JavaScript code} and <i>scopeDocument</i> is the nested
     * document representing the {@link CodeWithScope#getScope() scope} in which the JavaScript code should be evaluated.
     * <p>
     * When nested documents of this form are read by this library's {@link Json JSON reader}, nested documents of this form will
     * be converted to {@link Code} or {@link CodeWithScope} value.
     * </p>
     * 
     * @param index The index in the array at which the value is to be set
     * @param code the code
     * @param scope the scope in which the JavaScript code should be evaulated, or null if there is no scope
     * @return the {@link EditableDocument} for the scope; or this array if the scope is null
     * @see #setCode(int, String, boolean)
     */
    EditableDocument addCode( int index,
                              String code,
                              Document scope );

    /**
     * Adds the supplied boolean value to this array.
     * 
     * @param value the new value for the field
     * @return This document, to allow for chaining methods
     */
    EditableArray addBoolean( boolean value );

    /**
     * Adds the supplied integer value to this array.
     * 
     * @param value the new value for the field
     * @return This document, to allow for chaining methods
     */
    EditableArray addNumber( int value );

    /**
     * Adds the supplied long value to this array.
     * 
     * @param value the new value for the field
     * @return This document, to allow for chaining methods
     */
    EditableArray addNumber( long value );

    /**
     * Adds the supplied float value to this array.
     * 
     * @param value the new value for the field
     * @return This document, to allow for chaining methods
     */
    EditableArray addNumber( float value );

    /**
     * Adds the supplied double value to this array.
     * 
     * @param value the new value for the field
     * @return This document, to allow for chaining methods
     */
    EditableArray addNumber( double value );

    /**
     * Adds the supplied string value to this array.
     * 
     * @param value the new value for the field
     * @return This document, to allow for chaining methods
     */
    EditableArray addString( String value );

    /**
     * Adds to this array a Symbol with the supplied string.
     * 
     * @param value the new value for the field
     * @return This document, to allow for chaining methods
     * @see #addString(String)
     */
    EditableArray addSymbol( String value );

    /**
     * Adds to this array a new empty document.
     * 
     * @return The editable document that was just created; never null
     */
    EditableDocument addDocument();

    /**
     * Adds to this array the supplied document.
     * 
     * @param document the document
     * @return The editable document that was just added to this array; never null and may or may not be the same instance as the
     *         supplied <code>document</code>.
     */
    EditableDocument addDocument( Document document );

    /**
     * Adds to this array a new empty array.
     * 
     * @return The editable array that was just created; never null
     */
    EditableArray addArray();

    /**
     * Adds to this array the supplied array.
     * 
     * @param array the array
     * @return The editable array that was just added to this array; never null and may or may not be the same instance as the
     *         supplied <code>array</code>.
     */
    EditableArray addArray( Array array );

    /**
     * Adds to this array the supplied date.
     * 
     * @param value the new value for the field
     * @return This document, to allow for chaining methods
     */
    EditableArray addDate( Date value );

    /**
     * Adds to this array a Date with the supplied ISO-8601 string.
     * 
     * @param isoDate the new value for the field
     * @return This document, to allow for chaining methods
     * @see #addDate(Date)
     * @throws ParseException if the supplied value could not be parsed into a valid date
     */
    EditableArray addDate( String isoDate ) throws ParseException;

    /**
     * Adds to this array a Timestamp with the supplied time in seconds and increment value.
     * 
     * @param timeInSeconds the time in seconds for the new Timestamp
     * @param increment the time increment for the new Timestamp
     * @return This document, to allow for chaining methods
     * @see #addDate(Date)
     */
    EditableArray addTimestamp( int timeInSeconds,
                                int increment );

    /**
     * Adds to this array an ObjectId with the supplied hexadecimal string.
     * 
     * @param hex the hexadecimal binary value for the ObjectId
     * @return This document, to allow for chaining methods
     * @see #addObjectId(byte[])
     * @see #addObjectId(int, int, int, int)
     */
    EditableArray addObjectId( String hex );

    /**
     * Adds to this array an ObjectId with the supplied 12-byte value.
     * 
     * @param bytes the 12-byte value for the ObjectId
     * @return This document, to allow for chaining methods
     * @see #addObjectId(String)
     * @see #addObjectId(int, int, int, int)
     */
    EditableArray addObjectId( byte[] bytes );

    /**
     * Adds to this array an ObjectId with the supplied time, machine, process, and increment.
     * 
     * @param time the Unix-style timestamp, which is a signed integer representing the number of seconds before or after January
     *        1st 1970 (UTC)
     * @param machine the first three bytes of the (md5) hash of the machine host name, or of the mac/network address, or the
     *        virtual machine id
     * @param process the 2 bytes of the process id (or thread id) of the process generating the object id
     * @param inc an ever incrementing value, or a random number if a counter can't be used in the language/runtime
     * @return This document, to allow for chaining methods
     * @see #addObjectId(String)
     * @see #addObjectId(byte[])
     */
    EditableArray addObjectId( int time,
                               int machine,
                               int process,
                               int inc );

    /**
     * Adds to this array a regular expression with the supplied pattern string.
     * 
     * @param pattern the regular expression pattern string
     * @return This document, to allow for chaining methods
     */
    EditableArray addRegularExpression( String pattern );

    /**
     * Adds to this array a regular expression with the supplied pattern string and option flags.
     * 
     * @param pattern the regular expression pattern string
     * @param flags the bitwise-anded {@link Pattern} options: {@link Pattern#CANON_EQ}, {@link Pattern#CASE_INSENSITIVE},
     *        {@link Pattern#CASE_INSENSITIVE}, {@link Pattern#COMMENTS}, {@link Pattern#DOTALL}, {@link Pattern#LITERAL},
     *        {@link Pattern#MULTILINE}, {@link Pattern#UNICODE_CASE}, and {@link Pattern#UNIX_LINES}
     * @return This document, to allow for chaining methods
     */
    EditableArray addRegularExpression( String pattern,
                                        int flags );

    /**
     * Adds to this array a {@link Null} value.
     * 
     * @return This document, to allow for chaining methods
     * @see #isNull(String)
     * @see #isNullOrMissing(String)
     */
    EditableArray addNull();

    /**
     * Adds to this array a {@link Binary} value with the supplied type and content.
     * 
     * @param type one of the {@link BinaryType BSON type} constants denoting the type of the {@link Binary} value
     * @param data the bytes for the {@link Binary} value
     * @return This document, to allow for chaining methods
     */
    EditableArray addBinary( byte type,
                             byte[] data );

    /**
     * Adds to this array the supplied UUID.
     * 
     * @param uuid the UUID value
     * @return This document, to allow for chaining methods
     */
    EditableArray addUuid( UUID uuid );

    /**
     * Adds to this array a {@link Code} with the supplied JavaScript code.
     * 
     * @param code the code
     * @param includeScope true if the code should include a scope (and if this method should return an {@link EditableArray} for
     *        this scope document), or false otherwise
     * @return if <code>includeScope</code> is <code>true</code>, then the {@link EditableArray} for the scope; otherwise, this
     *         document to allow for chaining methods
     * @see #addCode(String, Document)
     */
    EditableDocument addCode( String code,
                              boolean includeScope );

    /**
     * Adds to this array a {@link CodeWithScope} with the supplied JavaScript code and scope.
     * 
     * @param code the code
     * @param scope the scope in which the JavaScript code should be evaulated, or null if there is no scope
     * @return the {@link EditableDocument} for the scope, or null if the <code>scope</code> reference is null
     * @see #addCode(String, boolean)
     */
    EditableDocument addCode( String code,
                              Document scope );

    /**
     * Adds the supplied boolean value to this array, if and only if an equivalent value doesn't already exist in the array.
     * 
     * @param value the new value for the field
     * @return This document, to allow for chaining methods
     */
    EditableArray addBooleanIfAbsent( boolean value );

    /**
     * Adds the supplied integer value to this array, if and only if an equivalent value doesn't already exist in the array.
     * 
     * @param value the new value for the field
     * @return This document, to allow for chaining methods
     */
    EditableArray addNumberIfAbsent( int value );

    /**
     * Adds the supplied long value to this array, if and only if an equivalent value doesn't already exist in the array.
     * 
     * @param value the new value for the field
     * @return This document, to allow for chaining methods
     */
    EditableArray addNumberIfAbsent( long value );

    /**
     * Adds the supplied float value to this array, if and only if an equivalent value doesn't already exist in the array.
     * 
     * @param value the new value for the field
     * @return This document, to allow for chaining methods
     */
    EditableArray addNumberIfAbsent( float value );

    /**
     * Adds the supplied double value to this array, if and only if an equivalent value doesn't already exist in the array.
     * 
     * @param value the new value for the field
     * @return This document, to allow for chaining methods
     */
    EditableArray addNumberIfAbsent( double value );

    /**
     * Adds the supplied string value to this array, if and only if an equivalent value doesn't already exist in the array.
     * 
     * @param value the new value for the field
     * @return This document, to allow for chaining methods
     */
    EditableArray addStringIfAbsent( String value );

    /**
     * Adds to this array a Symbol with the supplied string, if and only if an equivalent value doesn't already exist in the
     * array.
     * 
     * @param value the new value for the field
     * @return This document, to allow for chaining methods
     * @see #addString(String)
     */
    EditableArray addSymbolIfAbsent( String value );

    /**
     * Adds to this array the supplied document, if and only if an equivalent value doesn't already exist in the array.
     * 
     * @param document the document
     * @return The editable document that was just added to this array; never null and may or may not be the same instance as the
     *         supplied <code>document</code>.
     */
    EditableDocument addDocumentIfAbsent( Document document );

    /**
     * Adds to this array the supplied array, if and only if an equivalent value doesn't already exist in the array.
     * 
     * @param array the array
     * @return The editable array that was just added to this array; never null and may or may not be the same instance as the
     *         supplied <code>array</code>.
     */
    EditableArray addArrayIfAbsent( Array array );

    /**
     * Adds to this array the supplied date, if and only if an equivalent value doesn't already exist in the array.
     * 
     * @param value the new value for the field
     * @return This document, to allow for chaining methods
     */
    EditableArray addDateIfAbsent( Date value );

    /**
     * Adds to this array a Date with the supplied ISO-8601 string, if and only if an equivalent value doesn't already exist in
     * the array.
     * 
     * @param isoDate the new value for the field
     * @return This document, to allow for chaining methods
     * @see #addDate(Date)
     * @throws ParseException if the supplied value could not be parsed into a valid date
     */
    EditableArray addDateIfAbsent( String isoDate ) throws ParseException;

    /**
     * Adds to this array a Timestamp with the supplied time in seconds and increment value, if and only if an equivalent value
     * doesn't already exist in the array.
     * 
     * @param timeInSeconds the time in seconds for the new Timestamp
     * @param increment the time increment for the new Timestamp
     * @return This document, to allow for chaining methods
     * @see #addDate(Date)
     */
    EditableArray addTimestampIfAbsent( int timeInSeconds,
                                        int increment );

    /**
     * Adds to this array an ObjectId with the supplied hexadecimal string, if and only if an equivalent value doesn't already
     * exist in the array.
     * 
     * @param hex the hexadecimal binary value for the ObjectId
     * @return This document, to allow for chaining methods
     * @see #addObjectId(byte[])
     * @see #addObjectId(int, int, int, int)
     */
    EditableArray addObjectIdIfAbsent( String hex );

    /**
     * Adds to this array an ObjectId with the supplied 12-byte value, if and only if an equivalent value doesn't already exist in
     * the array.
     * 
     * @param bytes the 12-byte value for the ObjectId
     * @return This document, to allow for chaining methods
     * @see #addObjectId(String)
     * @see #addObjectId(int, int, int, int)
     */
    EditableArray addObjectIdIfAbsent( byte[] bytes );

    /**
     * Adds to this array an ObjectId with the supplied time, machine, process, and increment, if and only if an equivalent value
     * doesn't already exist in the array.
     * 
     * @param time the Unix-style timestamp, which is a signed integer representing the number of seconds before or after January
     *        1st 1970 (UTC)
     * @param machine the first three bytes of the (md5) hash of the machine host name, or of the mac/network address, or the
     *        virtual machine id
     * @param process the 2 bytes of the process id (or thread id) of the process generating the object id
     * @param inc an ever incrementing value, or a random number if a counter can't be used in the language/runtime
     * @return This document, to allow for chaining methods
     * @see #addObjectId(String)
     * @see #addObjectId(byte[])
     */
    EditableArray addObjectIdIfAbsent( int time,
                                       int machine,
                                       int process,
                                       int inc );

    /**
     * Adds to this array a regular expression with the supplied pattern string, if and only if an equivalent value doesn't
     * already exist in the array.
     * 
     * @param pattern the regular expression pattern string
     * @return This document, to allow for chaining methods
     */
    EditableArray addRegularExpressionIfAbsent( String pattern );

    /**
     * Adds to this array a regular expression with the supplied pattern string and option flags, if and only if an equivalent
     * value doesn't already exist in the array.
     * 
     * @param pattern the regular expression pattern string
     * @param flags the bitwise-anded {@link Pattern} options: {@link Pattern#CANON_EQ}, {@link Pattern#CASE_INSENSITIVE},
     *        {@link Pattern#CASE_INSENSITIVE}, {@link Pattern#COMMENTS}, {@link Pattern#DOTALL}, {@link Pattern#LITERAL},
     *        {@link Pattern#MULTILINE}, {@link Pattern#UNICODE_CASE}, and {@link Pattern#UNIX_LINES}
     * @return This document, to allow for chaining methods
     */
    EditableArray addRegularExpressionIfAbsent( String pattern,
                                                int flags );

    /**
     * Adds to this array a {@link Null} value, if and only if there is not already a null value in the array.
     * 
     * @return This document, to allow for chaining methods
     * @see #isNull(String)
     * @see #isNullOrMissing(String)
     */
    EditableArray addNullIfAbsent();

    /**
     * Adds to this array a {@link Binary} value with the supplied type and content, if and only if an equivalent value doesn't
     * already exist in the array.
     * 
     * @param type one of the {@link BinaryType BSON type} constants denoting the type of the {@link Binary} value
     * @param data the bytes for the {@link Binary} value
     * @return This document, to allow for chaining methods
     */
    EditableArray addBinaryIfAbsent( byte type,
                                     byte[] data );

    /**
     * Adds to this array the supplied UUID, if and only if an equivalent value doesn't already exist in the array.
     * 
     * @param uuid the UUID value
     * @return This document, to allow for chaining methods
     */
    EditableArray addUuidIfAbsent( UUID uuid );

    /**
     * Adds to this array a {@link CodeWithScope} with the supplied JavaScript code and scope, if and only if an equivalent value
     * doesn't already exist in the array.
     * 
     * @param code the code
     * @param scope the scope in which the JavaScript code should be evaulated, or null if there is no scope
     * @return the {@link EditableDocument} for the scope, or null if the <code>scope</code> reference is null
     * @see #addCode(String, boolean)
     */
    EditableDocument addCodeIfAbsent( String code,
                                      Document scope );

}
