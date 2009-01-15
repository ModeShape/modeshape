/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.graph.property.basic;

import net.jcip.annotations.Immutable;
import org.jboss.dna.graph.property.NamespaceRegistry;
import org.jboss.dna.graph.property.NamespaceRegistry.Namespace;

/**
 * @author Randall Hauch
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
     * @see org.jboss.dna.graph.property.NamespaceRegistry.Namespace#getNamespaceUri()
     */
    public String getNamespaceUri() {
        return namespaceUri;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.NamespaceRegistry.Namespace#getPrefix()
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
        return prefix + ":" + namespaceUri;
    }
}
