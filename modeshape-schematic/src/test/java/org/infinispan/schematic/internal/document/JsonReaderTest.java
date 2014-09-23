package org.infinispan.schematic.internal.document;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.io.InputStream;
import org.infinispan.schematic.FixFor;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.DocumentSequence;
import org.infinispan.schematic.document.Null;
import org.infinispan.schematic.document.ParsingException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JsonReaderTest {

    protected JsonReader reader;
    protected boolean print;
    protected Document doc;

    @Before
    public void beforeTest() {
        reader = new JsonReader();
        print = false;
    }

    @After
    public void afterTest() {
        reader = null;
    }

    @Test
    public void shouldParseEmptyDocument() throws Exception {
        doc = reader.read("{ }");
    }

    @Test
    public void shouldParseEmptyDocumentWithLeadingWhitespace() throws Exception {
        doc = reader.read("  { }");
    }

    @Test
    public void shouldParseEmptyDocumentWithEmbeddedWhitespace() throws Exception {
        doc = reader.read("{ \n \t \r\n }  ");
    }

    @Test
    public void shouldParseDocumentWithSingleFieldWithStringValue() throws Exception {
        doc = reader.read("{ \"foo\" : \"bar\" }");
        assertField("foo", "bar");
    }

    @Test
    public void shouldParseDocumentWithSingleFieldWithBooleanFalseValue() throws Exception {
        doc = reader.read("{ \"foo\" : false }");
        assertField("foo", false);
    }

    @Test
    public void shouldParseDocumentWithSingleFieldWithBooleanTrueValue() throws Exception {
        doc = reader.read("{ \"foo\" : true }");
        assertField("foo", true);
    }

    @Test
    public void shouldParseDocumentWithSingleFieldWithIntegerValue() throws Exception {
        doc = reader.read("{ \"foo\" : 0 }");
        assertField("foo", 0);
    }

    @Test
    public void shouldParseDocumentWithSingleFieldWithLongValue() throws Exception {
        long val = Integer.MAX_VALUE + 10L;
        doc = reader.read("{ \"foo\" : " + val + " }");
        assertField("foo", val);
    }

    @Test
    public void shouldParseDocumentWithSingleFieldWithDoubleValue() throws Exception {
        doc = reader.read("{ \"foo\" : 2.3543 }");
        assertField("foo", 2.3543d);
    }

    @Test
    public void shouldParseDocumentWithMultipleFieldsAndExtraDelimiters() throws Exception {
        doc = reader.read("{ \"foo\" : 32 ,, \"nested\" : { \"bar\" : \"baz\", \"bom\" : true } ,}");
        assertField("foo", 32);
        doc = doc.getDocument("nested");
        assertField("bar", "baz");
        assertField("bom", true);
    }

    @Test
    public void shouldParseMultipleDocuments() throws Exception {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("json/multiple-json-files.txt");
        DocumentSequence seq = reader.readMultiple(stream);
        int count = 0;
        while (true) {
            Document doc = seq.nextDocument();
            if (doc == null) break;
            ++count;
            // System.out.println("Read document " + count + " --> " + doc);
        }
        assertThat(count, is(265));
    }

    @Test
    @FixFor( "MODE-2309" )
    public void shouldParseJsonWithControlCharactersInFieldValues() throws Exception {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("json/example-with-control-characters.json");
        doc = reader.read(stream);
        assertNotNull(doc);
        assertEquals(6, countFields(doc));
        assertField("firstName", "Jane");
        assertField("lastName", "Doe");
        assertField("fieldWithLF", "This is a value\nwith a line feed");
        assertField("fieldWithCRLF", "This is a value\r\nwith a line feed\r\ncarriage return");

        doc = doc.getDocument("address");
        assertEquals(3, countFields(doc));
        assertField("street", "Main Street");
        assertField("city", "Memphis");
        assertField("zip", 12345);
    }

    @Test
    @FixFor( "MODE-2317" )
    public void shouldParseNonUnicodeEscapeSequence() throws Exception {
        String url = "jdbc:h2:file:path\\upstream.jboss-integration.modeshape\\modeshape-jcr;DB_CLOSE_DELAY=-1";
        doc = reader.read("{ \"url\" : \"" + url +  "\"}");
        assertField("url", url);

        InputStream stream = getClass().getClassLoader().getResourceAsStream("json/non-unicode-escape.json");
        reader.read(stream);
    }

    private int countFields( Document doc ) {
        int fieldCount = 0;
        for (Document.Field field : doc.fields()) {
            assertThat(field, is(notNullValue()));
            fieldCount++;
        }
        return fieldCount;
    }

    protected void assertField( String name,
                                Object value ) {
        Object actual = doc.get(name);
        if (value == null) {
            assertNull(actual);
        } else {
            assertEquals(value, actual);
        }
    }

}
