/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.dna.jcr;

import java.util.Iterator;
import java.util.Set;
import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;

/**
 * @author John Verhaeg
 * @author Randall Hauch
 */
final class JcrNamespaceRegistry implements NamespaceRegistry {

    private org.jboss.dna.spi.graph.NamespaceRegistry dnaNamespaceRegistry;

    JcrNamespaceRegistry( org.jboss.dna.spi.graph.NamespaceRegistry dnaNamespaceRegistry ) {
        this.dnaNamespaceRegistry = dnaNamespaceRegistry;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.NamespaceRegistry#getPrefix(java.lang.String)
     */
    public String getPrefix( String uri ) throws NamespaceException, RepositoryException {
        String prefix = dnaNamespaceRegistry.getPrefixForNamespaceUri(uri, false);
        if (prefix == null) {
            throw new NamespaceException();
        }
        return prefix;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.NamespaceRegistry#getPrefixes()
     */
    public String[] getPrefixes() {
        Set<String> uris = dnaNamespaceRegistry.getRegisteredNamespaceUris();
        String[] prefixes = new String[uris.size()];
        Iterator<String> iter = uris.iterator();
        for (int ndx = 0; iter.hasNext(); ndx++) {
            prefixes[ndx] = dnaNamespaceRegistry.getPrefixForNamespaceUri(iter.next(), false);
        }
        return prefixes;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.NamespaceRegistry#getURI(java.lang.String)
     */
    public String getURI( String prefix ) throws NamespaceException {
        String uri = dnaNamespaceRegistry.getNamespaceForPrefix(prefix);
        if (uri == null) {
            throw new NamespaceException();
        }
        return uri;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.NamespaceRegistry#getURIs()
     */
    public String[] getURIs() {
        Set<String> uris = dnaNamespaceRegistry.getRegisteredNamespaceUris();
        return uris.toArray(new String[uris.size()]);
    }

    /**
     * {@inheritDoc}
     */
    public void registerNamespace( String prefix,
                                   String uri ) throws UnsupportedRepositoryOperationException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void unregisterNamespace( String prefix ) throws UnsupportedRepositoryOperationException {
        throw new UnsupportedRepositoryOperationException();
    }
}
