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
package org.jboss.dna.repository.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.property.NamespaceException;
import org.jboss.dna.graph.property.NamespaceRegistry;
import org.jboss.dna.graph.property.basic.BasicNamespace;

/**
 * @author Randall Hauch
 */
public class JcrNamespaceRegistry implements NamespaceRegistry {

    private final String repositoryWorkspaceName;
    private final SessionFactory sessionFactory;

    public JcrNamespaceRegistry( SessionFactory sessionFactory,
                                 String repositoryWorkspaceName ) {
        CheckArg.isNotNull(sessionFactory, "sessionFactory");
        CheckArg.isNotNull(repositoryWorkspaceName, "repositoryWorkspaceName");
        this.repositoryWorkspaceName = repositoryWorkspaceName;
        this.sessionFactory = sessionFactory;
    }

    /**
     * {@inheritDoc}
     */
    public String getDefaultNamespaceUri() {
        Session session = null;
        try {
            session = this.sessionFactory.createSession(this.repositoryWorkspaceName);
            return session.getNamespaceURI("");
        } catch (RepositoryException e) {
            throw new NamespaceException(e);
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getNamespaceForPrefix( String prefix ) {
        Session session = null;
        try {
            session = this.sessionFactory.createSession(this.repositoryWorkspaceName);
            return session.getNamespaceURI(prefix);
        } catch (RepositoryException e) {
            throw new NamespaceException(e);
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getPrefixForNamespaceUri( String namespaceUri,
                                            boolean generateIfMissing ) {
        Session session = null;
        try {
            session = this.sessionFactory.createSession(this.repositoryWorkspaceName);
            return session.getNamespacePrefix(namespaceUri);
        } catch (RepositoryException e) {
            throw new NamespaceException(e);
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isRegisteredNamespaceUri( String namespaceUri ) {
        Session session = null;
        try {
            session = this.sessionFactory.createSession(this.repositoryWorkspaceName);
            session.getNamespacePrefix(namespaceUri);
            return true;
        } catch (javax.jcr.NamespaceException e) {
            return false;
        } catch (RepositoryException e) {
            throw new NamespaceException(e);
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public String register( String prefix,
                            String namespaceUri ) {
        String previousNamespaceUriForPrefix = null;
        Session session = null;
        try {
            session = this.sessionFactory.createSession(this.repositoryWorkspaceName);
            previousNamespaceUriForPrefix = session.getNamespacePrefix(namespaceUri);
            javax.jcr.NamespaceRegistry registry = session.getWorkspace().getNamespaceRegistry();
            registry.registerNamespace(prefix, namespaceUri);
        } catch (RepositoryException e) {
            throw new NamespaceException(e);
        } finally {
            if (session != null) {
                session.logout();
            }
        }
        return previousNamespaceUriForPrefix;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.NamespaceRegistry#unregister(java.lang.String)
     */
    public boolean unregister( String namespaceUri ) {
        Session session = null;
        try {
            session = this.sessionFactory.createSession(this.repositoryWorkspaceName);
            String prefix = session.getNamespacePrefix(namespaceUri);
            javax.jcr.NamespaceRegistry registry = session.getWorkspace().getNamespaceRegistry();
            registry.unregisterNamespace(prefix);
        } catch (javax.jcr.NamespaceException e) {
            return false;
        } catch (RepositoryException e) {
            throw new NamespaceException(e);
        } finally {
            if (session != null) {
                session.logout();
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getRegisteredNamespaceUris() {
        Session session = null;
        try {
            session = this.sessionFactory.createSession(this.repositoryWorkspaceName);
            javax.jcr.NamespaceRegistry registry = session.getWorkspace().getNamespaceRegistry();
            Set<String> result = new HashSet<String>();
            for (String uri : registry.getURIs()) {
                result.add(uri);
            }
            return Collections.unmodifiableSet(result);
        } catch (RepositoryException e) {
            throw new NamespaceException(e);
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.property.NamespaceRegistry#getNamespaces()
     */
    public Set<Namespace> getNamespaces() {
        Session session = null;
        try {
            session = this.sessionFactory.createSession(this.repositoryWorkspaceName);
            javax.jcr.NamespaceRegistry registry = session.getWorkspace().getNamespaceRegistry();
            Set<Namespace> result = new HashSet<Namespace>();
            for (String uri : registry.getURIs()) {
                String prefix = registry.getPrefix(uri);
                result.add(new BasicNamespace(prefix, uri));
            }
            return Collections.unmodifiableSet(result);
        } catch (RepositoryException e) {
            throw new NamespaceException(e);
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

}
