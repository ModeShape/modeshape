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

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import org.infinispan.schematic.document.Array;
import org.infinispan.schematic.document.Bson;
import org.infinispan.schematic.document.Bson.BinaryType;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.MaxKey;
import org.infinispan.schematic.document.MinKey;
import org.infinispan.schematic.document.ThreadSafe;
import org.infinispan.schematic.internal.io.BsonDataInput;

/**
 * A component that reads BSON representations and constructs the in-memory {@link Document} representation.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 */
@ThreadSafe
public class BsonReader {

    protected static final DocumentValueFactory VALUE_FACTORY = new DefaultDocumentValueFactory();

    /**
     * Read the binary BSON representation from supplied input stream and construct the {@link Document} representation.
     * 
     * @param stream the input stream; may not be null
     * @return the in-memory {@link Document} representation
     * @throws IOException if there was a problem reading from the stream
     */
    public Document read( InputStream stream ) throws IOException {
        // Create an object so that this reader is thread safe ...
        DocumentValueFactory valueFactory = VALUE_FACTORY;
        Reader reader = new Reader(new BsonDataInput(new DataInputStream(stream)), valueFactory);
        // try {
        reader.startDocument();
        // } catch (IOException e) {
        // Json.writePretty(reader.endDocument(), System.err);
        // throw e;
        // } catch (RuntimeException e) {
        // Json.writePretty(reader.endDocument(), System.err);
        // throw e;
        // }
        return reader.endDocument();
    }

    /**
     * Read the binary BSON representation from supplied input stream and construct the {@link Document} representation.
     * 
     * @param input the input stream; may not be null
     * @return the in-memory {@link Document} representation
     * @throws IOException if there was a problem reading from the stream
     */
    public Document read( DataInput input ) throws IOException {
        // Create an object so that this reader is thread safe ...
        DocumentValueFactory valueFactory = VALUE_FACTORY;
        Reader reader = new Reader(new BsonDataInput(input), valueFactory);
        reader.startDocument();
        return reader.endDocument();
    }

    /**
     * Read the binary BSON representation from supplied input stream and construct the {@link Array} representation.
     * 
     * @param stream the input stream; may not be null
     * @return the in-memory {@link Document} representation
     * @throws IOException if there was a problem reading from the stream
     */
    public Array readArray( InputStream stream ) throws IOException {
        // Create an object so that this reader is thread safe ...
        DocumentValueFactory valueFactory = VALUE_FACTORY;
        Reader reader = new Reader(new BsonDataInput(new DataInputStream(stream)), valueFactory);
        // try {
        reader.startArray();
        // } catch (IOException e) {
        // Json.writePretty(reader.endDocument(), System.err);
        // throw e;
        // } catch (RuntimeException e) {
        // Json.writePretty(reader.endDocument(), System.err);
        // throw e;
        // }
        return (Array)reader.endDocument();
    }

    /**
     * Read the binary BSON representation from supplied input stream and construct the {@link Document} representation.
     * 
     * @param input the input stream; may not be null
     * @return the in-memory {@link Document} representation
     * @throws IOException if there was a problem reading from the stream
     */
    public Array readArray( DataInput input ) throws IOException {
        // Create an object so that this reader is thread safe ...
        DocumentValueFactory valueFactory = VALUE_FACTORY;
        Reader reader = new Reader(new BsonDataInput(input), valueFactory);
        reader.startArray();
        return (Array)reader.endDocument();
    }

    protected static class Reader {
        private final BsonDataInput data;
        private MutableDocument object;
        private DocumentValueFactory values;

        // private final BsonEditor editor;

        protected Reader( BsonDataInput data,
                          DocumentValueFactory valueFactory ) {
            this.data = data;
            this.values = valueFactory;
        }

        protected void startDocument() throws IOException {
            object = readDocument(false);
        }

        protected void startArray() throws IOException {
            object = readDocument(true);
        }

        protected MutableDocument readDocument( boolean array ) throws IOException {
            // Read the size int32, but we don't care about the value 'cuz it's in bytes ...
            int length = data.readInt();
            int startingIndex = data.getTotalBytesRead();
            int endingIndex = startingIndex + length;
            MutableDocument doc = array ? new BasicArray() : new BasicDocument();
            // Read the elements ...
            while (data.getTotalBytesRead() < endingIndex) {
                byte type = data.readByte();
                if (type == Bson.END_OF_DOCUMENT) break;
                readElement(type, doc);
            }
            return doc;
        }

        protected void readElement( byte type,
                                    MutableDocument bson ) throws IOException {
            String name = readCString();
            Object value = null;
            switch (type) {
                case Bson.Type.ARRAY:
                    value = readDocument(true);
                    break;
                case Bson.Type.BINARY:
                    int length = data.readInt();
                    byte subtype = data.readByte();
                    if (subtype == BinaryType.UUID) {
                        long mostSig = data.readLong();
                        long leastSig = data.readLong();
                        value = new UUID(mostSig, leastSig);
                    } else {
                        byte[] bytes = new byte[length];
                        data.readFully(bytes);
                        value = values.createBinary(subtype, bytes);
                    }
                    break;
                case Bson.Type.BOOLEAN:
                    value = values.createBoolean(data.readBoolean());
                    break;
                case Bson.Type.DATETIME:
                    value = values.createDate(data.readLong());
                    break;
                case Bson.Type.DBPOINTER:
                    // Deprecated, so ignore ...
                    break;
                case Bson.Type.DOCUMENT:
                    value = readDocument(false);
                    break;
                case Bson.Type.DOUBLE:
                    value = values.createDouble(data.readDouble());
                    break;
                case Bson.Type.INT32:
                    value = values.createInt(data.readInt());
                    break;
                case Bson.Type.INT64:
                    value = values.createLong(data.readLong());
                    break;
                case Bson.Type.JAVASCRIPT:
                    value = values.createCode(readString());
                    break;
                case Bson.Type.JAVASCRIPT_WITH_SCOPE:
                    data.readInt(); // the length, but we don't use this
                    String code = readString();
                    Document scope = readDocument(false);
                    value = values.createCode(code, scope);
                    break;
                case Bson.Type.MAXKEY:
                    value = MaxKey.getInstance();
                    break;
                case Bson.Type.MINKEY:
                    value = MinKey.getInstance();
                    break;
                case Bson.Type.NULL:
                    value = values.createNull();
                    break;
                case Bson.Type.OBJECTID:
                    byte[] objectIdBytes = new byte[12];
                    data.readFully(objectIdBytes);
                    value = values.createObjectId(objectIdBytes);
                    break;
                case Bson.Type.REGEX:
                    value = values.createRegex(readCString(), readCString());
                    break;
                case Bson.Type.STRING:
                    value = readString();
                    break;
                case Bson.Type.SYMBOL:
                    value = readString();
                    break;
                case Bson.Type.TIMESTAMP:
                    int inc = data.readInt();
                    int time = data.readInt();
                    value = values.createTimestamp(time, inc);
                    break;
                case Bson.Type.UNDEFINED:
                    // ignore ...
                    break;
            }
            bson.put(name, value);
        }

        protected String readCString() throws IOException {
            return data.readUTF(-1); // this reads the zero-byte terminator
        }

        protected String readString() throws IOException {
            int length = data.readInt();
            String result = values.createString(data.readUTF(length - 1)); // don't read the zero-byte
            data.readByte(); // reads the zero-byte terminator
            return result;
        }

        protected Document endDocument() {
            return object;
        }

    }

}
