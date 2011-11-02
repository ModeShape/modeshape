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
package org.infinispan.schematic;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.infinispan.Cache;
import org.infinispan.config.GlobalConfiguration;
import org.infinispan.manager.CacheContainer;
import org.infinispan.marshall.AbstractExternalizer;
import org.infinispan.marshall.AdvancedExternalizer;
import org.infinispan.marshall.Externalizer;
import org.infinispan.schematic.document.Binary;
import org.infinispan.schematic.document.Changes;
import org.infinispan.schematic.document.Code;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableArray;
import org.infinispan.schematic.document.EditableDocument;
import org.infinispan.schematic.document.Editor;
import org.infinispan.schematic.document.MaxKey;
import org.infinispan.schematic.document.MinKey;
import org.infinispan.schematic.document.Null;
import org.infinispan.schematic.document.ObjectId;
import org.infinispan.schematic.document.Symbol;
import org.infinispan.schematic.document.Timestamp;
import org.infinispan.schematic.internal.CacheSchematicDb;
import org.infinispan.schematic.internal.InMemorySchemaLibrary;
import org.infinispan.schematic.internal.SchematicEntryLiteral;
import org.infinispan.schematic.internal.delta.AddValueIfAbsentOperation;
import org.infinispan.schematic.internal.delta.AddValueOperation;
import org.infinispan.schematic.internal.delta.ClearOperation;
import org.infinispan.schematic.internal.delta.DocumentObserver;
import org.infinispan.schematic.internal.delta.Operation;
import org.infinispan.schematic.internal.delta.PutOperation;
import org.infinispan.schematic.internal.delta.RemoveAllValuesOperation;
import org.infinispan.schematic.internal.delta.RemoveAtIndexOperation;
import org.infinispan.schematic.internal.delta.RemoveOperation;
import org.infinispan.schematic.internal.delta.RemoveValueOperation;
import org.infinispan.schematic.internal.delta.RetainAllValuesOperation;
import org.infinispan.schematic.internal.delta.SetValueOperation;
import org.infinispan.schematic.internal.document.ArrayEditor;
import org.infinispan.schematic.internal.document.BasicArray;
import org.infinispan.schematic.internal.document.BasicDocument;
import org.infinispan.schematic.internal.document.DefaultDocumentValueFactory;
import org.infinispan.schematic.internal.document.DocumentEditor;
import org.infinispan.schematic.internal.document.DocumentExternalizer;
import org.infinispan.schematic.internal.document.DocumentValueFactory;
import org.infinispan.schematic.internal.document.MutableArray.Entry;
import org.infinispan.schematic.internal.document.MutableDocument;
import org.infinispan.schematic.internal.document.ObservableDocumentEditor;
import org.infinispan.schematic.internal.document.Paths;
import org.infinispan.schematic.internal.marshall.Ids;
import org.infinispan.util.Util;

public class Schematic {

    protected static DocumentValueFactory DEFAULT_FACTORY = DefaultDocumentValueFactory.INSTANCE;

    public static interface ContentTypes {
        public static final String BINARY = "application/octet-stream";
        public static final String JSON = "application/json";
        public static final String BSON = "application/bson";
        public static final String JSON_SCHEMA = "application/schema+json";
    }

    /**
     * Get the {@link SchematicDb} instance given the cache name and container.
     * 
     * @param cacheContainer the container for the named cache; may not be null
     * @param cacheName the name of the cache; may not be null
     * @return the schematic database instance; never null
     */
    public static SchematicDb get( CacheContainer cacheContainer,
                                   String cacheName ) {
        Cache<String, SchematicEntry> cache = cacheContainer.getCache(cacheName);
        return new CacheSchematicDb(cache);
    }

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

    /**
     * Obtain an editor for the supplied document. The editor allows the caller to make changes to the document and to obtain
     * these changes as a {@link Changes serializable memento} that can be applied to another document.
     * 
     * @param document the document to be edited
     * @param clone true if the editor should operate against a clone of the document, or false if it should operate against the
     *        supplied document
     * @return the editor for the document
     */
    public static Editor editDocument( Document document,
                                       boolean clone ) {
        if (clone) {
            document = document.clone();
        }
        final List<Operation> operations = new LinkedList<Operation>();
        final DocumentObserver observer = new DocumentObserver() {
            @Override
            public void addOperation( Operation o ) {
                if (o != null) {
                    operations.add(o);
                }
            }
        };
        return new EditorImpl((MutableDocument)document, observer, operations);
    }

    protected static class EditorImpl extends ObservableDocumentEditor implements Editor {
        private static final long serialVersionUID = 1L;
        private final List<Operation> operations;

        public EditorImpl( MutableDocument document,
                           DocumentObserver observer,
                           List<Operation> operations ) {
            super(document, Paths.rootPath(), observer, null);
            this.operations = operations;
        }

        @Override
        public Changes getChanges() {
            return new DocumentChanges(operations);
        }

        @Override
        public void apply( Changes changes ) {
            apply(changes, null);
        }

        private final static Entry newEntry( int index,
                                             Object value ) {
            return new BasicArray.BasicEntry(index, value);
        }

        @Override
        public void apply( Changes changes,
                           Observer observer ) {
            if (changes.isEmpty()) {
                return;
            }
            MutableDocument mutable = asMutableDocument();
            for (Operation operation : (DocumentChanges)changes) {
                operation.replay(mutable);
                if (observer != null) {
                    if (operation instanceof SetValueOperation) {
                        SetValueOperation op = (SetValueOperation)operation;
                        observer.setArrayValue(op.getPath(), newEntry(op.getIndex(), op.getValue()));
                    } else if (operation instanceof AddValueOperation) {
                        AddValueOperation op = (AddValueOperation)operation;
                        if (op.getActualIndex() != -1) {
                            observer.addArrayValue(op.getPath(), newEntry(op.getActualIndex(), op.getValue()));
                        }
                    } else if (operation instanceof AddValueIfAbsentOperation) {
                        AddValueIfAbsentOperation op = (AddValueIfAbsentOperation)operation;
                        if (op.isAdded()) {
                            observer.addArrayValue(op.getPath(), newEntry(op.getIndex(), op.getValue()));
                        }
                    } else if (operation instanceof RemoveValueOperation) {
                        RemoveValueOperation op = (RemoveValueOperation)operation;
                        if (op.getActualIndex() != -1) {
                            observer.removeArrayValue(op.getPath(), newEntry(op.getActualIndex(), op.getRemovedValue()));
                        }
                    } else if (operation instanceof RemoveAtIndexOperation) {
                        RemoveAtIndexOperation op = (RemoveAtIndexOperation)operation;
                        observer.removeArrayValue(op.getPath(), newEntry(op.getIndex(), op.getRemovedValue()));
                    } else if (operation instanceof RetainAllValuesOperation) {
                        RetainAllValuesOperation op = (RetainAllValuesOperation)operation;
                        for (Entry entry : op.getRemovedEntries()) {
                            observer.removeArrayValue(op.getPath(), entry);
                        }
                    } else if (operation instanceof RemoveAllValuesOperation) {
                        RemoveAllValuesOperation op = (RemoveAllValuesOperation)operation;
                        for (Entry entry : op.getRemovedEntries()) {
                            observer.removeArrayValue(op.getPath(), entry);
                        }
                    } else if (operation instanceof ClearOperation) {
                        ClearOperation op = (ClearOperation)operation;
                        observer.clear(op.getPath());
                    } else if (operation instanceof PutOperation) {
                        PutOperation op = (PutOperation)operation;
                        observer.put(op.getPath(), op.getNewValue());
                    } else if (operation instanceof RemoveOperation) {
                        RemoveOperation op = (RemoveOperation)operation;
                        if (op.isRemoved()) {
                            observer.remove(op.getPath());
                        }
                    }
                }
            }
        }
    }

    protected static class DocumentChanges implements Changes, Iterable<Operation> {

        private final List<Operation> operations;

        protected DocumentChanges( List<Operation> operations ) {
            this.operations = operations;
        }

        @Override
        public Iterator<Operation> iterator() {
            return operations.iterator();
        }

        @Override
        public boolean isEmpty() {
            return operations.isEmpty();
        }

        public static class Externalizer extends AbstractExternalizer<DocumentChanges> {
            /** The serialVersionUID */
            private static final long serialVersionUID = 1L;

            @SuppressWarnings( "synthetic-access" )
            @Override
            public void writeObject( ObjectOutput output,
                                     DocumentChanges changes ) throws IOException {
                output.writeObject(changes.operations);
            }

            @Override
            public DocumentChanges readObject( ObjectInput input ) throws IOException, ClassNotFoundException {
                @SuppressWarnings( "unchecked" )
                List<Operation> operations = (List<Operation>)input.readObject();
                return new DocumentChanges(operations);
            }

            @Override
            public Integer getId() {
                return Ids.SCHEMATIC_DOCUMENT_CHANGES;
            }

            @SuppressWarnings( "unchecked" )
            @Override
            public Set<Class<? extends DocumentChanges>> getTypeClasses() {
                return Util.<Class<? extends DocumentChanges>>asSet(DocumentChanges.class);
            }
        }
    }

    /**
     * Create an in-memory schema library.
     * 
     * @return the empty, in-memory schema library
     */
    public static SchemaLibrary createSchemaLibrary() {
        return new InMemorySchemaLibrary("In-memory schema library");
    }

    /**
     * Create an in-memory schema library.
     * 
     * @param name the name of the library; may be null if a default name is to be used
     * @return the empty, in-memory schema library
     */
    public static SchemaLibrary createSchemaLibrary( String name ) {
        return new InMemorySchemaLibrary(name != null ? name : "In-memory schema library");
    }

    /**
     * Get the set of {@link Externalizer} implementations that are used by Schematic. These need to be registered with the
     * {@link GlobalConfiguration}:
     * 
     * <pre>
     * GlobalConfiguration config = new GlobalConfiguration();
     * config = config.fluent().serialization().addAdvancedExternalizer(Schematic.externalizers()).build();
     * </pre>
     * 
     * @return the list of externalizer
     */
    @SuppressWarnings( "unchecked" )
    public static AdvancedExternalizer<Object>[] externalizers() {
        return EXTERNALIZERS.toArray(new AdvancedExternalizer[EXTERNALIZERS.size()]);
    }

    public static Set<AdvancedExternalizer<?>> externalizerSet() {
        return EXTERNALIZERS;
    }

    private static final Set<AdvancedExternalizer<?>> EXTERNALIZERS;

    static {
        Set<AdvancedExternalizer<?>> externalizers = new HashSet<AdvancedExternalizer<?>>();

        // SchematicDb values ...
        externalizers.add(new SchematicEntryLiteral.Externalizer());

        // Documents ...
        externalizers.add(new DocumentExternalizer()); // BasicDocument and BasicArray
        externalizers.add(new Binary.Externalizer());
        externalizers.add(new Code.Externalizer()); // both Code and CodeWithScope
        externalizers.add(new MaxKey.Externalizer());
        externalizers.add(new MinKey.Externalizer());
        externalizers.add(new Null.Externalizer());
        externalizers.add(new ObjectId.Externalizer());
        externalizers.add(new Symbol.Externalizer());
        externalizers.add(new Timestamp.Externalizer());
        externalizers.add(new Paths.Externalizer());

        // Operations ...
        externalizers.add(new AddValueIfAbsentOperation.Externalizer());
        externalizers.add(new AddValueOperation.Externalizer());
        externalizers.add(new ClearOperation.Externalizer());
        externalizers.add(new PutOperation.Externalizer());
        externalizers.add(new RemoveAllValuesOperation.Externalizer());
        externalizers.add(new RemoveAtIndexOperation.Externalizer());
        externalizers.add(new RemoveOperation.Externalizer());
        externalizers.add(new RemoveValueOperation.Externalizer());
        externalizers.add(new RetainAllValuesOperation.Externalizer());
        externalizers.add(new SetValueOperation.Externalizer());

        EXTERNALIZERS = Collections.unmodifiableSet(externalizers);
    }
}
