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
package org.jboss.dna.connector.store.jpa.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.UUID;
import org.jboss.dna.common.SystemFailureException;
import org.jboss.dna.common.util.SecureHash;
import org.jboss.dna.connector.store.jpa.models.basic.LargeValueEntity;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.properties.Binary;
import org.jboss.dna.graph.properties.DateTime;
import org.jboss.dna.graph.properties.Name;
import org.jboss.dna.graph.properties.Path;
import org.jboss.dna.graph.properties.Property;
import org.jboss.dna.graph.properties.PropertyFactory;
import org.jboss.dna.graph.properties.PropertyType;
import org.jboss.dna.graph.properties.Reference;
import org.jboss.dna.graph.properties.ValueFactories;

/**
 * @author Randall Hauch
 */
public class Serializer {

    private final PropertyFactory propertyFactory;
    private final ValueFactories valueFactories;
    private final LargeValues largeValues;

    public Serializer( ExecutionContext context,
                       LargeValues largeValues ) {
        this.propertyFactory = context.getPropertyFactory();
        this.valueFactories = context.getValueFactories();
        this.largeValues = largeValues;
    }

    /**
     * Interface that represents the location where "large" objects are stored.
     * 
     * @author Randall Hauch
     */
    public interface LargeValues {
        /**
         * Get the minimum size for large values, specified as {@link String#length() number of characters} for a {@link String}
         * or the {@link Binary#getSize() number of bytes for a binary value}
         * 
         * @return the size at which a property value is considered to be <i>large</i>
         */
        long getMinimumSize();

        void write( byte[] hash,
                    long length,
                    PropertyType type,
                    Object value ) throws IOException;

        Object read( ValueFactories valueFactories,
                     byte[] hash,
                     long length ) throws IOException;
    }

    /**
     * Serialize the properties' values to the object stream.
     * <p>
     * If any of the property values are considered {@link LargeValues#getMinimumSize() large}, the value's hash and length of the
     * property value will be written to the object stream, but the property value will be sent to the supplied
     * {@link LargeValueEntity} object.
     * </p>
     * <p>
     * This method does not automatically write each property value to the stream using
     * {@link ObjectOutputStream#writeObject(Object)}, but instead serializes the primitive values that make up the property value
     * object with a code that describes the {@link PropertyType property's type}. This is more efficient, since most of the
     * property values are really non-primitive objects, and writing to the stream using
     * {@link ObjectOutputStream#writeObject(Object)} would include larger class metadata.
     * </p>
     * 
     * @param stream the stream where the properties' values are to be serialized; may not be null
     * @param number the number of properties exposed by the supplied <code>properties</code> iterator; must be 0 or positive
     * @param properties the iterator over the properties that are to be serialized; may not be null
     * @throws IOException if there is an error writing to the <code>stream</code> or <code>largeValues</code>
     * @see #deserializeProperties(ObjectInputStream, Collection)
     * @see #serializeProperty(ObjectOutputStream, Property)
     */
    public void serializeProperties( ObjectOutputStream stream,
                                     int number,
                                     Iterable<Property> properties ) throws IOException {
        assert number >= 0;
        assert properties != null;
        stream.writeInt(number);
        for (Property property : properties) {
            if (property == null) continue;
            serializeProperty(stream, property);
        }
    }

    /**
     * Serialize the property's values to the object stream.
     * <p>
     * If any of the property values are considered {@link LargeValues#getMinimumSize() large}, the value's hash and length of the
     * property value will be written to the object stream, but the property value will be sent to the supplied
     * {@link LargeValueEntity} object.
     * </p>
     * <p>
     * This method does not automatically write each property value to the stream using
     * {@link ObjectOutputStream#writeObject(Object)}, but instead serializes the primitive values that make up the property value
     * object with a code that describes the {@link PropertyType property's type}. This is more efficient, since most of the
     * property values are really non-primitive objects, and writing to the stream using
     * {@link ObjectOutputStream#writeObject(Object)} would include larger class metadata.
     * </p>
     * 
     * @param stream the stream where the property's values are to be serialized; may not be null
     * @param property the property to be serialized; may not be null
     * @throws IOException if there is an error writing to the <code>stream</code> or <code>largeValues</code>
     * @see #serializeProperties(ObjectOutputStream, int, Iterable)
     * @see #deserializeProperty(ObjectInputStream)
     */
    public void serializeProperty( ObjectOutputStream stream,
                                   Property property ) throws IOException {
        assert stream != null;
        assert property != null;
        // Write the name ...
        stream.writeObject(property.getName().getString());
        // Write the number of values ...
        stream.writeInt(property.size());
        for (Object value : property) {
            if (value instanceof String) {
                String stringValue = (String)value;
                if (largeValues != null && stringValue.length() > largeValues.getMinimumSize()) {
                    // Store the value in the large values area, but record the hash and length here.
                    byte[] hash = computeHash(stringValue);
                    stream.writeChar('L');
                    stream.writeInt(hash.length);
                    stream.write(hash);
                    stream.writeLong(stringValue.length());
                    // Now write to the large objects ...
                    largeValues.write(computeHash(stringValue), stringValue.length(), PropertyType.STRING, stringValue);
                } else {
                    stream.writeChar('S');
                    stream.writeObject(stringValue);
                }
            } else if (value instanceof Boolean) {
                stream.writeChar('b');
                stream.writeBoolean(((Boolean)value).booleanValue());
            } else if (value instanceof Long) {
                stream.writeChar('l');
                stream.writeLong(((Long)value).longValue());
            } else if (value instanceof Double) {
                stream.writeChar('d');
                stream.writeDouble(((Double)value).doubleValue());
            } else if (value instanceof Integer) {
                stream.writeChar('i');
                stream.writeInt(((Integer)value).intValue());
            } else if (value instanceof Short) {
                stream.writeChar('s');
                stream.writeShort(((Short)value).shortValue());
            } else if (value instanceof Float) {
                stream.writeChar('f');
                stream.writeFloat(((Float)value).floatValue());
            } else if (value instanceof UUID) {
                stream.writeChar('U');
                UUID uuid = (UUID)value;
                stream.writeLong(uuid.getMostSignificantBits());
                stream.writeLong(uuid.getLeastSignificantBits());
            } else if (value instanceof URI) {
                stream.writeChar('I');
                stream.writeObject(((URI)value).toString());
            } else if (value instanceof Name) {
                stream.writeChar('N');
                stream.writeObject(((Name)value).getString());
            } else if (value instanceof Path) {
                stream.writeChar('P');
                stream.writeObject(((Path)value).getString());
            } else if (value instanceof DateTime) {
                stream.writeChar('T');
                stream.writeObject(((DateTime)value).getString());
            } else if (value instanceof BigDecimal) {
                stream.writeChar('D');
                stream.writeObject(value);
            } else if (value instanceof Character) {
                stream.writeChar('c');
                char c = ((Character)value).charValue();
                stream.writeChar(c);
            } else if (value instanceof Reference) {
                stream.writeChar('R');
                stream.writeObject(((Reference)value).getString());
            } else if (value instanceof Binary) {
                Binary binary = (Binary)value;
                byte[] hash = null;
                long length = 0;
                try {
                    binary.acquire();
                    length = binary.getSize();
                    if (largeValues != null && length > largeValues.getMinimumSize()) {
                        // Store the value in the large values area, but record the hash and length here.
                        hash = binary.getHash();
                        stream.writeChar('L');
                        stream.writeInt(hash.length);
                        stream.write(hash);
                        stream.writeLong(length);
                        // Write to large objects after releasing the binary
                    } else {
                        // The value is small enough to store here ...
                        stream.writeChar('B');
                        stream.writeLong(length);
                        InputStream data = binary.getStream();
                        try {
                            byte[] buffer = new byte[1024];
                            int numRead = 0;
                            while ((numRead = data.read(buffer)) > -1) {
                                stream.write(buffer, 0, numRead);
                            }
                        } finally {
                            data.close();
                        }
                    }
                } finally {
                    binary.release();
                }
                // If this is a large value and the binary has been released, write it to the large objects ...
                if (largeValues != null && hash != null) {
                    largeValues.write(hash, length, PropertyType.BINARY, value);
                }
            } else {
                // Other kinds of values ...
                stream.writeChar('O');
                stream.writeObject(value);
            }
        }
        stream.flush();
    }

    /**
     * Deserialize the serialized properties on the supplied object stream.
     * 
     * @param stream the stream that contains the serialized properties; may not be null
     * @param properties the collection into which each deserialized property is to be placed; may not be null
     * @throws IOException if there is an error writing to the <code>stream</code> or <code>largeValues</code>
     * @throws ClassNotFoundException if the class for the value's object could not be found
     * @see #deserializeProperty(ObjectInputStream)
     * @see #serializeProperties(ObjectOutputStream, int, Iterable)
     */
    public void deserializeProperties( ObjectInputStream stream,
                                       Collection<Property> properties ) throws IOException, ClassNotFoundException {
        assert propertyFactory != null;
        assert valueFactories != null;
        assert stream != null;
        assert properties != null;
        assert largeValues != null;
        // Read the number of properties ...
        int count = stream.readInt();
        for (int i = 0; i != count; ++i) {
            Property property = deserializeProperty(stream);
            assert property != null;
            properties.add(property);
        }
    }

    /**
     * Deserialize the serialized property on the supplied object stream.
     * 
     * @param stream the stream that contains the serialized properties; may not be null
     * @return the deserialized property; never null
     * @throws IOException if there is an error writing to the <code>stream</code> or <code>largeValues</code>
     * @throws ClassNotFoundException if the class for the value's object could not be found
     * @see #deserializeProperties(ObjectInputStream, Collection)
     * @see #serializeProperty(ObjectOutputStream, Property)
     */
    public Property deserializeProperty( ObjectInputStream stream ) throws IOException, ClassNotFoundException {
        assert propertyFactory != null;
        assert valueFactories != null;
        assert stream != null;
        assert largeValues != null;
        // Read the name ...
        String nameStr = (String)stream.readObject();
        Name name = valueFactories.getNameFactory().create(nameStr);
        assert name != null;
        // Read the number of values ...
        int size = stream.readInt();
        Object[] values = new Object[size];
        for (int i = 0; i != size; ++i) {
            Object value = null;
            // Read the type of value ...
            char type = stream.readChar();
            switch (type) {
                case 'S':
                    // String
                    value = valueFactories.getStringFactory().create((String)stream.readObject());
                    break;
                case 'b':
                    // boolean
                    value = valueFactories.getBooleanFactory().create(stream.readBoolean());
                    break;
                case 'i':
                    // integer
                    value = valueFactories.getLongFactory().create(stream.readInt());
                    break;
                case 'l':
                    // long
                    value = valueFactories.getLongFactory().create(stream.readLong());
                    break;
                case 's':
                    // short
                    value = valueFactories.getLongFactory().create(stream.readShort());
                    break;
                case 'f':
                    // float
                    value = valueFactories.getDoubleFactory().create(stream.readFloat());
                    break;
                case 'd':
                    // double
                    value = valueFactories.getDoubleFactory().create(stream.readDouble());
                    break;
                case 'c':
                    // double
                    value = valueFactories.getStringFactory().create("" + stream.readChar());
                    break;
                case 'U':
                    // UUID
                    long msb = stream.readLong();
                    long lsb = stream.readLong();
                    UUID uuid = new UUID(msb, lsb);
                    value = valueFactories.getUuidFactory().create(uuid);
                    break;
                case 'I':
                    // URI
                    String uriStr = (String)stream.readObject();
                    value = valueFactories.getUriFactory().create(uriStr);
                    break;
                case 'N':
                    // Name
                    String nameValueStr = (String)stream.readObject();
                    value = valueFactories.getNameFactory().create(nameValueStr);
                    break;
                case 'P':
                    // Path
                    String pathStr = (String)stream.readObject();
                    value = valueFactories.getPathFactory().create(pathStr);
                    break;
                case 'T':
                    // DateTime
                    String dateTimeStr = (String)stream.readObject();
                    value = valueFactories.getDateFactory().create(dateTimeStr);
                    break;
                case 'D':
                    // BigDecimal
                    Object bigDecimal = stream.readObject();
                    value = valueFactories.getDecimalFactory().create(bigDecimal);
                    break;
                case 'R':
                    // Reference
                    value = valueFactories.getReferenceFactory().create((String)stream.readObject());
                    break;
                case 'B':
                    // Binary
                    // Read the length of the content ...
                    long binaryLength = stream.readLong();
                    value = valueFactories.getBinaryFactory().create(stream, binaryLength);
                    break;
                case 'L':
                    // Large object ...
                    // Read the hash ...
                    int hashLength = stream.readInt();
                    byte[] hash = new byte[hashLength];
                    stream.read(hash);
                    // Read the length of the content ...
                    long length = stream.readLong();
                    value = largeValues.read(valueFactories, hash, length);
                    break;
                default:
                    // All other objects ...
                    Object object = stream.readObject();
                    value = valueFactories.getObjectFactory().create(object);
                    break;
            }
            assert value != null;
            values[i] = value;
        }
        // Add the property to the collection ...
        return propertyFactory.create(name, values);
    }

    public byte[] computeHash( String value ) {
        try {
            return SecureHash.getHash(SecureHash.Algorithm.SHA_1, value.getBytes());
        } catch (NoSuchAlgorithmException e) {
            throw new SystemFailureException(e);
        }
    }

}
