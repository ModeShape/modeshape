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

import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableDocument;

/**
 * SPI of a generic external connector, representing the interface to an external system integrated with ModeShape.
 * Since it is expected that the documents are well formed (structure-wise), the {@link FederatedDocumentBuilder} class should
 * be used.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public interface Connector {

    /**
     * Returns the name of the source which this connector interfaces with.
     *
     * @return a {@code non-null} string.
     */
    public String getSourceName();

    /**
     * Returns an {@link EditableDocument} instance representing the document with a given id. The document should have a "proper"
     * structure for it to be usable by ModeShape.
     *
     * @param id a {@code non-null} string
     * @return either an {@link EditableDocument} instance or {@code null}
     */
    public EditableDocument getDocumentById( String id );

    /**
     * Returns an {@link EditableDocument} instance representing the document at a given location. The document should contain
     * at least the {@link org.modeshape.jcr.cache.document.DocumentTranslator#KEY} and {@link org.modeshape.jcr.cache.document.DocumentTranslator#NAME}
     * fields.
     *
     * @param location a {@code non-null} string
     * @return either an {@link EditableDocument} instance or {@code null}
     */
    public EditableDocument getDocumentAtLocation( String location );

    /**
     * Removes the document with the given id.
     *
     * @param id a {@code non-null} string.
     */
    public void removeDocument( String id );

    /**
     * Checks if a document with the given id exists in the end-source.
     *
     * @param id a {@code non-null} string.
     * @return {@code true} if such a document exists, {@code false} otherwise.
     */
    public boolean hasDocument( String id );

    /**
     * Stores the given document.
     *
     * @param document a {@code non-null} {@link org.infinispan.schematic.document.Document} instance.
     */
    public void storeDocument(Document document);

    /**
     * Updates the document with the given id.
     *
     * @param id a {@code non-null} string representing the id of a document
     * @param document a {@code non-null} {@link org.infinispan.schematic.document.Document} instance.
     */
    public void updateDocument(String id, Document document);

}
