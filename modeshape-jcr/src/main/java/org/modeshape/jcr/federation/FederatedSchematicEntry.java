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

package org.modeshape.jcr.federation;

import org.infinispan.schematic.SchematicEntry;
import org.infinispan.schematic.document.Binary;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableDocument;

/**
 * A simple {@link SchematicEntry} implementation which is used by the {@link FederatedDocumentStore} to wrap editable documents.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class FederatedSchematicEntry implements SchematicEntry {

    private EditableDocument document;

    public FederatedSchematicEntry( EditableDocument document ) {
        this.document = document;
    }

    @Override
    public Document getMetadata() {
        throw new UnsupportedOperationException("Metadata not supported for " + this.getClass().getName());
    }

    @Override
    public String getContentType() {
        throw new UnsupportedOperationException("Metadata not supported for " + this.getClass().getName());
    }

    @Override
    public Object getContent() {
        return document;
    }

    @Override
    public Document getContentAsDocument() {
        return document;
    }

    @Override
    public Binary getContentAsBinary() {
        throw new UnsupportedOperationException("Binaries documents not supported for " + this.getClass().getName());
    }

    @Override
    public boolean hasDocumentContent() {
        return document != null;
    }

    @Override
    public boolean hasBinaryContent() {
        return false;
    }

    @Override
    public void setContent( Document content,
                            Document metadata,
                            String defaultContentType ) {
        throw new UnsupportedOperationException(this.getClass().getName() + " does support content changing");
    }

    @Override
    public void setContent( Binary content,
                            Document metadata,
                            String defaultContentType ) {
        throw new UnsupportedOperationException("Binaries documents not supported for " + this.getClass().getName());
    }

    @Override
    public EditableDocument editDocumentContent() {
       return document;
    }

    @Override
    public EditableDocument editMetadata() {
        throw new UnsupportedOperationException("Metadata not supported for " + this.getClass().getName());
    }

    @Override
    public Document asDocument() {
        return document;
    }
}
