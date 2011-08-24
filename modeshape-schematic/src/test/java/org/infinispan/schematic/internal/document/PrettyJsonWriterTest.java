package org.infinispan.schematic.internal.document;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.junit.Test;

public class PrettyJsonWriterTest extends CompactJsonWriterTest {

    @Override
    @Test
    public void shouldCorrectlyWriteSimpleBsonObject() {
        writer = new PrettyJsonWriter();
        BasicDocument top = new BasicDocument();
        top.put("firstName", "Jack");
        top.put("lastName", "Riley");
        top.put("age", 31);
        String actual = writer.write(top);
        String expected = "{\n  \"firstName\" : \"Jack\",\n  \"lastName\" : \"Riley\",\n  \"age\" : 31\n}";
        assertSame(expected, actual);
    }

    @Override
    @Test
    public void shouldCorrectlyWriteSimpleBsonObjectWithNullValue() {
        writer = new PrettyJsonWriter();
        BasicDocument top = new BasicDocument();
        top.put("firstName", "Jack");
        top.put("lastName", null);
        top.put("age", 31);
        String actual = writer.write(top);
        String expected = "{\n  \"firstName\" : \"Jack\",\n  \"lastName\" : null,\n  \"age\" : 31\n}";
        assertSame(expected, actual);
    }

    @Override
    @Test
    public void shouldCorrectlyWriteBsonObjectWithNestedObjectValue() {
        writer = new PrettyJsonWriter();
        BasicDocument address = new BasicDocument();
        address.put("street", "100 Main St.");
        address.put("city", "Springfield");
        BasicDocument top = new BasicDocument();
        top.put("firstName", "Jack");
        top.put("lastName", "Riley");
        top.put("address", address);
        String actual = writer.write(top);
        String expected = "{\n  \"firstName\" : \"Jack\",\n  \"lastName\" : \"Riley\",\n  \"address\" : {\n    \"street\" : \"100 Main St.\",\n    \"city\" : \"Springfield\"\n  }\n}";
        assertSame(expected, actual);
    }

    @Test
    public void shouldCorrectlyWriteDocumentToStream() throws IOException {
        writer = new PrettyJsonWriter();
        BasicDocument address = new BasicDocument();
        address.put("street", "100 Main St.");
        address.put("city", "Springfield");
        BasicDocument top = new BasicDocument();
        top.put("firstName", "Jack");
        top.put("lastName", "Riley");
        top.put("address", address);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writer.write(top, baos);
        String actual = baos.toString();
        String expected = "{\n  \"firstName\" : \"Jack\",\n  \"lastName\" : \"Riley\",\n  \"address\" : {\n    \"street\" : \"100 Main St.\",\n    \"city\" : \"Springfield\"\n  }\n}";
        assertSame(expected, actual);
    }

    @Test
    public void shouldCorrectlyWriteBsonObjectWithNestedArrayValue() {
        writer = new PrettyJsonWriter();
        BasicArray emails = new BasicArray();
        emails.put("0", "jriley@example.com");
        emails.put("1", "jriley@foobar.com");
        BasicDocument top = new BasicDocument();
        top.put("firstName", "Jack");
        top.put("lastName", "Riley");
        top.put("emails", emails);
        String actual = writer.write(top);
        String expected = "{\n  \"firstName\" : \"Jack\",\n  \"lastName\" : \"Riley\",\n  \"emails\" : [\n    \"jriley@example.com\",\n    \"jriley@foobar.com\"\n  ]\n}";
        assertSame(expected, actual);
    }

    @Override
    @Test
    public void shouldCorrectlyWriteListValue() {
        writer = new PrettyJsonWriter();
        testWritingList("[ ]");
        testWritingList("[\n  \"value1\"\n]", "value1");
        testWritingList("[\n  \"value1\",\n  null,\n  \"value3\"\n]", "value1", null, "value3");
        testWritingList("[\n  \"value1\",\n  \"value2\",\n  \"value3\"\n]", "value1", "value2", "value3");
        testWritingList("[\n  \"value1\",\n  \"value2\",\n  4\n]", "value1", "value2", 4L);
    }
}
