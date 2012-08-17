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

import org.infinispan.schematic.SchematicEntry;
import org.infinispan.schematic.document.Binary;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableDocument;

public class ImmutableSchematicValue implements SchematicEntry {

   private final SchematicEntry delegate;

   public ImmutableSchematicValue(SchematicEntry delegate) {
      this.delegate = delegate;
   }

   @Override
   public Document getMetadata() {
      return delegate.getMetadata();
   }

   @Override
   public Object getContent() {
      return delegate.getContent();
   }

   @Override
   public String getContentType() {
      return delegate.getContentType();
   }

   @Override
   public Binary getContentAsBinary() {
      return delegate.getContentAsBinary();
   }

   @Override
   public Document getContentAsDocument() {
      return delegate.getContentAsDocument();
   }

   @Override
   public boolean hasDocumentContent() {
      return delegate.hasDocumentContent();
   }

   @Override
   public boolean hasBinaryContent() {
      return delegate.hasBinaryContent();
   }

   @Override
   public void setContent(Binary content, Document metadata, String defaultContentType) {
      throw new UnsupportedOperationException("This SchematicValue is read only");
   }

   @Override
   public void setContent(Document content, Document metadata, String defaultContentType) {
      throw new UnsupportedOperationException("This SchematicValue is read only");
   }

   @Override
   public EditableDocument editDocumentContent() {
      throw new UnsupportedOperationException("This SchematicValue is read only");
   }

   @Override
   public EditableDocument editMetadata() {
      throw new UnsupportedOperationException("This SchematicValue is read only");
   }

   @Override
    public Document asDocument() {
        return delegate.asDocument();
    }
}
