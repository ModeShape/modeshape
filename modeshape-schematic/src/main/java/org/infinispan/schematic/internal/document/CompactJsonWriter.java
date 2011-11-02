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
package org.infinispan.schematic.internal.document;

import static org.infinispan.schematic.document.Json.ReservedField.BASE_64;
import static org.infinispan.schematic.document.Json.ReservedField.BINARY_TYPE;
import static org.infinispan.schematic.document.Json.ReservedField.CODE;
import static org.infinispan.schematic.document.Json.ReservedField.DATE;
import static org.infinispan.schematic.document.Json.ReservedField.INCREMENT;
import static org.infinispan.schematic.document.Json.ReservedField.OBJECT_ID;
import static org.infinispan.schematic.document.Json.ReservedField.REGEX_OPTIONS;
import static org.infinispan.schematic.document.Json.ReservedField.REGEX_PATTERN;
import static org.infinispan.schematic.document.Json.ReservedField.SCOPE;
import static org.infinispan.schematic.document.Json.ReservedField.TIMESTAMP;
import static org.infinispan.schematic.document.Json.ReservedField.UUID;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.infinispan.schematic.document.Binary;
import org.infinispan.schematic.document.Bson;
import org.infinispan.schematic.document.Code;
import org.infinispan.schematic.document.CodeWithScope;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.Document.Field;
import org.infinispan.schematic.document.MaxKey;
import org.infinispan.schematic.document.MinKey;
import org.infinispan.schematic.document.ObjectId;
import org.infinispan.schematic.document.Symbol;
import org.infinispan.schematic.document.Timestamp;

public class CompactJsonWriter implements JsonWriter {

    @Override
    public void write( Object object,
                       OutputStream stream ) throws IOException {
        Writer writer = new OutputStreamWriter(stream);
        write(object, writer);
        writer.flush();
    }

    @Override
    public void write( Object object,
                       Writer writer ) throws IOException {
        if (object == null) {
            writeNull(writer);
        } else if (object instanceof String) {
            write((String)object, writer);
        } else if (object instanceof Boolean) {
            write(((Boolean)object).booleanValue(), writer);
        } else if (object instanceof Integer) {
            write(((Integer)object).intValue(), writer);
        } else if (object instanceof Long) {
            write(((Long)object).longValue(), writer);
        } else if (object instanceof Float) {
            write(((Float)object).floatValue(), writer);
        } else if (object instanceof Double) {
            write(((Double)object).doubleValue(), writer);
        } else if (object.getClass().isArray()) {
            writeArray(object, writer);
        } else if (object instanceof ArrayEditor) {
            write((Iterable<?>)((ArrayEditor)object).unwrap(), writer);
        } else if (object instanceof DocumentEditor) {
            write(((DocumentEditor)object).unwrap(), writer);
        } else if (object instanceof Iterable) { // must check before 'BsonObject' because of inheritance
            write((Iterable<?>)object, writer);
        } else if (object instanceof Map) {
            write((Document)object, writer);
        } else if (object instanceof Binary) {
            write((Binary)object, writer);
        } else if (object instanceof Symbol) {
            write((Symbol)object, writer);
        } else if (object instanceof Pattern) {
            write((Pattern)object, writer);
        } else if (object instanceof Date) {
            write((Date)object, writer);
        } else if (object instanceof UUID) {
            write((UUID)object, writer);
        } else if (object instanceof CodeWithScope) { // must check before 'Code' because of inheritance
            write((CodeWithScope)object, writer);
        } else if (object instanceof Code) {
            write((Code)object, writer);
        } else if (object instanceof Timestamp) {
            write((Timestamp)object, writer);
        } else if (object instanceof Field) {
            write((Field)object, writer);
        } else if (object instanceof ObjectId) {
            write((ObjectId)object, writer);
        } else if (object instanceof MaxKey) {
            write((MaxKey)object, writer);
        } else if (object instanceof MinKey) {
            write((MinKey)object, writer);
        }
    }

    @Override
    public void write( Object object,
                       StringBuilder writer ) {
        try {
            write(object, new StringBuilderWriter(writer));
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public String write( Object object ) {
        StringBuilder sb = new StringBuilder();
        write(object, sb);
        return sb.toString();
    }

    protected void write( boolean value,
                          Writer writer ) throws IOException {
        writer.append(Boolean.toString(value));
    }

    protected void write( int value,
                          Writer writer ) throws IOException {
        writer.append(Integer.toString(value));
    }

    protected void write( long value,
                          Writer writer ) throws IOException {
        writer.append(Long.toString(value));
    }

    protected void write( float value,
                          Writer writer ) throws IOException {
        writer.append(Float.toString(value));
    }

    protected void write( double value,
                          Writer writer ) throws IOException {
        writer.append(Double.toString(value));
    }

    protected void writeNull( Writer writer ) throws IOException {
        writer.append("null");
    }

    protected void write( String value,
                          Writer writer ) throws IOException {
        writer.append('"').append(value).append('"');
    }

    protected void write( Symbol value,
                          Writer writer ) throws IOException {
        writer.append('"').append(value.getSymbol()).append('"');
    }

    protected void write( ObjectId value,
                          Writer writer ) throws IOException {
        write(new BasicDocument(OBJECT_ID, value.getBytesInBase16()), writer);
    }

    protected void write( Date value,
                          Writer writer ) throws IOException {
        String isoDate = Bson.getDateFormatter().format(value);
        write(new BasicDocument(DATE, isoDate), writer);
    }

    protected void write( Timestamp value,
                          Writer writer ) throws IOException {
        write(new BasicDocument(TIMESTAMP, value.getTime(), INCREMENT, value.getInc()), writer);
    }

    protected void write( MinKey value,
                          Writer writer ) throws IOException {
        write("MinKey", writer);
    }

    protected void write( MaxKey value,
                          Writer writer ) throws IOException {
        write("MaxKey", writer);
    }

    protected void write( Pattern value,
                          Writer writer ) throws IOException {
        BasicDocument obj = new BasicDocument(REGEX_PATTERN, value.pattern());
        String options = BsonUtils.regexFlagsFor(value);
        if (options.length() != 0) {
            obj.put(REGEX_OPTIONS, options);
        }
        write(obj, writer);
    }

    protected void write( Binary value,
                          Writer writer ) throws IOException {
        int type = value.getType() - 0;
        String base64 = value.getBytesInBase64();
        write(new BasicDocument(BINARY_TYPE, type, BASE_64, base64), writer);
    }

    protected void write( UUID value,
                          Writer writer ) throws IOException {
        write(new BasicDocument(UUID, value.toString()), writer);
    }

    protected void write( Code value,
                          Writer writer ) throws IOException {
        write(new BasicDocument(CODE, value.getCode()), writer);
    }

    protected void write( CodeWithScope value,
                          Writer writer ) throws IOException {
        write(new BasicDocument(CODE, value.getCode(), SCOPE, value.getScope()), writer);
    }

    protected void write( Field field,
                          Writer writer ) throws IOException {
        writer.append('"').append(field.getName()).append('"').append(' ').append(':').append(' ');
        write(field.getValue(), writer);
    }

    protected void write( Document bson,
                          Writer writer ) throws IOException {
        writer.append('{').append(' ');
        Iterator<Field> iter = bson.fields().iterator();
        if (iter.hasNext()) {
            write(iter.next(), writer);
            writer.append(' ');
            while (iter.hasNext()) {
                writer.append(',').append(' ');
                write(iter.next(), writer);
                writer.append(' ');
            }
        }
        writer.append('}');
    }

    protected void write( Iterable<?> arrayValue,
                          Writer writer ) throws IOException {
        writer.append('[');
        Iterator<?> iter = arrayValue.iterator();
        if (iter.hasNext()) {
            writer.append(' ');
            write(iter.next(), writer);
            while (iter.hasNext()) {
                writer.append(' ').append(',').append(' ');
                write(iter.next(), writer);
            }
        }
        writer.append(' ').append(']');
    }

    protected void writeArray( Object array,
                               Writer writer ) throws IOException {
        // Could transform this into a List, but this is more efficient ...
        writer.append('[');
        for (int i = 0, len = Array.getLength(array); i < len; i++) {
            if (i > 0) {
                writer.append(' ').append(',').append(' ');
            }
            write(Array.get(array, i), writer);
        }
        writer.append(' ').append(']');
    }

    protected static final class StringBuilderWriter extends Writer {

        private final StringBuilder builder;

        public StringBuilderWriter( final StringBuilder builder ) {
            this.builder = builder;
        }

        @Override
        public void write( final char[] cbuf,
                           final int off,
                           final int len ) {
            builder.append(cbuf, off, len);
        }

        @Override
        public Writer append( char c ) {
            builder.append(c);
            return this;
        }

        @Override
        public Writer append( CharSequence csq ) {
            builder.append(csq);
            return this;
        }

        @Override
        public Writer append( CharSequence csq,
                              int start,
                              int end ) {
            builder.append(csq, start, end);
            return this;
        }

        @Override
        public void write( final int c ) {
            builder.append(c);
        }

        @Override
        public void write( final char[] cbuf ) {
            builder.append(cbuf);
        }

        @Override
        public void write( final String str ) {
            builder.append(str);
        }

        @Override
        public void write( final String str,
                           final int off,
                           final int len ) {
            builder.append(str, off, len);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }

}
