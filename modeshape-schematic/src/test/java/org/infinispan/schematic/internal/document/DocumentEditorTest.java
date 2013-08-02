/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * ModeShape is distributed in the hope that it will be useful,
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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.io.InputStream;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableDocument;
import org.infinispan.schematic.document.Json;
import org.junit.Test;

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
