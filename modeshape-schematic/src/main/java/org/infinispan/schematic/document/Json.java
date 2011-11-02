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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.UUID;
import java.util.regex.Pattern;
import org.infinispan.schematic.internal.document.CompactJsonWriter;
import org.infinispan.schematic.internal.document.JsonReader;
import org.infinispan.schematic.internal.document.JsonWriter;
import org.infinispan.schematic.internal.document.PrettyJsonWriter;

/**
 * A utility class for working with JSON documents. This class is able to read and write JSON documents that are in a special
 * modified format. <h3>Modified Format</h3> Any JSON document written in this modified format is still a valid JSON document.
 * However, certain value types supported by BSON are written as nested objects in particular patterns. In fact, it is nearly
 * identical to the JSON serialization used by MongoDB. All standard JSON values are written as expected, but the types unique to
 * BSON are written as follows:
 * <p>
 * <table border="1" cellspacing="0" cellpadding="3">
 * <tr>
 * <th>BSON Type</th>
 * <th>Class</th>
 * <th>Format</th>
 * <th>Example</th>
 * </tr>
 * <tr>
 * <td>Symbol</td>
 * <td>{@link Symbol}</td>
 * <td>"<i>value</i>"</td>
 * <td>"The quick brown fox"</td>
 * </tr>
 * <tr>
 * <td>Regular Expression</td>
 * <td>{@link Pattern}</td>
 * <td>{ "$regex" : "<i>pattern</i>", "$otions" : "<i>flags</i>" }</td>
 * <td>{ "$regex" : "[CH]at\sin", "$options" : "im" }</td>
 * </tr>
 * <tr>
 * <td>Date</td>
 * <td>{@link Date}</td>
 * <td>{ "$date" : "<i>yyyy-MM-dd</i>T<i>HH:mm:ss</i>Z" }</td>
 * <td>{ "$date" : "2011-06-11T08:44:25Z" }</td>
 * </tr>
 * <tr>
 * <td>Timestamp</td>
 * <td>{@link Timestamp}</td>
 * <td>{ "$ts" : <i>timeValue</i>, "$inc" : <i>incValue</i> }</td>
 * <td>"\/TS("2011-06-11T08:44:25Z")\/"</td>
 * </tr>
 * <tr>
 * <td>ObjectId</td>
 * <td>{@link ObjectId}</td>
 * <td>{ "$oid" : "<i>12bytesOfIdInBase16</i>" }</td>
 * <td>{ "$oid" : "0000012c0000c8000900000f" }</td>
 * </tr>
 * <tr>
 * <td>Binary</td>
 * <td>{@link Binary}</td>
 * <td>{ "$type" : <i>typeAsInt</i>, "$base64" : "<i>bytesInBase64</i>" }"</td>
 * <td>{ "$type" : 0, "$base64" : "TWFuIGlzIGRpc3R" }"</td>
 * </tr>
 * <tr>
 * <td>UUID</td>
 * <td>{@link UUID}</td>
 * <td>{ "$uuid" : "<i>string-form-of-uuid</i>" }</td>
 * <td>{ "$uuid" : "09e0e949-bba4-459c-bb1d-9352e5ee8958" }</td>
 * </tr>
 * <tr>
 * <td>Code</td>
 * <td>{@link Code}</td>
 * <td>{ "$code" : "<i>code</i>" }</td>
 * <td>{ "$code" : "244-I2" }</td>
 * </tr>
 * <tr>
 * <td>CodeWithScope</td>
 * <td>{@link CodeWithScope}</td>
 * <td>{ "$code" : "<i>code</i>", "$scope" : <i>scope document</i> }</td>
 * <td>{ "$code" : "244-I2", "$scope" : { "name" : "Joe" } }</td>
 * </tr>
 * <tr>
 * <td>MinKey</td>
 * <td>{@link MinKey}</td>
 * <td>"MinKey"</td>
 * <td>"MinKey"</td>
 * </tr>
 * <tr>
 * <td>MaxKey</td>
 * <td>{@link MaxKey}</td>
 * <td>"MaxKey"</td>
 * <td>"MaxKey"</td>
 * </tr>
 * <tr>
 * <td>Null value</td>
 * <td>n/a</td>
 * <td>null</td>
 * <td>null</td>
 * </tr>
 * </table>
 * </p>
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 * @since 5.1
 */
public class Json {

    private static final CompactJsonWriter SHARED_COMPACT_WRITER = new CompactJsonWriter();

    /**
     * A set of field names that are reserved for special formatting of non-standard JSON value types as nested objects.
     * 
     * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
     * @since 5.1
     */
    public class ReservedField {
        /**
         * The "$oid" field name, used within an {@link ObjectId} value. A single ObjectId will be written
         */
        public static final String OBJECT_ID = "$oid";
        public static final String DATE = "$date";
        public static final String TIMESTAMP = "$ts";
        public static final String INCREMENT = "$inc";
        public static final String REGEX_PATTERN = "$regex";
        public static final String REGEX_OPTIONS = "$options";
        public static final String BINARY_TYPE = "$type";
        public static final String BASE_64 = "$base64";
        public static final String UUID = "$uuid";
        public static final String CODE = "$code";
        public static final String SCOPE = "$scope";
    }

    protected static JsonWriter getCompactJsonWriter() {
        return SHARED_COMPACT_WRITER;
    }

    protected static JsonWriter getPrettyWriter() {
        return new PrettyJsonWriter();
    }

    private static final JsonReader SHARED_READER = new JsonReader();

    protected static JsonReader getReader() {
        return SHARED_READER;
    }

    /**
     * Read the JSON representation from supplied URL and construct the {@link Document} representation, using the
     * {@link Charset#defaultCharset() default character set}.
     * <p>
     * This method will read standard JSON and modified JSON, and tolerates whitespace and use of several delimeters, including
     * the standard ':' as well as '=' and '=>'.
     * </p>
     * 
     * @param url the URL of the JSON document; may not be null and must be resolvable
     * @return the in-memory {@link Document} representation
     * @throws ParsingException if there was a problem reading from the stream
     */
    public static Document read( URL url ) throws ParsingException {
        return SHARED_READER.read(url);
    }

    /**
     * Read the JSON representation from supplied input stream and construct the {@link Document} representation, using the
     * {@link Charset#defaultCharset() default character set}.
     * <p>
     * This method will read standard JSON and modified JSON, and tolerates whitespace and use of several delimeters, including
     * the standard ':' as well as '=' and '=>'.
     * </p>
     * 
     * @param stream the input stream; may not be null
     * @return the in-memory {@link Document} representation
     * @throws ParsingException if there was a problem reading from the stream
     */
    public static Document read( InputStream stream ) throws ParsingException {
        return SHARED_READER.read(stream);
    }

    /**
     * Read the JSON representation from supplied input stream and construct the {@link Document} representation, using the
     * supplied {@link Charset character set}.
     * <p>
     * This method will read standard JSON and modified JSON, and tolerates whitespace and use of several delimeters, including
     * the standard ':' as well as '=' and '=>'.
     * </p>
     * 
     * @param stream the input stream; may not be null
     * @param charset the character set that should be used; may not be null
     * @return the in-memory {@link Document} representation
     * @throws ParsingException if there was a problem reading from the stream
     */
    public static Document read( InputStream stream,
                                 Charset charset ) throws ParsingException {
        return SHARED_READER.read(stream, charset);
    }

    /**
     * Read the JSON representation from supplied input stream and construct the {@link Document} representation.
     * <p>
     * This method will read standard JSON and modified JSON, and tolerates whitespace and use of several delimeters, including
     * the standard ':' as well as '=' and '=>'.
     * </p>
     * 
     * @param reader the IO reader; may not be null
     * @return the in-memory {@link Document} representation
     * @throws ParsingException if there was a problem reading from the stream
     */
    public static Document read( Reader reader ) throws ParsingException {
        return SHARED_READER.read(reader);
    }

    /**
     * Read the supplied JSON representation and construct the {@link Document} representation.
     * <p>
     * This method will read standard JSON and modified JSON, and tolerates whitespace and use of several delimeters, including
     * the standard ':' as well as '=' and '=>'.
     * </p>
     * 
     * @param json the JSON document string; may not be null
     * @return the in-memory {@link Document} representation
     * @throws ParsingException if there was a problem reading from the stream
     */
    public static Document read( String json ) throws ParsingException {
        return SHARED_READER.read(json);
    }

    /**
     * Return the modified JSON representation for the supplied in-memory {@link Document}. The resulting JSON will have no
     * embedded line feeds or extra spaces.
     * <p>
     * This format is compact and easy for software to read, but usually very difficult for people to read anything but very small
     * documents.
     * </p>
     * 
     * @param bson the BSON object or BSON value; may not be null
     * @return the string; may not be null
     */
    public static String write( Document bson ) {
        return getCompactJsonWriter().write(bson);
    }

    /**
     * Return the modified JSON representation for the supplied object value. The resulting JSON will have no embedded line feeds
     * or extra spaces.
     * <p>
     * This format is compact and easy for software to read, but usually very difficult for people to read anything but very small
     * documents.
     * </p>
     * 
     * @param value the BSON object or BSON value; may not be null
     * @return the string; may not be null
     */
    public static String write( Object value ) {
        return getCompactJsonWriter().write(value);
    }

    /**
     * Write to the supplied writer the modified JSON representation of the supplied in-memory {@link Document}. The resulting
     * JSON will have no embedded line feeds or extra spaces.
     * <p>
     * This format is compact and easy for software to read, but usually very difficult for people to read anything but very small
     * documents.
     * </p>
     * 
     * @param bson the BSON object or BSON value; may not be null
     * @param writer the writer; may not be null
     * @throws IOException if there was a problem reading from the stream
     */
    public static void write( Document bson,
                              Writer writer ) throws IOException {
        getCompactJsonWriter().write(bson, writer);
    }

    /**
     * Write to the supplied writer the modified JSON representation of the supplied in-memory {@link Document}. The resulting
     * JSON will have no embedded line feeds or extra spaces.
     * <p>
     * This format is compact and easy for software to read, but usually very difficult for people to read anything but very small
     * documents.
     * </p>
     * 
     * @param bson the BSON object or BSON value; may not be null
     * @param stream the output stream; may not be null
     * @throws IOException if there was a problem reading from the stream
     */
    public static void write( Document bson,
                              OutputStream stream ) throws IOException {
        getCompactJsonWriter().write(bson, stream);
    }

    /**
     * Return the modified JSON representation for the supplied in-memory {@link Document}. The resulting JSON will be indented
     * for each name/value pair and each array value.
     * <p>
     * This format is very readable by people and software, but is less compact due to the extra whitespace.
     * </p>
     * 
     * @param bson the BSON object or BSON value; may not be null
     * @return the JSON representation; never null
     */
    public static String writePretty( Document bson ) {
        return getPrettyWriter().write(bson);
    }

    /**
     * Return the modified JSON representation for the supplied object value. The resulting JSON will be indented for each
     * name/value pair and each array value.
     * <p>
     * This format is very readable by people and software, but is less compact due to the extra whitespace.
     * </p>
     * 
     * @param value the BSON object or BSON value; may not be null
     * @return the JSON representation; never null
     */
    public static String writePretty( Object value ) {
        return getPrettyWriter().write(value);
    }

    /**
     * Write to the supplied writer the modified JSON representation of the supplied in-memory {@link Document}. The resulting
     * JSON will be indented for each name/value pair and each array value.
     * <p>
     * This format is very readable by people and software, but is less compact due to the extra whitespace.
     * </p>
     * 
     * @param bson the BSON object or BSON value; may not be null
     * @param writer the writer; may not be null
     * @throws IOException if there was a problem reading from the stream
     */
    public static void writePretty( Document bson,
                                    Writer writer ) throws IOException {
        getPrettyWriter().write(bson, writer);
    }

    /**
     * Write to the supplied writer the modified JSON representation of the supplied in-memory {@link Document}. The resulting
     * JSON will be indented for each name/value pair and each array value.
     * <p>
     * This format is very readable by people and software, but is less compact due to the extra whitespace.
     * </p>
     * 
     * @param bson the BSON object or BSON value; may not be null
     * @param stream the output stream; may not be null
     * @throws IOException if there was a problem reading from the stream
     */
    public static void writePretty( Document bson,
                                    OutputStream stream ) throws IOException {
        getPrettyWriter().write(bson, stream);
    }

}
