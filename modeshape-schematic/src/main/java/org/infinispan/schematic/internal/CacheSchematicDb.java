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
import org.infinispan.context.Flag;
import org.infinispan.schematic.SchematicDb;
import org.infinispan.schematic.SchematicEntry;
import org.infinispan.schematic.SchematicEntry.FieldName;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableDocument;
import org.infinispan.schematic.internal.document.DocumentEditor;
import org.infinispan.schematic.internal.document.MutableDocument;
import org.infinispan.transaction.LockingMode;

public class CacheSchematicDb implements SchematicDb {

    private final String name;
    private final AdvancedCache<String, SchematicEntry> store;
    private final AdvancedCache<String, SchematicEntry> storeForWriting;
    private final AdvancedCache<String, SchematicEntry> lockingStore;
    private final boolean explicitLocking;

    public CacheSchematicDb( AdvancedCache<String, SchematicEntry> store ) {
        assert store != null;
        this.store = store;
        this.storeForWriting = store.withFlags(Flag.SKIP_CACHE_LOAD, Flag.SKIP_REMOTE_LOOKUP);
        this.name = store.getName();
        this.explicitLocking = store.getCacheConfiguration().transaction().lockingMode() == LockingMode.PESSIMISTIC;
        if (this.explicitLocking) {
            this.lockingStore = store.withFlags(Flag.FAIL_SILENTLY).getAdvancedCache();
        } else {
            this.lockingStore = store;
        }
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
    public void put( String key,
                     Document document ) {
        SchematicEntry newEntry = new SchematicEntryLiteral(key, document);
        // use storeForWriting because we don't care about the return type - i.e. we're doing a local put
        storeForWriting.put(key, newEntry);
    }

    @Override
    public void put( Document entryDocument ) {
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
        // use storeForWriting because we don't care about the return type - i.e. we're doing a local put
        storeForWriting.put(key, newEntry);
    }

    @Override
    public SchematicEntry putIfAbsent( String key,
                                       Document document ) {
        SchematicEntryLiteral newEntry = new SchematicEntryLiteral(key, document);
        // we can't use storeForWriting, because we care about the return value here 
        return store.putIfAbsent(key, newEntry);
    }

    @Override
    public void replace( String key,
                         Document document ) {
        SchematicEntryLiteral newEntry = new SchematicEntryLiteral(key, document);
        // use storeForWriting because we don't care about the return type - i.e. we're doing a local replace
        storeForWriting.replace(key, newEntry);
    }

    @Override
    public SchematicEntry remove( String key ) {
        // we can't use storeForWriting, because we care about the return value here
        return store.remove(key);
    }

    @Override
    public EditableDocument editContent( String key,
                                         boolean createIfMissing ) {
        return editContent(key, createIfMissing, true);
    }

    @Override
    public EditableDocument editContent( String key,
                                         boolean createIfMissing,
                                         boolean acquireLock ) {
        // Get the literal ...
        SchematicEntryLiteral literal = (SchematicEntryLiteral)store.get(key);
        if (literal == null) {
            if (!createIfMissing) return null;
            literal = new SchematicEntryLiteral(key);
            SchematicEntry existingLiteral = store.putIfAbsent(key, literal);
            MutableDocument content = existingLiteral == null ? (MutableDocument)literal.getContent()
                                                              : (MutableDocument)existingLiteral.getContent();
            return new DocumentEditor(content);
        }
        // this makes a copy and puts the new copy into the store ...
        return literal.edit(key, storeForWriting, shouldAcquireLock(acquireLock));
    }

    private AdvancedCache<String, SchematicEntry> shouldAcquireLock( boolean acquireLockRequested ) {
        return explicitLocking && acquireLockRequested ? lockingStore : null;
    }

    @Override
    public boolean lock( Collection<String> keys ) {
        return !explicitLocking ? true : lockingStore.lock(keys);
    }

    @Override
    public boolean lock( String key ) {
        return !explicitLocking ? true : lockingStore.lock(key);
    }
}
