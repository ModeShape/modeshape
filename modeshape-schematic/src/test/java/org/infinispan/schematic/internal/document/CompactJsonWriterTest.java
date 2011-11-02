package org.infinispan.schematic.internal.document;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import org.infinispan.schematic.document.Binary;
import org.infinispan.schematic.document.Bson;
import org.infinispan.schematic.document.Code;
import org.infinispan.schematic.document.CodeWithScope;
import org.infinispan.schematic.document.MaxKey;
import org.infinispan.schematic.document.MinKey;
import org.infinispan.schematic.document.ObjectId;
import org.infinispan.schematic.document.Symbol;
import org.infinispan.schematic.document.Timestamp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CompactJsonWriterTest {

    protected JsonWriter writer;
    protected boolean print;

    @Before
    public void beforeTest() {
        writer = new CompactJsonWriter();
        print = false;
    }

    @After
    public void afterTest() {
        writer = null;
    }

    @Test
    public void shouldCorrectlyWriteNullValue() {
        assertSame("null", writer.write(null));
    }

    @Test
    public void shouldCorrectlyWriteBooleanValues() {
        assertSame("true", writer.write(Boolean.TRUE));
        assertSame("false", writer.write(Boolean.FALSE));
    }

    @Test
    public void shouldCorrectlyWriteIntegerValues() {
        assertSame("10", writer.write(10));
        assertSame("0", writer.write(0));
        assertSame("-1", writer.write(-1));
        assertSame(Integer.toString(Integer.MAX_VALUE), writer.write(Integer.MAX_VALUE));
        assertSame(Integer.toString(Integer.MIN_VALUE), writer.write(Integer.MIN_VALUE));
    }

    @Test
    public void shouldCorrectlyWriteLongValues() {
        assertSame("10", writer.write(10L));
        assertSame("0", writer.write(0L));
        assertSame("-1", writer.write(-1L));
        assertSame(Long.toString(Integer.MAX_VALUE + 10L), writer.write(Integer.MAX_VALUE + 10L));
        assertSame(Long.toString(Integer.MIN_VALUE - 10L), writer.write(Integer.MIN_VALUE - 10L));
        assertSame(Long.toString(Long.MAX_VALUE), writer.write(Long.MAX_VALUE));
        assertSame(Long.toString(Long.MIN_VALUE), writer.write(Long.MIN_VALUE));
    }

    @Test
    public void shouldCorrectlyWriteFloatValues() {
        assertSame("10.01", writer.write(10.01));
        assertSame("0.0", writer.write(0.0));
        assertSame("-1.0135", writer.write(-1.0135));
        assertSame(Float.toString(Float.MAX_VALUE), writer.write(Float.MAX_VALUE));
        assertSame(Float.toString(Float.MIN_VALUE), writer.write(Float.MIN_VALUE));
    }

    @Test
    public void shouldCorrectlyWriteDoubleValues() {
        assertSame("10.01", writer.write(10.01d));
        assertSame("0.0", writer.write(0.0d));
        assertSame("-1.0135", writer.write(-1.0135d));
        assertSame(Double.toString(Double.MAX_VALUE), writer.write(Double.MAX_VALUE));
        assertSame(Double.toString(Double.MIN_VALUE), writer.write(Double.MIN_VALUE));
    }

    @Test
    public void shouldCorrectlyWriteStringValues() {
        assertSame("\"\"", writer.write(""));
        assertSame("\"10.01\"", writer.write("10.01"));
        assertSame("\"10.01d\"", writer.write("10.01d"));
        assertSame("\"null\"", writer.write("null"));
        assertSame("\"abcdefghijklmnopqrstuvwxyz\"", writer.write("abcdefghijklmnopqrstuvwxyz"));
    }

    @Test
    public void shouldCorrectlyWriteSymbolValues() {
        assertSame("\"\"", writer.write(new Symbol("")));
        assertSame("\"10.01\"", writer.write(new Symbol("10.01")));
        assertSame("\"10.01d\"", writer.write(new Symbol("10.01d")));
        assertSame("\"null\"", writer.write(new Symbol("null")));
        assertSame("\"abcdefghijklmnopqrstuvwxyz\"", writer.write(new Symbol("abcdefghijklmnopqrstuvwxyz")));
    }

    @Test
    public void shouldCorrectlyWriteUuid() {
        UUID id = UUID.randomUUID();
        String expected = "{ \"$uuid\" : \"" + id + "\" }";
        String actual = writer.write(id);
        // print =true;
        assertSame(expected, actual);
    }

    @Test
    public void shouldCorrectlyWriteObjectId() {
        ObjectId id = new ObjectId(300, 200, 9, 15);
        String expected = "{ \"$oid\" : \"0000012c0000c8000900000f\" }";
        String actual = writer.write(id);
        assertSame(expected, actual);
    }

    @Test
    public void shouldCorrectlyWriteDate() {
        Date now = new Date();
        String dateStr = Bson.getDateFormatter().format(now);
        String expected = "{ \"$date\" : \"" + dateStr + "\" }";
        String actual = writer.write(now);
        assertSame(expected, actual);
    }

    @Test
    public void shouldCorrectlyWriteTimestamp() {
        Timestamp now = new Timestamp(new Date());
        String expected = "{ \"$ts\" : " + now.getTime() + " , \"$inc\" : " + now.getInc() + " }";
        String actual = writer.write(now);
        assertSame(expected, actual);
    }

    @Test
    public void shouldCorrectlyWriteMinKeyValue() {
        assertSame("\"MinKey\"", writer.write(MinKey.getInstance()));
    }

    @Test
    public void shouldCorrectlyWriteMaxKeyValue() {
        assertSame("\"MaxKey\"", writer.write(MaxKey.getInstance()));
    }

    @Test
    public void shouldCorrectlyWriteBinaryValue() {
        byte[] data = new byte[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05};
        @SuppressWarnings( "deprecation" )
        Binary binary = new Binary(Bson.BinaryType.BINARY, data);
        String expected = "{ \"$type\" : 2 , \"$base64\" : \"AAECAwQF\" }";
        String actual = writer.write(binary);
        // print =true;
        assertSame(expected, actual);
    }

    @Test
    public void shouldCorrectlyWritePattern() {
        Pattern pattern = Pattern.compile("[CH]at\\s+in", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        String expected = "{ \"$regex\" : \"[CH]at\\s+in\" , \"$options\" : \"im\" }";
        String actual = writer.write(pattern);
        assertSame(expected, actual);
    }

    @Test
    public void shouldCorrectlyWriteSimpleBsonObject() {
        BasicDocument top = new BasicDocument();
        top.put("firstName", "Jack");
        top.put("lastName", "Riley");
        top.put("age", 31);
        String actual = writer.write(top);
        String expected = "{ \"firstName\" : \"Jack\" , \"lastName\" : \"Riley\" , \"age\" : 31 }";
        assertSame(expected, actual);
    }

    @Test
    public void shouldCorrectlyWriteSimpleBsonObjectWithNullValue() {
        BasicDocument top = new BasicDocument();
        top.put("firstName", "Jack");
        top.put("lastName", null);
        top.put("age", 31);
        String actual = writer.write(top);
        String expected = "{ \"firstName\" : \"Jack\" , \"lastName\" : null , \"age\" : 31 }";
        assertSame(expected, actual);
    }

    @Test
    public void shouldCorrectlyWriteBsonObjectWithNestedObjectValue() {
        BasicDocument address = new BasicDocument();
        address.put("street", "100 Main St.");
        address.put("city", "Springfield");
        BasicDocument top = new BasicDocument();
        top.put("firstName", "Jack");
        top.put("lastName", "Riley");
        top.put("address", address);
        String actual = writer.write(top);
        String expected = "{ \"firstName\" : \"Jack\" , \"lastName\" : \"Riley\" , \"address\" : { \"street\" : \"100 Main St.\" , \"city\" : \"Springfield\" } }";
        assertSame(expected, actual);
    }

    @Test
    public void shouldCorrectlyWriteListValue() {
        testWritingList("[ ]");
        testWritingList("[ \"value1\" ]", "value1");
        testWritingList("[ \"value1\" , null , \"value3\" ]", "value1", null, "value3");
        testWritingList("[ \"value1\" , \"value2\" , \"value3\" ]", "value1", "value2", "value3");
        testWritingList("[ \"value1\" , \"value2\" , 4 ]", "value1", "value2", 4L);
    }

    protected void testWritingList( String expected,
                                    Object... values ) {
        List<Object> list = Arrays.asList(values);
        String actual = writer.write(list);
        assertSame(expected, actual);
    }

    @Test
    public void shouldCorrectlyWriteCode() {
        Code code = new Code("name");
        String expected = "{ \"$code\" : \"name\" }";
        String actual = writer.write(code);
        assertSame(expected, actual);
    }

    @Test
    public void shouldCorrectlyWriteCodeWithScope() {
        BasicDocument scope = new BasicDocument();
        scope.put("firstName", "Jack");
        scope.put("lastName", "Riley");
        CodeWithScope code = new CodeWithScope("name", scope);
        String actual = writer.write(code);
        String expected = "{ \"$code\" : \"name\" , \"$scope\" : { \"firstName\" : \"Jack\" , \"lastName\" : \"Riley\" } }";
        assertSame(expected, actual);
    }

    protected void assertSame( String expected,
                               String actual ) {
        if (print) {
            System.out.println("************************************************************");
            System.out.println(actual);
            System.out.println(expected);
        }
        assert expected.equals(actual);
    }

}
