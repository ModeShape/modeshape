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

import java.util.HashSet;
import java.util.Set;
import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.modeshape.common.collection.Collections;
import org.modeshape.graph.connector.RepositorySourceException;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.basic.BasicNamespace;
import org.modeshape.graph.property.basic.SimpleNamespaceRegistry;

/**
 * This represents the {@link NamespaceRegistry} implementation mirroring a supplied JCR Session. This registry is used by a
 * custom context to create Name and Path objects from JCR Session values, and it ensures that any namespace used is also in the
 * connector's normal NamespaceRegistry.
 */
public class JcrNamespaceRegistry implements NamespaceRegistry {

    private final String sourceName;
    private final String defaultNamespaceUri;
    private final NamespaceRegistry cache;
    private final Session session;
    private final javax.jcr.NamespaceRegistry jcrRegistry;
    private final NamespaceRegistry connectorRegistry;

    JcrNamespaceRegistry( String sourceName,
                          Session session,
                          NamespaceRegistry connectorRegistry ) throws RepositoryException {
        this.sourceName = sourceName;
        this.session = session;
        this.jcrRegistry = this.session.getWorkspace().getNamespaceRegistry();
        this.connectorRegistry = connectorRegistry;
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
     * <p>
     * This method is most commonly used in this connector, because it is called to create {@link Name} and {@link Path} objects
     * given the string representation returned by the remote JCR session.
     * </p>
     * 
     * @see org.modeshape.graph.property.NamespaceRegistry#getNamespaceForPrefix(java.lang.String)
     */
    @Override
    public String getNamespaceForPrefix( String prefix ) {
        String uri = cache.getNamespaceForPrefix(prefix);
        if (uri == null) {
            try {
                uri = this.jcrRegistry.getURI(prefix);
                // Make sure this is in the connector's registry ...
                ensureRegisteredInConnector(prefix, uri);
            } catch (NamespaceException e) {
                // namespace is not known, so return null ...
            } catch (RepositoryException e) {
                throw new RepositorySourceException(sourceName, e);
            }
        }
        return uri;
    }

    protected void ensureRegisteredInConnector( String prefix,
                                                String uri ) {
        if (!connectorRegistry.isRegisteredNamespaceUri(uri)) {
            int index = 0;
            while (connectorRegistry.getNamespaceForPrefix(prefix) != null) {
                // The prefix is already used, so let it determine the best one ...
                prefix = prefix + (++index);
            }
            connectorRegistry.register(prefix, uri);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.NamespaceRegistry#getNamespaces()
     */
    @Override
    public Set<Namespace> getNamespaces() {
        // Always delegate to the session's registry ...
        Set<Namespace> namespaces = new HashSet<Namespace>();
        try {
            for (String prefix : this.jcrRegistry.getPrefixes()) {
                String uri = this.jcrRegistry.getURI(prefix);
                namespaces.add(new BasicNamespace(prefix, uri));
            }
        } catch (RepositoryException e) {
            throw new RepositorySourceException(sourceName, e);
        }
        return namespaces;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.NamespaceRegistry#getPrefixForNamespaceUri(java.lang.String, boolean)
     */
    @Override
    public String getPrefixForNamespaceUri( String namespaceUri,
                                            boolean generateIfMissing ) {
        String prefix = cache.getPrefixForNamespaceUri(namespaceUri, false);
        if (prefix == null) {
            try {
                // Check the session ...
                prefix = this.jcrRegistry.getPrefix(namespaceUri);
                // Make sure this is in the connector's registry ...
                ensureRegisteredInConnector(prefix, namespaceUri);
            } catch (NamespaceException e) {
                // namespace is not known, so return null ...
            } catch (RepositoryException e) {
                throw new RepositorySourceException(sourceName, e);
            }
        }
        return prefix;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.NamespaceRegistry#getRegisteredNamespaceUris()
     */
    @Override
    public Set<String> getRegisteredNamespaceUris() {
        // Always delegate to the session's registry ...
        try {
            return Collections.unmodifiableSet(this.jcrRegistry.getURIs());
        } catch (RepositoryException e) {
            throw new RepositorySourceException(sourceName, e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.NamespaceRegistry#isRegisteredNamespaceUri(java.lang.String)
     */
    @Override
    public boolean isRegisteredNamespaceUri( String namespaceUri ) {
        // Always delegate to the session's registry ...
        try {
            this.jcrRegistry.getPrefix(namespaceUri);
            return true;
        } catch (NamespaceException e) {
            // namespace is not known, so return false ...
            return false;
        } catch (RepositoryException e) {
            throw new RepositorySourceException(sourceName, e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.NamespaceRegistry#register(java.lang.String, java.lang.String)
     */
    @Override
    public String register( String prefix,
                            String namespaceUri ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.NamespaceRegistry#unregister(java.lang.String)
     */
    @Override
    public boolean unregister( String namespaceUri ) {
        throw new UnsupportedOperationException();
    }
}
