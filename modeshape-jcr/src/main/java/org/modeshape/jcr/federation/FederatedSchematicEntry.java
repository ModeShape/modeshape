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
