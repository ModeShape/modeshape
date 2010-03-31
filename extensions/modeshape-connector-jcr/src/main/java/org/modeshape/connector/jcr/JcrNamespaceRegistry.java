/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.connector.jcr;

import java.util.Set;
import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.modeshape.graph.connector.RepositorySourceException;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.basic.SimpleNamespaceRegistry;

/**
 * 
 */
public class JcrNamespaceRegistry implements NamespaceRegistry {

    private final String sourceName;
    private final String defaultNamespaceUri;
    private final NamespaceRegistry cache;
    private final Session session;
    private final javax.jcr.NamespaceRegistry jcrRegistry;

    JcrNamespaceRegistry( String sourceName,
                          Session session ) throws RepositoryException {
        this.sourceName = sourceName;
        this.session = session;
        this.jcrRegistry = this.session.getWorkspace().getNamespaceRegistry();
        this.cache = new SimpleNamespaceRegistry();
        assert this.session != null;
        assert this.cache != null;
        assert this.jcrRegistry != null;
        assert this.sourceName != null;
        this.defaultNamespaceUri = getNamespaceForPrefix("");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.NamespaceRegistry#getDefaultNamespaceUri()
     */
    @Override
    public String getDefaultNamespaceUri() {
        return this.defaultNamespaceUri;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.NamespaceRegistry#getNamespaceForPrefix(java.lang.String)
     */
    @Override
    public String getNamespaceForPrefix( String prefix ) {
        String uri = cache.getNamespaceForPrefix(prefix);
        if (uri == null) {
            try {
                uri = this.jcrRegistry.getURI(prefix);
            } catch (NamespaceException e) {
                // namespace is not known, so return null ...
            } catch (RepositoryException e) {
                throw new RepositorySourceException(sourceName, e);
            }
        }
        return uri;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.NamespaceRegistry#getNamespaces()
     */
    @Override
    public Set<Namespace> getNamespaces() {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.NamespaceRegistry#getPrefixForNamespaceUri(java.lang.String, boolean)
     */
    @Override
    public String getPrefixForNamespaceUri( String namespaceUri,
                                            boolean generateIfMissing ) {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.NamespaceRegistry#getRegisteredNamespaceUris()
     */
    @Override
    public Set<String> getRegisteredNamespaceUris() {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.NamespaceRegistry#isRegisteredNamespaceUri(java.lang.String)
     */
    @Override
    public boolean isRegisteredNamespaceUri( String namespaceUri ) {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.NamespaceRegistry#register(java.lang.String, java.lang.String)
     */
    @Override
    public String register( String prefix,
                            String namespaceUri ) {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.NamespaceRegistry#unregister(java.lang.String)
     */
    @Override
    public boolean unregister( String namespaceUri ) {
        return false;
    }
}
