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
import java.util.Collection;
import java.util.HashSet;
import org.infinispan.schematic.internal.SchematicDelta;
import org.infinispan.schematic.internal.SchematicEntryDelta;
import org.junit.Before;
import org.junit.Test;

public class OperationExternalizerTest extends AbstractExternalizerTest {

    private SchematicDelta delta;
    private ObservableDocumentEditor docEditor;
    private MutableDocument doc;
    private DocumentValueFactory valueFactory = new DefaultDocumentValueFactory();
    private boolean print;

    @Before
    public void beforeEach() {
        doc = new BasicDocument();
        delta = new SchematicEntryDelta();
        docEditor = new ObservableDocumentEditor(doc, Paths.rootPath(), delta, valueFactory);
        print = false;
    }

    protected void resetEditor() {
        delta = new SchematicEntryDelta();
        docEditor = new ObservableDocumentEditor(doc, Paths.rootPath(), delta, valueFactory);
    }

    protected Collection<?> values( Object... values ) {
        Collection<Object> result = new HashSet<Object>();
        for (Object value : values) {
            result.add(value);
        }
        return result;
    }

    @Test
    public void shouldRoundTripSettingStringValue() throws Exception {
        docEditor.setString("foo", "value");
        assertOperationsAreMarshallable();
    }

    @Test
    public void shouldRoundTripSettingDocumentValue() throws Exception {
        docEditor.setDocument("foo").setString("nested", "value");
        assertOperationsAreMarshallable();
    }

    @Test
    public void shouldRoundTripRemovingDocumentField() throws Exception {
        // print = true;
        docEditor.setDocument("foo").setString("nested", "value").setNumber("nested2", 13);
        assertOperationsAreMarshallable();
        docEditor.getDocument("foo").remove("nested");
        docEditor.getDocument("foo").remove("non-existant");
        assertOperationsAreMarshallable();
    }

    @Test
    public void shouldRoundTripSettingArray() throws Exception {
        docEditor.setArray("foo", new BasicArray("value1", "value2"));
        assertOperationsAreMarshallable();
    }

    @Test
    public void shouldRoundTripAddingValueToArray() throws Exception {
        docEditor.setArray("foo", new BasicArray("value1", "value2"));
        docEditor.getArray("foo").addNumber(3L);
        assertOperationsAreMarshallable();
        assertThat(doc.getArray("foo").size(), is(3));
    }

    @Test
    public void shouldRoundTripAddingValueMultipleTimesToArray() throws Exception {
        docEditor.setArray("foo", new BasicArray("value1", "value2"));
        docEditor.getArray("foo").addNumber(3L).addNumber(3L).addNumber(5L).addNumber(3L);
        assertOperationsAreMarshallable();
        assertThat(doc.getArray("foo").size(), is(6));
    }

    @Test
    public void shouldRoundTripAddingValueToArrayIfValueAbsent() throws Exception {
        docEditor.setArray("foo", new BasicArray("value1", "value2"));
        docEditor.getArray("foo").addNumber(3L).addNumberIfAbsent(3L).addNumberIfAbsent(5L);
        assertOperationsAreMarshallable();
        assertThat(doc.getArray("foo").size(), is(4));
    }

    @Test
    public void shouldRoundTripRetainingValuesInArray() throws Exception {
        // print = true;
        docEditor.setArray("foo", new BasicArray("value1", "value2", "value3", 4));
        assertOperationsAreMarshallable();
        assertThat(doc.getArray("foo").size(), is(4));
        docEditor.getArray("foo").retainAll(values("value1", 4));
        assertOperationsAreMarshallable();
        assertThat(doc.getArray("foo").size(), is(2));
        assertThat(doc.getArray("foo").get(0), is((Object)"value1"));
        assertThat(doc.getArray("foo").get(1), is((Object)4));
    }

    @Test
    public void shouldRoundTripClearingArray() throws Exception {
        docEditor.setArray("foo", new BasicArray("value1", "value2"));
        docEditor.getArray("foo").addNumber(3L).addNumberIfAbsent(3L).addNumberIfAbsent(5L);
        assertOperationsAreMarshallable();
        assertThat(doc.getArray("foo").size(), is(4));
        docEditor.getArray("foo").clear();
        assertOperationsAreMarshallable();
    }

    @Test
    public void shouldRoundTripSettingArrayUsingArrayEditor() throws Exception {
        docEditor.setArray("foo").addString("value1").addString("value2");
        assertOperationsAreMarshallable();
    }

    protected void assertOperationsAreMarshallable() throws Exception {
        if (print) System.out.println("delta: " + delta);
        byte[] bytes = marshall(delta);
        SchematicDelta newDelta = (SchematicDelta)unmarshall(bytes);
        assertThat(newDelta, is(delta));
        if (print) System.out.println("document: " + doc);
        resetEditor();
    }

}
