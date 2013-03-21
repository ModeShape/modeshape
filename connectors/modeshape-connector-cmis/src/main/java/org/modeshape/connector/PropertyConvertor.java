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
package org.modeshape.connector;

import org.apache.chemistry.opencmis.client.api.Property;
import org.infinispan.schematic.document.Document;
import org.modeshape.jcr.value.Name;

import java.math.BigDecimal;
import java.net.URI;
import java.util.GregorianCalendar;

/**
 * Provides conversations between CMIS and JCR properties and values.
 *
 * @author kulikov
 */
public class PropertyConvertor {
    /**
     * Converts JCR property name to CMIS property name.
     *
     * @param jcrName the JCR property name
     * @return CMIS property name
     */
    public String cmisName(Name jcrName) {
        return CmisLexicon.Namespace.PREFIX + ":" + jcrName.getLocalName();
    }

    /**
     * Converts text representation of the value(s) into java CMIS with
     * accordance to the CMIS property definition.
     *
     * @param property CMIS property definition object
     * @param jcrName  the name of the property used by JCR
     * @param document JCR text representation of the value(s)
     * @return Value as java object.
     */
    public Object cmisValue(Property property, String jcrName, Document document) {
        if (property.isMultiValued()) {

        }

        switch (property.getType()) {
            case STRING:
                return document.getString(jcrName);
            case BOOLEAN:
                return document.getBoolean(jcrName);
            case DECIMAL:
                return BigDecimal.valueOf(document.getLong(jcrName));
            case INTEGER:
                return document.getInteger(jcrName);
            case DATETIME:
                //FIXME
                return new GregorianCalendar();
            case URI:
                try {
                    return new URI(document.getString(jcrName));
                } catch (Exception e) {
                }
                break;
            case ID:
                return document.getString(jcrName);
            case HTML:
                return document.getString(jcrName);
        }

        return null;
    }
}
