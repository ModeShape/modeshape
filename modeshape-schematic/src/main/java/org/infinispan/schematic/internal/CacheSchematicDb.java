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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.infinispan.Cache;
import org.infinispan.context.FlagContainer;
import org.infinispan.distexec.mapreduce.Collector;
import org.infinispan.distexec.mapreduce.MapReduceTask;
import org.infinispan.schematic.SchemaLibrary;
import org.infinispan.schematic.SchemaLibrary.Results;
import org.infinispan.schematic.Schematic;
import org.infinispan.schematic.SchematicDb;
import org.infinispan.schematic.SchematicEntry;
import org.infinispan.schematic.SchematicEntry.FieldName;
import org.infinispan.schematic.document.Binary;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.JsonSchema;
import org.infinispan.util.concurrent.FutureListener;
import org.infinispan.util.concurrent.NotifyingFuture;

public class CacheSchematicDb implements SchematicDb {

    private final String name;
    private final Cache<String, SchematicEntry> store;
    private final CacheContext context;
    private final AtomicReference<CacheSchemaLibrary> schemaLibrary = new AtomicReference<CacheSchemaLibrary>();
    private final String schemaCacheName;
    private String defaultContentTypeForDocument;
    private String defaultContentTypeForBinary;
    private String defaultSchemaUri;
    private String description;

    public CacheSchematicDb( Cache<String, SchematicEntry> store ) {
        this.name = store.getName();
        this.store = store;
        String defaultContentTypeForDocs = Schematic.ContentTypes.JSON;
        String defaultContentTypeForBinary = Schematic.ContentTypes.BINARY;
        String defaultSchemaUri = JsonSchema.Version.Latest.CORE_METASCHEMA_URL;
        String description = "";
        String schemaCacheName = store.getName() + "Schemas";

        // Load the database document from the cache ...
        SchematicEntry databaseDocument = store.get("");
        if (databaseDocument != null && databaseDocument.hasDocumentContent()) {
            Document dbDoc = databaseDocument.getContentAsDocument();
            defaultContentTypeForDocs = dbDoc.getString("defaultContentTypeForDocuments", defaultContentTypeForDocs);
            defaultContentTypeForBinary = dbDoc.getString("defaultContentTypeForBinary", defaultContentTypeForBinary);
            defaultSchemaUri = dbDoc.getString("defaultSchemaUri", defaultSchemaUri);
            description = dbDoc.getString("description", description);
            schemaCacheName = dbDoc.getString("schemaCacheName", schemaCacheName);
        }

        this.defaultContentTypeForBinary = defaultContentTypeForBinary;
        this.defaultContentTypeForDocument = defaultContentTypeForDocs;
        this.defaultSchemaUri = defaultSchemaUri;
        this.description = description;
        this.schemaCacheName = schemaCacheName;
        this.context = new CacheContext(store.getAdvancedCache());
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Cache<String, SchematicEntry> getCache() {
        return this.store;
    }

    @Override
    public void start() {
        this.store.start();
    }

    @Override
    public void stop() {
        this.store.stop();
        CacheSchemaLibrary schemaLibrary = schemaLibrary(false);
        if (schemaLibrary != null) schemaLibrary.stop();
    }

    @Override
    public SchemaLibrary getSchemaLibrary() {
        return schemaLibrary(true);
    }

    protected CacheSchemaLibrary schemaLibrary( boolean createIfMissing ) {
        CacheSchemaLibrary schemaLibrary = this.schemaLibrary.get();
        if (schemaLibrary == null && createIfMissing) {
            // Now get the cache for the schema and create the library ...
            Cache<String, SchematicEntry> schemaStore = this.store.getCacheManager().getCache(schemaCacheName);
            schemaLibrary = new CacheSchemaLibrary(schemaStore);
            if (this.schemaLibrary.compareAndSet(null, schemaLibrary)) {
                schemaLibrary.start();
            } else {
                // Someone else snuck in and set the value ...
                schemaLibrary = this.schemaLibrary.get();
            }
        }
        return schemaLibrary;
    }

    @Override
    public Map<String, Results> validateAll() {
        CacheSchemaLibrary schemaLibrary = schemaLibrary(true);
        if (store.getAdvancedCache().getRpcManager() == null) {
            // This is a non-clustered cache, which cannot run Map-Reduce. In this case, just go through them
            // all and run validation manually using the mapper...
            DocumentValidationMapper mapper = new DocumentValidationMapper(schemaLibrary, defaultSchemaUri);
            ResultsCollector resultsCollector = new ResultsCollector();
            for (Map.Entry<String, SchematicEntry> entry : store.entrySet()) {
                String key = entry.getKey();
                SchematicEntry value = entry.getValue();
                mapper.map(key, value, resultsCollector);
            }
            return resultsCollector.getResultsByKey();
        }

        // It is a clustered cache, so we can run Map-Reduce ...

        // Create a copy of all of the JSON Schema documents ...
        InMemoryDocumentLibrary schemaDocs = new InMemoryDocumentLibrary(schemaLibrary.getName());
        for (Map.Entry<String, SchematicEntry> entry : schemaLibrary.store().entrySet()) {
            String key = entry.getKey();
            SchematicEntry value = entry.getValue();
            schemaDocs.put(key, value.getContentAsDocument());
        }

        // Now create the Map-Reduce task, using the copy of the JSON Schema library ...
        MapReduceTask<String, SchematicEntry, String, Results> task = new MapReduceTask<String, SchematicEntry, String, Results>(
                                                                                                                                 this.store);
        task.mappedWith(new DocumentValidationMapper(schemaDocs, defaultSchemaUri));
        task.reducedWith(new DocumentValidationReducer());

        // Now execute ...
        return task.execute();
    }

    @Override
    public Map<String, Results> validate( String firstKey,
                                          String... additionalKeys ) {
        Map<String, Results> resultsByKey = new HashMap<String, Results>();
        Results results = validate(firstKey);
        if (results != null && results.hasProblems()) {
            resultsByKey.put(firstKey, results);
        }
        for (String key : additionalKeys) {
            results = validate(key);
            if (results != null && results.hasProblems()) {
                resultsByKey.put(key, results);
            }
        }
        return resultsByKey;
    }

    @Override
    public Results validate( String key ) {
        SchematicEntry entry = store.get(key);
        if (entry != null) {
            DocumentValidationMapper mapper = new DocumentValidationMapper(schemaLibrary(true), defaultSchemaUri);
            return mapper.validate(key, entry); // might be null if no JSON document or doc has no affiliated schema
        }
        return null;
    }

    protected static class ResultsCollector implements Collector<String, Results> {
        private final Map<String, Results> resultsByKey = new HashMap<String, Results>();

        @Override
        public void emit( String key,
                          Results value ) {
            resultsByKey.put(key, value);
        }

        public Map<String, Results> getResultsByKey() {
            return resultsByKey;
        }
    }

    protected SchematicEntry proxy( String key,
                                    SchematicEntry entry ) {
        if (entry == null) return null;
        SchematicEntryLiteral literal = (SchematicEntryLiteral)entry;
        FlagContainer flagContainer = null;
        return literal.getProxy(context, key, flagContainer);
    }

    protected SchematicEntry removedResult( String key,
                                            SchematicEntry entry ) {
        SchematicEntryLiteral literal = (SchematicEntryLiteral)entry;
        literal.markRemoved(true);
        return proxy(key, literal);
    }

    protected SchematicEntryProxyFuture future( String key,
                                                NotifyingFuture<SchematicEntry> future,
                                                boolean isRemoved ) {
        return new SchematicEntryProxyFuture(future, key, isRemoved);
    }

    @Override
    public SchematicEntry get( String key ) {
        return proxy(key, store.get(key));
    }

    @Override
    public boolean containsKey( String key ) {
        return store.containsKey(key);
    }

    @Override
    public SchematicEntry put( String key,
                               Document document,
                               Document metadata ) {
        if (metadata == null) metadata = Schematic.newDocument(FieldName.ID, key);
        SchematicEntry newEntry = new SchematicEntryLiteral(key, document, metadata, defaultContentTypeForDocument);
        SchematicEntry oldValue = store.put(key, newEntry);
        return oldValue != null ? removedResult(key, oldValue) : null;
    }

    @Override
    public SchematicEntry put( String key,
                               Binary binaryContent,
                               Document metadata ) {
        if (metadata == null) metadata = Schematic.newDocument(FieldName.ID, key);
        SchematicEntry newEntry = new SchematicEntryLiteral(key, binaryContent, metadata, defaultContentTypeForBinary);
        SchematicEntry oldValue = store.put(key, newEntry);
        return oldValue != null ? removedResult(key, oldValue) : null;
    }

    @Override
    public SchematicEntry put( Document entryDocument ) {
        Document metadata = entryDocument.getDocument(FieldName.METADATA);
        Object content = entryDocument.getDocument(FieldName.CONTENT);
        if (metadata == null || content == null) {
            throw new IllegalArgumentException("The supplied document is not of the required format");
        }
        String key = metadata.getString(FieldName.ID);
        if (key == null) {
            throw new IllegalArgumentException("The supplied document is not of the required format");
        }
        SchematicEntry newEntry = null;
        if (content instanceof Document) {
            newEntry = new SchematicEntryLiteral(key, (Document)content, metadata, defaultContentTypeForDocument);
        } else {
            newEntry = new SchematicEntryLiteral(key, (Binary)content, metadata, defaultContentTypeForBinary);
        }
        SchematicEntry oldValue = store.put(key, newEntry);
        return oldValue != null ? removedResult(key, oldValue) : null;
    }

    @Override
    public SchematicEntry putIfAbsent( String key,
                                       Document document,
                                       Document metadata ) {
        if (metadata == null) metadata = Schematic.newDocument(FieldName.ID, key);
        SchematicEntryLiteral newEntry = new SchematicEntryLiteral(key, document, metadata, defaultContentTypeForDocument);
        SchematicEntry existingEntry = store.putIfAbsent(key, newEntry);
        if (existingEntry == null) return null;
        return proxy(key, existingEntry);
    }

    @Override
    public SchematicEntry putIfAbsent( String key,
                                       Binary binaryContent,
                                       Document metadata ) {
        if (metadata == null) metadata = Schematic.newDocument(FieldName.ID, key);
        SchematicEntryLiteral newEntry = new SchematicEntryLiteral(key, binaryContent, metadata, defaultContentTypeForBinary);
        SchematicEntry existingEntry = store.putIfAbsent(key, newEntry);
        if (existingEntry == null) return null;
        return proxy(key, existingEntry);
    }

    @Override
    public SchematicEntry putIfAbsent( Document entryDocument ) {
        Document metadata = entryDocument.getDocument(FieldName.METADATA);
        Object content = entryDocument.getDocument(FieldName.CONTENT);
        if (metadata == null || content == null) {
            throw new IllegalArgumentException("The supplied document is not of the required format");
        }
        String key = metadata.getString(FieldName.ID);
        if (key == null) {
            throw new IllegalArgumentException("The supplied document is not of the required format");
        }
        SchematicEntry newEntry = null;
        if (content instanceof Document) {
            newEntry = new SchematicEntryLiteral(key, (Document)content, metadata, defaultContentTypeForDocument);
        } else {
            newEntry = new SchematicEntryLiteral(key, (Binary)content, metadata, defaultContentTypeForBinary);
        }
        SchematicEntry existingEntry = store.putIfAbsent(key, newEntry);
        if (existingEntry == null) return null;
        return proxy(key, existingEntry);
    }

    @Override
    public SchematicEntry replace( String key,
                                   Document document,
                                   Document metadata ) {
        if (metadata == null) metadata = Schematic.newDocument(FieldName.ID, key);
        SchematicEntryLiteral newEntry = new SchematicEntryLiteral(key, document, metadata, defaultContentTypeForDocument);
        return removedResult(key, store.replace(key, newEntry));
    }

    @Override
    public SchematicEntry replace( String key,
                                   Binary binaryContent,
                                   Document metadata ) {
        if (metadata == null) metadata = Schematic.newDocument(FieldName.ID, key);
        SchematicEntryLiteral newEntry = new SchematicEntryLiteral(key, binaryContent, metadata, defaultContentTypeForBinary);
        return removedResult(key, store.replace(key, newEntry));
    }

    @Override
    public SchematicEntry remove( String key ) {
        return removedResult(key, store.remove(key));
    }

    @Override
    public NotifyingFuture<SchematicEntry> getAsync( String key ) {
        return future(key, store.getAsync(key), false);
    }

    @Override
    public NotifyingFuture<SchematicEntry> putAsync( String key,
                                                     Document document,
                                                     Document metadata ) {
        if (metadata == null) metadata = Schematic.newDocument(FieldName.ID, key);
        SchematicEntryLiteral newEntry = new SchematicEntryLiteral(key, document, metadata, defaultContentTypeForDocument);
        return future(key, store.putAsync(key, newEntry), true);
    }

    @Override
    public NotifyingFuture<SchematicEntry> putAsync( String key,
                                                     Binary binaryContent,
                                                     Document metadata ) {
        if (metadata == null) metadata = Schematic.newDocument(FieldName.ID, key);
        SchematicEntryLiteral newEntry = new SchematicEntryLiteral(key, binaryContent, metadata, defaultContentTypeForBinary);
        return future(key, store.putAsync(key, newEntry), true);
    }

    @Override
    public NotifyingFuture<SchematicEntry> putIfAbsentAsync( String key,
                                                             Document document,
                                                             Document metadata ) {
        if (metadata == null) metadata = Schematic.newDocument(FieldName.ID, key);
        SchematicEntryLiteral newEntry = new SchematicEntryLiteral(key, document, metadata, defaultContentTypeForDocument);
        return future(key, store.putIfAbsentAsync(key, newEntry), true);
    }

    @Override
    public NotifyingFuture<SchematicEntry> putIfAbsentAsync( String key,
                                                             Binary binaryContent,
                                                             Document metadata ) {
        if (metadata == null) metadata = Schematic.newDocument(FieldName.ID, key);
        SchematicEntryLiteral newEntry = new SchematicEntryLiteral(key, binaryContent, metadata, defaultContentTypeForBinary);
        return future(key, store.putIfAbsentAsync(key, newEntry), true);
    }

    @Override
    public NotifyingFuture<SchematicEntry> replaceAsync( String key,
                                                         Document document,
                                                         Document metadata ) {
        if (metadata == null) metadata = Schematic.newDocument(FieldName.ID, key);
        SchematicEntryLiteral newEntry = new SchematicEntryLiteral(key, document, metadata, defaultContentTypeForDocument);
        return future(key, store.replaceAsync(key, newEntry), true);
    }

    @Override
    public NotifyingFuture<SchematicEntry> replaceAsync( String key,
                                                         Binary binaryContent,
                                                         Document metadata ) {
        if (metadata == null) metadata = Schematic.newDocument(FieldName.ID, key);
        SchematicEntryLiteral newEntry = new SchematicEntryLiteral(key, binaryContent, metadata, defaultContentTypeForBinary);
        return future(key, store.replaceAsync(key, newEntry), true);
    }

    @Override
    public NotifyingFuture<SchematicEntry> removeAsync( String key ) {
        return future(key, store.removeAsync(key), true);
    }

    @Override
    public boolean lock( Collection<String> keys ) {
        if (context.isExplicitLockingEnabled() && !keys.isEmpty()) {
            return context.getCacheForLocking().lock(keys);
        }
        return true;
    }

    @Override
    public boolean isExplicitLockingEnabled() {
        return context.isExplicitLockingEnabled();
    }

    protected class SchematicEntryProxyFuture implements NotifyingFuture<SchematicEntry> {
        private final NotifyingFuture<SchematicEntry> original;
        private final String key;
        private final boolean isRemoved;

        protected SchematicEntryProxyFuture( NotifyingFuture<SchematicEntry> original,
                                             String key,
                                             boolean isRemoved ) {
            this.original = original;
            this.key = key;
            this.isRemoved = isRemoved;
        }

        @Override
        public NotifyingFuture<SchematicEntry> attachListener( final FutureListener<SchematicEntry> listener ) {
            original.attachListener(listener);
            return this;
        }

        @Override
        public boolean cancel( boolean mayInterruptIfRunning ) {
            return original.cancel(mayInterruptIfRunning);
        }

        @Override
        public SchematicEntry get() throws InterruptedException, ExecutionException {
            SchematicEntry result = original.get();
            return isRemoved ? removedResult(key, result) : proxy(key, result);
        }

        @Override
        public SchematicEntry get( long timeout,
                                   TimeUnit unit ) throws InterruptedException, ExecutionException, TimeoutException {
            SchematicEntry result = original.get(timeout, unit);
            return isRemoved ? removedResult(key, result) : proxy(key, result);
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

}
