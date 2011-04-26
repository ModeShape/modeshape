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
package org.modeshape.sequencer.xsd;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 */
public class XsdResolvers {

    /**
     * In XML Schema, there is a distinct symbol space within each target namespace for each kind of <a
     * href="http://www.w3.org/TR/xmlschema-1/#concepts-data-model">declaration and definition component</a>, except that within a
     * target namespace the simple type definitions and complex type definitions share a single symbol space. See the <a
     * href="http://www.w3.org/TR/xmlschema-1/#concepts-nameSymbolSpaces">specification</a> for details.
     */
    public static enum SymbolSpace {
        ATTRIBUTE_DECLARATIONS,
        ELEMENT_DECLARATION,
        TYPE_DEFINITIONS,
        ATTRIBUTE_GROUP_DEFINITIONS,
        MODEL_GROUP_DEFINITIONS,
        IDENTITY_CONSTRAINT_DEFINITIONS,
    }

    private final Map<SymbolSpace, NamespaceEntityResolver> resolversByKind = new HashMap<XsdResolvers.SymbolSpace, NamespaceEntityResolver>();

    public NamespaceEntityResolver get( SymbolSpace kind ) {
        NamespaceEntityResolver resolver = resolversByKind.get(kind);
        if (resolver == null) {
            resolver = new NamespaceEntityResolver();
            resolversByKind.put(kind, resolver);
        }
        return resolver;
    }

}
