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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.infinispan.schematic.DocumentFactory;
import org.infinispan.schematic.document.Array;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableDocument;
import org.modeshape.jcr.cache.document.DocumentTranslator;
import org.modeshape.jcr.federation.spi.DocumentReader;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Property;

/**
 * Implementation of a {@link org.modeshape.jcr.federation.spi.DocumentReader} that be used to obtain "semantic" information
 * from a federated document
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
}
