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

import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.spi.graph.NamespaceRegistry;
import org.jboss.dna.spi.graph.PropertyFactory;
import org.jboss.dna.spi.graph.ValueFactories;
import org.jboss.dna.spi.graph.impl.BasicNamespaceRegistry;
import org.jboss.dna.spi.graph.impl.BasicPropertyFactory;
import org.jboss.dna.spi.graph.impl.StandardValueFactories;

/**
 * @author Randall Hauch
 */
public class BasicExecutionEnvironment implements ExecutionEnvironment {

    private final PropertyFactory propertyFactory;
    private final ValueFactories valueFactories;
    private final NamespaceRegistry namespaceRegistry;

    public BasicExecutionEnvironment() {
        this(new BasicNamespaceRegistry());
    }

    public BasicExecutionEnvironment( NamespaceRegistry namespaceRegistry ) {
        this(namespaceRegistry, null, null);
    }

    public BasicExecutionEnvironment( NamespaceRegistry namespaceRegistry,
                                      ValueFactories valueFactories,
                                      PropertyFactory propertyFactory ) {
        ArgCheck.isNotNull(namespaceRegistry, "namespace registry");
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
}
