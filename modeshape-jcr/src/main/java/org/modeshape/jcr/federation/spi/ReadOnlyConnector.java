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
package org.modeshape.jcr.federation.spi;

import org.infinispan.schematic.document.Document;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.cache.DocumentStoreException;
import org.modeshape.jcr.value.Name;

/**
 * A specialized abstract {@link Connector} class that is readable and can <i>never</i> update content. The connector always
 * throws {@link DocumentStoreException}s whenever it is asked to {@link #storeDocument(Document) store},
 * {@link Connector#updateDocument(DocumentChanges) update}, or {@link #removeDocument(String) remove} documents. Thus, do not subclass if
 * your custom connector can <i>sometimes</i> modify content.
 */
public abstract class ReadOnlyConnector extends Connector {

    @Override
    public final boolean removeDocument( String id ) {
        String msg = JcrI18n.connectorIsReadOnly.text(getSourceName(), id);
        throw new DocumentStoreException(id, msg);
    }

    @Override
    public final void storeDocument( Document document ) {
        DocumentReader reader = readDocument(document);
        String id = reader.getDocumentId();
        String msg = JcrI18n.connectorIsReadOnly.text(getSourceName(), id);
        throw new DocumentStoreException(id, msg);
    }

    @Override
    public final void updateDocument( DocumentChanges documentChanges ) {
        String documentId = documentChanges.getDocumentId();
        String msg = JcrI18n.connectorIsReadOnly.text(getSourceName(), documentId);
        throw new DocumentStoreException(documentId, msg);
    }

    @Override
    public String newDocumentId( String parentId,
                                 Name newDocumentName ) {
        throw new DocumentStoreException(JcrI18n.connectorIsReadOnly.text(getSourceName(), newDocumentName));
    }
}
