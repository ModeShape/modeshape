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

import java.util.Collection;
import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.schematic.SchematicDb;
import org.infinispan.schematic.SchematicEntry;
import org.infinispan.schematic.SchematicEntry.FieldName;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableDocument;
import org.infinispan.schematic.internal.document.DocumentEditor;
import org.infinispan.schematic.internal.document.MutableDocument;

public class CacheSchematicDb implements SchematicDb {

    private final String name;
    private final AdvancedCache<String, SchematicEntry> store;

    public CacheSchematicDb( AdvancedCache<String, SchematicEntry> store ) {
        this.store = store;
        this.name = store.getName();
    }

    @Override
    public void start() {
        this.store.start();
    }

    @Override
    public void stop() {
        this.store.stop();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Cache<String, SchematicEntry> getCache() {
        return store;
    }

    @Override
    public SchematicEntry get( String key ) {
        return store.get(key);
    }

    @Override
    public boolean containsKey( String key ) {
        return store.containsKey(key);
    }

    @Override
    public SchematicEntry put( String key,
                               Document document ) {
        SchematicEntry newEntry = new SchematicEntryLiteral(key, document);
        SchematicEntry oldValue = store.put(key, newEntry);
        return oldValue != null ? removedResult(key, oldValue) : null;
    }

    protected SchematicEntry removedResult( String key,
                                            SchematicEntry entry ) {
        SchematicEntryLiteral literal = (SchematicEntryLiteral)entry;
        literal.markRemoved(true);
        return literal;
    }

    @Override
    public SchematicEntry put( Document entryDocument ) {
        Document metadata = entryDocument.getDocument(FieldName.METADATA);
        Object content = entryDocument.get(FieldName.CONTENT);
        if (metadata == null || content == null) {
            throw new IllegalArgumentException("The supplied document is not of the required format");
        }
        String key = metadata.getString(FieldName.ID);
        if (key == null) {
            throw new IllegalArgumentException("The supplied document is not of the required format");
        }
        SchematicEntry newEntry = null;
        if (content instanceof Document) {
            newEntry = new SchematicEntryLiteral(metadata, (Document)content);
        }
        SchematicEntry oldValue = store.put(key, newEntry);
        return oldValue != null ? removedResult(key, oldValue) : null;
    }

    @Override
    public SchematicEntry putIfAbsent( String key,
                                       Document document ) {
        SchematicEntryLiteral newEntry = new SchematicEntryLiteral(key, document);
        SchematicEntry existingEntry = store.putIfAbsent(key, newEntry);
        return existingEntry;
    }

    @Override
    public SchematicEntry replace( String key,
                                   Document document ) {
        SchematicEntryLiteral newEntry = new SchematicEntryLiteral(key, document);
        return removedResult(key, store.replace(key, newEntry));
    }

    @Override
    public SchematicEntry remove( String key ) {
        SchematicEntry existing = store.remove(key);
        return existing == null ? null : removedResult(key, existing);
    }

    @Override
    public EditableDocument editContent( String key,
                                         boolean createIfMissing ) {
        // Get the literal ...
        SchematicEntryLiteral literal = (SchematicEntryLiteral)store.get(key);
        if (literal == null) {
            if (!createIfMissing) return null;
            literal = new SchematicEntryLiteral(key);
            store.put(key, literal);
            return new DocumentEditor((MutableDocument)literal.getContent());
        }
        // this makes a copy and puts the new copy into the store ...
        return literal.edit(store);
    }

    @Override
    public boolean lock( Collection<String> keys ) {
        return store.lock(keys);
    }

    @Override
    public boolean lock( String key ) {
        return store.lock(key);
    }
}
