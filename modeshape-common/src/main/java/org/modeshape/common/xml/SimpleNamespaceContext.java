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
package org.modeshape.common.xml;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.xml.namespace.NamespaceContext;

/**
 * A simple {@link NamespaceContext} implementation that maintains the namespace and prefix mappings discovered within an XML
 * document.
 */
public class SimpleNamespaceContext implements NamespaceContext {

    private final Map<String, String> prefixToNamespace = new HashMap<String, String>();

    /**
     * {@inheritDoc}
     */
    public String getNamespaceURI( String prefix ) {
        return this.prefixToNamespace.get(prefix);
    }

    /**
     * {@inheritDoc}
     */
    public String getPrefix( String namespaceURI ) {
        for (Map.Entry<String, String> entry : this.prefixToNamespace.entrySet()) {
            if (entry.getValue().equals(namespaceURI)) return entry.getKey();
        }
        return null;
    }

    public SimpleNamespaceContext setNamespace( String prefix,
                                                String namespaceUri ) {
        this.prefixToNamespace.put(prefix, namespaceUri);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<String> getPrefixes( String namespaceURI ) {
        return this.prefixToNamespace.keySet().iterator();
    }

}
