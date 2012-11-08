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

import java.util.ArrayList;
import java.util.List;
import org.infinispan.schematic.DocumentFactory;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableArray;
import org.infinispan.schematic.document.EditableDocument;
import org.modeshape.jcr.cache.document.DocumentTranslator;
import org.modeshape.jcr.federation.Connector.DocumentWriter;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.basic.BasicMultiValueProperty;
import org.modeshape.jcr.value.basic.BasicSingleValueProperty;

/**
 * Helper class which should be used by {@link Connector} implementations to create {@link EditableDocument}(s) with the correct
 * structure.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class FederatedDocumentWriter implements Connector.DocumentWriter {

    private final EditableDocument federatedDocument;
    private final DocumentTranslator translator;

    public FederatedDocumentWriter( DocumentTranslator translator,
                                    Document document ) {
        this.federatedDocument = document != null ? DocumentFactory.newDocument(document) : DocumentFactory.newDocument();
        this.translator = translator;
    }

    public FederatedDocumentWriter( DocumentTranslator translator ) {
        this(translator, null);
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
        Name nameObj = translator.getNameFactory().create(name);
        return addProperty(nameObj, value);
    }

    @Override
    public DocumentWriter addProperty( String name,
                                       Object[] values ) {
        if (values == null) return this;
        Name nameObj = translator.getNameFactory().create(name);
        return addProperty(nameObj, values);
    }

    @Override
    public DocumentWriter addProperty( String name,
                                       Object firstValue,
                                       Object... additionalValues ) {
        Name nameObj = translator.getNameFactory().create(name);
        return addProperty(nameObj, firstValue, additionalValues);
    }

    @Override
    public FederatedDocumentWriter addProperty( Name name,
                                                Object value ) {
        if (value == null) {
            return this;
        }
        translator.setProperty(federatedDocument, new BasicSingleValueProperty(name, value), null);
        return this;
    }

    @Override
    public DocumentWriter addProperty( Name name,
                                       Object[] values ) {
        if (values == null) return this;
        translator.setProperty(federatedDocument, new BasicMultiValueProperty(name, values), null);
        return this;
    }

    @Override
    public DocumentWriter addProperty( Name name,
                                       Object firstValue,
                                       Object... additionalValues ) {
        List<Object> values = new ArrayList<Object>(1 + additionalValues.length);
        values.add(firstValue);
        for (Object value : additionalValues) {
            values.add(value);
        }
        translator.setProperty(federatedDocument, new BasicMultiValueProperty(name, values), null);
        return this;
    }

    @Override
    public FederatedDocumentWriter addChild( String id,
                                             Name name ) {
        String nameStr = translator.getStringFactory().create(name);
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
    public FederatedDocumentWriter setChildren( List<Document> children ) {
        EditableArray childrenArray = federatedDocument.getArray(DocumentTranslator.CHILDREN);
        childrenArray = DocumentFactory.newArray();
        federatedDocument.setArray(DocumentTranslator.CHILDREN, childrenArray);

        for (Document child : children) {
            childrenArray.add(child);
        }
        return this;
    }

    @Override
    public FederatedDocumentWriter setParents( String... parentIds ) {
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
