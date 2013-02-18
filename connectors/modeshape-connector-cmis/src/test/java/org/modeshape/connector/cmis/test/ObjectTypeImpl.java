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
package org.modeshape.connector.cmis.test;

import java.util.List;
import java.util.Map;
import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.client.api.Tree;
import org.apache.chemistry.opencmis.commons.data.CmisExtensionElement;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;

/**
 *
 * @author kulikov
 */
public class ObjectTypeImpl implements ObjectType {

    public ObjectTypeImpl(String typeId) {

    }

    @Override
    public boolean isBaseType() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ObjectType getBaseType() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ObjectType getParentType() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ItemIterable<ObjectType> getChildren() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Tree<ObjectType>> getDescendants(int i) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getId() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getLocalName() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getLocalNamespace() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getDisplayName() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getQueryName() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getDescription() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public BaseTypeId getBaseTypeId() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getParentTypeId() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Boolean isCreatable() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Boolean isFileable() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Boolean isQueryable() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Boolean isFulltextIndexed() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Boolean isIncludedInSupertypeQuery() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Boolean isControllablePolicy() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Boolean isControllableAcl() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Map<String, PropertyDefinition<?>> getPropertyDefinitions() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<CmisExtensionElement> getExtensions() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setExtensions(List<CmisExtensionElement> list) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
