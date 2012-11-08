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
import org.infinispan.schematic.document.Array;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableDocument;
import org.modeshape.jcr.cache.document.DocumentTranslator;

/**
 * Implementation of a {@link org.modeshape.jcr.federation.Connector.DocumentReader} that be used to obtain "semantic" information
 * from a federated document
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class FederatedDocumentReader implements Connector.DocumentReader {

    private final Document federatedDocument;

    public FederatedDocumentReader( Document federatedDocument ) {
        this.federatedDocument = federatedDocument;
    }

    @Override
    public String getDocumentId() {
        return federatedDocument.getString(DocumentTranslator.KEY);
    }

    @Override
    public List<String> getParentIds() {
        List<String> parents = new ArrayList<String>();
        if (!federatedDocument.containsField(DocumentTranslator.PARENT)) {
            return parents;
        }
        Object parentFieldValue = federatedDocument.get(DocumentTranslator.PARENT);
        if (parentFieldValue instanceof Array) {
            for (Array.Entry entry : ((Array) parentFieldValue).getEntries()) {
                parents.add(entry.getValue().toString());
            }
        } else {
            parents.add(parentFieldValue.toString());
        }
        return parents;
    }

    @Override
    public List<EditableDocument> getChildren() {
        List<EditableDocument> children = new ArrayList<EditableDocument>();
        if (!federatedDocument.containsField(DocumentTranslator.CHILDREN)) {
            return children;
        }
        List<?> childrenArray = federatedDocument.getArray(DocumentTranslator.CHILDREN);
        for (Object child : childrenArray) {
            assert child instanceof Document;
            children.add(DocumentFactory.newDocument((Document)child));
        }

        return children;
    }

    @Override
    public String getName() {
        return federatedDocument.getString(DocumentTranslator.NAME);
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
