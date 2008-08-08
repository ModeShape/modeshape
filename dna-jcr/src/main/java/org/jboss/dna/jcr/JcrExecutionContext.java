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

import java.security.AccessControlContext;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.spi.ExecutionContext;
import org.jboss.dna.spi.connector.RepositoryConnection;
import org.jboss.dna.spi.graph.NamespaceRegistry;
import org.jboss.dna.spi.graph.PropertyFactory;
import org.jboss.dna.spi.graph.ValueFactories;

/**
 * @author jverhaeg
 */
@NotThreadSafe
class JcrExecutionContext implements ExecutionContext {

    private final ExecutionContext delegate;
    private final GraphTools tools;

    JcrExecutionContext( ExecutionContext delegate,
                         RepositoryConnection connection ) {
        assert delegate != null;
        assert connection != null;
        this.delegate = delegate;
        tools = new GraphTools(delegate, connection);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.ExecutionContext#getAccessControlContext()
     */
    public AccessControlContext getAccessControlContext() {
        return delegate.getAccessControlContext();
    }

    GraphTools getGraphTools() {
        return tools;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.ExecutionContext#getLoginContext()
     */
    public LoginContext getLoginContext() {
        return delegate.getLoginContext();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.ExecutionContext#getNamespaceRegistry()
     */
    public NamespaceRegistry getNamespaceRegistry() {
        return delegate.getNamespaceRegistry();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.ExecutionContext#getPropertyFactory()
     */
    public PropertyFactory getPropertyFactory() {
        return delegate.getPropertyFactory();
    }

    RepositoryConnection getRepositoryConnection() {
        return tools.getConnection();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.ExecutionContext#getSubject()
     */
    public Subject getSubject() {
        return delegate.getSubject();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.ExecutionContext#getValueFactories()
     */
    public ValueFactories getValueFactories() {
        return delegate.getValueFactories();
    }
}
