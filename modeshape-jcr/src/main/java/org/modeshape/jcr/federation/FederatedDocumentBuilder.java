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

import java.util.Map;
import org.infinispan.schematic.DocumentFactory;
import org.infinispan.schematic.document.EditableArray;
import org.infinispan.schematic.document.EditableDocument;
import org.modeshape.jcr.cache.document.DocumentTranslator;

/**
 * Helper class which should be used by {@link Connector} implementation to create {@link EditableDocument} with the correct
 * structure.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class FederatedDocumentBuilder {

    private final EditableDocument federatedDocument;

    public FederatedDocumentBuilder() {
        this.federatedDocument = DocumentFactory.newDocument();
    }

    public FederatedDocumentBuilder createDocument( String id,
                                                    String name,
                                                    Map<String, ?> properties ) {
        assert id != null;
        federatedDocument.set(DocumentTranslator.KEY, id);
        assert name != null;
        federatedDocument.set(DocumentTranslator.NAME, name);
        if (properties != null) {
            EditableDocument propertiesDoc = DocumentFactory.newDocument();
            for (String propertyName : properties.keySet()) {
                propertiesDoc.set(propertyName, properties.get(propertyName));
            }
            federatedDocument.set(DocumentTranslator.PROPERTIES, propertiesDoc);
        }

        return this;
    }

    public FederatedDocumentBuilder createDocument( String id,
                                                    String name) {
        return createDocument(id, name, null);
    }

    public FederatedDocumentBuilder addProperty( String name,
                                                 Object value ) {
        EditableDocument properties = federatedDocument.containsField(DocumentTranslator.PROPERTIES) ? federatedDocument
                .getDocument(DocumentTranslator.PROPERTIES) : DocumentFactory.newDocument();
        properties.set(name, value);
        return this;
    }

    public FederatedDocumentBuilder addChild( String id,
                                              String name ) {
        EditableArray children = federatedDocument.getArray(DocumentTranslator.CHILDREN);
        if (children == null) {
            children = DocumentFactory.newArray();
            federatedDocument.set(DocumentTranslator.CHILDREN, children);
        }
        children.addDocument(DocumentFactory.newDocument(DocumentTranslator.KEY, id, DocumentTranslator.NAME, name));
        return this;
    }

    public EditableDocument build() {
        return federatedDocument;
    }
}
