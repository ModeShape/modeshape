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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.infinispan.schematic.document.Array;
import org.infinispan.schematic.document.Document;
import org.modeshape.jcr.cache.document.DocumentTranslator;
import org.modeshape.jcr.spi.federation.DocumentReader;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Property;

/**
 * Implementation of a {@link org.modeshape.jcr.spi.federation.DocumentReader} that be used to obtain "semantic" information from
 * a federated document
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class FederatedDocumentReader implements DocumentReader {

    private final Document federatedDocument;
    private final DocumentTranslator translator;

    public FederatedDocumentReader( DocumentTranslator translator,
                                    Document federatedDocument ) {
        this.federatedDocument = federatedDocument;
        this.translator = translator;
    }

    @Override
    public String getDocumentId() {
        return translator.getKey(federatedDocument);
    }

    @Override
    public List<String> getParentIds() {
        List<String> parents = new ArrayList<String>();
        if (!federatedDocument.containsField(DocumentTranslator.PARENT)) {
            return parents;
        }
        Object parentFieldValue = federatedDocument.get(DocumentTranslator.PARENT);
        if (parentFieldValue instanceof Array) {
            for (Array.Entry entry : ((Array)parentFieldValue).getEntries()) {
                parents.add(entry.getValue().toString());
            }
        } else {
            parents.add(parentFieldValue.toString());
        }
        return parents;
    }

    @Override
    public List<Document> getChildren() {
        List<Document> children = new ArrayList<Document>();
        if (!federatedDocument.containsField(DocumentTranslator.CHILDREN)) {
            return children;
        }
        List<?> childrenArray = federatedDocument.getArray(DocumentTranslator.CHILDREN);
        for (Object child : childrenArray) {
            assert child instanceof Document;
            children.add((Document)child);
        }

        return children;
    }

    @Override
    public LinkedHashMap<String, Name> getChildrenMap() {
        LinkedHashMap<String, Name> children = new LinkedHashMap<String, Name>();
        if (!federatedDocument.containsField(DocumentTranslator.CHILDREN)) {
            return children;
        }
        List<?> childrenArray = federatedDocument.getArray(DocumentTranslator.CHILDREN);
        for (Object child : childrenArray) {
            assert child instanceof Document;
            Document childDocument = (Document)child;
            String childId = translator.getKey(childDocument);
            Name childName = translator.getNameFactory().create(childDocument.get(DocumentTranslator.NAME));
            children.put(childId, childName);
        }
        return children;
    }

    @Override
    public Name getPrimaryType() {
        return translator.getPrimaryType(federatedDocument);
    }

    @Override
    public Set<Name> getMixinTypes() {
        return translator.getMixinTypes(federatedDocument);
    }

    @Override
    public String getPrimaryTypeName() {
        return translator.getPrimaryTypeName(federatedDocument);
    }

    @Override
    public Set<String> getMixinTypeNames() {
        return translator.getMixinTypeNames(federatedDocument);
    }

    @Override
    public Map<Name, Property> getProperties() {
        Map<Name, Property> props = new HashMap<Name, Property>();
        translator.getProperties(federatedDocument, props);
        return props;
    }

    @Override
    public Property getProperty( Name name ) {
        return translator.getProperty(federatedDocument, name);
    }

    @Override
    public Property getProperty( String name ) {
        return translator.getProperty(federatedDocument, name);
    }

    @Override
    public Document document() {
        return federatedDocument;
    }

    @Override
    public Integer getCacheTtlSeconds() {
        return federatedDocument.getInteger(DocumentTranslator.CACHE_TTL_SECONDS);
    }

    @Override
    public boolean isQueryable() {
        return federatedDocument.getBoolean(DocumentTranslator.QUERYABLE_FIELD, true);
    }
}
