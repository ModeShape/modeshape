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

import org.infinispan.lifecycle.Lifecycle;
import org.infinispan.schematic.DocumentLibrary;
import org.infinispan.schematic.SchemaLibrary;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.internal.document.Paths;
import org.infinispan.schematic.internal.schema.SchemaDocument;
import org.infinispan.schematic.internal.schema.SchemaDocumentCache;
import org.infinispan.schematic.internal.schema.ValidationResult;
import org.infinispan.util.concurrent.NotifyingFuture;

public class InMemorySchemaLibrary implements SchemaLibrary, Lifecycle {

   private final DocumentLibrary documents;
   private final SchemaDocumentCache schemaDocuments;

   public InMemorySchemaLibrary(String name) {
      this.documents = new InMemoryDocumentLibrary(name);
      this.schemaDocuments = new SchemaDocumentCache(this, null);
   }

   @Override
   public void start() {
   }

   @Override
   public void stop() {
   }

   @Override
   public String getName() {
      return documents.getName();
   }

   @Override
   public Document get(String key) {
      return documents.get(key);
   }

   @Override
   public Document put(String key, Document document) {
      Document result = documents.put(key, document);
      schemaDocuments.remove(key);
      return result;
   }

   @Override
   public Document putIfAbsent(String key, Document document) {
      Document result = documents.putIfAbsent(key, document);
      schemaDocuments.remove(key);
      return result;
   }

   @Override
   public Document replace(String key, Document document) {
      Document result = documents.replace(key, document);
      schemaDocuments.remove(key);
      return result;
   }

   @Override
   public Document remove(String key) {
      Document result = documents.remove(key);
      schemaDocuments.remove(key);
      return result;
   }

   @Override
   public NotifyingFuture<Document> getAsync(String key) {
      return documents.getAsync(key);
   }

   @Override
   public NotifyingFuture<Document> putAsync(String key, Document document) {
      NotifyingFuture<Document> result = documents.putAsync(key, document);
      schemaDocuments.remove(key);
      return result;
   }

   @Override
   public NotifyingFuture<Document> putIfAbsentAsync(String key, Document document) {
      NotifyingFuture<Document> result = documents.putIfAbsentAsync(key, document);
      schemaDocuments.remove(key);
      return result;
   }

   @Override
   public NotifyingFuture<Document> replaceAsync(String key, Document document) {
      NotifyingFuture<Document> result = documents.replaceAsync(key, document);
      schemaDocuments.remove(key);
      return result;
   }

   @Override
   public NotifyingFuture<Document> removeAsync(String key) {
      NotifyingFuture<Document> result = documents.removeAsync(key);
      schemaDocuments.remove(key);
      return result;
   }

   @Override
   public Results validate(Document document, String schemaUri) {
      ValidationResult result = new ValidationResult();
      SchemaDocument schema = schemaDocuments.get(schemaUri, result);
      if (schema != null) {
         schema.getValidator().validate(null, null, document, Paths.rootPath(), result, schemaDocuments);
      }
      return result;
   }
}
