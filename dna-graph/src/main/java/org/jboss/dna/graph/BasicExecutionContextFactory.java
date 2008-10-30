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
package org.jboss.dna.graph;

import java.security.AccessControlContext;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import org.jboss.dna.graph.properties.NamespaceRegistry;
import org.jboss.dna.graph.properties.basic.BasicNamespaceRegistry;

/**
 * Basic implementation of a {@link ExecutionContextFactory} that returns {@link BasicExecutionContext basic}
 * {@link ExecutionContext}s.
 * 
 * @author Randall Hauch
 */
public class BasicExecutionContextFactory implements ExecutionContextFactory {

    private final NamespaceRegistry defaultNamespaces = new BasicNamespaceRegistry();

    /**
     * Create a new instance of this factory.
     */
    public BasicExecutionContextFactory() {
        defaultNamespaces.register("jcr", "http://www.jcp.org/jcr/1.0");
        defaultNamespaces.register("mix", "http://www.jcp.org/jcr/mix/1.0");
        defaultNamespaces.register("nt", "http://www.jcp.org/jcr/nt/1.0");
        defaultNamespaces.register("dna", "http://www.jboss.org/dna");
        defaultNamespaces.register("dnadtd", "http://www.jboss.org/dna/dtd/1.0");
        defaultNamespaces.register("dnaxml", "http://www.jboss.org/dna/xml/1.0");
    }

    /**
     * Create a new instance of this factory.
     * 
     * @param defaultNamespaceUri the URI of the namespace that should be used with names that have no specified namespace prefix
     * @throws IllegalArgumentException if the URI is null
     */
    public BasicExecutionContextFactory( String defaultNamespaceUri ) {
        this();
        defaultNamespaces.register("", defaultNamespaceUri);
    }

    /**
     * @return defaultNamespaces
     */
    public NamespaceRegistry getDefaultNamespaces() {
        return defaultNamespaces;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.ExecutionContextFactory#create()
     */
    public ExecutionContext create() {
        ExecutionContext context = new BasicExecutionContext();
        initialize(context);
        return context;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.ExecutionContextFactory#create(java.security.AccessControlContext)
     */
    public ExecutionContext create( AccessControlContext accessControlContext ) {
        ExecutionContext context = new BasicExecutionContext(accessControlContext);
        initialize(context);
        return context;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.ExecutionContextFactory#create(javax.security.auth.login.LoginContext)
     */
    public ExecutionContext create( LoginContext loginContext ) {
        ExecutionContext context = new BasicExecutionContext(loginContext);
        initialize(context);
        return context;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.ExecutionContextFactory#create(java.lang.String)
     */
    public ExecutionContext create( String name ) throws LoginException {
        LoginContext loginContext = new LoginContext(name);
        ExecutionContext context = new BasicExecutionContext(loginContext);
        initialize(context);
        return context;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.ExecutionContextFactory#create(java.lang.String, javax.security.auth.Subject)
     */
    public ExecutionContext create( String name,
                                    Subject subject ) throws LoginException {
        LoginContext loginContext = new LoginContext(name, subject);
        ExecutionContext context = new BasicExecutionContext(loginContext);
        initialize(context);
        return context;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.ExecutionContextFactory#create(java.lang.String, javax.security.auth.callback.CallbackHandler)
     */
    public ExecutionContext create( String name,
                                    CallbackHandler callbackHandler ) throws LoginException {
        LoginContext loginContext = new LoginContext(name, callbackHandler);
        ExecutionContext context = new BasicExecutionContext(loginContext);
        initialize(context);
        return context;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.ExecutionContextFactory#create(java.lang.String, javax.security.auth.Subject,
     *      javax.security.auth.callback.CallbackHandler)
     */
    public ExecutionContext create( String name,
                                    Subject subject,
                                    CallbackHandler callbackHandler ) throws LoginException {
        LoginContext loginContext = new LoginContext(name, subject, callbackHandler);
        ExecutionContext context = new BasicExecutionContext(loginContext);
        initialize(context);
        return context;
    }

    protected synchronized void initialize( ExecutionContext context ) {
        for (NamespaceRegistry.Namespace namespace : this.defaultNamespaces.getNamespaces()) {
            context.getNamespaceRegistry().register(namespace.getPrefix(), namespace.getNamespaceUri());
        }
    }
}
