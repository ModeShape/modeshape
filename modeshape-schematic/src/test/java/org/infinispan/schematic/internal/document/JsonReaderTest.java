/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.infinispan.schematic.internal.document;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
        }
        assertThat(count, is(265));
    }

    @Test
    @FixFor( "MODE-2082" )
    public void shouldParseJsonWithComments() throws Exception {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("json/example-with-comments.json");
        doc = reader.read(stream);
        assertNotNull(doc);
        assertEquals(4, countFields(doc));
        assertField("firstName", "Jane");
        assertField("lastName", "Doe");

        doc = doc.getDocument("address");
        assertEquals(3, countFields(doc));
        assertField("street", "Main // Street");
        assertField("city", "Mem/ phis");
        assertField("zip", 12345);
    }

    @Test
    @FixFor( "MODE-2082" )
    public void shouldNotParseJsonWithInvalidComments() throws Exception {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("json/invalid-example-with-comments.json");
        try {
            doc = reader.read(stream);
            fail("Expected parsing exception");
        } catch (ParsingException e) {
            //expected
        }

        stream = getClass().getClassLoader().getResourceAsStream("json/invalid-example-with-comments2.json");
        try {
            doc = reader.read(stream);
            fail("Expected parsing exception");
        } catch (ParsingException e) {
            //expected
        }
    }

    private int countFields(Document doc) {
        int fieldCount = 0;
        for (Document.Field field : doc.fields()) {
            fieldCount++;
        }
        return fieldCount;
    }

    protected void assertField( String name,
                                Object value ) {
        Object actual = doc.get(name);
        if (value == null) {
            assert Null.matches(actual);
        } else {
            assert value.equals(actual);
        }
    }

}
