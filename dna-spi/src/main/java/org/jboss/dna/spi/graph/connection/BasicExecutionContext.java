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
package org.jboss.dna.spi.graph.connection;

import javax.security.auth.Subject;
import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.spi.ExecutionContext;
import org.jboss.dna.spi.graph.NamespaceRegistry;
import org.jboss.dna.spi.graph.PropertyFactory;
import org.jboss.dna.spi.graph.ValueFactories;
import org.jboss.dna.spi.graph.impl.BasicNamespaceRegistry;
import org.jboss.dna.spi.graph.impl.BasicPropertyFactory;
import org.jboss.dna.spi.graph.impl.StandardValueFactories;

/**
 * @author Randall Hauch
 */
public class BasicExecutionContext implements ExecutionContext {

    private final Subject subject;
    private final PropertyFactory propertyFactory;
    private final ValueFactories valueFactories;
    private final NamespaceRegistry namespaceRegistry;

    public BasicExecutionContext() {
        this(new Subject(), new BasicNamespaceRegistry());
    }

    public BasicExecutionContext( NamespaceRegistry namespaceRegistry ) {
        this(new Subject(), namespaceRegistry, null, null);
    }

    public BasicExecutionContext( NamespaceRegistry namespaceRegistry,
                                  ValueFactories valueFactories,
                                  PropertyFactory propertyFactory ) {
        this(new Subject(), namespaceRegistry, valueFactories, propertyFactory);
    }

    public BasicExecutionContext( Subject subject ) {
        this(subject, new BasicNamespaceRegistry());
    }

    public BasicExecutionContext( Subject subject,
                                  NamespaceRegistry namespaceRegistry ) {
        this(subject, namespaceRegistry, null, null);
    }

    public BasicExecutionContext( Subject subject,
                                  NamespaceRegistry namespaceRegistry,
                                  ValueFactories valueFactories,
                                  PropertyFactory propertyFactory ) {
        ArgCheck.isNotNull(subject, "subject");
        ArgCheck.isNotNull(namespaceRegistry, "namespace registry");
        this.subject = subject;
        this.namespaceRegistry = namespaceRegistry;
        this.valueFactories = valueFactories != null ? valueFactories : new StandardValueFactories(this.namespaceRegistry);
        this.propertyFactory = propertyFactory != null ? propertyFactory : new BasicPropertyFactory(this.valueFactories);
    }

    /**
     * {@inheritDoc}
     */
    public NamespaceRegistry getNamespaceRegistry() {
        return this.namespaceRegistry;
    }

    /**
     * {@inheritDoc}
     */
    public ValueFactories getValueFactories() {
        return this.valueFactories;
    }

    /**
     * {@inheritDoc}
     */
    public PropertyFactory getPropertyFactory() {
        return this.propertyFactory;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.ExecutionContext#getSubject()
     */
    public Subject getSubject() {
        return this.subject;
    }
}
