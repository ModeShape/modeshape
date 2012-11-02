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
import org.modeshape.jcr.value.Name;

/**
 * Helper class which should be used by {@link Connector} implementations to create {@link EditableDocument}(s) with the correct
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

    public FederatedDocumentBuilder addProperty( Object name,
                                                 Object value ) {
        EditableDocument properties = federatedDocument.getDocument(DocumentTranslator.PROPERTIES);
        if (properties == null) {
            properties = DocumentFactory.newDocument();
            federatedDocument.set(DocumentTranslator.PROPERTIES, properties);
        }

        //the correct structure of properties is to be grouped by their namespace, so we need to take this into account
        String propertiesNamespace = "";
        if (name instanceof Name) {
            propertiesNamespace = ((Name) name).getNamespaceUri();
            name = ((Name) name).getLocalName();
        }

        EditableDocument propertiesUnderNamespace = properties.getDocument(propertiesNamespace);
        if (propertiesUnderNamespace == null) {
            propertiesUnderNamespace = DocumentFactory.newDocument();
            properties.setDocument(propertiesNamespace, propertiesUnderNamespace);
        }
        propertiesUnderNamespace.set(name.toString(), value);

        return this;
    }

    public FederatedDocumentBuilder addChild( String id,
                                              String name ) {
        EditableArray children = federatedDocument.getArray(DocumentTranslator.CHILDREN);
        if (children == null) {
            children = DocumentFactory.newArray();
            federatedDocument.setArray(DocumentTranslator.CHILDREN, children);
        }
        EditableDocument childDocument = DocumentFactory.newDocument(DocumentTranslator.KEY, id, DocumentTranslator.NAME, name);
        childDocument.set(DocumentTranslator.PARENT, getDocumentId());
        children.addDocument(childDocument);
        return this;
    }

    private Object getDocumentId() {
        return federatedDocument.get(DocumentTranslator.KEY);
    }

    public EditableDocument build() {
        return federatedDocument;
    }
}
