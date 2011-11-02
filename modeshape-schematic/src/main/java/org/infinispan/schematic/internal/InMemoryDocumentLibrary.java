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
