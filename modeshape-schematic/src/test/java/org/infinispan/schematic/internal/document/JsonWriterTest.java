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

import static org.junit.Assert.assertEquals;
import org.infinispan.schematic.Schematic;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableDocument;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JsonWriterTest {

    protected JsonWriter writer;
    protected JsonReader reader;
    protected boolean print;
    protected EditableDocument doc1;
    protected Document doc2;

    @Before
    public void beforeTest() {
        writer = new PrettyJsonWriter();
        reader = new JsonReader();
        print = false;
    }

    @After
    public void afterTest() {
        writer = null;
        reader = null;
        doc1 = null;
        doc2 = null;
    }

    @Test
    public void shouldWriteEmptyDocument() throws Exception {
        doc1 = Schematic.newDocument();
        String s = writer.write(doc1);
        doc2 = reader.read(s);
        assertMatch(doc1, doc2);
    }

    @Test
    public void shouldWriteDocumentWithOneField() throws Exception {
        doc1 = Schematic.newDocument("field1", "value1");
        String s = writer.write(doc1);
        doc2 = reader.read(s);
        assertMatch(doc1, doc2);
    }

    @Test
    public void shouldWriteDocumentWithTwoFields() throws Exception {
        doc1 = Schematic.newDocument("field1", "value1", "field2", 3);
        String s = writer.write(doc1);
        doc2 = reader.read(s);
        assertMatch(doc1, doc2);
    }

    @Test
    public void shouldWriteDocumentWithEscapedCharacters() throws Exception {
        doc1 = Schematic.newDocument("field1", "value1", "field2", 3);
        doc1.setString("field3", "This has\nmultiple\nlines");
        doc1.setString("field4", "This has\r\nmultiple\r\n lines");
        String s = writer.write(doc1);
        // System.out.println(s);
        doc2 = reader.read(s);
        assertMatch(doc1, doc2);
    }

    protected void assertMatch( Document doc1,
                                Document doc2 ) {
        assertEquals(doc1.size(), doc2.size());
        for (Document.Field field1 : doc1.fields()) {
            if (field1.getValue() instanceof Document) {
                assertMatch(field1.getValueAsDocument(), doc2.getDocument(field1.getName()));
            } else {
                Object value2 = doc2.get(field1.getName());
                assertEquals(field1.getValue(), value2);
            }
        }
    }

}
