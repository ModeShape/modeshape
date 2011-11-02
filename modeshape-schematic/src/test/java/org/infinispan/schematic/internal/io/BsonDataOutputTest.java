package org.infinispan.schematic.internal.io;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class BsonDataOutputTest {

    private BsonDataOutput output;
    private ByteArrayOutputStream bigEndianBytes;
    private DataOutputStream bigEndian;
    private ByteArrayOutputStream littleEndianBytes;

    @Before
    public void beforeMethod() {
        this.output = new BsonDataOutput();
        this.bigEndianBytes = new ByteArrayOutputStream();
        this.bigEndian = new DataOutputStream(this.bigEndianBytes);
        this.littleEndianBytes = new ByteArrayOutputStream();
    }

    @After
    public void afterMethod() {
    }

    @Test
    public void shouldWriteAndReadInteger() throws Exception {
        output.writeInt(102);
        bigEndian.writeInt(toLittleEndian(102));
        output.writeTo(littleEndianBytes);
        assertSame(littleEndianBytes.toByteArray(), bigEndianBytes.toByteArray(), "littleEndian", "bigEndian");
    }

    @Test
    public void shouldWriteAndReadLong() throws Exception {
        output.writeLong(102L);
        bigEndian.writeLong(toLittleEndian(102L));
        output.writeTo(littleEndianBytes);
        assertSame(littleEndianBytes.toByteArray(), bigEndianBytes.toByteArray(), "littleEndian", "bigEndian");
    }

    @Test
    public void shouldWriteAndReadShort() throws Exception {
        output.writeShort(102);
        bigEndian.writeShort(toLittleEndian((short)102));
        output.writeTo(littleEndianBytes);
        assertSame(littleEndianBytes.toByteArray(), bigEndianBytes.toByteArray(), "littleEndian", "bigEndian");
    }

    @Test
    public void shouldWriteAndReadChar() throws Exception {
        output.writeChar('c');
        bigEndian.writeChar(toLittleEndian('c'));
        output.writeTo(littleEndianBytes);
        assertSame(littleEndianBytes.toByteArray(), bigEndianBytes.toByteArray(), "littleEndian", "bigEndian");
    }

    protected char toLittleEndian( char value ) {
        byte b1 = (byte)value;
        byte b2 = (byte)(value >> 8);
        int result = b1 << 8 | (b2 & 0xFF);
        return (char)result;
    }

    protected short toLittleEndian( short value ) {
        byte b1 = (byte)value;
        byte b2 = (byte)(value >> 8);
        int result = b1 << 8 | (b2 & 0xFF);
        return (short)result;
    }

    protected int toLittleEndian( int value ) {
        byte b1 = (byte)value;
        byte b2 = (byte)(value >> 8);
        byte b3 = (byte)(value >> 16);
        byte b4 = (byte)(value >> 24);
        int result = b1 << 24 | (b2 & 0xFF) << 16 | (b3 & 0xFF) << 8 | (b4 & 0xFF);
        return result;
    }

    protected long toLittleEndian( long value ) {
        byte b1 = (byte)value;
        byte b2 = (byte)(value >> 8);
        byte b3 = (byte)(value >> 16);
        byte b4 = (byte)(value >> 24);
        byte b5 = (byte)(value >> 32);
        byte b6 = (byte)(value >> 40);
        byte b7 = (byte)(value >> 48);
        byte b8 = (byte)(value >> 56);
        long result = (b1 & 0xFFL) << 56 | (b2 & 0xFFL) << 48 | (b3 & 0xFFL) << 40 | (b4 & 0xFFL) << 32 | (b5 & 0xFFL) << 24
                      | (b6 & 0xFFL) << 16 | (b7 & 0xFFL) << 8 | (b8 & 0xFFL);
        return result;
    }

    @SuppressWarnings( "cast" )
    protected void assertSame( byte[] b1,
                               byte[] b2,
                               String name1,
                               String name2 ) {
        if (b1.equals(b2)) return;
        int s1 = b1.length;
        int s2 = b2.length;
        StringBuilder sb1 = new StringBuilder();
        for (byte b : b1) {
            sb1.append((int)b).append(' ');
        }
        StringBuilder sb2 = new StringBuilder();
        for (byte b : b2) {
            sb2.append((int)b).append(' ');
        }
        if (!sb1.toString().equals(sb2.toString())) {
            System.out.println(name1 + " size: " + s1 + " content: " + sb1);
            System.out.println(name2 + " size: " + s2 + " content: " + sb2);
            assert false;
        }
    }

}
