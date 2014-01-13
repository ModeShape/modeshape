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

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import org.infinispan.schematic.DocumentLibrary;
import org.infinispan.schematic.document.Document;
import org.infinispan.util.concurrent.FutureListener;
import org.infinispan.util.concurrent.NotifyingFuture;

public class InMemoryDocumentLibrary implements DocumentLibrary, Serializable {

    private static final long serialVersionUID = 1L;

    private final String name;
    private final ConcurrentMap<String, Document> documents = new ConcurrentHashMap<String, Document>();

    public InMemoryDocumentLibrary( String name ) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Document get( String key ) {
        return documents.get(key);
    }

    @Override
    public Document put( String key,
                         Document document ) {
        return documents.put(key, document);
    }

    @Override
    public Document putIfAbsent( String key,
                                 Document document ) {
        return documents.putIfAbsent(key, document);
    }

    @Override
    public Document replace( String key,
                             Document document ) {
        return documents.replace(key, document);
    }

    @Override
    public Document remove( String key ) {
        return documents.remove(key);
    }

    @Override
    public NotifyingFuture<Document> getAsync( String key ) {
        return new ImmediateFuture(get(key));
    }

    @Override
    public NotifyingFuture<Document> putAsync( String key,
                                               Document document ) {
        return new ImmediateFuture(put(key, document));
    }

    @Override
    public NotifyingFuture<Document> putIfAbsentAsync( String key,
                                                       Document document ) {
        return new ImmediateFuture(putIfAbsent(key, document));
    }

    @Override
    public NotifyingFuture<Document> replaceAsync( String key,
                                                   Document document ) {
        return new ImmediateFuture(replace(key, document));
    }

    @Override
    public NotifyingFuture<Document> removeAsync( String key ) {
        return new ImmediateFuture(remove(key));
    }

    protected static class ImmediateFuture implements NotifyingFuture<Document> {

        private final Document value;

        protected ImmediateFuture( Document value ) {
            this.value = value;
        }

        @Override
        public boolean cancel( boolean mayInterruptIfRunning ) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public Document get() {
            return value;
        }

        @Override
        public Document get( long timeout,
                             TimeUnit unit ) {
            return value;
        }

        @Override
        public NotifyingFuture<Document> attachListener( FutureListener<Document> listener ) {
            throw new UnsupportedOperationException();
        }

    }

}
