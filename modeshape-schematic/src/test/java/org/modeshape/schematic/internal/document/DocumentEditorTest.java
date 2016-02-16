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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.io.InputStream;
import org.junit.Test;
import org.modeshape.schematic.document.Document;
import org.modeshape.schematic.document.EditableDocument;
import org.modeshape.schematic.document.Json;

public class DocumentEditorTest {

    protected InputStream stream( String path ) {
        return this.getClass().getClassLoader().getResourceAsStream(path);
    }

    @Test
    public void shouldMergeTwoDocuments() throws Exception {
        Document doc1 = Json.read(stream("json/merge-1.json"));
        Document doc2 = Json.read(stream("json/merge-2.json"));
        Document doc3 = Json.read(stream("json/merge-3.json"));
        EditableDocument editor = new DocumentEditor((MutableDocument)doc1);
        assertThat(Json.writePretty(editor).equals(Json.writePretty(doc3)), is(false));
        editor.merge(doc2);
        assertThat(Json.writePretty(editor).equals(Json.writePretty(doc3)), is(true));
    }
}
