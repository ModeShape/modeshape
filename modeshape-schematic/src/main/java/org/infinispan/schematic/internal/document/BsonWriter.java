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

import static org.infinispan.schematic.document.Bson.END_OF_DOCUMENT;
import static org.infinispan.schematic.document.Bson.END_OF_STRING;
import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.Date;
import java.util.Iterator;
import java.util.UUID;
import java.util.regex.Pattern;
import org.infinispan.schematic.document.Binary;
import org.infinispan.schematic.document.Bson.BinaryType;
import org.infinispan.schematic.document.Bson.Type;
import org.infinispan.schematic.document.Code;
import org.infinispan.schematic.document.CodeWithScope;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.Document.Field;
import org.infinispan.schematic.document.MaxKey;
import org.infinispan.schematic.document.MinKey;
import org.infinispan.schematic.document.ObjectId;
import org.infinispan.schematic.document.Symbol;
import org.infinispan.schematic.document.ThreadSafe;
import org.infinispan.schematic.document.Timestamp;
import org.infinispan.schematic.internal.io.BsonDataOutput;

/**
 * A component that writes BSON representations from the in-memory {@link Document} representation.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 */
@ThreadSafe
public class BsonWriter {

    /**
     * Write the supplied in-memory {@link Document} in standard BSON binary format to the supplied stream.
     * 
     * @param object the BSON object or BSON value; may not be null
     * @param stream the output stream; may not be null
     * @throws IOException if there was a problem reading from the stream
     */
    public void write( Object object,
                       OutputStream stream ) throws IOException {
        BsonDataOutput buffer = new BsonDataOutput();
        write(null, object, buffer);
        buffer.writeTo(stream);
    }

    /**
     * Return the array of bytes containing the standard BSON binary form of the supplied in-memory {@link Document}.
     * 
     * @param object the BSON object or BSON value; may not be null
     * @return the bytes
     * @throws IOException if there was a problem reading from the stream
     */
    public byte[] write( Object object ) throws IOException {
        BsonDataOutput buffer = new BsonDataOutput();
        write(null, object, buffer);
        ByteArrayOutputStream stream = new ByteArrayOutputStream(buffer.size());
        buffer.writeTo(stream);
        return stream.toByteArray();
    }

    /**
     * Write to the supplied output the binary BSON representation of the supplied in-memory {@link Document}.
     * 
     * @param object the BSON object or BSON value; may not be null
     * @param output the output; may not be null
     * @throws IOException if there was a problem writing to the ObjectOutput
     */
    public void write( Object object,
                       DataOutput output ) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        write(object, stream);
        stream.flush();
        byte[] bytes = stream.toByteArray();
        output.write(bytes);
    }

    protected void write( String name,
                          Object object,
                          BsonDataOutput output ) {
        if (object == null) {
            writeNull(name, output);
        } else if (object instanceof String) {
            write(name, (String)object, output);
        } else if (object instanceof Boolean) {
            write(name, ((Boolean)object).booleanValue(), output);
        } else if (object instanceof Integer) {
            write(name, ((Integer)object).intValue(), output);
        } else if (object instanceof Long) {
            write(name, ((Long)object).longValue(), output);
        } else if (object instanceof Float) {
            write(name, ((Float)object).floatValue(), output);
        } else if (object instanceof Double) {
            write(name, ((Double)object).doubleValue(), output);
        } else if (object.getClass().isArray()) {
            writeArray(name, object, output);
        } else if (object instanceof ArrayEditor) {
            write(name, (Iterable<?>)((ArrayEditor)object).unwrap(), output);
        } else if (object instanceof DocumentEditor) {
            write(name, ((DocumentEditor)object).unwrap(), output);
        } else if (object instanceof Iterable) { // must check before 'BsonObject' because of inheritance
            write(name, (Iterable<?>)object, output);
        } else if (object instanceof Document) {
            write(name, (Document)object, output);
        } else if (object instanceof Binary) {
            write(name, (Binary)object, output);
        } else if (object instanceof Symbol) {
            write(name, (Symbol)object, output);
        } else if (object instanceof Pattern) {
            write(name, (Pattern)object, output);
        } else if (object instanceof Date) {
            write(name, (Date)object, output);
        } else if (object instanceof UUID) {
            write(name, (UUID)object, output);
        } else if (object instanceof CodeWithScope) { // must check before 'Code' because of inheritance
            write(name, (CodeWithScope)object, output);
        } else if (object instanceof Code) {
            write(name, (Code)object, output);
        } else if (object instanceof Timestamp) {
            write(name, (Timestamp)object, output);
        } else if (object instanceof ObjectId) {
            write(name, (ObjectId)object, output);
        } else if (object instanceof MaxKey) {
            write(name, (MaxKey)object, output);
        } else if (object instanceof MinKey) {
            write(name, (MinKey)object, output);
        } else {
            throw new RuntimeException("Unable to serialize type \'" + object.getClass() + "\" to BSON");
        }
    }

    protected void writeCString( String value,
                                 BsonDataOutput output ) {
        output.writeUTFString(value);
        output.writeByte(END_OF_STRING);
    }

    protected void writeString( String value,
                                BsonDataOutput output ) {
        // Write out the size of the string; use '0' now and rewrite it ...
        int position = output.size();
        output.writeInt(0);
        int len = output.writeUTFString(value);
        output.writeByte(END_OF_STRING);
        // Now write the length ...
        assert (len + 1) >= 0;
        output.writeInt(position, len + 1);// +1 for the zero-byte
    }

    protected void writeNull( String name,
                              BsonDataOutput output ) {
        output.writeByte(Type.NULL);
        writeCString(name, output);
    }

    protected void write( String name,
                          String value,
                          BsonDataOutput output ) {
        output.writeByte(Type.STRING);
        writeCString(name, output);
        writeString(value, output);
    }

    protected void write( String name,
                          boolean value,
                          BsonDataOutput output ) {
        output.writeByte(Type.BOOLEAN);
        writeCString(name, output);
        output.writeByte(value ? (byte)0x01 : (byte)0x00);
    }

    protected void write( String name,
                          int value,
                          BsonDataOutput output ) {
        output.writeByte(Type.INT32);
        writeCString(name, output);
        output.writeInt(value);
    }

    protected void write( String name,
                          long value,
                          BsonDataOutput output ) {
        output.writeByte(Type.INT64);
        writeCString(name, output);
        output.writeLong(value);
    }

    protected void write( String name,
                          float value,
                          BsonDataOutput output ) {
        output.writeByte(Type.DOUBLE);
        writeCString(name, output);
        output.writeDouble(value);
    }

    protected void write( String name,
                          double value,
                          BsonDataOutput output ) {
        output.writeByte(Type.DOUBLE);
        writeCString(name, output);
        output.writeDouble(value);
    }

    protected void writeArray( String name,
                               Object arrayValue,
                               BsonDataOutput output ) {
        if (name != null) {
            output.writeByte(Type.ARRAY);
            writeCString(name, output);
        }
        // Write the size for the array; we'll come back to this after we write the array ...
        int arraySizePosition = output.size();
        output.writeInt(0);

        // Now write out the array as a document ...
        int length = Array.getLength(arrayValue);
        Iterator<String> indexIter = IndexSequence.infiniteSequence();
        for (int i = 0; i != length; ++i) {
            String elementName = indexIter.next(); // the strings are shared/reused
            Object value = Array.get(arrayValue, i);
            write(elementName, value, output);
        }
        output.writeByte(END_OF_DOCUMENT);

        // Determine the number of bytes written in the array, and overwrite the value we wrote earlier ..
        int arraySize = output.size() - arraySizePosition;
        output.writeInt(arraySizePosition, arraySize);
    }

    protected void write( String name,
                          Iterable<?> arrayValue,
                          BsonDataOutput output ) {
        if (name != null) {
            output.writeByte(Type.ARRAY);
            writeCString(name, output);
        }
        // Write the size for the array; we'll come back to this after we write the array ...
        int arraySizePosition = output.size();
        output.writeInt(0);
        // Now write out the array as a document ...
        Iterator<String> indexIter = IndexSequence.infiniteSequence();
        Iterator<?> valueIter = arrayValue.iterator();
        while (valueIter.hasNext()) {
            String elementName = indexIter.next(); // the strings are shared/reused
            Object value = valueIter.next();
            write(elementName, value, output);
        }
        output.writeByte(END_OF_DOCUMENT);

        // Determine the number of bytes written in the array, and overwrite the value we wrote earlier ..
        int arraySize = output.size() - arraySizePosition;
        output.writeInt(arraySizePosition, arraySize);
    }

    protected void write( String name,
                          Document document,
                          BsonDataOutput output ) {
        if (name != null) {
            output.writeByte(Type.DOCUMENT);
            writeCString(name, output);
        }
        // Write the size for the document; we'll come back to this after we write the array ...
        int arraySizePosition = output.size();
        output.writeInt(-1);

        for (Field field : document.fields()) {
            write(field.getName(), field.getValue(), output);
        }
        output.writeByte(END_OF_DOCUMENT);

        // Determine the number of bytes written in the array, and overwrite the value we wrote earlier ..
        int arraySize = output.size() - arraySizePosition;
        output.writeInt(arraySizePosition, arraySize);
    }

    protected void write( String name,
                          Binary value,
                          BsonDataOutput output ) {
        output.writeByte(Type.BINARY);
        writeCString(name, output);
        byte[] bytes = value.getBytes();
        output.writeInt(bytes.length);
        output.writeByte(value.getType());
        output.write(bytes);
    }

    protected void write( String name,
                          Symbol value,
                          BsonDataOutput output ) {
        output.writeByte(Type.SYMBOL);
        writeCString(name, output);
        writeString(value.getSymbol(), output);
    }

    protected void write( String name,
                          Pattern value,
                          BsonDataOutput output ) {
        output.writeByte(Type.REGEX);
        writeCString(name, output);
        writeCString(value.pattern(), output);
        writeCString(BsonUtils.regexFlagsFor(value), output);
    }

    protected void write( String name,
                          Date value,
                          BsonDataOutput output ) {
        output.writeByte(Type.DATETIME);
        writeCString(name, output);
        output.writeLong(value.getTime());
    }

    protected void write( String name,
                          UUID value,
                          BsonDataOutput output ) {
        output.writeByte(Type.BINARY);
        writeCString(name, output);
        output.writeInt(16);
        output.writeByte(BinaryType.UUID);
        output.writeLong(value.getMostSignificantBits());
        output.writeLong(value.getLeastSignificantBits());
    }

    protected void write( String name,
                          CodeWithScope value,
                          BsonDataOutput output ) {
        output.writeByte(Type.JAVASCRIPT_WITH_SCOPE);
        writeCString(name, output);
        // Write the size for the CodeWithScope; we'll come back to this after we write the object ...
        int arraySizePosition = output.size();
        output.writeInt(0);

        // Write the code & scope ...
        writeString(value.getCode(), output);
        write(null, value.getScope(), output);

        // Determine the number of bytes written in the array, and overwrite the value we wrote earlier ..
        int arraySize = output.size() - arraySizePosition;
        output.writeInt(arraySizePosition, arraySize);
    }

    protected void write( String name,
                          Code value,
                          BsonDataOutput output ) {
        output.writeByte(Type.JAVASCRIPT);
        writeCString(name, output);
        writeString(value.getCode(), output);
    }

    protected void write( String name,
                          Timestamp value,
                          BsonDataOutput output ) {
        output.writeByte(Type.TIMESTAMP);
        writeCString(name, output);
        output.writeInt(value.getInc());
        output.writeInt(value.getTime());
    }

    protected void write( String name,
                          ObjectId value,
                          BsonDataOutput output ) {
        output.writeByte(Type.OBJECTID);
        writeCString(name, output);
        output.write(value.getBytes());
    }

    protected void write( String name,
                          MaxKey value,
                          BsonDataOutput output ) {
        output.writeByte(Type.MAXKEY);
        writeCString(name, output);
    }

    protected void write( String name,
                          MinKey value,
                          BsonDataOutput output ) {
        output.writeByte(Type.MINKEY);
        writeCString(name, output);
    }
}
