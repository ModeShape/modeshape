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
package org.infinispan.schematic.internal;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.Set;
import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.commons.marshall.SerializeWith;
import org.infinispan.schematic.SchematicEntry;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.Document.Field;
import org.infinispan.schematic.document.EditableDocument;
import org.infinispan.schematic.internal.document.BasicDocument;
import org.infinispan.schematic.internal.document.DocumentEditor;
import org.infinispan.schematic.internal.document.MutableDocument;
import org.infinispan.schematic.internal.marshall.Ids;
import org.infinispan.util.concurrent.TimeoutException;

/**
 * The primary implementation of {@link SchematicEntry}.
 *
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 */
@SerializeWith( SchematicEntryLiteral.Externalizer.class )
public class SchematicEntryLiteral implements SchematicEntry {

    /**
     * Construction only allowed through this factory method. This factory is intended for use internally by the CacheDelegate.
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

    private volatile MutableDocument value;

    public SchematicEntryLiteral( String key ) {
        this(key, new BasicDocument());
    }

    protected SchematicEntryLiteral( MutableDocument document ) {
        this.value = document;
        assert this.value != null;
    }

    public SchematicEntryLiteral( String key,
                                  Document content ) {
        Document metadata = new BasicDocument(FieldName.ID, key);
        value = new BasicDocument(FieldName.METADATA, metadata, FieldName.CONTENT, unwrap(content));
    }

    protected SchematicEntryLiteral( Document metadata,
                                     Document content ) {
        value = new BasicDocument(FieldName.METADATA, unwrap(metadata), FieldName.CONTENT, unwrap(content));
    }

    protected MutableDocument unwrap( Document doc ) {
        if (doc instanceof EditableDocument) {
            doc = ((EditableDocument)doc).unwrap();
        }
        return (MutableDocument)doc;
    }

    protected final String key() {
        return getMetadata().getString(FieldName.ID);
    }

    protected final MutableDocument data() {
        return value;
    }

    protected void setDocument( Document document ) {
        assert this.value != null;
        this.value = unwrap(document);
    }

    @Override
    public String toString() {
        return "SchematicEntryLiteral" + value;
    }

    @Override
    public Document getMetadata() {
        return value.getDocument(FieldName.METADATA);
    }

    protected String getKey() {
        return getMetadata().getString(FieldName.ID);
    }

    @Override
    public Document getContent() {
        return value.getDocument(FieldName.CONTENT);
    }

    protected void setMetadata( Document metadata ) {
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

            // Now record the change ...
            value.put(FieldName.METADATA, newMetadata);
        }
    }

    @Override
    public void setContent( Document content ) {
        this.value.put(FieldName.CONTENT, unwrap(content));
    }

    @Override
    public Document asDocument() {
        return value;
    }

    protected EditableDocument edit( String key,
                                     AdvancedCache<String, SchematicEntry> cache,
                                     AdvancedCache<String, SchematicEntry> lockingCache ) {
        if (lockingCache != null) {
            if (!lockingCache.lock(key)) {
                throw new TimeoutException("Unable to aquire lock on " + key);
            }
        }
        MutableDocument copy = (MutableDocument)value.clone();
        SchematicEntryLiteral newEntry = new SchematicEntryLiteral(copy);
        cache.put(getKey(), newEntry);
        return new DocumentEditor((MutableDocument)newEntry.getContent());
    }

    @Override
    public SchematicEntryLiteral clone() {
        return new SchematicEntryLiteral((MutableDocument)value.clone());
    }

    /**
     * The {@link org.infinispan.commons.marshall.Externalizer} for {@link SchematicEntryLiteral} instances.
     */
    public static final class Externalizer extends SchematicExternalizer<SchematicEntryLiteral> {
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
            return Collections.<Class<? extends SchematicEntryLiteral>>singleton(SchematicEntryLiteral.class);
        }
    }
}
