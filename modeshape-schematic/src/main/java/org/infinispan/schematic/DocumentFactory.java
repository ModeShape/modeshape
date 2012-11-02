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

package org.infinispan.schematic;

import java.util.Collection;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableArray;
import org.infinispan.schematic.document.EditableDocument;
import org.infinispan.schematic.internal.document.ArrayEditor;
import org.infinispan.schematic.internal.document.BasicArray;
import org.infinispan.schematic.internal.document.BasicDocument;
import org.infinispan.schematic.internal.document.DefaultDocumentValueFactory;
import org.infinispan.schematic.internal.document.DocumentEditor;
import org.infinispan.schematic.internal.document.DocumentValueFactory;

/**
 * Factory class that creates {@link EditableDocument} instances
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class DocumentFactory {

    protected static DocumentValueFactory DEFAULT_FACTORY = DefaultDocumentValueFactory.INSTANCE;

    /**
     * Create a new editable document that is a copy of the supplied document.
     *
     * @param original the original document
     * @return the editable document; never null
     */
    public static EditableDocument newDocument( Document original ) {
        BasicDocument newDoc = new BasicDocument();
        newDoc.putAll(original);
        return new DocumentEditor(newDoc, DEFAULT_FACTORY);
    }

    /**
     * Create a new editable document that can be used as a new document entry in a SchematicDb or as nested documents for other
     * documents.
     *
     * @return the editable document; never null
     */
    public static EditableDocument newDocument() {
        return new DocumentEditor(new BasicDocument(), DEFAULT_FACTORY);
    }

    /**
     * Create a new editable document, initialized with a single field, that can be used as a new document entry in a SchematicDb
     * or as nested documents for other documents.
     *
     * @param name the name of the initial field in the resulting document; if null, the field will not be added to the returned
     *        document
     * @param value the value of the initial field in the resulting document
     * @return the editable document; never null
     */
    public static EditableDocument newDocument( String name,
                                                Object value ) {
        return new DocumentEditor(new BasicDocument(name, value), DEFAULT_FACTORY);
    }

    /**
     * Create a new editable document, initialized with two fields, that can be used as a new document entry in a SchematicDb or
     * as nested documents for other documents.
     *
     * @param name1 the name of the first field in the resulting document; if null, the field will not be added to the returned
     *        document
     * @param value1 the value of the first field in the resulting document
     * @param name2 the name of the second field in the resulting document; if null, the field will not be added to the returned
     *        document
     * @param value2 the value of the second field in the resulting document
     * @return the editable document; never null
     */
    public static EditableDocument newDocument( String name1,
                                                Object value1,
                                                String name2,
                                                Object value2 ) {
        return new DocumentEditor(new BasicDocument(name1, value1, name2, value2), DEFAULT_FACTORY);
    }

    /**
     * Create a new editable document, initialized with three fields, that can be used as a new document entry in a SchematicDb or
     * as nested documents for other documents.
     *
     * @param name1 the name of the first field in the resulting document; if null, the field will not be added to the returned
     *        document
     * @param value1 the value of the first field in the resulting document
     * @param name2 the name of the second field in the resulting document; if null, the field will not be added to the returned
     *        document
     * @param value2 the value of the second field in the resulting document
     * @param name3 the name of the third field in the resulting document; if null, the field will not be added to the returned
     *        document
     * @param value3 the value of the third field in the resulting document
     * @return the editable document; never null
     */
    public static EditableDocument newDocument( String name1,
                                                Object value1,
                                                String name2,
                                                Object value2,
                                                String name3,
                                                Object value3 ) {
        return new DocumentEditor(new BasicDocument(name1, value1, name2, value2, name3, value3), DEFAULT_FACTORY);
    }

    /**
     * Create a new editable document, initialized with four fields, that can be used as a new document entry in a SchematicDb or
     * as nested documents for other documents.
     *
     * @param name1 the name of the first field in the resulting document; if null, the field will not be added to the returned
     *        document
     * @param value1 the value of the first field in the resulting document
     * @param name2 the name of the second field in the resulting document; if null, the field will not be added to the returned
     *        document
     * @param value2 the value of the second field in the resulting document
     * @param name3 the name of the third field in the resulting document; if null, the field will not be added to the returned
     *        document
     * @param value3 the value of the third field in the resulting document
     * @param name4 the name of the fourth field in the resulting document; if null, the field will not be added to the returned
     *        document
     * @param value4 the value of the fourth field in the resulting document
     * @return the editable document; never null
     */
    public static EditableDocument newDocument( String name1,
                                                Object value1,
                                                String name2,
                                                Object value2,
                                                String name3,
                                                Object value3,
                                                String name4,
                                                Object value4 ) {
        return new DocumentEditor(new BasicDocument(name1, value1, name2, value2, name3, value3, name4, value4), DEFAULT_FACTORY);
    }

    /**
     * Create a new, empty editable array that can be used as a new array value in other documents.
     *
     * @return the editable array; never null
     */
    public static EditableArray newArray() {
        return new ArrayEditor(new BasicArray(), DEFAULT_FACTORY);
    }

    /**
     * Create a new, empty editable array that can be used as a new array value in other documents.
     *
     * @param initialCapacity the initial allocated capacity for the array
     * @return the editable array; never null
     */
    public static EditableArray newArray( int initialCapacity ) {
        return new ArrayEditor(new BasicArray(initialCapacity), DEFAULT_FACTORY);
    }

    /**
     * Create a new editable array that can be used as a new array value in other documents.
     *
     * @param values the initial values for the array
     * @return the editable array; never null
     */
    public static EditableArray newArray( Collection<?> values ) {
        BasicArray array = new BasicArray(values.size());
        array.addAllValues(values);
        return new ArrayEditor(array, DEFAULT_FACTORY);
    }

    /**
     * Create a new editable array that can be used as a new array value in other documents.
     *
     * @param values the initial values for the array
     * @return the editable array; never null
     */
    public static EditableArray newArray( Object... values ) {
        BasicArray array = new BasicArray();
        for (Object value : values) {
            array.addValue(value);
        }
        return new ArrayEditor(array, DEFAULT_FACTORY);
    }
}
