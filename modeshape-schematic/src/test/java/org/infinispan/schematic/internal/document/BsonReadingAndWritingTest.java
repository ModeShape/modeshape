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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.bson.BSONObject;
import org.bson.BasicBSONCallback;
import org.bson.BasicBSONDecoder;
import org.bson.BasicBSONEncoder;
import org.bson.BasicBSONObject;
import org.bson.types.BSONTimestamp;
import org.bson.types.BasicBSONList;
import org.codehaus.jackson.JsonToken;
import org.infinispan.schematic.TestUtil;
import org.infinispan.schematic.document.Binary;
import org.infinispan.schematic.document.Code;
import org.infinispan.schematic.document.CodeWithScope;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.Json;
import org.infinispan.schematic.document.MaxKey;
import org.infinispan.schematic.document.MinKey;
import org.infinispan.schematic.document.ObjectId;
import org.infinispan.schematic.document.Symbol;
import org.infinispan.schematic.document.Timestamp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class BsonReadingAndWritingTest {

    protected BsonReader reader;
    protected BsonWriter writer;
    protected Document input;
    protected Document output;
    protected boolean print;

    @Before
    public void beforeTest() {
        reader = new BsonReader();
        writer = new BsonWriter();
        print = false;
    }

    @After
    public void afterTest() {
        reader = null;
        writer = null;
    }

    @Test
    public void shouldReadExampleBsonStream() throws IOException {
        // "\x16\x00\x00\x00\x02hello\x00\x06\x00\x00\x00world\x00\x00"
        byte[] bytes = new byte[] {0x16, 0x00, 0x00, 0x00, 0x02, 0x68, 0x65, 0x6c, 0x6c, 0x6f, 0x00, 0x06, 0x00, 0x00, 0x00,
            0x77, 0x6f, 0x72, 0x6c, 0x64, 0x00, 0x00};
        output = reader.read(new ByteArrayInputStream(bytes));
        String json = Json.write(output);
        String expected = "{ \"hello\" : \"world\" }";
        if (print) {
            System.out.println(json);
            System.out.flush();
        }
        assert expected.equals(json);
    }

    @Test
    public void shouldRoundTripSimpleBsonObjectWithStringValue() {
        input = new BasicDocument("name", "Joe");
        assertRoundtrip(input);
    }

    @Test
    public void shouldRoundTripSimpleBsonObjectWithBooleanValue() {
        input = new BasicDocument("foo", 3L);
        assertRoundtrip(input);
    }

    @Test
    public void shouldRoundTripSimpleBsonObjectWithIntValue() {
        input = new BasicDocument("foo", 3);
        assertRoundtrip(input);
    }

    @Test
    public void shouldRoundTripSimpleBsonObjectWithLongValue() {
        input = new BasicDocument("foo", 3L);
        assertRoundtrip(input);
    }

    @Test
    public void shouldRoundTripSimpleBsonObjectWithFloatValue() {
        input = new BasicDocument("foo", 3.0f);
        assertRoundtrip(input);
    }

    @Test
    public void shouldRoundTripSimpleBsonObjectWithDoubleValue() {
        input = new BasicDocument("foo", 3.0d);
        assertRoundtrip(input);
    }

    @Test
    public void shouldRoundTripSimpleBsonObjectWithDateValue() {
        input = new BasicDocument("foo", new Date(now()));
        assertRoundtrip(input);
    }

    @Test
    public void shouldRoundTripSimpleBsonObjectWithTimestampValue() {
        input = new BasicDocument("foo", new Timestamp(new Date(now())));
        assertRoundtrip(input);
    }

    @Test
    public void shouldRoundTripSimpleBsonObjectWithObjectId() {
        // print = true;
        int time = (int)(now() / 1000L);
        if (print) System.out.println("time value: " + time);
        input = new BasicDocument("foo", new ObjectId(time, 1, 2, 3));
        assertRoundtrip(input);
    }

    @Test
    public void shouldRoundTripSimpleBsonObjectWithCode() {
        input = new BasicDocument("foo", new Code("bar"));
        assertRoundtrip(input);
    }

    @Test
    public void shouldRoundTripSimpleBsonObjectWithCodeWithScope() {
        Document scope = new BasicDocument("baz", "bam", "bak", "bat");
        input = new BasicDocument("foo", new CodeWithScope("bar", scope));
        assertRoundtrip(input);
    }

    @Test
    public void shouldRoundTripSimpleBsonObjectWithMaxKey() {
        input = new BasicDocument("foo", MaxKey.getInstance());
        assertRoundtrip(input, false);
    }

    @Test
    public void shouldRoundTripSimpleBsonObjectWithMinKey() {
        input = new BasicDocument("foo", MinKey.getInstance());
        assertRoundtrip(input, false);
    }

    @Test
    public void shouldRoundTripSimpleBsonObjectWithSymbol() {
        input = new BasicDocument("foo", new Symbol("bar"));
        assertRoundtrip(input);
    }

    @Test
    public void shouldRoundTripSimpleBsonObjectWithNull() {
        input = new BasicDocument("foo", null);
        assertRoundtrip(input);
    }

    @Test
    public void shouldRoundTripSimpleBsonObjectWithBinary() {
        byte[] data = new byte[] {0x16, 0x00, 0x00, 0x00, 0x02, 0x68, 0x65, 0x6c};
        input = new BasicDocument("foo", new Binary(data));
        assertRoundtrip(input);
    }

    @Test
    public void shouldRoundTripSimpleBsonObjectWithUuid() {
        input = new BasicDocument("foo", UUID.randomUUID());
        assertRoundtrip(input);
    }

    @Test
    public void shouldRoundTripSimpleBsonObjectWithPattern() {
        // print = true;
        input = new BasicDocument("foo", Pattern.compile("[CH]at\\s+"));
        assertRoundtrip(input);
    }

    @Test
    public void shouldRoundTripSimpleBsonObjectWithPatternAndFlags() {
        // print = true;
        input = new BasicDocument("foo", Pattern.compile("[CH]at\\s+", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE));
        assertRoundtrip(input);
    }

    @Test
    public void shouldRoundTripSimpleBsonObjectWithArray() {
        BasicArray array = new BasicArray();
        array.addValue("value1");
        array.addValue(new Symbol("value2"));
        array.addValue(30);
        array.addValue(40L);
        array.addValue(4.33d);
        array.addValue(false);
        array.addValue(null);
        array.addValue("value2");
        input = new BasicDocument("foo", array);
        assertRoundtrip(input);
    }

    @Test
    public void shouldRoundTripBsonObjectWithTwoFields() {
        input = new BasicDocument("name", "Joe", "age", 35);
        assertRoundtrip(input);
    }

    @Test
    public void shouldRoundTripBsonObjectWithThreeFields() {
        input = new BasicDocument("name", "Joe", "age", 35, "nick", "joey");
        assertRoundtrip(input);
    }

    @Test
    public void shouldRoundTripBsonObjectWithNestedDocument() {
        BasicDocument address = new BasicDocument("street", "100 Main", "city", "Springfield", "zip", 12345);
        input = new BasicDocument("name", "Joe", "age", 35, "address", address, "nick", "joey");
        assertRoundtrip(input);
    }

    @Test
    public void shouldRoundTripLargeModeShapeDocument() throws Exception {
        Document doc = Json.read(TestUtil.resource("json/sample-large-modeshape-doc.json"));
        // OutputStream os = new FileOutputStream("src/test/resources/json/sample-large-modeshape-doc2.json");
        // Json.writePretty(doc, os);
        // os.flush();
        // os.close();
        assertRoundtrip(doc);
    }

    protected void assertRoundtrip( Document input ) {
        assertRoundtrip(input, true);
    }

    protected void assertRoundtrip( Document input,
                                    boolean compareToOtherImpls ) {
        assert input != null;
        Document output = writeThenRead(input, compareToOtherImpls);
        if (print) {
            System.out.println("********************************************************************************");
            System.out.println("INPUT :  " + input);
            System.out.println();
            System.out.println("OUTPUT:  " + output);
            System.out.println("********************************************************************************");
            System.out.flush();
        }
        assert input.equals(output);
    }

    protected long now() {
        return System.currentTimeMillis();
    }

    protected Document writeThenRead( Document object,
                                      boolean compareToOtherImpls ) {
        try {
            long start = System.nanoTime();
            byte[] bytes = writer.write(object);
            long writeTime = System.nanoTime() - start;

            start = System.nanoTime();
            Document result = reader.read(new ByteArrayInputStream(bytes));
            long readTime = System.nanoTime() - start;

            if (compareToOtherImpls) {
                // Convert to MongoDB, write to bytes, and compare ...
                BSONObject mongoData = createMongoData(object);
                start = System.nanoTime();
                byte[] mongoBytes = new BasicBSONEncoder().encode(mongoData);
                long mongoWriteTime = System.nanoTime() - start;
                assertSame(bytes, mongoBytes, "BSON   ", "Mongo  ");

                // FYI: The Jackson BSON library writes several of the types incorrectly,
                // whereas the MongoDB library seems to write things per the spec.

                // // Convert to Jackson BSON, write to bytes, and compare ...
                // ByteArrayOutputStream stream2 = new ByteArrayOutputStream();
                // ObjectMapper om = new ObjectMapper(new BsonFactory());
                // Map<String, Object> jacksonData = createJacksonData(object);
                // om.writeValue(stream2, jacksonData);
                // byte[] jacksonBytes = stream2.toByteArray();
                // assertSame(bytes, jacksonBytes, "BSON   ", "Jackson");

                start = System.nanoTime();
                new BasicBSONDecoder().decode(bytes, new BasicBSONCallback());
                long mongoReadTime = System.nanoTime() - start;

                Document fromMongo = reader.read(new ByteArrayInputStream(mongoBytes));
                if (!fromMongo.equals(result)) {
                    System.out.println("from Schematic: " + result);
                    System.out.println("from Mongo:     " + fromMongo);
                    assert false : "Document read from bytes written by Mongo did not match expected document: " + result;
                }

                if (print) {
                    System.out.println("Reading with Schematic:  " + percent(readTime, mongoReadTime) + " than Mongo");
                    System.out.println("Writing with Schematic:  " + percent(writeTime, mongoWriteTime) + " than Mongo");
                }
            }

            return result;
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    protected String time( long nanos ) {
        return "" + TimeUnit.NANOSECONDS.convert(nanos, TimeUnit.NANOSECONDS) + "ns";
    }

    protected String percent( long nanos1,
                              long nanos2 ) {
        float percent = 100.0f * (float)(((double)nanos2 - (double)nanos1) / nanos1);
        if (percent < 0.0d) {
            return "" + -percent + "% slower";
        }
        return "" + percent + "% faster";
    }

    protected BSONObject createMongoData( Document document ) {
        BSONObject obj = new BasicBSONObject();
        for (Document.Field field : document.fields()) {
            Object value = field.getValue();
            obj.put(field.getName(), createMongoData(value));
        }
        return obj;
    }

    protected Object createMongoData( Object value ) {
        if (value instanceof MinKey) {
            value = "MinKey";
        } else if (value instanceof MaxKey) {
            value = "MaxKey";
        } else if (value instanceof Symbol) {
            Symbol symbol = (Symbol)value;
            value = new org.bson.types.Symbol(symbol.getSymbol());
        } else if (value instanceof ObjectId) {
            ObjectId id = (ObjectId)value;
            value = new org.bson.types.ObjectId(id.getBytes());
        } else if (value instanceof Timestamp) {
            Timestamp ts = (Timestamp)value;
            value = new BSONTimestamp(ts.getTime(), ts.getInc());
        } else if (value instanceof CodeWithScope) {
            CodeWithScope code = (CodeWithScope)value;
            value = new org.bson.types.CodeWScope(code.getCode(), createMongoData(code.getScope()));
        } else if (value instanceof Code) {
            Code code = (Code)value;
            value = new org.bson.types.Code(code.getCode());
        } else if (value instanceof Binary) {
            Binary binary = (Binary)value;
            value = new org.bson.types.Binary(binary.getBytes());
        } else if (value instanceof List) {
            List<?> values = (List<?>)value;
            BasicBSONList newValues = new BasicBSONList();
            for (Object v : values) {
                newValues.add(createMongoData(v));
            }
            value = newValues;
        } else if (value instanceof Document) {
            value = createMongoData((Document)value);
        }
        return value;
    }

    protected Map<String, Object> createJacksonData( Document document ) {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        for (Document.Field field : document.fields()) {
            Object value = field.getValue();
            data.put(field.getName(), createJacksonData(value));
        }
        return data;
    }

    protected Object createJacksonData( Object value ) {
        if (value instanceof MinKey) {
            value = JsonToken.VALUE_STRING;
        } else if (value instanceof MaxKey) {
            value = JsonToken.VALUE_STRING;
        } else if (value instanceof Symbol) {
            value = new de.undercouch.bson4jackson.types.Symbol(((Symbol)value).getSymbol());
        } else if (value instanceof ObjectId) {
            ObjectId id = (ObjectId)value;
            value = new de.undercouch.bson4jackson.types.ObjectId(id.getTime(), id.getMachine(), id.getInc());
        } else if (value instanceof Timestamp) {
            Timestamp ts = (Timestamp)value;
            value = new de.undercouch.bson4jackson.types.Timestamp(ts.getTime(), ts.getInc());
        } else if (value instanceof CodeWithScope) {
            CodeWithScope code = (CodeWithScope)value;
            value = new de.undercouch.bson4jackson.types.JavaScript(code.getCode(), createJacksonData(code.getScope()));
        } else if (value instanceof Code) {
            Code code = (Code)value;
            value = new de.undercouch.bson4jackson.types.JavaScript(code.getCode(), null);
        } else if (value instanceof List) {
            List<?> values = (List<?>)value;
            List<Object> newValues = new ArrayList<Object>(values.size());
            for (Object v : values) {
                newValues.add(createJacksonData(v));
            }
            value = newValues;
        } else if (value instanceof Document) {
            value = createJacksonData((Document)value);
        }
        return value;
    }

    protected void assertSame( byte[] b1,
                               byte[] b2,
                               String name1,
                               String name2 ) {
        if (b1.equals(b2)) return;
        int s1 = b1.length;
        int s2 = b2.length;
        String sb1 = toString(b1);
        String sb2 = toString(b2);
        if (!sb1.equals(sb2)) {
            System.out.println(name1 + " size: " + padLeft(s1, 3) + " content: " + sb1);
            System.out.println(name2 + " size: " + padLeft(s2, 3) + " content: " + sb2);
            assert false;
        }
    }

    protected String padLeft( Object value,
                              int width ) {
        String result = value != null ? value.toString() : "null";
        while (result.length() < width) {
            result = " " + result;
        }
        return result;
    }

    protected String toString( byte[] bytes ) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(padLeft((int)b, 4)).append(' ');
        }
        return sb.toString();
    }

}
