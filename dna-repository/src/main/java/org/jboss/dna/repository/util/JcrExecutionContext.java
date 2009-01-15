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
package org.jboss.dna.repository.util;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.repository.RepositoryI18n;

/**
 * The context of an execution within a JCR environment.
 * 
 * @author Randall Hauch
 */
public class JcrExecutionContext extends ExecutionContext {

    private final String repositoryWorkspaceForNamespaceRegistry;
    private final ClosableSessionFactory sessionFactory;
    private final JcrTools jcrTools;

    public JcrExecutionContext( ExecutionContext context,
                                final SessionFactory sessionFactory,
                                String repositoryWorkspaceForNamespaceRegistry ) {
        super(context.with(new JcrNamespaceRegistry(sessionFactory, repositoryWorkspaceForNamespaceRegistry)));
        this.sessionFactory = new ClosableSessionFactory(sessionFactory);
        this.jcrTools = new JcrTools();
        this.repositoryWorkspaceForNamespaceRegistry = repositoryWorkspaceForNamespaceRegistry;
    }

    public JcrExecutionContext( SessionFactory sessionFactory,
                                String repositoryWorkspaceForNamespaceRegistry ) {
        this(new ExecutionContext(), sessionFactory, repositoryWorkspaceForNamespaceRegistry);
    }

    /**
     * Get the session factory, which can be used to obtain sessions temporarily for this context. Any session obtained from this
     * factory should be {@link Session#logout() closed} before the execution finishes.
     * 
     * @return the session factory
     */
    public SessionFactory getSessionFactory() {
        return this.sessionFactory;
    }

    /**
     * Get a set of utilities for working with JCR.
     * 
     * @return the tools
     */
    public JcrTools getTools() {
        return this.jcrTools;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.ExecutionContext#clone()
     */
    @Override
    public JcrExecutionContext clone() {
        return new JcrExecutionContext(this, this.sessionFactory.getDelegateFactory(),
                                       this.repositoryWorkspaceForNamespaceRegistry);
    }

    /**
     * This this context and release all resources (including any Session instances created).
     */
    public void close() {
        this.sessionFactory.close();
    }

    protected static class ClosableSessionFactory implements SessionFactory {
        private final SessionFactory delegateFactory;
        private final Set<Session> sessions = new HashSet<Session>();
        protected final AtomicBoolean closed = new AtomicBoolean(false);

        protected ClosableSessionFactory( SessionFactory sessionFactory ) {
            this.delegateFactory = sessionFactory;
        }

        public SessionFactory getDelegateFactory() {
            return this.delegateFactory;
        }

        public Session createSession( String name ) throws RepositoryException {
            if (closed.get()) throw new IllegalStateException(RepositoryI18n.executionContextHasBeenClosed.text());
            Session session = delegateFactory.createSession(name);
            if (session != null) sessions.add(session);
            return session;
        }

        public synchronized void close() {
            if (this.closed.get()) return;
            this.closed.set(true);
            for (Session session : sessions) {
                if (session != null) session.logout();
            }
        }
    }

}
