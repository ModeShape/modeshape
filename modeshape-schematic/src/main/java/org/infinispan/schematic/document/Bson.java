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

import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.SimpleTimeZone;
import java.util.regex.Pattern;
import org.infinispan.schematic.internal.document.BsonReader;
import org.infinispan.schematic.internal.document.BsonWriter;

/**
 * A utility class for working with BSON documents.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 * @since 5.1
 */
public class Bson {

    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    private static final ThreadLocal<SoftReference<DateFormat>> dateFormatter = new ThreadLocal<SoftReference<DateFormat>>();

    protected static final String DATE_FORMAT_FOR_PARSING = "yyyy-MM-dd'T'HH:mm:ssz";
    private static final ThreadLocal<SoftReference<DateFormat>> dateFormatterForParsing = new ThreadLocal<SoftReference<DateFormat>>();

    /**
     * Obtain a {@link DateFormat} object that can be used within the current thread to format {@link Date} objects.
     * 
     * @return the formatter; never null
     */
    public static DateFormat getDateFormatter() {
        SoftReference<DateFormat> ref = dateFormatter.get();
        DateFormat formatter = ref != null ? ref.get() : null;
        if (formatter == null) {
            formatter = new SimpleDateFormat(DATE_FORMAT);
            formatter.setCalendar(new GregorianCalendar(new SimpleTimeZone(0, "GMT")));
            dateFormatter.set(new SoftReference<DateFormat>(formatter));
        }
        return formatter;
    }

    public static DateFormat getDateParsingFormatter() {
        SoftReference<DateFormat> ref = dateFormatterForParsing.get();
        DateFormat formatter = ref != null ? ref.get() : null;
        if (formatter == null) {
            formatter = new SimpleDateFormat(DATE_FORMAT_FOR_PARSING);
            formatter.setCalendar(new GregorianCalendar(new SimpleTimeZone(0, "GMT")));
            dateFormatterForParsing.set(new SoftReference<DateFormat>(formatter));
        }
        return formatter;
    }

    /**
     * Byte used for the end of a document within a BSON stream.
     */
    public static final byte END_OF_DOCUMENT = 0x00;

    /**
     * Byte used for the end of a string within a BSON stream.
     */
    public static final byte END_OF_STRING = 0x00;

    /**
     * The bytes used for the types within a BSON stream.
     */
    public static final class Type {
        public static final byte DOUBLE = 0x01;
        public static final byte STRING = 0x02;
        public static final byte DOCUMENT = 0x03;
        public static final byte ARRAY = 0x04;
        public static final byte BINARY = 0x05;
        public static final byte UNDEFINED = 0x06;
        public static final byte OBJECTID = 0x07;
        public static final byte BOOLEAN = 0x08;
        public static final byte DATETIME = 0x09;
        public static final byte NULL = 0x0A;
        public static final byte REGEX = 0x0B;
        public static final byte DBPOINTER = 0x0C;
        public static final byte JAVASCRIPT = 0x0D;
        public static final byte SYMBOL = 0x0E;
        public static final byte JAVASCRIPT_WITH_SCOPE = 0x0F;
        public static final byte INT32 = 0x10;
        public static final byte TIMESTAMP = 0x11;
        public static final byte INT64 = 0x12;
        public static final byte MINKEY = (byte)0xFF;
        public static final byte MAXKEY = 0x7f;
    }

    /**
     * The bytes used for the subtypes of a binary value within a BSON stream.
     */
    public static final class BinaryType {
        /**
         * The most common binary subtype, and the one should be used as the default.
         */
        public static final byte GENERAL = 0x00;
        /**
         * The binary subtype that represents functions.
         */
        public static final byte FUNCTION = 0x01;
        /**
         * The old generic subtype. This used to be the default subtype, but was deprecated in favor of {@link #GENERAL}. Drivers
         * and tools should be sure to handle this type appropriately.
         * 
         * @deprecated Use {@link #GENERAL} instead
         */
        @Deprecated
        public static final byte BINARY = 0x02;
        public static final byte UUID = 0x03;
        public static final byte MD5 = 0x05;
        public static final byte USER_DEFINED = (byte)0x80;
    }

    private static final BsonWriter SHARED_WRITER = new BsonWriter();

    protected static BsonWriter getBsonWriter() {
        return SHARED_WRITER;
    }

    /**
     * Write to the supplied stream the binary BSON representation of the supplied in-memory {@link Document}.
     * 
     * @param bson the BSON object or BSON value; may not be null
     * @param stream the output stream; may not be null
     * @throws IOException if there was a problem writing to the stream
     */
    public static void write( Document bson,
                              OutputStream stream ) throws IOException {
        getBsonWriter().write(bson, stream);
    }

    /**
     * Write to the supplied output the binary BSON representation of the supplied in-memory {@link Document}.
     * 
     * @param bson the BSON object or BSON value; may not be null
     * @param output the output; may not be null
     * @throws IOException if there was a problem writing to the ObjectOutput
     */
    public static void write( Document bson,
                              ObjectOutput output ) throws IOException {
        getBsonWriter().write(bson, output);
    }

    /**
     * Return the array of bytes containing the standard BSON binary form of the supplied in-memory {@link Document}.
     * 
     * @param object the BSON object or BSON value; may not be null
     * @return the bytes
     * @throws IOException if there was a problem reading from the stream
     */
    public static byte[] write( Object object ) throws IOException {
        return getBsonWriter().write(object);
    }

    private static final BsonReader SHARED_READER = new BsonReader();

    protected static BsonReader getReader() {
        return SHARED_READER;
    }

    /**
     * Read the binary BSON representation from supplied input stream and construct the {@link Document} representation.
     * 
     * @param stream the input stream; may not be null
     * @return the in-memory {@link Document} representation
     * @throws IOException if there was a problem reading from the stream
     */
    public static Document read( InputStream stream ) throws IOException {
        return SHARED_READER.read(stream);
    }

    /**
     * Read the binary BSON representation from supplied data input and construct the {@link Document} representation.
     * 
     * @param input the data input; may not be null
     * @return the in-memory {@link Document} representation
     * @throws IOException if there was a problem reading from the stream
     */
    public static Document read( DataInput input ) throws IOException {
        return SHARED_READER.read(input);
    }

    /**
     * Get the {@link Type} constant that describes the type of value for the given field name.
     * 
     * @param value The value
     * @return the {@link Type} constant describing the value
     */
    public static int getTypeForValue( Object value ) {
        if (value == null) return Type.NULL;
        if (value instanceof String) return Type.STRING;
        if (value instanceof Symbol) return Type.SYMBOL;
        if (value instanceof Integer) return Type.INT32;
        if (value instanceof Long) return Type.INT64;
        if (value instanceof Double) return Type.DOUBLE;
        if (value instanceof List) return Type.ARRAY;
        if (value instanceof Document) return Type.DOCUMENT;
        if (value instanceof Boolean) return Type.BOOLEAN;
        if (value instanceof Binary) return Type.BINARY;
        if (value instanceof ObjectId) return Type.OBJECTID;
        if (value instanceof Date) return Type.DATETIME;
        if (value instanceof Null) return Type.NULL;
        if (value instanceof Pattern) return Type.REGEX;
        if (value instanceof Symbol) return Type.DBPOINTER;
        if (value instanceof Code) return Type.JAVASCRIPT;
        if (value instanceof CodeWithScope) return Type.JAVASCRIPT_WITH_SCOPE;
        if (value instanceof Timestamp) return Type.TIMESTAMP;
        if (value instanceof MinKey) return Type.MINKEY;
        if (value instanceof MaxKey) return Type.MAXKEY;
        return Type.UNDEFINED;
    }
}
