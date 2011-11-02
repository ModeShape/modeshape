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

import org.infinispan.schematic.document.Document;
import org.infinispan.util.concurrent.NotifyingFuture;

/**
 * A library of JSON documents.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 * @since 5.1
 */
public interface DocumentLibrary {

   /**
    * Get the name of this library.
    * 
    * @return the library name; never null
    */
   String getName();

   /**
    * Get the document with the supplied key.
    * 
    * @param key
    *           the key or identifier for the document
    * @return the document, or null if there was no document with the supplied key
    */
   Document get(String key);

   /**
    * Store the supplied document at the given key.
    * 
    * @param key
    *           the key or identifier for the document
    * @param document
    *           the document that is to be stored
    * @return the document that was previously stored at this key, or null if there was no document with the supplied
    *         key
    */
   Document put(String key, Document document);

   /**
    * Store the supplied document at the given key.
    * 
    * @param key
    *           the key or identifier for the document
    * @param document
    *           the document that is to be stored
    * @return the document that was previously stored at this key, or null if there was no document with the supplied
    *         key
    */
   Document putIfAbsent(String key, Document document);

   /**
    * Replace the existing document at the given key with the document that is supplied. This method does nothing if
    * there is no document at the given key.
    * 
    * @param key
    *           the key or identifier for the document
    * @param document
    *           the document that is to replace the existing document
    * @return the document that was replaced, or null if nothing was replaced
    */
   Document replace(String key, Document document);

   /**
    * Remove the existing document at the given key.
    * 
    * @param key
    *           the key or identifier for the document
    * @return the document that was removed, or null if there was no document with the supplied key
    */
   Document remove(String key);

   /**
    * Asynchronous version of {@link #get(String)}. This method does not block on remote calls, even if the library
    * cache mode is synchronous.
    * 
    * @param key
    *           the key or identifier for the document
    * @return a future containing the document at the given key; never null
    */
   NotifyingFuture<Document> getAsync(String key);

   /**
    * Asynchronous version of {@link #put(String, Document)}. This method does not block on remote calls, even if the
    * library cache mode is synchronous.
    * 
    * @param key
    *           the key or identifier for the document
    * @param document
    *           the document that is to be stored
    * @return a future containing the old document that was previously stored at this key; never null
    */
   NotifyingFuture<Document> putAsync(String key, Document document);

   /**
    * Asynchronous version of {@link #putIfAbsent(String, Document)}. This method does not block on remote calls, even
    * if the library cache mode is synchronous.
    * 
    * @param key
    *           the key or identifier for the document
    * @param document
    *           the document that is to be stored
    * @return a future containing the existing document or the new document if there was no existing document at the
    *         supplied key
    */
   NotifyingFuture<Document> putIfAbsentAsync(String key, Document document);

   /**
    * Asynchronous version of {@link #replace(String, Document)}. This method does not block on remote calls, even if
    * the library cache mode is synchronous.
    * 
    * @param key
    *           the key or identifier for the document
    * @param document
    *           the document that is to be stored
    * @return a future containing the document that was replaced; never null
    */
   NotifyingFuture<Document> replaceAsync(String key, Document document);

   /**
    * Asynchronous version of {@link #remove(String)}. This method does not block on remote calls, even if the library
    * cache mode is synchronous.
    * 
    * @param key
    *           the key or identifier for the document
    * @return a future containing the old document that was removed; never null
    */
   NotifyingFuture<Document> removeAsync(String key);

}
