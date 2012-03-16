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
package org.infinispan.schematic.internal;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;
import org.infinispan.Cache;
import org.infinispan.atomic.Delta;
import org.infinispan.atomic.DeltaAware;
import org.infinispan.batch.BatchContainer;
import org.infinispan.context.InvocationContextContainer;
import org.infinispan.marshall.AbstractExternalizer;
import org.infinispan.schematic.SchematicEntry;
import org.infinispan.schematic.document.Binary;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.Document.Field;
import org.infinispan.schematic.document.EditableDocument;
import org.infinispan.schematic.document.Path;
import org.infinispan.schematic.internal.delta.NullDelta;
import org.infinispan.schematic.internal.delta.PutOperation;
import org.infinispan.schematic.internal.delta.RemoveOperation;
import org.infinispan.schematic.internal.document.BasicDocument;
import org.infinispan.schematic.internal.document.DocumentEditor;
import org.infinispan.schematic.internal.document.MutableDocument;
import org.infinispan.schematic.internal.document.ObservableDocumentEditor;
import org.infinispan.schematic.internal.document.Paths;
import org.infinispan.schematic.internal.marshall.Ids;
import org.infinispan.util.Util;

/**
 * The primary implementation of {@link SchematicEntry}.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 * @since 5.1
 * @see org.infinispan.atomic.AtomicHashMap
 * @SerializeWith(SchematicEntryLiteral.Externalizer.class)
 */
public class SchematicEntryLiteral implements SchematicEntry, DeltaAware {

    protected static class FieldPath {
        protected static final Path ROOT = Paths.path();
        protected static final Path METADATA = Paths.path(FieldName.METADATA);
        protected static final Path CONTENT = Paths.path(FieldName.CONTENT);
        protected static final Path ID = Paths.path(FieldName.METADATA, FieldName.ID);
        protected static final Path CONTENT_TYPE = Paths.path(FieldName.METADATA, FieldName.CONTENT_TYPE);
    }

    /**
     * Construction only allowed through this factory method. This factory is intended for use internally by the CacheDelegate.
     * User code should use {@link SchematicEntryLookup#getSchematicValue(Cache, String)}.
     * 
     * @param cache underlying cache
     * @param key key under which the schematic value exists
     * @return the schematic entry
     */
    public static SchematicEntry newInstance( Cache<String, SchematicEntry> cache,
                                              String key ) {
        SchematicEntry value = new SchematicEntryLiteral(key);
        SchematicEntry oldValue = cache.putIfAbsent(key, value);
        if (oldValue != null) value = oldValue;
        return value;
    }

    private MutableDocument value;
    private SchematicEntryDelta delta = null;
    private volatile SchematicEntryProxy proxy;
    volatile boolean copied = false;
    volatile boolean removed = false;

    public SchematicEntryLiteral() {
        Document metadata = new BasicDocument();
        value = new BasicDocument(FieldName.METADATA, metadata);
    }

    public SchematicEntryLiteral( String key ) {
        Document metadata = new BasicDocument(FieldName.ID, key);
        value = new BasicDocument(FieldName.METADATA, metadata);
    }

    public SchematicEntryLiteral( String key,
                                  boolean isCopy ) {
        this(key);
        this.copied = true;
    }

    protected SchematicEntryLiteral( MutableDocument document ) {
        this.value = document;
    }

    public SchematicEntryLiteral copyForWrite() {
        try {
            SchematicEntryLiteral clone = (SchematicEntryLiteral)super.clone();
            clone.proxy = proxy;
            clone.copied = true;
            return clone;
        } catch (CloneNotSupportedException e) {
            // should never happen!!
            throw new RuntimeException(e);
        }
    }

    protected final MutableDocument data() {
        return value;
    }

    @Override
    public String toString() {
        return "SchematicValueImpl" + value;
    }

    /**
     * Initializes the delta instance to start recording changes.
     */
    public void initForWriting() {
        delta = new SchematicEntryDelta();
    }

    /**
     * Builds a thread-safe proxy for this instance so that concurrent reads are isolated from writes.
     * 
     * @param cache the cache
     * @param mapKey the key
     * @param batchContainer the container
     * @param icc the invocation context container
     * @return an instance of {@link SchematicEntryProxy}
     */
    public SchematicEntry getProxy( Cache<String, SchematicEntry> cache,
                                    String mapKey,
                                    BatchContainer batchContainer,
                                    InvocationContextContainer icc ) {
        // construct the proxy lazily
        if (proxy == null) { // DCL is OK here since proxy is volatile (and we live in a post-JDK 5 world)
            synchronized (this) {
                if (proxy == null) proxy = new SchematicEntryProxy(cache, mapKey, batchContainer, icc);
            }
        }
        return proxy;
    }

    public void markRemoved( boolean b ) {
        removed = b;
    }

    @Override
    public Delta delta() {
        Delta toReturn = delta == null ? NullDelta.INSTANCE : delta;
        delta = null; // reset
        return toReturn;
    }

    protected SchematicEntryDelta getDelta() {
        if (delta == null) delta = new SchematicEntryDelta();
        return delta;
    }

    @Override
    public void commit() {
        copied = false;
        delta = null;
    }

    @Override
    public Document getMetadata() {
        return value.getDocument(FieldName.METADATA);
    }

    protected MutableDocument mutableMetadata() {
        return (MutableDocument)getMetadata();
    }

    @Override
    public String getContentType() {
        return getMetadata().getString(FieldName.CONTENT_TYPE);
    }

    @Override
    public Object getContent() {
        return value.getDocument(FieldName.CONTENT);
    }

    @Override
    public Document getContentAsDocument() {
        return value.getDocument(FieldName.CONTENT);
    }

    @Override
    public Binary getContentAsBinary() {
        return value.getBinary(FieldName.CONTENT);
    }

    @Override
    public boolean hasDocumentContent() {
        return getContentAsDocument() != null;
    }

    @Override
    public boolean hasBinaryContent() {
        return getContentAsBinary() != null;
    }

    protected Object setContent( Object content ) {
        assert content != null;
        Object existing = this.value.put(FieldName.CONTENT, content);
        if (existing != null) {
            getDelta().addOperation(new PutOperation(FieldPath.ROOT, FieldName.CONTENT, existing, content));
        } else {
            getDelta().addOperation(new RemoveOperation(FieldPath.ROOT, FieldName.CONTENT, content));
        }
        return existing;
    }

    protected void setMetadata( Document metadata,
                                String defaultContentType ) {
        if (metadata != null) {
            if (metadata instanceof EditableDocument) metadata = ((EditableDocument)metadata).unwrap();

            // Copy all the metadata into the entry's metadata ...
            Document existingMetadata = getMetadata();
            MutableDocument newMetadata = new BasicDocument(metadata.size() + 1);
            newMetadata.put(FieldName.ID, existingMetadata.get(FieldName.ID));
            for (Field field : metadata.fields()) {
                String fieldName = field.getName();
                if (fieldName.equals(FieldName.ID)) continue;
                newMetadata.put(fieldName, field.getValue());
            }

            // Make sure the metadata has the content type
            if (newMetadata.getString(FieldName.CONTENT_TYPE) == null) {
                newMetadata.put(FieldName.CONTENT_TYPE, defaultContentType);
            }

            // Now record the change ...
            value.put(FieldName.METADATA, newMetadata);
            PutOperation op = new PutOperation(FieldPath.ROOT, FieldName.METADATA, existingMetadata, newMetadata);
            getDelta().addOperation(op);
        }
    }

    @Override
    public void setContent( Document content,
                            Document metadata,
                            String defaultContentType ) {
        if (content instanceof EditableDocument) content = ((EditableDocument)content).unwrap();
        setContent(content);
        setMetadata(metadata, defaultContentType);
    }

    @Override
    public void setContent( Binary content,
                            Document metadata,
                            String defaultContentType ) {
        setContent(content);
        setMetadata(metadata, defaultContentType);
    }

    @Override
    public EditableDocument editDocumentContent() {
        Document doc = getContentAsDocument();
        if (doc instanceof MutableDocument) {
            MutableDocument mutable = (MutableDocument)doc;
            return new ObservableDocumentEditor(mutable, FieldPath.CONTENT, getDelta(), null);
        }
        if (doc instanceof DocumentEditor) {
            return (DocumentEditor)doc;
        }
        return null;
    }

    @Override
    public EditableDocument editMetadata() {
        return new ObservableDocumentEditor(mutableMetadata(), FieldPath.METADATA, getDelta(), null);
    }

    /**
     * The {@link org.infinispan.marshall.Externalizer Externalizer} for {@link SchematicEntryLiteral} instances.
     */
    public static class Externalizer extends AbstractExternalizer<SchematicEntryLiteral> {
        /** The serialVersionUID */
        private static final long serialVersionUID = 1L;

        @Override
        public void writeObject( ObjectOutput output,
                                 SchematicEntryLiteral literal ) throws IOException {
            output.writeObject(literal.data());
        }

        @Override
        public SchematicEntryLiteral readObject( ObjectInput input ) throws IOException, ClassNotFoundException {
            MutableDocument doc = (MutableDocument)input.readObject();
            return new SchematicEntryLiteral(doc);
        }

        @Override
        public Integer getId() {
            return Ids.SCHEMATIC_VALUE_LITERAL;
        }

        @SuppressWarnings( "unchecked" )
        @Override
        public Set<Class<? extends SchematicEntryLiteral>> getTypeClasses() {
            return Util.<Class<? extends SchematicEntryLiteral>>asSet(SchematicEntryLiteral.class);
        }
    }
}
