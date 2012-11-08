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

import java.util.List;
import org.infinispan.schematic.DocumentFactory;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableArray;
import org.infinispan.schematic.document.EditableDocument;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.cache.document.DocumentTranslator;
import org.modeshape.jcr.value.Name;

/**
 * Helper class which should be used by {@link Connector} implementations to create {@link EditableDocument}(s) with the correct
 * structure.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class FederatedDocumentWriter implements Connector.DocumentWriter {

    private final ExecutionContext context;
    private final EditableDocument federatedDocument;

    public FederatedDocumentWriter( ExecutionContext context,
                                    Document document ) {
        this.context = context;
        this.federatedDocument = document != null ? DocumentFactory.newDocument(document) : DocumentFactory.newDocument();
    }

    public FederatedDocumentWriter( ExecutionContext context ) {
        this(context, null);
    }

    @Override
    public FederatedDocumentWriter setId( String id ) {
        assert id != null;
        federatedDocument.set(DocumentTranslator.KEY, id);
        return this;
    }

    @Override
    public FederatedDocumentWriter addProperty( String name,
                                                Object value ) {
        Name nameObj = context.getValueFactories().getNameFactory().create(name);
        return addProperty(nameObj, value);
    }

    @Override
    public FederatedDocumentWriter addProperty( Name name,
                                                Object value ) {
        if (value == null) {
            return this;
        }
        EditableDocument properties = federatedDocument.getDocument(DocumentTranslator.PROPERTIES);
        if (properties == null) {
            properties = DocumentFactory.newDocument();
            federatedDocument.set(DocumentTranslator.PROPERTIES, properties);
        }

        // the correct structure of properties is to be grouped by their namespace, so we need to take this into account
        final String propertiesNamespace = name.getNamespaceUri();
        EditableDocument propertiesUnderNamespace = properties.getDocument(propertiesNamespace);
        if (propertiesUnderNamespace == null) {
            propertiesUnderNamespace = DocumentFactory.newDocument();
            properties.setDocument(propertiesNamespace, propertiesUnderNamespace);
        }
        propertiesUnderNamespace.set(name.getLocalName(), value);

        return this;
    }

    @Override
    public FederatedDocumentWriter addChild( String id,
                                             Name name ) {
        String nameStr = context.getValueFactories().getStringFactory().create(name);
        return addChild(id, nameStr);
    }

    @Override
    public FederatedDocumentWriter addChild( String id,
                                             String name ) {
        EditableArray children = federatedDocument.getArray(DocumentTranslator.CHILDREN);
        if (children == null) {
            children = DocumentFactory.newArray();
            federatedDocument.setArray(DocumentTranslator.CHILDREN, children);
        }
        children.addDocument(DocumentFactory.newDocument(DocumentTranslator.KEY, id, DocumentTranslator.NAME, name));
        return this;
    }

    @Override
    public FederatedDocumentWriter addChild( EditableDocument child ) {
        EditableArray children = federatedDocument.getArray(DocumentTranslator.CHILDREN);
        if (children == null) {
            children = DocumentFactory.newArray();
            federatedDocument.setArray(DocumentTranslator.CHILDREN, children);
        }
        children.add(child);
        return this;
    }

    @Override
    public FederatedDocumentWriter setChildren(List<Document> children) {
        EditableArray childrenArray = federatedDocument.getArray(DocumentTranslator.CHILDREN);
        childrenArray = DocumentFactory.newArray();
        federatedDocument.setArray(DocumentTranslator.CHILDREN, childrenArray);

        for (Document child : children) {
            childrenArray.add(child);
        }
        return this;
    }

    @Override
    public FederatedDocumentWriter setParents( String...parentIds ) {
        if (parentIds.length == 1) {
            federatedDocument.setString(DocumentTranslator.PARENT, parentIds[0]);
        }
        if (parentIds.length > 1) {
            EditableArray parents = DocumentFactory.newArray();
            for (String parentId : parentIds) {
                parents.add(parentId);
            }
            federatedDocument.setArray(DocumentTranslator.PARENT, parents);
        }
        return this;
    }

    @Override
    public FederatedDocumentWriter setCacheTtlSeconds( int seconds ) {
        federatedDocument.setNumber(DocumentTranslator.CACHE_TTL_SECONDS, seconds);
        return this;
    }

    @Override
    public Connector.DocumentWriter setParents( List<String> parentIds ) {
        return setParents(parentIds.toArray(new String[parentIds.size()]));
    }

    @Override
    public FederatedDocumentWriter merge( Document document ) {
        federatedDocument.putAll(document);
        return this;
    }

    @Override
    public EditableDocument document() {
        return federatedDocument;
    }
}
