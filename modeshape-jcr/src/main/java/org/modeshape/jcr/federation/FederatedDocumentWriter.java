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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.infinispan.schematic.DocumentFactory;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableArray;
import org.infinispan.schematic.document.EditableDocument;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.jcr.cache.document.DocumentTranslator;
import org.modeshape.jcr.spi.federation.DocumentWriter;
import org.modeshape.jcr.spi.federation.PageKey;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Property;
import org.modeshape.jcr.value.basic.BasicMultiValueProperty;
import org.modeshape.jcr.value.basic.BasicSingleValueProperty;

/**
 * Default implementation of the {@link DocumentWriter} interface.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class FederatedDocumentWriter implements DocumentWriter {

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
        translator.setKey(federatedDocument, id);
        return this;
    }

    protected final Name nameFrom( String name ) {
        return translator.getNameFactory().create(name);
    }

    @Override
    public DocumentWriter setPrimaryType( Name name ) {
        return addProperty(JcrLexicon.PRIMARY_TYPE, name);
    }

    @Override
    public DocumentWriter setPrimaryType( String name ) {
        return setPrimaryType(nameFrom(name));
    }

    @Override
    public DocumentWriter addMixinType( Name name ) {
        return addPropertyValue(JcrLexicon.MIXIN_TYPES, name);
    }

    @Override
    public DocumentWriter addMixinType( String name ) {
        return addMixinType(nameFrom(name));
    }

    @Override
    public DocumentWriter addPropertyValue( Name name,
                                            Object value ) {
        boolean knownToBeMultiple = false;
        translator.addPropertyValues(federatedDocument, name, knownToBeMultiple, Collections.singleton(value), null, null);
        return this;
    }

    @Override
    public DocumentWriter addPropertyValue( String name,
                                            Object value ) {
        return addPropertyValue(nameFrom(name), value);
    }

    @Override
    public DocumentWriter addProperty( String name,
                                       Object value ) {
        Name nameObj = nameFrom(name);
        return addProperty(nameObj, value);
    }

    @Override
    public DocumentWriter addProperty( String name,
                                       Object[] values ) {
        if (values == null) return this;
        Name nameObj = nameFrom(name);
        return addProperty(nameObj, values);
    }

    @Override
    public DocumentWriter addProperty( String name,
                                       Object firstValue,
                                       Object... additionalValues ) {
        Name nameObj = nameFrom(name);
        return addProperty(nameObj, firstValue, additionalValues);
    }

    @Override
    public DocumentWriter addProperty( Name name,
                                       Object value ) {
        if (value == null) {
            return this;
        }
        translator.setProperty(federatedDocument, new BasicSingleValueProperty(name, value), null, null);
        return this;
    }

    @Override
    public DocumentWriter addProperty( Name name,
                                       Object[] values ) {
        if (values == null) return this;
        int len = values.length;
        if (len == 0) return this;
        if (len == 1) {
            translator.setProperty(federatedDocument, new BasicSingleValueProperty(name, values[0]), null, null);
        } else {
            translator.setProperty(federatedDocument, new BasicMultiValueProperty(name, values), null, null);
        }
        return this;
    }

    @Override
    public DocumentWriter addProperty( Name name,
                                       Object firstValue,
                                       Object... additionalValues ) {
        if (additionalValues.length == 0) {
            translator.setProperty(federatedDocument, new BasicSingleValueProperty(name, firstValue), null, null);
        } else {
            List<Object> values = new ArrayList<Object>(1 + additionalValues.length);
            values.add(firstValue);
            for (Object value : additionalValues) {
                values.add(value);
            }
            translator.setProperty(federatedDocument, new BasicMultiValueProperty(name, values), null, null);
        }
        return this;
    }

    @Override
    public DocumentWriter addProperties( Map<Name, Property> properties ) {
        if (properties != null && !properties.isEmpty()) {
            for (Map.Entry<Name, Property> entry : properties.entrySet()) {
                translator.setProperty(federatedDocument, entry.getValue(), null, null);
            }
        }
        return this;
    }

    @Override
    public DocumentWriter addChild( String id,
                                    Name name ) {
        String nameStr = translator.getStringFactory().create(name);
        return addChild(id, nameStr);
    }

    @Override
    public DocumentWriter addChild( String id,
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
    public DocumentWriter removeChild( String id ) {
        EditableArray children = federatedDocument.getArray(DocumentTranslator.CHILDREN);
        if (children != null) {
            for (int i = 0; i != children.size(); ++i) {
                Object val = children.get(i);
                if (val instanceof Document) {
                    Document child = (Document)val;
                    if (child.getString(DocumentTranslator.KEY).equals(id)) {
                        children.remove(i);
                        return this;
                    }
                }
            }
        }
        return this;
    }

    @Override
    public DocumentWriter setProperties( Map<Name, Property> properties ) {
        // clear the existing values
        federatedDocument.setDocument(DocumentTranslator.PROPERTIES);
        return addProperties(properties);
    }

    @Override
    public DocumentWriter setChildren( List<? extends Document> children ) {
        EditableArray childrenArray = DocumentFactory.newArray();
        federatedDocument.setArray(DocumentTranslator.CHILDREN, childrenArray);

        for (Document child : children) {
            childrenArray.add(child);
        }
        return this;
    }

    @Override
    public DocumentWriter setChildren( LinkedHashMap<String, Name> children ) {
        EditableArray childrenArray = DocumentFactory.newArray();
        federatedDocument.setArray(DocumentTranslator.CHILDREN, childrenArray);

        for (String childId : children.keySet()) {
            addChild(childId, children.get(childId));
        }

        return this;
    }

    @Override
    public DocumentWriter setParents( String... parentIds ) {
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
    public DocumentWriter setParent( String parentId ) {
        return setParents(parentId);
    }

    @Override
    public DocumentWriter setCacheTtlSeconds( int seconds ) {
        federatedDocument.setNumber(DocumentTranslator.CACHE_TTL_SECONDS, seconds);
        return this;
    }

    @Override
    public DocumentWriter setNotQueryable() {
        translator.setQueryable(federatedDocument, false);
        return this;
    }

    @Override
    public DocumentWriter setParents( List<String> parentIds ) {
        return setParents(parentIds.toArray(new String[parentIds.size()]));
    }

    @Override
    public EditableDocument document() {
        return federatedDocument;
    }

    @Override
    public DocumentWriter addPage( String parentId,
                                   int nextPageOffset,
                                   long blockSize,
                                   long totalChildCount ) {
        return addPage(parentId, String.valueOf(nextPageOffset), blockSize, totalChildCount);
    }

    @Override
    public DocumentWriter addPage( String parentId,
                                   String nextPageOffset,
                                   long blockSize,
                                   long totalChildCount ) {
        EditableDocument childrenInfo = document().getOrCreateDocument(DocumentTranslator.CHILDREN_INFO);
        childrenInfo.setNumber(DocumentTranslator.COUNT, totalChildCount);
        childrenInfo.setNumber(DocumentTranslator.BLOCK_SIZE, blockSize);
        PageKey pageKey = new PageKey(parentId, nextPageOffset, blockSize);
        childrenInfo.setString(DocumentTranslator.NEXT_BLOCK, pageKey.toString());
        return this;
    }

    protected DocumentTranslator translator() {
        return translator;
    }

  
    @Override
    @Deprecated
    public DocumentWriter writeAdditionalDocument( String id ) {
        throw new UnsupportedOperationException("Deprecated and should not be used.");
    }
}
