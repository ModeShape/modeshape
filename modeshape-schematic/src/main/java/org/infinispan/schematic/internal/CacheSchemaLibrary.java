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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.infinispan.Cache;
import org.infinispan.lifecycle.Lifecycle;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;
import org.infinispan.schematic.SchemaLibrary;
import org.infinispan.schematic.Schematic;
import org.infinispan.schematic.SchematicEntry;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.internal.document.Paths;
import org.infinispan.schematic.internal.schema.DocumentTransformer;
import org.infinispan.schematic.internal.schema.SchemaDocument;
import org.infinispan.schematic.internal.schema.SchemaDocumentCache;
import org.infinispan.schematic.internal.schema.ValidationResult;
import org.infinispan.util.concurrent.FutureListener;
import org.infinispan.util.concurrent.NotifyingFuture;

public class CacheSchemaLibrary implements SchemaLibrary, Lifecycle {

    private final String name;
    private final Cache<String, SchematicEntry> store;
    private final String defaultContentType;
    private final SchemaDocumentCache schemaDocuments;
    private final SchemaListener listener;

    public CacheSchemaLibrary( Cache<String, SchematicEntry> schemaStore ) {
        this.name = schemaStore.getName();
        this.store = schemaStore;
        this.defaultContentType = Schematic.ContentTypes.JSON_SCHEMA;
        this.schemaDocuments = new SchemaDocumentCache(this, null);
        this.listener = new SchemaListener(this.schemaDocuments);
        this.store.addListener(this.listener);
    }

    @Override
    public void start() {
        this.store.start();
    }

    @Override
    public void stop() {
        this.store.removeListener(this.listener);
        this.store.stop();
    }

    protected Cache<String, SchematicEntry> store() {
        return store;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Document get( String key ) {
        return document(store.get(key));
    }

    @Override
    public Document put( String key,
                         Document document ) {
        SchematicEntryLiteral newEntry = new SchematicEntryLiteral(key, document, null, defaultContentType);
        return document(store.put(key, newEntry));
    }

    @Override
    public Document putIfAbsent( String key,
                                 Document document ) {
        SchematicEntryLiteral newEntry = new SchematicEntryLiteral(key, document, null, defaultContentType);
        return document(store.putIfAbsent(key, newEntry));
    }

    @Override
    public Document replace( String key,
                             Document document ) {
        SchematicEntryLiteral newEntry = new SchematicEntryLiteral(key, document, null, defaultContentType);
        return document(store.replace(key, newEntry));
    }

    @Override
    public Document remove( String key ) {
        return document(store.remove(key));
    }

    @Override
    public NotifyingFuture<Document> getAsync( String key ) {
        return future(store.getAsync(key));
    }

    @Override
    public NotifyingFuture<Document> putAsync( String key,
                                               Document document ) {
        SchematicEntryLiteral newEntry = new SchematicEntryLiteral(key, document, null, defaultContentType);
        return future(store.putAsync(key, newEntry));
    }

    @Override
    public NotifyingFuture<Document> putIfAbsentAsync( String key,
                                                       Document document ) {
        SchematicEntryLiteral newEntry = new SchematicEntryLiteral(key, document, null, defaultContentType);
        return future(store.putIfAbsentAsync(key, newEntry));
    }

    @Override
    public NotifyingFuture<Document> replaceAsync( String key,
                                                   Document document ) {
        SchematicEntryLiteral newEntry = new SchematicEntryLiteral(key, document, null, defaultContentType);
        return future(store.replaceAsync(key, newEntry));
    }

    @Override
    public NotifyingFuture<Document> removeAsync( String key ) {
        return future(store.removeAsync(key));
    }

    @Override
    public Results validate( Document document,
                             String schemaUri ) {
        ValidationResult result = new ValidationResult();
        SchemaDocument schema = schemaDocuments.get(schemaUri, result);
        if (schema != null) {
            schema.getValidator().validate(null, null, document, Paths.rootPath(), result, schemaDocuments);
        }
        return result;
    }

    @Override
    public Document convertValues( Document document,
                                   Results results ) {
        return DocumentTransformer.convertValuesWithMismatchedTypes(document, results);
    }

    @Override
    public Document convertValues( Document document,
                                   String schemaUri ) {
        Results results = validate(document, schemaUri);
        return convertValues(document, results);
    }

    protected Document document( SchematicEntry entry ) {
        return entry != null ? entry.getContentAsDocument() : null;
    }

    protected NotifyingFuture<Document> future( final NotifyingFuture<SchematicEntry> original ) {
        return new WrappedFuture(original);
    }

    protected class WrappedFuture implements NotifyingFuture<Document> {
        private final NotifyingFuture<SchematicEntry> original;

        protected WrappedFuture( NotifyingFuture<SchematicEntry> original ) {
            this.original = original;
        }

        @Override
        public NotifyingFuture<Document> attachListener( final FutureListener<Document> listener ) {
            original.attachListener(new FutureListener<SchematicEntry>() {
                @Override
                public void futureDone( Future<SchematicEntry> future ) {
                    listener.futureDone(WrappedFuture.this);
                }
            });
            return this;
        }

        @Override
        public boolean cancel( boolean mayInterruptIfRunning ) {
            return original.cancel(mayInterruptIfRunning);
        }

        @Override
        public Document get() throws InterruptedException, ExecutionException {
            SchematicEntry result = original.get();
            return result != null ? result.getContentAsDocument() : null;
        }

        @Override
        public Document get( long timeout,
                             TimeUnit unit ) throws InterruptedException, ExecutionException, TimeoutException {
            SchematicEntry result = original.get(timeout, unit);
            return result != null ? result.getContentAsDocument() : null;
        }

        @Override
        public boolean isCancelled() {
            return original.isCancelled();
        }

        @Override
        public boolean isDone() {
            return original.isDone();
        }
    }

    @Listener
    public class SchemaListener {
        private final SchemaDocumentCache cachedSchemas;

        protected SchemaListener( SchemaDocumentCache cachedSchemas ) {
            this.cachedSchemas = cachedSchemas;
        }

        @CacheEntryModified
        public void schemaChanged( CacheEntryModifiedEvent<String, SchematicEntry> event ) {
            String key = event.getKey();
            this.cachedSchemas.remove(key);
        }

        @CacheEntryRemoved
        public void schemaChanged( CacheEntryRemovedEvent<String, SchematicEntry> event ) {
            String key = event.getKey();
            this.cachedSchemas.remove(key);
        }
    }

}
