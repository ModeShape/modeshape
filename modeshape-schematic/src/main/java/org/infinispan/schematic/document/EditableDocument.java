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
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.infinispan.schematic.document.Bson.BinaryType;

public interface EditableDocument extends Document {

    /**
     * Unwrap this editor to obtain the potentially wrapped document.
     * 
     * @return the wrapped document, or this object; never null
     */
    Document unwrap();

    /**
     * Remove the field with the supplied name, and return the value.
     * 
     * @param name The name of the field
     * @return the value that was removed, or null if there was no such value
     */
    Object remove( String name );

    /**
     * Remove all fields from this document.
     */
    void removeAll();

    /**
     * Sets on this object all name/value pairs from the supplied object. If the supplied object is null, this method does
     * nothing.
     * 
     * @param object the object containing the name/value pairs to be set on this object
     */
    void putAll( Document object );

    /**
     * Sets on this object all key/value pairs from the supplied map. If the supplied map is null, this method does nothing.
     * 
     * @param map the map containing the name/value pairs to be set on this object
     */
    void putAll( Map<? extends String, ? extends Object> map );

    /**
     * Set the value for the field with the given name to the supplied value.
     * 
     * @param name The name of the field
     * @param value the new value for the field
     * @return This document, to allow for chaining methods
     */
    EditableDocument set( String name,
                          Object value );

    /**
     * Set the value for the field with the given name to the supplied boolean value.
     * 
     * @param name The name of the field
     * @param value the new value for the field
     * @return This document, to allow for chaining methods
     */
    EditableDocument setBoolean( String name,
                                 boolean value );

    /**
     * Set the value for the field with the given name to the supplied integer value.
     * 
     * @param name The name of the field
     * @param value the new value for the field
     * @return This document, to allow for chaining methods
     */
    EditableDocument setNumber( String name,
                                int value );

    /**
     * Set the value for the field with the given name to the supplied long value.
     * 
     * @param name The name of the field
     * @param value the new value for the field
     * @return This document, to allow for chaining methods
     */
    EditableDocument setNumber( String name,
                                long value );

    /**
     * Set the value for the field with the given name to the supplied float value.
     * 
     * @param name The name of the field
     * @param value the new value for the field
     * @return This document, to allow for chaining methods
     */
    EditableDocument setNumber( String name,
                                float value );

    /**
     * Set the value for the field with the given name to the supplied double value.
     * 
     * @param name The name of the field
     * @param value the new value for the field
     * @return This document, to allow for chaining methods
     */
    EditableDocument setNumber( String name,
                                double value );

    /**
     * Set the value for the field with the given name to the supplied string value.
     * 
     * @param name The name of the field
     * @param value the new value for the field
     * @return This document, to allow for chaining methods
     */
    EditableDocument setString( String name,
                                String value );

    /**
     * Set the value for the field with the given name to a {@link Symbol} created from the supplied string value. Symbols are
     * defined in the BSON specification as being similar to a string but which exists for those languages that have a specific
     * symbol type. Symbols are serialized to JSON as a normal string.
     * 
     * @param name The name of the field
     * @param value the new value for the field
     * @return This document, to allow for chaining methods
     * @see #setString(String, String)
     */
    EditableDocument setSymbol( String name,
                                String value );

    /**
     * Set the value for the field with the given name to be a new, empty Document.
     * 
     * @param name The name of the field
     * @return The editable document that was just created; never null
     */
    EditableDocument setDocument( String name );

    /**
     * Set the value for the field with the given name to be the supplied Document.
     * 
     * @param name The name of the field
     * @param document the document
     * @return The editable document that was just set as the value for the named field; never null and may or may not be the same
     *         instance as the supplied <code>document</code>.
     */
    EditableDocument setDocument( String name,
                                  Document document );

    /**
     * Get the existing document value in this document for the given field name.
     * 
     * @param name The name of the pair
     * @return The editable document field value, if found, or null if there is no such pair or if the value is not a document
     */
    @Override
    EditableDocument getDocument( String name );

    /**
     * Get the existing document value in this document for the given field name, or create a new document if there is no existing
     * document at this field.
     * 
     * @param name The name of the pair
     * @return The editable document field value; never null
     */
    EditableDocument getOrCreateDocument( String name );

    /**
     * Set the value for the field with the given name to be a new, empty array.
     * 
     * @param name The name of the field
     * @return The editable array that was just created; never null
     */
    EditableArray setArray( String name );

    /**
     * Set the value for the field with the given name to be the supplied array.
     * 
     * @param name The name of the field
     * @param array the array
     * @return The editable array that was just set as the value for the named field; never null and may or may not be the same
     *         instance as the supplied <code>array</code>.
     */
    EditableArray setArray( String name,
                            Array array );

    /**
     * Set the value for the field with the given name to be the supplied array.
     * 
     * @param name The name of the field
     * @param values the (valid) values for the array
     * @return The editable array that was just set as the value for the named field; never null and may or may not be the same
     *         instance as the supplied <code>array</code>.
     */
    EditableArray setArray( String name,
                            Object... values );

    /**
     * Get the existing array value in this document for the given field name.
     * 
     * @param name The name of the pair
     * @return The editable array field value (as a list), if found, or null if there is no such pair or if the value is not an
     *         array
     */
    @Override
    EditableArray getArray( String name );

    /**
     * Get the existing array value in this document for the given field name, or create a new array if there is no existing array
     * at this field.
     * 
     * @param name The name of the pair
     * @return The editable array field value; never null
     */
    EditableArray getOrCreateArray( String name );

    /**
     * Set the value for the field with the given name to the supplied date value.
     * 
     * @param name The name of the field
     * @param value the new value for the field
     * @return This document, to allow for chaining methods
     */
    EditableDocument setDate( String name,
                              Date value );

    /**
     * Set the value for the field with the given name to the date value parsed from the ISO-8601 date representation.
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
     * @param name The name of the field
     * @param isoDate the new value for the field
     * @return This document, to allow for chaining methods
     * @throws ParseException if the supplied value could not be parsed into a valid date
     */
    EditableDocument setDate( String name,
                              String isoDate ) throws ParseException;

    /**
     * Set the value for the field with the given name to a {@link Timestamp} with the supplied time in seconds and increment.
     * Note that {@link Date} values are recommended for most purposes, as they are better suited to most applications'
     * representations of time instants.
     * 
     * @param name The name of the field
     * @param timeInSeconds the time in seconds for the new Timestamp
     * @param increment the time increment for the new Timestamp
     * @return This document, to allow for chaining methods
     * @see #setDate(String, Date)
     */
    EditableDocument setTimestamp( String name,
                                   int timeInSeconds,
                                   int increment );

    /**
     * Set the value for the field with the given name to an {@link ObjectId} created from the supplied hexadecimal binary value.
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
     * @param name The name of the field
     * @param hex the hexadecimal binary value for the ObjectId
     * @return This document, to allow for chaining methods
     * @see #setObjectId(String, byte[])
     * @see #setObjectId(String, int, int, int, int)
     */
    EditableDocument setObjectId( String name,
                                  String hex );

    /**
     * Set the value for the field with the given name to an {@link ObjectId} created from the supplied 12-byte binary value.
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
     * @param name The name of the field
     * @param bytes the 12-byte value for the ObjectId
     * @return This document, to allow for chaining methods
     * @see #setObjectId(String, String)
     * @see #setObjectId(String, int, int, int, int)
     */
    EditableDocument setObjectId( String name,
                                  byte[] bytes );

    /**
     * Set the value for the field with the given name to an {@link ObjectId} created from the supplied hexadecimal binary value.
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
     * @param name The name of the field
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
    EditableDocument setObjectId( String name,
                                  int time,
                                  int machine,
                                  int process,
                                  int inc );

    /**
     * Set the value for the field with the given name to the supplied regular expression. Regular expression values are
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
     * @param name The name of the field
     * @param pattern the regular expression pattern string
     * @return This document, to allow for chaining methods
     * @see #setRegularExpression(String, String, int)
     */
    EditableDocument setRegularExpression( String name,
                                           String pattern );

    /**
     * Set the value for the field with the given name to the supplied regular expression. Regular expression values are
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
     * @param name The name of the field
     * @param pattern the regular expression pattern string
     * @param flags the bitwise-anded {@link Pattern} options: {@link Pattern#CANON_EQ}, {@link Pattern#CASE_INSENSITIVE},
     *        {@link Pattern#CASE_INSENSITIVE}, {@link Pattern#COMMENTS}, {@link Pattern#DOTALL}, {@link Pattern#LITERAL},
     *        {@link Pattern#MULTILINE}, {@link Pattern#UNICODE_CASE}, and {@link Pattern#UNIX_LINES}
     * @return This document, to allow for chaining methods
     * @see #setRegularExpression(String, String)
     */
    EditableDocument setRegularExpression( String name,
                                           String pattern,
                                           int flags );

    /**
     * Set the value for the field with the given name to be a null value. Both JSON and BSON formats support null values, and
     * {@link Null} is used for the value in the in-memory representation. The {@link #isNull(String)} methods can be used to
     * determine if a field has been set to null, or {@link #isNullOrMissing(String)} if the field has not be set or if it has
     * been set to null.
     * 
     * @param name The name of the field
     * @return This document, to allow for chaining methods
     * @see #isNull(String)
     * @see #isNullOrMissing(String)
     */
    EditableDocument setNull( String name );

    /**
     * Set the value for the field with the given name to be a binary value. JSON does not formally support binary values, and so
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
     * @param name The name of the field
     * @param type one of the {@link BinaryType BSON type} constants denoting the type of the {@link Binary} value
     * @param data the bytes for the {@link Binary} value
     * @return This document, to allow for chaining methods
     */
    EditableDocument setBinary( String name,
                                byte type,
                                byte[] data );

    /**
     * Set the value for the field with the given name to be a {@link UUID}. JSON does not formally support binary values, and so
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
     * @param name The name of the field
     * @param uuid the UUID value
     * @return This document, to allow for chaining methods
     */
    EditableDocument setUuid( String name,
                              UUID uuid );

    /**
     * Set the value for the field with the given name to be a {@link Code} or {@link CodeWithScope}. JSON does not formally
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
     * Note that when <code>includeScope</code> is <code>true</code>, the returned {@link EditableDocument} can be used to
     * populate the scope document.
     * 
     * @param name The name of the field
     * @param code the code
     * @param includeScope true if the code should include a scope (and if this method should return an {@link EditableDocument}
     *        for this scope document), or false otherwise
     * @return if <code>includeScope</code> is <code>true</code>, then the {@link EditableDocument} for the scope; otherwise, this
     *         document to allow for chaining methods
     * @see #setCode(String, String, Document)
     */
    EditableDocument setCode( String name,
                              String code,
                              boolean includeScope );

    /**
     * Set the value for the field with the given name to be a {@link Code} or {@link CodeWithScope}. JSON does not formally
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
     * @param name The name of the field
     * @param code the code
     * @param scope the scope in which the JavaScript code should be evaulated, or null if there is no scope
     * @return the {@link EditableDocument} for the scope, or null if the <code>scope</code> reference is null
     * @see #setCode(String, String, boolean)
     */
    EditableDocument setCode( String name,
                              String code,
                              Document scope );

}
