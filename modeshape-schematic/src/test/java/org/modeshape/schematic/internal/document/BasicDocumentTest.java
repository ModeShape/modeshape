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
package org.modeshape.schematic.internal.document;

import org.junit.Before;
import org.junit.Test;
import org.modeshape.schematic.document.Array;
import org.modeshape.schematic.document.Document;
import org.modeshape.schematic.document.Document.Field;

public class BasicDocumentTest {
    private BasicDocument doc;

    @Before
    public void beforeTest() {
        doc = new BasicDocument();
        doc.put("foo", "value for foo");
        doc.put("bar", "value for bar");
        doc.put("baz", new BasicDocument("key1", "value1", "key2", "value2"));
        doc.put("bom", new BasicArray("v1", "v2", "v3", new BasicDocument("v4-Key", "v4-value")));
    }

    @Test
    public void shouldBeCloneable() {
        assertEquals(doc, doc.clone());
    }

    protected void assertEquals( Document doc1,
                                 Document doc2 ) {
        assert doc1 != doc2;
        assert doc1.equals(doc2);
        for (Field field : doc1.fields()) {
            Object value2 = doc2.get(field.getName());
            assert value2.equals(field.getValue());
            if (value2 instanceof Array) {
                assert value2 != field.getValue();
                assertEquals((Array)field.getValue(), (Array)value2);
            } else if (value2 instanceof Document) {
                assert value2 != field.getValue();
                assertEquals(field.getValueAsDocument(), (Document)value2);
            } else {
                // The values can actually be the same instances since they're immutable ...
            }
        }
    }

    protected void assertEquals( Array array1,
                                 Array array2 ) {
        assert array1 != array2;
        assert array1.equals(array2);
        for (int i = 0; i != array1.size(); ++i) {
            Object value1 = array1.get(i);
            Object value2 = array2.get(i);
            assert value1.equals(value2);
            if (value2 instanceof Array) {
                assert value1 != value2;
                assertEquals((Array)value1, (Array)value2);
            } else if (value2 instanceof Document) {
                assert value1 != value2;
                assertEquals((Document)value1, (Document)value2);
            } else {
                // The values can actually be the same instances since they're immutable ...
            }
        }
    }
}
