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
package org.infinispan.schematic.internal.io;

import java.io.DataOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.List;

/**
 * An implementation of {@link DataOutput} with additional methods needed for writing BSON formatted content. Specifically, this
 * class reads little-endian byte order (purposefully in violation of the DataInput interface specification) and provides a way to
 * read C-style strings (where the length is not known up front but instead contain all characters until the zero-byte
 * terminator), which are commonly used within the BSON specification.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 * @since 5.1
 */
public class BsonDataOutput implements DataOutput {

    private static final int DEFAULT_BUFFER_SIZE = 1048 * 8;

    private final List<ByteBuffer> buffers = new ArrayList<ByteBuffer>();
    private final int bufferSize;
    private final byte[] bytes = new byte[8];
    private int position = 0;
    private int size = 0;
    private ByteBuffer remainderBuffer;

    public BsonDataOutput() {
        this.bufferSize = DEFAULT_BUFFER_SIZE;
    }

    public BsonDataOutput( int initialSize ) {
        if (initialSize <= 0) {
            throw new IllegalArgumentException("Initial size must be larger than 0");
        }
        this.bufferSize = initialSize;
    }

    protected ByteBuffer getBufferFor( int position ) {
        int bufferIndex = position / bufferSize;
        while (bufferIndex >= buffers.size()) {
            buffers.add(newByteBuffer(bufferSize));
        }
        return buffers.get(bufferIndex);
    }

    protected ByteBuffer newByteBuffer( int bufferSize ) {
        return ByteBuffer.allocate(bufferSize).order(ByteOrder.LITTLE_ENDIAN);
    }

    protected void updateSize( int newSize ) {
        if (size < newSize) {
            size = newSize;
        }
    }

    public int size() {
        return size;
    }

    @Override
    public void writeByte( int value ) {
        writeByte(position, value);
        position += 1;
    }

    public void writeByte( int position,
                           int value ) {
        updateSize(position + 1);
        ByteBuffer buffer = getBufferFor(position);
        int index = position % bufferSize;
        buffer.put(index, (byte)value);
    }

    @Override
    public void write( byte[] b ) {
        write(position, b, 0, b.length);
        position += b.length;
    }

    @Override
    public void write( byte[] b,
                       int off,
                       int len ) {
        write(position, b, off, len);
        position += len;
    }

    public void write( int position,
                       byte[] value,
                       int offset,
                       int length ) {
        updateSize(position + length);
        ByteBuffer buffer = getBufferFor(position);
        int index = position % bufferSize;
        for (int i = offset; i != length; ++i) {
            byte b = value[i];
            if (index == bufferSize) {
                // We have to use the next buffer ...
                buffer = getBufferFor(position);
                index = position % bufferSize;
            }
            buffer.put(index, b);
            ++index;
            ++position;
        }
    }

    @Override
    public void write( int b ) {
        write((byte)b);
    }

    @Override
    public void writeBoolean( boolean value ) {
        writeBoolean(position, value);
        position += 1;
    }

    public void writeBoolean( int position,
                              boolean value ) {
        updateSize(position + 1);
        ByteBuffer buffer = getBufferFor(position);
        int index = position % bufferSize;
        buffer.put(index, value ? (byte)1 : (byte)0);
    }

    @Override
    public void writeChar( int value ) {
        writeChar(position, value);
        position += 2;
    }

    public void writeChar( int position,
                           int value ) {
        updateSize(position + 2);
        ByteBuffer buffer = getBufferFor(position);
        int index = position % bufferSize;
        if (buffer.limit() - index >= 2) {
            // There's enough room to write on the buffer ...
            buffer.putChar(index, (char)value);
        } else {
            // The value will have to span buffers ...
            bytes[0] = (byte)value;
            bytes[1] = (byte)(value >> 8);
            write(position, bytes, 0, 2);
        }
    }

    @Override
    public void writeInt( int value ) {
        writeInt(position, value);
        position += 4;
    }

    public void writeInt( int position,
                          int value ) {
        updateSize(position + 4);
        ByteBuffer buffer = getBufferFor(position);
        int index = position % bufferSize;
        if (buffer.limit() - index >= 4) {
            // There's enough room to write on the buffer ...
            buffer.putInt(index, value);
        } else {
            // The value will have to span buffers ...
            bytes[0] = (byte)value;
            bytes[1] = (byte)(value >> 8);
            bytes[2] = (byte)(value >> 16);
            bytes[3] = (byte)(value >> 24);
            write(position, bytes, 0, 4);
        }
    }

    @Override
    public void writeShort( int value ) {
        writeShort(position, value);
        position += 2;
    }

    public void writeShort( int position,
                            int value ) {
        updateSize(position + 2);
        ByteBuffer buffer = getBufferFor(position);
        int index = position % bufferSize;
        if (buffer.limit() - index >= 2) {
            // There's enough room to write on the buffer ...
            buffer.putShort(index, (short)value);
        } else {
            // The value will have to span buffers ...
            bytes[0] = (byte)value;
            bytes[1] = (byte)(value >> 8);
            write(position, bytes, 0, 2);
        }
    }

    @Override
    public void writeLong( long value ) {
        writeLong(position, value);
        position += 8;
    }

    public void writeLong( int position,
                           long value ) {
        updateSize(position + 8);
        ByteBuffer buffer = getBufferFor(position);
        int index = position % bufferSize;
        if (buffer.limit() - index >= 8) {
            // There's enough room to write on the buffer ...
            buffer.putLong(index, value);
        } else {
            // The value will have to span buffers ...
            bytes[0] = (byte)value;
            bytes[1] = (byte)(value >> 8);
            bytes[2] = (byte)(value >> 16);
            bytes[3] = (byte)(value >> 24);
            bytes[4] = (byte)(value >> 32);
            bytes[5] = (byte)(value >> 40);
            bytes[6] = (byte)(value >> 48);
            bytes[7] = (byte)(value >> 56);
            write(position, bytes, 0, 8);
        }
    }

    @Override
    public void writeFloat( float value ) {
        writeFloat(position, value);
        position += 4;
    }

    public void writeFloat( int position,
                            float value ) {
        writeInt(position, Float.floatToRawIntBits(value));
    }

    @Override
    public void writeDouble( double value ) {
        writeDouble(position, value);
        position += 8;
    }

    public void writeDouble( int position,
                             double value ) {
        writeLong(position, Double.doubleToRawLongBits(value));
    }

    /**
     * Writes a string to the output stream. For every character in the string <code>s</code>, taken in order, one byte is written
     * to the output stream. If <code>s</code> is <code>null</code>, a <code>NullPointerException</code> is thrown.
     * <p>
     * If <code>s.length</code> is zero, then no bytes are written. Otherwise, the character <code>s[0]</code> is written first,
     * then <code>s[1]</code>, and so on; the last character written is <code>s[s.length-1]</code>. For each character, one byte
     * is written, the low-order byte, in exactly the manner of the <code>writeByte</code> method . The high-order eight bits of
     * each character in the string are ignored.
     * 
     * @param str the string value to be written.
     * @deprecated The semantics of {@code writeBytes(String s)} are considered dangerous. Please use {@link #writeUTF(String s)},
     *             {@link #writeChars(String s)} or another write method instead.
     */
    @Deprecated
    @Override
    public void writeBytes( String str ) {
        CharacterIterator iter = new StringCharacterIterator(str);
        for (char c = iter.first(); c != CharacterIterator.DONE; c = iter.next()) {
            writeByte(c);
        }
    }

    /**
     * Writes every character in the string <code>s</code>, to the output stream, in order, two bytes per character. If
     * <code>s</code> is <code>null</code>, a <code>NullPointerException</code> is thrown. If <code>s.length</code> is zero, then
     * no characters are written. Otherwise, the character <code>s[0]</code> is written first, then <code>s[1]</code>, and so on;
     * the last character written is <code>s[s.length-1]</code>. For each character, two bytes are actually written, low-order
     * byte first, in exactly the manner of the <code>writeChar</code> method.
     * 
     * @param str the string value to be written.
     */
    @Override
    public void writeChars( String str ) {
        CharacterIterator iter = new StringCharacterIterator(str);
        for (char c = iter.first(); c != CharacterIterator.DONE; c = iter.next()) {
            writeChar(c);
        }
    }

    @Override
    public void writeUTF( String str ) {
        int numBytesWritten = 0;
        int numBytesPosition = this.position;
        // Write a placeholder for the length ...
        writeShort(numBytesPosition, numBytesWritten);
        numBytesWritten = writeUTF(position, str);
        position += numBytesWritten;
        // Now write the real number of bytes written ...
        writeShort(numBytesPosition, numBytesWritten);
    }

    /**
     * Writes the <a href="DataInput.html#modified-utf-8">modified UTF-8</a> representation of every character in the string
     * <code>s</code>. <i>This is similar to the standard {@link #writeUTF(String)} but without the leading two byte length.</i>
     * If <code>s</code> is <code>null</code>, a <code>NullPointerException</code> is thrown. Each character in the string
     * <code>s</code> is converted to a group of one, two, or three bytes, depending on the value of the character.
     * <p>
     * If a character <code>c</code> is in the range <code>&#92;u0001</code> through <code>&#92;u007f</code>, it is represented by
     * one byte:
     * <p>
     * 
     * <pre>
     * (byte)c
     * </pre>
     * <p>
     * If a character <code>c</code> is <code>&#92;u0000</code> or is in the range <code>&#92;u0080</code> through
     * <code>&#92;u07ff</code>, then it is represented by two bytes, to be written in the order shown:
     * <p>
     * 
     * <pre>
     * <code>
     * (byte)(0xc0 | (0x1f &amp; (c &gt;&gt; 6)))
     * (byte)(0x80 | (0x3f &amp; c))
     *  </code>
     * </pre>
     * <p>
     * If a character <code>c</code> is in the range <code>&#92;u0800</code> through <code>uffff</code>, then it is represented by
     * three bytes, to be written in the order shown:
     * <p>
     * 
     * <pre>
     * <code>
     * (byte)(0xe0 | (0x0f &amp; (c &gt;&gt; 12)))
     * (byte)(0x80 | (0x3f &amp; (c &gt;&gt;  6)))
     * (byte)(0x80 | (0x3f &amp; c))
     *  </code>
     * </pre>
     * <p>
     * First, the total number of bytes needed to represent all the characters of <code>s</code> is calculated. If this number is
     * larger than <code>65535</code>, then a <code>UTFDataFormatException</code> is thrown. Otherwise, this length is written to
     * the output stream in exactly the manner of the <code>writeShort</code> method; after this, the one-, two-, or three-byte
     * representation of each character in the string <code>s</code> is written.
     * <p>
     * The bytes written by this method may be read by the <code>readUTF</code> method of interface <code>DataInput</code> , which
     * will then return a <code>String</code> equal to <code>s</code>.
     * 
     * @param str the string value to be written.
     * @return the number of bytes written
     */
    public int writeUTFString( String str ) {
        int numBytesWritten = writeUTF(position, str);
        position += numBytesWritten;
        return numBytesWritten;
    }

    public int writeUTF( int position,
                         String str ) {
        CharBuffer chars = CharBuffer.wrap(str);
        CharsetEncoder encoder = Utf8Util.getUtf8Encoder();
        ByteBuffer output = getBufferFor(position);
        int index = position % bufferSize;
        int newPosition = position;
        output.position(index);
        try {
            while (chars.hasRemaining()) {
                CoderResult result = encoder.encode(chars, output, true);

                if (output == remainderBuffer) {
                    // There is some remainder from the previous iteration ...
                    output.flip();
                    while (output.remaining() > 0) {
                        writeByte(newPosition, output.get());
                        ++newPosition;
                    }
                } else {
                    // Calculate the new position based upon where we started ...
                    newPosition += output.position() - index;
                }

                if (result.isError()) {
                    // Some problem occurred ...
                    result.throwException();
                } else if (result.isOverflow()) {
                    // Unable to write all of the content to the output buffer, so there's still some characters left ...
                    if (output.remaining() > 0) {

                        // We ran out of space in 'buffer', so go again with a small 'remainder' buffer ...
                        if (remainderBuffer != null) {
                            // There is one, so clear it out and prepare it for use ...
                            remainderBuffer.clear();
                        } else {
                            // There is none, so allocate one for this object ...
                            remainderBuffer = ByteBuffer.allocate(4);
                        }
                        output = remainderBuffer;
                        index = 0;
                    } else {
                        // We ran out of space by fully utilizing this buffer, so just go to the next buffer ...
                        output = getBufferFor(newPosition);
                        index = newPosition % bufferSize;
                        output.position(index);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to encode string", e);
        }
        updateSize(newPosition);
        return newPosition - position;
    }

    /**
     * Write all content to the supplied stream.
     * 
     * @param stream the stream to which the content is to be written.
     * @throws IOException if there is a problem writing to the supplied stream
     */
    public void writeTo( OutputStream stream ) throws IOException {
        writeTo(Channels.newChannel(stream));
    }

    /**
     * Write all content to the supplied channel.
     * 
     * @param channel the channel to which the content is to be written.
     * @throws IOException if there is a problem writing to the supplied stream
     */
    public void writeTo( WritableByteChannel channel ) throws IOException {
        int numberOfBytesToWrite = size;
        for (ByteBuffer buffer : buffers) {
            if (buffer == null) {
                // already flushed
                continue;
            }
            int numBytesInBuffer = Math.min(numberOfBytesToWrite, bufferSize);
            buffer.position(numBytesInBuffer);
            buffer.flip();
            channel.write(buffer);
            numberOfBytesToWrite -= numBytesInBuffer;
        }
        buffers.clear();
    }
}
