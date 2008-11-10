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
import java.security.AccessController;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import org.jboss.dna.common.component.ClassLoaderFactory;
import org.jboss.dna.common.component.StandardClassLoaderFactory;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.graph.properties.NamespaceRegistry;
import org.jboss.dna.graph.properties.PropertyFactory;
import org.jboss.dna.graph.properties.ValueFactories;
import org.jboss.dna.graph.properties.basic.BasicNamespaceRegistry;
import org.jboss.dna.graph.properties.basic.BasicPropertyFactory;
import org.jboss.dna.graph.properties.basic.StandardValueFactories;

/**
 * @author Randall Hauch
 * @author John Verhaeg
 */
public class BasicExecutionContext implements ExecutionContext {

    private final ClassLoaderFactory classLoaderFactory;
    private final LoginContext loginContext;
    private final AccessControlContext accessControlContext;
    private final Subject subject;
    private final PropertyFactory propertyFactory;
    private final ValueFactories valueFactories;
    private final NamespaceRegistry namespaceRegistry;

    public BasicExecutionContext() {
        this(null, null, null, null, null);
    }

    public BasicExecutionContext( LoginContext loginContext ) {
        this(loginContext, null, null, null, null);
    }

    public BasicExecutionContext( AccessControlContext accessControlContext ) {
        this(null, accessControlContext, null, null, null);
    }

    public BasicExecutionContext( NamespaceRegistry namespaceRegistry,
                                  ValueFactories valueFactories,
                                  PropertyFactory propertyFactory ) {
        this(null, null, namespaceRegistry, valueFactories, propertyFactory);
    }

    public BasicExecutionContext( NamespaceRegistry namespaceRegistry ) {
        this(null, null, namespaceRegistry, null, null);
    }

    public BasicExecutionContext( LoginContext loginContext,
                                  NamespaceRegistry namespaceRegistry,
                                  ValueFactories valueFactories,
                                  PropertyFactory propertyFactory ) {
        this(loginContext, null, namespaceRegistry, valueFactories, propertyFactory);
    }

    public BasicExecutionContext( AccessControlContext accessControlContext,
                                  NamespaceRegistry namespaceRegistry,
                                  ValueFactories valueFactories,
                                  PropertyFactory propertyFactory ) {
        this(null, accessControlContext, namespaceRegistry, valueFactories, propertyFactory);
    }

    public BasicExecutionContext( ExecutionContext inheritedContext,
                                  NamespaceRegistry namespaceRegistry ) {
        this(inheritedContext.getLoginContext(), inheritedContext.getAccessControlContext(), namespaceRegistry, null, null);
    }

    /*
     * This constructor exists to deal with mutually-exclusive parameters, such as LoginContext and AccessControlContext.
     */
    private BasicExecutionContext( LoginContext loginContext,
                                   AccessControlContext accessControlContext,
                                   NamespaceRegistry namespaceRegistry,
                                   ValueFactories valueFactories,
                                   PropertyFactory propertyFactory ) {
        this.loginContext = loginContext;
        this.accessControlContext = accessControlContext;
        if (loginContext == null) {
            this.subject = Subject.getSubject(accessControlContext == null ? AccessController.getContext() : accessControlContext);
        } else {
            this.subject = loginContext.getSubject();
        }
        this.namespaceRegistry = namespaceRegistry == null ? new BasicNamespaceRegistry() : namespaceRegistry;
        this.valueFactories = valueFactories == null ? new StandardValueFactories(this.namespaceRegistry) : valueFactories;
        this.propertyFactory = propertyFactory == null ? new BasicPropertyFactory(this.valueFactories) : propertyFactory;
        this.classLoaderFactory = new StandardClassLoaderFactory();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.common.component.ClassLoaderFactory#getClassLoader(java.lang.String[])
     */
    public ClassLoader getClassLoader( String... classpath ) {
        return this.classLoaderFactory.getClassLoader(classpath);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.ExecutionContext#getAccessControlContext()
     */
    public AccessControlContext getAccessControlContext() {
        return accessControlContext;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.ExecutionContext#getLoginContext()
     */
    public LoginContext getLoginContext() {
        return loginContext;
    }

    /**
     * {@inheritDoc}
     */
    public NamespaceRegistry getNamespaceRegistry() {
        return namespaceRegistry;
    }

    /**
     * {@inheritDoc}
     */
    public PropertyFactory getPropertyFactory() {
        return propertyFactory;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.ExecutionContext#getSubject()
     */
    public Subject getSubject() {
        return subject;
    }

    /**
     * {@inheritDoc}
     */
    public ValueFactories getValueFactories() {
        return valueFactories;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.ExecutionContext#getLogger(java.lang.Class)
     */
    public Logger getLogger( Class<?> clazz ) {
        return Logger.getLogger(clazz);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.ExecutionContext#getLogger(java.lang.String)
     */
    public Logger getLogger( String name ) {
        return Logger.getLogger(name);
    }
}
