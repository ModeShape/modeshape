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
