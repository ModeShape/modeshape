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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.chemistry.opencmis.client.api.Property;
import org.apache.chemistry.opencmis.commons.data.CmisExtensionElement;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.enums.PropertyType;

/**
 *
 * @author kulikov
 */
public class PropertyImpl implements Property {

    private String id;    
    private String localName, displayName, queryName;
    private PropertyType type;

    private Object[] values;

    public PropertyImpl(
            PropertyType type,
            String id,
            String localName,
            String displayName,
            String queryName,
            Object... values) {
        this.type = type;
        this.id = id;
        this.localName = localName;
        this.displayName = displayName;
        this.queryName = queryName;
        this.values = values;
    }

    @Override
    public boolean isMultiValued() {
        return values.length > 1;
    }

    @Override
    public PropertyType getType() {
        return type;
    }

    @Override
    public PropertyDefinition getDefinition() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object getValue() {
        return values == null ? null : values[0];
    }

    @Override
    public String getValueAsString() {
        return values == null ? null : values[0].toString();
    }

    @Override
    public String getValuesAsString() {
        StringBuilder builder = new StringBuilder();
        builder.append(values[0].toString());
        for (int i = 1; i < values.length; i++) {
            builder.append(",");
            builder.append(values[i].toString());
        }
        return builder.toString();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getLocalName() {
        return localName;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getQueryName() {
        return queryName;
    }

    @Override
    public List getValues() {
        ArrayList list = new ArrayList();
        list.addAll(Arrays.asList(values));
        return list;
    }

    @Override
    public Object getFirstValue() {
        return values[0];
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
