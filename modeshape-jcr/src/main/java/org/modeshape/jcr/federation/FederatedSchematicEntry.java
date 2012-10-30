/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.modeshape.jcr.federation;

import org.infinispan.schematic.SchematicEntry;
import org.infinispan.schematic.document.Binary;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableDocument;
import org.infinispan.schematic.internal.document.BasicDocument;
import org.infinispan.schematic.internal.document.DocumentEditor;
import org.infinispan.schematic.internal.document.MutableDocument;

/**
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class FederatedSchematicEntry implements SchematicEntry {

    private MutableDocument document;

    public FederatedSchematicEntry( MutableDocument document ) {
        this.document = document;
    }

    @Override
    public Document getMetadata() {
        return document.getDocument(FieldName.METADATA);
    }

    protected MutableDocument mutableMetadata() {
        return (MutableDocument)getMetadata();
    }

    @Override
    public String getContentType() {
        return getMetadata().getString(FieldName.CONTENT_TYPE);
    }

    @Override
    public Object getContent() {
        return document.getDocument(FieldName.CONTENT);
    }

    @Override
    public Document getContentAsDocument() {
        return document.getDocument(FieldName.CONTENT);
    }

    @Override
    public Binary getContentAsBinary() {
        return document.getBinary(FieldName.CONTENT);
    }

    @Override
    public boolean hasDocumentContent() {
        return getContentAsDocument() != null;
    }

    @Override
    public boolean hasBinaryContent() {
        return getContentAsBinary() != null;
    }

    protected Object setContent( Object content ) {
        assert content != null;
        return this.document.put(FieldName.CONTENT, content);
    }

    protected void setMetadata( Document metadata,
                                String defaultContentType ) {
        if (metadata != null) {
            if (metadata instanceof EditableDocument) metadata = ((EditableDocument)metadata).unwrap();

            // Copy all the metadata into the entry's metadata ...
            Document existingMetadata = getMetadata();
            MutableDocument newMetadata = new BasicDocument(metadata.size() + 1);
            newMetadata.put(FieldName.ID, existingMetadata.get(FieldName.ID));
            for (Document.Field field : metadata.fields()) {
                String fieldName = field.getName();
                if (fieldName.equals(FieldName.ID)) continue;
                newMetadata.put(fieldName, field.getValue());
            }

            // Make sure the metadata has the content type
            if (newMetadata.getString(FieldName.CONTENT_TYPE) == null) {
                newMetadata.put(FieldName.CONTENT_TYPE, defaultContentType);
            }

            // Now record the change ...
            document.put(FieldName.METADATA, newMetadata);
        }
    }

    @Override
    public void setContent( Document content,
                            Document metadata,
                            String defaultContentType ) {
        if (content instanceof EditableDocument) content = ((EditableDocument)content).unwrap();
        setContent(content);
        setMetadata(metadata, defaultContentType);
    }

    @Override
    public void setContent( Binary content,
                            Document metadata,
                            String defaultContentType ) {
        setContent(content);
        setMetadata(metadata, defaultContentType);
    }

    @Override
    public EditableDocument editDocumentContent() {
       return new DocumentEditor(document);
    }

    @Override
    public EditableDocument editMetadata() {
        return new DocumentEditor(mutableMetadata());
    }

    @Override
    public Document asDocument() {
        return document;
    }
}
