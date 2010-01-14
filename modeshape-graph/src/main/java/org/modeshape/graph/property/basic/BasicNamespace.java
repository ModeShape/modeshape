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
package org.modeshape.graph.property.basic;

import net.jcip.annotations.Immutable;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.NamespaceRegistry.Namespace;

/**
 * Basic implementation of a {@link NamespaceRegistry} namespace.
 */
@Immutable
public class BasicNamespace implements NamespaceRegistry.Namespace {
    private final String prefix;
    private final String namespaceUri;

    /**
     * Create a namespace instance.
     * 
     * @param prefix the namespace prefix; may not be null (this is not checked)
     * @param namespaceUri the namespace URI; may not be null (this is not checked)
     */
    public BasicNamespace( String prefix,
                           String namespaceUri ) {
        assert prefix != null;
        assert namespaceUri != null;
        this.prefix = prefix;
        this.namespaceUri = namespaceUri;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.NamespaceRegistry.Namespace#getNamespaceUri()
     */
    public String getNamespaceUri() {
        return namespaceUri;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.NamespaceRegistry.Namespace#getPrefix()
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return namespaceUri.hashCode();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo( Namespace that ) {
        if (that == null) return 1;
        if (this == that) return 0;
        return this.getNamespaceUri().compareTo(that.getNamespaceUri());
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof Namespace) {
            Namespace that = (Namespace)obj;
            if (!this.namespaceUri.equals(that.getNamespaceUri())) return false;
            // if (!this.prefix.equals(that.getPrefix())) return false;
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return prefix + "=" + namespaceUri;
    }
}
